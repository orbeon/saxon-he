package net.sf.saxon.expr;

import net.sf.saxon.om.Item;
import net.sf.saxon.om.LookaheadIterator;
import net.sf.saxon.om.SequenceIterator;
import net.sf.saxon.trans.XPathException;

/**
 * ItemMappingIterator applies a mapping function to each item in a sequence.
 * The mapping function either returns a single item, or null (representing an
 * empty sequence).
 * <p/>
 * This is a specialization of the more general MappingIterator class, for use
 * in cases where a single input item never maps to a sequence of more than one
 * output item.
 */

public final class ItemMappingIterator implements SequenceIterator, LookaheadIterator {

    private SequenceIterator base;
    private ItemMappingFunction action;
    private Item current = null;
    private int position = 0;
    private boolean oneToOne = false;

    /**
     * Construct an ItemMappingIterator that will apply a specified ItemMappingFunction to
     * each Item returned by the base iterator.
     *
     * @param base   the base iterator
     * @param action the mapping function to be applied
     */

    public ItemMappingIterator(SequenceIterator base, ItemMappingFunction action) {
        this.base = base;
        this.action = action;
    }

    /**
     * Construct an ItemMappingIterator that will apply a specified ItemMappingFunction to
     * each Item returned by the base iterator.
     *
     * @param base   the base iterator
     * @param action the mapping function to be applied
     * @param oneToOne true if this iterator is one-to-one
     */

    public ItemMappingIterator(SequenceIterator base, ItemMappingFunction action, boolean oneToOne) {
        this.base = base;
        this.action = action;
        this.oneToOne = oneToOne;
    }

    /**
     * Say whether this ItemMappingIterator is one-to-one: that is, for every input item, there is
     * always exactly one output item. The default is false.
     * @param oneToOne true if this iterator is one-to-one
     * @throws XPathException
     */

    public void setOneToOne(boolean oneToOne) {
        this.oneToOne = oneToOne;
    }

    /**
     * Ask whether this ItemMappingIterator is one-to-one: that is, for every input item, there is
     * always exactly one output item. The default is false.
     * @return true if this iterator is one-to-one
     * @throws XPathException
     */

    public boolean isOneToOne() {
        return oneToOne;
    }

    public boolean hasNext() {
        // Must only be called if this is a lookahead iterator, which will only be true if the base iterator
        // is a lookahead iterator
        return ((LookaheadIterator)base).hasNext();
    }

    public Item next() throws XPathException {
        while (true) {
            Item nextSource = base.next();
            if (nextSource == null) {
                current = null;
                position = -1;
                return null;
            }
            // Call the supplied mapping function
            current = action.map(nextSource);
            if (current != null) {
                position++;
                return current;
            }
            // otherwise go round the loop to get the next item from the base sequence
        }
    }

    public Item current() {
        return current;
    }

    public int position() {
        return position;
    }

    public void close() {
        base.close();
    }

    public SequenceIterator getAnother() throws XPathException {
        return new ItemMappingIterator(base.getAnother(), action, oneToOne);
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
        if (oneToOne && ((base.getProperties() & LOOKAHEAD) != 0)) {
            return LOOKAHEAD;
        } else {
            return 0;
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
// The Original Code is: all this file.
//
// The Initial Developer of the Original Code is Michael H. Kay
//
// Portions created by (your name) are Copyright (C) (your legal entity). All Rights Reserved.
//
// Contributor(s): none.
//
