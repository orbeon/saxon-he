////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
// Copyright (c) 2017 Saxonica Limited.
// This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
// If a copy of the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
// This Source Code Form is "Incompatible With Secondary Licenses", as defined by the Mozilla Public License, v. 2.0.
////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

package net.sf.saxon.pattern;

import com.saxonica.ee.stream.Posture;
import com.saxonica.ee.stream.Streamability;
import com.saxonica.ee.stream.Sweep;
import com.saxonica.ee.trans.ContextItemStaticInfoEE;
import net.sf.saxon.expr.*;
import net.sf.saxon.expr.instruct.SlotManager;
import net.sf.saxon.expr.parser.*;
import net.sf.saxon.om.Item;
import net.sf.saxon.om.NodeInfo;
import net.sf.saxon.om.SequenceIterator;
import net.sf.saxon.om.TreeInfo;
import net.sf.saxon.trace.ExpressionPresenter;
import net.sf.saxon.trans.XPathException;
import net.sf.saxon.tree.iter.ManualIterator;
import net.sf.saxon.type.ItemType;
import net.sf.saxon.type.UType;
import net.sf.saxon.value.SequenceType;

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

    protected Operand selectionOp;
    protected ItemType itemType;

    /**
     * Create a node-set pattern.
     *
     * @param exp an expression that can be evaluated to return a node-set; a node matches the pattern
     *            if it is present in this node-set. The expression must not depend on the focus, though it can depend on
     *            other aspects of the dynamic context such as local or global variables.
     */
    public NodeSetPattern(Expression exp) {
        selectionOp = new Operand(this, exp, OperandRole.NAVIGATE);
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
        return selectionOp;
    }

    /**
     * Get the underlying expression
     * @return the expression that returns all the selected nodes
     */

    public Expression getSelectionExpression() {
        return selectionOp.getChildExpression();
    }

    /**
     * Type-check the pattern.
     * Default implementation does nothing. This is only needed for patterns that contain
     * variable references or function calls.
     *
     * @return the optimised Pattern
     */

    public Pattern typeCheck(ExpressionVisitor visitor, ContextItemStaticInfo contextItemType) throws XPathException {
        selectionOp.setChildExpression(getSelectionExpression().typeCheck(visitor, contextItemType));
        RoleDiagnostic role = new RoleDiagnostic(RoleDiagnostic.MATCH_PATTERN, getSelectionExpression().toString(), 0);
        TypeChecker tc = visitor.getConfiguration().getTypeChecker(false);
        Expression checked = null;
        try {
            checked = tc.staticTypeCheck(
                    getSelectionExpression(), SequenceType.NODE_SEQUENCE, role, visitor);
        } catch (XPathException e) {
            visitor.issueWarning("Pattern will never match anything. " + e.getMessage(), getLocation());
            checked = Literal.makeEmptySequence();
        }
        selectionOp.setChildExpression(checked);
        itemType = getSelectionExpression().getItemType();
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
        return getSelectionExpression().getDependencies();
    }

    //#ifdefined STREAM

    /**
     * Test whether a pattern is motionless, that is, whether it can be evaluated against a node
     * without repositioning the input stream. This is a necessary condition for patterns used
     * as the match pattern of a streamed template rule.
     *
     * @return true if the pattern is motionless, that is, if it can be evaluated against a streamed
     * node without changing the position in the streamed input file
     */
    @Override
    public boolean isMotionless() {
        Expression select = getSelectionExpression();
        Streamability.getStreamability(select, new ContextItemStaticInfoEE(getItemType(), false, Posture.STRIDING), null);
        return Streamability.getSweep(select) == Sweep.MOTIONLESS;
    }
//#endif

    /**
     * Allocate slots to any variables used within the pattern
     *
     * @param slotManager the slot manager representing the stack frame for local variables
     * @param nextFree    the next slot that is free to be allocated @return the next slot that is free to be allocated
     */

    public int allocateSlots(SlotManager slotManager, int nextFree) {
        return ExpressionTool.allocateSlots(getSelectionExpression(), nextFree, slotManager);
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
        return getSelectionExpression().iterate(c2);
    }

    /**
     * Determine whether this Pattern matches the given Node
     *
     * @param item The NodeInfo representing the Element or other node to be tested against the Pattern
     * @return true if the node matches the Pattern, false otherwise
     */

    public boolean matches(Item item, XPathContext context) throws XPathException {
        if (item instanceof NodeInfo) {
            SequenceIterator iter = getSelectionExpression().iterate(context);
            return SingletonIntersectExpression.containsNode(iter, (NodeInfo)item);
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
        return getItemType().getUType();
    }

    /**
     * Get a NodeTest that all the nodes matching this pattern must satisfy
     */

    public ItemType getItemType() {
        if (itemType == null) {
            itemType = getSelectionExpression().getItemType();
        }
        if (itemType instanceof NodeTest) {
            return itemType;
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
                ((NodeSetPattern) other).getSelectionExpression().equals(getSelectionExpression());
    }

    /**
     * Hashcode supporting equals()
     */

    public int computeHashCode() {
        return 0x73108728 ^ getSelectionExpression().hashCode();
    }

    /**
     * Get a string representation of the pattern. This will be in a form similar to the
     * original pattern text, but not necessarily identical. It is not guaranteed to be
     * in legal pattern syntax.
     */

    public String toString() {
        return getSelectionExpression().toString();
    }

    /**
     * Copy a pattern. This makes a deep copy.
     *
     * @return the copy of the original pattern
     * @param rebindings variable bindings to be changed if encountered
     */

    /*@NotNull*/
    public Pattern copy(RebindingMap rebindings) {
        NodeSetPattern n = new NodeSetPattern(getSelectionExpression().copy(rebindings));
        ExpressionTool.copyLocationInfo(this, n);
        return n;
    }

    public void export(ExpressionPresenter presenter) throws XPathException {
        presenter.startElement("p.nodeSet");
        if (itemType != null) {
            presenter.emitAttribute("type", itemType.toExportString());
        }
        getSelectionExpression().export(presenter);
        presenter.endElement();
    }

}

