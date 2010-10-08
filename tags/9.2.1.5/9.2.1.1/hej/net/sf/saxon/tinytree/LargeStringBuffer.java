package net.sf.saxon.tinytree;

import net.sf.saxon.om.FastStringBuffer;

import java.io.Serializable;

/**
 * This is an implementation of the JDK 1.4 CharSequence interface: it implements
 * a CharSequence as a list of arrays of characters (the individual arrays are known
 * as segments). The segments have a fixed size of 65536 characters.
 * <p/>
 * This is more efficient than a buffer backed by a contiguous array of characters
 * in cases where the size is likely to grow very large, and where substring operations
 * are rare. As used within the TinyTree, extraction of the string value of a node
 * requires character copying only in the case where the value crosses segment
 * boundaries.
 */

public final class LargeStringBuffer implements CharSequence, Serializable {

    private final static int BITS = 16;
    private final static int SEGLEN = 1<<BITS;
    private final static int MASK = SEGLEN - 1;

    // Variant of LargeStringBuffer using fixed-length segments

    private char[][] data;
    private int length;         // total length of the CharSequence
    private int segmentsUsed;

    /**
     * Create an empty LargeStringBuffer with default space allocation
     */

    public LargeStringBuffer() {
        data = new char[1][];
        segmentsUsed = 0;
        length = 0;
    }

    /**
     * Expand the data structure. Note this only involves expanding the "index" (the list of
     * segments), it does not cause any character data to be copied.
     * @param seg the new segment to be added.
     */

    private void addSegment(char[] seg) {
        int segs = data.length;
        if (segmentsUsed + 1 > segs) {
            char[][] d2 = new char[segs*2][];
            System.arraycopy(data, 0, d2, 0, segmentsUsed);
            data = d2;
        }
        data[segmentsUsed++] = seg;
    }

    /**
     * Append a CharSequence to this LargeStringBuffer
     * @param s the data to be appended
     */

    public void append(CharSequence s) {
        // Although we provide variants of this method for different subtypes, Java decides which to use based
        // on the static type of the operand. We want to use the right method based on the dynamic type, to avoid
        // creating objects and copying strings unnecessarily. So we do a dynamic dispatch. (This is only necessary
        // of course because the CharSequence class offers no getChars() method).

        if (s instanceof CompressedWhitespace) {
            FastStringBuffer fsb = new FastStringBuffer(FastStringBuffer.SMALL);
            ((CompressedWhitespace)s).uncompress(fsb);
            append(fsb);
            return;
        }

        final int len = s.length();
        char[] firstSeg;
        int firstSegOffset = length & MASK;
        if (firstSegOffset == 0) {
            firstSeg = new char[SEGLEN];
            addSegment(firstSeg);
        } else {
            firstSeg = data[length>>BITS];
        }
        int firstSegLen;
        int fullSegments;
        int lastSegLen;
        if (len <= SEGLEN - firstSegOffset) {
            // all fits in the current segment
            firstSegLen = len;
            fullSegments = 0;
            lastSegLen = 0;
        } else {
            firstSegLen = SEGLEN - firstSegOffset;
            fullSegments = (len - firstSegLen) >> BITS;
            lastSegLen = (len - firstSegLen) & MASK;
        }

        if (s instanceof CharSlice) {
            ((CharSlice)s).getChars(0, firstSegLen, firstSeg, firstSegOffset);
            int start = firstSegLen;
            for (int i=0; i<fullSegments; i++) {
                char[] seg = new char[SEGLEN];
                addSegment(seg);
                ((CharSlice)s).getChars(start, start+SEGLEN, seg, 0);
                start += SEGLEN;
            }
            if (lastSegLen > 0) {
                char[] seg = new char[SEGLEN];
                addSegment(seg);
                ((CharSlice)s).getChars(start, len, seg, 0);
            }
            length += len;

        } else if (s instanceof String) {
            ((String)s).getChars(0, firstSegLen, firstSeg, firstSegOffset);
            int start = firstSegLen;
            for (int i=0; i<fullSegments; i++) {
                char[] seg = new char[SEGLEN];
                addSegment(seg);
                ((String)s).getChars(start, start+SEGLEN, seg, 0);
                start += SEGLEN;
            }
            if (lastSegLen > 0) {
                char[] seg = new char[SEGLEN];
                addSegment(seg);
                ((String)s).getChars(start, len, seg, 0);
            }
            length += len;

        } else if (s instanceof FastStringBuffer) {
            ((FastStringBuffer)s).getChars(0, firstSegLen, firstSeg, firstSegOffset);
            int start = firstSegLen;
            for (int i=0; i<fullSegments; i++) {
                char[] seg = new char[SEGLEN];
                addSegment(seg);
                ((FastStringBuffer)s).getChars(start, start+SEGLEN, seg, 0);
                start += SEGLEN;
            }
            if (lastSegLen > 0) {
                char[] seg = new char[SEGLEN];
                addSegment(seg);
                ((FastStringBuffer)s).getChars(start, len, seg, 0);
            }
            length += len;

        } else {
            throw new IllegalArgumentException("Unknown kind of CharSequence");
        }
    }

