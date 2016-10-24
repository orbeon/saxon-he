////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
// Copyright (c) 2015 Saxonica Limited.
// This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
// If a copy of the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
// This Source Code Form is "Incompatible With Secondary Licenses", as defined by the Mozilla Public License, v. 2.0.
////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

package net.sf.saxon.ma.map;

import net.sf.saxon.om.Function;
import net.sf.saxon.om.Sequence;
import net.sf.saxon.trans.XPathException;
import net.sf.saxon.tree.iter.AtomicIterator;
import net.sf.saxon.type.AtomicType;
import net.sf.saxon.type.UType;
import net.sf.saxon.value.AtomicValue;
import net.sf.saxon.value.SequenceType;

import java.util.Iterator;

/**
 * Interface supported by different implementations of an XDM map item
 */
public interface MapItem extends Function, Iterable<KeyValuePair> {

//    public static enum TimeZoneStatus {
//        CONTAINS_CALENDAR_VALUES_WITH_TIMEZONE,
//        CONTAINS_CALENDAR_VALUES_WITHOUT_TIMEZONE,
//        CONTAINS_NO_CALENDAR_VALUES
//    }

    /**
     * Get an entry from the Map
     *
     *
     * @param key     the value of the key
     * @return the value associated with the given key, or null if the key is not present in the map
     */

    public Sequence get(AtomicValue key);

    /**
     * Get the size of the map
     *
     * @return the number of keys/entries present in this map
     */

    public int size();

    /**
     * Ask whether the map is empty
     *
     * @return true if and only if the size of the map is zero
     */

    public boolean isEmpty();

    /**
     * Get the set of all key values in the map.
     *
     * @return a set containing all the key values present in the map, in unpredictable order
     */

    public AtomicIterator keys();

//    /**
//     * Ask whether the map includes calendar values with a timezone, calendar values without a timezone, or neither.
//     * Maps are not allowed to mix keys that have a timezone and keys that do not. Here "calendar values" means any
//     * of the types dateTime, date, time, gYear, gYearMonth, gMonth, gMonthDay, or gDay
//     * @return the time zone status of the map contents.
//     */
//
//    public TimeZoneStatus getTimeZoneStatus();

    /**
     * Get the set of all key-value pairs in the map
     * @return an iterator over the key-value pairs
     */

    public Iterator<KeyValuePair> iterator();

    /**
     * Remove an entry from the map
     *
     *
     * @param key     the key of the entry to be removed
     * @return a new map in which the requested entry has been removed; or this map
     *         unchanged if the specified key was not present
     * @throws net.sf.saxon.trans.XPathException
     *          if a dynamic error occurs
     */

    public MapItem remove(AtomicValue key) throws XPathException;

    /**
     * Get the lowest common item type of the keys in the map
     *
     * @return the most specific type to which all the keys belong. If the map is
     *         empty, return ErrorType.getInstance() (the type with no instances)
     */

    public AtomicType getKeyType();

    /**
     * Get the lowest common item type of the keys in the map
     *
     * @return the most specific type to which all the keys belong. If the map is
     *         empty, return ErrorType.getInstance() (the type with no instances)
     */

    public UType getKeyUType();

    /**
     * Get the lowest common sequence type of all the values in the map
     *
     * @return the most specific sequence type to which all the values belong. If the map
     *         is empty, return SequenceType.VOID
     */

    public SequenceType getValueType();
}

// Copyright (c) 2011-2014 Saxonica Limited. All rights reserved.
