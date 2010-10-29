package net.sf.saxon.s9api;

import net.sf.saxon.Configuration;
import net.sf.saxon.Version;
import net.sf.saxon.event.NamespaceReducer;
import net.sf.saxon.event.Receiver;
import net.sf.saxon.event.TreeReceiver;
import net.sf.saxon.lib.ExtensionFunctionDefinition;
import net.sf.saxon.om.Item;
import net.sf.saxon.om.NodeInfo;
import net.sf.saxon.trans.XPathException;

import javax.xml.transform.Source;
import java.io.File;
import java.io.OutputStream;
import java.io.Writer;
import java.util.Iterator;

/**
 * The <code>Processor</code> class serves three purposes: it allows global Saxon configuration options to be set;
 * it acts as a factory for generating XQuery, XPath, and XSLT compilers; and it owns certain shared
 * resources such as the Saxon NamePool and compiled schemas. This is the first object that a
 * Saxon application should create. Once established, a Processor may be used in multiple threads.
 *
 * <p>It is possible to run more than one Saxon Processor concurrently, but only when running completely
 * independent workloads. Nothing can be shared between Processor instances. Within a query or transformation,
 * all source documents and schemas must be built using the same Processor, which must also be used to
 * compile the query or stylesheet.</p>
 */

public class Processor {

    private Configuration config;
    private SchemaManager schemaManager;

    /**
     * Create a Processor
     * @param licensedEdition indicates whether the Processor requires features of Saxon that need a license
     * file (that is, features not available in Saxon HE (Home Edition). If true, the method will create
     * a Configuration appropriate to the version of the software that is running: for example, if running
     * Saxon-EE, it will create an EnterpriseConfiguration. The method does not at this stage check that a license
     * is available, and in the absence of a license, it should run successfully provided no features that
     * require licensing are actually used. If the argument is set to false, a plain Home Edition Configuration
     * is created unconditionally.
     */

    public Processor(boolean licensedEdition) {
        if (licensedEdition) {
            config = Configuration.newConfiguration();
            if (config.getEditionCode().equals("EE")) {
                schemaManager = new SchemaManager(config);
            }
        } else {
            config = new Configuration();
        }
        config.setProcessor(this);
    }

    /**
     * Create a Processor based on an existing Configuration. This constructor is useful for transition,
     * when new components of an application are to use s9api interfaces but existing components use lower-level
     * interfaces.
     * @param config the Configuration to be used by this processor
     * @since 9.3
     */

    public Processor(Configuration config) {
        this.config = config;
        if (config.getEditionCode().equals("EE")) {
            schemaManager = new SchemaManager(config);
        }
    }

    /**
     * Create a Processor configured according to the settings in a supplied configuration file.
     * @param source the Source of the configuration file
     * @throws SaxonApiException if the configuration file cannot be read, or its contents are invalid
     * @since 9.2
     */

    public Processor(Source source) throws SaxonApiException {
        try {
            config = Configuration.readConfiguration(source);
            schemaManager = new SchemaManager(config);
        } catch (XPathException e) {
            throw new SaxonApiException(e);
        }
    }

    /**
     * Create a DocumentBuilder. A DocumentBuilder is used to load source XML documents.
     * @return a newly created DocumentBuilder
     */

    public DocumentBuilder newDocumentBuilder() {
        return new DocumentBuilder(config);
    }

    /**
     * Create an XPathCompiler. An XPathCompiler is used to compile XPath expressions.
     * @return a newly created XPathCompiler
     */

    public XPathCompiler newXPathCompiler() {
        return new XPathCompiler(this);
    }

    /**
     * Create an XsltCompiler. An XsltCompiler is used to compile XSLT stylesheets.
     * @return a newly created XsltCompiler
     * @throws UnsupportedOperationException if this version of the Saxon product does not support XSLT processing
     */

