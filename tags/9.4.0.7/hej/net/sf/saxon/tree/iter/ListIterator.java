package net.sf.saxon.tree.iter;

import net.sf.saxon.expr.LastPositionFinder;
import net.sf.saxon.om.FunctionItem;
import net.sf.saxon.om.GroundedValue;
import net.sf.saxon.om.Item;
import net.sf.saxon.om.NodeInfo;
import net.sf.saxon.value.EmptySequence;
import net.sf.saxon.value.SequenceExtent;
import net.sf.saxon.value.SingletonItem;

import java.util.List;

/**
 * Class ListIterator, iterates over a sequence of items held in a Java List
*/

public class ListIterator<T extends Item>
        implements UnfailingIterator<T>, LastPositionFinder<T>, LookaheadIterator<T>, GroundedIterator<T> {

    int index=0;
    int length;
    /*@Nullable*/ T current = null;
    /*@NotNull*/ List<T> list;

    /**
     * Create a ListIterator over a given List
     * @param list the list: all objects in the list must be instances of {@link Item}
     */

    public ListIterator(/*@NotNull*/ List<T> list) {
        index = 0;
        this.list = list;
        this.length = list.size();
    }

   /**
     * Create a ListIterator over the leading part of a given List
     * @param list the list: all objects in the list must be instances of {@link Item}
     * @param length the number of items to be included
     */

    public ListIterator(List<T> list, int length) {
        index = 0;
        this.list = list;
        this.length = length;
    }

    public boolean hasNext() {
        return index<length;
    }

    /*@Nullable*/ public T next() {
        if (index >= length) {
            current = null;
            index = -1;
            length = -1;
            return null;
        }
        current = list.get(index++);
        return current;
    }

    /*@Nullable*/ public T current() {
        return current;
    }

    public int position() {
        return index;
    }

    public void close() {
    }

    public int getLength() {
        return length;
    }

    /*@NotNull*/
    public ListIterator<T> getAnother() {
        return new ListIterator<T>(list);
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
     * Return a SequenceValue containing all the items in the sequence returned by this
     * SequenceIterator
     *
     * @return the corresponding SequenceValue
     */

    /*@Nullable*/ public GroundedValue<T> materialize() {
        if (length == 0) {
            return EmptySequence.getInstance();
        } else if (length == 1) {
            T item = list.get(0);
            if (item instanceof NodeInfo || item instanceof FunctionItem) {
                return new SingletonItem<T>(item);
            } else /*if (item instanceof AtomicValue)*/ {
                return (GroundedValue<T>)item;
            }
        } else {
            return new SequenceExtent<T>(list);
        }
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