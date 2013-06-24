package net.sf.saxon.expr;

import net.sf.saxon.Configuration;
import net.sf.saxon.lib.ExternalObjectModel;
import net.sf.saxon.om.Item;
import net.sf.saxon.om.NodeInfo;
import net.sf.saxon.om.SequenceIterator;
import net.sf.saxon.om.ValueRepresentation;
import net.sf.saxon.pattern.EmptySequenceTest;
import net.sf.saxon.pattern.NodeTest;
import net.sf.saxon.trans.SaxonErrorCode;
import net.sf.saxon.trans.XPathException;
import net.sf.saxon.tree.wrapper.VirtualNode;
import net.sf.saxon.type.BuiltInAtomicType;
import net.sf.saxon.type.ExternalObjectType;
import net.sf.saxon.type.ItemType;
import net.sf.saxon.type.TypeHierarchy;
import net.sf.saxon.value.*;

import java.io.Serializable;
import java.lang.reflect.Array;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;

/**
 * This class together with its embedded subclasses handles conversion from XPath values
 * to Java values
 */
public abstract class PJConverter implements Serializable {

    private static HashMap<Class, SequenceType> jpmap = new HashMap<Class, SequenceType>();

    static {
        jpmap.put(boolean.class, SequenceType.SINGLE_BOOLEAN);
        jpmap.put(Boolean.class, SequenceType.OPTIONAL_BOOLEAN);
        jpmap.put(String.class, SequenceType.OPTIONAL_STRING);
        jpmap.put(CharSequence.class, SequenceType.OPTIONAL_STRING);
        // Mappings for long and int are chosen to avoid static type errors when
        // a Java method expecting long or int is called with an integer literal
        jpmap.put(long.class, SequenceType.SINGLE_INTEGER);
        jpmap.put(Long.class, SequenceType.OPTIONAL_INTEGER);
        jpmap.put(int.class, SequenceType.SINGLE_INTEGER);
        jpmap.put(Integer.class, SequenceType.OPTIONAL_INTEGER);
        jpmap.put(short.class, SequenceType.SINGLE_SHORT);
        jpmap.put(Short.class, SequenceType.OPTIONAL_SHORT);
        jpmap.put(byte.class, SequenceType.SINGLE_BYTE);
        jpmap.put(Byte.class, SequenceType.OPTIONAL_BYTE);
        jpmap.put(float.class, SequenceType.SINGLE_FLOAT);
        jpmap.put(Float.class, SequenceType.OPTIONAL_FLOAT);
        jpmap.put(double.class, SequenceType.SINGLE_DOUBLE);
        jpmap.put(Double.class, SequenceType.OPTIONAL_DOUBLE);
        jpmap.put(URI.class, SequenceType.OPTIONAL_ANY_URI);
        jpmap.put(URL.class, SequenceType.OPTIONAL_ANY_URI);
        jpmap.put(BigInteger.class, SequenceType.OPTIONAL_INTEGER);
        jpmap.put(BigDecimal.class, SequenceType.OPTIONAL_DECIMAL);
    }



    /**
     * Get the nearest XPath equivalent to a Java class. A function call will
     * be type-checked against an XPath function signature in which the Java classes
     * are replaced by their nearest equivalent XPath types
     * @param javaClass a Java class
     * @return the nearest equivalent XPath SequenceType
     */

    public static SequenceType getEquivalentItemType(Class javaClass) {
        return jpmap.get(javaClass);
    }

    /**
     * Convert an XPath value to a Java value of a specified class
     *
     * @param value the supplied XPath value
     * @param targetClass the class of the required Java value
     * @param context the XPath dynamic context
     * @return the corresponding Java value, which is guaranteed to be an instance of the
     * target class (except that an empty sequence is converted to null)
     * @throws XPathException if the conversion is not possible or fails
     */

    /*@Nullable*/ public abstract Object convert(ValueRepresentation<? extends Item> value, Class targetClass, XPathContext context)
            throws XPathException;

