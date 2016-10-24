////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
// Copyright (c) 2015 Saxonica Limited.
// This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
// If a copy of the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
// This Source Code Form is "Incompatible With Secondary Licenses", as defined by the Mozilla Public License, v. 2.0.
////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

package net.sf.saxon.expr;

import com.saxonica.ee.bytecode.ExpressionCompiler;
import com.saxonica.ee.bytecode.SubscriptExpressionCompiler;
import com.saxonica.ee.stream.adjunct.StreamingAdjunct;
import com.saxonica.ee.stream.adjunct.SubscriptExpressionAdjunct;
import net.sf.saxon.expr.parser.ContextItemStaticInfo;
import net.sf.saxon.expr.parser.ExpressionTool;
import net.sf.saxon.expr.parser.ExpressionVisitor;
import net.sf.saxon.expr.parser.RebindingMap;
import net.sf.saxon.om.GroundedValue;
import net.sf.saxon.om.Item;
import net.sf.saxon.om.SequenceIterator;
import net.sf.saxon.trace.ExpressionPresenter;
import net.sf.saxon.trans.XPathException;
import net.sf.saxon.tree.iter.GroundedIterator;
import net.sf.saxon.value.MemoClosure;
import net.sf.saxon.value.NumericValue;

/**
 * A SubscriptExpression represents a FilterExpression of the form EXPR[n]
 * where n is known to be singleton numeric and to be independent of the focus; it does not need to be constant
 */
public class SubscriptExpression extends SingleItemFilter {

    private Operand subscriptOp;

    /**
     * Construct a SubscriptExpression
     *
     * @param base      the expression to be filtered
     * @param subscript the positional subscript filter
     */

    public SubscriptExpression(Expression base, Expression subscript) {
        super(base);
        subscriptOp = new Operand(this, subscript, OperandRole.SINGLE_ATOMIC);
        //adoptChildExpression(base);
        //adoptChildExpression(subscript);
    }


    public Expression getSubscript() {
        return subscriptOp.getChildExpression();
    }

    public void setSubscript(Expression subscript) {
        subscriptOp.setChildExpression(subscript);
    }

    /**
     * Type-check the expression.
     */
    @Override
    public Expression typeCheck(ExpressionVisitor visitor, ContextItemStaticInfo contextInfo) throws XPathException {
        typeCheckChildren(visitor, contextInfo);
        return this;
    }

    /*@NotNull*/
    public Expression optimize(ExpressionVisitor visitor, ContextItemStaticInfo contextInfo) throws XPathException {
        optimizeChildren(visitor, contextInfo);
        if (Literal.isConstantOne(getSubscript())) {
            return FirstItemExpression.makeFirstItemExpression(getBaseExpression());
        }
        return this;
    }

    /**
     * Copy an expression. This makes a deep copy.
     *
     * @return the copy of the original expression
     * @param rebindings
     */

    /*@NotNull*/
    public Expression copy(RebindingMap rebindings) {
        SubscriptExpression exp = new SubscriptExpression(getBaseExpression().copy(rebindings), getSubscript().copy(rebindings));
        ExpressionTool.copyLocationInfo(this, exp);
        return exp;
    }

    @Override
    public Iterable<Operand> operands() {
        return operandList(getOperand(), subscriptOp);
    }

    /**
     * Get the subscript expression
     *
     * @return the expression used to compute the one-based start offset
     */

    public Expression getSubscriptExpression() {
        return getSubscript();
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
     * Compare two expressions to see if they are equal
     *
     * @param other the other expression
     * @return true if the expressions are equivalent
     */

    public boolean equals(Object other) {
        return other instanceof SubscriptExpression &&
                getBaseExpression().equals(((SubscriptExpression) other).getBaseExpression()) &&
                getSubscript() == ((SubscriptExpression) other).getSubscript();
    }

    public int hashCode() {
        return getBaseExpression().hashCode() ^ getSubscript().hashCode();
    }

    /**
     * Get the static cardinality: this implementation is appropriate for [1] and [last()] which will always
     * return something if the input is non-empty
     */

    public int computeCardinality() {
        return StaticProperty.ALLOWS_ZERO_OR_ONE;
    }


//#ifdefined STREAM

    /**
     * Get a class that supports streamed evaluation of this expression
     *
     * @return the relevant StreamingAdjunct, or null if none is available
     */
    @Override
    protected StreamingAdjunct getStreamingAdjunct() {
        return new SubscriptExpressionAdjunct();
    }


//#endif

    public Item evaluateItem(XPathContext context) throws XPathException {
        NumericValue index = (NumericValue) getSubscript().evaluateItem(context);
        if (index == null) {
            return null;
        }
        if (index.compareTo(Integer.MAX_VALUE) <= 0 && index.isWholeNumber()) {
            int intindex = (int) index.longValue();
            if (intindex < 1) {
                return null;
            }
            Item item;
            SequenceIterator iter = getBaseExpression().iterate(context);
            if (intindex == 1) {
                item = iter.next();
            } else if (iter instanceof MemoClosure.ProgressiveIterator) {
                MemoClosure mem = ((MemoClosure.ProgressiveIterator)iter).getMemoClosure();
                item = mem.itemAt(intindex - 1);
            } else if ((iter.getProperties() & SequenceIterator.GROUNDED) != 0) {
                GroundedValue value = ((GroundedIterator) iter).materialize();
                item = value.itemAt(intindex - 1);
            } else {
                SequenceIterator tail = TailIterator.make(iter, intindex);
                item = tail.next();
                tail.close();
            }
            return item;
        } else {
            // there is no item at the required position
            return null;
        }
    }


//#ifdefined BYTECODE

    /**
     * Return the compiler of the TailExpression expression
     *
     * @return the relevant ExpressionCompiler
     */
    @Override
    public ExpressionCompiler getExpressionCompiler() {
        return new SubscriptExpressionCompiler();
    }
//#endif


    /**
     * Diagnostic print of expression structure. The abstract expression tree
     * is written to the supplied output destination.
     */

    public void export(ExpressionPresenter destination) throws XPathException {
        destination.startElement("subscript", this);
        getBaseExpression().export(destination);
        getSubscript().export(destination);
        destination.endElement();
    }

    /**
     * <p>The toString() method for an expression attempts to give a representation of the expression
     * in an XPath-like form.</p>
     * <p/>
     * <p>For subclasses of Expression that represent XPath expressions, the result should always be a string that
     * parses as an XPath 3.0 expression.</p>
     *
     * @return a representation of the expression as a string
     */
    @Override
    public String toString() {
        return ExpressionTool.parenthesize(getBaseExpression()) + "[" + getSubscript().toString() + "]";
    }

}

