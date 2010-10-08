using System;
using System.IO;
using System.Xml;
using System.Collections;
using System.Collections.Specialized;
using System.Reflection;
using System.Globalization;
using javax.xml.transform;
using javax.xml.transform.stream;
using JClass = java.lang.Class;
using JBoolean = java.lang.Boolean;
using JResult = javax.xml.transform.Result;
using JSource = javax.xml.transform.Source;
using JProperties = java.util.Properties;
using JBufferedReader = java.io.BufferedReader;
using JStringReader = java.io.StringReader;
using JConfiguration = net.sf.saxon.Configuration;
using JVersion = net.sf.saxon.Version;
using JDocumentWrapper = net.sf.saxon.dotnet.DocumentWrapper;
using JDotNetObjectModel = net.sf.saxon.dotnet.DotNetObjectModel;
using JNodeInfo = net.sf.saxon.om.NodeInfo;
using JDocumentInfo = net.sf.saxon.om.DocumentInfo;
//using JSingletonItem = net.sf.saxon.value.SingletonItem;
using JValue = net.sf.saxon.value.Value;
using JValueRepresentation = net.sf.saxon.om.ValueRepresentation;
using JPipelineConfiguration = net.sf.saxon.@event.PipelineConfiguration;
using JXPathContext = net.sf.saxon.expr.XPathContext;
using JSequenceExtent = net.sf.saxon.value.SequenceExtent;
using AugmentedSource = net.sf.saxon.AugmentedSource;
using StructuredQName = net.sf.saxon.om.StructuredQName;
using Whitespace = net.sf.saxon.value.Whitespace;
using StaticQueryContext = net.sf.saxon.query.StaticQueryContext;
using JReceiver = net.sf.saxon.@event.Receiver;
using JTreeReceiver = net.sf.saxon.@event.TreeReceiver;
using JNamespaceReducer = net.sf.saxon.@event.NamespaceReducer;
using net.sf.saxon.om;
using net.sf.saxon.pull;
using net.sf.saxon.dotnet;


namespace Saxon.Api
{

    /// <summary>
    /// The Processor class serves three purposes: it allows global Saxon configuration
    /// options to be set; it acts as a factory for generating XQuery, XPath, and XSLT
    /// compilers; and it owns certain shared resources such as the Saxon NamePool and 
    /// compiled schemas. This is the first object that a Saxon application should create. Once
    /// established, a Processor may be used in multiple threads.
    /// </summary>

    [Serializable]
    public class Processor
    {

        //Transformation data variables
        internal JConfiguration config;
        private SchemaManager schemaManager = null;

        /// <summary>
        /// Create a new Processor. This Processor will have capabilities that depend on the version
        /// of the software that has been loaded, and on the features that have been licensed.
        /// </summary>

        public Processor()
        {
            config = JConfiguration.newConfiguration();
            config.registerExternalObjectModel(new DotNetObjectModelDefinition());
            config.setProcessor(this);
        }

        /// <summary>
        /// Create a Processor.
        /// </summary>
        /// <param name="licensedEdition">Set to true if the Processor is to use a licensed edition of Saxon
        /// (that is, Saxon-PE or Saxon-EE). If true, the Processor will attempt to enable the capabilities
        /// of the licensed edition of Saxon, according to the version of the software that is loaded, and will
        /// verify the license key. If false, the Processor will load a default Configuration that gives restricted
        /// capability and does not require a license, regardless of which version of the software is actually being run.</param>

        public Processor(bool licensedEdition) 
            // newline needed by documentation stylesheet
            : this(licensedEdition, false) { }

        /// <summary>
        /// Create a Processor.
        /// </summary>
        /// <param name="licensedEdition">Set to true if the Processor is to use a licensed edition of Saxon
        /// (that is, Saxon-PE or Saxon-EE). If true, the Processor will attempt to enable the capabilities
        /// of the licensed edition of Saxon, according to the version of the software that is loaded, and will
        /// verify the license key. If false, the Processor will load a default Configuration that gives restricted
        /// capability and does not require a license, regardless of which version of the software is actually being run.</param>
        /// <param name="loadLocally">This option has no effect at this release.</param>

        public Processor(bool licensedEdition, bool loadLocally)
        {
            if (licensedEdition)
            {
                config = JConfiguration.newConfiguration();
                schemaManager = new SchemaManager(config);
            }
            else
            {
                config = new JConfiguration();
            }
            config.registerExternalObjectModel(new DotNetObjectModelDefinition());
            config.setProcessor(this);
        }

