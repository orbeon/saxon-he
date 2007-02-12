package net.sf.saxon.expr;

import net.sf.saxon.om.Item;
import net.sf.saxon.om.StandardNames;
import net.sf.saxon.pattern.EmptySequenceTest;
import net.sf.saxon.trans.DynamicError;
import net.sf.saxon.trans.XPathException;
import net.sf.saxon.type.AtomicType;
import net.sf.saxon.type.BuiltInAtomicType;
import net.sf.saxon.type.ItemType;
import net.sf.saxon.type.TypeHierarchy;
import net.sf.saxon.value.*;

/**
 * Arithmetic Expression: an expression using one of the operators
 * plus, minus, multiply, div, idiv, mod. Note that this code does not handle backwards
 * compatibility mode: see {@link ArithmeticExpression10}
 */

public class ArithmeticExpression extends BinaryExpression {

    private Calculator calculator;

    public ArithmeticExpression(Expression p1, int operator, Expression p2) {
        super(p1, operator, p2);
    }

    public Expression simplify(StaticContext env) throws XPathException {
        Expression e = super.simplify(env);
        if (e == this && env.isInBackwardsCompatibleMode()) {
            return new ArithmeticExpression10(operand0, operator, operand1);
        } else {
            return e;
        }
    }

    /**
     * Determine whether the expression is to be evaluated in backwards-compatible mode
     */

    public boolean isBackwardsCompatible() {
        return false;
    }

    /**
     * Get the calculator allocated to evaluate this expression
     */

    public Calculator getCalculator() {
        return calculator;
    }

    /**
     * Type-check the expression statically. We try to work out which particular
     * arithmetic function to use if the types of operands are known an compile time.
     */

    public Expression typeCheck(StaticContext env, ItemType contextItemType) throws XPathException {

        final TypeHierarchy th = env.getConfiguration().getTypeHierarchy();

        Expression oldOp0 = operand0;
        Expression oldOp1 = operand1;

        operand0 = operand0.typeCheck(env, contextItemType);
        operand1 = operand1.typeCheck(env, contextItemType);


        SequenceType atomicType = SequenceType.OPTIONAL_ATOMIC;

        RoleLocator role0 = new RoleLocator(RoleLocator.BINARY_EXPR, Token.tokens[operator], 0, null);
        role0.setSourceLocator(this);
        operand0 = TypeChecker.staticTypeCheck(operand0, atomicType, false, role0, env);
        final ItemType itemType0 = operand0.getItemType(th);
        if (itemType0 instanceof EmptySequenceTest) {
            return new Literal(EmptySequence.getInstance());
        }
        AtomicType type0 = (AtomicType) itemType0.getPrimitiveItemType();
        if (type0.getFingerprint() == StandardNames.XS_UNTYPED_ATOMIC) {
            operand0 = new UntypedAtomicConverter(operand0, BuiltInAtomicType.DOUBLE, true);
            type0 = BuiltInAtomicType.DOUBLE;
        } else if (!(operand0 instanceof UntypedAtomicConverter) &&
                th.relationship(type0, BuiltInAtomicType.UNTYPED_ATOMIC) != TypeHierarchy.DISJOINT) {
            operand0 = new UntypedAtomicConverter(operand0, BuiltInAtomicType.DOUBLE, false);
            type0 = (AtomicType)operand0.getItemType(th);
        }

        // System.err.println("First operand"); operand0.display(10);

        RoleLocator role1 = new RoleLocator(RoleLocator.BINARY_EXPR, Token.tokens[operator], 1, null);
        role1.setSourceLocator(this);
        operand1 = TypeChecker.staticTypeCheck(operand1, atomicType, false, role1, env);
        final ItemType itemType1 = operand1.getItemType(th);
        if (itemType1 instanceof EmptySequenceTest) {
            return new Literal(EmptySequence.getInstance());
        }
        AtomicType type1 = (AtomicType)itemType1.getPrimitiveItemType();
        if (type1.getFingerprint() == StandardNames.XS_UNTYPED_ATOMIC) {
            operand1 = new UntypedAtomicConverter(operand1, BuiltInAtomicType.DOUBLE, true);
            type1 = BuiltInAtomicType.DOUBLE;
        } else if (!(operand1 instanceof UntypedAtomicConverter) &&
                th.relationship(type1, BuiltInAtomicType.UNTYPED_ATOMIC) != TypeHierarchy.DISJOINT) {
            operand1 = new UntypedAtomicConverter(operand1, BuiltInAtomicType.DOUBLE, false);
            type1 = (AtomicType)operand1.getItemType(th);
        }

        if (operand0 != oldOp0) {
            adoptChildExpression(operand0);
        }

        if (operand1 != oldOp1) {
            adoptChildExpression(operand1);
        }

        if (Literal.isEmptySequence(operand0) ||
                Literal.isEmptySequence(operand1)) {
            return new Literal(EmptySequence.getInstance());
        }

        if (operator == Token.NEGATE) {
            if (operand1 instanceof Literal && ((Literal)operand1).getValue() instanceof NumericValue) {
                NumericValue nv = (NumericValue)((Literal)operand1).getValue();
                return new Literal(nv.negate());
            } else {
                NegateExpression ne = new NegateExpression(operand1);
                ne.setBackwardsCompatible(false);
                return ne.typeCheck(env, contextItemType);
            }
        }

        // Get a calculator to implement the arithmetic operation. If the types are not yet specifically known,
        // we allow this to return an "ANY" calculator which defers the decision. However, we only allow this if
        // at least one of the operand types is AnyAtomicType or (otherwise unspecified) numeric.

        boolean mustResolve = !(type0.equals(BuiltInAtomicType.ANY_ATOMIC) || type1.equals(BuiltInAtomicType.ANY_ATOMIC)
                || type0.equals(BuiltInAtomicType.NUMERIC) || type1.equals(BuiltInAtomicType.NUMERIC));

        calculator = Calculator.getCalculator(
                type0.getFingerprint(), type1.getFingerprint(), mapOpCode(operator), mustResolve);

        if (calculator == null) {
            DynamicError de = new DynamicError("Arithmetic operator is not defined for arguments of types (" +
                    type0.getDescription() + ", " + type1.getDescription() + ")");
            de.setLocator(this);
            de.setErrorCode("XPTY0004");
            throw de;
        }

        try {
            if ((operand0 instanceof Literal) && (operand1 instanceof Literal)) {
                return new Literal(Value.asValue(evaluateItem(env.makeEarlyEvaluationContext())));
            }
        } catch (DynamicError err) {
            // if early evaluation fails, suppress the error: the value might
            // not be needed at run-time
        }
        return this;
    }

