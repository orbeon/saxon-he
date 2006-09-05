package net.sf.saxon.tinytree;

import net.sf.saxon.om.FastStringBuffer;

import java.io.Writer;
import java.io.Serializable;
import java.util.List;
import java.util.ArrayList;
import java.util.Arrays;

/**
 * This is an implementation of the JDK 1.4 CharSequence interface: it implements
 * a CharSequence as a list of arrays of characters (the individual arrays are known
 * as segments). When characters are appended, a new segment is started if the previous
 * array would otherwise overflow a threshold size (the maxAllocation size).
 * <p/>
 * This is more efficient than a buffer backed by a contiguous array of characters
 * in cases where the size is likely to grow very large, and where substring operations
 * are rare. As used within the TinyTree, the value of each text node is contiguous within
 * one segment, so extraction of the value of a text node is efficient.
 */

public final class LargeStringBuffer implements CharSequence, Serializable {

    private int minAllocation;
    private int maxAllocation;
    private List segments;      // each segment is a FastStringBuffer
    private int[] startOffsets; // if startOffsets[23] is 123456, then the first
                                // character in segment 23 is the 123456'th character
                                // of the CharSequence value.
    private int length;         // total length of the CharSequence

   /**
    * Create an empty LargeStringBuffer with default space allocation
    */

    public LargeStringBuffer() {
        this(4096, 65536);
    }

   /**
    * Create an empty LargeStringBuffer
    * @param minAllocation initial allocation size for each segment (including the first). If minAllocation
    * exceeds maxAllocation, it is rounded down to the value of maxAllocation
    * @param maxAllocation maximum allocation size for each segment. When a segment reaches this
    * size, a new segment is created rather than appending more characters to the existing segment.
    */
    public LargeStringBuffer(int minAllocation, int maxAllocation) {
        this.minAllocation = Math.min(minAllocation, maxAllocation);
        this.maxAllocation = maxAllocation;
        FastStringBuffer initial = new FastStringBuffer(minAllocation);
        segments = new ArrayList(4);
        segments.add(initial);
        startOffsets = new int[1];
        startOffsets[0] = 0;
        length = 0;
    }

    /**
     * Append a CharSequence to this LargeStringBuffer
     */

    public void append(CharSequence data) {
        FastStringBuffer last = ((FastStringBuffer)segments.get(segments.size()-1));
        if (last.length() + data.length() <= maxAllocation) {
            last.append(data);
        } else {
            int[] s2 = new int[startOffsets.length+1];
            System.arraycopy(startOffsets, 0, s2, 0, startOffsets.length);
            s2[startOffsets.length] = length;
            startOffsets = s2;
            last = new FastStringBuffer(Math.max(minAllocation, data.length()));
            segments.add(last);
            last.append(data);
        }
        length += data.length();
    }

    /**
     * Returns the length of this character sequence.  The length is the number
     * of 16-bit UTF-16 characters in the sequence. </p>
     *
     * @return  the number of characters in this sequence
     */
    public int length() {
        return length;
    }

    /**
     * Returns the character at the specified index.  An index ranges from zero
     * to <tt>length() - 1</tt>.  The first character of the sequence is at
     * index zero, the next at index one, and so on, as for array
     * indexing. </p>
     *
     * @param   index   the index of the character to be returned
     *
     * @return  the specified character
     *
     * @throws  IndexOutOfBoundsException
     *          if the <tt>index</tt> argument is negative or not less than
     *          <tt>length()</tt>
     */
    public char charAt(int index) {
        if (startOffsets.length == 1) {
            // optimize for small documents
            return ((FastStringBuffer)segments.get(0)).charAt(index);
        }
        if (index < 0 || index >= length) {
            throw new IndexOutOfBoundsException(index+"");
        }
        int seg = Arrays.binarySearch(startOffsets, index);
        if (seg >= 0) {
            return ((FastStringBuffer)segments.get(seg)).charAt(0);
        }
        seg = -seg - 2;
        final int offset = index - startOffsets[seg];
        return ((FastStringBuffer)segments.get(seg)).charAt(offset);
    }

