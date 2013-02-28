package net.sf.saxon.expr;

import net.sf.saxon.Configuration;
import net.sf.saxon.expr.parser.ExpressionVisitor;
import net.sf.saxon.expr.parser.RoleLocator;
import net.sf.saxon.expr.parser.TypeChecker;
import net.sf.saxon.functions.StringFn;
import net.sf.saxon.lib.ConversionRules;
import net.sf.saxon.om.*;
import net.sf.saxon.pattern.AnyNodeTest;
import net.sf.saxon.pattern.EmptySequenceTest;
import net.sf.saxon.trace.ExpressionPresenter;
import net.sf.saxon.trans.XPathException;
import net.sf.saxon.type.*;
import net.sf.saxon.value.*;

/**
 * Cast Expression: implements "cast as data-type ( expression )". It also allows an internal
 * cast, which has the same semantics as a user-requested cast, but maps an empty sequence to
 * an empty sequence.
 *
 * This expression class does not handle casting to a union type.
*/

public class CastExpression extends CastingExpression  {

    /**
     * Create a cast expression
     * @param source expression giving the value to be converted
     * @param target the type to which the value is to be converted
     * @param allowEmpty true if the expression allows an empty sequence as input, producing
     * an empty sequence as output. If false, an empty sequence is a type error.
     */

    public CastExpression(Expression source, AtomicType target, boolean allowEmpty) {
        super(source, target, allowEmpty);
    }

    /**
    * Type-check the expression
    */

    /*@NotNull*/
    public Expression typeCheck(ExpressionVisitor visitor, ExpressionVisitor.ContextItemType contextItemType) throws XPathException {
        operand = visitor.typeCheck(operand, contextItemType);
        SequenceType atomicType = SequenceType.makeSequenceType(BuiltInAtomicType.ANY_ATOMIC, getCardinality());

        final TypeHierarchy th = visitor.getConfiguration().getTypeHierarchy();
        RoleLocator role = new RoleLocator(RoleLocator.TYPE_OP, "cast as", 0);
        ItemType sourceItemType = null;

        if (getTargetType().isNamespaceSensitive()) {
            int rel = th.relationship(operand.getItemType(th), AnyNodeTest.getInstance());
            if (rel == TypeHierarchy.SAME_TYPE || rel == TypeHierarchy.SUBSUMED_BY) {
                // See spec bug 11964
                XPathException err = new XPathException("Atomization is not allowed when casting to QName or NOTATION");
                err.setErrorCode("XPTY0004");
                err.setLocator(this);
                err.setIsTypeError(true);
                throw err;
            } else if (rel != TypeHierarchy.DISJOINT) {
                operand = new ItemChecker(operand, BuiltInAtomicType.ANY_ATOMIC, role);
            }
            sourceItemType = BuiltInAtomicType.ANY_ATOMIC;
        } else {
            operand = TypeChecker.staticTypeCheck(operand, atomicType, false, role, visitor);
            sourceItemType = operand.getItemType(th);
        }

        if (sourceItemType instanceof EmptySequenceTest) {
            if (allowsEmpty()) {
                return new Literal(EmptySequence.getInstance());
            } else {
                XPathException err = new XPathException("Cast does not allow an empty sequence as input");
                err.setErrorCode("XPTY0004");
                err.setLocator(this);
                err.setIsTypeError(true);
                throw err;
            }
        }
        AtomicType sourceType = (AtomicType) sourceItemType;
        int r = th.relationship(sourceType, getTargetType());
        if (r == TypeHierarchy.SAME_TYPE) {
            return operand;
        } else if (r == TypeHierarchy.SUBSUMED_BY) {
            // It's generally true that any expression defined to return an X is allowed to return a subtype of X.
            // However, people seem to get upset if we treat the cast as a no-op.
            converter = new Converter.UpCastingConverter(getTargetType());
        } else {

            ConversionRules rules = visitor.getConfiguration().getConversionRules();

            if (!(sourceType == BuiltInAtomicType.ANY_ATOMIC || sourceType.getFingerprint() == Type.EMPTY)) {
                converter = rules.getConverter(sourceType, getTargetType());
                if (converter == null) {
                    XPathException err = new XPathException("Casting from " + sourceType + " to " + getTargetType() +
                            " can never succeed");
                    err.setErrorCode("XPTY0004");
                    err.setLocator(this);
                    err.setIsTypeError(true);
                    throw err;
                } else {
                    if (getTargetType().isNamespaceSensitive()) {
                        converter.setNamespaceResolver(nsResolver);
                    }
                }
                if (converter.isXPath30Conversion() && !visitor.getStaticContext().getXPathLanguageLevel().equals(DecimalValue.THREE)
                        && !(operand instanceof Literal && getTargetType().getPrimitiveType() == StandardNames.XS_QNAME)) {
                    XPathException err = new XPathException("Casting from " + sourceType + " to " + getTargetType() +
                            " requires XPath 3.0 functionality to be enabled");
                    err.setErrorCode("XPTY0004");
                    err.setLocator(this);
                    err.setIsTypeError(true);
                    throw err;
                }
            }
        }
        
        if (operand instanceof Literal) {
            return preEvaluate();
        }

        return this;
    }

