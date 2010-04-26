package net.sf.saxon.s9api;

import net.sf.saxon.Configuration;
import net.sf.saxon.Controller;
import net.sf.saxon.event.Receiver;
import net.sf.saxon.event.TransformerReceiver;
import net.sf.saxon.om.NodeInfo;
import net.sf.saxon.om.ValueRepresentation;
import net.sf.saxon.trace.TraceListener;
import net.sf.saxon.trans.XPathException;

import javax.xml.transform.Source;
import javax.xml.transform.TransformerException;
import javax.xml.transform.ErrorListener;
import javax.xml.transform.dom.DOMSource;

/**
 * An <code>XsltTransformer</code> represents a compiled and loaded stylesheet ready for execution.
 * The <code>XsltTransformer</code> holds details of the dynamic evaluation context for the stylesheet.
 *
 * <p>An <code>XsltTransformer</code> must not be used concurrently in multiple threads.
 * It is safe, however, to reuse the object within a single thread to run the same
 * stylesheet several times. Running the stylesheet does not change the context
 * that has been established.</p>
 *
 * <p>An <code>XsltTransformer</code> is always constructed by running the <code>Load</code>
 * method of an {@link XsltExecutable}.</p>
 *
 * <p>An <code>XsltTransformer</code> is itself a <code>Destination</code>. This means it is possible to use
 * one <code>XsltTransformer</code> as the destination to receive the results of another transformation,
 * this providing a simple way for transformations to be chained into a pipeline.</p>
 * @since 9.0
 */
public class XsltTransformer implements Destination {

    private Processor processor;
    private Controller controller;
    private NodeInfo initialContextNode;
    private Destination destination;

    /**
     * Protected constructor
     * @param processor the S9API processor
     * @param controller the Saxon controller object
     */

    protected XsltTransformer(Processor processor, Controller controller) {
        this.processor = processor;
        this.controller = controller;
    }

    /**
     * Set the initial named template for the transformation
     * @param templateName the name of the initial template, or null to indicate
     * that there should be no initial named template
     * @throws SaxonApiException if there is no named template with this name
     */

    public void setInitialTemplate(QName templateName) throws SaxonApiException {
        try {
            controller.setInitialTemplate(
                    templateName == null ? null : templateName.getClarkName());
        } catch (XPathException e) {
            throw new SaxonApiException(e);
        }
    }

    /**
     * Get the initial named template for the transformation, if one has been set
     * @return the name of the initial template, or null if none has been set
     */

    public QName getInitialTemplate() {
        String template = controller.getInitialTemplate();
        return (template == null ? null : QName.fromClarkName(template));
    }

    /**
     * Set the initial mode for the transformation
     * @param modeName the name of the initial mode, or null to indicate the default
     * (unnamed) mode
     */

    public void setInitialMode(QName modeName) {
        controller.setInitialMode(modeName == null ? null : modeName.getClarkName());
    }

    /**
     * Get the initial mode for the transformation, if one has been set.
     * @return the initial mode for the transformation. Returns null if no mode has been set,
     * or if the mode was set to null to represent the default (unnamed) mode
     */

    public QName getInitialMode() {
        String mode = controller.getInitialMode();
        if (mode == null) {
            return null;
        } else {
            return QName.fromClarkName(mode);
        }
    }

    /**
     * Set the schema validation mode for the transformation. This indicates how source documents
     * loaded specifically for this transformation will be handled. This applies to the
     * principal source document if supplied as a SAXSource or StreamSource, and to all
     * documents loaded during the transformation using the <code>doc()</code>, <code>document()</code>,
     * or <code>collection()</code> functions.
     * @param mode the validation mode. Passing null causes no change to the existing value.
     * Passing <code>Validation.DEFAULT</code> resets to the initial value, which determines
     * the validation requirements from the Saxon Configuration.
     */

    public void setSchemaValidationMode(ValidationMode mode) {
        if (mode != null) {
            controller.setSchemaValidationMode(mode.getNumber());
        }
    }

    /**
     * Get the schema validation mode for the transformation. This indicates how source documents
     * loaded specifically for this transformation will be handled. This applies to the
     * principal source document if supplied as a SAXSource or StreamSource, and to all
     * documents loaded during the transformation using the <code>doc()</code>, <code>document()</code>,
     * or <code>collection()</code> functions.
     * @return the validation mode.
     */

    public ValidationMode getSchemaValidationMode() {
        return ValidationMode.get(controller.getSchemaValidationMode());
    }
    
