package net.sf.saxon.type;

import net.sf.saxon.Configuration;
import net.sf.saxon.expr.sort.IntHashSet;
import net.sf.saxon.expr.sort.IntIterator;
import net.sf.saxon.expr.sort.IntSet;
import net.sf.saxon.om.NamePool;
import net.sf.saxon.om.StandardNames;
import net.sf.saxon.pattern.*;

import java.io.Serializable;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * This class exists to provide answers to questions about the type hierarchy. Because
 * such questions are potentially expensive, it caches the answers. There is one instance of
 * this class for a Configuration.
 */

public class TypeHierarchy implements Serializable {

    private Map<ItemTypePair, Integer> map;
    private Configuration config;

    /**
     * Constant denoting relationship between two types: A is the same type as B
     */
    public static final int SAME_TYPE = 0;
    /**
     * Constant denoting relationship between two types: A subsumes B
     */
    public static final int SUBSUMES = 1;
    /**
     * Constant denoting relationship between two types: A is subsumed by B
     */
    public static final int SUBSUMED_BY = 2;
    /**
     * Constant denoting relationship between two types: A overlaps B
     */
    public static final int OVERLAPS = 3;
    /**
     * Constant denoting relationship between two types: A is disjoint from B
     */
    public static final int DISJOINT = 4;

    //private String[] relnames = {"SAME", "SUBSUMES", "SUBSUMED_BY", "OVERLAPS", "DISJOINT"};

    /**
     * Create the type hierarchy cache for a configuration
     * @param config the configuration
     */

    public TypeHierarchy(Configuration config){
        this.config = config;
        map = new ConcurrentHashMap<ItemTypePair, Integer>();
    }

    /**
     * Get the Saxon configuration to which this type hierarchy belongs
     * @return the configuration
     */

    public Configuration getConfiguration() {
        return config;
    }

    /**
     * Determine whether type A is type B or one of its subtypes, recursively.
     * "Subtype" here means a type that is subsumed, that is, a type whose instances
     * are a subset of the instances of the other type.
     *
     * @param subtype identifies the first type
     * @param supertype identifies the second type
     * @return true if the first type is the second type or is subsumed by the second type
     */

    public boolean isSubType(ItemType subtype, /*@NotNull*/ ItemType supertype) {
        int relation = relationship(subtype, supertype);
        return (relation==SAME_TYPE || relation==SUBSUMED_BY);
    }

    /**
     * Determine the relationship of one item type to another.
     * @param t1 the first item type
     * @param t2 the second item type
     * @return {@link #SAME_TYPE} if the types are the same; {@link #SUBSUMES} if the first
     * type subsumes the second (that is, all instances of the second type are also instances
     * of the first); {@link #SUBSUMED_BY} if the second type subsumes the first;
     * {@link #OVERLAPS} if the two types overlap (have a non-empty intersection, but neither
     * subsumes the other); {@link #DISJOINT} if the two types are disjoint (have an empty intersection)
     */

    public int relationship(/*@Nullable*/ ItemType t1, /*@NotNull*/ ItemType t2) {
        if (t1 == null) {
            throw new NullPointerException();
        }
        if (t1.equals(t2)) {
            return SAME_TYPE;
        }
        ItemTypePair pair = new ItemTypePair(t1, t2);
        Integer result = map.get(pair);
        if (result == null) {
            result = computeRelationship(t1, t2);
            map.put(pair, result);
        }
        return result;
    }

    /**
     * Determine the relationship of one item type to another.
     * @param t1 the first item type
     * @param t2 the second item type
     * @return {@link #SAME_TYPE} if the types are the same; {@link #SUBSUMES} if the first
     * type subsumes the second (that is, all instances of the second type are also instances
     * of the first); {@link #SUBSUMED_BY} if the second type subsumes the first;
     * {@link #OVERLAPS} if the two types overlap (have a non-empty intersection, but neither
     * subsumes the other); {@link #DISJOINT} if the two types are disjoint (have an empty intersection)
     */

