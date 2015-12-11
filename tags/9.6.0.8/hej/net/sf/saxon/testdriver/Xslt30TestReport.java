////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
// Copyright (c) 2014 Saxonica Limited.
// This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
// If a copy of the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
// This Source Code Form is “Incompatible With Secondary Licenses”, as defined by the Mozilla Public License, v. 2.0.
////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

package net.sf.saxon.testdriver;


import net.sf.saxon.Version;
import net.sf.saxon.s9api.Processor;
import net.sf.saxon.s9api.SaxonApiException;
import net.sf.saxon.s9api.XdmNode;

import javax.xml.stream.XMLStreamException;
import java.io.IOException;

public class Xslt30TestReport extends TestReport {

    public Xslt30TestReport(TestDriver testDriver, Spec sp) {
        super(testDriver, sp);
    }

    public void writeResultFilePreamble(Processor processor, XdmNode catalog)
            throws IOException, SaxonApiException, XMLStreamException {
        createWriter(processor);
        results.writeStartElement("test-suite-result");
        results.writeDefaultNamespace("http://www.w3.org/2012/11/xslt30-test-results");
        results.writeStartElement("implementation");
        results.writeAttribute("name", testDriver.getProductEdition());
        results.writeAttribute("version", Version.getProductVersion());
        results.writeAttribute("anonymous-result-column", "false");
        results.writeEmptyElement("organization");
        results.writeAttribute("name", "http://www.saxonica.com/");
        results.writeAttribute("anonymous", "false");
        results.writeEmptyElement("submitter");
        results.writeAttribute("name", "Michael Kay");
        results.writeAttribute("email", "mike@saxonica.com");
        //outputDiscretionaryItems();
        results.writeEmptyElement("configuration");
        results.writeAttribute("timeRun", getTime());
        results.writeAttribute("lang", testDriver.lang);
        results.writeAttribute("bytecode", testDriver.isByteCode()?"on":"off");


        results.writeEndElement(); //implementation
        results.writeEmptyElement("test-run");
        results.writeAttribute("dateRun", getDate());
        results.writeAttribute("testsuiteVersion", "3.0.1");



    }

}

