package net.sf.saxon.expr;

import net.sf.saxon.instruct.Instruction;
import net.sf.saxon.instruct.TailCall;
import net.sf.saxon.instruct.UserFunction;
import net.sf.saxon.om.NamespaceConstant;
import net.sf.saxon.om.StructuredQName;
import net.sf.saxon.om.ValueRepresentation;
import net.sf.saxon.trace.ExpressionPresenter;
import net.sf.saxon.trans.XPathException;

/**
 * A compiled saxon:break instruction. The effect of executing this instruction is to register with the
 * dynamic context that a tail call on a pseudo-function break() has been made; the enclosing saxon:iterate
 * loop detects this tail call request and uses it as a signal to terminate execution of the loop.
 */
public class BreakInstr extends Instruction {

    UserFunction breakFunction;
    static ValueRepresentation[] emptyArgs = new ValueRepresentation[0];
    public static StructuredQName SAXON_BREAK = new StructuredQName("saxon", NamespaceConstant.SAXON, "break");

    /**
     * Create the instruction
     */
    public BreakInstr() {
        breakFunction = new UserFunction();
        breakFunction.setFunctionName(SAXON_BREAK);
    }

    public Expression simplify(ExpressionVisitor visitor) throws XPathException {
        return this;
    }

    public Expression copy() {
        return this;
    }


    public boolean createsNewNodes() {
        // this is a fiction, but it prevents the instruction being moved to a global variable,
        // which would be pointless and possibly harmful
        return true;
    }

    public TailCall processLeavingTail(XPathContext context) throws XPathException {
        if (context instanceof XPathContextMajor) {
            ((XPathContextMajor)context).requestTailCall(breakFunction, emptyArgs);
        }
        return null;
    }



    public void explain(ExpressionPresenter out) {
        out.startElement("saxonBreak");
        out.endElement();
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
// The Initial Developer of the Original Code is Michael H. Kay.
//
// Contributor(s):
//

