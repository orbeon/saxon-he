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
import net.sf.saxon.lib.AugmentedSource;
import net.sf.saxon.lib.FeatureKeys;
import net.sf.saxon.lib.Validation;
import net.sf.saxon.s9api.*;
import net.sf.saxon.trans.XPathException;
import net.sf.saxon.trans.packages.PackageLibrary;
import sun.security.krb5.internal.crypto.Des;

import javax.xml.transform.Source;
import javax.xml.transform.SourceLocator;
import javax.xml.transform.stream.StreamSource;
import java.io.File;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.*;


//TODO: setParameter('name', 'value')
//TODO: setParameter('name', 'value', 'type')


/**
 * * This class is to use with Saxon/C on C++
 */
public class Xslt30Processor extends SaxonCAPI {

    private XsltExecutable executable = null;
    private List<XdmNode> xslMessages = new ArrayList<XdmNode>();
    private Set<File> packagesToLoad = new HashSet<File>();
    private boolean jitCompilation = false;


    /**
     * Constructor to initialise XsltProcessor with processor and license flag
     *
     * @param proc - s9api processor
     */
    public Xslt30Processor(Processor proc) {
        super(proc);
        Configuration config = processor.getUnderlyingConfiguration();
        schemaAware = config.isLicensedFeature(Configuration.LicenseFeature.ENTERPRISE_XSLT);
        if (debug) {
            System.err.println("Xslt30Processor constructor1(proc, l), Processor: " + System.identityHashCode(proc));
        }
//#if EE==true || PE==true
        if (config.isLicensedFeature(Configuration.LicenseFeature.PROFESSIONAL_EDITION)) {
            if (debug) {
                System.err.println("Xslt30Processor1-1(l), Processor: function libraries added");
            }
            config.getBuiltInExtensionLibraryList().addFunctionLibrary(PHPFunctionSet.getInstance());
            config.getBuiltInExtensionLibraryList().addFunctionLibrary(CPPFunctionSet.getInstance());
        }
//#endif
    }


    /**
     * default Constructor to initialise XsltProcessor. Assume no license file available
     */
    public Xslt30Processor() {
        super();
        if (debug) {
            System.err.println("Xslt30Processor constructor2(), Processor: " + System.identityHashCode(processor));
        }
    }


    /**
     * Constructor to initialise XsltProcessor with license flag
     *
     * @param license - flag indicating presence of license file
     */
    public Xslt30Processor(boolean license) {
        processor = new Processor(license);
        Configuration config = processor.getUnderlyingConfiguration();
        schemaAware = config.isLicensedFeature(Configuration.LicenseFeature.ENTERPRISE_XSLT);
        if (debug) {
            System.err.println("XsltProcessor3(l), Processor: " + System.identityHashCode(processor));
        }
//#if EE==true || PE==true
        if (config.isLicensedFeature(Configuration.LicenseFeature.PROFESSIONAL_EDITION)) {
            if (debug) {
                System.err.println("XsltProcessor3-1(l), Processor: function libraries added");
            }
            config.getBuiltInExtensionLibraryList().addFunctionLibrary(PHPFunctionSet.getInstance());
            config.getBuiltInExtensionLibraryList().addFunctionLibrary(CPPFunctionSet.getInstance());
        }
//#endif

    }


    /**
     * Create new object of this class
     *
     * @param proc - s9api processor
     * @return XsltProcessor
     */
    public static Xslt30Processor newInstance(Processor proc) {
        return new Xslt30Processor(proc);
    }

    public XdmNode[] getXslMessages() {
        return xslMessages.toArray(new XdmNode[xslMessages.size()]);
    }

    public MessageListener newMessageListener() {
        return new MyMessageListener();
    }

    public class MyMessageListener implements MessageListener {

        //TODO: This is not ideal. We should output the xsl-message to the System.err as they happen.
        //Second option is to write them out to a file.

        public void message(XdmNode content, boolean terminate, SourceLocator locator) {
            xslMessages.add(content);
        }
    }

    /**
     * Compile package from source file and save resulting sef to file store.
     *
     * @param cwd         - current working directory
     * @param xsl         - File name of the stylesheet
     * @param outFilename - the file to which the compiled package should be saved
     */
    public void compileFromFileAndSave(String cwd, String xsl, String outFilename) {
        XsltCompiler compiler = processor.newXsltCompiler();
        try {
            Source source = resolveFileToSource(cwd, xsl);
            XsltPackage pack = compiler.compilePackage(source);
            File file = absoluteFile(cwd, outFilename);
            pack.save(file);
        } catch (SaxonApiException e) {
            e.printStackTrace();
        }
    }

    public void setJustInTimeCompilation(boolean jit) {
        jitCompilation = jit;
    }


    /**
     * Compile package from string and save resulting sef to file store.
     *
     * @param cwd      - current working directory
     * @param str      - File name of the stylsheet
     * @param filename - the file to which the compiled package should be saved
     */
    public void compileFromStringAndSave(String cwd, String str, String filename) {

        XsltCompiler compiler = processor.newXsltCompiler();

        try {
            XsltPackage pack = compiler.compilePackage(new StreamSource(new StringReader(str)));
            File file = absoluteFile(cwd, filename);
            pack.save(file);
        } catch (SaxonApiException e) {
            e.printStackTrace();
        }
    }

