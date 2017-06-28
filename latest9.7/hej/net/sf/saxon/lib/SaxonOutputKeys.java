////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
// Copyright (c) 2015 Saxonica Limited.
// This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
// If a copy of the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
// This Source Code Form is "Incompatible With Secondary Licenses", as defined by the Mozilla Public License, v. 2.0.
////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

package net.sf.saxon.lib;

import net.sf.saxon.Configuration;
import net.sf.saxon.om.NameChecker;
import net.sf.saxon.om.NamespaceResolver;
import net.sf.saxon.om.QNameException;
import net.sf.saxon.trans.Err;
import net.sf.saxon.trans.XPathException;
import net.sf.saxon.type.ValidationException;
import net.sf.saxon.value.DecimalValue;

import javax.xml.transform.OutputKeys;
import java.math.BigDecimal;
import java.util.Properties;
import java.util.StringTokenizer;
import java.util.regex.Pattern;

/**
 * Provides string constants that can be used to set
 * output properties for a Transformer, or to retrieve
 * output properties from a Transformer or Templates object.
 * <p/>
 * These keys are private Saxon keys that supplement the standard keys
 * defined in javax.xml.transform.OutputKeys. As well as Saxon extension
 * attributes, the list includes new attributes defined in XSLT 2.0 which
 * are not yet supported in JAXP
 */

public class SaxonOutputKeys {

    /**
     * This class is not instantiated
     */

    private SaxonOutputKeys() {
    }

    /**
     * String constant representing the saxon:xquery output method name
     */

    /*@NotNull*/ public static final String SAXON_XQUERY_METHOD = "{http://saxon.sf.net/}xquery";

    /**
     * String constant representing the saxon:base64Binary output method name
     */

    /*@NotNull*/ public static final String SAXON_BASE64_BINARY_METHOD = "{http://saxon.sf.net/}base64Binary";

    /**
     * String constant representing the saxon:hexBinary output method name
     */

    /*@NotNull*/ public static final String SAXON_HEX_BINARY_METHOD = "{http://saxon.sf.net/}hexBinary";

    /**
     * String constant representing the saxon:ptree output method name
     */

    /*@NotNull*/ public static final String SAXON_PTREE_METHOD = "{http://saxon.sf.net/}ptree";

    /**
     * allow-duplicate-names = yes|no.
     * <p/>
     * <p>Defines whether duplicate keys are allowed in a JSON map (new in 3.1)</p>
     */

    /*@NotNull*/ public static final String ALLOW_DUPLICATE_NAMES = "allow-duplicate-names";

    /**
     * build-tree = yes|no.
     * <p/>
     * <p>Defines whether the raw output is used to build an XML document tree</p>
     */

    /*@NotNull*/ public static final String BUILD_TREE = "build-tree";

    /**
     * saxon:indent-spaces = integer.
     * <p/>
     * <p>Defines the number of spaces used for indentation of output</p>
     */

    /*@NotNull*/ public static final String INDENT_SPACES = "{http://saxon.sf.net/}indent-spaces";

    /**
     * saxon:line-length = integer.
     * <p/>
     * <p>Defines the desired maximum line length used when indenting output</p>
     */

    /*@NotNull*/ public static final String LINE_LENGTH = "{http://saxon.sf.net/}line-length";

    /**
     * suppress-indentation = list of element names
     * <p/>
     * <p>Defines elements within which no indentation will occur</p>
     */

    /*@NotNull*/ public static final String SUPPRESS_INDENTATION = "suppress-indentation";

    /**
     * html-version = decimal
     * <p>Defines the version of HTML. For the XHTML output method this allows separate
     * specification of the XHTML version and the XML version. This is a new serialization
     * parameter in the draft 3.0 specification.</p>
     */

    public static final String HTML_VERSION = "html-version";

    /**
     * item-separator = string
     * <p>Relevant to XQuery, where an arbitrary sequence can be serialized; defines a separator
     * to be inserted between successive items in the sequence.</p>
     */

    public static final String ITEM_SEPARATOR = "item-separator";

    /**
     * json-node-output-method = method-name
     * <p/>
     * <p>Defines the serialization method for nodes encountered while serializing as JSON</p>
     */

