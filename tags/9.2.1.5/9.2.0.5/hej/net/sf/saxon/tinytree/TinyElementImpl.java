package net.sf.saxon.tinytree;
import net.sf.saxon.Configuration;
import net.sf.saxon.event.CopyInformee;
import net.sf.saxon.event.CopyNamespaceSensitiveException;
import net.sf.saxon.event.Receiver;
import net.sf.saxon.event.ReceiverOptions;
import net.sf.saxon.om.*;
import net.sf.saxon.trans.XPathException;
import net.sf.saxon.type.SchemaType;
import net.sf.saxon.type.SimpleType;
import net.sf.saxon.type.Type;
import net.sf.saxon.value.Value;


/**
  * A node in the XML parse tree representing an XML element.<P>
  * This class is an implementation of NodeInfo. The object is a wrapper around
  * one entry in the arrays maintained by the TinyTree. Note that the same node
  * might be represented by different TinyElementImpl objects at different times.
  * @author Michael H. Kay
  */

public final class TinyElementImpl extends TinyParentNodeImpl {

    /**
     * Constructor - create a tiny element node
     * @param tree the Tinytree containing the node
     * @param nodeNr the node number
    */

    public TinyElementImpl(TinyTree tree, int nodeNr) {
        this.tree = tree;
        this.nodeNr = nodeNr;
    }

    /**
    * Return the type of node.
    * @return Type.ELEMENT
    */

    public final int getNodeKind() {
        return Type.ELEMENT;
    }

    /**
    * Get the base URI of this element node. This will be the same as the System ID unless
    * xml:base has been used.
    */

    public String getBaseURI() {
        return Navigator.getBaseURI(this);
    }

    /**
    * Get the type annotation of this node, if any
    * Returns Type.UNTYPED_ANY if there is no type annotation
    */

    public int getTypeAnnotation() {
        return tree.getTypeAnnotation(nodeNr);
    }

    /**
     * Get the typed value of this node.
     * If there is no type annotation, we return the string value, as an instance
     * of xs:untypedAtomic
     */

    public SequenceIterator getTypedValue() throws XPathException {
        return tree.getTypedValueOfElement(this).iterate();
    }

    /**
     * Get the typed value. The result of this method will always be consistent with the method
     * {@link net.sf.saxon.om.Item#getTypedValue()}. However, this method is often more convenient and may be
     * more efficient, especially in the common case where the value is expected to be a singleton.
     *
     * @return the typed value. It will be a Value representing a sequence whose items are atomic
     *         values.
     * @since 8.5
     */

    public Value atomize() throws XPathException {
        return tree.getTypedValueOfElement(this);
    }
    
    /**
     * Get all namespace undeclarations and undeclarations defined on this element.
     *
     * @param buffer If this is non-null, and the result array fits in this buffer, then the result
     *               may overwrite the contents of this array, to avoid the cost of allocating a new array on the heap.
     * @return An array of integers representing the namespace declarations and undeclarations present on
     *         this element. For a node other than an element, return null. Otherwise, the returned array is a
     *         sequence of namespace codes, whose meaning may be interpreted by reference to the name pool. The
     *         top half word of each namespace code represents the prefix, the bottom half represents the URI.
     *         If the bottom half is zero, then this is a namespace undeclaration rather than a declaration.
     *         The XML namespace is never included in the list. If the supplied array is larger than required,
     *         then the first unused entry will be set to -1.
     *         <p/>
     *         <p>For a node other than an element, the method returns null.</p>
     */

    public int[] getDeclaredNamespaces(int[] buffer) {
        return getDeclaredNamespaces(tree, nodeNr, buffer);
    }

    /**
     * Static method to get all namespace undeclarations and undeclarations defined on a given element,
     * without instantiating the node object.
     * @param tree The tree containing the given element node
     * @param nodeNr The node number of the given element node within the tinyTree
     * @param buffer If this is non-null, and the result array fits in this buffer, then the result
     *               may overwrite the contents of this array, to avoid the cost of allocating a new array on the heap.
     * @return An array of integers representing the namespace declarations and undeclarations present on
     *         this element. For a node other than an element, return null. Otherwise, the returned array is a
     *         sequence of namespace codes, whose meaning may be interpreted by reference to the name pool. The
     *         top half word of each namespace code represents the prefix, the bottom half represents the URI.
     *         If the bottom half is zero, then this is a namespace undeclaration rather than a declaration.
     *         The XML namespace is never included in the list. If the supplied array is larger than required,
     *         then the first unused entry will be set to -1.
     *         <p/>
     *         <p>For a node other than an element, the method returns null.</p>
     */

