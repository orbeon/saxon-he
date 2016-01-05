////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
// Copyright (c) 2014 Saxonica Limited.
// This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
// If a copy of the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
// This Source Code Form is "Incompatible With Secondary Licenses", as defined by the Mozilla Public License, v. 2.0.
////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

package net.sf.saxon.testdriver;

import net.sf.saxon.Configuration;
import net.sf.saxon.lib.EnvironmentVariableResolver;
import net.sf.saxon.lib.FeatureKeys;
import net.sf.saxon.lib.NamespaceConstant;
import net.sf.saxon.lib.StandardModuleURIResolver;
import net.sf.saxon.om.Sequence;
import net.sf.saxon.om.TreeModel;
import net.sf.saxon.s9api.*;
import net.sf.saxon.trace.ExpressionPresenter;
import net.sf.saxon.trans.LicenseException;
import net.sf.saxon.trans.XPathException;

import javax.xml.stream.XMLStreamException;
import javax.xml.transform.Source;
import javax.xml.transform.TransformerException;
import javax.xml.transform.URIResolver;
import javax.xml.transform.stream.StreamSource;
import java.io.File;
import java.io.StringWriter;
import java.net.URI;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Test Driver for the QT3 test suite
 */
public class QT3TestDriverHE extends TestDriver {


    public static String RNS = "http://www.w3.org/2012/08/qt-fots-results";
    public static String CNS = "http://www.w3.org/2010/09/qt-fots-catalog";

    public Map<String, Dependency> getDependencyMap() {
        return dependencyMap;
    }


    //private FotsResultsDocument resultsDoc;

    public String catalogNamespace() {
        return CNS;
    }

    @Override
    public void go(String[] args) throws Exception {
        super.go(args);
    }

    public void processSpec(String specStr) {

        if (specStr.equals("XP20")) {
            spec = Spec.XP20;
        } else if (specStr.equals("XP30")) {
            spec = Spec.XP30;
        } else if (specStr.equals("XP30")) {
            spec = Spec.XP31;
        } else if (specStr.equals("XQ10")) {
            spec = Spec.XQ10;
        } else if (specStr.equals("XQ30")) {
            spec = Spec.XQ30;
        } else if (specStr.equals("XQ31")) {
            spec = Spec.XQ31;
        } else if (specStr.equals("XT30")) { // map function tests
            spec = Spec.XT30;
        } else {
            throw new IllegalArgumentException("The specific language must be one of the following: XP20, XP30, XQ10, XQ30, XT30");
        }
        resultsDoc = new QT3TestReport(this, spec);
    }

    public static void main(String[] args) throws Exception {
        if (args.length == 0 || args[0].equals("-?")) {
            usage();
            return;
        }
        new QT3TestDriverHE().go(args);
    }

    public static void usage() {
        System.err.println("java net.sf.saxon.testdriver.QT3TestSuiteDriverHE testsuiteDir catalog [-o:resultsdir] [-s:testSetName]" +
                " [-t:testNamePattern] [-unfolded] [-bytecode:on|off|debug] [-tree] [-lang:XP20|XP30|XQ10|XQ30]");

    }


    @Override
    protected void createGlobalEnvironments(XdmNode catalog, XPathCompiler xpc) throws SaxonApiException {
        Environment environment = null;
        for (XdmItem env : xpc.evaluate("//environment", catalog)) {
            try {
                environment = Environment.processEnvironment(
                        this, xpc, env, globalEnvironments, localEnvironments.get("default"));
            } catch (NullPointerException ex) {
                System.err.println("Failed to load environment");
            }
        }
        try {
            buildDependencyMap(driverProc, environment);
        } catch (Exception ex) {
            System.err.println("Environment map error" + ex.getMessage());
        }
    }

    /**
     * Decide whether a dependency is satisfied
     *
     * @param dependency the dependency element in the catalog
     * @param env        an environment in the catalog, which can be modified to satisfy the dependency if necessary.
     *                   May be null.
     * @return true if the environment satisfies the dependency, else false
     */