        /// <summary>
        /// Create a Processor, based on configuration information supplied in a configuration file.
        /// </summary>
        /// <remarks>
        /// Not fully supported in this release: for experimental use only.
        /// </remarks>
        /// <param name="configurationFile">A stream holding the text of the XML configuration file. Details of the file format
        /// can be found in the Saxon documentation.</param>
        /// 

        public Processor(Stream configurationFile)
        {
            StreamSource ss = new StreamSource(new DotNetInputStream(configurationFile));
            config = JConfiguration.readConfiguration(ss);
            config.registerExternalObjectModel(new DotNetObjectModelDefinition());
            config.setProcessor(this);
        }


        /// <summary>
        /// Get the full name of the Saxon product version implemented by this Processor
        /// </summary>

        public string ProductTitle
        {
            get { return JVersion.getProductTitle(); }
        }

        /// <summary>
        /// Get the Saxon product version number (for example, "9.2.0.2")
        /// </summary>

        public string ProductVersion
        {
            get { return JVersion.getProductVersion(); }
        }

        /// <summary>
        /// Get the Saxon product edition (for example, "EE" for Enterprise Edition)
        /// </summary>
        /// 

        public string Edition
        {
            get { return config.getEditionCode(); }
        }


        /// <summary>
        /// Indicates whether the Processor is schema-aware. In Saxon 9.2, returns true if using Saxon
        /// Enterprise Edition.
        /// </summary>
        /// <remarks>
        /// Deprecated since Saxon 9.2; schema-awareness is not directly a property of a Processor any more.
        /// Use the Edition property to determine which Saxon edition is in use.
        /// </remarks>

        public bool IsSchemaAware
        {
            get { return config.getEditionCode() == "EE"; }
        }

        /// <summary>
        /// Gets the SchemaManager for the Processor. Returns null
        /// if the Processor is not schema-aware.
        /// </summary>

        public SchemaManager SchemaManager
        {
            get { return schemaManager; }
        }

        /// <summary>
        /// An XmlResolver, which will be used while compiling and running queries, 
        /// XPath expressions, and stylesheets, if no other XmlResolver is nominated
        /// </summary>
        /// <remarks>
        /// <para>By default an <c>XmlUrlResolver</c> is used. This means that the responsibility
        /// for resolving and dereferencing URIs rests with the .NET platform, not with the
        /// GNU Classpath.</para>
        /// <para>When Saxon invokes a user-written <c>XmlResolver</c>, the <c>GetEntity</c> method
        /// may return any of: a <c>System.IO.Stream</c>; a <c>System.IO.TextReader</c>; or a
        /// <c>java.xml.transform.Source</c>.</para>
        /// </remarks>

        public XmlResolver XmlResolver
        {
            get
            {
                return ((DotNetURIResolver)config.getURIResolver()).getXmlResolver();
            }
            set
            {
                config.setURIResolver(new DotNetURIResolver(value));
            }
        }

        /// <summary>
        /// Create a new <c>DocumentBuilder</c>, which may be used to build XDM documents from
        /// a variety of sources.
        /// </summary>
        /// <returns>A new <c>DocumentBuilder</c></returns>

        public DocumentBuilder NewDocumentBuilder()
        {
            DocumentBuilder builder = new DocumentBuilder(this);
            builder.XmlResolver = XmlResolver;
            return builder;
        }

        /// <summary>
        /// Create a new XQueryCompiler, which may be used to compile XQuery queries.
        /// </summary>
        /// <remarks>
        /// The returned XQueryCompiler retains a live link to the Processor, and
        /// may be affected by subsequent changes to the Processor.
        /// </remarks>
        /// <returns>A new XQueryCompiler</returns>

        public XQueryCompiler NewXQueryCompiler()
        {
            return new XQueryCompiler(this);
        }

        /// <summary>
        /// Create a new XsltCompiler, which may be used to compile XSLT stylesheets.
        /// </summary>
        /// <remarks>
        /// The returned XsltCompiler retains a live link to the Processor, and
        /// may be affected by subsequent changes to the Processor.
        /// </remarks>
        /// <returns>A new XsltCompiler</returns>

        public XsltCompiler NewXsltCompiler()
        {
            return new XsltCompiler(config);
        }

        /// <summary>
        /// Create a new XPathCompiler, which may be used to compile XPath expressions.
        /// </summary>
        /// <remarks>
        /// The returned XPathCompiler retains a live link to the Processor, and
        /// may be affected by subsequent changes to the Processor.
        /// </remarks>
        /// <returns>A new XPathCompiler</returns>

