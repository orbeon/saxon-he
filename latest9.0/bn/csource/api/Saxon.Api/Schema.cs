using System;
using System.IO;
using System.Xml;
using System.Collections;
using javax.xml.transform;
using javax.xml.transform.stream;
//using JConfiguration = com.saxonica.validate.SchemaAwareConfiguration;
using JConfiguration = net.sf.saxon.Configuration;
using JReceiver = net.sf.saxon.@event.Receiver;
using net.sf.saxon;
using net.sf.saxon.om;
using net.sf.saxon.pull;
using net.sf.saxon.@event;
using net.sf.saxon.dotnet;
using net.sf.saxon.type;


namespace Saxon.Api {

    /// <summary>
    /// A <c>SchemaManager</c> is responsible for compiling schemas and
    /// maintaining a cache of compiled schemas that can be used for validating
    /// instance documents.
    /// </summary>
    /// <remarks>
    /// <para>To obtain a <c>SchemaManager</c>, use the 
    /// <c>SchemaManager</c> property of the <c>Processor</c> object.</para>
    /// <para>In a schema-aware Processor there is exactly one
    /// <c>SchemaManager</c> (in a non-schema-aware Processor there is none).</para>
    /// <para>The cache of compiled schema definitions can include only one schema
    /// component (for example a type, or an element declaration) with any given name.
    /// An attempt to compile two different schemas in the same namespace will usually
    /// therefore fail.</para>
    /// <para>As soon as a type definition or element declaration is used for the first
    /// time in a validation episode, it is marked as being "sealed": this prevents subsequent
    /// modifications to the component. Examples of modifications that are thereby disallowed
    /// include adding to the substitution group of an existing element declaration, adding subtypes
    /// to an existing type, or redefining components using &lt;xs:redefine&gt;</para>
    /// </remarks>

    [Serializable]
    public class SchemaManager {

        private JConfiguration config;
        private IList errorList = null;

        // internal constructor: the public interface is a factory method
        // on the Processor object

        internal SchemaManager(net.sf.saxon.Configuration config) {
            this.config = (JConfiguration)config;
        }

        /// <summary>
        /// The SchemaResolver is a user-supplied class used for resolving references to
        /// schema documents. It applies to references from one schema document to another
        /// appearing in <c>xs:import</c>, <c>xs:include</c>, and <c>xs:redefine</c>; to
        /// references from an instance document to a schema in <c>xsi:schemaLocation</c> and
        /// <c>xsi:noNamespaceSchemaLocation</c>, to <c>xsl:import-schema</c> in XSLT, and to
        /// the <c>import schema</c> declaration in XQuery.
        /// </summary>

        public SchemaResolver SchemaResolver {
            get {
                SchemaURIResolver r = config.getSchemaURIResolver();
                if (r is DotNetSchemaURIResolver) {
                    return ((DotNetSchemaURIResolver)r).resolver;
                } else {
                    return null;
                }
            }
            set {
                config.setSchemaURIResolver(new DotNetSchemaURIResolver(value));
            }
        }

        /// <summary>
        /// List of errors. The caller may supply an empty list before calling Compile;
        /// the processor will then populate the list with error information obtained during
        /// the schema compilation. Each error will be included as an object of type StaticError.
        /// If no error list is supplied by the caller, error information will be written to
        /// the standard error stream.
        /// </summary>
        /// <remarks>
        /// <para>By supplying a custom List with a user-written add() method, it is possible to
        /// intercept error conditions as they occur.</para>
        /// <para>Note that this error list is used only for errors detected during the compilation
        /// of the schema. It is not used for errors detected when using the schema to validate
        /// a source document.</para>
        /// </remarks>

        public IList ErrorList {
            set {
                errorList = value;
            }
            get {
                return errorList;
            }
        }

        /// <summary>
        /// Compile a schema supplied as a Stream. The resulting schema components are added
        /// to the cache.
        /// </summary>
        /// <param name="input">A stream containing the source text of the schema</param>
        /// <param name="baseUri">The base URI of the schema document, for resolving any references to other
        /// schema documents</param>        

        public void Compile(Stream input, Uri baseUri) {
            StreamSource ss = new StreamSource(new DotNetInputStream(input));
            ss.setSystemId(baseUri.ToString());
            if (errorList == null) {
                config.addSchemaSource(ss);
            } else {
                config.addSchemaSource(ss, new ErrorGatherer(errorList));
            }
        }

