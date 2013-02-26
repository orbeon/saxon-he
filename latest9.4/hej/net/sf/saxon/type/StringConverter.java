package net.sf.saxon.type;

import net.sf.saxon.lib.ConversionRules;
import net.sf.saxon.om.NameChecker;
import net.sf.saxon.om.NamespaceResolver;
import net.sf.saxon.om.QNameException;
import net.sf.saxon.om.StandardNames;
import net.sf.saxon.trans.Err;
import net.sf.saxon.trans.XPathException;
import net.sf.saxon.tree.util.FastStringBuffer;
import net.sf.saxon.value.*;

import java.util.regex.Pattern;

/**
 * A {@link Converter} that accepts a string as input. This subclass of Converter is provided
 * to avoid having to wrap the string into a StringValue prior to conversion. Every Converter whose
 * source type is xs:string must be an instance of this subclass.
 *
 * <p>The input to a StringConverter can also be an xs:untypedAtomic value, since the conversion semantics
 * are always the same as from a string.</p>
 *
 * <p>A StringConverter also provides a method to validate that a string is valid against the target type,
 * without actually performing the conversion.</p>
 */
public abstract class StringConverter extends Converter {

    // Constants are defined only for converters that are independent of the conversion rules

    /*@NotNull*/ public final static StringToString
            STRING_TO_STRING = new StringToString();
    /*@NotNull*/ public final static StringToLanguage
            STRING_TO_LANGUAGE = new StringToLanguage();
    /*@NotNull*/ public final static StringToNormalizedString
            STRING_TO_NORMALIZED_STRING = new StringToNormalizedString();
    /*@NotNull*/ public final static StringToToken
            STRING_TO_TOKEN = new StringToToken();
    /*@NotNull*/ public final static StringToDecimal
            STRING_TO_DECIMAL = new StringToDecimal();
    /*@NotNull*/ public final static StringToInteger
            STRING_TO_INTEGER = new StringToInteger();
    /*@NotNull*/ public final static StringToDuration
            STRING_TO_DURATION = new StringToDuration();
    /*@NotNull*/ public final static StringToDayTimeDuration
            STRING_TO_DAY_TIME_DURATION = new StringToDayTimeDuration();
    /*@NotNull*/ public final static StringToYearMonthDuration
            STRING_TO_YEAR_MONTH_DURATION = new StringToYearMonthDuration();
    /*@NotNull*/ public final static StringToTime
            STRING_TO_TIME = new StringToTime();
    /*@NotNull*/ public final static StringToBoolean
            STRING_TO_BOOLEAN = new StringToBoolean();
    /*@NotNull*/ public final static StringToHexBinary
            STRING_TO_HEX_BINARY = new StringToHexBinary();
    /*@NotNull*/ public final static StringToBase64BinaryConverter
            STRING_TO_BASE64_BINARY = new StringToBase64BinaryConverter();
    /*@NotNull*/ public final static StringToUntypedAtomic
            STRING_TO_UNTYPED_ATOMIC = new StringToUntypedAtomic();

    /**
     * Create a StringConverter
     */

    public StringConverter(){}

    /**
     * Create a StringConverter
     * @param rules the conversion rules to be applied
     */

    public StringConverter(ConversionRules rules) {
        super(rules);
    }

    /**
     * Convert a string to the target type of this converter.
     * @param input the string to be converted
     * @return either an {@link net.sf.saxon.value.AtomicValue} of the appropriate type for this converter (if conversion
     * succeeded), or a {@link ValidationFailure} if conversion failed.
     */

    /*@NotNull*/
    public abstract ConversionResult convertString(/*@NotNull*/ CharSequence input);

    /**
     * Validate a string for conformance to the target type, without actually performing
     * the conversion
     * @param input the string to be validated
     * @return null if validation is successful, or a ValidationFailure indicating the reasons for failure
     * if unsuccessful
     */

    /*@Nullable*/
    public ValidationFailure validate(/*@NotNull*/ CharSequence input) {
        ConversionResult result = convertString(input);
        return (result instanceof ValidationFailure ? (ValidationFailure)result : null);
    }

    /*@NotNull*/
    @Override
    public ConversionResult convert(/*@NotNull*/ AtomicValue input) {
        return convertString(input.getStringValueCS());
    }

