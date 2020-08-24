using System;
using System.Collections;
using System.Collections.Generic;
using System.Text;
using System.Xml;
using System.IO;
using System.Text.RegularExpressions;
using System.Globalization;
using Saxon.Api;


using JFeatureKeys = net.sf.saxon.lib.FeatureKeys;
using JConfiguration = net.sf.saxon.Configuration;
using JVersion = net.sf.saxon.Version;
using JResult = javax.xml.transform.Result;
using System.Diagnostics;
using TestRunner;
using System.Linq;
using java.util;

namespace TestRunner
{
    //C:/work/repositories/hg/xslt30-test C:/work/repositories/hg/xslt30-test/results/saxon/ -lang:XT30 -xt30:on

    // file:////C:/work/repositories/hg/xslt30-test/ C:/work/repositories/hg/xslt30-test/results/saxon/ -lang:XT30 -xt30:on -s:mode -t:mode-1801
    /**
     * This class runs the W3C XSLT Test Suite, driven from the test catalog.
     */
    public class Xslt30TestSuiteDriver : TestDriver {



        private HashSet<String> alwaysOn = new HashSet<String>();

        private HashSet<String> alwaysOff = new HashSet<String>();

        private HashSet<String> needsEE = new HashSet<String>();

        private HashSet<String> needsPE = new HashSet<String>();

        public Xslt30TestSuiteDriver() {
            SetDependencyData();
        }


        //file:////C:/work/repositories/hg/xslt30-test/ C:/work/repositories/hg/xslt30-test/results/saxon/ -lang:XT30 -xt30:on
        public static void Main(string[] args) {
        if (args.Length == 0 || args[0].Equals("-?")) {
            System.Console.WriteLine("testsuiteDir catalog [-o:resultsdir] [-s:testSetName]" +
                    " [-t:testNamePattern] [-bytecode:on|off|debug] [-tree] [-lang] [-save][-export][-strict]");
        }

        System.Console.WriteLine("Testing Saxon " + (new Processor()).ProductVersion);
        new Xslt30TestSuiteDriver().go(args);
    }

        public static IPredicate<XdmItem> IsTrue(string attName) {
            return new Saxon.Api.Predicate<XdmItem>(element =>
            {
                if (element is XdmNode)
                {
                    string attVal = ((XdmNode)element).GetAttributeValue(attName);
                    if (attVal != null)
                    {
                        attVal = attVal.Trim();
                        return "yes".Equals(attVal) || "1".Equals(attVal) || "true".Equals(attVal);
                    }

                }
                return false;

            });
            
        }

        public void SetDependencyData() {
            alwaysOn.Add("feature/disabling_output_escaping");
            alwaysOn.Add("feature/serialization");
            alwaysOn.Add("feature/namespace_axis");
            alwaysOn.Add("feature/dtd");
            alwaysOn.Add("feature/built_in_derived_types");
            alwaysOn.Add("feature/remote_http");
            alwaysOn.Add("feature/xsl-stylesheet-processing-instruction");
            alwaysOn.Add("feature/fn-transform-XSLT");
            alwaysOn.Add("available_documents");
            alwaysOn.Add("ordinal_scheme_name");
            alwaysOn.Add("default_calendar_in_date_formatting_functions");
            alwaysOn.Add("supported_calendars_in_date_formatting_functions");
            alwaysOn.Add("maximum_number_of_decimal_digits");
            alwaysOn.Add("default_output_encoding");
            alwaysOn.Add("unparsed_text_encoding");
            alwaysOn.Add("recognize_id_as_uri_fragment");
            alwaysOn.Add("feature/XPath_3.1");
            alwaysOn.Add("feature/backwards_compatibility");
            alwaysOn.Add("feature/HTML4");
            alwaysOn.Add("feature/HTML5");

            needsPE.Add("feature/Saxon-PE");
            needsPE.Add("feature/dynamic_evaluation");

            needsEE.Add("languages_for_numbering");
            needsEE.Add("feature/streaming");
            needsEE.Add("feature/schema_aware");
            needsEE.Add("feature/Saxon-EE");
            //needsEE.Add("feature/XSD_1.1");


            needsEE.Add("feature/xquery_invocation");
            needsEE.Add("feature/higher_order_functions");

            alwaysOn.Add("detect_accumulator_cycles");




        }

    
    public override string catalogNamespace() {
        return "http://www.w3.org/2012/10/xslt-test-catalog";
    }

    public void writeResultFilePreamble(Processor processor, XdmNode catalog) {
        resultsDoc = new Xslt30ResultsDocument(this.resultsDir, Spec.XT30);
        //super.writeResultFilePreamble(processor, catalog);
    }

    
    public override void processSpec(string specStr) {
			if (specStr.Equals("XT10")) {
				spec = Spec.XT10;
			} else if (specStr.Equals("XT20")) {
				spec = Spec.XT20;
			} else if (specStr.Equals("XT30")) {
				spec = Spec.XT30;
			} else {
				throw new Exception("Unknown spec " + specStr);
			}
        resultsDoc = new Xslt30ResultsDocument(this.resultsDir, Spec.XT30);
        // No action: always use XSLT
    }

    
    protected override void createGlobalEnvironments(XdmNode catalog, XPathCompiler xpc) {
        Environment environment = null;
        Environment defaultEnv = null;
         try
            {
                defaultEnv = localEnvironments["default"];
            }
            catch (Exception) { }
        foreach (XdmNode env in catalog.Select(Steps.Descendant("environment"))) {
            environment = Environment.processEnvironment(this, xpc, env, globalEnvironments, defaultEnv);
        }
        //buildDependencyMap(driverProc, environment);
    }

		/**
     * Return a set of named parameters as a map
     *
     * @param xpath     The XPath compiler to use
     * @param node      The node to search for <param> children
     * @param getStatic Whether to collect static or non-static sets
     * @param tunnel    Whether to collect tunnelled or non-tunnelled sets
     * @return Map of the evaluated parameters, keyed by QName
     * @throws SaxonApiException
     */

		internal Dictionary <QName, XdmValue> GetNamedParameters(XPathCompiler xpath, XdmNode node, bool getStatic, bool tunnel)  {
			Dictionary<QName, XdmValue> params1 = new Dictionary<QName, XdmValue>();
			int i = 1;
			string staticTest = getStatic ? "@static='yes'" : "not(@static='yes')";
			foreach (XdmItem parami in xpath.Evaluate("param[" + staticTest + "]", node)) {
				QName name = GetQNameAttribute(xpath, parami, "@name");
				string select = ((XdmNode) parami).GetAttributeValue(new QName("select"));
				string tunnelled = ((XdmNode) parami).GetAttributeValue(new QName("tunnel"));
				bool required = tunnel == (tunnelled != null && tunnelled.Equals("yes"));
				XdmValue value;
				if (name == null) {
					System.Console.WriteLine("*** No name for parameter " + i + " in initial-template");
					throw new Exception("*** No name for parameter " + i + " in initial-template");
				}
				try {
					value = xpath.Evaluate(select, null);
					i++;
				} catch (Exception e) {
					System.Console.WriteLine("*** Error evaluating parameter " + name + " in initial-template : " + e.Message);
					throw e;
				}
				if (required) {
					params1.Add(name, value);
				}
			}
			return params1;
		}

		internal XdmValue[] getParameters(XPathCompiler xpath, XdmNode node) {
			List<XdmValue> params1 = new List<XdmValue>();

			int i = 1;
			foreach (XdmItem param in xpath.Evaluate("param[not(@static='yes')]", node)) {
				string select = ((XdmNode) param).GetAttributeValue(new QName("select"));
				XdmValue value;
				try {
					value = xpath.Evaluate(select, null);
					i++;
				} catch (Exception e) {
					System.Console.WriteLine("*** Error evaluating parameter " + i + " in initial-function : " + e.Message);
					throw e;
				}
				params1.Add(value);
			}
			return params1.ToArray();
		}



