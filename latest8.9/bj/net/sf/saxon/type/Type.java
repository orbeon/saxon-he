package net.sf.saxon.type;
import net.sf.saxon.om.Item;
import net.sf.saxon.om.NamePool;
import net.sf.saxon.om.NodeInfo;
import net.sf.saxon.om.StandardNames;
import net.sf.saxon.pattern.AnyNodeTest;
import net.sf.saxon.pattern.EmptySequenceTest;
import net.sf.saxon.pattern.NodeKindTest;
import net.sf.saxon.pattern.NodeTest;
import net.sf.saxon.value.AtomicValue;
import net.sf.saxon.value.ObjectValue;

import java.io.Serializable;


/**
 * This class contains static information about types and methods for constructing type codes.
 * The class is never instantiated.
 *
 */

public abstract class Type implements Serializable {

    // Note that the integer codes representing node kinds are the same as
    // the codes allocated in the DOM interface, while the codes for built-in
    // atomic types are fingerprints allocated in StandardNames. These two sets of
    // codes must not overlap!

    /**
     * Type representing an element node - element()
     */

    public static final short ELEMENT = 1;
    /**
     * Item type representing an attribute node - attribute()
     */
    public static final short ATTRIBUTE = 2;
    /**
     * Item type representing a text node - text()
     */
    public static final short TEXT = 3;
    /**
     * Item type representing a text node stored in the tiny tree as compressed whitespace
     */
    public static final short WHITESPACE_TEXT = 4;
    /**
     * Item type representing a processing-instruction node
     */
    public static final short PROCESSING_INSTRUCTION = 7;
    /**
     * Item type representing a comment node
     */
    public static final short COMMENT = 8;
    /**
     * Item type representing a document node
     */
    public static final short DOCUMENT = 9;
    /**
     * Item type representing a namespace node
     */
    public static final short NAMESPACE = 13;
    /**
     * Dummy node kind used in the tiny tree to mark the end of the tree
     */
    public static final short STOPPER = 11;
    /**
     * Dummy node kind used in the tiny tree to contain a parent pointer
     */
    public static final short PARENT_POINTER = 12;

    /**
     * An item type that matches any node
     */

    public static final short NODE = 0;

    public static final ItemType NODE_TYPE = AnyNodeTest.getInstance();

    /**
     * An item type that matches any item
     */

    public static final short ITEM = 88;

    public static final ItemType ITEM_TYPE = AnyItemType.getInstance();

    public static final short MAX_NODE_TYPE = 13;
    /**
     * Item type that matches no items (corresponds to SequenceType empty())
     */
    public static final short EMPTY = 15;    // a test for this type will never be satisfied

    private Type() {
    }

    /**
     * Test whether a given type is (some subtype of) node()
     *
     * @param type The type to be tested
     * @return true if the item type is node() or a subtype of node()
     */

    public static boolean isNodeType(ItemType type) {
        return type instanceof NodeTest;
    }

    /**
     * Constant denoting any atomic type (the union of all primitive types and types
     * derived from primitive types by restriction or by union)
     */

    //public static final int ATOMIC          = 90;
    public static final int ANY_ATOMIC      = StandardNames.XS_ANY_ATOMIC_TYPE;

    /**
     * Constant denoting any numeric type (the union of float, double, and decimal)
     */

    //public static final int NUMBER          = 91;
    public static final int NUMBER          = StandardNames.XS_NUMERIC;

    /**
     * Constants representing primitive data types defined in Schema Part 2
     */

