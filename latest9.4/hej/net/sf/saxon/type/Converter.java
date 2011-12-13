package net.sf.saxon.type;

import net.sf.saxon.lib.ConversionRules;
import net.sf.saxon.om.NamespaceResolver;
import net.sf.saxon.om.StandardNames;
import net.sf.saxon.trans.XPathException;
import net.sf.saxon.value.*;

import java.math.BigDecimal;

/**
 * A converter implements conversions from one atomic type to another - that is, it implements the casting
 * rules for a (source type, destination type) pair.
 * <p/>
 * <p>There is potentially one Converter implementation for each pair of (source, target) classes; though in many
 * cases the same implementation handles a number of such pairs.</p>
 * <p/>
 * <p>In cases where the conversion rules are fixed (specifically, where they do not depend on differences between
 * versions of the XSD or QT specifications), the appropriate Converter can be obtained as a static constant, for example
 * {@link #BOOLEAN_TO_DOUBLE}. In other cases the converter is paramaterized by the {@link ConversionRules} object,
 * and should be obtained by calling the appropriate factory method on the ConversionRules.</p>
 * <p/>
 * <p>Where the source type of the conversion is xs:string, the converter will always be a subclass of
 * {@link StringConverter}</p>
 */
public abstract class Converter {
    /*@NotNull*/ public final static StringConverter.IdentityConverter
            IDENTITY_CONVERTER = new StringConverter.IdentityConverter();
    /*@NotNull*/ public final static ToUntypedAtomicConverter
            TO_UNTYPED_ATOMIC = new ToUntypedAtomicConverter();
    /*@NotNull*/ public final static ToStringConverter
            TO_STRING = new ToStringConverter();
    /*@NotNull*/ public final static NumericToFloat
            NUMERIC_TO_FLOAT = new NumericToFloat();
    /*@NotNull*/ public final static BooleanToFloat
            BOOLEAN_TO_FLOAT = new BooleanToFloat();
    /*@NotNull*/ public final static NumericToDouble
            NUMERIC_TO_DOUBLE = new NumericToDouble();
    /*@NotNull*/ public final static BooleanToDouble
            BOOLEAN_TO_DOUBLE = new BooleanToDouble();
    /*@NotNull*/ public final static DoubleToDecimal
            DOUBLE_TO_DECIMAL = new DoubleToDecimal();
    /*@NotNull*/ public final static FloatToDecimal
            FLOAT_TO_DECIMAL = new FloatToDecimal();
    /*@NotNull*/ public final static IntegerToDecimal
            INTEGER_TO_DECIMAL = new IntegerToDecimal();
    /*@NotNull*/ public final static NumericToDecimal
            NUMERIC_TO_DECIMAL = new NumericToDecimal();
    /*@NotNull*/ public final static BooleanToDecimal
            BOOLEAN_TO_DECIMAL = new BooleanToDecimal();
    /*@NotNull*/ public final static DoubleToInteger
            DOUBLE_TO_INTEGER = new DoubleToInteger();
    /*@NotNull*/ public final static FloatToInteger
            FLOAT_TO_INTEGER = new FloatToInteger();
    /*@NotNull*/ public final static DecimalToInteger
            DECIMAL_TO_INTEGER = new DecimalToInteger();
    /*@NotNull*/ public final static NumericToInteger
            NUMERIC_TO_INTEGER = new NumericToInteger();
    /*@NotNull*/ public final static BooleanToInteger
            BOOLEAN_TO_INTEGER = new BooleanToInteger();
    /*@NotNull*/ public final static DurationToDayTimeDuration
            DURATION_TO_DAY_TIME_DURATION = new DurationToDayTimeDuration();
    /*@NotNull*/ public final static DurationToYearMonthDuration
            DURATION_TO_YEAR_MONTH_DURATION = new DurationToYearMonthDuration();
    /*@NotNull*/ public final static DateToDateTime
            DATE_TO_DATE_TIME = new DateToDateTime();
    /*@NotNull*/ public final static DateTimeToDate
            DATE_TIME_TO_DATE = new DateTimeToDate();
    /*@NotNull*/ public final static DateTimeToGMonth
            DATE_TIME_TO_G_MONTH = new DateTimeToGMonth();
    /*@NotNull*/ public final static DateTimeToGYearMonth
            DATE_TIME_TO_G_YEAR_MONTH = new DateTimeToGYearMonth();
    /*@NotNull*/ public final static DateTimeToGYear
            DATE_TIME_TO_G_YEAR = new DateTimeToGYear();
    /*@NotNull*/ public final static DateTimeToGMonthDay
            DATE_TIME_TO_G_MONTH_DAY = new DateTimeToGMonthDay();
    /*@NotNull*/ public final static DateTimeToGDay
            DATE_TIME_TO_G_DAY = new DateTimeToGDay();
    /*@NotNull*/ public final static DateTimeToTime
            DATE_TIME_TO_TIME = new DateTimeToTime();
    /*@NotNull*/ public final static NumericToBoolean
            NUMERIC_TO_BOOLEAN = new NumericToBoolean();
    /*@NotNull*/ public final static Base64BinaryToHexBinary
            BASE64_BINARY_TO_HEX_BINARY = new Base64BinaryToHexBinary();
    /*@NotNull*/ public final static HexBinaryToBase64Binary
            HEX_BINARY_TO_BASE64_BINARY = new HexBinaryToBase64Binary();
    /*@NotNull*/ public final static NotationToQName
            NOTATION_TO_QNAME = new NotationToQName();
    /*@NotNull*/ public final static QNameToNotation
            QNAME_TO_NOTATION = new QNameToNotation();


