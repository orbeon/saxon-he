package net.sf.saxon.value;
import net.sf.saxon.expr.StaticProperty;
import net.sf.saxon.expr.Token;
import net.sf.saxon.expr.XPathContext;
import net.sf.saxon.om.*;
import net.sf.saxon.pattern.*;
import net.sf.saxon.trans.XPathException;
import net.sf.saxon.type.ItemType;
import net.sf.saxon.type.Type;
import net.sf.saxon.type.TypeHierarchy;

/**
* A value that is a sequence containing zero or one nodes
*/

public class SingletonNode extends Value {

    protected NodeInfo node = null;


    /**
     * Create a node-set containing zero or one nodes
     * @param node The node to be contained in the node-set, or null if the node-set
     * is to be empty
    */

    public SingletonNode(NodeInfo node) {
        this.node = node;
    }

    /**
     * Return the value in the form of an Item
     * @return the value in the form of an Item
     */

    public Item asItem() {
        return node;
    }

    /**
     * Determine whether the value is multivalued, that is, whether it is a sequence that
     * potentially contains more than one item
     *
     * @return true if the value might contain more than one item, false if it definitely
     *         contains zero or one items
     */

    public boolean isMultiValued() {
        return true;
    }

    /**
      * Process the instruction, without returning any tail calls
      * @param context The dynamic context, giving access to the current node,
      * the current variables, etc.
      */

    public void process(XPathContext context) throws XPathException {
        if (node != null) {
            context.getReceiver().append(node, 0, NodeInfo.ALL_NAMESPACES);
        }
    }


    /**
     * Determine the data type of the items in the expression. This method determines the most
     * precise type that it can, because it is called when testing that the node conforms to a required
     * type.
     * @return the most precise possible type of the node.
     * @param th
     */

    public ItemType getItemType(TypeHierarchy th) {
        switch (node.getNodeKind()) {
            case Type.DOCUMENT:
                // Need to know whether the document is well-formed and if so what the element type is
                AxisIterator iter = node.iterateAxis(Axis.CHILD);
                ItemType elementType = null;
                while (true) {
                    NodeInfo n = (NodeInfo)iter.next();
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
                        elementType = new SingletonNode(n).getItemType(th);
                    }
                }
                if (elementType == null) {
                    return NodeKindTest.DOCUMENT;
                } else {
                    return new DocumentNodeTest((NodeTest)elementType);
                }

            case Type.ELEMENT:
                int eltype = node.getTypeAnnotation();
                if (eltype == -1 || eltype == StandardNames.XS_UNTYPED || eltype == StandardNames.XS_ANY_TYPE) {
                    return new NameTest(Type.ELEMENT, node.getFingerprint(), node.getNamePool());
                } else {
                    return new CombinedNodeTest(
                            new NameTest(Type.ELEMENT, node.getFingerprint(), node.getNamePool()),
                            Token.INTERSECT,
                            new ContentTypeTest(Type.ELEMENT, node.getConfiguration().getSchemaType(eltype),
                                    node.getConfiguration()));
                }

            case Type.ATTRIBUTE:
                int attype = node.getTypeAnnotation();
                if (attype == -1 || attype == StandardNames.XS_UNTYPED_ATOMIC) {
                    return new NameTest(Type.ATTRIBUTE, node.getFingerprint(), node.getNamePool());
                } else {
                    return new CombinedNodeTest(
                            new NameTest(Type.ATTRIBUTE, node.getFingerprint(), node.getNamePool()),
                            Token.INTERSECT,
                            new ContentTypeTest(Type.ATTRIBUTE, node.getConfiguration().getSchemaType(attype),
                                    node.getConfiguration()));
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
    }

    /**
    * Determine the static cardinality
    */

    public int getCardinality() {
        if (node==null) {
            return StaticProperty.EMPTY;
        } else {
            return StaticProperty.EXACTLY_ONE;
        }
    }

    /**
     * Get the length of the sequence
     */

    public int getLength() throws XPathException {
        return (node==null ? 0 : 1);
    }

    /**
     * Get the n'th item in the sequence (starting from 0). This is defined for all
     * SequenceValues, but its real benefits come for a SequenceValue stored extensionally
     * (or for a MemoClosure, once all the values have been read)
     */

    public Item itemAt(int n) throws XPathException {
        if (n==0 && node!=null) {
            return node;
        } else {
            return null;
        }

    }

    /**
    * Get the node that forms the node-set. Return null if there is none.
    */

    public NodeInfo getNode() {
        return node;
    }

    /**
    * Return an enumeration of this nodeset value.
    */

    public SequenceIterator iterate() {
        return SingletonIterator.makeIterator(node);
    }

    /**
     * Get the effective boolean value
     */

    public boolean effectiveBooleanValue() {
        return (node != null);
    }

    /**
     * Convert the value to a string, using the serialization rules.
     * For atomic values this is the same as a cast; for sequence values
     * it gives a space-separated list. For QNames and NOTATIONS, or lists
     * containing them, it fails.
     */

    public String getStringValue() {
        return (node==null ? "" : node.getStringValue());
    }


    /**
    * Convert to Java object (for passing to external functions)
    */

    public Object convertToJava(Class target, XPathContext context) throws XPathException {
        if (node == null) {
            return null;
        }
        if (target.isAssignableFrom(node.getClass())) {
            return node;
        }
        if (target == String.class) {
            return node.getStringValue();
        }
        return super.convertToJava(target, context);
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

