using System;
using System.IO;
using System.Collections;
using System.Xml;
using System.Text;
using Saxon.Api;

/// <summary>
/// Test suite driver for running the XQTS test suite against Saxon on .NET.
/// </summary>
/// <remarks>
/// <paraThe test suite directory must contain a subdirectory named SaxonDriver.
/// In this subdirectory there must be a document named exceptions.xml, containing</para>
/// <example><![CDATA[
/// <exceptions>
///   <exception>
///     <tests>trivial-1 trivial-2 trivial-3 trivial-4</tests>
///     <description>No support in Saxon for trivial embedding feature</description>
///   </exception>
/// </exceptions>
/// ]]></example>
/// <para>The SaxonDriver directory should also have a subdirectory named <c>results</c>,
/// with the same structure as the supplied <c>expectedResults</c> directory.</para>
/// <para>Note that the submitted results for Saxon were obtained with the Java product;
/// this driver will produce different results. <b>At present this test driver is
/// reporting many spurious test failures where the results are in fact correct.</b></para>
/// </remarks>


public class XQueryTestSuiteDriver {
    static void Main(string[] args) {
        if (args.Length == 0 || args[0].Equals("-?")) {
            Console.WriteLine("XQueryTestSuiteDriver testsuiteDir testName?");
        }
        try {
            new XQueryTestSuiteDriver().go(args);
        } catch (Exception e) {
            Console.WriteLine(e.Message);
            Console.WriteLine(e.StackTrace);
        }
    }

    String testURI = "http://www.w3.org/2005/02/query-test-XQTSCatalog";
    String testSuiteDir;
    Processor processor = new Processor(true, true);  // processor is schema-aware
    // TODO: change to (true, false) when not debugging

    XPathExecutable findSourcePath;
    XPathExecutable compareDocuments;
    XPathExecutable findCollection;
    XPathExecutable findModule;


    string testPattern = null;
    bool debug = false;

    StreamWriter results;


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

        for (int i = 0; i < args.Length; i++) {
            if (args[i].Equals("-w")) {
                //showWarnings = true;
            } else if (args[i].Equals("-debug")) {
                debug = true;
            }
        }

        

        XPathCompiler xpc = processor.NewXPathCompiler();
        xpc.DeclareNamespace("t", testURI);
        xpc.DeclareVariable(new QName("", "param"));
        findSourcePath = xpc.Compile("//t:test-suite/t:sources/t:source[@ID=$param]");

        findCollection = xpc.Compile("//t:test-suite/t:sources/t:collection[@ID=$param]");

        xpc = processor.NewXPathCompiler();
        xpc.DeclareNamespace("t", testURI);
        xpc.DeclareVariable(new QName("", "testcase"));
        xpc.DeclareVariable(new QName("", "moduleuri"));
        findModule = xpc.Compile("for $m in $testcase/t:module[@namespace=$moduleuri] " +
            "return concat('file:///" + testSuiteDir + 
            "', root($testcase)/t:test-suite/t:sources/t:module[@ID=string($m)]/@FileName, '.xq')");

        xpc = processor.NewXPathCompiler();
        xpc.DeclareNamespace("saxon", "http://saxon.sf.net/");
        xpc.DeclareVariable(new QName("", "actual"));
        xpc.DeclareVariable(new QName("", "gold"));
        xpc.DeclareVariable(new QName("", "debug"));
        compareDocuments = xpc.Compile("saxon:deep-equal($actual, $gold, (), if ($debug) then 'JNCPS?!' else 'JNCPS')");

        QName testCaseNT = new QName(testURI, "test-case");
        QName nameNT = new QName(testURI, "name");
        QName queryNT = new QName(testURI, "query");
        QName inputNT = new QName(testURI, "input");
        QName inputFileNT = new QName(testURI, "input-file");
        QName inputUriNT = new QName(testURI, "input-URI");
        QName defaultCollectionNT = new QName(testURI, "defaultCollection");
        QName outputFileNT = new QName(testURI, "output-file");
        QName expectedErrorNT = new QName(testURI, "expected-error");
        QName schemaNT = new QName(testURI, "schema");
        QName contextItemNT = new QName(testURI, "contextItem");
        QName inputQueryNT = new QName(testURI, "input-query");
        QName sourceDocumentNT = new QName(testURI, "source-document");
        QName errorNT = new QName(testURI, "error");
        QName validationNT = new QName(testURI, "validation");
        QName discretionaryItemsNT = new QName(testURI, "discretionary-items");
        QName discretionaryFeatureNT = new QName(testURI, "discretionary-feature");
        QName discretionaryChoiceNT = new QName(testURI, "discretionary-choice");
        QName initialContextNodeNT = new QName(testURI, "initial-context-node");


