////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
// Copyright (c) 2015 Saxonica Limited.
// This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
// If a copy of the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
// This Source Code Form is "Incompatible With Secondary Licenses", as defined by the Mozilla Public License, v. 2.0.
////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

package net.sf.saxon.jaxp;

import net.sf.saxon.Configuration;
import net.sf.saxon.lib.DelegatingErrorListener;
import net.sf.saxon.lib.FeatureKeys;
import net.sf.saxon.lib.UnfailingErrorListener;
import net.sf.saxon.s9api.Processor;
import net.sf.saxon.s9api.SaxonApiException;
import net.sf.saxon.s9api.XsltCompiler;
import net.sf.saxon.s9api.XsltExecutable;
import net.sf.saxon.trans.CompilerInfo;
import net.sf.saxon.trans.ConfigurationReader;
import net.sf.saxon.trans.XPathException;
import org.xml.sax.XMLFilter;

import javax.xml.transform.*;
import javax.xml.transform.dom.DOMResult;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.sax.*;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;
import java.io.File;
import java.util.Comparator;


/**
 * A SaxonTransformerFactory instance can be used to create Transformer and Template
 * objects.
 * <p/>
 * <p>The system property that determines which Factory implementation
 * to create is named "javax.xml.transform.TransformerFactory". This
 * property names a concrete subclass of the TransformerFactory abstract
 * class. If the property is not defined, a platform default is be used.</p>
 * <p/>
 * <p>This implementation class implements the abstract methods on both the
 * javax.xml.transform.TransformerFactory and javax.xml.transform.sax.SAXTransformerFactory
 * classes.</p>
 * <p/>
 * <p>Since Saxon 9.6, the JAXP transformation interface is re-implemented as a layer
 * on top of the s9api interface. This will affect applications that attempt to
 * down-cast from JAXP interfaces to the underlying implementation classes.</p>
 */

public class SaxonTransformerFactory extends SAXTransformerFactory {

    private Processor processor;

    /**
     * Default constructor.
     */
    public SaxonTransformerFactory() {
        processor = new Processor(true);
    }

    /**
     * Construct a TransformerFactory using an existing Configuration.
     *
     * @param config the Saxon configuration
     */
    public SaxonTransformerFactory(Configuration config) {
        processor = new Processor(config);
    }

    /**
     * Set the configuration. This can also be done using the JAXP method
     * setAttribute, with the attribute name {@link net.sf.saxon.lib.FeatureKeys#CONFIGURATION}
     *
     * @param config the Saxon configuration
     */

    public void setConfiguration(Configuration config) {
        processor.setConfigurationProperty(FeatureKeys.CONFIGURATION, config);
    }

    /**
     * Get the configuration. This can also be done using the JAXP method
     * getAttribute, with the attribute name {@link net.sf.saxon.lib.FeatureKeys#CONFIGURATION}
     *
     * @return the Saxon configuration
     */

    public Configuration getConfiguration() {
        return processor.getUnderlyingConfiguration();
    }

    /**
     * Process the Source into a Transformer object.
     * The Transformer object must not be used in multiple threads running concurrently.
     * Different TransformerFactories can be used concurrently by different
     * threads.
     *
     * @param source An object that holds a URI, input stream, etc. of the stylesheet
     *               used to perform the transformation.
     * @return A Transformer object that may be used to perform a transformation
     *         in a single thread, never null.
     * @throws javax.xml.transform.TransformerConfigurationException
     *          May throw this during the parse
     *          when it is constructing the Templates object and fails.
     */

    public Transformer newTransformer(Source source)
            throws TransformerConfigurationException {
        Templates templates = newTemplates(source);
        return templates.newTransformer();
    }

    /**
     * Create a new Transformer object that performs a copy
     * of the source to the result.
     *
     * @return A Transformer object that may be used to perform a transformation
     *         in a single thread, never null.
     * @throws javax.xml.transform.TransformerConfigurationException
     *          May throw this during
     *          the parse when it is constructing the
     *          Templates object and fails.
     */

    public Transformer newTransformer()
            throws TransformerConfigurationException {

        return new IdentityTransformer(processor.getUnderlyingConfiguration());
    }


    /**
     * Process the Source into a Templates object, which is a
     * a compiled representation of the source. This Templates object
     * may then be used concurrently across multiple threads.  Creating
     * a Templates object allows the TransformerFactory to do detailed
     * performance optimization of transformation instructions, without
     * penalizing runtime transformation.
     *
     * @param source An object that holds a URL, input stream, etc.
     * @return A Templates object capable of being used for transformation purposes,
     *         never null.
     * @throws javax.xml.transform.TransformerConfigurationException
     *          May throw this during the parse when it
     *          is constructing the Templates object and fails.
     */

