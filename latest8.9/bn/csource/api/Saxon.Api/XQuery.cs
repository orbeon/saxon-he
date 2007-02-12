using System;
using System.IO;
using System.Xml;
using System.Collections;
using JConfiguration = net.sf.saxon.Configuration;
using net.sf.saxon.om;
using net.sf.saxon.value;
using net.sf.saxon.query;
using net.sf.saxon.dotnet;
using JXPathException = net.sf.saxon.trans.XPathException;
using JStreamSource = javax.xml.transform.stream.StreamSource;


namespace Saxon.Api {

    /// <summary>
    /// An XQueryCompiler object allows XQuery queries to be compiled.
    /// </summary>
    /// <remarks>
    /// <para>To construct an <c>XQueryCompiler</c>, use the factory method
    /// <c>newXQueryCompiler</c> on the <c>Processor</c> object.</para>
    /// <para>The <c>XQueryCompiler</c> holds information that represents the static context
    /// for the queries that it compiles. This information remains intact after performing
    /// a compilation. An <c>XQueryCompiler</c> may therefore be used repeatedly to compile multiple
    /// queries. Any changes made to the <c>XQueryCompiler</c> (that is, to the
    /// static context) do not affect queries that have already been compiled.
    /// An <c>XQueryCompiler</c> may be used concurrently in multiple threads, but
    /// it should not then be modified once initialized.</para>
    /// 
    /// </remarks>

    [Serializable]
    public class XQueryCompiler {

        private JConfiguration config;
        private StaticQueryContext env;
        private IQueryResolver moduleResolver;
        private IList errorList;

        // internal constructor: the public interface is a factory method
        // on the Processor object

        internal XQueryCompiler(Processor processor) {
            this.config = processor.config;
            this.env = new StaticQueryContext(config);
            env.setModuleURIResolver(new DotNetStandardModuleURIResolver(config, processor.XmlResolver));
        }

        /// <summary>
        /// Declare a namespace for use by the query. This has the same
        /// status as a namespace appearing within the query prolog (though
        /// a declaration in the query prolog of the same prefix will take
        /// precedence)
        /// </summary>
        /// <param name="prefix">The namespace prefix to be declared. Use
        /// a zero-length string to declare the default namespace (that is, the
        /// default namespace for elements and types).</param>
        /// <param name="uri">The namespace URI. It is possible to specify
        /// a zero-length string to "undeclare" a namespace.</param>

        public void DeclareNamespace(String prefix, String uri) {
            env.declarePassiveNamespace(prefix, uri, false);
        }

        /// <summary>
        /// The base URI of the query, which forms part of the static context
        /// of the query. This is used for resolving any relative URIs appearing
        /// within the query, for example in references to library modules, schema
        /// locations, or as an argument to the <c>doc()</c> function.
        /// </summary>


        public String BaseUri {
            get { return env.getBaseURI(); }
            set { env.setBaseURI(value); }
        }

        /// <summary>
        /// A user-supplied <c>IQueryResolver</c> used to resolve location hints appearing in an
        /// <c>import module</c> declaration.
        /// </summary>
        /// <remarks>
        /// <para>In the absence of a user-supplied <c>QueryResolver</c>, an <c>import module</c> declaration
        /// is interpreted as follows. First, if the module URI identifies an already loaded module, that module
        /// is used and the location hints are ignored. Otherwise, each URI listed in the location hints is
        /// resolved using the <c>XmlResolver</c> registered with the <c>Processor</c>.</para>
        /// </remarks>

        public IQueryResolver QueryResolver {
            get { return moduleResolver; }
            set {
                moduleResolver = value;
                env.setModuleURIResolver(new DotNetModuleURIResolver(value));
            }
        }

        /// <summary>
        /// List of errors. The caller should supply an empty list before calling Compile;
        /// the processor will then populate the list with error information obtained during
        /// the compilation. Each error will be included as an object of type StaticError.
        /// If no error list is supplied by the caller, error information will be written to
        /// the standard error stream.
        /// </summary>
        /// <remarks>
        /// By supplying a custom List with a user-written add() method, it is possible to
        /// intercept error conditions as they occur.
        /// </remarks>

        public IList ErrorList {
            set {
                errorList = value;
                env.setErrorListener(new ErrorGatherer(value));
            }
            get {
                return errorList;
            }
        }

