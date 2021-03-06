////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
// Copyright (c) 2018 Saxonica Limited.
// This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
// If a copy of the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
// This Source Code Form is "Incompatible With Secondary Licenses", as defined by the Mozilla Public License, v. 2.0.
////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

package net.sf.saxon.pattern;

import net.sf.saxon.om.NodeInfo;
import net.sf.saxon.om.NodeName;
import net.sf.saxon.om.StructuredQName;
import net.sf.saxon.tree.tiny.NodeVectorTree;
import net.sf.saxon.type.ItemType;
import net.sf.saxon.type.SchemaType;
import net.sf.saxon.type.Type;
import net.sf.saxon.type.UType;

import java.util.function.IntPredicate;

/**
 * NodeTest is an interface that enables a test of whether a node has a particular
 * name and type. An AnyNodeTest matches any node.
 *
 * @author Michael H. Kay
 */

public final class AnyNodeTest extends NodeTest implements QNameTest {

    private static AnyNodeTest THE_INSTANCE = new AnyNodeTest();

    /**
     * Get an instance of AnyNodeTest
     * @return the singleton instance of this class
     */

    public static AnyNodeTest getInstance() {
        return THE_INSTANCE;
    }

    /**
     * Private constructor
     */

    private AnyNodeTest() {
    }

    /**
     * Get the corresponding {@link net.sf.saxon.type.UType}. A UType is a union of primitive item
     * types.
     *
     * @return the smallest UType that subsumes this item type
     */
    public UType getUType() {
        return UType.ANY_NODE;
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
        return nodeKind != Type.PARENT_POINTER;
    }

    @Override
    public IntPredicate getMatcher(NodeVectorTree tree) {
        final byte[] nodeKindArray = tree.getNodeKindArray();
        return nodeNr -> nodeKindArray[nodeNr] != Type.PARENT_POINTER;
    }

    /**
     * Test whether this node test is satisfied by a given node. This alternative
     * method is used in the case of nodes where calculating the fingerprint is expensive,
     * for example DOM or JDOM nodes.
     *
     * @param node the node to be matched
     */

    public boolean matchesNode(NodeInfo node) {
        return true;
    }

    /**
     * Test whether this QNameTest matches a given QName
     *
     * @param qname the QName to be matched
     * @return true if the name matches, false if not
     */

    public boolean matches(StructuredQName qname) {
        return true;
    }

    /**
     * Determine the default priority of this node test when used on its own as a Pattern
     */

    public final double getDefaultPriority() {
        return -0.5;
    }

    /*@NotNull*/
    public String toString() {
        return "node()";
    }

    /**
     * Generate Javascript code to test if a name matches the test.
     *
     * @return JS code as a string. The generated code will be used
     * as the body of a JS function in which the argument name "q" is an
     * XdmQName object holding the name. The XdmQName object has properties
     * uri and local.
     * @param targetVersion The version of Saxon-JS being targeted
     */
    public String generateJavaScriptNameTest(int targetVersion) {
        return "true";
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
        return "return SaxonJS.U.isNode(item);";
    }


}

