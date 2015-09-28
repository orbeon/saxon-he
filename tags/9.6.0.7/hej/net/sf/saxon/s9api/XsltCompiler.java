////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
// Copyright (c) 2014 Saxonica Limited.
// This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
// If a copy of the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
// This Source Code Form is "Incompatible With Secondary Licenses", as defined by the Mozilla Public License, v. 2.0.
////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

package net.sf.saxon.s9api;

import net.sf.saxon.Configuration;
import net.sf.saxon.PreparedStylesheet;
import net.sf.saxon.lib.ErrorGatherer;
import net.sf.saxon.lib.StringCollator;
import net.sf.saxon.om.AxisInfo;
import net.sf.saxon.om.NodeInfo;
import net.sf.saxon.om.StandardNames;
import net.sf.saxon.pattern.NameTest;
import net.sf.saxon.style.Compilation;
import net.sf.saxon.style.StylesheetModule;
import net.sf.saxon.trace.XSLTTraceCodeInjector;
import net.sf.saxon.trans.CompilerInfo;
import net.sf.saxon.trans.PackageLibrary;
import net.sf.saxon.trans.XPathException;
import net.sf.saxon.tree.iter.AxisIterator;
import net.sf.saxon.tree.linked.DocumentImpl;
import net.sf.saxon.tree.linked.ElementImpl;
import net.sf.saxon.type.Type;
import net.sf.saxon.type.ValidationException;
import net.sf.saxon.value.DecimalValue;
import net.sf.saxon.value.NestedIntegerValue;

import javax.xml.transform.ErrorListener;
import javax.xml.transform.Source;
import javax.xml.transform.URIResolver;
import java.util.ArrayList;
import java.util.List;

/**
 * An XsltCompiler object allows XSLT 2.0 stylesheets to be compiled. The compiler holds information that
 * represents the static context for the compilation.
 * <p/>
 * <p>To construct an XsltCompiler, use the factory method {@link Processor#newXsltCompiler} on the Processor object.</p>
 * <p/>
 * <p>An XsltCompiler may be used repeatedly to compile multiple queries. Any changes made to the
 * XsltCompiler (that is, to the static context) do not affect queries that have already been compiled.
 * An XsltCompiler may be used concurrently in multiple threads, but it should not then be modified once
 * initialized.</p>
 *
 * @since 9.0
 */
public class XsltCompiler {

    private Processor processor;
    private Configuration config;
    private CompilerInfo compilerInfo;


    /**
     * Protected constructor. The public way to create an <tt>XsltCompiler</tt> is by using the factory method
     * {@link Processor#newXsltCompiler} .
     *
     * @param processor the Saxon processor
     */

    protected XsltCompiler(Processor processor) {
        this.processor = processor;
        this.config = processor.getUnderlyingConfiguration();
        compilerInfo = new CompilerInfo(config.getDefaultXsltCompilerInfo());
        compilerInfo.setGenerateByteCode(config.isGenerateByteCode(Configuration.XSLT));
    }

    /**
     * Get the Processor from which this XsltCompiler was constructed
     *
     * @return the Processor to which this XsltCompiler belongs
     * @since 9.3
     */

    public Processor getProcessor() {
        return processor;
    }

    /**
     * Set the URIResolver to be used during stylesheet compilation. This URIResolver, despite its name,
     * is <b>not</b> used for resolving relative URIs against a base URI; it is used for dereferencing
     * an absolute URI (after resolution) to return a {@link javax.xml.transform.Source} representing the
     * location where a stylesheet module can be found.
     * <p/>
     * <p>This URIResolver is used to dereference the URIs appearing in <code>xsl:import</code>,
     * <code>xsl:include</code>, and <code>xsl:import-schema</code> declarations. It is not used
     * for resolving the URI supplied for the main stylesheet module (as supplied to the
     * {@link #compile(javax.xml.transform.Source)} or {@link #compilePackage(javax.xml.transform.Source)} methods.
     * It is not used at run-time for resolving requests to the <code>document()</code> or similar functions.</p>
     *
     * @param resolver the URIResolver to be used during stylesheet compilation.
     */

    public void setURIResolver(URIResolver resolver) {
        compilerInfo.setURIResolver(resolver);
    }

    /**
     * Set the value of a stylesheet parameter. Static (compile-time) parameters must be provided using
     * this method on the XsltCompiler object, prior to stylesheet compilation. Non-static parameters
     * may also be provided using this method if their values will not vary from one transformation
     * to another.
     *
     * @param name  the StructuredQName of the parameter
     * @param value as a XdmValue of the parameter
     */
    public void setParameter(QName name, XdmValue value) {
        compilerInfo.setParameter(name.getStructuredQName(), value.getUnderlyingValue());
    }

