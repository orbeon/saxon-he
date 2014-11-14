using System;
using System.Collections.Generic;
using System.Text;
using JNamespaceConstant = net.sf.saxon.lib.NamespaceConstant;
namespace Saxon.Api
{
    class NamespaceConstant
    {

	/**
	 * A URI representing the null namespace (actually, an empty string)
	 */

	public static readonly String NULL = "";
	/**
	 * The numeric URI code representing the null namespace (actually, zero)
	 */
	public static readonly short NULL_CODE = 0;

    /**
     * Fixed namespace name for XML: "http://www.w3.org/XML/1998/namespace".
     */
    public static readonly String XML = "http://www.w3.org/XML/1998/namespace";
    /**
     * Numeric code representing the XML namespace
     */
    public static readonly short XML_CODE = 1;


    /**
     * Fixed namespace name for XSLT: "http://www.w3.org/1999/XSL/Transform"
     */
    public static readonly String XSLT = "http://www.w3.org/1999/XSL/Transform";
    /**
     * Numeric code representing the XSLT namespace
     */
    public static readonly short XSLT_CODE = 2;

    /**
     * Fixed namespace name for SAXON: "http://saxon.sf.net/"
     */
    public static readonly String SAXON = "http://saxon.sf.net/";
    /**
     * Numeric code representing the SAXON namespace
     */
    public static readonly short SAXON_CODE = 3;

    /**
     * Namespace name for XML Schema: "http://www.w3.org/2001/XMLSchema"
     */
    public static readonly String SCHEMA = "http://www.w3.org/2001/XMLSchema";
    /**
     * Numeric code representing the schema namespace
     */
    public static readonly short SCHEMA_CODE = 4;

    /**
     * XML-schema-defined namespace for use in instance documents ("xsi")
     */
    public static readonly String SCHEMA_INSTANCE = "http://www.w3.org/2001/XMLSchema-instance";

    public static readonly short XSI_CODE = 5;

    /**
     * Namespace defined in XSD 1.1 for schema versioning
     */
    public static readonly String SCHEMA_VERSIONING = "http://www.w3.org/2007/XMLSchema-versioning";

    /**
     * Fixed namespace name for SAXON SQL extension: "http://saxon.sf.net/sql"
     */
    public static readonly String SQL = "http://saxon.sf.net/sql";

    /**
     * Fixed namespace name for EXSLT/Common: "http://exslt.org/common"
     */
    public static readonly String EXSLT_COMMON = "http://exslt.org/common";

    /**
     * Fixed namespace name for EXSLT/math: "http://exslt.org/math"
     */
    public static readonly String EXSLT_MATH = "http://exslt.org/math";

    /**
     * Fixed namespace name for EXSLT/sets: "http://exslt.org/sets"
     */
    public static readonly String EXSLT_SETS = "http://exslt.org/sets";

    /**
     * Fixed namespace name for EXSLT/date: "http://exslt.org/dates-and-times"
     */
    public static readonly String EXSLT_DATES_AND_TIMES = "http://exslt.org/dates-and-times";

    /**
     * Fixed namespace name for EXSLT/random: "http://exslt.org/random"
     */
    public static readonly String EXSLT_RANDOM = "http://exslt.org/random";

    /**
     * The standard namespace for functions and operators
     */
    public static readonly String FN = "http://www.w3.org/2005/xpath-functions";

    /**
     * The standard namespace for XQuery output declarations
     */
    public static readonly String OUTPUT = "http://www.w3.org/2010/xslt-xquery-serialization";


    /**
     * The standard namespace for system error codes
     */
    public static readonly String ERR = "http://www.w3.org/2005/xqt-errors";


    /**
     * Predefined XQuery namespace for local functions
     */
    public static readonly String LOCAL = "http://www.w3.org/2005/xquery-local-functions";
    
    /**
     * Math namespace for the XPath 3.0 math functions
     */

    public static readonly String MATH = "http://www.w3.org/2005/xpath-functions/math";

    /**
     * Namespace URI for XPath 3.0 functions associated with maps
     */
    public readonly static String MAP_FUNCTIONS = "http://www.w3.org/2005/xpath-functions/map";

    
    /**
     * Recognize the Microsoft namespace so we can give a suitably sarcastic error message
     */

    public static readonly String MICROSOFT_XSL = "http://www.w3.org/TR/WD-xsl";

    /**
     * The XHTML namespace http://www.w3.org/1999/xhtml
     */

    public static readonly String XHTML = "http://www.w3.org/1999/xhtml";

    /**
     * The SVG namespace
     */

    public static readonly String SVG = "http://www.w3.org/2000/svg";

    /**
     * The MathML namespace
     */

    public static readonly String MATHML = "http://www.w3.org/1998/Math/MathML";

