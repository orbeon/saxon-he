////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
// Copyright (c) 2015 Saxonica Limited.
// This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
// If a copy of the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
// This Source Code Form is "Incompatible With Secondary Licenses", as defined by the Mozilla Public License, v. 2.0.
////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

package net.sf.saxon.pattern;

import net.sf.saxon.expr.XPathContext;
import net.sf.saxon.expr.parser.ExpressionTool;
import net.sf.saxon.expr.parser.RebindingMap;
import net.sf.saxon.om.Item;
import net.sf.saxon.om.NodeInfo;
import net.sf.saxon.trans.XPathException;
import net.sf.saxon.type.ItemType;
import net.sf.saxon.type.UType;

import java.util.HashSet;
import java.util.Set;

/**
 * A pattern formed as the difference of two other patterns
 */

public class IntersectPattern extends VennPattern {

    /**
     * Constructor
     *
     * @param p1 the left-hand operand
     * @param p2 the right-hand operand
     */

    public IntersectPattern(Pattern p1, Pattern p2) {
        super(p1, p2);
    }

    /**
     * Get an ItemType that all the items matching this pattern must satisfy
     */
    @Override
    public ItemType getItemType() {
        return p1.getItemType();
    }

    /**
     * Get a UType indicating which kinds of items this Pattern can match.
     *
     * @return a UType indicating all the primitive types of item that the pattern can match.
     */
    @Override
    public UType getUType() {
        return p1.getUType().intersection(p2.getUType());
    }

    /**
     * Determine if the supplied node matches the pattern
     *
     * @param item the node to be compared
     * @return true if the node matches either of the operand patterns
     */

    public boolean matches(Item item, XPathContext context) throws XPathException {
        return p1.matches(item, context) && p2.matches(item, context);
    }

    /**
     * Determine whether this pattern matches a given Node within the subtree rooted at a given
     * anchor node. This method is used when the pattern is used for streaming.
     *
     * @param node    The NodeInfo representing the Element or other node to be tested against the Pattern
     * @param anchor  The anchor node, which must match any AnchorPattern subpattern
     * @param context The dynamic context. Only relevant if the pattern
     *                uses variables, or contains calls on functions such as document() or key().
     * @return true if the node matches the Pattern, false otherwise
     */

    public boolean matchesBeneathAnchor(NodeInfo node, NodeInfo anchor, XPathContext context) throws XPathException {
        return p1.matchesBeneathAnchor(node, anchor, context) &&
                p2.matchesBeneathAnchor(node, anchor, context);
    }

    /**
     * Convert the pattern to a typed pattern, in which an element name is treated as
     * schema-element(N)
     *
     * @param val either "strict" or "lax" depending on the value of xsl:mode/@typed
     * @return either the original pattern unchanged, or a new pattern as the result of the
     *         conversion
     * @throws net.sf.saxon.trans.XPathException
     *          if the pattern cannot be converted
     */
    @Override
    public Pattern convertToTypedPattern(String val) throws XPathException {
        Pattern np1 = p1.convertToTypedPattern(val);
        Pattern np2 = p2.convertToTypedPattern(val);
        if (p1 == np1 && p2 == np2) {
            return this;
        } else {
            return new IntersectPattern(np1, np2);
        }
    }


    /**
     * Determine whether this pattern is the same as another pattern
     *
     * @param other the other object
     */

    public boolean equals(/*@NotNull*/ Object other) {
        if (other instanceof IntersectPattern) {
            Set s0 = new HashSet(10);
            gatherComponentPatterns(s0);
            Set s1 = new HashSet(10);
            ((IntersectPattern) other).gatherComponentPatterns(s1);
            return s0.equals(s1);
        } else {
            return false;
        }
    }

    /**
     * Hashcode supporting equals()
     */

    public int hashCode() {
        return 0x13d7dfa6 ^ p1.hashCode() ^ p2.hashCode();
    }

    /**
     * Get the relevant operator: "union", "intersect", or "except"
     *
     * @return the operator, as a string
     */
    @Override
    protected String getOperatorName() {
        return "intersect";
    }

    /**
     * Copy a pattern. This makes a deep copy.
     *
     * @return the copy of the original pattern
     * @param rebindings
     */

    /*@NotNull*/
    public Pattern copy(RebindingMap rebindings) {
        IntersectPattern n = new IntersectPattern(p1.copy(rebindings), p2.copy(rebindings));
        ExpressionTool.copyLocationInfo(this, n);
        return n;
    }

}

