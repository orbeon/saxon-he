package net.sf.saxon;


/**
  * FeatureKeys defines a set of constants, names of Saxon configuration
  * options which can be supplied to the Saxon implementations of the JAXP
  * interfaces TransformerFactory, SchemaFactory, Validator, and ValidationHandler.
  *
  * @author Michael H. Kay
  */


public abstract class FeatureKeys {

	/**
	 * ALLOW_EXTERNAL_FUNCTIONS must be a Boolean; it determines whether calls to external functions are allowed.
     * More specifically, it disallows all of the following:
     *
     * <ul>
     * <li>Calls to Java extension functions</li>
     * <li>Use of the XSLT system-property() function to access Java system properties</li>
     * <li>Use of a relative URI in the <code>xsl:result-document</code> instruction</li>
     * <li>Calls to XSLT extension instructions</li>
     * </ul>
     *
     * <p>Note that this option does not disable use of the <code>doc()</code> function or similar
     * functions to access the filestore of the machine where the transformation or query is running.
     * That should be done using a user-supplied <code>URIResolver</code></p>
     *
     * <p>Note that an {@link net.sf.saxon.functions.ExtensionFunctionCall} is trusted; calls are allowed even if
     * this configuration option is false. In cases where an IntegratedFunction
     * is used to load and execute untrusted code, it should check this configuration option before doing so.</p>
	*/

	public static final String ALLOW_EXTERNAL_FUNCTIONS =
	        "http://saxon.sf.net/feature/allow-external-functions";

    /**
    * COLLATION_URI_RESOLVER must be a {@link net.sf.saxon.sort.CollationURIResolver}.
     * This resolver will be used to resolve collation URIs used in stylesheets compiled or executed under the
     * control of this TransformerFactory
    */

    public static final String COLLATION_URI_RESOLVER =
            "http://saxon.sf.net/feature/collation-uri-resolver";

    /**
     * COLLATION_URI_RESOLVER_CLASS must be the name of a class that implements the interface
     * {@link net.sf.saxon.sort.CollationURIResolver}. The class will be instantiated, and the instance
     * will act as the value of the {@link #COLLATION_URI_RESOLVER} property.
    */

    public static final String COLLATION_URI_RESOLVER_CLASS =
            "http://saxon.sf.net/feature/collation-uri-resolver-class";

    /**
    * COLLECTION_URI_RESOLVER must be a {@link net.sf.saxon.CollectionURIResolver}.
     * This resolver will be used to resolve collection URIs used in calls of the collection() function
    */

    public static final String COLLECTION_URI_RESOLVER =
            "http://saxon.sf.net/feature/collection-uri-resolver";

    /**
     * COLLECTION_URI_RESOLVER must be must be the name of a class that implements the interface
     *  {@link net.sf.saxon.CollectionURIResolver}.
     * The class will be instantiated, and the instance
     * will act as the value of the {@link #COLLECTION_URI_RESOLVER} property.
    */

    public static final String COLLECTION_URI_RESOLVER_CLASS =
            "http://saxon.sf.net/feature/collection-uri-resolver-class";

    /**
     * COMPILE_WITH_TRACING must be a Boolean. If true, stylesheets and queries
     * are compiled with tracing enabled, but the choice of a trace listener
     * is deferred until run time (see {@link Controller#addTraceListener(net.sf.saxon.trace.TraceListener)})
     */

    public static final String COMPILE_WITH_TRACING =
            "http://saxon.sf.net/feature/compile-with-tracing";

    /**
     * CONFIGURATION must be an instance of {@link Configuration}. This attribute cannot be set on the
     * Configuration itself, but it can be set on various JAXP factory objects such as a
     * TransformerFactory or DocumentBuilderFactory, to ensure that several such factories use the same
     * configuration. Note that other configuration options are held in the Configuration object, so setting
     * this attribute will cancel all others that have been set. Also, if two factories share the same
     * configuration, then setting an attribute on one affects all the others.
     */

    public static final String CONFIGURATION =
            "http://saxon.sf.net/feature/configuration";

