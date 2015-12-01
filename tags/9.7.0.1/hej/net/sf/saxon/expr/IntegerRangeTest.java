////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
// Copyright (c) 2015 Saxonica Limited.
// This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
// If a copy of the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
// This Source Code Form is "Incompatible With Secondary Licenses", as defined by the Mozilla Public License, v. 2.0.
////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

package net.sf.saxon.expr;

import com.saxonica.ee.bytecode.ExpressionCompiler;
import com.saxonica.ee.bytecode.IntegerRangeTestCompiler;
import net.sf.saxon.expr.parser.ContextItemStaticInfo;
import net.sf.saxon.expr.parser.ExpressionTool;
import net.sf.saxon.expr.parser.ExpressionVisitor;
import net.sf.saxon.om.SequenceIterator;
import net.sf.saxon.trace.ExpressionPresenter;
import net.sf.saxon.trans.XPathException;
import net.sf.saxon.type.*;
import net.sf.saxon.value.*;

/**
 * An IntegerRangeTest is an expression of the form
 * E = N to M
 * where N and M are both expressions of type integer.
 * The result of the expression is true if any value in the result of E is a numeric value in the
 * range N to M. Any untyped atomic values in E are converted to integers; values that cannot be converted
 * result in an error.
 */

public class IntegerRangeTest extends Expression {

    private Operand valueOp;
    private Operand minOp;
    private Operand maxOp;

    /**
     * Construct a IntegerRangeTest
     *
     * @param value the integer value to be tested to see if it is in the range min to max inclusive
     * @param min   the lowest permitted value
     * @param max   the highest permitted value
     */

    public IntegerRangeTest(Expression value, Expression min, Expression max) {
        valueOp = new Operand(this, value, OperandRole.ATOMIC_SEQUENCE);
        minOp = new Operand(this, min, OperandRole.SINGLE_ATOMIC);
        maxOp = new Operand(this, max, OperandRole.SINGLE_ATOMIC);
    }

     @Override
     public Iterable<Operand> operands() {
         return operandList(valueOp, minOp, maxOp);
     }

    public Expression getValue() {
        return valueOp.getChildExpression();
    }

    public void setValue(Expression value) {
        valueOp.setChildExpression(value);
    }

    public Expression getMin() {
        return minOp.getChildExpression();
    }

    public void setMin(Expression min) {
        minOp.setChildExpression(min);
    }

    public Expression getMax() {
        return maxOp.getChildExpression();
    }

    public void setMax(Expression max) {
        maxOp.setChildExpression(max);
    }

    /**
     * Type-check the expression
     */

    /*@NotNull*/
    public Expression typeCheck(ExpressionVisitor visitor, ContextItemStaticInfo contextInfo) throws XPathException {
        // Already done, we only get one of these expressions after the operands have been analyzed
        return this;
    }

    /**
     * Perform optimisation of an expression and its subexpressions.
     * <p/>
     * <p>This method is called after all references to functions and variables have been resolved
     * to the declaration of the function or variable, and after all type checking has been done.</p>
     *
     * @param visitor         an expression visitor
     * @param contextItemType the static type of "." at the point where this expression is invoked.
     *                        The parameter is set to null if it is known statically that the context item will be undefined.
     *                        If the type of the context item is not known statically, the argument is set to
     *                        {@link net.sf.saxon.type.Type#ITEM_TYPE}
     * @return the original expression, rewritten if appropriate to optimize execution
     * @throws XPathException if an error is discovered during this phase
     *                        (typically a type error)
     */

    /*@NotNull*/
    public Expression optimize(ExpressionVisitor visitor, ContextItemStaticInfo contextItemType) throws XPathException {
        return this;
    }

    /**
     * Get the data type of the items returned
     */

    /*@NotNull*/
    public ItemType getItemType() {
        return BuiltInAtomicType.BOOLEAN;
    }

    /**
     * Determine the static cardinality
     */

    public int computeCardinality() {
        return StaticProperty.EXACTLY_ONE;
    }

