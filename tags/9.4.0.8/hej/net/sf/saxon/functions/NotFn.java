package net.sf.saxon.functions;

import net.sf.saxon.expr.Expression;
import net.sf.saxon.expr.Negatable;
import net.sf.saxon.expr.XPathContext;
import net.sf.saxon.expr.parser.ExpressionTool;
import net.sf.saxon.expr.parser.ExpressionVisitor;
import net.sf.saxon.expr.parser.Optimizer;
import net.sf.saxon.expr.parser.TypeChecker;
import net.sf.saxon.om.Item;
import net.sf.saxon.trans.XPathException;
import net.sf.saxon.value.BooleanValue;

/**
* This class supports the XPath functions boolean(), not(), true(), and false()
*/


public class NotFn extends SystemFunction implements Negatable {

    /**
     * Static analysis: prevent sorting of the argument
     */

    public void checkArguments(/*@NotNull*/ ExpressionVisitor visitor) throws XPathException {
        super.checkArguments(visitor);
        XPathException err = TypeChecker.ebvError(argument[0], visitor.getConfiguration().getTypeHierarchy());
        if (err != null) {
            err.setLocator(this);
            throw err;
        }
        Optimizer opt = visitor.getConfiguration().obtainOptimizer();
        argument[0] = ExpressionTool.unsortedIfHomogeneous(opt, argument[0]);
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
    public Expression optimize(/*@NotNull*/ ExpressionVisitor visitor, ExpressionVisitor.ContextItemType contextItemType) throws XPathException {
        Expression e = super.optimize(visitor, contextItemType);
        if (e == this) {
            Expression ebv = BooleanFn.rewriteEffectiveBooleanValue(argument[0], visitor, contextItemType);
            if (ebv != null) {
                argument[0] = ebv;
            }
            if (argument[0] instanceof Negatable && ((Negatable)argument[0]).isNegatable(visitor)) {
                return ((Negatable)argument[0]).negate().optimize(visitor, contextItemType);
            }
            return this;
        }
        return e;
    }

    /**
     * Check whether this specific instance of the expression is negatable
     *
     * @return true if it is
     */

    public boolean isNegatable(ExpressionVisitor visitor) {
        return true;
    }

    /**
     * Create an expression that returns the negation of this expression
     * @return the negated expression
     */

    public Expression negate() {
        return SystemFunction.makeSystemFunction("boolean", getArguments());
    }


    /**
    * Evaluate the function
    */

    public Item evaluateItem(XPathContext context) throws XPathException {
        return BooleanValue.get(effectiveBooleanValue(context));
    }

    /**
    * Evaluate the effective boolean value
    */

    public boolean effectiveBooleanValue(XPathContext c) throws XPathException {
        try {
            return !argument[0].effectiveBooleanValue(c);
        } catch (XPathException e) {
            e.maybeSetLocation(this);
            e.maybeSetContext(c);
            throw e;
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
// The Original Code is: all this file
//
// The Initial Developer of the Original Code is Saxonica Limited.
// Portions created by ___ are Copyright (C) ___. All rights reserved.
//
// Contributor(s):
//