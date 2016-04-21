////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
// Copyright (c) 2015 Saxonica Limited.
// This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
// If a copy of the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
// This Source Code Form is "Incompatible With Secondary Licenses", as defined by the Mozilla Public License, v. 2.0.
////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

package net.sf.saxon.style;

import net.sf.saxon.Configuration;
import net.sf.saxon.PreparedStylesheet;
import net.sf.saxon.expr.PackageData;
import net.sf.saxon.om.*;
import net.sf.saxon.pattern.NodeKindTest;
import net.sf.saxon.trans.CompilerInfo;
import net.sf.saxon.trans.XPathException;
import net.sf.saxon.type.Type;
import net.sf.saxon.value.AtomicValue;
import net.sf.saxon.value.NestedIntegerValue;

import javax.xml.transform.ErrorListener;
import javax.xml.transform.Source;
import javax.xml.transform.TransformerException;
import java.util.HashMap;
import java.util.Map;
import java.util.Stack;

/**
 * Represents an XSLT compilation episode, compiling a single package.
 */
public class Compilation {

    // diagnostic switch to control output of timing information
    public final static boolean TIMING = false;
    private Configuration config;
    private CompilerInfo compilerInfo;
    private int processorVersion; // the first version attribute in the package or stylesheet, times ten
    private PrincipalStylesheetModule principalStylesheetModule;
    private int errorCount = 0;
    private boolean schemaAware;
    private QNameParser qNameParser;
    private Map<StructuredQName, ValueAndPrecedence> staticVariables = new HashMap<StructuredQName, ValueAndPrecedence>();
    private Map<DocumentURI, TreeInfo> stylesheetModules = new HashMap<DocumentURI, TreeInfo>();
    private Stack<DocumentURI> importStack = new Stack<DocumentURI>(); // handles both include and import
    private PackageData packageData;

    private static class ValueAndPrecedence {
        public ValueAndPrecedence(GroundedValue v, NestedIntegerValue p) {
            this.value = v;
            this.precedence = p;
        }

        public GroundedValue value;
        public NestedIntegerValue precedence;
    }

    /**
     * Create a compilation object ready to perform an XSLT compilation
     *
     * @param config the Saxon Configuration
     * @param info   compilation options
     */

    public Compilation(Configuration config, CompilerInfo info) {
        this.config = config;
        this.compilerInfo = info;
        processorVersion = info.getXsltVersion();
        schemaAware = info.isSchemaAware();

//        PackageData pd = new PackageData(config);
//        pd.setConfiguration(config);
//        pd.setXPathVersion(info.getXsltVersion() == 30 ? 31 : 20);
//        pd.setHostLanguage(Configuration.XSLT);
//        pd.setSchemaAware(isSchemaAware());
//        pd.setLocationMap(getLocationMap());
//        packageData = pd;

        qNameParser = new QNameParser(null);
        qNameParser.setAcceptEQName(true);
        qNameParser.setDefaultNamespace("");
        qNameParser.setErrorOnBadSyntax("XTSE0020");
        qNameParser.setErrorOnUnresolvedPrefix("XTSE0280");
    }

    /**
     * Static factory method: Compile an XSLT stylesheet consisting of a single package
     *
     * @param config       the Saxon Configuration
     * @param compilerInfo the compilation options
     * @param source       the source of the root stylesheet module in the package to be compiled. This may
     *                     contain an xsl:package element at its root, or it may be a conventional xsl:stylesheet or xsl:transform,
     *                     or a "simplified stylesheet" rooted at a literal result element
     * @return the PreparedStylesheet representing the result of the compilation
     * @throws XPathException if any errors occur. The errors will have been reported to the ErrorListener
     *                        contained in the CompilerInfo.
     */

    public static PreparedStylesheet compileSingletonPackage(Configuration config, CompilerInfo compilerInfo, Source source) throws XPathException {
        try {
            Compilation compilation = new Compilation(config, compilerInfo);
            return StylesheetModule.loadStylesheet(source, compilation);
//        }
//
//
//        Compilation compilation = new Compilation(config, compilerInfo);
//        PrincipalStylesheetModule pp = compilation.compilePackage(source);
//        int errs = compilation.getErrorCount();
//        if (errs > 0) {
//            throw new XPathException("Stylesheet compilation failed: " + errs + " error" + (errs == 1 ? "" : "s") + " reported");
//        }
//        PreparedStylesheet pss = new PreparedStylesheet(compilation);
//        try {
//            pp.getStylesheetPackage().checkForAbstractComponents();
//            pp.getStylesheetPackage().updatePreparedStylesheet(pss);
//            pss.addPackage(compilation.getPackageData());
        } catch (XPathException err) {
            if (!err.hasBeenReported()) {
                try {
                    compilerInfo.getErrorListener().fatalError(err);
                } catch (TransformerException e2) {
                    // ignore error
                }
            }
            throw err;
        }
//        errs = compilation.getErrorCount();
//        if (errs > 0) {
//            throw new XPathException("Stylesheet compilation failed: " + errs + " error" + (errs == 1 ? "" : "s") + " reported");
//        }
//        return pss;
    }

