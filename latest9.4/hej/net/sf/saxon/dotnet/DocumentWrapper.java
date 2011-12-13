package net.sf.saxon.dotnet;

import cli.System.Xml.XmlDocument;
import cli.System.Xml.XmlNode;
import cli.System.Xml.XmlNodeType;
import net.sf.saxon.Configuration;
import net.sf.saxon.om.DocumentInfo;
import net.sf.saxon.om.NamePool;
import net.sf.saxon.om.NodeInfo;
import net.sf.saxon.om.StandardNames;
import net.sf.saxon.type.SchemaType;
import net.sf.saxon.type.Type;
import net.sf.saxon.type.Untyped;

import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

/**
 * The document node of a tree implemented as a wrapper around a DOM Document.
 */

public class DocumentWrapper extends NodeWrapper implements DocumentInfo {

    protected Configuration config;
    protected String baseURI;
    protected long documentNumber;
    protected boolean level3 = false;
    private HashMap<String, Object> userData;

    /**
     * Wrap a DOM Document or DocumentFragment node
     * @param doc a DOM Document or DocumentFragment node
     * @param baseURI the base URI of the document
     * @param config the Saxon configuration
     */

    public DocumentWrapper(XmlNode doc, String baseURI, Configuration config) {
        //System.err.println("Creating DocumentWrapper for " +node);
        this.node = doc;
        this.parent = null;
        this.index = 0;
        //super(doc, null, 0);
        if (doc.get_NodeType().Value != XmlNodeType.Document) {
            throw new IllegalArgumentException("Node must be a DOM Document");
        }
        node = doc;
        nodeKind = Type.DOCUMENT;
        this.baseURI = baseURI;
        docWrapper = this;

//        // Find out if this is a level-3 DOM implementation
//        Method[] methods = doc.getClass().getMethods();
//        for (int i=0; i<methods.length; i++) {
//            if (methods[i].getName().equals("isSameNode")) {
//                level3 = true;
//                break;
//            }
//        }
        //System.err.println("Setting configuration");
        setConfiguration(config);
    }

    /**
     * Create a wrapper for a node in this document
     *
     * @param node the DOM node to be wrapped. This must be a node within the document wrapped by this
     *             DocumentWrapper
     * @throws IllegalArgumentException if the node is not a descendant of the Document node wrapped by
     *                                  this DocumentWrapper
     * @return a NodeInfo that wraps the supplied node
     */

    public NodeWrapper wrap(XmlNode node) {
        if (node == this.node) {
            return this;
        }
        if (node.get_OwnerDocument() == this.node) {
            return makeWrapper(node, this);
        } else {
            throw new IllegalArgumentException(
                "DocumentWrapper#wrap: supplied node does not belong to the wrapped DOM document");
        }
    }

    /**
     * Set the Configuration that contains this document
     * @param config the Saxon configuration
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
     * Ask whether the document contains any nodes whose type annotation is anything other than
     * UNTYPED
     *
     * @return true if the document contains elements whose type is other than UNTYPED
     */
    public boolean isTyped() {
        return false;
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
     * @param getParent true if running the element-with-id() function rather than the id()
     * function; the difference is that in the case of an element of type xs:ID, the parent of
     * the element should be returned, not the element itself.
     * @return a NodeInfo representing the element with the given ID, or null if there
     *         is no such element. This implementation does not necessarily conform to the
     *         rule that if an invalid document contains two elements with the same ID, the one
     *         that comes last should be returned.
     */

    public NodeInfo selectID(String id, boolean getParent) {
        if (node instanceof XmlDocument) {
            XmlNode el = ((XmlDocument)node).GetElementById(id);
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
        return other instanceof DocumentWrapper && node == ((DocumentWrapper) other).node;
    }

    /**
     * Get the Base URI for the node, that is, the URI used for resolving a relative URI contained
     * in the node. In the DOM model, base URIs are held only an the document level.
    */

    public String getBaseURI() {
        return node.get_BaseURI();
    }

    /**
     * Get the list of unparsed entities defined in this document
     * @return an Iterator, whose items are of type String, containing the names of all
     *         unparsed entities defined in this document. If there are no unparsed entities or if the
     *         information is not available then an empty iterator is returned
     */

    public Iterator<String> getUnparsedEntityNames() {
        List<String> ls = Collections.emptyList();
        return ls.iterator();
    }

    /**
     * Get the unparsed entity with a given name
     *
     * @param name the name of the entity
     * @return null: JDOM does not provide access to unparsed entities
     */

    public String[] getUnparsedEntity(String name) {
        return null;
    }

    /**
     * Get the type annotation. Always XS_UNTYPED.
     */

    public int getTypeAnnotation() {
        return StandardNames.XS_UNTYPED;
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
        return Untyped.getInstance();
    }

    /**
     * Set user data on the document node. The user data can be retrieved subsequently
     * using {@link #getUserData}
     * @param key   A string giving the name of the property to be set. Clients are responsible
     *              for choosing a key that is likely to be unique. Must not be null. Keys used internally
     *              by Saxon are prefixed "saxon:".
     * @param value The value to be set for the property. May be null, which effectively
     *              removes the existing value for the property.
     */

    public void setUserData(String key, Object value) {
        if (userData == null) {
            userData = new HashMap<String, Object>(4);
        }
        if (value == null) {
            userData.remove(key);
        } else {
            userData.put(key, value);
        }
    }

    /**
     * Get user data held in the document node. This retrieves properties previously set using
     * {@link #setUserData}
     * @param key A string giving the name of the property to be retrieved.
     * @return the value of the property, or null if the property has not been defined.
     */

    /*@Nullable*/ public Object getUserData(String key) {
        if (userData == null) {
            return null;
        } else {
            return userData.get(key);
        }
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
