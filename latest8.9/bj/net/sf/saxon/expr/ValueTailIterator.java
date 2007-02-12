package net.sf.saxon.expr;

import net.sf.saxon.om.*;
import net.sf.saxon.trans.XPathException;
import net.sf.saxon.value.Value;
import net.sf.saxon.value.SequenceExtent;

/**
 * TailIterator iterates over a base sequence starting at an element other than the first.
 * The base sequence is represented by an iterator which is consumed in the process
 */

public class ValueTailIterator implements SequenceIterator, GroundedIterator, LookaheadIterator {

    private Value baseValue;
    private int start;  // zero-based
    private int pos = 0;

    /**
    * Static factory method. Creates a PositionIterator, unless the base Iterator is an
    * ArrayIterator, in which case it optimizes by creating a new ArrayIterator directly over the
    * underlying array. This optimization is important when doing recursion over a node-set using
    * repeated calls of $nodes[position()>1]
    * @param base   The items to be filtered
    * @param start    The position of the first item to be included (zero-based)
    */

//    public static SequenceIterator make(Value base, int start) throws XPathException {
//        if (base instanceof ArrayIterator) {
//            return ((ArrayIterator)base).makeSliceIterator(start, Integer.MAX_VALUE);
//        } else {
//            return new ValueTailIterator(base, start);
//        }
//    }

    /**
     * Construct a ValueTailIterator
     * @param base   The items to be filtered
     * @param start    The position of the first item to be included (zero-based)
     * @throws XPathException
     */

    public ValueTailIterator(Value base, int start) throws XPathException {
        this.baseValue = base;
        this.start = start;
        this.pos = 0;
    }

    public Item next() throws XPathException {
        return baseValue.itemAt(start + pos++);
    }

    public Item current() {
        try {
            return baseValue.itemAt(start + pos - 1);
        } catch (XPathException e) {
            throw new AssertionError(e);    // can't happen
        }
    }

    public int position() {
        return pos;
    }

    public boolean hasNext() {
        try {
            return baseValue.itemAt(start + pos) != null;
        } catch (XPathException e) {
            return true; // rely on the exception being thrown again when next() is called
        }
    }

    public SequenceIterator getAnother() throws XPathException {
        return new ValueTailIterator(baseValue, start);
    }

    /**
     * Return a Value containing all the items in the sequence returned by this
     * SequenceIterator. This should be an "in-memory" value, not a Closure.
     *
     * @return the corresponding Value
     */

    public Value materialize() throws XPathException {
        if (start == 0) {
            return baseValue.reduce();
        } else if (baseValue instanceof SequenceExtent) {
            return new SequenceExtent((SequenceExtent)baseValue, start, baseValue.getLength() - start);
        } else {
            return Value.asValue(SequenceExtent.makeSequenceExtent(getAnother()));
        }
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
        return GROUNDED | LOOKAHEAD;
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