    public Templates newTemplates(Source source)
            throws TransformerConfigurationException {

        try {
            XsltCompiler compiler = processor.newXsltCompiler();
            XsltExecutable executable = compiler.compile(source);
            return new TemplatesImpl(executable);
        } catch (SaxonApiException e) {
            throw new TransformerConfigurationException(e);
        }
    }

    /**
     * Process the Source into a Templates object, which is a
     * a compiled representation of the source. This Templates object
     * may then be used concurrently across multiple threads.  Creating
     * a Templates object allows the TransformerFactory to do detailed
     * performance optimization of transformation instructions, without
     * penalizing runtime transformation.
     *
     * @param source An object that holds a URL, input stream, etc.
     * @param info   compile-time options for this stylesheet compilation
     * @return A Templates object capable of being used for transformation purposes,
     *         never null.
     * @throws javax.xml.transform.TransformerConfigurationException
     *          May throw this during the parse when it
     *          is constructing the Templates object and fails.
     */

    public Templates newTemplates(Source source, CompilerInfo info)
            throws TransformerConfigurationException {

        try {
            XsltCompiler compiler = processor.newXsltCompiler();
            compiler.getUnderlyingCompilerInfo().copyFrom(info);
            return new TemplatesImpl(compiler.compile(source));
        } catch (SaxonApiException e) {
            throw new TransformerConfigurationException(e);
        }
    }


    /**
     * Get the stylesheet specification(s) associated
     * via the xml-stylesheet processing instruction (see
     * http://www.w3.org/TR/xml-stylesheet/) with the document
     * document specified in the source parameter, and that match
     * the given criteria.  Note that it is possible to return several
     * stylesheets, in which case they are applied as if they were
     * a list of imports or cascades.
     *
     * @param source  The XML source document.
     * @param media   The media attribute to be matched.  May be null, in which
     *                case the prefered templates will be used (i.e. alternate = no).
     *                Note that Saxon does not implement the complex CSS3-based syntax for
     *                media queries. By default, the media value is simply ignored. An algorithm for
     *                comparing the requested media with the declared media can be defined using
     *                the method {@link Configuration#setMediaQueryEvaluator(Comparator)}.
     * @param title   The value of the title attribute to match.  May be null.
     * @param charset The value of the charset attribute to match.  May be null.
     * @return A Source object suitable for passing to the TransformerFactory.
     * @throws javax.xml.transform.TransformerConfigurationException
     *          if any problems occur
     */

    public Source getAssociatedStylesheet(
            Source source, String media, String title, String charset)
            throws TransformerConfigurationException {
        try {
            XsltCompiler compiler = processor.newXsltCompiler();
            return compiler.getAssociatedStylesheet(source, media, title, charset);
        } catch (SaxonApiException e) {
            throw new TransformerConfigurationException(e);
        }
    }


    /**
     * Set an object that is used by default during the transformation
     * to resolve URIs used in xsl:import, or xsl:include.
     *
     * @param resolver An object that implements the URIResolver interface,
     *                 or null.
     */

    public void setURIResolver(URIResolver resolver) {
        getConfiguration().setURIResolver(resolver);
    }

    /**
     * Get the object that is used by default during the transformation
     * to resolve URIs used in document(), xsl:import, or xsl:include.
     *
     * @return The URIResolver that was set with setURIResolver.
     */

    public URIResolver getURIResolver() {
        return getConfiguration().getURIResolver();
    }

    //======= CONFIGURATION METHODS =======

    private static final String FEATURE_SECURE_PROCESSING =
            javax.xml.XMLConstants.FEATURE_SECURE_PROCESSING;

    /**
     * Look up the value of a feature.
     * <p/>
     * <p>The feature name is any absolute URI.</p>
     *
     * @param name The feature name, which is an absolute URI.
     * @return The current state of the feature (true or false).
     */

    public boolean getFeature(String name) {
        if (name.equals(SAXSource.FEATURE)) return true;
        if (name.equals(SAXResult.FEATURE)) return true;
        if (name.equals(DOMSource.FEATURE)) return true;
        if (name.equals(DOMResult.FEATURE)) return true;
        if (name.equals(StreamSource.FEATURE)) return true;
        if (name.equals(StreamResult.FEATURE)) return true;
        if (name.equals(SAXTransformerFactory.FEATURE)) return true;
        if (name.equals(SAXTransformerFactory.FEATURE_XMLFILTER)) return true;
        if (name.equals(FEATURE_SECURE_PROCESSING)) {
            return !getConfiguration().getBooleanProperty(FeatureKeys.ALLOW_EXTERNAL_FUNCTIONS);
        }
        try {
            Object val = getConfiguration().getConfigurationProperty(name);
            return (val instanceof Boolean && (Boolean) val);
        } catch (IllegalArgumentException err) {
            return false;
        }
    }

