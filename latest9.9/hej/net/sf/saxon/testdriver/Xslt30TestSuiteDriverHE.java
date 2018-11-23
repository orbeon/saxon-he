////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
// Copyright (c) 2018 Saxonica Limited.
// This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
// If a copy of the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
// This Source Code Form is "Incompatible With Secondary Licenses", as defined by the Mozilla Public License, v. 2.0.
////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

package net.sf.saxon.testdriver;


import net.sf.saxon.Configuration;
import net.sf.saxon.Query;
import net.sf.saxon.functions.ResolveQName;
import net.sf.saxon.lib.*;
import net.sf.saxon.om.StructuredQName;
import net.sf.saxon.s9api.*;
import net.sf.saxon.s9api.streams.Step;
import net.sf.saxon.trace.XSLTTraceListener;
import net.sf.saxon.trans.XPathException;
import net.sf.saxon.trans.XsltController;
import net.sf.saxon.value.AtomicValue;
import net.sf.saxon.value.QNameValue;

import javax.xml.transform.Result;
import javax.xml.transform.Source;
import javax.xml.transform.TransformerException;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;
import java.io.*;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.*;
import java.util.function.Predicate;

import static net.sf.saxon.s9api.streams.Predicates.*;
import static net.sf.saxon.s9api.streams.Steps.*;

/**
 * This class runs the W3C XSLT Test Suite, driven from the test catalog.
 */
public class Xslt30TestSuiteDriverHE extends TestDriver {


    public Xslt30TestSuiteDriverHE() {
        spec = Spec.XT30;
        setDependencyData();
    }


    public static void main(String[] args) throws Exception {
        if (args.length == 0 || args[0].equals("-?")) {
            usage();
        }
        new Xslt30TestSuiteDriverHE().go(args);
    }

    protected static void usage() {
        System.err.println("java com.saxonica.testdriver.Xslt30TestSuiteDriver[HE] testsuiteDir catalog [-o:resultsdir] [-s:testSetName]" +
                " [-t:testNamePattern] [-bytecode:on|off|debug] [-export] [-tree] [-lang] [-save] [-streaming:off|std|ext]" +
                " [-xt30:on] [-T] [-js]");
    }

    @Override
    public String catalogNamespace() {
        return "http://www.w3.org/2012/10/xslt-test-catalog";
    }


    @Override
    public void processSpec(String specStr) {
        switch (specStr) {
            case "XT10":
                spec = Spec.XT10;
                break;
            case "XT20":
                spec = Spec.XT20;
                break;
            case "XT30":
                spec = Spec.XT30;
                break;
            default:
                throw new IllegalArgumentException("Unknown spec " + specStr);
        }
        resultsDoc = new Xslt30TestReport(this, spec);
    }

    @Override
    protected void createGlobalEnvironments(XdmNode catalog, XPathCompiler xpc) throws SaxonApiException {
        for (XdmNode env : catalog.select(descendant("environment")).asListOfNodes()) {
            Environment.processEnvironment(
                    this, xpc, env, globalEnvironments, localEnvironments.get("default"));
        }
    }

    protected boolean isSlow(String testName) {
        //return false;
        return testName.startsWith("regex-classes")
                || testName.equals("normalize-unicode-008")
        || (testName.equals("function-1031") && driverProc.getSaxonEdition().equals("HE"));
              //  || testName.equals("catalog-005");
    }

    protected static Predicate<XdmItem> isTrue(String attName) {
        return element -> {
            if (element instanceof XdmNode) {
                String attVal = ((XdmNode) element).attribute(attName);
                if (attVal != null) {
                    attVal = attVal.trim();
                    return "yes".equals(attVal) || "1".equals(attVal) || "true".equals(attVal);
                }
            }
            return false;
        };
    }

