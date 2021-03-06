////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
// Copyright (c) 2014 Saxonica Limited.
// This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
// If a copy of the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
// This Source Code Form is "Incompatible With Secondary Licenses", as defined by the Mozilla Public License, v. 2.0.
////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

package net.sf.saxon.om;

import net.sf.saxon.expr.LastPositionFinder;
import net.sf.saxon.functions.Count;
import net.sf.saxon.trans.XPathException;
import net.sf.saxon.tree.iter.LookaheadIterator;

/**
 * An iterator that maintains the values of position() and current(), as a wrapper
 * over an iterator which does not maintain these values itself.
 *
 * <p>Note that when a FocusTrackingIterator is used to wrap a SequenceIterator
 * in order to track the values of position() and current(), it is important to ensure
 * (a) that the SequenceIterator is initially positioned at the start of the sequence,
 * and (b) that all calls on next() to advance the iterator are directed at the
 * FocusTrackingIterator, and not at the wrapped SequenceIterator.</p>
 *
 * @since 9.6
 */
public class FocusTrackingIterator implements FocusIterator, LookaheadIterator, LastPositionFinder {

    private SequenceIterator base;
    private Item curr;
    private int pos = 0;
    private int last = -1;

    public FocusTrackingIterator(SequenceIterator base) {
        this.base = base;
    }

    /**
     * Get the underlying iterator
     * @return the iterator underlying this FocusIterator
     */

    public SequenceIterator getUnderlyingIterator() {
        return base;
    }

    /**
     * Get the next item in the sequence. This method changes the state of the
     * iterator, in particular it affects the result of subsequent calls of
     * position() and current().
     *
     * @return the next item, or null if there are no more items. Once a call
     *         on next() has returned null, no further calls should be made. The preferred
     *         action for an iterator if subsequent calls on next() are made is to return
     *         null again, and all implementations within Saxon follow this rule.
     * @throws XPathException
     *          if an error occurs retrieving the next item
     * @since 8.4
     */
    public Item next() throws XPathException {
        curr = base.next();
        if (curr == null) {
            pos = -1;
        } else {
            pos++;
        }
        return curr;
    }

    /**
     * Get the current value in the sequence (the one returned by the
     * most recent call on next()). This will be null before the first
     * call of next(). This method does not change the state of the iterator.
     *
     * @return the current item, the one most recently returned by a call on
     *         next(). Returns null if next() has not been called, or if the end
     *         of the sequence has been reached.
     * @since 8.4
     */
    public Item current() {
        return curr;
    }

    /**
     * Get the current position. This will usually be zero before the first call
     * on next(), otherwise it will be the number of times that next() has
     * been called. Once next() has returned null, the preferred action is
     * for subsequent calls on position() to return -1, but not all existing
     * implementations follow this practice. (In particular, the EmptyIterator
     * is stateless, and always returns 0 as the value of position(), whether
     * or not next() has been called.)
     * <p/>
     * This method does not change the state of the iterator.
     *
     * @return the current position, the position of the item returned by the
     *         most recent call of next(). This is 1 after next() has been successfully
     *         called once, 2 after it has been called twice, and so on. If next() has
     *         never been called, the method returns zero. If the end of the sequence
     *         has been reached, the value returned will always be <= 0; the preferred
     *         value is -1.
     * @since 8.4
     */
    public int position() {
        return pos;
    }

    /**
     * Get the position of the last item in the sequence
     * @return the position of the last item
     * @throws XPathException if a failure occurs reading the sequence
     */

    public int getLength() throws XPathException {
        if (last == -1) {
            if (base instanceof LastPositionFinder) {
                last = ((LastPositionFinder)base).getLength();
            }
            if (last == -1) {
                last = Count.count(base.getAnother());
            }
        }
        return last;
    }

    /**
     * Get another SequenceIterator that iterates over the same items as the original,
     * but which is repositioned at the start of the sequence.
     * <p/>
     * This method allows access to all the items in the sequence without disturbing the
     * current position of the iterator. Internally, its main use is in evaluating the last()
     * function.
     * <p/>
     * This method does not change the state of the iterator.
     *
     * @return a SequenceIterator that iterates over the same items,
     *         positioned before the first item
     * @throws XPathException
     *          if any error occurs
     * @since 8.4
     */
    public FocusTrackingIterator getAnother() throws XPathException {
        return new FocusTrackingIterator(base.getAnother());
    }

    /**
     * Determine whether there are more items to come. Note that this operation
     * is stateless and it is not necessary (or usual) to call it before calling
     * next(). It is used only when there is an explicit need to tell if we
     * are at the last element.
     * <p/>
     * This method must not be called unless the result of getProperties() on the iterator
     * includes the bit setting {@link net.sf.saxon.om.SequenceIterator#LOOKAHEAD}
     *
     * @return true if there are more items in the sequence
     */
    public boolean hasNext() {
        return ((LookaheadIterator)base).hasNext();
    }

    /**
     * Close the iterator. This indicates to the supplier of the data that the client
     * does not require any more items to be delivered by the iterator. This may enable the
     * supplier to release resources. After calling close(), no further calls on the
     * iterator should be made; if further calls are made, the effect of such calls is undefined.
     * <p/>
     * <p>(Currently, closing an iterator is important only when the data is being "pushed" in
     * another thread. Closing the iterator terminates that thread and means that it needs to do
     * no additional work. Indeed, failing to close the iterator may cause the push thread to hang
     * waiting for the buffer to be emptied.)</p>
     *
     * @since 9.1
     */
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
     * @since 8.6
     */
    public int getProperties() {
        return base.getProperties();
    }
}