    /**
     * Copy an expression. This makes a deep copy.
     *
     * @return the copy of the original expression
     */

    /*@NotNull*/
    public Expression copy() {
        IntegerRangeTest exp = new IntegerRangeTest(getValue().copy(), getMin().copy(), getMax().copy());
        ExpressionTool.copyLocationInfo(this, exp);
        return exp;
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
     *
     * @param other the expression to be compared with this one
     * @return true if the two expressions are statically equivalent
     */
    @Override
    public boolean equals(Object other) {
        return other instanceof IntegerRangeTest && ((IntegerRangeTest) other).getValue().equals(getValue())
                && ((IntegerRangeTest) other).getMin().equals(getMin())
                && ((IntegerRangeTest) other).getMax().equals(getMax());
    }

    /**
     * Hashcode supporting equals()
     */

    public int hashCode() {
        int h = getValue().hashCode() + 77;
        h ^= getMin().hashCode() ^ getMax().hashCode();
        return h;
    }

    /**
     * Evaluate the expression
     */

    public BooleanValue evaluateItem(XPathContext c) throws XPathException {
        IntegerValue minVal = null;
        IntegerValue maxVal = null;
        StringConverter toDouble = null;
        SequenceIterator iter = getValue().iterate(c);
        AtomicValue atom;
        while ((atom = (AtomicValue) iter.next()) != null) {
            if (minVal == null) {
                minVal = (IntegerValue) getMin().evaluateItem(c);
                maxVal = (IntegerValue) getMax().evaluateItem(c);
            }
            NumericValue v;
            if (atom instanceof UntypedAtomicValue) {
                if (toDouble == null) {
                    toDouble = BuiltInAtomicType.DOUBLE.getStringConverter(c.getConfiguration().getConversionRules());
                }
                ConversionResult result = toDouble.convertString(atom.getStringValueCS());
                if (result instanceof ValidationFailure) {
                    XPathException e = new XPathException("Failed to convert untypedAtomic value {" +
                            atom.getStringValueCS() + "}  to xs:integer", "FORG0001");
                    e.setLocation(getLocation());
                    throw e;
                } else {
                    v = (DoubleValue) result.asAtomic();
                }
            } else if (atom instanceof NumericValue) {
                v = (NumericValue) atom;
            } else {
                XPathException e = new XPathException("Cannot compare value of type " +
                        atom.getUType() + " to xs:integer", "XPTY0004");
                e.setIsTypeError(true);
                e.setLocation(getLocation());
                throw e;
            }
            if (v.isWholeNumber() && v.compareTo(minVal) >= 0 && v.compareTo(maxVal) <= 0) {
                return BooleanValue.TRUE;
            }
        }
        return BooleanValue.FALSE;
    }

//#ifdefined BYTECODE

    /**
     * Return the compiler of the IntegerRangeTest expression
     *
     * @return the relevant ExpressionCompiler
     */
    @Override
    public ExpressionCompiler getExpressionCompiler() {
        return new IntegerRangeTestCompiler();
    }
//#endif

    /**
     * Diagnostic print of expression structure. The abstract expression tree
     * is written to the supplied output destination.
     */

    public void export(ExpressionPresenter destination) {
        destination.startElement("intRangeTest", this);
        getValue().export(destination);
        getMin().export(destination);
        getMax().export(destination);
        destination.endElement();
    }

    /**
     * <p>The toString() method for an expression attempts to give a representation of the expression
     * in an XPath-like form.</p>
     * <p/>
     * <p>For subclasses of Expression that represent XPath expressions, the result should always be a string that
     * parses as an XPath 3.0 expression. </p>
     * <p/>
     *
     * @return a representation of the expression as a string
     */
    @Override
    public String toString() {
        return ExpressionTool.parenthesize(getValue()) + " = (" +
                ExpressionTool.parenthesize(getMin()) + " to " +
                ExpressionTool.parenthesize(getMax()) + ")";
    }


}