    /**
     * Factory method to instantiate a converter from a given XPath type to a given Java class
     * @param config the Saxon Configuration
     * @param itemType the item type of the XPath value to be converted
     * @param cardinality the cardinality of the XPath value to be converted
     * @param targetClass the Java class required for the conversion result
     * @return a suitable converter
     * @throws net.sf.saxon.trans.XPathException if no conversion is possible
     */

    public static PJConverter allocate(Configuration config, ItemType itemType,
                                       int cardinality, Class targetClass)
    throws XPathException {
        TypeHierarchy th = config.getTypeHierarchy();
        if (targetClass == SequenceIterator.class) {
            return ToSequenceIterator.INSTANCE;
        }
        if (targetClass == ValueRepresentation.class || targetClass == Item.class) {
            return Identity.INSTANCE;
        }
        if (targetClass == Value.class | targetClass == SequenceExtent.class) {
            return ToSequenceExtent.INSTANCE;
        }

        if (!(itemType instanceof BuiltInAtomicType)) {
            List<ExternalObjectModel> externalObjectModels = config.getExternalObjectModels();
            for (ExternalObjectModel model : externalObjectModels) {
                PJConverter converter = model.getPJConverter(targetClass);
                if (converter != null) {
                    return converter;
                }
            }

            if (NodeInfo.class.isAssignableFrom(targetClass)) {
                return Identity.INSTANCE;
            }
        }

        if (Collection.class.isAssignableFrom(targetClass)) {
            return ToCollection.INSTANCE;
        }
        if (targetClass.isArray()) {
            PJConverter itemConverter =
                    allocate(config, itemType, StaticProperty.EXACTLY_ONE, targetClass.getComponentType());
            return new ToArray(itemConverter);
        }
        if (!Cardinality.allowsMany(cardinality)) {
            if (itemType instanceof ExternalObjectType) {
                return UnwrapExternalObject.INSTANCE;

            } else if (itemType.isPlainType()) {
                if (th.isSubType(itemType, BuiltInAtomicType.STRING)) {
                    if (targetClass == Object.class || targetClass == String.class || targetClass == CharSequence.class) {
                        return StringValueToString.INSTANCE;
                    } else if (targetClass.isAssignableFrom(StringValue.class)) {
                        return Identity.INSTANCE;
                    } else if (targetClass == char.class || targetClass == Character.class) {
                        return StringValueToChar.INSTANCE;
                    } else {
                        throw cannotConvert(itemType, targetClass, config);
                    }
                } else if (itemType == BuiltInAtomicType.UNTYPED_ATOMIC) {
                    if (targetClass == Object.class || targetClass == String.class || targetClass == CharSequence.class) {
                        return StringValueToString.INSTANCE;
                    } else if (targetClass.isAssignableFrom(UntypedAtomicValue.class)) {
                        return Identity.INSTANCE;
                    } else {
                        throw cannotConvert(itemType, targetClass, config);
                    }
                } else if (th.isSubType(itemType, BuiltInAtomicType.BOOLEAN)) {
                    if (targetClass == Object.class || targetClass == Boolean.class || targetClass == boolean.class) {
                        return BooleanValueToBoolean.INSTANCE;
                    } else if (targetClass.isAssignableFrom(BooleanValue.class)) {
                        return Identity.INSTANCE;
                    } else {
                        throw cannotConvert(itemType, targetClass, config);
                    }
                } else if (th.isSubType(itemType, BuiltInAtomicType.INTEGER)) {
                    if (targetClass == Object.class || targetClass == BigInteger.class) {
                        return IntegerValueToBigInteger.INSTANCE;
                    } else if (targetClass == long.class || targetClass == Long.class) {
                        return IntegerValueToLong.INSTANCE;
                    } else if (targetClass == int.class || targetClass == Integer.class) {
                        return IntegerValueToInt.INSTANCE;
                    } else if (targetClass == short.class || targetClass == Short.class) {
                        return IntegerValueToShort.INSTANCE;
                    } else if (targetClass == byte.class || targetClass == Byte.class) {
                        return IntegerValueToByte.INSTANCE;
                    } else if (targetClass == char.class || targetClass == Character.class) {
                        return IntegerValueToChar.INSTANCE;
                    } else if (targetClass == double.class || targetClass == Double.class) {
                        return NumericValueToDouble.INSTANCE;
                    } else if (targetClass == float.class || targetClass == Float.class) {
                        return NumericValueToFloat.INSTANCE;
                    } else if (targetClass == BigDecimal.class) {
                        return NumericValueToBigDecimal.INSTANCE;
                    } else if (targetClass.isAssignableFrom(IntegerValue.class)) {
                        return Identity.INSTANCE;
                    } else {
                        throw cannotConvert(itemType, targetClass, config);
                    }
                } else if (th.isSubType(itemType, BuiltInAtomicType.DECIMAL)) {
                    if (targetClass == Object.class || targetClass == BigDecimal.class) {
                        return NumericValueToBigDecimal.INSTANCE;
                    } else if (targetClass == double.class || targetClass == Double.class) {
                        return NumericValueToDouble.INSTANCE;
                    } else if (targetClass == float.class || targetClass == Float.class) {
                        return NumericValueToFloat.INSTANCE;
                    } else if (targetClass.isAssignableFrom(DecimalValue.class)) {
                        return Identity.INSTANCE;
                    } else {
                        throw cannotConvert(itemType, targetClass, config);
                    }
                } else if (th.isSubType(itemType, BuiltInAtomicType.FLOAT)) {
                    if (targetClass == Object.class || targetClass == Float.class || targetClass == float.class) {
                        return NumericValueToFloat.INSTANCE;
                    } else if (targetClass == double.class || targetClass == Double.class) {
                        return NumericValueToDouble.INSTANCE;
                    } else if (targetClass.isAssignableFrom(FloatValue.class)) {
                        return Identity.INSTANCE;
                    } else {
                        throw cannotConvert(itemType, targetClass, config);
                    }
                } else if (th.isSubType(itemType, BuiltInAtomicType.DOUBLE)) {
                    if (targetClass == Object.class || targetClass == Double.class || targetClass == double.class) {
                        return NumericValueToDouble.INSTANCE;
                    } else if (targetClass.isAssignableFrom(DoubleValue.class)) {
                        return Identity.INSTANCE;
                    } else {
                        return Atomic.INSTANCE;
                    }
                } else if (th.isSubType(itemType, BuiltInAtomicType.ANY_URI)) {
                    if (targetClass == Object.class || URI.class.isAssignableFrom(targetClass)) {
                        return AnyURIValueToURI.INSTANCE;
                    } else if (URL.class.isAssignableFrom(targetClass)) {
                        return AnyURIValueToURL.INSTANCE;
                    } else if (targetClass == String.class || targetClass == CharSequence.class) {
                        return StringValueToString.INSTANCE;
                    } else if (targetClass.isAssignableFrom(AnyURIValue.class)) {
                        return Identity.INSTANCE;
                    } else {
                        throw cannotConvert(itemType, targetClass, config);
                    }
                } else if (th.isSubType(itemType, BuiltInAtomicType.QNAME)) {
                    if (targetClass == Object.class || targetClass == javax.xml.namespace.QName.class) {
                        // Note JDK1.5 dependency
                        return QualifiedNameValueToQName.INSTANCE;
                    } else if (targetClass.isAssignableFrom(QNameValue.class)) {
                        return Identity.INSTANCE;
                    } else {
                        throw cannotConvert(itemType, targetClass, config);
                    }
                } else if (th.isSubType(itemType, BuiltInAtomicType.NOTATION)) {
                    if (targetClass == Object.class || targetClass == javax.xml.namespace.QName.class) {
                        // Note JDK1.5 dependency
                        return QualifiedNameValueToQName.INSTANCE;
                    } else if (targetClass.isAssignableFrom(NotationValue.class)) {
                        return Identity.INSTANCE;
                    } else {
                        throw cannotConvert(itemType, targetClass, config);
                    }
                } else if (th.isSubType(itemType, BuiltInAtomicType.DURATION)) {
                    if (targetClass.isAssignableFrom(DurationValue.class)) {
                        return Identity.INSTANCE;
                    } else {
                        throw cannotConvert(itemType, targetClass, config);
                    }
                } else if (th.isSubType(itemType, BuiltInAtomicType.DATE_TIME)) {
                    if (targetClass.isAssignableFrom(DateTimeValue.class)) {
                        return Identity.INSTANCE;
                    } else if (targetClass == java.util.Date.class) {
                        return CalendarValueToDate.INSTANCE;
                    } else if (targetClass == java.util.Calendar.class) {
                        return CalendarValueToCalendar.INSTANCE;
                    } else {
                        throw cannotConvert(itemType, targetClass, config);
                    }
                } else if (th.isSubType(itemType, BuiltInAtomicType.DATE)) {
                    if (targetClass.isAssignableFrom(DateValue.class)) {
                        return Identity.INSTANCE;
                    } else if (targetClass == java.util.Date.class) {
                        return CalendarValueToDate.INSTANCE;
                    } else if (targetClass == java.util.Calendar.class) {
                        return CalendarValueToCalendar.INSTANCE;
                    } else {
                        throw cannotConvert(itemType, targetClass, config);
                    }
                } else if (th.isSubType(itemType, BuiltInAtomicType.TIME)) {
                    if (targetClass.isAssignableFrom(TimeValue.class)) {
                        return Identity.INSTANCE;
                    } else if (targetClass == java.util.Date.class) {
                        return CalendarValueToDate.INSTANCE;
                    } else if (targetClass == java.util.Calendar.class) {
                        return CalendarValueToCalendar.INSTANCE;
                    } else {
                        throw cannotConvert(itemType, targetClass, config);
                    }
                } else if (th.isSubType(itemType, BuiltInAtomicType.G_YEAR)) {
                    if (targetClass.isAssignableFrom(GYearValue.class)) {
                        return Identity.INSTANCE;
                    } else {
                        throw cannotConvert(itemType, targetClass, config);
                    }
                } else if (th.isSubType(itemType, BuiltInAtomicType.G_YEAR_MONTH)) {
                    if (targetClass.isAssignableFrom(GYearMonthValue.class)) {
                        return Identity.INSTANCE;
                    } else {
                        throw cannotConvert(itemType, targetClass, config);
                    }
                } else if (th.isSubType(itemType, BuiltInAtomicType.G_MONTH)) {
                    if (targetClass.isAssignableFrom(GMonthValue.class)) {
                        return Identity.INSTANCE;
                    } else {
                        throw cannotConvert(itemType, targetClass, config);
                    }
                } else if (th.isSubType(itemType, BuiltInAtomicType.G_MONTH_DAY)) {
                    if (targetClass.isAssignableFrom(GMonthDayValue.class)) {
                        return Identity.INSTANCE;
                    } else {
                        throw cannotConvert(itemType, targetClass, config);
                    }
                } else if (th.isSubType(itemType, BuiltInAtomicType.G_DAY)) {
                    if (targetClass.isAssignableFrom(GDayValue.class)) {
                        return Identity.INSTANCE;
                    } else {
                        throw cannotConvert(itemType, targetClass, config);
                    }
                } else if (th.isSubType(itemType, BuiltInAtomicType.BASE64_BINARY)) {
                    if (targetClass.isAssignableFrom(Base64BinaryValue.class)) {
                        return Identity.INSTANCE;
                    } else {
                        throw cannotConvert(itemType, targetClass, config);
                    }
                } else if (th.isSubType(itemType, BuiltInAtomicType.HEX_BINARY)) {
                    if (targetClass.isAssignableFrom(HexBinaryValue.class)) {
                        return Identity.INSTANCE;
                    } else {
                        throw cannotConvert(itemType, targetClass, config);
                    }
                } else {
                    return Atomic.INSTANCE;
                }

            } else if (itemType instanceof EmptySequenceTest) {
                return ToNull.INSTANCE;

            } else if (itemType instanceof NodeTest) {
                if (NodeInfo.class.isAssignableFrom(targetClass)) {
                    return Identity.INSTANCE;
                } else {
                    return General.INSTANCE;
                }

            } else {
                // ItemType is item()
                return General.INSTANCE;
            }
        } else {
            // Cardinality allows many (but target type is not a collection)
            return General.INSTANCE;
        }
    }