    /**
     * Convenience method to convert a given value to a given type. Note: it is more efficient
     * to obtain a converter in advance and to reuse it for multiple conversions
     * @param value the value to be converted
     * @param targetType the type to which the value is to be converted
     * @param rules the conversion rules for the configuration
     * @return the converted value
     * @throws ValidationException if conversion fails
     */

    public static AtomicValue convert(/*@NotNull*/ AtomicValue value, /*@NotNull*/ AtomicType targetType, /*@NotNull*/ ConversionRules rules)
            throws ValidationException {
        Converter converter = rules.getConverter(value.getPrimitiveType(), targetType);
        if (converter == null) {
            ValidationException ve = new ValidationException("Cannot convert value from " + value.getPrimitiveType() + " to " + targetType);
            ve.setErrorCode("FORG0001");
            throw ve;
        }
        return converter.convert(value).asAtomic();
    }

    /**
     * Get a converter that handles conversion from one primitive type to another.
     * <p/>
     * <p>This method is intended for internal use only. The approved way to get a converter is using the
     * factory method {@link ConversionRules#getConverter(net.sf.saxon.type.AtomicType, net.sf.saxon.type.AtomicType)}}</p>
     *
     * @param sourceType the fingerprint of the source primitive type
     * @param targetType the fingerprint of the target primitive type
     * @param rules      the conversion rules to be applied
     * @return the converter if one is available; or null otherwise
     */

