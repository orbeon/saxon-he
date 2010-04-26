package net.sf.saxon.value;
import net.sf.saxon.expr.XPathContext;
import net.sf.saxon.om.FastStringBuffer;
import net.sf.saxon.om.StandardNames;
import net.sf.saxon.trans.Err;
import net.sf.saxon.trans.XPathException;
import net.sf.saxon.type.*;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.regex.Pattern;

/**
* A decimal value
*/

public final class DecimalValue extends NumericValue {

    public static final int DIVIDE_PRECISION = 18;
    private static boolean canSetScaleNegative = true;  // until proved otherwise

    private BigDecimal value;

    public static final BigDecimal BIG_DECIMAL_ONE = BigDecimal.valueOf(1);
    public static final BigInteger BIG_INTEGER_TEN = BigInteger.valueOf(10);
    public static final BigDecimal BIG_DECIMAL_ONE_MILLION = BigDecimal.valueOf(1000000);

    public static final DecimalValue ZERO = new DecimalValue(BigDecimal.valueOf(0));
    public static final DecimalValue ONE = new DecimalValue(BigDecimal.valueOf(1));

    /**
    * Constructor supplying a BigDecimal
    * @param value the value of the DecimalValue
    */

    public DecimalValue(BigDecimal value) {
        this.value = value.stripTrailingZeros();
        typeLabel = BuiltInAtomicType.DECIMAL;
    }

    private static final Pattern decimalPattern = Pattern.compile("(\\-|\\+)?((\\.[0-9]+)|([0-9]+(\\.[0-9]*)?))");

    /**
     * Factory method to construct a DecimalValue from a string
     * @param in the value of the DecimalValue
     * @param validate true if validation is required; false if the caller knows that the value is valid
     * @return the required DecimalValue if the input is valid, or a ValidationFailure encapsulating the error
     * message if not.
     */

    public static ConversionResult makeDecimalValue(CharSequence in, boolean validate) {

        try {
            FastStringBuffer digits = new FastStringBuffer(in.length());
            int scale = 0;
            int state = 0;
            // 0 - in initial whitespace; 1 - after sign
            // 3 - after decimal point; 5 - in final whitespace
            boolean foundDigit = false;
            int len = in.length();
            for (int i=0; i<len; i++) {
                char c = in.charAt(i);
                switch (c) {
                    case ' ':
                    case '\t':
                    case '\r':
                    case '\n':
                        if (state != 0) {
                            state = 5;
                        }
                        break;
                    case '+':
                        if (state != 0) {
                            throw new NumberFormatException("unexpected sign");
                        }
                        state = 1;
                        break;
                    case '-':
                        if (state != 0) {
                            throw new NumberFormatException("unexpected sign");
                        }
                        state = 1;
                        digits.append(c);
                        break;
                    case '0':
                    case '1':
                    case '2':
                    case '3':
                    case '4':
                    case '5':
                    case '6':
                    case '7':
                    case '8':
                    case '9':
                        if (state == 0) {
                            state = 1;
                        } else if (state >= 3) {
                            scale++;
                        }
                        if (state == 5) {
                            throw new NumberFormatException("contains embedded whitespace");
                        }
                        digits.append(c);
                        foundDigit = true;
                        break;
                    case '.':
                        if (state == 5) {
                            throw new NumberFormatException("contains embedded whitespace");
                        }
                        if (state >= 3) {
                            throw new NumberFormatException("more than one decimal point");
                        }
                        state = 3;
                        break;
                    default:
                        throw new NumberFormatException("invalid character '" + c + "'");
                }

            }

            if (!foundDigit) {
                throw new NumberFormatException("no digits in value");
            }

            // remove insignificant trailing zeroes
            while (scale > 0) {
                if (digits.charAt(digits.length()-1) == '0') {
                    digits.setLength(digits.length() - 1);
                    scale--;
                } else {
                    break;
                }
            }
            if (digits.length() == 0 || (digits.length() == 1 && digits.charAt(0) == '-')) {
                return DecimalValue.ZERO;
            }
            BigInteger bigInt = new BigInteger(digits.toString());
            BigDecimal bigDec = new BigDecimal(bigInt, scale);
            return new DecimalValue(bigDec);
        } catch (NumberFormatException err) {
            ValidationFailure e = new ValidationFailure(
                    "Cannot convert string " + Err.wrap(Whitespace.trim(in), Err.VALUE) +
                            " to xs:decimal: " + err.getMessage());
            e.setErrorCode("FORG0001");
            return e;
        }
    }

    /**
     * Test whether a string is castable to a decimal value
     * @param in the string to be tested
     * @return true if the string has the correct format for a decimal
     */

