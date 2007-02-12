package net.sf.saxon.sort;
import net.sf.saxon.Configuration;
import net.sf.saxon.Platform;
import net.sf.saxon.expr.XPathContext;
import net.sf.saxon.om.StandardNames;
import net.sf.saxon.type.Type;
import net.sf.saxon.type.TypeHierarchy;
import net.sf.saxon.type.AtomicType;
import net.sf.saxon.value.*;

/**
 * An AtomicComparer used for comparing atomic values of arbitrary item types. It encapsulates
 * a collator that is used when the values to be compared are strings. It also supports
 * a separate method for testing equality of items, which can be used for data types that
 * are not ordered.
 *
 * The AtomicSortComparer is identical to the GenericAtomicComparer except for its handling
 * of NaN: it treats NaN values as lower than any other value, and as equal to each other.
 *
 * @author Michael H. Kay
 *
 */

public class AtomicSortComparer implements AtomicComparer {

    private StringCollator collator;
    private XPathContext conversionContext;
    private int itemType;

    /**
     * Factory method to get an atomic comparer suitable for sorting or for grouping (operations in which
     * NaN is considered equal to NaN)
     * @param collator Collating comparer to be used when comparing strings. This argument may be null
     * if the itemType excludes the possibility of comparing strings. If the method is called at compile
     * time, this should be a NamedCollation so that it can be cloned at run-time.
     * @param itemType the primitive item type of the values to be compared
     * @param context Dynamic context (may be an EarlyEvaluationContext)
     * @return a suitable AtomicComparer
     */

    public static AtomicComparer makeSortComparer(StringCollator collator, int itemType, XPathContext context) {
        switch (itemType) {
            case Type.STRING:
            case Type.UNTYPED_ATOMIC:
            case Type.ANY_URI:
                if (collator instanceof CodepointCollator) {
                    return CodepointCollatingComparer.getInstance();
                } else {
                    return new CollatingAtomicComparer(collator, Configuration.getPlatform());
                }
            case Type.INTEGER:
            case Type.DECIMAL:
                return DecimalSortComparer.getInstance();
            case Type.DOUBLE:
            case Type.FLOAT:
            case Type.NUMBER:
                return DoubleSortComparer.getInstance();
            case Type.DATE_TIME:
            case Type.DATE:
            case Type.TIME:
                return new CalendarValueComparer(context.getConfiguration());
            default:
            // use the general-purpose comparer that handles all types
                return new AtomicSortComparer(collator, itemType, context);
        }

    }

    private AtomicSortComparer(StringCollator collator, int itemType, XPathContext context) {
        this.collator = collator;
        if (collator == null) {
            this.collator = CodepointCollator.getInstance();
        }
        this.conversionContext = context;
        this.itemType = itemType;
    }

    /**
     * Get the underlying StringCollator
     */

    public StringCollator getStringCollator() {
        return collator;
    }

    /**
     * Get the requested item type
     */

    public int getItemType() {
        return itemType;
    }

    /**
    * Compare two AtomicValue objects according to the rules for their data type. UntypedAtomic
    * values are compared as if they were strings; if different semantics are wanted, the conversion
    * must be done by the caller.
    * @param a the first object to be compared. It is intended that this should normally be an instance
    * of AtomicValue, though this restriction is not enforced. If it is a StringValue, the
    * collator is used to compare the values, otherwise the value must implement the java.util.Comparable
    * interface.
    * @param b the second object to be compared. This must be comparable with the first object: for
    * example, if one is a string, they must both be strings.
    * @return <0 if a<b, 0 if a=b, >0 if a>b
    * @throws ClassCastException if the objects are not comparable
    */

