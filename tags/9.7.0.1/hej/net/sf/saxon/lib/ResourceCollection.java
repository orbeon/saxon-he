package net.sf.saxon.lib;

import net.sf.saxon.expr.XPathContext;
import net.sf.saxon.trans.XPathException;

import java.util.Iterator;

/**
 * This interface defines a ResourceCollection. This is a counterpart to the JAXP
 * URIResolver, but is used to map the URI of collection into a sequence of Resource objects.
 * It is used to support the fn:collection() and fn:uri-collection() functions.
 * @since 9.7
 */
public interface ResourceCollection {


    /**
     * Get the URI of the collection
     *
     * @return        The URI as passed to the fn:collection() or fn:uri-collection()
     *                function, resolved if it is relative against the static base URI.
     *                If the collection() or uri-collection() function
     *                was called with no arguments (to get the "default collection") this
     *                will be the URI of the default collection registered with the Configuration.
    */

    String getCollectionURI() ;

    /**
     * Get the URIs of the resources in the collection. This supports the fn:uri-collection()
     * function. It is not required that all collections expose a list of URIs in this way, or
     * that the URIs bear any particular relationship to the resources returned by the
     * getResources() method for the same collection URI. The URIs that are returned should be
     * suitable for passing to the registered URIResolver (in the case of XML resources),
     * or the UnparsedTextResolver (in the case of unparsed text and JSON resources), etc.
     * @param context the XPath evaluation context
     * @return an iterator over the URIs of the resources in the collection
     * @throws XPathException in the event of any error (for example, if the collection URI
     * is not recognized)
     */

    Iterator<String> getResourceURIs(XPathContext context) throws XPathException;

    /**
     * Get the resources in the collection. This supports the fn:collection() function. It is not
     * required that all collections expose a set of resources in this way, or that the resources
     * returned bear any particular relationship to the URIs returned by the getResourceURIs() method
     * for the same collection URI.
     * @param context the XPath evaluation context
     * @return an iterator over the resources in the collection
     * @throws XPathException in the event of any error (for example, if the collection URI
     * is not recognized)
     */

    Iterator<? extends Resource> getResources(XPathContext context) throws XPathException;

    /**
     * Ask whether the collection is stable: in this case Saxon will retain the contents of the
     * collection in memory, and will not make a second request on the CollectionFinder for the
     * same collection URI.
     * @param context the XPath dynamic evaluation context (in case the decision is context dependent)
     */

    boolean isStable(XPathContext context);

}
