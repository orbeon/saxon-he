////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
// Copyright (c) 2015 Saxonica Limited.
// This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
// If a copy of the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
// This Source Code Form is "Incompatible With Secondary Licenses", as defined by the Mozilla Public License, v. 2.0.
////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

package net.sf.saxon.expr;

import com.saxonica.ee.bytecode.ExpressionCompiler;
import com.saxonica.ee.bytecode.LetExpressionCompiler;
import net.sf.saxon.expr.parser.ContextItemStaticInfo;
import net.sf.saxon.expr.parser.ExpressionTool;
import net.sf.saxon.expr.parser.ExpressionVisitor;
import net.sf.saxon.trans.XPathException;

/**
 * An EagerLetExpression is the same as a LetExpression except that the variable is evaluated using
 * eager evaluation rather than lazy evaluation. This is used when performing diagnostic tracing.
 */

public class EagerLetExpression extends LetExpression {

    public EagerLetExpression() {
    }

    /*@NotNull*/
    public Expression optimize(ExpressionVisitor visitor, ContextItemStaticInfo contextItemType) throws XPathException {
        Expression e = super.optimize(visitor, contextItemType);
        if (e == this) {
            setEvaluationMode(ExpressionTool.eagerEvaluationMode(getSequence()));
        }
        return e;
    }

//#ifdefined BYTECODE

    /**
     * Return the compiler of the EagerLet expression
     *
     * @return the relevant ExpressionCompiler
     */
    @Override
    public ExpressionCompiler getExpressionCompiler() {
        return new LetExpressionCompiler();
    }
//#endif

    /**
     * Evaluate the variable.
     */

//    protected ValueRepresentation eval(XPathContext context) throws XPathException {
//        return ExpressionTool.eagerEvaluate(sequence, context);
//    }

}


