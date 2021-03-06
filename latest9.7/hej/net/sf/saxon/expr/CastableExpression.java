////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
// Copyright (c) 2015 Saxonica Limited.
// This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
// If a copy of the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
// This Source Code Form is "Incompatible With Secondary Licenses", as defined by the Mozilla Public License, v. 2.0.
////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

package net.sf.saxon.expr;

import com.saxonica.ee.bytecode.CastableExpressionCompiler;
import com.saxonica.ee.bytecode.ExpressionCompiler;
import net.sf.saxon.expr.parser.ContextItemStaticInfo;
import net.sf.saxon.expr.parser.ExpressionTool;
import net.sf.saxon.expr.parser.ExpressionVisitor;
import net.sf.saxon.expr.parser.RebindingMap;
import net.sf.saxon.om.*;
import net.sf.saxon.trace.ExpressionPresenter;
import net.sf.saxon.trans.XPathException;
import net.sf.saxon.type.*;
import net.sf.saxon.value.AtomicValue;
import net.sf.saxon.value.BooleanValue;
import net.sf.saxon.value.Cardinality;

/**
 * Castable Expression: implements "Expr castable as atomic-type?".
 * The implementation simply wraps a cast expression with a try/catch.
 */

public final class CastableExpression extends CastingExpression {

    /**
     * Create a "castable" expression of the form "source castable as target"
     *
     * @param source     The source expression
     * @param target     The type being tested against
     * @param allowEmpty true if an empty sequence is acceptable, that is if the expression
     *                   was written as "source castable as target?"
     */

    public CastableExpression(Expression source, AtomicType target, boolean allowEmpty) {
        super(source, target, allowEmpty);
    }

    /**
     * Type-check the expression
     */

    /*@NotNull*/
    public Expression typeCheck(ExpressionVisitor visitor, ContextItemStaticInfo contextInfo) throws XPathException {
        typeCheckChildren(visitor, contextInfo);

        // We need to take care here. The usual strategy of wrapping the operand in an expression that
        // does type-checking doesn't work here, because an error in the type checking should be caught,
        // while an error in evaluating the expression as written should not.

        Expression operand = getBaseExpression();
        ItemType sourceItemType = operand.getItemType();

        AtomicType atomizedType = (AtomicType) sourceItemType.getAtomizedItemType().getPrimitiveItemType();
        if (!(atomizedType == BuiltInAtomicType.ANY_ATOMIC)) {
            converter = getConfiguration().getConversionRules().getConverter(atomizedType, getTargetType());
            if (converter == null) {
                if (!allowsEmpty() || !Cardinality.allowsZero(operand.getCardinality())) {
                    // Conversion from source to target type will never succeed
                    return Literal.makeLiteral(BooleanValue.FALSE);
                }
            } else {
                if (getTargetPrimitiveType().isNamespaceSensitive()) {
                    converter = converter.setNamespaceResolver(getRetainedStaticContext());
                }
                if (converter.isAlwaysSuccessful() && !allowsEmpty() && operand.getCardinality() == StaticProperty.ALLOWS_ONE) {
                    return Literal.makeLiteral(BooleanValue.TRUE);
                }
            }
        }

        setBaseExpression(operand);
        if (operand instanceof Literal) {
            return preEvaluate();
        }

        return this;
    }


    protected Expression preEvaluate() throws XPathException {
        GroundedValue literalOperand = ((Literal) getBaseExpression()).getValue();
        if (literalOperand instanceof AtomicValue && converter != null) {
            if (converter.isXPath30Conversion() && getRetainedStaticContext().getXPathVersion() < 30 &&
                    !(converter instanceof StringConverter.StringToQName && isOperandIsStringLiteral())) {
                return Literal.makeLiteral(BooleanValue.FALSE);
            }
            ConversionResult result = converter.convert((AtomicValue) literalOperand);
            return Literal.makeLiteral(BooleanValue.get(!(result instanceof ValidationFailure)));
        }
        final int length = literalOperand.getLength();
        if (length == 0) {
            return Literal.makeLiteral(BooleanValue.get(allowsEmpty()));
        }
        if (length > 1) {
            return Literal.makeLiteral(BooleanValue.FALSE);
        }
        return this;
    }

    /**
     * Optimize the expression
     */

