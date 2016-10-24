package net.sf.saxon.functions;

import net.sf.saxon.expr.XPathContext;
import net.sf.saxon.expr.sort.*;
import net.sf.saxon.lib.StringCollator;
import net.sf.saxon.ma.arrays.ArraySort;
import net.sf.saxon.om.*;
import net.sf.saxon.trans.XPathException;
import net.sf.saxon.value.SequenceExtent;

import java.util.ArrayList;
import java.util.List;

/**
 * This class implements the function fn:sort#1, which is a standard function in XPath 3.1
 */

public class SortOne extends SystemFunction {

    public static class ItemToBeSorted {
        public Item value;
        public GroundedValue sortKey;
        public int originalPosition;
    }

    /**
     * Evaluate the expression
     *
     * @param context   the dynamic evaluation context
     * @param arguments the values of the arguments, supplied as SequenceIterators
     * @return the result of the evaluation, in the form of a SequenceIterator
     * @throws XPathException if a dynamic error occurs during the evaluation of the expression
     */
    public Sequence call(XPathContext context, Sequence[] arguments) throws XPathException {
        Sequence input = arguments[0];
        final List<ItemToBeSorted> inputList = new ArrayList<ItemToBeSorted>();
        int i = 0;
        SequenceIterator iterator = input.iterate();
        Item item;
        while ((item = iterator.next()) != null) {
            ItemToBeSorted member = new ItemToBeSorted();
            member.value = item;
            member.originalPosition = i++;
            member.sortKey = item.atomize();
            inputList.add(member);
        }
        StringCollator collation = context.getConfiguration().getCollation(getRetainedStaticContext().getDefaultCollationName());
        return doSort(inputList, collation, context);
    }

    protected Sequence doSort(final List<ItemToBeSorted> inputList, StringCollator collation, XPathContext context) throws XPathException {
        final AtomicComparer atomicComparer = AtomicSortComparer.makeSortComparer(
                collation, StandardNames.XS_ANY_ATOMIC_TYPE, context);

        Sortable sortable = new Sortable() {
            public int compare(int a, int b) {
                int result = ArraySort.compareSortKeys(inputList.get(a).sortKey, inputList.get(b).sortKey, atomicComparer);
                if (result == 0) {
                    return inputList.get(a).originalPosition - inputList.get(b).originalPosition;
                } else {
                    return result;
                }
            }

            public void swap(int a, int b) {
                ItemToBeSorted temp = inputList.get(a);
                inputList.set(a, inputList.get(b));
                inputList.set(b, temp);
            }
        };
        try {
            GenericSorter.quickSort(0, inputList.size(), sortable);
        } catch (ClassCastException e) {
            XPathException err = new XPathException("Non-comparable types found while sorting: " + e.getMessage());
            err.setErrorCode("XPTY0004");
            throw err;
        }
        List<Item> outputList = new ArrayList<Item>(inputList.size());
        for (ItemToBeSorted member : inputList) {
            outputList.add(member.value);
        }
        return new SequenceExtent(outputList);
    }


}


// Copyright (c) 2015 Saxonica Limited.