////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
// Copyright (c) 2015 Saxonica Limited.
// This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
// If a copy of the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
// This Source Code Form is "Incompatible With Secondary Licenses", as defined by the Mozilla Public License, v. 2.0.
////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

package net.sf.saxon.expr;

import net.sf.saxon.Configuration;
import net.sf.saxon.expr.instruct.Choose;
import net.sf.saxon.expr.parser.*;
import net.sf.saxon.functions.Number_1;
import net.sf.saxon.functions.SystemFunction;
import net.sf.saxon.lib.NamespaceConstant;
import net.sf.saxon.om.GroundedValue;
import net.sf.saxon.om.Sequence;
import net.sf.saxon.om.SequenceTool;
import net.sf.saxon.om.StructuredQName;
import net.sf.saxon.trace.ExpressionPresenter;
import net.sf.saxon.trans.XPathException;
import net.sf.saxon.type.*;
import net.sf.saxon.value.*;

import java.util.Map;

/**
 * Arithmetic Expression: an expression using one of the operators
 * plus, minus, multiply, div, idiv, mod, in backwards
 * compatibility mode: see {@link ArithmeticExpression} for the non-backwards
 * compatible case.
 */

public class ArithmeticExpression10 extends BinaryExpression implements Callable {

    private Calculator calculator;

    /**
     * Create an arithmetic expression to be evaluated in XPath 1.0 mode
     *
     * @param p0       the first operand
     * @param operator the operator, for example {@link Token#PLUS}
     * @param p1       the second operand
     */

    public ArithmeticExpression10(Expression p0, int operator, Expression p1) {
        super(p0, operator, p1);
    }

    /**
     * Type-check the expression statically. We try to work out which particular
     * arithmetic function to use if the types of operands are known an compile time.
     */

    /*@NotNull*/
    public Expression typeCheck(ExpressionVisitor visitor, ContextItemStaticInfo contextInfo) throws XPathException {

        typeCheckChildren(visitor, contextInfo);

        final Configuration config = getConfiguration();
        final TypeHierarchy th = config.getTypeHierarchy();

        if (Literal.isEmptySequence(getLhsExpression())) {
            return Literal.makeLiteral(DoubleValue.NaN);
        }

        if (Literal.isEmptySequence(getRhsExpression())) {
            return Literal.makeLiteral(DoubleValue.NaN);
        }

        Expression oldOp0 = getLhsExpression();
        Expression oldOp1 = getRhsExpression();


        SequenceType atomicType = SequenceType.OPTIONAL_ATOMIC;

        RoleDiagnostic role0 = new RoleDiagnostic(RoleDiagnostic.BINARY_EXPR, Token.tokens[operator], 0);
        //role0.setSourceLocator(this);
        setLhsExpression(TypeChecker.staticTypeCheck(getLhsExpression(), atomicType, true, role0, visitor));

        RoleDiagnostic role1 = new RoleDiagnostic(RoleDiagnostic.BINARY_EXPR, Token.tokens[operator], 1);
        //role1.setSourceLocator(this);
        setRhsExpression(TypeChecker.staticTypeCheck(getRhsExpression(), atomicType, true, role1, visitor));

        final ItemType itemType0 = getLhsExpression().getItemType();
        if (itemType0 instanceof ErrorType) {
            return Literal.makeLiteral(DoubleValue.NaN);
        }
        AtomicType type0 = (AtomicType) itemType0.getPrimitiveItemType();

        final ItemType itemType1 = getRhsExpression().getItemType();
        if (itemType1 instanceof ErrorType) {
            return Literal.makeLiteral(DoubleValue.NaN);
        }
        AtomicType type1 = (AtomicType) itemType1.getPrimitiveItemType();

        // If both operands are integers, use integer arithmetic and convert the result to a double
        if (th.isSubType(type0, BuiltInAtomicType.INTEGER) &&
                th.isSubType(type1, BuiltInAtomicType.INTEGER) &&
                (operator == Token.PLUS || operator == Token.MINUS || operator == Token.MULT)) {
            ArithmeticExpression arith = new ArithmeticExpression(getLhsExpression(), operator, getRhsExpression());
            arith.simplified = true;
            Expression n = SystemFunction.makeCall("number", getRetainedStaticContext(), arith);
            return n.typeCheck(visitor, contextInfo);
        }

        if (calculator == null) {
            setLhsExpression(createConversionCode(getLhsExpression(), config, type0));
        }
        type0 = (AtomicType) getLhsExpression().getItemType().getPrimitiveItemType();

        // System.err.println("First operand"); operand0.display(10);


        if (calculator == null) {
            setRhsExpression(createConversionCode(getRhsExpression(), config, type1));
        }

        type1 = (AtomicType) getRhsExpression().getItemType().getPrimitiveItemType();

        if (getLhsExpression() != oldOp0) {
            adoptChildExpression(getLhsExpression());
        }

        if (getRhsExpression() != oldOp1) {
            adoptChildExpression(getRhsExpression());
        }

        if (operator == Token.NEGATE) {
            if (getRhsExpression() instanceof Literal) {
                GroundedValue v = ((Literal) getRhsExpression()).getValue();
                if (v instanceof NumericValue) {
                    return Literal.makeLiteral(((NumericValue) v).negate());
                }
            }
            NegateExpression ne = new NegateExpression(getRhsExpression());
            ne.setBackwardsCompatible(true);
            return ne.typeCheck(visitor, contextInfo);
        }

        // Get a calculator to implement the arithmetic operation. If the types are not yet specifically known,
        // we allow this to return an "ANY" calculator which defers the decision. However, we only allow this if
        // at least one of the operand types is AnyAtomicType or (otherwise unspecified) numeric.

        boolean mustResolve = !(type0.equals(BuiltInAtomicType.ANY_ATOMIC) || type1.equals(BuiltInAtomicType.ANY_ATOMIC)
                || type0.equals(NumericType.getInstance()) || type1.equals(NumericType.getInstance()));

        calculator = assignCalculator(type0, type1, mustResolve);

        try {
            if ((getLhsExpression() instanceof Literal) && (getRhsExpression() instanceof Literal)) {
                return Literal.makeLiteral(
                        SequenceTool.toGroundedValue(evaluateItem(visitor.getStaticContext().makeEarlyEvaluationContext())));
            }
        } catch (XPathException err) {
            // if early evaluation fails, suppress the error: the value might
            // not be needed at run-time
        }
        return this;
    }

