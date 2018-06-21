////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
// Copyright (c) 2017 Saxonica Limited.
// This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
// If a copy of the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
// This Source Code Form is "Incompatible With Secondary Licenses", as defined by the Mozilla Public License, v. 2.0.
////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

package net.sf.saxon.s9api;

import net.sf.saxon.ma.map.HashTrieMap;
import net.sf.saxon.ma.map.KeyValuePair;
import net.sf.saxon.ma.map.MapItem;
import net.sf.saxon.om.Sequence;
import net.sf.saxon.tree.iter.AtomicIterator;
import net.sf.saxon.value.AtomicValue;
import org.jetbrains.annotations.NotNull;

import java.util.*;

/**
 * A map in the XDM data model. A map is a list of zero or more entries, each of which
 * is a pair comprising a key (which is an atomic value) and a value (which is an arbitrary value).
 * The map itself is an XDM item.
 * <p/>
 * An XdmMap is immutable.
 * <p/>
 * As originally issued in Saxon 9.8, this class implemented the {@link java.util.Map} interface.
 * It no longer does so, because it was found that the methods {@link #size()}, {@link #put(XdmAtomicValue, XdmValue)},
 * and {@link #remove} did not adhere to the semantic contract of that interface. Instead, it is now
 * possible to obtain a view of this object as an immutable Java {@link java.util.Map} by calling the
 * method {@link #asImmutableMap()}. See bug 3824.
 * @since 9.8
 */

public class XdmMap extends XdmFunctionItem {

    /**
     * Create an empty XdmMap
     */

    public XdmMap() {
        setValue(new HashTrieMap());
    }

    /**
     * Create an XdmMap whose underlying value is a MapItem
     * @param map the MapItem to be encapsulated
     */

    public XdmMap(MapItem map) {
        super(map);
    }

    /**
     * Create an XdmMap supplying the entries in the form of a Java Map,
     * where the keys and values in the Java Map are XDM values
     *
     * @param map a Java map whose entries are the (key, value) pairs
     * @since 9.8
     */

    public XdmMap(Map<? extends XdmAtomicValue, ? extends XdmValue> map) {
        HashTrieMap val = new HashTrieMap();
        for (Map.Entry<? extends XdmAtomicValue, ? extends XdmValue> entry : map.entrySet()) {
            val.initialPut(entry.getKey().getUnderlyingValue(), entry.getValue().getUnderlyingValue());
        }
        setValue(val);
    }

    /**
     * Get the underlying implementation object representing the value. This method allows
     * access to lower-level Saxon functionality, including classes and methods that offer
     * no guarantee of stability across releases.
     *
     * @return the underlying implementation object representing the value
     * @since 9.8 (previously inherited from XdmValue which returns a Sequence)
     */
    @Override
    public MapItem getUnderlyingValue() {
        return (MapItem)super.getUnderlyingValue();
    }

    /**
     * Get the number of entries in the map
     *
     * @return the number of entries in the map. (Note that the {@link #size()} method returns 1 (one),
     * because an XDM map is an item.)
     */

    public int mapSize() {
        return getUnderlyingValue().size();
    }

    /**
     * Create a new map containing an additional (key, value) pair.
     * If there is an existing entry with the same key, it is removed
     * @return a new map containing the additional entry. The original map is unchanged.
     */

    public XdmMap put(XdmAtomicValue key, XdmValue value) {
        XdmMap map2 = new XdmMap();
        map2.setValue(getUnderlyingValue().addEntry(key.getUnderlyingValue(), value.getUnderlyingValue()));
        return map2;
    }

    /**
     * Create a new map in which the entry for a given key has been removed.
     * If there is no entry with the same key, the new map has the same content as the old (it may or may not
     * be the same Java object)
     *
     * @return a map without the specified entry. The original map is unchanged.
     */

    public XdmMap remove(XdmAtomicValue key) {
        XdmMap map2 = new XdmMap();
        map2.setValue(getUnderlyingValue().remove(key.getUnderlyingValue()));
        return map2;
    }

