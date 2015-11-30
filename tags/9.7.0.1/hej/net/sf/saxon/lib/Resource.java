package net.sf.saxon.lib;


import net.sf.saxon.expr.XPathContext;
import net.sf.saxon.om.Item;
import net.sf.saxon.trans.XPathException;

/**
 * This interface defines a Resource. The Resource objects belong to a collection
 * It is used to support the fn:collection() and fn:uri-collection() functions.
 * @since 9.7
 */
public interface Resource {

    /**
     * Get a URI that identifies this resource
     * @return a URI identifying this resource
     */

    String getResourceURI();

    /**
     * Get an XDM Item holding the contents of this resource.
     * @param context the XPath evaluation context
     * @return an item holding the contents of the resource. The type of item will
     * reflect the type of the resource: a document node for XML resources, a string
     * for text resources, a map or array for JSON resources, a base64Binary value
     * for binary resource. May also return null if the resource cannot be materialized
     * and this is not to be treated as an error.
     * @throws XPathException if a failure occurs materializing the resource
     */

    Item getItem(XPathContext context) throws XPathException;

    /**
     * Get the media type (MIME type) of the resource if known
     * @return the media type if known; otherwise null
     */

    String getContentType();


}
