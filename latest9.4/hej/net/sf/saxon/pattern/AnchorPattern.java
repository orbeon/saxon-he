package net.sf.saxon.pattern;

import net.sf.saxon.expr.XPathContext;
import net.sf.saxon.expr.parser.ExpressionVisitor;
import net.sf.saxon.om.Item;
import net.sf.saxon.om.NodeInfo;
import net.sf.saxon.trans.XPathException;
import net.sf.saxon.type.ItemType;

/**
 * This is a special pattern that matches the "anchor node"; it is not used for XSLT patterns,
 * but for the selectors that arise when evaluating XPath expressions in streaming mode; the anchor
 * node is the context node for the streamed XPath evaluation.
 */
public class AnchorPattern extends Pattern {

    private NodeTest nodeTest = AnyNodeTest.getInstance();

    /**
     * Type-check the pattern.
     * @param visitor         the expression visitor
     * @param contextItemType the type of the context item at the point where the pattern
     *                        is defined. Set to null if it is known that the context item is undefined.
     * @return the optimised Pattern
     */

    public Pattern analyze(ExpressionVisitor visitor, ExpressionVisitor.ContextItemType contextItemType) throws XPathException {
        if (contextItemType.itemType instanceof NodeTest) {
            nodeTest = (NodeTest)contextItemType.itemType;
        }
        return this;
    }

    /**
     * Determine whether this pattern matches a given Node within the subtree rooted at a given
     * anchor node. This method is used when the pattern is used for streaming.
     * @param node    The NodeInfo representing the Element or other node to be tested against the Pattern
     * @param anchor  The anchor node, which must match any AnchorPattern subpattern
     * @param context The dynamic context. Only relevant if the pattern
     *                uses variables, or contains calls on functions such as document() or key().
     * @return true if the node matches the Pattern, false otherwise
     */

    public boolean matchesBeneathAnchor(NodeInfo node, NodeInfo anchor, XPathContext context) throws XPathException {
        return node == anchor;
    }

    /**
     * Determine whether this Pattern matches the given Node. This is the main external interface
     * for matching patterns: it sets current() to the node being tested
     * @param item    The NodeInfo representing the Element or other node to be tested against the Pattern
     * @param context The dynamic context. Only relevant if the pattern
     *                uses variables, or contains calls on functions such as document() or key().
     * @return true if the node matches the Pattern, false otherwise
     */

    public boolean matches(Item item, XPathContext context) throws XPathException {
        return false;
    }

    /**
    * Determine whether this Pattern matches the given Node. This is an internal interface used
    * for matching sub-patterns; it does not alter the value of current(). The default implementation
    * is identical to matches().
    * @param node The NodeInfo representing the Element or other node to be tested against the Pattern
    * @param anchor
     *@param context The dynamic context. Only relevant if the pattern
     * uses variables, or contains calls on functions such as document() or key().
     * @return true if the node matches the Pattern, false otherwise
    */

    protected boolean internalMatches(NodeInfo node, NodeInfo anchor, XPathContext context) throws XPathException {
        return node == anchor;
    }

    /**
     * Get a NodeTest that all the nodes matching this pattern must satisfy
     * @return a NodeTest, as specific as possible, which all the matching nodes satisfy
     */

    public ItemType getItemType() {
        return nodeTest;
    }

    /*@NotNull*/ public String toString() {
        return ".";
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
// The Original Code is: all this file
//
// The Initial Developer of the Original Code is Saxonica Limited.
// Portions created by ___ are Copyright (C) ___. All rights reserved.
//
// Contributor(s):
//