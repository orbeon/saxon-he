////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
// Copyright (c) 2014 Saxonica Limited.
// This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
// If a copy of the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
// This Source Code Form is "Incompatible With Secondary Licenses", as defined by the Mozilla Public License, v. 2.0.
////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

package net.sf.saxon.type;

import com.saxonica.functions.map.MapType;
import net.sf.saxon.expr.parser.Token;
import net.sf.saxon.om.*;
import net.sf.saxon.pattern.*;
import net.sf.saxon.tree.iter.AxisIterator;
import net.sf.saxon.value.AtomicValue;
import net.sf.saxon.value.Cardinality;
import net.sf.saxon.value.ObjectValue;
import net.sf.saxon.value.SequenceType;


/**
 * This class contains static information about types and methods for constructing type codes.
 * The class is never instantiated.
 * <p/>
 * <p><i>The constant integers used for type names in earlier versions of this class have been replaced
 * by constants in {@link StandardNames}. The constants representing {@link AtomicType} objects are now
 * available through the {@link BuiltInAtomicType} class.</i></p>
 */

public abstract class Type {

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

    /*@NotNull*/ public static final ItemType ITEM_TYPE = AnyItemType.getInstance();

    /**
     * A type number for function()
     */

    public static final short FUNCTION = 99;

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
     * Get the ItemType of an Item
     *
     * @param item the item whose type is required
     * @param th   the type hierarchy cache. If null, the returned type may be less precise
     * @return the item type of the item
     */

    /*@NotNull*/
    public static ItemType getItemType(/*@NotNull*/ Item item, /*@Nullable*/ TypeHierarchy th) {
        if (item == null) {
            return AnyItemType.getInstance();
        } else if (item instanceof AtomicValue) {
            return ((AtomicValue) item).getItemType();
        } else if (item instanceof NodeInfo) {
            NodeInfo node = (NodeInfo) item;
            if (th == null) {
                th = node.getConfiguration().getTypeHierarchy();
            }
            switch (node.getNodeKind()) {
                case Type.DOCUMENT:
                    // Need to know whether the document is well-formed and if so what the element type is
                    AxisIterator iter = node.iterateAxis(AxisInfo.CHILD);
                    ItemType elementType = null;
                    while (true) {
                        NodeInfo n = iter.next();
                        if (n == null) {
                            break;
                        }
                        int kind = n.getNodeKind();
                        if (kind == Type.TEXT) {
                            elementType = null;
                            break;
                        } else if (kind == Type.ELEMENT) {
                            if (elementType != null) {
                                elementType = null;
                                break;
                            }
                            elementType = Type.getItemType(n, th);
                        }
                    }
                    if (elementType == null) {
                        return NodeKindTest.DOCUMENT;
                    } else {
                        return new DocumentNodeTest((NodeTest) elementType);
                    }

                case Type.ELEMENT:
                    SchemaType eltype = node.getSchemaType();
                    if (eltype.equals(Untyped.getInstance()) || eltype.equals(AnyType.getInstance())) {
                        return new NameTest(Type.ELEMENT, node.getFingerprint(), node.getNamePool());
                    } else {
                        return new CombinedNodeTest(
                                new NameTest(Type.ELEMENT, node.getFingerprint(), node.getNamePool()),
                                Token.INTERSECT,
                                new ContentTypeTest(Type.ELEMENT, eltype, node.getConfiguration(), false));
                    }

                case Type.ATTRIBUTE:
                    SchemaType attype = node.getSchemaType();
                    if (attype.equals(BuiltInAtomicType.UNTYPED_ATOMIC)) {
                        return new NameTest(Type.ATTRIBUTE, node.getFingerprint(), node.getNamePool());
                    } else {
                        return new CombinedNodeTest(
                                new NameTest(Type.ATTRIBUTE, node.getFingerprint(), node.getNamePool()),
                                Token.INTERSECT,
                                new ContentTypeTest(Type.ATTRIBUTE, attype, node.getConfiguration(), false));
                    }

                case Type.TEXT:
                    return NodeKindTest.TEXT;

                case Type.COMMENT:
                    return NodeKindTest.COMMENT;

                case Type.PROCESSING_INSTRUCTION:
                    return NodeKindTest.PROCESSING_INSTRUCTION;

                case Type.NAMESPACE:
                    return NodeKindTest.NAMESPACE;

                default:
                    throw new IllegalArgumentException("Unknown node kind " + node.getNodeKind());
            }
        } else if (item instanceof ObjectValue) {
            return ((ObjectValue) item).getItemType(th);
        } else { //if (item instanceof FunctionItem) {
            return ((FunctionItem) item).getFunctionItemType(th);
        }
    }


