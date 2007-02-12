package net.sf.saxon.value;
import net.sf.saxon.Err;
import net.sf.saxon.expr.Token;
import net.sf.saxon.expr.XPathContext;
import net.sf.saxon.om.FastStringBuffer;
import net.sf.saxon.trans.DynamicError;
import net.sf.saxon.trans.XPathException;
import net.sf.saxon.type.*;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.regex.Pattern;

/**
* A decimal value
*/

public final class DecimalValue extends NumericValue {

    public static final int DIVIDE_PRECISION = 18;
    private static boolean stripTrailingZerosMethodUnavailable = false;
    private static Method stripTrailingZerosMethod = null;
    private static boolean canSetScaleNegative = true;  // until proved otherwise
    private static final Object[] EMPTY_OBJECT_ARRAY = {};

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
        this.value = stripTrailingZeros(value);
        this.typeLabel = BuiltInAtomicType.DECIMAL;
    }

    private static final Pattern decimalPattern = Pattern.compile("(\\-|\\+)?((\\.[0-9]+)|([0-9]+(\\.[0-9]*)?))");

    /**
    * Factory method to construct a DecimalValue from a string
    * @param in the value of the DecimalValue
     * @param validate true if validation is required; false if the caller knows that the value is valid
     * @return the required DecimalValue if the input is valid, or an ErrorValue encapsulating the error
     * message if not.
     */

    public static AtomicValue makeDecimalValue(CharSequence in, boolean validate) {
        // TODO: tune this method. Do the trimming, validation, and losing trailing zeros in a single pass.
        // Use the BigDecimal(integer, scale) constructor.

        String trimmed = Whitespace.trimWhitespace(in).toString();
        try {
            if (validate) {
                if (!decimalPattern.matcher(trimmed).matches()) {
                    ValidationException err = new ValidationException(
                            "Cannot convert string " + Err.wrap(trimmed, Err.VALUE) + " to xs:decimal");
                    err.setErrorCode("FORG0001");
                    return new ValidationErrorValue(err);
                }
            }
            BigDecimal val = new BigDecimal(trimmed);
            val = stripTrailingZeros(val);
            DecimalValue value = new DecimalValue(val);
            return value;
        } catch (NumberFormatException err) {
            ValidationException e = new ValidationException(
                    "Cannot convert string " + Err.wrap(trimmed, Err.VALUE) + " to xs:decimal");
            e.setErrorCode("FORG0001");
            return new ValidationErrorValue(e);
        }
    }

    /**
     * Test whether a string is castable to a decimal value
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
            this.value = stripTrailingZeros(new BigDecimal(in));
        } catch (NumberFormatException err) {
            // Must be a special value such as NaN or infinity
            ValidationException e = new ValidationException(
                    "Cannot convert double " + Err.wrap(in+"", Err.VALUE) + " to decimal");
            e.setErrorCode("FOCA0002");
            throw e;
        }
        this.typeLabel = BuiltInAtomicType.DECIMAL;
    }

    /**
    * Constructor supplying a long integer
    * @param in the value of the DecimalValue
    */

    public DecimalValue(long in) {
        this.value = BigDecimal.valueOf(in);
        this.typeLabel = BuiltInAtomicType.DECIMAL;
    }

    /**
     * Create a copy of this atomic value, with a different type label
     *
     * @param typeLabel the type label of the new copy. The caller is responsible for checking that
     *                  the value actually conforms to this type.
     */

    public AtomicValue copy(AtomicType typeLabel) {
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
    * Remove insignificant trailing zeros (the Java BigDecimal class retains trailing zeros,
    * but the XPath 2.0 xs:decimal type does not). The BigDecimal#stripTrailingZeros() method
    * was introduced in JDK 1.5: we use it if available, and simulate it if not.
    */

    private static BigDecimal stripTrailingZeros(BigDecimal value) {
        if (stripTrailingZerosMethodUnavailable) {
            return stripTrailingZerosFallback(value);
        }

        try {
            if (stripTrailingZerosMethod == null) {
                Class[] argTypes = {};
                stripTrailingZerosMethod = BigDecimal.class.getMethod("stripTrailingZeros", argTypes);
            }
            Object result = stripTrailingZerosMethod.invoke(value, EMPTY_OBJECT_ARRAY);
            return (BigDecimal)result;
        } catch (NoSuchMethodException e) {
            stripTrailingZerosMethodUnavailable = true;
            return stripTrailingZerosFallback(value);
        } catch (IllegalAccessException e) {
            stripTrailingZerosMethodUnavailable = true;
            return stripTrailingZerosFallback(value);
        } catch (InvocationTargetException e) {
            stripTrailingZerosMethodUnavailable = true;
            return stripTrailingZerosFallback(value);
        }

    }

    private static BigDecimal stripTrailingZerosFallback(BigDecimal value) {

        // The code below differs from JDK 1.5 stripTrailingZeros in that it does not remove trailing zeros
        // from integers, for example 1000 is not changed to 1E3.

        int scale = value.scale();
        if (scale > 0) {
            BigInteger i = value.unscaledValue();
            while (true) {
                BigInteger[] dr = i.divideAndRemainder(BIG_INTEGER_TEN);
                if (dr[1].equals(BigInteger.ZERO)) {
                    i = dr[0];
                    scale--;
                    if (scale==0) {
                        break;
                    }
                } else {
                    break;
                }
            }
            if (scale != value.scale()) {
                value = new BigDecimal(i, scale);
            }
        }
        return value;
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
            return new Double(this.getDoubleValue()).hashCode();
        }
    }

    public boolean effectiveBooleanValue() {
        return value.signum() != 0;
    }

    /**
    * Convert to target data type
    */

    public AtomicValue convertPrimitive(BuiltInAtomicType requiredType, boolean validate, XPathContext context) {
        switch(requiredType.getPrimitiveType()) {
        case Type.BOOLEAN:
                // 0.0 => false, anything else => true
            return BooleanValue.get(value.signum()!=0);
        case Type.NUMBER:
        case Type.DECIMAL:
        case Type.ANY_ATOMIC:
        case Type.ITEM:
            return this;
        case Type.INTEGER:
            return BigIntegerValue.makeIntegerValue(value.toBigInteger());
        case Type.DOUBLE:
            return new DoubleValue(value.doubleValue());
        case Type.FLOAT:
            return new FloatValue(value.floatValue());
        case Type.STRING:
            return new StringValue(getStringValueCS());
        case Type.UNTYPED_ATOMIC:
            return new UntypedAtomicValue(getStringValueCS());
        default:
            ValidationException err = new ValidationException("Cannot convert decimal to " +
                                     requiredType.getDisplayName());
            err.setErrorCode("XPTY0004");
            err.setIsTypeError(true);
            return new ValidationErrorValue(err);
        }
    }

    /**
     * Get the value of the item as a CharSequence. This is in some cases more efficient than
     * the version of the method that returns a String.
     */

    public CharSequence getStringValueCS() {
        return decimalToString(value);
    }

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

    public String getStringValue() {
        return decimalToString(value).toString();
    }

    public static CharSequence decimalToString(BigDecimal value) {
        // Can't use the plain BigDecimal#toString() under JDK 1.5 because this produces values like "1E-5".
        // JDK 1.5 offers BigDecimal#toPlainString() which might do the job directly
        int scale = value.scale();
        if (scale == 0) {
            return value.toString();
        } else if (scale < 0) {
            String s = value.abs().unscaledValue().toString();
            if (s.equals("0")) {
                return s;
            }
            FastStringBuffer sb = new FastStringBuffer(s.length() + (-scale) + 2);
            if (value.signum() < 0) {
                sb.append('-');
            }
            sb.append(s);
            for (int i=0; i<(-scale); i++) {
                sb.append('0');
            }
            return sb;
        } else {
            String s = value.abs().unscaledValue().toString();
            if (s.equals("0")) {
                return s;
            }
            int len = s.length();
            FastStringBuffer sb = new FastStringBuffer(len+1);
            if (value.signum() < 0) {
                sb.append('-');
            }
            if (scale >= len) {
                sb.append("0.");
                for (int i=len; i<scale; i++) {
                    sb.append('0');
                }
                sb.append(s);
            } else {
                sb.append(s.substring(0, len-scale));
                sb.append('.');
                sb.append(s.substring(len-scale));
            }
            return sb;
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
                AtomicValue val = convert(BuiltInAtomicType.INTEGER, null);
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
            return new DecimalValue(stripTrailingZeros(scaledValue));
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
               value.equals(value.setScale(0, BigDecimal.ROUND_DOWN));
    }

    /**
    * Evaluate a binary arithmetic operator.
    */

    public NumericValue arithmetic(int operator, NumericValue other, XPathContext context) throws XPathException {
        if (other instanceof DecimalValue) {
            try {
                switch(operator) {
                    case Token.PLUS:
                        return new DecimalValue(value.add(((DecimalValue)other).value));
                    case Token.MINUS:
                        return new DecimalValue(value.subtract(((DecimalValue)other).value));
                    case Token.MULT:
                        return new DecimalValue(value.multiply(((DecimalValue)other).value));
                    case Token.DIV:
                        int scale = Math.max(DIVIDE_PRECISION,
                                             Math.max(value.scale(), ((DecimalValue)other).value.scale()));
                        BigDecimal result = value.divide(((DecimalValue)other).value, scale, BigDecimal.ROUND_HALF_DOWN);
                        return new DecimalValue(result);
                    case Token.IDIV:
                        if (((DecimalValue)other).value.signum() == 0) {
                            DynamicError e = new DynamicError("Integer division by zero");
                            e.setErrorCode("FOAR0001");
                            e.setXPathContext(context);
                            throw e;
                        }
                        BigInteger quot = value.divide(((DecimalValue)other).value, 0, BigDecimal.ROUND_DOWN).toBigInteger();
                        return BigIntegerValue.makeIntegerValue(quot);
                    case Token.MOD:
                        BigDecimal quotient = value.divide(((DecimalValue)other).value, 0, BigDecimal.ROUND_DOWN);
                        BigDecimal remainder = value.subtract(quotient.multiply(((DecimalValue)other).value));
                        return new DecimalValue(remainder);
                    default:
                        throw new AssertionError("Unknown operator");
                }
            } catch (ArithmeticException err) {
                throw new DynamicError(err);
            }
        } else if (NumericValue.isInteger(other)) {
            return arithmetic(operator, (DecimalValue)other.convert(BuiltInAtomicType.DECIMAL, context), context);
        } else {
            final NumericValue n = (NumericValue)convert(other.getPrimitiveType(), context);
            return n.arithmetic(operator, other, context);
        }
    }

    /**
    * Compare the value to another numeric value
    */

    public int compareTo(Object other) {
        if ((NumericValue.isInteger((NumericValue)other))) {
            // deliberately triggers a ClassCastException if other value is the wrong type
            try {
                return compareTo(((NumericValue)other).convert(BuiltInAtomicType.DECIMAL, null));
            } catch (XPathException err) {
                throw new AssertionError("Conversion of integer to decimal should never fail");
            }
//        } else if (other instanceof BigIntegerValue) {
//            return value.compareTo(((BigIntegerValue)other).asDecimal());
        } else if (other instanceof DecimalValue) {
            return value.compareTo(((DecimalValue)other).value);
        } else if (other instanceof FloatValue) {
            try {
                return ((FloatValue)convert(BuiltInAtomicType.FLOAT, null)).compareTo(other);
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

    public Comparable getSchemaComparable() {
        return value;
    }

    /**
    * Convert to Java object (for passing to external functions)
    */

    public Object convertToJava(Class target, XPathContext context) throws XPathException {
        if (target==Object.class || target.isAssignableFrom(BigDecimal.class)) {
            return value;
        } else if (target.isAssignableFrom(DecimalValue.class)) {
            return this;
        } else if (target==boolean.class) {
            BooleanValue bval = (BooleanValue)convert(BuiltInAtomicType.BOOLEAN, context);
            return Boolean.valueOf(bval.getBooleanValue());
        } else if (target==Boolean.class) {
            BooleanValue bval = (BooleanValue)convert(BuiltInAtomicType.BOOLEAN, context);
            return Boolean.valueOf(bval.getBooleanValue());
        } else if (target==String.class || target==CharSequence.class) {
            return getStringValue();
        } else if (target==double.class || target==Double.class) {
            return new Double(value.doubleValue());
        } else if (target==float.class || target==Float.class) {
            return new Float(value.floatValue());
        } else if (target==long.class || target==Long.class) {
            return new Long(value.longValue());
        } else if (target==int.class || target==Integer.class) {
            return new Integer(value.intValue());
        } else if (target==short.class || target==Short.class) {
            return new Short(value.shortValue());
        } else if (target==byte.class || target==Byte.class) {
            return new Byte(value.byteValue());
        } else if (target==char.class || target==Character.class) {
            return new Character((char)value.intValue());
        } else {
            Object o = super.convertToJava(target, context);
            if (o == null) {
                throw new DynamicError("Conversion of decimal to " + target.getName() +
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

