////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
// Copyright (c) 2018 Saxonica Limited.
// This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
// If a copy of the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
// This Source Code Form is "Incompatible With Secondary Licenses", as defined by the Mozilla Public License, v. 2.0.
////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

package net.sf.saxon.testdriver;


import net.sf.saxon.Configuration;
import net.sf.saxon.Version;
import net.sf.saxon.dom.DOMObjectModel;
import net.sf.saxon.lib.Feature;
import net.sf.saxon.lib.FeatureKeys;
import net.sf.saxon.om.TreeModel;
import net.sf.saxon.s9api.*;
import net.sf.saxon.s9api.streams.Steps;
import org.w3c.dom.Document;

import javax.xml.stream.XMLStreamException;
import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.function.Predicate;
import java.util.regex.Pattern;

import static net.sf.saxon.s9api.streams.Predicates.attributeEq;
import static net.sf.saxon.s9api.streams.Predicates.hasAttribute;
import static net.sf.saxon.s9api.streams.Steps.*;
import static net.sf.saxon.s9api.streams.XdmCollectors.asString;

public abstract class TestDriver {

    protected String catalogFileName;
    private XdmNode catalogNode;
    protected String exceptionsFileName = "exceptions.xml";
    protected String resultsDir = null;
    protected TestReport resultsDoc;
    protected int successes = 0;
    protected int failures = 0;
    protected int notrun = 0;
    protected int wrongErrorResults = 0;
    protected TestDriverShell shell = new TestDriverShell();
    protected boolean unfolded = false;
    protected boolean saveResults = false;
    protected boolean runPostureAndSweepTests = true;
    protected int generateByteCode = 1;
    protected boolean debugByteCode = false;
    protected String optimizerFlags = "-";
    protected boolean jitFlag = false;
    protected int streaming = 1;
    protected TreeModel treeModel = TreeModel.TINY_TREE;
    protected boolean debug = false;
    protected boolean export = false;
    protected boolean runWithJS = false;
    protected boolean runWithNodeJS = false;
    protected boolean relocatable = false;
    protected String jsBase = null;
    protected Pattern testPattern = null;
    protected String requestedTestSet = null;
    protected String testSuiteDir;
    protected Processor driverProc = null;
    protected Serializer driverSerializer = null;
    protected HashMap<String, XdmNode> exceptionsMap = new HashMap<String, XdmNode>();
    protected HashMap<String, String> optimizationAssertions = new HashMap<String, String>();
    protected Map<String, Environment> globalEnvironments = new HashMap<String, Environment>();
    protected Map<String, Environment> localEnvironments = new HashMap<String, Environment>();
    protected Map<String, File> queryModules = new HashMap<String, File>();
    protected Spec spec;
    protected String lang;
    protected boolean useXslt30Transformer = true;  // Temporary for controlling test processor
    protected boolean tracing = false;
    protected boolean isAltova = false;
    protected Map<String, Integer> failSummary = new TreeMap<>();
    protected boolean quiet = false;

    static Set<String> unsharedEnvironments = new HashSet<>();
    static {
        unsharedEnvironments.add("import-schema-e01");
        unsharedEnvironments.add("merge002");
    }

    public abstract String catalogNamespace();

    public boolean hasEECapability() {
        return false;
    }