    private static XPathException cannotConvert(ItemType source, Class target, Configuration config) {
        return new XPathException("Cannot convert from " + source.toString(config.getNamePool()) +
            " to " + target.getName());
    }

    /**
     * Static method to get a converter from an XPath sequence of nodes to the representation of a NodeList
     * in an external object model (this is really a special for DOM, which uses NodeList rather than general
     * purpose Java collection classes)
     * @param config the Saxon configuration
     * @param node an object representing a node in an external model
     * @return the Java object representing the external node
     */

    public static PJConverter allocateNodeListCreator(Configuration config, Object node) {
        List<ExternalObjectModel> externalObjectModels = config.getExternalObjectModels();
        for (ExternalObjectModel model : externalObjectModels) {
            PJConverter converter = model.getNodeListCreator(node);
            if (converter != null) {
                return converter;
            }
        }
        return ToCollection.INSTANCE;
    }

    public static class ToSequenceIterator extends PJConverter {

        public static final ToSequenceIterator INSTANCE = new ToSequenceIterator();

        public Object convert(ValueRepresentation<? extends Item> value, Class targetClass, XPathContext context) throws XPathException {
            return Value.asIterator(value);
        }

    }

    public static class ToNull extends PJConverter {

        public static final ToNull INSTANCE = new ToNull();

