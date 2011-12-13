package net.sf.saxon.functions;

import net.sf.saxon.expr.CallableExpression;
import net.sf.saxon.expr.XPathContext;
import net.sf.saxon.om.*;
import net.sf.saxon.trans.XPathException;
import net.sf.saxon.tree.iter.SingletonIterator;
import net.sf.saxon.value.AnyURIValue;
import net.sf.saxon.value.StringValue;


/**
* This class supports the function namespace-uri-for-prefix()
*/

public class NamespaceForPrefix extends SystemFunction implements CallableExpression {

    /**
     * Evaluate the function
     * @param context the XPath dynamic context
     * @return the URI corresponding to the prefix supplied in the first argument, or null
     * if the prefix is not in scope
     * @throws XPathException if a failure occurs evaluating the arguments
     */

    public Item evaluateItem(XPathContext context) throws XPathException {
        NodeInfo element = (NodeInfo)argument[1].evaluateItem(context);
        StringValue p = (StringValue)argument[0].evaluateItem(context);
        return namespaceUriForPrefix(p, element);
    }

    /**
     * Evaluate the expression
     *
     * @param arguments the values of the arguments, supplied as SequenceIterators
     * @param context   the dynamic evaluation context
     * @return the result of the evaluation, in the form of a SequenceIterator
     * @throws net.sf.saxon.trans.XPathException
     *          if a dynamic error occurs during the evaluation of the expression
     */
    public SequenceIterator call(SequenceIterator[] arguments, XPathContext context) throws XPathException {
        AnyURIValue result = namespaceUriForPrefix((StringValue)arguments[0].next(), (NodeInfo)arguments[1].next());
        return SingletonIterator.makeIterator(result);
    }

    /**
     * Private supporting method
     * @param p  the prefix
     * @param element the element node
     * @return  the corresponding namespace, or null if not in scope
     */

    /*@Nullable*/ private static AnyURIValue namespaceUriForPrefix(StringValue p, NodeInfo element) {
        String prefix;
        if (p == null) {
            prefix = "";
        } else {
            prefix = p.getStringValue();
        }
        NamespaceResolver resolver = new InscopeNamespaceResolver(element);
        String uri = resolver.getURIForPrefix(prefix, true);
        if (uri == null) {
            return null;
        }
        return new AnyURIValue(uri);
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