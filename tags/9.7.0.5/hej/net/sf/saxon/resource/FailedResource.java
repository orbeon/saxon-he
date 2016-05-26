package net.sf.saxon.resource;

import net.sf.saxon.expr.XPathContext;
import net.sf.saxon.lib.Resource;
import net.sf.saxon.om.Item;
import net.sf.saxon.trans.XPathException;

/**
 * FailedResource represents an item in a collection that could not be processed because of some kind of error
 */
public class FailedResource implements Resource {

    private String uri;
    private XPathException error;

    public FailedResource(String uri, XPathException error) {
        this.uri = uri;
        this.error = error;
    }

    /**
     * Get the media type (MIME type) of the resource if known
     *
     * @return the media type if known; otherwise null
     */
    public String getContentType() {
        return null;
    }

    /**
     * Get a URI that identifies this resource
     *
     * @return a URI identifying this resource
     */
    public String getResourceURI() {
        return uri;
    }

    /**
     * Get an XDM Item holding the contents of this resource.
     *
     * @param context the XPath evaluation context
     * @return an item holding the contents of the resource. The type of item will
     * reflect the type of the resource: a document node for XML resources, a string
     * for text resources, a map or array for JSON resources, a base64Binary value
     * for binary resource
     * @throws XPathException if a failure occurs materializing the resource
     */
    public Item getItem(XPathContext context) throws XPathException {
        throw error;
    }

    /**
     * Get the underlying error
     */

    public XPathException getError() {
        return error;
    }
}

