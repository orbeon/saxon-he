package net.sf.saxon.option.cpp;

import com.saxonica.functions.extfn.cpp.CPPFunctionSet;
import com.saxonica.functions.extfn.cpp.PHPFunctionSet;
import net.sf.saxon.Configuration;
import net.sf.saxon.s9api.*;

import javax.xml.transform.Source;
import java.io.File;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.Map;

/**
 * * This class is to use with Saxon/C on C++
 */
public class XPathProcessor extends SaxonCAPI {

    private XPathCompiler compiler = null;
    private XPathSelector selector = null;
    private XdmItem contextItem = null;

    public XPathProcessor() {
        super();
        compiler = processor.newXPathCompiler();
        compiler.setAllowUndeclaredVariables(true);
    }

    public XPathProcessor(boolean l) {
        super(l);
        compiler = processor.newXPathCompiler();
        compiler.setAllowUndeclaredVariables(true);
        Configuration config = processor.getUnderlyingConfiguration();
//#if EE==true || PE==true
        if(config.isLicensedFeature(Configuration.LicenseFeature.PROFESSIONAL_EDITION)) {
            config.getBuiltInExtensionLibraryList().addFunctionLibrary(PHPFunctionSet.getInstance());
            config.getBuiltInExtensionLibraryList().addFunctionLibrary(CPPFunctionSet.getInstance());
        }
//#endif
    }

    public XPathProcessor(Processor proc) {
        super(proc);
        compiler = processor.newXPathCompiler();
        compiler.setAllowUndeclaredVariables(true);
        Configuration config = processor.getUnderlyingConfiguration();
//#if EE==true || PE==true
        if(config.isLicensedFeature(Configuration.LicenseFeature.PROFESSIONAL_EDITION)) {
            config.getBuiltInExtensionLibraryList().addFunctionLibrary(PHPFunctionSet.getInstance());
            config.getBuiltInExtensionLibraryList().addFunctionLibrary(CPPFunctionSet.getInstance());
        }
//#endif
    }

    /**
     * Get the XPath Compiler created for this XPath Processor
     * @return XPathCompiler
     */
    public XPathCompiler getCompiler(){
        return compiler;
    }

    /**
     * Declare a namespace binding as part of the static context for XPath expressions compiled using this
     * XPathCompiler
     *
     * @param prefix The namespace prefix. If the value is a zero-length string, this method sets the default
     *               namespace for elements and types.
     * @param uri    The namespace URI. It is possible to specify a zero-length string to "undeclare" a namespace;
     *               in this case the prefix will not be available for use, except in the case where the prefix
     *               is also a zero length string, in which case the absence of a prefix implies that the name
     *               is in no namespace.
     * @throws NullPointerException if either the prefix or uri is null.
     */
    public void declareNamespace(String prefix, String uri) {
        compiler.declareNamespace(prefix, uri);
    }


    /* TODO public void declareVariable(String qname, ItemType itemType, OccurrenceIndicator occurrences) {
        compiler.declareVariable(QName.fromClarkName(qname));
    } */

    /**
     * Set whether XPath 1.0 backwards compatibility mode is to be used. In backwards compatibility
     * mode, more implicit type conversions are allowed in XPath expressions, for example it
     * is possible to compare a number with a string. The default is false (backwards compatibility
     * mode is off).
     *
     * @param option true if XPath 1.0 backwards compatibility is to be enabled, false if it is to
     *               be disabled.
     */

    public void setBackwardsCompatible(boolean option) {
        compiler.setBackwardsCompatible(option);
    }

    /**
     * Set the static base URI for XPath expressions compiled using this XPathCompiler. The base URI
     * is part of the static context, and is used to resolve any relative URIs appearing within an XPath
     * expression, for example a relative URI passed as an argument to the doc() function. If no
     * static base URI is supplied, then the current working directory is used.
     *
     * @param uriStr
     * @throws SaxonApiException
     */
    public void setBaseURI(String uriStr) throws SaxonApiException {
        URI uri = null;
        try {
            uri = new URI(uriStr);
            compiler.setBaseURI(uri);
        } catch (URISyntaxException e) {
            SaxonApiException ex = new SaxonApiException(e);
            throw ex;
        }

    }


