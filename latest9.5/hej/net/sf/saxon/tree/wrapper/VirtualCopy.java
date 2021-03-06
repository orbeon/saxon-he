////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
// Copyright (c) 2013 Saxonica Limited.
// This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
// If a copy of the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
// This Source Code Form is "Incompatible With Secondary Licenses", as defined by the Mozilla Public License, v. 2.0.
////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

package net.sf.saxon.tree.wrapper;

import net.sf.saxon.Configuration;
import net.sf.saxon.event.Receiver;
import net.sf.saxon.om.*;
import net.sf.saxon.pattern.AnyNodeTest;
import net.sf.saxon.pattern.NodeTest;
import net.sf.saxon.trans.XPathException;
import net.sf.saxon.tree.iter.AxisIterator;
import net.sf.saxon.tree.iter.SingletonIterator;
import net.sf.saxon.tree.util.FastStringBuffer;
import net.sf.saxon.tree.util.NamespaceIterator;
import net.sf.saxon.tree.util.Navigator;
import net.sf.saxon.type.SchemaType;
import net.sf.saxon.type.Type;

import javax.xml.transform.SourceLocator;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * This class represents a node that is a virtual copy of another node: that is, it behaves as a node that's the
 * same as another node, but has different identity. Moreover, this class can create a virtual copy of a subtree,
 * so that the parent of the virtual copy is null rather than being a virtual copy of the parent of the original.
 * This means that none of the axes when applied to the virtual copy is allowed to stray outside the subtree.
 * The virtual copy is implemented by means of a reference to the node of which
 * it is a copy, but methods that are sensitive to node identity return a different result.
 */

public class VirtualCopy implements NodeInfo, SourceLocator {

    protected String systemId;
    protected long documentNumber;
    protected NodeInfo original;
    protected VirtualCopy parent;
    protected NodeInfo root;        // the node forming the root of the subtree that was copied

    /**
     * Protected constructor: create a virtual copy of a node
     *
     * @param base the node to be copied
     */

    protected VirtualCopy(/*@NotNull*/ NodeInfo base) {
        original = base;
        systemId = base.getBaseURI();
    }

    /**
     * Public factory method: create a virtual copy of a node
     *
     * @param original the node to be copied
     * @param root     the root of the tree containing the node to be copied
     * @return the virtual copy. If the original was already a virtual copy, this will be a virtual copy
     *         of the real underlying node.
     */

    public static VirtualCopy makeVirtualCopy(NodeInfo original, NodeInfo root) {

        VirtualCopy vc;
        // Don't allow copies of copies of copies: define the new copy in terms of the original
        while (original instanceof VirtualCopy) {
            original = ((VirtualCopy) original).original;
        }
        while (root instanceof VirtualCopy) {
            root = ((VirtualCopy) root).original;
        }
        if (original.getNodeKind() == Type.DOCUMENT) {
            vc = new VirtualDocumentCopy((DocumentInfo) original);
        } else {
            vc = new VirtualCopy(original);
        }
        vc.root = root;
        return vc;
    }

    /**
     * Wrap a node in a VirtualCopy.
     *
     * @param node the node to be wrapped
     * @return a virtual copy of the node
     */

    /*@NotNull*/
    protected VirtualCopy wrap(/*@NotNull*/ NodeInfo node) {
        return new VirtualCopy(node);
    }

    /**
     * To implement {@link Sequence}, this method returns the item itself
     * @return this item
     */

    public Item head() {
        return this;
    }

    /**
     * To implement {@link Sequence}, this method returns a singleton iterator
     * that delivers this item in the form of a sequence
     * @return a singleton iterator that returns this item
     */

    public SequenceIterator iterate() {
        return SingletonIterator.makeIterator(this);
    }

    /**
     * Set the unique document number of the virtual document. This method must be called to ensure
     * that nodes in the virtual copy have unique node identifiers
     *
     * @param documentNumber the document number to be allocated. This can be obtained from the
     *                       {@link net.sf.saxon.tree.util.DocumentNumberAllocator} which is accessible from the Configuration using
     *                       {@link net.sf.saxon.Configuration#getDocumentNumberAllocator()}
     */

    public void setDocumentNumber(long documentNumber) {
        this.documentNumber = documentNumber;
    }

