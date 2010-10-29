package net.sf.saxon.value;
import net.sf.saxon.lib.ConversionRules;
import net.sf.saxon.om.StandardNames;
import net.sf.saxon.trans.Err;
import net.sf.saxon.trans.XPathException;
import net.sf.saxon.type.*;

import java.math.BigDecimal;
import java.util.regex.Pattern;


/**
* A precision decimal value (new data type defined in XSD 1.1)
*/

public final class PrecisionDecimalValue extends NumericValue {

    public static final int DIVIDE_PRECISION = 18;

    private static final int ORDINARY_VALUE = 0;
    private static final int NEGATIVE_ZERO_DATAPOINT = 1;
    private static final int NEGATIVE_INFINITY_DATAPOINT = 2;
    private static final int POSITIVE_INFINITY_DATAPOINT = 3;
    private static final int NAN_DATAPOINT = 4;
    
    private int dataPoint;
    private BigDecimal value;


    public static final PrecisionDecimalValue ZERO =
            new PrecisionDecimalValue(BigDecimal.ZERO);
    public static final PrecisionDecimalValue ONE =
            new PrecisionDecimalValue(BigDecimal.ONE);
    public static final PrecisionDecimalValue POSITIVE_INFINITY =
            new PrecisionDecimalValue(BigDecimal.ZERO, POSITIVE_INFINITY_DATAPOINT);
    public static final PrecisionDecimalValue NEGATIVE_INFINITY =
            new PrecisionDecimalValue(BigDecimal.ZERO, NEGATIVE_INFINITY_DATAPOINT);
    public static final PrecisionDecimalValue NEGATIVE_ZERO =
            new PrecisionDecimalValue(BigDecimal.ZERO, NEGATIVE_ZERO_DATAPOINT);
    public static final PrecisionDecimalValue NaN =
            new PrecisionDecimalValue(BigDecimal.ZERO, NAN_DATAPOINT);

    /**
    * Default Constructor for internal use only
    */

    private PrecisionDecimalValue() {
        this.dataPoint = ORDINARY_VALUE;
        this.value = BigDecimal.ZERO;
        this.typeLabel = BuiltInAtomicType.PRECISION_DECIMAL;
    }

    /**
    * Constructor supplying a BigDecimal
    * @param value the value of the DecimalValue
    */

    public PrecisionDecimalValue(BigDecimal value) {
        this.dataPoint = ORDINARY_VALUE;
        this.value = value;
        this.typeLabel = BuiltInAtomicType.PRECISION_DECIMAL;
    }

    /**
    * Constructor for special values
    * @param value the conventional decimal value
    * @param dataPoint the status flag
    */

    private PrecisionDecimalValue(BigDecimal value, int dataPoint) {
        this.dataPoint = dataPoint;
        this.value = value;
        this.typeLabel = BuiltInAtomicType.PRECISION_DECIMAL;
    }

    private static final Pattern precisionDecimalPattern =
            Pattern.compile("[-+]?([0-9]+(\\.[0-9]*)?|\\.[0-9]+)([Ee][-+]?[0-9]+)?|[-+]?INF|NaN");

    /**
     * Constructor supplying a double
     * @param in the value of the DecimalValue
     */

    public PrecisionDecimalValue(double in) {
        if (Double.isNaN(in)) {
            value = BigDecimal.ZERO;
            dataPoint = NAN_DATAPOINT;
        } else if (Double.isInfinite(in)) {
            value = BigDecimal.ZERO;
            if (in > 0) {
                dataPoint = POSITIVE_INFINITY_DATAPOINT;
            } else {
                dataPoint = NEGATIVE_INFINITY_DATAPOINT;
            }
        } else if (in == 0.0 && Double.doubleToRawLongBits(in) == Double.doubleToRawLongBits(-0.0)) {
            // double value is negative zero
            value = BigDecimal.ZERO;
            dataPoint = NEGATIVE_ZERO_DATAPOINT;
        } else {
            dataPoint = ORDINARY_VALUE;
            BigDecimal d = new BigDecimal(in);
            value = d.stripTrailingZeros();
        }
        typeLabel = BuiltInAtomicType.PRECISION_DECIMAL;
    }

