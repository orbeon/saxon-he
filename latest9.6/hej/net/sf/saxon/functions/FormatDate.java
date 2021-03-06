////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
// Copyright (c) 2014 Saxonica Limited.
// This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
// If a copy of the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
// This Source Code Form is "Incompatible With Secondary Licenses", as defined by the Mozilla Public License, v. 2.0.
////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

package net.sf.saxon.functions;

import net.sf.saxon.Configuration;
import net.sf.saxon.expr.Callable;
import net.sf.saxon.expr.StaticContext;
import net.sf.saxon.expr.XPathContext;
import net.sf.saxon.expr.instruct.SavedNamespaceContext;
import net.sf.saxon.expr.number.Alphanumeric;
import net.sf.saxon.expr.number.NamedTimeZone;
import net.sf.saxon.expr.number.Numberer_en;
import net.sf.saxon.expr.parser.ExpressionVisitor;
import net.sf.saxon.lib.Numberer;
import net.sf.saxon.om.NamespaceResolver;
import net.sf.saxon.om.Sequence;
import net.sf.saxon.om.StructuredQName;
import net.sf.saxon.om.ZeroOrOne;
import net.sf.saxon.regex.UnicodeString;
import net.sf.saxon.trans.Err;
import net.sf.saxon.trans.XPathException;
import net.sf.saxon.tree.util.FastStringBuffer;
import net.sf.saxon.value.*;
import net.sf.saxon.value.StringValue;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Implement the format-date(), format-time(), and format-dateTime() functions
 * in XSLT 2.0 and XQuery 1.1.
 */

public class FormatDate extends SystemFunctionCall implements Callable {

    static String[] knownCalendars = {"AD", "AH", "AME", "AM", "AP", "AS", "BE", "CB", "CE", "CL", "CS", "EE", "FE", "ISO", "JE",
            "KE", "KY", "ME", "MS", "NS", "OS", "RS", "SE", "SH", "SS", "TE", "VE", "VS"};

    private NamespaceResolver nsContext = null;
    private boolean is30 = false;


    public void checkArguments(/*@NotNull*/ ExpressionVisitor visitor) throws XPathException {
        int numArgs = argument.length;
        if (numArgs != 2 && numArgs != 5) {
            throw new XPathException("Function " + getDisplayName() +
                    " must have either two or five arguments",
                    this);
        }
        super.checkArguments(visitor);
    }

    /**
     * Bind aspects of the static context on which the particular function depends
     *
     * @param env the static context of the function call
     */
    @Override
    public void bindStaticContext(StaticContext env) throws XPathException {
        is30 = env.getXPathLanguageLevel().equals(DecimalValue.THREE);
        if (nsContext == null && getNumberOfArguments() > 2) {
            nsContext = new SavedNamespaceContext(env.getNamespaceResolver());
        }
    }


    /**
     * Evaluate in a general context
     */

    /*@Nullable*/
    public StringValue evaluateItem(XPathContext context) throws XPathException {
        CalendarValue value = (CalendarValue) argument[0].evaluateItem(context);
        if (value == null) {
            return null;
        }
        String format = argument[1].evaluateItem(context).getStringValue();

        StringValue calendarVal = null;
        StringValue countryVal = null;
        StringValue languageVal = null;
        if (argument.length > 2) {
            languageVal = (StringValue) argument[2].evaluateItem(context);
            calendarVal = (StringValue) argument[3].evaluateItem(context);
            countryVal = (StringValue) argument[4].evaluateItem(context);
        }

        String language = languageVal == null ? null : languageVal.getStringValue();
        String country = countryVal == null ? null : countryVal.getStringValue();
        CharSequence result = formatDate(value, format, language, country, context);
        if (calendarVal != null) {
            result = adjustCalendar(calendarVal, result, context);
        }
        return new StringValue(result);
    }

