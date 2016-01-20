////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
// Copyright (c) 2015 Saxonica Limited.
// This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
// If a copy of the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
// This Source Code Form is "Incompatible With Secondary Licenses", as defined by the Mozilla Public License, v. 2.0.
////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

package net.sf.saxon.functions;

import net.sf.saxon.expr.Callable;
import net.sf.saxon.expr.XPathContext;
import net.sf.saxon.om.Sequence;
import net.sf.saxon.trans.XPathException;
import net.sf.saxon.value.*;
import net.sf.saxon.value.StringValue;

import java.util.ArrayList;
import java.util.List;

/**
 * This class implements the function parse-ietf-date(), which is a standard function in XPath 3.1
 */

public class ParseIetfDate extends SystemFunction implements Callable {

    /**
     * Evaluate the expression
     *
     * @param context   the dynamic evaluation context
     * @param arguments the values of the arguments, supplied as Sequence objects
     * @return the result of the evaluation, in the form of a Sequence
     * @throws net.sf.saxon.trans.XPathException
     *          if a dynamic error occurs during the evaluation of the expression
     */
    public Sequence call(XPathContext context, Sequence[] arguments) throws XPathException {
        StringValue stringValue = (StringValue) arguments[0].head();
        if (stringValue == null) {
            return EmptySequence.getInstance();
        }
        return parse(stringValue.getStringValue(), context);
    }

    private String[] dayNames = new String[]{
        "Mon","Tue","Wed","Thu","Fri","Sat","Sun","Monday","Tuesday","Wednesday","Thursday","Friday","Saturday","Sunday"
    };

    private boolean isDayName(String string){
        for (String s: dayNames){
            if (s.equalsIgnoreCase(string)){
                return true;
            }
        }
        return false;
    }

    private String[] monthNames = new String[]{
            "Jan","Feb","Mar","Apr","May","Jun","Jul","Aug","Sep","Oct","Nov","Dec"
    };

    private boolean isMonthName(String string){
        for (String s: monthNames){
            if (s.equalsIgnoreCase(string)){
                return true;
            }
        }
        return false;
    }

    private byte getMonthNumber(String string){
        if ("Jan".equalsIgnoreCase(string)){
            return (byte) 1;
        } else if ("Feb".equalsIgnoreCase(string)){
            return (byte) 2;
        } else if ("Mar".equalsIgnoreCase(string)){
            return (byte) 3;
        } else if ("Apr".equalsIgnoreCase(string)){
            return (byte) 4;
        } else if ("May".equalsIgnoreCase(string)){
            return (byte) 5;
        } else if ("Jun".equalsIgnoreCase(string)){
            return (byte) 6;
        } else if ("Jul".equalsIgnoreCase(string)){
            return (byte) 7;
        } else if ("Aug".equalsIgnoreCase(string)){
            return (byte) 8;
        } else if ("Sep".equalsIgnoreCase(string)){
            return (byte) 9;
        } else if ("Oct".equalsIgnoreCase(string)){
            return (byte) 10;
        } else if ("Nov".equalsIgnoreCase(string)){
            return (byte) 11;
        } else if ("Dec".equalsIgnoreCase(string)){
            return (byte) 12;
        }
        return (byte) 0;
    }

    private int requireDSep(List<String> tokens, int i, String input) throws XPathException {
        boolean found = false;
        if (" ".equals(tokens.get(i))){
            i++;
            found = true;
        }
        if ("-".equals(tokens.get(i))){
            i++;
            found = true;
        }
        if (" ".equals(tokens.get(i))){
            i++;
            found = true;
        }
        if (!found) {
            badDate("Date separator missing", input);
        }
        return i;
    }

    private static void badDate(String msg, String value) throws XPathException{
        XPathException err = new XPathException(
                "Invalid IETF date value " + value + " (" + msg + ")");
        err.setErrorCode("FORG0010");
        throw err;
    }

    private String[] timezoneNames = new String[]{
            "UT","UTC","GMT","EST","EDT","CST","CDT","MST","MDT","PST","PDT"
    };

    private boolean isTimezoneName(String string){
        for (String s: timezoneNames){
            if (s.equalsIgnoreCase(string)){
                return true;
            }
        }
        return false;
    }