		internal static QName GetQNameAttribute(XPathCompiler xpath, XdmItem contextItem, string attributePath) {
			string exp = "for $att in " + attributePath +
				" return if (contains($att, ':')) then resolve-QName($att, $att/..) else " +
				" if (contains($att,'{')) then QName(substring-before(substring-after($att,'{'),'}'),substring-after($att,'}')) else" +
				" xs:string($att)";
            XdmAtomicValue qname = null;
            try
            {
               qname = (XdmAtomicValue)xpath.EvaluateSingle(exp, contextItem);
            }
            catch (Exception ex) {
                Trace.WriteLine("Error in GetQNameAttribute : " + ex.Message);
                return null;
            }
            if (qname == null) {
                return null;
            }
            String qnameStr = qname.GetStringValue();
            if (qnameStr.Equals("#unnamed")) {
                return new QName(NamespaceConstant.XSLT, "unnamed");
            } else if (qnameStr.Equals("#default"))
            {
                return new QName(NamespaceConstant.XSLT, "default");
            }
            else if (qnameStr.StartsWith("{"))
            {
                return QName.FromClarkName(qnameStr);
            }
            else if (qnameStr.StartsWith("Q{"))
            {
                return QName.FromEQName(qnameStr);
            }
            if (qnameStr.Contains(":")) {
                QName qqname = new QName(qnameStr, (XdmNode)contextItem);
                return qqname;
            }
			return qname == null ? null : new QName(qname.ToString());
		}

    private bool isSlow(string testName) {
			return testName.StartsWith("regex-classes")||
				testName.Equals("normalize-unicode-008");
    }

        internal void CopySchemaNamespace(Environment env, XPathCompiler testXpc) {
            JConfiguration config = env.xpathCompiler.Processor.Implementation;
            Set ns = config.getImportedNamespaces();
            foreach (String s in ns.toArray()) {
                testXpc.ImportSchemaNamespace(s);

            }


        }


