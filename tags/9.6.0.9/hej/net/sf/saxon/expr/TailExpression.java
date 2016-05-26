////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
// Copyright (c) 2014 Saxonica Limited.
// This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
// If a copy of the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
// This Source Code Form is "Incompatible With Secondary Licenses", as defined by the Mozilla Public License, v. 2.0.
////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

package net.sf.saxon.expr;

import com.saxonica.ee.bytecode.ExpressionCompiler;
import com.saxonica.ee.bytecode.TailExpressionCompiler;
import com.saxonica.ee.stream.adjunct.StreamingAdjunct;
import com.saxonica.ee.stream.adjunct.TailExpressionAdjunct;
import net.sf.saxon.expr.parser.ContextItemStaticInfo;
import net.sf.saxon.expr.parser.ExpressionTool;
import net.sf.saxon.expr.parser.ExpressionVisitor;
import net.sf.saxon.expr.parser.PromotionOffer;
import net.sf.saxon.om.GroundedValue;
import net.sf.saxon.om.Item;
import net.sf.saxon.om.SequenceIterator;
import net.sf.saxon.trace.ExpressionPresenter;
import net.sf.saxon.trans.XPathException;
import net.sf.saxon.type.ItemType;
import net.sf.saxon.value.SequenceExtent;

/**
 * A TailExpression represents a FilterExpression of the form EXPR[position() > n]
 * Here n is usually 2, but we allow other values
 */
public class TailExpression extends Expression {

    /*@Nullable*/ Expression base;
    int start;      // 1-based offset of first item from base expression
    // to be included

    /**
     * Construct a TailExpression, representing a filter expression of the form
     * $base[position() >= $start]
     *
     * @param base  the expression to be filtered
     * @param start the position (1-based) of the first item to be included
     */

    public TailExpression(Expression base, int start) {
        this.base = base;
        this.start = start;
        adoptChildExpression(base);
    }

    /*@NotNull*/
    public Expression typeCheck(ExpressionVisitor visitor, ContextItemStaticInfo contextInfo) throws XPathException {
        base = visitor.typeCheck(base, contextInfo);
        return this;
    }

    /*@NotNull*/
    public Expression optimize(ExpressionVisitor visitor, ContextItemStaticInfo contextItemType) throws XPathException {
        base = visitor.optimize(base, contextItemType);
        if (base instanceof Literal) {
            GroundedValue value =
                    SequenceExtent.makeSequenceExtent(iterate(visitor.getStaticContext().makeEarlyEvaluationContext()));
            return Literal.makeLiteral(value, getContainer());
        }
        return this;
    }

    public Expression promote(PromotionOffer offer, Expression parent) throws XPathException {
        Expression exp = offer.accept(parent, this);
        if (exp != null) {
            return exp;
        } else {
            base = doPromotion(base, offer);
            return this;
        }
    }

    public int computeSpecialProperties() {
        return base.getSpecialProperties();
    }

    /**
     * Copy an expression. This makes a deep copy.
     *
     * @return the copy of the original expression
     */

    /*@NotNull*/
    public Expression copy() {
        return new TailExpression(base.copy(), start);
    }

    /*@NotNull*/
    public ItemType getItemType() {
        return base.getItemType();
    }

    public int computeCardinality() {
        return base.getCardinality() | StaticProperty.ALLOWS_ZERO;
    }

    @Override
    public Iterable<Operand> operands() {
        return operandList(
                new Operand(base, OperandRole.SAME_FOCUS_ACTION)
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
        if (base == original) {
            base = replacement;
            found = true;
        }
        return found;
    }

    /**
     * Get the base expression (of which this expression returns the tail part of the value)
     *
     * @return the base expression
     */

    public Expression getBaseExpression() {
        return base;
    }

    /**
     * Get the start offset
     *
     * @return the one-based start offset (returns 2 if all but the first item is being selected)
     */

    public int getStart() {
        return start;
    }

    /**
     * Compare two expressions to see if they are equal
     *
     * @param other the other expression
     * @return true if the expressions are equivalent
     */

    public boolean equals(Object other) {
        return other instanceof TailExpression &&
                base.equals(((TailExpression) other).base) &&
                start == ((TailExpression) other).start;
    }

    public int hashCode() {
        return base.hashCode();
    }

//#ifdefined STREAM

    /**
     * Get a class that supports streamed evaluation of this expression
     *
     * @return the relevant StreamingAdjunct, or null if none is available
     */
    @Override
    public StreamingAdjunct getStreamingAdjunct() {
        return new TailExpressionAdjunct();
    }

    //#endif

    /*@NotNull*/
    public SequenceIterator iterate(XPathContext context) throws XPathException {
        SequenceIterator baseIter = base.iterate(context);
        return TailIterator.make(baseIter, start);
    }

//#ifdefined BYTECODE

    /**
     * Return the compiler of the TailExpression expression
     *
     * @return the relevant ExpressionCompiler
     */
    @Override
    public ExpressionCompiler getExpressionCompiler() {
        return new TailExpressionCompiler();
    }
//#endif


    /**
     * Diagnostic print of expression structure. The abstract expression tree
     * is written to the supplied output destination.
     */

    public void explain(ExpressionPresenter destination) {
        destination.startElement("tail");
        destination.emitAttribute("start", start + "");
        base.explain(destination);
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
        if (start == 2) {
            return "tail(" + base.toString() + ")";
        } else {
            return ExpressionTool.parenthesize(base) + "[position() ge " + start + "]";
        }
    }

    /**
     * Produce a short string identifying the expression for use in error messages
     *
     * @return a short string, sufficient to identify the expression
     */
    @Override
    public String toShortString() {
        if (start == 2) {
            return "tail(" + base.toShortString() + ")";
        } else {
            return base.toShortString() + "[position() ge " + start + "]";
        }
    }
}

