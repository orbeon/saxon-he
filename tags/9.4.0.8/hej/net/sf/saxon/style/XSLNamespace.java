package net.sf.saxon.style;

import net.sf.saxon.expr.Expression;
import net.sf.saxon.expr.StringLiteral;
import net.sf.saxon.expr.instruct.Executable;
import net.sf.saxon.expr.instruct.NamespaceConstructor;
import net.sf.saxon.om.Axis;
import net.sf.saxon.om.NodeInfo;
import net.sf.saxon.trans.XPathException;
import net.sf.saxon.tree.iter.AxisIterator;
import net.sf.saxon.type.Type;
import net.sf.saxon.value.StringValue;

/**
* An xsl:namespace element in the stylesheet. (XSLT 2.0)
*/

public class XSLNamespace extends XSLLeafNodeConstructor {

    /*@Nullable*/ Expression name;

    public void prepareAttributes() throws XPathException {
        name = prepareAttributesNameAndSelect();
    }

    public void validate(Declaration decl) throws XPathException {
        name = typeCheck("name", name);
        select = typeCheck("select", select);
        int countChildren = 0;
        NodeInfo firstChild = null;
        AxisIterator kids = iterateAxis(Axis.CHILD);
        while (true) {
            NodeInfo child = kids.next();
            if (child == null) {
                break;
            }
            if (child instanceof XSLFallback) {
                continue;
            }
            if (select != null) {
                String errorCode = getErrorCodeForSelectPlusContent();
                compileError("An " + getDisplayName() + " element with a select attribute must be empty", errorCode);
            }
            countChildren++;
            if (firstChild == null) {
                firstChild = child;
            } else {
                break;
            }
        }

        if (select == null) {
            if (countChildren == 0) {
                // there are no child nodes and no select attribute
                select = new StringLiteral(StringValue.EMPTY_STRING);
            } else if (countChildren == 1) {
                // there is exactly one child node
                if (firstChild.getNodeKind() == Type.TEXT) {
                    // it is a text node: optimize for this case
                    select = new StringLiteral(firstChild.getStringValueCS());
                }
            }
        }
    }

    /**
     * Get the error code to be returned when the element has a select attribute but is not empty.
     *
     * @return the error code defined for this condition, for this particular instruction
     */

    protected String getErrorCodeForSelectPlusContent() {
        return "XTSE0910";
    }

    public Expression compile(Executable exec, Declaration decl) throws XPathException {
        NamespaceConstructor inst = new NamespaceConstructor(name);
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