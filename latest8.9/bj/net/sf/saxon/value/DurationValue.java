package net.sf.saxon.value;
import net.sf.saxon.expr.XPathContext;
import net.sf.saxon.functions.Component;
import net.sf.saxon.om.FastStringBuffer;
import net.sf.saxon.trans.DynamicError;
import net.sf.saxon.trans.XPathException;
import net.sf.saxon.trans.SaxonErrorCode;
import net.sf.saxon.type.*;

import java.util.StringTokenizer;

/**
* A value of type xs:duration
*/

public class DurationValue extends AtomicValue {

    // TODO: the spec has moved away from treating a duration as a 6-tuple to treating it as (months + seconds)

    protected boolean negative = false;
    protected int years = 0;
    protected int months = 0;
    protected int days = 0;
    protected int hours = 0;
    protected int minutes = 0;
    protected int seconds = 0;
    protected int microseconds = 0;
    protected boolean normalized = false;

    /**
    * Private constructor for internal use
    */

    protected DurationValue() {
    }

    public DurationValue(boolean positive, int years, int months, int days,
                         int hours, int minutes, int seconds, int microseconds) {
        this(positive, years, months, days, hours, minutes, seconds, microseconds, BuiltInAtomicType.DURATION);
    }

     public DurationValue(boolean positive, int years, int months, int days,
                         int hours, int minutes, int seconds, int microseconds, AtomicType type) {
        this.negative = !positive;
        this.years = years;
        this.months = months;
        this.days = days;
        this.hours = hours;
        this.minutes = minutes;
        this.seconds = seconds;
        this.microseconds = microseconds;
        normalizeZeroDuration();
        normalized = (months<12 && hours<24 && minutes<60 && seconds<60 && microseconds<1000000);
        this.typeLabel = type;
    }

    protected void normalizeZeroDuration() {
        if (years==0 && months==0 && days==0 && hours==0 && minutes==0 && seconds==0 && microseconds==0) {
            // don't allow negative zero
            negative = false;
        }
    }

    /**
    * Constructor: create a duration value from a supplied string, in
    * ISO 8601 format [-]PnYnMnDTnHnMnS
    */

    public DurationValue(CharSequence s) throws XPathException {
        StringTokenizer tok = new StringTokenizer(Whitespace.trimWhitespace(s).toString(), "-+.PYMDTHS", true);
        int components = 0;
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
                if ("T".equals(part)) {
                    state = 4;
                    if (!tok.hasMoreElements()) {
                        badDuration("T must be followed by time components", s);
                    }
                    part = (String)tok.nextElement();
                }
                int value = Integer.parseInt(part);
                if (!tok.hasMoreElements()) badDuration("missing unit letter at end", s);
                char delim = ((String)tok.nextElement()).charAt(0);
                switch (delim) {
                    case 'Y':
                        if (state > 0) badDuration("Y is out of sequence", s);
                        years = value;
                        state = 1;
                        components++;
                        break;
                    case 'M':
                        if (state == 4 || state==5) {
                            minutes = value;
                            state = 6;
                            components++;
                            break;
                        } else if (state == 0 || state==1) {
                            months = value;
                            state = 2;
                            components++;
                            break;
                        } else {
                            badDuration("M is out of sequence", s);
                        }
                    case 'D':
                        if (state > 2) badDuration("D is out of sequence", s);
                        days = value;
                        state = 3;
                        components++;
                        break;
                    case 'H':
                        if (state != 4) badDuration("H is out of sequence", s);
                        hours = value;
                        state = 5;
                        components++;
                        break;
                    case '.':
                        if (state < 4 || state > 6) badDuration("misplaced decimal point", s);
                        seconds = value;
                        state = 7;
                        break;
                    case 'S':
                        if (state < 4 || state > 7) badDuration("S is out of sequence", s);
                        if (state==7) {
                            while (part.length() < 6) part += "0";
                            if (part.length() > 6) part = part.substring(0, 6);
                            microseconds = Integer.parseInt(part);
                        } else {
                            seconds = value;
                        }
                        state = 8;
                        components++;
                        break;
                   default:
                        badDuration("misplaced " + delim, s);
                }
            }

