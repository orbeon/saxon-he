package net.sf.saxon.expr;

import net.sf.saxon.om.Item;
import net.sf.saxon.om.SequenceIterator;
import net.sf.saxon.trans.XPathException;

/**
* MappingIterator merges a sequence of sequences into a single flat
* sequence. It takes as inputs an iteration, and a mapping function to be
* applied to each Item returned by that iteration. The mapping function itself
* returns another iteration. The result is an iteration of the concatenation of all
* the iterations returned by the mapping function.<p>
*
* This is a powerful class. It is used, with different mapping functions,
* in a great variety of ways. It underpins the way that "for" expressions and
* path expressions are evaluated, as well as sequence expressions. It is also
* used in the implementation of the document(), key(), and id() functions.
*/

public class MappingIterator<F extends Item, T extends Item>
        implements SequenceIterator<T> {

    private SequenceIterator<F> base;
    private MappingFunction<F, T> action;
    /*@Nullable*/ private SequenceIterator<T> results = null;
    //private boolean atomizing = false;
    private T current = null;
    private int position = 0;

    /**
     * Construct a MappingIterator that will apply a specified MappingFunction to
     * each Item returned by the base iterator.
     * @param base the base iterator
     * @param action the mapping function to be applied
     */

    public MappingIterator(SequenceIterator<F> base, MappingFunction<F, T> action) {
        this.base = base;
        this.action = action;
    }

    public T next() throws XPathException {
        T nextItem;
        while (true) {
            if (results != null) {
                nextItem = results.next();
                if (nextItem != null) {
                    break;
                } else {
                    results = null;
                }
            }
            F nextSource = base.next();
            if (nextSource != null) {
                // Call the supplied mapping function
                SequenceIterator<T> obj = action.map(nextSource);

                // The result may be null (representing an empty sequence),
                //  or a SequenceIterator (any sequence)

                if (obj != null) {
                    results = obj;
                    nextItem = results.next();
                    if (nextItem == null) {
                        results = null;
                    } else {
                        break;
                    }
                }
                // now go round the loop to get the next item from the base sequence
            } else {
                results = null;
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
        if (results != null) {
            results.close();
        }
        base.close();
    }

    /*@NotNull*/
    public SequenceIterator<T> getAnother() throws XPathException {
        SequenceIterator<F> newBase = base.getAnother();
        MappingFunction<F, T> newAction = action instanceof StatefulMappingFunction ?
                (MappingFunction<F, T>)((StatefulMappingFunction)action).getAnother() :
                action;
        return new MappingIterator<F, T>(newBase, newAction);
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