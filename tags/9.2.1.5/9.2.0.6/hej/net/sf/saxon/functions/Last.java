package net.sf.saxon.functions;
import net.sf.saxon.expr.*;
import net.sf.saxon.om.Item;
import net.sf.saxon.trans.XPathException;
import net.sf.saxon.type.ItemType;
import net.sf.saxon.value.Int64Value;

/**
* Implement the XPath 2.0 function last()
*/

public class Last extends SystemFunction {

    // TODO: eliminate last() as a run-time expression: rewrite all uses statically
    // For example SEQ[last()-1] becomes let $s := SEQ return item-at($s, count($s) - 1)
    // (Not clear that this is possible with calls linked to xsl:apply-templates)

    /**
    * preEvaluate: this method suppresses compile-time evaluation by doing nothing
    * (because the value of the expression depends on the runtime context)
     * @param visitor an expression visitor
     */

    public Expression preEvaluate(ExpressionVisitor visitor) {
        return this;
    }

    public Expression typeCheck(ExpressionVisitor visitor, ItemType contextItemType) throws XPathException {
        if (contextItemType == null) {
            XPathException err = new XPathException("The context for last() is undefined");
            err.setErrorCode("XPDY0002");
            err.setIsTypeError(true);
            err.setLocator(this);
            throw err;
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
// The Original Code is: all this file.
//
// The Initial Developer of the Original Code is Michael H. Kay.
//
// Portions created by (your name) are Copyright (C) (your legal entity). All Rights Reserved.
//
// Contributor(s): none.
//
