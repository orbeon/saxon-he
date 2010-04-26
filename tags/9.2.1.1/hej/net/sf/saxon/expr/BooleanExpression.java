package net.sf.saxon.expr;

import net.sf.saxon.functions.BooleanFn;
import net.sf.saxon.functions.SystemFunction;
import net.sf.saxon.instruct.Choose;
import net.sf.saxon.om.Item;
import net.sf.saxon.trans.XPathException;
import net.sf.saxon.type.BuiltInAtomicType;
import net.sf.saxon.type.ItemType;
import net.sf.saxon.type.TypeHierarchy;
import net.sf.saxon.value.BooleanValue;

import java.util.Iterator;
import java.util.List;


/**
* Boolean expression: two truth values combined using AND or OR.
*/

public class BooleanExpression extends BinaryExpression implements Negatable {

    /**
     * Construct a boolean expression
     * @param p1 the first operand
     * @param operator one of {@link Token#AND} or {@link Token#OR}
     * @param p2 the second operand
     */

    public BooleanExpression(Expression p1, int operator, Expression p2) {
        super(p1, operator, p2);
    }

    public Expression typeCheck(ExpressionVisitor visitor, ItemType contextItemType) throws XPathException {
        Expression e = super.typeCheck(visitor, contextItemType);
        if (e == this) {
            XPathException err0 = TypeChecker.ebvError(operand0, visitor.getConfiguration().getTypeHierarchy());
            if (err0 != null) {
                err0.setLocator(this);
                throw err0;
            }
            XPathException err1 = TypeChecker.ebvError(operand1, visitor.getConfiguration().getTypeHierarchy());
            if (err1 != null) {
                err1.setLocator(this);
                throw err1;
            }
            // Precompute the EBV of any constant operand
            if (operand0 instanceof Literal && !(((Literal)operand0).getValue() instanceof BooleanValue)) {
                operand0 = Literal.makeLiteral(BooleanValue.get(operand0.effectiveBooleanValue(null)));
            }
            if (operand1 instanceof Literal && !(((Literal)operand1).getValue() instanceof BooleanValue)) {
                operand1 = Literal.makeLiteral(BooleanValue.get(operand1.effectiveBooleanValue(null)));
            }
        }
        return e;
    }

    /**
    * Determine the static cardinality. Returns [1..1]
    */