    private int computeRelationship(/*@NotNull*/ ItemType t1, /*@NotNull*/ ItemType t2) {
        //System.err.println("computeRelationship " + t1 + ", " + t2);
        if (t1 == t2) {
            return SAME_TYPE;
        }
        if (t1 instanceof AnyItemType) {
            if (t2 instanceof AnyItemType) {
                return SAME_TYPE;
            } else {
                return SUBSUMES;
            }
        } else if (t2 instanceof AnyItemType) {
            return SUBSUMED_BY;
        } else if (t1.isPlainType()) {
            if (t2 instanceof NodeTest || t2 instanceof FunctionItemType) {
                return DISJOINT;
            } else if (t1 instanceof ExternalObjectType) {
                if (t2 instanceof ExternalObjectType) {
                    return ((ExternalObjectType)t1).getRelationship((ExternalObjectType)t2);
                } else if (((AtomicType)t2).getFingerprint() == StandardNames.XS_ANY_ATOMIC_TYPE) {
                    return SUBSUMED_BY;
                } else {
                    return DISJOINT;
                }
            } else if (t2 instanceof ExternalObjectType) {
                if (((AtomicType)t1).getFingerprint() == StandardNames.XS_ANY_ATOMIC_TYPE) {
                    return SUBSUMES;
                } else {
                    return DISJOINT;
                }
            } else if (t1 instanceof AtomicType && t2 instanceof AtomicType) {
                if (((AtomicType)t1).getFingerprint() == ((AtomicType)t2).getFingerprint()) {
                    return SAME_TYPE;
                }
                ItemType t = t2;
                while (t.isPlainType()) {
                    if (((AtomicType)t1).getFingerprint() == ((AtomicType)t).getFingerprint()) {
                        return SUBSUMES;
                    }
                    t = t.getSuperType(this);
                }
                t = t1;
                while (t.isPlainType()) {
                    if (((AtomicType)t).getFingerprint() == ((AtomicType)t2).getFingerprint()) {
                        return SUBSUMED_BY;
                    }
                    t = t.getSuperType(this);
                }
                return DISJOINT;
            } else if (t1.isPlainType() && !(t1.isAtomicType()) && t2 instanceof PlainType) {
                // relationship(union, atomic) or relationship(union, union)
                boolean overlap = false;
                Set<PlainType> s1 = ((PlainType)t1).getPlainMemberTypes();
                for (PlainType a1 : s1) {
                    if (a1.equals(t2)) {
                        return SUBSUMES;
                    }
                    int r = relationship(a1, t2);
                    if (r == SUBSUMES || r == SAME_TYPE) {
                        return SUBSUMES;
                    }
                    if (r != DISJOINT) {
                        overlap = true;
                    }
                }
                return (overlap ? OVERLAPS : DISJOINT);
            } else if (t1 instanceof AtomicType) {
                // relationship (atomic, union)
                int r = relationship(t2, t1);
                return inverseRelationship(r);

            }
            else {
                // all options exhausted
                throw new IllegalStateException();
            }
        } else if (t1 instanceof NodeTest) {
            if (t2.isPlainType() || (t2 instanceof FunctionItemType)) {
                return DISJOINT;
            } else {
                // both types are NodeTests
                if (t1 instanceof AnyNodeTest) {
                    if (t2 instanceof AnyNodeTest) {
                        return SAME_TYPE;
                    } else {
                        return SUBSUMES;
                    }
                } else if (t2 instanceof AnyNodeTest) {
                    return SUBSUMED_BY;
                } else if (t1 instanceof EmptySequenceTest) {
                    return DISJOINT;
                } else if (t2 instanceof EmptySequenceTest) {
                    return DISJOINT;
                } else {
                    // first find the relationship between the node kinds allowed
                    int nodeKindRelationship;
                    int m1 = ((NodeTest)t1).getNodeKindMask();
                    int m2 = ((NodeTest)t2).getNodeKindMask();
                    if ((m1 & m2) == 0) {
                        return DISJOINT;
                    } else if (m1 == m2) {
                        nodeKindRelationship = SAME_TYPE;
                    } else if ((m1 & m2) == m1) {
                        nodeKindRelationship = SUBSUMED_BY;
                    } else if ((m1 & m2) == m2) {
                        nodeKindRelationship = SUBSUMES;
                    } else {
                        nodeKindRelationship = OVERLAPS;
                    }

                    // now find the relationship between the node names allowed. Note that although
                    // NamespaceTest and LocalNameTest are NodeTests, they do not occur in SequenceTypes,
                    // so we don't need to consider them.
                    int nodeNameRelationship;
                    IntSet n1 = ((NodeTest)t1).getRequiredNodeNames(); // null means all names allowed
                    IntSet n2 = ((NodeTest)t2).getRequiredNodeNames(); // null means all names allowed
                    if (n1 == null) {
                        if (n2 == null) {
                            nodeNameRelationship = SAME_TYPE;
                        } else {
                            nodeNameRelationship = SUBSUMES;
                        }
                    } else if (n2 == null) {
                        nodeNameRelationship = SUBSUMED_BY;
                    } else if (n1.containsAll(n2)) {
                        if (n1.size() == n2.size()) {
                            nodeNameRelationship = SAME_TYPE;
                        } else {
                            nodeNameRelationship = SUBSUMES;
                        }
                    } else if (n2.containsAll(n1)) {
                        nodeNameRelationship = SUBSUMED_BY;
                    } else if (IntHashSet.containsSome(n1, n2)) {
                        nodeNameRelationship = OVERLAPS;
                    } else {
                        nodeNameRelationship = DISJOINT;
                    }

                    // now find the relationship between the content types allowed

                    int contentRelationship;

                    if (t1 instanceof DocumentNodeTest) {
                        if (t2 instanceof DocumentNodeTest) {
                            contentRelationship = relationship(((DocumentNodeTest)t1).getElementTest(),
                                ((DocumentNodeTest)t2).getElementTest());
                        } else {
                            contentRelationship = SUBSUMED_BY;
                        }
                    } else if (t2 instanceof DocumentNodeTest) {
                        contentRelationship = SUBSUMES;
                    } else {
                        // This is changed as a result of spec bug 10065. If t2 is schema-element(e2), it is
                        // no longer sufficient to check the content type of t1 against the declared type of e2
                        // (which might be xs:anyType, as in test schvalid010). Instead it must be checked against
                        // the declared type of the element declaration corresponding to the actual name N of
                        // the element being tested.
                        SchemaType s1 = ((NodeTest)t1).getContentType();
                        SchemaType s2 = ((NodeTest)t2).getContentType();
                        if (t1 instanceof SchemaNodeTest && n2 != null && n2.size() == 1) {
                            IntIterator ii2 = n2.iterator();
                            int name2 = (ii2.hasNext() ? ii2.next() : -1);
                            SchemaDeclaration decl2 = config.getElementDeclaration(name2);
                            if (decl2 != null) {
                                s1 = decl2.getType();
                            }
                        }
                        if (t2 instanceof SchemaNodeTest && n1 != null && n1.size() == 1) {
                            IntIterator ii1 = n1.iterator();
                            int name1 = (ii1.hasNext() ? ii1.next() : -1);
                            SchemaDeclaration decl1 = config.getElementDeclaration(name1);
                            if (decl1 != null) {
                                s2 = decl1.getType();
                            }
                        }
                        contentRelationship = schemaTypeRelationship(s1, s2);
                    }

                    // now analyse the three different relationsships

                    if (nodeKindRelationship == SAME_TYPE &&
                            nodeNameRelationship == SAME_TYPE &&
                            contentRelationship == SAME_TYPE) {
                        return SAME_TYPE;
                    } else if ((nodeKindRelationship == SAME_TYPE || nodeKindRelationship == SUBSUMES) &&
                            (nodeNameRelationship == SAME_TYPE || nodeNameRelationship == SUBSUMES) &&
                            (contentRelationship == SAME_TYPE || contentRelationship == SUBSUMES)) {
                        return SUBSUMES;
                    } else if ((nodeKindRelationship == SAME_TYPE || nodeKindRelationship == SUBSUMED_BY) &&
                            (nodeNameRelationship == SAME_TYPE || nodeNameRelationship == SUBSUMED_BY) &&
                            (contentRelationship == SAME_TYPE || contentRelationship == SUBSUMED_BY)) {
                        return SUBSUMED_BY;
                    } else if (nodeKindRelationship == DISJOINT ||
                            nodeNameRelationship == DISJOINT ||
                            contentRelationship == DISJOINT) {
                        return DISJOINT;
                    } else {
                        return OVERLAPS;
                    }
                }
            }
        } else {
            // t1 is a FunctionItemType
            if (t2 instanceof FunctionItemType) {
                return ((FunctionItemType)t1).relationship((FunctionItemType)t2, this);
            } else {
                return DISJOINT;
            }
        }

    }

