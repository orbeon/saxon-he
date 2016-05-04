////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
// Copyright (c) 2015 Saxonica Limited.
// This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
// If a copy of the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
// This Source Code Form is "Incompatible With Secondary Licenses", as defined by the Mozilla Public License, v. 2.0.
////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

package net.sf.saxon.ma.map;

import net.sf.saxon.expr.OperandRole;
import net.sf.saxon.expr.XPathContext;
import net.sf.saxon.expr.sort.AtomicComparer;
import net.sf.saxon.expr.sort.AtomicMatchKey;
import net.sf.saxon.expr.sort.CodepointMatchKey;
import net.sf.saxon.functions.Count;
import net.sf.saxon.om.*;
import net.sf.saxon.trace.ExpressionPresenter;
import net.sf.saxon.trans.XPathException;
import net.sf.saxon.tree.iter.AtomicIterator;
import net.sf.saxon.tree.iter.SingletonIterator;
import net.sf.saxon.tree.iter.UnfailingIterator;
import net.sf.saxon.type.AtomicType;
import net.sf.saxon.type.BuiltInAtomicType;
import net.sf.saxon.type.UType;
import net.sf.saxon.value.*;
import net.sf.saxon.value.StringValue;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.TreeMap;

/**
 * This class implements an XPath map item as a view of an XSLT key index. It is possible to specify a minimum
 * and maximum key value to be included in the map, and keys are returned in sorted order.
 * <p/>
 * <p>At present range keys are only available for string-valued keys using the Unicode codepoint collating sequence.</p>
 */

public class RangeKey implements MapItem {

    private CodepointMatchKey min;
    private CodepointMatchKey max;
    private XPathContext context;
    private TreeMap<AtomicMatchKey, List<NodeInfo>> index;

    public RangeKey(String min, String max, XPathContext context, TreeMap<AtomicMatchKey, List<NodeInfo>> index) throws XPathException {
        this.min = min == null ? null : new CodepointMatchKey(min);
        this.max = max == null ? null : new CodepointMatchKey(max);
        this.index = index;
        this.context = context;
    }

    /**
     * Ask whether this function item is an array
     *
     * @return false (it is not an array)
     */
    public boolean isArray() {
        return false;
    }

    /**
     * Ask whether this function item is a map
     *
     * @return true (it is a map)
     */
    public boolean isMap() {
        return true;
    }

    /**
     * Get the roles of the arguments, for the purposes of streaming
     *
     * @return an array of OperandRole objects, one for each argument
     */
    public OperandRole[] getOperandRoles() {
        return new OperandRole[]{OperandRole.SINGLE_ATOMIC};
    }

    /**
     * Atomize the item.
     *
     * @return the result of atomization
     * @throws net.sf.saxon.trans.XPathException
     *          if atomization is not allowed
     *          for this kind of item
     */
    public AtomicSequence atomize() throws XPathException {
        throw new XPathException("Maps cannot be atomized", "FOTY0013");
    }

    /**
     * Get an entry from the Map
     *
     *
     * @param key     the value of the key
     * @return the value associated with the given key, or null if the key is not present in the map
     */
    public Sequence get(AtomicValue key)  {
        CodepointMatchKey k = new CodepointMatchKey(key.getStringValue());
        if ((min == null || min.compareTo(k) <= 0) &&
                (max == null || max.compareTo(k) >= 0)) {
            List<NodeInfo> nodes = index.get(k);
            if (nodes == null) {
                return EmptySequence.getInstance();
            } else {
                return SequenceExtent.makeSequenceExtent(nodes);
            }
        }
        return EmptySequence.getInstance();
    }

    /**
     * Get the size of the map
     *
     * @return the number of keys/entries present in this map
     */
    public int size() {
        try {
            return Count.count(keys());
        } catch (XPathException err) {
            return 0;
        }
    }

