////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
// Copyright (c) 2018 Saxonica Limited.
// This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
// If a copy of the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
// This Source Code Form is "Incompatible With Secondary Licenses", as defined by the Mozilla Public License, v. 2.0.
////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

package net.sf.saxon.expr;

import net.sf.saxon.om.Item;
import net.sf.saxon.om.SequenceIterator;
import net.sf.saxon.trans.XPathException;
import net.sf.saxon.tree.iter.LookaheadIterator;

/**
 * ItemMappingIterator applies a mapping function to each item in a sequence.
 * The mapping function either returns a single item, or null (representing an
 * empty sequence).
 * <p>This is a specialization of the more general MappingIterator class, for use
 * in cases where a single input item never maps to a sequence of more than one
 * output item.</p>
 */

public class ItemMappingIterator<F extends Item<?>, T extends Item<?>>
        implements SequenceIterator<T>, LookaheadIterator<T>, LastPositionFinder {

    private SequenceIterator<? extends F> base;
    private ItemMappingFunction<F, T> action;
    private boolean oneToOne = false;

    /**
     * Construct an ItemMappingIterator that will apply a specified DummyItemMappingFunction to
     * each Item returned by the base iterator.
     *
     * @param base   the base iterator
     * @param action the mapping function to be applied.
     */

    public ItemMappingIterator(SequenceIterator<F> base, ItemMappingFunction<F, T> action) {
        this.base = base;
        this.action = action;
    }

    /**
     * Construct an ItemMappingIterator that will apply a specified ItemMappingFunction to
     * each Item returned by the base iterator.
     *
     * @param base     the base iterator
     * @param action   the mapping function to be applied
     * @param oneToOne true if this iterator is one-to-one
     */

    public ItemMappingIterator(SequenceIterator<? extends F> base, ItemMappingFunction<F, T> action, boolean oneToOne) {
        this.base = base;
        this.action = action;
        this.oneToOne = oneToOne;
    }

    /**
     * Say whether this ItemMappingIterator is one-to-one: that is, for every input item, there is
     * always exactly one output item. The default is false.
     *
     * @param oneToOne true if this iterator is one-to-one
     */

    public void setOneToOne(boolean oneToOne) {
        this.oneToOne = oneToOne;
    }

    /**
     * Ask whether this ItemMappingIterator is one-to-one: that is, for every input item, there is
     * always exactly one output item. The default is false.
     *
     * @return true if this iterator is one-to-one
     */

    public boolean isOneToOne() {
        return oneToOne;
    }

    /**
     * Get the base (input) iterator
     * @return the iterator over the input sequence
     */

    protected SequenceIterator<? extends F> getBaseIterator() {
        return base;
    }

    /**
     * Get the mapping function (the function applied to each item in the input sequence
     * @return the mapping function
     */

    protected ItemMappingFunction<F, T> getMappingFunction() {
        return action;
    }

    public boolean hasNext() {
        // Must only be called if this is a lookahead iterator, which will only be true if the base iterator
        // is a lookahead iterator and one-to-one is true
        return ((LookaheadIterator) base).hasNext();
    }

    public T next() throws XPathException {
        while (true) {
            F nextSource = base.next();
            if (nextSource == null) {
                return null;
            }
            // Call the supplied mapping function
            T current = action.mapItem(nextSource);
            if (current != null) {
                return current;
            }
            // otherwise go round the loop to get the next item from the base sequence
        }
    }

    public void close() {
        base.close();
    }

    public int getLength() throws XPathException {
        // Must only be called if this is a last-position-finder iterator, which will only be true if the base iterator
        // is a last-position-finder iterator and one-to-one is true
        return ((LastPositionFinder) base).getLength();
    }

    /**
     * Get properties of this iterator, as a bit-significant integer.
     *
     * @return the properties of this iterator. This will be some combination of
     *         properties such as {@link net.sf.saxon.om.SequenceIterator#GROUNDED},
     *         {@link net.sf.saxon.om.SequenceIterator#LAST_POSITION_FINDER},
     *         and {@link net.sf.saxon.om.SequenceIterator#LOOKAHEAD}. It is always
     *         acceptable to return the value zero, indicating that there are no known special properties.
     *         It is acceptable for the properties of the iterator to change depending on its state.
     */

    public int getProperties() {
        if (oneToOne) {
            return base.getProperties() & (LOOKAHEAD | LAST_POSITION_FINDER);
        } else {
            return 0;
        }
    }
}

