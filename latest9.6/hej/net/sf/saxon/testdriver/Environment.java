////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
// Copyright (c) 2014 Saxonica Limited.
// This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
// If a copy of the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
// This Source Code Form is “Incompatible With Secondary Licenses”, as defined by the Mozilla Public License, v. 2.0.
////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

package net.sf.saxon.testdriver;

import net.sf.saxon.Configuration;
import net.sf.saxon.expr.XPathContext;
import net.sf.saxon.expr.sort.SimpleCollation;
import net.sf.saxon.lib.*;
import net.sf.saxon.om.*;
import net.sf.saxon.s9api.*;
import net.sf.saxon.trans.DecimalFormatManager;
import net.sf.saxon.trans.DecimalSymbols;
import net.sf.saxon.trans.XPathException;
import net.sf.saxon.tree.iter.EmptyIterator;
import net.sf.saxon.tree.iter.ListIterator;
import net.sf.saxon.tree.util.FastStringBuffer;
import net.sf.saxon.value.AnyURIValue;

import javax.xml.transform.Source;
import javax.xml.transform.TransformerException;
import javax.xml.transform.URIResolver;
import javax.xml.transform.stream.StreamSource;
import java.io.*;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.*;
import java.text.Collator;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * This class represents a collection of resources (source documents, schemas, collections etc) used for a number
 * of test cases.
 */

