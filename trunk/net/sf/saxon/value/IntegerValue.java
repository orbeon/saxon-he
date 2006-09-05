package net.sf.saxon.value;

import net.sf.saxon.Err;
import net.sf.saxon.expr.Token;
import net.sf.saxon.expr.XPathContext;
import net.sf.saxon.trans.DynamicError;
import net.sf.saxon.trans.XPathException;
import net.sf.saxon.type.*;

import java.math.BigDecimal;
import java.math.BigInteger;

/**
 * An integer value: note this is a subtype of decimal in XML Schema, not a primitive type.
 * This class also supports the built-in subtypes of xs:integer.
 * Actually supports a value in the range permitted by a Java "long"
 */

public final class IntegerValue extends NumericValue {

    /**
     * IntegerValue representing the value -1
     */
    public static final IntegerValue MINUS_ONE = new IntegerValue(-1);
    /**
     * IntegerValue representing the value zero
     */
    public static final IntegerValue ZERO = new IntegerValue(0);
    /**
     * IntegerValue representing the value +1
     */
    public static final IntegerValue PLUS_ONE = new IntegerValue(+1);
    /**
     * IntegerValue representing the maximum value for this class
     */
    public static final IntegerValue MAX_LONG = new IntegerValue(Long.MAX_VALUE);
    /**
     * IntegerValue representing the minimum value for this class
     */
    public static final IntegerValue MIN_LONG = new IntegerValue(Long.MIN_VALUE);

    private long value;
    private ItemType type;

    /**
     * Constructor supplying a long
     *
     * @param value the value of the IntegerValue
     */

    public IntegerValue(long value) {
        this.value = value;
        this.type = Type.INTEGER_TYPE;
    }

    /**
     * Constructor for a subtype, supplying an integer
     *
     * @param val The supplied value, as an integer
     * @param type the required item type, a subtype of xs:integer
     * @exception DynamicError if the supplied value is out of range for the
     *      target type
     */

    public IntegerValue(long val, AtomicType type) throws DynamicError {
        this.value = val;
        this.type = type;
        if (!checkRange(value, type)) {
            DynamicError err = new DynamicError("Integer value " + val +
                    " is out of range for the requested type " + type.getDescription());
            err.setErrorCode("XPTY0004");
            err.setIsTypeError(true);
            throw err;
        };
    }

    /**
     * Convert the value to a subtype of xs:integer
     * @param subtype the target subtype
     * @param validate true if validation is required; false if the caller already knows that the value is valid
     * @return null if the conversion succeeds; a ValidationException describing the failure if it fails. Note
     * that the exception is returned, not thrown.
     */

    public ValidationException convertToSubtype(AtomicType subtype, boolean validate) {
        if (!validate) {
            setSubType(subtype);
            return null;
        } else if (checkRange(subtype)) {
            return null;
        } else {
            ValidationException err = new ValidationException("String " + value +
                    " cannot be converted to integer subtype " + subtype.getDescription());
            err.setErrorCode("FORG0001");
            return err;
        }
    }

    /**
     * This class allows subtypes of xs:integer to be held, as well as xs:integer values.
     * This method sets the required type label. It is the caller's responsibility to check that
     * the value is within range.
     */
    public void setSubType(AtomicType type) {
        this.type = type;
    }

    /**
     * This class allows subtypes of xs:integer to be held, as well as xs:integer values.
     * This method checks that the value is within range, and also sets the type label.
     * @param type the subtype of integer required
     * @return true if successful, false if value is out of range for the subtype
     */
    public boolean checkRange(AtomicType type) {
        this.type = type;
        return checkRange(value, type);
    }

    /**
     * Static factory method to convert strings to integers.
     * @param s CharSequence representing the string to be converted
     * @return either an IntegerValue or a BigIntegerValue representing the value of the String, or
     * an ErrorValue encapsulating an Exception if the value cannot be converted.
     */

