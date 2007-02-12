using System;
using System.IO;
using System.Collections;
using System.Xml;
using Saxon.Api;

/// <summary>
/// This is the test suite driver for running the W3C XSLT test suite against Saxon on .NET.
/// Note that the W3C XSLT test suite at the time of writing is available to W3C members only.
/// </summary>
/// <remarks>
/// <para>Before running, carry out the following steps:</para>
/// <para>1. Create a subdirectory SaxonResults.net within TestSuiteStagingArea</para>
/// <para>2. Copy compare.xsl from samples/styles into that subdirectory</para>
/// <para>3. Create a subdirectory SaxonResults within TestSuiteStagingArea</para>
/// <para>4. Create a file exceptions.xml within that subdirectory listing tests that
/// are not to be run (with a reason). Specimen format
/// as follows:</para>
/// <para><![CDATA[
/// <testcase-exceptions xmlns="http://www.w3.org/2005/05/xslt20-test-catalog">
///   <exception>
///     <testcase name="atrs24">
///       <comment>Expected result wrong, bug 700</comment>
///     </testcase>
///   </exception>
/// </testcase-exceptions>
/// ]]></para>
/// <para>TODO: use a different exceptions file for .NET, to exclude tests that rely on
/// features such as id() support.</para>
/// </remarks>


public class XsltTestSuiteDriver {
    static void Main(string[] args) {
        if (args.Length == 0 || args[0].Equals("-?")) {
            Console.WriteLine("XsltTestSuiteDriver testsuiteDir testName?");
        }

        new XsltTestSuiteDriver().go(args);
    }

    String testSuiteDir;
    Processor processor = new Processor();
    Processor schemaAwareProcessor = new Processor(true);
 
    string testPattern = null;
    bool xml11 = false;

    StreamWriter results;

    

    /**
     * Some tests use schemas that conflict with others, so they can't use the common schema cache.
     * These tests are run in a Configuration of their own. (Ideally we would put this list in a
     * catalogue file of some kind).
     */
/*
    static HashSet noCacheTests = new HashSet(30);
    static {
        noCacheTests.add("schemainline20_005_01");
        noCacheTests.add("schemamatch20_001_01");
        noCacheTests.add("schemamatch20_003_01");
        noCacheTests.add("schemamatch20_005_01");
        noCacheTests.add("schemamatch20_007_01");
        noCacheTests.add("schemamatch20_036_01");
        noCacheTests.add("schemamatch20_038_01");
        noCacheTests.add("schemamatch20_061_01");
        noCacheTests.add("schemamatch20_079_01");
        noCacheTests.add("schemamatch20_092_01");
        noCacheTests.add("schemamatch20_123_01");
        noCacheTests.add("schemamatch20_140_01");
        noCacheTests.add("schemanodetest20_001_01");
        noCacheTests.add("schemanodetest20_023_01");
        noCacheTests.add("schvalid001");
        noCacheTests.add("schvalid009");
        noCacheTests.add("schvalid014");
        noCacheTests.add("schvalid015");
        noCacheTests.add("schvalid020");
        noCacheTests.add("striptype20_003_01");
        noCacheTests.add("striptype20_006_01");
        noCacheTests.add("striptype20_008_01");
        noCacheTests.add("striptype20_011_01");
        noCacheTests.add("striptype20_012_01");
        noCacheTests.add("striptype20_039_01");

    }
*/
    private XdmNode getChildElement(XdmNode parent, QName child) {
        IEnumerator e = parent.EnumerateAxis(XdmAxis.Child, child);
        return e.MoveNext() ? (XdmNode)e.Current : null;
    }


