package net.sf.saxon.s9api;

import net.sf.saxon.Controller;
import net.sf.saxon.event.Receiver;
import net.sf.saxon.expr.instruct.UserFunction;
import net.sf.saxon.om.Item;
import net.sf.saxon.om.NodeInfo;
import net.sf.saxon.om.SequenceIterator;
import net.sf.saxon.om.ValueRepresentation;
import net.sf.saxon.query.DynamicQueryContext;
import net.sf.saxon.query.XQueryExpression;
import net.sf.saxon.lib.TraceListener;
import net.sf.saxon.trans.XPathException;
import net.sf.saxon.value.*;

import javax.xml.transform.ErrorListener;
import javax.xml.transform.Source;
import javax.xml.transform.TransformerException;
import javax.xml.transform.URIResolver;
import javax.xml.transform.dom.DOMSource;
import java.io.PrintStream;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

/**
 * An <code>XQueryEvaluator</code> represents a compiled and loaded query ready for execution.
 * The <code>XQueryEvaluator</code> holds details of the dynamic evaluation context for the query.
 *
 * <p>An <code>XQueryEvaluator</code> must not be used concurrently in multiple threads.
 * It is safe, however, to reuse the object within a single thread to run the same
 * query several times. Running the query does not change the context
 * that has been established.</p>
 *
 * <p>An <code>XQueryEvaluator</code> is always constructed by running the <code>Load</code>
 * method of an {@link net.sf.saxon.s9api.XQueryExecutable}.</p>
 *
 * <p>An <code>XQueryEvaluator</code> is itself a <code>Iterable</code>. This makes it possible to
 * evaluate the results in a for-each expression.</p>
 */
public class XQueryEvaluator implements Iterable<XdmItem> {

    private Processor processor;
    private XQueryExpression expression;
    private DynamicQueryContext context;
    private Controller controller;  // used only when making direct calls to global functions
    private Destination destination;
    private Set<XdmNode> updatedDocuments;

    /**
     * Protected constructor
     * @param processor the Saxon processor
     * @param expression the XQuery expression
     */

    protected XQueryEvaluator(Processor processor, XQueryExpression expression) {
        this.processor = processor;
        this.expression = expression;
        this.context = new DynamicQueryContext(expression.getExecutable().getConfiguration());
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
            context.setSchemaValidationMode(mode.getNumber());
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
        return ValidationMode.get(context.getSchemaValidationMode());
    }


    /**
     * Set the source document for the query.
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
     * @param source the source document to act as the initial context item for the query.
     */

    public void setSource(Source source) throws SaxonApiException {
        if (source instanceof NodeInfo) {
            setContextItem(new XdmNode((NodeInfo)source));
        } else if (source instanceof DOMSource) {
            setContextItem(processor.newDocumentBuilder().wrap(source));
        } else {
            setContextItem(processor.newDocumentBuilder().build(source));
        }
    }

    /**
     * Set the initial context item for the query
     * @param item the initial context item, or null if there is to be no initial context item
     */

    public void setContextItem(XdmItem item) {
        if (item != null) {
            context.setContextItem((Item)item.getUnderlyingValue());
        }
    }

    /**
     * Get the initial context item for the query, if one has been set
     * @return the initial context item, or null if none has been set. This will not necessarily
     * be the same object as was supplied, but it will be an XdmItem object that represents
     * the same underlying node or atomic value.
     */

    public XdmItem getContextItem() {
        return (XdmItem)XdmValue.wrap(context.getContextItem());
    }

    /**
     * Set the value of external variable defined in the query
     * @param name the name of the external variable, as a QName
     * @param value the value of the external variable, or null to clear a previously set value
     */

    public void setExternalVariable(QName name, XdmValue value) {
        context.setParameterValue(name.getClarkName(),
                (value == null ? null : value.getUnderlyingValue()));
    }

    /**
     * Get the value that has been set for an external variable
     * @param name the name of the external variable whose value is required
     * @return the value that has been set for the external variable, or null if no value has been set
     */

    public XdmValue getExternalVariable(QName name) {
        Object oval = context.getParameter(name.getClarkName());
        if (oval == null) {
            return null;
        }
        if (oval instanceof ValueRepresentation) {
            return XdmValue.wrap((ValueRepresentation)oval);
        }
        throw new IllegalStateException(oval.getClass().getName());
    }

    /**
     * Set an object that will be used to resolve URIs used in
     * fn:doc() and related functions.
     *
     * @param resolver An object that implements the URIResolver interface, or
     *      null.
     */

    public void setURIResolver(URIResolver resolver) {
        context.setURIResolver(resolver);
    }

    /**
     * Get the URI resolver.
     *
     * @return the user-supplied URI resolver if there is one, or the
     *     system-defined one otherwise
     */

    public URIResolver getURIResolver() {
        return context.getURIResolver();
    }