    /**
      * DEFAULT_COLLATION must be a String in the form of an absolute URI.
      * This determines the collation that is used when no explicit collation is requested.
      * It is not necessary for this collation to exist (or to have been registered) when setting
      * this option; it only needs to exist by the time it is used.
     */

     public static final String DEFAULT_COLLATION =
             "http://saxon.sf.net/feature/defaultCollation";


    /**
      * DEFAULT_COLLECTION must be a String in the form of an absolute URI.
      * This determines the collection that is used when the fn:collection() function is called
      * with no arguments; the effect is the same as if it were called passing the URI that is
      * the value of this configuration property.
     */

     public static final String DEFAULT_COLLECTION =
             "http://saxon.sf.net/feature/defaultCollection";

    /**
      * DEFAULT_COUNTRY must be a String in the form of an ISO country code.
      * This determines the country that is used by format-date() and similar functions if
      * no country code is supplied explicitly.
     */

     public static final String DEFAULT_COUNTRY =
             "http://saxon.sf.net/feature/defaultCountry";

    /**
      * DEFAULT_LANGUAGE must be a String in the form of an ISO language code.
      * This determines the language that is used by format-date(), xsl:number etc if
      * no language code is supplied explicitly.
     */

     public static final String DEFAULT_LANGUAGE =
             "http://saxon.sf.net/feature/defaultLanguage";


    /**
    * DTD_VALIDATION must be a Boolean. This determines whether source documents should be
    * parsed with DTD-validation enabled.
    */

    public static final String DTD_VALIDATION =
            "http://saxon.sf.net/feature/validation";

    /**
     * DTD_VALIDATION_RECOVERABLE must be a Boolean. This determines whether DTD validation failures
     * should be treated as recoverable
    */

    public static final String DTD_VALIDATION_RECOVERABLE =
            "http://saxon.sf.net/feature/dtd-validation-recoverable";

    /**
     * ERROR_LISTENER_CLASS is the name of the class used to implement the JAXP ErrorListener.
     * This is used both at compile time and at run-time.
     * Currently if this option is specified, the class is instantiated, and the same instance
     * is used for all processes running under this configuration. This may change in future so that
     * a new instance is created for each compilation or evaluation.
     */

    public static final String ERROR_LISTENER_CLASS =
            "http://saxon.sf.net/feature/errorListenerClass";

    /**
     * EXPAND_ATTRIBUTE_DEFAULTS must be a Boolean; it determines whether fixed and default values defined
     * in a schema or DTD will be expanded. By default (and for conformance with the
     * specification) validation against a DTD or schema will cause default values defined in the schema
     * or DTD to be inserted into the document. Setting this feature to false suppresses this behaviour. In
     * the case of DTD-defined defaults this only works if the XML parser reports whether each attribute was
     * specified in the source or generated by expanding a default value. Not all XML parsers report this
     * information.
    */


    public static final String EXPAND_ATTRIBUTE_DEFAULTS =
            "http://saxon.sf.net/feature/expandAttributeDefaults";

    /**
     * LAZY_CONSTRUCTION_MODE must be a Boolean; it determines whether temporary trees are constructed
     * lazily. The default setting is false; there are a few situations (but not many) where setting this
     * to true can give a performance benefit (especially a memory saving)
    */

    public static final String LAZY_CONSTRUCTION_MODE =
            "http://saxon.sf.net/feature/lazyConstructionMode";    

    /**
     * LINE_NUMBERING must be a Boolean; it determines whether line and column numbers are maintained for
     * source documents. Note that some tree implementations do not support line numbering, and some may support
     * it only partially; the implementation always has the option of returning -1 as the line number or column
     * number of a node if the location is unknown. This value sets the default for the Configuration; it can be
     * overridden for individual documents, or for a PipelineConfiguration
    */

    public static final String LINE_NUMBERING =
            "http://saxon.sf.net/feature/linenumbering";

    /**
    * MESSAGE_EMITTER_CLASS must be the class name of a class that implements net.sf.saxon.event.Receiver.
    * Despite the name, it is not required to be an instance of net.sf.saxon.event.Emitter
    */