    /**
     * Ask whether the map is empty
     *
     * @return true if and only if the size of the map is zero
     */
    public boolean isEmpty() {
        return keys().next() == null;
    }

    /**
     * Get the set of all key values in the map.
     *
     * @return a set containing all the key values present in the map.
     */
    public AtomicIterator keys() {
        return new RangeKeyIterator();
    }

//    /**
//     * Ask whether the map includes calendar values with a timezone, calendar values without a timezone, or neither.
//     * Maps are not allowed to mix keys that have a timezone and keys that do not. Here "calendar values" means any
//     * of the types dateTime, date, time, gYear, gYearMonth, gMonth, gMonthDay, or gDay
//     * @return the time zone status of the map contents: always {@link TimeZoneStatus#CONTAINS_NO_CALENDAR_VALUES}
//     */
//
//    public TimeZoneStatus getTimeZoneStatus() {
//        return TimeZoneStatus.CONTAINS_NO_CALENDAR_VALUES;
//    }

    /**
     * Get the set of all key-value pairs in the map
     *
     * @return an iterator over the key-value pairs
     */
    public Iterator<KeyValuePair> iterator() {
        List<KeyValuePair> list = new ArrayList<KeyValuePair>();
        AtomicIterator keys = keys();
        AtomicValue key;
        while ((key = keys.next()) != null) {
            list.add(new KeyValuePair(key, get(key)));
        }
        return list.iterator();
    }

    /**
     * Remove an entry from the map
     *
     * @param key     the key of the entry to be removed
     * @return a new map in which the requested entry has been removed; or this map
     *         unchanged if the specified key was not present
     * @throws net.sf.saxon.trans.XPathException
     *          if a dynamic error occurs
     */
    public MapItem remove(AtomicValue key) throws XPathException {
        HashTrieMap copy = HashTrieMap.copy(this, context);
        return copy.remove(key);
    }

    /**
     * Get the lowest common item type of the keys in the map
     *
     * @return the most specific type to which all the keys belong. If the map is
     *         empty, return AnyAtomicType by convention
     */
    public AtomicType getKeyType() {
        return BuiltInAtomicType.STRING;
    }

    /**
     * Get the lowest common item type of the keys in the map
     *
     * @return the most specific type to which all the keys belong. If the map is
     *         empty, return ErrorType.getInstance() (the type with no instances)
     */
    public UType getKeyUType() {
        return UType.STRING;
    }

    /**
     * Get the lowest common sequence type of all the values in the map
     *
     * @return the most specific sequence type to which all the values belong. If the map
     *         is empty, return ANY_SEQUENCE by convention
     */
    public SequenceType getValueType() {
        return SequenceType.NODE_SEQUENCE;
    }

    /**
     * Get the item type of the function item
     *
     * @return the function item's type
     */
    public MapType getFunctionItemType() {
        return new MapType(BuiltInAtomicType.STRING, SequenceType.NODE_SEQUENCE);
    }

    /**
     * Get the name of the function, or null if it is anonymous
     *
     * @return the function name, or null for an anonymous inline function
     */
    public StructuredQName getFunctionName() {
        return null;
    }

    /**
     * Get a description of this function for use in error messages. For named functions, the description
     * is the function name (as a lexical QName). For others, it might be, for example, "inline function",
     * or "partially-applied ends-with function".
     *
     * @return a description of the function for use in error messages
     */
    public String getDescription() {
        return "range key";
    }

    /**
     * Get the arity of the function
     *
     * @return the number of arguments in the function signature
     */
    public int getArity() {
        return 1;
    }

    /**
     * Invoke the function
     *
     * @param context the XPath dynamic evaluation context
     * @param args    the actual arguments to be supplied
     * @return the result of invoking the function
     * @throws net.sf.saxon.trans.XPathException
     *          if a dynamic error occurs within the function
     */
    public Sequence call(XPathContext context, Sequence[] args) throws XPathException {
        return get((AtomicValue) args[0].head());
    }

