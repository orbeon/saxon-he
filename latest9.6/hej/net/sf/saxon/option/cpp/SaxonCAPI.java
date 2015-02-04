////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
// Copyright (c) 2014 Saxonica Limited.
// This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
// If a copy of the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
// This Source Code Form is "Incompatible With Secondary Licenses", as defined by the Mozilla Public License, v. 2.0.
////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

package net.sf.saxon.option.cpp;


import net.sf.saxon.lib.ConversionRules;
import net.sf.saxon.lib.NamespaceConstant;
import net.sf.saxon.lib.StandardErrorListener;
import net.sf.saxon.om.SequenceTool;
import net.sf.saxon.om.StandardNames;
import net.sf.saxon.s9api.*;
import net.sf.saxon.trans.XPathException;
import net.sf.saxon.type.BuiltInAtomicType;
import net.sf.saxon.type.BuiltInType;
import net.sf.saxon.type.Converter;
import net.sf.saxon.value.StringValue;
import org.xml.sax.InputSource;

import javax.xml.transform.ErrorListener;
import javax.xml.transform.Source;
import javax.xml.transform.TransformerException;
import javax.xml.transform.sax.SAXSource;
import javax.xml.transform.stream.StreamSource;
import java.io.*;
import java.net.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

public class SaxonCAPI {
    protected Properties props = null;
    protected Processor processor = null;
    protected XdmNode doc = null;
    protected List<SaxonCException> saxonExceptions = new ArrayList<SaxonCException>();
    protected static boolean debug = false;
    protected Serializer serializer = null;
    protected InputStream in = null;
    public static String RESOURCES_DIR = null;

    public SaxonCAPI() {
        processor = new Processor(false);
        if (debug) {
            System.err.println("New processor created in SAxonCAPI(), Processor: " + System.identityHashCode(processor));
        }
    }

    public SaxonCAPI(boolean license) {
        processor = new Processor(license);
        if (debug) {
            System.err.println("New processor created in SAxonCAPI(l), Processor: " + System.identityHashCode(processor));
        }
    }


    public SaxonCAPI(Processor proc, boolean license) {
        if (proc == null) {
            processor = new Processor(license);
            if (debug) {
                System.err.println("New processor created, Processor: " + System.identityHashCode(processor));
            }
        } else {
            processor = proc;
            if (debug) {
                System.err.println("processor used in SAxonCAPI(proc, l), Processor: " + System.identityHashCode(processor));
            }
        }
    }

    public SchemaValidator registerSchema(String cwd, String xsd) throws SaxonApiException {
        //TODO sort out the handling of options on the SchemaValidator
        SchemaManager schemaManager = processor.getSchemaManager();
        Source source_xsd = resolveFileToSource(cwd, xsd);

        SchemaValidator validator = schemaManager.newSchemaValidator();
        validator.setErrorListener(errorListener);
        schemaManager.load(source_xsd);
        return validator;
    }


     protected ErrorListener errorListener = new StandardErrorListener() {

        @Override
        public void error(TransformerException exception){
            SaxonCException saxonException = new SaxonCException((XPathException) exception);
            saxonExceptions.add(saxonException);
            try{
                super.error(exception);
            }catch(Exception ex){}
        }

        @Override
        public void fatalError(TransformerException exception){
            SaxonCException saxonException = new SaxonCException((XPathException) exception);
            saxonExceptions.add(saxonException);
            try{
                super.fatalError(exception);
            }catch(Exception ex){}
        }
    };


    public SchemaManager getSchemaManager(){
        return processor.getSchemaManager();
    }


    public Processor getProcessor() {
        return processor;
    }

    public static void setDebugMode(boolean d) {
        debug = d;
    }

    public InputStream getInputStream() {
        return in;
    }


    public SaxonCException[] getExceptions() {
        return saxonExceptions.toArray(new SaxonCException[saxonExceptions.size()]);
    }

    public boolean checkException() {
        return saxonExceptions.size() > 0;
    }


    public void clearExceptions() {
        saxonExceptions.clear();
    }

    public SaxonCException getException(int i) {
        if (i < saxonExceptions.size()) {
            return saxonExceptions.get(i);
        } else {
            return null;
        }
    }

    public XdmNode xmlParseFile(String cwd, String filename) throws SaxonApiException {
        try {
            doc = xmlParseFile(processor, cwd, null, filename);
            return doc;
        } catch (SaxonApiException ex) {
            SaxonCException saxonException = new SaxonCException(ex);
            saxonExceptions.add(saxonException);
            throw ex;
        }
    }



    public XdmNode xmlParseFile(String cwd, SchemaValidator validator, String filename, String[] params, String[] values) throws SaxonApiException {
        try {
            doc = xmlParseFile(processor, cwd, validator, filename);
            return doc;
        } catch (SaxonApiException ex) {
            SaxonCException saxonException = new SaxonCException(ex);
            saxonExceptions.add(saxonException);
            throw ex;
        }
    }

    public XdmNode xmlParseString(String xml) throws SaxonApiException {
        return xmlParseString(null, xml, null, null);
    }

    public XdmNode xmlParseString(SchemaValidator validator, String xml , String[] params, String[] values) throws SaxonApiException {
        try {
            doc = xmlParseString(processor, validator, xml);
            if (debug) {
                System.err.println("xmlParseString, Processor: " + System.identityHashCode(processor));
            }
            return doc;
        } catch (SaxonApiException ex) {
            SaxonCException saxonException = new SaxonCException(ex);
            saxonExceptions.add(saxonException);
            throw ex;
        }
    }


