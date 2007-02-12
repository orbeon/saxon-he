package net.sf.saxon.sort;

import net.sf.saxon.Configuration;
import net.sf.saxon.expr.*;
import net.sf.saxon.om.EmptyIterator;
import net.sf.saxon.om.Item;
import net.sf.saxon.om.SequenceIterator;
import net.sf.saxon.trans.XPathException;
import net.sf.saxon.type.ItemType;
import net.sf.saxon.type.TypeHierarchy;
import net.sf.saxon.value.Cardinality;

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Expression equivalent to the imaginary syntax
 * expr sortby (sort-key)+
 */

public class SortExpression extends Expression implements SortKeyEvaluator {

    private Expression select = null;
    private SortKeyDefinition[] sortKeyDefinitions = null;
    private transient AtomicComparer[] comparators = null;
        // created early if all comparators can be created statically
        // transient because Java RuleBasedCollator is not serializable


    public SortExpression(Expression select, SortKeyDefinition[] sortKeys) {
        this.select = select;
        this.sortKeyDefinitions = sortKeys;
        Iterator children = iterateSubExpressions();
        while (children.hasNext()) {
            Expression exp = (Expression) children.next();
            adoptChildExpression(exp);
        }
    }

    /**
     * Get the expression defining the sequence being sorted
     */

    public Expression getBaseExpression() {
        return select;
    }

    /**
     * Get the immediate sub-expressions of this expression. Default implementation
     * returns a zero-length array, appropriate for an expression that has no
     * sub-expressions.
     *
     * @return an iterator containing the sub-expressions of this expression
     */

    public Iterator iterateSubExpressions() {
        List list = new ArrayList(8);
        list.add(select);
        for (int i = 0; i < sortKeyDefinitions.length; i++) {
            list.add(sortKeyDefinitions[i].getSortKey());
            Expression e = sortKeyDefinitions[i].order;
            if (e != null) {
                list.add(e);
            }
            e = sortKeyDefinitions[i].caseOrder;
            if (e != null) {
                list.add(e);
            }
            e = sortKeyDefinitions[i].dataTypeExpression;
            if (e != null) {
                list.add(e);
            }
            e = sortKeyDefinitions[i].language;
            if (e != null) {
                list.add(e);
            }
            e = sortKeyDefinitions[i].collationName;
            if (e != null) {
                list.add(e);
            }
            e = sortKeyDefinitions[i].stable;
            if (e != null) {
                list.add(e);
            }
        }
        return list.iterator();
    }

    /**
     * Replace one subexpression by a replacement subexpression
     *
     * @param original    the original subexpression
     * @param replacement the replacement subexpression
     * @return true if the original subexpression is found
     */

    public boolean replaceSubExpression(Expression original, Expression replacement) {
        boolean found = false;
        if (select == original) {
            select = replacement;
            found = true;
        }
        for (int i = 0; i < sortKeyDefinitions.length; i++) {
            if (sortKeyDefinitions[i].getSortKey() == original) {
                sortKeyDefinitions[i].setSortKey(replacement);
                found = true;
            }
            if (sortKeyDefinitions[i].getOrder() == original) {
                sortKeyDefinitions[i].setOrder(replacement);
                found = true;
            }
            if (sortKeyDefinitions[i].getCaseOrder() == original) {
                sortKeyDefinitions[i].setCaseOrder(replacement);
                found = true;
            }
            if (sortKeyDefinitions[i].getDataTypeExpression() == original) {
                sortKeyDefinitions[i].setDataTypeExpression(replacement);
                found = true;
            }
            if (sortKeyDefinitions[i].getLanguage() == original) {
                sortKeyDefinitions[i].setLanguage(replacement);
                found = true;
            }
        }
        return found;
    }

    /**
     * Simplify an expression
     */

    public Expression simplify(StaticContext env) throws XPathException {
        select = select.simplify(env);
        return this;
    }

    /**
     * Type-check the expression
     */