        /// <summary>
        /// Compile a query supplied as a Stream.
        /// </summary>
        /// <remarks>
        /// <para>The XQuery processor attempts to deduce the encoding of the query
        /// by looking for a byte-order-mark, or if none is present, by looking
        /// for the encoding declaration in the XQuery version declaration.
        /// For this to work, the stream must have the <c>CanSeek</c> property.
        /// If no encoding information is present, UTF-8 is assumed.</para>
        /// <para>The base URI of the query is set to the value of the <c>BaseUri</c>
        /// property. If this has not been set, then the base URI will be undefined, which
        /// means that any use of an expression that depends on the base URI will cause
        /// an error.</para>
        /// </remarks>
        /// <example>
        /// <code>
        /// XQueryExecutable q = compiler.Compile(new FileStream("input.xq", FileMode.Open, FileAccess.Read));
        /// </code>
        /// </example>
        /// <param name="query">A stream containing the source text of the query</param>
        /// <returns>An <c>XQueryExecutable</c> which represents the compiled query object.
        /// The XQueryExecutable may be run as many times as required, in the same or a different
        /// thread. The <c>XQueryExecutable</c> is not affected by any changes made to the <c>XQueryCompiler</c>
        /// once it has been compiled.</returns>
        /// <exception cref="StaticError">Throws a StaticError if errors were detected
        /// during static analysis of the query. Details of the errors will be added as StaticError
        /// objects to the ErrorList if supplied; otherwise they will be written to the standard
        /// error stream. The exception that is returned is merely a summary indicating the
        /// status.</exception>

        public XQueryExecutable Compile(Stream query) {
            try {
                XQueryExpression exp = env.compileQuery(new DotNetInputStream(query), null);
                return new XQueryExecutable(exp);
            } catch (JXPathException e) {
                throw new StaticError(e);
            }
        }

        /// <summary>
        /// Compile a query supplied as a String.
        /// </summary>
        /// <remarks>
        /// Using this method the query processor is provided with a string of Unicode
        /// characters, so no decoding is necessary. Any encoding information present in the
        /// version declaration is therefore ignored.
        /// </remarks>
        /// <example>
        /// <code>
        /// XQueryExecutable q = compiler.Compile("distinct-values(//*/node-name()");
        /// </code>
        /// </example>
        /// <param name="query">A string containing the source text of the query</param>
        /// <returns>An <c>XQueryExecutable</c> which represents the compiled query object.
        /// The XQueryExecutable may be run as many times as required, in the same or a different
        /// thread. The <c>XQueryExecutable</c> is not affected by any changes made to the <c>XQueryCompiler</c>
        /// once it has been compiled.</returns>
        /// <exception cref="StaticError">Throws a StaticError if errors were detected
        /// during static analysis of the query. Details of the errors will be added as StaticError
        /// objects to the ErrorList if supplied; otherwise they will be written to the standard
        /// error stream. The exception that is returned is merely a summary indicating the
        /// status.</exception>        

        public XQueryExecutable Compile(String query) {
            try {
                XQueryExpression exp = env.compileQuery(query);
                return new XQueryExecutable(exp);
            } catch (JXPathException e) {
                throw new StaticError(e);
            }
        }

        /// <summary>
        /// Escape hatch to the underying Java implementation
        /// </summary>

        public StaticQueryContext Implementation {
            get { return env; }
        }
    }

    /// <summary>
    /// An <c>XQueryExecutable</c> represents the compiled form of a query. To execute the query,
    /// it must first be loaded to form an <c>XQueryEvaluator</c>.
    /// </summary>
    /// <remarks>
    /// <para>An <c>XQueryExecutable</c> is immutable, and therefore thread-safe. It is simplest to
    /// load a new <c>XQueryEvaluator</c> each time the query is to be run. However, the 
    /// <c>XQueryEvaluator</c> is serially reusable within a single thread.</para>
    /// <para>An <c>XQueryExecutable</c> is created by using one of the <c>Compile</c>
    /// methods on the <c>XQueryCompiler</c> class.</para>
    /// </remarks>    

    [Serializable]
    public class XQueryExecutable {

        private XQueryExpression exp;

        // internal constructor

        internal XQueryExecutable(XQueryExpression exp) {
            this.exp = exp;
        }

