////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
// Copyright (c) 2013 Saxonica Limited.
// This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
// If a copy of the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
// This Source Code Form is "Incompatible With Secondary Licenses", as defined by the Mozilla Public License, v. 2.0.
////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

package net.sf.saxon.expr.sort;

import net.sf.saxon.om.Item;
import net.sf.saxon.om.NodeInfo;
import net.sf.saxon.om.SequenceIterator;
import net.sf.saxon.trans.XPathException;
import net.sf.saxon.value.SequenceExtent;

/**
* DocumentOrderIterator takes as input an iteration of nodes in any order, and
* returns as output an iteration of the same nodes in document order, eliminating
* any duplicates.
*/

public final class DocumentOrderIterator implements SequenceIterator, Sortable {

    private SequenceIterator iterator;
    private SequenceExtent sequence;
    private ItemOrderComparer comparer;
    /*@Nullable*/ private NodeInfo current = null;
    private int position = 0;

    /**
    * Iterate over a sequence in document order.
    */

    public DocumentOrderIterator(SequenceIterator base, ItemOrderComparer comparer) throws XPathException {

        this.comparer = comparer;

        sequence = new SequenceExtent(base);
        //System.err.println("sort into document order: sequence length = " + sequence.getLength());
        if (sequence.getLength()>1) {
            //QuickSort.sort(this, 0, sequence.getLength()-1);
            GenericSorter.quickSort(0, sequence.getLength(), this);
            //GenericSorter.mergeSort(0, sequence.getLength(), this);
        }
        iterator = sequence.iterate();
    }

    /**
    * Private constructor used only by getAnother()
    */

    private DocumentOrderIterator() {}

    /**
    * Compare two nodes in document sequence
    * (needed to implement the Sortable interface)
    */

    public int compare(int a, int b) {
        //System.err.println("compare " + a + " with " + b);
        return comparer.compare((NodeInfo)sequence.itemAt(a),
                                (NodeInfo)sequence.itemAt(b));
    }

    /**
    * Swap two nodes (needed to implement the Sortable interface)
    */

    public void swap(int a, int b) {
        sequence.swap(a, b);
    }

    // Implement the SequenceIterator as a wrapper around the underlying iterator
    // over the sequenceExtent, but looking ahead to remove duplicates.

    public Item next() throws XPathException {
        while (true) {
            NodeInfo next = (NodeInfo)iterator.next();
            if (next == null) {
                current = null;
                position = -1;
                return null;
            }
            if (current != null && next.isSameNodeInfo(current)) {
                continue;
            } else {
                position++;
                current = next;
                return current;
            }
        }
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
        return 0;
    }

    public Item current() {
        return current;
    }

    public int position() {
        return position;
    }

    public void close() {
    }

    /*@NotNull*/
    public SequenceIterator getAnother() throws XPathException {
        DocumentOrderIterator another = new DocumentOrderIterator();
        another.iterator = iterator.getAnother();    // don't need to sort it again
        return another;
    }

}

