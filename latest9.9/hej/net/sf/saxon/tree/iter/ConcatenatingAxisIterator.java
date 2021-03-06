////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
// Copyright (c) 2018 Saxonica Limited.
// This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
// If a copy of the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
// This Source Code Form is "Incompatible With Secondary Licenses", as defined by the Mozilla Public License, v. 2.0.
////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

package net.sf.saxon.tree.iter;

import net.sf.saxon.om.NodeInfo;

/**
 * An iterator over nodes, that concatenates the nodes returned by two supplied iterators.
 */

public class ConcatenatingAxisIterator implements AxisIterator {

    AxisIterator first;
    AxisIterator second;
    AxisIterator active;

    public ConcatenatingAxisIterator(AxisIterator first, AxisIterator second) {
        this.first = first;
        this.second = second;
        this.active = first;
    }

    public static AxisIterator makeConcatenatingIterator(AxisIterator first, AxisIterator second) {
        if (first instanceof EmptyIterator.OfNodes) {
            return second;
        }
        if (second instanceof EmptyIterator.OfNodes) {
            return first;
        }
        return new ConcatenatingAxisIterator(first, second);
    }


    /**
     * Get the next item in the sequence. <BR>
     *
     * @return the next Item. If there are no more nodes, return null.
     */

    /*@Nullable*/
    public NodeInfo next() {
        NodeInfo n = active.next();
        if (n == null && active == first) {
            active = second;
            n = second.next();
        }
        return n;
    }

    public void close() {
        first.close();
        second.close();
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

