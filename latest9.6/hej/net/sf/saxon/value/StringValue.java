////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
// Copyright (c) 2014 Saxonica Limited.
// This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
// If a copy of the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
// This Source Code Form is "Incompatible With Secondary Licenses", as defined by the Mozilla Public License, v. 2.0.
////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

package net.sf.saxon.value;

import net.sf.saxon.expr.sort.AtomicMatchKey;
import net.sf.saxon.lib.StringCollator;
import net.sf.saxon.regex.BMPString;
import net.sf.saxon.regex.LatinString;
import net.sf.saxon.regex.UnicodeString;
import net.sf.saxon.tree.iter.UnfailingIterator;
import net.sf.saxon.tree.util.FastStringBuffer;
import net.sf.saxon.type.AtomicType;
import net.sf.saxon.type.BuiltInAtomicType;


/**
 * An atomic value of type xs:string. This class is also used for types derived from xs:string.
 * Subclasses of StringValue are used for xs:untypedAtomic and xs:anyURI values.
 */

public class StringValue extends AtomicValue {

    /*@NotNull*/ public static final StringValue EMPTY_STRING = new StringValue("");
    /*@NotNull*/ public static final StringValue SINGLE_SPACE = new StringValue(" ");
    /*@NotNull*/ public static final StringValue TRUE = new StringValue("true");
    /*@NotNull*/ public static final StringValue FALSE = new StringValue("false");

    // We hold the value as a CharSequence (it may be a StringBuffer rather than a string)
    // But the first time this is converted to a string, we keep it as a string

    protected CharSequence value;     // may be zero-length, will never be null

    /**
     * Protected constructor for use by subtypes
     */

    protected StringValue() {
        value = "";
        typeLabel = BuiltInAtomicType.STRING;
    }

    /**
     * Constructor. Note that although a StringValue may wrap any kind of CharSequence
     * (usually a String, but it can also be, for example, a StringBuffer), the caller
     * is responsible for ensuring that the value is immutable.
     *
     * @param value the String value. Null is taken as equivalent to "".
     */

    public StringValue(/*@Nullable*/ CharSequence value) {
        this.value = value == null ? "" : value;
        typeLabel = BuiltInAtomicType.STRING;
    }

    /**
     * Constructor. Note that although a StringValue may wrap any kind of CharSequence
     * (usually a String, but it can also be, for example, a StringBuffer), the caller
     * is responsible for ensuring that the value is immutable.
     *
     * @param value     the String value.
     * @param typeLabel the type of the value to be created. The caller must ensure that this is
     *                  a type derived from string and that the string is valid against this type.
     */

    public StringValue(CharSequence value, AtomicType typeLabel) {
        this.value = value;
        this.typeLabel = typeLabel;
    }


    /**
     * Assert that the string is known to contain no surrogate pairs
     */

    public synchronized void setContainsNoSurrogates() {
        if (!(value instanceof BMPString || value instanceof LatinString)) {
            value = new BMPString(value);
        }
    }

    /**
     * Create a copy of this atomic value, with a different type label
     *
     * @param typeLabel the type label of the new copy. The caller is responsible for checking that
     *                  the value actually conforms to this type.
     */

    public AtomicValue copyAsSubType(AtomicType typeLabel) {
        StringValue v = new StringValue(value);
        v.typeLabel = typeLabel;
        return v;
    }

    /**
     * Determine the primitive type of the value. This delivers the same answer as
     * getItemType().getPrimitiveItemType(). The primitive types are
     * the 19 primitive types of XML Schema, plus xs:integer, xs:dayTimeDuration and xs:yearMonthDuration,
     * and xs:untypedAtomic. For external objects, the result is AnyAtomicType.
     */

    public BuiltInAtomicType getPrimitiveType() {
        return BuiltInAtomicType.STRING;
    }

    /**
     * Factory method. Unlike the constructor, this avoids creating a new StringValue in the case
     * of a zero-length string (and potentially other strings, in future)
     *
     * @param value the String value. Null is taken as equivalent to "".
     * @return the corresponding StringValue
     */

    /*@NotNull*/
    public static StringValue makeStringValue(/*@Nullable*/ CharSequence value) {
        if (value == null || value.length() == 0) {
            return StringValue.EMPTY_STRING;
        } else {
            return new StringValue(value);
        }
    }

//    public static StringValue makeStringValue(UnicodeString value) {
//        if (value == null || value.uLength() == 0) {
//            return StringValue.EMPTY_STRING;
//        } else {
//            StringValue sv = new StringValue(value.getCharSequence());
//            sv.unicodeString = value;
//            return sv;
//        }
//    }

