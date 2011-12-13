package net.sf.saxon.functions;

import net.sf.saxon.expr.CallableExpression;
import net.sf.saxon.expr.XPathContext;
import net.sf.saxon.om.Item;
import net.sf.saxon.om.NodeInfo;
import net.sf.saxon.om.SequenceIterator;
import net.sf.saxon.trans.XPathException;
import net.sf.saxon.tree.iter.EmptyIterator;
import net.sf.saxon.tree.iter.SingletonIterator;
import net.sf.saxon.type.Type;
import net.sf.saxon.value.BooleanValue;

/**
* This class supports the nilled() function
*/

public class Nilled extends SystemFunction implements CallableExpression {

    /**
    * Evaluate the function
    */

    public Item evaluateItem(XPathContext c) throws XPathException {
        NodeInfo node = (NodeInfo)argument[0].evaluateItem(c);
        return getNilledProperty(node);
    }

    /**
     * Determine whether a node has the nilled property
     * @param node the node in question (if null, the function returns null)
     * @return the value of the nilled accessor. Returns null for any node other than an
     * element node. For an element node, returns true if the element has been validated and
     * has an xsi:nil attribute whose value is true.
     */

    /*@Nullable*/ public static BooleanValue getNilledProperty(NodeInfo node) {
        // TODO: if the type annotation is ANYTYPE, we need to keep an extra bit to represent the nilled
        // property: it will be set only if validation has been performed. A newly-constructed element using
        // validation="preserve" has nilled=false even if xsi:nil = true 
        if (node==null || node.getNodeKind() != Type.ELEMENT) {
            return null;
        }
        return BooleanValue.get(node.isNilled());
    }

    /**
     * Determine whether a node is nilled. Returns true if the value
     * of the nilled property is true; false if the value is false or absent
     */

    public static boolean isNilled(NodeInfo node) {
        BooleanValue b = getNilledProperty(node);
        return b != null && b.getBooleanValue();
    }

    /**
     * Evaluate the expression
     *
     * @param arguments the values of the arguments, supplied as SequenceIterators
     * @param context   the dynamic evaluation context
     * @return the result of the evaluation, in the form of a SequenceIterator
     * @throws net.sf.saxon.trans.XPathException
     *          if a dynamic error occurs during the evaluation of the expression
     */
    public SequenceIterator call(SequenceIterator[] arguments, XPathContext context) throws XPathException {
        NodeInfo node = (NodeInfo)arguments[0].next();
        if (node==null || node.getNodeKind() != Type.ELEMENT) {
            return EmptyIterator.getInstance();
        }
        BooleanValue result = BooleanValue.get(isNilled(node));
        return SingletonIterator.makeIterator(result);
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
