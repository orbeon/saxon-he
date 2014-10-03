////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
// Copyright (c) 2014 Saxonica Limited.
// This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
// If a copy of the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
// This Source Code Form is "Incompatible With Secondary Licenses", as defined by the Mozilla Public License, v. 2.0.
////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

package net.sf.saxon.regex;

import net.sf.saxon.tree.util.FastStringBuffer;

/**
 * An abstract class that efficiently handles Unicode strings including
 * non-BMP characters; it has two subclasses, one optimized for strings
 * whose characters are all in the BMP, the other handling the general case.
 */

public abstract class UnicodeString implements CharSequence {

    public static UnicodeString EMPTY_STRING = new LatinString("");

    /**
     * Make a UnicodeString for a given CharSequence
     *
     * @param in the input CharSequence
     * @return a UnicodeString using an appropriate implementation class
     */

    public static UnicodeString makeUnicodeString(CharSequence in) {
        if (in instanceof UnicodeString) {
            return (UnicodeString)in;
        }
        int width = getMaxWidth(in);
        if (width == 1) {
            return new LatinString(in);
        } else if (width == 2) {
            return new BMPString(in);
        } else {
            return new GeneralUnicodeString(in);
        }
    }

    /**
     * Make a UnicodeString for a given array of codepoints
     *
     * @param in the input CharSequence
     * @return a UnicodeString using an appropriate implementation class
     */

    public static UnicodeString makeUnicodeString(int[] in) {
        for (int ch : in) {
            if (ch > 65535) {
                return new GeneralUnicodeString(in, 0, in.length);
            }
        }
        FastStringBuffer fsb = new FastStringBuffer(in.length);
        for (int ch : in) {
            fsb.append((char) ch);
        }
        return new BMPString(fsb);
    }

    /**
     * Test whether a CharSequence contains Unicode codepoints outside the BMP range
     *
     * @param value the string to be tested
     * @return true if the string contains non-BMP codepoints
     */

    public static boolean containsSurrogatePairs(CharSequence value) {
        if (value instanceof BMPString) {
            return false;
        }
        for (int i = 0; i < value.length(); i++) {
            int c = (int) value.charAt(i);
            if (c >= 55296 && c <= 56319) {
                return true;
            }
        }
        return false;
    }

    /**
     * Get the maximum width of codepoints in this string, in bytes
     * @param value the string to be examined
     * @return 1, 2, or 4 depending on whether the string contains characters
     * with codepoints >255 and/or >65535
     */

    public static int getMaxWidth(CharSequence value) {
        if (value instanceof LatinString) {
            return 1;
        }
        if (value instanceof BMPString) {
            return 2;
        }
        if (value instanceof GeneralUnicodeString) {
            return 4;
        }
        boolean nonLatin = false;
        for (int i = 0; i < value.length(); i++) {
            int c = (int) value.charAt(i);
            if (c > 255) {
                nonLatin = true;
            }
            if (c >= 55296 && c <= 56319) {
                return 4;
            }
        }
        return nonLatin ? 2 : 1;
    }

    /**
     * Get a substring of this string
     *
     * @param beginIndex the index of the first character to be included (counting
     *                   codepoints, not 16-bit characters)
     * @param endIndex   the index of the first character to be NOT included (counting
     *                   codepoints, not 16-bit characters)
     * @return a substring
     * @throws IndexOutOfBoundsException if the selection goes off the start or end of the string
     *         (this function follows the semantics of String.substring(), not the XPath semantics)
     */
    public abstract UnicodeString uSubstring(int beginIndex, int endIndex);

    /**
     * Get the first match for a given character
     *
     * @param search the character to look for
     * @param start  the first position to look
     * @return the position of the first occurrence of the sought character, or -1 if not found
     */

    public abstract int uIndexOf(int search, int start);

    /**
     * Get the character at a specified position
     *
     * @param pos the index of the required character (counting
     *            codepoints, not 16-bit characters)
     * @return a character (Unicode codepoint) at the specified position.
     */

    public abstract int uCharAt(int pos);

    /**
     * Get the length of the string, in Unicode codepoints
     *
     * @return the number of codepoints in the string
     */

    public abstract int uLength();

    /**
     * Ask whether a given position is at (or beyond) the end of the string
     *
     * @param pos the index of the required character (counting
     *            codepoints, not 16-bit characters)
     * @return <tt>true</tt> iff if the specified index is after the end of the character stream
     */

    public abstract boolean isEnd(int pos);

    /**
     * Implementations of UnicodeString can be compared with each other, but not with
     * other implementations of CharSequence
     * @return a hashCode that distinuishes this UnicodeString from others
     */

    @Override
    public int hashCode() {
        // Same result as String#hashCode() in the case where all characters are BMP chars
        int h = 0;
        for (int i = 0; i < uLength(); i++) {
            h = 31 * h + uCharAt(i);
        }
        return h;
    }

    /**
     * Implementations of UnicodeString can be compared with each other, but not with
     * other implementations of CharSequence
     * @param obj the object to be compared
     * @return true if obj is a UnicodeString containing the same codepoints
     */

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof UnicodeString)) {
            return false;
        }
        if (uLength() != ((UnicodeString)obj).uLength()) {
            return false;
        }
        for (int i=0; i<uLength(); i++) {
            if (uCharAt(i) != ((UnicodeString)obj).uCharAt(i)) {
                return false;
            }
        }
        return true;
    }
}
