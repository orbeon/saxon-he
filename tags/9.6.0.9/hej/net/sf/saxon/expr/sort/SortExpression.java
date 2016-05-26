////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
// Copyright (c) 2014 Saxonica Limited.
// This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
// If a copy of the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
// This Source Code Form is "Incompatible With Secondary Licenses", as defined by the Mozilla Public License, v. 2.0.
////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

package net.sf.saxon.expr.sort;

import com.saxonica.ee.bytecode.ExpressionCompiler;
import com.saxonica.ee.bytecode.SortExpressionCompiler;
import net.sf.saxon.expr.OperandUsage;
import com.saxonica.ee.stream.adjunct.SortExpressionAdjunct;
import com.saxonica.ee.stream.adjunct.StreamingAdjunct;
import net.sf.saxon.expr.*;
import net.sf.saxon.expr.parser.*;
import net.sf.saxon.om.Item;
import net.sf.saxon.om.SequenceIterator;
import net.sf.saxon.trace.ExpressionPresenter;
import net.sf.saxon.trans.XPathException;
import net.sf.saxon.tree.iter.EmptyIterator;
import net.sf.saxon.type.ItemType;
import net.sf.saxon.value.AtomicValue;
import net.sf.saxon.value.Cardinality;
import net.sf.saxon.value.SequenceType;

import java.util.ArrayList;
import java.util.List;

/**
 * Expression equivalent to the imaginary syntax
 * expr sortby (sort-key)+
 */