    /**
     * Get the kind of node. This will be a value such as Type.ELEMENT or Type.ATTRIBUTE
     *
     * @return an integer identifying the kind of node. These integer values are the
     *         same as those used in the DOM
     * @see net.sf.saxon.type.Type
     */

    public int getNodeKind() {
        return original.getNodeKind();
    }

    /**
     * Determine whether this is the same node as another node.
     * Note: a.isSameNodeInfo(b) if and only if generateId(a)==generateId(b).
     * This method has the same semantics as isSameNode() in DOM Level 3, but
     * works on Saxon NodeInfo objects rather than DOM Node objects.
     *
     * @param other the node to be compared with this node
     * @return true if this NodeInfo object and the supplied NodeInfo object represent
     *         the same node in the tree.
     */

    public boolean isSameNodeInfo(/*@NotNull*/ NodeInfo other) {
        return other instanceof VirtualCopy &&
                documentNumber == other.getDocumentNumber() &&
                original.isSameNodeInfo(((VirtualCopy) other).original);


    }

    /**
     * The equals() method compares nodes for identity. It is defined to give the same result
     * as isSameNodeInfo().
     *
     * @param other the node to be compared with this node
     * @return true if this NodeInfo object and the supplied NodeInfo object represent
     *         the same node in the tree.
     * @since 8.7 Previously, the effect of the equals() method was not defined. Callers
     *        should therefore be aware that third party implementations of the NodeInfo interface may
     *        not implement the correct semantics. It is safer to use isSameNodeInfo() for this reason.
     *        The equals() method has been defined because it is useful in contexts such as a Java Set or HashMap.
     */

    public boolean equals(Object other) {
        return other instanceof NodeInfo && isSameNodeInfo((NodeInfo) other);
    }

    /**
     * The hashCode() method obeys the contract for hashCode(): that is, if two objects are equal
     * (represent the same node) then they must have the same hashCode()
     *
     * @since 8.7 Previously, the effect of the equals() and hashCode() methods was not defined. Callers
     *        should therefore be aware that third party implementations of the NodeInfo interface may
     *        not implement the correct semantics.
     */

    public int hashCode() {
        return original.hashCode() ^ ((int) (documentNumber & 0x7fffffff) << 19);
    }

    /**
     * Get the System ID for the node.
     *
     * @return the System Identifier of the entity in the source document
     *         containing the node, or null if not known. Note this is not the
     *         same as the base URI: the base URI can be modified by xml:base, but
     *         the system ID cannot.
     */

    public String getSystemId() {
        return systemId;
    }

    /**
     * Get the Base URI for the node, that is, the URI used for resolving a relative URI contained
     * in the node. This will be the same as the System ID unless xml:base has been used.
     *
     * @return the base URI of the node
     */

    /*@Nullable*/
    public String getBaseURI() {
        return Navigator.getBaseURI(this);
    }

    /**
     * Get line number
     *
     * @return the line number of the node in its original source document; or
     *         -1 if not available
     */

    public int getLineNumber() {
        return original.getLineNumber();
    }

    /**
     * Get column number
     *
     * @return the column number of the node in its original source document; or -1 if not available
     */

    public int getColumnNumber() {
        return original.getColumnNumber();
    }

    /**
     * Determine the relative position of this node and another node, in document order.
     * The other node will always be in the same document.
     *
     * @param other The other node, whose position is to be compared with this
     *              node
     * @return -1 if this node precedes the other node, +1 if it follows the
     *         other node, or 0 if they are the same node. (In this case,
     *         isSameNode() will always return true, and the two nodes will
     *         produce the same result for generateId())
     */

    public int compareOrder(/*@NotNull*/ NodeInfo other) {
        return original.compareOrder(((VirtualCopy) other).original);
    }

    /**
     * Determine the relative position of this node and another node, in document order,
     * distinguishing whether the first node is a preceding, following, descendant, ancestor,
     * or the same node as the second.
     * <p/>
     * The other node must always be in the same tree; the effect of calling this method
     * when the two nodes are in different trees is undefined. If either node is a namespace
     * or attribute node, the method should throw UnsupportedOperationException.
     *
     * @param other The other node, whose position is to be compared with this
     *              node
     * @return {@link net.sf.saxon.om.AxisInfo#PRECEDING} if this node is on the preceding axis of the other node;
     *         {@link net.sf.saxon.om.AxisInfo#FOLLOWING} if it is on the following axis; {@link net.sf.saxon.om.AxisInfo#ANCESTOR} if the first node is an
     *         ancestor of the second; {@link net.sf.saxon.om.AxisInfo#DESCENDANT} if the first is a descendant of the second;
     *         {@link net.sf.saxon.om.AxisInfo#SELF} if they are the same node.
     * @throws UnsupportedOperationException if either node is an attribute or namespace
     * @since 9.5
     */
    public int comparePosition(NodeInfo other) {
        return original.comparePosition(((VirtualCopy) other).original);
    }

