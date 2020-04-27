////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
// Copyright (c) 2014 Saxonica Limited.
// This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
// If a copy of the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
// This Source Code Form is "Incompatible With Secondary Licenses", as defined by the Mozilla Public License, v. 2.0.
////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

package net.sf.saxon.option.cpp;

import com.saxonica.functions.extfn.cpp.CPPFunctionSet;
import com.saxonica.functions.extfn.cpp.PHPFunctionSet;
import net.sf.saxon.Configuration;
import net.sf.saxon.s9api.*;

import javax.xml.transform.Source;
import java.io.*;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

/**
 * * XQuery engine class to use with Saxon/C on C++
 */
public class XQueryEngine extends SaxonCAPI {

    private XQueryExecutable executable = null;

    /**
     * Default Constructor to initialise XQueryEngine. s9api Processor is created with license flag as false
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
        Configuration config = processor.getUnderlyingConfiguration();
        schemaAware = config.isLicensedFeature(Configuration.LicenseFeature.ENTERPRISE_XSLT);
//#if EE==true || PE==true
        if (config.isLicensedFeature(Configuration.LicenseFeature.PROFESSIONAL_EDITION)) {
            config.getBuiltInExtensionLibraryList().addFunctionLibrary(PHPFunctionSet.getInstance());
            config.getBuiltInExtensionLibraryList().addFunctionLibrary(CPPFunctionSet.getInstance());
        }
//#endif
    }

    /**
     * Constructor to initialise XQueryEngine with processor and license flag
     *
     * @param proc - s9api processor
     */
    public XQueryEngine(Processor proc) {
        super(proc);
        Configuration config = processor.getUnderlyingConfiguration();
        schemaAware = config.isLicensedFeature(Configuration.LicenseFeature.ENTERPRISE_XSLT);
//#if EE==true || PE==true
        if (config.isLicensedFeature(Configuration.LicenseFeature.PROFESSIONAL_EDITION)) {
            config.getBuiltInExtensionLibraryList().addFunctionLibrary(PHPFunctionSet.getInstance());
            config.getBuiltInExtensionLibraryList().addFunctionLibrary(CPPFunctionSet.getInstance());
        }
//#endif
    }

    public XdmNode parseXMLString(String xml) throws SaxonApiException {
        try {
            return SaxonCAPI.parseXmlString(null, processor, null, xml);
        } catch (SaxonApiException ex) {
            throw ex;
        }
    }





    Map<String, Object> convertArraysToMap(XQueryCompiler compiler, Serializer serializer, String[] params, Object[] values, Map<QName, XdmValue> parameters, Properties props) throws SaxonApiException {
            Map<String, Object> map = new HashMap();

            if (params == null || values == null) {
                return map;
            }
            if (params.length != values.length) {
                throw new SaxonApiException("Saxon/C internal Error: params array length does not match values length");
            }
            for (int i = 0; i < params.length; i++) {
                if (params[i].startsWith("!")) {
                    String name = params[i].substring(1);
                    Serializer.Property prop = Serializer.Property.get(name);
                    if (prop == null) {
                        throw new SaxonApiException("Property name " + name + " not found");
                    }
                    props.put(prop.getQName().getClarkName(),  values[i]);
                } else if (params[i].startsWith("--") && values[i] != null) {
                    try {
                        processor.setConfigurationProperty("http://saxon.sf.net/feature/" + params[i].substring(2), (String) values[i]);
                    } catch (IllegalArgumentException err) {
                        throw new SaxonApiException(err.getMessage());
                    }
                } else if (params[i].equals("sa")) {
                    String value = ((String)values[i]).toLowerCase();
                    if(value.equals("true") || value.equals("on")) {
                        schemaAware = true;
                    } else {
                        schemaAware = false;
                    }

                } else if (params[i].equals("base")) {
                    String baseURI = (String) values[i];
                    try {
                        compiler.setBaseURI(new URI(baseURI));
                    } catch (URISyntaxException e) {
                        SaxonApiException ex = new SaxonApiException(e);
                        throw ex;
                                    }
                } else if (params[i].startsWith("ns-prefix:")) {
                    String prefix = params[i].substring(10);
                    String namespace = null;
                    if(values[i] instanceof String) {
                        namespace = (String) values[i];
                    } else {
                        throw new SaxonApiException("XQuery declare-namespace error: namespace is not a String value");
                    }
                    compiler.declareNamespace(prefix, namespace);

                } else if (params[i].startsWith("param:")) {
                    String paramName = params[i].substring(6);
                    Object value = values[i];
                    XdmValue valueForCpp = null;
                    QName qname = QName.fromClarkName(paramName);
                    valueForCpp = convertObjectToXdmValue(values[i]);
                    if (debug) {
                        System.err.println("DEBUG: SaxonCAPI param: " + paramName);
                    }

                    if (qname != null && valueForCpp != null) {
                            if(parameters != null) {
                                parameters.put(qname, valueForCpp);

                            }
                        }

                }else {
                    if (debug) {
                        System.err.println("DEBUG: SaxonCAPI: param-name: " +params[i] + ", type of Value= "+values[i].getClass().getName());
                    }
                    map.put(params[i], values[i]);
                }
            }
            if(schemaAware) {
                compiler.setSchemaAware(true);
            }
            return map;

        }