    @Override
    protected void runTestCase(XdmNode testCase, XPathCompiler xpath) throws SaxonApiException {

        final TestOutcome outcome = new TestOutcome(this);
        String testName = testCase.attribute("name");
        String testSetName = testCase.getParent().attribute("name");

        if (exceptionsMap.containsKey("$" + testSetName)) {
            notrun++;
            XdmNode exceptionElement = exceptionsMap.get("$" + testSetName);
            resultsDoc.writeTestcaseElement(testName, exceptionElement.attribute("report"),
                    exceptionElement.axisIterator(Axis.CHILD, new QName("reason")).next().getStringValue());
            return;
        }

        if (exceptionsMap.containsKey(testName)) {
            notrun++;
            XdmNode exceptionElement = exceptionsMap.get(testName);
            resultsDoc.writeTestcaseElement(testName, exceptionElement.attribute("report"),
                    exceptionElement.axisIterator(Axis.CHILD, new QName("reason")).next().getStringValue());
            return;
        }

        if (isSlow(testName)) {
            notrun++;
            resultsDoc.writeTestcaseElement(testName, "tooBig", "requires excessive resources");
            return;
        }

        Step<XdmNode> testSetDependencies = root().then(child("test-set").then(child("dependencies")));
        Step<XdmNode> testCaseDependencies = child("dependencies");
        XdmValue allDependencies = testCase.select(testSetDependencies.cat(testCaseDependencies)).asXdmValue();

        Optional<XdmNode> lastSpecValue = allDependencies.select(path("spec", "@value")).last().asOptionalNode();
        String specAtt = lastSpecValue.isPresent() ? lastSpecValue.get().getStringValue() : "XSLT10+";

        final Environment env = getEnvironment(testCase, xpath);
        if (env == null) {
            resultsDoc.writeTestcaseElement(testName, "notRun", "test catalog error");
            notrun++;
            return;
        }

        for (XdmNode dep : allDependencies.select(child(isElement())).asListOfNodes()) {
            if (!ensureDependencySatisfied(dep, env)) {
                notrun++;
                String type = dep.getNodeName().getLocalName();
                String value = dep.attribute("value");
                if (value == null) {
                    value = type;
                } else {
                    value = type + ":" + value;
                }
                if ("false".equals(dep.attribute("satisfied"))) {
                    value = "!" + value;
                }
                String message = "dependency not satisfied: " + value;
                if (value.startsWith("feature:")) {
                    message = "requires optional " + value;
                }
                resultsDoc.writeTestcaseElement(testName, "n/a", message);
                return;
            }
        }

        if (env.failedToBuild) {
            resultsDoc.writeTestcaseElement(testName, "fail", "unable to build environment");
            noteFailure(testSetName, testName);
            return;
        }

        if (!env.usable) {
            resultsDoc.writeTestcaseElement(testName, "n/a", "environment dependencies not satisfied");
            notrun++;
            return;
        }


        if (testName.contains("environment-variable")) {
            EnvironmentVariableResolver resolver = new EnvironmentVariableResolver() {
                public Set<String> getAvailableEnvironmentVariables() {
                    Set<String> strings = new HashSet<>();
                    strings.add("QTTEST");
                    strings.add("QTTEST2");
                    strings.add("QTTESTEMPTY");
                    return strings;
                }

                public String getEnvironmentVariable(String name) {
                    switch (name) {
                        case "QTTEST":
                            return "42";
                        case "QTTEST2":
                            return "other";
                        case "QTTESTEMPTY":
                            return "";
                        default:
                            return null;
                    }
                }
            };
            env.processor.setConfigurationProperty(Feature.ENVIRONMENT_VARIABLE_RESOLVER, resolver);
        }

        if (testName.contains("load-xquery-module")) {
            env.processor.getUnderlyingConfiguration().setModuleURIResolver(
                    (moduleURI, baseURI, locations) -> {
                File file = queryModules.get(moduleURI);
                if (file == null) {
                    return null;
                }
                try {
                    StreamSource ss = new StreamSource(new FileInputStream(file), baseURI);
                    return new StreamSource[]{ss};
                } catch (FileNotFoundException e) {
                    throw new XPathException(e);
                }
            });
        }
        XdmNode testInput = testCase.select(child("test")).asNode();
        Optional<XdmNode> stylesheet = testInput.select(
                child("stylesheet").where(not(attributeEq("role", "secondary")))).asOptionalNode();
        Optional<XdmNode> postureAndSweep = testInput.select(
                child("posture-and-sweep")).asOptionalNode();
        Optional<XdmNode> principalPackage = testInput.select(
                child("package").where(attributeEq("role", "principal"))).asOptionalNode();
        List<XdmNode> usedPackages = testInput.select(
                child("package").where(attributeEq("role", "secondary"))).asList();

        XsltExecutable sheet = env.xsltExecutable;
        ErrorCollector collector = new ErrorCollector();
        if (quiet) {
            collector.setLogger(new Logger() {
                @Override
                public void println(String message, int severity) {
                    // no action
                }

                @Override
                public StreamResult asStreamResult() {
                    return null;
                }
            });
        }
        String xsltLanguageVersion = specAtt.contains("XSLT30") || specAtt.contains("XSLT20+") ? "3.0" : "2.0";

        if (postureAndSweep.isPresent()) {
            if (runPostureAndSweepTests) {
                runStreamabilityTests(xpath, testCase);
            }
            return;
        }

        boolean strictStreamability = testCase.select(
                child("result").then(descendant("error"))
                        .where(attributeEq("code", "XTSE3430"))).exists();
        if (strictStreamability) {
            env.processor.setConfigurationProperty(Feature.STRICT_STREAMABILITY, true);
            env.resetActions.add(new Environment.ResetAction() {
                @Override
                public void reset(Environment env) {
                    env.processor.setConfigurationProperty(Feature.STRICT_STREAMABILITY, false);
                }
            });
        }

        Predicate<XdmItem> isStaticError = item -> item.getStringValue().startsWith("XTSE");
        boolean staticError = testCase.select(
                path("result", "//", "error", "@code").where(isStaticError)).exists();
        //boolean staticError = xpath.evaluate("result//error[starts-with(@code, 'XTSE')]", testCase).size() > 0;
        if (staticError && jitFlag) {
            env.xsltCompiler.setJustInTimeCompilation(false);
            env.resetActions.add(new Environment.ResetAction() {
                @Override
                public void reset(Environment env) {
                    env.xsltCompiler.setJustInTimeCompilation(true);
                }
            });
        }

        if (stylesheet.isPresent()) {
            String fileName = stylesheet.get().attribute("file");

            Source styleSource = new StreamSource(testCase.getBaseURI().resolve(fileName).toString());

            XsltCompiler compiler = env.xsltCompiler;
            compiler.setErrorListener(collector);
            initPatternOptimization(compiler);
            compiler.clearParameters();
            //compiler.getUnderlyingCompilerInfo().setRuleOptimization(optimizeRules);
            //compiler.getUnderlyingCompilerInfo().setRulePreconditions(preconditionsRules);
            prepareForSQL(compiler.getProcessor());

            List<XdmNode> params = testInput.select(child("param").where(isTrue("static"))).asListOfNodes();
            for (XdmNode param : params) {
                String name = param.attribute("name");
                String select = param.attribute("select");
                XdmValue value;
                try {
                    value = xpath.evaluate(select, null);
                } catch (SaxonApiException e) {
                    System.err.println("*** Error evaluating parameter " + name + ": " + e.getMessage());
                    throw e;
                }
                compiler.setParameter(new QName(name), value);
            }
            for (XdmNode pack : usedPackages) {
                String fileName2 = pack.attribute("file");
                Source styleSource2 = new StreamSource(testCase.getBaseURI().resolve(fileName2).toString());
                XsltPackage xpack = compiler.compilePackage(styleSource2);
                xpack = exportImportPackage(testName, testSetName, outcome, compiler, xpack, collector);
                compiler.importPackage(xpack);
                // Following needed for dynamic loading of packages using fn:transform()
                env.processor.getUnderlyingConfiguration().getDefaultXsltCompilerInfo().getPackageLibrary().addPackage(xpack.getUnderlyingPreparedPackage());
            }
            sheet = exportImport(testName, testSetName, outcome, compiler, sheet, collector, styleSource);


            String optimizationAssertion = optimizationAssertions.get(testName);
            if (optimizationAssertion != null && sheet != null) {
                try {
                    assertOptimization(sheet, optimizationAssertion);
                    System.err.println("Optimization OK: " + optimizationAssertion);
                } catch (SaxonApiException e) {
                    System.err.println("Optimization assertion failed: " + optimizationAssertion);
                }
            }
        } else if (principalPackage.isPresent()) {
            XsltCompiler compiler = env.xsltCompiler;
            //compiler.setXsltLanguageVersion(xsltLanguageVersion);
            compiler.setErrorListener(collector);
            compiler.clearParameters();
            testInput.select(child("param").where(isTrue("static"))).forEach(param -> {
                String name = param.attribute("name");
                String select = param.attribute("select");
                XdmValue value;
                try {
                    value = xpath.evaluate(select, null);
                } catch (SaxonApiException e) {
                    System.err.println("*** Error evaluating parameter " + name + ": " + e.getMessage());
                    throw new SaxonApiUncheckedException(e);
                }
                compiler.setParameter(new QName(name), value);
            });

            for (XdmNode pack : usedPackages) {
                String fileName = pack.attribute("file");
                Source styleSource = new StreamSource(testCase.getBaseURI().resolve(fileName).toString());
                XsltPackage xpack = compiler.compilePackage(styleSource);
                compiler.importPackage(xpack);
            }
            String fileName = principalPackage.get().attribute("file");
            Source styleSource = new StreamSource(testCase.getBaseURI().resolve(fileName).toString());

            try {
                XsltPackage xpack = compiler.compilePackage(styleSource);
                sheet = xpack.link();
            } catch (SaxonApiException err) {
                System.err.println(err.getMessage());
                outcome.setException(err);
                outcome.setErrorsReported(collector.getErrorCodes());
            } catch (Exception err) {
                err.printStackTrace();
                System.err.println(err.getMessage());
                outcome.setException(new SaxonApiException(err));
                outcome.setErrorsReported(collector.getErrorCodes());
            }

            sheet = exportImport(testName, testSetName, outcome, compiler, sheet, collector, styleSource);

        }
        if (runWithNodeJS && !outcome.isException()) {
            /* TODO - add case where expecting a static error but compiled OK */
            URI r = new File(resultsDir).toURI();
            if (env.exportedStylesheet != null) {
                URI s = env.exportedStylesheet.toURI();
                s = r.relativize(s);
                resultsDoc.writeTestcaseElement(testName, "deferred", s.toString());
            } else {
                resultsDoc.writeTestcaseElement(testName, "deferred", "export/" + testSetName + "/" + testName + ".sef.json");
            }
            return;
        }

        XdmValue initialMatchSelection = null;
        Optional<XdmNode> initialMode = testInput.select(child("initial-mode")).asOptionalNode();
        Optional<XdmNode> initialFunction = testInput.select(child("initial-function")).asOptionalNode();
        Optional<XdmNode> initialTemplate = testInput.select(child("initial-template")).asOptionalNode();

        QName initialModeName = null;
        if (initialMode.isPresent()) {
            initialModeName = getQNameAttribute(xpath, initialMode.get(), attribute("name"));
            String select = initialMode.get().attribute("select");
            if (select != null) {
                initialMatchSelection = env.xpathCompiler.evaluate(select, null);
                if (runWithJS) {
                    resultsDoc.writeTestcaseElement(testName, "n/a", "Test driver limitation: cannot set initial match selection");
                    notrun++;
                    return;
                }
            }
        }

        QName initialTemplateName = getQNameAttribute(xpath, testInput, path("initial-template", "@name"));
        QName initialFunctionName = getQNameAttribute(xpath, testInput, path("initial-function", "@name"));

        if (runWithJS && !outcome.isException()) {
            Optional<String> wellFormed = testInput.select(path("output", "@well-formed")).asOptionalString();
            if (wellFormed.equals(Optional.of("no"))) {
                outcome.getPrincipalResultDoc().wellFormed = false;
            }

            clearGlobalParameters();
            Map<QName, XdmValue> caseGlobalParams = getNamedParameters(xpath, testInput, false, false);
            for (Map.Entry<QName, XdmValue> entry : caseGlobalParams.entrySet()) {
                setGlobalParameter(entry.getKey(), entry.getValue());
            }
            if (initialFunctionName != null) {
                clearInitialFunctionArguments();
                XdmNode iniFunc = testInput.select(child("initial-function")).asNode();
                XdmValue[] args = getParameters(xpath, iniFunc);
                for (XdmValue val : args) {
                    addInitialFunctionArgument(val);
                }
            }
            URI baseOut = new File(resultsDir + "/results/output.xml").toURI();
            try {
                runJSTransform(env, outcome, initialTemplateName, initialModeName, initialFunctionName, baseOut);
            } catch (StackOverflowError e) {
                notrun++;
                resultsDoc.writeTestcaseElement(testName, "tooBig", "Stack Overflow");
                return;
            }
            if ("*notJS*".equals(outcome.getComment())) {
                return;
            }
        }

        if (sheet != null && !runWithJS) {
            XdmItem contextItem = env.contextItem;

            Optional<String> outputUri = testInput.select(path("output", "@file")).asOptionalString();

            URI baseOut = new File(resultsDir + "/results/output.xml").toURI();
            String baseOutputURI = new File(resultsDir + "/results/output.xml").toURI().toString();
            if (outputUri.isPresent() && !outputUri.get().equals("")) {
                baseOutputURI = baseOut.resolve(outputUri.get()).toString();
            }
            boolean failure;
            if (useXslt30Transformer) {
                failure = runWithXslt30Transformer(testCase, xpath, outcome, testName,
                                                   testSetName, env, testInput, sheet, collector,
                                                   xsltLanguageVersion, contextItem, initialMode.orElse(null),
                                                   initialFunction.orElse(null), initialTemplate.orElse(null), initialModeName,
                                                   initialMatchSelection, initialTemplateName, baseOutputURI);
            } else {
                failure = runWithXsltTransformer(xpath, outcome, testName, testSetName, env, testInput,
                                                 sheet, collector, contextItem, initialMode.orElse(null), initialModeName,
                                                 initialTemplateName, baseOutputURI);
            }
            if (failure) {
                return;
            }
        }

        for (Environment.ResetAction action : env.resetActions) {
            action.reset(env);
        }
        env.resetActions.clear();

        Optional<String> earlyExit = testCase.select(
                path(child("result"), attribute("early-exit-possible"))).asOptionalString();
        boolean expectEarlyExit = earlyExit.equals(Optional.of("true"));
        if (expectEarlyExit != collector.isMadeEarlyExit()) {
            outcome.setComment(expectEarlyExit ? "Failed to make early exit" : "Unexpected early exit");
        }

        Optional<XdmNode> assertion = testCase.select(
                path(child("result"), child().where(isElement()))).asOptionalNode();
        if (!assertion.isPresent()) {
            noteFailure(testSetName, testName);
            resultsDoc.writeTestcaseElement(testName, "fail", "No test assertions found");
            return;
        }
        XPathCompiler assertionXPath = env.processor.newXPathCompiler();
        assertionXPath.setSchemaAware(env.processor.isSchemaAware());
        assertionXPath.setBaseURI(assertion.get().getBaseURI());
        copySchemaNamespaces(env, assertionXPath);  // ensure environment has schema namespaces
        boolean success = outcome.testAssertion(assertion.get(), outcome.getPrincipalResultDoc(), assertionXPath, xpath, debug);
        if (success) {
            successes++;
            resultsDoc.writeTestcaseElement(testName, "pass", outcome.getComment());
        } else {
            if (outcome.getWrongErrorMessage() != null) {
                outcome.setComment(outcome.getWrongErrorMessage());
                wrongErrorResults++;
                successes++;
                resultsDoc.writeTestcaseElement(testName, "wrongError", outcome.getComment());
            } else {
                noteFailure(testSetName, testName);
                // MHK: Nov 2015 - temporary diagnostics
                if (outcome.getException() != null && outcome.getComment() == null) {
                    outcome.setComment(outcome.getException().getMessage());
                }
                resultsDoc.writeTestcaseElement(testName, "fail", outcome.getComment());
            }
        }
    }