        public Object convert(ValueRepresentation<? extends Item> value, Class targetClass, XPathContext context) throws XPathException {
            return null;
        }

    }

    public static class ToSequenceExtent extends PJConverter {

        public static final ToSequenceExtent INSTANCE = new ToSequenceExtent();

        public Object convert(ValueRepresentation<? extends Item> value, Class targetClass, XPathContext context) throws XPathException {
            return SequenceExtent.makeSequenceExtent(Value.asIterator(value));
        }

    }

    /**
     * Converter for use when the target class is a collection class. Also used when the target
     * class is Object
     */

    public static class ToCollection extends PJConverter {

        public static final ToCollection INSTANCE = new ToCollection();

        public Object convert(ValueRepresentation<? extends Item> value, Class targetClass, XPathContext context) throws XPathException {
            Collection<Object> list;
            if (targetClass.isAssignableFrom(ArrayList.class)) {
                list = new ArrayList<Object>(100);
            } else {
                try {
                    list = (Collection<Object>)targetClass.newInstance();
                } catch (InstantiationException e) {
                    XPathException de = new XPathException("Cannot instantiate collection class " + targetClass);
                    de.setXPathContext(context);
                    throw de;
                } catch (IllegalAccessException e) {
                    XPathException de = new XPathException("Cannot access collection class " + targetClass);
                    de.setXPathContext(context);
                    throw de;
                }
            }
            Configuration config = context.getConfiguration();
            TypeHierarchy th = config.getTypeHierarchy();
            SequenceIterator<? extends Item> iter = Value.asIterator(value);
            while (true) {
                Item it = iter.next();
                if (it == null) {
                    return list;
                }
                if (it instanceof AtomicValue) {
                    PJConverter pj = allocate(
                            config, ((AtomicValue)it).getItemType(th), StaticProperty.EXACTLY_ONE, Object.class);
                    list.add(pj.convert(it, Object.class, context));
                    //list.add(((AtomicValue)it).convertToJava(Object.class, context));
                } else if (it instanceof VirtualNode) {
                    list.add(((VirtualNode)it).getRealNode());
                } else {
                    list.add(it);
                }
            }
            //return Value.asValue(value).convertToJavaList(list, context);
        }

    }

