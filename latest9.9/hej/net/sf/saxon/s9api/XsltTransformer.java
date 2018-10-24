////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
// Copyright (c) 2018 Saxonica Limited.
// This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
// If a copy of the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
// This Source Code Form is "Incompatible With Secondary Licenses", as defined by the Mozilla Public License, v. 2.0.
////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

package net.sf.saxon.s9api;

import net.sf.saxon.event.PipelineConfiguration;
import net.sf.saxon.event.Receiver;
import net.sf.saxon.event.SequenceNormalizerWithSpaceSeparator;
import net.sf.saxon.expr.instruct.GlobalContextRequirement;
import net.sf.saxon.expr.instruct.GlobalParameterSet;
import net.sf.saxon.om.Item;
import net.sf.saxon.om.NodeInfo;
import net.sf.saxon.om.Sequence;
import net.sf.saxon.serialize.SerializationProperties;
import net.sf.saxon.trans.XPathException;
import net.sf.saxon.trans.XsltController;
import net.sf.saxon.tree.linked.DocumentImpl;

import javax.xml.transform.Source;
import javax.xml.transform.TransformerException;
import javax.xml.transform.dom.DOMSource;
import java.net.URI;

/**
 * An {@code XsltTransformer} represents a compiled and loaded stylesheet ready for execution.
 * The {@code XsltTransformer} holds details of the dynamic evaluation context for the stylesheet.
 * <p>
 * An {@code XsltTransformer} must not be used concurrently in multiple threads.
 * It is safe, however, to reuse the object within a single thread to run the same
 * stylesheet several times. Running the stylesheet does not change the context
 * that has been established.
 * <p>
 * An {@code XsltTransformer} is always constructed by running the {@code Load}
 * method of an {@link XsltExecutable}.
 * <p>
 * An {@code XsltTransformer} is itself a {@link Destination}. This means it is possible to use
 * one {@code XsltTransformer} as the destination to receive the results of another transformation,
 * this providing a simple way for transformations to be chained into a pipeline. Note however that a
 * when the input to a transformation is supplied in this way, it will always be built as a tree in
 * memory, rather than the transformation being streamed. As a {@code Destination}, the transformer
 * performs <i>Sequence Normalization</i> on its input; that is, it converts the input to a single
 * document node. (The main reason for this is that when chaining XSLT transformations, the raw
 * output of the first stylesheet is often an element node, but the second stylesheet traditionally
 * expects a document node.)
 *
 * @since 9.0
 */
public class XsltTransformer extends AbstractXsltTransformer implements Destination {

    // TODO: when input is piped into an XsltTransformer, do a streamed transformation where appropriate.

    private QName initialTemplateName;
    private GlobalParameterSet parameters;
    /*@Nullable*/ private Source initialSource;
    private Destination destination;
    private Receiver sourceTreeBuilder;
    private DestinationHelper destinationHelper = new DestinationHelper(this);
    private URI destinationBaseUri;

    /**
     * Protected constructor
     *
     * @param processor  the S9API processor
     * @param controller the Saxon controller object
     * @param staticParameters the static parameters supplied at stylesheet compile time
     */

    protected XsltTransformer(Processor processor, XsltController controller, GlobalParameterSet staticParameters) {
        super(processor, controller);
        parameters = new GlobalParameterSet(/*staticParameters*/);
    }

    /**
     * Set the base URI of the resource being written to this destination
     *
     * @param baseURI the base URI to be used
     */

    public void setDestinationBaseURI(URI baseURI) {
        this.destinationBaseUri = baseURI;
    }

    /**
     * Get the base URI of the resource being written to this destination
     *
     * @return the baseURI, or null if none is known
     */

    public URI getDestinationBaseURI() {
        return destinationBaseUri;
    }

    @Override
    public void onClose(Action listener) {
        destinationHelper.onClose(listener);
    }

    @Override
    public void closeAndNotify() throws SaxonApiException {
        destinationHelper.closeAndNotify();
    }