    /**
     * Allows the user to set specific attributes on the underlying
     * implementation.  An attribute in this context is defined to
     * be an option that the implementation provides.
     *
     * @param name  The name of the attribute. This must be one of the constants
     *              defined in class {@link net.sf.saxon.lib.FeatureKeys}.
     * @param value The value of the attribute.
     * @throws IllegalArgumentException thrown if Saxon
     *                                  doesn't recognize the attribute.
     * @see net.sf.saxon.lib.FeatureKeys
     */

    public void setAttribute(String name, Object value) throws IllegalArgumentException {
        if (name.equals(FeatureKeys.CONFIGURATION_FILE)) {
            ConfigurationReader reader = new ConfigurationReader();
            try {
                setConfiguration(reader.makeConfiguration(new StreamSource(new File((String) (value)))));
            } catch (XPathException err) {
                throw new IllegalArgumentException(err);
            }
        } else {
            processor.setConfigurationProperty(name, value);
        }
    }

    /**
     * Allows the user to retrieve specific attributes on the underlying
     * implementation.
     *
     * @param name The name of the attribute. This must be one of the constants
     *             defined in class {@link net.sf.saxon.lib.FeatureKeys}.
     * @return value The value of the attribute.
     * @throws IllegalArgumentException thrown if the underlying
     *                                  implementation doesn't recognize the attribute.
     */
    /*@Nullable*/
    public Object getAttribute(String name) throws IllegalArgumentException {
        return getConfiguration().getConfigurationProperty(name);
    }

    /**
     * Set the error event listener for the TransformerFactory, which
     * is used for the processing of transformation instructions,
     * and not for the transformation itself.
     * <p/>
     * <p>This method is defined in JAXP but its use with Saxon is deprecated,
     * because the errorListener will be shared by all stylesheet compilations
     * running under this factory, which may be operating concurrently
     * in different threads.</p>
     *
     * @param listener The new error listener.
     * @throws IllegalArgumentException if listener is null.
     */

    public void setErrorListener(ErrorListener listener)
            throws IllegalArgumentException {
        getConfiguration().setErrorListener(listener);
    }

    /**
     * Get the error event handler for the TransformerFactory.
     *
     * @return The current error listener, which should never be null.
     */
    public ErrorListener getErrorListener() {
        UnfailingErrorListener uel = getConfiguration().getErrorListener();
        if (uel instanceof DelegatingErrorListener) {
            return ((DelegatingErrorListener) uel).getBaseErrorListener();
        } else {
            return uel;
        }
    }


    ///////////////////////////////////////////////////////////////////////////////
    // Methods defined in class javax.xml.transform.sax.SAXTransformerFactory
    ///////////////////////////////////////////////////////////////////////////////

    /**
     * Get a TransformerHandler object that can process SAX
     * ContentHandler events into a Result, based on the transformation
     * instructions specified by the argument.
     *
     * @param src The Source of the transformation instructions.
     * @return TransformerHandler ready to transform SAX events.
     * @throws javax.xml.transform.TransformerConfigurationException
     *          If for some reason the
     *          TransformerHandler can not be created.
     */

    public TransformerHandler newTransformerHandler(Source src)
            throws TransformerConfigurationException {
        Templates tmpl = newTemplates(src);
        return newTransformerHandler(tmpl);
    }

    /**
     * Get a TransformerHandler object that can process SAX
     * ContentHandler events into a Result, based on the Templates argument.
     *
     * @param templates The compiled transformation instructions.
     * @return TransformerHandler ready to transform SAX events.
     * @throws javax.xml.transform.TransformerConfigurationException
     *          If for some reason the
     *          TransformerHandler can not be created.
     */

    public TransformerHandler newTransformerHandler(Templates templates)
            throws TransformerConfigurationException {
        if (!(templates instanceof TemplatesImpl)) {
            throw new TransformerConfigurationException("Templates object was not created by Saxon");
        }
        TransformerImpl transformer = (TransformerImpl) templates.newTransformer();
        return transformer.newTransformerHandler();
    }

    /**
     * Get a TransformerHandler object that can process SAX
     * ContentHandler events into a Result. The transformation
     * is defined as an identity (or copy) transformation, for example
     * to copy a series of SAX parse events into a DOM tree.
     *
     * @return A non-null reference to a TransformerHandler, that may
     *         be used as a ContentHandler for SAX parse events.
     * @throws javax.xml.transform.TransformerConfigurationException
     *          If for some reason the
     *          TransformerHandler cannot be created.
     */

