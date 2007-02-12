package net.sf.saxon.expr;

import net.sf.saxon.Configuration;
import net.sf.saxon.om.Item;
import net.sf.saxon.trans.XPathException;
import net.sf.saxon.type.ItemType;
import net.sf.saxon.type.TypeHierarchy;
import net.sf.saxon.value.DoubleValue;
import net.sf.saxon.value.NumericValue;
import net.sf.saxon.value.SequenceType;

/**
 * Negate Expression: implements the unary minus operator.
 * This expression is initially created as an ArithmeticExpression (or in backwards
 * compatibility mode, an ArithmeticExpression10) to take advantage of the type checking code.
 * So we don't need to worry about type checking or argument conversion.
 */

public class NegateExpression extends UnaryExpression {

    private boolean backwardsCompatible;

    public NegateExpression(Expression base) {
        super(base);
    }

    public void setBackwardsCompatible(boolean compatible) {
        backwardsCompatible = compatible;
    }

    public Expression typeCheck(StaticContext env, ItemType contextItemType) throws XPathException {
        // always called from ArithmeticExpression.typeCheck, so the operand has already been checked.
        // Now need to ensure that it's numeric
        Expression oldop = operand;
        RoleLocator role = new RoleLocator(RoleLocator.UNARY_EXPR, "-", 0, null);
        role.setSourceLocator(this);
        operand = TypeChecker.staticTypeCheck(operand, SequenceType.OPTIONAL_NUMERIC, backwardsCompatible,
                role, env);
        if (operand != oldop) {
            adoptChildExpression(operand);
        }
        return this;
    }

    /**
     * Determine the data type of the expression, if this is known statically
     * @param th the type hierarchy cache
     */

    public ItemType getItemType(TypeHierarchy th) {
        return operand.getItemType(th);
    }

    /**
     * Evaluate the expression.
     */

    public Item evaluateItem(XPathContext context) throws XPathException {

        NumericValue v1 = (NumericValue) operand.evaluateItem(context);
        if (v1 == null) {
            return (backwardsCompatible ? DoubleValue.NaN : null);
        }
        return v1.negate();
    }

    protected String displayOperator(Configuration config) {
        return "-";
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
