package net.sf.saxon.value;
import net.sf.saxon.expr.XPathContext;
import net.sf.saxon.functions.Component;
import net.sf.saxon.om.FastStringBuffer;
import net.sf.saxon.trans.DynamicError;
import net.sf.saxon.trans.XPathException;
import net.sf.saxon.type.*;

import java.util.Calendar;
import java.util.GregorianCalendar;

/**
* A value of type Date. Note that a Date may include a TimeZone.
*/

public class DateValue extends GDateValue {

    /**
     * Constructor given a year, month, and day. Performs no validation.
     * @param year The year as held internally (note that the year before 1AD is 0)
     * @param month The month, 1-12
     * @param day The day 1-31
     */

    public DateValue(int year, byte month, byte day) {
        this.year = year;
        this.month = month;
        this.day = day;
        this.typeLabel = BuiltInAtomicType.DATE;
    }

    /**
     * Constructor given a year, month, and day, and timezone. Performs no validation.
     * @param year The year as held internally (note that the year before 1AD is 0)
     * @param month The month, 1-12
     * @param day The day 1-31
     * @param tz the timezone displacement in minutes from UTC. Supply the value
     * {@link CalendarValue#NO_TIMEZONE} if there is no timezone component.
     */

    public DateValue(int year, byte month, byte day, int tz) {
        this.year = year;
        this.month = month;
        this.day = day;
        setTimezoneInMinutes(tz);
        this.typeLabel = BuiltInAtomicType.DATE;
    }

    /**
     * Constructor given a year, month, and day, and timezone, and an AtomicType representing
     * a subtype of xs:date. Performs no validation.
     * @param year The year as held internally (note that the year before 1AD is 0)
     * @param month The month, 1-12
     * @param day The day 1-31
     * @param tz the timezone displacement in minutes from UTC. Supply the value
     * {@link CalendarValue#NO_TIMEZONE} if there is no timezone component.
     * @param type the type. This must be a type derived from xs:date, and the value
     * must conform to this type. The method does not check these conditions.
     */

    public DateValue(int year, byte month, byte day, int tz, AtomicType type) {
        this.year = year;
        this.month = month;
        this.day = day;
        setTimezoneInMinutes(tz);
        this.typeLabel = type;
    }

    /**
    * Constructor: create a dateTime value from a supplied string, in
    * ISO 8601 format
    */
    public DateValue(CharSequence s) throws XPathException {
        setLexicalValue(s);
        this.typeLabel = BuiltInAtomicType.DATE;
    }

    /**
     * Create a DateValue
     * @param calendar the absolute date/time value
     * @param tz The timezone offset from GMT in minutes, positive or negative; or the special
     * value NO_TIMEZONE indicating that the value is not in a timezone
     */
    public DateValue(GregorianCalendar calendar, int tz) {
        // Note: this constructor is not used by Saxon itself, but might be used by applications
        int era = calendar.get(GregorianCalendar.ERA);
        year = calendar.get(Calendar.YEAR);
        if (era == GregorianCalendar.BC) {
            year = 1-year;
        }
        month = (byte)(calendar.get(Calendar.MONTH)+1);
        day = (byte)(calendar.get(Calendar.DATE));
        setTimezoneInMinutes(tz);
        this.typeLabel = BuiltInAtomicType.DATE;
    }

    /**
     * Determine the primitive type of the value. This delivers the same answer as
     * getItemType().getPrimitiveItemType(). The primitive types are
     * the 19 primitive types of XML Schema, plus xs:integer, xs:dayTimeDuration and xs:yearMonthDuration,
     * and xs:untypedAtomic. For external objects, the result is AnyAtomicType.
     */

    public BuiltInAtomicType getPrimitiveType() {
        return BuiltInAtomicType.DATE;
    }

    /**
     * Get the date that immediately follows a given date
     * @return a new DateValue with no timezone information
     */