        /// <summary>
        /// Compile a schema, retrieving the source using a URI. The resulting schema components are added
        /// to the cache.
        /// </summary>
        /// <remarks>
        /// The document located via the URI is parsed using the <c>System.Xml</c> parser.
        /// </remarks>
        /// <param name="uri">The URI identifying the location where the schema document can be
        /// found</param>

        public void Compile(Uri uri) {
            StreamSource ss = new StreamSource(uri.ToString());
            if (errorList == null) {
                config.addSchemaSource(ss);
            } else {
                config.addSchemaSource(ss, new ErrorGatherer(errorList));
            }
        }

        /// <summary>
        /// Compile a schema, delivered using an XmlReader. The resulting schema components are added
        /// to the cache.
        /// </summary>
        /// <remarks>
        /// The <c>XmlReader</c> is responsible for parsing the document; this method builds a tree
        /// representation of the document (in an internal Saxon format) and compiles it.
        /// If the <c>XmlReader</c> is an <c>XmlTextReader</c>, Saxon will set its <c>Normalization</c>
        /// property to true, and will wrap it in a (non-validating) <c>XmlValidatingReader</c> to ensure
        /// that entity references are expanded.
        /// </remarks>

        public void Compile(XmlReader reader) {
            if (reader is XmlTextReader) {
                ((XmlTextReader)reader).Normalization = true;
                reader = new XmlValidatingReader(reader);
                ((XmlValidatingReader)reader).ValidationType = ValidationType.None;
            }
            PullProvider pp = new DotNetPullProvider(reader);
            pp.setPipelineConfiguration(config.makePipelineConfiguration());
            // pp = new PullTracer(pp);  /* diagnostics */
            PullSource ss = new PullSource(pp);
            ss.setSystemId(reader.BaseURI);
            if (errorList == null) {
                config.addSchemaSource(ss);
            } else {
                config.addSchemaSource(ss, new ErrorGatherer(errorList));
            }
        }

        /// <summary>
        /// Compile a schema document, located at an XdmNode. This may be a document node whose
        /// child is an <c>xs:schema</c> element, or it may be
        /// the <c>xs:schema</c> element itself. The resulting schema components are added
        /// to the cache.
        /// </summary>

        public void Compile(XdmNode node) {
            ErrorGatherer eg = null;
            if (errorList != null) {
                eg = new ErrorGatherer(errorList);
            }
            config.readInlineSchema((NodeInfo)node.value, null, eg);
        }

        /// <summary>
        /// Create a new <c>SchemaValidator</c>, which may be used for validating instance
        /// documents.
        /// </summary>
        /// <remarks>
        /// <para>The <c>SchemaValidator</c> uses the cache of schema components held by the
        /// <c>SchemaManager</c>. It may also add new components to this cache (for example,
        /// when the instance document references a schema using <c>xsi:schemaLocation</c>).
        /// It is also affected by changes to the schema cache that occur after the 
        /// <c>SchemaValidator</c> is created.</para>
        /// <para>When schema components are used for validating instance documents (or for compiling
        /// schema-aware queries and stylesheets) they are <i>sealed</i> to prevent subsequent modification.
        /// The modifications disallowed once a component is sealed include adding to the substitution group
        /// of an element declaration, adding subtypes derived by extension to an existing complex type, and
        /// use of <c>&lt;xs:redefine&gt;</c></para>
        /// </remarks>

        public SchemaValidator NewSchemaValidator() {
            return new SchemaValidator(config);
        }

    }

    /// <summary>
    /// A <c>SchemaValidator</c> is an object that is used for validating instance documents
    /// against a schema. The schema consists of the collection of schema components that are
    /// available within the schema cache maintained by the <c>SchemaManager</c>, together with
    /// any additional schema components located during the course of validation by means of an
    /// <c>xsl:schemaLocation</c> or <c>xsi:noNamespaceSchemaLocation</c> attribute within the
    /// instance document.
    /// </summary>
    /// <remarks>
    /// If validation fails, an exception is thrown. If validation succeeds, the validated
    /// document can optionally be written to a specified destination. This will be a copy of
    /// the original document, augmented with default values for absent elements and attributes,
    /// and carrying type annotations derived from the schema processing. Saxon does not deliver
    /// the full PSVI as described in the XML schema specifications, only the subset of the
    /// PSVI properties featured in the XDM data model.
    /// </remarks>    