    /**
     * Set the initial named template for the transformation
     *
     * @param templateName the name of the initial template, or null to indicate
     *                     that there should be no initial named template. Changed
     *                     in 9.9: the method no longer checks that the named
     *                     template exists.
     */

    public void setInitialTemplate(QName templateName) {
        initialTemplateName = templateName;
    }

    /**
     * Get the initial named template for the transformation, if one has been set
     *
     * @return the name of the initial template, or null if none has been set
     */

    public QName getInitialTemplate() {
        return initialTemplateName;
    }


    /**
     * Set the source document for the transformation.
     * <p>If the source is an instance of {@link net.sf.saxon.om.NodeInfo}, the supplied node is used
     * directly as the initial context item of the transformation.</p>
     * <p>If the source is an instance of {@link javax.xml.transform.dom.DOMSource}, the DOM node identified
     * by the DOMSource is wrapped as a Saxon node, and this is then used as the context item.</p>
     * <p>In other cases a new Saxon tree will be built by the transformation engine when the
     * transformation starts, unless it is a Saxon-EE streaming transformation, in which case the
     * document is processed in streaming fashion as it is being parsed.</p>
     * <p>To run a transformation in streaming mode, the source should be supplied as an instance
     * of {@link javax.xml.transform.stream.StreamSource} or {@link javax.xml.transform.sax.SAXSource}.
     * </p>
     *
     * @param source the principal source document for the transformation
     */

    public void setSource(Source source) {
        if (source instanceof NodeInfo) {
            setInitialContextNode(new XdmNode((NodeInfo) source));
        } else if (source instanceof DOMSource) {
            if (((DOMSource)source).getNode() == null) {
                DocumentImpl doc = new DocumentImpl();
                doc.setConfiguration(controller.getConfiguration());
                setInitialContextNode(new XdmNode(doc));
            } else {
                NodeInfo n = processor.getUnderlyingConfiguration().unravel(source);
                setInitialContextNode(new XdmNode(n));
            }
        } else {
            initialSource = source;
        }
    }

    /**
     * Set the initial context node for the transformation.
     * <p>In XSLT 3.0 terms, this sets the initial match selection (the sequence to which the
     * initial implicit call of xsl:applyTemplates is applied). It also determines how the
     * global context item for evaluating global variables is set: following the XSLT 1.0 and 2.0 rules
     * (XSLT 2.0 section 9.5): "For a global variable or the default value of a stylesheet parameter,
     * the expression or sequence constructor specifying the variable value is evaluated with a singleton
     * focus based on the root node of the tree containing the initial context node." </p>
     * <p>This value is ignored in the case where the {@link XsltTransformer} is used as the
     * {@link Destination} of another process. In that case the initial context node will always
     * be the document node of the document that is being streamed to this destination.</p>
     * <p>Calling this method has the side-effect of setting the initial source to null.</p>
     *
     * @param node the initial context node, or null if there is to be no initial context node
     * @throws SaxonApiUncheckedException if the node is unsuitable, for example if it was
     * built using the wrong Configuration
     */

    public void setInitialContextNode(XdmNode node) throws SaxonApiUncheckedException {
        try {
            if (node == null) {
                initialSource = null;
                controller.setGlobalContextItem(null);
            } else {
                initialSource = node.getUnderlyingNode();
                controller.setGlobalContextItem(node.getUnderlyingNode().getRoot());
            }
        } catch (XPathException e) {
            throw new SaxonApiUncheckedException(e);
        }
    }

    /**
     * Get the initial context node for the transformation, if one has been set
     *
     * @return the initial context node, or null if none has been set. This will not necessarily
     *         be the same {@link XdmNode} instance as was supplied, but it will be an XdmNode object that represents
     *         the same underlying node.
     */

    public XdmNode getInitialContextNode() {
        if (initialSource instanceof NodeInfo) {
            return (XdmNode) XdmValue.wrap((NodeInfo) initialSource);
        } else {
            return null;
        }
    }

