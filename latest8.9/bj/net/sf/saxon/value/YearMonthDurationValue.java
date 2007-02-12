package net.sf.saxon.value;
import net.sf.saxon.expr.XPathContext;
import net.sf.saxon.om.FastStringBuffer;
import net.sf.saxon.trans.DynamicError;
import net.sf.saxon.trans.SaxonErrorCode;
import net.sf.saxon.trans.XPathException;
import net.sf.saxon.type.AtomicType;
import net.sf.saxon.type.BuiltInAtomicType;

import java.math.BigDecimal;
import java.util.StringTokenizer;

/**
* A value of type xs:yearMonthDuration
*/

public final class YearMonthDurationValue extends DurationValue implements Comparable {

    /**
    * Private constructor for internal use
    */

    private YearMonthDurationValue() {
        this.typeLabel = BuiltInAtomicType.YEAR_MONTH_DURATION;
    }

    /**
    * Constructor: create a duration value from a supplied string, in
    * ISO 8601 format [+|-]PnYnM
    */

    public YearMonthDurationValue(CharSequence s) throws XPathException {

        int components = 0;
        StringTokenizer tok = new StringTokenizer(Whitespace.trimWhitespace(s).toString(), "-+PYM", true);
        try {
            if (!tok.hasMoreElements()) badDuration("empty string", s);
            String part = (String)tok.nextElement();
            if ("+".equals(part)) {
                badDuration("+ sign not allowed in a duration", s);
            } else if ("-".equals(part)) {
                negative = true;
                part = (String)tok.nextElement();
            }
            if (!"P".equals(part)) badDuration("missing 'P'", s);
            int state = 0;
            while (tok.hasMoreElements()) {
                part = (String)tok.nextElement();
                int value = Integer.parseInt(part);
                if (!tok.hasMoreElements()) badDuration("missing unit letter at end", s);
                char delim = ((String)tok.nextElement()).charAt(0);
                switch (delim) {
                    case 'Y':
                        if (state > 0) badDuration("Y is out of sequence", s);
                        years = value;
                        components++;
                        state = 1;
                        break;
                    case 'M':
                        if (state == 0 || state==1) {
                            months = value;
                            components++;
                            state = 2;
                            break;
                        } else {
                            badDuration("M is out of sequence", s);
                        }

                   default:
                        badDuration("misplaced " + delim, s);
                }
            }
            if (components == 0) {
                badDuration("Duration specifies no components", s);
            }
            normalize();

        } catch (NumberFormatException err) {
            badDuration("non-numeric component", s);
        }
        this.typeLabel = BuiltInAtomicType.YEAR_MONTH_DURATION;
    }

    /**
     * Create a copy of this atomic value, with a different type label
     *
     * @param typeLabel the type label of the new copy. The caller is responsible for checking that
     *                  the value actually conforms to this type.
     */

    public AtomicValue copy(AtomicType typeLabel) {
        YearMonthDurationValue v = YearMonthDurationValue.fromMonths(getLengthInMonths());
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
        return BuiltInAtomicType.YEAR_MONTH_DURATION;
    }

    /**
    * Convert to string
    * @return ISO 8601 representation.
    */

    public CharSequence getStringValueCS() {

        // The canonical representation has months in the range 0-11

        int mm = years*12 + months;
        int y = mm / 12;
        int m = mm % 12;

        FastStringBuffer sb = new FastStringBuffer(32);
        if (negative) {
            sb.append('-');
        }
        sb.append('P');
        if (y!=0) {
            sb.append(y + "Y");
        }
        if (m!=0 || y==0) {
            sb.append(m + "M");
        }
        return sb;

    }

    /**
    * Normalize the value, for example 90M becomes 1H30M
    */

    public void normalize() {
        if (months >= 12) {
            years += (months / 12);
            months = months % 12;
        }
        normalizeZeroDuration();
        normalized = true;
    }

    /**
    * Get the number of months in the duration
    */

    public int getLengthInMonths() {
        return (years*12 + months) * (negative ? -1 : +1);
    }

    /**
    * Construct a duration value as a number of months.
    */

    public static YearMonthDurationValue fromMonths(int months) {
        YearMonthDurationValue mdv = new YearMonthDurationValue();
        mdv.negative = (months<0);
        mdv.months = (months<0 ? -months : months);
        mdv.normalize();
        return mdv;
    }

    /**
    * Multiply duration by a number. Also used when dividing a duration by a number
    */

