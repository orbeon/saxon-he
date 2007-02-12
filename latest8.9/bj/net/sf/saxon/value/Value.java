package net.sf.saxon.value;
import net.sf.saxon.AugmentedSource;
import net.sf.saxon.Configuration;
import net.sf.saxon.Controller;
import net.sf.saxon.event.Builder;
import net.sf.saxon.event.PipelineConfiguration;
import net.sf.saxon.event.Sender;
import net.sf.saxon.event.SequenceReceiver;
import net.sf.saxon.expr.*;
import net.sf.saxon.functions.Aggregate;
import net.sf.saxon.om.*;
import net.sf.saxon.tinytree.TinyBuilder;
import net.sf.saxon.trans.DynamicError;
import net.sf.saxon.trans.SaxonErrorCode;
import net.sf.saxon.trans.XPathException;
import net.sf.saxon.type.*;

import javax.xml.transform.Source;
import javax.xml.transform.dom.DOMSource;
import java.io.Serializable;
import java.lang.reflect.Array;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.net.URI;
import java.net.URL;
import java.util.*;
import java.util.regex.Pattern;

/**
* A value is the result of an expression but it is also an expression in its own right.
* Note that every value can be regarded as a sequence - in many cases, a sequence of
* length one.
*/

public abstract class Value
        implements Serializable, SequenceIterable, ValueRepresentation, Comparable {

    /**
     * Static method to make a Value from a given Item (which may be either an AtomicValue
     * or a NodeInfo
     * @param val       The supplied value, or null, indicating the empty sequence.
     * @return          The supplied value, if it is a value, or a SingletonNode that
     *                  wraps the item, if it is a node. If the supplied value was null,
     *                  return an EmptySequence
     */

    public static Value asValue(ValueRepresentation val) {
        if (val instanceof Value) {
            return (Value)val;
        } else if (val == null) {
            return EmptySequence.getInstance();
        } else {
            return new SingletonNode((NodeInfo)val);
        }
    }

    /**
     * Static method to make an Item from a Value
     * @param value the value to be converted
     * @return null if the value is an empty sequence; or the only item in the value
     * if it is a singleton sequence
     * @throws XPathException if the Value contains multiple items
     */

    public static Item asItem(ValueRepresentation value) throws XPathException {
        if (value instanceof Item) {
            return (Item)value;
        } else {
            return ((Value)value).asItem();
        }
    }

    /**
     * Return the value in the form of an Item
     * @return the value in the form of an Item
     */

    public Item asItem() throws XPathException {
        SequenceIterator iter = iterate();
        Item item = iter.next();
        if (item == null) {
            return null;
        } else if (iter.next() != null) {
            throw new DynamicError("Attempting to access a sequence as a singleton item");
        } else {
            return item;
        }
    }

    /**
     * Static method to get a Value from an Item
     */

    public static Value fromItem(Item item) {
        if (item == null) {
            return EmptySequence.getInstance();
        } else if (item instanceof AtomicValue) {
            return (AtomicValue)item;
        } else {
            return new SingletonNode((NodeInfo)item);
        }
    }

    /**
     * Static method to get an Iterator over any ValueRepresentation (which may be either a Value
     * or a NodeInfo
     * @param val       The supplied value, or null, indicating the empty sequence.
     * @return          The supplied value, if it is a value, or a SingletonNode that
     *                  wraps the item, if it is a node. If the supplied value was null,
     *                  return an EmptySequence
     */

    public static SequenceIterator asIterator(ValueRepresentation val) throws XPathException {
        if (val instanceof Value) {
            return ((Value)val).iterate();
        } else if (val == null) {
            return EmptyIterator.getInstance();
        } else {
            return SingletonIterator.makeIterator((NodeInfo)val);
        }
    }

    /**
    * Static method to convert strings to doubles.
    * @param s the String to be converted
    * @return a double representing the value of the String
     * @throws NumberFormatException if the value cannot be converted
    */

    public static double stringToNumber(CharSequence s) throws NumberFormatException {
        // first try to parse simple numbers by hand (it's cheaper)
        int len = s.length();
        if (len < 9) {
            boolean useJava = false;
            long num = 0;
            int dot = -1;
            int lastDigit = -1;
            boolean onlySpaceAllowed = false;
            loop: for (int i=0; i<s.length(); i++) {
                char c = s.charAt(i);
                switch (c) {
                    case ' ':
                    case '\n':
                    case '\t':
                    case '\r':
                        if (lastDigit != -1) {
                            onlySpaceAllowed = true;
                        }
                        break;
                    case '0': case '1': case '2': case '3': case '4':
                    case '5': case '6': case '7': case '8': case '9':
                        if (onlySpaceAllowed) {
                            throw new NumberFormatException("Numeric value contains embedded whitespace");
                        }
                        lastDigit = i;
                        num = num*10 + (c - '0');
                        break;
                    case '.':
                        if (onlySpaceAllowed) {
                            throw new NumberFormatException("Numeric value contains embedded whitespace");
                        }
                        if (dot != -1) {
                            throw new NumberFormatException("Only one decimal point allowed");
                        }
                        dot = i;
                        break;
                    default:
                        // there's something like a sign or an exponent: take the slow train instead
                        useJava = true;
                        break loop;
                }
            }
            if (!useJava) {
                if (lastDigit == -1) {
                    throw new NumberFormatException("No digits found");
                } else if (dot == -1 || dot > lastDigit) {
                    return (double)num;
                } else {
                    int afterPoint = lastDigit - dot;
                    return ((double)num)/powers[afterPoint];
                }
            }
        }
        String n = Whitespace.trimWhitespace(s).toString();
        if ("INF".equals(n)) {
            return Double.POSITIVE_INFINITY;
        } else if ("-INF".equals(n)) {
            return Double.NEGATIVE_INFINITY;
        } else if ("NaN".equals(n)) {
            return Double.NaN;
        } else if (!doublePattern.matcher(n).matches()) {
            // Need to disallow values that are OK in Java but not in XPath, specifically
            // - special values like +NaN or -Infinity
            // - hex digits
            // - binary exponents
            // TODO: this checking incurs a performance hit. Perhaps we should do the whole conversion in-house
            throw new NumberFormatException("Invalid characters in float/double value");
        } else {
            return Double.parseDouble(n);
        }
    }

    private static double[] powers = new double[]{1, 10, 100, 1000, 10000, 100000, 1000000, 10000000, 100000000};
    private static Pattern doublePattern = Pattern.compile("^[0-9.eE+-]+$");

    /**
     * Get a SequenceIterator over a ValueRepresentation
     */

    public static SequenceIterator getIterator(ValueRepresentation val) throws XPathException {
        if (val instanceof Value) {
            return ((Value)val).iterate();
        } else if (val instanceof NodeInfo) {
            return SingletonIterator.makeIterator((NodeInfo)val);
        } else if (val == null) {
            throw new AssertionError("Value of variable is undefined (null)");
        } else {
            throw new AssertionError("Unknown value representation " + val.getClass());
        }
    }

    /**
     * Iterate over the items contained in this value.
     * @return an iterator over the sequence of items
     * @throws XPathException if a dynamic error occurs. This is possible only in the case of values
     * that are materialized lazily, that is, where the iterate() method leads to computation of an
     * expression that delivers the values.
     */

    public abstract SequenceIterator iterate() throws XPathException;

    /**
     * Return an iterator over the results of evaluating an expression
     * @param context the dynamic evaluation context (not used in this implementation)
     * @return an iterator over the items delivered by the expression
     */

    public final SequenceIterator iterate(XPathContext context) throws XPathException {
        return iterate();
    }

    /**
     * Get the value of the item as a CharSequence. This is in some cases more efficient than
     * the version of the method that returns a String.
     */

    public CharSequence getStringValueCS() throws XPathException {
        return getStringValue();
    }

    /**
     * Get the canonical lexical representation as defined in XML Schema. This is not always the same
     * as the result of casting to a string according to the XPath rules.
     */

    public CharSequence getCanonicalLexicalRepresentation() {
        try {
            return getStringValueCS();
        } catch (XPathException err) {
            throw new IllegalStateException("Failed to get canonical lexical representation: " + err.getMessage());
        }
    }

    /**
     * Determine the data type of the items in the expression, if possible
     * @return for the default implementation: AnyItemType (not known)
     * @param th The TypeHierarchy. Can be null if the target is an AtomicValue.
     */

    public ItemType getItemType(TypeHierarchy th) {
        return AnyItemType.getInstance();
    }

    /**
     * Determine the cardinality
     */

    public int getCardinality() {
        try {
            SequenceIterator iter = iterate();
            Item next = iter.next();
            if (next == null) {
                return StaticProperty.EMPTY;
            } else {
                if (iter.next() != null) {
                    return StaticProperty.ALLOWS_ONE_OR_MORE;
                } else {
                    return StaticProperty.EXACTLY_ONE;
                }
            }
        } catch (XPathException err) {
            // can't actually happen
            return StaticProperty.ALLOWS_ZERO_OR_MORE;
        }
    }

    /**
     * Determine whether the value is multivalued, that is, whether it is a sequence that
     * potentially contains more than one item
     * @return true if the value might contain more than one item, false if it definitely
     * contains zero or one items
     */

    public abstract boolean isMultiValued();

    /**
     * Get the n'th item in the sequence (starting from 0). This is defined for all
     * Values, but its real benefits come for a sequence Value stored extensionally
     * (or for a MemoClosure, once all the values have been read)
     * @param n position of the required item, counting from zero.
     * @return the n'th item in the sequence, where the first item in the sequence is
     * numbered zero. If n is negative or >= the length of the sequence, returns null.
     */

    public Item itemAt(int n) throws XPathException {
        if (n < 0) {
            return null;
        }
        int i = 0;        // indexing is zero-based
        SequenceIterator iter = iterate();
        while (true) {
            Item item = iter.next();
            if (item == null) {
                return null;
            }
            if (i++ == n) {
                return item;
            }
        }
    }

    /**
     * Get the length of the sequence
     */

    public int getLength() throws XPathException {
        return Aggregate.count(iterate());
    }

    /**
      * Process the value as an instruction, without returning any tail calls
      * @param context The dynamic context, giving access to the current node,
      * the current variables, etc.
      */

    public void process(XPathContext context) throws XPathException {
        SequenceIterator iter = iterate();
        SequenceReceiver out = context.getReceiver();
        while (true) {
            Item it = iter.next();
            if (it==null) break;
            out.append(it, 0, NodeInfo.ALL_NAMESPACES);
        }
    }


    /**
     * Convert the value to a string, using the serialization rules.
     * For atomic values this is the same as a cast; for sequence values
     * it gives a space-separated list.
     * @throws XPathException The method can fail if evaluation of the value
     * has been deferred, and if a failure occurs during the deferred evaluation.
     * No failure is possible in the case of an AtomicValue.
     */

    public String getStringValue() throws XPathException {
        FastStringBuffer sb = new FastStringBuffer(1024);
        SequenceIterator iter = iterate();
        Item item = iter.next();
        if (item != null) {
            while (true) {
                sb.append(item.getStringValueCS());
                item = iter.next();
                if (item == null) {
                    break;
                }
                sb.append(' ');
            }
        }
        return sb.toString();
    }


    /**
     * Get the effective boolean value of the expression. This returns false if the value
     * is the empty sequence, a zero-length string, a number equal to zero, or the boolean
     * false. Otherwise it returns true.
     *
     * @exception XPathException if any dynamic error occurs evaluating the
     *     expression
     * @return the effective boolean value
     */

    public boolean effectiveBooleanValue() throws XPathException {
        return ExpressionTool.effectiveBooleanValue(iterate());
    }

    /**
     * Get a Comparable value that implements the XML Schema ordering comparison semantics for this value.
     * The default implementation returns "this", which ensures that comparisons between list values are handled
     * by the equals() and compareTo() methods on this class itself. This is overridden for atomic values.
     *
     * <p>In the case of data types that are partially ordered, the returned Comparable extends the standard
     * semantics of the compareTo() method by returning the value {@link #INDETERMINATE_ORDERING} when there
     * is no defined order relationship between two given values.</p>
     */

    public Comparable getSchemaComparable() {
        return this;
    }

    /**
     * Constant returned by compareTo() method to indicate an indeterminate ordering between two values
     */

    public static final int INDETERMINATE_ORDERING = Integer.MIN_VALUE;

    /**
     * Compare two (sequence) values for equality. This supports identity constraints in XML Schema,
     * which allow list-valued elements and attributes to participate in key and uniqueness constraints.
     * This method returns false if any error occurs during the comparison, or if any of the items
     * in either sequence is a node rather than an atomic value.
     */

    public boolean equals(Object obj) {
        // TODO: this reports a sequence of length one as being equal to a singleton. It's not clear this is correct.
        // Tests such as idc_NMTOKENS_Name are relevant. Saxon 8.8 reports equal, 8.7.3 reported not equal.
        try {
            if (obj instanceof Value) {
                SequenceIterator iter1 = iterate();
                SequenceIterator iter2 = ((Value)obj).iterate();
                while (true) {
                    Item item1 = iter1.next();
                    Item item2 = iter2.next();
                    if (item1 == null && item2 == null) {
                        return true;
                    }
                    if (item1 == null || item2 == null) {
                        return false;
                    }
                    if (item1 instanceof NodeInfo || item2 instanceof NodeInfo) {
                        return false;
                    }
                    if (!((AtomicValue)item1).getSchemaComparable().equals(((AtomicValue)item2).getSchemaComparable())) {
                        return false;
                    }
                }
            } else {
                return false;
            }
        } catch (XPathException e) {
            return false;
        }
    }

    /**
     * Return a hash code to support the equals() function
     */

    public int hashCode() {
        try {
            int hash = 0x06639662;  // arbitrary seed
            SequenceIterator iter = iterate();
            while (true) {
                Item item = iter.next();
                if (item == null) {
                    return hash;
                }
                hash ^= item.hashCode();
            }
        } catch (XPathException e) {
            return 0;
        }
    }

    /**
     * Compares this object with the specified object for order. This implements the comparison semantics
     * for XML Schema ordering facets such as minInclusive etc
     */
    public int compareTo(Object o) {
        return INDETERMINATE_ORDERING;
    }


    /**
     * Check statically that the results of the expression are capable of constructing the content
     * of a given schema type.
     * @param parentType The schema type
     * @param env the static context
     * @param whole
     * @throws XPathException if the expression doesn't match the required content type
     */

    public void checkPermittedContents(SchemaType parentType, StaticContext env, boolean whole) throws XPathException {
        return;
    }

    /**
     * Reduce a value to its simplest form. If the value is a closure or some other form of deferred value
     * such as a FunctionCallPackage, then it is reduced to a SequenceExtent. If it is a SequenceExtent containing
     * a single item, then it is reduced to that item. One consequence that is exploited by class FilterExpression
     * is that if the value is a singleton numeric value, then the result will be an instance of NumericValue
     */

    public Value reduce() throws XPathException {
        return this;
    }

    /**
     * Convert to Java object (for passing to external functions)
     */

    public Object convertToJava(Class target, XPathContext context) throws XPathException {

        if (target == Object.class) {
            List list = new ArrayList(20);
            return convertToJavaList(list, context);
        }

        // See if the extension function is written to accept native Saxon objects

        if (target.isAssignableFrom(this.getClass())) {
            return this;
        } else if (target.isAssignableFrom(SequenceIterator.class)) {
            return iterate();
        }

        // Offer the object to registered external object models

        if ((this instanceof ObjectValue || !(this instanceof AtomicValue)) && !(this instanceof EmptySequence)) {
            List externalObjectModels = context.getConfiguration().getExternalObjectModels();
            for (int m=0; m<externalObjectModels.size(); m++) {
                ExternalObjectModel model = (ExternalObjectModel)externalObjectModels.get(m);
                Object object = model.convertXPathValueToObject(this, target, context);
                if (object != null) {
                    return object;
                }
            }
        }

        if (Collection.class.isAssignableFrom(target)) {
            Collection list;
            if (target.isAssignableFrom(ArrayList.class)) {
                list = new ArrayList(100);
            } else {
                try {
                    list = (Collection)target.newInstance();
                } catch (InstantiationException e) {
                    DynamicError de = new DynamicError("Cannot instantiate collection class " + target);
                    de.setXPathContext(context);
                    throw de;
                } catch (IllegalAccessException e) {
                    DynamicError de = new DynamicError("Cannot access collection class " + target);
                    de.setXPathContext(context);
                    throw de;
                }
            }
            return convertToJavaList(list, context);
        } else if (target.isArray()) {
            Class component = target.getComponentType();
            if (component.isAssignableFrom(Item.class) ||
                    component.isAssignableFrom(NodeInfo.class) ||
                    component.isAssignableFrom(DocumentInfo.class)) {
                Value extent = this;
                if (extent instanceof Closure) {
                    extent = Value.asValue(SequenceExtent.makeSequenceExtent(extent.iterate()));
                }
                int length = extent.getLength();
                Object array = Array.newInstance(component, length);
                SequenceIterator iter = extent.iterate();
                for (int i=0; i<length; i++) {
                    Item item = iter.next();
                    try {
                        Array.set(array, i, item);
                    } catch (IllegalArgumentException err) {
                        DynamicError d = new DynamicError(
                                "Item " + i + " in supplied sequence cannot be converted " +
                                "to the component type of the Java array (" + component + ')', err);
                        d.setErrorCode(SaxonErrorCode.SXJE0023);
                        d.setXPathContext(context);
                        throw d;
                    }
                }
                return array;
            } else /* if (!(this instanceof AtomicValue)) */ {
                // try atomizing the sequence, unless this is a single atomic value, in which case we've already
                // tried that.
                SequenceIterator it = Atomizer.getAtomizingIterator(iterate());
                int length;
                if ((it.getProperties() & SequenceIterator.LAST_POSITION_FINDER) == 0) {
                    SequenceExtent extent = new SequenceExtent(it);
                    length = extent.getLength();
                    it = extent.iterate();
                } else {
                    length = ((LastPositionFinder)it).getLastPosition();
                }
                Object array = Array.newInstance(component, length);
                for (int i=0; i<length; i++) {
                    try {
                        AtomicValue val = (AtomicValue)it.next();
                        Object jval = val.convertToJava(component, context);
                        Array.set(array, i, jval);
                    } catch (XPathException err) {
                        DynamicError d = new DynamicError(
                                "Cannot convert item in atomized sequence to the component type of the Java array", err);
                        d.setErrorCode(SaxonErrorCode.SXJE0023);
                        d.setXPathContext(context);
                        throw d;
                    }
                }
                return array;
            }

        } else if (target.isAssignableFrom(Item.class) ||
                target.isAssignableFrom(NodeInfo.class) ||
                target.isAssignableFrom(DocumentInfo.class)) {

            // try passing the first item in the sequence provided it is the only one
            SequenceIterator iter = iterate();
            Item first = null;
            while (true) {
                Item next = iter.next();
                if (next == null) {
                    break;
                }
                if (first != null) {
                    DynamicError err = new DynamicError(
                            "Sequence contains more than one value; Java method expects only one",
                            SaxonErrorCode.SXJE0022);
                    err.setXPathContext(context);
                    throw err;
                }
                first = next;
            }
            if (first == null) {
                // sequence is empty; pass a Java null
                return null;
            }
            if (target.isAssignableFrom(first.getClass())) {
                // covers Item and NodeInfo
                return first;
            }

            Object n = first;
            while (n instanceof VirtualNode) {
                // If we've got a wrapper around a DOM or JDOM node, and the user wants a DOM
                // or JDOM node, we unwrap it
                Object vn = ((VirtualNode) n).getUnderlyingNode();
                if (target.isAssignableFrom(vn.getClass())) {
                    return vn;
                } else {
                    n = vn;
                }
            }

            throw new DynamicError(
                    "Cannot convert supplied XPath value to the required type for the extension function",
                    SaxonErrorCode.SXJE0021);
        } else if (!(this instanceof AtomicValue)) {
            // try atomizing the value, unless this is an atomic value, in which case we've already tried that
            SequenceIterator it = Atomizer.getAtomizingIterator(iterate());
            Item first = null;
            while (true) {
                Item next = it.next();
                if (next == null) {
                    break;
                }
                if (first != null) {
                    DynamicError err = new DynamicError(
                            "Sequence contains more than one value; Java method expects only one",
                            SaxonErrorCode.SXJE0022);
                    err.setXPathContext(context);
                    throw err;
                }
                first = next;
            }
            if (first == null) {
                // sequence is empty; pass a Java null
                return null;
            }
            if (target.isAssignableFrom(first.getClass())) {
                return first;
            } else {
                return ((AtomicValue)first).convertToJava(target, context);
            }
        } else {
            throw new DynamicError("Cannot convert supplied XPath value to the required type for the extension function",
                    SaxonErrorCode.SXJE0021);
        }
    }
    
    /**
     * Convert this XPath value to a Java collection
     * @param list an empty Collection, to which the relevant values will be added
     * @param context the evaluation context
     * @return the supplied list, with relevant values added
     * @throws XPathException
     */

    private Collection convertToJavaList(Collection list, XPathContext context) throws XPathException {
        // TODO: with JDK 1.5, check to see if the item type of the list is constrained
        SequenceIterator iter = iterate();
        while (true) {
            Item it = iter.next();
            if (it == null) {
                return list;
            }
            if (it instanceof AtomicValue) {
                list.add(((AtomicValue)it).convertToJava(Object.class, context));
            } else if (it instanceof VirtualNode) {
                list.add(((VirtualNode)it).getUnderlyingNode());
            } else {
                list.add(it);
            }
        }
    }

    /**
    * Convert a Java object to an XPath value. This method is called to handle the result
    * of an external function call, and also to process global parameters passed to the stylesheet or query.
    * @param object The Java object to be converted
    * @param requiredType The required type of the result (if known)
    * @param context
     * @return the result of converting the value. If the value is null, returns null.
    */

    public static Value convertJavaObjectToXPath(Object object, SequenceType requiredType, XPathContext context)
                                          throws XPathException {

        Configuration config= context.getConfiguration();
        ItemType requiredItemType = requiredType.getPrimaryType();

        // TODO: make more use of the requiredType, e.g. to decide what to convert a Date into.

        if (object==null) {
            return EmptySequence.getInstance();
        }

        // Offer the object to all the registered external object models

        List externalObjectModels = config.getExternalObjectModels();
        for (int m=0; m<externalObjectModels.size(); m++) {
            ExternalObjectModel model = (ExternalObjectModel)externalObjectModels.get(m);
            Value val = model.convertObjectToXPathValue(object, config);
            if (val != null && TypeChecker.testConformance(val, requiredType, context) == null) {
                return val;
            }
        }

        if (requiredItemType instanceof ExternalObjectType) {
            Class theClass = ((ExternalObjectType)requiredItemType).getJavaClass();
            if (theClass.isAssignableFrom(object.getClass())) {
                return new ObjectValue(object, (ExternalObjectType)requiredItemType);
            } else {
                throw new DynamicError("Supplied parameter value is not of class " + theClass.getName());
            }
        }

        Value value = convertToBestFit(object, config);
        return value;

    }

    private static Value convertToBestFit(Object object, Configuration config) throws XPathException {
        if (object instanceof String) {
            return StringValue.makeStringValue((String)object);

        } else if (object instanceof Character) {
            return new StringValue(object.toString());

        } else if (object instanceof Boolean) {
            return BooleanValue.get(((Boolean)object).booleanValue());

        } else if (object instanceof Double) {
            return new DoubleValue(((Double)object).doubleValue());

        } else if (object instanceof Float) {
            return new FloatValue(((Float)object).floatValue());

        } else if (object instanceof Short) {
            return new Int64Value(((Short)object).shortValue(), BuiltInAtomicType.SHORT, false);
        } else if (object instanceof Integer) {
            return new Int64Value(((Integer)object).intValue(), BuiltInAtomicType.INT, false);
        } else if (object instanceof Long) {
            return new Int64Value(((Long)object).longValue(), BuiltInAtomicType.LONG, false);
        } else if (object instanceof Byte) {
            return new Int64Value(((Byte)object).byteValue(), BuiltInAtomicType.BYTE, false);

        } else if (object instanceof BigInteger) {
            return BigIntegerValue.makeIntegerValue(((BigInteger)object));

        } else if (object instanceof BigDecimal) {
            return new DecimalValue(((BigDecimal)object));

//        } else if (object instanceof QName) {
//            return new QNameValue((QName)object);
            // TODO: reinstate above lines in JDK 1.5
        } else if (object.getClass().getName().equals("javax.xml.namespace.QName")) {
            return makeQNameValue(object, config);

        } else if (object instanceof URI) {
            return new AnyURIValue(object.toString());

        } else if (object instanceof URL) {
            return new AnyURIValue(object.toString());

        } else if (object instanceof Date) {
            return DateTimeValue.fromJavaDate((Date)object);

        // TODO: recognize GregorianCalendar...

        } else if (object instanceof Closure) {
            // Force eager evaluation, because of problems with side-effects.
            // (The value might depend on data that is mutable.)
            //return Value.asValue(ExpressionTool.evaluate((Closure)object, ExpressionTool.ITERATE_AND_MATERIALIZE, config.getConversionContext(), 10));
            return Value.asValue(
                    SequenceExtent.makeSequenceExtent(((Closure)object).iterate()));
        } else if (object instanceof Value) {
            return (Value)object;

        } else if (object instanceof NodeInfo) {
            if (((NodeInfo)object).getConfiguration() != config) {
                throw new DynamicError("Externally-supplied NodeInfo belongs to wrong Configuration",
                        SaxonErrorCode.SXXP0004);
            }
            return new SingletonNode((NodeInfo)object);

        } else if (object instanceof SequenceIterator) {
            return Closure.makeIteratorClosure((SequenceIterator)object);

        } else if (object instanceof List) {
            Item[] array = new Item[((List)object).size()];
            int a = 0;
            for (Iterator i=((List)object).iterator(); i.hasNext(); ) {
                Object obj = i.next();
                if (obj instanceof NodeInfo) {
                    if (((NodeInfo)obj).getConfiguration() != config) {
                        throw new DynamicError("Externally-supplied NodeInfo belongs to wrong Configuration",
                        SaxonErrorCode.SXXP0004);
                    }
                    array[a++] = (NodeInfo)obj;
                } else {
                    Value v = convertToBestFit(obj, config);
                    if (v!=null) {
                        if (v instanceof Item) {
                            array[a++] = (Item)v;
                        } else if (v instanceof EmptySequence) {
                            // no action
                        } else if (v instanceof SingletonNode) {
                            NodeInfo node = ((SingletonNode)v).getNode();
                            if (node != null) {
                                array[a++] = node;
                            }
                        } else {
                            throw new DynamicError(
                                    "Returned List contains an object that cannot be converted to an Item (" + obj.getClass() + ')',
                                    SaxonErrorCode.SXJE0051);
                        }
                    }
                }
            }

            return new SequenceExtent(array);

        } else if (object instanceof Object[]) {
            Object[] arrayObject = (Object[])object;
            Item[] newArray = new Item[arrayObject.length];
             int a = 0;
             for (int i = 0; i < arrayObject.length; i++){
                 Object itemObject = arrayObject[i];
                 if (itemObject instanceof NodeInfo) {
                     if (((NodeInfo)itemObject).getConfiguration() != config) {
                         throw new DynamicError("Externally-supplied NodeInfo belongs to wrong Configuration",
                            SaxonErrorCode.SXXP0004);
                     }
                     newArray[a++] = (NodeInfo)itemObject;
                 } else if (itemObject != null) {
                     Value v = convertToBestFit(itemObject, config);
                     if (v!=null) {
                         if (v instanceof Item) {
                             newArray[a++] = (Item)v;
                         } else {
                             throw new DynamicError(
                                     "Returned array contains an object that cannot be converted to an Item (" +
                                            itemObject.getClass() + ')',
                                     SaxonErrorCode.SXJE0051);
                         }
                     }
                 }
             }
             return new SequenceExtent(newArray, 0, a);

        } else if (object instanceof long[]) {
             Item[] array = new Item[((long[])object).length];
             for (int i = 0; i < array.length; i++){
                 array[i] = Int64Value.makeIntegerValue(((long[])object)[i]);
             }
             return new SequenceExtent(array);

        } else if (object instanceof int[]) {
             Item[] array = new Item[((int[])object).length];
             for (int i = 0; i < array.length; i++){
                 array[i] = Int64Value.makeIntegerValue(((int[])object)[i]);
             }
             return new SequenceExtent(array);

        } else if (object instanceof short[]) {
             Item[] array = new Item[((short[])object).length];
             for (int i = 0; i < array.length; i++){
                 array[i] = Int64Value.makeIntegerValue(((short[])object)[i]);
             }
             return new SequenceExtent(array);

        } else if (object instanceof byte[]) {  // interpret this as unsigned bytes
             Item[] array = new Item[((byte[])object).length];
             for (int i = 0; i < array.length; i++){
                 array[i] = Int64Value.makeIntegerValue(255 & (int)((byte[])object)[i]);
             }
             return new SequenceExtent(array);

        } else if (object instanceof char[]) {
             return StringValue.makeStringValue(new String((char[])object));

        } else if (object instanceof boolean[]) {
             Item[] array = new Item[((boolean[])object).length];
             for (int i = 0; i < array.length; i++){
                 array[i] = BooleanValue.get(((boolean[])object)[i]);
             }
             return new SequenceExtent(array);

        } else if (object instanceof double[]) {
             Item[] array = new Item[((double[])object).length];
             for (int i = 0; i < array.length; i++){
                 array[i] = new DoubleValue(((double[])object)[i]);
             }
             return new SequenceExtent(array);

        } else if (object instanceof float[]) {
             Item[] array = new Item[((float[])object).length];
             for (int i = 0; i < array.length; i++){
                 array[i] = new FloatValue(((float[])object)[i]);
             }
             return new SequenceExtent(array);

        } else if (object instanceof Source && config != null) {
            if (object instanceof DOMSource) {
                NodeInfo node = Controller.unravel((Source)object, config);
                if (node.getConfiguration() != config) {
                    throw new DynamicError("Externally-supplied DOM Node belongs to wrong Configuration",
                            SaxonErrorCode.SXXP0004);
                }
                return new SingletonNode(node);
            }
            try {
                Builder b = new TinyBuilder();
                PipelineConfiguration pipe = config.makePipelineConfiguration();
                b.setPipelineConfiguration(pipe);
                new Sender(pipe).send((Source)object, b);
                if (object instanceof AugmentedSource && ((AugmentedSource)object).isPleaseCloseAfterUse()) {
                     ((AugmentedSource)object).close();
                }
                return new SingletonNode(b.getCurrentRoot());
            } catch (XPathException err) {
                throw new DynamicError(err);
            }
        } else {
            // See whether this is an object representing a Node in some recognized object model
            ExternalObjectModel model = config.findExternalObjectModel(object);
            if (model != null) {
                DocumentInfo doc = model.wrapDocument(object, "", config);
                NodeInfo node = model.wrapNode(doc, object);
                return Value.asValue(node);
            }
        }
        return new ObjectValue(object);
    }

    /**
     * Temporary method to make a QNameValue from a JAXP 1.3 QName, without creating a compile-time link
     * to the JDK 1.5 QName class
     * @param object an instance of javax.xml.namespace.QName
     * @return a corresponding Saxon QNameValue, or null if any error occurs performing the conversion
     */

    public static QNameValue makeQNameValue(Object object, Configuration config) {
        try {
            Class qnameClass = config.getClass("javax.xml.namespace.QName", false, null);
            Class[] args = EMPTY_CLASS_ARRAY;
            Method getPrefix = qnameClass.getMethod("getPrefix", args);
            Method getLocalPart = qnameClass.getMethod("getLocalPart", args);
            Method getNamespaceURI = qnameClass.getMethod("getNamespaceURI", args);
            String prefix = (String)getPrefix.invoke(object, (Object[])args);
            String localPart = (String)getLocalPart.invoke(object, (Object[])args);
            String uri = (String)getNamespaceURI.invoke(object, (Object[])args);
            return new QNameValue(prefix, uri, localPart, BuiltInAtomicType.QNAME, config.getNameChecker());
        } catch (XPathException e) {
            return null;
        } catch (NoSuchMethodException e) {
            return null;
        } catch (IllegalAccessException e) {
            return null;
        } catch (InvocationTargetException e) {
            return null;
        }
    }

    public static final Class[] EMPTY_CLASS_ARRAY = new Class[0];

    /**
     * Convert to a string for diagnostic output
     */

    public String toString() {
        try {
            return getStringValue();
        } catch (XPathException err) {
            return super.toString();
        }
    }

    /**
     * Internal method to convert an XPath value to a Java object.
     * An atomic value is returned as an instance
     * of the best available Java class. If the item is a node, the node is "unwrapped",
     * to return the underlying node in the original model (which might be, for example,
     * a DOM or JDOM node).
    */

    public static Object convert(Item item) throws XPathException {
        if (item instanceof NodeInfo) {
            Object node = item;
            while (node instanceof VirtualNode) {
                // strip off any layers of wrapping
                node = ((VirtualNode)node).getUnderlyingNode();
            }
            return node;
        } else if (item instanceof ObjectValue) {
            return ((ObjectValue)item).getObject();
        } else {
            AtomicValue value = (AtomicValue)item;
            switch (value.getItemType(null).getPrimitiveType()) {
                case Type.STRING:
                case Type.UNTYPED_ATOMIC:
                case Type.ANY_URI:
                case Type.DURATION:
                    return value.getStringValue();
                case Type.BOOLEAN:
                    return (((BooleanValue)value).getBooleanValue() ? Boolean.TRUE : Boolean.FALSE );
                case Type.DECIMAL:
                    return ((DecimalValue)value).getDecimalValue();
                case Type.INTEGER:
                    return new Long(((NumericValue)value).longValue());
                case Type.DOUBLE:
                    return new Double(((DoubleValue)value).getDoubleValue());
                case Type.FLOAT:
                    return new Float(((FloatValue)value).getFloatValue());
                case Type.DATE_TIME:
                    return ((DateTimeValue)value).getCalendar().getTime();
                case Type.DATE:
                    return ((DateValue)value).getCalendar().getTime();
                case Type.TIME:
                    return value.getStringValue();
                case Type.BASE64_BINARY:
                    return ((Base64BinaryValue)value).getBinaryValue();
                case Type.HEX_BINARY:
                    return ((HexBinaryValue)value).getBinaryValue();
                default:
                    return item;
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
// The Initial Developer of the Original Code is Michael H. Kay.
//
// Portions created by (your name) are Copyright (C) (your legal entity). All Rights Reserved.
//
// Contributor(s): none.
//
