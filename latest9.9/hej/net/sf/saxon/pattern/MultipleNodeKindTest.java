////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
// Copyright (c) 2013 Saxonica Limited.
// This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
// If a copy of the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
// This Source Code Form is "Incompatible With Secondary Licenses", as defined by the Mozilla Public License, v. 2.0.
////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

package net.sf.saxon.pattern;

import net.sf.saxon.om.NodeInfo;
import net.sf.saxon.om.NodeName;
import net.sf.saxon.tree.tiny.NodeVectorTree;
import net.sf.saxon.tree.util.FastStringBuffer;
import net.sf.saxon.type.*;

import java.util.LinkedList;
import java.util.function.Function;
import java.util.function.IntPredicate;

/**
 * An MultipleNodeKindTest is a nodetest that matches nodes belonging to any subset of possible
 * node kinds, for example element and document nodes, or attribute and namespace nodes
 */

public final class MultipleNodeKindTest extends NodeTest {

    public final static MultipleNodeKindTest PARENT_NODE =
            new MultipleNodeKindTest(UType.DOCUMENT.union(UType.ELEMENT));

    public final static MultipleNodeKindTest DOC_ELEM_ATTR =
        new MultipleNodeKindTest(UType.DOCUMENT.union(UType.ELEMENT).union(UType.ATTRIBUTE));

    public final static MultipleNodeKindTest LEAF =
            new MultipleNodeKindTest(UType.TEXT.union(UType.COMMENT).union(UType.PI).union(UType.NAMESPACE).union(UType.ATTRIBUTE));


    UType uType;
    int nodeKindMask;

    public MultipleNodeKindTest(UType u) {
        this.uType = u;
        if (UType.DOCUMENT.overlaps(u)) {
            nodeKindMask |= 1 << Type.DOCUMENT;
        }
        if (UType.ELEMENT.overlaps(u)) {
            nodeKindMask |= 1 << Type.ELEMENT;
        }
        if (UType.ATTRIBUTE.overlaps(u)) {
            nodeKindMask |= 1 << Type.ATTRIBUTE;
        }
        if (UType.TEXT.overlaps(u)) {
            nodeKindMask |= 1 << Type.TEXT;
        }
        if (UType.COMMENT.overlaps(u)) {
            nodeKindMask |= 1 << Type.COMMENT;
        }
        if (UType.PI.overlaps(u)) {
            nodeKindMask |= 1 << Type.PROCESSING_INSTRUCTION;
        }
        if (UType.NAMESPACE.overlaps(u)) {
            nodeKindMask |= 1 << Type.NAMESPACE;
        }
    }

    /**
     * Get the corresponding {@link net.sf.saxon.type.UType}. A UType is a union of primitive item
     * types.
     *
     * @return the smallest UType that subsumes this item type
     */
    public UType getUType() {
        return uType;
    }

    /**
     * Test whether this node test is satisfied by a given node. This method is only
     * fully supported for a subset of NodeTests, because it doesn't provide all the information
     * needed to evaluate all node tests. In particular (a) it can't be used to evaluate a node
     * test of the form element(N,T) or schema-element(E) where it is necessary to know whether the
     * node is nilled, and (b) it can't be used to evaluate a node test of the form
     * document-node(element(X)). This in practice means that it is used (a) to evaluate the
     * simple node tests found in the XPath 1.0 subset used in XML Schema, and (b) to evaluate
     * node tests where the node kind is known to be an attribute.
     *
     * @param nodeKind   The kind of node to be matched
     * @param name       identifies the expanded name of the node to be matched.
     *                   The value should be null for a node with no name.
     * @param annotation The actual content type of the node
     */
    @Override
    public boolean matches(int nodeKind, NodeName name, SchemaType annotation) {
        return (nodeKindMask & (1<<nodeKind)) != 0;
    }