    public static final int STRING = StandardNames.XS_STRING;
    /**
     * Item type representing the type xs:boolean
     */
    public static final int BOOLEAN = StandardNames.XS_BOOLEAN;
    /**
     * Item type representing the type xs:decimal
     */
    public static final int DECIMAL = StandardNames.XS_DECIMAL;
    /**
     * Item type representing the type xs:float
     */
    public static final int FLOAT = StandardNames.XS_FLOAT;
    /**
     * Item type representing the type xs:double
     */
    public static final int DOUBLE = StandardNames.XS_DOUBLE;
    /**
     * Item type representing the type xs:duration
     */
    public static final int DURATION = StandardNames.XS_DURATION;
    /**
     * Item type representing the type xs:dateTime
     */
    public static final int DATE_TIME = StandardNames.XS_DATE_TIME;
    /**
     * Item type representing the type xs:time
     */
    public static final int TIME = StandardNames.XS_TIME;
    /**
     * Item type representing the type xs:date
     */
    public static final int DATE = StandardNames.XS_DATE;
    /**
     * Item type representing the type xs:gYearMonth
     */
    public static final int G_YEAR_MONTH = StandardNames.XS_G_YEAR_MONTH;
    /**
     * Item type representing the type xs:gYear
     */
    public static final int G_YEAR = StandardNames.XS_G_YEAR;
    /**
     * Item type representing the type xs:monthDay
     */
    public static final int G_MONTH_DAY = StandardNames.XS_G_MONTH_DAY;
    /**
     * Item type representing the type xs:gDay
     */
    public static final int G_DAY = StandardNames.XS_G_DAY;
    /**
     * Item type representing the type xs:gMonth
     */
    public static final int G_MONTH = StandardNames.XS_G_MONTH;
    /**
     * Item type representing the type xs:hexBinary
     */
    public static final int HEX_BINARY = StandardNames.XS_HEX_BINARY;
    /**
     * Item type representing the type xs:base64Binary
     */
    public static final int BASE64_BINARY   = StandardNames.XS_BASE64_BINARY;
    /**
     * Item type representing the type xs:anyURI
     */
    public static final int ANY_URI = StandardNames.XS_ANY_URI;
    /**
     * Item type representing the type xs:QName
     */
    public static final int QNAME = StandardNames.XS_QNAME;
    /**
     * Item type representing the type xs:NOTATION
     *
     */
    public static final int NOTATION = StandardNames.XS_NOTATION;

    /**
     * Item type representing the type xs:untypedAtomic
     * (the type of the content of a schema-less node)
     */

    public static final int UNTYPED_ATOMIC = StandardNames.XS_UNTYPED_ATOMIC;

    public static final int ANY_SIMPLE_TYPE = StandardNames.XS_ANY_SIMPLE_TYPE;

    /**
     * Constant representing the type of an external object (for use by extension functions)
     */

    public static final int OBJECT = StandardNames.SAXON_JAVA_LANG_OBJECT;

    /**
     * Item type representing the type xs:integer
     */

    public static final int INTEGER = StandardNames.XS_INTEGER;
    /**
     * Item type representing the type xs:nonPositiveInteger
     */
    public static final int NON_POSITIVE_INTEGER = StandardNames.XS_NON_POSITIVE_INTEGER;
    /**
     * Item type representing the type xs:negativeInteger
     */
    public static final int NEGATIVE_INTEGER = StandardNames.XS_NEGATIVE_INTEGER;
    /**
     * Item type representing the type xs:long
     */
    public static final int LONG = StandardNames.XS_LONG;
    /**
     * Item type representing the type xs:int
     */
    public static final int INT = StandardNames.XS_INT;
    /**
     * Item type representing the type xs:short
     */
    public static final int SHORT = StandardNames.XS_SHORT;
    /**
     * Item type representing the type xs:byte
     */
    public static final int BYTE = StandardNames.XS_BYTE;
    /**
     * Item type representing the type xs:nonNegativeInteger
     */
    public static final int NON_NEGATIVE_INTEGER = StandardNames.XS_NON_NEGATIVE_INTEGER;
    /**
     * Item type representing the type xs:positiveInteger
     */
    public static final int POSITIVE_INTEGER = StandardNames.XS_POSITIVE_INTEGER;
    /**
     * Item type representing the type xs:unsignedLong
     */
    public static final int UNSIGNED_LONG = StandardNames.XS_UNSIGNED_LONG;
    /**
     * Item type representing the type xs:unsignedInt
     */
    public static final int UNSIGNED_INT = StandardNames.XS_UNSIGNED_INT;
    /**
     * Item type representing the type xs:unsignedShort
     */
    public static final int UNSIGNED_SHORT = StandardNames.XS_UNSIGNED_SHORT;
    /**
     * Item type representing the type xs:unsignedByte
     */
    public static final int UNSIGNED_BYTE = StandardNames.XS_UNSIGNED_BYTE;

