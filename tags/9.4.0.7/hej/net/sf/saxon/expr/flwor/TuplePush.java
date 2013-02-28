package net.sf.saxon.expr.flwor;

import net.sf.saxon.expr.XPathContext;
import net.sf.saxon.trans.XPathException;

/**
 * Abtract class representing a tuple stream (used to evaluate a FLWOR expression) in push mode
 * (where the provider of tuples activates the consumer of those tuples)
 */
public abstract class TuplePush {

    /**
     * Notify the availability of the next tuple. Before calling this method,
     * the supplier of the tuples must set all the variables corresponding
     * to the supplied tuple in the local stack frame associated with the context object
     * @param context the dynamic evaluation context
     * @throws XPathException if a dynamic error occurs
     */

    public abstract void processTuple(XPathContext context) throws XPathException;

    /**
     * Close the tuple stream, indicating that no more tuples will be supplied
     * @throws XPathException if a dynamic error occurs
     */

    public void close() throws XPathException {
        // default implementation takes no action
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