    public IntPredicate getMatcher(final NodeVectorTree tree) {
        final byte[] nodeKindArray = tree.getNodeKindArray();
        return nodeNr -> {
            int nodeKind = nodeKindArray[nodeNr] & 0x0f;
            if (nodeKind == Type.WHITESPACE_TEXT) {
                nodeKind = Type.TEXT;
            }
            return (nodeKindMask & (1 << nodeKind)) != 0;
        };
    }

    /**
     * Test whether this node test is satisfied by a given node. This alternative
     * method is used in the case of nodes where calculating the fingerprint is expensive,
     * for example DOM or JDOM nodes.
     *
     * @param node the node to be matched
     */

    public boolean matchesNode(NodeInfo node) {
        int nodeKind = node.getNodeKind();
        return (nodeKindMask & (1<<nodeKind)) != 0;
    }


    /**
     * Determine the default priority to use if this pattern appears as a match pattern
     * for a template with no explicit priority attribute.
     */

    public double getDefaultPriority() {
        return -0.5;
    }

    /*@NotNull*/
    public String toString() {
        FastStringBuffer fsb = new FastStringBuffer(FastStringBuffer.C64);
        LinkedList<PrimitiveUType> types = new LinkedList<>(uType.decompose());
        format(types, fsb, ItemType::toString);
        return fsb.toString();
    }

    public String toExportString() {
        FastStringBuffer fsb = new FastStringBuffer(FastStringBuffer.C64);
        LinkedList<PrimitiveUType> types = new LinkedList<>(uType.decompose());
        format(types, fsb, ItemType::toExportString);
        return fsb.toString();
    }

    private void format(LinkedList<PrimitiveUType> list, FastStringBuffer fsb, Function<ItemType, String> show) {
        if (list.size() == 1) {
            fsb.append(list.get(0).toItemType().toString());
        } else {
            fsb.append('(');
            fsb.append(list.removeFirst().toItemType().toString());
            fsb.append('|');
            format(list, fsb, show);
            fsb.append(')');
        }
    }

    /**
     * Returns a hash code value for the object.
     */

    public int hashCode() {
        return uType.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        return obj instanceof MultipleNodeKindTest && uType.equals(((MultipleNodeKindTest)obj).uType);
    }

    /**
     * Generate Javascript code to test whether an item conforms to this item type
     *
     * @return a Javascript instruction or sequence of instructions, which can be used as the body
     * of a Javascript function, and which returns a boolean indication whether the value of the
     * variable "item" is an instance of this item type.
     * @param knownToBe An item type that the supplied item is known to conform to; the generated code
     *                  can assume that the item is an instance of this type.
     * @param targetVersion The version of Saxon-JS for which code is being generated. Currently either 1 or 2.

     */
    @Override
    public String generateJavaScriptItemTypeTest(ItemType knownToBe, int targetVersion) {
        FastStringBuffer fsb = new FastStringBuffer(256);
        fsb.append("return SaxonJS.U.isNode(item) && (");
        String or = "";
        UType u = uType;
        if (UType.DOCUMENT.overlaps(u)) {
            fsb.append(or + "item.nodeType===9||item.nodeType===11");
            or = "||";
        }
        if (UType.ELEMENT.overlaps(u)) {
            fsb.append(or + "item.nodeType===1");
            or = "||";
        }
        if (UType.ATTRIBUTE.overlaps(u)) {
            fsb.append(or + "item.nodeType===2");
            or = "||";
        }
        if (UType.TEXT.overlaps(u)) {
            fsb.append(or + "item.nodeType===3");
            or = "||";
        }
        if (UType.COMMENT.overlaps(u)) {
            fsb.append(or + "item.nodeType===8");
            or = "||";
        }
        if (UType.PI.overlaps(u)) {
            fsb.append(or + "item.nodeType===7");
            or = "||";
        }
        if (UType.NAMESPACE.overlaps(u)) {
            fsb.append(or + "item.nodeType===13");
            or = "||";
        }
        fsb.append(");");
        return fsb.toString().replace("false||", "");
    }


}

