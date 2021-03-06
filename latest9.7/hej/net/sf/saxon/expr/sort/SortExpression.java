////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
// Copyright (c) 2015 Saxonica Limited.
// This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
// If a copy of the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
// This Source Code Form is "Incompatible With Secondary Licenses", as defined by the Mozilla Public License, v. 2.0.
////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

package net.sf.saxon.expr.sort;

import com.saxonica.ee.bytecode.ExpressionCompiler;
import com.saxonica.ee.bytecode.SortExpressionCompiler;
import com.saxonica.ee.stream.adjunct.SortExpressionAdjunct;
import com.saxonica.ee.stream.adjunct.StreamingAdjunct;
import net.sf.saxon.expr.*;
import net.sf.saxon.expr.parser.*;
import net.sf.saxon.om.SequenceIterator;
import net.sf.saxon.trace.ExpressionPresenter;
import net.sf.saxon.trans.XPathException;
import net.sf.saxon.tree.iter.EmptyIterator;
import net.sf.saxon.type.ItemType;
import net.sf.saxon.value.AtomicValue;
import net.sf.saxon.value.Cardinality;
import net.sf.saxon.value.SequenceType;

import java.util.Map;

/**
 * Expression equivalent to the imaginary syntax
 * expr sortby (sort-key)+
 */