    public void setPackageData(PackageData pack) {
        this.packageData = pack;
    }

    public void setMinimalPackageData() {
        if (getPackageData() == null) {
            // Create a temporary PackageData for use during use-when processing
            PackageData pd = new PackageData(getConfiguration());
            pd.setXPathVersion(getProcessorVersion() == 30 ? 31 : getProcessorVersion());
            pd.setHostLanguage(Configuration.XSLT);
            pd.setTargetEdition(compilerInfo.getTargetEdition());
            packageData = pd;
        }
    }

    /**
     * Compile a stylesheet package
     *
     * @param source the XML document containing the top-level stylesheet module in the package,
     *               which may be an xsl:package element, an xsl:stylesheet or xsl:transform element, or a
     *               "simplified stylesheet" rooted at a literal result element
     * @return the StylesheetPackage representing the result of the compilation
     * @throws XPathException if any error occurs while compiling the package
     */

    public PrincipalStylesheetModule compilePackage(Source source) throws XPathException {
        setMinimalPackageData();
        NodeInfo document;
        NodeInfo outermost = null;
        if (source instanceof NodeInfo) {
            NodeInfo root = (NodeInfo)source;
            if (root.getNodeKind() == Type.DOCUMENT) {
                document = root;
                outermost = document.iterateAxis(AxisInfo.CHILD, NodeKindTest.ELEMENT).next();
            } else if (root.getNodeKind() == Type.ELEMENT) {
                document = root.getRoot();
                outermost = root;
            }
        }
        if (!(outermost instanceof XSLPackage)) {
            document = StylesheetModule.loadStylesheetModule(source, true, this, NestedIntegerValue.TWO);
            outermost = document.iterateAxis(AxisInfo.CHILD, NodeKindTest.ELEMENT).next();
        }

        if (outermost instanceof LiteralResultElement) {
            document = ((LiteralResultElement) outermost).makeStylesheet(true);
            outermost = document.iterateAxis(AxisInfo.CHILD, NodeKindTest.ELEMENT).next();
        }

        XSLPackage xslpackage;
        try {
            if (outermost instanceof XSLPackage) {
                xslpackage = (XSLPackage) outermost;
            } else {
                throw new XPathException("Outermost element must be xsl:package, xsl:stylesheet, or xsl:transform");
            }
        } catch (XPathException e) {
            if (!e.hasBeenReported()) {
                try {
                    getCompilerInfo().getErrorListener().fatalError(e);
                } catch (TransformerException e1) {
                    // ignore secondary error
                }
            }
            throw e;
        }
        CompilerInfo info = getCompilerInfo();
//        if (info.getXsltVersion() == 0) {
//            setProcessorVersion(getXPathVersion());
//        }
        StyleNodeFactory factory = getStyleNodeFactory(true);
        PrincipalStylesheetModule psm = factory.newPrincipalModule(xslpackage);
        StylesheetPackage pack = psm.getStylesheetPackage();
        pack.setVersion(xslpackage.getVersion());
        pack.setPackageVersion(xslpackage.getPackageVersion());
        pack.setPackageName(xslpackage.getName());
        pack.setSchemaAware(info.isSchemaAware());
        pack.setXPathVersion(processorVersion >= 30 ? 31 : 20);
        pack.createFunctionLibrary();
        if (info.getExtensionFunctionLibrary() != null) {
            pack.getFunctionLibrary().addFunctionLibrary(info.getExtensionFunctionLibrary());
        }
        psm.getRuleManager().setRecoveryPolicy(info.getRecoveryPolicy());
        psm.getRuleManager().setCompilerInfo(info);
        setPrincipalStylesheetModule(psm);
        packageData = null;

        try {
            psm.preprocess();
        } catch (XPathException e) {
            try {
                info.getErrorListener().fatalError(e);
            } catch (TransformerException e3) {
                // ignore an error thrown by the ErrorListener
            }
            throw e;
        }

        // project:preconditions
        psm.findKeyablePatterns(this);

        if (getErrorCount() == 0) {
            try {
                psm.fixup();
            } catch (XPathException e) {
                reportError(e);
            }
        }

        // Compile the stylesheet package
        if (getErrorCount() == 0) {
            try {
                psm.compile(this);
            } catch (XPathException e) {
                reportError(e);
            }
        }

        if (getErrorCount() == 0) {
            try {
                psm.complete();
            } catch (XPathException e) {
                reportError(e);
            }
        }


        return psm;
    }

