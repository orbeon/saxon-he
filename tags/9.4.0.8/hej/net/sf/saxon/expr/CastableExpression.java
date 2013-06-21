package net.sf.saxon.expr;

import net.sf.saxon.expr.parser.ExpressionVisitor;
import net.sf.saxon.om.Item;
import net.sf.saxon.om.NamePool;
import net.sf.saxon.om.NodeInfo;
import net.sf.saxon.om.SequenceIterator;
import net.sf.saxon.pattern.EmptySequenceTest;
import net.sf.saxon.trace.ExpressionPresenter;
import net.sf.saxon.trans.XPathException;
import net.sf.saxon.type.*;
import net.sf.saxon.value.*;

/**
* Castable Expression: implements "Expr castable as atomic-type?".
* The implementation simply wraps a cast expression with a try/catch.
*/

public final class CastableExpression extends CastingExpression {

    /**
     * Create a "castable" expression of the form "source castable as target"
     * @param source The source expression
     * @param target The type being tested against
     * @param allowEmpty true if an empty sequence is acceptable, that is if the expression
     * was written as "source castable as target?"
     */

    public CastableExpression(Expression source, AtomicType target, boolean allowEmpty) {
        super(source, target, allowEmpty);
    }

    /**
    * Type-check the expression
    */

    /*@NotNull*/
    public Expression typeCheck(ExpressionVisitor visitor, ExpressionVisitor.ContextItemType contextItemType) throws XPathException {
        operand = visitor.typeCheck(operand, contextItemType);

        // We need to take care here. The usual strategy of wrapping the operand in an expression that
        // does type-checking doesn't work here, because an error in the type checking should be caught,
        // while an error in evaluating the expression as written should not.

        final TypeHierarchy th = visitor.getConfiguration().getTypeHierarchy();
        ItemType sourceItemType = operand.getItemType(th);
        if (sourceItemType instanceof EmptySequenceTest) {
            return Literal.makeLiteral(BooleanValue.get(allowsEmpty()));
        }
        AtomicType atomizedType = (AtomicType)sourceItemType.getAtomizedItemType().getPrimitiveItemType();
        if (!(atomizedType == BuiltInAtomicType.ANY_ATOMIC)) {
            converter = visitor.getConfiguration().getConversionRules().getConverter(atomizedType, getTargetType());
            if (converter == null) {
                if (!allowsEmpty() || !Cardinality.allowsZero(operand.getCardinality())) {
                    // Conversion from source to target type will never succeed
                    return Literal.makeLiteral(BooleanValue.FALSE);
                }
            } else {
                if (getTargetPrimitiveType().isNamespaceSensitive()) {
                    converter.setNamespaceResolver(nsResolver);
                }
                if (converter.isAlwaysSuccessful() && !allowsEmpty() && operand.getCardinality() == StaticProperty.ALLOWS_ONE) {
                    return Literal.makeLiteral(BooleanValue.TRUE);
                }
            }
        }
        
        if (operand instanceof Literal) {
            return preEvaluate();
        }
        
        return this;
    }


    protected Expression preEvaluate() throws XPathException {
        Value literalOperand = ((Literal)operand).getValue();
        if (literalOperand instanceof AtomicValue && converter != null) {
            if (converter.isXPath30Conversion() && !getExecutable().isAllowXPath30() &&
                    !(converter instanceof StringConverter.StringToQName && isOperandIsStringLiteral())) {
                return Literal.makeLiteral(BooleanValue.FALSE);
            }
            ConversionResult result = converter.convert((AtomicValue)literalOperand);
            return Literal.makeLiteral(BooleanValue.get(!(result instanceof ValidationFailure)));
        }
        if (literalOperand instanceof EmptySequence) {
            return Literal.makeLiteral(BooleanValue.get(allowsEmpty()));
        }
        if (literalOperand.getLength() > 1) {
            return Literal.makeLiteral(BooleanValue.FALSE);
        }
        return this;
    }

    /**
    * Optimize the expression
    */

    /*@NotNull*/
    public Expression optimize(ExpressionVisitor visitor, ExpressionVisitor.ContextItemType contextItemType) throws XPathException {
        operand = visitor.optimize(operand, contextItemType);
        if (operand instanceof Literal) {
            return preEvaluate();
        }
        return this;
    }

