////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
// Copyright (c) 2014 Saxonica Limited.
// This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
// If a copy of the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
// This Source Code Form is "Incompatible With Secondary Licenses", as defined by the Mozilla Public License, v. 2.0.
////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

package net.sf.saxon.option.cpp;

import net.sf.saxon.om.SequenceTool;
import net.sf.saxon.query.QueryResult;
import net.sf.saxon.s9api.*;
import net.sf.saxon.trans.XPathException;

import javax.xml.transform.Source;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Properties;

/**
 * * XQuery engine class to use with Saxon/C on C++
 */
public class XQueryEngine extends SaxonCAPI {

    private XQueryExecutable executable = null;
    private File queryFile = null;

    private boolean serializerSet = false;

    /**
     * Default Constructor to initialise XQueryEngine. s9api Processor is created with license flag as false
     *
     */
    public XQueryEngine() {
        super(false);
    }

    /**
     * Constructor to initialise XQueryEngine with license flag
     *
     * @param license - flag indicating presence of license file
     */
    public XQueryEngine(boolean license) {
        super(license);
    }

    public XdmNode parseXMLString(String xml) throws SaxonApiException {
        return SaxonCAPI.xmlParseString(processor, null, xml);
    }

    public void setXQueryFile(String queryFileName) throws SaxonApiException {
        queryFile = new File(queryFileName);
    }

    /**
     * Constructor to initialise XQueryEngine with processor and license flag
     *
     * @param proc    - s9api processor
     * @param license - flag indicating presence of license file
     */
    public XQueryEngine(Processor proc, boolean license) {
        super(proc, license);
    }


    public XQueryEvaluator xqueryEvaluator(String cwd, String[] params, Object[] values) throws SaxonApiException {
        clearExceptions();

        XQueryCompiler compiler = processor.newXQueryCompiler();
        String query = null;
        if (params != null && params.length != values.length) {
            throw new SaxonApiException("Length of params array not equal to the length of values array");
        }

        if (params != null && params.length != 0) {
            for (int i = 0; i < params.length; i++) {
                if (params[i].equals("qs")) {
                    query = (String) values[i];
                    executable = compiler.compile(query);
                } else if (params[i].equals("q")) {
                    if (cwd != null && cwd.length() > 0 && cwd.startsWith("http")) {
                        URI cwdURI = null;
                        if (!cwd.endsWith("/")) {
                            cwd = cwd.concat("/");
                        }
                        try {
                            cwdURI = new URI(cwd + (String) values[i]);
                            URL url = cwdURI.toURL();
                            InputStream in = url.openStream();
                            executable = compiler.compile(in);
                            queryFile = new File("");
                        } catch (URISyntaxException e) {
                            throw new SaxonApiException(e);
                        } catch (MalformedURLException e) {
                            throw new SaxonApiException(e);
                        } catch (IOException e) {
                            throw new SaxonApiException(e);
                        }

                    } else {
                        queryFile = resolveFile(cwd, (String) values[i]);

                        try {
                            executable = compiler.compile(queryFile);
                        } catch (IOException e) {
                            throw new SaxonApiException(e);
                        }
                    }
                } else if (params[i].equals("base")) {
                    String baseURI = (String) values[i];
                    try {
                        compiler.setBaseURI(new URI(baseURI));
                    } catch (URISyntaxException e) {
                        throw new SaxonApiException(e);
                    }
                }
            }
        }


        if (query == null && queryFile == null) {
            throw new SaxonApiException("No Query supplied");
        }

        XQueryEvaluator eval = executable.load();
        applyXQueryProperties(this, cwd, serializer, processor, eval, params, values, props);
        return eval;
    }


    public String executeQueryToString(String cwd, String[] params, Object[] values) throws SaxonApiException {
        XQueryEvaluator eval = xqueryEvaluator(cwd, params, values);
        StringWriter sw = new StringWriter();
        if (props == null) {
            props = new Properties();
            props.setProperty("method", "xml");
            props.setProperty("indent", "yes");
            props.setProperty("omit-xml-declaration", "yes");
        }
        try {
            QueryResult.serializeSequence(eval.evaluate().getUnderlyingValue().iterate(), processor.getUnderlyingConfiguration(), sw, props);
        } catch (XPathException e) {
            SaxonApiException apiEx = new SaxonApiException(e);
            SaxonCException saxonException = new SaxonCException(apiEx);
            saxonExceptions.add(saxonException);
            throw apiEx;
        }
        return sw.toString();
    }

    public XdmValue executeQueryToValue(String cwd, String outFilename, String[] params, Object[] values) throws SaxonApiException {
        XQueryEvaluator eval = xqueryEvaluator(cwd, params, values);
        return eval.evaluate();
    }

    public void executeQueryToFile(String cwd, String outFilename, String[] params, Object[] values) throws SaxonApiException {
        XQueryEvaluator eval = xqueryEvaluator(cwd, params, values);
        if (outFilename != null) {
            serializer = resolveOutputFile(processor, cwd, outFilename);
            eval.run(serializer);
            return;
        }
        eval.run();
    }