    /**
     * Compile package from Xdm node and save resulting sef to file store.
     *
     * @param cwd      - current working directory
     * @param obj      -
     * @param filename - the file to which the compiled package should be saved
     */
    public void compileFromXdmNodeAndSave(String cwd, Object obj, String filename) throws SaxonApiException {

        XsltCompiler compiler = processor.newXsltCompiler();
        XdmNode node;
        if (obj instanceof XdmNode) {
            node = (XdmNode) obj;
        } else {
            SaxonCException ex = new SaxonCException("Failed to create Stylesheet from XdoNode");
            saxonExceptions.add(ex);
            throw ex;
        }

        XsltPackage pack = compiler.compilePackage(node.asSource());
        File file = new File(filename);
        pack.save(file);

    }


    public XsltExecutable createStylesheetFromAssoicatedFile(String cwd, String filename, String[] sparams, Object[] values) throws SaxonApiException {

        try {
            clearExceptions();
            XsltCompiler compiler = processor.newXsltCompiler();
            setStaticParametersFromArray(compiler, sparams, values);
            if (jitCompilation) {
                compiler.setJustInTimeCompilation(jitCompilation);
            }
            executable = null;
            if (packagesToLoad.size() > 0) {
                compilePackages(compiler);
            }
            Source source = resolveFileToSource(cwd, filename);

            compiler.setErrorListener(errorListener);
            compiler.setSchemaAware(schemaAware);
            executable = compiler.compile(compiler.getAssociatedStylesheet(source, null, null, null));

            return executable;
        } catch (SaxonApiException ex) {
            SaxonCException ex2 = new SaxonCException(ex);
            saxonExceptions.add(ex2);
            throw ex;
        }
    }


    /**
     * Compile the stylesheet from file  for use later
     *
     * @param cwd      - current working directory
     * @param filename - File name of the stylsheet
     * @return XsltExecutable
     */
    public XsltExecutable createStylesheetFromFile(String cwd, String filename, String[] sparams, Object[] values) throws SaxonApiException {

        try {
            clearExceptions();
            XsltCompiler compiler = processor.newXsltCompiler();
            if (jitCompilation) {
                compiler.setJustInTimeCompilation(jitCompilation);
            }
            executable = null;
            if (packagesToLoad.size() > 0) {
                compilePackages(compiler);
            }
            Source source = resolveFileToSource(cwd, filename);

            compiler.setErrorListener(errorListener);
            compiler.setSchemaAware(schemaAware);
            executable = compiler.compile(source);
            return executable;
        } catch (SaxonApiException ex) {
            SaxonCException ex2 = new SaxonCException(ex);
            saxonExceptions.add(ex2);
            throw ex;
        }
    }

    /**
     * Compile the stylesheet from string  for use later
     *
     * @param cwd - current working directory
     * @param str - stylesheet as a string
     * @return XsltExecutable
     */
    public XsltExecutable createStylesheetFromString(String cwd, String str, String[] sparams, Object[] values) throws SaxonApiException {
        clearExceptions();
        XsltCompiler compiler = processor.newXsltCompiler();
        if (jitCompilation) {
            compiler.setJustInTimeCompilation(jitCompilation);
        }
        Source source;
        executable = null;

        StringReader reader = new StringReader(str);
        compiler.setSchemaAware(schemaAware);
        source = new StreamSource(reader);
       /* if (cwd != null && cwd.length() > 0) {
            if (!cwd.endsWith("/")) {
                cwd = cwd.concat("/");
            }
            source.setSystemId(cwd + "file");

        }    */

        compiler.setErrorListener(errorListener);
        try {
            if (packagesToLoad.size() > 0) {
                compilePackages(compiler);
            }
            executable = compiler.compile(source);
            return executable;
        } catch (SaxonApiException ex) {
            SaxonCException ex2 = new SaxonCException(ex);
            saxonExceptions.add(ex2);
            throw ex2;
        }

    }

    /**
     * Compile the stylesheet from string  for use later
     *
     * @param cwd     - current working directory
     * @param obj     - stylesheet as a XdmNode object
     * @param sparams - Supplied static parameters
     * @param values
     * @return XsltExecutable
     */
    public XsltExecutable createStylesheetFromXdmNode(String cwd, Object obj, String[] sparams, Object[] values) throws SaxonApiException {
        clearExceptions();
        executable = null;

        XsltCompiler compiler = processor.newXsltCompiler();
        setStaticParametersFromArray(compiler, sparams, values);
        if (jitCompilation) {
            compiler.setJustInTimeCompilation(jitCompilation);
        }

        XdmNode node;
        if (obj instanceof XdmNode) {
            node = (XdmNode) obj;
        } else {
            SaxonCException ex = new SaxonCException("Failed to create Stylesheet from XdoNode");
            saxonExceptions.add(ex);
            throw ex;
        }

        compiler.setErrorListener(errorListener);
        try {
            if (packagesToLoad.size() > 0) {
                compilePackages(compiler);
            }
            executable = compiler.compile(node.asSource());
            return executable;
        } catch (SaxonApiException ex) {
            SaxonCException ex2;
            if (ex.getErrorCode() == null) {
                ex2 = new SaxonCException(new XPathException(ex.getMessage(), ""));
            } else {
                ex2 = new SaxonCException(ex);
            }

            saxonExceptions.add(ex2);
            throw ex;
        }

    }

