package net.sf.saxon.expr;

import net.sf.saxon.expr.parser.ExpressionVisitor;
import net.sf.saxon.expr.parser.Token;
import net.sf.saxon.functions.NotFn;
import net.sf.saxon.functions.SystemFunction;
import net.sf.saxon.trans.XPathException;
import net.sf.saxon.type.TypeHierarchy;
import net.sf.saxon.value.BooleanValue;

public class OrExpression extends BooleanExpression {


	/**
	 * Construct a boolean OR expression
	 * @param p1 the first operand
     * @param p2 the second operand
     */

	public OrExpression(Expression p1, Expression p2) {
		super(p1, Token.OR, p2);
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

		final Expression e = super.optimize(visitor, contextItemType);
		final TypeHierarchy th = visitor.getConfiguration().getTypeHierarchy();

		if (e != this) {
			return e;
		}

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

		return this;
	}
	
	/**
	 * Copy an expression. This makes a deep copy.
	 *
	 * @return the copy of the original expression
	 */

	/*@NotNull*/
    public Expression copy() {
		return new OrExpression(operand0.copy(), operand1.copy());
	}


	/**
	 * Return the negation of this boolean expression, that is, an expression that returns true
	 * when this expression returns false, and vice versa
	 *
	 * @return the negation of this expression
	 */

	public Expression negate() {
		// Apply de Morgan's laws
		// not(A or B) => not(A) and not(B)
		NotFn not0 = (NotFn)SystemFunction.makeSystemFunction("not", new Expression[]{operand0});
		NotFn not1 = (NotFn)SystemFunction.makeSystemFunction("not", new Expression[]{operand1});
		return new AndExpression(not0, not1);
	}


	/**
	 * Evaluate as a boolean.
	 */

	public boolean effectiveBooleanValue(XPathContext c) throws XPathException {
		return operand0.effectiveBooleanValue(c) || operand1.effectiveBooleanValue(c);
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