    /**
     * Get the keys present in the map in the form of a list.
     * @return a list of the keys present in this map, in arbitrary order.
     */
    @NotNull
    public Set<XdmAtomicValue> keySet() {
        Set<XdmAtomicValue> result = new HashSet<XdmAtomicValue>();
        AtomicIterator iter = getUnderlyingValue().keys();
        AtomicValue key;
        while ((key = iter.next()) != null) {
            result.add((XdmAtomicValue)XdmValue.wrap(key));
        }
        return result;
    }

    /**
     * Return this map as an immutable instance of {@link java.util.Map}
     *
     * @return an immutable instance of {@link java.util.Map} backed by this map.
     * Methods such as {@link #remove} and {@link #put} applied to the result will
     * always throw {@link UnsupportedOperationException}.
     */

    public Map<XdmAtomicValue, XdmValue> asImmutableMap() {
        // See bug 3824
        final XdmMap base = this;
        return new AbstractMap<XdmAtomicValue, XdmValue>() {
            @Override
            public Set<Entry<XdmAtomicValue, XdmValue>> entrySet() {
                return base.entrySet();
            }

            @Override
            public int size() {
                return base.mapSize();
            }

            @Override
            public boolean isEmpty() {
                return base.isEmpty();
            }

            @Override
            public boolean containsValue(Object value) {
                for (Entry<XdmAtomicValue, XdmValue> e : entrySet()) {
                    if (value.equals(e.getValue())) {
                        return true;
                    }
                }
                return false;
            }

            @Override
            public boolean containsKey(Object key) {
                return base.containsKey(key);
            }

            @Override
            public XdmValue get(Object key) {
                return base.get(key);
            }

            @Override
            public XdmValue put(XdmAtomicValue key, XdmValue value) {
                throw new UnsupportedOperationException();
            }

            @Override
            public XdmValue remove(Object key) {
                throw new UnsupportedOperationException();
            }

            @Override
            public void putAll(Map<? extends XdmAtomicValue, ? extends XdmValue> m) {
                throw new UnsupportedOperationException();
            }

            @Override
            public void clear() {
                throw new UnsupportedOperationException();
            }

            @Override
            public Set<XdmAtomicValue> keySet() {
                return base.keySet();
            }

            @Override
            public Collection<XdmValue> values() {
                return base.values();
            }
        };
    }

    /**
     * Return a mutable Java Map containing the same entries as this map.
     *
     * @return a mutable Map from atomic values to (sequence) values, containing the
     * same entries as this map
     * @since 9.6.
     */

    public Map<XdmAtomicValue, XdmValue> asMap() {
        return new HashMap<XdmAtomicValue, XdmValue>(asImmutableMap());
    }


    /**
     * Removes all of the mappings from this map (optional operation).
     * The map will be empty after this call returns.
     *
     * @throws UnsupportedOperationException if the <tt>clear</tt> operation
     *                                       is not supported by this map
     */
    public void clear() {
        throw new UnsupportedOperationException("XdmMap is immutable");
    }

    /**
     * Returns <tt>true</tt> if this map contains no key-value mappings.
     *
     * @return <tt>true</tt> if this map contains no key-value mappings
     */
    public boolean isEmpty() {
        return getUnderlyingValue().isEmpty();
    }

    /**
     * Returns <tt>true</tt> if this map contains a mapping for the specified
     * key.  More formally, returns <tt>true</tt> if and only if
     * this map contains a mapping for a key <tt>k</tt> such that
     * <tt>(key==null ? k==null : key.equals(k))</tt>.  (There can be
     * at most one such mapping.)
     *
     * @param key key whose presence in this map is to be tested
     * @return <tt>true</tt> if this map contains a mapping for the specified
     * key
     * @throws ClassCastException   if the key is of an inappropriate type for
     *                              this map
     *                              (<a href="{@docRoot}/java/util/Collection.html#optional-restrictions">optional</a>)
     * @throws NullPointerException if the specified key is null and this map
     *                              does not permit null keys
     *                              (<a href="{@docRoot}/java/util/Collection.html#optional-restrictions">optional</a>)
     */
    public boolean containsKey(Object key) {
        AtomicValue k = ((XdmAtomicValue)key).getUnderlyingValue();
        return getUnderlyingValue().get(k) != null;
    }

