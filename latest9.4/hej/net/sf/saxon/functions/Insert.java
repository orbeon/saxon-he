package net.sf.saxon.functions;

import net.sf.saxon.expr.CallableExpression;
import net.sf.saxon.expr.XPathContext;
import net.sf.saxon.om.Item;
import net.sf.saxon.om.SequenceIterator;
import net.sf.saxon.trans.XPathException;
import net.sf.saxon.value.AtomicValue;
import net.sf.saxon.value.NumericValue;

/**
* The XPath 2.0 insert-before() function
*/


public class Insert extends SystemFunction implements CallableExpression {

    /**
    * Evaluate the function to return an iteration of selected nodes.
    */

    /*@NotNull*/
    public SequenceIterator iterate(XPathContext context) throws XPathException {
        SequenceIterator seq = argument[0].iterate(context);
        AtomicValue n0 = (AtomicValue)argument[1].evaluateItem(context);
        NumericValue n = (NumericValue)n0;
        int pos = (int)n.longValue();
        SequenceIterator ins = argument[2].iterate(context);
        return new InsertIterator(seq, ins, pos);
    }

    /**
     * Evaluate the expression as a general function call
     *
     * @param arguments the values of the arguments, supplied as SequenceIterators
     * @param context   the dynamic evaluation context
     * @return the result of the evaluation, in the form of a SequenceIterator
     * @throws net.sf.saxon.trans.XPathException
     *          if a dynamic error occurs during the evaluation of the expression
     */
    public SequenceIterator call(SequenceIterator[] arguments, XPathContext context) throws XPathException {
        NumericValue n = (NumericValue)arguments[1].next();
        int pos = (int)n.longValue();
        return new InsertIterator(arguments[0], arguments[2], pos);
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
        /*@Nullable*/ private Item current = null;
        private boolean inserting = false;

        public InsertIterator(SequenceIterator base, SequenceIterator insert, int insertPosition) {
            this.base = base;
            this.insert = insert;
            this.insertPosition = (insertPosition<1 ? 1 : insertPosition);
            this.inserting = (insertPosition==1);
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
                if (position == insertPosition-1) {
                    nextItem = insert.next();
                    if (nextItem == null) {
                        nextItem = base.next();
                    } else {
                        inserting = true;
                    }
                } else {
                    nextItem = base.next();
                    if (nextItem==null && position < insertPosition-1) {
                        inserting = true;
                        nextItem = insert.next();
                    }
                }
            }
            if (nextItem == null) {
                current = null;
                position = -1;
                return null;
            } else {
                current = nextItem;
                position++;
                return current;
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
            insert.close();
        }

        /*@NotNull*/
        public SequenceIterator getAnother() throws XPathException {
            return new InsertIterator(  base.getAnother(),
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