    /**
     * Perform optimisation of an expression and its subexpressions.
     * <p/>
     * <p>This method is called after all references to functions and variables have been resolved
     * to the declaration of the function or variable, and after all type checking has been done.</p>
     * @param visitor         an expression visitor
     * @param contextItemType the static type of "." at the point where this expression is invoked.
     *                        The parameter is set to null if it is known statically that the context item will be undefined.
     *                        If the type of the context item is not known statically, the argument is set to
     *                        {@link net.sf.saxon.type.Type#ITEM_TYPE}
     * @return the original expression, rewritten if appropriate to optimize execution
     * @throws net.sf.saxon.trans.XPathException
     *          if an error is discovered during this phase
     *          (typically a type error)
     */

    /*@NotNull*/
    public Expression optimize(ExpressionVisitor visitor, ExpressionVisitor.ContextItemType contextItemType) throws XPathException {
        final TypeHierarchy th = visitor.getConfiguration().getTypeHierarchy();
        Expression e2 = super.optimize(visitor, contextItemType);
        if (e2 != this) {
            return e2;
        }
        // Eliminate pointless casting between untypedAtomic and string
        if (getTargetType() == BuiltInAtomicType.UNTYPED_ATOMIC) {
            if (operand instanceof StringFn) {
                Expression e = ((StringFn)operand).getArguments()[0];
                if (e.getItemType(th) instanceof AtomicType && e.getCardinality() == StaticProperty.EXACTLY_ONE) {
                    operand = e;
                }
            } else if (operand instanceof CastExpression) {
                if (((CastExpression)operand).getTargetType() == BuiltInAtomicType.UNTYPED_ATOMIC) {
                    return operand;
                } else if (((CastExpression)operand).getTargetType() == BuiltInAtomicType.STRING) {
                    ((CastExpression)operand).setTargetType(BuiltInAtomicType.UNTYPED_ATOMIC);
                    return operand;
                }
            } else if (operand instanceof AtomicSequenceConverter) {
                if (operand.getItemType(th) == BuiltInAtomicType.UNTYPED_ATOMIC) {
                    return operand;
                } else if (operand.getItemType(th) == BuiltInAtomicType.STRING) {
                    AtomicSequenceConverter old = (AtomicSequenceConverter)operand;
                    AtomicSequenceConverter asc = new AtomicSequenceConverter(
                            old.getBaseExpression(),
                            BuiltInAtomicType.UNTYPED_ATOMIC,
                            old.isAllItemsConverted());
                    return asc.typeCheck(visitor, contextItemType)
                            .optimize(visitor, contextItemType);
                }
            }
        }
        // avoid converting anything to a string and then back again
        if (operand instanceof StringFn) {
            Expression e = ((StringFn)operand).getArguments()[0];
            ItemType et = e.getItemType(th);
            if (et instanceof AtomicType &&
                    e.getCardinality() == StaticProperty.EXACTLY_ONE &&
                    th.isSubType(et, getTargetType())) {
                return e;
            }
        }
        // avoid converting anything to untypedAtomic and then back again
        if (operand instanceof CastExpression) {
            ItemType it = ((CastExpression)operand).getTargetType();
            if (th.isSubType(it, BuiltInAtomicType.STRING) || th.isSubType(it, BuiltInAtomicType.UNTYPED_ATOMIC)) {
                Expression e = ((CastExpression)operand).getBaseExpression();
                ItemType et = e.getItemType(th);
                if (et instanceof AtomicType &&
                        e.getCardinality() == StaticProperty.EXACTLY_ONE &&
                        th.isSubType(et, getTargetType())) {
                    return e;
                }
            }
        }
        if (operand instanceof AtomicSequenceConverter) {
            ItemType it = operand.getItemType(th);
            if (th.isSubType(it, BuiltInAtomicType.STRING) || th.isSubType(it, BuiltInAtomicType.UNTYPED_ATOMIC)) {
                Expression e = ((AtomicSequenceConverter)operand).getBaseExpression();
                ItemType et = e.getItemType(th);
                if (et instanceof AtomicType &&
                        e.getCardinality() == StaticProperty.EXACTLY_ONE &&
                        th.isSubType(et, getTargetType())) {
                    return e;
                }
            }
        }
        // if the operand can't be empty, then set allowEmpty to false to provide more information for analysis
        if (!Cardinality.allowsZero(operand.getCardinality())) {
            setAllowEmpty(false);
            resetLocalStaticProperties();
        }

        if (operand instanceof Literal) {
            return preEvaluate();
        }
        return this;
    }

