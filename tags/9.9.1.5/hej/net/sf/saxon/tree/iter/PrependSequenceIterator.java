////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
// Copyright (c) 2018 Saxonica Limited.
// This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
// If a copy of the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
// This Source Code Form is "Incompatible With Secondary Licenses", as defined by the Mozilla Public License, v. 2.0.
////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

package net.sf.saxon.tree.iter;

import net.sf.saxon.om.Item;
import net.sf.saxon.om.SequenceIterator;
import net.sf.saxon.trans.XPathException;

/**
 * An iterator over nodes, that prepends a given node to the nodes
 * returned by another iterator. Used to modify an iterator over axis A
 * to one that iterates over A-OR-SELF.
 */

public class PrependSequenceIterator<T extends Item<?>> implements SequenceIterator<T> {
    T start;
    SequenceIterator<T> base;

    public PrependSequenceIterator(T start, SequenceIterator<T> base) {
        this.start = start;
        this.base = base;
    }


    /**
     * Get the next item in the sequence.
     * @return the next Item. If there are no more items, return null.
     */

    /*@Nullable*/
    public T next() throws XPathException {
        if (start != null) {
            T temp = start;
            start = null;
            return temp;
        } else {
            return base.next();
        }
    }

    public void close() {
        base.close();
    }

    /**
     * Get properties of this iterator, as a bit-significant integer.
     *
     * @return the properties of this iterator. This will be some combination of
     *         properties such as {@link #GROUNDED}, {@link #LAST_POSITION_FINDER},
     *         and {@link #LOOKAHEAD}. It is always
     *         acceptable to return the value zero, indicating that there are no known special properties.
     *         It is acceptable for the properties of the iterator to change depending on its state.
     */

    public int getProperties() {
        return 0;
    }


}