    public static boolean castableAsDecimal(CharSequence in) {
        CharSequence trimmed = Whitespace.trimWhitespace(in);
        return decimalPattern.matcher(trimmed).matches();
    }

    /**
    * Constructor supplying a double
    * @param in the value of the DecimalValue
    */

    public DecimalValue(double in) throws ValidationException {
        try {
            BigDecimal d = new BigDecimal(in);
            value = d.stripTrailingZeros();
        } catch (NumberFormatException err) {
            // Must be a special value such as NaN or infinity
            ValidationException e = new ValidationException(
                    "Cannot convert double " + Err.wrap(in+"", Err.VALUE) + " to decimal");
            e.setErrorCode("FOCA0002");
            throw e;
        }
        typeLabel = BuiltInAtomicType.DECIMAL;
    }

    /**
    * Constructor supplying a long integer
    * @param in the value of the DecimalValue
    */

    public DecimalValue(long in) {
        value = BigDecimal.valueOf(in);
        typeLabel = BuiltInAtomicType.DECIMAL;
    }

    /**
     * Create a copy of this atomic value, with a different type label
     *
     * @param typeLabel the type label of the new copy. The caller is responsible for checking that
     *                  the value actually conforms to this type.
     */

    public AtomicValue copyAsSubType(AtomicType typeLabel) {
        DecimalValue v = new DecimalValue(value);
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
        return BuiltInAtomicType.DECIMAL;
    }


    /**
    * Get the value
    */

    public BigDecimal getDecimalValue() {
        return value;
    }

    /**
     * Get the hashCode. This must conform to the rules for other NumericValue hashcodes
     * @see NumericValue#hashCode
     */

    public int hashCode() {
        BigDecimal round = value.setScale(0, BigDecimal.ROUND_DOWN);
        long value = round.longValue();
        if (value > Integer.MIN_VALUE && value < Integer.MAX_VALUE) {
            return (int)value;
        } else {
            return new Double(getDoubleValue()).hashCode();
        }
    }

    public boolean effectiveBooleanValue() {
        return value.signum() != 0;
    }

    /**
    * Convert to target data type
    */

    public ConversionResult convertPrimitive(BuiltInAtomicType requiredType, boolean validate, XPathContext context) {
        switch(requiredType.getFingerprint()) {
        case StandardNames.XS_BOOLEAN:
                // 0.0 => false, anything else => true
            return BooleanValue.get(value.signum()!=0);
        case StandardNames.XS_NUMERIC:
        case StandardNames.XS_DECIMAL:
        case StandardNames.XS_ANY_ATOMIC_TYPE:
            return this;
        case StandardNames.XS_INTEGER:
            return BigIntegerValue.makeIntegerValue(value.toBigInteger());
        case StandardNames.XS_UNSIGNED_LONG:
        case StandardNames.XS_UNSIGNED_INT:
        case StandardNames.XS_UNSIGNED_SHORT:
        case StandardNames.XS_UNSIGNED_BYTE:
        case StandardNames.XS_NON_POSITIVE_INTEGER:
        case StandardNames.XS_NEGATIVE_INTEGER:
        case StandardNames.XS_LONG:
        case StandardNames.XS_INT:
        case StandardNames.XS_SHORT:
        case StandardNames.XS_BYTE:
        case StandardNames.XS_NON_NEGATIVE_INTEGER:
        case StandardNames.XS_POSITIVE_INTEGER:
            IntegerValue iv = BigIntegerValue.makeIntegerValue(value.toBigInteger());
            return iv.convertPrimitive(requiredType, validate, context);     
        case StandardNames.XS_DOUBLE:
            return new DoubleValue(value.doubleValue());
        case StandardNames.XS_FLOAT:
            return new FloatValue(value.floatValue());
        case StandardNames.XS_STRING:
            return new StringValue(getStringValueCS());
        case StandardNames.XS_UNTYPED_ATOMIC:
            return new UntypedAtomicValue(getStringValueCS());
        default:
            ValidationFailure err = new ValidationFailure("Cannot convert decimal to " +
                                     requiredType.getDisplayName());
            err.setErrorCode("XPTY0004");
            return err;
        }
    }

    /**
     * Get the value of the item as a CharSequence. This is in some cases more efficient than
     * the version of the method that returns a String.
     */

//    public CharSequence getStringValueCS() {
//        return decimalToString(value, new FastStringBuffer(20));
//    }

    /**
     * Get the canonical lexical representation as defined in XML Schema. This is not always the same
     * as the result of casting to a string according to the XPath rules. For xs:decimal, the canonical
     * representation always contains a decimal point.
     */

    public CharSequence getCanonicalLexicalRepresentation() {
        String s = getStringValue();
        if (s.indexOf('.') < 0) {
            s += ".0";
        }
        return s;
    }