        public XPathCompiler NewXPathCompiler()
        {
            return new XPathCompiler(config);
        }

        /// <summary>
        /// The XML version used in this <c>Processor</c> (for example, this determines what characters
        /// are permitted in a name)
        /// </summary>
        /// <remarks>
        /// The value must be 1.0 or 1.1, as a <c>decimal</c>. The default version is currently 1.0, but may
        /// change in the future.
        /// </remarks>

        public decimal XmlVersion
        {
            get
            {
                return (config.getXMLVersion() == JConfiguration.XML10 ? 1.0m : 1.1m);
            }
            set
            {
                if (value == 1.0m)
                {
                    config.setXMLVersion(JConfiguration.XML10);
                }
                else if (value == 1.1m)
                {
                    config.setXMLVersion(JConfiguration.XML11);
                }
                else
                {
                    throw new ArgumentException("Invalid XML version: " + value);
                }
            }
        }

        /// <summary>
        /// Register a named collection. A collection is identified by a URI (the collection URI),
        /// and its content is represented by an <c>IEnumerable</c> that enumerates the contents
        /// of the collection. The values delivered by this enumeration are Uri values, which 
        /// can be mapped to nodes using the registered <c>XmlResolver</c>.
        /// </summary>
        /// <param name="collectionUri">The URI used to identify the collection in a call
        /// of the XPath <c>collection()</c> function. The default collection is registered
        /// by supplying null as the value of this argument (this is the collection returned
        /// when the XPath <c>collection()</c> function is called with no arguments).</param> 
        /// <param name="contents">An enumerable object that represents the contents of the
        /// collection, as a sequence of document URIs. The enumerator returned by this
        /// IEnumerable object must return instances of the Uri class.</param>
        /// <remarks>
        /// <para>Collections should be stable: that is, two calls to retrieve the same collection URI
        /// should return the same sequence of document URIs. This requirement is imposed by the
        /// W3C specifications, but in the case of a user-defined collection it is not enforced by
        /// the Saxon product.</para>
        /// <para>A collection may be "unregistered" by providing null as the value of the
        /// contents argument. A collection may be replaced by specifying the URI of an existing
        /// collection.</para>
        /// <para>Collections registered with a processor are available to all queries and stylesheets
        /// running under the control of that processor. Collections should not normally be registered
        /// while queries and transformations are in progress.</para>
        /// </remarks>
        /// 

        public void RegisterCollection(Uri collectionUri, IEnumerable contents)
        {
            String u = (collectionUri == null ? null : collectionUri.ToString());
            DotNetCollectionURIResolver resolver =
                (DotNetCollectionURIResolver)config.getCollectionURIResolver();
            resolver.registerCollection(u, contents);
        }

        /// <summary>
        /// Register an extension function with the Processor
        /// </summary>
        /// <param name="function">
        /// An object that defines the extension function, including its name, arity, arguments types, and
        /// a reference to the class that implements the extension function call.
        /// </param>

        public void RegisterExtensionFunction(ExtensionFunctionDefinition function)
        {
            WrappedExtensionFunctionDefinition f = new WrappedExtensionFunctionDefinition(function);
            config.registerExtensionFunction(f);
        }

        /// <summary>
        /// Copy an XdmValue to an XmlDestination
        /// </summary>
        /// <remarks>
        /// This method can be used to copy any kind of <c>XdmValue</c> to any kind
        /// of <c>XdmDestination</c>. The supplied <c>XdmValue</c> is first converted
        /// to an XML document according to the rules of the XSLT/XQuery serialization
        /// specification (for example, if the <c>XdmValue</c> is a sequence of atomic
        /// values, they will be turned in a text node in which the values are converted
        /// to strings and separated by single spaces). The resulting document is then
        /// written to the supplied <c>XmlDestination</c>.</remarks>
        /// <param name="sequence">The value to be written</param>
        /// <param name="destination">The destination to which the value should be written</param>
        /// 

        public void WriteXdmValue(XdmValue sequence, XmlDestination destination)
        {
            JResult result = destination.GetResult();
            JReceiver r = config.getSerializerFactory().getReceiver(result,
                config.makePipelineConfiguration(), new JProperties());
            r = new JNamespaceReducer(r);
            JTreeReceiver tree = new JTreeReceiver(r);
            tree.open();
            tree.startDocument(0);
            foreach (XdmItem it in sequence)
            {
                tree.append((Item)it.Unwrap(), 0, JNodeInfo.__Fields.ALL_NAMESPACES);
            }
            tree.endDocument();
            tree.close();
        }