    public XsltCompiler newXsltCompiler() {
        if (isSchemaAware() && !config.isLicensedFeature(Configuration.LicenseFeature.ENTERPRISE_XSLT)) {
            throw new UnsupportedOperationException(
                    "XSLT processing is not supported with this Saxon installation");
        }
        return new XsltCompiler(this);
    }

    /**
     * Create an XQueryCompiler. An XQueryCompiler is used to compile XQuery queries.
     * @return a newly created XQueryCompiler
     * @throws UnsupportedOperationException if this version of the Saxon product does not support XQuery processing
     */

    public XQueryCompiler newXQueryCompiler() {
        if (isSchemaAware() && !config.isLicensedFeature(Configuration.LicenseFeature.ENTERPRISE_XQUERY)) {
            throw new UnsupportedOperationException(
                    "XQuery processing is not supported with this Saxon installation");
        }
        return new XQueryCompiler(this);
    }

    /**
     * Create a Serializer
     * @return a new Serializer
     * @since 9.3
     */

    public Serializer newSerializer() {
        Serializer s = new Serializer();
        s.setProcessor(this);
        return s;
    }

    /**
     * Create a Serializer initialized to write to a given OutputStream.
     * <p>Closing the output stream after use is the responsibility of the caller.</p>
     * @param stream The OutputStream to which the Serializer will write
     * @return a new Serializer
     * @since 9.3
     */

    public Serializer newSerializer(OutputStream stream) {
        Serializer s = new Serializer();
        s.setProcessor(this);
        s.setOutputStream(stream);
        return s;
    }

    /**
     * Create a Serializer initialized to write to a given Writer.
     * <p>Closing the writer after use is the responsibility of the caller.</p>
     * @param writer The Writer to which the Serializer will write
     * @return a new Serializer
     * @since 9.3
     */

    public Serializer newSerializer(Writer writer) {
        Serializer s = new Serializer();
        s.setProcessor(this);
        s.setOutputWriter(writer);
        return s;
    }

    /**
     * Create a Serializer initialized to write to a given File.
     * @param file The File to which the Serializer will write
     * @return a new Serializer
     * @since 9.3
     */

    public Serializer newSerializer(File file) {
        Serializer s = new Serializer();
        s.setProcessor(this);
        s.setOutputFile(file);
        return s;
    }


   /**
      * Register an extension function that is to be made available within any stylesheet, query,
      * or XPath expression compiled under the control of this processor. This method
      * registers an extension function implemented as an instance of
      * {@link net.sf.saxon.lib.ExtensionFunctionCall}, using an arbitrary name and namespace.
      * This supplements the ability to call arbitrary Java methods using a namespace and local name
      * that are related to the Java class and method name.
      * @param function the class that implements the extension function. This must be a class that extends
      * {@link net.sf.saxon.lib.ExtensionFunctionCall}, and it must have a public zero-argument
      * constructor
      * @throws IllegalArgumentException if the class cannot be instantiated or does not extend
      * {@link net.sf.saxon.lib.ExtensionFunctionCall}
      * @since 9.2
      */

     public void registerExtensionFunction(ExtensionFunctionDefinition function) {
         try {
             config.registerExtensionFunction(function);
         } catch (Exception err) {
             throw new IllegalArgumentException(err);
         }
     }

    /**
     * Get the associated SchemaManager. The SchemaManager provides capabilities to load and cache
     * XML schema definitions. There is exactly one SchemaManager in a schema-aware Processor, and none
     * in a Processor that is not schema-aware. The SchemaManager is created automatically by the system.
     * @return the associated SchemaManager, or null if the Processor is not schema-aware.
     */

    public SchemaManager getSchemaManager() {
        return schemaManager;
    }

    /**
     * Test whether this processor is schema-aware
     * @return true if this this processor is licensed for schema processing, false otherwise
     */

    public boolean isSchemaAware() {
        return config.isLicensedFeature(Configuration.LicenseFeature.SCHEMA_VALIDATION);
    }

    /**
     * Get the user-visible Saxon product version, for example "9.0.0.1"
     * @return the Saxon product version, as a string
     */