    /**
     * Return the string value of the node. The interpretation of this depends on the type
     * of node. For an element it is the accumulated character content of the element,
     * including descendant elements.
     *
     * @return the string value of the node
     */

    public String getStringValue() {
        return original.getStringValue();
    }

    /**
     * Get the value of the item as a CharSequence. This is in some cases more efficient than
     * the version of the method that returns a String.
     */

    public CharSequence getStringValueCS() {
        return original.getStringValueCS();
    }

    /**
     * Get name code. The name code is a coded form of the node name: two nodes
     * with the same name code have the same namespace URI, the same local name,
     * and the same prefix. By masking the name code with &0xfffff, you get a
     * fingerprint: two nodes with the same fingerprint have the same local name
     * and namespace URI.
     *
     * @return an integer name code, which may be used to obtain the actual node
     *         name from the name pool
     * @see net.sf.saxon.om.NamePool#allocate allocate
     * @see net.sf.saxon.om.NamePool#getFingerprint getFingerprint
     */

    public int getNameCode() {
        return original.getNameCode();
    }

    /**
     * Get fingerprint. The fingerprint is a coded form of the expanded name
     * of the node: two nodes
     * with the same name code have the same namespace URI and the same local name.
     * A fingerprint of -1 should be returned for a node with no name.
     *
     * @return an integer fingerprint; two nodes with the same fingerprint have
     *         the same expanded QName
     */

    public int getFingerprint() {
        return original.getFingerprint();
    }

    /**
     * Get the local part of the name of this node. This is the name after the ":" if any.
     *
     * @return the local part of the name. For an unnamed node, returns "". Unlike the DOM
     *         interface, this returns the full name in the case of a non-namespaced name.
     */

    public String getLocalPart() {
        return original.getLocalPart();
    }

    /**
     * Get the URI part of the name of this node. This is the URI corresponding to the
     * prefix, or the URI of the default namespace if appropriate.
     *
     * @return The URI of the namespace of this node. For an unnamed node,
     *         or for a node with an empty prefix, return an empty
     *         string.
     */

    public String getURI() {
        return original.getURI();
    }

    /**
     * Get the prefix of the name of the node. This is defined only for elements and attributes.
     * If the node has no prefix, or for other kinds of node, return a zero-length string.
     *
     * @return The prefix of the name of the node.
     */

    public String getPrefix() {
        return original.getPrefix();
    }

    /**
     * Get the display name of this node. For elements and attributes this is [prefix:]localname.
     * For unnamed nodes, it is an empty string.
     *
     * @return The display name of this node. For a node with no name, return
     *         an empty string.
     */

    public String getDisplayName() {
        return original.getDisplayName();
    }

    /**
     * Get the configuration
     */

    public Configuration getConfiguration() {
        return original.getConfiguration();
    }

    /**
     * Get the NamePool that holds the namecode for this node
     *
     * @return the namepool
     */

    public NamePool getNamePool() {
        return original.getNamePool();
    }

    /**
     * Get the type annotation of this node, if any.
     *
     * @return the type annotation of the node.
     * @see net.sf.saxon.type.Type
     */

    public int getTypeAnnotation() {
        return original.getTypeAnnotation();
    }

    /**
     * Get the type annotation of this node, if any. The type annotation is represented as
     * SchemaType object.
     * <p/>
     * <p>Types derived from a DTD are not reflected in the result of this method.</p>
     *
     * @return For element and attribute nodes: the type annotation derived from schema
     *         validation (defaulting to xs:untyped and xs:untypedAtomic in the absence of schema
     *         validation). For comments, text nodes, processing instructions, and namespaces: null.
     *         For document nodes, either xs:untyped if the document has not been validated, or
     *         xs:anyType if it has.
     * @since 9.4
     */
    public SchemaType getSchemaType() {
        return original.getSchemaType();
    }

    /**
     * Get the NodeInfo object representing the parent of this node
     *
     * @return the parent of this node; null if this node has no parent
     */