public class
        Environment implements URIResolver {

    public Processor processor;
    public Map<String, XdmNode> sourceDocs = new HashMap<String, XdmNode>();
    public Map<String, String> streamedSecondaryDocs = new HashMap<String, String>();
    public String streamedContent;
    public File streamedFile;
    public XPathCompiler xpathCompiler;
    public XQueryCompiler xqueryCompiler;
    public XsltCompiler xsltCompiler;
    public XsltExecutable xsltExecutable;
    public XdmItem contextItem;
    public HashMap<QName, XdmValue> params = new HashMap<QName, XdmValue>();
    public boolean usable = true;
    public FastStringBuffer paramDeclarations = new FastStringBuffer(256);
    public FastStringBuffer paramDecimalDeclarations = new FastStringBuffer(256);
    public UnparsedTextURIResolver unparsedTextResolver;
    public List<ResetAction> resetActions = new ArrayList<ResetAction>();
    public boolean schemaAvailable = false;
    public boolean outputTree = true;
    public boolean outputSerialize = false;


    /**
     * Construct a local default environment for a test set
     */

    public static Environment createLocalEnvironment(URI baseURI, int generateByteCode, boolean unfolded, Spec spec, TestDriver testDriver) {
        final Environment environment = new Environment();
        environment.processor = new Processor(!testDriver.driverProc.getSaxonEdition().equals("HE"));
        //AutoActivate.activate(environment.processor);

        configureByteCode(testDriver, environment, generateByteCode);

        environment.xpathCompiler = environment.processor.newXPathCompiler();
        environment.xpathCompiler.setBaseURI(baseURI);
        environment.xqueryCompiler = environment.processor.newXQueryCompiler();
        environment.xqueryCompiler.setBaseURI(baseURI);
        environment.xsltCompiler = environment.processor.newXsltCompiler();
        if (spec == Spec.XT30) {
            environment.xsltCompiler.setXsltLanguageVersion("3.0");
        } else if (spec == Spec.XT20) {
            environment.xsltCompiler.setXsltLanguageVersion("2.0");
        }
        if (unfolded) {
            testDriver.addInjection(environment.xqueryCompiler);
        }
        environment.processor.getUnderlyingConfiguration().setDefaultCollection(null);
        return environment;
    }

    /**
     * Construct an Environment
     *
     * @param xpc          the XPathCompiler used to process the catalog file
     * @param env          the Environment element in the catalog file
     * @param environments the set of environments to which this one should be added (may be null)
     * @return the constructed Environment object
     * @throws SaxonApiException
     */

    public static Environment processEnvironment(
            final TestDriver driver,
            XPathCompiler xpc, XdmItem env, Map<String, Environment> environments, Environment defaultEnvironment)
            throws SaxonApiException {
        Environment environment = new Environment();
        String name = ((XdmNode) env).getAttributeValue(new QName("name"));
        if (name != null) {
            System.err.println("Loading environment " + name);
        }
        environment.processor = new Processor(!driver.driverProc.getSaxonEdition().equals("HE"));
        if (defaultEnvironment != null) {
            environment.processor.setConfigurationProperty(FeatureKeys.XSD_VERSION,
                    defaultEnvironment.processor.getConfigurationProperty(FeatureKeys.XSD_VERSION));
        }
        //AutoActivate.activate(environment.processor);


        //jwL TODO temporary
        environment.processor.setConfigurationProperty(FeatureKeys.EXPATH_FILE_DELETE_TEMPORARY_FILES, "true");


        configureByteCode(driver, environment, driver.generateByteCode);

        environment.xpathCompiler = environment.processor.newXPathCompiler();
        environment.xpathCompiler.setBaseURI(((XdmNode) env).getBaseURI());
        environment.xqueryCompiler = environment.processor.newXQueryCompiler();
        environment.xqueryCompiler.setBaseURI(((XdmNode) env).getBaseURI());
        if (driver.spec.shortSpecName.equals("XT")) {
            environment.xsltCompiler = environment.processor.newXsltCompiler();
            environment.xsltCompiler.setXsltLanguageVersion(driver.spec.version);
        }

        if (driver.unfolded) {
            driver.addInjection(environment.xqueryCompiler);
        }

        DocumentBuilder builder = environment.processor.newDocumentBuilder();
        builder.setTreeModel(driver.treeModel);
        environment.sourceDocs = new HashMap<String, XdmNode>();
        if (environments != null && name != null) {
            environments.put(name, environment);
        }
        // TODO: following code inactive? environment does not have dependency child in either QT3 or XSLT3
        for (XdmItem dependency : xpc.evaluate("dependency", env)) {
            if (!driver.dependencyIsSatisfied((XdmNode) dependency, environment)) {
                environment.usable = false;
            }
        }

        for (XdmItem dependency : xpc.evaluate("../dependencies/*", env)) {
            if (!driver.dependencyIsSatisfied((XdmNode) dependency, environment)) {
                environment.usable = false;
            }
        }

        // set the base URI if specified

        setBaseUri(driver, xpc, env, environment);

        // set any requested collations

        registerCollations(xpc, env, environment);

        // declare the requested namespaces

        declareNamespaces(xpc, env, environment);

        // load the requested schema documents

        SchemaManager manager = environment.processor.getSchemaManager();
        boolean validateSources = loadSchemaDocuments(xpc, env, environment, manager);

        // load the requested source documents

        loadSourceDocuments(driver, xpc, env, environment, builder, manager, validateSources);

        // create a collection URI resolver to handle the requested collections

        createCollectionUriResolver(driver, xpc, env, environment, builder);

        // create an unparsed text resolver to handle any unparsed text resources

        createUnparsedTextResolver(driver, xpc, env, environment);

        // register any required decimal formats

        registerDecimalFormats(driver, xpc, env, environment);

        // declare any variables

        declareExternalVariables(driver, xpc, env, environment);

        // declare any output controls
        declareOutputControls(driver, xpc, env, environment);

        // handle requested context item
        for (XdmItem param : xpc.evaluate("context-item", env)) {
            String select = ((XdmNode) param).getAttributeValue(new QName("select"));
            XdmValue value = xpc.evaluate(select, null);
            environment.contextItem = value.itemAt(0);
        }

        // compile any stylesheet defined as part of the environment (only one allowed)
        // use the XSLT processor version associated with the stylesheet/@version attribute
        for (XdmItem stylesheet : xpc.evaluate("stylesheet[not(@role='secondary')]", env)) {
            String fileName = ((XdmNode) stylesheet).getAttributeValue(new QName("file"));
            Source styleSource = new StreamSource(((XdmNode) env).getBaseURI().resolve(fileName).toString());
            try {
                environment.xsltExecutable = environment.xsltCompiler.compile(styleSource);
            } catch (SaxonApiException e) {
                driver.println("**** failure while compiling environment-defined stylesheet " + fileName);
                environment.usable = false;
            }
        }

        // compile any stylesheet packages defined as part of the environment
        // Support this only in EE - an unusable environment in PE/HE
        for (XdmItem stylesheet : xpc.evaluate("package[@role='secondary']", env)) {
            if(!"EE".equals(environment.processor.getSaxonEdition())) {
                environment.usable = false;
                break;
            }
            String fileName = ((XdmNode) stylesheet).getAttributeValue(new QName("file"));
            Source styleSource = new StreamSource(((XdmNode) env).getBaseURI().resolve(fileName).toString());
            try {
                XsltPackage pkg = environment.xsltCompiler.compilePackage(styleSource);
                environment.xsltCompiler.importPackage(pkg);
            } catch (SaxonApiException e) {
                //e.printStackTrace();
                driver.println("**** failure while compiling environment-defined stylesheet package " + fileName);
                 environment.usable = false;
            } catch (Exception e) {
                e.printStackTrace();
                driver.println("****Failure "+ e.toString()+ " in compiling environment "+ name);
               environment.usable = false;
            }
        }

        return environment;
    }

    private static void configureByteCode(TestDriver driver, Environment environment, int generate) {
        if (environment.processor.getSaxonEdition().equals("EE")) {
            if (generate == 1) {
                environment.processor.setConfigurationProperty(FeatureKeys.GENERATE_BYTE_CODE, "true");
                environment.processor.setConfigurationProperty(FeatureKeys.DEBUG_BYTE_CODE, "false");
            } else if (generate == 2) {
                environment.processor.setConfigurationProperty(FeatureKeys.GENERATE_BYTE_CODE, "true");
                environment.processor.setConfigurationProperty(FeatureKeys.DEBUG_BYTE_CODE, "true");
                environment.processor.setConfigurationProperty(FeatureKeys.DEBUG_BYTE_CODE_DIR, "debugByteCode");
            } else {
                environment.processor.setConfigurationProperty(FeatureKeys.GENERATE_BYTE_CODE, "false");
                environment.processor.setConfigurationProperty(FeatureKeys.DEBUG_BYTE_CODE, "false");
            }
        }
    }

    private static void declareOutputControls(TestDriver driver, XPathCompiler xpc, XdmItem env, Environment environment) throws SaxonApiException {
        String needsTree = xpc.evaluate("string((output/@tree,'yes')[1])", env).toString();
        environment.outputTree = "yes".equals(needsTree);
        String needsSerialization = xpc.evaluate("string((output/@serialize,'no')[1])", env).toString();
        environment.outputSerialize = "yes".equals(needsSerialization);
    }

    private static void setBaseUri(TestDriver driver, XPathCompiler xpc, XdmItem env, Environment environment) throws SaxonApiException {
        for (XdmItem base : xpc.evaluate("static-base-uri", env)) {
            String uri = ((XdmNode) base).getAttributeValue(new QName("uri"));
            if (uri == null || "#UNDEFINED".equals(uri)) {
                /*environment.xpathCompiler.setBaseURI(null);
                environment.xqueryCompiler.setBaseURI(null); */
                driver.println("**** Error: The BaseURI null and #UNDEFINED is not supported");
            } else {
                try {
                    environment.xpathCompiler.setBaseURI(new URI(uri));
                    environment.xqueryCompiler.setBaseURI(new URI(uri));
                } catch (URISyntaxException e) {
                    driver.println("**** invalid base URI " + uri);
                } catch (IllegalArgumentException e) {
                    driver.println("**** invalid base URI " + uri);
                }
            }
        }
    }

    private static void registerCollations(XPathCompiler xpc, XdmItem env, Environment environment) throws SaxonApiException {
        for (XdmItem base : xpc.evaluate("collation", env)) {
            String uri = ((XdmNode) base).getAttributeValue(new QName("uri"));
            if (uri.equals("http://www.w3.org/2010/09/qt-fots-catalog/collation/caseblind") ||
                    uri.equals("http://www.w3.org/xslts/collation/caseblind")) {
                Configuration config = xpc.getProcessor().getUnderlyingConfiguration();
                try {
                    StringCollator collator = config.getCollation("http://saxon.sf.net/collation?ignore-case=yes");
                    environment.processor.declareCollation(uri, (Collator) ((SimpleCollation) collator).getCollation());
                } catch (XPathException e) {
                    throw new SaxonApiException(e);
                }
            }
            String defaultAtt = ((XdmNode) base).getAttributeValue(new QName("default"));
            if (defaultAtt != null && (defaultAtt.trim().equals("true") || defaultAtt.trim().equals("1"))) {
                environment.xpathCompiler.declareDefaultCollation(uri);
                environment.xqueryCompiler.declareDefaultCollation(uri);
                environment.xsltCompiler.declareDefaultCollation(uri);
            }
        }
    }

    private static void declareNamespaces(XPathCompiler xpc, XdmItem env, Environment environment) throws SaxonApiException {
        for (XdmItem nsElement : xpc.evaluate("namespace", env)) {
            String prefix = ((XdmNode) nsElement).getAttributeValue(new QName("prefix"));
            String uri = ((XdmNode) nsElement).getAttributeValue(new QName("uri"));
            environment.xpathCompiler.declareNamespace(prefix, uri);
            environment.xqueryCompiler.declareNamespace(prefix, uri);
            if (uri.equals("http://expath.org/ns/file")) {
                // For EXPath file tests, set the EXPath base directory to the catalog directory
                String base = ((XdmNode) nsElement).getBaseURI().toString();
                if (base.startsWith("file:///")) {
                    base = base.substring(7);
                } else if (base.startsWith("file:/")) {
                    base = base.substring(5);
                }
                File file = new File(base);
                file = file.getParentFile();
                System.setProperty("expath.base.directory", file.getAbsolutePath());
            }
        }
    }

    private static boolean loadSchemaDocuments(XPathCompiler xpc, XdmItem env, Environment environment, SchemaManager manager) throws SaxonApiException {
        boolean validateSources = false;
        for (XdmItem schema : xpc.evaluate("schema", env)) {
            String role = ((XdmNode) schema).getAttributeValue(new QName("role"));
            String xsdVersion = ((XdmNode) schema).getAttributeValue(new QName("xsd-version"));
            if (manager == null) {
                System.err.println("*** Processor is not schema aware");
                environment.usable = false;
                return false;
            }
            if (xsdVersion != null) {
                manager.setXsdVersion(xsdVersion);
            }
            if (!"secondary".equals(role)) {
                String href = ((XdmNode) schema).getAttributeValue(new QName("file"));
                File file = new File(((XdmNode) env).getBaseURI().resolve(href));
                try {
                    manager.load(new StreamSource(file));
                } catch (SaxonApiException err) {
                    System.err.println("*** Failed to load schema: " + href + " - " + err.getMessage());
                } catch (NullPointerException err) {
                    err.printStackTrace();
                    System.err.println("*** NPE: Failed to load schema: " + href + " - " + err.getMessage());
                }

                if ("source-reference".equals(role)) {
                    validateSources = true;
                }
                if ("stylesheet-import".equals(role)) {
                    environment.xsltCompiler.setSchemaAware(true);
                }
            }
        }
        return validateSources;
    }

    private static void declareExternalVariables(TestDriver driver, XPathCompiler xpc, XdmItem env, Environment environment) throws SaxonApiException {
        for (XdmItem param : xpc.evaluate("param", env)) {
            String varName = ((XdmNode) param).getAttributeValue(new QName("name"));
            XdmValue value;
            String source = ((XdmNode) param).getAttributeValue(new QName("source"));
            if (source != null) {
                XdmNode sourceDoc = environment.sourceDocs.get(source);
                if (sourceDoc == null) {
                    driver.println("**** Unknown source document " + source);
                }
                value = sourceDoc;
            } else {
                String select = ((XdmNode) param).getAttributeValue(new QName("select"));
                value = xpc.evaluate(select, null);
            }
            environment.params.put(new QName(varName), value);
            environment.xpathCompiler.declareVariable(new QName(varName));
            String declared = ((XdmNode) param).getAttributeValue(new QName("declared"));
            if (declared != null && "true".equals(declared) || "1".equals(declared)) {
                // no action
            } else {
                environment.paramDeclarations.append("declare variable $" + varName + " external; ");
            }
        }
    }

    private static void registerDecimalFormats(TestDriver driver, XPathCompiler xpc, XdmItem env, Environment environment) throws SaxonApiException {
        for (XdmItem decimalFormat : xpc.evaluate("decimal-format", env)) {
            DecimalFormatManager dfm = environment.xpathCompiler.getUnderlyingStaticContext().getDecimalFormatManager();

            XdmNode formatElement = (XdmNode) decimalFormat;
            String formatName = formatElement.getAttributeValue(new QName("name"));
            StructuredQName formatQName = null;
            if (formatName != null) {
                if (formatName.indexOf(':') < 0) {
                    formatQName = new StructuredQName("", "", formatName);
                } else {
                    try {
                        formatQName = StructuredQName.fromLexicalQName(formatName, false, true,
                                new InscopeNamespaceResolver(formatElement.getUnderlyingNode()));
                    } catch (XPathException e) {
                        driver.println("**** Invalid QName as decimal-format name");
                        formatQName = new StructuredQName("", "", "error-name");
                    }
                }
                environment.paramDecimalDeclarations.append("declare decimal-format " + formatQName.getEQName() + " ");
            } else {
                environment.paramDecimalDeclarations.append("declare default decimal-format ");
            }
            DecimalSymbols symbols = (formatQName == null ? dfm.getDefaultDecimalFormat() : dfm.obtainNamedDecimalFormat(formatQName));
            symbols.setHostLanguage(Configuration.XQUERY);
            for (XdmItem decimalFormatAtt : xpc.evaluate("@* except @name", formatElement)) {
                XdmNode formatAttribute = (XdmNode) decimalFormatAtt;
                String property = formatAttribute.getNodeName().getLocalName();
                String value = formatAttribute.getStringValue();
                environment.paramDecimalDeclarations.append(property + "=\"" + value + "\" ");
                try {
                    if (property.equals("decimal-separator")) {
                        symbols.setDecimalSeparator(value);
                    } else if (property.equals("grouping-separator")) {
                        symbols.setGroupingSeparator(value);
                    } else if (property.equals("infinity")) {
                        symbols.setInfinity(value);
                    } else if (property.equals("NaN")) {
                        symbols.setNaN(value);
                    } else if (property.equals("minus-sign")) {
                        symbols.setMinusSign(value);
                    } else if (property.equals("percent")) {
                        symbols.setPercent(value);
                    } else if (property.equals("per-mille")) {
                        symbols.setPerMille(value);
                    } else if (property.equals("zero-digit")) {
                        symbols.setZeroDigit(value);
                    } else if (property.equals("digit")) {
                        symbols.setDigit(value);
                    } else if (property.equals("pattern-separator")) {
                        symbols.setPatternSeparator(value);
                    } else {
                        driver.println("**** Unknown decimal format attribute " + property);
                    }
                } catch (XPathException e) {
                    driver.println("**** " + e.getMessage());
                }
            }
            environment.paramDecimalDeclarations.append(";");
            try {
                symbols.checkConsistency(formatQName);
            } catch (XPathException err) {
                driver.println("**** " + err.getMessage());
            }

        }
    }

    private static void createUnparsedTextResolver(TestDriver driver, XPathCompiler xpc, XdmItem env, Environment environment) throws SaxonApiException {
        final HashMap<URI, Object> resources = new HashMap<URI, Object>();
        final HashMap<URI, String> encodings = new HashMap<URI, String>();
        for (XdmItem resource : xpc.evaluate("resource", env)) {
            String uri = ((XdmNode) resource).getAttributeValue(new QName("uri"));
            String href = ((XdmNode) resource).getAttributeValue(new QName("file"));
            String encoding = ((XdmNode) resource).getAttributeValue(new QName("encoding"));
            if (href != null) {
                Object obj = null;
                if (href.startsWith("http")) {
                    try {
                        obj = new URL(href);
                    } catch (MalformedURLException e) {
                        e.printStackTrace();
                    }
                } else {
                    obj = new File((((XdmNode) env).getBaseURI().resolve(href)));
                }
                try {
                    resources.put(new URI(uri), obj);
                    encodings.put(new URI(uri), encoding);
                    URI abs = ((XdmNode) resource).getBaseURI().resolve(uri);
                    resources.put(abs, obj);
                    encodings.put(abs, encoding);
                } catch (URISyntaxException e) {
                    driver.println("** Invalid URI in environment: " + e.getMessage());
                }
            }
        }
        if (!resources.isEmpty()) {
            environment.unparsedTextResolver =
                    new UnparsedTextURIResolver() {
                        public Reader resolve(URI absoluteURI, String encoding, Configuration config) throws XPathException {
                            if (encoding == null) {
                                encoding = encodings.get(absoluteURI);
                            }
                            if (encoding == null) {
                                encoding = "utf-8";
                            }
                            try {
                                // The following is necessary to ensure that encoding errors are not recovered.
                                Charset charset = Charset.forName(encoding);
                                CharsetDecoder decoder = charset.newDecoder();
                                decoder = decoder.onMalformedInput(CodingErrorAction.REPORT);
                                decoder = decoder.onUnmappableCharacter(CodingErrorAction.REPORT);
                                Object obj = resources.get(absoluteURI);
                                if (obj instanceof File) {
                                    return new BufferedReader(new InputStreamReader(new FileInputStream((File) obj), decoder));
                                } else {
                                    URL resource = (URL) resources.get(absoluteURI);
                                    if (resource == null) {
                                        resource = absoluteURI.toURL();
                                        //throw new XPathException("Unparsed text resource " + absoluteURI + " not registered in catalog", "FOUT1170");
                                    }
                                    InputStream in = resource.openConnection().getInputStream();
                                    return new BufferedReader(new InputStreamReader(in, decoder));
                                }
                                //   return new InputStreamReader(new FileInputStream(resources.get(absoluteURI)), encoding);
                            } catch (IOException ioe) {
                                throw new XPathException(ioe.getMessage(), "FOUT1170");
                            } catch (IllegalCharsetNameException icne) {
                                throw new XPathException("Invalid encoding name: " + encoding, "FOUT1190");
                            } catch (UnsupportedCharsetException uce) {
                                throw new XPathException("Invalid encoding name: " + encoding, "FOUT1190");
                            }
                        }
                    };
        }
    }

    private static void createCollectionUriResolver(final TestDriver driver, XPathCompiler xpc, XdmItem env, Environment environment, DocumentBuilder builder) throws SaxonApiException {
        final HashMap<URI, List<Item>> collections = new HashMap<URI, List<Item>>();
        for (XdmItem coll : xpc.evaluate("collection", env)) {
            String collectionURI = ((XdmNode) coll).getAttributeValue(new QName("uri"));
            if (collectionURI == null) {
                collectionURI = "";
            }
            URI u;
            try {
                u = new URI(collectionURI);
            } catch (URISyntaxException e) {
                driver.println("**** Invalid collection URI " + collectionURI);
                break;
            }
            if (!collectionURI.equals("") && !u.isAbsolute()) {
                u = ((XdmNode) env).getBaseURI().resolve(collectionURI);
                collectionURI = u.toString();
            }
            List<Item> docs = new ArrayList<Item>();
            for (XdmItem source : xpc.evaluate("source", coll)) {
                String href = ((XdmNode) source).getAttributeValue(new QName("file"));
                String frag = null;
                int hash = href.indexOf('#');
                if (hash > 0) {
                    frag = href.substring(hash + 1);
                    href = href.substring(0, hash);
                }
                File file = new File((((XdmNode) env).getBaseURI().resolve(href)));
                String id = ((XdmNode) source).getAttributeValue(new QName(NamespaceConstant.XML, "id"));
                XdmNode doc = builder.build(file);
                if (frag != null) {
                    XdmNode selected = (XdmNode) environment.xpathCompiler.evaluateSingle("id('" + frag + "')", doc);
                    if (selected == null) {
                        driver.println("**** Fragment not found: " + frag);
                        break;
                    }
                    docs.add(selected.getUnderlyingNode());
                } else {
                    docs.add(new AnyURIValue(doc.getDocumentURI().toString()));
                }
                environment.sourceDocs.put(id, doc);
            }
            try {
                collections.put(new URI(collectionURI), docs);
            } catch (URISyntaxException e) {
                driver.println("**** Invalid collection URI " + collectionURI);
            }

        }
        if (!collections.isEmpty()) {
            environment.processor.getUnderlyingConfiguration().setCollectionURIResolver(
                    new CollectionURIResolver() {
                        public SequenceIterator resolve(String href, String base, XPathContext context) throws XPathException {
                            try {
                                List<? extends Item> docs;
                                if (href == null || base == null) {
                                    docs = collections.get(new URI(""));
                                } else {
                                    URI abs = new URI(base).resolve(href);
                                    docs = collections.get(abs);
                                }
                                if (docs == null) {
                                    throw new XPathException("Collection URI not known", "FODC0002");
                                    //driver.println("** Collection URI " + href + " not known");
                                    //return EmptyIterator.getInstance();
                                } else {
                                    return new ListIterator(docs);
                                }
                            } catch (URISyntaxException e) {
                                driver.println("** Invalid URI: " + e.getMessage());
                                return EmptyIterator.getInstance();
                            }
                        }
                    }
            );
        }
    }

    private static void loadSourceDocuments(TestDriver driver, XPathCompiler xpc, XdmItem env, Environment environment, DocumentBuilder builder, SchemaManager manager, boolean validateSources) throws SaxonApiException {
        for (XdmItem source : xpc.evaluate("source", env)) {
            XdmNode doc = null;

            String uri = ((XdmNode) source).getAttributeValue(new QName("uri"));
            String mime = ((XdmNode) source).getAttributeValue(new QName("media-type"));
            if (mime != null && "application/xml".equals(mime)) {
                continue;
            }
            String validation = ((XdmNode) source).getAttributeValue(new QName("", "validation"));
            if (validation == null) {
                validation = "skip";
            }
            String streaming = ((XdmNode) source).getAttributeValue(new QName("", "streaming"));
            TreeModel selectedTreeModel = builder.getTreeModel();
            if (!validateSources && validation.equals("skip")) {
                builder.setSchemaValidator(null);
            } else if (manager == null) {
                environment.usable = false;
            } else {
                if (!selectedTreeModel.isSchemaAware()) {
                    builder.setTreeModel(TreeModel.TINY_TREE);
                }
                SchemaValidator validator = manager.newSchemaValidator();
                validator.setLax("lax".equals(validation));
                builder.setSchemaValidator(validator);
                environment.xpathCompiler.setSchemaAware(true);
                environment.xqueryCompiler.setSchemaAware(true);
                if (environment.xsltCompiler != null) {
                    environment.xsltCompiler.setSchemaAware(true);
                }
            }

            String role = ((XdmNode) source).getAttributeValue(new QName("role"));
            String href = ((XdmNode) source).getAttributeValue(new QName("file"));
            if ("true".equals(streaming)) {
                if (".".equals(role)) {
                    if (href == null) {
                        environment.streamedContent = xpc.evaluate("string(content)", source).toString();
                    } else {
                        environment.streamedFile = new File(((XdmNode) env).getBaseURI().resolve(href));
                    }
                } else {
                    environment.streamedSecondaryDocs.put(
                            new File(((XdmNode) env).getBaseURI().resolve(href)).toURI().toString(),
                            validation);
                }
            } else {
                Source ss;
                if (href != null) {
                    URI fileLoc = ((XdmNode) env).getBaseURI().resolve(href);
                    File file = null;
                    if (fileLoc.getScheme().equals("file")) {
                        file = new File(((XdmNode) env).getBaseURI().resolve(href));
                        if (uri == null) {
                            uri = file.toURI().toString();
                        }
                    }
                    try {
                        ss = (file == null ? new StreamSource(fileLoc.toString())
                                : new StreamSource(new FileInputStream(file), uri));
                    } catch (FileNotFoundException e) {
                        driver.println("*** failed to find source document " + href);
                        continue;
                    }
                } else {
                    // content is inline in the catalog
                    if (uri == null) {
                        uri = ((XdmNode) env).getBaseURI().toString();
                    }
                    String content = xpc.evaluate("string(content)", source).toString();
                    ss = new StreamSource(new StringReader(content), uri);
                }


                try {
//                    if (builder.getTreeModel() instanceof DOMObjectModel && href != null) {
//                        // temporary: build DOM using native parser
//                        URI fileLoc = ((XdmNode) env).getBaseURI().resolve(href);
//                        DOMParser dp = new DOMParser();
//                        try {
//                            dp.parse(new InputSource(fileLoc.toString()));
//                        } catch (SAXException e) {
//                            throw new SaxonApiException(e);
//                        } catch (IOException e) {
//                            throw new SaxonApiException(e);
//                        }
//                        Document dom = dp.getDocument();
//                        doc = builder.wrap(dom);
//                        doc.asSource().setSystemId(uri);
//                    } else {
                        doc = builder.build(ss);
//                    }
                    environment.sourceDocs.put(uri, doc);
                } catch (SaxonApiException e) {
                    //e.printStackTrace();
                    driver.println("*** failed to build source document " + href + " - " + e.getMessage());
                }

                builder.setTreeModel(selectedTreeModel);

                XdmItem selectedItem = doc;
                String select = ((XdmNode) source).getAttributeValue(new QName("select"));
                if (select != null) {
                    XPathSelector selector = environment.xpathCompiler.compile(select).load();
                    selector.setContextItem(selectedItem);
                    selectedItem = selector.evaluateSingle();
                }


                if (role != null) {
                    if (".".equals(role)) {
                        environment.contextItem = selectedItem;
                    } else if (role.startsWith("$")) {
                        String varName = role.substring(1);
                        environment.params.put(new QName(varName), selectedItem);
                        environment.xpathCompiler.declareVariable(new QName(varName));
                        environment.paramDeclarations.append("declare variable $" + varName + " external; ");
                    }
                }
            }
            String definesStylesheet = ((XdmNode) source).getAttributeValue(new QName("defines-stylesheet"));
            if (definesStylesheet != null) {
                definesStylesheet = definesStylesheet.trim();
            }
            if ("true".equals(definesStylesheet) || "1".equals(definesStylesheet)) {
                // try using an embedded stylesheet from the source document
                try {
                    Source styleSource = environment.xsltCompiler.getAssociatedStylesheet(
                            ((XdmNode) environment.contextItem).asSource(), null, null, null);
                    environment.xsltExecutable = environment.xsltCompiler.compile(styleSource);
                } catch (SaxonApiException e) {
                    driver.println("*** failed to compile stylesheet referenced in source document " + href);
                }
            }
        }
    }

    /**
     * The environment acts as a URIResolver
     */

    public Source resolve(String href, String base) throws TransformerException {
        XdmNode node = sourceDocs.get(href);
        if (node == null) {
            String uri;
            try {
                uri = new URI(base).resolve(href).toString();
            } catch (URISyntaxException e) {
                uri = href;
            } catch (IllegalArgumentException e) {
                uri = href;
            }
            String val = streamedSecondaryDocs.get(uri);
            if (val != null) {
                Source source = new StreamSource(uri);
                if (!val.equals("skip")) {
                    source = AugmentedSource.makeAugmentedSource(source);
                    ((AugmentedSource) source).setSchemaValidationMode(Validation.getCode(val));
                }
                return source;
            } else {
                return null;
            }
        } else {
            return node.asSource();
        }
    }


    public abstract static class ResetAction {
        public abstract void reset(Environment env);
    }


}