    /**
     * Returns <tt>true</tt> if this map maps one or more keys to the
     * specified value.  More formally, returns <tt>true</tt> if and only if
     * this map contains at least one mapping to a value <tt>v</tt> such that
     * <tt>(value==null ? v==null : value.equals(v))</tt>.  This operation
     * will probably require time linear in the map size for most
     * implementations of the <tt>Map</tt> interface.
     *
     * <p>This implementation always throws UnsupportedOperationException.</p>
     *
     * @param value value whose presence in this map is to be tested
     * @return <tt>true</tt> if this map maps one or more keys to the
     * specified value
     * @throws ClassCastException   if the value is of an inappropriate type for
     *                              this map
     *                              (<a href="{@docRoot}/java/util/Collection.html#optional-restrictions">optional</a>)
     * @throws NullPointerException if the specified value is null and this
     *                              map does not permit null values
     *                              (<a href="{@docRoot}/java/util/Collection.html#optional-restrictions">optional</a>)
     */
    public boolean containsValue(Object value) {
        throw new UnsupportedOperationException("Not implemented: cannot compare sequences");
    }

    /**
     * Returns the value to which the specified key is mapped,
     * or {@code null} if this map contains no mapping for the key.
     * <p/>
     *
     * @param key the key whose associated value is to be returned. If this is
     *            not an XdmAtomicValue, the method attempts to construct an
     *            XdmAtomicValue using the method {@link XdmAtomicValue#makeAtomicValue(Object)};
     *            it is therefore possible to pass a simple key such as a string or integer.
     * @return the value to which the specified key is mapped, or
     * {@code null} if this map contains no mapping for the key
     * @throws ClassCastException   if the key is of an inappropriate type for
     *                              this map (in this case, XdmAtomicValue)
     *                              (<a href="{@docRoot}/java/util/Collection.html#optional-restrictions">optional</a>)
     * @throws NullPointerException if the specified key is null
     *                              (<a href="{@docRoot}/java/util/Collection.html#optional-restrictions">optional</a>)
     */
    public XdmValue get(Object key) {
        if (key == null) {
            throw new NullPointerException();
        }
        if (!(key instanceof XdmAtomicValue)) {
            try {
                key = XdmAtomicValue.makeAtomicValue(key);
            } catch (IllegalArgumentException err) {
                throw new ClassCastException(err.toString());
            }
        }
        AtomicValue k = ((XdmAtomicValue) key).getUnderlyingValue();
        Sequence v = getUnderlyingValue().get(k);
        return v == null ? null : XdmValue.wrap(v);
    }

    /**
     * Removes the mapping for a key from this map if it is present
     * (optional operation). This map implementation is immutable
     * so the method always throws {@link UnsupportedOperationException}.
     *
     * @param key key whose mapping is to be removed from the map
     * @return the previous value associated with <tt>key</tt>, or
     * <tt>null</tt> if there was no mapping for <tt>key</tt>.
     * @throws UnsupportedOperationException if the <tt>remove</tt> operation
     *                                       is not supported by this map
     */
    public XdmValue remove(Object key) {
        throw new UnsupportedOperationException("XdmMap is immutable");
    }

    /**
     * Copies all of the mappings from the specified map to this map
     * (optional operation).  This map implementation is immutable
     * so the method always throws {@link UnsupportedOperationException}.
     *
     * @param m mappings to be stored in this map
     * @throws UnsupportedOperationException if the <tt>putAll</tt> operation
     *                                       is not supported by this map
     */
    public void putAll(@NotNull Map<? extends XdmAtomicValue, ? extends XdmValue> m) {
        throw new UnsupportedOperationException("XdmMap is immutable");
    }