    /**
     * Get the Saxon Configuration used by this Compilation
     *
     * @return the Saxon Configuration
     */

    public Configuration getConfiguration() {
        return config;
    }

    /**
     * Get the compilation options used by this compilation
     *
     * @return the compilation options
     */

    public CompilerInfo getCompilerInfo() {
        return compilerInfo;
    }

    /**
     * Get information about the package that was compiled in the course of this Compilation
     *
     * @return package information
     */

    public PackageData getPackageData() {
        if (packageData != null) {
            return packageData;
        }
        return principalStylesheetModule == null ? null : principalStylesheetModule.getStylesheetPackage();
    }

    /**
     * Set the XSLT processor version, determining whether processing should be done according to the XSLT 2.0
     * or XSLT 3.0 specification. This is initially based on the externally-requested version, as set in the
     * CompilerInfo object from which this Compilation was produced. But if no specific version was requested,
     * then the processor version in the Compilation object is determined according to the version attribute
     * of the xsl:package/xsl:stylesheet element at the root of the principal stylesheet module.
     *
     * @param v the version number times ten as an integer value. If the value is set to 3.0, an XSLT 3.0 processor will
     *          be used; if any other value is supplied, an XSLT 2.0 processor is used.
     */

    public void setProcessorVersion(int v) {
        if (processorVersion == 0) {
            if (v == 20 || v == 30) {
                processorVersion = v;
                if (getPackageData() != null) {
                    getPackageData().setXPathVersion(v);
                }
            }
        }
    }

    /**
     * Get the XSLT processor version, determining whether processing should be done according to the XSLT 2.0
     * or XSLT 3.0 specification. This is initially based on the externally-requested version, as set in the
     * CompilerInfo object from which this Compilation was produced. But if no specific version was requested,
     * then the processor version in the Compilation object is determined according to the version attribute
     * of the xsl:package/xsl:stylesheet element at the root of the principal stylesheet module.
     *
     * @return the XSLT processor version number, times ten as an int value. Always 2.0 or 3.0, represented as 20 and 30,
     * or zero meaning undefined
     */

    public int getProcessorVersion() {
        return processorVersion;
    }

    /**
     * Ask whether this compilation is schema-aware. It is schema-aware either if this is explicitly
     * requested in the supplied CompilerInfo, or if this is explicitly requested within the stylesheet
     * source code, for example by the presence of an <code>xsl:import-schema</code> declaration or a
     * <code>validation</code> attribute
     *
     * @return true if the compilation is schema-aware
     */

    public boolean isSchemaAware() {
        return schemaAware;
    }

    /**
     * Say that this compilation is schema-aware. This method is called internally during the course
     * of a compilation if an <code>xsl:import-schema</code> declaration is encountered.
     *
     * @param schemaAware true if the compilation is schema-aware.
     */

    public void setSchemaAware(boolean schemaAware) {
        this.schemaAware = schemaAware;
        getPackageData().setSchemaAware(schemaAware);
    }

    /**
     * Get the StyleNodeFactory used to build the stylesheet tree
     *
     * @param topLevel true if the factory is for the top-level (package) module of a package
     * @return the StyleNodeFactory
     */

    public StyleNodeFactory getStyleNodeFactory(boolean topLevel) {
        StyleNodeFactory factory = getConfiguration().makeStyleNodeFactory(this);
        factory.setTopLevelModule(topLevel);
        return factory;
    }

    private void setPrincipalStylesheetModule(PrincipalStylesheetModule module) {
        this.principalStylesheetModule = module;
    }

    /**
     * Get the (most recent) stylesheet package compiled using this Compilation
     *
     * @return the most recent stylesheet package compiled
     */

    public PrincipalStylesheetModule getPrincipalStylesheetModule() {
        return principalStylesheetModule;
    }

