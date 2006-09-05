package net.sf.saxon.xpath;
import net.sf.saxon.Configuration;
import net.sf.saxon.event.Builder;
import net.sf.saxon.event.Stripper;
import net.sf.saxon.expr.Expression;
import net.sf.saxon.expr.ExpressionTool;
import net.sf.saxon.expr.XPathContextMajor;
import net.sf.saxon.instruct.SlotManager;
import net.sf.saxon.om.AllElementStripper;
import net.sf.saxon.om.Item;
import net.sf.saxon.om.NodeInfo;
import net.sf.saxon.om.SequenceIterator;
import net.sf.saxon.type.SchemaException;
import net.sf.saxon.type.Type;
import net.sf.saxon.value.Value;
import org.xml.sax.InputSource;

import javax.xml.namespace.NamespaceContext;
import javax.xml.namespace.QName;
import javax.xml.transform.Source;
import javax.xml.transform.stream.StreamSource;
import javax.xml.xpath.*;
import java.util.ArrayList;
import java.util.List;
import java.io.File;

/**
  * <p>XPathEvaluator provides a simple API for standalone XPath processing (that is,
  * executing XPath expressions in the absence of an XSLT stylesheet). It is an implementation
 * of the JAXP 1.3 XPath interface, with additional methods provided (a) for backwards
 * compatibility (b) to give extra control over the XPath evaluation, and (c) to support
 * XPath 2.0.</p>
  *
  * @author Michael H. Kay
  */

public class XPathEvaluator implements XPath {

    private Configuration config;
    private NodeInfo contextNode = null;
    private StandaloneContext staticContext;
    private boolean stripSpace = false;

    /**
     * Default constructor. Creates an XPathEvaluator with a default configuration and name pool.
     * The default Configuration is not schema-aware.
     */

    public XPathEvaluator() {
        this(new Configuration());
    }

    /**
     * Construct an XPathEvaluator with a specified configuration.
     * @param config the configuration to be used. If schema-aware XPath expressions are to be used,
     * this must be a SchemaAwareConfiguration.
     */
    public XPathEvaluator(Configuration config) {
        this.config = config;
        staticContext = new StandaloneContext(config);
    }

    /**
    * Construct an XPathEvaluator to process a particular source document. This is equivalent to
    * using the default constructor and immediately calling setSource().
    * @param source The source document (or a specific node within it).
    */

    public XPathEvaluator(Source source) throws net.sf.saxon.trans.XPathException {
        super();
        if (source instanceof NodeInfo) {
            config = ((NodeInfo)source).getDocumentRoot().getConfiguration();
        } else {
            config = new Configuration();
        }
        staticContext = new StandaloneContext(config);
        setSource(source);
    }

    /**
    * Indicate whether all whitespace text nodes in the source document are to be
    * removed. This option has no effect unless it is called before the call on setSource(),
    * and unless the Source supplied to setSource() is a SAXSource or StreamSource.
    * @param strip True if all whitespace text nodes are to be stripped from the source document,
    * false otherwise. The default if the method is not called is false.
    */

    public void setStripSpace(boolean strip) {
        stripSpace = strip;
    }

    /**
    * Supply the document against which XPath expressions are to be executed. This
    * method must be called before preparing or executing an XPath expression.
    * Setting a new source document clears all the namespaces that have been declared.
    * @param source Any javax.xml.transform.Source object representing the document against
    * which XPath expressions will be executed. Note that a Saxon {@link net.sf.saxon.om.DocumentInfo DocumentInfo}
     * (indeed any {@link net.sf.saxon.om.NodeInfo NodeInfo})
     * can be used as a Source. To use a third-party DOM Document as a source, create an instance of
     * {@link javax.xml.transform.dom.DOMSource DOMSource} to wrap it.
    *  <p>The Source object supplied also determines the initial setting
    * of the context item. In most cases the context node will be the root of the supplied document;
    * however, if a NodeInfo or DOMSource is supplied it can be any node in the document. </p>
    * @return the NodeInfo of the start node in the resulting document object.
    */

    public NodeInfo setSource(Source source) throws net.sf.saxon.trans.XPathException {
        Stripper stripper = null;
        if (stripSpace) {
            stripper = AllElementStripper.getInstance();
        }
        contextNode = Builder.build(source, stripper, config);
        return contextNode;
    }

