////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
// Copyright (c) 2015 Saxonica Limited.
// This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
// If a copy of the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
// This Source Code Form is "Incompatible With Secondary Licenses", as defined by the Mozilla Public License, v. 2.0.
////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

package net.sf.saxon.ma.json;

import net.sf.saxon.ma.map.MapItem;
import net.sf.saxon.expr.XPathContext;
import net.sf.saxon.om.Item;
import net.sf.saxon.trans.XPathException;

/**
 * Implements the parse-json function, as defined in XPath 3.1
 *
 * The parsing code, and the handling of options is shared with the json-to-xml function.
 */
public class ParseJsonFn extends JsonToXMLFn {


    /**
     * Parse the JSON string according to supplied options
     *
     * @param input   JSON input string
     * @param options options for the conversion as a map of xs:string : value pairs
     * @param context XPath evaluation context
     * @return the result of the parsing, as an XML element
     * @throws XPathException if the syntax of the input is incorrect
     */
    @Override
    protected Item eval(String input, MapItem options, XPathContext context) throws XPathException {
        return parse(input, options, context);
    }

    /**
     * Parse the JSON string according to supplied options
     *
     * @param input   JSON input string
     * @param options options for the conversion as a map of xs:string : value pairs
     * @param context XPath evaluation context
     * @return the result of the parsing, as an XML element
     * @throws XPathException if the syntax of the input is incorrect
     */

    public static Item parse(String input, MapItem options, XPathContext context) throws XPathException {
        JsonParser parser = new JsonParser(context.getConfiguration().getValidCharacterChecker());
        options = JsonParser.checkOptions(options, context);
        int flags = JsonParser.getFlags(options, context, false);
        JsonHandlerMap handler = new JsonHandlerMap(context, flags);
        if ((flags & JsonParser.DUPLICATES_RETAINED) != 0) {
            throw new XPathException("parse-json: duplicates=retain is not allowed", "FOJS0005");
        }
        if ((flags & JsonParser.DUPLICATES_SPECIFIED) == 0) {
            flags |= JsonParser.DUPLICATES_FIRST;
        }
        handler.setFallbackFunction(options, context);
        parser.parse(input, flags, handler, context);
        return handler.getResult().head();
    }


}

// Copyright (c) 2011-2014 Saxonica Limited. All rights reserved.