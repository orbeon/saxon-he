////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
// Copyright (c) 2018 Saxonica Limited.
// This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
// If a copy of the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
// This Source Code Form is "Incompatible With Secondary Licenses", as defined by the Mozilla Public License, v. 2.0.
////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

package net.sf.saxon.tree.iter;

import net.sf.saxon.expr.LastPositionFinder;
import net.sf.saxon.om.Item;
import net.sf.saxon.om.SequenceIterator;
import net.sf.saxon.trans.XPathException;
import net.sf.saxon.value.AtomicValue;

/**
 * AtomizingIterator returns the atomization of an underlying sequence supplied
 * as an iterator.  We use a specialist class rather than a general-purpose
 * MappingIterator for performance, especially as the relationship of items
 * in the result sequence to those in the base sequence is often one-to-one.
 * <p>This UntypedAtomizingIterator is used only when it is known that the input
 * sequence consists entirely of nodes, and that all nodes will be untyped.</p>
 */

public class UntypedAtomizingIterator implements SequenceIterator<AtomicValue>,
        LastPositionFinder, LookaheadIterator<AtomicValue> {

    private SequenceIterator base;

    /**
     * Construct an AtomizingIterator that will atomize the values returned by the base iterator.
     *
     * @param base the base iterator
     */

    public UntypedAtomizingIterator(SequenceIterator base) {
        this.base = base;
    }

    /*@Nullable*/
    public AtomicValue next() throws XPathException {
        Item nextSource = base.next();
        if (nextSource == null) {
            return null;
        } else {
            return (AtomicValue) nextSource.atomize();
        }
    }

    public void close() {
        base.close();
    }

    /**
     * Get properties of this iterator, as a bit-significant integer.
     *
     * @return the properties of this iterator. This will be some combination of
     *         properties such as {@link net.sf.saxon.om.SequenceIterator#GROUNDED}, {@link net.sf.saxon.om.SequenceIterator#LAST_POSITION_FINDER},
     *         and {@link net.sf.saxon.om.SequenceIterator#LOOKAHEAD}. It is always
     *         acceptable to return the value zero, indicating that there are no known special properties.
     *         It is acceptable for the properties of the iterator to change depending on its state.
     */

    public int getProperties() {
        return base.getProperties() & (SequenceIterator.LAST_POSITION_FINDER | SequenceIterator.LOOKAHEAD);
    }

    public int getLength() throws XPathException {
        return ((LastPositionFinder) base).getLength();
    }

    public boolean hasNext() {
        return ((LookaheadIterator) base).hasNext();
    }
}

