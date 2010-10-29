package net.sf.saxon.s9api;

import javax.xml.stream.XMLStreamWriter;

/**
 * A BuildingStreamWriter allows a document to be constructed by calling the methods defined in the
 * {@link javax.xml.stream.XMLStreamWriter} interface; after the document has been constructed, its root
 * node may be retrieved by calling the getDocumentNode() method.
 *
 * <p>The class will attempt to generate namespace prefixes where none have been supplied, unless the
 * <code>inventPrefixes</code> option is set to false. The preferred mode of use is to call the versions
 * of <code>writeStartElement</code> and <code>writeAttribute</code> that supply the prefix, URI, and
 * local name in full. If the prefix is omitted, the class attempts to invent a prefix. If the URI is
 * omitted, the name is assumed to be in no namespace. The <code>writeNamespace</p> method should be
 * called only if there is a need to declare a namespace prefix that is not used on any element or
 * attribute name.</p>
 *
 * <p>The class will check all names, URIs, and character content for conformance against XML well-formedness
 * rules unless the <code>checkValues</code> option is set to false.</p>
 */

public interface BuildingStreamWriter extends XMLStreamWriter {

    /**
     * After building the document by writing a sequence of events, retrieve the root node
     * of the constructed document tree
     * @return the root node of the constructed tree. The result is undefined (maybe null, maybe an exception)
     * if the method is called before successfully completing the sequence of events (of which the last should be
     * {@link #writeEndDocument}) that constructs the tree.
     */

    public XdmNode getDocumentNode() throws SaxonApiException;

    /**
     * Say whether prefixes are to be invented when none is specified by the user
     * @param invent true if prefixes are to be invented. Default is true.
     */

    public void setInventPrefixes(boolean invent);

    /**
     * Ask whether prefixes are to be invented when none is specified by the user
     * @return true if prefixes are to be invented. Default is true.
     */

    public boolean isInventPrefixes();

    /**
     * Say whether names and values are to be checked for conformance with XML rules
     * @param check true if names and values are to be checked. Default is true.
     */

    public void setCheckValues(boolean check);

    /**
     * Ask whether names and values are to be checked for conformance with XML rules
     * @return true if names and values are to be checked. Default is true.
     */

    public boolean isCheckValues();


}

//
// The contents of this file are subject to the Mozilla Public License Version
// 1.0 (the "License");
// you may not use this file except in compliance with the License. You may
// obtain a copy of the
// License at http://www.mozilla.org/MPL/
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

