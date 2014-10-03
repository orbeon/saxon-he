////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
// Copyright (c) 2014 Saxonica Limited.
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
import net.sf.saxon.functions.Current;
import net.sf.saxon.om.AxisInfo;
import net.sf.saxon.om.Item;
import net.sf.saxon.om.NodeInfo;
import net.sf.saxon.om.SequenceIterator;
import net.sf.saxon.trans.XPathException;
import net.sf.saxon.tree.iter.AxisIterator;
import net.sf.saxon.tree.iter.ManualIterator;
import net.sf.saxon.type.ItemType;

import java.util.Iterator;

/**
 * A GeneralNodePattern represents a pattern which, because of the presence of positional
 * predicates or otherwise, can only be evaluated "the hard way", by evaluating the equivalent
 * expression with successive ancestors of the tested node as context item.
 */

public final class GeneralNodePattern extends Pattern {


    private Expression equivalentExpr = null;
    private NodeTest itemType = null;

    /**
     * Create a GeneralNodePattern
     *
     * @param expr     the "equivalent expression"
     * @param itemType a type that all matched nodes must satisfy
     */

    public GeneralNodePattern(Expression expr, NodeTest itemType) {
        equivalentExpr = expr;
        this.itemType = itemType;
    }

    /**
     * Test whether a pattern is motionless, that is, whether it can be evaluated against a node
     * without repositioning the input stream. This is a necessary condition for patterns used
     * as the match pattern of a streamed template rule.
     *
     * @param allowExtensions true if Saxon streamability extensions are allowed
     * @return true if the pattern is motionless, that is, if it can be evaluated against a streamed
     *         node without changing the position in the streamed input file
     */

    public boolean isMotionless(boolean allowExtensions) {
        return false;
    }

    /**
     * Type-check the pattern, performing any type-dependent optimizations.
     *
     * @param visitor         an expression visitor
     * @param contextItemType the type of the context item at the point where the pattern appears
     * @return the optimised Pattern
     */

    public Pattern analyze(ExpressionVisitor visitor, ContextItemStaticInfo contextItemType) throws XPathException {
        equivalentExpr = visitor.typeCheck(equivalentExpr, contextItemType);
        equivalentExpr = visitor.optimize(equivalentExpr, contextItemType);
        // See if the expression is now known to be non-positional
        if (equivalentExpr instanceof FilterExpression && !((FilterExpression)equivalentExpr).isFilterIsPositional()) {
            return PatternMaker.fromExpression(equivalentExpr, visitor.getConfiguration(), true)
                    .analyze(visitor, contextItemType);
        }
        return this;
    }

    /**
     * Get the dependencies of the pattern. The only possible dependency for a pattern is
     * on local variables. This is analyzed in those patterns where local variables may appear.
     */

    public int getDependencies() {
        return equivalentExpr.getDependencies() & StaticProperty.DEPENDS_ON_LOCAL_VARIABLES;
    }

    /**
     * Iterate over the subexpressions within this pattern
     */

    /*@NotNull*/
    public Iterator<Expression> iterateSubExpressions() {
        return new MonoIterator<Expression>(equivalentExpr);
    }

    /**
     * Replace one subexpression by a replacement subexpression
     *
     * @param original    the original subexpression
     * @param replacement the replacement subexpression
     * @return true if the original subexpression is found
     */

    public boolean replaceSubExpression(Expression original, Expression replacement) {
        return equivalentExpr.replaceOperand(original, replacement);
    }

    /**
     * Replace any calls on current() by a variable reference bound to the supplied binding
     */
    @Override
    public void bindCurrent(LocalBinding binding) {
        if (ExpressionTool.callsFunction(equivalentExpr, Current.FN_CURRENT)) {
            replaceCurrent(equivalentExpr, binding);
        }
    }

    /**
     * Allocate slots to any variables used within the pattern
     *
     * @param slotManager details of the stack frame
     * @param nextFree    the next slot that is free to be allocated @return the next slot that is free to be allocated
     */

