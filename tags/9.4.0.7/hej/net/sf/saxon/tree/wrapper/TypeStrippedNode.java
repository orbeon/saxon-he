package net.sf.saxon.tree.wrapper;

import net.sf.saxon.event.Receiver;
import net.sf.saxon.om.CopyOptions;
import net.sf.saxon.om.DocumentInfo;
import net.sf.saxon.om.NodeInfo;
import net.sf.saxon.om.StandardNames;
import net.sf.saxon.trans.XPathException;
import net.sf.saxon.tree.iter.AxisIterator;
import net.sf.saxon.type.BuiltInAtomicType;
import net.sf.saxon.type.SchemaType;
import net.sf.saxon.type.Type;
import net.sf.saxon.type.Untyped;
import net.sf.saxon.value.UntypedAtomicValue;
import net.sf.saxon.value.Value;


/**
 * A TypeStrippedNode is a view of a node, in a virtual tree that has type
 * annotations stripped from it.
*/

public class TypeStrippedNode extends AbstractVirtualNode implements WrappingFunction {

    protected TypeStrippedNode() {}

    /**
     * This constructor is protected: nodes should be created using the makeWrapper
     * factory method
     * @param node    The node to be wrapped
     * @param parent  The StrippedNode that wraps the parent of this node
     */

    protected TypeStrippedNode(NodeInfo node, TypeStrippedNode parent) {
        this.node = node;
        this.parent = parent;
    }

    /**
     * Factory method to wrap a node with a wrapper that implements the Saxon
     * NodeInfo interface.
     * @param node        The underlying node
     * @param docWrapper  The wrapper for the document node (must be supplied)
     * @param parent      The wrapper for the parent of the node (null if unknown)
     * @return            The new wrapper for the supplied node
     */

    /*@NotNull*/ public static TypeStrippedNode makeWrapper(NodeInfo node,
                                       TypeStrippedDocument docWrapper,
                                       TypeStrippedNode parent) {
        TypeStrippedNode wrapper = new TypeStrippedNode(node, parent);
        wrapper.docWrapper = docWrapper;
        return wrapper;
    }

    /**
     * Factory method to wrap a node with a VirtualNode
     * @param node        The underlying node
     * @param parent      The wrapper for the parent of the node (null if unknown)
     * @return            The new wrapper for the supplied node
     */

    /*@NotNull*/ public VirtualNode makeWrapper(NodeInfo node, VirtualNode parent) {
        TypeStrippedNode wrapper = new TypeStrippedNode(node, (TypeStrippedNode)parent);
        wrapper.docWrapper = this.docWrapper;
        return wrapper;
    }

    /**
     * Get the typed value. The result of this method will always be consistent with the method
     * {@link net.sf.saxon.om.Item#getTypedValue()}. However, this method is often more convenient and may be
     * more efficient, especially in the common case where the value is expected to be a singleton.
     *
     * @return the typed value. If requireSingleton is set to true, the result will always be an
     *         AtomicValue. In other cases it may be a Value representing a sequence whose items are atomic
     *         values.
     * @since 8.5
     */

    /*@NotNull*/ public Value atomize() throws XPathException {
        return new UntypedAtomicValue(getStringValueCS());
    }

    /**
    * Get the type annotation
    * @return untyped or untypedAtomic (the purpose of this class is to strip the type annotation)
    */

    public int getTypeAnnotation() {
        if (getNodeKind() == Type.ELEMENT) {
            return StandardNames.XS_UNTYPED;
        } else {
            return StandardNames.XS_UNTYPED_ATOMIC;
        }
    }

    /**
     * Get the type annotation
     *
     * @return the type annotation of the base node
     */
    @Override
    public SchemaType getSchemaType() {
        if (getNodeKind() == Type.ELEMENT) {
            return Untyped.getInstance();
        } else {
            return BuiltInAtomicType.UNTYPED_ATOMIC;
        }
    }

    /**
    * Determine whether this is the same node as another node. <br />
    * Note: a.isSameNode(b) if and only if generateId(a)==generateId(b)
    * @return true if this Node object and the supplied Node object represent the
    * same node in the tree.
    */

    public boolean isSameNodeInfo(/*@NotNull*/ NodeInfo other) {
        if (other instanceof TypeStrippedNode) {
            return node.isSameNodeInfo(((TypeStrippedNode)other).node);
        } else {
            return node.isSameNodeInfo(other);
        }
    }

    /**
    * Determine the relative position of this node and another node, in document order.
    * The other node will always be in the same document.
    * @param other The other node, whose position is to be compared with this node
    * @return -1 if this node precedes the other node, +1 if it follows the other
    * node, or 0 if they are the same node. (In this case, isSameNode() will always
    * return true, and the two nodes will produce the same result for generateId())
    */

    public int compareOrder(/*@NotNull*/ NodeInfo other) {
        if (other instanceof TypeStrippedNode) {
            return node.compareOrder(((TypeStrippedNode)other).node);
        } else {
            return node.compareOrder(other);
        }
    }

    /**
    * Get the NodeInfo object representing the parent of this node
    */

    /*@Nullable*/ public NodeInfo getParent() {
        if (parent==null) {
            NodeInfo realParent = node.getParent();
            if (realParent != null) {
                parent = makeWrapper(realParent, ((TypeStrippedDocument)docWrapper), null);
            }
        }
        return parent;
    }

    /**
    * Return an iteration over the nodes reached by the given axis from this node
    * @param axisNumber the axis to be used
    * @return a SequenceIterator that scans the nodes reached by the axis in turn.
    */

    /*@Nullable*/ public AxisIterator iterateAxis(byte axisNumber) {
        return new WrappingIterator(node.iterateAxis(axisNumber), this, null);
    }

    /**
    * Get the root (document) node
    * @return the DocumentInfo representing the containing document
    */

    /*@NotNull*/ public DocumentInfo getDocumentRoot() {
        return (DocumentInfo)docWrapper;
    }

    /**
    * Copy this node to a given outputter (deep copy)
    */

    public void copy(Receiver out, int copyOptions, int locationId) throws XPathException {
        node.copy(out, copyOptions & ~CopyOptions.TYPE_ANNOTATIONS, locationId);
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