    public static final String MESSAGE_EMITTER_CLASS =
            "http://saxon.sf.net/feature/messageEmitterClass";

    /**
     * MODULE_URI_RESOLVER must be an instance of {@link net.sf.saxon.query.ModuleURIResolver}. This is a user-written
     * class that takes responsibility for locating XQuery modules.
    */

    public static final String MODULE_URI_RESOLVER =
            "http://saxon.sf.net/feature/moduleURIResolver";

    /**
     * MODULE_URI_RESOLVER_CLASS must be the name of a class that implements the interface
     * {@link net.sf.saxon.query.ModuleURIResolver}. This is a user-written
     * class that takes responsibility for locating XQuery modules.
    */

    public static final String MODULE_URI_RESOLVER_CLASS =
            "http://saxon.sf.net/feature/moduleURIResolverClass";

    /**
    * NAME_POOL must be an instance of net.sf.saxon.om.NamePool
    */

    public static final String NAME_POOL =
            "http://saxon.sf.net/feature/namePool";

    /**
     * OCCURRENCE_LIMITS is a string containing a pair of integers, comma-separated. It determines the largest
     * values of minOccurs and maxOccurs that can be accommodated when compiling an "expanded" finite state
     * machine to represent an XSD content model grammar. These limits do not apply in the common cases where
     * the grammar can be implemented using a counting finite-state-machine, but in those cases where this is
     * not possible, any minOccurs value greater than the first integer is reduced to the value of the first
     * integer, and any maxOccurs value greater than the second integer is treated as "unbounded".
     *
     * <p>This property applies to Saxon-SA only</p>
    */

    public static final String OCCURRENCE_LIMITS =
            "http://saxon.sf.net/feature/occurrenceLimits";

    /**
     * OPTIMIZATION_LEVEL must be an integer (represented as a string) in the range 0 to 10
     */

    public static final String OPTIMIZATION_LEVEL =
            "http://saxon.sf.net/feature/optimizationLevel";

    /**
     * OUTPUT_URI_RESOLVER must be an instance of {@link net.sf.saxon.OutputURIResolver}. This is a
     * user-written class that takes responsibility for disposing of the result trees produced
     * using XSLT xsl:result-document instruction
    */

    public static final String OUTPUT_URI_RESOLVER =
            "http://saxon.sf.net/feature/outputURIResolver";

    /**
     * OUTPUT_URI_RESOLVER_CLASS must be the name of a class that implements the interface
     *  {@link net.sf.saxon.OutputURIResolver}. This is a
     * user-written class that takes responsibility for disposing of the result trees produced
     * using XSLT xsl:result-document instruction
    */

    public static final String OUTPUT_URI_RESOLVER_CLASS =
            "http://saxon.sf.net/feature/outputURIResolverClass";

    /**
     * PRE_EVALUATE_DOC_FUNCTION is a boolean. If set, calls to the doc() or document() function with a statically-known
     * document URI are evaluated at compile time, so that the document only needs to be parsed and constructed once.
     * The effect of this is that subsequent changes to the contents of the file will not be reflected during
     * query or stylesheet processing
     */

    public static final String PRE_EVALUATE_DOC_FUNCTION =
            "http://saxon.sf.net/feature/preEvaluateDocFunction";

    /**
     * PREFER_JAXP_PARSER is a boolean. It has no effect when running on the Java platform. When running on the
     * .NET platform, it causes a JAXP XML parser to be used in preference to the .NET XML parser. By default,
     * the .NET XML parser (System.Xml.XmlTextReader) is used. One advantageof this option is
     * that the .NET XML parser does not report ID attributes, which means that the id() function
     * does not work.
     */

    public static final String PREFER_JAXP_PARSER =
            "http://saxon.sf.net/feature/preferJaxpParser";