    /**
     * Output (for diagnostics) a representation of the type of an item. This
     * does not have to be the most specific type
     *
     * @param item the item whose type is to be displayed
     * @return a string representation of the type of the item
     */

    public static String displayTypeName(/*@NotNull*/ Item item) {
        if (item instanceof NodeInfo) {
            NodeInfo node = (NodeInfo) item;
            switch (node.getNodeKind()) {
                case DOCUMENT:
                    return "document-node()";
                case ELEMENT:
                    SchemaType annotation = node.getSchemaType();
                    return "element(" +
                            ((NodeInfo) item).getDisplayName() + ", " +
                            annotation.getDisplayName() + ')';
                case ATTRIBUTE:
                    SchemaType annotation2 = node.getSchemaType();
                    return "attribute(" +
                            ((NodeInfo) item).getDisplayName() + ", " +
                            annotation2.getDisplayName() + ')';
                case TEXT:
                    return "text()";
                case COMMENT:
                    return "comment()";
                case PROCESSING_INSTRUCTION:
                    return "processing-instruction()";
                case NAMESPACE:
                    return "namespace()";
                default:
                    return "";
            }
        } else if (item instanceof ObjectValue) {
            return ((ObjectValue) item).displayTypeName();
        } else if (item instanceof AtomicValue) {
            return ((AtomicValue) item).getItemType().toString();
        } else if (item instanceof FunctionItem) {
            return "function(*)";
        } else {
            return item.getClass().toString();
        }
    }

    /**
     * Get the ItemType object for a built-in atomic type
     *
     * @param namespace the namespace URI of the type
     * @param localName the local name of the type
     * @return the ItemType, or null if not found
     */

    /*@Nullable*/
    public static ItemType getBuiltInItemType(String namespace, String localName) {
        SchemaType t = BuiltInType.getSchemaType(
                StandardNames.getFingerprint(namespace, localName));
        if (t instanceof ItemType) {
            return (ItemType) t;
        } else {
            return null;
        }
    }

    /**
     * Get the SimpleType object for a built-in simple type (atomic type or list type)
     *
     * @param namespace the namespace URI of the type
     * @param localName the local name of the type
     * @return the SimpleType, or null if not found
     */

    /*@Nullable*/
    public static SimpleType getBuiltInSimpleType(String namespace, String localName) {
        SchemaType t = BuiltInType.getSchemaType(
                StandardNames.getFingerprint(namespace, localName));
        if (t instanceof SimpleType && ((SimpleType) t).isBuiltInType()) {
            return (SimpleType) t;
        } else {
            return null;
        }
    }

    /**
     * Get a type that is a common supertype of two given item types
     *
     * @param t1 the first item type
     * @param t2 the second item type
     * @param th the type hierarchy cache
     * @return the item type that is a supertype of both
     *         the supplied item types
     */

    /*@NotNull*/
    public static ItemType getCommonSuperType(/*@NotNull*/ ItemType t1, /*@NotNull*/ ItemType t2, /*@NotNull*/ TypeHierarchy th) {
        if (t1 == t2) {
            return t1;
        }
        if (t1 instanceof ErrorType) {
            return t2;
        }
        if (t2 instanceof ErrorType) {
            return t1;
        }
        if (t1 instanceof JavaExternalObjectType && t2 instanceof JavaExternalObjectType) {
            Class c1 = ((JavaExternalObjectType) t1).getJavaClass();
            Class c2 = ((JavaExternalObjectType) t2).getJavaClass();
            return new JavaExternalObjectType(leastCommonSuperClass(c1, c2));
        }
        int r = th.relationship(t1, t2);
        if (r == TypeHierarchy.SAME_TYPE) {
            return t1;
        } else if (r == TypeHierarchy.SUBSUMED_BY) {
            return t2;
        } else if (r == TypeHierarchy.SUBSUMES) {
            return t1;
//#if EE==true || PE==true
        } else if (t1 instanceof MapType && t2 instanceof MapType) {
            AtomicType k = (AtomicType)getCommonSuperType(((MapType)t1).getKeyType(), ((MapType)t2).getKeyType());
            SequenceType v1 = ((MapType)t1).getValueType();
            SequenceType v2 = ((MapType)t2).getValueType();
            SequenceType v = SequenceType.makeSequenceType(
                    getCommonSuperType(v1.getPrimaryType(), v2.getPrimaryType()),
                    Cardinality.union(v1.getCardinality(), v2.getCardinality()));
            return new MapType(k, v);
//#endif
        } else {
            ItemType st = t2.getSuperType(th);
            if (st == null) {
                return AnyItemType.getInstance();
            } else {
                return getCommonSuperType(st, t1, th);
            }
            // eventually we will hit a type that is a supertype of t2. We reverse
            // the arguments so we go up each branch of the tree alternately.
            // If we hit the root of the tree, one of the earlier conditions will be satisfied,
            // so the recursion will stop.
        }
    }

