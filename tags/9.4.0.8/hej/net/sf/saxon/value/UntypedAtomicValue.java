package net.sf.saxon.value;

import net.sf.saxon.Configuration;
import net.sf.saxon.expr.XPathContext;
import net.sf.saxon.expr.sort.CodepointCollator;
import net.sf.saxon.lib.StringCollator;
import net.sf.saxon.type.*;

/**
* An Untyped Atomic value. This inherits from StringValue for implementation convenience, even
* though an untypedAtomic value is not a String in the data model type hierarchy.
*/

public class UntypedAtomicValue extends StringValue {

    /*@NotNull*/ public static final UntypedAtomicValue ZERO_LENGTH_UNTYPED =
            new UntypedAtomicValue("");

    // If the value is used once as a number, it's likely that it will be used
    // repeatedly as a number, so we cache the result of conversion

    /*@Nullable*/ DoubleValue doubleValue = null;

    /**
    * Constructor
    * @param value the String value. Null is taken as equivalent to "".
    */

    public UntypedAtomicValue(/*@Nullable*/ CharSequence value) {
        this.value = (value==null ? "" : value);
        typeLabel = BuiltInAtomicType.UNTYPED_ATOMIC;
    }

    /**
     * Create a copy of this atomic value, with a different type label
     *
     * @param typeLabel the type label of the new copy. The caller is responsible for checking that
     *                  the value actually conforms to this type.
     */

    /*@NotNull*/ public AtomicValue copyAsSubType(AtomicType typeLabel) {
        UntypedAtomicValue v = new UntypedAtomicValue(value);
        v.noSurrogates = noSurrogates;
        v.doubleValue = doubleValue;
        v.typeLabel = typeLabel;
        return v;
    }

    /**
     * Determine the primitive type of the value. This delivers the same answer as
     * getItemType().getPrimitiveItemType(). The primitive types are
     * the 19 primitive types of XML Schema, plus xs:integer, xs:dayTimeDuration and xs:yearMonthDuration,
     * and xs:untypedAtomic. For external objects, the result is AnyAtomicType.
     */

    /*@NotNull*/ public BuiltInAtomicType getPrimitiveType() {
        return BuiltInAtomicType.UNTYPED_ATOMIC;
    }

    /**
    * Compare an untypedAtomic value with another value, using a given collator to perform
    * any string comparisons. This works by converting the untypedAtomic value to the type
     * of the other operand, which is the correct behavior for operators like "=" and "!=",
     * but not for "eq" and "ne": in the latter case, the untypedAtomic value is converted
     * to a string and this method is therefore not used.
     * @param other the value to compare this value with
     * @param collator the collation to be used for comparing strings
     * @param context the XPath dynamic context
     *
     * @return -1 if the this value is less than the other, 0 if they are equal, +1 if this
     * value is greater.
     * @throws ClassCastException if the value cannot be cast to the type of the other operand
    */

    public int compareTo(/*@NotNull*/ AtomicValue other, /*@NotNull*/ StringCollator collator, /*@NotNull*/ XPathContext context) {
        if (other instanceof NumericValue) {
            if (doubleValue == null) {
                try {
                    doubleValue = new DoubleValue(context.getConfiguration().getConversionRules()
                            .getStringToDoubleConverter().stringToNumber(value));
                } catch (NumberFormatException e) {
                    throw new ClassCastException("Cannot convert untyped atomic value '" + getStringValue()
                        + "' to a double ");
                }
            }
            return doubleValue.compareTo(other);
        } else if (other instanceof StringValue) {
            if (collator instanceof CodepointCollator) {
                // This optimization avoids creating String objects for the purpose of the comparison
                return ((CodepointCollator)collator).compareCS(getStringValueCS(),
                                                              other.getStringValueCS());
            } else {
                return collator.compareStrings(getStringValue(), other.getStringValue());
            }
        } else {
            final Configuration config = context.getConfiguration();
            final TypeHierarchy th = config.getTypeHierarchy();
            Converter converter = config.getConversionRules().getConverter(BuiltInAtomicType.UNTYPED_ATOMIC, (AtomicType) other.getItemType(th));
            ConversionResult result = converter.convert(this);
            if (result instanceof ValidationFailure) {
                throw new ClassCastException("Cannot convert untyped atomic value '" + getStringValue()
                        + "' to type " + other.getItemType(th));
            }
            return ((Comparable)((AtomicValue)result)).compareTo(other);

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