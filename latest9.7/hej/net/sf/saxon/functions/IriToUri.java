////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
// Copyright (c) 2015 Saxonica Limited.
// This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
// If a copy of the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
// This Source Code Form is "Incompatible With Secondary Licenses", as defined by the Mozilla Public License, v. 2.0.
////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

package net.sf.saxon.functions;

import net.sf.saxon.expr.XPathContext;
import net.sf.saxon.om.Item;
import net.sf.saxon.om.ZeroOrOne;
import net.sf.saxon.serialize.charcode.UTF8CharacterSet;
import net.sf.saxon.trans.XPathException;
import net.sf.saxon.tree.util.FastStringBuffer;
import net.sf.saxon.value.AtomicValue;
import net.sf.saxon.value.StringValue;

import java.util.Arrays;

/**
 * This class supports the functions encode-for-uri() and iri-to-uri()
 */

public class IriToUri extends ScalarSystemFunction {

    public static boolean[] allowedASCII = new boolean[128];

    static {
        Arrays.fill(allowedASCII, 0, 32, false);
        Arrays.fill(allowedASCII, 33, 127, true);
        allowedASCII[(int) '"'] = false;
        allowedASCII[(int) '<'] = false;
        allowedASCII[(int) '>'] = false;
        allowedASCII[(int) '\\'] = false;
        allowedASCII[(int) '^'] = false;
        allowedASCII[(int) '`'] = false;
        allowedASCII[(int) '{'] = false;
        allowedASCII[(int) '|'] = false;
        allowedASCII[(int) '}'] = false;
    }

    @Override
    public AtomicValue evaluate(Item arg, XPathContext context) throws XPathException {
        final CharSequence s = arg.getStringValueCS();
        return StringValue.makeStringValue(iriToUri(s));
    }

    @Override
    public ZeroOrOne<? extends AtomicValue> resultWhenEmpty() {
        return ZERO_LENGTH_STRING;
    }

    /**
     * Escape special characters in a URI. The characters that are %HH-encoded are
     * all non-ASCII characters
     *
     * @param s the URI to be escaped
     * @return the %HH-encoded string
     */

    public static CharSequence iriToUri(CharSequence s) {
        // NOTE: implements a late spec change which says that characters that are illegal in an IRI,
        // for example "\", must be %-encoded.
        if (allAllowedAscii(s)) {
            // it's worth doing a prescan to avoid the cost of copying in the common all-ASCII case
            return s;
        }
        FastStringBuffer sb = new FastStringBuffer(s.length() + 20);
        for (int i = 0; i < s.length(); i++) {
            final char c = s.charAt(i);
            if (c >= 0x7f || !allowedASCII[(int) c]) {
                escapeChar(c, (i + 1) < s.length() ? s.charAt(i + 1) : ' ', sb);
            } else {
                sb.append(c);
            }
        }
        return sb;
    }

    private static boolean allAllowedAscii(CharSequence s) {
        for (int i = 0; i < s.length(); i++) {
            final char c = s.charAt(i);
            if (c >= 0x7f || !allowedASCII[(int) c]) {
                return false;
            }
        }
        return true;
    }


    private static final String hex = "0123456789ABCDEF";

    /**
     * Escape a single character in %HH representation, or a pair of two chars representing
     * a surrogate pair
     *
     * @param c  the character to be escaped, or the first character of a surrogate pair
     * @param c2 the second character of a surrogate pair
     * @param sb the buffer to contain the escaped result
     */

    private static void escapeChar(char c, char c2, FastStringBuffer sb) {
        byte[] array = new byte[4];
        int used = UTF8CharacterSet.getUTF8Encoding(c, c2, array);
        for (int b = 0; b < used; b++) {
            int v = (int) array[b] & 0xff;
            sb.append('%');
            sb.append(hex.charAt(v / 16));
            sb.append(hex.charAt(v % 16));
        }
    }

}

