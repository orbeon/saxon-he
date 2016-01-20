////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
// Copyright (c) 2015 Saxonica Limited.
// This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
// If a copy of the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
// This Source Code Form is "Incompatible With Secondary Licenses", as defined by the Mozilla Public License, v. 2.0.
////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

package net.sf.saxon.serialize;

import net.sf.saxon.event.ProxyReceiver;
import net.sf.saxon.event.Receiver;
import net.sf.saxon.expr.parser.Location;
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
     * Get the underlying normalizer
     * @return the underlying Normalizer
     */

    public Normalizer getNormalizer() {
        return normalizer;
    }

    /**
     * Output an attribute
     */

    public void attribute(NodeName nameCode, SimpleType typeCode, CharSequence value, Location locationId, int properties)
            throws XPathException {
        nextReceiver.attribute(nameCode, typeCode, normalizer.normalize(value), locationId, properties);
    }

    /**
     * Output character data
     */

    public void characters(/*@NotNull*/ CharSequence chars, Location locationId, int properties) throws XPathException {
        if (Whitespace.isWhite(chars)) {
            nextReceiver.characters(chars, locationId, properties);
        } else {
            nextReceiver.characters(normalizer.normalize(chars), locationId, properties);
        }
    }

};

