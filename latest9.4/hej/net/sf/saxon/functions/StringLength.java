package net.sf.saxon.functions;

import net.sf.saxon.expr.Expression;
import net.sf.saxon.expr.Literal;
import net.sf.saxon.expr.StaticProperty;
import net.sf.saxon.expr.XPathContext;
import net.sf.saxon.expr.parser.ExpressionVisitor;
import net.sf.saxon.om.Item;
import net.sf.saxon.trans.XPathException;
import net.sf.saxon.value.*;

/**
 * Implement the XPath string-length() function
 */

public class StringLength extends SystemFunction {

    boolean contextPossiblyUndefined = true;

    /**
    * Simplify and validate.
    * This is a pure function so it can be simplified in advance if the arguments are known
     * @param visitor an expression visitor
     */

     /*@NotNull*/
     public Expression simplify(ExpressionVisitor visitor) throws XPathException {
        //useContextItemAsDefault();
        return simplifyArguments(visitor);
     }

    /**
     * For an expression that returns an integer or a sequence of integers, get
     * a lower and upper bound on the values of the integers that may be returned, from
     * static analysis. The default implementation returns null, meaning "unknown" or
     * "not applicable". Other implementations return an array of two IntegerValue objects,
     * representing the lower and upper bounds respectively. The values
     * UNBOUNDED_LOWER and UNBOUNDED_UPPER are used by convention to indicate that
     * the value may be arbitrarily large. The values MAX_STRING_LENGTH and MAX_SEQUENCE_LENGTH
     * are used to indicate values limited by the size of a string or the size of a sequence.
     *
     * @return the lower and upper bounds of integer values in the result, or null to indicate
     *         unknown or not applicable.
     */
    @Override
    public IntegerValue[] getIntegerBounds() {
        return new IntegerValue[]{Int64Value.ZERO, MAX_STRING_LENGTH};
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
     * @param visitor an expression visitor
     * @return the expression, either unchanged, or pre-evaluated
     */

    public Expression preEvaluate(ExpressionVisitor visitor) throws XPathException {
        if (argument.length == 0) {
            return this;
        } else {
            return Literal.makeLiteral(
                    (Value)evaluateItem(visitor.getStaticContext().makeEarlyEvaluationContext()));
        }
    }

    /*@NotNull*/
    public Expression typeCheck(ExpressionVisitor visitor, /*@Nullable*/ ExpressionVisitor.ContextItemType contextItemType) throws XPathException {
        if (argument.length == 0 && contextItemType == null) {
            XPathException err = new XPathException("The context item for string-length() is undefined");
            err.setErrorCode("XPDY0002");
            err.setIsTypeError(true);
            err.setLocator(this);
            throw err;
        } else if (contextItemType != null) {
            contextPossiblyUndefined = contextItemType.contextMaybeUndefined;
        }
        return super.typeCheck(visitor, contextItemType);
    }

    /**
     * Ask whether the context item may possibly be undefined
     * @return true if it might be undefined
     */

    public boolean isContextPossiblyUndefined() {
        return contextPossiblyUndefined;
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
                return null;
            }
            sv = StringValue.makeStringValue(contextItem.getStringValueCS());
        } else {
            sv = (AtomicValue)argument[0].evaluateItem(c);
        }
        if (sv==null) {
            return Int64Value.ZERO;
        }

        if (sv instanceof StringValue) {
            return Int64Value.makeIntegerValue(((StringValue)sv).getStringLength());
        } else {
            CharSequence s = sv.getStringValueCS();
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
// The Original Code is: all this file
//
// The Initial Developer of the Original Code is Saxonica Limited.
// Portions created by ___ are Copyright (C) ___. All rights reserved.
//
// Contributor(s):
//