    /**
     * Perform early (compile-time) evaluation
     */

    protected Expression preEvaluate() throws XPathException {
        Value literalOperand = ((Literal)operand).getValue();
        if (literalOperand instanceof AtomicValue && converter != null) {
            ConversionResult result = converter.convert((AtomicValue)literalOperand);
            if (result instanceof ValidationFailure) {
                ValidationFailure err = (ValidationFailure)result;
                String code = err.getErrorCode();
                if (code == null) {
                    code = "FORG0001";
                }
                XPathException xpe = new XPathException(err.getMessage(), this);
                xpe.setErrorCode(code);
                throw xpe;
            } else {
                return Literal.makeLiteral((AtomicValue)result);
            }
        }
        if (literalOperand instanceof EmptySequence) {
            if (allowsEmpty()) {
                return operand;
            } else {
                XPathException err = new XPathException("Cast can never succeed: the operand must not be an empty sequence");
                err.setErrorCode("XPTY0004");
                err.setLocator(this);
                err.setIsTypeError(true);
                throw err;
            }
        }
        return this;
    }

    /**
    * Get the static cardinality of the expression
    */

    public int computeCardinality() {
        return (allowsEmpty() && Cardinality.allowsZero(operand.getCardinality())
                ? StaticProperty.ALLOWS_ZERO_OR_ONE : StaticProperty.EXACTLY_ONE);
    }

    /**
     * Get the static type of the expression
     * @param th the type hierarchy cache
     */

    /*@NotNull*/
    public ItemType getItemType(TypeHierarchy th) {
        return getTargetType();
    }

    /**
     * For an expression that returns an integer or a sequence of integers, get
     * a lower and upper bound on the values of the integers that may be returned, from
     * static analysis. The default implementation returns null, meaning "unknown" or
     * "not applicable". Other implementations return an array of two IntegerValue objects,
     * representing the lower and upper bounds respectively. The values
     * UNBOUNDED_LOWER and UNBOUNDED_UPPER are used by convention to indicate that
     * the value may be arbitrarily large. The values MAX_STRING_LENGTH and MAX_SEQUENCE_LENGTH
     * are used to indicate values limited by the size of a string or the size of a sequence.
     *
     * @return the lower and upper bounds of integer values in the result, or null to indicate
     *         unknown or not applicable.
     */
    /*@Nullable*/@Override
    public IntegerValue[] getIntegerBounds() {
        if (converter == Converter.BOOLEAN_TO_INTEGER) {
            return new IntegerValue[]{Int64Value.ZERO, Int64Value.PLUS_ONE};
        } else {
            return null;
        }
    }

    /**
     * Copy an expression. This makes a deep copy.
     *
     * @return the copy of the original expression
     */

    /*@NotNull*/
    public Expression copy() {
        CastExpression c2 = new CastExpression(getBaseExpression().copy(), getTargetType(), allowsEmpty());
        c2.converter = converter;
        c2.nsResolver = nsResolver;
        c2.setOperandIsStringLiteral(isOperandIsStringLiteral());
        return c2;
    }

    /**
    * Evaluate the expression
    */

    /*@Nullable*/ public Item evaluateItem(XPathContext context) throws XPathException {
        AtomicValue value = (AtomicValue)operand.evaluateItem(context);
        if (value==null) {
            if (allowsEmpty()) {
                return null;
            } else {
                XPathException e = new XPathException("Cast does not allow an empty sequence");
                e.setXPathContext(context);
                e.setLocator(this);
                e.setErrorCode("XPTY0004");
                throw e;
            }
        }

        Converter converter = this.converter;
        if (converter == null) {
            ConversionRules rules = context.getConfiguration().getConversionRules();
            converter = rules.getConverter(value.getPrimitiveType(), getTargetType());
            if (converter == null) {
                XPathException e = new XPathException("Casting from " + value.getPrimitiveType() + " to " + getTargetType() + " is not permitted");
                e.setXPathContext(context);
                e.setLocator(this);
                e.setErrorCode("XPTY0004");
                throw e;
            }
            if (converter.isXPath30Conversion() && !getExecutable().isAllowXPath30() &&
                    !(converter instanceof StringConverter.StringToQName && isOperandIsStringLiteral())) {
                XPathException e = new XPathException("Casting from " + value.getPrimitiveType() + " to " + getTargetType() + " requires XPath 3.0 to be enabled");
                e.setXPathContext(context);
                e.setLocator(this);
                e.setErrorCode("XPTY0004");
                throw e;
            }
            if (nsResolver != null) {
                converter.setNamespaceResolver(nsResolver);
            }
        }
        ConversionResult result = converter.convert(value);
        if (result instanceof ValidationFailure) {
            ValidationFailure err = (ValidationFailure)result;
            String code = err.getErrorCode();
            if (code == null) {
                code = "FORG0001";
            }
            dynamicError(err.getMessage(), code, context);
            return null;
        }
        return (AtomicValue)result;
    }

