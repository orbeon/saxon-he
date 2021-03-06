////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
// Copyright (c) 2015 Saxonica Limited.
// This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
// If a copy of the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
// This Source Code Form is "Incompatible With Secondary Licenses", as defined by the Mozilla Public License, v. 2.0.
////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

package net.sf.saxon.expr;

import com.saxonica.ee.bytecode.ExpressionCompiler;
import com.saxonica.ee.bytecode.LetExpressionCompiler;
import com.saxonica.ee.stream.Sweep;
import com.saxonica.ee.stream.adjunct.LetExpressionAdjunct;
import com.saxonica.ee.stream.adjunct.LetExpressionAdjunctB;
import com.saxonica.ee.stream.adjunct.StreamingAdjunct;
import net.sf.saxon.evpull.EventIterator;
import net.sf.saxon.expr.instruct.DocumentInstr;
import net.sf.saxon.expr.instruct.GlobalVariable;
import net.sf.saxon.expr.instruct.TailCall;
import net.sf.saxon.expr.instruct.TailCallReturner;
import net.sf.saxon.expr.parser.*;
import net.sf.saxon.om.*;
import net.sf.saxon.trace.ExpressionPresenter;
import net.sf.saxon.trans.XPathException;
import net.sf.saxon.type.ItemType;
import net.sf.saxon.type.SchemaType;
import net.sf.saxon.value.Cardinality;
import net.sf.saxon.value.IntegerValue;
import net.sf.saxon.value.SequenceType;

import java.util.ArrayList;


/**
 * A LetExpression represents the XQuery construct let $x := expr return expr. It is used
 * also for XSLT local variables.
 */

public class LetExpression extends Assignation implements TailCallReturner {

    private int evaluationMode = ExpressionTool.UNDECIDED;
    private boolean needsEagerEvaluation = false;

    /**
     * Create a LetExpression
     */

    public LetExpression() {
        //System.err.println("let");
    }

    /**
     * Get a name identifying the kind of expression, in terms meaningful to a user.
     *
     * @return a name identifying the kind of expression, in terms meaningful to a user.
     *         The name will always be in the form of a lexical XML QName, and should match the name used
     *         in explain() output displaying the expression.
     */

    public String getExpressionName() {
        return "let";
    }

    public void setNeedsEagerEvaluation(boolean req) {
        this.needsEagerEvaluation = req;
    }

//    private void recomputeRefCount() {
//        if (refCount != FilterExpression.FILTERED) {
//            setRefCount(countReferences(this, this, false));
//        }
//    }
//
//    private static int countReferences(LetExpression let, Expression child, boolean inLoop) {
//        List<Integer> visits = new ArrayList<Integer>();
//        visits.add(0);
//        return countReferencesInternal(let, child, inLoop, visits);
//    }
//
//    private static int countReferencesInternal(LetExpression let, Expression child, boolean inLoop, List<Integer> visits) {
//        int total = 0;
//        for (Operand o : child.operands()) {
//            Expression g = o.getChildExpression();
//            boolean repeated = inLoop || o.isEvaluatedRepeatedly();
//            if (!repeated && !o.hasSameFocus() && ExpressionTool.dependsOnFocus(let.getSequence())) {
//                repeated = true;
//            }
//            if (g instanceof LocalVariableReference && ((LocalVariableReference) g).getBinding() == let) {
//                total += repeated ? 10 : 1;
//            } else {
//                int v = visits.get(0) + 1;
//                if (v > 100) {
//                    return 17; // enough; no point wasting more time on this
//                }
//                visits.set(0, v);
//                total += countReferencesInternal(let, g, repeated, visits);
//            }
//            if (total >= 3) {
//                break;
//            }
//        }
//        return total;
//    }
//
//    public void setRefCount(int refCount) {
//        this.refCount = refCount;
//    }

    @Override
    public void resetLocalStaticProperties() {
        super.resetLocalStaticProperties();
        references = new ArrayList<VariableReference>(); // bug 3233
    }

