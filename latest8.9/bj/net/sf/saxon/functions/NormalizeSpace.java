package net.sf.saxon.functions;
import net.sf.saxon.expr.*;
import net.sf.saxon.om.Item;
import net.sf.saxon.om.FastStringBuffer;
import net.sf.saxon.trans.XPathException;
import net.sf.saxon.trans.StaticError;
import net.sf.saxon.value.AtomicValue;
import net.sf.saxon.value.StringValue;
import net.sf.saxon.value.Whitespace;
import net.sf.saxon.value.Value;
import net.sf.saxon.type.ItemType;

import java.util.StringTokenizer;

/**
 * Implement the XPath normalize-space() function
 */

public class NormalizeSpace extends SystemFunction {

    /**
     * Determine the intrinsic dependencies of an expression, that is, those which are not derived
     * from the dependencies of its subexpressions. For example, position() has an intrinsic dependency
     * on the context position, while (position()+1) does not. The default implementation
     * of the method returns 0, indicating "no dependencies".
     *
     * @return a set of bit-significant flags identifying the "intrinsic"
     *         dependencies. The flags are documented in class net.sf.saxon.value.StaticProperty
     */

    public int getIntrinsicDependencies() {
        int d = super.getIntrinsicDependencies();
        if (argument.length == 0) {
            d |= StaticProperty.DEPENDS_ON_CONTEXT_ITEM;
        }
        return d;
    }

    public Expression typeCheck(StaticContext env, ItemType contextItemType) throws XPathException {
        if (argument.length == 0 && contextItemType == null) {
            StaticError err = new StaticError("The context item for normalize-space() is undefined");
            err.setErrorCode("XPDY0002");
            err.setIsTypeError(true);
            err.setLocator(this);
            throw err;
        }
        return super.typeCheck(env, contextItemType);
    }

    /**
    * Pre-evaluate a function at compile time. Functions that do not allow
    * pre-evaluation, or that need access to context information, can override this method.
    */

    public Expression preEvaluate(StaticContext env) throws XPathException {
        if (argument.length == 0) {
            return this;
        } else {
            return Literal.makeLiteral((Value)evaluateItem(env.makeEarlyEvaluationContext()));
        }
    }

    /**
    * Evaluate in a general context
    */

    public Item evaluateItem(XPathContext c) throws XPathException {
        if (argument.length == 0) {
            Item item = c.getContextItem();
            if (item == null) {
                dynamicError("Context item for normalize-space() is undefined", "FONC0001", c);
            }
            return StringValue.makeStringValue(
                    Whitespace.collapseWhitespace(item.getStringValueCS()));
        } else {
            AtomicValue sv = (AtomicValue)argument[0].evaluateItem(c);
            if (sv==null) {
                return StringValue.EMPTY_STRING;
            }
            return StringValue.makeStringValue(
                    Whitespace.collapseWhitespace(sv.getStringValueCS()));
        }
    }

    /**
     * The algorithm that does the work: it removes leading and trailing whitespace, and
     * replaces internal whitespace by a single space character. The code is optimized for
     * two special cases: where the string is all whitespace, and where it contains no spaces
     * at all (including the case where it is empty). In these two cases it avoids creating
     * a new object.
    */

    public static CharSequence normalize(CharSequence s) {
        if (Whitespace.containsWhitespace(s)) {
            StringTokenizer st = new StringTokenizer(s.toString()); // TODO: treats FF (formfeed) as whitespace
            if (st.hasMoreTokens()) {
                FastStringBuffer sb = new FastStringBuffer(s.length());
                while (true) {
                    sb.append(st.nextToken());
                    if (st.hasMoreTokens()) {
                        sb.append(' ');
                    } else {
                        break;
                    }
                }
                return sb.condense();
            } else {
                return "";
            }
        } else {
            return s;
        }
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
