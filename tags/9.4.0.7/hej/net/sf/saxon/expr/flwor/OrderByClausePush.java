package net.sf.saxon.expr.flwor;

import net.sf.saxon.expr.XPathContext;
import net.sf.saxon.expr.sort.*;
import net.sf.saxon.trans.NoDynamicContextException;
import net.sf.saxon.trans.XPathException;
import net.sf.saxon.value.AtomicValue;

import java.util.ArrayList;

/**
 * Represents the tuple stream delivered by an "order by" clause. This sorts the tuple stream supplied
 * as its input, and outputs the same tuples but in sorted order.
 */
public class OrderByClausePush extends TuplePush implements Sortable {

    private TuplePush destination;
    private OrderByClause orderByClause;
    private TupleExpression tupleExpr;
    protected AtomicComparer[] comparers;
    XPathContext context;
    int position = 0;
    private ArrayList<ItemToBeSorted> tupleArray = new ArrayList<ItemToBeSorted>(100);

    public OrderByClausePush(TuplePush destination, TupleExpression tupleExpr, OrderByClause orderBy, XPathContext context) {
        this.destination = destination;
        this.tupleExpr = tupleExpr;
        this.orderByClause = orderBy;
        this.context = context;

        AtomicComparer[] suppliedComparers = orderBy.getAtomicComparers();
        comparers = new AtomicComparer[suppliedComparers.length];
        for (int n=0; n< comparers.length; n++) {
            this.comparers[n] = suppliedComparers[n].provideContext(context);
        }
    }

    /**
     * Process the next tuple.
     */
    @Override
    public void processTuple(XPathContext context) throws XPathException {

        Tuple tuple = tupleExpr.evaluateItem(context);
        SortKeyDefinition[] sortKeyDefinitions = orderByClause.getSortKeyDefinitions();
        ItemToBeSorted itbs = new ItemToBeSorted(sortKeyDefinitions.length);
        itbs.value = tuple;
        for (int i=0; i<sortKeyDefinitions.length; i++) {
            itbs.sortKeyValues[i] = orderByClause.evaluateSortKey(i, context);
        }
        itbs.originalPosition = ++position;
        tupleArray.add(itbs);

    }

    /**
     * Compare two objects within this Sortable, identified by their position.
     *
     * @return <0 if obj[a]<obj[b], 0 if obj[a]=obj[b], >0 if obj[a]>obj[b]
     */
    public int compare(int a, int b) {
        try {
            for (int i=0; i< comparers.length; i++) {
                int comp = comparers[i].compareAtomicValues(
                        (AtomicValue)tupleArray.get(a).sortKeyValues[i], (AtomicValue)tupleArray.get(b).sortKeyValues[i]);
                if (comp != 0) {
                    // we have found a difference, so we can return
                    return comp;
                }
            }
        } catch (NoDynamicContextException e) {
            throw new AssertionError("Sorting without dynamic context: " + e.getMessage());
        }

        // all sort keys equal: return the items in their original order

        return tupleArray.get(a).originalPosition - tupleArray.get(b).originalPosition;
    }

    /**
     * Swap two objects within this Sortable, identified by their position.
     */
    public void swap(int a, int b) {
        ItemToBeSorted temp = tupleArray.get(a);
        tupleArray.set(a, tupleArray.get(b));
        tupleArray.set(b, temp);
    }

    /**
     * Close the tuple stream, indicating that no more tuples will be delivered
     */
    @Override
    public void close() throws XPathException {
        try {
            GenericSorter.quickSort(0, position, this);
        } catch (ClassCastException e) {
            XPathException err = new XPathException("Non-comparable types found while sorting: " + e.getMessage());
            err.setErrorCode("XPTY0004");
            throw err;
        }

        for (ItemToBeSorted itbs : tupleArray) {
            tupleExpr.setCurrentTuple(context, (Tuple)itbs.value);
            destination.processTuple(context);
        }
        destination.close();
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