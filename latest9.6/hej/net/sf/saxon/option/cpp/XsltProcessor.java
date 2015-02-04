////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
// Copyright (c) 2014 Saxonica Limited.
// This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
// If a copy of the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
// This Source Code Form is "Incompatible With Secondary Licenses", as defined by the Mozilla Public License, v. 2.0.
////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

package net.sf.saxon.option.cpp;

import net.sf.saxon.om.SequenceTool;
import net.sf.saxon.s9api.*;
import net.sf.saxon.trans.XPathException;

import javax.xml.transform.Source;
import javax.xml.transform.stream.StreamSource;
import java.io.*;
import java.net.*;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

/**
 * * This class is to use with Saxon/C on C++
 */
public class XsltProcessor extends SaxonCAPI {

    private XsltExecutable executable = null;


    public XsltProcessor(Processor proc, boolean license) {
        super(proc, license);
        if(debug) {
            System.err.println("XsltProcessor constructor(proc, l), Processor: "+System.identityHashCode(proc));
        }
    }

    public XsltProcessor() {
        processor = new Processor(false);
                if(debug) {
            System.err.println("XsltProcessor constructor(), Processor: "+System.identityHashCode(processor));
        }
    }


    public XsltProcessor(boolean license) {
        processor = new Processor(license);
        if(debug) {
            System.err.println("XsltProcessor(l), Processor: "+System.identityHashCode(processor));
        }
    }


    public static XsltProcessor newInstance(boolean license, Processor proc) {
        return new XsltProcessor(proc, license);
    }


    public XsltExecutable createStylesheetFromFile(String cwd, String filename) throws SaxonApiException {

        try {
            clearExceptions();
            XsltCompiler compiler = processor.newXsltCompiler();

            Source source = resolveFileToSource(cwd, filename);

            compiler.setErrorListener(errorListener);

            executable = compiler.compile(source);
            return executable;
        } catch (SaxonApiException ex) {
            if (ex.getErrorCode() == null) {
                throw new SaxonApiException(new XPathException(ex.getMessage(), saxonExceptions.get(0).getErrorCode()));
            }

            throw ex;
        }
    }


    public XsltExecutable createStylesheetFromString(String cwd, String str) throws SaxonApiException {
        clearExceptions();
        XsltCompiler compiler = processor.newXsltCompiler();
        Source source;


        StringReader reader = new StringReader(str);

        source = new StreamSource(reader);
        if (cwd != null && cwd.length() > 0) {
            if (!cwd.endsWith("/")) {
                cwd = cwd.concat("/");
            }
            source.setSystemId(cwd + "file");

        }

        compiler.setErrorListener(errorListener);
        try {
            executable = compiler.compile(source);
            return executable;
        } catch (SaxonApiException ex) {
            throw new SaxonApiException(new XPathException(ex.getMessage(), (ex.getErrorCode() == null ? saxonExceptions.get(0).getErrorCode() : "")));
        }

    }


    public void transformToFile(String cwd, String sourceFilename, String stylesheet, String outFilename, String[] params, Object[] values) throws SaxonApiException {

        try {
            clearExceptions();
            serializer = null;
            Source source;
            XsltTransformer transformer = null;
            if (stylesheet == null && executable != null) {
                transformer = executable.load();
            } else {
                XsltCompiler compiler = processor.newXsltCompiler();

                source = resolveFileToSource(cwd, stylesheet);

                compiler.setErrorListener(errorListener);

                try {
                    transformer = compiler.compile(source).load();
                } catch (SaxonApiException ex) {
                    if (ex.getErrorCode() == null) {
                        throw new SaxonApiException(new XPathException(ex.getMessage(), saxonExceptions.get(0).getErrorCode()));
                    }
                }
            }
            if (outFilename != null) {
               serializer = resolveOutputFile(processor, cwd, outFilename);
            }

            applyXsltTransformerProperties(this, cwd, processor, transformer, params, values, props);
            if (sourceFilename == null && doc != null) {
                transformer.setInitialContextNode(doc);
            } else if (sourceFilename != null) {
                source = resolveFileToSource(cwd, sourceFilename);

                transformer.setSource(source);
            }
            if (serializer == null) {
                throw new SaxonApiException("Output file not set for this transformation");
            }
            transformer.setDestination(serializer);
            transformer.transform();
            serializer = null;

        } catch (SaxonApiException e) {
            SaxonCException saxonException = new SaxonCException(e);
            saxonExceptions.add(saxonException);
            throw e;
        } catch(NullPointerException ex) {
            throw new SaxonApiException(ex);
        }
    }