    /**
     * Get the string value as a CharSequence
     */

    public final CharSequence getPrimitiveStringValue() {
        return value;
    }

    /**
     * Set the value of the item as a CharSequence.
     * <p><b>For system use only. In principle, a StringValue is immutable. However, in special circumstances,
     * if it is newly constructed, the content can be changed to reflect the effect of the whiteSpace facet.</b></p>
     *
     * @param value the value of the string
     */

    public final void setStringValueCS(CharSequence value) {
        this.value = value;
    }

    /**
     * Get the length of this string, as defined in XPath. This is not the same as the Java length,
     * as a Unicode surrogate pair counts as a single character
     *
     * @return the length of the string in Unicode code points
     */

    public synchronized int getStringLength() {
        if (!(value instanceof UnicodeString)) {
            makeUnicodeString();
        }
        return ((UnicodeString)value).uLength();
    }

    /**
     * Get an upper bound on the length of the string in Unicode codepoints.
     * @return a value N such that getStringLength <= N. In practice, if the string is held
     * as UTF16 codepoints this will be the length in UTF16 codepoints; if it is held in Unicode
     * codepoints, it will be the length in Unicode codepoints
     */

    public synchronized int getStringLengthUpperBound() {
        if (value instanceof UnicodeString) {
            return ((UnicodeString) value).uLength();
        } else {
            return value.length();
        }
    }

    /**
     * Get a UnicodeString value representing the same characters as this string. This is a memo-function;
     * the value is computed the first time it is needed, and is cached for subsequent reuse
     *
     * @return the corresponding UnicodeString
     */

    public synchronized UnicodeString getUnicodeString() {
        if (!(value instanceof UnicodeString)) {
            makeUnicodeString();
        }
        return (UnicodeString)value;
    }

    /**
     * Construct a Unicode representation of this string in which each character occupies a fixed amount of space,
     * allowing direct addressing and counting of Unicode characters
     */

    private void makeUnicodeString() {
        value = UnicodeString.makeUnicodeString(value);
    }

    /**
     * Get the length of a string, as defined in XPath. This is not the same as the Java length,
     * as a Unicode surrogate pair counts as a single character.
     *
     * @param s The string whose length is required
     * @return the length of the string in Unicode code points
     */

    public static int getStringLength(/*@NotNull*/ CharSequence s) {
        if (s instanceof UnicodeString) {
            return ((UnicodeString) s).uLength();
        }
        int n = 0;
        for (int i = 0; i < s.length(); i++) {
            int c = (int) s.charAt(i);
            if (c < 55296 || c > 56319) {
                n++;    // don't count high surrogates, i.e. D800 to DBFF
            }
        }
        return n;
    }


    /**
     * Determine whether the string is a zero-length string. This may
     * be more efficient than testing whether the length is equal to zero
     *
     * @return true if the string is zero length
     */

    public boolean isZeroLength() {
        return value.length() == 0;
    }

    /**
     * Determine whether the string contains surrogate pairs
     *
     * @return true if the string contains any non-BMP characters
     */

    public boolean containsSurrogatePairs() {
        return UnicodeString.containsSurrogatePairs(value);
    }

    /**
     * Ask whether the string is known to contain no surrogate pairs.
     *
     * @return true if it is known to contain no surrogates, false if the answer is not known
     */

    public boolean isKnownToContainNoSurrogates() {
        return value instanceof BMPString || value instanceof LatinString;
    }

    /**
     * Iterate over a string, returning a sequence of integers representing the Unicode code-point values
     *
     * @return an iterator over the characters (Unicode code points) in the string
     */

    /*@NotNull*/
    public UnfailingIterator iterateCharacters() {
        CharSequence val = value;
        if (val instanceof UnicodeString) {
            return new UnicodeCharacterIterator((UnicodeString)val);
        } else {
            return new CharacterIterator(val);
        }
    }

    /**
     * Expand a string containing surrogate pairs into an array of 32-bit characters
     *
     * @param s the string to be expanded
     * @return an array of integers representing the Unicode code points
     */

