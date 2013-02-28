package net.sf.saxon.dom;

import org.w3c.dom.Node;

import java.util.List;

/**
* This class wraps a list of nodes as a DOM NodeList
*/

public final class DOMNodeList implements org.w3c.dom.NodeList {
    private List<Node> sequence;

    /**
     * Construct an node list that wraps a supplied list of DOM Nodes.
     * @param extent the list of nodes to be wrapped
     */

    public DOMNodeList(List<Node> extent) {
        sequence = extent;
    }

    /**
    * return the number of nodes in the list (DOM method)
    */

    public int getLength() {
        return sequence.size();
    }

    /**
    * Return the n'th item in the list (DOM method)
    * @throws java.lang.ClassCastException if the item is not a DOM Node
    */

    /*@Nullable*/ public Node item(int index) {
        if (index < 0 || index >= sequence.size()) {
            return null;
        } else {
            return sequence.get(index);
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
