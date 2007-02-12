package net.sf.saxon.value;

import net.sf.saxon.expr.Token;
import net.sf.saxon.expr.XPathContext;
import net.sf.saxon.expr.Calculator;
import net.sf.saxon.trans.DynamicError;
import net.sf.saxon.trans.XPathException;
import net.sf.saxon.type.*;

import java.math.BigDecimal;
import java.math.BigInteger;

/**
 * An integer value: note this is a subtype of decimal in XML Schema, not a primitive type.
 * This class supports integer values in the range permitted by a Java "long",
 * and also supports the built-in subtypes of xs:integer.
 */

public final class Int64Value extends IntegerValue {

    private long value;

    /**
     * Constructor supplying a long
     *
     * @param value the value of the IntegerValue
     */

    public Int64Value(long value) {
        this.value = value;
        this.typeLabel = BuiltInAtomicType.INTEGER;
    }

    /**
     * Constructor for a subtype, supplying a long and a type label.
     *
     * @param val The supplied value, as an integer
     * @param type the required item type, a subtype of xs:integer
     * @param check Set to true if the method is required to check that the value is in range;
     * false if the caller can guarantee that the value has already been checked.
     * @exception DynamicError if the supplied value is out of range for the
     *      target type
     */

    public Int64Value(long val, AtomicType type, boolean check) throws DynamicError {
        this.value = val;
        this.typeLabel = type;
        if (check && !checkRange(value, type)) {
            DynamicError err = new DynamicError("Integer value " + val +
                    " is out of range for the requested type " + type.getDescription());
            err.setErrorCode("XPTY0004");
            err.setIsTypeError(true);
            throw err;
        };
    }

    /**
     * Factory method: allows Int64Value objects to be reused. Note that
     * a value obtained using this method must not be modified to set a type label, because
     * the value is in general shared.
     */

    public static Int64Value makeIntegerValue(long value) {
        if (value <= 20 && value >= 0) {
            return SMALL_INTEGERS[(int)value];
        } else {
            return new Int64Value(value);
        }
    }

    /**
     * Factory method to create a derived value, with no checking of the value against the
     * derived type
     */

    public static Int64Value makeDerived(long val, AtomicType type)  {
        Int64Value v = new Int64Value(val);
        v.typeLabel = type;
        return v;
    }

    /**
     * Factory method returning the integer -1, 0, or +1 according as the argument
     * is negative, zero, or positive
     */

    public static Int64Value signum(long val) {
        if (val == 0) {
            return IntegerValue.ZERO;
        } else {
            return (val < 0 ? IntegerValue.MINUS_ONE : IntegerValue.PLUS_ONE);
        }
    }

    /**
     * Create a copy of this atomic value, with a different type label
     *
     * @param typeLabel the type label of the new copy. The caller is responsible for checking that
     *                  the value actually conforms to this type.
     */

    public AtomicValue copy(AtomicType typeLabel) {
        Int64Value v = new Int64Value(value);
        v.typeLabel = typeLabel;
        return v;
    }

    /**
     * Convert the value to a subtype of xs:integer
     * @param subtype the target subtype
     * @param validate true if validation is required; false if the caller already knows that the value is valid
     * @return null if the conversion succeeds; a ValidationException describing the failure if it fails. Note
     * that the exception is returned, not thrown.
     */

    public ValidationException convertToSubType(AtomicType subtype, boolean validate) {
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
        this.typeLabel = type;
    }

    /**
     * This class allows subtypes of xs:integer to be held, as well as xs:integer values.
     * This method checks that the value is within range, and also sets the type label.
     * @param type the subtype of integer required
     * @return true if successful, false if value is out of range for the subtype
     */
    public boolean checkRange(AtomicType type) {
        this.typeLabel = type;
        return checkRange(value, type);
    }


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
     * @return false if the integer is zero, otherwise true
     */
    public boolean effectiveBooleanValue() {
        return value != 0;
    }

    /**
     * Compare the value to another numeric value
     * @param other the numeric value to be compared to this value
     * @return -1 if this value is less than the other, 0 if they are equal,
     *     +1 if this value is greater
     */

