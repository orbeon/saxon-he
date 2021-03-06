////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
// Copyright (c) 2018 Saxonica Limited.
// This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
// If a copy of the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
// This Source Code Form is "Incompatible With Secondary Licenses", as defined by the Mozilla Public License, v. 2.0.
////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

package net.sf.saxon.z;

/**
 * An IntPredicate formed as the union of two other predicates: it matches
 * an integer if either of the operands matches the integer
 */

public class IntUnionPredicate implements java.util.function.IntPredicate {

    private java.util.function.IntPredicate p1;
    private java.util.function.IntPredicate p2;

    public IntUnionPredicate(java.util.function.IntPredicate p1, java.util.function.IntPredicate p2) {
        this.p1 = p1;
        this.p2 = p2;
    }

    /**
     * Ask whether a given value matches this predicate
     *
     * @param value the value to be tested
     * @return true if the value matches; false if it does not
     */
    public boolean test(int value) {
        return p1.test(value) || p2.test(value);
    }

    /**
     * Get the operands
     *
     * @return an array containing the two operands
     */

    public java.util.function.IntPredicate[] getOperands() {
        return new java.util.function.IntPredicate[]{p1, p2};
    }

    @Override
    public String toString() {
        return p1.toString() + "||" + p2.toString();
    }
}