    public static DateValue tomorrow(int year, byte month, byte day) {
        if (DateValue.isValidDate(year, month, day+1)) {
            return new DateValue(year, month, (byte)(day+1));
        } else if (month < 12) {
            return new DateValue(year, (byte)(month+1), (byte)1);
        } else {
            return new DateValue(year+1, (byte)1, (byte)1);
        }
    }

    /**
     * Get the date that immediately precedes a given date
     * @return a new DateValue with no timezone information
     */

    public static DateValue yesterday(int year, byte month, byte day) {
        if (day > 1) {
            return new DateValue(year, month, (byte)(day-1));
        } else if (month > 1) {
            if (month == 3 && isLeapYear(year)) {
                return new DateValue(year, (byte)2, (byte)29);
            } else {
                return new DateValue(year, (byte)(month-1), daysPerMonth[month-2]);
            }
        } else {
            return new DateValue(year-1, (byte)12, (byte)31);
        }
    }

    /**
    * Convert to target data type
    * @param requiredType an integer identifying the required atomic type
    * @param context
     * @return an AtomicValue, a value of the required type; or an ErrorValue
    */

    public AtomicValue convertPrimitive(BuiltInAtomicType requiredType, boolean validate, XPathContext context) {
        switch(requiredType.getPrimitiveType()) {
        case Type.DATE:
        case Type.ANY_ATOMIC:
        case Type.ITEM:
            return this;
        case Type.DATE_TIME:
            return toDateTime();

        case Type.STRING:
            return new StringValue(getStringValueCS());

        case Type.UNTYPED_ATOMIC:
            return new UntypedAtomicValue(getStringValueCS());

        case Type.G_YEAR: {
            return new GYearValue(year, getTimezoneInMinutes());
        }
        case Type.G_YEAR_MONTH: {
            return new GYearMonthValue(year, month, getTimezoneInMinutes());
        }
        case Type.G_MONTH: {
            return new GMonthValue(month, getTimezoneInMinutes());
        }
        case Type.G_MONTH_DAY: {
            return new GMonthDayValue(month, day, getTimezoneInMinutes());
        }
        case Type.G_DAY:{
            return new GDayValue(day, getTimezoneInMinutes());
        }

        default:
            ValidationException err = new ValidationException("Cannot convert date to " +
                                     requiredType.getDisplayName());
            err.setErrorCode("XPTY0004");
            err.setIsTypeError(true);
            return new ValidationErrorValue(err);
        }
    }

    /**
    * Convert to string
    * @return ISO 8601 representation.
    */

    public CharSequence getStringValueCS() {

        FastStringBuffer sb = new FastStringBuffer(16);
        int yr = year;
        if (year <= 0) {
            sb.append('-');
            yr = -yr +1;           // no year zero in lexical space
        }
        appendString(sb, yr, (yr>9999 ? (yr+"").length() : 4));
        sb.append('-');
        appendTwoDigits(sb, month);
        sb.append('-');
        appendTwoDigits(sb, day);

        if (hasTimezone()) {
            appendTimezone(sb);
        }

        return sb;

    }

    /**
     * Make a copy of this date, time, or dateTime value
     * @param typeLabel
     */

    public AtomicValue copy(AtomicType typeLabel) {
        DateValue v = new DateValue(year, month, day, getTimezoneInMinutes());
        v.typeLabel = typeLabel;
        return v;
    }

    /**
     * Return a new date with the same normalized value, but
     * in a different timezone. This is called only for a DateValue that has an explicit timezone
     * @param timezone the new timezone offset, in minutes
     * @return the time in the new timezone. This will be a new TimeValue unless no change
     * was required to the original value
     */

    public CalendarValue adjustTimezone(int timezone) {
        DateTimeValue dt = (DateTimeValue)toDateTime().adjustTimezone(timezone);
        return new DateValue(dt.getYear(), dt.getMonth(), dt.getDay(), dt.getTimezoneInMinutes());
    }

    /**
    * Get a component of the value. Returns null if the timezone component is
    * requested and is not present.
    */

