package net.sf.saxon.expr.flwor;

import net.sf.saxon.expr.*;
import net.sf.saxon.expr.parser.*;
import net.sf.saxon.functions.KeyFn;
import net.sf.saxon.functions.SystemFunction;
import net.sf.saxon.lib.NamespaceConstant;
import net.sf.saxon.om.StructuredQName;
import net.sf.saxon.trace.ExpressionPresenter;
import net.sf.saxon.trans.XPathException;
import net.sf.saxon.tree.util.FastStringBuffer;
import net.sf.saxon.type.BuiltInAtomicType;
import net.sf.saxon.type.ItemType;
import net.sf.saxon.type.TypeHierarchy;
import net.sf.saxon.value.SequenceType;

import java.util.ArrayList;
import java.util.List;

/**
 * A "for" clause in a FLWOR expression
 */
public class ForClause extends Clause {

    private LocalVariableBinding rangeVariable;
    /*@Nullable*/
    private LocalVariableBinding positionVariable;
    private Expression sequence;
    private boolean allowsEmpty;

    @Override
    public int getClauseKey() {
        return FOR;
    }

    public ForClause copy() {
        ForClause f2 = new ForClause();
        f2.rangeVariable = rangeVariable.copy();
        if (positionVariable != null) {
            f2.positionVariable = positionVariable.copy();
        }
        f2.sequence = sequence.copy();
        f2.allowsEmpty = allowsEmpty;
        return f2;
    }

    /**
     * Set the expression over which the "for" variable iterates
     *
     * @param sequence the expression over which the variable ranges
     */
    public void setSequence(Expression sequence) {
        this.sequence = sequence;
    }

    /**
     * Get the expression over which the "for" variable iterates
     *
     * @return the expression over which the variable ranges
     */

    public Expression getSequence() {
        return sequence;
    }

    /**
     * Set the range variable (the primary variable bound by this clause)
     *
     * @param binding the range variable
     */

    public void setRangeVariable(LocalVariableBinding binding) {
        rangeVariable = binding;
    }

    /**
     * Get the range variable (the primary variable bound by this clause)
     *
     * @return the range variable
     */

    public LocalVariableBinding getRangeVariable() {
        return rangeVariable;
    }

    /**
     * Set the position variable (the variable bound by the "at" clause)
     *
     * @param binding the position variable
     */

    public void setPositionVariable(/*@Nullable*/ LocalVariableBinding binding) {
        positionVariable = binding;
    }

    /**
     * Get the position variable (the variable bound by the "at" clause)
     *
     * @return the position variable
     */

    /*@Nullable*/
    public LocalVariableBinding getPositionVariable() {
        return positionVariable;
    }

    /**
     * Get the number of variables bound by this clause
     *
     * @return the number of variable bindings (1 or 2 depending on whether there is a position variable)
     */
    @Override
    public LocalVariableBinding[] getRangeVariables() {
        if (positionVariable == null) {
            return new LocalVariableBinding[]{rangeVariable};
        } else {
            return new LocalVariableBinding[]{rangeVariable, positionVariable};
        }
    }

    /**
     * Say whether the "allowing empty" option is present
     *
     * @param option true if the "allowing empty" option is present
     */

    public void setAllowingEmpty(boolean option) {
        allowsEmpty = option;
    }

    /**
     * Ask whether the "allowing empty" option is present
     *
     * @return true if the "allowing empty" option is present
     */

    public boolean isAllowingEmpty() {
        return allowsEmpty;
    }

    /**
     * Type-check the expression
     */
    @Override
    public void typeCheck(ExpressionVisitor visitor) throws XPathException {
        SequenceType decl = rangeVariable.getRequiredType();
        SequenceType sequenceType = SequenceType.makeSequenceType(
                decl.getPrimaryType(), StaticProperty.ALLOWS_ZERO_OR_MORE);
        RoleLocator role = new RoleLocator(RoleLocator.VARIABLE, rangeVariable.getVariableQName(), 0
        );
        //role.setSourceLocator(this);
        sequence = TypeChecker.strictTypeCheck(
                sequence, sequenceType, role, visitor.getStaticContext());
        // TODO: refine the type information for the variable
    }

    /**
     * Get a tuple stream that implements the functionality of this clause, taking its
     * input from another tuple stream which this clause modifies
     *
     * @param base    the input tuple stream
     * @param context the XPath dynamic context
     * @return the output tuple stream
     */
    @Override
    public TuplePull getPullStream(TuplePull base, XPathContext context) {
        if (allowsEmpty) {
            return new ForClauseOuterPull(base, this);
        } else {
            return new ForClausePull(base, this);
        }
    }

