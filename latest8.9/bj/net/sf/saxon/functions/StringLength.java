package net.sf.saxon.functions;
import net.sf.saxon.expr.*;
import net.sf.saxon.om.Item;
import net.sf.saxon.trans.XPathException;
import net.sf.saxon.trans.StaticError;
import net.sf.saxon.value.AtomicValue;
import net.sf.saxon.value.Int64Value;
import net.sf.saxon.value.StringValue;
import net.sf.saxon.value.Value;
import net.sf.saxon.type.ItemType;

/**
 * Implement the XPath string-length() function
 */

public class StringLength extends SystemFunction {

    private boolean shortcut = false;
                                // if this is set we return 0 for a zero length string,
                                // 1 for any other. Used by the optimizer.
    /**
    * Simplify and validate.
    * This is a pure function so it can be simplified in advance if the arguments are known
    */

     public Expression simplify(StaticContext env) throws XPathException {
        //useContextItemAsDefault();
        return simplifyArguments(env);
    }

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

    /**
    * Pre-evaluate a function at compile time. Functions that do not allow
    * pre-evaluation, or that need access to context information, can override this method.
    */

    public Expression preEvaluate(StaticContext env) throws XPathException {
        if (argument.length == 0) {
            return this;
        } else {
            return Literal.makeLiteral(
                    (Value)evaluateItem(env.makeEarlyEvaluationContext()));
        }
    }

    public Expression typeCheck(StaticContext env, ItemType contextItemType) throws XPathException {
        if (argument.length == 0 && contextItemType == null) {
            StaticError err = new StaticError("The context item for string-length() is undefined");
            err.setErrorCode("XPDY0002");
            err.setIsTypeError(true);
            err.setLocator(this);
            throw err;
        }
        return super.typeCheck(env, contextItemType);
    }

    /**
    * setShortCut() - used by optimizer when we only need to know if the length is non-zero
    */

    public void setShortcut() {
        shortcut = true;
    }

    /**
    * Evaluate in a general context
    */

    public Item evaluateItem(XPathContext c) throws XPathException {
        AtomicValue sv;
        if (argument.length == 0) {
            final Item contextItem = c.getContextItem();
            if (contextItem == null) {
                dynamicError("The context item for string-length() is not set", "XPDY0002", c);
            }
            sv = StringValue.makeStringValue(contextItem.getStringValueCS());
        } else {
            sv = (AtomicValue)argument[0].evaluateItem(c);
        }
        if (sv==null) {
            return Int64Value.ZERO;
        }
        CharSequence s = sv.getStringValueCS();

        if (shortcut) {
            return (s.length()>0 ? Int64Value.PLUS_ONE : Int64Value.ZERO);
        } else {
            return Int64Value.makeIntegerValue(StringValue.getStringLength(s));
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
