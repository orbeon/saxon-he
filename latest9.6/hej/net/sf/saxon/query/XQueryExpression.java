////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
// Copyright (c) 2014 Saxonica Limited.
// This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
// If a copy of the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
// This Source Code Form is "Incompatible With Secondary Licenses", as defined by the Mozilla Public License, v. 2.0.
////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

package net.sf.saxon.query;

import net.sf.saxon.Configuration;
import net.sf.saxon.Controller;
import net.sf.saxon.event.*;
import net.sf.saxon.evpull.ComplexContentProcessor;
import net.sf.saxon.evpull.EventIterator;
import net.sf.saxon.evpull.EventIteratorToReceiver;
import net.sf.saxon.expr.*;
import net.sf.saxon.expr.instruct.Executable;
import net.sf.saxon.expr.instruct.GlobalParam;
import net.sf.saxon.expr.instruct.GlobalVariable;
import net.sf.saxon.expr.instruct.SlotManager;
import net.sf.saxon.expr.parser.ContextItemStaticInfo;
import net.sf.saxon.expr.parser.ExpressionTool;
import net.sf.saxon.expr.parser.ExpressionVisitor;
import net.sf.saxon.expr.parser.Optimizer;
import net.sf.saxon.lib.*;
import net.sf.saxon.om.*;
import net.sf.saxon.trace.ExpressionPresenter;
import net.sf.saxon.trans.SaxonErrorCode;
import net.sf.saxon.trans.XPathException;
import net.sf.saxon.tree.iter.ManualIterator;

import javax.xml.transform.Result;
import javax.xml.transform.Source;
import javax.xml.transform.TransformerException;
import javax.xml.transform.stream.StreamResult;
import java.io.OutputStream;
import java.util.*;

/**
 * XQueryExpression represents a compiled query. This object is immutable and thread-safe,
 * the same compiled query may be executed many times in series or in parallel. The object
 * can be created only by using the compileQuery method of the QueryProcessor class.
 * <p/>
 * <p>Various methods are provided for evaluating the query, with different options for
 * delivery of the results.</p>
 */
public class XQueryExpression implements Container {

    protected Expression expression;
    protected SlotManager stackFrameMap;
    protected Executable executable;
    protected QueryModule mainModule;


    /**
     * The constructor is protected, to ensure that instances can only be
     * created using the compileQuery() methods of StaticQueryContext
     * @param exp        an expression to be wrapped as an XQueryExpression
     * @param mainModule the static context of the main module
     * @param streaming  true if streamed execution is requested
     * @throws XPathException if an error occurs
     */

    public XQueryExpression(Expression exp,  QueryModule mainModule, boolean streaming)
            throws XPathException {
        Executable exec = mainModule.getExecutable();
        Configuration config = mainModule.getConfiguration();
        stackFrameMap = config.makeSlotManager();
        executable = exec;
        this.mainModule = mainModule;
        exp.setContainer(this);
        Optimizer optimizer = config.obtainOptimizer();
        try {
            ExpressionVisitor visitor = ExpressionVisitor.make(mainModule);
            visitor.setOptimizeForStreaming(streaming);
            exp = visitor.simplify(exp);
            exp.checkForUpdatingSubexpressions();
            ContextItemStaticInfo cit = new ContextItemStaticInfo(mainModule.getUserQueryContext().getRequiredContextItemType(), true);
            exp = visitor.typeCheck(exp, cit);
            if (optimizer.getOptimizationLevel() != Optimizer.NO_OPTIMIZATION) {
                exp = exp.optimize(visitor, cit);
            }
        } catch (XPathException err) {
            //err.printStackTrace();
            mainModule.reportStaticError(err);
            throw err;
        }
        ExpressionTool.allocateSlots(exp, 0, stackFrameMap);


        expression = exp;
        executable.setConfiguration(config);
    }

    /**
     * Get the expression wrapped in this XQueryExpression object
     *
     * @return the underlying expression
     */

    public Expression getExpression() {
        return expression;
    }

    /**
     * Get data about the unit of compilation (XQuery module, XSLT package) to which this
     * container belongs
     */
    public PackageData getPackageData() {
        return mainModule.getPackageData();
    }

