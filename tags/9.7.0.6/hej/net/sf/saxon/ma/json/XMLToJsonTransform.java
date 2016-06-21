////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
// Copyright (c) 2015 Saxonica Limited.
// This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
// If a copy of the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
// This Source Code Form is "Incompatible With Secondary Licenses", as defined by the Mozilla Public License, v. 2.0.
////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

package net.sf.saxon.ma.json;

import net.sf.saxon.ma.map.MapItem;
import net.sf.saxon.expr.XPathContext;
import net.sf.saxon.functions.SystemFunction;
import net.sf.saxon.lib.StandardEntityResolver;
import net.sf.saxon.om.Sequence;
import net.sf.saxon.s9api.QName;
import net.sf.saxon.s9api.SaxonApiException;
import net.sf.saxon.s9api.XsltCompiler;
import net.sf.saxon.s9api.XsltTransformer;
import net.sf.saxon.trans.XPathException;
import net.sf.saxon.value.AtomicValue;
import net.sf.saxon.value.BooleanValue;
import net.sf.saxon.value.StringValue;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import javax.xml.transform.Source;
import javax.xml.transform.TransformerException;
import javax.xml.transform.URIResolver;
import javax.xml.transform.sax.SAXSource;
import java.io.IOException;

/**
 * Implement the XML to JSON conversion as a built-in function - fn:xml-to-json()
 * <p>This exploits the stylesheets xml-to-json.xsl and xml-to-json-indent.xsl, to perform the actual conversion.
 * These are held as resources within the Saxon jar and compiled when needed.  </p>
 */
public class XMLToJsonTransform extends SystemFunction  {

    // TODO: this implementation is not in active use.

    public static final String STYLESHEET_BASIC_URI = "xml-to-json.xsl";
    public static final String STYLESHEET_INDENT_URI = "xml-to-json-indent.xsl";
    //public static final String STYLESHEET_URI = "http://www.w3.org/2013/XSL/xml-to-json.xsl";
    //public static final String STYLESHEET_INDENT_URI = "http://www.w3.org/2013/XSL/xml-to-json-indent.xsl";
    public static final String JSON_NS = "http://www.w3.org/2005/xpath-functions/json";
    public static final String XML_TO_JSON = "xml-to-json";

    public static String paramNames[] = {
            "key-delimiter", "string-delimiter",
            "start-array", "end-array", "start-map", "end-map",
            "key-value-separator", "array-separator", "map-separator"};

    private static final String ERR_OPTIONS = "XTDE3260";

    public XsltTransformer transform_basic = null;
    public XsltTransformer transform_indent = null;

    /**
     * Throw an error
     *
     * @param message the error message
     * @param code    the error code to be used
     * @throws net.sf.saxon.trans.XPathException
     *          always
     */

    private void error(String message, String code)
            throws XPathException {
        throw new XPathException(message, code);
    }

    /**
     * A specialist URI resolver for handling the stylesheet(s) for the conversion.
     * In particular it permits an entity (e.g. xml-to-json-indent.xsl) to refer to
     * another entity (i.e. xsl:to-json.xsl) via an xsl:import
     */
    class EntityURIResolver implements URIResolver {
        public Source resolve(String href, String base) throws TransformerException {
            InputSource is = null;
            try {
                is = StandardEntityResolver.getInstance().resolveEntity(null, href);
                is.setSystemId(href);
            } catch (SAXException e) {
                throw new TransformerException(e);
            } catch (IOException e) {
                throw new TransformerException(e);
            }
            return new SAXSource(is);
        }
    }

    /**
     * Make a transform for the given entity URI which will presumably be cached for
     * future multiple use of the conversion
     *
     * @param xslt the XSLT compiler object to use
     * @param uri  the 'uri' of the stylesheet
     * @return the transformer
     * @throws net.sf.saxon.trans.XPathException in the event of a failure
     */
    private XsltTransformer makeTransform(XsltCompiler xslt, String uri) throws XPathException {
        try {
            URIResolver res = new EntityURIResolver();
            xslt.setURIResolver(res);
            XsltTransformer t = xslt.compile(res.resolve(uri, null)).load();
            t.setInitialTemplate(new QName(JSON_NS, XML_TO_JSON));
            return t;
        } catch (SaxonApiException e) {
            error("cannot compile/load " + uri + ":" + e.getMessage(), "Error1234");
        } catch (TransformerException e) {
            error("cannot find/transform " + uri + ":" + e.getMessage(), "Error1234");
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        }
        return null;
    }