    /*@NotNull*/ public static final String JSON_NODE_OUTPUT_METHOD = "json-node-output-method";


    /**
     * saxon:attribute-order = list of attribute names
     * <p/>
     * <p>Defines an ordering for attributes in the serialized output. Any attribute present in the list
     * will appear correctly ordered according to the list; other attributes will be ordered first by namespace,
     * then by local name.</p>
     */

    /*@NotNull*/ public static final String ATTRIBUTE_ORDER = "{http://saxon.sf.net/}attribute-order";

    /**
     * saxon:double-space = list of element names
     * <p/>
     * <p>Defines elements that will have an extra blank line added before the start tag, in addition
     * to normal indentation</p>
     */

    /*@NotNull*/ public static final String DOUBLE_SPACE = "{http://saxon.sf.net/}double-space";

    /**
     * saxon:newline = string
     * <p/>
     * <p>Defines the sequence of characters used to represent a newline when using the text
     * output method</p>
     */

    /*@NotNull*/ public static final String NEWLINE = "{http://saxon.sf.net/}newline";

    /**
     * stylesheet-version. This serialization parameter is set automatically by the XSLT processor
     * to the value of the version attribute on the principal stylesheet module.
     */

    /*@NotNull*/ public static final String STYLESHEET_VERSION = "{http://saxon.sf.net/}stylesheet-version";

    /**
     * use-character-map = list-of-qnames.
     * <p/>
     * <p>Defines the character maps used in this output definition. The QNames
     * are represented in Clark notation as {uri}local-name.</p>
     */

    /*@NotNull*/ public static final String USE_CHARACTER_MAPS = "use-character-maps";


    /**
     * include-content-type = "yes" | "no". This attribute is defined in XSLT 2.0
     * <p/>
     * <p>Indicates whether the META tag is to be added to HTML output</p>
     */

    /*@NotNull*/ public static final String INCLUDE_CONTENT_TYPE = "include-content-type";

    /**
     * undeclare-prefixes = "yes" | "no". This attribute is defined in XSLT 2.0
     * <p/>
     * <p>Indicates XML 1.1 namespace undeclarations are to be output when required</p>
     */

    /*@NotNull*/ public static final String UNDECLARE_PREFIXES = "undeclare-prefixes";

    /**
     * escape-uri-attributes = "yes" | "no". This attribute is defined in XSLT 2.0
     * <p/>
     * <p>Indicates whether HTML attributes of type URI are to be URI-escaped</p>
     */

    /*@NotNull*/ public static final String ESCAPE_URI_ATTRIBUTES = "escape-uri-attributes";

    /**
     * representation = rep1[;rep2].
     * <p/>
     * <p>Indicates the preferred way of representing non-ASCII characters in HTML
     * and XML output. rep1 is for characters in the range 128-256, rep2 for those
     * above 256.</p>
     */
    /*@NotNull*/ public static final String CHARACTER_REPRESENTATION = "{http://saxon.sf.net/}character-representation";

    /**
     * saxon:next-in-chain = URI.
     * <p/>
     * <p>Indicates that the output is to be piped into another XSLT stylesheet
     * to perform another transformation. The auxiliary property NEXT_IN_CHAIN_BASE_URI
     * records the base URI of the stylesheet element where this attribute was found.</p>
     */
    /*@NotNull*/ public static final String NEXT_IN_CHAIN = "{http://saxon.sf.net/}next-in-chain";
    /*@NotNull*/ public static final String NEXT_IN_CHAIN_BASE_URI = "{http://saxon.sf.net/}next-in-chain-base-uri";

    /**
     * byte-order-mark = yes|no.
     * <p/>
     * <p>Indicates whether UTF-8/UTF-16 output is to start with a byte order mark. Values are "yes" or "no",
     * default is "no"
     */

    /*@NotNull*/ public static final String BYTE_ORDER_MARK = "byte-order-mark";

    /**
     * normalization-form = NFC|NFD|NFKC|NFKD|non.
     * <p/>
     * <p>Indicates that a given Unicode normalization form (or no normalization) is required.
     */

    /*@NotNull*/ public static final String NORMALIZATION_FORM = "normalization-form";