    public static AtomicValue stringToInteger(CharSequence s) {

        int len = s.length();
        int start = 0;
        while (start < len && s.charAt(start) <= 0x20) {
            start++;
        }
        int last = len - 1;
        while (last > start && s.charAt(last) <= 0x20) {
            last--;
        }
        if (start > last) {
            return numericError("Cannot convert zero-length string to an integer");
        }
        if (last - start < 16) {
            // for short numbers, we do the conversion ourselves, to avoid throwing unnecessary exceptions
            boolean negative = false;
            long value = 0;
            int i=start;
            if (s.charAt(i) == '+') {
                i++;
            } else if (s.charAt(i) == '-') {
                negative = true;
                i++;
            }
            if (i > last) {
                return numericError("Cannot convert string " + Err.wrap(s, Err.VALUE) +
                        " to integer: no digits after the sign");
            }
            while (i <= last) {
                char d = s.charAt(i++);
                if (d >= '0' && d <= '9') {
                    value = 10*value + (d-'0');
                } else {
                    return numericError("Cannot convert string " + Err.wrap(s, Err.VALUE) + " to an integer");
                }
            }
            return new IntegerValue((negative ? -value : value));
        } else {
            // for longer numbers, rely on library routines
            try {
                CharSequence t = Whitespace.trimWhitespace(s);
                if (t.charAt(0) == '+') {
                    t = t.subSequence(1, t.length());
                }
                if (t.length() < 16) {
                    return new IntegerValue(Long.parseLong(t.toString()));
                } else {
                    return new BigIntegerValue(new BigInteger(t.toString()));
                }
            } catch (NumberFormatException err) {
                return numericError("Cannot convert string " + Err.wrap(s, Err.VALUE) + " to an integer");
            }
        }
    }

    /**
     * Helper method to handle errors converting a string to a number
     * @param message error message
     * @return an ErrorValue encapsulating an Exception describing the error
     */
    private static ValidationErrorValue numericError(String message) {
        ValidationException err = new ValidationException(message);
        err.setErrorCode("FORG0001");
        return new ValidationErrorValue(err);
    }

    /**
     * Check that a value is in range for the specified subtype of xs:integer
     *
     * @param value the value to be checked
     * @param type the required item type, a subtype of xs:integer
     * @return true if successful, false if value is out of range for the subtype
     */

    static boolean checkRange(long value, AtomicType type) {
        for (int i = 0; i < ranges.length; i += 3) {
            if (ranges[i] == type.getFingerprint()) {
                long min = ranges[i+1];
                if (min != NO_LIMIT && value < min) {
                    return false;
                }
                long max = ranges[i+2];
                if (max != NO_LIMIT && max != MAX_UNSIGNED_LONG && value > max) {
                    return false;
                }
                return true;
            }
        }
        throw new IllegalArgumentException(
                "No range information found for integer subtype " + type.getDescription());
    }

    /**
     * Check that a BigInteger is within the required range for a given integer subtype.
     * This method is expensive, so it should not be used unless the BigInteger is outside the range of a long.
     */

    static boolean checkBigRange(BigInteger big, AtomicType type) {

        for (int i = 0; i < ranges.length; i += 3) {
            if (ranges[i] == type.getFingerprint()) {
                long min = ranges[i+1];
                if (min != NO_LIMIT && BigInteger.valueOf(min).compareTo(big) > 0) {
                    return false;
                }
                long max = ranges[i+2];
                if (max == NO_LIMIT) {
                    return true;
                } else if (max == MAX_UNSIGNED_LONG) {
                    return BigIntegerValue.MAX_UNSIGNED_LONG.compareTo(big) >= 0;
                } else {
                    return BigInteger.valueOf(max).compareTo(big) >= 0;
                }
            }
        }
        throw new IllegalArgumentException(
                "No range information found for integer subtype " + type.getDescription());
    }