    public AtomicValue getComponent(int component) throws XPathException {
        switch (component) {
        case Component.YEAR:
            return new Int64Value((year > 0 ? year : year-1));
        case Component.MONTH:
            return Int64Value.makeIntegerValue(month);
        case Component.DAY:
            return Int64Value.makeIntegerValue(day);
        case Component.TIMEZONE:
            if (hasTimezone()) {
                return DayTimeDurationValue.fromMilliseconds(getTimezoneInMinutes()*60000);
            } else {
                return null;
            }
        default:
            throw new IllegalArgumentException("Unknown component for date: " + component);
        }
    }

    /**
     * Add a duration to a date
     * @param duration the duration to be added (may be negative)
     * @return the new date
     * @throws net.sf.saxon.trans.XPathException if the duration is an xs:duration, as distinct from
     * a subclass thereof
     */

    public CalendarValue add(DurationValue duration) throws XPathException {
        if (duration instanceof DayTimeDurationValue) {
            long microseconds = ((DayTimeDurationValue)duration).getLengthInMicroseconds();
            boolean negative = (microseconds < 0);
            microseconds = Math.abs(microseconds);
            int days = (int)Math.floor((double)microseconds / (1000000L*60L*60L*24L));
            boolean partDay = (microseconds % (1000000L*60L*60L*24L)) > 0;
            int julian = getJulianDayNumber(year, month, day);
            DateValue d = dateFromJulianDayNumber(julian + (negative ? -days : days));
            if (partDay) {
                if (negative) {
                    d = yesterday(d.year, d.month, d.day);
                }
            }
            d.setTimezoneInMinutes(getTimezoneInMinutes());
            return d;
        } else if (duration instanceof YearMonthDurationValue) {
            int months = ((YearMonthDurationValue)duration).getLengthInMonths();
            int m = (month-1) + months;
            int y = year + m / 12;
            m = m % 12;
            if (m < 0) {
                m += 12;
                y -= 1;
            }
            m++;
            int d = day;
            while (!isValidDate(y, m, d)) {
                d -= 1;
            }
            return new DateValue(y, (byte)m, (byte)d, getTimezoneInMinutes());
        } else {
            DynamicError err = new DynamicError(
                    "Date arithmetic is not supported on xs:duration, only on its subtypes");
            err.setIsTypeError(true);
            err.setErrorCode("XPTY0004");
            throw err;
        }
    }

    /**
     * Determine the difference between two points in time, as a duration
     * @param other the other point in time
     * @param context
     * @return the duration as an xs:dayTimeDuration
     * @throws net.sf.saxon.trans.XPathException for example if one value is a date and the other is a time
     */

    public DayTimeDurationValue subtract(CalendarValue other, XPathContext context) throws XPathException {
        if (!(other instanceof DateValue)) {
            DynamicError err = new DynamicError(
                    "First operand of '-' is a date, but the second is not");
            err.setIsTypeError(true);
            err.setErrorCode("XPTY0004");
            throw err;
        }
        return super.subtract(other, context);
    }

    /**
     * Calculate the Julian day number at 00:00 on a given date. This algorithm is taken from
     * http://vsg.cape.com/~pbaum/date/jdalg.htm and
     * http://vsg.cape.com/~pbaum/date/jdalg2.htm
     * (adjusted to handle BC dates correctly)
     */

    public static int getJulianDayNumber(int year, int month, int day) {
        int z = year - (month<3 ? 1 : 0);
        short f = monthData[month-1];
        if (z >= 0) {
            return day + f + 365*z + z/4 - z/100 + z/400 + 1721118;
        } else {
            // for negative years, add 12000 years and then subtract the days!
            z += 12000;
            int j = day + f + 365*z + z/4 - z/100 + z/400 + 1721118;
            return j - (365*12000 + 12000/4 - 12000/100 + 12000/400);  // number of leap years in 12000 years
        }
    }

    /**
     * Get the Gregorian date corresponding to a particular Julian day number. The algorithm
     * is taken from http://www.hermetic.ch/cal_stud/jdn.htm#comp
     * @return a DateValue with no timezone information set
     */