    /**
     * Converter for use when the target class is an array
     */

    public static class ToArray extends PJConverter {

        private PJConverter itemConverter;

        public ToArray(PJConverter itemConverter) {
            this.itemConverter = itemConverter;
        }

        public Object convert(ValueRepresentation<? extends Item> value, Class targetClass, XPathContext context) throws XPathException {
            Class componentClass = targetClass.getComponentType();
            List<Object> list = new ArrayList<Object>(20);
            SequenceIterator<? extends Item> iter = Value.asIterator(value);
            while (true) {
                Item item = iter.next();
                if (item == null) break;
                Object obj = itemConverter.convert(item, componentClass, context);
                if (obj != null) {
                    list.add(obj);
                }
            }
            Object array = Array.newInstance(componentClass, list.size());
            for (int i=0; i<list.size(); i++) {
                Array.set(array, i, list.get(i));
            }
            return array;
            //return list.toArray((Object[])array);
        }

    }

    public static class Identity extends PJConverter {

        public static final Identity INSTANCE = new Identity();

        public Object convert(ValueRepresentation<? extends Item> value, Class targetClass, XPathContext context) throws XPathException {
            if (value instanceof SingletonItem) {
                value = ((SingletonItem)value).asItem();
            }
            if (value instanceof VirtualNode) {
                Object obj = ((VirtualNode)value).getRealNode();
                if (targetClass.isAssignableFrom(obj.getClass())) {
                    return obj;
                }
            }
            if (targetClass.isAssignableFrom(value.getClass())) {
                return value;
            } else {
                ValueRepresentation val = Value.asValue(value).reduce();
                if (val instanceof SingletonItem) {
                    val = ((SingletonItem)val).asItem();
                }
                if (targetClass.isAssignableFrom(val.getClass())) {
                    return val;
                } else if (val instanceof EmptySequence) {
                    return null;
                } else {
                    throw new XPathException("Cannot convert value " + val.getClass() + " of type " +
                            Value.asValue(value).getItemType(context.getConfiguration().getTypeHierarchy()) +
                            " to class " + targetClass.getName());
                }
            }
        }

    }

