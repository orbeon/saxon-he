using System;
using System.IO;
using System.Xml;
using System.Collections;
using System.Globalization;
using JStreamSource = javax.xml.transform.stream.StreamSource;
using JResult = javax.xml.transform.Result;
using JTransformerException = javax.xml.transform.TransformerException;
using JOutputURIResolver = net.sf.saxon.lib.OutputURIResolver;
using JAugmentedSource = net.sf.saxon.lib.AugmentedSource;
using JConfiguration = net.sf.saxon.Configuration;
using JLocationProvider = net.sf.saxon.@event.LocationProvider;
using JPipelineConfiguration = net.sf.saxon.@event.PipelineConfiguration;
using JStructuredQName = net.sf.saxon.om.StructuredQName;
using JProperties = java.util.Properties;
using JSequenceWriter = net.sf.saxon.@event.SequenceWriter;
using JReceiver = net.sf.saxon.@event.Receiver;
using JReceiverOptions = net.sf.saxon.@event.ReceiverOptions;
using JCompilerInfo = net.sf.saxon.trans.CompilerInfo;
using JExpressionPresenter = net.sf.saxon.trace.ExpressionPresenter;
using JValidation = net.sf.saxon.lib.Validation;
using JDocumentURI = net.sf.saxon.om.DocumentURI;
using JDecimalValue = net.sf.saxon.value.DecimalValue;
using JIndependentContext = net.sf.saxon.sxpath.IndependentContext;
using net.sf.saxon;
using JItem = net.sf.saxon.om.Item;
using JNodeInfo = net.sf.saxon.om.NodeInfo;
using JNodeName = net.sf.saxon.om.NodeName;
using JSchemaType = net.sf.saxon.type.SchemaType;
using JDocumentInfo = net.sf.saxon.om.DocumentInfo;
using JPullProvider = net.sf.saxon.pull.PullProvider;
using JPullSource = net.sf.saxon.pull.PullSource;
using JProcInstParser = net.sf.saxon.tree.util.ProcInstParser;
using net.sf.saxon.dotnet;
using CharSequence = java.lang.CharSequence;
using JBoolean = java.lang.Boolean;


namespace Saxon.Api
{

    /// <summary>
    /// An <c>XsltCompiler</c> object allows XSLT 2.0 stylesheets to be compiled.
    /// The compiler holds information that represents the static context
    /// for the compilation.
    /// </summary>
    /// <remarks>
    /// <para>To construct an <c>XsltCompiler</c>, use the factory method
    /// <c>NewXsltCompiler</c> on the <c>Processor</c> object.</para>
    /// <para>An <c>XsltCompiler</c> may be used repeatedly to compile multiple
    /// queries. Any changes made to the <c>XsltCompiler</c> (that is, to the
    /// static context) do not affect queries that have already been compiled.
    /// An <c>XsltCompiler</c> may be used concurrently in multiple threads, but
    /// it should not then be modified once initialized.</para>
    /// </remarks>

    [Serializable]
    public class XsltCompiler
    {

        private TransformerFactoryImpl factory;
        private Processor processor;
        private JConfiguration config;
        private JCompilerInfo info;
        private JIndependentContext env;
        private Uri baseUri;
        private IList errorList = null;
        private Hashtable variableList = new Hashtable();

        // internal constructor: the public interface is a factory method
        // on the Processor object

        internal XsltCompiler(Processor processor)
        {
            this.processor = processor;
            this.config = processor.config;
            this.factory = new TransformerFactoryImpl(config);
            this.info = new JCompilerInfo(config.getDefaultXsltCompilerInfo());
            info.setURIResolver(config.getURIResolver());
            info.setErrorListener(config.getErrorListener());
            this.env = new JIndependentContext(config);
        }

        /// <summary>
        /// The base URI of the stylesheet, which forms part of the static context
        /// of the stylesheet. This is used for resolving any relative URIs appearing
        /// within the stylesheet, for example in <c>xsl:include</c> and <c>xsl:import</c>
        /// declarations, in schema locations defined to <c>xsl:import-schema</c>, 
        /// or as an argument to the <c>document()</c> or <c>doc()</c> function.
        /// </summary>
        /// <remarks>
        /// This base URI is used only if the input supplied to the <c>Compile</c> method
        /// does not provide its own base URI. It is therefore used on the version of the
        /// method that supplies input from a <c>Stream</c>. On the version that supplies
        /// input from an <c>XmlReader</c>, this base URI is used only if the <c>XmlReader</c>
        /// does not have its own base URI.
        /// </remarks>


        public Uri BaseUri
        {
            get { return baseUri; }
            set { baseUri = value; }
        }

        /// <summary>
        /// Create a collation based on a given <c>CompareInfo</c> and <c>CompareOptions</c>    
        /// </summary>
        /// <param name="uri">The collation URI to be used within the XPath expression to refer to this collation</param>
        /// <param name="compareInfo">The <c>CompareInfo</c>, which determines the language-specific
        /// collation rules to be used</param>
        /// <param name="options">Options to be used in performing comparisons, for example
        /// whether they are to be case-blind and/or accent-blind</param>
        /// <param name="isDefault">If true, this collation will be used as the default collation</param>

        public void DeclareCollation(Uri uri, CompareInfo compareInfo, CompareOptions options, Boolean isDefault)
        {
            DotNetComparator comparator = new DotNetComparator(compareInfo, options);
            env.declareCollation(uri.ToString(), comparator, isDefault);
        }

        /// <summary>
        /// Get the Processor from which this XsltCompiler was constructed
        /// </summary>
        public Processor Processor
        {
            get { return processor; }
            set { processor = value; }
        }

