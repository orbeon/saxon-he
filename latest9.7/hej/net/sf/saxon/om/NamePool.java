////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
// Copyright (c) 2015 Saxonica Limited.
// This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
// If a copy of the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
// This Source Code Form is "Incompatible With Secondary Licenses", as defined by the Mozilla Public License, v. 2.0.
////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

package net.sf.saxon.om;

import net.sf.saxon.lib.NamespaceConstant;
import net.sf.saxon.tree.wrapper.VirtualNode;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;


/**
 * A NamePool holds a collection of expanded names, each containing a namespace URI,
 * a namespace prefix, and a local name.
 * <p/>
 * <p>Each expanded name is allocated a unique integer namecode. The namecode enables
 * all three parts of the expanded name to be determined, that is, the prefix, the
 * URI, and the local name.</p>
 * <p/>
 * <p>The equivalence between names depends only on the URI and the local name.
 * The namecode is designed so that if two namecodes represent names with the same
 * URI and local name, the two namecodes are the same in the bottom 20 bits. It is
 * therefore possible to compare two names for equivalence by performing an integer
 * comparison of these 20 bits. The bottom 20 bits of a namecode are referred to as
 * a fingerprint.</p>
 * <p/>
 * <p>The NamePool eliminates duplicate names if they have the same prefix, uri,
 * and local part. It retains duplicates if they have different prefixes</p>
 * <p/>
 * <p>The NamePool has been redesigned in Saxon 9.7 to make use of two Java
 * ConcurrentHashMaps, one from QNames to integers, one from integers to QNames.
 * This gives better scaleability in terms of multithreaded concurrency and in terms
 * of the capacity of the NamePool and retention of performance as the size of
 * the vocabulary increases.</p>
 * <p/>
 * <p>Fingerprints in the range 0 to 1023 are reserved for system use, and are allocated as constants
 * mainly to names in the XSLT and XML Schema namespaces: constants representing these names
 * are found in {@link StandardNames}.
 * <p/>
 * <p>A nameCode contains the fingerprint in the bottom 20 bits. It also contains
 * a 10-bit prefix index. This distinguishes the prefix used, among all the
 * prefixes that have been used with this namespace URI. If the prefix index is
 * zero, the prefix is null. Otherwise, it indexes an array of
 * prefix Strings associated with the namespace URI. Note that the data structures
 * and algorithms are optimized for the case where URIs usually use the same prefix.</p>
 * <p/>
 * <p>The nameCode -1 is reserved to mean "not known" or inapplicable. The fingerprint -1
 * has the same meaning. Note that masking the nameCode -1 to extract its bottom 20 bits is
 * incorrect, and will lead to errors.</p>
 * <p/>
 * <p>Modified in 9.4 to remove namespace codes.</p>
 * <p>Modified in 9.7 to remove URI codes.</p>
 */

public final class NamePool {

    /**
     * FP_MASK is a mask used to obtain a fingerprint from a nameCode. Given a
     * nameCode nc, the fingerprint is <code>nc & NamePool.FP_MASK</code>.
     * (In practice, Saxon code often uses the literal constant 0xfffff,
     * to extract the bottom 20 bits).
     * <p/>
     * <p>The difference between a fingerprint and a nameCode is that
     * a nameCode contains information
     * about the prefix of a name, the fingerprint depends only on the namespace
     * URI and local name. Note that the "null" nameCode (-1) does not produce
     * the "null" fingerprint (also -1) when this mask is applied.</p>
     */

    public static final int FP_MASK = 0xfffff;

    // Since fingerprints in the range 0-1023 belong to predefined names, user-defined names
    // will always have a fingerprint above this range, which can be tested by a mask.

    public static final int USER_DEFINED_MASK = 0xffc00;

    // Limit: maximum number of prefixes allowed for one URI

    private static final int MAX_PREFIXES_PER_URI = 1023;

    // Limit: maximum number of fingerprints

    private static final int MAX_FINGERPRINT = FP_MASK;

    // A map from QNames to fingerprints

