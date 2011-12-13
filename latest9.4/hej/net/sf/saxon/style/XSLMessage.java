package net.sf.saxon.style;

import net.sf.saxon.expr.Expression;
import net.sf.saxon.expr.StringLiteral;
import net.sf.saxon.expr.instruct.Block;
import net.sf.saxon.expr.instruct.Executable;
import net.sf.saxon.expr.instruct.Message;
import net.sf.saxon.om.AttributeCollection;
import net.sf.saxon.om.Axis;
import net.sf.saxon.om.StandardNames;
import net.sf.saxon.trans.XPathException;
import net.sf.saxon.value.Whitespace;


/**
* An xsl:message element in the stylesheet. <br>
*/

public final class XSLMessage extends StyleElement {

    private Expression terminate = null;
    private Expression select = null;

    /**
    * Determine whether this node is an instruction.
    * @return true - it is an instruction
    */

    public boolean isInstruction() {
        return true;
    }

    /**
    * Determine whether this type of element is allowed to contain a template-body
    * @return true: yes, it may contain a template-body
    */

    public boolean mayContainSequenceConstructor() {
        return true;
    }

    public void prepareAttributes() throws XPathException {

        String terminateAtt = null;
        String selectAtt = null;
		AttributeCollection atts = getAttributeList();

		for (int a=0; a<atts.getLength(); a++) {
			String f = atts.getQName(a);
			if (f.equals(StandardNames.TERMINATE)) {
        		terminateAtt = Whitespace.trim(atts.getValue(a));
            } else if (f.equals(StandardNames.SELECT)) {
                selectAtt = atts.getValue(a);

            } else {
        		checkUnknownAttribute(atts.getNodeName(a));
        	}
        }
        if (selectAtt!=null) {
            select = makeExpression(selectAtt);
        }


        if (terminateAtt==null) {
            terminateAtt = "no";
        }

        checkAttributeValue("terminate", terminateAtt, true, StyleElement.YES_NO);
        terminate = makeAttributeValueTemplate(terminateAtt);
    }

    public void validate(Declaration decl) throws XPathException {
        select = typeCheck("select", select);
        terminate = typeCheck("terminate", terminate);
    }

    public Expression compile(Executable exec, Declaration decl) throws XPathException {
        Expression b = compileSequenceConstructor(exec, decl, iterateAxis(Axis.CHILD), true);
        if (b != null) {
            if (select == null) {
                select = b;
            } else {
                select = Block.makeBlock(select, b);
                select.setLocationId(
                            allocateLocationId(getSystemId(), getLineNumber()));
            }
        }
        if (select == null) {
            select = new StringLiteral("xsl:message (no content)");
        }
        return new Message(select, terminate);
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