    protected override void runTestCase(XdmNode testCase, XPathCompiler xpath)  {

        TestOutcome outcome = new TestOutcome(this);
        string testName = testCase.GetAttributeValue(new QName("name"));
        string testSetName = testCase.Parent.GetAttributeValue(new QName("name"));

        if (exceptionsMap.ContainsKey(testName)) {
            notrun++;
            resultsDoc.writeTestcaseElement(testName, "notRun", exceptionsMap[testName].GetAttributeValue(new QName("reason")));
            return;
        }

        if (exceptionsMap.ContainsKey(testName) || isSlow(testName)) {
            notrun++;
            resultsDoc.writeTestcaseElement(testName, "notRun", "requires excessive resources");
            return;
        }

       
        


        Environment env = getEnvironment(testCase, xpath);
        if(env == null) {
            resultsDoc.writeTestcaseElement(testName, "notRun", "test catalog error");
            return;
        }

            if (testName.Contains("environment-variable"))
            {
                env.processor.Implementation.setConfigurationProperty(JFeatureKeys.ENVIRONMENT_VARIABLE_RESOLVER, new EnvironmentVariableResolver()); 

            }
        
       // XdmNode testInput = (XdmNode) xpath.EvaluateSingle("test", testCase);
        //XdmNode stylesheet = (XdmNode) xpath.EvaluateSingle("stylesheet", testInput);
		//XdmNode pack = (XdmNode) xpath.EvaluateSingle("package", testInput);

        Step<XdmNode, XdmNode> testSetDependencies = Steps.Root().Then(Steps.Child("test-set").Then(Steps.Child("dependencies")));
        Step<XdmNode, XdmNode> testCaseDependencies = Steps.Child("dependencies");
            

            XdmValue allDependencies = new XdmValue(testCase.Select(testSetDependencies.Cat(testCaseDependencies)));
            XdmNode lastSpecValue = allDependencies.Select(Steps.Path("spec", "@value")).FirstOrDefault();
            String specAtt = lastSpecValue!= null ? lastSpecValue.GetStringValue() : "XSLT10+";
            foreach (XdmNode dep in allDependencies.Select(Steps.Child(Predicates.IsElement()))) {
                if (!EnsureDependencySatisfied((XdmNode)dep, env)) {
                    notrun++;
                    string type = dep.NodeName.LocalName;
                    string value = dep.GetAttributeValue("value");
                    if (value == null) {
                        value = type;
                    } else {
                        value = type + ":" + value;
                    }

                    if ("false".Equals(dep.GetAttributeValue("satisfied"))) {
                        value = "!" + value;
                    }
                    String message = "dependency not satisfied" + value;
                    if (value.StartsWith("feature:")) {

                        message = "requires optional " + value;
                    }
                    resultsDoc.writeTestcaseElement(testName, "notRun", message);
                    return;
                }
            }

            if (env.failedToBuild)
            {
                resultsDoc.writeTestcaseElement(testName, "fail", "unable to build environment");
                //noteFailure(testSetName, testName);
                return;
            }

            if (!env.usable)
            {
                resultsDoc.writeTestcaseElement(testName, "n/a", "environment dependencies not satisfied");
                notrun++;
                return;
            }

            if (testName.Contains("load-xquery-module"))
            {

                ModuleResolver mr = new ModuleResolver(null, this);
                mr.setTestCase(testCase);
                env.processor.QueryResolver = mr;
                /*env.processor.getUnderlyingConfiguration().setModuleURIResolver(
                        (moduleURI, baseURI, locations)-> {
                    File file = queryModules.get(moduleURI);
                    if (file == null)
                    {
                        return null;
                    }
                    try
                    {
                        StreamSource ss = new StreamSource(new FileInputStream(file), baseURI);
                        return new StreamSource[] { ss };
                    }
                    catch (FileNotFoundException e)
                    {
                        throw new XPathException(e);
                    }
                }); */
            }

            XdmNode testInput = testCase.Select(Steps.Child("test")).FirstOrDefault();
            XdmNode stylesheet = testInput.Select(Steps.Child("stylesheet").Where(Predicates.Not(Predicates.AttributeEq("role", "secondary")))).FirstOrDefault();
            XdmNode postureAndSweep = testInput.Select(Steps.Child("posture-and-sweep")).FirstOrDefault();
            XdmNode principalPackage = testInput.Select(Steps.Child("package").Where(Predicates.AttributeEq("role", "principal"))).FirstOrDefault();
            IEnumerable<XdmNode> usedPackages = testInput.Select(Steps.Child("package").Where(Predicates.AttributeEq("role", "secondary")));



            XsltExecutable sheet = env.xsltExecutable;
       //ErrorCollector collector = new ErrorCollector();
		
			ErrorCollector collector = new ErrorCollector(outcome);
			IList<XmlProcessingError> errorList = new List<XmlProcessingError> ();
        XmlUrlResolver res = new XmlUrlResolver();
		string xsltLanguageVersion = specAtt.Contains("XSLT30") || specAtt  .Contains("XSLT20+") ? "3.0" : "2.0";
        if (stylesheet != null) {
               
                env.processor.SetProperty(FeatureKeys.ALLOW_MULTITHREADING, "false");
            XsltCompiler compiler = env.xsltCompiler;
			compiler.SetErrorList(errorList);
            Uri hrefFile = res.ResolveUri(stylesheet.BaseUri, stylesheet.GetAttributeValue(new QName("file")));
            Stream styleSource = new FileStream(hrefFile.AbsolutePath, FileMode.Open, FileAccess.Read);
            compiler.BaseUri = hrefFile;
            compiler.XsltLanguageVersion = (specAtt.Contains("XSLT30") || specAtt.Contains("XSLT20+") ? "3.0" : "2.0");

            IEnumerable<XdmNode> paramsi = testInput.Select(Steps.Child("param").Where(IsTrue("static")));
			foreach (XdmNode param in paramsi) {
					String name = param.GetAttributeValue("name");
					String select = param.GetAttributeValue("select");
					XdmValue value;
					try {
						value = xpath.Evaluate(select, null);
					} catch (Exception e) {
						Console.WriteLine("*** Error evaluating parameter " + name + ": " + e.Message);
						//throw e;
						continue;
					}
					compiler.SetParameter(new QName(name), value);

		    }
                
                    foreach (XdmNode pack in usedPackages)
                    {
                        String fileName2 = pack.GetAttributeValue("file");
                        Uri fileUri = res.ResolveUri(pack.BaseUri, fileName2);
                        Stream styleSource2 = new FileStream(fileUri.AbsolutePath, FileMode.Open, FileAccess.Read);

                        XsltPackage xpack = compiler.CompilePackage(styleSource2);
                        xpack = exportImportPackage(testName, testSetName, outcome, compiler, xpack, collector);
                        compiler.ImportPackage(xpack);
                        // Following needed for dynamic loading of packages using fn:transform()
                        // env.processor.getUnderlyingConfiguration().getDefaultXsltCompilerInfo().getPackageLibrary().addPackage(xpack.getUnderlyingPreparedPackage()); //TODO - check this on .net
                    }
                

            try {
                sheet = exportImport(testName, testSetName, outcome, compiler, sheet, collector, styleSource);  //compiler.Compile(stream);

            } catch(Exception err){
					Console.WriteLine (err.Message);
					//Console.WriteLine(err.StackTrace);
					IEnumerator enumerator = errorList.GetEnumerator();
					bool checkCur = enumerator.MoveNext();
					/*if (checkCur && enumerator.Current != null) {
						outcome.SetException ((Exception)(enumerator.Current));
					} else {
						Console.WriteLine ("Error: Unknown exception thrown");
					}*/
					outcome.SetErrorsReported (errorList);
                    outcome.SetException(err);

                    //outcome.SetErrorsReported(collector.GetErrorCodes);
            }


                string optimizationAssertion;
                optimizationAssertions.TryGetValue(testName, out optimizationAssertion);
                if (optimizationAssertion != null && sheet != null)
                {
                    try
                    {
                        bool ok = AssertOptimization(sheet, optimizationAssertion);
                        if (ok)
                        {
                            Console.WriteLine("Optimization OK: " + optimizationAssertion);
                        }
                        else
                        {
                            outcome.SetComment("Optimization assertion failed");
                            if (strict)
                            {
                               //noteFailure(testSetName, testName);
                                resultsDoc.writeTestcaseElement(testName, "fail", outcome.GetComment());
                            }
                        }
                        Console.WriteLine("Optimization OK: " + optimizationAssertion);
                    }
                    catch (Exception e)
                    {
                        Console.WriteLine("Optimization assertion failed: " + optimizationAssertion);
                    }
                }


                //  compiler.setErrorListener(collector);
            }  else if (principalPackage != null) {
                /*	Uri hrefFile = res.ResolveUri(pack.BaseUri, pack.GetAttributeValue(new QName("file")));
                    Stream stream = new FileStream(hrefFile.AbsolutePath, FileMode.Open, FileAccess.Read);

                    XsltCompiler compiler = env.xsltCompiler;
                    compiler.ErrorList = errorList;
                    compiler.XsltLanguageVersion =  (spec.Contains("XSLT30") || spec.Contains("XSLT20+") ? "3.0" : "2.0");
                    //compiler.setErrorListener(collector);

                    try {
                        XsltPackage xpack = compiler.CompilePackage(stream);
                        sheet = xpack.Link();
                    } catch (Exception err) {
                        Console.WriteLine (err.Message);
                        IEnumerator enumerator = errorList.GetEnumerator ();
                        if (enumerator.MoveNext())
                        {
                            outcome.SetException((Exception)(enumerator.Current));
                            outcome.SetErrorsReported(errorList);
                        }
                    } */

                XsltCompiler compiler = env.xsltCompiler;
                //compiler.setXsltLanguageVersion(xsltLanguageVersion);
                //compiler.ErrorReporter = collector;
                compiler.ClearParameters();
                IEnumerable<XdmNode> paramEnumerable  = testInput.Select(Steps.Child("param").Where(IsTrue("static")));

                foreach(XdmNode itemxx in paramEnumerable) {
                    String name = itemxx.GetAttributeValue("name");
                    String select = itemxx.GetAttributeValue("select");
                    XdmValue value;
                    try
                    {
                        value = xpath.Evaluate(select, null);
                    }
                    catch (Exception e)
                    {
                        Console.WriteLine("*** Error evaluating parameter " + name + ": " + e.Message);
                        throw e;
                    }
                    compiler.SetParameter(new QName(name), value);
                };

                foreach (XdmNode pack in usedPackages)
                {
                    String pfileName = pack.GetAttributeValue("file");
                    Uri pfileUri = res.ResolveUri(testCase.BaseUri, pfileName);
                    Stream pstyleSource = new FileStream(pfileUri.AbsolutePath, FileMode.Open, FileAccess.Read);


                    XsltPackage xpack = compiler.CompilePackage(pstyleSource);
                    compiler.ImportPackage(xpack);
                }
                String fileName = principalPackage.GetAttributeValue("file");
                Uri fileUri = res.ResolveUri(testCase.BaseUri, fileName);
                Stream styleSource = new FileStream(fileUri.AbsolutePath, FileMode.Open, FileAccess.Read);

                try
                {
                    XsltPackage xpack = compiler.CompilePackage(styleSource);
                    sheet = xpack.Link();
                }
                catch (Exception err)
                {
                    println(err.Message);
                    outcome.SetException(err);
                    //outcome.SetErrorsReported(collector.);
                }
               

                sheet = exportImport(testName, testSetName, outcome, compiler, sheet, collector, styleSource);

            }

            if (sheet != null)
            {
                XdmItem contextItem = env.contextItem;
                XdmValue initialMatchSelection = null;
                XdmNode initialMode = (XdmNode)xpath.EvaluateSingle("initial-mode", testInput);
                XdmNode initialFunction = (XdmNode)xpath.EvaluateSingle("initial-function", testInput);
                XdmNode initialTemplate = (XdmNode)xpath.EvaluateSingle("initial-template", testInput);

                QName initialModeName = initialMode == null ? null : GetQNameAttribute(xpath, initialMode, "@name");
                QName initialTemplateName = initialTemplate == null ? null : GetQNameAttribute(xpath, initialTemplate, "@name");
                string baseOutputURI = "file:////" + resultsDir + "/results/output.xml";
                XdmNode outputNode = testInput.Select(Steps.Path("output", "@file")).FirstOrDefault();
                string outputUri = (outputNode != null ? outputNode.GetStringValue() : null);

                if (outputUri != null)
                {
                    if (outputUri.Equals(""))
                    {
                        String testSetUri = testCase.BaseUri.AbsoluteUri;
                        int c = testSetUri.IndexOf("/tests/");
                        outputUri = testSetUri.Substring(c + 7);


                    }
                    baseOutputURI = (new XmlUrlResolver()).ResolveUri(new Uri(baseOutputURI), outputUri).AbsolutePath;
                }
                outcome.BaseOutputUri = new Uri(baseOutputURI);

                if (initialMode != null)
                {
                    XdmItem select = xpath.EvaluateSingle("@select", initialMode);
                    if (select != null)
                    {
                        initialMatchSelection = env.xpathCompiler.Evaluate(select.GetStringValue(), null);
                    }
                }

                if (useXslt30Transformer)
                {
                    try
                    {

                        bool assertsSerial = xpath.Evaluate("result//(assert-serialization|assert-serialization-error|serialization-matches)", testCase).Count > 0;
                        bool resultAsTree = env.outputTree;
                        bool serializationDeclared = env.outputSerialize;
                        XdmNode needsTree = (XdmNode)xpath.EvaluateSingle("output/@tree", testInput);
                        if (needsTree != null)
                        {
                            resultAsTree = needsTree.StringValue.Equals("yes");
                        }
                        XdmNode needsSerialization = (XdmNode)xpath.EvaluateSingle("output/@serialize", testInput);
                        if (needsSerialization != null)
                        {
                            serializationDeclared = needsSerialization.StringValue.Equals("yes");
                        }
                        bool resultSerialized = serializationDeclared || assertsSerial;

                        if (assertsSerial)
                        {
                            String comment = outcome.GetComment();
                            comment = (comment == null ? "" : comment) + "*Serialization " + (serializationDeclared ? "declared* " : "required* ");
                            outcome.SetComment(comment);
                        }


                        Xslt30Transformer transformer = sheet.Load30();
                        transformer.InputXmlResolver = env;
                        if (env.unparsedTextResolver != null)
                        {
                            transformer.GetUnderlyingController.setUnparsedTextURIResolver(env.unparsedTextResolver);
                        }

                        Dictionary<QName, XdmValue> caseGlobalParams = GetNamedParameters(xpath, testInput, false, false);
                        Dictionary<QName, XdmValue> caseStaticParams = GetNamedParameters(xpath, testInput, true, false);
                        Dictionary<QName, XdmValue> globalParams = new Dictionary<QName, XdmValue>(env.params1);

                        foreach (KeyValuePair<QName, XdmValue> entry in caseGlobalParams)
                        {
                            globalParams.Add(entry.Key, entry.Value);

                        }

                        /*foreach(KeyValuePair<QName, XdmValue> entry in caseStaticParams) {
							globalParams.Add(entry.Key, entry.Value);

						}*/


                        transformer.SetStylesheetParameters(globalParams);

                        if (contextItem != null)
                        {
                            transformer.GlobalContextItem = contextItem;
                        }

                        transformer.MessageListener = collector;

                        transformer.BaseOutputURI = baseOutputURI;

                        transformer.MessageListener = new TestOutcome.MessageListener(outcome);

                        XdmValue result = null;

                        TextWriter sw = new StringWriter();

                        Serializer serializer = env.processor.NewSerializer();

                        serializer.SetOutputWriter(sw);
                        //serializer.setOutputProperty(Serializer.Property.OMIT_XML_DECLARATION, "yes");

                        OutputResolver serializingOutput = new OutputResolver(env.processor, outcome, true);
                        net.sf.saxon.Controller controller = transformer.GetUnderlyingController;


                        XmlDestination dest = null;
                        if (resultAsTree && !resultSerialized)
                        {
                            // If we want non-serialized, we need to accumulate any result documents as trees too
                            transformer.ResultDocumentHandler = new ResultDocumentHandler(env.processor, outcome, baseOutputURI, false);


                            dest = new XdmDestination();

                        }
                        else
                        if (resultSerialized)
                        {
                            dest = serializer;
                        }

                        Stream src = null;
                        Uri srcBaseUri = new Uri("http://uri");
                        XdmNode srcNode = null;
                        DocumentBuilder builder2 = env.processor.NewDocumentBuilder();

                        if (env.streamedPath != null)
                        {
                            src = new FileStream(env.streamedPath, FileMode.Open, FileAccess.Read);
                            srcBaseUri = new Uri(env.streamedPath);
                        }
                        else if (env.streamedContent != null)
                        {
                            byte[] byteArray = Encoding.UTF8.GetBytes(env.streamedContent);
                            src = new MemoryStream(byteArray);//, "inlineDoc");
                            builder2.BaseUri = new Uri("http://uri");
                        }
                        else if (initialTemplate == null && contextItem != null)
                        {
                            srcNode = (XdmNode)(contextItem);
                        }

                        if (src != null && !"skip".Equals(env.streamedInputValidation))
                        {
                            Processor processor = (Processor)sheet.Implementation.getUnderlyingCompiledStylesheet().getConfiguration().getProcessor();
                            SchemaValidator validatori = processor.SchemaManager.NewSchemaValidator();
                            //validatori.SetDestination

                        }

                        if (initialMode != null)
                        {
                            QName name = GetQNameAttribute(xpath, initialMode, "@name");
                            try
                            {
                                if (name != null)
                                {
                                    transformer.InitialMode = name;
                                }
                            }
                            catch (Exception e)
                            {
                                if (e.InnerException is net.sf.saxon.trans.XPathException)
                                {
                                    Console.WriteLine(e.Message);
                                    outcome.SetException(e);
                                    //throw new SaxonApiException(e.getCause());
                                }
                                else
                                {
                                    throw e;
                                }
                            }
                        }
                        if (src == null && srcNode == null && initialFunction == null && initialTemplateName == null && initialModeName == null)
                        {
                            initialTemplateName = new QName("xsl", NamespaceConstant.XSLT, "initial-template");
                        }

                        if (initialMode != null || initialTemplate != null)
                        {
                            XdmNode init = (XdmNode)(initialMode == null ? initialTemplate : initialMode);
                            Dictionary<QName, XdmValue> params1 = GetNamedParameters(xpath, init, false, false);
                            Dictionary<QName, XdmValue> tunnelledParams = GetNamedParameters(xpath, init, false, true);
                            if (xsltLanguageVersion.Equals("2.0"))
                            {
                                if (!(params1.Count == 0 && tunnelledParams.Count == 0))
                                {
                                    Console.WriteLine("*** Initial template parameters ignored for XSLT 2.0");
                                }
                            }
                            else
                            {
                                transformer.SetInitialTemplateParameters(params1, false);
                                transformer.SetInitialTemplateParameters(tunnelledParams, true);
                            }
                        }


                        if (initialTemplateName != null)
                        {
                            transformer.GlobalContextItem = contextItem;
                            if (dest == null)
                            {
                                result = transformer.CallTemplate(initialTemplateName);
                            }
                            else
                            {
                                transformer.CallTemplate(initialTemplateName, dest);
                            }
                        }
                        else if (initialFunction != null)
                        {
                            QName name = GetQNameAttribute(xpath, initialFunction, "@name");
                            XdmValue[] params2 = getParameters(xpath, initialFunction);
                            if (dest == null)
                            {
                                result = transformer.CallFunction(name, params2);
                            }
                            else
                            {
                                transformer.CallFunction(name, params2, dest);
                            }
                        }
                        else if (initialMatchSelection != null)
                        {
                            if (dest == null)
                            {
                                result = transformer.ApplyTemplates(initialMatchSelection);
                            }
                            else
                            {
                                transformer.ApplyTemplates(initialMatchSelection, dest);
                            }

                        }
                        else
                        {
                            if (dest == null)
                            {
                                if (src != null)
                                {
                                    result = transformer.ApplyTemplates(src, srcBaseUri);
                                }
                                else
                                {
                                    result = transformer.ApplyTemplates(srcNode);
                                }
                            }
                            else
                            {
                                if (src != null)
                                {
                                    transformer.ApplyTemplates(src, dest);
                                }
                                else
                                {
                                   
                                        transformer.ApplyTemplates(srcNode, dest);
                                        
                                    
                                    }
                            }
                        }

                        //outcome.SetWarningsReported(collector.getFoundWarnings());
                        if (resultAsTree && !resultSerialized)
                        {
                            result = ((XdmDestination)(dest)).XdmNode;
                        }
                        if (resultSerialized)
                        {
                            outcome.SetPrincipalSerializedResult(sw.ToString());
                        }
                        outcome.SetPrincipalResult(result);

                        if (saveResults)
                        {
                            String s = sw.ToString();
                            // If a transform result is entirely xsl:result-document, then result will be null
                            if (!resultSerialized && result != null)
                            {
                                StringWriter sw2 = new StringWriter();
                                Serializer se = env.processor.NewSerializer(sw2);
                                se.SetOutputProperty(Serializer.OMIT_XML_DECLARATION, "yes");
                                env.processor.WriteXdmValue(result, se);
                                se.Close();
                                s = sw2.ToString();
                            }
                            // currently, only save the principal result file in the result directory
                            saveResultsToFile(s, resultsDir + "/results/" + testSetName + "/" + testName + ".out");
                            Dictionary<Uri, TestOutcome.SingleResultDoc> xslResultDocuments = outcome.GetSecondaryResultDocuments();
                            foreach (KeyValuePair<Uri, TestOutcome.SingleResultDoc> entry in xslResultDocuments)
                            {
                                Uri key = entry.Key;
                                String path = key.AbsolutePath;
                                String serialization = outcome.Serialize(env.processor, entry.Value);

                                saveResultsToFile(serialization, path);
                            }
                        }
                    }
                    catch (Exception err)
                    {
                        //if (err.getCause() is XPathException &&
                        //!((XPathException) err.getCause()).hasBeenReported()) {
                        //System.err.println("Unreported ERROR: " + err.getCause());
                        //}
                        outcome.SetException(err);
                        if (collector.getErrorCodes().Count > 0)
                        {
                            outcome.SetErrorQNameReported(collector.getErrorCodes());
                        }
                        //Console.WriteLine(err.StackTrace);
                        /*if(err.getErrorCode() == null) {
                        int b = 3 + 4;  }
                    if(err.getErrorCode() != null)
                        outcome.AddReportedError(err.getErrorCode().getLocalName());
                    } else {
                        outcome.SetErrorsReported(collector.getErrorCodes());
                    }*/
                    } /*catch (Exception err) {
						err.printStackTrace();
						failures++;
						resultsDoc.writeTestcaseElement(testName, "fail", "*** crashed " + err.getClass() + ": " + err.getMessage());
						return;
					}*/
                }
                else
                {

                    try
                    {
                        XsltTransformer transformer = sheet.Load();


                        //transformer.SetURIResolver(env); //TODO
                        if (env.unparsedTextResolver != null)
                        {
                            transformer.Implementation.setUnparsedTextURIResolver(env.unparsedTextResolver);
                        }
                        if (initialTemplate != null)
                        {
                            transformer.InitialTemplate = initialTemplateName;
                        }
                        if (initialMode != null)
                        {
                            transformer.InitialMode = initialModeName;
                        }
                        foreach (XdmItem param in xpath.Evaluate("param", testInput))
                        {
                            string name = ((XdmNode)param).GetAttributeValue(new QName("name"));
                            string select = ((XdmNode)param).GetAttributeValue(new QName("select"));
                            XdmValue value = xpath.Evaluate(select, null);
                            transformer.SetParameter(new QName(name), value);
                        }
                        if (contextItem != null)
                        {
                            transformer.InitialContextNode = (XdmNode)contextItem;
                        }
                        if (env.streamedPath != null)
                        {
                            transformer.SetInputStream(new FileStream(env.streamedPath, FileMode.Open, FileAccess.Read), testCase.BaseUri);
                        }
                        foreach (QName varName in env.params1.Keys)
                        {
                            transformer.SetParameter(varName, env.params1[varName]);
                        }
                        //transformer.setErrorListener(collector);
                        transformer.BaseOutputUri = new Uri(resultsDir + "/results/output.xml");
                        /*transformer.MessageListener = (new MessageListener() {
                            public void message(XdmNode content, bool terminate, SourceLocator locator) {
                                outcome.addXslMessage(content);
                            }
                        });*/


                        // Run the transformation twice, once for serialized results, once for a tree.
                        // TODO: we could be smarter about this and capture both

                        // run with serialization
                        StringWriter sw = new StringWriter();
                        Serializer serializer = env.processor.NewSerializer(sw);
                        transformer.ResultDocumentHandler = new ResultDocumentHandler(env.processor, outcome, testCase.BaseUri.AbsolutePath, true);


                        transformer.Run(serializer);

                        outcome.SetPrincipalSerializedResult(sw.ToString());
                        if (saveResults)
                        {
                            // currently, only save the principal result file
                            saveResultsToFile(sw.ToString(),
                                    resultsDir + "/results/" + testSetName + "/" + testName + ".out");
                            Dictionary<Uri, TestOutcome.SingleResultDoc> xslResultDocument = outcome.GetSecondaryResultDocuments();

                            foreach (KeyValuePair<Uri, TestOutcome.SingleResultDoc> entry in xslResultDocument) {
                                Uri key = entry.Key;
                                if (key != null) {
                                    String path = entry.Key.AbsolutePath;
                                    String serialization = outcome.Serialize(env.processor, entry.Value);
                                    saveResultsToFile(serialization, path);

                                }

                            }

                        }
                        transformer.MessageListener = new TestOutcome.MessageListener(outcome);

                        // run without serialization
                        if (env.streamedPath != null)
                        {
                            transformer.SetInputStream(new FileStream(env.streamedPath, FileMode.Open, FileAccess.Read), testCase.BaseUri);
                        }
                        XdmDestination destination = new XdmDestination();
                        transformer.ResultDocumentHandler = new ResultDocumentHandler(env.processor, outcome, testCase.BaseUri.AbsolutePath, false);
                        transformer.Run(destination);

                        //transformer. .transform();
                        outcome.SetPrincipalResult(destination.XdmNode);
                        //}
                    }
                    catch (Exception err)
                    {
                        outcome.SetException(err);
                        //outcome.SetErrorsReported(collector.getErrorCodes());
                        // err.printStackTrace();
                        // failures++;
                        //resultsDoc.writeTestcaseElement(testName, "fail", "*** crashed " + err.Message);
                        //return;
                    }
                }
            }
            XdmNode assertion = (XdmNode) xpath.EvaluateSingle("result/*", testCase); //testCase.Select(PathFromList(new List<>Child("result"), Child().Where(IsElement())));
            if (assertion == null) {
            failures++;
            resultsDoc.writeTestcaseElement(testName, "fail", "No test assertions found");
            return;
        }
        XPathCompiler assertionXPath = env.processor.NewXPathCompiler();
            //assertionXPath.setLanguageVersion("3.0");
            CopySchemaNamespace(env, assertionXPath);
        bool success = outcome.TestAssertion(outcome, assertion, outcome.GetPrincipalResultDoc(), assertionXPath, xpath, debug);
        if (success) {
            if (outcome.GetWrongErrorMessage() != null) {
                outcome.SetComment(outcome.GetWrongErrorMessage());
                wrongErrorResults++;
            } else {
                successes++;
            }
            resultsDoc.writeTestcaseElement(testName, "pass", outcome.GetComment());
        } else {
            failures++;
            resultsDoc.writeTestcaseElement(testName, "fail", outcome.GetComment());
        }
    
}

