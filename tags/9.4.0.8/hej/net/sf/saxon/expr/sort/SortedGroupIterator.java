package net.sf.saxon.expr.sort;

import net.sf.saxon.Configuration;
import net.sf.saxon.expr.LastPositionFinder;
import net.sf.saxon.expr.XPathContext;
import net.sf.saxon.expr.XPathContextMajor;
import net.sf.saxon.om.Item;
import net.sf.saxon.om.SequenceIterator;
import net.sf.saxon.trace.InstructionInfo;
import net.sf.saxon.trans.XPathException;
import net.sf.saxon.value.Value;

/**
 * A SortedGroupIterator is a modified SortedIterator. It sorts a sequence of groups,
 * and is itself a GroupIterator. The modifications retain extra information about
 * the items being sorted. The items are each the leading item of a group, and as well
 * as the item itself, the iterator preserves information about the group: specifically,
 * an iterator over the items in the group, and the value of the grouping key (if any).
 */

public class SortedGroupIterator extends SortedIterator implements GroupIterator {

    public SortedGroupIterator(XPathContext context,
                               GroupIterator base,
                               SortKeyEvaluator sortKeyEvaluator,
                               AtomicComparer[] comparators
    ) {
        super(context, base, sortKeyEvaluator, comparators, true);
        setHostLanguage(Configuration.XSLT);
    }

    /**
     * Override the method that builds the array of values and sort keys.
     * @throws XPathException
     */

    protected void buildArray() throws XPathException {
        int allocated;
        if ((base.getProperties() & SequenceIterator.LAST_POSITION_FINDER) != 0) {
            allocated = ((LastPositionFinder)base).getLength();
        } else {
            allocated = 100;
        }

        values = new GroupToBeSorted[allocated];
        count = 0;

        XPathContextMajor c2 = context.newContext();
        c2.setCurrentIterator(base);
        c2.setCurrentGroupIterator((GroupIterator)base);
        if (sortKeyEvaluator instanceof InstructionInfo) {
            c2.setOrigin((InstructionInfo)sortKeyEvaluator);
        }
        // this provides the context for evaluating the sort key

        // initialise the array with data

        while (true) {
            Item item = base.next();
            if (item == null) {
                break;
            }
            if (count==allocated) {
                allocated *= 2;
                GroupToBeSorted[] nk2 = new GroupToBeSorted[allocated];
                System.arraycopy((GroupToBeSorted[])values, 0, nk2, 0, count);
                values = nk2;
            }
            GroupToBeSorted gtbs = new GroupToBeSorted(comparators.length);
            values[count] = gtbs;
            gtbs.value = item;
            for (int n=0; n<comparators.length; n++) {
                gtbs.sortKeyValues[n] = sortKeyEvaluator.evaluateSortKey(n, c2);
            }
            gtbs.originalPosition = count++;
            gtbs.currentGroupingKey = ((GroupIterator)base).getCurrentGroupingKey();
            gtbs.currentGroupIterator = ((GroupIterator)base).iterateCurrentGroup();
        }
    }

    /*@Nullable*/ public Value getCurrentGroupingKey() {
        return ((GroupToBeSorted)values[position-1]).currentGroupingKey;
    }

    public SequenceIterator iterateCurrentGroup() throws XPathException {
        SequenceIterator iter = ((GroupToBeSorted)values[position-1]).currentGroupIterator;
        return iter.getAnother();
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