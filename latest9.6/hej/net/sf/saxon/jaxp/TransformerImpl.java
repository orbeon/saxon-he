////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
// Copyright (c) 2014 Saxonica Limited.
// This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
// If a copy of the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
// This Source Code Form is "Incompatible With Secondary Licenses", as defined by the Mozilla Public License, v. 2.0.
////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

package net.sf.saxon.jaxp;

import net.sf.saxon.Configuration;
import net.sf.saxon.Controller;
import net.sf.saxon.event.Receiver;
import net.sf.saxon.expr.JPConverter;
import net.sf.saxon.expr.XPathContext;
import net.sf.saxon.expr.parser.RoleLocator;
import net.sf.saxon.lib.SerializerFactory;
import net.sf.saxon.om.Sequence;
import net.sf.saxon.s9api.*;
import net.sf.saxon.trans.XPathException;
import net.sf.saxon.type.JavaExternalObjectType;
import net.sf.saxon.value.EmptySequence;
import net.sf.saxon.value.ObjectValue;
import net.sf.saxon.value.UntypedAtomicValue;
import org.w3c.dom.Node;
import org.xml.sax.XMLFilter;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.ErrorListener;
import javax.xml.transform.Result;
import javax.xml.transform.Source;
import javax.xml.transform.URIResolver;
import javax.xml.transform.dom.DOMResult;
import javax.xml.transform.sax.SAXResult;
import javax.xml.transform.sax.TransformerHandler;
import javax.xml.transform.stream.StreamResult;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

/**
 * Saxon implementation of the JAXP Transformer interface.
 * <p/>
 * <p>Since Saxon 9.6, JAXP interfaces are implemented as a layer above the s9api interface</p>
 */
public class TransformerImpl extends IdentityTransformer {

    private XsltExecutable xsltExecutable;
    private XsltTransformer xsltTransformer;
    private Map<String, Object> parameters = new HashMap<String, Object>(8);

    protected TransformerImpl(XsltExecutable e, XsltTransformer t) {
        super(e.getProcessor().getUnderlyingConfiguration());
        this.xsltExecutable = e;
        this.xsltTransformer = t;
    }

    /**
     * <p>Transform the XML <code>Source</code> to a <code>Result</code>.
     * Specific transformation behavior is determined by the settings of the
     * <code>TransformerFactory</code> in effect when the
     * <code>Transformer</code> was instantiated and any modifications made to
     * the <code>Transformer</code> instance.</p>
     * <p/>
     * <p>An empty <code>Source</code> is represented as an empty document
     * as constructed by {@link javax.xml.parsers.DocumentBuilder#newDocument()}.
     * The result of transforming an empty <code>Source</code> depends on
     * the transformation behavior; it is not always an empty
     * <code>Result</code>.</p>
     *
     * @param xmlSource    The XML input to transform.
     * @param outputTarget The <code>Result</code> of transforming the
     *                     <code>xmlSource</code>.
     * @throws javax.xml.transform.TransformerException
     *          If an unrecoverable error occurs
     *          during the course of the transformation.
     */
    @Override
    public void transform(Source xmlSource, final Result outputTarget) throws XPathException {
        try {
            xsltTransformer.setSource(xmlSource);
            if (outputTarget.getSystemId() != null) { //bug 2214
                xsltTransformer.setBaseOutputURI(outputTarget.getSystemId());
            }
            Destination destination;

            if (outputTarget instanceof StreamResult)
            {
                StreamResult sr = (StreamResult) outputTarget;
                if (sr.getOutputStream() != null) {
                    destination = xsltExecutable.getProcessor().newSerializer(sr.getOutputStream());
                } else if (sr.getWriter() != null) {
                    destination = xsltExecutable.getProcessor().newSerializer(sr.getWriter());
                } else if (sr.getSystemId() != null) {
                    URI uri;
                    try {
                        uri = new URI(sr.getSystemId());
                    } catch (URISyntaxException e) {
                        throw new XPathException("System ID in Result object is not a valid URI: " + sr.getSystemId(), e);
                    }
                    if (!uri.isAbsolute()) {
                        try {
                            uri = new File(sr.getSystemId()).getAbsoluteFile().toURI();
                        } catch (Exception e) {
                            // if we fail, we'll get another exception
                        }
                    }
                    File file = new File(uri);
                    try {
                        if ("file".equals(uri.getScheme()) && !file.exists()) {
                            File directory = file.getParentFile();
                            if (directory != null && !directory.exists()) {
                                directory.mkdirs();
                            }
                            file.createNewFile();
                        }
                    } catch (IOException err) {
                        throw new XPathException("Failed to create output file " + uri, err);
                    }
                    FileOutputStream stream;
                    try {
                        stream = new FileOutputStream(file);
                    } catch (FileNotFoundException e) {
                        throw new XPathException("Failed to create output file", e);
                    }
                    destination = xsltExecutable.getProcessor().newSerializer(stream);
                } else {
                    throw new IllegalArgumentException("StreamResult supplies neither an OutputStream nor a Writer");
                }
                // Copy the local output properties to the Serializer
                Properties localOutputProperties = getLocalOutputProperties();
                for (String key : localOutputProperties.stringPropertyNames()) {
                    ((Serializer) destination).setOutputProperty(Serializer.getProperty(QName.fromClarkName(key)),
                            localOutputProperties.getProperty(key));
                }
            } else if (outputTarget instanceof SAXResult) {
                destination = new SAXDestination(((SAXResult) outputTarget).getHandler());
            } else if (outputTarget instanceof DOMResult) {
                Node root = ((DOMResult) outputTarget).getNode();
                if (root == null) {
                    try {
                        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
                        dbf.setNamespaceAware(true);
                        root = dbf.newDocumentBuilder().newDocument();
                        ((DOMResult) outputTarget).setNode(root);
                    } catch (ParserConfigurationException e) {
                        throw new XPathException(e);
                    }
                }
                destination = new DOMDestination(root);
            } else if (outputTarget instanceof Receiver) {
                destination = new Destination() {
                    public Receiver getReceiver(Configuration config) throws SaxonApiException {
                        return (Receiver) outputTarget;
                    }

                    public void close() throws SaxonApiException {
                        try {
                            ((Receiver) outputTarget).close();
                        } catch (XPathException e) {
                            throw new SaxonApiException(e);
                        }
                    }
                };
            } else {
                SerializerFactory sf = getConfiguration().getSerializerFactory();
                Receiver r = sf.getReceiver(outputTarget, getConfiguration().makePipelineConfiguration(), getLocalOutputProperties());
                transform(xmlSource, r);
                return;
                //throw new IllegalArgumentException("Unknown Result class " + outputTarget.getClass());
            }
            xsltTransformer.setDestination(destination);

            xsltTransformer.transform();
        } catch (SaxonApiException e) {
            throw XPathException.makeXPathException(e);
        }
    }

