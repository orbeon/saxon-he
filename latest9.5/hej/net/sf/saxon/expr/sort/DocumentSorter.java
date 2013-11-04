////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
// Copyright (c) 2013 Saxonica Limited.
// This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
// If a copy of the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
// This Source Code Form is "Incompatible With Secondary Licenses", as defined by the Mozilla Public License, v. 2.0.
////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

package net.sf.saxon.expr.sort;

import com.saxonica.bytecode.DocumentSorterCompiler;
import com.saxonica.bytecode.ExpressionCompiler;
import net.sf.saxon.Configuration;
import net.sf.saxon.expr.*;
import net.sf.saxon.expr.parser.ExpressionVisitor;
import net.sf.saxon.expr.parser.PromotionOffer;
import net.sf.saxon.om.SequenceIterator;
import net.sf.saxon.pattern.Pattern;
import net.sf.saxon.trace.ExpressionPresenter;
import net.sf.saxon.trans.XPathException;

import java.util.List;


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

    /**
     * Get a name identifying the kind of expression, in terms meaningful to a user.
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
    public Expression optimize(ExpressionVisitor visitor, ExpressionVisitor.ContextItemType contextItemType) throws XPathException {
        operand = visitor.optimize(operand, contextItemType);
        if ((operand.getSpecialProperties() & StaticProperty.ORDERED_NODESET) != 0) {
            // this can happen as a result of further simplification
            return operand;
        }
        if (operand instanceof SlashExpression) {
            return visitor.getConfiguration().obtainOptimizer().makeConditionalDocumentSorter(
                    this, (SlashExpression)operand);
        }
        return this;
    }

    /**
     * Replace this expression by an expression that returns the same result but without
     * regard to order
     *
     * @param retainAllNodes true if all nodes in the result must be retained; false
     *                       if duplicates can be eliminated
     */
    @Override
    public Expression unordered(boolean retainAllNodes) throws XPathException {
        if (!retainAllNodes) {
            return operand.unordered(retainAllNodes);
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

//#ifdefined STREAM

    /**
     * Convert this expression to a streaming pattern (a pattern used internally to match nodes during
     * push processing of an event stream)
     *
     * @param config the Saxon configuration
     * @param reasonForFailure a list which will be populated with messages giving reasons why the
     *                         expression cannot be converted
     * @return the equivalent pattern if conversion succeeds; otherwise null
     */

    public Pattern toStreamingPattern(Configuration config, List<String> reasonForFailure) {
        return getBaseExpression().toStreamingPattern(config, reasonForFailure);
    }

//#endif


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

}

