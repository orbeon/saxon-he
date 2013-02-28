package net.sf.saxon.expr;

import net.sf.saxon.Configuration;
import net.sf.saxon.expr.parser.ExpressionVisitor;
import net.sf.saxon.expr.parser.RoleLocator;
import net.sf.saxon.expr.parser.TypeChecker;
import net.sf.saxon.om.Item;
import net.sf.saxon.trans.XPathException;
import net.sf.saxon.type.ItemType;
import net.sf.saxon.type.TypeHierarchy;
import net.sf.saxon.value.DoubleValue;
import net.sf.saxon.value.NumericValue;
import net.sf.saxon.value.SequenceType;
import net.sf.saxon.value.Value;

/**
 * Negate Expression: implements the unary minus operator.
 * This expression is initially created as an ArithmeticExpression (or in backwards
 * compatibility mode, an ArithmeticExpression10) to take advantage of the type checking code.
 * So we don't need to worry about type checking or argument conversion.
 */

public class NegateExpression extends UnaryExpression {

    private boolean backwardsCompatible;

    /**
     * Create a NegateExpression
     * @param base the expression that computes the value whose sign is to be reversed
     */

    public NegateExpression(Expression base) {
        super(base);
    }

    /**
     * Set whether the expression is to be evaluated in XPath 1.0 compatibility mode
     * @param compatible true if XPath 1.0 compatibility mode is enabled
     */

    public void setBackwardsCompatible(boolean compatible) {
        backwardsCompatible = compatible;
    }

    /**
     * Ask whether the expression is to be evaluated in XPath 1.0 compatibility mode
     * @return true if XPath 1.0 compatibility mode is enabled
     */

    public boolean isBackwardsCompatible() {
        return backwardsCompatible;
    }

    /*@NotNull*/
    public Expression typeCheck(ExpressionVisitor visitor, ExpressionVisitor.ContextItemType contextItemType) throws XPathException {
        Expression oldop = operand;
        RoleLocator role = new RoleLocator(RoleLocator.UNARY_EXPR, "-", 0);
        operand = TypeChecker.staticTypeCheck(operand, SequenceType.OPTIONAL_NUMERIC, backwardsCompatible,
                role, visitor);
        operand = visitor.typeCheck(operand, contextItemType);
        if (operand != oldop) {
            adoptChildExpression(operand);
        }
        if (operand instanceof Literal) {
            Value v = ((Literal)operand).getValue();
            if (v instanceof NumericValue) {
                return new Literal(((NumericValue)v).negate());
            }
        }
        return this;
    }

    /**
     * Determine the data type of the expression, if this is known statically
     * @param th the type hierarchy cache
     */

    /*@NotNull*/
    public ItemType getItemType(TypeHierarchy th) {
        return operand.getItemType(th);
    }

    /**
     * Evaluate the expression.
     */

    public Item evaluateItem(XPathContext context) throws XPathException {

        NumericValue v1 = (NumericValue) operand.evaluateItem(context);
        if (v1 == null) {
            return backwardsCompatible ? DoubleValue.NaN : null;
        }
        return v1.negate();
    }

    /**
     * Copy an expression. This makes a deep copy.
     *
     * @return the copy of the original expression
     */

    /*@NotNull*/
    public Expression copy() {
        return new NegateExpression(getBaseExpression().copy());
    }

    protected String displayOperator(Configuration config) {
        return "-";
    }

    /**
     * Get a name identifying the kind of expression, in terms meaningful to a user.
     *
     * @return a name identifying the kind of expression, in terms meaningful to a user.
     *         The name will always be in the form of a lexical XML QName, and should match the name used
     *         in explain() output displaying the expression.
     */
    @Override
    public String getExpressionName() {
        return "unaryMinus";
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
