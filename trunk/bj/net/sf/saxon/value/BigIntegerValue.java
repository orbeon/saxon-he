package net.sf.saxon.value;
import net.sf.saxon.expr.Token;
import net.sf.saxon.expr.XPathContext;
import net.sf.saxon.trans.DynamicError;
import net.sf.saxon.trans.XPathException;
import net.sf.saxon.type.*;

import java.math.BigDecimal;
import java.math.BigInteger;

/**
 * An integer value: note this is a subtype of decimal in XML Schema, not a primitive type.
 * The class IntegerValue is used to represent xs:integer values that fit comfortably
 * in a Java long; this class is used to represent xs:integer values outside this range,
 * including the built-in subtype xs:unsignedLong
 */

public final class BigIntegerValue extends NumericValue {

    private BigInteger value;
    private ItemType type;

    private static final BigInteger MAX_INT = BigInteger.valueOf(Integer.MAX_VALUE);
    private static final BigInteger MIN_INT = BigInteger.valueOf(Integer.MIN_VALUE);
    private static final BigInteger MAX_LONG = BigInteger.valueOf(Long.MAX_VALUE);
    private static final BigInteger MIN_LONG = BigInteger.valueOf(Long.MIN_VALUE);
    public static final BigInteger MAX_UNSIGNED_LONG = new BigInteger("18446744073709551615");
    public static final BigIntegerValue ZERO = new BigIntegerValue(BigInteger.ZERO);

    public BigIntegerValue(BigInteger value) {
        this.value = value;
        this.type = Type.INTEGER_TYPE;
    }

    public BigIntegerValue(long value) {
        this.value = BigInteger.valueOf(value);
        this.type = Type.INTEGER_TYPE;
    }

    /**
     * This class allows subtypes of xs:integer to be held, as well as xs:integer values.
     * This method sets the required type label.
     * @param type the subtype of integer required
     * @return null if the operation succeeds, or a ValidationException if the value is out of range
     */

    public ValidationException convertToSubType(AtomicType type, boolean validate) {
        if (!validate) {
            this.type = type;
            return null;
        }
        if (IntegerValue.checkBigRange(value, type)) {
            this.type = type;
            return null;
        } else {
            ValidationException err = new ValidationException(
                    "Integer value is out of range for subtype " + type.getDisplayName());
            err.setErrorCode("FORG0001");
            return err;
        }
    }

    /**
     * Factory method: makes either an IntegerValue or a BigIntegerValue depending on the value supplied
     */

    public static NumericValue makeValue(BigInteger value) {
        if (value.compareTo(MAX_LONG) > 0 || value.compareTo(MIN_LONG) < 0) {
            return new BigIntegerValue(value);
        } else {
            return new IntegerValue(value.longValue());
        }
    }

    /**
     * Get the hashCode. This must conform to the rules for other NumericValue hashcodes
     * @see NumericValue#hashCode
     */

    public int hashCode() {
        if (value.compareTo(MIN_INT) >= 0 && value.compareTo(MAX_INT) <= 0) {
            return value.intValue();
        } else {
            return new Double(this.getDoubleValue()).hashCode();
        }
    }

    /**
     * Get the value as a long
     *
     * @return the value of the xs:integer, as a Java long
     */

    public long longValue() {
        return value.longValue();
    }

    /**
     * Get the value as a BigInteger
     * @return teh value of the xs:integer as a Java BigInteger
     */

    public BigInteger getBigInteger() {
        return value;
    }

    public boolean isWithinLongRange() {
        return value.compareTo(MIN_LONG) >= 0 && value.compareTo(MAX_LONG) <= 0;
    }

    public BigDecimal asDecimal() {
        return new BigDecimal(value);
    }

    /**
     * Return the effective boolean value of this integer
     *
     * @param context The dynamic evaluation context; ignored in this
     *     implementation of the method
     * @return false if the integer is zero, otherwise true
     */
    public boolean effectiveBooleanValue(XPathContext context) {
        return value.compareTo(BigInteger.ZERO) != 0;
    }

    /**
     * Compare the value to another numeric value
     *
     * @param other the numeric value to be compared to this value
     * @return -1 if this value is less than the other, 0 if they are equal,
     *     +1 if this value is greater
     */