    public TransformerHandler newTransformerHandler()
            throws TransformerConfigurationException {
        IdentityTransformer transformer = new IdentityTransformer(getConfiguration());
        return new IdentityTransformerHandler(transformer);
    }

    /**
     * Get a TemplatesHandler object that can process SAX
     * ContentHandler events into a Templates object.
     *
     * @return A non-null reference to a TransformerHandler, that may
     *         be used as a ContentHandler for SAX parse events.
     * @throws javax.xml.transform.TransformerConfigurationException
     *          If for some reason the
     *          TemplatesHandler cannot be created.
     */

    public TemplatesHandler newTemplatesHandler()
            throws TransformerConfigurationException {
        return new TemplatesHandlerImpl(processor);
    }

    /**
     * Create an XMLFilter that uses the given Source as the
     * transformation instructions.
     *
     * @param src The Source of the transformation instructions.
     * @return An XMLFilter object, or null if this feature is not supported.
     * @throws javax.xml.transform.TransformerConfigurationException
     *          If for some reason the
     *          XMLFilter cannot be created.
     */

    public XMLFilter newXMLFilter(Source src)
            throws TransformerConfigurationException {
        Templates tmpl = newTemplates(src);
        return newXMLFilter(tmpl);
    }

    /**
     * Create an XMLFilter, based on the Templates argument..
     *
     * @param templates The compiled transformation instructions.
     * @return An XMLFilter object, or null if this feature is not supported.
     * @throws javax.xml.transform.TransformerConfigurationException
     *          If for some reason the
     *          XMLFilter cannot be created.
     */

    public XMLFilter newXMLFilter(Templates templates)
            throws TransformerConfigurationException {
        if (!(templates instanceof TemplatesImpl)) {
            throw new TransformerConfigurationException("Supplied Templates object was not created using Saxon");
        }
        TransformerImpl transformer = (TransformerImpl) templates.newTransformer();
        return transformer.newXMLFilter();
    }

    /**
     * <p>Set a feature for this <code>TransformerFactory</code> and <code>Transformer</code>s
     * or <code>Template</code>s created by this factory.</p>
     * <p/>
     * <p/>
     * Feature names are fully qualified {@link java.net.URI}s.
     * Implementations may define their own features.
     * An {@link javax.xml.transform.TransformerConfigurationException} is thrown if this <code>TransformerFactory</code> or the
     * <code>Transformer</code>s or <code>Template</code>s it creates cannot support the feature.
     * It is possible for an <code>TransformerFactory</code> to expose a feature value but be unable to change its state.
     * </p>
     * <p/>
     * <p>All implementations are required to support the FEATURE_SECURE_PROCESSING feature.
     * When the feature is:</p>
     * <ul>
     * <li>
     * <code>true</code>: the implementation will limit XML processing to conform to implementation limits
     * and behave in a secure fashion as defined by the implementation.
     * Examples include resolving user defined style sheets and functions.
     * If XML processing is limited for security reasons, it will be reported via a call to the registered
     * {@link javax.xml.transform.ErrorListener#fatalError(javax.xml.transform.TransformerException exception)}.
     * See {@link  #setErrorListener(javax.xml.transform.ErrorListener listener)}. In the Saxon implementation,
     * this option causes calls on extension functions and extensions instructions to be disabled, and also
     * disables the use of xsl:result-document to write to secondary output destinations.
     * </li>
     * <li>
     * <code>false</code>: the implementation will processing XML according to the XML specifications without
     * regard to possible implementation limits.
     * </li>
     * </ul>
     *
     * @param name  Feature name.
     * @param value Is feature state <code>true</code> or <code>false</code>.
     * @throws javax.xml.transform.TransformerConfigurationException
     *                              if this <code>TransformerFactory</code>
     *                              or the <code>Transformer</code>s or <code>Template</code>s it creates cannot support this feature.
     * @throws NullPointerException If the <code>name</code> parameter is null.
     */

    public void setFeature(String name, boolean value) throws TransformerConfigurationException {
        if (name.equals(FEATURE_SECURE_PROCESSING)) {
            getConfiguration().setBooleanProperty(FeatureKeys.ALLOW_EXTERNAL_FUNCTIONS, !value);
        } else {
            try {
                getConfiguration().setBooleanProperty(name, value);
            } catch (IllegalArgumentException err) {
                throw new TransformerConfigurationException("Unsupported TransformerFactory feature: " + name);
            }
        }
    }

}