    /**
     * Test whether this FunctionItem is deep-equal to another function item,
     * under the rules of the deep-equal function
     *
     * @param other    the other function item
     * @param context  the dynamic evaluation context
     * @param comparer the object to perform the comparison
     * @param flags    options for how the comparison is performed
     * @return true if the two function items are deep-equal
     * @throws net.sf.saxon.trans.XPathException
     *          if the comparison cannot be performed
     */
    public boolean deepEquals(Function other, XPathContext context, AtomicComparer comparer, int flags) throws XPathException {
        if (other instanceof RangeKey) {
            RangeKey rk = (RangeKey) other;
            return min.equals(rk.min) && max.equals(rk.max) && index.equals(rk.index);
        } else {
            return false;
        }
    }

    /**
     * Get the n'th item in the value, counting from 0
     *
     * @param n the index of the required item, with 0 representing the first item in the sequence
     * @return the n'th item if it exists, or null otherwise
     */
    public Function itemAt(int n) {
        return this;
    }

    /**
     * Get a subsequence of the value
     *
     * @param start  the index of the first item to be included in the result, counting from zero.
     *               A negative value is taken as zero. If the value is beyond the end of the sequence, an empty
     *               sequence is returned
     * @param length the number of items to be included in the result. Specify Integer.MAX_VALUE to
     *               get the subsequence up to the end of the base sequence. If the value is negative, an empty sequence
     *               is returned. If the value goes off the end of the sequence, the result returns items up to the end
     *               of the sequence
     * @return the required subsequence. If min is
     */
    public GroundedValue subsequence(int start, int length) {
        if (start <= 0 && length >= 1) {
            return this;
        } else {
            return EmptySequence.getInstance();
        }
    }

    /**
     * Get the size of the value (the number of items)
     *
     * @return the number of items in the sequence
     */
    public int getLength() {
        return 1;
    }

    /**
     * Get the effective boolean value of this sequence
     *
     * @return the effective boolean value
     * @throws net.sf.saxon.trans.XPathException
     *          if the sequence has no effective boolean value (for example a sequence of two integers)
     */
    public boolean effectiveBooleanValue() throws XPathException {
        throw new XPathException("The effective boolean value of a map is not defined");
    }

    /**
     * Reduce the sequence to its simplest form. If the value is an empty sequence, the result will be
     * EmptySequence.getInstance(). If the value is a single atomic value, the result will be an instance
     * of AtomicValue. If the value is a single item of any other kind, the result will be an instance
     * of SingletonItem. Otherwise, the result will typically be unchanged.
     *
     * @return the simplified sequence
     */
    public GroundedValue reduce() {
        return this;
    }

    /**
     * Get the value of the item as a string. For nodes, this is the string value of the
     * node as defined in the XPath 2.0 data model, except that all nodes are treated as being
     * untyped: it is not an error to get the string value of a node with a complex type.
     * For atomic values, the method returns the result of casting the atomic value to a string.
     * <p/>
     * If the calling code can handle any CharSequence, the method {@link #getStringValueCS} should
     * be used. If the caller requires a string, this method is preferred.
     *
     * @return the string value of the item
     * @throws UnsupportedOperationException if the item is a function item (an unchecked exception
     *                                       is used here to avoid introducing exception handling to a large number of paths where it is not
     *                                       needed)
     * @see #getStringValueCS
     * @since 8.4
     */
    public String getStringValue() {
        throw new UnsupportedOperationException("Cannot get the string value of a map");
    }