    public void runJSTransform(Environment env, TestOutcome outcome, QName initialTemplateName,
                               QName initialModeName, QName initialFunctionName,
                               URI baseOutputURI) {
        // exists to be overridden
    }

    private boolean runWithXsltTransformer(XPathCompiler xpath, final TestOutcome outcome, String testName,
                                           String testSetName, Environment env, XdmNode testInput,
                                           XsltExecutable sheet, ErrorCollector collector,
                                           XdmItem contextItem, XdmNode initialMode, QName initialModeName,
                                           QName initialTemplateName, String baseOutputURI) {
        try {
            XsltTransformer transformer = sheet.load();
            transformer.setURIResolver(env);
            transformer.setBaseOutputURI(baseOutputURI);
            if (env.unparsedTextResolver != null) {
                transformer.getUnderlyingController().setUnparsedTextURIResolver(env.unparsedTextResolver);
            }
            if (initialTemplateName != null) {
                transformer.setInitialTemplate(initialTemplateName);
            }
            if (initialMode != null) {
                try {
                    transformer.setInitialMode(initialModeName);
                } catch (IllegalArgumentException e) {
                    if (e.getCause() instanceof XPathException) {
                        collector.fatalError((XPathException) e.getCause());
                        throw new SaxonApiException(e.getCause());
                    } else {
                        throw e;
                    }
                }
            }
            testInput.select(child("param").where(not(isTrue("static")))).forEach(param -> {
                String name = param.attribute("name");
                String select = param.attribute("select");
                XdmValue value;
                try {
                    value = xpath.evaluate(select, null);
                } catch (SaxonApiException e) {
                    System.err.println("*** Error evaluating parameter " + name + ": " + e.getMessage());
                    throw new SaxonApiUncheckedException(e);
                }
                transformer.setParameter(new QName(name), value);
                setGlobalParameter(new QName(name), value);
            });
            if (contextItem != null) {
                transformer.setInitialContextNode((XdmNode) contextItem);
            }
            if (env.streamedFile != null) {
                transformer.setSource(new StreamSource(env.streamedFile));
            } else if (env.streamedContent != null) {
                transformer.setSource(new StreamSource(new StringReader(env.streamedContent), "inlineDoc"));
            }
            for (QName varName : env.params.keySet()) {
                transformer.setParameter(varName, env.params.get(varName));
                setGlobalParameter(varName, env.params.get(varName));
            }
            transformer.setErrorListener(collector);
           /* if (outputUri != null) {
                transformer.setBaseOutputURI(testInput.getBaseURI().resolve(outputUri).toString());
            } else {
                transformer.setBaseOutputURI(new File(resultsDir + "/results/output.xml").toURI().toString());
            }*/
            transformer.setMessageListener((content, terminate, locator) -> {
                outcome.addXslMessage(content);
                System.err.println(content.getStringValue());
            });


            // Run the transformation twice, once for serialized results, once for a tree.
            // TODO: we could be smarter about this and capture both

            // run with serialization
            StringWriter sw = new StringWriter();
            Serializer serializer = env.processor.newSerializer(sw);
            transformer.setDestination(serializer);
            transformer.getUnderlyingController().setOutputURIResolver(
                    new OutputResolver(env.processor, outcome, true));
            transformer.transform();
            outcome.setPrincipalSerializedResult(sw.toString());
            if (saveResults) {
                // currently, only save the principal result file
                saveResultsToFile(sw.toString(),
                        new File(resultsDir + "/results/" + testSetName + "/" + testName + ".out"));
                Map<URI, TestOutcome.SingleResultDoc> xslResultDocuments = outcome.getSecondaryResultDocuments();
                for (Map.Entry<URI, TestOutcome.SingleResultDoc> entry : xslResultDocuments.entrySet()) {
                    URI key = entry.getKey();
                    if (key != null) {
                        String path = key.getPath();
                        String serialization = outcome.serialize(env.processor, entry.getValue());
                        saveResultsToFile(serialization, new File(path));
                    }
                }
            }

            // run without serialization
            if (env.streamedContent != null) {
                transformer.setSource(new StreamSource(new StringReader(env.streamedContent), "inlineDoc"));
            }
            XdmDestination destination = new XdmDestination();
            destination.setTreeModel(treeModel);
            transformer.setDestination(destination);
            transformer.getUnderlyingController().setOutputURIResolver(
                    new OutputResolver(env.processor, outcome, false));
            transformer.transform();
            outcome.setPrincipalResult(destination.getXdmNode());
            outcome.setWarningsReported(collector.getFoundWarnings());
            //}
        } catch (SaxonApiException err) {
            outcome.setException(err);
            if (collector.getErrorCodes().isEmpty()) {
                outcome.addReportedError(err.getErrorCode().getLocalName());
            } else {
                outcome.setErrorsReported(collector.getErrorCodes());
            }
        } catch (Exception err) {
            err.printStackTrace();
            noteFailure(testSetName, testName);
            resultsDoc.writeTestcaseElement(testName, "fail", "*** crashed " + err.getClass() + ": " + err.getMessage());
            return true;
        }
        return false;
    }