        private bool AssertOptimization(XsltExecutable stylesheet, string assertion)
        {
            XdmDestination builder = new XdmDestination();
            stylesheet.Explain(builder);
            builder.Close();
            XdmNode expressionTree = builder.XdmNode;
            XPathCompiler xpe = stylesheet.Processor.NewXPathCompiler();
            XPathSelector exp = xpe.Compile(assertion).Load();
            exp.ContextItem = expressionTree;
            XdmAtomicValue bv = (XdmAtomicValue)exp.EvaluateSingle();
            if (bv == null || !bv.GetBooleanValue())
            {
                println("** Optimization assertion failed");
                println(expressionTree.StringValue);
            }
            return bv != null && bv.GetBooleanValue();
        }

        protected XsltExecutable exportImport(string testName, string testSetName, TestOutcome outcome, XsltCompiler compiler, XsltExecutable sheet, ErrorCollector collector, Stream styleSource)
        {
            try
            {
                if (export && IsExportable(testName))
                {
                    sheet = ExportStylesheet(testName, testSetName, compiler, sheet, styleSource);
                }
                else if (sheet == null)
                {
                    sheet = compiler.Compile(styleSource);
                }
            }

            catch (Exception err)
            {
                //err.printStackTrace();
                Console.WriteLine(err.Message);
                outcome.SetException(err);
                //outcome.setErrorsReported(collector.getErrorCodes());
            }
            return sheet;
        }