    /**
     * recognize-binary = yes|no.
     * <p/>
     * <p>If set to "yes", and the output is being written using output method "text", Saxon will recognize
     * two processing instructions &lt;?hex XXXX?&gt; and &lt;b64 XXXX?&gt; containing binary data encoded
     * as a hexBinary or base64 string respectively. The corresponding strings will be decoded as characters
     * in the encoding being used for the output file, and will be written out to the output without checking
     * that they represent valid XML strings.</p>
     */

    /*@NotNull*/ public static final String RECOGNIZE_BINARY = "{http://saxon.sf.net/}recognize-binary";

    /**
     * saxon:require-well-formed = yes|no.
     * <p/>
     * <p>Indicates whether a user-supplied ContentHandler requires the stream of SAX events to be
     * well-formed (that is, to have a single element node and no text nodes as children of the root).
     * The default is "no".</p>
     */

    /*@NotNull*/ public static final String REQUIRE_WELL_FORMED = "{http://saxon.sf.net/}require-well-formed";

    /**
     * supply-source-locator = yes|no.
     * <p/>
     * <p>If set to "yes", and the output is being sent to a SAXResult (or to a user-supplied content handler),
     * indicates that the SAX Locator made available to the ContentHandler will contain information about the
     * location of the context node in the source document as well as the location in the stylesheet or query.</p>
     */

    /*@NotNull*/ public static final String SUPPLY_SOURCE_LOCATOR = "{http://saxon.sf.net/}supply-source-locator";

    /**
     * wrap="yes"|"no".
     * <p/>
     * This property is only available in the XQuery API. The value "yes" indicates that the result
     * sequence produced by the query is to be wrapped, that is, each item in the result is represented
     * as a separate element. This format allows any sequence to be represented as an XML document,
     * including for example sequences consisting of parentless attribute nodes.
     */

    /*@NotNull*/ public static final String WRAP = "{http://saxon.sf.net/}wrap-result-sequence";

    /**
     * Property used internally to identify the XSLT implicit result document
     */

    /*@NotNull*/ public static final String IMPLICIT_RESULT_DOCUMENT = "{http://saxon.sf.net/}implicit-result-document";

    /**
     * Property saxon:unfailing used to indicate that serialization should not fail. This is used when a serialization method such
     * as XML or JSON is invoked from the ADAPTIVE serialization method, and it tailors the error-handling behaviour
     * of the subsidiary output method. There is nothing to stop the property being set directly by the user, though
     * (at least in the XSLT case) the resulting behaviour is non-conformant. The values of the property are yes/no.
     */

    public static final String UNFAILING = "{http://saxon.sf.net/}unfailing";


    /**
     * Check that a supplied output property is valid, and normalize the value (specifically in the case of boolean
     * values where yes|true|1 are normalized to "yes", and no|false|0 are normalized to "no").
     *
     *
     * @param key    the name of the property, in Clark format
     * @param value  the value of the property. This may be set to null, in which case no validation takes place.
     *               The value must be in JAXP format, that is, with lexical QNames expanded to Clark names
     * @param config the Saxon Configuration. May be null, in which case validation may be incomplete
     * @param allow30 true if serializer xslt 3.0 rules are followed
     * @return     normalized value of the property, or null if the supplied value is null
     *
     * @throws XPathException if the property name or value is invalid
     */

