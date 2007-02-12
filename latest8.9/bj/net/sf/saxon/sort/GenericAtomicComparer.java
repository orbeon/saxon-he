package net.sf.saxon.sort;
import net.sf.saxon.Configuration;
import net.sf.saxon.Platform;
import net.sf.saxon.expr.XPathContext;
import net.sf.saxon.type.Type;
import net.sf.saxon.type.BuiltInAtomicType;
import net.sf.saxon.value.AtomicValue;
import net.sf.saxon.value.CalendarValue;
import net.sf.saxon.value.StringValue;
import net.sf.saxon.value.UntypedAtomicValue;

import java.io.Serializable;

/**
 * An AtomicComparer used for comparing atomic values of arbitrary item types. It encapsulates
 * a Collator that is used when the values to be compared are strings. It also supports
 * a separate method for testing equality of items, which can be used for data types that
 * are not ordered.
 *
 * @author Michael H. Kay
 *
 */

public class GenericAtomicComparer implements AtomicComparer, Serializable {

    private StringCollator collator;
    private XPathContext conversionContext;

    /**
     * Create an GenericAtomicComparer
     * @param collator the collation to be used
     * @param conversion a context, used when converting untyped atomic values to the target type.
     */

    public GenericAtomicComparer(StringCollator collator, XPathContext conversion) {
        this.collator = collator;
        if (collator == null) {
            this.collator = CodepointCollator.getInstance();
        }
        this.conversionContext = conversion;
    }

    public GenericAtomicComparer(StringCollator collator, Configuration config) {
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
     * @param collator the collation to be used, if any. This is supplied as a NamedCollation object
     * which encapsulated both the collation URI and the collation itself.
     * @param config the configuration
     * @return a GenericAtomicComparer for values of known types
     */

    public static AtomicComparer makeAtomicComparer(
            BuiltInAtomicType type0, BuiltInAtomicType type1, StringCollator collator, Configuration config) {
        int fp0 = type0.getFingerprint();
        int fp1 = type1.getFingerprint();
        if (fp0 == fp1) {
            switch (fp0) {
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
                case Type.DAY_TIME_DURATION:
                case Type.YEAR_MONTH_DURATION:
                    return ComparableAtomicValueComparer.getInstance();

                case Type.BASE64_BINARY:
                case Type.HEX_BINARY:
                case Type.QNAME:
                case Type.NOTATION:
                    return EqualityComparer.getInstance();

            }
        }

        if (type0.isPrimitiveNumeric() && type1.isPrimitiveNumeric()) {
            return ComparableAtomicValueComparer.getInstance();
        }

        if ((type0.equals(BuiltInAtomicType.STRING) ||
                type0.equals(BuiltInAtomicType.UNTYPED_ATOMIC) ||
                type0.equals(BuiltInAtomicType.ANY_URI)) &&
               (type1.equals(BuiltInAtomicType.STRING) ||
                type1.equals(BuiltInAtomicType.UNTYPED_ATOMIC) ||
                type1.equals(BuiltInAtomicType.ANY_URI))) {
            if (collator instanceof CodepointCollator) {
                return CodepointCollatingComparer.getInstance();
            } else {
                return new CollatingAtomicComparer(collator, Configuration.getPlatform());
            }
        }
        return new GenericAtomicComparer(collator, config);
    }

    /**
     * Get the underlying string collator
     */

    public StringCollator getStringCollator() {
        return collator;
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

    public int compareAtomicValues(AtomicValue a, AtomicValue b) {

        // System.err.println("Comparing " + a.getClass() + "(" + a + ") with " + b.getClass() + "(" + b + ") using " + collator);

        if (a == null) {
            return (b == null ? 0 : -1);
        } else if (b == null) {
            return +1;
        }

        if (a instanceof UntypedAtomicValue) {
            return ((UntypedAtomicValue)a).compareTo(b, collator, conversionContext);
        } else if (b instanceof UntypedAtomicValue) {
            return -((UntypedAtomicValue)b).compareTo(a, collator, conversionContext);
        } else if (a instanceof CalendarValue && b instanceof CalendarValue) {
            return ((CalendarValue)a).compareTo((CalendarValue)b, conversionContext.getConfiguration());
        } else if (a instanceof StringValue) {
            return collator.compareStrings(((StringValue)a).getStringValue(), ((StringValue)b).getStringValue());
        } else {
            Comparable ac = a.getXPathComparable();
            Comparable bc = b.getXPathComparable();
            if (ac == null || bc == null) {
                if (a instanceof AtomicValue && b instanceof AtomicValue) {
                    throw new ClassCastException("Objects are not comparable (" +
                            Type.displayTypeName((AtomicValue)a) + ", " + Type.displayTypeName((AtomicValue)b) + ')');
                } else {
                    throw new ClassCastException("Objects are not comparable (" +
                            a.getClass() + ", " + b.getClass() + ')');
                }
            } else {
                return ac.compareTo(bc);
            }
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

        if (a instanceof UntypedAtomicValue) {
            return ((UntypedAtomicValue)a).compareTo(b, collator, conversionContext) == 0;
        } else if (b instanceof UntypedAtomicValue) {
            return ((UntypedAtomicValue)b).compareTo(a, collator, conversionContext) == 0;
        } else if (a instanceof StringValue) {
            return collator.compareStrings(((StringValue)a).getStringValue(), ((StringValue)b).getStringValue()) == 0;
        } else if (a instanceof CalendarValue && b instanceof CalendarValue) {
            return ((CalendarValue)a).compareTo((CalendarValue)b, conversionContext.getConfiguration()) == 0;
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

        if (a instanceof StringValue) {
            Platform platform = Configuration.getPlatform();
            if (platform.canReturnCollationKeys(collator)) {
                return new ComparisonKey(Type.STRING,
                        collator.getCollationKey(((StringValue)a).getStringValue(), platform));
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