    /**
     * Static data identifying the min and max values for each built-in subtype of xs:integer.
     * This is a sequence of triples, each holding the fingerprint of the type, the minimum
     * value, and the maximum value. The special value NO_LIMIT indicates that there is no
     * minimum (or no maximum) for this type. The special value MAX_UNSIGNED_LONG represents the
     * value 2^64-1
     */
    private static long NO_LIMIT = -9999;
    private static long MAX_UNSIGNED_LONG = -9998;
    private static long[] ranges = {
        Type.INTEGER, NO_LIMIT, NO_LIMIT,
        Type.NON_POSITIVE_INTEGER, NO_LIMIT, 0,
        Type.NEGATIVE_INTEGER, NO_LIMIT, -1,
        Type.LONG, Long.MIN_VALUE, Long.MAX_VALUE,
        Type.INT, Integer.MIN_VALUE, Integer.MAX_VALUE,
        Type.SHORT, Short.MIN_VALUE, Short.MAX_VALUE,
        Type.BYTE, Byte.MIN_VALUE, Byte.MAX_VALUE,
        Type.NON_NEGATIVE_INTEGER, 0, NO_LIMIT,
        Type.POSITIVE_INTEGER, 1, NO_LIMIT,
        Type.UNSIGNED_LONG, 0, MAX_UNSIGNED_LONG,
        Type.UNSIGNED_INT, 0, 4294967295L,
        Type.UNSIGNED_SHORT, 0, 65535,
        Type.UNSIGNED_BYTE, 0, 255};


    /**
     * Get the hashCode. This must conform to the rules for other NumericValue hashcodes
     * @see NumericValue#hashCode
     */

    public int hashCode() {
        if (value > Integer.MIN_VALUE && value < Integer.MAX_VALUE) {
            return (int) value;
        } else {
            return new Double(this.getDoubleValue()).hashCode();
        }
    }

    /**
     * Get the value
     * @return the value of the xs:integer, as a Java long
     */

    public long longValue() {
        return value;
    }

    /**
     * Return the effective boolean value of this integer
     * @param context The dynamic evaluation context; ignored in this
     *     implementation of the method
     * @return false if the integer is zero, otherwise true
     */
    public boolean effectiveBooleanValue(XPathContext context) {
        return value != 0;
    }

    /**
     * Compare the value to another numeric value
     * @param other the numeric value to be compared to this value
     * @return -1 if this value is less than the other, 0 if they are equal,
     *     +1 if this value is greater
     */

