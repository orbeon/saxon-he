package net.sf.saxon.s9api;

import net.sf.saxon.event.Builder;
import net.sf.saxon.event.Receiver;
import net.sf.saxon.event.StreamWriterToReceiver;
import net.sf.saxon.trans.XPathException;

/**
 * This class is an implementation of {@link javax.xml.stream.XMLStreamWriter}, allowing
 * a document to be constructed by means of a series of XMLStreamWriter method calls such
 * as writeStartElement(), writeAttribute(), writeCharacters(), and writeEndElement().
 *
 * <p>The detailed way in which this class is packaged is carefully designed to ensure that
 * if the functionality is not used, the <code>DocumentBuilder</code> is still usable under
 * JDK 1.5 (which does not include javax.xml.stream interfaces).</p>
*/

public class BuildingStreamWriterImpl extends StreamWriterToReceiver implements BuildingStreamWriter {

    Builder builder;

    public BuildingStreamWriterImpl(Receiver receiver, Builder builder) {
        super(receiver);
        this.builder = builder;
        builder.open();
    }

    /*@Nullable*/ public XdmNode getDocumentNode() throws SaxonApiException {
        try {
            builder.close();
        } catch (XPathException e) {
            throw new SaxonApiException(e);
        }
        return new XdmNode(builder.getCurrentRoot());
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
// The Original Code is: all this file
//
// The Initial Developer of the Original Code is Saxonica Limited.
// Portions created by ___ are Copyright (C) ___. All rights reserved.
//
// Contributor(s):
//
