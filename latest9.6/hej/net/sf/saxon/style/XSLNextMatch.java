////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
// Copyright (c) 2014 Saxonica Limited.
// This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
// If a copy of the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
// This Source Code Form is "Incompatible With Secondary Licenses", as defined by the Mozilla Public License, v. 2.0.
////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

package net.sf.saxon.style;

import net.sf.saxon.expr.Expression;
import net.sf.saxon.expr.instruct.NextMatch;
import net.sf.saxon.om.AttributeCollection;
import net.sf.saxon.om.AxisInfo;
import net.sf.saxon.om.NodeInfo;
import net.sf.saxon.trans.XPathException;
import net.sf.saxon.tree.iter.AxisIterator;
import net.sf.saxon.type.Type;
import net.sf.saxon.value.Whitespace;

/**
 * An xsl:next-match element in the stylesheet
 */

public class XSLNextMatch extends StyleElement {

    private boolean useTailRecursion = false;

    /**
     * Determine whether this node is an instruction.
     *
     * @return true - it is an instruction
     */

    public boolean isInstruction() {
        return true;
    }

    /**
     * Determine whether this type of element is allowed to contain an xsl:fallback
     * instruction
     */

    public boolean mayContainFallback() {
        return true;
    }

    public void prepareAttributes() throws XPathException {

        AttributeCollection atts = getAttributeList();

        for (int a = 0; a < atts.getLength(); a++) {
            checkUnknownAttribute(atts.getNodeName(a));
        }
    }

    public void validate(ComponentDeclaration decl) throws XPathException {
        AxisIterator kids = iterateAxis(AxisInfo.CHILD);
        while (true) {
            NodeInfo child = kids.next();
            if (child == null) {
                break;
            }
            if (child instanceof XSLWithParam || child instanceof XSLFallback) {
                // OK;
            } else if (child.getNodeKind() == Type.TEXT) {
                // with xml:space=preserve, white space nodes may still be there
                if (!Whitespace.isWhite(child.getStringValueCS())) {
                    compileError("No character data is allowed within xsl:next-match", "XTSE0010");
                }
            } else {
                compileError("Child element " + child.getDisplayName() +
                        " is not allowed as a child of xsl:next-match", "XTSE0010");
            }
        }

    }


    /**
     * Mark tail-recursive calls on templates and functions.
     * For most instructions, this does nothing.
     */

    protected boolean markTailCalls() {
        useTailRecursion = true;
        return true;
    }

    /*@NotNull*/
    public Expression compile(Compilation exec, ComponentDeclaration decl) throws XPathException {
        NextMatch inst = new NextMatch(useTailRecursion);
        inst.setActualParameters(getWithParamInstructions(exec, decl, false),
                getWithParamInstructions(exec, decl, true));
        return inst;
    }

}

