package net.sf.saxon;

import net.sf.saxon.event.PipelineConfiguration;
import net.sf.saxon.expr.sort.SimpleCollation;
import net.sf.saxon.lib.StringCollator;
import net.sf.saxon.trans.XPathException;
import net.sf.saxon.type.SchemaType;
import org.xml.sax.XMLReader;

import javax.xml.transform.Source;
import javax.xml.transform.stream.StreamSource;
import java.io.Serializable;
import java.util.Properties;

/**
 * This interface provides access to methods whose implementation depends on the chosen platform
 * (typically Java or .NET)
 */
public interface Platform extends Serializable {

    /**
     * Perform platform-specific initialization of the configuration
     * @param config the Saxon Configuration
     */

    public void initialize(Configuration config);

    /**
     * Return true if this is the Java platform
     * @return true if this is the Java platform
     */

    public boolean isJava();

    /**
     * Return true if this is the .NET platform
     * @return true if this is the .NET platform
     */

    public boolean isDotNet();

    /**
     * Get the verifier class name appropriate to the platform
     */

    public String getVerifierClassName();

    /**
     * Get the platform version
     * @return the version of the platform, for example "Java version 1.5.09"
     */

    public String getPlatformVersion();

    /**
     * Get a suffix letter to add to the Saxon version number to identify the platform
     * @return "J" for Java, "N" for .NET
     */

    public String getPlatformSuffix();

    /**
     * Get a parser by instantiating the SAXParserFactory
     *
     * @return the parser (XMLReader)
     */

    public XMLReader loadParser();

    /**
     * Convert a StreamSource to either a SAXSource or a PullSource, depending on the native
     * parser of the selected platform
     * @param pipe the pipeline Configuration
     * @param input the supplied StreamSource
     * @param validation required validation mode, for example Validation.STRICT
     * @param dtdValidation true if DTD-based input validation is required
     * @param stripspace option for whitespace-stripping  (ALL, NONE, or IGNORABLE)
     * @return the PullSource or SAXSource, initialized with a suitable parser, or the original
     * input Source, if now special handling is required or possible
     */

    public Source getParserSource(PipelineConfiguration pipe, StreamSource input,
                                  int validation, boolean dtdValidation, int stripspace);

    /**
     * Obtain a collation with a given set of properties. The set of properties is extensible
     * and variable across platforms. Common properties with example values include lang=en-GB,
     * strength=primary, case-order=upper-first, ignore-modifiers=yes, alphanumeric=yes.
     * Properties that are not supported are generally ignored; however some errors, such as
     * failing to load a requested class, are fatal.
     * @param config the configuration object
     * @param props the desired properties of the collation
     * @param uri the collation URI
     * @return a collation with these properties
     * @throws XPathException if a fatal error occurs
     */

    /*@Nullable*/ public StringCollator makeCollation(Configuration config, Properties props, String uri) throws XPathException;

    /**
     * Given a collation, determine whether it is capable of returning collation keys.
     * The essential property of collation keys
     * is that if two values are equal under the collation, then the collation keys are
     * equal under the equals() method.
     * @param collation the collation being examined, provided as a Comparator
     * @return true if this collation can supply collation keys
     */

    public boolean canReturnCollationKeys(StringCollator collation);

    /**
     * Given a collation, get a collation key. The essential property of collation keys
     * is that if two values are equal under the collation, then the collation keys are
     * equal under the equals() method.
     * @param namedCollation the collation in use
     * @param value the string whose collation key is required
     * @return a representation of the collation key, such that two collation keys are
     * equal() if and only if the string values they represent are equal under the specified collation.
     * @throws ClassCastException if the collation is not one that is capable of supplying
     * collation keys (this should have been checked in advance)
     */

    public Object getCollationKey(SimpleCollation namedCollation, String value);

    /**
     * Get a SchemaType representing a wrapped external (Java or .NET) object
     * @param config the Saxon Configuration
     * @param uri the namespace URI of the schema type
     * @param localName the local name of the schema type
     * @return the SchemaType object representing this type
     */

    public SchemaType getExternalObjectType(Configuration config, String uri, String localName);

    /**
     * Return the name of the directory in which the software is installed (if available)
     * @return the name of the directory in which Saxon is installed, if available, or null otherwise
     * @param edition The edition of the software that is loaded ("HE", "PE", or "EE")
     * @param config the Saxon configuration
     */

    public String getInstallationDirectory(String edition, Configuration config);

    /**
     * Register all the external object models that are provided as standard
     * with the relevant edition of Saxon for this Configuration
     *
     * @since 9.3
     */

    public void registerAllBuiltInObjectModels(Configuration config);

    /**
     * Set the default XML parser to be loaded by the SAXParserFactory on this platform.
     * Needed because the Apache catalog resolver uses the SAXParserFactory to instantiate
     * a parser, and if not customized this causes a failure on the .NET platform.
     *
     * @since 9.4
     */

    public void setDefaultSAXParserFactory();

    /**
     * Return the class loader required to load the bytecode generated classes
     * @return the class loader object
     * @param definedClassName The generated class name
     * @param classFile The bytecode of the generated class
     * @param config The cThe saxon configuration
     * @param thisClass The class object generated
     *
     * @since 9.4
     *
     * */

     public ClassLoader getClassLoaderForGeneratedClass(final String definedClassName, final byte[] classFile, Configuration config, Class thisClass);
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