    /**
     * Item type representing the type xs:normalizedString
     */
    public static final int NORMALIZED_STRING = StandardNames.XS_NORMALIZED_STRING;
    /**
     * Item type representing the type xs:token
     */
    public static final int TOKEN = StandardNames.XS_TOKEN;
    /**
     * Item type representing the type xs:language
     */
    public static final int LANGUAGE = StandardNames.XS_LANGUAGE;
    /**
     * Item type representing the type xs:NMTOKEN
     */
    public static final int NMTOKEN = StandardNames.XS_NMTOKEN;
    /**
     * Content type representing the complex type xs:NMTOKENS
     */
    public static final int NMTOKENS = StandardNames.XS_NMTOKENS;      // NB: list type
    /**
     * Item type representing the type xs:NAME
     */
    public static final int NAME = StandardNames.XS_NAME;
    /**
     * Item type representing the type xs:NCNAME
     */
    public static final int NCNAME = StandardNames.XS_NCNAME;
    /**
     * Item type representing the type xs:ID
     */
    public static final int ID = StandardNames.XS_ID;
    /**
     * Item type representing the type xs:IDREF
     */
    public static final int IDREF = StandardNames.XS_IDREF;
    /**
     * Content type representing the complex type xs:IDREFS
     */
    public static final int IDREFS = StandardNames.XS_IDREFS;      // NB: list type
    /**
     *
     * Item type representing the type xs:ENTITY
     */
    public static final int ENTITY = StandardNames.XS_ENTITY;

    /**
     *
     * Item type representing the type xs:yearMonthDuration
     */
    public static final int YEAR_MONTH_DURATION = StandardNames.XS_YEAR_MONTH_DURATION;
    /**
     *
     * Item type representing the type xs:dayTimeDuration
     */
    public static final int DAY_TIME_DURATION = StandardNames.XS_DAY_TIME_DURATION;

    /**
     * Get the ItemType of an Item
     */

    public static ItemType getItemType(Item item, TypeHierarchy th) {
        if (item instanceof AtomicValue) {
            return ((AtomicValue)item).getItemType(th);
        } else {
            return NodeKindTest.makeNodeKindTest(((NodeInfo)item).getNodeKind());
            // We could return a more precise type than this, for example one that includes
            // a ContentTypeTest for the type annotation of the nodes. However, given the way in which
            // this method is used, this wouldn't be very useful
        }
    }


    /**
     * Output (for diagnostics) a representation of the type of an item. This
     * does not have to be the most specific type
     */

    public static final String displayTypeName(Item item) {
        if (item instanceof NodeInfo) {
            NodeInfo node = (NodeInfo)item;
            switch (node.getNodeKind()) {
                case DOCUMENT:
                    return "document-node()";
                case ELEMENT:
                    NamePool pool = node.getNamePool();
                    int annotation = node.getTypeAnnotation();
                    return "element(" +
                            ((NodeInfo)item).getDisplayName() + ", " +
                            (annotation == -1 ?
                                "xs:untyped)" :
                                pool.getDisplayName(annotation) + ')');
                case ATTRIBUTE:
                    NamePool pool2 = node.getNamePool();
                    int annotation2 = node.getTypeAnnotation() & NamePool.FP_MASK;
                    return "attribute(" +
                            ((NodeInfo)item).getDisplayName()+ ", " +
                            (annotation2 == -1 ?
                                "xs:untypedAtomic)" :
                                pool2.getDisplayName(annotation2) + ')');
                case TEXT:      return "text()";
                case COMMENT:   return "comment()";
                case PROCESSING_INSTRUCTION:
                                return "processing-instruction()";
                case NAMESPACE: return "namespace()";
                default:        return "";
            }
        } else if (item instanceof ObjectValue) {
            return ((ObjectValue)item).displayTypeName();
        } else {
            return ((AtomicValue)item).getItemType(null).toString();
        }
    }

    /**
     * Get the SimpleType object for a built-in simple type code
     * @return the SimpleType, or null if not found
     */

    public static ItemType getBuiltInItemType(String namespace, String localName) {
        SchemaType t = BuiltInType.getSchemaType(
                StandardNames.getFingerprint(namespace, localName));
        if (t instanceof ItemType) {
            return (ItemType)t;
        } else {
            return null;
        }
    }

    /**
     * Get the relationship of two schema types to each other
     */

    public static int schemaTypeRelationship(SchemaType s1, SchemaType s2) {
        if (s1.isSameType(s2)) {
            return TypeHierarchy.SAME_TYPE;
        }
        if (s1 instanceof AnyType) {
            return TypeHierarchy.SUBSUMES;
        }
        if (s2 instanceof AnyType) {
            return TypeHierarchy.SUBSUMED_BY;
        }
        SchemaType t1 = s1;
        while (true) {
            t1 = t1.getBaseType();
            if (t1 == null) {
                break;
            }
            if (t1.isSameType(s2)) {
                return TypeHierarchy.SUBSUMED_BY;
            }
        }
        SchemaType t2 = s2;
        while (true) {
            t2 = t2.getBaseType();
            if (t2 == null) {
                break;
            }
            if (t2.isSameType(s1)) {
                return TypeHierarchy.SUBSUMES;
            }
        }
        return TypeHierarchy.DISJOINT;
    }