    protected void clearGlobalParameters(){}

    protected void setGlobalParameter(QName qName, XdmValue value) {
        // For overriding in Javascript driver
    }

    protected void clearInitialFunctionArguments() {
    }

    protected void addInitialFunctionArgument(XdmValue value) {
        // For overriding in Javascript driver
    }

    protected boolean runWithXslt30Transformer(XdmNode testCase, XPathCompiler xpath, final TestOutcome outcome,
                                               String testName, String testSetName, Environment env,
                                               XdmNode testInput, XsltExecutable sheet, ErrorCollector collector,
                                               String xsltLanguageVersion, XdmItem contextItem, XdmNode initialMode,
                                               XdmNode initialFunction,
                                               XdmNode initialTemplate, QName initialModeName, XdmValue initialMatchSelection,
                                               QName initialTemplateName, String baseOutputURI) {
        try {
            boolean assertsSerial =
                testCase.select(path(child("result"),
                                     descendant("assert-serialization")
                                         .cat(descendant("assert-serialization-error"))
                                         .cat(descendant("serialization-matches")))).exists();
            boolean resultAsTree = env.outputTree;
            boolean serializationDeclared = env.outputSerialize;

            Optional<String> needsTree = testInput.select(path("output", "@tree")).asOptionalString();
            if (needsTree.isPresent()) {
                resultAsTree = needsTree.get().equals("yes");
            }

            testInput.select(path("output", "@result-var"))
                    .forEach(att -> outcome.setResultVar(att.getStringValue()));

            Optional<String> serializeAtt = testInput.select(path("output", "@serialize")).asOptionalString();
            if (serializeAtt.isPresent()) {
                serializationDeclared = serializeAtt.equals(Optional.of("yes"));
            }
            boolean resultSerialized = serializationDeclared || assertsSerial;

//                    if (assertsSerial) {
//                        String comment = outcome.getComment();
//                        comment = (comment == null ? "" : comment) + "*Serialization " + (serializationDeclared ? "declared* " : "required* ");
//                        outcome.setComment(comment);
//                    }


            Xslt30Transformer transformer = sheet.load30();
            transformer.setURIResolver(env);
            if (env.unparsedTextResolver != null) {
                transformer.getUnderlyingController().setUnparsedTextURIResolver(env.unparsedTextResolver);
            }
            if (tracing) {
                transformer.setTraceListener(new XSLTTraceListener());
            }

            Map<QName, XdmValue> caseGlobalParams = getNamedParameters(xpath, testInput, false, false);
            //Map<QName, XdmValue> caseStaticParams = getNamedParameters(xpath, testInput, true, false);
            Map<QName, XdmValue> globalParams = new HashMap<>(env.params);
            //globalParams.putAll(caseStaticParams);
            globalParams.putAll(caseGlobalParams);  // has to be this way to ensure test-local overrides global
            transformer.setStylesheetParameters(globalParams);

            if (contextItem != null) {
                transformer.setGlobalContextItem(contextItem);
            }

            transformer.setErrorListener(collector);

            transformer.setBaseOutputURI(baseOutputURI);

            transformer.setMessageListener((content, errorCode, terminate, locator) -> {
                outcome.addXslMessage(content);
                System.err.println(content.getStringValue());
            });

            XdmValue result = null;

            StringWriter sw = new StringWriter();

            Serializer serializer = env.processor.newSerializer(sw);
            //serializer.setOutputProperty(Serializer.Property.OMIT_XML_DECLARATION, "yes");


            XsltController controller = transformer.getUnderlyingController();

            Destination dest = null;
            if (resultAsTree && !resultSerialized) {
                // If we want non-serialized, we need to accumulate any result documents as trees too
                // But we can't do both, so serializing takes precedence.
                transformer.setResultDocumentHandler(
                        new ResultDocHandler(env.processor, outcome, baseOutputURI, false));
                dest = new XdmDestination();
                ((XdmDestination)dest).setTreeModel(treeModel);
            } else {
                transformer.setResultDocumentHandler(
                        new ResultDocHandler(env.processor, outcome, baseOutputURI, true));
            }
            if (resultSerialized) {
                dest = serializer;
            }

            Source src = null;
            if (env.streamedFile != null) {
                src = new StreamSource(env.streamedFile);
            } else if (env.streamedContent != null) {
                src = new StreamSource(new StringReader(env.streamedContent), "inlineDoc");
            } else if (initialTemplate == null && contextItem != null) {
                src = ((XdmNode) contextItem).getUnderlyingNode();
            }
            if (src != null && !"skip".equals(env.streamedInputValidation)) {
                Processor processor = sheet.getProcessor();
                SchemaValidator validator = processor.getSchemaManager().newSchemaValidator();
                src = validator.asSource(src);
            }

            if (src == null && initialFunction == null && initialTemplateName == null && initialModeName == null) {
                initialTemplateName = new QName("xsl", NamespaceConstant.XSLT, "initial-template");
            }

            try {
                if (initialModeName != null) {
                    transformer.setInitialMode(initialModeName);
                } else {
                    controller.getInitialMode();   /// has the side effect of setting to the unnamed
                }
            } catch (IllegalArgumentException e) {
                if (e.getCause() instanceof XPathException) {
                    collector.fatalError((XPathException) e.getCause());
                    throw new SaxonApiException(e.getCause());
                } else {
                    throw e;
                }
            }

            if (initialMode != null || initialTemplate != null) {
                XdmNode init = initialMode == null ? initialTemplate : initialMode;
                Map<QName, XdmValue> params = getNamedParameters(xpath, init, false, false);
                Map<QName, XdmValue> tunnelledParams = getNamedParameters(xpath, init, false, true);
                if (xsltLanguageVersion.equals("2.0")) {
                    if (!(params.isEmpty() && tunnelledParams.isEmpty())) {
                        System.err.println("*** Initial template parameters ignored for XSLT 2.0");
                    }
                } else {
                    transformer.setInitialTemplateParameters(params, false);
                    transformer.setInitialTemplateParameters(tunnelledParams, true);
                }
            }

            if (initialTemplateName != null) {
                transformer.setGlobalContextItem(contextItem);
                if (dest == null) {
                    result = transformer.callTemplate(initialTemplateName);
                } else {
                    transformer.callTemplate(initialTemplateName, dest);
                }
            } else if (initialFunction != null) {
                QName name = getQNameAttribute(xpath, initialFunction, attribute("name"));
                XdmValue[] params = getParameters(xpath, initialFunction);
                if (dest == null) {
                    result = transformer.callFunction(name, params);
                } else {
                    transformer.callFunction(name, params, dest);
                }
            } else if (initialMatchSelection != null) {
                if (dest == null) {
                    result = transformer.applyTemplates(initialMatchSelection);
                } else {
                    transformer.applyTemplates(initialMatchSelection, dest);
                }
            } else {
                if (dest == null) {
                    result = transformer.applyTemplates(src);
                } else {
                    transformer.applyTemplates(src, dest);
                }
            }

            outcome.setWarningsReported(collector.getFoundWarnings());
            if (resultAsTree && !resultSerialized) {
                result = ((XdmDestination) dest).getXdmNode();
            }
            // Don't overwrite the principal result if it already exists: this happens
            // when an xsl:result-document with empty or absent href has been executed
            if (resultSerialized && outcome.getPrincipalSerializedResult() == null) {
                outcome.setPrincipalSerializedResult(sw.toString());
            }
            if (outcome.getPrincipalResult() == null) {
                outcome.setPrincipalResult(result);
            }

            if (saveResults) {
                String s = sw.toString();
                // If a transform result is entirely xsl:result-document, then result will be null
                if (!resultSerialized && result != null) {
                    StringWriter sw2 = new StringWriter();
                    Serializer se = env.processor.newSerializer(sw2);
                    se.setOutputProperty(Serializer.Property.OMIT_XML_DECLARATION, "yes");
                    env.processor.writeXdmValue(result, se);
                    se.close();
                    s = sw2.toString();
                }
                // currently, only save the principal result file in the result directory
                saveResultsToFile(s,
                        new File(resultsDir + "/results/" + testSetName + "/" + testName + ".out"));
                Map<URI, TestOutcome.SingleResultDoc> xslResultDocuments = outcome.getSecondaryResultDocuments();
                for (Map.Entry<URI, TestOutcome.SingleResultDoc> entry : xslResultDocuments.entrySet()) {
                    URI key = entry.getKey();
                    String path = key.getPath();
                    String serialization = outcome.serialize(env.processor, entry.getValue());
                    saveResultsToFile(serialization, new File(path));
                }
            }
        } catch (SaxonApiException err) {
            if (err.getCause() instanceof XPathException &&
                    !((XPathException) err.getCause()).hasBeenReported()) {
                System.err.println("Thrown exception " + ((XPathException) err.getCause()).getErrorCodeLocalPart() +
                        ": " + err.getCause().getMessage());
            }
            outcome.setException(err);
            if (collector.getErrorCodes().isEmpty()) {
                if (err.getErrorCode() == null) {
                    outcome.addReportedError("Error_with_no_error_code");
                } else {
                    outcome.addReportedError(err.getErrorCode().getLocalName());
                }
            } else {
                outcome.setErrorsReported(collector.getErrorCodes());
            }
        } catch (Exception err) {
            err.printStackTrace();
            noteFailure(testSetName, testName);
            resultsDoc.writeTestcaseElement(testName, "fail", "*** crashed " + err.getClass() + ": " + err.getMessage());
            return true;
        }
        return false;
    }