    public static void applyXQueryProperties(SaxonCAPI api, String cwd, Serializer serializer, Processor processor, XQueryEvaluator eval, String[] params, Object[] values, Properties props) throws SaxonApiException {

        if (debug) {
            for (int i = 0; i < params.length; i++) {
                System.err.println("Param[" + i + "]:" + params[i]);
            }
        }

        XdmItem item = null;
        String outfile = null;
        File sourceFile = null;
        Source source = null;
        DocumentBuilder builder = processor.newDocumentBuilder();
        //Serializer serializer = processor.newSerializer();
        if (params != null && params.length != 0) {
            for (int i = 0; i < params.length; i++) {
                if (params[i].startsWith("!")) {
                    String name = params[i].substring(1);
                    Serializer.Property prop = null;//Serializer.Property.get(name);
                    serializer.setOutputProperty(prop, (String) values[i]);
                } else if (params[i].equals("o") && outfile == null) {
                    outfile = (String) values[i];
                    serializer = api.resolveOutputFile(processor, cwd, outfile);
                    eval.setDestination(serializer);
                } else if (params[i].equals("dtd")) {
                    String option = (String) values[i];
                    if (option.equals("on")) {
                        builder.setDTDValidation(true);
                    } else {
                        builder.setDTDValidation(false);
                    }

                } else if (params[i].equals("s")) {
                    source = api.resolveFileToSource(cwd, (String) values[i]);
                    eval.setSource(source);

                } else if (params[i].equals("item") || params[i].equals("node")) {
                    Object value = values[i];
                    if (value instanceof XdmItem) {
                        item = (XdmItem) value;
                    }
                    eval.setContextItem(item);
                } else if (params[i].equals("resources")) {
                    if (SaxonCAPI.RESOURCES_DIR == null) {
                        String dir1 = (String)values[i];
                            if (!dir1.endsWith("/")) {
                               dir1 = cwd.concat("/");
                            }
                            SaxonCAPI.RESOURCES_DIR = dir1;
                    }

                } else if (params[i].startsWith("param:")) {
                    String paramName = params[i].substring(6);
                    Object value = values[i];
                    XdmValue valueForCpp;
                    if (value instanceof XdmValue) {
                        valueForCpp = (XdmValue) value;
                        if (debug) {
                            System.err.println("XQuery localname: " + paramName);
                            System.err.println("XQuery: " + valueForCpp.getUnderlyingValue().toString());
                            net.sf.saxon.type.ItemType suppliedItemType = SequenceTool.getItemType(valueForCpp.getUnderlyingValue(), processor.getUnderlyingConfiguration().getTypeHierarchy());
                            System.err.println("XQuery: " + valueForCpp.getUnderlyingValue());
                            System.err.println("XQuery: " + suppliedItemType.toString());
                        }

                        QName qname = QName.fromClarkName(paramName);
                        eval.setExternalVariable(qname, valueForCpp);
                    }
                }
            }
            if (sourceFile != null) {
                eval.setSource(builder.build(sourceFile).asSource());

            }
        }


    }

    public static void main(String[] args) throws Exception {


        String sourcefile = "xmark10.xml";
        String q1 = "q1";
        String outfile = "outfile.xml";
        XQueryEngine xquery = new XQueryEngine(false);
        XdmNode doc = xquery.xmlParseString(null, "<bookstore>\n" +
                "\n" +
                "<book category=\"COOKING\">\n" +
                "  <title lang=\"en\">Everyday Italian</title>\n" +
                "  <author>Giada De Laurentiis</author>\n" +
                "  <year>2005</year>\n" +
                "  <price>30.00</price>\n" +
                "</book></bookstore>", null, null);
        //XdmValueForCpp resultNode2 = new XdmValueForCpp(doc);
        String[] params1 = {"node", "s", "q"};
        Object[] values1 = {doc, "xmark10.xml", "q8.xq"};

        String[] params2 = {"node", "qs"};
        Object[] values2 = {doc, "/bookstore/book/title"};
        String cwd = "/Users/ond1/work/development/files/xmark";
        //String cwd = "C:///www///html///query";
        //String cwd = "http://localhost/query";
        xquery.executeQueryToFile(cwd, "output1a.xml", params1, values1);
        String result = xquery.executeQueryToString(cwd, params2, values2);
        String result2 = xquery.executeQueryToString(cwd, params1, values1);

        //XdmValueForCpp result3 = xquery.executeQueryToValue("", "", params2, values2);
        // xquery.executeQueryToFile("/home/ond1/test/saxon9-5-1-1source", outfile, params1, values1);
        System.out.println("Result1" + result);
        System.out.println("Result2" + result2);




        QName qname = QName.XS_INTEGER;

        System.out.println(qname.getNamespaceURI());
        System.out.println(qname.getPrefix());
        System.out.println(qname.getLocalName());
        System.out.println(qname.toString());

    }


}
