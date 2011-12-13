package net.sf.saxon.expr;

import net.sf.saxon.om.Item;
import net.sf.saxon.om.SequenceIterator;
import net.sf.saxon.trans.XPathException;

/**
* A FirstItemExpression returns the first item in the sequence returned by a given
* base expression
*/

public final class FirstItemExpression extends SingleItemFilter {

    /**
    * Private Constructor
    * @param base A sequence expression denoting sequence whose first item is to be returned
    */

    private FirstItemExpression(Expression base) {
        operand = base;
        adoptChildExpression(base);
        //computeStaticProperties();
    }

    /**
     * Static factory method
     * @param base A sequence expression denoting sequence whose first item is to be returned
     * @return the FirstItemExpression, or an equivalent
     */

    public static Expression makeFirstItemExpression(Expression base) {
        if (base instanceof FirstItemExpression) {
            return base;
        } else {
            return new FirstItemExpression(base);
        }
    }

    /**
     * Copy an expression. This makes a deep copy.
     *
     * @return the copy of the original expression
     */

    /*@NotNull*/
    public Expression copy() {
        return new FirstItemExpression(getBaseExpression().copy());
    }

    /**
    * Evaluate the expression
    */

    public Item evaluateItem(XPathContext context) throws XPathException {
        SequenceIterator iter = operand.iterate(context);
        Item result = iter.next();
        iter.close();
        return result;
    }

   
    public String getExpressionName() {
        return "firstItem";
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