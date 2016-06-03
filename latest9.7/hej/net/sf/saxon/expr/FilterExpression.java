////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
// Copyright (c) 2015 Saxonica Limited.
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
import net.sf.saxon.functions.PositionAndLast;
import net.sf.saxon.functions.SystemFunction;
import net.sf.saxon.functions.VendorFunctionLibrary;
import net.sf.saxon.lib.FeatureKeys;
import net.sf.saxon.lib.NamespaceConstant;
import net.sf.saxon.om.*;
import net.sf.saxon.pattern.*;
import net.sf.saxon.trace.ExpressionPresenter;
import net.sf.saxon.trans.XPathException;
import net.sf.saxon.tree.iter.EmptyIterator;
import net.sf.saxon.type.*;
import net.sf.saxon.value.*;

import java.math.BigInteger;
import java.util.HashMap;
import java.util.Map;


/**
 * A FilterExpression contains a base expression and a filter predicate, which may be an
 * integer expression (positional filter), or a boolean expression (qualifier)
 */

public final class FilterExpression extends BinaryExpression implements ContextSwitchingExpression {

    private boolean filterIsPositional;         // true if the value of the filter might depend on
    // the context position
    private boolean filterIsSingletonBoolean;   // true if the filter expression always returns a single boolean
    private boolean filterIsIndependent;        // true if the filter expression does not
    // depend on the context item or position. (It may depend on last()).
    public static final int FILTERED = 10000;

    public final static OperandRole FILTER_PREDICATE =
            new OperandRole(OperandRole.USES_NEW_FOCUS | OperandRole.HIGHER_ORDER, OperandUsage.INSPECTION, SequenceType.ANY_SEQUENCE);


    /**
     * Constructor
     *
     * @param base   The base expression to be filtered.
     * @param filter An expression defining the filter predicate
     */

    public FilterExpression(Expression base, Expression filter) {
        super(base, Token.LSQB, filter);
        base.setFiltered(true);
    }

    @Override
    protected OperandRole getOperandRole(int arg) {
        return arg == 0 ? OperandRole.SAME_FOCUS_ACTION : FILTER_PREDICATE;
    }

    public Expression getBase() {
        return getLhsExpression();
    }

    public void setBase(Expression base) {
        setLhsExpression(base);
    }

    /**
     * Get the filter expression
     *
     * @return the expression acting as the filter predicate
     */

    public Expression getFilter() {
        return getRhsExpression();
    }


    public void setFilter(Expression filter) {
        setRhsExpression(filter);
    }

    /**
     * Get a name identifying the kind of expression, in terms meaningful to a user.
     *
     * @return a name identifying the kind of expression, in terms meaningful to a user.
     * The name will always be in the form of a lexical XML QName, and should match the name used
     * in explain() output displaying the expression.
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
        if (getFilter() instanceof InstanceOfExpression &&
                ((InstanceOfExpression) getFilter()).getBaseExpression() instanceof ContextItemExpression) {
            return ((InstanceOfExpression) getFilter()).getRequiredItemType();
        }
        return getBase().getItemType();
    }

    /**
     * Get the base expression
     *
     * @return the base expression being filtered
     */

    public Expression getSelectExpression() {
        return getBase();
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
        return getFilter();
    }


    /**
     * Determine if the filter is positional
     *
     * @param th the Type Hierarchy (for cached access to type information)
     * @return true if the value of the filter depends on the position of the item against
     * which it is evaluated
     */

    public boolean isPositional(TypeHierarchy th) {
        return isPositionalFilter(getFilter(), th);
    }

    /**
     * Test if the filter always returns a singleton boolean.
     * <p/>
     * <p>This information is available only after typeCheck() has been called.</p>
     *
     * @return true if the filter is a simple boolean expression
     */

    public boolean isSimpleBooleanFilter() {
        return filterIsSingletonBoolean;
    }

    /**
     * Determine whether the filter is independent of the context item and position
     * <p/>
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
     * @throws XPathException if any failure occurs
     */

