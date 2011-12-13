package net.sf.saxon.expr.sort;

/**
 * Abstract superclass containing helper methods for various implementations of IntSet
 */
public abstract class AbstractIntSet implements IntSet {

    /**
     * Test if this set is a superset of another set
     * @param other the other set
     * @return true if every item in the other set is also in this set
     */

    public boolean containsAll(IntSet other) {
        if (other == IntUniversalSet.getInstance() || (other instanceof IntComplementSet)) {
            return false;
        }
        IntIterator it = other.iterator();
        while (it.hasNext()) {
            if (!contains(it.next())) {
                return false;
            }
        }
        return true;
    }

    /**
     * Form a new set that is the union of two IntSets.
     * @param other the second set
     * @return the union of the two sets
     */

    public IntSet union(IntSet other) {
        if (other == IntUniversalSet.getInstance()) {
            return other;
        }
        if (this.isEmpty()) {
            return other.copy();
        }
        if (other.isEmpty()) {
            return this.copy();
        }
        if (other instanceof IntComplementSet) {
            return other.union(this);
        }
        IntHashSet n = new IntHashSet(this.size() + other.size());
        IntIterator it = iterator();
        while (it.hasNext()) {
            n.add(it.next());
        }
        it = other.iterator();
        while (it.hasNext()) {
            n.add(it.next());
        }
        return n;
    }

    /**
     * Form a new set that is the intersection of two IntSets.
     * @param other the second set
     * @return the intersection of the two sets
     */

    public IntSet intersect(IntSet other) {
        if (this.isEmpty() || other.isEmpty()) {
            return IntEmptySet.getInstance();
        }
        IntHashSet n = new IntHashSet(size());
        IntIterator it = iterator();
        while (it.hasNext()) {
            int v = it.next();
            if (other.contains(v)) {
                n.add(v);
            }
        }
        return n;
    }

    /**
     * Form a new set that is the difference of this set and another set.
     * The result will either be an immutable object, or a newly constructed object.
     * @param other the second set
     * @return the intersection of the two sets
     */


    public IntSet except(IntSet other) {
        IntHashSet n = new IntHashSet(size());
        IntIterator it = iterator();
        while (it.hasNext()) {
            int v = it.next();
            if (!other.contains(v)) {
                n.add(v);
            }
        }
        return n;
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