    public void go(String[] args) {

        Console.WriteLine("Testing Saxon " + processor.ProductVersion);
        testSuiteDir = args[0];
        Hashtable exceptions = new Hashtable();

        if (args.Length > 1) {
            testPattern = (args[1]); // TODO: allow a regex
        }

        for (int i=0; i<args.Length; i++) {
            if (args[i].Equals("-w")) {
                //showWarnings = true;
            }
        }


        //try {


            String testURI = "http://www.w3.org/2005/05/xslt20-test-catalog";
            
            QName testCaseNT = new QName(testURI, "testcase");
            QName nameNT = new QName(testURI, "name");
            QName inputNT = new QName(testURI, "input");
            QName outputNT = new QName(testURI, "output");
            QName stylesheetNT = new QName(testURI, "stylesheet");
            QName schemaNT = new QName(testURI, "schema");
            QName initialModeNT = new QName(testURI, "initial-mode");
            QName entryNamedTemplateNT = new QName(testURI, "entry-named-template");
            QName sourceDocumentNT = new QName(testURI, "source-document");
            QName stylesheetParametersNT = new QName(testURI, "stylesheet-parameters");
            QName paramNT = new QName(testURI, "param");
            QName resultDocumentNT = new QName(testURI, "result-document");
            QName errorNT = new QName(testURI, "error");
            QName validationNT = new QName(testURI, "validation");
            QName discretionaryItemsNT = new QName(testURI, "discretionary-items");
            QName discretionaryFeatureNT = new QName(testURI, "discretionary-feature");
            QName discretionaryChoiceNT = new QName(testURI, "discretionary-choice");
            QName initialContextNodeNT = new QName(testURI, "initial-context-node");


            QName fileAtt = new QName("",  "file");
            QName errorIdAtt = new QName("", "error-id");
            QName typeAtt = new QName("", "type");
            QName nameAtt = new QName("", "name");
            QName behaviorAtt = new QName("", "behavior");
            QName qnameAtt = new QName("", "qname");
            QName modeAtt = new QName("", "mode");
            QName validatesAtt = new QName("", "validates");
            QName roleAtt = new QName("", "role");

            DocumentBuilder builder = processor.NewDocumentBuilder();
            XdmNode exceptionsDoc = builder.Build(new Uri(testSuiteDir + '/' + getResultDirectoryName() + "/exceptions.xml"));

            IEnumerator exceptionTestCases = exceptionsDoc.EnumerateAxis(XdmAxis.Descendant, testCaseNT);
            while (exceptionTestCases.MoveNext()) {
                XdmNode n = (XdmNode)exceptionTestCases.Current;
                String nameAttVal = n.GetAttributeValue(nameAtt);
                char[] seps = {' ', '\n', '\t'};
                String[] parts = nameAttVal.Split(seps);
                foreach (string p in parts) {
                    if (!exceptions.ContainsKey(p)) {
                        exceptions. Add(p, "Kilroy");
                    }
                }               
            }

            XdmNode catalog = builder.Build(new Uri(testSuiteDir + "/catalog.xml"));

            results = new StreamWriter(testSuiteDir + "/SaxonResults.net/results"
                        + processor.ProductVersion + ".xml");

            results.WriteLine("<test-suite-result>");
            results.WriteLine(" <implementation name='Saxon-SA' version='" + processor.ProductVersion +
                    "' anonymous-result-column='false'>");
            results.WriteLine("  <organization name='http://www.saxonica.com/' anonymous='false'/>");
            results.WriteLine("  <submitter name='Michael Kay' email='mike@saxonica.com'/>");
            outputDiscretionaryItems();
            results.WriteLine(" </implementation>");

            IEnumerator testCases = catalog.EnumerateAxis(XdmAxis.Descendant, testCaseNT);
            while (testCases.MoveNext()) {
                bool useAssociated = false;
                XdmNode testCase = (XdmNode)testCases.Current;

                String testName = getChildElement(testCase, nameNT).StringValue;
                if (testPattern != null && !testName.StartsWith(testPattern)/*!testPattern.matcher(testName).matches()*/) {
                    continue;
                }
                if (exceptions.ContainsKey(testName)) {
                    continue;
                }
                if (isExcluded(testName)) {
                    continue;
                }
                Console.WriteLine("Test " + testName);
                XdmNode testInput = getChildElement(testCase, inputNT);

                XdmNode stylesheet = getChildElement(testInput, stylesheetNT);
                String absXSLName = null;
                if (stylesheet == null) {
                    useAssociated = true;
                } else {
                    absXSLName = testSuiteDir + "/TestInputs/" + stylesheet.GetAttributeValue(fileAtt);
                }

                XdmNode sourceDocument = getChildElement(testInput, sourceDocumentNT);
                String absXMLName = null;
                if (sourceDocument != null) {
                    absXMLName = testSuiteDir + "/TestInputs/" + sourceDocument.GetAttributeValue(fileAtt);
                }

                bool schemaAware = false;
                bool recoverRecoverable = true;
                bool backwardsCompatibility = true;
                bool supportsDOE = true;
                bool recoverSESU0007 = false;
                XdmNode discretionaryItems = getChildElement(testCase, discretionaryItemsNT);
                if (discretionaryItems != null) {
                    IEnumerator features = discretionaryItems.EnumerateAxis(XdmAxis.Child, discretionaryFeatureNT);
                    while (features.MoveNext()) {
                        XdmNode feature = (XdmNode)features.Current;
                        String featureName = feature.GetAttributeValue(nameAtt);
                        if ("schema_aware".Equals(featureName)) {
                            schemaAware = "on".Equals(feature.GetAttributeValue(behaviorAtt));
                        } else if ("XML_1.1".Equals(featureName)) {
                            xml11 = "on".Equals(feature.GetAttributeValue(behaviorAtt));
                        } else if ("backwards_compatibility".Equals(featureName)) {
                            backwardsCompatibility = "on".Equals(feature.GetAttributeValue(behaviorAtt));
                        } else if ("disabling_output_escaping".Equals(featureName)) {
                            supportsDOE = "on".Equals(feature.GetAttributeValue(behaviorAtt));
                        }
                    }
                    IEnumerator choices = discretionaryItems.EnumerateAxis(
                            XdmAxis.Child, discretionaryChoiceNT);
                    while (features.MoveNext()) {
                        XdmNode choice = (XdmNode)choices.Current;
                        String featureName = choice.GetAttributeValue(nameAtt);
                        if ("error".Equals(choice.GetAttributeValue(behaviorAtt))) {
                            recoverRecoverable = false;
                        } else if ("SESU0007".Equals(featureName)) {
                            recoverSESU0007 = "recovery".Equals(choice.GetAttributeValue(behaviorAtt));
                        }
                    }
                }

                if (!backwardsCompatibility) {
                    // Saxon cannot run with BC switched off
                    results.WriteLine(" <testcase name='" + testName + "' result='not run' comment='requires backwards-compatibility=off'/>");
                    continue;
                }

                if (!supportsDOE) {
                    // Saxon cannot run with DOE switched off
                    results.WriteLine(" <testcase name='" + testName + "' result='not run' comment='requires disable-output-escaping=off'/>");
                    continue;
                }

                if (recoverSESU0007) {
                    // Saxon cannot recover from error SESU0007
                    results.WriteLine(" <testcase name='" + testName + "' result='not run' comment='requires recovery from error SESU0007'/>");
                    continue;
                }

                XdmNode initialMode = getChildElement(testInput, initialModeNT);
                QName initialModeName = null;
                if (initialMode != null) {
                    String ini = initialMode.GetAttributeValue(qnameAtt);
                    if (ini.IndexOf(":") >= 0) {
                        initialModeName = new QName(ini, initialMode);
                    } else {
                        initialModeName = new QName("", ini);
                    }
                }
                
                XdmNode initialTemplate = getChildElement(testInput, entryNamedTemplateNT);
                QName initialTemplateName = null;
                if (initialTemplate != null) {
                    String ini = initialTemplate.GetAttributeValue(qnameAtt);
                    if (ini.IndexOf(":") >= 0) {
                        initialTemplateName = new QName(ini, initialTemplate);
                    } else {
                        initialTemplateName = new QName("", ini);
                    }
                }

                XdmNode initialContextNode = getChildElement(testInput, initialContextNodeNT);
                String initialContextPath = null;
                if (initialContextNode != null) {
                    initialContextPath = initialContextNode.StringValue;
                }

                XdmNode validation = getChildElement(testInput, validationNT);
                String validationMode = null;
                if (validation != null) {
                    validationMode = validation.GetAttributeValue(modeAtt);
                }

                Hashtable paramTable = null;
                XdmNode paramList = getChildElement(testInput, stylesheetParametersNT);
                if (paramList != null) {
                    paramTable = new Hashtable(5);
                    IEnumerator paramIter = paramList.EnumerateAxis(XdmAxis.Child, paramNT);
                    while (paramIter.MoveNext()) {
                        XdmNode param = (XdmNode)paramIter.Current;
                        QName name = new QName(param.GetAttributeValue(qnameAtt), param);
                        String value = param.StringValue;
                        paramTable.Add(name, value);
                    }
                }

                IEnumerator schemas = testInput.EnumerateAxis(XdmAxis.Child, schemaNT);
                while (schemas.MoveNext()) {
                    XdmNode schema = (XdmNode)schemas.Current;
                    if (schema == null) {
                        break;
                    }
                    String role = schema.GetAttributeValue(roleAtt);
                    if (("source-validator".Equals(role) || "source-reference".Equals(role))
                            /* && schema.GetAttributeValue(validatesAtt) != null */) {
                        validationMode = "strict";
                        // TODO: control which source documents are validated...
                    }
                }
                XdmNode testOutput = getChildElement(testCase, outputNT);
                XdmNode resultDocument = getChildElement(testOutput, resultDocumentNT);
                    // TODO: handle alternative result documents
                String refFileName = null;
                String outFileName;
                String comparator = "xml";
                if (resultDocument != null) {
                    String relativePath = resultDocument.GetAttributeValue(fileAtt);
                    int slash = relativePath.IndexOf('/');
                    if (slash > 0) {
                        String relativeDir = relativePath.Substring(0, slash);
                        String fullDir = testSuiteDir + '/' + getResultDirectoryName() + ".net/" + relativeDir;
                        if (!Directory.Exists(fullDir)) {
                            Directory.CreateDirectory(fullDir);
                        }
                    }
                    refFileName = testSuiteDir + "/ExpectedTestResults/" + relativePath;
                    outFileName = testSuiteDir + '/' + getResultDirectoryName() + ".net/" + relativePath;
                    comparator = resultDocument.GetAttributeValue(typeAtt);
                } else {
                    outFileName = testSuiteDir + '/' + getResultDirectoryName() + ".net/temp.out";
                }
                XdmNode error = getChildElement(testOutput, errorNT);
                String expectedError = null;
                if (error != null) {
                    expectedError = error.GetAttributeValue(errorIdAtt);
                }
                bool success;
                string errorCode =               
                    runXSLT(testName, absXMLName, absXSLName, initialModeName, initialTemplateName,
                        outFileName, paramTable, initialContextPath,
                        useAssociated, schemaAware, validationMode, recoverRecoverable);
                if (errorCode == null) {
                    success = true;
                    if (expectedError != null && resultDocument == null) {
                        Console.WriteLine("Test failed. Expected error " + expectedError + ", got success");
                        success = false;
                        results.WriteLine(" <testcase name='" + testName +
                                "' result='differ' comment='Expected error " +
                                expectedError + ", got success'/>");
                    }
                } else {
                    if (expectedError != null && errorCode != null && errorCode.Equals(expectedError)) {
                        Console.WriteLine("Test succeeded (" + expectedError + ')');
                        results.WriteLine(" <testcase name='" + testName +
                                "' result='full' comment='Error " +
                                expectedError + " as expected'/>");
                    } else if (expectedError != null) {
                        Console.WriteLine("Test succeeded (??) (expected " + expectedError + ", got " + errorCode + ')');
                        results.WriteLine(" <testcase name='" + testName +
                                "' result='different-error' comment='Expected " +
                                expectedError + " got " + errorCode + "'/>");
                    } else {
                        Console.WriteLine("Test failed. Expected success, got " + errorCode);
                        results.WriteLine(" <testcase name='" + testName +
                                "' result='differ' comment='Expected success, got " +
                                errorCode + "'/>");
                    }
                    success = false;
                    continue;
                } 


                if (success) {
                    bool same = compare(outFileName, refFileName, comparator);
                    if (same) {
                        results.WriteLine(" <testcase name='" + testName + "' result='full'/>");
                    } else {
                        results.WriteLine(" <testcase name='" + testName + "' result='differ'/>");
                    }
                }

            }

            results.WriteLine("</test-suite-result>");
            results.Close();

        //} 
    }

