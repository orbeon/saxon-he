////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
// Copyright (c) 2016 Saxonica Limited.
// This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
// If a copy of the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
// This Source Code Form is "Incompatible With Secondary Licenses", as defined by the Mozilla Public License, v. 2.0.
////////////////////////////////////////////////////////////////////////////////////////////////////////////////////


package net.sf.saxon.expr.parser;

/**
 * A wrapper class for use when we want to compare objects for Java identity even though
 * they have an equals method; we can put wrapped objects into a Map or Set and they
 * will then be compared using object identity rather than using their equals() method.
 * @param <C> the type of the wrapped object
 */
public class IdentityWrapper<C extends Object> {

    private C payload;

    public IdentityWrapper(C payload) {
        this.payload = payload;
    }

    @Override
    public boolean equals(Object obj) {
        return obj instanceof IdentityWrapper && ((IdentityWrapper)obj).payload == payload;
    }

    @Override
    public int hashCode() {
        return System.identityHashCode(payload);
    }
}

