////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
// Copyright (c) 2015 Saxonica Limited.
// This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
// If a copy of the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
// This Source Code Form is "Incompatible With Secondary Licenses", as defined by the Mozilla Public License, v. 2.0.
////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

package net.sf.saxon.ma.map;

import net.sf.saxon.ma.trie.ImmutableHashTrieMap;
import net.sf.saxon.ma.trie.ImmutableMap;
import net.sf.saxon.ma.trie.Option;
import net.sf.saxon.ma.trie.Tuple2;
import net.sf.saxon.expr.OperandRole;
import net.sf.saxon.expr.StaticProperty;
import net.sf.saxon.expr.XPathContext;
import net.sf.saxon.expr.sort.AtomicComparer;
import net.sf.saxon.expr.sort.AtomicMatchKey;
import net.sf.saxon.functions.DeepEqual;
import net.sf.saxon.om.*;
import net.sf.saxon.trace.ExpressionPresenter;
import net.sf.saxon.trans.NoDynamicContextException;
import net.sf.saxon.trans.XPathException;
import net.sf.saxon.tree.iter.AtomicIterator;
import net.sf.saxon.type.*;
import net.sf.saxon.value.AtomicValue;
import net.sf.saxon.value.Cardinality;
import net.sf.saxon.value.EmptySequence;
import net.sf.saxon.value.SequenceType;

import java.util.Iterator;

/**
 * An immutable map. This is the Saxon 9.6 implementation, which uses a hash trie
 */
public class HashTrieMap extends AbstractItem implements MapItem, GroundedValue {

    public final static SequenceType SINGLE_MAP_TYPE = SequenceType.makeSequenceType(
            MapType.ANY_MAP_TYPE, StaticProperty.EXACTLY_ONE);

    // The underlying trie holds key-value pairs, but these do not correspond directly
    // to the key value pairs in the XDM map. Instead, the key in the trie is an AtomicMatchKey
    // based on the XDM key, which allows equality matching to differ from the Java-level
    // equals method (to take account of collations, etc). The value in the trie is
    // actually a tuple holding both the real key, and the value.

    private ImmutableMap<AtomicMatchKey, KeyValuePair> imap;

    // The following values are null if unknown; they are then maintained incrementally when
    // entries are added to the map, but are reset to null when values are removed from the map.
    private AtomicType keyType;
    private ItemType valueType;
    private int valueCardinality;
    private int entries = -1;
    private XPathContext context; // used only to get the TypeHierarchy for the Configuration

    // Count of CalendarValue keys: positive if they all have a timezone, negative if they all have no timezone
    //private int timezoneCount = 0;

    /**
     * Create an empty map
     * @param context The XPath dynamic context. Gives access to type information.
     */

    public HashTrieMap(XPathContext context) {
        setContext(context);
        this.imap = ImmutableHashTrieMap.empty();
        this.entries = 0;
    }

    /**
     * Create a singleton map with a single key and value
     * @param key the key value
     * @param value the associated value
     * @param context dynamic evaluation context. Gives access to type information.
     * @return a singleton map
     * @throws XPathException if map mixes timezoned and timezoneless values
     */

    public static HashTrieMap singleton(AtomicValue key, Sequence value, XPathContext context) throws XPathException {
        return new HashTrieMap(context).addEntry(key, value);
    }

    /**
     * Create a map whose contents are a copy of an existing immutable map
     * @param imap the map to be copied
     * @param context The XPath dynamic context. Used to define the implicit timezone for comparing
     * keys in this map, and also gives access to type information.
     * @throws NoDynamicContextException if the supplied context has no implicit timezone available
     */

    public HashTrieMap(ImmutableMap<AtomicMatchKey, KeyValuePair> imap, XPathContext context)  throws NoDynamicContextException {
        setContext(context);
        this.imap = imap;
        entries = -1;
    }

    /**
     * Create a map whose entries are copies of the entries in an existing MapItem
     *
     * @param map       the existing map to be copied
     * @param context   the XPath dynamic context
     * @return the new map
     * @throws net.sf.saxon.trans.XPathException
     *          if a dynamic error occurs
     */