    /**
     * Get the URIResolver to be used during stylesheet compilation.
     *
     * @return the URIResolver used during stylesheet compilation. Returns null if no user-supplied
     *         URIResolver has been set.
     */

    public URIResolver getURIResolver() {
        return compilerInfo.getURIResolver();
    }

    /**
     * Set the ErrorListener to be used during this compilation episode
     *
     * @param listener The error listener to be used. This is notified of all errors detected during the
     *                 compilation.
     */

    public void setErrorListener(ErrorListener listener) {
        compilerInfo.setErrorListener(listener);
    }

    /**
     * Get the ErrorListener being used during this compilation episode
     *
     * @return listener The error listener in use. This is notified of all errors detected during the
     *         compilation. Returns null if no user-supplied ErrorListener has been set.
     */

    public ErrorListener getErrorListener() {
        return compilerInfo.getErrorListener();
    }

    /**
     * Say that the stylesheet must be compiled to be schema-aware, even if it contains no
     * xsl:import-schema declarations. Normally a stylesheet is treated as schema-aware
     * only if it contains one or more xsl:import-schema declarations. If it is not schema-aware,
     * then all input documents must be untyped, and validation of temporary trees is disallowed
     * (though validation of the final result tree is permitted). Setting the argument to true
     * means that schema-aware code will be compiled regardless.
     *
     * @param schemaAware If true, the stylesheet will be compiled with schema-awareness
     *                    enabled even if it contains no xsl:import-schema declarations. If false, the stylesheet
     *                    is treated as schema-aware only if it contains one or more xsl:import-schema declarations.
     * @since 9.2
     */

    public void setSchemaAware(boolean schemaAware) {
        compilerInfo.setSchemaAware(schemaAware);
    }

    /**
     * Ask whether schema-awareness has been requested by means of a call on
     * {@link #setSchemaAware}
     *
     * @return true if schema-awareness has been requested
     * @since 9.2
     */

    public boolean isSchemaAware() {
        return compilerInfo.isSchemaAware();
    }

    /**
     * Bind a collation URI to a collation
     *
     * @param uri       the absolute collation URI
     * @param collation a {@link java.text.Collator} object that implements the required collation
     * @throws IllegalArgumentException if an attempt is made to rebind the standard URI
     *                                  for the Unicode codepoint collation
     * @since 9.5
     * @deprecated since 9.6. Collations are now held globally. If this method is called, the effect
     * is to update the pool of collations held globally by the Processor.
     */

    public void declareCollation(String uri, final java.text.Collator collation) {
        getProcessor().declareCollation(uri, collation);
    }

    /**
     * Declare the default collation
     *
     * @param uri the absolute URI of the default collation. This URI must have been bound to a collation
     *            using the method {@link #declareCollation(String, java.text.Collator)}, or it must be a
     *            collation that is recognized implicitly, such as a UCA collation
     * @throws IllegalStateException if the collation URI has not been registered, unless it is the standard
     *                               Unicode codepoint collation which is registered implicitly
     * @since 9.5
     */

    public void declareDefaultCollation(String uri) {
        StringCollator c;
        try {
            c = getProcessor().getUnderlyingConfiguration().getCollation(uri);
        } catch (XPathException e) {
            c = null;
        }
        if (c == null) {
            throw new IllegalStateException("Unknown collation " + uri);
        }
        compilerInfo.setDefaultCollation(uri);
    }

    /**
     * Set the XSLT (and XPath) language level to be supported by the processor.
     *
     * @param version the language level to be supported. The value 2.0 requests a processor conforming to the
     *                XSLT 2.0 specification; 3.0 requests an XSLT 3.0 processor, while the value "0.0" (which is the
     *                default setting in the absence of a call on this method) gets a processor according to the
     *                value of the version attribute on the xsl:stylesheet element.
     *                <p>Although this interface can be used to run a 1.0 stylesheet, it is not possible to request
     *                a 1.0 processor; Saxon will handle it as a 2.0 processor in backwards-compatibility mode, which
     *                is not quite the same thing.</p>
     *                <p>Note that Saxon-HE does not support XSLT 3.0. However, requesting 3.0
     *                via this interface does not cause an error; an error only occurs if the stylesheet
     *                actually uses XSLT 3.0 features.</p>
     *
     * @throws IllegalArgumentException if the value is not numerically equal to 0.0, 2.0, or 3.0
     * @since 9.3
     */