    /**
     * Set the source document for the transformation.
     *
     * <p>If the source is an instance of {@link net.sf.saxon.om.NodeInfo}, the supplied node is used
     * directly as the context node of the query.</p>
     *
     * <p>If the source is an instance of {@link javax.xml.transform.dom.DOMSource}, the DOM node identified
     * by the DOMSource is wrapped as a Saxon node, and this is then used as the context item</p>
     *
     * <p>In all other cases a new Saxon tree is built, by calling
     * {@link net.sf.saxon.s9api.DocumentBuilder#build(javax.xml.transform.Source)}, and the document
     * node of this tree is then used as the context item for the query.</p>
     *
     * @param source the principal source document for the transformation
     */

    public void setSource(Source source) throws SaxonApiException {
        if (source instanceof NodeInfo) {
            setInitialContextNode(new XdmNode((NodeInfo)source));
        } else if (source instanceof DOMSource) {
            setInitialContextNode(processor.newDocumentBuilder().wrap(source));
        } else {
            setInitialContextNode(processor.newDocumentBuilder().build(source));
        }
    }

    /**
     * Set the initial context node for the transformation.
     * <p>This is ignored in the case where the {@link XsltTransformer} is used as the
     * {@link Destination} of another process. In that case the initial context node will always
     * be the document node of the document that is being streamed to this destination.</p>
     * @param node the initial context node, or null if there is to be no initial context node
     */

    public void setInitialContextNode(XdmNode node) {
        initialContextNode = (node == null ? null : node.getUnderlyingNode());
    }

    /**
     * Get the initial context node for the transformation, if one has been set
     * @return the initial context node, or null if none has been set. This will not necessarily
     * be the same {@link XdmNode} instance as was supplied, but it will be an XdmNode object that represents
     * the same underlying node.
     */

    public XdmNode getInitialContextNode() {
        return (XdmNode)XdmValue.wrap(initialContextNode);
    }

    /**
     * Set
     */

    /**
     * Set the value of a stylesheet parameter
     * @param name the name of the stylesheet parameter, as a QName
     * @param value the value of the stylesheet parameter, or null to clear a previously set value
     */

    public void setParameter(QName name, XdmValue value) {
        controller.setParameter(name.getStructuredQName(),
                (value == null ? null : value.getUnderlyingValue()));
    }

    /**
     * Get the value that has been set for a stylesheet parameter
     * @param name the parameter whose name is required
     * @return the value that has been set for the parameter, or null if no value has been set
     */

    public XdmValue getParameter(QName name) {
        Object oval = controller.getParameter(name.getClarkName());
        if (oval == null) {
            return null;
        }
        if (oval instanceof ValueRepresentation) {
            return XdmValue.wrap((ValueRepresentation)oval);
        }
        throw new IllegalStateException(oval.getClass().getName());
    }

    /**
     * Set the destination to be used for the transformation.
     * <p>This method can be used to chain transformations into a pipeline, by using one
     * {@link XsltTransformer} as the destination of another</p>
     * @param destination the destination to be used
     */

    public void setDestination(Destination destination) {
        this.destination = destination;
    }

    /**
     * Get the destination that was specified in a previous call of {@link #setDestination}
     * @return the destination, or null if none has been supplied
     */

    public Destination getDestination() {
        return destination;
    }

    /**
     * Set the base output URI.
     *
     * <p>This defaults to the system ID of the Destination for the principal output
     * of the transformation if this is known; if it is not known, it defaults
     * to the current directory.</p>
     *
     * <p> The base output URI is used for resolving relative URIs in the <code>href</code> attribute
     * of the <code>xsl:result-document</code> instruction.</p>
     *
     * @param uri the base output URI
     * @since 9.2
     */

    public void setBaseOutputURI(String uri) {
        controller.setBaseOutputURI(uri);
    }

    /**
     * Get the base output URI.
     *
     * <p>This returns the value set using the {@link #setBaseOutputURI} method. If no value has been set
     * explicitly, then the method returns null if called before the transformation, or the computed
     * default base output URI if called after the transformation.</p>
     *
     * <p> The base output URI is used for resolving relative URIs in the <code>href</code> attribute
     * of the <code>xsl:result-document</code> instruction.</p>
     *
     * @return the base output URI
     * @since 9.2
     */

    public String getBaseOutputURI() {
        return controller.getBaseOutputURI();
    }



    /**
     * Set the ErrorListener to be used during this transformation
     * @param listener The error listener to be used. This is notified of all dynamic errors detected during the
     * transformation.
     * @since 9.2
     */

    public void setErrorListener(ErrorListener listener) {
        controller.setErrorListener(listener);
    }

    /**
     * Get the ErrorListener being used during this compilation episode
     * @return listener The error listener in use. This is notified of all dynamic errors detected during the
     * transformation. If no user-supplied ErrorListener has been set the method will return a system-supplied
     * ErrorListener.
     * @since 9.2
     */