    public void setContextItem(XdmItem item) throws SaxonApiException {
        this.contextItem = item;
    }

    public void setProperties(String[] params, Object[] values) throws SaxonApiException {
        if (selector != null) {
            try {
                Map<QName, XdmValue> parameters = new HashMap<>();
                Map<String, Object> map = setupConfigurationAndBuildMap(params, values, null, parameters, null, null, null, false);
                applyXPathProperties(this, "", processor, selector, map, parameters);
            } catch (SaxonApiException e) {
                throw e;
            }
        } else {
            SaxonApiException ex = new SaxonApiException("XPathExecutable not created");
            throw ex;

        }

    }

    public void reset() {
        compiler = null;
        selector = null;
    }


    /**
     * Compile and evaluate an XPath expression, supplied as a character string, with properties and parameters required
     * by the XPath expression
     *
     * @param cwd      - Current working directory
     * @param xpathStr - A string containing the source text of the XPath expression
     * @param params   - Parameters and properties names required by the XPath expression. This could contain the context node , source as string or file name, etc
     * @param values   -  The values for the parameters and properties required by the XPath expression
     **/
    public XdmValue[] evaluate(String cwd, String xpathStr, String[] params, Object[] values) throws SaxonApiException {

        if (debug) {
            if (xpathStr != null) {
                System.err.println("xpathString: " + xpathStr);
            }
        }
        Map<QName, XdmValue> parameters = new HashMap<>();
        Map<String, Object> optionsMap = setupConfigurationAndBuildMap(params, values, null, parameters, null, null, null, false);
        setupXPathCompiler(optionsMap);
        compiler.setSchemaAware(schemaAware);

        selector = compiler.compile(xpathStr).load();
        applyXPathProperties(this, cwd, processor, selector, optionsMap, parameters);
        if (contextItem != null) {
            selector.setContextItem(contextItem);
        }
        XdmValue value = selector.evaluate();//compiler.evaluate(xpathStr, contextItem);
        if (value.size() == 0) {
            return null;
        }
        XdmValue[] xdmValues = new XdmValue[value.size()];
        int i = 0;
        for (XdmItem item : value) {
            xdmValues[i] = item;
            i++;
        }
        return xdmValues;

    }

    private void setupXPathCompiler(Map<String, Object> parameterMap) {
        Object valuei = null;
        if(parameterMap.containsKey("caching:")){
            valuei = parameterMap.get("caching");
            if(valuei instanceof Boolean) {
                compiler.setCaching((Boolean)valuei);
            } if (valuei instanceof String && (valuei.equals("yes") || valuei.equals("true"))) {
                compiler.setCaching(true);
            }

        }

        /*if(parameterMap.containsKey("allowUVar:")){
            compiler.setAllowUndeclaredVariables(true);
        }  */

        if(parameterMap.containsKey("importSN:")){
            valuei = parameterMap.get("importSN:");
            if(valuei != null && valuei instanceof String) {
                compiler.importSchemaNamespace((String)valuei);

            }

        }

        if(parameterMap.containsKey("backwardsCom:")) {
            compiler.setBackwardsCompatible(true);
        }

        /*if(!variables.isEmpty()) {
            for(QName name : variables) {
                compiler.declareVariable(name);
            }
        } */
    }


    /**
     * Compile and evaluate an XPath expression whose result is expected to be
     * a single item, with a given context item. The expression is supplied as
     * a character string.
     *
     * @param cwd      - Current working directory
     * @param xpathStr - A string containing the source text of the XPath expression
     * @param params   - Parameters and properties names required by the XPath expression. This could contain the context node , source as string or file name, etc
     * @param values   -  The values for the parameters and properties required by the XPath expression
     **/
    public XdmItem evaluateSingle(String cwd, String xpathStr, String[] params, Object[] values) throws SaxonApiException {
        if (debug) {
            if (xpathStr != null) {
                System.err.println("xpathString: " + xpathStr);
            }
        }
        Map<QName, XdmValue> parameters = new HashMap<>();
        Map<String, Object> optionsMap = setupConfigurationAndBuildMap(params, values, null, parameters, null, null, null, false);
        setupXPathCompiler(optionsMap);
        selector = compiler.compile(xpathStr).load();
        applyXPathProperties(this, cwd, processor, selector, optionsMap, parameters);
        if (contextItem != null) {
            selector.setContextItem(contextItem);
        }

        return selector.evaluateSingle();// compiler.evaluateSingle(xpathStr, contextItem);

    }

