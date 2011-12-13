package net.sf.saxon.expr;

import net.sf.saxon.expr.parser.ExpressionTool;
import net.sf.saxon.expr.parser.ExpressionVisitor;
import net.sf.saxon.trans.XPathException;

/**
 * An EagerLetExpression is the same as a LetExpression except that the variable is evaluated using
 * eager evaluation rather than lazy evaluation. This is used when performing diagnostic tracing.
 */

public class EagerLetExpression extends LetExpression {

    public EagerLetExpression() {}

    /*@NotNull*/
    public Expression optimize(ExpressionVisitor visitor, ExpressionVisitor.ContextItemType contextItemType) throws XPathException {
        Expression e = super.optimize(visitor, contextItemType);
        if (e == this) {
            setEvaluationMode(ExpressionTool.eagerEvaluationMode(sequence));
        }
        return e;
    }

    /**
     * Evaluate the variable.
     */ 
    
//    protected ValueRepresentation eval(XPathContext context) throws XPathException {
//        return ExpressionTool.eagerEvaluate(sequence, context);
//    }

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