////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
// Copyright (c) 2015 Saxonica Limited.
// This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
// If a copy of the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
// This Source Code Form is “Incompatible With Secondary Licenses”, as defined by the Mozilla Public License, v. 2.0.
////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

package net.sf.saxon.testdriver;

import net.sf.saxon.s9api.XdmNode;

/**
 * Represents an assertion about the results of a test
 */
public interface ResultAssertion {

    /**
     * Determine whether the outcome of a test matches the assertion
     *
     * @param assertion the assertion element in the test catalog
     * @param outcome   the actual outcome of the test
     * @param debug     if set to true, and the assertion is not satisfied, print explanation to System.err
     * @return true if the assertion is satisfied
     */

    public int test(XdmNode assertion, TestOutcome outcome, boolean debug);

}