    private int getTimezoneOffsetFromName(String string){
        if ("UT".equalsIgnoreCase(string)|"UTC".equalsIgnoreCase(string)|"GMT".equalsIgnoreCase(string)){
            return 0;
        } else if ("EST".equalsIgnoreCase(string)){
            return -5*60;
        } else if ("EDT".equalsIgnoreCase(string)){
            return -4*60;
        } else if ("CST".equalsIgnoreCase(string)){
            return -6*60;
        } else if ("CDT".equalsIgnoreCase(string)){
            return -5*60;
        } else if ("MST".equalsIgnoreCase(string)){
            return -7*60;
        } else if ("MDT".equalsIgnoreCase(string)){
            return -6*60;
        } else if ("PST".equalsIgnoreCase(string)){
            return -8*60;
        } else if ("PDT".equalsIgnoreCase(string)){
            return -7*60;
        }
        return 0; /* what should this return? */
    }

    /**
     * Parse a supplied string to obtain a dateTime
     *
     * @param input     a string containing the date and time in IETF format
     * @param context   the XPath context
     * @return either a DateTimeValue representing the input supplied, or a ValidationFailure if
     *         the input string was invalid
     */

    public DateTimeValue parse(String input, XPathContext context) throws XPathException {
        List<String> tokens = tokenize(input);
        int year = 0;
        byte month = 0;
        byte day = 0;
        List<TimeValue> timeValue = new ArrayList<TimeValue>();
        int i = 0;
        String currentToken = tokens.get(i);
        if (currentToken.matches("[A-Za-z]+") && isDayName(currentToken)){
            currentToken = tokens.get(++i);
            if (",".equals(currentToken)){
                currentToken = tokens.get(++i);
            }
            if (!" ".equals(currentToken)){
                badDate("Space missing after day name", input);
            }
            currentToken = tokens.get(++i);
                /* Now expect either day number or month name */
        }
        if (isMonthName(currentToken)){
            month = getMonthNumber(currentToken);
            i = requireDSep(tokens, i+1, input);
            currentToken = tokens.get(i);
            if (!currentToken.matches("[0-9]+")){
                badDate("Day number expected after month name", input);
            }
            if (currentToken.length() > 2){
                badDate("Day number exceeds two digits", input);
            }
            day = (byte) Integer.parseInt(currentToken);
            currentToken = tokens.get(++i);
            if (!" ".equals(currentToken)){
                badDate("Space missing after day number", input);
            }
                /* Now expect time string */
            i = parseTime(tokens, ++i, timeValue, input);
            currentToken = tokens.get(++i);
            if (!" ".equals(currentToken)){
                badDate("Space missing after time string", input);
            }
            currentToken = tokens.get(++i);
            if (!currentToken.matches("[0-9]+")){
                badDate("Year number expected after time", input);
            }
            else if (currentToken.length() == 4){
                year = Integer.parseInt(currentToken);
            }
            else if (currentToken.length() == 2){
                year = Integer.parseInt(currentToken) +1900;
            }
            else if (currentToken.length() != 4 && currentToken.length() != 4){
                badDate("Year number must be two or four digits", input);
            }
        }
        else if (currentToken.matches("[0-9]+")) {
            if (currentToken.length() > 2){
                badDate("First number in string expected to be day in two digits", input);
            }
            day = (byte) Integer.parseInt(currentToken);
            i = requireDSep(tokens, ++i, input);
            currentToken = tokens.get(i);
            if (!isMonthName(currentToken)){
                badDate("Abbreviated month name expected after day number", input);
            }
            month = getMonthNumber(currentToken);
            i = requireDSep(tokens, ++i, input);
            currentToken = tokens.get(i);
            if (!currentToken.matches("[0-9]+")){
                badDate("Year number expected after month name", input);
            }
            else if (currentToken.length() == 4){
                year = Integer.parseInt(currentToken);
            }
            else if (currentToken.length() == 2){
                year = Integer.parseInt(currentToken) +1900;
            }
            else if (currentToken.length() != 4 && currentToken.length() != 4){
                badDate("Year number must be two or four digits", input);
            }
            currentToken = tokens.get(++i);
            if (!" ".equals(currentToken)){
                badDate("Space missing after year number", input);
            }
                /* Now expect time string ("after..." may differ) */
            i = parseTime(tokens, ++i, timeValue, input);
        }
        else {
            badDate("String expected to begin with month name or day name (or day number)", input);
        }
        if (!GDateValue.isValidDate(year, month, day)) {
            badDate("Date is not valid", input);
        }
        currentToken = tokens.get(++i);
        if (!currentToken.equals(EOF)){
            badDate("Extra content found in string after date",input);
        }
        DateValue date = new DateValue(year, month, day);
        return DateTimeValue.makeDateTimeValue(date, timeValue.get(0));
        /*return DateTimeValue.getCurrentDateTime(context);*/
    }

