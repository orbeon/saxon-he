////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
// Copyright (c) 2015 Saxonica Limited.
// This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
// If a copy of the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
// This Source Code Form is "Incompatible With Secondary Licenses", as defined by the Mozilla Public License, v. 2.0.
////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

package net.sf.saxon.ma.json;

import net.sf.saxon.ma.map.HashTrieMap;
import net.sf.saxon.ma.map.MapItem;
import net.sf.saxon.Configuration;
import net.sf.saxon.expr.XPathContext;
import net.sf.saxon.functions.SystemFunction;
import net.sf.saxon.functions.UnparsedTextFunction;
import net.sf.saxon.om.Item;
import net.sf.saxon.om.Sequence;
import net.sf.saxon.trans.Err;
import net.sf.saxon.trans.XPathException;
import net.sf.saxon.value.EmptySequence;
import net.sf.saxon.z.IntPredicate;

import java.io.Reader;
import java.net.URI;

/**
 * Implements the json-to-xml function defined in XSLT 3.0.
 */

public class JsonDoc extends SystemFunction  {

    /**
     * Evaluate the expression
     *
     * @param context   the dynamic evaluation context
     * @param arguments the values of the arguments, supplied as SequenceIterators
     * @return the result of the evaluation, in the form of a SequenceIterator
     * @throws net.sf.saxon.trans.XPathException
     *          if a dynamic error occurs during the evaluation of the expression
     */
    public Sequence call(XPathContext context, Sequence[] arguments) throws XPathException {

        Item arg0 = arguments[0].head();
        if (arg0 == null) {
            return EmptySequence.getInstance();
        }
        String href = arg0.getStringValue();

        final Configuration config = context.getConfiguration();
        IntPredicate checker = config.getValidCharacterChecker();

        // Use the URI machinery to validate and resolve the URIs

        URI absoluteURI = UnparsedTextFunction.getAbsoluteURI(href, getStaticBaseUriString(), context);

        String encoding = "UTF-8"; // for now

        Reader reader;
        try {
            reader = context.getController().getUnparsedTextURIResolver().resolve(absoluteURI, encoding, config);
        } catch (XPathException err) {
            err.maybeSetErrorCode("FOUT1170");
            throw err;
        }
        CharSequence content;
        try {
            content = UnparsedTextFunction.readFile(checker, reader, context);
        } catch (java.io.UnsupportedEncodingException encErr) {
            XPathException e = new XPathException("Unknown encoding " + Err.wrap(encoding), encErr);
            e.setErrorCode("FOUT1190");
            throw e;
        } catch (java.io.IOException ioErr) {
//            System.err.println("ProxyHost: " + System.getProperty("http.proxyHost"));
//            System.err.println("ProxyPort: " + System.getProperty("http.proxyPort"));
            XPathException e = UnparsedTextFunction.handleIOError(absoluteURI, ioErr, context);
            throw e;
        }

        MapItem options;
        if (getArity() == 2) {
            options = (MapItem) arguments[1].head();
        } else {
            options = new HashTrieMap(context);
        }
        Item result = ParseJsonFn.parse(content.toString(), options, context);
        return result == null ? EmptySequence.getInstance() : result;
    }


}

// Copyright (c) 2015 Saxonica Limited. All rights reserved.