    public static int[] getDeclaredNamespaces(TinyTree tree, int nodeNr, int[] buffer) {
        int ns = tree.beta[nodeNr]; // by convention
        if (ns>0 ) {
            int count = 0;
            while (ns < tree.numberOfNamespaces &&
                    tree.namespaceParent[ns] == nodeNr ) {
                count++;
                ns++;
            }
            if (count == 0) {
                return NodeInfo.EMPTY_NAMESPACE_LIST;
            } else if (buffer != null && count <= buffer.length) {
                System.arraycopy(tree.namespaceCode, tree.beta[nodeNr], buffer, 0, count);
                if (count < buffer.length) {
                    buffer[count] = -1;
                }
                return buffer;
            } else {
                int[] array = new int[count];
                System.arraycopy(tree.namespaceCode, tree.beta[nodeNr], array, 0, count);
                return array;
            }
        } else {
            return NodeInfo.EMPTY_NAMESPACE_LIST;
        }
    }

    /**
    * Get all the inscope namespaces for an element node. This method is better than the generic method
     * provided by {@link net.sf.saxon.om.NamespaceIterator} because it doesn't require the element node
     * (or its ancestors) to be instantiated as objects.
     * @param tree the TinyTree containing the element node whose in-scope namespaces are required
     * @param nodeNr the node number of the element node within the TinyTree. The caller is responsible
     * for ensuring that this is indeed an element node
     * @param buffer a buffer to hold the result, assuming it is large enough
     * @return an integer array of namespace codes representing the inscope namespaces of the given element.
     * The returned array will either be fully used, or it will contain a -1 entry marking the effective end
     * of the list of namespace codes. Note that only distinct declared namespaces are included in the result;
     * it does not contain any entries for namespace undeclarations or for overridden declarations.
     */

    public static int[] getInScopeNamespaces(TinyTree tree, int nodeNr, int[] buffer) {

        if (buffer == null || buffer.length == 0) {
            buffer = new int[10];
        }
        buffer[0] = NamespaceConstant.XML_NAMESPACE_CODE;
        int used = 1;

        if (tree.usesNamespaces) {
            do {
                // gather the namespaces declared for this node
                int ns = tree.beta[nodeNr]; // by convention
                if (ns>0 ) {
                    while (ns < tree.numberOfNamespaces &&
                            tree.namespaceParent[ns] == nodeNr ) {
                        int nscode = tree.namespaceCode[ns];

                        // See if the prefix has already been declared; if so, this declaration is ignored
                        short prefixCode = (short)(nscode >> 16);
                        boolean duplicate = false;
                        for (int i=0; i<used; i++) {
                            if ((buffer[i] >> 16) == prefixCode) {
                                duplicate = true;
                                break;
                            }
                        }
                        if (!duplicate) {
                            if (used >= buffer.length) {
                                int[] b2 = new int[used*2];
                                System.arraycopy(buffer, 0, b2, 0, used);
                                buffer = b2;
                            }
                            buffer[used++] = nscode;
                        }
                        ns++;
                    }
                }

                // move on to the parent of this node
                nodeNr = getParentNodeNr(tree, nodeNr);
            } while (nodeNr != -1);

            // The list of namespaces we have built up includes undeclarations as well as declarations.
            // We now remove the undeclarations (which have a URI code of zero)

            int j = 0;
            for (int i=0; i<used; i++) {
                int nscode = buffer[i];
                if ((nscode & 0xffff) != 0) {
                    buffer[j++] = nscode;
                }
            }
            used = j;
        }

        // If there are unused entries at the end of the array, add a -1 to mark the end
        if (used < buffer.length) {
            buffer[used] = -1;
        }

        return buffer;
    }


    /**
    * Get the value of a given attribute of this node
    * @param fingerprint The fingerprint of the attribute name
    * @return the attribute value if it exists or null if not
    */