    /**
     * Get the granularity of the container.
     *
     * @return 0 for a temporary container created during parsing; 1 for a container
     *         that operates at the level of an XPath expression; 2 for a container at the level
     *         of a global function or template
     */

    public int getContainerGranularity() {
        return 2;
    }

    /**
     * Get the Configuration to which this Container belongs
     *
     * @return the Configuration
     */
    public Configuration getConfiguration() {
        return mainModule.getConfiguration();
    }

    /**
     * Ask whether this query uses the context item
     *
     * @return true if the context item is referenced either in the query body or in the initializer
     *         of any global variable
     */

    public boolean usesContextItem() {
        GlobalVariable contextVar = getExecutable().getInitialContextItemVariable();
        Binding[] binding = null;
        if (contextVar != null) {
            binding = new Binding[]{contextVar};
        }
        if (ExpressionTool.dependsOnFocus(expression)) {
            return true;
        }
        if (binding != null && ExpressionTool.dependsOnVariable(expression, binding)) {
            return true;
        }
        List<GlobalVariable> map = executable.getCompiledGlobalVariables();
        if (map != null) {
            for (GlobalVariable var : map) {
                Expression select = var.getSelectExpression();
                if (select != null && ExpressionTool.dependsOnFocus(select)) {
                    return true;
                }
                if (select != null && binding != null && ExpressionTool.dependsOnVariable(select, binding)) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Ask whether this is an update query
     *
     * @return true if the body of the query is an updating expression
     *         (as defined by the XQuery Update specification). Note that a query can use Update syntax
     *         (notably the copy-modify syntax) without being an updating expression.
     */

    public boolean isUpdateQuery() {
        return false;
    }

    /**
     * Get the stack frame map used for the outermost level of this query
     *
     * @return the stack frame map
     */

    public SlotManager getStackFrameMap() {
        return stackFrameMap;
    }

    /**
     * Output the path map of the query for diagnostics
     */

    public void explainPathMap() {
        // No action (requires Saxon-EE)
    }

    /**
     * Get the static context in which this expression was compiled. This is essentially an internal
     * copy of the original user-created StaticQueryContext object, augmented with information obtained
     * from the query prolog of the main query module, and with information about functions and variables
     * imported from other library modules. The user-created StaticQueryContext object is not modified
     * by Saxon, whereas the QueryModule object includes additional information found in the query prolog.
     *
     * @return the QueryModule object representing the static context of the main module of the query.
     *         This is available for inspection, but must not be modified or reused by the application.
     */
    public QueryModule getMainModule() {
        return mainModule;
    }

    /**
     * Get a list containing the names of the external variables in the query.
     * <p/>
     * <p><i>Changed in Saxon 9.0 to return an array of StructuredQName objects rather than
     * integer fingerprints.</i></p>
     *
     * @return an array of StructuredQName objects, representing the names of external variables defined
     *         in the query
     */

    /*@NotNull*/
    public StructuredQName[] getExternalVariableNames() {
        List list = stackFrameMap.getVariableMap();
        StructuredQName[] names = new StructuredQName[stackFrameMap.getNumberOfVariables()];
        for (int i = 0; i < names.length; i++) {
            names[i] = (StructuredQName) list.get(i);
        }
        return names;
    }

    /**
     * Execute a the compiled Query, returning the results as a List.
     *
     * @param env Provides the dynamic query evaluation context
     * @return The results of the expression, as a List. The List represents the sequence
     *         of items returned by the expression. Each item in the list will either be an
     *         object representing a node, or an object representing an atomic value.
     *         For the types of Java object that may be returned, see the description of the
     *         {@link net.sf.saxon.xpath.XPathEvaluator#evaluate evaluate} method
     *         of class XPathProcessor
     * @throws XPathException if a dynamic error occurs during query evaluation
     */

    /*@NotNull*/
    public List<Object> evaluate(/*@NotNull*/ DynamicQueryContext env) throws XPathException {
        if (isUpdateQuery()) {
            throw new XPathException("Cannot call evaluate() on an updating query");
        }
        SequenceIterator iterator = iterator(env);
        ArrayList<Object> list = new ArrayList<Object>(100);
        Item item;
        while ((item = iterator.next()) != null) {
            list.add(SequenceTool.convertToJava(item));
        }
        return list;
    }

    /**
     * Execute the compiled Query, returning the first item in the result.
     * This is useful where it is known that the expression will only return
     * a singleton value (for example, a single node, or a boolean).
     *
     * @param env Provides the dynamic query evaluation context
     * @return The first item in the sequence returned by the expression. If the expression
     *         returns an empty sequence, this method returns null. Otherwise, it returns the first
     *         item in the result sequence, represented as a Java object using the same mapping as for
     *         the {@link XQueryExpression#evaluate evaluate} method
     * @throws XPathException if evaluation fails with a dynamic error
     */

    /*@Nullable*/
    public Object evaluateSingle(/*@NotNull*/ DynamicQueryContext env) throws XPathException {
        if (isUpdateQuery()) {
            throw new XPathException("Cannot call evaluateSingle() on an updating query");
        }
        SequenceIterator iterator = iterator(env);
        Item item = iterator.next();
        if (item == null) {
            return null;
        }
        return SequenceTool.convertToJava(item);
    }

    /**
     * Get an iterator over the results of the expression. This returns results without
     * any conversion of the returned items to "native" Java classes. The iterator will
     * deliver a sequence of Item objects, each item being either a NodeInfo (representing
     * a node) or an AtomicValue (representing an atomic value).
     * <p/>
     * <p>To get the results of the query in the form of an XML document in which each
     * item is wrapped by an element indicating its type, use:</p>
     * <p/>
     * <p><code>QueryResult.wrap(iterator(env))</code></p>
     * <p/>
     * <p>To serialize the results to a file, use the QueryResult.serialize() method.</p>
     *
     * @param env Provides the dynamic query evaluation context
     * @return an iterator over the results of the query. The class SequenceIterator
     *         is modeled on the standard Java Iterator class, but has extra functionality
     *         and can throw exceptions when errors occur.
     * @throws XPathException if a dynamic error occurs in evaluating the query. Some
     *                        dynamic errors will not be reported by this method, but will only be reported
     *                        when the individual items of the result are accessed using the returned iterator.
     */

    /*@NotNull*/
    public SequenceIterator iterator(/*@NotNull*/ DynamicQueryContext env) throws XPathException {
        if (isUpdateQuery()) {
            throw new XPathException("Cannot call iterator() on an updating query");
        }
        if (!env.getConfiguration().isCompatible(getExecutable().getConfiguration())) {
            throw new XPathException("The query must be compiled and executed under the same Configuration", SaxonErrorCode.SXXP0004);
        }
        Controller controller = newController(env);

        try {
            Item contextItem = env.getContextItem();
            if (contextItem instanceof DocumentInfo && ((DocumentInfo) contextItem).isTyped() && !getExecutable().isSchemaAware()) {
                throw new XPathException("A typed input document can only be used with a schema-aware query");
            }

            XPathContextMajor context = initialContext(env, controller);

            // In tracing/debugging mode, evaluate all the global variables first
            if (controller.getTraceListener() != null) {
                controller.preEvaluateGlobals(context);
            }

            context.openStackFrame(stackFrameMap);

            SequenceIterator iterator = expression.iterate(context);
            if ((iterator.getProperties() & SequenceIterator.GROUNDED) != 0) {
                return iterator;
            } else {
                return new ErrorReportingIterator(iterator, controller.getErrorListener());
            }
        } catch (XPathException err) {
            TransformerException terr = err;
            while (terr.getException() instanceof TransformerException) {
                terr = (TransformerException) terr.getException();
            }
            XPathException de = XPathException.makeXPathException(terr);
            controller.reportFatalError(de);
            throw de;
        }
    }

    /**
     * Run the query, sending the results directly to a JAXP Result object. This way of executing
     * the query is most efficient in the case of queries that produce a single document (or parentless
     * element) as their output, because it avoids constructing the result tree in memory: instead,
     * it is piped straight to the serializer.
     *
     * @param env              the dynamic query context
     * @param result           the destination for the results of the query. The query is effectively wrapped
     *                         in a document{} constructor, so that the items in the result are concatenated to form a single
     *                         document; this is then written to the requested Result destination, which may be (for example)
     *                         a DOMResult, a SAXResult, or a StreamResult
     * @param outputProperties Supplies serialization properties, in JAXP format, if the result is to
     *                         be serialized. This parameter can be defaulted to null.
     * @throws XPathException if the query fails.
     */

    public void run(/*@NotNull*/ DynamicQueryContext env, /*@NotNull*/ Result result, Properties outputProperties) throws XPathException {
        if (isUpdateQuery()) {
            throw new XPathException("Cannot call run() on an updating query");
        }
        if (!env.getConfiguration().isCompatible(getExecutable().getConfiguration())) {
            throw new XPathException("The query must be compiled and executed under the same Configuration", SaxonErrorCode.SXXP0004);
        }
        Item contextItem = env.getContextItem();
        if (contextItem instanceof DocumentInfo && ((DocumentInfo) contextItem).isTyped() && !getExecutable().isSchemaAware()) {
            throw new XPathException("A typed input document can only be used with a schema-aware query");
        }
        Controller controller = newController(env);

        Properties actualProperties = validateOutputProperties(controller, outputProperties);

        //controller.defineGlobalParameters();

        XPathContextMajor context = initialContext(env, controller);

        // In tracing/debugging mode, evaluate all the global variables first
        TraceListener tracer = controller.getTraceListener();
        if (tracer != null) {
            controller.preEvaluateGlobals(context);
            tracer.open(controller);
        }

        context.openStackFrame(stackFrameMap);

        boolean mustClose = result instanceof StreamResult && ((StreamResult) result).getOutputStream() == null;
        SerializerFactory sf = context.getConfiguration().getSerializerFactory();
        PipelineConfiguration pipe = controller.makePipelineConfiguration();
        pipe.setHostLanguage(Configuration.XQUERY);
        Receiver receiver = sf.getReceiver(result, pipe, actualProperties);
        context.changeOutputDestination(receiver, null);
        Receiver out = context.getReceiver();
        String itemSeparator = actualProperties.getProperty(SaxonOutputKeys.ITEM_SEPARATOR);
        if (out instanceof ComplexContentOutputter && itemSeparator != null) {
            out = new SequenceNormalizer((SequenceReceiver)out, itemSeparator);
            context.setReceiver((SequenceReceiver)out);
        }
        out.open();

        // Run the query
        try {
            expression.process(context);
        } catch (XPathException err) {
            controller.reportFatalError(err);
            throw err;
        }

        if (tracer != null) {
            tracer.close();
        }

        out.close();
        if (mustClose) {
            OutputStream os = ((StreamResult) result).getOutputStream();
            if (os != null) {
                try {
                    os.close();
                } catch (java.io.IOException err) {
                    throw new XPathException(err);
                }
            }
        }
    }

    /**
     * Run the query in streaming mode, assuming it has been compiled for streaming.
     * This requires Saxon-EE.
     * @param dynamicEnv the dynamic execution context
     * @param source the input document, as a SAXSource or StreamSource
     * @param result the destination for the query results
     * @param outputProperties serialization options for the query result
     * @throws XPathException if streamed evaluation of the query fails
     */

    public void runStreamed(DynamicQueryContext dynamicEnv, Source source, Result result, Properties outputProperties) throws XPathException {
        throw new XPathException("Streaming requires Saxon-EE");
    }


    /*@NotNull*/
    protected Properties validateOutputProperties(/*@NotNull*/ Controller controller, /*@Nullable*/ Properties outputProperties) throws XPathException {
        // Validate the serialization properties requested

        Properties baseProperties = controller.getExecutable().getDefaultOutputProperties();
        if (outputProperties != null) {
            Enumeration iter = outputProperties.propertyNames();
            while (iter.hasMoreElements()) {
                String key = (String) iter.nextElement();
                String value = outputProperties.getProperty(key);
                try {
                    value = SaxonOutputKeys.checkOutputProperty(key, value, controller.getConfiguration(), true);
                    baseProperties.setProperty(key, value);
                } catch (XPathException dynamicError) {
                    outputProperties.remove(key);
                    controller.getErrorListener().warning(dynamicError);
                }
            }
        }
        if (baseProperties.getProperty("method") == null) {
            // XQuery forces the default method to XML, unlike XSLT where it depends on the contents of the result tree
            baseProperties.setProperty("method", "xml");
        }
        return baseProperties;
    }

    /**
     * Run the query in pull mode.
     * <p/>
     * <p>For maximum effect this method should be used when lazyConstructionMode has been set in the Configuration.</p>
     * <p/>
     * <p><b>Note: this method usually has very similar performance to the
     * {@link #run(DynamicQueryContext, javax.xml.transform.Result, java.util.Properties)} method (which does
     * the same thing), but sometimes it is significantly slower. Therefore, the run() method is preferred.</b></p>
     *
     * @param dynamicEnv       the dynamic context for query evaluation
     * @param destination      the destination of the query results
     * @param outputProperties additional serialization parameters (may be null)
     * @throws XPathException if a dynamic error occurs during the evaluation
     * @see FeatureKeys#LAZY_CONSTRUCTION_MODE
     */

    public void pull(/*@NotNull*/ DynamicQueryContext dynamicEnv, /*@NotNull*/ Result destination, Properties outputProperties) throws XPathException {
        if (isUpdateQuery()) {
            throw new XPathException("Cannot call pull() on an updating query");
        }
        Configuration config = dynamicEnv.getConfiguration();
        try {
            Controller controller = newController(dynamicEnv);

            Properties actualProperties = validateOutputProperties(controller, outputProperties);
            SerializerFactory sf = config.getSerializerFactory();
            PipelineConfiguration pipe = config.makePipelineConfiguration();
            pipe.setSerializing(true);
            Receiver receiver = sf.getReceiver(destination, pipe, actualProperties);

            receiver = new NamespaceReducer(receiver);
            if ("yes".equals(actualProperties.getProperty(SaxonOutputKeys.WRAP))) {
                receiver = config.getSerializerFactory().newSequenceWrapper(receiver);
            } else {
                receiver = new TreeReceiver(receiver);
            }
            EventIterator iter = internalIterateEvents(controller, dynamicEnv);
            EventIteratorToReceiver.copy(iter, (SequenceReceiver)receiver);
        } catch (XPathException err) {
            config.reportFatalError(err);
            throw err;
        }

    }

    /**
     * Run the query in pull mode.
     * <p/>
     * <p>For maximum effect this method should be used when lazyConstructionMode has been set in the Configuration.</p>
     * <p/>
     * <p><b>Note: this method usually has very similar performance to the
     * {@link #run(DynamicQueryContext, javax.xml.transform.Result, java.util.Properties)} method (which does
     * the same thing), but sometimes it is significantly slower. Therefore, the run() method is preferred.</b></p>
     *
     * @param dynamicEnv  the dynamic context for query evaluation
     * @param destination the Receiver to accept the query results *usually a serializer)
     * @throws XPathException if a dynamic error occurs during the evaluation
     * @see FeatureKeys#LAZY_CONSTRUCTION_MODE
     */

    public void pull(DynamicQueryContext dynamicEnv, SequenceReceiver destination) throws XPathException {
        if (isUpdateQuery()) {
            throw new XPathException("Cannot call pull() on an updating query");
        }
        Configuration config = dynamicEnv.getConfiguration();
        try {
            Controller controller = newController(dynamicEnv);
            EventIterator iter = internalIterateEvents(controller, dynamicEnv);
            EventIteratorToReceiver.copy(iter, destination);
        } catch (XPathException err) {
            config.reportFatalError(err);
            throw err;
        }

    }


    /**
     * Run the query returning the results as an EventIterator
     *
     * @param controller The Controller used to run the query
     * @param dynamicEnv the XQuery dynamic context for evaluating the query
     * @return an EventIterator representing the results of the query as a sequence of events
     * @throws XPathException if evaluation fails with a dynamic error
     */

    /*@NotNull*/
    public EventIterator iterateEvents(/*@NotNull*/ Controller controller, /*@NotNull*/ DynamicQueryContext dynamicEnv) throws XPathException {
        if (isUpdateQuery()) {
            throw new XPathException("Cannot call iterateEvents() on an updating query");
        }
        dynamicEnv.initializeController(controller);
        return internalIterateEvents(controller, dynamicEnv);
    }

    private EventIterator internalIterateEvents(Controller controller, DynamicQueryContext dynamicEnv) throws XPathException {
        XPathContextMajor context = initialContext(dynamicEnv, controller);

        // In tracing/debugging mode, evaluate all the global variables first
        if (controller.getTraceListener() != null) {
            controller.preEvaluateGlobals(context);
        }

        context.openStackFrame(stackFrameMap);

        final Configuration config = executable.getConfiguration();

        EventIterator ei = expression.iterateEvents(context);
        //ei = new TracingEventIterator(EventStackIterator.flatten(ei));
        return new ComplexContentProcessor(config, ei);
    }

    /**
     * Run an updating query
     *
     * @param dynamicEnv the dynamic context for query execution
     * @return a set of nodes representing the roots of trees that have been modified as a result
     *         of executing the update. Note that this method will never modify persistent data on disk; it returns
     *         the root nodes of the affected documents (which will often be document nodes whose document-uri can
     *         be ascertained), and it is the caller's responsibility to decide what to do with them.
     *         <p>On completion of this method it is generally unsafe to rely on the contents or relationships
     *         of NodeInfo objects that were obtained before the updating query was run. Such objects may or may not
     *         reflect the results of the update operations. This is especially true in the case of nodes that
     *         are part of a subtree that has been deleted (detached from its parent). Instead, the new updated
     *         tree should be accessed by navigation from the root nodes returned by this method.</p>
     * @throws XPathException if evaluation of the update query fails, or it this is not an updating query
     */

    /*@NotNull*/
    public Set<MutableNodeInfo> runUpdate(/*@NotNull*/ DynamicQueryContext dynamicEnv) throws XPathException {
        throw new XPathException("Calling runUpdate() on a non-updating query");
    }

    /**
     * Run an updating query, writing back eligible updated node to persistent storage.
     * <p/>
     * <p>A node is eligible to be written back to disk if it is present in the document pool,
     * which generally means that it was originally read using the doc() or collection() function.</p>
     * <p/>
     * <p>On completion of this method it is generally unsafe to rely on the contents or relationships
     * of NodeInfo objects that were obtained before the updating query was run. Such objects may or may not
     * reflect the results of the update operations. This is especially true in the case of nodes that
     * are part of a subtree that has been deleted (detached from its parent). Instead, the new updated
     * tree should be accessed by navigation from the root nodes returned by this method.</p>
     * <p/>
     * <p>If one or more eligible updated nodes cannot be written back to disk, perhaps because the URI
     * identifies a non-updatable location, then an exception is thrown. In this case it is undefined
     *
     * @param dynamicEnv the dynamic context for query execution
     * @param agent      a callback class that is called to process each document updated by the query
     * @throws XPathException if evaluation of the update query fails, or it this is not an updating query
     */

    public void runUpdate(/*@NotNull*/ DynamicQueryContext dynamicEnv, /*@NotNull*/ UpdateAgent agent) throws XPathException {
        throw new XPathException("Calling runUpdate() on a non-updating query");
    }


    protected XPathContextMajor initialContext(DynamicQueryContext dynamicEnv, Controller controller) throws XPathException {

        // The way the initial context item is handled in XQuery is somewhat convoluted. We create a global variable
        // or parameter called saxon:context-item based on the context item declaration in the query if there is one;
        // this is used to take advantage of the circularity detection for global variables. References to the context
        // item within a global variable are bound to $saxon:context-item, to allow forwards references to work in the
        // case where the context item is initialized within the query prolog. But not all top-level references to the
        // context item are caught by this mechanism; for example implicit references caused by making dynamic function
        // calls on context-dependent functions. So we bind the context item in the XPathContext object as well.

        Item contextItem = dynamicEnv.getContextItem();

        GlobalVariable contextVar = executable.getInitialContextItemVariable();
        if (contextVar != null && contextItem != null) {
            dynamicEnv.setParameter(contextVar.getVariableQName(), contextItem);
        }

        XPathContextMajor context = controller.newXPathContext();

        if (contextItem != null) {
            // Check the type of the externally-supplied context item against the API-defined required type
            if (!mainModule.getUserQueryContext().getRequiredContextItemType().matchesItem(
                    contextItem, false, dynamicEnv.getConfiguration())) {
                throw new XPathException("The supplied context item does not match the required context item type", "XPTY0004");
            }
            // Now check it against the required type defined in the query prolog
            if (contextVar != null) {
                dynamicEnv.setParameter(contextVar.getVariableQName(), contextItem);
                try {
                    // do early evaluation to force the type-checking of the context item
                    controller.getBindery().useGlobalParameter(
                            contextVar.getVariableQName(), contextVar.getBinderySlotNumber(), contextVar.getRequiredType(), context);
                } catch (XPathException err) {
                    err.maybeSetLocation(contextVar);
                    if (!err.hasBeenReported()) {
                        controller.getErrorListener().fatalError(err);
                    }
                    throw err;
                }
            }
            ManualIterator single = new ManualIterator(contextItem);
            context.setCurrentIterator(single);
            controller.setInitialContextItem(contextItem);
        } else {
            // there might be a default value for the context item
            if (contextVar != null) {
                if (!(contextVar instanceof GlobalParam && contextVar.getSelectExpression() instanceof ErrorExpression)) {
                    try {
                        // do early evaluation to force the type-checking of the context item
                        controller.getBindery().useGlobalParameter(
                                contextVar.getVariableQName(), contextVar.getBinderySlotNumber(), contextVar.getRequiredType(), context);
                    } catch (XPathException err) {
                        err.maybeSetLocation(contextVar);
                        if (!err.hasBeenReported()) {
                            controller.getErrorListener().fatalError(err);
                        }
                        throw err;
                    }
                    Sequence val = contextVar.getSelectValue(context, contextVar.getDeclaringComponent());
                    contextItem = val.head();
                    ManualIterator single = new ManualIterator(contextItem);
                    context.setCurrentIterator(single);
                    controller.setInitialContextItem(contextItem);
                }
            }
        }

        return context;
    }

    /**
     * Get a controller that can be used to execute functions in this compiled query.
     * Functions in the query module can be found using {@link QueryModule#getUserDefinedFunction}.
     * They can then be called directly from the Java application using {@link net.sf.saxon.expr.instruct.UserFunction#call}
     * The same Controller can be used for a series of function calls. Note that the Controller should only be used
     * in a single thread.
     * @param env the dynamic context for evaluation
     * @return a newly constructed Controller
     * @throws XPathException if evaluation fails with a dynamic error
     */

    /*@NotNull*/
    public Controller newController(DynamicQueryContext env) throws XPathException {
        Controller controller = new Controller(executable.getConfiguration(), executable);
        controller.getBindery().allocateGlobals(executable.getGlobalVariableMap());
        env.initializeController(controller);
        return controller;
    }

    /**
     * Diagnostic method: display a representation of the compiled query on the
     * selected output stream.
     *
     * @param out an ExpressionPresenter to which the XML representation of the compiled query
     *            will be sent
     */

    public void explain(/*@NotNull*/ ExpressionPresenter out) {
        out.startElement("query");
        getExecutable().getKeyManager().explainKeys(out);
        getExecutable().explainGlobalVariables(out);
        mainModule.explainGlobalFunctions(out);
        out.startElement("body");
        expression.explain(out);
        out.endElement();
        out.endElement();
        out.close();
    }

    /**
     * Get the Executable (representing a complete stylesheet or query) of which this Container forms part
     * @return the Executable
     */

    public Executable getExecutable() {
        return executable;
    }

    /**
     * Get the LocationProvider allowing location identifiers representing the position
     * of an expression in the query) to be resolved.
     * @return the location provider
     */

    public LocationProvider getLocationProvider() {
        return executable.getLocationMap();
    }

    /**
     * Indicate that document projection is or is not allowed
     *
     * @param allowed true if projection is allowed
     */

    public void setAllowDocumentProjection(boolean allowed) {
        if (allowed) {
            throw new UnsupportedOperationException("Document projection requires Saxon-EE");
        }
    }

    /**
     * Ask whether document projection is allowed
     *
     * @return true if document projection is allowed
     */

    public boolean isDocumentProjectionAllowed() {
        return false;
    }

    /**
     * Return the public identifier for the current document event.
     * <p/>
     * <p>The return value is the public identifier of the document
     * entity or of the external parsed entity in which the markup that
     * triggered the event appears.</p>
     *
     * @return A string containing the public identifier, or
     *         null if none is available.
     * @see #getSystemId
     */
    /*@Nullable*/
    public String getPublicId() {
        return null;
    }

    /**
     * Return the system identifier for the current document event.
     * <p/>
     * <p>The return value is the system identifier of the document
     * entity or of the external parsed entity in which the markup that
     * triggered the event appears.</p>
     * <p/>
     * <p>If the system identifier is a URL, the parser must resolve it
     * fully before passing it to the application.</p>
     *
     * @return A string containing the system identifier, or null
     *         if none is available.
     * @see #getPublicId
     */
    /*@Nullable*/
    public String getSystemId() {
        return null;
    }

    /**
     * Return the line number where the current document event ends.
     * <p/>
     * <p><strong>Warning:</strong> The return value from the method
     * is intended only as an approximation for the sake of error
     * reporting; it is not intended to provide sufficient information
     * to edit the character content of the original XML document.</p>
     * <p/>
     * <p>The return value is an approximation of the line number
     * in the document entity or external parsed entity where the
     * markup that triggered the event appears.</p>
     *
     * @return The line number, or -1 if none is available.
     * @see #getColumnNumber
     */
    public int getLineNumber() {
        return -1;
    }

    /**
     * Return the character position where the current document event ends.
     * <p/>
     * <p><strong>Warning:</strong> The return value from the method
     * is intended only as an approximation for the sake of error
     * reporting; it is not intended to provide sufficient information
     * to edit the character content of the original XML document.</p>
     * <p/>
     * <p>The return value is an approximation of the column number
     * in the document entity or external parsed entity where the
     * markup that triggered the event appears.</p>
     *
     * @return The column number, or -1 if none is available.
     * @see #getLineNumber
     */
    public int getColumnNumber() {
        return -1;
    }

    /**
     * Get the host language (XSLT, XQuery, XPath) used to implement the code in this container
     *
     * @return typically {@link net.sf.saxon.Configuration#XSLT} or {@link net.sf.saxon.Configuration#XQUERY}
     */

    public int getHostLanguage() {
        return Configuration.XQUERY;
    }

    /**
     * ErrorReportingIterator is an iterator that wraps a base iterator and reports
     * any exceptions that are raised to the ErrorListener
     */

    private class ErrorReportingIterator implements SequenceIterator {
        private SequenceIterator base;
        private UnfailingErrorListener listener;

        public ErrorReportingIterator(SequenceIterator base, UnfailingErrorListener listener) {
            this.base = base;
            this.listener = listener;
        }

        /*@Nullable*/
        public Item next() throws XPathException {
            try {
                return base.next();
            } catch (XPathException e1) {
                e1.maybeSetLocation(expression);
                listener.fatalError(e1);
                e1.setHasBeenReported(true);
                throw e1;
            }
        }

        public void close() {
            base.close();
        }

        /*@NotNull*/
        public SequenceIterator getAnother() throws XPathException {
            return new ErrorReportingIterator(base.getAnother(), listener);
        }

        /**
         * Get properties of this iterator, as a bit-significant integer.
         *
         * @return the properties of this iterator. This will be some combination of
         *         properties such as {@link #GROUNDED}, {@link #LAST_POSITION_FINDER},
         *         and {@link #LOOKAHEAD}. It is always
         *         acceptable to return the value zero, indicating that there are no known special properties.
         *         It is acceptable for the properties of the iterator to change depending on its state.
         */

        public int getProperties() {
            return 0;
        }
    }

}