    /**
     * Factory method to construct a DecimalValue from a string
     * @param in the value of the DecimalValue
     * @param validate true if validation is required; false if the caller knows that the value is valid
     * @return the required DecimalValue if the input is valid, or a ValidationFailure encapsulating the error             V
     * message if not.
     */

    public static ConversionResult makePrecisionDecimalValue(CharSequence in, boolean validate) {
        String s = Whitespace.collapseWhitespace(in).toString();
        if (s.equals("INF")) {
            return POSITIVE_INFINITY;
        } else if (s.equals("+INF")) {
            return POSITIVE_INFINITY;
        } else if (s.equals("-INF")) {
            return NEGATIVE_INFINITY;
        } else if (s.equals("NaN")) {
            return NaN;
        } else {
            try {
                BigDecimal v = new BigDecimal(s);
                int dataPoint = ORDINARY_VALUE;
                if (v.compareTo(BigDecimal.ZERO) == 0 && s.charAt(0) == '-') {
                    dataPoint = NEGATIVE_ZERO_DATAPOINT;
                }
                return new PrecisionDecimalValue(v, dataPoint);
            } catch (NumberFormatException err) {
                ValidationFailure e = new ValidationFailure(
                        "Cannot convert string " + Err.wrap(Whitespace.trim(in), Err.VALUE) +
                                " to xs:precisionDecimal: " + err.getMessage());
                e.setErrorCode("FORG0001");
                return e;
            }
        }
    }

    /**
     * Test whether a string is castable to a precision decimal value
     * @param in the string to be tested
     * @return true if the string has the correct format for a decimal
     */

    public static boolean castableAsPrecisionDecimal(CharSequence in) {
        CharSequence trimmed = Whitespace.trimWhitespace(in);
        return precisionDecimalPattern.matcher(trimmed).matches();
    }

    /**
    * Constructor supplying a long integer
    * @param in the value of the DecimalValue
    */

    public PrecisionDecimalValue(long in) {
        value = BigDecimal.valueOf(in);
        dataPoint = ORDINARY_VALUE;
        typeLabel = BuiltInAtomicType.DECIMAL;
    }

    /**
     * Create a copy of this atomic value, with a different type label
     *
     * @param typeLabel the type label of the new copy. The caller is responsible for checking that
     *                  the value actually conforms to this type.
     */

    public AtomicValue copyAsSubType(AtomicType typeLabel) {
        PrecisionDecimalValue v = new PrecisionDecimalValue();
        v.value = value;
        v.dataPoint = dataPoint;
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
        return BuiltInAtomicType.PRECISION_DECIMAL;
    }


    /**
    * Get the value
    */

    public BigDecimal getDecimalValue() {
        return value;
    }

    /**
      * Test whether the value is the special value NaN
      * @return true if the value is NaN; otherwise false
      */

     public boolean isNaN() {
         return dataPoint == NAN_DATAPOINT;
     }

    /**
     * Test whether the value is positive or negative infinity
     */

    public boolean isInfinite() {
        return dataPoint == POSITIVE_INFINITY_DATAPOINT || dataPoint == NEGATIVE_INFINITY_DATAPOINT;
    }

