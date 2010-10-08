package net.sf.saxon.s9api;

import net.sf.saxon.om.Item;
import net.sf.saxon.om.NodeInfo;
import net.sf.saxon.pattern.AnyNodeTest;
import net.sf.saxon.pattern.NodeTest;
import net.sf.saxon.type.AnyItemType;
import net.sf.saxon.type.AtomicType;
import net.sf.saxon.type.BuiltInAtomicType;
import net.sf.saxon.type.SchemaType;
import net.sf.saxon.value.AtomicValue;

/**
 * An item type, as defined in the XPath/XQuery specifications.
 *
 * <p>This class contains a number of static properties
 * to obtain instances representing simple item types, such as
 * <code>item()</code>, <code>node()</code>, and <code>xs:anyAtomicType</code>.</p>
 *
 * <p>More complicated item types, especially those that are dependent on information in a schema,
 * are available using factory methods on the {@link ItemTypeFactory} object.</p>
 */

public abstract class ItemType {

    /**
     * ItemType representing the type item(), that is, any item at all
     */

    public static ItemType ANY_ITEM = new ItemType() {

        public boolean matches(XdmItem item) {
            return true;
        }

        public boolean subsumes(ItemType other) {
            return true;
        }

        public net.sf.saxon.type.ItemType getUnderlyingItemType() {
            return AnyItemType.getInstance();
        }
    };

    /**
     * ItemType representing the type node(), that is, any node
     */

    public static ItemType ANY_NODE = new ItemType() {

        public boolean matches(XdmItem item) {
            return item.getUnderlyingValue() instanceof NodeInfo;
        }

        public boolean subsumes(ItemType other) {
            return other.getUnderlyingItemType() instanceof NodeTest;
        }

        public net.sf.saxon.type.ItemType getUnderlyingItemType() {
            return AnyNodeTest.getInstance();
        }
    };

    /**
     * ItemType representing the type xs:anyAtomicType, that is, any atomic value
     */

    public static ItemType ANY_ATOMIC_VALUE = new ItemType() {

        public boolean matches(XdmItem item) {
            return item.getUnderlyingValue() instanceof AtomicValue;
        }

        public boolean subsumes(ItemType other) {
            return other.getUnderlyingItemType() instanceof AtomicType;
        }

        public net.sf.saxon.type.ItemType getUnderlyingItemType() {
            return BuiltInAtomicType.ANY_ATOMIC;
        }
    };

    private static class BuiltInAtomicItemType extends ItemType {

        private BuiltInAtomicType underlyingType;

        public BuiltInAtomicItemType(BuiltInAtomicType underlyingType) {
            this.underlyingType = underlyingType;
        }

        public boolean matches(XdmItem item) {
            Item value = (Item)item.getUnderlyingValue();
            if (!(value instanceof AtomicValue)) {
                return false;
            }
            AtomicType type = ((AtomicValue)value).getTypeLabel();
            return subsumesUnderlyingType(type);
        }

        public boolean subsumes(ItemType other) {
            net.sf.saxon.type.ItemType otherType = other.getUnderlyingItemType();
            if (!(otherType.isAtomicType())) {
                return false;
            }
            AtomicType type = (AtomicType)otherType;
            return subsumesUnderlyingType(type);
        }

        private boolean subsumesUnderlyingType(AtomicType type) {
            BuiltInAtomicType builtIn =
                    (type instanceof BuiltInAtomicType ? ((BuiltInAtomicType)type) : (BuiltInAtomicType)type.getBuiltInBaseType());
            while (true) {
                if (builtIn.isSameType(underlyingType)) {
                    return true;
                }
                SchemaType base = builtIn.getBaseType();
                if (!(base instanceof BuiltInAtomicType)) {
                    return false;
                }
                builtIn = (BuiltInAtomicType)base;
            }
        }

        public net.sf.saxon.type.ItemType getUnderlyingItemType() {
            return underlyingType;
        }
    }

    /**
     * A Saxon-specific item type representing the base type of double, float, and decimal
     */

    public static ItemType NUMERIC = new BuiltInAtomicItemType(BuiltInAtomicType.NUMERIC);

    /**
     * ItemType representing the primitive type xs:string
     */

    public static ItemType STRING = new BuiltInAtomicItemType(BuiltInAtomicType.STRING);

    /**
     * ItemType representing the primitive type xs:boolean
     */

    public static ItemType BOOLEAN = new BuiltInAtomicItemType(BuiltInAtomicType.BOOLEAN);

    /**
     * ItemType representing the primitive type xs:duration
     */

    public static ItemType DURATION = new BuiltInAtomicItemType(BuiltInAtomicType.DURATION);

    /**
     * ItemType representing the primitive type xs:dateTime
     */

    public static ItemType DATE_TIME = new BuiltInAtomicItemType(BuiltInAtomicType.DATE_TIME);