    public int compareTo(Object other) {
        if (other instanceof BigIntegerValue) {
            return value.compareTo(((BigIntegerValue)other).value);
        } else if (other instanceof DecimalValue) {
            return asDecimal().compareTo(((DecimalValue)other).getValue());
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
        return value.compareTo((BigInteger.valueOf(other)));
    }


    /**
     * Convert to target data type
     *
     * @param requiredType an integer identifying the required atomic type
     * @param context
     * @return an AtomicValue, a value of the required type; or an ErrorValue
     */

    public AtomicValue convertPrimitive(BuiltInAtomicType requiredType, boolean validate, XPathContext context) {
        switch (requiredType.getFingerprint()) {
            case Type.BOOLEAN:
                return BooleanValue.get(effectiveBooleanValue(null));

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
            case Type.UNSIGNED_INT:
            case Type.UNSIGNED_SHORT:
            case Type.UNSIGNED_BYTE:
                if (isWithinLongRange()) {
                    IntegerValue val = new IntegerValue(longValue());
                    ValidationException err = val.convertToSubtype(requiredType, validate);
                    if (err == null) {
                        return val;
                    } else {
                        return new ValidationErrorValue(err);
                    }
                } else {
                    BigIntegerValue val = new BigIntegerValue(value);
                    ValidationException err = val.convertToSubType(requiredType, validate);
                    if (err == null) {
                        return val;
                    } else {
                        return new ValidationErrorValue(err);
                    }
                }

            case Type.UNSIGNED_LONG:
                if (value.signum() < 0 || value.bitLength() > 64) {
                    ValidationException err = new ValidationException("Integer value is out of range for type " +
                            requiredType.getDisplayName());
                    err.setErrorCode("FORG0001");
                    //err.setXPathContext(context);
                    return new ValidationErrorValue(err);
                } else if (isWithinLongRange()) {
                    IntegerValue val = new IntegerValue(longValue());
                    ValidationException err = val.convertToSubtype(requiredType, validate);
                    if (err != null) {
                        return new ValidationErrorValue(err);
                    }
                    return val;
                } else {
                    BigIntegerValue nv = new BigIntegerValue(value);
                    ValidationException err = nv.convertToSubType(requiredType, validate);
                    if (err != null) {
                        return new ValidationErrorValue(err);
                    }
                    return nv;
                }

            case Type.DOUBLE:
                return new DoubleValue(value.doubleValue());

            case Type.FLOAT:
                return new FloatValue(value.floatValue());

            case Type.DECIMAL:
                return new DecimalValue(new BigDecimal(value));

            case Type.STRING:
                return new StringValue(getStringValueCS());

            case Type.UNTYPED_ATOMIC:
                return new UntypedAtomicValue(getStringValueCS());

            default:
                ValidationException err = new ValidationException("Cannot convert integer to " +
                                         requiredType.getDisplayName());
                err.setErrorCode("XPTY0004");
                err.setIsTypeError(true);
                return new ValidationErrorValue(err);
        }
    }

    /**
     * Get the value as a String
     * @return a String representation of the value
     */

    public String getStringValue() {
        return value.toString();
    }

    /**
     * Negate the value
     * @return the result of inverting the sign of the value
     */

    public NumericValue negate() {
        return new BigIntegerValue(value.negate());
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
        if (scale >= 0) {
            return this;
        } else {
            BigInteger factor = BigInteger.valueOf(10).pow(-scale);
            BigInteger[] pair = value.divideAndRemainder(factor);
            int up = pair[1].compareTo(factor.divide(BigInteger.valueOf(2)));
            if (up > 0) {
                // remainder is > .5
                pair[0] = pair[0].add(BigInteger.valueOf(1));
            } else if (up == 0) {
                // remainder == .5
                if (pair[0].mod(BigInteger.valueOf(2)).signum() != 0) {
                    // last digit of quotient is odd: make it even
                    pair[0] = pair[0].add(BigInteger.valueOf(1));
                }
            }
            return makeValue(pair[0].multiply(factor));
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
        if (other instanceof BigIntegerValue) {
            BigInteger that = ((BigIntegerValue)other).value;
            switch(operator) {
                case Token.PLUS:
                    return makeValue(value.add(that));
                case Token.MINUS:
                    return makeValue(value.subtract(that));
                case Token.MULT:
                    return makeValue(value.multiply(that));
                case Token.IDIV:
                    try {
                        return makeValue(value.divide(that));
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
                    DecimalValue a = new DecimalValue(new BigDecimal(value));
                    DecimalValue b = new DecimalValue(new BigDecimal(that));
                    return a.arithmetic(operator, b, context);
                case Token.MOD:
                    return makeValue(value.remainder(that));
                default:
                    throw new UnsupportedOperationException("Unknown operator");
            }
        } else if (other instanceof IntegerValue) {
            final BigIntegerValue val = new BigIntegerValue(other.longValue());
            return arithmetic(operator, val, context);
        } else {
            final TypeHierarchy th = context.getConfiguration().getTypeHierarchy();
            final NumericValue v = (NumericValue)convert(other.getItemType(th).getPrimitiveType(), context);
            return v.arithmetic(operator, other, context);
        }
    }


    /**
     * Determine the data type of the expression
     *
     * @return the actual data type
     * @param th
     */

    public ItemType getItemType(TypeHierarchy th) {
        return type;
    }

    /**
     * Convert to Java object (for passing to external functions)
     *
     * @param target The Java class to which conversion is required
     * @exception XPathException if conversion is not possible, or fails
     * @return the Java object that results from the conversion; always an
     *     instance of the target class
     */

    public Object convertToJava(Class target, XPathContext context) throws XPathException {
        if (isWithinLongRange()) {
            IntegerValue val = new IntegerValue(longValue());
            return val.convertToJava(target, context);
        } else if (target.isAssignableFrom(IntegerValue.class)) {
            return this;
        } else if (target == BigInteger.class) {
            return value;
        } else {
            return convert(Type.DECIMAL, context).convertToJava(target, context);
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

