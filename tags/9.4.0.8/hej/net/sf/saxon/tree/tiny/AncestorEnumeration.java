package net.sf.saxon.tree.tiny;

import net.sf.saxon.om.NodeInfo;
import net.sf.saxon.pattern.NodeTest;
import net.sf.saxon.tree.iter.AxisIterator;
import net.sf.saxon.tree.iter.AxisIteratorImpl;

/**
* This class enumerates the ancestor:: or ancestor-or-self:: axes,
* starting at a given node. The start node will never be the root.
*/

final class AncestorEnumeration extends AxisIteratorImpl {

    private TinyNodeImpl startNode;
    private NodeTest test;
    private boolean includeSelf;

    public AncestorEnumeration(TinyNodeImpl node, NodeTest nodeTest, boolean includeSelf) {
        test = nodeTest;
        startNode = node;
        this.includeSelf = includeSelf;
        current = startNode;
    }

    /*@Nullable*/ public NodeInfo next() {
        if (position <= 0) {
            if (position < 0) {
                return null;
            }
            if (position==0 && includeSelf && test.matches(startNode)) {
                current = startNode;
                position = 1;
                return current;
            }
        }
        assert current != null;
        NodeInfo node = current.getParent();
        while (node != null && !test.matches(node)) {
            node = node.getParent();
        }
        current = node;
        if (node == null) {
            position = -1;
        } else {
            position++;
        }
        return current;
    }

    /**
    * Get another enumeration of the same nodes
    */

    /*@NotNull*/ public AxisIterator getAnother() {
        return new AncestorEnumeration(startNode, test, includeSelf);
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