    /*@Nullable*/
    public NodeInfo getParent() {
        if (original.isSameNodeInfo(root)) {
            return null;
        }
        if (parent == null) {
            NodeInfo basep = original.getParent();
            if (basep == null) {
                return null;
            }
            parent = wrap(basep);
            parent.setDocumentNumber(documentNumber);
        }
        return parent;
    }

    /**
     * Return an iteration over all the nodes reached by the given axis from this node
     *
     * @param axisNumber an integer identifying the axis; one of the constants
     *                   defined in class net.sf.saxon.om.Axis
     * @return an AxisIterator that scans the nodes reached by the axis in
     *         turn.
     * @throws UnsupportedOperationException if the namespace axis is
     *                                       requested and this axis is not supported for this implementation.
     * @see net.sf.saxon.om.AxisInfo
     */

    public AxisIterator iterateAxis(byte axisNumber) {
        return iterateAxis(axisNumber, AnyNodeTest.getInstance());
    }

    /**
     * Return an iteration over all the nodes reached by the given axis from this node
     * that match a given NodeTest
     *
     * @param axisNumber an integer identifying the axis; one of the constants
     *                   defined in class net.sf.saxon.om.Axis
     * @param nodeTest   A pattern to be matched by the returned nodes; nodes
     *                   that do not match this pattern are not included in the result
     * @return an AxisIterator that scans the nodes reached by the axis in
     *         turn.
     * @throws UnsupportedOperationException if the namespace axis is
     *                                       requested and this axis is not supported for this implementation.
     * @see net.sf.saxon.om.AxisInfo
     */

    public AxisIterator iterateAxis(byte axisNumber, NodeTest nodeTest) {
        VirtualCopy newParent = null;
        if (axisNumber == AxisInfo.CHILD || axisNumber == AxisInfo.ATTRIBUTE || axisNumber == AxisInfo.NAMESPACE) {
            newParent = this;
        } else if (axisNumber == AxisInfo.SELF || axisNumber == AxisInfo.PRECEDING_SIBLING || axisNumber == AxisInfo.FOLLOWING_SIBLING) {
            newParent = parent;
        }
        NodeInfo root;
        if (AxisInfo.isSubtreeAxis[axisNumber]) {
            root = null;
        } else {
            root = this.root;
        }
        return makeCopier(original.iterateAxis(axisNumber, nodeTest), newParent, root);
    }

    /**
     * Get the string value of a given attribute of this node
     *
     * @param uri   the namespace URI of the attribute name. Supply the empty string for an attribute
     *              that is in no namespace
     * @param local the local part of the attribute name.
     * @return the attribute value if it exists, or null if it does not exist. Always returns null
     *         if this node is not an element.
     */
    public String getAttributeValue(/*@NotNull*/ String uri, /*@NotNull*/ String local) {
        return original.getAttributeValue(uri, local);
    }

    /**
     * Get the root node of the tree containing this node
     *
     * @return the NodeInfo representing the top-level ancestor of this node.
     *         This will not necessarily be a document node
     */

    /*@Nullable*/
    public NodeInfo getRoot() {
        NodeInfo n = this;
        while (true) {
            NodeInfo p = n.getParent();
            if (p == null) {
                return n;
            }
            n = p;
        }
    }

    /**
     * Get the root node, if it is a document node.
     *
     * @return the DocumentInfo representing the containing document. If this
     *         node is part of a tree that does not have a document node as its
     *         root, return null.
     */

    /*@Nullable*/
    public DocumentInfo getDocumentRoot() {
        NodeInfo root = getRoot();
        if (root.getNodeKind() == Type.DOCUMENT) {
            return (DocumentInfo) root;
        }
        return null;
    }

    /**
     * Determine whether the node has any children. <br />
     * Note: the result is equivalent to <br />
     * getEnumeration(Axis.CHILD, AnyNodeTest.getInstance()).hasNext()
     *
     * @return True if the node has one or more children
     */

    public boolean hasChildNodes() {
        return original.hasChildNodes();
    }

    /**
     * Get a character string that uniquely identifies this node.
     * Note: a.isSameNode(b) if and only if generateId(a)==generateId(b)
     *
     * @param buffer a buffer, to which will be appended
     *               a string that uniquely identifies this node, across all
     *               documents.
     */

