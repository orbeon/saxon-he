package net.sf.saxon.instruct;

import net.sf.saxon.expr.XPathContext;
import net.sf.saxon.trans.XPathException;

import java.util.Stack;

/**
 * This interface is implemented by instructions that can be executed in streaming mode. The instruction
 * offers two methods, processLeft() which is executed before processing the descendants of the context node,
 * and processRight() which is executed after processing the descendants.
 */
public interface DivisibleInstruction {

    /**
     * In streaming mode, process the first half of the instruction (for example, to start a new document or element)
     * @param contextStack Stack of XPathContext objects. The instruction should use the one at the top of the stack.
     * @param state a stack on which the instruction can save state information during the call on processLeft()
     */

    public void processLeft(Stack<XPathContext> contextStack, Stack state) throws XPathException;

    /**
     * In streaming mode, process the right half of the instruction (for example, to end a new document or element)
     * @param contextStack Stack of XPathContext objects. The instruction should use the one at the top of the stack.
     * @param state a stack on which the instruction can save state information during the call on processLeft()
     */

    public void processRight(Stack<XPathContext> contextStack, Stack state) throws XPathException;

}

//
// The contents of this file are subject to the Mozilla Public License Version
// 1.0 (the "License");
// you may not use this file except in compliance with the License. You may
// obtain a copy of the
// License at http://www.mozilla.org/MPL/
//
// Software distributed under the License is distributed on an "AS IS" basis,
// WITHOUT WARRANTY OF ANY KIND, either express or implied.
// See the License for the specific language governing rights and limitations
// under the License.
//
// The Original Code is: all this file.
//
// The Initial Developer of the Original Code is Michael Kay
//
// Portions created by (your name) are Copyright (C) (your legal entity). All
// Rights Reserved.
//
// Contributor(s): none.
//

