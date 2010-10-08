package net.sf.saxon.option.dom4j;

import net.sf.saxon.Configuration;
import net.sf.saxon.om.DocumentInfo;
import net.sf.saxon.om.NamePool;
import net.sf.saxon.om.NodeInfo;
import net.sf.saxon.om.StandardNames;
import net.sf.saxon.type.Type;
import org.dom4j.Document;

import java.util.Collections;
import java.util.Iterator;

/**
  * The root node of an XPath tree. (Or equivalently, the tree itself).<P>
  * This class should have been named Root; it is used not only for the root of a document,
  * but also for the root of a result tree fragment, which is not constrained to contain a
  * single top-level element.
  * @author Michael H. Kay
  */

public class DocumentWrapper extends NodeWrapper implements DocumentInfo {

    protected Configuration config;
    protected String baseURI;
    protected long documentNumber;

    /**
     * Create a Saxon wrapper for a dom4j document
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
     * @param node The node to be wrapped. This must be a node in the same document
     * (the system does not check for this).
     * @return the wrapping NodeInfo object
     */

    public NodeInfo wrap(Object node) {
        if (node==this.node) {
            return this;
        }
        return makeWrapper(node, this);
    }

    /**
    * Get the unique document number
    */

    public long getDocumentNumber() {
        return documentNumber;
    }

    /**
    * Get the element with a given ID, if any
    * @param id the required ID value
    * @param getParent
     * @return null: dom4j does not provide any information about attribute types.
    */

    public NodeInfo selectID(String id, boolean getParent) {
        return null;
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
// The Initial Developer of the Original Code is
// Michael Kay (michael.h.kay@ntlworld.com).
//
// Portions created by (your name) are Copyright (C) (your legal entity). All Rights Reserved.
//
// Contributor(s): none.
//
