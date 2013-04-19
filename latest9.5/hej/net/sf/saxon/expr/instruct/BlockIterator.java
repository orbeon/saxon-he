////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
// Copyright (c) 2013 Saxonica Limited.
// This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
// If a copy of the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
// This Source Code Form is "Incompatible With Secondary Licenses", as defined by the Mozilla Public License, v. 2.0.
////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

package net.sf.saxon.expr.instruct;

import net.sf.saxon.expr.Expression;
import net.sf.saxon.expr.XPathContext;
import net.sf.saxon.om.Item;
import net.sf.saxon.om.SequenceIterator;
import net.sf.saxon.trans.XPathException;

/**
 * Iterate over the instructions in the Block, concatenating the result of each instruction
 * into a single combined sequence.
 */

public class BlockIterator implements SequenceIterator<Item> {

    private Expression[] children;
    private int i = 0;
    /*@Nullable*/ private SequenceIterator child;
    private XPathContext context;
    private Item current;
    private int position = 0;

    public BlockIterator(Expression[] children, XPathContext context) {
        this.children = children;
        this.context = context;
    }

    /**
     * Get the next item in the sequence. <BR>
     *
     * @return the next item, or null if there are no more items.
     * @throws net.sf.saxon.trans.XPathException
     *          if an error occurs retrieving the next item
     */

    public Item next() throws XPathException {
        if (position < 0) {
            return null;
        }
        while (true) {
            if (child == null) {
                child = children[i++].iterate(context);
            }
            current = child.next();
            if (current != null) {
                position++;
                return current;
            }
            child = null;
            if (i >= children.length) {
                current = null;
                position = -1;
                return null;
            }
        }
    }

    /**
     * Get the current value in the sequence (the one returned by the
     * most recent call on next()). This will be null before the first
     * call of next().
     *
     * @return the current item, the one most recently returned by a call on
     *         next(); or null, if next() has not been called, or if the end
     *         of the sequence has been reached.
     */

    public Item current() {
        return current;
    }

    /**
     * Get the current position. This will be zero before the first call
     * on next(), otherwise it will be the number of times that next() has
     * been called.
     *
     * @return the current position, the position of the item returned by the
     *         most recent call of next()
     */

    public int position() {
        return position;
    }

    public void close() {
        if (child != null) {
            child.close();
        }
    }

    /**
     * Get another SequenceIterator that iterates over the same items as the original,
     * but which is repositioned at the start of the sequence.
     *
     * @return a SequenceIterator that iterates over the same items,
     *         positioned before the first item
     * @throws net.sf.saxon.trans.XPathException
     *          if any error occurs
     */

    /*@NotNull*/
    public BlockIterator getAnother() throws XPathException {
        return new BlockIterator(children, context);
    }

    /**
     * Get properties of this iterator, as a bit-significant integer.
     *
     * @return the properties of this iterator. This will be some combination of
     *         properties such as {@link SequenceIterator#GROUNDED}, {@link SequenceIterator#LAST_POSITION_FINDER},
     *         and {@link SequenceIterator#LOOKAHEAD}. It is always
     *         acceptable to return the value zero, indicating that there are no known special properties.
     *         It is acceptable for the properties of the iterator to change depending on its state.
     */

    public int getProperties() {
        return 0;
    }
}

