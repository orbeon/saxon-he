////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
// Copyright (c) 2015 Saxonica Limited.
// This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
// If a copy of the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
// This Source Code Form is "Incompatible With Secondary Licenses", as defined by the Mozilla Public License, v. 2.0.
////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

package net.sf.saxon.tree.iter;

import net.sf.saxon.om.Item;

import java.util.Iterator;

/**
 * A SequenceIterator that wraps a Java Iterator. This is an abstract class, because the Java
 * iterator does not hold enough information to support the getAnother() method, needed to
 * implement the XPath last() function
 */

public abstract class IteratorIterator
        implements LookaheadIterator, UnfailingIterator {

    private Iterator<? extends Item> base;

    /**
     * Create a SequenceIterator over a given iterator
     *
     * @param base the base Iterator
     */

    public IteratorIterator(Iterator<? extends Item> base) {
        this.base = base;
    }


    public boolean hasNext() {
        return base.hasNext();
    }

    /*@Nullable*/
    public Item next() {
        Item current;
        if (base.hasNext()) {
            current = base.next();
        } else {
            current = null;
        }
        return current;
    }

    public void close() {
    }

    /*@NotNull*/
    public abstract UnfailingIterator getAnother();

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
        return LOOKAHEAD;
    }

}

