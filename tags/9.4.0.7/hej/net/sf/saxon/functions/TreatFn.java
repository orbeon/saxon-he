package net.sf.saxon.functions;

import net.sf.saxon.expr.CallableExpression;
import net.sf.saxon.expr.StaticProperty;
import net.sf.saxon.expr.XPathContext;
import net.sf.saxon.om.Item;
import net.sf.saxon.om.SequenceIterator;
import net.sf.saxon.trans.XPathException;

/**
* This class supports the XPath 2.0 functions exactly-one(), one-or-more(), zero-or-one().
* Because Saxon doesn't do strict static type checking, these are essentially identity
* functions; the run-time type checking is done as part of the function call mechanism
*/

public class TreatFn extends SystemFunction implements CallableExpression{

    /**
     * Return the error code to be used for type errors
     */

    public String getErrorCodeForTypeErrors() {
        switch (operation) {
            case StaticProperty.ALLOWS_ZERO_OR_ONE:
                return "FORG0003";
            case StaticProperty.ALLOWS_ONE_OR_MORE:
                return "FORG0004";
            case StaticProperty.EXACTLY_ONE:
                return "FORG0005";
            default:
                return "XPTY0004";
        }
    }

    /**
    * Evaluate the function
    */

    /*@Nullable*/ public Item evaluateItem(XPathContext context) throws XPathException {
        return argument[0].evaluateItem(context);
    }

    /**
    * Iterate over the results of the function
    */

    /*@NotNull*/
    public SequenceIterator iterate(XPathContext context) throws XPathException {
       return argument[0].iterate(context);
    }

	public SequenceIterator call(SequenceIterator[] arguments,
			XPathContext context) throws XPathException {
		return arguments[0];
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