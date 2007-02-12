package net.sf.saxon.expr;
import net.sf.saxon.Configuration;
import net.sf.saxon.om.Item;
import net.sf.saxon.om.SequenceIterator;
import net.sf.saxon.trans.DynamicError;
import net.sf.saxon.trans.XPathException;
import net.sf.saxon.type.BuiltInAtomicType;
import net.sf.saxon.type.ItemType;
import net.sf.saxon.type.TypeHierarchy;
import net.sf.saxon.value.*;

/**
* A NumericPromoter performs numeric promotion on each item in a supplied sequence
*/

public final class NumericPromoter extends UnaryExpression {

    private BuiltInAtomicType requiredType; // always xs:float or xs:double

    /**
    * Constructor
    * @param sequence this must be a sequence of atomic values. This is not checked; a ClassCastException
    * will occur if the precondition is not satisfied.
    * @param requiredType the item type to which all items in the sequence should be converted,
    * using the rules for "cast as".
    */

    public NumericPromoter(Expression sequence, BuiltInAtomicType requiredType) {
        super(sequence);
        this.requiredType = requiredType;
        ExpressionTool.copyLocationInfo(sequence, this);
    }

    /**
    * Simplify an expression
    */

     public Expression simplify(StaticContext env) throws XPathException {
        operand = operand.simplify(env);
        if (operand instanceof Literal) {
            if (((Literal)operand).getValue() instanceof AtomicValue) {
                return Literal.makeLiteral(
                        promote(((AtomicValue)((Literal)operand).getValue()), null));
            } else {
                return Literal.makeLiteral(
                        ((Value)SequenceExtent.makeSequenceExtent(iterate(env.makeEarlyEvaluationContext()))).reduce());
            }
        }
        return this;
    }

    /**
    * Type-check the expression
    */

    public Expression typeCheck(StaticContext env, ItemType contextItemType) throws XPathException {
        operand = operand.typeCheck(env, contextItemType);
        return this;
    }

    /**
    * Optimize the expression
    */

    public Expression optimize(Optimizer opt, StaticContext env, ItemType contextItemType) throws XPathException {
        operand = operand.optimize(opt, env, contextItemType);
        return this;
    }

    /**
    * Iterate over the sequence of values
    */

    public SequenceIterator iterate(final XPathContext context) throws XPathException {
        SequenceIterator base = operand.iterate(context);
        ItemMappingFunction promoter = new ItemMappingFunction() {
            public Item map(Item item) throws XPathException {
                return promote(((AtomicValue)item), context);
            }
        };
        return new ItemMappingIterator(base, promoter);
    }

    /**
    * Evaluate as an Item. This should only be called if the expression has cardinality zero-or-one
    */

    public Item evaluateItem(XPathContext context) throws XPathException {
        Item item = operand.evaluateItem(context);
        if (item==null) return null;
        return promote(((AtomicValue)item), context);
    }

    /**
     * Perform the promotion
     */

    private AtomicValue promote(AtomicValue value, XPathContext context) throws XPathException {
        AtomicValue v = value;
        if (!(v instanceof NumericValue || v instanceof UntypedAtomicValue)) {
            final TypeHierarchy th = context.getConfiguration().getTypeHierarchy();
            DynamicError err =
                    new DynamicError("Cannot promote non-numeric value to " + getItemType(th).toString(),
                            "XPTY0004", context);
            err.setLocator(this);
            throw err;
        }
        return v.convert(requiredType, context);
    }

    /**
     * Get the required type. Always StandardNames.XS_DOUBLE or StandardNames.XS_FLOAT
     */

    public int getRequiredType() {
        return requiredType.getFingerprint();
    }

    /**
    * Determine the data type of the items returned by the expression, if possible
    * @return a value such as Type.STRING, Type.BOOLEAN, Type.NUMBER, Type.NODE,
    * or Type.ITEM (meaning not known in advance)
     * @param th
     */

	public ItemType getItemType(TypeHierarchy th) {
        if (requiredType.equals(BuiltInAtomicType.DOUBLE)) {
            return BuiltInAtomicType.DOUBLE;
        } else {
            return BuiltInAtomicType.FLOAT;
        }
	}

    /**
     * Is this expression the same as another expression?
     */

    public boolean equals(Object other) {
        return super.equals(other) &&
                requiredType == ((NumericPromoter)other).requiredType;
    }

    /**
     * Give a string representation of the operator for use in diagnostics
     * @return the operator, as a string
     * @param config
     */

    protected String displayOperator(Configuration config) {
        final TypeHierarchy th = config.getTypeHierarchy();
        return "promote items to " + getItemType(th).toString(config.getNamePool());
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
