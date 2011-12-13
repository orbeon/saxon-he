package net.sf.saxon.functions;

import net.sf.saxon.expr.*;
import net.sf.saxon.expr.parser.ExpressionVisitor;
import net.sf.saxon.expr.parser.Token;
import net.sf.saxon.om.Item;
import net.sf.saxon.om.SequenceIterator;
import net.sf.saxon.trans.XPathException;
import net.sf.saxon.tree.iter.LookaheadIterator;
import net.sf.saxon.value.BooleanValue;


/**
 * Implementation of the fn:exists function
 */
public class Exists extends Aggregate {

    public int getImplementationMethod() {
        return super.getImplementationMethod() | WATCH_METHOD;
    }        

    /**
     * Check whether this specific instance of the expression is negatable
     * @return true if it is
     */

    public boolean isNegatable(ExpressionVisitor visitor) {
        return true;
    }

    /**
     * Return the negation of the expression
     * @return the negation of the expression
     */

    public Expression negate() {
        FunctionCall fc = SystemFunction.makeSystemFunction("empty", getArguments());
        fc.setLocationId(getLocationId());
        return fc;
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
     * @throws net.sf.saxon.trans.XPathException
     *          if an error is discovered during this phase
     *          (typically a type error)
     */

    /*@NotNull*/
    public Expression optimize(/*@NotNull*/ ExpressionVisitor visitor, ExpressionVisitor.ContextItemType contextItemType) throws XPathException {
        Expression e2 = super.optimize(visitor, contextItemType);
        if (e2 != this) {
            return e2;
        }
        // See if we can deduce the answer from the cardinality
        int c = argument[0].getCardinality();
        if (c == StaticProperty.ALLOWS_ONE_OR_MORE) {
            return new Literal(BooleanValue.TRUE);
        } else if (c == StaticProperty.ALLOWS_ZERO) {
            return new Literal(BooleanValue.FALSE);
        }
        // Rewrite
        //    exists(A|B) => exists(A) or exists(B)
        if (argument[0] instanceof VennExpression && !visitor.isOptimizeForStreaming()) {
            VennExpression v = (VennExpression)argument[0];
            if (v.getOperator() == Token.UNION) {
                FunctionCall e0 = SystemFunction.makeSystemFunction("exists", new Expression[]{v.getOperands()[0]});
                FunctionCall e1 = SystemFunction.makeSystemFunction("exists", new Expression[]{v.getOperands()[1]});
                return new OrExpression(e0, e1).optimize(visitor, contextItemType);
            }
        }
        return this;
    }

    /**
    * Evaluate the function
    */

    public Item evaluateItem(XPathContext context) throws XPathException {
        return BooleanValue.get(effectiveBooleanValue(context));
    }

    /**
    * Evaluate the function in a boolean context
    */

    public boolean effectiveBooleanValue(XPathContext c) throws XPathException {
        SequenceIterator iter = argument[0].iterate(c);
        boolean result;
        if ((iter.getProperties() & SequenceIterator.LOOKAHEAD) != 0) {
            result = ((LookaheadIterator)iter).hasNext();
        } else {
            result = iter.next() != null;
        }
        iter.close();
        return result;
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