	/**
	 * Set the error listener. The error listener receives reports of all run-time
     * errors and can decide how to report them.
	 *
	 * @param listener the ErrorListener to be used
	 */

	public void setErrorListener(ErrorListener listener) {
		context.setErrorListener(listener);
	}

	/**
	 * Get the error listener.
	 *
	 * @return the ErrorListener in use
	 */

	public ErrorListener getErrorListener() {
		return context.getErrorListener();
	}

    /**
     * Set a TraceListener which will receive messages relating to the evaluation of all expressions.
     * This option has no effect unless the query was compiled to enable tracing.
     * @param listener the TraceListener to use
     */

    public void setTraceListener(TraceListener listener) {
        context.setTraceListener(listener);
    }

    /**
     * Get the registered TraceListener, if any
     * @return listener the TraceListener in use, or null if none has been set
     */

    public TraceListener getTraceListener() {
        return context.getTraceListener();
    }

    /**
     * Set the destination for output from the fn:trace() function.
     * By default, the destination is System.err. If a TraceListener is in use,
     * this is ignored, and the trace() output is sent to the TraceListener.
     * @param stream the PrintStream to which trace output will be sent. If set to
     * null, trace output is suppressed entirely. It is the caller's responsibility
     * to close the stream after use.
     * @since 9.1
     */

    public void setTraceFunctionDestination(PrintStream stream) {
        context.setTraceFunctionDestination(stream);
    }

    /**
     * Get the destination for output from the fn:trace() function.
     * @return the PrintStream to which trace output will be sent. If no explicitly
     * destination has been set, returns System.err. If the destination has been set
     * to null to suppress trace output, returns null.
     * @since 9.1
     */

    public PrintStream getTraceFunctionDestination() {
        return context.getTraceFunctionDestination();
    }

    /**
     * Set the destination to be used for the query results
     * @param destination the destination to which the results of the query will be sent
     */

    public void setDestination(Destination destination) {
        this.destination = destination;
    }

     /**
     * Perform the query.
     *
     * <ul><li>In the case of a non-updating query, the results are sent to the
     * registered Destination.</li>
     * <li>In the case of an updating query, all updated documents will be available after query
      * execution as the result of the {@link #getUpdatedDocuments} method.</li>
     * </ul>
     * @throws net.sf.saxon.s9api.SaxonApiException if any dynamic error occurs during the query
     * @throws IllegalStateException if this is a non-updating query and no Destination has been
     * supplied for the query results
     */

    public void run() throws SaxonApiException {
        try {
            if (expression.isUpdateQuery()) {
                Set docs = expression.runUpdate(context);
                updatedDocuments = new HashSet();
                for (Iterator iter = docs.iterator(); iter.hasNext();) {
                    NodeInfo root = (NodeInfo)iter.next();
                    updatedDocuments.add((XdmNode)XdmNode.wrapItem(root));
                }
            } else {
                if (destination == null) {
                    throw new IllegalStateException("No destination supplied");
                }

                Receiver receiver;
                if (destination instanceof Serializer) {
                    receiver = ((Serializer)destination).getReceiver(expression.getExecutable());
                } else {
                    receiver = destination.getReceiver(expression.getExecutable().getConfiguration());
                }
                expression.run(context, receiver, null);
                destination.close();
            }
        } catch (TransformerException e) {
            throw new SaxonApiException(e);
        }
    }

    /**
     * Perform the query, sending the results to a specified destination.
     *
     * <p>This method must not be used with an updating query.</p>
     *
     * <p>This method is designed for use with a query that produces a single node (typically
     * a document node or element node) as its result. If the query produces multiple nodes,
     * the effect depends on the kind of destination. For example, if the result is an
     * <code>XdmDestination</code>, only the last of the nodes will be accessible.</p>
     * 
     * @param destination The destination where the result document will be sent
     * @throws net.sf.saxon.s9api.SaxonApiException if any dynamic error occurs during the query
     * @throws IllegalStateException if this is an updating query
     */

    public void run(Destination destination) throws SaxonApiException {
        if (expression.isUpdateQuery()) {
            throw new IllegalStateException("Query is updating");
        }
        try {
            Receiver receiver;
            if (destination instanceof Serializer) {
                receiver = ((Serializer)destination).getReceiver(expression.getExecutable());
            } else {
                receiver = destination.getReceiver(expression.getExecutable().getConfiguration());
            }
            expression.run(context, receiver, null);
        } catch (TransformerException e) {
            throw new SaxonApiException(e);
        }
    }

    /**
     * Perform the query, returning the results as an XdmValue. This method
     * must not be used with an updating query
     * @return an XdmValue representing the results of the query
     * @throws SaxonApiException if the query fails with a dynamic error
     * @throws IllegalStateException if this is an updating query
     */

