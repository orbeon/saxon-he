package net.sf.saxon.option.local;

import net.sf.saxon.charcode.UTF16CharacterSet;
import net.sf.saxon.number.Numberer_en;
import net.sf.saxon.om.FastStringBuffer;


/**
  * Class Numberer_en does number formatting for language="en".
  * This supports the xsl:number element.
  * Methods and data are declared as protected, and static is avoided, to allow easy subclassing.
  * @author Michael H. Kay
  */

public class Numberer_he extends Numberer_en {

    /**
    * Format a number into a string
    * @param number The number to be formatted
    * @param picture The format token. This is a single component of the format attribute
    * of xsl:number, e.g. "1", "01", "i", or "a"
    * @param groupSize number of digits per group (0 implies no grouping)
    * @param groupSeparator string to appear between groups of digits
    * @param letterValue The letter-value specified to xsl:number: "alphabetic" or
    * "traditional". Can also be an empty string or null.
    * @param ordinal The value of the ordinal attribute specified to xsl:number
    * The value "yes" indicates that ordinal numbers should be used; "" or null indicates
    * that cardinal numbers
    * @return the formatted number. Note that no errors are reported; if the request
    * is invalid, the number is formatted as if the string() function were used.
    */

    public String format(long number,
                         String picture,
                         int groupSize,
                         String groupSeparator,
                         String letterValue,
                         String ordinal) {

        FastStringBuffer sb = new FastStringBuffer(16);
        int formchar = picture.charAt(0);
        if (UTF16CharacterSet.isHighSurrogate(formchar)) {
            formchar = UTF16CharacterSet.combinePair((char)formchar, picture.charAt(1));
        }

        /* only catch traditional formatting. */
        if (!"traditional".equals(letterValue)) {
        	return super.format(number, picture, groupSize, groupSeparator, letterValue, ordinal);
        }

        switch(formchar) {
        case '\u05d0':
            if (number==0) return "0";
            sb.append(toTraditionalSequence(number, groupSeparator));
            break;
        default:
        	return super.format(number, picture, groupSize, groupSeparator, letterValue, ordinal);
        }

        return sb.toString();
    }

    /** Convert a number to traditional Hebrew representation.
     * All parameters are the same as for format()
     *
     * @param number
     * @param groupSeparator
     * @return The number in traditional Hebrew.  The result for numbers above 9999
     * is not really well-defined.  Neither is 0.
     */
    protected String toTraditionalSequence(long number,
    		String groupSeparator) {
    	String result = "";

    	if (groupSeparator == null || groupSeparator.length() == 0) {
    		groupSeparator = "\u05f3";
    	}

    	long originalNumber = number;
    	while (number > 0) {
    		int thisTriplet = (int)(number % 1000);
    		String thisThousandString;
    		number /= 1000;
    		/* 344 (which would be shin-mem-dalet) is an exception to the scheme */
    		if (thisTriplet == 344) {
    			thisThousandString =  "\u05e9\u05d3\u05de";
    		}
    		else {
    			int thisUnit = thisTriplet % 10;
    			int thisTen = (thisTriplet/10) % 10;
    			int thisTeen = thisTriplet % 100;
    			int thisHundred = thisTriplet / 100;
    			/* 15 and 16 are exceptions, \u05f4 is added on the last digit
    			 * (that really should be an option) */
				thisThousandString = hebrewHundreds[thisHundred] +
					(
							(thisTeen == 15 || thisTeen == 16) ?
									("\u05d8" +
											((thisTeen == 15) ? "\u05d5" : "\u05d6"))
											: (hebrewTens[thisTen] + hebrewUnits[thisUnit]));

    		}
    		/* add the thousands separator */
    		result = thisThousandString + ((result.length() > 0) ? groupSeparator: "") + result;
    	}
    	/* add the gershayim if length > 2 or a geresh for single-length results */
    	int len = result.length();
    	int minGershayimLength = 2;
    	if (originalNumber > 1000) {
    		minGershayimLength = 4;
    	}

    	if (len == 1) {
    		result = result + ((number > 1000) ? '\u05f4' : '\u05f3');
    	}
    	else if (len >= minGershayimLength) {
    		result = result.substring(0, len - 1) + '\u05f4' + result.charAt(len - 1);
    	}

    	return result;

    }

    private static String[] hebrewUnits = {
        "", "\u05d0", "\u05d1", "\u05d2", "\u05d3", "\u05d4", "\u05d5", "\u05d6", "\u05d7", "\u05d8",
        "\u05d9"};

    private static String[] hebrewTens = {
        "", "\u05d9", "\u05db", "\u05dc", "\u05de", "\u05e0", "\u05e1",
        "\u05e2", "\u05e4", "\u05e6"};

    private static String[] hebrewHundreds = {
    	"", "\u05e7", "\u05e8", "\u05e9", "\u05ea", "\u05ea\u05e7",
    	"\u05ea\u05e8", "\u05ea\u05e9", "\u05ea\u05ea", "\u05ea\u05ea\u05e7"};




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
// The Original Code is: all code taken from Numberer_en.java.
//
// The Initial Developer of the Original Code is Michael H. Kay.
//
// Portions created by Efraim Feinstein are Copyright (C) Efraim Feinstein. All Rights Reserved.
//
// Contributor(s): none.
//
