package net.sf.saxon.expr.sort;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * An LRU cache, based on <code>LinkedHashMap</code>.
 * Synthesized and simplified from various published examples of the genre.
 * The methods are not synchronized.
 */
public class LRUCache<K,V> {

    private Map<K,V> map;

    /**
     * Creates a new LRU cache.
     *
     * @param cacheSize the maximum number of entries that will be kept in this cache.
     */
    public LRUCache(final int cacheSize) {
        this(cacheSize, false);
    }

    /**
     * Creates a new LRU cache with the option of synchronization.
     *
     * @param cacheSize the maximum number of entries that will be kept in this cache.
     */
    public LRUCache(final int cacheSize, boolean concurrent) {
        map = new LinkedHashMap<K,V>(cacheSize, 0.75f, true) {
            protected boolean removeEldestEntry(Map.Entry eldest) {
                return cacheSize < super.size();
            }
        };
        if (concurrent) {
            map = Collections.synchronizedMap(map);
        }
    }

    /**
     * Retrieves an entry from the cache.<br>
     * The retrieved entry becomes the most recently used entry.
     *
     * @param key the key whose associated value is to be returned.
     * @return the value associated to this key, or null if no value with this key exists in the cache.
     */
    public V get(K key) {
        return map.get(key);
    }

    /**
     * Adds an entry to this cache.
     * If the cache is full, the LRU (least recently used) entry is dropped.
     *
     * @param key   the key with which the specified value is to be associated.
     * @param value a value to be associated with the specified key.
     */
    public void put(K key, V value) {
        map.put(key, value);
    }

    /**
     * Clear the cache
     */
    public void clear() {
        map.clear();
    }

    /**
     * Get the number of entries in the cache
     */

    public int size() {
        return map.size();
    }

//    public static void main(String[] args) {
//        ExecutorService executor = Executors.newFixedThreadPool(10);
//        for (int i = 0; i < 100; i++) {
//            executor.execute(new Runnable() {
//                public void run() {
//                    for (int i=0; i<1000000; i++) {
//                        String uri = "http://a.com/aaa" + i;
//                        boolean b = AnyURIValue.isValidURI(uri);
//                        if (i % 1000 == 0) {
//                            System.err.println("Memory: " + (Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory()));
//                        }
//                    }
//                }
//            });
//        }
//    }

    


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