    private CharSequence adjustCalendar(StringValue calendarVal, CharSequence result, XPathContext context) throws XPathException {
        StructuredQName cal;
        try {
            cal = StructuredQName.fromLexicalQName(calendarVal.getStringValue(), false,
                    is30, nsContext);
        } catch (XPathException e) {
            XPathException err = new XPathException("Invalid calendar name. " + e.getMessage());
            err.setErrorCode("FOFD1340");
            err.setLocator(this);
            err.setXPathContext(context);
            throw err;
        }

        if (cal.hasURI("")) {
            String calLocal = cal.getLocalPart();
            if (calLocal.equals("AD") || calLocal.equals("ISO")) {
                // no action
            } else if (Arrays.binarySearch(knownCalendars, calLocal) >= 0) {
                result = "[Calendar: AD]" + result.toString();
            } else {
                XPathException err = new XPathException("Unknown no-namespace calendar: " + calLocal);
                err.setErrorCode("FOFD1340");
                err.setLocator(this);
                err.setXPathContext(context);
                throw err;
            }
        } else {
            result = "[Calendar: AD]" + result.toString();
        }
        return result;
    }

    /**
     * This method analyzes the formatting picture and delegates the work of formatting
     * individual parts of the date.
     *
     * @param value    the value to be formatted
     * @param format   the supplied format picture
     * @param language the chosen language
     * @param country  the chosen country
     * @param context  the XPath dynamic evaluation context
     * @return the formatted date/time
     * @throws XPathException if a dynamic error occurs
     */

    private static CharSequence formatDate(CalendarValue value, String format, String language, String country, XPathContext context)
            throws XPathException {

        Configuration config = context.getConfiguration();

        boolean languageDefaulted = language == null;
        if (language == null) {
            language = config.getDefaultLanguage();
        }
        if (country == null) {
            country = config.getDefaultCountry();
        }

        Numberer numberer = config.makeNumberer(language, country);
        FastStringBuffer sb = new FastStringBuffer(FastStringBuffer.SMALL);
        if (numberer.getClass() == Numberer_en.class && !"en".equals(language) && !languageDefaulted) {
            sb.append("[Language: en]");
        }
        if(numberer.defaultedLocale() != null) {
            sb.append("[Language: "+ numberer.defaultedLocale().getLanguage()+"]");
        }


        int i = 0;
        while (true) {
            while (i < format.length() && format.charAt(i) != '[') {
                sb.append(format.charAt(i));
                if (format.charAt(i) == ']') {
                    i++;
                    if (i == format.length() || format.charAt(i) != ']') {
                        XPathException e = new XPathException("Closing ']' in date picture must be written as ']]'");
                        e.setErrorCode("XTDE1340");
                        e.setXPathContext(context);
                        throw e;
                    }
                }
                i++;
            }
            if (i == format.length()) {
                break;
            }
            // look for '[['
            i++;
            if (i < format.length() && format.charAt(i) == '[') {
                sb.append('[');
                i++;
            } else {
                int close = i < format.length() ? format.indexOf("]", i) : -1;
                if (close == -1) {
                    XPathException e = new XPathException("Date format contains a '[' with no matching ']'");
                    e.setErrorCode("XTDE1340");
                    e.setXPathContext(context);
                    throw e;
                }
                String componentFormat = format.substring(i, close);
                sb.append(formatComponent(value, Whitespace.removeAllWhitespace(componentFormat),
                        numberer, country, context));
                i = close + 1;
            }
        }
        return sb;
    }

    private static Pattern componentPattern =
            Pattern.compile("([YMDdWwFHhmsfZzPCE])\\s*(.*)");

