////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
// Copyright (c) 2015 Saxonica Limited.
// This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
// If a copy of the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
// This Source Code Form is "Incompatible With Secondary Licenses", as defined by the Mozilla Public License, v. 2.0.
////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

package net.sf.saxon.functions;

import net.sf.saxon.expr.XPathContext;
import net.sf.saxon.om.*;
import net.sf.saxon.trans.XPathException;
import net.sf.saxon.type.BuiltInAtomicType;
import net.sf.saxon.value.AtomicValue;
import net.sf.saxon.value.QNameValue;


/**
 * This class supports the resolve-QName function in XPath 2.0
 */

public class ResolveQName extends SystemFunction {

    /**
     * Evaluate the expression
     *
     * @param context   the dynamic evaluation context
     * @param arguments the values of the arguments, supplied as SequenceIterators
     * @return the result of the evaluation, in the form of a SequenceIterator
     * @throws net.sf.saxon.trans.XPathException
     *          if a dynamic error occurs during the evaluation of the expression
     */
    public ZeroOrOne<QNameValue> call(XPathContext context, Sequence[] arguments) throws XPathException {
        return new ZeroOrOne<QNameValue>(
                resolveQName((AtomicValue) arguments[0].head(), (NodeInfo) arguments[1].head()));
    }

    /**
     * Internal method to do the real work
     *
     * @param qname   the first argument
     * @param element the second argument
     * @return the result
     * @throws XPathException if a dynamic error occurs
     */

    /*@Nullable*/
    private QNameValue resolveQName(AtomicValue qname, NodeInfo element) throws XPathException {
        if (qname == null) {
            return null;
        }
        CharSequence lexicalQName = qname.getStringValueCS();

        NamespaceResolver resolver = new InscopeNamespaceResolver(element);
        StructuredQName qName = StructuredQName.fromLexicalQName(lexicalQName, true, false, resolver);

        return new QNameValue(qName, BuiltInAtomicType.QNAME);
    }

}