    /**
	* RECOGNIZE_URI_QUERY_PARAMETERS must be a Boolean; it determines whether query parameters (things after a question mark)
     * in a URI passed to the document() or doc() function are specially recognized by the system default URIResolver.
     * Allowed parameters include, for example validation=strict to perform schema validation, and strip-space=yes
     * to perform stripping of all whitespace-only text nodes.
	*/

	public static final String RECOGNIZE_URI_QUERY_PARAMETERS =
	        "http://saxon.sf.net/feature/recognize-uri-query-parameters";

    /**
    * RECOVERY_POLICY must be an Integer: one of {@link Configuration#RECOVER_SILENTLY},
    * {@link Configuration#RECOVER_WITH_WARNINGS}, or {@link Configuration#DO_NOT_RECOVER}
    */

    public static final String RECOVERY_POLICY =
            "http://saxon.sf.net/feature/recoveryPolicy";

    /**
    * RECOVERY_POLICY_NAME must be a string: one of "recoverSilently", "recoverWithWarnings", "doNotRecover"
    */

    public static final String RECOVERY_POLICY_NAME =
            "http://saxon.sf.net/feature/recoveryPolicyName";

    /**
     * SCHEMA_URI_RESOLVER must be an instance of {@link net.sf.saxon.type.SchemaURIResolver}. This is a user-written
     * class that takes responsibility for locating schema documents.
    */

    public static final String SCHEMA_URI_RESOLVER =
            "http://saxon.sf.net/feature/schemaURIResolver";

    /**
     * SCHEMA_URI_RESOLVER_CLASS must be the name of a class that implements
     *  {@link net.sf.saxon.type.SchemaURIResolver}. This is a user-written
     * class that takes responsibility for locating schema documents.
    */

    public static final String SCHEMA_URI_RESOLVER_CLASS =
            "http://saxon.sf.net/feature/schemaURIResolverClass";    

    /**
    * SCHEMA_VALIDATION must be an Integer. This determines whether source documents should be
    * parsed with schema-validation enabled. The string takes one of the values
     * {@link net.sf.saxon.om.Validation#STRICT}, {@link net.sf.saxon.om.Validation#LAX},
     * {@link net.sf.saxon.om.Validation#PRESERVE}, {@link net.sf.saxon.om.Validation#SKIP}.
    */

    public static final String SCHEMA_VALIDATION =
            "http://saxon.sf.net/feature/schema-validation";

    /**
    * SCHEMA_VALIDATION_MODE must be a String: one of "strict", "lax", "preserve", or "skip".
    */

    public static final String SCHEMA_VALIDATION_MODE =
            "http://saxon.sf.net/feature/schema-validation-mode";

    /**
      * SERIALIZER_FACTORY_CLASS must be the full class name of a class that extends
     * {@link net.sf.saxon.event.SerializerFactory}. This class is used to customize the creation of a serializer,
     * for example by adding extra steps to the pipeline to implement user-defined serialization options.
      */

     public static final String SERIALIZER_FACTORY_CLASS =
             "http://saxon.sf.net/feature/serializerFactoryClass";


    /**
     * SOURCE_PARSER_CLASS must be the full class name of an XMLReader. This identifies the parser
     * used for source documents.
     */

    public static final String SOURCE_PARSER_CLASS =
            "http://saxon.sf.net/feature/sourceParserClass";

    /**
      * SOURCE_RESOLVER_CLASS must be the full class name of a class that extends
     * {@link net.sf.saxon.SourceResolver}. This class is used to enable Saxon to handle additional
     * implementations of {@link javax.xml.transform.Source} beyond the built-in implementations
      */

     public static final String SOURCE_RESOLVER_CLASS =
             "http://saxon.sf.net/feature/sourceResolverClass";

    /**
     * STRIP_WHITESPACE must be a string set to one of the values "all", "none", or "ignorable".
     * This determines what whitespace is stripped during tree construction: "all" removes all
     * whitespace-only text nodes; "ignorable" removes whitespace text nodes in element-only content
     * (as identified by a DTD or Schema), and "none" preserves all whitespace. This whitespace stripping
     * is additional to any stripping caused by the xsl:strip-space declaration in a stylesheet.
     *
     * <p>This option also controls stripping of whitespace by a JAXP identity Transformer or identity
     * transformerHandler.</p>
     */