    /**
     * Add a parameter for the transformation.
     * <p/>
     * <p>Pass a qualified name as a two-part string, the namespace URI
     * enclosed in curly braces ({}), followed by the local name. If the
     * name has a null URL, the String only contain the local name. An
     * application can safely check for a non-null URI by testing to see if the
     * first character of the name is a '{' character.</p>
     * <p>For example, if a URI and local name were obtained from an element
     * defined with &lt;xyz:foo
     * xmlns:xyz="http://xyz.foo.com/yada/baz.html"/&gt;,
     * then the qualified name would be "{http://xyz.foo.com/yada/baz.html}foo".
     * Note that no prefix is used.</p>
     *
     * @param name  The name of the parameter, which may begin with a
     *              namespace URI in curly braces ({}).
     * @param value The value object.  This can be any valid Java object. It is
     *              up to the processor to provide the proper object coercion or to simply
     *              pass the object on for use in an extension.
     * @throws NullPointerException     If name is null.
     * @throws IllegalArgumentException If the supplied value cannot be converted to the declared
     *                                  type of the corresponding stylesheet parameter
     */
    @Override
    public void setParameter(String name, Object value) {
        if (name == null) {
            throw new NullPointerException("Transformer.setParameter() - name is null");
        }
        if (value == null) {
            throw new NullPointerException("Transformer.setParameter() - value is null");
        }
        parameters.put(name, value);
        QName qName = QName.fromClarkName(name);
        XsltExecutable.ParameterDetails details = xsltExecutable.getGlobalParameters().get(qName);
        if (details == null) {
            // no parameter with this name is defined; we can simply ignore it
            return;
        }
        Configuration config = getConfiguration();
        net.sf.saxon.value.SequenceType required = details.getUnderlyingDeclaredType();
        Sequence converted;
        try {
            if (value instanceof Sequence) {
                converted = (Sequence) value;
            } else if (value instanceof String) {
                converted = new UntypedAtomicValue((String) value);
            } else if (required.getPrimaryType() instanceof JavaExternalObjectType) {
                converted = new ObjectValue<Object>(value);
            } else {
                JPConverter converter = JPConverter.allocate(value.getClass(), null, config);
                XPathContext context = xsltTransformer.getUnderlyingController().newXPathContext();
                converted = converter.convert(value, context);
            }
            if (converted == null) {
                converted = EmptySequence.getInstance();
            }

            if (required != null && !required.matches(converted, config)) {
                RoleLocator role = new RoleLocator(RoleLocator.VARIABLE, qName.toString(), -1);
                converted = config.getTypeHierarchy().applyFunctionConversionRules(converted, required, role, null);
            }
        } catch (XPathException e) {
            throw new IllegalArgumentException(e);
        }

        xsltTransformer.setParameter(qName, XdmValue.wrap(converted));
    }