    public static void applyXsltTransformerProperties(SaxonCAPI api, String cwd, Processor processor, XsltTransformer transformer, String[] params, Object[] values, Properties props) throws SaxonApiException {
        if (params != null) {
            String initialTemplate;
            String initialMode ;
            XdmNode node;
            String outfile = null;
            Source source;
            DocumentBuilder builder = processor.newDocumentBuilder();
            Map<Serializer.Property, String> propsList = new HashMap<Serializer.Property, String>();
            if (params.length != values.length) {
                throw new SaxonApiException("Length of params array not equal to the length of values array");
            }
            if (params.length != 0) {
                if (cwd != null && cwd.length() > 0) {
            if (!cwd.endsWith("/")) {
                cwd = cwd.concat("/");
            }
                }
                for (int i = 0; i < params.length; i++) {
                    if (params[i].startsWith("!")) {
                        String name = params[i].substring(1);
                        Serializer.Property prop = Serializer.Property.get(name);
                        if(prop == null) {
                            throw new SaxonApiException("Property name "+name+ " not found");
                        }
                        propsList.put(prop, (String) values[i]);
                    } else if (params[i].equals("o") && outfile == null) {
                        outfile = (String) values[i];
                        api.serializer = api.resolveOutputFile(processor, cwd, outfile);
                        transformer.setDestination(api.serializer);
                    } else if (params[i].equals("it")) {
                        initialTemplate = (String) values[i];
                        transformer.setInitialTemplate(new QName(initialTemplate));
                    } else if (params[i].equals("im")) {
                        initialMode = (String) values[i];
                        transformer.setInitialMode(new QName(initialMode));
                    } else if (params[i].equals("s")) {
                       source = api.resolveFileToSource(cwd, (String) values[i]);
                        transformer.setSource(builder.build(source).asSource());
                    } else if (params[i].equals("item") || params[i].equals("node")) {
                        Object value = values[i];
                        if (value instanceof XdmNode) {
                            node = (XdmNode) value;
                            transformer.setSource((node).asSource());
                        }
                    } else if(params[i].equals("resources")){
                        char separatorChar = '/';
                        if (SaxonCAPI.RESOURCES_DIR == null) {
                            String dir1 = (String)values[i];
                            if (!dir1.endsWith("/")) {
                               dir1 = dir1.concat("/");
                            }
                             if (File.separatorChar != '/') {
                               dir1.replace(separatorChar, File.separatorChar);
                                separatorChar = '\\';
                                 dir1.replace('/', '\\');
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
                                System.err.println("XSLTTransformerForCpp: " + paramName);
                                System.err.println("XSLTTransformerForCpp: " + valueForCpp.getUnderlyingValue().toString());
                                net.sf.saxon.type.ItemType suppliedItemType = SequenceTool.getItemType(valueForCpp.getUnderlyingValue(), processor.getUnderlyingConfiguration().getTypeHierarchy());
                                System.err.println("XSLTTransformerForCpp: " + valueForCpp.getUnderlyingValue());
                                System.err.println("XSLTTransformerForCpp Type: " + suppliedItemType.toString());
                            }


                            QName qname = QName.fromClarkName(paramName);
                            transformer.setParameter(qname, valueForCpp);
                        } else {
                            if(value instanceof Integer) {

                            }
                        }
                    }

                    //TODO fast track for primitive values
                }
            }
            if(api.serializer != null) {
                for(Map.Entry pairi : propsList.entrySet()){
                        api.serializer.setOutputProperty((Serializer.Property)pairi.getKey(), (String) pairi.getValue());
                }
            }
        }

    }


