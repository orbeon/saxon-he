package net.sf.saxon.functions;

import net.sf.saxon.expr.XPathContext;
import net.sf.saxon.sort.*;
import net.sf.saxon.trans.XPathException;
import net.sf.saxon.value.StringValue;
import net.sf.saxon.value.BooleanValue;
import net.sf.saxon.om.Item;

import java.text.RuleBasedCollator;

/**
 * Implements the fn:ends-with() function
 */
public class EndsWith extends CollatingFunction {

    /**
     * Get the effective boolean value of the expression. This returns false if the value
     * is the empty sequence, a zero-length string, a number equal to zero, or the boolean
     * false. Otherwise it returns true.
     * @param context The context in which the expression is to be evaluated
     * @return the effective boolean value
     * @throws net.sf.saxon.trans.XPathException
     *          if any dynamic error occurs evaluating the
     *          expression
     */

    public boolean effectiveBooleanValue(XPathContext context) throws XPathException {

        StringValue arg1 = (StringValue)argument[1].evaluateItem(context);
        if (arg1==null || arg1.isZeroLength()) {
            return true;
        }

        StringValue arg0 = (StringValue)argument[0].evaluateItem(context);
        if (arg0==null || arg0.isZeroLength()) {
            return false;
        }

        String s0 = arg0.getStringValue();
        String s1 = arg1.getStringValue();

        if (stringCollator instanceof CodepointCollator) {
            // fast path for this common case
            return s0.endsWith(s1);
        } else {
            StringCollator collator = getCollator(2, context);
            if (collator instanceof NamedCollation &&
                    ((NamedCollation)collator).getCollation() instanceof RuleBasedCollator) {
                collator = new RuleBasedSubstringMatcher((RuleBasedCollator)((NamedCollation)collator).getCollation());
            }

            if (collator instanceof SubstringMatcher) {
                return ((SubstringMatcher)collator).endsWith(s0, s1);
            } else {
                dynamicError("The collation requested for " + getDisplayName() +
                        " does not support substring matching", "FOCH0004", context);
                return false;
            }
        }
    }

    /**
     * Evaluate the function
     */

    public Item evaluateItem(XPathContext context) throws XPathException {
        return BooleanValue.get(effectiveBooleanValue(context));
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
