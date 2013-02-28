package net.sf.saxon.functions;

import net.sf.saxon.expr.CallableExpression;
import net.sf.saxon.expr.XPathContext;
import net.sf.saxon.om.Item;
import net.sf.saxon.om.SequenceIterator;
import net.sf.saxon.serialize.codenorm.Normalizer;
import net.sf.saxon.trans.XPathException;
import net.sf.saxon.tree.iter.SingletonIterator;
import net.sf.saxon.tree.tiny.CompressedWhitespace;
import net.sf.saxon.value.AtomicValue;
import net.sf.saxon.value.StringValue;
import net.sf.saxon.value.Value;
import net.sf.saxon.value.Whitespace;

/**
 * Implement the XPath normalize-unicode() function
 */

public class NormalizeUnicode extends SystemFunction implements CallableExpression {

    /**
    * Evaluate in a general context
    */

    public Item evaluateItem(XPathContext c) throws XPathException {
        StringValue sv = (StringValue)argument[0].evaluateItem(c);
        if (sv==null) {
            return StringValue.EMPTY_STRING;
        }
        String form = (argument.length == 1 ? "NFC" : Whitespace.trim(argument[1].evaluateAsString(c)));
        return normalize(sv, form, c);
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
    public SequenceIterator<? extends AtomicValue> call(SequenceIterator[] arguments, /*@NotNull*/ XPathContext context) throws XPathException {
        StringValue sv = (StringValue)arguments[0].next();
        if(sv == null){
            return SingletonIterator.makeIterator(StringValue.EMPTY_STRING);
        }
        String nf = (arguments.length == 1 ? "NFC" : Whitespace.trim(arguments[1].next().getStringValue()));
        StringValue result = normalize(sv, nf, context);
        return Value.asIterator(result);
    }

    public StringValue normalize(StringValue sv, String form, XPathContext c) throws XPathException {
        byte fb = Normalizer.C;
        if (argument.length == 2) {
            if (form.equalsIgnoreCase("NFC")) {
                fb = Normalizer.C;
            } else if (form.equalsIgnoreCase("NFD")) {
                fb = Normalizer.D;
            } else if (form.equalsIgnoreCase("NFKC")) {
                fb = Normalizer.KC;
            } else if (form.equalsIgnoreCase("NFKD")) {
                fb = Normalizer.KD;
            } else if (form.length() == 0) {
                return sv;
            } else {
                String msg = "Normalization form " + form + " is not supported";
                XPathException err = new XPathException(msg);
                err.setErrorCode("FOCH0003");
                err.setXPathContext(c);
                err.setLocator(this);
                throw err;
            }
        }

        // fast path for ASCII strings: normalization is a no-op
        boolean allASCII = true;
        CharSequence chars = sv.getStringValueCS();
        if (chars instanceof CompressedWhitespace) {
            return sv;
        }
        for (int i=chars.length()-1; i>=0; i--) {
            if (chars.charAt(i) > 127) {
                allASCII = false;
                break;
            }
        }
        if (allASCII) {
            return sv;
        }


        Normalizer norm = new Normalizer(fb, c.getConfiguration());
        CharSequence result = norm.normalize(sv.getStringValueCS());
        return StringValue.makeStringValue(result);
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