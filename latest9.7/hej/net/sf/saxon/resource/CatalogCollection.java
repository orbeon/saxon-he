package net.sf.saxon.resource;

import net.sf.saxon.Configuration;
import net.sf.saxon.expr.XPathContext;
import net.sf.saxon.functions.DocumentFn;
import net.sf.saxon.functions.URIQueryParameters;
import net.sf.saxon.lib.ParseOptions;
import net.sf.saxon.lib.Resource;
import net.sf.saxon.lib.Validation;
import net.sf.saxon.om.AxisInfo;
import net.sf.saxon.om.NodeInfo;
import net.sf.saxon.om.TreeInfo;
import net.sf.saxon.pattern.NodeKindTest;
import net.sf.saxon.trans.XPathException;
import net.sf.saxon.tree.iter.AxisIterator;
import net.sf.saxon.tree.iter.MappingJavaIterator;
import net.sf.saxon.tree.util.Navigator;

import javax.xml.transform.Source;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;


public class CatalogCollection extends AbstractResourceCollection {

    private boolean stable;


    //TODO we might know the catalog File already
    public CatalogCollection(Configuration config, String collectionURI) {
        super(config);
        this.collectionURI = collectionURI;
    }


    //TODO - Map never used - therefore should be removed
    public static HashMap<String, String> mimeTypeMap = new HashMap<String, String>() {{
        put("application/atom+xml", "atom");
        put("text/html", "html");
        put("application/json", "json");
        put("application/svg+xml", "svg");
        put("application/xhtml+xml", "xhtml");
        put("application/xml", "xml");
        put("text/html", "html");
        put("text/plain", "txt");
        put("text/xml", "xml");
        put("application/octet-stream", "binary");
        put("application/mac-binary", "binary");
        put("application/binary", "binary");
        put("image/jpg", "binary");
        put("image/png", "binary");
        put("image/gif", "binary");
        put("application/java","binary");
        put("application/java-byte-code","binary");
        put("application/x-java-class","binary");
    }};


    public static void putMimeTypeMap(String contentType, String extension){
        mimeTypeMap.put(contentType, extension);
    }


    public Iterator<String> getResourceURIs(XPathContext context) throws XPathException {
        if (collectionURI == null) {
            XPathException err = new XPathException("No default collection has been defined");
            err.setErrorCode("FODC0002");
            err.setXPathContext(context);
            throw err;
        }
        return catalogContents(collectionURI, context);
    }


    public Iterator<Resource> getResources(final XPathContext context) throws XPathException {

        if (collectionURI == null) {
            XPathException err = new XPathException("No default collection has been defined");
            err.setErrorCode("FODC0002");
            err.setXPathContext(context);
            throw err;
        }

        Iterator<String> resourceURIs = getResourceURIs(context);

        return new MappingJavaIterator<String, Resource>(resourceURIs,
             new MappingJavaIterator.Mapper<String, Resource>() {
                 public Resource map(String in) {
                     try {
                         InputDetails id = getInputDetails(in);
                         return makeResource(context.getConfiguration(), id, in);
                     } catch (XPathException e) {
                         int onError = params.getOnError();
                         if (onError == URIQueryParameters.ON_ERROR_FAIL) {
                             return new FailedResource(in, e);
                         } else if (onError == URIQueryParameters.ON_ERROR_WARNING) {
                             context.getController().warning("collection(): failed to parse " + in + ": " + e.getMessage(), e.getErrorCodeLocalPart());
                             return null;
                         } else {
                             return null;
                         }
                     }
                 }
             });

    }


    @Override
    public boolean isStable(XPathContext context) {
        return stable;
    }

    public static StringBuilder makeStringBuilderFromStream(InputStream in) throws IOException {
        InputStreamReader is = new InputStreamReader(in);
        StringBuilder sb = new StringBuilder();
        BufferedReader br = new BufferedReader(is);
        String read = br.readLine();

        while (read != null) {
            sb.append(read);
            read = br.readLine();
        }
        br.close();
        return sb;
    }



    /**
     * Return a collection defined as a list of URIs in a catalog file
     *
     * @param href    the absolute URI of the catalog file
     * @param context the dynamic evaluation context
     * @return an iterator over the documents in the collection
     * @throws XPathException if any failures occur
     */

    protected Iterator<String> catalogContents(String href, final XPathContext context)
            throws XPathException {

        Source source = DocumentFn.resolveURI(href, null, null, context);
        ParseOptions options = new ParseOptions();
        options.setSchemaValidationMode(Validation.SKIP);
        TreeInfo catalog = context.getConfiguration().buildDocumentTree(source, options);
        if (catalog == null) {
            // we failed to read the catalogue
            XPathException err = new XPathException("Failed to load collection catalog " + href);
            err.setErrorCode("FODC0004");
            err.setXPathContext(context);
            throw err;
        }

        // Now return an iterator over the documents that it refers to

        AxisIterator iter =
                catalog.getRootNode().iterateAxis(AxisInfo.CHILD, NodeKindTest.ELEMENT);
        NodeInfo top = iter.next();
        if (top == null || !("collection".equals(top.getLocalPart()) && top.getURI().isEmpty())) {
            String message;
            if (top == null) {
                message = "No outermost element found in collection catalog";
            } else {
                message = "Outermost element of collection catalog should be Q{}catalog " +
                    "(found Q{" + top.getURI() + "}" + top.getLocalPart() + ")";
            }
            XPathException err = new XPathException(message);
            err.setErrorCode("FODC0004");
            err.setXPathContext(context);
            throw err;
        }
        iter.close();

        String stableAtt = top.getAttributeValue("", "stable");
        if (stableAtt != null) {
            if ("true".equals(stableAtt)) {
                stable = true;
            } else if ("false".equals(stableAtt)) {
                stable = false;
            } else {
                XPathException err = new XPathException(
                        "The 'stable' attribute of element <collection> must be true or false");
                err.setErrorCode("FODC0004");
                err.setXPathContext(context);
                throw err;
            }
        }

        AxisIterator documents = top.iterateAxis(AxisInfo.CHILD, NodeKindTest.ELEMENT);
        List<String> result = new ArrayList<String>();
        NodeInfo item;
        while ((item = documents.next()) != null) {

            if (!("doc".equals(item.getLocalPart()) &&
                    item.getURI().isEmpty())) {
                XPathException err = new XPathException("children of <collection> element must be <doc> elements");
                err.setErrorCode("FODC0004");
                err.setXPathContext(context);
                throw err;
            }
            String hrefAtt = Navigator.getAttributeValue(item, "", "href");
            if (hrefAtt == null) {
                XPathException err = new XPathException("\"<doc> element in catalog has no @href attribute\"");
                err.setErrorCode("FODC0004");
                err.setXPathContext(context);
                throw err;
            }
            String uri;
            try {
                uri = new URI(item.getBaseURI()).resolve(hrefAtt).toString();
            } catch (URISyntaxException e) {
                XPathException err = new XPathException("Invalid base URI or href URI in collection catalog: ("
                        + item.getBaseURI() + ", " + hrefAtt + ")");
                err.setErrorCode("FODC0004");
                err.setXPathContext(context);
                throw err;
            }
            result.add(uri);

        }

        return result.iterator();
    }

    // TODO: provide control over error recovery (etc) through options in the catalog file.


}