    private static CharSequence formatComponent(CalendarValue value, CharSequence specifier,
                                                Numberer numberer, String country, XPathContext context)
            throws XPathException {
        boolean ignoreDate = value instanceof TimeValue;
        boolean ignoreTime = value instanceof DateValue;
        DateTimeValue dtvalue = value.toDateTime();

        Matcher matcher = componentPattern.matcher(specifier);
        if (!matcher.matches()) {
            XPathException error = new XPathException("Unrecognized date/time component [" + specifier + ']');
            error.setErrorCode("XTDE1340");
            error.setXPathContext(context);
            throw error;
        }
        String component = matcher.group(1);
        String format = matcher.group(2);
        if (format == null) {
            format = "";
        }
        boolean defaultFormat = false;
        if ("".equals(format) || format.startsWith(",")) {
            defaultFormat = true;
            switch (component.charAt(0)) {
                case 'F':
                    format = "Nn" + format;
                    break;
                case 'P':
                    format = 'n' + format;
                    break;
                case 'C':
                case 'E':
                    format = 'N' + format;
                    break;
                case 'm':
                case 's':
                    format = "01" + format;
                    break;
                case 'z':
                case 'Z':
                    //format = "00:00" + format;
                    break;
                default:
                    format = '1' + format;
            }
        }

        switch (component.charAt(0)) {
            case 'Y':       // year
                if (ignoreDate) {
                    XPathException error = new XPathException("In formatTime(): an xs:time value does not contain a year component");
                    error.setErrorCode("XTDE1350");
                    error.setXPathContext(context);
                    throw error;
                } else {
                    int year = dtvalue.getYear();
                    if (year < 0) {
                        year = 1 - year;
                    }
                    return formatNumber(component, year, format, defaultFormat, numberer, context);
                }
            case 'M':       // month
                if (ignoreDate) {
                    XPathException error = new XPathException("In formatTime(): an xs:time value does not contain a month component");
                    error.setErrorCode("XTDE1350");
                    error.setXPathContext(context);
                    throw error;
                } else {
                    int month = dtvalue.getMonth();
                    return formatNumber(component, month, format, defaultFormat, numberer, context);
                }
            case 'D':       // day in month
                if (ignoreDate) {
                    XPathException error = new XPathException("In formatTime(): an xs:time value does not contain a day component");
                    error.setErrorCode("XTDE1350");
                    error.setXPathContext(context);
                    throw error;
                } else {
                    int day = dtvalue.getDay();
                    return formatNumber(component, day, format, defaultFormat, numberer, context);
                }
            case 'd':       // day in year
                if (ignoreDate) {
                    XPathException error = new XPathException("In formatTime(): an xs:time value does not contain a day component");
                    error.setErrorCode("XTDE1350");
                    error.setXPathContext(context);
                    throw error;
                } else {
                    int day = DateValue.getDayWithinYear(dtvalue.getYear(), dtvalue.getMonth(), dtvalue.getDay());
                    return formatNumber(component, day, format, defaultFormat, numberer, context);
                }
            case 'W':       // week of year
                if (ignoreDate) {
                    XPathException error = new XPathException("In formatTime(): cannot obtain the week number from an xs:time value");
                    error.setErrorCode("XTDE1350");
                    error.setXPathContext(context);
                    throw error;
                } else {
                    int week = DateValue.getWeekNumber(dtvalue.getYear(), dtvalue.getMonth(), dtvalue.getDay());
                    return formatNumber(component, week, format, defaultFormat, numberer, context);
                }
            case 'w':       // week in month
                if (ignoreDate) {
                    XPathException error = new XPathException("In formatTime(): cannot obtain the week number from an xs:time value");
                    error.setErrorCode("XTDE1350");
                    error.setXPathContext(context);
                    throw error;
                } else {
                    int week = DateValue.getWeekNumberWithinMonth(dtvalue.getYear(), dtvalue.getMonth(), dtvalue.getDay());
                    return formatNumber(component, week, format, defaultFormat, numberer, context);
                }
            case 'H':       // hour in day
                if (ignoreTime) {
                    XPathException error = new XPathException("In formatDate(): an xs:date value does not contain an hour component");
                    error.setErrorCode("XTDE1350");
                    error.setXPathContext(context);
                    throw error;
                } else {
                    Int64Value hour = (Int64Value) value.getComponent(AccessorFn.HOURS);
                    return formatNumber(component, (int) hour.longValue(), format, defaultFormat, numberer, context);
                }
            case 'h':       // hour in half-day (12 hour clock)
                if (ignoreTime) {
                    XPathException error = new XPathException("In formatDate(): an xs:date value does not contain an hour component");
                    error.setErrorCode("XTDE1350");
                    error.setXPathContext(context);
                    throw error;
                } else {
                    Int64Value hour = (Int64Value) value.getComponent(AccessorFn.HOURS);
                    int hr = (int) hour.longValue();
                    if (hr > 12) {
                        hr = hr - 12;
                    }
                    if (hr == 0) {
                        hr = 12;
                    }
                    return formatNumber(component, hr, format, defaultFormat, numberer, context);
                }
            case 'm':       // minutes
                if (ignoreTime) {
                    XPathException error = new XPathException("In formatDate(): an xs:date value does not contain a minutes component");
                    error.setErrorCode("XTDE1350");
                    error.setXPathContext(context);
                    throw error;
                } else {
                    Int64Value min = (Int64Value) value.getComponent(AccessorFn.MINUTES);
                    return formatNumber(component, (int) min.longValue(), format, defaultFormat, numberer, context);
                }
            case 's':       // seconds
                if (ignoreTime) {
                    XPathException error = new XPathException("In formatDate(): an xs:date value does not contain a seconds component");
                    error.setErrorCode("XTDE1350");
                    error.setXPathContext(context);
                    throw error;
                } else {
                    IntegerValue sec = (IntegerValue) value.getComponent(AccessorFn.WHOLE_SECONDS);
                    return formatNumber(component, (int) sec.longValue(), format, defaultFormat, numberer, context);
                }
            case 'f':       // fractional seconds
                // ignore the format
                if (ignoreTime) {
                    XPathException error = new XPathException("In formatDate(): an xs:date value does not contain a fractional seconds component");
                    error.setErrorCode("XTDE1350");
                    error.setXPathContext(context);
                    throw error;
                } else {
                    int micros = (int) ((Int64Value) value.getComponent(AccessorFn.MICROSECONDS)).longValue();
                    return formatNumber(component, micros, format, defaultFormat, numberer, context);
                }
            case 'z':
            case 'Z':
                return formatTimeZone(value.toDateTime(), component.charAt(0), format, country);

            case 'F':       // day of week
                if (ignoreDate) {
                    XPathException error = new XPathException("In formatTime(): an xs:time value does not contain day-of-week component");
                    error.setErrorCode("XTDE1350");
                    error.setXPathContext(context);
                    throw error;
                } else {
                    int day = DateValue.getDayOfWeek(dtvalue.getYear(), dtvalue.getMonth(), dtvalue.getDay());
                    return formatNumber(component, day, format, defaultFormat, numberer, context);
                }
            case 'P':       // am/pm marker
                if (ignoreTime) {
                    XPathException error = new XPathException("In formatDate(): an xs:date value does not contain an am/pm component");
                    error.setErrorCode("XTDE1350");
                    error.setXPathContext(context);
                    throw error;
                } else {
                    int minuteOfDay = dtvalue.getHour() * 60 + dtvalue.getMinute();
                    return formatNumber(component, minuteOfDay, format, defaultFormat, numberer, context);
                }
            case 'C':       // calendar
                return numberer.getCalendarName("AD");
            case 'E':       // era
                if (ignoreDate) {
                    XPathException error = new XPathException("In formatTime(): an xs:time value does not contain an AD/BC component");
                    error.setErrorCode("XTDE1350");
                    error.setXPathContext(context);
                    throw error;
                } else {
                    int year = dtvalue.getYear();
                    return numberer.getEraName(year);
                }
            default:
                XPathException e = new XPathException("Unknown formatDate/time component specifier '" + format.charAt(0) + '\'');
                e.setErrorCode("XTDE1340");
                e.setXPathContext(context);
                throw e;
        }
    }

