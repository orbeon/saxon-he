////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
// Copyright (c) 2014 Saxonica Limited.
// This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
// If a copy of the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
// This Source Code Form is "Incompatible With Secondary Licenses", as defined by the Mozilla Public License, v. 2.0.
////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

package net.sf.saxon.functions;

import net.sf.saxon.expr.Callable;
import net.sf.saxon.expr.XPathContext;
import net.sf.saxon.om.Sequence;
import net.sf.saxon.om.ZeroOrOne;
import net.sf.saxon.trans.XPathException;
import net.sf.saxon.value.BooleanValue;
import net.sf.saxon.value.StringValue;

/**
 * Implements the XPath 2.0 fn:codepoint-equal() function.
 * <p/>
 * <p>Compares two strings using the unicode codepoint collation. (The function was introduced
 * specifically to allow URI comparison: URIs are promoted to strings when necessary.) </p>
 */

public class CodepointEqual extends SystemFunctionCall implements Callable {

    /**
     * Evaluate the expression
     */

    /*@Nullable*/
    public BooleanValue evaluateItem(XPathContext context) throws XPathException {
        StringValue op1 = (StringValue) argument[0].evaluateItem(context);
        StringValue op2 = (StringValue) argument[1].evaluateItem(context);
        return codepointEqual(op1, op2);
    }

    public ZeroOrOne<BooleanValue> call(XPathContext context, Sequence[] arguments) throws XPathException {
        StringValue op1 = (StringValue) arguments[0].head();
        StringValue op2 = (StringValue) arguments[1].head();
        return new ZeroOrOne<BooleanValue>(codepointEqual(op1, op2));
    }

    private static BooleanValue codepointEqual(StringValue s1, StringValue s2) {
        if (s1 == null || s2 == null) {
            return null;
        }
        return BooleanValue.get(s1.getStringValue().equals(s2.getStringValue()));
    }

}

