package net.sf.saxon.expr;
import net.sf.saxon.om.Item;
import net.sf.saxon.sort.AtomicComparer;
import net.sf.saxon.trans.XPathException;
import net.sf.saxon.type.BuiltInAtomicType;
import net.sf.saxon.type.ItemType;
import net.sf.saxon.type.TypeHierarchy;
import net.sf.saxon.value.AtomicValue;
import net.sf.saxon.value.BooleanValue;
import net.sf.saxon.trace.ExpressionPresenter;


/**
* Class to handle comparisons of singletons. Unlike ValueComparison, this class
* converts untyped atomic values to the type of the other argument, and returns false
 * (rather than ()) if either operand is ().
*/

public class SingletonComparison extends BinaryExpression implements ComparisonExpression {

    private AtomicComparer comparer;
    private boolean needsRuntimeCheck = true;

    /**
     * Create a singleton comparison - that is, a comparison between two singleton (0:1) sequences
     * using the general comparison semantics
     * @param p1 the first operand
     * @param operator the operator
     * @param p2 the second operand
     * @param checkTypes true if a run-time check for comparability of the operand types is needed
     */

    public SingletonComparison(Expression p1, int operator, Expression p2, boolean checkTypes) {
        super(p1, operator, p2);
        this.needsRuntimeCheck = checkTypes;
    }

    public void setAtomicComparer(AtomicComparer comp) {
        comparer = comp;
    }

    public AtomicComparer getAtomicComparer() {
        return comparer;
    }

    public int getSingletonOperator() {
        return operator;
    }

    /**
     * Determine whether untyped atomic values should be converted to the type of the other operand
     * @return true if untyped values should be converted to the type of the other operand, false if they
     * should be converted to strings.
     */

    public boolean convertsUntypedToOther() {
        return true;
    }

    /**
    * Determine the static cardinality. Returns [1..1]
    */

    public int computeCardinality() {
        return StaticProperty.EXACTLY_ONE;
    }

    /**
    * Determine the data type of the expression
    * @return Type.BOOLEAN
     * @param th the type hierarchy cache
     */

    public ItemType getItemType(TypeHierarchy th) {
        return BuiltInAtomicType.BOOLEAN;
    }


    /**
     * Copy an expression. This makes a deep copy.
     *
     * @return the copy of the original expression
     */

    public Expression copy() {
        SingletonComparison sc = new SingletonComparison(operand0.copy(), operator, operand1.copy(), needsRuntimeCheck);
        sc.comparer = comparer;
        return sc;
    }

    /**
     * Determine whether a run-time check is needed to check that the types of the arguments
     * are comparable
     * @return true if a run-time check is needed
     */

    public boolean needsRuntimeComparabilityCheck() {
        return needsRuntimeCheck;
    }


    /**
    * Evaluate the expression in a given context
    * @param context the given context for evaluation
    * @return a BooleanValue representing the result of the numeric comparison of the two operands
    */

    public Item evaluateItem(XPathContext context) throws XPathException {
        return BooleanValue.get(effectiveBooleanValue(context));
    }

    /**
    * Evaluate the expression in a boolean context
    * @param context the given context for evaluation
    * @return a boolean representing the result of the numeric comparison of the two operands
    */

    public boolean effectiveBooleanValue(XPathContext context) throws XPathException {
        AtomicValue v1 = (AtomicValue)operand0.evaluateItem(context);
        if (v1==null) {
            return false;
        }
        AtomicValue v2 = (AtomicValue)operand1.evaluateItem(context);
        if (v2==null) {
            return false;
        }

        try {
            return GeneralComparison.compare(v1, operator, v2, comparer, needsRuntimeCheck, context);
        } catch (XPathException e) {
            // re-throw the exception with location information added
            e.maybeSetLocation(this);
            e.maybeSetContext(context);
            throw e;
        }
    }

    protected void explainExtraAttributes(ExpressionPresenter out) {
        out.emitAttribute("cardinality", "singleton");
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
