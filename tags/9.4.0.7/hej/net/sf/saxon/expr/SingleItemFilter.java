package net.sf.saxon.expr;

import net.sf.saxon.expr.parser.ExpressionVisitor;
import net.sf.saxon.expr.parser.PromotionOffer;
import net.sf.saxon.trans.XPathException;
import net.sf.saxon.value.Cardinality;

/**
* A SingleItemFilter is an expression that selects zero or one items from a supplied sequence
*/

public abstract class SingleItemFilter extends UnaryExpression {

    /**
     * Perform optimisation of an expression and its subexpressions.
     * <p/>
     * <p>This method is called after all references to functions and variables have been resolved
     * to the declaration of the function or variable, and after all type checking has been done.</p>
     *
     * @param visitor an expresion visitor
     * @param contextItemType the static type of "." at the point where this expression is invoked.
     *                        The parameter is set to null if it is known statically that the context item will be undefined.
     *                        If the type of the context item is not known statically, the argument is set to
     *                        {@link net.sf.saxon.type.Type#ITEM_TYPE}
     * @return the original expression, rewritten if appropriate to optimize execution
     * @throws net.sf.saxon.trans.XPathException if an error is discovered during this phase
     *                                        (typically a type error)
     */

    /*@NotNull*/
    public Expression optimize(ExpressionVisitor visitor, ExpressionVisitor.ContextItemType contextItemType) throws XPathException {
        operand = visitor.optimize(operand, contextItemType);
        if (!Cardinality.allowsMany(operand.getCardinality())) {
            return operand;
        }
        return super.optimize(visitor, contextItemType);
    }

    /**
    * Promote this expression if possible
    */

    /*@Nullable*/ public Expression promote(PromotionOffer offer, Expression parent) throws XPathException {
        Expression exp = offer.accept(parent, this);
        if (exp != null) {
            return exp;
        } else {
            if (offer.action != PromotionOffer.UNORDERED) {
                // we can't push the UNORDERED property down to the operand, because order is significant
                operand = doPromotion(operand, offer);
            }
            return this;
        }
    }

    /**
     * Get the static cardinality: this implementation is appropriate for [1] and [last()] which will always
     * return something if the input is non-empty
    */

    public int computeCardinality() {
        return operand.getCardinality() & ~StaticProperty.ALLOWS_MANY;
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