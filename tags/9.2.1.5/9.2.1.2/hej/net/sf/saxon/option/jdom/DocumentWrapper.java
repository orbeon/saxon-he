package net.sf.saxon.option.jdom;

import net.sf.saxon.Configuration;
import net.sf.saxon.om.*;
import net.sf.saxon.pattern.NodeKindTest;
import net.sf.saxon.type.Type;
import org.jdom.Attribute;
import org.jdom.Document;
import org.jdom.Element;

import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

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
    private HashMap<String, Element> idIndex = null;

    /**
     * Create a Saxon wrapper for a JDOM document
     * @param doc     The JDOM document
     * @param baseURI The base URI for all the nodes in the document
     * @param config  The Saxon Configuration
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
     * Wrap a node in the JDOM document.
     * @param node The node to be wrapped. This must be a node in the same document
     * (the system does not check for this).
     * @return the wrapping NodeInfo object
     */

    public NodeWrapper wrap(Object node) {
        if (node==this.node) {
            return this;
        }
        return makeWrapper(node, this);
    }



	/**
	* Set the configuration (containing the name pool used for all names in this document)
	*/

	public void setConfiguration(Configuration config) {
        this.config = config;
		documentNumber = config.getDocumentNumberAllocator().allocateDocumentNumber();
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
	* Get the unique document number
	*/

	public long getDocumentNumber() {
	    return documentNumber;
	}

    /**
    * Get the element with a given ID, if any
    * @param id the required ID value
    * @param getParent
     * @return the element node with the given ID if there is one, otherwise null.
    */

    public NodeInfo selectID(String id, boolean getParent) {
        if (idIndex == null) {
            idIndex = new HashMap<String, Element>(100);
            AxisIterator iter = iterateAxis(Axis.DESCENDANT, NodeKindTest.ELEMENT);
            while (true) {
                NodeInfo node = (NodeInfo)iter.next();
                if (node == null) {
                    break;
                }
                Element element = (Element)((NodeWrapper)node).node;
                List attributes = element.getAttributes();
                for (int a=0; a<attributes.size(); a++) {
                    Attribute att = (Attribute)attributes.get(a);
                    if (att.getAttributeType() == Attribute.ID_TYPE) {
                        idIndex.put(att.getValue(), element);
                    }
                }
            }
        }
        Element element = idIndex.get(id);
        return (element == null ? null : wrap(element));
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

}

// Original Code is Copyright (c) Saxonica Limited 2009. All rights reserved.

//
// The contents of this file are subject to the Mozilla Public License Version 1.1 (the "License");
// you may not use this file except in compliance with the License. You may obtain a copy of the
// License at http://www.mozilla.org/MPL/
//
// Software distributed under the License is distributed on an "AS IS" basis,
// WITHOUT WARRANTY OF ANY KIND, either express or implied.
// See the License for the specific language governing rights and limitations under the License.
//
// The Original Code is: all this file.
//
// The Initial Developer of the Original Code is Saxonica Limited
//
// Portions created by (your name) are Copyright (C) (your legal entity). All Rights Reserved.
//
// Contributor(s): none
//