    /**
     * Static factory method to get a StringConverter for a specific target type
     * @param targetType the target type of the conversion
     * @param rules the conversion rules in use
     * @return a StringConverter that can be used to convert strings to the target type, or to
     * validate strings against the target type
     */

    /*@NotNull*/ public static StringConverter getStringConverter(/*@NotNull*/ final AtomicType targetType, /*@NotNull*/ final ConversionRules rules) {

        int tt = targetType.getPrimitiveType();
        if (targetType.isBuiltInType()) {
            if (tt == StandardNames.XS_STRING) {
                switch (targetType.getFingerprint()) {
                    case StandardNames.XS_STRING:
                        return STRING_TO_STRING;
                    case StandardNames.XS_NORMALIZED_STRING:
                        return STRING_TO_NORMALIZED_STRING;
                    case StandardNames.XS_TOKEN:
                        return STRING_TO_TOKEN;
                    case StandardNames.XS_LANGUAGE:
                        return STRING_TO_LANGUAGE;
                    case StandardNames.XS_NAME:
                        return new StringToName(rules);
                    case StandardNames.XS_NCNAME:
                    case StandardNames.XS_ID:
                    case StandardNames.XS_IDREF:
                    case StandardNames.XS_ENTITY:
                        return new StringToNCName(rules, targetType);
                    case StandardNames.XS_NMTOKEN:
                        return new StringToNMTOKEN(rules);
                    default:
                        throw new AssertionError("Unknown built-in subtype of xs:string");

                }
            } else if (tt == StandardNames.XS_UNTYPED_ATOMIC) {
                return STRING_TO_UNTYPED_ATOMIC;
            } else if (targetType.isPrimitiveType()) {
                // converter to built-in types unrelated to xs:string
                Converter converter = getConverter(BuiltInAtomicType.STRING, targetType, rules);
                assert converter != null;
                return (StringConverter)converter;
            } else if (tt == StandardNames.XS_INTEGER) {
                return new StringToIntegerSubtype((BuiltInAtomicType)targetType);
            } else {
                switch (targetType.getFingerprint()) {
                    case StandardNames.XS_DAY_TIME_DURATION:
                        return STRING_TO_DAY_TIME_DURATION;
                    case StandardNames.XS_YEAR_MONTH_DURATION:
                        return STRING_TO_YEAR_MONTH_DURATION;
                    case StandardNames.XS_DATE_TIME_STAMP:
                        StringConverter first = new StringToDateTime(rules);
                        DownCastingConverter second = new DownCastingConverter(targetType, rules);
                        return new StringToNonStringDerivedType(first, second);
                    default:
                        throw new AssertionError("Unknown built in type " + targetType.toString());
                }
            }
        } else {
            if (tt == StandardNames.XS_STRING) {
                if (targetType.getBuiltInBaseType() == BuiltInAtomicType.STRING) {
                    // converter to user-defined subtypes of xs:string
                    return new StringToStringSubtype(rules, targetType);
                } else {
                    // converter to user-defined subtypes of built-in subtypes of xs:string
                    return new StringToDerivedStringSubtype(rules, targetType);
                }
            } if(targetType instanceof ExternalObjectType) {
               return new StringToExternalObjectType();
            } else {
                String className = "net.sf.saxon.dotnet.DotNetExternalObjectType";
                Class theClass;
                ClassLoader classLoader = StringConverter.class.getClassLoader();
                try {
                    try{
                        theClass = classLoader.loadClass(className);
                    }catch(Exception ex) {
                        theClass = Class.forName(className);
                    }
                    if(theClass.isInstance(targetType)) {
                        return new StringToExternalObjectType();
                    }
                }
                catch(ClassNotFoundException ex) {}

                // converter to user-defined types derived from types other than xs:string

                StringConverter first = getStringConverter((AtomicType)targetType.getPrimitiveItemType(), rules);
                DownCastingConverter second = new DownCastingConverter(targetType, rules);
                return new StringToNonStringDerivedType(first, second);
            }
        }

//       } else {
//            // converter to built-in types unrelated to xs:string
//            Converter converter = getConverter(BuiltInAtomicType.STRING, targetType, rules);
//            return (StringConverter)converter;
//        }
    }

    /**
      * Converter from string to a derived type (derived from a type other than xs:string),
      * where the derived type needs to retain the original
      * string for validating against lexical facets such as pattern.
      */