    protected void initPatternOptimization(XsltCompiler compiler) {
    }

    protected XsltExecutable exportImport(String testName, String testSetName, TestOutcome outcome, XsltCompiler compiler, XsltExecutable sheet, ErrorCollector collector, Source styleSource) {
        try {
            if (export && isExportable(testName)) {
                sheet = exportStylesheet(testName, testSetName, compiler, sheet, styleSource);
            } else if (sheet == null) {
                sheet = compiler.compile(styleSource);
            }
        } catch (SaxonApiException err) {
            outcome.setException(err);
            if (err.getErrorCode() != null) {
                collector.getErrorCodes().add(err.getErrorCode().getLocalName());
            }
            outcome.setErrorsReported(collector.getErrorCodes());
        } catch (Exception err) {
            err.printStackTrace();
            System.err.println(err.getMessage());
            outcome.setException(new SaxonApiException(err));
            outcome.setErrorsReported(collector.getErrorCodes());
        }
        return sheet;
    }

    private boolean isExportable(String testName) {
        // there are so few exceptions that we hard-code them
        return !(testName.equals("load-xquery-module-004")      // static variable calling fn:load-xquery-module
                         || testName.equals("transform-004"));  // static variable calling fn:transform
    }

    protected XsltExecutable exportStylesheet(String testName, String testSetName, XsltCompiler compiler, XsltExecutable sheet, Source styleSource) throws SaxonApiException {
        try {
            File exportFile = new File(resultsDir + "/export/" + testSetName + "/" + testName + ".sef");
            XsltPackage compiledPack = compiler.compilePackage(styleSource);
            compiledPack.save(exportFile);
            sheet = reloadExportedStylesheet(compiler, exportFile);
        } catch (SaxonApiException e) {
            try {
                compiler.getErrorListener().fatalError(XPathException.makeXPathException(e));
            } catch (TransformerException te) {
                assert false;
            }
            //System.err.println(e.getMessage());
            //e.printStackTrace();  //temporary, for debugging
            throw e;
        }
        return sheet;
    }