    /**
     * Evaluate the XPath expression, returning the effective boolean value of the result.
     *
     * @param cwd      - Current working directory
     * @param xpathStr - A string containing the source text of the XPath expression
     * @param params   - Parameters and properties names required by the XPath expression. This could contain the context node , source as string or file name, etc
     * @param values   -  The values for the parameters and properties required by the XPath expression
     **/
    public boolean effectiveBooleanValue(String cwd, String xpathStr, String[] params, Object[] values) throws SaxonApiException {

        Map<QName, XdmValue> parameters = new HashMap<>();
        Map<String, Object> optionsMap = setupConfigurationAndBuildMap(params, values, null, parameters, null, null, null, false);
        setupXPathCompiler(optionsMap);
        selector = compiler.compile(xpathStr).load();

        try {

            applyXPathProperties(this, cwd, processor, selector, optionsMap, parameters);
        } catch (SaxonApiException e) {
            throw e;
        }

        if (contextItem != null) {
            selector.setContextItem(contextItem);
        }
        boolean result;
        try {
            result = selector.effectiveBooleanValue();
        } catch (SaxonApiException e) {
            throw e;
        }
        return result;


    }


    /**
     * Applies the properties and parameters required in the transformation.
     * In addition we can supply the source, stylesheet and output file names.
     * We can also supply values to xsl:param and xsl:variables required in the stylesheet.
     * The parameter names and values are supplied as a two arrays in the form of a key and value.
     *  @param cwd       - current working directory
     * @param processor - required to use the same processor as for the compiled stylesheet
     * @param selector  - compiled and loaded XPath expression ready for execution.
     * @param map
     */
    public static void applyXPathProperties(SaxonCAPI api, String cwd, Processor processor, XPathSelector selector, Map<String, Object> map, Map<QName, XdmValue> parameters) throws SaxonApiException {
        if (!map.isEmpty()) {
            String outputFilename = null;
            String initialTemplate = null;
            String initialMode = null;
            XdmItem item = null;
            String outfile = null;
            Source source = null;
            DocumentBuilder builder = processor.newDocumentBuilder();
            Object valuei = null;

            if (cwd != null && cwd.length() > 0) {
                if (!cwd.endsWith("/")) {
                    cwd = cwd.concat("/");
                }
            }

            if (map.containsKey("s")) {
                valuei = map.get("s");
                if (!(valuei instanceof String)) {
                    throw new SaxonApiException("Source file has incorrect type");
                }
                source = api.resolveFileToSource(cwd, (String) valuei);
                ((XPathProcessor) api).setContextItem(builder.build(source));
            }

            if (map.containsKey("item")) {
                valuei = map.get("item");
                if (valuei instanceof XdmItem) {
                    item = (XdmItem) valuei;
                    ((XPathProcessor) api).setContextItem(item);
                }

            } else if (map.containsKey("node")) {
                valuei = map.get("node");
                if (valuei instanceof XdmItem) {
                    if (debug && valuei != null) {
                        System.err.println("DEBUG: Type of value=" + (valuei).getClass().getName());

                    }
                }
                item = (XdmItem) valuei;
                ((XPathProcessor) api).setContextItem(item);

            }


            if (map.containsKey("resources")) {
                valuei = map.get("resources");

                char separatorChar = '/';
                if (SaxonCAPI.RESOURCES_DIR == null) {
                    String dir1 = (String) valuei;
                    if (!dir1.endsWith("/")) {
                        dir1 = dir1.concat("/");
                    }
                    if (File.separatorChar != '/') {
                        dir1.replace(separatorChar, File.separatorChar);
                        separatorChar = '\\';
                    }
                    SaxonCAPI.RESOURCES_DIR = dir1;
                }

            }

            if (map.containsKey("extc")) {
                //extension function library path
                String libName = (String) map.get("extc");
                SaxonCAPI.setLibrary("", libName);


            }
        }
         if (!parameters.isEmpty()){
             Map<QName, XdmValue> variables = parameters;
             for(Map.Entry<QName, XdmValue> entry: variables.entrySet()){
                 selector.setVariable(entry.getKey(), entry.getValue());

             }
         }


    }