        QName fileAtt = new QName("", "file");
        QName filePathAtt = new QName("", "FilePath");
        QName fileNameAtt = new QName("", "FileName");
        QName errorIdAtt = new QName("", "error-id");
        QName compareAtt = new QName("", "compare");
        QName nameAtt = new QName("", "name");
        QName behaviorAtt = new QName("", "behavior");
        QName qnameAtt = new QName("", "qname");
        QName modeAtt = new QName("", "mode");
        QName validatesAtt = new QName("", "validates");
        QName variableAtt = new QName("", "variable");
        QName roleAtt = new QName("", "role");

        DocumentBuilder builder = processor.NewDocumentBuilder();
        XdmNode exceptionsDoc = builder.Build(new Uri(testSuiteDir + "SaxonDriver/exceptions.xml"));

        // The exceptions.xml file contains details of tests that aren't to be run, for example
        // because they have known bugs or require special configuration

        IEnumerator exceptionTestCases = exceptionsDoc.EnumerateAxis(XdmAxis.Descendant, new QName("", "exception"));
        while (exceptionTestCases.MoveNext()) {
            XdmNode n = (XdmNode)exceptionTestCases.Current;
            String nameAttVal = n.StringValue;
            char[] seps = { ' ', '\n', '\t' };
            String[] parts = nameAttVal.Split(seps);
            foreach (string p in parts) {
                if (!exceptions.ContainsKey(p)) {
                    exceptions.Add(p, "Exception");
                }
            }
        }

        // Hash table containing all source documents. The key is the document name in the
        // catalog, the value is the corresponding document node

        Hashtable sourceDocs = new Hashtable(50);

        // Load the catalog

        XdmNode catalog = builder.Build(new Uri(testSuiteDir + "/XQTScatalog.xml"));

        // Add all Static Typing test cases to the exceptions list

        xpc = processor.NewXPathCompiler();
        xpc.DeclareNamespace("t", testURI);
        XPathSelector st = xpc.Compile("//t:test-group[@name='StaticTyping']//t:test-case").Load();
        st.ContextItem = catalog;
        IEnumerator ste = st.GetEnumerator();
        while (ste.MoveNext()) {
            XdmNode testCase = (XdmNode)ste.Current;
            exceptions.Add(testCase.GetAttributeValue(nameAtt), "StaticTypingException");
        }

        // Create the results file

        results = new StreamWriter(testSuiteDir + "/SaxonDriver/results"
                    + processor.ProductVersion + "n.xml");

        results.WriteLine("<test-suite-result xmlns='http://www.w3.org/2005/02/query-test-XQTSResult'>");

        // Pre-load all the schemas

        SchemaManager mgr = processor.SchemaManager;
        IEnumerator se = catalog.EnumerateAxis(XdmAxis.Descendant, schemaNT);
        while (se.MoveNext()) {
            XdmNode schemaNode = (XdmNode)se.Current;
            Console.WriteLine("Loading schema " + schemaNode.GetAttributeValue(fileNameAtt));
            Uri location = new Uri(testSuiteDir + schemaNode.GetAttributeValue(fileNameAtt));
            mgr.Compile(location);
        }

        // Process the test cases in turn