    protected XsltExecutable reloadExportedStylesheet(XsltCompiler compiler, File exportFile) throws SaxonApiException {
        return compiler.loadExecutablePackage(exportFile.toURI());
    }

    private XsltPackage exportImportPackage(String testName, String testSetName, TestOutcome outcome, XsltCompiler compiler, XsltPackage pack, ErrorCollector collector) {
        try {
            if (export) {
                try {
                    File exportFile = new File(resultsDir + "/export/" + testSetName + "/" + testName + ".base.sef");
                    pack.save(exportFile);
                    return compiler.loadLibraryPackage(exportFile.toURI());
                } catch (SaxonApiException e) {
                    System.err.println(e.getMessage());
                    //e.printStackTrace();  //temporary, for debugging
                    throw e;
                }
            } else {
                return pack;
            }
        } catch (SaxonApiException err) {
            outcome.setException(err);
            if (err.getErrorCode() != null) {
                collector.getErrorCodes().add(err.getErrorCode().getLocalName());
            }
            outcome.setErrorsReported(collector.getErrorCodes());
        } catch (Exception err) {
            err.printStackTrace();
            System.err.println(err.getMessage());
            outcome.setException(new SaxonApiException(err));
            outcome.setErrorsReported(collector.getErrorCodes());
        }
        return pack;
    }

    /**
     * Run streamability tests
     */

    public void runStreamabilityTests(XPathCompiler xpc, XdmNode testCase) {
        // no action for Saxon-HE - ignore the test
    }

    /**
     * Return a set of named parameters as a map
     *
     * @param xpath     The XPath compiler to use
     * @param node      The node to search for <param> children
     * @param getStatic Whether to collect static or non-static sets
     * @param tunnel    Whether to collect tunnelled or non-tunnelled sets
     * @return Map of the evaluated parameters, keyed by QName
     * @throws SaxonApiException if things go wrong
     */
    protected Map<QName, XdmValue> getNamedParameters(XPathCompiler xpath, XdmNode node, boolean getStatic, boolean tunnel) throws SaxonApiException {
        Map<QName, XdmValue> params = new HashMap<>();
        int i = 1;
        Predicate<? super XdmNode> staticTest = isTrue("static");
        if (!getStatic) {
            staticTest = not(staticTest);
        }
        List<XdmNode> paramElements = node.select(child("param").where(staticTest)).asListOfNodes();
        for (XdmNode param : paramElements) {
            QName name = getQNameAttribute(xpath, param, attribute("name"));
            String select = param.attribute("select");
            String tunnelled = param.attribute("tunnel");
            QName as = getQNameAttribute(xpath, param, attribute("as"));
                // TODO: it won't always be a QName, could be a sequence type
            boolean required = tunnel == (tunnelled != null && tunnelled.equals("yes"));
            XdmValue value;
            if (name == null) {
                System.err.println("*** No name for parameter " + i + " in initial-template");
                throw new SaxonApiException("*** No name for parameter " + i + " in initial-template");
            }
            try {
                value = xpath.evaluate(select, null);
                i++;
            } catch (SaxonApiException e) {
                System.err.println("*** Error evaluating parameter " + name + " in initial-template : " + e.getMessage());
                throw e;
            }
            if (as != null) {
                value = new XdmAtomicValue(((AtomicValue) value.getUnderlyingValue()).getStringValue(),
                        new ItemTypeFactory(xpath.getProcessor()).getAtomicType(as));
            }
            if (required) {
                params.put(name, value);
            }
        }
        return params;
    }

    /**
     * Return a set of unnamed parameters as an array
     *
     * @param xpath The XPath compiler to use
     * @param node  The node to search for <param> children
     * @return Array of the parameter values
     */
    protected XdmValue[] getParameters(XPathCompiler xpath, XdmNode node) {
        ArrayList<XdmValue> params = new ArrayList<>();

        node.select(child("param").where(not(isTrue("static")))).forEach(param -> {
            String select = param.attribute("select");
            XdmValue value;
            try {
                value = xpath.evaluate(select, null);
            } catch (SaxonApiException e) {
                System.err.println("*** Error evaluating parameter " + params.size() + " in initial-function : " + e.getMessage());
                throw new SaxonApiUncheckedException(e);
            }
            params.add(value);
        });
        return params.toArray(new XdmValue[0]);
    }

    @SuppressWarnings({"ResultOfMethodCallIgnored"})
    protected void saveResultsToFile(String content, File file) {
        try {
            Query.createFileIfNecessary(file);
            FileWriter writer = new FileWriter(file);
            writer.append(content);
            writer.close();
        } catch (IOException e) {
            System.err.println("*** Failed to save results to " + file.getAbsolutePath());
            e.printStackTrace();
        }
    }

    protected void assertOptimization(XsltExecutable stylesheet, String assertion) throws SaxonApiException {

        XdmDestination builder = new XdmDestination();
        stylesheet.explain(builder);
        builder.close();
        XdmNode expressionTree = builder.getXdmNode();
        XPathCompiler xpe = stylesheet.getProcessor().newXPathCompiler();
        XPathSelector exp = xpe.compile(assertion).load();
        exp.setContextItem(expressionTree);
        XdmAtomicValue bv = (XdmAtomicValue) exp.evaluateSingle();
        if (bv == null || !bv.getBooleanValue()) {
            println("** Optimization assertion failed");
            println(expressionTree.toString());
            throw new SaxonApiException("Expected optimization not performed");
        }

    }

    // Dependencies which are always satisfied in Saxon. (Note, this doesn't necessarily
    // mean that we support all possible values for the dependency, only that we support
    // all the values that actually appear in the test suite as it exists today.)

