package net.sf.saxon.tree.tiny;

import net.sf.saxon.om.NodeInfo;
import net.sf.saxon.pattern.NodeTest;
import net.sf.saxon.tree.iter.AxisIterator;
import net.sf.saxon.tree.iter.AxisIteratorImpl;

/**
* This class supports the preceding-sibling axis.
* The starting node must be an element, text node, comment, or processing instruction:
* to ensure this, construct the enumeration using NodeInfo#getEnumeration()
*/

final class PrecedingSiblingEnumeration extends AxisIteratorImpl {

    private TinyTree document;
    private TinyNodeImpl startNode;
    private int nextNodeNr;
    private NodeTest test;
    private TinyNodeImpl parentNode;

    PrecedingSiblingEnumeration(TinyTree doc, /*@NotNull*/ TinyNodeImpl node, NodeTest nodeTest) {
        document = doc;
        document.ensurePriorIndex();
        test = nodeTest;
        startNode = node;
        nextNodeNr = node.nodeNr;
        parentNode = node.parent;   // doesn't matter if this is null (unknown)
    }

    /*@Nullable*/ public NodeInfo next() {
        if (nextNodeNr < 0) {
            // This check is needed because an errant caller can call next() again after hitting the end of sequence
            return null;
        }
        while (true) {
            nextNodeNr = document.prior[nextNodeNr];
            if (nextNodeNr < 0) {
                current = null;
                position = -1;
                return null;
            }
            if (test.matches(document, nextNodeNr)) {
                position++;
                TinyNodeImpl next = document.getNode(nextNodeNr);
                next.setParentNode(parentNode);
                return (current = next);
            }
        }
    }

    /**
    * Get another enumeration of the same nodes
    */

    /*@NotNull*/ public AxisIterator getAnother() {
        return new PrecedingSiblingEnumeration(document, startNode, test);
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