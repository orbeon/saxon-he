package net.sf.saxon.dom;

import net.sf.saxon.Configuration;
import net.sf.saxon.om.DocumentInfo;
import net.sf.saxon.om.NamePool;
import net.sf.saxon.om.NodeInfo;
import net.sf.saxon.om.StandardNames;
import net.sf.saxon.type.Type;
import org.w3c.dom.*;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

/**
 * The document node of a tree implemented as a wrapper around a DOM Document.
 */

public class DocumentWrapper extends NodeWrapper implements DocumentInfo {

    protected Configuration config;
    protected String baseURI;
    protected long documentNumber;
    protected boolean domLevel3;

    /**
     * Wrap a DOM Document or DocumentFragment node
     * @param doc a DOM Document or DocumentFragment node
     * @param baseURI the base URI of the document
     * @param config the Saxon configuration
     */

    public DocumentWrapper(Node doc, String baseURI, Configuration config) {
        super(doc, null, 0);
        if (doc.getNodeType() != Node.DOCUMENT_NODE && doc.getNodeType() != Node.DOCUMENT_FRAGMENT_NODE) {
            throw new IllegalArgumentException("Node must be a DOM Document or DocumentFragment");
        }
        node = doc;
        nodeKind = Type.DOCUMENT;
        this.baseURI = baseURI;
        docWrapper = this;
        domLevel3 = config.getDOMLevel() == 3;
        if (config.getExternalObjectModel(doc.getClass()) == null) {
            throw new IllegalArgumentException(
                    "Node class " + doc.getClass().getName() + " is not recognized in this Saxon configuration");
        }
        setConfiguration(config);
    }

    /**
     * Create a wrapper for a node in this document
     *
     * @param node the DOM node to be wrapped. This must be a node within the document wrapped by this
     *             DocumentWrapper
     * @throws IllegalArgumentException if the node is not a descendant of the Document node wrapped by
     *                                  this DocumentWrapper
     */

    public NodeWrapper wrap(Node node) {
        if (node == this.node) {
            return this;
        }
        Document doc = node.getOwnerDocument();
        if (doc == this.node || (domLevel3 && doc != null && doc.isSameNode(this.node))) {
            return makeWrapper(node, this);
        } else {
            throw new IllegalArgumentException(
                "DocumentWrapper#wrap: supplied node does not belong to the wrapped DOM document");
        }
    }

    /**
     * Set the Configuration that contains this document
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
     */

    public NamePool getNamePool() {
        return config.getNamePool();
    }

    /**
     * Get the unique document number
     */

    public long getDocumentNumber() {
        return documentNumber;
    }

    /**
     * Get the element with a given ID, if any
     *
     * @param id the required ID value
     * @param getParent
     * @return a NodeInfo representing the element with the given ID, or null if there
     *         is no such element. This implementation does not necessarily conform to the
     *         rule that if an invalid document contains two elements with the same ID, the one
     *         that comes last should be returned.
     */

    public NodeInfo selectID(String id, boolean getParent) {
        if (node instanceof Document) {
            Node el = ((Document)node).getElementById(id);
            if (el == null) {
                return null;
            }
            return wrap(el);
        } else {
            return null;
        }
    }

    /**
     * Determine whether this is the same node as another node. <br />
     * Note: a.isSameNode(b) if and only if generateId(a)==generateId(b)
     *
     * @return true if this Node object and the supplied Node object represent the
     *         same node in the tree.
     */

    public boolean isSameNodeInfo(NodeInfo other) {
        return other instanceof DocumentWrapper && node == ((DocumentWrapper)other).node;
    }

    /**
     * Get the list of unparsed entities defined in this document
     * @return an Iterator, whose items are of type String, containing the names of all
     *         unparsed entities defined in this document. If there are no unparsed entities or if the
     *         information is not available then an empty iterator is returned
     * @since 9.1 (implemented for this subclass since 9.2)
     */

    public Iterator<String> getUnparsedEntityNames() {
        DocumentType docType = ((Document)node).getDoctype();
        if (docType == null) {
            return Collections.EMPTY_LIST.iterator();
        }
        NamedNodeMap map = docType.getEntities();
        if (map == null) {
            return Collections.EMPTY_LIST.iterator();
        }
        List<String> names = new ArrayList<String>(map.getLength());
        for (int i=0; i<map.getLength(); i++) {
            Entity e = (Entity)map.item(i);
            if (e.getNotationName() != null) {
                // it is an unparsed entity
                names.add(e.getLocalName());
            }
        }
        return names.iterator();
    }

    /**
     * Get the unparsed entity with a given name
     *
     * @param name the name of the entity
     * @return if the entity exists, return an array of two Strings, the first
     *      holding the system ID of the entity (as an absolute URI if possible),
     *      the second holding the public ID if there is one, or null if not.
     *      If the entity does not exist, the method returns null.
     *      Applications should be written on the assumption that this array may
     *      be extended in the future to provide additional information.
     * @since 8.4 (implemented for this subclass since 9.2)
     */

    public String[] getUnparsedEntity(String name) {
        DocumentType docType = ((Document)node).getDoctype();
        if (docType == null) {
            return null;
        }
        NamedNodeMap map = docType.getEntities();
        if (map == null) {
            return null;
        }
        Entity entity = (Entity)map.getNamedItem(name);
        if (entity == null || entity.getNotationName() == null) {
            // In the first case, no entity found. In the second case, it's a parsed entity.
            return null;
        }
        String systemId = entity.getSystemId();
        try {
            URI systemIdURI = new URI(systemId);
            if (!systemIdURI.isAbsolute()) {
                systemIdURI = new URI(getBaseURI()).resolve(systemIdURI);
                systemId = systemIdURI.toString();
            }
        } catch (URISyntaxException err) {
            // invalid URI: no action - return the "URI" as written
        }
        return new String[]{systemId, entity.getPublicId()};
    }

    /**
     * Get the type annotation. Always XS_UNTYPED.
     */

    public int getTypeAnnotation() {
        return StandardNames.XS_UNTYPED;
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
// The Initial Developer of the Original Code is Michael H. Kay
//
// Portions created by (your name) are Copyright (C) (your legal entity). All Rights Reserved.
//
// Contributor(s): none.
//