    /**
     * Returns a new character sequence that is a subsequence of this sequence.
     * The subsequence starts with the character at the specified index and
     * ends with the character at index <tt>end - 1</tt>.  The length of the
     * returned sequence is <tt>end - start</tt>, so if <tt>start == end</tt>
     * then an empty sequence is returned. </p>
     *
     * @param   start   the start index, inclusive
     * @param   end     the end index, exclusive
     *
     * @return  the specified subsequence
     *
     * @throws  IndexOutOfBoundsException
     *          if <tt>start</tt> or <tt>end</tt> are negative,
     *          if <tt>end</tt> is greater than <tt>length()</tt>,
     *          or if <tt>start</tt> is greater than <tt>end</tt>
     */
    public CharSequence subSequence(int start, int end) {
        if (startOffsets.length == 1) {
            // optimize for small documents
            return ((FastStringBuffer)segments.get(0)).subSequence(start, end);
        }
        if (start < 0 || end < 0 || end > length || start > end) {
            throw new IndexOutOfBoundsException("[" + start + ',' + end + ']');
        }
        int seg0 = Arrays.binarySearch(startOffsets, start);
        int offset0;
        if (seg0 >= 0) {
            offset0 = 0;
        } else {
            seg0 = -seg0 - 2;
            offset0 = start - startOffsets[seg0];
        }
        int seg1 = Arrays.binarySearch(startOffsets, end);
        int offset1;
        if (seg1 >= 0) {
            offset1 = 0;
        } else {
            seg1 = -seg1 - 2;
            offset1 = end - startOffsets[seg1];
        }
        FastStringBuffer startSegment = (FastStringBuffer)segments.get(seg0);
        if (seg0 == seg1) {
            // the required substring is all in one segment
            return startSegment.subSequence(offset0, offset1);
        } else {
            // copy the data into a new FastStringBuffer. This case should be exceptional
            FastStringBuffer sb = new FastStringBuffer(end - start);
            sb.append(startSegment.subSequence(offset0, startSegment.length()));
            for (int i=seg0+1; i<seg1; i++) {
                sb.append(((FastStringBuffer)segments.get(i)));
            }
            if (offset1 > 0) {
                sb.append(((FastStringBuffer)segments.get(seg1)).subSequence(0, offset1));
            }
            return sb;
        }
    }

    /**
     * Convert to a string
     */

    public String toString() {
        if (startOffsets.length == 1) {
            // optimize for small documents
            return segments.get(0).toString();
        }
        FastStringBuffer sb = new FastStringBuffer(length);
        for (int i=0; i<segments.size(); i++) {
            sb.append(((FastStringBuffer)segments.get(i)));
        }
        return sb.toString();
    }

    /**
     * Compare equality
     */

    public boolean equals(Object other) {
        return toString().equals(other.toString());
    }

    /**
     * Generate a hash code
     */

    public int hashCode() {
        // Same algorithm as String#hashCode(), but not cached
        int h = 0;
        for (int s=0; s<segments.size(); s++) {
            FastStringBuffer sb = ((FastStringBuffer)segments.get(s));
            for (int i=0; i<sb.length(); i++) {
                h = 31 * h + charAt(i);
            }
        }
        return h;
    }

    /**
     * Returns a new character sequence that is a subsequence of this sequence.
     * Unlike subSequence, this is guaranteed to return a String.
     */

    public String substring(int start, int end) {
        return subSequence(start, end).toString();
    }

    /**
     * Write the value to a writer
     */

    public void write(Writer writer) throws java.io.IOException {
        for (int s=0; s<segments.size(); s++) {
            FastStringBuffer sb = ((FastStringBuffer)segments.get(s));
            sb.write(writer);
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
// The Original Code is: all this file.
//
// The Initial Developer of the Original Code is Michael H. Kay
//
// Portions created by (your name) are Copyright (C) (your legal entity). All Rights Reserved.
//
// Contributor(s): none
//
