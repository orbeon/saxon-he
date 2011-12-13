package net.sf.saxon.tree.linked;

import net.sf.saxon.pattern.NodeTest;
import net.sf.saxon.tree.iter.AxisIterator;

final class DescendantEnumeration extends TreeEnumeration {

    private NodeImpl root;
    private boolean includeSelf;

    public DescendantEnumeration(NodeImpl node, NodeTest nodeTest, boolean includeSelf) {
        super(node, nodeTest);
        root = node;
        this.includeSelf = includeSelf;
        if (!includeSelf || !conforms(node)) {
            advance();
        }
    }

    protected void step() {
        next = next.getNextInDocument(root);
    }

    /**
    * Get another enumeration of the same nodes
    */

    /*@NotNull*/ public AxisIterator getAnother() {
        return new DescendantEnumeration(start, nodeTest, includeSelf);
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