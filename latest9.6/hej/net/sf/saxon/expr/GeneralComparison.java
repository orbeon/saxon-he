////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
// Copyright (c) 2014 Saxonica Limited.
// This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
// If a copy of the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
// This Source Code Form is "Incompatible With Secondary Licenses", as defined by the Mozilla Public License, v. 2.0.
////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

package net.sf.saxon.expr;

import com.saxonica.ee.bytecode.ExpressionCompiler;
import com.saxonica.ee.bytecode.GeneralComparisonCompiler;
import com.saxonica.ee.stream.adjunct.GeneralComparisonAdjunct;
import net.sf.saxon.Configuration;
import net.sf.saxon.expr.parser.*;
import net.sf.saxon.expr.sort.AtomicComparer;
import net.sf.saxon.expr.sort.CodepointCollator;
import net.sf.saxon.expr.sort.GenericAtomicComparer;
import net.sf.saxon.functions.Minimax;
import net.sf.saxon.functions.SystemFunctionCall;
import net.sf.saxon.lib.ConversionRules;
import net.sf.saxon.lib.StringCollator;
import net.sf.saxon.om.GroundedValue;
import net.sf.saxon.om.Sequence;
import net.sf.saxon.om.SequenceIterator;
import net.sf.saxon.om.SequenceTool;
import net.sf.saxon.trace.ExpressionPresenter;
import net.sf.saxon.trans.XPathException;
import net.sf.saxon.type.*;
import net.sf.saxon.value.*;

import java.util.ArrayList;
import java.util.List;


/**
 * GeneralComparison: a boolean expression that compares two expressions
 * for equals, not-equals, greater-than or less-than. This implements the operators
 * =, !=, <, >, etc. This implementation is not used when in backwards-compatible mode
 */

public class GeneralComparison extends BinaryExpression implements ComparisonExpression, Callable {

    public static final int ONE_TO_ONE = 0;
    public static final int MANY_TO_ONE = 1;
    public static final int MANY_TO_MANY = 2;
    // Note, a one-to-many comparison is inverted into a many-to-one comparison

    protected int singletonOperator;
    protected AtomicComparer comparer;
    protected boolean needsRuntimeCheck = true;
    protected int comparisonCardinality = MANY_TO_MANY;


    /**
     * Create a relational expression identifying the two operands and the operator
     *
     * @param p0 the left-hand operand
     * @param op the operator, as a token returned by the Tokenizer (e.g. Token.LT)
     * @param p1 the right-hand operand
     */

    public GeneralComparison(Expression p0, int op, Expression p1) {
        super(p0, op, p1);
        singletonOperator = getCorrespondingSingletonOperator(op);
    }

    /**
     * Ask whether a runtime check of the types of the operands is needed
     *
     * @return true if the types of the operands need to be checked at run-time
     */

    public boolean needsRuntimeCheck() {
        return needsRuntimeCheck;
    }

    /**
     * Say whether a runtime check of the types of the operands is needed
     *
     * @param needsCheck true if the types of the operands need to be checked at run-time
     */

    public void setNeedsRuntimeCheck(boolean needsCheck) {
        needsRuntimeCheck = needsCheck;
    }

    /**
     * Ask whether the comparison is known to be many-to-one, one-to-one, or many-to-many.
     * (Note, an expression that is one-to-many will be converted to one that is many-to-one).
     *
     * @return the Cardinality of the comparison as one of the values {@link #ONE_TO_ONE},
     *         {@link #MANY_TO_MANY}, {@link #MANY_TO_ONE}
     */

    public int getComparisonCardinality() {
        return comparisonCardinality;
    }

    /**
     * Say whether the comparison is known to be many-to-one, one-to-one, or many-to-many.
     *
     * @param card the Cardinality of the comparison as one of the values {@link #ONE_TO_ONE},
     *             {@link #MANY_TO_MANY}, {@link #MANY_TO_ONE}
     */
    public void setComparisonCardinality(int card) {
        comparisonCardinality = card;
    }

    /**
     * Set the comparer to be used
     *
     * @param comparer the comparer to be used
     */

    public void setAtomicComparer(AtomicComparer comparer) {
        this.comparer = comparer;
    }

