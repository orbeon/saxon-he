package net.sf.saxon.expr;

import net.sf.saxon.om.SequenceIterator;
import net.sf.saxon.om.LookaheadIterator;
import net.sf.saxon.om.Item;
import net.sf.saxon.om.ArrayIterator;
import net.sf.saxon.trans.XPathException;

/**
 * TailIterator iterates over a base sequence starting at an element other than the first.
 * The base sequence is represented by an iterator which is consumed in the process
 */

public class TailIterator implements SequenceIterator, LastPositionFinder, LookaheadIterator {

    private SequenceIterator base;
    private int start;

    /**
    * Static factory method. Creates a PositionIterator, unless the base Iterator is an
    * ArrayIterator, in which case it optimizes by creating a new ArrayIterator directly over the
    * underlying array. This optimization is important when doing recursion over a node-set using
    * repeated calls of $nodes[position()>1]
    * @param base   An iteration of the items to be filtered
    * @param start    The position of the first item to be included (base 1)
    */

    public static SequenceIterator make(SequenceIterator base, int start) throws XPathException {
        if (base instanceof ArrayIterator) {
            return ((ArrayIterator)base).makeSliceIterator(start, Integer.MAX_VALUE);
        } else {
            return new TailIterator(base, start);
        }
    }

    public TailIterator(SequenceIterator base, int start) throws XPathException {
        this.base = base;
        this.start = start;

        // discard the first n-1 items from the underlying iterator
        // TODO: better approaches are possible if the base iterator is grounded
        for (int i=0; i < start-1; i++) {
            Item b = base.next();
            if (b == null) {
                break;
            }
        }
    }

    public Item next() throws XPathException {
        return base.next();
    }

    public Item current() {
        return base.current();
    }

    public int position() {
        int bp = base.position();
        return (bp > 0 ? (base.position() - start + 1) : bp);
    }

    public boolean hasNext() {
        return ((LookaheadIterator)base).hasNext();
    }

    public int getLastPosition() throws XPathException {
        int bl = ((LastPositionFinder)base).getLastPosition() - start + 1;
        return (bl > 0 ? bl : 0);
    }

    public SequenceIterator getAnother() throws XPathException {
        return new TailIterator(base.getAnother(), start);
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
        return base.getProperties() & (LAST_POSITION_FINDER | LOOKAHEAD);
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
// The Initial Developer of the Original Code is Michael H. Kay.
//
// Contributor(s):
//

