package net.sf.saxon.functions;

import net.sf.saxon.event.SequenceOutputter;
import net.sf.saxon.event.SequenceReceiver;
import net.sf.saxon.expr.Expression;
import net.sf.saxon.expr.ExpressionVisitor;
import net.sf.saxon.expr.PathMap;
import net.sf.saxon.expr.XPathContext;
import net.sf.saxon.instruct.DivisibleInstruction;
import net.sf.saxon.om.Item;
import net.sf.saxon.trans.XPathException;
import net.sf.saxon.type.BuiltInAtomicType;
import net.sf.saxon.value.StringValue;

import java.util.Stack;

/**
* Implement XPath function string()
*/

public class StringFn extends SystemFunction implements DivisibleInstruction {

    /**
    * Simplify and validate.
    * This is a pure function so it can be simplified in advance if the arguments are known
     * @param visitor an expression visitor
     */

     public Expression simplify(ExpressionVisitor visitor) throws XPathException {
        useContextItemAsDefault();
        argument[0].setFlattened(true);
        return simplifyArguments(visitor);
    }


    /**
     * Add a representation of a doc() call or similar function to a PathMap.
     * This is a convenience method called by the addToPathMap() methods for doc(), document(), collection()
     * and similar functions. These all create a new root expression in the path map.
     *
     * @param pathMap      the PathMap to which the expression should be added
     * @param pathMapNodes the node in the PathMap representing the focus at the point where this expression
     *                     is called. Set to null if this expression appears at the top level.
     * @return the pathMapNode representing the focus established by this expression, in the case where this
     *         expression is the first operand of a path expression or filter expression
     */

    public PathMap.PathMapNodeSet addDocToPathMap(PathMap pathMap, PathMap.PathMapNodeSet pathMapNodes) {
        PathMap.PathMapNodeSet result = argument[0].addToPathMap(pathMap, pathMapNodes);
        if (result != null) {
            result.setAtomized();
        }
        return null;
    }

    /**
    * Evaluate the function
    */

    public Item evaluateItem(XPathContext c) throws XPathException {
        try {
            Item arg = argument[0].evaluateItem(c);
            if (arg==null) {
                return StringValue.EMPTY_STRING;
            } else if (arg instanceof StringValue && ((StringValue)arg).getTypeLabel() == BuiltInAtomicType.STRING) {
                return arg;
            } else {
                return StringValue.makeStringValue(arg.getStringValueCS());
            }
        } catch (UnsupportedOperationException e) {
            // Cannot obtain the string value of a function item
            XPathException err = new XPathException(e.getMessage(), "FOTY0014");
            err.setLocator(this);
            err.setXPathContext(c);
            throw err;
        }
    }

    /**
     * In streaming mode, process the first half of the instruction (for example, to start a new document or element)
     * @param contextStack
     * @param state   a stack on which the instruction can save state information during the call on processLeft()
     */

    public void processLeft(Stack<XPathContext> contextStack, Stack state) throws XPathException {
        XPathContext context = contextStack.peek();
        SequenceReceiver out = context.getReceiver();
        state.push(out);
        SequenceOutputter out2 = new SequenceOutputter();
        out2.setPipelineConfiguration(out.getPipelineConfiguration());
        context.setReceiver(out2);
    }

    /**
     * In streaming mode, process the right half of the instruction (for example, to end a new document or element)
     * Note that unlike other divisible instructions this one doesn't push the result to the current output
     * destination, it leaves a sequenceIterator over the result on the stack.
     * @param contextStack
     * @param state   a stack on which the instruction can save state information during the call on processLeft()
     */

    public void processRight(Stack<XPathContext> contextStack, Stack state) throws XPathException {
        XPathContext context = contextStack.peek();
        CharSequence value = ((SequenceOutputter)context.getReceiver()).getFirstItem().getStringValueCS();
        SequenceReceiver out = (SequenceReceiver)state.pop();
        context.setReceiver(out);
        out.append(new StringValue(value), 0, 0);
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
