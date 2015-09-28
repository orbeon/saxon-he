////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
// Copyright (c) 2014 Saxonica Limited.
// This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
// If a copy of the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
// This Source Code Form is "Incompatible With Secondary Licenses", as defined by the Mozilla Public License, v. 2.0.
////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

package net.sf.saxon.style;

import net.sf.saxon.Configuration;
import net.sf.saxon.PreparedStylesheet;
import net.sf.saxon.expr.PackageData;
import net.sf.saxon.expr.instruct.LocationMap;
import net.sf.saxon.om.*;
import net.sf.saxon.trans.CompilerInfo;
import net.sf.saxon.trans.XPathException;
import net.sf.saxon.tree.linked.DocumentImpl;
import net.sf.saxon.tree.linked.ElementImpl;
import net.sf.saxon.value.AtomicValue;
import net.sf.saxon.value.DecimalValue;
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
    private CompilerInfo compilerInfo;
    private DecimalValue processorVersion;
    //private StyleNodeFactory nodeFactory;
    private StylesheetPackage stylesheetPackage;
    private PackageData packageData;
    private LocationMap locationMap = new LocationMap();
    private int errorCount = 0;
    private boolean schemaAware;
    private Map<StructuredQName, ValueAndPrecedence> staticVariables = new HashMap<StructuredQName, ValueAndPrecedence>();
    private Map<DocumentURI, DocumentInfo> stylesheetModules = new HashMap<DocumentURI, DocumentInfo>();
    private Stack<DocumentURI> importStack = new Stack<DocumentURI>(); // handles both include and import

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
        this.compilerInfo = info;
        schemaAware = info.isSchemaAware();
        processorVersion = info.getXsltVersion();
        PackageData pd = new PackageData(config);
        pd.setConfiguration(config);
        pd.setAllowXPath30(info.getXsltVersion().equals(DecimalValue.THREE));
        pd.setHostLanguage(Configuration.XSLT);
        pd.setSchemaAware(isSchemaAware());
        pd.setLocationMap(getLocationMap());
        packageData = pd;
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
        Compilation compilation = new Compilation(config, compilerInfo);
        StylesheetPackage pp = compilation.compilePackage(source);
        int errs = compilation.getErrorCount();
        if (errs > 0) {
            throw new XPathException("Stylesheet compilation failed: " + errs + " error" + (errs == 1 ? "" : "s") + " reported");
        }
        PreparedStylesheet pss = new PreparedStylesheet(compilation);
        try {
            pp.getPrincipalStylesheetModule().updatePreparedStylesheet(pss);
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
        errs = compilation.getErrorCount();
        if (errs > 0) {
            throw new XPathException("Stylesheet compilation failed: " + errs + " error" + (errs == 1 ? "" : "s") + " reported");
        }
        return pss;
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

    public StylesheetPackage compilePackage(Source source)
            throws XPathException {
        DocumentImpl document = StylesheetModule.loadStylesheetModule(source, true, this, NestedIntegerValue.TWO);
        ElementImpl element = document.getDocumentElement();
        if (element instanceof LiteralResultElement) {
            document = ((LiteralResultElement) element).makeStylesheet(true);
            element = document.getDocumentElement();
        }

        XSLPackage xslpackage;
        try {
            if (element instanceof XSLPackage) {
                xslpackage = (XSLPackage) element;
                // For "real" packages (those with a real xsl:package element at the root), use the global
                // location map for line number information, so that location information from different packages
                // can be mixed
                if (element.getLocalPart().equals("package")) {
                    // Note that in PE and HE, whilst packages aren't supported, their configurations do not have
                    // a global location map - hence we should probably use the default local.
                    LocationMap lm = getConfiguration().getGlobalLocationMap();
                    if (lm != null) {
                        locationMap = lm;
                        packageData.setLocationMap(locationMap);
                    }
                }
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
        if (processorVersion.equals(DecimalValue.ZERO)) {
            processorVersion = xslpackage.getVersion();
        }
        StyleNodeFactory factory = getStyleNodeFactory(true);
        StylesheetPackage pp = factory.newStylesheetPackage(xslpackage);
        pp.setVersion(xslpackage.getVersion());
        pp.setPackageVersion(xslpackage.getPackageVersion());
        pp.setPackageName(xslpackage.getName());
        pp.getRuleManager().setRecoveryPolicy(info.getRecoveryPolicy());
        setStylesheetPackage(pp);
        pp.createFunctionLibrary(this);
        try {
            pp.preprocess();
        } catch (XPathException e) {
            try {
                info.getErrorListener().fatalError(e);
            } catch (TransformerException e3) {
                // ignore an error thrown by the ErrorListener
            }
            throw e;
        }

        if (getErrorCount() == 0) {
            try {
                pp.fixup();
            } catch (XPathException e) {
                reportError(e);
            }
        }

        // Compile the stylesheet package
        if (getErrorCount() == 0) {
            try {
                pp.compile(this);
            } catch (XPathException e) {
                reportError(e);
            }
        }

        if (getErrorCount() == 0) {
            try {
                pp.complete();
            } catch (XPathException e) {
                reportError(e);
            }
        }


        return pp;
    }

    /**
     * Get the Saxon Configuration used by this Compilation
     *
     * @return the Saxon Configuration
     */

    public Configuration getConfiguration() {
        return getPackageData().getConfiguration();
    }

    /**
     * Get the LocationMap which keeps track of module and line number information
     * for diagnostics
     *
     * @return the LocationMap
     */

    public LocationMap getLocationMap() {
        return locationMap;
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
        return packageData;
    }

    /**
     * Internal method called during the compilation of a package to register the XSLT version number that
     * appeared at the outermost level of the root module (typically the version attribute on the xsl:package or
     * xsl:stylesheet element)
     *
     * @param v the version number as a decimal value
     */

    public void setVersion(DecimalValue v) {
        if (v.equals(DecimalValue.THREE)) {
            processorVersion = v;
            packageData.setAllowXPath30(true);
        } else {
            processorVersion = DecimalValue.TWO;
            packageData.setAllowXPath30(false);
        }
    }

    /**
     * Get the XSLT version number that appeared at the outermost level of the root module (typically the
     * version attribute on the xsl:package or xsl:stylesheet element)
     *
     * @return the XSLT version number, assuming it has already been read; otherwise null.
     */

    public DecimalValue getVersion() {
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
        packageData.setSchemaAware(schemaAware);
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
        //factory.setXsltProcessorVersion(getCompilerInfo().getXsltVersion());
        return factory;
    }

    private void setStylesheetPackage(StylesheetPackage module) {
        this.stylesheetPackage = module;
    }

    /**
     * Get the (most recent) stylesheet package compiled using this Compilation
     *
     * @return the most recent stylesheet package compiled
     */

    public StylesheetPackage getStylesheetPackage() {
        return stylesheetPackage;
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

    public Map<DocumentURI, DocumentInfo> getStylesheetModules() {
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
}