public class SortExpression extends Expression
        implements SortKeyEvaluator {

    private Operand selectOp;
    private Operand sortOp;
    private transient AtomicComparer[] comparators = null;
    // created early if all comparators can be created statically
    // transient because Java RuleBasedCollator is not serializable

    /**
     * Create a sort expression
     *
     * @param select   the expression whose result is to be sorted
     * @param sortKeys the set of sort key definitions to be used, in major to minor order
     */

    public SortExpression(Expression select, SortKeyDefinitionList sortKeys) {
        selectOp = new Operand(this, select, OperandRole.FOCUS_CONTROLLING_SELECT);
        sortOp = new Operand(this, sortKeys, OperandRole.ATOMIC_SEQUENCE);
        adoptChildExpression(select);
        adoptChildExpression(sortKeys);
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
     * Get the operand representing the expresion being sorted
     */

    public Operand getBaseOperand() {
        return selectOp;
    }

    /**
     * Get the expression defining the sequence being sorted
     *
     * @return the expression whose result is to be sorted
     */

    public Expression getBaseExpression() {
        return getSelect();
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
        return operandList(selectOp, sortOp);
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
        PathMap.PathMapNodeSet target = getSelect().addToPathMap(pathMap, pathMapNodeSet);
        for (SortKeyDefinition sortKeyDefinition : getSortKeyDefinitionList()) {
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
        return target;
    }

    /**
     * Type-check the expression
     */

    /*@NotNull*/
    public Expression typeCheck(ExpressionVisitor visitor, ContextItemStaticInfo contextInfo) throws XPathException {

        selectOp.typeCheck(visitor, contextInfo);

        Expression select2 = getSelect();
        if (select2 != getSelect()) {
            adoptChildExpression(select2);
            setSelect(select2);
        }
        if (!Cardinality.allowsMany(select2.getCardinality())) {
            // exit now because otherwise the type checking of the sort key can cause spurious failures
            return select2;
        }
        ItemType sortedItemType = getSelect().getItemType();

        boolean allKeysFixed = true;
        for (SortKeyDefinition sortKeyDefinition : getSortKeyDefinitionList()) {
            if (!sortKeyDefinition.isFixed()) {
                allKeysFixed = false;
                break;
            }
        }

        if (allKeysFixed) {
            comparators = new AtomicComparer[getSortKeyDefinitionList().size()];
        }

        for (int i = 0; i < getSortKeyDefinitionList().size(); i++) {
            SortKeyDefinition sortKeyDef = getSortKeyDefinition(i);
            Expression sortKey = sortKeyDef.getSortKey();
            if (sortKeyDef.isSetContextForSortKey()) {
                ContextItemStaticInfo cit = new ContextItemStaticInfo(sortedItemType, false);
                sortKey = sortKey.typeCheck(visitor, cit);
            } else {
                sortKey = sortKey.typeCheck(visitor, contextInfo);
            }
            if (sortKeyDef.isBackwardsCompatible()) {
                sortKey = FirstItemExpression.makeFirstItemExpression(sortKey);
            } else {
                RoleDiagnostic role =
                        new RoleDiagnostic(RoleDiagnostic.INSTRUCTION, "xsl:sort/select", 0);
                role.setErrorCode("XTTE1020");
                sortKey = TypeChecker.staticTypeCheck(sortKey, SequenceType.OPTIONAL_ATOMIC, false, role, visitor);
                //sortKey = CardinalityChecker.makeCardinalityChecker(sortKey, StaticProperty.ALLOWS_ZERO_OR_ONE, role);
            }
            sortKeyDef.setSortKey(sortKey, sortKeyDef.isSetContextForSortKey());
            sortKeyDef.typeCheck(visitor, contextInfo);
            if (sortKeyDef.isFixed()) {
                AtomicComparer comp = sortKeyDef.makeComparator(
                        visitor.getStaticContext().makeEarlyEvaluationContext());
                sortKeyDef.setFinalComparator(comp);
                if (allKeysFixed) {
                    comparators[i] = comp;
                }
            }
            if (sortKeyDef.isSetContextForSortKey() && !ExpressionTool.dependsOnFocus(sortKey)) {
                visitor.getStaticContext().issueWarning(
                        "Sort key will have no effect because its value does not depend on the context item",
                        sortKey.getLocation());
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
        selectOp.optimize(visitor, contextItemType);

        // optimize the sort keys
        ContextItemStaticInfo cit;
        if (getSortKeyDefinition(0).isSetContextForSortKey()) {
            ItemType sortedItemType = getSelect().getItemType();
            cit = new ContextItemStaticInfo(sortedItemType, false);
        } else {
            cit = contextItemType;
        }
        for (SortKeyDefinition sortKeyDefinition : getSortKeyDefinitionList()) {
            Expression sortKey = sortKeyDefinition.getSortKey();
            sortKey = sortKey.optimize(visitor, cit);
            sortKeyDefinition.setSortKey(sortKey, true);
        }
        if (Cardinality.allowsMany(getSelect().getCardinality())) {
            return this;
        } else {
            return getSelect();
        }
    }

    /**
     * Copy an expression. This makes a deep copy.
     *
     * @return the copy of the original expression
     * @param rebindings
     */

    /*@NotNull*/
    public Expression copy(RebindingMap rebindings) {
        int len = getSortKeyDefinitionList().size();
        SortKeyDefinition[] sk2 = new SortKeyDefinition[len];
        for (int i = 0; i < len; i++) {
            sk2[i] = getSortKeyDefinition(i).copy(rebindings);
        }
        SortExpression se2 = new SortExpression(getSelect().copy(rebindings), new SortKeyDefinitionList(sk2));
        ExpressionTool.copyLocationInfo(this, se2);
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
     *
     * @param offer  details of the offer, for example the offer to move
     *               expressions that don't depend on the context to an outer level in
     *               the containing expression
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
            setSelect(doPromotion(getSelect(), offer));
            for (SortKeyDefinition sortKeyDefinition : getSortKeyDefinitionList()) {
                final Expression sk2 = sortKeyDefinition.getSortKey().promote(offer);
                sortKeyDefinition.setSortKey(sk2, true);
                for (Operand o : sortKeyDefinition.operands()) {
                    o.setChildExpression(o.getChildExpression().promote(offer));
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
        for (SortKeyDefinition sortKeyDefinition : getSortKeyDefinitionList()) {
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
        return getSelect().getCardinality();
    }

    /**
     * Determine the data type of the items returned by the expression, if possible
     *
     * @return a value such as Type.STRING, Type.BOOLEAN, Type.NUMBER, Type.NODE,
     *         or Type.ITEM (meaning not known in advance)
     */

    /*@NotNull*/
    public ItemType getItemType() {
        return getSelect().getItemType();
    }

    /**
     * Get the static properties of this expression (other than its type). The result is
     * bit-significant. These properties are used for optimizations. In general, if
     * property bit is set, it is true, but if it is unset, the value is unknown.
     */

    public int computeSpecialProperties() {
        int props = 0;
        if ((getSelect().getSpecialProperties() & StaticProperty.CONTEXT_DOCUMENT_NODESET) != 0) {
            props |= StaticProperty.CONTEXT_DOCUMENT_NODESET;
        }
        if ((getSelect().getSpecialProperties() & StaticProperty.SINGLE_DOCUMENT_NODESET) != 0) {
            props |= StaticProperty.SINGLE_DOCUMENT_NODESET;
        }
        if ((getSelect().getSpecialProperties() & StaticProperty.NON_CREATIVE) != 0) {
            props |= StaticProperty.NON_CREATIVE;
        }
        return props;
    }

    /**
     * An implementation of Expression must provide at least one of the methods evaluateItem(), iterate(), or process().
     * This method indicates which of these methods is provided directly. The other methods will always be available
     * indirectly, using an implementation that relies on one of the other methods.
     *
     * @return the implementation method, for example {@link #ITERATE_METHOD} or {@link #EVALUATE_METHOD} or
     * {@link #PROCESS_METHOD}
     */
    @Override
    public int getImplementationMethod() {
        return ITERATE_METHOD;
    }

    /**
     * Enumerate the results of the expression
     */

    /*@NotNull*/
    public SequenceIterator iterate(XPathContext context) throws XPathException {

        SequenceIterator iter = getSelect().iterate(context);
        if (iter instanceof EmptyIterator) {
            return iter;
        }

        AtomicComparer[] comps = comparators;
        if (comparators == null) {
            int len = getSortKeyDefinitionList().size();
            comps = new AtomicComparer[len];
            for (int s = 0; s < len; s++) {
                AtomicComparer comp = getSortKeyDefinition(s).getFinalComparator();
                if (comp == null) {
                    comp = getSortKeyDefinition(s).makeComparator(context);
                }
                comps[s] = comp;
            }
        }
        iter = new SortedIterator(context, iter, this, comps, getSortKeyDefinition(0).isSetContextForSortKey());
        ((SortedIterator) iter).setHostLanguage(getHostLanguage());
        return iter;
    }

    /**
     * Callback for evaluating the sort keys
     */

    public AtomicValue evaluateSortKey(int n, XPathContext c) throws XPathException {
        return (AtomicValue) getSortKeyDefinition(n).getSortKey().evaluateItem(c);
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

    public void export(ExpressionPresenter out) throws XPathException {
        out.startElement("sort", this);
        out.setChildRole("select");
        getSelect().export(out);
        getSortKeyDefinitionList().export(out);
        out.endElement();
    }

    public Expression getSelect() {
        return selectOp.getChildExpression();
    }

    public void setSelect(Expression select) {
        selectOp.setChildExpression(select);
    }

    public SortKeyDefinitionList getSortKeyDefinitionList() {
        return (SortKeyDefinitionList)sortOp.getChildExpression();
    }

    public SortKeyDefinition getSortKeyDefinition(int i) {
        return getSortKeyDefinitionList().getSortKeyDefinition(i);
    }

    public void setSortKeyDefinitionList(SortKeyDefinitionList skd) {
        sortOp.setChildExpression(skd);
    }
}