    /*@Nullable*/ public static Converter getConverter(/*@NotNull*/ AtomicType sourceType, /*@NotNull*/ AtomicType targetType, ConversionRules rules) {
        if (sourceType == targetType) {
            return StringConverter.IdentityConverter.THE_INSTANCE;
        }

        int tt = targetType.getFingerprint();
        int tp = targetType.getPrimitiveType();
        int st = sourceType.getPrimitiveType();

        if ((st == StandardNames.XS_STRING || st == StandardNames.XS_UNTYPED_ATOMIC) &&
                (tp == StandardNames.XS_STRING || tp == StandardNames.XS_UNTYPED_ATOMIC)) {
            return StringConverter.getStringConverter(targetType, rules);
        }

        if (!(targetType.isPrimitiveType())) {
            AtomicType primTarget = (AtomicType) targetType.getPrimitiveItemType();
            if (sourceType == primTarget) {
                return new DownCastingConverter(targetType, rules);
            } else if (st == StandardNames.XS_STRING || st == StandardNames.XS_UNTYPED_ATOMIC) {
                return StringConverter.getStringConverter(targetType, rules);
            } else {
                Converter stageOne = getConverter(sourceType, primTarget, rules);
                if (stageOne == null) {
                    return null;
                }
                Converter stageTwo = new DownCastingConverter(targetType, rules);
                return new TwoPhaseConverter(stageOne, stageTwo);
            }
        }


        if (st == tt) {
            // we are casting between subtypes of the same primitive type.
            // TODO: (optimization) check whether the targetType is a supertype of the source type, in which case
            // it's a simple upcast. (Unfortunately, we don't have the TypeHierarchy available)
            Converter upcast = new UpCastingConverter((AtomicType) sourceType.getPrimitiveItemType());
            Converter downcast = new DownCastingConverter(targetType, rules);
            return new TwoPhaseConverter(upcast, downcast);
        }

        switch (tt) {
            case StandardNames.XS_UNTYPED_ATOMIC:
                return TO_UNTYPED_ATOMIC;
            case StandardNames.XS_STRING:
                return TO_STRING;
            case StandardNames.XS_FLOAT:
                switch (st) {
                    case StandardNames.XS_UNTYPED_ATOMIC:
                    case StandardNames.XS_STRING:
                        return new StringConverter.StringToFloat(rules);
                    case StandardNames.XS_DOUBLE:
                    case StandardNames.XS_DECIMAL:
                    //case StandardNames.XS_PRECISION_DECIMAL:
                    case StandardNames.XS_INTEGER:
                    case StandardNames.XS_NUMERIC:
                        return NUMERIC_TO_FLOAT;
                    case StandardNames.XS_BOOLEAN:
                        return BOOLEAN_TO_FLOAT;
                    default:
                        return null;
                }
            case StandardNames.XS_DOUBLE:
            case StandardNames.XS_NUMERIC:
                switch (st) {
                    case StandardNames.XS_UNTYPED_ATOMIC:
                    case StandardNames.XS_STRING:
                        return new StringConverter.StringToDouble(rules);
                    case StandardNames.XS_FLOAT:
                    case StandardNames.XS_DECIMAL:
                    //case StandardNames.XS_PRECISION_DECIMAL:
                    case StandardNames.XS_INTEGER:
                    case StandardNames.XS_NUMERIC:
                        return NUMERIC_TO_DOUBLE;
                    case StandardNames.XS_BOOLEAN:
                        return BOOLEAN_TO_DOUBLE;
                    default:
                        return null;
                }
            case StandardNames.XS_DECIMAL:
                switch (st) {
                    case StandardNames.XS_UNTYPED_ATOMIC:
                    case StandardNames.XS_STRING:
                        return StringConverter.STRING_TO_DECIMAL;
                    case StandardNames.XS_FLOAT:
                        return FLOAT_TO_DECIMAL;
                    case StandardNames.XS_DOUBLE:
                        return DOUBLE_TO_DECIMAL;
//                    case StandardNames.XS_PRECISION_DECIMAL:
//                        return PRECISION_DECIMAL_TO_DECIMAL;
                    case StandardNames.XS_INTEGER:
                        return INTEGER_TO_DECIMAL;
                    case StandardNames.XS_NUMERIC:
                        return NUMERIC_TO_DECIMAL;
                    case StandardNames.XS_BOOLEAN:
                        return BOOLEAN_TO_DECIMAL;
                    default:
                        return null;
                }

            case StandardNames.XS_INTEGER:
                switch (st) {
                    case StandardNames.XS_UNTYPED_ATOMIC:
                    case StandardNames.XS_STRING:
                        return StringConverter.STRING_TO_INTEGER;
                    case StandardNames.XS_FLOAT:
                        return FLOAT_TO_INTEGER;
                    case StandardNames.XS_DOUBLE:
                        return DOUBLE_TO_INTEGER;
                    case StandardNames.XS_DECIMAL:
                    //case StandardNames.XS_PRECISION_DECIMAL:
                        return DECIMAL_TO_INTEGER;
                    case StandardNames.XS_NUMERIC:
                        return NUMERIC_TO_INTEGER;
                    case StandardNames.XS_BOOLEAN:
                        return BOOLEAN_TO_INTEGER;
                    default:
                        return null;
                }
            case StandardNames.XS_DURATION:
                switch (st) {
                    case StandardNames.XS_UNTYPED_ATOMIC:
                    case StandardNames.XS_STRING:
                        return StringConverter.STRING_TO_DURATION;
                    case StandardNames.XS_DAY_TIME_DURATION:
                    case StandardNames.XS_YEAR_MONTH_DURATION:
                        return new UpCastingConverter(BuiltInAtomicType.DURATION);
                    default:
                        return null;
                }
            case StandardNames.XS_YEAR_MONTH_DURATION:
                switch (st) {
                    case StandardNames.XS_UNTYPED_ATOMIC:
                    case StandardNames.XS_STRING:
                        return StringConverter.STRING_TO_YEAR_MONTH_DURATION;
                    case StandardNames.XS_DURATION:
                    case StandardNames.XS_DAY_TIME_DURATION:
                        return DURATION_TO_YEAR_MONTH_DURATION;
                    default:
                        return null;
                }
            case StandardNames.XS_DAY_TIME_DURATION:
                switch (st) {
                    case StandardNames.XS_UNTYPED_ATOMIC:
                    case StandardNames.XS_STRING:
                        return StringConverter.STRING_TO_DAY_TIME_DURATION;
                    case StandardNames.XS_DURATION:
                    case StandardNames.XS_YEAR_MONTH_DURATION:
                        return DURATION_TO_DAY_TIME_DURATION;
                    default:
                        return null;
                }
            case StandardNames.XS_DATE_TIME:
                switch (st) {
                    case StandardNames.XS_UNTYPED_ATOMIC:
                    case StandardNames.XS_STRING:
                        return new StringConverter.StringToDateTime(rules);
                    case StandardNames.XS_DATE:
                        return DATE_TO_DATE_TIME;
                    default:
                        return null;
                }
            case StandardNames.XS_TIME:
                switch (st) {
                    case StandardNames.XS_UNTYPED_ATOMIC:
                    case StandardNames.XS_STRING:
                        return StringConverter.STRING_TO_TIME;
                    case StandardNames.XS_DATE_TIME:
                        return DATE_TIME_TO_TIME;
                    default:
                        return null;
                }
            case StandardNames.XS_DATE:
                switch (st) {
                    case StandardNames.XS_UNTYPED_ATOMIC:
                    case StandardNames.XS_STRING:
                        return new StringConverter.StringToDate(rules);
                    case StandardNames.XS_DATE_TIME:
                        return DATE_TIME_TO_DATE;
                    default:
                        return null;
                }
            case StandardNames.XS_G_YEAR_MONTH:
                switch (st) {
                    case StandardNames.XS_UNTYPED_ATOMIC:
                    case StandardNames.XS_STRING:
                        return new StringConverter.StringToGYearMonth(rules);
                    case StandardNames.XS_DATE:
                        return TwoPhaseConverter.makeTwoPhaseConverter(
                                BuiltInAtomicType.DATE, BuiltInAtomicType.DATE_TIME, BuiltInAtomicType.G_YEAR_MONTH, rules);
                    case StandardNames.XS_DATE_TIME:
                        return DATE_TIME_TO_G_YEAR_MONTH;
                    default:
                        return null;
                }
            case StandardNames.XS_G_YEAR:
                switch (st) {
                    case StandardNames.XS_UNTYPED_ATOMIC:
                    case StandardNames.XS_STRING:
                        return new StringConverter.StringToGYear(rules);
                    case StandardNames.XS_DATE:
                        return TwoPhaseConverter.makeTwoPhaseConverter(BuiltInAtomicType.DATE, BuiltInAtomicType.DATE_TIME,
                                BuiltInAtomicType.G_YEAR, rules);
                    case StandardNames.XS_DATE_TIME:
                        return DATE_TIME_TO_G_YEAR;
                    default:
                        return null;
                }
            case StandardNames.XS_G_MONTH_DAY:
                switch (st) {
                    case StandardNames.XS_UNTYPED_ATOMIC:
                    case StandardNames.XS_STRING:
                        return new StringConverter.StringToGMonthDay(rules);
                    case StandardNames.XS_DATE:
                        return TwoPhaseConverter.makeTwoPhaseConverter(BuiltInAtomicType.DATE, BuiltInAtomicType.DATE_TIME,
                                BuiltInAtomicType.G_MONTH_DAY, rules);
                    case StandardNames.XS_DATE_TIME:
                        return DATE_TIME_TO_G_MONTH_DAY;
                    default:
                        return null;
                }
            case StandardNames.XS_G_DAY:
                switch (st) {
                    case StandardNames.XS_UNTYPED_ATOMIC:
                    case StandardNames.XS_STRING:
                        return new StringConverter.StringToGDayConverter(rules);
                    case StandardNames.XS_DATE:
                        return TwoPhaseConverter.makeTwoPhaseConverter(BuiltInAtomicType.DATE, BuiltInAtomicType.DATE_TIME,
                                BuiltInAtomicType.G_DAY, rules);
                    case StandardNames.XS_DATE_TIME:
                        return DATE_TIME_TO_G_DAY;
                    default:
                        return null;
                }
            case StandardNames.XS_G_MONTH:
                switch (st) {
                    case StandardNames.XS_UNTYPED_ATOMIC:
                    case StandardNames.XS_STRING:
                        return new StringConverter.StringToGMonth(rules);
                    case StandardNames.XS_DATE:
                        return TwoPhaseConverter.makeTwoPhaseConverter(BuiltInAtomicType.DATE, BuiltInAtomicType.DATE_TIME,
                                BuiltInAtomicType.G_MONTH, rules);
                    case StandardNames.XS_DATE_TIME:
                        return DATE_TIME_TO_G_MONTH;
                    default:
                        return null;
                }
            case StandardNames.XS_BOOLEAN:
                switch (st) {
                    case StandardNames.XS_UNTYPED_ATOMIC:
                    case StandardNames.XS_STRING:
                        return StringConverter.STRING_TO_BOOLEAN;
                    case StandardNames.XS_FLOAT:
                    case StandardNames.XS_DOUBLE:
                    case StandardNames.XS_DECIMAL:
                   // case StandardNames.XS_PRECISION_DECIMAL:
                    case StandardNames.XS_INTEGER:
                    case StandardNames.XS_NUMERIC:
                        return NUMERIC_TO_BOOLEAN;
                    default:
                        return null;
                }
            case StandardNames.XS_BASE64_BINARY:
                switch (st) {
                    case StandardNames.XS_UNTYPED_ATOMIC:
                    case StandardNames.XS_STRING:
                        return StringConverter.STRING_TO_BASE64_BINARY;
                    case StandardNames.XS_HEX_BINARY:
                        return HEX_BINARY_TO_BASE64_BINARY;
                    default:
                        return null;
                }
            case StandardNames.XS_HEX_BINARY:
                switch (st) {
                    case StandardNames.XS_UNTYPED_ATOMIC:
                    case StandardNames.XS_STRING:
                        return StringConverter.STRING_TO_HEX_BINARY;
                    case StandardNames.XS_BASE64_BINARY:
                        return BASE64_BINARY_TO_HEX_BINARY;
                    default:
                        return null;
                }
            case StandardNames.XS_ANY_URI:
                switch (st) {
                    case StandardNames.XS_UNTYPED_ATOMIC:
                    case StandardNames.XS_STRING:
                        return new StringConverter.StringToAnyURI(rules);
                    default:
                        return null;
                }
            case StandardNames.XS_QNAME:
                switch (st) {
                    case StandardNames.XS_UNTYPED_ATOMIC:
                    case StandardNames.XS_STRING:
                        return new StringConverter.StringToQName(rules);
                    case StandardNames.XS_NOTATION:
                        return NOTATION_TO_QNAME;
                    default:
                        return null;
                }
            case StandardNames.XS_NOTATION:
                switch (st) {
                    case StandardNames.XS_UNTYPED_ATOMIC:
                    case StandardNames.XS_STRING:
                        return new StringConverter.StringToNotation(rules);
                    case StandardNames.XS_QNAME:
                        return QNAME_TO_NOTATION;
                    default:
                        return null;
                }

            case StandardNames.XS_ANY_ATOMIC_TYPE:
                return IDENTITY_CONVERTER;

            default:
                throw new IllegalArgumentException("Unknown primitive type " + tt);
        }
    }