        /// <summary>
        /// Load the query to prepare it for execution.
        /// </summary>
        /// <returns>
        /// An <c>XQueryEvaluator</c>. The returned <c>XQueryEvaluator</c> can be used to
        /// set up the dynamic context for query evaluation, and to run the query.
        /// </returns>

        public XQueryEvaluator Load() {
            return new XQueryEvaluator(exp);
        }
    }

    /// <summary inherits="IEnumerable">
    /// An <c>XQueryEvaluator</c> represents a compiled and loaded query ready for execution.
    /// The <c>XQueryEvaluator</c> holds details of the dynamic evaluation context for the query.
    /// </summary>
    /// <remarks>
    /// <para>An <c>XQueryEvaluator</c> should not be used concurrently in multiple threads. It is safe,
    /// however, to reuse the object within a single thread to run the same query several times.
    /// Running the query does not change the context that has been established.</para>
    /// <para>An <c>XQueryEvaluator</c> is always constructed by running the <c>Load</c> method of
    /// an <c>XQueryExecutable</c>.</para>
    /// </remarks>     

    [Serializable]
    public class XQueryEvaluator : IEnumerable {

        private XQueryExpression exp;
        private DynamicQueryContext context;

        // internal constructor

        internal XQueryEvaluator(XQueryExpression exp) {
            this.exp = exp;
            this.context =
                new DynamicQueryContext(exp.getStaticContext().getConfiguration());
        }

        /// <summary>
        /// The context item for the query.
        /// </summary>
        /// <remarks> This may be either a node or an atomic
        /// value. Most commonly it will be a document node, which might be constructed
        /// using the <c>LoadDocument</c> method of the <c>Processor</c> object.
        /// </remarks>

        public XdmItem ContextItem {
            get { return (XdmItem)XdmValue.Wrap(context.getContextItem()); }
            set { context.setContextItem((Item)value.Unwrap()); }
        }

        /// <summary>
        /// The <code>XmlResolver</code> to be used at run-time to resolve and dereference URIs
        /// supplied to the <c>doc()</c> function.
        /// </summary>

        public XmlResolver InputXmlResolver {
            get {
                return ((DotNetURIResolver)context.getURIResolver()).getXmlResolver();
            }
            set {
                context.setURIResolver(new DotNetURIResolver(value));
            }
        }

        /// <summary>
        /// Set the value of an external variable declared in the query.
        /// </summary>
        /// <param name="name">The name of the external variable, expressed
        /// as a QName. If an external variable of this name has been declared in the
        /// query prolog, the given value will be assigned to the variable. If the
        /// variable has not been declared, calling this method has no effect (it is
        /// not an error).</param>
        /// <param name="value">The value to be given to the external variable.
        /// If the variable declaration defines a required type for the variable, then
        /// this value must match the required type: no conversions are applied.</param>

        public void SetExternalVariable(QName name, XdmValue value) {
            context.setParameter(name.ClarkName, value.Unwrap());
        }

        /// <summary>
        /// Evaluate the query, returning the result as an <c>XdmValue</c> (that is,
        /// a sequence of nodes and/or atomic values).
        /// </summary>
        /// <returns>
        /// An <c>XdmValue</c> representing the results of the query
        /// </returns>
        /// <exception cref="DynamicError">Throws a DynamicError if any run-time failure
        /// occurs while evaluating the query.</exception>

        public XdmValue Evaluate() {
            try {
                ValueRepresentation value = SequenceExtent.makeSequenceExtent(exp.iterator(context));
                return XdmValue.Wrap(value);
            } catch (JXPathException err) {
                throw new DynamicError(err);
            }
        }

        /// <summary>
        /// Evaluate the query, returning the result as an <c>XdmItem</c> (that is,
        /// a single node or atomic value).
        /// </summary>
        /// <returns>
        /// An <c>XdmItem</c> representing the result of the query, or null if the query
        /// returns an empty sequence. If the query returns a sequence of more than one item,
        /// any items after the first are ignored.
        /// </returns>
        /// <exception cref="DynamicError">Throws a DynamicError if any run-time failure
        /// occurs while evaluating the expression.</exception>

        public XdmItem EvaluateSingle() {
            try {
                return (XdmItem)XdmValue.Wrap(exp.iterator(context).next());
            } catch (JXPathException err) {
                throw new DynamicError(err);
            }
        }

