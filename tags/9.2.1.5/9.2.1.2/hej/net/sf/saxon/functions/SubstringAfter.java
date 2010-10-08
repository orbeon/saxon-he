package net.sf.saxon.functions;

import net.sf.saxon.expr.XPathContext;
import net.sf.saxon.sort.*;
import net.sf.saxon.trans.XPathException;
import net.sf.saxon.value.StringValue;
import net.sf.saxon.om.Item;

import java.text.RuleBasedCollator;

/**
 * Implements the fn:substring-after() function
 */
public class SubstringAfter extends CollatingFunction {

    /**
     * Evaluate the function
     */

    public Item evaluateItem(XPathContext context) throws XPathException {
        StringValue arg1 = (StringValue)argument[0].evaluateItem(context);
        StringValue arg2 = (StringValue)argument[1].evaluateItem(context);

        if (arg1 == null) {
            arg1 = StringValue.EMPTY_STRING;
        }
        if (arg2 == null) {
            arg2 = StringValue.EMPTY_STRING;
        }
        if (arg2.isZeroLength()) {
            return arg1;
        }
        if (arg1.isZeroLength()) {
            return StringValue.EMPTY_STRING;
        }

        String s1 = arg1.getStringValue();
        String s2 = arg2.getStringValue();

        String result = null;
        if (stringCollator instanceof CodepointCollator) {
            // fast path for this common case
            int i = s1.indexOf(s2);
            if (i < 0) {
                result = "";
            } else {
                result = s1.substring(i + s2.length());
            }

        } else {
            StringCollator collator = getCollator(2, context);
            if (collator instanceof NamedCollation &&
                    ((NamedCollation)collator).getCollation() instanceof RuleBasedCollator) {
                collator = new RuleBasedSubstringMatcher((RuleBasedCollator)((NamedCollation)collator).getCollation());
            }

            if (collator instanceof SubstringMatcher) {
                result = ((SubstringMatcher)collator).substringAfter(s1, s2);
            } else {
                dynamicError("The collation requested for " + getDisplayName() +
                        " does not support substring matching", "FOCH0004", context);
            }
        }
        StringValue s = StringValue.makeStringValue(result);
        if (arg1.isKnownToContainNoSurrogates()) {
            s.setContainsNoSurrogates();
        }
        return s;
    }

}

//
// The contents of this file are subject to the Mozilla Public License Version
// 1.0 (the "License");
// you may not use this file except in compliance with the License. You may
// obtain a copy of the
// License at http://www.mozilla.org/MPL/
//
// Software distributed under the License is distributed on an "AS IS" basis,
// WITHOUT WARRANTY OF ANY KIND, either express or implied.
// See the License for the specific language governing rights and limitations
// under the License.
//
// The Original Code is: all this file.
//
// The Initial Developer of the Original Code is Michael Kay,
//
// Portions created by (your name) are Copyright (C) (your legal entity). All
// Rights Reserved.
//
// Contributor(s): none.
//