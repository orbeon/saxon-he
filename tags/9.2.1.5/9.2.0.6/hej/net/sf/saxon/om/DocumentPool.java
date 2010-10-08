package net.sf.saxon.om;

import java.io.Serializable;
import java.util.*;

/**
  * An object representing the collection of documents handled during
  * a single transformation.
  *
  * <p>The function of allocating document numbers is performed
  * by the DocumentNumberAllocator in the Configuration, not by the DocumentPool. This has a
  * number of effects: in particular it allows operations involving multiple
  * documents (such as generateId() and document()) to occur in a free-standing
  * XPath environment.</p>
  */

public final class DocumentPool implements Serializable {

    // The document pool ensures that the document()
    // function, when called twice with the same URI, returns the same document
    // each time. For this purpose we use a hashtable from
    // URI to DocumentInfo object.

    private Map<String, DocumentInfo> documentNameMap = new HashMap<String, DocumentInfo>(10);

    // The set of documents known to be unavailable. These documents must remain
    // unavailable for ever!

    private Set<String> unavailableDocuments = new HashSet<String>(10);

    /**
    * Add a document to the pool
    * @param doc The DocumentInfo for the document in question
    * @param uri The document-uri property of the document.
    */

    public void add(DocumentInfo doc, String uri) {
        if (uri!=null) {
            documentNameMap.put(uri, doc);
        }
    }

    /**
    * Get the document with a given document-uri
    * @param uri The document-uri property of the document.
    * @return the DocumentInfo with the given document-uri property if it exists,
    * or null if it is not found.
    */

    public DocumentInfo find(String uri) {
        return documentNameMap.get(uri);
    }

    /**
     * Get the URI for a given document node, if it is present in the pool. This supports the
     * document-uri() function.
     * @param doc The document node
     * @return The uri of the document node, if present in the pool, or the systemId of the document node otherwise
     */

    public String getDocumentURI(NodeInfo doc) {
        Iterator<String> iter = documentNameMap.keySet().iterator();
        while (iter.hasNext()) {
            String uri = iter.next();
            if (find(uri).isSameNodeInfo(doc)) {
                return uri;
            }
        }
        return null;
    }

    /**
     * Release a document from the document pool. This means that if the same document is
     * loaded again later, the source will need to be re-parsed, and nodes will get new identities.
     * @param doc the document to be discarded from the pool
     * @return the document supplied in the doc parameter
     */

    public DocumentInfo discard(DocumentInfo doc) {
        Iterator<String> iter = documentNameMap.keySet().iterator();
        while (iter.hasNext()) {
            String name = iter.next();
            DocumentInfo entry = documentNameMap.get(name);
            if (entry == doc) {
                documentNameMap.remove(name);
                return doc;
            }
        }
        return doc;
    }

    /**
     * Add a document URI to the set of URIs known to be unavailable (because doc-available() has returned
     * false
     * @param uri the URI of the unavailable document
     */

    public void markUnavailable(String uri) {
        unavailableDocuments.add(uri);
    }

    /**
     * Ask whether a document URI is in the set of URIs known to be unavailable, because doc-available()
     * has been previously called and has returned false
     */

    public boolean isMarkedUnavailable(String uri) {
        return unavailableDocuments.contains(uri);
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