    public XdmValue evaluate() throws SaxonApiException {
        if (expression.isUpdateQuery()) {
            throw new IllegalStateException("Query is updating");
        }
        try {
            SequenceIterator iter = expression.iterator(context);
            ValueRepresentation result = SequenceExtent.makeSequenceExtent(iter);
            if (result instanceof NodeInfo) {
                return new XdmNode((NodeInfo)result);
            } else if (result instanceof AtomicValue) {
                return new XdmAtomicValue((AtomicValue)result);
            } else if (result instanceof EmptySequence) {
                return XdmEmptySequence.getInstance();
            } else {
                return new XdmValue(result);
            }
        } catch (XPathException e) {
            throw new SaxonApiException(e);
        }
    }

    /**
     * Evaluate the XQuery expression, returning the result as an <code>XdmItem</code> (that is,
     * a single node or atomic value).
     *
     * @return an <code>XdmItem</code> representing the result of the query, or null if the query
     * returns an empty sequence. If the expression returns a sequence of more than one item,
     * any items after the first are ignored.
     * @throws SaxonApiException if a dynamic error occurs during the query evaluation.
     * @since 9.2
     */


    public XdmItem evaluateSingle() throws SaxonApiException {
        try {
            SequenceIterator iter = expression.iterator(context);
            return (XdmItem) XdmValue.wrap(iter.next());
        } catch (XPathException e) {
            throw new SaxonApiException(e);
        }
    }

    /**
     * Evaluate the query, and return an iterator over its results.
     * <p>This method must not be used with an updating query.</p>
     * @throws SaxonApiUncheckedException if a dynamic error is detected while constructing the iterator.
     * It is also possible for an SaxonApiUncheckedException to be thrown by the hasNext() method of the
     * returned iterator if a dynamic error occurs while evaluating the result sequence.
     * @throws IllegalStateException if this is an updating query
     */

    public Iterator<XdmItem> iterator() throws SaxonApiUncheckedException {
        if (expression.isUpdateQuery()) {
            throw new IllegalStateException("Query is updating");
        }
        try {
            return new XdmSequenceIterator(expression.iterator(context));
        } catch (XPathException e) {
            throw new SaxonApiUncheckedException(e);
        }
    }

    /**
     * After executing an updating query using the {@link #run()} method, iterate over the root
     * nodes of the documents updated by the query.
     *
     * <p>The results remain available until a new query is executed. This method returns the results
     * of the most recently executed query. It does not consume the results.</p>
     * @return an iterator over the root nodes of documents (or other trees) that were updated by the query
     * @since 9.1
     */

    public Iterator<XdmNode> getUpdatedDocuments() {
        return updatedDocuments.iterator();
    }

    /**
     * Call a global user-defined function in the compiled query.
     *
     * If this is called more than once (to evaluate the same function repeatedly with different arguments,
     * or to evaluate different functions) then the sequence of evaluations uses the same values of global
     * variables including external variables (query parameters); the effect of any changes made to query parameters
     * between calls is undefined.
     * @param function The name of the function to be called
     * @param arguments The values of the arguments to be supplied to the function. These
     * must be of the correct type as defined in the function signature (there is no automatic
     * conversion to the required type).
     * @throws SaxonApiException if no function has been defined with the given name and arity;
     * or if any of the arguments does not match its required type according to the function
     * signature; or if a dynamic error occurs in evaluating the function.
     * @since 9.3
     */

    public XdmValue callFunction(QName function, XdmValue[] arguments) throws SaxonApiException {
        final UserFunction fn = expression.getStaticContext().getUserDefinedFunction(
                function.getNamespaceURI(), function.getLocalName(), arguments.length);
        if (fn == null) {
            throw new SaxonApiException("No function with name " + function.getClarkName() +
                    " and arity " + arguments.length + " has been declared in the query");
        }
        try {
            // TODO: use the same controller in other interfaces such as run(), and expose it in a trapdoor API
            if (controller == null) {
                controller = expression.newController();
                context.initializeController(controller);
                controller.defineGlobalParameters();
            }
            ValueRepresentation[] vr = new ValueRepresentation[arguments.length];

            for (int i=0; i<arguments.length; i++) {
                SequenceType type = fn.getParameterDefinitions()[i].getRequiredType();
                vr[i] = arguments[i].getUnderlyingValue();
                if (!type.matches(Value.asValue(vr[i]), controller.getConfiguration())) {
                    throw new SaxonApiException("Argument " + (i+1) +
                            " of function " + function.getClarkName() +
                            " does not match the required type " + type.toString());
                }
            }
            ValueRepresentation result = fn.call(vr, controller);
            return XdmValue.wrap(result);
        } catch (XPathException e) {
            throw new SaxonApiException(e);
        }
    }

    /**
     * Get the underlying dynamic context object. This provides an escape hatch to the underlying
     * implementation classes, which contain methods that may change from one release to another.
     * @return the underlying object representing the dynamic context for query execution
     */

    public DynamicQueryContext getUnderlyingQueryContext() {
        return context;
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