    /**
     * Type-check the expression. This also has the side-effect of counting the number of references
     * to the variable (treating references that occur within a loop specially)
     */

    /*@NotNull*/
    public Expression typeCheck(ExpressionVisitor visitor, ContextItemStaticInfo contextInfo) throws XPathException {

        // The order of events is critical here. First we ensure that the type of the
        // sequence expression is established. This is used to establish the type of the variable,
        // which in turn is required when type-checking the action part.

        getSequenceOp().typeCheck(visitor, contextInfo);

        RoleDiagnostic role = new RoleDiagnostic(RoleDiagnostic.VARIABLE, getVariableQName().getDisplayName(), 0);
        //role.setSourceLocator(this);
        setSequence(TypeChecker.strictTypeCheck(
                getSequence(), requiredType, role, visitor.getStaticContext()));
        final ItemType actualItemType = getSequence().getItemType();

        refineTypeInformation(actualItemType,
                              getSequence().getCardinality(),
                              getSequence() instanceof Literal ? ((Literal) getSequence()).getValue() : null,
                              getSequence().getSpecialProperties(), this);

//        boolean indexed = refCount == FilterExpression.FILTERED;
//        refCount = 0;
        getActionOp().typeCheck(visitor, contextInfo);
//        if (indexed) {
//            refCount = FilterExpression.FILTERED;
//        }
        return this;
    }


    /**
     * Determine whether this expression implements its own method for static type checking
     *
     * @return true - this expression has a non-trivial implementation of the staticTypeCheck()
     *         method
     */

    public boolean implementsStaticTypeCheck() {
        return true;
    }

    /**
     * Static type checking for let expressions is delegated to the expression itself,
     * and is performed on the "action" expression, to allow further delegation to the branches
     * of a conditional
     *
     *
     * @param req                 the required type
     * @param backwardsCompatible true if backwards compatibility mode applies
     * @param role                the role of the expression in relation to the required type
     * @param visitor             an expression visitor
     * @return the expression after type checking (perhaps augmented with dynamic type checking code)
     * @throws XPathException if failures occur, for example if the static type of one branch of the conditional
     *                        is incompatible with the required type
     */

