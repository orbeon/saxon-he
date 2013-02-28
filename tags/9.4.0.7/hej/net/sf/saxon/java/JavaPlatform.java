package net.sf.saxon.java;

import net.sf.saxon.Configuration;
import net.sf.saxon.Platform;
import net.sf.saxon.dom.DOMEnvelope;
import net.sf.saxon.dom.DOMObjectModel;
import net.sf.saxon.event.PipelineConfiguration;
import net.sf.saxon.expr.sort.CodepointCollator;
import net.sf.saxon.expr.sort.SimpleCollation;
import net.sf.saxon.functions.FunctionLibraryList;
import net.sf.saxon.lib.StringCollator;
import net.sf.saxon.trans.XPathException;
import net.sf.saxon.type.SchemaType;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParserFactory;
import javax.xml.transform.Source;
import javax.xml.transform.TransformerFactoryConfigurationError;
import javax.xml.transform.stream.StreamSource;
import java.text.Collator;
import java.util.Properties;

/**
 * Implementation of the Platform class containing methods specific to the Java platform
 * (as distinct from .NET). This is a singleton class with no instance data.
 */
public class JavaPlatform implements Platform {

    /**
     * The constructor is called during the static initialization of the Configuration, by virtue of
     * the class name appearing in the edition.properties file within the issued JAR file
     */

    public JavaPlatform() {
    }

    /**
     * Perform platform-specific initialization of the configuration
     */

    public void initialize(Configuration config) {
        config.registerExternalObjectModel(DOMEnvelope.getInstance());
        config.registerExternalObjectModel(DOMObjectModel.getInstance());
    }

    /**
     * Return true if this is the Java platform
     */

    public boolean isJava() {
        return true;
    }

    /**
     * Return true if this is the .NET platform
     */

    public boolean isDotNet() {
        return false;
    }

    /**
     * Get the verifier appropriate to the platform
     */

    public String getVerifierClassName() {
        return "com.saxonica.config.Verifier";
    }

    /**
     * Get the platform version
     */

    public String getPlatformVersion() {
        return "Java version " + System.getProperty("java.version");
    }

    /**
     * Get a suffix letter to add to the Saxon version number to identify the platform
     */

    public String getPlatformSuffix() {
        return "J";
    }

    /**
     * Get a parser by instantiating the SAXParserFactory
     *
     * @return the parser (XMLReader)
     */

    public XMLReader loadParser() {
        XMLReader parser;
        try {
            parser = SAXParserFactory.newInstance().newSAXParser().getXMLReader();
        } catch (ParserConfigurationException err) {
            throw new TransformerFactoryConfigurationError(err);
        } catch (SAXException err) {
            throw new TransformerFactoryConfigurationError(err);
        }
        return parser;
    }

    /**
     * Convert a StreamSource to either a SAXSource or a PullSource, depending on the native
     * parser of the selected platform
     *
     * @param pipe          the pipeline configuration
     * @param input         the supplied StreamSource
     * @param validation    indicates whether schema validation is required
     * @param dtdValidation indicates whether DTD validation is required
     * @param stripspace    indicates whether whitespace text nodes should be stripped
     * @return the PullSource or SAXSource, initialized with a suitable parser, or the original
     *         input Source, if now special handling is required or possible. This implementation
     *         always returns the original input unchanged.
     */

    public Source getParserSource(PipelineConfiguration pipe, StreamSource input, int validation,
                                  boolean dtdValidation, int stripspace) {
        return input;
    }

    /**
     * Obtain a collation with a given set of properties. The set of properties is extensible
     * and variable across platforms. Common properties with example values include lang=ed-GB,
     * strength=primary, case-order=upper-first, ignore-modifiers=yes, alphanumeric=yes.
     * Properties that are not supported are generally ignored; however some errors, such as
     * failing to load a requested class, are fatal.
     *
     * @param config the configuration object
     * @param props  the desired properties of the collation
     * @param uri    the collation URI
     * @return a collation with these properties
     * @throws XPathException if a fatal error occurs
     */

