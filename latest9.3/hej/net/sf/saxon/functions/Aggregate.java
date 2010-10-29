package net.sf.saxon.functions;

import net.sf.saxon.expr.ExpressionTool;
import net.sf.saxon.expr.ExpressionVisitor;
import net.sf.saxon.expr.Optimizer;
import net.sf.saxon.trans.XPathException;

/**
 * This abstract class provides functionality common to the sum(), avg(), count(),
 * exists(), and empty() functions. These all take a sequence as input and produce a singleton
 * as output; and they are all independent of the ordering of the items in the input.
*/

public abstract class Aggregate extends SystemFunction  {

    /**
     * Static analysis: prevent sorting of the argument
     */

    public void checkArguments(ExpressionVisitor visitor) throws XPathException {
        super.checkArguments(visitor);
        Optimizer opt = visitor.getConfiguration().getOptimizer();
        argument[0] = ExpressionTool.unsorted(opt, argument[0], true);
        // we don't care about the order of the results, but we do care about how many nodes there are
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
// The Original Code is: all this file.
//
// The Initial Developer of the Original Code is Michael H. Kay.
//
// Portions created by (your name) are Copyright (C) (your legal entity). All Rights Reserved.
//
// Contributor(s): none.
//
