package net.sf.saxon.tree.iter;

import net.sf.saxon.expr.LastPositionFinder;
import net.sf.saxon.om.Item;
import net.sf.saxon.om.NodeInfo;
import net.sf.saxon.om.SequenceIterator;
import net.sf.saxon.trans.XPathException;
import net.sf.saxon.value.AtomicValue;

/**
 * AtomizingIterator returns the atomization of an underlying sequence supplied
 * as an iterator.  We use a specialist class rather than a general-purpose
 * MappingIterator for performance, especially as the relationship of items
 * in the result sequence to those in the base sequence is often one-to-one.
 *
 * This UntypedAtomizingIterator is used only when it is known that all nodes
 * will be untyped, and that atomizing a node therefore always returns a singleton.
 * However, it is not necessarily the case that the input sequence contains only
 * nodes, and therefore the result sequence may contains atomic values that are
 * not untyped.
 *
 * The parameter type B denotes the type of the items being atomized.
*/

public class UntypedAtomizingIterator<B extends Item> implements SequenceIterator<AtomicValue>,
        LastPositionFinder<AtomicValue>, LookaheadIterator<AtomicValue> {

    private SequenceIterator<B> base;
    /*@Nullable*/ private AtomicValue current = null;
    private int position = 0;

    /**
     * Construct an AtomizingIterator that will atomize the values returned by the base iterator.
     * @param base the base iterator
     */

    public UntypedAtomizingIterator(SequenceIterator<B> base) {
        this.base = base;
    }

    /*@Nullable*/ public AtomicValue next() throws XPathException {
        Item nextSource = base.next();
        if (nextSource != null) {
            if (nextSource instanceof NodeInfo) {
                current = (AtomicValue)((NodeInfo)nextSource).atomize();
                position++;
                return current;
             } else if (nextSource instanceof AtomicValue) {
                return (AtomicValue)nextSource;
            } else {
                throw new XPathException("The typed value of a function item is not defined", "FOTY0013");
            }
        } else {
            current = null;
            position = -1;
            return null;
        }
    }

    /*@Nullable*/ public AtomicValue current() {
        return current;
    }

    public int position() {
        return position;
    }

    public void close() {
        base.close();
    }

    /*@NotNull*/
    public SequenceIterator<AtomicValue> getAnother() throws XPathException {
        return new UntypedAtomizingIterator<B>(base.getAnother());
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
        return ((LastPositionFinder)base).getLength();
    }

    public boolean hasNext() {
        return ((LookaheadIterator)base).hasNext();
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
// The Initial Developer of the Original Code is Saxonica Limited.
// Portions created by ___ are Copyright (C) ___. All rights reserved.
//
// Contributor(s):
//