    /**
     * Get a push-mode tuple stream that implements the functionality of this clause, supplying its
     * output to another tuple stream
     *
     * @param destination the output tuple stream
     * @param context     the dynamic evaluation context
     * @return the push tuple stream that implements the functionality of this clause of the FLWOR
     *         expression
     */
    @Override
    public TuplePush getPushStream(TuplePush destination, XPathContext context) {
        if (allowsEmpty) {
            return new ForClauseOuterPush(destination, this);
        } else {
            return new ForClausePush(destination, this);
        }
    }

    /**
     * Convert where clause to a predicate.
     *
     * @param visitor         the expression visitor
     * @param contextItemType the item type of the context item
     * @param condition       the predicate to be added. This will always be a single term (never a composite condition
     *                        using "and"), as the where clause is split into separate terms before calling this method
     * @return true if the expression has been changed, that is, if the where clause has been converted
     * @throws XPathException if an error is encountered
     */

    public boolean addPredicate(ExpressionVisitor visitor, ExpressionVisitor.ContextItemType contextItemType, Expression condition) throws XPathException {
        Optimizer opt = visitor.getConfiguration().obtainOptimizer();
        boolean debug = opt.getConfiguration().isOptimizerTracing();

        // assert: condition has no dependency on context item. We removed any such dependency before we got here.

        final TypeHierarchy th = visitor.getConfiguration().getTypeHierarchy();
        Expression head = null;
        Expression selection = sequence;
        ItemType selectionContextItemType = (contextItemType == null ? null : contextItemType.itemType);
        if (sequence instanceof SlashExpression) {
            if (((SlashExpression) sequence).isAbsolute(th)) {
                head = ((SlashExpression) sequence).getFirstStep();
                selection = ((SlashExpression) sequence).getRemainingSteps();
                selectionContextItemType = head.getItemType(th);
            } else {
                SlashExpression p = ((SlashExpression) sequence).tryToMakeAbsolute(th);
                if (p != null) {
                    sequence = p;
                    head = ((SlashExpression) sequence).getFirstStep();
                    selection = ((SlashExpression) sequence).getRemainingSteps();
                    selectionContextItemType = head.getItemType(th);
                }
            }
        }

        boolean changed = false;

        // Process each term in the where clause independently
        Expression term = condition;

        if (positionVariable != null &&
                (term instanceof ValueComparison || term instanceof GeneralComparison)) {
            BinaryExpression comp = (BinaryExpression) term;
            Expression[] operands = comp.getOperands();
            for (int op = 0; op < 2; op++) {

                // If the where clause is a simple test on the position variable, for example
                //    for $x at $p in EXPR where $p = 5 return A
                // then absorb the where condition into a predicate, rewriting it as
                //    for $x in EXPR[position() = 5] return A
                // This takes advantage of the optimizations applied to positional filter expressions
                // Only do this if the sequence expression has not yet been changed, because
                // the position in a predicate after the first is different.
                Binding[] thisVar = {this.getRangeVariable()};
                if (positionVariable != null && operands[op] instanceof VariableReference && !changed) {
                    List varRefs = new ArrayList();
                    ExpressionTool.gatherVariableReferences(condition, positionVariable, varRefs);
                    if (varRefs.size() == 1 && varRefs.get(0) == operands[op] &&
                            !ExpressionTool.dependsOnFocus(operands[1 - op]) &&
                            !ExpressionTool.dependsOnVariable(operands[1 - op], thisVar)) {
                        FunctionCall position =
                                SystemFunction.makeSystemFunction("position", SimpleExpression.NO_ARGUMENTS);
                        Expression predicate = term.copy();
                        predicate.replaceSubExpression(((BinaryExpression) predicate).getOperands()[op], position);
                        if (debug) {
                            opt.trace("Replaced positional variable in predicate by position()");
                        }
                        selection = new FilterExpression(selection, predicate);
                        ExpressionTool.copyLocationInfo(predicate, selection);
                        ExpressionVisitor.ContextItemType cit = new ExpressionVisitor.ContextItemType(selectionContextItemType, true);
                        selection = visitor.typeCheck(selection, cit);
                        positionVariable = null;
                        changed = true;
                        break;
                    }
                }
            }
        }

        if (positionVariable == null) {
            Binding[] thisVar = {this.getRangeVariable()};
            if (opt.isVariableReplaceableByDot(term, thisVar)) {
                boolean useDotDirectly = opt.isVariableReplaceableByDot(term, thisVar);
                Expression replacement;
                // When rewriting the where expression as a filter, we have to replace references to the
                // range variable by references to the context item. If we can do this directly, we do. But
                // if the reference to the range variable occurs inside a predicate, or on the rhs of slash,
                // we have to bind a new variable to the context item. So for example "for $x in S where
                // T[abc = $x]" gets rewritten as "for $x in S[let $dot := . return T[abc = $dot]]"
                if (useDotDirectly) {
                    replacement = new ContextItemExpression();
                } else {
                    LetExpression let = new LetExpression();
                    let.setVariableQName(
                            new StructuredQName("saxon", NamespaceConstant.SAXON, "dot" + hashCode()));
                    let.setRequiredType(SequenceType.makeSequenceType(contextItemType.itemType, StaticProperty.EXACTLY_ONE));
                    let.setSequence(new ContextItemExpression());
                    let.setAction(term);
                    term = let;
                    replacement = new VariableReference(let);
                }

                PromotionOffer offer = new PromotionOffer(visitor.getConfiguration().obtainOptimizer());
                offer.action = PromotionOffer.INLINE_VARIABLE_REFERENCES;
                offer.bindingList = thisVar;
                offer.containingExpression = replacement;
                Expression newTerm = term.promote(offer, sequence);
                if (newTerm != null && offer.accepted) {
                    ExpressionVisitor.ContextItemType cit = new ExpressionVisitor.ContextItemType(sequence.getItemType(th), true);
                    Expression predicate = visitor.typeCheck(newTerm, cit);
                    // If the result of the predicate might be a number, wrap it in a call of boolean()
                    int rel = th.relationship(predicate.getItemType(th), BuiltInAtomicType.INTEGER);
                    if (rel != TypeHierarchy.DISJOINT) {
                        predicate = SystemFunction.makeSystemFunction("boolean", new Expression[]{predicate});
                        assert predicate != null;
                    }
                    selection = new FilterExpression(selection, predicate);
                    ExpressionTool.copyLocationInfo(predicate, selection);
                    cit = new ExpressionVisitor.ContextItemType(selectionContextItemType, true);
                    selection = visitor.typeCheck(selection, cit);
                    changed = true;
                }
            }

        }
        if (changed) {

            if (head == null) {
                sequence = selection;
            } else if (head instanceof RootExpression && selection instanceof KeyFn) {
                sequence = selection;
            } else {
                Expression path = ExpressionTool.makePathExpression(head, selection, false);
                if (!(path instanceof SlashExpression)) {
                    return changed;
                }
                ExpressionTool.copyLocationInfo(condition, path);
                Expression k = visitor.getConfiguration().obtainOptimizer().convertPathExpressionToKey((SlashExpression) path, visitor);
                if (k == null) {
                    sequence = path;
                } else {
                    sequence = k;
                }
                sequence = visitor.optimize(visitor.typeCheck(visitor.simplify(sequence), contextItemType), contextItemType);
            }
        }
        return changed;
    }