    private void setStaticParametersFromArray(XsltCompiler compiler, String[] sparams, Object[] values) {
        if (sparams != null && values != null) {
            for (int i = 0; i < sparams.length; i++) {
                if (sparams[i].startsWith("sparam:")) {
                    //static parameters
                    String paramName = sparams[i].substring(7);
                    Object value = values[i];
                    XdmValue valueForCpp = null;
                    QName qname = QName.fromClarkName(paramName);
                    valueForCpp = convertObjectToXdmValue(values[i]);
                    compiler.setParameter(qname, valueForCpp);

                }
            }


        }
    }



    private Xslt30Transformer getXslt30Transformer(String cwd, String stylesheet) throws SaxonApiException {
        Xslt30Transformer transformer = null;
        if (stylesheet == null && executable != null) {
            transformer = executable.load30();
        } else {
            if (stylesheet == null) {
                throw new SaxonApiException("Stylesheet file is null");
            }
            XsltCompiler compiler = processor.newXsltCompiler();
            if (jitCompilation) {
                compiler.setJustInTimeCompilation(jitCompilation);
            }
            source = resolveFileToSource(cwd, stylesheet);
            compiler.setErrorListener(errorListener);

            compiler.setSchemaAware(schemaAware);

            try {
                if (packagesToLoad.size() > 0) {
                    compilePackages(compiler);
                }
                if (!staticParameters.isEmpty()) {
                    for (Map.Entry<QName, XdmValue> entry : staticParameters.entrySet()) {
                        compiler.setParameter(entry.getKey(), entry.getValue());
                    }
                }
                transformer = compiler.compile(source).load30();
            } catch (SaxonApiException ex) {
                SaxonCException ex2 = new SaxonCException(ex);
                saxonExceptions.add(ex2);
                throw ex;
            }
        }
        return transformer;
    }


    public XdmValue applyTemplateToValue(String cwd, Object sourceObj, String stylesheet, String[] params, Object[] values) throws SaxonApiException {

        Map<String, Object> paramsMap = convertArraysToMap(params, values);
        Xslt30Transformer transformer = getXslt30Transformer(cwd, stylesheet);

        applyXsltTransformerProperties(this, cwd, processor, transformer, paramsMap);
        Destination xdmResult;
        if(returnXdmValue) {
            xdmResult = new RawDestination();
        }else {
            xdmResult = new XdmDestination();
        }
        if (sourceObj instanceof String) {
            source = resolveFileToSource(cwd, (String) sourceObj);
            if (paramsMap.containsKey("dtd")) {
                AugmentedSource asource = AugmentedSource.makeAugmentedSource(source);
                String option = (String) paramsMap.get("dtd");
                if (!option.isEmpty()) {
                    asource.setDTDValidationMode(Validation.getCode(option));
                }
                source = asource;
            }

            transformer.applyTemplates(source, xdmResult);
        } else if (sourceObj instanceof XdmValue) {
            XdmValue selection = (XdmValue) sourceObj;

            transformer.applyTemplates(selection, xdmResult);
        }


        return getXdmValue(xdmResult);
    }

    private XdmValue getXdmValue(Destination xdmResult) {
        if(returnXdmValue) {
            return ((RawDestination)xdmResult).getXdmValue();
        } else {
            return ((XdmDestination)xdmResult).getXdmNode();
        }
    }

    public void applyTemplateToFile(String cwd, Object sourceObj, String stylesheet, String outFilename, String[] params, Object[] values) throws SaxonApiException {

        if (outFilename != null) {
            serializer = resolveOutputFile(processor, cwd, outFilename);
        }
        Map<String, Object> paramsMap = convertArraysToMap(params, values);
        Xslt30Transformer transformer = getXslt30Transformer(cwd, stylesheet);

        applyXsltTransformerProperties(this, cwd, processor, transformer, paramsMap);

        if (sourceObj instanceof String) {

            source = resolveFileToSource(cwd, (String) sourceObj);
            if (paramsMap.containsKey("dtd")) {
                AugmentedSource asource = AugmentedSource.makeAugmentedSource(source);
                String option = (String) paramsMap.get("dtd");
                if (!option.isEmpty()) {
                    asource.setDTDValidationMode(Validation.getCode(option));
                }
                source = asource;
            }
            transformer.applyTemplates(source, serializer);
        } else if (sourceObj instanceof XdmValue) {
            XdmValue selection = (XdmValue) sourceObj;
            transformer.applyTemplates(selection, serializer);
        }


    }