    /**
    * Set the static context for compiling XPath expressions. This provides control over the
    * environment in which the expression is compiled, for example it allows namespace prefixes to
    * be declared, variables to be bound and functions to be defined. For most purposes, the static
    * context can be defined by providing and tailoring an instance of the StandaloneContext class.
    * Until this method is called, a default static context is used, in which no namespaces are defined
    * other than the standard ones (xml, xslt, and saxon), and no variables or functions (other than the
    * core XPath functions) are available.
    */

    public void setStaticContext(StandaloneContext context) {
        staticContext = context;
    }

    /**
    * Get the current static context
    */

    public StandaloneContext getStaticContext() {
        return staticContext;
    }

    /**
    * Prepare an XPath expression for subsequent evaluation.
    * @param expression The XPath expression to be evaluated, supplied as a string.
    * @return an XPathExpression object representing the prepared expression
    * @throws net.sf.saxon.trans.XPathException if the syntax of the expression is wrong, or if it references namespaces,
    * variables, or functions that have not been declared.
    */

    public XPathExpressionImpl createExpression(String expression) throws net.sf.saxon.trans.XPathException {
        Expression exp = ExpressionTool.make(expression, staticContext,0,-1,1);
        exp = exp.typeCheck(staticContext, Type.ITEM_TYPE);
        SlotManager map = staticContext.getConfiguration().makeSlotManager();
        ExpressionTool.allocateSlots(exp, 0, map);
        XPathExpressionImpl xpe = new XPathExpressionImpl(exp, config);
        xpe.setStackFrameMap(map);
        if (contextNode != null) {
            xpe.setContextNode(contextNode);
        }
        return xpe;
    }

    /**
    * Set the context node. This provides the context node for any expressions executed after this
    * method is called, including expressions that were prepared before it was called.
    * @param node The node to be used as the context node. This must
    * be a node within the context document (the document supplied using the setSource() method).
    * @throws NullPointerException if the argument is null
    * @throws IllegalArgumentException if the supplied node is not a node in the context document
    */

    public void setContextNode(NodeInfo node) {
        if (node==null) {
            throw new NullPointerException("Context node cannot be null");
        }
        contextNode = node;
    }

    /**
    * Prepare and execute an XPath expression, supplied as a string, and returning the results
    * as a List.
    * @param expression The XPath expression to be evaluated, supplied as a string.
    * @return The results of the expression, as a List. The List represents the sequence
    * of items returned by the expression. Each item in the list will either be an object
    *  representing a node, or a Java object representing an atomic value.
    * The types of Java object that may be included in the list, and the XML Schema data types that they
    * correspond to, are as follows:<p>
    * <ul>
    * <li>Boolean (xs:boolean)</li>
    * <li>String (xs:string)</li>
    * <li>BigDecimal (xs:decimal)</li>
    * <li>Long (xs:integer and its derived types)</li>
    * <li>Double (xs:double)</li>
    * <li>Float (xs:float)</li>
    * <li>Date (xs:date, xs:dateTime)</li>
    * </ul>
    */

    public List evaluate(String expression) throws net.sf.saxon.trans.XPathException {
        Expression exp = ExpressionTool.make(expression, staticContext,0,-1,1);
        exp = exp.typeCheck(staticContext, Type.ITEM_TYPE);
        SlotManager map = staticContext.getConfiguration().makeSlotManager();
        ExpressionTool.allocateSlots(exp, 0, map);
        XPathContextMajor context = new XPathContextMajor(contextNode, staticContext.getConfiguration());
        context.openStackFrame(map);
        SequenceIterator iterator = exp.iterate(context);
        ArrayList list = new ArrayList(20);
        while (true) {
            Item item = iterator.next();
            if (item == null) {
                return list;
            }
            list.add(Value.convert(item));
        }
    }

    public void reset() {
        contextNode = null;
        stripSpace = false;
        staticContext = new StandaloneContext(config);
    }

    /**
     * Set XPath 1.0 compatibility mode on or off (by default, it is false)
     * @param compatible true if XPath 1.0 compatibility mode is to be set to true, false
     * if it is to be set to false.
     */

