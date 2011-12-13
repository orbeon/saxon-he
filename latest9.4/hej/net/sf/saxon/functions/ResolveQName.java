package net.sf.saxon.functions;

import net.sf.saxon.expr.CallableExpression;
import net.sf.saxon.expr.XPathContext;
import net.sf.saxon.om.*;
import net.sf.saxon.trans.XPathException;
import net.sf.saxon.type.BuiltInAtomicType;
import net.sf.saxon.value.AtomicValue;
import net.sf.saxon.value.QNameValue;
import net.sf.saxon.value.Value;


/**
* This class supports the resolve-QName function in XPath 2.0
*/

public class ResolveQName extends SystemFunction implements CallableExpression {

    /**
    * Evaluate the expression
    */

    public Item evaluateItem(XPathContext context) throws XPathException {
        AtomicValue arg0 = (AtomicValue)argument[0].evaluateItem(context);
        NodeInfo element = (NodeInfo)argument[1].evaluateItem(context);
        return resolveQName(arg0, element, context);
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
        QNameValue result = resolveQName((AtomicValue)arguments[0].next(), (NodeInfo)arguments[1].next(), context);
        return Value.asIterator(result);
    }

    /**
     * Internal method to do the real work
     * @param qname the first argument
     * @param element the second argument
     * @param context the dynamic context
     * @return the result
     * @throws XPathException
     */

    /*@Nullable*/ private QNameValue resolveQName(AtomicValue qname, NodeInfo element, XPathContext context) throws XPathException {
        if (qname == null) {
            return null;
        }
        CharSequence lexicalQName = qname.getStringValueCS();
        final NameChecker checker = context.getConfiguration().getNameChecker();

        NamespaceResolver resolver = new InscopeNamespaceResolver(element);
        StructuredQName qName;

        try {
            qName= StructuredQName.fromLexicalQName(lexicalQName, true, checker, resolver);
        } catch (XPathException e) {
            e.maybeSetLocation(this);
            throw e;
        }

        return new QNameValue(qName, BuiltInAtomicType.QNAME);
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