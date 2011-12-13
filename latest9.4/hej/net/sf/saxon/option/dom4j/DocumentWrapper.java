package net.sf.saxon.option.dom4j;

import net.sf.saxon.Configuration;
import net.sf.saxon.om.DocumentInfo;
import net.sf.saxon.om.NamePool;
import net.sf.saxon.om.NodeInfo;
import net.sf.saxon.om.StandardNames;
import net.sf.saxon.type.SchemaType;
import net.sf.saxon.type.Type;
import net.sf.saxon.type.Untyped;
import org.dom4j.Document;

import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;

/**
 * The root node of an XPath tree. (Or equivalently, the tree itself).<P>
 * This class should have been named Root; it is used not only for the root of a document,
 * but also for the root of a result tree fragment, which is not constrained to contain a
 * single top-level element.
 *
 * @author Michael H. Kay
 */

public class DocumentWrapper extends NodeWrapper implements DocumentInfo {

    protected Configuration config;
    protected String baseURI;
    protected long documentNumber;
    private HashMap<String, Object> userData;

    /**
     * Create a Saxon wrapper for a dom4j document
     *
     * @param doc     The dom4j document
     * @param baseURI The base URI for all the nodes in the document
     * @param config  The Saxon configuration
     */

    public DocumentWrapper(Document doc, String baseURI, Configuration config) {
        super(doc, null, 0);
        node = doc;
        nodeKind = Type.DOCUMENT;

        this.baseURI = baseURI;

        docWrapper = this;
        setConfiguration(config);
    }

    /**
     * Wrap a node in the dom4j document.
     *
     * @param node The node to be wrapped. This must be a node in the same document
     *             (the system does not check for this).
     * @return the wrapping NodeInfo object
     */

    public NodeInfo wrap(Object node) {
        if (node == this.node) {
            return this;
        }
        return makeWrapper(node, this);
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
     * @param id        the required ID value
     * @param getParent
     * @return null: dom4j does not provide any information about attribute types.
     */


   /*@Nullable*/ public NodeInfo selectID(String id, boolean getParent) {
        return null;
    }

    /**
     * Get the list of unparsed entities defined in this document
     *
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
     * @return null: dom4j does not provide access to unparsed entities
     */

    public String[] getUnparsedEntity(String name) {
        return null;
    }

    /**
     * Get the configuration previously set using setConfiguration
     * (or the default configuraton allocated automatically)
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
     * Set the configuration (containing the name pool used for all names in this document). Calling
     * this method allocates a unique number to the document (unique within the Configuration); this
     * will form the basis for testing node identity
     *
     * @param config the configuration
     */

    public void setConfiguration(Configuration config) {
        this.config = config;
        documentNumber = config.getDocumentNumberAllocator().allocateDocumentNumber();
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
     *
     * @param key   A string giving the name of the property to be set. Clients are responsible
     *              for choosing a key that is likely to be unique. Must not be null. Keys used internally
     *              by Saxon are prefixed "saxon:".
     * @param value The value to be set for the property. May be null, which effectively
     *              removes the existing value for the property.
     */

    public void setUserData(String key, Object value) {
        if (userData == null) {
            userData = new HashMap(4);
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
     *
     * @param key A string giving the name of the property to be retrieved.
     * @return the value of the property, or null if the property has not been defined.
     */

    public Object getUserData(String key) {
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