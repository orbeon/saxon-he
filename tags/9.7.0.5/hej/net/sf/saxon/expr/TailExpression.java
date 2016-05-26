////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
// Copyright (c) 2015 Saxonica Limited.
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
import net.sf.saxon.om.GroundedValue;
import net.sf.saxon.om.SequenceIterator;
import net.sf.saxon.trace.ExpressionPresenter;
import net.sf.saxon.trans.XPathException;
import net.sf.saxon.type.ItemType;
import net.sf.saxon.value.SequenceExtent;

/**
 * A TailExpression represents a FilterExpression of the form EXPR[position() > n]
 * Here n is usually 2, but we allow other values
 */
public class TailExpression extends UnaryExpression {

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
        super(base);
        this.start = start;
    }

    /*@NotNull*/
    public Expression optimize(ExpressionVisitor visitor, ContextItemStaticInfo contextInfo) throws XPathException {
        optimizeChildren(visitor, contextInfo);
        if (getBaseExpression() instanceof Literal) {
            GroundedValue value =
                    SequenceExtent.makeSequenceExtent(iterate(visitor.getStaticContext().makeEarlyEvaluationContext()));
            return Literal.makeLiteral(value);
        }
        return this;
    }

    /**
     * Copy an expression. This makes a deep copy.
     *
     * @return the copy of the original expression
     */

    /*@NotNull*/
    public Expression copy() {
        TailExpression exp = new TailExpression(getBaseExpression().copy(), start);
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
        return ITERATE_METHOD;
    }

    /*@NotNull*/
    public ItemType getItemType() {
        return getBaseExpression().getItemType();
    }

    public int computeCardinality() {
        return getBaseExpression().getCardinality() | StaticProperty.ALLOWS_ZERO;
    }

    @Override
    protected OperandRole getOperandRole() {
        return OperandRole.SAME_FOCUS_ACTION;
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
                super.equals(other) &&
                start == ((TailExpression) other).start;
    }

    public int hashCode() {
        return super.hashCode() ^ start;
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
        SequenceIterator baseIter = getBaseExpression().iterate(context);
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

    public void export(ExpressionPresenter destination) throws XPathException {
        destination.startElement("tail", this);
        destination.emitAttribute("start", start + "");
        getBaseExpression().export(destination);
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
            return "tail(" + getBaseExpression().toString() + ")";
        } else {
            return ExpressionTool.parenthesize(getBaseExpression()) + "[position() ge " + start + "]";
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
            return "tail(" + getBaseExpression().toShortString() + ")";
        } else {
            return getBaseExpression().toShortString() + "[position() ge " + start + "]";
        }
    }
}