    private final ConcurrentHashMap<StructuredQName, Integer> qNameToInteger = new ConcurrentHashMap<StructuredQName, Integer>(1000);

    // A map from fingerprints to QNames

    private final ConcurrentHashMap<Integer, StructuredQName> integerToQName = new ConcurrentHashMap<Integer, StructuredQName>(1000);

    // Next fingerprint available to be allocated. Starts at 1024 as low-end fingerprints are statically allocated to system-defined
    // names

    private AtomicInteger unique = new AtomicInteger(1024);

    // A map from URIs to lists of prefixes known to be used with that URI

    private final ConcurrentHashMap<String, List<String>> prefixesForUri = new ConcurrentHashMap<String, List<String>>(20);


    /**
     * Create a NamePool
     */

    public NamePool() {

    }

    /**
     * Get a namespace binding for a given namecode.
     *
     * @param nameCode a code identifying an expanded QName, e.g. of an element or attribute
     * @return an object identifying the namespace binding used in the given name. The namespace binding
     * identifies both the prefix and the URI.
     */

    public NamespaceBinding getNamespaceBinding(int nameCode) {
        String uri = getURI(nameCode);
        String prefix = getPrefix(nameCode);
        return new NamespaceBinding(prefix, uri);
    }

    /**
     * Get a QName for a given namecode.
     *
     * @param nameCode a code identifying an expanded QName, e.g. of an element or attribute
     * @return a qName containing the URI and local name corresponding to the supplied name code.
     * The prefix will be set to an empty string.
     */

    public StructuredQName getUnprefixedQName(int nameCode) {
        int fp = nameCode & FP_MASK;
        if ((fp & USER_DEFINED_MASK) == 0) {
            return StandardNames.getUnprefixedStructuredQName(fp);
        }
        return integerToQName.get(fp);
    }

    /**
     * Get a QName for a given namecode.
     *
     * @param nameCode a code identifying an expanded QName, e.g. of an element or attribute
     * @return a qName containing the URI and local name corresponding to the supplied name code.
     * The prefix will be set according to the value supplied in nameCode.
     */

    public StructuredQName getStructuredQName(int nameCode) {
        StructuredQName qn = getUnprefixedQName(nameCode);
        if (isPrefixed(nameCode)) {
            String prefix = getPrefix(nameCode);
            return new StructuredQName(prefix, qn.getURI(), qn.getLocalPart());
        } else {
            return qn;
        }
    }

    /**
     * Determine whether a given namecode has a non-empty prefix (and therefore, in the case of attributes,
     * whether the name is in a non-null namespace
     *
     * @param nameCode the name code to be tested
     * @return true if the name has a non-empty prefix
     */

    public static boolean isPrefixed(int nameCode) {
        return (nameCode & 0x3ff00000) != 0;
    }

    /**
     * Suggest a prefix for a given URI. If there are several, it's undefined which one is returned.
     * If there are no prefixes registered for this URI, return null.
     *
     * @param uri the namespace URI
     * @return a prefix that has previously been associated with this URI, if available; otherwise null
     */

    public String suggestPrefixForURI(String uri) {
        if (uri.equals(NamespaceConstant.XML)) {
            return "xml";
        }
        return getPrefixWithIndex(uri, 1);
    }

    /**
     * Get a prefix among all the prefixes used with a given URI, given its index
     *
     * @param uri   the URI
     * @param index indicates which of the prefixes associated with this URI is required
     * @return the prefix with the given index. If the index is 0, the prefix is always "".
     */

    private String getPrefixWithIndex(String uri, int index) {
        if (index == 0) {
            return "";
        }
        List<String> prefixes = prefixesForUri.get(uri);
        if (prefixes == null || prefixes.size() < index) {
            return null;
        } else {
            return prefixes.get(index - 1);
        }
    }

    /**
     * Allocate an index number for a prefix, relative to other prefixes that are used with
     * the same URI. The prefix index forms the top bits of a nameCode.
     * @param uri the namespace URI
     * @param prefix the namespace prefix
     * @return the prefix index.
     */

