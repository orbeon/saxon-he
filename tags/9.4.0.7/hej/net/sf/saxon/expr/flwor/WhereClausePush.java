package net.sf.saxon.expr.flwor;

import net.sf.saxon.expr.Expression;
import net.sf.saxon.expr.XPathContext;
import net.sf.saxon.trans.XPathException;

/**
 * This class represents the tuple stream delivered as the output of a where clause in a
 * FLWOR expression: that is, it supplies all the tuples in its input stream that satisfy
 * a specified predicate. It does not change the values of any variables in the tuple stream.
 */
public class WhereClausePush extends TuplePush {

    TuplePush destination;
    Expression predicate;

    public WhereClausePush(TuplePush destination, Expression predicate) {
        this.destination = destination;
        this.predicate = predicate;
    }

    /*
     * Process the next tuple.
     */
    @Override
    public void processTuple(XPathContext context) throws XPathException {
        if (predicate.effectiveBooleanValue(context)) {
            destination.processTuple(context);
        }
    }

    /*
     * Notify the end of the tuple stream
     */
    @Override
    public void close() throws XPathException {
        destination.close();
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