    /**
     * Is this expression the same as another expression?
     */

    public boolean equals(Object other) {
        return other instanceof CastableExpression &&
                operand.equals(((CastableExpression)other).operand) &&
                getTargetType() == ((CastableExpression)other).getTargetType() &&
                allowsEmpty() == ((CastableExpression)other).allowsEmpty();
    }

    /**
     * get HashCode for comparing two expressions. Note that this hashcode gives the same
     * result for (A op B) and for (B op A), whether or not the operator is commutative.
     */

    @Override
    public int hashCode() {
        return super.hashCode() ^ 0x5555;
    }

    /**
     * Determine the data type of the result of the Castable expression
     * @param th the type hierarchy cache
     */

    /*@NotNull*/
    public ItemType getItemType(TypeHierarchy th) {
        return BuiltInAtomicType.BOOLEAN;
    }

    public int computeCardinality() {
        return StaticProperty.EXACTLY_ONE;
    }

    /*@NotNull*/
    public Expression copy() {
        CastableExpression ce = new CastableExpression(getBaseExpression().copy(), getTargetType(), allowsEmpty());
        ce.nsResolver = nsResolver;
        ce.converter = converter;
        return ce;
    }

    /**
    * Evaluate the expression
    */

    public Item evaluateItem(XPathContext context) throws XPathException {
        return BooleanValue.get(effectiveBooleanValue(context));
    }

    public boolean effectiveBooleanValue(XPathContext context) throws XPathException {
        // This method does its own atomization so that it can distinguish between atomization
        // failures and casting failures
        int count = 0;
        SequenceIterator iter = operand.iterate(context);
        while (true) {
            Item item = iter.next();
            if (item == null) {
                break;
            }
            if (item instanceof NodeInfo) {
                Value atomizedValue = ((NodeInfo)item).atomize();
                int length = atomizedValue.getLength();
                count += length;
                if (count > 1) {
                    return false;
                }
                if (length != 0) {
                    AtomicValue av = (AtomicValue)atomizedValue.itemAt(0);
                    if (!isCastable(av, getTargetType(), context)) {
                        return false;
                    }
                }
            } else {
                AtomicValue av = (AtomicValue)item;
                count++;
                if (count > 1) {
                    return false;
                }
                if (!isCastable(av, getTargetType(), context)) {
                    return false;
                }
            }
        }
        return count != 0 || allowsEmpty();
    }

    /**
     * Determine whether a value is castable to a given type
     * @param value the value to be tested
     * @param targetType the type to be tested against
     * @param context XPath dynamic context
     * @return true if the value is castable to the required type
     */

    private boolean isCastable(AtomicValue value, AtomicType targetType, XPathContext context) {
        Converter converter = this.converter;
        if (converter == null) {
            converter = context.getConfiguration().getConversionRules().getConverter(value.getPrimitiveType(), targetType);
            if (converter == null) {
                return false;
            }
            if (converter.isAlwaysSuccessful()) {
                return true;
            }
            if (converter.isXPath30Conversion() && !getExecutable().isAllowXPath30() &&
                    !(converter instanceof StringConverter.StringToQName && isOperandIsStringLiteral())) {
                return false;
            }
            if (nsResolver != null) {
                converter.setNamespaceResolver(nsResolver);
            }
        }
        return !(converter.convert(value) instanceof ValidationFailure);
    }


    /**
     * The toString() method for an expression attempts to give a representation of the expression
     * in an XPath-like form, but there is no guarantee that the syntax will actually be true XPath.
     * In the case of XSLT instructions, the toString() method gives an abstracted view of the syntax
     */

    public String toString() {
        try {
            NamePool pool = getExecutable().getConfiguration().getNamePool();
            return operand.toString() + " castable as " + getTargetType().toString(pool);
        } catch (Exception err) {
            return operand.toString() + " castable as " + getTargetType().toString();
        }
    }



    /**
     * Diagnostic print of expression structure. The abstract expression tree
     * is written to the supplied output destination.
     */

    public void explain(ExpressionPresenter out) {
        out.startElement("castable");
        out.emitAttribute("as", getTargetType().toString(out.getConfiguration().getNamePool()));
        operand.explain(out);
        out.endElement();
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