    private static Pattern formatPattern =
            //Pattern.compile("([^ot,]*?)([ot]?)(,.*)?");   // GNU Classpath has problems with this one
            Pattern.compile("([^,]*)(,.*)?");           // Note, the group numbers are different from above

    private static Pattern widthPattern =
            Pattern.compile(",(\\*|[0-9]+)(\\-(\\*|[0-9]+))?");

    private static Pattern alphanumericPattern =
            Pattern.compile("([A-Za-z0-9]|\\p{L}|\\p{N})*");
    // the first term is redundant, but GNU Classpath can't cope with the others...

    private static Pattern digitsPattern =
            Pattern.compile("\\p{Nd}*");

    private static CharSequence formatNumber(String component, int value,
                                             String format, boolean defaultFormat, Numberer numberer, XPathContext context)
            throws XPathException {
        Matcher matcher = formatPattern.matcher(format);
        if (!matcher.matches()) {
            XPathException error = new XPathException("Unrecognized format picture [" + component + format + ']');
            error.setErrorCode("XTDE1340");
            error.setXPathContext(context);
            throw error;
        }
        //String primary = matcher.group(1);
        //String modifier = matcher.group(2);
        String primary = matcher.group(1);
        String modifier = null;
        if (primary.endsWith("t")) {
            primary = primary.substring(0, primary.length() - 1);
            modifier = "t";
        } else if (primary.endsWith("o")) {
            primary = primary.substring(0, primary.length() - 1);
            modifier = "o";
        }
        String letterValue = "t".equals(modifier) ? "traditional" : null;
        String ordinal = "o".equals(modifier) ? numberer.getOrdinalSuffixForDateTime(component) : null;
        String widths = matcher.group(2);   // was 3

        if (!alphanumericPattern.matcher(primary).matches()) {
            XPathException error = new XPathException("In format picture at '" + primary +
                    "', primary format must be alphanumeric");
            error.setErrorCode("XTDE1340");
            error.setXPathContext(context);
            throw error;
        }
        int min = 1;
        int max = Integer.MAX_VALUE;

        if (widths == null || "".equals(widths)) {
            if (digitsPattern.matcher(primary).matches()) {
                int len = StringValue.getStringLength(primary);
                if (len > 1) {
                    // "A format token containing leading zeroes, such as 001, sets the minimum and maximum width..."
                    // We interpret this literally: a format token of "1" does not set a maximum, because it would
                    // cause the year 2006 to be formatted as "6".
                    min = len;
                    max = len;
                }
            }
        } else if (primary.equals("I") || primary.equals("i")) {
            int[] range = getWidths(widths);
            min = range[0];
            //max = Integer.MAX_VALUE;

            String s = numberer.format(value, UnicodeString.makeUnicodeString(primary), null, letterValue, ordinal);
            int len = StringValue.getStringLength(s);
            while (len < min) {
                s = s + ' ';
                len++;
            }
            return s;
        } else {
            int[] range = getWidths(widths);
            min = range[0];
            max = range[1];
            if (defaultFormat) {
                // if format was defaulted, the explicit widths override the implicit format
                if (primary.endsWith("1") && min != primary.length()) {
                    FastStringBuffer sb = new FastStringBuffer(min + 1);
                    for (int i = 1; i < min; i++) {
                        sb.append('0');
                    }
                    sb.append('1');
                    primary = sb.toString();
                }
            }
        }

        if ("P".equals(component)) {
            // A.M./P.M. can only be formatted as a name
            if (!("N".equals(primary) || "n".equals(primary) || "Nn".equals(primary))) {
                primary = "n";
            }
            if (max == Integer.MAX_VALUE) {
                // if no max specified, use 4. An explicit greater value allows use of "noon" and "midnight"
                max = 4;
            }
        } else if ("f".equals(component)) {
            // value is supplied as integer number of microseconds
            String s;
            if (value == 0) {
                s = "0";
            } else {
                s = ((1000000 + value) + "").substring(1);
                if (s.length() > max) {
                    DecimalValue dec = new DecimalValue(new BigDecimal("0." + s));
                    dec = (DecimalValue) dec.roundHalfToEven(max);
                    s = dec.getStringValue();
                    if (s.length() > 2) {
                        // strip the ".0"
                        s = s.substring(2);
                    } else {
                        // fractional seconds value was 0
                        s = "";
                    }
                }
            }
            while (s.length() < min) {
                s = s + '0';
            }
            while (s.length() > min && s.charAt(s.length() - 1) == '0') {
                s = s.substring(0, s.length() - 1);
            }
            return s;
        }

        if ("N".equals(primary) || "n".equals(primary) || "Nn".equals(primary)) {
            String s = "";
            if ("M".equals(component)) {
                s = numberer.monthName(value, min, max);
            } else if ("F".equals(component)) {
                s = numberer.dayName(value, min, max);
            } else if ("P".equals(component)) {
                s = numberer.halfDayName(value, min, max);
            } else {
                primary = "1";
            }
            if ("N".equals(primary)) {
                return s.toUpperCase();
            } else if ("n".equals(primary)) {
                return s.toLowerCase();
            } else {
                return s;
            }
        }

        String s = numberer.format(value, UnicodeString.makeUnicodeString(primary), null, letterValue, ordinal);
        int len = StringValue.getStringLength(s);
        if (len < min) {
            FastStringBuffer fsb = new FastStringBuffer(s);
            while (len < min) {
                fsb.prepend("0");
                len = len + 1;
            }
            s = fsb.toString();
        }
        if (len > max) {
            // the year is the only field we allow to be truncated
            if (component.charAt(0) == 'Y') {
                if (len == s.length()) {
                    // no wide characters
                    s = s.substring(s.length() - max);
                } else {
                    // assert: each character must be two bytes long
                    s = s.substring(s.length() - 2 * max);
                }

            }
        }
        return s;
    }

