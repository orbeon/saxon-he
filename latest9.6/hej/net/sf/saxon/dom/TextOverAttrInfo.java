////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
// Copyright (c) 2014 Saxonica Limited.
// This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
// If a copy of the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
// This Source Code Form is "Incompatible With Secondary Licenses", as defined by the Mozilla Public License, v. 2.0.
////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

package net.sf.saxon.dom;

import net.sf.saxon.type.Type;
import org.w3c.dom.DOMException;
import org.w3c.dom.Node;

/**
 * This class represents a DOM text node that is the child of a DOM attribute node. The DOM attribute node
 * will be a wrapper over a Saxon attribute node or namespace node.
 */
public class TextOverAttrInfo extends TextOverNodeInfo {

    private AttrOverNodeInfo attr;

    public TextOverAttrInfo(AttrOverNodeInfo attr) {
        this.attr = attr;
        this.node = attr.getUnderlyingNodeInfo();
    }

    /**
     * Returns whether this text node contains <a href='http://www.w3.org/TR/2004/REC-xml-infoset-20040204#infoitem.character'>
     * element content whitespace</a>, often abusively called "ignorable whitespace". The text node is
     * determined to contain whitespace in element content during the load
     * of the document or if validation occurs while using
     * <code>Document.normalizeDocument()</code>.
     *
     * @since DOM Level 3
     */
    public boolean isElementContentWhitespace() {
        return false;
    }

    /**
     * Get the type of this node (node kind, in XPath terminology).
     * Note, the numbers assigned to node kinds
     * in Saxon (see {@link net.sf.saxon.type.Type}) are the same as those assigned in the DOM
     */

    public short getNodeType() {
        return Type.TEXT;
    }

    /**
     * Compare the position of the (other) node in document order with the reference node (this node).
     * DOM Level 3 method.
     *
     * @param other the other node.
     * @return Returns how the node is positioned relatively to the reference
     *         node.
     * @throws org.w3c.dom.DOMException
     */

    public short compareDocumentPosition(Node other) throws DOMException {
        final short DOCUMENT_POSITION_FOLLOWING = 0x04;
        if (other instanceof TextOverAttrInfo) {
            if (node.isSameNodeInfo(((TextOverAttrInfo) other).node)) {
                return 0;
            } else {
                return attr.compareDocumentPosition(((TextOverAttrInfo) other).attr);
            }
        } else if (other instanceof AttrOverNodeInfo) {
            if (node.isSameNodeInfo(((AttrOverNodeInfo) other).getUnderlyingNodeInfo())) {
                return DOCUMENT_POSITION_FOLLOWING;
            }
        }
        return attr.compareDocumentPosition(other);
    }

    /**
     * Find the parent node of this node.
     *
     * @return The Node object describing the containing element or root node.
     */

    public Node getParentNode() {
        return attr;
    }
}

