package net.sf.saxon.functions;

import net.sf.saxon.expr.CallableExpression;
import net.sf.saxon.expr.XPathContext;
import net.sf.saxon.expr.sort.GenericAtomicComparer;
import net.sf.saxon.lib.StringCollator;
import net.sf.saxon.om.Item;
import net.sf.saxon.om.SequenceIterator;
import net.sf.saxon.trans.XPathException;
import net.sf.saxon.type.BuiltInAtomicType;
import net.sf.saxon.type.Type;
import net.sf.saxon.value.AtomicValue;
import net.sf.saxon.value.Int64Value;
import net.sf.saxon.value.IntegerValue;


/**
* The XPath 2.0 index-of() function
*/


public class IndexOf extends CollatingFunction implements CallableExpression {

    /**
     * For an expression that returns an integer or a sequence of integers, get
     * a lower and upper bound on the values of the integers that may be returned, from
     * static analysis. The default implementation returns null, meaning "unknown" or
     * "not applicable". Other implementations return an array of two IntegerValue objects,
     * representing the lower and upper bounds respectively. The values
     * UNBOUNDED_LOWER and UNBOUNDED_UPPER are used by convention to indicate that
     * the value may be arbitrarily large. The values MAX_STRING_LENGTH and MAX_SEQUENCE_LENGTH
     * are used to indicate values limited by the size of a string or the size of a sequence.
     *
     * @return the lower and upper bounds of integer values in the result, or null to indicate
     *         unknown or not applicable.
     */
    @Override
    public IntegerValue[] getIntegerBounds() {
        return new IntegerValue[]{Int64Value.PLUS_ONE, MAX_SEQUENCE_LENGTH};
    }

    /**
    * Evaluate the function to return an iteration of selected nodes.
    */

    /*@NotNull*/
    public SequenceIterator iterate(XPathContext context) throws XPathException {
        GenericAtomicComparer comparer = getAtomicComparer(2, context);
        SequenceIterator seq = argument[0].iterate(context);
        AtomicValue val = (AtomicValue)argument[1].evaluateItem(context);
        return new IndexIterator(seq, val, comparer);
    }

    /**
     * Evaluate the expression
     *
     * @param arguments the values of the arguments, supplied as SequenceIterators
     * @param context   the dynamic evaluation context
     * @return the result of the evaluation, in the form of a SequenceIterator
     * @throws net.sf.saxon.trans.XPathException
     *          if a dynamic error occurs during the evaluation of the expression
     */
    public SequenceIterator call(SequenceIterator[] arguments, XPathContext context) throws XPathException {
        StringCollator collator = stringCollator;
        if (collator == null) {
            String collationName = arguments[2].next().getStringValue();
            try {
                collationName = expandCollationURI(collationName, getExpressionBaseURI(), context);
            } catch (XPathException err) {
                err.setLocator(this);
            }
            collator = context.getCollation(collationName);
        }
        GenericAtomicComparer comparer = new GenericAtomicComparer(collator, context);
        SequenceIterator seq = arguments[0];
        AtomicValue val = (AtomicValue)arguments[1].next();
        return new IndexIterator(seq, val, comparer);
    }

    /**
     * Iterator to return the index positions of selected items in a sequence
     */

    public static class IndexIterator implements SequenceIterator {

        private SequenceIterator base;
        private AtomicValue value;
        private GenericAtomicComparer comparer;
        private int index = 0;
        private int position = 0;
        /*@Nullable*/ private Item current = null;
        private BuiltInAtomicType primitiveTypeRequired;

        /**
         * Get an iterator returning the index positions of selected items in a sequence
         * @param base The sequence to be searched
         * @param value The value being sought
         * @param comparer Comparer used to determine whether values match
         */

        public IndexIterator(SequenceIterator base, AtomicValue value, GenericAtomicComparer comparer) {
            this.base = base;
            this.value = value;
            this.comparer = comparer;
            primitiveTypeRequired = value.getPrimitiveType();
        }

        public Item next() throws XPathException {
            while (true) {
                AtomicValue i = (AtomicValue)base.next();
                if (i==null) break;
                index++;
                if (Type.isComparable(primitiveTypeRequired,
                            i.getPrimitiveType(), false)) {
                    try {
                        if (comparer.comparesEqual(i, value)) {
                            current = Int64Value.makeIntegerValue(index);
                            position++;
                            return current;
                        }
                    } catch (ClassCastException err) {
                        // non-comparable values are treated as not equal
                        // Exception shouldn't happen but we catch it anyway
                    }
                }
            }
            current = null;
            position = -1;
            return null;
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

        /*@NotNull*/
        public SequenceIterator getAnother() throws XPathException {
            return new IndexIterator(base.getAnother(), value, comparer);
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