    // The string in question is either a feature name, or it takes the form "feature/value"
    // where feature is the feature name (element name of a child of the dependencies element)
    // and value is the value of the "value" attribute.

    protected Set<String> alwaysOn = new HashSet<>();

    // Dependencies which are always satisfied in Saxon-EE but not in Saxon-HE or -PE

    protected Set<String> needsEE = new HashSet<>();

    // Dependencies which are always satisfied in Saxon-PE and -EE but not in Saxon-HE

    protected Set<String> needsPE = new HashSet<>();

    // Dependencies which are never satisfied in Saxon

    protected Set<String> alwaysOff = new HashSet<>();

    protected void setDependencyData() {
        alwaysOn.add("feature/disabling_output_escaping");
        alwaysOn.add("feature/serialization");
        alwaysOn.add("feature/namespace_axis");
        alwaysOn.add("feature/dtd");
        alwaysOn.add("feature/built_in_derived_types");
        alwaysOn.add("feature/remote_http");
        alwaysOn.add("feature/xsl-stylesheet-processing-instruction");
        alwaysOn.add("feature/fn-transform-XSLT");
        alwaysOn.add("available_documents");
        alwaysOn.add("ordinal_scheme_name");
        alwaysOn.add("default_calendar_in_date_formatting_functions");
        alwaysOn.add("supported_calendars_in_date_formatting_functions");
        alwaysOn.add("maximum_number_of_decimal_digits");
        alwaysOn.add("default_output_encoding");
        alwaysOn.add("unparsed_text_encoding");
        alwaysOn.add("recognize_id_as_uri_fragment");
        alwaysOn.add("feature/XPath_3.1");
        alwaysOn.add("feature/backwards_compatibility");

        needsPE.add("feature/Saxon-PE");
        needsPE.add("feature/dynamic_evaluation");

        needsEE.add("languages_for_numbering");
        needsEE.add("feature/streaming");
        needsEE.add("feature/schema_aware");
        needsEE.add("feature/Saxon-EE");
        //needsEE.add("feature/XSD_1.1");


        needsEE.add("feature/xquery_invocation");
        needsEE.add("feature/higher_order_functions");

        alwaysOn.add("detect_accumulator_cycles");
    }

    /**
     * Ensure that a dependency is satisfied, first by checking whether Saxon supports
     * the requested feature, and if necessary by reconfiguring Saxon so that it does;
     * if configuration changes are made, then resetActions should be registered to
     * reverse the changes when the test is complete.
     *
     * @param dependency the dependency to be checked
     * @param env        the environment in which the test runs. The method may modify this
     *                   environment provided the changes are reversed for subsequent tests.
     * @return true if the test can proceed, false if the dependencies cannot be
     * satisfied.
     */

    @Override
    public boolean ensureDependencySatisfied(XdmNode dependency, Environment env) {
        String type = dependency.getNodeName().getLocalName();
        String value = dependency.attribute("value");
        if (value == null) {
            value = "*";
        }

        String tv = type + "/" + value;

        boolean inverse = "false".equals(dependency.attribute("satisfied"));
        boolean needed = !"false".equals(dependency.attribute("satisfied"));

        if (alwaysOn.contains(type) || alwaysOn.contains(tv)) {
            return needed;
        }
        if (alwaysOff.contains(type) || alwaysOff.contains(tv)) {
            return !needed;
        }
        String edition = env.processor.getSaxonEdition();
        if (needsPE.contains(type) || needsPE.contains(tv)) {
            return (edition.equals("PE") || edition.equals("EE")) == needed;
        }
        if (needsEE.contains(type) || needsEE.contains(tv)) {
            return edition.equals("EE") == needed;
        }

        switch (type) {
            case "spec":
                boolean atLeast = value.endsWith("+");
                value = value.replace("+", "");
                String specName = spec.specAndVersion.replace("XT", "XSLT");
                int order = value.compareTo(specName);
                return atLeast ? order <= 0 : order == 0;

            case "feature":
                switch (value) {
                    case "XML_1.1": {
                        String requiredVersion = inverse ? "1.0" : "1.1";
                        final String oldVersion = env.processor.getXmlVersion();
                        env.resetActions.add(new Environment.ResetAction() {
                            @Override
                            public void reset(Environment env) {
                                env.processor.setXmlVersion(oldVersion);
                            }
                        });
                        env.processor.setXmlVersion(requiredVersion);
                        return true;
                    }
                    case "XSD_1.1": {
                        String requiredVersion = inverse ? "1.0" : "1.1";
                        final String oldVersion = env.processor.getConfigurationProperty(Feature.XSD_VERSION);
                        if (!oldVersion.equals(requiredVersion)) {
                            env.processor.setConfigurationProperty(Feature.XSD_VERSION, requiredVersion);
                            env.resetActions.add(new Environment.ResetAction() {
                                @Override
                                public void reset(Environment env) {
                                    env.processor.setConfigurationProperty(Feature.XSD_VERSION, oldVersion);
                                }
                            });
                        }
                        return true;
                    }
                    case "higher_order_functions":
                        return (edition.equals("PE") || edition.equals("EE")) ^ inverse;
                    case "simple-uca-fallback":
                        return !inverse;
                    case "advanced-uca-fallback":
                        return (edition.equals("PE") || edition.equals("EE")) ^ inverse;
                    case "streaming-fallback":
                        boolean required = !inverse;
                        final boolean old = env.processor.getConfigurationProperty(Feature.STREAMING_FALLBACK);
                        if (old != required) {
                            env.processor.setConfigurationProperty(Feature.STREAMING_FALLBACK, required);
                            env.resetActions.add(new Environment.ResetAction() {
                                @Override
                                public void reset(Environment env) {
                                    env.processor.setConfigurationProperty(Feature.STREAMING_FALLBACK, old);
                                }
                            });
                        }
                        return true;
                    default:
                        System.err.println("*** Unknown feature in HE: " + value);
                        return env.processor.getSaxonEdition().equals("HE") ? false : null;
                }

            case "default_language_for_numbering": {
                final String old = env.processor.getConfigurationProperty(Feature.DEFAULT_LANGUAGE);
                if (!value.equals(old)) {
                    env.processor.setConfigurationProperty(Feature.DEFAULT_LANGUAGE, value);
                    env.resetActions.add(new Environment.ResetAction() {
                        public void reset(Environment env) {
                            env.processor.setConfigurationProperty(Feature.DEFAULT_LANGUAGE, old);
                        }
                    });
                }
                return true;
            }
            case "enable_assertions": {
                boolean on = !inverse;
                final boolean old = env.xsltCompiler.isAssertionsEnabled();
                env.xsltCompiler.setAssertionsEnabled(on);
                env.resetActions.add(new Environment.ResetAction() {
                    public void reset(Environment env) {
                        env.xsltCompiler.setAssertionsEnabled(old);
                    }
                });
                return true;
            }
            case "extension-function":
                if (value.equals("Q{http://relaxng.org/ns/structure/1.0}schema-report#1")) {
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
                }
                return false;
            case "year_component_values":
                if ("support year zero".equals(value)) {
                    if (env != null) {
                        env.processor.setConfigurationProperty(Feature.XSD_VERSION, inverse ? "1.0" : "1.1");
                        return true;
                    } else {
                        return false;
                    }
                }
                return !inverse;
            case "additional_normalization_form":
                if ("support FULLY-NORMALIZED".equals(value)) {
                    return inverse;
                }
                return !inverse;

            case "on-multiple-match":
                env.resetActions.add(new Environment.ResetAction() {
                    @Override
                    public void reset(Environment env) {
                        env.xsltCompiler.getUnderlyingCompilerInfo().setRecoveryPolicy(Configuration.RECOVER_WITH_WARNINGS);
                    }
                });
                if ("error".equals(value)) {
                    env.xsltCompiler.getUnderlyingCompilerInfo().setRecoveryPolicy(Configuration.DO_NOT_RECOVER);
                } else {
                    env.xsltCompiler.getUnderlyingCompilerInfo().setRecoveryPolicy(Configuration.RECOVER_SILENTLY);
                }
                return true;

            case "ignore_doc_failure":
                env.resetActions.add(new Environment.ResetAction() {
                    @Override
                    public void reset(Environment env) {
                        env.xsltCompiler.getUnderlyingCompilerInfo().setRecoveryPolicy(Configuration.RECOVER_WITH_WARNINGS);
                    }
                });
                if (inverse) {
                    env.xsltCompiler.getUnderlyingCompilerInfo().setRecoveryPolicy(Configuration.DO_NOT_RECOVER);
                } else {
                    env.xsltCompiler.getUnderlyingCompilerInfo().setRecoveryPolicy(Configuration.RECOVER_SILENTLY);
                }
                return true;

            case "combinations_for_numbering":
                if (value.equals("COPTIC EPACT DIGIT ONE") || value.equals("SINHALA ARCHAIC DIGIT ONE") || value.equals("MENDE KIKAKUI DIGIT ONE")) {
                    return false;
                }
                return !inverse;

            case "xsd-version":
                return env.processor.getSaxonEdition().equals("HE") ? false : null;

            case "sweep_and_posture":
                return env.processor.getSaxonEdition().equals("HE") ? inverse : null;

            case "unicode-version":
                return value.equals("6.0"); // Avoid running Unicode 9.0 tests - they are slow!

            default:
                println("**** dependency not recognized for HE: " + type);
                return false;
        }
    }