        /// <summary>
        /// The underlying Configuration object in the Saxon implementation
        /// </summary>
        /// <remarks>
        /// <para>This property provides access to internal methods in the Saxon engine that are
        /// not specifically exposed in the .NET API. In general these methods should be
        /// considered to be less stable than the classes in the Saxon.Api namespace.</para> 
        /// <para>The internal methods follow
        /// Java naming conventions rather than .NET conventions.</para>
        /// <para>Information about the returned object (and the objects it provides access to)
        /// is included in the Saxon JavaDoc docmentation, available 
        /// <link href="http://www.saxonica.com/documentation/javadoc/index.html">online</link>.
        /// </para>
        /// </remarks>

        public net.sf.saxon.Configuration Implementation
        {
            get { return config; }
        }

        /// <summary>
        /// Set a configuration property
        /// </summary>
        /// <remarks>
        /// <para>This method provides the ability to set named properties of the configuration.
        /// The property names are set as strings, whose values can be found in the Java
        /// class <c>net.sf.saxon.FeatureKeys</c>. The property values are always strings. 
        /// Properties whose values are other types are not available via this interface:
        /// however all properties have an effective equivalent whose value is a string.
        /// Note that on/off properties are set using the strings "true" and "false".</para>
        /// <para><i>Method added in Saxon 9.1</i></para>
        /// </remarks>
        /// <param name="name">The property name</param>
        /// <param name="value">The property value</param>

        public void SetProperty(String name, String value)
        {
            config.setConfigurationProperty(name, value);
        }

        /// <summary>
        /// Get the value of a configuration property
        /// </summary>
        /// <remarks>
        /// <para>This method provides the ability to get named properties of the configuration.
        /// The property names are supplied as strings, whose values can be found in the Java
        /// class <c>net.sf.saxon.FeatureKeys</c>. The property values are always returned as strings. 
        /// Properties whose values are other types are returned by converting the value to a string.
        /// Note that on/off properties are returned using the strings "true" and "false".</para>
        /// <para><i>Method added in Saxon 9.1</i></para>
        /// </remarks>
        /// <param name="name">The property name</param>
        /// <returns>The property value, as a string; or null if the property is unset.</returns>

        public String GetProperty(String name)
        {
            Object obj = config.getConfigurationProperty(name);
            return (obj == null ? null : obj.ToString());
        }

    }

    /// <summary>
    /// The <c>DocumentBuilder</c> class enables XDM documents to be built from various sources.
    /// The class is always instantiated using the <c>NewDocumentBuilder</c> method
    /// on the <c>Processor</c> object.
    /// </summary>

    [Serializable]
    public class DocumentBuilder
    {

        private Processor processor;
        private JConfiguration config;
        private XmlResolver xmlResolver;
        private SchemaValidationMode validation;
        private bool dtdValidation;
        private bool lineNumbering;
        private WhitespacePolicy whitespacePolicy;
        private Uri baseUri;
        private QName topLevelElement;
        private TreeModel treeModel = TreeModel.Unspecified;

        internal DocumentBuilder(Processor processor)
        {
            this.processor = processor;
            this.config = processor.Implementation;
            this.xmlResolver = new XmlUrlResolver();
        }

        /// <summary>
        /// An XmlResolver, which will be used to resolve URIs of documents being loaded
        /// and of references to external entities within those documents (including any external DTD).
        /// </summary>
        /// <remarks>
        /// <para>By default an <c>XmlUrlResolver</c> is used. This means that the responsibility
        /// for resolving and dereferencing URIs rests with the .NET platform (and not with the
        /// GNU Classpath).</para>
        /// <para>When Saxon invokes a user-written <c>XmlResolver</c>, the <c>GetEntity</c> method
        /// may return any of: a <c>System.IO.Stream</c>; a <c>System.IO.TextReader</c>; or a
        /// <c>java.xml.transform.Source</c>. However, if the <c>XmlResolver</c> is called
        /// by the XML parser to resolve external entity references, then it must return an 
        /// instance of <c>System.IO.Stream</c>.</para>
        /// </remarks>

        public XmlResolver XmlResolver
        {
            get
            {
                return xmlResolver;
            }
            set
            {
                xmlResolver = value;
            }
        }