    public void generateId(/*@NotNull*/ FastStringBuffer buffer) {
        buffer.append("d");
        buffer.append(Long.toString(documentNumber));
        original.generateId(buffer);
    }

    /**
     * Get the document number of the document containing this node. For a free-standing
     * orphan node, just return the hashcode.
     */

    public long getDocumentNumber() {
        return documentNumber;
    }

    /**
     * Copy this node to a given outputter
     *
     * @param out         the Receiver to which the node should be copied
     * @param copyOptions a selection of the options defined in {@link net.sf.saxon.om.CopyOptions}
     * @param locationId  Identifies the location of the instruction
     */

    public void copy(Receiver out, int copyOptions, int locationId) throws XPathException {
        original.copy(out, copyOptions, locationId);
    }

    /**
     * Get all namespace undeclarations and undeclarations defined on this element.
     *
     * @param buffer If this is non-null, and the result array fits in this buffer, then the result
     *               may overwrite the contents of this array, to avoid the cost of allocating a new array on the heap.
     * @return An array representing the namespace declarations and undeclarations present on
     *         this element. For a node other than an element, return null. Otherwise, the returned array is a
     *         sequence of namespace bindings
     *         The XML namespace is never included in the list. If the supplied array is larger than required,
     *         then the first unused entry will be set to -1.
     *         <p/>
     *         <p>For a node other than an element, the method returns null.</p>
     */

    public NamespaceBinding[] getDeclaredNamespaces(NamespaceBinding[] buffer) {
        List<NamespaceBinding> allNamespaces = new ArrayList<NamespaceBinding>(20);
        Iterator<NamespaceBinding> iter = NamespaceIterator.iterateNamespaces(original);
        while (iter.hasNext()) {
            allNamespaces.add(iter.next());
        }
        return allNamespaces.toArray(new NamespaceBinding[allNamespaces.size()]);
    }

    /**
     * Set the system identifier for this Source.
     * <p/>
     * <p>The system identifier is optional if the source does not
     * get its data from a URL, but it may still be useful to provide one.
     * The application can use a system identifier, for example, to resolve
     * relative URIs and to include in error messages and warnings.</p>
     *
     * @param systemId The system identifier as a URL string.
     */
    public void setSystemId(String systemId) {
        this.systemId = systemId;
    }

    /**
     * Get the typed value.
     *
     * @return the typed value. If requireSingleton is set to true, the result will always be an
     *         AtomicValue. In other cases it may be a Value representing a sequence whose items are atomic
     *         values.
     * @since 8.5
     */

    public AtomicSequence atomize() throws XPathException {
        return original.atomize();
    }


    /**
     * Determine whether this node has the is-id property
     *
     * @return true if the node is an ID
     */

    public boolean isId() {
        return original.isId();
    }

    /**
     * Determine whether this node has the is-idref property
     *
     * @return true if the node is an IDREF or IDREFS element or attribute
     */

    public boolean isIdref() {
        return original.isIdref();
    }

    /**
     * Determine whether the node has the is-nilled property
     *
     * @return true if the node has the is-nilled property
     */

    public boolean isNilled() {
        return original.isNilled();
    }

    /**
     * Return the public identifier for the current document event.
     * <p/>
     * <p>The return value is the public identifier of the document
     * entity or of the external parsed entity in which the markup that
     * triggered the event appears.</p>
     *
     * @return A string containing the public identifier, or
     *         null if none is available.
     * @see #getSystemId
     */
    /*@Nullable*/
    public String getPublicId() {
        return (original instanceof SourceLocator ? ((SourceLocator) original).getPublicId() : null);
    }

    /**
     * Create an iterator that makes and returns virtual copies of nodes on the original tree
     *
     * @param axis      the axis to be navigated
     * @param newParent the parent of the nodes in the new virtual tree (may be null)
     * @param root      the root of the virtual tree
     * @return the iterator that does the copying
     */

    protected VirtualCopier makeCopier(AxisIterator axis, VirtualCopy newParent, NodeInfo root) {
        return new VirtualCopier(axis, newParent, root);
    }

    /**
     * VirtualCopier implements the XPath axes as applied to a VirtualCopy node. It works by
     * applying the requested axis to the node of which this is a copy. There are two
     * complications: firstly, all nodes encountered must themselves be (virtually) copied
     * to give them a new identity. Secondly, axes that stray outside the subtree rooted at
     * the original copied node must be truncated.
     */

