////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
// Copyright (c) 2014 Saxonica Limited.
// This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
// If a copy of the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
// This Source Code Form is “Incompatible With Secondary Licenses”, as defined by the Mozilla Public License, v. 2.0.
////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

package net.sf.saxon.testdriver;


import net.sf.saxon.Version;
import net.sf.saxon.dom.DOMObjectModel;
import net.sf.saxon.lib.FeatureKeys;
import net.sf.saxon.om.TreeModel;
import net.sf.saxon.s9api.*;

import javax.xml.stream.XMLStreamException;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

public abstract class TestDriver {

    protected String resultsDir = null;
    protected TestReport resultsDoc;
    protected int successes = 0;
    protected int failures = 0;
    protected int notrun = 0;
    protected int wrongErrorResults = 0;
    protected TestDriverShell shell = new TestDriverShell();
    protected boolean unfolded = false;
    protected boolean saveResults = false;
    protected int generateByteCode = 0;
    protected int streaming = 1;
    protected TreeModel treeModel = TreeModel.TINY_TREE;
    protected boolean debug = false;
    protected Pattern testPattern = null;
    protected String requestedTestSet = null;
    protected String testSuiteDir;
    protected Processor driverProc = null;
    protected Serializer driverSerializer = null;
    protected HashMap<String, XdmNode> exceptionsMap = new HashMap<String, XdmNode>();
    protected HashMap<String, String> optimizationAssertions = new HashMap<String, String>();
    protected Map<String, Environment> globalEnvironments = new HashMap<String, Environment>();
    protected Map<String, Environment> localEnvironments = new HashMap<String, Environment>();
    protected Spec spec;
    protected String lang;
    protected boolean useXslt30Transformer = true;  // Temporary for controlling test processor

    public abstract String catalogNamespace();

    public void go(String[] args) throws Exception {

        if (driverProc == null) {
            driverProc = new Processor(false);
        }
        driverSerializer = driverProc.newSerializer();

        System.err.println("Testing " + getProductEdition() + " " + Version.getProductVersion());
        System.err.println("Java version "+System.getProperty("java.version"));

        testSuiteDir = args[0];
        String catalog = args[1];

        for (int i = 2; i < args.length; i++) {
            if (args[i].startsWith("-t:")) {
                testPattern = Pattern.compile(args[i].substring(3));
            }
            if (args[i].startsWith("-s:")) {
                requestedTestSet = args[i].substring(3);
            }
            if (args[i].startsWith("-o")) {
                resultsDir = args[i].substring(3);
            }
            if (args[i].startsWith("-debug")) {
                debug = true;
            }
            if (args[i].equals("-unfolded")) {
                unfolded = true;
            }
            if (args[i].equals("-save")) {
                saveResults = true;
            }
            if (args[i].startsWith("-bytecode")) {
                if (args[i].substring(10).equals("on")) {
                    generateByteCode = 1;
                } else if (args[i].substring(10).equals("debug")) {
                    generateByteCode = 2;
                } else {
                    generateByteCode = 0;
                }
            }
            if (args[i].startsWith("-streaming")) {
                if (args[i].substring(11).equals("off")) {
                    streaming = 0;
                } else if (args[i].substring(11).equals("std")) {
                    streaming = 1;
                } else if (args[i].substring(11).equals("ext")) {
                    streaming = 2;
                }
            }
            if (args[i].startsWith("-tree")) {
                String model = args[i].substring(6);
                treeModel = getTreeModel(model);
                if (treeModel == null) {
                    throw new Exception("The requested TreeModel '" + model + "' does not exist");
                }

            }
            if (args[i].startsWith("-lang")) {
                String specStr = null;
                specStr = args[i].substring(6);
                lang = specStr;
                processSpec(specStr);
            }
            // Temporary for controlling test processor
            if (args[i].startsWith("-xt30")) {
                if (args[i].substring(6).equals("on")) {
                    useXslt30Transformer = true;
                } else if (args[i].substring(6).equals("off")) {
                    useXslt30Transformer = false;
                }
            }
        }
        if (resultsDoc == null) {
            printError("No result document: missing -lang option", "");
            if (shell == null) {
                System.exit(2);
            }
        }
        if (resultsDir == null) {
            printError("No results directory specified (use -o:dirname)", "");
            if (shell == null) {
                System.exit(2);
            }
        }

        System.err.println("UsingXslt30Transformer: " + useXslt30Transformer);

        driverSerializer.setOutputStream(System.err);
        driverSerializer.setOutputProperty(Serializer.Property.OMIT_XML_DECLARATION, "yes");
        processCatalog(new File(catalog));
        printResults(resultsDir + "/results" + Version.getProductVersion() + ".xml");
    }