        /// <summary>
        /// Determines whether line numbering is enabled for documents loaded using this
        /// <c>DocumentBuilder</c>.
        /// </summary>
        /// <remarks>
        /// <para>By default, line numbering is disabled.</para>
        /// <para>Line numbering is not available for all kinds of source: in particular,
        /// it is not available when loading from an existing XmlDocument.</para>
        /// <para>The resulting line numbers are accessible to applications using the
        /// extension function saxon:line-number() applied to a node.</para>  
        /// <para>Line numbers are maintained only for element nodes; the line number
        /// returned for any other node will be that of the most recent element.</para> 
        /// </remarks>

        public bool IsLineNumbering
        {
            get
            {
                return lineNumbering;
            }
            set
            {
                lineNumbering = value;
            }
        }

        /// <summary>
        /// Determines whether schema validation is applied to documents loaded using this
        /// <c>DocumentBuilder</c>, and if so, whether it is strict or lax.
        /// </summary>
        /// <remarks>
        /// <para>By default, no schema validation takes place.</para>
        /// <para>This option requires the schema-aware version of the Saxon product (Saxon-SA).</para>
        /// </remarks>

        public SchemaValidationMode SchemaValidationMode
        {
            get
            {
                return validation;
            }
            set
            {
                validation = value;
            }
        }

        /// <summary>
        /// The required name of the top level element in a document instance being validated
        /// against a schema.
        /// </summary>
        /// <remarks>
        /// <para>If this property is set, and if schema validation is requested, then validation will
        /// fail unless the outermost element of the document has the required name.</para>
        /// <para>This option requires the schema-aware version of the Saxon product (Saxon-SA).</para>
        /// </remarks> 

        public QName TopLevelElementName
        {
            get
            {
                return topLevelElement;
            }
            set
            {
                topLevelElement = value;
            }
        }

        /// <summary>
        /// Determines whether DTD validation is applied to documents loaded using this
        /// <c>DocumentBuilder</c>.
        /// </summary>
        /// <remarks>
        ///
        /// <para>By default, no DTD validation takes place.</para>
        /// 
        /// </remarks>

        public bool DtdValidation
        {
            get
            {
                return dtdValidation;
            }
            set
            {
                dtdValidation = value;
            }
        }

        /// <summary>
        /// Determines the whitespace stripping policy applied when loading a document
        /// using this <c>DocumentBuilder</c>.
        /// </summary>
        /// <remarks>
        /// <para>By default, whitespace text nodes appearing in element-only content
        /// are stripped, and all other whitespace text nodes are retained.</para>
        /// </remarks>

        public WhitespacePolicy WhitespacePolicy
        {
            get
            {
                return whitespacePolicy;
            }
            set
            {
                whitespacePolicy = value;
            }
        }

        ///<summary>
        /// The Tree Model implementation to be used for the constructed document. By default
        /// the TinyTree is used. The main reason for using the LinkedTree alternative is if
        /// updating is required (the TinyTree is not updateable).
        ///</summary>

        public TreeModel TreeModel
        {
            get
            {
                return treeModel;
            }
            set
            {
                treeModel = value;
            }
        }

        /// <summary>
        /// The base URI of a document loaded using this <c>DocumentBuilder</c>.
        /// This is used for resolving any relative URIs appearing
        /// within the document, for example in references to DTDs and external entities.
        /// </summary>
        /// <remarks>
        /// This information is required when the document is loaded from a source that does not
        /// provide an intrinsic URI, notably when loading from a Stream or a TextReader.
        /// </remarks>


        public Uri BaseUri
        {
            get { return baseUri; }
            set { baseUri = value; }
        }

        /// <summary>
        /// Load an XML document, retrieving it via a URI.
        /// </summary>
        /// <remarks>
        /// <para>Note that the type <c>Uri</c> requires an absolute URI.</para>
        /// <para>The URI is dereferenced using the registered <c>XmlResolver</c>.</para>
        /// <para>This method takes no account of any fragment part in the URI.</para>
        /// <para>The <c>role</c> passed to the <c>GetEntity</c> method of the <c>XmlResolver</c> 
        /// is "application/xml", and the required return type is <c>System.IO.Stream</c>.</para>
        /// <para>The document located via the URI is parsed using the <c>System.Xml</c> parser.</para>
        /// <para>Note that the Microsoft <c>System.Xml</c> parser does not report whether attributes are
        /// defined in the DTD as being of type <c>ID</c> and <c>IDREF</c>. This is true whether or not
        /// DTD-based validation is enabled. This means that such attributes are not accessible to the 
        /// <c>id()</c> and <c>idref()</c> functions.</para>
        /// </remarks>
        /// <param name="uri">The URI identifying the location where the document can be
        /// found. This will also be used as the base URI of the document (regardless
        /// of the setting of the <c>BaseUri</c> property).</param>
        /// <returns>An <c>XdmNode</c>. This will be
        ///  the document node at the root of the tree of the resulting in-memory document. 
        /// </returns>

