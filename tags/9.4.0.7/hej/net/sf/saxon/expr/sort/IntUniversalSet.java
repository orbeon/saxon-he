package net.sf.saxon.expr.sort;

/**
 * An immutable integer set containing every integer
 */
public class IntUniversalSet implements IntSet {

    private static IntUniversalSet THE_INSTANCE = new IntUniversalSet();

    public static IntUniversalSet getInstance() {
        return THE_INSTANCE;
    }


    private IntUniversalSet() {
        // no action
    }

    public IntSet copy() {
        return this;
    }

    public IntSet mutableCopy() {
        return new IntComplementSet(new IntHashSet());
    }

    public void clear() {
        throw new UnsupportedOperationException("IntUniversalSet is immutable");
    }

    public int size() {
        return Integer.MAX_VALUE;
    }

    public boolean isEmpty() {
        return false;
    }

    public boolean contains(int value) {
        return true;
    }

    public boolean remove(int value) {
        throw new UnsupportedOperationException("IntUniversalSet is immutable");
    }

    public boolean add(int value) {
        throw new UnsupportedOperationException("IntUniversalSet is immutable");
    }

    public IntIterator iterator() {
        throw new UnsupportedOperationException("Cannot enumerate an infinite set");
    }

    public IntSet union(IntSet other) {
        return this;
    }

    public IntSet intersect(IntSet other) {
        return other.copy();
    }

    public IntSet except(IntSet other) {
        if (other instanceof IntUniversalSet) {
            return IntEmptySet.getInstance();
        } else {
            return new IntComplementSet(other.copy());
        }
    }

    public boolean containsAll(/*@NotNull*/ IntSet other) {
        return true;
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