    /**
     * Set the value of a stylesheet parameter.
     *
     * <p>If the stylesheet does not have a parameter with this name, then the supplied value will
     * simply be ignored (no error occurs)</p>
     *
     * <p>If the stylesheet has a parameter with this name, and the supplied value does not match the
     * required type, then no error will be reported at this stage, but a dynamic error will occur
     * when the parameter value is first used. Supplied values are converted to the required type
     * using the function conversion rules.</p>
     *
     * <p>If the stylesheet has a parameter with this name, and the parameter is declared
     * with <code>static="yes"</code>, or if a parameter with the same name was supplied to the
     * {@link XsltCompiler}, then no error will be reported at this stage, but an error
     * will be reported when the transformation is initiated. Static parameters must be initialized
     * using {@link XsltCompiler#setParameter(QName, XdmValue)}.</p>
     *
     * @param name  the name of the stylesheet parameter, as a QName
     * @param value the value of the stylesheet parameter, or null to clear a previously set value
     * @throws SaxonApiUncheckedException if the value is lazily evaluated, and evaluation fails
     */

    public void setParameter(QName name, XdmValue value) {
        try {
            parameters.put(name.getStructuredQName(),
                    value == null ? null : ((Sequence<? extends Item>) value.getUnderlyingValue()).materialize());
        } catch (XPathException e) {
            throw new SaxonApiUncheckedException(e);
        }
    }

    /**
     * Clear the values of all parameters that have been set
     */

    public void clearParameters() {
        parameters = new GlobalParameterSet();
    }

    /**
     * Get the value that has been set for a stylesheet parameter
     *
     * @param name the parameter whose name is required
     * @return the value that has been set for the parameter, or null if no value has been set
     */

    public XdmValue getParameter(QName name) {
        Sequence oval = parameters.get(name.getStructuredQName());
        return oval == null ? null : XdmValue.wrap(oval);
    }

    /**
     * Set the destination to be used for the result of the transformation.
     * <p>
     * This method can be used to chain transformations into a pipeline, by using one
     * {@link XsltTransformer} as the destination of another
     * <p>
     * If the destination is a {@link Serializer}, then this call has two side-effects:
     * <p><ul>
     * <li>It sets the base output URI for the transformation. This acts as the base URI for resolving
     * the {@code href} attribute of any {@code xsl:result-document} instruction.</li>
     *
     * <li>It modifies the supplied Serializer to make it aware of the serialization properties
     * defined in the default xsl:output declaration of the stylesheet.
     * The serialization parameters defined in the {@code Serializer} override any
     * serialization parameters defined using {@code xsl:output} for the principal
     * output of the transformation. However, they have no effect on any output produced
     * using {@code xsl:result-document}.</li>
     * </ul>
     *
     * @param destination the destination to be used for the result of this transformation
     */

    public void setDestination(Destination destination) {
        this.destination = destination;
//        controller.setNewReceiverFactory(
//                props -> destination.getReceiver(controller.makePipelineConfiguration(), props));
    }

    /**
     * Get the destination that was specified in a previous call of {@link #setDestination}
     *
     * @return the destination, or null if none has been supplied
     */

    public Destination getDestination() {
        return destination;
    }

    /**
     * Perform the transformation. If this method is used, a destination must have been supplied
     * previously
     *
     * @throws SaxonApiException     if any dynamic error occurs during the transformation
     * @throws IllegalStateException if no destination has been supplied
     */

