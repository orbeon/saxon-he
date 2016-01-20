////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
// Copyright (c) 2015 Saxonica Limited.
// This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
// If a copy of the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
// This Source Code Form is "Incompatible With Secondary Licenses", as defined by the Mozilla Public License, v. 2.0.
////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

package net.sf.saxon.functions;

import com.saxonica.ee.stream.adjunct.CurrentGroupAdjunct;
import net.sf.saxon.expr.Expression;
import net.sf.saxon.expr.StaticProperty;
import net.sf.saxon.expr.XPathContext;
import net.sf.saxon.expr.instruct.ForEachGroup;
import net.sf.saxon.expr.sort.GroupIterator;
import net.sf.saxon.om.SequenceIterator;
import net.sf.saxon.trace.ExpressionPresenter;
import net.sf.saxon.trans.XPathException;
import net.sf.saxon.tree.iter.EmptyIterator;
import net.sf.saxon.type.AnyItemType;
import net.sf.saxon.type.ItemType;

/**
 * Implements the XSLT function current-group()
 */

public class CurrentGroupCall extends Expression {

    private boolean isInLoop = false;
    private ItemType itemType = AnyItemType.getInstance();
    private ForEachGroup controllingInstruction = null; // may be unknown, when current group has dynamic scope

    /**
     * Set the containing xsl:for-each-group instruction, if there is one
     * @param instruction the (innermost) containing xsl:for-each-group instruction
     * @param itemType the statically inferred item type of the grouping population
     * @param isInLoop true if the current-group() expression is evaluated more than once during
     * evaluation of the body of the for-each-group instruction
     */

    public void setControllingInstruction(ForEachGroup instruction, ItemType itemType, boolean isInLoop) {
        this.controllingInstruction = instruction;
        this.isInLoop = isInLoop;
        this.itemType = itemType;
        resetLocalStaticProperties();
    }

    /**
     * Get the innermost containing xsl:for-each-group instruction, if there is one
     * @return the innermost containing xsl:for-each-group instruction
     */

    public ForEachGroup getControllingInstruction() {
        if (controllingInstruction == null) {
            Expression child = this;
            Expression parent = getParentExpression();
            while (parent != null) {
                if (parent instanceof ForEachGroup && child == ((ForEachGroup)parent).getActionExpression()) {
                    break;
                }
                child = parent;
                parent = parent.getParentExpression();
            }
            controllingInstruction = (ForEachGroup)parent;
        }
        return controllingInstruction;
    }

    /**
     * Determine whether the current-group() function is executed repeatedly within a single iteration
     * of the containing xsl:for-each-group
     * @return true if it is evaluated repeatedly
     */

    public boolean isInLoop() {
        return isInLoop;
    }

    /**
     * Determine the item type of the value returned by the function
     */

    @Override
    public ItemType getItemType() {
        return itemType;
    }

    /**
     * Determine the dependencies
     */

    public int getIntrinsicDependencies() {
        return StaticProperty.DEPENDS_ON_CURRENT_GROUP;
    }

    /**
     * Compute the static cardinality of this expression
     *
     * @return the computed cardinality, as one of the values {@link net.sf.saxon.expr.StaticProperty#ALLOWS_ZERO_OR_ONE},
     * {@link net.sf.saxon.expr.StaticProperty#EXACTLY_ONE}, {@link net.sf.saxon.expr.StaticProperty#ALLOWS_ONE_OR_MORE},
     * {@link net.sf.saxon.expr.StaticProperty#ALLOWS_ZERO_OR_MORE}
     */
    @Override
    protected int computeCardinality() {
        return StaticProperty.ALLOWS_ONE_OR_MORE;
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

    /**
     * Diagnostic print of expression structure. The abstract expression tree
     * is written to the supplied output destination.
     *
     * @param out the expression presenter used to display the structure
     */
    @Override
    public void export(ExpressionPresenter out) {
        out.startElement("currentGroup");
        out.endElement();
    }

    /**
     * Determine the special properties of this expression. The properties such as document-ordering are the same as
     * the properties of the grouping population as a whole.
     *
     * @return {@link net.sf.saxon.expr.StaticProperty#NON_CREATIVE} (unless the variable is assignable using saxon:assign)
     */
    @Override
    public int computeSpecialProperties() {
        if (getControllingInstruction() == null) {
            return 0;
        } else {
            return controllingInstruction.getSelectExpression().getSpecialProperties();
        }
    }

    @Override
    public Expression copy() {
        CurrentGroupCall cg = new CurrentGroupCall();
        cg.isInLoop = isInLoop;
        cg.itemType = itemType;
        cg.controllingInstruction = controllingInstruction;
        return cg;
    }

    /**
     * Return an iteration over the result sequence
     */

    /*@NotNull*/
    public SequenceIterator iterate(XPathContext c) throws XPathException {
        GroupIterator gi = c.getCurrentGroupIterator();
        if (gi == null || !gi.hasCurrentGroup()) {
            if (getRetainedStaticContext().getXPathVersion() >= 30) {
                XPathException err = new XPathException("There is no current group", "XTDE1061");
                err.setLocation(getLocation());
                throw err;
            } else {
                return EmptyIterator.emptyIterator();
            }
        }
        return gi.iterateCurrentGroup();
    }

    /**
     * <p>The toString() method for an expression attempts to give a representation of the expression
     * in an XPath-like form.</p>
     * <p/>
     * <p>For subclasses of Expression that represent XPath expressions, the result should always be a string that
     * parses as an XPath 3.0 expression. The expression produced should be equivalent to the original making certain
     * assumptions about the static context. In general the expansion will make no assumptions about namespace bindings,
     * except that (a) the prefix "xs" is used to refer to the XML Schema namespace, and (b) the default funcion namespace
     * is assumed to be the "fn" namespace.</p>
     * <p/>
     * <p>In the case of XSLT instructions and XQuery expressions, the toString() method gives an abstracted view of the syntax
     * that is not designed in general to be parseable.</p>
     *
     * @return a representation of the expression as a string
     */
    @Override
    public String toString() {
        return "current-group()";
    }

    /**
     * Produce a short string identifying the expression for use in error messages
     *
     * @return a short string, sufficient to identify the expression
     */
    @Override
    public String toShortString() {
        return toString();
    }

    //#ifdefined STREAM
    /**
     * Get a class that supports streamed evaluation of this expression
     *
     * @return the relevant StreamingAdjunct, or null if none is available
     */
    @Override
    public CurrentGroupAdjunct getStreamingAdjunct() {
        return new CurrentGroupAdjunct();
    }
    //#endif
}

