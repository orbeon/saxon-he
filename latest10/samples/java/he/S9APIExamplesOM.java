////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
// Copyright (c) 2019 Saxonica Limited.
// This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
// If a copy of the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
// This Source Code Form is "Incompatible With Secondary Licenses", as defined by the Mozilla Public License, v. 2.0.
////////////////////////////////////////////////////////////////////////////////////////////////////////////////////


package he;
import net.sf.saxon.option.dom4j.DOM4JObjectModel;
import net.sf.saxon.option.jdom2.JDOM2ObjectModel;
import net.sf.saxon.option.xom.XOMObjectModel;
import net.sf.saxon.s9api.*;
import org.w3c.dom.Document;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.*;
import java.util.*;

/**
 * Some examples to show how the Saxon S9API API should be used with source documents supplied
 * using different object models (see S9APIExamples for main S9API examples)
 */
public class S9APIExamplesOM {

    /**
     * Class is not instantiated, so give it a private constructor
     */
    private S9APIExamplesOM() {
    }

    /**
     * Command line entry point.
     * The command should be run with the current working directory set to the "samples" directory.
     *
     * @param argv arguments supplied from the command line. If an argument is supplied, it should be the
     *             name of a test, or "all" to run all tests, or "nonschema" to run all the tests that are not schema-aware.
     */
    public static void main(String[] argv) {

        List<Test> tests = new ArrayList<>();
        tests.add(new S9APIExamplesOM.XPathDOM());
        tests.add(new S9APIExamplesOM.XPathJDOM());
        tests.add(new S9APIExamplesOM.XPathXOM());
        tests.add(new S9APIExamplesOM.XPathDOM4J());
        
        String requestedTest = "all";
        if (argv.length > 0) {
            requestedTest = argv[0];
        }


        boolean found = false;
        for (Test next : tests) {
            if (requestedTest.equals("all") ||
                    (requestedTest.equals("nonschema") && !next.needsSaxonEE()) ||
                    next.name().equals(requestedTest)) {
                found = true;
                try {
                    System.out.println("\n==== " + next.name() + "====\n");
                    next.run();
                } catch (SaxonApiException ex) {
                    handleException(ex);
                }
            }
        }

        if (!found) {
            System.err.println("Please supply a valid test name, or 'all' or 'nonschema' ('" + requestedTest + "' is invalid)");
        }


    }

    private interface Test {

        /**
         * The name of the test
         * @return the name of the test
         */
        String name();

        /**
         * Ask if the test needs Saxon-EE (in some cases Saxon-PE is adequate)
         * @return true if the test will not run with Saxon-HE
         */
        boolean needsSaxonEE();

        /**
         * Run the test
         * @throws SaxonApiException if the test fails
         */
        void run() throws SaxonApiException;
    }

    /**
     * Demonstrate use of an XPath expression against a DOM source document.
     */

    private static class XPathDOM implements S9APIExamplesOM.Test {
        public String name() {
            return "XPathDOM";
        }

        public boolean needsSaxonEE() {
            return false;
        }

        public void run() throws SaxonApiException {
            // Build the DOM document
            File file = new File("data/books.xml");
            DocumentBuilderFactory dfactory = DocumentBuilderFactory.newInstance();
            dfactory.setNamespaceAware(true);
            javax.xml.parsers.DocumentBuilder docBuilder;
            try {
                docBuilder = dfactory.newDocumentBuilder();
            } catch (ParserConfigurationException e) {
                throw new SaxonApiException(e);
            }
            Document doc;
            try {
                doc = docBuilder.parse(new InputSource(file.toURI().toString()));
            } catch (SAXException | IOException e) {
                throw new SaxonApiException(e);
            }
            // Compile the XPath Expression
            Processor processor = new Processor(false);
            DocumentBuilder builder = processor.newDocumentBuilder();
            XdmNode xdmDoc = builder.wrap(doc);
            XPathCompiler xpath = processor.newXPathCompiler();
            XPathExecutable xx = xpath.compile("//ITEM/TITLE");
            // Run the XPath Expression
            XPathSelector selector = xx.load();
            selector.setContextItem(xdmDoc);
            for (XdmItem item : selector) {
                XdmNode node = (XdmNode) item;
                org.w3c.dom.Node element = (org.w3c.dom.Node) node.getExternalNode();
                System.out.println(element.getTextContent());
            }
        }

    }


