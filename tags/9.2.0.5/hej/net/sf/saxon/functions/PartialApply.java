package net.sf.saxon.functions;

import net.sf.saxon.expr.XPathContext;
import net.sf.saxon.om.FunctionItem;
import net.sf.saxon.om.Item;
import net.sf.saxon.om.ValueRepresentation;
import net.sf.saxon.trans.XPathException;
import net.sf.saxon.value.IntegerValue;
import net.sf.saxon.value.SequenceExtent;


/**
 * This class implements the function partial-apply(), which is a standard function in XQuery 1.1
*/

public class PartialApply extends SystemFunction {

    /**
     * Evaluate this function call at run-time
     * @param context   The XPath dynamic evaluation context
     * @return the result of the function, or null to represent an empty sequence
     * @throws net.sf.saxon.trans.XPathException
     *          if a dynamic error occurs during evaluation of the function.
     */

    public Item evaluateItem(XPathContext context) throws XPathException {
        FunctionItem f = (FunctionItem)getArguments()[0].evaluateItem(context);
        ValueRepresentation val = SequenceExtent.makeSequenceExtent(
                getArguments()[1].iterate(context));
        int arg = 1;
        if (getNumberOfArguments() == 3) {
            arg = (int)((IntegerValue)getArguments()[2].evaluateItem(context)).longValue();
        }
        try {
            return f.curry(arg, val);
        } catch (XPathException e) {
            e.maybeSetLocation(this);
            e.maybeSetContext(context);
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
// The Original Code is: all this file.
//
// The Initial Developer of the Original Code is Michael H. Kay.
//
// Portions created by (your name) are Copyright (C) (your legal entity). All Rights Reserved.
//
// Contributor(s): none.
//