    public static String checkOutputProperty(/*@NotNull*/ String key, /*@Nullable*/ String value, /*@Nullable*/ Configuration config, boolean allow30) throws XPathException {
        if (!key.startsWith("{") || key.startsWith("{http://saxon.sf.net/}")) {
            if (key.equals(ALLOW_DUPLICATE_NAMES)) {
                if (value != null) {
                    value = checkYesOrNo(key, value, allow30);
                }
            } else if (key.equals(BUILD_TREE)) {
                if (value != null) {
                    value = checkYesOrNo(key, value, allow30);
                }
            } else if (key.equals(BYTE_ORDER_MARK)) {
                if (value != null) {
                    value = checkYesOrNo(key, value, allow30);
                }
            } else if (key.equals(OutputKeys.CDATA_SECTION_ELEMENTS)) {
                if (value != null) {
                    checkListOfClarkNames(key, value);
                }
            } else if (key.equals(OutputKeys.DOCTYPE_PUBLIC)) {
                if (value != null) {
                    checkPublicIdentifier(value);
                }
            } else if (key.equals(OutputKeys.DOCTYPE_SYSTEM)) {
                if (value != null) {
                    checkSystemIdentifier(value);
                }
            } else if (key.equals(OutputKeys.ENCODING)) {
                // no constraints
            } else if (key.equals(ESCAPE_URI_ATTRIBUTES)) {
                if (value != null) {
                    value = checkYesOrNo(key, value, allow30);
                }
            } else if (key.equals(INCLUDE_CONTENT_TYPE)) {
                if (value != null) {
                    value = checkYesOrNo(key, value, allow30);
                }
            } else if (key.equals(OutputKeys.INDENT)) {
                if (value != null) {
                    value = checkYesOrNo(key, value, allow30);
                }
            } else if (key.equals(OutputKeys.MEDIA_TYPE)) {
                // no constraints
            } else if (key.equals(OutputKeys.METHOD) || key.equals(JSON_NODE_OUTPUT_METHOD)) {
                if (value != null) {
                    checkMethod(key, value, allow30, config);
                }
            } else if (key.equals(NEWLINE)) {
                // no constraints
            } else if (key.equals(NORMALIZATION_FORM)) {
                if (value != null) {
                    checkNormalizationForm(value);
                }
            } else if (key.equals(OutputKeys.OMIT_XML_DECLARATION)) {
                if (value != null) {
                    value = checkYesOrNo(key, value, allow30);
                }
            } else if (key.equals(OutputKeys.STANDALONE)) {
                if (value != null && !value.equals("omit")) {
                    value = checkYesOrNo(key, value, allow30);
                }
            } else if (key.equals(UNDECLARE_PREFIXES)) {
                if (value != null) {
                    value = checkYesOrNo(key, value, allow30);
                }
            } else if (key.equals(USE_CHARACTER_MAPS)) {
                if (value != null) {
                    checkListOfClarkNames(key, value);
                }
            } else if (key.equals(OutputKeys.VERSION)) {
                // no constraints
            } else if (key.equals(STYLESHEET_VERSION)) {
                // no constraints
            } else if (key.equals(INDENT_SPACES)) {
                if (value != null) {
                    checkExtensions(key, config);
                    checkNonNegativeInteger(key, value);
                }
            } else if (key.equals(LINE_LENGTH)) {
                if (value != null) {
                    checkExtensions(key, config);
                    checkNonNegativeInteger(key, value);
                }
            } else if (key.equals(CHARACTER_REPRESENTATION)) {
                checkExtensions(key, config);
            } else if (key.equals(NEXT_IN_CHAIN)) {
                checkExtensions(key, config);
            } else if (key.equals(NEXT_IN_CHAIN_BASE_URI)) {
                // no validation performed
            } else if (key.equals(REQUIRE_WELL_FORMED)) {
                if (value != null) {
                    value = checkYesOrNo(key, value, allow30);
                }
            } else if (key.equals(RECOGNIZE_BINARY)) {
                if (value != null) {
                    checkExtensions(key, config);
                    value = checkYesOrNo(key, value, allow30);
                }
            } else if (key.equals(SUPPRESS_INDENTATION)) {
                if (value != null) {
                    checkExtensions(key, config);
                    checkListOfClarkNames(key, value);
                }

            } else if (key.equals(DOUBLE_SPACE)) {
                if (value != null) {
                    checkExtensions(key, config);
                    checkListOfClarkNames(key, value);
                }

            } else if (key.equals(WRAP)) {
                if (value != null) {
                    checkExtensions(key, config);
                    value = checkYesOrNo(key, value, allow30);
                }
            } else if (key.equals(SUPPLY_SOURCE_LOCATOR)) {
                if (value != null) {
                    value = checkYesOrNo(key, value, allow30);
                }
            } else if (key.equals(HTML_VERSION)) {
                if (value != null) {
                    checkDecimal(key, value);
                }
            } else if (key.equals(UNFAILING)) {
                if (value != null) {
                    value = checkYesOrNo(key, value, allow30);
                }
            } else if (key.equals(ITEM_SEPARATOR)) {
                // no checking needed
            } else {
                throw new XPathException("Unknown serialization parameter " + Err.wrap(key), "XQST0109");
            }
        } else {
            //return;
        }
        return value;
    }

