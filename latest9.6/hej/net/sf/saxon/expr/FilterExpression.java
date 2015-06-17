////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
// Copyright (c) 2014 Saxonica Limited.
// This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
// If a copy of the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
// This Source Code Form is "Incompatible With Secondary Licenses", as defined by the Mozilla Public License, v. 2.0.
////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

package net.sf.saxon.expr;


import com.saxonica.ee.bytecode.ExpressionCompiler;
import com.saxonica.ee.bytecode.FilterExpressionCompiler;
import com.saxonica.ee.stream.adjunct.FilterExpressionAdjunct;
import net.sf.saxon.Configuration;
import net.sf.saxon.expr.instruct.Choose;
import net.sf.saxon.expr.parser.*;
import net.sf.saxon.functions.*;
import net.sf.saxon.lib.FeatureKeys;
import net.sf.saxon.lib.NamespaceConstant;
import net.sf.saxon.om.*;
import net.sf.saxon.pattern.*;
import net.sf.saxon.trace.ExpressionPresenter;
import net.sf.saxon.trans.XPathException;
import net.sf.saxon.tree.iter.EmptyIterator;
import net.sf.saxon.type.*;
import net.sf.saxon.value.*;
import net.sf.saxon.value.StringValue;

import java.math.BigInteger;


/**
 * A FilterExpression contains a base expression and a filter predicate, which may be an
 * integer expression (positional filter), or a boolean expression (qualifier)
 */

public final class FilterExpression extends Expression implements ContextSwitchingExpression {

    private Expression base;
    private Expression filter;
    private boolean filterIsPositional;         // true if the value of the filter might depend on
                                                // the context position
    private boolean filterIsSingletonBoolean;   // true if the filter expression always returns a single boolean
    private boolean filterIsIndependent;        // true if the filter expression does not
                                                // depend on the context item or position. (It may depend on last()).
    public static final int FILTERED = 10000;

    public final static OperandRole FILTER_PREDICATE =
            new OperandRole(OperandRole.USES_NEW_FOCUS | OperandRole.HIGHER_ORDER, OperandUsage.INSPECTION,  SequenceType.ANY_SEQUENCE);


    /**
     * Constructor
     *
     * @param base  The base expression to be filtered.
     * @param filter An expression defining the filter predicate
     */

    public FilterExpression(/*@NotNull*/ Expression base, /*@NotNull*/ Expression filter) {
        this.base = base;
        this.filter = filter;
        adoptChildExpression(base);
        adoptChildExpression(filter);
        base.setFiltered(true);
    }

    /**
     * Get a name identifying the kind of expression, in terms meaningful to a user.
     *
     * @return a name identifying the kind of expression, in terms meaningful to a user.
     *         The name will always be in the form of a lexical XML QName, and should match the name used
     *         in explain() output displaying the expression.
     */

    public String getExpressionName() {
        return "filter";
    }

    /**
     * Get the data type of the items returned
     *
     * @return an integer representing the data type
     */

    /*@NotNull*/
    public ItemType getItemType() {
        // special case the expression B[. instance of x]
        if (filter instanceof InstanceOfExpression &&
                ((InstanceOfExpression) filter).getBaseExpression() instanceof ContextItemExpression) {
            return ((InstanceOfExpression) filter).getRequiredItemType();
        }
        return base.getItemType();
    }

    /**
     * Get the base expression
     *
     * @return the base expression being filtered
     */

    public Expression getSelectExpression() {
        return base;
    }

    /**
     * @return the boolean of filter is positional
     * @see com.saxonica.ee.bytecode.FilterExpressionCompiler
     */
    public boolean isFilterIsPositional() {
        return filterIsPositional;
    }

    /**
     * Get the subexpression that is evaluated in the new context
     *
     * @return the subexpression evaluated in the context set by the controlling expression
     */

    public Expression getActionExpression() {
        return filter;
    }

    /**
     * Get the filter expression
     *
     * @return the expression acting as the filter predicate
     */

    public Expression getFilter() {
        return filter;
    }


    /**
     * Determine if the filter is positional
     *
     * @param th the Type Hierarchy (for cached access to type information)
     * @return true if the value of the filter depends on the position of the item against
     *         which it is evaluated
     */

    public boolean isPositional(TypeHierarchy th) {
        return isPositionalFilter(filter, th);
    }

    /**
     * Test if the filter always returns a singleton boolean.
     *
     * <p>This information is available only after typeCheck() has been called.</p>
     *
     * @return true if the filter is a simple boolean expression
     */

    public boolean isSimpleBooleanFilter() {
        return filterIsSingletonBoolean;
    }

    /**
     * Determine whether the filter is independent of the context item and position
     *
     * <p>This information is available only after typeCheck() has been called.</p>
     *
     * @return true if the filter is a numeric value that does not depend on the context item or position
     */

    public boolean isIndependentFilter() {
        return filterIsIndependent;
    }

    /**
     * Simplify an expression
     *
     * @param visitor the expression visitor
     * @return the simplified expression
     * @throws XPathException if any failure occurs
     */

    /*@NotNull*/
    public Expression simplify(ExpressionVisitor visitor) throws XPathException {

        base = visitor.simplify(base);
        filter = visitor.simplify(filter);

        // ignore the filter if the base expression is an empty sequence
        if (Literal.isEmptySequence(base)) {
            return base;
        }

        // check whether the filter is a constant true() or false()
        if (filter instanceof Literal && !(((Literal) filter).getValue() instanceof NumericValue)) {
            try {
                if (filter.effectiveBooleanValue(visitor.getStaticContext().makeEarlyEvaluationContext())) {
                    return base;
                } else {
                    return Literal.makeEmptySequence(getContainer());
                }
            } catch (XPathException e) {
                e.maybeSetLocation(this);
                throw e;
            }
        }

        // check whether the filter is [last()] (note, [position()=last()] is handled elsewhere)

        if (filter instanceof Last) {
            filter = new IsLastExpression(true);
            adoptChildExpression(filter);
        }

        return this;

    }