    public XQueryEvaluator xqueryEvaluator(String cwd, String[] params, Object[] values, Serializer serializer) throws SaxonApiException {
        File queryFile = null;
        XQueryCompiler compiler = processor.newXQueryCompiler();
        Map<QName, XdmValue> parameters = new HashMap<>();
        Properties props = new Properties();
        Map<String, Object> optionsMap = convertArraysToMap(compiler, serializer, params, values, parameters, props);
        //compiler.setErrorListener(errorListener);
        String query = null;
        if (params != null && params.length != values.length) {
            SaxonCException ex = new SaxonCException("Length of params array not equal to the length of values array");
            throw ex;
        }


        if (params != null && params.length != 0) {
                if (optionsMap.containsKey("qs")) {
                    query = (String) optionsMap.get("qs");
                    executable = compiler.compile(query);
                } else if (optionsMap.containsKey("q")) {
                    if (cwd != null && cwd.length() > 0 && cwd.startsWith("http")) {
                        URI cwdURI = null;
                        if (!cwd.endsWith("/")) {
                            cwd = cwd.concat("/");
                        }
                        try {
                            cwdURI = new URI(cwd + (String) optionsMap.get("q"));
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
                        queryFile = resolveFile(cwd, (String) optionsMap.get("q"));

                        try {
                            executable = compiler.compile(queryFile);
                        } catch (IOException e) {
                            SaxonApiException ex = new SaxonApiException(e);
                            throw ex;
                        }
                    }
                }
            
        }


        if (query == null && queryFile == null) {
            SaxonApiException ex = new SaxonApiException("No Query supplied");
            throw ex;
        }

        XQueryEvaluator eval = executable.load();

        try {
            applyXQueryProperties(this, cwd, serializer, processor, eval, optionsMap, parameters, props);
        } catch (SaxonApiException ex) {
            throw ex;
        }
        return eval;
    }


    public byte[] executeQueryToString(String cwd, String[] params, Object[] values) throws SaxonApiException {

        ByteArrayOutputStream bStream = new ByteArrayOutputStream();
        Serializer serializer = processor.newSerializer(bStream);
        XQueryEvaluator eval = xqueryEvaluator(cwd, params, values, serializer);
        eval.run(serializer);
        return bStream.toByteArray();

    }

    public XdmValue executeQueryToValue(String cwd, String[] params, Object[] values) throws SaxonApiException {
        XQueryEvaluator eval = xqueryEvaluator(cwd, params, values, null);
        return eval.evaluate();
    }

    public void executeQueryToFile(String cwd, String outFilename, String[] params, Object[] values) throws SaxonApiException {
        try {
            Serializer serializer = resolveOutputFile(processor, cwd, outFilename);
            XQueryEvaluator eval = xqueryEvaluator(cwd, params, values,  serializer);
            if (outFilename != null) {

                eval.run(serializer);
                return;
            }
            eval.run();
        } catch (SaxonApiException ex) {
            SaxonCException saxonException = new SaxonCException(ex);
            throw ex;

        }
    }

    public static void applyXQueryProperties(SaxonCAPI api, String cwd, Serializer serializer, Processor processor, XQueryEvaluator eval, Map<String, Object> optionsMap, Map<QName, XdmValue> parameters, Properties props) throws SaxonApiException {

        if (debug) {
            for (Object key : optionsMap.keySet()) {
                System.err.println((String)key);
            }
        }

        XdmItem item = null;
        File sourceFile = null;
        Source source = null;
        DocumentBuilder builder = processor.newDocumentBuilder();
        if (optionsMap.size() > 0) {

                /*if (params[i].startsWith("!")) {
                    String name = params[i].substring(1);

                    if(!(values[i] instanceof String)) {
                        throw new SaxonApiException("Property with name " + name + " has value which is not a string");
                    }


                    Serializer.Property prop = Serializer.Property.get(name);
                    if (prop == null) {
                        System.err.println("Property name " + name + " not found");
                        continue;

                    }

                    props.put(prop.getQName().getClarkName(), values[i]);
                    if(serializer != null) {
                        serializer.setOutputProperty(prop, (String) values[i]);
                    }
                } else */

                if (optionsMap.containsKey("o")) {
                    serializer = api.resolveOutputFile(processor, cwd, (String) optionsMap.get("o"));
                    eval.setDestination(serializer);
                } else if (optionsMap.containsKey("dtd")) {
                    String option = (String) optionsMap.get("dtd");
                    if (option.equals("on")) {
                        builder.setDTDValidation(true);
                    } else {
                        builder.setDTDValidation(false);
                    }

                } else if (optionsMap.containsKey("s")) {
                    source = api.resolveFileToSource(cwd, (String) optionsMap.get("s"));
                    eval.setSource(source);

                } else if (optionsMap.containsKey("extc")) {
                    //extension function library path
                    String libName = (String) optionsMap.get("extc");
                    SaxonCAPI.setLibrary(cwd, libName);


                } else if (optionsMap.containsKey("item") ) {
                    Object value = optionsMap.get("item");
                    if (value instanceof XdmItem) {
                        item = (XdmItem) value;
                    }
                    eval.setContextItem(item);
                } else if (optionsMap.containsKey("node")) {
                    Object value = optionsMap.get("node");
                    if (value instanceof XdmItem) {
                        item = (XdmItem) value;
                    }
                    eval.setContextItem(item);
                }else if (optionsMap.containsKey("resources")) {
                    if (SaxonCAPI.RESOURCES_DIR == null) {
                        String dir1 = (String) optionsMap.get("resources");
                        if (!dir1.endsWith("/")) {
                            dir1 = cwd.concat("/");
                        }
                        SaxonCAPI.RESOURCES_DIR = dir1;
                    }

                }

            }

        if (!props.isEmpty() && serializer != null) {
            serializer.setOutputProperties(props);
        }

        for(Map.Entry<QName, XdmValue> entry : parameters.entrySet()) {
            eval.setExternalVariable(entry.getKey(), entry.getValue());
        }



    }

    public static void main(String[] args) throws Exception {


        String sourcefile = "xmark10.xml";
        String q1 = "q1";
        String outfile = "outfile.xml";
        Processor processor = new Processor(true);
        String[] paramcon = {"l"};

        String[] valuesCon = {"on"};
        SaxonCAPI.applyToConfiguration(processor, paramcon, valuesCon);
        XQueryEngine xquery = new XQueryEngine(processor);
        XdmNode doc = xquery.parseXmlString(null, "<bookstore>\n" +
                "\n" +
                "<book category=\"COOKING\">\n" +
                "  <title lang=\"en\">Everyday Italian</title>\n" +
                "  <author>Giada De Laurentiis</author>\n" +
                "  <year>2005</year>\n" +
                "  <price>30.00</price>\n" +
                "</book></bookstore>");

        String[] params1 = {"s", "qs"};
        Object[] values1 = {"xmark10.xml", "count(//*)"};


        String[] params2 = {"node", "qs"};
        String[] params3 = {"qs"};
        String[] params4 = {"qs"};
        Object[] values2 = {doc, "saxon:line-number(/bookstore/book/title)"};
        Object[] values3 = {"declare variable $n external := 10; (1 to $n)!(. * .)"};
        Object[] value4 = {"declare namespace output = \"http://www.w3.org/2010/xslt-xquery-serialization\";\n" +
                "\n" +
                "declare option output:method 'json';\n" +
                "\n" +
                "map { 'prop1' : 'value 1', 'prop2' : 'value 2' }"};
        String cwd = "/Users/ond1/work/development/files/xmark";
        //String cwd = "C:///www///html///query";
        //String cwd = "http://localhost/query";
        //xquery.executeQueryToFile(cwd, "output1a.xml", params1, values1);
        String result0 = new String(xquery.executeQueryToString(cwd, params2, values2));
        //String result2 = xquery.executeQueryToString(cwd, params1, values1);
        XdmValue result = xquery.executeQueryToValue(cwd, params4, value4);

        String result2 = new String(xquery.executeQueryToString(cwd, params4, value4));


        // xquery.executeQueryToFile("/home/ond1/test/saxon9-5-1-1source", outfile, params1, values1);
        System.out.println("Result1=" + result.toString());

        System.out.println("Result2=" + result2);
        //System.out.println("Result2" + result2);


        QName qname = QName.XS_INTEGER;

        System.out.println(qname.getNamespaceURI());
        System.out.println(qname.getPrefix());
        System.out.println(qname.getLocalName());
        System.out.println(qname.toString());

    }


}