            if (components == 0) {
                badDuration("Duration specifies no components", s);
            }
            // Note, duration values (unlike the two xs: subtypes) are not normalized.
            // However, negative zero durations are normalized to positive

            normalizeZeroDuration();

        } catch (NumberFormatException err) {
            badDuration("non-numeric component", s);
        }
        this.typeLabel = BuiltInAtomicType.DURATION;
    }

    protected void badDuration(String msg, CharSequence s) throws XPathException {
        DynamicError err = new DynamicError("Invalid duration value '" + s + "' (" + msg + ')');
        err.setErrorCode("FORG0001");
        throw err;
    }

    /**
     * Create a copy of this atomic value, with a different type label
     *
     * @param typeLabel the type label of the new copy. The caller is responsible for checking that
     *                  the value actually conforms to this type.
     */

    public AtomicValue copy(AtomicType typeLabel) {
        DurationValue v = new DurationValue(!negative, years, months, days, hours, minutes, seconds, microseconds);
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
        return BuiltInAtomicType.DURATION;
    }

    /**
    * Convert to target data type
     * @param requiredType an integer identifying the required atomic type
     * @param validate if set to false, the caller asserts that the value is known to be valid
     * @param context
     * @return an AtomicValue, a value of the required type; or a {@link ValidationErrorValue} if
     * the value cannot be converted.
    */

    public AtomicValue convertPrimitive(BuiltInAtomicType requiredType, boolean validate, XPathContext context) {
        //System.err.println("Convert duration " + getClass() + " to " + Type.getTypeName(requiredType));
        switch(requiredType.getPrimitiveType()) {
        case Type.DURATION:
        case Type.ANY_ATOMIC:
        case Type.ITEM:
            return this;
        case Type.STRING:
            return new StringValue(getStringValueCS());
        case Type.UNTYPED_ATOMIC:
            return new UntypedAtomicValue(getStringValueCS());
        case Type.YEAR_MONTH_DURATION:
            return YearMonthDurationValue.fromMonths((years*12 + months) * (negative ? -1 : +1));
        case Type.DAY_TIME_DURATION:
            try {
                return new DayTimeDurationValue((negative?-1:+1), days, hours, minutes, seconds, microseconds);
            } catch (ValidationException err) {
                return new ValidationErrorValue(err);
            }
        default:
            ValidationException err = new ValidationException("Cannot convert duration to " +
                                     requiredType.getDisplayName());
            err.setErrorCode("XPTY0004");
            err.setIsTypeError(true);
            return new ValidationErrorValue(err);
        }
    }

    /**
     * Normalize the duration, so that months<12, hours<24, minutes<60, seconds<60.
     * At present we do this when converting to a string. It's possible that it should be done immediately
     * on constructing the duration (so that component extraction functions get the normalized value).
     * We're awaiting clarification of the spec (bugzilla 3369)
     * @return a new, normalized duration
     */

    public DurationValue normalizeDuration() {
        int totalMonths = years*12 + months;
        int years = totalMonths / 12;
        int months = totalMonths % 12;
        long totalMicroSeconds = ((((((days*24L + hours)*60L)+minutes)*60L)+seconds)*1000000L)+microseconds;
        int microseconds = (int)(totalMicroSeconds % 1000000L);
        int totalSeconds = (int)(totalMicroSeconds / 1000000L);
        int seconds = totalSeconds % 60;
        int totalMinutes = totalSeconds / 60;
        int minutes = totalMinutes % 60;
        int totalHours = totalMinutes / 60;
        int hours = totalHours % 24;
        int days = totalHours / 24;
        return new DurationValue(!negative, years, months, days, hours, minutes, seconds, microseconds);

    }

    /**
     * Return the signum of the value
     * @return -1 if the duration is negative, zero if it is zero-length, +1 if it is positive
     */

    public int signum() {
        if (negative) {
            return -1;
        }
        if (years == 0 && months ==0 && days == 0 && hours == 0 && minutes == 0 && seconds == 0 && microseconds == 0) {
            return 0;
        }
        return +1;
    }

    /**
     * Get the year component
     * @return the number of years in the normalized duration; always positive
     */

    public int getYears() {
        return years;
    }

    /**
     * Get the months component
     * @return the number of months in the normalized duration; always positive
     */

    public int getMonths() {
        return months;
    }

    /**
     * Get the days component
     * @return the number of days in the normalized duration; always positive
     */

    public int getDays() {
        return days;
    }

    /**
     * Get the hours component
     * @return the number of hours in the normalized duration; always positive
     */

    public int getHours() {
        return hours;
    }

    /**
     * Get the minutes component
     * @return the number of minutes in the normalized duration; always positive
     */

    public int getMinutes() {
        return minutes;
    }

    /**
     * Get the seconds component
     * @return the number of whole seconds in the normalized duration; always positive
     */

    public int getSeconds() {
        return seconds;
    }

    /**
     * Get the microseconds component
     * @return the number of microseconds in the normalized duration; always positive
     */

    public int getMicroseconds() {
        return microseconds;
    }


    /**
     * Convert the value to a string, using the serialization rules.
     * For atomic values this is the same as a cast; for sequence values
     * it gives a space-separated list. This method is refined for AtomicValues
     * so that it never throws an Exception.
     */

    public String getStringValue() {
        return getStringValueCS().toString();
    }

    /**
    * Convert to string
    * @return ISO 8601 representation.
    */

    public CharSequence getStringValueCS() {

        // Note, Schema does not define a canonical representation. We omit all zero components, unless
        // the duration is zero-length, in which case we output PT0S.

        if (years==0 && months==0 && days==0 && hours==0 && minutes==0 && seconds==0 && microseconds==0) {
            return "PT0S";
        }

        if (!normalized) {
            return normalizeDuration().getStringValueCS();
        }

        FastStringBuffer sb = new FastStringBuffer(32);
        if (negative) {
            sb.append('-');
        }
        sb.append("P");
        if (years != 0) {
            sb.append(years + "Y");
        }
        if (months != 0) {
            sb.append(months + "M");
        }
        if (days != 0) {
            sb.append(days + "D");
        }
        if (hours != 0 || minutes != 0 || seconds != 0 || microseconds != 0) {
            sb.append("T");
        }
        if (hours != 0) {
            sb.append(hours + "H");
        }
        if (minutes != 0) {
            sb.append(minutes + "M");
        }
        if (seconds != 0 || microseconds != 0) {
            if (seconds != 0 && microseconds == 0) {
                sb.append(seconds + "S");
            } else {
                long ms = (seconds * 1000000) + microseconds;
                String mss = ms + "";
                if (seconds == 0) {
                    mss = "0000000" + mss;
                    mss = mss.substring(mss.length()-7);
                }
                sb.append(mss.substring(0, mss.length()-6));
                sb.append('.');
                int lastSigDigit = mss.length()-1;
                while (mss.charAt(lastSigDigit) == '0') {
                    lastSigDigit--;
                }
                sb.append(mss.substring(mss.length()-6, lastSigDigit+1));
                sb.append('S');
            }
        }

        return sb;

    }

    /**
    * Get length of duration in seconds, assuming an average length of month. (Note, this defines a total
    * ordering on durations which is different from the partial order defined in XML Schema; XPath 2.0
    * currently avoids defining an ordering at all. But the ordering here is consistent with the ordering
    * of the two duration subtypes in XPath 2.0.)
    */

    public double getLengthInSeconds() {
        double a = years;
        a = a*12 + months;
        a = a*(365.242199/12.0) + days;
        a = a*24 + hours;
        a = a*60 + minutes;
        a = a*60 + seconds;
        a = a + ((double)microseconds / 1000000);
        return (negative ? -a : a);
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
            Object o = super.convertToJava(target, context);
            if (o == null) {
                DynamicError err = new DynamicError("Conversion of xs:duration to " + target.getName() +
                        " is not supported");
                err.setXPathContext(context);
                err.setErrorCode(SaxonErrorCode.SXJE0003);
            }
            return o;
        }
    }

    /**
    * Get a component of the normalized value
    */

    public AtomicValue getComponent(int component) throws XPathException {
        // TODO: there's no longer any need to maintain unnormalized values
        if (!normalized) {
            return normalizeDuration().getComponent(component);
        }
        switch (component) {
        case Component.YEAR:
            return Int64Value.makeIntegerValue((negative?-years:years));
        case Component.MONTH:
            return Int64Value.makeIntegerValue((negative?-months:months));
        case Component.DAY:
            return Int64Value.makeIntegerValue((negative?-days:days));
        case Component.HOURS:
            return Int64Value.makeIntegerValue((negative?-hours:hours));
        case Component.MINUTES:
            return Int64Value.makeIntegerValue((negative?-minutes:minutes));
        case Component.SECONDS:
            FastStringBuffer sb = new FastStringBuffer(16);
            String ms = ("000000" + microseconds);
            ms = ms.substring(ms.length()-6);
            sb.append((negative?"-":"") + seconds + '.' + ms);
            return DecimalValue.makeDecimalValue(sb, false);
        case Component.WHOLE_SECONDS:
            return Int64Value.makeIntegerValue((negative?-seconds:seconds));
        case Component.MICROSECONDS:
            return new Int64Value((negative?-microseconds:microseconds));
        default:
            throw new IllegalArgumentException("Unknown component for duration: " + component);
        }
    }


    /**
    * Test if the two durations are of equal length.
     * @throws ClassCastException if the other value is not an xs:duration or subtype thereof
    */

    public boolean equals(Object other) {
        DurationValue val = (DurationValue)other;

        DurationValue d1 = normalizeDuration();
        DurationValue d2 = val.normalizeDuration();
        return d1.negative == d2.negative &&
                d1.years == d2.years &&
                d1.months == d2.months &&
                d1.days == d2.days &&
                d1.hours == d2.hours &&
                d1.minutes == d2.minutes &&
                d1.seconds == d2.seconds &&
                d1.microseconds == d2.microseconds;
    }

    public int hashCode() {
        return new Double(getLengthInSeconds()).hashCode();
    }

    /**
    * Add two durations
    */

    public DurationValue add(DurationValue other) throws XPathException {
        DynamicError err = new DynamicError("Only subtypes of xs:duration can be added");
        err.setErrorCode("XPTY0004");
        err.setIsTypeError(true);
        throw err;
    }

    /**
    * Subtract two durations
    */

    public DurationValue subtract(DurationValue other) throws XPathException{
        DynamicError err = new DynamicError("Only subtypes of xs:duration can be subtracted");
        err.setErrorCode("XPTY0004");
        err.setIsTypeError(true);
        throw err;
    }

    /**
     * Negate a duration (same as subtracting from zero, but it preserves the type of the original duration)
     */

    public DurationValue negate() {
        return new DurationValue(negative, years, months, days, hours, minutes, seconds, microseconds);
    }

    /**
    * Multiply a duration by a number
    */

    public DurationValue multiply(double factor) throws XPathException {
        DynamicError err = new DynamicError("Only subtypes of xs:duration can be multiplied by a number");
        err.setErrorCode("XPTY0004");
        err.setIsTypeError(true);
        throw err;
    }

    /**
    * Divide a duration by a another duration
    */

    public DecimalValue divide(DurationValue other) throws XPathException {
        DynamicError err = new DynamicError("Only subtypes of xs:duration can be divided by another duration");
        err.setErrorCode("XPTY0004");
        err.setIsTypeError(true);
        throw err;
    }

    /**
     * Get a Comparable value that implements the XPath ordering comparison semantics for this value.
     * Returns null if the value is not comparable according to XPath rules. The default implementation
     * returns the value itself. This is modified for types such as
     * xs:duration which allow ordering comparisons in XML Schema, but not in XPath.
     */

    public Comparable getXPathComparable() {
        return null;
    }

    /**
     * Get a Comparable value that implements the XML Schema ordering comparison semantics for this value.
     * This implementation handles the ordering rules for durations in XML Schema.
     * It is overridden for the two subtypes DayTimeDuration and YearMonthDuration.
     */

    public Comparable getSchemaComparable() {
        return getSchemaComparable(this);
    }

    /**
     * Get a Comparable value that implements the XML Schema ordering comparison semantics for this value.
     * This implementation handles the ordering rules for durations in XML Schema.
     */

    public static Comparable getSchemaComparable(DurationValue value) {
        int m = 12*value.years + value.months;
        double s = value.days;
        s = s*24 + value.hours;
        s = s*60 + value.minutes;
        s = s*60 + value.seconds;
        s = s + ((double)value.microseconds / 1000000);
        if (value.negative) {
            s = -s;
            m = -m;
        }
        return new DurationValueOrderingKey(m, s);
    }

    /**
     * DurationValueOrderingKey is a Comparable value that acts as a surrogate for a Duration,
     * having ordering rules that implement the XML Schema specification.
     */

    static class DurationValueOrderingKey implements Comparable {

        private int months;
        private double seconds;

        public DurationValueOrderingKey(int m, double s) {
            months = m;
            seconds = s;
        }

        /**
         * Compare two durations according to the XML Schema rules.
         * @param o the other duration
         * @return -1 if this duration is smaller; 0 if they are equal; +1 if this duration is greater;
         * {@link AtomicValue.INDETERMINATE_ORDERING} if there is no defined order
         */

        public int compareTo(Object o) {
            DurationValueOrderingKey other;
            if (o instanceof DurationValueOrderingKey) {
                other = (DurationValueOrderingKey)o;
            } else if (o instanceof YearMonthDurationValue) {
                other = (DurationValueOrderingKey) getSchemaComparable((YearMonthDurationValue)o);
            } else if (o instanceof DayTimeDurationValue) {
                other = (DurationValueOrderingKey) getSchemaComparable((DayTimeDurationValue)o);
            } else {
                throw new ClassCastException("Non-comparable values");
            }
            if (months == other.months) {
                return Double.compare(seconds, other.seconds);
            } else if (seconds == other.seconds) {
                return (months == other.months ? 0 : (months < other.months ? -1 : +1));
            } else {
                double oneDay = 24e0 * 60e0 * 60e0;
                double min0 = monthsToDaysMinimum(months) * oneDay + seconds;
                double max0 = monthsToDaysMaximum(months) * oneDay + seconds;
                double min1 = monthsToDaysMinimum(other.months) * oneDay + other.seconds;
                double max1 = monthsToDaysMaximum(other.months) * oneDay + other.seconds;
                if (max0 < min1) {
                    return -1;
                } else if (min0 > max1) {
                    return +1;
                } else {
                    return AtomicValue.INDETERMINATE_ORDERING;
                }
            }
        }

        private int monthsToDaysMinimum(int months) {
            if (months < 0) {
                return -monthsToDaysMaximum(-months);
            }
            if (months < 12) {
                int[] shortest = {0, 28, 59, 89, 120, 150, 181, 212, 242, 273, 303, 334};
                return shortest[months];
            } else {
                int years = months / 12;
                int remainingMonths = months % 12;
                // the -1 is to allow for the fact that we might miss a leap day if we time the start badly
                int yearDays = years*365 + (years%4) - (years%100) + (years%400) - 1;
                return yearDays + monthsToDaysMinimum(remainingMonths);
            }
        }

        private int monthsToDaysMaximum(int months) {
            if (months < 0) {
                return -monthsToDaysMinimum(-months);
            }
            if (months < 12) {
                int[] longest = {0, 31, 62, 92, 123, 153, 184, 215, 245, 276, 306, 337};
                return longest[months];
            } else {
                int years = months / 12;
                int remainingMonths = months % 12;
                // the +1 is to allow for the fact that we might miss a leap day if we time the start badly
                int yearDays = years*365 + (years%4) - (years%100) + (years%400) + 1;
                return yearDays + monthsToDaysMaximum(remainingMonths);
            }
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
// Contributor(s): none.
//