    public void setBackwardsCompatible(boolean compatible) {
        staticContext.setBackwardsCompatibilityMode(true);
    }

    /**
     * Get the value of XPath 1.0 compatibility mode
     * @return true if XPath 1.0 compatibility mode is set
     */

    public boolean isBackwardsCompatible() {
        return staticContext.isInBackwardsCompatibleMode();
    }

    /**
     * Set the resolver for XPath variables
     * @param xPathVariableResolver
     */

    public void setXPathVariableResolver(XPathVariableResolver xPathVariableResolver) {
        staticContext.setXPathVariableResolver(xPathVariableResolver);
    }

    /**
     * Get the resolver for XPath variables
     * @return the resolver, if one has been set
     */
    public XPathVariableResolver getXPathVariableResolver() {
        return staticContext.getXPathVariableResolver();
    }

    /**
     * Set the resolver for XPath functions
     * @param xPathFunctionResolver
     */

    public void setXPathFunctionResolver(XPathFunctionResolver xPathFunctionResolver) {
        staticContext.setXPathFunctionResolver(xPathFunctionResolver);
    }

    /**
     * Get the resolver for XPath functions
     * @return the resolver, if one has been set
     */

    public XPathFunctionResolver getXPathFunctionResolver() {
        return staticContext.getXPathFunctionResolver();
    }

    /**
     * Set the namespace context to be used. This supplements any namespaces declared directly
     * using declareNamespace on the staticContext object
     * @param namespaceContext The namespace context
     */

    public void setNamespaceContext(NamespaceContext namespaceContext) {
        staticContext.setNamespaceContext(namespaceContext);
    }

    /**
     * Get the namespace context, if one has been set using {@link #setNamespaceContext}
     * @return the namespace context if set, or null otherwise
     */

    public NamespaceContext getNamespaceContext() {
        return staticContext.getNamespaceContext();
    }

    /**
     * Import a schema. This is possible only if the schema-aware version of Saxon is being used,
     * and if the Configuration is a SchemaAwareConfiguration. Having imported a schema, the types
     * defined in that schema become part of the static context.
     * @param source A Source object identifying the schema document to be loaded
     * @throws net.sf.saxon.type.SchemaException if the schema contained in this document is invalid
     * @throws UnsupportedOperationException if the configuration is not schema-aware
     */

    public void importSchema(Source source) throws SchemaException {
        staticContext.importSchema(source);
    }

    /**
     * Compile an XPath 2.0 expression
     * @param expr the XPath 2.0 expression to be compiled, as a string
     * @return the compiled form of the expression
     * @throws XPathExpressionException if there are any static errors in the expression.
     * Note that references to undeclared variables are not treated as static errors, because
     * variables are not pre-declared using this API.
     */
    public XPathExpression compile(String expr) throws XPathExpressionException {
        try {
            return createExpression(expr);
        } catch (net.sf.saxon.trans.XPathException e) {
            throw new XPathExpressionException(e);
        }
    }

    /**
     * Single-shot method to compile and execute an XPath 2.0 expression.
     * @param expr The XPath 2.0 expression to be compiled and executed
     * @param node The context node for evaluation of the expression
     * @param qName The type of result required. For details, see
     *  {@link XPathExpressionImpl#evaluate(Object, javax.xml.namespace.QName)}
     * @return the result of evaluating the expression, returned as described in
     *  {@link XPathExpressionImpl#evaluate(Object, javax.xml.namespace.QName)}
     * @throws XPathExpressionException if any static or dynamic error occurs
     * in evaluating the expression.
     */

    public Object evaluate(String expr, Object node, QName qName) throws XPathExpressionException {
        XPathExpression exp = compile(expr);
        return exp.evaluate(node, qName);
    }

    /**
     * Single-shot method to compile an execute an XPath 2.0 expression, returning
     * the result as a string.
     * @param expr The XPath 2.0 expression to be compiled and executed
     * @param node The context node for evaluation of the expression
     * @return the result of evaluating the expression, converted to a string as if
     * by calling the XPath string() function
     * @throws XPathExpressionException if any static or dynamic error occurs
     * in evaluating the expression.
     */