    /**
     * Type-check the expression
     *
     *
       param visitor         the expression visitor
     * @param contextInfo
 *  return the expression after type-checking (potentially modified to add run-time
     *         checks and/or conversions)
     */

    /*@NotNull*/
    public Expression typeCheck(ExpressionVisitor visitor, ContextItemStaticInfo contextInfo) throws XPathException {
        final TypeHierarchy th = visitor.getConfiguration().getTypeHierarchy();
        Expression base2 = visitor.typeCheck(base,  contextInfo);
        if (base2 != base) {
            base = base2;
            adoptChildExpression(base2);
        }
        base.setFiltered(true);
        if (Literal.isEmptySequence(base)) {
            return base;
        }

        Expression filter2 = visitor.typeCheck(filter, new ContextItemStaticInfo(base.getItemType(), false, base));
        if (filter2 != filter) {
            filter = filter2;
            adoptChildExpression(filter2);
        }

        // The filter expression usually doesn't need to be sorted

        filter2 = ExpressionTool.unsortedIfHomogeneous(filter, visitor.isOptimizeForStreaming());
        if (filter2 != filter) {
            filter = filter2;
            adoptChildExpression(filter2);
        }

        // detect head expressions (E[1]) and treat them specially

        if (Literal.isConstantOne(filter)) {
            Expression fie = FirstItemExpression.makeFirstItemExpression(base);
            ExpressionTool.copyLocationInfo(this, fie);
            return fie;
        }

        // determine whether the filter might depend on position
        filterIsPositional = isPositionalFilter(filter, th);

        // determine whether the filter always evaluates to a single boolean
        filterIsSingletonBoolean =
                filter.getCardinality() == StaticProperty.EXACTLY_ONE &&
                        filter.getItemType().equals(BuiltInAtomicType.BOOLEAN);

        // determine whether the filter expression is independent of the focus

        filterIsIndependent = (filter.getDependencies() & StaticProperty.DEPENDS_ON_FOCUS) == 0;

        visitor.resetStaticProperties();
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
        final Configuration config = visitor.getConfiguration();
        final Optimizer opt = config.obtainOptimizer();
        final boolean debug = config.getBooleanProperty(FeatureKeys.TRACE_OPTIMIZER_DECISIONS);
        final TypeHierarchy th = config.getTypeHierarchy();

        Expression start2 = visitor.optimize(base, contextItemType);
        if (start2 != base) {
            base = start2;
            adoptChildExpression(start2);
        }
        base.setFiltered(true);

        Expression originalFilter;
        try {
            originalFilter = filter.copy();
        } catch (UnsupportedOperationException err) {
            originalFilter = null;
        }
        ContextItemStaticInfo baseItemType = new ContextItemStaticInfo(base.getItemType(), false, base);
        Expression filter2 = filter.optimize(visitor, baseItemType);
        if (filter2 != filter) {
            filter = filter2;
            adoptChildExpression(filter2);
        }

        // The filter expression usually doesn't need to be sorted

        filter2 = ExpressionTool.unsortedIfHomogeneous(filter, visitor.isOptimizeForStreaming());
        if (filter2 != filter) {
            filter = filter2;
            adoptChildExpression(filter2);
        }

        // Rewrite child::X[last()] as child::X[empty(following-sibling::X)] - especially useful for patterns

        if (filter instanceof IsLastExpression && base instanceof AxisExpression &&
            ((AxisExpression) base).getAxis() == AxisInfo.CHILD) {
            NodeTest test = ((AxisExpression) base).getNodeTest();
            AxisExpression fs = new AxisExpression(AxisInfo.FOLLOWING_SIBLING, test);
            filter = SystemFunctionCall.makeSystemFunction("empty", new Expression[]{fs});
        }

        // if the result of evaluating the filter cannot include numeric values, then we can use
        // its effective boolean value

        ItemType filterType = filter.getItemType();
        if (!th.isSubType(filterType, BuiltInAtomicType.BOOLEAN)
                && th.relationship(filterType, BuiltInAtomicType.NUMERIC) == TypeHierarchy.DISJOINT) {
            BooleanFn f = (BooleanFn) SystemFunctionCall.makeSystemFunction("boolean", new Expression[]{filter});
            filter = visitor.optimize(f, baseItemType);
        }

        // the filter expression may have been reduced to a constant boolean by previous optimizations
        if (filter instanceof Literal && ((Literal) filter).getValue() instanceof BooleanValue) {
            if (((BooleanValue) ((Literal) filter).getValue()).getBooleanValue()) {
                if (debug) {
                    opt.trace("Redundant filter removed", base);
                }
                return base;
            } else {
                if (debug) {
                    opt.trace("Filter expression eliminated because predicate is always false",
                            Literal.makeEmptySequence(getContainer()));
                }
                return Literal.makeEmptySequence(getContainer());
            }
        }

        // determine whether the filter might depend on position
        filterIsPositional = isPositionalFilter(filter, th);
        filterIsSingletonBoolean =
                filter.getCardinality() == StaticProperty.EXACTLY_ONE &&
                        filter.getItemType().equals(BuiltInAtomicType.BOOLEAN);

        // determine whether the filter is indexable
        if (!filterIsPositional && !visitor.isOptimizeForStreaming()) {
            int isIndexable = opt.isIndexableFilter(filter);
            if (isIndexable == 0 && filter != originalFilter && originalFilter != null) {
                // perhaps the original filter was indexable; if so, revert to the original
                // this happens when [@a = 1] is rewritten as [some $x in @a satisfies $x eq 1]
                // TODO: this rollback mechanism is very unsatisfactory. Better: make the some expression indexable!
                int origIndexable = opt.isIndexableFilter(originalFilter);
                if (origIndexable != 0) {
                    isIndexable = origIndexable;
                    filter = originalFilter;
                    adoptChildExpression(originalFilter);
                }
            }
            // If the filter is indexable consider creating a key, or an indexed filter expression
            // (This happens in Saxon-EE only)
            if (isIndexable != 0) {
                boolean contextIsDoc = contextItemType != null && contextItemType.getItemType() != ErrorType.getInstance() &&
                        th.isSubType(contextItemType.getItemType(), NodeKindTest.DOCUMENT);
                Expression f = opt.tryIndexedFilter(this, visitor, isIndexable > 0, contextIsDoc);
                if (f != this) {
                    return f.typeCheck(visitor, contextItemType).optimize(visitor, contextItemType);
                }
            }
        }

        // if the filter is positional, try changing f[a and b] to f[a][b] to increase
        // the chances of finishing early.

        if (filterIsPositional &&
                filter instanceof BooleanExpression &&
                ((BooleanExpression) filter).operator == Token.AND) {
            BooleanExpression bf = (BooleanExpression) filter;
            if (isExplicitlyPositional(bf.operand0) &&
                    !isExplicitlyPositional(bf.operand1)) {
                Expression p0 = forceToBoolean(bf.operand0);
                Expression p1 = forceToBoolean(bf.operand1);
                FilterExpression f1 = new FilterExpression(base, p0);
                ExpressionTool.copyLocationInfo(this, f1);
                FilterExpression f2 = new FilterExpression(f1, p1);
                ExpressionTool.copyLocationInfo(this, f2);
                if (debug) {
                    opt.trace("Composite filter replaced by nested filter expressions", f2);
                }
                return visitor.optimize(f2, contextItemType);
            }
            if (isExplicitlyPositional(bf.operand1) &&
                    !isExplicitlyPositional(bf.operand0)) {
                Expression p0 = forceToBoolean(bf.operand0);
                Expression p1 = forceToBoolean(bf.operand1);
                FilterExpression f1 = new FilterExpression(base, p1);
                ExpressionTool.copyLocationInfo(this, f1);
                FilterExpression f2 = new FilterExpression(f1, p0);
                ExpressionTool.copyLocationInfo(this, f2);
                if (debug) {
                    opt.trace("Composite filter replaced by nested filter expressions", f2);
                }
                return visitor.optimize(f2, contextItemType);
            }
        }

        if (filter instanceof IsLastExpression &&
                ((IsLastExpression) filter).getCondition()) {

            if (base instanceof Literal) {
                filter = Literal.makeLiteral(new Int64Value(((Literal) base).getValue().getLength()), getContainer());
            } else {
                return new LastItemExpression(base);
            }
        }

        Expression subsequence = tryToRewritePositionalFilter(visitor);
        if (subsequence != null) {
            if (debug) {
                opt.trace("Rewrote Filter Expression as:", subsequence);
            }
            ExpressionTool.copyLocationInfo(this, subsequence);
            return subsequence.simplify(visitor)
                    .typeCheck(visitor, contextItemType)
                    .optimize(visitor, contextItemType);
        }

        // If any subexpressions within the filter are not dependent on the focus,
        // promote them: this causes them to be evaluated once, outside the filter
        // expression. Note: we do this even if the filter is numeric, because it ensures that
        // the subscript is pre-evaluated, allowing direct indexing into the sequence.

        PromotionOffer offer = new PromotionOffer(opt);
        offer.action = PromotionOffer.FOCUS_INDEPENDENT;
        offer.promoteDocumentDependent = (base.getSpecialProperties() & StaticProperty.CONTEXT_DOCUMENT_NODESET) != 0;
        offer.containingExpression = this;
        filter2 = doPromotion(filter, offer);
        if (filter2 != filter) {
            filter = filter2;
            adoptChildExpression(filter2);
        }

        if (offer.containingExpression instanceof LetExpression) {
            if (debug) {
                opt.trace("Subexpression extracted from filter because independent of context", offer.containingExpression);
            }
            offer.containingExpression = visitor.optimize(offer.containingExpression, contextItemType);
        }
        Expression result = offer.containingExpression;

        if (result instanceof FilterExpression) {
            final Sequence sequence = ((FilterExpression) result).tryEarlyEvaluation(visitor);
            if (sequence != null) {
                GroundedValue value = SequenceTool.toGroundedValue(sequence);
                return Literal.makeLiteral(value, getContainer());
            }
        }
        return result;
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
    @Override
    public IntegerValue[] getIntegerBounds() {
        return base.getIntegerBounds();
    }


    private Sequence tryEarlyEvaluation(ExpressionVisitor visitor) throws XPathException {
        // Attempt early evaluation of a filter expression if the base sequence is constant and the
        // filter depends only on the context. (This can't be done if, for example, the predicate uses
        // local variables, even variables declared within the predicate)
        try {
            if (base instanceof Literal &&
                    !ExpressionTool.refersToVariableOrFunction(filter) &&
                    (filter.getDependencies() & ~StaticProperty.DEPENDS_ON_FOCUS) == 0) {
                XPathContext context = visitor.getStaticContext().makeEarlyEvaluationContext();
                return SequenceExtent.makeSequenceExtent(iterate(context));
            }
        } catch (Exception e) {
            // can happen for a variety of reasons, for example the filter references a global parameter,
            // references the doc() function, etc.
            return null;
        }
        return null;
    }

    /**
     * Add a representation of this expression to a PathMap. The PathMap captures a map of the nodes visited
     * by an expression in a source tree.
     *
     * @param pathMap        the PathMap to which the expression should be added
     * @param pathMapNodeSet the PathMapNodeSet to which the paths embodied in this expression should be added
     * @return the pathMapNode representing the focus established by this expression, in the case where this
     *         expression is the first operand of a path expression or filter expression
     */

    public PathMap.PathMapNodeSet addToPathMap(PathMap pathMap, PathMap.PathMapNodeSet pathMapNodeSet) {
        PathMap.PathMapNodeSet target = base.addToPathMap(pathMap, pathMapNodeSet);
        filter.addToPathMap(pathMap, target);
        return target;
    }

    /**
     * Construct an expression that obtains the effective boolean value of a given expression,
     * by wrapping it in a call of the boolean() function
     *
     *
     * @param in     the given expression
     * @return an expression that wraps the given expression in a call to the fn:boolean() function
     */

    private static Expression forceToBoolean(Expression in) {
        if (in.getItemType().getPrimitiveType() == StandardNames.XS_BOOLEAN) {
            return in;
        }
        return SystemFunctionCall.makeSystemFunction("boolean", new Expression[]{in});
    }

    /**
     * Attempt to rewrite a filter expression whose predicate is a test of the form
     * [position() op expr] as a call on functions such as subsequence, remove, or saxon:itemAt
     *
     * @param visitor the current expression visitor
     * @return the rewritten expression if a rewrite was possible, or null otherwise
     * @throws XPathException if an error occurs
     */

    private Expression tryToRewritePositionalFilter(ExpressionVisitor visitor) throws XPathException {
        if (visitor.isOptimizeForStreaming()) {
            // TODO: we're suppressing these optimizations because they generate expressions that
            // can't currently be streamed. But in principle the optimizations are still worth doing.
            return null;
        }
        TypeHierarchy th = visitor.getConfiguration().getTypeHierarchy();
        if (filter instanceof Literal) {
            GroundedValue val = ((Literal) filter).getValue();
            if (val instanceof NumericValue) {
                if (((NumericValue) val).isWholeNumber()) {
                    long lvalue = ((NumericValue) val).longValue();
                    if (lvalue <= 0) {
                        return Literal.makeEmptySequence(getContainer());
                    } else if (lvalue == 1) {
                        return FirstItemExpression.makeFirstItemExpression(base);
                    } else {
                        return new SubscriptExpression(base, filter);
                    }
                } else {
                    return Literal.makeEmptySequence(getContainer());
                }
            } else {
                return ExpressionTool.effectiveBooleanValue(val.iterate()) ? base : Literal.makeEmptySequence(getContainer());
            }
        }
        if (th.isSubType(filter.getItemType(), BuiltInAtomicType.NUMERIC) &&
                !Cardinality.allowsMany(filter.getCardinality()) &&
                (filter.getDependencies() & StaticProperty.DEPENDS_ON_FOCUS) == 0) {
            return new SubscriptExpression(base, filter);
        }
        if (filter instanceof ComparisonExpression) {
            VendorFunctionLibrary lib = visitor.getConfiguration().getVendorFunctionLibrary();
            StaticContext env = visitor.getStaticContext();
            Expression[] operands = ((ComparisonExpression) filter).getOperands();
            int operator = ((ComparisonExpression) filter).getSingletonOperator();
            Expression comparand;
            if (operands[0] instanceof Position
                    && th.isSubType(operands[1].getItemType(), BuiltInAtomicType.NUMERIC)) {
                comparand = operands[1];
            } else if (operands[1] instanceof Position
                    && th.isSubType(operands[0].getItemType(), BuiltInAtomicType.NUMERIC)) {
                comparand = operands[0];
                operator = Token.inverse(operator);
            } else {
                return null;
            }

            if (ExpressionTool.dependsOnFocus(comparand)) {
                return null;
            }

            int card = comparand.getCardinality();
            if (Cardinality.allowsMany(card)) {
                return null;
            }

            // If the comparand might be an empty sequence, do the base rewrite and then wrap the
            // rewritten expression EXP in "let $n := comparand if exists($n) then EXP else ()
            if (Cardinality.allowsZero(card)) {
                LetExpression let = new LetExpression();
                let.setRequiredType(SequenceType.makeSequenceType(comparand.getItemType(), card));
                let.setVariableQName(new StructuredQName("pp", NamespaceConstant.SAXON, "pp" + let.hashCode()));
                let.setSequence(comparand);
                comparand = new LocalVariableReference(let);
                LocalVariableReference existsArg = new LocalVariableReference(let);
                Exists exists = (Exists) SystemFunctionCall.makeSystemFunction("exists", new Expression[]{existsArg});
                Expression rewrite = tryToRewritePositionalFilterSupport(base, comparand, operator, th, lib, env);
                if (rewrite == null) {
                    return this;
                }
                Expression choice = Choose.makeConditional(exists, rewrite);
                let.setAction(choice);
                return let;
            } else {
                return tryToRewritePositionalFilterSupport(base, comparand, operator, th, lib, env);
            }
        } else if (filter instanceof IntegerRangeTest) {
            // rewrite SEQ[position() = N to M]
            // => let $n := N return subsequence(SEQ, $n, (M - ($n - 1))
            // (precise form is optimized for the case where $n is a literal, especially N = 1)
            Expression val = ((IntegerRangeTest) filter).getValueExpression();
            if (!(val instanceof Position)) {
                return null;
            }
            Expression min = ((IntegerRangeTest) filter).getMinValueExpression();
            Expression max = ((IntegerRangeTest) filter).getMaxValueExpression();

            if (ExpressionTool.dependsOnFocus(min)) {
                return null;
            }
            if (ExpressionTool.dependsOnFocus(max)) {
                if (max instanceof Last) {
                    return SystemFunctionCall.makeSystemFunction("subsequence", new Expression[]{base, min});
                } else {
                    return null;
                }
            }

            LetExpression let = new LetExpression();
            let.setRequiredType(SequenceType.SINGLE_INTEGER);
            let.setVariableQName(new StructuredQName("nn", NamespaceConstant.SAXON, "nn" + let.hashCode()));
            let.setSequence(min);
            min = new LocalVariableReference(let);
            LocalVariableReference min2 = new LocalVariableReference(let);
            Expression minMinusOne = new ArithmeticExpression(
                    min2, Token.MINUS, Literal.makeLiteral(Int64Value.makeIntegerValue(1), getContainer()));
            Expression length = new ArithmeticExpression(max, Token.MINUS, minMinusOne);
            Subsequence subs = (Subsequence) SystemFunctionCall.makeSystemFunction(
                    "subsequence", new Expression[]{base, min, length});
            let.setAction(subs);
            return let;

        } else {
            return null;
        }
    }

    private static Expression tryToRewritePositionalFilterSupport(
            Expression start, Expression comparand, int operator,
            TypeHierarchy th, VendorFunctionLibrary lib, StaticContext env)
            throws XPathException {
        if (th.isSubType(comparand.getItemType(), BuiltInAtomicType.INTEGER)) {
            switch (operator) {
                case Token.FEQ: {
                    if (Literal.isConstantOne(comparand)) {
                        return FirstItemExpression.makeFirstItemExpression(start);
                    } else if (comparand instanceof Literal && ((IntegerValue) ((Literal) comparand).getValue()).asBigInteger().compareTo(BigInteger.ZERO) <= 0) {
                        return Literal.makeEmptySequence(start.getContainer());
                    } else {
                        return new SubscriptExpression(start, comparand);
                    }
                }
                case Token.FLT: {

                    Expression[] args = new Expression[3];
                    args[0] = start;
                    args[1] = Literal.makeLiteral(Int64Value.makeIntegerValue(1), start.getContainer());
                    if (Literal.isAtomic(comparand)) {
                        long n = ((NumericValue) ((Literal) comparand).getValue()).longValue();
                        args[2] = Literal.makeLiteral(Int64Value.makeIntegerValue(n - 1), start.getContainer());
                    } else {
                        args[2] = new ArithmeticExpression(
                                comparand, Token.MINUS, Literal.makeLiteral(Int64Value.makeIntegerValue(1), start.getContainer()));
                    }
                    return SystemFunctionCall.makeSystemFunction("subsequence", args);
                }
                case Token.FLE: {
                    Expression[] args = new Expression[3];
                    args[0] = start;
                    args[1] = Literal.makeLiteral(Int64Value.makeIntegerValue(1), start.getContainer());
                    args[2] = comparand;
                    return SystemFunctionCall.makeSystemFunction("subsequence", args);
                }
                case Token.FNE: {
                    return SystemFunctionCall.makeSystemFunction("remove", new Expression[]{start, comparand});
                }
                case Token.FGT: {
                    Expression[] args = new Expression[2];
                    args[0] = start;
                    if (Literal.isAtomic(comparand)) {
                        long n = ((NumericValue) ((Literal) comparand).getValue()).longValue();
                        args[1] = Literal.makeLiteral(Int64Value.makeIntegerValue(n + 1), start.getContainer());
                    } else {
                        args[1] = new ArithmeticExpression(
                                comparand, Token.PLUS, Literal.makeLiteral(Int64Value.makeIntegerValue(1), start.getContainer()));
                    }
                    return SystemFunctionCall.makeSystemFunction("subsequence", args);
                }
                case Token.FGE: {
                    return SystemFunctionCall.makeSystemFunction("subsequence", new Expression[]{start, comparand});
                }
                default:
                    throw new IllegalArgumentException("operator");
            }

        } else {
            // the comparand is not known statically to be an integer
            switch (operator) {
                case Token.FEQ: {
                    return new SubscriptExpression(start, comparand);
                }
                case Token.FLT: {
                    // rewrite SEQ[position() lt V] as
                    // let $N := V return subsequence(SEQ, 1, if (is-whole-number($N)) then $N-1 else floor($N)))
                    LetExpression let = new LetExpression();
                    let.setRequiredType(SequenceType.makeSequenceType(
                            comparand.getItemType(), StaticProperty.ALLOWS_ONE));
                    let.setVariableQName(new StructuredQName("pp", NamespaceConstant.SAXON, "pp" + let.hashCode()));
                    let.setSequence(comparand);
                    LocalVariableReference isWholeArg = new LocalVariableReference(let);
                    LocalVariableReference arithArg = new LocalVariableReference(let);
                    LocalVariableReference floorArg = new LocalVariableReference(let);
                    Expression isWhole = lib.makeSaxonFunction(
                            "is-whole-number", new Expression[]{isWholeArg}, env, start.getContainer());
                    Expression minusOne = new ArithmeticExpression(
                            arithArg, Token.MINUS, Literal.makeLiteral(Int64Value.makeIntegerValue(1), start.getContainer()));
                    Floor floor = (Floor) SystemFunctionCall.makeSystemFunction(
                            "floor", new Expression[]{floorArg});
                    Expression choice = Choose.makeConditional(isWhole, minusOne, floor);
                    Subsequence subs = (Subsequence) SystemFunctionCall.makeSystemFunction(
                            "subsequence", new Expression[]{start, Literal.makeLiteral(Int64Value.makeIntegerValue(1), start.getContainer()), choice});
                    let.setAction(subs);
                    //decl.fixupReferences(let);
                    return let;
                }
                case Token.FLE: {
                    Floor floor = (Floor) SystemFunctionCall.makeSystemFunction(
                            "floor", new Expression[]{comparand});
                    return SystemFunctionCall.makeSystemFunction(
                            "subsequence", new Expression[]{start, Literal.makeLiteral(Int64Value.makeIntegerValue(1), start.getContainer()), floor});
                }
                case Token.FNE: {
                    // rewrite SEQ[position() ne V] as
                    // let $N := V return remove(SEQ, if (is-whole-number($N)) then xs:integer($N) else 0)
                    LetExpression let = new LetExpression();
                    ExpressionTool.copyLocationInfo(start, let);
                    let.setRequiredType(SequenceType.makeSequenceType(
                            comparand.getItemType(), StaticProperty.ALLOWS_ONE));
                    let.setVariableQName(new StructuredQName("pp", NamespaceConstant.SAXON, "pp" + let.hashCode()));
                    let.setSequence(comparand);
                    LocalVariableReference isWholeArg = new LocalVariableReference(let);
                    LocalVariableReference castArg = new LocalVariableReference(let);
                    Expression isWhole = lib.makeSaxonFunction(
                            "is-whole-number", new Expression[]{isWholeArg}, env, start.getContainer());
                    ExpressionTool.copyLocationInfo(start, isWhole);
                    Expression cast = new CastExpression(castArg, BuiltInAtomicType.INTEGER, false);
                    ExpressionTool.copyLocationInfo(start, cast);
                    Expression choice = Choose.makeConditional(
                            isWhole, cast, Literal.makeLiteral(Int64Value.makeIntegerValue(0), start.getContainer()));
                    Remove rem = (Remove) SystemFunctionCall.makeSystemFunction(
                            "remove", new Expression[]{start, choice});
                    let.setAction(rem);
                    return let;
                }
                case Token.FGT: {
                    // rewrite SEQ[position() gt V] as
                    // let $N := V return subsequence(SEQ, if (is-whole-number($N)) then $N+1 else ceiling($N)))
                    LetExpression let = new LetExpression();
                    let.setRequiredType(SequenceType.makeSequenceType(
                            comparand.getItemType(), StaticProperty.ALLOWS_ONE));
                    let.setVariableQName(new StructuredQName("pp", NamespaceConstant.SAXON, "pp" + let.hashCode()));
                    let.setSequence(comparand);
                    LocalVariableReference isWholeArg = new LocalVariableReference(let);
                    LocalVariableReference arithArg = new LocalVariableReference(let);
                    LocalVariableReference ceilingArg = new LocalVariableReference(let);
                    Expression isWhole = lib.makeSaxonFunction(
                            "is-whole-number", new Expression[]{isWholeArg}, env, start.getContainer());
                    Expression plusOne = new ArithmeticExpression(
                            arithArg, Token.PLUS, Literal.makeLiteral(Int64Value.makeIntegerValue(1), start.getContainer()));
                    Ceiling ceiling = (Ceiling) SystemFunctionCall.makeSystemFunction(
                            "ceiling", new Expression[]{ceilingArg});
                    Expression choice = Choose.makeConditional(isWhole, plusOne, ceiling);
                    Subsequence subs = (Subsequence) SystemFunctionCall.makeSystemFunction(
                            "subsequence", new Expression[]{start, choice});
                    let.setAction(subs);
                    return let;
                }
                case Token.FGE: {
                    // rewrite SEQ[position() ge V] => subsequence(SEQ, ceiling(V))
                    Ceiling ceiling = (Ceiling) SystemFunctionCall.makeSystemFunction(
                            "ceiling", new Expression[]{comparand});
                    return SystemFunctionCall.makeSystemFunction(
                            "subsequence", new Expression[]{start, ceiling});
                }
                default:
                    throw new IllegalArgumentException("operator");
            }
        }
    }

    /**
     * Promote this expression if possible
     *
     * @param offer  details of the promotion that is possible
     * @param parent the parent of this expression in the expression tree
     * @return the promoted expression (or the original expression, unchanged)
     */

    public Expression promote(PromotionOffer offer, Expression parent) throws XPathException {
        Expression exp = offer.accept(parent, this);
        if (exp != null) {
            return exp;
        } else {
            if ((offer.action == PromotionOffer.RANGE_INDEPENDENT) && (base instanceof FilterExpression)) {
                // Try to handle the situation where we have (EXP[PRED1])[PRED2], and
                // EXP[PRED2] does not depend on the range variable, but PRED1 does
                TypeHierarchy th = offer.getOptimizer().getConfiguration().getTypeHierarchy();
                FilterExpression newfe = promoteIndependentPredicates(offer.bindingList, offer.getOptimizer(), th);
                if (newfe != this) {
                    return newfe.promote(offer, parent);
                }
            }
            base = doPromotion(base, offer);
                // Don't pass on requests to the filter. We could pass them on, but only after augmenting
                // them to say we are interested in subexpressions that don't depend on either the
                // outer context or the inner context.
            return this;
        }
    }

    /**
     * Replace this expression by a simpler expression that delivers the results without regard
     * to order.
     *
     * @param retainAllNodes set to true if the result must contain exactly the same nodes as the
     *                       original; set to false if the result can eliminate (or introduce) duplicates.
     * @param forStreaming  set to true if optimizing for streaming
     */
    @Override
    public Expression unordered(boolean retainAllNodes, boolean forStreaming) throws XPathException {
        if (!filterIsPositional) {
            base = base.unordered(retainAllNodes, forStreaming);
        }
        return this;
    }

    /**
     * Rearrange a filter expression so that predicates that are independent of a given
     * set of range variables come first, allowing them to be promoted along with the base
     * expression
     *
     * @param bindings the given set of range variables
     * @param opt the Optimizer
     * @param th       the type hierarchy cache
     * @return the expression after promoting independent predicates
     */

    private FilterExpression promoteIndependentPredicates(Binding[] bindings, Optimizer opt, TypeHierarchy th) {
        if (!ExpressionTool.dependsOnVariable(base, bindings)) {
            return this;
        }
        if (isPositional(th)) {
            return this;
        }
        if (base instanceof FilterExpression) {
            FilterExpression fe = (FilterExpression) base;
            if (fe.isPositional(th)) {
                return this;
            }
            if (!ExpressionTool.dependsOnVariable(fe.filter, bindings)) {
                return this;
            }
            if (!ExpressionTool.dependsOnVariable(filter, bindings)) {
                FilterExpression result = new FilterExpression(
                        new FilterExpression(fe.base, filter).promoteIndependentPredicates(bindings, opt, th),
                        fe.filter);
                opt.trace("Reordered filter predicates:", result);
                return result;
            }
        }
        return this;
    }

    /**
     * Determine whether an expression, when used as a filter, is potentially positional;
     * that is, where it either contains a call on position() or last(), or where it is capable of returning
     * a numeric result.
     *
     * @param exp the expression to be examined
     * @param th  the type hierarchy cache
     * @return true if the expression depends on position() or last() explicitly or implicitly
     */

    public static boolean isPositionalFilter(Expression exp, TypeHierarchy th) {
        ItemType type = exp.getItemType();
        if (type.equals(BuiltInAtomicType.BOOLEAN)) {
            // common case, get it out of the way quickly
            return isExplicitlyPositional(exp);
        }
        return type.equals(BuiltInAtomicType.ANY_ATOMIC) ||
                type instanceof AnyItemType ||
                type.equals(BuiltInAtomicType.INTEGER) ||
                type.equals(BuiltInAtomicType.NUMERIC) ||
                th.isSubType(type, BuiltInAtomicType.NUMERIC) ||
                isExplicitlyPositional(exp);
    }

    /**
     * Determine whether an expression, when used as a filter, has an explicit dependency on position() or last()
     *
     * @param exp the expression being tested
     * @return true if the expression is explicitly positional, that is, if it contains an explicit call on
     *         position() or last()
     */

    private static boolean isExplicitlyPositional(Expression exp) {
        return (exp.getDependencies() & (StaticProperty.DEPENDS_ON_POSITION | StaticProperty.DEPENDS_ON_LAST)) != 0;
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
        return operandList(
                new Operand(base, OperandRole.SAME_FOCUS_ACTION),
                new Operand(filter, FILTER_PREDICATE)
        );
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
        if (base == original) {
            base = replacement;
            found = true;
        }
        if (filter == original) {
            filter = replacement;
            found = true;
        }
        return found;
    }


    /**
     * Get the static cardinality of this expression
     *
     * @return the cardinality. The method attempts to determine the case where the
     *         filter predicate is guaranteed to select at most one item from the sequence being filtered
     */

    public int computeCardinality() {
        if (filter instanceof Literal && ((Literal) filter).getValue() instanceof NumericValue) {
            if (((NumericValue) ((Literal) filter).getValue()).compareTo(1) == 0 &&
                    !Cardinality.allowsZero(base.getCardinality())) {
                return StaticProperty.ALLOWS_ONE;
            } else {
                return StaticProperty.ALLOWS_ZERO_OR_ONE;
            }
        }
        if (filterIsIndependent) {
            ItemType filterType = filter.getItemType().getPrimitiveItemType();
            if (filterType == BuiltInAtomicType.INTEGER || filterType == BuiltInAtomicType.DOUBLE ||
                    filterType == BuiltInAtomicType.DECIMAL || filterType == BuiltInAtomicType.FLOAT) {
                return StaticProperty.ALLOWS_ZERO_OR_ONE;
            }
            if (filter instanceof ArithmeticExpression || filter instanceof ArithmeticExpression10) {
                return StaticProperty.ALLOWS_ZERO_OR_ONE;
            }
        }
        if (filter instanceof IsLastExpression && ((IsLastExpression) filter).getCondition()) {
            return base.getCardinality() & ~StaticProperty.ALLOWS_MANY;
        }
        if (!Cardinality.allowsMany(base.getCardinality())) {
            return StaticProperty.ALLOWS_ZERO_OR_ONE;
        }

        return StaticProperty.ALLOWS_ZERO_OR_MORE;
    }

    /**
     * Get the static properties of this expression (other than its type). The result is
     * bit-significant. These properties are used for optimizations. In general, if
     * property bit is set, it is true, but if it is unset, the value is unknown.
     *
     * @return the static properties of the expression, as a bit-significant value
     */

    public int computeSpecialProperties() {
        return base.getSpecialProperties();
    }

    /**
     * Is this expression the same as another expression?
     *
     * @param other the expression to be compared with this one
     * @return true if the two expressions are statically equivalent
     */

    public boolean equals(Object other) {
        if (other instanceof FilterExpression) {
            FilterExpression f = (FilterExpression) other;
            return base.equals(f.base) &&
                    filter.equals(f.filter);
        }
        return false;
    }

    /**
     * get HashCode for comparing two expressions
     *
     * @return the hash code
     */

    public int hashCode() {
        return "FilterExpression".hashCode() + base.hashCode() + filter.hashCode();
    }

    /**
     * Convert this expression to an equivalent XSLT pattern
     *
     * @param config the Saxon configuration
     * @param is30   true if this is XSLT 3.0
     * @return the equivalent pattern
     * @throws net.sf.saxon.trans.XPathException
     *          if conversion is not possible
     */
    @Override
    public Pattern toPattern(Configuration config, boolean is30) throws XPathException {
        Expression base = getSelectExpression();
        Expression filter = getFilter();
        TypeHierarchy th = config.getTypeHierarchy();
        Pattern basePattern = base.toPattern(config, is30);
        if (!isPositional(th)) {
            return new PatternWithPredicate(basePattern, filter);
        } else if (basePattern instanceof NodeTestPattern &&
                basePattern.getItemType() instanceof NodeTest &&
                filterIsPositional &&
                base instanceof AxisExpression &&
                ((AxisExpression) base).getAxis() == AxisInfo.CHILD &&
                (filter.getDependencies() & StaticProperty.DEPENDS_ON_LAST) == 0) {
            return new GeneralPositionalPattern((NodeTest) basePattern.getItemType(), filter);
        }
        if (base.getItemType() instanceof NodeTest) {
            return new GeneralNodePattern(this, (NodeTest) base.getItemType());
        } else {
            throw new XPathException("The filtered expression in an XSLT 2.0 pattern must be a simple step");
        }
    }

    /**
     * Iterate over the results, returning them in the correct order
     *
     * @param context the dynamic context for the evaluation
     * @return an iterator over the expression results
     * @throws XPathException if any dynamic error occurs
     */

    /*@NotNull*/
    public SequenceIterator iterate(XPathContext context) throws XPathException {

        // Fast path where the filter value is independent of the focus

        if (filterIsIndependent) {
            try {
                SequenceIterator it = filter.iterate(context);
                Item first = it.next();
                if (first == null) {
                    return EmptyIterator.emptyIterator();
                }
                if (first instanceof NumericValue) {
                    if (it.next() != null) {
                        ExpressionTool.ebvError("sequence of two or more items starting with a numeric value");
                    } else {
                        // Filter is a constant number
                        if (((NumericValue) first).isWholeNumber()) {
                            int pos = (int) ((NumericValue) first).longValue();
                            if (pos > 0) {
                                if (base instanceof VariableReference) {
                                    Sequence baseVal = ((VariableReference)base).evaluateVariable(context);
                                    if (baseVal instanceof MemoClosure) {
                                        final Item m = ((MemoClosure) baseVal).itemAt(pos - 1);
                                        return m == null ? EmptyIterator.emptyIterator() : m.iterate();
                                    } else {
                                        Item m = SequenceTool.toGroundedValue(baseVal).itemAt(pos - 1);
                                        return m == null ? EmptyIterator.emptyIterator() : m.iterate();
                                    }
                                } else if (base instanceof Literal) {
                                    final Item m = ((Literal) base).getValue().itemAt(pos - 1);
                                    return m == null ? EmptyIterator.emptyIterator() : m.iterate();
                                } else {
                                    SequenceIterator baseIter = base.iterate(context);
                                    return SubsequenceIterator.make(baseIter, pos, pos);
                                }
                            }
                        }
                        // a non-integer value or non-positive number will never be equal to position()
                        return EmptyIterator.emptyIterator();
                    }
                } else {
                    // Filter is focus-independent, but not numeric: need to use the effective boolean value
                    boolean ebv = false;
                    if (first instanceof NodeInfo) {
                        ebv = true;
                    } else if (first instanceof BooleanValue) {
                        ebv = ((BooleanValue)first).getBooleanValue();
                        if (it.next() != null) {
                            ExpressionTool.ebvError("sequence of two or more items starting with a boolean value");
                        }
                    } else if (first instanceof StringValue) {
                        ebv = !((StringValue)first).isZeroLength();
                        if (it.next() != null) {
                            ExpressionTool.ebvError("sequence of two or more items starting with a boolean value");
                        }
                    } else {
                        ExpressionTool.ebvError("sequence starting with an atomic value other than a boolean, number, or string");
                    }
                    if (ebv) {
                        return base.iterate(context);
                    } else {
                        return EmptyIterator.emptyIterator();
                    }
                }
            } catch (XPathException e) {
                e.maybeSetLocation(this);
                throw e;
            }
        }

        // get an iterator over the base nodes

        SequenceIterator baseIter = base.iterate(context);

        // quick exit for an empty sequence

        if (baseIter instanceof EmptyIterator) {
            return baseIter;
        }

        if (filterIsPositional && !filterIsSingletonBoolean) {
            return new FilterIterator(baseIter, filter, context);
        } else {
            return new FilterIterator.NonNumeric(baseIter, filter, context);
        }

    }

    /**
     * Determine which aspects of the context the expression depends on. The result is
     * a bitwise-or'ed value composed from constants such as XPathContext.VARIABLES and
     * XPathContext.CURRENT_NODE
     *
     * @return the dependencies
     */

    public int computeDependencies() {
        // not all dependencies in the filter expression matter, because the context node,
        // position, and size are not dependent on the outer context.
        return base.getDependencies() |
                (filter.getDependencies() & (StaticProperty.DEPENDS_ON_XSLT_CONTEXT |
                        StaticProperty.DEPENDS_ON_LOCAL_VARIABLES |
                        StaticProperty.DEPENDS_ON_USER_FUNCTIONS));
    }

    /**
     * Copy an expression. This makes a deep copy.
     *
     * @return the copy of the original expression
     */

    /*@NotNull*/
    public Expression copy() {
        FilterExpression fe = new FilterExpression(base.copy(), filter.copy());
        fe.filterIsIndependent = filterIsIndependent;
        fe.filterIsPositional = filterIsPositional;
        fe.filterIsSingletonBoolean = filterIsSingletonBoolean;
        return fe;
    }

//#ifdefined BYTECODE

    /**
     * Return the compiler of the Filter expression
     *
     * @return the relevant ExpressionCompiler
     */
    @Override
    public ExpressionCompiler getExpressionCompiler() {
        return new FilterExpressionCompiler();
    }
//#endif

//#ifdefined STREAM

    /**
     * Get a class that supports streamed evaluation of this expression
     *
     * @return the relevant StreamingAdjunct, or null if none is available
     */
    @Override
    public FilterExpressionAdjunct getStreamingAdjunct() {
        return new FilterExpressionAdjunct();
    }


//#endif

    /**
     * The toString() method for an expression attempts to give a representation of the expression
     * in an XPath-like form, but there is no guarantee that the syntax will actually be true XPath.
     * In the case of XSLT instructions, the toString() method gives an abstracted view of the syntax
     */

    public String toString() {
        return ExpressionTool.parenthesize(base) + "[" + filter.toString() + "]";
    }

    @Override
    public String toShortString() {
        return base.toShortString() + "[" + filter.toShortString() + "]";
    }

    /**
     * Diagnostic print of expression structure. The abstract expression tree
     * is written to the supplied output destination.
     *
     * @param out the ExpressionPresenter to be used
     */

    public void explain(ExpressionPresenter out) {
        out.startElement("filterExpression");
        base.explain(out);
        filter.explain(out);
        out.endElement();
    }

}

