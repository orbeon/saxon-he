////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
// Copyright (c) 2015 Saxonica Limited.
// This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
// If a copy of the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
// This Source Code Form is "Incompatible With Secondary Licenses", as defined by the Mozilla Public License, v. 2.0.
////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

package net.sf.saxon.pattern;

import net.sf.saxon.expr.*;
import net.sf.saxon.expr.instruct.SlotManager;
import net.sf.saxon.expr.parser.ContextItemStaticInfo;
import net.sf.saxon.expr.parser.ExpressionTool;
import net.sf.saxon.expr.parser.ExpressionVisitor;
import net.sf.saxon.expr.parser.PromotionOffer;
import net.sf.saxon.om.AxisInfo;
import net.sf.saxon.om.Item;
import net.sf.saxon.om.NodeInfo;
import net.sf.saxon.trace.ExpressionPresenter;
import net.sf.saxon.trans.XPathException;
import net.sf.saxon.type.ItemType;
import net.sf.saxon.type.Type;
import net.sf.saxon.type.UType;

import java.util.Iterator;

/**
 * An AncestorQualifiedPattern represents a path of the form A/B or A//B, where nodes must match the
 * pattern B and also have a parent/ancestor (respectively) that matches A.
 */

public final class AncestorQualifiedPattern extends Pattern {

    private Pattern basePattern;
    private Pattern upperPattern;
    private byte upwardsAxis = AxisInfo.PARENT;
    private ItemType refinedItemType;
    private boolean testUpperPatternFirst = false;

    public AncestorQualifiedPattern(Pattern base, Pattern upper, byte axis) {
        this.basePattern = base;
        this.upperPattern = upper;
        this.upwardsAxis = axis;
        adoptChildExpression(base);
        adoptChildExpression(upper);
    }

    /**
     * Get the immediate sub-expressions of this expression, with information about the relationship
     * of each expression to its parent expression. Default implementation
     * works off the results of iterateSubExpressions()
     * <p/>
     * <p>If the expression is a Callable, then it is required that the order of the operands
     * returned by this function is the same as the order of arguments supplied to the corresponding
     * call() method.</p>
     *
     * @return an iterator containing the sub-expressions of this expression
     */
    @Override
    public Iterable<Operand> operands() {
        return operandList(
            new Operand(this, upperPattern, OperandRole.SAME_FOCUS_ACTION),
            new Operand(this, basePattern, OperandRole.SAME_FOCUS_ACTION));
    }

    /**
     * Replace any calls on current() by a variable reference bound to the supplied binding
     */
    @Override
    public void bindCurrent(LocalBinding binding) {
        basePattern.bindCurrent(binding);
        upperPattern.bindCurrent(binding);
    }

    /**
     * Get the base pattern, the pattern applying to the node itself
     *
     * @return the base pattern
     */
    public Pattern getBasePattern() {
        return basePattern;
    }

    /**
     * Get the pattern applying to the parent node, if there is one
     *
     * @return the parent pattern, for example if the pattern is a/b[1]/c then the parent
     * pattern is a/b[1]
     */

    public Pattern getUpperPattern() {
        return upperPattern;
    }

    /**
     * Get the upwards axis, that is, the axis by which the upper pattern is reached.
     * Typically Axis.PARENT or Axis.ANCESTOR
     *
     * @return the relevant axis
     */

    public byte getUpwardsAxis() {
        return upwardsAxis;
    }

    /**
     * Test whether a pattern is motionless, that is, whether it can be evaluated against a node
     * without repositioning the input stream. This is a necessary condition for patterns used
     * as the match pattern of a streamed template rule.
     *
     * @param allowExtensions if false, the result is determined strictly according to the W3C
     *                        guaranteed streamability rules. If true, Saxon extensions are permitted: that is, constructs
     *                        may be recognized as motionless by Saxon even if they are not recognized as motionless by
     *                        the W3C rules.
     * @return true if the pattern is motionless, that is, if it can be evaluated against a streamed
     * node without changing the position in the streamed input file
     */

    public boolean isMotionless(boolean allowExtensions) {
        return basePattern.isMotionless(allowExtensions) && upperPattern.isMotionless(allowExtensions);
    }

    /**
     * Simplify the pattern: perform any context-independent optimisations
     *
     */

    public Pattern simplify() throws XPathException {
        upperPattern = upperPattern.simplify();
        basePattern = basePattern.simplify();
        return this;
    }

    /**
     * Type-check the pattern, performing any type-dependent optimizations.
     *
     * @param visitor         an expression visitor
     * @param contextItemType the type of the context item at the point where the pattern appears
     * @return the optimised Pattern
     */

