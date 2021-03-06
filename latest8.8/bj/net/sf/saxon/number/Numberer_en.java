package net.sf.saxon.number;
import net.sf.saxon.om.FastStringBuffer;
import net.sf.saxon.om.XMLChar;
import net.sf.saxon.value.DateTimeValue;
import net.sf.saxon.value.StringValue;

import java.io.Serializable;
import java.util.HashMap;
import java.util.List;
import java.util.ArrayList;

/**
  * Class Numberer_en does number formatting for language="en".
  * This supports the xsl:number element.
  * Methods and data are declared as protected, and static is avoided, to allow easy subclassing.
  * @author Michael H. Kay
  */

public class Numberer_en implements Numberer, Serializable {

    private String country;

    public static final int UPPER_CASE = 0;
    public static final int LOWER_CASE = 1;
    public static final int TITLE_CASE = 2;


    /**
     * Set the country used by this numberer (currenly used only for names of timezones)
     */

    public void setCountry(String country) {
        this.country = country;
    }

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

        if (number < 0) {
            return "" + number;
        }
        if (picture==null || picture.length()==0) {
            return "" + number;
        }

        int pictureLength = StringValue.getStringLength(picture);
        FastStringBuffer sb = new FastStringBuffer(16);
        int formchar = picture.charAt(0);
        if (XMLChar.isHighSurrogate(formchar)) {
            formchar = XMLChar.supplemental((char)formchar, picture.charAt(1));
        }

        switch(formchar) {

        case '0':
        case '1':
            sb.append(toRadical(number, westernDigits, pictureLength, groupSize, groupSeparator));
            if (ordinal != null && ordinal.length() > 0) {
                sb.append(ordinalSuffix(ordinal, number));
            }
            break;

        case 'A':
            if (number==0) return "0";
            sb.append(toAlphaSequence(number, latinUpper));
            break;

        case 'a':
            if (number==0) return "0";
            sb.append(toAlphaSequence(number, latinLower));
            break;

        case 'w':
        case 'W':
            int wordCase;
            if (picture.equals("W")) {
                wordCase = UPPER_CASE;
            } else if (picture.equals("w")) {
                wordCase = LOWER_CASE;
            } else {
                wordCase = TITLE_CASE;
            }
            if (ordinal != null && ordinal.length() > 0) {
                sb.append(toOrdinalWords(ordinal, number, wordCase));

            } else {
                sb.append(toWords(number, wordCase));
            }
            break;

        case 'i':
            if (number==0) return "0";
            if (letterValue==null || letterValue.equals("") ||
                    letterValue.equals("traditional")) {
                sb.append(toRoman(number));
            } else {
                alphaDefault(number, 'i', sb);
            }
            break;

        case 'I':
            if (number==0) return "0";
            if (letterValue==null || letterValue.equals("") ||
                    letterValue.equals("traditional")) {
                sb.append(toRoman(number).toUpperCase());
            } else {
                alphaDefault(number, 'I', sb);
            }
            break;

        case '\u0391':
            if (number==0) return "0";
            sb.append(toAlphaSequence(number, greekUpper));
            break;

        case '\u03b1':
            if (number==0) return "0";
            sb.append(toAlphaSequence(number, greekLower));
            break;

        case '\u0410':
            if (number==0) return "0";
            sb.append(toAlphaSequence(number, cyrillicUpper));
            break;

        case '\u0430':
            if (number==0) return "0";
            sb.append(toAlphaSequence(number, cyrillicLower));
            break;

        case '\u05d0':
            if (number==0) return "0";
            sb.append(toAlphaSequence(number, hebrew));
            break;

        case '\u3042':
            if (number==0) return "0";
            sb.append(toAlphaSequence(number, hiraganaA));
            break;

        case '\u30a2':
            if (number==0) return "0";
            sb.append(toAlphaSequence(number, katakanaA));
            break;

        case '\u3044':
            if (number==0) return "0";
            sb.append(toAlphaSequence(number, hiraganaI));
            break;

        case '\u30a4':
            if (number==0) return "0";
            sb.append(toAlphaSequence(number, katakanaI));
            break;

        case '\u4e00':
            if (number==0) return "0";
            sb.append(toRadical(number, kanjiDigits, pictureLength, groupSize, groupSeparator));
            break;

        default:

            int digitValue = NumberFormatter.getDigitValue(formchar);
            if (digitValue >= 0) {

                int zero = formchar - digitValue;
                int[] digits = new int[10];
                for (int z=0; z<=9; z++) {
                    digits[z] = zero+z;
                }

                sb.append(toRadical(number, digits, pictureLength, groupSize, groupSeparator));
                break;

            } else {
                if (number==0) return "0";
                if (formchar < '\u1100') {
                    alphaDefault(number, (char)formchar, sb);
                } else {
                    // fallback to western numbering
                    sb.append(
                        toRadical(number, westernDigits, pictureLength, groupSize, groupSeparator));
                }
                break;

            }
        }

