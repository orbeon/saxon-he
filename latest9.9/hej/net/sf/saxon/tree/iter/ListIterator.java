////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
// Copyright (c) 2018 Saxonica Limited.
// This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
// If a copy of the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
// This Source Code Form is "Incompatible With Secondary Licenses", as defined by the Mozilla Public License, v. 2.0.
////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

package net.sf.saxon.tree.iter;

import net.sf.saxon.expr.LastPositionFinder;
import net.sf.saxon.om.GroundedValue;
import net.sf.saxon.om.Item;
import net.sf.saxon.om.NodeInfo;
import net.sf.saxon.om.SequenceIterator;
import net.sf.saxon.value.AtomicValue;
import net.sf.saxon.value.SequenceExtent;

import java.util.List;

/**
 * Class ListIterator, iterates over a sequence of items held in a Java List
 */

public class ListIterator<T extends Item<?>>
        implements UnfailingIterator<T>, LastPositionFinder, LookaheadIterator<T>, GroundedIterator<T>, ReversibleIterator<T> {

    private int index = 0;
    protected List<T> list;

    /**
     * Create a ListIterator over a given List
     *
     * @param list the list: all objects in the list must be instances of {@link Item}
     */

    public ListIterator(/*@NotNull*/ List<T> list) {
        index = 0;
        this.list = list;
    }

    public boolean hasNext() {
        return index < list.size();
    }

    /*@Nullable*/
    public T next() {
        if (index >= list.size()) {
            return null;
        }
        return list.get(index++);
    }

    public int getLength() {
        return list.size();
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
        return GROUNDED | LAST_POSITION_FINDER | LOOKAHEAD;
    }

    /**
     * Return a Sequence containing all the items in the sequence returned by this
     * SequenceIterator
     *
     * @return the corresponding GroundedValue
     */

    /*@Nullable*/
    public GroundedValue<T> materialize() {
        return SequenceExtent.makeSequenceExtent(list);
    }

    @Override
    public GroundedValue<? extends T> getResidue()  {
        List<? extends T> l2 = list;
        if (index != 0) {
            l2 = l2.subList(index, l2.size());
        }
        return SequenceExtent.makeSequenceExtent(l2);
    }

    public SequenceIterator<T> getReverseIterator() {
        return new ReverseListIterator<>(list);
    }

    public static class Atomic extends ListIterator<AtomicValue> implements AtomicIterator {
        public Atomic (List<AtomicValue> list) {
            super(list);
        }
    }

    public static class OfNodes extends ListIterator<NodeInfo> implements AxisIterator {
        public OfNodes(List<NodeInfo> list) {
            super(list);
        }

        public NodeInfo next() {
            return super.next();
        }

    }
}