    private static int[] getWidths(String widths) throws XPathException {
        try {
            int min = -1;
            int max = -1;

            if (!"".equals(widths)) {
                Matcher widthMatcher = widthPattern.matcher(widths);
                if (widthMatcher.matches()) {
                    String smin = widthMatcher.group(1);
                    if (smin == null || "".equals(smin) || "*".equals(smin)) {
                        min = 1;
                    } else {
                        min = Integer.parseInt(smin);
                    }
                    String smax = widthMatcher.group(3);
                    if (smax == null || "".equals(smax) || "*".equals(smax)) {
                        max = Integer.MAX_VALUE;
                    } else {
                        max = Integer.parseInt(smax);
                    }
                } else {
                    XPathException error = new XPathException("Unrecognized width specifier " + Err.wrap(widths, Err.VALUE));
                    error.setErrorCode("XTDE1340");
                    throw error;
                }
            }

            if (min > max && max != -1) {
                XPathException e = new XPathException("Minimum width in date/time picture exceeds maximum width");
                e.setErrorCode("XTDE1340");
                throw e;
            }
            int[] result = new int[2];
            result[0] = min;
            result[1] = max;
            return result;
        } catch (NumberFormatException err) {
            XPathException e = new XPathException("Invalid integer used as width in date/time picture");
            e.setErrorCode("XTDE1340");
            throw e;
        }
    }