    public int compareAtomicValues(AtomicValue a, AtomicValue b) {

        if (a == null) {
            if (b == null) {
                return 0;
            } else {
                return -1;
            }
        } else if (b == null) {
            return +1;
        }

        // System.err.println("Comparing " + a.getClass() + "(" + a + ") with " + b.getClass() + "(" + b + ") using " + collator);

        if (a instanceof UntypedAtomicValue) {
            return ((UntypedAtomicValue)a).compareTo(b, collator, conversionContext);
        } else if (b instanceof UntypedAtomicValue) {
            return -((UntypedAtomicValue)b).compareTo(a, collator, conversionContext);
        } else if (a instanceof NumericValue && ((NumericValue)a).isNaN()) {
            if (b instanceof NumericValue && ((NumericValue)b).isNaN()) {
                return 0;
            } else {
                return -1;
            }
        } else if (b instanceof NumericValue && ((NumericValue)b).isNaN()) {
            return +1;
        } else if (a instanceof CalendarValue && b instanceof CalendarValue) {
            return ((CalendarValue)a).compareTo((CalendarValue)b, conversionContext.getConfiguration());
        } else if (a instanceof StringValue && b instanceof StringValue) {
            if (collator instanceof CodepointCollator) {
                return ((CodepointCollator)collator).compareCS(((StringValue)a).getStringValueCS(), ((StringValue)b).getStringValueCS());
            } else {
                return collator.compareStrings(((StringValue)a).getStringValue(), ((StringValue)b).getStringValue());
            }
        } else if (a instanceof StringValue || b instanceof StringValue) {
            if (a instanceof AtomicValue && b instanceof AtomicValue) {
                throw new ClassCastException("Values are not comparable (" +
                        Type.displayTypeName((AtomicValue)a) + ", " + Type.displayTypeName((AtomicValue)b) + ')');
            } else {
                throw new ClassCastException("Values are not comparable (" +
                        a.getClass() + ", " + b.getClass() + ')');
            }
        } else {
            Comparable ac = a.getXPathComparable();
            Comparable bc = b.getXPathComparable();
            if (ac == null || bc == null) {
                if (a instanceof AtomicValue && b instanceof AtomicValue) {
                    throw new ClassCastException("Values are not comparable (" +
                            Type.displayTypeName((AtomicValue)a) + ", " + Type.displayTypeName((AtomicValue)b) + ')');
                } else {
                    throw new ClassCastException("Values are not comparable (" +
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
     *
     * @param a the first object to be compared. It is intended that this should be an instance
     *          of AtomicValue, though this restriction is not enforced. If it is a StringValue, the
     *          collator is used to compare the values, otherwise the value must implement the equals() method.
     * @param b the second object to be compared. This must be comparable with the first object: for
     *          example, if one is a string, they must both be strings.
     * @return true if the values are equal, false if not
     * @throws ClassCastException if the objects are not comparable
     */

    public boolean comparesEqual(AtomicValue a, AtomicValue b) {
        return compareAtomicValues(a, b) == 0;
    }

    /**
    * Get a comparison key for an object. This must satisfy the rule that if two objects are equal,
    * then their comparison keys are equal, and vice versa. There is no requirement that the
    * comparison keys should reflect the ordering of the underlying objects.
    */

    public ComparisonKey getComparisonKey(AtomicValue a) {
        AtomicValue prim = a;
        final Configuration config = conversionContext.getConfiguration();
        final TypeHierarchy th = config.getTypeHierarchy();
        if (prim instanceof NumericValue) {
            if (((NumericValue)prim).isNaN()) {
                // Deal with NaN specially. For this function, NaN is considered equal to itself
                return new ComparisonKey(StandardNames.XS_NUMERIC, COLLATION_KEY_NaN);
            } else {
                return new ComparisonKey(StandardNames.XS_NUMERIC, prim);
            }
        } else if (prim instanceof StringValue) {
            final Platform platform = Configuration.getPlatform();
            if (platform.canReturnCollationKeys(collator)) {
                return new ComparisonKey(Type.STRING,
                        collator.getCollationKey(((StringValue)prim).getStringValue(), platform));
            } else {
                return new ComparisonKey(Type.STRING, prim);
            }
        } else if (prim instanceof CalendarValue) {
            CalendarValue cv = (CalendarValue)prim;
            if (cv.hasTimezone()) {
                return new ComparisonKey(prim.getTypeLabel().getPrimitiveType(), prim);
            } else {
                cv = (CalendarValue)cv.copy((AtomicType)cv.getTypeLabel());
                cv.setTimezoneInMinutes(config.getImplicitTimezone());
                return new ComparisonKey(cv.getTypeLabel().getPrimitiveType(), cv);
            }
        } else if (prim instanceof DurationValue) {
            // dayTimeDuration and yearMonthDuration are comparable in the special case of the zero duration
            return new ComparisonKey(Type.DURATION, prim);
        } else {
            return new ComparisonKey(prim.getTypeLabel().getPrimitiveType(), prim);
        }
    }

    public static StringValue COLLATION_KEY_NaN = new StringValue("NaN");

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