    public static final String STRIP_WHITESPACE =
            "http://saxon.sf.net/feature/strip-whitespace";

    /**
     * STYLE_PARSER_CLASS must be an XMLReader. This identifies the parser used for stylesheets and
     * schema modules.
     */

    public static final String STYLE_PARSER_CLASS =
            "http://saxon.sf.net/feature/styleParserClass";

    /**
     * TIMING must be an Boolean; it determines whether basic timing information is output to System.err
     * (This attribute is a bit of a misnomer; it outputs timing information when used from the command line,
     * but also basic tracing information when used from the Java API: for example, names of output files
     * written using xsl:result-document, and names of classes dynamically loaded)
    */

    public static final String TIMING =
            "http://saxon.sf.net/feature/timing";

	/**
	* TRACE_EXTERNAL_FUNCTIONS must be a Boolean; it determines whether the loading and binding of extension
     * functions is traced
	*/

	public static final String TRACE_EXTERNAL_FUNCTIONS =
	        "http://saxon.sf.net/feature/trace-external-functions";

	/**
	* TRACE_OPTIMIZER_DECISIONS must be a Boolean; it determines whether decisions made by the optimizer
     * are traced
	*/

	public static final String TRACE_OPTIMIZER_DECISIONS =
	        "http://saxon.sf.net/feature/trace-optimizer-decisions";

    /**
    * TRACE_LISTENER must be an instance of a class that implements 
     * {@link net.sf.saxon.trace.TraceListener}. Setting this property automatically
     * sets {@link #COMPILE_WITH_TRACING} to true.
    */

    public static final String TRACE_LISTENER =
            "http://saxon.sf.net/feature/traceListener";

    /**
    * TRACE_LISTENER_CLASS must be the name of a class that implements
     * {@link net.sf.saxon.trace.TraceListener}. A new instance of this class will be created
     * as the trace listener for each query or transformation run under this Configuration.
     * Setting this property automatically
     * sets {@link #COMPILE_WITH_TRACING} to true.
    */

    public static final String TRACE_LISTENER_CLASS =
            "http://saxon.sf.net/feature/traceListenerClass";


    /**
	* TREE_MODEL must be an Integer: {@link net.sf.saxon.event.Builder#LINKED_TREE},
     *  {@link net.sf.saxon.event.Builder#TINY_TREE}, or {@link net.sf.saxon.event.Builder#TINY_TREE_CONDENSED}
	*/

	public static final String TREE_MODEL =
	        "http://saxon.sf.net/feature/treeModel";

   /**
	* TREE_MODEL_NAME must be a string: "linkedTree" or "tinyTree" or "tinyTreeCondensed"
	*/

	public static final String TREE_MODEL_NAME =
	        "http://saxon.sf.net/feature/treeModelName";    

    /**
     * URI_RESOLVER_CLASS must be the full class name of a class that extends
     * {@link javax.xml.transform.URIResolver}. Setting this property causes the specified class
     * to be instantiated, and the instance is then registered using {@link Configuration#setURIResolver}.
     * This affects the dereferencing of URIs both at compile-time and at run-time.
     */

     public static final String URI_RESOLVER_CLASS =
             "http://saxon.sf.net/feature/uriResolverClass";
    
    /**
     * USE_PI_DISABLE_OUTPUT_ESCAPING must be a Boolean. This determines whether a TransformerHandler
     * created with this Factory or Configuration recognizes the processing instructions
     * <code>Result.PI_DISABLE_OUTPUT_ESCAPING</code> and <code>Result.PI_ENABLE_OUTPUT_ESCAPING</code>
     * in the input stream as instructions to disable or to re-enable output escaping.
    */

    public static final String USE_PI_DISABLE_OUTPUT_ESCAPING =
            "http://saxon.sf.net/feature/use-pi-disable-output-escaping";

