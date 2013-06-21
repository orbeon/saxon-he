package net.sf.saxon.tree.iter;

import net.sf.saxon.om.Item;
import net.sf.saxon.om.NodeInfo;
import net.sf.saxon.om.SequenceIterator;
import net.sf.saxon.trans.XPathException;
import net.sf.saxon.value.AtomicValue;
import net.sf.saxon.value.Value;

/**
 * AtomizingIterator returns the atomization of an underlying sequence supplied
 * as an iterator.  We use a specialist class rather than a general-purpose
 * MappingIterator for performance, especially as the relationship of items
 * in the result sequence to those in the base sequence is often one-to-one.
 *
 * This AtomizingIterator is capable of handling list-typed nodes whose atomized value
 * is a sequence of more than one item. When it is known that all input will be untyped,
 * an {@link UntypedAtomizingIterator} is used in preference.
*/

public class AtomizingIterator implements SequenceIterator<AtomicValue> {

    private SequenceIterator base;
    /*@Nullable*/ private Value currentValue = null;
    /*@Nullable*/ private AtomicValue current = null;
    private int position = 0;
    private int currentValuePosition = 1;
    private int currentValueSize = 1;

    /**
     * Construct an AtomizingIterator that will atomize the values returned by the base iterator.
     * @param base the base iterator
     */

    public AtomizingIterator(SequenceIterator base) {
        this.base = base;
    }

    /*@Nullable*/ public AtomicValue next() throws XPathException {
        while (true) {
            if (currentValue != null) {
                if (currentValuePosition < currentValueSize) {
                    current = (AtomicValue)currentValue.itemAt(currentValuePosition++);
                    position++;
                    return current;
                } else {
                    currentValue = null;
                }
            }
            Item nextSource = base.next();
            if (nextSource != null) {
                if (nextSource instanceof NodeInfo) {
                    Value v = ((NodeInfo)nextSource).atomize();
                    if (v instanceof AtomicValue) {
                        current = (AtomicValue)v;
                        position++;
                        return (AtomicValue)v;
                    } else {
                        currentValue = v;
                        currentValuePosition = 0;
                        currentValueSize = currentValue.getLength();
                        // now go round the loop to get the first item from the atomized value
                    }
                } else if (nextSource instanceof AtomicValue) {
                    return (AtomicValue)nextSource;
                } else {
                    throw new XPathException("The typed value of a function item is not defined", "FOTY0013");
                }
            } else {
                currentValue = null;
                current = null;
                position = -1;
                return null;
            }
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
        return new AtomizingIterator(base.getAnother());
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