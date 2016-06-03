////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
// Copyright (c) 2015 Saxonica Limited.
// This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
// If a copy of the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
// This Source Code Form is "Incompatible With Secondary Licenses", as defined by the Mozilla Public License, v. 2.0.
////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

package net.sf.saxon.pattern;

import com.saxonica.ee.stream.Sweep;
import net.sf.saxon.expr.Binding;
import net.sf.saxon.expr.Expression;
import net.sf.saxon.expr.Operand;
import net.sf.saxon.expr.XPathContext;
import net.sf.saxon.expr.instruct.SlotManager;
import net.sf.saxon.expr.parser.*;
import net.sf.saxon.om.Item;
import net.sf.saxon.om.NodeInfo;
import net.sf.saxon.trace.ExpressionPresenter;
import net.sf.saxon.trans.XPathException;
import net.sf.saxon.tree.iter.ManualIterator;
import net.sf.saxon.type.AnyItemType;
import net.sf.saxon.type.ItemType;
import net.sf.saxon.type.UType;

import java.util.*;

/**
 * A ConditionalPattern tests a node against one of a number of patterns depending on the value of a condition.
 * This is used only as a streaming selection.
 */
public class ConditionalPattern extends Pattern {

    private Expression[] conditions;
    private Pattern[] patterns;

    public ConditionalPattern(Expression[] conditions, Pattern[] patterns) {
        this.conditions = conditions;
        this.patterns = patterns;
    }

    private Expression getCondition(int n) {
        return conditions[n];
    }

    private void setCondition(int n, Expression exp) {
        conditions[n] = exp;
    }

    // TODO: implement operands()

    /**
     * Simplify the pattern: perform any context-independent optimisations
     *
     */

    public Pattern simplify() throws XPathException {
        for (int i = 0; i < conditions.length; i++) {
            setCondition(i, getCondition(i).simplify());
            patterns[i] = patterns[i].simplify();
        }
        return this;
    }

    /**
     * Type-check the pattern.
     * This is only needed for patterns that contain variable references or function calls.
     *
     * @return the optimised Pattern
     */

