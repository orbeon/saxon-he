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

namespace TestRunner
{
/**
 * This class runs the W3C XSLT Test Suite, driven from the test catalog.
 */
public class Xslt30TestSuiteDriver : TestDriver {

    public static void main(string[] args) {
        if (args.Length == 0 || args[0].Equals("-?")) {
            System.Console.WriteLine("java com.saxonica.testdriver.Xslt30TestSuiteDriver testsuiteDir catalog [-o:resultsdir] [-s:testSetName]" +
                    " [-t:testNamePattern] [-bytecode:on|off|debug] [-tree] [-lang] [-save]");
        }

        System.Console.WriteLine("Testing Saxon " + (new Processor()).ProductVersion);
        new Xslt30TestSuiteDriver().go(args);
    }


    
    public override string catalogNamespace() {
        return "http://www.w3.org/2012/10/xslt-test-catalog";
    }

    public void writeResultFilePreamble(Processor processor, XdmNode catalog) {
        resultsDoc = new Xslt30ResultsDocument(this.resultsDir, Spec.XT30);
        //super.writeResultFilePreamble(processor, catalog);
    }

    
    public override void processSpec(string specStr) {
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
        foreach (XdmItem env in xpc.Evaluate("//environment", catalog)) {
            environment = Environment.processEnvironment(this, xpc, env, globalEnvironments, defaultEnv);
        }
        //buildDependencyMap(driverProc, environment);
    }

    private bool isSlow(string testName) {
        return testName.Equals("normalize-unicode-008");
    }