    private int allocateIndexForPrefix(String uri, String prefix) {
        if (prefix.isEmpty()) {
            return 0;
        }
        List<String> prefixes = prefixesForUri.get(uri);
        if (prefixes != null) {
            int index = prefixes.indexOf(prefix);
            if (index >= 0) {
                return index + 1;
            }
        }
        if (prefixesForUri.size() > MAX_PREFIXES_PER_URI) {
            throw new NamePoolLimitException("Too many prefixes (>1023) in use for URI " + uri);
        }
        synchronized (prefixesForUri) {
            prefixes = prefixesForUri.get(uri);
            if (prefixes == null) {
                prefixes = new ArrayList<String>(4);
                prefixesForUri.put(uri, prefixes);
            }
            prefixes.add(prefix);
            return prefixes.size();
        }
    }

    /**
     * Allocate a name code from the pool, or return an existing one if it already exists
     *
     * @param prefix    the namespace prefix. Use "" for the null prefix, representing the absent namespace
     * @param uri       the namespace URI. Use "" or null for the non-namespace.
     * @param localName the local part of the name
     * @return an integer (the "namecode") identifying the name within the namepool. The namecode
     * includes information about the prefix
     */

    public synchronized int allocate(String prefix, String uri, String localName) {
        int fp = allocateFingerprint(uri, localName);
        if (prefix.isEmpty()) {
            return fp;
        }
        int prefixIndex = allocateIndexForPrefix(uri, prefix);
        return (prefixIndex << 20) + fp;
    }

    /**
     * Allocate a fingerprint from the pool, or a new Name if there is not a matching one there
     *
     * @param uri       the namespace URI. Use "" or null for the non-namespace.
     * @param local     the local part of the name
     * @return an integer (the "fingerprint") identifying the name within the namepool.
     * The fingerprint omits information about the prefix, and is the same as the nameCode
     * for the same name with a prefix equal to "".
     */


    private int allocateFingerprint(String uri, String local) {
        if (NamespaceConstant.isReserved(uri) || NamespaceConstant.SAXON.equals(uri)) {
            int fp = StandardNames.getFingerprint(uri, local);
            if (fp != -1) {
                return fp;
            }
        }
        StructuredQName qName = new StructuredQName("", uri, local);
        Integer existing = qNameToInteger.get(qName);
        if (existing != null) {
            return existing;
        }
        Integer next = unique.getAndIncrement();
        if (next > MAX_FINGERPRINT) {
            throw new NamePoolLimitException("Too many distinct names in NamePool");
        }
        existing = qNameToInteger.putIfAbsent(qName, next);
        if (existing == null) {
            integerToQName.put(next, qName);
            return next;
        } else {
            return existing;
        }
    }

    /**
     * Get the namespace-URI of a name, given its name code or fingerprint
     *
     * @param nameCode the name code or fingerprint of a name
     * @return the namespace URI corresponding to this name code. Returns "" for the
     * no-namespace.
     * @throws IllegalArgumentException if the nameCode is not known to the NamePool.
     */

    /*@NotNull*/
    public String getURI(int nameCode) {
        return getUnprefixedQName(nameCode).getURI();
    }

    /**
     * Get the local part of a name, given its name code or fingerprint
     *
     * @param nameCode the integer name code or fingerprint of the name
     * @return the local part of the name represented by this name code or fingerprint
     */

    public String getLocalName(int nameCode) {
        return getUnprefixedQName(nameCode).getLocalPart();
    }

    /**
     * Get the prefix part of a name, given its name code
     *
     * @param nameCode the integer name code of a name in the name pool
     * @return the prefix of this name. Note that if a fingerprint rather than a full name code is supplied
     * the returned prefix will be ""
     */

    public String getPrefix(int nameCode) {
        int prefixIndex = (nameCode >> 20) & 0x3ff;
        if (prefixIndex == 0) {
            return "";
        }
        String uri = getURI(nameCode);
        return getPrefixWithIndex(uri, prefixIndex);
    }