    /*@NotNull*/
    public Expression optimize(ExpressionVisitor visitor, ContextItemStaticInfo contextInfo) throws XPathException {
        optimizeChildren(visitor, contextInfo);
        if (getBaseExpression() instanceof Literal) {
            return preEvaluate();
        }
        return this;
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

    /**
     * Is this expression the same as another expression?
     */

    public boolean equals(Object other) {
        return other instanceof CastableExpression &&
                getBaseExpression().equals(((CastableExpression) other).getBaseExpression()) &&
                getTargetType() == ((CastableExpression) other).getTargetType() &&
                allowsEmpty() == ((CastableExpression) other).allowsEmpty();
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
     * <p/>
     * <p/>
     * /*@NotNull
     */
    public ItemType getItemType() {
        return BuiltInAtomicType.BOOLEAN;
    }

    public int computeCardinality() {
        return StaticProperty.EXACTLY_ONE;
    }

    /*@NotNull*/
    public Expression copy(RebindingMap rebindings) {
        CastableExpression ce = new CastableExpression(getBaseExpression().copy(rebindings), getTargetType(), allowsEmpty());
        ExpressionTool.copyLocationInfo(this, ce);
        ce.setRetainedStaticContext(getRetainedStaticContext());
        ce.converter = converter;
        return ce;
    }

    /**
     * Evaluate the expression
     */

    public BooleanValue evaluateItem(XPathContext context) throws XPathException {
        return BooleanValue.get(effectiveBooleanValue(context));
    }

    public boolean effectiveBooleanValue(XPathContext context) throws XPathException {
        // This method does its own atomization so that it can distinguish between atomization
        // failures and casting failures
        int count = 0;
        SequenceIterator iter = getBaseExpression().iterate(context);
        Item item;
        while ((item = iter.next()) != null) {
            if (item instanceof NodeInfo) {
                AtomicSequence atomizedValue = item.atomize();
                int length = SequenceTool.getLength(atomizedValue);
                count += length;
                if (count > 1) {
                    return false;
                }
                if (length != 0) {
                    AtomicValue av = atomizedValue.head();
                    if (!isCastable(av, getTargetType(), context)) {
                        return false;
                    }
                }
            } else if (item instanceof AtomicValue) {
                AtomicValue av = (AtomicValue) item;
                count++;
                if (count > 1) {
                    return false;
                }
                if (!isCastable(av, getTargetType(), context)) {
                    return false;
                }
            } else {
                throw new XPathException("Input to cast cannot be atomized", "XPTY0004");
            }
        }
        return count != 0 || allowsEmpty();
    }

    /**
     * Determine whether a value is castable to a given type
     *
     * @param value      the value to be tested
     * @param targetType the type to be tested against
     * @param context    XPath dynamic context
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
            if (converter.isXPath30Conversion() && getRetainedStaticContext().getXPathVersion() < 30 &&
                    !(converter instanceof StringConverter.StringToQName && isOperandIsStringLiteral())) {
                return false;
            }
            if (getTargetType().isNamespaceSensitive()) {
                converter = converter.setNamespaceResolver(getRetainedStaticContext());
            }
        }
        return !(converter.convert(value) instanceof ValidationFailure);
    }

//#ifdefined BYTECODE

    /**
     * Return the compiler of the Castable expression
     *
     * @return the relevant ExpressionCompiler
     */
    @Override
    public ExpressionCompiler getExpressionCompiler() {
        return new CastableExpressionCompiler();
    }
//#endif


    /**
     * The toString() method for an expression attempts to give a representation of the expression
     * in an XPath-like form, but there is no guarantee that the syntax will actually be true XPath.
     * In the case of XSLT instructions, the toString() method gives an abstracted view of the syntax
     */

    public String toString() {
        return getBaseExpression().toString() + " castable as " + getTargetType().getEQName();
    }


    /**
     * Diagnostic print of expression structure. The abstract expression tree
     * is written to the supplied output destination.
     */

    public void export(ExpressionPresenter out) throws XPathException {
        out.startElement("castable", this);
        out.emitAttribute("as", getTargetType().getTypeName());
        out.emitAttribute("emptiable", allowsEmpty() ? "1" : "0");
        getBaseExpression().export(out);
        out.endElement();
    }

}

