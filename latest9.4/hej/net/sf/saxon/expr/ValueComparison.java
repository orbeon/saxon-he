package net.sf.saxon.expr;

import net.sf.saxon.expr.parser.*;
import net.sf.saxon.expr.sort.AtomicComparer;
import net.sf.saxon.expr.sort.CodepointCollatingComparer;
import net.sf.saxon.expr.sort.CodepointCollator;
import net.sf.saxon.expr.sort.GenericAtomicComparer;
import net.sf.saxon.functions.*;
import net.sf.saxon.lib.StringCollator;
import net.sf.saxon.om.Item;
import net.sf.saxon.om.NamePool;
import net.sf.saxon.om.StandardNames;
import net.sf.saxon.trace.ExpressionPresenter;
import net.sf.saxon.trans.NoDynamicContextException;
import net.sf.saxon.trans.XPathException;
import net.sf.saxon.type.*;
import net.sf.saxon.value.*;

import java.io.IOException;
import java.io.ObjectInputStream;

/**
 * ValueComparison: a boolean expression that compares two atomic values
 * for equals, not-equals, greater-than or less-than. Implements the operators
 * eq, ne, lt, le, gt, ge
 */

public final class ValueComparison extends BinaryExpression implements ComparisonExpression, Negatable {

    private AtomicComparer comparer;
    /*@Nullable*/ private BooleanValue resultWhenEmpty = null;
    private boolean needsRuntimeCheck;

    /**
     * Create a comparison expression identifying the two operands and the operator
     *
     * @param p1 the left-hand operand
     * @param op the operator, as a token returned by the Tokenizer (e.g. Token.LT)
     * @param p2 the right-hand operand
     */

    public ValueComparison(Expression p1, int op, Expression p2) {
        super(p1, op, p2);
    }

    /**
     * Get a name identifying the kind of expression, in terms meaningful to a user.
     * @return a name identifying the kind of expression, in terms meaningful to a user.
     *         The name will always be in the form of a lexical XML QName, and should match the name used
     *         in explain() output displaying the expression.
     */

    public String getExpressionName() {
        return "ValueComparison";
    }

    /**
     * Deserialization method ensures that there is only one BooleanValue.TRUE and only one BooleanValue.FALSE
     * @param in the input stream
     */