    protected string getResultDirectoryName() {
        return "SaxonResults";
    }

    protected bool isExcluded(String testName) {
        return false;
    }

    protected string runXSLT(String testName, String xml, String xsl, QName initialMode,
                           QName initialTemplate, String outfile, Hashtable paramTable, String initialContextPath,
                           bool useAssociated, bool schemaAware,
                           String validationMode, bool recoverRecoverable) {
        Serializer sr = new Serializer();
        sr.SetOutputFile(outfile);
        Processor f;
        //if (noCacheTests.contains(testName) || testName.startsWith("schemaas20") ||
        //        testName.startsWith("striptype20") || testName.startsWith("notation20")) {
            // create a custom Processor to avoid schema caching
        //} else {
            if (schemaAware) {
                f = schemaAwareProcessor;
            } else if (xml11) {
                f = processor;
                // Use an Xml 1.1 processor
            } else {
                f = processor;
            }
        //}

        XdmNode source = null;
        
        IList errors = new ArrayList();
        XsltCompiler compiler = f.NewXsltCompiler();
        compiler.ErrorList = errors;
        XsltExecutable sheet = null;
        XsltTransformer inst;

        if (useAssociated) {
            try {
                source = buildSource(f.NewDocumentBuilder(), xml, validationMode);
            } catch (Exception e) {
                Console.WriteLine("Failed to build source document: " + e.Message);
                return "ErrorBBB";
            }
            try {
                sheet = compiler.CompileAssociatedStylesheet(source);
            } catch (Exception e) {
                Console.WriteLine("Failed to compile stylesheet: " + e.Message);
                if (errors.Count > 0) {
                    QName code = ((StaticError)errors[0]).ErrorCode;
                    return (code==null ? "ErrorXXX" : code.LocalName);
                } else {
                    return "ErrorXXX";
                }
            }
        } else {
            Stream stream = new FileStream(xsl, FileMode.Open, FileAccess.Read);
            compiler.BaseUri = new Uri(xsl);
            try {
                sheet = compiler.Compile(stream);
            } catch (Exception e) {
                if (errors.Count > 0) {
                    return ((StaticError)errors[0]).ErrorCode.LocalName;
                } else {
                    Console.WriteLine(e.Message);
                    return "ErrorXXX";
                }
            } finally {
                stream.Close();
            }
        }
        if (source == null && xml != null) {
            try {
                source = buildSource(f.NewDocumentBuilder(), xml, validationMode);
            } catch (Exception e) {
                Console.WriteLine("Failed to build source document: " + e.Message);
                return "ErrorCCC";
            }
        }
        if (initialContextPath != null) {
            XPathCompiler xc = f.NewXPathCompiler();
            XPathExecutable exp = xc.Compile(initialContextPath);
            XPathSelector xpe = exp.Load();
            xpe.ContextItem = source;
            XdmNode node = (XdmNode)xpe.EvaluateSingle();
            source = node;
        }
        
        inst = sheet.Load();
        if (initialMode != null) {
            inst.InitialMode = initialMode;
        }
        if (initialTemplate != null) {
            try {
                inst.InitialTemplate = initialTemplate;
            } catch (DynamicError e) {
                QName code = e.ErrorCode;
                if (code != null) {
                    return code.LocalName;
                } else {
                    return "ErrorYYY";
                }
            }
        }
        if (paramTable != null) {
            foreach (DictionaryEntry de in paramTable) {
                inst.SetParameter((QName)de.Key, new XdmAtomicValue(de.Value.ToString()));
            }
        }
        inst.InitialContextNode = source;
        
        //inst.setURIResolver(factory.getURIResolver());
        //inst.setErrorListener(errorListener);
        //((Controller)inst).setRecoveryPolicy(recoverRecoverable ? Configuration.RECOVER_SILENTLY : Configuration.DO_NOT_RECOVER);
        // To avoid test results being dependent on the date and time (and timezone), set a fixed
        // date and time for the run
        //((Controller)inst).setCurrentDateTime(new DateTimeValue("2005-01-01T12:49:30.5+01:00"));

        try {
            inst.Run(sr);
        } catch (DynamicError e) {
            Console.WriteLine(e.Message);
            QName code = e.ErrorCode;
            if (code != null) {
                return code.LocalName;
            } else {
                return "ErrYYYYY";
            }
        }
        return null;    // indicating success
    }

