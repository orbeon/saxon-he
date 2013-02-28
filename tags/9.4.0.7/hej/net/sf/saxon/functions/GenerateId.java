package net.sf.saxon.functions;

import net.sf.saxon.expr.Expression;
import net.sf.saxon.expr.StaticProperty;
import net.sf.saxon.expr.XPathContext;
import net.sf.saxon.expr.parser.ExpressionVisitor;
import net.sf.saxon.om.Item;
import net.sf.saxon.om.NodeInfo;
import net.sf.saxon.trans.XPathException;
import net.sf.saxon.tree.util.FastStringBuffer;
import net.sf.saxon.value.StringValue;

/**
* This class supports the generate-id() function
*/

public class GenerateId extends SystemFunction {

    /**
    * Simplify and validate.
     * @param visitor an expression visitor
     */

     /*@NotNull*/
     public Expression simplify(/*@NotNull*/ ExpressionVisitor visitor) throws XPathException {
        useContextItemAsDefault(visitor);
        return simplifyArguments(visitor);
    }

    /**
     * Determine the special properties of this expression. The generate-id()
     * function is a special case: it is considered creative if its operand
     * is creative, so that generate-id(f()) is not taken out of a loop
     */

    public int computeSpecialProperties() {
        int p = super.computeSpecialProperties();
        return p & ~StaticProperty.NON_CREATIVE;
    }

    /**
    * Evaluate the function in a string context
    */

    public Item evaluateItem(XPathContext c) throws XPathException {
        NodeInfo node = (NodeInfo)argument[0].evaluateItem(c);
        if (node==null) {
            return StringValue.EMPTY_STRING;
        }

        FastStringBuffer buffer = new FastStringBuffer(FastStringBuffer.TINY);
        node.generateId(buffer);
        buffer.condense();
        return new StringValue(buffer);

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