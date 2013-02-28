package net.sf.saxon.expr.flwor;

import net.sf.saxon.expr.Expression;
import net.sf.saxon.expr.XPathContext;
import net.sf.saxon.trans.XPathException;

/**
 * The class represents the final stage in a push-mode tuple pipeline. The previous stages
 * have stored the values corresponding to the current tuple in local variables on the
 * stack; all that remains is to evaluate the return expression (with reference to these
 * variables) and send the results to the current receiver.
 */
public class ReturnClausePush extends TuplePush {

    private Expression returnExpr;

    public ReturnClausePush(Expression returnExpr) {
        this.returnExpr = returnExpr;
    }

    /**
     * Notify the availability of the next tuple. Before calling this method,
     * the supplier of the tuples must set all the variables corresponding
     * to the supplied tuple in the local stack frame associated with the context object
     *
     * @param context the dynamic evaluation context
     */
    @Override
    public void processTuple(XPathContext context) throws XPathException {
        returnExpr.process(context);
    }

    /**
     * Close the tuple stream, indicating that no more tuples will be supplied
     */
    @Override
    public void close() throws XPathException {
        // no action
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