    public String getAttributeValue(int fingerprint) {
        int a = tree.alpha[nodeNr];
        if (a<0) return null;
        while (a < tree.numberOfAttributes && tree.attParent[a] == nodeNr) {
            if ((tree.attCode[a] & 0xfffff) == fingerprint ) {
                return tree.attValue[a].toString();
            }
            a++;
        }
        return null;
    }

    /**
    * Copy this node to a given receiver
    * @param whichNamespaces indicates which namespaces should be copied: all, none,
    * or local (i.e., those not declared on a parent element)
    */

    public void copy(Receiver receiver, int whichNamespaces, boolean copyAnnotations, int locationId) throws XPathException {

        // Based on an algorithm supplied by Ruud Diterwich

        // Performance measurements show that this achieves no speed-up over the OLD version
        // (in 7.4). So might as well switch back.

		// control vars
		short level = -1;
		boolean closePending = false;
		short startLevel = tree.depth[nodeNr];
		boolean first = true;
        boolean disallowNamespaceSensitiveContent =
                whichNamespaces == NO_NAMESPACES &&
                copyAnnotations;
        Configuration config = tree.getConfiguration();
        int next = nodeNr;
        CopyInformee informee = receiver.getPipelineConfiguration().getCopyInformee();

		// document.diagnosticDump();

        do {

			// determine node depth
			short nodeLevel = tree.depth[next];

			// extra close required?
			if (closePending) {
				level++;
			}

			// close former elements
			for (; level > nodeLevel; level--) {
				receiver.endElement();
			}

			// new node level
			level = nodeLevel;

			// output depends on node kind
			switch (tree.nodeKind[next]) {
				case Type.ELEMENT : {

					// start element
                    final int typeCode = (copyAnnotations ? tree.getTypeAnnotation(next): StandardNames.XS_UNTYPED);
                    if (disallowNamespaceSensitiveContent) {
                        checkNotNamespaceSensitive(config, typeCode);
                    }
                    if (informee != null) {
                        locationId = informee.notifyElementNode(tree.getNode(next));
                    }
                    int nameCode = tree.nameCode[next];
                    receiver.startElement(nameCode,
                            typeCode,
                            locationId, (first ? 0 : ReceiverOptions.NAMESPACE_OK));

					// there is an element to close
					closePending = true;

					// output namespaces
                    if (whichNamespaces != NO_NAMESPACES && tree.usesNamespaces) {
                        if (first) {
                            switch (whichNamespaces) {
                                case NodeInfo.NO_NAMESPACES:
                                    break;  // (unreachable)
                                case NodeInfo.LOCAL_NAMESPACES:
                                    int[] localNamespaces = getDeclaredNamespaces(null);
                                    for (int i=0; i<localNamespaces.length; i++) {
                                        int ns = localNamespaces[i];
                                        if (ns == -1) {
                                            break;
                                        }
                                        receiver.namespace(ns, 0);
                                    }
                                    break;
                                case NodeInfo.ALL_NAMESPACES:
                                    NamespaceCodeIterator.sendNamespaces(this, receiver);
                                    break;
                            }
                        } else {
                            int ns = tree.beta[next]; // by convention
                            if (ns>0 ) {
                                while (ns < tree.numberOfNamespaces &&
                                        tree.namespaceParent[ns] == next ) {
                                    int nscode = tree.namespaceCode[ns];
                                    receiver.namespace(nscode, 0);
                                    ns++;
                                }
                            }
                        }
                    }
					first = false;

					// output attributes

					int att = tree.alpha[next];
					if (att >= 0) {
                        while (att < tree.numberOfAttributes && tree.attParent[att] == next ) {
                            int attCode = tree.attCode[att];
                            int attType = (copyAnnotations ?
                                    tree.getAttributeAnnotation(att) :
                                    StandardNames.XS_UNTYPED_ATOMIC);
                            if (disallowNamespaceSensitiveContent) {
                                checkNotNamespaceSensitive(config, attType);
                            }
                            receiver.attribute(attCode, attType, tree.attValue[att], locationId, 0);
                            att++;
                        }
                    }

					// start content
					receiver.startContent();
					break;
                }
				case Type.TEXT: {

					// don't close text nodes
					closePending = false;

					// output characters
                    final CharSequence value = TinyTextImpl.getStringValue(tree, next);
                    receiver.characters(value, locationId, ReceiverOptions.WHOLE_TEXT_NODE);
					break;
                }

                case Type.WHITESPACE_TEXT: {

					// don't close text nodes
					closePending = false;

					// output characters
                    final CharSequence value = WhitespaceTextImpl.getStringValueCS(tree, next);
                    receiver.characters(value, locationId, ReceiverOptions.WHOLE_TEXT_NODE);
					break;
                }

				case Type.COMMENT : {

					// don't close text nodes
					closePending = false;

					// output copy of comment
                    int start = tree.alpha[next];
                    int len = tree.beta[next];
                    if (len>0) {
                        receiver.comment(tree.commentBuffer.subSequence(start, start+len), locationId, 0);
                    } else {
                        receiver.comment("", 0, 0);
                    }
					break;
                }
				case Type.PROCESSING_INSTRUCTION : {

					// don't close text nodes
					closePending = false;

					// output copy of PI
					NodeInfo pi = tree.getNode(next);
					receiver.processingInstruction(pi.getLocalPart(), pi.getStringValue(), locationId, 0);
					break;
                }

                case Type.PARENT_POINTER : {
                    closePending = false;
                }
			}

			next++;

		} while (next < tree.numberOfNodes && tree.depth[next] > startLevel);

		// close all remaining elements
		if (closePending) {
			level++;
		}
		for (; level > startLevel; level--) {
			receiver.endElement();
		}
    }

