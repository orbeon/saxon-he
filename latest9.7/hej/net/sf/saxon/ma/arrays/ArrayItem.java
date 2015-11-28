////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
// Copyright (c) 2015 Saxonica Limited.
// This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
// If a copy of the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
// This Source Code Form is “Incompatible With Secondary Licenses”, as defined by the Mozilla Public License, v. 2.0.
////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

package net.sf.saxon.ma.arrays;

import net.sf.saxon.expr.StaticProperty;
import net.sf.saxon.om.Function;
import net.sf.saxon.om.Sequence;
import net.sf.saxon.trans.XPathException;
import net.sf.saxon.value.SequenceType;

import java.util.Iterator;
import java.util.List;

/**
 * Interface supported by different implementations of an XDM array item
 */
public interface ArrayItem extends Function, Iterable<Sequence> {

    SequenceType SINGLE_ARRAY_TYPE =
            SequenceType.makeSequenceType(ArrayItemType.ANY_ARRAY_TYPE, StaticProperty.EXACTLY_ONE);

    /**
     * Get a member of the array
     *
     * @param index the position of the member to retrieve (zero-based)
     * @return the value at the given position.
     * @throws XPathException if the index is out of range
     */

    Sequence get(int index) throws XPathException;

    /**
     * Get the size of the array
     *
     * @return the number of members in this array
     */

    int size();

    /**
     * Ask whether the array is empty
     *
     * @return true if and only if the size of the array is zero
     */

    boolean isEmpty();

    /**
     * Get the list of all members of the array
     * @return an iterator over the members of the array
     */

    Iterator<Sequence> iterator();

    /**
     * Concatenate this array with another
     * @param other the second array
     * @return the concatenation of the two arrays; that is, an array
     * containing first the members of this array, and then the members of the other array
     */

    ArrayItem concat(ArrayItem other);

    /**
     * Remove a member from the array
     *
     *
     * @param index  the position of the member to be removed (zero-based)
     * @return a new array in which the requested member has been removed
     * @throws net.sf.saxon.trans.XPathException if the index is out of range
     */

    ArrayItem remove(int index) throws XPathException;

    /**
     * Get the lowest common item type of the members of the array
     *
     * @return the most specific type to which all the members belong.
     */

    SequenceType getMemberType();

    /**
     * Get a list of the members of the array
     *
     * @return the list of members.
     */

    List<Sequence> getMembers();


}

// Copyright (c) 2014 Saxonica Limited. All rights reserved.