    public void setXsltLanguageVersion(String version) {
        DecimalValue v;
        try {
            v = (DecimalValue) DecimalValue.makeDecimalValue(version, true).asAtomic();
        } catch (ValidationException ve) {
            throw new IllegalArgumentException(ve);
        }
        if (!DecimalValue.ZERO.equals(v) && !DecimalValue.TWO.equals(v) && !DecimalValue.THREE.equals(v)) {
            throw new IllegalArgumentException("LanguageVersion " + v);
        }
        compilerInfo.setXsltVersion(v);
    }

    /**
     * Get the XSLT (and XPath) language level supported by the processor.
     *
     * @return the language level supported. The value 2.0 represents a processor conforming to the
     *         XSLT 2.0 specification; 3.0 represents an XSLT 3.0 processor, while the value zero (which is the
     *         default setting in the absence of a call on {@link #setXsltLanguageVersion} represents a processor
     *         selected according to the value of the version attribute on the xsl:stylesheet element.
     * @since 9.3
     */

    public String getXsltLanguageVersion() {
        return compilerInfo.getXsltVersion().toString();
    }

    /**
     * Set whether trace hooks are to be included in the compiled code. To use tracing, it is necessary
     * both to compile the code with trace hooks included, and to supply a TraceListener at run-time
     *
     * @param option true if trace code is to be compiled in, false otherwise
     * @since 9.3
     */

    public void setCompileWithTracing(boolean option) {
        if (option) {
            compilerInfo.setCodeInjector(new XSLTTraceCodeInjector());
        } else {
            compilerInfo.setCodeInjector(null);
        }
    }

    /**
     * Ask whether trace hooks are included in the compiled code.
     *
     * @return true if trace hooks are included, false if not.
     * @since 9.3
     */

    public boolean isCompileWithTracing() {
        return compilerInfo.isCompileWithTracing();
    }

    /**
     * Set whether bytecode should be generated for the compiled stylesheet. This option
     * is available only with Saxon-EE. The default depends on the setting in the configuration
     * at the time the XsltCompiler is instantiated, and by default is true for Saxon-EE.
     *
     * @param option true if bytecode is to be generated, false otherwise
     * @since 9.6
     */

    public void setGenerateByteCode(boolean option) {
        compilerInfo.setGenerateByteCode(option);
    }

    /**
     * Ask whether bytecode is to be generated in the compiled code.
     *
     * @return true if bytecode is to be generated, false if not.
     * @since 9.6
     */

    public boolean isGenerateByteCode() {
        return compilerInfo.isGenerateByteCode();
    }



//    /**
//     * Import a compiled XQuery function library.
//     * @param queryCompiler An XQueryCompiler that has been used to compile a library of XQuery functions
//     * (by using one of the overloaded methods named <code>compileLibrary</code>).
//     * @param namespace The namespace of the functions that are to be made available to the stylesheet.
//     * All the global functions with this namespace that are
//     * defined in libraries that have been compiled using this XQueryCompiler are added to the static context
//     * of the XSLT stylesheet. The stylesheet does not need to (and should not) contain a call on
//     * <code>saxon:import-query</code> to import these functions.
//     */
//
//    public void importXQueryLibrary(XQueryCompiler queryCompiler, String namespace) {
//        compilerInfo.importXQueryLibrary(queryCompiler.)
//    }


    /**
     * Get the stylesheet associated
     * via the xml-stylesheet processing instruction (see
     * http://www.w3.org/TR/xml-stylesheet/) with the document
     * document specified in the source parameter, and that match
     * the given criteria.  If there are several suitable xml-stylesheet
     * processing instructions, then the returned Source will identify
     * a synthesized stylesheet module that imports all the referenced
     * stylesheet module.
     *
     * <p>The returned Source will have an absolute URI, created by resolving
     * any relative URI against the base URI of the supplied source document,
     * and redirected if necessary by using the URIResolver associated with this
     * <code>XsltCompiler</code>.</p>
     * @param source  The XML source document. Note that if the source document
     * is available as an instance of {@link XdmNode}, a corresponding <code>Source</code>
     * can be obtained using the method {@link net.sf.saxon.s9api.XdmNode#asSource()}.
     * If the source is a StreamSource or SAXSource, it will be read only as far as the
     * xml-stylesheet processing instruction (but the Source will be consumed and must not
     * be re-used).
     * @param media   The media attribute to be matched.  May be null, in which
     *                case the prefered templates will be used (i.e. alternate = no).
     * @param title   The value of the title attribute to match.  May be null.
     * @param charset The value of the charset attribute to match.  May be null.
     * @return A Source object suitable for passing to {@link #compile(javax.xml.transform.Source)}.
     * @throws SaxonApiException if any problems occur, including the case where no matching
     * xml-stylesheet processing instruction is found.
     * @since 9.6
     */