    public void transform() throws SaxonApiException {
        if (destination == null) {
            throw new IllegalStateException("No destination has been supplied");
        }

        try {
            Receiver out = getDestinationReceiver(controller, destination);
            GlobalContextRequirement gcr = controller.getExecutable().getGlobalContextRequirement();
            if ((gcr == null || !gcr.isAbsentFocus()) && initialSource != null) {
                if (initialSource instanceof NodeInfo) {
                    maybeSetGlobalContextItem((NodeInfo)initialSource);
                } else if (initialSource instanceof DOMSource) {
                    NodeInfo node = controller.prepareInputTree(initialSource);
                    maybeSetGlobalContextItem(node);
                    initialSource = node;
                } else {
                    NodeInfo node = controller.makeSourceTree(initialSource, getSchemaValidationMode().getNumber());
                    maybeSetGlobalContextItem(node);
                    initialSource = node;
                }
            }

            if (baseOutputUriWasSet) {
                out.setSystemId(getBaseOutputURI());
            }
            controller.initializeController(parameters);



            if (initialTemplateName != null) {
                controller.callTemplate(initialTemplateName.getStructuredQName(), out);
            } else {
                applyTemplatesToSource(initialSource, out);
            }
            destination.closeAndNotify();

        } catch (XPathException e) {
            if (!e.hasBeenReported()) {
                try {
                    getErrorListener().error(e);
                } catch (TransformerException e1) {
                    // no action
                }
            }
            throw new SaxonApiException(e);
        }
    }

    private void maybeSetGlobalContextItem(Item item) throws XPathException {
        if (controller.getGlobalContextItem() == null) {
            controller.setGlobalContextItem(item, true);
        }
    }

    /**
     * Return a Receiver which can be used to supply the principal source document for the transformation.
     * This method is intended primarily for internal use, though it can also
     * be called by a user application that wishes to feed events into the transformation engine.
     * <p>
     * Saxon calls this method to obtain a Receiver, to which it then sends
     * a sequence of events representing the content of an XML document. This method is provided so that
     * {@code XsltTransformer} implements {@code Destination}, allowing one transformation
     * to receive the results of another in a pipeline.
     * <p>
     * Before calling this method, the {@link #setDestination} method must be called to supply a destination
     * for the transformation.
     * <p>
     * Note that when an {@code XsltTransformer} is used as a {@code Destination}, the initial
     * context node set on that {@code XsltTransformer} using {@link #setInitialContextNode(XdmNode)} is ignored,
     * as is the source set using {@link #setSource(javax.xml.transform.Source)}.
     *
     * @param pipe The Saxon configuration. This is supplied so that the destination can
     *               use information from the configuration (for example, a reference to the name pool)
     *               to construct or configure the returned Receiver.
     * @param params serialization parameters (not relevant here since we aren't serializing; except
     *               possibly for the item-separator property)
     * @return the Receiver to which events are to be sent.
     * @throws SaxonApiException     if the Receiver cannot be created
     * @throws IllegalStateException if no Destination has been supplied
     */

    public Receiver getReceiver(PipelineConfiguration pipe, SerializationProperties params) throws SaxonApiException {
        if (destination == null) {
            throw new IllegalStateException("No destination has been supplied");
        }
        Receiver rt = getReceivingTransformer(controller, parameters, destination);
        rt = new SequenceNormalizerWithSpaceSeparator(rt);
        rt.setPipelineConfiguration(pipe);
        sourceTreeBuilder = rt;
        return rt;
    }

    /**
     * Close this destination, allowing resources to be released. Used when this XsltTransformer is acting
     * as the destination of another transformation. Saxon calls this method when it has finished writing
     * to the destination.
     */

    public void close() throws SaxonApiException {
//        if (sourceTreeBuilder != null) {
//            try {
//                sourceTreeBuilder.close();
//            } catch (XPathException e) {
//                throw new SaxonApiException(e);
//            }
////            NodeInfo doc = sourceTreeBuilder.getCurrentRoot();
////            sourceTreeBuilder = null;
////            if (doc != null) {
////                Receiver sOut = getDestinationReceiver(controller, destination);
////                try {
////                    sOut.open();
////                    controller.setGlobalContextItem(doc);
////                    controller.applyTemplates(doc, sOut);
////                    sOut.close();
////                } catch (TransformerException e) {
////                    throw new SaxonApiException(e);
////                }
////            }
//            destination.closeAndNotify();
//        }
    }

}

