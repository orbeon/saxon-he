////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
// Copyright (c) 2018 Saxonica Limited.
// This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
// If a copy of the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
// This Source Code Form is "Incompatible With Secondary Licenses", as defined by the Mozilla Public License, v. 2.0.
////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

package net.sf.saxon.style;

import net.sf.saxon.expr.Expression;
import net.sf.saxon.om.AttributeCollection;
import net.sf.saxon.trans.XPathException;

public class XSLMergeAction extends StyleElement {

    /**
     * Determine whether this node is an instruction.
     *
     * @return true - it is an instruction
     */

    public boolean isInstruction() {
        return false;
    }

    /**
     * Determine whether this type of element is allowed to contain a sequence constructor
     *
     * @return true: yes, it may contain a sequence constructor
     */

    public boolean mayContainSequenceConstructor() {
        return true;
    }

    @Override
    public Expression compile(Compilation exec, ComponentDeclaration decl)
            throws XPathException {
        Expression content = compileSequenceConstructor(exec, decl, true);

        return content;


    }

    @Override
    protected void prepareAttributes() throws XPathException {
        AttributeCollection atts = getAttributeList();

        for (int a = 0; a < atts.getLength(); a++) {
            checkUnknownAttribute(atts.getNodeName(a));
        }

    }

    public void validate(ComponentDeclaration decl) throws XPathException {
        if (!(getParent() instanceof XSLMerge)) {
            compileError("xsl:merge-action may appear only as a child of xsl:merge", "XTSE0010");
        }
    }

}
