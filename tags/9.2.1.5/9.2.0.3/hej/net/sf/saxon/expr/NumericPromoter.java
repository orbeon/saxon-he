package net.sf.saxon.expr;
import net.sf.saxon.om.Item;
import net.sf.saxon.om.SequenceIterator;
import net.sf.saxon.trace.ExpressionPresenter;
import net.sf.saxon.trans.XPathException;
import net.sf.saxon.type.ItemType;
import net.sf.saxon.value.AtomicValue;
import net.sf.saxon.value.SequenceExtent;
import net.sf.saxon.value.Value;

/**
* A NumericPromoter performs numeric promotion on each item in a supplied sequence.
 * There are two subclasses, to handle promotion to double and promotion to float
*/

public abstract class NumericPromoter extends UnaryExpression {

    public NumericPromoter(Expression exp) {
        super(exp);
    }

    /**
    * Simplify an expression
     * @param visitor an expression visitor
     */

     public Expression simplify(ExpressionVisitor visitor) throws XPathException {
        operand = visitor.simplify(operand);
        if (operand instanceof Literal) {
            if (((Literal)operand).getValue() instanceof AtomicValue) {
                return Literal.makeLiteral(
                        promote(((AtomicValue)((Literal)operand).getValue()), null));
            } else {
                return Literal.makeLiteral(
                        ((Value)SequenceExtent.makeSequenceExtent(
                                iterate(visitor.getStaticContext().makeEarlyEvaluationContext()))).reduce());
            }
        }
        return this;
    }

    /**
    * Type-check the expression
    */

    public Expression typeCheck(ExpressionVisitor visitor, ItemType contextItemType) throws XPathException {
        operand = visitor.typeCheck(operand, contextItemType);
        return this;
    }

    /**
    * Optimize the expression
    */

    public Expression optimize(ExpressionVisitor visitor, ItemType contextItemType) throws XPathException {
        operand = visitor.optimize(operand, contextItemType);
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
        return new ItemMappingIterator(base, promoter, true);
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
     * @param value the numeric or untyped atomic value to be promoted
     * @param context the XPath dynamic evaluation context
     * @return the value that results from the promotion
     */

    protected abstract AtomicValue promote(AtomicValue value, XPathContext context) throws XPathException;

    /**
     * Diagnostic print of expression structure. The abstract expression tree
     * is written to the supplied output destination.
     */

    public void explain(ExpressionPresenter out) {
        out.startElement("promoteNumeric");
        out.emitAttribute("to", getItemType(out.getTypeHierarchy()).toString(out.getConfiguration().getNamePool()));
        operand.explain(out);
        out.endElement();
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
