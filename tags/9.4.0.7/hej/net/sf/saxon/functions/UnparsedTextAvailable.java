package net.sf.saxon.functions;

import net.sf.saxon.expr.CallableExpression;
import net.sf.saxon.expr.XPathContext;
import net.sf.saxon.om.Item;
import net.sf.saxon.om.SequenceIterator;
import net.sf.saxon.trans.XPathException;
import net.sf.saxon.value.BooleanValue;
import net.sf.saxon.value.StringValue;
import net.sf.saxon.value.Value;


public class UnparsedTextAvailable extends UnparsedText implements CallableExpression {

    // TODO: There is now a requirement that the results should be stable

    /**
     * This method handles evaluation of the function:
     * it returns a StringValue in the case of unparsed-text(), or a BooleanValue
     * in the case of unparsed-text-available(). In the case of unparsed-text-lines()
     * this shouldn't be called, but we deal with it anyway.
     */

    public Item evaluateItem(XPathContext context) throws XPathException {
        StringValue hrefVal = (StringValue)argument[0].evaluateItem(context);
        String encoding = (getNumberOfArguments() == 2 ? argument[1].evaluateItem(context).getStringValue() : null);
        return BooleanValue.get(evalUnparsedTextAvailable(hrefVal, encoding, context));
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
        StringValue hrefVal = (StringValue)arguments[0].next();
        String encoding = (getNumberOfArguments() == 2 ? arguments[1].next().getStringValue() : null);
        return Value.asIterator(
                BooleanValue.get(
                        evalUnparsedTextAvailable(hrefVal, encoding, context)));
    }

    public boolean evalUnparsedTextAvailable(/*@Nullable*/ StringValue hrefVal, String encoding, XPathContext context) throws XPathException {
        try {
            if (hrefVal == null) {
                return false;
            }
            String href = hrefVal.getStringValue();

            readFile(href, expressionBaseURI, encoding, context);
        } catch (XPathException err) {
            return false;
        }
        return true;
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