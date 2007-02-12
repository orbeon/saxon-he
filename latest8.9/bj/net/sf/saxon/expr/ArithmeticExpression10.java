package net.sf.saxon.expr;

import net.sf.saxon.functions.NumberFn;
import net.sf.saxon.functions.SystemFunction;
import net.sf.saxon.om.Item;
import net.sf.saxon.pattern.EmptySequenceTest;
import net.sf.saxon.trans.XPathException;
import net.sf.saxon.trans.DynamicError;
import net.sf.saxon.type.AtomicType;
import net.sf.saxon.type.BuiltInAtomicType;
import net.sf.saxon.type.ItemType;
import net.sf.saxon.type.TypeHierarchy;
import net.sf.saxon.value.*;

/**
 * Arithmetic Expression: an expression using one of the operators
 * plus, minus, multiply, div, idiv, mod, in backwards
 * compatibility mode: see {@link ArithmeticExpression} for the non-backwards
 * compatible case.
 */

public class ArithmeticExpression10 extends BinaryExpression {

    private Calculator calculator;

    public ArithmeticExpression10(Expression p1, int operator, Expression p2) {
        super(p1, operator, p2);
    }

    /**
     * Determine whether the expression is to be evaluated in backwards-compatible mode
     */

    public boolean isBackwardsCompatible() {
        return true;
    }

    /**
     * Type-check the expression statically. We try to work out which particular
     * arithmetic function to use if the types of operands are known an compile time.
     */

    public Expression typeCheck(StaticContext env, ItemType contextItemType) throws XPathException {

        final TypeHierarchy th = env.getConfiguration().getTypeHierarchy();

        if (Literal.isEmptySequence(operand0)) {
            return new Literal(DoubleValue.NaN);
        }

        if (Literal.isEmptySequence(operand1)) {
            return new Literal(DoubleValue.NaN);
        }

        Expression oldOp0 = operand0;
        Expression oldOp1 = operand1;

        operand0 = operand0.typeCheck(env, contextItemType);
        operand1 = operand1.typeCheck(env, contextItemType);


        SequenceType atomicType = SequenceType.OPTIONAL_ATOMIC;

        RoleLocator role0 = new RoleLocator(RoleLocator.BINARY_EXPR, Token.tokens[operator], 0, null);
        role0.setSourceLocator(this);
        operand0 = TypeChecker.staticTypeCheck(operand0, atomicType, true, role0, env);
        final ItemType itemType0 = operand0.getItemType(th);
        if (itemType0 instanceof EmptySequenceTest) {
            return Literal.makeLiteral(DoubleValue.NaN);
        }
        AtomicType type0 = (AtomicType) itemType0.getPrimitiveItemType();
        operand0 = createConversionCode(operand0, th, type0, env);
        type0 = (AtomicType) operand0.getItemType(th).getPrimitiveItemType();

        // System.err.println("First operand"); operand0.display(10);

        RoleLocator role1 = new RoleLocator(RoleLocator.BINARY_EXPR, Token.tokens[operator], 1, null);
        role1.setSourceLocator(this);
        operand1 = TypeChecker.staticTypeCheck(operand1, atomicType, true, role1, env);
        final ItemType itemType1 = operand1.getItemType(th);
        if (itemType1 instanceof EmptySequenceTest) {
            return Literal.makeLiteral(DoubleValue.NaN);
        }
        AtomicType type1 = (AtomicType)itemType1.getPrimitiveItemType();
        operand1 = createConversionCode(operand1, th, type1, env);
        type1 = (AtomicType) operand1.getItemType(th).getPrimitiveItemType();

        if (operand0 != oldOp0) {
            adoptChildExpression(operand0);
        }

        if (operand1 != oldOp1) {
            adoptChildExpression(operand1);
        }

        if (operator == Token.NEGATE) {
            if (operand1 instanceof Literal) {
                Value v = ((Literal)operand1).getValue();
                if (v instanceof NumericValue) {
                    return Literal.makeLiteral(((NumericValue)v).negate());
                }
            }
            NegateExpression ne = new NegateExpression(operand1);
            ne.setBackwardsCompatible(false);
            return ne.typeCheck(env, contextItemType);
        }

        // Get a calculator to implement the arithmetic operation. If the types are not yet specifically known,
        // we allow this to return an "ANY" calculator which defers the decision. However, we only allow this if
        // at least one of the operand types is AnyAtomicType.

        boolean mustResolve = !(type0.equals(BuiltInAtomicType.ANY_ATOMIC) || type1.equals(BuiltInAtomicType.ANY_ATOMIC));

        calculator = Calculator.getCalculator(type0.getFingerprint(), type1.getFingerprint(),
                ArithmeticExpression.mapOpCode(operator), mustResolve);

        if (calculator == null) {
            DynamicError de = new DynamicError("Arithmetic operator is not defined for arguments of types (" +
                    type0.getDescription() + ", " + type1.getDescription() + ")");
            de.setLocator(this);
            de.setErrorCode("XPTY0004");
            throw de;
        }

        try {
            if ((operand0 instanceof Literal) && (operand1 instanceof Literal)) {
                return Literal.makeLiteral(
                        Value.asValue(evaluateItem(env.makeEarlyEvaluationContext())));
            }
        } catch (DynamicError err) {
            // if early evaluation fails, suppress the error: the value might
            // not be needed at run-time
        }
        return this;
    }

    private Expression createConversionCode(
            Expression operand, final TypeHierarchy th, AtomicType type, StaticContext env) {
        if (Cardinality.allowsMany(operand.getCardinality())) {
            FirstItemExpression fie = new FirstItemExpression(operand);
            fie.setParentExpression(this);
            operand = fie;
        }
        if (!th.isSubType(type, BuiltInAtomicType.DOUBLE)) {
            NumberFn fn = (NumberFn)SystemFunction.makeSystemFunction("number", 1, env.getNamePool());
            fn.setParentExpression(operand.getParentExpression());
            Expression[] args = {operand};
            fn.setArguments(args);
            operand = fn;
        }
        return operand;
    }

    /**
     * Determine the data type of the expression, if this is known statically
     *
     * @param th
     */

    public ItemType getItemType(TypeHierarchy th) {
        if (calculator == null) {
            return BuiltInAtomicType.ANY_ATOMIC;  // type is not known statically
        } else {
            ItemType t1 = operand0.getItemType(th);
            if (!(t1 instanceof AtomicType)) {
                t1 = t1.getAtomizedItemType();
            }
            ItemType t2 = operand1.getItemType(th);
            if (!(t2 instanceof AtomicType)) {
                t2 = t2.getAtomizedItemType();
            }
            return calculator.getResultType((AtomicType) t1.getPrimitiveItemType(), (AtomicType) t2.getPrimitiveItemType());
        }
    }

    /**
     * Evaluate the expression.
     */

    public Item evaluateItem(XPathContext context) throws XPathException {

        AtomicValue v1 = (AtomicValue) operand0.evaluateItem(context);
        if (v1 == null) {
            return DoubleValue.NaN;
        }

        AtomicValue v2 = (AtomicValue) operand1.evaluateItem(context);
        if (v2 == null) {
            return DoubleValue.NaN;
        }

        return calculator.compute(v1, v2, context);
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