    /*@NotNull*/
    @Override
    public Expression simplify(ExpressionVisitor visitor) throws XPathException {
        Expression e = super.simplify(visitor);
        if (e != this) {
            ExpressionTool.copyLocationInfo(this, e);
            return e;
        } else if (visitor.getStaticContext().isInBackwardsCompatibleMode()) {
            Expression[] operands = getOperands();
            GeneralComparison10 gc10 = new GeneralComparison10(operands[0], getOperator(), operands[1]);
            gc10.setAtomicComparer(getAtomicComparer());
            return gc10;
        } else {
            Expression[] operands = getOperands();
            GeneralComparison20 gc20 = new GeneralComparison20(operands[0], getOperator(), operands[1]);
            gc20.setAtomicComparer(getAtomicComparer());
            return gc20;
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
        return "GeneralComparison";
    }

    /**
     * Get the AtomicComparer used to compare atomic values. This encapsulates any collation that is used
     */

    public AtomicComparer getAtomicComparer() {
        return comparer;
    }

    /**
     * Get the primitive (singleton) operator used: one of Token.FEQ, Token.FNE, Token.FLT, Token.FGT,
     * Token.FLE, Token.FGE
     */

    public int getSingletonOperator() {
        return singletonOperator;
    }

    /**
     * Determine whether untyped atomic values should be converted to the type of the other operand
     *
     * @return true if untyped values should be converted to the type of the other operand, false if they
     *         should be converted to strings.
     */

    public boolean convertsUntypedToOther() {
        return true;
    }

    /**
     * Determine the static cardinality. Returns [1..1]
     */

    public int computeCardinality() {
        return StaticProperty.EXACTLY_ONE;
    }

    /**
     * Type-check the expression
     *
     * @return the checked expression
     */

    /*@NotNull*/
    public Expression typeCheck(ExpressionVisitor visitor, ContextItemStaticInfo contextInfo) throws XPathException {

        final Configuration config = visitor.getConfiguration();
        final TypeHierarchy th = config.getTypeHierarchy();

        Expression oldOp0 = operand0;
        Expression oldOp1 = operand1;

        operand0 = visitor.typeCheck(operand0, contextInfo);
        operand1 = visitor.typeCheck(operand1, contextInfo);

        // If either operand is statically empty, return false

        if (Literal.isEmptySequence(operand0) || Literal.isEmptySequence(operand1)) {
            return Literal.makeLiteral(BooleanValue.FALSE, getContainer());
        }

        // Neither operand needs to be sorted

        operand0 = operand0.unordered(false, false);
        operand1 = operand1.unordered(false, false);

        SequenceType atomicType = SequenceType.ATOMIC_SEQUENCE;

        RoleLocator role0 = new RoleLocator(RoleLocator.BINARY_EXPR, Token.tokens[operator], 0);
        //role0.setSourceLocator(this);
        operand0 = TypeChecker.staticTypeCheck(operand0, atomicType, false, role0, visitor);

        RoleLocator role1 = new RoleLocator(RoleLocator.BINARY_EXPR, Token.tokens[operator], 1);
        //role1.setSourceLocator(this);
        operand1 = TypeChecker.staticTypeCheck(operand1, atomicType, false, role1, visitor);

        if (operand0 != oldOp0) {
            adoptChildExpression(operand0);
        }

        if (operand1 != oldOp1) {
            adoptChildExpression(operand1);
        }

        ItemType t0 = operand0.getItemType();  // this is always an atomic type or union type or xs:error
        ItemType t1 = operand1.getItemType();  // this is always an atomic type or union type or xs:error

        if (t0 instanceof ErrorType || t1 instanceof ErrorType) {
            return Literal.makeLiteral(BooleanValue.FALSE, getContainer());
        }

        if (((PlainType) t0).isExternalType() || ((PlainType) t1).isExternalType()) {
            XPathException err = new XPathException("Cannot perform comparisons involving external objects");
            err.setIsTypeError(true);
            err.setErrorCode("XPTY0004");
            err.setLocator(this);
            throw err;
        }

        BuiltInAtomicType pt0 = (BuiltInAtomicType) t0.getPrimitiveItemType();
        BuiltInAtomicType pt1 = (BuiltInAtomicType) t1.getPrimitiveItemType();

        int c0 = operand0.getCardinality();
        int c1 = operand1.getCardinality();

        if (c0 == StaticProperty.EMPTY || c1 == StaticProperty.EMPTY) {
            return Literal.makeLiteral(BooleanValue.FALSE, getContainer());
        }

        if (t0.equals(BuiltInAtomicType.ANY_ATOMIC) || t0.equals(BuiltInAtomicType.UNTYPED_ATOMIC) ||
                t1.equals(BuiltInAtomicType.ANY_ATOMIC) || t1.equals(BuiltInAtomicType.UNTYPED_ATOMIC)) {
            // then no static type checking is possible
        } else {

            if (!Type.isPossiblyComparable(pt0, pt1, Token.isOrderedOperator(singletonOperator))) {
                XPathException err = new XPathException("Cannot compare " +
                        t0.toString() + " to " + t1.toString());
                err.setErrorCode("XPTY0004");
                err.setIsTypeError(true);
                err.setLocator(this);
                throw err;
            }

        }

        needsRuntimeCheck = !Type.isGuaranteedGenerallyComparable(pt0, pt1, Token.isOrderedOperator(singletonOperator));

        if (c0 == StaticProperty.EXACTLY_ONE &&
                c1 == StaticProperty.EXACTLY_ONE &&
                !t0.equals(BuiltInAtomicType.ANY_ATOMIC) &&
                !t1.equals(BuiltInAtomicType.ANY_ATOMIC)) {

            // Use a value comparison if both arguments are singletons, and if the comparison operator to
            // be used can be determined.

            Expression e0 = operand0;
            Expression e1 = operand1;

            if (t0.equals(BuiltInAtomicType.UNTYPED_ATOMIC)) {
                if (t1.equals(BuiltInAtomicType.UNTYPED_ATOMIC)) {
                    e0 = new CastExpression(operand0, BuiltInAtomicType.STRING, false);
                    adoptChildExpression(e0);
                    e1 = new CastExpression(operand1, BuiltInAtomicType.STRING, false);
                    adoptChildExpression(e1);
                } else if (th.isSubType(t1, BuiltInAtomicType.NUMERIC)) {
                    e0 = new CastExpression(operand0, BuiltInAtomicType.DOUBLE, false);
                    adoptChildExpression(e0);
                } else {
                    e0 = new CastExpression(operand0, pt1, false);
                    adoptChildExpression(e0);
                }
            } else if (t1.equals(BuiltInAtomicType.UNTYPED_ATOMIC)) {
                if (th.isSubType(t0, BuiltInAtomicType.NUMERIC)) {
                    e1 = new CastExpression(operand1, BuiltInAtomicType.DOUBLE, false);
                    adoptChildExpression(e1);
                } else {
                    e1 = new CastExpression(operand1, pt0, false);
                    adoptChildExpression(e1);
                }
            }

            ValueComparison vc = new ValueComparison(e0, singletonOperator, e1);
            vc.setAtomicComparer(comparer);
            ExpressionTool.copyLocationInfo(this, vc);
            //return visitor.typeCheck(visitor.simplify(vc), contextInfo);
            return visitor.typeCheck(vc, contextInfo);        // bug 2225
        }

        StaticContext env = visitor.getStaticContext();
        if (comparer == null) {
            // In XSLT, only do this the first time through, otherwise default-collation may be missed
            final String defaultCollationName = env.getDefaultCollationName();
            StringCollator collation = config.getCollation(defaultCollationName);
            if (collation == null) {
                collation = CodepointCollator.getInstance();
            }
            comparer = GenericAtomicComparer.makeAtomicComparer(
                    pt0, pt1, collation, config.getConversionContext());
        }

//        Expression e0 = operand0;
//        Expression e1 = operand1;
//
//        // If one operand is numeric, convert untypedAtomic values in the other operand to numeric
//
//        boolean numeric0 = th.isSubType(t0, BuiltInAtomicType.NUMERIC);
//        boolean numeric1 = th.isSubType(t1, BuiltInAtomicType.NUMERIC);
//        if (numeric1 && !numeric0) {
//            RoleLocator role = new RoleLocator(RoleLocator.BINARY_EXPR, Token.tokens[operator], 0);
//            e0 = TypeChecker.staticTypeCheck(e0, SequenceType.NUMERIC_SEQUENCE, false, role, visitor);
//            if (e0 != operand0) {
//                adoptChildExpression(e0);
//                operand0 = e0;
//            }
//        }
//
//        if (numeric0 && !numeric1) {
//            RoleLocator role = new RoleLocator(RoleLocator.BINARY_EXPR, Token.tokens[operator], 1);
//            e1 = TypeChecker.staticTypeCheck(e1, SequenceType.NUMERIC_SEQUENCE, false, role, visitor);
//            if (e1 != operand1) {
//                adoptChildExpression(e1);
//                operand1 = e1;
//            }
//        }


        // evaluate the expression now if both arguments are constant

        if ((operand0 instanceof Literal) && (operand1 instanceof Literal)) {
            return Literal.makeLiteral(evaluateItem(env.makeEarlyEvaluationContext()), getContainer());
        }

        return this;
    }

    private static Expression makeMinOrMax(Expression exp, String function) {
        FunctionCall fn = SystemFunctionCall.makeSystemFunction(function, new Expression[]{exp});
        ((Minimax) fn).setIgnoreNaN(true);
        return fn;
    }

    /**
     * Optimize the expression
     *
     * @return the checked expression
     */

    /*@NotNull*/
    public Expression optimize(ExpressionVisitor visitor, ContextItemStaticInfo contextItemType) throws XPathException {

        final TypeHierarchy th = visitor.getConfiguration().getTypeHierarchy();
        final StaticContext env = visitor.getStaticContext();

        operand0 = visitor.optimize(operand0, contextItemType);
        operand1 = visitor.optimize(operand1, contextItemType);

        // If either operand is statically empty, return false

        if (Literal.isEmptySequence(operand0) || Literal.isEmptySequence(operand1)) {
            return Literal.makeLiteral(BooleanValue.FALSE, getContainer());
        }

        // Neither operand needs to be sorted

        operand0 = operand0.unordered(false, false);
        operand1 = operand1.unordered(false, false);

        if (operand0 instanceof Literal && operand1 instanceof Literal) {
            return Literal.makeLiteral(
                    SequenceTool.toGroundedValue(evaluateItem(visitor.getStaticContext().makeEarlyEvaluationContext())),
                    getContainer());
        }

        ItemType t0 = operand0.getItemType();
        ItemType t1 = operand1.getItemType();

        int c0 = operand0.getCardinality();
        int c1 = operand1.getCardinality();

        // Check if neither argument allows a sequence of >1

        boolean many0 = Cardinality.allowsMany(c0);
        boolean many1 = Cardinality.allowsMany(c1);

        if (many0) {
            if (many1) {
                comparisonCardinality = MANY_TO_MANY;
            } else {
                comparisonCardinality = MANY_TO_ONE;
            }
        } else {
            if (many1) {
                GeneralComparison mc = getInverseComparison();
                mc.comparisonCardinality = MANY_TO_ONE;
                ExpressionTool.copyLocationInfo(this, mc);
                mc.comparer = comparer;
                mc.needsRuntimeCheck = needsRuntimeCheck;
                return visitor.optimize(mc, contextItemType);
            } else {
                comparisonCardinality = ONE_TO_ONE;
            }
        }

        // look for (N to M = I)
        // First a variable range...

        if (operand0 instanceof RangeExpression &&
                th.isSubType(operand1.getItemType(), BuiltInAtomicType.NUMERIC) &&
                operator == Token.EQUALS &&
                !Cardinality.allowsMany(operand1.getCardinality())) {
            Expression min = ((RangeExpression) operand0).operand0;
            Expression max = ((RangeExpression) operand0).operand1;
            IntegerRangeTest ir = new IntegerRangeTest(operand1, min, max);
            ExpressionTool.copyLocationInfo(this, ir);
            return ir;
        }

        // Now a fixed range...

        if (operand0 instanceof Literal) {
            GroundedValue value0 = ((Literal) operand0).getValue();
            if (value0 instanceof IntegerRange &&
                    th.isSubType(operand1.getItemType(), BuiltInAtomicType.NUMERIC) &&
                    operator == Token.EQUALS &&
                    !Cardinality.allowsMany(operand1.getCardinality())) {
                long min = ((IntegerRange) value0).getStart();
                long max = ((IntegerRange) value0).getEnd();
                IntegerRangeTest ir = new IntegerRangeTest(operand1,
                        Literal.makeLiteral(Int64Value.makeIntegerValue(min), getContainer()),
                        Literal.makeLiteral(Int64Value.makeIntegerValue(max), getContainer()));
                ExpressionTool.copyLocationInfo(this, ir);
                return ir;
            }
        }

        // If the operator is gt, ge, lt, le then replace X < Y by min(X) < max(Y)

        // This optimization is done only in the case where at least one of the
        // sequences is known to be purely numeric. It isn't safe if both sequences
        // contain untyped atomic values, because in that case, the type of the
        // comparison isn't known in advance. For example [(1, U1) < ("fred", U2)]
        // involves both string and numeric comparisons.

        // Only do this optimization for a many-to-many comparison, because it prevents
        // early exit on a many-to-one comparison.

        if (operator != Token.EQUALS && operator != Token.NE && comparisonCardinality == MANY_TO_MANY &&
                (th.isSubType(t0, BuiltInAtomicType.NUMERIC) || th.isSubType(t1, BuiltInAtomicType.NUMERIC))) {

            // System.err.println("** using minimax optimization **");
            ValueComparison vc;
            switch (operator) {
                case Token.LT:
                case Token.LE:
                    vc = new ValueComparison(makeMinOrMax(operand0, "min"),
                            singletonOperator,
                            makeMinOrMax(operand1, "max"));
                    vc.setResultWhenEmpty(BooleanValue.FALSE);
                    vc.setAtomicComparer(comparer);
                    break;
                case Token.GT:
                case Token.GE:
                    vc = new ValueComparison(makeMinOrMax(operand0, "max"),
                            singletonOperator,
                            makeMinOrMax(operand1, "min"));
                    vc.setResultWhenEmpty(BooleanValue.FALSE);
                    vc.setAtomicComparer(comparer);
                    break;
                default:
                    throw new UnsupportedOperationException("Unknown operator " + operator);
            }

            ExpressionTool.copyLocationInfo(this, vc);
            return visitor.typeCheck(vc, contextItemType);
        }

        // evaluate the expression now if both arguments are constant

        if ((operand0 instanceof Literal) && (operand1 instanceof Literal)) {
            return Literal.makeLiteral(evaluateItem(env.makeEarlyEvaluationContext()), getContainer());
        }

        // Finally, convert to use the GeneralComparisonEE algorithm if in Saxon-EE
        return visitor.getConfiguration().obtainOptimizer().optimizeGeneralComparison(this, false);
    }


    /**
     * Copy an expression. This makes a deep copy.
     *
     * @return the copy of the original expression
     */

    /*@NotNull*/
    public Expression copy() {
        GeneralComparison gc = new GeneralComparison(operand0.copy(), operator, operand1.copy());
        gc.comparer = comparer;
        gc.singletonOperator = singletonOperator;
        gc.needsRuntimeCheck = needsRuntimeCheck;
        gc.comparisonCardinality = comparisonCardinality;
        return gc;
    }

    /**
     * Evaluate the expression in a given context
     *
     * @param context the given context for evaluation
     * @return a BooleanValue representing the result of the numeric comparison of the two operands
     */

    /*@Nullable*/
    public BooleanValue evaluateItem(XPathContext context) throws XPathException {
        switch (comparisonCardinality) {
            case ONE_TO_ONE: {
                AtomicValue value0 = (AtomicValue) operand0.evaluateItem(context);
                AtomicValue value1 = (AtomicValue) operand1.evaluateItem(context);
                return BooleanValue.get(evaluateOneToOne(value0, value1, context));
            }
            case MANY_TO_ONE: {
                SequenceIterator iter0 = operand0.iterate(context);
                AtomicValue value1 = (AtomicValue) operand1.evaluateItem(context);
                return BooleanValue.get(evaluateManyToOne(iter0, value1, context));
            }
            case MANY_TO_MANY: {
                SequenceIterator iter1 = operand0.iterate(context);
                SequenceIterator iter2 = operand1.iterate(context);
                return BooleanValue.get(evaluateManyToMany(iter1, iter2, context));
            }
        }
        return null;
    }

    /**
     * Evaluate the expression
     *
     * @param context   the dynamic evaluation context
     * @param arguments the values of the arguments, supplied as SequenceIterators
     * @return the result of the evaluation, in the form of a SequenceIterator
     * @throws XPathException if a dynamic error occurs during the evaluation of the expression
     */

    public Sequence call(XPathContext context, Sequence[] arguments) throws XPathException {
        switch (comparisonCardinality) {
            case ONE_TO_ONE: {
                AtomicValue value0 = (AtomicValue) arguments[0].head();
                AtomicValue value1 = (AtomicValue) arguments[1].head();
                return BooleanValue.get(evaluateOneToOne(value0, value1, context));
            }
            case MANY_TO_ONE: {
                SequenceIterator iter0 = arguments[0].iterate();
                AtomicValue value1 = (AtomicValue) arguments[1].head();
                return BooleanValue.get(evaluateManyToOne(iter0, value1, context));
            }
            case MANY_TO_MANY: {
                SequenceIterator iter1 = arguments[0].iterate();
                SequenceIterator iter2 = arguments[1].iterate();
                return BooleanValue.get(evaluateManyToMany(iter1, iter2, context));
            }
        }
        return null;
    }

    /**
     * Evaluate the expression in a boolean context
     *
     * @param context the given context for evaluation
     * @return a boolean representing the result of the numeric comparison of the two operands
     */

    public boolean effectiveBooleanValue(XPathContext context) throws XPathException {
        switch (comparisonCardinality) {
            case ONE_TO_ONE: {
                AtomicValue value0 = (AtomicValue) operand0.evaluateItem(context);
                AtomicValue value1 = (AtomicValue) operand1.evaluateItem(context);
                return evaluateOneToOne(value0, value1, context);
            }
            case MANY_TO_ONE: {
                SequenceIterator iter0 = operand0.iterate(context);
                AtomicValue value1 = (AtomicValue) operand1.evaluateItem(context);
                return evaluateManyToOne(iter0, value1, context);
            }
            case MANY_TO_MANY: {
                SequenceIterator iter1 = operand0.iterate(context);
                SequenceIterator iter2 = operand1.iterate(context);
                return evaluateManyToMany(iter1, iter2, context);
            }
        }
        return false;
    }

    /**
     * Evaluate a (zero-or-one)-to-(zero-or-one) comparison
     *
     * @param value0  the first value, or null if empty
     * @param value1  the second value, or null if empty
     * @param context dynamic evaluation context
     * @return the comparison result
     * @throws XPathException if a dynamic error occurs
     */

    private boolean evaluateOneToOne(AtomicValue value0, AtomicValue value1, XPathContext context) throws XPathException {
        try {
            return !(value0 == null || value1 == null) &&
                    compare(value0, singletonOperator, value1, comparer, needsRuntimeCheck, context);
        } catch (XPathException e) {
            // re-throw the exception with location information added
            e.maybeSetLocation(this);
            e.maybeSetContext(context);
            throw e;
        }

    }

    /**
     * Evaluate a (zero-to-many)-to-(zero-or-one) comparison
     *
     * @param iter0   iterator over the first value
     * @param value1  the second value, or null if empty
     * @param context dynamic evaluation context
     * @return the comparison result
     * @throws XPathException if a dynamic error occurs
     */

    private boolean evaluateManyToOne(SequenceIterator iter0, AtomicValue value1, XPathContext context) throws XPathException {
        try {
            if (value1 == null) {
                return false;
            }
            AtomicValue item0;
            while ((item0 = (AtomicValue) iter0.next()) != null) {
                if (compare(item0, singletonOperator, value1, comparer, needsRuntimeCheck, context)) {
                    iter0.close();
                    return true;
                }
            }
            return false;
        } catch (XPathException e) {
            // re-throw the exception with location information added
            e.maybeSetLocation(this);
            e.maybeSetContext(context);
            throw e;
        }

    }

    /**
     * Evaluate a (zero-or-one)-to-(zero-or-one) comparison
     *
     * @param iter0   iterator over the first value
     * @param iter1   iterator the second value
     * @param context dynamic evaluation context
     * @return the comparison result
     * @throws XPathException if a dynamic error occurs
     */

    public boolean evaluateManyToMany(SequenceIterator iter0, SequenceIterator iter1, XPathContext context) throws XPathException {
        try {
            boolean exhausted0 = false;
            boolean exhausted1 = false;

            List<AtomicValue> value0 = new ArrayList<AtomicValue>();
            List<AtomicValue> value1 = new ArrayList<AtomicValue>();

            // Read items from the two sequences alternately, in each case comparing the item to
            // all items that have previously been read from the other sequence. In the worst case
            // the number of comparisons is N*M, and the memory usage is (max(N,M)*2) where N and M
            // are the number of items in the two sequences. In practice, either M or N is often 1,
            // meaning that in this case neither list will ever hold more than one item.

            while (true) {
                if (!exhausted0) {
                    AtomicValue item0 = (AtomicValue) iter0.next();
                    if (item0 == null) {
                        if (exhausted1) {
                            return false;
                        }
                        exhausted0 = true;
                    } else {
                        for (AtomicValue item1 : value1) {
                            if (compare(item0, singletonOperator, item1, comparer, needsRuntimeCheck, context)) {
                                iter0.close();
                                iter1.close();
                                return true;
                            }
                        }
                        if (!exhausted1) {
                            value0.add(item0);
                        }
                    }
                }
                if (!exhausted1) {
                    AtomicValue item1 = (AtomicValue) iter1.next();
                    if (item1 == null) {
                        if (exhausted0) {
                            return false;
                        }
                        exhausted1 = true;
                    } else {
                        for (AtomicValue item0 : value0) {
                            if (compare(item0, singletonOperator, item1, comparer, needsRuntimeCheck, context)) {
                                iter0.close();
                                iter1.close();
                                return true;
                            }
                        }
                        if (!exhausted0) {
                            value1.add(item1);
                        }
                    }
                }
            }
        } catch (XPathException e) {
            // re-throw the exception with location information added
            e.maybeSetLocation(this);
            e.maybeSetContext(context);
            throw e;
        }

    }

    /**
     * Compare two atomic values
     *
     * @param a0         the first value
     * @param operator   the singleton version of the comparison operator,
     *                   for example {@link net.sf.saxon.expr.parser.Token#FEQ}
     * @param a1         the second value
     * @param comparer   the comparer to be used to perform the comparison
     * @param checkTypes set to true if the operand types need to be checked for comparability at runtime
     * @param context    the XPath evaluation context
     * @return true if the comparison succeeds
     * @throws XPathException if a dynamic error occurs during the comparison
     */

    public static boolean compare(AtomicValue a0,
                                  int operator,
                                  AtomicValue a1,
                                  AtomicComparer comparer,
                                  boolean checkTypes,
                                  XPathContext context) throws XPathException {

        final ConversionRules rules = context.getConfiguration().getConversionRules();
        boolean u0 = a0 instanceof UntypedAtomicValue;
        boolean u1 = a1 instanceof UntypedAtomicValue;
        if (u0 != u1) {
            // one value untyped, the other not
            if (u0) {
                // a0 is untyped atomic
                if (a1 instanceof NumericValue) {
                    return quickCompare((UntypedAtomicValue)a0, (NumericValue)a1, operator, rules);
                } else {
                    a0 = Converter.convert(a0, a1.getPrimitiveType(), rules).asAtomic();
                }
            } else {
                // a1 is untyped atomic
                if (a0 instanceof NumericValue) {
                    return quickCompare((UntypedAtomicValue)a1, (NumericValue)a0, Token.inverse(operator), rules);
                } else {
                    a1 = Converter.convert(a1, a0.getPrimitiveType(), rules).asAtomic();
                }
            }
            checkTypes = false; // No further checking needed if conversion succeeded
        }
        return ValueComparison.compare(a0, operator, a1, comparer.provideContext(context), checkTypes);

    }

    /**
     * Determine the data type of the expression
     *
     * @return the value BuiltInAtomicType.BOOLEAN
     */

    /*@NotNull*/
    public ItemType getItemType() {
        return BuiltInAtomicType.BOOLEAN;
    }

    /**
     * Return the singleton form of the comparison operator, e.g. FEQ for EQUALS, FGT for GT
     *
     * @param op the many-to-many form of the operator, for example {@link Token#LE}
     * @return the corresponding singleton operator, for example {@link Token#FLE}
     */

    public static int getCorrespondingSingletonOperator(int op) {
        switch (op) {
            case Token.EQUALS:
                return Token.FEQ;
            case Token.GE:
                return Token.FGE;
            case Token.NE:
                return Token.FNE;
            case Token.LT:
                return Token.FLT;
            case Token.GT:
                return Token.FGT;
            case Token.LE:
                return Token.FLE;
            default:
                return op;
        }
    }

    protected GeneralComparison getInverseComparison() {
        return new GeneralComparison(operand1, Token.inverse(operator), operand0);
    }

//#ifdefined BYTECODE

    /**
     * Return the compiler of the GeneralComparison expression
     *
     * @return the relevant ExpressionCompiler
     */
    @Override
    public ExpressionCompiler getExpressionCompiler() {
        return new GeneralComparisonCompiler();
    }

    /**
     * Get a class that supports streamed evaluation of this expression
     *
     * @return the relevant StreamingAdjunct, or null if none is available
     */
    @Override
    public GeneralComparisonAdjunct getStreamingAdjunct() {
        return new GeneralComparisonAdjunct();
    }

    //#endif

//    protected String displayOperator() {
//        return "many-to-many " + super.displayOperator();
//    }

    protected void explainExtraAttributes(ExpressionPresenter out) {
        String cc = "";
        switch (comparisonCardinality) {
            case ONE_TO_ONE:
                cc = "one-to-one";
                break;
            case MANY_TO_ONE:
                cc = "many-to-one";
                break;
            case MANY_TO_MANY:
                cc = "many-to-many";
                break;
        }
        out.emitAttribute("cardinality", cc);
    }

    /**
     * Optimized routine to compare an untyped atomic value with a numeric value.
     * This attempts to deliver a quick answer if the comparison if obviously false,
     * without performing the full string-to-double conversion
     * @param a0 the untypedAtomic comparand
     * @param a1 the numeric comparand
     * @param operator the comparison operator: a singleton operator such as Token.FEQ
     * @return the result of the comparison
     * @throws XPathException if the first operand is not convertible to a double
     */

    public static boolean quickCompare(
            UntypedAtomicValue a0, NumericValue a1, int operator, ConversionRules rules)
            throws XPathException {
        double d1 = a1.getDoubleValue();
        double d0;
        int comp;
        DoubleValue dv0 = a0.getDoubleValueIfKnown();
        if (dv0 != null) {
            d0 = dv0.getDoubleValue();
            comp = Double.compare(d0, d1);
        } else {
            CharSequence cs = Whitespace.trimWhitespace(a0.getStringValueCS());

            boolean simple = true;
            int wholePartLength = 0;
            int firstDigit = -1;
            int decimalPoints = 0;
            char sign = '?';
            for (int i=0; i<cs.length(); i++) {
                char c = cs.charAt(i);
                if (c >= '0' && c <= '9') {
                    if (firstDigit < 0) {
                        firstDigit = c - '0';
                    }
                    if (decimalPoints == 0) {
                        wholePartLength++;
                    }
                } else if (c == '-') {
                    if (sign != '?' || wholePartLength > 0 || decimalPoints > 0) {
                        simple = false;
                        break;
                    }
                    sign = c;
                } else if (c == '.') {
                    if (decimalPoints > 0) {
                        simple = false;
                        break;
                    }
                    decimalPoints = 1;
                } else {
                    simple = false;
                    break;
                }
            }
            if (firstDigit < 0) {
                simple = false;
            }
            if (simple && wholePartLength > 0 && wholePartLength <= 10) {
                double lowerBound = bounds[firstDigit][wholePartLength];
                double upperBound = bounds[firstDigit+1][wholePartLength];
                if (sign == '-') {
                    double temp = lowerBound;
                    lowerBound = -upperBound;
                    upperBound = -temp;
                }
                switch (operator) {
                    case Token.FEQ:
                        if (lowerBound > d1 || upperBound < d1) {
                            return false;
                        }
                        break;
                    case Token.FLE:
                        if (lowerBound > d1) {
                            return false;
                        }
                        if (upperBound <= d1) {
                            return true;
                        }
                        break;
                    case Token.FLT:
                        if (lowerBound >= d1) {
                            return false;
                        }
                        if (upperBound < d1) {
                            return true;
                        }
                        break;
                    case Token.FGE:
                        if (upperBound < d1) {
                            return false;
                        }
                        if (lowerBound >= d1) {
                            return true;
                        }
                        break;
                    case Token.FGT:
                        if (upperBound <= d1) {
                            return false;
                        }
                        if (lowerBound > d1) {
                            return true;
                        }
                        break;
                    case Token.FNE:
                        if (upperBound < d1 || lowerBound > d1) {
                            return true;
                        }
                        break;
                    default:
                        break;
                }
            }
            if (simple && decimalPoints == 0 && wholePartLength <= 15 && a1 instanceof Int64Value) {
                long l0 = Long.parseLong(cs.toString());
                comp = new Long(l0).compareTo(a1.longValue());
            } else {
                d0 = ((DoubleValue)BuiltInAtomicType.DOUBLE.getStringConverter(rules).convertString(cs).asAtomic()).getDoubleValue();
                a0.setDoubleValue(new DoubleValue(d0));
                comp = Double.compare(d0, d1);
            }
        }
        switch (operator) {
            case Token.FEQ:
                return comp == 0;
            case Token.FLE:
                return comp <= 0;
            case Token.FLT:
                return comp < 0;
            case Token.FGE:
                return comp >= 0;
            case Token.FGT:
                return comp > 0;
            case Token.FNE:
            default:
                return comp != 0;
        }
    }

    private static double[][] bounds = {
            {1, 0e0, 0e1, 0e2, 0e3, 0e4, 0e5, 0e6, 0e7, 0e8, 0e9, 0e10},
            {1, 1e0, 1e1, 1e2, 1e3, 1e4, 1e5, 1e6, 1e7, 1e8, 1e9, 1e10},
            {1, 2e0, 2e1, 2e2, 2e3, 2e4, 2e5, 2e6, 2e7, 2e8, 2e9, 2e10},
            {1, 3e0, 3e1, 3e2, 3e3, 3e4, 3e5, 3e6, 3e7, 3e8, 3e9, 3e10},
            {1, 4e0, 4e1, 4e2, 4e3, 4e4, 4e5, 4e6, 4e7, 4e8, 4e9, 4e10},
            {1, 5e0, 5e1, 5e2, 5e3, 5e4, 5e5, 5e6, 5e7, 5e8, 5e9, 5e10},
            {1, 6e0, 6e1, 6e2, 6e3, 6e4, 6e5, 6e6, 6e7, 6e8, 6e9, 6e10},
            {1, 7e0, 7e1, 7e2, 7e3, 7e4, 7e5, 7e6, 7e7, 7e8, 7e9, 7e10},
            {1, 8e0, 8e1, 8e2, 8e3, 8e4, 8e5, 8e6, 8e7, 8e8, 8e9, 8e10},
            {1, 9e0, 9e1, 9e2, 9e3, 9e4, 9e5, 9e6, 9e7, 9e8, 9e9, 9e10},
            {1, 10e0, 10e1, 10e2, 10e3, 10e4, 10e5, 10e6, 10e7, 10e8, 10e9, 10e10}
    };

}

