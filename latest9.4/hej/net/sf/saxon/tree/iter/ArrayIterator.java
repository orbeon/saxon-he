package net.sf.saxon.tree.iter;

import net.sf.saxon.expr.LastPositionFinder;
import net.sf.saxon.om.GroundedValue;
import net.sf.saxon.om.Item;
import net.sf.saxon.om.SequenceIterator;
import net.sf.saxon.value.SequenceExtent;

/**
 * ArrayIterator is used to enumerate items held in an array.
 * The items are always held in the correct sorted order for the sequence.
 *
 * @author Michael H. Kay
 */


public class ArrayIterator<T extends Item> implements UnfailingIterator<T>,
        ReversibleIterator<T>, LastPositionFinder<T>, LookaheadIterator<T>, GroundedIterator<T> {

    protected T[] items;
    private int index;          // position in array of current item, zero-based
                                // set equal to end+1 when all the items required have been read.
    protected int start;          // position of first item to be returned, zero-based
    protected int end;            // position of first item that is NOT returned, zero-based
    /*@Nullable*/ private T current = null;

    /**
     * Create an iterator over all the items in an array
     *
     * @param nodes the array (of any items, not necessarily nodes) to be
     *     processed by the iterator
     */

    public ArrayIterator(/*@NotNull*/ T[] nodes) {
        items = nodes;
        start = 0;
        end = nodes.length;
        index = 0;
    }

    /**
     * Create an iterator over a range of an array. Note that the start position is zero-based
     *
     * @param items the array (of nodes or simple values) to be processed by
     *     the iterator
     * @param start the position of the first item to be processed
     *     (numbering from zero). Must be between zero and nodes.length-1; if not,
     *     undefined exceptions are likely to occur.
     * @param end position of first item that is NOT returned, zero-based. Must be
     *     beween 1 and nodes.length; if not, undefined exceptions are likely to occur.
     */

    public ArrayIterator(T[] items, int start, int end) {
        this.items = items;
        this.end = end;
        this.start = start;
        index = start;
    }

    /**
     * Create a new ArrayIterator over the same items,
     * with a different start point and end point
     * @param min the start position (1-based) of the new ArrayIterator
     * relative to the original
     * @param max the end position (1-based) of the last item to be delivered
     * by the new ArrayIterator, relative to the original. For example, min=2, max=3
     * delivers the two items ($base[2], $base[3]). Set this to Integer.MAX_VALUE if
     * there is no end limit.
     * @return an iterator over the items between the min and max positions
     */

    public SequenceIterator<T> makeSliceIterator(int min, int max) {
        T[] items = getArray();
        int currentStart = getStartPosition();
        int currentEnd = getEndPosition();
        if (min < 1) {
            min = 1;
        }
        int newStart = currentStart + (min-1);
        if (newStart < currentStart) {
            newStart = currentStart;
        }
        int newEnd = (max == Integer.MAX_VALUE ? currentEnd : newStart + (max - min + 1));
        if (newEnd > currentEnd) {
            newEnd = currentEnd;
        }
        if (newEnd <= newStart) {
            return EmptyIterator.emptyIterator();
        }
        return new ArrayIterator<T>(items, newStart, newEnd);
    }

    /**
     * Test whether there are any more items
     * @return true if there are more items
     */

    public boolean hasNext() {
        return index < end;
    }

    /**
     * Get the next item in the array
     * @return the next item in the array
     */

    /*@Nullable*/ public T next() {
        if (index >= end) {
            index = end+1;
            current = null;
            return null;
        }
        current = items[index++];
        return current;
    }

    /**
     * Get the current item in the array
     *
     * @return the item returned by the most recent call of next()
     */
    /*@Nullable*/ public T current() {
        return current;
    }

    /**
     * Get the position of the current item in the array
     *
     * @return the current position (starting at 1 for the first item)
     */
    public int position() {
        if (index > end) {
            return -1;
        }
        return index - start;
    }

    /**
     * Get the number of items in the part of the array being processed
     *
     * @return the number of items; equivalently, the position of the last
     *     item
     */
    public int getLength() {
        return end - start;
    }

    public void close() {

    }

    /**
     * Get another iterator over the same items
     *
     * @return a new ArrayIterator
     */
    /*@NotNull*/
    public ArrayIterator<T> getAnother() {
        return new ArrayIterator<T>(items, start, end);
    }

    /**
     * Get an iterator that processes the same items in reverse order
     *
     * @return a new ArrayIterator
     */
    public SequenceIterator<T> getReverseIterator() {
        return new ReverseArrayIterator<T>(items, start, end);
    }

    /**
     * Indicate that any nodes returned in the sequence will be atomized. This
     * means that if it wishes to do so, the implementation can return the typed
     * values of the nodes rather than the nodes themselves. The implementation
     * is free to ignore this hint.
     * @param atomizing true if the caller of this iterator will atomize any
     * nodes that are returned, and is therefore willing to accept the typed
     * value of the nodes instead of the nodes themselves.
     */

    //public void setIsAtomizing(boolean atomizing) {}

    /**
     * Get the underlying array
     *
     * @return the underlying array being processed by the iterator
     */

    public T[] getArray() {
        return items;
    }

    /**
     * Get the initial start position
     *
     * @return the start position of the iterator in the array (zero-based)
     */

    public int getStartPosition() {
        return start;
    }

    /**
     * Get the end position in the array
     *
     * @return the position in the array (zero-based) of the first item not included
     * in the iteration
     */

    public int getEndPosition() {
        return end;
    }

    /**
     * Return a SequenceValue containing all the items in the sequence returned by this
     * SequenceIterator
     *
     * @return the corresponding SequenceValue
     */

    /*@NotNull*/ public GroundedValue materialize() {
        if (start==0 && end == items.length) {
            return new SequenceExtent<T>(items);
        } else {
            SequenceExtent e = new SequenceExtent<T>(items);
            return new SequenceExtent<T>(e, start, end-start);
        }
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