    public Pattern typeCheck(ExpressionVisitor visitor, ContextItemStaticInfo contextItemType) throws XPathException {
        if (upwardsAxis == AxisInfo.PARENT) {
            ItemType type = basePattern.getItemType();
            basePattern.typeCheck(visitor, contextItemType); // project:preconditions
            if (type instanceof NodeTest) {
                // Check that this step in the pattern makes sense in the context of the parent step
                AxisExpression step;
                if (type.getPrimitiveType() == Type.ATTRIBUTE) {
                    step = new AxisExpression(AxisInfo.ATTRIBUTE, (NodeTest) type);
                } else {
                    step = new AxisExpression(AxisInfo.CHILD, (NodeTest) type);
                }
                ExpressionTool.copyLocationInfo(this, step);
                Expression exp = step.typeCheck(visitor, new ContextItemStaticInfo(upperPattern.getItemType(), false));
                refinedItemType = exp.getItemType();
            }
        }
        testUpperPatternFirst = upperPattern.getCost() < basePattern.getCost();
        return this;
    }


    /**
     * Get the dependencies of the pattern. The only possible dependency for a pattern is
     * on local variables. This is analyzed in those patterns where local variables may appear.
     */

    public int getDependencies() {
        return basePattern.getDependencies() | upperPattern.getDependencies();
    }

    /**
     * Iterate over the subexpressions within this pattern
     */

    /*@NotNull*/
    public Iterator<Expression> iterateSubExpressions() {

        Iterator[] pair = {basePattern.iterateSubExpressions(), upperPattern.iterateSubExpressions()};
        //noinspection unchecked
        return new MultiIterator<Expression>(pair);
    }

    /**
     * Allocate slots to any variables used within the pattern
     *
     * @param slotManager keeps track of slots
     * @param nextFree    the next slot that is free to be allocated @return the next slot that is free to be allocated
     */

    public int allocateSlots(SlotManager slotManager, int nextFree) {
        // See tests cnfr23, idky239, match54
        // SlotManager slotManager = env.getStyleElement().getContainingSlotManager();
        nextFree = upperPattern.allocateSlots(slotManager, nextFree);
        nextFree = basePattern.allocateSlots(slotManager, nextFree);
        return nextFree;
    }

    /**
     * Offer promotion for subexpressions within this pattern. The offer will be accepted if the subexpression
     * is not dependent on the factors (e.g. the context item) identified in the PromotionOffer.
     * By default the offer is not accepted - this is appropriate in the case of simple expressions
     * such as constant values and variable references where promotion would give no performance
     * advantage. This method is always called at compile time.
     * <p/>
     * <p>Unlike the corresponding method on {@link net.sf.saxon.expr.Expression}, this method does not return anything:
     * it can make internal changes to the pattern, but cannot return a different pattern. Only certain
     * kinds of promotion are applicable within a pattern: specifically, promotions affecting local
     * variable references within the pattern.
     *
     * @param offer  details of the offer, for example the offer to move
     *               expressions that don't depend on the context to an outer level in
     *               the containing expression
     * @param parent he parent expression
     * @throws net.sf.saxon.trans.XPathException if any error is detected
     */

    public void promote(PromotionOffer offer, Expression parent) throws XPathException {
        basePattern.promote(offer, parent);
        upperPattern.promote(offer, parent);
    }


    /**
     * Determine whether the pattern matches a given item.
     *
     * @param item the item to be tested
     * @return true if the pattern matches, else false
     */

    public boolean matches(Item item, XPathContext context) throws XPathException {
        return item instanceof NodeInfo && matchesBeneathAnchor((NodeInfo) item, null, context);
    }

    /**
     * Determine whether this pattern matches a given Node within the subtree rooted at a given
     * anchor node. This method is used when the pattern is used for streaming.
     *
     * @param node    The NodeInfo representing the Element or other node to be tested against the Pattern
     * @param anchor  The anchor node, which must match any AnchorPattern subpattern
     * @param context The dynamic context. Only relevant if the pattern
     *                uses variables, or contains calls on functions such as document() or key().
     * @return true if the node matches the Pattern, false otherwise
     */

    public boolean matchesBeneathAnchor(NodeInfo node, NodeInfo anchor, XPathContext context) throws XPathException {
        if (testUpperPatternFirst) {
            return matchesUpperPattern(node, anchor, context) && basePattern.matches(node, context);
        } else {
            return basePattern.matches(node, context) && matchesUpperPattern(node, anchor, context);
        }
    }

