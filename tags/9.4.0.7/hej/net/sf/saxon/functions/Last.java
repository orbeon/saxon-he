package net.sf.saxon.functions;

import net.sf.saxon.expr.Expression;
import net.sf.saxon.expr.StaticProperty;
import net.sf.saxon.expr.XPathContext;
import net.sf.saxon.expr.parser.ExpressionVisitor;
import net.sf.saxon.expr.parser.PromotionOffer;
import net.sf.saxon.om.Item;
import net.sf.saxon.trans.XPathException;
import net.sf.saxon.value.Int64Value;
import net.sf.saxon.value.IntegerValue;

/**
* Implement the XPath 2.0 function last()
*/

public class Last extends SystemFunction {

    boolean contextPossiblyUndefined = true;

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
        return new IntegerValue[]{Int64Value.PLUS_ONE, MAX_SEQUENCE_LENGTH};
    }

    /*@NotNull*/
    public Expression typeCheck(ExpressionVisitor visitor, /*@Nullable*/ ExpressionVisitor.ContextItemType contextItemType) throws XPathException {
        if (contextItemType == null || contextItemType.itemType == null) {
            XPathException err = new XPathException("The context for last() is undefined");
            err.setErrorCode("XPDY0002");
            err.setIsTypeError(true);
            err.setLocator(this);
            throw err;
        } else {
            contextPossiblyUndefined = contextItemType.contextMaybeUndefined;
        }
        return super.typeCheck(visitor, contextItemType);
    }

    /**
     * Promote this expression if possible
     */

    public Expression promote(PromotionOffer offer, Expression parent) throws XPathException {
        return this;
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
        return Int64Value.makeIntegerValue(c.getLast());
    }

    /**
    * Determine the dependencies
    */

    public int getIntrinsicDependencies() {
        return StaticProperty.DEPENDS_ON_LAST;
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
