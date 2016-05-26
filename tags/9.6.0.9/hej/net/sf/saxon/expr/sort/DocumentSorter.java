////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
// Copyright (c) 2014 Saxonica Limited.
// This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
// If a copy of the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
// This Source Code Form is "Incompatible With Secondary Licenses", as defined by the Mozilla Public License, v. 2.0.
////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

package net.sf.saxon.expr.sort;

import com.saxonica.ee.bytecode.DocumentSorterCompiler;
import com.saxonica.ee.bytecode.ExpressionCompiler;
import com.saxonica.ee.stream.adjunct.DocumentSorterAdjunct;
import com.saxonica.ee.stream.adjunct.StreamingAdjunct;
import net.sf.saxon.Configuration;
import net.sf.saxon.expr.*;
import net.sf.saxon.expr.parser.ContextItemStaticInfo;
import net.sf.saxon.expr.parser.ExpressionTool;
import net.sf.saxon.expr.parser.ExpressionVisitor;
import net.sf.saxon.expr.parser.PromotionOffer;
import net.sf.saxon.functions.Reverse;
import net.sf.saxon.om.AxisInfo;
import net.sf.saxon.om.SequenceIterator;
import net.sf.saxon.pattern.Pattern;
import net.sf.saxon.trace.ExpressionPresenter;
import net.sf.saxon.trans.XPathException;


/**
 * A DocumentSorter is an expression that sorts a sequence of nodes into
 * document order.
 */
public class DocumentSorter extends UnaryExpression {

    private ItemOrderComparer comparer;

    public DocumentSorter(Expression base) {
        super(base);
        int props = base.getSpecialProperties();
        if (((props & StaticProperty.CONTEXT_DOCUMENT_NODESET) != 0) ||
                (props & StaticProperty.SINGLE_DOCUMENT_NODESET) != 0) {
            comparer = LocalOrderComparer.getInstance();
        } else {
            comparer = GlobalOrderComparer.getInstance();
        }
    }

    protected OperandRole getOperandRole() {
        return OperandRole.SAME_FOCUS_ACTION;
    }

    /**
     * Get a name identifying the kind of expression, in terms meaningful to a user.
     *
     * @return a name identifying the kind of expression, in terms meaningful to a user.
     *         The name will always be in the form of a lexical XML QName, and should match the name used
     *         in explain() output displaying the expression.
     */

    public String getExpressionName() {
        return "documentSort";
    }

    public ItemOrderComparer getComparer() {
        return comparer;
    }

    /*@NotNull*/
    public Expression simplify(ExpressionVisitor visitor) throws XPathException {
        operand = visitor.simplify(operand);
        if ((operand.getSpecialProperties() & StaticProperty.ORDERED_NODESET) != 0) {
            // this can happen as a result of further simplification
            return operand;
        }
        return this;
    }

    /*@NotNull*/
    public Expression optimize(ExpressionVisitor visitor, ContextItemStaticInfo contextItemType) throws XPathException {
        operand = visitor.optimize(operand, contextItemType);
        if ((operand.getSpecialProperties() & StaticProperty.ORDERED_NODESET) != 0) {
            // this can happen as a result of further simplification
            return operand;
        }
        if (operand instanceof SlashExpression) {
            return visitor.getConfiguration().obtainOptimizer().makeConditionalDocumentSorter(
                    this, (SlashExpression) operand);
        }
        return this;
    }

