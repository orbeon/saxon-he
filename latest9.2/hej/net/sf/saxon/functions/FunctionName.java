package net.sf.saxon.functions;

import net.sf.saxon.expr.XPathContext;
import net.sf.saxon.om.FunctionItem;
import net.sf.saxon.om.Item;
import net.sf.saxon.om.StructuredQName;
import net.sf.saxon.trans.XPathException;
import net.sf.saxon.type.BuiltInAtomicType;
import net.sf.saxon.value.QNameValue;

/**
 * This class implements the function function-name(), which is a standard function in XQuery 1.1
*/

public class FunctionName extends SystemFunction {

    /**
     * Evaluate this function call at run-time
     * @param context   The XPath dynamic evaluation context
     * @return the result of the function, or null to represent an empty sequence
     * @throws net.sf.saxon.trans.XPathException
     *          if a dynamic error occurs during evaluation of the function.
     */

    public Item evaluateItem(XPathContext context) throws XPathException {
        FunctionItem f = (FunctionItem)getArguments()[0].evaluateItem(context);
        StructuredQName name = f.getFunctionName();
        if (name == null) {
            return null;
        } else {
            return new QNameValue(name, BuiltInAtomicType.QNAME);
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