    /**
     * Demonstrate use of an XPath expression against a JDOM2 source document.
     */

    private static class XPathJDOM implements S9APIExamplesOM.Test {
        public String name() {
            return "XPathJDOM";
        }

        public boolean needsSaxonEE() {
            return false;
        }

        public void run() throws SaxonApiException {
            // Build the JDOM document
            org.jdom2.input.SAXBuilder jdomBuilder = new org.jdom2.input.SAXBuilder();
            File file = new File("data/books.xml");
            org.jdom2.Document doc;
            try {
                doc = jdomBuilder.build(file);
            } catch (org.jdom2.JDOMException | IOException e) {
                throw new SaxonApiException(e);
            }
            Processor processor = new Processor(false);
            processor.getUnderlyingConfiguration().registerExternalObjectModel(new JDOM2ObjectModel());
            DocumentBuilder builder = processor.newDocumentBuilder();
            XdmNode xdmDoc = builder.wrap(doc);
            XPathCompiler xpath = processor.newXPathCompiler();
            XPathExecutable xx = xpath.compile("//ITEM/TITLE");
            XPathSelector selector = xx.load();
            selector.setContextItem(xdmDoc);
            for (XdmItem item : selector) {
                XdmNode node = (XdmNode) item;
                org.jdom2.Element element = (org.jdom2.Element) node.getExternalNode();
                System.out.println(element.getValue());
            }
        }

    }

    /**
     * Demonstrate use of an XPath expression against a XOM source document.
     */

    private static class XPathXOM implements S9APIExamplesOM.Test {
        public String name() {
            return "XPathXOM";
        }

        public boolean needsSaxonEE() {
            return false;
        }

        public void run() throws SaxonApiException {
            // Build the XOM document
            nu.xom.Builder xomBuilder = new nu.xom.Builder();
            File file = new File("data/books.xml");
            nu.xom.Document doc;
            try {
                doc = xomBuilder.build(file);
            } catch (nu.xom.ParsingException | IOException e) {
                throw new SaxonApiException(e);
            }
            Processor processor = new Processor(false);
            processor.getUnderlyingConfiguration().registerExternalObjectModel(new XOMObjectModel());
            DocumentBuilder builder = processor.newDocumentBuilder();
            XdmNode xdmDoc = builder.wrap(doc);
            XPathCompiler xpath = processor.newXPathCompiler();
            XPathExecutable xx = xpath.compile("//ITEM/TITLE");
            XPathSelector selector = xx.load();
            selector.setContextItem(xdmDoc);
            for (XdmItem item : selector) {
                XdmNode node = (XdmNode) item;
                nu.xom.Element element = (nu.xom.Element) node.getExternalNode();
                System.out.println(element.toXML());
            }
        }

    }

    /**
     * Demonstrate use of an XPath expression against a DOM4J source document.
     */

    private static class XPathDOM4J implements S9APIExamplesOM.Test {
        public String name() {
            return "XPathDOM4J";
        }

        public boolean needsSaxonEE() {
            return false;
        }

        public void run() throws SaxonApiException {
            // Build the DOM4J document
            org.dom4j.io.SAXReader dom4jBuilder = new org.dom4j.io.SAXReader();
            File file = new File("data/books.xml");
            org.dom4j.Document doc;
            try {
                doc = dom4jBuilder.read(file);
            } catch (org.dom4j.DocumentException e) {
                throw new SaxonApiException(e);
            }
            Processor processor = new Processor(false);
            processor.getUnderlyingConfiguration().registerExternalObjectModel(new DOM4JObjectModel());
            DocumentBuilder builder = processor.newDocumentBuilder();
            XdmNode xdmDoc = builder.wrap(doc);
            XPathCompiler xpath = processor.newXPathCompiler();
            XPathExecutable xx = xpath.compile("//ITEM/TITLE");
            XPathSelector selector = xx.load();
            selector.setContextItem(xdmDoc);
            for (XdmItem item : selector) {
                XdmNode node = (XdmNode) item;
                org.dom4j.Element element = (org.dom4j.Element) node.getExternalNode();
                System.out.println(element.asXML());
            }
        }

    }


    /**
     * Handle an exception thrown while running one of the examples
     *
     * @param ex the exception
     */
    private static void handleException(Exception ex) {
        System.out.println("EXCEPTION: " + ex);
        ex.printStackTrace();
    }

}