        public XdmNode Build(Uri uri)
        {
            Object obj = XmlResolver.GetEntity(uri, "application/xml", Type.GetType("System.IO.Stream"));
            if (obj is Stream)
            {
                try
                {
                    return Build((Stream)obj, uri);
                }
                finally
                {
                    ((Stream)obj).Close();
                }
            }
            else
            {
                throw new ArgumentException("Invalid type of result from XmlResolver.GetEntity: " + obj);
            }
        }

        /// <summary>
        /// Load an XML document supplied as raw (lexical) XML on a Stream.
        /// </summary>
        /// <remarks>
        /// <para>The document is parsed using the <c>System.Xml</c> parser.</para>
        /// <para>Before calling this method, the BaseUri property must be set to identify the
        /// base URI of this document, used for resolving any relative URIs contained within it.</para>
        /// <para>Note that the Microsoft <c>System.Xml</c> parser does not report whether attributes are
        /// defined in the DTD as being of type <c>ID</c> and <c>IDREF</c>. This is true whether or not
        /// DTD-based validation is enabled. This means that such attributes are not accessible to the 
        /// <c>id()</c> and <c>idref()</c> functions.</para>         
        /// </remarks>
        /// <param name="input">The Stream containing the XML source to be parsed</param>
        /// <returns>An <c>XdmNode</c>, the document node at the root of the tree of the resulting
        /// in-memory document
        /// </returns>

        public XdmNode Build(Stream input)
        {
            if (baseUri == null)
            {
                throw new ArgumentException("No base URI supplied");
            }
            return Build(input, baseUri);
        }

        // Build a document from a given stream, with the base URI supplied
        // as an extra argument

        internal XdmNode Build(Stream input, Uri baseUri)
        {
            Source source;
            if (processor.GetProperty("http://saxon.sf.net/feature/preferJaxpParser") == "true")
            {
                source = new StreamSource(new DotNetInputStream(input), baseUri.ToString());
                source = AugmentedSource.makeAugmentedSource(source);
                ((AugmentedSource)source).setEntityResolver(new DotNetURIResolver(XmlResolver));
            }
            else
            {

                XmlReaderSettings settings = new XmlReaderSettings();
                settings.ProhibitDtd = false;   // must expand entity references

                
                //((XmlTextReader)parser).Normalization = true;
                switch (whitespacePolicy)
                {
                    case WhitespacePolicy.PreserveAll:
                        settings.IgnoreWhitespace = false;
                        //((XmlTextReader)parser).WhitespaceHandling = WhitespaceHandling.All;
                        break;
                    case WhitespacePolicy.StripAll:
                        settings.IgnoreWhitespace = true;
                        //((XmlTextReader)parser).WhitespaceHandling = WhitespaceHandling.None;
                        break;
                    case WhitespacePolicy.StripIgnorable:
                        settings.IgnoreWhitespace = true;
                        //((XmlTextReader)parser).WhitespaceHandling = WhitespaceHandling.Significant;
                        break;
                }
                if (xmlResolver != null)
                {
                    settings.XmlResolver = xmlResolver;
                }
                
                settings.ValidationType = (dtdValidation ? ValidationType.DTD : ValidationType.None);
                
                XmlReader parser = XmlReader.Create(input, settings, baseUri.ToString());
                source = new PullSource(new DotNetPullProvider(parser));
                source.setSystemId(baseUri.ToString());
            }
            source = augmentSource(source);
            DocumentInfo doc = config.buildDocument(source);
            return (XdmNode)XdmValue.Wrap(doc);
        }

