////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
// Copyright (c) 2018 Saxonica Limited.
// This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
// If a copy of the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
// This Source Code Form is "Incompatible With Secondary Licenses", as defined by the Mozilla Public License, v. 2.0.
////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

package net.sf.saxon.tree.tiny;

import net.sf.saxon.om.AxisInfo;
import net.sf.saxon.om.NodeInfo;
import net.sf.saxon.pattern.NodeTest;
import net.sf.saxon.tree.iter.AxisIterator;
import net.sf.saxon.type.Type;
import net.sf.saxon.type.UType;

import java.util.function.IntPredicate;

/**
 * This class supports both the descendant:: and descendant-or-self:: axes, which are
 * identical except for the route to the first candidate node.
 * It enumerates descendants of the specified node.
 * The calling code must ensure that the start node is not an attribute or namespace node.
 */

final class GraftingDescendantIterator implements AxisIterator {

    private final TinyTree tree;
    private final TinyNodeImpl startNode;
    private int nextNodeNr;
    private final int startDepth;
    private final NodeTest test;
    private final IntPredicate matcher;
    private AxisIterator nestedIterator;
    private NodeInfo pending = null;
    private boolean includeTextNodes;

    /**
     * Create an iterator over the descendant axis
     *
     * @param doc         the containing TinyTree
     * @param node        the node whose descendants are required
     * @param nodeTest    test to be satisfied by each returned node
     */

    GraftingDescendantIterator(/*@NotNull*/ TinyTree doc, /*@NotNull*/ TinyNodeImpl node, NodeTest nodeTest) {
        tree = doc;
        startNode = node;
        test = nodeTest;
        nextNodeNr = node.nodeNr;
        startDepth = doc.depth[nextNodeNr];
        matcher = nodeTest.getMatcher(doc);
        includeTextNodes = nodeTest.getUType().overlaps(UType.TEXT);
    }

    /*@Nullable*/
    public NodeInfo next() {
        while (true) {
            if (pending != null) {
                NodeInfo p = pending;
                pending = null;
                return p;
            }
            if (nestedIterator != null) {
                NodeInfo nested = nestedIterator.next();
                if (nested != null) {
                    return nested;
                } else {
                    nestedIterator = null;
                }
            }
            nextNodeNr++;
            try {
                if (tree.depth[nextNodeNr] <= startDepth) {
                    nextNodeNr = -1;
                    return null;
                }
                if (tree.nodeKind[nextNodeNr] == Type.EXTERNAL_NODE_REFERENCE) {
                    byte axis = nextNodeNr == startNode.nodeNr ? AxisInfo.DESCENDANT : AxisInfo.DESCENDANT_OR_SELF;
                    nestedIterator = tree.externalNodes.get(tree.alpha[nextNodeNr])
                            .iterateAxis(axis, test);
                    continue;
                }
                if (includeTextNodes && tree.nodeKind[nextNodeNr] == Type.TEXTUAL_ELEMENT) {
                    pending = ((TinyTextualElement) tree.getNode(nextNodeNr)).getTextNode();
                }
            } catch (ArrayIndexOutOfBoundsException e) {
                // this shouldn't happen. If it does happen, it means the tree wasn't properly closed
                // during construction (there is no stopper node at the end). In this case, we'll recover
                // by returning end-of sequence
                //System.err.println("********* no stopper node **********");
                nextNodeNr = -1;
                return null;
            }
            if (matcher.test(nextNodeNr)) {
                break;
            }
        }

        return tree.getNode(nextNodeNr);
    }

}