    /**
     * Parse part of a string (already tokenized) to obtain a TimeValue
     *
     * @param tokens    tokenized string containing the date and time in IETF format
     * @param currentPosition   index of current token
     * @param result    TimeValue produced from parsing time from tokens
     * @return  index of token after parsing the time
     */

    public int parseTime(List<String> tokens, int currentPosition, List<TimeValue> result, String input) throws XPathException{
        byte hour;
        byte minute;
        byte second = 0;
        int microsecond = 0; /*the number of microseconds, 0-999999*/
        int tz = 0; /*the timezone displacement in minutes from UTC.*/
        int i = currentPosition;
        String currentToken = tokens.get(i);
        if (!currentToken.matches("[0-9]+")){
            badDate("Hour number expected", input);
        }
        if (currentToken.length() != 2){
            badDate("Hour must be exactly two digits", input);
        }
        hour = (byte) Integer.parseInt(currentToken);
        currentToken = tokens.get(++i);
        if (!":".equals(currentToken)){
            badDate("Separator ':' missing after hour", input);
        }
        currentToken = tokens.get(++i);
        if (!currentToken.matches("[0-9]+")){
            badDate("Minutes expected after hour", input);
        }
        if (currentToken.length() != 2){
            badDate("Minutes must be exactly two digits", input);
        }
        minute = (byte) Integer.parseInt(currentToken);
        currentToken = tokens.get(++i);
        if (currentToken.equals(EOF)){
            /* seconds, microseconds, timezones not given*/
            TimeValue timeValue = new TimeValue(hour, minute, second, microsecond, tz);
            result.add(timeValue);
            return i-1;
        }
        else if (":".equals(currentToken)){
            currentToken = tokens.get(++i);
            if (!currentToken.matches("[0-9]+")){
                badDate("Seconds expected after ':' separator after minutes", input);
            }
            if (currentToken.length() != 2){
                badDate("Seconds number must have exactly two digits (before decimal point)", input);
            }
            second = (byte) Integer.parseInt(currentToken);
            currentToken = tokens.get(++i);
            if (currentToken.equals(EOF)){
                /* microseconds, timezones not given*/
                TimeValue timeValue = new TimeValue(hour, minute, second, microsecond, tz);
                result.add(timeValue);
                return i-1;
            }
            if (".".equals(currentToken)){
                currentToken = tokens.get(++i);
                if (!currentToken.matches("[0-9]+")){
                    badDate("Fractional part of seconds expected after decimal point", input);
                }
                int len = Math.min(6, currentToken.length());
                currentToken = currentToken.substring(0,len);
                while (currentToken.length() < 6){
                    currentToken = currentToken+"0";
                }
                microsecond = Integer.parseInt(currentToken);
                if (i < tokens.size()-1) {
                    currentToken = tokens.get(++i);
                }
            }
        }
        if (" ".equals(currentToken)) {
            currentToken = tokens.get(++i);
            if (currentToken.matches("[0-9]+")) {
                /* no timezone is given in the time, we must have reached a year */
                TimeValue timeValue = new TimeValue(hour, minute, second, microsecond, tz);
                result.add(timeValue);
                return i-2;
            }
        }
        if (currentToken.matches("[A-Za-z]+")){
            if (!isTimezoneName(currentToken)){
                badDate("Timezone name not recognised",input);
            }
            tz = getTimezoneOffsetFromName(currentToken);
            TimeValue timeValue = new TimeValue(hour, minute, second, microsecond, tz);
            result.add(timeValue);
            return i;

        } else if ("+".equals(currentToken)|"-".equals(currentToken)) {
            String sign = currentToken;
            int tzOffsetHours = 0;
            int tzOffsetMinutes = 0;
            currentToken = tokens.get(++i);
            if (!currentToken.matches("[0-9]+")){
                badDate("Parsing timezone offset, number expected after '" + sign + "'",input);
            }
            else if (currentToken.length() != 2 && currentToken.length() != 4){
                badDate("Timezone offset does not have the correct number of digits",input);
            }
            else if (currentToken.length() == 4){
                tzOffsetHours = Integer.parseInt(currentToken.substring(0,2));
                tzOffsetMinutes = Integer.parseInt(currentToken.substring(2,4));
                currentToken = tokens.get(++i);
            }
            else if (currentToken.length() == 2){
                tzOffsetHours = Integer.parseInt(currentToken);
                currentToken = tokens.get(++i);
                if (":".equals(currentToken)) {
                    currentToken = tokens.get(++i);
                    if (currentToken.matches("[0-9]+")){
                        if (currentToken.length() != 2) {
                            badDate("Parsing timezone offset, minutes must be two digits",input);
                        }
                        else tzOffsetMinutes = Integer.parseInt(currentToken);
                        currentToken = tokens.get(++i);
                    }
                }
            }
            tz = tzOffsetHours*60 + tzOffsetMinutes;
            if (sign.equals("-")){
                tz = -tz;
            }
            if (currentToken.equals(EOF)){
                TimeValue timeValue = new TimeValue(hour, minute, second, microsecond, tz);
                result.add(timeValue);
                return i-1;
            }
            if (" ".equals(currentToken)) {
                currentToken = tokens.get(++i);
                if (currentToken.matches("[0-9]+")) {
                    /* we must have reached the year */
                    TimeValue timeValue = new TimeValue(hour, minute, second, microsecond, tz);
                    result.add(timeValue);
                    return i-2;
                }
            }
            if ("(".equals(currentToken)) {
                currentToken = tokens.get(++i);
                if (" ".equals(currentToken)) {
                    currentToken = tokens.get(++i);
                }
                if (!currentToken.matches("[A-Za-z]+")) {
                    badDate("Timezone name expected after '('",input);
                }
                else if (currentToken.matches("[A-Za-z]+")) {
                    if (!isTimezoneName(currentToken)){
                        badDate("Timezone name not recognised",input);
                    }
                    currentToken = tokens.get(++i);
                }
                if (" ".equals(currentToken)) {
                    currentToken = tokens.get(++i);
                }
                if (!")".equals(currentToken)){
                    badDate("Expected ')' after timezone name",input);
                }
                TimeValue timeValue = new TimeValue(hour, minute, second, microsecond, tz);
                result.add(timeValue);
                return i;
            }
            else {
                badDate("Unexpected content after timezone offset", input);
            }
        } else {
            badDate("Unexpected content in time (after minutes)", input);
        }
        return i; /* Should never reach here */
    }



    private List<String> tokenize(String input) throws XPathException{
        List<String> tokens = new ArrayList<String>();
        input = input.trim();
        if (input.isEmpty()){
            badDate("Input is empty",input);
            return tokens;
        }
        int i = 0;
        input = input + (char) 0;
        while (true) {
            char c = input.charAt(i);
            if (c == 0) {
                tokens.add(EOF);
                return tokens;
            }
            if (Whitespace.isWhite(c)){
                int j = i;
                while (Whitespace.isWhite(input.charAt(j++))){}
                tokens.add(" ");
                i = j-1;
            } else if (Character.isLetter(c)){
                int j = i;
                while (Character.isLetter(input.charAt(j++))){}
                tokens.add(input.substring(i,j-1));
                i = j-1;
            } else if (Character.isDigit(c)){
                int j = i;
                while (Character.isDigit(input.charAt(j++))){}
                tokens.add(input.substring(i,j-1));
                i = j-1;
            } else {
                tokens.add(input.substring(i,i+1));
                i++;
            }
        }
    }

    private static final String EOF = "";
}


// Copyright (c) 2015 Saxonica Limited.