    /**
     * Get the hashCode. This must conform to the rules for other NumericValue hashcodes
     * @see net.sf.saxon.value.NumericValue#hashCode
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

    public ConversionResult convertPrimitive(BuiltInAtomicType requiredType, boolean validate, ConversionRules rules) {
        switch(requiredType.getFingerprint()) {
        case StandardNames.XS_BOOLEAN:
                // 0.0 => false, anything else => true
            return BooleanValue.get(value.signum()!=0);
        case StandardNames.XS_NUMERIC:
        case StandardNames.XS_PRECISION_DECIMAL:
        case StandardNames.XS_ANY_ATOMIC_TYPE:
            return this;
        case StandardNames.XS_DECIMAL:
            if (dataPoint == ORDINARY_VALUE || dataPoint == NEGATIVE_ZERO_DATAPOINT) {
                return new DecimalValue(value);
            } else {
                ValidationFailure err = new ValidationFailure("Cannot convert precision decimal value " +
                        toString() + " to " + requiredType.getDisplayName());
                err.setErrorCode("FORG0001");
                return err;
            }
        case StandardNames.XS_INTEGER:
            if (dataPoint == ORDINARY_VALUE || dataPoint == NEGATIVE_ZERO_DATAPOINT) {
                return BigIntegerValue.makeIntegerValue(value.toBigInteger());
            } else {
                ValidationFailure err = new ValidationFailure("Cannot convert precision decimal value " +
                        toString() + " to " + requiredType.getDisplayName());
                err.setErrorCode("FORG0001");
                return err;
            }
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
            return iv.convertPrimitive(requiredType, validate, rules);
        case StandardNames.XS_DOUBLE:
            if (dataPoint == POSITIVE_INFINITY_DATAPOINT) {
                return new DoubleValue(Double.POSITIVE_INFINITY);
            } else if (dataPoint == NEGATIVE_INFINITY_DATAPOINT) {
                return new DoubleValue(Double.NEGATIVE_INFINITY);
            } else if (dataPoint == NAN_DATAPOINT) {
                return new DoubleValue(Double.NaN);
            } else {
                return new DoubleValue(value.doubleValue());
            }
        case StandardNames.XS_FLOAT:
            if (dataPoint == POSITIVE_INFINITY_DATAPOINT) {
                return new FloatValue(Float.POSITIVE_INFINITY);
            } else if (dataPoint == NEGATIVE_INFINITY_DATAPOINT) {
                return new FloatValue(Float.NEGATIVE_INFINITY);
            } else if (dataPoint == NAN_DATAPOINT) {
                return new FloatValue(Float.NaN);
            } else {
                return new FloatValue(value.floatValue());
            }
        case StandardNames.XS_STRING:
            return new StringValue(getStringValueCS());
        case StandardNames.XS_UNTYPED_ATOMIC:
            return new UntypedAtomicValue(getStringValueCS());
        default:
            ValidationFailure err = new ValidationFailure("Cannot convert precision decimal to " +
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
        switch (dataPoint) {
            case ORDINARY_VALUE:
                return value.toString();
                // TODO: check that this matches the canonical lexical representation
            case NEGATIVE_ZERO_DATAPOINT:
                return "-" + value.toString();
            case POSITIVE_INFINITY_DATAPOINT:
                return "INF";
            case NEGATIVE_INFINITY_DATAPOINT:
                return "-INF";
            case NAN_DATAPOINT:
            default:
                return "NaN";
        }
    }

    /**
     * Check the value against the totalDigits facet
     * @param requiredValue the value of the constraining facet
     * @return true if the value conforms, false if it does not
     */

    public boolean checkTotalDigits(int requiredValue) {
        switch (dataPoint) {
            case ORDINARY_VALUE:
            case NEGATIVE_ZERO_DATAPOINT:
                return equals(DecimalValue.ZERO) ||
                        value.precision() <= requiredValue;
            case POSITIVE_INFINITY_DATAPOINT:
            case NEGATIVE_INFINITY_DATAPOINT:
            case NAN_DATAPOINT:
            default:
                return true;
        }
    }

    /**
     * Check the value against the minScale facet
     * @param requiredValue the value of the constraining facet
     * @return true if the value conforms, false if it does not
     */


    public boolean checkMinScale(int requiredValue) {
        switch (dataPoint) {
            case ORDINARY_VALUE:
            case NEGATIVE_ZERO_DATAPOINT:
                return value.scale() >= requiredValue;
            case POSITIVE_INFINITY_DATAPOINT:
            case NEGATIVE_INFINITY_DATAPOINT:
            case NAN_DATAPOINT:
            default:
                return true;
        }
    }

    /**
     * Check the value against the maxScale facet
     * @param requiredValue the value of the constraining facet
     * @return true if the value conforms, false if it does not
     */


    public boolean checkMaxScale(int requiredValue) {
        switch (dataPoint) {
            case ORDINARY_VALUE:
            case NEGATIVE_ZERO_DATAPOINT:
                return value.scale() <= requiredValue;
            case POSITIVE_INFINITY_DATAPOINT:
            case NEGATIVE_INFINITY_DATAPOINT:
            case NAN_DATAPOINT:
            default:
                return true;
        }
    }



    /**
    * Negate the value
    */