    public Pattern typeCheck(ExpressionVisitor visitor, ContextItemStaticInfo contextItemType) throws XPathException {
        for (int i = 0; i < conditions.length; i++) {
            setCondition(i, getCondition(i).typeCheck(visitor, contextItemType));
            patterns[i] = patterns[i].typeCheck(visitor, contextItemType);
        }
        return this;
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
     * @param parent
     * @throws net.sf.saxon.trans.XPathException
     *          if any error is detected
     */

    public void promote(PromotionOffer offer, Expression parent) throws XPathException {
        for (int i = 0; i < conditions.length; i++) {
            setCondition(i, getCondition(i).promote(offer));
            patterns[i].promote(offer, parent);
        }
    }

    /**
     * Set the original text
     */

    public void setOriginalText(String pattern) {
        super.setOriginalText(pattern);
        for (int i = 0; i < conditions.length; i++) {
            patterns[i].setOriginalText(pattern);
        }
    }

    //#ifdefined STREAM
    @Override
    public boolean isMotionless(boolean allowExtensions) {
        for (Expression condition : conditions) {
            condition.getStreamability(allowExtensions, new ContextItemStaticInfo(getItemType(), false, true), null);
            if (condition.getSweep() != Sweep.MOTIONLESS) {
                return false;
            }
        }
        for (Pattern p : patterns) {
            if (!p.isMotionless(allowExtensions)) {
                return false;
            }
        }
        return true;
    }
//#endif

    /**
     * Allocate slots to any variables used within the pattern
     *
     * @param slotManager
     * @param nextFree    the next slot that is free to be allocated @return the next slot that is free to be allocated
     */

    public int allocateSlots(SlotManager slotManager, int nextFree) {

        for (int i = 0; i < conditions.length; i++) {
            nextFree = ExpressionTool.allocateSlots(getCondition(i), nextFree, slotManager);
            nextFree = patterns[i].allocateSlots(slotManager, nextFree);
        }
        return nextFree;
    }

    @Override
    public void export(ExpressionPresenter presenter) throws XPathException {
        presenter.startElement("p.choose");
        for (int i = 0; i < conditions.length; i++) {
            conditions[i].export(presenter);
            patterns[i].export(presenter);
        }
        presenter.endElement();
    }

    /**
     * Determine if the supplied node matches the pattern
     *
     * @param item the node to be compared
     * @return true if the node matches either of the operand patterns
     */

    public boolean matches(Item item, XPathContext context) throws XPathException {
        for (int i = 0; i < conditions.length; i++) {
            boolean b = getCondition(i).effectiveBooleanValue(context);
            if (b) {
                return patterns[i].matches(item, context);
            }
        }
        return false;
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
        XPathContext c2 = context.newContext();
        ManualIterator iter = new ManualIterator(anchor);
        c2.setCurrentIterator(iter);
        for (int i = 0; i < conditions.length; i++) {
            boolean b = getCondition(i).effectiveBooleanValue(c2);
            if (b) {
                return patterns[i].matchesBeneathAnchor(node, anchor, context);
            }
        }
        return false;
    }

    /**
     * Get a UType indicating which kinds of items this Pattern can match.
     *
     * @return a UType indicating all the primitive types of item that the pattern can match.
     */
    @Override
    public UType getUType() {
        UType mask = patterns[0].getUType();
        for (int i = 1; i < patterns.length; i++) {
            mask = mask.union(patterns[i].getUType());
        }
        return mask;
    }

    /**
     * Get a NodeTest that all the nodes matching this pattern must satisfy
     */

    public ItemType getItemType() {
        UType uType = getUType();
        if (uType.equals(UType.ANY)) {
            return AnyItemType.getInstance();
        } else if (uType.equals(UType.ANY_NODE)) {
            return AnyNodeTest.getInstance();
        } else {
            return new MultipleNodeKindTest(uType);
        }
    }


    /**
     * Get the dependencies of the pattern. The only possible dependency for a pattern is
     * on local variables. This is analyzed in those patterns where local variables may appear.
     *
     * @return the dependencies, as a bit-significant mask
     */

    public int getDependencies() {
        int d = 0;
        for (int i = 0; i < conditions.length; i++) {
            d |= getCondition(i).getDependencies();
            d |= patterns[i].getDependencies();
        }
        return d;
    }

    /**
     * Iterate over the subexpressions within this pattern
     *
     * @return an iterator over the subexpressions.
     */

    /*@NotNull*/
    public Iterator<Expression> iterateSubExpressions() {
        List<Expression> list = new ArrayList<Expression>(conditions.length * 2);
        for (int i = 0; i < conditions.length; i += 2) {
            for (Operand o : getCondition(i).operands()) {
                list.add(o.getChildExpression());
            }
            for (Iterator<Expression> iter = patterns[i].iterateSubExpressions(); iter.hasNext();) {
                list.add(iter.next());
            }
        }
        return list.iterator();
    }

    /**
     * Override method to set the system ID
     */

//    public void setSystemId(String systemId) {
//        super.setSystemId(systemId);
//        for (int i = 0; i < conditions.length; i += 2) {
//            patterns[i].setSystemId(systemId);
//        }
//    }
//
//    /**
//     * Override method to set the line number
//     */
//
//    public void setLineNumber(int lineNumber) {
//        super.setLineNumber(lineNumber);
//        for (int i = 0; i < conditions.length; i += 2) {
//            patterns[i].setLineNumber(lineNumber);
//        }
//    }

    /**
     * Determine whether this pattern is the same as another pattern
     *
     * @param other the other object
     */

    public boolean equals(/*@NotNull*/ Object other) {
        if (other instanceof ConditionalPattern) {
            return Arrays.equals(conditions, ((ConditionalPattern) other).conditions) &&
                    Arrays.equals(patterns, ((ConditionalPattern) other).patterns);
        } else {
            return false;
        }
    }

    /**
     * Hashcode supporting equals()
     */

    public int hashCode() {
        int h = 0x836b92a0;
        for (int i = 0; i < conditions.length; i += 2) {
            h ^= patterns[i].hashCode() ^ getCondition(i).hashCode();
        }
        return h;
    }

    /**
     * Copy a pattern. This makes a deep copy.
     *
     * @return the copy of the original pattern
     * @param rebindings
     */

    /*@NotNull*/
    public Pattern copy(Map<IdentityWrapper<Binding>, Binding> rebindings) {
        //TODO - copy the conditions and patterns
        ConditionalPattern n = new ConditionalPattern(conditions,patterns);
        ExpressionTool.copyLocationInfo(this, n);
        return n;
    }
}

