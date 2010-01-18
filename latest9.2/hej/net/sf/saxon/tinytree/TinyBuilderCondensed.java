package net.sf.saxon.tinytree;

import net.sf.saxon.sort.IntHashMap;
import net.sf.saxon.trans.XPathException;

/**
 * Variant of the TinyBuilder to create a tiny tree in which multiple text nodes or attribute
 * nodes sharing the same string value economize on space by only holding the value once.
 */
public class TinyBuilderCondensed extends TinyBuilder {

    // Keep a map from the hashcode of the string (calculated within this class) to the list
    // of node numbers of text nodes having that hashcode

    public IntHashMap<int[]> textValues = new IntHashMap<int[]>(100);

    /**
     * Create a text node. Separate method so it can be overridden
     * @param chars the contents of the text node
     * @param len   the length of the text node
     */

//    protected int makeTextNode(CharSequence chars, int len) {
//        TinyTree tree = getTree();
//        int hash = chars.hashCode();
//        int[] nodes = textValues.get(hash);
//        if (nodes != null) {
//            int used = nodes[0];
//            for (int i=1; i<used; i++) {
//                int nodeNr = nodes[i];
//                if (nodeNr == 0) {
//                    break;
//                } else if (chars.equals(TinyTextImpl.getStringValue(tree, nodeNr))) {
//                    return tree.addTextNodeCopy(getCurrentDepth(), nodeNr);
//                }
//            }
//        } else {
//            nodes = new int[4];
//            nodes[0] = 1;
//            textValues.put(hash, nodes);
//        }
//        if (nodes[0] + 1 > nodes.length) {
//            int[] n2 = new int[nodes.length*2];
//            System.arraycopy(nodes, 0, n2, 0, nodes[0]);
//            textValues.put(hash, n2);
//            nodes = n2;
//        }
//        int newNode = super.makeTextNode(chars, len);
//        nodes[nodes[0]++] = newNode;
//        return newNode;
//    }

    /**
     * For attribute nodes, the commoning-up of stored values is achieved simply by calling intern() on the
     * string value of the attribute.
     */

    public void attribute(int nameCode, int typeCode, CharSequence value, int locationId, int properties) throws XPathException {
        super.attribute(nameCode, typeCode, value.toString().intern(), locationId, properties);
    }
}

//
// The contents of this file are subject to the Mozilla Public License Version
// 1.0 (the "License");
// you may not use this file except in compliance with the License. You may
// obtain a copy of the
// License at http://www.mozilla.org/MPL/
//
// Software distributed under the License is distributed on an "AS IS" basis,
// WITHOUT WARRANTY OF ANY KIND, either express or implied.
// See the License for the specific language governing rights and limitations
// under the License.
//
// The Original Code is: all this file.
//
// The Initial Developer of the Original Code is Michael Kay,
//
// Portions created by (your name) are Copyright (C) (your legal entity). All
// Rights Reserved.
//
// Contributor(s): none.
//



