////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
// Copyright (c) 2014 Saxonica Limited.
// This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
// If a copy of the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
// This Source Code Form is "Incompatible With Secondary Licenses", as defined by the Mozilla Public License, v. 2.0.
////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

package net.sf.saxon.functions;

import net.sf.saxon.expr.XPathContext;
import net.sf.saxon.om.Item;
import net.sf.saxon.om.Sequence;
import net.sf.saxon.om.ZeroOrOne;
import net.sf.saxon.trans.XPathException;
import net.sf.saxon.tree.util.FastStringBuffer;
import net.sf.saxon.value.StringValue;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;

/**
 * Implementation of fn:unparsed-text()
 */
public class UnparsedText extends UnparsedTextFunction {

    /**
     * This method handles evaluation of the function:
     * it returns a StringValue in the case of unparsed-text(), or a BooleanValue
     * in the case of unparsed-text-available(). In the case of unparsed-text-lines()
     * this shouldn't be called, but we deal with it anyway.
     */

    public Item evaluateItem(XPathContext context) throws XPathException {
        StringValue hrefVal = (StringValue) argument[0].evaluateItem(context);
        String encoding = getNumberOfArguments() == 2 ? argument[1].evaluateItem(context).getStringValue() : null;
        return evalUnparsedText(hrefVal, encoding, context);
    }

    /**
     * Evaluate the expression
     *
     * @param context   the dynamic evaluation context
     * @param arguments the values of the arguments, supplied as SequenceIterators
     * @return the result of the evaluation, in the form of a SequenceIterator
     * @throws net.sf.saxon.trans.XPathException
     *          if a dynamic error occurs during the evaluation of the expression
     */
    public ZeroOrOne<StringValue> call(XPathContext context, Sequence[] arguments) throws XPathException {
        StringValue hrefVal = (StringValue) arguments[0].head();
        String encoding = getNumberOfArguments() == 2 ? arguments[1].head().getStringValue() : null;
        return new ZeroOrOne<StringValue>(evalUnparsedText(hrefVal, encoding, context));
    }

    public StringValue evalUnparsedText(StringValue hrefVal, String encoding, XPathContext context) throws XPathException {
        CharSequence content;
        StringValue result;
        try {
            if (hrefVal == null) {
                return null;
            }
            String href = hrefVal.getStringValue();

            content = readFile(href, expressionBaseURI, encoding, context);
            result = new StringValue(content);
        } catch (XPathException err) {
            err.maybeSetErrorCode(getErrorCodePrefix(context) + "1170");
            throw err;
        }
        return result;
    }



    // diagnostic method to output the octets of a file

    public static void main(String[] args) throws Exception {
        FastStringBuffer sb1 = new FastStringBuffer(FastStringBuffer.MEDIUM);
        FastStringBuffer sb2 = new FastStringBuffer(FastStringBuffer.MEDIUM);
        File file = new File(args[0]);
        InputStream is = new FileInputStream(file);
        while (true) {
            int b = is.read();
            if (b < 0) {
                System.out.println(sb1.toString());
                System.out.println(sb2.toString());
                break;
            }
            sb1.append(Integer.toHexString(b) + " ");
            sb2.append((char) b + " ");
            if (sb1.length() > 80) {
                System.out.println(sb1.toString());
                System.out.println(sb2.toString());
                sb1 = new FastStringBuffer(FastStringBuffer.MEDIUM);
                sb2 = new FastStringBuffer(FastStringBuffer.MEDIUM);
            }
        }
        is.close();
    }

}