    [Serializable]
    public class SchemaValidator {

        private JConfiguration config;
        private bool lax = false;
        private Source source;
        private XmlDestination destination;
        private IList errorList = null;

        // internal constructor

        internal SchemaValidator(JConfiguration config) {
            this.config = config;
        }

        /// <summary>
        /// The validation mode may be either strict or lax. The default is strict;
        /// this property is set to indicate that lax validation is required. With strict validation,
        /// validation fails if no element declaration can be located for the outermost element. With lax
        /// validation, the absence of an element declaration results in the content being considered valid.
        /// </summary>

        public bool IsLax {
            get { return lax; }
            set { lax = value; }
        }

        /// <summary>
        /// Supply the instance document to be validated in the form of a Stream
        /// </summary>
        /// <param name="source">A stream containing the XML document to be parsed
        /// and validated.</param>
        /// <param name="baseUri">The base URI to be used for resolving any relative
        /// references, for example a reference to an <c>xsi:schemaLocation</c></param>                  

        public void SetSource(Stream source, Uri baseUri) {
            StreamSource ss = new StreamSource(new DotNetInputStream(source));
            ss.setSystemId(baseUri.ToString());
            this.source = ss;
        }

        /// <summary>
        /// Supply the instance document to be validated in the form of a Uri reference
        /// </summary>
        /// <remarks>
        /// <para>The supplied node must be either a document node or an element node.
        /// If an element node is supplied, then the subtree rooted at this element is
        /// validated as if it were a complete document: that is, it must not only conform
        /// to the structure required of that element, but any referential constraints
        /// (keyref, IDREF) must be satisfied within that subtree.
        /// </para>
        /// </remarks>
        /// <param name="baseUri">URI of the document to be validated</param>                  

        public void SetSource(Uri baseUri) {
            StreamSource ss = new StreamSource(baseUri.ToString());
            this.source = ss;
        }

        /// <summary>
        /// Supply the instance document to be validated, in the form of an XmlReader.
        /// </summary>
        /// <remarks>
        /// The XmlReader is responsible for parsing the document; this method validates it.
        /// </remarks>
        /// <param name="reader">The <c>XmlReader</c> used to read and parse the instance
        /// document being validated. This is used as supplied. For conformance, use of a
        /// plain <c>XmlTextReader</c> is discouraged, because it does not expand entity
        /// references. This may cause validation failures.
        /// </param>

        public void SetSource(XmlReader reader) {
            PullProvider pp = new DotNetPullProvider(reader);
            pp.setPipelineConfiguration(config.makePipelineConfiguration());
            // pp = new PullTracer(pp);  /* diagnostics */
            PullSource psource = new PullSource(pp);
            psource.setSystemId(reader.BaseURI);
            this.source = psource;
        }

        /// <summary>
        /// Supply the instance document to be validated in the form of an XdmNode
        /// </summary>
        /// <remarks>
        /// <para>The supplied node must be either a document node or an element node.
        /// If an element node is supplied, then the subtree rooted at this element is
        /// validated as if it were a complete document: that is, it must not only conform
        /// to the structure required of that element, but any referential constraints
        /// (keyref, IDREF) must be satisfied within that subtree.
        /// </para>
        /// </remarks>
        /// <param name="source">The document or element node at the root of the tree
        /// to be validated</param>        

        public void SetSource(XdmNode source) {
            this.source = (NodeInfo)source.value;
        }

        /// <summary>
        /// Supply the destination to hold the validated document. If no destination
        /// is supplied, the validated document is discarded.
        /// </summary>
        /// <remarks>
        /// The destination differs from the source in that (a) default values of missing
        /// elements and attributes are supplied, and (b) the typed values of elements and
        /// attributes are available. However, typed values can only be accessed if the result
        /// is represented using the XDM data model, that is, if the destination is supplied
        /// as an XdmDestination.
        /// </remarks>

        public void SetDestination(XmlDestination destination) {
            this.destination = destination;
        }

        /// <summary>
        /// List of errors. The caller may supply an empty list before calling Compile;
        /// the processor will then populate the list with error information obtained during
        /// the schema compilation. Each error will be included as an object of type StaticError.
        /// If no error list is supplied by the caller, error information will be written to
        /// the standard error stream.
        /// </summary>
        /// <remarks>
        /// <para>By supplying a custom List with a user-written add() method, it is possible to
        /// intercept error conditions as they occur.</para>
        /// <para>Note that this error list is used only for errors detected while 
        /// using the schema to validate a source document. It is not used to report errors
        /// in the schema itself.</para>
        /// </remarks>