    /**
     * Is this expression the same as another expression?
     */

    public boolean equals(Object other) {
        return other instanceof CastExpression &&
                operand.equals(((CastExpression)other).operand) &&
                getTargetType() == ((CastExpression)other).getTargetType() &&
                getTargetType() == ((CastExpression)other).getTargetType();
    }

    /**
     * get HashCode for comparing two expressions. Note that this hashcode gives the same
     * result for (A op B) and for (B op A), whether or not the operator is commutative.
     */

    @Override
    public int hashCode() {
        return super.hashCode() ^ getTargetType().hashCode();
    }

    /**
     * The toString() method for an expression attempts to give a representation of the expression
     * in an XPath-like form, but there is no guarantee that the syntax will actually be true XPath.
     * In the case of XSLT instructions, the toString() method gives an abstracted view of the syntax
     */

    public String toString() {
        try {
            NamePool pool = getExecutable().getConfiguration().getNamePool();
            return getTargetType().toString(pool) + "(" + operand.toString() + ")";
        } catch (Exception err) {
            return getTargetType().toString() + "(" + operand.toString() + ")";
        }
    }

    /**
     * Diagnostic print of expression structure. The abstract expression tree
     * is written to the supplied output destination.
     */

    public void explain(ExpressionPresenter out) {
        out.startElement("cast");
        out.emitAttribute("as", getTargetType().toString(out.getConfiguration().getNamePool()));
        operand.explain(out);
        out.endElement();
    }

    /**
     * Evaluate the "pseudo-cast" of a string literal to a QName or NOTATION value. This can only happen
     * at compile time
     * @param operand the value to be converted
     * @param targetType the type to which it is to be converted
     * @param env the static context
     * @return the QName or NOTATION value that results from casting the string to a QName.
     * This will either be a QNameValue or a derived AtomicValue derived from QName or NOTATION
     */

    /*@Nullable*/ public static AtomicValue castStringToQName(
            CharSequence operand, AtomicType targetType, StaticContext env) throws XPathException {
        try {
            ConversionRules rules = env.getConfiguration().getConversionRules();
            CharSequence arg = Whitespace.trimWhitespace(operand);
            String parts[] = env.getConfiguration().getNameChecker().getQNameParts(arg);
            String uri;
            if (parts[0].length() == 0) {
                uri = env.getDefaultElementNamespace();
            } else {
                try {
                    uri = env.getURIForPrefix(parts[0]);
                } catch (XPathException e) {
                    uri = null;
                }
                if (uri == null) {
                    XPathException e = new XPathException("Prefix '" + parts[0] + "' has not been declared");
                    e.setErrorCode("FONS0004");
                    throw e;
                }
            }
            Configuration config = env.getConfiguration();
            final NameChecker checker = config.getNameChecker();
            final TypeHierarchy th = config.getTypeHierarchy();
            if (targetType.getFingerprint() == StandardNames.XS_QNAME) {
                return new QNameValue(parts[0], uri, parts[1], BuiltInAtomicType.QNAME, checker);
            } else if (th.isSubType(targetType, BuiltInAtomicType.QNAME)) {
                QNameValue q = new QNameValue(parts[0], uri, parts[1], targetType, checker);
                ValidationFailure vf = targetType.validate(q, null, rules);
                if (vf != null) {
                    throw vf.makeException();
                }
                q.setTypeLabel(targetType);
                return q;
            } else {
                NotationValue n = new NotationValue(parts[0], uri, parts[1], checker);
                ValidationFailure vf = targetType.validate(n, null, rules);
                if (vf != null) {
                    throw vf.makeException();
                }
                n.setTypeLabel(targetType);
                return n;
            }
        } catch (XPathException err) {
            if (err.getErrorCodeQName() == null) {
                err.setErrorCode("FONS0004");
            }
            throw err;
        } catch (QNameException err) {
            XPathException e = new XPathException(err);
            e.setErrorCode("FORG0001");
            throw e;
        }
    }

}

//
// The contents of this file are subject to the Mozilla Public License Version 1.0 (the "License");
// you may not use this file except in compliance with the License. You may obtain a copy of the
// License at http://www.mozilla.org/MPL/
//
// Software distributed under the License is distributed on an "AS IS" basis,
// WITHOUT WARRANTY OF ANY KIND, either express or implied.
// See the License for the specific language governing rights and limitations under the License.
//
// The Original Code is: all this file
//
// The Initial Developer of the Original Code is Saxonica Limited.
// Portions created by ___ are Copyright (C) ___. All rights reserved.
//
// Contributor(s):
//