    public Sequence call(XPathContext context, Sequence[] arguments) throws XPathException {
        //Item arg0 = arguments[0].head();
        return new StringValue("a string");
    }

//    /**
//     * Evaluate the function to produce a single item or an empty sequence
//     * <p>The first argument is the XML tree to convert. The second, optional, is a map of parameters.
//     * Valid entries in this map have the following string-valued keys,
//     * and corresponding typed entry values:</p>
//     * <ul>
//     * <li>'indent' as xs:boolean - whether to produce an indented result - default false()</li>
//     * <li>'indent-spaces' as xs:integer - the number of spaces to indent by - default 3</li>
//     * <li>'start-array' as xs:string - the string to use to mark the start of an array - default '['</li>
//     * <li>'array-separator' as xs:string = the string to separate array elements - default ','</li>
//     * <li>'end-array' as xs:string - the string to use to mark the end of an array - default ']'</li>
//     * <li>'start-map' as xs:string - the string to use to mark the start of an map - default '{'</li>
//     * <li>'key-delimiter' as xs:string - the string to delimit start and end of map keys - default '"'</li>
//     * <li>'key-value-separator' as xs:string - the string to separate map keys from values - default ':'</li>
//     * <li>'map-separator' as xs:string = the string to separate map entries - default ','</li>
//     * <li>'end-map' as xs:string - the string to use to mark the end of an map - default '}'</li>
//     * <li>'string-delimiter' as xs:string - the string to delimit start and end of string values - default '"'</li>
//     * </ul>
//     *
//     * @param context the context in which the expression is to be evaluated
//     * @return either an item, or null to denote the empty sequence
//     * @throws net.sf.saxon.trans.XPathException
//     *          if a failure occurs, e.g. bad JSON syntax
//     */
//
//    /*@Nullable*/
//    public Item evaluateItem(XPathContext context) throws XPathException {
//        XsltCompiler xslt = ((Processor) context.getConfiguration().getProcessor()).newXsltCompiler();
//        Item xml = getArg(0).evaluateItem(context);
//        MapItem options;
//        if (getArity() == 2) {
//            options = (MapItem) getArg(1).evaluateItem(context);   // check it is a map
//        } else {
//            options = new HashTrieMap(context);
//        }
//        Sequence val = options.get(new StringValue("indent"));
//        boolean indent = val != null && val instanceof BooleanValue && ((BooleanValue) val).getBooleanValue();
//
//        val = options.get(new StringValue("indent-spaces"));
//        long indentSpaces = val != null && val instanceof Int64Value ? ((Int64Value) val).longValue() : -1;
//
//        HashMap<String, String> params = new HashMap<String, String>();
//        for (String s : paramNames) {
//            String value = getOption(options, s, context, null);
//            if (value != null) {
//                params.put(s, value);
//            }
//        }
//
//        XsltTransformer transform;
//        if (indent) {
//            if (transform_indent == null) {
//                transform_indent = makeTransform(xslt, STYLESHEET_INDENT_URI);
//            }
//            transform = transform_indent;
//        } else {
//            if (transform_basic == null) {
//                transform_basic = makeTransform(xslt, STYLESHEET_BASIC_URI);
//            }
//            transform = transform_basic;
//        }
//        XdmDestination xdm = null;
//        try {
//            transform.setInitialContextNode(new XdmNode((NodeInfo) xml));   // check it is a nodeinfo
//            xdm = new XdmDestination();
//            transform.setDestination(xdm);
//            if (indentSpaces >= 0) {
//                try {
//                    transform.setParameter(new QName(JSON_NS, "indent-spaces"), new XdmAtomicValue(indentSpaces));
//                } catch (NumberFormatException e) {
//                    error("Indent-spaces - nNumber format error:" + indentSpaces, ERR_OPTIONS);
//                }
//            }
//            for (String s : params.keySet()) {
//                transform.setParameter(new QName(JSON_NS, s), new XdmAtomicValue(params.get(s)));
//            }
//            transform.transform();
//        } catch (SaxonApiException e) {
//            error("cannot run " + ":" + e, "error1234");
//        }
//        return (Item) xdm.getXdmNode().getUnderlyingNode();
//    }

    /**
     * Get the value of an option setting (as a string)
     *
     * @param options      the set of options provided
     * @param option       the name of the option required
     * @param context      XPath evaluation context
     * @param defaultValue the default to be used if the option has not been specified
     * @return the setting of the option
     * @throws net.sf.saxon.trans.XPathException
     *          if the options cannot be read
     */

    private String getOption(MapItem options, String option, XPathContext context, String defaultValue)
            throws XPathException {
        StringValue ov = new StringValue(option);
        Sequence val = options.get(ov);
        if (val == null) {
            return defaultValue;
        } else if (val instanceof AtomicValue) {
            return ((AtomicValue) val).getStringValue();
        } else {
            error("Value of option '" + option + "' is not xs:string", ERR_OPTIONS);
            return defaultValue;
        }
    }

    /**
     * Get the value of an option setting (as a boolean)
     *
     * @param options      the set of options provided
     * @param option       the name of the option required
     * @param context      XPath evaluation context
     * @param defaultValue the default to be used if the option has not been specified
     * @return the setting of the option
     * @throws net.sf.saxon.trans.XPathException
     *          if the options cannot be read
     */
    private boolean getOption(MapItem options, String option, XPathContext context, boolean defaultValue)
            throws XPathException {
        StringValue ov = new StringValue(option);
        Sequence val = options.get(ov);
        if (val == null) {
            return defaultValue;
            //} else if (val instanceof AtomicValue) {
        } else if (val instanceof BooleanValue) {
            return ((BooleanValue) val).getBooleanValue();
        } else {
            error("Value of option '" + option + "' is not xs:boolean", ERR_OPTIONS);
            return defaultValue;
        }
    }

}

// Copyright (c) 2015 Saxonica Limited. All rights reserved.
