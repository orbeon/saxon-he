////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
// Copyright (c) 2012 Michael Froh.
// This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
// If a copy of the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
// This Source Code Form is "Incompatible With Secondary Licenses", as defined by the Mozilla Public License, v. 2.0.
////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

package net.sf.saxon.ma.trie;

import java.util.Collections;
import java.util.Iterator;
import java.util.NoSuchElementException;


public abstract class Option<T> implements Iterable<T> {
    public abstract T get();

    public abstract boolean isDefined();

    public final T getOrElse(T defaultVal) {
        return isDefined() ? get() : defaultVal;
    }

    public Iterator<T> iterator() {
        return isDefined() ? Collections.singleton(get()).iterator() :
                Collections.<T>emptySet().iterator();
    }

    public static <T> Option<T> option(T value) {
        if (value == null) {
            return none();
        }
        return some(value);
    }

    // Factory method to return the singleton None instance
    @SuppressWarnings({"unchecked"})
    public static <T> Option<T> none() {
        return NONE;
    }

    // Factory method to return a non-empty Some instance
    public static <T> Option<T> some(final T value) {
        return new Some<T>(value);
    }

    private static None NONE = new None();

    private static class None extends Option {

        @Override
        public Object get() {
            throw new NoSuchElementException("get() called on None");
        }

        @Override
        public boolean isDefined() {
            return false;
        }

        @Override
        public String toString() {
            return "None";
        }
    }

    private static class Some<T> extends Option<T> {
        private final T value;

        public Some(final T value) {
            this.value = value;
        }

        @Override
        public T get() {
            return value;
        }

        @Override
        public boolean isDefined() {
            return true;
        }

        @Override
        public String toString() {
            return "Some(" + value + ")";
        }

        public boolean equals(Object other) {
            return other instanceof Some && ((Some) other).value.equals(value);
        }
    }
}