        IEnumerator testCases = catalog.EnumerateAxis(XdmAxis.Descendant, testCaseNT);
        while (testCases.MoveNext()) {
            XdmNode testCase = (XdmNode)testCases.Current;

            String testName = testCase.GetAttributeValue(nameAtt);
            if (testPattern != null && !testName.StartsWith(testPattern)) {
                continue;
            }
            if (exceptions.ContainsKey(testName)) {
                continue;
            }

            Console.WriteLine("Test " + testName);


            // Compile the query

            String errorCode = null;

            String filePath = testCase.GetAttributeValue(filePathAtt);
            XdmNode query = getChildElement(testCase, queryNT);
            String queryName = query.GetAttributeValue(nameAtt);
            String queryPath = testSuiteDir + "/Queries/XQuery/" + filePath + queryName + ".xq";

            XQueryCompiler compiler = processor.NewXQueryCompiler();
            compiler.BaseUri = new Uri(queryPath).ToString();
            compiler.QueryResolver = new XqtsModuleResolver(testCase, findModule);

            ArrayList errors = new ArrayList();
            compiler.ErrorList = errors;
            XQueryEvaluator xqe = null;
            FileStream stream = null;
            try {
                stream = new FileStream(queryPath, FileMode.Open, FileAccess.Read, FileShare.Read);
                xqe = compiler.Compile(stream).Load();
            } catch (Exception e) {
                if (errors.Count > 0 && ((StaticError)errors[0]).ErrorCode != null) {
                    errorCode = ((StaticError)errors[0]).ErrorCode.LocalName;
                } else if (e is StaticError && ((StaticError)e).ErrorCode != null) {
                    Console.WriteLine(e.Message);
                    errorCode = ((StaticError)e).ErrorCode.LocalName;
                } else {
                    Console.WriteLine(e.Message);
                    errorCode = "ErrorXXX";
                }
            } finally {
                if (stream != null) {
                    stream.Close();
                }
            }

            // if the query compiled successfully, try to run it

            String outputPath = null;
            if (errorCode == null && xqe != null) {

                // Supply any input documents

                IEnumerator en = testCase.EnumerateAxis(XdmAxis.Child, inputFileNT);
                while (en.MoveNext()) {
                    XdmNode file = (XdmNode)en.Current;
                    String var = file.GetAttributeValue(variableAtt);
                    if (var != null) {
                        String sourceName = file.StringValue;
                        XdmNode sourceDoc;
                        if (sourceDocs.ContainsKey(sourceName)) {
                            sourceDoc = (XdmNode)sourceDocs[sourceName];
                        } else {
                            sourceDoc = buildSource(catalog, builder, sourceName);
                            sourceDocs.Add(sourceName, sourceDoc);
                        }
                        xqe.SetExternalVariable(new QName("", var), sourceDoc);
                    }
                }

                // Supply any input URIs

                IEnumerator eu = testCase.EnumerateAxis(XdmAxis.Child, inputUriNT);
                while (eu.MoveNext()) {
                    XdmNode file = (XdmNode)eu.Current;
                    String var = file.GetAttributeValue(variableAtt);
                    if (var != null) {
                        String sourceName = file.StringValue;
                        if (sourceName.StartsWith("collection")) {
                            // Supply a collection URI. 
                            // This seems to be the only way to distinguish a document URI 
                            // from a collection URI.
                            String uri = "collection:" + sourceName;
                            XPathSelector xpe = findCollection.Load();
                            xpe.SetVariable(new QName("", "param"), new XdmAtomicValue(sourceName));
                            xpe.ContextItem = catalog;
                            XdmNode collectionNode = (XdmNode)xpe.EvaluateSingle();
                            if (collectionNode == null) {
                                Console.WriteLine("*** Collection " + sourceName + " not found");
                            }
                            processor.RegisterCollection(new Uri(uri), getCollection(collectionNode));
                            xqe.SetExternalVariable(new QName("", var), new XdmAtomicValue(uri));
                        } else {
                            // Supply a document URI.
                            // We exploit the fact that the short name of the document is
                            // always the same as the file name in these tests
                            String uri = "file:///" + testSuiteDir + "TestSources/" + sourceName + ".xml";
                            xqe.SetExternalVariable(new QName("", var), new XdmAtomicValue(uri));
                        }
                    }
                }

                // Supply the default collection if required

                XdmNode defaultCollection = getChildElement(testCase, defaultCollectionNT);
                if (defaultCollection != null) {
                    String sourceName = defaultCollection.StringValue;
                    XPathSelector xpe = findCollection.Load();
                    xpe.SetVariable(new QName("", "param"), new XdmAtomicValue(sourceName));
                    xpe.ContextItem = catalog;
                    XdmNode collectionNode = (XdmNode)xpe.EvaluateSingle();
                    if (collectionNode == null) {
                        Console.WriteLine("*** Collection " + sourceName + " not found");
                    }
                    processor.RegisterCollection(null, getCollection(collectionNode));
                }

                // Supply any external variables defined as the result of a separate query

                IEnumerator ev = testCase.EnumerateAxis(XdmAxis.Child, inputQueryNT);
                while (ev.MoveNext()) {
                    XdmNode inputQuery = (XdmNode)ev.Current;

                    String fileName = inputQuery.GetAttributeValue(nameAtt);
                    String subQueryPath = testSuiteDir + "/Queries/XQuery/" + filePath + fileName + ".xq";
                    XQueryCompiler subCompiler = processor.NewXQueryCompiler();
                    compiler.BaseUri = new Uri(subQueryPath).ToString();
                    FileStream subStream = new FileStream(subQueryPath, FileMode.Open, FileAccess.Read, FileShare.Read);
                    XdmValue value = subCompiler.Compile(subStream).Load().Evaluate();
                    String var = inputQuery.GetAttributeValue(variableAtt);
                    xqe.SetExternalVariable(new QName("", var), value);                   
                }

                // Supply the context item if required

                IEnumerator ci = testCase.EnumerateAxis(XdmAxis.Child, contextItemNT);
                while (ci.MoveNext()) {
                    XdmNode file = (XdmNode)ci.Current;

                    String sourceName = file.StringValue;
                    if (!sourceDocs.ContainsKey(sourceName)) {
                        XdmNode doc = buildSource(catalog, builder, sourceName);
                        sourceDocs.Add(sourceName, doc);
                    }
                    XdmNode sourceDoc = (XdmNode)sourceDocs[sourceName];
                    xqe.ContextItem = sourceDoc;
                }

                // Create a serializer for the output


                outputPath = testSuiteDir + "SaxonDriver/results.net/" + filePath + queryName + ".out";
                Serializer sr = new Serializer();
                try {
                    sr.SetOutputFile(outputPath);
                    sr.SetOutputProperty(new QName("", "method"), "xml");
                    sr.SetOutputProperty(new QName("", "omit-xml-declaration"), "yes");
                    sr.SetOutputProperty(new QName("", "indent"), "no");
                } catch (DynamicError) {
                    // probably means that no output directory exists, which is probably because
                    // an error is expected
                    outputPath = testSuiteDir + "SaxonDriver/results.net/" + queryName + ".out";
                    sr.SetOutputFile(outputPath);
                }

                // Finally, run the query

                try {
                    xqe.Run(sr);
                } catch (DynamicError e) {
                    Console.WriteLine(e.Message);
                    QName code = e.ErrorCode;
                    if (code != null && code.LocalName != null) {
                        errorCode = code.LocalName;
                    } else {
                        errorCode = "ErrYYYYY";
                    }
                } catch (Exception e2) {
                    Console.WriteLine("Unexpected exception: " + e2.Message);
                    Console.WriteLine(e2.StackTrace);
                    errorCode = "CRASH!!!";
                }
            }

            // Compare actual results with expected results

            if (errorCode != null) {
                // query returned an error at compile time or run-time, check this was expected

                string expectedError = "";
                bool matched = false;
                IEnumerator en = testCase.EnumerateAxis(XdmAxis.Child, expectedErrorNT);
                while (en.MoveNext()) {
                    XdmNode error = (XdmNode)en.Current;
                    String expectedErrorCode = error.StringValue;
                    expectedError += (expectedErrorCode + " ");
                    if (expectedErrorCode.Equals(errorCode)) {
                        matched = true;
                        Console.WriteLine("Error " + errorCode + " as expected");
                        results.WriteLine("<test-case name='" + testName + "' result='pass'/>");
                        break;
                    }
                }
                if (!matched) {
                    if (expectedError.Equals("")) {
                        Console.WriteLine("Error " + errorCode + ", expected success");
                        results.WriteLine("<test-case name='" + testName + "' result='fail' comment='error " + errorCode + ", expected success'/>");
                    } else {
                        Console.WriteLine("Error " + errorCode + ", expected " + expectedError);
                        results.WriteLine("<test-case name='" + testName + "' result='pass' comment='error " + errorCode + ", expected " + expectedError + "'/>");
                    }
                }

            } else {
                // query returned no error

                bool matched = false;
                IEnumerator en = testCase.EnumerateAxis(XdmAxis.Child, outputFileNT);
                while (en.MoveNext()) {
                    XdmNode outputFile = (XdmNode)en.Current;
                    String fileName = testSuiteDir + "ExpectedTestResults/" + filePath + outputFile.StringValue;
                    String comparator = outputFile.GetAttributeValue(compareAtt);
                    if (comparator.Equals("Inspect")) {
                        matched = true;
                        results.WriteLine("<test-case name='" + testName + "' result='inspect'/>");
                        break;
                    } else {
                        matched = compare(outputPath, fileName, comparator);
                        if (matched) {
                            results.WriteLine("<test-case name='" + testName + "' result='pass'/>");
                            break;
                        }
                    }
                }

                if (!matched) {
                    string expectedError = "";
                    IEnumerator ee = testCase.EnumerateAxis(XdmAxis.Child, expectedErrorNT);
                    while (ee.MoveNext()) {
                        XdmNode error = (XdmNode)ee.Current;
                        String expectedErrorCode = error.StringValue;
                        expectedError += (expectedErrorCode + " ");
                    }

                    if (expectedError.Equals("")) {
                        Console.WriteLine("Results differ from expected results");
                        results.WriteLine("<test-case name='" + testName + "' result='fail'/>");
                    } else {
                        Console.WriteLine("Error " + expectedError + "expected but not reported");
                        results.WriteLine("<test-case name='" + testName + "' result='fail' comment='expected error " + expectedError + "not reported'/>");
                    }
                }
            }
        }