    private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
        in.defaultReadObject();
        if (resultWhenEmpty != null) {
            resultWhenEmpty = (resultWhenEmpty.getBooleanValue() ? BooleanValue.TRUE : BooleanValue.FALSE);
        }
    }

    /**
     * Set the AtomicComparer used to compare atomic values
     * @param comparer the AtomicComparer
     */

    public void setAtomicComparer(AtomicComparer comparer) {
        this.comparer = comparer;
    }

    /**
     * Get the AtomicComparer used to compare atomic values. This encapsulates any collation that is used.
     * Note that the comparer is always known at compile time.
     */

    public AtomicComparer getAtomicComparer() {
        return comparer;
    }

    /**
     * Get the primitive (singleton) operator used: one of Token.FEQ, Token.FNE, Token.FLT, Token.FGT,
     * Token.FLE, Token.FGE
     */

    public int getSingletonOperator() {
        return operator;
    }

    /**
     * Determine whether untyped atomic values should be converted to the type of the other operand
     *
     * @return true if untyped values should be converted to the type of the other operand, false if they
     *         should be converted to strings.
     */

    public boolean convertsUntypedToOther() {
        return false;
    }

    /**
     * Set the result to be returned if one of the operands is an empty sequence
     * @param value the result to be returned if an operand is empty. Supply null to mean the empty sequence.
     */

    public void setResultWhenEmpty(BooleanValue value) {
        resultWhenEmpty = value;
    }

    /**
     * Get the result to be returned if one of the operands is an empty sequence
     * @return BooleanValue.TRUE, BooleanValue.FALSE, or null (meaning the empty sequence)
     */

    public BooleanValue getResultWhenEmpty() {
        return resultWhenEmpty;
    }

    /**
     * Determine whether a run-time check is needed to check that the types of the arguments
     * are comparable
     * @return true if a run-time check is needed
     */

    public boolean needsRuntimeComparabilityCheck() {
        return needsRuntimeCheck;
    }

    /**
     * Type-check the expression
     */

    /*@NotNull*/
    public Expression typeCheck(ExpressionVisitor visitor, ExpressionVisitor.ContextItemType contextItemType) throws XPathException {

        NamePool namePool = visitor.getConfiguration().getNamePool();
        TypeHierarchy th = visitor.getConfiguration().getTypeHierarchy();
        StaticContext env = visitor.getStaticContext();

        operand0 = visitor.typeCheck(operand0, contextItemType);
        if (Literal.isEmptySequence(operand0)) {
            return (resultWhenEmpty == null ? operand0 : Literal.makeLiteral(resultWhenEmpty));
        }

        operand1 = visitor.typeCheck(operand1, contextItemType);
        if (Literal.isEmptySequence(operand1)) {
            return (resultWhenEmpty == null ? operand1 : Literal.makeLiteral(resultWhenEmpty));
        }

        final SequenceType optionalAtomic = SequenceType.OPTIONAL_ATOMIC;

        RoleLocator role0 = new RoleLocator(RoleLocator.BINARY_EXPR, Token.tokens[operator], 0);
        operand0 = TypeChecker.staticTypeCheck(operand0, optionalAtomic, false, role0, visitor);

        RoleLocator role1 = new RoleLocator(RoleLocator.BINARY_EXPR, Token.tokens[operator], 1);
        operand1 = TypeChecker.staticTypeCheck(operand1, optionalAtomic, false, role1, visitor);

        PlainType t0 = operand0.getItemType(th).getAtomizedItemType();
        PlainType t1 = operand1.getItemType(th).getAtomizedItemType();

        if (t0.isExternalType() || t1.isExternalType()) {
            XPathException err = new XPathException("Cannot perform comparisons involving external objects");
            err.setIsTypeError(true);
            err.setErrorCode("XPTY0004");
            err.setLocator(this);
            throw err;
        }

        BuiltInAtomicType p0 = (BuiltInAtomicType)t0.getPrimitiveItemType();
        if (p0.equals(BuiltInAtomicType.UNTYPED_ATOMIC)) {
            p0 = BuiltInAtomicType.STRING;
        }
        BuiltInAtomicType p1 = (BuiltInAtomicType)t1.getPrimitiveItemType();
        if (p1.equals(BuiltInAtomicType.UNTYPED_ATOMIC)) {
            p1 = BuiltInAtomicType.STRING;
        }

        needsRuntimeCheck =
                p0.equals(BuiltInAtomicType.ANY_ATOMIC) || p1.equals(BuiltInAtomicType.ANY_ATOMIC);

        if (!needsRuntimeCheck && !Type.isComparable(p0, p1, Token.isOrderedOperator(operator))) {
            boolean opt0 = Cardinality.allowsZero(operand0.getCardinality());
            boolean opt1 = Cardinality.allowsZero(operand1.getCardinality());
            if (opt0 || opt1) {
                // This is a comparison such as (xs:integer? eq xs:date?). This is almost
                // certainly an error, but we need to let it through because it will work if
                // one of the operands is an empty sequence.

                String which = null;
                if (opt0) which = "the first operand is";
                if (opt1) which = "the second operand is";
                if (opt0 && opt1) which = "one or both operands are";

                visitor.getStaticContext().issueWarning("Comparison of " + t0.toString(namePool) +
                        (opt0 ? "?" : "") + " to " + t1.toString(namePool) +
                        (opt1 ? "?" : "") + " will fail unless " + which + " empty", this);
                needsRuntimeCheck = true;
            } else {
                XPathException err = new XPathException("Cannot compare " + t0.toString(namePool) +
                        " to " + t1.toString(namePool));
                err.setIsTypeError(true);
                err.setErrorCode("XPTY0004");
                err.setLocator(this);
                throw err;
            }
        }
        if (!(operator == Token.FEQ || operator == Token.FNE)) {
            if (!p0.isOrdered()) {
                XPathException err = new XPathException("Type " + t0.toString(env.getNamePool()) + " is not an ordered type");
                err.setErrorCode("XPTY0004");
                err.setIsTypeError(true);
                err.setLocator(this);
                throw err;
            }
            if (!p1.isOrdered()) {
                XPathException err = new XPathException("Type " + t1.toString(env.getNamePool()) + " is not an ordered type");
                err.setErrorCode("XPTY0004");
                err.setIsTypeError(true);
                err.setLocator(this);
                throw err;
            }
        }

        if (comparer == null) {
            // In XSLT, only do this the first time through, otherwise the default-collation attribute may be missed
            final String defaultCollationName = env.getDefaultCollationName();
            StringCollator comp = env.getCollation(defaultCollationName);
            if (comp == null) {
                comp = CodepointCollator.getInstance();
            }
            comparer = GenericAtomicComparer.makeAtomicComparer(
                    p0, p1, comp, env.getConfiguration().getConversionContext());
        }
        return this;
    }

    /**
     * Perform optimisation of an expression and its subexpressions.
     * <p/>
     * <p>This method is called after all references to functions and variables have been resolved
     * to the declaration of the function or variable, and after all type checking has been done.</p>
     *
     * @param visitor an expression visitor
     * @param contextItemType the static type of "." at the point where this expression is invoked.
     *                        The parameter is set to null if it is known statically that the context item will be undefined.
     *                        If the type of the context item is not known statically, the argument is set to
     *                        {@link net.sf.saxon.type.Type#ITEM_TYPE}
     * @return the original expression, rewritten if appropriate to optimize execution
     * @throws XPathException if an error is discovered during this phase
     *                                        (typically a type error)
     */

    /*@NotNull*/
    public Expression optimize(ExpressionVisitor visitor, ExpressionVisitor.ContextItemType contextItemType) throws XPathException {

        Optimizer opt = visitor.getConfiguration().obtainOptimizer();
        TypeHierarchy th = visitor.getConfiguration().getTypeHierarchy();

        operand0 = visitor.optimize(operand0, contextItemType);
        operand1 = visitor.optimize(operand1, contextItemType);

        Value value0 = null;
        Value value1 = null;

        if (operand0 instanceof Literal) {
            value0 = ((Literal)operand0).getValue();
        }

        if (operand1 instanceof Literal) {
            value1 = ((Literal)operand1).getValue();
        }

        // evaluate the expression now if both arguments are constant

        if ((value0 != null) && (value1 != null)) {
            try {
                AtomicValue r = (AtomicValue)evaluateItem(visitor.getStaticContext().makeEarlyEvaluationContext());
                //noinspection RedundantCast
                return Literal.makeLiteral(r == null ? (Value)EmptySequence.getInstance() : (Value)r);
            } catch (NoDynamicContextException e) {
                // early evaluation failed, typically because the implicit context isn't available.
                // Try again at run-time
                return this;
            }
        }        

        // optimise count(x) eq N (or gt N, ne N, eq N, etc)

        if (operand0 instanceof Count && Literal.isAtomic(operand1)) {
            Expression e2 = optimizeCount(visitor, contextItemType, false);
            if (e2 != null) {
                return visitor.optimize(e2, contextItemType);
            }
        } else if (operand1 instanceof Count && Literal.isAtomic(operand0)) {
            Expression e2 = optimizeCount(visitor, contextItemType, true);
            if (e2 != null) {
                return visitor.optimize(e2, contextItemType);
            }
        }


        // optimise string-length(x) = 0, >0, !=0 etc

        if ((operand0 instanceof StringLength) &&
                (((StringLength) operand0).getNumberOfArguments() == 1) &&
                isZero(value1)) {
            Expression arg = (((StringLength)operand0).getArguments()[0]);
            switch (operator) {
                case Token.FEQ:
                case Token.FLE:
                    return SystemFunction.makeSystemFunction("not", new Expression[]{arg});
                case Token.FNE:
                case Token.FGT:
                    return SystemFunction.makeSystemFunction("boolean", new Expression[]{arg});
                case Token.FGE:
                    return Literal.makeLiteral(BooleanValue.TRUE);
                case Token.FLT:
                    return Literal.makeLiteral(BooleanValue.FALSE);
            }
        }

        // optimise (0 = string-length(x)), etc

        if ((operand1 instanceof StringLength) &&
                        (((StringLength) operand1).getNumberOfArguments() == 1) &&
                        isZero(value0)) {
            Expression arg = (((StringLength)operand1).getArguments()[0]);
            switch (operator) {
                case Token.FEQ:
                case Token.FGE:
                    return SystemFunction.makeSystemFunction("not", new Expression[]{arg});
                case Token.FNE:
                case Token.FLT:
                    return SystemFunction.makeSystemFunction("boolean", new Expression[]{arg});
                case Token.FLE:
                    return Literal.makeLiteral(BooleanValue.TRUE);
                case Token.FGT:
                    return Literal.makeLiteral(BooleanValue.FALSE);
            }
        }

        // optimise string="" etc
        // Note we can change S!="" to boolean(S) for cardinality zero-or-one, but we can only
        // change S="" to not(S) for cardinality exactly-one.

        int p0 = operand0.getItemType(th).getPrimitiveType();
        if ((p0 == StandardNames.XS_STRING ||
                p0 == StandardNames.XS_ANY_URI ||
                p0 == StandardNames.XS_UNTYPED_ATOMIC) &&
                operand1 instanceof Literal &&
                ((Literal)operand1).getValue() instanceof StringValue &&
                ((StringValue)((Literal)operand1).getValue()).isZeroLength() &&
                comparer instanceof CodepointCollatingComparer) {

            switch (operator) {
                case Token.FNE:
                case Token.FGT:
                    return SystemFunction.makeSystemFunction("boolean", new Expression[]{operand0});
                case Token.FEQ:
                case Token.FLE:
                    if (operand0.getCardinality() == StaticProperty.EXACTLY_ONE) {
                        return SystemFunction.makeSystemFunction("not", new Expression[]{operand0});
                    }
            }
        }

        // optimize "" = string etc

        int p1 = operand1.getItemType(th).getPrimitiveType();
        if ((p1 == StandardNames.XS_STRING ||
                p1 == StandardNames.XS_ANY_URI ||
                p1 == StandardNames.XS_UNTYPED_ATOMIC) &&
                operand0 instanceof Literal &&
                ((Literal)operand0).getValue() instanceof StringValue &&
                ((StringValue)((Literal)operand0).getValue()).isZeroLength() &&
                comparer instanceof CodepointCollatingComparer) {

            switch (operator) {
                case Token.FNE:
                case Token.FLT:
                    return SystemFunction.makeSystemFunction("boolean", new Expression[]{operand1});
                case Token.FEQ:
                case Token.FGE:
                    if (operand1.getCardinality() == StaticProperty.EXACTLY_ONE) {
                        return SystemFunction.makeSystemFunction("not", new Expression[]{operand1});
                    }
            }
        }


        // optimise [position()=last()] etc

        if ((operand0 instanceof Position) && (operand1 instanceof Last)) {
            switch (operator) {
                case Token.FEQ:
                case Token.FGE:
                    IsLastExpression iletrue = new IsLastExpression(true);
                    ExpressionTool.copyLocationInfo(this, iletrue);
                    return iletrue;
                case Token.FNE:
                case Token.FLT:
                    IsLastExpression ilefalse = new IsLastExpression(false);
                    ExpressionTool.copyLocationInfo(this, ilefalse);
                    return ilefalse;
                case Token.FGT:
                    return Literal.makeLiteral(BooleanValue.FALSE);
                case Token.FLE:
                    return Literal.makeLiteral(BooleanValue.TRUE);
            }
        }
        if ((operand0 instanceof Last) && (operand1 instanceof Position)) {
            switch (operator) {
                case Token.FEQ:
                case Token.FLE:
                    IsLastExpression iletrue = new IsLastExpression(true);
                    ExpressionTool.copyLocationInfo(this, iletrue);
                    return iletrue;
                case Token.FNE:
                case Token.FGT:
                    IsLastExpression ilefalse = new IsLastExpression(false);
                    ExpressionTool.copyLocationInfo(this, ilefalse);
                    return ilefalse;
                case Token.FLT:
                    return Literal.makeLiteral(BooleanValue.FALSE);
                case Token.FGE:
                    return Literal.makeLiteral(BooleanValue.TRUE);
            }
        }

        // optimize comparison against an integer constant

        if (value1 instanceof Int64Value &&
                operand0.getCardinality() == StaticProperty.EXACTLY_ONE &&
                th.isSubType(operand0.getItemType(th), BuiltInAtomicType.NUMERIC)) {
            return new CompareToIntegerConstant(operand0, operator, ((Int64Value)value1).longValue());
        }

        if (value0 instanceof Int64Value &&
                operand1.getCardinality() == StaticProperty.EXACTLY_ONE &&
                th.isSubType(operand1.getItemType(th), BuiltInAtomicType.NUMERIC)) {
            return new CompareToIntegerConstant(operand1, Token.inverse(operator), ((Int64Value) value0).longValue());
        }

        // optimize (boolean expression) == (boolean literal)

        if (p0 == StandardNames.XS_BOOLEAN &&
                p1 == StandardNames.XS_BOOLEAN &&
                (operator == Token.FEQ || operator == Token.FNE) &&
                operand0.getCardinality() == StaticProperty.EXACTLY_ONE &&
                operand1.getCardinality() == StaticProperty.EXACTLY_ONE &&
                (operand0 instanceof Literal || operand1 instanceof Literal)) {
            Literal literal = (Literal)(operand0 instanceof Literal ? operand0 : operand1);
            Expression other = (operand0 instanceof Literal ? operand1 : operand0);
            boolean negate = ((operator == Token.FEQ) != (((BooleanValue)literal.getValue()).getBooleanValue()));
            if (negate) {
                NotFn fn = (NotFn)SystemFunction.makeSystemFunction("not", new Expression[]{other});
                ExpressionTool.copyLocationInfo(this, fn);
                return fn.optimize(visitor, contextItemType);
            } else {
                return other;
            }
        }


        // optimize generate-id(X) = generate-id(Y) as "X is Y"
        // This construct is often used in XSLT 1.0 stylesheets.
        // Only do this if we know the arguments are singletons, because "is" doesn't
        // do first-value extraction.

        if (operand0 instanceof GenerateId && operand1 instanceof GenerateId) {
            GenerateId f0 = (GenerateId) operand0;
            GenerateId f1 = (GenerateId) operand1;
            if (!Cardinality.allowsMany(f0.argument[0].getCardinality()) &&
                    !Cardinality.allowsMany(f1.argument[0].getCardinality()) &&
                    (operator == Token.FEQ)) {
                IdentityComparison id =
                        new IdentityComparison(f0.argument[0],
                                Token.IS,
                                f1.argument[0]);
                id.setGenerateIdEmulation(true);
                ExpressionTool.copyLocationInfo(this, id);
                return visitor.optimize(visitor.typeCheck(visitor.simplify(id), contextItemType), contextItemType);
            }
        }

        return this;
    }

    /**
     * Optimize comparisons of count(X) to a literal value. The objective here is to count items in the sequence only
     * until the result of the comparison is deducible; for example to evaluate (count(X)>2) we can stop at the third
     * item in the sequence.
     * @param visitor the expression visitor
     * @param contextItemType the context item type
     * @param inverted true if the call to count(X) is the right-hand operand
     * @return the optimized expression, or null if no optimization is possible
     * @throws XPathException if an error occurs
     */

    /*@Nullable*/
    private Expression optimizeCount(ExpressionVisitor visitor, ExpressionVisitor.ContextItemType contextItemType, boolean inverted) throws XPathException {
        Count countFn = (Count)(inverted ? operand1 : operand0);
        Expression sequence = countFn.argument[0];
        Optimizer opt = visitor.getConfiguration().obtainOptimizer();
        sequence = ExpressionTool.unsorted(opt, sequence, false);

        AtomicValue literalOperand = (AtomicValue)((Literal)(inverted ? operand0 : operand1)).getValue();
        int op = (inverted ? Token.inverse(operator) : operator);

        if (isZero(literalOperand)) {
            if (op == Token.FEQ || op == Token.FLE) {
                // rewrite count(x)=0 as empty(x)
                Expression result = SystemFunction.makeSystemFunction(
                        "empty", new Expression[]{sequence});
                opt.trace("Rewrite count()=0 as:", result);
                return result;
            } else if (op == Token.FNE || op == Token.FGT) {
                // rewrite count(x)!=0, count(x)>0 as exists(x)
                Expression result = SystemFunction.makeSystemFunction("exists", new Expression[] {sequence});
                opt.trace("Rewrite count()>0 as:", result);
                return result;
            } else if (op == Token.FGE) {
                // rewrite count(x)>=0 as true()
                return Literal.makeLiteral(BooleanValue.TRUE);
            } else {  // singletonOperator == Token.FLT
                // rewrite count(x)<0 as false()
                return Literal.makeLiteral(BooleanValue.FALSE);
            }
        } else if (literalOperand instanceof NumericValue) {
            long operand;
            if (literalOperand instanceof IntegerValue) {
                operand = ((IntegerValue) literalOperand).longValue();
            } else if (literalOperand.isNaN()) {
                return new Literal(BooleanValue.get(op == Token.FNE));
            } else if (((NumericValue) literalOperand).isWholeNumber()) {
                operand = ((NumericValue) literalOperand).longValue();
            } else if (op == Token.FEQ) {
                return Literal.makeLiteral(BooleanValue.FALSE);
            } else if (op == Token.FNE) {
                return Literal.makeLiteral(BooleanValue.TRUE);
            } else if (op == Token.FGT || op == Token.FGE) {
                operand = ((NumericValue) literalOperand).ceiling().longValue();
                op = Token.FGE;
            } else /*if (op == Token.FLT || op == Token.FLE)*/ {
                operand = ((NumericValue) literalOperand).floor().longValue();
                op = Token.FLE;
            }
            if (operand < 0) {
                switch (op) {
                    case Token.FEQ:
                    case Token.FLT:
                    case Token.FLE:
                        return Literal.makeLiteral(BooleanValue.FALSE);
                    default:
                        return Literal.makeLiteral(BooleanValue.TRUE);
                }
            }
            if (operand > Integer.MAX_VALUE) {
                switch (op) {
                    case Token.FEQ:
                    case Token.FGT:
                    case Token.FGE:
                        return Literal.makeLiteral(BooleanValue.FALSE);
                    default:
                        return Literal.makeLiteral(BooleanValue.TRUE);
                }
            }
            if (sequence instanceof TailExpression || sequence instanceof Subsequence) {
                // it's probably the result of a previous optimization
                return null;
            }
            switch (op) {
                case Token.FEQ:
                case Token.FNE:
                case Token.FLE:
                case Token.FLT:
                    // rewrite count(E) op N as count(subsequence(E, 1, N+1)) op N
                    Subsequence ss = (Subsequence)SystemFunction.makeSystemFunction("subsequence",
                            new Expression[]{
                                    sequence,
                                    Literal.makeLiteral(IntegerValue.PLUS_ONE),
                                    Literal.makeLiteral(Int64Value.makeIntegerValue(operand + 1))});
                    Count ct = (Count)SystemFunction.makeSystemFunction("count", new Expression[]{ss});
                    CompareToIntegerConstant ctic = new CompareToIntegerConstant(ct, op, operand);
                    opt.trace("Rewrite count()~N as:", ctic);
                    ExpressionTool.copyLocationInfo(this, ctic);
                    return ctic;
                case Token.FGE:
                case Token.FGT:
                    // rewrite count(x) gt n as exists(x[n+1])
                    //     and count(x) ge n as exists(x[n])
                    TailExpression tail = new TailExpression(sequence, (int)(op == Token.FGE ? operand : operand+1));
                    ExpressionTool.copyLocationInfo(this, tail);
                    Expression result = SystemFunction.makeSystemFunction("exists", new Expression[]{tail});
                    ExpressionTool.copyLocationInfo(this, result);
                    opt.trace("Rewrite count()>=N as:", result);
                    return result;
            }
        }
        return null;
    }


    /**
     * Check whether this specific instance of the expression is negatable
     *
     * @return true if it is
     */

    public boolean isNegatable(ExpressionVisitor visitor) {
        // Expression is not negatable if it might involve NaN
        TypeHierarchy th = visitor.getConfiguration().getTypeHierarchy();
        return !maybeNaN(operand0, th) && !maybeNaN(operand1, th);
    }

    private boolean maybeNaN(Expression exp, TypeHierarchy th) {
        return th.relationship(exp.getItemType(th), BuiltInAtomicType.DOUBLE) != TypeHierarchy.DISJOINT ||
                th.relationship(exp.getItemType(th), BuiltInAtomicType.FLOAT) != TypeHierarchy.DISJOINT;
    }

    /**
     * Return the negation of this value comparison: that is, a value comparison that returns true()
     * if and only if the original returns false(). The result must be the same as not(this) even in the
     * case where one of the operands is ().
     * @return the inverted comparison
     */

    public Expression negate() {
        ValueComparison vc = new ValueComparison(operand0, Token.negate(operator), operand1);
        vc.comparer = comparer;
        if (resultWhenEmpty == null || resultWhenEmpty == BooleanValue.FALSE) {
            vc.resultWhenEmpty = BooleanValue.TRUE;
        } else {
            vc.resultWhenEmpty = BooleanValue.FALSE;
        }
        ExpressionTool.copyLocationInfo(this, vc);
        return vc;
    }


    /**
     * Test whether an expression is constant zero
     * @param v the value to be tested
     * @return true if the operand is the constant zero (of any numeric data type)
     */

    private static boolean isZero(Value v) {
        return v instanceof NumericValue && ((NumericValue)v).compareTo(0) == 0;
    }


    /**
     * Copy an expression. This makes a deep copy.
     *
     * @return the copy of the original expression
     */

    /*@NotNull*/
    public Expression copy() {
        ValueComparison vc = new ValueComparison(operand0.copy(), operator, operand1.copy());
        vc.comparer = comparer;
        vc.resultWhenEmpty = resultWhenEmpty;
        vc.needsRuntimeCheck = needsRuntimeCheck;
        return vc;
    }

    /**
     * Evaluate the effective boolean value of the expression
     *
     * @param context the given context for evaluation
     * @return a boolean representing the result of the comparison of the two operands
     */

    public boolean effectiveBooleanValue(XPathContext context) throws XPathException {
        try {
            AtomicValue v0 = ((AtomicValue) operand0.evaluateItem(context));
            if (v0 == null) {
                return (resultWhenEmpty == BooleanValue.TRUE);  // normally false
            }
            AtomicValue v1 = ((AtomicValue) operand1.evaluateItem(context));
            if (v1 == null) {
                return (resultWhenEmpty == BooleanValue.TRUE);  // normally false
            }
            return compare(v0, operator, v1, comparer.provideContext(context), needsRuntimeCheck);
        } catch (XPathException e) {
            // re-throw the exception with location information added
            e.maybeSetLocation(this);
            e.maybeSetContext(context);
            throw e;
        } 
    }

    /**
     * Compare two atomic values, using a specified operator and collation
     *
     * @param v0       the first operand
     * @param op       the operator, as defined by constants such as {@link net.sf.saxon.expr.parser.Token#FEQ} or
     *                 {@link net.sf.saxon.expr.parser.Token#FLT}
     * @param v1       the second operand
     * @param comparer the Collator to be used when comparing strings
     * @param checkTypes
     * @return the result of the comparison: -1 for LT, 0 for EQ, +1 for GT
     * @throws XPathException if the values are not comparable
     */

    static boolean compare(AtomicValue v0, int op, AtomicValue v1, AtomicComparer comparer, boolean checkTypes)
            throws XPathException {
        if (checkTypes &&
                    !Type.isComparable(v0.getPrimitiveType(), v1.getPrimitiveType(), Token.isOrderedOperator(op))) {
            XPathException e2 = new XPathException("Cannot compare " + Type.displayTypeName(v0) +
                " to " + Type.displayTypeName(v1));
            e2.setErrorCode("XPTY0004");
            e2.setIsTypeError(true);
            throw e2;
        }
        if (v0.isNaN() || v1.isNaN()) {
            return (op == Token.FNE);
        }
        try {
            switch (op) {
                case Token.FEQ:
                    return comparer.comparesEqual(v0, v1);
                case Token.FNE:
                    return !comparer.comparesEqual(v0, v1);
                case Token.FGT:
                    return comparer.compareAtomicValues(v0, v1) > 0;
                case Token.FLT:
                    return comparer.compareAtomicValues(v0, v1) < 0;
                case Token.FGE:
                    return comparer.compareAtomicValues(v0, v1) >= 0;
                case Token.FLE:
                    return comparer.compareAtomicValues(v0, v1) <= 0;
                default:
                    throw new UnsupportedOperationException("Unknown operator " + op);
            }
        } catch (ClassCastException err) {
            XPathException e2 = new XPathException("Cannot compare " + Type.displayTypeName(v0) +
                    " to " + Type.displayTypeName(v1));
            e2.setErrorCode("XPTY0004");
            e2.setIsTypeError(true);
            throw e2;
        }
    }

    /**
     * Evaluate the expression in a given context
     *
     * @param context the given context for evaluation
     * @return a BooleanValue representing the result of the numeric comparison of the two operands,
     *         or null representing the empty sequence
     */

    public Item evaluateItem(XPathContext context) throws XPathException {
        try {
            AtomicValue v0 = (AtomicValue) operand0.evaluateItem(context);
            if (v0 == null) {
                return resultWhenEmpty;
            }
            AtomicValue v1 = (AtomicValue) operand1.evaluateItem(context);
            if (v1 == null) {
                return resultWhenEmpty;
            }
            return BooleanValue.get(compare(v0, operator, v1, comparer.provideContext(context), needsRuntimeCheck));
        } catch (XPathException e) {
            // re-throw the exception with location information added
            e.maybeSetLocation(this);
            e.maybeSetContext(context);
            throw e;
        }
    }


    /**
     * Determine the data type of the expression
     *
     * @param th the type hierarchy cache
     * @return Type.BOOLEAN
     */

    /*@NotNull*/
    public ItemType getItemType(TypeHierarchy th) {
        return BuiltInAtomicType.BOOLEAN;
    }

    /**
     * Determine the static cardinality.
     */

    public int computeCardinality() {
        if (resultWhenEmpty != null) {
            return StaticProperty.EXACTLY_ONE;
        } else {
            return super.computeCardinality();
        }
    }

//    protected String displayOperator() {
//        return Token.tokens[operator] +
//                (resultWhenEmpty == null ? "" : " (on empty return " + resultWhenEmpty + ')');
//    }

    protected void explainExtraAttribute(ExpressionPresenter out) {
        out.emitAttribute("on-empty", resultWhenEmpty.toString());
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