    /**
     * ItemType representing the primitive type xs:date
     */

    public static ItemType DATE = new BuiltInAtomicItemType(BuiltInAtomicType.DATE);

    /**
     * ItemType representing the primitive type xs:time
     */

    public static ItemType TIME = new BuiltInAtomicItemType(BuiltInAtomicType.TIME);

    /**
     * ItemType representing the primitive type xs:gYearMonth
     */

    public static ItemType G_YEAR_MONTH = new BuiltInAtomicItemType(BuiltInAtomicType.G_YEAR_MONTH);

    /**
     * ItemType representing the primitive type xs:gMonth
     */

    public static ItemType G_MONTH = new BuiltInAtomicItemType(BuiltInAtomicType.G_MONTH);

    /**
     * ItemType representing the primitive type xs:gMonthDay
     */

    public static ItemType G_MONTH_DAY = new BuiltInAtomicItemType(BuiltInAtomicType.G_MONTH_DAY);

    /**
     * ItemType representing the primitive type xs:gYear
     */

    public static ItemType G_YEAR = new BuiltInAtomicItemType(BuiltInAtomicType.G_YEAR);

    /**
     * ItemType representing the primitive type xs:gDay
     */

    public static ItemType G_DAY = new BuiltInAtomicItemType(BuiltInAtomicType.G_DAY);

    /**
     * ItemType representing the primitive type xs:hexBinary
     */

    public static ItemType HEX_BINARY = new BuiltInAtomicItemType(BuiltInAtomicType.HEX_BINARY);

    /**
     * ItemType representing the primitive type xs:base64Binary
     */

    public static ItemType BASE64_BINARY = new BuiltInAtomicItemType(BuiltInAtomicType.BASE64_BINARY);

    /**
     * ItemType representing the primitive type xs:anyURI
     */

    public static ItemType ANY_URI = new BuiltInAtomicItemType(BuiltInAtomicType.ANY_URI);

    /**
     * ItemType representing the primitive type xs:QName
     */

    public static ItemType QNAME = new BuiltInAtomicItemType(BuiltInAtomicType.QNAME);

    /**
     * ItemType representing the primitive type xs:NOTATION
     */

    public static ItemType NOTATION = new BuiltInAtomicItemType(BuiltInAtomicType.NOTATION);

    /**
     * ItemType representing the XPath-defined type xs:untypedAtomic
     */

    public static ItemType UNTYPED_ATOMIC = new BuiltInAtomicItemType(BuiltInAtomicType.UNTYPED_ATOMIC);

    /**
     * ItemType representing the primitive type xs:decimal
     */

    public static ItemType DECIMAL = new BuiltInAtomicItemType(BuiltInAtomicType.DECIMAL);

    /**
     * ItemType representing the primitive type xs:float
     */

    public static ItemType FLOAT = new BuiltInAtomicItemType(BuiltInAtomicType.FLOAT);

    /**
     * ItemType representing the primitive type xs:double
     */

    public static ItemType DOUBLE = new BuiltInAtomicItemType(BuiltInAtomicType.DOUBLE);

    /**
     * ItemType representing the built-in (but non-primitive) type xs:integer
     */

    public static ItemType INTEGER = new BuiltInAtomicItemType(BuiltInAtomicType.INTEGER);

    /**
     * ItemType representing the built-in (but non-primitive) type xs:nonPositiveInteger
     */

    public static ItemType NON_POSITIVE_INTEGER = new BuiltInAtomicItemType(BuiltInAtomicType.NON_POSITIVE_INTEGER);

    /**
     * ItemType representing the built-in (but non-primitive) type xs:negativeInteger
     */

    public static ItemType NEGATIVE_INTEGER = new BuiltInAtomicItemType(BuiltInAtomicType.NEGATIVE_INTEGER);

    /**
     * ItemType representing the built-in (but non-primitive) type xs:long
     */

    public static ItemType LONG = new BuiltInAtomicItemType(BuiltInAtomicType.LONG);

    /**
     * ItemType representing the built-in (but non-primitive) type xs:int
     */

    public static ItemType INT = new BuiltInAtomicItemType(BuiltInAtomicType.INT);

    /**
     * ItemType representing the built-in (but non-primitive) type xs:short
     */

    public static ItemType SHORT = new BuiltInAtomicItemType(BuiltInAtomicType.SHORT);

    /**
     * ItemType representing the built-in (but non-primitive) type xs:byte
     */

    public static ItemType BYTE = new BuiltInAtomicItemType(BuiltInAtomicType.BYTE);

    /**
     * ItemType representing the built-in (but non-primitive) type xs:nonNegativeInteger
     */

    public static ItemType NON_NEGATIVE_INTEGER = new BuiltInAtomicItemType(BuiltInAtomicType.NON_NEGATIVE_INTEGER);