    private static void checkExtensions(String key, /*@Nullable*/ Configuration config) throws XPathException {
        if (config != null) {
            config.checkLicensedFeature(Configuration.LicenseFeature.PROFESSIONAL_EDITION, "custom serialization " + key, -1);
        }
    }

    private static String checkYesOrNo(String key, String value, boolean allow30) throws XPathException {
        if (allow30){
            if ("yes".equals(value)||"true".equals(value)||"1".equals(value)){
                return "yes";
            } else if ("no".equals(value)||"false".equals(value)||"0".equals(value)){
                return "no";
            }
            else {
                throw new XPathException("Serialization parameter " + Err.wrap(key) + " must have the value yes|no, true|false, or 1|0", "SEPM0016");
            }
        } else if ("yes".equals(value) || "no".equals(value)) {
            return value;
        } else {
            throw new XPathException("Serialization parameter " + Err.wrap(key) + " must have the value yes or no", "SEPM0016");
        }
    }

    private static void checkMethod(String key, String value, boolean allow30, Configuration config) throws XPathException {
        if (!"xml".equals(value) && !"html".equals(value) && !"xhtml".equals(value) && !"text".equals(value)) {
            if (allow30 && !JSON_NODE_OUTPUT_METHOD.equals(key) && ("json".equals(value) || "adaptive".equals(value))) {
                return;
            }
            if (isValidClarkName(value)) {
                checkExtensions(value, config);
            } else {
                throw new XPathException("Invalid value for serialization method: " +
                        "must be xml, html, xhtml, text, or a QName in '{uri}local' form", "SEPM0016");
            }
        }

    }

    private static void checkNormalizationForm(String value) throws XPathException {
        if (!NameChecker.isValidNmtoken(value)) {
            throw new XPathException("Invalid value for normalization-form: " +
                    "must be NFC, NFD, NFKC, NFKD, fully-normalized, or none", "SEPM0016");
        }
//        if ("NFC".equals(value)) return;
//        if ("NFD".equals(value)) return;
//        if ("NFKC".equals(value)) return;
//        if ("NFKD".equals(value)) return;
//        if ("fully-normalized".equals(value)) return;
//        if ("none".equals(value)) return;
//        throw new XPathException("Invalid value for normalization-form: " +
//                "must be NFC, NFD, NFKC, NFKD, fully-normalized, or none");

    }

    private static boolean isValidClarkName(/*@NotNull*/ String value) {
        if (value.isEmpty() || value.charAt(0) != '{') {
            return false;
        }
        int closer = value.indexOf('}');
        return closer >= 1 &&
                closer != value.length() - 1 &&
                NameChecker.isValidNCName(value.substring(closer + 1));
    }

    private static void checkNonNegativeInteger(String key, String value) throws XPathException {
        try {
            int n = Integer.parseInt(value);
            if (n < 0) {
                throw new XPathException("Value of " + Err.wrap(key) + " must be a non-negative integer", "SEPM0016");
            }
        } catch (NumberFormatException err) {
            throw new XPathException("Value of " + Err.wrap(key) + " must be a non-negative integer", "SEPM0016");
        }
    }

    private static void checkDecimal(String key, String value) throws XPathException {
        if (!DecimalValue.castableAsDecimal(value)) {
            throw new XPathException("Value of " + Err.wrap(key) +
                    " must be a decimal number", "SEPM0016");
        }
    }

    private static void checkListOfClarkNames(String key, String value) throws XPathException {
        StringTokenizer tok = new StringTokenizer(value, " \t\n\r", false);
        while (tok.hasMoreTokens()) {
            String s = tok.nextToken();
            if (isValidClarkName(s) || NameChecker.isValidNCName(s)) {
                // ok
            } else {
                throw new XPathException("Value of " + Err.wrap(key) +
                        " must be a list of QNames in '{uri}local' notation", "SEPM0016");
            }
        }
    }

    private static Pattern publicIdPattern = Pattern.compile("^[\\s\\r\\na-zA-Z0-9\\-'()+,./:=?;!*#@$_%]*$");

    private static void checkPublicIdentifier(String value) throws XPathException {
        if (!publicIdPattern.matcher(value).matches()) {
            throw new XPathException("Invalid character in doctype-public parameter", "SEPM0016");
        }
    }