    public Expression staticTypeCheck(SequenceType req,
                                      boolean backwardsCompatible,
                                      RoleDiagnostic role, ExpressionVisitor visitor)
            throws XPathException {
        setAction(TypeChecker.staticTypeCheck(getAction(), req, backwardsCompatible, role, visitor));
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
        Optimizer opt = getConfiguration().obtainOptimizer();

        // if this is a construct of the form "let $j := EXP return $j" replace it with EXP
        // Remarkably, people do write this, and it can also be produced by previous rewrites
        // Note that type checks will already have been added to the sequence expression

        if (getAction() instanceof VariableReference &&
                ((VariableReference) getAction()).getBinding() == this &&
                !ExpressionTool.changesXsltContext(getSequence())) {
            getSequenceOp().optimize(visitor, contextItemType);
            opt.trace("Eliminated trivial variable " + getVariableName(), getSequence());
            return getSequence();
        }

        if (getSequence() instanceof Literal) {
            // inline the variable: replace all references by the constant value
            // This relies on the fact that optimizing the action part will cause any references to be inlined
            opt.trace("Inlined constant variable " + getVariableName(), getSequence());
            return getAction().optimize(visitor, contextItemType);
        }

        // if this is an XSLT construct of the form <xsl:variable>text</xsl:variable>, try to replace
        // it by <xsl:variable select=""/>. This can be done if all the references to the variable use
        // its value as a string (rather than, say, as a node or as a boolean)
        if (getSequence() instanceof DocumentInstr && ((DocumentInstr) getSequence()).isTextOnly()) {
            // Ensure the list of references is accurate
            verifyReferences();
            // Check whether all uses of the variable are atomized or stringified
            if (allReferencesAreFlattened()) {
                Expression stringValueExpression = ((DocumentInstr) getSequence()).getStringValueExpression();
                stringValueExpression = stringValueExpression.typeCheck(visitor, contextItemType);
                setSequence(stringValueExpression);
                requiredType = SequenceType.SINGLE_UNTYPED_ATOMIC;
                adoptChildExpression(getSequence());
                refineTypeInformation(requiredType.getPrimaryType(), requiredType.getCardinality(), null, 0, this);
            }
        }

        // Removal of redundant variables, and inlining of variables that are only used once, depends on accurate
        // knowledge of all references to the variable. The problem is that obtaining this knowledge can be expensive:
        // see bug 2707. On the other hand, failing to do these optimizations is not fatal. So the general approach
        // is that we limit the time spent discovering the information, and we don't do the optimization unless
        // it is safe.

        // Typically on entry to optimize(), the typeCheck() method has already been called, and this has set up
        // a list of references. First we examine this list of references and remove any that are "dead", that is
        // they no longer have this LetExpression as an ancestor in the expression tree. This function also checks
        // whether any of these references are known to be in a loop, and returns true if so.

        hasLoopingReference |= removeDeadReferences();

        // If there are less than two references, and none is in a loop, then there is potential
        // for optimization. But we now need to be absolutely sure that we have an accurate list
        // of references.

        boolean considerRemoval = ((references != null && references.size() < 2) || getSequence() instanceof VariableReference) &&
                !isIndexedVariable && !hasLoopingReference && !needsEagerEvaluation;

        if (considerRemoval) {
            verifyReferences();
            // At this point the list of references is either accurate, or null
            considerRemoval = references != null;
        }

        if (considerRemoval && references.isEmpty()) {
            // variable is not used - no need to evaluate it
            getActionOp().optimize(visitor, contextItemType);
            opt.trace("Eliminated unused variable " + getVariableName(), getAction());
            return getAction();
        }

        // Don't inline context-dependent variables in a streamable template. See strmode011.
        // The reason for this is that a variable <xsl:variable><xsl:copy-of select="."/></xsl:variable>
        // can be evaluated in streaming mode, but an arbitrary expression using copy() inline can't (e.g.
        // if it appears in a path expression or as an operand of an arithmetic expression)

        if (considerRemoval && references.size() == 1 && ExpressionTool.dependsOnFocus(getSequence())) {
            if (visitor.isOptimizeForStreaming()) {
                considerRemoval = false;
            }
            // Disallow inlining if the focus of the variable reference differs from the containing binding.
            Expression child = references.get(0);
            Expression parent = child.getParentExpression();
            while (parent != null && parent != this) {
                Operand o = ExpressionTool.findOperand(parent, child);
                if (o == null || !o.hasSameFocus()) {
                    considerRemoval = false;
                    break;
                }
                child = parent;
                parent = child.getParentExpression();
            }
        }

        // Don't inline variables whose initializer might contain a call to xsl:result-document
        if (considerRemoval && references.size() == 1 && ExpressionTool.changesXsltContext(getSequence())) {
            considerRemoval = false;
        }

        if (considerRemoval && (references.size() == 1 || getSequence() instanceof Literal || getSequence() instanceof VariableReference)) {
            inlineReferences();
            references = null;
            return getAction().optimize(visitor, contextItemType);
        }

        int tries = 0;
        while (tries++ < 5) {
            Expression seq0 = getSequence();
            getSequenceOp().optimize(visitor, contextItemType);
            if (getSequence() instanceof Literal && !isIndexedVariable) {
                return getAction().optimize(visitor, contextItemType);
            }
            if (seq0 == getSequence()) {
                break;
            }
        }

        tries = 0;
        while (tries++ < 5) {
            Expression act0 = getAction();
            getActionOp().optimize(visitor, contextItemType);
            if (act0 == getAction()) {
                break;
            }
            if (!isIndexedVariable) {
                verifyReferences();
                if (references != null && references.size() < 2) {
                    if (references.isEmpty()) {
                        // We may have removed references to the variable; try again at eliminating this expression.
                        return optimize(visitor, contextItemType);
                    } else {
                        // there is one remaining reference; try again at inlining if it's not in a loop
                        if (!references.get(0).isInLoop()) {
                            return optimize(visitor, contextItemType);
                        }
                    }
                }
            }
        }

        // Don't use lazy evaluation for a variable that is referenced inside the "try" part of a contained try catch (XSLT3 test try-031)

        if (needsEagerEvaluation) {
            setEvaluationMode(ExpressionTool.eagerEvaluationMode(getSequence()));
        } else if (isIndexedVariable()) {
            setEvaluationMode(ExpressionTool.MAKE_CLOSURE);
        } else {
            setEvaluationMode(ExpressionTool.lazyEvaluationMode(getSequence()));
        }
        return this;
    }