        results.WriteLine("</test-suite-result>");
        results.Close();

    }

    protected string getResultDirectoryName() {
        return "SaxonResults";
    }


    /**
     * Construct source object. This method allows subclassing e.g. to build a DOM or XOM source.
     * @param xml
     * @return
     * @throws XPathException
     */

    protected XdmNode buildSource(XdmNode catalog, DocumentBuilder builder, String sourceName) {
        
        // Find the source element in the catalog

        XPathSelector xps = findSourcePath.Load();
        xps.SetVariable(new QName("", "param"), new XdmAtomicValue(sourceName));
        xps.ContextItem = catalog;
        XdmNode source = (XdmNode)xps.EvaluateSingle();

        // decide whether schema validation is needed

        bool validate = source.GetAttributeValue(new QName("", "schema")) != null;
        if (validate) {
            builder.SchemaValidationMode = SchemaValidationMode.Strict;
        } else {
            builder.SchemaValidationMode = SchemaValidationMode.None;
        }

        // build the document tree from the source file

        string filename = testSuiteDir + source.GetAttributeValue(new QName("", "FileName"));
        if (source == null) {
            throw new ArgumentException("Source " + sourceName + " not found in catalog");
        }
        return builder.Build(new Uri(filename));
    }

    // Compare actual results file with reference results

    private bool compare(String outfile, String reffile, String comparator) {
        //Console.WriteLine("Comparing " + outfile + " with " + reffile);
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
            if (contents1[0] == '\u00ef' && contents1[1] == '\u00bb' && contents1[2] == '\u00bf') {
                offset1 += 3;
            }
            if (contents2[0] == '\u00ef' && contents2[1] == '\u00bb' && contents2[2] == '\u00bf') {
                offset2 += 3;
            }
            actResult = (size1 == -1 ? "" : new String(contents1, offset1, size1 - offset1));
            refResult = (size2 == -1 ? "" : new String(contents2, offset2, size2 - offset2));

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

        if (comparator.Equals("Text") || comparator.Equals("Fragment")) {
            return compareFragments(actResult, refResult);

        } else if (comparator.Equals("html-output")) {

            Console.WriteLine("*** Compare HTML outputs by hand");

        } else if (comparator.Equals("xhtml-output")) {
            refResult = canonizeXhtml(processor, refResult);
            actResult = canonizeXhtml(processor, actResult);
            return (actResult.Equals(refResult));

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
                new FileStream(testSuiteDir + "SaxonResults/canonizeXhtml.xsl", FileMode.Open));
        }
        return xhtmlCanonizer;
    }

    /// <summary>
    /// Compare XML fragments
    /// </summary>
    /// <param name="actual">Actual results (the results, not the filename)</param>
    /// <param name="gold">Reference results (the results, not the filename)</param>
    /// <returns>true if equivalent</returns>

    private bool compareFragments(String actual, String gold) {

        String a = "<d>" + expandSpecialChars(actual) + "</d>";
        String g = "<d>" + expandSpecialChars(gold) + "</d>";
        XdmNode doc1;
        try {
            doc1 = processor.NewDocumentBuilder().Build(
                new XmlTextReader(new StringReader(a)));
        } catch (Exception e) {
            //Console.WriteLine(e.StackTrace);
            Console.WriteLine("*** Error parsing actual results " + e.Message);
            Console.WriteLine(a);
            return false;
        }
        XdmNode doc2;
        try {
            doc2 = processor.NewDocumentBuilder().Build(
                new XmlTextReader(new StringReader(g)));
        } catch (Exception e) {
            //Console.WriteLine(e.StackTrace);
            Console.WriteLine("*** Error parsing gold results " + e.Message);
            Console.WriteLine(g);
            return false;
        }
        try {
            XPathSelector t = compareDocuments.Load();
            t.SetVariable(new QName("", "actual"), doc1);
            t.SetVariable(new QName("", "gold"), doc2);
            t.SetVariable(new QName("", "debug"), new XdmAtomicValue(debug));
            XdmAtomicValue result = (XdmAtomicValue)t.EvaluateSingle();
            return (bool)result.Value;
        } catch (Exception e) {
            //Console.WriteLine(e.StackTrace);
            Console.WriteLine("*** Error comparing results " + e.Message);
            return false;
        }
    }

    private String expandSpecialChars(String s) {
        StringBuilder sb = new StringBuilder();
        int start = 0;
        if (s.StartsWith("<?xml")) {
            start = s.IndexOf("?>") + 2;
        }
        for (int i = start; i < s.Length; i++) {
            char c = s[i];
            if (c < 127) {
                sb.Append(c);
            } else if (c >= 55296 && c <= 56319) {
                // we'll trust the data to be sound
                int charval = ((c - 55296) * 1024) + ((int)s[i + 1] - 56320) + 65536;
                sb.Append("&#" + charval + ";");
                i++;
            } else {
                sb.Append("&#" + ((int)c) + ";");
            }
        }
        return sb.ToString();
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

    /// <summary>
    /// Compare two files using the XML comparator.
    /// </summary>
    /// <param name="actual">Filename of results obtained in this test run</param>
    /// <param name="gold">Filename of reference results (expected results)</param>
    /// <returns>true if the results are the same</returns>

    private bool compareXML(String actual, String gold) {
        try {
            XdmNode doc1 = processor.NewDocumentBuilder().Build(new Uri(actual));
            XdmNode doc2 = processor.NewDocumentBuilder().Build(new Uri(gold));
            XPathSelector t = compareDocuments.Load();
            t.SetVariable(new QName("", "actual"), doc1);
            t.SetVariable(new QName("", "gold"), doc2);
            t.SetVariable(new QName("", "debug"), new XdmAtomicValue(debug));
            XdmAtomicValue result = (XdmAtomicValue)t.EvaluateSingle();
            return (bool)result.Value;
        } catch (Exception e) {
            Console.WriteLine(e.StackTrace);
            Console.WriteLine("***" + e.Message);
            return false;
        }
    }

    private IList getCollection(XdmNode collectionNode) {
        ArrayList list = new ArrayList(10);
        IEnumerator e = collectionNode.EnumerateAxis(
            XdmAxis.Child, new QName(testURI, "input-document"));
        while (e.MoveNext()) {
            XdmNode node = (XdmNode)e.Current;
            list.Add(new Uri(testSuiteDir + "TestSources/" + node.StringValue + ".xml"));
        }
        return list;
    }

    // Implementation of IQueryResolver used to locate library modules

    private class XqtsModuleResolver : IQueryResolver {

        private XdmNode testCase;
        private XPathExecutable findModule;

        public XqtsModuleResolver(XdmNode testCase, XPathExecutable findModule) {
            this.testCase = testCase;
            this.findModule = findModule;
        }

        public Uri[] GetModules(String moduleUri, Uri baseUri, String[] locationHints) {
            XPathSelector xps = findModule.Load();
            xps.SetVariable(new QName("", "testcase"), testCase);
            xps.SetVariable(new QName("", "moduleuri"), new XdmAtomicValue(moduleUri));
            XdmAtomicValue s = (XdmAtomicValue)xps.EvaluateSingle();
            return new Uri[] { new Uri((String)s.Value) };
        }

        public Object GetEntity(Uri absoluteUri) {
            String u = absoluteUri.ToString();
            if (u.StartsWith("file:///")) {
                u = u.Substring(8);
            } else if (u.StartsWith("file:/")) {
                u = u.Substring(6);
            }
            return new FileStream(u, FileMode.Open, FileAccess.Read, FileShare.Read);
        }
    }
        



}


