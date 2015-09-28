////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
// Copyright (c) 2014 Saxonica Limited.
// This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
// If a copy of the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
// This Source Code Form is "Incompatible With Secondary Licenses", as defined by the Mozilla Public License, v. 2.0.
////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

package net.sf.saxon.expr;

import com.saxonica.ee.bytecode.ExpressionCompiler;
import com.saxonica.ee.bytecode.NegateExpressionCompiler;
import net.sf.saxon.Configuration;
import net.sf.saxon.expr.parser.ContextItemStaticInfo;
import net.sf.saxon.expr.parser.ExpressionVisitor;
import net.sf.saxon.expr.parser.RoleLocator;
import net.sf.saxon.expr.parser.TypeChecker;
import net.sf.saxon.om.GroundedValue;
import net.sf.saxon.trans.XPathException;
import net.sf.saxon.type.ItemType;
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

    /**
     * Create a NegateExpression
     *
     * @param base the expression that computes the value whose sign is to be reversed
     */

    public NegateExpression(Expression base) {
        super(base);
    }

    /**
     * Set whether the expression is to be evaluated in XPath 1.0 compatibility mode
     *
     * @param compatible true if XPath 1.0 compatibility mode is enabled
     */

    public void setBackwardsCompatible(boolean compatible) {
        backwardsCompatible = compatible;
    }

    /**
     * Ask whether the expression is to be evaluated in XPath 1.0 compatibility mode
     *
     * @return true if XPath 1.0 compatibility mode is enabled
     */

    public boolean isBackwardsCompatible() {
        return backwardsCompatible;
    }

    protected OperandRole getOperandRole() {
        return OperandRole.SINGLE_ATOMIC;
    }

    /*@NotNull*/
    public Expression typeCheck(ExpressionVisitor visitor, ContextItemStaticInfo contextInfo) throws XPathException {
        Expression oldop = operand;
        RoleLocator role = new RoleLocator(RoleLocator.UNARY_EXPR, "-", 0);
        operand = TypeChecker.staticTypeCheck(operand, SequenceType.OPTIONAL_NUMERIC, backwardsCompatible,
                role, visitor);
        operand = visitor.typeCheck(operand, contextInfo);
        if (operand != oldop) {
            adoptChildExpression(operand);
        }
        if (operand instanceof Literal) {
            GroundedValue v = ((Literal) operand).getValue();
            if (v instanceof NumericValue) {
                return Literal.makeLiteral(((NumericValue) v).negate(), getContainer());
            }
        }
        return this;
    }

    /**
     * Determine the data type of the expression, if this is known statically
     * <p/>
     * <p/>
     * /*@NotNull
     */
    public ItemType getItemType() {
        return operand.getItemType();
    }

    /**
     * Evaluate the expression.
     */

    public NumericValue evaluateItem(XPathContext context) throws XPathException {

        NumericValue v1 = (NumericValue) operand.evaluateItem(context);
        if (v1 == null) {
            return backwardsCompatible ? DoubleValue.NaN : null;
        }
        return v1.negate();
    }

//#ifdefined BYTECODE

    /**
     * Return the compiler of the Negate expression
     *
     * @return the relevant ExpressionCompiler
     */
    @Override
    public ExpressionCompiler getExpressionCompiler() {
        return new NegateExpressionCompiler();
    }
//#endif

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