    private void inlineReferences() {
        // Note that the list of references might include references that are no longer reachable on the tree.
        // We therefore take no action if (a) the parent of the reference is null, or (b) the reference is
        // not found among the children of its parent.
        for (VariableReference ref : references) {
            Expression parent = ref.getParentExpression();
            if (parent != null) {
                for (Operand o : parent.operands()) {
                    if (o.getChildExpression() == ref) {
                        o.setChildExpression(getSequence().copy(new RebindingMap()));
                        break;
                    }
                }
                ExpressionTool.resetStaticProperties(parent);
            }
        }
    }

    /**
     * Return the estimated cost of evaluating an expression. This is a very crude measure based
     * on the syntactic form of the expression (we have no knowledge of data values). We take
     * the cost of evaluating a simple scalar comparison or arithmetic expression as 1 (one),
     * and we assume that a sequence has length 5. The resulting estimates may be used, for
     * example, to reorder the predicates in a filter expression so cheaper predicates are
     * evaluated first.
     */
    @Override
    public int getCost() {
        return getSequence().getCost() + getAction().getCost();
    }


    /**
     * Determine whether all references to this variable are using the value either
     * (a) by atomizing it, or (b) by taking its string value. (This excludes usages
     * such as testing the existence of a node or taking the effective boolean value).
     *
     * @return true if all references are known to atomize (or stringify) the value,
     *         false otherwise. The value false may indicate "not known".
     */

    private boolean allReferencesAreFlattened() throws XPathException {
        if (references != null) {
            for (VariableReference ref : references) {
                if (!ref.isFlattened()) {
                    return false;
                }
            }
            return true;
        }
        return false;
    }

    /**
     * Determine whether this is a vacuous expression as defined in the XQuery update specification
     *
     * @return true if this expression is vacuous
     */

    public boolean isVacuousExpression() {
        return getAction().isVacuousExpression();
    }

    /**
     * Check that any elements and attributes constructed or returned by this expression are acceptable
     * in the content model of a given complex type. It's always OK to say yes, since the check will be
     * repeated at run-time. The process of checking element and attribute constructors against the content
     * model of a complex type also registers the type of content expected of those constructors, so the
     * static validation can continue recursively.
     */

    public void checkPermittedContents(SchemaType parentType, boolean whole) throws XPathException {
        getAction().checkPermittedContents(parentType, whole);
    }

    /**
     * For an expression that returns an integer or a sequence of integers, get
     * a lower and upper bound on the values of the integers that may be returned, from
     * static analysis. The default implementation returns null, meaning "unknown" or
     * "not applicable". Other implementations return an array of two IntegerValue objects,
     * representing the lower and upper bounds respectively. The values
     * UNBOUNDED_LOWER and UNBOUNDED_UPPER are used by convention to indicate that
     * the value may be arbitrarily large. The values MAX_STRING_LENGTH and MAX_SEQUENCE_LENGTH
     * are used to indicate values limited by the size of a string or the size of a sequence.
     *
     * @return the lower and upper bounds of integer values in the result, or null to indicate
     *         unknown or not applicable.
     */
    /*@Nullable*/
    @Override
    public IntegerValue[] getIntegerBounds() {
        return getAction().getIntegerBounds();
    }

