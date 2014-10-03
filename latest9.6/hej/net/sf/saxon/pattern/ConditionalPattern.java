////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
// Copyright (c) 2014 Saxonica Limited.
// This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
// If a copy of the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
// This Source Code Form is "Incompatible With Secondary Licenses", as defined by the Mozilla Public License, v. 2.0.
////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

package net.sf.saxon.pattern;

import com.saxonica.ee.stream.Sweep;
import net.sf.saxon.expr.Expression;
import net.sf.saxon.expr.Operand;
import net.sf.saxon.expr.XPathContext;
import net.sf.saxon.expr.instruct.SlotManager;
import net.sf.saxon.expr.parser.ContextItemStaticInfo;
import net.sf.saxon.expr.parser.ExpressionTool;
import net.sf.saxon.expr.parser.ExpressionVisitor;
import net.sf.saxon.expr.parser.PromotionOffer;
import net.sf.saxon.om.Item;
import net.sf.saxon.om.NodeInfo;
import net.sf.saxon.trans.XPathException;
import net.sf.saxon.tree.iter.ManualIterator;
import net.sf.saxon.type.ItemType;
import net.sf.saxon.type.Type;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

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

    /**
     * Simplify the pattern: perform any context-independent optimisations
     *
     * @param visitor an expression visitor
     */

    public Pattern simplify(ExpressionVisitor visitor) throws XPathException {
        for (int i = 0; i < conditions.length; i++) {
            conditions[i] = visitor.simplify(conditions[i]);
            patterns[i] = patterns[i].simplify(visitor);
        }
        return this;
    }

    /**
     * Type-check the pattern.
     * This is only needed for patterns that contain variable references or function calls.
     *
     * @return the optimised Pattern
     */

    public Pattern analyze(ExpressionVisitor visitor, ContextItemStaticInfo contextItemType) throws XPathException {
        for (int i = 0; i < conditions.length; i++) {
            conditions[i] = visitor.typeCheck(conditions[i], contextItemType);
            patterns[i] = patterns[i].analyze(visitor, contextItemType);
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
            conditions[i] = conditions[i].promote(offer, parent);
            patterns[i].promote(offer, parent);
        }
    }

    /**
     * Replace a subexpression by a replacement subexpression
     *
     * @param original    the expression to be replaced
     * @param replacement the new expression to be inserted in its place
     * @return true if the replacement was carried out
     */

    public boolean replaceSubExpression(Expression original, Expression replacement) {
        boolean found = false;
        for (int i = 0; i < conditions.length; i++) {
            found |= conditions[i].replaceOperand(original, replacement);
            found |= patterns[i].replaceSubExpression(original, replacement);
        }
        return found;
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
            nextFree = ExpressionTool.allocateSlots(conditions[i], nextFree, slotManager);
            nextFree = patterns[i].allocateSlots(slotManager, nextFree);
        }
        return nextFree;
    }

    /**
     * Determine if the supplied node matches the pattern
     *
     * @param item the node to be compared
     * @return true if the node matches either of the operand patterns
     */

    public boolean matches(Item item, XPathContext context) throws XPathException {
        for (int i = 0; i < conditions.length; i++) {
            boolean b = conditions[i].effectiveBooleanValue(context);
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
            boolean b = conditions[i].effectiveBooleanValue(c2);
            if (b) {
                return patterns[i].matchesBeneathAnchor(node, anchor, context);
            }
        }
        return false;
    }

    /**
     * Determine the types of nodes to which this pattern applies. Used for optimisation.
     * For patterns that match nodes of several types, return Type.NODE
     *
     * @return the type of node matched by this pattern. e.g. Type.ELEMENT or Type.TEXT
     */

    public int getNodeKind() {
        int type = patterns[0].getNodeKind();
        for (int i = 1; i < patterns.length; i++) {
            int p = patterns[i].getNodeKind();
            if (p != type) {
                return Type.NODE;
            }
        }
        return type;
    }

    /**
     * Get a mask indicating which kinds of nodes this NodeTest can match. This is a combination
     * of bits: 1<<Type.ELEMENT for element nodes, 1<<Type.TEXT for text nodes, and so on. The default
     * implementation indicates that nodes of all kinds are matched.
     *
     * @return a combination of bits: 1<<Type.ELEMENT for element nodes, 1<<Type.TEXT for text nodes, and so on
     */
    @Override
    public int getNodeKindMask() {
        int mask = patterns[0].getNodeKindMask();
        for (int i = 1; i < patterns.length; i++) {
            mask |= patterns[i].getNodeKindMask();
        }
        return mask;
    }

    /**
     * Get a NodeTest that all the nodes matching this pattern must satisfy
     */

    public ItemType getItemType() {
        int nodeType = getNodeKind();
        if (nodeType == Type.NODE) {
            return AnyNodeTest.getInstance();
        } else {
            return NodeKindTest.makeNodeKindTest(nodeType);
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
            d |= conditions[i].getDependencies();
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
            for (Operand o : conditions[i].operands()) {
                list.add(o.getExpression());
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

    public void setSystemId(String systemId) {
        super.setSystemId(systemId);
        for (int i = 0; i < conditions.length; i += 2) {
            patterns[i].setSystemId(systemId);
        }
    }

    /**
     * Override method to set the line number
     */

    public void setLineNumber(int lineNumber) {
        super.setLineNumber(lineNumber);
        for (int i = 0; i < conditions.length; i += 2) {
            patterns[i].setLineNumber(lineNumber);
        }
    }

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
            h ^= patterns[i].hashCode() ^ conditions[i].hashCode();
        }
        return h;
    }

}

