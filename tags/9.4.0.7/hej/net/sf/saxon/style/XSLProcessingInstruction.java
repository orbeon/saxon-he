package net.sf.saxon.style;

import net.sf.saxon.expr.Expression;
import net.sf.saxon.expr.StringLiteral;
import net.sf.saxon.expr.instruct.Executable;
import net.sf.saxon.expr.instruct.ProcessingInstruction;
import net.sf.saxon.trans.XPathException;
import net.sf.saxon.value.StringValue;

/**
* An xsl:processing-instruction element in the stylesheet.
*/

public class XSLProcessingInstruction extends XSLLeafNodeConstructor {

    Expression name;

    public void prepareAttributes() throws XPathException {
        name = prepareAttributesNameAndSelect();
    }

    public void validate(Declaration decl) throws XPathException {
        name = typeCheck("name", name);
        select = typeCheck("select", select);
        super.validate(decl);
    }

    /**
     * Get the error code to be returned when the element has a select attribute but is not empty.
     *
     * @return the error code defined for this condition, for this particular instruction
     */

    protected String getErrorCodeForSelectPlusContent() {
        return "XTSE0880";
    }

    public Expression compile(Executable exec, Declaration decl) throws XPathException {
        ProcessingInstruction inst = new ProcessingInstruction(name);
        compileContent(exec, decl, inst, new StringLiteral(StringValue.SINGLE_SPACE));
        return inst;
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