    public String evaluate(String expr, Object node) throws XPathExpressionException {
        XPathExpression exp = compile(expr);
        return exp.evaluate(node);
    }

    /**
     * Single-shot method to parse and build a source document, and
     * compile an execute an XPath 2.0 expression, against that document
     * @param expr The XPath 2.0 expression to be compiled and executed
     * @param inputSource The source document: this will be parsed and built into a tree,
     * and the XPath expression will be executed with the root node of the tree as the
     * context node
     * @param qName The type of result required. For details, see
     *  {@link XPathExpressionImpl#evaluate(Object, javax.xml.namespace.QName)}
     * @return the result of evaluating the expression, returned as described in
     *  {@link XPathExpressionImpl#evaluate(Object, javax.xml.namespace.QName)}
     * @throws XPathExpressionException if any static or dynamic error occurs
     * in evaluating the expression.
     */

    public Object evaluate(String expr, InputSource inputSource, QName qName) throws XPathExpressionException {
        XPathExpression exp = compile(expr);
        return exp.evaluate(inputSource, qName);
    }

    /**
     * Single-shot method to parse and build a source document, and
     * compile an execute an XPath 2.0 expression, against that document,
     * returning the result as a string
     * @param expr The XPath 2.0 expression to be compiled and executed
     * @param inputSource The source document: this will be parsed and built into a tree,
     * and the XPath expression will be executed with the root node of the tree as the
     * context node
     * @return the result of evaluating the expression, converted to a string as
     * if by calling the XPath string() function
     * @throws XPathExpressionException if any static or dynamic error occurs
     * in evaluating the expression.
     */

    public String evaluate(String expr, InputSource inputSource) throws XPathExpressionException {
        XPathExpression exp = compile(expr);
        return exp.evaluate(inputSource);
    }

    /**
    * Prepare and execute an XPath expression, supplied as a string, and returning the first
    * item in the result. This is useful where it is known that the expression will only return
    * a singleton value (for example, a single node, or a boolean).
    * @param expression The XPath expression to be evaluated, supplied as a string.
    * @return The first item in the sequence returned by the expression. If the expression
    * returns an empty sequence, this method returns null. Otherwise, it returns the first
    * item in the result sequence, represented as a Java object using the same mapping as for
    * the evaluate() method
    */

    public Object evaluateSingle(String expression) throws net.sf.saxon.trans.XPathException {
        Expression exp = ExpressionTool.make(expression, staticContext,0,-1,1);
        exp = exp.typeCheck(staticContext, Type.ITEM_TYPE);
        SlotManager map = staticContext.getConfiguration().makeSlotManager();
        ExpressionTool.allocateSlots(exp, 0, map);
        XPathContextMajor context = new XPathContextMajor(contextNode, staticContext.getConfiguration());
        context.openStackFrame(map);
        SequenceIterator iterator = exp.iterate(context);
        Item item = iterator.next();
        if (item == null) {
            return null;
        } else {
            return Value.convert(item);
        }
    }

    /**
     * A simple command-line interface for the XPathEvaluator (not documented).
     * First parameter is the filename containing the source document, second
     * parameter is the XPath expression.
     */

    public static void main(String[] args) throws Exception {
        if (args.length != 2) {
            System.err.println("format: java XPathEvaluator source.xml \"expression\"");
            return;
        }
        XPathEvaluator xpe = new XPathEvaluator(new StreamSource(new File(args[0])));
        List results = xpe.evaluate(args[1]);
        for (int i = 0; i < results.size(); i++) {
            Object o = results.get(i);
            System.err.println(o);
        }
//        Configuration config = new Configuration();
//        config.setLineNumbering(true);
//        XPathEvaluator xpath = new XPathEvaluator(config);
//        NodeInfo doca = xpath.setSource(new StreamSource((new File(args[0]))));
//        List list = xpath.evaluate("//*");
//        for (int i=0; i<list.size(); i++) {
//            NodeInfo element = (NodeInfo)list.get(i);
//            System.out.println("Element " + element.getDisplayName() + " at line " + element.getLineNumber());
//        }

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
// The Original Code is: all this file.
//
// The Initial Developer of the Original Code is Michael H. Kay
//
// Contributor(s):
//
