////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
// Copyright (c) 2014 Saxonica Limited.
// This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
// If a copy of the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
// This Source Code Form is "Incompatible With Secondary Licenses", as defined by the Mozilla Public License, v. 2.0.
////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

package net.sf.saxon.functions;

import com.saxonica.ee.stream.adjunct.DistinctValuesAdjunct;
import net.sf.saxon.expr.Callable;
import net.sf.saxon.expr.XPathContext;
import net.sf.saxon.expr.parser.ExpressionVisitor;
import net.sf.saxon.expr.sort.AtomicComparer;
import net.sf.saxon.lib.StringCollator;
import net.sf.saxon.om.Sequence;
import net.sf.saxon.om.SequenceIterator;
import net.sf.saxon.om.SequenceTool;
import net.sf.saxon.trans.XPathException;
import net.sf.saxon.type.AtomicType;
import net.sf.saxon.type.ItemType;
import net.sf.saxon.value.AtomicValue;

import java.util.HashSet;

/**
 * The XPath 2.0 distinct-values() function
 */

public class DistinctValues extends CollatingFunction implements Callable {

    /**
     * Get the argument position (0-based) containing the collation name
     *
     * @return the position of the argument containing the collation URI
     */
    @Override
    protected int getCollationArgument() {
        return 1;
    }

    @Override
    public void checkArguments(/*@NotNull*/ ExpressionVisitor visitor) throws XPathException {
        super.checkArguments(visitor);
        ItemType itemType = argument[0].getItemType();
        if (itemType instanceof AtomicType) {
            preAllocateComparer((AtomicType) itemType, (AtomicType) itemType, visitor.getStaticContext(), true);
        }
    }

    /**
     * Get the AtomicComparer allocated at compile time.
     *
     * @return the AtomicComparer if one has been allocated at compile time; return null
     *         if the collation is not known until run-time
     */

    public AtomicComparer getAtomicComparer() {
        return getPreAllocatedAtomicComparer();
    }

//#ifdefined STREAM


    /**
     * Get a class that supports streamed evaluation of this expression
     *
     * @return the relevant StreamingAdjunct, or null if none is available
     */
    @Override
    public DistinctValuesAdjunct getStreamingAdjunct() {
        return new DistinctValuesAdjunct();
    }
//#endif

    /**
     * Evaluate the function to return an iteration of selected values or nodes.
     */

    /*@NotNull*/
    public SequenceIterator iterate(XPathContext context) throws XPathException {
        StringCollator collator = getCollator(context);
        SequenceIterator iter = argument[0].iterate(context);
        return new DistinctIterator(iter, collator, context);
    }

    /**
     * Iterator class to return the distinct values in a sequence
     */

    public static class DistinctIterator implements SequenceIterator {

        private SequenceIterator base;
        private StringCollator collator;
        private XPathContext context;
        private HashSet<Object> lookup = new HashSet<Object>(40);

        /**
         * Create an iterator over the distinct values in a sequence
         *
         * @param base     the input sequence. This must return atomic values only.
         * @param collator The comparer used to obtain comparison keys from each value;
         *                 these comparison keys are themselves compared using equals().
         * @param context the XPath dynamic context
         */

        public DistinctIterator(SequenceIterator base, StringCollator collator, XPathContext context) {
            this.base = base;
            this.collator = collator;
            this.context = context;
        }

        /**
         * Get the next item in the sequence. <BR>
         *
         * @return the next item, or null if there are no more items.
         * @throws net.sf.saxon.trans.XPathException
         *          if an error occurs retrieving the next item
         */

        public AtomicValue next() throws XPathException {
            int implicitTimezone = context.getImplicitTimezone();
            while (true) {
                AtomicValue nextBase = (AtomicValue) base.next();
                if (nextBase == null) {
                    return null;
                }
                Object key;
                if (nextBase.isNaN()) {
                    key = DistinctValues.class;
                } else {
                    key = nextBase.getXPathComparable(false, collator, implicitTimezone);
                }
                if (lookup.add(key)) {
                    // returns true if newly added (if not, keep looking)
                    return nextBase;
                }
            }
        }

        public void close() {
            base.close();
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
        public DistinctIterator getAnother() throws XPathException {
            return new DistinctIterator(base.getAnother(), collator, context);
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

    public Sequence call(XPathContext context, Sequence[] arguments) throws XPathException {
        StringCollator collator = getCollatorFromLastArgument(arguments, 1, context);
        return SequenceTool.toLazySequence(new DistinctIterator(arguments[0].iterate(), collator, context));
    }

}

