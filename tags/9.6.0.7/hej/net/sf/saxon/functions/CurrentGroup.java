////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
// Copyright (c) 2014 Saxonica Limited.
// This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
// If a copy of the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
// This Source Code Form is "Incompatible With Secondary Licenses", as defined by the Mozilla Public License, v. 2.0.
////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

package net.sf.saxon.functions;

import com.saxonica.ee.stream.adjunct.CurrentGroupAdjunct;
import net.sf.saxon.expr.Callable;
import net.sf.saxon.expr.Expression;
import net.sf.saxon.expr.StaticProperty;
import net.sf.saxon.expr.XPathContext;
import net.sf.saxon.expr.instruct.ForEachGroup;
import net.sf.saxon.expr.parser.ExpressionVisitor;
import net.sf.saxon.expr.sort.GroupIterator;
import net.sf.saxon.om.Item;
import net.sf.saxon.om.Sequence;
import net.sf.saxon.om.SequenceIterator;
import net.sf.saxon.om.SequenceTool;
import net.sf.saxon.trans.XPathException;
import net.sf.saxon.tree.iter.EmptyIterator;
import net.sf.saxon.type.AnyItemType;
import net.sf.saxon.type.ItemType;
import net.sf.saxon.value.DecimalValue;

/**
 * Implements the XSLT function current-group()
 */

public class CurrentGroup extends SystemFunctionCall implements Callable {

    private boolean is30 = false;
    private boolean isInLoop = false;
    private ItemType itemType = AnyItemType.getInstance();
    private ForEachGroup controllingInstruction = null; // may be unknown, when current group has dynamic scope

    @Override
    public void checkArguments(ExpressionVisitor visitor) throws XPathException {
        is30 = visitor.getStaticContext().getXPathLanguageLevel().equals(DecimalValue.THREE);
        super.checkArguments(visitor);
    }

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
    }

    /**
     * Get the innermost containing xsl:for-each-group instruction, if there is one
     * @return the innermost containing xsl:for-each-group instruction
     */

    public ForEachGroup getControllingInstruction() {
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
     * Determine the special properties of this expression. The properties such as document-ordering are the same as
     * the properties of the grouping population as a whole.
     *
     * @return {@link net.sf.saxon.expr.StaticProperty#NON_CREATIVE} (unless the variable is assignable using saxon:assign)
     */
    @Override
    public int computeSpecialProperties() {
        if (controllingInstruction == null) {
            return 0;
        } else {
            return controllingInstruction.getSelectExpression().getSpecialProperties();
        }
    }

    @Override
    public Expression copy() {
        CurrentGroup cg = (CurrentGroup)super.copy();
        cg.is30 = is30;
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
            if (is30) {
                XPathException err = new XPathException("There is no current group", "XTDE1061");
                err.setLocator(this);
                throw err;
            } else {
                return EmptyIterator.emptyIterator();
            }
        }
        return gi.iterateCurrentGroup();
    }

    /**
     * Evaluate the expression
     *
     * @param context   the dynamic evaluation context
     * @param arguments the values of the arguments, supplied as SequenceIterators
     * @return the result of the evaluation, in the form of a SequenceIterator
     * @throws net.sf.saxon.trans.XPathException
     *          if a dynamic error occurs during the evaluation of the expression
     */
    public Sequence call(XPathContext context, Sequence[] arguments /*@NotNull*/) throws XPathException {
        return SequenceTool.toLazySequence(iterate(context));
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