    public static class UnwrapExternalObject extends PJConverter {

        public static final UnwrapExternalObject INSTANCE = new UnwrapExternalObject();

        public Object convert(ValueRepresentation<? extends Item> value, Class targetClass, XPathContext context) throws XPathException {
            Value val = Value.asValue(value).reduce();
            if (!(val instanceof ObjectValue)) {
                throw new XPathException("Expected external object of class " + targetClass +
                        ", got " + val.getClass());
            }
            Object obj = ((ObjectValue)val).getObject();
            if (!targetClass.isAssignableFrom(obj.getClass())) {
                throw new XPathException("External object has wrong class (is "
                        + obj.getClass().getName() + ", expected " + targetClass.getName());
            }
            return obj;
        }

    }

    public static class StringValueToString extends PJConverter {

        public static final StringValueToString INSTANCE = new StringValueToString();

        public Object convert(ValueRepresentation<? extends Item> value, Class targetClass, XPathContext context) throws XPathException {
            return value.getStringValue();
        }

    }

    public static class StringValueToChar extends PJConverter {

        public static final StringValueToChar INSTANCE = new StringValueToChar();

        public Object convert(ValueRepresentation<? extends Item> value, Class targetClass, XPathContext context) throws XPathException {
            String str = value.getStringValue();
            if (str.length() == 1) {
                return str.charAt(0);
            } else {
                XPathException de = new XPathException("Cannot convert xs:string to Java char unless length is 1");
                de.setXPathContext(context);
                de.setErrorCode(SaxonErrorCode.SXJE0005);
                throw de;
            }
        }

    }