    /*@NotNull*/
    public static int[] expand(/*@NotNull*/ CharSequence s) {
        int[] array = new int[getStringLength(s)];
        int o = 0;
        for (int i = 0; i < s.length(); i++) {
            int charval;
            int c = s.charAt(i);
            if (c >= 55296 && c <= 56319) {
                // we'll trust the data to be sound
                charval = ((c - 55296) * 1024) + ((int) s.charAt(i + 1) - 56320) + 65536;
                i++;
            } else {
                charval = c;
            }
            array[o++] = charval;
        }
        return array;
    }

    /**
     * Contract an array of integers containing Unicode codepoints into a Java string
     *
     * @param codes an array of integers representing the Unicode code points
     * @param used  the number of items in the array that are actually used
     * @return the constructed string
     */

    /*@NotNull*/
    public static CharSequence contract(/*@NotNull*/ int[] codes, int used) {
        FastStringBuffer sb = new FastStringBuffer(codes.length);
        for (int i = 0; i < used; i++) {
            sb.appendWideChar(codes[i]);
        }
        return sb;
    }


    /**
     * Get an object value that implements the XPath equality and ordering comparison semantics for this value.
     * If the ordered parameter is set to true, the result will be a Comparable and will support a compareTo()
     * method with the semantics of the XPath lt/gt operator, provided that the other operand is also obtained
     * using the getXPathComparable() method. In all cases the result will support equals() and hashCode() methods
     * that support the semantics of the XPath eq operator, again provided that the other operand is also obtained
     * using the getXPathComparable() method. A context argument is supplied for use in cases where the comparison
     * semantics are context-sensitive, for example where they depend on the implicit timezone or the default
     * collation.
     *
     *
     * @param ordered  true if an ordered comparison is required. In this case the result is null if the
     *                 type is unordered; in other cases the returned value will be a Comparable.
     * @param collator Collation to be used for comparing strings
     * @param implicitTimezone  the XPath dynamic evaluation context, used in cases where the comparison is context
     *                 sensitive
     * @return an Object whose equals() and hashCode() methods implement the XPath comparison semantics
     *         with respect to this atomic value. If ordered is specified, the result will either be null if
     *         no ordering is defined, or will be a Comparable
     */

    public AtomicMatchKey getXPathComparable(boolean ordered, /*@NotNull*/ StringCollator collator, int implicitTimezone) {
        return collator.getCollationKey(asString());
    }

    /**
     * Determine if two AtomicValues are equal, according to XPath rules. (This method
     * is not used for string comparisons, which are always under the control of a collation.
     * If we get here, it's because there's a type error in the comparison.)
     *
     * @throws ClassCastException always
     */

    @SuppressWarnings({"EqualsWhichDoesntCheckParameterClass"})
    public boolean equals(Object other) {
        throw new ClassCastException("equals on StringValue is not allowed");
    }

    public int hashCode() {
        return value.hashCode();
    }

    /**
     * Returns the underlying string, with the side-effect of storing the string internally for future use,
     * potentially reducing the cost if it is ever needed again
     *
     * @return the value as a String
     */

    public synchronized String asString() {
        if (value instanceof String) {
            return (String) value;
        }
        String s = value.toString();
        value = s;
        return s;
    }

    /**
     * Test whether this StringValue is equal to another under the rules of the codepoint collation
     *
     * @param other the value to be compared with this value
     * @return true if the strings are equal on a codepoint-by-codepoint basis
     */

    public boolean codepointEquals(/*@NotNull*/ StringValue other) {
        // avoid conversion of CharSequence to String if values are different lengths
        return value.length() == other.value.length() &&
                asString().equals(other.asString());
        // It might be better to do character-by-character comparison in all cases; or it might not.
        // We do it this way in the hope that string comparison compiles to native code.
    }

    /**
     * Get the effective boolean value of a string
     *
     * @return true if the string has length greater than zero
     */

    public boolean effectiveBooleanValue() {
        return value.length() > 0;
    }


    /*@NotNull*/
    public String toString() {
        return "\"" + value + '\"';
    }

    /**
     * Get a Comparable value that implements the XML Schema comparison semantics for this value.
     * Returns null if the value is not comparable according to XML Schema rules. This implementation
     * returns the underlying Java string, which works because strings will only be compared for
     * equality, not for ordering, and the equality rules for strings in XML schema are the same as in Java.
     */