    /**
     * Replace this expression by a simpler expression that delivers the results without regard
     * to order.
     *
     * @param retainAllNodes set to true if the result must contain exactly the same nodes as the
     *                       original; set to false if the result can eliminate (or introduce) duplicates.
     * @param forStreaming set to true if optimizing for streaming
     */
    @Override
    public Expression unordered(boolean retainAllNodes, boolean forStreaming) throws XPathException {
        operand = operand.unordered(retainAllNodes, forStreaming);
        if ((operand.getSpecialProperties() & StaticProperty.ORDERED_NODESET) != 0) {
            return operand;
        }
        if (!retainAllNodes) {
            return operand;
        } else if (operand instanceof SlashExpression) {
            // handle the common case of //section/head where it is safe to remove sorting, because
            // no duplicates need to be removed
            SlashExpression exp = (SlashExpression)operand;
            Expression a = exp.getSelectExpression();
            Expression b = exp.getActionExpression();
            a = ExpressionTool.unfilteredExpression(a, false);
            b = ExpressionTool.unfilteredExpression(b, false);
            if (a instanceof AxisExpression &&
                    (((AxisExpression)a).getAxis() == AxisInfo.DESCENDANT || ((AxisExpression)a).getAxis() == AxisInfo.DESCENDANT_OR_SELF) &&
                    b instanceof AxisExpression &&
                    ((AxisExpression)b).getAxis() == AxisInfo.CHILD) {
                return operand.unordered(retainAllNodes, false);
            }
        }
        return this;
    }


    public int computeSpecialProperties() {
        return operand.getSpecialProperties() | StaticProperty.ORDERED_NODESET;
    }

    /**
     * Copy an expression. This makes a deep copy.
     *
     * @return the copy of the original expression
     */

    /*@NotNull*/
    public Expression copy() {
        return new DocumentSorter(getBaseExpression().copy());
    }

    /**
     * Promote this expression if possible
     */

    public Expression promote(PromotionOffer offer, Expression parent) throws XPathException {
        Expression exp = offer.accept(parent, this);
        if (exp != null) {
            return exp;
        } else {
            operand = doPromotion(operand, offer);
            return this;
        }
    }

    /**
     * Convert this expression to an equivalent XSLT pattern
     *
     * @param config the Saxon configuration
     * @param is30   true if this is XSLT 3.0
     * @return the equivalent pattern
     * @throws net.sf.saxon.trans.XPathException
     *          if conversion is not possible
     */
    @Override
    public Pattern toPattern(Configuration config, boolean is30) throws XPathException {
        return getBaseExpression().toPattern(config, is30);
    }

    /*@NotNull*/
    public SequenceIterator iterate(XPathContext context) throws XPathException {
        //System.err.println("** SORTING **");
        return new DocumentOrderIterator(operand.iterate(context), comparer);
    }

    public boolean effectiveBooleanValue(XPathContext context) throws XPathException {
        return operand.effectiveBooleanValue(context);
    }

//#ifdefined BYTECODE

    /**
     * Return the compiler of the DocumentSorter expression
     *
     * @return the relevant ExpressionCompiler
     */
    @Override
    public ExpressionCompiler getExpressionCompiler() {
        return new DocumentSorterCompiler();
    }
//#endif

    /**
     * Diagnostic print of expression structure. The abstract expression tree
     * is written to the supplied output destination.
     */

    public void explain(ExpressionPresenter out) {
        out.startElement("sortAndDeduplicate");
        out.emitAttribute("intraDocument", comparer instanceof LocalOrderComparer ? "true" : "false");
        operand.explain(out);
        out.endElement();
    }

//#ifdefined STREAM


    /**
     * Get a class that supports streamed evaluation of this expression
     *
     * @return the relevant StreamingAdjunct, or null if none is available
     */
    @Override
    protected StreamingAdjunct getStreamingAdjunct() {
        // The DocumentSorterAdjunct is designed to handle ancestors arriving in axis order, not in reverse axis order. If
        // a reverse() call has been wrapped around the ancestor axis, remove it.
        // TODO: this is a kludge. See test sf-insert-before-031
        if (operand instanceof SlashExpression && ((SlashExpression)operand).getActionExpression() instanceof Reverse) {
            ((SlashExpression)operand).setStepExpression(((Reverse)((SlashExpression)operand).getActionExpression()).getArguments()[0]);
        }
        return new DocumentSorterAdjunct();
    }

//#endif


}