    public Expression typeCheck(StaticContext env, ItemType contextItemType) throws XPathException {
        Expression select2 = select.typeCheck(env, contextItemType);
        if (select2 != select) {
            adoptChildExpression(select2);
            select = select2;
        }
        ItemType sortedItemType = select.getItemType(env.getConfiguration().getTypeHierarchy());

        boolean allKeysFixed = true;
        for (int i = 0; i < sortKeyDefinitions.length; i++) {
            sortKeyDefinitions[i].setParentExpression(this);
            if (!(sortKeyDefinitions[i].isFixed())) {
                allKeysFixed = false;
            }
        }

        if (allKeysFixed) {
            comparators = new AtomicComparer[sortKeyDefinitions.length];
        }

        for (int i = 0; i < sortKeyDefinitions.length; i++) {
            Expression sortKey = sortKeyDefinitions[i].getSortKey();
            sortKey = sortKey.typeCheck(env, sortedItemType);
            if (env.isInBackwardsCompatibleMode()) {
                sortKey = new FirstItemExpression(sortKey);
            } else {
                RoleLocator role =
                        new RoleLocator(RoleLocator.INSTRUCTION, "xsl:sort/select", 0, null);
                role.setErrorCode("XTTE1020");
                sortKey = CardinalityChecker.makeCardinalityChecker(sortKey, StaticProperty.ALLOWS_ZERO_OR_ONE, role);
            }
            sortKeyDefinitions[i].setSortKey(sortKey);
            if (sortKeyDefinitions[i].isFixed()) {
                AtomicComparer comp = sortKeyDefinitions[i].makeComparator(env.makeEarlyEvaluationContext());
                sortKeyDefinitions[i].setFinalComparator(comp);
                if (allKeysFixed) {
                    comparators[i] = comp;
                }
            }

        }
        return this;
    }

    /**
     * Perform optimisation of an expression and its subexpressions.
     * <p/>
     * <p>This method is called after all references to functions and variables have been resolved
     * to the declaration of the function or variable, and after all type checking has been done.</p>
     *
     * @param opt             the optimizer in use. This provides access to supporting functions; it also allows
     *                        different optimization strategies to be used in different circumstances.
     * @param env             the static context of the expression
     * @param contextItemType the static type of "." at the point where this expression is invoked.
     *                        The parameter is set to null if it is known statically that the context item will be undefined.
     *                        If the type of the context item is not known statically, the argument is set to
     *                        {@link net.sf.saxon.type.Type#ITEM_TYPE}
     * @return the original expression, rewritten if appropriate to optimize execution
     * @throws net.sf.saxon.trans.StaticError if an error is discovered during this phase
     *                                        (typically a type error)
     */

    public Expression optimize(Optimizer opt, StaticContext env, ItemType contextItemType) throws XPathException {
        Expression select2 = select.optimize(opt, env, contextItemType);
        if (select2 != select) {
            adoptChildExpression(select2);
            select = select2;
        }
        //ItemType sortedItemType = select.getItemType(env.getConfiguration().getTypeHierarchy());
        // TODO: optimize the sort keys etc.
        if (Cardinality.allowsMany(select.getCardinality())) {
            return this;
        } else {
            Expression.setParentExpression(select, getParentExpression());
            return select;
        }
    }

    /**
     * Offer promotion for this subexpression. The offer will be accepted if the subexpression
     * is not dependent on the factors (e.g. the context item) identified in the PromotionOffer.
     * By default the offer is not accepted - this is appropriate in the case of simple expressions
     * such as constant values and variable references where promotion would give no performance
     * advantage. This method is always called at compile time.
     *
     * @param offer details of the offer, for example the offer to move
     *              expressions that don't depend on the context to an outer level in
     *              the containing expression
     * @return if the offer is not accepted, return this expression unchanged.
     *         Otherwise return the result of rewriting the expression to promote
     *         this subexpression
     * @throws net.sf.saxon.trans.XPathException
     *          if any error is detected
     */