    /**
     * Construct source object. This method allows subclassing e.g. to build a DOM or XOM source.
     * @param xml
     * @return
     * @throws XPathException
     */

    protected XdmNode buildSource(DocumentBuilder builder, String xml, String validationMode) {
        if ("strict".Equals(validationMode)) {
            builder.SchemaValidationMode = SchemaValidationMode.Strict;
        } else {
            builder.SchemaValidationMode = SchemaValidationMode.None;
        }
        return builder.Build(new Uri(xml));
    }


    private bool compare(String outfile, String reffile, String comparator) {
        if (reffile == null) {
            Console.WriteLine("*** No reference results available");
            return false;
        }
        //File outfileFile = new File(outfile);
        //File reffileFile = new File(reffile);

        if (!File.Exists(reffile)) {
            Console.WriteLine("*** No reference results available");
            return false;
        }

        // try direct comparison first

        String refResult = null;
        String actResult = null;

        try {
            // This is decoding bytes assuming the platform default encoding
            StreamReader reader1;
            try {
                reader1 = new StreamReader(outfile);
            } catch (Exception err) {
                Console.WriteLine("Failed to read output file " + outfile + ": " + err);
                return false;
            }
            StreamReader reader2;
            try {
                reader2 = new StreamReader(reffile);
            } catch (Exception err) {
                Console.WriteLine("Failed to read reference file " + reffile + ": " + err);
                return false;
            }

            char[] contents1 = new char[65536];
            char[] contents2 = new char[65536];
            int size1 = reader1.Read(contents1, 0, 65536);
            int size2 = reader2.Read(contents2, 0, 65536);
            reader1.Close();
            reader2.Close();

            int offset1 = 0;
            int offset2 = 0;
            if (contents1[0]=='\u00ef' && contents1[1]=='\u00bb' && contents1[2]=='\u00bf') {
                offset1 += 3;
            }
            if (contents2[0]=='\u00ef' && contents2[1]=='\u00bb' && contents2[2]=='\u00bf') {
                offset2 += 3;
            }
            actResult = (size1==-1 ? "" : new String(contents1, offset1, size1-offset1));
            refResult = (size2==-1 ? "" : new String(contents2, offset2, size2-offset2));

            actResult = actResult.Replace("\r\n", "\n");
            refResult = refResult.Replace("\r\n", "\n");
            if (actResult.Equals(refResult)) {
                return true;
            }
            if (size1 == 0) {
                Console.WriteLine("** ACTUAL RESULTS EMPTY; REFERENCE RESULTS LENGTH " + size2);
                return false;
            }
            if (size2 == 0) {
                Console.WriteLine("** REFERENCED RESULTS EMPTY; ACTUAL RESULTS LENGTH " + size2);
                return false;
            }
            
        } catch (Exception e) {
            Console.Write(e.StackTrace);
            return false;
        }

        // HTML: can't do logical comparison

        if (comparator.Equals("html-output")) {
                     
            Console.WriteLine("*** Compare HTML outputs by hand");
                
        } else if (comparator.Equals("xhtml-output")) {
            refResult = canonizeXhtml(processor, refResult);
            actResult = canonizeXhtml(processor, actResult);
            return (actResult.Equals(refResult));

        } else if (comparator.Equals("xml-frag")) {
            try {
                return compareFragments(outfile, reffile);
            } catch (Exception err2) {
                Console.WriteLine("Failed to compare results for: " + outfile);
                Console.Write(err2.StackTrace);
                return false;
            }
        } else {
            // convert both files to Canonical XML and compare them again
            return compareXML(outfile, reffile);
  
        }
        return false;
    }