    public int compareTo(Object other) {
        if (other instanceof IntegerValue) {
            long val2 = ((IntegerValue) other).value;
            if (value == val2) return 0;
            if (value < val2) return -1;
            return 1;
        } else if (other instanceof BigIntegerValue) {
            return new BigIntegerValue(value).compareTo(other);
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
        if (value == other) return 0;
        if (value < other) return -1;
        return 1;
    }

    /**
     * Convert to target data type
     *
     * @param requiredType an integer identifying the required atomic type
     * @param context
     * @return an AtomicValue, a value of the required type
     */

    public AtomicValue convertPrimitive(BuiltInAtomicType requiredType, boolean validate, XPathContext context) {
        switch (requiredType.getFingerprint()) {
            case Type.BOOLEAN:
                return BooleanValue.get(value != 0);

            case Type.NUMBER:
            case Type.INTEGER:
            case Type.ANY_ATOMIC:
            case Type.ITEM:
                return this;

            case Type.NON_POSITIVE_INTEGER:
            case Type.NEGATIVE_INTEGER:
            case Type.LONG:
            case Type.INT:
            case Type.SHORT:
            case Type.BYTE:
            case Type.NON_NEGATIVE_INTEGER:
            case Type.POSITIVE_INTEGER:
            case Type.UNSIGNED_LONG:
            case Type.UNSIGNED_INT:
            case Type.UNSIGNED_SHORT:
            case Type.UNSIGNED_BYTE:
                IntegerValue val = new IntegerValue(value);
                ValidationException err = val.convertToSubtype(requiredType, validate);
                if (err != null) {
                    return new ValidationErrorValue(err);
                }
                return val;

            case Type.DOUBLE:
                return new DoubleValue((double) value);

            case Type.FLOAT:
                return new FloatValue((float) value);

            case Type.DECIMAL:
                return new DecimalValue(value);

            case Type.STRING:
                return new StringValue(getStringValue());

            case Type.UNTYPED_ATOMIC:
                return new UntypedAtomicValue(getStringValue());

            default:
                ValidationException err2 = new ValidationException("Cannot convert integer to " +
                        requiredType.getDisplayName());
                err2.setErrorCode("XPTY0004");
                return new ValidationErrorValue(err2);
        }
    }

    /**
     * Get the value as a String
     *
     * @return a String representation of the value
     */

    public String getStringValue() {
        return value + "";
    }

    /**
     * Negate the value
     *
     * @return the result of inverting the sign of the value
     */

    public NumericValue negate() {
        return new IntegerValue(-value);
    }

    /**
     * Implement the XPath floor() function
     * @return the integer value, unchanged
     */

    public NumericValue floor() {
        return this;
    }

    /**
     * Implement the XPath ceiling() function
     * @return the integer value, unchanged
     */

    public NumericValue ceiling() {
        return this;
    }

    /**
     * Implement the XPath round() function
     * @return the integer value, unchanged
     */

    public NumericValue round() {
        return this;
    }

    /**
     * Implement the XPath round-to-half-even() function
     *
     * @param scale number of digits required after the decimal point; the
     *     value -2 (for example) means round to a multiple of 100
     * @return if the scale is >=0, return this value unchanged. Otherwise
     *     round it to a multiple of 10**-scale
     */

    public NumericValue roundHalfToEven(int scale) {
        long absolute = Math.abs(value);
        if (scale >= 0) {
            return this;
        } else {
            if (scale < -15) {
                return new BigIntegerValue(value).roundHalfToEven(scale);
            }
            long factor = 1;
            for (long i = 1; i <= -scale; i++) {
                factor *= 10;
            }
            long modulus = absolute % factor;
            long rval = absolute - modulus;
            long d = modulus * 2;
            if (d > factor) {
                rval += factor;
            } else if (d < factor) {
                // no-op
            } else {
                // round to even
                if (rval % (2 * factor) == 0) {
                    // no-op
                } else {
                    rval += factor;
                }
            }
            if (value < 0) rval = -rval;
            return new IntegerValue(rval);
        }
    }

    /**
     * Determine whether the value is negative, zero, or positive
     * @return -1 if negative, 0 if zero, +1 if positive, NaN if NaN
     */

    public double signum() {
        if (value > 0) return +1;
        if (value == 0) return 0;
        return -1;
    }

    /**
     * Determine whether the value is a whole number, that is, whether it compares
     * equal to some integer
     *
     * @return always true for this implementation
     */

    public boolean isWholeNumber() {
        return true;
    }

    /**
     * Evaluate a binary arithmetic operator.
     *
     * @param operator the operator to be applied, identified by a constant in
     *      the Tokenizer class
     * @param other the other operand of the arithmetic expression
     * @exception XPathException if an arithmetic failure occurs, e.g. divide
     *     by zero
     * @return the result of performing the arithmetic operation
     */

    public NumericValue arithmetic(int operator, NumericValue other, XPathContext context) throws XPathException {

        if (other instanceof IntegerValue) {
            // if either of the values is large, we use BigInteger arithmetic to be on the safe side
            if (value >= Integer.MAX_VALUE || value <= Integer.MIN_VALUE ||
                    ((IntegerValue) other).value >= Integer.MAX_VALUE ||
                    ((IntegerValue) other).value <= Integer.MIN_VALUE) {
                return new BigIntegerValue(value).arithmetic(operator, other, context);
            }
            switch (operator) {
                case Token.PLUS:
                    return new IntegerValue(value + ((IntegerValue) other).value);
                case Token.MINUS:
                    return new IntegerValue(value - ((IntegerValue) other).value);
                case Token.MULT:
                    return new IntegerValue(value * ((IntegerValue) other).value);
                case Token.IDIV:
                    try {
                        return new IntegerValue(value / ((IntegerValue) other).value);
                    } catch (ArithmeticException err) {
                        DynamicError e;
                        if ("/ by zero".equals(err.getMessage())) {
                            e = new DynamicError("Integer division by zero");
                            e.setErrorCode("FOAR0001");
                        } else {
                            e = new DynamicError("Integer division failure", err);
                        }
                        e.setXPathContext(context);
                        throw e;
                    }
                case Token.DIV:
                    // the result of dividing two integers is a decimal; but if
                    // one divides exactly by the other, we implement it as an integer
                    long quotient = ((IntegerValue) other).value;
                    if (quotient == 0) {
                        DynamicError err = new DynamicError("Integer division by zero");
                        err.setXPathContext(context);
                        err.setErrorCode("FOAR0001");
                        throw err;
                    }
                    if (value % quotient == 0) {
                        return new IntegerValue(value / quotient);
                    }
                    return new DecimalValue(value).arithmetic(Token.DIV,
                            new DecimalValue(quotient), context);
                case Token.MOD:
                    return new IntegerValue(value % ((IntegerValue) other).value);
                default:
                    throw new UnsupportedOperationException("Unknown operator");
            }
        } else if (other instanceof BigIntegerValue) {
            return new BigIntegerValue(value).arithmetic(operator, other, context);
        } else {
            NumericValue v = (NumericValue) convert(other.getItemType(null).getPrimitiveType(), context);
            return v.arithmetic(operator, other, context);
        }
    }

    /**
     * Determine the data type of the expression
     * @return the actual data type
     * @param th
     */

    public ItemType getItemType(TypeHierarchy th) {
        return type;
    }

    /**
     * Get conversion preference for this value to a Java class.
     *
     * @param required the Java class to which conversion is required
     * @return the conversion preference. A low result indicates higher
     *     preference.
     */

    // Note: this table gives java Long preference over Integer, even if the
    // XML Schema type is xs:int

    /**
     * Convert to Java object (for passing to external functions)
     *
     * @param target The Java class to which conversion is required
     * @exception XPathException if conversion is not possible, or fails
     * @return the Java object that results from the conversion; always an
     *     instance of the target class
     */

    public Object convertToJava(Class target, XPathContext context) throws XPathException {
        if (target == Object.class) {
            return new Long(value);
        } else if (target.isAssignableFrom(IntegerValue.class)) {
            return this;
        } else if (target == boolean.class) {
            BooleanValue bval = (BooleanValue) convert(Type.BOOLEAN, context);
            return Boolean.valueOf(bval.getBooleanValue());
        } else if (target == Boolean.class) {
            BooleanValue bval = (BooleanValue) convert(Type.BOOLEAN, context);
            return Boolean.valueOf(bval.getBooleanValue());
        } else if (target == String.class || target == CharSequence.class) {
            return getStringValue();
        } else if (target == double.class || target == Double.class) {
            return new Double(value);
        } else if (target == float.class || target == Float.class) {
            return new Float(value);
        } else if (target == long.class || target == Long.class) {
            return new Long(value);
        } else if (target == int.class || target == Integer.class) {
            return new Integer((int) value);
        } else if (target == short.class || target == Short.class) {
            return new Short((short) value);
        } else if (target == byte.class || target == Byte.class) {
            return new Byte((byte) value);
        } else if (target == char.class || target == Character.class) {
            return new Character((char) value);
        } else if (target == BigInteger.class) {
            return BigInteger.valueOf(value);
        } else if (target == BigDecimal.class) {
            return BigDecimal.valueOf(value);
        } else {
            Object o = super.convertToJava(target, context);
            if (o == null) {
                throw new DynamicError("Conversion of integer to " + target.getName() +
                        " is not supported");
            }
            return o;
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
// The Original Code is: all this file except the asStringXT() and zeros() methods (not currently used).
//
// The Initial Developer of the Original Code is Michael H. Kay.
//
// Portions created by (xt) are Copyright (C) (James Clark). All Rights Reserved.
//
// Contributor(s): none.
//