    // All converters can hold a reference to the conversion rules, though many don't
    private ConversionRules conversionRules;

    // Protected constructor for a Converter

    protected Converter() {
    }

    /**
     * Construct a converter with a given set of conversion rules. For use in constructing subclasses
     *
     * @param rules the conversion rules for the configuration
     */
    protected Converter(ConversionRules rules) {
        setConversionRules(rules);
    }

    /**
     * Convert an atomic value from the source type to the target type
     *
     * @param input the atomic value to be converted, which the caller guarantees to be of the appropriate
     *              type for the converter
     * @return the result of the conversion, as an {@link AtomicValue}, if conversion succeeds, or a {@link ValidationFailure}
     *         object describing the reasons for failure if conversion is not possible. Note that the ValidationFailure
     *         object is not (and does not contain) an exception, because it does not necessarily result in an error being
     *         thrown, and creating exceptions on non-failure paths is expensive.
     */

    /*@NotNull*/
    public abstract ConversionResult convert(/*@NotNull*/ AtomicValue input);

    /**
     * Set the conversion rules to be used by this Converter
     *
     * @param rules the conversion rules
     */

    public final void setConversionRules(ConversionRules rules) {
        this.conversionRules = rules;
    }

    /**
     * Get the conversion rules to be used by this Converter
     *
     * @return the conversion rules
     */


