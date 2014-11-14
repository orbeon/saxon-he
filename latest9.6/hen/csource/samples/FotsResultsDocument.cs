﻿using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using Saxon.Api;

namespace TestRunner
{
    class FotsResultsDocument: ResultsDocument
    {

        public FotsResultsDocument(string rdir, Spec sp)
            : base(rdir, sp)
        {
            
        }

        public override void writeResultFilePreamble(Processor processor, XdmNode catalog) { 
                    createWriter(processor);
        String today =  DateTime.Now.ToString();
        XdmNode outermost = (XdmNode)catalog.EnumerateAxis(XdmAxis.Child, new QName(FOTestSuiteDriver.CNS, "catalog")).Current;
        results.WriteStartElement("test-suite-result", FOTestSuiteDriver.RNS);
        results.WriteStartElement("submission");
        results.WriteAttributeString("anonymous", "false");

        results.WriteStartElement("created");
        results.WriteAttributeString("by", "Michael Kay");
        results.WriteAttributeString("email", "mike@saxonica.com");
        results.WriteAttributeString("organization", "Saxonica");
        results.WriteAttributeString("on", today);
        results.WriteEndElement();

        results.WriteStartElement("test-run");
        results.WriteAttributeString("test-suite-version", outermost.GetAttributeValue(new QName("", "version")));
        results.WriteAttributeString("date-run", today);
        results.WriteEndElement();

        results.WriteStartElement("notes");
        results.WriteEndElement(); // notes

        results.WriteEndElement(); // submission

        results.WriteStartElement("product");

        results.WriteAttributeString("vendor", "Saxonica");
        results.WriteAttributeString("name", "Saxon-EEN");
        results.WriteAttributeString("version", processor.ProductVersion);
        results.WriteAttributeString("released", "false");
        results.WriteAttributeString("open-source", "false");
        results.WriteAttributeString("language", ((SpecAttr)spec.GetAttr()).svname);


        /*Map<String, FOTestSuiteDriver.Dependency> dependencyMap = ((FOTestSuiteDriver)testDriver).getDependencyMap();
        if (!dependencyMap.isEmpty()) {
            for (Map.Entry<String, FOTestSuiteDriver.Dependency> entry : dependencyMap.entrySet()) {
                FOTestSuiteDriver.Dependency dep = entry.getValue();
                if (!"spec".Equals(dep.dType)) {
                    results.writeStartElement("dependency");
                    results.writeAttribute("type", dep.dType);
                    results.writeAttribute("value", entry.getKey());
                    results.writeAttribute("satisfied", Boolean.toString(dep.satisfied));
                    results.writeEndElement(); //dependency
                }
            }*/
            //TODO

        results.WriteEndElement(); //product
        }
    }
}