    public ErrorListener getErrorListener() {
        return controller.getErrorListener();
    }

    /**
     * Set the MessageListener to be notified whenever the stylesheet evaluates an
     * <code>xsl:message</code> instruction.  If no MessageListener is nominated,
     * the output of <code>xsl:message</code> instructions will be serialized and sent
     * to the standard error stream.
     * @param listener the MessageListener to be used
     * @since 9.1
     */

    public void setMessageListener(MessageListener listener) {
        controller.setMessageEmitter(new MessageListenerProxy(listener));
    }

    /**
     * Get the MessageListener to be notified whenever the stylesheet evaluates an
     * <code>xsl:message</code> instruction. If no MessageListener has been nominated,
     * return null
     * @return the user-supplied MessageListener, or null if none has been supplied
     * @since 9.1
     */

    public MessageListener getMessageListener() {
        Receiver r = controller.getMessageEmitter();
        if (r instanceof MessageListenerProxy) {
            return ((MessageListenerProxy)r).getMessageListener();
        } else {
            return null;
        }
    }

    /**
     * Set a TraceListener to be notified of all events occurring during the transformation.
     * This will only be effective if the stylesheet was compiled with trace code enabled
     * (see {@link XsltCompiler#setCompileWithTracing(boolean)})
     * @param listener the TraceListener to be used. Note that the TraceListener has access to
     * interal Saxon interfaces which may vary from one release to the next. It is also possible that
     * the TraceListener interface itself may be changed in future releases.
     * @since 9.2
     */

    public void setTraceListener(TraceListener listener) {
        controller.setTraceListener(listener);
    }

    /**
     * Get the TraceListener to be notified of all events occurring during the transformation.
     * If no TraceListener has been nominated, return null
     * @return the user-supplied TraceListener, or null if none has been supplied
     * @since 9.2
     */

    public TraceListener getTraceListener() {
        return controller.getTraceListener();
    }

    /**
     * Perform the transformation. If this method is used, a destination must have been supplied
     * previously
     * @throws SaxonApiException if any dynamic error occurs during the transformation
     * @throws IllegalStateException if no destination has been supplied
     */

    public void transform() throws SaxonApiException {
        if (destination == null) {
            throw new IllegalStateException("No destination has been supplied");
        }
        try {
            Receiver receiver;
            if (destination instanceof Serializer) {
                receiver = ((Serializer)destination).getReceiver(
                        controller.getConfiguration(), controller, controller.getOutputProperties());
            } else {
                receiver = destination.getReceiver(controller.getConfiguration());
            }
            controller.transform(initialContextNode, receiver);
            if (destination instanceof Serializer) {
                ((Serializer)destination).close();
            }
        } catch (TransformerException e) {
            throw new SaxonApiException(e);
        }
    }

   /**
     * Return a Receiver which can be used to supply the principal source document for the transformation.
     * This method is intended primarily for internal use, though it can also
     * be called by a user application that wishes to feed events into the transformation engine.
     *
     * <p>Saxon calls this method to obtain a Receiver, to which it then sends
     * a sequence of events representing the content of an XML document. This method is provided so that
     * <code>XsltTransformer</code> implements <code>Destination</code>, allowing one transformation
     * to receive the results of another in a pipeline.</p>
     *
     * <p>Before calling this method, the {@link #setDestination} method must be called to supply a destination
     * for the transformation.</p>
     *
     * <p>Note that when an <code>XsltTransformer</code> is used as a <code>Destination</code>, the initial
     * context node set on that <code>XsltTransformer</code> is ignored.</p>
     *
     * @param config The Saxon configuration. This is supplied so that the destination can
     *               use information from the configuration (for example, a reference to the name pool)
     *               to construct or configure the returned Receiver.
     * @return the Receiver to which events are to be sent.
     * @throws SaxonApiException if the Receiver cannot be created
     * @throws IllegalStateException if no Destination has been supplied
     */

    public Receiver getReceiver(Configuration config) throws SaxonApiException {
        if (destination == null) {
            throw new IllegalStateException("No destination has been supplied");
        }
        TransformerReceiver tr = new TransformerReceiver(controller);
        tr.setResult(destination.getReceiver(config));
        tr.setPipelineConfiguration(controller.makePipelineConfiguration());
        return tr;
    }

    /**
     * Get the underlying Controller used to implement this XsltTransformer. This provides access
     * to lower-level methods not otherwise available in the s9api interface. Note that classes
     * and methods obtained by this route cannot be guaranteed stable from release to release.
     * @since 9.0.0.4
     */

    public Controller getUnderlyingController() {
        return controller;
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

