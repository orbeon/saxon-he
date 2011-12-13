package net.sf.saxon.functions;

import net.sf.saxon.expr.Expression;
import net.sf.saxon.expr.parser.ExpressionTool;
import net.sf.saxon.expr.parser.ExpressionVisitor;
import net.sf.saxon.expr.parser.Optimizer;
import net.sf.saxon.trans.XPathException;

/**
* XPath 2.0 unordered() function
*/

public class Unordered extends CompileTimeFunction {

    /*@NotNull*/
    public Expression typeCheck(ExpressionVisitor visitor, ExpressionVisitor.ContextItemType contextItemType) throws XPathException {
        Expression exp = super.typeCheck(visitor, contextItemType);
        if (exp instanceof Unordered) {
            Optimizer opt = visitor.getConfiguration().obtainOptimizer();
            return ExpressionTool.unsorted(opt, ((Unordered)exp).argument[0], false);
        }
        return exp;
    }

    /*@NotNull*/
    public Expression optimize(/*@NotNull*/ ExpressionVisitor visitor, ExpressionVisitor.ContextItemType contextItemType) throws XPathException {
        Expression exp = super.optimize(visitor, contextItemType);
        if (exp instanceof Unordered) {
            return ExpressionTool.unsorted(visitor.getConfiguration().obtainOptimizer(),
                    ((Unordered) exp).argument[0], false);
        }
        return exp;
    }

    /**
    * preEvaluate: called if the argument is constant
     * @param visitor an expression visitor
     */

    public Expression preEvaluate(ExpressionVisitor visitor) throws XPathException {
        return argument[0];
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