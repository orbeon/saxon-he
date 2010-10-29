package net.sf.saxon.pattern;

import net.sf.saxon.Configuration;
import net.sf.saxon.expr.*;
import net.sf.saxon.expr.instruct.SlotManager;
import net.sf.saxon.om.NodeInfo;
import net.sf.saxon.om.SequenceIterator;
import net.sf.saxon.trans.XPathException;
import net.sf.saxon.type.ItemType;
import net.sf.saxon.type.Type;
import net.sf.saxon.value.SequenceType;

import java.util.Iterator;

/**
* A NodeSetPattern is a pattern based on an expression that is evaluated to return a set of nodes;
* a node matches the pattern if it is a member of this node-set.
 *
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
     * @param exp an expression that can be evaluated to return a node-set; a node matches the pattern
     * if it is present in this node-set. The expression must not depend on the focus, though it can depend on
     * other aspects of the dynamic context such as local or global variables.
     */
    public NodeSetPattern(Expression exp, Configuration config) {
        expression = exp;
        if ((expression.getDependencies() & StaticProperty.DEPENDS_ON_NON_DOCUMENT_FOCUS) != 0) {
            throw new IllegalArgumentException("Expression used in pattern must not depend on focus");
        }
//        itemType = (exp.getItemType(config.getTypeHierarchy()));
//        if (!(itemType instanceof NodeTest)) {
//            throw new IllegalArgumentException("Expression used in a pattern must evaluate to a node-set");
//        }
    }

    /**
    * Type-check the pattern.
    * Default implementation does nothing. This is only needed for patterns that contain
    * variable references or function calls.
    * @return the optimised Pattern
    */

    public Pattern analyze(ExpressionVisitor visitor, ItemType contextItemType) throws XPathException {
        expression = visitor.typeCheck(expression, contextItemType);
        RoleLocator role = new RoleLocator(RoleLocator.VARIABLE, expression.toString(), 0);
        expression = TypeChecker.staticTypeCheck(expression, SequenceType.NODE_SEQUENCE, false, role, visitor);
        itemType = expression.getItemType(visitor.getConfiguration().getTypeHierarchy());
        return this;
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

    public Iterator iterateSubExpressions() {
        return new MonoIterator(expression);
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
     * @param offer details of the offer, for example the offer to move
     *              expressions that don't depend on the context to an outer level in
     *              the containing expression
     * @param parent
     * @throws net.sf.saxon.trans.XPathException
     *          if any error is detected
     */

    public void promote(PromotionOffer offer, Expression parent) throws XPathException {
        expression = expression.promote(offer, parent);
    }

    /**
     * Replace a subexpression by a replacement subexpression
     * @param original    the expression to be replaced
     * @param replacement the new expression to be inserted in its place
     * @return true if the replacement was carried out
     */

    public boolean replaceSubExpression(Expression original, Expression replacement) {
        if (expression == original) {
            expression = replacement;
            return true;
        } else {
            return false;
        }
    }

    /**
     * Allocate slots to any variables used within the pattern
     * @param env         the static context in the XSLT stylesheet
     * @param slotManager the slot manager representing the stack frame for local variables
     * @param nextFree    the next slot that is free to be allocated @return the next slot that is free to be allocated
     */

     public int allocateSlots(StaticContext env, SlotManager slotManager, int nextFree) {
        return ExpressionTool.allocateSlots(expression, nextFree, slotManager);
    }


    /**
    * Determine whether this Pattern matches the given Node
    * @param e The NodeInfo representing the Element or other node to be tested against the Pattern
    * @return true if the node matches the Pattern, false otherwise
    */

    public boolean matches(NodeInfo e, XPathContext context) throws XPathException {
        SequenceIterator iter = expression.iterate(context);
        while (true) {
            NodeInfo node = (NodeInfo)iter.next();
            if (node == null) {
                return false;
            }
            if (node.isSameNodeInfo(e)) {
                return true;
            }
        }
    }

    /**
    * Determine the type of nodes to which this pattern applies.
    * @return Type.NODE (may be overridden in subclasses)
    */

    public int getNodeKind() {
        if (itemType instanceof NodeTest) {
            return ((NodeTest)itemType).getPrimitiveType();
        } else {
            return Type.NODE;
        }
    }

    /**
     * Get a NodeTest that all the nodes matching this pattern must satisfy
     */

    public NodeTest getNodeTest() {
        if (itemType instanceof NodeTest) {
            return (NodeTest)itemType;
        } else {
            return AnyNodeTest.getInstance();
        }
    }

    /**
     * Determine whether this pattern is the same as another pattern
     * @param other the other object
     */

    public boolean equals(Object other) {
        return (other instanceof NodeSetPattern) &&
                ((NodeSetPattern)other).expression.equals(expression);
    }

    /**
     * Hashcode supporting equals()
     */

    public int hashCode() {
        return 0x73108728 ^ expression.hashCode();
    }

}

//
// The contents of this file are subject to the Mozilla Public License Version 1.0 (the "License");
// you may not use this file except in compliance with the License. You may obtain a copy of the
// License at http://www.mozilla.org/MPL/
//
// Software distributed under the License is distributed on an "AS IS" basis,
// WITHOUT WARRANTY OF ANY KIND, either express or implied.
// See the License for the specific language governing rights and limitations under the License.
//
// The Original Code is: all this file.
//
// The Initial Developer of the Original Code is Michael H. Kay.
//
// Portions created by (your name) are Copyright (C) (your legal entity). All Rights Reserved.
//
// Contributor(s): none.
//