using System;
using System.Collections;
using JConfiguration = net.sf.saxon.Configuration;
using JXPathEvaluator = net.sf.saxon.sxpath.XPathEvaluator;
using JItem = net.sf.saxon.om.Item;
using JSequenceExtent = net.sf.saxon.value.SequenceExtent;
using JValueRepresentation = net.sf.saxon.om.ValueRepresentation;
using JValue = net.sf.saxon.value.Value;
using DotNetIndependentContext = net.sf.saxon.dotnet.DotNetIndependentContext;
using JXPathExpression = net.sf.saxon.sxpath.XPathExpression;
using JExpression = net.sf.saxon.expr.Expression;
using JXPathContextMajor = net.sf.saxon.expr.XPathContextMajor;


namespace Saxon.Api {

    /// <summary>
    /// An XPathCompiler object allows XPath queries to be compiled.
    /// The compiler holds information that represents the static context
    /// for the expression.
    /// </summary>
    /// <remarks>
    /// <para>To construct an XPathCompiler, use the factory method
    /// <c>newXPathCompiler</c> on the <c>Processor</c> object.</para>
    /// <para>An XPathCompiler may be used repeatedly to compile multiple
    /// queries. Any changes made to the XPathCompiler (that is, to the
    /// static context) do not affect queries that have already been compiled.
    /// An XPathCompiler may be used concurrently in multiple threads, but
    /// it should not then be modified once initialized.</para>
    /// </remarks>

    [Serializable]
    public class XPathCompiler {

        private JConfiguration config;
        private DotNetIndependentContext env;

        // internal constructor: the public interface is a factory method
        // on the Processor object

        internal XPathCompiler(JConfiguration config) {
            this.config = config;
            this.env = new DotNetIndependentContext(config);
        }

        /// <summary>
        /// Declare a namespace for use by the XPath expression.
        /// </summary>
        /// <param name="prefix">The namespace prefix to be declared. Use
        /// a zero-length string to declare the default namespace (that is, the
        /// default namespace for elements and types).</param>
        /// <param name="uri">The namespace URI. It is possible to specify
        /// a zero-length string to "undeclare" a namespace.</param>

        public void DeclareNamespace(String prefix, String uri) {
            env.declareNamespace(prefix, uri);
        }

        /// <summary>
        /// Declare a variable for use by the XPath expression. If the expression
        /// refers to any variables, then they must be declared here.
        /// </summary>
        /// <param name="name">The name of the variable, as a <c>QName</c></param>


        public void DeclareVariable(QName name) {
            env.declareVariable(name.ToQNameValue());
        }

        /// <summary>
        /// The base URI of the expression, which forms part of the static context
        /// of the expression. This is used for resolving any relative URIs appearing
        /// within the expression, for example in references to library modules, schema
        /// locations, or as an argument to the <c>doc()</c> function.
        /// </summary>


        public String BaseUri {
            get { return env.getBaseURI(); }
            set { env.setBaseURI(value); }
        }


        /// <summary>
        /// Compile an expression supplied as a String.
        /// </summary>
        /// <example>
        /// <code>
        /// XPathExecutable q = compiler.Compile("distinct-values(//*/node-name()");
        /// </code>
        /// </example>
        /// <param name="source">A string containing the source text of the XPath expression</param>
        /// <returns>An <c>XPathExecutable</c> which represents the compiled xpath expression object.
        /// The XPathExecutable may be run as many times as required, in the same or a different
        /// thread. The <c>XPathExecutable</c> is not affected by any changes made to the <c>XPathCompiler</c>
        /// once it has been compiled.</returns>

        public XPathExecutable Compile(String source) {
            JXPathEvaluator eval = new JXPathEvaluator(config);
            eval.setStaticContext(env);
            JXPathExpression cexp = eval.createExpression(source);
            return new XPathExecutable(cexp, config, env);
        }
    }

    /// <summary>
    /// An <c>XPathExecutable</c> represents the compiled form of an XPath expression. 
    /// To evaluate the expression,
    /// it must first be loaded to form an <c>XPathSelector</c>.
    /// </summary>
    /// <remarks>
    /// <para>An <c>XPathExecutable</c> is immutable, and therefore thread-safe. It is simplest to
    /// load a new <c>XPathSelector</c> each time the expression is to be evaluated. However, the 
    /// <c>XPathSelector</c> is serially reusable within a single thread.</para>
    /// <para>An <c>XPathExecutable</c> is created by using one of the <c>Compile</c>
    /// methods on the <c>XPathCompiler</c> class.</para>
    /// </remarks>    

    [Serializable]
    public class XPathExecutable {

        private JXPathExpression exp;
        private JConfiguration config;
        private DotNetIndependentContext env;

        // internal constructor

        internal XPathExecutable(JXPathExpression exp, JConfiguration config, DotNetIndependentContext env) {
            this.exp = exp;
            this.config = config;
            this.env = env;
        }

        /// <summary>
        /// Load the compiled XPath expression to prepare it for execution.
        /// </summary>
        /// <returns>
        /// An <c>XPathSelector</c>. The returned <c>XPathSelector</c> can be used to
        /// set up the dynamic context, and then to evaluate the expression.
        /// </returns>

