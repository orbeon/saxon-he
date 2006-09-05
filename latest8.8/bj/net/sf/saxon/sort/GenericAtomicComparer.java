package net.sf.saxon.sort;
import net.sf.saxon.Configuration;
import net.sf.saxon.Platform;
import net.sf.saxon.type.Type;
import net.sf.saxon.expr.XPathContext;
import net.sf.saxon.value.AtomicValue;
import net.sf.saxon.value.CalendarValue;
import net.sf.saxon.value.StringValue;
import net.sf.saxon.value.UntypedAtomicValue;

import java.util.Comparator;

/**
 * A Comparator used for comparing atomic values of arbitrary item types. It encapsulates
 * a Collator that is used when the values to be compared are strings. It also supports
 * a separate method for testing equality of items, which can be used for data types that
 * are not ordered.
 *
 * @author Michael H. Kay
 *
 */

public class GenericAtomicComparer implements AtomicComparer, Comparator, java.io.Serializable {

    private Comparator collator;
    private XPathContext conversionContext;

    /**
     * Create an GenericAtomicComparer
     * @param collator the collation to be used
     * @param conversion a context, used when converting untyped atomic values to the target type.
     */

    public GenericAtomicComparer(Comparator collator, XPathContext conversion) {
        this.collator = collator;
        if (collator == null) {
            this.collator = CodepointCollator.getInstance();
        }
        this.conversionContext = conversion;
    }

    public GenericAtomicComparer(Comparator collator, Configuration config) {
        this.collator = collator;
        if (collator == null) {
            this.collator = CodepointCollator.getInstance();
        }
        this.conversionContext = config.getConversionContext();
    }

    /**
     * Factory method to make a GenericAtomicComparer for values of known types
     * @param type0 primitive type of the first operand
     * @param type1 primitive type of the second operand
     * @param collator the collation to be used, if any
     * @param config the configuration
     * @return a GenericAtomicComparer for values of known types
     */

    public static AtomicComparer makeAtomicComparer(int type0, int type1, Comparator collator, Configuration config) {
        if (type0 == type1) {
            switch (type0) {
                case Type.DATE_TIME:
                case Type.DATE:
                case Type.TIME:
                case Type.G_DAY:
                case Type.G_MONTH:
                case Type.G_YEAR:
                case Type.G_MONTH_DAY:
                case Type.G_YEAR_MONTH:
                    return new CalendarValueComparer(config);

                case Type.BOOLEAN:
                case Type.BASE64_BINARY:
                case Type.DAY_TIME_DURATION:
                case Type.YEAR_MONTH_DURATION:
                case Type.DURATION:
                case Type.HEX_BINARY:
                case Type.QNAME:
                case Type.NOTATION:
                    return new ComparableAtomicValueComparer();
            }
        }

        if (Type.isNumericPrimitiveType(type0) && Type.isNumericPrimitiveType(type1)) {
            return new ComparableAtomicValueComparer();
        }

        if ((type0 == Type.STRING || type0 == Type.UNTYPED_ATOMIC || type0 == Type.ANY_URI) &&
               (type1 == Type.STRING || type1 == Type.UNTYPED_ATOMIC || type1 == Type.ANY_URI)) {
            if (collator instanceof CodepointCollator) {
                return CodepointCollatingComparer.getInstance();
            } else {
                return new CollatingAtomicComparer(collator, config.getPlatform());
            }
        }
        return new GenericAtomicComparer(collator, config);
    }

    /**
    * Compare two AtomicValue objects according to the rules for their data type. UntypedAtomic
    * values are compared as if they were strings; if different semantics are wanted, the conversion
    * must be done by the caller.
    * @param a the first object to be compared. It is intended that this should be an instance
    * of AtomicValue, though this restriction is not enforced. If it is a StringValue, the
    * collator is used to compare the values, otherwise the value must implement the java.util.Comparable
    * interface.
    * @param b the second object to be compared. This must be comparable with the first object: for
    * example, if one is a string, they must both be strings.
    * @return <0 if a<b, 0 if a=b, >0 if a>b
    * @throws ClassCastException if the objects are not comparable
    */