    /**
     * Get a type that is a common supertype of two given item types, without the benefit of a TypeHierarchy cache.
     * This will generally give a less precise answer than the method {@link #getCommonSuperType(ItemType, ItemType, TypeHierarchy)}
     *
     * @param t1 the first item type
     * @param t2 the second item type
     * @return an item type that is a supertype of both the supplied item types
     */

    /*@NotNull*/
    public static ItemType getCommonSuperType(/*@NotNull*/ ItemType t1, /*@NotNull*/ ItemType t2) {
        if (t1 == t2) {
            return t1;
        }
        if (t1 instanceof ErrorType) {
            return t2;
        }
        if (t2 instanceof ErrorType) {
            return t1;
        }
        if (t1 == AnyItemType.getInstance() || t2 == AnyItemType.getInstance()) {
            return AnyItemType.getInstance();
        }
        ItemType p1 = t1.getPrimitiveItemType();
        ItemType p2 = t2.getPrimitiveItemType();
        if (p1 == p2) {
            return p1;
        }
        if ((p1 == BuiltInAtomicType.DECIMAL && p2 == BuiltInAtomicType.INTEGER) ||
                (p2 == BuiltInAtomicType.DECIMAL && p1 == BuiltInAtomicType.INTEGER)) {
            return BuiltInAtomicType.DECIMAL;
        }
        if (p1 instanceof BuiltInAtomicType && ((BuiltInAtomicType) p1).isNumericType() &&
                p2 instanceof BuiltInAtomicType && ((BuiltInAtomicType) p2).isNumericType()) {
            return BuiltInAtomicType.NUMERIC;
        }
        if (t1.isAtomicType() && t2.isAtomicType()) {
            return BuiltInAtomicType.ANY_ATOMIC;
        }
        if (t1 instanceof NodeTest && t2 instanceof NodeTest) {
            return AnyNodeTest.getInstance();
        }
        if (t1 instanceof JavaExternalObjectType && t2 instanceof JavaExternalObjectType) {
            Class c1 = ((JavaExternalObjectType) t1).getJavaClass();
            Class c2 = ((JavaExternalObjectType) t2).getJavaClass();
            return new JavaExternalObjectType(leastCommonSuperClass(c1, c2));
        }
        return AnyItemType.getInstance();

        // Note: for function items, the result is always AnyFunctionType, since all functions have the same primitive type

    }

    private static Class leastCommonSuperClass(Class class1, Class class2) {
        if (class1 == class2) {
            return class1;
        }
        if (class1 == null || class2 == null) {
            return null;
        }
        if (!class1.isArray() && class1.isAssignableFrom(class2)) {
            return class1;
        }
        if (!class2.isArray() && class2.isAssignableFrom(class1)) {
            return class2;
        }
        if (class1.isInterface() || class2.isInterface()) {
            return Object.class;
        }
        return leastCommonSuperClass(class1.getSuperclass(), class2.getSuperclass());
    }


    /**
     * Determine whether this type is a primitive type. The primitive types are
     * the 19 primitive types of XML Schema, plus xs:integer, xs:dayTimeDuration and xs:yearMonthDuration;
     * xs:untypedAtomic; the 7 node kinds; and all supertypes of these (item(), node(), xs:anyAtomicType,
     * xs:numeric, ...)
     *
     * @param code the item type code to be tested
     * @return true if the type is considered primitive under the above rules
     */
    public static boolean isPrimitiveType(int code) {
        return code >= 0 && (code <= StandardNames.XS_INTEGER ||
                code == StandardNames.XS_NUMERIC ||
                code == StandardNames.XS_UNTYPED_ATOMIC ||
                code == StandardNames.XS_ANY_ATOMIC_TYPE ||
                code == StandardNames.XS_DAY_TIME_DURATION ||
                code == StandardNames.XS_YEAR_MONTH_DURATION ||
                code == StandardNames.XS_ANY_SIMPLE_TYPE);
    }