     public static class StringToNonStringDerivedType extends StringConverter {
         private StringConverter phaseOne;
         private DownCastingConverter phaseTwo;

         public StringToNonStringDerivedType(StringConverter phaseOne, DownCastingConverter phaseTwo) {
             this.phaseOne = phaseOne;
             this.phaseTwo = phaseTwo;
         }

         @Override
         public void setNamespaceResolver(NamespaceResolver resolver) {
             phaseOne.setNamespaceResolver(resolver);
             phaseTwo.setNamespaceResolver(resolver);
         }

         /*@NotNull*/
         public ConversionResult convert(/*@NotNull*/ AtomicValue input) {
             CharSequence in = input.getStringValueCS();
             try {
                in = phaseTwo.getTargetType().preprocess(in);
             } catch (ValidationException err) {
                 return new ValidationFailure(err);
             }
             ConversionResult temp = phaseOne.convertString(in);
             if (temp instanceof ValidationFailure) {
                 return temp;
             }
             return phaseTwo.convert((AtomicValue) temp, in);
         }

         /*@NotNull*/
         public ConversionResult convertString(/*@NotNull*/ CharSequence input) {
             try {
                input = phaseTwo.getTargetType().preprocess(input);
             } catch (ValidationException err) {
                 return new ValidationFailure(err);
             }
             ConversionResult temp = phaseOne.convertString(input);
             if (temp instanceof ValidationFailure) {
                 return temp;
             }
             return phaseTwo.convert((AtomicValue) temp, input);
         }
     }

    /**
     * Converts from xs:string or xs:untypedAtomic to xs:String
     */

    public static class StringToString extends StringConverter {
        /*@NotNull*/@Override
        public ConversionResult convert(/*@NotNull*/ AtomicValue input) {
            return new StringValue(input.getStringValueCS());
        }
        /*@NotNull*/ public ConversionResult convertString(/*@NotNull*/ CharSequence input) {
            return new StringValue(input);
        }
        /*@Nullable*/ public ValidationFailure validate(/*@NotNull*/ CharSequence input) {
            return null;
        }
        public boolean isAlwaysSuccessful() {
            return true;
        }
    }

/**
     * Converts from xs:string or xs:untypedAtomic to xs:untypedAtomic
     */

    public static class StringToUntypedAtomic extends StringConverter {
        /*@NotNull*/@Override
        public ConversionResult convert(/*@NotNull*/ AtomicValue input) {
            return new UntypedAtomicValue(input.getStringValueCS());
        }
        /*@NotNull*/ public ConversionResult convertString(/*@NotNull*/ CharSequence input) {
            return new UntypedAtomicValue(input);
        }
        /*@Nullable*/ public ValidationFailure validate(/*@NotNull*/ CharSequence input) {
            return null;
        }
        public boolean isAlwaysSuccessful() {
            return true;
        }
    }


    /**
     * Converts from xs:string to xs:normalizedString
     */

    public static class StringToNormalizedString extends StringConverter {
        /*@NotNull*/ public ConversionResult convertString(/*@NotNull*/ CharSequence input) {
            return new StringValue(Whitespace.normalizeWhitespace(input), BuiltInAtomicType.NORMALIZED_STRING);
        }
        /*@Nullable*/ public ValidationFailure validate(/*@NotNull*/ CharSequence input) {
            return null;
        }
        public boolean isAlwaysSuccessful() {
            return true;
        }
    }

    /**
     * Converts from xs:string to xs:token
     */

    public static class StringToToken extends StringConverter {
        /*@NotNull*/ public ConversionResult convertString(/*@NotNull*/ CharSequence input) {
            return new StringValue(Whitespace.collapseWhitespace(input), BuiltInAtomicType.TOKEN);
        }
        /*@Nullable*/ public ValidationFailure validate(/*@NotNull*/ CharSequence input) {
            return null;
        }
        public boolean isAlwaysSuccessful() {
            return true;
        }
    }

    /**
     * Converts from xs:string to xs:language
     */

    public static class StringToLanguage extends StringConverter {
        private final static Pattern regex = Pattern.compile("[a-zA-Z]{1,8}(-[a-zA-Z0-9]{1,8})*");
                                                // See erratum E2-25 to XML Schema Part 2.

