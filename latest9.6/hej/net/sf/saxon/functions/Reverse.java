////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
// Copyright (c) 2014 Saxonica Limited.
// This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
// If a copy of the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
// This Source Code Form is "Incompatible With Secondary Licenses", as defined by the Mozilla Public License, v. 2.0.
////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

package net.sf.saxon.functions;

import com.saxonica.ee.stream.adjunct.ReverseAdjunct;
import com.saxonica.ee.stream.adjunct.StreamingAdjunct;
import net.sf.saxon.expr.Callable;
import net.sf.saxon.expr.Expression;
import net.sf.saxon.expr.StaticProperty;
import net.sf.saxon.expr.XPathContext;
import net.sf.saxon.om.Item;
import net.sf.saxon.om.Sequence;
import net.sf.saxon.om.SequenceIterator;
import net.sf.saxon.om.SequenceTool;
import net.sf.saxon.trans.XPathException;
import net.sf.saxon.tree.iter.ReversibleIterator;
import net.sf.saxon.type.AnyItemType;
import net.sf.saxon.type.ItemType;
import net.sf.saxon.value.SequenceExtent;

/**
 * Implement XPath function fn:reverse()
 */

public class Reverse extends SystemFunctionCall implements Callable {

    /**
     * Determine the item type of the value returned by the function
     * <p/>
     * <p/>
     * <p/>
     * /*@NotNull
     */
    public ItemType getItemType() {
        return argument[0].getItemType();    //AUTO
    }

    public int computeSpecialProperties() {
        int baseProps = argument[0].getSpecialProperties();
        if ((baseProps & StaticProperty.REVERSE_DOCUMENT_ORDER) != 0) {
            return (baseProps &
                    (~StaticProperty.REVERSE_DOCUMENT_ORDER)) |
                    StaticProperty.ORDERED_NODESET;
        } else if ((baseProps & StaticProperty.ORDERED_NODESET) != 0) {
            return (baseProps &
                    (~StaticProperty.ORDERED_NODESET)) |
                    StaticProperty.REVERSE_DOCUMENT_ORDER;
        } else {
            return baseProps;
        }
    }

    /**
     * Replace this expression by a simpler expression that delivers the results without regard
     * to order.
     *
     * @param retainAllNodes set to true if the result must contain exactly the same nodes as the
     *                       original; set to false if the result can eliminate (or introduce) duplicates.
     * @param forStreaming  set to true if optimizing for streaming
     */
    @Override
    public Expression unordered(boolean retainAllNodes, boolean forStreaming) throws XPathException {
        return argument[0].unordered(retainAllNodes, false);
    }

    /*@NotNull*/
    public SequenceIterator iterate(XPathContext context) throws XPathException {
        SequenceIterator forwards = argument[0].iterate(context);
        return getReverseIterator(forwards);
    }

    public static SequenceIterator getReverseIterator(SequenceIterator forwards) throws XPathException {
        if (forwards instanceof ReversibleIterator) {
            return ((ReversibleIterator) forwards).getReverseIterator();
        } else {
            SequenceExtent extent = new SequenceExtent(forwards);
            return extent.reverseIterate();
        }
    }

    public boolean effectiveBooleanValue(XPathContext context) throws XPathException {
        // EBV is independent of sequence order unless the sequence mixes atomic values and nodes
        // Note, calls to get the EBV of reverse() should normally have been rewritten at compile time
        ItemType type = argument[0].getItemType();
        if (type == AnyItemType.getInstance()) {
            return super.effectiveBooleanValue(context);
        } else {
            return argument[0].effectiveBooleanValue(context);
        }
    }

    public Sequence call(XPathContext context, Sequence[] arguments) throws XPathException {
        return SequenceTool.toLazySequence(getReverseIterator(arguments[0].iterate()));
    }

    //#ifdefined STREAM

    /**
     * Get a class that supports streamed evaluation of this expression
     *
     * @return the relevant StreamingAdjunct, or null if none is available
     */
    @Override
    protected StreamingAdjunct getStreamingAdjunct() {
        return new ReverseAdjunct();
    }

    //#endif
}

