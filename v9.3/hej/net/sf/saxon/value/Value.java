package net.sf.saxon.value;
import net.sf.saxon.event.SequenceReceiver;
import net.sf.saxon.expr.*;
import net.sf.saxon.functions.Count;
import net.sf.saxon.om.*;
import net.sf.saxon.tree.iter.EmptyIterator;
import net.sf.saxon.tree.iter.SingletonIterator;
import net.sf.saxon.trans.XPathException;
import net.sf.saxon.type.*;
import net.sf.saxon.tree.wrapper.VirtualNode;
import net.sf.saxon.tree.util.FastStringBuffer;

import java.io.Serializable;

/**
* A value is the result of an expression but it is also an expression in its own right.
* Note that every value can be regarded as a sequence - in many cases, a sequence of
* length one.
*/

public abstract class Value
        implements Serializable, SequenceIterable, ValueRepresentation {

    /**
     * Static method to make a Value from a given Item (which may be either an AtomicValue
     * or a NodeInfo or a FunctionItem
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
            return new SingletonItem((Item)val);
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
            throw new XPathException("Attempting to access a sequence as a singleton item");
        } else {
            return item;
        }
    }

    /**
     * Static method to get a Value from an Item
     * @param item the supplied item
     * @return the item expressed as a Value
     */

    public static Value fromItem(Item item) {
        if (item == null) {
            return EmptySequence.getInstance();
        } else if (item instanceof AtomicValue) {
            return (AtomicValue)item;
        } else {
            return new SingletonItem(item);
        }
    }

    /**
     * Static method to get an Iterator over any ValueRepresentation (which may be either a Value
     * or a NodeInfo or a FunctionItem
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
            return SingletonIterator.makeIterator((Item)val);
        }
    }

    /**
     * Get a SequenceIterator over a ValueRepresentation
     * @param val the value to iterate over
     * @return the iterator
     */

    public static SequenceIterator getIterator(ValueRepresentation val) throws XPathException {
        if (val instanceof Value) {
            return ((Value)val).iterate();
        } else if (val instanceof Item) {
            return SingletonIterator.makeIterator((Item)val);
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
        // Note, this method, and the SequenceIterable interface, are used from XQuery compiled code
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
     * @return the canonical lexical representation if defined in XML Schema; otherwise, the result
     * of casting to string according to the XPath 2.0 rules
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
     * @return the cardinality
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
     * @return the number of items in the sequence
     */

    public int getLength() throws XPathException {
        return Count.count(iterate());
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
        FastStringBuffer sb = new FastStringBuffer(FastStringBuffer.SMALL);
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
     * The default implementation is written to compare sequences of atomic values.
     * This method is overridden for AtomicValue and its subclasses.
     *
     * <p>In the case of data types that are partially ordered, the returned Comparable extends the standard
     * semantics of the compareTo() method by returning the value {@link #INDETERMINATE_ORDERING} when there
     * is no defined order relationship between two given values.</p>
     *
     * @return a Comparable that follows XML Schema comparison rules
     */

    public Comparable getSchemaComparable() {
        return new ValueSchemaComparable();
    }

    private class ValueSchemaComparable implements Comparable {
        public Value getValue() {
            return Value.this;
        }
        public int compareTo(Object obj) {
            try {
                if (obj instanceof ValueSchemaComparable) {
                    SequenceIterator iter1 = getValue().iterate();
                    SequenceIterator iter2 = ((ValueSchemaComparable)obj).getValue().iterate();
                    while (true) {
                        Item item1 = iter1.next();
                        Item item2 = iter2.next();
                        if (item1 == null && item2 == null) {
                            return 0;
                        }
                        if (item1 == null) {
                            return -1;
                        } else if (item2 == null) {
                            return +1;
                        }
                        if (!(item1 instanceof AtomicValue && item2 instanceof AtomicValue)) {
                            throw new UnsupportedOperationException(
                                    "Sequences containing nodes or function items are not schema-comparable");
                        }
                        int c = ((AtomicValue)item1).getSchemaComparable().compareTo(
                                    ((AtomicValue)item2).getSchemaComparable());
                        if (c != 0) {
                            return c;
                        }
                    }
                } else {
                    return INDETERMINATE_ORDERING;
                }
            } catch (XPathException e) {
                throw new AssertionError("Failure comparing schema values: " + e.getMessage());
            }
        }

        public boolean equals(Object obj) {
            return compareTo(obj) == 0;
        }

        public int hashCode() {
            try {
                int hash = 0x06639662;  // arbitrary seed
                SequenceIterator iter = getValue().iterate();
                while (true) {
                    Item item = iter.next();
                    if (item == null) {
                        return hash;
                    }
                    hash ^= ((AtomicValue)item).getSchemaComparable().hashCode();
                }
            } catch (XPathException e) {
                return 0;
            }
        }
    }

    /**
     * Constant returned by compareTo() method to indicate an indeterminate ordering between two values
     */

    public static final int INDETERMINATE_ORDERING = Integer.MIN_VALUE;

    /**
     * Compare two (sequence) values for equality. This method throws an UnsupportedOperationException,
     * because it should not be used: there are too many "equality" operators that can be defined on
     * values for the concept to be meaningful.
     * <p>Consider creating an XPathComparable from each value, and comparing those; or creating a
     * SchemaComparable to achieve equality comparison as defined in XML Schema.</p>
     * @throws UnsupportedOperationException (always)
     */

    public boolean equals(Object obj) {
        throw new UnsupportedOperationException("Value.equals()");
    }

    public int hashCode() {
        return 42;
    }

    /**
     * Determine whether two values are identical, as determined by XML Schema rules. This is a stronger
     * test than equality (even schema-equality); for example two dateTime values are not identical unless
     * they are in the same timezone.
     * <p>Note that even this check ignores the type annotation of the value. The integer 3 and the short 3
     * are considered identical, even though they are not fully interchangeable. "Identical" means the
     * same point in the value space, regardless of type annotation.</p>
     * <p>Although the schema rules cover atomic values only, this method also handles values that include nodes,
     * using node identity in this case.</p>
     * <p>The empty sequence is considered identical to the empty sequence.</p>
     * <p>NaN is identical to itself.</p>
     * <p>Function items are not identical to anything except themselves
     * @param v the other value to be compared with this one
     * @return true if the two values are identical, false otherwise.
     */

    public boolean isIdentical(Value v) {
        try {
            SequenceIterator i0 = iterate();
            SequenceIterator i1 = v.iterate();
            while (true) {
                Item m0 = i0.next();
                Item m1 = i1.next();
                if (m0==null && m1==null) {
                    return true;
                }
                if (m0==null || m1==null) {
                    return false;
                }
                boolean n0 = (m0 instanceof NodeInfo);
                boolean n1 = (m1 instanceof NodeInfo);
                if (n0 != n1) {
                    return false;
                }
                if (n0 && n1 && !((NodeInfo)m0).isSameNodeInfo((NodeInfo)m1)) {
                    return false;
                }
                boolean a0 = (m0 instanceof AtomicValue);
                boolean a1 = (m1 instanceof AtomicValue);
                if (a0 && a1 && !((AtomicValue)m0).isIdentical((AtomicValue)m1)) {
                    return false;
                }
                if ((!a0 || !a1) && m0 != m1) {
                    // one of them is a function item, and they are not the same function item
                    return false;
                }
            }
        } catch (XPathException err) {
            return false;
        }
    }


    /**
     * Check statically that the results of the expression are capable of constructing the content
     * of a given schema type.
     * @param parentType The schema type
     * @param env the static context
     * @param whole true if this value accounts for the entire content of the containing node
     * @throws XPathException if the expression doesn't match the required content type
     */

    public void checkPermittedContents(SchemaType parentType, StaticContext env, boolean whole) throws XPathException {
        //return;
    }

    /**
     * Reduce a value to its simplest form. If the value is a closure or some other form of deferred value
     * such as a FunctionCallPackage, then it is reduced to a SequenceExtent. If it is a SequenceExtent containing
     * a single item, then it is reduced to that item. One consequence that is exploited by class FilterExpression
     * is that if the value is a singleton numeric value, then the result will be an instance of NumericValue
     * @return the value in simplified form
     */

    public Value reduce() throws XPathException {
        return this;
    }


    /**
     * Convert an XPath value to a Java object.
     * An atomic value is returned as an instance
     * of the best available Java class. If the item is a node, the node is "unwrapped",
     * to return the underlying node in the original model (which might be, for example,
     * a DOM or JDOM node).
     * @param item the item to be converted
     * @return the value after conversion
    */

    public static Object convertToJava(Item item) throws XPathException {
        if (item instanceof NodeInfo) {
            Object node = item;
            while (node instanceof VirtualNode) {
                // strip off any layers of wrapping
                node = ((VirtualNode)node).getRealNode();
            }
            return node;
        } else if (item instanceof FunctionItem) {
            return item;
        } else if (item instanceof ObjectValue) {
            return ((ObjectValue)item).getObject();
        } else {
            AtomicValue value = (AtomicValue)item;
            switch (value.getItemType(null).getPrimitiveType()) {
                case StandardNames.XS_STRING:
                case StandardNames.XS_UNTYPED_ATOMIC:
                case StandardNames.XS_ANY_URI:
                case StandardNames.XS_DURATION:
                    return value.getStringValue();
                case StandardNames.XS_BOOLEAN:
                    return (((BooleanValue)value).getBooleanValue() ? Boolean.TRUE : Boolean.FALSE );
                case StandardNames.XS_DECIMAL:
                    return ((DecimalValue)value).getDecimalValue();
                case StandardNames.XS_INTEGER:
                    return Long.valueOf(((NumericValue)value).longValue());
                case StandardNames.XS_DOUBLE:
                    return new Double(((DoubleValue)value).getDoubleValue());
                case StandardNames.XS_FLOAT:
                    return new Float(((FloatValue)value).getFloatValue());
                case StandardNames.XS_DATE_TIME:
                    return ((DateTimeValue)value).getCalendar().getTime();
                case StandardNames.XS_DATE:
                    return ((DateValue)value).getCalendar().getTime();
                case StandardNames.XS_TIME:
                    return value.getStringValue();
                case StandardNames.XS_BASE64_BINARY:
                    return ((Base64BinaryValue)value).getBinaryValue();
                case StandardNames.XS_HEX_BINARY:
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