        /*@NotNull*/ public ConversionResult convertString(/*@NotNull*/ CharSequence input) {
            CharSequence trimmed = Whitespace.trimWhitespace(input);
            if (!regex.matcher(trimmed).matches()) {
                return new ValidationFailure("The value '" + input + "' is not a valid xs:language");
            }
            return new StringValue(trimmed, BuiltInAtomicType.LANGUAGE);
        }
        /*@Nullable*/ public ValidationFailure validate(/*@NotNull*/ CharSequence input) {
            if (regex.matcher(Whitespace.trimWhitespace(input)).matches()) {
                return null;
            } else {
                return new ValidationFailure("The value '" + input + "' is not a valid xs:language");
            }
        }
    }

    /**
     * Converts from xs:string to xs:NCName, xs:ID, xs:IDREF, or xs:ENTITY
     */

    public static class StringToNCName extends StringConverter {
        NameChecker checker;
        AtomicType targetType;

        public StringToNCName(/*@NotNull*/ ConversionRules rules, AtomicType targetType) {
            super(rules);
            checker = rules.getNameChecker();
            this.targetType = targetType;
        }

        /*@NotNull*/
        public ConversionResult convertString(/*@NotNull*/ CharSequence input) {
            CharSequence trimmed = Whitespace.trimWhitespace(input);
            if (checker.isValidNCName(trimmed)) {
                return new StringValue(trimmed, targetType);
            } else {
                return new ValidationFailure("The value '" + input + "' is not a valid xs:NCName");
            }
        }
        /*@Nullable*/ public ValidationFailure validate(/*@NotNull*/ CharSequence input) {
            if (checker.isValidNCName(Whitespace.trimWhitespace(input))) {
                return null;
            } else {
                return new ValidationFailure("The value '" + input + "' is not a valid xs:NCName");
            }
        }
    }

    /**
     * Converts from xs:string to xs:NMTOKEN
     */

    public static class StringToNMTOKEN extends StringConverter {
        NameChecker checker;

        public StringToNMTOKEN(/*@NotNull*/ ConversionRules rules) {
            super(rules);
            checker = rules.getNameChecker();
        }

        /*@NotNull*/ public ConversionResult convertString(/*@NotNull*/ CharSequence input) {
            CharSequence trimmed = Whitespace.trimWhitespace(input);
            if (checker.isValidNmtoken(trimmed)) {
                return new StringValue(trimmed, BuiltInAtomicType.NMTOKEN);
            } else {
                return new ValidationFailure("The value '" + input + "' is not a valid xs:NMTOKEN");
            }
        }
        /*@Nullable*/ public ValidationFailure validate(/*@NotNull*/ CharSequence input) {
            if (checker.isValidNmtoken(Whitespace.trimWhitespace(input))) {
                return null;
            } else {
                return new ValidationFailure("The value '" + input + "' is not a valid xs:NMTOKEN");
            }
        }
    }


    /**
     * Converts from xs:string to xs:Name
     */

    public static class StringToName extends StringToNCName {

        public StringToName(/*@NotNull*/ ConversionRules rules) {
            super(rules, BuiltInAtomicType.NAME);
        }

        /*@NotNull*/
        public ConversionResult convertString(/*@NotNull*/ CharSequence input) {
            ValidationFailure vf = validate(input);
            if (vf == null) {
                return new StringValue(Whitespace.trimWhitespace(input), BuiltInAtomicType.NAME);
            } else {
                return vf;
            }
        }
        /*@Nullable*/ public ValidationFailure validate(/*@NotNull*/ CharSequence input) {
            // if it's valid as an NCName then it's OK
            CharSequence trimmed = Whitespace.trimWhitespace(input);
            if (checker.isValidNCName(trimmed)) {
                return null;
            }

            // if not, replace any colons by underscores and then test if it's a valid NCName
            FastStringBuffer buff = new FastStringBuffer(trimmed.length());
            buff.append(trimmed);
            for (int i = 0; i < buff.length(); i++) {
                if (buff.charAt(i) == ':') {
                    buff.setCharAt(i, '_');
                }
            }
            if (checker.isValidNCName(buff)) {
                return null;
            } else {
                return new ValidationFailure("The value '" + trimmed + "' is not a valid xs:Name");
            }
        }
    }

    /**
     * Converts from xs;string to a user-defined type derived directly from xs:string
     */

