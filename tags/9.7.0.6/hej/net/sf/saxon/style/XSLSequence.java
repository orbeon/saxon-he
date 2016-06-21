////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
// Copyright (c) 2015 Saxonica Limited.
// This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
// If a copy of the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
// This Source Code Form is "Incompatible With Secondary Licenses", as defined by the Mozilla Public License, v. 2.0.
////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

package net.sf.saxon.style;

import net.sf.saxon.expr.Expression;
import net.sf.saxon.expr.Literal;
import net.sf.saxon.om.AttributeCollection;
import net.sf.saxon.om.AxisInfo;
import net.sf.saxon.om.NodeInfo;
import net.sf.saxon.trans.XPathException;
import net.sf.saxon.tree.iter.AxisIterator;


/**
 * An xsl:sequence element in the stylesheet.
 */

public class XSLSequence extends StyleElement {

    private Expression select;

    /**
     * Determine whether this node is an instruction.
     *
     * @return true - it is an instruction
     */

    public boolean isInstruction() {
        return true;
    }

    /**
     * Determine whether this type of element is allowed to contain a sequence constructor
     *
     * @return in XSLT 2.0, false. In XSLT 3.0 true: yes, it may contain a sequence constructor
     */

    public boolean mayContainSequenceConstructor() {
        return isXslt30Processor();
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

        for (int a = 0; a < atts.getLength(); a++) {
            String f = atts.getQName(a);
            if (f.equals("select")) {
                selectAtt = atts.getValue(a);
                select = makeExpression(selectAtt, a);
            } else {
                checkUnknownAttribute(atts.getNodeName(a));
            }
        }

        if (selectAtt != null) {
            //
        } else if (!isXslt30Processor()) {
            reportAbsence("select");
            select = Literal.makeEmptySequence();
        }
    }

    public void validate(ComponentDeclaration decl) throws XPathException {
        AxisIterator kids = iterateAxis(AxisInfo.CHILD);
        NodeInfo child;
        while ((child = kids.next()) != null) {
            if (!(child instanceof XSLFallback)) {
                if (isXslt30Processor()) {
                    if (select != null) {
                        compileError("An " + getDisplayName() + " element with a select attribute must be empty", "XTSE3185");
                    }
                } else {
                    compileError("The only child node allowed for \" + getDisplayName() + \" is an xsl:fallback instruction", "XTSE0010");
                }
                break;
            }
        }
        select = typeCheck("select", select);
    }

    /*@Nullable*/
    public Expression compile(Compilation exec, ComponentDeclaration decl) throws XPathException {
        if (select == null) {
            select = compileSequenceConstructor(exec, decl, false);
        }
        return select;
    }

}