        public IList ErrorList {
            set {
                errorList = value;
            }
            get {
                return errorList;
            }
        }


        /// <summary>
        /// Run the validation of the supplied source document, optionally
        /// writing the validated document to the supplied destination.
        /// </summary>

        public void Run() {
            AugmentedSource aug = AugmentedSource.makeAugmentedSource(source);
            aug.setSchemaValidationMode(lax ? Validation.LAX : Validation.STRICT);
            JReceiver receiver;
            if (destination == null) {
                receiver = new Sink();
            } else if (destination is Serializer) {
                receiver = ((Serializer)destination).GetReceiver(config);
            } else {
                Result result = destination.GetResult();
                if (result is JReceiver) {
                    receiver = (JReceiver)result;
                } else {
                    throw new ArgumentException("Unknown type of destination");
                }
            }
            PipelineConfiguration pipe = config.makePipelineConfiguration();
            if (errorList != null) {
                pipe.setErrorListener(new ErrorGatherer(errorList));
            }
            new Sender(pipe).send(aug, receiver, true);
        }

    }


    /// <summary>
    /// The SchemaResolver is a user-supplied class used for resolving references to
    /// schema documents. It applies to references from one schema document to another
    /// appearing in <c>xs:import</c>, <c>xs:include</c>, and <c>xs:redefine</c>; to
    /// references from an instance document to a schema in <c>xsi:schemaLocation</c> and
    /// <c>xsi:noNamespaceSchemaLocation</c>, to <c>xsl:import-schema</c> in XSLT, and to
    /// the <c>import schema</c> declaration in XQuery.
    /// </summary>


    public interface SchemaResolver {

        /// <summary>
        /// Given a targetNamespace and a set of location hints, return a set of schema documents.
        /// </summary>
        /// <param name="targetNamespace">The target namespace of the required schema components</param>
        /// <param name="baseUri">The base URI of the module containing the reference to a schema document
        /// declaration</param>
        /// <param name="locationHints">The sequence of URIs (if any) listed as location hints.
        /// In most cases there will only be one; but the <c>import schema</c> declaration in
        /// XQuery permits several.</param>
        /// <returns>A set of absolute Uris identifying the query modules to be loaded. There is no requirement
        /// that these correspond one-to-one with the URIs defined in the <c>locationHints</c>. The 
        /// returned URIs will be dereferenced by calling the <c>GetEntity</c> method.
        /// </returns>

        Uri[] GetSchemaDocuments(String targetNamespace, Uri baseUri, String[] locationHints);

        /// <summary>
        /// Dereference a URI returned by <c>GetModules</c> to retrieve a <c>Stream</c> containing
        /// the actual XML schema document.
        /// </summary>
        /// <param name="absoluteUri">A URI returned by the <code>GetSchemaDocuments</code> method.</param>
        /// <returns>Either a <c>Stream</c> or a <c>String</c> containing the query text. 
        /// The supplied URI will be used as the base URI of the query module.</returns>

        Object GetEntity(Uri absoluteUri);

    }

    // internal class that wraps a (.NET) QueryResolver to create a (Java) SchemaURIResolver

    internal class DotNetSchemaURIResolver : net.sf.saxon.type.SchemaURIResolver {

        internal SchemaResolver resolver;

        public DotNetSchemaURIResolver(SchemaResolver resolver) {
            this.resolver = resolver;
        }

        public Source[] resolve(String targetNamespace, String baseURI, String[] locations) {
            Uri baseU = (baseURI == null ? null : new Uri(baseURI));
            Uri[] modules = resolver.GetSchemaDocuments(targetNamespace, baseU, locations);
            StreamSource[] ss = new StreamSource[modules.Length];
            for (int i = 0; i < ss.Length; i++) {
                ss[i] = new StreamSource();
                ss[i].setSystemId(modules[i].ToString());
                Object doc = resolver.GetEntity(modules[i]);
                if (doc is Stream) {
                    ss[i].setInputStream(new DotNetInputStream((Stream)doc));
                } else if (doc is String) {
                    ss[i].setReader(new DotNetReader(new StringReader((String)doc)));
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