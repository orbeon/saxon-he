package net.sf.saxon.resource;

import net.sf.saxon.Configuration;
import net.sf.saxon.expr.XPathContext;
import net.sf.saxon.lib.Resource;
import net.sf.saxon.lib.ResourceFactory;
import net.sf.saxon.om.Item;
import net.sf.saxon.trans.XPathException;
import net.sf.saxon.value.StringValue;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;

/**
 * This class implements th interface Resource. We handle unparded text here.
 * The Resource objects belong to a collection
 * It is used to support the fn:collection() and fn:uri-collection() functions.
 * @since 9.7
 */
public class UnparsedTextResource implements Resource {
    private Configuration config;
    private String href = null;
    private String encoding = null;
    private String unparsedText = null;
    private InputStream inputStream;
    private String contentType = null;

    public UnparsedTextResource(Configuration config, String href, InputStream in){
        this.config = config;
        this.href = href;
        inputStream = in;
    }

    public final static ResourceFactory FACTORY = new ResourceFactory() {
        public Resource makeResource(Configuration config, String resourceURI, String contentType, AbstractResourceCollection.InputDetails details) throws XPathException {
            return new UnparsedTextResource(config, resourceURI, details.inputStream);
        }
    };

    public String getResourceURI() {
        return href;
    }

    public Item getItem(XPathContext context) throws XPathException {
        if(unparsedText == null) {
            StringBuilder builder = null;
            try {
                builder = CatalogCollection.makeStringBuilderFromStream(inputStream);
            } catch (FileNotFoundException e) {
                throw new XPathException(e);
            } catch (IOException e) {
                throw new XPathException(e);
            }
            unparsedText = builder.toString();
        }
        return new StringValue(unparsedText);
    }

    public String getContentType() {
        return contentType;
    }


}