    //#ifdefined STREAM

    /**
     * Get a class that supports streamed evaluation of this expression
     *
     * @return the relevant StreamingAdjunct, or null if none is available
     */
    @Override
    public StreamingAdjunct getStreamingAdjunct() {
        if (getAction().getPostureAndSweepIfKnown() != null && getAction().getSweep() == Sweep.CONSUMING) {
            return new LetExpressionAdjunctB();
        } else {
            return new LetExpressionAdjunct();
        }
    }

 //#endif

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
        return getAction().getImplementationMethod();
    }



    /**
     * Iterate over the result of the expression to return a sequence of items
     */

    /*@NotNull*/
    public SequenceIterator iterate(XPathContext context) throws XPathException {
        // minimize stack consumption by evaluating nested LET expressions iteratively
        LetExpression let = this;
        while (true) {
            Sequence val = let.eval(context);
            context.setLocalVariable(let.getLocalSlotNumber(), val);
            if (let.getAction() instanceof LetExpression) {
                let = (LetExpression) let.getAction();
            } else {
                break;
            }
        }
        return let.getAction().iterate(context);
    }

    /**
     * Iterate over the result of the expression to return a sequence of events
     */

    public EventIterator iterateEvents(XPathContext context) throws XPathException {
        // minimize stack consumption by evaluating nested LET expressions iteratively
        LetExpression let = this;
        while (true) {
            Sequence val = let.eval(context);
            context.setLocalVariable(let.getLocalSlotNumber(), val);
            if (let.getAction() instanceof LetExpression) {
                let = (LetExpression) let.getAction();
            } else {
                break;
            }
        }
        return let.getAction().iterateEvents(context);
    }


    /**
     * Evaluate the variable.
     *
     * @param context the dynamic evaluation context
     * @return the result of evaluating the expression that is bound to the variable
     */

    public Sequence eval(XPathContext context) throws XPathException {
        if (evaluationMode == ExpressionTool.UNDECIDED) {
            setEvaluationMode(ExpressionTool.lazyEvaluationMode(getSequence()));
        }
        int savedOutputState = context.getTemporaryOutputState();
        context.setTemporaryOutputState(StandardNames.XSL_VARIABLE);
        Sequence result = ExpressionTool.evaluate(getSequence(), evaluationMode, context, getNominalReferenceCount());
        context.setTemporaryOutputState(savedOutputState);
        return result;
    }

    /**
     * Evaluate the expression as a singleton
     */

    public Item evaluateItem(XPathContext context) throws XPathException {
        // minimize stack consumption by evaluating nested LET expressions iteratively
        LetExpression let = this;
        while (true) {
            Sequence val = let.eval(context);
            context.setLocalVariable(let.getLocalSlotNumber(), val);
            if (let.getAction() instanceof LetExpression) {
                let = (LetExpression) let.getAction();
            } else {
                break;
            }
        }
        return let.getAction().evaluateItem(context);
    }

    /**
     * Get the effective boolean value of the expression. This returns false if the value
     * is the empty sequence, a zero-length string, a number equal to zero, or the boolean
     * false. Otherwise it returns true.
     *
     * @param context The context in which the expression is to be evaluated
     * @return the effective boolean value
     * @throws net.sf.saxon.trans.XPathException
     *          if any dynamic error occurs evaluating the
     *          expression
     */

    public boolean effectiveBooleanValue(XPathContext context) throws XPathException {
        // minimize stack consumption by evaluating nested LET expressions iteratively
        LetExpression let = this;
        while (true) {
            Sequence val = let.eval(context);
            context.setLocalVariable(let.getLocalSlotNumber(), val);
            if (let.getAction() instanceof LetExpression) {
                let = (LetExpression) let.getAction();
            } else {
                break;
            }
        }
        return let.getAction().effectiveBooleanValue(context);
    }

    /**
     * Process this expression as an instruction, writing results to the current
     * outputter
     */

    public void process(XPathContext context) throws XPathException {
        // minimize stack consumption by evaluating nested LET expressions iteratively
        LetExpression let = this;
        while (true) {
            Sequence val = let.eval(context);
            context.setLocalVariable(let.getLocalSlotNumber(), val);
            if (let.getAction() instanceof LetExpression) {
                let = (LetExpression) let.getAction();
            } else {
                break;
            }
        }
        let.getAction().process(context);
    }


    /**
     * Determine the data type of the items returned by the expression, if possible
     *
     * @return one of the values Type.STRING, Type.BOOLEAN, Type.NUMBER, Type.NODE,
     *         or Type.ITEM (meaning not known in advance)
     */

    /*@NotNull*/
    public ItemType getItemType() {
        return getAction().getItemType();
    }

    /**
     * Determine the static cardinality of the expression
     */

    public int computeCardinality() {
        return getAction().getCardinality();
    }

    /**
     * Get the static properties of this expression (other than its type). The result is
     * bit-signficant. These properties are used for optimizations. In general, if
     * property bit is set, it is true, but if it is unset, the value is unknown.
     */

    public int computeSpecialProperties() {
        int props = getAction().getSpecialProperties();
        int seqProps = getSequence().getSpecialProperties();
        if ((seqProps & StaticProperty.NON_CREATIVE) == 0) {
            props &= ~StaticProperty.NON_CREATIVE;
        }
        return props;
    }

    /**
     * Mark tail function calls
     */

    public int markTailFunctionCalls(StructuredQName qName, int arity) {
        return ExpressionTool.markTailFunctionCalls(getAction(), qName, arity);
    }

    /**
     * Promote this expression if possible
     */

    public Expression promote(PromotionOffer offer) throws XPathException {
        Expression exp = offer.accept(this);
        if (exp != null) {
            return exp;
        } else {
            // pass the offer on to the sequence expression
            Expression seq2 = doPromotion(getSequence(), offer);
            if (seq2 != getSequence()) {
                // if we've extracted a global variable, it may need to be marked indexable
                if (seq2 instanceof VariableReference) {
                    Binding b = ((VariableReference) seq2).getBinding();
                    if (b instanceof GlobalVariable && isIndexedVariable) {
                        ((GlobalVariable) b).setIndexedVariable();
                    }
                }
                setSequence(seq2);
            }
            if (offer.action == PromotionOffer.EXTRACT_GLOBAL_VARIABLES) {
                setAction(doPromotion(getAction(), offer));
            } else if (offer.action == PromotionOffer.RANGE_INDEPENDENT ||
                    offer.action == PromotionOffer.FOCUS_INDEPENDENT) {
                // Pass the offer to the action expression after adding the variable bound by this let expression,
                // so that a subexpression must depend on neither variable if it is to be promoted
                Binding[] savedBindingList = offer.bindingList;
                offer.bindingList = extendBindingList(offer.bindingList);
                setAction(doPromotion(getAction(), offer));
                offer.bindingList = savedBindingList;
            }
            // if this results in the expression (let $x := $y return Z), replace all references to
            // to $x by references to $y in the Z part, and eliminate this LetExpression by
            // returning the action part.
            if (getSequence() instanceof VariableReference && !needsEagerEvaluation) {
                rebuildReferenceList(true);
                if (references != null) {
                    inlineReferences();
                    return getAction();
                }
            }
            references = null;
            return this;
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
        LetExpression let = new LetExpression();
        ExpressionTool.copyLocationInfo(this, let);
        let.isIndexedVariable = isIndexedVariable;
        let.hasLoopingReference = hasLoopingReference;
        let.setNeedsEagerEvaluation(needsEagerEvaluation);
        let.setVariableQName(variableName);
        let.setRequiredType(requiredType);
        let.setSequence(getSequence().copy(rebindings));
        Expression newAction = getAction().copy(rebindings);
        let.setAction(newAction);
        ExpressionTool.rebindVariableReferences(newAction, this, let);
        return let;
    }

    /**
     * ProcessLeavingTail: called to do the real work of this instruction.
     * The results of the instruction are written
     * to the current Receiver, which can be obtained via the Controller.
     *
     * @param context The dynamic context of the transformation, giving access to the current node,
     *                the current variables, etc.
     * @return null if the instruction has completed execution; or a TailCall indicating
     *         a function call or template call that is delegated to the caller, to be made after the stack has
     *         been unwound so as to save stack space.
     */

    public TailCall processLeavingTail(XPathContext context) throws XPathException {
        // minimize stack consumption by evaluating nested LET expressions iteratively
        LetExpression let = this;
        while (true) {
            Sequence val = let.eval(context);
            context.setLocalVariable(let.getLocalSlotNumber(), val);
            if (let.getAction() instanceof LetExpression) {
                let = (LetExpression) let.getAction();
            } else {
                break;
            }
        }
        if (let.getAction() instanceof TailCallReturner) {
            return ((TailCallReturner) let.getAction()).processLeavingTail(context);
        } else {
            let.getAction().process(context);
            return null;
        }
    }

    /**
     * Evaluate an updating expression, adding the results to a Pending Update List.
     * The default implementation of this method, which is used for non-updating expressions,
     * throws an UnsupportedOperationException
     *
     * @param context the XPath dynamic evaluation context
     * @param pul     the pending update list to which the results should be written
     */

    public void evaluatePendingUpdates(XPathContext context, PendingUpdateList pul) throws XPathException {
        // minimize stack consumption by evaluating nested LET expressions iteratively
        LetExpression let = this;
        while (true) {
            Sequence val = let.eval(context);
            context.setLocalVariable(let.getLocalSlotNumber(), val);
            if (let.getAction() instanceof LetExpression) {
                let = (LetExpression) let.getAction();
            } else {
                break;
            }
        }
        let.getAction().evaluatePendingUpdates(context, pul);
    }

//#ifdefined BYTECODE

    /**
     * Return the compiler of the Let expression
     *
     * @return the relevant ExpressionCompiler
     */
    @Override
    public ExpressionCompiler getExpressionCompiler() {
        return new LetExpressionCompiler();
    }
//#endif

    /**
     * The toString() method for an expression attempts to give a representation of the expression
     * in an XPath-like form, but there is no guarantee that the syntax will actually be true XPath.
     * In the case of XSLT instructions, the toString() method gives an abstracted view of the syntax
     *
     * @return a representation of the expression as a string
     */

    public String toString() {
        return "let $" + getVariableEQName() + " := " + getSequence().toString() +
                " return " + ExpressionTool.parenthesize(getAction());
    }

    /**
     * Produce a short string identifying the expression for use in error messages
     *
     * @return a short string, sufficient to identify the expression
     */
    @Override
    public String toShortString() {
        return "let $" + getVariableName() + " := ...";
    }

    /**
     * Diagnostic print of expression structure. The abstract expression tree
     * is written to the supplied output destination.
     */

    public void export(ExpressionPresenter out) throws XPathException {
        out.startElement("let", this);
        out.emitAttribute("var", variableName);
        out.emitAttribute("as", getSequence().getItemType().toString() +
                Cardinality.getOccurrenceIndicator(getSequence().getCardinality()));
        if (isIndexedVariable()) {
            out.emitAttribute("indexable", "true");
        }
        out.emitAttribute("slot", getLocalSlotNumber() + "");
        out.emitAttribute("eval", getEvaluationMode() + "");
        getSequence().export(out);
        getAction().export(out);
        out.endElement();
    }

    public void setEvaluationMode(int evaluationMode) {
        this.evaluationMode = evaluationMode;
    }

    public int getEvaluationMode() {
        return evaluationMode;
    }


}

