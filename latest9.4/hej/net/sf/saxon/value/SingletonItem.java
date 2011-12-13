package net.sf.saxon.value;

import net.sf.saxon.expr.StaticProperty;
import net.sf.saxon.expr.XPathContext;
import net.sf.saxon.expr.parser.Token;
import net.sf.saxon.om.*;
import net.sf.saxon.pattern.*;
import net.sf.saxon.trans.XPathException;
import net.sf.saxon.tree.iter.AxisIterator;
import net.sf.saxon.tree.iter.SingletonIterator;
import net.sf.saxon.type.*;

/**
* A value that is a sequence containing zero or one items. Used only for items that are not atomic values
 * (that is, nodes, and function items)
*/

public class SingletonItem<T extends Item> extends Value<T> implements GroundedValue<T> {

    /*@Nullable*/ protected T item = null;


    /**
     * Create a node-set containing zero or one nodes
     * @param item The node or function-item to be contained in the node-set, or null if the sequence
     * is to be empty
    */

    public SingletonItem(T item) {
        this.item = item;
    }

    /**
     * Return the value in the form of an Item
     * @return the value in the form of an Item
     */

    /*@Nullable*/ public T asItem() {
        return item;
    }

    /**
      * Process the instruction, without returning any tail calls
      * @param context The dynamic context, giving access to the current node,
      * the current variables, etc.
      */

    public void process(/*@NotNull*/ XPathContext context) throws XPathException {
        if (item != null) {
            context.getReceiver().append(item, 0, NodeInfo.ALL_NAMESPACES);
        }
    }


    /**
     * Determine the data type of the items in the expression. This method determines the most
     * precise type that it can, because it is called when testing that the node conforms to a required
     * type.
     * @return the most precise possible type of the node.
     * @param th the type hierarchy cache
     */

    /*@NotNull*/
    public ItemType getItemType(TypeHierarchy th) {
        if (item == null) {
            return AnyItemType.getInstance();
        }
        if (item instanceof NodeInfo) {
            NodeInfo node = ((NodeInfo)item);
            if (th == null) {
                th = node.getConfiguration().getTypeHierarchy();
            }
            switch (node.getNodeKind()) {
                case Type.DOCUMENT:
                    // Need to know whether the document is well-formed and if so what the element type is
                    AxisIterator iter = node.iterateAxis(Axis.CHILD);
                    ItemType elementType = null;
                    while (true) {
                        NodeInfo n = iter.next();
                        if (n==null) {
                            break;
                        }
                        int kind = n.getNodeKind();
                        if (kind==Type.TEXT) {
                            elementType = null;
                            break;
                        } else if (kind==Type.ELEMENT) {
                            if (elementType != null) {
                                elementType = null;
                                break;
                            }
                            elementType = new SingletonItem<NodeInfo>(n).getItemType(th);
                        }
                    }
                    if (elementType == null) {
                        return NodeKindTest.DOCUMENT;
                    } else {
                        return new DocumentNodeTest((NodeTest)elementType);
                    }

                case Type.ELEMENT:
                    SchemaType eltype = node.getSchemaType();
                    if (eltype.equals(Untyped.getInstance()) || eltype.equals(AnyType.getInstance())) {
                        return new NameTest(Type.ELEMENT, node.getFingerprint(), node.getNamePool());
                    } else {
                        return new CombinedNodeTest(
                                new NameTest(Type.ELEMENT, node.getFingerprint(), node.getNamePool()),
                                Token.INTERSECT,
                                new ContentTypeTest(Type.ELEMENT, eltype, node.getConfiguration()));
                    }

                case Type.ATTRIBUTE:
                    SchemaType attype = node.getSchemaType();
                    if (attype.equals(BuiltInAtomicType.UNTYPED_ATOMIC)) {
                        return new NameTest(Type.ATTRIBUTE, node.getFingerprint(), node.getNamePool());
                    } else {
                        return new CombinedNodeTest(
                                new NameTest(Type.ATTRIBUTE, node.getFingerprint(), node.getNamePool()),
                                Token.INTERSECT,
                                new ContentTypeTest(Type.ATTRIBUTE, attype, node.getConfiguration()));
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
        } else if (item instanceof FunctionItem) {
            // item is not a node
            return ((FunctionItem)item).getFunctionItemType(th);
        } else {
            // it must be an atomic value, though we don't use this option
            return ((AtomicValue)item).getTypeLabel();
        }
    }

    /**
    * Determine the static cardinality
    */

    public int getCardinality() {
        if (item ==null) {
            return StaticProperty.EMPTY;
        } else {
            return StaticProperty.EXACTLY_ONE;
        }
    }

    /**
     * Get the length of the sequence
     */

    public int getLength() {
        return (item ==null ? 0 : 1);
    }

    /**
     * Get the n'th item in the sequence (starting from 0). This is defined for all
     * SequenceValues, but its real benefits come for a SequenceValue stored extensionally
     * (or for a MemoClosure, once all the values have been read)
     */

    /*@Nullable*/ public T itemAt(int n) {
        if (n==0 && item !=null) {
            return item;
        } else {
            return null;
        }
    }


    /**
     * Get a subsequence of the value
     *
     *
     * @param start  the index of the first item to be included in the result, counting from zero.
     *               A negative value is taken as zero. If the value is beyond the end of the sequence, an empty
     *               sequence is returned
     * @param length the number of items to be included in the result. Specify Integer.MAX_VALUE to
     *               get the subsequence up to the end of the base sequence. If the value is negative, an empty sequence
     *               is returned. If the value goes off the end of the sequence, the result returns items up to the end
     *               of the sequence
     * @return the required subsequence. If min is
     */

    /*@NotNull*/ public GroundedValue<T> subsequence(int start, int length) {
        if (item != null && start <= 0 && start+length > 0) {
            return this;
        } else {
            return EmptySequence.getInstance();
        }
    }

    /**
    * Return an enumeration of this nodeset value.
    */

    /*@NotNull*/ public SequenceIterator<T> iterate() {
        return SingletonIterator.makeIterator(item);
    }

    /**
     * Get the effective boolean value
     */

    public boolean effectiveBooleanValue() {
        return (item != null);
    }

    /**
     * Convert the value to a string, using the serialization rules.
     * For atomic values this is the same as a cast; for sequence values
     * it gives a space-separated list. For QNames and NOTATIONS, or lists
     * containing them, it fails.
     */

    /*@NotNull*/ public String getStringValue() {
        return (item ==null ? "" : item.getStringValue());
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