    /**
    * Get the value as a String
    * @return a String representation of the value
    */

    public CharSequence getPrimitiveStringValue() {
        return decimalToString(value, new FastStringBuffer(FastStringBuffer.TINY));
    }

    /**
     * Convert a decimal value to a string, using the XPath rules for formatting
     * @param value the decimal value to be converted
     * @param fsb the FastStringBuffer to which the value is to be appended
     * @return the supplied FastStringBuffer, suitably populated
     */

    public static FastStringBuffer decimalToString(BigDecimal value, FastStringBuffer fsb) {
        // Can't use the plain BigDecimal#toString() under JDK 1.5 because this produces values like "1E-5".
        // JDK 1.5 offers BigDecimal#toPlainString() which might do the job directly
        int scale = value.scale();
        if (scale == 0) {
            fsb.append(value.toString());
            return fsb;
        } else if (scale < 0) {
            String s = value.abs().unscaledValue().toString();
            if (s.equals("0")) {
                fsb.append('0');
                return fsb;
            }
            //FastStringBuffer sb = new FastStringBuffer(s.length() + (-scale) + 2);
            if (value.signum() < 0) {
                fsb.append('-');
            }
            fsb.append(s);
            for (int i=0; i<(-scale); i++) {
                fsb.append('0');
            }
            return fsb;
        } else {
            String s = value.abs().unscaledValue().toString();
            if (s.equals("0")) {
                fsb.append('0');
                return fsb;
            }
            int len = s.length();
            //FastStringBuffer sb = new FastStringBuffer(len+1);
            if (value.signum() < 0) {
                fsb.append('-');
            }
            if (scale >= len) {
                fsb.append("0.");
                for (int i=len; i<scale; i++) {
                    fsb.append('0');
                }
                fsb.append(s);
            } else {
                fsb.append(s.substring(0, len-scale));
                fsb.append('.');
                fsb.append(s.substring(len-scale));
            }
            return fsb;
        }
    }

    /**
    * Negate the value
    */

    public NumericValue negate() {
        return new DecimalValue(value.negate());
    }

    /**
    * Implement the XPath floor() function
    */

    public NumericValue floor() {
        return new DecimalValue(value.setScale(0, BigDecimal.ROUND_FLOOR));
    }

    /**
    * Implement the XPath ceiling() function
    */

    public NumericValue ceiling() {
        return new DecimalValue(value.setScale(0, BigDecimal.ROUND_CEILING));
    }

    /**
    * Implement the XPath round() function
    */

    public NumericValue round() {
        // The XPath rules say that we should round to the nearest integer, with .5 rounding towards
        // positive infinity. Unfortunately this is not one of the rounding modes that the Java BigDecimal
        // class supports, so we need different rules depending on the value.

        // If the value is positive, we use ROUND_HALF_UP; if it is negative, we use ROUND_HALF_DOWN (here "UP"
        // means "away from zero")

        switch (value.signum()) {
            case -1:
                return new DecimalValue(value.setScale(0, BigDecimal.ROUND_HALF_DOWN));
            case 0:
                return this;
            case +1:
                return new DecimalValue(value.setScale(0, BigDecimal.ROUND_HALF_UP));
            default:
                // can't happen
                return this;
        }

    }

    /**
    * Implement the XPath round-half-to-even() function
    */

    public NumericValue roundHalfToEven(int scale) {
        if (scale<0 && !canSetScaleNegative) {
            // This path is taken on JDK 1.4. But it gives the wrong answer, because
            // it ignores the fractional part of the number: so when rounding to a multiple of
            // 10, the value 65.05 is rounded to 60 instead of 70.
            try {
                AtomicValue val = convertPrimitive(BuiltInAtomicType.INTEGER, true, null).asAtomic();
                if (val instanceof Int64Value) {
                    return ((Int64Value)val).roundHalfToEven(scale);
                } else {
                    return ((BigIntegerValue)val).roundHalfToEven(scale);
                }
            } catch (XPathException err) {
                throw new IllegalArgumentException("internal error in integer-decimal conversion");
            }
        } else {
            BigDecimal scaledValue;
            try {
                scaledValue = value.setScale(scale, BigDecimal.ROUND_HALF_EVEN);
            } catch (ArithmeticException e) {
                if (scale < 0) {
                    canSetScaleNegative = false;
                    return roundHalfToEven(scale);
                } else {
                    throw e;
                }
            }
            return new DecimalValue(scaledValue.stripTrailingZeros());
        }
    }

    /**
     * Determine whether the value is negative, zero, or positive
     * @return -1 if negative, 0 if zero, +1 if positive, NaN if NaN
     */

    public double signum() {
        return value.signum();
    }

