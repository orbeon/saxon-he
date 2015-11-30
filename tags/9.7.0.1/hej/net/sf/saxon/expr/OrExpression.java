////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
// Copyright (c) 2015 Saxonica Limited.
// This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
// If a copy of the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
// This Source Code Form is "Incompatible With Secondary Licenses", as defined by the Mozilla Public License, v. 2.0.
////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

package net.sf.saxon.expr;

import com.saxonica.ee.bytecode.ExpressionCompiler;
import com.saxonica.ee.bytecode.OrExpressionCompiler;
import net.sf.saxon.expr.parser.ContextItemStaticInfo;
import net.sf.saxon.expr.parser.ExpressionTool;
import net.sf.saxon.expr.parser.ExpressionVisitor;
import net.sf.saxon.expr.parser.Token;
import net.sf.saxon.functions.SystemFunction;
import net.sf.saxon.trans.XPathException;
import net.sf.saxon.type.TypeHierarchy;
import net.sf.saxon.value.BooleanValue;

public class OrExpression extends BooleanExpression {


    /**
     * Construct a boolean OR expression
     *
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

        final TypeHierarchy th = getConfiguration().getTypeHierarchy();

        if ((getLhsExpression() instanceof OrExpression) || (getRhsExpression() instanceof OrExpression)) {
            final Expression e2 = getConfiguration().obtainOptimizer().tryGeneralComparison(visitor, contextItemType, (OrExpression) this);
            if (e2 != null && e2 != this) {

                return e2;
            }
        }

        final Expression e = super.optimize(visitor, contextItemType);


        if (e != this) {
            return e;
        }

        if (Literal.isConstantBoolean(getLhsExpression(), true) || Literal.isConstantBoolean(getRhsExpression(), true)) {
            // A or true() => true()
            // true() or B => true()
            return Literal.makeLiteral(BooleanValue.TRUE);
        } else if (Literal.isConstantBoolean(getLhsExpression(), false)) {
            // false() or B => B
            return forceToBoolean(getRhsExpression());
        } else if (Literal.isConstantBoolean(getRhsExpression(), false)) {
            // A or false() => A
            return forceToBoolean(getLhsExpression());
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
        OrExpression exp = new OrExpression(getLhsExpression().copy(), getRhsExpression().copy());
        ExpressionTool.copyLocationInfo(this, exp);
        return exp;
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
        Expression not0 = SystemFunction.makeCall("not", getRetainedStaticContext(), getLhsExpression());
        Expression not1 = SystemFunction.makeCall("not", getRetainedStaticContext(), getRhsExpression());
        return new AndExpression(not0, not1);
    }

    /**
     * Get the element name used to identify this expression in exported expression format
     *
     * @return the element name used to identify this expression
     */
    @Override
    protected String tag() {
        return "or";
    }

    /**
     * Evaluate as a boolean.
     */

    public boolean effectiveBooleanValue(XPathContext c) throws XPathException {
        return getLhsExpression().effectiveBooleanValue(c) || getRhsExpression().effectiveBooleanValue(c);
    }

    //#ifdefined BYTECODE

    /**
     * Return the compiler of the Or expression
     *
     * @return the relevant ExpressionCompiler
     */
    @Override
    public ExpressionCompiler getExpressionCompiler() {
        return new OrExpressionCompiler();
    }
//#endif


}