    /**
     * Determine whether two primitive atomic types are comparable under the rules for ValueComparisons
     * (that is, untyped atomic values treated as strings)
     *
     * @param t1      the first type to compared.
     *                This must be a primitive atomic type as defined by {@link ItemType#getPrimitiveType}
     * @param t2      the second type to compared.
     *                This must be a primitive atomic type as defined by {@link ItemType#getPrimitiveType}
     * @param ordered true if testing for an ordering comparison (lt, gt, le, ge). False
     *                if testing for an equality comparison (eq, ne)
     * @return true if the types are comparable, as defined by the rules of the "eq" operator; false if they
     *         are not comparable, or if we don't yet know (because some subtypes of the static type are comparable
     *         and others are not)
     */

    public static boolean isGuaranteedComparable(/*@NotNull*/ BuiltInAtomicType t1, /*@NotNull*/ BuiltInAtomicType t2, boolean ordered) {
        if (t1 == t2) {
            return true; // short cut
        }
//        if (t1.equals(BuiltInAtomicType.ANY_ATOMIC) || t2.equals(BuiltInAtomicType.ANY_ATOMIC)) {
//            return true; // meaning we don't actually know at this stage
//        }
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

    /**
     * Determine whether two primitive atomic types are comparable under the rules for ValueComparisons
     * (that is, untyped atomic values treated as strings)
     *
     * @param t1      the first type to compared.
     *                This must be a primitive atomic type as defined by {@link ItemType#getPrimitiveType}
     * @param t2      the second type to compared.
     *                This must be a primitive atomic type as defined by {@link ItemType#getPrimitiveType}
     * @param ordered true if testing for an ordering comparison (lt, gt, le, ge). False
     *                if testing for an equality comparison (eq, ne)
     * @return true if the types are guaranteed comparable, as defined by the rules of the "eq" operator,
     *         or if we don't yet know (because some subtypes of the static type are comparable
     *         and others are not). False if they are definitely not comparable.
     */

    public static boolean isPossiblyComparable(/*@NotNull*/ BuiltInAtomicType t1, /*@NotNull*/ BuiltInAtomicType t2, boolean ordered) {
        if (t1 == t2) {
            return true; // short cut
        }
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
        return t1 == t2;
    }


    /**
     * Determine whether two primitive atomic types are comparable under the rules for GeneralComparisons
     * (that is, untyped atomic values treated as comparable to anything)
     *
     * @param t1      the first type to compared.
     *                This must be a primitive atomic type as defined by {@link ItemType#getPrimitiveType}
     * @param t2      the second type to compared.
     *                This must be a primitive atomic type as defined by {@link ItemType#getPrimitiveType}
     * @param ordered true if testing for an ordering comparison (lt, gt, le, ge). False
     *                if testing for an equality comparison (eq, ne)
     * @return true if the types are comparable, as defined by the rules of the "=" operator
     */

    public static boolean isGenerallyComparable(/*@NotNull*/ BuiltInAtomicType t1, /*@NotNull*/ BuiltInAtomicType t2, boolean ordered) {
        return t1.equals(BuiltInAtomicType.ANY_ATOMIC)
                || t2.equals(BuiltInAtomicType.ANY_ATOMIC)
                || t1.equals(BuiltInAtomicType.UNTYPED_ATOMIC)
                || t2.equals(BuiltInAtomicType.UNTYPED_ATOMIC)
                || isGuaranteedComparable(t1, t2, ordered);
    }

    /**
     * Determine whether two primitive atomic types are guaranteed comparable under the rules for GeneralComparisons
     * (that is, untyped atomic values treated as comparable to anything). This method returns false if a run-time
     * check is necessary.
     *
     * @param t1      the first type to compared.
     *                This must be a primitive atomic type as defined by {@link ItemType#getPrimitiveType}
     * @param t2      the second type to compared.
     *                This must be a primitive atomic type as defined by {@link ItemType#getPrimitiveType}
     * @param ordered true if testing for an ordering comparison (lt, gt, le, ge). False
     *                if testing for an equality comparison (eq, ne)
     * @return true if the types are comparable, as defined by the rules of the "=" operator
     */

    public static boolean isGuaranteedGenerallyComparable(/*@NotNull*/ BuiltInAtomicType t1, /*@NotNull*/ BuiltInAtomicType t2, boolean ordered) {
        return !(t1.equals(BuiltInAtomicType.ANY_ATOMIC) || t2.equals(BuiltInAtomicType.ANY_ATOMIC))
                && isGenerallyComparable(t1, t2, ordered);
    }


}