    public String getSaxonProductVersion() {
        return Version.getProductVersion();
    }

    /**
     * Set the version of XML used by this Processor. If the value is set to "1.0", then
     * output documents will be serialized as XML 1.0. This option also affects
     * the characters permitted to appear in queries and stylesheets, and the characters that can appear
     * in names (for example, in path expressions).
     *
     * <p>Note that source documents specifying xml version="1.0" or "1.1" are accepted
     * regardless of this setting.</p>
     * @param version must be one of the strings "1.0" or "1.1"
     * @throws IllegalArgumentException if any string other than "1.0" or "1.1" is supplied
     */

    public void setXmlVersion(String version) {
        if (version.equals("1.0")) {
            config.setXMLVersion(Configuration.XML10);
        } else if (version.equals("1.1")) {
            config.setXMLVersion(Configuration.XML11);
        } else {
            throw new IllegalArgumentException("XmlVersion");
        }
    }

    /**
     * Get the version of XML used by this Processor. If the value is "1.0", then input documents
     * must be XML 1.0 documents, and output documents will be serialized as XML 1.0. This option also affects
     * the characters permitted to appear in queries and stylesheets, and the characters that can appear
     * in names (for example, in path expressions).
     * @return one of the strings "1.0" or "1.1"
     */

    public String getXmlVersion() {
        if (config.getXMLVersion() == Configuration.XML10) {
            return "1.0";
        } else {
            return "1.1";
        }
    }

    /**
     * Set a configuration property
     * @param name the name of the option to be set. The names of the options available are listed
     * as constants in class {@link net.sf.saxon.lib.FeatureKeys}.
     * @param value the value of the option to be set.
     * @throws IllegalArgumentException if the property name is not recognized
     */

    public void setConfigurationProperty(String name, Object value) {
        config.setConfigurationProperty(name, value);
    }

    /**
     * Get the value of a configuration property
     * @param name the name of the option required. The names of the properties available are listed
     * as constants in class {@link net.sf.saxon.lib.FeatureKeys}.
     * @return the value of the property, if one is set; or null if the property is unset and there is
     * no default.
     * @throws IllegalArgumentException if the property name is not recognized
     */
    
    public Object getConfigurationProperty(String name) {
        return config.getConfigurationProperty(name);
    }

    /**
     * Get the underlying {@link Configuration} object that underpins this Processor. This method
     * provides an escape hatch to internal Saxon implementation objects that offer a finer and lower-level
     * degree of control than the s9api classes and methods. Some of these classes and methods may change
     * from release to release.
     * @return the underlying Configuration object
     */

    public Configuration getUnderlyingConfiguration() {
        return config;
    }

    /**
     * Write an XdmValue to a given destination. The sequence represented by the XdmValue is "normalized"
     * as defined in the serialization specification (this is equivalent to constructing a document node
     * in XSLT or XQuery with this sequence as the content expression), and the resulting document is
     * then copied to the destination. If the destination is a serializer this has the effect of serializing
     * the sequence as described in the W3C specifications.
     * @param value the value to be written
     * @param destination the destination to which the value is to be written
     */

    public void writeXdmValue(XdmValue value, Destination destination) throws SaxonApiException {
        try {
            Receiver out = destination.getReceiver(config);
            out = new NamespaceReducer(out);
            TreeReceiver tree = new TreeReceiver(out);
            tree.open();
            tree.startDocument(0);
            for (Iterator<XdmItem> it = value.iterator(); it.hasNext();) {
                XdmItem item = it.next();
                tree.append((Item)item.getUnderlyingValue(), 0, NodeInfo.ALL_NAMESPACES);
            }
            tree.endDocument();
            tree.close();
            destination.close();
        } catch (XPathException err) {
            throw new SaxonApiException(err);
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
// The Original Code is: all this file
//
// The Initial Developer of the Original Code is Michael H. Kay.
//
// Contributor(s):
//

