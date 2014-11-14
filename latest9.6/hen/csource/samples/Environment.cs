﻿using System;
using System.Collections;
using System.Collections.Generic;
using System.Text;
using Saxon.Api;
using System.Xml;
using System.IO;
using System.Text.RegularExpressions;
using System.Globalization;
using JavaExtenionFunctionCall = com.saxonica.expr.JavaExtensionFunctionCall;
using JConfiguration = net.sf.saxon.Configuration;
using JPreparedStylesheet = net.sf.saxon.PreparedStylesheet;
using JClause = net.sf.saxon.expr.flwor.Clause;
using JCodeInjector = net.sf.saxon.expr.parser.CodeInjector;
using JSimpleCollection = net.sf.saxon.expr.sort.SimpleCollation;
using JDecimalFormatManager = net.sf.saxon.trans.DecimalFormatManager;
using JDecimalSymbols = net.sf.saxon.trans.DecimalSymbols;
using JXpathException = net.sf.saxon.trans.XPathException;
using JFastStringBuffer = net.sf.saxon.tree.util.FastStringBuffer;
using JUnparsedTextURIResolver = net.sf.saxon.lib.UnparsedTextURIResolver;
using JFeatureKeys=net.sf.saxon.lib.FeatureKeys;
using JStringCollator = net.sf.saxon.lib.StringCollator;
using JAnyURIValue = net.sf.saxon.value.AnyURIValue;
using JItem = net.sf.saxon.om.Item;
using JNamespaceConstant = net.sf.saxon.lib.NamespaceConstant;
using JCharSequence = java.lang.CharSequence;
using JExpression = net.sf.saxon.expr.Expression;
using JStructuredQName = net.sf.saxon.om.StructuredQName;