    /**
     * Set the calculator externally (used when reconstructing the expression tree)
     * @param calc the calculator to be used
     */

    public void setCalculator(Calculator calc) {
        this.calculator = calc;
    }

    private Calculator assignCalculator(AtomicType type0, AtomicType type1, boolean mustResolve) throws XPathException {
        Calculator calculator = Calculator.getCalculator(type0.getFingerprint(), type1.getFingerprint(),
                ArithmeticExpression.mapOpCode(operator), mustResolve);

        if (calculator == null) {
            XPathException de = new XPathException("Arithmetic operator is not defined for arguments of types (" +
                    type0.getDescription() + ", " + type1.getDescription() + ")");
            de.setLocation(getLocation());
            de.setErrorCode("XPTY0004");
            throw de;
        }
        return calculator;
    }

    private Expression createConversionCode(
            Expression operand, final Configuration config, AtomicType type) {
        TypeHierarchy th = config.getTypeHierarchy();
        if (Cardinality.allowsMany(operand.getCardinality())) {
            Expression fie = FirstItemExpression.makeFirstItemExpression(operand);
            ExpressionTool.copyLocationInfo(this, fie);
            operand = fie;
        }

        if (th.isSubType(type, BuiltInAtomicType.DOUBLE) ||
                th.isSubType(type, BuiltInAtomicType.DATE) ||
                th.isSubType(type, BuiltInAtomicType.TIME) ||
                th.isSubType(type, BuiltInAtomicType.DATE_TIME) ||
                th.isSubType(type, BuiltInAtomicType.DURATION)) {
            return operand;
        }
        if (th.isSubType(type, BuiltInAtomicType.BOOLEAN) ||
                th.isSubType(type, BuiltInAtomicType.STRING) ||
                th.isSubType(type, BuiltInAtomicType.UNTYPED_ATOMIC) ||
                th.isSubType(type, BuiltInAtomicType.FLOAT) ||
                th.isSubType(type, BuiltInAtomicType.DECIMAL)) {
            if (operand instanceof Literal) {
                GroundedValue val = ((Literal) operand).getValue();
                Literal lit = Literal.makeLiteral(Number_1.convert((AtomicValue) val, config));
                //lit.setContainer(getContainer());
                return lit;
            } else {
                Expression exp = SystemFunction.makeCall("number", getRetainedStaticContext(), operand);
                //exp.setContainer(getContainer());
                return exp;
            }
        }
        // If we can't determine the primitive type at compile time, we generate a run-time typeswitch

        LetExpression let = new LetExpression();
        let.setRequiredType(SequenceType.OPTIONAL_ATOMIC);
        let.setVariableQName(new StructuredQName("nn", NamespaceConstant.SAXON, "nn" + let.hashCode()));
        let.setSequence(operand);

        LocalVariableReference var = new LocalVariableReference(let);
        Expression isDouble = new InstanceOfExpression(
                var, SequenceType.makeSequenceType(BuiltInAtomicType.DOUBLE, StaticProperty.ALLOWS_ZERO_OR_ONE));

        var = new LocalVariableReference(let);
        Expression isDecimal = new InstanceOfExpression(
                var, SequenceType.makeSequenceType(BuiltInAtomicType.DECIMAL, StaticProperty.ALLOWS_ZERO_OR_ONE));

        var = new LocalVariableReference(let);
        Expression isFloat = new InstanceOfExpression(
                var, SequenceType.makeSequenceType(BuiltInAtomicType.FLOAT, StaticProperty.ALLOWS_ZERO_OR_ONE));

        var = new LocalVariableReference(let);
        Expression isString = new InstanceOfExpression(
                var, SequenceType.makeSequenceType(BuiltInAtomicType.STRING, StaticProperty.ALLOWS_ZERO_OR_ONE));

        var = new LocalVariableReference(let);
        Expression isUntypedAtomic = new InstanceOfExpression(
                var, SequenceType.makeSequenceType(BuiltInAtomicType.UNTYPED_ATOMIC, StaticProperty.ALLOWS_ZERO_OR_ONE));

        var = new LocalVariableReference(let);
        Expression isBoolean = new InstanceOfExpression(
                var, SequenceType.makeSequenceType(BuiltInAtomicType.BOOLEAN, StaticProperty.ALLOWS_ZERO_OR_ONE));

        Expression condition = new OrExpression(isDouble, isDecimal);
        condition = new OrExpression(condition, isFloat);
        condition = new OrExpression(condition, isString);
        condition = new OrExpression(condition, isUntypedAtomic);
        condition = new OrExpression(condition, isBoolean);

        var = new LocalVariableReference(let);
        Expression fn = SystemFunction.makeCall("number", getRetainedStaticContext(), var);

        var = new LocalVariableReference(let);
        var.setStaticType(SequenceType.SINGLE_ATOMIC, null, 0);
        Expression action = Choose.makeConditional(condition, fn, var);
        let.setAction(action);
        return let;
    }

