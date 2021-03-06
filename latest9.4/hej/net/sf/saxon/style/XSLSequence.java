package net.sf.saxon.style;

import net.sf.saxon.expr.Expression;
import net.sf.saxon.expr.Literal;
import net.sf.saxon.expr.instruct.Executable;
import net.sf.saxon.om.AttributeCollection;
import net.sf.saxon.om.Axis;
import net.sf.saxon.om.NodeInfo;
import net.sf.saxon.om.StandardNames;
import net.sf.saxon.trans.XPathException;
import net.sf.saxon.tree.iter.AxisIterator;
import net.sf.saxon.type.AnyItemType;
import net.sf.saxon.type.ItemType;
import net.sf.saxon.type.TypeHierarchy;


/**
 * An xsl:sequence element in the stylesheet. <br>
 * The xsl:sequence element takes attributes:<ul>
 * <li>a mandatory attribute select="expression".</li>
 * </ul>
 */

public final class XSLSequence extends StyleElement {

    private Expression select;

    /**
    * Determine whether this node is an instruction.
    * @return true - it is an instruction
    */

    public boolean isInstruction() {
        return true;
    }

    /**
     * Determine the type of item returned by this instruction (only relevant if
     * it is an instruction).
     * @return the item type returned
     */

    protected ItemType getReturnedItemType() {
        if (select == null) {
            return AnyItemType.getInstance();
        }
        final TypeHierarchy th = getConfiguration().getTypeHierarchy();
        return select.getItemType(th);
    }

    /**
    * Determine whether this type of element is allowed to contain a template-body
    * @return true: yes, it may contain a template-body
    */

    public boolean mayContainSequenceConstructor() {
        return false;
    }

    /**
    * Determine whether this type of element is allowed to contain an xsl:fallback
    * instruction
    */

    public boolean mayContainFallback() {
        return true;
    }

    public void prepareAttributes() throws XPathException {

		String selectAtt = null;

		AttributeCollection atts = getAttributeList();

		for (int a=0; a<atts.getLength(); a++) {
			String f = atts.getQName(a);
			if (f.equals(StandardNames.SELECT)) {
        		selectAtt = atts.getValue(a);
        	} else {
        		checkUnknownAttribute(atts.getNodeName(a));
        	}
        }

        if (selectAtt!=null) {
            select = makeExpression(selectAtt);
        } else {
            reportAbsence(StandardNames.SELECT);
            select = Literal.makeEmptySequence();
        }
    }

    public void validate(Declaration decl) throws XPathException {
        AxisIterator kids = iterateAxis(Axis.CHILD);
        while (true) {
            NodeInfo child = kids.next();
            if (child == null) break;
            if (!(child instanceof XSLFallback)) {
                compileError("The only child node allowed for xsl:sequence is an xsl:fallback instruction", "XTSE0010");
                break;
            }
        }
        select = typeCheck("select", select);
    }

    /*@Nullable*/ public Expression compile(Executable exec, Declaration decl) {
        return select;
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