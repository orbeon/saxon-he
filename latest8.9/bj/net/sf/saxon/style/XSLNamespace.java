package net.sf.saxon.style;
import net.sf.saxon.expr.Expression;
import net.sf.saxon.expr.ExpressionTool;
import net.sf.saxon.expr.StringLiteral;
import net.sf.saxon.instruct.Executable;
import net.sf.saxon.instruct.Namespace;
import net.sf.saxon.om.*;
import net.sf.saxon.trans.XPathException;
import net.sf.saxon.value.StringValue;
import net.sf.saxon.type.Type;

/**
* An xsl:namespace element in the stylesheet. (XSLT 2.0)
*/

public class XSLNamespace extends XSLStringConstructor {

    Expression name;

    public void prepareAttributes() throws XPathException {

        String nameAtt = null;
        String selectAtt = null;

		AttributeCollection atts = getAttributeList();

		for (int a=0; a<atts.getLength(); a++) {
			int nc = atts.getNameCode(a);
			String f = getNamePool().getClarkName(nc);
			if (f==StandardNames.NAME) {
        		nameAtt = atts.getValue(a).trim();
       	    } else if (f==StandardNames.SELECT) {
        		selectAtt = atts.getValue(a).trim();
        	} else {
        		checkUnknownAttribute(nc);
        	}
        }
        if (nameAtt==null) {
            reportAbsence("name");
        } else {
            name = makeAttributeValueTemplate(nameAtt);
        }

        if (selectAtt!=null) {
            select = makeExpression(selectAtt);
        }

    }

    public void validate() throws XPathException {
        name = typeCheck("name", name);
        select = typeCheck("select", select);
        int countChildren = 0;
        NodeInfo firstChild = null;
        AxisIterator kids = iterateAxis(Axis.CHILD);
        while (true) {
            NodeInfo child = (NodeInfo)kids.next();
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

    public Expression compile(Executable exec) throws XPathException {
        Namespace inst = new Namespace(name);
        compileContent(exec, inst, new StringLiteral(StringValue.SINGLE_SPACE));
        ExpressionTool.makeParentReferences(inst);
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
// The Original Code is: all this file.
//
// The Initial Developer of the Original Code is Michael H. Kay.
//
// Portions created by (your name) are Copyright (C) (your legal entity). All Rights Reserved.
//
// Contributor(s): none.
//
