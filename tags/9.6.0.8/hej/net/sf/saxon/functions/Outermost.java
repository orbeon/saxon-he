////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
// Copyright (c) 2014 Saxonica Limited.
// This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
// If a copy of the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
// This Source Code Form is "Incompatible With Secondary Licenses", as defined by the Mozilla Public License, v. 2.0.
////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

package net.sf.saxon.functions;

import com.saxonica.ee.stream.adjunct.OutermostAdjunct;
import com.saxonica.ee.stream.adjunct.StreamingAdjunct;
import net.sf.saxon.expr.Expression;
import net.sf.saxon.expr.StaticProperty;
import net.sf.saxon.expr.XPathContext;
import net.sf.saxon.expr.parser.ContextItemStaticInfo;
import net.sf.saxon.expr.parser.ExpressionVisitor;
import net.sf.saxon.expr.sort.DocumentOrderIterator;
import net.sf.saxon.expr.sort.GlobalOrderComparer;
import net.sf.saxon.om.*;
import net.sf.saxon.trans.XPathException;
import net.sf.saxon.tree.util.Navigator;
import net.sf.saxon.type.ItemType;

/**
 * This class implements the function fn:outermost(), which is a standard function in XPath 3.0
 */

public class Outermost extends SystemFunctionCall {

    boolean presorted = false;

    @Override
    public ItemType getItemType() {
        return getArguments()[0].getItemType();
    }

    @Override
    public Expression optimize(ExpressionVisitor visitor, ContextItemStaticInfo contextItemType) throws XPathException {
        Expression e2 = super.optimize(visitor, contextItemType);
        if (e2 != this) {
            return e2;
        }
        if ((argument[0].getSpecialProperties() & StaticProperty.PEER_NODESET) != 0) {
            return argument[0];
        }
        presorted = (argument[0].getSpecialProperties() & StaticProperty.ORDERED_NODESET) != 0;
        return this;
    }

    @Override
    public Expression copy() {
        Outermost o = (Outermost) super.copy();
        o.presorted = presorted;
        return o;
    }

    public SequenceIterator iterate(XPathContext context) throws XPathException {
        return outermost(argument[0].iterate(context));
    }

    @Override
    public int computeSpecialProperties() {
        return StaticProperty.ORDERED_NODESET | StaticProperty.PEER_NODESET;
    }

    /**
     * Evaluate the expression in a dynamic call
     *
     * @param context   the dynamic evaluation context
     * @param arguments the values of the arguments, supplied as Sequences
     * @return the result of the evaluation, in the form of a Sequence
     * @throws net.sf.saxon.trans.XPathException
     *          if a dynamic error occurs during the evaluation of the expression
     */
    public Sequence call(XPathContext context, Sequence[] arguments) throws XPathException {
        return SequenceTool.toLazySequence(outermost(arguments[0].iterate()));
    }

    public SequenceIterator outermost(SequenceIterator in) throws XPathException {
        if (!presorted) {
            in = new DocumentOrderIterator(in, GlobalOrderComparer.getInstance());
        }
        return new OutermostIterator(in);
    }

    /**
     * Inner class implementing the logic in the form of an iterator.
     * <p/>
     * The principle behind the algorithm is the assertion that in a sorted input
     * sequence, if a node has an ancestor within the sequence, then the most recently
     * selected node will be an ancestor: so we only need to test against the most
     * recently output node.
     */

    private class OutermostIterator implements SequenceIterator {

        SequenceIterator in;
        NodeInfo current = null;
        int position = 0;

        /**
         * Create an iterator which filters an input sequence to select only those nodes
         * that have no ancestor in the sequence
         *
         * @param in the input sequence, which must be  sequence of nodes in document order with no duplicates
         */

        public OutermostIterator(SequenceIterator in) {
            this.in = in;
        }

        public NodeInfo next() throws XPathException {
            while (true) {
                NodeInfo next = (NodeInfo)in.next();
                if (next == null) {
                    current = null;
                    position = -1;
                    return null;
                }
                if (current == null || !Navigator.isAncestorOrSelf(current, next)) {
                    current = next;
                    position++;
                    return current;
                }
            }
        }

        public void close() {
            in.close();
        }

        public SequenceIterator getAnother() throws XPathException {
            return new OutermostIterator(in.getAnother());
        }

        public int getProperties() {
            return 0;
        }
    }

//#ifdefined STREAM
    /**
     * Get a class that supports streamed evaluation of this expression
     *
     * @return the relevant StreamingAdjunct, or null if none is available
     */
    @Override
    protected StreamingAdjunct getStreamingAdjunct() {
        return new OutermostAdjunct();
    }
//#endif
}

// Copyright (c) 2012 Saxonica Limited. All rights reserved.