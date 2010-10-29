package net.sf.saxon.serialize;

import net.sf.saxon.expr.sort.IntHashMap;
import net.sf.saxon.expr.sort.IntIterator;
import net.sf.saxon.tree.util.FastStringBuffer;
import net.sf.saxon.tree.tiny.CompressedWhitespace;
import net.sf.saxon.serialize.charcode.UTF16CharacterSet;
import net.sf.saxon.value.Whitespace;

import java.io.Serializable;
import java.util.Iterator;

/**
 * This class defines a character map, that is, a mapping from characters to strings used by the serializer
 * when mapping individual characters in the output.
 */
public class CharacterMap implements Serializable {

    private IntHashMap<String> charMap;
    private int min = Integer.MAX_VALUE;    // the lowest mapped character
    private int max = 0;                    // the highest mapped character
    private boolean mapsWhitespace = false;

    /**
     * Create a CharacterMap from a raw map of integers to strings
     * @param map the mapping of integer Unicode character codes to strings
     */

    public CharacterMap(IntHashMap<String> map) {
        this.charMap = map;
        init();
    }

    /**
     * Create a CharacterMap that combines a set of existing character maps.
     * @param list the list of existing character maps. If the same character
     * is mapped by more than one map in the list, the last mapping takes
     * precedence
     */

    public CharacterMap(Iterable<CharacterMap> list) {
        charMap = new IntHashMap(64);
        for (Iterator iter = list.iterator(); iter.hasNext(); ) {
            CharacterMap map = (CharacterMap)iter.next();
            IntIterator keys = map.charMap.keyIterator();
            while (keys.hasNext()) {
                int next = keys.next();
                charMap.put(next, map.charMap.get(next));
            }
        }
        init();
    }

    private void init() {
        IntIterator keys = charMap.keyIterator();
        while (keys.hasNext()) {
            int next = keys.next();
            if (next < min) {
                min = next;
            }
            if (next > max) {
                max = next;
            }
            if (!mapsWhitespace && Whitespace.isWhitespace(next)) {
                mapsWhitespace = true;
            }
        }
        if (min > 0xD800) {
            // if all the mapped characters are above the BMP, we need to check
            // surrogates
            min = 0xD800;
        }
    }


    /**
     * Expand all the characters in a string using this character mapping
     * @param in the input string to be mapped
     * @param insertNulls true if null (0) characters are to be inserted before
     * and after replacement characters. This is done to signal
     * that output escaping of these characters is disabled. The flag is set to true when writing
     * XML or HTML, but to false when writing TEXT.
     */

    public CharSequence map(CharSequence in, boolean insertNulls) {

        if ((!mapsWhitespace) && in instanceof CompressedWhitespace) {
            return in;
        }

        // First scan the string to see if there are any possible mapped
        // characters; if not, don't bother creating the new buffer

        boolean move = false;
        for (int i=0; i<in.length();) {
            char c = in.charAt(i++);
            if (c >= min && c <= max) {
                move = true;
                break;
            }
        }
        if (!move) {
            return in;
        }

        FastStringBuffer buffer = new FastStringBuffer(in.length()*2);
        int i = 0;
        while(i < in.length()) {
            char c = in.charAt(i++);
            if (c >= min && c <= max) {
                if (UTF16CharacterSet.isHighSurrogate(c)) {
                    // assume the string is properly formed
                    char d = in.charAt(i++);
                    int s = UTF16CharacterSet.combinePair(c, d);
                    String rep = charMap.get(s);
                    if (rep == null) {
                        buffer.append(c);
                        buffer.append(d);
                    } else {
                        if (insertNulls) {
                            buffer.append((char)0);
                            buffer.append(rep);
                            buffer.append((char)0);
                        } else {
                            buffer.append(rep);
                        }
                    }
                } else {
                    String rep = charMap.get(c);
                    if (rep == null) {
                        buffer.append(c);
                    } else {
                        if (insertNulls) {
                            buffer.append((char)0);
                            buffer.append(rep);
                            buffer.append((char)0);
                        } else {
                            buffer.append(rep);
                        }
                    }
                }
            } else {
                buffer.append(c);
            }
        }
        return buffer;
    }



}

//
// The contents of this file are subject to the Mozilla Public License Version
// 1.0 (the "License");
// you may not use this file except in compliance with the License. You may
// obtain a copy of the
// License at http://www.mozilla.org/MPL/
//
// Software distributed under the License is distributed on an "AS IS" basis,
// WITHOUT WARRANTY OF ANY KIND, either express or implied.
// See the License for the specific language governing rights and limitations
// under the License.
//
// The Original Code is: all this file.
//
// The Initial Developer of the Original Code is Michael Kay,
//
// Portions created by (your name) are Copyright (C) (your legal entity). All
// Rights Reserved.
//
// Contributor(s): none.
//