    /**
     * Get a parameter that was explicitly set with setParameter.
     * <p/>
     * <p>This method does not return a default parameter value, which
     * cannot be determined until the node context is evaluated during
     * the transformation process.
     *
     * @param name of <code>Object</code> to get
     * @return A parameter that has been set with setParameter, or null if none has been set.
     */
    @Override
    public Object getParameter(String name) {
        return parameters.get(name);
    }

    /**
     * Clear all parameters set with setParameter.
     */
    @Override
    public void clearParameters() {
        parameters.clear();
        xsltTransformer.clearParameters();
    }

    /**
     * Set an object that will be used to resolve URIs used in
     * document().
     * <p/>
     * <p>If the resolver argument is null, the URIResolver value will
     * be cleared and the transformer will no longer have a resolver.</p>
     *
     * @param resolver An object that implements the URIResolver interface,
     *                 or null.
     */
    @Override
    public void setURIResolver(URIResolver resolver) {
        super.setURIResolver(resolver);
        xsltTransformer.setURIResolver(resolver);
    }


    /**
     * Set the error event listener in effect for the transformation.
     *
     * @param listener The new error listener.
     * @throws IllegalArgumentException if listener is null.
     */
    @Override
    public void setErrorListener(ErrorListener listener) throws IllegalArgumentException {
        super.setErrorListener(listener);
        xsltTransformer.setErrorListener(listener);
    }

    /**
     * Supply an initial template for a transformation.
     * <p>This is a Saxon extension to the JAXP interface, needed for XSLT 2.0</p>
     *
     * @param name the name of the initial template, in Clark notation (either a local name,
     *             or "{uri}local")
     * @throws IllegalArgumentException if the argument is invalid, for example if the
     *                                  format of the name is incorrect or if there is no template with this name
     */

    public void setInitialTemplate(String name) throws IllegalArgumentException {
        try {
            xsltTransformer.setInitialTemplate(QName.fromClarkName(name));
        } catch (SaxonApiException e) {
            throw new IllegalArgumentException(e);
        }
    }

    /**
     * Supply an initial mode for a transformation.
     * <p>This is a Saxon extension to the JAXP interface, needed for XSLT 2.0</p>
     *
     * @param name the name of the initial mode, in Clark notation (either a local name,
     *             or "{uri}local")
     * @throws IllegalArgumentException if the argument is invalid, for example if the
     *                                  format of the name is incorrect or if there is no mode with this name
     */

    public void setInitialMode(String name) throws IllegalArgumentException {
        xsltTransformer.setInitialMode(QName.fromClarkName(name));
    }

    /**
     * Get the output properties defined in the unnamed xsl:output declaration(s) within
     * the stylesheet
     * @return the values of output properties set in the stylesheet
     */

    @Override
    protected Properties getStylesheetOutputProperties() {
        return xsltExecutable.getUnderlyingCompiledStylesheet().getDefaultOutputProperties();
    }

    /**
     * Get the underlying s9api implementation class wrapped by this JAXP Transformer
     * @return the underlying s9api XsltTransformer
     */

    public XsltTransformer getUnderlyingXsltTransformer() {
        return xsltTransformer;
    }

    /**
     * Get the underlying s9api implementation class representing the compled stylesheet
     * which this transformer is executing
     * @return the underlying s9api XsltExecutable
     */

    public XsltExecutable getUnderlyingXsltExecutable() {
        return xsltExecutable;
    }

    /**
     * Get the internal Saxon Controller instance that implements this transformation.
     * Note that the Controller interface will not necessarily remain stable in future releases
     * @return the underlying Saxon Controller instance
     */

    public Controller getUnderlyingController() {
        return xsltTransformer.getUnderlyingController();
    }

    /**
     * Create a JAXP TransformerHandler to perform the transformation
     * @return a JAXP TransformerHandler, which allows the transformation to be performed
     * in "push" mode on a SAX pipeline.
     */

    public TransformerHandler newTransformerHandler() {
        return new TransformerHandlerImpl(this);
    }

    /**
     * Create a JAXP XMLFilter which allows this transformation to be added to a SAX pipeline
     * @return the transformation in the form of an XMLFilter
     */

    public XMLFilter newXMLFilter() {
        return new FilterImpl(this);
    }
}