    /**
     * USE_TYPED_VALUE_CACHE is a Boolean. The value is relevant only when the TinyTree is used;
     * it determines whether (for a validated document) a cache will be maintained containing the
     * typed values of nodes. Typed values are held in the cache only for elements and attributes whose
     * type is other than string, untypedAtomic, or anyURI. The default value is true. Setting this
     * value to false can reduce memory requirements and the cost of requiring recomputation
     * of typed values on each access.
     */

    public static final String USE_TYPED_VALUE_CACHE =
            "http://saxon.sf.net/feature/use-typed-value-cache";

    /**
     * USE_XSI_SCHEMA_LOCATION must be a Boolean. This determines whether the schema processor
     * takes notice of (and attempts to dereference) URIs specified in the xsi:schemaLocation
     * and xsi:noNamespaceSchemaLocation attributes of the instance document. The default is true.
    */

    public static final String USE_XSI_SCHEMA_LOCATION =
            "http://saxon.sf.net/feature/useXsiSchemaLocation";


    /**
    * VALIDATION_WARNINGS must be a Boolean. This determines whether validation errors in result
    * documents should be treated as fatal. By default they are fatal; with this option set, they
    * are treated as warnings.
    */

    public static final String VALIDATION_WARNINGS =
            "http://saxon.sf.net/feature/validation-warnings";

    /**
    * VERSION_WARNING must be a Boolean. This determines whether a warning should be output when
     * running an XSLT 2.0 processor against an XSLT 1.0 stylesheet. The XSLT specification requires
     * this to be done by default.
    */

    public static final String VERSION_WARNING =
            "http://saxon.sf.net/feature/version-warning";

    /**
     * XINCLUDE must be a Boolean. This determines whether XInclude processing should be performed by default
     * when XML source documents (including stylesheets and schemas) are read.
    */

    public static final String XINCLUDE =
            "http://saxon.sf.net/feature/xinclude-aware";

    /**
     * XML_VERSION is a character string. This determines the XML version used by the Configuration: the
     * value must be "1.0" or "1.1". For details, see {@link Configuration#setXMLVersion(int)}.
     *
     * <p>Note that source documents specifying xml version="1.0" or "1.1" are accepted
     * regardless of this setting. The effect of this switch is to change the validation
     * rules for types such as Name and NCName, to change the meaning of \i and \c in
     * regular expressions, and to determine whether the serializer allows XML 1.1 documents
     * to be constructed. </p>
     */

    public static final String XML_VERSION =
            "http://saxon.sf.net/feature/xml-version";

    /**
     * XQUERY_ALLOW_UPDATE is a boolean. If true, XQuery update syntax is accepted; if false, it is not.
     */

    public static final String XQUERY_ALLOW_UPDATE =
            "http://saxon.sf.net/feature/xqueryAllowUpdate";

    /**
     * XQUERY_CONSTRUCTION_MODE is a string with the value "strip" or "preserve". It defines the default value
     * of construction mode in the XQuery static context (overridable in the query prolog)
     */

    public static final String XQUERY_CONSTRUCTION_MODE =
            "http://saxon.sf.net/feature/xqueryConstructionMode";

    /**
     * XQUERY_DEFAULT_ELEMENT_NAMESPACE is a namespace URI. It defines the default namespace for elements and types
     * that are not qualified by a namespace prefix.
     */

    public static final String XQUERY_DEFAULT_ELEMENT_NAMESPACE =
            "http://saxon.sf.net/feature/xqueryDefaultElementNamespace";

    /**
     * XQUERY_DEFAULT_FUNCTION_NAMESPACE is a namespace URI. It defines the default namespace for function names
     * that are not qualified by a namespace prefix.
     */

    public static final String XQUERY_DEFAULT_FUNCTION_NAMESPACE =
            "http://saxon.sf.net/feature/xqueryDefaultFunctionNamespace";

    /**
     * XQUERY_EMPTY_LEAST is boolean. It defines how the empty sequence is handled in XQuery sorting (the "order by"
     * clause). If true, () comes at the start of the sorted sequence; if false, it comes last.
     */

    public static final String XQUERY_EMPTY_LEAST =
            "http://saxon.sf.net/feature/xqueryEmptyLeast";

