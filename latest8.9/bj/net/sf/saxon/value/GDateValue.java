package net.sf.saxon.value;

import net.sf.saxon.expr.XPathContext;
import net.sf.saxon.trans.XPathException;
import net.sf.saxon.trans.DynamicError;
import net.sf.saxon.type.ValidationException;
import net.sf.saxon.type.TypeHierarchy;
import net.sf.saxon.type.Type;
import net.sf.saxon.Err;
import net.sf.saxon.Configuration;
import net.sf.saxon.sort.ComparisonKey;

import java.util.*;

/**
 * Abstract superclass for the primitive types containing date components: xs:date, xs:gYear,
 * xs:gYearMonth, xs:gMonth, xs:gMonthDay, xs:gDay
 */
public abstract class GDateValue extends CalendarValue {
    protected int year;         // unlike the lexical representation, includes a year zero
    protected byte month;
    protected byte day;
    /**
     * Test whether a candidate date is actually a valid date in the proleptic Gregorian calendar
     */

    protected static byte[] daysPerMonth = {31, 28, 31, 30, 31, 30, 31, 31, 30, 31, 30, 31};
    protected static final short[] monthData = {306, 337, 0, 31, 61, 92, 122, 153, 184, 214, 245, 275};

    /**
     * Get the year component of the date (in local form)
     */

    public int getYear() {
        return year;
    }

    /**
     * Get the month component of the date (in local form)
     */

    public byte getMonth() {
        return month;
    }

    /**
     * Get the day component of the date (in local form)
     */

    public byte getDay() {
        return day;
    }

    public GregorianCalendar getCalendar() {

        int tz = (hasTimezone() ? getTimezoneInMinutes() : 0);
        TimeZone zone = new SimpleTimeZone(tz*60000, "LLL");
        GregorianCalendar calendar = new GregorianCalendar(zone);
        calendar.setGregorianChange(new Date(Long.MIN_VALUE));
        calendar.clear();
        calendar.setLenient(false);
        int yr = year;
        if (year <= 0) {
            yr = 1-year;
            calendar.set(Calendar.ERA, GregorianCalendar.BC);
        }
        calendar.set(yr, month-1, day);
        calendar.set(Calendar.ZONE_OFFSET, tz*60000);
        calendar.set(Calendar.DST_OFFSET, 0);
        calendar.getTime();
        return calendar;
    }

    /**
    * Convert to Java object (for passing to external functions)
    */

    public Object convertToJava(Class target, XPathContext context) throws XPathException {
        if (target.isAssignableFrom(Date.class)) {
            return getCalendar().getTime();
        } else if (target.isAssignableFrom(GregorianCalendar.class)) {
            return getCalendar();
        } else if (target.isAssignableFrom(DateValue.class)) {
            return this;
        } else if (target==String.class) {
            return getStringValue();
        } else if (target.isAssignableFrom(CharSequence.class)) {
            return getStringValueCS();
        } else if (target==Object.class) {
            return getStringValue();
        } else {
            Object o = convertToJava(target, context);
            if (o == null) {
                throw new DynamicError("Conversion of date to " + target.getName() +
                        " is not supported");
            }
            return o;
        }
    }

