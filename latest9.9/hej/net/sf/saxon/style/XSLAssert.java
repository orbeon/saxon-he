////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
// Copyright (c) 2018 Saxonica Limited.
// This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
// If a copy of the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
// This Source Code Form is "Incompatible With Secondary Licenses", as defined by the Mozilla Public License, v. 2.0.
////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

package net.sf.saxon.style;

import net.sf.saxon.expr.Expression;
import net.sf.saxon.expr.Literal;
import net.sf.saxon.expr.StringLiteral;
import net.sf.saxon.expr.instruct.Block;
import net.sf.saxon.expr.instruct.Choose;
import net.sf.saxon.expr.instruct.Message;
import net.sf.saxon.functions.SystemFunction;
import net.sf.saxon.om.AttributeCollection;
import net.sf.saxon.om.StructuredQName;
import net.sf.saxon.trans.XPathException;


/**
 * An xsl:assert element in an XSLT 3.0 stylesheet.
 */

public final class XSLAssert extends StyleElement {

    private Expression test = null;
    private Expression select = null;
    private Expression errorCode = null;

    /**
     * Determine whether this node is an instruction.
     *
     * @return true - it is an instruction
     */

    public boolean isInstruction() {
        return true;
    }

    /**
     * Determine whether this type of element is allowed to contain a template-body
     *
     * @return true: yes, it may contain a template-body
     */

    public boolean mayContainSequenceConstructor() {
        return true;
    }

    public void prepareAttributes() {

        String testAtt = null;
        String selectAtt = null;
        String errorCodeAtt = null;
        AttributeCollection atts = getAttributeList();

        for (int a = 0; a < atts.getLength(); a++) {
            String f = atts.getQName(a);
            switch (f) {
                case "test":
                    testAtt = atts.getValue(a);
                    test = makeExpression(testAtt, a);
                    break;
                case "select":
                    selectAtt = atts.getValue(a);
                    select = makeExpression(selectAtt, a);
                    break;
                case "error-code":
                    errorCodeAtt = atts.getValue(a);
                    errorCode = makeAttributeValueTemplate(errorCodeAtt, a);
                    break;
                default:
                    checkUnknownAttribute(atts.getNodeName(a));
                    break;
            }
        }

        if (testAtt == null) {
            reportAbsence("test");
        }

    }

    public void validate(ComponentDeclaration decl) throws XPathException {
        select = typeCheck("select", select);
        test = typeCheck("test", test);
        if (errorCode == null) {
            errorCode = new StringLiteral("Q{http://www.w3.org/2005/xqt-errors}XTMM9001");
        } else {
            errorCode = typeCheck("error-code", errorCode);
        }
    }

    public Expression compile(Compilation exec, ComponentDeclaration decl) throws XPathException {
        if (exec.getCompilerInfo().isAssertionsEnabled()) {
            Expression b = compileSequenceConstructor(exec, decl, true);
            if (b != null) {
                if (select == null) {
                    select = b;
                } else {
                    select = Block.makeBlock(select, b);
                    select.setLocation(
                        allocateLocation());
                }
            }
            if (select == null) {
                select = new StringLiteral("xsl:message (no content)");
            }
            if (errorCode instanceof StringLiteral) {
                // resolve any QName prefix now
                String code = ((StringLiteral) errorCode).getStringValue();
                if (code.contains(":") && !code.startsWith("Q{")) {
                    StructuredQName name = makeQName(code);
                    errorCode = new StringLiteral(name.getEQName());
                }
            }
            Message msg = new Message(select, new StringLiteral("yes"), errorCode);
            msg.setIsAssert(true);
            if (!(errorCode instanceof StringLiteral)) {
                // evaluation of the error code may need the namespace context
                msg.setRetainedStaticContext(makeRetainedStaticContext());
            }
            Expression condition = SystemFunction.makeCall("not", test.getRetainedStaticContext(), test);

            return new Choose(new Expression[]{condition}, new Expression[]{msg});
        } else {
            // assertions are disabled (the default)
            return Literal.makeEmptySequence();
        }
    }

}
