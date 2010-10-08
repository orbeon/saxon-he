package net.sf.saxon.om;
import net.sf.saxon.Configuration;

import java.util.Iterator;


/**
  * A TypeStrippedDocument represents a view of a real Document in which all nodes are
  * untyped
  */

public class TypeStrippedDocument extends TypeStrippedNode implements DocumentInfo {

    /**
     * Create a type-stripped view of a document
     * @param doc the underlying document
     */

    public TypeStrippedDocument(DocumentInfo doc) {
        node = doc;
        parent = null;
        docWrapper = this;
    }

    /**
     * Create a wrapped node within this document
     */

    public TypeStrippedNode wrap(NodeInfo node) {
        return makeWrapper(node, this, null);
    }

    /**
     * Get the configuration
     */

    public Configuration getConfiguration() {
        return node.getConfiguration();
    }

	/**
	* Get the name pool used for the names in this document
	*/

	public NamePool getNamePool() {
	    return node.getNamePool();
	}

	/**
	* Get the unique document number
	*/

	public long getDocumentNumber() {
	    return node.getDocumentNumber();
	}

    /**
    * Get the element with a given ID, if any
    * @param id the required ID value
    * @param getParent
     * @return the element with the given ID value, or null if there is none.
    */

    public NodeInfo selectID(String id, boolean getParent) {
        NodeInfo n = ((DocumentInfo)node).selectID(id, false);
        if (n==null) {
            return null;
        } else {
            return makeWrapper(n, this, null);
        }
    }

    /**
     * Get the list of unparsed entities defined in this document
     * @return an Iterator, whose items are of type String, containing the names of all
     *         unparsed entities defined in this document. If there are no unparsed entities or if the
     *         information is not available then an empty iterator is returned
     */

    public Iterator<String> getUnparsedEntityNames() {
        return ((DocumentInfo)node).getUnparsedEntityNames();
    }

    /**
    * Get the unparsed entity with a given name
    * @param name the name of the entity
    */

    public String[] getUnparsedEntity(String name) {
        return ((DocumentInfo)node).getUnparsedEntity(name);
    }

    /**
     * Get the type annotation of this node. This implementation always returns XS_UNTYPED.
     * @return XS_UNTYPED
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
// Michael H. Kay.
//
// Portions created by (your name) are Copyright (C) (your legal entity). All Rights Reserved.
//
// Contributor(s): none.
//