    private int inverseRelationship(int relation) {
        switch (relation) {
            case SAME_TYPE: return SAME_TYPE;
            case SUBSUMES: return SUBSUMED_BY;
            case SUBSUMED_BY: return SUBSUMES;
            case OVERLAPS: return OVERLAPS;
            case DISJOINT: return DISJOINT;
            default: throw new IllegalArgumentException();
        }
    }


    /**
     * Test whether a type annotation code represents the type xs:ID or one of its subtypes
     * @param typeCode the type annotation to be tested
     * @return true if the type annotation represents an xs:ID
     */

     public boolean isIdCode(int typeCode) {
         typeCode &= NamePool.FP_MASK;
         if (typeCode == StandardNames.XS_ID) {
             return true;
         } else if (typeCode < 1024) {
             // No other built-in type is an ID
             return false;
         } else {
             SchemaType type = config.getSchemaType(typeCode);
             if (type == null) {
                 return false;      // this shouldn't happen, but there's no need to crash right here
             }
             return type.isIdType();
         }
     }

    /**
     * Test whether a type annotation code represents the type xs:IDREF, xs:IDREFS or one of their subtypes
     * @param typeCode the type annotation to be tested
     * @return true if the type annotation represents an xs:IDREF or xs:IDREFS or a subtype thereof
     */

