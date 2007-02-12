package net.sf.saxon.functions;
import net.sf.saxon.codenorm.Normalizer;
import net.sf.saxon.expr.XPathContext;
import net.sf.saxon.om.Item;
import net.sf.saxon.trans.DynamicError;
import net.sf.saxon.trans.XPathException;
import net.sf.saxon.value.StringValue;
import net.sf.saxon.tinytree.CompressedWhitespace;

/**
 * Implement the XPath normalize-unicode() function
 */

public class NormalizeUnicode extends SystemFunction {

    /**
    * Evaluate in a general context
    */

    public Item evaluateItem(XPathContext c) throws XPathException {
        StringValue sv = (StringValue)argument[0].evaluateItem(c);
        if (sv==null) {
            return StringValue.EMPTY_STRING;
        }

        byte fb = Normalizer.C;
        if (argument.length == 2) {
            String form = argument[1].evaluateAsString(c).trim();
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
                DynamicError err = new DynamicError(msg);
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


        Normalizer norm = new Normalizer(fb);
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
// The Original Code is: all this file.
//
// The Initial Developer of the Original Code is Michael H. Kay.
//
// Portions created by (your name) are Copyright (C) (your legal entity). All Rights Reserved.
//
// Contributor(s): none.
//