    public boolean dependencyIsSatisfied(XdmNode dependency, Environment env) {
        String type = dependency.getAttributeValue(new QName("type"));
        String value = dependency.getAttributeValue(new QName("value"));
        boolean inverse = "false".equals(dependency.getAttributeValue(new QName("satisfied")));
        if ("xml-version".equals(type)) {
            if (value.equals("1.0:4-") && !inverse) {
                // we don't support XML 1.0 4th edition or earlier
                return false;
            }
            if (value.contains("1.1") && !inverse) {
                if (treeModel.getName().equals("JDOM") || treeModel.getName().equals("JDOM2") || treeModel.getName().equals("XOM")) {
                    return false;
                }
                if (env != null) {
                    env.processor.setXmlVersion("1.1");
                } else {
                    return false;
                }
            } else if (value.contains("1.0") && !inverse) {
                if (env != null) {
                    env.processor.setXmlVersion("1.0");
                } else {
                    return false;
                }
            }
            return true;
        } else if ("xsd-version".equals(type)) {
            final String old = (String) env.processor.getConfigurationProperty(FeatureKeys.XSD_VERSION);
            if ("1.1".equals(value)) {
                if (env != null) {
                    env.processor.setConfigurationProperty(FeatureKeys.XSD_VERSION, inverse ? "1.0" : "1.1");
                } else {
                    return false;
                }
            } else if ("1.0".equals(value)) {
                if (env != null) {
                    env.processor.setConfigurationProperty(FeatureKeys.XSD_VERSION, inverse ? "1.1" : "1.0");
                } else {
                    return false;
                }
            }
            env.resetActions.add(new Environment.ResetAction() {
                public void reset(Environment env) {
                    env.processor.setConfigurationProperty(FeatureKeys.XSD_VERSION, old);
                }
            });
            return true;
        } else if ("limits".equals(type)) {
            return ("year_lt_0".equals(value) || "big_integer".equals(value)) && !inverse;    //jwL - add big-integer
        } else if ("spec".equals(type)) {
            return true;
        } else if ("collection-stability".equals(type)) {
            // SAXON has a problem here - we don't support stable collections
            return "false".equals(value) != inverse;
        } else if ("default-language".equals(type)) {
            return "en".equals(value) != inverse;
        } else if ("directory-as-collection-uri".equals(type)) {
            return "true".equals(value) != inverse;
        } else if ("language".equals(type)) {
            return ("en".equals(value) || "de".equals(value) || "fr".equals(value) || "it".equals(value) || "xib".equals(value)) != inverse;
        } else if ("calendar".equals(type)) {
            return ("AD".equals(value) || "ISO".equals(value)) != inverse;
        } else if ("format-integer-sequence".equals(type)) {
            return !inverse;
        } else if ("unicode-normalization-form".equals(type)) {
            return value.equalsIgnoreCase("FULLY-NORMALIZED") ? inverse : !inverse;
        } else if ("unicode-version".equals(type)) {
            return true || value.equals("6.0");
        } else if ("feature".equals(type)) {
            if ("namespace-axis".equals(value)) {
                return !inverse;
            } else if ("higherOrderFunctions".equals(value)) {
                String edition = env.processor.getSaxonEdition();
                return (edition.equals("PE") || edition.equals("EE")) ^ inverse;
            } else if ("schemaImport".equals(value) || "schemaValidation".equals(value) || "schemaAware".equals(value)) {
                return makeSchemaAware(env, inverse);
            } else if ("xpath-1.0-compatibility".equals(value)) {
                if (env != null) {
                    env.xpathCompiler.setBackwardsCompatible(!inverse);
                    return true;
                } else {
                    return false;
                }
            } else if ("staticTyping".equals(value)) {
                return inverse;
            } else if ("moduleImport".equals(value)) {
                return !inverse;
            } else if ("schema-location-hint".equals(value)) {
                String edition = env.processor.getUnderlyingConfiguration().getEditionCode();
                return (edition.equals("PE") || edition.equals("EE")) ^ inverse;
            } else if ("infoset-dtd".equals(value)) {
                if (treeModel == TreeModel.TINY_TREE || treeModel == TreeModel.LINKED_TREE || treeModel == TreeModel.TINY_TREE_CONDENSED) {
                    return !inverse;
                } else {
                    return inverse;
                }
            } else if ("serialization".equals(value)) {
                return true;
            } else if ("non_unicode_codepoint_collation".equals(value)) {
                return !inverse;
            } else if ("non_empty_sequence_collection".equals(value)) {
                return !inverse;
            } else {
                println("**** feature = " + value + "  ????");
                return false;
            }
        } else {
            println("**** dependency not recognized: " + type);
            return false;
        }
    }

