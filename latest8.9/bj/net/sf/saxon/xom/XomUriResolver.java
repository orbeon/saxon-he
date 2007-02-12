package net.sf.saxon.xom;
import net.sf.saxon.*;
import net.sf.saxon.java.JavaPlatform;
import net.sf.saxon.functions.ResolveURI;
import net.sf.saxon.trans.DynamicError;
import net.sf.saxon.trans.XPathException;
import nu.xom.Builder;
import nu.xom.Document;
import nu.xom.ParsingException;

import javax.xml.transform.Source;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;


/**
 * This class is a URI resolver that loads an XML document found at the relevant
 * URI and builds a XOM tree from it; the XOM tree is then returned as a source object.
 * The main purpose of this class is for testing: any application that works with
 * a Saxon tinytree can be tested against XOM merely by selecting this URIResolver.
* @author Michael H. Kay
*/

public class XomUriResolver extends StandardURIResolver {

    public XomUriResolver(Configuration config) {
        super(config);
    }

    /**
    * Resolve a URI
    * @param href The relative or absolute URI. May be an empty string. May contain
    * a fragment identifier starting with "#", which must be the value of an ID attribute
    * in the referenced XML document.
    * @param base The base URI that should be used. May be null if uri is absolute.
    * @return a Source object representing an XML document
    */

    public Source resolve(String href, String base)
    throws XPathException {

        String relativeURI = href;
        String id = null;

        // Extract any fragment identifier. Note, this code is no longer used to
        // resolve fragment identifiers in URI references passed to the document()
        // function: the code of the document() function handles these itself.

        int hash = href.indexOf('#');
        if (hash>=0) {
            relativeURI = href.substring(0, hash);
            id = href.substring(hash+1);
            // System.err.println("StandardURIResolver, href=" + href + ", id=" + id);
        }

        URI url;
        URI relative;
        try {
            relativeURI = ResolveURI.escapeSpaces(relativeURI);
            relative = new URI(relativeURI);
        } catch (URISyntaxException err) {
            throw new DynamicError("Invalid relative URI " + Err.wrap(relativeURI), err);
        }

        Platform platform = JavaPlatform.getInstance();

        try {
            url = platform.makeAbsolute(relativeURI, base);
        } catch (URISyntaxException err) {
            // System.err.println("Recovering from " + err);
            // last resort: if the base URI is null, or is itself a relative URI, we
            // try to expand it relative to the current working directory
            String expandedBase = ResolveURI.tryToExpand(base);
            if (!expandedBase.equals(base)) { // prevent infinite recursion
                return resolve(href, expandedBase);
            }
            //err.printStackTrace();
            throw new DynamicError("Invalid URI " + Err.wrap(relativeURI) + " - base " + Err.wrap(base), err);
        }

        try {
            Builder builder = new Builder();
            Document doc = builder.build(url.toString());
            if (getConfiguration() == null) {
                throw new DynamicError("XomUriResolver requires access to the Configuration");
            }
            DocumentWrapper wrapper = new DocumentWrapper(doc, url.toString(), getConfiguration());
            return wrapper;
        } catch (IOException io) {
            throw new DynamicError(io);
        } catch (ParsingException pe) {
            throw new DynamicError(pe);
        }

    }

    /**
     * Handle a PTree source file (Saxon-SA only)
     */

    protected Source getPTreeSource(String href, String base) throws XPathException {
        return null;
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
// The Original Code is: all this file.
//
// The Initial Developer of the Original Code is Michael H. Kay.
//
// Portions created by (your name) are Copyright (C) (your legal entity). All Rights Reserved.
//
// Contributor(s): none.
//