     public boolean isIdrefsCode(int typeCode) {
         typeCode &= NamePool.FP_MASK;
         if (typeCode == StandardNames.XS_IDREF || typeCode == StandardNames.XS_IDREFS) {
             return true;
         } else if (typeCode < 1024) {
             // No other built-in type is an IDREF or IDREFS
             return false;
         } else {
             SchemaType type = config.getSchemaType(typeCode);
             if (type == null) {
                 // shouldn't happen, but we don't need to crash right now
                 return false;
             }
             return type.isIdRefType();
         }
     }

    /**
     * Get the relationship of two schema types to each other
     * @param s1 the first type
     * @param s2 the second type
     * @return the relationship of the two types, as one of the constants
     * {@link net.sf.saxon.type.TypeHierarchy#SAME_TYPE}, {@link net.sf.saxon.type.TypeHierarchy#SUBSUMES},
     * {@link net.sf.saxon.type.TypeHierarchy#SUBSUMED_BY}, {@link net.sf.saxon.type.TypeHierarchy#DISJOINT}
     */

    public static int schemaTypeRelationship(/*@NotNull*/ SchemaType s1, SchemaType s2) {
        if (s1.isSameType(s2)) {
            return SAME_TYPE;
        }
        if (s1 instanceof AnyType) {
            return SUBSUMES;
        }
        if (s2 instanceof AnyType) {
            return SUBSUMED_BY;
        }
        SchemaType t1 = s1;
        while (true) {
            t1 = t1.getBaseType();
            if (t1 == null) {
                break;
            }
            if (t1.isSameType(s2)) {
                return SUBSUMED_BY;
            }
        }
        SchemaType t2 = s2;
        while (true) {
            t2 = t2.getBaseType();
            if (t2 == null) {
                break;
            }
            if (t2.isSameType(s1)) {
                return SUBSUMES;
            }
        }
        return DISJOINT;
    }


    private static class ItemTypePair implements Serializable {
        ItemType s;
        ItemType t;

        public ItemTypePair(ItemType s, ItemType t) {
            this.s = s;
            this.t = t;
        }

        /**
         * Returns a hash code value for the object.
         * @return a hash code value for this object.
         * @see Object#equals(Object)
         * @see java.util.Hashtable
         */
        public int hashCode() {
            return s.hashCode() ^ t.hashCode();
        }

        /**
         * Indicates whether some other object is "equal to" this one.
         */

        public boolean equals(Object obj) {
            if (obj instanceof ItemTypePair) {
                final ItemTypePair pair = (ItemTypePair)obj;
                return s.equals(pair.s) && t.equals(pair.t);
            } else {
                return false;
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
// The Original Code is: all this file
//
// The Initial Developer of the Original Code is Saxonica Limited.
// Portions created by ___ are Copyright (C) ___. All rights reserved.
//
// Contributor(s):
//