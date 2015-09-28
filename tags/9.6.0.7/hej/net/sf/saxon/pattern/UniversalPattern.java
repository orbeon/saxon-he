////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
// Copyright (c) 2014 Saxonica Limited.
// This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
// If a copy of the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
// This Source Code Form is "Incompatible With Secondary Licenses", as defined by the Mozilla Public License, v. 2.0.
////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

package net.sf.saxon.pattern;

import net.sf.saxon.expr.XPathContext;
import net.sf.saxon.om.Item;
import net.sf.saxon.type.AnyItemType;
import net.sf.saxon.type.ItemType;

/**
 * The UniversalPattern matches everything
 *
 * @author Michael H. Kay
 */

public class UniversalPattern extends Pattern {

    private static final UniversalPattern THE_INSTANCE = new UniversalPattern();

    public static UniversalPattern getInstance() {
        return THE_INSTANCE;
    }

    /**
     * Create an UniversalPattern that matches all items
     */

    private UniversalPattern() {
        setPriority(-1);
    }

    /**
     * Determine whether this Pattern matches the given Node. This is the main external interface
     * for matching patterns: it sets current() to the node being tested
     *
     * @param item    The item to be tested against the Pattern
     * @param context The context in which the match is to take place.
     * @return true if the item matches the Pattern, false otherwise
     */

    public boolean matches(Item item, XPathContext context) {
        return true;
    }

    /**
     * Get a NodeTest that all the nodes matching this pattern must satisfy
     */

    public ItemType getItemType() {
        return AnyItemType.getInstance();
    }

    /**
     * Determine the name fingerprint of nodes to which this pattern applies. Used for
     * optimisation.
     *
     * @return A fingerprint that the nodes must match, or -1 if it can match multiple fingerprints
     */

    public int getFingerprint() {
        return -1;
    }

    /**
     * Display the pattern for diagnostics
     */

    public String toString() {
        return ".";
    }

    /**
     * Determine whether this pattern is the same as another pattern
     *
     * @param other the other object
     */

    public boolean equals(/*@NotNull*/ Object other) {
        return (other instanceof UniversalPattern);
    }

    /**
     * Hashcode supporting equals()
     */

    public int hashCode() {
        return 0x7aeccea8;
    }


}