    public void go(String[] args) throws Exception {

        if (driverProc == null) {
            driverProc = new Processor(false);
        }
        driverSerializer = driverProc.newSerializer();

        System.err.println("Testing " + getProductEdition() + " " + Version.getProductVersion());
        System.err.println("Java version " + System.getProperty("java.version"));

        testSuiteDir = args[0];
        String catalog = args[1];
        char separatorChar = '/';
        if (File.separatorChar != '/') {
            separatorChar = '\\';
        }
        if(!testSuiteDir.endsWith(""+separatorChar)) {
            testSuiteDir = testSuiteDir + separatorChar;
        }
        catalogFileName = catalog;
        catalog = testSuiteDir+catalog;

        for (int i = 2; i < args.length; i++) {
            int colon = args[i].indexOf(':');
            String argValue = colon < 0 ? "" : args[i].substring(colon+1);

            if (args[i].startsWith("-t:")) {
                testPattern = Pattern.compile(argValue);
            }
            if (args[i].startsWith("-s:")) {
                requestedTestSet = argValue;
            }
            if (args[i].equals("-quiet")) {
                quiet = true;
            }
            if (args[i].startsWith("-o:")) {
                resultsDir = argValue;
            }
            if (args[i].startsWith("-debug")) {
                debug = true;
            }
            if (args[i].startsWith("-export") && !argValue.equals("off")) {
                export = true;
            }
            if (args[i].startsWith("-jit") && !argValue.equals("off")) {
                jitFlag = true;
            }
            if (args[i].startsWith("-js")) {
                export = true;
                runWithJS = true;
                treeModel = DOMObjectModel.getInstance();
                if (args[i].startsWith("-js:") || args[i].startsWith("-js2:")) {
                    jsBase = argValue;
                }
            }
            if (args[i].startsWith("-nodeJS")) {
                export = true;
                runWithJS = true;
                runWithNodeJS = true;
                treeModel = DOMObjectModel.getInstance();
            }
            if (args[i].equals("-unfolded")) {
                unfolded = true;
            }
            if (args[i].equals("-save")) {
                saveResults = true;
            }
            if (args[i].startsWith("-exceptions:")) {
                exceptionsFileName = argValue;
            }
            if (args[i].equals("-T")) {
                tracing = true;
            }
            if (args[i].startsWith("-bytecode:")) {
                switch (argValue) {
                    case "on":
                        generateByteCode = 0;
                        break;
                    case "debug":
                        debugByteCode = true;
                        generateByteCode = 0;
                        break;
                    case "off":
                        generateByteCode = -1;

                        break;
                    default:
                        generateByteCode = Integer.parseInt(argValue);
                        break;
                }
            }
            if (args[i].startsWith("-streaming:")) {
                switch (argValue) {
                    case "off":
                        streaming = 0;
                        break;
                    case "on":
                        streaming = 1;
                        break;
                    case "strict":
                        streaming = 3;
                        break;
                    default:
                        throw new Exception("-streaming must be off|on|strict");
                }
            }
            if (args[i].startsWith("-tree")) {
                treeModel = getTreeModel(argValue);
                if (treeModel == null) {
                    throw new Exception("The requested TreeModel '" + argValue + "' does not exist");
                }

            }
            if (args[i].startsWith("-lang:")) {
                lang = argValue;
                processSpec(argValue);
            }
            // Temporary for controlling test processor
            if (args[i].startsWith("-xt30:")) {
                useXslt30Transformer = argValue.equals("on");
            }
            // Determine whether Posture/Sweep tests should be run
            if (args[i].startsWith("-ps:")) {
                runPostureAndSweepTests = argValue.equals("on");
            }
            // Determine whether static base uri should be exported
            if (args[i].startsWith("-relocatable:")) {
                relocatable = argValue.equals("on");
            }
            // Set optimization flags
            if (args[i].startsWith("-opt:")) {
                optimizerFlags = argValue;
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
        System.err.println("Relocatable: " + relocatable);

        driverSerializer.setOutputStream(System.err);
        driverSerializer.setOutputProperty(Serializer.Property.METHOD, "adaptive");
        driverSerializer.setOutputProperty(Serializer.Property.OMIT_XML_DECLARATION, "yes");
        driverSerializer.setOutputProperty(Serializer.Property.INDENT, "yes");
        driverProc.setConfigurationProperty(Feature.STABLE_UNPARSED_TEXT, true);
        processCatalog(new File(catalog));
        printResults(resultsDir + "/results" + Version.getProductVersion() + ".xml");
    }

    public boolean isDebugByteCode(){
        return debugByteCode;
    }

    /**
     * Return the appropriate tree model to use
     *
     * @param s The name of the tree model required
     * @return The tree model - null if model requested is unrecognised
     */
    protected TreeModel getTreeModel(String s) {
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

    public XdmNode makeDominoTree(Document doc, Configuration config, String baseUri) throws SaxonApiException {
        throw new UnsupportedOperationException("Domino needs PE+");
    }

    public String getResultsDir() {
        return resultsDir;
    }

    public abstract void processSpec(String specStr);

    public boolean isByteCode() {
        return generateByteCode == 0;
    }

    public String getProductEdition() {
        return "Saxon-" + driverProc.getSaxonEdition();
    }

    public String getProductVersion() {
        return Version.getProductVersion();
    }

    public void prepareForSQL(Processor processor) {}


    protected void processCatalog(File catalogFile) throws SaxonApiException {
        if (driverProc.getSaxonEdition().equals("EE")) {
            if (generateByteCode == 0 && !debugByteCode) {
                driverProc.setConfigurationProperty(Feature.GENERATE_BYTE_CODE, true);
                driverProc.setConfigurationProperty(Feature.DEBUG_BYTE_CODE, false);
            } else if (generateByteCode >0) {
                driverProc.setConfigurationProperty(FeatureKeys.GENERATE_BYTE_CODE, ""+generateByteCode);
                driverProc.setConfigurationProperty(Feature.DEBUG_BYTE_CODE, false);
            } else if (generateByteCode == 0 && debugByteCode) {
                driverProc.setConfigurationProperty(Feature.GENERATE_BYTE_CODE, true);
                driverProc.setConfigurationProperty(Feature.DEBUG_BYTE_CODE, true);
            } else {
                driverProc.setConfigurationProperty(Feature.GENERATE_BYTE_CODE, false);
                driverProc.setConfigurationProperty(Feature.DEBUG_BYTE_CODE, false);
            }
        }
        DocumentBuilder catbuilder = driverProc.newDocumentBuilder();
        catbuilder.setTreeModel(treeModel);
        catbuilder.setLineNumbering(true);
        XdmNode catalog = catbuilder.build(catalogFile);
        catalogNode = catalog;
        XPathCompiler xpc = driverProc.newXPathCompiler();
        xpc.setLanguageVersion("3.1");
        xpc.setCaching(true);
        xpc.declareNamespace("", catalogNamespace());

        if (requestedTestSet == null) {
            createGlobalEnvironments(catalog, xpc);
        }

        try {
            writeResultFilePreamble(driverProc, catalog);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        readExceptionsFile();


        if (requestedTestSet != null) {
            try {
                Predicate<XdmNode> isRequestedTestSet = node -> requestedTestSet.equals(node.attribute("name"));
                Optional<XdmNode> funcSetNode = catalog.select(
                        descendant("test-set").where(isRequestedTestSet)).asOptionalNode();
                if (!funcSetNode.isPresent()) {
                    throw new Exception("Test-set " + requestedTestSet + " not found!");
                }
                processTestSet(catbuilder, xpc, funcSetNode.get());
            } catch (Exception e1) {
                e1.printStackTrace();
            }
        } else {
            for (XdmNode testSet : catalog.select(descendant("test-set")).asList()) {
                processTestSet(catbuilder, xpc, testSet);
            }
        }
        try {
            writeResultFilePostamble();
        } catch (Exception e) {
            e.printStackTrace();
        }


    }

    protected String getNameOfExceptionsFile() {
        return exceptionsFileName;
    }

    /**
     * Look for an exceptions.xml document with the general format:
     * {@code
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
     * }
     */

    protected void readExceptionsFile() {

        XdmNode exceptionsDoc = null;
        DocumentBuilder exceptBuilder = driverProc.newDocumentBuilder();
        QName testSet = new QName("", "test-set");
        QName testCase = new QName("", "test-case");
        QName run = new QName("", "run");
        QName edition = new QName("", "edition");
        String saxonEdition = driverProc.getSaxonEdition();
        try {
            String suppliedName = getNameOfExceptionsFile();
            File exceptionsFile;
            if (suppliedName.startsWith("/") || suppliedName.matches("^[a-zA-Z]:.*")) {
                exceptionsFile = new File(suppliedName);
            } else {
                exceptionsFile = new File(resultsDir + "/" + suppliedName);
            }
            System.err.println("Loading exceptions file " + exceptionsFile.getAbsolutePath());
            exceptionsDoc = exceptBuilder.build(exceptionsFile);
            XdmSequenceIterator iter = exceptionsDoc.axisIterator(Axis.DESCENDANT, new QName("", "exception"));
            while (iter.hasNext()) {
                XdmNode entry = (XdmNode) iter.next();
                String testName = entry.getAttributeValue(testCase);
                if (testName == null) {
                    testName = "$" + entry.getAttributeValue(testSet);
                }
                String runVal = entry.getAttributeValue(run);
                String editionVal = entry.getAttributeValue(edition);
                if (runVal == null) {
                    runVal = "false";
                }
                if (editionVal == null) {
                    editionVal = saxonEdition;
                }
                boolean appliesThisEdition = false;
                for (String ed : editionVal.trim().split("\\s+")) {
                    if (ed.equals(saxonEdition)) {
                        appliesThisEdition = true;
                        break;
                    }
                }
                if (appliesThisEdition) {
                    if (runVal.equals("false")) {
                        for (String tc : testName.trim().split("\\s+")) {
                            exceptionsMap.put(tc, entry);
                        }
                    } else {
                        XdmSequenceIterator iter2 = entry.axisIterator(Axis.CHILD, new QName("optimization"));
                        if (iter2.hasNext()) {
                            XdmNode optim = (XdmNode) iter2.next();
                            optimizationAssertions.put(testName, optim.attribute("assert"));
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
        String testCaseName = testCase.attribute("name");
        Optional<XdmNode> environmentNode = testCase.select(child("environment")).asOptionalNode();
        Environment env;
        if (!environmentNode.isPresent()) {
            env = localEnvironments.get("default");
        } else {
            String envName = environmentNode.get().attribute("ref");
            if (envName == null || envName.equals("")) {
                boolean baseUriCheck = false;
                env = null;
                try {
                    env = Environment.processEnvironment(
                            this, xpc, environmentNode.get(), null, localEnvironments.get("default"));
                    baseUriCheck =  environmentNode.get().select(
                            child("static-base-uri").where(attributeEq("uri", "#UNDEFINED"))).exists();
                } catch (NullPointerException ex) {
                    ex.printStackTrace();
                    System.err.println("Failure loading environment");
                    if (env != null) {
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
                    try {
                        for (XdmNode e : testCase.getParent().select(descendant("environment").where(attributeEq("name", envName))).asList()) {
                            Environment.processEnvironment(this, xpc, e, localEnvironments, localEnvironments.get("default"));
                        }
                        env = localEnvironments.get(envName);
                        if (unsharedEnvironments.contains(envName)){
                            localEnvironments.remove(envName);
                        }
                    } catch (NullPointerException ex) {
                        ex.printStackTrace();
                        System.err.println("Failure loading environment");
                        if (env != null) {
                            env.usable = false;
                        }
                    }
                }
                if (env == null) {
                    // when running a single test set, we no longer pre-build the global environments. So we now
                    // need to check if the reference is to a global environment
                    createGlobalEnvironments(catalogNode, xpc);
                    env = globalEnvironments.get(envName);
                }
                if (env == null) {
                    println("*** Unknown environment " + envName);
                    //writeTestcaseElement(testCaseName, "fail", "Environment " + envName + " not found", null);
                    noteFailure(testCase.getParent().attribute("name"), testCaseName);
                    return null;
                }

            }
        }
        return env;
    }

    /**
     * Register a query module (for the XSLT tests using load-query-module
     */

    public void registerXQueryModule(String uri, File resource) {
        queryModules.put(uri, resource);
    }

    /**
     * Export a stylesheet (used in the XSLT JS driver
     */

    public File exportStylesheet(XsltCompiler compiler, String fileName) throws SaxonApiException {
        return null;
    }

    /**
     * Inject code into a compiled query
     *
     * @param compiler the query compiler
     */

    public void addInjection(XQueryCompiler compiler) {
        // added in subclasses
    }

    public void noteFailure(String testSet, String testCase) {
        failures++;
        if (requestedTestSet != null) {
            failSummary.put(testCase, 1);
        } else {
            failSummary.merge(testSet, 1, (a, b) -> a + b);
        }
    }

    protected void writeResultFilePreamble(Processor processor, XdmNode catalog)
            throws IOException, SaxonApiException, XMLStreamException {
        resultsDoc.writeResultFilePreamble(processor, catalog);
    }

    protected void writeResultFilePostamble()
            throws XMLStreamException {
        resultsDoc.writeResultFilePostamble();
        if (requestedTestSet != null) {
            System.err.println("Failing tests: ");
            for (Map.Entry<String, Integer> entry : failSummary.entrySet()) {
                System.err.println("  " + entry.getKey());
            }
        } else {
            System.err.println("Failures by Test Set: ");
            for (Map.Entry<String, Integer> entry : failSummary.entrySet()) {
                System.err.println("  " + entry.getKey() + " : " + entry.getValue());
            }
        }
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
        File testSetFile = new File(testSuiteDir + "/" + testSetNode.attribute("file"));
        XdmNode testSetDocNode = catbuilder.build(testSetFile);
        createLocalEnvironments(testSetDocNode);
        boolean run = true;
        // pick up any test-set level dependencies in the QT3 catalog
        if (testSetDocNode.select(child("test-set").then(child("dependency"))).exists()) {
            for (XdmNode dependency : testSetDocNode.select(child("test-set").then(child("dependency"))).asList()) {
                if (!ensureDependencySatisfied(dependency, localEnvironments.get("default"))) {
                    for (XdmNode testCase : testSetDocNode.select(descendant("test-case")).asList()) {
                        String testCaseName = testCase.attribute("name");
                        resultsDoc.writeTestcaseElement(testCaseName, "n/a", "test-set dependencies not satisfied");
                        notrun++;
                    }
                    run = false;
                    break;
                }
            }
        }
        // pick up any test-set level dependencies in the XSLT 3.0 catalog
        if (testSetDocNode.select(child("test-set").then(child("dependencies"))).exists()) {
            for (XdmNode dependency : testSetDocNode.select(path("test-set", "dependencies", "*")).asListOfNodes()) {
                if (!ensureDependencySatisfied(dependency, localEnvironments.get("default"))) {
                    for (XdmNode testCase : testSetDocNode.select(descendant("test-case")).asList()) {
                        String type = dependency.getNodeName().getLocalName();
                        String value = dependency.attribute("value");
                        if (value == null) {
                            value = type;
                        } else {
                            value = type + ":" + value;
                        }
                        if ("false".equals(dependency.attribute("satisfied"))) {
                            value = "!" + value;
                        }
                        String testCaseName = testCase.attribute("name");
                        resultsDoc.writeTestcaseElement(testCaseName, "n/a", "test-set dependencies not satisfied: " + value);
                        notrun++;
                    }
                    run = false;
                    break;
                }
            }
        }
        if (run) {
            if (testPattern == null) {
                for (XdmNode env : testSetDocNode.select(descendant("environment").where(hasAttribute("name"))).asList()) {
                    String envName = env.attribute("name");
                    if (!unsharedEnvironments.contains(envName)) {
                        try {
                            Environment.processEnvironment(this, xpc, env, localEnvironments, localEnvironments.get("default"));
                        } catch (NullPointerException ex) {
                            ex.printStackTrace();
                            System.err.println("Failure loading environment, in processTestSet");
                        }
                    }
                }
            }
            testSet = testSetDocNode.select(Steps.path("/", "test-set", "@name")).collect(asString());
            testSetDocNode.select(descendant("test-case")).forEach(testCase -> {
                String name = testCase.attribute("name");
                if (testPattern == null || testPattern.matcher(name).matches()) {
                    println("-s:" + testSet + " -t:" + name);

                    try {
                        runTestCase(testCase, xpc);
                    } catch (SaxonApiException ex) {
                        ex.printStackTrace();
                        System.err.println("*** Error in evaluating testcase:" + ex.getMessage());
                    }
                }
            });
        }
        writeTestSetEndElement();
    }

    protected abstract void runTestCase(XdmNode testCase, XPathCompiler catalogXpc)
            throws SaxonApiException;

    public void setTestDriverShell(TestDriverShell gui) {
        shell = gui;
    }

    public void println(String data) {
        if (!quiet) {
            shell.println(data);
        }
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

    /**
     * Ensure that a dependency is satisfied, first by checking whether Saxon supports
     * the requested feature, and if necessary by reconfiguring Saxon so that it does;
     * if configuration changes are made, then resetActions should be registered to
     * reverse the changes when the test is complete.
     * @param dependency the dependency to be checked
     * @param env the environment in which the test runs. The method may modify this
     *            environment provided the changes are reversed for subsequent tests.
     * @return true if the test can proceed, false if the dependencies cannot be
     * satisfied.
     */

    public abstract boolean ensureDependencySatisfied(XdmNode dependency, Environment env);


    protected void copySchemaNamespaces(Environment env, XPathCompiler testXpc) {
        Configuration config = env.xpathCompiler.getProcessor().getUnderlyingConfiguration();
        for (String s : config.getImportedNamespaces()) {
            testXpc.importSchemaNamespace(s);
        }
    }

}