    /**
     * Get the display form of a name (the QName), given its name code or fingerprint
     *
     * @param nameCode the integer name code or fingerprint of a name in the name pool
     * @return the corresponding lexical QName (if a fingerprint was supplied, this will
     * simply be the local name)
     */

    public String getDisplayName(int nameCode) {
        String prefix = getPrefix(nameCode);
        String local = getLocalName(nameCode);
        return prefix.isEmpty() ? local : prefix + ":" + local;
    }

    /**
     * Get the Clark form of a name, given its name code or fingerprint
     *
     * @param nameCode the integer name code or fingerprint of a name in the name pool
     * @return the local name if the name is in the null namespace, or "{uri}local"
     * otherwise. The name is always interned.
     */

    public String getClarkName(int nameCode) {
        return getUnprefixedQName(nameCode).getClarkName();
    }

    /**
     * Get the EQName form of a name, given its name code or fingerprint
     *
     * @param nameCode the integer name code or fingerprint of a name in the name pool
     * @return the name in the form Q{}local for a name in no namespace, or Q{uri}local for
     * a name in a namespace
     */

    public String getEQName(int nameCode) {
        return getUnprefixedQName(nameCode).getEQName();
    }

    /**
     * Allocate a fingerprint given a Clark Name
     *
     * @param expandedName the name in Clark notation, that is "localname" or "{uri}localName"
     * @return the fingerprint of the name, which need not previously exist in the name pool
     */

    public int allocateClarkName(String expandedName) {
        String namespace;
        String localName;
        if (expandedName.charAt(0) == '{') {
            int closeBrace = expandedName.indexOf('}');
            if (closeBrace < 0) {
                throw new IllegalArgumentException("No closing '}' in Clark name");
            }
            namespace = expandedName.substring(1, closeBrace);
            if (closeBrace == expandedName.length()) {
                throw new IllegalArgumentException("Missing local part in Clark name");
            }
            localName = expandedName.substring(closeBrace + 1);
        } else {
            namespace = "";
            localName = expandedName;
        }

        return allocate("", namespace, localName);
    }

    /**
     * Get a fingerprint for the name with a given uri and local name.
     * These must be present in the NamePool.
     * The fingerprint has the property that if two fingerprint are the same, the names
     * are the same (ie. same local name and same URI).
     *
     * @param uri       the namespace URI of the required QName
     * @param localName the local part of the required QName
     * @return the integer fingerprint, or -1 if this is not found in the name pool
     */

    public int getFingerprint(String uri, String localName) {
        // A read-only version of allocate()

        if (NamespaceConstant.isReserved(uri) || uri.equals(NamespaceConstant.SAXON)) {
            int fp = StandardNames.getFingerprint(uri, localName);
            if (fp != -1) {
                return fp;
                // otherwise, look for the name in this namepool
            }
        }
        Integer fp = qNameToInteger.get(new StructuredQName("", uri, localName));
        return fp == null ? -1 : fp;

    }

    /**
     * Unchecked Exception raised when some limit in the design of the name pool is exceeded
     */
    public static class NamePoolLimitException extends RuntimeException {

        /**
         * Create the exception
         *
         * @param message the error message associated with the error
         */

        public NamePoolLimitException(String message) {
            super(message);
        }
    }

    public static boolean isFingerprintedNode(NodeInfo node) {
        return node instanceof FingerprintedNode ||
                (node instanceof VirtualNode && (((VirtualNode) node).getRealNode() instanceof FingerprintedNode));
    }

    public static int getFingerprintOfNode(NodeInfo node) {
        if (node instanceof FingerprintedNode) {
            return ((FingerprintedNode)node).getFingerprint();
        } else if (node instanceof VirtualNode) {
            Object real = ((VirtualNode)node).getRealNode();
            if (real instanceof FingerprintedNode) {
                return ((FingerprintedNode)real).getFingerprint();
            }
        }
        return -1;
    }

}

