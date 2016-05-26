////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
// Copyright (c) 2014 Saxonica Limited.
// This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
// If a copy of the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
// This Source Code Form is "Incompatible With Secondary Licenses", as defined by the Mozilla Public License, v. 2.0.
////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

package net.sf.saxon.functions;

import com.saxonica.ee.stream.adjunct.InsertBeforeAdjunct;
import net.sf.saxon.expr.Callable;
import net.sf.saxon.expr.XPathContext;
import net.sf.saxon.om.Item;
import net.sf.saxon.om.Sequence;
import net.sf.saxon.om.SequenceIterator;
import net.sf.saxon.om.SequenceTool;
import net.sf.saxon.trans.XPathException;
import net.sf.saxon.type.ItemType;
import net.sf.saxon.type.Type;
import net.sf.saxon.value.AtomicValue;
import net.sf.saxon.value.NumericValue;

/**
 * The XPath 2.0 insert-before() function
 */


public class InsertBefore extends SystemFunctionCall implements Callable {

    /**
     * Determine the item type of the value returned by the function
     * <p/>
     * <p/>
     * /*@NotNull
     */
    @Override
    public ItemType getItemType() {
        return Type.getCommonSuperType(getArguments()[0].getItemType(), getArguments()[2].getItemType());
    }

    /**
     * Evaluate the function to return an iteration of selected nodes.
     */

    /*@NotNull*/
    public SequenceIterator iterate(XPathContext context) throws XPathException {
        SequenceIterator seq = argument[0].iterate(context);
        AtomicValue n0 = (AtomicValue) argument[1].evaluateItem(context);
        NumericValue n = (NumericValue) n0;
        int pos = (int) n.longValue();
        SequenceIterator ins = argument[2].iterate(context);
        return new InsertIterator(seq, ins, pos);
    }

    /**
     * Evaluate the expression as a general function call
     *
     * @param context   the dynamic evaluation context
     * @param arguments the values of the arguments, supplied as SequenceIterators
     * @return the result of the evaluation, in the form of a SequenceIterator
     * @throws net.sf.saxon.trans.XPathException
     *          if a dynamic error occurs during the evaluation of the expression
     */
    public Sequence call(XPathContext context, Sequence[] arguments) throws XPathException {
        NumericValue n = (NumericValue) arguments[1].head();
        int pos = (int) n.longValue();
        return SequenceTool.toLazySequence(
                new InsertIterator(arguments[0].iterate(), arguments[2].iterate(), pos));
    }

    /**
     * Insertion iterator. This is supplied with an iterator over the base sequence,
     * an iterator over the sequence to be inserted, and the insert position.
     */

    public static class InsertIterator implements SequenceIterator {

        private SequenceIterator base;
        private SequenceIterator insert;
        private int insertPosition;
        private int position = 0;
        private boolean inserting = false;

        public InsertIterator(SequenceIterator base, SequenceIterator insert, int insertPosition) {
            this.base = base;
            this.insert = insert;
            this.insertPosition = insertPosition < 1 ? 1 : insertPosition;
            this.inserting = insertPosition == 1;
        }


        public Item next() throws XPathException {
            Item nextItem;
            if (inserting) {
                nextItem = insert.next();
                if (nextItem == null) {
                    inserting = false;
                    nextItem = base.next();
                }
            } else {
                if (position == insertPosition - 1) {
                    nextItem = insert.next();
                    if (nextItem == null) {
                        nextItem = base.next();
                    } else {
                        inserting = true;
                    }
                } else {
                    nextItem = base.next();
                    if (nextItem == null && position < insertPosition - 1) {
                        inserting = true;
                        nextItem = insert.next();
                    }
                }
            }
            if (nextItem == null) {
                position = -1;
                return null;
            } else {
                position++;
                return nextItem;
            }
        }

        public void close() {
            base.close();
            insert.close();
        }

        /*@NotNull*/
        public InsertIterator getAnother() throws XPathException {
            return new InsertIterator(base.getAnother(),
                    insert.getAnother(),
                    insertPosition);
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
            return 0;
        }

    }

//#ifdefined STREAM

    /**
     * Get a class that supports streamed evaluation of this expression
     *
     * @return the relevant StreamingAdjunct, or null if none is available
     */
    @Override
    public InsertBeforeAdjunct getStreamingAdjunct() {
        return new InsertBeforeAdjunct();
    }
//#endif

}