        private XsltExecutable ExportStylesheet(string testName, string testSetName, XsltCompiler compiler, XsltExecutable sheet, Stream styleSource)
        {
            try
            {
                Uri fileUri = new Uri(resultsDir + "/export/" + testSetName + "/" + testName + ".sef");
                Stream exportFile = new FileStream(fileUri.AbsolutePath, FileMode.Open, FileAccess.Read);
               
                XsltPackage compiledPack = compiler.CompilePackage(styleSource);
                compiledPack.Save(exportFile);
                
                sheet = ReloadExportedStylesheet(compiler,fileUri);
            }
            catch (Exception e)
            {
               // compiler.GetErrorList().Add(new StaticError(e));
                //System.err.println(e.getMessage());
                //e.printStackTrace();  //temporary, for debugging
                throw e;
            }
            return sheet;
        }

        private XsltExecutable ReloadExportedStylesheet(XsltCompiler compiler, Uri exportFile)
        {
            return compiler.LoadExecutablePackage(exportFile);
        }

        private bool IsExportable(string testName)
        {
            return !(testName.Equals("load-xquery-module-004") || testName.Equals("transform-004"));
        }

        private XsltPackage exportImportPackage(string testName, string testSetName, TestOutcome outcome, XsltCompiler compiler, XsltPackage pack, ErrorCollector collector)
        {
            try
            {
                if (export)
                {
                    try
                    {
                        Uri fileUri = new Uri(resultsDir + "/export/" + testSetName + "/" + testName + ".base.sef");
                        Stream exportFile = new FileStream(fileUri.AbsolutePath, FileMode.Open, FileAccess.Read);

                        Console.WriteLine("Exported package to " + fileUri);
                        pack.Save(exportFile);
                        return compiler.LoadLibraryPackage(fileUri);
                    }
                    catch (Exception e)
                    {
                        Console.WriteLine(e.Message);
                        //e.printStackTrace();  //temporary, for debugging
                        throw e;
                    }
                }
                else
                {
                    return pack;
                }
            }
            catch (Exception err)
            {
                outcome.SetException(err);
               
              //  outcome.SetErrorsReported(collector.getErrorCodes());
            }
           /* catch (Exception err)
            {
                err.printStackTrace();
                System.err.println(err.getMessage());
                outcome.setException(new QName(err.getClass().getName()));
                outcome.setErrorsReported(collector.getErrorCodes());
            }*/
            return pack;
        }

public class EnvironmentVariableResolver : net.sf.saxon.lib.EnvironmentVariableResolver
{

