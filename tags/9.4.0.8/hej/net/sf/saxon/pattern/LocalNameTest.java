package net.sf.saxon.pattern;

import net.sf.saxon.om.NamePool;
import net.sf.saxon.om.NodeInfo;
import net.sf.saxon.om.NodeName;
import net.sf.saxon.om.StructuredQName;
import net.sf.saxon.tree.tiny.TinyTree;

/**
  * NodeTest is an interface that enables a test of whether a node has a particular
  * name and type. A LocalNameTest matches the node type and the local name,
  * it represents an XPath 2.0 test of the form *:name.
  *
  * @author Michael H. Kay
  */

public final class LocalNameTest extends NodeTest implements QNameTest {

	private NamePool namePool;
	private int nodeKind;
	private String localName;

	public LocalNameTest(NamePool pool, int nodeKind, String localName) {
	    namePool = pool;
		this.nodeKind = nodeKind;
		this.localName = localName;
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
    public boolean matches(int nodeKind, /*@Nullable*/ NodeName name, int annotation) {
        return name != null && nodeKind == this.nodeKind && localName.equals(name.getLocalPart());
    }

    /**
     * Test whether this node test is satisfied by a given node on a TinyTree. The node
     * must be a document, element, text, comment, or processing instruction node.
     * This method is provided so that when navigating a TinyTree a node can be rejected without
     * actually instantiating a NodeInfo object.
     *
     * @param tree   the TinyTree containing the node
     * @param nodeNr the number of the node within the TinyTree
     * @return true if the node matches the NodeTest, otherwise false
     */

    public boolean matches(TinyTree tree, int nodeNr) {
        int nameCode = tree.getNameCode(nodeNr);
        return nameCode != -1 &&
                tree.getNodeKind(nodeNr) == nodeKind &&
                localName.equals(namePool.getLocalName(nameCode & NamePool.FP_MASK));
    }

    /**
     * Test whether this node test is satisfied by a given node. This alternative
     * method is used in the case of nodes where calculating the fingerprint is expensive,
     * for example DOM or JDOM nodes.
     * @param node the node to be matched
     */

    public boolean matches(NodeInfo node) {
        return localName.equals(node.getLocalPart()) && nodeKind == node.getNodeKind();
    }

    /**
     * Test whether this QNameTest matches a given QName
     * @param qname the QName to be matched
     * @return true if the name matches, false if not
     */

    public boolean matches(StructuredQName qname) {
        return localName.equals(qname.getLocalPart());
    }

    /**
    * Determine the default priority of this node test when used on its own as a Pattern
    */

    public final double getDefaultPriority() {
    	return -0.25;
    }

    /**
     * Get the local name used in this LocalNameTest
     */

    public String getLocalName() {
        return localName;
    }

    /**
    * Determine the types of nodes to which this pattern applies. Used for optimisation.
    * For patterns that match nodes of several types, return Type.NODE
    * @return the type of node matched by this pattern. e.g. Type.ELEMENT or Type.TEXT
    */

    public int getPrimitiveType() {
        return nodeKind;
    }

    /**
     * Get a mask indicating which kinds of nodes this NodeTest can match. This is a combination
     * of bits: 1<<Type.ELEMENT for element nodes, 1<<Type.TEXT for text nodes, and so on.
     */

    public int getNodeKindMask() {
        return 1<<nodeKind;
    }

    public String toString() {
        return "*:" + localName;
    }

    /**
      * Returns a hash code value for the object.
      */

     public int hashCode() {
         return nodeKind<<20 ^ localName.hashCode();
     }

    /**
     * Indicates whether some other object is "equal to" this one.
     */
    public boolean equals(Object other) {
        return other instanceof LocalNameTest &&
                ((LocalNameTest)other).namePool == namePool &&
                ((LocalNameTest)other).nodeKind == nodeKind &&
                ((LocalNameTest)other).localName.equals(localName);
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