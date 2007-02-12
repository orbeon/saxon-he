package net.sf.saxon.expr;
import net.sf.saxon.Configuration;
import net.sf.saxon.om.Item;
import net.sf.saxon.om.SequenceIterator;
import net.sf.saxon.trans.XPathException;
import net.sf.saxon.type.BuiltInAtomicType;
import net.sf.saxon.type.ItemType;
import net.sf.saxon.type.TypeHierarchy;
import net.sf.saxon.value.BooleanValue;
import net.sf.saxon.value.Int64Value;
import net.sf.saxon.value.NumericValue;

import java.io.PrintStream;
import java.util.Iterator;

/**
* PositionRange: a boolean expression that tests whether the position() is
* within a certain range. This expression can occur in any context but it is
* optimized when it appears as a predicate (see FilterIterator)
*/

public final class PositionRange extends Expression {

    private Expression minPosition;
    private Expression maxPosition; // may be null to indicate either (a) an open-ended range, or
                                    // (b) that max is same as min.
    boolean maxSameAsMin = false;

    /**
    * Create a position range
    */

    public PositionRange(Expression min, Expression max) {
        minPosition = min;
        maxPosition = max;
        adoptChildExpression(min);
        adoptChildExpression(max);
    }

    /**
     * Create a position "range" for an exact position
     */

     public PositionRange(Expression pos) {
        minPosition = pos;
        adoptChildExpression(pos);
        maxSameAsMin = true;
    }

    /**
    * Create a constant position range
    */

    public PositionRange(int min, int max) {
        minPosition = Literal.makeLiteral(Int64Value.makeIntegerValue(min));
        adoptChildExpression(minPosition);
        if (max == Integer.MAX_VALUE) {
            maxPosition = null;
        } else if (max == min) {
            maxPosition = null;
            maxSameAsMin = true;
        } else {
            maxPosition = Literal.makeLiteral(Int64Value.makeIntegerValue(max));
            adoptChildExpression(maxPosition);
        }
    }

    /**
     * Get the starting (min) position of the range
     */

    public Expression getMinPosition() {
        return minPosition;
    }

    /**
     * Get the ending (max) position of the range. If the range is open-ended,
     * or if the max position is by definition the same as the min, return null
     */

    public Expression getMaxPosition() {
        return maxPosition;
    }

    /**
     * Test whether the max position is (by definition) the same as the min position
     */

    public boolean isMaxSameAsMin() {
        return maxSameAsMin;
    }

    /**
    * Simplify an expression
    * @return the simplified expression
    */

     public Expression simplify(StaticContext env) throws XPathException {
        minPosition = minPosition.simplify(env);
        if (maxPosition != null) {
            maxPosition = maxPosition.simplify(env);
        }
        return this;
    }

    /**
    * Type-check the expression
    */

    public Expression typeCheck(StaticContext env, ItemType contextItemType) throws XPathException {
        minPosition = minPosition.typeCheck(env, contextItemType);
        if (maxPosition != null) {
            maxPosition = maxPosition.typeCheck(env, contextItemType);
        }
        return this;
    }

    /**
     * Perform optimisation of an expression and its subexpressions.
     * <p/>
     * <p>This method is called after all references to functions and variables have been resolved
     * to the declaration of the function or variable, and after all type checking has been done.</p>
     *
     * @param opt             the optimizer in use. This provides access to supporting functions; it also allows
     *                        different optimization strategies to be used in different circumstances.
     * @param env             the static context of the expression
     * @param contextItemType the static type of "." at the point where this expression is invoked.
     *                        The parameter is set to null if it is known statically that the context item will be undefined.
     *                        If the type of the context item is not known statically, the argument is set to
     *                        {@link net.sf.saxon.type.Type#ITEM_TYPE}
     * @return the original expression, rewritten if appropriate to optimize execution
     * @throws net.sf.saxon.trans.StaticError if an error is discovered during this phase
     *                                        (typically a type error)
     */

    public Expression optimize(Optimizer opt, StaticContext env, ItemType contextItemType) throws XPathException {
        minPosition = minPosition.optimize(opt, env, contextItemType);
        if (maxPosition != null) {
            maxPosition = maxPosition.optimize(opt, env, contextItemType);
        }
        return this;
    }

    /**
     * Determine the special properties of this expression
     * @return {@link StaticProperty#NON_CREATIVE}.
     */

    public int computeSpecialProperties() {
        int p = super.computeSpecialProperties();
        return p | StaticProperty.NON_CREATIVE;
    }

    /**
    * Evaluate the expression
    */

