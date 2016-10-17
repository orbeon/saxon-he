////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
// Copyright (c) 2015 Saxonica Limited.
// This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
// If a copy of the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
// This Source Code Form is "Incompatible With Secondary Licenses", as defined by the Mozilla Public License, v. 2.0.
////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

package net.sf.saxon.pattern;

import com.saxonica.ee.stream.ManualGroupIterator;
import net.sf.saxon.expr.XPathContext;
import net.sf.saxon.expr.parser.ContextItemStaticInfo;
import net.sf.saxon.expr.parser.ExpressionVisitor;
import net.sf.saxon.expr.parser.RebindingMap;
import net.sf.saxon.om.Item;
import net.sf.saxon.om.NodeInfo;
import net.sf.saxon.trace.ExpressionPresenter;
import net.sf.saxon.trans.XPathException;
import net.sf.saxon.type.ItemType;
import net.sf.saxon.type.UType;

/**
 * This is a special pattern that matches the first item in the current group, that is,
 * current-group()[1] or simply "." when used in an appropriate context.
 */
public class FirstInGroupPattern extends Pattern {

    private static FirstInGroupPattern THE_INSTANCE = new FirstInGroupPattern();

    public static FirstInGroupPattern getInstance() {
        return THE_INSTANCE;
    }

    protected FirstInGroupPattern(){};

    /**
     * Get a UType indicating which kinds of items this Pattern can match.
     *
     * @return a UType indicating all the primitive types of item that the pattern can match.
     */
    @Override
    public UType getUType() {
        return UType.ANY_NODE;
    }

    /**
     * Type-check the pattern.
     *
     * @param visitor         the expression visitor
     * @param contextItemType the type of the context item at the point where the pattern
     *                        is defined. Set to null if it is known that the context item is undefined.
     * @return the optimised Pattern
     */

    public Pattern typeCheck(ExpressionVisitor visitor, ContextItemStaticInfo contextItemType) throws XPathException {
        return this;
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
        return (anchor == null || node == anchor) && node == ((ManualGroupIterator)context.getCurrentGroupIterator()).current();
    }

    /**
     * Determine whether this Pattern matches the given Node. This is the main external interface
     * for matching patterns: it sets current() to the node being tested
     *
     * @param item    The NodeInfo representing the Element or other node to be tested against the Pattern
     * @param context The dynamic context. Only relevant if the pattern
     *                uses variables, or contains calls on functions such as document() or key().
     * @return true if the node matches the Pattern, false otherwise
     */

    public boolean matches(Item item, XPathContext context) throws XPathException {
        return item == ((ManualGroupIterator) context.getCurrentGroupIterator()).current();
    }

    /**
     * Get a NodeTest that all the nodes matching this pattern must satisfy
     *
     * @return a NodeTest, as specific as possible, which all the matching nodes satisfy
     */

    public ItemType getItemType() {
        return AnyNodeTest.getInstance();
    }

    /*@NotNull*/
    public String toString() {
        return ".";
    }

    @Override
    public void export(ExpressionPresenter presenter) throws XPathException {
        presenter.startElement("p.anchor");
        presenter.endElement();
    }

    /**
     * Copy an AnchorPattern.
     * Since there is only one, return the same.
     *
     * @return the original nodeTest
     * @param rebindings
     */

    /*@NotNull*/
    public Pattern copy(RebindingMap rebindings) {
        return this;
    }
}

