package net.sf.saxon.sort;
import net.sf.saxon.Platform;

/**
 * A Comparer used for comparing strings, with upper case collated before lower case
 * if the strings are otherwise equal. This is implemented as a wrapper around a collator
 * that compares the strings ignoring case.
 *
 * @author Michael H. Kay
 *
 */

public class UppercaseFirstComparer implements StringCollator, java.io.Serializable {

    private StringCollator baseCollator;

    public UppercaseFirstComparer(StringCollator base) {
        baseCollator = base;
    }

    /**
    * Compare two string objects: case is irrelevant, unless the strings are equal ignoring
    * case, in which case uppercase comes first.
    * @return <0 if a<b, 0 if a=b, >0 if a>b
    * @throws ClassCastException if the objects do not implement the CharSequence interface
    */

    public int compareStrings(String a, String b) {
        int diff = baseCollator.compareStrings(a, b);
        if (diff != 0) {
            return diff;
        }

        // This is doing a character-by-character comparison, which isn't really right.
        // There might be a sequence of letters constituting a single collation unit.

        CharSequence a1 = (CharSequence)a;
        CharSequence b1 = (CharSequence)b;

        int i = 0;
        int j = 0;
        while (true) {
            // Skip characters that are equal in the two strings
            while (i < a1.length() && j < b1.length() && a1.charAt(i) == b1.charAt(j)) {
                i++;
                j++;
            }
            // Skip non-letters in the first string
            while (i < a1.length() && !Character.isLetter(a1.charAt(i))) {
                i++;
            }
            // Skip non-letters in the second string
            while (j < b1.length() && !Character.isLetter(b1.charAt(j))) {
                j++;
            }
            // if either string is exhausted, treat the strings as equal
            if (i >= a1.length()) {
                return 0;
            }
            if (j >= b1.length()) {
                return 0;
            }
            // if one character is upper case and the other isn't, the issue is decided
            boolean aUpper = Character.isUpperCase(a1.charAt(i++));
            boolean bUpper = Character.isUpperCase(b1.charAt(j++));
            if (aUpper && !bUpper) {
                return -1;
            }
            if (bUpper && !aUpper) {
                return +1;
            }
        }
    }

    /**
     * Get a collation key for two Strings. The essential property of collation keys
     * is that if two values are equal under the collation, then the collation keys are
     * compare correctly under the equals() method.
     */

    public Object getCollationKey(String s, Platform platform) {
        return baseCollator.getCollationKey(s, platform);
    }

}

//
// The contents of this file are subject to the Mozilla Public License Version 1.0 (the "License");
// you may not use this file except in compliance with the License. You may obtain a copy of the
// License at http://www.mozilla.org/MPL/
//
// Software distributed under the License is distributed on an "AS IS" basis,
// WITHOUT WARRANTY OF ANY KIND, either express or implied.
// See the License for the specific language governing rights and limitations under the License.
//
// The Original Code is: all this file.
//
// The Initial Developer of this module is Michael H. Kay.
//
// Portions created by (your name) are Copyright (C) (your legal entity). All Rights Reserved.
//
// Contributor(s): none.
//