    public final ConversionRules getConversionRules() {
        return conversionRules;
    }

    /**
     * Ask if this converter implements a conversion that requires XPath 3.0 (or XQuery 3.0 etc)
     * to be enabled
     *
     * @return true if XPath 3.0 support is required
     */

    public boolean isXPath30Conversion() {
        return false;
    }

    /**
     * Ask if this converter will always succeed
     *
     * @return true if this Converter will never return a ValidationFailure
     */

    public boolean isAlwaysSuccessful() {
        return false;
    }

    /**
     * Provide a namespace resolver, needed for conversion to namespace-sensitive types such as QName and NOTATION.
     * The resolver is ignored if the target type is not namespace-sensitive
     *
     * @param resolver the namespace resolver to be used
     */

    public void setNamespaceResolver(NamespaceResolver resolver) {
        // no action
    }

    /**
     * Get the namespace resolver if one has been supplied
     *
     * @return the namespace resolver, or null if none has been supplied
     */

    /*@Nullable*/ public NamespaceResolver getNamespaceResolver() {
        return null;
    }

    /**
     * Converter that does nothing except change the type annotation of the value. The caller
     * is responsible for ensuring that this type annotation is legimite, that is, that the value
     * is in the value space of this type
     */

    public static class UpCastingConverter extends Converter {
        private AtomicType newTypeAnnotation;

        public UpCastingConverter(AtomicType annotation) {
            this.newTypeAnnotation = annotation;
        }

        /*@NotNull*/
        public ConversionResult convert(/*@NotNull*/ AtomicValue input) {
            return input.copyAsSubType(newTypeAnnotation);
        }

        public boolean isAlwaysSuccessful() {
            return true;
        }
    }

    /**
     * Converter that does nothing except change the type annotation of the value. The caller
     * is responsible for ensuring that this type annotation is legimite, that is, that the value
     * is in the value space of this type
     */

    public static class DownCastingConverter extends Converter {
        private AtomicType newType;

        public DownCastingConverter(AtomicType annotation, ConversionRules rules) {
            this.newType = annotation;
            setConversionRules(rules);
        }

        /*@NotNull*/
        public ConversionResult convert(/*@NotNull*/ AtomicValue input) {
            return convert(input, input.getStringValueCS());
        }

        public ConversionResult convert(/*@NotNull*/ AtomicValue input, CharSequence lexicalForm) {
            ValidationFailure f = newType.validate(input, lexicalForm, getConversionRules());
            if (f == null) {
                // success
                return input.copyAsSubType(newType);
            } else {
                // validation failed
                return f;
            }
        }
    }

    /**
     * Converter that operates in two phases, via an intermediate type
     */

    public static class TwoPhaseConverter extends StringConverter {
        private Converter phaseOne;
        private Converter phaseTwo;

        public TwoPhaseConverter(Converter phaseOne, Converter phaseTwo) {
            this.phaseOne = phaseOne;
            this.phaseTwo = phaseTwo;
        }

        /*@Nullable*/ public static TwoPhaseConverter makeTwoPhaseConverter(/*@NotNull*/ AtomicType inputType, /*@NotNull*/ AtomicType viaType, /*@NotNull*/ AtomicType outputType, ConversionRules rules) {
            return new TwoPhaseConverter(
                    getConverter(inputType, viaType, rules),
                    getConverter(viaType, outputType, rules));
        }

        @Override
        public void setNamespaceResolver(NamespaceResolver resolver) {
            phaseOne.setNamespaceResolver(resolver);
            phaseTwo.setNamespaceResolver(resolver);
        }

        /*@NotNull*/
        public ConversionResult convert(/*@NotNull*/ AtomicValue input) {
            ConversionResult temp = phaseOne.convert(input);
            if (temp instanceof ValidationFailure) {
                return temp;
            }
            return phaseTwo.convert((AtomicValue) temp);
        }

        /*@NotNull*/
        public ConversionResult convertString(/*@NotNull*/ CharSequence input) {
            ConversionResult temp = ((StringConverter) phaseOne).convertString(input);
            if (temp instanceof ValidationFailure) {
                return temp;
            }
            return ((DownCastingConverter) phaseTwo).convert((AtomicValue) temp, input);
        }
    }

    /**
     * Converts any value to untyped atomic
     */

