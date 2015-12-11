////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
// Copyright (c) 2014 Saxonica Limited.
// This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
// If a copy of the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
// This Source Code Form is “Incompatible With Secondary Licenses”, as defined by the Mozilla Public License, v. 2.0.
////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

package net.sf.saxon.testdriver;


import net.sf.saxon.Version;
import net.sf.saxon.s9api.*;
import net.sf.saxon.value.DateTimeValue;

import javax.xml.stream.XMLStreamException;
import java.io.IOException;
import java.util.Map;

public class QT3TestReport extends TestReport {

    public QT3TestReport(TestDriver testDriver, Spec sp) {
        super(testDriver, sp);
    }

    public void writeResultFilePreamble(Processor processor, XdmNode catalog)
            throws IOException, SaxonApiException, XMLStreamException {
        createWriter(processor);
        String today = DateTimeValue.getCurrentDateTime(null).toDateValue().getStringValue().substring(0, 10);
        XdmNode outermost = (XdmNode) catalog.axisIterator(Axis.CHILD, new QName(QT3TestDriverHE.CNS, "catalog")).next();
        results.writeStartElement("test-suite-result");
        results.writeDefaultNamespace(QT3TestDriverHE.RNS);
        results.writeStartElement("submission");
        results.writeAttribute("anonymous", "false");

        results.writeStartElement("created");
        results.writeAttribute("by", "Michael Kay");
        results.writeAttribute("email", "mike@saxonica.com");
        results.writeAttribute("organization", "Saxonica");
        results.writeAttribute("on", today);
        results.writeEndElement();

        results.writeStartElement("test-run");
        results.writeAttribute("test-suite-version", outermost.getAttributeValue(new QName("", "version")));
        results.writeAttribute("date-run", today);
        results.writeEndElement();

        results.writeStartElement("notes");
        results.writeEndElement(); // notes

        results.writeEndElement(); // submission

        results.writeStartElement("product");

        results.writeAttribute("vendor", "Saxonica");
        results.writeAttribute("name", testDriver.getProductEdition());
        results.writeAttribute("version", Version.getProductVersion());
        results.writeAttribute("released", "false");
        results.writeAttribute("open-source", "false");
        results.writeAttribute("language", spec.specAndVersion);
        results.writeAttribute("bytecode", testDriver.isByteCode()?"on":"off");


        Map<String, QT3TestDriverHE.Dependency> dependencyMap = ((QT3TestDriverHE) testDriver).getDependencyMap();
        if (!dependencyMap.isEmpty()) {
            for (Map.Entry<String, QT3TestDriverHE.Dependency> entry : dependencyMap.entrySet()) {
                QT3TestDriverHE.Dependency dep = entry.getValue();
                if (!"spec".equals(dep.dType)) {
                    results.writeStartElement("dependency");
                    results.writeAttribute("type", dep.dType);
                    results.writeAttribute("value", entry.getKey());
                    results.writeAttribute("satisfied", Boolean.toString(dep.satisfied));
                    results.writeEndElement(); //dependency
                }
            }
        }

        results.writeEndElement(); //product

    }

}

