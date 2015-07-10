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
import net.sf.saxon.om.StructuredQName;
import net.sf.saxon.s9api.*;
import net.sf.saxon.trans.XPathException;
import net.sf.saxon.type.BuiltInAtomicType;
import net.sf.saxon.type.BuiltInType;
import net.sf.saxon.type.Converter;
import net.sf.saxon.value.*;
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



/**
 *  This class holds common attributes and methods required in the XsltProcessor, XQueryEngine and XPathProcessor
 *
 * */

 public class SaxonCAPI {
    protected Properties props = null;
    protected Processor processor = null;
    protected XdmNode doc = null;
    protected List<SaxonCException> saxonExceptions = new ArrayList<SaxonCException>();
    protected List<SaxonCException> saxonWarnings = new ArrayList<SaxonCException>();
    protected static boolean debug = false;
    protected Serializer serializer = null;
    protected InputStream in = null;
    public static String RESOURCES_DIR = null;


    /**
     * Default Constructor. Creates a processor that does not require a license edition
    */
    public SaxonCAPI() {
        processor = new Processor(false);
        if (debug) {
            System.err.println("New processor created in SAxonCAPI(), Processor: " + System.identityHashCode(processor));
        }
    }

    /**
     * Constructor with license edition flag
     * @param license    - specify license edition flag
    */
    public SaxonCAPI(boolean license) {
        processor = new Processor(license);
        if (debug) {
            System.err.println("New processor created in SAxonCAPI(l), Processor: " + System.identityHashCode(processor));
        }
    }

    /**
     * Constructor
     * @param proc    - specify processor object
     * @param license - license edition
    */
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



    /**
     * Error Listener to capture errors
    */
     protected ErrorListener errorListener = new StandardErrorListener() {

        @Override
        public void warning(TransformerException exception) {
            SaxonCException saxonException = new SaxonCException((XPathException) exception);
            saxonWarnings.add(saxonException);
            try{
                super.error(exception);
            }catch(Exception ex){}

        }

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

    /**
     * Get the Schema manager
     * @return SchemaManager
    */
    public SchemaManager getSchemaManager(){
        return processor.getSchemaManager();
    }

    /**
     * Get the Processor object created
     * @return Processor
    */
    public Processor getProcessor() {
        return processor;
    }

    /**
     * set debug mode on or off
     * @param d    - flag for debug mode
    */
    public static void setDebugMode(boolean d) {
        debug = d;
    }

    /**
     * get the input stream
     * @return InputStream - created input stream
    */
    public InputStream getInputStream() {
        return in;
    }


    /**
     * Get the exceptions thrown during the compile and execution of the XSLT/XQuery
     * @return SaxCEExeption[] -- array of the exceptions
    */
    public SaxonCException[] getExceptions() {
        if(saxonExceptions.size() >0) {
            return saxonExceptions.toArray(new SaxonCException[saxonExceptions.size()]);
        } else
            return null;
    }


    /**
     * Check for exceptions thrown
     * @return boolean - Return true if exception thrown during the process and false otherwise.
    */
    public boolean checkException() {
        return saxonExceptions.size() > 0;
    }

    /**
     * Clear exceptions recorded during the process
    */
    public void clearExceptions() {
        saxonExceptions.clear();
    }

    /**
     * Get a particular exceptions
     * @param i - index into the list of thrown exceptions
     * @return SaxonCException - Saxon/C wrapped exception
    */
    public SaxonCException getException(int i) {
        if (i < saxonExceptions.size()) {
            return saxonExceptions.get(i);
        } else {
            return null;
        }
    }


    /**
     * parse XML document supplied by file
     * @param cwd    - Current Working directory
     * @param filename    - File name of the XML document to parse
     * @return XdmNode
    */
    public XdmNode parseXmlFile(String cwd, String filename) throws SaxonApiException {
        try {
            doc = parseXmlFile(processor, cwd, null, filename);
            return doc;
        } catch (SaxonApiException ex) {
            SaxonCException saxonException = new SaxonCException(ex);
            saxonExceptions.add(saxonException);
            throw ex;
        }
    }


    /**
     * parse XML document with addition parameters. Document supplied by file name.
     * @param cwd    - Current Working directory
     * @param validator - Supplied Schema validator
     * @param filename    - File name of the XML document to parse
     * @return  XdmNode
    */
    public XdmNode parseXmlFile(String cwd, SchemaValidator validator, String filename) throws SaxonApiException {
        try {
            doc = parseXmlFile(processor, cwd, validator, filename);
            return doc;
        } catch (SaxonApiException ex) {
            SaxonCException saxonException = new SaxonCException(ex);
            saxonExceptions.add(saxonException);
            throw ex;
        }
    }


    /**
     * parse XML document supplied string
     * @param xml    - string representation of XML document
     * @return XdmNode
    */
    public XdmNode parseXmlString(String xml) throws SaxonApiException {
        return parseXmlString(null, xml);
    }

    /**
     * parse XML document supplied string
     * @param xml    - string representation of XML document
     * @param validator - Supplied Schema validator
     * @return XdmNode
    */
    public XdmNode parseXmlString(SchemaValidator validator, String xml) throws SaxonApiException {
        try {
            doc = parseXmlString(processor, validator, xml);
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


    /**
     * Create an Xdm atomic value from string representation
     * @param typeStr    - Specified type of the XdmValue
     * @param valueStr - The value given in a strign form
     * @return XdmValue - value
    */
    public static XdmValue createXdmAtomicItem(String typeStr, String valueStr) throws Exception{


        int fp = StandardNames.getFingerprint(NamespaceConstant.SCHEMA, typeStr);

        BuiltInAtomicType type = (BuiltInAtomicType) BuiltInType.getSchemaType(fp);
        ConversionRules rules = new ConversionRules();
        Converter converter = rules.getConverter(BuiltInAtomicType.STRING, type);

        return XdmValue.wrap(converter.convert(new StringValue(valueStr)).asAtomic());


    }

    /**
     * Wrap a boxed primitive type as an XdmValue.
     * @param value    - boxed primitive type
     * @return XdmValue
    */
    public static XdmValue getXdmValue(Object value) {
        XdmValue valueForCpp = null;
        if (value instanceof Integer) {
            valueForCpp = XdmValue.wrap(new Int64Value(((Integer) value).intValue()));
        } else if (value instanceof Boolean) {
            valueForCpp = XdmValue.wrap(BooleanValue.get(((Boolean) value).booleanValue()));
        } else if (value instanceof Double) {
            valueForCpp = XdmValue.wrap(DoubleValue.makeDoubleValue(((Double) value).doubleValue()));
        } else if (value instanceof Float) {
            valueForCpp = XdmValue.wrap(FloatValue.makeFloatValue(((Float) value).floatValue()));
        } else if (value instanceof Long) {
            valueForCpp = XdmValue.wrap(Int64Value.makeIntegerValue((((Long) value).longValue())));
        } else if (value instanceof String) {
            valueForCpp = XdmValue.wrap(StringValue.makeStringValue(((String) value)));
        }
        return valueForCpp;
    }

    // This method used to be used internally
    public static XdmNode parseXmlString(Processor processor, SchemaValidator validator, String xml) throws SaxonApiException {
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

    public static String getTypeName(String typeStr) {

        int fp = StandardNames.getFingerprint(NamespaceConstant.SCHEMA, typeStr);

        BuiltInAtomicType typei = (BuiltInAtomicType) BuiltInType.getSchemaType(fp);
        StructuredQName sname = typei.getTypeName();
        return new QName(sname.getPrefix(), sname.getURI(), sname.getLocalPart()).getClarkName();

    }

    public static String getStringValue(XdmValue value) {
        return value.toString();
    }


    public static XdmNode parseXmlFile(Processor processor, String cwd, SchemaValidator validator, String filename) throws SaxonApiException {
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

    /**
     *   Create a File object given the filename and the cwd used fix-up the location of the file.
     *
      * @param cwd - Supply the current working directory which the filename will be resolved against
     * @param filename
     * @return  file object
     */
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

    /**
     * Resolve the file with the cwd
     * deprecated method
     * @param cwd - Current working directory
     * @param filename -
     * @return  File object
     * @throws SaxonApiException
     */
    public File resolveFile(String cwd, String filename) throws SaxonApiException {
        URI cwdURI = null;
        File file = null;
        if (cwd != null && cwd.length() > 0) {

            try {

                file = absoluteFile(cwd, filename);

            } catch (Exception e) {
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


    /**
     *  Resolve file name. Returns the file as a Source object
     * @param cwd - Current working directory
     * @param filename
     * @return Source
     * @throws SaxonApiException
     */
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

    /**
     * Resolve the output file and wrap it into a Serializer for use in XQuery and XSLT processors
     * @param processor - The same processor used in XQuery or XSLT engine must be used. Otherwise errors might occur
     * @param cwd - Current working directory
     * @param outfile  - the output filename where result will be stored
     * @return  Serializer
     * @throws SaxonApiException
     */
    public Serializer resolveOutputFile(Processor processor, String cwd, String outfile) throws SaxonApiException {
        Serializer serializer = null;
        File file =  absoluteFile(cwd, outfile);

        serializer = processor.newSerializer(file);
        return serializer;
    }

}