    protected boolean makeSchemaAware(Environment env, boolean inverse) {
        // Saxon-HE cannot run schema-aware tests
        return !inverse;
    }

    /**
     * Run a test case
     *
     * @param testCase the test case element in the catalog
     * @param xpc      the XPath compiler to be used for compiling XPath expressions against the catalog
     * @throws SaxonApiException
     */

    @SuppressWarnings({"ThrowableResultOfMethodCallIgnored"})
    protected void runTestCase(XdmNode testCase, XPathCompiler xpc) throws SaxonApiException {
        boolean run = true;
        String hostLang;
        String langVersion;
        Spec specOpt = null;
        XPathCompiler xpath = driverProc.newXPathCompiler();
        String testCaseName = testCase.getAttributeValue(new QName("name"));
        String testSetName = testCase.getParent().getAttributeValue(new QName("name"));
        boolean needSerializedResult = ((XdmAtomicValue) xpc.evaluateSingle(
                "exists(./result//assert-serialization-error) or exists(./result//serialization-matches)", testCase)).getBooleanValue();
        boolean needResultValue = true;
        if (needSerializedResult) {
            needResultValue = ((XdmAtomicValue) xpc.evaluateSingle(
                    "exists(./result//*[not(self::serialization-matches or self::assert-serialization-error or self::any-of or self::all-of)])", testCase)).getBooleanValue();
        }

        XdmNode alternativeResult = null;
        XdmNode optimization = null;


        hostLang = spec.shortSpecName;
        langVersion = spec.version;


        Environment env = getEnvironment(testCase, xpc);
        if (env == null) {
            notrun++;
            return;
        }
        if (!env.usable) {
            writeTestcaseElement(testCaseName, "n/a", "environment dependencies not satisfied");
            notrun++;
            return;
        }
        env.xpathCompiler.setBackwardsCompatible(false);
        env.processor.setXmlVersion("1.0");

        for (XdmItem dependency : xpc.evaluate("/*/dependency, ./dependency", testCase)) {
            String type = ((XdmNode) dependency).getAttributeValue(new QName("type"));
            if (type == null) {
                throw new IllegalStateException("dependency/@type is missing");
            }
            String value = ((XdmNode) dependency).getAttributeValue(new QName("value"));
            if (value == null) {
                throw new IllegalStateException("dependency/@value is missing");
            }

            if (type.equals("spec")) {
                boolean applicable = false;
                if (!value.contains(spec.shortSpecName)) {
                    applicable = false;
                } else if (value.contains(spec.specAndVersion)) {
                    applicable = true;
                } else if ((spec.specAndVersion.equals("XQ30") || spec.specAndVersion.equals("XQ31")) &&
                        (value.contains("XQ10+") || value.contains("XQ30+"))) {
                    applicable = true;
                } else if ((spec.specAndVersion.equals("XP30") || spec.specAndVersion.equals("XP31")) &&
                        (value.contains("XP20+") || value.contains("XP30+"))) {
                    applicable = true;
                }
                if (!applicable) {
                    writeTestcaseElement(testCaseName, "n/a", "not" + spec.specAndVersion);
                    notrun++;
                    return;
                }
            }
            if (langVersion.equals("3.0") || langVersion.equals("3.1")) {
                EnvironmentVariableResolver resolver = new EnvironmentVariableResolver() {
                    public Set<String> getAvailableEnvironmentVariables() {
                        Set<String> strings = new HashSet<String>();
                        strings.add("QTTEST");
                        strings.add("QTTEST2");
                        strings.add("QTTESTEMPTY");
                        return strings;
                    }

                    public String getEnvironmentVariable(String name) {
                        if (name.equals("QTTEST")) {
                            return "42";
                        } else if (name.equals("QTTEST2")) {
                            return "other";
                        } else if (name.equals("QTTESTEMPTY")) {
                            return "";
                        } else {
                            return null;
                        }
                    }
                };
                env.processor.setConfigurationProperty(FeatureKeys.ENVIRONMENT_VARIABLE_RESOLVER, resolver);
            }
            /*if (type.equals("feature") && value.equals("xpath-1.0-compatibility")) {
                hostLang = "XP";
                langVersion = "3.0";
                xpDependency = true;
                specOpt = Spec.XP30;
            }
            if (type.equals("feature") && value.equals("namespace-axis")) {
                hostLang = "XP";
                langVersion = "3.0";
                xpDependency = true;
                specOpt = Spec.XP30;
            } */
            if (!dependencyIsSatisfied((XdmNode) dependency, env)) {
                println("*** Dependency not satisfied: " + ((XdmNode) dependency).getAttributeValue(new QName("type")));
                writeTestcaseElement(testCaseName, "n/a", "Dependency not satisfied");
                //run = false;
                notrun++;
                return;
            }
        }

        XdmNode exceptionElement = exceptionsMap.get(testCaseName);
        if (exceptionElement == null) {
            exceptionElement = exceptionsMap.get(testSetName + "*");
        }
        if (exceptionElement != null) {
            XdmItem config = xpath.evaluateSingle("configuration", exceptionElement);

            String runAtt = exceptionElement.getAttributeValue(new QName("run"));
            String reasonMsg = exceptionElement.getAttributeValue(new QName("reason"));
            if (reasonMsg == null) {
                reasonMsg = "no reason given";
            }
            String reportAtt = exceptionElement.getAttributeValue(new QName("report"));
            if (reportAtt == null) {
                reportAtt = "";
            }

            if (config != null) {
                XdmItem paramValue = xpath.evaluateSingle("param[@name='not-unfolded' and @value='yes']/@name", config);
                if (unfolded && paramValue != null) {
                    writeTestcaseElement(testCaseName, "notRun", reasonMsg);
                    notrun++;
                    return;
                }
            }

            if ("false".equals(runAtt)) {
                writeTestcaseElement(testCaseName, reportAtt, reasonMsg);
                notrun++;
                return;
            }

            alternativeResult = (XdmNode) xpc.evaluateSingle("result", exceptionElement);
            optimization = (XdmNode) xpc.evaluateSingle("optimization", exceptionElement);
        }

        if (run && (specOpt == null || specOpt == spec)) {

            TestOutcome outcome = new TestOutcome(this);
            String exp = null;
            try {
                exp = xpc.evaluate("if (test/@file) then unparsed-text(resolve-uri(test/@file, base-uri(.))) else string(test)", testCase).toString();
            } catch (SaxonApiException err) {
                println("*** Failed to read query: " + err.getMessage());
                outcome.setException(err);
            }

            //noinspection ThrowableResultOfMethodCallIgnored
            if (outcome.getException() == null) {
                if (hostLang.equals("XP") || hostLang.equals("XT")) {
                    XPathCompiler testXpc = env.xpathCompiler;
                    testXpc.setLanguageVersion(langVersion);
                    testXpc.declareNamespace("fn", NamespaceConstant.FN);
                    testXpc.declareNamespace("xs", NamespaceConstant.SCHEMA);
                    //testXpc.declareNamespace("math", NamespaceConstant.MATH);
                    testXpc.declareNamespace("map", NamespaceConstant.MAP_FUNCTIONS);

                    copySchemaNamespaces(env, testXpc);  //jwL

                    try {
                        XPathSelector selector = testXpc.compile(exp).load();
                        for (QName varName : env.params.keySet()) {
                            selector.setVariable(varName, env.params.get(varName));
                        }
                        if (env.contextItem != null) {
                            selector.setContextItem(env.contextItem);
                        }
                        selector.setURIResolver(new TestURIResolver(env));
                        selector.setURIResolver(new TestURIResolver(env));
                        if (env.unparsedTextResolver != null) {
                            selector.getUnderlyingXPathContext().setUnparsedTextURIResolver(env.unparsedTextResolver);
                        }
                        XdmValue result = selector.evaluate();
                        outcome.setPrincipalResult(result);
                    } catch (SaxonApiException err) {
                        println(err.getMessage());
                        outcome.setException(err);
                    } catch (Exception err) {
                        println(err.getMessage());
                        err.printStackTrace();
                        writeTestcaseElement(testCaseName, "fail", "*** crashed: + err.getMessage()");
                        //notrun++;
                        //return;
                    }
                } else if (hostLang.equals("XQ")) {
                    XQueryCompiler testXqc = env.xqueryCompiler;
                    testXqc.setLanguageVersion(langVersion);
                    testXqc.declareNamespace("fn", NamespaceConstant.FN);
                    testXqc.declareNamespace("xs", NamespaceConstant.SCHEMA);
                    //testXqc.declareNamespace("math", NamespaceConstant.MATH);
                    testXqc.declareNamespace("map", NamespaceConstant.MAP_FUNCTIONS);
                    ErrorCollector errorCollector = new ErrorCollector();
                    testXqc.setErrorListener(errorCollector);
                    String decVars = env.paramDecimalDeclarations.toString();
                    if (decVars.length() != 0) {
                        int x = exp.indexOf("(:%DECL%:)");
                        if (x < 0) {
                            exp = decVars + exp;
                        } else {
                            exp = exp.substring(0, x) + decVars + exp.substring(x + 13);
                        }
                    }
                    String vars = env.paramDeclarations.toString();
                    if (vars.length() != 0) {
                        int x = exp.indexOf("(:%VARDECL%:)");
                        if (x < 0) {
                            exp = vars + exp;
                        } else {
                            exp = exp.substring(0, x) + vars + exp.substring(x + 13);
                        }
                    }
                    ModuleResolver mr = new ModuleResolver(xpc);
                    mr.setTestCase(testCase);
                    testXqc.setModuleURIResolver(mr);

                    try {
                        XQueryExecutable q = testXqc.compile(exp);
                        if (optimization != null) {
                            // Test whether required optimizations have been performed
                            XdmDestination expDest = new XdmDestination();
                            Configuration config = driverProc.getUnderlyingConfiguration();
                            ExpressionPresenter presenter = new ExpressionPresenter(config, expDest.getReceiver(config));
                            q.getUnderlyingCompiledQuery().explain(presenter);
                            presenter.close();
                            XdmNode explanation = expDest.getXdmNode();
                            XdmItem optResult = xpc.evaluateSingle(optimization.getAttributeValue(new QName("assert")), explanation);
                            if (((XdmAtomicValue) optResult).getBooleanValue()) {
                                println("Optimization result OK");
                            } else {
                                println("Failed optimization test");
                                driverProc.writeXdmValue(explanation, driverProc.newSerializer(System.err));
                                writeTestcaseElement(testCaseName, "fail", "Failed optimization assertions");
                                failures++;
                                return;
                            }

                        }
                        XQueryEvaluator selector = q.load();
                        for (QName varName : env.params.keySet()) {
                            selector.setExternalVariable(varName, env.params.get(varName));
                        }
                        if (env.contextItem != null) {
                            selector.setContextItem(env.contextItem);
                        }
                        selector.setURIResolver(new TestURIResolver(env));
                        if (env.unparsedTextResolver != null) {
                            selector.getUnderlyingQueryContext().setUnparsedTextURIResolver(env.unparsedTextResolver);
                        }
                        if (needSerializedResult) {
                            StringWriter sw = new StringWriter();
                            Serializer serializer = env.processor.newSerializer(sw);
                            selector.setDestination(serializer);
                            selector.run();
                            outcome.setPrincipalSerializedResult(sw.toString());
                        }
                        if (needResultValue) {
                            XdmValue result = selector.evaluate();
                            outcome.setPrincipalResult(result);
                        }
                    } catch (SaxonApiException err) {
                        println("in TestSet " + testSetName);
                        println(err.getMessage());
                        outcome.setException(err);
                        outcome.setErrorsReported(errorCollector.getErrorCodes());
                    } catch (LicenseException err) {
                        // treat this as "facility not available", XQST0075
                        println("in TestSet " + testSetName);
                        println(err.getMessage());
                        XPathException xe = new XPathException(err.getMessage(), "XPST0075");
                        outcome.setException(new SaxonApiException(xe));
                        try {
                            errorCollector.error(xe);
                        } catch (Exception e3) {
                            // ignore the exception
                        }
                        outcome.setErrorsReported(errorCollector.getErrorCodes());
                    }
                } else {
                    writeTestcaseElement(testCaseName, "notRun", "No processor found");
                    notrun++;
                    return;
                }
            }

            for (Environment.ResetAction action : env.resetActions) {
                action.reset(env);
            }
            env.resetActions.clear();
            XdmNode assertion;
            if (alternativeResult != null) {
                assertion = (XdmNode) xpc.evaluateSingle("*[1]", alternativeResult);
            } else {
                assertion = (XdmNode) xpc.evaluateSingle("result/*[1]", testCase);
            }
            if (assertion == null) {
                println("*** No assertions found for test case " + testCaseName);
                writeTestcaseElement(testCaseName, "disputed", "No assertions in test case");
                failures++;
                return;
            }
            XPathCompiler assertXpc = env.processor.newXPathCompiler();
            assertXpc.setLanguageVersion("3.0");
            assertXpc.declareNamespace("fn", NamespaceConstant.FN);
            assertXpc.declareNamespace("xs", NamespaceConstant.SCHEMA);
            assertXpc.declareNamespace("math", NamespaceConstant.MATH);
            assertXpc.declareNamespace("map", NamespaceConstant.MAP_FUNCTIONS);
            assertXpc.declareVariable(new QName("result"));
            assertXpc.setBaseURI(assertion.getBaseURI());

            copySchemaNamespaces(env, assertXpc);  //jwL

            boolean b = outcome.testAssertion(assertion, outcome.getPrincipalResultDoc(), assertXpc, xpc, debug);
            if (b) {
                //println("OK");
                writeTestcaseElement(testCaseName, "pass", null);
                successes++;

            } else {

                if (outcome.isException()) {
                    XdmItem expectedError = xpc.evaluateSingle("result//error/@code", testCase);

                    if (expectedError == null) {
//                        if (debug) {
//                            outcome.getException().printStackTrace(System.out);
//                        }
                        writeTestcaseElement(testCaseName, "fail", "Expected success, got " + outcome.getException().getErrorCode());
                        println("*** fail, result " + outcome.getException().getErrorCode() +
                                " Expected success.");
                        failures++;
                    } else {
                        writeTestcaseElement(testCaseName, "wrongError",
                                "Expected error:" + expectedError.getStringValue() + ", got " + outcome.getException().getErrorCode());
                        println("*** fail, result " + outcome.getException().getErrorCode() +
                                " Expected error:" + expectedError.getStringValue());
                        wrongErrorResults++;
                        successes++;
                    }

                } else {
                    writeTestcaseElement(testCaseName, "fail", "Wrong results, got " +
                            truncate(outcome.serialize(assertXpc.getProcessor(), outcome.getPrincipalResultDoc())));
                    failures++;
                    if (debug) {
                        try {
                            println("Result:");
                            driverSerializer.serializeXdmValue(outcome.getPrincipalResult());
                            println("<=======");
                        } catch (Exception err) {
                            // ignore exception
                        }
                        //println(outcome.getResult());
                    } else {
                        println("*** fail (use -debug to show actual result)");
                        //failures++;
                    }
                }
            }
        }
    }