    private void checkNotNamespaceSensitive(Configuration config, final int typeCode) throws XPathException {
        SchemaType type = config.getSchemaType(typeCode & NamePool.FP_MASK);
        if (type instanceof SimpleType && ((SimpleType)type).isNamespaceSensitive()) {
            throw new CopyNamespaceSensitiveException(
                    "Cannot copy QName or NOTATION values without copying namespaces");
//            err.setErrorCode((language == Configuration.XSLT ? "XTTE0950" : "XQTY0086"));
//            throw err;
        }
    }

    /**
     * Get the namespace URI corresponding to a given prefix. Return null
     * if the prefix is not in scope.
     *
     * @param prefix     the namespace prefix. May be the zero-length string, indicating
     *                   that there is no prefix. This indicates either the default namespace or the
     *                   null namespace, depending on the value of useDefault.
     * @param useDefault true if the default namespace is to be used when the
     *                   prefix is "". If false, the method returns "" when the prefix is "".
     * @return the uri for the namespace, or null if the prefix is not in scope.
     *         The "null namespace" is represented by the pseudo-URI "".
     */

    public String getURIForPrefix(String prefix, boolean useDefault) {
        if (!useDefault && (prefix==null || prefix.length()==0)) {
            return "";
        }
        int prefixCode = getNamePool().getCodeForPrefix(prefix);
        if (prefixCode == -1) {
            return null;
        }
        int ns = tree.beta[nodeNr]; // by convention
        if (ns>0 ) {
            while (ns < tree.numberOfNamespaces &&
                    tree.namespaceParent[ns] == nodeNr ) {
                int nscode = tree.namespaceCode[ns];
                if ((nscode >> 16) == prefixCode) {
                    int uriCode = nscode & 0xffff;
                    if (uriCode == 0) {
                        // this is a namespace undeclaration, so the prefix is not in scope
                        if (prefixCode == 0) {
                            // the namespace xmlns="" is always in scope
                            return "";
                        } else {
                            return null;
                        }
                    } else {
                        return getNamePool().getURIFromURICode((short)uriCode);
                    }
                }
                ns++;
            }
        }

        // now search the namespaces defined on the ancestor nodes.

        NodeInfo parent = getParent();
        if (parent instanceof NamespaceResolver) {
            return ((NamespaceResolver)parent).getURIForPrefix(prefix, useDefault);
        }
        return null;
    }

    /**
     * Determine whether this node has the is-id property
     *
     * @return true if the node is an ID
     */

    public boolean isId() {
        // this looks very inefficient, but the method isn't actually used...
        return tree.isIdElement(nodeNr);
    }

    /**
     * Determine whether this node has the is-idref property
     *
     * @return true if the node is an IDREF or IDREFS element or attribute
     */

    public boolean isIdref() {
        return tree.isIdrefElement(nodeNr);
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
// The new copy() routine (in version 7.4.1) is contributed by Ruud Diterwich
//
// Portions created by (your name) are Copyright (C) (your legal entity). All Rights Reserved.
//
// Contributor(s): none.
//
