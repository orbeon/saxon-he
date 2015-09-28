////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
// Copyright (c) 2014 Saxonica Limited.
// This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
// If a copy of the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
// This Source Code Form is "Incompatible With Secondary Licenses", as defined by the Mozilla Public License, v. 2.0.
////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

package net.sf.saxon.pattern;

import net.sf.saxon.expr.XPathContext;
import net.sf.saxon.om.Item;
import net.sf.saxon.trans.XPathException;
import net.sf.saxon.type.SchemaDeclaration;
import net.sf.saxon.type.Type;

/**
 * A NodeTestPattern is a pattern that consists simply of a NodeTest
 *
 * @author Michael H. Kay
 */

public class NodeTestPattern extends Pattern {

    private NodeTest nodeTest;
    private int nodeKindMask;


    /**
     * Create an NodeTestPattern that matches all items of a given type
     *
     * @param test the type that the items must satisfy for the pattern to match
     */

    public NodeTestPattern(NodeTest test) {
        nodeTest = test;
        nodeKindMask = nodeTest.getNodeKindMask();
        setPriority(test.getDefaultPriority());
    }

    /**
     * Determine whether this Pattern matches the given Node. This is the main external interface
     * for matching patterns: it sets current() to the node being tested
     *
     * @param item    The NodeInfo representing the Element or other node to be tested against the Pattern
     * @param context The context in which the match is to take place. Only relevant if the pattern
     *                uses variables, or contains calls on functions such as document() or key(). Not used (and can be
     *                set to null) in the case of patterns that are NodeTests
     * @return true if the node matches the Pattern, false otherwise
     */

    public boolean matches(Item item, XPathContext context) {
        return nodeTest.matches(item, context);
    }

    /**
     * Get a NodeTest that all the nodes matching this pattern must satisfy
     */

    public NodeTest getItemType() {
        return nodeTest;
    }

    /**
     * Determine the types of nodes to which this pattern applies. Used for optimisation.
     * For patterns that match nodes of several types, return Type.NODE. For patterns that
     * do not match nodes, return -1.
     *
     * @return the type of node matched by this pattern. e.g. Type.ELEMENT or Type.TEXT
     */

    public int getNodeKind() {
        return nodeTest.getPrimitiveType();
    }

    /**
     * Get a mask indicating which kinds of nodes this NodeTest can match. This is a combination
     * of bits: 1<<Type.ELEMENT for element nodes, 1<<Type.TEXT for text nodes, and so on. The default
     * implementation indicates that nodes of all kinds are matched.
     *
     * @return a combination of bits: 1<<Type.ELEMENT for element nodes, 1<<Type.TEXT for text nodes, and so on
     */
    @Override
    public int getNodeKindMask() {
        return nodeKindMask;
    }


    /**
     * Determine the name fingerprint of nodes to which this pattern applies. Used for
     * optimisation.
     *
     * @return A fingerprint that the nodes must match, or -1 if it can match multiple fingerprints
     */

    public int getFingerprint() {
        return nodeTest.getFingerprint();
    }

    /**
     * Display the pattern for diagnostics
     */

    public String toString() {
        return nodeTest.toString();
    }

    /**
     * Determine whether this pattern is the same as another pattern
     *
     * @param other the other object
     */

    public boolean equals(/*@NotNull*/ Object other) {
        return (other instanceof NodeTestPattern) &&
                ((NodeTestPattern) other).nodeTest.equals(nodeTest);
    }

    /**
     * Hashcode supporting equals()
     */

    public int hashCode() {
        return 0x7aeffea8 ^ nodeTest.hashCode();
    }

    /**
     * Convert the pattern to a typed pattern, in which an element name is treated as
     * schema-element(N)
     *
     * @param val either "strict" or "lax" depending on the value of xsl:mode/@typed
     * @return either the original pattern unchanged, or a new pattern as the result of the
     *         conversion
     */
    @Override
    public Pattern convertToTypedPattern(String val) throws XPathException {
        if (nodeTest instanceof NameTest && ((NameTest) nodeTest).getNodeKind() == Type.ELEMENT) {
            SchemaDeclaration decl = getPackageData().getConfiguration().getElementDeclaration(nodeTest.getFingerprint());
            if (decl == null) {
                if ("lax".equals(val)) {
                    return this;
                } else {
                    // See spec bug 25517
                    throw new XPathException("The mode specifies typed='strict', " +
                            "but there is no schema element declaration named " + nodeTest.toString(), "XTSE3105");
                }
            } else {
                NodeTest schemaNodeTest = decl.makeSchemaNodeTest();
                return new NodeTestPattern(schemaNodeTest);
            }
        } else {
            return this;
        }
    }
}