    /**
    * Determine whether the value is a whole number, that is, whether it compares
    * equal to some integer
    */

    public boolean isWholeNumber() {
        return value.scale()==0 ||
               value.compareTo(value.setScale(0, BigDecimal.ROUND_DOWN)) == 0;
    }

    /**
     * Get the absolute value as defined by the XPath abs() function
     * @return the absolute value
     * @since 9.2
     */

    public NumericValue abs() {
        if (value.signum() > 0) {
            return this;
        } else {
            return new DecimalValue(value.negate());
        }
    }

    /**
    * Compare the value to another numeric value
    */

    public int compareTo(Object other) {
        if ((NumericValue.isInteger((NumericValue)other))) {
            // deliberately triggers a ClassCastException if other value is the wrong type
            try {
                return compareTo(((NumericValue)other).convertPrimitive(BuiltInAtomicType.DECIMAL, true, null).asAtomic());
            } catch (XPathException err) {
                throw new AssertionError("Conversion of integer to decimal should never fail");
            }
        } else if (other instanceof DecimalValue) {
            return value.compareTo(((DecimalValue)other).value);
        } else if (other instanceof FloatValue) {
            try {
                return ((FloatValue)convertPrimitive(BuiltInAtomicType.FLOAT, true, null).asAtomic()).compareTo(other);
            } catch (XPathException err) {
                throw new AssertionError("Conversion of decimal to float should never fail");
            }
        } else {
            return super.compareTo(other);
        }
    }

    /**
     * Compare the value to a long
     * @param other the value to be compared with
     * @return -1 if this is less, 0 if this is equal, +1 if this is greater or if this is NaN
     */

    public int compareTo(long other) {
        if (other == 0) {
            return value.signum();
        }
        return value.compareTo(BigDecimal.valueOf(other));
    }

    /**
     * Get a Comparable value that implements the XML Schema ordering comparison semantics for this value.
     * Returns null if the value is not comparable according to XML Schema rules. The default implementation
     * returns the value itself if it is comparable, or null otherwise. This is modified for types such as
     * xs:duration which allow ordering comparisons in XML Schema, but not in XPath.
     * <p/>
     * <p>In the case of data types that are partially ordered, the returned Comparable extends the standard
     * semantics of the compareTo() method by returning the value {@link Value#INDETERMINATE_ORDERING} when there
     * is no defined order relationship between two given values.</p>
     */

    /**
     * Get an object that implements XML Schema comparison semantics
     */

    public Comparable getSchemaComparable() {
        return new DecimalComparable(this);
    }

    protected static class DecimalComparable implements Comparable {

        protected DecimalValue value;

        public DecimalComparable(DecimalValue value) {
            this.value = value;
        }

        public BigDecimal asBigDecimal() {
            return value.getDecimalValue();
        }

        public int compareTo(Object o) {
            if (o instanceof DecimalComparable) {
                return asBigDecimal().compareTo(((DecimalComparable)o).asBigDecimal());
            } else if (o instanceof Int64Value.Int64Comparable) {
                return asBigDecimal().compareTo(BigDecimal.valueOf(((Int64Value.Int64Comparable)o).asLong()));
            } else if (o instanceof BigIntegerValue.BigIntegerComparable) {
                return value.compareTo(new BigDecimal(((BigIntegerValue.BigIntegerComparable)o).asBigInteger()));
            } else {
                return INDETERMINATE_ORDERING;
            }
        }

        public boolean equals(Object o) {
            return compareTo(o) == 0;
        }

        public int hashCode() {
            // Must align with hashCodes for other subtypes of xs:decimal
            if (value.isWholeNumber()) {
                try {
                    return value.convertPrimitive(BuiltInAtomicType.INTEGER, true, null).asAtomic()
                            .getSchemaComparable().hashCode();
                } catch (ValidationException e) {
                    return 12345678;    // can't happen
                }
            }
            return value.hashCode();
        }
    }

    /**
     * Determine whether two atomic values are identical, as determined by XML Schema rules. This is a stronger
     * test than equality (even schema-equality); for example two dateTime values are not identical unless
     * they are in the same timezone.
     * <p>Note that even this check ignores the type annotation of the value. The integer 3 and the short 3
     * are considered identical, even though they are not fully interchangeable. "Identical" means the
     * same point in the value space, regardless of type annotation.</p>
     * <p>NaN is identical to itself.</p>
     * @param v the other value to be compared with this one
     * @return true if the two values are identical, false otherwise.
     */

    public boolean isIdentical(Value v) {
        return (v instanceof DecimalValue) && equals(v);
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
// The Original Code is: all this file except the asStringXT() and zeros() methods (not currently used).
//
// The Initial Developer of the Original Code is Michael H. Kay.
//
// Contributor(s): none.
//