        private Source augmentSource(Source source)
        {
            if (validation != SchemaValidationMode.None)
            {
                source = AugmentedSource.makeAugmentedSource(source);
                if (validation == SchemaValidationMode.Strict)
                {
                    ((AugmentedSource)source).setSchemaValidationMode(Validation.STRICT);
                }
                else if (validation == SchemaValidationMode.Lax)
                {
                    ((AugmentedSource)source).setSchemaValidationMode(Validation.LAX);
                }
                else if (validation == SchemaValidationMode.None)
                {
                    ((AugmentedSource)source).setSchemaValidationMode(Validation.STRIP);
                }
                else if (validation == SchemaValidationMode.Preserve)
                {
                    ((AugmentedSource)source).setSchemaValidationMode(Validation.PRESERVE);
                }
            }
            if (topLevelElement != null)
            {
                source = AugmentedSource.makeAugmentedSource(source);
                ((AugmentedSource)source).setTopLevelElement(
                    new StructuredQName(
                        topLevelElement.Prefix, topLevelElement.Uri, topLevelElement.LocalName));
            }

            if (whitespacePolicy != WhitespacePolicy.PreserveAll)
            {
                source = AugmentedSource.makeAugmentedSource(source);
                if (whitespacePolicy == WhitespacePolicy.StripIgnorable)
                {
                    ((AugmentedSource)source).setStripSpace(Whitespace.IGNORABLE);
                }
                else
                {
                    ((AugmentedSource)source).setStripSpace(Whitespace.ALL);
                }
            }
            if (treeModel != TreeModel.Unspecified)
            {
                source = AugmentedSource.makeAugmentedSource(source);
                if (treeModel == TreeModel.TinyTree)
                {
                    ((AugmentedSource)source).setModel(net.sf.saxon.om.TreeModel.TINY_TREE);
                }
                else if (treeModel == TreeModel.TinyTreeCondensed)
                {
                    ((AugmentedSource)source).setModel(net.sf.saxon.om.TreeModel.TINY_TREE_CONDENSED);
                }
                else
                {
                    ((AugmentedSource)source).setModel(net.sf.saxon.om.TreeModel.LINKED_TREE);
                }
            }
            if (lineNumbering)
            {
                source = AugmentedSource.makeAugmentedSource(source);
                ((AugmentedSource)source).setLineNumbering(true);
            }
            if (dtdValidation)
            {
                source = AugmentedSource.makeAugmentedSource(source);
                ((AugmentedSource)source).setDTDValidationMode(Validation.STRICT);
            }
            return source;
        }

        /// <summary>
        /// Load an XML document, delivered using an XmlReader.
        /// </summary>
        /// <remarks>
        /// <para>The XmlReader is responsible for parsing the document; this method builds a tree
        /// representation of the document (in an internal Saxon format) and returns its document node.
        /// The XmlReader is not required to perform validation but it must expand any entity references.
        /// Saxon uses the properties of the <c>XmlReader</c> as supplied.</para>
        /// <para>Use of a plain <c>XmlTextReader</c> is discouraged, because it does not expand entity
        /// references. This should only be used if you know in advance that the document will contain
        /// no entity references (or perhaps if your query or stylesheet is not interested in the content
        /// of text and attribute nodes). Instead, with .NET 1.1 use an <c>XmlValidatingReader</c> (with <c>ValidationType</c>
        /// set to <c>None</c>). The constructor for <c>XmlValidatingReader</c> is obsolete in .NET 2.0,
        /// but the same effect can be achieved by using the <c>Create</c> method of <c>XmlReader</c> with
        /// appropriate <c>XmlReaderSettings</c></para>
        /// <para>Conformance with the W3C specifications requires that the <c>Normalization</c> property
        /// of an <c>XmlTextReader</c> should be set to <c>true</c>. However, Saxon does not insist
        /// on this.</para>
        /// <para>If the <c>XmlReader</c> performs schema validation, Saxon will ignore any resulting type
        /// information. Type information can only be obtained by using Saxon's own schema validator, which
        /// will be run if the <c>SchemaValidationMode</c> property is set to <c>Strict</c> or <c>Lax</c></para>
        /// <para>Note that the Microsoft <c>System.Xml</c> parser does not report whether attributes are
        /// defined in the DTD as being of type <c>ID</c> and <c>IDREF</c>. This is true whether or not
        /// DTD-based validation is enabled. This means that such attributes are not accessible to the 
        /// <c>id()</c> and <c>idref()</c> functions.</para>
        /// <para>Note that setting the <c>XmlResolver</c> property of the <c>DocumentBuilder</c>
        /// has no effect when this method is used; if an <c>XmlResolver</c> is required, it must
        /// be set on the <c>XmlReader</c> itself.</para>
        /// </remarks>
        /// <param name="reader">The XMLReader that supplies the parsed XML source</param>
        /// <returns>An <c>XdmNode</c>, the document node at the root of the tree of the resulting
        /// in-memory document
        /// </returns>

