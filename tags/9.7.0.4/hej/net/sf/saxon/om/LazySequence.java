////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
// Copyright (c) 2015 Saxonica Limited.
// This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
// If a copy of the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
// This Source Code Form is "Incompatible With Secondary Licenses", as defined by the Mozilla Public License, v. 2.0.
////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

package net.sf.saxon.om;

import net.sf.saxon.trans.XPathException;

/**
 * A sequence that wraps an iterator, without being materialized. If used more than once, the input
 * iterator is re-evaluated (which can cause problems if the client is dependent on node identity).
 */
public class LazySequence implements Sequence {

    SequenceIterator iterator;
    boolean used = false;

    /**
     * Create a virtual sequence backed by an iterator
     * @param iterator the iterator that delivers the items in the sequence
     */

    public LazySequence(SequenceIterator iterator) {
        this.iterator = iterator;
    }

    /**
     * Get the first item in the sequence
     * @return the first item
     * @throws XPathException
     */

    public Item head() throws XPathException {
        return iterate().next();
    }

    /**
     * Iterate over all the items in the sequence. The first time this is called it returns the original
     * iterator backing the sequence. On subsequent calls it clones the original iterator.
     * @return  an iterator over the items in the sequence
     * @throws XPathException if evaluation of the iterator fails.
     */

    public synchronized SequenceIterator iterate() throws XPathException {
        if (used) {
            throw new IllegalStateException("A LazySequence can only be read once");
            //return iterator.getAnother();
        } else {
            used = true;
            return iterator;
        }
    }
}

