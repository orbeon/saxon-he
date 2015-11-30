package net.sf.saxon.lib;

import net.sf.saxon.Configuration;
import net.sf.saxon.resource.AbstractResourceCollection;
import net.sf.saxon.trans.XPathException;

/**
 * A ResourceFactory is used for constructing a particular type of resource
 */

public interface ResourceFactory {
    /**
     * Create a Resource with given content
     * @param config the Saxon configuration
     * @param resourceURI the URI identifying the resource
     * @param contentType the content type ( = media type or MIME type) of the resource
     * @param details the stream of bytes making up the binary content of the resource
     * @return the resource
     * @throws XPathException if a failure occurs creating the resource
     */
    Resource makeResource(Configuration config, String resourceURI, String contentType, AbstractResourceCollection.InputDetails details)
        throws XPathException;
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
// The Initial Developer of the Original Code is Saxonica Limited
//
// Portions created by (your name) are Copyright (C) (your legal entity). All Rights Reserved.
//
// Contributor(s): none.
//
