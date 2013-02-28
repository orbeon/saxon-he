package net.sf.saxon.style;

import net.sf.saxon.expr.Expression;
import net.sf.saxon.expr.instruct.Executable;
import net.sf.saxon.om.AttributeCollection;
import net.sf.saxon.trans.XPathException;
import net.sf.saxon.type.ItemType;

/**
* Handler for xsl:matching-substring and xsl:non-matching-substring elements in stylesheet.
* New at XSLT 2.0<BR>
*/

public class XSLMatchingSubstring extends StyleElement {

    /**
     * Determine the type of item returned by this instruction (only relevant if
     * it is an instruction).
     * @return the item type returned
     */

    protected ItemType getReturnedItemType() {
        return getCommonChildItemType();
    }


    public void prepareAttributes() throws XPathException {
		AttributeCollection atts = getAttributeList();
		for (int a=0; a<atts.getLength(); a++) {
        	checkUnknownAttribute(atts.getNodeName(a));
        }
    }

    /**
    * Determine whether this type of element is allowed to contain a template-body
    * @return true: yes, it may contain a template-body
    */

    public boolean mayContainSequenceConstructor() {
        return true;
    }

    public void validate(Declaration decl) throws XPathException {
        if (!(getParent() instanceof XSLAnalyzeString)) {
            compileError(getDisplayName() + " must be immediately within xsl:analyze-string", "XTSE0010");
        }
    }

    /*@NotNull*/ public Expression compile(Executable exec, Declaration decl) throws XPathException {
        throw new UnsupportedOperationException("XSLMatchingSubstring#compile() should not be called");
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