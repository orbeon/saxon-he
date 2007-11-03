package net.sf.saxon.s9api;

import net.sf.saxon.Configuration;
import net.sf.saxon.Version;

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

    // TODO: collection URI resolver, collation URI resolver
    // TODO: extension functions
    // TODO: external object models

    private Configuration config;
    private SchemaManager schemaManager;

    /**
     * Create a Processor
     * @param schemaAware indicates whether the Processor is schema-aware or not. To create a schema-aware
     * processor, the Saxon-SA product is required.
     */

    public Processor(boolean schemaAware) {
        if (schemaAware) {
            config = Configuration.makeSchemaAwareConfiguration(null, null);
            schemaManager = new SchemaManager(config);
        } else {
            config = new Configuration();
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
        return new XPathCompiler(config);
    }

    /**
     * Create an XsltCompiler. An XsltCompiler is used to compile XSLT stylesheets.
     * @return a newly created XsltCompiler
     * @throws UnsupportedOperationException if this version of the Saxon product does not support XSLT processing
     */

    public XsltCompiler newXsltCompiler() {
        if (isSchemaAware() && !config.isSchemaAware(Configuration.XSLT)) {
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
        if (isSchemaAware() && !config.isSchemaAware(Configuration.XQUERY)) {
            throw new UnsupportedOperationException(
                    "XQuery processing is not supported with this Saxon installation");
        }
        return new XQueryCompiler(this);
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
     * @return true if this is a schema-aware processor, false otherwise
     */

    public boolean isSchemaAware() {
        return config.isSchemaAware(Configuration.XML_SCHEMA);
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
     * Set a configuration option
     * @param name the name of the option to be set. The names of the options available are listed
     * as constants in class {@link net.sf.saxon.FeatureKeys}
     * @param value the value of the option to be set. The type of the value depends on the option
     * that is being set
     * @throws IllegalArgumentException if the property name is not recognized
     */

    // TODO: make this a typesafe method
    public void setConfigurationProperty(String name, Object value) {
        config.setConfigurationProperty(name, value);
    }

    /**
     * Get a configuration option
     * @param name the name of the option required. The names of the options available are listed
     * as constants in class {@link net.sf.saxon.FeatureKeys}
     * @return the value of the option to be set. The type of the value depends on the option
     * that is being set
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

