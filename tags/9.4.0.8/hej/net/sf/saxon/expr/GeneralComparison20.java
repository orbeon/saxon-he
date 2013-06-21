package net.sf.saxon.expr;

import net.sf.saxon.expr.parser.Token;

/**
 *  The class GeneralComparison20 specializes GeneralComparison for the case where
 *  the comparison is done with 2.0 semantics (i.e. with backwards compatibility off).
 *  It differs from the superclass in that it will never turn the expression into
 *  a GeneralComparison10, which could lead to non-terminating optimizations
 */
public class GeneralComparison20 extends GeneralComparison {

    /**
     * Create a relational expression identifying the two operands and the operator
     *
     * @param p0 the left-hand operand
     * @param op the operator, as a token returned by the Tokenizer (e.g. Token.LT)
     * @param p1 the right-hand operand
     */
    public GeneralComparison20(Expression p0, int op, Expression p1) {
        super(p0, op, p1);
    }

    /**
     * Copy an expression. This makes a deep copy.
     * @return the copy of the original expression
     */

    /*@NotNull*/
    public Expression copy() {
        GeneralComparison20 gc = new GeneralComparison20(operand0.copy(), operator, operand1.copy());
        gc.comparer = comparer;
        gc.singletonOperator = singletonOperator;
        gc.needsRuntimeCheck = needsRuntimeCheck;
        gc.comparisonCardinality = comparisonCardinality;
        return gc;
    }

    protected GeneralComparison getInverseComparison() {
        return new GeneralComparison20(operand1, Token.inverse(operator), operand0);
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