    public int allocateSlots(SlotManager slotManager, int nextFree) {
        return ExpressionTool.allocateSlots(equivalentExpr, nextFree, slotManager);
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
     * @param parent the parent expression in the expression tree
     * @throws net.sf.saxon.trans.XPathException
     *          if any error is detected
     */

    public void promote(PromotionOffer offer, Expression parent) throws XPathException {
        Binding[] savedBindingList = offer.bindingList;
        equivalentExpr = equivalentExpr.promote(offer, parent);
        offer.bindingList = savedBindingList;
    }

    /**
     * Determine whether the pattern matches a given item.
     *
     * @param item the item to be tested
     * @return true if the pattern matches, else false
     */

    public boolean matches(Item item, XPathContext context) throws XPathException {
        if (!itemType.matches(item, context)) {
            return false;
        }
        AxisIterator anc = ((NodeInfo) item).iterateAxis(AxisInfo.ANCESTOR_OR_SELF);
        while (true) {
            NodeInfo a = anc.next();
            if (a == null) {
                return false;
            }
            if (matchesBeneathAnchor((NodeInfo) item, a, context)) {
                return true;
            }
        }
    }

    /**
     * Determine whether this pattern matches a given Node within the subtree rooted at a given
     * anchor node. This method is used when the pattern is used for streaming.
     *
     * @param node    The NodeInfo representing the Element or other node to be tested against the Pattern
     * @param anchor  The anchor node, which if present must match any AnchorPattern subpattern; may be null
     * @param context The dynamic context. Only relevant if the pattern
     *                uses variables, or contains calls on functions such as document() or key().
     * @return true if the node matches the Pattern, false otherwise
     */

    public boolean matchesBeneathAnchor(NodeInfo node, NodeInfo anchor, XPathContext context) throws XPathException {
        if (!itemType.matches(node, context)) {
            return false;
        }

        // for a positional pattern, we do it the hard way: test whether the
        // node is a member of the nodeset obtained by evaluating the
        // equivalent expression

        if (anchor == null) {
            AxisIterator ancestors = node.iterateAxis(AxisInfo.ANCESTOR_OR_SELF);
            while (true) {
                NodeInfo ancestor = ancestors.next();
                if (ancestor == null) {
                    return false;
                }
                if (matchesBeneathAnchor(node, ancestor, context)) {
                    return true;
                }
            }
        }

        // System.err.println("Testing positional pattern against node " + node.generateId());
        XPathContext c2 = context.newMinorContext();
        ManualIterator iter = new ManualIterator(anchor);
        c2.setCurrentIterator(iter);
        try {
            SequenceIterator nsv = equivalentExpr.iterate(c2);
            while (true) {
                NodeInfo n = (NodeInfo) nsv.next();
                if (n == null) {
                    return false;
                }
                if (n.isSameNodeInfo(node)) {
                    return true;
                }
            }
        } catch (XPathException.Circularity e) {
            throw e;
        } catch (XPathException e) {
            XPathException err = new XPathException("An error occurred matching pattern {" + toString() + "}: ", e);
            err.setXPathContext(c2);
            err.setErrorCodeQName(e.getErrorCodeQName());
            err.setLocator(this);
            c2.getController().recoverableError(err);
            return false;
        }
    }

    /**
     * Determine the types of nodes to which this pattern applies. Used for optimisation.
     * For patterns that match nodes of several types, return Node.NODE
     *
     * @return the type of node matched by this pattern. e.g. Node.ELEMENT or Node.TEXT
     */

    public int getNodeKind() {
        return itemType.getPrimitiveType();
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
        return itemType.getNodeKindMask();
    }


    /**
     * Determine the fingerprint of nodes to which this pattern applies.
     * Used for optimisation.
     *
     * @return the fingerprint of nodes matched by this pattern.
     */

    public int getFingerprint() {
        return itemType.getFingerprint();
    }

    /**
     * Get a NodeTest that all the nodes matching this pattern must satisfy
     */

    public ItemType getItemType() {
        return itemType;
    }


    /**
     * Determine whether this pattern is the same as another pattern
     *
     * @param other the other object
     */

    public boolean equals(Object other) {
        if (other instanceof GeneralNodePattern) {
            GeneralNodePattern lpp = (GeneralNodePattern) other;
            return equivalentExpr.equals(lpp.equivalentExpr);
        } else {
            return false;
        }
    }

    /**
     * hashcode supporting equals()
     */

    public int hashCode() {
        return 83641 ^ equivalentExpr.hashCode();
    }

}