    /**
     * Initialize the DateValue using a character string in the format yyyy-mm-dd and an optional time zone.
     * Input must have format [-]yyyy-mm-dd[([+|-]hh:mm | Z)]
     * @param s the supplied string value
     * @throws net.sf.saxon.trans.XPathException
     */
    public void setLexicalValue(CharSequence s) throws XPathException {
        StringTokenizer tok = new StringTokenizer(Whitespace.trimWhitespace(s).toString(), "-:+Z", true);
        try {
            if (!tok.hasMoreElements()) badDate("Too short", s);
            String part = (String)tok.nextElement();
            int era = +1;
            if ("+".equals(part)) {
                badDate("Date may not start with '+' sign", s);
            } else if ("-".equals(part)) {
                era = -1;
                part = (String)tok.nextElement();
            }

            if (part.length() < 4) {
                badDate("Year is less than four digits", s);
            }
            if (part.length() > 4 && part.charAt(0) == '0') {
                badDate("When year exceeds 4 digits, leading zeroes are not allowed", s);
            }
            year = Integer.parseInt(part) * era;
            if (year==0) {
                badDate("Year zero is not allowed", s);
            }
            if (era < 0) {
                year++;     // internal representation allows a year zero.
            }
            if (!tok.hasMoreElements()) badDate("Too short", s);
            if (!"-".equals(tok.nextElement())) badDate("Wrong delimiter after year", s);

            if (!tok.hasMoreElements()) badDate("Too short", s);
            part = (String)tok.nextElement();
            if (part.length() != 2) badDate("Month must be two digits", s);
            month = (byte)Integer.parseInt(part);
            if (month < 1 || month > 12) badDate("Month is out of range", s);
            if (!tok.hasMoreElements()) badDate("Too short", s);
            if (!"-".equals(tok.nextElement())) badDate("Wrong delimiter after month", s);

            if (!tok.hasMoreElements()) badDate("Too short", s);
            part = (String)tok.nextElement();
            if (part.length() != 2) badDate("Day must be two digits", s);
            day = (byte)Integer.parseInt(part);
            if (day < 1 || day > 31) badDate("Day is out of range", s);

            int tzOffset;
            if (tok.hasMoreElements()) {

                String delim = (String)tok.nextElement();

                if ("Z".equals(delim)) {
                    tzOffset = 0;
                    if (tok.hasMoreElements()) badDate("Continues after 'Z'", s);
                    setTimezoneInMinutes(tzOffset);

                } else if (!(!"+".equals(delim) && !"-".equals(delim))) {
                    if (!tok.hasMoreElements()) badDate("Missing timezone", s);
                    part = (String)tok.nextElement();
                    int tzhour = Integer.parseInt(part);
                    if (part.length() != 2) badDate("Timezone hour must be two digits", s);
                    if (tzhour > 14) badDate("Timezone hour is out of range", s);
                    //if (tzhour > 12) badDate("Because of Java limitations, Saxon currently limits the timezone to +/- 12 hours", s);
                    if (!tok.hasMoreElements()) badDate("No minutes in timezone", s);
                    if (!":".equals(tok.nextElement())) badDate("Wrong delimiter after timezone hour", s);

                    if (!tok.hasMoreElements()) badDate("No minutes in timezone", s);
                    part = (String)tok.nextElement();
                    int tzminute = Integer.parseInt(part);
                    if (part.length() != 2) badDate("Timezone minute must be two digits", s);
                    if (tzminute > 59) badDate("Timezone minute is out of range", s);
                    if (tok.hasMoreElements()) badDate("Continues after timezone", s);

                    tzOffset = (tzhour*60 + tzminute);
                    if ("-".equals(delim)) tzOffset = -tzOffset;
                    setTimezoneInMinutes(tzOffset);

                } else {
                    badDate("Timezone format is incorrect", s);
                }
            }

            if (!isValidDate(year, month, day)) {
                badDate("Non-existent date", s);
            }


        } catch (NumberFormatException err) {
            badDate("Non-numeric component", s);
        }
    }

    private void badDate(String msg, CharSequence value) throws ValidationException {
        ValidationException err = new ValidationException(
                "Invalid date " + Err.wrap(value, Err.VALUE) + " (" + msg + ")");
        err.setErrorCode("FORG0001");
        throw err;
    }

    public static boolean isValidDate(int year, int month, int day) {
        if (month > 0 && month <= 12 && day > 0 && day <= daysPerMonth[month-1]) {
            return true;
        }
        if (month == 2 && day == 29) {
            return isLeapYear(year);
        }
        return false;
    }

    /**
     * Test whether a year is a leap year
     */

    public static boolean isLeapYear(int year) {
        return (year % 4 == 0) && !(year % 100 == 0 && !(year % 400 == 0));
    }

    /**
    * Compare the value to another date value. This method is used only during schema processing,
    * and uses XML Schema semantics rather than XPath semantics.
    * @param other The other date value. Must be an object of class DateValue.
    * @return negative value if this one is the earlier, 0 if they are chronologically equal,
    * positive value if this one is the later. For this purpose, dateTime values with an unknown
    * timezone are considered to be UTC values (the Comparable interface requires
    * a total ordering).
    * @throws ClassCastException if the other value is not a DateValue (the parameter
    * is declared as Object to satisfy the Comparable interface)
    */

    public int compareTo(Object other) {
        if (!(other instanceof GDateValue)) {
            throw new ClassCastException("Date values are not comparable to " + other.getClass());
        }
        return compareTo((GDateValue)other, new Configuration());
    }

    /**
     * Compare this value to another value of the same type, using the supplied context object
     * to get the implicit timezone if required. This method implements the XPath comparison semantics.
     */

    public int compareTo(CalendarValue other, Configuration config) {
        final TypeHierarchy th = config.getTypeHierarchy();
        if (this.getItemType(th).getPrimitiveType() != other.getItemType(th).getPrimitiveType()) {
            throw new ClassCastException("Cannot compare values of different types");
                        // covers, for example, comparing a gYear to a gYearMonth
        }
        // This code allows comparison of a gYear (etc) to a date, but this is prevented at a higher level
        return toDateTime().compareTo(other.toDateTime(), config);
    }

    /**
     * Convert to DateTime
     */

    public DateTimeValue toDateTime() {
        return new DateTimeValue(year, month, day, (byte)0, (byte)0, (byte)0, 0, getTimezoneInMinutes());
    }

    /**
     * Get a comparison key for this value. Two values are equal if and only if they their comparison
     * keys are equal
     */


    public ComparisonKey getComparisonKey(Configuration config) {
        return new ComparisonKey(Type.DATE, toDateTime().normalize(config));
    }

    public boolean equals(Object other) {
        return compareTo(other) == 0;
    }

    public int hashCode() {
        // Equality must imply same hashcode, but not vice-versa
        return getCalendar().getTime().hashCode() + getTimezoneInMinutes();
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
// The Original Code is: all this file
//
// The Initial Developer of the Original Code is Michael H. Kay.
//
// Contributor(s):
//