    public static class StringToStringSubtype extends StringConverter {
        AtomicType targetType;
        int whitespaceAction;

        public StringToStringSubtype(ConversionRules rules, /*@NotNull*/ AtomicType targetType) {
            super(rules);
            this.targetType = targetType;
            this.whitespaceAction = targetType.getWhitespaceAction();
        }
        /*@NotNull*/
        public ConversionResult convertString(/*@NotNull*/ CharSequence input) {
            CharSequence cs = Whitespace.applyWhitespaceNormalization(whitespaceAction, input);
            try {
                cs = targetType.preprocess(cs);
            } catch (ValidationException err) {
                return new ValidationFailure(err);
            }
            StringValue sv = new StringValue(cs);
            ValidationFailure f = targetType.validate(sv, cs, getConversionRules());
            if (f == null) {
                sv.setTypeLabel(targetType);
                return sv;
            } else {
                return f;
            }
        }

        public ValidationFailure validate(/*@NotNull*/ CharSequence input) {
            CharSequence cs = Whitespace.applyWhitespaceNormalization(whitespaceAction, input);
            return targetType.validate(new StringValue(cs), cs, getConversionRules());
        }
    }

    /**
     * Converts from xs;string to a user-defined type derived from a built-in subtype of xs:string
     */

    public static class StringToDerivedStringSubtype extends StringConverter {
        AtomicType targetType;
        StringConverter builtInValidator;
        int whitespaceAction;

        public StringToDerivedStringSubtype(/*@NotNull*/ ConversionRules rules, /*@NotNull*/ AtomicType targetType) {
            super(rules);
            this.targetType = targetType;
            this.whitespaceAction = targetType.getWhitespaceAction();
            builtInValidator = getStringConverter((AtomicType)targetType.getBuiltInBaseType(), rules);
        }
        /*@NotNull*/
        public ConversionResult convertString(/*@NotNull*/ CharSequence input) {
            CharSequence cs = Whitespace.applyWhitespaceNormalization(whitespaceAction, input);
            try {
                cs = targetType.preprocess(cs);
            } catch (ValidationException err) {
                return new ValidationFailure(err);
            }
            ValidationFailure f = builtInValidator.validate(cs);
            if (f != null) {
                return f;
            }
            StringValue sv = new StringValue(cs);
            f = targetType.validate(sv, cs, getConversionRules());
            if (f == null) {
                sv.setTypeLabel(targetType);
                return sv;
            } else {
                return f;
            }
        }
    }


    /**
     * Converts a string to xs:float
     */

    public static class StringToFloat extends StringConverter {
        public StringToFloat(ConversionRules rules) {
            super(rules);
        }

       /*@NotNull*/ public ConversionResult convertString(/*@NotNull*/ CharSequence input) {
            try {
                float flt = (float) getConversionRules().getStringToDoubleConverter().stringToNumber(input);
                return new FloatValue(flt);
            } catch (NumberFormatException err) {
                ValidationFailure ve = new ValidationFailure("Cannot convert string to float: " + input.toString());
                ve.setErrorCode("FORG0001");
                return ve;
            }
        }
    }

    /**
     * Converts a string to a double. The rules change in XSD 1.1 to permit "+INF"
     */

    public static class StringToDouble extends StringConverter {
        net.sf.saxon.type.StringToDouble worker;
        public StringToDouble(/*@NotNull*/ ConversionRules rules) {
            super(rules);
            worker = rules.getStringToDoubleConverter();
        }

       /*@NotNull*/ public ConversionResult convertString(/*@NotNull*/ CharSequence input) {
            try {
                double dble = worker.stringToNumber(input);
                return new DoubleValue(dble);
            } catch (NumberFormatException err) {
                return new ValidationFailure("Cannot convert string to double: " + Err.wrap(input.toString(), Err.VALUE));
            }
        }
    }

    /**
     * Converts a string to an xs:decimal
     */

    public static class StringToDecimal extends StringConverter {
        /*@NotNull*/
        public ConversionResult convertString(/*@NotNull*/ CharSequence input) {
            return DecimalValue.makeDecimalValue(input, true);
        }

        /*@Nullable*/ @Override
        public ValidationFailure validate(/*@NotNull*/ CharSequence input) {
            if (DecimalValue.castableAsDecimal(input)) {
                return null;
            } else {
                return new ValidationFailure("Cannot convert string to decimal: " + input.toString());
            }
        }
    }