    XsltExecutable xhtmlCanonizer;

    private String canonizeXhtml(Processor p, String input) {
        try {
            XsltExecutable canonizer = getXhtmlCanonizer(p);
            XsltTransformer t = canonizer.Load();
            StringWriter sw = new StringWriter();
            Serializer r = new Serializer();
            r.SetOutputWriter(sw);
            t.InitialContextNode = p.NewDocumentBuilder().Build(
                new FileStream(input, FileMode.Open));
            t.Run(r);
            return sw.ToString();
        } catch (Exception err) {
            Console.WriteLine("*** Failed to compile or run XHTML canonicalizer stylesheet: " + err.ToString());
        } 
        return "";
    }

    private XsltExecutable getXhtmlCanonizer(Processor p) {
        if (xhtmlCanonizer == null) {
            xhtmlCanonizer = p.NewXsltCompiler().Compile(
                new FileStream(testSuiteDir + "/SaxonResults/canonizeXhtml.xsl", FileMode.Open));
        }
        return xhtmlCanonizer;
    }

    private bool compareFragments(String outfile, String reffile) {
        // if we can't parse the output as a document, try it as an external entity, with space stripping
        String outurl = outfile;
        String refurl = reffile;
        String outdoc = "<?xml version='1.1'?><!DOCTYPE doc [ <!ENTITY e SYSTEM '" + outurl + "'>]><doc>&e;</doc>";
        String refdoc = "<?xml version='1.1'?><!DOCTYPE doc [ <!ENTITY e SYSTEM '" + refurl + "'>]><doc>&e;</doc>";
        //InputSource out2 = new InputSource(new StringReader(outdoc));
        //InputSource ref2 = new InputSource(new StringReader(refdoc));
        //String outxml2 = canon.toCanonicalXML(fragmentParser, out2, true);
        //String refxml2 = canon.toCanonicalXML(fragmentParser, ref2, true);
        //if (outxml2 != null && refxml2 != null && !outxml2.Equals(refxml2)) {
        //    Console.WriteLine("Mismatch with reference results: " + outfile);
        //    Console.WriteLine("REFERENCE RESULTS:");
        //    Console.WriteLine(truncate(refxml2));
        //    Console.WriteLine("ACTUAL RESULTS:");
        //    Console.WriteLine(truncate(outxml2));
        //    findDiff(refxml2, outxml2);
            return false;
        //} else if (outxml2 == null) {
        //    Console.WriteLine("Cannot canonicalize actual results");
        //    return false;
        //} else if (refxml2 == null) {
        //    Console.WriteLine("Cannot canonicalize reference results");
        //    return false;
        //}
        //return true;
    }