    public static HashTrieMap copy(MapItem map, XPathContext context) throws XPathException {
        if (map instanceof HashTrieMap) {
            return (HashTrieMap)map;
        }
        HashTrieMap m2 = new HashTrieMap(context);
        ImmutableHashTrieMap<AtomicMatchKey, KeyValuePair> imap  = ImmutableHashTrieMap.empty();
        int entries = 0;
        for (KeyValuePair pair : map) {
            imap = imap.put(m2.makeKey(pair.key), pair);
            entries++;
        }
        m2.imap = imap;
        m2.entries = entries;
        m2.computeTypeInformation();
        return m2;
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
     * Atomize the item.
     *
     * @return the result of atomization
     * @throws XPathException if atomization is not allowed for this kind of item
     */
    public AtomicSequence atomize() throws XPathException {
        throw new XPathException("Maps cannot be atomized", "FOTY0013");
    }

    private void setContext(XPathContext context) {
        this.context = context;
    }

    private void updateTypeInformation(AtomicValue key, Sequence val) {
        TypeHierarchy th = context.getConfiguration().getTypeHierarchy();
        if (keyType == null) {
            keyType = key.getItemType();
            valueType = SequenceTool.getItemType(val, th);
            valueCardinality = SequenceTool.getCardinality(val);
        } else {
            keyType = (AtomicType) Type.getCommonSuperType(keyType, key.getItemType(), th);
            valueType = Type.getCommonSuperType(valueType, SequenceTool.getItemType(val, th), th);
            valueCardinality = Cardinality.union(valueCardinality, SequenceTool.getCardinality(val));
        }
    }

    /**
     * Get the size of the map
     */

    public int size() {
        if (entries >= 0) {
            return entries;
        }
        int count = 0;
        //noinspection UnusedDeclaration
        for (KeyValuePair entry: this) {
            count++;
        }
        return entries = count;
    }

    /**
     * Ask whether the map is empty
     *
     * @return true if and only if the size of the map is zero
     */
    public boolean isEmpty() {
        return entries == 0 || !imap.iterator().hasNext();
    }

    /**
     * Get the lowest common item type of the keys in the map
     *
     * @return the most specific type to which all the keys belong. If the map is
     *         empty, return AnyAtomicType by convention
     */
    public AtomicType getKeyType() {
        if (isEmpty()) {
            return ErrorType.getInstance();
        }
        computeTypeInformation();
        return keyType;
    }

    /**
     * Get the lowest common item type of the keys in the map
     *
     * @return the most specific type to which all the keys belong. If the map is
     *         empty, return ErrorType.getInstance() (the type with no instances)
     */
    public UType getKeyUType() {
        // temporary
        return UType.ANY_ATOMIC.except(UType.QNAME).except(UType.NOTATION);
    }

//    /**
//     * Ask whether the map includes calendar values with a timezone, calendar values without a timezone, or neither.
//     * Maps are not allowed to mix keys that have a timezone and keys that do not. Here "calendar values" means any
//     * of the types dateTime, date, time, gYear, gYearMonth, gMonth, gMonthDay, or gDay
//     *
//     * @return the time zone status of the map contents.
//     */
//    public TimeZoneStatus getTimeZoneStatus() {
//        if (timezoneCount == 0) {
//            return TimeZoneStatus.CONTAINS_NO_CALENDAR_VALUES;
//        } else if (timezoneCount > 0) {
//            return TimeZoneStatus.CONTAINS_CALENDAR_VALUES_WITH_TIMEZONE;
//        } else {
//            return TimeZoneStatus.CONTAINS_CALENDAR_VALUES_WITHOUT_TIMEZONE;
//        }
//    }

    private void computeTypeInformation() {
        if (keyType == null) {
            TypeHierarchy th = context.getConfiguration().getTypeHierarchy();
            for (KeyValuePair kvp : this) {
                AtomicValue key = kvp.key;
                Sequence value = kvp.value;
                if (keyType == null) {
                    keyType = key.getItemType();
                    valueType = SequenceTool.getItemType(value, th);
                    valueCardinality = SequenceTool.getCardinality(value);
                } else {
                    updateTypeInformation(key, value);
                }
            }
        }
    }

    /**
     * Get the lowest common sequence type of all the values in the map
     *
     * @return the most specific sequence type to which all the values belong. If the map
     *         is empty, return SequenceType.VOID
     */
    public SequenceType getValueType() {
        if (isEmpty()) {
            return SequenceType.VOID;
        }
        computeTypeInformation();
        return SequenceType.makeSequenceType(valueType, valueCardinality);
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
     * Create a new map containing the existing entries in the map plus an additional entry,
     * without modifying the original. If there is already an entry with the specified key,
     * this entry is replaced by the new entry.
     *
     * @param key     the key of the new entry
     * @param value   the value associated with the new entry
     * @return the new map containing the additional entry
     * @throws XPathException if adding the entry violates the rule against mixing keys with timezones
     * and keys with no timezone
     */

    public HashTrieMap addEntry(AtomicValue key, Sequence value) throws XPathException {
        ImmutableMap<AtomicMatchKey, KeyValuePair> imap2 = imap.put(makeKey(key), new KeyValuePair(key, value));
        HashTrieMap t2;
        try {
            t2 = new HashTrieMap(imap2, context);
        } catch (NoDynamicContextException e) {
            // Can't happen, we've already checked the context for suitability
            throw new IllegalStateException(e);
        }
        t2.valueCardinality = this.valueCardinality;
        t2.keyType = keyType;
        t2.valueType = valueType;
        t2.updateTypeInformation(key, value);
//        t2.timezoneCount = timezoneCount;
//        if (key instanceof CalendarValue) {
//            if (((CalendarValue)key).hasTimezone()) {
//                if (timezoneCount >= 0) {
//                    t2.timezoneCount = timezoneCount + 1;
//                } else {
//                    throw new XPathException("Cannot mix values with timezone and values without timezone in the same map", "XTDE3368");
//                }
//            } else {
//                if (timezoneCount <= 0) {
//                    t2.timezoneCount = timezoneCount - 1;
//                } else {
//                    throw new XPathException("Cannot mix values with timezone and values without timezone in the same map", "XTDE3368");
//                }
//            }
//        }
        return t2;
    }

    /**
     * Add a new entry to this map. Since the map is supposed to be immutable, this method
     * must only be called while initially populating the map, and must not be called if
     * anyone else might already be using the map.
     *
     * @param key     the key of the new entry. Any existing entry with this key is replaced.
     * @param value   the value associated with the new entry
     * @return true if an existing entry with the same key was replaced
     */

    public boolean initialPut(AtomicValue key, Sequence value) {
        boolean exists = get(key) != null;
        imap = imap.put(makeKey(key), new KeyValuePair(key, value));
        updateTypeInformation(key, value);
        entries = -1;
        return exists;
    }

    private AtomicMatchKey makeKey(AtomicValue key) {
        return key.asMapKey();
    }


    /**
     * Remove an entry from the map
     *
     * @param key     the key of the entry to be removed
     * @return a new map in which the requested entry has been removed; or this map
     *         unchanged if the specified key was not present
     */

    public HashTrieMap remove(AtomicValue key) throws XPathException {
        ImmutableMap<AtomicMatchKey, KeyValuePair> m2 = imap.remove(makeKey(key));
        if (m2 == imap) {
            // The key is not present; the map is unchanged
            return this;
        }
        HashTrieMap map = new HashTrieMap(m2, context);
//        if (key instanceof CalendarValue) {
//            if (((CalendarValue)key).hasTimezone()) {
//                map.timezoneCount = this.timezoneCount - 1;
//            } else {
//                map.timezoneCount = this.timezoneCount + 1;
//            }
//        } else {
//            map.timezoneCount = this.timezoneCount;
//        }
        return map;
    }

    /**
     * Get an entry from the Map
     *
     * @param key     the value of the key
     * @return the value associated with the given key, or null if the key is not present in the map
     */

    public Sequence get(AtomicValue key)  {
        Option<KeyValuePair> o = imap.get(makeKey(key));
        if (o.isDefined()) {
            return o.get().value;
        } else {
            return null;
        }
    }

    /**
     * Get an key/value pair from the Map
     *
     * @param key     the value of the key
     * @return the key-value-pair associated with the given key, or null if the key is not present in the map
     */

    public KeyValuePair getKeyValuePair(AtomicValue key) {
        Option<KeyValuePair> kvp = imap.get(makeKey(key));
        if (kvp.isDefined()) {
            return kvp.get();
        } else {
            return null;
        }
    }

    /**
     * Get the set of all key values in the map
     * @return an iterator over the keys, in undefined order
     */

    public AtomicIterator keys() {
        return new AtomicIterator() {

            int pos = 0;
            Iterator<Tuple2<AtomicMatchKey, KeyValuePair>> base = imap.iterator();

            public AtomicValue next() {
                if (base.hasNext()) {
                    AtomicValue curr = base.next()._2.key;
                    pos++;
                    return curr;
                } else {
                    pos = -1;
                    return null;
                }
            }

            public AtomicIterator getAnother() {
                return keys();
            }

            public void close() {
            }

            public int getProperties() {
                return 0;
            }
        };

    }

    /**
     * Get the set of all key-value pairs in the map
     *
     * @return an iterator over the key-value pairs
     */
    public Iterator<KeyValuePair> iterator() {
        return new Iterator<KeyValuePair>() {

            Iterator<Tuple2<AtomicMatchKey, KeyValuePair>> base = imap.iterator();

            public boolean hasNext() {
                return base.hasNext();
            }

            public KeyValuePair next() {
                return base.next()._2;
            }

            public void remove() {
                base.remove();
            }
        };

    }

    /**
     * Get the item type of this item as a function item. Note that this returns the generic function
     * type for maps, not a type related to this specific map.
     *
     * @return the function item's type
     */
    public FunctionItemType getFunctionItemType(/*@Nullable*/) {
        //return new MapType(getKeyType(), getValueType());
        return MapType.ANY_MAP_TYPE;
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
        return "map";
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
     * @throws net.sf.saxon.trans.XPathException if an error occurs evaluating
     * the supplied argument
     *
     */
    public Sequence call(XPathContext context, Sequence[] args) throws XPathException {
        AtomicValue key = (AtomicValue) args[0].head();
        Sequence value = get(key);
        if (value == null) {
            return EmptySequence.getInstance();
        } else {
            return value;
        }
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
        throw new UnsupportedOperationException("A map has no string value");
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
        throw new UnsupportedOperationException("A map has no string value");
    }

    /**
     * Get the typed value of the item.
     * <p/>
     * For a node, this is the typed value as defined in the XPath 2.0 data model. Since a node
     * may have a list-valued data type, the typed value is in general a sequence, and it is returned
     * in the form of a SequenceIterator.
     * <p/>
     * If the node has not been validated against a schema, the typed value
     * will be the same as the string value, either as an instance of xs:string or as an instance
     * of xs:untypedAtomic, depending on the node kind.
     * <p/>
     * For an atomic value, this method returns an iterator over a singleton sequence containing
     * the atomic value itself.
     *
     * @return an iterator over the items in the typed value of the node or atomic value. The
     *         items returned by this iterator will always be atomic values.
     * @throws net.sf.saxon.trans.XPathException
     *          where no typed value is available, for example in the case of
     *          an element with complex content
     * @since 8.4
     */
    public SequenceIterator getTypedValue() throws XPathException {
        throw new XPathException("A map has no typed value");
    }

    /**
     * Test whether this FunctionItem is deep-equal to another function item,
     * under the rules of the deep-equal function
     *
     * @param other the other function item
     */
    public boolean deepEquals(Function other, XPathContext context, AtomicComparer comparer, int flags) throws XPathException {
        if (other instanceof MapItem &&
                ((MapItem) other).size() == size()) {
            AtomicIterator keys = keys();
            AtomicValue key;
            while ((key = keys.next()) != null) {
                Sequence thisValue = get(key);
                Sequence otherValue = ((MapItem) other).get(key);
                if (otherValue == null) {
                    return false;
                }
                if (!DeepEqual.deepEqual(otherValue.iterate(),
                    thisValue.iterate(), comparer, context, flags)) {
                    return false;
                }
            }
            return true;
        }
        return false;
    }

    /*@Nullable*/
    public MapItem itemAt(int n) {
        return n == 0 ? this : null;
    }

    public boolean effectiveBooleanValue() throws XPathException {
        throw new XPathException("A map item has no effective boolean value");
    }

    /**
     * Output information about this function item to the diagnostic explain() output
     */
    public void export(ExpressionPresenter out) throws XPathException {
        out.startElement("map");
        out.emitAttribute("size", size() + "");
        out.endElement();
    }

    public boolean isTrustedResultType() {
        return false;
    }
}

// Copyright (c) 2015 Saxonica Limited. All rights reserved.



