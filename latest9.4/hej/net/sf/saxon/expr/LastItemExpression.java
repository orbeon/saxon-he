package net.sf.saxon.expr;

import net.sf.saxon.om.Item;
import net.sf.saxon.om.SequenceIterator;
import net.sf.saxon.trans.XPathException;
import net.sf.saxon.tree.iter.ReversibleIterator;

/**
 * A LastItemExpression returns the last item in the sequence returned by a given
 * base expression. The evaluation strategy is to read the input sequence with a one-item lookahead.
*/

public final class LastItemExpression extends SingleItemFilter {

    /**
    * Constructor
    * @param base A sequence expression denoting sequence whose first item is to be returned
    */

    public LastItemExpression(Expression base) {
        operand = base;
        adoptChildExpression(base);
        //computeStaticProperties();
    }

    /**
     * Copy an expression. This makes a deep copy.
     *
     * @return the copy of the original expression
     */

    /*@NotNull*/
    public Expression copy() {
        return new LastItemExpression(getBaseExpression().copy());
    }

    /**
    * Evaluate the expression
    */

    /*@Nullable*/ public Item evaluateItem(XPathContext context) throws XPathException {
        SequenceIterator forwards = operand.iterate(context);
        if (forwards instanceof ReversibleIterator) {
            return ((ReversibleIterator)forwards).getReverseIterator().next();
        } else {
            Item current = null;
            while (true) {
                Item item = forwards.next();
                if (item == null) {
                    return current;
                }
                current = item;
            }
        }
    }


    public String getExpressionName() {
        return "lastItem";
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