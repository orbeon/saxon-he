package net.sf.saxon.expr.sort;

/**
 * An immutable integer set containing all int values except those in an excluded set
 */
public class IntComplementSet implements IntSet {

    private IntSet exclusions;

    public IntComplementSet(IntSet exclusions) {
        this.exclusions = exclusions.copy();
    }

    public IntSet getExclusions() {
        return exclusions;
    }

    public IntSet copy() {
        return new IntComplementSet(exclusions);
    }

    public IntSet mutableCopy() {
        return copy();
    }

    public void clear() {
        throw new UnsupportedOperationException("IntComplementSet cannot be emptied");
    }

    public int size() {
        return Integer.MAX_VALUE - exclusions.size();
    }

    public boolean isEmpty() {
        return size() != 0;
    }

    public boolean contains(int value) {
        return !exclusions.contains(value);
    }

    public boolean remove(int value) {
        boolean b = contains(value);
        if (b) {
            exclusions.add(value);
        }
        return b;
    }

    public boolean add(int value) {
        boolean b = contains(value);
        if (!b) {
            exclusions.remove(value);
        }
        return b;
    }

    public IntIterator iterator() {
        throw new UnsupportedOperationException("Cannot enumerate an infinite set");
    }

    public IntSet union(IntSet other) {
        return new IntComplementSet(exclusions.except(other));
    }

    public IntSet intersect(IntSet other) {
        if (other.isEmpty()) {
            return IntEmptySet.getInstance();
        } else if (other == IntUniversalSet.getInstance()) {
            return copy();
        } else if (other instanceof IntComplementSet) {
            return new IntComplementSet(exclusions.union(((IntComplementSet)other).exclusions));
        } else {
            return other.intersect(this);
        }
    }

    public IntSet except(IntSet other) {
        return new IntComplementSet(exclusions.union(other));
    }

    public boolean containsAll(/*@NotNull*/ IntSet other) {
        if (other.size() > 1) {
            return false;
        }
        IntIterator ii = other.iterator();
        while (ii.hasNext()) {
            if (exclusions.contains(ii.next())) {
                return false;
            }
        }
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



