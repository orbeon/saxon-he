package net.sf.saxon.s9api;

import org.xml.sax.ContentHandler;

/**
 * A SAX {@link ContentHandler} that builds a Saxon tree, and allows the node at the root of the tree
 * to be retrieved on completion
 */
public interface BuildingContentHandler extends ContentHandler {

    /**
     * After building the document by writing a sequence of events, retrieve the root node
     * of the constructed document tree
     * @return the root node of the constructed tree. The result is undefined (maybe null, maybe an exception)
     * if the method is called before successfully completing the sequence of events (of which the last should be
     * {@link #endDocument}) that constructs the tree.
     */

    public XdmNode getDocumentNode() throws SaxonApiException;

}

//
// The contents of this file are subject to the Mozilla Public License Version
// 1.0 (the "License");
// you may not use this file except in compliance with the License. You may
// obtain a copy of the License at http://www.mozilla.org/MPL/
//
// Software distributed under the License is distributed on an "AS IS" basis,
// WITHOUT WARRANTY OF ANY KIND, either express or implied.
// See the License for the specific language governing rights and limitations
// under the License.
//
// The Original Code is: all this file.
//
// The Initial Developer of the Original Code is Michael Kay
//
// Portions created by (your name) are Copyright (C) (your legal entity). All
// Rights Reserved.
//
// Contributor(s): none.
//

