package net.sf.saxon;

import net.sf.saxon.functions.FunctionLibraryList;
import net.sf.saxon.functions.FunctionLibrary;
import net.sf.saxon.regex.RegularExpression;
import net.sf.saxon.sort.StringCollator;
import net.sf.saxon.sort.NamedCollation;
import net.sf.saxon.trans.XPathException;
import net.sf.saxon.type.SchemaType;

import javax.xml.transform.Source;
import javax.xml.transform.stream.StreamSource;
import java.io.Serializable;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Properties;

/**
 * This interface provides access to methods whose implementation depends on the chosen platform
 * (typically Java or .NET)
 */
public interface Platform extends Serializable {

    /**
     * Perform platform-specific initialization of the configuration
     */

    public void initialize(Configuration config);

    /**
     * Return true if this is the Java platform
     */

    public boolean isJava();

    /**
     * Return true if this is the .NET platform
     */

    public boolean isDotNet();

    /**
     * Construct an absolute URI from a relative URI and a base URI
     * @param relativeURI the relative URI. Null is permitted provided that the base URI is an absolute URI
     * @param base the base URI
     * @return the absolutized URI
     * @throws URISyntaxException
     */

    public URI makeAbsolute(String relativeURI, String base) throws URISyntaxException;

    /**
     * Get the platform version
     */

    public String getPlatformVersion();

    /**
     * Get a suffix letter to add to the Saxon version number to identify the platform
     */

    public String getPlatformSuffix();

    /**
     * Convert a StreamSource to either a SAXSource or a PullSource, depending on the native
     * parser of the selected platform
     * @param config
     * @param input the supplied StreamSource
     * @param validation
     * @param dtdValidation
     * @param stripspace
     * @return the PullSource or SAXSource, initialized with a suitable parser, or the original
     * input Source, if now special handling is required or possible
     */

    public Source getParserSource(Configuration config, StreamSource input,
                                  int validation, boolean dtdValidation, int stripspace);

    /**
     * Create a compiled regular expression
     * @param regex the source text of the regular expression, in XML Schema or XPath syntax
     * @param isXPath set to true if this is an XPath regular expression, false if it is XML Schema
     * @param flags the flags argument as supplied to functions such as fn:matches(), in string form
     * @throws XPathException if the syntax of the regular expression or flags is incorrect
     * @return the compiled regular expression
     */

    public RegularExpression compileRegularExpression(CharSequence regex, boolean isXPath, CharSequence flags)
    throws XPathException;

    /**
     * Obtain a collation with a given set of properties. The set of properties is extensible
     * and variable across platforms. Common properties with example values include lang=ed-GB,
     * strength=primary, case-order=upper-first, ignore-modifiers=yes, alphanumeric=yes.
     * Properties that are not supported are generally ignored; however some errors, such as
     * failing to load a requested class, are fatal.
     * @param config the configuration object
     * @param props the desired properties of the collation
     * @param uri
     * @return a collation with these properties
     * @throws XPathException if a fatal error occurs
     */

    public StringCollator makeCollation(Configuration config, Properties props, String uri) throws XPathException;

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
     * @return a representation of the collation key, such that two collation keys are
     * equal() if and only if the string values they represent are equal under the specified collation.
     * @throws ClassCastException if the collation is not one that is capable of supplying
     * collation keys (this should have been checked in advance)
     */

    public Object getCollationKey(NamedCollation namedCollation, String value);

    /**
     * Make the default extension function factory appropriate to the platform
     */

    public FunctionLibrary makeExtensionLibrary(Configuration config);

    /**
     * Add platform-specific function libraries to the function library list
     */

    public void addFunctionLibraries(FunctionLibraryList list, Configuration config);

    /**
     * Register a namespace-to-Java-class mapping declared using saxon:script in an XSLT stylesheet
     * @param library the library to contain the function, which must be a JavaExtensionLibrary
     * @param uri the namespace of the function name
     * @param theClass the Java class that implements this namespace
     */

    public void declareJavaClass(FunctionLibrary library, String uri, Class theClass);

    public SchemaType getExternalObjectType(Configuration config, String uri, String localName);

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