    public EnvironmentVariableResolver() { }

    public Set getAvailableEnvironmentVariables()
    {
        Set strings = new HashSet();
        strings.add("QTTEST");
        strings.add("QTTEST2");
        strings.add("QTTESTEMPTY");
        return strings;
    }

    public string getEnvironmentVariable(string name)
    {
        if (name.Equals("QTTEST"))
        {
            return "42";
        }
        else if (name.Equals("QTTEST2"))
        {
            return "other";
        }
        else if (name.Equals("QTTESTEMPTY"))
        {
            return "";
        }
        else
        {
            return null;
        }
    }


}

public class ResultDocumentHandler : IResultDocumentHandler
        {

            private Processor proc;
            private TestOutcome outcome;
            private Uri baseOutputUri;
            bool serialized;
            public ResultDocumentHandler(Processor processor, TestOutcome outcome, String baseOutputURI, bool serialized) {
                this.proc = processor;
                this.outcome = outcome;
                try {
                    this.baseOutputUri = new Uri(baseOutputURI);
                }
                catch (Exception e) { }
                this.serialized = serialized;

            }
            public XmlDestination HandleResultDocument(string href, Uri baseUri)
            {
               // if (serialized) {

                    StringWriter writer = new StringWriter();
                    Serializer dest = proc.NewSerializer();
                   
                    XmlUrlResolver res = new XmlUrlResolver();
                    Uri uri = res.ResolveUri(baseUri, href);
                    dest.BaseUri = uri;
                dest.SetOutputFile(uri.AbsolutePath);

                if (uri.Equals(baseOutputUri))
                    {

                        outcome.SetPrincipalSerializedResult(writer.ToString());
                    }
                    else
                    {
                        outcome.SetSecondaryResult(uri, null, writer.ToString(), proc);
                    }
                    return dest;
                /*   }
                   else {

                       XdmDestination dest = new XdmDestination();
                       XmlUrlResolver res = new XmlUrlResolver();
                       Uri uri = res.ResolveUri(baseUri, href);
                       dest.BaseUri = uri;
                    
                    XdmValue doc = proc.NewXPathCompiler().Compile("doc('" + uri + "')").Load().EvaluateSingle();
                    if (uri.Equals(baseOutputUri))
                       {

                           outcome.SetPrincipalResult(null);
                       }
                       else
                       {
                        
                          outcome.SetSecondaryResult(uri, null ,null, proc);
                       }
                       return dest;

                   }*/
            }
        }




        internal bool mustSerialize(XdmNode testCase, XPathCompiler xpath) {
        return saveResults ||
                ((XdmAtomicValue) xpath.EvaluateSingle(
                "exists(./result//(assert-serialization-error|serialization-matches|assert-serialization)[not(parent::*[self::assert-message|self::assert-result-document])])", testCase)).GetBooleanValue();
    }

    private void saveResultsToFile(string content, string filePath) {
        try {
            System.IO.File.WriteAllText(filePath, content);
        } catch (IOException e) {
            println("*** Failed to save results to " + filePath);
            throw e;
        }
    }


    public class ResetAction {

            Environment env = null;
            decimal oldVersion;

            ResetAction(Environment envi, decimal oldVer) {
                oldVersion = oldVer;
                env = envi;
            }
        public void reset(Environment env)
        {
            env.processor.XmlVersion = oldVersion;
        }
    }