        public XPathSelector Load() {
            return new XPathSelector(exp, config, env);
        }
    }

    /// <summary inherits="IEnumerable">
    /// An <c>XPathSelector</c> represents a compiled and loaded XPath expression ready for execution.
    /// The <c>XPathSelector</c> holds details of the dynamic evaluation context for the XPath expression.
    /// </summary>
    /// <remarks>
    /// <para>An <c>XPathSelector</c> should not be used concurrently in multiple threads. It is safe,
    /// however, to reuse the object within a single thread to evaluate the same XPath expression several times.
    /// Evaluating the expression does not change the context that has been established.</para>
    /// <para>An <c>XPathSelector</c> is always constructed by running the <c>Load</c> method of
    /// an <c>XPathExecutable</c>.</para>
    /// </remarks>     

    [Serializable]
    public class XPathSelector : IEnumerable {

        private JExpression exp;
        private JItem contextItem;
        private JConfiguration config;
        private DotNetIndependentContext env;
        private JValueRepresentation[] variableValues;

        // internal constructor

        internal XPathSelector(JXPathExpression exp, JConfiguration config, DotNetIndependentContext env) {
            this.exp = exp.getInternalExpression();
            this.config = config;
            this.env = env;
            variableValues = new JValueRepresentation[env.getStackFrameMap().getNumberOfVariables()];
            // TODO: there are some reuse issues here if the compiler/static context has been reused
        }

        /// <summary>
        /// The context item for the XPath expression evaluation.
        /// </summary>
        /// <remarks> This may be either a node or an atomic
        /// value. Most commonly it will be a document node, which might be constructed
        /// using the <c>Build</c> method of the <c>DocumentBuilder</c> object.
        /// </remarks>

        public XdmItem ContextItem {
            get { return (XdmItem)XdmValue.Wrap(contextItem); }
            set { contextItem = (JItem)value.Unwrap(); }
        }

        /// <summary>
        /// Set the value of a variable
        /// </summary>

        public void SetVariable(QName name, XdmValue value) {
            int slot = env.getSlotNumber(name.ToQNameValue());
            if (slot == -1) {
                throw new ArgumentException("Variable has not been declared: " + name);
            }
            variableValues[slot] = value.Unwrap();
        }

        /// <summary>
        /// Evaluate the expression, returning the result as an <c>XdmValue</c> (that is,
        /// a sequence of nodes and/or atomic values).
        /// </summary>
        /// <remarks>
        /// Although a singleton result <i>may</i> be represented as an <c>XdmItem</c>, there is
        /// no guarantee that this will always be the case. If you know that the expression will return at
        /// most one node or atomic value, it is best to use the <c>EvaluateSingle</c> method, which 
        /// does guarantee that an <c>XdmItem</c> (or null) will be returned.
        /// </remarks>
        /// <returns>
        /// An <c>XdmValue</c> representing the results of the expression. 
        /// </returns>

        public XdmValue Evaluate() {
            JXPathContextMajor context = new JXPathContextMajor(contextItem, config);
            context.setStackFrame(env.getStackFrameMap(), variableValues);
            JValueRepresentation value = JSequenceExtent.makeSequenceExtent(exp.iterate(context));
            return XdmValue.Wrap(value);
        }

        /// <summary>
        /// Evaluate the XPath expression, returning the result as an <c>XdmItem</c> (that is,
        /// a single node or atomic value).
        /// </summary>
        /// <returns>
        /// An <c>XdmItem</c> representing the result of the expression, or null if the expression
        /// returns an empty sequence. If the expression returns a sequence of more than one item,
        /// any items after the first are ignored.
        /// </returns>


        public XdmItem EvaluateSingle() {
            JXPathContextMajor context = new JXPathContextMajor(contextItem, config);
            context.setStackFrame(env.getStackFrameMap(), variableValues);
            net.sf.saxon.om.Item i = exp.iterate(context).next();
            if (i == null) {
                return null;
            }
            return (XdmItem)XdmValue.Wrap(i);
        }

        /// <summary>
        /// Evaluate the expression, returning the result as an <c>IEnumerator</c> (that is,
        /// an enumerator over a sequence of nodes and/or atomic values).
        /// </summary>
        /// <returns>
        /// An enumerator over the sequence that represents the results of the expression.
        /// Each object in this sequence will be an instance of <c>XdmItem</c>. Note
        /// that the expression may be evaluated lazily, which means that a successful response
        /// from this method does not imply that the expression has executed successfully: failures
        /// may be reported later while retrieving items from the iterator. 
        /// </returns>

        public IEnumerator GetEnumerator() {
            JXPathContextMajor context = new JXPathContextMajor(contextItem, config);
            context.setStackFrame(env.getStackFrameMap(), variableValues);
            return new SequenceEnumerator(exp.iterate(context));
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
// The Original Code is: all this file.
//
// The Initial Developer of the Original Code is Michael H. Kay.
//
// Portions created by (your name) are Copyright (C) (your legal entity). All Rights Reserved.
//
// Contributor(s): none.
//