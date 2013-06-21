package net.sf.saxon.serialize;

import net.sf.saxon.event.ProxyReceiver;
import net.sf.saxon.event.Receiver;
import net.sf.saxon.om.NodeName;
import net.sf.saxon.serialize.codenorm.Normalizer;
import net.sf.saxon.trans.XPathException;
import net.sf.saxon.type.SimpleType;
import net.sf.saxon.value.Whitespace;

/**
 * UnicodeNormalizer: This ProxyReceiver performs unicode normalization on the contents
 * of attribute and text nodes.
 *
 * @author Michael Kay
*/


public class UnicodeNormalizer extends ProxyReceiver {

    private Normalizer normalizer;

    public UnicodeNormalizer(String form, Receiver next) throws XPathException {
        super(next);
        byte fb;
        if (form.equals("NFC")) {
            fb = Normalizer.C;
        } else if (form.equals("NFD")) {
            fb = Normalizer.D;
        } else if (form.equals("NFKC")) {
            fb = Normalizer.KC;
        } else if (form.equals("NFKD")) {
            fb = Normalizer.KD;
        } else {
            XPathException err = new XPathException("Unknown normalization form " + form);
            err.setErrorCode("SESU0011");
            throw err;
        }

        normalizer = new Normalizer(fb, getConfiguration());
    }

    /**
     * Output an attribute
     */

    public void attribute(NodeName nameCode, SimpleType typeCode, CharSequence value, int locationId, int properties)
            throws XPathException {
        nextReceiver.attribute(nameCode, typeCode, normalizer.normalize(value), locationId, properties);
    }

    /**
    * Output character data
    */

    public void characters(/*@NotNull*/ CharSequence chars, int locationId, int properties) throws XPathException {
        if (Whitespace.isWhite(chars)) {
            nextReceiver.characters(chars, locationId, properties);
        } else {
            nextReceiver.characters(normalizer.normalize(chars), locationId, properties);
        }
    }

};

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