    public static class ToUntypedAtomicConverter extends Converter {
        /*@NotNull*/ public ConversionResult convert(/*@NotNull*/ AtomicValue input) {
            return new UntypedAtomicValue(input.getStringValueCS());
        }

        public boolean isAlwaysSuccessful() {
            return true;
        }
    }

    /**
     * Converts any value to a string
     */

    public static class ToStringConverter extends Converter {
        /*@NotNull*/ public ConversionResult convert(/*@NotNull*/ AtomicValue input) {
            return new StringValue(input.getStringValueCS());
        }

        public boolean isAlwaysSuccessful() {
            return true;
        }
    }

    /**
     * Converts any numeric value to xs:float
     */

    public static class NumericToFloat extends Converter {
        /*@NotNull*/ public ConversionResult convert(/*@NotNull*/ AtomicValue input) {
            return new FloatValue(((NumericValue) input).getFloatValue());
        }

        public boolean isAlwaysSuccessful() {
            return true;
        }
    }

    /**
     * Converts a boolean to an xs:float
     */

    public static class BooleanToFloat extends Converter {
        /*@NotNull*/ public ConversionResult convert(/*@NotNull*/ AtomicValue input) {
            return new FloatValue(((BooleanValue) input).getBooleanValue() ? 1.0f : 0.0f);
        }

        public boolean isAlwaysSuccessful() {
            return true;
        }
    }

    /**
     * Converts any numeric value to a double.
     */

    public static class NumericToDouble extends Converter {
        /*@NotNull*/ public ConversionResult convert(/*@NotNull*/ AtomicValue input) {
            if (input instanceof DoubleValue) {
                return input;
            } else {
                return new DoubleValue(((NumericValue) input).getDoubleValue());
            }
        }

        public boolean isAlwaysSuccessful() {
            return true;
        }
    }

    /**
     * Converts a boolean to a double
     */

    public static class BooleanToDouble extends Converter {
        /*@NotNull*/ public ConversionResult convert(/*@NotNull*/ AtomicValue input) {
            return new DoubleValue(((BooleanValue) input).getBooleanValue() ? 1.0e0 : 0.0e0);
        }

        public boolean isAlwaysSuccessful() {
            return true;
        }
    }

    /**
     * Convers a double to a decimal
     */

    public static class DoubleToDecimal extends Converter {
        /*@NotNull*/ public ConversionResult convert(/*@NotNull*/ AtomicValue input) {
            try {
                return new DecimalValue(((DoubleValue) input).getDoubleValue());
            } catch (ValidationException e) {
                return new ValidationFailure(e);
            }
        }
    }

    /**
     * Converts a float to a decimal
     */

    public static class FloatToDecimal extends Converter {
        /*@NotNull*/ public ConversionResult convert(/*@NotNull*/ AtomicValue input) {
            try {
                return new DecimalValue(((FloatValue) input).getFloatValue());
            } catch (ValidationException e) {
                return new ValidationFailure(e);
            }
        }
    }

    /**
     * Converts an integer to a decimal
     */

    public static class IntegerToDecimal extends Converter {
        /*@NotNull*/ public ConversionResult convert(/*@NotNull*/ AtomicValue input) {
            if (input instanceof Int64Value) {
                return new DecimalValue(((Int64Value) input).longValue());
            } else {
                return new DecimalValue(((BigIntegerValue) input).asDecimal());
            }
        }

        public boolean isAlwaysSuccessful() {
            return true;
        }
    }

    /**
     * Converts any numeric value to a decimal
     */

    public static class NumericToDecimal extends Converter {
        /*@NotNull*/ public ConversionResult convert(/*@NotNull*/ AtomicValue input) {
            try {
                BigDecimal decimal = ((NumericValue) input).getDecimalValue();
                return new DecimalValue(decimal);
            } catch (XPathException e) {
                return new ValidationFailure(e);
            }
        }
    }

    /**
     * Converts a boolean to a decimal
     */

    public static class BooleanToDecimal extends Converter {
        /*@NotNull*/
        public ConversionResult convert(/*@NotNull*/ AtomicValue input) {
            return ((BooleanValue) input).getBooleanValue() ? DecimalValue.ONE : DecimalValue.ZERO;
        }

        public boolean isAlwaysSuccessful() {
            return true;
        }
    }


    /**
     * Converts a double to an integer
     */

    public static class DoubleToInteger extends Converter {
        /*@NotNull*/
        public ConversionResult convert(/*@NotNull*/ AtomicValue input) {
            return IntegerValue.makeIntegerValue((DoubleValue) input);
        }
    }

    /**
     * Converts a float to an integer
     */

    public static class FloatToInteger extends Converter {
        /*@NotNull*/
        public ConversionResult convert(/*@NotNull*/ AtomicValue input) {
            return IntegerValue.makeIntegerValue(new DoubleValue(((FloatValue) input).getDoubleValue()));
        }
    }

    /**
     * Converts a decimal to an integer. Because an xs:integer is an xs:decimal,
     * this must also be prepared to accept an xs:integer
     */

    public static class DecimalToInteger extends Converter {
        /*@NotNull*/
        public ConversionResult convert(/*@NotNull*/ AtomicValue input) {
            if (input instanceof IntegerValue) {
                return input;
            }
            return BigIntegerValue.makeIntegerValue(((DecimalValue) input).getDecimalValue().toBigInteger());
        }

