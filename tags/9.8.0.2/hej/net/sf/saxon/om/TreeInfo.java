////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
// Copyright (c) 2017 Saxonica Limited.
// This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
// If a copy of the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
// This Source Code Form is "Incompatible With Secondary Licenses", as defined by the Mozilla Public License, v. 2.0.
////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

package net.sf.saxon.om;

import net.sf.saxon.Configuration;

import javax.xml.transform.Source;
import java.util.Iterator;

/**
 * This interface represents information about a tree as a whole. The tree may or may not be rooted
 * at a document node. In some tree models, the interface is implemented by the object representing the
 * root node of the tree (typically but not necessarily the document node). In other tree models,
 * it is a free-standing object. The TreeInfo holds information that is common to all the nodes in
 * a tree, such as the document number and a reference to the Configuration.
 *
 * <p>Java object identity for TreeInfo objects equates to XPath node identity for the root
 * nodes of the relevant trees: that is, two root nodes are "the same node" if and only if
 * their TreeInfo objects are the same Java object. However, when sorting into document order,
 * the order of trees is based on their "document number", a unique number allocated by the
 * document number allocator for the Configuration.</p>
 *
 * @author Michael H. Kay
 * @since 9.7. Replaces the DocumentInfo interface which represented both a document as a whole, and
 * the document node at the root of a document, but which did not apply to trees rooted at a node other
 * than a document node.
 */

public interface TreeInfo extends Source {

    /**
     * Get the NodeInfo object representing the document node at the root of the tree
     * @return the document node
     */

    NodeInfo getRootNode();

    /**
     * Get the Configuration to which this document belongs
     * @return the configuration
     */

    Configuration getConfiguration();

    /**
     * Get the document number, which identifies this tree uniquely within a Configuration
     * @return the document number
     */

    long getDocumentNumber();

    /**
     * Ask whether the document contains any nodes whose type annotation is anything other than
     * UNTYPED or UNTYPED_ATOMIC
     *
     * @return true if the document contains elements whose type is other than UNTYPED or UNTYPED_ATOMIC
     */

    boolean isTyped();

    /**
     * Get the element with a given ID, if any
     *
     * @param id        the required ID value
     * @param getParent true if running the element-with-id() function rather than the id()
     *                  function; the difference is that in the case of an element of type xs:ID, the parent of
     *                  the element should be returned, not the element itself.
     * @return the element with the given ID, or null if there is no such ID
     *         present (or if the parser has not notified attributes as being of
     *         type ID)
     * @since 8.4. Second argument added in 9.2.
     */

    NodeInfo selectID(String id, boolean getParent);

    /**
     * Get the list of unparsed entities defined in this document
     *
     * @return an Iterator, whose items are of type String, containing the names of all
     *         unparsed entities defined in this document. If there are no unparsed entities or if the
     *         information is not available then an empty iterator is returned
     * @since 9.1
     */

    Iterator<String> getUnparsedEntityNames();

    /**
     * Get the unparsed entity with a given name
     *
     * @param name the name of the entity
     * @return if the entity exists, return an array of two Strings, the first
     *         holding the system ID of the entity (as an absolute URI if possible),
     *         the second holding the public ID if there is one, or null if not.
     *         If the entity does not exist, the method returns null.
     *         Applications should be written on the assumption that this array may
     *         be extended in the future to provide additional information.
     * @since 8.4
     */

    String[] getUnparsedEntity(String name);

    /**
     * Set user data on the document node. The user data can be retrieved subsequently
     * using {@link #getUserData}
     *
     * @param key   A string giving the name of the property to be set. Clients are responsible
     *              for choosing a key that is likely to be unique. Must not be null. Keys used internally
     *              by Saxon are prefixed "saxon:".
     *              <p>The key <code>saxon:document-uri</code> may be used to set the document-uri
     *              property of the document node. If this is set to a non-empty string, the result
     *              of the document-uri() function on the document node will be that string, as an
     *              instance of xs:anyURI. If it is set to a zero-length string, the result of
     *              document-uri() will be the empty sequence, ().</p>
     * @param value The value to be set for the property. May be null, which effectively
     *              removes the existing value for the property.
     */

    void setUserData(String key, Object value);

    /**
     * Get user data held in the document node. This retrieves properties previously set using
     * {@link #setUserData}
     *
     * @param key A string giving the name of the property to be retrieved.
     * @return the value of the property, or null if the property has not been defined.
     */

    Object getUserData(String key);

}