    public static DateValue dateFromJulianDayNumber(int julianDayNumber) {
        if (julianDayNumber >= 0) {
            int L = julianDayNumber + 68569 + 1;    // +1 adjustment for days starting at noon
            int n = ( 4 * L ) / 146097;
            L = L - ( 146097 * n + 3 ) / 4;
            int i = ( 4000 * ( L + 1 ) ) / 1461001;
            L = L - ( 1461 * i ) / 4 + 31;
            int j = ( 80 * L ) / 2447;
            int d = L - ( 2447 * j ) / 80;
            L = j / 11;
            int m = j + 2 - ( 12 * L );
            int y = 100 * ( n - 49 ) + i + L;
            return new DateValue(y, (byte)m, (byte)d);
        } else {
            // add 12000 years and subtract them again...
            DateValue dt = dateFromJulianDayNumber(julianDayNumber +
                    (365*12000 + 12000/4 - 12000/100 + 12000/400));
            dt.year -= 12000;
            return dt;
        }
    }

    /**
     * Get the ordinal day number within the year (1 Jan = 1, 1 Feb = 32, etc)
     */

    public static final int getDayWithinYear(int year, int month, int day) {
        int j = getJulianDayNumber(year, month, day);
        int k = getJulianDayNumber(year, 1, 1);
        return j - k + 1;
    }

    /**
     * Get the day of the week.  The days of the week are numbered from
     * 1 (Monday) to 7 (Sunday)
     */

    public static final int getDayOfWeek(int year, int month, int day) {
        int d = getJulianDayNumber(year, month, day);
        d -= 2378500;   // 1800-01-05 - any Monday would do
        while (d <= 0) {
            d += 70000000;  // any sufficiently-high multiple of 7 would do
        }
        return (d-1)%7 + 1;
    }

    /**
     * Get the ISO week number for a given date.  The days of the week are numbered from
     * 1 (Monday) to 7 (Sunday), and week 1 in any calendar year is the week (from Monday to Sunday)
     * that includes the first Thursday of that year
     */

    public static final int getWeekNumber(int year, int month, int day) {
        int d = getDayWithinYear(year, month, day);
        int firstDay = getDayOfWeek(year, 1, 1);
        if (firstDay > 4 && (firstDay + d) <= 8) {
            // days before week one are part of the last week of the previous year (52 or 53)
            return getWeekNumber(year-1, 12, 31);
        }
        int inc = (firstDay < 5 ? 1 : 0);   // implements the First Thursday rule
        return ((d + firstDay - 2) / 7) + inc;

    }

    /**
     * Get the week number within a month. This is required for the XSLT format-date() function,
     * and the rules are not entirely clear. The days of the week are numbered from
     * 1 (Monday) to 7 (Sunday), and by analogy with the ISO week number, we consider that week 1
     * in any calendar month is the week (from Monday to Sunday) that includes the first Thursday
     * of that month. Unlike the ISO week number, we put the previous days in week zero.
     */

    public static final int getWeekNumberWithinMonth(int year, int month, int day) {
        int firstDay = getDayOfWeek(year, month, 1);
        int inc = (firstDay < 5 ? 1 : 0);   // implements the First Thursday rule
        return ((day + firstDay - 2) / 7) + inc;
    }

    /**
     * Get a Comparable value that implements the XPath ordering comparison semantics for this value.
     * Returns null if the value is not comparable according to XPath rules. The default implementation
     * returns null. This is overridden for types that allow ordered comparisons in XPath: numeric, boolean,
     * string, date, time, dateTime, yearMonthDuration, dayTimeDuration, and anyURI.
     */

    public Comparable getXPathComparable() {
        return this;
    }

    /**
     * Temporary test rig
     */

    public static void main(String[] args) throws Exception {
        DateValue date = new DateValue(args[0]);
        System.out.println(date.getStringValue());
        int jd = getJulianDayNumber(date.year,  date.month, date.day);
        System.out.println(jd);
        System.out.println(dateFromJulianDayNumber(jd).getStringValue());
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