    private static String formatTimeZone(DateTimeValue value, char component, String format, String country) throws XPathException {
        int comma = format.lastIndexOf(',');
        String widthModifier = "";
        if (comma >= 0) {
            widthModifier = format.substring(comma);
            format = format.substring(0, comma);
        }
        if (!value.hasTimezone()) {
            if (format.equals("Z")) {
                return "J"; // military "local time"
            } else {
                return "";
            }
        }
        if (format.length() == 0 && widthModifier.length() > 0) {
            int[] widths = getWidths(widthModifier);
            int min = widths[0];
            int max = widths[1];
            if (min <= 1) {
                format = max >= 4 ? "0:00" : "0";
            } else if (min <= 4) {
                format = max >= 5 ? "00:00" : "00";
            } else {
                format = "00:00";
            }
        }
        if (format.length() == 0) {
            format = "00:00";
        }
        int tz = value.getTimezoneInMinutes();
        boolean useZforZero = format.endsWith("t");
        if (useZforZero && tz == 0) {
            return "Z";
        }
        if (useZforZero) {
            format = format.substring(0, format.length() - 1);
        }
        int digits = 0;
        int separators = 0;
        int separatorChar = ':';
        int zeroDigit = -1;
        int[] expandedFormat = StringValue.expand(format);
        for (int ch : expandedFormat) {
            if (Character.isDigit(ch)) {
                digits++;
                if (zeroDigit < 0) {
                    zeroDigit = Alphanumeric.getDigitFamily(ch);
                }
            } else {
                separators++;
                separatorChar = ch;
            }
        }
        int[] buffer = new int[10];
        int used = 0;
        if (digits > 0) {
            // Numeric timezone formatting
            if (component == 'z') {
                buffer[0] = 'G';
                buffer[1] = 'M';
                buffer[2] = 'T';
                used = 3;
            }
            boolean negative = tz < 0;
            tz = java.lang.Math.abs(tz);
            buffer[used++] = negative ? '-' : '+';

            int hour = tz / 60;
            int minute = tz % 60;

            boolean includeMinutes = minute != 0 || digits >= 3 || separators > 0;
            boolean includeSep = (minute != 0 && digits <= 2) || (separators > 0 && (minute != 0 || digits >= 3));

            int hourDigits = digits <= 2 ? digits : digits - 2;

            if (hour > 9 || hourDigits >= 2) {
                buffer[used++] = zeroDigit + hour / 10;
            }
            buffer[used++] = (hour % 10) + zeroDigit;

            if (includeSep) {
                buffer[used++] = separatorChar;
            }
            if (includeMinutes) {
                buffer[used++] = minute / 10 + zeroDigit;
                buffer[used++] = minute % 10 + zeroDigit;
            }

            return StringValue.contract(buffer, used).toString();
        } else if (format.equals("Z")) {
            // military timezone formatting
            int hour = tz / 60;
            int minute = tz % 60;
            if (hour < -12 || hour > 12 || minute != 0) {
                return formatTimeZone(value, 'Z', "00:00", country);
            } else {
                return Character.toString("YXWVUTSRQPONZABCDEFGHIKLM".charAt(hour + 12));
            }
        } else if (format.charAt(0) == 'N' || format.charAt(0) == 'n') {
            return getNamedTimeZone(value, country, format);
        } else {
            return formatTimeZone(value, 'Z', "00:00", country);
        }

    }

