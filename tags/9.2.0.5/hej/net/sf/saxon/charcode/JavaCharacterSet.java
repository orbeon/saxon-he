package net.sf.saxon.charcode;

import java.nio.charset.Charset;
import java.nio.charset.CharsetEncoder;
import java.util.HashMap;

/**
 * This class establishes properties of a character set that is
 * known to the Java VM but not specifically known to Saxon
*/

public class JavaCharacterSet implements CharacterSet {

    public static HashMap<Charset, JavaCharacterSet> map;

    private CharsetEncoder encoder;

    // This class is written on the assumption that the CharsetEncoder.canEncode()
    // method may be expensive. For BMP characters, it therefore remembers the results
    // so each character is only looked up the first time it is encountered.

    private byte[] charinfo = new byte[65536];
        // rely on initialization to zeroes
    private StringBuffer supplementary = new StringBuffer(2);

    //private final static byte UNKNOWN = 0;
    private static final byte GOOD = 1;
    private static final byte BAD = 2;

    private JavaCharacterSet(Charset charset) {
        encoder = charset.newEncoder();
    }

    public static synchronized JavaCharacterSet makeCharSet(Charset charset) {
        if (map == null) {
            map = new HashMap<Charset, JavaCharacterSet>(10);
        }
        JavaCharacterSet c = map.get(charset);
        if (c == null) {
            c = new JavaCharacterSet(charset);
            map.put(charset, c);
        }
        return c;
    }

    public final boolean inCharset(int c) {
        // Assume ASCII chars are always OK
        if (c <= 127) {
            return true;
        }
        if (c <= 65535) {
            if (charinfo[c] == GOOD) {
                return true;
            } else if (charinfo[c] == BAD) {
                return false;
            } else {
                if (encoder.canEncode((char)c)) {
                    charinfo[c] = GOOD;
                    return true;
                } else {
                    charinfo[c] = BAD;
                    return false;
                }
            }
        } else {
            supplementary.setCharAt(0, UTF16CharacterSet.highSurrogate(c));
            supplementary.setCharAt(1, UTF16CharacterSet.lowSurrogate(c));
            return encoder.canEncode(supplementary);
        }
    }

    public String getCanonicalName() {
        return encoder.charset().name();
    }
}

// Copyright (c) 2009 Saxonica Limited. All rights reserved.

// The contents of this file are subject to the Mozilla Public License Version 1.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at http://www.mozilla.org/MPL/
//
// Software distributed under the License is distributed on an "AS IS" basis,
// WITHOUT WARRANTY OF ANY KIND, either express or implied.
// See the License for the specific language governing rights and limitations under the License.
//
// The Original Code is: all this file.
//
// The Initial Developer of the Original Code is Saxonica Limited.
//
// Portions created by __ are Copyright (C) __. All Rights Reserved.
//
// Contributor(s): 	None
//