        return sb.toString();
    }


    /**
     * Construct the ordinal suffix for a number, for example "st", "nd", "rd"
     * @param ordinalParam the value of the ordinal attribute (used in non-English
     * language implementations)
     * @param number the number being formatted
     * @return the ordinal suffix to be appended to the formatted number
     */

    protected String ordinalSuffix(String ordinalParam, long number) {
        int penult = ((int)(number % 100)) / 10;
        int ult = (int)(number % 10);
        if (penult==1) {
            // e.g. 11th, 12th, 13th
            return "th";
        } else {
            if (ult==1) {
                return "st";
            } else if (ult==2) {
                return "nd";
            } else if (ult==3) {
                return "rd";
            } else {
                return "th";
            }
        }
    }

    protected static final int[] westernDigits = {'0', '1', '2', '3', '4', '5', '6', '7', '8', '9'};

    protected static final String latinUpper =
        "ABCDEFGHIJKLMNOPQRSTUVWXYZ";

    protected static final String latinLower =
        "abcdefghijklmnopqrstuvwxyz";

    protected static final String greekUpper =
        "\u0391\u0392\u0393\u0394\u0395\u0396\u0397\u0398\u0399\u039a" +
        "\u039b\u039c\u039d\u039e\u039f\u03a0\u03a1\u03a2\u03a3\u03a4" +
        "\u03a5\u03a6\u03a7\u03a8\u03a9";

    protected static final String greekLower =
        "\u03b1\u03b2\u03b3\u03b4\u03b5\u03b6\u03b7\u03b8\u03b9\u03ba" +
        "\u03bb\u03bc\u03bd\u03be\u03bf\u03c0\u03c1\u03c2\u03c3\u03c4" +
        "\u03c5\u03c6\u03c7\u03c8\u03c9";


    // Cyrillic information from Dmitry Kirsanov [dmitry@kirsanov.com]
    // (based on his personal knowledge of Russian texts, not any authoritative source)

    protected static final String cyrillicUpper =
        "\u0410\u0411\u0412\u0413\u0414\u0415\u0416\u0417\u0418" +
        "\u041a\u041b\u041c\u041d\u041e\u041f\u0420\u0421\u0421\u0423" +
        "\u0424\u0425\u0426\u0427\u0428\u0429\u042b\u042d\u042e\u042f";

    protected static final String cyrillicLower =
        "\u0430\u0431\u0432\u0433\u0434\u0435\u0436\u0437\u0438" +
        "\u043a\u043b\u043c\u043d\u043e\u043f\u0440\u0441\u0441\u0443" +
        "\u0444\u0445\u0446\u0447\u0448\u0449\u044b\u044d\u044e\u044f";

    protected static final String hebrew =
        "\u05d0\u05d1\u05d2\u05d3\u05d4\u05d5\u05d6\u05d7\u05d8\u05d9\u05db\u05dc" +
        "\u05de\u05e0\u05e1\u05e2\u05e4\u05e6\u05e7\u05e8\u05e9\u05ea";


    // The following Japanese sequences were supplied by
    // MURAKAMI Shinyu [murakami@nadita.com]

    protected static final String hiraganaA =
        "\u3042\u3044\u3046\u3048\u304a\u304b\u304d\u304f\u3051\u3053" +
        "\u3055\u3057\u3059\u305b\u305d\u305f\u3061\u3064\u3066\u3068" +
        "\u306a\u306b\u306c\u306d\u306e\u306f\u3072\u3075\u3078\u307b" +
        "\u307e\u307f\u3080\u3081\u3082\u3084\u3086\u3088\u3089\u308a" +
        "\u308b\u308c\u308d\u308f\u3092\u3093";

    protected static final String katakanaA =

        "\u30a2\u30a4\u30a6\u30a8\u30aa\u30ab\u30ad\u30af\u30b1\u30b3" +
        "\u30b5\u30b7\u30b9\u30bb\u30bd\u30bf\u30c1\u30c4\u30c6\u30c8" +
        "\u30ca\u30cb\u30cc\u30cd\u30ce\u30cf\u30d2\u30d5\u30d8\u30db" +
        "\u30de\u30df\u30e0\u30e1\u30e2\u30e4\u30e6\u30e8\u30e9\u30ea" +
        "\u30eb\u30ec\u30ed\u30ef\u30f2\u30f3";

    protected static final String hiraganaI =

        "\u3044\u308d\u306f\u306b\u307b\u3078\u3068\u3061\u308a\u306c" +
        "\u308b\u3092\u308f\u304b\u3088\u305f\u308c\u305d\u3064\u306d" +
        "\u306a\u3089\u3080\u3046\u3090\u306e\u304a\u304f\u3084\u307e" +
        "\u3051\u3075\u3053\u3048\u3066\u3042\u3055\u304d\u3086\u3081" +
        "\u307f\u3057\u3091\u3072\u3082\u305b\u3059";

    protected static final String katakanaI =

        "\u30a4\u30ed\u30cf\u30cb\u30db\u30d8\u30c8\u30c1\u30ea\u30cc" +
        "\u30eb\u30f2\u30ef\u30ab\u30e8\u30bf\u30ec\u30bd\u30c4\u30cd" +
        "\u30ca\u30e9\u30e0\u30a6\u30f0\u30ce\u30aa\u30af\u30e4\u30de" +
        "\u30b1\u30d5\u30b3\u30a8\u30c6\u30a2\u30b5\u30ad\u30e6\u30e1" +
        "\u30df\u30b7\u30f1\u30d2\u30e2\u30bb\u30b9";


    protected static final int[] kanjiDigits =
            {0x3007, 0x4e00, 0x4e8c, 0x4e09, 0x56db, 0x4e94, 0x516d, 0x4e03, 0x516b, 0x4e5d};
        //"\u3007\u4e00\u4e8c\u4e09\u56db\u4e94\u516d\u4e03\u516b\u4e5d";


    /**
    * Default processing with an alphabetic format token: use the contiguous
    * range of Unicode letters starting with that token.
    */

    protected void alphaDefault(long number, char formchar, FastStringBuffer sb) {
        int min = (int)formchar;
        int max = (int)formchar;
        // use the contiguous range of letters starting with the specified one
        while (Character.isLetterOrDigit((char)(max+1))) {
            max++;
        }
        sb.append(toAlpha(number, min, max));
    }

    /**
    * Format the number as an alphabetic label using the alphabet consisting
    * of consecutive Unicode characters from min to max
    */

    protected String toAlpha(long number, int min, int max) {
        if (number<=0) return "" + number;
        int range = max - min + 1;
        char last = (char)(((number-1) % range) + min);
        if (number>range) {
            return toAlpha((number-1)/range, min, max) + last;
        } else {
            return "" + last;
        }
    }

    /**
    * Convert the number into an alphabetic label using a given alphabet.
    * For example, if the alphabet is "xyz" the sequence is x, y, z, xx, xy, xz, ....
    */

    protected String toAlphaSequence(long number, String alphabet) {
        if (number<=0) return "" + number;
        int range = alphabet.length();
        char last = alphabet.charAt((int)((number-1) % range));
        if (number>range) {
            return toAlphaSequence((number-1)/range, alphabet) + last;
        } else {
            return "" + last;
        }
    }

    /**
    * Convert the number into a decimal or other representation using the given set of
    * digits.
    * For example, if the digits are "01" the sequence is 1, 10, 11, 100, 101, 110, 111, ...
    * More commonly, the digits will be "0123456789", giving the usual decimal numbering.
    * @param number the number to be formatted
    * @param digits the codepoints to be used for the digits
    * @param pictureLength the length of the picture that is significant: for example "3" if the
     * picture is "001"
    * @param groupSize the number of digits in each group
    * @param groupSeparator the separator to use between groups of digits.
    */

    private String toRadical(long number, int[] digits, int pictureLength,
                                 int groupSize, String groupSeparator) {

        FastStringBuffer sb = new FastStringBuffer(16);
        FastStringBuffer temp = new FastStringBuffer(16);
        int base = digits.length;
        int width = (digits[0] > 0xffff ? 2 : 1);
        FastStringBuffer s = new FastStringBuffer(16);
        long n = number;
        int count = 0;
        while (n>0) {
            int digit = digits[(int)(n % base)];
            s.prependWideChar(digit);
            count++;
            n = n / base;
        }

        for (int i=0; i<(pictureLength-count); i++) {
            temp.appendWideChar(digits[0]);
        }
        temp.append(s);

        if (groupSize>0) {
            groupSize *= width;
            for (int i=0; i<temp.length(); i++) {
                if (i!=0 && ((temp.length()-i) % groupSize) == 0) {
                    sb.append(groupSeparator);
                }
                sb.append(temp.charAt(i));
            }
        } else {
            sb = temp;
        }

        return sb.toString();
    }

    /**
    * Generate a Roman numeral (in lower case)
    */

    public static String toRoman(long n) {
        if (n<=0 || n>9999) return "" + n;
        return romanThousands[(int)n/1000] +
               romanHundreds[((int)n/100) % 10] +
               romanTens[((int)n/10) % 10] +
               romanUnits[(int)n % 10];
    }

    // Roman numbers beyond 4000 use overlining and other conventions which we won't
    // attempt to reproduce. We'll go high enough to handle present-day Gregorian years.

    private static String[] romanThousands =
        {"", "m", "mm", "mmm", "mmmm", "mmmmm", "mmmmmm", "mmmmmmm", "mmmmmmmm", "mmmmmmmmm"};
    private static String[] romanHundreds =
        {"", "c", "cc", "ccc", "cd", "d", "dc", "dcc", "dccc", "cm"};
    private static String[] romanTens =
        {"", "x", "xx", "xxx", "xl", "l", "lx", "lxx", "lxxx", "xc"};
    private static String[] romanUnits =
        {"", "i", "ii", "iii", "iv", "v", "vi", "vii", "viii", "ix"};


    /**
    * Show the number as words in title case. (We choose title case because
     * the result can then be converted algorithmically to lower case or upper case).
    */

    public String toWords(long number) {
        if (number >= 1000000000) {
            long rem = number % 1000000000;
            return toWords(number / 1000000000) + " Billion" +
                    (rem==0 ? "" : (rem < 100 ? " and " : " ") + toWords(rem));
        } else if (number >= 1000000) {
            long rem = number % 1000000;
            return toWords(number / 1000000) + " Million" +
                    (rem==0 ? "" : (rem < 100 ? " and " : " ") + toWords(rem));
        } else if (number >= 1000) {
            long rem = number % 1000;
            return toWords(number / 1000) + " Thousand" +
                    (rem==0 ? "" : (rem < 100 ? " and " : " ") + toWords(rem));
        } else if (number >= 100) {
            long rem = number % 100;
            return toWords(number / 100) + " Hundred" +
                (rem==0 ? "" : " and " + toWords(rem));
        } else {
            if (number < 20) return englishUnits[(int)number];
            int rem = (int)(number % 10);
            return englishTens[(int)number / 10] +
                (rem==0 ? "" : ' ' + englishUnits[rem]);
        }
    }

    public String toWords(long number, int wordCase) {
        String s;
        if (number == 0) {
            s = "Zero";
        } else {
            s = toWords(number);
        }
        if (wordCase == UPPER_CASE) {
            return s.toUpperCase();
        } else if (wordCase == LOWER_CASE) {
            return s.toLowerCase();
        } else {
            return s;
        }
    }

    /**
    * Show an ordinal number as English words in a requested case (for example, Twentyfirst)
    */

    public String toOrdinalWords(String ordinalParam, long number, int wordCase) {
        String s;
        if (number >= 1000000000) {
            long rem = number % 1000000000;
            s = toWords(number / 1000000000) + " Billion" +
                    (rem==0 ? "th" : (rem < 100 ? " and " : " ") +
                    toOrdinalWords(ordinalParam, rem, wordCase));
        } else if (number >= 1000000) {
            long rem = number % 1000000;
            s = toWords(number / 1000000) + " Million" +
                    (rem==0 ? "th" : (rem < 100 ? " and " : " ") +
                    toOrdinalWords(ordinalParam, rem, wordCase));
        } else if (number >= 1000) {
            long rem = number % 1000;
            s = toWords(number / 1000) + " Thousand" +
                    (rem==0 ? "th" : (rem < 100 ? " and " : " ") +
                    toOrdinalWords(ordinalParam, rem, wordCase));
        } else if (number >= 100) {
            long rem = number % 100;
            s = toWords(number / 100) + " Hundred" +
                    (rem==0 ? "th" : " and " +
                    toOrdinalWords(ordinalParam, rem, wordCase));
        } else {
            if (number < 20) {
                s = englishOrdinalUnits[(int)number];
            } else {
                int rem = (int)(number % 10);
                if (rem==0) {
                    s = englishOrdinalTens[(int)number / 10];
                } else {
                    s = englishTens[(int)number / 10] + '-' + englishOrdinalUnits[rem];
                }
            }
        }
        if (wordCase == UPPER_CASE) {
            return s.toUpperCase();
        } else if (wordCase == LOWER_CASE) {
            return s.toLowerCase();
        } else {
            return s;
        }
    }

    private static String[] englishUnits = {
        "", "One", "Two", "Three", "Four", "Five", "Six", "Seven", "Eight", "Nine",
        "Ten", "Eleven", "Twelve", "Thirteen", "Fourteen", "Fifteen", "Sixteen",
        "Seventeen", "Eighteen", "Nineteen"};

    private static String[] englishTens = {
        "", "Ten", "Twenty", "Thirty", "Forty", "Fifty",
        "Sixty", "Seventy", "Eighty", "Ninety"};

    private static String[] englishOrdinalUnits = {
        "", "First", "Second", "Third", "Fourth", "Fifth", "Sixth", "Seventh", "Eighth", "Ninth",
        "Tenth", "Eleventh", "Twelfth", "Thirteenth", "Fourteenth", "Fifteenth", "Sixteenth",
        "Seventeenth", "Eighteenth", "Nineteenth"};

    private static String[] englishOrdinalTens = {
        "", "Tenth", "Twentieth", "Thirtieth", "Fortieth", "Fiftieth",
        "Sixtieth", "Seventieth", "Eightieth", "Ninetieth"};

    /**
     * Get a month name or abbreviation
     * @param month The month number (1=January, 12=December)
     * @param minWidth The minimum number of characters
     * @param maxWidth The maximum number of characters
     */

    public String monthName(int month, int minWidth, int maxWidth) {
        String name = englishMonths[month-1];
        if (maxWidth < 3) {
            maxWidth = 3;
        }
        if (name.length() > maxWidth) {
            name = name.substring(0, maxWidth);
        }
        while (name.length() < minWidth) {
            name = name + ' ';
        }
        return name;
    }

    private static String[] englishMonths = {
        "January", "February", "March", "April", "May", "June",
        "July", "August", "September", "October", "November", "December"
    };

    /**
     * Get a day name or abbreviation
     * @param day The day of the week (1=Monday, 7=Sunday)
     * @param minWidth The minimum number of characters
     * @param maxWidth The maximum number of characters
     */

    public String dayName(int day, int minWidth, int maxWidth) {
        String name = englishDays[day-1];
        if (maxWidth < 2) {
            maxWidth = 2;
        }
        if (name.length() > maxWidth) {
            name = englishDayAbbreviations[day-1];
            if (name.length() > maxWidth) {
                name = name.substring(0, maxWidth);
            }
        }
        while (name.length() < minWidth) {
            name = name + ' ';
        }
        if (minWidth==1 && maxWidth==2) {
            // special case
            name = name.substring(0, minUniqueDayLength[day-1]);
        }
        return name;
    }

    private static String[] englishDays = {
        "Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday"
    };

    private static String[] englishDayAbbreviations = {
        "Mon", "Tues", "Weds", "Thurs", "Fri", "Sat", "Sun"
    };

    private static int[] minUniqueDayLength = {
        1, 2, 1, 2, 1, 2, 2
    };

    /**
     * Get an am/pm indicator
     * @param minutes the minutes within the day
     * @param minWidth minimum width of output
     * @param maxWidth maximum width of output
     * @return the AM or PM indicator
     */

    public String halfDayName(int minutes, int minWidth, int maxWidth) {
        String s;
        if (minutes < 12*60) {
            switch (maxWidth) {
                case 1:
                    s = "A";
                    break;
                case 2:
                case 3:
                    s = "Am";
                    break;
                default:
                    s = "A.M.";
            }
        } else {
            switch (maxWidth) {
                case 1:
                    s = "P";
                    break;
                case 2:
                case 3:
                    s = "Pm";
                    break;
                default:
                    s = "P.M.";
            }
        }
        return s;
    }

    /**
     * Get an ordinal suffix for a particular component of a date/time.
     *
     * @param component the component specifier from a format-dateTime picture, for
     *            example "M" for the month or "D" for the day.
     * @return a string that is acceptable in the ordinal attribute of xsl:number
     *         to achieve the required ordinal representation. For example, "-e" for the day component
     *         in German, to have the day represented as "dritte August".
     */

    public String getOrdinalSuffixForDateTime(String component) {
        return "yes";
    }

    /**
     * Get the name for an era (e.g. "BC" or "AD")
     *
     * @param year the proleptic gregorian year, using "0" for the year before 1AD
     */

    public String getEraName(int year) {
        return (year > 0 ? "AD" : "BC");
    }

    /**
     * Get the name of a timezone
     *
     * @param tz the offset of the timezone from GMT in minutes
     */

    public String getTimezoneName(int tz) {
        List list = (List)timezones.get(new Integer(tz));
        if (list == null) {
            FastStringBuffer sb = new FastStringBuffer(10);
            DateTimeValue.appendTimezone(tz, sb);
            return sb.toString();
        } else {
            String c = (country == null ? "us" : country);
            if (list.size() == 1) {
                return ((String[])list.get(0))[0];
            } else {
                for (int i=0; i<list.size(); i++) {
                    String[] entry = (String[])list.get(i);
                    if (entry[1].equals(c)) {
                        return entry[0];
                    }
                }
                return ((String[])list.get(0))[0];
            }
        }
    }

    static HashMap timezones = new HashMap(50);

    static void tz(double offset, String name, String country) {
        Integer key = new Integer((int)(offset*60));
        String[] val = {name, country};
        List list = (List)timezones.get(key);
        if (list == null) {
            list = new ArrayList(3);
        }
        list.add(val);
        timezones.put(key, list);
    }

    static {

        tz(9.5,     "ACST",     "au");              // Australian central standard time
        tz(10,      "AEST",     "au");              // Australian eastern standard time
        tz(-9,      "AKST",     "us");              // Alaska standard time
        tz(8,       "AWST",     "au");              // Australian Western standard time
        tz(1,       "BST",      "gb");              // British Summer Time
        tz(1,       "CET",      "eu");              // Central European Time
        tz(9.5,     "CST",      "au");              // Central Standard Time
        tz(-6,      "CST",      "us");              // Central Standard Time
        tz(7,       "CXT",      "au");              // Christmas Island Time
        tz(2,       "EET",      "eu");              // Eastern European Time
        tz(10,      "EST",      "au");              // Eastern Standard Time
        tz(-5,      "EST",      "us");              // Eastern Standard Time
        tz(0,       "GMT",      "gb");              // Greenwich Mean Time
        tz(-10,     "HAST",     "us");              // Hawaii-Aleutian
        tz(+1,      "MEZ",      "de");              // Mitteleuropaische Zeit
        tz(-7,      "MST",      "us");              // Mountain Standard
        tz(+11.5,   "NFT",      "au");              // Norfolk Island
        tz(-3.5,    "NST",      "us");              // Newfoundland Standard
        tz(-8,      "PST",      "us");              // Pacific Standard
        tz(0,       "UTC",      "eu");              // UTC
        tz(0,       "WET",      "eu");              // Western European
        tz(+8,      "WST",      "au");              // Western Standard

        tz(10.5,    "ACDT",     "au*");             // Australian central daylight time
        tz(-3,      "ADT",      "us*");             // Atlantic Daylight time
        tz(11,      "AEDT",     "au*");             // Australian eastern daylight time
        tz(-8,      "AKDT",     "us*");             // Alaska daylight time
        tz(-4,      "AST",      "us*");             // Atlantic Standard time
        tz(10.5,    "CDT",      "au*");             // Australia Central Daylight Time
        tz(-5,      "CDT",      "us*");             // Central Daylight Time
        tz(2,       "CEDT",     "eu*");             // Central Europe Daylight Time
        tz(11,      "EDT",      "au*");             // Eastern Daylight Time
        tz(-4,      "EDT",      "us*");             // Eastern Daylight Time
        tz(3,       "EEDT",     "eu*");             // Eastern European Daylight Time
        tz(-6,      "MDT",      "us*");             // Mountain Daylight
        tz(+2,      "MESZ",     "de*");             // Mitteleuropaische Sommerzeit
        tz(-2.5,    "NDT",      "us*");             // Newfoundland Daylight
        tz(-7,      "PDT",      "us*");             // Pacific Daylight
        tz(+1,      "WEDT",     "eu*");             // Western European Daylight

    }

    /**
     * Get the name of a calendar
     *
     * @param code The code representing the calendar as in the XSLT 2.0 spec, e.g. AD for the Gregorian calendar
     */

    public String getCalendarName(String code) {
        if (code.equals("AD")) {
            return "Gregorian";
        } else {
            return code;
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
// The Initial Developer of the Original Code is Michael H. Kay.
//
// Portions created by (your name) are Copyright (C) (your legal entity). All Rights Reserved.
//
// Contributor(s): none.
//