    /**
     * Returns a {@link Collection} view of the values contained in this map.
     * The collection is backed by the map, so changes to the map are
     * reflected in the collection, and vice-versa.  If the map is
     * modified while an iteration over the collection is in progress
     * (except through the iterator's own <tt>remove</tt> operation),
     * the results of the iteration are undefined.  The collection
     * supports element removal, which removes the corresponding
     * mapping from the map, via the <tt>Iterator.remove</tt>,
     * <tt>Collection.remove</tt>, <tt>removeAll</tt>,
     * <tt>retainAll</tt> and <tt>clear</tt> operations.  It does not
     * support the <tt>add</tt> or <tt>addAll</tt> operations.
     *
     * @return a collection view of the values contained in this map
     */
    @NotNull
    public Collection<XdmValue> values() {
        List<XdmValue> result = new ArrayList<XdmValue>();
        for (KeyValuePair keyValuePair : getUnderlyingValue()) {
            result.add(XdmValue.wrap(keyValuePair.value));
        }
        return result;
    }

    /**
     * Returns a {@link Set} view of the mappings contained in this map.
     *
     * @return a set view of the mappings contained in this map
     */
    @NotNull
    public Set<Map.Entry<XdmAtomicValue, XdmValue>> entrySet() {
        Set<Map.Entry<XdmAtomicValue, XdmValue>> result = new HashSet<Map.Entry<XdmAtomicValue, XdmValue>>();
        for (KeyValuePair keyValuePair : getUnderlyingValue()) {
            Map.Entry<XdmAtomicValue, XdmValue> entry = new XdmMapEntry(keyValuePair);
            result.add(entry);
        }
        return result;
    }

    /**
     * Static factory method to construct an XDM map by converting each entry
     * in a supplied Java map. The keys in the Java map must be convertible
     * to XDM atomic values using the {@link XdmAtomicValue#makeAtomicValue(Object)}
     * method. The associated values must be convertible to XDM sequences
     * using the {@link XdmValue#makeValue(Object)} method.
     * @param input the supplied map
     * @return the resulting XdmMap
     * @throws IllegalArgumentException if any value in the input map cannot be converted
     * to a corresponding XDM value.
     */

    public static XdmMap makeMap(Map input) throws IllegalArgumentException {
        HashTrieMap result = new HashTrieMap();
        for (Object entry : input.entrySet()) {
            Object key = ((Map.Entry)entry).getKey();
            Object value = ((Map.Entry)entry).getValue();
            XdmAtomicValue xKey = XdmAtomicValue.makeAtomicValue(key);
            XdmValue xValue = XdmValue.makeValue(value);
            result.initialPut(xKey.getUnderlyingValue(), xValue.getUnderlyingValue());
        }
        return new XdmMap(result);
    }

    private static class XdmMapEntry implements Map.Entry<XdmAtomicValue, XdmValue> {

        KeyValuePair pair;

        public XdmMapEntry(KeyValuePair pair) {
            this.pair = pair;
        }
        /**
         * Returns the key corresponding to this entry.
         *
         * @return the key corresponding to this entry
         */
        @Override
        public XdmAtomicValue getKey() {
            return (XdmAtomicValue)XdmValue.wrap(pair.key);
        }

        /**
         * Returns the value corresponding to this entry.
         *
         * @return the value corresponding to this entry
         */
        @Override
        public XdmValue getValue() {
            return XdmValue.wrap(pair.value);
        }

        /**
         * Replaces the value corresponding to this entry with the specified
         * value (optional operation). Always throws {@link UnsupportedOperationException}
         * @throws UnsupportedOperationException always
         */
        @Override
        public XdmValue setValue(XdmValue value) {
            throw new UnsupportedOperationException();
        }
    }
}

