////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
// Copyright (c) 2014 Saxonica Limited.
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
import net.sf.saxon.expr.parser.PromotionOffer;
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

    Expression subscript;

    /**
     * Construct a SubscriptExpression
     *
     * @param base      the expression to be filtered
     * @param subscript the positional subscript filter
     */

    public SubscriptExpression(Expression base, Expression subscript) {
        this.operand = base;
        this.subscript = subscript;
        adoptChildExpression(base);
        adoptChildExpression(subscript);
    }

    /*@NotNull*/
    public Expression typeCheck(ExpressionVisitor visitor, ContextItemStaticInfo contextInfo) throws XPathException {
        operand = visitor.typeCheck(operand, contextInfo);
        subscript = visitor.typeCheck(subscript, contextInfo);
        return this;
    }

    /*@NotNull*/
    public Expression optimize(ExpressionVisitor visitor, ContextItemStaticInfo contextItemType) throws XPathException {
        operand = visitor.optimize(operand, contextItemType);
        subscript = visitor.optimize(subscript, contextItemType);
        if (Literal.isConstantOne(subscript)) {
            return FirstItemExpression.makeFirstItemExpression(operand);
        }
        return this;
    }

    public Expression promote(PromotionOffer offer, Expression parent) throws XPathException {
        Expression exp = offer.accept(parent, this);
        if (exp != null) {
            return exp;
        } else {
            operand = doPromotion(operand, offer);
            subscript = doPromotion(subscript, offer);
            return this;
        }
    }

    /**
     * Copy an expression. This makes a deep copy.
     *
     * @return the copy of the original expression
     */

    /*@NotNull*/
    public Expression copy() {
        return new SubscriptExpression(operand.copy(), subscript.copy());
    }

    @Override
    public Iterable<Operand> operands() {
        return operandList(
                new Operand(operand, OperandRole.SAME_FOCUS_ACTION),
                new Operand(subscript, FilterExpression.FILTER_PREDICATE)
        );
    }

    /**
     * Replace one subexpression by a replacement subexpression
     *
     * @param original    the original subexpression
     * @param replacement the replacement subexpression
     * @return true if the original subexpression is found
     */

    public boolean replaceOperand(Expression original, Expression replacement) {
        boolean found = false;
        if (operand == original) {
            operand = replacement;
            found = true;
        } else if (subscript == original) {
            subscript = replacement;
            found = true;
        }
        return found;
    }

    /**
     * Get the subscript expression
     *
     * @return the expression used to compute the one-based start offset
     */

    public Expression getSubscriptExpression() {
        return subscript;
    }

    /**
     * Compare two expressions to see if they are equal
     *
     * @param other the other expression
     * @return true if the expressions are equivalent
     */

    public boolean equals(Object other) {
        return other instanceof SubscriptExpression &&
                operand.equals(((SubscriptExpression) other).operand) &&
                subscript == ((SubscriptExpression) other).subscript;
    }

    public int hashCode() {
        return operand.hashCode() ^ subscript.hashCode();
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
        NumericValue index = (NumericValue) subscript.evaluateItem(context);
        if (index == null) {
            return null;
        }
        if (index.compareTo(Integer.MAX_VALUE) <= 0 && index.isWholeNumber()) {
            int intindex = (int) index.longValue();
            if (intindex < 1) {
                return null;
            }
            Item item;
            SequenceIterator iter = operand.iterate(context);
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

    public void explain(ExpressionPresenter destination) {
        destination.startElement("subscript");
        operand.explain(destination);
        subscript.explain(destination);
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
        return ExpressionTool.parenthesize(operand) + "[" + subscript.toString() + "]";
    }
}

