////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
// Copyright (c) 2015 Saxonica Limited.
// This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
// If a copy of the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
// This Source Code Form is "Incompatible With Secondary Licenses", as defined by the Mozilla Public License, v. 2.0.
////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

package net.sf.saxon.pattern;

import net.sf.saxon.om.NodeInfo;
import net.sf.saxon.om.NodeName;
import net.sf.saxon.tree.tiny.TinyTree;
import net.sf.saxon.type.SchemaType;
import net.sf.saxon.type.Type;
import net.sf.saxon.type.UType;

/**
 * An AnyChildNodePattern is the pattern node(), which matches any node except a root node,
 * an attribute node, or a namespace node: in other words, any node that is potentially the child of another
 * node. But it matches the node whether or not it actually has a parent.
 */

public final class AnyChildNodeTest extends NodeTest {

    private final static AnyChildNodeTest THE_INSTANCE = new AnyChildNodeTest();

    /**
     * Get the singular instance of this class
     *
     * @return the singular instance
     */

    public static AnyChildNodeTest getInstance() {
        return THE_INSTANCE;
    }

    private AnyChildNodeTest() {
    }

    /**
     * Get the corresponding {@link net.sf.saxon.type.UType}. A UType is a union of primitive item
     * types.
     *
     * @return the smallest UType that subsumes this item type
     */
    public UType getUType() {
        return UType.CHILD_NODE_KINDS;
    }

    /**
     * Test whether this node test is satisfied by a given node. This method is only
     * fully supported for a subset of NodeTests, because it doesn't provide all the information
     * needed to evaluate all node tests. In particular (a) it can't be used to evaluate a node
     * test of the form element(N,T) or schema-element(E) where it is necessary to know whether the
     * node is nilled, and (b) it can't be used to evaluate a node test of the form
     * document-node(element(X)). This in practice means that it is used (a) to evaluate the
     * simple node tests found in the XPath 1.0 subset used in XML Schema, and (b) to evaluate
     * node tests where the node kind is known to be an attribute.
     *
     * @param nodeKind   The kind of node to be matched
     * @param name       identifies the expanded name of the node to be matched.
     *                   The value should be null for a node with no name.
     * @param annotation The actual content type of the node
     */
    @Override
    public boolean matches(int nodeKind, NodeName name, SchemaType annotation) {
        return  nodeKind == Type.ELEMENT ||
                nodeKind == Type.TEXT ||
                nodeKind == Type.COMMENT ||
                nodeKind == Type.PROCESSING_INSTRUCTION;
    }

    /**
     * Test whether this node test is satisfied by a given node on a TinyTree. The node
     * must be a document, element, text, comment, or processing instruction node.
     * This method is provided
     * so that when navigating a TinyTree a node can be rejected without
     * actually instantiating a NodeInfo object.
     *
     * @param tree   the TinyTree containing the node
     * @param nodeNr the number of the node within the TinyTree
     * @return true if the node matches the NodeTest, otherwise false
     */

    public boolean matches(TinyTree tree, int nodeNr) {
        int nodeKind = tree.nodeKind[nodeNr];
        return  nodeKind == Type.ELEMENT ||
                nodeKind == Type.TEXT ||
                nodeKind == Type.WHITESPACE_TEXT ||
                nodeKind == Type.COMMENT ||
                nodeKind == Type.PROCESSING_INSTRUCTION;
    }

    /**
     * Test whether this node test is satisfied by a given node. This alternative
     * method is used in the case of nodes where calculating the fingerprint is expensive,
     * for example DOM or JDOM nodes.
     *
     * @param node the node to be matched
     */

    public boolean matchesNode(NodeInfo node) {
        int nodeKind = node.getNodeKind();
        return  nodeKind == Type.ELEMENT ||
                nodeKind == Type.TEXT ||
                nodeKind == Type.COMMENT ||
                nodeKind == Type.PROCESSING_INSTRUCTION;
    }


    /**
     * Determine the default priority to use if this pattern appears as a match pattern
     * for a template with no explicit priority attribute.
     */

    public double getDefaultPriority() {
        return -0.5;
    }

    /**
     * Get a mask indicating which kinds of nodes this NodeTest can match. This is a combination
     * of bits: 1<<Type.ELEMENT for element nodes, 1<<Type.TEXT for text nodes, and so on.
     */

    public int getNodeKindMask() {
        return 1 << Type.ELEMENT | 1 << Type.TEXT | 1 << Type.COMMENT | 1 << Type.PROCESSING_INSTRUCTION;
    }

    /*@NotNull*/
    public String toString() {
        return "( element() | text() | comment() | processing-instruction() )";
    }

    /**
     * Returns a hash code value for the object.
     */

    public int hashCode() {
        return "AnyChildNodeTest".hashCode();
    }

}