        public XdmNode Build(XmlReader reader)
        {
            PullProvider pp = new DotNetPullProvider(reader);
            pp.setPipelineConfiguration(config.makePipelineConfiguration());
            // pp = new PullTracer(pp);  /* diagnostics */
            Source source = new PullSource(pp);
            source.setSystemId(reader.BaseURI);
            source = augmentSource(source);
            DocumentInfo doc = config.buildDocument(source);
            return (XdmNode)XdmValue.Wrap(doc);
        }

        /// <summary>
        /// Load an XML DOM document, supplied as an <c>XmlNode</c>, into a Saxon XdmNode.
        /// </summary>
        /// <remarks>
        /// <para>
        /// The returned document will contain only the subtree rooted at the supplied node.
        /// </para>
        /// <para>
        /// This method copies the DOM tree to create a Saxon tree. See the <c>Wrap</c> method for
        /// an alternative that creates a wrapper the DOM tree, allowing it to be modified in situ.
        /// </para>
        /// </remarks>
        /// <param name="source">The DOM Node to be copied to form a Saxon tree</param>
        /// <returns>An <c>XdmNode</c>, the document node at the root of the tree of the resulting
        /// in-memory document
        /// </returns>

        public XdmNode Build(XmlNode source)
        {
            return Build(new XmlNodeReader(source));
        }

        /// <summary>
        /// Wrap an XML DOM document, supplied as an <c>XmlNode</c>, as a Saxon XdmNode.
        /// </summary>
        /// <remarks>
        /// <para>
        /// This method must be applied at the level of the Document Node. Unlike the
        /// <c>Build</c> method, the original DOM is not copied. This saves memory and
        /// time, but it also means that it is not possible to perform operations such as
        /// whitespace stripping and schema validation.
        /// </para>
        /// </remarks>
        /// <param name="doc">The DOM document node to be wrapped</param>
        /// <returns>An <c>XdmNode</c>, the Saxon document node at the root of the tree of the resulting
        /// in-memory document
        /// </returns>

        public XdmNode Wrap(XmlDocument doc)
        {
            String baseu = (baseUri == null ? null : baseUri.ToString());
            JDocumentWrapper wrapper = new JDocumentWrapper(doc, baseu, config);
            return (XdmNode)XdmValue.Wrap(wrapper);
        }


    }


    /// <summary>
    /// Enumeration identifying the various Schema validation modes
    /// </summary>

    public enum SchemaValidationMode
    {
        /// <summary>No validation (or strip validation, which removes existing type annotations)</summary> 
        None,
        /// <summary>Strict validation</summary>
        Strict,
        /// <summary>Lax validation</summary>
        Lax,
        /// <summary>Validation mode preserve, which preserves any existing type annotations</summary>
        Preserve,
        /// <summary>Unspecified validation: this means that validation is defined elsewhere, for example in the
        /// Saxon Configuration</summary>
        Unspecified
    }

    /// <summary>
    /// Enumeration identifying the various Whitespace stripping policies
    /// </summary>

    public enum WhitespacePolicy
    {
        /// <summary>No whitespace is stripped</summary> 
        PreserveAll,
        /// <summary>Whitespace text nodes appearing in element-only content are stripped</summary>
        StripIgnorable,
        /// <summary>All whitespace text nodes are stripped</summary>
        StripAll
    }

    /// <summary>
    /// Enumeration identifying the different tree model implementations
    /// </summary>
    /// 
    public enum TreeModel
    {
        /// <summary>
        /// Saxon TinyTree
        /// </summary>
        TinyTree,
        /// <summary>
        /// Saxon Condensed TinyTree (shares storage for duplicated text and attribute nodes)
        /// </summary>
        TinyTreeCondensed,
        /// <summary>
        /// Saxon LinkedTree
        /// </summary>
        LinkedTree,
        /// <summary>
        /// Unspecified tree model
        /// </summary>
        Unspecified
    }

    internal class DotNetObjectModelDefinition : JDotNetObjectModel
    {

        public override bool isXdmValue(object obj)
        {
            return obj is XdmValue;
        }

        public override bool isXdmAtomicValueType(Type type)
        {
            return typeof(XdmAtomicValue).IsAssignableFrom(type);
        }

        public override bool isXdmValueType(Type type)
        {
            return typeof(XdmValue).IsAssignableFrom(type);
        }

        public override JValueRepresentation unwrapXdmValue(object obj)
        {
            return ((XdmValue)obj).Unwrap();
        }

        public override object wrapAsXdmValue(JValue value)
        {
            return XdmValue.Wrap(value);
        }

        public override bool isXmlNodeType(Type type)
        {
            return typeof(System.Xml.XmlNode).IsAssignableFrom(type);
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