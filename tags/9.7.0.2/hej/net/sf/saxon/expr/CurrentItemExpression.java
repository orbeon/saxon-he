////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
// Copyright (c) 2015 Saxonica Limited.
// This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
// If a copy of the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
// This Source Code Form is "Incompatible With Secondary Licenses", as defined by the Mozilla Public License, v. 2.0.
////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

package net.sf.saxon.expr;

import com.saxonica.ee.bytecode.ContextItemCompiler;
import com.saxonica.ee.bytecode.ExpressionCompiler;

/**
 * The expression is generated when compiling the current() function in XSLT. It differs from
 * the ContextItemExpression "." only in the error code that is returned when there is no context item.
 */

public class CurrentItemExpression extends ContextItemExpression {

    /**
     * Get the error code for use when there is no context item
     *
     * @return the string "XTDE1360"
     */

    protected String getErrorCodeForUndefinedContext() {
        return "XTDE1360";
    }

//#ifdefined BYTECODE

    /**
     * Return the compiler of the CurrentItem expression
     *
     * @return the relevant ExpressionCompiler
     */
    @Override
    public ExpressionCompiler getExpressionCompiler() {
        return new ContextItemCompiler();
    }
//#endif
}