        public boolean isAlwaysSuccessful() {
            return true;
        }
    }

    /**
     * Converts any numeric value to an integer.
     */

    public static class NumericToInteger extends Converter {
        /*@NotNull*/
        public ConversionResult convert(/*@NotNull*/ AtomicValue input) {
            try {
                if (input instanceof IntegerValue) {
                    return input;
                } else if (input instanceof DoubleValue) {
                    return IntegerValue.makeIntegerValue((DoubleValue) input);
                } else if (input instanceof FloatValue) {
                    return IntegerValue.makeIntegerValue(new DoubleValue(((FloatValue) input).getDoubleValue()));
                } else {
                    return BigIntegerValue.makeIntegerValue(((NumericValue) input).getDecimalValue().toBigInteger());
                }
            } catch (XPathException e) {
                return new ValidationFailure(e);
            }
        }
    }

    /**
     * Converts a boolean to an integer
     */


    public static class BooleanToInteger extends Converter {
        /*@NotNull*/
        public ConversionResult convert(/*@NotNull*/ AtomicValue input) {
            return ((BooleanValue) input).getBooleanValue() ? Int64Value.PLUS_ONE : Int64Value.ZERO;
        }

        public boolean isAlwaysSuccessful() {
            return true;
        }
    }

    /**
     * Converts a duration to a dayTimeDuration
     */

    public static class DurationToDayTimeDuration extends Converter {
        /*@NotNull*/ public ConversionResult convert(/*@NotNull*/ AtomicValue input) {
            DurationValue d = (DurationValue) input;
            return new DayTimeDurationValue(d.signum(), d.getDays(), d.getHours(), d.getMinutes(), d.getSeconds(), d.getMicroseconds());
        }
    }

    /**
     * Converts a duration to a yearMonthDuration
     */

    public static class DurationToYearMonthDuration extends Converter {
        /*@NotNull*/
        public ConversionResult convert(/*@NotNull*/ AtomicValue input) {
            DurationValue d = (DurationValue) input;
            return YearMonthDurationValue.fromMonths(d.getTotalMonths());
        }
    }

    /**
     * Converts a date to a dateTime
     */

    public static class DateToDateTime extends Converter {
        /*@NotNull*/
        public ConversionResult convert(/*@NotNull*/ AtomicValue input) {
            return ((DateValue) input).toDateTime();
        }

        public boolean isAlwaysSuccessful() {
            return true;
        }
    }

    /**
     * Converts a dateTime to a date
     */

    public static class DateTimeToDate extends Converter {
        /*@NotNull*/ public ConversionResult convert(/*@NotNull*/ AtomicValue input) {
            DateTimeValue dt = (DateTimeValue) input;
            return new DateValue(dt.getYear(), dt.getMonth(), dt.getDay(), dt.getTimezoneInMinutes(), dt.isXsd10Rules());
        }

        public boolean isAlwaysSuccessful() {
            return true;
        }
    }

    /**
     * Converts a dateTime to a gMonth
     */

    public static class DateTimeToGMonth extends Converter {
        /*@NotNull*/ public ConversionResult convert(/*@NotNull*/ AtomicValue input) {
            DateTimeValue dt = (DateTimeValue) input;
            return new GMonthValue(dt.getMonth(), dt.getTimezoneInMinutes());
        }

        public boolean isAlwaysSuccessful() {
            return true;
        }
    }

    /**
     * Converts a dateTime to a gYearMonth
     */

    public static class DateTimeToGYearMonth extends Converter {
        /*@NotNull*/ public ConversionResult convert(/*@NotNull*/ AtomicValue input) {
            DateTimeValue dt = (DateTimeValue) input;
            return new GYearMonthValue(dt.getYear(), dt.getMonth(), dt.getTimezoneInMinutes(), dt.isXsd10Rules());
        }

        public boolean isAlwaysSuccessful() {
            return true;
        }
    }

    /**
     * Converts a dateTime to a gYear
     */

    public static class DateTimeToGYear extends Converter {
        /*@NotNull*/ public ConversionResult convert(/*@NotNull*/ AtomicValue input) {
            DateTimeValue dt = (DateTimeValue) input;
            return new GYearValue(dt.getYear(), dt.getTimezoneInMinutes(), dt.isXsd10Rules());
        }

        public boolean isAlwaysSuccessful() {
            return true;
        }
    }

    /**
     * Converts a dateTime to a gMonthDay
     */

    public static class DateTimeToGMonthDay extends Converter {
        /*@NotNull*/ public ConversionResult convert(/*@NotNull*/ AtomicValue input) {
            DateTimeValue dt = (DateTimeValue) input;
            return new GMonthDayValue(dt.getMonth(), dt.getDay(), dt.getTimezoneInMinutes());
        }

        public boolean isAlwaysSuccessful() {
            return true;
        }
    }

    /**
     * Converts a dateTime to a gDay
     */

    public static class DateTimeToGDay extends Converter {
        /*@NotNull*/ public ConversionResult convert(/*@NotNull*/ AtomicValue input) {
            DateTimeValue dt = (DateTimeValue) input;
            return new GDayValue(dt.getDay(), dt.getTimezoneInMinutes());
        }

        public boolean isAlwaysSuccessful() {
            return true;
        }
    }

    /**
     * Converts a dateTime to a time
     */

