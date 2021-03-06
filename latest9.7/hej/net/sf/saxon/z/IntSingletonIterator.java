////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
// Copyright (c) 2015 Saxonica Limited.
// This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
// If a copy of the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
// This Source Code Form is "Incompatible With Secondary Licenses", as defined by the Mozilla Public License, v. 2.0.
////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

package net.sf.saxon.z;

/**
 * An iterator over a single integer
 */

public class IntSingletonIterator implements IntIterator {

    private int value;
    boolean gone = false;

    public IntSingletonIterator(int value) {
        this.value = value;
    }

    public boolean hasNext() {
        return !gone;
    }

    public int next() {
        gone = true;
        return value;
    }

}