    public Expression promote(PromotionOffer offer) throws XPathException {
        Expression exp = offer.accept(this);
        if (exp != null) {
            return exp;
        } else {
            select = doPromotion(select, offer);
            for (int i = 0; i < sortKeyDefinitions.length; i++) {
                final Expression sk2 = sortKeyDefinitions[i].getSortKey().promote(offer);
                sortKeyDefinitions[i].setSortKey(sk2);
                if (sortKeyDefinitions[i].caseOrder != null) {
                    sortKeyDefinitions[i].caseOrder = sortKeyDefinitions[i].caseOrder.promote(offer);
                }
                if (sortKeyDefinitions[i].dataTypeExpression != null) {
                    sortKeyDefinitions[i].dataTypeExpression = sortKeyDefinitions[i].dataTypeExpression.promote(offer);
                }
                if (sortKeyDefinitions[i].language != null) {
                    sortKeyDefinitions[i].language = sortKeyDefinitions[i].language.promote(offer);
                }
                if (sortKeyDefinitions[i].collationName != null) {
                    sortKeyDefinitions[i].collationName = sortKeyDefinitions[i].collationName.promote(offer);
                }
            }
            return this;
        }
    }

    /**
     * Test whether a given expression is one of the sort keys
     */

    public boolean isSortKey(Expression child) {
        for (int i = 0; i < sortKeyDefinitions.length; i++) {
            Expression exp = sortKeyDefinitions[i].getSortKey();
            if (exp == child) {
                return true;
            }
        }
        return false;
    }

    /**
     * Determine the static cardinality
     */

    public int computeCardinality() {
        return select.getCardinality();
    }

    /**
     * Determine the data type of the items returned by the expression, if possible
     *
     * @param th
     * @return a value such as Type.STRING, Type.BOOLEAN, Type.NUMBER, Type.NODE,
     *         or Type.ITEM (meaning not known in advance)
     */

    public ItemType getItemType(TypeHierarchy th) {
        return select.getItemType(th);
    }

    /**
     * Get the static properties of this expression (other than its type). The result is
     * bit-significant. These properties are used for optimizations. In general, if
     * property bit is set, it is true, but if it is unset, the value is unknown.
     */

    public int computeSpecialProperties() {
        int props = 0;
        if ((select.getSpecialProperties() & StaticProperty.CONTEXT_DOCUMENT_NODESET) != 0) {
            props |= StaticProperty.CONTEXT_DOCUMENT_NODESET;
        }
        if ((select.getSpecialProperties() & StaticProperty.SINGLE_DOCUMENT_NODESET) != 0) {
            props |= StaticProperty.SINGLE_DOCUMENT_NODESET;
        }
        if ((select.getSpecialProperties() & StaticProperty.NON_CREATIVE) != 0) {
            props |= StaticProperty.NON_CREATIVE;
        }
        return props;
    }

    /**
     * Enumerate the results of the expression
     */

    public SequenceIterator iterate(XPathContext context) throws XPathException {

        SequenceIterator iter = select.iterate(context);
        if (iter instanceof EmptyIterator) {
            return iter;
        }
        XPathContext xpc = context.newMinorContext();
        xpc.setOrigin(this);

        AtomicComparer[] comps = comparators;
        if (comparators == null) {
            comps = new AtomicComparer[sortKeyDefinitions.length];
            for (int s = 0; s < sortKeyDefinitions.length; s++) {
                AtomicComparer comp = sortKeyDefinitions[s].getFinalComparator();
                if (comp == null) {
                    comp = sortKeyDefinitions[s].makeComparator(xpc);
                }
                comps[s] = comp;
            }
        }
        iter = new SortedIterator(xpc, iter, this, comps);
        ((SortedIterator) iter).setHostLanguage(getHostLanguage());
        return iter;
    }

    /**
     * Callback for evaluating the sort keys
     */

    public Item evaluateSortKey(int n, XPathContext c) throws XPathException {
        return sortKeyDefinitions[n].getSortKey().evaluateItem(c);
    }

    /**
     * Diagnostic display of the expression
     * @param level
     * @param out
     * @param config
     */

    public void display(int level, PrintStream out, Configuration config) {
        out.println(ExpressionTool.indent(level) + "sort");
        select.display(level + 1, out, config);
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
// Contributor(s): none.
//