    public static class BooleanValueToBoolean extends PJConverter {

        public static final BooleanValueToBoolean INSTANCE = new BooleanValueToBoolean();

        public Object convert(ValueRepresentation<? extends Item> value, Class targetClass, XPathContext context) throws XPathException {
            BooleanValue bv = (BooleanValue)Value.asItem(value);
            assert bv != null;
            return bv.getBooleanValue();
        }

    }

    public static class IntegerValueToBigInteger extends PJConverter {

        public static final IntegerValueToBigInteger INSTANCE = new IntegerValueToBigInteger();

        public Object convert(ValueRepresentation<? extends Item> value, Class targetClass, XPathContext context) throws XPathException {
            return ((IntegerValue)value).asBigInteger();
        }

    }

    public static class IntegerValueToLong extends PJConverter {

        public static final IntegerValueToLong INSTANCE = new IntegerValueToLong();

        public Object convert(ValueRepresentation<? extends Item> value, Class targetClass, XPathContext context) throws XPathException {
            IntegerValue iv = (IntegerValue)Value.asItem(value);
            assert iv != null;
            return iv.longValue();
        }

    }

    public static class IntegerValueToInt extends PJConverter {

        public static final IntegerValueToInt INSTANCE = new IntegerValueToInt();

        public Object convert(ValueRepresentation<? extends Item> value, Class targetClass, XPathContext context) throws XPathException {
            IntegerValue iv = (IntegerValue)Value.asItem(value);
            assert iv != null;
            return (int) iv.longValue();
        }

    }

     public static class IntegerValueToShort extends PJConverter {

        public static final IntegerValueToShort INSTANCE = new IntegerValueToShort();

        public Object convert(ValueRepresentation<? extends Item> value, Class targetClass, XPathContext context) throws XPathException {
            IntegerValue iv = (IntegerValue)Value.asItem(value);
            assert iv != null;
            return (short) iv.longValue();
        }

     }

    public static class IntegerValueToByte extends PJConverter {

        public static final IntegerValueToByte INSTANCE = new IntegerValueToByte();

        public Object convert(ValueRepresentation<? extends Item> value, Class targetClass, XPathContext context) throws XPathException {
            IntegerValue iv = (IntegerValue)Value.asItem(value);
            assert iv != null;
            return (byte) iv.longValue();
        }

    }

    public static class IntegerValueToChar extends PJConverter {

        public static final IntegerValueToChar INSTANCE = new IntegerValueToChar();

        public Object convert(ValueRepresentation<? extends Item> value, Class targetClass, XPathContext context) throws XPathException {
            IntegerValue iv = (IntegerValue)Value.asItem(value);
            assert iv != null;
            return (char) iv.longValue();
        }

    }

    public static class NumericValueToBigDecimal extends PJConverter {

        public static final NumericValueToBigDecimal INSTANCE = new NumericValueToBigDecimal();

        public Object convert(ValueRepresentation<? extends Item> value, Class targetClass, XPathContext context) throws XPathException {
            NumericValue nv = (NumericValue)Value.asItem(value);
            assert nv != null;
            return nv.getDecimalValue();
        }

    }

    public static class NumericValueToDouble extends PJConverter {

        public static final NumericValueToDouble INSTANCE = new NumericValueToDouble();

        public Object convert(ValueRepresentation<? extends Item> value, Class targetClass, XPathContext context) throws XPathException {
            NumericValue nv = (NumericValue)Value.asItem(value);
            assert nv != null;
            return nv.getDoubleValue();
        }

    }

    public static class NumericValueToFloat extends PJConverter {