    public Item evaluateItem(XPathContext c) throws XPathException {
        int p = c.getContextPosition();
        if (maxPosition == null) {
            NumericValue min = (NumericValue)minPosition.evaluateItem(c);
            if (min == null) {
                return BooleanValue.FALSE;
            }
            return BooleanValue.get(maxSameAsMin ? p == min.longValue() : p >= min.longValue());
        } else {
            NumericValue min = (NumericValue)minPosition.evaluateItem(c);
            if (min == null) {
                return BooleanValue.FALSE;
            }
            NumericValue max = (NumericValue)maxPosition.evaluateItem(c);
            if (max == null) {
                return BooleanValue.FALSE;
            }
            return BooleanValue.get(p >= min.longValue() && p <= max.longValue());
        }
    }

    /**
     * Make an iterator over a range of a sequence determined by this position range
     */

    public SequenceIterator makePositionIterator(SequenceIterator base, XPathContext c) throws XPathException {
        int low, high;
        NumericValue min = (NumericValue)minPosition.evaluateItem(c);
        low = (int)min.longValue();
        if (maxPosition == null) {
            if (maxSameAsMin) {
                high = low;
            } else {
                return TailIterator.make(base, low);
            }
        } else {
            NumericValue max = (NumericValue)maxPosition.evaluateItem(c);
            high = (int)max.longValue();
        }
        return PositionIterator.make(base, low, high);
    }

    /**
    * Determine the data type of the expression
    * @return Type.BOOLEAN
     * @param th
     */

    public ItemType getItemType(TypeHierarchy th) {
        return BuiltInAtomicType.BOOLEAN;
    }

    /**
    * Determine the static cardinality
    */

    public int computeCardinality() {
        return StaticProperty.EXACTLY_ONE;
    }

    /**
    * Get the dependencies
    */

    public int getIntrinsicDependencies() {
        return StaticProperty.DEPENDS_ON_POSITION;
    }

    /**
     * Get the immediate sub-expressions of this expression. Default implementation
     * returns a zero-length array, appropriate for an expression that has no
     * sub-expressions.
     *
     * @return an iterator containing the sub-expressions of this expression
     */

    public Iterator iterateSubExpressions() {
        if (maxPosition == null) {
            return new MonoIterator(minPosition);
        } else {
            return new PairIterator(minPosition, maxPosition);
        }
    }

   /**
     * Replace one subexpression by a replacement subexpression
     * @param original the original subexpression
     * @param replacement the replacement subexpression
     * @return true if the original subexpression is found
     */

    public boolean replaceSubExpression(Expression original, Expression replacement) {
        boolean found = false;
        if (minPosition == original) {
            minPosition = replacement;
            found = true;
        }
        if (maxPosition == original) {
            maxPosition = replacement;
            found = true;
        }
                return found;
    }

    /**
     * Test if the first and last position are both constant 1
     */

    public boolean isFirstPositionOnly() {
        return Literal.isConstantOne(minPosition) &&
                    (maxSameAsMin || Literal.isConstantOne(maxPosition));

    }

    /**
     * Test whether the range is focus-dependent. An example of a focus-dependent range is
     * (1 to position()). We could treat last() specially but we don't.
     */

    public boolean hasFocusDependentRange() {
        return ((minPosition.getDependencies() & StaticProperty.DEPENDS_ON_FOCUS) != 0) ||
                (maxPosition != null && (maxPosition.getDependencies() & StaticProperty.DEPENDS_ON_FOCUS) != 0);
    }

    /**
     * Test if the position range matches at most one item
     */

    public boolean matchesAtMostOneItem() {
        return maxSameAsMin ||
                (maxPosition != null && minPosition.equals(maxPosition) && !hasFocusDependentRange());
    }

    /**
     * If this is an open-ended range with a constant start position, make a TailExpression.
     * Otherwise return null
     */

    public TailExpression makeTailExpression(Expression start) {
        if (maxPosition == null &&
                minPosition instanceof Literal &&
                ((Literal)minPosition).getValue() instanceof Int64Value &&
                !maxSameAsMin) {
            return new TailExpression(start, (int)((Int64Value)((Literal)minPosition).getValue()).longValue());
        } else {
            return null;
        }

    }

    /**
    * Diagnostic print of expression structure
    */

    public void display(int level, PrintStream out, Configuration config) {
        out.println(ExpressionTool.indent(level) + "positionRange");
        minPosition.display(level+1, out, config);
        if (maxPosition == null) {
            if (maxSameAsMin) {
                out.println(ExpressionTool.indent(level+1) + "(one item only)");
            } else {
                out.println(ExpressionTool.indent(level+1) + "(end)");
            }
        } else {
            maxPosition.display(level+1, out, config);
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
