package net.sf.saxon.tree.wrapper;

import net.sf.saxon.Configuration;
import net.sf.saxon.om.*;
import net.sf.saxon.pattern.NodeKindTest;
import net.sf.saxon.tree.iter.AxisIterator;

import java.util.HashMap;
import java.util.Iterator;

/**
  * A SpaceStrippedDocument represents a view of a real Document in which selected
  * whitespace text nodes are treated as having been stripped.
  */

public class SpaceStrippedDocument extends SpaceStrippedNode implements DocumentInfo {

    private SpaceStrippingRule strippingRule;
    private boolean preservesSpace;
    private HashMap<String, Object> userData;

    /**
     * Create a space-stripped view of a document
     * @param doc the underlying document
     * @param strippingRule an object that contains the rules defining which whitespace
     * text nodes are to be absent from the view
     */

    public SpaceStrippedDocument(/*@NotNull*/ DocumentInfo doc, SpaceStrippingRule strippingRule) {
        node = doc;
        parent = null;
        docWrapper = this;
        this.strippingRule = strippingRule;
        preservesSpace = findPreserveSpace(doc);
    }

    /**
     * Create a wrapped node within this document
     */

    public SpaceStrippedNode wrap(NodeInfo node) {
        return makeWrapper(node, this, null);
    }

    /**
     * Ask whether the document contains any nodes whose type annotation is anything other than
     * UNTYPED
     *
     * @return true if the document contains elements whose type is other than UNTYPED
     */
    public boolean isTyped() {
        return ((DocumentInfo)node).isTyped();
    }

    /**
     * Get the document's strippingRule
     */

    public SpaceStrippingRule getStrippingRule() {
        return strippingRule;
    }

    /**
     * Get the configuration previously set using setConfiguration
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

    /*@Nullable*/ public NodeInfo selectID(String id, boolean getParent) {
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
     * Determine whether the wrapped document contains any xml:space="preserve" attributes. If it
     * does, we will look for them when stripping individual nodes. It's more efficient to scan
     * the document in advance checking for xml:space attributes than to look for them every time
     * we hit a whitespace text node.
     * @param doc the wrapper of the document node
     * @return true if any element in the document has an xml:space attribute with the value "preserve"
     */

    private static boolean findPreserveSpace(/*@NotNull*/ DocumentInfo doc) {
        AxisIterator iter = doc.iterateAxis(Axis.DESCENDANT, NodeKindTest.ELEMENT);
        while (true) {
            NodeInfo node = iter.next();
            if (node == null) {
                return false;
            }
            String val = node.getAttributeValue(StandardNames.XML_SPACE);
            if ("preserve".equals(val)) {
                return true;
            }
        }
    }

    /**
     * Does the stripped document contain any xml:space="preserve" attributes?
     * @return true if any element in the document has an xml:space attribute with the value "preserve"
     */

    public boolean containsPreserveSpace() {
        return preservesSpace;
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

    public void setUserData(String key, /*@Nullable*/ Object value) {
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