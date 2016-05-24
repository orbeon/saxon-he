////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
// Copyright (c) 2015 Saxonica Limited.
// This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
// If a copy of the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
// This Source Code Form is "Incompatible With Secondary Licenses", as defined by the Mozilla Public License, v. 2.0.
////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

package net.sf.saxon.expr;


import com.saxonica.ee.bytecode.CastExpressionCompiler;
import com.saxonica.ee.bytecode.ExpressionCompiler;
import net.sf.saxon.expr.parser.*;
import net.sf.saxon.functions.String_1;
import net.sf.saxon.lib.ConversionRules;
import net.sf.saxon.om.GroundedValue;
import net.sf.saxon.om.Sequence;
import net.sf.saxon.om.StandardNames;
import net.sf.saxon.trace.ExpressionPresenter;
import net.sf.saxon.trans.XPathException;
import net.sf.saxon.type.*;
import net.sf.saxon.value.*;


/**
 * Cast Expression: implements "cast as data-type ( expression )". It also allows an internal
 * cast, which has the same semantics as a user-requested cast, but maps an empty sequence to
 * an empty sequence.
 * <p/>
 * This expression class does not handle casting to a union type.
 */

public class CastExpression extends CastingExpression implements Callable {

    /**
     * Create a cast expression
     *
     * @param source     expression giving the value to be converted
     * @param target     the type to which the value is to be converted
     * @param allowEmpty true if the expression allows an empty sequence as input, producing
     *                   an empty sequence as output. If false, an empty sequence is a type error.
     */

    public CastExpression(Expression source, AtomicType target, boolean allowEmpty) {
        super(source, target, allowEmpty);
    }

    /**
     * Type-check the expression
     */

    /*@NotNull*/
    public Expression typeCheck(ExpressionVisitor visitor, ContextItemStaticInfo contextInfo) throws XPathException {
        typeCheckChildren(visitor, contextInfo);
        SequenceType atomicType = SequenceType.makeSequenceType(BuiltInAtomicType.ANY_ATOMIC, getCardinality());

        final TypeHierarchy th = getConfiguration().getTypeHierarchy();
        RoleDiagnostic role = new RoleDiagnostic(RoleDiagnostic.TYPE_OP, "cast as", 0);
        ItemType sourceItemType = null;

        Expression operand = TypeChecker.staticTypeCheck(getBaseExpression(), atomicType, false, role, visitor);
        setBaseExpression(operand);
        sourceItemType = operand.getItemType();


        if (sourceItemType instanceof ErrorType) {
            if (allowsEmpty()) {
                return Literal.makeEmptySequence();
            } else {
                XPathException err = new XPathException("Cast does not allow an empty sequence as input");
                err.setErrorCode("XPTY0004");
                err.setLocation(getLocation());
                err.setIsTypeError(true);
                throw err;
            }
        }
        boolean isXPath30 = visitor.getStaticContext().getXPathVersion() >= 30;

        PlainType sourceType = (PlainType) sourceItemType;
        int r = th.relationship(sourceType, getTargetType());
        if (r == TypeHierarchy.SAME_TYPE) {
            return operand;
        } else if (r == TypeHierarchy.SUBSUMED_BY) {
            // It's generally true that any expression defined to return an X is allowed to return a subtype of X.
            // However, people seem to get upset if we treat the cast as a no-op.
            converter = new Converter.UpCastingConverter(getTargetType());
        } else {

            ConversionRules rules = getConfiguration().getConversionRules();

            if (sourceType.isAtomicType() && sourceType != BuiltInAtomicType.ANY_ATOMIC) {
                converter = rules.getConverter((AtomicType)sourceType, getTargetType());
                if (converter == null) {
                    XPathException err = new XPathException("Casting from " + sourceType + " to " + getTargetType() +
                            " can never succeed");
                    err.setErrorCode("XPTY0004");
                    err.setLocation(getLocation());
                    err.setIsTypeError(true);
                    throw err;
                } else {
                    if (getTargetType().isNamespaceSensitive()) {
                        converter = converter.setNamespaceResolver(getRetainedStaticContext());
                    }
                }
                if (converter.isXPath30Conversion() && !isXPath30
                        && !(operand instanceof Literal && getTargetType().getPrimitiveType() == StandardNames.XS_QNAME)) {
                    XPathException err = new XPathException("Casting from " + sourceType + " to " + getTargetType() +
                            " requires XPath 3.0 functionality to be enabled");
                    err.setErrorCode("XPTY0004");
                    err.setLocation(getLocation());
                    err.setIsTypeError(true);
                    throw err;
                }
            }
        }

        if (operand instanceof Literal) {
            return preEvaluate(isXPath30);
        }

        return this;
    }

