package net.sf.saxon.expr;

import net.sf.saxon.expr.instruct.Choose;
import net.sf.saxon.expr.parser.ExpressionTool;
import net.sf.saxon.expr.parser.ExpressionVisitor;
import net.sf.saxon.expr.parser.Token;
import net.sf.saxon.functions.NotFn;
import net.sf.saxon.functions.SystemFunction;
import net.sf.saxon.trans.XPathException;
import net.sf.saxon.type.BuiltInAtomicType;
import net.sf.saxon.type.TypeHierarchy;
import net.sf.saxon.value.BooleanValue;

public class AndExpression extends BooleanExpression{

	/**
	 * Construct a boolean AND expression
	 * @param p1 the first operand
     * @param p2 the second operand
     */

	public AndExpression(Expression p1, Expression p2) {
		super(p1, Token.AND, p2);
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

        Expression e = super.optimize(visitor, contextItemType);
        if (e != this) {
			return e;
		}

		// If the value can be determined from knowledge of one operand, precompute the result

        final TypeHierarchy th = visitor.getConfiguration().getTypeHierarchy();
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

		// Rewrite (A and B) as (if (A) then B else false()). The benefit of this is that when B is a recursive
		// function call, it is treated as a tail call (test qxmp290). To avoid disrupting other optimizations
		// of "and" expressions (specifically, where clauses in FLWOR expressions), do this ONLY if B is a user
		// function call (we can't tell if it's recursive), and it's not in a loop.


		if (e == this &&
				operand1 instanceof UserFunctionCall &&
				th.isSubType(operand1.getItemType(th), BuiltInAtomicType.BOOLEAN) &&
				!visitor.isLoopingSubexpression(null)) {
			Expression cond = Choose.makeConditional(operand0, operand1, Literal.makeLiteral(BooleanValue.FALSE));
			ExpressionTool.copyLocationInfo(this, cond);
			return cond;
		}
		return this;
	}
	
	/**
	 * Copy an expression. This makes a deep copy.
	 *
	 * @return the copy of the original expression
	 */

	/*@NotNull*/
    public Expression copy() {
		return new AndExpression(operand0.copy(), operand1.copy());
	}


	/**
	 * Return the negation of this boolean expression, that is, an expression that returns true
	 * when this expression returns false, and vice versa
	 *
	 * @return the negation of this expression
	 */

	public Expression negate() {
		// Apply de Morgan's laws
		// not(A and B) ==> not(A) or not(B)
		NotFn not0 = (NotFn)SystemFunction.makeSystemFunction("not", new Expression[]{operand0});
		NotFn not1 = (NotFn)SystemFunction.makeSystemFunction("not", new Expression[]{operand1});
		return new OrExpression(not0, not1);

	}


	/**
	 * Evaluate as a boolean.
	 */

	public boolean effectiveBooleanValue(XPathContext c) throws XPathException {
		return operand0.effectiveBooleanValue(c) && operand1.effectiveBooleanValue(c);
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