    /**
     * Converts a string to an integer
     */

    public static class StringToInteger extends StringConverter {
        /*@NotNull*/
        public ConversionResult convert(/*@NotNull*/ AtomicValue input) {
            return IntegerValue.stringToInteger(input.getStringValueCS());
        }

        /*@NotNull*/
        public ConversionResult convertString(/*@NotNull*/ CharSequence input) {
            return IntegerValue.stringToInteger(input);
        }

        @Override
        public ValidationFailure validate(/*@NotNull*/ CharSequence input) {
            return IntegerValue.castableAsInteger(input);
        }
    }

    /**
     * Converts a string to a built-in subtype of integer
     */

    public static class StringToIntegerSubtype extends StringConverter {

        BuiltInAtomicType targetType;

        public StringToIntegerSubtype(BuiltInAtomicType targetType) {
            this.targetType = targetType;
        }

        /*@NotNull*/
        public ConversionResult convertString(/*@NotNull*/ CharSequence input) {
            ConversionResult iv = IntegerValue.stringToInteger(input);
            if (iv instanceof Int64Value) {
                boolean ok = IntegerValue.checkRange(((Int64Value)iv).longValue(), targetType);
                if (ok) {
                    return ((Int64Value)iv).copyAsSubType(targetType);
                } else {
                    return new ValidationFailure("Integer value is out of range for type " + targetType.toString());
                }
            } else if (iv instanceof BigIntegerValue) {
                boolean ok = IntegerValue.checkBigRange(((BigIntegerValue)iv).asBigInteger(), targetType);
                if (ok) {
                    ((BigIntegerValue)iv).setTypeLabel(targetType);
                    return iv;
                } else {
                    return new ValidationFailure("Integer value is out of range for type " + targetType.toString());
                }
            } else {
                assert (iv instanceof ValidationFailure);
                return iv;
            }
        }
    }

    /**
     * Converts a string to a duration
     */

    public static class StringToDuration extends StringConverter {
       /*@NotNull*/
       public ConversionResult convertString(/*@NotNull*/ CharSequence input) {
            return DurationValue.makeDuration(input);
        }
    }

    /**
     * Converts a string to a dayTimeDuration
     */


    public static class StringToDayTimeDuration extends StringConverter {
        /*@NotNull*/
        public ConversionResult convertString(/*@NotNull*/ CharSequence input) {
            return DayTimeDurationValue.makeDayTimeDurationValue(input);
        }
    }

    /**
     * Converts a string to a yearMonthDuration
     */

    public static class StringToYearMonthDuration extends StringConverter {
        /*@NotNull*/
        public ConversionResult convertString(/*@NotNull*/ CharSequence input) {
            return YearMonthDurationValue.makeYearMonthDurationValue(input);
        }
    }

    /**
     * Converts a string to a dateTime
     */

    public static class StringToDateTime extends StringConverter {
        public StringToDateTime(ConversionRules rules) {
            super(rules);
        }

        /*@NotNull*/
        public ConversionResult convertString(/*@NotNull*/ CharSequence input) {
            return DateTimeValue.makeDateTimeValue(input, getConversionRules());
        }
    }

    /**
     * Converts a string to a date
     */

    public static class StringToDate extends StringConverter{
        public StringToDate(ConversionRules rules) {
            super(rules);
        }

        /*@NotNull*/
        public ConversionResult convertString(/*@NotNull*/ CharSequence input) {
            return DateValue.makeDateValue(input, getConversionRules());
        }
    }

    /**
     * Converts a string to a gMonth
     */

    public static class StringToGMonth extends StringConverter {
        public StringToGMonth(ConversionRules rules) {
            super(rules);
        }

       /*@NotNull*/
       public ConversionResult convertString(/*@NotNull*/ CharSequence input) {
            return GMonthValue.makeGMonthValue(input, getConversionRules());
        }
    }

    /**
     * Converts a string to a gYearMonth
     */

    public static class StringToGYearMonth extends StringConverter {
        public StringToGYearMonth(ConversionRules rules) {
            super(rules);
        }

        /*@NotNull*/
        public ConversionResult convertString(/*@NotNull*/ CharSequence input) {
            return GYearMonthValue.makeGYearMonthValue(input, getConversionRules());
        }
    }