        /// <summary>
        /// Evaluate the query, returning the result as an <c>IEnumerator</c> (that is,
        /// an enumerator over a sequence of nodes and/or atomic values).
        /// </summary>
        /// <returns>
        /// An enumerator over the sequence that represents the results of the query.
        /// Each object in this sequence will be an instance of <c>XdmItem</c>. Note
        /// that the query may be evaluated lazily, which means that a successful response
        /// from this method does not imply that the query has executed successfully: failures
        /// may be reported later while retrieving items from the iterator. 
        /// </returns>
        /// <exception cref="DynamicError">Throws a DynamicError if any run-time failure
        /// occurs while evaluating the expression.</exception>
        
        public IEnumerator GetEnumerator() {
            try {
                return new SequenceEnumerator(exp.iterator(context));
            } catch (JXPathException err) {
                throw new DynamicError(err);
            }
        }

        /// <summary>
        /// Evaluate the query, sending the result to a specified destination.
        /// </summary>
        /// <param name="destination">
        /// The destination for the results of the query. The class <c>XmlDestination</c>
        /// is an abstraction that allows a number of different kinds of destination
        /// to be specified.
        /// </param>
        /// <exception cref="DynamicError">Throws a DynamicError if any run-time failure
        /// occurs while evaluating the expression.</exception>
        
        public void Run(XmlDestination destination) {
            try {
                exp.run(context, destination.GetResult(), destination.GetOutputProperties());
            } catch (JXPathException err) {
                throw new DynamicError(err);
            }
            destination.Close();
        }

    }


    /// <summary>
    /// Interface defining a user-supplied class used to retrieve XQUery library modules listed
    /// in an <c>import module</c> declaration in the query prolog.
    /// </summary>


    public interface IQueryResolver {

        /// <summary>
        /// Given a module URI and a set of location hints, return a set of query modules.
        /// </summary>
        /// <param name="moduleUri">The URI of the required library module as written in the
        /// <c>import module</c> declaration</param>
        /// <param name="baseUri">The base URI of the module containing the <c>import module</c>
        /// declaration</param>
        /// <param name="locationHints">The sequence of URIs (if any) listed as location hints
        /// in the <c>import module</c> declaration in the query prolog.</param>
        /// <returns>A set of absolute Uris identifying the query modules to be loaded. There is no requirement
        /// that these correspond one-to-one with the URIs defined in the <c>locationHints</c>. The 
        /// returned URIs will be dereferenced by calling the <c>GetEntity</c> method.
        /// </returns>

        Uri[] GetModules(String moduleUri, Uri baseUri, String[] locationHints);

        /// <summary>
        /// Dereference a URI returned by <c>GetModules</c> to retrieve a <c>Stream</c> containing
        /// the actual query text.
        /// </summary>
        /// <param name="absoluteUri">A URI returned by the <code>GetModules</code> method.</param>
        /// <returns>Either a <c>Stream</c> or a <c>String</c> containing the query text. 
        /// The supplied URI will be used as the base URI of the query module.</returns>

        Object GetEntity(Uri absoluteUri);

    }

    // internal class that wraps a (.NET) IQueryResolver to create a (Java) ModuleURIResolver

    internal class DotNetModuleURIResolver : net.sf.saxon.query.ModuleURIResolver {

        private IQueryResolver resolver;

        public DotNetModuleURIResolver(IQueryResolver resolver) {
            this.resolver = resolver;
        }

        public JStreamSource[] resolve(String moduleURI, String baseURI, String[] locations) {
            Uri baseU = (baseURI == null ? null : new Uri(baseURI));
            Uri[] modules = resolver.GetModules(moduleURI, baseU, locations);
            JStreamSource[] ss = new JStreamSource[modules.Length];
            for (int i = 0; i < ss.Length; i++) {
                ss[i] = new JStreamSource();
                ss[i].setSystemId(modules[i].ToString());
                Object query = resolver.GetEntity(modules[i]);
                if (query is Stream) {
                    ss[i].setInputStream(new DotNetInputStream((Stream)query));
                } else if (query is String) {
                    ss[i].setReader(new DotNetReader(new StringReader((String)query)));
                } else {
                    throw new ArgumentException("Invalid response from GetEntity()");
                }
            }
            return ss;
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