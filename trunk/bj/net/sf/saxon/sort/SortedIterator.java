package net.sf.saxon.sort;
import net.sf.saxon.Configuration;
import net.sf.saxon.expr.LastPositionFinder;
import net.sf.saxon.expr.XPathContext;
import net.sf.saxon.om.Item;
import net.sf.saxon.om.SequenceIterator;
import net.sf.saxon.trace.Location;
import net.sf.saxon.trans.DynamicError;
import net.sf.saxon.trans.XPathException;

import java.util.Comparator;

/**
* Class to do a sorted iteration
*/

public class SortedIterator implements SequenceIterator, LastPositionFinder, Sortable {

    // the items to be sorted
    protected SequenceIterator base;

    // the sort key definitions
    protected SortKeyDefinition[] sortkeys;

    // the comparators corresponding to these sort keys
    protected Comparator[] comparators;

    // The items and keys are read into an array (nodeKeys) for sorting. This
    // array contains one "record" representing each node: the "record" contains
    // first, the Item itself, then an entry for each of its sort keys, in turn;
    // the last sort key is the position of the Item in the original sequence.
    protected int recordSize;
    protected Object[] nodeKeys;

    // The number of items to be sorted. -1 means not yet known.
    protected int count = -1;

    // The next item to be delivered from the sorted iteration
    protected int index = 0;

    // The context for the evaluation of sort keys
    protected XPathContext context;
    //private Comparator[] keyComparers;

    private int hostLanguage;

    private SortedIterator(){}

    public SortedIterator(XPathContext context, SequenceIterator base,
                                SortKeyDefinition[] sortkeys, Comparator[] comparators) {
        this.context = context.newMinorContext();
        this.context.setOriginatingConstructType(Location.SORT_KEY);
        this.context.setCurrentIterator(base);
        this.base = base;
        this.sortkeys = sortkeys;
        this.comparators = comparators;
        recordSize = sortkeys.length + 2;

        // Avoid doing the sort until the user wants the first item. This is because
        // sometimes the user only wants to know whether the collection is empty.
    }

    public void setHostLanguage(int language) {
        hostLanguage = language;
    }

    /**
    * Get the next item, in sorted order
    */

    public Item next() throws XPathException {
        if (index < 0) {
            return null;
        }
        if (count<0) {
            doSort();
        }
        if (index < count) {
            return (Item)nodeKeys[(index++)*recordSize];
        } else {
            index = -1;
            return null;
        }
    }

    public Item current() {
        if (index < 1) {
            return null;
        }
        return (Item)nodeKeys[(index-1)*recordSize];
    }

    public int position() {
        return index;
    }

    public int getLastPosition() throws XPathException {
        if (count<0) {
            doSort();
        }
        return count;
    }

    public SequenceIterator getAnother() throws XPathException {
        // make sure the sort has been done, so that multiple iterators over the
        // same sorted data only do the sorting once.
        if (count<0) {
            doSort();
        }
        SortedIterator s = new SortedIterator();
        // the new iterator is the same as the old ...
        s.base = base.getAnother();
        s.sortkeys = sortkeys;
        s.comparators = comparators;
        s.recordSize = recordSize;
        s.nodeKeys = nodeKeys;
        s.count = count;
        s.context = context;
        //s.keyComparers = keyComparers;
        // ... except for its start position.
        s.index = 0;
        return s;
    }

    /**
     * Get properties of this iterator, as a bit-significant integer.
     *
     * @return the properties of this iterator. This will be some combination of
     *         properties such as {@link #GROUNDED}, {@link #LAST_POSITION_FINDER},
     *         and {@link #LOOKAHEAD}. It is always
     *         acceptable to return the value zero, indicating that there are no known special properties.
     *         It is acceptable for the properties of the iterator to change depending on its state.
     */

    public int getProperties() {
        return LAST_POSITION_FINDER;
    }

    /**
     * Create an array holding the items to be sorted and the values of their sort keys
     * @throws XPathException
     */

    protected void buildArray() throws XPathException {
        int allocated;
        if ((base.getProperties() & SequenceIterator.LAST_POSITION_FINDER) != 0) {
            allocated = ((LastPositionFinder)base).getLastPosition();
        } else {
            allocated = 100;
        }

        nodeKeys = new Object[allocated * recordSize];
        count = 0;

        XPathContext c2 = context;

        // initialise the array with data

        while (true) {
            Item item = base.next();
            if (item == null) {
                break;
            }
            if (count==allocated) {
                allocated *= 2;
                Object[] nk2 = new Object[allocated * recordSize];
                System.arraycopy(nodeKeys, 0, nk2, 0, count * recordSize);
                nodeKeys = nk2;
            }
            int k = count*recordSize;
            nodeKeys[k] = item;
            // TODO: delay evaluating the sort keys until we know they are needed. Often the 2nd and subsequent
            // sort key values will never be used. The only problem is with sort keys that depend on position().
            // TODO:BUG?: in XQuery, are we evaluating a sort key with the wrong context item?
            for (int n=0; n<sortkeys.length; n++) {
                nodeKeys[k+n+1] = sortkeys[n].getSortKey().evaluateItem(c2);
            }
            // make the sort stable by adding the record number
            nodeKeys[k+sortkeys.length+1] = new Integer(count);
            count++;
        }

        // If there's lots of unused space, reclaim it

        if (allocated * 2 < count || (allocated - count) > 2000) {
            Object[] nk2 = new Object[count * recordSize];
            System.arraycopy(nodeKeys, 0, nk2, 0, count * recordSize);
            nodeKeys = nk2;
        }
    }

    private void doSort() throws XPathException {
        buildArray();
        if (count<2) return;

        // sort the array

        //QuickSort.sort(this, 0, count-1);
        try {
            GenericSorter.quickSort(0, count, this);
        } catch (ClassCastException e) {
            DynamicError err = new DynamicError("Non-comparable types found while sorting: " + e.getMessage());
            if (hostLanguage == Configuration.XSLT) {
                err.setErrorCode("XTDE1030");
            } else {
                err.setErrorCode("XPTY0004");
            }
            throw err;
        }
        //GenericSorter.mergeSort(0, count, this);
    }

    /**
    * Compare two items in sorted sequence
    * (needed to implement the Sortable interface)
    * @return <0 if obj[a]<obj[b], 0 if obj[a]=obj[b], >0 if obj[a]>obj[b]
    */

    public int compare(int a, int b) {
        int a1 = a*recordSize + 1;
        int b1 = b*recordSize + 1;
        for (int i=0; i<sortkeys.length; i++) {
            int comp = comparators[i].compare(nodeKeys[a1+i], nodeKeys[b1+i]);
            if (comp != 0) {
                // we have found a difference, so we can return
                return comp;
            }
        }

        // all sort keys equal: return the items in their original order

        return ((Integer)nodeKeys[a1+sortkeys.length]).intValue() -
                ((Integer)nodeKeys[b1+sortkeys.length]).intValue();
    }

    /**
    * Swap two items (needed to implement the Sortable interface)
    */

    public void swap(int a, int b) {
        int a1 = a*recordSize;
        int b1 = b*recordSize;
        for (int i=0; i<recordSize; i++) {
            Object temp = nodeKeys[a1+i];
            nodeKeys[a1+i] = nodeKeys[b1+i];
            nodeKeys[b1+i] = temp;
        }
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
// The Original Code is: all this file.
//
// The Initial Developer of the Original Code is Michael H. Kay.
//
// Portions created by (your name) are Copyright (C) (your legal entity). All Rights Reserved.
//
// Contributor(s): none.
//