        public override bool EnsureDependencySatisfied(XdmNode dependency, Environment env) {
            string type = dependency.NodeName.LocalName;
            string value = dependency.GetAttributeValue("value");
            if (value == null) {
                value = "*";
            }
            String tv = type + "/" + value;
            bool inverse = "false".Equals(dependency.GetAttributeValue("satisfied"));
            bool needed = !"false".Equals(dependency.GetAttributeValue("satisfied"));

            if (alwaysOn.Contains(type) || alwaysOn.Contains(tv)) {
                return needed;
            }

            if (alwaysOff.Contains(type) || alwaysOff.Contains(tv))
            {
                return !needed;
            }

            String edition = env.processor.Edition;

            if (needsPE.Contains(type) || needsPE.Contains(tv)) {

                return (edition.Equals("PE") || edition.Equals("EE")) == needed;
            }

            if (needsEE.Contains(type) || needsEE.Contains(tv)) {
                return edition.Equals("EE") == needed;
            }
            


            switch (type)
            {
                case "spec":

                    bool atLeast = value.EndsWith("+");
                    value = value.Replace("+", "");

                    String specName = ((SpecAttr)spec.GetAttr()).svname.Replace("XT", "XSLT");
                    int order = value.CompareTo(specName);
                    return atLeast ? order <= 0 : order == 0;

                case "feature":
                    switch (value)
                    {
                        case "XML_1.1":
                            {
                                decimal requiredVersion = (decimal)(inverse ? 1.0 : 1.1);
                                decimal oldVersion = env.processor.XmlVersion;
                                env.resetActions.Add(envi =>
                                {
                                    envi.processor.XmlVersion = oldVersion;
                                }
                            );
                                env.processor.XmlVersion = requiredVersion;
                                return true;
                            }

                        case "XSD_1.1":
                            {
                                String requiredVersion = (inverse ? "1.0" : "1.1");
                                String oldVersion = env.processor.GetProperty(Saxon.Api.Feature<string>.XSD_VERSION);
                                if (!oldVersion.Equals(requiredVersion))
                                {
                                    env.processor.SetProperty(Saxon.Api.Feature<string>.XSD_VERSION, requiredVersion);
                                    env.resetActions.Add(envi =>
                                    {
                                        envi.processor.SetProperty(Saxon.Api.Feature<String>.XSD_VERSION, oldVersion);

                                    });
                                }
                                return true;
                            }

                        case "higher_order_functions":
                            return !inverse;
                        case "simple-uca-fallback":
                            return !inverse;
                        case "advanced-uca-fallback":
                            return (edition.Equals("PE") || edition.Equals("EE")) ^ inverse;
                        case "streaming-fallback":
                            bool requiredi = !inverse;
                            bool old = (bool)env.processor.GetProperty(Feature<bool>.STREAMING_FALLBACK);
                            if (old != requiredi)
                            {
                                env.processor.SetProperty(Feature<bool>.STREAMING_FALLBACK, requiredi);
                                env.resetActions.Add(envi =>
                                {
                                    env.processor.SetProperty(Feature<bool>.STREAMING_FALLBACK, old);
                                });
                            }
                            return true;
                    

                    default:
                        Console.WriteLine("*** Unknown feature in HE: " + value);
                        return env.processor.Edition.Equals("HE") ? false : true; //in java the true is null. Check logic on .net
                    
            }


            
             case "default_language_for_numbering": {
                String old = env.processor.GetProperty(Feature<string>.DEFAULT_LANGUAGE);
                if (!value.Equals(old)) {
                    env.processor.SetProperty(Feature<string>.DEFAULT_LANGUAGE, value);
                    env.resetActions.Add(envi => 
                        {
                            env.processor.SetProperty(Feature<string>.DEFAULT_LANGUAGE, old);
                        }
                    );
                }
                return true;
            }
            case "enable_assertions": {
                bool on = !inverse;
                bool old = env.xsltCompiler.AssertionsEnabled;
                env.xsltCompiler.AssertionsEnabled = on;
                env.resetActions.Add(envi =>
                {
                    env.xsltCompiler.AssertionsEnabled = old;
                });
                return true;
            }
            case "extension-function":
               /* if (value.Equals("Q{http://relaxng.org/ns/structure/1.0}schema-report#1")) {
                    try {
                        Configuration config = env.processor.getUnderlyingConfiguration();
Object sf = config.getInstance("net.cfoster.saxonjing.SchemaFunction", null);
env.processor.registerExtensionFunction((ExtensionFunctionDefinition) sf);
                        Object sfd = config.getInstance("net.cfoster.saxonjing.SchemaReportFunction", null);
env.processor.registerExtensionFunction((ExtensionFunctionDefinition) sfd);
                        return true;
                    } catch (XPathException err) {
                        System.err.println("Failed to load Saxon-Jing extension functions");
                        return false;
                    }
                }*/
                return false;
            case "year_component_values":
                if ("support year zero".Equals(value)) {
                    if (env != null) {
                        env.processor.SetProperty(Feature<string>.XSD_VERSION, inverse? "1.0" : "1.1");
                        return true;
                    } else {
                        return false;
                    }
                }
                return !inverse;
            case "additional_normalization_form":
                if ("support FULLY-NORMALIZED".Equals(value)) {
                    return inverse;
                }
                return !inverse;

            case "on-multiple-match":
                throw new Exception("on-multiple-match is no longer recognized");
//                env.resetActions.add(new Environment.ResetAction() {
//                    @Override
//                    public void reset(Environment env) {
//                        env.xsltCompiler.getUnderlyingCompilerInfo().setRecoveryPolicy(Mode.RECOVER_WITH_WARNINGS);
//                    }
//                });
//                if ("error".Equals(value)) {
//                    env.xsltCompiler.getUnderlyingCompilerInfo().setRecoveryPolicy(Mode.DO_NOT_RECOVER);
//                } else {
//                    env.xsltCompiler.getUnderlyingCompilerInfo().setRecoveryPolicy(Mode.RECOVER_SILENTLY);
//                }
//                return true;

            case "ignore_doc_failure":
                return inverse;

            case "combinations_for_numbering":
                if (value.Equals("COPTIC EPACT DIGIT ONE") || value.Equals("SINHALA ARCHAIC DIGIT ONE") || value.Equals("MENDE KIKAKUI DIGIT ONE")) {
                    return false;
                }
                return !inverse;

            case "xsd-version":
                return env.processor.Edition.Equals("HE") ? false : true;

            case "sweep_and_posture":
                return env.processor.Edition.Equals("HE") ? inverse : true;

            case "unicode-version":
                return value.Equals("6.0"); // Avoid running Unicode 9.0 tests - they are slow!

            case "default_html_version": {
                return value.Equals("5");
            }

            default:
                println("**** dependency not recognized for HE: " + type);
                return false;
        }



            if ("spec".Equals(type))
            {
                bool atLeast = value.EndsWith("+");
                value = value.Replace("+", "");

                String specName = ((SpecAttr)spec.GetAttr()).svname.Replace("XT", "XSLT");
                int order = value.CompareTo(specName);
                return atLeast ? order <= 0 : order == 0;
            }
            else if ("feature".Equals(type))
            {
                //            <xs:enumeration value="backwards_compatibility" />
                //            <xs:enumeration value="disabling_output_escaping" />
                //            <xs:enumeration value="schema_aware" />
                //            <xs:enumeration value="namespace_axis" />
                //            <xs:enumeration value="streaming" />
                //            <xs:enumeration value="XML_1.1" />

                if ("XML_1.1".Equals(value) && !inverse)
                {
                    if (env != null)
                    {
                        env.processor.XmlVersion = (decimal)1.1;
                        return true;
                    }
                    else
                    {
                        return false;
                    }
                }
                else if ("disabling_output_escaping".Equals(value))
                {
                    return !inverse;
                }
                else if ("schema_aware".Equals(value))
                {
                    if (!env.xsltCompiler.SchemaAware)
                    {
                        return false; // cannot use the selected tree model for schema-aware tests
                    }
                    if (env != null)
                    {
                        env.xsltCompiler.SchemaAware = !inverse;
                    }
                    return true;
                }
                else if ("namespace_axis".Equals(value))
                {
                    return !inverse;
                }
                else if ("streaming".Equals(value))
                {
                    return !inverse;
                }
                else if ("backwards_compatibility".Equals(value))
                {
                    return !inverse;
                }
                return false;
            }
            else if ("xsd-version".Equals(type))
            {
                if ("1.1".Equals(value))
                {
                    if (env != null)
                    {
                        env.processor.SetProperty(JFeatureKeys.XSD_VERSION, (inverse ? "1.0" : "1.1"));
                    }
                    else
                    {
                        return false;
                    }
                }
                else if ("1.0".Equals(value))
                {
                    if (env != null)
                    {
                        env.processor.SetProperty(JFeatureKeys.XSD_VERSION, (inverse ? "1.1" : "1.0"));
                    }
                    else
                    {
                        return false;
                    }
                }
                return true;
            }
            else if ("available_documents".Equals(type))
            {
                return !inverse;
            }
            else if ("default_language_for_numbering".Equals(type))
            {
                return !inverse;
            }
            else if ("languages_for_numbering".Equals(type))
            {
                return !inverse;
            }
            else if ("supported_calendars_in_date_formatting_functions".Equals(type))
            {
                return !inverse;
            }
            else if ("default_calendar_in_date_formatting_functions".Equals(type))
            {
                return !inverse;
            }
            else if ("maximum_number_of_decimal_digits".Equals(type))
            {
                return !inverse;
                //        } else if ("collation_uri".Equals(type)) {
                //            return !inverse;
                //        } else if ("statically_known_collations".Equals(type)) {
                //            if (value.Equals("http://www.w3.org/xslts/collation/caseblind") && !inverse) {
                //                env.processor.getUnderlyingConfiguration().setCollationURIResolver(
                //                        new StandardCollationURIResolver() {
                //                            public stringCollator resolve(string uri, string base, Configuration config) {
                //                                if ("http://www.w3.org/xslts/collation/caseblind".Equals(uri)) {
                //                                    return super.resolve("http://saxon.sf.net/collation?ignore-case=yes", "", config);
                //                                } else {
                //                                    return super.resolve(uri, base, config);
                //                                }
                //                            }
                //                        }
                //                );
                //            }
                //            // Alternative case-blind collation URI used in QT3 tests
                //            if (value.Equals("http://www.w3.org/2010/09/qt-fots-catalog/collation/caseblind") && !inverse) {
                //                env.processor.getUnderlyingConfiguration().setCollationURIResolver(
                //                        new StandardCollationURIResolver() {
                //                            public stringCollator resolve(string uri, string base, Configuration config) {
                //                                if ("http://www.w3.org/2010/09/qt-fots-catalog/collation/caseblind".Equals(uri)) {
                //                                    return super.resolve("http://saxon.sf.net/collation?ignore-case=yes", "", config);
                //                                } else {
                //                                    return super.resolve(uri, base, config);
                //                                }
                //                            }
                //                        }
                //                );
                //            }
                //            return true;
            }
            else if ("default_output_encoding".Equals(type))
            {
                return !inverse;
            }
            else if ("unparsed_text_encoding".Equals(type))
            {
                return !inverse;
            }
            else if ("additional_normalization_form".Equals(type))
            {
                return !inverse;
            }
            else if ("recognize_id_as_uri_fragment".Equals(type))
            {
                return !inverse;
            }
            else if ("on-multiple-match".Equals(type))
            {
                throw new Exception("on-muplte-match is no longer recognized");
                /*if (value.Equals("error"))
                {
                    env.xsltCompiler.GetUnderlyingCompilerInfo().setRecoveryPolicy((int)RecoveryPolicy.DoNotRecover);
                }
                else
                {
                    env.xsltCompiler.GetUnderlyingCompilerInfo().setRecoveryPolicy((int)RecoveryPolicy.RecoverSilently);
                }
                return true; */
            }
            else if ("ignore_doc_failure".Equals(type))
            {
                return inverse;
            }
            else if ("combinations_for_numbering".Equals(type))
            {
                if (value.Equals("COPTIC EPACT DIGIT ONE") || value.Equals("SINHALA ARCHAIC DIGIT ONE") || value.Equals("MENDE KIKAKUI DIGIT ONE")) {
                    return false;
                }
                return !inverse;
            }
            else if ("higher_order_functions".Equals(type))
            {
                return !inverse;

            }
            else if ("ignore_doc_failure".Equals(type))
            {
                //TODO
                return false;

            }
            else if ("simple-uca-fallback".Equals(type))
            {
                return !inverse;

            }
            else if ("advanced-uca-fallback".Equals(type))
            {
                return !inverse;

            }
            else if ("streaming-fallback".Equals(type))
            {
                bool required = !inverse;
                bool old = env.processor.GetProperty(FeatureKeys.STREAMING_FALLBACK) == "true" || env.processor.GetProperty(FeatureKeys.STREAMING_FALLBACK) == "on";
                if (old != required)
                {
                    env.processor.SetProperty<bool>(Feature<bool>.STREAMING_FALLBACK, required);
                }

                return true;
            }
            else if ("sweep_and_posture".Equals(type))
            {
                return !inverse;
            }
            else if ("unicode_version".Equals(type))
            {
                return value.Equals("6.0");
            }
            else if ("enable_assertions".Equals(type))
            {
                return false;
            }
            else if ("year_component_values".Equals(type))
            {
                if ("support year zero".Equals(value)) {
                    if (env != null)
                    {
                        env.processor.SetProperty<String>(Feature<String>.XSD_VERSION, inverse ? "1.0" : "1.1");
                    }
                    else {
                        return false;
                    }

                }
                return !inverse;

            }
            else
            {
                println("**** dependency not recognized: " + type);
                return false;
            }
    }