    public String applyTemplateToString(String cwd, Object sourceObj, String stylesheet, String[] params, Object[] values) throws SaxonApiException {

        Map<String, Object> paramsMap = convertArraysToMap(params, values);
        Xslt30Transformer transformer = getXslt30Transformer(cwd, stylesheet);
        StringWriter sw = new StringWriter();
        serializer = processor.newSerializer(sw);

        applyXsltTransformerProperties(this, cwd, processor, transformer, paramsMap);


        if (sourceObj instanceof String) {
            source = resolveFileToSource(cwd, (String) sourceObj);
            if (paramsMap.containsKey("dtd")) {
                AugmentedSource asource = AugmentedSource.makeAugmentedSource(source);
                String option = (String) paramsMap.get("dtd");
                if (!option.isEmpty()) {
                    asource.setDTDValidationMode(Validation.getCode(option));
                }
                source = asource;
            }
            transformer.applyTemplates(source, serializer);
        } else if (sourceObj instanceof XdmValue) {
            XdmValue selection = (XdmValue) sourceObj;
            transformer.applyTemplates(selection, serializer);
        }
        serializer = null;
        return sw.toString();
    }

    XdmValue[] getArguments(Object[] arguments) throws SaxonApiException {
        if (arguments != null && arguments.length > 0) {
            XdmValue[] values = new XdmValue[arguments.length];
            for (int i = 0; i < arguments.length; i++) {
                if (arguments[i] instanceof XdmValue) {
                    values[i] = (XdmValue) arguments[i];
                } else {
                    throw new SaxonApiException("Argument is not of type xdmValue");
                }

            }
            return values;
        }
        return new XdmValue[]{};

    }


    public void callFunctionToFile(String cwd, String stylesheet, String cFuncName, String outFilename, Objects[] arguments, String[] params, Object[] values) throws SaxonApiException {
        QName qname = QName.fromClarkName(cFuncName);
        Map<String, Object> paramsMap = convertArraysToMap(params, values);
        Xslt30Transformer transformer = getXslt30Transformer(cwd, stylesheet);
        Serializer serializer1;
        if (outFilename != null) {
            serializer1 = resolveOutputFile(processor, cwd, outFilename);
        } else {
            throw new SaxonApiException("File name for CallFunction has not been set");
        }

        applyXsltTransformerProperties(this, cwd, processor, transformer, paramsMap);

        transformer.callFunction(qname, getArguments(arguments), serializer1);

    }

    public XdmValue callFunctionToValue(String cwd, String stylesheet, String cFuncName, Object[] arguments, String[] params, Object[] values) throws SaxonApiException {
        QName qname = QName.fromClarkName(cFuncName);
        Map<String, Object> paramsMap = convertArraysToMap(params, values);
        Xslt30Transformer transformer = getXslt30Transformer(cwd, stylesheet);

        applyXsltTransformerProperties(this, cwd, processor, transformer, paramsMap);

        XdmValue result = transformer.callFunction(qname, getArguments(arguments));
        return result;
    }

    public String callFunctionToString(String cwd, String stylesheet, String cFuncName, Object[] arguments, String[] params, Object[] values) throws SaxonApiException {
        QName qname = QName.fromClarkName(cFuncName);
        Map<String, Object> paramsMap = convertArraysToMap(params, values);
        Xslt30Transformer transformer = getXslt30Transformer(cwd, stylesheet);

        StringWriter sw = new StringWriter();
        serializer = processor.newSerializer(sw);
        applyXsltTransformerProperties(this, cwd, processor, transformer, paramsMap);

        transformer.callFunction(qname, getArguments(arguments), serializer);
        serializer = null;
        return sw.toString();
    }


    public void callTemplateToFile(String cwd, String stylesheet, String templateName, String outFilename, String[] params, Object[] values) throws SaxonApiException {
        QName qname = null;
        if(templateName != null) {
            qname = QName.fromClarkName(templateName);
        }
        Map<String, Object> paramsMap = convertArraysToMap(params, values);
        Xslt30Transformer transformer = getXslt30Transformer(cwd, stylesheet);

        if (outFilename != null) {
            serializer = resolveOutputFile(processor, cwd, outFilename);
        } else {
            throw new SaxonApiException("File name for CallFunction has not been set");
        }

        applyXsltTransformerProperties(this, cwd, processor, transformer, paramsMap);

        transformer.callTemplate(qname, serializer);
    }

    public XdmValue callTemplateToValue(String cwd, Object sourceObj, String stylesheet, String clarkName, String[] params, Object[] values) throws SaxonApiException {
        QName qname = null;
        if(clarkName != null) {
            qname = QName.fromClarkName(clarkName);
        }
        Map<String, Object> paramsMap = convertArraysToMap(params, values);
        Xslt30Transformer transformer = getXslt30Transformer(cwd, stylesheet);

        setsource(cwd, transformer, sourceObj, paramsMap);
        applyXsltTransformerProperties(this, cwd, processor, transformer, paramsMap);
        Destination xdmResult;
        if(returnXdmValue) {
            xdmResult = new RawDestination();
        }else {
            xdmResult = new XdmDestination();
        }

        transformer.callTemplate(qname, xdmResult);

        return getXdmValue(xdmResult);
    }