    /**
     * XQUERY_INHERIT_NAMESPACES is boolean. It defines the default value of the inherit-namespaces property
     * in the XQuery static context.
     */

    public static final String XQUERY_INHERIT_NAMESPACES =
            "http://saxon.sf.net/feature/xqueryInheritNamespaces";

    /**
     * XQUERY_PRESERVE_BOUNDARY_SPACE is boolean. It defines whether "boundary space" (insignificant space in
     * direct element constructors) should be retained or not
     */

    public static final String XQUERY_PRESERVE_BOUNDARY_SPACE =
            "http://saxon.sf.net/feature/xqueryPreserveBoundarySpace";

    /**
     * XQUERY_PRESERVE_NAMESPACES is boolean. It defines whether unused namespace declarations are retained
     * by XQuery element copy operations
     */

    public static final String XQUERY_PRESERVE_NAMESPACES =
            "http://saxon.sf.net/feature/xqueryPreserveNamespaces";

    /**
     * XQUERY_REQUIRED_CONTEXT_ITEM_TYPE is a string containing an ItemType, for example <code>document-node()</code>.
     * It defines the default expected context item type for a query.
     */

    public static final String XQUERY_REQUIRED_CONTEXT_ITEM_TYPE =
            "http://saxon.sf.net/feature/xqueryRequiredContextItemType";

    /**
     * XQUERY_SCHEMA_AWARE is a boolean. It indicates that queries should be compiled to handle schema-typed input
     * documents, even if they contain no "import schema" declaration.
     */

    public static final String XQUERY_SCHEMA_AWARE =
            "http://saxon.sf.net/feature/xquerySchemaAware";

    /**
     * XQUERY_STATIC_ERROR_LISTENER_CLASS is the name of a Java class that implements
     * {@link javax.xml.transform.ErrorListener}. All reports of static errors in a query will go to this
     * ErrorListener.
     */

    public static final String XQUERY_STATIC_ERROR_LISTENER_CLASS =
            "http://saxon.sf.net/feature/xqueryStaticErrorListenerClass";

    /**
     * XQUERY_VERSION is a character string. This determines the version of XQuery used by the Configuration: the
     * value must be "1.0" or "1.1".
     */

    public static final String XQUERY_VERSION =
            "http://saxon.sf.net/feature/xqueryVersion";

    /**
     * XSD_VERSION is a character string. This determines the version of XML Schema used by the Configuration: the
     * value must be "1.0" or "1.1".
     */

    public static final String XSD_VERSION =
            "http://saxon.sf.net/feature/xsd-version";

    /**
     * XSLT_SCHEMA_AWARE is a boolean. It indicates whether stylesheets should be compiled with the ability to
     * handle schema-typed input documents. By default a stylesheet is compiled to handle such input if it contains
     * an <code>xsl:import-schema</code> instruction, and not otherwise.
     */

    public static final String XSLT_SCHEMA_AWARE =
            "http://saxon.sf.net/feature/xsltSchemaAware";
    /**
     * XSLT_STATIC_ERROR_LISTENER_CLASS must be the name of a class that implements {@link javax.xml.transform.ErrorListener}.
     * This is an ErrorListener used to handle static errors found when compiling a stylesheet.
    */

    public static final String XSLT_STATIC_ERROR_LISTENER_CLASS =
            "http://saxon.sf.net/feature/stylesheetErrorListener";

    /**
     * XSLT_STATIC_URI_RESOLVER_CLASS must be the name of a class that implements {@link javax.xml.transform.URIResolver}.
     * This is a URIResolver used when dereferencing the URIs that appear in the <code>href</code> attributes of the
     * <code>xsl:include</code> and <code>xsl:import</code> declarations. Note that this defaults to the
     * setting of the global <code>URI_RESOLVER</code> property.
    */

    public static final String XSLT_STATIC_URI_RESOLVER_CLASS =
            "http://saxon.sf.net/feature/stylesheetURIResolver";

    private FeatureKeys() {
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