    public static void main(String[] arg) throws SaxonApiException {

        int num = Integer.parseInt("123", 5);
        System.out.println("Xxxxxxx= " + num);

        XPathProcessor xpath = new XPathProcessor(false);
        System.out.println("Version: " + SaxonCAPI.getProductVersion(xpath.processor));
        String sourcefile1 = "kamervragen.xml";
        String[] params1 = {"s"};
        Object[] values1 = {sourcefile1};
        Processor p = xpath.getProcessor();
        /*DocumentBuilder b = p.newDocumentBuilder();
        XdmNode foo = b.build(new StreamSource(new StringReader("<foo><bar/></foo>")));
        xpath.setContextItem(foo);  */
        XdmNode node = xpath.parseXmlString("<out>\n" +
                "<person attr1='value1' attr2='value2' xmlns='http://example.com'>text1</person>\n" +
                "    <person>text2</person>\n" +
                "    <person1>text3</person1>\n" +
                "</out>");
        XdmNode node2 = xpath.parseXmlFile("/Users/ond1/work/development/svn/test", "books.xml");
        XdmNode[] children1 = XdmUtils.getChildren(XdmUtils.getChildren(node)[0]);
        // xpath.setContextItem(node);
        Object[] values3 = {node2};
        String[] params2 = {"node",  };
        Object[] values2 = {node};

        String[] params4 = {"param:s1"};
        Object[] values4 = {"text in var"};

        String[] params5 = {"param:s1"};
        Object[] values5 = {"10"};
        XdmValue value = xpath.evaluateSingle("/Users/ond1/work/development/tests/jeroen/xml/", "//person[1]", params2, values2);
        if (value instanceof XdmNode) {
            String nodename = XdmUtils.getEQName(((XdmNode) value).getNodeName());
            System.out.println(nodename);
            //String [] values = XdmUtils.getAttributeValues((XdmNode)value);
            String valuex1 = XdmUtils.getAttributeValue((XdmNode) value, "attr1");
            XdmNode[] children = XdmUtils.getChildren((XdmNode) value);
            XdmNode parent = children[0].getParent();
            // System.out.println(values[0]);
            System.out.println(valuex1);
            System.out.println("Parent =" + parent.getParent().getNodeName());
        }
        boolean ebv = xpath.effectiveBooleanValue("/Users/ond1/work/development/tests/jeroen/xml/", "count(/out/person)>0", params2, values2);
        // System.out.println(value.toString());
        System.out.println(ebv);
        XdmValue valuex = xpath.evaluateSingle("/Users/ond1/work/development/tests/jeroen/xml/", "//person[1]", params2, values2);
        if (valuex != null) {
            System.out.println("evalSingle = " + XdmUtils.getStringValue(valuex));
        }
        XdmValue[] value2 = xpath.evaluate("/Users/ond1/work/development/svn/test", "/BOOKLIST/BOOKS/ITEM/TITLE", params2, values3);
        if (value2 != null) {
            for (int i = 0; i < value2.length; i++) {
                System.out.println("Book Title: " + XdmUtils.getStringValue(value2[i]));
            }
        } else {
            System.out.println("Book xpath expr returned null!!!");

        }

        XdmValue[] value3 = xpath.evaluate("/Users/ond1/work/development/svn/test", "$s1", params4, values4);
        XdmValue[] value4 = xpath.evaluate("/Users/ond1/work/development/svn/test", "$s1", params5, values5);
        if(value3.length>0 && value4.length>0) {
            System.out.println("Value of parameter = "+ XdmUtils.getStringValue(value3[0]));
            System.out.println("Value of parameter = "+ XdmUtils.getStringValue(value4[0]));
        }
    }


}