    /**
     * Converts a string to a gYear
     */

    public static class StringToGYear extends StringConverter {
        public StringToGYear(ConversionRules rules) {
            super(rules);
        }

        /*@NotNull*/
        public ConversionResult convertString(/*@NotNull*/ CharSequence input) {
            return GYearValue.makeGYearValue(input, getConversionRules());
        }
    }

    /**
     * Converts a string to a gMonthDay
     */

    public static class StringToGMonthDay extends StringConverter {
        public StringToGMonthDay(ConversionRules rules) {
            super(rules);
        }

        /*@NotNull*/
        public ConversionResult convertString(/*@NotNull*/ CharSequence input) {
            return GMonthDayValue.makeGMonthDayValue(input, getConversionRules());
        }
    }

    /**
     * Converts a string to a gDay
     */

    public static class StringToGDayConverter extends StringConverter {
        public StringToGDayConverter(ConversionRules rules) {
            super(rules);
        }

        /*@NotNull*/
        public ConversionResult convertString(/*@NotNull*/ CharSequence input) {
            return GDayValue.makeGDayValue(input, getConversionRules());
        }
    }

    /**
     * Converts a string to a time
     */

    public static class StringToTime extends StringConverter {
        /*@NotNull*/
        public ConversionResult convertString(/*@NotNull*/ CharSequence input) {
            return TimeValue.makeTimeValue(input);
        }
    }

    /**
     * Converts a string to a boolean
     */

    public static class StringToBoolean extends StringConverter {
        /*@NotNull*/
        public ConversionResult convertString(/*@NotNull*/ CharSequence input) {
            return BooleanValue.fromString(input);
        }
    }

    /**
     * Converts a string to hexBinary
     */

    public static class StringToHexBinary extends StringConverter {
        /*@NotNull*/ public ConversionResult convertString(/*@NotNull*/ CharSequence input) {
            try {
                return new HexBinaryValue(input);
            } catch (XPathException e) {
                return new ValidationFailure(e);
            }
        }
    }

    /**
     * Converts String to QName
     */

    public static class StringToQName extends StringConverter {

        NamespaceResolver nsResolver;

        public StringToQName(ConversionRules rules) {
            super(rules);
        }

        @Override
        public boolean isXPath30Conversion() {
            return true;
        }

        public void setNamespaceResolver(NamespaceResolver resolver) {
            this.nsResolver = resolver;
        }

        @Override
        public NamespaceResolver getNamespaceResolver() {
            return nsResolver;
        }

        /*@NotNull*/ public ConversionResult convertString(/*@NotNull*/ CharSequence input) {
            if (nsResolver == null) {
                throw new UnsupportedOperationException("Cannot validate a QName without a namespace resolver");
            }
            try {
                NameChecker nameChecker = getConversionRules().getNameChecker();
                String[] parts = nameChecker.getQNameParts(Whitespace.trimWhitespace(input));
                String uri = nsResolver.getURIForPrefix(parts[0], true);
                if (uri == null) {
                    return new ValidationFailure("Namespace prefix " + Err.wrap(parts[0]) + " has not been declared");
                }
//                if (fingerprint == StandardNames.XS_NOTATION) {
//                    // This check added in 9.3. The XSLT spec says that this check should not be performed during
//                    // validation. However, this appears to be based on an incorrect assumption: see spec bug 6952
//                    if (!rules.isDeclaredNotation(uri, parts[1])) {
//                        return new ValidationFailure("Notation {" + uri + "}" + parts[1] + " is not declared in the schema");
//                    }
//                }
                return new QNameValue(parts[0], uri, parts[1], BuiltInAtomicType.QNAME, nameChecker);
            } catch (QNameException err) {
                return new ValidationFailure("Invalid lexical QName " + Err.wrap(input));
            } catch (XPathException err) {
                return new ValidationFailure(err.getMessage());
            }
        }
    }

    /**
     * Converts String to NOTATION
     */

    public static class StringToNotation extends StringConverter {

        NamespaceResolver nsResolver;

        public StringToNotation(ConversionRules rules) {
            super(rules);
        }

        @Override
        public void setNamespaceResolver(NamespaceResolver resolver) {
            nsResolver = resolver;
        }