    public Comparable getSchemaComparable() {
        return asString();
    }

    /**
     * Determine whether two atomic values are identical, as determined by XML Schema rules. This is a stronger
     * test than equality (even schema-equality); for example two dateTime values are not identical unless
     * they are in the same timezone.
     * <p>Note that even this check ignores the type annotation of the value. The integer 3 and the short 3
     * are considered identical, even though they are not fully interchangeable. "Identical" means the
     * same point in the value space, regardless of type annotation.</p>
     * <p>NaN is identical to itself.</p>
     *
     * @param v the other value to be compared with this one
     * @return true if the two values are identical, false otherwise.
     */

    public boolean isIdentical(/*@NotNull*/ AtomicValue v) {
        return v instanceof StringValue &&
                (this instanceof AnyURIValue == v instanceof AnyURIValue) &&
                (this instanceof UntypedAtomicValue == v instanceof UntypedAtomicValue) &&
                asString().equals(((StringValue) v).asString());
    }

    /**
     * Produce a diagnostic representation of the contents of the string
     *
     * @param s the string
     * @return a string in which non-Ascii-printable characters are replaced by \ uXXXX escapes
     */

    /*@NotNull*/
    public static String diagnosticDisplay(/*@NotNull*/ String s) {
        FastStringBuffer fsb = new FastStringBuffer(s.length());
        for (int i = 0, len = s.length(); i < len; i++) {
            char c = s.charAt(i);
            if (c >= 0x20 && c <= 0x7e) {
                fsb.append(c);
            } else {
                fsb.append("\\u");
                for (int shift = 12; shift >= 0; shift -= 4) {
                    fsb.append("0123456789ABCDEF".charAt((c >> shift) & 0xF));
                }
            }
        }
        return fsb.toString();
    }

    /**
     * CharacterIterator is used to iterate over the characters in a string,
     * returning them as integers representing the Unicode code-point.
     */


    public final class CharacterIterator implements UnfailingIterator {

        CharSequence val;
        int inpos = 0;        // 0-based index of the current Java char

        /**
         * Create an iterator over a string
         */

        public CharacterIterator(CharSequence val) {
            this.val = val;
        }

        /*@Nullable*/
        public Int64Value next() {
            if (inpos < val.length()) {
                int c = val.charAt(inpos++);
                int current;
                if (c >= 55296 && c <= 56319) {
                    // we'll trust the data to be sound
                    try {
                        current = ((c - 55296) * 1024) + ((int) val.charAt(inpos++) - 56320) + 65536;
                    } catch (StringIndexOutOfBoundsException e) {
                        System.err.println("Invalid surrogate at end of string");
                        System.err.println(diagnosticDisplay(value.toString()));
                        e.printStackTrace();
                        throw e;
                    }
                } else {
                    current = c;
                }
                return new Int64Value(current);
            } else {
                return null;
            }
        }

        public void close() {
        }

        /*@NotNull*/
        public UnfailingIterator getAnother() {
            return new CharacterIterator(val);
        }

        /**
         * Get properties of this iterator, as a bit-significant integer.
         *
         * @return the properties of this iterator. This will be some combination of
         *         properties such as {@link #GROUNDED} and {@link #LAST_POSITION_FINDER}. It is always
         *         acceptable to return the value zero, indicating that there are no known special properties.
         */

        public int getProperties() {
            return 0;
        }
    }

    public final class UnicodeCharacterIterator implements UnfailingIterator {

        UnicodeString val;
        int inpos = 0;        // 0-based index of the current Java char

        /**
         * Create an iterator over a string
         */

        public UnicodeCharacterIterator(UnicodeString val) {
            this.val = val;
        }

        /*@Nullable*/
        public Int64Value next() {
            if (inpos < val.uLength()) {
                return new Int64Value(val.uCharAt(inpos++));
            } else {
                return null;
            }
        }

        public void close() {
        }

        /*@NotNull*/
        public UnfailingIterator getAnother() {
            return new UnicodeCharacterIterator(val);
        }

        /**
         * Get properties of this iterator, as a bit-significant integer.
         *
         * @return the properties of this iterator. This will be some combination of
         *         properties such as {@link #GROUNDED} and {@link #LAST_POSITION_FINDER}. It is always
         *         acceptable to return the value zero, indicating that there are no known special properties.
         */

        public int getProperties() {
            return 0;
        }
    }


}

