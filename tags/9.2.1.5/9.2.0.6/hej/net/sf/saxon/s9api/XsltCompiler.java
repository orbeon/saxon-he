package net.sf.saxon.s9api;

import net.sf.saxon.Configuration;
import net.sf.saxon.PreparedStylesheet;
import net.sf.saxon.trans.CompilerInfo;

import javax.xml.transform.ErrorListener;
import javax.xml.transform.Source;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.URIResolver;

/**
 *  An XsltCompiler object allows XSLT 2.0 stylesheets to be compiled. The compiler holds information that
 *  represents the static context for the compilation.
 *
  * <p>To construct an XsltCompiler, use the factory method {@link Processor#newXsltCompiler} on the Processor object.</p>
 *
 * <p>An XsltCompiler may be used repeatedly to compile multiple queries. Any changes made to the
 * XsltCompiler (that is, to the static context) do not affect queries that have already been compiled.
 * An XsltCompiler may be used concurrently in multiple threads, but it should not then be modified once
 * initialized.</p>
 * @since 9.0
 */
public class XsltCompiler {

    private Processor processor;
    private Configuration config;
    private CompilerInfo compilerInfo;

    /**
     * Protected constructor. The public way to create an <tt>XsltCompiler</tt> is by using the factory method
     * {@link Processor#newXsltCompiler} .
     * @param processor the Saxon processor
     */

    protected XsltCompiler(Processor processor) {
        this.processor = processor;
        this.config = processor.getUnderlyingConfiguration();
        compilerInfo = new CompilerInfo(config.getDefaultXsltCompilerInfo());
    }

    /**
     * Set the URIResolver to be used during stylesheet compilation. This URIResolver, despite its name,
     * is <b>not</b> used for resolving relative URIs against a base URI; it is used for dereferencing
     * an absolute URI (after resolution) to return a {@link javax.xml.transform.Source} representing the
     * location where a stylesheet module can be found.
     *
     * <p>This URIResolver is used to dereference the URIs appearing in <code>xsl:import</code>,
     * <code>xsl:include</code>, and <code>xsl:import-schema</code> declarations.
     * It is not used at run-time for resolving requests to the <code>document()</code> or similar functions.</p>
     *
     * @param resolver the URIResolver to be used during stylesheet compilation.
     */

    public void setURIResolver(URIResolver resolver) {
        compilerInfo.setURIResolver(resolver);
    }

    /**
     * Get the URIResolver to be used during stylesheet compilation.
     *
     * @return the URIResolver used during stylesheet compilation. Returns null if no user-supplied
     * URIResolver has been set.
     */

    public URIResolver getURIResolver() {
        return compilerInfo.getURIResolver();
    }

    /**
     * Set the ErrorListener to be used during this compilation episode
     * @param listener The error listener to be used. This is notified of all errors detected during the
     * compilation.
     */

    public void setErrorListener(ErrorListener listener) {
        compilerInfo.setErrorListener(listener);
    }

    /**
     * Get the ErrorListener being used during this compilation episode
     * @return listener The error listener in use. This is notified of all errors detected during the
     * compilation. Returns null if no user-supplied ErrorListener has been set.
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
     * @param schemaAware If true, the stylesheet will be compiled with schema-awareness
     * enabled even if it contains no xsl:import-schema declarations. If false, the stylesheet
     * is treated as schema-aware only if it contains one or more xsl:import-schema declarations.
     * @since 9.2
     */

    public void setSchemaAware(boolean schemaAware) {
        compilerInfo.setSchemaAware(schemaAware);
    }

    /**
     * Ask whether schema-awareness has been requested by means of a call on
     * {@link #setSchemaAware}
     * @return true if schema-awareness has been requested
     * @since 9.2
     */

    public boolean isSchemaAware() {
        return compilerInfo.isSchemaAware();
    }

    /**
     * Set whether trace hooks are to be included in the compiled code. To use tracing, it is necessary
     * both to compile the code with trace hooks included, and to supply a TraceListener at run-time
     * @param option true if trace code is to be compiled in, false otherwise
     */

    public void setCompileWithTracing(boolean option) {
        compilerInfo.setCompileWithTracing(option);
    }

    /**
     * Ask whether trace hooks are included in the compiled code.
     * @return true if trace hooks are included, false if not.
     */

    public boolean isCompileWithTracing() {
        return compilerInfo.isCompileWithTracing();
    }

    /**
     * Compile a stylesheet.
     *
     * <p><i>Note: the term "compile" here indicates that the stylesheet is converted into an executable
     * form. There is no implication that this involves code generation.</i></p>
     *
     * <p>The source argument identifies the XML document holding the principal stylesheet module. Other
     * modules will be located relative to this module by resolving against the base URI that is defined
     * as the systemId property of the supplied Source.</p>
     *
     * <p>The following kinds of {@link javax.xml.transform.Source} are recognized:</p>
     *
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
     * @param source Source object representing the principal stylesheet module to be compiled
     * @return an XsltExecutable, which represents the compiled stylesheet.
     * @throws SaxonApiException if the stylesheet contains static errors or if it cannot be read. Note that
     * the exception that is thrown will <b>not</b> contain details of the actual errors found in the stylesheet. These
     * will instead be notified to the registered ErrorListener. The default ErrorListener displays error messages
     * on the standard error output.
     */

    public XsltExecutable compile(Source source) throws SaxonApiException {
        try {
            PreparedStylesheet pss = PreparedStylesheet.compile(source, config, compilerInfo);
            return new XsltExecutable(processor, pss);
        } catch (TransformerConfigurationException e) {
            throw new SaxonApiException(e);
        }
    }

}

//
// The contents of this file are subject to the Mozilla Public License Version 1.0 (the "License");
// you may not use this file except in compliance with the License. You may obtain a copy of the
// License at http://www.mozilla.org/MPL/
//
// Software distributed under the License is distributed on an "AS IS" basis,
// WITHOUT WARRANTY OF ANY KIND, either express or implied.
// See the License for the specific language governing rights and limitations under the License.
//
// The Original Code is: all this file
//
// The Initial Developer of the Original Code is Michael H. Kay.
//
// Contributor(s):
//