    private static void checkSystemIdentifier(/*@NotNull*/ String value) throws XPathException {
        if (value.contains("'") && value.contains("\"")) {
            throw new XPathException("The doctype-system parameter must not contain both an apostrophe and a quotation mark", "SEPM0016");
        }
    }

    /**
     * Process a serialization property whose value is a list of element names, for example cdata-section-elements
     *
     *
     * @param value        The value of the property as written
     * @param nsResolver   The namespace resolver to use; may be null if prevalidated is set or if names are supplied
     *                     in Clark format
     * @param useDefaultNS
     * @param prevalidated true if the property has already been validated
     * @param errorCode    The error code to return in the event of problems
     * @return The list of element names with lexical QNames replaced by Clark names, starting with a single space
     * @throws XPathException if any error is found in the list of element names, for example, an undeclared namespace prefix
     */

    /*@NotNull*/
    public static String parseListOfNodeNames(
            String value, /*@Nullable*/ NamespaceResolver nsResolver, boolean useDefaultNS, boolean prevalidated, /*@NotNull*/  String errorCode)
            throws XPathException {
        String s = "";
        StringTokenizer st = new StringTokenizer(value, " \t\n\r", false);
        while (st.hasMoreTokens()) {
            String displayname = st.nextToken();
            if (prevalidated || (nsResolver == null)) {
                s += ' ' + displayname;
            } else if (displayname.startsWith("Q{")) {
                s += ' ' + displayname.substring(1);
            } else {
                try {
                    String[] parts = NameChecker.getQNameParts(displayname);
                    String muri = nsResolver.getURIForPrefix(parts[0], useDefaultNS);
                    if (muri == null) {
                        throw new XPathException("Namespace prefix '" + parts[0] + "' has not been declared", errorCode);
                    }
                    s += " {" + muri + '}' + parts[1];
                } catch (QNameException err) {
                    throw new XPathException("Invalid element name. " + err.getMessage(), errorCode);
                }
            }
        }
        return s;
    }

    /**
     * Examine the already-validated properties to see whether the html-version property is present
     * with the decimal value 5.; used to decide whether to produce XHTML 5.0 in the XHTML output
     * method.
     *
     * @param properties the properties to be examined
     * @return true if the properties include html-version="5.0". The property is a decimal value, so
     *         it can also be written, for example, "5" or "+5.00".
     */
    public static boolean isXhtmlHtmlVersion5(Properties properties) {
        String htmlVersion = properties.getProperty(SaxonOutputKeys.HTML_VERSION);
        try {
            return htmlVersion != null &&
                    ((DecimalValue) DecimalValue.makeDecimalValue(htmlVersion, false).asAtomic())
                            .getDecimalValue().equals(BigDecimal.valueOf(5));
        } catch (ValidationException e) {
            return false;
        }
    }

    /**
     * Examine the already-validated properties to see whether the html-version property is present
     * with the decimal value 5.0, or if absent, the version property is present with the value 5.0.
     * Used to decide whether to produce HTML5 output in the HTML output method.
     *
     * @param properties the properties to be examined
     * @return true if the properties include html-version="5.0". The property is a decimal value, so
     *         it can also be written, for example, "5" or "+5.00".
     */
    public static boolean isHtmlVersion5(Properties properties) {
        String htmlVersion = properties.getProperty(SaxonOutputKeys.HTML_VERSION);
        if (htmlVersion == null) {
            htmlVersion = properties.getProperty(OutputKeys.VERSION);
        }
        if (htmlVersion != null) {
            try {
                return ((DecimalValue) DecimalValue.makeDecimalValue(htmlVersion, false).asAtomic())
                                .getDecimalValue().equals(BigDecimal.valueOf(5));
            } catch (ValidationException e) {
                return false;
            }
        } else {
            return false;
        }
    }

    public static boolean isBuildTree(Properties properties) {
        String buildTreeProperty = properties.getProperty("build-tree");
        if (buildTreeProperty != null) {
            return "yes".equals(buildTreeProperty);
        }
        String method = properties.getProperty("method");
        return !("json".equals(method) || "adaptive".equals(method));
    }
}