    protected override void runTestCase(XdmNode testCase, XPathCompiler xpath)  {

        TestOutcome outcome = new TestOutcome(this);
        string testName = testCase.GetAttributeValue(new QName("name"));
        string testSetName = testCase.Parent.GetAttributeValue(new QName("name"));
        ////
        if (testName.Equals("type-0174"))
        {
            int num = 0;
            System.Console.WriteLine("Test driver" + num);

        }

        ///
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

       

        XdmValue specAtt = (XdmValue)(xpath.EvaluateSingle("(/test-set/dependencies/spec/@value, ./dependencies/spec/@value)[last()]", testCase));
        string spec = specAtt.ToString();

        Environment env = getEnvironment(testCase, xpath);
        if(env == null) {
            resultsDoc.writeTestcaseElement(testName, "notRun", "test catalog error");
            return;
        }

        /*if(testName("environment-variable")) {
                        EnvironmentVariableResolver resolver = new EnvironmentVariableResolver() {
                    public Set<string> getAvailableEnvironmentVariables() {
                        Set<string> strings = new HashSet<string>();
                        strings.add("QTTEST");
                        strings.add("QTTEST2");
                        strings.add("QTTESTEMPTY");
                        return strings;
                    }

                    public string getEnvironmentVariable(string name) {
                        if (name.Equals("QTTEST")) {
                            return "42";
                        } else if (name.Equals("QTTEST2")) {
                            return "other";
                        } else if (name.Equals("QTTESTEMPTY")) {
                            return "";
                        } else {
                            return null;
                        }
                    }
                }; //TODO
            } */
         //   env.processor.SetProperty(JFeatureKeys.ENVIRONMENT_VARIABLE_RESOLVER, resolver); //TODO
        
        XdmNode testInput = (XdmNode) xpath.EvaluateSingle("test", testCase);
        XdmNode stylesheet = (XdmNode) xpath.EvaluateSingle("stylesheet", testInput);


        foreach (XdmItem dep in xpath.Evaluate("(/test-set/dependencies/*, ./dependencies/*)", testCase)) {
            if (!dependencyIsSatisfied((XdmNode)dep, env)) {
                notrun++;
                resultsDoc.writeTestcaseElement(testName, "notRun", "dependency not satisfied");
                return;
            }
        }

        XsltExecutable sheet = env.xsltExecutable;
       //ErrorCollector collector = new ErrorCollector();
        XmlUrlResolver res = new XmlUrlResolver();
        if (stylesheet != null) {
            XsltCompiler compiler = env.xsltCompiler;
            Uri hrefFile = res.ResolveUri(stylesheet.BaseUri, stylesheet.GetAttributeValue(new QName("file")));
            Stream stream = new FileStream(hrefFile.AbsolutePath, FileMode.Open, FileAccess.Read);
            compiler.BaseUri = hrefFile;
            compiler.XsltLanguageVersion = (spec.Contains("XSLT30") || spec.Contains("XSLT20+") ? "3.0" : "2.0");
            try
            {
                sheet = compiler.Compile(stream);
            } catch(Exception err){
                outcome.SetException(err);
                
                //outcome.SetErrorsReported(collector.GetErrorCodes);
            }

           
          //  compiler.setErrorListener(collector);
        }

        if (sheet != null) {
            XdmItem contextItem = env.contextItem;
            QName initialMode = getQNameAttribute(xpath, testInput, "initial-mode/@name");
            QName initialTemplate = getQNameAttribute(xpath, testInput, "initial-template/@name");

            try {
                XsltTransformer transformer = sheet.Load();
                
                //transformer.SetURIResolver(env); //TODO
                if (env.unparsedTextResolver != null) {
                    transformer.Implementation.setUnparsedTextURIResolver(env.unparsedTextResolver);
                }
                if (initialTemplate != null) {
                    transformer.InitialTemplate = initialTemplate;
                }
                if (initialMode != null) {
                    transformer.InitialMode = initialMode;
                }
                foreach (XdmItem param in xpath.Evaluate("param", testInput)) {
                    string name = ((XdmNode)param).GetAttributeValue(new QName("name"));
                    string select = ((XdmNode) param).GetAttributeValue(new QName("select"));
                    XdmValue value = xpath.Evaluate(select, null);
                    transformer.SetParameter(new QName(name), value);
                }
                if (contextItem != null) {
                    transformer.InitialContextNode = (XdmNode)contextItem;
                }
                if (env.streamedPath != null) {
                    transformer.SetInputStream(new FileStream(env.streamedPath, FileMode.Open, FileAccess.Read), testCase.BaseUri);
                }
                foreach (QName varName in env.params1.Keys) {
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
                transformer.Implementation.setOutputURIResolver(new OutputResolver(driverProc, outcome, true));

            
                transformer.Run(serializer);
               
                outcome.SetPrincipalSerializedResult(sw.ToString());
                if (saveResults) {
                    // currently, only save the principal result file
                    saveResultsToFile(sw.ToString(),
                            resultsDir + "/results/" + testSetName + "/" + testName + ".out");
                }
                transformer.MessageListener = new TestOutcome.MessageListener(outcome);

                // run without serialization
                if (env.streamedPath != null)
                {
                    transformer.SetInputStream(new FileStream(env.streamedPath, FileMode.Open, FileAccess.Read), testCase.BaseUri); 
                }
                XdmDestination destination = new XdmDestination();
                 transformer.Implementation.setOutputURIResolver(
                    new OutputResolver(env.processor, outcome, false));
                transformer.Run(destination);
               
                //transformer. .transform();
                outcome.SetPrincipalResult(destination.XdmNode);
                //}
            } catch (Exception err) {
                outcome.SetException(err);
                //outcome.SetErrorsReported(collector.getErrorCodes());
               // err.printStackTrace();
               // failures++;
                //resultsDoc.writeTestcaseElement(testName, "fail", "*** crashed " + err.Message);
                //return;
            }
        }
        XdmNode assertion = (XdmNode) xpath.EvaluateSingle("result/*", testCase);
        if (assertion == null) {
            failures++;
            resultsDoc.writeTestcaseElement(testName, "fail", "No test assertions found");
            return;
        }
        XPathCompiler assertionXPath = env.processor.NewXPathCompiler();
        //assertionXPath.setLanguageVersion("3.0");
        bool success = outcome.TestAssertion(assertion, outcome.GetPrincipalResultDoc(), assertionXPath, xpath, debug);
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


   

    private bool mustSerialize(XdmNode testCase, XPathCompiler xpath) {
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


    
    public override bool dependencyIsSatisfied(XdmNode dependency, Environment env) {
        string type = dependency.NodeName.LocalName;
        string value = dependency.GetAttributeValue(new QName("value"));
        bool inverse = "false".Equals(dependency.GetAttributeValue(new QName("satisfied")));
        if ("spec".Equals(type)) {
            return true;
        } else if ("feature".Equals(type)) {
//            <xs:enumeration value="backwards_compatibility" />
//            <xs:enumeration value="disabling_output_escaping" />
//            <xs:enumeration value="schema_aware" />
//            <xs:enumeration value="namespace_axis" />
//            <xs:enumeration value="streaming" />
//            <xs:enumeration value="XML_1.1" />

            if ("XML_1.1".Equals(value) && !inverse) {
                if (env != null) {
                    env.processor.XmlVersion = (decimal)1.1;
                    return true;
                } else {
                    return false;
                }
            } else if ("disabling_output_escaping".Equals(value)) {
                return !inverse;
            } else if ("schema_aware".Equals(value)) {
                if (!driverProc.IsSchemaAware) {
                    return false; // cannot use the selected tree model for schema-aware tests
                }
                if (env != null) {
                    env.xsltCompiler.SchemaAware = !inverse;
                }
                return true;
            } else if ("namespace_axis".Equals(value)) {
                return !inverse;
            } else if ("streaming".Equals(value)) {
                return !inverse;
            } else if ("backwards_compatibility".Equals(value)) {
                return !inverse;
            }
            return false;
        } else if ("xsd-version".Equals(type)) {
            if ("1.1".Equals(value)) {
                if (env != null) {
                    env.processor.SetProperty(JFeatureKeys.XSD_VERSION, (inverse ? "1.0" : "1.1"));
                } else {
                    return false;
                }
            } else if ("1.0".Equals(value)) {
                if (env != null) {
                    env.processor.SetProperty(JFeatureKeys.XSD_VERSION, (inverse ? "1.1" : "1.0"));
                } else {
                    return false;
                }
            }
            return true;
        } else if ("available_documents".Equals(type)) {
            return !inverse;
        } else if ("default_language_for_numbering".Equals(type)) {
            return !inverse;
        } else if ("languages_for_numbering".Equals(type)) {
            return !inverse;
        } else if ("supported_calendars_in_date_formatting_functions".Equals(type)) {
            return !inverse;
        } else if ("default_calendar_in_date_formatting_functions".Equals(type)) {
            return !inverse;
        } else if ("maximum_number_of_decimal_digits".Equals(type)) {
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
        } else if ("default_output_encoding".Equals(type)) {
            return !inverse;
        } else if ("unparsed_text_encoding".Equals(type)) {
            return !inverse;
        } else if ("year_component_values".Equals(type)) {
            return !inverse;
        } else if ("additional_normalization_form".Equals(type)) {
            return !inverse;
        } else if ("recognize_id_as_uri_fragment".Equals(type)) {
            return !inverse;
        } else if ("on-multiple-match".Equals(type)) {
            if (value.Equals("error")) {
                env.xsltCompiler.GetUnderlyingCompilerInfo().setRecoveryPolicy((int)RecoveryPolicy.DoNotRecover);
            } else {
                env.xsltCompiler.GetUnderlyingCompilerInfo().setRecoveryPolicy((int)RecoveryPolicy.RecoverSilently);
            }
            return true;
        } else if ("ignore-doc-failure".Equals(type)) {
            if (value.Equals("false")) {
                env.xsltCompiler.GetUnderlyingCompilerInfo().setRecoveryPolicy((int)RecoveryPolicy.DoNotRecover);
            } else {
                env.xsltCompiler.GetUnderlyingCompilerInfo().setRecoveryPolicy((int)RecoveryPolicy.RecoverSilently);
            }
            return true;
        } else if ("combinations_for_numbering".Equals(type)) {
            return !inverse;
        } else {
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

    private class OutputResolver : net.sf.saxon.lib.OutputURIResolver{

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
                    return destination.GetResult(proc.Implementation.makePipelineConfiguration());
                }
            } catch (Exception e) {
                throw e;
            }
        }

        public void close(JResult result) {
            if (serialized) {
                outcome.SetSecondaryResult(uri, null, stringWriter.ToString());
            } else {
                XdmDestination xdm = (XdmDestination)destination;
                outcome.SetSecondaryResult(xdm.BaseUri, xdm.XdmNode, null);
            }
        }

    }
}

}