    /**
     * Returns the length of this character sequence.  The length is the number
     * of 16-bit UTF-16 characters in the sequence. </p>
     * @return the number of characters in this sequence
     */
    public int length() {
        return length;
    }

    /**
     * Returns the character at the specified index.  An index ranges from zero
     * to <tt>length() - 1</tt>.  The first character of the sequence is at
     * index zero, the next at index one, and so on, as for array
     * indexing. </p>
     * @param index the index of the character to be returned
     * @return the specified character
     * @throws IndexOutOfBoundsException if the <tt>index</tt> argument is negative or not less than
     *                                   <tt>length()</tt>
     */
    public char charAt(int index) {
        if (index < 0 || index >= length) {
            throw new IndexOutOfBoundsException(index + "");
        }
        return data[index>>BITS][index&MASK];
    }

    /**
     * Returns a new character sequence that is a subsequence of this sequence.
     * The subsequence starts with the character at the specified index and
     * ends with the character at index <tt>end - 1</tt>.  The length of the
     * returned sequence is <tt>end - start</tt>, so if <tt>start == end</tt>
     * then an empty sequence is returned. </p>
     * @param start the start index, inclusive
     * @param end   the end index, exclusive
     * @return the specified subsequence
     * @throws IndexOutOfBoundsException if <tt>start</tt> or <tt>end</tt> are negative,
     *                                   if <tt>end</tt> is greater than <tt>length()</tt>,
     *                                   or if <tt>start</tt> is greater than <tt>end</tt>
     */
    public CharSequence subSequence(int start, int end) {
        int firstSeg = start>>BITS;
        int lastSeg = (end-1)>>BITS;
        if (firstSeg == lastSeg) {
            return new CharSlice(data[firstSeg], start & MASK, end - start);
        } else {
            FastStringBuffer fsb = new FastStringBuffer(end - start);
            int firstSegLen = SEGLEN - (start & MASK);
            fsb.append(data[firstSeg], start & MASK, firstSegLen);
            int doneTo = start + firstSegLen;
            while (true) {
                firstSeg++;
                if (doneTo + SEGLEN < end) {
                    fsb.append(data[firstSeg]);
                    doneTo += SEGLEN;
                } else {
                    fsb.append(data[firstSeg], 0, end - doneTo);
                    break;
                }
            }
            return fsb;
        }
    }

    /**
     * Convert to a string
     */

    public String toString() {
        return subSequence(0, length).toString();
    }

    /**
     * Compare equality
     */

    public boolean equals(Object other) {
        return other instanceof CharSequence && toString().equals(other.toString());
    }

    /**
     * Generate a hash code
     */

    public int hashCode() {
        // Same algorithm as String#hashCode(), but not cached
        int h = 0;
        for (int s = 0; s < data.length; s++) {
            char[] chars = data[s];
            for (int i = 0; i < SEGLEN; i++) {
                h = 31 * h + chars[i];
            }
        }
        return h;
    }

    /**
     * Returns a new character sequence that is a subsequence of this sequence.
     * Unlike subSequence, this is guaranteed to return a String.
     * @param start index of the first character to be included
     * @param end   index of the character after the last one to be included
     * @return the substring at the given position
     */

    public String substring(int start, int end) {
        return subSequence(start, end).toString();
    }

    /**
     * Write the value to a writer
     * @param writer the writer to which the value is to be written
     */

    public void write(java.io.Writer writer) throws java.io.IOException {
        writer.write(toString());
    }

    /**
     * Produce diagnostic dump
     */

//    public void dumpDataStructure() {
//        System.err.println("** Segments:");
//        for (int s = 0; s < segments.size(); s++) {
//            System.err.println("   SEG " + s + " start offset " + startOffsets[s] + " length "
//                    + ((FastStringBuffer)segments.get(s)).length());
//        }
//    }

//    public static void main(String[] args) {
//        LargeStringBuffer lsb = new LargeStringBuffer();
//        for (int i=0; i<30; i++)  {
//            char[] chars = new char[i*5000];
//            Arrays.fill(chars, 'x');
//            lsb.append(new String(chars));
//            lsb.append("");
//        }
//        for (int i=0; i<lsb.length()-10000; i+=10000) {
//            System.out.println(i + ":" + lsb.subSequence(i, i+9999).length());
//        }
//        lsb.dumpDataStructure();
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
// The Original Code is: all this file.
//
// The Initial Developer of the Original Code is Michael H. Kay
//
// Portions created by (your name) are Copyright (C) (your legal entity). All Rights Reserved.
//
// Contributor(s): none
//