    /**
     * The XMLNS namespace (used in DOM)
     */

    public static readonly String XMLNS = "http://www.w3.org/2000/xmlns/";

    /**
     * The XLink namespace
     */

    public static readonly String XLINK = "http://www.w3.org/1999/xlink";

    /**
     * The xquery-option namespace for the XQuery 3.0 feature names
     */

    public static readonly String XQUERY_OPTIONS = "http://www.w3.org/2011/xquery-options";

    /**
     * The xquery namespace for the XQuery 3.0 declare option
     */

    public static readonly String XQUERY = "http://www.w3.org/2012/xquery";

    /**
     * Namespace for types representing external Java objects
     */

    public static readonly String JAVA_TYPE = "http://saxon.sf.net/java-type";

   /**
     * Namespace for types representing external .NET objects
     */

    public static readonly String DOT_NET_TYPE = "http://saxon.sf.net/clitype";    

    /**
     * Namespace for names allocated to anonymous types. This exists so that
     * a name fingerprint can be allocated for use as a type annotation.
     */

    public static readonly String ANONYMOUS = "http://ns.saxonica.com/anonymous-type";

    /**
     * Namespace for the Saxon serialization of the schema component model
     */

    public static readonly String SCM = "http://ns.saxonica.com/schema-component-model";

    /**
     * URI identifying the Saxon object model for use in the JAXP 1.3 XPath API
     */

    public static readonly String OBJECT_MODEL_SAXON = "http://saxon.sf.net/jaxp/xpath/om";


    /**
     * URI identifying the XOM object model for use in the JAXP 1.3 XPath API
     */

    public static readonly String OBJECT_MODEL_XOM = "http://www.xom.nu/jaxp/xpath/xom";

    /**
     * URI identifying the JDOM object model for use in the JAXP 1.3 XPath API
     */

    public static readonly String OBJECT_MODEL_JDOM = "http://jdom.org/jaxp/xpath/jdom";

    /**
     * URI identifying the AXIOM object model for use in the JAXP 1.3 XPath API
     */

    // TODO: get an official URI approved
    public static readonly String OBJECT_MODEL_AXIOM = "http://ws.apache.org/jaxp/xpath/axiom";

    /**
     * URI identifying the DOM4J object model for use in the JAXP 1.3 XPath API
     */

    public static readonly String OBJECT_MODEL_DOM4J = "http://www.dom4j.org/jaxp/xpath/dom4j";

    /**
     * URI identifying the .NET DOM object model (not used, but needed for consistency)
     */

    public static readonly String OBJECT_MODEL_DOT_NET_DOM = "http://saxon.sf.net/object-model/dotnet/dom";

    /**
     * URI identifying the Unicode codepoint collation
     */

    public static readonly String CODEPOINT_COLLATION_URI = "http://www.w3.org/2005/xpath-functions/collation/codepoint";

    /**
     * URI for the names of generated global variables
     */

    public static readonly String SAXON_GENERATED_GLOBAL = SAXON + "generated-global-variable";

    /**
     * URI for the Saxon configuration file
     */

    public static readonly String SAXON_CONFIGURATION = "http://saxon.sf.net/ns/configuration";

    /**
     * URI for the EXPath zip module
     */

    public static readonly String EXPATH_ZIP = "http://expath.org/ns/zip";


    /**
     * Determine whether a namespace is a reserved namespace
     * @param uri the namespace URI to be tested
     * @return true if this namespace URI is a reserved namespace
     */

        public static bool isReserved(/*@Nullable*/ String uri) {

            return JNamespaceConstant.isReserved(uri);    
        }

        /**
         * Determine whether a namespace is a reserved namespace
         * @param uri the namespace URI to be tested
         * @return true if this namespace URI is reserved in XQuery
         */
        public static bool isReservedInQuery(String uri)
        {
            return JNamespaceConstant.isReservedInQuery(uri);
        }

        /**
          * Determine whether a namespace is a reserved namespace
          * @param uri the namespace URI to be tested
          * @return true if this namespace URI is reserved in XQuery
          */
        public static bool isReservedInQuery30(String uri) {
            return JNamespaceConstant.isReservedInQuery30(uri);
        }

        /**
         * Find a similar namespace to one that is a possible mis-spelling
         * @param candidate the possibly mis-spelt namespace
         * @return the correct spelling of the namespace
         */
        public static String findSimilarNamespace(String candidate) {
            return JNamespaceConstant.findSimilarNamespace(candidate);
        }
    }
}

////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
// Copyright (c) 2013 Saxonica Limited.
// This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
// If a copy of the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
// This Source Code Form is "Incompatible With Secondary Licenses", as defined by the Mozilla Public License, v. 2.0.
////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
