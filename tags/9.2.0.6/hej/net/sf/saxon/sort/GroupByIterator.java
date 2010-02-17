package net.sf.saxon.sort;

import net.sf.saxon.expr.Expression;
import net.sf.saxon.expr.LastPositionFinder;
import net.sf.saxon.expr.XPathContext;
import net.sf.saxon.om.Item;
import net.sf.saxon.om.ListIterator;
import net.sf.saxon.om.LookaheadIterator;
import net.sf.saxon.om.SequenceIterator;
import net.sf.saxon.trace.Location;
import net.sf.saxon.trans.XPathException;
import net.sf.saxon.value.AtomicValue;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * A GroupByIterator iterates over a sequence of groups defined by
 * xsl:for-each-group group-by="x". The groups are returned in
 * order of first appearance. Note that an item can appear in several groups;
 * indeed, an item may be the leading item of more than one group, which means
 * that knowing the leading item is not enough to know the current group.
 *
 * <p>The GroupByIterator acts as a SequenceIterator, where successive calls of
 * next() return the leading item of each group in turn. The current item of
 * the iterator is therefore the leading item of the current group. To get access
 * to all the members of the current group, the method iterateCurrentGroup() is used;
 * this underpins the current-group() function in XSLT. The grouping key for the
 * current group is available via the getCurrentGroupingKey() method.</p>
 */

public class GroupByIterator implements GroupIterator, LastPositionFinder, LookaheadIterator {

    // The implementation of group-by is not pipelined. All the items in the population
    // are read at the start, their grouping keys are calculated, and the groups are formed
    // in memory as a hash table indexed by the grouping key. This hash table is then
    // flattened into three parallel lists: a list of groups (each group being represented
    // as a list of items in population order), a list of grouping keys, and a list of
    // the initial items of the groups.

    private SequenceIterator population;
    protected Expression keyExpression;
    private StringCollator collator;
    private XPathContext keyContext;
    private int position = 0;

    // Main data structure holds one entry for each group. The entry is also an ArrayList,
    // which contains the Items that are members of the group, in population order.
    // The groups are arranged in order of first appearance within the population.
    protected List<List<Item>> groups = new ArrayList<List<Item>>(40);

    // This parallel structure identifies the grouping key for each group. The list
    // corresponds one-to-one with the list of groups.
    protected List<AtomicValue> groupKeys = new ArrayList<AtomicValue>(40);

    // A SortComparer is used to do the comparisons
    protected AtomicComparer comparer;

    /**
     * Create a GroupByIterator
     * @param population iterator over the population to be grouped
     * @param keyExpression the expression used to calculate the grouping key
     * @param keyContext dynamic context for calculating the grouping key
     * @param collator Collation to be used for comparing grouping keys
     * @throws XPathException
     */

    public GroupByIterator(SequenceIterator population, Expression keyExpression,
                           XPathContext keyContext, StringCollator collator)
    throws XPathException {
        this.population = population;
        this.keyExpression = keyExpression;
        this.keyContext = keyContext;
        this.collator = collator;
        int type = keyExpression.getItemType(keyContext.getConfiguration().getTypeHierarchy()).getPrimitiveType();
        this.comparer = AtomicSortComparer.makeSortComparer(collator, type, keyContext);
        buildIndexedGroups();
    }

    /**
      * Build the grouping table forming groups of items with equal keys.
      * This form of grouping allows a member of the population to be present in zero
      * or more groups, one for each value of the grouping key.
     */

    private void buildIndexedGroups() throws XPathException {
        HashMap index = new HashMap(40);
        XPathContext c2 = keyContext.newMinorContext();
        c2.setCurrentIterator(population);
        c2.setOriginatingConstructType(Location.GROUPING_KEY);
        while (true) {
            Item item = population.next();
            if (item==null) {
                break;
            }
            processItem(index, item, c2);
        }
    }

    /**
     * Process one item in the population
     * @param index the index of items
     * @param item the item from the population to be processed
     * @param c2 the XPath evaluation context
     * @throws XPathException
     */

    protected void processItem(HashMap<ComparisonKey, List<Item>> index,
                               Item item, XPathContext c2) throws XPathException {
        SequenceIterator keys = keyExpression.iterate(c2);
        boolean firstKey = true;
        while (true) {
            AtomicValue key = (AtomicValue) keys.next();
            if (key==null) {
                break;
            }
            ComparisonKey comparisonKey = comparer.getComparisonKey(key);
            List<Item> g = index.get(comparisonKey);
            if (g == null) {
                List<Item> newGroup = new ArrayList<Item>(20);
                newGroup.add(item);
                groups.add(newGroup);
                groupKeys.add(key);
                index.put(comparisonKey, newGroup);
            } else {
                if (firstKey) {
                    g.add(item);
                } else {
                    // if this is not the first key value for this item, we
                    // check whether the item is already in this group before
                    // adding it again. If it is in this group, then we know
                    // it will be at the end.
                    if (g.get(g.size() - 1) != item) {
                        g.add(item);
                    }
                }
            }
            firstKey = false;
        }
    }

    /**
     * Get the value of the grouping key for the current group
     * @return the grouping key, or null if the grouping key is an empty sequence
     */

    public AtomicValue getCurrentGroupingKey() {
        return groupKeys.get(position-1);
    }

    /**
     * Get an iterator over the items in the current group
     * @return the iterator
     */

    public SequenceIterator iterateCurrentGroup() {
        return new ListIterator(groups.get(position-1));
    }

    /**
     * Get the contents of the current group as a java List
     * @return the contents of the current group
     */

    public List getCurrentGroup() {
        return groups.get(position-1);
    }

    public boolean hasNext() {
        return position < groups.size();
    }

    public Item next() throws XPathException {
        if (position >= 0 && position < groups.size()) {
            position++;
            return current();
        } else {
            position = -1;
            return null;
        }
    }

    public Item current() {
        if (position < 1) {
            return null;
        }
        // return the initial item of the current group
        return (Item)((ArrayList)groups.get(position-1)).get(0);
    }

    public int position() {
        return position;
    }

    public void close() {
    }

    public SequenceIterator getAnother() throws XPathException {
        XPathContext c2 = keyContext.newMinorContext();
        c2.setOriginatingConstructType(Location.GROUPING_KEY);
        return new GroupByIterator(population.getAnother(), keyExpression, c2, collator);
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
        return LAST_POSITION_FINDER | LOOKAHEAD;
    }

    /**
     * Get the last position (that is, the number of groups)
     */

    public int getLastPosition() throws XPathException {
        return groups.size();
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
// The Initial Developer of the Original Code is Michael H. Kay
//
// Portions created by (your name) are Copyright (C) (your legal entity). All Rights Reserved.
//
// Contributor(s): none
//