    /*@Nullable*/
    public StringCollator makeCollation(Configuration config, Properties props, String uri) throws XPathException {
        return JavaCollationFactory.makeCollation(config, uri, props);
    }

    /**
     * Given a collation, determine whether it is capable of returning collation keys.
     * The essential property of collation keys
     * is that if two values are equal under the collation, then the collation keys are
     * equal under the equals() method.
     *
     * @param collation the collation, provided as a Comparator
     * @return true if this collation can supply collation keys
     */

    public boolean canReturnCollationKeys(StringCollator collation) {
        return (collation instanceof CodepointCollator) ||
                ((collation instanceof SimpleCollation) &&
                        (((SimpleCollation) collation).getCollation() instanceof Collator));
    }

    /**
     * Given a collation, get a collation key. The essential property of collation keys
     * is that if two values are equal under the collation, then the collation keys are
     * compare correctly under the equals() method.
     *
     * @throws ClassCastException if the collation is not one that is capable of supplying
     *                            collation keys (this should have been checked in advance)
     */

    public Object getCollationKey(SimpleCollation namedCollation, String value) {
        return ((Collator) namedCollation.getCollation()).getCollationKey(value);
    }

    /**
     * Add the platform-specific function libraries to a function library list. This version
     * of the method does nothing
     *
     * @param list         the function library list that is to be extended
     * @param config       the Configuration
     * @param hostLanguage the host language, for example Configuration.XQUERY
     */

    public void addFunctionLibraries(FunctionLibraryList list, Configuration config, int hostLanguage) {
        // do nothing
    }

    public SchemaType getExternalObjectType(Configuration config, String uri, String localName) {
        throw new UnsupportedOperationException("getExternalObjectType for Java");
    }

    /**
     * Return the name of the directory in which the software is installed (if available)
     *
     * @param edition
     * @return the name of the directory in which Saxon is installed, if available, or null otherwise
     */

    public String getInstallationDirectory(String edition, Configuration config) {
        return System.getenv("SAXON_HOME");
    }

    /**
     * Register all the external object models that are provided as standard
     * with the relevant edition of Saxon for this Configuration
     *
     * @since 9.3
     */

    public void registerAllBuiltInObjectModels(Configuration config) {
        // No action for Saxon-HE
    }

    /**
     * Set the default XML parser to be loaded by the SAXParserFactory on this platform.
     * Needed because the Apache catalog resolver uses the SAXParserFactory to instantiate
     * a parser, and if not customized this causes a failure on the .NET platform.
     *
     * @since 9.4
     */

    public void setDefaultSAXParserFactory() {
        // No action for Saxon on Java
    }

    /**
     * Return the class loader required to load the bytecode generated classes
     *
     * @param definedClassName The generated class name
     * @param classFile        The bytecode of the generated class
     * @param config           The cThe saxon configuration
     * @param thisClass        The class object generated
     * @return the class loader object
     * @since 9.4
     */

    public ClassLoader getClassLoaderForGeneratedClass(final String definedClassName, final byte[] classFile, Configuration config, Class thisClass) {
        ClassLoader parentClassLoader = config.getDynamicLoader().getClassLoader();
        if (parentClassLoader == null) {
            parentClassLoader = Thread.currentThread().getContextClassLoader();
        }
        if (parentClassLoader == null) {
            parentClassLoader = thisClass.getClassLoader();
        }
        return new ClassLoader(parentClassLoader) {
            @Override
            protected Class<?> findClass(String name) throws ClassNotFoundException {
                if (name.equals(definedClassName)) {
                    return defineClass(name, classFile, 0, classFile.length);
                } else {
                    return super.findClass(name);
                }
            }
        };
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
// The Initial Developer of the Original Code is Saxonica Limited.
// Portions created by ___ are Copyright (C) ___. All rights reserved.
//
// Contributor(s):
//