    public NumericValue negate() {
        switch (dataPoint) {
            case ORDINARY_VALUE:
                return new PrecisionDecimalValue(value.negate());
            case NEGATIVE_ZERO_DATAPOINT:
                return PrecisionDecimalValue.ZERO;
            case POSITIVE_INFINITY_DATAPOINT:
                return PrecisionDecimalValue.NEGATIVE_INFINITY;
            case NEGATIVE_INFINITY_DATAPOINT:
                return PrecisionDecimalValue.POSITIVE_INFINITY;
            case NAN_DATAPOINT:
            default:
                return this;
        }
    }

    /**
    * Implement the XPath floor() function
    */

    public NumericValue floor() {
        if (dataPoint != ORDINARY_VALUE) {
            return this;
        }
        return new PrecisionDecimalValue(value.setScale(0, BigDecimal.ROUND_FLOOR));
    }

    /**
    * Implement the XPath ceiling() function
    */

    public NumericValue ceiling() {
        if (dataPoint != ORDINARY_VALUE) {
            return this;
        }
        return new PrecisionDecimalValue(value.setScale(0, BigDecimal.ROUND_CEILING));
    }

    /**
    * Implement the XPath round() function
    */

    public NumericValue round(int scale) {

        if (dataPoint != ORDINARY_VALUE) {
            return this;
        }
        // The XPath rules say that we should round to the nearest integer, with .5 rounding towards
        // positive infinity. Unfortunately this is not one of the rounding modes that the Java BigDecimal
        // class supports, so we need different rules depending on the value.

        // If the value is positive, we use ROUND_HALF_UP; if it is negative, we use ROUND_HALF_DOWN (here "UP"
        // means "away from zero")

        switch (value.signum()) {
            case -1:
                return new PrecisionDecimalValue(value.setScale(0, BigDecimal.ROUND_HALF_DOWN));
            case 0:
                return this;
            case +1:
                return new PrecisionDecimalValue(value.setScale(0, BigDecimal.ROUND_HALF_UP));
            default:
                // can't happen
                return this;
        }

    }

    /**
    * Implement the XPath round-half-to-even() function
    */

    public NumericValue roundHalfToEven(int scale) {
        BigDecimal scaledValue = value.setScale(scale, BigDecimal.ROUND_HALF_EVEN);
        return new DecimalValue(scaledValue);
    }

    /**
     * Determine whether the value is negative, zero, or positive
     * @return -1 if negative, 0 if zero, +1 if positive, NaN if NaN
     */

    public double signum() {
        switch (dataPoint) {
            case ORDINARY_VALUE:
                return value.signum();
            case NEGATIVE_ZERO_DATAPOINT:
                return 0.0;
            case POSITIVE_INFINITY_DATAPOINT:
                return 1;
            case NEGATIVE_INFINITY_DATAPOINT:
                return -1;
            case NAN_DATAPOINT:
            default:
                return Double.NaN;
        }
    }

    /**
    * Determine whether the value is a whole number, that is, whether it compares
    * equal to some integer
    */

    public boolean isWholeNumber() {
        switch (dataPoint) {
            case ORDINARY_VALUE:
                return value.scale()==0 ||
                        value.compareTo(value.setScale(0, BigDecimal.ROUND_DOWN)) == 0;
            case NEGATIVE_ZERO_DATAPOINT:
                return true;
            case POSITIVE_INFINITY_DATAPOINT:
                return false;
            case NEGATIVE_INFINITY_DATAPOINT:
                return false;
            case NAN_DATAPOINT:
            default:
                return false;
        }
    }

    /**
     * Get the absolute value as defined by the XPath abs() function
     * @return the absolute value
     * @since 9.2
     */

    public NumericValue abs() {
        switch (dataPoint) {
            case ORDINARY_VALUE:
                if (value.signum() > 0) {
                    return this;
                } else {
                    return negate();
                }
            case NEGATIVE_ZERO_DATAPOINT:
                return ZERO;
            case POSITIVE_INFINITY_DATAPOINT:
                return this;
            case NEGATIVE_INFINITY_DATAPOINT:
                return POSITIVE_INFINITY;
            case NAN_DATAPOINT:
            default:
                return this;
        }

    }

