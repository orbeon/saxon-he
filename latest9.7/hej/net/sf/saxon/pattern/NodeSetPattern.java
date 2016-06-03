////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
// Copyright (c) 2015 Saxonica Limited.
// This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
// If a copy of the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
// This Source Code Form is "Incompatible With Secondary Licenses", as defined by the Mozilla Public License, v. 2.0.
////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

package net.sf.saxon.pattern;

import com.saxonica.ee.stream.Sweep;
import net.sf.saxon.expr.*;
import net.sf.saxon.expr.instruct.SlotManager;
import net.sf.saxon.expr.parser.*;
import net.sf.saxon.om.TreeInfo;
import net.sf.saxon.om.Item;
import net.sf.saxon.om.NodeInfo;
import net.sf.saxon.om.SequenceIterator;
import net.sf.saxon.trace.ExpressionPresenter;
import net.sf.saxon.trans.XPathException;
import net.sf.saxon.tree.iter.ManualIterator;
import net.sf.saxon.type.ItemType;
import net.sf.saxon.type.UType;
import net.sf.saxon.value.SequenceType;

import java.util.Iterator;
import java.util.Map;

/**
 * A NodeSetPattern is a pattern based on an expression that is evaluated to return a set of nodes;
 * a node matches the pattern if it is a member of this node-set.
 * <p/>
 * <p>In XSLT 2.0 there are two forms of NodeSetPattern allowed, represented by calls on the id() and
 * key() functions. In XSLT 3.0, additional forms are allowed, for example a variable reference, and
 * a call to the doc() function. This class provides the general capability to use any expression
 * at the head of a pattern. This is used also to support streaming, where streaming XPath expressions
 * are mapped to patterns.</p>
 */

public class NodeSetPattern extends Pattern {

    protected Expression expression;
    protected ItemType itemType;

    /**
     * Create a node-set pattern.
     *
     * @param exp an expression that can be evaluated to return a node-set; a node matches the pattern
     *            if it is present in this node-set. The expression must not depend on the focus, though it can depend on
     *            other aspects of the dynamic context such as local or global variables.
     */
    public NodeSetPattern(Expression exp) {
        expression = exp;
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
        return new Operand(this, expression, OperandRole.NAVIGATE);
    }

    /**
     * Get the underlying expression
     * @return the expression that returns all the selected nodes
     */

    public Expression getSelectionExpression() {
        return expression;
    }

    /**
     * Type-check the pattern.
     * Default implementation does nothing. This is only needed for patterns that contain
     * variable references or function calls.
     *
     * @return the optimised Pattern
     */

    public Pattern typeCheck(ExpressionVisitor visitor, ContextItemStaticInfo contextItemType) throws XPathException {
        expression = expression.typeCheck(visitor, contextItemType);
        RoleDiagnostic role = new RoleDiagnostic(RoleDiagnostic.VARIABLE, expression.toString(), 0);
        expression = TypeChecker.staticTypeCheck(expression, SequenceType.NODE_SEQUENCE, false, role, visitor);
        itemType = expression.getItemType();
        return this;
    }

    /**
     * Set the item type, that is, the type of nodes/items which the pattern will match
     * @param type the item type that the pattern will match
     */

    public void setItemType(ItemType type) {
        this.itemType = type;
    }

    /**
     * Get the dependencies of the pattern. The only possible dependency for a pattern is
     * on local variables. This is analyzed in those patterns where local variables may appear.
     */

    public int getDependencies() {
        return expression.getDependencies();
    }

    /**
     * Iterate over the subexpressions within this pattern
     */

    /*@NotNull*/
    public Iterator<Expression> iterateSubExpressions() {
        return new MonoIterator<Expression>(expression);
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
     * @throws net.sf.saxon.trans.XPathException if any error is detected
     */

    public void promote(PromotionOffer offer, Expression parent) throws XPathException {
        expression = expression.promote(offer);
    }

    //#ifdefined STREAM

    /**
     * Test whether a pattern is motionless, that is, whether it can be evaluated against a node
     * without repositioning the input stream. This is a necessary condition for patterns used
     * as the match pattern of a streamed template rule.
     *
     * @param allowExtensions
     * @return true if the pattern is motionless, that is, if it can be evaluated against a streamed
     * node without changing the position in the streamed input file
     */
    @Override
    public boolean isMotionless(boolean allowExtensions) {
        expression.getStreamability(allowExtensions, new ContextItemStaticInfo(getItemType(), false, true), null);
        return expression.getSweep() == Sweep.MOTIONLESS;
    }
//#endif

    /**
     * Allocate slots to any variables used within the pattern
     *
     * @param slotManager the slot manager representing the stack frame for local variables
     * @param nextFree    the next slot that is free to be allocated @return the next slot that is free to be allocated
     */

    public int allocateSlots(SlotManager slotManager, int nextFree) {
        return ExpressionTool.allocateSlots(expression, nextFree, slotManager);
    }

    /**
     * Select nodes in a document using this PatternFinder.
     *
     * @param doc     the document node at the root of a tree
     * @param context the dynamic evaluation context
     * @return an iterator over the selected nodes in the document.
     */

    public SequenceIterator selectNodes(TreeInfo doc, XPathContext context) throws XPathException {
        XPathContext c2 = context.newMinorContext();
        ManualIterator mi = new ManualIterator(doc.getRootNode());
        c2.setCurrentIterator(mi);
        return expression.iterate(c2);
    }

    /**
     * Determine whether this Pattern matches the given Node
     *
     * @param item The NodeInfo representing the Element or other node to be tested against the Pattern
     * @return true if the node matches the Pattern, false otherwise
     */

    public boolean matches(Item item, XPathContext context) throws XPathException {
        if (item instanceof NodeInfo) {
            NodeInfo e = (NodeInfo) item;
            SequenceIterator iter = expression.iterate(context);
            while (true) {
                NodeInfo node = (NodeInfo) iter.next();
                if (node == null) {
                    return false;
                }
                if (node.isSameNodeInfo(e)) {
                    return true;
                }
            }
        } else {
            return false;
        }
    }

    /**
     * Get a UType indicating which kinds of items this Pattern can match.
     *
     * @return a UType indicating all the primitive types of item that the pattern can match.
     */
    @Override
    public UType getUType() {
        return itemType.getUType();
    }

    /**
     * Get a NodeTest that all the nodes matching this pattern must satisfy
     */

    public ItemType getItemType() {
        if (itemType instanceof NodeTest) {
            return (NodeTest) itemType;
        } else {
            return AnyNodeTest.getInstance();
        }
    }

    /**
     * Determine whether this pattern is the same as another pattern
     *
     * @param other the other object
     */

    public boolean equals(Object other) {
        return (other instanceof NodeSetPattern) &&
                ((NodeSetPattern) other).expression.equals(expression);
    }

    /**
     * Hashcode supporting equals()
     */

    public int hashCode() {
        return 0x73108728 ^ expression.hashCode();
    }

    /**
     * Copy a pattern. This makes a deep copy.
     *
     * @return the copy of the original pattern
     * @param rebindings
     */

    /*@NotNull*/
    public Pattern copy(Map<IdentityWrapper<Binding>, Binding> rebindings) {
        NodeSetPattern n = new NodeSetPattern(expression.copy(rebindings));
        ExpressionTool.copyLocationInfo(this, n);
        return n;
    }

    public void export(ExpressionPresenter presenter) throws XPathException {
        presenter.startElement("p.nodeSet");
        if (itemType != null) {
            presenter.emitAttribute("type", itemType.toString());
        }
        expression.export(presenter);
        presenter.endElement();
    }

}