    /**
     * Map operator codes from those in the Token class to those in the Calculator class
     */

    public static int mapOpCode(int op) {
        switch (op) {
            case Token.PLUS:
                return Calculator.PLUS;
            case Token.MINUS:
            case Token.NEGATE:
                return Calculator.MINUS;
            case Token.MULT:
                return Calculator.TIMES;
            case Token.DIV:
                return Calculator.DIV;
            case Token.IDIV:
                return Calculator.IDIV;
            case Token.MOD:
                return Calculator.MOD;
            default:
                throw new IllegalArgumentException();
        }

    }


    /**
     * Determine the data type of the expression, if this is known statically
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
            ItemType resultType = calculator.getResultType((AtomicType) t1.getPrimitiveItemType(),
                    (AtomicType) t2.getPrimitiveItemType());

            if (resultType.equals(BuiltInAtomicType.ANY_ATOMIC)) {
                // there are a few special cases where we can do better. For example, given X+1, where the type of X
                // is unknown, we can still infer that the result is numeric. (Not so for X*2, however, where it could
                // be a duration)
                // TODO: perhaps we should have allocated a more specific calculator in this case?
                if ((operator == Token.PLUS || operator == Token.MINUS) &&
                        (th.isSubType(t2, BuiltInAtomicType.NUMERIC) || th.isSubType(t1, BuiltInAtomicType.NUMERIC))) {
                    resultType = BuiltInAtomicType.NUMERIC;
                }
            }
            return resultType;
        }
    }

    /**
     * Evaluate the expression.
     */

    public Item evaluateItem(XPathContext context) throws XPathException {

        AtomicValue v1 = (AtomicValue) operand0.evaluateItem(context);
        if (v1 == null) {
            return null;
        }

        AtomicValue v2 = (AtomicValue) operand1.evaluateItem(context);
        if (v2 == null) {
            return null;
        }

        try {
            return calculator.compute(v1, v2, context);
        } catch (DynamicError e) {
            if (e.getLocator() == null) {
                e.setLocator(this);
            }
            if (e.getXPathContext() == null) {
                e.setXPathContext(context);
            }
            throw e;
        }
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