    /**
     * Process the subexpressions of this clause
     *
     * @param processor the expression processor used to process the subexpressions
     */
    @Override
    public void processSubExpressions(ExpressionProcessor processor) throws XPathException {
        sequence = processor.processExpression(sequence);
    }

    public void gatherVariableReferences(List references) {
        if (positionVariable != null) {
            ExpressionTool.gatherVariableReferences(sequence, (Binding) positionVariable, references);
        }
        ExpressionTool.gatherVariableReferences(sequence, (Binding) rangeVariable, references);
    }

    @Override
    public void gatherVariableReferences(final ExpressionVisitor visitor, Binding binding, List<VariableReference> references) {
        ExpressionTool.gatherVariableReferences(sequence, binding, references);
    }

    @Override
    public void refineVariableType(ExpressionVisitor visitor, List<VariableReference> references, Expression returnExpr) {
        final TypeHierarchy th = visitor.getConfiguration().getTypeHierarchy();
        final ItemType actualItemType = sequence.getItemType(th);
        for (VariableReference ref : references) {
            ref.refineVariableType(actualItemType,
                    (allowsEmpty ? StaticProperty.ALLOWS_ZERO_OR_ONE : StaticProperty.EXACTLY_ONE),
                    null, sequence.getSpecialProperties(), visitor);
        }
    }

    /**
     * Diagnostic print of expression structure. The abstract expression tree
     * is written to the supplied output destination.
     *
     * @param out the expression presenter used to display the structure
     */
    @Override
    public void explain(ExpressionPresenter out) {
        out.startElement("for");
        out.emitAttribute("var", getRangeVariable().getVariableQName().getDisplayName());
        LocalVariableBinding posVar = getPositionVariable();
        if (posVar != null) {
            out.emitAttribute("at", posVar.getVariableQName().getDisplayName());
        }
        sequence.explain(out);
        out.endElement();
    }

    public String toString() {
        FastStringBuffer fsb = new FastStringBuffer(FastStringBuffer.SMALL);
        fsb.append("for $");
        fsb.append(rangeVariable.getVariableQName().getDisplayName());
        fsb.append(' ');
        LocalVariableBinding posVar = getPositionVariable();
        if (posVar != null) {
            fsb.append("at $");
            fsb.append(posVar.getVariableQName().getDisplayName());
            fsb.append(' ');
        }
        fsb.append("in ");
        fsb.append(sequence.toString());
        return fsb.toString();
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