    /**
     * Determine the data type of the expression, if this is known statically
     */

    /*@NotNull*/
    public ItemType getItemType() {
        if (calculator == null) {
            return BuiltInAtomicType.ANY_ATOMIC;  // type is not known statically
        } else {
            ItemType t1 = getLhsExpression().getItemType();
            if (!(t1 instanceof AtomicType)) {
                t1 = t1.getAtomizedItemType();
            }
            ItemType t2 = getRhsExpression().getItemType();
            if (!(t2 instanceof AtomicType)) {
                t2 = t2.getAtomizedItemType();
            }
            return calculator.getResultType((AtomicType) t1.getPrimitiveItemType(),
                    (AtomicType) t2.getPrimitiveItemType());
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
        ArithmeticExpression10 a2 = new ArithmeticExpression10(getLhsExpression().copy(rebindings), operator, getRhsExpression().copy(rebindings));
        ExpressionTool.copyLocationInfo(this, a2);
        a2.calculator = calculator;
        return a2;
    }

    @Override
    protected String tag() {
        return "arith10";
    }

    @Override
    protected void explainExtraAttributes(ExpressionPresenter out) {
        out.emitAttribute("calc", calculator.code());
    }

    /**
     * Evaluate the expression.
     */

    public AtomicValue evaluateItem(XPathContext context) throws XPathException {

        Calculator calc = calculator;
        AtomicValue v1 = (AtomicValue) getLhsExpression().evaluateItem(context);
        if (v1 == null) {
            return DoubleValue.NaN;
        }

        AtomicValue v2 = (AtomicValue) getRhsExpression().evaluateItem(context);
        if (v2 == null) {
            return DoubleValue.NaN;
        }

        if (calc == null) {
            // This shouldn't happen. It's a fallback for a failure to assign the calculator earlier
            // at compile time. It has been known to happen when simplify() is called without typeCheck().
            calc = assignCalculator(v1.getPrimitiveType(), v2.getPrimitiveType(), true);
        }

        return calc.compute(v1, v2, context);
    }

    /**
     * Evaluate the expression
     *
     * @param context   the dynamic evaluation context
     * @param arguments the values of the arguments, supplied as SequenceIterators
     * @return the result of the evaluation, in the form of a SequenceIterator
     * @throws net.sf.saxon.trans.XPathException
     *          if a dynamic error occurs during the evaluation of the expression
     */
    public Sequence call(XPathContext context, Sequence[] arguments) throws XPathException {
        Calculator calc = calculator;
        AtomicValue v1 = (AtomicValue) arguments[0].head();
        if (v1 == null) {
            return DoubleValue.NaN;
        }

        AtomicValue v2 = (AtomicValue) arguments[1].head();
        if (v2 == null) {
            return DoubleValue.NaN;
        }

        if (calc == null) {
            // This shouldn't happen. It's a fallback for a failure to assign the calculator earlier
            // at compile time. It has been known to happen when simplify() is called without typeCheck().
            calc = assignCalculator(v1.getPrimitiveType(), v2.getPrimitiveType(), true);
        }

        return calc.compute(v1, v2, context);
    }
}