public class SortExpression extends Expression
        implements SortKeyEvaluator {

    /*@Nullable*/ private Expression select = null;
    private SortKeyDefinition[] sortKeyDefinitions = null;
    private transient AtomicComparer[] comparators = null;
    // created early if all comparators can be created statically
    // transient because Java RuleBasedCollator is not serializable

    /**
     * Create a sort expression
     *
     * @param select   the expression whose result is to be sorted
     * @param sortKeys the set of sort key definitions to be used, in major to minor order
     */

    public SortExpression(Expression select, SortKeyDefinition[] sortKeys) {
        this.select = select;
        sortKeyDefinitions = sortKeys;
        for (Operand o : operands()) {
            adoptChildExpression(o.getExpression());
        }
    }

    /**
     * Get a name identifying the kind of expression, in terms meaningful to a user.
     *
     * @return a name identifying the kind of expression, in terms meaningful to a user.
     *         The name will always be in the form of a lexical XML QName, and should match the name used
     *         in explain() output displaying the expression.
     */

    public String getExpressionName() {
        return "sort";
    }

    /**
     * Get the expression defining the sequence being sorted
     *
     * @return the expression whose result is to be sorted
     */

    public Expression getBaseExpression() {
        return select;
    }

    /**
     * Get the sort key definitions
     *
     * @return the sort key definitions, one per sort key
     */

    public SortKeyDefinition[] getSortKeyDefinitions() {
        return sortKeyDefinitions;
    }

    /**
     * Get the comparators, if known statically. Otherwise, return null.
     *
     * @return The comparators, if they have been allocated; otherwise null
     */

    public AtomicComparer[] getComparators() {
        return comparators;
    }

    /**
     * Get the immediate sub-expressions of this expression, with information about the relationship
     * of each expression to its parent expression. Default implementation
     * returns a zero-length array, appropriate for an expression that has no
     * sub-expressions.
     *
     * @return an iterator containing the sub-expressions of this expression
     */
    @Override
    public Iterable<Operand> operands() {
        List<Operand> list = new ArrayList<Operand>(8);
        list.add(new Operand(select, OperandRole.FOCUS_CONTROLLING_SELECT));
        for (SortKeyDefinition sortKeyDefinition : sortKeyDefinitions) {
            list.add(new Operand(sortKeyDefinition.getSortKey(),
                    sortKeyDefinition.isSetContextForSortKey() ? NEW_FOCUS_SORT_KEY : SAME_FOCUS_SORT_KEY));
            Expression e = sortKeyDefinition.order;
            if (e != null) {
                list.add(new Operand(e, OperandRole.SINGLE_ATOMIC));
            }
            e = sortKeyDefinition.caseOrder;
            if (e != null) {
                list.add(new Operand(e, OperandRole.SINGLE_ATOMIC));
            }
            e = sortKeyDefinition.dataTypeExpression;
            if (e != null) {
                list.add(new Operand(e, OperandRole.SINGLE_ATOMIC));
            }
            e = sortKeyDefinition.language;
            if (e != null) {
                list.add(new Operand(e, OperandRole.SINGLE_ATOMIC));
            }
            e = sortKeyDefinition.collationName;
            if (e != null) {
                list.add(new Operand(e, OperandRole.SINGLE_ATOMIC));
            }
            e = sortKeyDefinition.stable;
            if (e != null) {
                list.add(new Operand(e, OperandRole.SINGLE_ATOMIC));
            }
        }
        return list;
    }

    private static final OperandRole SAME_FOCUS_SORT_KEY =
            new OperandRole(OperandRole.HIGHER_ORDER, OperandUsage.ABSORPTION, SequenceType.OPTIONAL_ATOMIC);
    private static final OperandRole NEW_FOCUS_SORT_KEY =
            new OperandRole(OperandRole.USES_NEW_FOCUS | OperandRole.HIGHER_ORDER, OperandUsage.ABSORPTION, SequenceType.OPTIONAL_ATOMIC);


    /**
     * Add a representation of this expression to a PathMap. The PathMap captures a map of the nodes visited
     * by an expression in a source tree.
     * <p/>
     * <p>The default implementation of this method assumes that an expression does no navigation other than
     * the navigation done by evaluating its subexpressions, and that the subexpressions are evaluated in the
     * same context as the containing expression. The method must be overridden for any expression
     * where these assumptions do not hold. For example, implementations exist for AxisExpression, ParentExpression,
     * and RootExpression (because they perform navigation), and for the doc(), document(), and collection()
     * functions because they create a new navigation root. Implementations also exist for PathExpression and
     * FilterExpression because they have subexpressions that are evaluated in a different context from the
     * calling expression.</p>
     *
     * @param pathMap        the PathMap to which the expression should be added
     * @param pathMapNodeSet the PathMapNodeSet to which the paths embodied in this expression should be added
     * @return the pathMapNode representing the focus established by this expression, in the case where this
     *         expression is the first operand of a path expression or filter expression. For an expression that does
     *         navigation, it represents the end of the arc in the path map that describes the navigation route. For other
     *         expressions, it is the same as the input pathMapNode.
     */

    public PathMap.PathMapNodeSet addToPathMap(PathMap pathMap, PathMap.PathMapNodeSet pathMapNodeSet) {
        PathMap.PathMapNodeSet target = select.addToPathMap(pathMap, pathMapNodeSet);
        if (sortKeyDefinitions != null) {
            for (SortKeyDefinition sortKeyDefinition : sortKeyDefinitions) {
                if (sortKeyDefinition.isSetContextForSortKey()) {
                    sortKeyDefinition.getSortKey().addToPathMap(pathMap, target);
                } else {
                    sortKeyDefinition.getSortKey().addToPathMap(pathMap, pathMapNodeSet);
                }
                Expression e = sortKeyDefinition.getOrder();
                if (e != null) {
                    e.addToPathMap(pathMap, pathMapNodeSet);
                }
                e = sortKeyDefinition.getCaseOrder();
                if (e != null) {
                    e.addToPathMap(pathMap, pathMapNodeSet);
                }
                e = sortKeyDefinition.getDataTypeExpression();
                if (e != null) {
                    e.addToPathMap(pathMap, pathMapNodeSet);
                }
                e = sortKeyDefinition.getLanguage();
                if (e != null) {
                    e.addToPathMap(pathMap, pathMapNodeSet);
                }
                e = sortKeyDefinition.getCollationNameExpression();
                if (e != null) {
                    e.addToPathMap(pathMap, pathMapNodeSet);
                }
            }
        }
        return target;
    }

    /**
     * Replace one subexpression by a replacement subexpression
     *
     * @param original    the original subexpression
     * @param replacement the replacement subexpression
     * @return true if the original subexpression is found
     */

    public boolean replaceOperand(Expression original, Expression replacement) {
        boolean found = false;
        if (select == original) {
            select = replacement;
            found = true;
        }
        for (SortKeyDefinition sortKeyDefinition : sortKeyDefinitions) {
            if (sortKeyDefinition.getSortKey() == original) {
                sortKeyDefinition.setSortKey(replacement, true);
                found = true;
            }
            if (sortKeyDefinition.getOrder() == original) {
                sortKeyDefinition.setOrder(replacement);
                found = true;
            }
            if (sortKeyDefinition.getCaseOrder() == original) {
                sortKeyDefinition.setCaseOrder(replacement);
                found = true;
            }
            if (sortKeyDefinition.getDataTypeExpression() == original) {
                sortKeyDefinition.setDataTypeExpression(replacement);
                found = true;
            }
            if (sortKeyDefinition.getLanguage() == original) {
                sortKeyDefinition.setLanguage(replacement);
                found = true;
            }
            if (sortKeyDefinition.collationName == original) {
                sortKeyDefinition.collationName = replacement;
                found = true;
            }
            if (sortKeyDefinition.stable == original) {
                sortKeyDefinition.stable = replacement;
                found = true;
            }
        }
        return found;
    }

    /**
     * Type-check the expression
     */

    /*@NotNull*/
    public Expression typeCheck(ExpressionVisitor visitor, ContextItemStaticInfo contextInfo) throws XPathException {
        Expression select2 = visitor.typeCheck(select, contextInfo);
        if (select2 != select) {
            adoptChildExpression(select2);
            select = select2;
        }
        if (!Cardinality.allowsMany(select2.getCardinality())) {
            // exit now because otherwise the type checking of the sort key can cause spurious failures
            return select2;
        }
        ItemType sortedItemType = select.getItemType();

        boolean allKeysFixed = true;
        for (SortKeyDefinition sortKeyDefinition : sortKeyDefinitions) {
            if (!(sortKeyDefinition.isFixed())) {
                allKeysFixed = false;
                break;
            }
        }

        if (allKeysFixed) {
            comparators = new AtomicComparer[sortKeyDefinitions.length];
        }

        for (int i = 0; i < sortKeyDefinitions.length; i++) {
            Expression sortKey = sortKeyDefinitions[i].getSortKey();
            if (sortKeyDefinitions[i].isSetContextForSortKey()) {
                ContextItemStaticInfo cit = new ContextItemStaticInfo(sortedItemType, false);
                sortKey = visitor.typeCheck(sortKey, cit);
            } else {
                sortKey = visitor.typeCheck(sortKey, contextInfo);
            }
            if (visitor.getStaticContext().isInBackwardsCompatibleMode()) {
                sortKey = FirstItemExpression.makeFirstItemExpression(sortKey);
            } else {
                RoleLocator role =
                        new RoleLocator(RoleLocator.INSTRUCTION, "xsl:sort/select", 0);
                role.setErrorCode("XTTE1020");
                sortKey = TypeChecker.staticTypeCheck(sortKey, SequenceType.OPTIONAL_ATOMIC, false, role, visitor);
                //sortKey = CardinalityChecker.makeCardinalityChecker(sortKey, StaticProperty.ALLOWS_ZERO_OR_ONE, role);
            }
            sortKeyDefinitions[i].setSortKey(sortKey, sortKeyDefinitions[i].isSetContextForSortKey());
            sortKeyDefinitions[i].typeCheck(visitor, contextInfo);
            if (sortKeyDefinitions[i].isFixed()) {
                AtomicComparer comp = sortKeyDefinitions[i].makeComparator(
                        visitor.getStaticContext().makeEarlyEvaluationContext());
                sortKeyDefinitions[i].setFinalComparator(comp);
                if (allKeysFixed) {
                    comparators[i] = comp;
                }
            }
            if (sortKeyDefinitions[i].isSetContextForSortKey() && !ExpressionTool.dependsOnFocus(sortKey)) {
                visitor.getStaticContext().issueWarning(
                        "Sort key will have no effect because its value does not depend on the context item",
                        sortKey);
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
     * @param visitor         an expression visitor
     * @param contextItemType the static type of "." at the point where this expression is invoked.
     *                        The parameter is set to null if it is known statically that the context item will be undefined.
     *                        If the type of the context item is not known statically, the argument is set to
     *                        {@link net.sf.saxon.type.Type#ITEM_TYPE}
     * @return the original expression, rewritten if appropriate to optimize execution
     * @throws XPathException if an error is discovered during this phase
     *                        (typically a type error)
     */

    /*@NotNull*/
    public Expression optimize(ExpressionVisitor visitor, ContextItemStaticInfo contextItemType) throws XPathException {
        Expression select2 = visitor.optimize(select, contextItemType);
        if (select2 != select) {
            adoptChildExpression(select2);
            select = select2;
        }
        // optimize the sort keys
        ContextItemStaticInfo cit;
        if (sortKeyDefinitions[0].isSetContextForSortKey()) {
            ItemType sortedItemType = select.getItemType();
            cit = new ContextItemStaticInfo(sortedItemType, false);
        } else {
            cit = contextItemType;
        }
        for (SortKeyDefinition sortKeyDefinition : sortKeyDefinitions) {
            Expression sortKey = sortKeyDefinition.getSortKey();
            sortKey = visitor.optimize(sortKey, cit);
            sortKeyDefinition.setSortKey(sortKey, true);
        }
        if (Cardinality.allowsMany(select.getCardinality())) {
            return this;
        } else {
            return select;
        }
    }

    /**
     * Copy an expression. This makes a deep copy.
     *
     * @return the copy of the original expression
     */

    /*@NotNull*/
    public Expression copy() {
        SortKeyDefinition[] sk2 = new SortKeyDefinition[sortKeyDefinitions.length];
        for (int i = 0; i < sortKeyDefinitions.length; i++) {
            sk2[i] = sortKeyDefinitions[i].copy();
        }
        SortExpression se2 = new SortExpression(select.copy(), sk2);
        se2.comparators = comparators;
        return se2;
    }

    /**
     * Offer promotion for this subexpression. The offer will be accepted if the subexpression
     * is not dependent on the factors (e.g. the context item) identified in the PromotionOffer.
     * By default the offer is not accepted - this is appropriate in the case of simple expressions
     * such as constant values and variable references where promotion would give no performance
     * advantage. This method is always called at compile time.
     *
     * @param offer  details of the offer, for example the offer to move
     *               expressions that don't depend on the context to an outer level in
     *               the containing expression
     * @param parent the parent of this expression in the expression tree
     * @return if the offer is not accepted, return this expression unchanged.
     *         Otherwise return the result of rewriting the expression to promote
     *         this subexpression
     * @throws net.sf.saxon.trans.XPathException
     *          if any error is detected
     */

    public Expression promote(PromotionOffer offer, Expression parent) throws XPathException {
        Expression exp = offer.accept(parent, this);
        if (exp != null) {
            return exp;
        } else {
            select = doPromotion(select, offer);
            for (SortKeyDefinition sortKeyDefinition : sortKeyDefinitions) {
                final Expression sk2 = sortKeyDefinition.getSortKey().promote(offer, parent);
                sortKeyDefinition.setSortKey(sk2, true);
                if (sortKeyDefinition.order != null) {
                    sortKeyDefinition.order = sortKeyDefinition.order.promote(offer, parent);
                }
                if (sortKeyDefinition.stable != null) {
                    sortKeyDefinition.stable = sortKeyDefinition.stable.promote(offer, parent);
                }
                if (sortKeyDefinition.caseOrder != null) {
                    sortKeyDefinition.caseOrder = sortKeyDefinition.caseOrder.promote(offer, parent);
                }
                if (sortKeyDefinition.dataTypeExpression != null) {
                    sortKeyDefinition.dataTypeExpression = sortKeyDefinition.dataTypeExpression.promote(offer, parent);
                }
                if (sortKeyDefinition.language != null) {
                    sortKeyDefinition.language = sortKeyDefinition.language.promote(offer, parent);
                }
                if (sortKeyDefinition.collationName != null) {
                    sortKeyDefinition.collationName = sortKeyDefinition.collationName.promote(offer, parent);
                }
            }
            return this;
        }
    }

    /**
     * Test whether a given expression is one of the sort keys
     *
     * @param child the given expression
     * @return true if the given expression is one of the sort keys
     */

    public boolean isSortKey(Expression child) {
        for (SortKeyDefinition sortKeyDefinition : sortKeyDefinitions) {
            Expression exp = sortKeyDefinition.getSortKey();
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
     * @return a value such as Type.STRING, Type.BOOLEAN, Type.NUMBER, Type.NODE,
     *         or Type.ITEM (meaning not known in advance)
     */

    /*@NotNull*/
    public ItemType getItemType() {
        return select.getItemType();
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

    /*@NotNull*/
    public SequenceIterator iterate(XPathContext context) throws XPathException {

        SequenceIterator iter = select.iterate(context);
        if (iter instanceof EmptyIterator) {
            return iter;
        }

        AtomicComparer[] comps = comparators;
        if (comparators == null) {
            comps = new AtomicComparer[sortKeyDefinitions.length];
            for (int s = 0; s < sortKeyDefinitions.length; s++) {
                AtomicComparer comp = sortKeyDefinitions[s].getFinalComparator();
                if (comp == null) {
                    comp = sortKeyDefinitions[s].makeComparator(context);
                }
                comps[s] = comp;
            }
        }
        iter = new SortedIterator(context, iter, this, comps, sortKeyDefinitions[0].isSetContextForSortKey());
        ((SortedIterator) iter).setHostLanguage(getHostLanguage());
        return iter;
    }

    /**
     * Callback for evaluating the sort keys
     */

    public AtomicValue evaluateSortKey(int n, XPathContext c) throws XPathException {
        return (AtomicValue) sortKeyDefinitions[n].getSortKey().evaluateItem(c);
    }

    @Override
    public String toShortString() {
        return "sort(" + getBaseExpression().toShortString() + ")";
    }

//#ifdefined BYTECODE

    /**
     * Return the bytecode compiler for the Sort expression
     *
     * @return the relevant ExpressionCompiler
     */
    @Override
    public ExpressionCompiler getExpressionCompiler() {
        return new SortExpressionCompiler();
    }
//#endif

//#ifdefined STREAM

    /**
     * Get a class that supports streamed evaluation of this expression
     *
     * @return the relevant StreamingAdjunct, or null if none is available
     */
    @Override
    public StreamingAdjunct getStreamingAdjunct() {
        return new SortExpressionAdjunct();
    }

//#endif

    /**
     * Diagnostic print of expression structure. The abstract expression tree
     * is written to the supplied output destination.
     */

    public void explain(ExpressionPresenter out) {
        out.startElement("sort");
        out.startSubsidiaryElement("select");
        select.explain(out);
        out.endSubsidiaryElement();
        for (SortKeyDefinition sortKeyDefinition : sortKeyDefinitions) {
            out.startSubsidiaryElement("by");
            sortKeyDefinition.getSortKey().explain(out);
            out.endSubsidiaryElement();
        }
        out.endElement();
    }
}