        @Override
        public NamespaceResolver getNamespaceResolver() {
            return nsResolver;
        }

        /*@NotNull*/ public ConversionResult convertString(/*@NotNull*/ CharSequence input) {
            if (getNamespaceResolver() == null) {
                throw new UnsupportedOperationException("Cannot validate a NOTATION without a namespace resolver");
            }
            try {
                NameChecker nameChecker = getConversionRules().getNameChecker();
                String[] parts = nameChecker.getQNameParts(Whitespace.trimWhitespace(input));
                String uri = getNamespaceResolver().getURIForPrefix(parts[0], true);
                if (uri == null) {
                    return new ValidationFailure("Namespace prefix " + Err.wrap(parts[0]) + " has not been declared");
                }
                // This check added in 9.3. The XSLT spec says that this check should not be performed during
                // validation. However, this appears to be based on an incorrect assumption: see spec bug 6952
                if (!getConversionRules().isDeclaredNotation(uri, parts[1])) {
                    return new ValidationFailure("Notation {" + uri + "}" + parts[1] + " is not declared in the schema");
                }
                return new NotationValue(parts[0], uri, parts[1], nameChecker);
            } catch (QNameException err) {
                return new ValidationFailure("Invalid lexical QName " + Err.wrap(input));
            } catch (XPathException err) {
                return new ValidationFailure(err.getMessage());
            }
        }
    }

    /**
     * Converts string to anyURI
     */

    public static class StringToAnyURI extends StringConverter {
        public StringToAnyURI(ConversionRules rules) {
            super(rules);
        }

       /*@NotNull*/ public ConversionResult convertString(/*@NotNull*/ CharSequence input) {
            if (getConversionRules().isValidURI(input)) {
                return new AnyURIValue(input);
            } else {
                return new ValidationFailure("Invalid URI: " + input.toString());
            }
        }

        /*@Nullable*/@Override
        public ValidationFailure validate(/*@NotNull*/ CharSequence input) {
            if (getConversionRules().isValidURI(input)) {
                return null;
            } else {
                return new ValidationFailure("Invalid URI: " + input.toString());
            }
        }
    }

    /**
     * Converter that does nothing - it returns the input unchanged
     */

    public static class IdentityConverter extends StringConverter {
        /*@NotNull*/ public static IdentityConverter THE_INSTANCE = new IdentityConverter();

        /*@NotNull*/
        public ConversionResult convert(/*@NotNull*/ AtomicValue input) {
            return input;
        }

        /*@NotNull*/
        public ConversionResult convertString(/*@NotNull*/ CharSequence input) {
            return StringValue.makeStringValue(input);
        }

        public boolean isAlwaysSuccessful() {
            return true;
        }

        /*@Nullable*/ public ValidationFailure validate(/*@NotNull*/ CharSequence input) {
            return null;
        }
    }

    /**
     * Converter from string to plain union types
     */

    public static class StringToUnionConverter extends StringConverter {

        SimpleType targetType;
        ConversionRules rules;

        public StringToUnionConverter(PlainType targetType, ConversionRules rules) {
            if (!targetType.isPlainType()) {
                throw new IllegalArgumentException();
            }
            if (((SimpleType)targetType).isNamespaceSensitive()) {
                throw new IllegalArgumentException();
            }
            this.targetType = (SimpleType)targetType;
            this.rules = rules;
        }
        /**
         * Convert a string to the target type of this converter.
         *
         * @param input the string to be converted
         * @return either an {@link net.sf.saxon.value.AtomicValue} of the appropriate type for this converter (if conversion
         *         succeeded), or a {@link net.sf.saxon.type.ValidationFailure} if conversion failed.
         */
        /*@NotNull*/
        @Override
        public ConversionResult convertString(/*@NotNull*/ CharSequence input) {
            try {
                return targetType.getTypedValue(input, null, rules).next();
            } catch (XPathException err) {
                return new ValidationFailure(ValidationException.makeXPathException(err));
            }
        }
    }

    public static class StringToExternalObjectType extends StringConverter {


        @Override
        public ConversionResult convertString(CharSequence input) {
            return new ValidationFailure("Cannot convert string to external object");
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
// The Original Code is: all this file
//
// The Initial Developer of the Original Code is Saxonica Limited.
// Portions created by ___ are Copyright (C) ___. All rights reserved.
//
// Contributor(s):
//