    private void setsource(String cwd, Xslt30Transformer transformer, Object sourceObj, Map<String, Object> paramsMap) throws SaxonApiException {
        if (sourceObj != null) {
            if (sourceObj instanceof String) {
                XdmNode node;
                DocumentBuilder builder = processor.newDocumentBuilder();
                source = resolveFileToSource(cwd, (String) sourceObj);
                if (paramsMap.containsKey("dtd")) {
                    AugmentedSource asource = AugmentedSource.makeAugmentedSource(source);
                    String option = (String) paramsMap.get("dtd");
                    if (!option.isEmpty()) {
                        asource.setDTDValidationMode(Validation.getCode(option));
                    }
                    source = asource;
                    node = builder.build(source);
                    transformer.setGlobalContextItem(node);
                }

            } else if (sourceObj instanceof XdmItem) {
                XdmItem node = (XdmItem) sourceObj;
                transformer.setGlobalContextItem(node);
            }
        }

    }

    public String callTemplateToString(String cwd, Object sourceObj, String stylesheet, String clarkName, String[] params, Object[] values) throws SaxonApiException {
        QName qname = null;
        if(clarkName != null) {
            qname = QName.fromClarkName(clarkName);
        }
        Map<String, Object> paramsMap = convertArraysToMap(params, values);
        Xslt30Transformer transformer = getXslt30Transformer(cwd, stylesheet);
        StringWriter sw = new StringWriter();
        serializer = processor.newSerializer(sw);

        setsource(cwd, transformer, sourceObj, paramsMap);

        applyXsltTransformerProperties(this, cwd, processor, transformer, paramsMap);


        transformer.callTemplate(qname, serializer);
        return sw.toString();
    }

    /**
     * Do transformation. Save result to file
     * This method is designed to be used with the createStylesheet[File/String] methods above,
     * therefore executable can be loaded in a previous step.
     * However this method can be used as a one-shot method to go
     * through the stages of compilation, loading any source documents and execution.
     * Here we supply parameters and properties required to do the transformation.
     * The parameter names and values are supplied as a two arrays in the form of a key and value.
     *
     * @param cwd            - current working directory
     * @param sourceFilename - source supplied as a file name
     * @param stylesheet     - File name of the stylesheet
     * @param outFilename    - Save result of transformation to the given file name
     * @param params         - parameters and property names given as an array of stings
     * @param values         - the values of the paramaters and properties. given as a array of Java objects
     */
    public void transformToFile(String cwd, String sourceFilename, String stylesheet, String outFilename, String[] params, Object[] values) throws SaxonApiException {
        try {
            clearExceptions();
            serializer = null;
            Source source;
            Map<String, Object> paramsMap = convertArraysToMap(params, values);
            Xslt30Transformer transformer = getXslt30Transformer(cwd, stylesheet);

            if (outFilename != null) {
                serializer = resolveOutputFile(processor, cwd, outFilename);
            } else if (paramsMap.containsKey("o")) {
                String outfile = (String) paramsMap.get("o");
                serializer = resolveOutputFile(processor, cwd, outfile);
            }

            applyXsltTransformerProperties(this, cwd, processor, transformer, paramsMap);

            if (serializer == null) {
                SaxonCException ex = new SaxonCException("Output file not set for this transformation");
            }
            if (sourceFilename == null && doc != null) {
                transformer.setGlobalContextItem(doc);
            } else if (sourceFilename != null) {
                source = resolveFileToSource(cwd, sourceFilename);

                transformer.transform(source, serializer);
            }


            serializer = null;

        } catch (SaxonApiException e) {
            SaxonCException ex = new SaxonCException(e);
            saxonExceptions.add(ex);
            throw e;
        } catch (NullPointerException ex) {
            SaxonApiException ex2 = new SaxonApiException(ex);
            saxonExceptions.add(new SaxonCException(ex2));
            throw ex2;
        } catch (Exception ex) {
            SaxonCException ex2 = new SaxonCException(ex);
            saxonExceptions.add(ex2);
            throw ex2;
        }
    }


