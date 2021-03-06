package net.sf.saxon.om;

import java.util.Iterator;

/**
 * This interface represents a document node as defined in the XPath 2.0 data model.
 * It extends NodeInfo, which is used to represent any node. Every document node must
 * be an instance of DocumentInfo.
 * <p>
 * The interface supports two methods in addition to those for NodeInfo: one to find
 * elements given their ID value, and one to locate unparsed entities. In addition,
 * document nodes have an important property that is not true of nodes in general:
 * two distinct Java DocumentInfo objects never represent the same document node.
 * So the Java "==" operator returns the same result as the {@link NodeInfo#isSameNodeInfo}
 * method.
 * <p>
 * This interface is part of the Saxon public API, and as such (from Saxon8.4 onwards)
 * those methods that form part of the stable public API are labelled with a JavaDoc "since" tag
 * to indicate when they were added to the product.
 *
 * @author Michael H. Kay
 * @since 8.4
 */

public interface DocumentInfo extends NodeInfo {

    /**
     * Get the element with a given ID, if any
     *
     * @param id the required ID value
     * @param getParent true if running the element-with-id() function rather than the id()
     * function; the difference is that in the case of an element of type xs:ID, the parent of
     * the element should be returned, not the element itself.
     * @return the element with the given ID, or null if there is no such ID
     *     present (or if the parser has not notified attributes as being of
     *     type ID)
     * @since 8.4. Second argument added in 9.2.
     */

    public NodeInfo selectID(String id, boolean getParent);

    /**
     * Get the list of unparsed entities defined in this document
     * @return an Iterator, whose items are of type String, containing the names of all
     * unparsed entities defined in this document. If there are no unparsed entities or if the
     * information is not available then an empty iterator is returned
     * @since 9.1
     */

    public Iterator<String> getUnparsedEntityNames();

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
     * @since 8.4
     */

    public String[] getUnparsedEntity(String name);

    /**
     * Set user data on the document node. The user data can be retrieved subsequently
     * using {@link #getUserData}
     * @param key A string giving the name of the property to be set. Clients are responsible
     * for choosing a key that is likely to be unique. Must not be null. Keys used internally
     * by Saxon are prefixed "saxon:".
     * @param value The value to be set for the property. May be null, which effectively
     * removes the existing value for the property.
     */

    public void setUserData(String key, Object value);

    /**
     * Get user data held in the document node. This retrieves properties previously set using
     * {@link #setUserData}
     * @param key A string giving the name of the property to be retrieved.
     * @return the value of the property, or null if the property has not been defined.
     */

    public Object getUserData(String key);

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
// Portions created by (your name) are Copyright (C) (your legal entity). All Rights Reserved.
//
// Contributor(s): none.
//