    public int compareTo(Object other) {
        if (other instanceof Int64Value) {
            long val2 = ((Int64Value) other).value;
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
                Int64Value val = new Int64Value(value);
                ValidationException err = val.convertToSubType(requiredType, validate);
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
        return Long.toString(value);
    }

    /**
     * Get the numeric value as a double
     *
     * @return A double representing this numeric value; NaN if it cannot be
     *         converted
     */
    public double getDoubleValue() {
        return (double) value;
    }

    /**
     * Get the numeric value converted to a float
     *
     * @return a float representing this numeric value; NaN if it cannot be converted
     */

    public float getFloatValue() {
        return (float)value;
    }

    /**
     * Get the numeric value converted to a decimal
     *
     * @return a decimal representing this numeric value;
     */

    public BigDecimal getDecimalValue() {
        return new BigDecimal(value);
    }

    /**
     * Negate the value
     *
     * @return the result of inverting the sign of the value
     */

    public NumericValue negate() {
        // TODO: negative range is not the same as positive range
        return new Int64Value(-value);
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
            return new Int64Value(rval);
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

        if (other instanceof Int64Value) {
            // if either of the values is large, we use BigInteger arithmetic to be on the safe side
            if (value >= Integer.MAX_VALUE || value <= Integer.MIN_VALUE ||
                    ((Int64Value) other).value >= Integer.MAX_VALUE ||
                    ((Int64Value) other).value <= Integer.MIN_VALUE) {
                return new BigIntegerValue(value).arithmetic(operator, other, context);
            }
            switch (operator) {
                case Token.PLUS:
                    return makeIntegerValue(value + ((Int64Value) other).value);
                case Token.MINUS:
                    return makeIntegerValue(value - ((Int64Value) other).value);
                case Token.MULT:
                    return makeIntegerValue(value * ((Int64Value) other).value);
                case Token.IDIV:
                    try {
                        return makeIntegerValue(value / ((Int64Value) other).value);
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
                    long quotient = ((Int64Value) other).value;
                    if (quotient == 0) {
                        DynamicError err = new DynamicError("Integer division by zero");
                        err.setXPathContext(context);
                        err.setErrorCode("FOAR0001");
                        throw err;
                    }
                    if (value % quotient == 0) {
                        return makeIntegerValue(value / quotient);
                    }
                    return new DecimalValue(value).arithmetic(Token.DIV,
                            new DecimalValue(quotient), context);
                case Token.MOD:
                    return makeIntegerValue(value % ((Int64Value) other).value);
                default:
                    throw new UnsupportedOperationException("Unknown operator");
            }
        } else if (other instanceof BigIntegerValue) {
            return new BigIntegerValue(value).arithmetic(operator, other, context);
        } else {
            NumericValue v = (NumericValue) convert(other.getPrimitiveType(), context);
            return v.arithmetic(operator, other, context);
        }
    }

    /**
     * Add another integer
     */

    public IntegerValue plus(IntegerValue other) {
        // if either of the values is large, we use BigInteger arithmetic to be on the safe side
        if (other instanceof Int64Value) {
            long topa = (value >> 60) & 0xf;
            if (topa != 0 && topa != 0xf) {
                return new BigIntegerValue(value).plus(new BigIntegerValue(((Int64Value)other).value));
            }
            long topb = (((Int64Value)other).value >> 60) & 0xf;
            if (topb != 0 && topb != 0xf) {
                return new BigIntegerValue(value).plus(new BigIntegerValue(((Int64Value)other).value));
            }
            return makeIntegerValue(value + ((Int64Value)other).value);
        } else {
            return new BigIntegerValue(value).plus(other);
        }
    }

    /**
     * Subtract another integer
     */

    public IntegerValue minus(IntegerValue other) {
        // if either of the values is large, we use BigInteger arithmetic to be on the safe side
        if (other instanceof Int64Value) {
            long topa = (value >> 60) & 0xf;
            if (topa != 0 && topa != 0xf) {
                return new BigIntegerValue(value).minus(new BigIntegerValue(((Int64Value)other).value));
            }
            long topb = (((Int64Value)other).value >> 60) & 0xf;
            if (topb != 0 && topb != 0xf) {
                return new BigIntegerValue(value).minus(new BigIntegerValue(((Int64Value)other).value));
            }
            return makeIntegerValue(value - ((Int64Value)other).value);
        } else {
            return new BigIntegerValue(value).minus(other);
        }
    }

    /**
     * Multiply by another integer
     */

    public IntegerValue times(IntegerValue other) {
        // if either of the values is large, we use BigInteger arithmetic to be on the safe side
        if (other instanceof Int64Value) {
            long topa = (value >> 32) & 0xffffffff;
            if (topa != 0 && topa != 0xffffffff) {
                return new BigIntegerValue(value).times(new BigIntegerValue(((Int64Value)other).value));
            }
            long topb = (((Int64Value)other).value >> 32) & 0xffffffff;
            if (topb != 0 && topb != 0xffffffff) {
                return new BigIntegerValue(value).times(new BigIntegerValue(((Int64Value)other).value));
            }
            return makeIntegerValue(value * ((Int64Value)other).value);
        } else {
            return new BigIntegerValue(value).times(other);
        }
    }

    /**
     * Divide by another integer
     */

    public NumericValue div(IntegerValue other) throws XPathException {
        // if either of the values is large, we use BigInteger arithmetic to be on the safe side
        if (other instanceof Int64Value) {
            long topa = (value >> 32) & 0xffffffff;
            if (topa != 0 && topa != 0xffffffff) {
                return new BigIntegerValue(value).div(new BigIntegerValue(((Int64Value)other).value));
            }
            long topb = (((Int64Value)other).value >> 32) & 0xffffffff;
            if (topb != 0 && topb != 0xffffffff) {
                return new BigIntegerValue(value).div(new BigIntegerValue(((Int64Value)other).value));
            }
            // the result of dividing two integers is a decimal; but if
            // one divides exactly by the other, we implement it as an integer
            long quotient = ((Int64Value) other).value;
            if (quotient == 0) {
                throw new DynamicError("Integer division by zero", "FOAR0001");
            }
            if (value % quotient == 0) {
                return makeIntegerValue(value / quotient);
            }
            return (NumericValue)Calculator.DECIMAL_DECIMAL[Calculator.DIV].compute(
                            new DecimalValue(value), new DecimalValue(quotient), null);
        } else {
            return new BigIntegerValue(value).div(other);
        }
    }

    /**
     * Take modulo another integer
     */

    public IntegerValue mod(IntegerValue other) throws XPathException {
        // if either of the values is large, we use BigInteger arithmetic to be on the safe side
        if (other instanceof Int64Value) {
            long topa = (value >> 32) & 0xffffffff;
            if (topa != 0 && topa != 0xffffffff) {
                return new BigIntegerValue(value).mod(new BigIntegerValue(((Int64Value)other).value));
            }
            long topb = (((Int64Value)other).value >> 32) & 0xffffffff;
            if (topb != 0 && topb != 0xffffffff) {
                return new BigIntegerValue(value).mod(new BigIntegerValue(((Int64Value)other).value));
            }
            return makeIntegerValue(value % ((Int64Value) other).value);
        } else {
            return new BigIntegerValue(value).mod(other);
        }
    }

    /**
     * Integer divide by another integer
     */

    public IntegerValue idiv(IntegerValue other) throws XPathException {
       // if either of the values is large, we use BigInteger arithmetic to be on the safe side
        if (other instanceof Int64Value) {
            long topa = (value >> 32) & 0xffffffff;
            if (topa != 0 && topa != 0xffffffff) {
                return new BigIntegerValue(value).idiv(new BigIntegerValue(((Int64Value)other).value));
            }
            long topb = (((Int64Value)other).value >> 32) & 0xffffffff;
            if (topb != 0 && topb != 0xffffffff) {
                return new BigIntegerValue(value).idiv(new BigIntegerValue(((Int64Value)other).value));
            }
            try {
                return makeIntegerValue(value / ((Int64Value) other).value);
            } catch (ArithmeticException err) {
                DynamicError e;
                if ("/ by zero".equals(err.getMessage())) {
                    e = new DynamicError("Integer division by zero", "FOAR0001");
                } else {
                    e = new DynamicError("Integer division failure", err);
                }
                throw e;
            }
        } else {
            return new BigIntegerValue(value).times(other);
        }        
    }

    /**
     * Get the value as a BigInteger
     */

    public BigInteger asBigInteger() {
        return BigInteger.valueOf(value);
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
        } else if (target.isAssignableFrom(Int64Value.class)) {
            return this;
        } else if (target == boolean.class) {
            BooleanValue bval = (BooleanValue) convert(BuiltInAtomicType.BOOLEAN, context);
            return Boolean.valueOf(bval.getBooleanValue());
        } else if (target == Boolean.class) {
            BooleanValue bval = (BooleanValue) convert(BuiltInAtomicType.BOOLEAN, context);
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