    private boolean matchesUpperPattern(NodeInfo node, NodeInfo anchor, XPathContext context) throws XPathException {
        switch (upwardsAxis) {
            case AxisInfo.PARENT:
                NodeInfo par = node.getParent();
                return par != null && upperPattern.matchesBeneathAnchor(par, anchor, context);

            case AxisInfo.ANCESTOR: {
                NodeInfo anc = node.getParent();
                while (anc != null) {
                    if (upperPattern.matchesBeneathAnchor(anc, anchor, context)) {
                        return true;
                    }
                    if (anc.isSameNodeInfo(anchor)) {
                        return false;
                    }
                    anc = anc.getParent();
                }
                return false;
            }
            case AxisInfo.ANCESTOR_OR_SELF: {
                NodeInfo anc = node;
                while (anc != null) {
                    if (upperPattern.matchesBeneathAnchor(anc, anchor, context)) {
                        return true;
                    }
                    if (anc.isSameNodeInfo(anchor)) {
                        return false;
                    }
                    anc = anc.getParent();
                }
                return false;
            }
            default:
                throw new XPathException("Unsupported axis " + AxisInfo.axisName[upwardsAxis] + " in pattern");
        }

    }

    /**
     * Get a UType indicating which kinds of items this Pattern can match.
     *
     * @return a UType indicating all the primitive types of item that the pattern can match.
     */
    @Override
    public UType getUType() {
        return basePattern.getUType();
    }

    /**
     * Determine the fingerprint of nodes to which this pattern applies.
     * Used for optimisation.
     *
     * @return the fingerprint of nodes matched by this pattern.
     */

    public int getFingerprint() {
        return basePattern.getFingerprint();
    }

    /**
     * Get a NodeTest that all the nodes matching this pattern must satisfy
     */

    public ItemType getItemType() {
        if (refinedItemType != null) {
            return refinedItemType;
        }
        return basePattern.getItemType();
    }

    /**
     * Convert the pattern to a typed pattern, in which an element name is treated as
     * schema-element(N)
     *
     * @param val either "strict" or "lax" depending on the value of xsl:mode/@typed
     * @return either the original pattern unchanged, or a new pattern as the result of the
     * conversion
     * @throws net.sf.saxon.trans.XPathException if the pattern cannot be converted
     */
    @Override
    public Pattern convertToTypedPattern(String val) throws XPathException {
        if (upperPattern.getUType().equals(UType.DOCUMENT)) {
            // suggests a pattern starting with a leading slash
            Pattern b2 = basePattern.convertToTypedPattern(val);
            if (b2 == basePattern) {
                return this;
            } else {
                return new AncestorQualifiedPattern(b2, upperPattern, upwardsAxis);
            }
        } else {
            Pattern u2 = upperPattern.convertToTypedPattern(val);
            if (u2 == upperPattern) {
                return this;
            } else {
                return new AncestorQualifiedPattern(basePattern, u2, upwardsAxis);
            }
        }
    }

    /**
     * Get the original pattern text
     */
    @Override
    public String toString() {
        return upperPattern.toString() +
            (upwardsAxis == AxisInfo.PARENT ? "/" : "//") +
            basePattern.toString();
    }

    /**
     * Determine whether this pattern is the same as another pattern
     *
     * @param other the other object
     */

    public boolean equals(Object other) {
        if (other instanceof AncestorQualifiedPattern) {
            AncestorQualifiedPattern aqp = (AncestorQualifiedPattern) other;
            return basePattern.equals(aqp.basePattern) && upperPattern.equals(aqp.upperPattern) && upwardsAxis == aqp.upwardsAxis;
        } else {
            return false;
        }
    }

    /**
     * hashcode supporting equals()
     */

    public int hashCode() {
        return 88267 ^ basePattern.hashCode() ^ upperPattern.hashCode() ^ (upwardsAxis << 22);
    }

    public void export(ExpressionPresenter presenter) throws XPathException {
        presenter.startElement("p.withUpper");
        presenter.emitAttribute("axis", AxisInfo.axisName[getUpwardsAxis()]);
        presenter.emitAttribute("upFirst", ""+ testUpperPatternFirst);
        basePattern.export(presenter);
        upperPattern.export(presenter);
        presenter.endElement();
    }

    /**
     * Copy a pattern. This makes a deep copy.
     *
     * @return the copy of the original pattern
     */

    /*@NotNull*/
    public Pattern copy() {
        AncestorQualifiedPattern n = new AncestorQualifiedPattern(basePattern.copy(),
            upperPattern.copy(), upwardsAxis);
        ExpressionTool.copyLocationInfo(this, n);
        return n;
    }

}