    public int compare(Object a, Object b) {

        // System.err.println("Comparing " + a.getClass() + "(" + a + ") with " + b.getClass() + "(" + b + ") using " + collator);

        if (a instanceof AtomicValue && !((AtomicValue)a).hasBuiltInType()) {
            a = ((AtomicValue)a).getPrimitiveValue();
        }
        if (b instanceof AtomicValue && !((AtomicValue)b).hasBuiltInType()) {
            b = ((AtomicValue)b).getPrimitiveValue();
        }

        if (a instanceof UntypedAtomicValue) {
            return ((UntypedAtomicValue)a).compareTo(b, collator, conversionContext);
        } else if (b instanceof UntypedAtomicValue) {
            return -((UntypedAtomicValue)b).compareTo(a, collator, conversionContext);
        } else if (a instanceof CalendarValue && b instanceof CalendarValue) {
            return ((CalendarValue)a).compareTo((CalendarValue)b, conversionContext.getConfiguration());
        } else if (a instanceof Comparable) {
            return ((Comparable)a).compareTo(b);
        } else if (a instanceof StringValue) {
            return collator.compare(((StringValue)a).getStringValue(), ((StringValue)b).getStringValue());
        } else {
            throw new ClassCastException("Objects are not comparable (" + a.getClass() + ", " + b.getClass() + ')');
        }
    }

    /**
    * Compare two AtomicValue objects for equality according to the rules for their data type. UntypedAtomic
    * values are compared by converting to the type of the other operand.
    * @param a the first object to be compared. It is intended that this should be an instance
    * of AtomicValue, though this restriction is not enforced. If it is a StringValue, the
    * collator is used to compare the values, otherwise the value must implement the equals() method.
    * @param b the second object to be compared. This must be comparable with the first object: for
    * example, if one is a string, they must both be strings.
    * @return <0 if a<b, 0 if a=b, >0 if a>b
    * @throws ClassCastException if the objects are not comparable
    */

    public boolean comparesEqual(AtomicValue a, AtomicValue b) {
        // System.err.println("Comparing " + a.getClass() + ": " + a + " with " + b.getClass() + ": " + b);

        a = ((AtomicValue)a).getPrimitiveValue();
        b = ((AtomicValue)b).getPrimitiveValue();

        if (a instanceof UntypedAtomicValue) {
            return ((UntypedAtomicValue)a).compareTo(b, collator, conversionContext) == 0;
        } else if (b instanceof UntypedAtomicValue) {
            return ((UntypedAtomicValue)b).compareTo(a, collator, conversionContext) == 0;
        } else if (a instanceof StringValue) {
            return collator.compare(((StringValue)a).getStringValue(), ((StringValue)b).getStringValue()) == 0;
        } else if (a instanceof CalendarValue && b instanceof CalendarValue) {
            return ((CalendarValue)a).compareTo((CalendarValue)b, conversionContext.getConfiguration()) == 0;
//        } else if (a instanceof String) {
//            if (collator instanceof SubstringMatcher) {
//                return ((SubstringMatcher)collator).comparesEqual((String)a, (String)b);
//            } else {
//                return collator.compare(a, b) == 0;
//            }
        } else {
            return a.equals(b);
        }
    }

    /**
    * Get a comparison key for an object. This must satisfy the rule that if two objects are equal,
    * then their comparison keys are equal, and vice versa. There is no requirement that the
    * comparison keys should reflect the ordering of the underlying objects.
    */

    public ComparisonKey getComparisonKey(AtomicValue a) {

        if (a instanceof AtomicValue && !((AtomicValue)a).hasBuiltInType()) {
            a = ((AtomicValue)a).getPrimitiveValue();
        }

        if (a instanceof StringValue) {
            Platform platform = conversionContext.getConfiguration().getPlatform();
            if (platform.canReturnCollationKeys(collator)) {
                return new ComparisonKey(Type.STRING,
                        platform.getCollationKey(collator, ((StringValue)a).getStringValue()));
            } else {
                return new ComparisonKey(Type.STRING, ((StringValue)a).getStringValue());
            }
        } else {
            return new ComparisonKey(Type.STRING, a);
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
// The Initial Developer of the Original Code is Michael H. Kay
//
// Portions created by (your name) are Copyright (C) (your legal entity). All Rights Reserved.
//
// Contributor(s): none
//