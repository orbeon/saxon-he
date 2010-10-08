package net.sf.saxon.functions;

import net.sf.saxon.om.NamespaceConstant;
import net.sf.saxon.om.StructuredQName;
import net.sf.saxon.expr.Expression;
import net.sf.saxon.expr.StaticContext;
import net.sf.saxon.trans.XPathException;

/**
 * The VendorFunctionLibrary represents specially-recognized functions in the Saxon namespace. 
 */

public class VendorFunctionLibrary extends IntegratedFunctionLibrary {

    /**
     * Create the Vendor Function Library for Saxon
     */

    public VendorFunctionLibrary() {
        init();
    }

    protected void init() {
        try {
            registerFunction(new IsWholeNumber());
            registerFunction(new ItemAt());
        } catch (XPathException e) {
            throw new IllegalStateException("Failed to instantiate Saxon extension function", e);
        }
    }

    /**
     * Make a Saxon function with a given name
     * @param localName the local name of the function
     * @param env the static context
     * @param arguments the arguments of the function
     * @return an exprssion representing a call on the given function
     */

    public Expression makeSaxonFunction(String localName, StaticContext env, Expression[] arguments)
    throws XPathException {
        String uri = NamespaceConstant.SAXON;
        StructuredQName functionName = new StructuredQName("saxon", uri, localName);
        return bind(functionName, arguments, env);
    }

    /**
    * Utility routine used in constructing error messages
     * @param num a number
     * @return the string " argument" or " arguments" if num is plural
    */

    public static String pluralArguments(int num) {
        if (num==1) return " argument";
        return " arguments";
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