    public static XdmValue createXdmAtomicItem(String typeStr, String valueStr) throws Exception{


        int fp = StandardNames.getFingerprint(NamespaceConstant.SCHEMA, typeStr);

        BuiltInAtomicType type = (BuiltInAtomicType) BuiltInType.getSchemaType(fp);
        ConversionRules rules = new ConversionRules();
        Converter converter = rules.getConverter(BuiltInAtomicType.STRING, type);

        return XdmValue.wrap(converter.convert(new StringValue(valueStr)).asAtomic());


    }

    //TODO pass the schema file and options as an array instead of the SchemaValidator object
    public static XdmNode xmlParseString(Processor processor, SchemaValidator validator, String xml) throws SaxonApiException {
        try {
            StringReader reader = new StringReader(xml);
            DocumentBuilder builder = processor.newDocumentBuilder();
            if(validator != null) {
               builder.setSchemaValidator(validator);
            }
            XdmNode doc = builder.build(new SAXSource(new InputSource(reader)));
            if (debug) {
                System.err.println("xmlParseString, Processor: " + System.identityHashCode(processor));
                System.err.println("XdmNode: " + System.identityHashCode(doc));
                net.sf.saxon.type.ItemType suppliedItemType = SequenceTool.getItemType(doc.getUnderlyingValue(), processor.getUnderlyingConfiguration().getTypeHierarchy());
                System.err.println("xmlParserString, ItemType: " + suppliedItemType.toString());
            }
            return doc;
        } catch (SaxonApiException ex) {
            throw ex;
        }
    }


    public static XdmNode xmlParseFile(Processor processor, String cwd, SchemaValidator validator, String filename) throws SaxonApiException {
        try {
            DocumentBuilder builder = processor.newDocumentBuilder();
            Source source = null;
            if (cwd != null && cwd.length() > 0) {
                if (!cwd.endsWith("/")) {
                    cwd = cwd.concat("/");
                }
                File file = new File(cwd, filename);
                source = new StreamSource(file);
                source.setSystemId(file.getAbsolutePath());
            } else {
                source = new StreamSource(new File(filename));
            }
            if(validator != null) {
               builder.setSchemaValidator(validator);
            }
            return builder.build(source);
        } catch (SaxonApiException ex) {
            throw ex;
        }
    }

    public File absoluteFile(String cwd, String filename) {
        char separatorChar = '/';
        if (File.separatorChar != '/') {
            filename.replace(separatorChar, File.separatorChar);
            cwd.replace(separatorChar, File.separatorChar);
            separatorChar = '\\';
        }
        if (!cwd.endsWith(String.valueOf(separatorChar))) {
            cwd = cwd.concat(String.valueOf(separatorChar));
        }
        File absFile = new File(filename);
        String fullpath;
        if (!absFile.isAbsolute()) {
            fullpath = cwd + filename;
        } else {
            fullpath = filename;
        }
        return new File(fullpath);
    }

    public File resolveFile(String cwd, String filename) throws SaxonApiException {
        URI cwdURI = null;
        Source source = null;
        File file = null;
        if (cwd != null && cwd.length() > 0) {

            try {

                file = absoluteFile(cwd, filename);

            } catch (Exception e) {
                URL url = null;
                try {
                    cwdURI = new URI(cwd + filename);
                    file = new File(cwdURI);
                } catch (URISyntaxException e1) {
                    throw new SaxonApiException(e1);
                }

            }

            return file;
        } else {
            return new File(filename);
        }
    }


    public Source resolveFileToSource(String cwd, String filename) throws SaxonApiException {

        Source source = null;
        File file = null;

        if (cwd != null && cwd.length() > 0) {
            if (!cwd.endsWith("/")) {
                cwd = cwd.concat("/");
            }

            try {
                if (filename.startsWith("http")) {
                    URI fileURI = null;
                    fileURI = new URI(filename);
                    URL url = fileURI.toURL();
                    InputStream in = url.openStream();
                    source = new StreamSource(in);
                    source.setSystemId(filename);

                } else {

                    file = absoluteFile(cwd, filename);
                    source = new StreamSource(file);
                    source.setSystemId(file.getAbsolutePath());
                }
            } catch (Exception e) {
                URL url = null;
                try {
                    URI fileURI = null;
                    fileURI = new URI(filename);
                    url = fileURI.toURL();
                    InputStream in = url.openStream();
                    source = new StreamSource(in);
                    source.setSystemId(filename);
                } catch (MalformedURLException e1) {
                    throw new SaxonApiException(e);
                } catch (IOException e1) {
                    throw new SaxonApiException(e);
                } catch (URISyntaxException e1) {
                    throw new SaxonApiException(e1);
                }

            }


            return source;
        } else {
            file = new File(filename);
            StreamSource stream = new StreamSource(file);
            stream.setSystemId(filename);
            source = stream;
            return source;
        }
    }

    public Serializer resolveOutputFile(Processor processor, String cwd, String outfile) throws SaxonApiException {
        Serializer serializer = null;
        File file =  absoluteFile(cwd, outfile);

        serializer = processor.newSerializer(file);
        return serializer;
    }

}