    public int computeCardinality() {
        return StaticProperty.EXACTLY_ONE;
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

    public Expression optimize(ExpressionVisitor visitor, ItemType contextItemType) throws XPathException {

        final Expression e = super.optimize(visitor, contextItemType);
        final TypeHierarchy th = visitor.getConfiguration().getTypeHierarchy();

        if (e != this) {
            return e;
        }

        Optimizer opt = visitor.getConfiguration().getOptimizer();
        operand0 = ExpressionTool.unsortedIfHomogeneous(opt, operand0);
        operand1 = ExpressionTool.unsortedIfHomogeneous(opt, operand1);

        // If the value can be determined from knowledge of one operand, precompute the result

        if (operator == Token.AND) {
            if (Literal.isConstantBoolean(operand0, false) || Literal.isConstantBoolean(operand1, false)) {
                // A and false() => false()
                // false() and B => false()
                return new Literal(BooleanValue.FALSE);
            } else if (Literal.isConstantBoolean(operand0, true)) {
                // true() and B => B
                return forceToBoolean(operand1, th);
            } else if (Literal.isConstantBoolean(operand1, true)) {
                // A and true() => A
                return forceToBoolean(operand0, th);
            }
        }

        if (operator == Token.OR) {
            if (Literal.isConstantBoolean(operand0, true) || Literal.isConstantBoolean(operand1, true)) {
                // A or true() => true()
                // true() or B => true()                
                return new Literal(BooleanValue.TRUE);
            } else if (Literal.isConstantBoolean(operand0, false)) {
                // false() or B => B
                return forceToBoolean(operand1, th);
            } else if (Literal.isConstantBoolean(operand1, false)) {
                // A or false() => A
                return forceToBoolean(operand0, th);
            }
        }

        // Rewrite (A and B) as (if (A) then B else false()). The benefit of this is that when B is a recursive
        // function call, it is treated as a tail call (test qxmp290). To avoid disrupting other optimizations
        // of "and" expressions (specifically, where clauses in FLWOR expressions), do this ONLY if B is a user
        // function call (we can't tell if it's recursive), and it's not in a loop.


        if (e == this && operator == Token.AND &&
                operand1 instanceof UserFunctionCall &&
                th.isSubType(operand1.getItemType(th), BuiltInAtomicType.BOOLEAN) &&
                !visitor.isLoopingSubexpression(null)) {
            Expression cond = Choose.makeConditional(operand0, operand1, Literal.makeLiteral(BooleanValue.FALSE));
            ExpressionTool.copyLocationInfo(this, cond);
            return cond;
        }
        return this;
    }

    private Expression forceToBoolean(Expression in, TypeHierarchy th) {
        if (in.getItemType(th) == BuiltInAtomicType.BOOLEAN && in.getCardinality() == StaticProperty.ALLOWS_ONE) {
            return in;
        } else {
            return SystemFunction.makeSystemFunction("boolean", new Expression[]{in});
        }
    }


    /**
     * Copy an expression. This makes a deep copy.
     *
     * @return the copy of the original expression
     */

    public Expression copy() {
        return new BooleanExpression(operand0.copy(), operator, operand1.copy());
    }


    /**
     * Check whether this specific instance of the expression is negatable
     *
     * @return true if it is
     */

    public boolean isNegatable(ExpressionVisitor visitor) {
        return true;
    }

    /**
     * Return the negation of this boolean expression, that is, an expression that returns true
     * when this expression returns false, and vice versa
     *
     * @return the negation of this expression
     */

    public Expression negate() {
        // Apply de Morgan's laws
        if (operator == Token.AND) {
            // not(A and B) ==> not(A) or not(B)
            BooleanFn not0 = (BooleanFn)SystemFunction.makeSystemFunction("not", new Expression[]{operand0});
            BooleanFn not1 = (BooleanFn)SystemFunction.makeSystemFunction("not", new Expression[]{operand1});
            return new BooleanExpression(not0, Token.OR, not1);
        } else {
            // not(A or B) => not(A) and not(B)
            BooleanFn not0 = (BooleanFn)SystemFunction.makeSystemFunction("not", new Expression[]{operand0});
            BooleanFn not1 = (BooleanFn)SystemFunction.makeSystemFunction("not", new Expression[]{operand1});
            return new BooleanExpression(not0, Token.AND, not1);
        }
    }

    /**
    * Evaluate the expression
    */

    public Item evaluateItem(XPathContext context) throws XPathException {
        return BooleanValue.get(effectiveBooleanValue(context));
    }

    /**
    * Evaluate as a boolean.
    */

    public boolean effectiveBooleanValue(XPathContext c) throws XPathException {
        switch(operator) {
            case Token.AND:
                return operand0.effectiveBooleanValue(c) && operand1.effectiveBooleanValue(c);

            case Token.OR:
                return operand0.effectiveBooleanValue(c) || operand1.effectiveBooleanValue(c);

            default:
                throw new UnsupportedOperationException("Unknown operator in boolean expression");
        }
    }

    /**
     * Determine the data type of the expression
     * @return BuiltInAtomicType.BOOLEAN
     * @param th the type hierarchy cache
     */

    public ItemType getItemType(TypeHierarchy th) {
        return BuiltInAtomicType.BOOLEAN;
    }

    /**
     * Construct a list containing the "anded" subexpressions of an expression:
     * if the expression is (A and B and C), this returns (A, B, C).
     * @param exp the expression to be decomposed
     * @param list the list to which the subexpressions are to be added.
     */

    public static void listAndComponents(Expression exp, List list) {
        if (exp instanceof BooleanExpression && ((BooleanExpression)exp).getOperator() == Token.AND) {
            for (Iterator iter = exp.iterateSubExpressions(); iter.hasNext();) {
                 listAndComponents((Expression)iter.next(), list);
            }
        } else {
            list.add(exp);
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
// The Initial Developer of the Original Code is Michael H. Kay.
//
// Portions created by (your name) are Copyright (C) (your legal entity). All Rights Reserved.
//
// Contributor(s): none.
//