    /*private static string getCanonicalPath(File file) {
        try {
            return file.getCanonicalPath();
        } catch (IOException err) {
            return file.getAbsolutePath();
        }
    }*/

    private static QName getQNameAttribute(XPathCompiler xpath, XdmItem contextItem, string attributePath)  {
        string exp = "for $att in " + attributePath +
                " return if (contains($att, ':')) then resolve-QName($att, $att/..) else QName('', $att)";
        XdmAtomicValue qname = (XdmAtomicValue) xpath.EvaluateSingle(exp, contextItem);
        return (qname == null ? null : (QName) qname.Value);
    }

    internal class OutputResolver : net.sf.saxon.lib.OutputURIResolver{

        private Processor proc;
        private TestOutcome outcome;
        private XdmDestination destination;
        private java.io.StringWriter stringWriter;
        bool serialized;
        Uri uri;

        public OutputResolver(Processor proc, TestOutcome outcome, bool serialized) {
            this.proc = proc;
            this.outcome = outcome;
            this.serialized = serialized;
        }

        public net.sf.saxon.lib.OutputURIResolver newInstance()
        {
            return new OutputResolver(proc, outcome, serialized);
        }
         XmlUrlResolver res = new XmlUrlResolver();
         public JResult resolve(string href, string base1)
         {
            try {
                uri = res.ResolveUri(new Uri(base1), href);
                if (serialized) {
                  
                    stringWriter = new java.io.StringWriter();
                    javax.xml.transform.stream.StreamResult result =  new javax.xml.transform.stream.StreamResult(stringWriter);
                    result.setSystemId(uri.ToString());
                    return result; 
                } else {
                    destination = new XdmDestination();
                    ((XdmDestination)destination).BaseUri = uri;
                        return null;//  destination..GetReceiver(proc.Implementation.makePipelineConfiguration());
                }
            } catch (Exception e) {
                throw e;
            }
        }

        public void close(JResult result) {
            if (serialized) {
                outcome.SetSecondaryResult(uri, null, stringWriter.ToString(), proc);
            } else {
                XdmDestination xdm = (XdmDestination)destination;
                outcome.SetSecondaryResult(xdm.BaseUri, xdm.XdmNode, null, proc);
            }
        }

    }



}

    public class ModuleResolver : IQueryResolver
    {

        XQueryCompiler catXQC;
        XdmNode testCase;
        TestDriver driver = null;

        public ModuleResolver(XQueryCompiler xqc, TestDriver driveri)
        {
            this.catXQC = xqc;
            driver = driveri;
        }

        public void setTestCase(XdmNode testCase)
        {
            this.testCase = testCase;
        }


        public Object GetEntity(Uri absoluteUri)
        {
            FileStream stream = null;
            try
            {
                stream =  new FileStream(driver.queryModules[absoluteUri], FileMode.Open, FileAccess.Read);
            }
            catch (Exception ex) {
                return null;
            }
            return stream;
        }

        internal Uri resolve(Uri basei, String child)
        {

            return (new XmlUrlResolver()).ResolveUri(testCase.BaseUri, child);


        }


        public Uri[] GetModules(String moduleUri, Uri baseUri, String[] locationHints)
        {
            //  XdmValue files = catXPC.Evaluate("./module[@uri='" + moduleUri + "']/@file/string()", testCase);
            /*if (files.Count == 0)
            {
                throw new Exception("Failed to find module entry for " + moduleUri);
            }*/
            XmlUrlResolver res = new XmlUrlResolver();
            
            Uri[] fullPaths = new Uri[1];

            try
            {
                fullPaths[0] = res.ResolveUri(baseUri, moduleUri);
            }
            catch (Exception e) {
                return null;
            }

            return fullPaths;
        }

        
    }
}