    /**
     * ItemType representing the built-in (but non-primitive) type xs:positiveInteger
     */

    public static ItemType POSITIVE_INTEGER = new BuiltInAtomicItemType(BuiltInAtomicType.POSITIVE_INTEGER);

    /**
     * ItemType representing the built-in (but non-primitive) type xs:unsignedLong
     */

    public static ItemType UNSIGNED_LONG = new BuiltInAtomicItemType(BuiltInAtomicType.UNSIGNED_LONG);

    /**
     * ItemType representing the built-in (but non-primitive) type xs:unsignedInt
     */

    public static ItemType UNSIGNED_INT = new BuiltInAtomicItemType(BuiltInAtomicType.UNSIGNED_INT);

    /**
     * ItemType representing the built-in (but non-primitive) type xs:unsignedShort
     */

    public static ItemType UNSIGNED_SHORT = new BuiltInAtomicItemType(BuiltInAtomicType.UNSIGNED_SHORT);

    /**
     * ItemType representing the built-in (but non-primitive) type xs:unsignedByte
     */

    public static ItemType UNSIGNED_BYTE = new BuiltInAtomicItemType(BuiltInAtomicType.UNSIGNED_BYTE);

    /**
     * ItemType representing the built-in (but non-primitive) type xs:yearMonthDuration
     */

    public static ItemType YEAR_MONTH_DURATION = new BuiltInAtomicItemType(BuiltInAtomicType.YEAR_MONTH_DURATION);

    /**
     * ItemType representing the built-in (but non-primitive) type xs:dayTimeDuration
     */

    public static ItemType DAY_TIME_DURATION = new BuiltInAtomicItemType(BuiltInAtomicType.DAY_TIME_DURATION);

    /**
     * ItemType representing the built-in (but non-primitive) type xs:normalizedString
     */

    public static ItemType NORMALIZED_STRING = new BuiltInAtomicItemType(BuiltInAtomicType.NORMALIZED_STRING);

    /**
     * ItemType representing the built-in (but non-primitive) type xs:token
     */

    public static ItemType TOKEN = new BuiltInAtomicItemType(BuiltInAtomicType.TOKEN);

    /**
     * ItemType representing the built-in (but non-primitive) type xs:language
     */

    public static ItemType LANGUAGE = new BuiltInAtomicItemType(BuiltInAtomicType.LANGUAGE);

    /**
     * ItemType representing the built-in (but non-primitive) type xs:Name
     */

    public static ItemType NAME = new BuiltInAtomicItemType(BuiltInAtomicType.NAME);

    /**
     * ItemType representing the built-in (but non-primitive) type xs:NMTOKEN
     */

    public static ItemType NMTOKEN = new BuiltInAtomicItemType(BuiltInAtomicType.NMTOKEN);

    /**
     * ItemType representing the built-in (but non-primitive) type xs:NCName
     */

    public static ItemType NCNAME = new BuiltInAtomicItemType(BuiltInAtomicType.NCNAME);

    /**
     * ItemType representing the built-in (but non-primitive) type xs:ID
     */

    public static ItemType ID = new BuiltInAtomicItemType(BuiltInAtomicType.ID);

    /**
     * ItemType representing the built-in (but non-primitive) type xs:IDREF
     */

    public static ItemType IDREF = new BuiltInAtomicItemType(BuiltInAtomicType.IDREF);

    /**
     * ItemType representing the built-in (but non-primitive) type xs:ENTITY
     */

    public static ItemType ENTITY = new BuiltInAtomicItemType(BuiltInAtomicType.ENTITY);

    /**
     * ItemType representing the built-in (but non-primitive) type xs:dateTimeStamp
     * (introduced in XSD 1.1)
     */

    public static ItemType DATE_TIME_STAMP = new BuiltInAtomicItemType(BuiltInAtomicType.DATE_TIME_STAMP);


    /**
     * Determine whether this item type matches a given item.
     *
     * @param item the item to be tested against this item type
     * @return true if the item matches this item type, false if it does not match.
     */

    public abstract boolean matches(XdmItem item);

    /**
     * Determine whether this ItemType subsumes another ItemType. Specifically,
     * <code>A.subsumes(B) is true if every value that matches the ItemType B also matches
     * the ItemType A.
     * @param other the other ItemType
     * @return true if this ItemType subsumes the other ItemType. This includes the case where A and B
     * represent the same ItemType.
     * @since 9.1
     */

    public abstract boolean subsumes(ItemType other);

    /**
     * Method to get the underlying Saxon implementation object
     *
     * <p>This gives access to Saxon methods that may change from one release to another.</p>
     * @return the underlying Saxon implementation object
     */

    public abstract net.sf.saxon.type.ItemType getUnderlyingItemType();

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
// The Initial Developer of the Original Code is Michael H. Kay.
//
// Contributor(s):
//