    /**
     * Report a compile time error. This calls the errorListener to output details
     * of the error, and increments an error count.
     *
     * @param err the exception containing details of the error
     */

    public void reportError(XPathException err) {
        ErrorListener el = compilerInfo.getErrorListener();
        if (el == null) {
            el = getConfiguration().getErrorListener();
        }
        if (!err.hasBeenReported()) {
            errorCount++;
            try {
                el.fatalError(err);
                err.setHasBeenReported(true);
            } catch (Exception err2) {
                // ignore secondary error
            }
        } else if (errorCount == 0) {
            errorCount++;
        }
    }

    /**
     * Get the number of errors reported so far
     *
     * @return the number of errors reported
     */

    public int getErrorCount() {
        return errorCount;
    }

    /**
     * Report a compile time warning. This calls the errorListener to output details
     * of the warning.
     *
     * @param err an exception holding details of the warning condition to be
     *            reported
     */

    public void reportWarning(XPathException err) {
        ErrorListener el = compilerInfo.getErrorListener();
        if (el == null) {
            el = getConfiguration().getErrorListener();
        }
        if (el != null) {
            try {
                el.warning(err);
            } catch (Exception err2) {
                // ignore secondary error
            }
        }
    }

    /**
     * Declare a static variable
     *
     * @param name       the name of the variable
     * @param value      the value of the variable
     * @param precedence the import precedence in the form of a "decimal" value (e.g. 2.14.6)
     * @throws XPathException if, for example, the value of the variable is incompatible with other
     *                        variables having the same name
     */

    public void declareStaticVariable(StructuredQName name, GroundedValue value, NestedIntegerValue precedence) throws XPathException {
        ValueAndPrecedence vp = staticVariables.get(name);
        if (vp != null) {
            if (vp.precedence.compareTo(precedence) < 0) {
                // new value must be compatible with the old, see spec bug 24478
                if (!valuesAreCompatible(value, vp.value)) {
                    throw new XPathException("Incompatible values assigned for static variable " + name.getDisplayName(), "XTSE3450");
                }
            } else {
                return; // ignore the new value
            }
        }
        staticVariables.put(name, new ValueAndPrecedence(value, precedence));
    }

    /**
     * Test whether two values are the same in the sense of error XTSE3450
     *
     * @param val0 the first value
     * @param val1 the second value
     * @return true if the values are the same: if they are atomic values, they must be "identical";
     * if they are nodes, they must be the same node.
     */

    private boolean valuesAreCompatible(GroundedValue val0, GroundedValue val1) {
        if (val0.getLength() != val1.getLength()) {
            return false;
        }
        if (val0.getLength() == 1) {
            Item i0 = val0.head();
            Item i1 = val1.head();
            if (i0 instanceof AtomicValue) {
                return i1 instanceof AtomicValue && ((AtomicValue) i0).isIdentical((AtomicValue) i1);
            } else if (i0 instanceof NodeInfo) {
                return i1 instanceof NodeInfo && ((NodeInfo) i0).isSameNodeInfo((NodeInfo) i1);
            } else {
                return i0 == i1;
            }
        } else {
            for (int i = 0; i < val0.getLength(); i++) {
                if (!valuesAreCompatible((GroundedValue) val0.itemAt(i), (GroundedValue) val1.itemAt(i))) {
                    return false;
                }
            }
            return true;
        }
    }

    /**
     * Get the value of a static variable
     *
     * @param name the name of the required variable
     * @return the value of the variable if there is one, or null if the variable is undeclared
     */

    public GroundedValue getStaticVariable(StructuredQName name) {
        ValueAndPrecedence vp = staticVariables.get(name);
        return vp == null ? null : vp.value;
    }

    /**
     * Get the map of stylesheet modules. This includes an entry for every stylesheet module
     * in the compilation; the key is the absolute URI, and the value is the corresponding
     * document.
     *
     * @return the map from absolute URIs to (documents containing) stylesheet modules
     */

    public Map<DocumentURI, TreeInfo> getStylesheetModules() {
        return stylesheetModules;
    }

    /**
     * Get the stack of include/imports, used to detect circularities
     *
     * @return the include/import stack
     */

    public Stack<DocumentURI> getImportStack() {
        return importStack;
    }

    /**
     * Get the QNameParser for parsing QNames in this compilation
     * Note that the namespaceResolver will be unitialized
     * @return the QNameParser
     */

    public QNameParser getQNameParser() {
        return qNameParser;
    }
}
