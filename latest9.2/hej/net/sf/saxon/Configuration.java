package net.sf.saxon;

import net.sf.saxon.charcode.CharacterSetFactory;
import net.sf.saxon.event.*;
import net.sf.saxon.evpull.PullEventSource;
import net.sf.saxon.expr.*;
import net.sf.saxon.functions.*;
import net.sf.saxon.instruct.*;
import net.sf.saxon.number.Numberer;
import net.sf.saxon.number.Numberer_en;
import net.sf.saxon.om.*;
import net.sf.saxon.pull.PullSource;
import net.sf.saxon.query.ModuleURIResolver;
import net.sf.saxon.query.StandardModuleURIResolver;
import net.sf.saxon.query.StaticQueryContext;
import net.sf.saxon.sort.CollationURIResolver;
import net.sf.saxon.sort.StandardCollationURIResolver;
import net.sf.saxon.sort.StringCollator;
import net.sf.saxon.style.StyleNodeFactory;
import net.sf.saxon.sxpath.IndependentContext;
import net.sf.saxon.trace.TraceListener;
import net.sf.saxon.trans.*;
import net.sf.saxon.type.*;
import net.sf.saxon.value.SequenceType;
import net.sf.saxon.value.Whitespace;
import org.xml.sax.*;
import org.xml.sax.ext.DefaultHandler2;
import org.xml.sax.ext.LexicalHandler;

import javax.xml.transform.*;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.sax.SAXSource;
import javax.xml.transform.stream.StreamSource;
import java.io.InputStream;
import java.io.Serializable;
import java.io.IOException;
import java.util.*;
import java.util.Collection;
import java.net.URL;


/**
 * This class holds details of user-selected configuration options for a set of transformations
 * and/or queries. When running XSLT, the preferred way of setting configuration options is via
 * the JAXP TransformerFactory interface, but the Configuration object provides a finer
 * level of control. As yet there is no standard API for XQuery, so the only way of setting
 * Configuration information is to use the methods on this class directly.
 * <p/>
 * <p>As well as holding configuration settings, this class acts as a factory for classes
 * providing service in particular areas: error handling, URI resolution, and the like. Some
 * of these services are chosen on the basis of the current platform (Java or .NET), some vary
 * depending whether the environment is schema-aware or not.</p>
 * <p/>
 * <p>The <code>Configuration</code> provides access to a {@link NamePool} which is used to manage
 * all the names used in stylesheets, queries, schemas, and source and documents: the NamePool
 * allocates integer codes to these names allowing efficient storage and comparison. Normally
 * there will be a one-to-one relationship between a <code>NamePool</code> and a <code>Configuration</code>.
 * It is possible, however, for several <code>Configuration</code> objects to share the same
 * <code>NamePool</code>. Until Saxon 8.9, by default all <code>Configuration</code> objects
 * shared a single <code>NamePool</code> unless configured otherwise; this changed in 8.9 so that
 * the default is to allocate a new <code>NamePool</code> for each <code>Configuration</code>.</p>
 * <p/>
 * <p>The <code>Configuration</code> establishes the scope within which node identity is managed.
 * Every document belongs to a <code>Configuration</code>, and every node has a distinct identity
 * within that <code>Configuration</code>. In consequence, it is not possible for any query or
 * transformation to manipulate multiple documents unless they all belong to the same
 * <code>Configuration</code>.</p>
 * <p/>
 * <p>Saxon-EE has a subclass of the <code>Configuration</code> class which provides the additional
 * services needed for schema-aware processing. The {@link com.saxonica.config.EnterpriseConfiguration}
 * also holds a cache of loaded schema components used for compiling schema-aware transformations
 * and queries, and for validating instance documents.</p>
 * <p/>
 * <p>Since Saxon 8.4, the JavaDoc documentation for Saxon attempts to identify interfaces
 * that are considered stable, and will only be changed in a backwards-incompatible way
 * if there is an overriding reason to do so. These interfaces and methods are labelled
 * with the JavaDoc "since" tag. The value 8.n indicates a method in this category that
 * was introduced in Saxon version 8.n: or in the case of 8.4, that was present in Saxon 8.4
 * and possibly in earlier releases. (In some cases, these methods have been unchanged for
 * a long time.) Methods without a "since" tag, although public, are provided for internal
 * use or for use by advanced users, and are subject to change from one release to the next.
 * The presence of a "since" tag on a class or interface indicates that there are one or more
 * methods in the class that are considered stable; it does not mean that all methods are
 * stable.
 *
 * @since 8.4
 */


public class Configuration implements Serializable, SourceResolver {

    private static Platform platform;
    public static Class<Configuration> configurationClass;
    private transient Object apiProcessor = null;
    private transient URIResolver uriResolver;
    private StandardURIResolver systemURIResolver = new StandardURIResolver(this);
    private int xmlVersion = XML10;
    protected int xsdVersion = XSD10;
    private boolean tracing = false;
    private boolean traceOptimizations = false;
    private transient TraceListener traceListener = null;
    private String traceListenerClass = null;

    private IntegratedFunctionLibrary integratedFunctionLibrary = new IntegratedFunctionLibrary();
    

    protected VendorFunctionLibrary vendorFunctionLibrary;
    private CollationURIResolver collationResolver = new StandardCollationURIResolver();
    private String defaultCollection = null;
    private CollationMap collationMap = new CollationMap(this);

    private CollectionURIResolver collectionResolver = new StandardCollectionURIResolver();
    private ModuleURIResolver moduleURIResolver = null;
    private ModuleURIResolver standardModuleURIResolver = StandardModuleURIResolver.getInstance();
    private SchemaURIResolver schemaURIResolver = null;
    private transient SourceResolver sourceResolver = this;
    private String sourceParserClass;
    private String styleParserClass;
    private boolean preferJaxpParser = true;
    private boolean timing = false;
    private boolean allowExternalFunctions = true;
    private boolean traceExternalFunctions = false;
    private boolean useTypedValueCache = true;
    private boolean lazyConstructionMode = false;
    private boolean allowMultiThreading = false;
    private boolean preEvaluateDocFunction = false;
    private boolean useDisableOutputEscaping = false;
    private NamePool namePool = new NamePool();
    private DocumentNumberAllocator documentNumberAllocator = new DocumentNumberAllocator();
    private DocumentPool globalDocumentPool = new DocumentPool();
    private transient XPathContext conversionContext = null;
    private transient TypeHierarchy typeHierarchy;

    private int hostLanguage = XSLT;
    private boolean validationWarnings = false;
    private boolean retainDTDattributeTypes = false;

    private ParseOptions defaultParseOptions = new ParseOptions();
    private transient Debugger debugger = null;
    protected Optimizer optimizer = null;
    protected int optimizationLevel = Optimizer.FULL_OPTIMIZATION;
    private transient DynamicLoader dynamicLoader = new DynamicLoader();
    protected transient StyleNodeFactory styleNodeFactory = null;

    private SerializerFactory serializerFactory = new SerializerFactory();
    private Properties defaultSerializationProperties = new Properties();
    private transient CharacterSetFactory characterSetFactory;
    private String defaultLanguage = Locale.getDefault().getLanguage();
    private String defaultCountry = Locale.getDefault().getCountry();

    private transient LocalizerFactory localizerFactory;

    private CompilerInfo defaultXsltCompilerInfo = new CompilerInfo();

    private transient StaticQueryContext defaultStaticQueryContext;

    //private int implicitTimezone;
    private transient List<XMLReader> sourceParserPool = new ArrayList<XMLReader>(5);
    private transient List<XMLReader> styleParserPool = new ArrayList<XMLReader>(5);



    private List<ExternalObjectModel> externalObjectModels = new ArrayList<ExternalObjectModel>(4);
    private int domLevel = 3;


    /**
     * Constant indicating that the processor should take the recovery action
     * when a recoverable error occurs, with no warning message.
     */
    public static final int RECOVER_SILENTLY = 0;
    /**
     * Constant indicating that the processor should produce a warning
     * when a recoverable error occurs, and should then take the recovery
     * action and continue.
     */
    public static final int RECOVER_WITH_WARNINGS = 1;
    /**
     * Constant indicating that when a recoverable error occurs, the
     * processor should not attempt to take the defined recovery action,
     * but should terminate with an error.
     */
    public static final int DO_NOT_RECOVER = 2;

    /**
     * Constant indicating the XML Version 1.0
     */

    public static final int XML10 = 10;

    /**
     * Constant indicating the XML Version 1.1
     */

    public static final int XML11 = 11;

    /**
     * Constant indicating that the host language is XSLT
     */
    public static final int XSLT = 50;

    /**
     * Constant indicating that the host language is XQuery
     */
    public static final int XQUERY = 51;

    /**
     * Constant indicating that the "host language" is XML Schema
     */
    public static final int XML_SCHEMA = 52;

    /**
     * Constant indicating that the host language is Java: that is, this is a free-standing
     * Java application with no XSLT or XQuery content
     */
    public static final int JAVA_APPLICATION = 53;

    /**
     * Constant indicating that the host language is XPATH itself - that is, a free-standing XPath environment
     */
    public static final int XPATH = 54;

    /**
     * Language versions for XML Schema
     */
    public static int XSD10 = 10;
    public static int XSD11 = 11;

    static {
        String messages = "";
        try {
            String sConfigFile = "edition.properties";

            ClassLoader loader = null;
            try {
                loader = Thread.currentThread().getContextClassLoader();
            } catch (Exception err) {
                messages += "Failed to getContextClassLoader() - continuing\n";
            }

            InputStream in = null;

            if (loader != null) {
                in = loader.getResourceAsStream(sConfigFile);
                if (in == null) {
                    messages += "Cannot read edition.properties file located using ClassLoader " +
                            loader + " - continuing\n";
                }
            }

            if (in == null) {
                loader = Configuration.class.getClassLoader();
                if (loader != null) {
                    in = loader.getResourceAsStream(sConfigFile);
                    if (in == null) {
                        messages += "Cannot read edition.properties file located using ClassLoader " +
                                loader + " - continuing\n";
                    }
                }
            }

            if (in == null) {
                // Means we're in a very strange class-loading environment, things are getting desparate
                URL url = ClassLoader.getSystemResource(sConfigFile);
                if (url != null) {
                    try {
                        in = url.openStream();
                    } catch (IOException ioe) {
                        messages += "IO error " + ioe.getMessage() +
                                " reading edition.properties located using getSystemResource(): using defaults";
                        in = null;
                    }
                }
            }

            String platformClassName;
            String configClassName;

            if (in == null) {
                System.err.println(messages);
                platformClassName = "net.sf.saxon.java.JavaPlatform";
                configClassName = "net.sf.saxon.Configuration";
            } else {
                Properties props = new Properties();
                props.load(in);
                platformClassName = props.getProperty("platform");
                if (platformClassName == null) {
                    platformClassName = "net.sf.saxon.java.JavaPlatform";
                }
                configClassName = props.getProperty("config");
                if (configClassName == null) {
                    configClassName = "net.sf.saxon.Configuration";
                }
            }

            Class platformClass;
            if (loader != null) {
                try {
                    platformClass = loader.loadClass(platformClassName);
                } catch (Exception ex) {
                    platformClass = Class.forName(platformClassName);
                }
            } else {
                platformClass = Class.forName(platformClassName);
            }
            platform = (Platform)platformClass.newInstance();

            Class configClass;
            if (loader != null) {
                try {
                    configClass = loader.loadClass(configClassName);
                } catch (Exception ex) {
                    configClass = Class.forName(configClassName);
                }
            } else {
                configClass = Class.forName(configClassName);
            }
            configurationClass = (Class<Configuration>)configClass;
        } catch (Exception e) {
            System.err.println(messages);
            e.printStackTrace();
            throw new RuntimeException("Failed to load configuration defined in edition.properties", e);
        }
    }
    
    /**
     * Factory method to construct a Configuration object by reading a configuration file.
     * Requires Saxon-PE or Saxon-EE
     * @param source Source object containing the configuration file
     * @return the resulting Configuration
     */

    public static Configuration readConfiguration(Source source) throws XPathException {
        try {
            Class readerClass = Class.forName("com.saxonica.config.ConfigurationReader");
            IConfigurationReader reader = (IConfigurationReader)readerClass.newInstance();
            return reader.makeConfiguration(source);
        } catch (ClassNotFoundException e) {
            throw new XPathException("Could not locate ConfigurationReader: needs Saxon-PE or Saxon-EE");
        } catch (InstantiationException e) {
            throw new XPathException(e);
        } catch (IllegalAccessException e) {
            throw new XPathException(e);
        }

    }


    /**
     * Create a non-schema-aware configuration object with default settings for all options.
     *
     * @since 8.4
     */

    public Configuration() {
        init();
    }

    /**
     * Factory method to create a Configuration, of the class specified in the edition.properties
     * properties file: that is, the type of Configuration appropriate to the edition of the software
     * being used. This method does not check that the Configuration is licensed.
     * @return a Configuration object of the class appropriate to the Saxon edition in use.
     * @since 9.2
     */

    public static Configuration newConfiguration() {
        //System.err.println("New configuration: " + configurationClass.getName());
        try {
            return configurationClass.newInstance();
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("Cannot instantiate a Configuration", e);
        }
    }

    /**
     * Factory method to create a Configuration. The resulting configuration will be an enterprise Configuration if
     * Saxon-EE is installed and if a license is available; otherwise it will be a basic configuration.
     * Note that the license might not permit all processing options.
     *
     * @param classLoader - the class loader to be used. If null, the context class loader for the current
     *                    thread is used.
     * @param className   - the name of the schema aware configuration class. Defaults to
     *                    "com.saxonica.EnterpriseConfiguration" if null is supplied. This allows an assembly
     *                    qualified name to be supplied under .NET. The class, once instantiated, must be an instance
     *                    of Configuration, but there is nothing that requires it to
     *                    be schema-aware.
     * @return an EnterpriseConfiguration object if Saxon-EE can be loaded and a valid license is
     *         installed; otherwise, a non-schema-aware configuration object
     * @deprecated since 9.2: use newConfiguration() instead
     * @since 9.0
     */

    public static Configuration makeConfiguration(ClassLoader classLoader, String className) {
        if (className == null) {
            className = configurationClass.getClass().getName();
        }
        try {
            Class theClass;
            ClassLoader loader = classLoader;
            if (loader == null) {
                try {
                    loader = Thread.currentThread().getContextClassLoader();
                } catch (Exception err) {
                    System.err.println("Failed to getContextClassLoader() - continuing");
                }
            }
            if (loader != null) {
                try {
                    theClass = loader.loadClass(className);
                } catch (Exception ex) {
                    theClass = Class.forName(className);
                }
            } else {
                theClass = Class.forName(className);
            }
            Configuration c = (Configuration)theClass.newInstance();
            if (c.isLicensed()) {
                return c;
            } else {
                return new Configuration();
            }
        } catch (ClassNotFoundException e) {
            return new Configuration();
        } catch (InstantiationException e) {
            return new Configuration();
        } catch (IllegalAccessException e) {
            return new Configuration();
        }


    }


    protected void init() {
        platform.initialize(this);
        defaultXsltCompilerInfo.setURIResolver(getSystemURIResolver());

        // Get the implicit timezone from the current system clock
        //GregorianCalendar calendar = new GregorianCalendar();
        //int tzmsecs = (calendar.get(Calendar.ZONE_OFFSET) + calendar.get(Calendar.DST_OFFSET));
        //implicitTimezone = tzmsecs / 60000;
    }

    /**
     * Static method to instantiate a professional or enterprise configuration.
     * <p>On the .NET platform, this method should not be called unless it is known that the assembly
     * saxon9sa.dll has already been loaded. This can be achieved by an appropriate call on Assembly.Load():
     * for an example, see the C# Configuration.cs class in the Saxon.Api namespace.</p>
     * <p>This method fails if Saxon-EE cannot be loaded, but it does not fail if there is no license
     * available. In that case it returns a schema-aware configuration object, but any attempt to use
     * schema-aware processing will fail.
     *
     * @param classLoader - the class loader to be used. If null, the context class loader for the current
     *                    thread is used.
     * @param className   - the name of the configuration class. Defaults to
     *                    "com.saxonica.config.ProfessionalConfiguration" if null is supplied. This allows an assembly
     *                    qualified name to be supplied under .NET. The class, once instantiated, must be an instance
     *                    of Configuration.
     * @return the new EnterpriseConfiguration
     * @throws RuntimeException if the Saxon-EE product cannot be loaded
     * @since 9.2 (renamed from makeSchemaAwareConfiguration)
     */

