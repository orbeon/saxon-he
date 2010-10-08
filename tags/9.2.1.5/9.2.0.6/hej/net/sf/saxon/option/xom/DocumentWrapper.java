package net.sf.saxon.option.xom;

import net.sf.saxon.Configuration;
import net.sf.saxon.om.DocumentInfo;
import net.sf.saxon.om.NamePool;
import net.sf.saxon.om.NodeInfo;
import net.sf.saxon.om.StandardNames;
import net.sf.saxon.type.Type;
import nu.xom.Attribute;
import nu.xom.Document;
import nu.xom.Element;
import nu.xom.Node;

import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;

/**
 * The root node of an XPath tree. (Or equivalently, the tree itself).
 * <P>
 * This class is used not only for a document, but also for the root
 * of a document-less tree fragment.
 *
 * @author Michael H. Kay
 * @author Wolfgang Hoschek (ported net.sf.saxon.jdom to XOM)
 */

public class DocumentWrapper extends NodeWrapper implements DocumentInfo {

	protected Configuration config;

	protected String baseURI;

	protected long documentNumber;

    private HashMap<String, NodeInfo> idIndex;

	/**
	 * Create a Saxon wrapper for a XOM root node
	 *
	 * @param root
	 *            The XOM root node
	 * @param baseURI
	 *            The base URI for all the nodes in the tree
	 * @param config
	 *            The configuration which defines the name pool used for all
	 *            names in this tree
	 */
	public DocumentWrapper(Node root, String baseURI, Configuration config) {
		super(root, null, 0);
		if (root.getParent() != null)
			throw new IllegalArgumentException("root node must not have a parent node");
		this.baseURI = baseURI;
        docWrapper = this;
        setConfiguration(config);
	}

	/**
	 * Wrap a node in the XOM document.
	 *
	 * @param node
	 *            The node to be wrapped. This must be a node in the same
	 *            document (the system does not check for this).
	 * @return the wrapping NodeInfo object
	 */

	public NodeInfo wrap(Node node) {
		if (node == this.node) {
            return this;
        }
		return makeWrapper(node, this);
	}

	/**
	 * Set the configuration, which defines the name pool used for all names in
	 * this document. This is always called after a new document has been
	 * created. The implementation must register the name pool with the
	 * document, so that it can be retrieved using getNamePool(). It must also
	 * call NamePool.allocateDocumentNumber(), and return the relevant document
	 * number when getDocumentNumber() is subsequently called.
	 *
	 * @param config The configuration to be used
	 */

	public void setConfiguration(Configuration config) {
		this.config = config;
        documentNumber = config.getDocumentNumberAllocator().allocateDocumentNumber();
	}

    /**
	 * Get the configuration previously set using setConfiguration
	 */

	public Configuration getConfiguration() {
		return config;
	}

	/**
	 * Get the name pool used for the names in this document
	 *
	 * @return the name pool in which all the names used in this document are
	 *         registered
	 */

	public NamePool getNamePool() {
		return config.getNamePool();
	}

	/**
	 * Get the unique document number for this document (the number is unique
	 * for all documents within a NamePool)
	 *
	 * @return the unique number identifying this document within the name pool
	 */

	public long getDocumentNumber() {
		return documentNumber;
	}

    /**
	 * Get the element with a given ID, if any
	 *
	 * @param id
	 *            the required ID value
	 * @param getParent
     * @return the element with the given ID, or null if there is no such ID
	 *         present (or if the parser has not notified attributes as being of
	 *         type ID).
	 */

	public NodeInfo selectID(String id, boolean getParent) {
		if (idIndex == null) {
			Element elem;
			switch (nodeKind) {
				case Type.DOCUMENT :
					elem = ((Document) node).getRootElement();
					break;
				case Type.ELEMENT :
					elem = (Element) node;
					break;
				default:
					return null;
			}
			idIndex = new HashMap<String, NodeInfo>(50);
			buildIDIndex(elem);
		}
		return idIndex.get(id);
	}


	private void buildIDIndex(Element elem) {
		// walk the tree in reverse document order, to satisfy the XPath 1.0 rule
		// that says if an ID appears twice, the first one wins
		for (int i=elem.getChildCount(); --i >= 0 ; ) {
			Node child = elem.getChild(i);
			if (child instanceof Element) {
				buildIDIndex((Element)child);
			}
		}
		for (int i=elem.getAttributeCount(); --i >= 0 ; ) {
			Attribute att = elem.getAttribute(i);
			if (att.getType() == Attribute.Type.ID) {
				idIndex.put(att.getValue(), wrap(elem));
			}
		}
	}

    /**
     * Get the list of unparsed entities defined in this document
     * @return an Iterator, whose items are of type String, containing the names of all
     *         unparsed entities defined in this document. If there are no unparsed entities or if the
     *         information is not available then an empty iterator is returned
     */

    public Iterator<String> getUnparsedEntityNames() {
        return Collections.EMPTY_LIST.iterator();
    }    

    /**
	 * Get the unparsed entity with a given name
	 *
	 * @param name the name of the entity
	 * @return null: XOM does not provide access to unparsed entities
	 */

	public String[] getUnparsedEntity(String name) {
		return null;
	}

    /**
     * Get the type annotation of this node, if any. Returns -1 for kinds of
     * nodes that have no annotation, and for elements annotated as untyped, and
     * attributes annotated as untypedAtomic.
     * @return the type annotation of the node.
     * @see net.sf.saxon.type.Type
     */

    public int getTypeAnnotation() {
        return StandardNames.XS_UNTYPED;
    }
}

//
// The contents of this file are subject to the Mozilla Public License Version
// 1.0 (the "License");
// you may not use this file except in compliance with the License. You may
// obtain a copy of the
// License at http://www.mozilla.org/MPL/
//
// Software distributed under the License is distributed on an "AS IS" basis,
// WITHOUT WARRANTY OF ANY KIND, either express or implied.
// See the License for the specific language governing rights and limitations
// under the License.
//
// The Original Code is: all this file.
//
// The Initial Developer of the Original Code is Michael Kay and Wolfgang Hoschek
// (WH converted MK's JDOM code to XOM; MK subsequently refined it)
//
// Portions created by (your name) are Copyright (C) (your legal entity). All
// Rights Reserved.
//
// Contributor(s): none.
//
