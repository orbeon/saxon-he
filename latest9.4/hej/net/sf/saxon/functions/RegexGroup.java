package net.sf.saxon.functions;

import net.sf.saxon.expr.StaticProperty;
import net.sf.saxon.expr.XPathContext;
import net.sf.saxon.functions.regex.RegexIterator;
import net.sf.saxon.om.Item;
import net.sf.saxon.trans.XPathException;
import net.sf.saxon.value.AtomicValue;
import net.sf.saxon.value.NumericValue;
import net.sf.saxon.value.StringValue;


public class RegexGroup extends SystemFunction {

    /**
    * Evaluate in a general context
    */

    public Item evaluateItem(XPathContext c) throws XPathException {
        AtomicValue gp0 = (AtomicValue)argument[0].evaluateItem(c);
        int gp = (int)((NumericValue)gp0).longValue();
        return StringValue.makeStringValue(getGroup(gp, c));
    }


    /*@Nullable*/ public static String getGroup(int gp, XPathContext c) {
        RegexIterator iter = c.getCurrentRegexIterator();
        if (iter == null) {
            return "";
        }
        String s = iter.getRegexGroup(gp);
        if (s == null) {
            return "";
        }
        return s;
    }

    /**
    * Determine the dependencies
    */

    public int getIntrinsicDependencies() {
       return StaticProperty.DEPENDS_ON_REGEX_GROUP;
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