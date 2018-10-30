////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
// Copyright (c) 2018 Saxonica Limited.
// This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
// If a copy of the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
// This Source Code Form is "Incompatible With Secondary Licenses", as defined by the Mozilla Public License, v. 2.0.
////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

package net.sf.saxon.om;

import net.sf.saxon.trans.XPathException;

/**
 * A sequence that wraps an iterator, without being materialized. It can only be used once.
 */
public class LazySequence<T extends Item<?>> implements Sequence<T> {

    SequenceIterator<T> iterator;
    boolean used = false;

    /**
     * Create a virtual sequence backed by an iterator
     * @param iterator the iterator that delivers the items in the sequence
     */

    public LazySequence(SequenceIterator<T> iterator) {
        this.iterator = iterator;
    }

    /**
     * Get the first item in the sequence. This calls iterate() internally, which means it can only
     * be called once; the method should only be used if the client requires only the first item in
     * the sequence (or knows that the sequence has length one).
     * @return the first item, or null if the sequence is empty
     * @throws XPathException if an error occurs evaluating the underlying expression
     */

    public T head() throws XPathException {
        return iterate().next();
    }

    /**
     * Iterate over all the items in the sequence. The first time this is called it returns the original
     * iterator backing the sequence. On subsequent calls it clones the original iterator.
     * @return  an iterator over the items in the sequence
     * @throws XPathException if evaluation of the iterator fails.
     * @throws IllegalStateException if iterate() has already been called
     */

    public synchronized SequenceIterator<T> iterate() throws XPathException {
        if (used) {
            throw new IllegalStateException("A LazySequence can only be read once");
        } else {
            used = true;
            return iterator;
        }
    }

    @Override
    public Sequence<T> makeRepeatable() throws XPathException {
        return materialize();
    }
}