    /**
     * Get the string value of the item as a CharSequence. This is in some cases more efficient than
     * the version of the method that returns a String. The method satisfies the rule that
     * <code>X.getStringValueCS().toString()</code> returns a string that is equal to
     * <code>X.getStringValue()</code>.
     * <p/>
     * Note that two CharSequence values of different types should not be compared using equals(), and
     * for the same reason they should not be used as a key in a hash table.
     * <p/>
     * If the calling code can handle any CharSequence, this method should
     * be used. If the caller requires a string, the {@link #getStringValue} method is preferred.
     *
     * @return the string value of the item
     * @throws UnsupportedOperationException if the item is a function item (an unchecked exception
     *                                       is used here to avoid introducing exception handling to a large number of paths where it is not
     *                                       needed)
     * @see #getStringValue
     * @since 8.4
     */
    public CharSequence getStringValueCS() {
        return getStringValue();
    }

    /**
     * Get the first item in the sequence.
     *
     * @return the first item in the sequence if there is one, or null if the sequence
     *         is empty
     */
    public Function head() {
        return this;
    }

    /**
     * Get an iterator over all the items in the sequence
     *
     * @return an iterator over all the items
     */
    public UnfailingIterator iterate() {
        return SingletonIterator.makeIterator(this);
    }

    /**
     * Output information about this function item to the diagnostic explain() output
     */
    public void export(ExpressionPresenter out) throws XPathException {
        out.startElement("range-key-map");
        out.emitAttribute("size", size() + "");
        out.endElement();
    }

    public boolean isTrustedResultType() {
        return false;
    }

    private class RangeKeyIterator implements AtomicIterator {

        int pos = 0;
        CodepointMatchKey curr = null;
        CodepointMatchKey top;

        public RangeKeyIterator() {
            top = (CodepointMatchKey) (max == null ? index.lastKey() : index.floorKey(max));
        }

        /**
         * Get the next item in the sequence. <BR>
         *
         * @return the next Item. If there are no more nodes, return null.
         */
        public StringValue next() {
            if (pos <= 0) {
                if (pos < 0) {
                    return null;
                }
                if (min == null) {
                    curr = (CodepointMatchKey) index.firstKey();
                } else {
                    curr = (CodepointMatchKey) index.ceilingKey(min);
                    if(curr != null && max != null && curr.compareTo(max) > 0) {
                        curr = null;
                    }
                }
            } else if (curr.equals(top)) {
                curr = null;
            } else {
                curr = (CodepointMatchKey) index.higherKey(curr);
            }
            if (curr == null) {
                pos = -1;
                return null;
            } else {
                pos++;
                return new StringValue(curr.toString());
            }
        }

        /**
         * Get another iterator over the same sequence of items, positioned at the
         * start of the sequence. It must be possible to call this method at any time, whether
         * none, some, or all of the items in the original iterator have been read. The method
         * is non-destructive: it does not change the state of the original iterator.
         *
         * @return a new iterator over the same sequence
         */
        public AtomicIterator getAnother() {
            return new RangeKeyIterator();
        }

        /**
         * Close the iterator. This indicates to the supplier of the data that the client
         * does not require any more items to be delivered by the iterator. This may enable the
         * supplier to release resources. After calling close(), no further calls on the
         * iterator should be made; if further calls are made, the effect of such calls is undefined.
         * <p/>
         * <p>(Currently, closing an iterator is important only when the data is being "pushed" in
         * another thread. Closing the iterator terminates that thread and means that it needs to do
         * no additional work. Indeed, failing to close the iterator may cause the push thread to hang
         * waiting for the buffer to be emptied.)</p>
         *
         * @since 9.1
         */
        public void close() {
            //
        }

        /**
         * Get properties of this iterator, as a bit-significant integer.
         *
         * @return the properties of this iterator. This will be some combination of
         *         properties such as {@link #GROUNDED}, {@link #LAST_POSITION_FINDER},
         *         and {@link #LOOKAHEAD}. It is always
         *         acceptable to return the value zero, indicating that there are no known special properties.
         *         It is acceptable for the properties of the iterator to change depending on its state.
         * @since 8.6
         */
        public int getProperties() {
            return 0;
        }
    }
}

// Copyright (c) 2012 Saxonica Limited. All rights reserved.