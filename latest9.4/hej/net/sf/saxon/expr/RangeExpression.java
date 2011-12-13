package net.sf.saxon.expr;

import net.sf.saxon.expr.parser.ExpressionTool;
import net.sf.saxon.expr.parser.ExpressionVisitor;
import net.sf.saxon.expr.parser.RoleLocator;
import net.sf.saxon.expr.parser.TypeChecker;
import net.sf.saxon.om.SequenceIterator;
import net.sf.saxon.trans.XPathException;
import net.sf.saxon.tree.iter.EmptyIterator;
import net.sf.saxon.type.BuiltInAtomicType;
import net.sf.saxon.type.ItemType;
import net.sf.saxon.type.TypeHierarchy;
import net.sf.saxon.value.*;

/**
* A RangeExpression is an expression that represents an integer sequence as
* a pair of end-points (for example "x to y").
* If the end-points are equal, the sequence is of length one.
 * <p>From Saxon 7.8, the sequence must be ascending; if the end-point is less
 * than the start-point, an empty sequence is returned. This is to allow
 * expressions of the form "for $i in 1 to count($seq) return ...." </p>
*/

public class RangeExpression extends BinaryExpression {

    /**
     * Construct a RangeExpression
     * @param start expression that computes the start of the range
     * @param op represents the operator "to", needed only because this class is a subclass of
     * BinaryExpression which needs an operator
     * @param end expression that computes the end of the range
    */

    public RangeExpression(Expression start, int op, Expression end) {
        super(start, op, end);
    }

    /**
    * Type-check the expression
    */

    /*@NotNull*/
    public Expression typeCheck(ExpressionVisitor visitor, ExpressionVisitor.ContextItemType contextItemType) throws XPathException {
        operand0 = visitor.typeCheck(operand0, contextItemType);
        operand1 = visitor.typeCheck(operand1, contextItemType);

        boolean backCompat = visitor.getStaticContext().isInBackwardsCompatibleMode();
        RoleLocator role0 = new RoleLocator(RoleLocator.BINARY_EXPR, "to", 0);
        //role0.setSourceLocator(this);
        operand0 = TypeChecker.staticTypeCheck(
                operand0, SequenceType.OPTIONAL_INTEGER, backCompat, role0, visitor);

        RoleLocator role1 = new RoleLocator(RoleLocator.BINARY_EXPR, "to", 1);
        //role1.setSourceLocator(this);
        operand1 = TypeChecker.staticTypeCheck(
                operand1, SequenceType.OPTIONAL_INTEGER, backCompat, role1, visitor);

        return makeConstantRange();
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
        operand0 = visitor.optimize(operand0, contextItemType);
        operand1 = visitor.optimize(operand1, contextItemType);
        return makeConstantRange();

    }

    private Expression makeConstantRange() throws XPathException {
        if (operand0 instanceof Literal && operand1 instanceof Literal) {
            Value v0 = ((Literal)operand0).getValue();
            Value v1 = ((Literal)operand1).getValue();
            if (v0 instanceof Int64Value && v1 instanceof Int64Value) {
                long i0 = ((Int64Value)v0).longValue();
                long i1 = ((Int64Value)v1).longValue();
                Literal result;
                if (i0 > i1) {
                    result = Literal.makeEmptySequence();
                } else if (i0 == i1) {
                    result = Literal.makeLiteral(Int64Value.makeIntegerValue(i0));
                } else {
                    try {
                        result = Literal.makeLiteral(new IntegerRange(i0, i1));
                    } catch (XPathException e) {
                        e.maybeSetLocation(this);
                        throw e;
                    }
                }
                ExpressionTool.copyLocationInfo(this, result);
                return result;
            }
        }
        return this;
    }


    /**
    * Get the data type of the items returned
     * @param th the type hierarchy cache
     */

    /*@NotNull*/
    public ItemType getItemType(TypeHierarchy th) {
        return BuiltInAtomicType.INTEGER;
    }

    /**
    * Determine the static cardinality
    */

    public int computeCardinality() {
        return StaticProperty.ALLOWS_ZERO_OR_MORE;
    }

    /**
     * For an expression that returns an integer or a sequence of integers, get
     * a lower and upper bound on the values of the integers that may be returned, from
     * static analysis. The default implementation returns null, meaning "unknown" or
     * "not applicable". Other implementations return an array of two IntegerValue objects,
     * representing the lower and upper bounds respectively. The values
     * UNBOUNDED_LOWER and UNBOUNDED_UPPER are used by convention to indicate that
     * the value may be arbitrarily large. The values MAX_STRING_LENGTH and MAX_SEQUENCE_LENGTH
     * are used to indicate values limited by the size of a string or the size of a sequence.
     *
     * @return the lower and upper bounds of integer values in the result, or null to indicate
     *         unknown or not applicable.
     */
    /*@Nullable*/@Override
    public IntegerValue[] getIntegerBounds() {
        IntegerValue[] start = operand0.getIntegerBounds();
        IntegerValue[] end = operand0.getIntegerBounds();
        if (start == null || end == null) {
            return null;
        } else {
            // range is from the smallest possible start value to the largest possible end value
            return new IntegerValue[]{start[0], end[1]};
        }
    }

    /**
     * Copy an expression. This makes a deep copy.
     *
     * @return the copy of the original expression
     */

    /*@NotNull*/
    public Expression copy() {
        return new RangeExpression(operand0.copy(), operator, operand1.copy());
    }

    /**
    * Return an iteration over the sequence
    */

    /*@NotNull*/
    public SequenceIterator iterate(XPathContext context) throws XPathException {
        AtomicValue av1 = (AtomicValue)operand0.evaluateItem(context);
        if (av1 == null) {
            return EmptyIterator.getInstance();
        }
        NumericValue v1 = (NumericValue)av1;

        AtomicValue av2 = (AtomicValue)operand1.evaluateItem(context);
        if (av2 == null) {
            return EmptyIterator.getInstance();
        }
        NumericValue v2 = (NumericValue)av2;

        if (v1.compareTo(v2) > 0) {
            return EmptyIterator.getInstance();
        }
        return new RangeIterator(v1.longValue(), v2.longValue());
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