        /// <summary>
        /// An <c>XmlResolver</c>, which will be used to resolve URI references while compiling
        /// a stylesheet
        /// </summary>
        /// <remarks>
        /// If no <c>XmlResolver</c> is set for the <c>XsltCompiler</c>, the <c>XmlResolver</c>
        /// is used that was set on the <c>Processor</c> at the time <c>NewXsltCompiler</c>
        /// was called.
        /// </remarks>

        public XmlResolver XmlResolver
        {
            get
            {
                return ((DotNetURIResolver)info.getURIResolver()).getXmlResolver();
            }
            set
            {
                info.setURIResolver(new DotNetURIResolver(value));
            }
        }

        /// <summary>
        /// The <c>SchemaAware</c> property determines whether the stylesheet is schema-aware. By default, a stylesheet
        /// is schema-aware if it contains one or more <code>xsl:import-schema</code> declarations. This option allows
        /// a stylesheet to be marked as schema-aware even if it does not contain such a declaration.
        /// </summary>
        /// <remarks>
        /// <para>If the stylesheet is not schema-aware, then schema-validated input documents will be rejected.</para>
        /// <para>The reason for this option is that it is expensive to generate code that can handle typed input
        /// documents when they will never arise in practice.</para>
        /// <para>The initial setting of this property is false, regardless of whether or not the <c>Processor</c>
        /// is schema-aware. Setting this property to true if the processor is not schema-aware will cause an Exception.</para>
        /// </remarks>
         
        public bool SchemaAware
        {
            get
            {
                return info.isSchemaAware();
            }
            set
            {
                info.setSchemaAware(value);
            }
        }

        /// <summary>
        /// The <c>XsltLanguageVersion</c> property determines whether the version of the XSLT language specification
        /// implemented by the compiler. The values 2.0 and 3.0 refer to the XSLT 2.0 and XSLT 3.0 (formerly XSLT 2.1) specifications.
        /// The value 0.0 (which is the initial default) indicates that the value is to be taken from the <c>version</c>
        /// attribute of the <c>xsl:stylesheet</c> element.
        /// </summary>
        /// <remarks>
        /// <para>Values that are not numerically equal to one of the above values are rejected.</para>
        /// <para>A warning is output (unless suppressed) when the XSLT language version supported by the processor
        /// is different from the value of the <c>version</c> attribute of the <c>xsl:stylesheet</c> element.</para>
        /// <para>XSLT 3.0 features are supported only in Saxon-PE and Saxon-EE. Setting the value to 3.0 under 
        /// Saxon-HE will cause an error if (and only if) the stylesheet actually uses XSLT 3.0 constructs.</para>
        /// </remarks>

