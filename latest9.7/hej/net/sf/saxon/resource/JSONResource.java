package net.sf.saxon.resource;

import net.sf.saxon.Configuration;
import net.sf.saxon.expr.XPathContext;
import net.sf.saxon.lib.Resource;
import net.sf.saxon.lib.ResourceFactory;
import net.sf.saxon.ma.json.ParseJsonFn;
import net.sf.saxon.ma.map.HashTrieMap;
import net.sf.saxon.ma.map.MapItem;
import net.sf.saxon.om.Item;
import net.sf.saxon.trans.XPathException;

import java.io.IOException;
import java.io.InputStream;


public class JSONResource implements Resource {
    private String href;
    private String jsonStr;
    private String encoding;
    private InputStream inputStream;
    private String contentType = "application/json";

    public final static ResourceFactory FACTORY = new ResourceFactory() {
        public Resource makeResource(Configuration config, String resourceURI, String contentType, AbstractResourceCollection.InputDetails details) throws XPathException {
            return new JSONResource(resourceURI, details);
        }
    };

    public JSONResource(String href, InputStream in) {
        this.href = href;
        this.inputStream = in;
        this.encoding = "UTF-8";
    }

    public JSONResource(String href, AbstractResourceCollection.InputDetails details) {
        this.href = href;
        this.inputStream = details.inputStream;
        this.encoding = details.encoding;
        if (encoding == null) {
            encoding = "UTF-8";
        }
    }


    public String getResourceURI() {
        return href;
    }

    public Item getItem(XPathContext context) throws XPathException {
        if (jsonStr == null) {
            try {
                StringBuilder sb = CatalogCollection.makeStringBuilderFromStream(inputStream, encoding);
                jsonStr = sb.toString();
            } catch (IOException e) {
                throw new XPathException(e);
            }
        }
        MapItem options = new HashTrieMap(context);
        Item item = ParseJsonFn.parse(jsonStr, options, context);
        return item;
    }


    public String getContentType() {
        return contentType;
    }


}
