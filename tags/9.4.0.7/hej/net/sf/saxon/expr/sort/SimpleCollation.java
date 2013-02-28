package net.sf.saxon.expr.sort;

import net.sf.saxon.Configuration;
import net.sf.saxon.Platform;
import net.sf.saxon.lib.StringCollator;

import java.util.Comparator;

/**
 * A simple collation that just wraps a suppled Comparator
 */

public class SimpleCollation implements StringCollator {

    private Comparator collation;
    /*@NotNull*/ private static Platform platform = Configuration.getPlatform();

    /**
     * Create a SimpleCollation
     * @param collation the Comparator that does the actual string comparison
     */

    public SimpleCollation(Comparator collation) {
         this.collation = collation;
    }

     /**
     * Compares its two arguments for order.  Returns a negative integer,
     * zero, or a positive integer as the first argument is less than, equal
     * to, or greater than the second.<p>
     * <p/>

     * @param o1 the first object to be compared.
     * @param o2 the second object to be compared.
     * @return a negative integer, zero, or a positive integer as the
     *         first argument is less than, equal to, or greater than the
     *         second.
     * @throws ClassCastException if the arguments' types prevent them from
     *                            being compared by this Comparator.
     */
    public int compareStrings(String o1, String o2) {
        return collation.compare(o1, o2);
    }

    /**
     * Compare two strings for equality. This may be more efficient than using compareStrings and
     * testing whether the result is zero, but it must give the same result
     * @param s1 the first string
     * @param s2 the second string
     * @return true if and only if the strings are considered equal,
     */

    public boolean comparesEqual(String s1, String s2) {
        return collation.compare(s1, s2) == 0;
    }

    /**
     * Get the underlying comparator
     * @return the underlying comparator
     */

    public Comparator getCollation() {
        return collation;
    }

    /**
     * Set the underlying comparator
     * @param collation the underlying comparator
     */

    public void setCollation(Comparator collation) {
        this.collation = collation;
    }

    /**
     * Get a collation key for two Strings. The essential property of collation keys
     * is that if two values are equal under the collation, then the collation keys are
     * compare correctly under the equals() method.
     */

    public Object getCollationKey(String s) {
        return platform.getCollationKey(this, s);
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