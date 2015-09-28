////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
// Copyright (c) 2014 Saxonica Limited.
// This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
// If a copy of the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
// This Source Code Form is "Incompatible With Secondary Licenses", as defined by the Mozilla Public License, v. 2.0.
////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

package net.sf.saxon.functions;

import net.sf.saxon.expr.Expression;
import net.sf.saxon.expr.StaticProperty;
import net.sf.saxon.expr.XPathContext;
import net.sf.saxon.expr.parser.ContextItemStaticInfo;
import net.sf.saxon.expr.parser.ExpressionVisitor;
import net.sf.saxon.expr.sort.DocumentOrderIterator;
import net.sf.saxon.expr.sort.GlobalOrderComparer;
import net.sf.saxon.om.NodeInfo;
import net.sf.saxon.om.Sequence;
import net.sf.saxon.om.SequenceIterator;
import net.sf.saxon.om.SequenceTool;
import net.sf.saxon.trans.XPathException;
import net.sf.saxon.tree.util.Navigator;

/**
 * This class implements the function fn:has-children(), which is a standard function in XPath 3.0
 */

public class Innermost extends SystemFunctionCall {

    boolean presorted = false;

    @Override
    public Expression optimize(ExpressionVisitor visitor, ContextItemStaticInfo contextItemType) throws XPathException {
        if ((argument[0].getSpecialProperties() & StaticProperty.PEER_NODESET) != 0) {
            return argument[0];
        }
        presorted = ((argument[0].getSpecialProperties() & StaticProperty.ORDERED_NODESET) != 0);
        return this;
    }

    @Override
    public Expression copy() {
        Innermost o = (Innermost) super.copy();
        o.presorted = presorted;
        return o;
    }

    public SequenceIterator iterate(XPathContext context) throws XPathException {
        return innermost(argument[0].iterate(context));
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
        return SequenceTool.toLazySequence(innermost(arguments[0].iterate()));
    }

    public SequenceIterator innermost(SequenceIterator in) throws XPathException {
        if (!presorted) {
            in = new DocumentOrderIterator(in, GlobalOrderComparer.getInstance());
        }
        return new InnermostIterator(in);
    }

    /**
     * Inner class implementing the logic in the form of an iterator.
     * <p/>
     * The principle behind the algorithm is the assertion that in a sorted input
     * sequence, if a node A is immediately followed by a descendant B, then we can
     * discard A; if it is immediately followed by a node B that is not a descendant,
     * then we can emit A. So there is a single-node lookahead. The last node in the input
     * sequence is always emitted. (Note, "descendant" is used loosely here to
     * include attributes and namespaces.)
     */

    private class InnermostIterator implements SequenceIterator {

        SequenceIterator in;
        NodeInfo pending = null;
        int position = 0;

        /**
         * Create an iterator which filters an input sequence to select only those nodes
         * that have no ancestor in the sequence
         *
         * @param in the input sequence, which must be a sequence of nodes in document order with no duplicates
         * @throws XPathException if an error occurs evaluating the input iterator
         */

        public InnermostIterator(SequenceIterator in) throws XPathException {
            this.in = in;
            pending = (NodeInfo)in.next();
        }

        public NodeInfo next() throws XPathException {
            if (pending == null) {
                // we're done
                position = -1;
                return null;
            } else {
                while (true) {
                    NodeInfo next = (NodeInfo)in.next();
                    if (next == null) {
                        NodeInfo current = pending;
                        position++;
                        pending = null;
                        return current;
                    }
                    if (Navigator.isAncestorOrSelf(pending, next)) {
                        // discard the pending node
                        pending = next;
                    } else {
                        // emit the pending node
                        position++;
                        NodeInfo current = pending;
                        pending = next;
                        return current;
                    }
                }
            }
        }

        public void close() {
            in.close();
        }

        public SequenceIterator getAnother() throws XPathException {
            return new InnermostIterator(in.getAnother());
        }

        public int getProperties() {
            return 0;
        }
    }
}

// Copyright (c) 2012 Saxonica Limited. All rights reserved.