    public DurationValue multiply(double n) throws XPathException {
        if (Double.isNaN(n)) {
            DynamicError err = new DynamicError("Cannot multiply/divide a duration by NaN");
            err.setErrorCode("FOCA0005");
            throw err;
        }
        double m = (double)getLengthInMonths();
        double product = n*m;
        if (Double.isInfinite(product) || product > Integer.MAX_VALUE || product < Integer.MIN_VALUE) {
            DynamicError err = new DynamicError("Overflow when multiplying/dividing a duration by a number");
            err.setErrorCode("FODT0002");
            throw err;
        }
        return fromMonths((int)Math.round(product));
    }

    /**
     * Find the ratio between two durations
     * @param other the dividend
     * @return the ratio, as a decimal
     * @throws XPathException
     */

    public DecimalValue divide(DurationValue other) throws XPathException {
        if (other instanceof YearMonthDurationValue) {
            BigDecimal v1 = BigDecimal.valueOf(this.getLengthInMonths());
            BigDecimal v2 = BigDecimal.valueOf(((YearMonthDurationValue)other).getLengthInMonths());
            if (v2.signum() == 0) {
                DynamicError err = new DynamicError("Divide by zero (durations)");
                err.setErrorCode("FOAR0001");
                throw err;
            }
            return new DecimalValue(v1.divide(v2, 20, BigDecimal.ROUND_HALF_EVEN));
        } else {
            DynamicError err = new DynamicError("Cannot divide two durations of different type");
            err.setErrorCode("XPTY0004");
            throw err;
        }
    }

    /**
    * Add two year-month-durations
    */

    public DurationValue add(DurationValue other) throws XPathException {
        if (other instanceof YearMonthDurationValue) {
            return fromMonths(this.getLengthInMonths() +
                    ((YearMonthDurationValue)other).getLengthInMonths());
        } else {
            DynamicError err = new DynamicError("Cannot add two durations of different type");
            err.setErrorCode("XPTY0004");
            throw err;
        }
    }

    /**
    * Subtract two year-month-durations
    */

    public DurationValue subtract(DurationValue other) throws XPathException {
        if (other instanceof YearMonthDurationValue) {
            return fromMonths(this.getLengthInMonths() -
                    ((YearMonthDurationValue)other).getLengthInMonths());
        } else {
            DynamicError err = new DynamicError("Cannot subtract two durations of different type");
            err.setErrorCode("XPTY0004");
            throw err;
        }
    }

    /**
     * Negate a duration (same as subtracting from zero, but it preserves the type of the original duration)
     */

    public DurationValue negate() {
        return fromMonths(-this.getLengthInMonths());
    }

    /**
    * Convert to Java object (for passing to external functions)
    */

    public Object convertToJava(Class target, XPathContext context) throws XPathException {
        if (target.isAssignableFrom(DurationValue.class)) {
            return this;
        } else if (target==String.class || target==CharSequence.class) {
            return getStringValue();
        } else if (target==Object.class) {
            return getStringValue();
        } else {
            DynamicError de = new DynamicError("Conversion of yearMonthDuration to " + target.getName() +
                        " is not supported");
            de.setErrorCode(SaxonErrorCode.SXJE0008);
            throw de;
        }
    }


    /**
    * Compare the value to another duration value
    * @param other The other dateTime value
    * @return negative value if this one is the earler, 0 if they are chronologically equal,
    * positive value if this one is the later. For this purpose, dateTime values with an unknown
    * timezone are considered to be UTC values (the Comparable interface requires
    * a total ordering).
    * @throws ClassCastException if the other value is not a DateTimeValue (the parameter
    * is declared as Object to satisfy the Comparable interface)
    */

    public int compareTo(Object other) {
        if (other instanceof YearMonthDurationValue) {
            return this.getLengthInMonths() - ((YearMonthDurationValue)other).getLengthInMonths();
        } else if (other instanceof DurationValue.DurationValueOrderingKey) {
            return DurationValue.getSchemaComparable(this).compareTo(other);
        } else {
            throw new ClassCastException("Cannot compare a yearMonthDuration to an object of class "
                    + other.getClass());
        }
    }

    /**
     * Get a Comparable value that implements the XML Schema ordering comparison semantics for this value.
     * Returns null if the value is not comparable according to XML Schema rules. This implementation
     * handles the ordering rules for durations in XML Schema, which for this subtype are identical to the
     * XPath rules.
     */

    public Comparable getSchemaComparable() {
        return this;
    }

    /**
     * Get a Comparable value that implements the XPath ordering comparison semantics for this value.
     * Returns null if the value is not comparable according to XPath rules. The default implementation
     * returns the value itself. This is modified for types such as
     * xs:duration which allow ordering comparisons in XML Schema, but not in XPath.
     */

    public Comparable getXPathComparable() {
        return this;
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
// Contributor(s): none.
//