        public string XsltLanguageVersion
        {
            get
            {
                return info.getXsltVersion().toString();
            }
            set
            {
                info.setXsltVersion((JDecimalValue)JDecimalValue.makeDecimalValue(value.ToString(), true));
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

        public IList ErrorList
        {
            set
            {
                errorList = value;
                info.setErrorListener(new ErrorGatherer(value));
            }
            get
            {
                return errorList;
            }
        }

        /// <summary>
        /// Compile a stylesheet supplied as a Stream.
        /// </summary>
        /// <example>
        /// <code>
        /// Stream source = new FileStream("input.xsl", FileMode.Open, FileAccess.Read);
        /// XsltExecutable q = compiler.Compile(source);
        /// source.Close();
        /// </code>
        /// </example>
        /// <param name="input">A stream containing the source text of the stylesheet</param>
        /// <returns>An <c>XsltExecutable</c> which represents the compiled stylesheet object.
        /// The XsltExecutable may be loaded as many times as required, in the same or a different
        /// thread. The <c>XsltExecutable</c> is not affected by any changes made to the <c>XsltCompiler</c>
        /// once it has been compiled.</returns>
        /// <remarks>
        /// <para>If the stylesheet contains any <c>xsl:include</c> or <c>xsl:import</c> declarations,
        /// then the <c>BaseURI</c> property must be set to allow these to be resolved.</para>
        /// <para>The stylesheet is contained in the part of the input stream between its current
        /// position and the end of the stream. It is the caller's responsibility to close the input 
        /// stream after use. If the compilation succeeded, then on exit the stream will be 
        /// exhausted; if compilation failed, the current position of the stream on exit is
        /// undefined.</para>
        /// </remarks>

        public XsltExecutable Compile(Stream input)
        {
            try
            {
                JStreamSource ss = new JStreamSource(new DotNetInputStream(input));
                if (baseUri != null)
                {
                    ss.setSystemId(baseUri.ToString());
                }
                PreparedStylesheet pss = (PreparedStylesheet)factory.newTemplates(ss, info);
                return new XsltExecutable(pss);
            }
            catch (JTransformerException err)
            {
                throw new StaticError(err);
            }
        }


        ///<summary>  
        ///  Get the underlying CompilerInfo object, which provides more detailed (but less stable) control
        ///  over some compilation options
        ///  </summary>
        /// <returns> the underlying CompilerInfo object, which holds compilation-time options. The methods on
         /// this object are not guaranteed stable from release to release.
         /// </returns>

        public JCompilerInfo GetUnderlyingCompilerInfo() {
            return info;
        }


        /// <summary>
        /// Externally set the value of a static parameter (new facility in XSLT 3.0) 
        /// </summary>
        /// <param name="name">The name of the parameter, expressed
        /// as a QName. If a parameter of this name has been declared in the
        /// stylesheet, the given value will be assigned to the variable. If the
        /// variable has not been declared, calling this method has no effect (it is
        /// not an error).</param>
        /// <param name="value">The value to be given to the parameter.
        /// If the parameter declaration defines a required type for the variable, then
        /// this value will be converted in the same way as arguments to function calls
        /// (for example, numeric promotion is applied).</param>
        public void SetParameter(QName name, XdmValue value)
        {
            variableList.Add(name, value);
            info.setParameter(name.ToStructuredQName(), value.Unwrap());
        }

        /// <summary>
        /// Compile a stylesheet supplied as a TextReader.
        /// </summary>
        /// <example>
        /// <code>
        /// String ss = "<![CDATA[<xsl:stylesheet version='2.0'>....</xsl:stylesheet>]]>";
        /// TextReader source = new StringReader(ss);
        /// XsltExecutable q = compiler.Compile(source);
        /// source.Close();
        /// </code>
        /// </example>
        /// <param name="input">A <c>TextReader</c> containing the source text of the stylesheet</param>
        /// <returns>An <c>XsltExecutable</c> which represents the compiled stylesheet object.
        /// The XsltExecutable may be loaded as many times as required, in the same or a different
        /// thread. The <c>XsltExecutable</c> is not affected by any changes made to the <c>XsltCompiler</c>
        /// once it has been compiled.</returns>
        /// <remarks>
        /// <para>If the stylesheet contains any <c>xsl:include</c> or <c>xsl:import</c> declarations,
        /// then the <c>BaseURI</c> property must be set to allow these to be resolved.</para>
        /// <para>The stylesheet is contained in the part of the input stream between its current
        /// position and the end of the stream. It is the caller's responsibility to close the 
        /// <c>TextReader</c> after use. If the compilation succeeded, then on exit the stream will be 
        /// exhausted; if compilation failed, the current position of the stream on exit is
        /// undefined.</para>
        /// </remarks>

        public XsltExecutable Compile(TextReader input)
        {
            JStreamSource ss = new JStreamSource(new DotNetReader(input));
            if (baseUri != null)
            {
                ss.setSystemId(baseUri.ToString());
            }
            PreparedStylesheet pss = (PreparedStylesheet)factory.newTemplates(ss, info);
            return new XsltExecutable(pss);
        }

        /// <summary>
        /// Compile a stylesheet, retrieving the source using a URI.
        /// </summary>
        /// <remarks>
        /// The document located via the URI is parsed using the <c>System.Xml</c> parser. This
        /// URI is used as the base URI of the stylesheet: the <c>BaseUri</c> property of the
        /// <c>Compiler</c> is ignored.
        /// </remarks>
        /// <param name="uri">The URI identifying the location where the stylesheet document can be
        /// found</param>
        /// <returns>An <c>XsltExecutable</c> which represents the compiled stylesheet object.
        /// The XsltExecutable may be run as many times as required, in the same or a different
        /// thread. The <c>XsltExecutable</c> is not affected by any changes made to the <c>XsltCompiler</c>
        /// once it has been compiled.</returns>

        public XsltExecutable Compile(Uri uri)
        {
            Object obj = XmlResolver.GetEntity(uri, "application/xml", Type.GetType("System.IO.Stream"));
            if (obj is Stream)
            {
                try
                {
                    XmlReaderSettings settings = new XmlReaderSettings();
                    settings.ProhibitDtd = false;   // must expand entity references
                    settings.XmlResolver = XmlResolver;
                    settings.IgnoreWhitespace = false;
                    settings.ValidationType = ValidationType.None;
                    XmlReader parser = XmlReader.Create((Stream)obj, settings, uri.ToString());
                    //XmlReader parser = new XmlTextReader(uri.ToString(), (Stream)obj);
                    //((XmlTextReader)parser).Normalization = true;
                    //((XmlTextReader)parser).WhitespaceHandling = WhitespaceHandling.All;
                    //((XmlTextReader)parser).XmlResolver = XmlResolver;
                    // Always need a validating parser, because that's the only way to get entity references expanded
                    //parser = new XmlValidatingReader(parser);
                    //((XmlValidatingReader)parser).ValidationType = ValidationType.None;
                    JPullSource source = new JPullSource(new DotNetPullProvider(parser));
                    PreparedStylesheet pss = (PreparedStylesheet)factory.newTemplates(source, info);
                    return new XsltExecutable(pss);
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
        /// Compile a stylesheet, delivered using an XmlReader.
        /// </summary>
        /// <remarks>
        /// The <c>XmlReader</c> is responsible for parsing the document; this method builds a tree
        /// representation of the document (in an internal Saxon format) and compiles it.
        /// The <c>XmlReader</c> will be used as supplied; it is the caller's responsibility to
        /// ensure that the settings of the <c>XmlReader</c> are consistent with the requirements
        /// of the XSLT specification (for example, that entity references are expanded and whitespace
        /// is preserved).
        /// </remarks>
        /// <remarks>
        /// If the <c>XmlReader</c> has a <c>BaseUri</c> property, then that property determines
        /// the base URI of the stylesheet module, which is used when resolving any <c>xsl:include</c>
        /// or <c>xsl:import</c> declarations. If the <c>XmlReader</c> has no <c>BaseUri</c>
        /// property, then the <c>BaseUri</c> property of the <c>Compiler</c> is used instead.
        /// An <c>ArgumentNullException</c> is thrown if this property has not been supplied.
        /// </remarks>
        /// <param name="reader">The XmlReader (that is, the XML parser) used to supply the document containing
        /// the principal stylesheet module.</param>
        /// <returns>An <c>XsltExecutable</c> which represents the compiled stylesheet object.
        /// The XsltExecutable may be run as many times as required, in the same or a different
        /// thread. The <c>XsltExecutable</c> is not affected by any changes made to the <c>XsltCompiler</c>
        /// once it has been compiled.</returns>


        public XsltExecutable Compile(XmlReader reader)
        {
            DotNetPullProvider pp = new DotNetPullProvider(reader);
            JPipelineConfiguration pipe = config.makePipelineConfiguration();
            pipe.setLocationProvider(pp);
            pp.setPipelineConfiguration(pipe);
            // pp = new PullTracer(pp);  /* diagnostics */
            JPullSource source = new JPullSource(pp);
            String baseu = reader.BaseURI;
            if (baseu == null || baseu == String.Empty)
            {
                // if no baseURI is supplied by the XmlReader, use the one supplied to this Compiler
                if (baseUri == null)
                {
                    throw new ArgumentNullException("BaseUri");
                }
                baseu = baseUri.ToString();
                pp.setBaseURI(baseu);
            }
            source.setSystemId(baseu);
            PreparedStylesheet pss = (PreparedStylesheet)factory.newTemplates(source, info);
            return new XsltExecutable(pss);
        }

        /// <summary>
        /// Compile a stylesheet, located at an XdmNode. This may be a document node whose
        /// child is an <c>xsl:stylesheet</c> or <c>xsl:transform</c> element, or it may be
        /// the <c>xsl:stylesheet</c> or <c>xsl:transform</c> element itself.
        /// </summary>
        /// <param name="node">The document node or the outermost element node of the document
        /// containing the principal stylesheet module.</param>
        /// <returns>An <c>XsltExecutable</c> which represents the compiled stylesheet object.
        /// The XsltExecutable may be run as many times as required, in the same or a different
        /// thread. The <c>XsltExecutable</c> is not affected by any changes made to the <c>XsltCompiler</c>
        /// once it has been compiled.</returns>

        public XsltExecutable Compile(XdmNode node)
        {
            PreparedStylesheet pss = (PreparedStylesheet)factory.newTemplates((JNodeInfo)node.value, info);
            return new XsltExecutable(pss);
        }

        /// <summary>Locate and compile a stylesheet identified by an &lt;?xml-stylesheet?&gt;
        /// processing instruction within a source document.
        /// </summary>
        /// <param name="source">The document node of the source document containing the
        /// xml-stylesheet processing instruction.</param>
        /// <returns>An <c>XsltExecutable</c> which represents the compiled stylesheet object.</returns>
        /// <remarks>There are some limitations in the current implementation. The media type
        /// is ignored, as are the other parameters of the xml-stylesheet instruction. The
        /// href attribute must either reference an embedded stylesheet within the same
        /// document or a non-embedded external stylesheet.</remarks>

        public XsltExecutable CompileAssociatedStylesheet(XdmNode source)
        {
            // TODO: lift the restrictions
            if (source == null || source.NodeKind != XmlNodeType.Document)
            {
                throw new ArgumentException("Source must be a document node");
            }
            IEnumerator kids = source.EnumerateAxis(XdmAxis.Child);
            QName xmlstyle = new QName("", "xml-stylesheet");
            while (kids.MoveNext())
            {
                XdmNode n = (XdmNode)kids.Current;
                if (n.NodeKind == XmlNodeType.ProcessingInstruction &&
                    n.NodeName.Equals(xmlstyle))
                {
                    // TODO: check the media type
                    String href = JProcInstParser.getPseudoAttribute(n.StringValue, "href");
                    if (href == null)
                    {
                        throw new DynamicError("xml-stylesheet processing instruction has no href attribute");
                    }
                    String fragment = null;
                    int hash = href.LastIndexOf('#');
                    if (hash == 0)
                    {
                        if (href.Length == 1)
                        {
                            throw new DynamicError("Relative URI of '#' is invalid");
                        }
                        fragment = href.Substring(1);
                        JNodeInfo target = ((JDocumentInfo)source.value).selectID(fragment, true);
                        XdmNode targetWrapper = null;
                        if (target == null)
                        {
                            // There's a problem here because the Microsoft XML parser doesn't
                            // report id values, so selectID() will never work. We work around that
                            // by looking for an attribute named "id" appearing on an xsl:stylesheet
                            // or xsl:transform element
                            QName qid = new QName("", "id");
                            IEnumerator en = source.EnumerateAxis(XdmAxis.Descendant);
                            while (en.MoveNext())
                            {
                                XdmNode x = (XdmNode)en.Current;
                                if (x.NodeKind == XmlNodeType.Element &&
                                        x.NodeName.Uri == "http://www.w3.org/1999/XSL/Transform" &&
                                        (x.NodeName.LocalName == "stylesheet" || x.NodeName.LocalName == "transform" &&
                                        x.GetAttributeValue(qid) == fragment))
                                {
                                    targetWrapper = x;
                                }
                            }
                        }
                        else
                        {
                            targetWrapper = (XdmNode)XdmValue.Wrap(target);
                        }
                        if (targetWrapper == null)
                        {
                            throw new DynamicError("No element with id='" + fragment + "' found");
                        }
                        return Compile(targetWrapper);
                    }
                    else if (hash > 0)
                    {
                        throw new NotImplementedException("href cannot identify an embedded stylesheet in a different document");
                    }
                    else
                    {
                        Uri uri = new Uri(n.BaseUri, href);
                        return Compile(uri);
                    }
                }
            }
            throw new DynamicError("xml-stylesheet processing instruction not found");
        }
    }

    /// <summary>
    /// An <c>XsltExecutable</c> represents the compiled form of a stylesheet. To execute the stylesheet,
    /// it must first be loaded to form an <c>XsltTransformer</c>.
    /// </summary>
    /// <remarks>
    /// <para>An <c>XsltExecutable</c> is immutable, and therefore thread-safe. It is simplest to
    /// load a new <c>XsltEvaluator</c> each time the stylesheet is to be run. However, the 
    /// <c>XsltEvaluator</c> is serially reusable within a single thread.</para>
    /// <para>An <c>XsltExecutable</c> is created by using one of the <c>Compile</c>
    /// methods on the <c>XsltCompiler</c> class.</para>
    /// </remarks>    

    [Serializable]
    public class XsltExecutable
    {

        private PreparedStylesheet pss;

        // internal constructor

        internal XsltExecutable(PreparedStylesheet pss)
        {
            this.pss = pss;
        }

        /// <summary>
        /// Load the stylesheet to prepare it for execution.
        /// </summary>
        /// <returns>
        /// An <c>XsltTransformer</c>. The returned <c>XsltTransformer</c> can be used to
        /// set up the dynamic context for stylesheet evaluation, and to run the stylesheet.
        /// </returns>

        public XsltTransformer Load()
        {
            Controller c = (Controller)pss.newTransformer();
            return new XsltTransformer(c);
        }

        /// <summary>
        /// Output an XML representation of the compiled code of the stylesheet, for purposes of 
        /// diagnostics and instrumentation
        /// </summary>
        /// <param name="destination">The destination for the diagnostic output</param>
        
        public void Explain(XmlDestination destination) {
            JConfiguration config = pss.getConfiguration();
            JResult result = destination.GetResult(config.makePipelineConfiguration());          
            JProperties properties = new JProperties();
            properties.setProperty("indent", "yes");
            properties.setProperty("{http://saxon.sf.net/}indent-spaces", "2");
            JReceiver receiver = config.getSerializerFactory().getReceiver(
                result, config.makePipelineConfiguration(), properties);
            JExpressionPresenter presenter = new JExpressionPresenter(config, receiver);
            pss.explain(presenter);
        }

        /// <summary>
        /// Escape hatch to the underlying Java implementation object
        /// </summary>

        public PreparedStylesheet Implementation
        {
            get
            {
                return pss;
            }
        }


    }

    /// <summary inherits="IEnumerable">
    /// An <c>XsltTransformer</c> represents a compiled and loaded stylesheet ready for execution.
    /// The <c>XsltTransformer</c> holds details of the dynamic evaluation context for the stylesheet.
    /// </summary>
    /// <remarks>
    /// <para>An <c>XsltTransformer</c> should not be used concurrently in multiple threads. It is safe,
    /// however, to reuse the object within a single thread to run the same stylesheet several times.
    /// Running the stylesheet does not change the context that has been established.</para>
    /// <para>An <c>XsltTransformer</c> is always constructed by running the <c>Load</c> method of
    /// an <c>XsltExecutable</c>.</para>
    /// </remarks>     

    [Serializable]
    public class XsltTransformer
    {

        private Controller controller;
        private JNodeInfo initialContextNode;
        private IResultDocumentHandler resultDocumentHandler;
        private IMessageListener messageListener;
        private JStreamSource streamSource;
        private Stream traceFunctionDestination;
        

        // internal constructor

        internal XsltTransformer(Controller controller)
        {
            this.controller = controller;
        }

        /// <summary>
        /// The initial context item for the stylesheet.
        /// </summary>
        /// <remarks><para>This may be either a node or an atomic
        /// value. Most commonly it will be a document node, which might be constructed
        /// using the <c>Build</c> method of the <c>DocumentBuilder</c> object.</para>
        /// <para>Note that this can be inefficient if the stylesheet uses <c>xsl:strip-space</c>
        /// to strip whitespace, or <c>input-type-annotations="strip"</c> to remove type
        /// annotations, since this will result in the transformation operating on a virtual document
        /// implemented as a view or wrapper of the supplied document.</para>
        /// </remarks>

        public XdmNode InitialContextNode
        {
            get { return (initialContextNode == null ? null : (XdmNode)XdmValue.Wrap(initialContextNode)); }
            set { initialContextNode = (value == null ? null : (JNodeInfo)value.Unwrap()); }
        }

        /// <summary>
        /// Supply the principal input document for the transformation in the form of a stream.
        /// </summary>
        /// <remarks>
        /// <para>If this method is used, the <c>InitialContextNode</c> is ignored.</para>
        /// <para>The supplied stream will be consumed by the <c>Run()</c> method.
        /// Closing the input stream after use is the client's responsibility.</para>
        /// <para>A base URI must be supplied in all cases. It is used to resolve relative
        /// URI references appearing within the input document.</para>
        /// <para>Schema validation is applied to the input document according to the value of
        /// the <c>SchemaValidationMode</c> property.</para>
        /// <para>Whitespace stripping is applied according to the value of the
        /// <c>xsl:strip-space</c> and <c>xsl:preserve-space</c> declarations in the stylesheet.</para>
        /// </remarks>
        /// <param name="input">
        /// The stream containing the source code of the principal input document to the transformation. The document
        /// node at the root of this document will be the initial context node for the transformation.
        /// </param>
        /// <param name="baseUri">
        /// The base URI of the principal input document. This is used for example by the <c>document()</c>
        /// function if the document contains links to other documents in the form of relative URIs.</param>

        public void SetInputStream(Stream input, Uri baseUri) 
        {
            streamSource = new JStreamSource(new DotNetInputStream(input), baseUri.ToString());
        }

        /// <summary>
        /// The initial mode for the stylesheet. This is either a QName, for a 
        /// named mode, or null, for the unnamed (default) mode.
        /// </summary>

        public QName InitialMode
        {
            get
            {
                JStructuredQName mode = controller.getInitialMode().getModeName();
                if (mode == null)
                {
                    return null;
                }
                return QName.FromClarkName(mode.getClarkName());
            }
            set
            {
                controller.setInitialMode(value.ClarkName);
            }
        }

        /// <summary>
        /// The initial template for the stylesheet. This is either a QName, for a 
        /// named template, or null, if no initial template has been set.
        /// </summary>
        /// <exception cref="DynamicError">Setting this property to the name of a template
        /// that does not exist in the stylesheet throws a DynamicError with error 
        /// code XTDE0040. Setting it to the name of a template that has template
        /// parameters throws a DynamicError with error code XTDE0060.</exception>

        public QName InitialTemplate
        {
            get
            {
                String name = controller.getInitialTemplate();
                if (name == null)
                {
                    return null;
                }
                return QName.FromClarkName(name);
            }
            set
            {
                try
                {
                    controller.setInitialTemplate(value.ClarkName);
                }
                catch (javax.xml.transform.TransformerException err)
                {
                    throw new DynamicError(err);
                }
            }
        }

        /// <summary>
        /// The base output URI, which acts as the base URI for resolving the <c>href</c>
        /// attribute of <c>xsl:result-document</c>.
        /// </summary>

        public Uri BaseOutputUri
        {
            get
            {
                return new Uri(controller.getBaseOutputURI());
            }
            set
            {
                controller.setBaseOutputURI(value.ToString());
            }
        }

        public RecoveryPolicy RecoveryPolicy
        {
            get
            {
                switch (controller.getRecoveryPolicy())
                {
                    case Configuration.RECOVER_SILENTLY:
                        return RecoveryPolicy.RecoverSilently;
                    case Configuration.RECOVER_WITH_WARNINGS:
                        return RecoveryPolicy.RecoverWithWarnings;
                    default: return RecoveryPolicy.DoNotRecover;
                }
            }
            set
            {
                controller.setRecoveryPolicy(
                    value == RecoveryPolicy.RecoverSilently ? Configuration.RECOVER_SILENTLY :
                    value == RecoveryPolicy.RecoverWithWarnings ? Configuration.RECOVER_WITH_WARNINGS :
                    Configuration.DO_NOT_RECOVER);
            }
        }

        /// <summary>
        /// The <c>SchemaValidationMode</c> to be used in this transformation, especially for documents
        /// loaded using the <c>doc()</c>, <c>document()</c>, or <c>collection()</c> functions.
        /// </summary>
        /// 

        public SchemaValidationMode SchemaValidationMode
        {
            get
            {
                switch (controller.getSchemaValidationMode())
                {
                    case JValidation.STRICT:
                        return SchemaValidationMode.Strict;
                    case JValidation.LAX:
                        return SchemaValidationMode.Lax;
                    case JValidation.STRIP:
                        return SchemaValidationMode.None;
                    case JValidation.PRESERVE:
                        return SchemaValidationMode.Preserve;
                    case JValidation.DEFAULT:
                    default:
                        return SchemaValidationMode.Unspecified;
                }
            }

            set
            {
                switch (value)
                {
                    case SchemaValidationMode.Strict:
                        controller.setSchemaValidationMode(JValidation.STRICT);
                        break;
                    case SchemaValidationMode.Lax:
                        controller.setSchemaValidationMode(JValidation.LAX);
                        break;
                    case SchemaValidationMode.None:
                        controller.setSchemaValidationMode(JValidation.STRIP);
                        break;
                    case SchemaValidationMode.Preserve:
                        controller.setSchemaValidationMode(JValidation.PRESERVE);
                        break;
                    case SchemaValidationMode.Unspecified:
                    default:
                        controller.setSchemaValidationMode(JValidation.DEFAULT);
                        break;
                }
            }
        }



        /// <summary>
        /// The <c>XmlResolver</c> to be used at run-time to resolve and dereference URIs
        /// supplied to the <c>doc()</c> and <c>document()</c> functions.
        /// </summary>

        public XmlResolver InputXmlResolver
        {
            get
            {
                return ((DotNetURIResolver)controller.getURIResolver()).getXmlResolver();
            }
            set
            {
                controller.setURIResolver(new DotNetURIResolver(value));
            }
        }

        /// <summary>
        /// The <c>IResultDocumentHandler</c> to be used at run-time to process the output
        /// produced by any <c>xsl:result-document</c> instruction with an <c>href</c>
        /// attribute.
        /// </summary>
        /// <remarks>
        /// In the absence of a user-supplied result document handler, the <c>href</c>
        /// attribute of the <c>xsl:result-document</c> instruction must be a valid relative
        /// URI, which is resolved against the value of the <c>BaseOutputUri</c> property,
        /// and the resulting absolute URI must identify a writable resource (typically
        /// a file in filestore, using the <c>file:</c> URI scheme).
        /// </remarks>

        public IResultDocumentHandler ResultDocumentHandler
        {
            get
            {
                return resultDocumentHandler;
            }
            set
            {
                resultDocumentHandler = value;
                controller.setOutputURIResolver(new ResultDocumentHandlerWrapper(value, controller.makePipelineConfiguration()));
            }
        }

        /// <summary>
        /// Listener for messages output using &lt;xsl:message&gt;. 
        /// <para>The caller may supply a message listener before calling <c>Run</c>;
        /// the processor will then invoke the listener once for each message generated during
        /// the transformation. Each message will be output as an object of type <c>XdmNode</c>
        /// representing a document node.</para>
        /// <para>If no message listener is supplied by the caller, message information will be written to
        /// the standard error stream.</para>
        /// </summary>
        /// <remarks>
        /// <para>Each message is presented as an XML document node. Calling <c>ToString()</c>
        /// on the message object will usually generate an acceptable representation of the
        /// message.</para>
        /// <para>When the &lt;xsl:message&gt; instruction specifies <c>terminate="yes"</c>,
        /// the message is first notified using this interface, and then an exception is thrown
        /// which terminates the transformation.</para>
        /// </remarks>

        public IMessageListener MessageListener
        {
            set
            {
                messageListener = value;
                JPipelineConfiguration pipe = controller.makePipelineConfiguration();
                controller.setMessageEmitter(new MessageListenerProxy(pipe, value));
            }
            get
            {
                return messageListener;
            }
        }

        /// <summary>
        /// Destination for output of messages using &lt;trace()&gt;. 
        /// <para>If no message listener is supplied by the caller, message information will be written to
        /// the standard error stream.</para>
        /// </summary>
        /// <remarks>
        /// <para>The supplied destination is ignored if a <c>TraceListener</c> is in use.</para>
        /// </remarks>

        public Stream TraceFunctionDestination
        {
            set
            {
                traceFunctionDestination = value;
                controller.setTraceFunctionDestination(
                    new java.io.PrintStream(new DotNetOutputStream(value)));
            }
            get
            {
                return traceFunctionDestination;
            }
        }



        /// <summary>
        /// Set the value of a stylesheet parameter.
        /// </summary>
        /// <param name="name">The name of the parameter, expressed
        /// as a QName. If a parameter of this name has been declared in the
        /// stylesheet, the given value will be assigned to the variable. If the
        /// variable has not been declared, calling this method has no effect (it is
        /// not an error).</param>
        /// <param name="value">The value to be given to the parameter.
        /// If the parameter declaration defines a required type for the variable, then
        /// this value will be converted in the same way as arguments to function calls
        /// (for example, numeric promotion is applied).</param>

        public void SetParameter(QName name, XdmValue value)
        {
            controller.setParameter(name.ClarkName, value.Unwrap());
        }

        /// <summary>
        /// Run the transformation, sending the result to a specified destination.
        /// </summary>
        /// <param name="destination">
        /// The destination for the results of the stylesheet. The class <c>XmlDestination</c>
        /// is an abstraction that allows a number of different kinds of destination
        /// to be specified.
        /// </param>
        /// <exception cref="DynamicError">Throws a DynamicError if the transformation
        /// fails.</exception>

        public void Run(XmlDestination destination)
        {
            try
            {
                controller.setOutputProperties(destination.GetOutputProperties());
                if (streamSource != null)
                {
                    controller.transform(streamSource, destination.GetResult(controller.makePipelineConfiguration()));
                }
                else if (initialContextNode != null)
                {
                    JDocumentInfo doc = initialContextNode.getDocumentRoot();
		    if(doc != null) {
                    	controller.registerDocument(doc, (doc.getBaseURI()==null ? null : new JDocumentURI(doc.getBaseURI())));
		    }
                    controller.transform(initialContextNode, destination.GetResult(controller.makePipelineConfiguration()));
                }
                else
                {
                    controller.transform(null, destination.GetResult(controller.makePipelineConfiguration()));
                }
                destination.Close();
            }
            catch (javax.xml.transform.TransformerException err)
            {
                throw new DynamicError(err);
            }
        }

        /// <summary>
        /// Escape hatch to the underlying Java implementation
        /// </summary>

        public Controller Implementation
        {
            get { return controller; }
        }


    }

    /// <summary>
    /// RecoveryPolicy is an enumeration of the different actions that can be taken when a "recoverable error" occurs
    /// </summary>

    public enum RecoveryPolicy
    {
        /// <summary>
        /// Ignore the error, take the recovery action, do not produce any message
        /// </summary>
        RecoverSilently,

        /// <summary>
        /// Take the recovery action after outputting a warning message
        /// </summary>
        RecoverWithWarnings,

        /// <summary>
        /// Treat the error as fatal
        /// </summary>
        DoNotRecover

    }



    ///<summary>An <c>IResultDocumentHandler</c> can be nominated to handle output
    /// produced by the <c>xsl:result-document</c> instruction in an XSLT stylesheet.
    ///</summary>
    ///<remarks>
    ///<para>This interface affects any <c>xsl:result-document</c> instruction
    /// executed by the stylesheet, provided that it has an <c>href</c> attribute.</para> 
    ///<para>If no <c>IResultDocumentHandler</c> is nominated (in the
    /// <c>IResultDocumentHandler</c> property of the <c>XsltTransformer</c>), the output
    /// of <code>xsl:result-document</code> is serialized, and is written to the file
    /// or other resource identified by the URI in the <c>href</c> attribute, resolved
    /// (if it is relative) against the URI supplied in the <c>BaseOutputUri</c> property
    /// of the <c>XsltTransformer</c>.</para>
    ///<para>If an <c>IResultDocumentHandler</c> is nominated, however, its
    /// <c>HandleResultDocument</c> method will be called whenever an <c>xsl:result-document</c>
    /// instruction with an <c>href</c> attribute is evaluated, and the generated result tree
    /// will be passed to the <c>XmlDestination</c> returned by that method.</para> 
    ///</remarks>

    public interface IResultDocumentHandler
    {

        /// <summary> Handle output produced by the <c>xsl:result-document</c>
        /// instruction in an XSLT stylesheet. This method is called by the XSLT processor
        /// when an <c>xsl:result-document</c> with an <c>href</c> attribute is evaluated.
        /// </summary>
        /// <param name="href">An absolute or relative URI. This will be the effective value of the 
        /// <c>href</c> attribute of the <c>xsl:result-document</c> in the stylesheet.</param>
        /// <param name="baseUri">The base URI that should be used for resolving the value of
        /// <c>href</c> if it is relative. This will always be the value of the <c>BaseOutputUri</c>
        /// property of the <c>XsltTransformer</c>.</param>
        /// <returns>An <c>XmlDestination</c> to handle the result tree produced by the
        /// <c>xsl:result-document</c> instruction. The <c>Close</c> method of the returned
        /// <c>XmlDestination</c> will be called when the output is complete.</returns>
        /// <remarks>
        /// <para>The XSLT processor will ensure that the stylesheet cannot create
        /// two distinct result documents which are sent to the same URI. It is the responsibility
        /// of the <c>IResultDocumentHandler</c> to ensure that two distinct result documents are
        /// not sent to the same <c>XmlDestination</c>. Failure to observe this rule can result
        /// in output streams being incorrectly closed.
        /// </para>
        /// <para>Note that more than one result document can be open at the same time,
        /// and that the order of opening, writing, and closing result documents chosen
        /// by the processor does not necessarily bear any direct resemblance to the way
        /// that the XSLT source code is written.</para></remarks>

        XmlDestination HandleResultDocument(string href, Uri baseUri);

    }

    internal class ResultDocumentHandlerWrapper : JOutputURIResolver
    {

        private IResultDocumentHandler handler;
        private ArrayList resultList = new ArrayList();
        private ArrayList destinationList = new ArrayList();
        private JPipelineConfiguration pipe;

        public ResultDocumentHandlerWrapper(IResultDocumentHandler handler, JPipelineConfiguration pipe)
        {
            this.handler = handler;
            this.pipe = pipe;
        }

        public JOutputURIResolver newInstance() {
            return new ResultDocumentHandlerWrapper(handler, pipe);
        }

        public JResult resolve(String href, String baseString)
        {
            Uri baseUri;
            try
            {
                baseUri = new Uri(baseString);
            }
            catch (System.UriFormatException err)
            {
                throw new JTransformerException("Invalid base output URI " + baseString, err);
            }
            XmlDestination destination = handler.HandleResultDocument(href, baseUri);
            JResult result = destination.GetResult(pipe);
            resultList.Add(result);
            destinationList.Add(destination);
            return result;
        }

        public void close(JResult result)
        {
            for (int i = 0; i < resultList.Count; i++)
            {
                if (Object.ReferenceEquals(resultList[i], result))
                {
                    ((XmlDestination)destinationList[i]).Close();
                    resultList.RemoveAt(i);
                    destinationList.RemoveAt(i);
                    return;
                }
            }
        }
    }

    ///<summary>An <c>IMessageListener</c> can be nominated to handle output
    /// produced by the <c>xsl:message</c> instruction in an XSLT stylesheet.
    ///</summary>
    ///<remarks>
    ///<para>This interface affects any <c>xsl:message</c> instruction
    /// executed by the stylesheet.</para> 
    ///<para>If no <c>IMessageListener</c> is nominated (in the
    /// <c>MessageListener</c> property of the <c>XsltTransformer</c>), the output
    /// of <code>xsl:message</code> is serialized, and is written to standard error
    /// output stream.</para>
    ///<para>If an <c>IMessageListener</c> is nominated, however, its
    /// <c>Message</c> method will be called whenever an <c>xsl:message</c>
    /// instruction is evaluated.</para> 
    ///</remarks>


    public interface IMessageListener
    {

        ///<summary>Handle the output of an <c>xsl:message</c> instruction
        ///in the stylesheet
        ///</summary>
        ///

        void Message(XdmNode content, bool terminate, IXmlLocation location);

    }

    /// <summary>
    /// An <c>IXmlLocation</c> represents the location of a node within an XML document.
    /// It is in two parts: the base URI (or system ID) of the external entity (which will usually
    /// be the XML document entity itself), and the line number of a node relative
    /// to the base URI of the containing external entity.
    /// </summary>
    /// 

    public interface IXmlLocation
    {

        /// <summary>
        /// The base URI (system ID) of an external entity within an XML document.
        /// Set to null if the base URI is not known (for example, for an XML document
        /// created programmatically where no base URI has been set up).
        /// </summary>

        Uri BaseUri { get; set; }

        /// <summary>
        /// The line number of a node relative to the start of the external entity.
        /// The value -1 indicates that the line number is not known or not applicable.
        /// </summary>

        int LineNumber { get; set; }
    }

    internal class XmlLocation : IXmlLocation
    {
        private Uri baseUri;
        private int lineNumber;
        public Uri BaseUri
        {
            get { return baseUri; }
            set { baseUri = value; }
        }
        public int LineNumber
        {
            get { return lineNumber; }
            set { lineNumber = value; }
        }
    }


    [Serializable]
    internal class MessageListenerProxy : JSequenceWriter
    {

        public IMessageListener listener;
        public bool terminate;
        public int locationId;

        public MessageListenerProxy(JPipelineConfiguration pipe, IMessageListener ml) : base(pipe)
        {
            listener = ml;
        }

        public override void startDocument(int properties)
        {
            terminate = (properties & JReceiverOptions.TERMINATE) != 0;
            locationId = -1;
            base.startDocument(properties);
        }

        public override void startElement(JNodeName nameCode, JSchemaType typeCode, int locationId, int properties)
        {
            if (this.locationId == -1)
            {
                this.locationId = locationId;
            }
            base.startElement(nameCode, typeCode, locationId, properties);
        }

        public override void characters(CharSequence s, int locationId, int properties)
        {
            if (this.locationId == -1)
            {
                this.locationId = locationId;
            }
            base.characters(s, locationId, properties);
        }

        public override void append(JItem item, int locationId, int copyNamespaces)
        {
            if (this.locationId == -1)
            {
                this.locationId = locationId;
            }
            base.append(item, locationId, copyNamespaces);
        }

        public override void write(JItem item)
        {
            XmlLocation loc = new XmlLocation();
            if (locationId != -1)
            {
                JLocationProvider provider = getPipelineConfiguration().getLocationProvider();
                loc.BaseUri = new Uri(provider.getSystemId(locationId));
                loc.LineNumber = provider.getLineNumber(locationId);
            }
            listener.Message((XdmNode)XdmItem.Wrap(item), terminate, loc);
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