    public XdmNode transformToNode(String cwd, String sourceFile, String stylesheet, String[] params, Object[] values) throws SaxonApiException {
        Source source;
        clearExceptions();
        XsltTransformer transformer = null;
        if (stylesheet == null && executable != null) {
            transformer = executable.load();
        } else {
            XsltCompiler compiler = processor.newXsltCompiler();
            source = resolveFileToSource(cwd, stylesheet);
            compiler.setErrorListener(errorListener);

            try {
                transformer = compiler.compile(source).load();
            } catch (SaxonApiException ex) {
                if (ex.getErrorCode() == null) {
                    throw new SaxonApiException(new XPathException(ex.getMessage(), saxonExceptions.get(0).getErrorCode()));
                }
            }
        }
        XdmDestination destination = new XdmDestination();

        try {
             transformer.setDestination(destination);
            this.applyXsltTransformerProperties(this, cwd, processor, transformer, params, values, props);

            if (sourceFile == null && doc != null) {
                transformer.setInitialContextNode(doc);
            } else if (sourceFile != null) {
                source = resolveFileToSource(cwd, sourceFile);
                transformer.setSource(source);
            }
            transformer.transform();
            return destination.getXdmNode();
        } catch (SaxonApiException e) {
            SaxonCException saxonException = new SaxonCException(e);
            saxonExceptions.add(saxonException);
            throw e;
        }  catch (NullPointerException ex){
            throw new SaxonApiException(ex);
        }

    }


    //TODO comments needed on method signatures
    public String transformToString(String cwd, String sourceFile, String stylesheet, String[] params, Object[] values) throws SaxonApiException {
        if(debug) {
            System.err.println("xsltApplyStylesheet, Processor: "+System.identityHashCode(processor));
        }

        Source source = null;
        clearExceptions();
        XsltTransformer transformer = null;
        if (stylesheet == null && executable != null) {
            transformer = executable.load();
        } else {
            XsltCompiler compiler = processor.newXsltCompiler();
            source = resolveFileToSource(cwd, stylesheet);
            compiler.setErrorListener(errorListener);

            try {
                transformer = compiler.compile(source).load();
            } catch (SaxonApiException ex) {
                if (ex.getErrorCode() == null) {
                    throw new SaxonApiException(new XPathException(ex.getMessage(), saxonExceptions.get(0).getErrorCode()));
                }
            }
        }
        StringWriter sw = new StringWriter();
        serializer = processor.newSerializer(sw);
        transformer.setDestination(serializer);
        try {
            applyXsltTransformerProperties(this, cwd, processor, transformer, params, values, props);

            if (sourceFile == null && doc != null) {
                transformer.setInitialContextNode(doc);
            } else if (sourceFile != null) {
               source = resolveFileToSource(cwd, sourceFile);
                transformer.setSource(source);
            }
            transformer.transform();
            serializer = null;
            return sw.toString();
        } catch (SaxonApiException e) {
            SaxonCException saxonException = new SaxonCException(e);
            saxonExceptions.add(saxonException);
            throw e;
        }
    }


    public String xsltApplyStylesheet(Processor processor, String sourceFile, String stylesheet, String[] params, Object[] values) throws SaxonApiException {

        clearExceptions();
        XsltCompiler compiler = processor.newXsltCompiler();
        File file = new File(stylesheet);

        StreamSource stream = new StreamSource(file);
        stream.setSystemId(stylesheet);

        compiler.setErrorListener(errorListener);
        XsltTransformer transformer = null;
        try {
            transformer = compiler.compile(stream).load();
        } catch (SaxonApiException ex) {
            if (ex.getErrorCode() == null) {
                throw new SaxonApiException(new XPathException(ex.getMessage(), saxonExceptions.get(0).getErrorCode()));
            }
        }

        try {
            StringWriter sw = new StringWriter();

            if (serializer == null) {
                serializer = processor.newSerializer(sw);
                transformer.setDestination(serializer);
            }

            this.applyXsltTransformerProperties(this, null, processor, transformer, params, values, props);
            if (sourceFile != null) {
                file = new File(sourceFile);
                DocumentBuilder builder = processor.newDocumentBuilder();
                transformer.setSource(builder.build(file).asSource());
            }


            transformer.transform();
            serializer = null;
            return sw.toString();
        } catch (SaxonApiException e) {
            SaxonCException saxonException = new SaxonCException(e);
            saxonExceptions.add(saxonException);
            throw e;
        }


    }