    private static String truncate(String s) {
        if (s.Length > 200) return s.Substring(0, 200);
        return s;
    }

    private static void findDiff(String s1, String s2) {
        int i = 0;
        while (true) {
            if (s1[i] != s2[i]) {
                int j = (i < 50 ? 0 : i - 50);
                int k = (i + 50 > s1.Length || i + 50 > s2.Length ? i + 1 : i + 50);
                Console.WriteLine("Different at char " + i + "\n+" + s1.Substring(j, k) +
                                   "\n+" + s2.Substring(j, k));
                break;
            }
            if (i >= s1.Length) break;
            if (i >= s2.Length) break;
            i++;
        }
    }

    private void outputDiscretionaryItems() {
        results.WriteLine("  <discretionary-items/>");
    }

    private XsltExecutable xmlComparer = null;

    private bool compareXML(String actual, String gold) {
        try {
            if (xmlComparer == null) {
                xmlComparer = processor.NewXsltCompiler().Compile(new Uri(testSuiteDir + "/SaxonResults.net/compare.xsl"));
            }
            XdmNode doc1 = processor.NewDocumentBuilder().Build(new Uri(actual));
            XdmNode doc2 = processor.NewDocumentBuilder().Build(new Uri(gold));
            XsltTransformer t = xmlComparer.Load();
            t.InitialTemplate = new QName("", "compare");
            t.SetParameter(new QName("", "actual"), doc1);
            t.SetParameter(new QName("", "gold"), doc2);

            StringWriter sw = new StringWriter();
            Serializer sr = new Serializer();
            sr.SetOutputWriter(sw);

            t.Run(sr);
            String result = sw.ToString();
            return result.StartsWith("true");
        } catch (Exception e) {
            Console.WriteLine("***" + e.Message);
            return false;
        }
    }




}


