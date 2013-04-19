////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
// Copyright (c) 2013 Saxonica Limited.
// This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
// If a copy of the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
// This Source Code Form is "Incompatible With Secondary Licenses", as defined by the Mozilla Public License, v. 2.0.
////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

package net.sf.saxon.expr;

import net.sf.saxon.om.Item;
import net.sf.saxon.om.SequenceIterator;
import net.sf.saxon.trans.XPathException;

/**
* ContextMappingIterator merges a sequence of sequences into a single flat
* sequence. It takes as inputs an iteration, and a mapping function to be
* applied to each Item returned by that iteration. The mapping function itself
* returns another iteration. The result is an iteration of the concatenation of all
* the iterations returned by the mapping function.<p>
*
* This is a specialization of the MappingIterator class: it differs in that it
* sets each item being processed as the context item
*/

public final class ContextMappingIterator<T extends Item> implements SequenceIterator<T> {

    private SequenceIterator base;
    private ContextMappingFunction<T> action;
    private XPathContext context;
    /*@Nullable*/ private SequenceIterator<T> stepIterator = null;
    private T current = null;
    private int position = 0;

    /**
    * Construct a ContextMappingIterator that will apply a specified ContextMappingFunction to
    * each Item returned by the base iterator.
     * @param action the mapping function to be applied
     * @param context the processing context. The mapping function is applied to each item returned
     * by context.getCurrentIterator() in turn.
     */

    public ContextMappingIterator(ContextMappingFunction<T> action, XPathContext context) {
        base = context.getCurrentIterator();
        this.action = action;
        this.context = context;
    }

    public T next() throws XPathException {
        T nextItem;
        while (true) {
            if (stepIterator != null) {
                nextItem = stepIterator.next();
                if (nextItem != null) {
                    break;
                } else {
                    stepIterator = null;
                }
            }
            if (base.next() != null) {
                // Call the supplied mapping function
                stepIterator = action.map(context);
                nextItem = stepIterator.next();
                if (nextItem == null) {
                    stepIterator = null;
                } else {
                    break;
                }

            } else {
                stepIterator = null;
                current = null;
                position = -1;
                return null;
            }
        }

        current = nextItem;
        position++;
        return nextItem;
    }

    public T current() {
        return current;
    }

    public int position() {
        return position;
    }

    public void close() {
        base.close();
    }

    /*@NotNull*/
    public SequenceIterator getAnother() throws XPathException {
        SequenceIterator newBase = base.getAnother();
        XPathContextMinor c2 = context.newMinorContext();
        c2.setCurrentIterator(newBase);
        return new ContextMappingIterator(action, c2);
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
        return 0;
    }

}