    /**
     * Applies the properties and parameters required in the transformation.
     * In addition we can supply the source, stylesheet and output file names.
     * We can also supply values to xsl:param and xsl:variables required in the stylesheet.
     * The parameter names and values are supplied as a two arrays in the form of a key and value.
     *
     * @param cwd         - current working directory
     * @param processor   - required to use the same processor as for the compiled stylesheet
     * @param transformer - pass the current object to set local variables supplied in the parameters
     * @param map         - parameters and property names given as string keys. Their Values given as a array of Java objects
     */
    public static void applyXsltTransformerProperties(SaxonCAPI api, String cwd, Processor processor, Xslt30Transformer transformer, Map<String, Object> map) throws SaxonApiException {
        boolean tunnel = false;
        if (!map.isEmpty()) {
            String initialMode;
            XdmItem item;
            String outfile = null;
            Source source;
            Object valuei = null;
            DocumentBuilder builder = processor.newDocumentBuilder();

            if (cwd != null && cwd.length() > 0) {
                if (!cwd.endsWith("/")) {
                    cwd = cwd.concat("/");
                }
            }

            if (map.containsKey("xsltversion")) {
                processor.setConfigurationProperty(FeatureKeys.XSLT_VERSION, map.get("xsltversion"));

            }

            if (map.containsKey("extc")) {
                //extension function library path
                String libName = (String) map.get("extc");
                SaxonCAPI.setLibrary("", libName);


            }
            if (map.containsKey("im")) {
                valuei = map.get("im");
                if (valuei instanceof String) {
                    initialMode = (String) valuei;
                    transformer.setInitialMode(new QName(initialMode));
                } else if (debug) {
                    System.err.println("DEBUG: value error for property 'im'");
                }
            }
            if (map.containsKey("s")) {
                valuei = map.get("s");
                if (valuei instanceof String) {
                    source = api.resolveFileToSource(cwd, (String) valuei);
                    transformer.setGlobalContextItem(builder.build(source));
                } else if (debug) {
                    System.err.println("DEBUG: value error for property 's'");
                }
            }
            if (map.containsKey("item")) {


                valuei = map.get("item");
                if (valuei instanceof XdmItem) {
                    if (debug && valuei != null) {
                        System.err.println("DEBUG: Type of value=" + (valuei).getClass().getName());

                    }
                }
                item = (XdmItem) valuei;
                transformer.setGlobalContextItem(item);

            } else if (map.containsKey("node")) {


                valuei = map.get("node");
                if (valuei instanceof XdmItem) {
                    if (debug && valuei != null) {
                        System.err.println("DEBUG: Type of value=" + (valuei).getClass().getName());

                    }
                }
                item = (XdmItem) valuei;
                transformer.setGlobalContextItem(item);

            } else if (map.containsKey("param:node")) {


                valuei = map.get("param:node");
                if (valuei instanceof XdmItem) {
                    if (debug && valuei != null) {
                        System.err.println("DEBUG: Type of value=" + (valuei).getClass().getName());

                    }
                }
                item = (XdmItem) valuei;
                transformer.setGlobalContextItem(item);

            }

            if (map.containsKey("m")) {
                transformer.setMessageListener(((Xslt30Processor) api).newMessageListener());

            }

            if (map.containsKey("resources")) {
                valuei = map.get("resources");
                char separatorChar = '/';
                if (SaxonCAPI.RESOURCES_DIR == null && valuei instanceof String) {
                    String dir1 = (String) valuei;
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

            }

            if(map.containsKey("tunnel")) {
                valuei = map.get("tunnel");
                if(valuei instanceof Boolean){
                    tunnel = (Boolean)valuei;
                }
            }

            if(map.containsKey("outvalue")){
                //Set if the return type of callTemplate, applyTemplate and transform methods is to return XdmValue,
                //otherwise return XdmNode object with root Document node
                valuei = map.get("outvalue");
                if(valuei instanceof Boolean){
                    api.returnXdmValue = (boolean) valuei;
                }

            }


        }
        if (!api.props.isEmpty() && api.serializer != null) {
            api.serializer.setOutputProperties(api.props);
        }

        if(!api.initialTemplateParameters.isEmpty()) {
            transformer.setInitialTemplateParameters(api.initialTemplateParameters, tunnel);
        }

        transformer.setStylesheetParameters(api.getGlobals());

    }


    /**
     * Do transformation and return result as an Xdm node in memory
     * This method is designed to be used with the createStylesheet[File/String] methods above,
     * therefore executable can be loaded in a previous step.
     * However this method can be used as a one-shot method to go
     * through the stages of compilation, loading any source documents and execution.
     * Here we supply parameters and properties required to do the transformation.
     * The parameter names and values are supplied as a two arrays in the form of a key and value.
     *
     * @param cwd        - current working directory
     * @param sourceFile - source supplied as a file name
     * @param stylesheet - File name of the stylesheet
     * @param params     - parameters and property names given as an array of stings
     * @param values     - the values of the paramaters and properties. given as a array of Java objects
     * @return result as an XdmNode
     */
    public XdmValue transformToValue(String cwd, String sourceFile, String stylesheet, String[] params, Object[] values) throws SaxonApiException {
        Source source;
        clearExceptions();


        try {
            Map<String, Object> paramsMap = convertArraysToMap(params, values);
            Xslt30Transformer transformer = getXslt30Transformer(cwd, stylesheet);


            this.applyXsltTransformerProperties(this, cwd, processor, transformer, paramsMap);
            Destination destination;
            if(returnXdmValue) {
                destination = new RawDestination();
            }else {
                destination = new XdmDestination();
            }


            if (sourceFile == null && doc != null) {
                transformer.transform(doc.asSource(), destination);
            } else if (sourceFile != null) {
                source = resolveFileToSource(cwd, sourceFile);

                transformer.transform(source, destination);

            } else {
                throw new SaxonApiException("Source not set in transform method");
            }

            return getXdmValue(destination);
        } catch (SaxonApiException e) {
            SaxonCException saxonException = new SaxonCException(e);
            saxonExceptions.add(saxonException);
            throw e;
        } catch (Exception ex) {
            throw new SaxonCException(ex);
        }

    }


    /**
     * Do transformation. The result is serialized as string representation in memory
     * This method is designed to be used with the createStylesheet[File/String] methods above,
     * therefore executable can be loaded in a previous step.
     * However this method can be used as a one-shot method to go
     * through the stages of compilation, loading any source documents and execution.
     * Here we supply parameters and properties required to do the transformation.
     * The parameter names and values are supplied as a two arrays in the form of a key and value.
     *
     * @param cwd        - current working directory
     * @param sourceFile - source supplied as a file name
     * @param stylesheet - File name of the stylesheet
     * @param params     - parameters and property names given as an array of stings
     * @param values     - the values of the paramaters and properties. given as a array of Java objects
     * @return result as a string representation
     */
    public String transformToString(String cwd, String sourceFile, String stylesheet, String[] params, Object[] values) throws SaxonApiException {
        if (debug) {
            System.err.println("xsltApplyStylesheet, Processor: " + System.identityHashCode(processor));
        }
        Map<String, Object> paramsMap = convertArraysToMap(params, values);
        try {
            Source source = null;
            clearExceptions();
            Xslt30Transformer transformer = getXslt30Transformer(cwd, stylesheet);

            StringWriter sw = new StringWriter();
            serializer = processor.newSerializer(sw);


            applyXsltTransformerProperties(this, cwd, processor, transformer, paramsMap);

            if (sourceFile == null && doc != null) {
                source = doc.asSource();
            } else if (sourceFile != null) {
                source = resolveFileToSource(cwd, sourceFile);
            }
            transformer.setErrorListener(errorListener);
            transformer.transform(source, serializer);
            serializer = null;
            return sw.toString();
        } catch (SaxonApiException e) {
            SaxonCException saxonException = new SaxonCException(e);
            saxonExceptions.add(saxonException);
            throw e;
        } catch (Exception e) {
            SaxonCException ex = new SaxonCException(e);
            saxonExceptions.add(ex);
            throw ex;
        }
    }


    /***
     * Compile a library package and link it for use.
     * <p>The source argument identifies an XML file containing an &lt;xsl:package&gt; element. Any packages
     * on which this package depends must have been made available to the <code>XsltCompiler</code>
     * by importing them.</p>
     */
    public void compilePackages(XsltCompiler compiler) throws SaxonApiException {
        try {
            PackageLibrary library = new PackageLibrary(compiler.getUnderlyingCompilerInfo(), packagesToLoad);
        } catch (XPathException e) {
            throw new SaxonApiException(e);
        }


    }


    /***
     * File names to XsltPackages stored on filestore are added to a set of packages, which
     * will be imported later when compiling
     * @param cwd current working directory
     * @param packs array of file names of XSLT packages stored in filestore
     */
    public void addPackages(String cwd, String[] packs) {
        File filei;
        for (int i = 0; i < packs.length; i++) {
            try {
                filei = resolveFile(cwd, packs[i]);
            } catch (SaxonApiException e) {
                System.err.println("Java: Failure in adding packages " + e.getMessage());
                continue;
            }
            packagesToLoad.add(filei);
        }
    }


    /***
     * Clear set of packages.
     */
    public void clearPackages() {
        packagesToLoad.clear();

    }


    public static void main(String[] args) throws Exception {
        String cwd2 = "/Users/ond1/work/development/svn/archive/opensource/latest9.9/hec/samples/php/trax";
        String cwd = "/Users/ond1/work/development/tests/jeroen";
        // String cwd = "C:///www///html///trax";
        //String cwd = "http://localhost/trax";
        /*if (args.length > 0) {
            cwd = args[0];
        }      */

        String sourcefile1 = "/Users/ond1/work/development/tests/jeroen/xml/kamervragen.xml";//"sinsello.xml";//"saxon_php3/xmark64.xml";
        String stylesheet12 = "xslt/overzicht-resultaten.xslt";//"cadenaoriginal_3_2.xslt";//""saxon_php3/q8.xsl";//"test.xsl";
        String outfile = "outfile.html";
        Processor processor = new Processor(true);
        Xslt30Processor cpp = new Xslt30Processor(processor);
        //cpp.createStylesheetFromFile(cwd2, "xsl/foo.xsl");
        cpp.compileFromFileAndSave(cwd2, "xsl/foo.xsl", "xsl/foo.xslp");
        String resultStr = cpp.transformToString(cwd2, "xml/foo.xml", "xsl/foo.xslp", null, null);
        System.out.println(resultStr);

        try {
            String resultStr4 = cpp.transformToString(cwd2, "xml/foo.xml", "xsl/fooExFunc.xsl", null, null);
            System.out.println("Result String 4 = " + resultStr4);
        } catch (SaxonApiException ex) {
            System.out.println("Error in transformation Message: " + ex.getMessage());
        }


        XdmValue resultV = cpp.callFunctionToValue(cwd2, "xsl/fooExFunc.xsl", "{http://www.saxonica.com/myfunction}is-licensed-EE", null, null, null);
        String resultCF = cpp.callFunctionToString(cwd2, "xsl/fooExFunc.xsl", "{http://www.saxonica.com/myfunction}is-licensed-EE", null, new String[]{"!omit-xml-declaration"}, new Object[]{"yes"});

        System.out.println("Running callFunction is-licensed-EE= " + resultV.toString());
        System.out.println("Running callFunction is-licensed-EE= " + resultCF);

        String[] params0 = {"it"};
        Object[] values0 = {null};
        String resultStr3 = cpp.transformToString(cwd2, null, "xsl/foo4.xsl", params0, values0);
        System.out.println("Using initial-template: " + resultStr3);

        XdmNode node2 = cpp.parseXmlFile("/Users/ond1/work/development/campos", "ORP0301177AA__EE__30954_sinsello.xml");
        String[] paramsx = {"node"};
        Object[] valuesx = {node2};
        String result2 = cpp.transformToString("/Users/ond1/work/development/campos", "ORP0301177AA__EE__30954_sinsello.xml", "campos.xsl", paramsx, valuesx);
        Object[] arrValues = {2, "test"};

        String[] params1 = {"resources", "param:test1", "node", "m", "xmlversion"};
        Object[] values1 = {"/Users/ond1/work/development/tests/jeroen/data", arrValues, node2, "m", "1.1"};
        String outputdoc = cpp.transformToString(cwd, null, stylesheet12, params1, values1);
        // System.out.println(outputdoc);
        // System.exit(0);
        // Processor processor = cpp.getProcessor();
        // XsltTransformer transformer = cpp.xsltParseStylesheetFile(args[0]).load();
        //XdmNode sourceNode = cpp.xmlParseFile(cwd, "xml/foo.xml");
        XdmNode sourceNode2 = SaxonCAPI.parseXmlString(processor, null, "<result><assert-xml file=\"type-0501.out\"/></result>");
        XdmValue node1 = (XdmValue) cpp.createXdmAtomicItem("string", "textXXXXX");

        XdmValue resultNode2 = cpp.parseXmlString("<?xml version=\"1.0\" encoding=\"UTF-8\"?><html><head><title>Untitled</title></head><body leftmargin=\"100\"></body></html>");

        XdmValue value1 = SaxonCAPI.createXdmAtomicItem("string", "good bye");
        String[] params2 = {"o"};
        Object[] values2 = {"output_test.xml"};
        String[] params3 = {"node", "!indent", "output_test.xml", "xmlversion"};
        String[] param4 = {"s", "param:a-param"};
        Object[] values4 = {"xml/foo.xml", value1};
        Object[] values3 = {sourceNode2, "yes", "o", "1.0"};
        cpp.createStylesheetFromFile(cwd, stylesheet12, null, null);


        String result = "";
        int repeat = 1;
        try {
            cpp.createStylesheetFromString("samples", "<xsl:stylesheet xmlns:xsl=\"http://www.w3.org/1999/XSL/Transform\"\n" +
                    "    version=\"2.0\" xmlns:pf=\"http://example.com\">\n" +
                    "<xsl:param name=\"pf:param-name\"  />" +
                    "<xsl:param name=\"test1\"  />" +

                    "    \n" +
                    "    \n" +
                    "    <xsl:template match=\"/\" >\n" +
                    "   <xsl:message>test messages</xsl:message>" +
                    "        <xsl:copy-of select=\".\"/>\n" +
                    "       XXXXXX <xsl:value-of select=\"$pf:param-name\"/>\n" +
                    "    </xsl:template>\n" +
                    "    \n" +
                    "   \n" +
                    "</xsl:stylesheet>", null, null);


            String valueStr = cpp.transformToString(cwd, "categories.xml", null, null, null);
            if (valueStr != null) {
                System.out.println("Output = " + valueStr);
            } else {
                System.out.println("valueSt is null");
            }

        } catch (SaxonCException ex) {
            System.out.println("2. Error message=" + ex.getMessage());
        }

        try {

            String resultStr2 = cpp.transformToString(cwd2, null, null, param4, values4);

           /* cpp.transformToFile(cwd, "categories.xml", stylesheet12, "outputTest.txt", null, null);
            long startTime = System.currentTimeMillis();
            for (int i = 0; i < repeat; i++) {
                //result = cpp.xsltApplyStylesheet(cwd, null, "xsl/foo.xsl", params3, values3);
                result = cpp.transformToString(cwd, null, null, params1, values1);

            }
            long endTime = System.currentTimeMillis();
            //  System.out.println("output:" + result + " Time:" + ((endTime - startTime) / 5));

            System.out.println("output:" + result2); */
        } catch (Exception ex) {
            System.out.println("Error found=" + ex.getMessage());
        }
        SaxonCException[] exceptionForCpps = cpp.getExceptions();

        System.out.println("xslMessage output:" + cpp.getXslMessages().length);

    }


}