    private void copySchemaNamespaces(Environment env, XPathCompiler testXpc) {
        Configuration config = env.xpathCompiler.getProcessor().getUnderlyingConfiguration();
        //if (config instanceof EnterpriseConfiguration) {
        for (String s : config.getImportedNamespaces()) {
            testXpc.importSchemaNamespace(s);
        }
        //}
    }

    private String truncate(String in) {
        if (in.length() > 80) {
            return in.substring(0, 80) + "...";
        } else {
            return in;
        }
    }


    protected void writeResultFilePreamble(Processor processor, XdmNode catalog) throws Exception {
        resultsDoc.writeResultFilePreamble(processor, catalog);
    }

    protected void writeResultFilePostamble() throws XMLStreamException {
        resultsDoc.writeResultFilePostamble();
    }


    protected void startTestSetElement(XdmNode testSetNode) {
        resultsDoc.startTestSetElement(testSetNode);
    }


    protected void writeTestSetEndElement() {
        resultsDoc.endElement();
    }

    private void writeTestcaseElement(String name, String result, String comment) {
        resultsDoc.writeTestcaseElement(name, result, comment);
    }

    public static class ModuleResolver extends StandardModuleURIResolver {

        XPathCompiler catXPC;
        XdmNode testCase;

        public ModuleResolver(XPathCompiler xpc) {
            this.catXPC = xpc;
        }