    /**
     * Return the appropriate tree model to use
     * @param s  The name of the tree model required
     * @return  The tree model - null if model requested is unrecognised
     */
    protected TreeModel getTreeModel(String s){
        TreeModel tree = null;
        if (s.equalsIgnoreCase("dom")) {
            tree = new DOMObjectModel();
        } else if (s.equalsIgnoreCase("tinytree")) {
            tree = TreeModel.TINY_TREE;
        } else if (s.equalsIgnoreCase("condensed")) {
            tree = TreeModel.TINY_TREE_CONDENSED;
        } else if (s.equalsIgnoreCase("linked")) {
            tree = TreeModel.LINKED_TREE;
        }
        return tree;
    }

    public String getResultsDir() {
        return resultsDir;
    }

    public abstract void processSpec(String specStr);

    public boolean isByteCode() {
        return generateByteCode != 0;
    }

    public String getProductEdition() {
        return "Saxon-" + driverProc.getSaxonEdition();
    }


    protected void processCatalog(File catalogFile) throws SaxonApiException {
        if (driverProc.getSaxonEdition().equals("EE")) {
            if (generateByteCode == 1) {
                driverProc.setConfigurationProperty(FeatureKeys.GENERATE_BYTE_CODE, "true");
                driverProc.setConfigurationProperty(FeatureKeys.DEBUG_BYTE_CODE, "false");
            } else if (generateByteCode == 2) {
                driverProc.setConfigurationProperty(FeatureKeys.GENERATE_BYTE_CODE, "true");
                driverProc.setConfigurationProperty(FeatureKeys.DEBUG_BYTE_CODE, "true");
            } else {
                driverProc.setConfigurationProperty(FeatureKeys.GENERATE_BYTE_CODE, "false");
                driverProc.setConfigurationProperty(FeatureKeys.DEBUG_BYTE_CODE, "false");
            }
        }
        DocumentBuilder catbuilder = driverProc.newDocumentBuilder();
        catbuilder.setTreeModel(treeModel);
        XdmNode catalog = catbuilder.build(catalogFile);
        XPathCompiler xpc = driverProc.newXPathCompiler();
        xpc.setLanguageVersion("3.0");
        xpc.setCaching(true);
        xpc.declareNamespace("", catalogNamespace());
        xpc.setBaseURI(catalogFile.toURI());

        createGlobalEnvironments(catalog, xpc);

        try {
            writeResultFilePreamble(driverProc, catalog);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        readExceptionsFile();


        if (requestedTestSet != null) {
            try {
                XdmNode funcSetNode = (XdmNode) xpc.evaluateSingle("//test-set[@name='" + requestedTestSet + "']", catalog);
                if (funcSetNode == null) {
                    throw new Exception("Test-set " + requestedTestSet + " not found!");
                }
                processTestSet(catbuilder, xpc, funcSetNode);
            } catch (Exception e1) {
                e1.printStackTrace();
            }
        } else {
            for (XdmItem testSet : xpc.evaluate("//test-set", catalog)) {
                processTestSet(catbuilder, xpc, (XdmNode) testSet);
            }
        }
        try {
            writeResultFilePostamble();
        } catch (Exception e) {
            e.printStackTrace();
        }


    }

    /**
     * Look for an exceptions.xml document with the general format:
     * <p/>
     * <exceptions xmlns="...test catalog namespace...">
     * <exception test-set ="testset1" test-case="testcase" run="yes/no/not-unfolded"
     * bug="bug-reference" reason="">
     * <results>
     * ... alternative expected results ...
     * </results>
     * <optimization>
     * ... assertions about the "explain" tree
     * </optimization>
     * </exception>
     * </exceptions>
     */

    protected void readExceptionsFile() {

        XdmNode exceptionsDoc = null;
        DocumentBuilder exceptBuilder = driverProc.newDocumentBuilder();
        QName testCase = new QName("", "test-case");
        QName run = new QName("", "run");
        QName edition = new QName("", "edition");
        String saxonEdition = driverProc.getSaxonEdition();
        try {
            exceptionsDoc = exceptBuilder.build(new File(resultsDir + "/exceptions.xml"));
            XdmSequenceIterator iter = exceptionsDoc.axisIterator(Axis.DESCENDANT, new QName("", "exception"));
            while (iter.hasNext()) {
                XdmNode entry = (XdmNode) iter.next();
                String testName = entry.getAttributeValue(testCase);
                String runVal = entry.getAttributeValue(run);
                String editionVal = entry.getAttributeValue(edition);
                if (runVal == null) {
                    runVal = "false";
                }
                if(editionVal == null) {
                    editionVal = saxonEdition;
                }
                boolean appliesThisEdition = false;
                for(String ed: editionVal.trim().split("\\s+"))  {
                    if(ed.equals(saxonEdition)) {
                        appliesThisEdition = true;
                        break;
                    }
                }
                if (testName != null && appliesThisEdition) {
                    if (runVal.equals("false")) {
                        exceptionsMap.put(testName, entry);
                    } else {
                        XdmSequenceIterator iter2 = entry.axisIterator(Axis.CHILD, new QName("optimization"));
                        if (iter2.hasNext()) {
                            XdmNode optim = (XdmNode) iter2.next();
                            optimizationAssertions.put(testName, optim.getAttributeValue(new QName("", "assert")));
                        }
                    }
                }
            }
        } catch (SaxonApiException e) {
            printError("*** Failed to process exceptions file: ", e.getMessage());
        }

    }

    protected abstract void createGlobalEnvironments(
            XdmNode catalog, XPathCompiler xpc)
            throws SaxonApiException;

    protected void createLocalEnvironments(XdmNode testSetDocNode) {
        localEnvironments.clear();
        Environment defaultEnvironment =
                Environment.createLocalEnvironment(testSetDocNode.getBaseURI(), generateByteCode, unfolded, spec, this);
        localEnvironments.put("default", defaultEnvironment);
    }

    protected Environment getEnvironment(XdmNode testCase, XPathCompiler xpc) throws SaxonApiException {
        String testCaseName = testCase.getAttributeValue(new QName("name"));
        XdmNode environmentNode = (XdmNode) xpc.evaluateSingle("environment", testCase);
        Environment env;
        if (environmentNode == null) {
            env = localEnvironments.get("default");
        } else {
            String envName = environmentNode.getAttributeValue(new QName("ref"));
            if (envName == null || envName.equals("")) {
                boolean baseUriCheck = false;
                env = null;
                try {
                    env = Environment.processEnvironment(this, xpc, environmentNode, null, localEnvironments.get("default"));
                    baseUriCheck = ((XdmAtomicValue) xpc.evaluateSingle("static-base-uri/@uri='#UNDEFINED'", environmentNode)).getBooleanValue();
                } catch (NullPointerException ex) {
                    // ex.printStackTrace();
                    System.err.println("Failure loading environment");
                    if(env != null) {
                        env.usable = false;
                    }
                }
                if (baseUriCheck) {
                    //writeTestcaseElement(testCaseName, "notRun", "static-base-uri not supported", null);
                    return null;
                }
            } else {
                env = localEnvironments.get(envName);
                if (env == null) {
                    env = globalEnvironments.get(envName);
                }
                if (env == null) {
                    for (XdmItem e : xpc.evaluate("//environment[@name='" + envName + "']", testCase)) {
                        Environment.processEnvironment(this, xpc, e, localEnvironments, localEnvironments.get("default"));
                    }
                    env = localEnvironments.get(envName);
                }
                if (env == null) {
                    println("*** Unknown environment " + envName);
                    //writeTestcaseElement(testCaseName, "fail", "Environment " + envName + " not found", null);
                    failures++;
                    return null;
                }

            }
        }
        return env;
    }

    /**
     * Inject code into a compiled query
     * @param compiler the query compiler
     */

    public void addInjection(XQueryCompiler compiler) {
        // added in subclasses
    }

    protected void writeResultFilePreamble(Processor processor, XdmNode catalog)
            throws IOException, SaxonApiException, XMLStreamException, Exception {
        resultsDoc.writeResultFilePreamble(processor, catalog);
    }

    protected void writeResultFilePostamble()
            throws XMLStreamException {
        resultsDoc.writeResultFilePostamble();
    }

    protected void startTestSetElement(XdmNode testSetNode) {
        resultsDoc.startTestSetElement(testSetNode);
    }

    protected void writeTestSetEndElement() {
        resultsDoc.endElement();
    }


    private void processTestSet(DocumentBuilder catbuilder, XPathCompiler xpc, XdmNode testSetNode) throws SaxonApiException {
        String testName;
        String testSet;
        startTestSetElement(testSetNode);
        File testSetFile = new File(testSuiteDir + "/" + testSetNode.getAttributeValue(new QName("file")));
        XdmNode testSetDocNode = catbuilder.build(testSetFile);
        createLocalEnvironments(testSetDocNode);
        boolean run = true;
        // TODO: this won't pick up any test-set level dependencies in the XSLT 3.0 catalog
        if (((XdmAtomicValue) xpc.evaluate("exists(/test-set/dependency)", testSetDocNode).itemAt(0)).getBooleanValue()) {
            for (XdmItem dependency : xpc.evaluate("/test-set/dependency", testSetDocNode)) {
                if (!dependencyIsSatisfied((XdmNode) dependency, localEnvironments.get("default"))) {
                    for (XdmItem testCase : xpc.evaluate("//test-case", testSetDocNode)) {
                        String testCaseName = ((XdmNode)testCase).getAttributeValue(new QName("name"));
                        resultsDoc.writeTestcaseElement(testCaseName, "n/a", "test-set dependencies not satisfied");
                        notrun++;
                    }
                    run = false;
                }
            }
        }
        if (run) {
            if (testPattern == null) {
                for (XdmItem env : xpc.evaluate("//environment[@name]", testSetDocNode)) {
                    try {
                        Environment.processEnvironment(this, xpc, env, localEnvironments, localEnvironments.get("default"));
                    } catch (NullPointerException ex) {
                       // ex.printStackTrace();
                        System.err.println("Failure loading environment, processTestSet");
                    }
                }
            }
            testSet = xpc.evaluateSingle("/test-set/@name", testSetDocNode).getStringValue();
            for (XdmItem testCase : xpc.evaluate("//test-case", testSetDocNode)) {

                testName = xpc.evaluateSingle("@name", testCase).getStringValue();
                if (testPattern != null && !testPattern.matcher(testName).matches()) {
                    continue;
                }
                println("-s:" + testSet + " -t:" + testName);

                runTestCase((XdmNode) testCase, xpc);
            }
        }
        writeTestSetEndElement();
    }

    protected abstract void runTestCase(XdmNode testCase, XPathCompiler catalogXpc)
            throws SaxonApiException;

    public void setTestDriverShell(TestDriverShell gui) {
        shell = gui;
    }

    public void println(String data) {
        shell.println(data);
    }

    public void printResults(String resultsFileStr) {
        shell.printResults("Result: " + successes + " successes, " + failures + " failures, " + wrongErrorResults + " incorrect ErrorCode, " + notrun + " not run",
                resultsFileStr, resultsDir);
    }

    public void printError(String error, String message) {
        shell.alert(error);
        shell.println(error + message);
    }

    public void printError(String error, Exception e) {
        shell.alert(error);
        e.printStackTrace();
    }

    public abstract boolean dependencyIsSatisfied(XdmNode dependency, Environment env);

}

