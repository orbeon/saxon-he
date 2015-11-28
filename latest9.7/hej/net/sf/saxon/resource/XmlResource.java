package net.sf.saxon.resource;

import net.sf.saxon.Configuration;
import net.sf.saxon.expr.XPathContext;
import net.sf.saxon.functions.URIQueryParameters;
import net.sf.saxon.lib.ParseOptions;
import net.sf.saxon.lib.Resource;
import net.sf.saxon.lib.ResourceFactory;
import net.sf.saxon.om.Item;
import net.sf.saxon.om.NodeInfo;
import net.sf.saxon.trans.XPathException;

import javax.xml.transform.Source;
import javax.xml.transform.stream.StreamSource;
import java.io.IOException;

/**
 * The class is an implementation of the generic Resource object (typically an item in a collection)
 * representing an XML document
 */

public class XmlResource implements Resource {
    private Source source;
    private ParseOptions options;
    private NodeInfo doc;
    private Configuration config;
    private String contentType = null;
    private int onError = URIQueryParameters.ON_ERROR_FAIL;

    public final static ResourceFactory FACTORY = new ResourceFactory() {
        public Resource makeResource(Configuration config, String resourceURI, String contentType, AbstractResourceCollection.InputDetails details) throws XPathException {
            Source source = new StreamSource(details.inputStream, resourceURI);
            ParseOptions options = details.parseOptions;
            if (options == null) {
                options = config.getParseOptions();
            }
            return new XmlResource(config, source, options, details.onError);
        }
    };

    public XmlResource(Configuration c, NodeInfo doc) {
        this.config = c;
        this.doc = doc;
    }

    public XmlResource(Configuration c, Source source, ParseOptions options, int onError) {
        this.config = c;
        this.source = source;
        this.options = options;
        this.onError = onError;
    }

    public String getResourceURI() {
        if (doc == null) {
            return source.getSystemId();
        } else {
            return doc.getSystemId();
        }
    }

    /**
     * Get an item representing the resource: in this case a document node for the XML document.
     * @param context the XPath evaluation context
     * @return the document; or null if there is an error and the error is to be ignored
     * @throws XPathException if (for example) XML parsing fails
     */

    public Item getItem(XPathContext context) throws XPathException {
        if (doc == null) {
            try {
                doc = config.buildDocumentTree(source, options).getRootNode();
            } catch (XPathException e) {
                if (onError == URIQueryParameters.ON_ERROR_FAIL) {
                    XPathException e2 = new XPathException("collection(): failed to parse XML file " + source.getSystemId() + ": " + e.getMessage(),
                                                           e.getErrorCodeLocalPart());
                    throw e2;
                } else if (onError == URIQueryParameters.ON_ERROR_WARNING) {
                    context.getController().warning("collection(): failed to parse XML file " + source.getSystemId() + ": " + e.getMessage(), e.getErrorCodeLocalPart());
                } else {
                    return null;
                }
            }
            if (source instanceof StreamSource && ((StreamSource)source).getInputStream() != null) {
                try {
                    ((StreamSource) source).getInputStream().close();
                } catch (IOException e) {
                    // ignore the failure
                }
            }
        }
        return doc;
    }

    public String getContentType() {
        return null;
    }


}