        public void setTestCase(XdmNode testCase) {
            this.testCase = testCase;
        }

        public StreamSource[] resolve(String moduleURI, String baseURI, String[] locations) throws XPathException {
            try {
                XdmValue files = catXPC.evaluate("./module[@uri='" + moduleURI + "']/@file/string()", testCase);
                if (files.size() == 0) {
                    throw new XPathException("Failed to find module entry for " + moduleURI);
                }
                StreamSource[] ss = new StreamSource[files.size()];
                for (int i = 0; i < files.size(); i++) {
                    URI uri = testCase.getBaseURI().resolve(files.itemAt(i).toString());
                    ss[i] = getQuerySource(uri);
                }
                return ss;
            } catch (SaxonApiException e) {
                throw new XPathException(e);
            }
        }
    }

    public static class TestURIResolver implements URIResolver {
        Environment env;

        public TestURIResolver(Environment env) {
            this.env = env;
        }

        public Source resolve(String href, String base) throws TransformerException {
            XdmNode node = env.sourceDocs.get(href);
            if (node == null) {
                return null;
            } else {
                return node.asSource();
            }
        }
    }


    /**
     * Static method called as an external function call to evaluate a literal when running in "unfolded" mode.
     * The function simply returns the value of its argument - but the optimizer doesn't know that, so it
     * can't pre-evaluate the call at compile time.
     *
     * @param value the value to be returned
     * @return the supplied value, unchanged
     */

