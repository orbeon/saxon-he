package net.sf.saxon.expr.sort;

/**
 * A set of integers represented as int values
 */
public interface IntSet {

    /**
     * Create a copy of this IntSet that leaves the original unchanged.
     * @return an IntSet containing the same integers. The result will not necessarily be the
     * same class as the original. It will either be an immutable object, or a newly constructed
     * object.
     */

    IntSet copy();

    /**
     * Create a copy of this IntSet that contains the same set of integers.
     * @return an IntSet containing the same integers. The result will not necessarily be the
     * same class as the original. It will always be a mutable object
     */

    IntSet mutableCopy();

    /**
     * Clear the contents of the IntSet (making it an empty set)
     */
    void clear();

    /**
     * Get the number of integers in the set
     * @return the size of the set
     */

    int size();

    /**
     * Determine if the set is empty
     * @return true if the set is empty, false if not
     */

    boolean isEmpty();

    /**
     * Determine whether a particular integer is present in the set
     * @param value the integer under test
     * @return true if value is present in the set, false if not
     */

    boolean contains(int value);

    /**
     * Remove an integer from the set
     * @param value the integer to be removed
     * @return true if the integer was present in the set, false if it was not present
     */

    boolean remove(int value);

    /**
     * Add an integer to the set
     * @param value the integer to be added
     * @return true if the integer was added, false if it was already present
     */

    boolean add(int value);

    /**
     * Get an iterator over the values
     * @return an iterator over the integers in the set
     */

    IntIterator iterator();

    /**
     * Form a new set that is the union of this IntSet and another.
     * The result will either be an immutable object, or a newly constructed object.
     * @param other the second set
     * @return the union of the two sets
     */

    IntSet union(IntSet other);

    /**
     * Form a new set that is the intersection of this IntSet and another.
     * The result will either be an immutable object, or a newly constructed object.
     * @param other the second set
     * @return the intersection of the two sets
     */

    IntSet intersect(IntSet other);

    /**
     * Form a new set that is the difference of this set and another set.
     * The result will either be an immutable object, or a newly constructed object.
     * @param other the second set
     * @return the intersection of the two sets
     */

    IntSet except(IntSet other);


    /**
     * Test if this set is a superset of another set
     * @param other the other set
     * @return true if every integer in the other set is also present in this set
     */

    public boolean containsAll(IntSet other);


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