        public static final NumericValueToFloat INSTANCE = new NumericValueToFloat();

        public Object convert(ValueRepresentation<? extends Item> value, Class targetClass, XPathContext context) throws XPathException {
            NumericValue nv = (NumericValue)Value.asItem(value);
            assert nv != null;
            return nv.getFloatValue();
        }

    }

    public static class AnyURIValueToURI extends PJConverter {

        public static final AnyURIValueToURI INSTANCE = new AnyURIValueToURI();

        public Object convert(ValueRepresentation<? extends Item> value, Class targetClass, XPathContext context) throws XPathException {
            try {
                return new URI(value.getStringValue());
            } catch (URISyntaxException err) {
                throw new XPathException("The anyURI value '" + value + "' is not an acceptable Java URI");
            }
        }

    }

    public static class AnyURIValueToURL extends PJConverter {

        public static final AnyURIValueToURL INSTANCE = new AnyURIValueToURL();

        public Object convert(ValueRepresentation<? extends Item> value, Class targetClass, XPathContext context) throws XPathException {
            try {
                return new URL(value.getStringValue());
            } catch (MalformedURLException err) {
                throw new XPathException("The anyURI value '" + value + "' is not an acceptable Java URL");
            }
        }

    }

    public static class QualifiedNameValueToQName extends PJConverter {

        public static final QualifiedNameValueToQName INSTANCE = new QualifiedNameValueToQName();

        public Object convert(ValueRepresentation<? extends Item> value, Class targetClass, XPathContext context) throws XPathException {
            QualifiedNameValue qv = (QualifiedNameValue)Value.asItem(value);
            assert qv != null;
            return qv.toJaxpQName();
        }

    }

    public static class CalendarValueToDate extends PJConverter {

        public static final CalendarValueToDate INSTANCE = new CalendarValueToDate();

        public Object convert(ValueRepresentation<? extends Item> value, Class targetClass, XPathContext context) throws XPathException {
            CalendarValue cv = (CalendarValue)Value.asItem(value);
            assert cv != null;
            return cv.getCalendar().getTime();
        }

    }

    public static class CalendarValueToCalendar extends PJConverter {

        public static final CalendarValueToCalendar INSTANCE = new CalendarValueToCalendar();

        public Object convert(ValueRepresentation<? extends Item> value, Class targetClass, XPathContext context) throws XPathException {
            CalendarValue cv = (CalendarValue)Value.asItem(value);
            assert cv != null;
            return cv.getCalendar();
        }

    }


    /**
     * Converter for use when the source object is an atomic value, but nothing more is known
     * statically.
     */

    public static class Atomic extends PJConverter {

        public static final Atomic INSTANCE = new Atomic();

        public Object convert(ValueRepresentation<? extends Item> value, Class targetClass, XPathContext context) throws XPathException {
            // TODO: not really worth separating from General
            AtomicValue item = (AtomicValue)Value.asItem(value);
            if (item == null) {
                return null;
            }
            Configuration config = context.getConfiguration();
            PJConverter converter = allocate(
                    config, item.getItemType(config.getTypeHierarchy()), StaticProperty.EXACTLY_ONE, targetClass);
            return converter.convert(item, targetClass, context);
        }
    }

    /**
     * General-purpose converter when nothing more specific is available.
     * (Provided largely as a transition aid)
     */

    public static class General extends PJConverter {

        public static final General INSTANCE = new General();

        public Object convert(ValueRepresentation<? extends Item> value, Class targetClass, XPathContext context)
                throws XPathException {
            Value val = Value.asValue(value).reduce();
            Configuration config = context.getConfiguration();
            PJConverter converter = allocate(
                    config, val.getItemType(config.getTypeHierarchy()), val.getCardinality(), targetClass);
            if (converter instanceof General) {
                converter = Identity.INSTANCE;
            }
            return converter.convert(val, targetClass, context);
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