    /**
     * Return a QNamed value from an attribute. This can handle active namespace prefix bindings or Clark notations in the
     * attribute string values
     *
     * @param compiler      XPath compiler
     * @param contextItem   Context item
     * @param attributePath Path to the required (singleton?) attribute
     * @return the value of the attribute as a QName
     */

    protected static QName getQNameAttribute(XPathCompiler compiler, XdmNode contextItem, Step<? extends XdmNode> attributePath) {
        Optional<XdmNode> att = contextItem.select(attributePath).asOptionalNode();
        if (att.isPresent()) {
            String val = att.get().getStringValue().trim();
            if (val.equals("#unnamed")) {
                return new QName(NamespaceConstant.XSLT, "unnamed");
            } else if (val.equals("#default")) {
                return new QName(NamespaceConstant.XSLT, "default");
            } else if (val.startsWith("{")) {
                return new QName(StructuredQName.fromClarkName(val));
            } else if (val.startsWith("Q{")) {
                return new QName(StructuredQName.fromEQName(val));
            } else if (val.contains(":")){
                try {
                    QNameValue qn = ResolveQName.resolveQName(val, att.get().getUnderlyingNode().getParent());
                    return new QName(qn.getStructuredQName());
                } catch (XPathException e) {
                    e.printStackTrace();
                    return null;
                }
            } else {
                return new QName(val);
            }
        } else {
            return null;
        }
    }

    protected static class ResultDocHandler implements java.util.function.Function<URI, Destination> {

        private Processor proc;
        private TestOutcome outcome;
        private URI baseOutputURI;
        boolean serialized;

        public ResultDocHandler(Processor proc, TestOutcome outcome, String baseOutputURI, boolean serialized) {
            this.proc = proc;
            this.outcome = outcome;
            try {
                this.baseOutputURI = new URI(baseOutputURI);
            } catch (URISyntaxException e) {
                throw new IllegalArgumentException(e);
            }
            this.serialized = serialized;
        }

        public Destination apply(URI uri) {
            if (serialized) {
                StringWriter stringWriter = new StringWriter();
                Serializer destination = proc.newSerializer(stringWriter);
                destination.setDestinationBaseURI(uri);
                if (uri.equals(baseOutputURI)) {
                    destination.onClose(() -> outcome.setPrincipalSerializedResult(stringWriter.toString()));
                } else {
                    destination.onClose(() -> outcome.setSecondaryResult(uri, null, stringWriter.toString()));
                }
                return destination;
            } else {
                XdmDestination destination = new XdmDestination();
                destination.setBaseURI(uri);
                if (uri.equals(baseOutputURI)) {
                    destination.onClose(() -> outcome.setPrincipalResult(destination.getXdmNode()));
                } else {
                    destination.onClose(() -> outcome.setSecondaryResult(uri, destination.getXdmNode(), null));
                }
                return destination;
            }
        }

    }

    protected static class OutputResolver implements OutputURIResolver {

        private Processor proc;
        private TestOutcome outcome;
        private Destination destination;
        private StringWriter stringWriter;
        boolean serialized;
        URI uri;

        public OutputResolver(Processor proc, TestOutcome outcome, boolean serialized) {
            this.proc = proc;
            this.outcome = outcome;
            this.serialized = serialized;
        }

        public OutputResolver newInstance() {
            return new OutputResolver(proc, outcome, serialized);
        }

        public Result resolve(String href, String base) throws XPathException {
            try {
                uri = new URI(base).resolve(href);
                if (serialized) {
                    //destination = proc.newSerializer();
                    stringWriter = new StringWriter();
                    StreamResult result = new StreamResult(stringWriter);
                    result.setSystemId(uri.toString());
                    return result;
//                    ((Serializer)destination).setOutputWriter(stringWriter);
//                    Receiver r = destination.getReceiver(proc.getUnderlyingConfiguration());
//                    r.setSystemId(uri.toString());
//                    return r;
                } else {
                    destination = new XdmDestination();
                    destination.setDestinationBaseURI(uri);
                    return destination.getReceiver(null, null); // TBA
                    //return new SequenceNormalizerWithSpaceSeparator(r);
                }
            } catch (SaxonApiException | URISyntaxException e) {
                throw new XPathException(e);
            }
        }

        public void close(Result result) throws XPathException {
            if (uri != null) {
                if (serialized) {
                    outcome.setSecondaryResult(uri, null, stringWriter == null ? "" : stringWriter.toString());
                } else {
                    XdmDestination xdm = (XdmDestination) destination;
                    if (xdm != null) {
                        outcome.setSecondaryResult(xdm.getDestinationBaseURI(), xdm.getXdmNode(), null);
                    }
                }
            }
        }

    }
}
