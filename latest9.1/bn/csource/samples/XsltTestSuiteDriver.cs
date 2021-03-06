﻿using System;
using System.IO;
using System.Collections;
using System.Xml;
using Saxon.Api;
using TestRunner;

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


public class XsltTestSuiteDriver
{
    static void MainXXX(string[] args)
    {
        if (args.Length == 0 || args[0].Equals("-?"))
        {
            Console.WriteLine("XsltTestSuiteDriver testsuiteDir testName?");
        }

        new XsltTestSuiteDriver().go(args);
    }

    String testSuiteDir;
    Processor processor = new Processor();    
    Processor schemaAwareProcessor; /* = new Processor(true); */
    IFeedbackListener feedback;
    FileComparer fileComparer;

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

    public void setFeedbackListener(IFeedbackListener f) {
        feedback = f;
    }

    private XdmNode getChildElement(XdmNode parent, QName child)
    {
        IEnumerator e = parent.EnumerateAxis(XdmAxis.Child, child);
        return e.MoveNext() ? (XdmNode)e.Current : null;
    }


    public void go(String[] args)
    {
        int passed = 0;
        int failed = 0;
        int total = 0;

        Console.WriteLine("Testing Saxon " + processor.ProductVersion);
        testSuiteDir = args[0];
        if (testSuiteDir.EndsWith("/"))
        {
            testSuiteDir = testSuiteDir.Substring(0, testSuiteDir.Length - 1);
        }
        Hashtable exceptions = new Hashtable();

        if (args.Length > 1)
        {
            testPattern = (args[1]); 
        }

        for (int i = 0; i < args.Length; i++)
        {
            if (args[i].Equals("-w"))
            {
                //showWarnings = true;
            }
        }

        try
        {
            schemaAwareProcessor = new Processor(true);
        }
        catch (Exception err)
        {
            Console.WriteLine("Cannot load Saxon-SA: continuing without it");
        }

        processor.SetProperty("http://saxon.sf.net/feature/preferJaxpParser", "true");

        if (schemaAwareProcessor != null)
        {
            schemaAwareProcessor.SetProperty("http://saxon.sf.net/feature/preferJaxpParser", "true");
        }
        fileComparer = new FileComparer(processor, testSuiteDir);

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


        QName fileAtt = new QName("", "file");
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
        while (exceptionTestCases.MoveNext())
        {
            XdmNode n = (XdmNode)exceptionTestCases.Current;
            String nameAttVal = n.GetAttributeValue(nameAtt);
            char[] seps = { ' ', '\n', '\t' };
            String[] parts = nameAttVal.Split(seps);
            foreach (string p in parts)
            {
                if (!exceptions.ContainsKey(p))
                {
                    exceptions.Add(p, "Kilroy");
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

        total = 0;
        IEnumerator testCases = catalog.EnumerateAxis(XdmAxis.Descendant, testCaseNT);
        while (testCases.MoveNext())
        {
            total++;
        }

        testCases = catalog.EnumerateAxis(XdmAxis.Descendant, testCaseNT);
        while (testCases.MoveNext())
        {
            bool useAssociated = false;
            XdmNode testCase = (XdmNode)testCases.Current;

            String testName = getChildElement(testCase, nameNT).StringValue;
            if (testPattern != null && !testName.StartsWith(testPattern))
            {
                continue;
            }
            if (exceptions.ContainsKey(testName))
            {
                continue;
            }
            if (isExcluded(testName))
            {
                continue;
            }
            Console.WriteLine("Test " + testName);
            XdmNode testInput = getChildElement(testCase, inputNT);

            XdmNode stylesheet = getChildElement(testInput, stylesheetNT);
            String absXSLName = null;
            if (stylesheet == null)
            {
                useAssociated = true;
            }
            else
            {
                absXSLName = testSuiteDir + "/TestInputs/" + stylesheet.GetAttributeValue(fileAtt);
            }

            XdmNode sourceDocument = getChildElement(testInput, sourceDocumentNT);
            String absXMLName = null;
            if (sourceDocument != null)
            {
                absXMLName = testSuiteDir + "/TestInputs/" + sourceDocument.GetAttributeValue(fileAtt);
            }

            bool schemaAware = false;
            bool recoverRecoverable = true;
            bool backwardsCompatibility = true;
            bool supportsDOE = true;
            bool recoverSESU0007 = false;
            XdmNode discretionaryItems = getChildElement(testCase, discretionaryItemsNT);
            if (discretionaryItems != null)
            {
                IEnumerator features = discretionaryItems.EnumerateAxis(XdmAxis.Child, discretionaryFeatureNT);
                while (features.MoveNext())
                {
                    XdmNode feature = (XdmNode)features.Current;
                    String featureName = feature.GetAttributeValue(nameAtt);
                    if ("schema_aware".Equals(featureName))
                    {
                        schemaAware = "on".Equals(feature.GetAttributeValue(behaviorAtt));
                    }
                    else if ("XML_1.1".Equals(featureName))
                    {
                        xml11 = "on".Equals(feature.GetAttributeValue(behaviorAtt));
                    }
                    else if ("backwards_compatibility".Equals(featureName))
                    {
                        backwardsCompatibility = "on".Equals(feature.GetAttributeValue(behaviorAtt));
                    }
                    else if ("disabling_output_escaping".Equals(featureName))
                    {
                        supportsDOE = "on".Equals(feature.GetAttributeValue(behaviorAtt));
                    }
                }
                IEnumerator choices = discretionaryItems.EnumerateAxis(
                        XdmAxis.Child, discretionaryChoiceNT);
                while (choices.MoveNext())
                {
                    XdmNode choice = (XdmNode)choices.Current;
                    String featureName = choice.GetAttributeValue(nameAtt);
                    if ("error".Equals(choice.GetAttributeValue(behaviorAtt)))
                    {
                        recoverRecoverable = false;
                    }
                    else if ("SESU0007".Equals(featureName))
                    {
                        recoverSESU0007 = "recovery".Equals(choice.GetAttributeValue(behaviorAtt));
                    }
                }
            }

            if (!backwardsCompatibility)
            {
                // Saxon cannot run with BC switched off
                results.WriteLine(" <testcase name='" + testName + "' result='not run' comment='requires backwards-compatibility=off'/>");
                continue;
            }

            if (!supportsDOE)
            {
                // Saxon cannot run with DOE switched off
                results.WriteLine(" <testcase name='" + testName + "' result='not run' comment='requires disable-output-escaping=off'/>");
                continue;
            }

            if (recoverSESU0007)
            {
                // Saxon cannot recover from error SESU0007
                results.WriteLine(" <testcase name='" + testName + "' result='not run' comment='requires recovery from error SESU0007'/>");
                continue;
            }

            XdmNode initialMode = getChildElement(testInput, initialModeNT);
            QName initialModeName = null;
            if (initialMode != null)
            {
                String ini = initialMode.GetAttributeValue(qnameAtt);
                initialModeName = makeQName(ini, initialMode);
            }

            XdmNode initialTemplate = getChildElement(testInput, entryNamedTemplateNT);
            QName initialTemplateName = null;
            if (initialTemplate != null)
            {
                String ini = initialTemplate.GetAttributeValue(qnameAtt);
                initialTemplateName = makeQName(ini, initialTemplate);
            }

            XdmNode initialContextNode = getChildElement(testInput, initialContextNodeNT);
            String initialContextPath = null;
            if (initialContextNode != null)
            {
                initialContextPath = initialContextNode.StringValue;
            }

            XdmNode validation = getChildElement(testInput, validationNT);
            String validationMode = null;
            if (validation != null)
            {
                validationMode = validation.GetAttributeValue(modeAtt);
            }

            Hashtable paramTable = null;
            XdmNode paramList = getChildElement(testInput, stylesheetParametersNT);
            if (paramList != null)
            {
                paramTable = new Hashtable(5);
                IEnumerator paramIter = paramList.EnumerateAxis(XdmAxis.Child, paramNT);
                while (paramIter.MoveNext())
                {
                    XdmNode param = (XdmNode)paramIter.Current;
                    QName name = makeQName(param.GetAttributeValue(qnameAtt), param);
                    String value = param.StringValue;
                    paramTable.Add(name, value);
                }
            }

            IEnumerator schemas = testInput.EnumerateAxis(XdmAxis.Child, schemaNT);
            while (schemas.MoveNext())
            {
                XdmNode schema = (XdmNode)schemas.Current;
                if (schema == null)
                {
                    break;
                }
                String role = schema.GetAttributeValue(roleAtt);
                if (("source-validator".Equals(role) || "source-reference".Equals(role))
                    /* && schema.GetAttributeValue(validatesAtt) != null */)
                {
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
            if (resultDocument != null)
            {
                String relativePath = resultDocument.GetAttributeValue(fileAtt);
                int slash = relativePath.IndexOf('/');
                if (slash > 0)
                {
                    String relativeDir = relativePath.Substring(0, slash);
                    String fullDir = testSuiteDir + '/' + getResultDirectoryName() + ".net/" + relativeDir;
                    if (!Directory.Exists(fullDir))
                    {
                        Directory.CreateDirectory(fullDir);
                    }
                }
                refFileName = testSuiteDir + "/ExpectedTestResults/" + relativePath;
                outFileName = testSuiteDir + '/' + getResultDirectoryName() + ".net/" + relativePath;
                comparator = resultDocument.GetAttributeValue(typeAtt);
            }
            else
            {
                outFileName = testSuiteDir + '/' + getResultDirectoryName() + ".net/temp.out";
            }
            XdmNode error = getChildElement(testOutput, errorNT);
            String expectedError = null;
            if (error != null)
            {
                expectedError = error.GetAttributeValue(errorIdAtt);
            }
            bool success;
            Exception xsltOutcome =
                runXSLT(testName, absXMLName, absXSLName, initialModeName, initialTemplateName,
                    outFileName, paramTable, initialContextPath,
                    useAssociated, schemaAware, validationMode, recoverRecoverable);
            if (xsltOutcome == null)
            {
                success = true;
                if (expectedError != null && resultDocument == null)
                {
                    Console.WriteLine("Test failed. Expected error " + expectedError + ", got success");
                    feedback.Feedback(passed, failed++, total);
                    success = false;
                    results.WriteLine(" <testcase name='" + testName +
                            "' result='differ' comment='Expected error " +
                            expectedError + ", got success'/>");
                }
                else
                {
                    feedback.Feedback(passed++, failed, total);
                }
            }
            else
            {
                String errorCode = null;
                if (xsltOutcome is StaticError)
                {
                    errorCode = ((StaticError)xsltOutcome).ErrorCode.LocalName;
                }
                else if (xsltOutcome is DynamicError)
                {
                    errorCode = ((DynamicError)xsltOutcome).ErrorCode.LocalName;
                }
                if (expectedError != null && errorCode != null && errorCode.Equals(expectedError))
                {
                    feedback.Feedback(passed++, failed, total);
                    Console.WriteLine("Test succeeded (" + expectedError + ')');
                    results.WriteLine(" <testcase name='" + testName +
                            "' result='full' comment='Error " +
                            expectedError + " as expected'/>");
                }
                else if (expectedError != null)
                {
                    feedback.Feedback(passed++, failed, total);
                    Console.WriteLine("Test succeeded (??) (expected " + expectedError + ", got " + errorCode + ')');
                    results.WriteLine(" <testcase name='" + testName +
                            "' result='different-error' comment='Expected " +
                            expectedError + " got " + errorCode + "'/>");
                }
                else
                {
                    feedback.Feedback(passed, failed++, total);
                    Console.WriteLine("Test failed. Expected success, got " + errorCode);
                    results.WriteLine(" <testcase name='" + testName +
                            "' result='differ' comment='Expected success, got " +
                            errorCode + "'/>");
                    results.WriteLine(" <!--" + xsltOutcome.Message + "-->");
                }
                success = false;
                continue;
            }


            if (success)
            {
                String outcome = fileComparer.compare(outFileName, refFileName, comparator);
                if (outcome == "OK")
                {
                    results.WriteLine(" <testcase name='" + testName + "' result='full'/>");
                }
                else if (outcome.StartsWith("#"))
                {
                    results.WriteLine(" <testcase name='" + testName + "' result='full' + comments='" + outcome.Substring(1) + "/>");
                }
                else
                {
                    results.WriteLine(" <testcase name='" + testName + "' result='differ' comments='" + outcome + "'/>");
                }
            }

        }

        results.WriteLine("</test-suite-result>");
        results.Close();

        //} 
    }

    private static QName makeQName(String lexical, XdmNode element)
    {
        if (lexical.IndexOf(":") >= 0)
        {
            return new QName(lexical, element);
        }
        else
        {
            return new QName("", lexical);
        }
    }

    protected string getResultDirectoryName()
    {
        return "SaxonResults";
    }

    protected bool isExcluded(String testName)
    {
        return false;
    }

    /// <summary>
    /// Run the transformation
    /// </summary>
    /// <param name="testName"></param>
    /// <param name="xml"></param>
    /// <param name="xsl"></param>
    /// <param name="initialMode"></param>
    /// <param name="initialTemplate"></param>
    /// <param name="outfile"></param>
    /// <param name="paramTable"></param>
    /// <param name="initialContextPath"></param>
    /// <param name="useAssociated"></param>
    /// <param name="schemaAware"></param>
    /// <param name="validationMode"></param>
    /// <param name="recoverRecoverable"></param>
    /// <returns>Either null, indicating success, or an Exception object with information about the failure</returns>

    protected Exception runXSLT(String testName, String xml, String xsl, QName initialMode,
                           QName initialTemplate, String outfile, Hashtable paramTable, String initialContextPath,
                           bool useAssociated, bool schemaAware,
                           String validationMode, bool recoverRecoverable)
    {
        Serializer sr = new Serializer();
        sr.SetOutputFile(outfile);
        Processor f;
        //if (noCacheTests.contains(testName) || testName.startsWith("schemaas20") ||
        //        testName.startsWith("striptype20") || testName.startsWith("notation20")) {
        // create a custom Processor to avoid schema caching
        //} else {
        if (schemaAware)
        {
            f = schemaAwareProcessor;
            if (f == null)
            {
                return new DynamicError("Saxon-SA not available");
            }
        }
        else if (xml11)
        {
            f = processor;
            // Use an Xml 1.1 processor
        }
        else
        {
            f = processor;
        }
        //}

        XdmNode source = null;

        IList errors = new ArrayList();
        XsltCompiler compiler = f.NewXsltCompiler();
        compiler.ErrorList = errors;
        XsltExecutable sheet = null;
        XsltTransformer inst;

        if (useAssociated)
        {
            try
            {
                source = buildSource(f.NewDocumentBuilder(), xml, validationMode);
            }
            catch (Exception e)
            {
                Console.WriteLine("Failed to build source document: " + e.Message);
                return e;
            }
            try
            {
                sheet = compiler.CompileAssociatedStylesheet(source);
            }
            catch (Exception e)
            {
                Console.WriteLine("Failed to compile stylesheet: " + e.Message);
                if (errors.Count > 0)
                {
                    return (Exception)errors[0];
                    //QName code = ((StaticError)errors[0]).ErrorCode;
                    //(code == null ? "Failed to compile stylesheet: " + e.Message : code.LocalName);
                }
                else
                {
                    return e;
                }
            }
        }
        else
        {
            Stream stream = new FileStream(xsl, FileMode.Open, FileAccess.Read);
            compiler.BaseUri = new Uri(xsl);
            try
            {
                sheet = compiler.Compile(stream);
            }
            catch (Exception e)
            {
                if (errors.Count > 0)
                {
                    return ((StaticError)errors[0]);
                }
                else
                {
                    Console.WriteLine(e.Message);
                    return e;
                }
            }
            finally
            {
                stream.Close();
            }
        }
        if (source == null && xml != null)
        {
            try
            {
                source = buildSource(f.NewDocumentBuilder(), xml, validationMode);
            }
            catch (Exception e)
            {
                Console.WriteLine("Failed to build source document: " + e.Message);
                return e;
            }
        }
        if (initialContextPath != null)
        {
            XPathCompiler xc = f.NewXPathCompiler();
            XPathExecutable exp = xc.Compile(initialContextPath);
            XPathSelector xpe = exp.Load();
            xpe.ContextItem = source;
            XdmNode node = (XdmNode)xpe.EvaluateSingle();
            source = node;
        }

        inst = sheet.Load();
        if (initialMode != null)
        {
            inst.InitialMode = initialMode;
        }
        if (initialTemplate != null)
        {
            try
            {
                inst.InitialTemplate = initialTemplate;
            }
            catch (DynamicError e)
            {
                return e;
            }
        }
        if (paramTable != null)
        {
            foreach (DictionaryEntry de in paramTable)
            {
                inst.SetParameter((QName)de.Key, new XdmAtomicValue(de.Value.ToString()));
            }
        }
        inst.InitialContextNode = source;

        inst.RecoveryPolicy = recoverRecoverable ? RecoveryPolicy.RecoverSilently : RecoveryPolicy.DoNotRecover;

        //inst.setURIResolver(factory.getURIResolver());
        //inst.setErrorListener(errorListener);
        //((Controller)inst).setRecoveryPolicy(recoverRecoverable ? Configuration.RECOVER_SILENTLY : Configuration.DO_NOT_RECOVER);
        // To avoid test results being dependent on the date and time (and timezone), set a fixed
        // date and time for the run
        //((Controller)inst).setCurrentDateTime(new DateTimeValue("2005-01-01T12:49:30.5+01:00"));

        try
        {
            inst.Run(sr);
        }
        catch (DynamicError e)
        {
            Console.WriteLine(e.Message);
            return e;
        }
        return null;    // indicating success
    }

    /**
     * Construct source object. This method allows subclassing e.g. to build a DOM or XOM source.
     * @param xml
     * @return
     * @throws XPathException
     */

    protected XdmNode buildSource(DocumentBuilder builder, String xml, String validationMode)
    {
        if ("strict".Equals(validationMode))
        {
            builder.SchemaValidationMode = SchemaValidationMode.Strict;
        }
        else
        {
            builder.SchemaValidationMode = SchemaValidationMode.None;
        }
        return builder.Build(new Uri(xml));
    }


    private void outputDiscretionaryItems()
    {
        results.WriteLine("  <discretionary-items/>");
    }




}