    public static Sequence lazyLiteral(Sequence value) {
        return value;
    }

    /**
     * Collect together information about all the dependencies of tests that use a given environment
     *
     * @param processor the Saxon processor
     * @param env       the environment for which dependency information is to be gathered
     * @throws SaxonApiException if a Saxon error occurs
     */

    private void buildDependencyMap(Processor processor, Environment env) throws SaxonApiException {
        XQueryCompiler xqCompiler = processor.newXQueryCompiler();
        xqCompiler.setLanguageVersion("3.0");
        xqCompiler.setBaseURI(new File(System.getProperty("user.dir")).toURI());
        XdmValue result = xqCompiler.compile(
                "        declare namespace fots = \"http://www.w3.org/2010/09/qt-fots-catalog\";\n" +
                        "        let $testsets := doc('" + testSuiteDir + "/catalog.xml')//fots:test-set/@file/doc(resolve-uri(., exactly-one(base-uri(.))))\n" +
                        "        for $dependencyTS in $testsets//fots:dependency\n" +
                        "        let $name := $dependencyTS/@type\n" +
                        "        let $value := $dependencyTS/@value\n" +
                        "        group by $name, $value\n" +
                        "        order by $name, $value\n" +
                        "        return <dependency type='{$name}' value='{$value}' />").load().evaluate();


        for (XdmItem item : result) {
            XdmNode node = (XdmNode) item;
            String type = node.getAttributeValue(new QName("type"));
            String value = node.getAttributeValue(new QName("value"));
            addDependency(type, value, dependencyIsSatisfied(node, env));
        }


    }


    protected class Dependency {
        public String dType;
        public boolean satisfied;
    }

    private Map<String, Dependency> dependencyMap = new HashMap<String, Dependency>();

    public void addDependency(String depStr, String value, boolean satisfied) {
        if (!dependencyMap.containsKey(value)) {
            Dependency dep = new Dependency();
            dep.dType = depStr;
            dep.satisfied = satisfied;
            dependencyMap.put(value, dep);
        }
    }


}