    protected class VirtualCopier implements AxisIterator {

        protected AxisIterator base;
        private VirtualCopy parent;
        protected NodeInfo subtreeRoot;
        /*@Nullable*/
        private NodeInfo current;

        public VirtualCopier(AxisIterator base, VirtualCopy parent, NodeInfo subtreeRoot) {
            this.base = base;
            this.parent = parent;
            this.subtreeRoot = subtreeRoot;
        }

        /**
         * Move to the next node, without returning it. Returns true if there is
         * a next node, false if the end of the sequence has been reached. After
         * calling this method, the current node may be retrieved using the
         * current() function.
         */

        public boolean moveNext() {
            return (next() != null);
        }


        /**
         * Get the next item in the sequence. <BR>
         *
         * @return the next Item. If there are no more nodes, return null.
         */

        /*@Nullable*/
        public NodeInfo next() {
            NodeInfo next = base.next();
            if (next != null) {
                if (subtreeRoot != null) {
                    // we're only interested in nodes within the subtree that was copied.
                    // Assert: once we find a node outside this subtree, all further nodes will also be outside
                    //         the subtree.
                    if (!isAncestorOrSelf(subtreeRoot, next)) {
                        return null;
                    }
                }
                VirtualCopy vc = createCopy(next, root);
                vc.parent = parent;
                vc.systemId = systemId;
                vc.documentNumber = documentNumber;
                next = vc;
            }
            current = next;
            return next;
        }

        /**
         * Get the current item in the sequence.
         *
         * @return the current item, that is, the item most recently returned by
         *         next()
         */

        /*@Nullable*/
        public NodeInfo current() {
            return current;
        }

        /**
         * Get the current position
         *
         * @return the position of the current item (the item most recently
         *         returned by next()), starting at 1 for the first node
         */

        public int position() {
            return base.position();
        }

        public void close() {
            base.close();
        }

        /**
         * Return an iterator over an axis, starting at the current node.
         *
         * @param axis the axis to iterate over, using a constant such as
         *             {@link net.sf.saxon.om.AxisInfo#CHILD}
         * @param test a predicate to apply to the nodes before returning them.
         * @throws NullPointerException if there is no current node
         */

        public AxisIterator iterateAxis(byte axis, NodeTest test) {
            return current.iterateAxis(axis, test);
        }

        /**
         * Return the atomized value of the current node.
         *
         * @return the atomized value.
         * @throws NullPointerException if there is no current node
         */

        public Sequence atomize() throws XPathException {
            return current.atomize();
        }

        /**
         * Return the string value of the current node.
         *
         * @return the string value, as an instance of CharSequence.
         * @throws NullPointerException if there is no current node
         */

        public CharSequence getStringValue() {
            return current.getStringValueCS();
        }

        /**
         * Get another iterator over the same sequence of items, positioned at the
         * start of the sequence
         *
         * @return a new iterator over the same sequence
         */

        /*@NotNull*/
        public AxisIterator getAnother() {
            return new VirtualCopier(base.getAnother(), parent, subtreeRoot);
        }

        /**
         * Get properties of this iterator, as a bit-significant integer.
         *
         * @return the properties of this iterator. This will be some combination of
         *         properties such as {@link #GROUNDED}, {@link #LAST_POSITION_FINDER},
         *         and {@link #LOOKAHEAD}. It is always
         *         acceptable to return the value zero, indicating that there are no known special properties.
         *         It is acceptable for the properties of the iterator to change depending on its state.
         */

        public int getProperties() {
            return 0;
        }

        /**
         * Test whether a node is an ancestor-or-self of another
         *
         * @param a the putative ancestor
         * @param d the putative descendant
         * @return true if a is an ancestor of d
         */

        private boolean isAncestorOrSelf(/*@NotNull*/ NodeInfo a, NodeInfo d) {
            while (true) {
                if (a.isSameNodeInfo(d)) {
                    return true;
                }
                d = d.getParent();
                if (d == null) {
                    return false;
                }
            }
        }

        /**
         * Method to create the virtual copy of a node encountered when navigating. This method
         * is separated out so that it can be overridden in a subclass.
         *
         * @param node the node to be copied
         * @param root the root of the tree
         * @return the virtual copy
         */

        protected VirtualCopy createCopy(NodeInfo node, NodeInfo root) {
            return VirtualCopy.makeVirtualCopy(node, root);
        }

    }

}

