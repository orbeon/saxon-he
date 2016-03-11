////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
// Copyright (c) 2015 Saxonica Limited.
// This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
// If a copy of the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
// This Source Code Form is "Incompatible With Secondary Licenses", as defined by the Mozilla Public License, v. 2.0.
////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

package net.sf.saxon.tree.tiny;

import net.sf.saxon.om.NodeInfo;
import net.sf.saxon.pattern.NodeTest;
import net.sf.saxon.tree.iter.AxisIterator;
import net.sf.saxon.tree.iter.AxisIteratorImpl;

/**
 * This class enumerates the ancestor:: or ancestor-or-self:: axes,
 * starting at a given node. The start node will never be the root.
 */

final class AncestorIterator extends AxisIteratorImpl {

    private TinyNodeImpl startNode;
    private NodeInfo current;
    private NodeTest test;

    public AncestorIterator(TinyNodeImpl node, NodeTest nodeTest) {
        test = nodeTest;
        startNode = node;
        current = startNode;
    }

    /*@Nullable*/
    public NodeInfo next() {
        if (current == null) {
            return null;
        }
        NodeInfo node = current.getParent();
        while (node != null && !test.matchesNode(node)) {
            node = node.getParent();
        }
        return current = node;
    }

    /**
     * Get another enumeration of the same nodes
     */

    /*@NotNull*/
    public AxisIterator getAnother() {
        return new AncestorIterator(startNode, test);
    }

}