    public static Configuration makeLicensedConfiguration(ClassLoader classLoader, String className)
            throws RuntimeException {
        if (className == null) {
            className = "com.saxonica.config.ProfessionalConfiguration";
        }
        try {
            Class theClass;
            ClassLoader loader = classLoader;
            if (loader == null) {
                try {
                    loader = Thread.currentThread().getContextClassLoader();
                } catch (Exception err) {
                    System.err.println("Failed to getContextClassLoader() - continuing");
                }
            }
            if (loader != null) {
                try {
                    theClass = loader.loadClass(className);
                } catch (Exception ex) {
                    theClass = Class.forName(className);
                }
            } else {
                theClass = Class.forName(className);
            }
            return (Configuration)theClass.newInstance();
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
        } catch (InstantiationException e) {
            throw new RuntimeException(e);
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Make an enterprise configuration
     * @param loader the class loader (or null)
     * @param className the name of the configuration class (or null)
     * @deprecated since 9.2. Use {@link #makeLicensedConfiguration} instead.
     */

    public static Configuration makeSchemaAwareConfiguration(ClassLoader loader, String className) {
        return makeLicensedConfiguration(loader, className);
    }

    /**
     * Get the edition code identifying this configuration: "HE", "PE" or "EE"
     */

    public String getEditionCode() {
        return "HE";
    }

    /**
     * Copy an existing Configuration to create a new Configuration.
     * This is a shallow copy. The new Configuration will share all the option settings of the old;
     * it will also share the same NamePool, and the same DocumentNumberAllocator. If this configuration
     * is schema-aware then the new one will also be schema-aware, and will share the same Schema manager
     * and so on. (So any schema component loaded into one configuration will affect both).
     * <p>This method may replace a ProfessionalConfiguration by an EnterpriseConfiguration 
     * according to what is found in the installed license.</p>
     * <p>Note that creating a new EnterpriseConfiguration using this method can be significantly cheaper
     * than creating one from scratch, because it avoids the need to verify the Saxon-EE license key if this
     * has already been done.</p>
     *
     * @return a shallow copy of this Configuration
     */

    public Configuration copy() {
        Configuration c = new Configuration();
        copyTo(c);
        return c;
    }


    protected void copyTo(Configuration c) {
        c.uriResolver = uriResolver;
        c.systemURIResolver = systemURIResolver;
        //c.listener = listener;
        c.xmlVersion = xmlVersion;
        //c.treeModel = treeModel;
        //c.lineNumbering = lineNumbering;
        c.tracing = tracing;
        c.traceOptimizations = traceOptimizations;
        c.traceListener = traceListener;
        c.integratedFunctionLibrary = (IntegratedFunctionLibrary)integratedFunctionLibrary.copy();
        c.vendorFunctionLibrary = vendorFunctionLibrary;

        c.collectionResolver = collectionResolver;
        c.defaultCollection = defaultCollection;
        c.collationResolver = collationResolver;
        c.collationMap = new CollationMap(collationMap);

        if (localizerFactory != null) {
            c.localizerFactory = localizerFactory.copy();
        }

        c.moduleURIResolver = moduleURIResolver;
        c.standardModuleURIResolver = standardModuleURIResolver;
        c.schemaURIResolver = schemaURIResolver;
        c.sourceResolver = sourceResolver;
        c.sourceParserClass = sourceParserClass;
        c.styleParserClass = styleParserClass;
        //c.outputURIResolver = outputURIResolver;
        c.timing = timing;
        c.defaultXsltCompilerInfo = new CompilerInfo(defaultXsltCompilerInfo);
        c.defaultStaticQueryContext = new StaticQueryContext(getDefaultStaticQueryContext());
        c.allowExternalFunctions = allowExternalFunctions;
        c.lazyConstructionMode = lazyConstructionMode;
        c.allowMultiThreading = allowMultiThreading;
        c.preEvaluateDocFunction = preEvaluateDocFunction;
        c.namePool = namePool;
        c.documentNumberAllocator = documentNumberAllocator;
        c.conversionContext = conversionContext;
        c.typeHierarchy = typeHierarchy;
        c.hostLanguage = hostLanguage;
        c.defaultParseOptions = new ParseOptions(defaultParseOptions);
        c.validationWarnings = validationWarnings;
        c.retainDTDattributeTypes = retainDTDattributeTypes;
        c.debugger = debugger;
        c.optimizer = optimizer;
        c.serializerFactory = serializerFactory;
        c.characterSetFactory = characterSetFactory;
        c.dynamicLoader = dynamicLoader;
        c.sourceParserPool = sourceParserPool;
        c.externalObjectModels = externalObjectModels;
        c.domLevel = domLevel;
    }

    /**
     * Save the Processor object that owns this Configuration in the relevant API.
     * @param processor This can be any object, but it is actually used to hold one of the
     * following:
     * <ul>
     *   <li>When using the Java s9api interface, the <code>net.sf.saxon.s9api.Processor</code></li>
     *   <li>When using the .NET interface, the <code>Saxon.Api.Processor</code></li>
     *   <li>When using the JAXP transformation interface, the JAXP <code>TransformerFactory</code></li>
     *   <li>When using the JAXP XPath interface, the JAXP <code>XPathFactory</code></li>
     *   <li>When using the JAXP Schema interface, the JAXP <code>SchemaFactory</code></li>
     *   <li>When using XQJ, the <code>XQDataSource</code></li>
     * </ul>
     * @since 9.2
     */

    public void setProcessor(Object processor) {
        this.apiProcessor = processor;
    }

    /**
     * Get the Processor object that created this Configuration in the relevant API.
     * <p>The main purpose of this interface is to allow extension functions called from
     * a stylesheet or query to get access to the originating processor. This is particularly
     * useful when methods are static as there is then limited scope for passing data from the
     * calling application to the code of the extension function.</p>
     * @return the processor that was supplied to the {@link #setProcessor(Object)} method, or null
     * if this method has not been called. In practice this property is used to hold one of the
     * following:
     * <ul>
     *   <li>When using the Java s9api interface, the <code>net.sf.saxon.s9api.Processor</code></li>
     *   <li>When using the .NET interface, the <code>Saxon.Api.Processor</code></li>
     *   <li>When using the JAXP transformation interface, the JAXP <code>TransformerFactory</code></li>
     *   <li>When using the JAXP XPath interface, the JAXP <code>XPathFactory</code></li>
     *   <li>When using the JAXP Schema interface, the JAXP <code>SchemaFactory</code></li>
     *   <li>When using XQJ, the <code>XQDataSource</code></li>
     * </ul>
     * @since 9.2
     */
    
    public Object getProcessor() {
        return apiProcessor;
    }

    /**
     * Get a message used to identify this product when a transformation is run using the -t option
     *
     * @return A string containing both the product name and the product
     *         version
     * @since 8.4
     */

    public String getProductTitle() {
        return "Saxon-" + getEditionCode() + " " + Version.getProductVersion() + platform.getPlatformSuffix() + " from Saxonica";
    }

    /**
     * Determine if the functionality available in this configuration is licensed.
     * @return true for Saxon-HE, since all Saxon-HE functionality is always available
     * @since 9.2
     */

    public boolean isLicensed() {
        return true;
    }

    /**
     * Determine if a particular feature is licensed.
     * @param feature the feature in question, identified by a constant in class {@link LicenseFeature}
     */

    public boolean isLicensedFeature(int feature) {
        return false;
    }

    /**
     * Determine if the configuration is schema-aware, for the given host language
     *
     * @param language the required host language: XSLT, XQUERY, or XML_SCHEMA
     * @return true if the configuration is schema-aware
     * @since 8.4
     * @deprecated since 9.2: use isLicensedFeature() instead
     */

    public boolean isSchemaAware(int language) {
        return false;
        // changing this to true will do no good!
    }

    /**
     * Display a message about the license status
     */

    public void displayLicenseMessage() {
    }

    /**
     * Get the host language used in this configuration. The typical values
     * are XSLT and XQUERY. The values XML_SCHEMA and JAVA_APPLICATION may also
     * be encountered.
     * <p/>
     * This method is problematic because it is possible to run multiple transformations
     * or queries within the same configuration. The method is therefore best avoided.
     * Instead, use {@link net.sf.saxon.expr.Container#getHostLanguage}.
     * Internally its only use is in deciding (in Saxon-EE only) which error listener to
     * use by default at compile time, and since the standard XSLT and XQuery listeners have
     * no differences when used for static errors, the choice is immaterial.
     *
     * @return Configuration.XSLT or Configuration.XQUERY
     */

    public int getHostLanguage() {
        return hostLanguage;
    }

    /**
     * Set the host language used in this configuration. The possible values
     * are XSLT and XQUERY.
     *
     * @param hostLanguage Configuration.XSLT or Configuration.XQUERY
     */

    public void setHostLanguage(int hostLanguage) {
        this.hostLanguage = hostLanguage;
    }

    /**
     * Get the Platform to be used for platform-dependent methods
     *
     * @return the platform to be used
     */

    public static Platform getPlatform() {
        return platform;
    }

    /**
     * Set the DynamicLoader to be used. By default an instance of {@link DynamicLoader} is used
     * for all dynamic loading of Java classes. This method allows the actions of the standard
     * DynamicLoader to be overridden
     *
     * @param dynamicLoader the DynamicLoader to be used by this Configuration
     */

    public void setDynamicLoader(DynamicLoader dynamicLoader) {
        this.dynamicLoader = dynamicLoader;
    }

    /**
     * Get the DynamicLoader used by this Configuration. By default the standard system-supplied
     * dynamic loader is returned.
     *
     * @return the DynamicLoader in use - either a user-supplied DynamicLoader, or the standard one
     *         supplied by the system.
     */

    public DynamicLoader getDynamicLoader() {
        return dynamicLoader;
    }

    /**
     * Load a class using the class name provided.
     * Note that the method does not check that the object is of the right class.
     * <p/>
     * This method is intended for internal use only. The call is delegated to the
     * <code>DynamicLoader</code>, which may be overridden by a user-defined <code>DynamicLoader</code>.
     *
     * @param className   A string containing the name of the
     *                    class, for example "com.microstar.sax.LarkDriver"
     * @param tracing     true if diagnostic tracing is required
     * @param classLoader The ClassLoader to be used to load the class, or null to
     *                    use the ClassLoader selected by the DynamicLoader.
     * @return an instance of the class named, or null if it is not
     *         loadable.
     * @throws XPathException if the class cannot be loaded.
     */

    public Class getClass(String className, boolean tracing, ClassLoader classLoader) throws XPathException {
        return dynamicLoader.getClass(className, tracing, classLoader);
    }

    /**
     * Instantiate a class using the class name provided.
     * Note that the method does not check that the object is of the right class.
     * <p/>
     * This method is intended for internal use only. The call is delegated to the
     * <code>DynamicLoader</code>, which may be overridden by a user-defined <code>DynamicLoader</code>.
     * <p/>
     * Diagnostic output is produced if the option "isTiming" is set (corresponding to the -t option on
     * the command line).
     *
     * @param className   A string containing the name of the
     *                    class, for example "com.microstar.sax.LarkDriver"
     * @param classLoader The ClassLoader to be used to load the class, or null to
     *                    use the ClassLoader selected by the DynamicLoader.
     * @return an instance of the class named, or null if it is not
     *         loadable.
     * @throws XPathException if the class cannot be loaded.
     */

    public Object getInstance(String className, ClassLoader classLoader) throws XPathException {
        return dynamicLoader.getInstance(className, isTiming(), classLoader);
    }

    /**
     * Get the URIResolver used in this configuration
     *
     * @return the URIResolver. If no URIResolver has been set explicitly, the
     *         default URIResolver is used.
     * @since 8.4
     */

    public URIResolver getURIResolver() {
        if (uriResolver == null) {
            return systemURIResolver;
        }
        return uriResolver;
    }

    /**
     * Set the URIResolver to be used in this configuration. This will be used to
     * resolve the URIs used statically (e.g. by xsl:include) and also the URIs used
     * dynamically by functions such as document() and doc(). Note that the URIResolver
     * does not resolve the URI in the sense of RFC 2396 (which is also the sense in which
     * the resolve-uri() function uses the term): rather it dereferences an absolute URI
     * to obtain an actual resource, which is returned as a Source object.
     *
     * @param resolver The URIResolver to be used.
     * @since 8.4
     */

    public void setURIResolver(URIResolver resolver) {
        uriResolver = resolver;
        if (resolver instanceof StandardURIResolver) {
            ((StandardURIResolver)resolver).setConfiguration(this);
        }
        defaultXsltCompilerInfo.setURIResolver(resolver);
    }

    /**
     * Set the URIResolver to a URI resolver that allows query parameters after the URI,
     * and in the case of Saxon-EE, that inteprets the file extension .ptree
     */

    public void setParameterizedURIResolver() {
        getSystemURIResolver().setRecognizeQueryParameters(true);
    }

    /**
     * Get the system-defined URI Resolver. This is used when the user-defined URI resolver
     * returns null as the result of the resolve() method
     *
     * @return the system-defined URI resolver
     */

    public StandardURIResolver getSystemURIResolver() {
        return systemURIResolver;
    }

    /**
     * Create an instance of a URIResolver with a specified class name.
     * Note that this method does not register the URIResolver with this Configuration.
     *
     * @param className The fully-qualified name of the URIResolver class
     * @return The newly created URIResolver
     * @throws TransformerException if the requested class does not
     *                              implement the javax.xml.transform.URIResolver interface
     */
    public URIResolver makeURIResolver(String className) throws TransformerException {
        Object obj = dynamicLoader.getInstance(className, null);
        if (obj instanceof StandardURIResolver) {
            ((StandardURIResolver)obj).setConfiguration(this);
        }
        if (obj instanceof URIResolver) {
            return (URIResolver)obj;
        }
        throw new XPathException("Class " + className + " is not a URIResolver");
    }

    /**
     * Get the ErrorListener used in this configuration. If no ErrorListener
     * has been supplied explicitly, the default ErrorListener is used.
     *
     * @return the ErrorListener.
     * @since 8.4
     */

    public ErrorListener getErrorListener() {
        ErrorListener listener = defaultParseOptions.getErrorListener();
        if (listener == null) {
            listener = new StandardErrorListener();
            ((StandardErrorListener)listener).setRecoveryPolicy(defaultXsltCompilerInfo.getRecoveryPolicy());
            defaultParseOptions.setErrorListener(listener);
        }
        return listener;
    }

    /**
     * Set the ErrorListener to be used in this configuration. The ErrorListener
     * is informed of all static and dynamic errors detected, and can decide whether
     * run-time warnings are to be treated as fatal.
     *
     * @param listener the ErrorListener to be used
     * @since 8.4
     */

    public void setErrorListener(ErrorListener listener) {
        defaultParseOptions.setErrorListener(listener);
    }


    /**
     * Report a fatal error
     *
     * @param err the exception to be reported
     */

    public void reportFatalError(XPathException err) {
        if (!err.hasBeenReported()) {
            try {
                getErrorListener().fatalError(err);
            } catch (TransformerException e) {
                //
            }
            err.setHasBeenReported(true);
        }
    }

    /**
     * Set whether multithreading optimizations are allowed
     *
     * @param multithreading true if multithreading optimizations area allowed
     */

    public void setMultiThreading(boolean multithreading) {
        allowMultiThreading = multithreading;
    }

    /**
     * Determine whether multithreading optimizations are allowed
     *
     * @return true if multithreading optimizations are allowed
     */

    public boolean isMultiThreading() {
        return allowMultiThreading;
    }

    /**
     * Set the XML version to be used by default for validating characters and names.
     * Note that source documents specifying xml version="1.0" or "1.1" are accepted
     * regardless of this setting. The effect of this switch is to change the validation
     * rules for types such as Name and NCName, to change the meaning of \i and \c in
     * regular expressions, and to determine whether the serializer allows XML 1.1 documents
     * to be constructed.
     *
     * @param version one of the constants XML10 or XML11
     * @since 8.6
     */

    public void setXMLVersion(int version) {
        xmlVersion = version;
    }

    /**
     * Get the XML version to be used by default for validating characters and names
     *
     * @return one of the constants {@link #XML10} or {@link #XML11}
     * @since 8.6
     */

    public int getXMLVersion() {
        return xmlVersion;
    }

    /**
     * Get the parsing and document building options defined in this configuration
     * @return the parsing and document building options. Note that any changes to this
     * ParseOptions object will be reflected back in the Configuration; if changes are to be made
     * locally, the caller should create a copy.
     * @since 9.2
     */

    public ParseOptions getParseOptions() {
        return defaultParseOptions;
    }

    /**
     * Get a class that can be used to check names against the selected XML version
     *
     * @return a class that can be used for name checking
     * @since 8.6
     */

    public NameChecker getNameChecker() {
        //noinspection RedundantCast
        return (xmlVersion == XML10 ?
                (NameChecker)Name10Checker.getInstance() :
                (NameChecker)Name11Checker.getInstance());
    }

    /**
     * Get the version of XML Schema to be used
     * @return {@link #XSD10} or {@link #XSD11}
     * @since 9.2
     */

    public int getXsdVersion() {
        return xsdVersion;
    }

    /**
     * Get an XPathContext object with sufficient capability to perform comparisons and conversions
     *
     * @return a dynamic context for performing conversions
     */

    public XPathContext getConversionContext() {
        if (conversionContext == null) {
            conversionContext = new EarlyEvaluationContext(this, new CollationMap(this));
        }
        return conversionContext;
    }

    /**
     * Get the Tree Model used by this Configuration. This is either
     * {@link Builder#LINKED_TREE}, {@link Builder#TINY_TREE}, or {@link Builder#TINY_TREE_CONDENSED}.
     * The default is <code>Builder.TINY_TREE</code>.
     *
     * @return the selected Tree Model
     * @since 8.4 (Condensed tinytree added in 9.2)
     */

    public int getTreeModel() {
        return defaultParseOptions.getModel().getSymbolicValue();
    }

    /**
     * Set the Tree Model used by this Configuration. This is either
     * {@link Builder#LINKED_TREE} or {@link Builder#TINY_TREE}, or {@link Builder#TINY_TREE_CONDENSED}.
     * The default is <code>Builder.TINY_TREE</code>.
     *
     * @param treeModel the integer constant representing the selected Tree Model
     * @since 8.4 (Condensed tinytree added in 9.2)
     */

    public void setTreeModel(int treeModel) {
        defaultParseOptions.setModel(TreeModel.getTreeModel(treeModel));
    }

    /**
     * Ask whether the typed value cache should be used for the TinyTree
     * @return true (the default) if the cache should be used, false otherwise
     */

    public boolean useTypedValueCache() {
        return useTypedValueCache;
    }

    /**
     * Determine whether source documents will maintain line numbers, for the
     * benefit of the saxon:line-number() extension function as well as run-time
     * tracing.
     *
     * @return true if line numbers are maintained in source documents
     * @since 8.4
     */

    public boolean isLineNumbering() {
        return defaultParseOptions.isLineNumbering();
    }

    /**
     * Determine whether source documents will maintain line numbers, for the
     * benefit of the saxon:line-number() extension function as well as run-time
     * tracing.
     *
     * @param lineNumbering true if line numbers are maintained in source documents
     * @since 8.4
     */

    public void setLineNumbering(boolean lineNumbering) {
        defaultParseOptions.setLineNumbering(lineNumbering);
    }

    /**
     * Set whether or not source documents (including stylesheets and schemas) are have
     * XInclude processing applied to them, or not. Default is false.
     *
     * @param state true if XInclude elements are to be expanded, false if not
     * @since 8.9
     */

    public void setXIncludeAware(boolean state) {
        defaultParseOptions.setXIncludeAware(state);
    }

    /**
     * Test whether or not source documents (including stylesheets and schemas) are to have
     * XInclude processing applied to them, or not
     *
     * @return true if XInclude elements are to be expanded, false if not
     * @since 8.9
     */

    public boolean isXIncludeAware() {
        return defaultParseOptions.isXIncludeAware();
    }

    /**
     * Get the TraceListener used for run-time tracing of instruction execution.
     *
     * @return the TraceListener that was set using {@link #getTraceListener()} if set.
     * Otherwise, returns null.
     * @since 8.4. Modified in 9.1.
     */

    public TraceListener getTraceListener() {
        return traceListener;
    }


    /**
     * Get or create the TraceListener used for run-time tracing of instruction execution.
     *
     * @return If a TraceListener has been set using {@link #setTraceListener(net.sf.saxon.trace.TraceListener)},
     * returns that TraceListener. Otherwise, if a TraceListener class has been set using
     * {@link #setTraceListenerClass(String)}, returns a newly created instance of that class.
     * Otherwise, returns null.
     * @throws XPathException if the supplied TraceListenerClass cannot be instantiated as an instance
     * of TraceListener
     * @since 9.1.
     */

    public TraceListener makeTraceListener() throws XPathException {
        if (traceListener != null) {
            return traceListener;
        } else if (traceListenerClass != null) {
            try {
                return makeTraceListener(traceListenerClass);
            } catch (ClassCastException e) {
                throw new XPathException(e);
            }
        } else {
            return null;
        }
    }

    /**
     * Set the TraceListener to be used for run-time tracing of instruction execution.
     * <p/>
     * <p>Note: this method should not be used if the Configuration is multithreading. In that situation,
     * use {@link #setCompileWithTracing(boolean)} to force stylesheets and queries to be compiled
     * with trace code enabled, and use {@link Controller#addTraceListener(net.sf.saxon.trace.TraceListener)} to
     * supply a TraceListener at run time.</p>
     *
     * @param traceListener The TraceListener to be used.
     * @since 8.4
     */

    public void setTraceListener(TraceListener traceListener) {
        this.traceListener = traceListener;
        setCompileWithTracing(true);
        setMultiThreading(false);
    }

    /**
     * Set the name of the trace listener class to be used for run-time tracing of instruction
     * execution. A new instance of this class will be created for each query or transformation
     * that requires tracing. The class must be an instance of {@link TraceListener}.
     * @param className the name of the trace listener class
     * @throws IllegalArgumentException if the class cannot be instantiated or does not implement
     * TraceListener
     * @since 9.1
     */

    public void setTraceListenerClass(String className) {
        try {
            makeTraceListener(className);
        } catch (XPathException err) {
            throw new IllegalArgumentException(className + ": " + err.getMessage());
        }
        this.traceListenerClass = className;
        setCompileWithTracing(true);
    }

    /**
     * Get the name of the trace listener class to be used for run-time tracing of instruction
     * execution. A new instance of this class will be created for each query or transformation
     * that requires tracing. The class must be an instance of {@link TraceListener}.
     * @return the name of the trace listener class, or null if no trace listener class
     * has been nominated.
     * @since 9.1
     */

    public String getTraceListenerClass() {
        return traceListenerClass;
    }

    /**
     * Determine whether compile-time generation of trace code was requested
     *
     * @return true if compile-time generation of code was requested
     * @since 8.8
     */

    public boolean isCompileWithTracing() {
        return tracing;
    }

    /**
     * Request compile-time generation of trace code (or not)
     *
     * @param trace true if compile-time generation of trace code is required
     * @since 8.8
     */

    public void setCompileWithTracing(boolean trace) {
        tracing = trace;
        if (defaultXsltCompilerInfo != null) {
            defaultXsltCompilerInfo.setCompileWithTracing(trace);
        }
        if (defaultStaticQueryContext != null) {
            defaultStaticQueryContext.setCompileWithTracing(trace);
        }  
    }

    /**
     * Set optimizer tracing on or off
     *
     * @param trace set to true to switch optimizer tracing on, false to switch it off
     */

    public void setOptimizerTracing(boolean trace) {
        traceOptimizations = trace;
    }

    /**
     * Test whether optimizer tracing is on or off
     *
     * @return true if optimizer tracing is switched on
     */

    public boolean isOptimizerTracing() {
        return traceOptimizations;
    }


    /**
     * Create an instance of a TraceListener with a specified class name
     *
     * @param className The fully qualified class name of the TraceListener to
     *                  be constructed
     * @return the newly constructed TraceListener
     * @throws net.sf.saxon.trans.XPathException
     *          if the requested class does not
     *          implement the net.sf.saxon.trace.TraceListener interface
     */

    public TraceListener makeTraceListener(String className)
            throws XPathException {
        Object obj = dynamicLoader.getInstance(className, null);
        if (obj instanceof TraceListener) {
            return (TraceListener)obj;
        }
        throw new XPathException("Class " + className + " is not a TraceListener");
    }

    /**
      * Register an extension function that is to be made available in all queries, stylesheets, and
      * XPath expressions compiled under thie Configuration. This method
      * incorporates an extension function implemented as an instance of
      * {@link net.sf.saxon.functions.ExtensionFunctionCall}, using an arbitrary name and namespace.
      * @param function the class that implements the extension function. This must be a class that extends
      * {@link net.sf.saxon.functions.ExtensionFunctionCall}, and it must have a public zero-argument
      * constructor
      * @throws XPathException if the class cannot be instantiated or does not extend
      * {@link net.sf.saxon.functions.ExtensionFunctionCall}
      * @since 9.2
      */

     public void registerExtensionFunction(ExtensionFunctionDefinition function) throws XPathException {
         try {
             integratedFunctionLibrary.registerFunction(function);
         } catch (Exception err) {
             throw new IllegalArgumentException(err);
         }
     }

    /**
     * Get the IntegratedFunction library containing integrated extension functions 
     * @return the IntegratedFunctionLibrary
     * @since 9.2
     */

    public IntegratedFunctionLibrary getIntegratedFunctionLibrary() {
        return integratedFunctionLibrary;
    }


    /**
     * Get the FunctionLibrary used to bind calls on Saxon-defined extension functions.
     * <p/>
     * This method is intended for internal use only.
     *
     * @return the FunctionLibrary used for extension functions in the Saxon library.
     */

    public VendorFunctionLibrary getVendorFunctionLibrary() {
        if (vendorFunctionLibrary == null) {
            vendorFunctionLibrary = new VendorFunctionLibrary();
        }
        return vendorFunctionLibrary;
    }

        /**
     * Add the registered extension binders to a function library.
     * This method is intended primarily for internal use
     */

    public void addExtensionBinders(FunctionLibraryList list) {
        // no action in this class
    }

    /**
     * Make a UserFunction object.
     * This method is for internal use.
     * @param memoFunction true if the function is to be a memo function, This option is ignored
     * in Saxon-HE.
     */

    public UserFunction newUserFunction(boolean memoFunction) {
        return new UserFunction();
    }


    /**
     * Set a CollationURIResolver to be used to resolve collation URIs (that is,
     * to take a URI identifying a collation, and return the corresponding collation).
     * Note that Saxon attempts first to resolve a collation URI using the resolver
     * registered with the Controller; if that returns null, it tries again using the
     * resolver registered with the Configuration.
     * <p/>
     * Note that it is undefined whether collation URIs are resolved at compile time
     * or at run-time. It is therefore inadvisable to change the CollationURIResolver after
     * compiling a query or stylesheet and before running it.
     *
     * @param resolver the collation URI resolver to be used. This replaces any collation
     *                 URI resolver previously registered.
     * @since 8.5
     */

    public void setCollationURIResolver(CollationURIResolver resolver) {
        collationResolver = resolver;
    }

    /**
     * Get the collation URI resolver associated with this configuration. This will
     * return the CollationURIResolver previously set using the {@link #setCollationURIResolver}
     * method; if this has not been called, it returns the system-defined collation URI resolver
     *
     * @return the registered CollationURIResolver
     * @since 8.5
     */

    public CollationURIResolver getCollationURIResolver() {
        return collationResolver;
    }

    /**
     * Get the collation map, which can be used to register named collations (and a default collation)
     * at the Configuration level. Any collations registered in this collation map apply to all
     * queries, stylesheets, and Xpath expressions compiled under this collation. The effective
     * contents of the collation map are the contents at the time the query or stylesheet is compiled.
     */

    public CollationMap getCollationMap() {
        return collationMap;
    }

    /**
     * Set the default collection.
     *
     * <p>If no default collection URI is specified, then a request for the default collection
     * is handled by calling the registered collection URI resolver with an argument of null.</p>
     *
     * @param uri the URI of the default collection. Calling the collection() function
     * with no arguments is equivalent to calling collection() with this URI as an argument.
     * The URI will be dereferenced by passing it to the registered CollectionURIResolver
     * @since 9.2
     */

    public void setDefaultCollection(String uri) {
        defaultCollection = uri;
    }

    /**
     * Get the URI of the default collection. Returns null if no default collection URI has
     * been registered.
     * @return the default collection URI. This is dereferenced in the same way as a normal
     * collection URI (via the CollectionURIResolver) to return a sequence of nodes
     * @since 9.2
     */

    public String getDefaultCollection() {
        return defaultCollection;
    }

    /**
     * Set a CollectionURIResolver to be used to resolve collection URIs (that is,
     * the URI supplied in a call to the collection() function).
     * <p/>
     * Collection URIs are always resolved at run-time, using the CollectionURIResolver
     * in force at the time the collection() function is called.
     *
     * @param resolver the collection URI resolver to be used. This replaces any collection
     *                 URI resolver previously registered.
     * @since 8.5
     */

    public void setCollectionURIResolver(CollectionURIResolver resolver) {
        collectionResolver = resolver;
    }

    /**
     * Get the collection URI resolver associated with this configuration. This will
     * return the CollectionURIResolver previously set using the {@link #setCollectionURIResolver}
     * method; if this has not been called, it returns the system-defined collection URI resolver
     *
     * @return the registered CollationURIResolver
     * @since 8.5
     */

    public CollectionURIResolver getCollectionURIResolver() {
        return collectionResolver;
    }

    /**
     * Set the localizer factory to be used
     * @param factory the LocalizerFactory
     * @since 9.2
     */

    public void setLocalizerFactory(LocalizerFactory factory) {
        this.localizerFactory = factory;
    }

    /**
     * Get the localizer factory in use
     * @return the LocalizerFactory, if any. If none has been set, returns null.
     * @since 9.2
     */

    public LocalizerFactory getLocalizerFactory() {
        return localizerFactory;
    }


    /**
     * Set the default language to be used for number and date formatting when no language is specified.
     * If none is set explicitly, the default Locale for the Java Virtual Machine is used.
     * @param language the default language to be used, as an ISO code for example "en" or "fr-CA"
     * @since 9.2
     */

    public void setDefaultLanguage(String language) {
        defaultLanguage = language;
    }

    /**
     * Get the default language. Unless an explicit default is set, this will be the language
     * of the default Locale for the Java Virtual Machine
     * @return the default language
     * @since 9.2
     */

    public String getDefaultLanguage() {
        return defaultLanguage;
    }

    /**
     * Set the default country to be used for number and date formatting when no country is specified.
     * If none is set explicitly, the default Locale for the Java Virtual Machine is used.
     * @param country the default country to be used, as an ISO code for example "US" or "GB"
     * @since 9.2
     */

    public void setDefaultCountry(String country) {
        defaultCountry = country;
    }

    /**
     * Get the default country to be used for number and date formatting when no country is specified.
     * If none is set explicitly, the default Locale for the Java Virtual Machine is used.
     * @return the default country to be used, as an ISO code for example "US" or "GB"
     * @since 9.2
     */

    public String getDefaultCountry() {
        return defaultCountry;
    }


    /**
     * Load a Numberer class for a given language and check it is OK.
     * This method is provided primarily for internal use.
     * @param language the language for which a Numberer is required. May be null,
     * indicating default language
     * @param country the country for which a Numberer is required. May be null,
     * indicating default country
     * @return a suitable numberer. If no specific numberer is available
     * for the language, the default numberer (normally English) is used.
     */

    public Numberer makeNumberer(String language, String country) {
        if (localizerFactory == null) {
            return new Numberer_en();
        } else {
            Numberer numberer = localizerFactory.getNumberer(language, country);
            if (numberer == null) {
                numberer = new Numberer_en();
            }
            return numberer;
        }
    }



    /**
     * Set a user-defined ModuleURIResolver for resolving URIs used in "import module"
     * declarations in an XQuery prolog.
     * This acts as the default value for the ModuleURIResolver in the StaticQueryContext, and may be
     * overridden by a more specific ModuleURIResolver nominated as part of the StaticQueryContext.
     *
     * @param resolver the URI resolver for XQuery modules
     */

    public void setModuleURIResolver(ModuleURIResolver resolver) {
        moduleURIResolver = resolver;
    }

    /**
     * Create and register an instance of a ModuleURIResolver with a specified class name.
     * This will be used for resolving URIs in XQuery "import module" declarations, unless
     * a more specific ModuleURIResolver has been nominated as part of the StaticQueryContext.
     *
     * @param className The fully-qualified name of the LocationHintResolver class
     * @throws TransformerException if the requested class does not
     *                              implement the net.sf.saxon.LocationHintResolver interface
     */
    public void setModuleURIResolver(String className) throws TransformerException {
        Object obj = dynamicLoader.getInstance(className, null);
        if (obj instanceof ModuleURIResolver) {
            setModuleURIResolver((ModuleURIResolver)obj);
        } else {
            throw new XPathException("Class " + className + " is not a ModuleURIResolver");
        }
    }

    /**
     * Get the user-defined ModuleURIResolver for resolving URIs used in "import module"
     * declarations in the XQuery prolog; returns null if none has been explicitly set.
     *
     * @return the resolver for Module URIs
     */

    public ModuleURIResolver getModuleURIResolver() {
        return moduleURIResolver;
    }

    /**
     * Get the standard system-defined ModuleURIResolver for resolving URIs used in "import module"
     * declarations in the XQuery prolog.
     *
     * @return the standard system-defined ModuleURIResolver for resolving URIs
     */

    public ModuleURIResolver getStandardModuleURIResolver() {
        return standardModuleURIResolver;
    }

    /**
     * Set a user-defined SchemaURIResolver for resolving URIs used in "import schema"
     * declarations.
     *
     * @param resolver the URI resolver used for import schema declarations
     */

    public void setSchemaURIResolver(SchemaURIResolver resolver) {
        schemaURIResolver = resolver;
    }

    /**
     * Get the user-defined SchemaURIResolver for resolving URIs used in "import schema"
     * declarations; if none has been explicitly set, returns null.
     *
     * @return the user-defined SchemaURIResolver for resolving URIs
     */

    public SchemaURIResolver getSchemaURIResolver() {
        return schemaURIResolver;
    }

    /**
     * Get the default options for XSLT compilation
     * @return the default options for XSLT compilation. The CompilerInfo object will reflect any options
     * set using other methods available for this Configuration object
     */

    public CompilerInfo getDefaultXsltCompilerInfo() {
        return defaultXsltCompilerInfo;
    }

    /**
     * Get the default options for XQuery compilation
     */

    public StaticQueryContext getDefaultStaticQueryContext() {
        if (defaultStaticQueryContext == null) {
            defaultStaticQueryContext = new StaticQueryContext(this, true);
        }
        return defaultStaticQueryContext;
    }

    /**
     * Determine how recoverable run-time errors are to be handled. This applies
     * only if the standard ErrorListener is used.
     *
     * @return the current recovery policy. The options are {@link #RECOVER_SILENTLY},
     *         {@link #RECOVER_WITH_WARNINGS}, or {@link #DO_NOT_RECOVER}.
     * @since 8.4
     */

    public int getRecoveryPolicy() {
        return defaultXsltCompilerInfo.getRecoveryPolicy();
    }

    /**
     * Determine how recoverable run-time errors are to be handled. This applies
     * only if the standard ErrorListener is used. The recovery policy applies to
     * errors classified in the XSLT 2.0 specification as recoverable dynamic errors,
     * but only in those cases where Saxon provides a choice over how the error is handled:
     * in some cases, Saxon makes the decision itself.
     *
     * @param recoveryPolicy the recovery policy to be used. The options are {@link #RECOVER_SILENTLY},
     *                       {@link #RECOVER_WITH_WARNINGS}, or {@link #DO_NOT_RECOVER}.
     * @since 8.4
     */

    public void setRecoveryPolicy(int recoveryPolicy) {
        defaultXsltCompilerInfo.setRecoveryPolicy(recoveryPolicy);
    }

    /**
     * Get the name of the class that will be instantiated to create a MessageEmitter,
     * to process the output of xsl:message instructions in XSLT.
     *
     * @return the full class name of the message emitter class.
     * @since 8.4
     */

    public String getMessageEmitterClass() {
        return defaultXsltCompilerInfo.getMessageReceiverClassName();
    }

    /**
     * Set the name of the class that will be instantiated to
     * to process the output of xsl:message instructions in XSLT.
     *
     * @param messageReceiverClassName the full class name of the message receiver. This
     *                            must implement net.sf.saxon.event.Receiver.
     * @since 8.4
     */

    public void setMessageEmitterClass(String messageReceiverClassName) {
        defaultXsltCompilerInfo.setMessageReceiverClassName(messageReceiverClassName);
    }

    /**
     * Get the name of the class that will be instantiated to create an XML parser
     * for parsing source documents (for example, documents loaded using the document()
     * or doc() functions).
     * <p/>
     * This method is retained in Saxon for backwards compatibility, but the preferred way
     * of choosing an XML parser is to use JAXP interfaces, for example by supplying a
     * JAXP Source object initialized with an appropriate implementation of org.xml.sax.XMLReader.
     *
     * @return the fully qualified name of the XML parser class
     */

    public String getSourceParserClass() {
        return sourceParserClass;
    }

    /**
     * Set the name of the class that will be instantiated to create an XML parser
     * for parsing source documents (for example, documents loaded using the document()
     * or doc() functions).
     * <p/>
     * This method is retained in Saxon for backwards compatibility, but the preferred way
     * of choosing an XML parser is to use JAXP interfaces, for example by supplying a
     * JAXP Source object initialized with an appropriate implementation of org.xml.sax.XMLReader.
     *
     * @param sourceParserClass the fully qualified name of the XML parser class. This must implement
     *                          the SAX2 XMLReader interface.
     */

    public void setSourceParserClass(String sourceParserClass) {
        this.sourceParserClass = sourceParserClass;
    }

    /**
     * Get the name of the class that will be instantiated to create an XML parser
     * for parsing stylesheet modules.
     * <p/>
     * This method is retained in Saxon for backwards compatibility, but the preferred way
     * of choosing an XML parser is to use JAXP interfaces, for example by supplying a
     * JAXP Source object initialized with an appropriate implementation of org.xml.sax.XMLReader.
     *
     * @return the fully qualified name of the XML parser class
     */

    public String getStyleParserClass() {
        return styleParserClass;
    }

    /**
     * Set the name of the class that will be instantiated to create an XML parser
     * for parsing stylesheet modules.
     * <p/>
     * This method is retained in Saxon for backwards compatibility, but the preferred way
     * of choosing an XML parser is to use JAXP interfaces, for example by supplying a
     * JAXP Source object initialized with an appropriate implementation of org.xml.sax.XMLReader.
     *
     * @param parser the fully qualified name of the XML parser class
     */

    public void setStyleParserClass(String parser) {
        this.styleParserClass = parser;
    }

    /**
     * Get the OutputURIResolver that will be used to resolve URIs used in the
     * href attribute of the xsl:result-document instruction.
     *
     * @return the OutputURIResolver. If none has been supplied explicitly, the
     *         default OutputURIResolver is returned.
     * @since 8.4
     */

    public OutputURIResolver getOutputURIResolver() {
        return defaultXsltCompilerInfo.getOutputURIResolver();
    }

    /**
     * Set the OutputURIResolver that will be used to resolve URIs used in the
     * href attribute of the xsl:result-document instruction.
     *
     * @param outputURIResolver the OutputURIResolver to be used.
     * @since 8.4
     */

    public void setOutputURIResolver(OutputURIResolver outputURIResolver) {
        defaultXsltCompilerInfo.setOutputURIResolver(outputURIResolver);
    }

    /**
     * Set a custom SerializerFactory. This will be used to create a serializer for a given
     * set of output properties and result destination.
     *
     * @param factory a custom SerializerFactory
     * @since 8.8
     */

    public void setSerializerFactory(SerializerFactory factory) {
        serializerFactory = factory;
    }

    /**
     * Get the SerializerFactory. This returns the standard built-in SerializerFactory, unless
     * a custom SerializerFactory has been registered.
     *
     * @return the SerializerFactory in use
     * @since 8.8
     */

    public SerializerFactory getSerializerFactory() {
        return serializerFactory;
    }

    /**
     * Get the CharacterSetFactory. Note: at present this cannot be changed.
     * @return the CharacterSetFactory in use.
     * @since 9.2
     */

    public CharacterSetFactory getCharacterSetFactory() {
        if (characterSetFactory == null) {
            characterSetFactory = new CharacterSetFactory();
        }
        return characterSetFactory;
    }

    /**
     * Set the default serialization properties
     * @param props the default properties
     */

    public void setDefaultSerializationProperties(Properties props) {
        defaultSerializationProperties = props;
    }

    /**
     * Get the default serialization properties
     * @return the default properties
     */

    public Properties getDefaultSerializationProperties() {
        return defaultSerializationProperties;
    }

    /**
     * Determine whether brief progress messages and timing information will be output
     * to System.err.
     * <p/>
     * This method is provided largely for internal use. Progress messages are normally
     * controlled directly from the command line interfaces, and are not normally used when
     * driving Saxon from the Java API.
     *
     * @return true if these messages are to be output.
     */

    public boolean isTiming() {
        return timing;
    }

    /**
     * Determine whether brief progress messages and timing information will be output
     * to System.err.
     * <p/>
     * This method is provided largely for internal use. Progress messages are normally
     * controlled directly from the command line interfaces, and are not normally used when
     *
     * @param timing true if these messages are to be output.
     */

    public void setTiming(boolean timing) {
        this.timing = timing;
    }

    /**
     * Determine whether a warning is to be output when running against a stylesheet labelled
     * as version="1.0". The XSLT specification requires such a warning unless the user disables it.
     *
     * @return true if these messages are to be output.
     * @since 8.4
     */

    public boolean isVersionWarning() {
        return defaultXsltCompilerInfo.isVersionWarning();
    }

    /**
     * Determine whether a warning is to be output when running against a stylesheet labelled
     * as version="1.0". The XSLT specification requires such a warning unless the user disables it.
     *
     * @param warn true if these messages are to be output.
     * @since 8.4
     */

    public void setVersionWarning(boolean warn) {
        defaultXsltCompilerInfo.setVersionWarning(warn);
    }

    /**
     * Determine whether calls to external Java functions are permitted.
     *
     * @return true if such calls are permitted.
     * @since 8.4
     */

    public boolean isAllowExternalFunctions() {
        return allowExternalFunctions;
    }

    /**
     * Determine whether calls to external Java functions are permitted. Allowing
     * external function calls is potentially a security risk if the stylesheet or
     * Query is untrusted, as it allows arbitrary Java methods to be invoked, which can
     * examine or modify the contents of filestore and other resources on the machine
     * where the query/stylesheet is executed.
     * <p/>
     * <p>Setting the value to false disallows all of the following:</p>
     * <p/>
     * <ul>
     * <li>Calls to Java extension functions</li>
     * <li>Use of the XSLT system-property() function to access Java system properties</li>
     * <li>Use of a relative URI in the <code>xsl:result-document</code> instruction</li>
     * <li>Calls to XSLT extension instructions</li>
     * </ul>
     * <p/>
     * <p>Note that this option does not disable use of the <code>doc()</code> function or similar
     * functions to access the filestore of the machine where the transformation or query is running.
     * That should be done using a user-supplied <code>URIResolver</code></p>
     *
     * @param allowExternalFunctions true if external function calls are to be
     *                               permitted.
     * @since 8.4
     */

    public void setAllowExternalFunctions(boolean allowExternalFunctions) {
        this.allowExternalFunctions = allowExternalFunctions;
    }

    /**
     * Determine whether calls on external functions are to be traced for diagnostic
     * purposes.
     *
     * @return true if tracing is enabled for calls to external Java functions
     */

    public boolean isTraceExternalFunctions() {
        return traceExternalFunctions;
    }

    /**
     * Say whether attribute types obtained from a DTD are to be used to set type annotations
     * on the resulting nodes.
     *
     * @param useTypes set to true if DTD types are to be taken into account
     * @deprecated since 9.2 This feature was dropped from the final XDM specification and will be dropped in a future
     * Saxon release. Use an XSD schema to define the attribute types instead.
     * @since 8.4
     */

    public void setRetainDTDAttributeTypes(boolean useTypes) throws TransformerFactoryConfigurationError {
        if (useTypes && !isLicensedFeature(LicenseFeature.SCHEMA_VALIDATION)) {
            throw new TransformerFactoryConfigurationError(
                    "Retaining DTD attribute types requires the schema-aware product");
        }
        retainDTDattributeTypes = useTypes;
    }

    /**
     * Ask whether attribute types obtained from a DTD are to be used to set type annotations
     * on the resulting nodes
     * @return true if DTD types are to be taken into account
     * @since 8.4
     */

    public boolean isRetainDTDAttributeTypes() {
        return retainDTDattributeTypes;
    }

    /**
     * Determine whether calls on external functions are to be traced for diagnostic
     * purposes.
     *
     * @param traceExternalFunctions true if tracing is to be enabled
     *                               for calls to external Java functions
     */

    public void setTraceExternalFunctions(boolean traceExternalFunctions) {
        this.traceExternalFunctions = traceExternalFunctions;
    }


    /**
     * Determine whether the XML parser for source documents will be asked to perform
     * validation of source documents
     *
     * @return true if DTD validation is requested.
     * @since 8.4
     */

    public boolean isValidation() {
        return defaultParseOptions.getDTDValidationMode() == Validation.STRICT ||
                defaultParseOptions.getDTDValidationMode() == Validation.LAX;
    }

    /**
     * Determine whether the XML parser for source documents will be asked to perform
     * DTD validation of source documents
     *
     * @param validation true if DTD validation is to be requested.
     * @since 8.4
     */

    public void setValidation(boolean validation) {
        defaultParseOptions.setDTDValidationMode(validation ? Validation.STRICT : Validation.STRIP);
    }

    /**
     * Create a document projector for a given path map. Document projection is available only
     * in Saxon-EE, so the Saxon-B version of this method throws an exception
     *
     * @param map the path map used to control document projection
     * @return a push filter that implements document projection
     * @throws UnsupportedOperationException if this is not a schema-aware configuration, or
     *                                       if no Saxon-EE license is available
     */

    public ProxyReceiver makeDocumentProjector(PathMap.PathMapRoot map) {
        throw new UnsupportedOperationException("Document projection requires a schema-aware Configuration");
    }

    /**
     * Ask whether source documents (supplied as a StreamSource or SAXSource)
     * should be subjected to schema validation, and if so, in what validation mode
     *
     * @return the schema validation mode previously set using setSchemaValidationMode(),
     *         or the default mode {@link Validation#STRIP} otherwise.
     */

    public int getSchemaValidationMode() {
        return defaultParseOptions.getSchemaValidationMode();
    }

    /**
     * Say whether source documents (supplied as a StreamSource or SAXSource)
     * should be subjected to schema validation, and if so, in what validation mode.
     * This value may be overridden at the level of a Controller for an individual transformation or query.
     *
     * @param validationMode the validation (or construction) mode to be used for source documents.
     *                       One of {@link Validation#STRIP}, {@link Validation#PRESERVE}, {@link Validation#STRICT},
     *                       {@link Validation#LAX}
     * @since 8.4
     */

    public void setSchemaValidationMode(int validationMode) {
        switch (validationMode) {
        case Validation.STRIP:
        case Validation.PRESERVE:
            break;
        case Validation.LAX:
        case Validation.STRICT:
            if (!isLicensedFeature(LicenseFeature.SCHEMA_VALIDATION)) {
                needEnterpriseEdition();
            }
            break;
        default:
            throw new IllegalArgumentException("Unsupported validation mode " + validationMode);
        }
        defaultParseOptions.setSchemaValidationMode(validationMode);
    }

    /**
     * Indicate whether schema validation failures on result documents are to be treated
     * as fatal errors or as warnings.
     *
     * @param warn true if schema validation failures are to be treated as warnings; false if they
     *             are to be treated as fatal errors.
     * @since 8.4
     */

    public void setValidationWarnings(boolean warn) {
        validationWarnings = warn;
    }

    /**
     * Determine whether schema validation failures on result documents are to be treated
     * as fatal errors or as warnings.
     *
     * @return true if validation errors are to be treated as warnings (that is, the
     *         validation failure is reported but processing continues as normal); false
     *         if validation errors are fatal.
     * @since 8.4
     */

    public boolean isValidationWarnings() {
        return validationWarnings;
    }

    /**
     * Indicate whether attributes that have a fixed or default value are to be expanded when
     * generating a final result tree. By default (and for conformance with the W3C specifications)
     * it is required that fixed and default values should be expanded. However, there are use cases
     * for example when generating XHTML when this serves no useful purpose and merely bloats the output.
     * <p/>
     * <p>This option can be overridden at the level of a PipelineConfiguration</p>
     *
     * @param expand true if fixed and default values are to be expanded as required by the W3C
     *               specifications; false if this action is to be disabled. Note that this only affects the validation
     *               of final result trees; it is not possible to suppress expansion of fixed or default values on input
     *               documents, as this would make the type annotations on input nodes unsound.
     * @since 9.0
     */

    public void setExpandAttributeDefaults(boolean expand) {
        defaultParseOptions.setExpandAttributeDefaults(expand);
    }

    /**
     * Determine whether elements and attributes that have a fixed or default value are to be expanded.
     * This option applies both to DTD-defined attribute defaults and to schema-defined defaults for
     * elements and attributes. If an XML parser is used that does not report whether defaults have
     * been used, this option is ignored.
     * <p/>
     * * <p>This option can be overridden at the level of a PipelineConfiguration</p>
     *
     * @return true if elements and attributes that have a fixed or default value are to be expanded,
     *         false if defaults are not to be expanded. The default value is true. Note that the setting "false"
     *         is potentially non-conformant with the W3C specifications.
     * @since 9.0
     */

    public boolean isExpandAttributeDefaults() {
        return defaultParseOptions.isExpandAttributeDefaults();
    }


    /**
     * Get the target namepool to be used for stylesheets/queries and for source documents.
     *
     * @return the target name pool. If no NamePool has been specified explicitly, the
     *         default NamePool is returned.
     * @since 8.4
     */

    public NamePool getNamePool() {
        return namePool;
    }

    /**
     * Set the NamePool to be used for stylesheets/queries and for source documents.
     * <p/>
     * <p> Using this method allows several Configurations to share the same NamePool. This
     * was the normal default arrangement until Saxon 8.9, which changed the default so
     * that each Configuration uses its own NamePool.</p>
     * <p/>
     * <p>Sharing a NamePool creates a potential bottleneck, since changes to the namepool are
     * synchronized.</p>
     *
     * @param targetNamePool The NamePool to be used.
     * @since 8.4
     */

    public void setNamePool(NamePool targetNamePool) {
        namePool = targetNamePool;
    }

    /**
     * Get the TypeHierarchy: a cache holding type information
     *
     * @return the type hierarchy cache
     */

    public final TypeHierarchy getTypeHierarchy() {
        if (typeHierarchy == null) {
            typeHierarchy = new TypeHierarchy(this);
        }
        return typeHierarchy;
    }

    /**
     * Get the document number allocator.
     * <p/>
     * The document number allocator is used to allocate a unique number to each document built under this
     * configuration. The document number forms the basis of all tests for node identity; it is therefore essential
     * that when two documents are accessed in the same XPath expression, they have distinct document numbers.
     * Normally this is ensured by building them under the same Configuration. Using this method together with
     * {@link #setDocumentNumberAllocator}, however, it is possible to have two different Configurations that share
     * a single DocumentNumberAllocator
     *
     * @return the current DocumentNumberAllocator
     * @since 9.0
     */

    public DocumentNumberAllocator getDocumentNumberAllocator() {
        return documentNumberAllocator;
    }

    /**
     * Set the document number allocator.
     * <p/>
     * The document number allocator is used to allocate a unique number to each document built under this
     * configuration. The document number forms the basis of all tests for node identity; it is therefore essential
     * that when two documents are accessed in the same XPath expression, they have distinct document numbers.
     * Normally this is ensured by building them under the same Configuration. Using this method together with
     * {@link #getDocumentNumberAllocator}, however, it is possible to have two different Configurations that share
     * a single DocumentNumberAllocator</p>
     * <p>This method is for advanced applications only. Misuse of the method can cause problems with node identity.
     * The method should not be used except while initializing a Configuration, and it should be used only to
     * arrange for two different configurations to share the same DocumentNumberAllocators. In this case they
     * should also share the same NamePool.
     *
     * @param allocator the DocumentNumberAllocator to be used
     * @since 9.0
     */

    public void setDocumentNumberAllocator(DocumentNumberAllocator allocator) {
        documentNumberAllocator = allocator;
    }

    /**
     * Determine whether two Configurations are compatible. When queries, transformations, and path expressions
     * are run, all the Configurations used to build the documents and to compile the queries and stylesheets
     * must be compatible. Two Configurations are compatible if they share the same NamePool and the same
     * DocumentNumberAllocator.
     *
     * @param other the other Configuration to be compared with this one
     * @return true if the two configurations are compatible
     */

    public boolean isCompatible(Configuration other) {
        return namePool == other.namePool && documentNumberAllocator == other.documentNumberAllocator;
    }

    /**
     * Get the global document pool. This is used for documents preloaded during query or stylesheet
     * compilation. The user application can preload documents into the global pool, where they will be found
     * if any query or stylesheet requests the specified document using the doc() or document() function.
     *
     * @return the global document pool
     * @since 9.1
     */

    public DocumentPool getGlobalDocumentPool() {
        return globalDocumentPool;
    }

    /**
     * Determine whether whitespace-only text nodes are to be stripped unconditionally
     * from source documents.
     *
     * @return true if all whitespace-only text nodes are stripped.
     * @since 8.4
     */

    public boolean isStripsAllWhiteSpace() {
        return defaultParseOptions.getStripSpace() == Whitespace.ALL;
    }

    /**
     * Determine whether whitespace-only text nodes are to be stripped unconditionally
     * from source documents.
     *
     * @param stripsAllWhiteSpace if all whitespace-only text nodes are to be stripped.
     * @since 8.4
     */

    public void setStripsAllWhiteSpace(boolean stripsAllWhiteSpace) {
        if (stripsAllWhiteSpace) {
            defaultParseOptions.setStripSpace(Whitespace.ALL);
        }
    }

    /**
     * Set which kinds of whitespace-only text node should be stripped.
     *
     * @param kind the kind of whitespace-only text node that should be stripped when building
     *             a source tree. One of {@link Whitespace#NONE} (none), {@link Whitespace#ALL} (all),
     *             or {@link Whitespace#IGNORABLE} (element-content whitespace as defined in a DTD or schema)
     */

    public void setStripsWhiteSpace(int kind) {
        defaultParseOptions.setStripSpace(kind);
    }

    /**
     * Set which kinds of whitespace-only text node should be stripped.
     *
     * @return kind the kind of whitespace-only text node that should be stripped when building
     *         a source tree. One of {@link Whitespace#NONE} (none), {@link Whitespace#ALL} (all),
     *         or {@link Whitespace#IGNORABLE} (element-content whitespace as defined in a DTD or schema)
     */

    public int getStripsWhiteSpace() {
        return defaultParseOptions.getStripSpace();
    }


    /**
     * Get a parser for source documents. The parser is allocated from a pool if any are available
     * from the pool: the client should ideally return the parser to the pool after use, so that it
     * can be reused.
     * <p/>
     * This method is intended primarily for internal use.
     *
     * @return a parser, in which the namespace properties must be set as follows:
     *         namespaces=true; namespace-prefixes=false. The DTD validation feature of the parser will be set
     *         on or off depending on the {@link #setValidation(boolean)} setting.
     */

    public synchronized XMLReader getSourceParser() throws TransformerFactoryConfigurationError {
        if (sourceParserPool == null) {
            sourceParserPool = new ArrayList<XMLReader>(10);
        }
        if (!sourceParserPool.isEmpty()) {
            int n = sourceParserPool.size() - 1;
            XMLReader parser = sourceParserPool.get(n);
            sourceParserPool.remove(n);
            return parser;
        }
        XMLReader parser;
        if (getSourceParserClass() != null) {
            parser = makeParser(getSourceParserClass());
        } else {
            parser = loadParser();
        }
        if (timing) {
            reportParserDetails(parser);
        }
        try {
            Sender.configureParser(parser);
        } catch (XPathException err) {
            throw new TransformerFactoryConfigurationError(err);
        }
        if (isValidation()) {
            try {
                parser.setFeature("http://xml.org/sax/features/validation", true);
            } catch (SAXException err) {
                throw new TransformerFactoryConfigurationError("The XML parser does not support validation");
            }
        }

        return parser;
    }

    /**
     * Report the parser details to System.err
     * @param reader the parser
     */

    private void reportParserDetails(XMLReader reader) {
        String name = reader.getClass().getName();
//        if (name.equals("com.sun.org.apache.xerces.internal.parsers.SAXParser")) {
//            name += " version " + com.sun.org.apache.xerces.internal.impl.Version.getVersion();
//        }
        System.err.println("Using parser " + name);
    }

    /**
     * Return a source parser to the pool, for reuse
     *
     * @param parser The parser: the caller must not supply a parser that was obtained by any
     *               mechanism other than calling the getSourceParser() method.
     */

    public synchronized void reuseSourceParser(XMLReader parser) {
        if (sourceParserPool == null) {
            sourceParserPool = new ArrayList<XMLReader>(10);
        }
        try {
            // give things back to the garbage collecter
            parser.setContentHandler(null);
            parser.setEntityResolver(null);
            parser.setDTDHandler(null);
            parser.setErrorHandler(null);
            // Unfortunately setting the lexical handler to null doesn't work on Xerces, because
            // it tests "value instanceof LexicalHandler". So we set it to a lexical handler that
            // holds no references
            parser.setProperty("http://xml.org/sax/properties/lexical-handler", dummyLexicalHandler);
        } catch (Exception err) {
            //
        }
        sourceParserPool.add(parser);
    }

    /**
     * Get a parser by instantiating the SAXParserFactory
     *
     * @return the parser (XMLReader)
     */

    private XMLReader loadParser() {
        return platform.loadParser();
    }

    /**
     * Get the parser for stylesheet documents. This parser is also used for schema documents.
     * <p/>
     * This method is intended for internal use only.
     *
     * @return an XML parser (a SAX2 parser) that can be used for stylesheets and schema documents
     */

    public synchronized XMLReader getStyleParser() throws TransformerFactoryConfigurationError {
        if (styleParserPool == null) {
            styleParserPool = new ArrayList<XMLReader>(10);
        }
        if (!styleParserPool.isEmpty()) {
            int n = styleParserPool.size() - 1;
            XMLReader parser = styleParserPool.get(n);
            styleParserPool.remove(n);
            return parser;
        }
        XMLReader parser;
        if (getStyleParserClass() != null) {
            parser = makeParser(getStyleParserClass());
        } else {
            parser = loadParser();
        }
        try {
            parser.setFeature("http://xml.org/sax/features/namespaces", true);
            parser.setFeature("http://xml.org/sax/features/namespace-prefixes", false);
        } catch (SAXNotRecognizedException e) {
            throw new TransformerFactoryConfigurationError(e);
        } catch (SAXNotSupportedException e) {
            throw new TransformerFactoryConfigurationError(e);
        }
        return parser;
    }

    private static LexicalHandler dummyLexicalHandler = new DefaultHandler2();

    /**
     * Return a stylesheet (or schema) parser to the pool, for reuse
     *
     * @param parser The parser: the caller must not supply a parser that was obtained by any
     *               mechanism other than calling the getStyleParser() method.
     */

    public synchronized void reuseStyleParser(XMLReader parser) {
        if (styleParserPool == null) {
            styleParserPool = new ArrayList<XMLReader>(10);
        }
        try {
            // give things back to the garbage collecter
            parser.setContentHandler(null);
            parser.setEntityResolver(null);
            parser.setDTDHandler(null);
            parser.setErrorHandler(null);
            // Unfortunately setting the lexical handler to null doesn't work on Xerces, because
            // it tests "value instanceof LexicalHandler". Instead we set the lexical handler to one
            // that holds no references
            parser.setProperty("http://xml.org/sax/properties/lexical-handler", dummyLexicalHandler);
        } catch (Exception err) {
            //
        }        
        styleParserPool.add(parser);
    }

    /**
     * Simple interface to load a schema document
     *
     * @param absoluteURI the absolute URI of the location of the schema document
     */

    public void loadSchema(String absoluteURI) throws SchemaException {
        readSchema(makePipelineConfiguration(), "", absoluteURI, null);
    }

    /**
     * Read a schema from a given schema location
     * <p/>
     * This method is intended for internal use.
     *
     * @param pipe           the PipelineConfiguration
     * @param baseURI        the base URI of the instruction requesting the reading of the schema
     * @param schemaLocation the location of the schema to be read
     * @param expected       The expected targetNamespace of the schema being read.
     * @return the target namespace of the schema; null if there is no expectation
     * @throws UnsupportedOperationException when called in the non-schema-aware version of the product
     */

    public String readSchema(PipelineConfiguration pipe, String baseURI, String schemaLocation, String expected)
            throws SchemaException {
        needEnterpriseEdition();
        return null;
    }

    /**
     * Read schemas from a list of schema locations.
     * <p/>
     * This method is intended for internal use.
     *
     * @param pipe            the pipeline configuration
     * @param baseURI         the base URI against which the schema locations are to be resolved
     * @param schemaLocations the relative URIs specified as schema locations
     * @param expected        the namespace URI which is expected as the target namespace of the loaded schema
     */

    public void readMultipleSchemas(PipelineConfiguration pipe, String baseURI, Collection schemaLocations, String expected)
            throws SchemaException {
        needEnterpriseEdition();
    }


    /**
     * Read an inline schema from a stylesheet.
     * <p/>
     * This method is intended for internal use.
     *
     * @param root          the xs:schema element in the stylesheet
     * @param expected      the target namespace expected; null if there is no
     *                      expectation.
     * @param errorListener The destination for error messages. May be null, in which case
     *                      the errorListener registered with this Configuration is used.
     * @return the actual target namespace of the schema
     */

    public String readInlineSchema(NodeInfo root, String expected, ErrorListener errorListener)
            throws SchemaException {
        needEnterpriseEdition();
        return null;
    }

    /**
     * Throw an error indicating that a request cannot be satisfied because it requires
     * the schema-aware edition of Saxon
     */

    private void needEnterpriseEdition() {
        throw new UnsupportedOperationException(
                "You need the Enterprise Edition of Saxon for this operation");
    }

    /**
     * Load a schema, which will be available for use by all subsequent operations using
     * this Configuration. Any errors will be notified to the ErrorListener associated with
     * this Configuration.
     *
     * @param schemaSource the JAXP Source object identifying the schema document to be loaded
     * @throws SchemaException               if the schema cannot be read or parsed or if it is invalid
     * @throws UnsupportedOperationException if the configuration is not schema-aware
     * @since 8.4
     */

    public void addSchemaSource(Source schemaSource) throws SchemaException {
        addSchemaSource(schemaSource, getErrorListener());
    }

    /**
     * Load a schema, which will be available for use by all subsequent operations using
     * this EnterpriseConfiguration.
     *
     * @param schemaSource  the JAXP Source object identifying the schema document to be loaded
     * @param errorListener the ErrorListener to be notified of any errors in the schema.
     * @throws SchemaException if the schema cannot be read or parsed or if it is invalid
     */

    public void addSchemaSource(Source schemaSource, ErrorListener errorListener) throws SchemaException {
        needEnterpriseEdition();
    }

    /**
     * Determine whether the Configuration contains a cached schema for a given target namespace
     *
     * @param targetNamespace the target namespace of the schema being sought (supply "" for the
     *                        unnamed namespace)
     * @return true if the schema for this namespace is available, false if not.
     */

    public boolean isSchemaAvailable(String targetNamespace) {
        return false;
    }

    /**
     * Get the set of namespaces of imported schemas
     *
     * @return a Set whose members are the namespaces of all schemas in the schema cache, as
     *         String objects
     */

    public Set getImportedNamespaces() {
        return Collections.EMPTY_SET;
    }

    /**
     * Mark a schema namespace as being sealed. This is done when components from this namespace
     * are first used for validating a source document or compiling a source document or query. Once
     * a namespace has been sealed, it is not permitted to change the schema components in that namespace
     * by redefining them, deriving new types by extension, or adding to their substitution groups.
     *
     * @param namespace the namespace URI of the components to be sealed
     */

    public void sealNamespace(String namespace) {
        //
    }

    /**
     * Get the set of complex types that have been defined as extensions of a given type.
     * Note that we do not seal the schema namespace, so this list is not necessarily final; we must
     * assume that new extensions of built-in simple types can be added at any time
     * @param type the type whose extensions are required
     */

    public Iterator getExtensionsOfType(SchemaType type) {
        return Collections.EMPTY_SET.iterator();
    }

    /**
     * Import a precompiled Schema Component Model from a given Source. The schema components derived from this schema
     * document are added to the cache of schema components maintained by this SchemaManager
     *
     * @param source the XML file containing the schema component model, as generated by a previous call on
     *               {@link #exportComponents}
     */

    public void importComponents(Source source) throws XPathException {
        needEnterpriseEdition();
    }

    /**
     * Export a precompiled Schema Component Model containing all the components (except built-in components)
     * that have been loaded into this Processor.
     *
     * @param out the destination to recieve the precompiled Schema Component Model in the form of an
     *            XML document
     */

    public void exportComponents(Receiver out) throws XPathException {
        needEnterpriseEdition();
    }


    /**
     * Get a global element declaration.
     * <p/>
     * This method is intended for internal use.
     *
     * @param fingerprint the NamePool fingerprint of the name of the required
     *                    element declaration
     * @return the element declaration whose name matches the given
     *         fingerprint, or null if no element declaration with this name has
     *         been registered.
     */

    public SchemaDeclaration getElementDeclaration(int fingerprint) {
        return null;
    }

    /**
     * Get a global attribute declaration.
     * <p/>
     * This method is intended for internal use
     *
     * @param fingerprint the namepool fingerprint of the required attribute
     *                    declaration
     * @return the attribute declaration whose name matches the given
     *         fingerprint, or null if no element declaration with this name has
     *         been registered.
     */

    public SchemaDeclaration getAttributeDeclaration(int fingerprint) {
        return null;
    }

    /**
     * Get the top-level schema type definition with a given fingerprint.
     * <p/>
     * This method is intended for internal use and for use by advanced
     * applications. (The SchemaType object returned cannot yet be considered
     * a stable API, and may be superseded when a JAXP API for schema information
     * is defined.)
     *
     * @param fingerprint the fingerprint of the schema type
     * @return the schema type , or null if there is none
     *         with this name.
     */

    public SchemaType getSchemaType(int fingerprint) {
        if (fingerprint < 1023) {
            return BuiltInType.getSchemaType(fingerprint);
        }
        return getExternalObjectType(fingerprint);
    }

    /**
     * Get the external object type corresponding to a fingerprint if it is indeed an external object
     * type, otherwise return null
     * @param fingerprint the name of the type
     * @return  the external object type it the name is in the JAVA_TYPE namespace, otherwise null.
     */

    protected ExternalObjectType getExternalObjectType(int fingerprint) {
        if (getNamePool().getURI(fingerprint).equals(NamespaceConstant.JAVA_TYPE)) {
            try {
                Class namedClass = dynamicLoader.getClass(getNamePool().getLocalName(fingerprint), false, null);
                if (namedClass == null) {
                    return null;
                }
                return new ExternalObjectType(namedClass, this);
            } catch (XPathException err) {
                return null;
            }
        }
        return null;
    }

    /**
     * Check that a type is validly derived from another type, following the rules for the Schema Component
     * Constraint "Is Type Derivation OK (Simple)" (3.14.6) or "Is Type Derivation OK (Complex)" (3.4.6) as
     * appropriate.
     *
     * @param derived the derived type
     * @param base    the base type; the algorithm tests whether derivation from this type is permitted
     * @param block   the derivations that are blocked by the relevant element declaration
     * @throws SchemaException if the derivation is not allowed
     */

    public void checkTypeDerivationIsOK(SchemaType derived, SchemaType base, int block)
            throws SchemaException, ValidationException {
        // no action. Although the method can be used to check built-in types, it is never
        // needed in the non-schema-aware product
    }

    /**
     * Get a document-level validator to add to a Receiver pipeline.
     * <p/>
     * This method is intended for internal use.
     *
     * @param receiver            The receiver to which events should be sent after validation
     * @param systemId            the base URI of the document being validated
     * @param validationMode      for example Validation.STRICT or Validation.STRIP. The integer may
     *                            also have the bit Validation.VALIDATE_OUTPUT set, indicating that the strean being validated
     *                            is to be treated as a final output stream (which means multiple errors can be reported)
     * @param stripSpace          options for space stripping
     * @param schemaType          The type against which the outermost element of the document must be validated
     *                            (null if there is no constraint)
     * @param topLevelElementName the namepool name code of the required top-level element in the instance
     *                            document, or -1 if there is no specific element required
     * @return A Receiver to which events can be sent for validation
     */

    public Receiver getDocumentValidator(Receiver receiver,
                                         String systemId,
                                         int validationMode,
                                         int stripSpace,
                                         SchemaType schemaType,
                                         int topLevelElementName) {
        // non-schema-aware version
        return receiver;
    }

    /**
     * Get a Receiver that can be used to validate an element, and that passes the validated
     * element on to a target receiver. If validation is not supported, the returned receiver
     * will be the target receiver.
     * <p/>
     * This method is intended for internal use.
     *
     * @param receiver   the target receiver tp receive the validated element
     * @param nameCode   the nameCode of the element to be validated. This must correspond to the
     *                   name of an element declaration in a loaded schema
     * @param locationId current location in the stylesheet or query
     * @param schemaType the schema type (typically a complex type) against which the element is to
     *                   be validated
     * @param validation The validation mode, for example Validation.STRICT or Validation.LAX
     * @return The target receiver, indicating that with this configuration, no validation
     *         is performed.
     */
    public SequenceReceiver getElementValidator(SequenceReceiver receiver,
                                                int nameCode,
                                                int locationId,
                                                SchemaType schemaType,
                                                int validation)
            throws XPathException {
        return receiver;
    }

    /**
     * Validate an attribute value.
     * <p/>
     * This method is intended for internal use.
     *
     * @param nameCode   the name of the attribute
     * @param value      the value of the attribute as a string
     * @param validation STRICT or LAX
     * @return the type annotation to apply to the attribute node
     * @throws ValidationException if the value is invalid
     */

    public int validateAttribute(int nameCode, CharSequence value, int validation)
            throws ValidationException {
        return StandardNames.XS_UNTYPED_ATOMIC;
    }

    /**
     * Add to a pipeline a receiver that strips all type annotations. This
     * has a null implementation in the Saxon-B product, because type annotations
     * can never arise.
     * <p/>
     * This method is intended for internal use.
     *
     * @param destination the Receiver that events will be written to after whitespace stripping
     * @return the Receiver to which events should be sent for stripping
     */

    public Receiver getAnnotationStripper(Receiver destination) {
        return destination;
    }

    /**
     * Create a new SAX XMLReader object using the class name provided. <br>
     * <p/>
     * The named class must exist and must implement the
     * org.xml.sax.XMLReader or Parser interface. <br>
     * <p/>
     * This method returns an instance of the parser named.
     * <p/>
     * This method is intended for internal use.
     *
     * @param className A string containing the name of the
     *                  SAX parser class, for example "com.microstar.sax.LarkDriver"
     * @return an instance of the Parser class named, or null if it is not
     *         loadable or is not a Parser.
     */
    public XMLReader makeParser(String className)
            throws TransformerFactoryConfigurationError {
        Object obj;
        try {
            obj = dynamicLoader.getInstance(className, null);
        } catch (XPathException err) {
            throw new TransformerFactoryConfigurationError(err);
        }
        if (obj instanceof XMLReader) {
            return (XMLReader)obj;
        }
        throw new TransformerFactoryConfigurationError("Class " + className +
                " is not a SAX2 XMLReader");
    }

    /**
     * Get a locale given a language code in XML format.
     * <p/>
     * This method is intended for internal use.
     *
     * @param lang the language code
     * @return the Java locale
     */

    public static Locale getLocale(String lang) {
        int hyphen = lang.indexOf("-");
        String language, country;
        if (hyphen < 1) {
            language = lang;
            country = "";
        } else {
            language = lang.substring(1, hyphen);
            country = lang.substring(hyphen + 1);
        }
        return new Locale(language, country);
    }

    /**
     * Set the debugger to be used.
     * <p/>
     * This method is provided for advanced users only, and is subject to change.
     *
     * @param debugger the debugger to be used.
     */

    public void setDebugger(Debugger debugger) {
        this.debugger = debugger;
    }

    /**
     * Get the debugger in use. This will be null if no debugger has been registered.
     * <p/>
     * This method is provided for advanced users only, and is subject to change.
     *
     * @return the debugger in use, or null if none is in use
     */

    public Debugger getDebugger() {
        return debugger;
    }

    /**
     * Factory method to create a SlotManager.
     * <p/>
     * This method is provided for advanced users only, and is subject to change.
     *
     * @return a SlotManager (which is a skeletal stack frame representing the mapping of variable
     *         names to slots on the stack frame)
     */

    public SlotManager makeSlotManager() {
        if (debugger == null) {
            return new SlotManager();
        } else {
            return debugger.makeSlotManager();
        }
    }

    /**
     * Create a streaming transformer
     * @param context the initial XPath context
     * @param mode the initial mode, which must be a streaming mode
     * @return a Receiver to which the streamed input document will be pushed
     */

    public Receiver makeStreamingTransformer(XPathContext context, Mode mode) {
        // Streaming is only available in Saxon-EE
        return null;
    }

    /**
     * Factory method to get an Optimizer.
     * <p/>
     * This method is intended for internal use only.
     *
     * @return the optimizer used in this configuration
     */

    public Optimizer getOptimizer() {
        if (optimizer == null) {
            optimizer = new Optimizer(this);
            optimizer.setOptimizationLevel(optimizationLevel);
        }
        return optimizer;
    }

    /**
     * Factory method to get the StyleNodeFactory, used for constructing elements
     * in a stylesheet document
     * @return the StyleNodeFactory used in this Configuration
     */

    public StyleNodeFactory getStyleNodeFactory() {
        if (styleNodeFactory == null) {
            styleNodeFactory = new StyleNodeFactory(this);
        }
        return styleNodeFactory;
    }

    /**
     * Load a named collator class and check it is OK.
     * <p/>
     * This method is intended for internal use only.
     *
     * @param className the name of the collator class
     * @return a StringCollator to implement a collation
     */

    public StringCollator makeCollator(String className) throws XPathException {
        Object handler = dynamicLoader.getInstance(className, null);

        if (handler instanceof StringCollator) {
            return (StringCollator)handler;
        } else {
            throw new XPathException("Failed to load collation class " + className +
                    ": it is not an instance of net.sf.saxon.sort.StringCollator");
        }

    }

    /**
     * Set lazy construction mode on or off. In lazy construction mode, element constructors
     * are not evaluated until the content of the tree is required. Lazy construction mode
     * is currently experimental and is therefore off by default.
     *
     * @param lazy true to switch lazy construction mode on, false to switch it off.
     */

    public void setLazyConstructionMode(boolean lazy) {
        lazyConstructionMode = lazy;
    }

    /**
     * Determine whether lazy construction mode is on or off. In lazy construction mode, element constructors
     * are not evaluated until the content of the tree is required. Lazy construction mode
     * is currently experimental and is therefore off by default.
     *
     * @return true if lazy construction mode is enabled
     */

    public boolean isLazyConstructionMode() {
        return lazyConstructionMode;
    }

    /**
     * Register an external object model with this Configuration.
     *
     * @param model The external object model.
     *              This can either be one of the system-supplied external
     *              object models for JDOM, XOM, or DOM, or a user-supplied external object model.
     *              <p/>
     *              This method is intended for advanced users only, and is subject to change.
     */

    public void registerExternalObjectModel(ExternalObjectModel model) {
        if (externalObjectModels == null) {
            externalObjectModels = new ArrayList<ExternalObjectModel>(4);
        }
        if (!externalObjectModels.contains(model)) {
            externalObjectModels.add(model);
        }
    }

    /**
     * Get the external object model with a given URI, if registered
     * @param uri the identifying URI of the required external object model
     * @return the requested external object model if available, or null otherwise
     */

    public ExternalObjectModel getExternalObjectModel(String uri) {
        for (int m=0; m<externalObjectModels.size(); m++) {
            ExternalObjectModel model = externalObjectModels.get(m);
            if (model.getIdentifyingURI().equals(uri)) {
                return model;
            }
        }
        return null;
    }

    /**
     * Get the external object model that recognizes a particular class of node, if available
     * @param nodeClass the class of the Node object in the external object model
     * @return the requested external object model if available, or null otherwise
     */

    public ExternalObjectModel getExternalObjectModel(Class nodeClass) {
        for (int m=0; m<externalObjectModels.size(); m++) {
            ExternalObjectModel model = externalObjectModels.get(m);
            PJConverter converter = model.getPJConverter(nodeClass);
            if (converter != null) {
                return model;
            }
        }
        return null;
    }

    /**
     * Get all the registered external object models.
     * <p/>
     * This method is intended for internal use only.
     *
     * @return a list of external object models supported. The members of the list are of
     *         type {@link ExternalObjectModel}
     */

    public List<ExternalObjectModel> getExternalObjectModels() {
        return externalObjectModels;
    }

    /**
     * Get a NodeInfo corresponding to a DOM or other external Node,
     * either by wrapping or unwrapping the external Node.
     * <p/>
     * This method is intended for internal use.
     *
     * @param source A Source representing the wrapped or unwrapped external Node. This will typically
     *               be a DOMSource, but it may be a similar Source recognized by some other registered external
     *               object model.
     * @return If the Source is a DOMSource and the underlying node is a wrapper around a Saxon NodeInfo,
     *         returns the wrapped Saxon NodeInfo. If the Source is a DOMSource and the undelying node is not such a wrapper,
     *         returns a new Saxon NodeInfo that wraps the DOM Node. If the Source is any other kind of source, it
     *         is offered to each registered external object model for similar treatment. The result is the
     *         NodeInfo object obtained by wrapping or unwrapping the supplied external node.
     * @throws IllegalArgumentException if the source object is not of a recognized class. This method does
     *                                  <emph>not</emph> call the registered {@link SourceResolver to resolve the Source}.
     */

    public NodeInfo unravel(Source source) {
        List<ExternalObjectModel> externalObjectModels = getExternalObjectModels();
        for (int m = 0; m < externalObjectModels.size(); m++) {
            ExternalObjectModel model = externalObjectModels.get(m);
            NodeInfo node = model.unravel(source, this);
            if (node != null) {
                if (node.getConfiguration() != this) {
                    throw new IllegalArgumentException("Externally supplied Node belongs to the wrong Configuration");
                }
                return node;
            }
        }
        if (source instanceof NodeInfo) {
            if (((NodeInfo)source).getConfiguration() != this) {
                throw new IllegalArgumentException("Externally supplied NodeInfo belongs to the wrong Configuration");
            }
            return (NodeInfo)source;
        }
        if (source instanceof DOMSource) {
            throw new IllegalArgumentException("When a DOMSource is used, saxon9-dom.jar must be on the classpath");
        } else {
            throw new IllegalArgumentException("A source of class " +
                    source.getClass() + " is not recognized by any registered object model");
        }
    }

    /**
     * Set the level of DOM interface to be used
     *
     * @param level the DOM level. Must be 2 or 3. By default Saxon assumes that DOM level 3 is available;
     *              this parameter can be set to the value 2 to indicate that Saxon should not use methods unless they
     *              are available in DOM level 2. From Saxon 9.2, this switch remains available, but the use of
     *              DOM level 2 is untested and unsupported.
     */

    public void setDOMLevel(int level) {
        if (!(level == 2 || level == 3)) {
            throw new IllegalArgumentException("DOM Level must be 2 or 3");
        }
        domLevel = level;
    }

    /**
     * Get the level of DOM interface to be used
     *
     * @return the DOM level. Always 2 or 3.
     */

    public int getDOMLevel() {
        return domLevel;
    }

    /**
     * Get a new StaticQueryContext (which is also the factory class for creating a query parser)
     * @return a new StaticQueryContext
     */

    public StaticQueryContext newStaticQueryContext() {
        return new StaticQueryContext(this, false);
    }

    /**
     * Get a new Pending Update List
     *
     * @return the new Pending Update List
     * @throws UnsupportedOperationException if called when using Saxon-B
     */

    public PendingUpdateList newPendingUpdateList() {
        throw new UnsupportedOperationException("XQuery update is supported only in Saxon-EE");
    }

    /**
     * Make a PipelineConfiguration from the properties of this Configuration
     *
     * @return a new PipelineConfiguration
     * @since 8.4
     */

    public PipelineConfiguration makePipelineConfiguration() {
        PipelineConfiguration pipe = new PipelineConfiguration();
        pipe.setConfiguration(this);
        //pipe.setErrorListener(getErrorListener());
        pipe.setURIResolver(getURIResolver());
        pipe.setSchemaURIResolver(getSchemaURIResolver());
        pipe.setHostLanguage(getHostLanguage());
        pipe.setParseOptions(new ParseOptions(defaultParseOptions));
        //pipe.setExpandAttributeDefaults(isExpandAttributeDefaults());
        //pipe.setUseXsiSchemaLocation(useXsiSchemaLocation);
        return pipe;
    }

    /**
     * Get the configuration, given the context. This is provided as a static method to make it accessible
     * as an extension function.
     *
     * @param context the XPath dynamic context
     * @return the Saxon Configuration for a given XPath dynamic context
     */

    public static Configuration getConfiguration(XPathContext context) {
        return context.getConfiguration();
    }

    /**
     * Get the type of function items (values representing a function that can be
     * used as an argument to higher-order functions)
     */

//    public AtomicType getFunctionItemType() {
//        throw new UnsupportedOperationException("Higher-order functions require Saxon-EE");
//    }

    /**
     * Supply a SourceResolver. This is used for handling unknown implementations of the
     * {@link javax.xml.transform.Source} interface: a user-supplied SourceResolver can handle
     * such Source objects and translate them to a kind of Source that Saxon understands.
     *
     * @param resolver the source resolver.
     */

    public void setSourceResolver(SourceResolver resolver) {
        sourceResolver = resolver;
    }

    /**
     * Get the current SourceResolver. If none has been supplied, a system-defined SourceResolver
     * is returned.
     *
     * @return the current SourceResolver
     */

    public SourceResolver getSourceResolver() {
        return sourceResolver;
    }

    /**
     * Resolve a Source.
     * @param source A source object, typically the source supplied as the first
     * argument to {@link javax.xml.transform.Transformer#transform(javax.xml.transform.Source, javax.xml.transform.Result)}
     * or similar methods.
     * @param config The Configuration. This provides the SourceResolver with access to
     * configuration information; it also allows the SourceResolver to invoke the
     * resolveSource() method on the Configuration object as a fallback implementation.
     * @return a source object that Saxon knows how to process. This must be an instance of one
     * of the classes  StreamSource, SAXSource, DOMSource, {@link net.sf.saxon.AugmentedSource},
     *  {@link net.sf.saxon.om.NodeInfo},
     * or {@link net.sf.saxon.pull.PullSource}. Return null if the Source object is not
     * recognized
     * @throws XPathException if the Source object is recognized but cannot be processed
    */

    public Source resolveSource(Source source, Configuration config) throws XPathException {
        if (source instanceof AugmentedSource) {
            return source;
        }
        if (source instanceof StreamSource) {
            return source;
        }
        if (source instanceof SAXSource) {
            return source;
        }
        if (source instanceof DOMSource) {
            return source;
        }
        if (source instanceof NodeInfo) {
            return source;
        }
        if (source instanceof PullSource) {
            return source;
        }
        if (source instanceof PullEventSource) {
            return source;
        }
        return null;
    }

    /**
     * Build a document tree, using options set on this Configuration and on the supplied source
     * object. Options set on the source object override options set in the Configuration. The Source
     * object must be one of the kinds of source recognized by Saxon, or a source that can be resolved
     * using the registered {@link SourceResolver}.
     *
     * @param source the Source to be used. This may be an {@link AugmentedSource}, allowing options
     *               to be specified for the way in which this document will be built. If an AugmentedSource
     *               is supplied then options set in the AugmentedSource take precendence over options
     *               set in the Configuration.
     *        <p>From Saxon 9.2, this method always creates a new tree, it never wraps or returns
     *        an existing tree.</p>
     *
     * @return the document node of the constructed document
     * @throws XPathException if any errors occur during document parsing or validation. Detailed
     *                        errors occurring during schema validation will be written to the ErrorListener associated
     *                        with the AugmentedSource, if supplied, or with the Configuration otherwise.
     * @since 8.9. Modified in 9.0 to avoid copying a supplied document where this is not
     *        necessary. Modified in 9.2 so that this interface always constructs a new tree; it never
     *        wraps an existing document, even if an AugmentedSource that requests wrapping is supplied.
     */

    public DocumentInfo buildDocument(Source source) throws XPathException {

        if (source == null) {
            throw new NullPointerException("source");
        }

        // Resolve user-defined implementations of Source
        source = resolveSource(source, this);   // TODO the called buildDocument() does this again redundantly
        if (source == null) {
            throw new XPathException("Unknown source class");
        }

        ParseOptions options;
        Source underlyingSource = source;
        if (source instanceof AugmentedSource) {
            options = ((AugmentedSource)source).getParseOptions();
            underlyingSource = ((AugmentedSource)source).getContainedSource();
        } else {
            options = new ParseOptions();
        }

        source = underlyingSource;
        return buildDocument(source, options);
    }

    /**
     * Build a document, using specified options for parsing and building. This method always
     * constructs a new tree, it never wraps an existing document (regardless of anything in
     * the parseOptions)
     * @param source the source of the document to be constructed. If this is an
     * AugmentedSource, then any parser options contained in the AugmentedSource take precedence
     * over options specified in the parseOptions argument.
     * @param parseOptions options for parsing and constructing the document. Any options that
     * are not explicitly set in parseOptions default first to the values supplied in the source
     * argument if it is an AugmentedSource, and then to the values set in this Configuration.
     * The supplied parseOptions object is not modified.
     * @return the document node of the constructed document
     * @throws XPathException if parsing fails, or if the Source represents a node other than
     * a document node
     * @since 9.2
     */

    public DocumentInfo buildDocument(Source source, ParseOptions parseOptions) throws XPathException {
        if (source == null) {
            throw new NullPointerException("source");
        }

        ParseOptions options = new ParseOptions(parseOptions);

        // Resolve user-defined implementations of Source
        source = resolveSource(source, this);
        if (source == null) {
            throw new XPathException("Unknown source class");
        }

        // Create a pipeline configuration
        PipelineConfiguration pipe = makePipelineConfiguration();

        if (source instanceof AugmentedSource) {
            options.merge(((AugmentedSource)source).getParseOptions());
        }

        options.applyDefaults(this);

        // Create an appropriate Builder

        TreeModel treeModel = options.getModel();

        // Decide whether line numbering is in use

        boolean lineNumbering = options.isLineNumbering();

        Builder b1 = treeModel.makeBuilder();
        b1.setTiming(isTiming());
        b1.setLineNumbering(lineNumbering);
        b1.setPipelineConfiguration(pipe);
        Builder b = b1;
        new Sender(pipe).send(source, b, options);

        // Get the constructed document

        NodeInfo newdoc = b.getCurrentRoot();
        if (!(newdoc instanceof DocumentInfo)) {
            throw new XPathException("Source object represents a node other than a document node");
        }

        // Reset the builder, detaching it from the constructed document

        b.reset();

        // If requested, close the input stream

        if (parseOptions.isPleaseCloseAfterUse()) {
            ParseOptions.close(source);
        }

        // Return the constructed document

        return (DocumentInfo)newdoc;

    }


    /**
     * Load a named output emitter or SAX2 ContentHandler and check it is OK.
     *
     * @param clarkName  the QName of the user-supplied ContentHandler (requested as a prefixed
     *                   value of the method attribute in xsl:output, or anywhere that serialization parameters
     *                   are allowed), encoded in Clark format as {uri}local
     * @param controller the Controller. Allows a local class loader to be used.
     * @return a Receiver (despite the name, it is not required to be an Emitter)
     */

    public Receiver makeEmitter(String clarkName, Controller controller) throws XPathException {
        int brace = clarkName.indexOf('}');
        String localName = clarkName.substring(brace + 1);
        int colon = localName.indexOf(':');
        String className = localName.substring(colon + 1);
        Object handler;
        try {
            handler = dynamicLoader.getInstance(className, controller.getClassLoader());
        } catch (XPathException e) {
            throw new XPathException("Cannot create user-supplied output method. " + e.getMessage(),
                    SaxonErrorCode.SXCH0004);
        }

        if (handler instanceof Receiver) {
            return (Receiver)handler;
        } else if (handler instanceof ContentHandler) {
            ContentHandlerProxy emitter = new ContentHandlerProxy();
            emitter.setUnderlyingContentHandler((ContentHandler)handler);
            emitter.setOutputProperties(controller.getOutputProperties());
            return emitter;
        } else {
            throw new XPathException("Output method " + className +
                    " is neither a Receiver nor a SAX2 ContentHandler");
        }

    }

    /**
     * Make an "unconstructed" (that is, lazily-constructed) element node
     * @param instr the instruction that creates the element
     * @param context the dynamic evaluation context
     * @return the lazily constructed element node
     */

    public NodeInfo makeUnconstructedElement(ElementCreator instr, XPathContext context)
            throws XPathException {
        throw new XPathException("Lazy element construction requires Saxon-PE");
    }

    /**
     * Make an "unconstructed" (that is, lazily-constructed) document node
     * @param instr the instruction that creates the document node
     * @param context the dynamic evaluation context
     * @return the lazily constructed document node
     */

    public NodeInfo makeUnconstructedDocument(DocumentInstr instr, XPathContext context)
            throws XPathException {
        throw new XPathException("Lazy document construction requires Saxon-PE");
    }

    /**
     * Set a property of the configuration. This method underpins the setAttribute() method of the
     * TransformerFactory implementation, and is provided
     * to enable setting of Configuration properties using URIs without instantiating a TransformerFactory:
     * specifically, this may be useful when running XQuery, and it is also used by the Validator API
     *
     * @param name  the URI identifying the property to be set. See the class {@link FeatureKeys} for
     *              constants representing the property names that can be set.
     * @param value the value of the property
     * @throws IllegalArgumentException if the property name is not recognized or if the value is not
     * a valid value for the named property
     */

    public void setConfigurationProperty(String name, Object value) {

        if (name.equals(FeatureKeys.ALLOW_EXTERNAL_FUNCTIONS)) {
            boolean b = requireBoolean(name, value);
            setAllowExternalFunctions(b);

        } else if (name.equals(FeatureKeys.COLLATION_URI_RESOLVER)) {
            if (!(value instanceof CollationURIResolver)) {
                throw new IllegalArgumentException(
                        "COLLATION_URI_RESOLVER value must be an instance of net.sf.saxon.sort.CollationURIResolver");
            }
            setCollationURIResolver((CollationURIResolver)value);

        } else if (name.equals(FeatureKeys.COLLATION_URI_RESOLVER_CLASS)) {
            setCollationURIResolver(
                    (CollationURIResolver)instantiateClassName(name, value, CollationURIResolver.class));

        } else if (name.equals(FeatureKeys.COLLECTION_URI_RESOLVER)) {
            if (!(value instanceof CollectionURIResolver)) {
                throw new IllegalArgumentException(
                        "COLLECTION_URI_RESOLVER value must be an instance of net.sf.saxon.CollectionURIResolver");
            }
            setCollectionURIResolver((CollectionURIResolver)value);

        } else if (name.equals(FeatureKeys.COLLECTION_URI_RESOLVER_CLASS)) {
            setCollectionURIResolver(
                    (CollectionURIResolver)instantiateClassName(name, value, CollectionURIResolver.class));

        } else if (name.equals(FeatureKeys.COMPILE_WITH_TRACING)) {
            boolean b = requireBoolean("COMPILE_WITH_TRACING", value);
            setCompileWithTracing(b);

        } else if (name.equals(FeatureKeys.DEFAULT_COLLATION)) {
            getCollationMap().setDefaultCollationName(value.toString());

        } else if (name.equals(FeatureKeys.DEFAULT_COLLECTION)) {
            setDefaultCollection(value.toString());

        } else if (name.equals(FeatureKeys.DEFAULT_COUNTRY)) {
            setDefaultCountry(value.toString());

        } else if (name.equals(FeatureKeys.DEFAULT_LANGUAGE)) {
            setDefaultLanguage(value.toString());

        } else if (name.equals(FeatureKeys.DTD_VALIDATION)) {
            boolean b = requireBoolean(name, value);
            setValidation(b);

        } else if (name.equals(FeatureKeys.DTD_VALIDATION_RECOVERABLE)) {
            boolean b = requireBoolean(name, value);
            if (b) {
                defaultParseOptions.setDTDValidationMode(Validation.LAX);
            } else {
                defaultParseOptions.setDTDValidationMode(isValidation() ? Validation.STRICT : Validation.SKIP);
            }

        } else if (name.equals(FeatureKeys.ERROR_LISTENER_CLASS)) {
            setErrorListener((ErrorListener)instantiateClassName(name, value, ErrorListener.class));

        } else if (name.equals(FeatureKeys.EXPAND_ATTRIBUTE_DEFAULTS)) {
            boolean b = requireBoolean(name, value);
            setExpandAttributeDefaults(b);

        } else if (name.equals(FeatureKeys.LAZY_CONSTRUCTION_MODE)) {
            boolean b = requireBoolean(name, value);
            setLazyConstructionMode(b);

        } else if (name.equals(FeatureKeys.LINE_NUMBERING)) {
            boolean b = requireBoolean(name, value);
            setLineNumbering(b);

        } else if (name.equals(FeatureKeys.MESSAGE_EMITTER_CLASS)) {
            if (!(value instanceof String)) {
                throw new IllegalArgumentException("MESSAGE_EMITTER_CLASS class must be a String");
            }
            setMessageEmitterClass((String)value);

        } else if (name.equals(FeatureKeys.MODULE_URI_RESOLVER)) {
            if (!(value instanceof ModuleURIResolver)) {
                throw new IllegalArgumentException(
                        "MODULE_URI_RESOLVER value must be an instance of net.sf.saxon.query.ModuleURIResolver");
            }
            setModuleURIResolver((ModuleURIResolver)value);

        } else if (name.equals(FeatureKeys.MODULE_URI_RESOLVER_CLASS)) {
            setModuleURIResolver(
                    (ModuleURIResolver)instantiateClassName(name, value, ModuleURIResolver.class));

        } else if (name.equals(FeatureKeys.NAME_POOL)) {
            if (!(value instanceof NamePool)) {
                throw new IllegalArgumentException("NAME_POOL value must be an instance of net.sf.saxon.om.NamePool");
            }
            setNamePool((NamePool)value);

        } else if (name.equals(FeatureKeys.OPTIMIZATION_LEVEL)) {
            if (!(value instanceof String)) {
                throw new IllegalArgumentException("OPTIMIZATION_LEVEL value must be string");
            }
            try {
                optimizationLevel = Integer.parseInt((String)value);
                if (optimizationLevel < Optimizer.NO_OPTIMIZATION || optimizationLevel > Optimizer.FULL_OPTIMIZATION) {
                    throw new IllegalArgumentException("OPTIMIZATION_LEVEL must be in the range " +
                            Optimizer.NO_OPTIMIZATION + " to " + Optimizer.FULL_OPTIMIZATION);
                }
                if (optimizer != null) {
                    optimizer.setOptimizationLevel(optimizationLevel);
                }
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException("OPTIMIZATION_LEVEL value must be a number represented as a string");
            }

        } else if (name.equals(FeatureKeys.OUTPUT_URI_RESOLVER)) {
            if (!(value instanceof OutputURIResolver)) {
                throw new IllegalArgumentException(
                        "OUTPUT_URI_RESOLVER value must be an instance of net.sf.saxon.OutputURIResolver");
            }
            setOutputURIResolver((OutputURIResolver)value);

        } else if (name.equals(FeatureKeys.OUTPUT_URI_RESOLVER_CLASS)) {
            setOutputURIResolver(
                    (OutputURIResolver)instantiateClassName(name, value, OutputURIResolver.class));

        } else if (name.equals(FeatureKeys.PRE_EVALUATE_DOC_FUNCTION)) {
            preEvaluateDocFunction = requireBoolean("PRE_EVALUATE_DOC_FUNCTION", value);

        } else if (name.equals(FeatureKeys.PREFER_JAXP_PARSER)) {
            preferJaxpParser = requireBoolean("PREFER_JAXP_PARSER", value);            

        } else if (name.equals(FeatureKeys.RECOGNIZE_URI_QUERY_PARAMETERS)) {
            boolean b = requireBoolean(name, value);
            getSystemURIResolver().setRecognizeQueryParameters(b);

        } else if (name.equals(FeatureKeys.RECOVERY_POLICY)) {
            if (!(value instanceof Integer)) {
                throw new IllegalArgumentException("RECOVERY_POLICY value must be an Integer");
            }
            setRecoveryPolicy(((Integer)value).intValue());

        } else if (name.equals(FeatureKeys.RECOVERY_POLICY_NAME)) {
            if (!(value instanceof String)) {
                throw new IllegalArgumentException("RECOVERY_POLICY_NAME value must be a String");
            }
            int rval;
            if (value.equals("recoverSilently")) {
                rval = RECOVER_SILENTLY;
            } else if (value.equals("recoverWithWarnings")) {
                rval = RECOVER_WITH_WARNINGS;
            } else if (value.equals("doNotRecover")) {
                rval = DO_NOT_RECOVER;
            } else {
                throw new IllegalArgumentException(
                        "Unrecognized value of RECOVERY_POLICY_NAME = '" + value +
                                "': must be 'recoverSilently', 'recoverWithWarnings', or 'doNotRecover'");
            }
            setRecoveryPolicy(rval);

        } else if (name.equals(FeatureKeys.SERIALIZER_FACTORY_CLASS)) {
            setSerializerFactory(
                    (SerializerFactory)instantiateClassName(name, value, OutputURIResolver.class));

        } else if (name.equals(FeatureKeys.SCHEMA_URI_RESOLVER)) {
            if (!(value instanceof SchemaURIResolver)) {
                throw new IllegalArgumentException(
                        "SCHEMA_URI_RESOLVER value must be an instance of net.sf.saxon.type.SchemaURIResolver");
            }
            setSchemaURIResolver((SchemaURIResolver)value);

        } else if (name.equals(FeatureKeys.SCHEMA_URI_RESOLVER_CLASS)) {
            setSchemaURIResolver(
                    (SchemaURIResolver)instantiateClassName(name, value, SchemaURIResolver.class));


        } else if (name.equals(FeatureKeys.SCHEMA_VALIDATION)) {
            if (!(value instanceof Integer)) {
                throw new IllegalArgumentException("SCHEMA_VALIDATION must be an integer");
            }
            setSchemaValidationMode(((Integer)value).intValue());

        } else if (name.equals(FeatureKeys.SCHEMA_VALIDATION_MODE)) {
            if (!(value instanceof String)) {
                throw new IllegalArgumentException("SCHEMA_VALIDATION_MODE must be a string");
            }
            setSchemaValidationMode(Validation.getCode((String)value));

        } else if (name.equals(FeatureKeys.SOURCE_PARSER_CLASS)) {
            if (!(value instanceof String)) {
                throw new IllegalArgumentException("SOURCE_PARSER_CLASS class must be a String");
            }
            setSourceParserClass((String)value);

        } else if (name.equals(FeatureKeys.SOURCE_RESOLVER_CLASS)) {
            setSourceResolver(
                    (SourceResolver)instantiateClassName(name, value, SourceResolver.class));

        } else if (name.equals(FeatureKeys.STRIP_WHITESPACE)) {
            if (!(value instanceof String)) {
                throw new IllegalArgumentException("STRIP_WHITESPACE must be a string");
            }
            int ival;
            if (value.equals("all")) {
                ival = Whitespace.ALL;
            } else if (value.equals("none")) {
                ival = Whitespace.NONE;
            } else if (value.equals("ignorable")) {
                ival = Whitespace.IGNORABLE;
            } else {
                throw new IllegalArgumentException(
                        "Unrecognized value STRIP_WHITESPACE = '" + value +
                                "': must be 'all', 'none', or 'ignorable'");
            }
            setStripsWhiteSpace(ival);


        } else if (name.equals(FeatureKeys.STYLE_PARSER_CLASS)) {
            if (!(value instanceof String)) {
                throw new IllegalArgumentException("STYLE_PARSER_CLASS class must be a String");
            }
            setStyleParserClass((String)value);

        } else if (name.equals(FeatureKeys.TIMING)) {
            setTiming(requireBoolean(name, value));

        } else if (name.equals(FeatureKeys.TRACE_EXTERNAL_FUNCTIONS)) {
            setTraceExternalFunctions(requireBoolean(name, value));

        } else if (name.equals(FeatureKeys.TRACE_OPTIMIZER_DECISIONS)) {
            setOptimizerTracing(requireBoolean(name, value));

        } else if (name.equals(FeatureKeys.TRACE_LISTENER)) {
            if (!(value instanceof TraceListener)) {
                throw new IllegalArgumentException("TRACE_LISTENER is of wrong class");
            }
            setTraceListener((TraceListener)value);

        } else if (name.equals(FeatureKeys.TRACE_LISTENER_CLASS)) {
            if (!(value instanceof String)) {
                throw new IllegalArgumentException("TRACE_LISTENER_CLASS must be a string");
            }
            setTraceListenerClass((String)value);

        } else if (name.equals(FeatureKeys.TREE_MODEL)) {
            if (!(value instanceof Integer)) {
                throw new IllegalArgumentException("Tree model must be an Integer");
            }
            setTreeModel(((Integer)value).intValue());

        } else if (name.equals(FeatureKeys.TREE_MODEL_NAME)) {
            if (!(value instanceof String)) {
                throw new IllegalArgumentException("TREE_MODEL_NAME must be a string");
            }
            if (value.equals("tinyTree")) {
                setTreeModel(Builder.TINY_TREE);
            } else if (value.equals("tinyTreeCondensed")) {
                setTreeModel(Builder.TINY_TREE_CONDENSED);
            } else if (value.equals("linkedTree")) {
                setTreeModel(Builder.LINKED_TREE);
            } else {
                throw new IllegalArgumentException(
                        "Unrecognized value TREE_MODEL_NAME = '" + value +
                                "': must be linkedTree|tinyTree|tinyTreeCondensed");
            }
        } else if (name.equals(FeatureKeys.URI_RESOLVER_CLASS)) {
            setURIResolver(
                    (URIResolver)instantiateClassName(name, value, URIResolver.class));

        } else if (name.equals(FeatureKeys.USE_PI_DISABLE_OUTPUT_ESCAPING)) {
            useDisableOutputEscaping = requireBoolean(name, value);

        } else if (name.equals(FeatureKeys.USE_TYPED_VALUE_CACHE)) {
            useTypedValueCache = requireBoolean(name, value);

        } else if (name.equals(FeatureKeys.USE_XSI_SCHEMA_LOCATION)) {
            defaultParseOptions.setUseXsiSchemaLocation(requireBoolean(name, value));

        } else if (name.equals(FeatureKeys.VALIDATION_WARNINGS)) {
            setValidationWarnings(requireBoolean(name, value));

        } else if (name.equals(FeatureKeys.VERSION_WARNING)) {
            setVersionWarning(requireBoolean(name, value));

        } else if (name.equals(FeatureKeys.XINCLUDE)) {
            setXIncludeAware(requireBoolean(name, value));

        } else if (name.equals(FeatureKeys.XQUERY_ALLOW_UPDATE)) {
            getDefaultStaticQueryContext().setUpdatingEnabled(requireBoolean(name, value));

        } else if (name.equals(FeatureKeys.XQUERY_CONSTRUCTION_MODE)) {
            getDefaultStaticQueryContext().setConstructionMode(Validation.getCode(value.toString()));

        } else if (name.equals(FeatureKeys.XQUERY_DEFAULT_ELEMENT_NAMESPACE)) {
            getDefaultStaticQueryContext().setDefaultElementNamespace(value.toString());

        } else if (name.equals(FeatureKeys.XQUERY_DEFAULT_FUNCTION_NAMESPACE)) {
            getDefaultStaticQueryContext().setDefaultFunctionNamespace(value.toString());

        } else if (name.equals(FeatureKeys.XQUERY_EMPTY_LEAST)) {
            getDefaultStaticQueryContext().setEmptyLeast(requireBoolean(name, value));

        } else if (name.equals(FeatureKeys.XQUERY_INHERIT_NAMESPACES)) {
            getDefaultStaticQueryContext().setInheritNamespaces(requireBoolean(name, value));

        } else if (name.equals(FeatureKeys.XQUERY_PRESERVE_BOUNDARY_SPACE)) {
            getDefaultStaticQueryContext().setPreserveBoundarySpace(requireBoolean(name, value));

        } else if (name.equals(FeatureKeys.XQUERY_PRESERVE_NAMESPACES)) {
            getDefaultStaticQueryContext().setPreserveNamespaces(requireBoolean(name, value));

        } else if (name.equals(FeatureKeys.XQUERY_REQUIRED_CONTEXT_ITEM_TYPE)) {
            ExpressionParser parser = new ExpressionParser();
            try {
                SequenceType type = parser.parseSequenceType(value.toString(), new IndependentContext(this));
                // TODO: disallow occurrence indicator
                getDefaultStaticQueryContext().setRequiredContextItemType(type.getPrimaryType());
            } catch (XPathException err) {
                throw new IllegalArgumentException(err);
            }

        } else if (name.equals(FeatureKeys.XQUERY_SCHEMA_AWARE)) {
            getDefaultStaticQueryContext().setSchemaAware(requireBoolean(name, value));

        } else if (name.equals(FeatureKeys.XQUERY_STATIC_ERROR_LISTENER_CLASS)) {
            getDefaultStaticQueryContext().setErrorListener(
                    (ErrorListener)instantiateClassName(name, value, ErrorListener.class));

        } else if (name.equals(FeatureKeys.XQUERY_VERSION)) {
            getDefaultStaticQueryContext().setLanguageVersion(value.toString());

        } else if (name.equals(FeatureKeys.XML_VERSION) || name.equals("http://saxon.sf.bet/feature/xml-version")) {
            // spelling mistake retained for backwards compatibility with 8.9 and earlier
            if (!(value instanceof String && (value.equals("1.0") || value.equals("1.1")))) {
                throw new IllegalArgumentException(
                        "XML_VERSION value must be \"1.0\" or \"1.1\" as a String");

            }
            setXMLVersion((value.equals("1.0") ? XML10 : XML11));

        } else if (name.equals(FeatureKeys.XSD_VERSION)) {
            if (!(value instanceof String && (value.equals("1.0") || value.equals("1.1")))) {
                throw new IllegalArgumentException(
                        "XSD_VERSION value must be \"1.0\" or \"1.1\" as a String");

            }
            xsdVersion = ((value.equals("1.0") ? XSD10 : XSD11));

        } else if (name.equals(FeatureKeys.XSLT_SCHEMA_AWARE)) {
            defaultXsltCompilerInfo.setSchemaAware(requireBoolean(name, value));

        } else if (name.equals(FeatureKeys.XSLT_STATIC_ERROR_LISTENER_CLASS)) {
            getDefaultXsltCompilerInfo().setErrorListener(
                    (ErrorListener)instantiateClassName(name, value, ErrorListener.class));

        } else if (name.equals(FeatureKeys.XSLT_STATIC_URI_RESOLVER_CLASS)) {
            getDefaultXsltCompilerInfo().setURIResolver(
                    (URIResolver)instantiateClassName(name, value, URIResolver.class));
        } else {
            throw new IllegalArgumentException("Unknown configuration option " + name);
        }
    }

    private boolean requireBoolean(String propertyName, Object value) {
        if (value instanceof Boolean) {
            return ((Boolean)value).booleanValue();
        } else if (value instanceof String) {
            if ("true".equals(value) || "on".equals(value)) {
                return true;
            } else if ("false".equals(value) || "off".equals(value)) {
                return false;
            } else {
                throw new IllegalArgumentException(propertyName + " must be 'true' or 'false'");
            }
        } else {
            throw new IllegalArgumentException(propertyName + " must be a boolean");
        }
    }

    protected Object instantiateClassName(String propertyName, Object value, Class requiredClass) {
        if (!(value instanceof String)) {
            throw new IllegalArgumentException(
                    propertyName + " must be a String");
        }
        try {
            Object obj = getInstance((String)value, null);
            if (!requiredClass.isAssignableFrom(obj.getClass())) {
                throw new IllegalArgumentException("Error in " + propertyName +
                        ": Class " + value + " does not implement " + requiredClass.getName());
            }
            return obj;
        } catch (XPathException err) {
            throw new IllegalArgumentException(
                    "Cannot instantiate COLLECTION_URI_RESOLVER_CLASS. " + err.getMessage());
        }
    }

    /**
     * Get a property of the configuration
     *
     * @param name the name of the required property. See the class {@link FeatureKeys} for
     *             constants representing the property names that can be requested.
     * @return the value of the property
     * @throws IllegalArgumentException thrown if the property is not one that Saxon recognizes.
     */

    public Object getConfigurationProperty(String name) {
        if (name.equals(FeatureKeys.ALLOW_EXTERNAL_FUNCTIONS)) {
            return Boolean.valueOf(isAllowExternalFunctions());

        } else if (name.equals(FeatureKeys.COLLATION_URI_RESOLVER)) {
            return getCollationURIResolver();

        } else if (name.equals(FeatureKeys.COLLATION_URI_RESOLVER_CLASS)) {
            return getCollationURIResolver().getClass().getName();

        } else if (name.equals(FeatureKeys.COLLECTION_URI_RESOLVER)) {
            return getCollectionURIResolver();

        } else if (name.equals(FeatureKeys.COLLECTION_URI_RESOLVER_CLASS)) {
            return getCollectionURIResolver().getClass().getName();            

        } else if (name.equals(FeatureKeys.COMPILE_WITH_TRACING)) {
            return Boolean.valueOf(isCompileWithTracing());

        } else if (name.equals(FeatureKeys.DEFAULT_COLLATION)) {
            return getCollationMap().getDefaultCollationName();

        } else if (name.equals(FeatureKeys.DEFAULT_COLLECTION)) {
            return getDefaultCollection();

        } else if (name.equals(FeatureKeys.DEFAULT_COUNTRY)) {
            return getDefaultCountry();

        } else if (name.equals(FeatureKeys.DEFAULT_LANGUAGE)) {
            return getDefaultLanguage();

        } else if (name.equals(FeatureKeys.DTD_VALIDATION)) {
            return Boolean.valueOf(isValidation());

        } else if (name.equals(FeatureKeys.DTD_VALIDATION_RECOVERABLE)) {
            return Boolean.valueOf(defaultParseOptions.getDTDValidationMode() == Validation.LAX);

        } else if (name.equals(FeatureKeys.ERROR_LISTENER_CLASS)) {
            return getErrorListener().getClass().getName();

        } else if (name.equals(FeatureKeys.EXPAND_ATTRIBUTE_DEFAULTS)) {
            return Boolean.valueOf(isExpandAttributeDefaults());

        } else if (name.equals(FeatureKeys.LINE_NUMBERING)) {
            return Boolean.valueOf(isLineNumbering());

        } else if (name.equals(FeatureKeys.MESSAGE_EMITTER_CLASS)) {
            return getMessageEmitterClass();

        } else if (name.equals(FeatureKeys.MODULE_URI_RESOLVER)) {
            return getModuleURIResolver();

        } else if (name.equals(FeatureKeys.MODULE_URI_RESOLVER_CLASS)) {
            return getModuleURIResolver().getClass().getName();            

        } else if (name.equals(FeatureKeys.NAME_POOL)) {
            return getNamePool();

        } else if (name.equals(FeatureKeys.OPTIMIZATION_LEVEL)) {
            return "" + optimizationLevel;

        } else if (name.equals(FeatureKeys.OUTPUT_URI_RESOLVER)) {
            return getOutputURIResolver();

        } else if (name.equals(FeatureKeys.OUTPUT_URI_RESOLVER_CLASS)) {
            return getOutputURIResolver().getClass().getName();

        } else if (name.equals(FeatureKeys.PRE_EVALUATE_DOC_FUNCTION)) {
            return Boolean.valueOf(preEvaluateDocFunction);

        } else if (name.equals(FeatureKeys.PREFER_JAXP_PARSER)) {
            return Boolean.valueOf(preferJaxpParser);

        } else if (name.equals(FeatureKeys.RECOGNIZE_URI_QUERY_PARAMETERS)) {
            return Boolean.valueOf(getSystemURIResolver().queryParametersAreRecognized());

        } else if (name.equals(FeatureKeys.RECOVERY_POLICY)) {
            return new Integer(getRecoveryPolicy());

        } else if (name.equals(FeatureKeys.RECOVERY_POLICY_NAME)) {
            switch (getRecoveryPolicy()) {
                case RECOVER_SILENTLY: return "recoverSilently";
                case RECOVER_WITH_WARNINGS: return "recoverWithWarnings";
                case DO_NOT_RECOVER: return "doNotRecover";
                default: return null;
            }

        } else if (name.equals(FeatureKeys.SCHEMA_URI_RESOLVER)) {
            return getSchemaURIResolver();

        } else if (name.equals(FeatureKeys.SCHEMA_URI_RESOLVER_CLASS)) {
            return getSchemaURIResolver().getClass().getName();

        } else if (name.equals(FeatureKeys.SCHEMA_VALIDATION)) {
            return new Integer(getSchemaValidationMode());

        } else if (name.equals(FeatureKeys.SCHEMA_VALIDATION_MODE)) {
            return Validation.toString(getSchemaValidationMode());

        } else if (name.equals(FeatureKeys.SERIALIZER_FACTORY_CLASS)) {
            return getSerializerFactory().getClass().getName();

        } else if (name.equals(FeatureKeys.SOURCE_PARSER_CLASS)) {
            return getSourceParserClass();

        } else if (name.equals(FeatureKeys.SOURCE_RESOLVER_CLASS)) {
            return getSourceResolver().getClass().getName();

        } else if (name.equals(FeatureKeys.STRIP_WHITESPACE)) {
            int s = getStripsWhiteSpace();
            if (s == Whitespace.ALL) {
                return "all";
            } else if (s == Whitespace.IGNORABLE) {
                return "ignorable";
            } else {
                return "none";
            }

        } else if (name.equals(FeatureKeys.STYLE_PARSER_CLASS)) {
            return getStyleParserClass();

        } else if (name.equals(FeatureKeys.TIMING)) {
            return Boolean.valueOf(isTiming());

        } else if (name.equals(FeatureKeys.TRACE_LISTENER)) {
            return traceListener;

        } else if (name.equals(FeatureKeys.TRACE_LISTENER_CLASS)) {
            return traceListenerClass;

        } else if (name.equals(FeatureKeys.TRACE_EXTERNAL_FUNCTIONS)) {
            return Boolean.valueOf(isTraceExternalFunctions());

        } else if (name.equals(FeatureKeys.TRACE_OPTIMIZER_DECISIONS)) {
            return Boolean.valueOf(isOptimizerTracing());

        } else if (name.equals(FeatureKeys.TREE_MODEL)) {
            return new Integer(getTreeModel());

        } else if (name.equals(FeatureKeys.TREE_MODEL_NAME)) {
            switch (getTreeModel()) {
                case Builder.TINY_TREE:
                default:
                    return "tinyTree";
                case Builder.TINY_TREE_CONDENSED:
                    return "tinyTreeCondensed";
                case Builder.LINKED_TREE:
                    return "linkedTree";
            }

        } else if (name.equals(FeatureKeys.URI_RESOLVER_CLASS)) {
            return getURIResolver().getClass().getName();  

        } else if (name.equals(FeatureKeys.USE_PI_DISABLE_OUTPUT_ESCAPING)) {
            return Boolean.valueOf(useDisableOutputEscaping);
            
        } else if (name.equals(FeatureKeys.USE_TYPED_VALUE_CACHE)) {
            return Boolean.valueOf(useTypedValueCache);

        } else if (name.equals(FeatureKeys.USE_XSI_SCHEMA_LOCATION)) {
            return Boolean.valueOf(defaultParseOptions.isUseXsiSchemaLocation());

        } else if (name.equals(FeatureKeys.VALIDATION_WARNINGS)) {
            return Boolean.valueOf(isValidationWarnings());

        } else if (name.equals(FeatureKeys.VERSION_WARNING)) {
            return Boolean.valueOf(isVersionWarning());

        } else if (name.equals(FeatureKeys.XINCLUDE)) {
            return Boolean.valueOf(isXIncludeAware());

        } else if (name.equals(FeatureKeys.XML_VERSION)) {
            // Spelling mistake retained for backwards compatibility with 8.9 and earlier
            return (getXMLVersion() == XML10 ? "1.0" : "1.1");

        } else if (name.equals(FeatureKeys.XQUERY_ALLOW_UPDATE)) {
            return getDefaultStaticQueryContext().isUpdatingEnabled();

        } else if (name.equals(FeatureKeys.XQUERY_CONSTRUCTION_MODE)) {
            return getDefaultStaticQueryContext().getConstructionMode();

        } else if (name.equals(FeatureKeys.XQUERY_DEFAULT_ELEMENT_NAMESPACE)) {
            return getDefaultStaticQueryContext().getDefaultElementNamespace();

        } else if (name.equals(FeatureKeys.XQUERY_DEFAULT_FUNCTION_NAMESPACE)) {
            return getDefaultStaticQueryContext().getDefaultFunctionNamespace();

        } else if (name.equals(FeatureKeys.XQUERY_EMPTY_LEAST)) {
            return getDefaultStaticQueryContext().isEmptyLeast();

        } else if (name.equals(FeatureKeys.XQUERY_INHERIT_NAMESPACES)) {
            return getDefaultStaticQueryContext().isInheritNamespaces();

        } else if (name.equals(FeatureKeys.XQUERY_PRESERVE_BOUNDARY_SPACE)) {
            return getDefaultStaticQueryContext().isPreserveBoundarySpace();

        } else if (name.equals(FeatureKeys.XQUERY_PRESERVE_NAMESPACES)) {
            return getDefaultStaticQueryContext().isPreserveNamespaces();

        } else if (name.equals(FeatureKeys.XQUERY_REQUIRED_CONTEXT_ITEM_TYPE)) {
            return getDefaultStaticQueryContext().getRequiredContextItemType();

        } else if (name.equals(FeatureKeys.XQUERY_SCHEMA_AWARE)) {
            return getDefaultStaticQueryContext().isSchemaAware();

        } else if (name.equals(FeatureKeys.XQUERY_STATIC_ERROR_LISTENER_CLASS)) {
            return getDefaultStaticQueryContext().getErrorListener().getClass().getName();

        } else if (name.equals(FeatureKeys.XQUERY_VERSION)) {
            return getDefaultStaticQueryContext().getLanguageVersion();

        } else if (name.equals(FeatureKeys.XSD_VERSION)) {
            return (xsdVersion == XSD10 ? "1.0" : "1.1");

        } else if (name.equals(FeatureKeys.XSLT_SCHEMA_AWARE)) {
            return Boolean.valueOf(defaultXsltCompilerInfo.isSchemaAware());

        } else if (name.equals(FeatureKeys.XSLT_STATIC_ERROR_LISTENER_CLASS)) {
            return getDefaultXsltCompilerInfo().getErrorListener().getClass().getName();

        } else if (name.equals(FeatureKeys.XSLT_STATIC_URI_RESOLVER_CLASS)) {
            return getDefaultXsltCompilerInfo().getURIResolver().getClass().getName();

        } else {
            throw new IllegalArgumentException("Unknown attribute " + name);
        }
    }


    public static class LicenseFeature {
        public static final int SCHEMA_VALIDATION = 1;
        public static final int SCHEMA_AWARE_XSLT = 2;
        public static final int SCHEMA_AWARE_XQUERY = 3;
        public static final int XOM_MODEL = 4;
        public static final int JDOM_MODEL = 5;
        public static final int DOM4J_MODEL = 6;
        public static final int SAXON_EXTENSION_FUNCTIONS = 7;
        public static final int SAXON_EXTENSION_INSTRUCTIONS = 8;
        public static final int COLLATIONS = 9;
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