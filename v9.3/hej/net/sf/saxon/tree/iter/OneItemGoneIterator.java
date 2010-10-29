package net.sf.saxon.tree.iter;

import net.sf.saxon.expr.LastPositionFinder;
import net.sf.saxon.om.Item;
import net.sf.saxon.om.SequenceIterator;
import net.sf.saxon.om.GroundedValue;
import net.sf.saxon.trans.XPathException;

/**
 * This is an iterator over a sequence whose first item has already been read. On entry, the baseIterator
 * must be positioned so the second item in the sequence is the next item to be returned; the first item
 * in the sequence is available by calling current() on the baseIterator.
 *
 * <p>This avoids the cost of calling getAnother() to re-read the first item (which itself can be an
 * expensive operation, for example if it involves calling a user function).</p>
 */
public class OneItemGoneIterator
        implements SequenceIterator, LastPositionFinder, GroundedIterator, LookaheadIterator {

    private SequenceIterator baseIterator;
    private boolean catchingUp;

    /**
     * Create an iterator that delivers all the items that the base iterator delivers, even
     * though the first item of the base iterator has already been read
     * @param baseIterator the base iterator, whose current position must be 1
     */

    public OneItemGoneIterator(SequenceIterator baseIterator) {
        this.baseIterator = baseIterator;
        if (baseIterator.position() != 1) {
            throw new IllegalStateException();
        }
        this.catchingUp = true;
    }

    /**
     * Get the next item in the sequence. This method changes the state of the
     * iterator, in particular it affects the result of subsequent calls of
     * position() and current().
     * @return the next item, or null if there are no more items. Once a call
     *         on next() has returned null, no further calls should be made. The preferred
     *         action for an iterator if subsequent calls on next() are made is to return
     *         null again, and all implementations within Saxon follow this rule.
     * @throws net.sf.saxon.trans.XPathException
     *          if an error occurs retrieving the next item
     * @since 8.4
     */

    public Item next() throws XPathException {
        if (catchingUp) {
            catchingUp = false;
            return baseIterator.current();
        } else {
            return baseIterator.next();
        }
    }

    /**
     * Get the current value in the sequence (the one returned by the
     * most recent call on next()). This will be null before the first
     * call of next(). This method does not change the state of the iterator.
     * @return the current item, the one most recently returned by a call on
     *         next(). Returns null if next() has not been called, or if the end
     *         of the sequence has been reached.
     * @since 8.4
     */

    public Item current() {
        return (catchingUp ? null : baseIterator.current());
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
     * @return the current position, the position of the item returned by the
     *         most recent call of next(). This is 1 after next() has been successfully
     *         called once, 2 after it has been called twice, and so on. If next() has
     *         never been called, the method returns zero. If the end of the sequence
     *         has been reached, the value returned will always be <= 0; the preferred
     *         value is -1.
     * @since 8.4
     */

    public int position() {
        return (catchingUp ? 0 : baseIterator.position());
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
     * @since 9.1
     */

    public void close() {
        baseIterator.close();
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
     * @return a SequenceIterator that iterates over the same items,
     *         positioned before the first item
     * @throws net.sf.saxon.trans.XPathException
     *          if any error occurs
     * @since 8.4
     */

    public SequenceIterator getAnother() throws XPathException {
        return baseIterator.getAnother();
    }

    /**
     * Get properties of this iterator, as a bit-significant integer.
     * @return the properties of this iterator. This will be some combination of
     *         properties such as {@link #GROUNDED}, {@link #LAST_POSITION_FINDER},
     *         and {@link #LOOKAHEAD}. It is always
     *         acceptable to return the value zero, indicating that there are no known special properties.
     *         It is acceptable for the properties of the iterator to change depending on its state.
     * @since 8.6
     */

    public int getProperties() {
        return baseIterator.getProperties();
    }

    /**
     * Get the last position (that is, the number of items in the sequence). This method is
     * non-destructive: it does not change the state of the iterator.
     * The result is undefined if the next() method of the iterator has already returned null.
     * This method must not be called unless the result of getProperties() on the iterator
     * includes the bit setting {@link #LAST_POSITION_FINDER}
     */

    public int getLastPosition() throws XPathException {
        return ((LastPositionFinder)baseIterator).getLastPosition();
    }

    /**
     * Return a GroundedValue containing all the items in the sequence returned by this
     * SequenceIterator. This should be an "in-memory" value, not a Closure.
     * @return the corresponding Value
     */

    public GroundedValue materialize() throws XPathException {
        return ((GroundedIterator)baseIterator).materialize();
    }

    /**
     * Determine whether there are more items to come. Note that this operation
     * is stateless and it is not necessary (or usual) to call it before calling
     * next(). It is used only when there is an explicit need to tell if we
     * are at the last element.
     * <p/>
     * This method must not be called unless the result of getProperties() on the iterator
     * includes the bit setting {@link net.sf.saxon.om.SequenceIterator#LOOKAHEAD}
     * @return true if there are more items in the sequence
     */

    public boolean hasNext() {
        return catchingUp || ((LookaheadIterator)baseIterator).hasNext();
    }
}

//
// The contents of this file are subject to the Mozilla Public License Version
// 1.0 (the "License");
// you may not use this file except in compliance with the License. You may
// obtain a copy of the
// License at http://www.mozilla.org/MPL/
//
// Software distributed under the License is distributed on an "AS IS" basis,
// WITHOUT WARRANTY OF ANY KIND, either express or implied.
// See the License for the specific language governing rights and limitations
// under the License.
//
// The Original Code is: all this file.
//
// The Initial Developer of the Original Code is Michael Kay,
//
// Portions created by (your name) are Copyright (C) (your legal entity). All
// Rights Reserved.
//
// Contributor(s): none.
//



