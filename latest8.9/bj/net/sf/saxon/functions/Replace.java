package net.sf.saxon.functions;
import net.sf.saxon.Platform;
import net.sf.saxon.Configuration;
import net.sf.saxon.expr.Expression;
import net.sf.saxon.expr.Literal;
import net.sf.saxon.expr.StaticContext;
import net.sf.saxon.expr.XPathContext;
import net.sf.saxon.om.Item;
import net.sf.saxon.regex.RegularExpression;
import net.sf.saxon.trans.DynamicError;
import net.sf.saxon.trans.StaticError;
import net.sf.saxon.trans.XPathException;
import net.sf.saxon.value.AtomicValue;
import net.sf.saxon.value.StringValue;

import java.util.regex.PatternSyntaxException;


/**
* This class implements the replace() function for replacing
* substrings that match a regular expression
*/

public class Replace extends SystemFunction {

    private RegularExpression regexp;

    /**
    * Simplify and validate.
    * This is a pure function so it can be simplified in advance if the arguments are known
    */

     public Expression simplify(StaticContext env) throws XPathException {
        Expression e = simplifyArguments(env);

        // compile the regular expression once if possible
        if (regexp == null && !(e instanceof Literal)) {
            try {
                regexp = Matches.tryToCompile(argument, 1, 3, env);
            } catch (StaticError err) {
                err.setLocator(this);
                throw err;
            }

            // check that it's not a pattern that matches ""
            if (regexp != null && regexp.matches("")) {
                DynamicError err = new DynamicError(
                        "The regular expression in replace() must not be one that matches a zero-length string");
                err.setErrorCode("FORX0003");
                err.setLocator(this);
                throw err;
            }
        }

        return e;
    }

    /**
     * Get the compiled regular expression if available, otherwise return null
     */

    public RegularExpression getCompiledRegularExpression() {
        return regexp;
    }    

    /**
    * Evaluate the function in a string context
    */

    public Item evaluateItem(XPathContext c) throws XPathException {

        AtomicValue arg0 = (AtomicValue)argument[0].evaluateItem(c);
        if (arg0==null) {
            arg0 = StringValue.EMPTY_STRING;
        }

        AtomicValue arg2 = (AtomicValue)argument[2].evaluateItem(c);
        CharSequence replacement = arg2.getStringValueCS();
        String msg = checkReplacement(replacement);
        if (msg != null) {
            dynamicError(msg, "FORX0004", c);
        }

        RegularExpression re = regexp;
        if (re == null) {

            AtomicValue arg1 = (AtomicValue)argument[1].evaluateItem(c);

            CharSequence flags;

            if (argument.length == 3) {
                flags = "";
            } else {
                AtomicValue arg3 = (AtomicValue)argument[3].evaluateItem(c);
                flags = arg3.getStringValueCS();
            }

            try {
                final Platform platform = Configuration.getPlatform();
                re = platform.compileRegularExpression(arg1.getStringValueCS(), true, flags);
            } catch (XPathException err) {
                DynamicError de = new DynamicError(err);
                de.setErrorCode("FORX0002");
                de.setXPathContext(c);
                de.setLocator(this);
                throw de;
            } catch (PatternSyntaxException err) {
                DynamicError de = new DynamicError(err);
                de.setErrorCode("FORX0002");
                de.setXPathContext(c);
                de.setLocator(this);
                throw de;
            }

            // check that it's not a pattern that matches ""
            if (re.matches("")) {
                dynamicError(
                        "The regular expression in replace() must not be one that matches a zero-length string",
                        "FORX0003", c);
            }
        }
        String input = arg0.getStringValue();
        CharSequence res = re.replace(input, replacement);
        return StringValue.makeStringValue(res);
    }

    /**
    * Check the contents of the replacement string
     * @return null if the string is OK, or an error message if not
    */

    public static String checkReplacement(CharSequence rep) {
        for (int i=0; i<rep.length(); i++) {
            char c = rep.charAt(i);
            if (c == '$') {
                if (i+1 < rep.length()) {
                    char next = rep.charAt(++i);
                    if (next < '0' || next > '9') {
                        return "Invalid replacement string in replace(): $ sign must be followed by digit 0-9";
                    }
                } else {
                    return "Invalid replacement string in replace(): $ sign at end of string";
                }
            } else if (c == '\\') {
                if (i+1 < rep.length()) {
                    char next = rep.charAt(++i);
                    if (next != '\\' && next != '$') {
                        return "Invalid replacement string in replace(): \\ character must be followed by \\ or $";
                    }
                } else {
                    return "Invalid replacement string in replace(): \\ character at end of string";
                }
            }
        }
        return null;
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
// The Initial Developer of the Original Code is Michael H. Kay
//
// Portions created by (your name) are Copyright (C) (your legal entity). All Rights Reserved.
//
// Contributor(s): none.
//
