package net.sf.saxon.expr.sort;

import net.sf.saxon.lib.StringCollator;


/**
 * A StringCollator that sorts lowercase before uppercase, or vice versa.
 *
 * <p>Case is irrelevant, unless the strings are equal ignoring
 * case, in which case lowercase comes first.</p>
 *
 * @author Michael H. Kay
 */

public class CaseFirstCollator implements StringCollator, java.io.Serializable {

    private StringCollator baseCollator;
    private boolean upperFirst;

    /**
     * Create a CaseFirstCollator
     * @param base the base collator, which determines how characters are sorted irrespective of case
     * @param upperFirst true if uppercase precedes lowercase, false otherwise
     */

    public CaseFirstCollator(StringCollator base, boolean upperFirst) {
        this.baseCollator = base;
        this.upperFirst = upperFirst;
    }

    /**
     * Compare two string objects: case is irrelevant, unless the strings are equal ignoring
     * case, in which case lowercase comes first.
     *
     * @return <0 if a<b, 0 if a=b, >0 if a>b
     * @throws ClassCastException if the objects are of the wrong type for this Comparer
     */

    public int compareStrings(String a, String b) {
        int diff = baseCollator.compareStrings(a, b);
        if (diff != 0) {
            return diff;
        }

        // This is doing a character-by-character comparison, which isn't really right.
        // There might be a sequence of letters constituting a single collation unit.

        int i = 0;
        int j = 0;
        while (true) {
            // Skip characters that are equal in the two strings
            while (i < a.length() && j < b.length() && a.charAt(i) == b.charAt(j)) {
                i++;
                j++;
            }
            // Skip non-letters in the first string
            while (i < a.length() && !Character.isLetter(a.charAt(i))) {
                i++;
            }
            // Skip non-letters in the second string
            while (j < b.length() && !Character.isLetter(b.charAt(j))) {
                j++;
            }
            // If we've got to the end of either string, treat the strings as equal
            if (i >= a.length()) {
                return 0;
            }
            if (j >= b.length()) {
                return 0;
            }
            // If one of the characters is upper/lower case and the other isn't, the issue is decided
            boolean aFirst = (upperFirst ? Character.isUpperCase(a.charAt(i++)) : Character.isLowerCase(a.charAt(i++)));
            boolean bFirst = (upperFirst ? Character.isUpperCase(b.charAt(j++)) : Character.isLowerCase(b.charAt(j++)));
            if (aFirst && !bFirst) {
                return -1;
            }
            if (bFirst && !aFirst) {
                return +1;
            }
        }
    }

    /**
     * Compare two strings for equality. This may be more efficient than using compareStrings and
     * testing whether the result is zero, but it must give the same result
     * @param s1 the first string
     * @param s2 the second string
     * @return true if and only if the strings are considered equal,
     */

    public boolean comparesEqual(String s1, /*@NotNull*/ String s2) {
        return compareStrings(s1, s2) == 0;
    }

    /**
     * Get a collation key for two Strings. The essential property of collation keys
     * is that if two values are equal under the collation, then the collation keys are
     * compare correctly under the equals() method.
     */

    public Object getCollationKey(String s) {
        return baseCollator.getCollationKey(s);
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
// The Original Code is: all this file
//
// The Initial Developer of the Original Code is Saxonica Limited.
// Portions created by ___ are Copyright (C) ___. All rights reserved.
//
// Contributor(s):
//