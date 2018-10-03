////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
// Copyright (c) 2018 Saxonica Limited.
// This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
// If a copy of the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
// This Source Code Form is "Incompatible With Secondary Licenses", as defined by the Mozilla Public License, v. 2.0.
////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

package net.sf.saxon.tree.wrapper;

import net.sf.saxon.om.NodeInfo;
import net.sf.saxon.tree.iter.AxisIterator;

import java.util.function.Function;


/**
 * A TypeStrippedNode is a view of a node, in a virtual tree that has type
 * annotations stripped from it.
 */

public class RebasedNode extends AbstractVirtualNode implements WrappingFunction {

    protected RebasedNode() {
    }

    /**
     * This constructor is protected: nodes should be created using the makeWrapper
     * factory method
     *
     * @param node   The node to be wrapped
     * @param parent The StrippedNode that wraps the parent of this node
     */

    protected RebasedNode(NodeInfo node, RebasedNode parent) {
        this.node = node;
        this.parent = parent;
    }

    /**
     * Factory method to wrap a node with a wrapper that implements the Saxon
     * NodeInfo interface.
     *
     * @param node       The underlying node
     * @param docWrapper The wrapper for the document node (must be supplied)
     * @param parent     The wrapper for the parent of the node (null if unknown)
     * @return The new wrapper for the supplied node
     */

    /*@NotNull*/
    public static RebasedNode makeWrapper(NodeInfo node,
                                          RebasedDocument docWrapper,
                                          RebasedNode parent) {
        RebasedNode wrapper = new RebasedNode(node, parent);
        wrapper.docWrapper = docWrapper;
        return wrapper;
    }

    /**
     * Factory method to wrap a node with a VirtualNode
     *
     * @param node   The underlying node
     * @param parent The wrapper for the parent of the node (null if unknown)
     * @return The new wrapper for the supplied node
     */

    /*@NotNull*/
    public VirtualNode makeWrapper(NodeInfo node, VirtualNode parent) {
        RebasedNode wrapper = new RebasedNode(node, (RebasedNode) parent);
        wrapper.docWrapper = this.docWrapper;
        return wrapper;
    }

    private Function<String, String> getBaseUriMappingFunction() {
        return ((RebasedDocument)docWrapper).getBaseUriMapper();
    }

    private Function<String, String> getSystemIdMappingFunction() {
        return ((RebasedDocument) docWrapper).getSystemIdMapper();
    }

    /**
     * Get the Base URI for the node, that is, the URI used for resolving a relative URI contained
     * in the node. In the JDOM model, base URIs are held only an the document level. We don't
     * currently take any account of xml:base attributes.
     */
    @Override
    public String getBaseURI() {
        String underBase = node.getBaseURI();
        return getBaseUriMappingFunction().apply(underBase == null ? "" : underBase);
    }

    /**
     * Get the System ID for the node.
     *
     * @return the System Identifier of the entity in the source document containing the node,
     * or null if not known. Note this is not the same as the base URI: the base URI can be
     * modified by xml:base, but the system ID cannot.
     */
    @Override
    public String getSystemId() {
        String underBase = node.getSystemId();
        return getSystemIdMappingFunction().apply(underBase == null ? "" : underBase);
    }

    /**
     * Determine whether this is the same node as another node.
     * <p>Note: a.isSameNode(b) if and only if generateId(a)==generateId(b)</p>
     *
     * @return true if this Node object and the supplied Node object represent the
     *         same node in the tree.
     */

    public boolean equals(Object other) {
        return other instanceof RebasedNode && node.equals(((RebasedNode) other).node);
    }

    /**
     * Determine the relative position of this node and another node, in document order.
     * The other node will always be in the same document.
     *
     * @param other The other node, whose position is to be compared with this node
     * @return -1 if this node precedes the other node, +1 if it follows the other
     *         node, or 0 if they are the same node. (In this case, isSameNode() will always
     *         return true, and the two nodes will produce the same result for generateId())
     */

    public int compareOrder(/*@NotNull*/ NodeInfo other) {
        if (other instanceof RebasedNode) {
            return node.compareOrder(((RebasedNode) other).node);
        } else {
            return node.compareOrder(other);
        }
    }

    /**
     * Get the NodeInfo object representing the parent of this node
     */

    /*@Nullable*/
    public NodeInfo getParent() {
        if (parent == null) {
            NodeInfo realParent = node.getParent();
            if (realParent != null) {
                parent = makeWrapper(realParent, (RebasedDocument) docWrapper, null);
            }
        }
        return parent;
    }

    /**
     * Return an iteration over the nodes reached by the given axis from this node
     *
     * @param axisNumber the axis to be used
     * @return a SequenceIterator that scans the nodes reached by the axis in turn.
     */

    /*@Nullable*/
    public AxisIterator iterateAxis(byte axisNumber) {
        return new WrappingIterator(node.iterateAxis(axisNumber), this, null);
    }

}