    public Source getAssociatedStylesheet(Source source, String media, String title, String charset)
            throws SaxonApiException {
        try {
            return StylesheetModule.getAssociatedStylesheet(config, compilerInfo.getURIResolver(), source, media, title, charset);
        } catch (XPathException e) {
            throw new SaxonApiException(e);
        }
    }



    /**
     * Compile a library package.
     * <p/>
     * <p>The source argument identifies an XML file containing an &lt;xsl:package&gt; element. Any packages
     * on which this package depends must have been made available to the <code>XsltCompiler</code>
     * by importing them using {@link #importPackage}.</p>
     *
     * @param source identifies an XML document holding the the XSLT package to be compiled
     * @return the XsltPackage that results from the compilation. Note that this package
     *         is not automatically imported to this <code>XsltCompiler</code>; if the package is required
     *         for use in subsequent compilations then it must be explicitly imported.
     * @throws SaxonApiException if the source cannot be read or if static errors are found during the
     *                           compilation. Any such errors will have been notified to the registered <code>ErrorListener</code>
     *                           if there is one, or reported on the <code>System.err</code> output stream otherwise.
     * @since 9.6
     */

    public XsltPackage compilePackage(Source source) throws SaxonApiException {
        try {
            Compilation compilation = new Compilation(config, compilerInfo);
            XsltPackage pack = new XsltPackage(processor, compilation.compilePackage(source));
            if (compilation.getErrorCount() > 0) {
                throw new SaxonApiException("Package compilation failed: " + compilation.getErrorCount() + " errors reported");
            }
            return pack;
        } catch (XPathException e) {
            throw new SaxonApiException(e.getMessage());
        }
    }


    /**
     * Compile a list of packages.
     * @param sources the collection of packages to be compiled, in the form of an Iterable
     * @return the collection of compiled packages, in the form of an Iterable.
     * @throws SaxonApiException if the source cannot be read or if static errors are found during the
     *                           compilation. Any such errors will have been notified to the registered <code>ErrorListener</code>
     *                           if there is one, or reported on the <code>System.err</code> output stream otherwise.
     * @since 9.6
     */
    public Iterable<XsltPackage> compilePackages(Iterable<Source> sources) throws SaxonApiException, XPathException {

        List<DocumentImpl> sourcesCopy = new ArrayList<DocumentImpl>();
        Compilation compilation = new Compilation(config, compilerInfo);
        compilerInfo.setPackageLibrary(new PackageLibrary());
        List<XsltPackage> result = new ArrayList<XsltPackage>();
        for (Source s : sources) {
            DocumentImpl document = StylesheetModule.loadStylesheetModule(s, true, compilation, NestedIntegerValue.TWO);
            ElementImpl packageElement = document.getDocumentElement();
            String packageName = packageElement.getAttributeValue("", "name");
            if (packageName == null) {
                throw new SaxonApiException("Outermost element must be an xsl:package element with a name attribute");
            }

            sourcesCopy.add(document);
        }


        while (!sourcesCopy.isEmpty()) {
            DocumentImpl compiled = null;
            for (DocumentImpl doc : sourcesCopy) {
                AxisIterator iter = doc.iterateAxis(AxisInfo.DESCENDANT,
                        new NameTest(Type.ELEMENT, StandardNames.XSL_USE_PACKAGE, config.getNamePool()));
                boolean canCompile = true;
                NodeInfo current;
                while ((current = iter.next()) != null) {
                    String usedPackageName = current.getAttributeValue("", "name");
                    if (compilerInfo.getPackageLibrary().getPackage(usedPackageName) == null) {
                        canCompile = false;
                        break;
                    }
                }
                if (canCompile) {
                    XsltPackage packagei = compilePackage(doc);
                    compilerInfo.getPackageLibrary().addPackage(packagei.getName(), packagei.getUnderlyingPreparedPackage());
                    result.add(packagei);
                    compiled = doc;
                    break;
                }
            }
            if (compiled == null) {
                throw new SaxonApiException("Unable to compile any of the packages supplied");
            } else {
                sourcesCopy.remove(compiled);
            }
        }

        return result;
    }


//    /**
//     * Load a compiled package from a file or from a remote location. NOT
//     * <p/>
//     * <p>The supplied URI represents the location of a resource which must have been originally
//     * created using {@link XsltPackage#save(java.io.File)}.</p>
//     * <p/>
//     * <p>The result of loading the package is returned as an <code>XsltPackage</code> object.
//     * Note that this package is not automatically imported to this <code>XsltCompiler</code>;
//     * if the package is required for use in subsequent compilations then it must be explicitly
//     * imported.</p>
//     *
//     * @param location the location from which the package is to be loaded, as a URI
//     * @return the compiled package loaded from the supplied file or remote location
//     * @throws SaxonApiException if no resource can be loaded from the supplied location or if the
//     *                           resource that is loaded is not a compiled package, or if the compiled package is not
//     *                           consistent with this <code>XsltCompiler</code> (for example, if it was created using an
//     *                           incompatible Saxon version).
//     * @since 9.6
//     */
//
//    public XsltPackage loadPackage(URI location) throws SaxonApiException {
//        return null;
//    }

