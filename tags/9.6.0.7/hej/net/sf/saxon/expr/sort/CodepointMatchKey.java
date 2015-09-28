////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
// Copyright (c) 2014 Saxonica Limited.
// This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
// If a copy of the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
// This Source Code Form is "Incompatible With Secondary Licenses", as defined by the Mozilla Public License, v. 2.0.
////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

package net.sf.saxon.expr.sort;

import net.sf.saxon.value.*;

/**
 * A match key for comparing strings (represented as an array of characters) using codepoint collation.
 */
public class CodepointMatchKey implements Comparable, AtomicMatchKey {

    private char[] value;
    private int hash = 0; // cached hash key: compatible with String.hashCode()

    public CodepointMatchKey(char[] in) {
        value = in;
    }

    public CodepointMatchKey(String in) {
        value = in.toCharArray();
    }

    public int compareTo(Object o) {
        if (o instanceof CodepointMatchKey) {
            char[] a = value;
            char[] b = ((CodepointMatchKey) o).value;
            int alen = a.length;
            int blen = b.length;
            int i = 0;
            int j = 0;
            while (true) {
                if (i == alen) {
                    if (j == blen) {
                        return 0;
                    } else {
                        return -1;
                    }
                }
                if (j == blen) {
                    return +1;
                }
                // Following code is needed when comparing a BMP character against a surrogate pair
                // Note: we could do this comparison without fully computing the codepoint, but it's a very rare case
                int nexta = (int) a[i++];
                if (nexta >= 55296 && nexta <= 56319) {
                    nexta = ((nexta - 55296) * 1024) + ((int) a[i++] - 56320) + 65536;
                }
                int nextb = (int) b[j++];
                if (nextb >= 55296 && nextb <= 56319) {
                    nextb = ((nextb - 55296) * 1024) + ((int) b[j++] - 56320) + 65536;
                }
                int c = nexta - nextb;
                if (c != 0) {
                    return c;
                }
            }
        } else {
            throw new ClassCastException();
        }
    }

    @Override
    public int hashCode() {
        int h = hash;
        if (h == 0) {
            char val[] = value;
            int len = val.length;

            for (int i = 0; i < len; i++) {
                h = 31 * h + val[i];
            }
            hash = h;
        }
        return h;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o instanceof CodepointMatchKey) {
            char[] a = value;
            char[] b = ((CodepointMatchKey) o).value;
            int alen = a.length;
            if (alen != b.length) {
                return false;
            }
            while (--alen >= 0) {
                if (a[alen] != b[alen]) {
                    return false;
                }
            }
            return true;
        } else {
            return false;
        }
    }

    @Override
    public String toString() {
        return new String(value);
    }

    /**
     * Get an atomic value that encapsulates this match key. Needed to support the collation-key() function.
     *
     * @return an atomic value that encapsulates this match key
     */
    public AtomicValue asAtomic() {
        return new StringValue(toString());
    }
}