    public static void main(String[] args) throws Exception {
        // String cwd = "/Users/ond1/work/development/svn/saxon-dev/tests/saxon-c/samples/trax";
         String cwd = "/Users/ond1/work/development/tests/jeroen";
       // String cwd = "C:///www///html///trax";
        //String cwd = "http://localhost/trax";
        /*if (args.length > 0) {
            cwd = args[0];
        }      */

        String sourcefile1 = "/Users/ond1/work/development/tests/jeroen/xml/kamervragen.xml";//"sinsello.xml";//"saxon_php3/xmark64.xml";
        String stylesheet12 = "xslt/overzicht-resultaten.xslt";//"cadenaoriginal_3_2.xslt";//""saxon_php3/q8.xsl";//"test.xsl";
        String outfile = "outfile.html";
        Processor processor = new Processor(false);
        XsltProcessor cpp = new XsltProcessor(processor, false);
        String[] params1 = {"resources"};
        Object[] values1 = {"/Users/ond1/work/development/tests/jeroen/data"};
        String outputdoc = cpp.transformToString(cwd, sourcefile1, stylesheet12, params1, values1);
        System.out.println(outputdoc);
        System.exit(0);
       // Processor processor = cpp.getProcessor();
        // XsltTransformer transformer = cpp.xsltParseStylesheetFile(args[0]).load();
        //XdmNode sourceNode = cpp.xmlParseFile(cwd, "xml/foo.xml");
       XdmNode sourceNode2 = SaxonCAPI.xmlParseString(processor, null, "<result><assert-xml file=\"type-0501.out\"/></result>");
        XdmValue node1 = (XdmValue)cpp.createXdmAtomicItem("string","textXXXXX");//new XdmValueForCpp(sourceNode2);

        XdmValue resultNode2 = cpp.xmlParseString("<?xml version=\"1.0\" encoding=\"UTF-8\"?><html><head><title>Untitled</title></head><body leftmargin=\"100\"></body></html>");


        String[] params2 = {"o"};
        Object[] values2 = {"output_test.xml"};
        String[] params3 = {"node", "!indent", "output_test.xml"};
        //Object[] values3 = {"xml/foo.xml"};
        Object[] values3 = {sourceNode2, "yes", "o"};
        cpp.createStylesheetFromFile(cwd, "xsl/foo.xsl");


        String result = "";
        int repeat = 1;

        cpp.createStylesheetFromString("samples", "<xsl:stylesheet xmlns:xsl=\"http://www.w3.org/1999/XSL/Transform\"\n" +
                "    version=\"2.0\" xmlns:pf=\"http://example.com\">\n" +
               "<xsl:param name=\"pf:param-name\"  />" +

                "    \n" +
                "    \n" +
                "    <xsl:template match=\"/\" >\n" +
                "        \n" +
                "        <xsl:copy-of select=\".\"/>\n" +
                "       XXXXXX <xsl:value-of select=\"$pf:param-name\"/>\n" +
                "    </xsl:template>\n" +
                "    \n" +
                "   \n" +
                "</xsl:stylesheet>");
        try {
            long startTime = System.currentTimeMillis();
            for (int i = 0; i < repeat; i++) {
                //result = cpp.xsltApplyStylesheet(cwd, null, "xsl/foo.xsl", params3, values3);
                 result = cpp.transformToString(cwd, null, null, params1, values1);
               // cpp.xsltSaveResultToFile(cwd, null, "xsl/foo.xsl", null, params3, values3);
            }
            long endTime = System.currentTimeMillis();
            System.out.println("output:" + result + " Time:" + ((endTime - startTime) / 5));
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        SaxonCException[] exceptionForCpps = cpp.getExceptions();
        // String result2 = cpp.xsltApplyStylesheet(cpp.getProcessor(), null, stylesheet, params1, values1);
       // System.out.println("output:" + result);


    }


}
