////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
// Copyright (c) 2017 Saxonica Limited.
// This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
// If a copy of the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
// This Source Code Form is "Incompatible With Secondary Licenses", as defined by the Mozilla Public License, v. 2.0.
////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

package net.sf.saxon.z;

/**
 * An IntPredicate that matches every integer, unconditionally
 */

public class IntPredicateTrue implements IntPredicate {

    public static IntPredicateTrue getInstance() {
        return THE_INSTANCE;
    }

    private static IntPredicateTrue THE_INSTANCE = new IntPredicateTrue();

    public IntPredicateTrue() {
    }

    /**
     * Ask whether a given value matches this predicate
     *
     * @param value the value to be tested
     * @return true if the value matches; false if it does not
     */
    public boolean matches(int value) {
        return true;
    }

}