namespace TestRunner
{

/**
 * This class represents a collection of resources (source documents, schemas, collections etc) used for a number
 * of test cases.
 */

public class
        Environment: XmlUrlResolver {

    private class MyEnvironment{

    }

    public Processor processor;
    public Dictionary<string, XdmNode> sourceDocs = new Dictionary<string, XdmNode>();
    public Dictionary<string, string> streamedSecondaryDocs = new Dictionary<string, string>();
    public string streamedPath;
	public string streamedContent;
    public XPathCompiler xpathCompiler;
    public XQueryCompiler xqueryCompiler;
    public XsltCompiler xsltCompiler;
    public XsltExecutable xsltExecutable;
    public XdmItem contextItem;
    public Dictionary<QName, XdmValue> params1 = new Dictionary<QName, XdmValue>();
    public bool xml11 = false;
    public bool usable = true;
    public JFastStringBuffer paramDeclarations = new JFastStringBuffer(256);
    public JFastStringBuffer paramDecimalDeclarations = new JFastStringBuffer(256);
    public JUnparsedTextURIResolver unparsedTextResolver;
    public ResetAction resetAction = null;
	public bool outputTree = true;
	public bool outputSerialize = false;


    /**
     * Construct a local default environment for a test set
     */

		public static Environment createLocalEnvironment(Uri baseURI, int generateByteCode, bool unfolded, Spec spec) {
        Environment environment = new Environment();
        environment.processor = new Processor(true);
        //AutoActivate.activate(environment.processor);
        if (generateByteCode == 1) {
            environment.processor.SetProperty(JFeatureKeys.GENERATE_BYTE_CODE, "true");
            environment.processor.SetProperty(JFeatureKeys.DEBUG_BYTE_CODE, "false");
        } else if (generateByteCode == 2) {
            environment.processor.SetProperty(JFeatureKeys.GENERATE_BYTE_CODE, "true");
            environment.processor.SetProperty(JFeatureKeys.DEBUG_BYTE_CODE, "true");
            //environment.processor.SetProperty(JFeatureKeys.DEBUG_BYTE_CODE_DIR, "debugByteCode");
        } else {
            environment.processor.SetProperty(JFeatureKeys.GENERATE_BYTE_CODE, "false");
            environment.processor.SetProperty(JFeatureKeys.DEBUG_BYTE_CODE, "false");
        }
        //environment.processor.SetProperty(JFeatureKeys.TRACE_EXTERNAL_FUNCTIONS, "true");
        environment.xpathCompiler = environment.processor.NewXPathCompiler();
        environment.xpathCompiler.BaseUri = baseURI.ToString(); //TODO: Check that this is OK
        environment.xqueryCompiler = environment.processor.NewXQueryCompiler();
		environment.xqueryCompiler.BaseUri  = baseURI.AbsolutePath;
        environment.xsltCompiler = environment.processor.NewXsltCompiler();
        environment.xsltCompiler.BaseUri = new Uri(baseURI.ToString());
		if (spec.Equals (Spec.XT30)) {
			environment.xsltCompiler.XsltLanguageVersion = "3.0";
		} else {
				environment.xsltCompiler.XsltLanguageVersion = "2.0";
		}
        if (unfolded) {
           // environment.xqueryCompiler.Implementation.setCodeInjector(new LazyLiteralInjector()); //TODO
        }
        environment.processor.Implementation.setDefaultCollection(null);
        return environment;
    }

	private static void DeclareOutputControls(TestRunner.TestDriver driver, XPathCompiler xpc, XdmItem env, Environment environment) {
			String needsTree = xpc.Evaluate("string((output/@tree,'yes')[1])", env).ToString();
			environment.outputTree = "yes".Equals(needsTree);
			String needsSerialization = xpc.Evaluate("string((output/@serialize,'no')[1])", env).ToString();
			environment.outputSerialize = "yes".Equals(needsSerialization);
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

    public static Environment processEnvironment(TestRunner.TestDriver driver,
            XPathCompiler xpc, XdmItem env, Dictionary<string, Environment> environments, Environment defaultEnvironment) {
        Environment environment = new Environment();
        String name = ((XdmNode) env).GetAttributeValue(new QName("name"));
        if (name != null) {
            System.Console.WriteLine("Loading environment " + name);
        }
        environment.processor = new Processor(true);
        if (defaultEnvironment != null) {
            environment.processor.SetProperty(JFeatureKeys.XSD_VERSION,
                    defaultEnvironment.processor.Implementation.getConfigurationProperty(JFeatureKeys.XSD_VERSION).ToString());
        }
       // AutoActivate.activate(environment.processor);
        if (driver.GenerateByteCode == 1) {
            environment.processor.SetProperty(JFeatureKeys.GENERATE_BYTE_CODE, "true");
            environment.processor.SetProperty(JFeatureKeys.DEBUG_BYTE_CODE, "false");
        } else if (driver.GenerateByteCode == 2) {
            environment.processor.SetProperty(JFeatureKeys.GENERATE_BYTE_CODE, "true");
            environment.processor.SetProperty(JFeatureKeys.DEBUG_BYTE_CODE, "true");
            //environment.processor.SetProperty(JFeatureKeys.DEBUG_BYTE_CODE_DIR, "debugByteCode");
        } else {
            environment.processor.SetProperty(JFeatureKeys.GENERATE_BYTE_CODE, "false");
            environment.processor.SetProperty(JFeatureKeys.DEBUG_BYTE_CODE, "false");
        }
        environment.xpathCompiler = environment.processor.NewXPathCompiler();
        environment.xpathCompiler.BaseUri = ((XdmNode) env).BaseUri.ToString();
        environment.xqueryCompiler = environment.processor.NewXQueryCompiler();
		environment.xqueryCompiler.BaseUri = ((XdmNode) env).BaseUri.AbsolutePath;
			if (driver.Spec.ToString().Contains ("XT")) {
				environment.xsltCompiler = environment.processor.NewXsltCompiler();
				environment.xsltCompiler.XsltLanguageVersion = ((SpecAttr)(driver.Spec.GetAttr())).version;	
		}
        if (driver.Unfolded) {
           // environment.xqueryCompiler.Implementation.setCodeInjector(new LazyLiteralInjector()); //TODO
        }
        DocumentBuilder builder = environment.processor.NewDocumentBuilder();
        builder.TreeModel = driver.TreeModel;
        environment.sourceDocs = new Dictionary<string,XdmNode>();
        if (environments != null && name != null) {
            try
            {
                environments.Add(name, environment);
            } catch(Exception){}
        }
        foreach (XdmItem dependency in xpc.Evaluate("dependency", env)) {
            if (!driver.dependencyIsSatisfied((XdmNode) dependency, environment)) {
                environment.usable = false;
            }
        }

        // set the base URI if specified

        SetBaseUri(driver, xpc, env, environment);

        // set any requested collations

        RegisterCollations(xpc, env, environment);

        // declare the requested namespaces

        DeclareNamespaces(xpc, env, environment);

        // load the requested schema documents

        SchemaManager manager = environment.processor.SchemaManager;
        bool validateSources = LoadSchemaDocuments(xpc, env, manager);

        // load the requested source documents

        LoadSourceDocuments(driver, xpc, env, environment, builder, manager, validateSources);

        // create a collection URI resolver to handle the requested collections

        CreateCollectionUriResolver(driver, xpc, env, environment, builder);

        // create an unparsed text resolver to handle any unparsed text resources

        CreateUnparsedTextResolver(driver, xpc, env, environment);

        // register any required decimal formats

       // registerDecimalFormats(driver, xpc, env, environment);

        // declare any variables

        DeclareExternalVariables(driver, xpc, env, environment);

		// declare any output controls
		DeclareOutputControls(driver, xpc, env, environment);

        // handle requested context item
        foreach (XdmItem param in xpc.Evaluate("context-item", env)) {
            String select = ((XdmNode) param).GetAttributeValue(new QName("select"));
            XdmValue value = xpc.Evaluate(select, null);
            environment.contextItem = (XdmItem)value;
        }

            XmlUrlResolver res = new XmlUrlResolver();
        // compile any stylesheet defined as part of the environment (only one allowed)
            DocumentBuilder builder1 = environment.processor.NewDocumentBuilder();
        foreach (XdmItem stylesheet in xpc.Evaluate("stylesheet[not(@role='secondary')]", env)) {
            string fileName = ((XdmNode)stylesheet).GetAttributeValue(new QName("file"));
            try
            {
                XdmNode styleSource = builder1.Build(res.ResolveUri(((XdmNode)env).BaseUri, fileName));
                environment.xsltExecutable = environment.xsltCompiler.Compile(styleSource);
            } catch (Exception e) {
                driver.println("**** failure while compiling environment-defined stylesheet " + fileName);
            }
        }


			// compile any stylesheet packages defined as part of the environment
			// Support this only in EE - an unusable environment in PE/HE
			foreach (XdmItem stylesheet in xpc.Evaluate("package[@role='secondary']", env)) {
				if(!"EE".Equals(environment.processor.Edition)) {
					environment.usable = false;
					break;
				}
				string fileName = ((XdmNode) stylesheet).GetAttributeValue(new QName("file"));
				Uri uri = res.ResolveUri(((XdmNode)env).BaseUri, fileName);
				FileStream file = new FileStream(uri.AbsolutePath, FileMode.Open, FileAccess.Read);
				try {
					XsltPackage pkg = environment.xsltCompiler.CompilePackage(file);
					environment.xsltCompiler.ImportPackage(pkg);
				} catch (Exception e) {
					//e.printStackTrace();
					driver.println("**** failure while compiling environment-defined stylesheet package " + fileName);
					driver.println("****Failure "+ e.Message+ " in compiling environment "+ name);
					environment.usable = false;
				}
			}

        return environment;
    }


    private static void SetBaseUri(TestDriver driver, XPathCompiler xpc, XdmItem env, Environment environment) {
        foreach (XdmItem basei in xpc.Evaluate("static-base-uri", env)) {
            string uri = ((XdmNode) basei).GetAttributeValue(new QName("uri"));
            if (uri == null || "#UNDEFINED".Equals(uri)) {
                driver.println("**** Error: The BaseURI null and #UNDEFINED is not supported");
            } else {
                try {
                    environment.xpathCompiler.BaseUri = uri;
                    environment.xqueryCompiler.BaseUri = uri;
                } catch (Exception e) {
                    driver.println("**** invalid base URI " + uri);
                } 
            }
        }
    }

    private static void RegisterCollations(XPathCompiler xpc, XdmItem env, Environment environment) {
        CompareInfo info = CultureInfo.CurrentCulture.CompareInfo;
        CompareOptions options = CompareOptions.IgnoreCase;
        foreach (XdmItem basei in xpc.Evaluate("collation", env)) {
            string uri = ((XdmNode) basei).GetAttributeValue(new QName("uri"));
            String defaultAtt = ((XdmNode) basei).GetAttributeValue(new QName("default"));
            bool isDefault = defaultAtt != null && (defaultAtt.Trim().Equals("true") || defaultAtt.Trim().Equals("1"));
            if (uri.Equals("http://www.w3.org/2010/09/qt-fots-catalog/collation/caseblind") ||
                    uri.Equals("http://www.w3.org/xslts/collation/caseblind")) {
                JConfiguration config = xpc.Processor.Implementation;
                JStringCollator collator = config.getCollationURIResolver().resolve("http://saxon.sf.net/collation?ignore-case=yes", config);
                
                environment.xpathCompiler.DeclareCollation(new Uri(uri),info , options, isDefault);//(Collator) ((SimpleCollation) collator.).getCollation());
                environment.xqueryCompiler.DeclareCollation(new Uri(uri),info , options, isDefault);
				if (environment.xsltCompiler != null) {
					environment.xsltCompiler.DeclareCollation (new Uri (uri), info, options, isDefault);
				}
            }
        }
    }

    private static void DeclareNamespaces(XPathCompiler xpc, XdmItem env, Environment environment)  {
        foreach (XdmItem nsElement in xpc.Evaluate("namespace", env)) {
            String prefix = ((XdmNode) nsElement).GetAttributeValue(new QName("prefix"));
            String uri = ((XdmNode) nsElement).GetAttributeValue(new QName("uri"));
            environment.xpathCompiler.DeclareNamespace(prefix, uri);
            environment.xqueryCompiler.DeclareNamespace(prefix, uri);
            if (uri.Equals("http://expath.org/ns/file")) {
                // For EXPath file tests, set the EXPath base directory to the catalog directory
                string basei = ((XdmNode) nsElement).BaseUri.ToString();
                if (basei.StartsWith("file:///")) {
                    basei = basei.Substring(7);
                } else if (basei.StartsWith("file:/")) {
                    basei = basei.Substring(5);
                }
                FileStream file = new FileStream(basei, FileMode.Open, FileAccess.Read); //new File(base);
                
                //System.SetProperty("expath.base.directory", file.ToString());
            }
        }
    }

    private static bool LoadSchemaDocuments(XPathCompiler xpc, XdmItem env, SchemaManager manager){
        bool validateSources = false;
        XmlUrlResolver res = new XmlUrlResolver();
        foreach (XdmItem schema in xpc.Evaluate("schema", env)) {
            String role = ((XdmNode) schema).GetAttributeValue(new QName("role"));
            String xsdVersion = ((XdmNode) schema).GetAttributeValue(new QName("xsd-version"));
            if (xsdVersion != null) {
                manager.XsdVersion = xsdVersion;
            }
            if (!"secondary".Equals(role)) {
                String href = ((XdmNode) schema).GetAttributeValue(new QName("file"));
                Uri uri = res.ResolveUri(((XdmNode)env).BaseUri, href);
                FileStream file = new FileStream(uri.AbsolutePath, FileMode.Open, FileAccess.Read);
                try {
                    manager.Compile(file, uri);
                } catch (Exception err) {
                    System.Console.WriteLine("*** Failed to load schema: " + err.Message + ", Trace:"+ err.StackTrace);
                }

                if ("source-reference".Equals(role)) {
                    validateSources = true;
                }
            }
        }
        return validateSources;
    }

    private static void DeclareExternalVariables(TestDriver driver, XPathCompiler xpc, XdmItem env, Environment environment) {
        foreach (XdmItem param in xpc.Evaluate("param", env)) {
            String varName = ((XdmNode) param).GetAttributeValue(new QName("name"));
            XdmValue value;
            String source = ((XdmNode) param).GetAttributeValue(new QName("source"));
            if (source != null) {
                XdmNode sourceDoc = null;
                try{
                sourceDoc = environment.sourceDocs[source];
                } catch(Exception) {}
                if (sourceDoc == null) {
                    driver.println("**** Unknown source document " + source);
                }
                value = sourceDoc;
            } else {
                String select = ((XdmNode) param).GetAttributeValue(new QName("select"));
                value = xpc.Evaluate(select, null);
            }
            environment.params1.Add(new QName(varName), value);
            environment.xpathCompiler.DeclareVariable(new QName(varName));
            String declared = ((XdmNode) param).GetAttributeValue(new QName("declared"));
            if (declared != null && "true".Equals(declared) || "1".Equals(declared)) {
                // no action
            } else {
                environment.paramDeclarations.append("declare variable $" + varName + " external; ");
            }
        }
    }

    /*private void RegisterDecimalFormats(TestDriver driver, XPathCompiler xpc, XdmItem env, Environment environment)  {
        foreach (XdmItem decimalFormat in xpc.Evaluate("decimal-format", env)) {
            DecimalFormatManager dfm = environment.xpathCompiler.getUnderlyingStaticContext().getDecimalFormatManager();

            XdmNode formatElement = (XdmNode) decimalFormat;
            String formatName = formatElement.getAttributeValue(new QName("name"));
            StructuredQName formatQName = null;
            if (formatName != null) {
                if (formatName.indexOf(':') < 0) {
                    formatQName = new StructuredQName("", "", formatName);
                } else {
                    try {
                        formatQName = StructuredQName.fromLexicalQName(formatName, false, true, Name11Checker.getInstance(),
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
            for (XdmItem decimalFormatAtt : xpc.Evaluate("@* except @name", formatElement)) {
                XdmNode formatAttribute = (XdmNode) decimalFormatAtt;
                String property = formatAttribute.getNodeName().getLocalName();
                String value = formatAttribute.getStringValue();
                environment.paramDecimalDeclarations.append(property + "=\"" + value + "\" ");
                try {
                    if (property.Equals("decimal-separator")) {
                        symbols.setDecimalSeparator(value);
                    } else if (property.Equals("grouping-separator")) {
                        symbols.setGroupingSeparator(value);
                    } else if (property.Equals("infinity")) {
                        symbols.setInfinity(value);
                    } else if (property.Equals("NaN")) {
                        symbols.setNaN(value);
                    } else if (property.Equals("minus-sign")) {
                        symbols.setMinusSign((value));
                    } else if (property.Equals("percent")) {
                        symbols.setPercent((value));
                    } else if (property.Equals("per-mille")) {
                        symbols.setPerMille((value));
                    } else if (property.Equals("zero-digit")) {
                        symbols.setZeroDigit((value));
                    } else if (property.Equals("digit")) {
                        symbols.setDigit((value));
                    } else if (property.Equals("pattern-separator")) {
                        symbols.setPatternSeparator((value));
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
    }*/

    private static void CreateUnparsedTextResolver(TestDriver driver, XPathCompiler xpc, XdmItem env, Environment environment)  {
        Dictionary<Uri, Object> resources = new Dictionary<Uri, Object>();
        Dictionary<Uri, String> encodings = new Dictionary<Uri, String>();
        XmlUrlResolver res = new XmlUrlResolver();
        foreach (XdmItem resource in xpc.Evaluate("resource", env)) {
            String uri = ((XdmNode) resource).GetAttributeValue(new QName("uri"));
            String href = ((XdmNode) resource).GetAttributeValue(new QName("file"));
            String encoding = ((XdmNode) resource).GetAttributeValue(new QName("encoding"));
            if (href != null) {
                Object obj = null;
                if(href.StartsWith("http")) {
                    try {
                        obj = new Uri(href);
                    } catch (Exception e) {
                        throw e;
                    }
                } else{
                    obj = new FileStream(res.ResolveUri(((XdmNode) env).BaseUri, href).AbsolutePath,FileMode.Open, FileAccess.Read);
                }
                try {
                    resources.Add(new Uri(uri), obj);
                    encodings.Add(new Uri(uri), encoding);
                    Uri abs = res.ResolveUri(((XdmNode) resource).BaseUri, uri);
                    resources.Add(abs, obj);
                    encodings.Add(abs, encoding);
                } catch (Exception e) {
                    driver.println("** Invalid URI in environment: " + e.Message);
                }
            }
        }
        if (resources.Count != 0) {
           /* environment.unparsedTextResolver =
                    new UnparsedTextURIResolver() {
                        public Reader resolve(URI absoluteURI, String encoding, Configuration config) {
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
                                    return new BufferedReader(new InputStreamReader(new FileInputStream((File)obj), decoder));
                                } else {
                                    URL resource = (URL)resources.get(absoluteURI);
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
                            } catch(UnsupportedCharsetException uce){
                                throw new XPathException("Invalid encoding name: " + encoding, "FOUT1190");
                            }
                        }
                    };*/
        }
    }

    private static void CreateCollectionUriResolver(TestDriver driver, XPathCompiler xpc, XdmItem env, Environment environment, DocumentBuilder builder) {
        Dictionary<Uri, List<JItem>> collections = new Dictionary<Uri, List<JItem>>();
        XmlUrlResolver res = new XmlUrlResolver();
        foreach (XdmItem coll in xpc.Evaluate("collection", env)) {
            String collectionURI = ((XdmNode) coll).GetAttributeValue(new QName("uri"));
            if (collectionURI == null) {
                collectionURI = "";
            }
            Uri u;
            try {
                u = new Uri(collectionURI);
            } catch (Exception e) {
                driver.println("**** Invalid collection URI " + collectionURI);
                break;
				}
            if (!collectionURI.Equals("") && !u.IsAbsoluteUri) {
                u = res.ResolveUri(((XdmNode)env).BaseUri, collectionURI);
                collectionURI = u.ToString();
            }
            List<JItem> docs = new List<JItem>();
            foreach (XdmItem source in xpc.Evaluate("source", coll)) {
                String href = ((XdmNode) source).GetAttributeValue(new QName("file"));
                String frag = null;
                int hash = href.IndexOf('#');
                if (hash > 0) {
                    frag = href.Substring(hash + 1);
                    href = href.Substring(0, hash);
                }
                FileStream file = new FileStream(res.ResolveUri(((XdmNode) env).BaseUri, href).AbsolutePath, FileMode.Open, FileAccess.Read);
               // String id = ((XdmNode) source).GetAttributeValue(new QName(JNamespaceConstant.XML, "id"));
					String uriStr = res.ResolveUri (((XdmNode)env).BaseUri, href).AbsoluteUri;
				builder.BaseUri = new Uri(uriStr);
                XdmNode doc = builder.Build(file);
                if (frag != null) {
                    XdmNode selected = (XdmNode)environment.xpathCompiler.EvaluateSingle("id('" + frag + "')", doc);
                    if (selected == null) {
                        driver.println("**** Fragment not found: " + frag);
                        break;
                    }
                    docs.Add(selected.Implementation);
                } else {
                    docs.Add(new JAnyURIValue(doc.DocumentUri.ToString()));
                }
					environment.sourceDocs.Add(uriStr, doc);
            }
            try {
                collections.Add(new Uri(collectionURI), docs);
            } catch (Exception e) {
                driver.println("**** Invalid collection URI " + collectionURI);
				}

        }
        if (collections.Count != 0) {
          /*  environment.processor.Implementation.setCollectionURIResolver(
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
            );*/
        }
    }

		// Methods
		//
		public override object GetEntity (Uri href, string role, Type ofObjectToReturn)
		{
			XdmNode node = null;
			String hrefStr = href.ToString();
			//hrefStr = hrefStr.Substring(hrefStr.LastIndexOf ('/')+1);

			bool foundDoc = sourceDocs.TryGetValue(hrefStr, out node);

			if (!foundDoc) {

				string val;
				hrefStr = href.ToString();
				//hrefStr = hrefStr.Substring(hrefStr.LastIndexOf ('/')+1);

				bool foundSecDoc = streamedSecondaryDocs.TryGetValue(hrefStr, out val);
				if (foundSecDoc) {


					//string hrefStr = href.ToString ();
					//hrefStr = (hrefStr.StartsWith("file://") ? hrefStr.Substring(7) : hrefStr);

					javax.xml.transform.Source sourcei = new javax.xml.transform.stream.StreamSource(href.ToString());

				
					 if(!val.Equals("skip")){
					
						sourcei = net.sf.saxon.lib.AugmentedSource.makeAugmentedSource((javax.xml.transform.Source)sourcei);
							((net.sf.saxon.lib.AugmentedSource) sourcei).setSchemaValidationMode(net.sf.saxon.lib.Validation.getCode(val));

					}
					sourcei.setSystemId(hrefStr) ;
					return sourcei;
				} else {
					return null;
				}
			} else {
				return node.Implementation;
			}
		}

		public override Uri ResolveUri (Uri basei, string href)
		{

			return base.ResolveUri (basei, href);
		}

    private static void LoadSourceDocuments(TestDriver driver, XPathCompiler xpc, XdmItem env, Environment environment, DocumentBuilder builder, SchemaManager manager, bool validateSources) {
        XmlUrlResolver res = new XmlUrlResolver();
        foreach (XdmItem source in xpc.Evaluate("source", env)) {
            XdmNode doc = null;

            String uri = ((XdmNode) source).GetAttributeValue(new QName("uri"));
            String mime = ((XdmNode) source).GetAttributeValue(new QName("media-type"));
            if (mime != null && "application/xml".Equals(mime)) {
                continue;
            }
            String validation = ((XdmNode) source).GetAttributeValue(new QName("", "validation"));
            if (validation == null) {
                validation = "skip";
            }
            String streaming = ((XdmNode) source).GetAttributeValue(new QName("", "streaming"));
            if (!validateSources && validation.Equals("skip")) {
                builder.SchemaValidationMode = SchemaValidationMode.None;
            } else {
                SchemaValidator validator = manager.NewSchemaValidator();
                if("lax".Equals(validation)) {
                    validator.IsLax = true;
                    builder.SchemaValidationMode = SchemaValidationMode.Lax;
                } else
                {
                    builder.SchemaValidationMode = SchemaValidationMode.Strict;
                }
                environment.xpathCompiler.SchemaAware = true;
                environment.xqueryCompiler.SchemaAware = true;
				if (environment.xsltCompiler != null) {
					environment.xsltCompiler.SchemaAware = true;
				}
            }

            String role = ((XdmNode) source).GetAttributeValue(new QName("role"));
            String href = ((XdmNode) source).GetAttributeValue(new QName("file"));
            if ("true".Equals(streaming)) {
                if (".".Equals(role)) {
						if (href == null) {
							environment.streamedContent = xpc.Evaluate ("string(content)", source).ToString();
						} else {
							environment.streamedPath = res.ResolveUri (((XdmNode)env).BaseUri, href).AbsolutePath;
						}
                } else {
                    try
                    {
							Console.WriteLine(res.ResolveUri(((XdmNode)env).BaseUri, href).AbsolutePath);
							Console.WriteLine(res.ResolveUri(((XdmNode)env).BaseUri, href).AbsoluteUri);
							Console.WriteLine(res.ResolveUri(((XdmNode)env).BaseUri, href).ToString());
							environment.streamedSecondaryDocs.Add(res.ResolveUri(((XdmNode)env).BaseUri, href).AbsoluteUri, validation);
                    }catch(Exception){}
                }
            } else {
                Stream ss;
                /*if (uri != null) {
                    uri = res.ResolveUri(((XdmNode)env).BaseUri, uri).AbsoluteUri;
                }*/
                if (href != null) {

                    Uri fileLoc = res.ResolveUri(((XdmNode) env).BaseUri, href);
                    FileStream file = null;
                    if (fileLoc.Scheme.Equals("file")) {
                        file = new FileStream(res.ResolveUri(((XdmNode) env).BaseUri, href).AbsolutePath, FileMode.Open, FileAccess.Read);
                        if (uri == null) {
								uri = fileLoc.AbsoluteUri;
                        }
                    }
                    try {
                        ss = (file == null ? new FileStream(fileLoc.ToString(), FileMode.Open, FileAccess.Read)
                                           : file);
                        
							builder.BaseUri = ((XdmNode) env).BaseUri;
                    } catch (FileNotFoundException e) {
                        driver.println("*** failed to find source document " + href + ", Exception:" + e.Message);
                        continue;
                    }
                } else {
                    // content is inline in the catalog
                    if (uri == null) {
                        uri = ((XdmNode) env).BaseUri.AbsolutePath;
                    }
                    string content = xpc.Evaluate("string(content)", source).ToString();
                    byte[] byteArray = Encoding.ASCII.GetBytes(content);
                    ss = new MemoryStream(byteArray);
                    builder.BaseUri = ((XdmNode)env).BaseUri;
                }


                try {
					builder.BaseUri = new Uri(res.ResolveUri(((XdmNode)env).BaseUri, uri).AbsoluteUri);
                    doc = builder.Build(ss);
                    environment.sourceDocs.Add(uri, doc);
                } catch (Exception e) {
                    driver.println("*** failed to build source document " + href + ", Exception:" + e.Message);
                }

                XdmItem selectedItem = doc;
                String select = ((XdmNode) source).GetAttributeValue(new QName("select"));
                if (select != null) {
                    XPathSelector selector = environment.xpathCompiler.Compile(select).Load();
                    selector.ContextItem = selectedItem;
                    selectedItem = selector.EvaluateSingle();
                }


                if (role != null) {
                    if (".".Equals(role)) {
                        environment.contextItem = selectedItem;
                    } else if (role.StartsWith("$")) {
                        String varName = role.Substring(1);
                        environment.params1.Add(new QName(varName), selectedItem);
                        environment.xpathCompiler.DeclareVariable(new QName(varName));
                        environment.paramDeclarations.append("declare variable $" + varName + " external; ");
                    }
                }
            }
            String definesStylesheet = ((XdmNode) source).GetAttributeValue(new QName("defines-stylesheet"));
            if (definesStylesheet != null) {
                definesStylesheet = definesStylesheet.Trim();
            }
            if ("true".Equals(definesStylesheet) || "1".Equals(definesStylesheet)) {
                // try using an embedded stylesheet from the source document
                try {
                    
                    //environment.xsltExecutable = environment.xsltCompiler.Compile(XdmNode.Wrap(JPreparedStylesheet.getAssociatedStylesheet(
                    //        environment.processor.Implementation,
                    //        ((XdmNode) environment.contextItem), null, null, null)));
                    environment.xsltExecutable = environment.xsltCompiler.CompileAssociatedStylesheet(((XdmNode)environment.contextItem));
                    
                } catch (Exception) {
                    driver.println("*** failed to compile stylesheet referenced in source document " + href);
                }
            }
        }
    }

    //public static Environment processEnvironmentOLD_REMOVE(
    //       TestRunner.TestDriver driver,
    //        XPathCompiler xpc, XdmItem env, Dictionary<string, Environment> environments, Environment defaultEnvironment) {
    //    Environment environment = new Environment();
    //    XmlUrlResolver res = new XmlUrlResolver();
    //    string name = ((XdmNode) env).GetAttributeValue(new QName("name"));
    //    if (name != null) {
    //        System.Console.WriteLine("Loading environment " + name);
    //    }
    //    environment.processor = new Processor(true);
    //    if (defaultEnvironment != null) {
    //        environment.processor.SetProperty(JFeatureKeys.XSD_VERSION,
    //                defaultEnvironment.processor.Implementation.getConfigurationProperty(JFeatureKeys.XSD_VERSION).ToString());
    //    }
    //   // AutoActivate.activate(environment.processor);
    //    if (driver.GenerateByteCode == 1) {
    //        environment.processor.SetProperty(JFeatureKeys.GENERATE_BYTE_CODE, "true");
    //        environment.processor.SetProperty(JFeatureKeys.DEBUG_BYTE_CODE, "false");
    //    } else if (driver.GenerateByteCode == 2) {
    //        environment.processor.SetProperty(JFeatureKeys.GENERATE_BYTE_CODE, "true");
    //        environment.processor.SetProperty(JFeatureKeys.DEBUG_BYTE_CODE, "true");
    //        //environment.processor.SetProperty(JFeatureKeys.DEBUG_BYTE_CODE_DIR, "debugByteCode");
    //    } else {
    //        environment.processor.SetProperty(JFeatureKeys.GENERATE_BYTE_CODE, "false");
    //        environment.processor.SetProperty(JFeatureKeys.DEBUG_BYTE_CODE, "false");
    //    }
    //    environment.xpathCompiler = environment.processor.NewXPathCompiler();
    //    environment.xpathCompiler.BaseUri = ((XdmNode) env).BaseUri.ToString();
    //    environment.xqueryCompiler = environment.processor.NewXQueryCompiler();
    //    environment.xqueryCompiler.BaseUri = ((XdmNode) env).BaseUri.ToString();
    //    environment.xsltCompiler = environment.processor.NewXsltCompiler();
    //    if (driver.Unfolded) {
    //       // environment.xqueryCompiler.Implementation.setCodeInjector(new LazyLiteralInjector()); //TODO
    //    }
    //    DocumentBuilder builder = environment.processor.NewDocumentBuilder();
    //    builder.TreeModel = driver.TreeModel;
    //    environment.sourceDocs = new Dictionary<string, XdmNode>();
    //    if (environments != null && name != null) {
    //        try
    //        {
    //            environments.Add(name, environment);
    //        } catch(Exception){}
    //    }
    //    bool dependencySatisfied;
    //    foreach (XdmItem dependency in xpc.Evaluate("dependency", env)) {
    //        if (!driver.dependencyIsSatisfied((XdmNode) dependency, environment)) {
    //            environment.usable = false;
    //        }
    //    }

    //    // set the base URI if specified
    //    foreach (XdmItem basei in xpc.Evaluate("static-base-uri", env)) {
    //        string uri = ((XdmNode) basei).GetAttributeValue(new QName("uri"));
    //        if (uri == null || "#UNDEFINED".Equals(uri)) {
    //            environment.xpathCompiler.BaseUri = null;
    //            environment.xqueryCompiler.BaseUri = null;
    //            driver.println("**** Error: The BaseURI null and #UNDEFINED is not supported");
    //        } else {
    //            //try {
    //            environment.xpathCompiler.BaseUri = uri;
    //            environment.xqueryCompiler.BaseUri = uri; ;
    //            /*} catch (UriSyntaxException e) {
    //                driver.println("**** invalid base URI " + uri);
    //            } catch (IllegalArgumentException e) {
    //                driver.println("**** invalid base URI " + uri);
    //            }*/
    //        }
    //    }

    //    // set any requested collations
        
    //    CompareInfo compareInfo = CultureInfo.CurrentCulture.CompareInfo; //TODO check this is right to use with default Collation
    //    CompareOptions options = CompareOptions.IgnoreCase; //TODO check this is right to use with default Collation

    //    foreach (XdmItem basei in xpc.Evaluate("collation", env)) {
    //        string uri = ((XdmNode) basei).GetAttributeValue(new QName("uri"));
    //        if (uri.Equals("http://www.w3.org/2010/09/qt-fots-catalog/collation/caseblind")) {
    //            JConfiguration config = xpc.Processor.Implementation;
    //            JStringCollator collator = config.getCollationURIResolver().resolve("http://saxon.sf.net/collation?ignore-case=yes", "", config);
                
                
    //            Boolean defaultCol = false;
    //            environment.xpathCompiler.DeclareCollation(new Uri(uri), compareInfo, options, defaultCol);
    //                environment.xqueryCompiler.DeclareCollation(new Uri(uri), compareInfo, options, defaultCol);
    //        }
    //        string defaultAtt = ((XdmNode) basei).GetAttributeValue(new QName("default"));
    //        if (defaultAtt != null && (defaultAtt.Trim().Equals("true") || defaultAtt.Trim().Equals("1"))) {
    //            environment.xpathCompiler.DeclareCollation(new Uri(uri), compareInfo, options, true);
    //            environment.xqueryCompiler.DeclareCollation(new Uri(uri), compareInfo, options, true);
    //        }
    //    }

    //    // declare the requested namespaces
    //    foreach (XdmItem nsElement in xpc.Evaluate("namespace", env)) {
    //        string prefix = ((XdmNode) nsElement).GetAttributeValue(new QName("prefix"));
    //        string uri = ((XdmNode) nsElement).GetAttributeValue(new QName("uri"));
    //        environment.xpathCompiler.DeclareNamespace(prefix, uri);
    //        environment.xqueryCompiler.DeclareNamespace(prefix, uri);
    //        /*if (uri.Equals(EXPathFile.NAMESPACE)) {
    //            // For EXPath file tests, set the EXPath base directory to the catalog directory
    //            string base = ((XdmNode) nsElement).BaseUri.tostring();
    //            if (base.startsWith("file:///")) {
    //                base = base.substring(7);
    //            } else if (base.startsWith("file:/")) {
    //                base = base.substring(5);
    //            }
    //            File file = new File(base);
    //            file = file.getParentFile();
    //            System.setProperty("expath.base.directory", file.getAbsolutePath());
    //        }*/
    //    }

    //    // load the requested schema documents
    //    SchemaManager manager = environment.processor.SchemaManager;
    //    bool validateSources = false;
    //    foreach (XdmItem schema in xpc.Evaluate("schema", env)) {
    //        string role = ((XdmNode) schema).GetAttributeValue(new QName("role"));
    //        string xsdVersion = ((XdmNode) schema).GetAttributeValue(new QName("xsd-version"));
    //        if (xsdVersion != null) {
    //            manager.XsdVersion = xsdVersion;
    //        }
    //        if (!"secondary".Equals(role)) {
    //            string href = ((XdmNode)schema).GetAttributeValue(new QName("file"));           
    //            try {
    //                manager.Compile((new Uri(((XdmNode)env).BaseUri, href)));
    //            } catch (Exception err) {
    //                System.Console.WriteLine("*** Failed to load schema: " + err.Message);
    //            }

    //            if ("source-reference".Equals(role)) {
    //                validateSources = true;
    //            }
    //        }
    //    }

    //    // load the requested source documents
    //    foreach (XdmItem source in xpc.Evaluate("source", env)) {
    //        XdmNode doc = null;
    //        string file = ((XdmNode)source).GetAttributeValue(new QName("file"));
    //        Uri href = res.ResolveUri(((XdmNode)env).BaseUri, ((XdmNode)source).GetAttributeValue(new QName("file")));
    //        string uri = ((XdmNode) source).GetAttributeValue(new QName("uri"));
    //        string mime = ((XdmNode) source).GetAttributeValue(new QName("media-type"));
    //        if (mime != null && "application/xml".Equals(mime)) {
    //            continue;
    //        }
    //        string validation = ((XdmNode) source).GetAttributeValue(new QName("", "validation"));
    //        if (validation == null) {
    //            validation = "skip";
    //        }
    //        string streaming = ((XdmNode) source).GetAttributeValue(new QName("", "streaming"));
    //        if (!validateSources && validation.Equals("skip")) {
    //            builder.SchemaValidationMode = SchemaValidationMode.None;
    //           // builder..setSchemaValidator(null);
                 
    //            try
    //                {
    //                    doc = builder.Build(href);
                       
    //                }
    //                catch (Exception e)
    //                {
    //                    System.Console.WriteLine("Error:" + e.Message+" *** failed to build source document " + href, false);
    //                }
    //        } else {
    //            SchemaValidator validator = manager.NewSchemaValidator();        
    //            XdmDestination xdmDest = new XdmDestination();
    //            validator.IsLax = validation.Equals("lax");
    //            validator.SetDestination(xdmDest);
    //            validator.SetSource(href);
    //            try
    //            {
    //                validator.Run();
    //            } catch(Exception ){}
    //            doc = xdmDest.XdmNode;
    //            environment.xpathCompiler.SchemaAware = true;
    //            environment.xqueryCompiler.SchemaAware = true;
    //            environment.xsltCompiler.SchemaAware = true;
    //        }

            
    //            if (uri != null)
    //            {
    //                environment.sourceDocs.Add(uri, doc);
    //            }
    //            string role = ((XdmNode)source).GetAttributeValue(new QName("role"));
    //            Uri hrefFile = res.ResolveUri(((XdmNode)env).BaseUri, ((XdmNode)source).GetAttributeValue(new QName("file")));
    //        if ("true".Equals(streaming)) {
    //            if (".".Equals(role)) {
    //                environment.streamedSource = new FileStream(hrefFile.AbsolutePath, FileMode.Open, FileAccess.Read);
    //            } else {
                    
    //                environment.streamedSecondaryDocs.Add(hrefFile.ToString(),
    //                        new FileStream(hrefFile.AbsolutePath, FileMode.Open, FileAccess.Read));
    //            }
    //        } else {
    //            Stream ss = null;
    //            if (href != null) {
    //                Stream file = null;
    //                if (hrefFile.Scheme.Equals("file")) {
    //                    file = new FileStream(hrefFile.AbsolutePath, FileMode.Open, FileAccess.Read);
    //                    try
    //                    {
    //                        doc = builder.Build(file);
    //                        environment.sourceDocs.Add(uri, doc);
    //                    }
    //                    catch (Exception e)
    //                    {
    //                        driver.println("*** failed to build source document " + href);
    //                    }
    //                }
    //                try {
    //                    file = new FileStream(hrefFile.AbsolutePath, FileMode.Open, FileAccess.Read);
    //                    try
    //                    {
    //                        doc = builder.Build(hrefFile);
    //                        environment.sourceDocs.Add(uri, doc);
    //                    }
    //                    catch (Exception e)
    //                    {
    //                        driver.println("*** failed to build source document " + href);
    //                    }
    //                    /*ss = (file == null ? (Stream) new XmlTextReader(hrefFile.ToString());
    //                                       : file);*/
    //                } catch (FileNotFoundException e) {
    //                    driver.println("*** failed to find source document " + href);
    //                    continue;
    //                }
    //            } else {
    //                // content is inline in the catalog
    //                if (uri == null) {
    //                    uri = ((XdmNode) env).BaseUri.ToString();
    //                }
    //                String content = xpc.Evaluate("string(content)", source).ToString();
    //                TextReader textReader = new StringReader(content);

    //                try
    //                {
    //                    doc = builder.Build(textReader);
    //                    environment.sourceDocs.Add(uri, doc);
    //                }
    //                catch (Exception e)
    //                {
    //                    driver.println("*** failed to build source document " + href);
    //                }
    //            }


                

    //            XdmItem selectedItem = doc;
    //            String select = ((XdmNode) source).GetAttributeValue(new QName("select"));
    //            if (select != null) {
    //                XPathSelector selector = environment.xpathCompiler.Compile(select).Load();
    //                selector.ContextItem = selectedItem;
    //                selectedItem = selector.EvaluateSingle();
    //            }


    //            try
    //            {
    //                if (role.StartsWith("$"))
    //                {
    //                    string varName = role.Substring(1);
    //                    environment.params1.Add(new QName(varName), doc);
    //                    environment.xpathCompiler.DeclareVariable(new QName(varName));
    //                    environment.paramDeclarations.append("declare variable $" + varName + " external; ");
    //                }
    //            } catch(Exception){}
                

           
                
    //        }
    //        /*string definesStylesheet = ((XdmNode) source).GetAttributeValue(new QName("defines-stylesheet"));
    //        if (definesStylesheet != null) {
    //            definesStylesheet = definesStylesheet.Trim();
    //        }
    //        if ("true".Equals(definesStylesheet) || "1".Equals(definesStylesheet)) {
    //            // try using an embedded stylesheet from the source document
    //            try {
    //                Source styleSource = PreparedStylesheet.getAssociatedStylesheet(
    //                        environment.processor.getUnderlyingConfiguration(),
    //                        ((XdmNode) environment.contextItem).asSource(), null, null, null);
    //                environment.xsltExecutable = environment.xsltCompiler.compile(styleSource);
    //            } catch (TransformerConfigurationException e) {
    //                driver.println("*** failed to compile stylesheet referenced in source document " + href);
    //            }
    //        } */
        

    //    // create a collection URI resolver to handle the requested collections
    //    Hashtable collections = new Hashtable();
    //    //Dictionary<Uri, List<JAnyURIValue>> collections = new Dictionary<Uri, List<JAnyURIValue>>();
    //    foreach (XdmItem coll in xpc.Evaluate("collection", env)) {
    //        string collectionURI = ((XdmNode) coll).GetAttributeValue(new QName("uri"));
    //        if (collectionURI == null) {
    //            collectionURI = "";
    //        }
    //        Uri u;
    //        try
    //        {
    //            u = new Uri(collectionURI);
    //        }catch(Exception) {
    //            driver.println("**** Invalid collection URI " + collectionURI);
    //            break;
    //        }
           
    //        if (!collectionURI.Equals("") && !u.IsAbsoluteUri) {
    //            u = res.ResolveUri(((XdmNode)env).BaseUri, collectionURI);
    //            collectionURI = u.ToString();
    //        }
    //        List<JAnyURIValue> docs = new List<JAnyURIValue>();
    //        foreach (XdmItem source1 in xpc.Evaluate("source", coll)) {
    //            string href1 = ((XdmNode) source1).GetAttributeValue(new QName("file"));
    //            if (href1.Contains("#")) {
    //                // TODO: support fragment identifiers
    //                driver.println("*** Fragment identifiers not supported");
    //                continue;
    //            }
    //            Uri uri1 = res.ResolveUri(((XdmNode)env).BaseUri, href1);
    //            //string id = ((XdmNode) source).GetAttributeValue(new QName(JNamespaceConstant.XML, "id"));
    //            XdmNode doc1 = builder.Build(uri1);
    //            //environment.sourceDocs.Add(id, doc);
    //            docs.Add(new JAnyURIValue(doc.DocumentUri.ToString()));
    //        }
    //        try {
    //            collections.Add(new Uri(collectionURI), docs);
    //        } catch (Exception e) {
    //            driver.println("**** Invalid collection URI " + collectionURI);
    //        }

    //    }
    //    if (collections.Count != 0) {
    //           environment.processor.Implementation.setCollectionURIResolver(new CollectionResolver(collections));
    //        /*environment.processor.Implementation.setCollectionURIResolver(
    //                new CollectionUriResolver() {
    //                    public SequenceIterator resolve(string href, string base, XPathContext context) throws XPathException {
    //                        try {
    //                            List<AnyURIValue> docs;
    //                            if (href == null || base == null) {
    //                                docs = collections.get(new URI(""));
    //                            } else {
    //                                URI abs = new URI(base).resolve(href);
    //                                docs = collections.get(abs);
    //                            }
    //                            if (docs == null) {
    //                                throw new XPathException("Collection URI not known", "FODC0002");
    //                                //driver.println("** Collection URI " + href + " not known");
    //                                //return EmptyIterator.getInstance();
    //                            } else {
    //                                return new ListIterator(docs);
    //                            }
    //                        } catch (URISyntaxException e) {
    //                            driver.println("** Invalid URI: " + e.getMessage());
    //                            return EmptyIterator.getInstance();
    //                        }
    //                    }
    //                }
    //        );*/
    //    }

    //    // create an unparsed text resolver to handle any unparsed text resources

    //    Dictionary<Uri, Object> resources = new Dictionary<Uri, Object>();
    //    Dictionary<Uri, string> encodings = new Dictionary<Uri, string>();
    //    foreach (XdmItem resource in xpc.Evaluate("resource", env)) {
    //        string uri1 = ((XdmNode) resource).GetAttributeValue(new QName("uri"));
    //        string href1 = ((XdmNode) resource).GetAttributeValue(new QName("file"));
    //        string encoding = ((XdmNode) resource).GetAttributeValue(new QName("encoding"));
    //        if (href1 != null) {
    //            Object obj = null;
    //            if(href1.StartsWith("http")) {
    //                try {
    //                    obj = new Uri(href1);
    //                } catch (Exception e) {
    //                    System.Console.WriteLine(e.StackTrace);  //To change body of catch statement use File | Settings | File Templates.
    //                }
    //            } else{
    //                obj = res.ResolveUri(((XdmNode) env).BaseUri, href1);
    //            }
    //            try {
    //                resources.Add(new Uri(uri1), obj);
    //                encodings.Add(new Uri(uri1), encoding);
    //            } catch (Exception e) {
    //                driver.println("** Invalid URI in environment: " + e.Message);
    //            }
    //        }
    //    }
    //    if (resources.Count != 0) {
    //      //  environment.unparsedTextResolver = 
    //               /* new UnparsedTextURIResolver() {
    //                    public Reader resolve(URI absoluteURI, string encoding, Configuration config) throws XPathException {
    //                        if (encoding == null) {
    //                            encoding = encodings.get(absoluteURI);
    //                        }
    //                        if (encoding == null) {
    //                            encoding = "utf-8";
    //                        }
    //                        try {
    //                            // The following is necessary to ensure that encoding errors are not recovered.
    //                            Charset charset = Charset.forName(encoding);
    //                            CharsetDecoder decoder = charset.newDecoder();
    //                            decoder = decoder.onMalformedInput(CodingErrorAction.REPORT);
    //                            decoder = decoder.onUnDictionarypableCharacter(CodingErrorAction.REPORT);
    //                            Object obj = resources.get(absoluteURI);
    //                            if (obj instanceof File) {
    //                                return new BufferedReader(new InputStreamReader(new FileInputStream((File)obj), decoder));
    //                            } else {
    //                                URL resource = (URL)resources.get(absoluteURI);
    //                                if (resource == null) {
    //                                    throw new XPathException("Unparsed text resource " + absoluteURI + " not registered in catalog", "FOUT1170");
    //                                }
    //                                InputStream in = resource.openConnection().getInputStream();
    //                                return new BufferedReader(new InputStreamReader(in, decoder));
    //                            }
    //                            //   return new InputStreamReader(new FileInputStream(resources.get(absoluteURI)), encoding);
    //                        } catch (IOException ioe) {
    //                            throw new XPathException(ioe.getMessage(), "FOUT1170");
    //                        } catch (IllegalCharsetNameException icne) {
    //                            throw new XPathException("Invalid encoding name: " + encoding, "FOUT1190");
    //                        } catch(UnsupportedCharsetException uce){
    //                            throw new XPathException("Invalid encoding name: " + encoding, "FOUT1190");
    //                        }
    //                    }
    //                }; */
    //    }

    //    // register any required decimal formats
    //   /* foreach (XdmItem decimalFormat in xpc.Evaluate("decimal-format", env)) {
    //        JDecimalFormatManager dfm = environment.xpathCompiler.getUnderlyingStaticContext().getDecimalFormatManager();

    //        XdmNode formatElement = (XdmNode) decimalFormat;
    //        string formatName = formatElement.GetAttributeValue(new QName("name"));
    //        StructuredQName formatQName = null;
    //        if (formatName != null) {
    //            if (formatName.indexOf(':') < 0) {
    //                formatQName = new StructuredQName("", "", formatName);
    //            } else {
    //                try {
    //                    formatQName = StructuredQName.fromLexicalQName(formatName, false, true, Name11Checker.getInstance(),
    //                            new InscopeNamespaceResolver(formatElement.getUnderlyingNode()));
    //                } catch (XPathException e) {
    //                    driver.println("**** Invalid QName as decimal-format name");
    //                    formatQName = new StructuredQName("", "", "error-name");
    //                }
    //            }
    //            environment.paramDecimalDeclarations.append("declare decimal-format " + formatQName.getEQName() + " ");
    //        } else {
    //            environment.paramDecimalDeclarations.append("declare default decimal-format ");
    //        }
    //        DecimalSymbols symbols = (formatQName == null ? dfm.getDefaultDecimalFormat() : dfm.obtainNamedDecimalFormat(formatQName));
    //        symbols.setHostLanguage(Configuration.XQUERY);
    //        for (XdmItem decimalFormatAtt : xpc.Evaluate("@* except @name", formatElement)) {
    //            XdmNode formatAttribute = (XdmNode) decimalFormatAtt;
    //            string property = formatAttribute.getNodeName().getLocalName();
    //            string value = formatAttribute.getstringValue();
    //            environment.paramDecimalDeclarations.append(property + "=\"" + value + "\" ");
    //            try {
    //                if (property.Equals("decimal-separator")) {
    //                    symbols.setDecimalSeparator(value);
    //                } else if (property.Equals("grouping-separator")) {
    //                    symbols.setGroupingSeparator(value);
    //                } else if (property.Equals("infinity")) {
    //                    symbols.setInfinity(value);
    //                } else if (property.Equals("NaN")) {
    //                    symbols.setNaN(value);
    //                } else if (property.Equals("minus-sign")) {
    //                    symbols.setMinusSign((value));
    //                } else if (property.Equals("percent")) {
    //                    symbols.setPercent((value));
    //                } else if (property.Equals("per-mille")) {
    //                    symbols.setPerMille((value));
    //                } else if (property.Equals("zero-digit")) {
    //                    symbols.setZeroDigit((value));
    //                } else if (property.Equals("digit")) {
    //                    symbols.setDigit((value));
    //                } else if (property.Equals("pattern-separator")) {
    //                    symbols.setPatternSeparator((value));
    //                } else {
    //                    driver.println("**** Unknown decimal format attribute " + property);
    //                }
    //            } catch (XPathException e) {
    //                driver.println("**** " + e.getMessage());
    //            }
    //        }
    //        environment.paramDecimalDeclarations.append(";");
    //        try {
    //            symbols.checkConsistency(formatQName);
    //        } catch (XPathException err) {
    //            driver.println("**** " + err.getMessage());
    //        }

    //    }*/

    //    // declare any variables
    //    foreach (XdmItem param in xpc.Evaluate("param", env)) {
    //        string varName = ((XdmNode) param).GetAttributeValue(new QName("name"));
    //        XdmValue value;
    //        string source1 = ((XdmNode) param).GetAttributeValue(new QName("source"));
    //        if (source1 != null) {
    //            XdmNode sourceDoc = environment.sourceDocs[source1];
    //            if (sourceDoc == null) {
    //                driver.println("**** Unknown source document " + source);
    //            }
    //            value = sourceDoc;
    //        } else {
    //            string select = ((XdmNode) param).GetAttributeValue(new QName("select"));
    //            value = xpc.Evaluate(select, null);
    //        }
    //        environment.params1.Add(new QName(varName), value);
    //        environment.xpathCompiler.DeclareVariable(new QName(varName));
    //        string declared = ((XdmNode) param).GetAttributeValue(new QName("declared"));
    //        if (declared != null && "true".Equals(declared) || "1".Equals(declared)) {
    //            // no action
    //        } else {
    //            environment.paramDeclarations.append("declare variable $" + varName + " external; ");
    //        }
    //    }

    //    // handle requested context item
    //    foreach (XdmItem param in xpc.Evaluate("context-item", env)) {
    //        string select = ((XdmNode) param).GetAttributeValue(new QName("select"));
    //        XdmValue value = xpc.Evaluate(select, null);
    //        environment.contextItem = (XdmItem)value.Simplify;
    //    }

    //    // compile any stylesheet defined as part of the environment (only one allowed)
    //    foreach (XdmItem stylesheet in xpc.Evaluate("stylesheet[not(@role='secondary')]", env)) {
    //        string fileName = ((XdmNode)stylesheet).GetAttributeValue(new QName("file"));
            
    //        try {
    //            XdmNode styleSource = builder.Build(res.ResolveUri(((XdmNode)env).BaseUri, fileName));
    //            environment.xsltExecutable = environment.xsltCompiler.Compile(styleSource);
    //        } catch (Exception e) {
    //            driver.println("**** failure while compiling environment-defined stylesheet " + fileName);
    //        }
    //    }

    //    return environment;
    //}
    //    return null;
    //}
        
    

     public class CollectionResolver: net.sf.saxon.lib.CollectionURIResolver {
            Hashtable collections;
            public CollectionResolver(Hashtable collections)
            { 
                this.collections = collections;
            }
                            public net.sf.saxon.om.SequenceIterator resolve(string href, string baseStr, net.sf.saxon.expr.XPathContext context)  {
                                try {
                                    List<Uri> docs;
                                    if (href == null) {
                                        docs = (List<Uri>)collections[new Uri("http://www.saxonica.com/defaultCollection")];
                                    } else {
                                        XmlUrlResolver res = new XmlUrlResolver();
                                        Uri abs= res.ResolveUri(new Uri(baseStr), href);
                                        try
                                        {
                                            docs = (List<Uri>)collections[abs];
                                        }
                                        catch (Exception) {
                                            docs = null;
                                        }
                                    }
                                    if (docs == null) {
                                        return net.sf.saxon.tree.iter.EmptyIterator.getInstance();
                                    } else {
                                        java.util.List list = new java.util.ArrayList();
                                        foreach(Uri uri in docs){
                                            list.add(new net.sf.saxon.value.AnyURIValue(uri.ToString()));
                                        }
                                        
                                        return new net.sf.saxon.tree.iter.ListIterator(list);
                                    }
                                } catch (java.net.URISyntaxException e) {
                                    Console.WriteLine("** Invalid Uri: " + e.Message);
                                    return net.sf.saxon.tree.iter.EmptyIterator.getInstance();
                                }
                            }
                        }

    /**
     * The environment acts as a URIResolver
     */

    /*public Source resolve(string href, string base) throws TransformerException {
        XdmNode node = sourceDocs.get(href);
        if (node == null) {
            string uri;
            try {
                uri = new URI(base).resolve(href).tostring();
            } catch (URISyntaxException e) {
                uri = href;
            } catch (IllegalArgumentException e) {
                uri = href;
            }
            string val = streamedSecondaryDocs.get(uri);
            if (val != null) {
                Source source = new StreamSource(uri);
                if (!val.Equals("skip")) {
                    source = AugmentedSource.makeAugmentedSource(source);
                    ((AugmentedSource)source).setSchemaValidationMode(Validation.getCode(val));
                }
                return source;
            } else {
                return null;
            }
        } else {
            return node.asSource();
        }
    }*/


   /* private class LazyLiteralInjector: JCodeInjector {
        public JExpression inject(JExpression exp, StaticContext env, int construct, JStructuredQName qName) {
            if (exp is net.sf.saxon.expr.Literal) {
                JStructuredQName name = new JStructuredQName("saxon", JNamespaceConstant.SAXON, "lazy-literal");
                com.saxonica.expr.JavaExtensionFunctionCall wrapper = new com.saxonica.expr.JavaExtensionFunctionCall();
                try {
                    wrapper.init(name, FOTestSuiteDriver.class,
                            FOTestSuiteDriver.class.getMethod("lazyLiteral", Sequence.class),
                            env.getConfiguration());
                    wrapper.setArguments(new Expression[]{exp});
                } catch (NoSuchMethodException e) {
                    throw new IllegalStateException(e);
                }
                return wrapper;
            } else {
                return exp;
            }
        }

        public Clause injectClause(Clause target, StaticContext env, Container container) {
            return null;
        }
    } */

    public abstract class ResetAction {
        public abstract void reset(Environment env);
    }


}
}

