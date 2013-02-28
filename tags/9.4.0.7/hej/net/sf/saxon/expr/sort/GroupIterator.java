package net.sf.saxon.expr.sort;

import net.sf.saxon.om.Item;
import net.sf.saxon.om.SequenceIterator;
import net.sf.saxon.trans.XPathException;
import net.sf.saxon.value.Value;

/**
 * A GroupIterator is an iterator that iterates over a sequence of groups.
 * The normal methods such as next() and current() always deliver the leading item
 * of the group. Additional methods are available to get the grouping key for the
 * current group (only applicable to group-by and group-adjacent), and to get all the
 * members of the current group.
 */

public interface GroupIterator extends SequenceIterator {

    /**
     * Get the grouping key of the current group
     * @return the current grouping key in the case of group-by or group-adjacent,
     * or null in the case of group-starting-with and group-ending-with
     */

    /*@Nullable*/ public Value getCurrentGroupingKey();

    /**
     * Get an iterator over the members of the current group, in population
     * order. This must always be a clean iterator, that is, an iterator that
     * starts at the first item of the group.
     * @return an iterator over all the members of the current group, in population
     * order.
     * @throws net.sf.saxon.trans.XPathException if a dynamic error occurs
     */

    public SequenceIterator<? extends Item> iterateCurrentGroup() throws XPathException;

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