    /*@NotNull*/
    public Expression simplify() throws XPathException {

        setBase(getBase().simplify());
        setFilter(getFilter().simplify());

        // ignore the filter if the base expression is an empty sequence
        if (Literal.isEmptySequence(getBase())) {
            return getBase();
        }

        // check whether the filter is a constant true() or false()
        if (getFilter() instanceof Literal && !(((Literal) getFilter()).getValue() instanceof NumericValue)) {
            try {
                if (getFilter().effectiveBooleanValue(new EarlyEvaluationContext(getConfiguration()))) {
                    return getBase();
                } else {
                    return Literal.makeEmptySequence();
                }
            } catch (XPathException e) {
                e.maybeSetLocation(getLocation());
                throw e;
            }
        }

        // check whether the filter is [last()] (note, [position()=last()] is handled elsewhere)

        if (getFilter().isCallOn(PositionAndLast.Last.class)) {
            setFilter(new IsLastExpression(true));
            adoptChildExpression(getFilter());
        }

        return this;

    }

    /**
     * Type-check the expression
     * <p/>
     * <p/>
     * param visitor         the expression visitor
     *
     * @param contextInfo return the expression after type-checking (potentially modified to add run-time
     *                    checks and/or conversions)
     */

    /*@NotNull*/
    public Expression typeCheck(ExpressionVisitor visitor, ContextItemStaticInfo contextInfo) throws XPathException {
        final TypeHierarchy th = getConfiguration().getTypeHierarchy();
        getLhs().typeCheck(visitor, contextInfo);
        getBase().setFiltered(true);
        if (Literal.isEmptySequence(getBase())) {
            return getBase();
        }

        getRhs().typeCheck(visitor, new ContextItemStaticInfo(getBase().getItemType(), false, getBase()));

        // The filter expression usually doesn't need to be sorted

        Expression filter2 = ExpressionTool.unsortedIfHomogeneous(getFilter(), visitor.isOptimizeForStreaming());
        if (filter2 != getFilter()) {
            setFilter(filter2);
        }

        // detect head expressions (E[1]) and treat them specially

        if (Literal.isConstantOne(getFilter())) {
            Expression fie = FirstItemExpression.makeFirstItemExpression(getBase());
            ExpressionTool.copyLocationInfo(this, fie);
            return fie;
        }

        // determine whether the filter might depend on position
        filterIsPositional = isPositionalFilter(getFilter(), th);

        // determine whether the filter always evaluates to a single boolean
        filterIsSingletonBoolean =
                getFilter().getCardinality() == StaticProperty.EXACTLY_ONE &&
                        getFilter().getItemType().equals(BuiltInAtomicType.BOOLEAN);

        // determine whether the filter expression is independent of the focus

        filterIsIndependent = (getFilter().getDependencies() & StaticProperty.DEPENDS_ON_FOCUS) == 0;

        ExpressionTool.resetStaticProperties(this);
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
        final Configuration config = getConfiguration();
        final Optimizer opt = config.obtainOptimizer();
        final boolean debug = config.getBooleanProperty(FeatureKeys.TRACE_OPTIMIZER_DECISIONS);
        final TypeHierarchy th = config.getTypeHierarchy();

        getLhs().optimize(visitor, contextItemType);
        getBase().setFiltered(true);

        Expression originalFilter;
        try {
            originalFilter = getFilter().copy(new HashMap<IdentityWrapper<Binding>, Binding>());
        } catch (UnsupportedOperationException err) {
            originalFilter = null;
        }
        ContextItemStaticInfo baseItemType = new ContextItemStaticInfo(getBase().getItemType(), false, getBase());
        getRhs().optimize(visitor, baseItemType);

        // The filter expression usually doesn't need to be sorted

        Expression filter2 = ExpressionTool.unsortedIfHomogeneous(getFilter(), visitor.isOptimizeForStreaming());
        if (filter2 != getFilter()) {
            setFilter(filter2);
        }

        // Rewrite child::X[last()] as child::X[empty(following-sibling::X)] - especially useful for patterns

        if (getFilter() instanceof IsLastExpression &&
            ((IsLastExpression) getFilter()).getCondition() &&
            getBase() instanceof AxisExpression &&
            ((AxisExpression) getBase()).getAxis() == AxisInfo.CHILD) {
            NodeTest test = ((AxisExpression) getBase()).getNodeTest();
            AxisExpression fs = new AxisExpression(AxisInfo.FOLLOWING_SIBLING, test);
            setFilter(SystemFunction.makeCall("empty", getRetainedStaticContext(), new Expression[]{fs}));
        }

        // if the result of evaluating the filter cannot include numeric values, then we can use
        // its effective boolean value

        ItemType filterType = getFilter().getItemType();
        if (!th.isSubType(filterType, BuiltInAtomicType.BOOLEAN)
                && th.relationship(filterType, NumericType.getInstance()) == TypeHierarchy.DISJOINT) {
            Expression f = SystemFunction.makeCall("boolean", getRetainedStaticContext(), getFilter());
            setFilter(f.optimize(visitor, baseItemType));
        }

        // the filter expression may have been reduced to a constant boolean by previous optimizations
        if (getFilter() instanceof Literal && ((Literal) getFilter()).getValue() instanceof BooleanValue) {
            if (((BooleanValue) ((Literal) getFilter()).getValue()).getBooleanValue()) {
                if (debug) {
                    opt.trace("Redundant filter removed", getBase());
                }
                return getBase();
            } else {
                if (debug) {
                    opt.trace("Filter expression eliminated because predicate is always false",
                            Literal.makeEmptySequence());
                }
                return Literal.makeEmptySequence();
            }
        }

        // determine whether the filter might depend on position
        filterIsPositional = isPositionalFilter(getFilter(), th);
        filterIsSingletonBoolean =
                getFilter().getCardinality() == StaticProperty.EXACTLY_ONE &&
                        getFilter().getItemType().equals(BuiltInAtomicType.BOOLEAN);

        // determine whether the filter is indexable
        if (!filterIsPositional && !visitor.isOptimizeForStreaming()) {
            int isIndexable = opt.isIndexableFilter(getFilter());
            if (isIndexable == 0 && getFilter() != originalFilter && originalFilter != null) {
                // perhaps the original filter was indexable; if so, revert to the original
                // this happens when [@a = 1] is rewritten as [some $x in @a satisfies $x eq 1]
                // TODO: this rollback mechanism is very unsatisfactory. Better: make the some expression indexable!
                int origIndexable = opt.isIndexableFilter(originalFilter);
                if (origIndexable != 0) {
                    isIndexable = origIndexable;
                    setFilter(originalFilter);
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
                getFilter() instanceof BooleanExpression &&
                ((BooleanExpression) getFilter()).operator == Token.AND) {
            BooleanExpression bf = (BooleanExpression) getFilter();
            if (isExplicitlyPositional(bf.getLhsExpression()) &&
                    !isExplicitlyPositional(bf.getRhsExpression())) {
                Expression p0 = forceToBoolean(bf.getLhsExpression());
                Expression p1 = forceToBoolean(bf.getRhsExpression());
                FilterExpression f1 = new FilterExpression(getBase(), p0);
                ExpressionTool.copyLocationInfo(this, f1);
                FilterExpression f2 = new FilterExpression(f1, p1);
                ExpressionTool.copyLocationInfo(this, f2);
                if (debug) {
                    opt.trace("Composite filter replaced by nested filter expressions", f2);
                }
                return f2.optimize(visitor, contextItemType);
            }
            if (isExplicitlyPositional(bf.getRhsExpression()) &&
                    !isExplicitlyPositional(bf.getLhsExpression())) {
                Expression p0 = forceToBoolean(bf.getLhsExpression());
                Expression p1 = forceToBoolean(bf.getRhsExpression());
                FilterExpression f1 = new FilterExpression(getBase(), p1);
                ExpressionTool.copyLocationInfo(this, f1);
                FilterExpression f2 = new FilterExpression(f1, p0);
                ExpressionTool.copyLocationInfo(this, f2);
                if (debug) {
                    opt.trace("Composite filter replaced by nested filter expressions", f2);
                }
                return f2.optimize(visitor, contextItemType);
            }
        }

        if (getFilter() instanceof IsLastExpression &&
                ((IsLastExpression) getFilter()).getCondition()) {

            if (getBase() instanceof Literal) {
                setFilter(Literal.makeLiteral(new Int64Value(((Literal) getBase()).getValue().getLength())));
            } else {
                return new LastItemExpression(getBase());
            }
        }

        Expression subsequence = tryToRewritePositionalFilter(visitor);
        if (subsequence != null) {
            if (debug) {
                subsequence.setRetainedStaticContext(getRetainedStaticContext());  // Avoids errors in debug explain
                opt.trace("Rewrote Filter Expression as:", subsequence);
            }
            ExpressionTool.copyLocationInfo(this, subsequence);
            return subsequence.simplify()
                    .typeCheck(visitor, contextItemType)
                    .optimize(visitor, contextItemType);
        }

        // If there are two non-positional filters, consider changing their order based on the estimated cost
        // of evaluation, so we evaluate the cheapest predicates first

        if (!filterIsPositional && getLhsExpression() instanceof FilterExpression &&
                !((FilterExpression)getLhsExpression()).isFilterIsPositional()) {
            Expression base = ((FilterExpression) getLhsExpression()).getLhsExpression();
            Expression predicate1 = ((FilterExpression) getLhsExpression()).getRhsExpression();
            Expression predicate2 = getRhsExpression();
            if (predicate1.getCost() > 2*predicate2.getCost()) {
                FilterExpression fe1 = new FilterExpression(base, predicate2);
                FilterExpression fe2 = new FilterExpression(fe1, predicate1);
                ExpressionTool.copyLocationInfo(this, fe2);
                return fe2.optimize(visitor, contextItemType);
            }

        }

        // If any subexpressions within the filter are not dependent on the focus,
        // promote them: this causes them to be evaluated once, outside the filter
        // expression. Note: we do this even if the filter is numeric, because it ensures that
        // the subscript is pre-evaluated, allowing direct indexing into the sequence.

        PromotionOffer offer = new PromotionOffer(opt);
        offer.action = PromotionOffer.FOCUS_INDEPENDENT;
        offer.promoteDocumentDependent = (getBase().getSpecialProperties() & StaticProperty.CONTEXT_DOCUMENT_NODESET) != 0;
        offer.containingExpression = this;
        filter2 = doPromotion(getFilter(), offer);
        if (filter2 != getFilter()) {
            setFilter(filter2);
            adoptChildExpression(filter2);
            ExpressionTool.resetStaticProperties(filter2);
        }

        if (offer.containingExpression instanceof LetExpression) {
            if (debug) {
                opt.trace("Subexpression extracted from filter because independent of context", offer.containingExpression);
            }
            ExpressionTool.resetStaticProperties(filter2);
            offer.containingExpression = offer.containingExpression.optimize(visitor, contextItemType);
        }
        Expression result = offer.containingExpression;

        if (result instanceof FilterExpression) {
            final Sequence sequence = ((FilterExpression) result).tryEarlyEvaluation(visitor);
            if (sequence != null) {
                GroundedValue value = SequenceTool.toGroundedValue(sequence);
                return Literal.makeLiteral(value);
            }
        }
        return result;
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
        return getLhsExpression().getCost() + 5*getRhsExpression().getCost();
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
     * unknown or not applicable.
     */
    @Override
    public IntegerValue[] getIntegerBounds() {
        return getBase().getIntegerBounds();
    }


    private Sequence tryEarlyEvaluation(ExpressionVisitor visitor) throws XPathException {
        // Attempt early evaluation of a filter expression if the base sequence is constant and the
        // filter depends only on the context. (This can't be done if, for example, the predicate uses
        // local variables, even variables declared within the predicate)
        try {
            if (getBase() instanceof Literal &&
                    !ExpressionTool.refersToVariableOrFunction(getFilter()) &&
                    (getFilter().getDependencies() & ~StaticProperty.DEPENDS_ON_FOCUS) == 0) {
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
     * expression is the first operand of a path expression or filter expression
     */

    public PathMap.PathMapNodeSet addToPathMap(PathMap pathMap, PathMap.PathMapNodeSet pathMapNodeSet) {
        PathMap.PathMapNodeSet target = getBase().addToPathMap(pathMap, pathMapNodeSet);
        getFilter().addToPathMap(pathMap, target);
        return target;
    }

    /**
     * Construct an expression that obtains the effective boolean value of a given expression,
     * by wrapping it in a call of the boolean() function
     *
     * @param in the given expression
     * @return an expression that wraps the given expression in a call to the fn:boolean() function
     */

    private static Expression forceToBoolean(Expression in) throws XPathException {
        if (in.getItemType().getPrimitiveType() == StandardNames.XS_BOOLEAN) {
            return in;
        }
        return SystemFunction.makeCall("boolean", in.getRetainedStaticContext(), in);
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
        TypeHierarchy th = getConfiguration().getTypeHierarchy();
        if (getFilter() instanceof Literal) {
            GroundedValue val = ((Literal) getFilter()).getValue();
            if (val instanceof NumericValue) {
                if (((NumericValue) val).isWholeNumber()) {
                    long lvalue = ((NumericValue) val).longValue();
                    if (lvalue <= 0 || lvalue >= Integer.MAX_VALUE) {
                        return Literal.makeEmptySequence();
                    } else if (lvalue == 1) {
                        return FirstItemExpression.makeFirstItemExpression(getBase());
                    } else {
                        return new SubscriptExpression(getBase(), getFilter());
                    }
                } else {
                    return Literal.makeEmptySequence();
                }
            } else {
                return ExpressionTool.effectiveBooleanValue(val.iterate()) ? getBase() : Literal.makeEmptySequence();
            }
        }
        if (th.isSubType(getFilter().getItemType(), NumericType.getInstance()) &&
                !Cardinality.allowsMany(getFilter().getCardinality()) &&
                (getFilter().getDependencies() & StaticProperty.DEPENDS_ON_FOCUS) == 0) {
            return new SubscriptExpression(getBase(), getFilter());
        }
        if (getFilter() instanceof ComparisonExpression) {
            VendorFunctionLibrary lib = getConfiguration().getVendorFunctionLibrary();
            StaticContext env = visitor.getStaticContext();
            Expression lhs = ((ComparisonExpression) getFilter()).getLhsExpression();
            Expression rhs = ((ComparisonExpression) getFilter()).getRhsExpression();
            int operator = ((ComparisonExpression) getFilter()).getSingletonOperator();
            Expression comparand;
            if (lhs.isCallOn(PositionAndLast.Position.class)
                    && th.isSubType(rhs.getItemType(), NumericType.getInstance())) {
                comparand = rhs;
            } else if (rhs.isCallOn(PositionAndLast.Position.class)
                    && th.isSubType(lhs.getItemType(), NumericType.getInstance())) {
                comparand = lhs;
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
                Expression exists = SystemFunction.makeCall("exists", getRetainedStaticContext(), existsArg);
                Expression rewrite = tryToRewritePositionalFilterSupport(getBase(), comparand, operator, th, lib, env);
                if (rewrite == null) {
                    return this;
                }
                Expression choice = Choose.makeConditional(exists, rewrite);
                let.setAction(choice);
                return let;
            } else {
                return tryToRewritePositionalFilterSupport(getBase(), comparand, operator, th, lib, env);
            }
        } else if (getFilter() instanceof IntegerRangeTest) {
            // rewrite SEQ[position() = N to M]
            // => let $n := N return subsequence(SEQ, $n, (M - ($n - 1))
            // (precise form is optimized for the case where $n is a literal, especially N = 1)
            Expression val = ((IntegerRangeTest) getFilter()).getValue();
            if (!val.isCallOn(PositionAndLast.class)) {
                return null;
            }
            Expression min = ((IntegerRangeTest) getFilter()).getMin();
            Expression max = ((IntegerRangeTest) getFilter()).getMax();

            if (ExpressionTool.dependsOnFocus(min)) {
                return null;
            }
            if (ExpressionTool.dependsOnFocus(max)) {
                if (max.isCallOn(PositionAndLast.Last.class)) {
                    return SystemFunction.makeCall("subsequence", getRetainedStaticContext(), getBase(), min);
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
                    min2, Token.MINUS, Literal.makeLiteral(Int64Value.makeIntegerValue(1)));
            Expression length = new ArithmeticExpression(max, Token.MINUS, minMinusOne);
            Expression subs = SystemFunction.makeCall("subsequence", getRetainedStaticContext(), getBase(), min, length);
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
                        return Literal.makeEmptySequence();
                    } else {
                        return new SubscriptExpression(start, comparand);
                    }
                }
                case Token.FLT: {

                    Expression[] args = new Expression[3];
                    args[0] = start;
                    args[1] = Literal.makeLiteral(Int64Value.makeIntegerValue(1));
                    if (Literal.isAtomic(comparand)) {
                        long n = ((NumericValue) ((Literal) comparand).getValue()).longValue();
                        args[2] = Literal.makeLiteral(Int64Value.makeIntegerValue(n - 1));
                    } else {
                        ArithmeticExpression decrement = new ArithmeticExpression(
                                comparand, Token.MINUS, Literal.makeLiteral(Int64Value.makeIntegerValue(1)));
                        decrement.setCalculator(Calculator.getCalculator(      // bug 2704
                                StandardNames.XS_INTEGER, StandardNames.XS_INTEGER, Calculator.MINUS, true));
                        args[2] = decrement;
                    }
                    return SystemFunction.makeCall("subsequence", start.getRetainedStaticContext(), args);
                }
                case Token.FLE: {
                    Expression[] args = new Expression[3];
                    args[0] = start;
                    args[1] = Literal.makeLiteral(Int64Value.makeIntegerValue(1));
                    args[2] = comparand;
                    return SystemFunction.makeCall("subsequence", start.getRetainedStaticContext(), args);
                }
                case Token.FNE: {
                    return SystemFunction.makeCall("remove", start.getRetainedStaticContext(), start, comparand);
                }
                case Token.FGT: {
                    Expression[] args = new Expression[2];
                    args[0] = start;
                    if (Literal.isAtomic(comparand)) {
                        long n = ((NumericValue) ((Literal) comparand).getValue()).longValue();
                        args[1] = Literal.makeLiteral(Int64Value.makeIntegerValue(n + 1));
                    } else {
                        args[1] = new ArithmeticExpression(
                                comparand, Token.PLUS, Literal.makeLiteral(Int64Value.makeIntegerValue(1)));
                    }
                    return SystemFunction.makeCall("subsequence", start.getRetainedStaticContext(), args);
                }
                case Token.FGE: {
                    return SystemFunction.makeCall("subsequence", start.getRetainedStaticContext(), start, comparand);
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
                            "is-whole-number", new Expression[]{isWholeArg}, env);
                    Expression minusOne = new ArithmeticExpression(
                            arithArg, Token.MINUS, Literal.makeLiteral(Int64Value.makeIntegerValue(1)));
                    Expression floor = SystemFunction.makeCall("floor", start.getRetainedStaticContext(), floorArg);
                    Expression choice = Choose.makeConditional(isWhole, minusOne, floor);
                    Expression subs = SystemFunction.makeCall(
                        "subsequence", start.getRetainedStaticContext(), start, Literal.makeLiteral(Int64Value.makeIntegerValue(1)), choice);
                    let.setAction(subs);
                    //decl.fixupReferences(let);
                    return let;
                }
                case Token.FLE: {
                    Expression floor = SystemFunction.makeCall("floor", start.getRetainedStaticContext(), comparand);
                    return SystemFunction.makeCall(
                        "subsequence", start.getRetainedStaticContext(), start, Literal.makeLiteral(Int64Value.makeIntegerValue(1)), floor);
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
                            "is-whole-number", new Expression[]{isWholeArg}, env);
                    ExpressionTool.copyLocationInfo(start, isWhole);
                    Expression cast = new CastExpression(castArg, BuiltInAtomicType.INTEGER, false);
                    ExpressionTool.copyLocationInfo(start, cast);
                    Expression choice = Choose.makeConditional(
                            isWhole, cast, Literal.makeLiteral(Int64Value.makeIntegerValue(0)));
                    Expression rem = SystemFunction.makeCall("remove", start.getRetainedStaticContext(), start, choice);
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
                            "is-whole-number", new Expression[]{isWholeArg}, env);
                    Expression plusOne = new ArithmeticExpression(
                            arithArg, Token.PLUS, Literal.makeLiteral(Int64Value.makeIntegerValue(1)));
                    Expression ceiling = SystemFunction.makeCall("ceiling", start.getRetainedStaticContext(), ceilingArg);
                    Expression choice = Choose.makeConditional(isWhole, plusOne, ceiling);
                    Expression subs = SystemFunction.makeCall("subsequence", start.getRetainedStaticContext(), start, choice);
                    let.setAction(subs);
                    return let;
                }
                case Token.FGE: {
                    // rewrite SEQ[position() ge V] => subsequence(SEQ, ceiling(V))
                    Expression ceiling = SystemFunction.makeCall("ceiling", start.getRetainedStaticContext(), comparand);
                    return SystemFunction.makeCall("subsequence", start.getRetainedStaticContext(), start, ceiling);
                }
                default:
                    throw new IllegalArgumentException("operator");
            }
        }
    }

    /**
     * Promote this expression if possible
     *
     * @param offer details of the promotion that is possible
     * @return the promoted expression (or the original expression, unchanged)
     */

    public Expression promote(PromotionOffer offer) throws XPathException {
        Expression exp = offer.accept(this);
        if (exp != null) {
            return exp;
        } else {
            if ((offer.action == PromotionOffer.RANGE_INDEPENDENT) && (getBase() instanceof FilterExpression)) {
                // Try to handle the situation where we have (EXP[PRED1])[PRED2], and
                // EXP[PRED2] does not depend on the range variable, but PRED1 does
                TypeHierarchy th = offer.getOptimizer().getConfiguration().getTypeHierarchy();
                FilterExpression newfe = promoteIndependentPredicates(offer.bindingList, offer.getOptimizer(), th);
                if (newfe != this) {
                    return newfe.promote(offer);
                }
            }
            setBase(doPromotion(getBase(), offer));
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
     * @param forStreaming   set to true if optimizing for streaming
     */
    @Override
    public Expression unordered(boolean retainAllNodes, boolean forStreaming) throws XPathException {
        if (!filterIsPositional) {
            setBase(getBase().unordered(retainAllNodes, forStreaming));
        }
        return this;
    }

    /**
     * Rearrange a filter expression so that predicates that are independent of a given
     * set of range variables come first, allowing them to be promoted along with the base
     * expression
     *
     * @param bindings the given set of range variables
     * @param opt      the Optimizer
     * @param th       the type hierarchy cache
     * @return the expression after promoting independent predicates
     */

    private FilterExpression promoteIndependentPredicates(Binding[] bindings, Optimizer opt, TypeHierarchy th) {
        if (!ExpressionTool.dependsOnVariable(getBase(), bindings)) {
            return this;
        }
        if (isPositional(th)) {
            return this;
        }
        if (getBase() instanceof FilterExpression) {
            FilterExpression fe = (FilterExpression) getBase();
            if (fe.isPositional(th)) {
                return this;
            }
            if (!ExpressionTool.dependsOnVariable(fe.getFilter(), bindings)) {
                return this;
            }
            if (!ExpressionTool.dependsOnVariable(getFilter(), bindings)) {
                FilterExpression result = new FilterExpression(
                        new FilterExpression(fe.getBase(), getFilter()).promoteIndependentPredicates(bindings, opt, th),
                        fe.getFilter());
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
                type.equals(NumericType.getInstance()) ||
                th.isSubType(type, NumericType.getInstance()) ||
                isExplicitlyPositional(exp);
    }

    /**
     * Determine whether an expression, when used as a filter, has an explicit dependency on position() or last()
     *
     * @param exp the expression being tested
     * @return true if the expression is explicitly positional, that is, if it contains an explicit call on
     * position() or last()
     */

    private static boolean isExplicitlyPositional(Expression exp) {
        return (exp.getDependencies() & (StaticProperty.DEPENDS_ON_POSITION | StaticProperty.DEPENDS_ON_LAST)) != 0;
    }


    /**
     * Get the static cardinality of this expression
     *
     * @return the cardinality. The method attempts to determine the case where the
     * filter predicate is guaranteed to select at most one item from the sequence being filtered
     */

    public int computeCardinality() {
        if (getFilter() instanceof Literal && ((Literal) getFilter()).getValue() instanceof NumericValue) {
            if (((NumericValue) ((Literal) getFilter()).getValue()).compareTo(1) == 0 &&
                    !Cardinality.allowsZero(getBase().getCardinality())) {
                return StaticProperty.ALLOWS_ONE;
            } else {
                return StaticProperty.ALLOWS_ZERO_OR_ONE;
            }
        }
        if (filterIsIndependent) {
            ItemType filterType = getFilter().getItemType().getPrimitiveItemType();
            if (filterType == BuiltInAtomicType.INTEGER || filterType == BuiltInAtomicType.DOUBLE ||
                    filterType == BuiltInAtomicType.DECIMAL || filterType == BuiltInAtomicType.FLOAT) {
                return StaticProperty.ALLOWS_ZERO_OR_ONE;
            }
            if (getFilter() instanceof ArithmeticExpression || getFilter() instanceof ArithmeticExpression10) {
                return StaticProperty.ALLOWS_ZERO_OR_ONE;
            }
        }
        if (getFilter() instanceof IsLastExpression && ((IsLastExpression) getFilter()).getCondition()) {
            return getBase().getCardinality() & ~StaticProperty.ALLOWS_MANY;
        }
        if (!Cardinality.allowsMany(getBase().getCardinality())) {
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
        return getBase().getSpecialProperties();
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
            return getBase().equals(f.getBase()) &&
                    getFilter().equals(f.getFilter());
        }
        return false;
    }

    /**
     * get HashCode for comparing two expressions
     *
     * @return the hash code
     */

    public int hashCode() {
        return "FilterExpression".hashCode() + getBase().hashCode() + getFilter().hashCode();
    }

    /**
     * Convert this expression to an equivalent XSLT pattern
     *
     * @param config the Saxon configuration
     * @param is30   true if this is XSLT 3.0
     * @return the equivalent pattern
     * @throws net.sf.saxon.trans.XPathException if conversion is not possible
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
                ((AxisExpression)base).getAxis() == AxisInfo.CHILD &&
                (filter.getDependencies() & StaticProperty.DEPENDS_ON_LAST) == 0) {
            if (filter instanceof Literal && ((Literal)filter).getValue() instanceof IntegerValue) {
                return new SimplePositionalPattern((NodeTest) basePattern.getItemType(), (int)((IntegerValue) ((Literal) filter).getValue()).longValue());
            } else {
                return new GeneralPositionalPattern((NodeTest) basePattern.getItemType(), filter);
            }
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
                SequenceIterator it = getFilter().iterate(context);
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
                                if (getBase() instanceof VariableReference) {
                                    Sequence baseVal = ((VariableReference) getBase()).evaluateVariable(context);
                                    if (baseVal instanceof MemoClosure) {
                                        Item m = ((MemoClosure) baseVal).itemAt(pos - 1);
                                        return m == null ? EmptyIterator.emptyIterator() : m.iterate();
                                    } else {
                                        Item m = SequenceTool.toGroundedValue(baseVal).itemAt(pos - 1);
                                        return m == null ? EmptyIterator.emptyIterator() : m.iterate();
                                    }
                                } else if (getBase() instanceof Literal) {
                                    Item i = ((Literal) getBase()).getValue().itemAt(pos - 1);
                                    return i == null ? EmptyIterator.getInstance() : i.iterate();
                                } else {
                                    SequenceIterator baseIter = getBase().iterate(context);
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
                        ebv = ((BooleanValue) first).getBooleanValue();
                        if (it.next() != null) {
                            ExpressionTool.ebvError("sequence of two or more items starting with a boolean value");
                        }
                    } else if (first instanceof StringValue) {
                        ebv = !((StringValue) first).isZeroLength();
                        if (it.next() != null) {
                            ExpressionTool.ebvError("sequence of two or more items starting with a boolean value");
                        }
                    } else {
                        ExpressionTool.ebvError("sequence starting with an atomic value other than a boolean, number, or string");
                    }
                    if (ebv) {
                        return getBase().iterate(context);
                    } else {
                        return EmptyIterator.emptyIterator();
                    }
                }
            } catch (XPathException e) {
                e.maybeSetLocation(getLocation());
                throw e;
            }
        }

        // get an iterator over the base nodes

        SequenceIterator baseIter = getBase().iterate(context);

        // quick exit for an empty sequence

        if (baseIter instanceof EmptyIterator) {
            return baseIter;
        }

        if (filterIsPositional && !filterIsSingletonBoolean) {
            return new FilterIterator(baseIter, getFilter(), context);
        } else {
            return new FilterIterator.NonNumeric(baseIter, getFilter(), context);
        }

    }

    /**
     * Determine which aspects of the context the expression depends on. The result is
     * a bitwise-or'ed value composed from constants such as XPathContext.VARIABLES and
     * XPathContext.CURRENT_NODE
     *
     * @return the dependencies
     */

//    public int computeDependencies() {
//        // not all dependencies in the filter expression matter, because the context node,
//        // position, and size are not dependent on the outer context.
//        return getBase().getDependencies() |
//                (getFilter().getDependencies() & (StaticProperty.DEPENDS_ON_XSLT_CONTEXT |
//                        StaticProperty.DEPENDS_ON_LOCAL_VARIABLES |
//                        StaticProperty.DEPENDS_ON_USER_FUNCTIONS));
//    }

    /**
     * Copy an expression. This makes a deep copy.
     *
     * @return the copy of the original expression
     * @param rebindings
     */

    /*@NotNull*/
    public Expression copy(Map<IdentityWrapper<Binding>, Binding> rebindings) {
        FilterExpression fe = new FilterExpression(getBase().copy(rebindings), getFilter().copy(rebindings));
        ExpressionTool.copyLocationInfo(this, fe);
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
        return ExpressionTool.parenthesize(getBase()) + "[" + getFilter().toString() + "]";
    }

    @Override
    public String toShortString() {
        return getBase().toShortString() + "[" + getFilter().toShortString() + "]";
    }

    /**
     * Diagnostic print of expression structure. The abstract expression tree
     * is written to the supplied output destination.
     *
     * @param out the ExpressionPresenter to be used
     */

    public void export(ExpressionPresenter out) throws XPathException {
        out.startElement("filter", this);
        String flags = "";
        if (filterIsIndependent) {
            flags += "i";
        }
        if (filterIsPositional) {
            flags += "p";
        }
        if (filterIsSingletonBoolean) {
            flags += "b";
        }
        out.emitAttribute("flags", flags);
        getBase().export(out);
        getFilter().export(out);
        out.endElement();
    }

    public void setFlags(String flags) {
        filterIsIndependent = flags.contains("i");
        filterIsPositional = flags.contains("p");
        filterIsSingletonBoolean = flags.contains("b");
    }


}