    public static class DateTimeToTime extends Converter {
        /*@NotNull*/ public ConversionResult convert(/*@NotNull*/ AtomicValue input) {
            DateTimeValue dt = (DateTimeValue) input;
            return new TimeValue(dt.getHour(), dt.getMinute(), dt.getSecond(), dt.getMicrosecond(), dt.getTimezoneInMinutes());
        }

        public boolean isAlwaysSuccessful() {
            return true;
        }
    }

    /**
     * Converts a numeric value to a boolean
     */

    public static class NumericToBoolean extends Converter {
        /*@NotNull*/
        public ConversionResult convert(/*@NotNull*/ AtomicValue input) {
            try {
                return BooleanValue.get(input.effectiveBooleanValue());
            } catch (XPathException err) {
                throw new AssertionError(err);
            }
        }

        public boolean isAlwaysSuccessful() {
            return true;
        }
    }

    /**
     * Converts base64 to hexBinary
     */

    public static class Base64BinaryToHexBinary extends Converter {
        /*@NotNull*/ public ConversionResult convert(/*@NotNull*/ AtomicValue input) {
            return new HexBinaryValue(((Base64BinaryValue) input).getBinaryValue());
        }

        public boolean isAlwaysSuccessful() {
            return true;
        }
    }

    /**
     * Converts string to base64
     */

    public static class StringToBase64BinaryConverter extends StringConverter {
        /*@NotNull*/ public ConversionResult convert(/*@NotNull*/ AtomicValue input) {
            return convertString(input.getStringValueCS());
        }

        /*@NotNull*/ public ConversionResult convertString(/*@NotNull*/ CharSequence input) {
            try {
                return new Base64BinaryValue(input);
            } catch (XPathException e) {
                return new ValidationFailure(e);
            }
        }
    }

    /**
     * Converts hexBinary to base64Binary
     */

    public static class HexBinaryToBase64Binary extends Converter {
        /*@NotNull*/ public ConversionResult convert(/*@NotNull*/ AtomicValue input) {
            return new Base64BinaryValue(((HexBinaryValue) input).getBinaryValue());
        }

        public boolean isAlwaysSuccessful() {
            return true;
        }
    }

    /**
     * Converts Notation to QName
     */

    public static class NotationToQName extends Converter {
        @Override
        public boolean isXPath30Conversion() {
            return true;
        }

        /*@NotNull*/ public ConversionResult convert(/*@NotNull*/ AtomicValue input) {
            return new QNameValue(((NotationValue) input).getStructuredQName(), BuiltInAtomicType.QNAME);
        }

        public boolean isAlwaysSuccessful() {
            return true;
        }
    }

    /**
     * Converts QName to Notation
     */

    public static class QNameToNotation extends Converter {

        @Override
        public boolean isXPath30Conversion() {
            return true;
        }

        /*@NotNull*/ public ConversionResult convert(/*@NotNull*/ AtomicValue input) {
            return new NotationValue(((QNameValue) input).getStructuredQName(), BuiltInAtomicType.NOTATION);
        }
    }

    /**
     * Converter that implements the promotion rules to a required type of xs:double
     */

    public static class PromoterToDouble extends Converter {

        /*@Nullable*/ private StringConverter stringToDouble = null;

        /*@NotNull*/
        @Override
        public ConversionResult convert(/*@NotNull*/ AtomicValue input) {
            if (input instanceof DoubleValue) {
                return input;
            } else if (input instanceof NumericValue) {
                return new DoubleValue(((NumericValue) input).getDoubleValue());
            } else if (input instanceof UntypedAtomicValue) {
                if (stringToDouble == null) {
                    stringToDouble = StringConverter.getStringConverter(BuiltInAtomicType.DOUBLE, getConversionRules());
                }
                return stringToDouble.convert(input);
            } else {
                ValidationFailure err = new ValidationFailure(
                        "Cannot promote non-numeric value to xs:double");
                err.setErrorCode("XPTY0004");
                return err;
            }
        }
    }

    /**
     * Converter that implements the promotion rules to a required type of xs:float
     */

    public static class PromoterToFloat extends Converter {

        /*@Nullable*/ private StringConverter stringToFloat = null;

        /*@NotNull*/
        @Override
        public ConversionResult convert(/*@NotNull*/ AtomicValue input) {
            if (input instanceof FloatValue) {
                return input;
            } else if (input instanceof DoubleValue) {
                ValidationFailure err = new ValidationFailure(
                        "Cannot promote from xs:double to xs:float");
                err.setErrorCode("XPTY0004");
                return err;
            } else if (input instanceof NumericValue) {
                return new FloatValue((float) ((NumericValue) input).getDoubleValue());
            } else if (input instanceof UntypedAtomicValue) {
                if (stringToFloat == null) {
                    stringToFloat = StringConverter.getStringConverter(BuiltInAtomicType.FLOAT, getConversionRules());
                }
                return stringToFloat.convert(input);
            } else {
                ValidationFailure err = new ValidationFailure(
                        "Cannot promote non-numeric value to xs:double");
                err.setErrorCode("XPTY0004");
                return err;
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
// The Original Code is: all this file
//
// The Initial Developer of the Original Code is Saxonica Limited.
// Portions created by ___ are Copyright (C) ___. All rights reserved.
//
// Contributor(s):
//