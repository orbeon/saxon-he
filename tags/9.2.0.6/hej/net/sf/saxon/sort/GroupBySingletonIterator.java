package net.sf.saxon.sort;

import net.sf.saxon.expr.Expression;
import net.sf.saxon.expr.XPathContext;
import net.sf.saxon.om.Item;
import net.sf.saxon.om.SequenceIterator;
import net.sf.saxon.trans.XPathException;
import net.sf.saxon.type.Type;
import net.sf.saxon.value.AtomicValue;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * A variant of the GroupByIterator used for XQuery 1.1 grouping, where the grouping key
 * is either a single atomic value or an empty sequence, and an empty sequence compares
 * equal to an empty sequence.
 */
public class GroupBySingletonIterator extends GroupByIterator {

   /**
     * Create a GroupByIterator
     * @param population iterator over the population to be grouped
     * @param keyExpression the expression used to calculate the grouping key
     * @param keyContext dynamic context for calculating the grouping key
     * @param collator Collation to be used for comparing grouping keys
     * @throws net.sf.saxon.trans.XPathException
     */

    public GroupBySingletonIterator(SequenceIterator population, Expression keyExpression,
                           XPathContext keyContext, StringCollator collator) throws XPathException {
       super(population, keyExpression, keyContext, collator);
    }

    /**
     * Process one item in the population
     * @param index the index of items
     * @param item  the item from the population to be processed
     * @param c2    the XPath evaluation context
     * @throws net.sf.saxon.trans.XPathException
     *
     */

    protected void processItem(HashMap<ComparisonKey, List<Item>> index,
                               Item item, XPathContext c2) throws XPathException {
        AtomicValue key = (AtomicValue)keyExpression.evaluateItem(c2);
        ComparisonKey comparisonKey;
        if (key == null) {
            comparisonKey = new ComparisonKey(Type.EMPTY, "()");
        } else {
            comparisonKey = comparer.getComparisonKey(key);
        }
        List<Item> g = index.get(comparisonKey);
        if (g == null) {
            List<Item> newGroup = new ArrayList<Item>(20);
            newGroup.add(item);
            groups.add(newGroup);
            groupKeys.add(key);
            index.put(comparisonKey, newGroup);
        } else {
            g.add(item);
        }
    }
}

//
// The contents of this file are subject to the Mozilla Public License Version
// 1.0 (the "License");
// you may not use this file except in compliance with the License. You may
// obtain a copy of the
// License at http://www.mozilla.org/MPL/
//
// Software distributed under the License is distributed on an "AS IS" basis,
// WITHOUT WARRANTY OF ANY KIND, either express or implied.
// See the License for the specific language governing rights and limitations
// under the License.
//
// The Original Code is: all this file.
//
// The Initial Developer of the Original Code is Michael Kay,
//
// Portions created by (your name) are Copyright (C) (your legal entity). All
// Rights Reserved.
//
// Contributor(s): none.
//



