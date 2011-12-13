package net.sf.saxon.expr.sort;

/**
 * An immutable integer set containing no integers
 */
public class IntEmptySet implements IntSet {

    private static IntEmptySet THE_INSTANCE = new IntEmptySet();

    public static IntEmptySet getInstance() {
        return THE_INSTANCE;
    }


    private IntEmptySet() {
        // no action
    }

    public IntSet copy() {
        return this;
    }

    public IntSet mutableCopy() {
        return new IntHashSet();
    }

    public void clear() {
        throw new UnsupportedOperationException("IntEmptySet is immutable");
    }

    public int size() {
        return 0;
    }

    public boolean isEmpty() {
        return true;
    }

    public boolean contains(int value) {
        return false;
    }

    public boolean remove(int value) {
        throw new UnsupportedOperationException("IntEmptySet is immutable");
    }

    public boolean add(int value) {
        throw new UnsupportedOperationException("IntEmptySet is immutable");
    }

    public IntIterator iterator() {
        return new IntIterator() {
            public boolean hasNext() {
                return false;
            }

            public int next() {
                return Integer.MIN_VALUE;
            }
        };
    }

    public IntSet union(IntSet other) {
        return other.copy();
    }

    public IntSet intersect(IntSet other) {
        return this;
    }

    public IntSet except(IntSet other) {
        return this;
    }

    public boolean containsAll(/*@NotNull*/ IntSet other) {
        return other.isEmpty();
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



