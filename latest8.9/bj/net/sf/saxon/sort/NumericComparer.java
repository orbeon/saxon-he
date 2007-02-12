package net.sf.saxon.sort;
import net.sf.saxon.value.AtomicValue;
import net.sf.saxon.value.NumericValue;
import net.sf.saxon.value.Value;
import net.sf.saxon.value.DoubleValue;
import net.sf.saxon.type.Type;

/**
 * A Comparer used for comparing sort keys when data-type="number". The items to be
 * compared are converted to numbers, and the numbers are then compared directly. NaN values
 * compare equal to each other, and equal to an empty sequence, but less than anything else.
 * <p/>
 * This class is used in XSLT only, so there is no need to handle XQuery's "empty least" vs
 * "empty greatest" options.
 *
 * @author Michael H. Kay
 *
 */

public class NumericComparer implements AtomicComparer, java.io.Serializable {

    private static NumericComparer THE_INSTANCE = new NumericComparer();

    public static NumericComparer getInstance() {
        return THE_INSTANCE;
    }

    private NumericComparer() {
    }

    /**
    * Compare two Items by converting them to numbers and comparing the numeric values. If either
    * value cannot be converted to a number, it is treated as NaN, and compares less that the other
    * (two NaN values compare equal).
    * @param a the first Item to be compared.
    * @param b the second Item to be compared.
    * @return <0 if a<b, 0 if a=b, >0 if a>b
    * @throws ClassCastException if the objects are not Items
    */

    public int compareAtomicValues(AtomicValue a, AtomicValue b) {
        double d1, d2;

        if (a instanceof NumericValue) {
            d1 = ((NumericValue)a).getDoubleValue();
        } else if (a == null) {
            d1 = Double.NaN;
        } else {
            try {
                d1 = Value.stringToNumber(a.getStringValueCS());
            } catch (NumberFormatException err) {
                d1 = Double.NaN;
            }
        }

        if (b instanceof NumericValue) {
            d2 = ((NumericValue)b).getDoubleValue();
        } else if (b == null) {
            d2 = Double.NaN;
        } else {
            try {
                d2 = Value.stringToNumber(b.getStringValueCS());
            } catch (NumberFormatException err) {
                d2 = Double.NaN;
            }
        }

        if (Double.isNaN(d1)) {
            if (Double.isNaN(d2)) {
                return 0;
            } else {
                return -1;
            }
        }
        if (Double.isNaN(d2)) {
            return +1;
        }
        if (d1 < d2) return -1;
        if (d1 > d2) return +1;
        return 0;

    }

    /**
     * Compare two AtomicValue objects for equality according to the rules for their data type. UntypedAtomic
     * values are compared by converting to the type of the other operand.
     *
     * @param a the first object to be compared.
     * @param b the second object to be compared.
     * @return true if the values are equal, false if not
     * @throws ClassCastException if the objects are not comparable
     */

    public boolean comparesEqual(AtomicValue a, AtomicValue b) {
        return compareAtomicValues(a, b) == 0;
    }

    /**
     * Get a comparison key for an object. This must satisfy the rule that if two objects are equal
     * according to the XPath eq operator, then their comparison keys are equal according to the Java
     * equals() method, and vice versa. There is no requirement that the
     * comparison keys should reflect the ordering of the underlying objects.
     */

    public ComparisonKey getComparisonKey(AtomicValue a) {
        if (a instanceof NumericValue) {
            return new ComparisonKey(Type.NUMBER, toDoubleValue(((NumericValue)a)));
        } else if (a == null) {
            return new ComparisonKey(Type.NUMBER, "NaN");
        } else {
            try {
                double d = Value.stringToNumber(a.getStringValueCS());
                return new ComparisonKey(Type.NUMBER, new DoubleValue(d));
            } catch (NumberFormatException err) {
                return new ComparisonKey(Type.NUMBER, "NaN");
            }
        }
    }

    private DoubleValue toDoubleValue(NumericValue nv) {
        if (nv instanceof DoubleValue) {
            return ((DoubleValue)nv);
        } else {
            return new DoubleValue(nv.getDoubleValue());
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
// The Initial Developer of this module is Michael H. Kay
//
// Portions created by (your name) are Copyright (C) (your legal entity). All Rights Reserved.
//
// Contributor(s): none.
//