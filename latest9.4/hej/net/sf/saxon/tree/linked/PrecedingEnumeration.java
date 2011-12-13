package net.sf.saxon.tree.linked;

import net.sf.saxon.pattern.NodeTest;
import net.sf.saxon.tree.iter.AxisIterator;

final class PrecedingEnumeration extends TreeEnumeration {

    /*@Nullable*/ NodeImpl nextAncestor;
    
    public PrecedingEnumeration(/*@NotNull*/ NodeImpl node, NodeTest nodeTest) {
        super(node, nodeTest);

        // we need to avoid returning ancestors of the starting node
        nextAncestor = (NodeImpl)node.getParent();
        advance();   
    }


    /**
    * Special code to skip the ancestors of the start node
    */

    protected boolean conforms(/*@Nullable*/ NodeImpl node) {
        // ASSERT: we'll never test the root node, because it's always
        // an ancestor, so nextAncestor will never be null.
        if (node!=null) {
            if (node.isSameNodeInfo(nextAncestor)) {
                nextAncestor = (NodeImpl)nextAncestor.getParent();
                return false;
            }
        }
        return super.conforms(node);
    }

    protected void step() {
        next = next.getPreviousInDocument();
    }

    /**
    * Get another enumeration of the same nodes
    */
    
    /*@NotNull*/ public AxisIterator getAnother() {
        return new PrecedingEnumeration(start, nodeTest);
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