    /**
    * Compare the value to another numeric value
    * @return -1 if this one is the lower, 0 if they are numerically equal,
     *     +1 if this one is the higher, or if either value is NaN. Where NaN values are
     *     involved, they should be handled by the caller before invoking this method.
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
            return value.compareTo(((DecimalValue)other).getDecimalValue());
        } else if (other instanceof PrecisionDecimalValue) {
            int sc = getSchemaComparable().compareTo(((PrecisionDecimalValue)other).getSchemaComparable());
            if (sc == Value.INDETERMINATE_ORDERING) {
                return +1;
            } else {
                return sc;
            }
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
     * semantics of the compareTo() method by returning the value {@link net.sf.saxon.value.Value#INDETERMINATE_ORDERING} when there
     * is no defined order relationship between two given values.</p>
     */

    /**
     * Get an object that implements XML Schema comparison semantics
     */

    public Comparable getSchemaComparable() {
        return new PrecisionDecimalComparable(this);
    }

    protected static class PrecisionDecimalComparable implements Comparable {

        protected PrecisionDecimalValue pdv;

        public PrecisionDecimalComparable(PrecisionDecimalValue value) {
            this.pdv = value;
        }

        public BigDecimal asBigDecimal() {
            return pdv.getDecimalValue();
        }

        public int compareTo(Object o) {
            if (o instanceof PrecisionDecimalComparable) {
                PrecisionDecimalValue pdv2 = ((PrecisionDecimalComparable)o).pdv;
                switch (pdv.dataPoint) {
                    case ORDINARY_VALUE:
                    case NEGATIVE_ZERO_DATAPOINT:
                        switch (pdv2.dataPoint) {
                            case ORDINARY_VALUE:
                            case NEGATIVE_ZERO_DATAPOINT:
                                return asBigDecimal().compareTo(pdv2.value);
                            case POSITIVE_INFINITY_DATAPOINT:
                                return -1;
                            case NEGATIVE_INFINITY_DATAPOINT:
                                return +1;
                            case NAN_DATAPOINT:
                            default:
                                return INDETERMINATE_ORDERING;
                        }
                    case POSITIVE_INFINITY_DATAPOINT:
                        switch (pdv2.dataPoint) {
                            case ORDINARY_VALUE:
                            case NEGATIVE_ZERO_DATAPOINT:
                            case NEGATIVE_INFINITY_DATAPOINT:
                                return +1;
                            case POSITIVE_INFINITY_DATAPOINT:
                                return 0;
                            case NAN_DATAPOINT:
                            default:
                                return INDETERMINATE_ORDERING;
                        }
                    case NEGATIVE_INFINITY_DATAPOINT:
                        switch (pdv2.dataPoint) {
                            case ORDINARY_VALUE:
                            case NEGATIVE_ZERO_DATAPOINT:
                            case POSITIVE_INFINITY_DATAPOINT:
                                return -1;
                            case NEGATIVE_INFINITY_DATAPOINT:
                                return 0;
                            case NAN_DATAPOINT:
                            default:
                                return INDETERMINATE_ORDERING;
                        }
                    case NAN_DATAPOINT:
                    default:
                        return INDETERMINATE_ORDERING;
                }
            } else if (o instanceof Int64Value.Int64Comparable) {
                return asBigDecimal().compareTo(BigDecimal.valueOf(((Int64Value.Int64Comparable)o).asLong()));
            } else if (o instanceof BigIntegerValue.BigIntegerComparable) {
                return pdv.compareTo(new BigDecimal(((BigIntegerValue.BigIntegerComparable)o).asBigInteger()));
            } else {
                return INDETERMINATE_ORDERING;
            }
        }

        public boolean equals(Object o) {
            return compareTo(o) == 0;
        }

        public int hashCode() {
            // Must align with hashCodes for other subtypes of xs:decimal
            if (pdv.isWholeNumber()) {
                try {
                    return pdv.convertPrimitive(BuiltInAtomicType.INTEGER, true, null).asAtomic()
                            .getSchemaComparable().hashCode();
                } catch (ValidationException e) {
                    return 12345678;    // can't happen
                }
            }
            return pdv.hashCode();
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
        return (v instanceof PrecisionDecimalValue) && equals(v);
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