    /**
     * Perform optimisation of an expression and its subexpressions.
     * <p/>
     * <p>This method is called after all references to functions and variables have been resolved
     * to the declaration of the function or variable, and after all type checking has been done.</p>
     *
     * @param visitor         an expression visitor
     * @param contextInfo the static type of "." at the point where this expression is invoked.
     *                        The parameter is set to null if it is known statically that the context item will be undefined.
     *                        If the type of the context item is not known statically, the argument is set to
     *                        {@link net.sf.saxon.type.Type#ITEM_TYPE}
     * @return the original expression, rewritten if appropriate to optimize execution
     * @throws net.sf.saxon.trans.XPathException
     *          if an error is discovered during this phase
     *          (typically a type error)
     */

    /*@NotNull*/
    public Expression optimize(ExpressionVisitor visitor, ContextItemStaticInfo contextInfo) throws XPathException {
        final TypeHierarchy th = getConfiguration().getTypeHierarchy();
        Expression e2 = super.optimize(visitor, contextInfo);
        if (e2 != this) {
            return e2;
        }
        // Eliminate pointless casting between untypedAtomic and string
        Expression operand = getBaseExpression();
        if (getTargetType() == BuiltInAtomicType.UNTYPED_ATOMIC) {
            if (operand.isCallOn(String_1.class)) {
                Expression e = ((SystemFunctionCall) operand).getArg(0);
                if (e.getItemType() instanceof AtomicType && e.getCardinality() == StaticProperty.EXACTLY_ONE) {
                    operand = e;
                }
            } else if (operand instanceof CastExpression) {
                if (((CastExpression) operand).getTargetType() == BuiltInAtomicType.UNTYPED_ATOMIC) {
                    return operand;
                } else if (((CastExpression) operand).getTargetType() == BuiltInAtomicType.STRING) {
                    ((CastExpression) operand).setTargetType(BuiltInAtomicType.UNTYPED_ATOMIC);
                    return operand;
                }
            } else if (operand instanceof AtomicSequenceConverter) {
                if (operand.getItemType() == BuiltInAtomicType.UNTYPED_ATOMIC) {
                    return operand;
                } else if (operand.getItemType() == BuiltInAtomicType.STRING) {
                    AtomicSequenceConverter old = (AtomicSequenceConverter) operand;
                    AtomicSequenceConverter asc = new AtomicSequenceConverter(
                            old.getBaseExpression(),
                            BuiltInAtomicType.UNTYPED_ATOMIC
                    );
                    return asc.typeCheck(visitor, contextInfo)
                            .optimize(visitor, contextInfo);
                }
            }
        }
        // avoid converting anything to a string and then back again
        if (operand.isCallOn(String_1.class)) {
            Expression e = ((SystemFunctionCall) operand).getArg(0);
            ItemType et = e.getItemType();
            if (et instanceof AtomicType &&
                    e.getCardinality() == StaticProperty.EXACTLY_ONE &&
                    th.isSubType(et, getTargetType())) {
                return e;
            }
        }
        // avoid converting anything to untypedAtomic and then back again
        if (operand instanceof CastExpression) {
            ItemType it = ((CastExpression) operand).getTargetType();
            if (th.isSubType(it, BuiltInAtomicType.STRING) || th.isSubType(it, BuiltInAtomicType.UNTYPED_ATOMIC)) {
                Expression e = ((CastExpression) operand).getBaseExpression();
                ItemType et = e.getItemType();
                if (et instanceof AtomicType &&
                        e.getCardinality() == StaticProperty.EXACTLY_ONE &&
                        th.isSubType(et, getTargetType())) {
                    return e;
                }
            }
        }
        if (operand instanceof AtomicSequenceConverter) {
            ItemType it = operand.getItemType();
            if (th.isSubType(it, BuiltInAtomicType.STRING) || th.isSubType(it, BuiltInAtomicType.UNTYPED_ATOMIC)) {
                Expression e = ((AtomicSequenceConverter) operand).getBaseExpression();
                ItemType et = e.getItemType();
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
            return preEvaluate(visitor.getStaticContext().getXPathVersion() >= 30);
        }
        return this;
    }

    /**
     * Perform early (compile-time) evaluation
     */

    protected Expression preEvaluate(boolean isXPath30) throws XPathException {
        GroundedValue literalOperand = ((Literal) getBaseExpression()).getValue();
        if (literalOperand instanceof AtomicValue && converter != null) {
            if (converter.isXPath30Conversion() && !isXPath30 &&
                    !(converter instanceof StringConverter.StringToQName && isOperandIsStringLiteral())) {
                XPathException e = new XPathException("Casting from " + ((AtomicValue) literalOperand).getPrimitiveType() + " to " + getTargetType() + " requires XPath 3.0 to be enabled");
                e.setLocation(getLocation());
                e.setErrorCode("XPTY0004");
                throw e;
            }
            ConversionResult result = converter.convert((AtomicValue) literalOperand);
            if (result instanceof ValidationFailure) {
                ValidationFailure err = (ValidationFailure) result;
                String code = err.getErrorCode();
                if (code == null) {
                    code = "FORG0001";
                }
                throw new XPathException(err.getMessage(), code, this.getLocation());
            } else {
                return Literal.makeLiteral((AtomicValue) result);
            }
        }
        if (literalOperand.getLength() == 0) {
            if (allowsEmpty()) {
                return getBaseExpression();
            } else {
                XPathException err = new XPathException("Cast can never succeed: the operand must not be an empty sequence", "XPTY0004", this.getLocation());
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
        return allowsEmpty() && Cardinality.allowsZero(getBaseExpression().getCardinality())
                ? StaticProperty.ALLOWS_ZERO_OR_ONE : StaticProperty.EXACTLY_ONE;
    }

    /**
     * Get the static type of the expression
     * <p/>
     * <p/>
     * /*@NotNull
     */
    public ItemType getItemType() {
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
    /*@Nullable*/
    @Override
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
        ExpressionTool.copyLocationInfo(this, c2);
        c2.converter = converter;
        c2.setRetainedStaticContext(getRetainedStaticContext());
        c2.setOperandIsStringLiteral(isOperandIsStringLiteral());
        return c2;
    }

    /**
     * An implementation of Expression must provide at least one of the methods evaluateItem(), iterate(), or process().
     * This method indicates which of these methods is provided directly. The other methods will always be available
     * indirectly, using an implementation that relies on one of the other methods.
     *
     * @return the implementation method, for example {@link #ITERATE_METHOD} or {@link #EVALUATE_METHOD} or
     * {@link #PROCESS_METHOD}
     */
    @Override
    public int getImplementationMethod() {
        return EVALUATE_METHOD;
    }

    public Sequence call(XPathContext context, Sequence[] arguments) throws XPathException {
        AtomicValue result = doCast((AtomicValue)arguments[0].head(), context);
        return result == null ? EmptySequence.getInstance() : result;
    }

    private AtomicValue doCast(AtomicValue value, XPathContext context) throws XPathException {
        if (value == null) {
            if (allowsEmpty()) {
                return null;
            } else {
                XPathException e = new XPathException("Cast does not allow an empty sequence");
                e.setXPathContext(context);
                e.setLocation(getLocation());
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
                e.setLocation(getLocation());
                e.setErrorCode("XPTY0004");
                throw e;
            }
            if (converter.isXPath30Conversion() && getRetainedStaticContext().getXPathVersion() < 30 &&
                    !(converter instanceof StringConverter.StringToQName && isOperandIsStringLiteral())) {
                XPathException e = new XPathException("Casting from " + value.getPrimitiveType() + " to " + getTargetType() + " requires XPath 3.0 to be enabled");
                e.setXPathContext(context);
                e.setLocation(getLocation());
                e.setErrorCode("XPTY0004");
                throw e;
            }
            if (getTargetType().isNamespaceSensitive()) {
                converter = converter.setNamespaceResolver(getRetainedStaticContext());
            }
        }
        ConversionResult result = converter.convert(value);
        if (result instanceof ValidationFailure) {
            ValidationFailure err = (ValidationFailure) result;
            XPathException xe = err.makeException();
            xe.maybeSetErrorCode("FORG0001");
            xe.maybeSetLocation(getLocation());
            throw xe;
        }
        return (AtomicValue) result;
    }

    /**
     * Evaluate the expression
     */

    /*@Nullable*/
    public AtomicValue evaluateItem(XPathContext context) throws XPathException {
        try {
            AtomicValue value = (AtomicValue) getBaseExpression().evaluateItem(context);
            return doCast(value, context);
        } catch (ClassCastException e) {
            e.printStackTrace();
            throw e;
        }
    }

    /**
     * Is this expression the same as another expression?
     */

    public boolean equals(Object other) {
        return other instanceof CastExpression &&
                getBaseExpression().equals(((CastExpression) other).getBaseExpression()) &&
                getTargetType() == ((CastExpression) other).getTargetType() &&
                getTargetType() == ((CastExpression) other).getTargetType();
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
     * Represent the expression as a string. The resulting string will be a valid XPath 3.0 expression
     * with no dependencies on namespace bindings.
     *
     * @return the expression as a string in XPath 3.0 syntax
     */

    public String toString() {
        return getTargetType().getEQName() + "(" + getBaseExpression().toString() + ")";
    }

    @Override
    public String toShortString() {
        return getTargetType().getDisplayName() + "(" + getBaseExpression().toShortString() + ")";
    }

    /**
     * Diagnostic print of expression structure. The abstract expression tree
     * is written to the supplied output destination.
     */

    public void export(ExpressionPresenter out) throws XPathException {
        out.startElement("cast", this);
        out.emitAttribute("as", getTargetType().getTypeName());
        out.emitAttribute("emptiable", allowsEmpty() ? "1" : "0");
        getBaseExpression().export(out);
        out.endElement();
    }


//#ifdefined BYTECODE

    /**
     * Return the compiler of the Cast expression
     *
     * @return the relevant ExpressionCompiler
     */
    @Override
    public ExpressionCompiler getExpressionCompiler() {
        return new CastExpressionCompiler();
    }
//#endif


}