    /**
     * Import a library package. Calling this method makes the supplied package available for reference
     * in the <code>xsl:use-package</code> declaration of subsequent compilations performed using this
     * <code>XsltCompiler</code>.
     *
     * @param thePackage the package to be imported
     * @throws SaxonApiException if the imported package was created under a different {@link Processor}
     * @since 9.6
     */

    public void importPackage(XsltPackage thePackage) throws SaxonApiException {
        if (thePackage.getProcessor() != processor) {
            throw new SaxonApiException("The imported package and the XsltCompiler must belong to the same Processor");
        }
        compilerInfo.getPackageLibrary().addPackage(thePackage.getName(), thePackage.getUnderlyingPreparedPackage());
    }


    /**
     * Compile a stylesheet.
     * <p/>
     * <p><i>Note: the term "compile" here indicates that the stylesheet is converted into an executable
     * form. There is no implication that this involves code generation.</i></p>
     * <p/>
     * <p>The source argument identifies the XML document holding the principal stylesheet module. Other
     * modules will be located relative to this module by resolving against the base URI that is defined
     * as the systemId property of the supplied Source.</p>
     * <p/>
     * <p>The following kinds of {@link javax.xml.transform.Source} are recognized:</p>
     * <p/>
     * <ul>
     * <li>{@link javax.xml.transform.stream.StreamSource}, allowing the stylesheet to be supplied as a
     * URI, as a {@link java.io.File}, as an {@link java.io.InputStream}, or as a {@link java.io.Reader}</li>
     * <li>{@link javax.xml.transform.sax.SAXSource}, allowing the stylesheet to be supplied as a stream
     * of SAX events from a SAX2-compliant XML parser (or any other source of SAX events)</li>
     * <li>{@link javax.xml.transform.dom.DOMSource}, allowing the stylesheet to be supplied as a
     * DOM tree. This option is available only if saxon9-dom.jar is on the classpath.</li>
     * <li>Document wrappers for XOM, JDOM, or DOM4J trees, provided the appropriate support libraries
     * are on the classpath</li>
     * <li>A Saxon NodeInfo, representing the root of a tree in any of the native tree formats supported
     * by Saxon</li>
     * <li>An {@link XdmNode} representing the document node of the stylesheet module</li>
     * </ul>
     *
     * @param source Source object representing the principal stylesheet module to be compiled
     * @return an XsltExecutable, which represents the compiled stylesheet.
     * @throws SaxonApiException if the stylesheet contains static errors or if it cannot be read. Note that
     *                           the exception that is thrown will <b>not</b> contain details of the actual errors found in the stylesheet. These
     *                           will instead be notified to the registered ErrorListener. The default ErrorListener displays error messages
     *                           on the standard error output.
     */

    public XsltExecutable compile(/*@NotNull*/ Source source) throws SaxonApiException {
        try {
            PreparedStylesheet pss = Compilation.compileSingletonPackage(config, compilerInfo, source);
            return new XsltExecutable(processor, pss);
        } catch (XPathException e) {
            throw new SaxonApiException(e);
        }
    }

    /**
     * Get the underlying CompilerInfo object, which provides more detailed (but less stable) control
     * over some compilation options
     *
     * @return the underlying CompilerInfo object, which holds compilation-time options. The methods on
     *         the CompilerInfo object are not guaranteed stable from release to release.
     */

    public CompilerInfo getUnderlyingCompilerInfo() {
        return compilerInfo;
    }

    /**
     * Supply a List object which will be populated with information about any static errors
     * encountered during the transformation.
     * @param errorList a List (typically empty) to which information will be appended about
     * static errors found during the compilation. Each such error is represented by a
     * {@link StaticError} object.
     */

    public void setErrorList(List<StaticError> errorList){
        compilerInfo.setErrorListener(new ErrorGatherer(errorList));
    }

}