    /**
     * Get a type that is a common supertype of two given types
     *
     * @param t1 the first item type
     * @param t2 the second item type
     * @param th
     * @return the item type that is a supertype of both
     *     the supplied item types
     */

    public static final ItemType getCommonSuperType(ItemType t1, ItemType t2, TypeHierarchy th) {
        if (t1 instanceof EmptySequenceTest) {
            return t2;
        }
        if (t2 instanceof EmptySequenceTest) {
            return t1;
        }
        int r = th.relationship(t1, t2);
        if (r == TypeHierarchy.SAME_TYPE) {
            return t1;
        } else if (r == TypeHierarchy.SUBSUMED_BY) {
            return t2;
        } else if (r == TypeHierarchy.SUBSUMES) {
            return t1;
        } else {
            return getCommonSuperType(t2.getSuperType(th), t1, th);
            // eventually we will hit a type that is a supertype of t2. We reverse
            // the arguments so we go up each branch of the tree alternately.
            // If we hit the root of the tree, one of the earlier conditions will be satisfied,
            // so the recursion will stop.
        }
    }

    /**
     * Determine whether this type is a primitive type. The primitive types are
     * the 19 primitive types of XML Schema, plus xs:integer, xs:dayTimeDuration and xs:yearMonthDuration;
     * xs:untypedAtomic; the 7 node kinds; and all supertypes of these (item(), node(), xs:anyAtomicType,
     * xs:numeric, ...)
     * @param code the item type code to be tested
     * @return true if the type is considered primitive under the above rules
     */
    public static boolean isPrimitiveType(int code) {
        return code >= 0 && (code <= INTEGER || code == StandardNames.XS_NUMERIC ||
                code == UNTYPED_ATOMIC || code == ANY_ATOMIC ||
                code == DAY_TIME_DURATION || code == YEAR_MONTH_DURATION ||
                code == StandardNames.XS_ANY_SIMPLE_TYPE);
    }

    /**
     * Determine whether two primitive atomic types are comparable
     * @param t1 the first type to compared.
     * This must be a primitive atomic type as defined by {@link ItemType#getPrimitiveType}
     * @param t2 the second type to compared.
     * This must be a primitive atomic type as defined by {@link ItemType#getPrimitiveType}
     * @param ordered true if testing for an ordering comparison (lt, gt, le, ge). False
     * if testing for an equality comparison (eq, ne)
     * @return true if the types are comparable, as defined by the rules of the "eq" operator
     */

    public static boolean isComparable(BuiltInAtomicType t1, BuiltInAtomicType t2, boolean ordered) {
        if (t1.equals(BuiltInAtomicType.ANY_ATOMIC) || t2.equals(BuiltInAtomicType.ANY_ATOMIC)) {
            return true; // meaning we don't actually know at this stage
        }
        if (t1.equals(BuiltInAtomicType.UNTYPED_ATOMIC)) {
            t1 = BuiltInAtomicType.STRING;
        }
        if (t2.equals(BuiltInAtomicType.UNTYPED_ATOMIC)) {
            t2 = BuiltInAtomicType.STRING;
        }
        if (t1.equals(BuiltInAtomicType.ANY_URI)) {
            t1 = BuiltInAtomicType.STRING;
        }
        if (t2.equals(BuiltInAtomicType.ANY_URI)) {
            t2 = BuiltInAtomicType.STRING;
        }
        if (t1.isPrimitiveNumeric()) {
            t1 = BuiltInAtomicType.NUMERIC;
        }
        if (t2.isPrimitiveNumeric()) {
            t2 = BuiltInAtomicType.NUMERIC;
        }
        if (!ordered) {
            if (t1.equals(BuiltInAtomicType.DAY_TIME_DURATION)) {
                t1 = BuiltInAtomicType.DURATION;
            }
            if (t2.equals(BuiltInAtomicType.DAY_TIME_DURATION)) {
                t2 = BuiltInAtomicType.DURATION;
            }
            if (t1.equals(BuiltInAtomicType.YEAR_MONTH_DURATION)) {
                t1 = BuiltInAtomicType.DURATION;
            }
            if (t2.equals(BuiltInAtomicType.YEAR_MONTH_DURATION)) {
                t2 = BuiltInAtomicType.DURATION;
            }
        }
        return t1 == t2;
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
// Contributor(s): none.
//