    private static String getNamedTimeZone(DateTimeValue value, String country, String format) throws XPathException {

        int min = 1;
        int comma = format.indexOf(',');
        if (comma > 0) {
            String widths = format.substring(comma);
            int[] range = getWidths(widths);
            min = range[0];
        }
        if (format.charAt(0) == 'N' || format.charAt(0) == 'n') {
            if (min <= 5) {
                String tzname = NamedTimeZone.getTimeZoneNameForDate(value, country);
                if (format.charAt(0) == 'n') {
                    tzname = tzname.toLowerCase();
                }
                return tzname;
            } else {
                return NamedTimeZone.getOlsenTimeZoneName(value, country);
            }
        }
        FastStringBuffer sbz = new FastStringBuffer(8);
        value.appendTimezone(sbz);
        return sbz.toString();
    }


    public ZeroOrOne<StringValue> call(XPathContext context, Sequence[] arguments) throws XPathException {
        CalendarValue value = (CalendarValue) arguments[0].head();
        if (value == null) {
            return ZeroOrOne.empty();
        }
        String format = arguments[1].head().getStringValue();

        StringValue calendarVal = null;
        StringValue countryVal = null;
        StringValue languageVal = null;
        if (argument.length > 2) {
            languageVal = (StringValue) arguments[2].head();
            calendarVal = (StringValue) arguments[3].head();
            countryVal = (StringValue) arguments[4].head();
        }

        String language = languageVal == null ? null : languageVal.getStringValue();
        String country = countryVal == null ? null : countryVal.getStringValue();
        CharSequence result = formatDate(value, format, language, country, context);
        if (calendarVal != null) {
            result = adjustCalendar(calendarVal, result, context);
        }
        return new ZeroOrOne<StringValue>(new StringValue(result));
    }

}

