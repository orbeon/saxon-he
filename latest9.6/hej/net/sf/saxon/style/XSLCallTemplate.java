////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
// Copyright (c) 2014 Saxonica Limited.
// This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
// If a copy of the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
// This Source Code Form is "Incompatible With Secondary Licenses", as defined by the Mozilla Public License, v. 2.0.
////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

package net.sf.saxon.style;

import net.sf.saxon.expr.Expression;
import net.sf.saxon.expr.instruct.CallTemplate;
import net.sf.saxon.expr.instruct.LocalParam;
import net.sf.saxon.expr.instruct.Template;
import net.sf.saxon.lib.NamespaceConstant;
import net.sf.saxon.om.*;
import net.sf.saxon.trans.Err;
import net.sf.saxon.trans.XPathException;
import net.sf.saxon.tree.iter.AxisIterator;
import net.sf.saxon.type.AnyItemType;
import net.sf.saxon.type.ItemType;
import net.sf.saxon.type.Type;
import net.sf.saxon.value.SequenceType;
import net.sf.saxon.value.Whitespace;

import java.util.List;

/**
 * An xsl:call-template element in the stylesheet
 */

public class XSLCallTemplate extends StyleElement {

    private static StructuredQName ERROR_TEMPLATE_NAME =
            new StructuredQName("saxon", NamespaceConstant.SAXON, "error-template");

    private StructuredQName calledTemplateName;   // the name of the called template
    private Template template = null;             // the template to be called (which may subsequently be overridden in another package)
    private boolean useTailRecursion = false;

    /**
     * Determine whether this node is an instruction.
     *
     * @return true - it is an instruction
     */

    public boolean isInstruction() {
        return true;
    }

    private boolean gettingReturnedItemType = false;

    /**
     * Determine the type of item returned by this instruction (only relevant if
     * it is an instruction).
     *
     * @return the item type returned
     */

    protected ItemType getReturnedItemType() {
        if (template == null || gettingReturnedItemType) {
            return AnyItemType.getInstance();
        } else {
            // protect against infinite recursion
            gettingReturnedItemType = true;
            ItemType result = template.getRequiredType().getPrimaryType();
            gettingReturnedItemType = false;
            return result;
        }
    }

    public void prepareAttributes() throws XPathException {

        AttributeCollection atts = getAttributeList();

        String nameAttribute = null;

        for (int a = 0; a < atts.getLength(); a++) {
            String f = atts.getQName(a);
            if (f.equals(StandardNames.NAME)) {
                nameAttribute = Whitespace.trim(atts.getValue(a));
            } else {
                checkUnknownAttribute(atts.getNodeName(a));
            }
        }

        if (nameAttribute == null) {
            calledTemplateName = ERROR_TEMPLATE_NAME;
            reportAbsence("name");
            return;
        }

        try {
            calledTemplateName = makeQName(nameAttribute);
        } catch (NamespaceException err) {
            calledTemplateName = ERROR_TEMPLATE_NAME;
            compileError(err.getMessage(), "XTSE0280");
        } catch (XPathException err) {
            calledTemplateName = ERROR_TEMPLATE_NAME;
            compileError(err.getMessage(), err.getErrorCodeQName());
        }
    }

    public void validate(ComponentDeclaration decl) throws XPathException {
        AxisIterator kids = iterateAxis(AxisInfo.CHILD);
        while (true) {
            NodeInfo child = kids.next();
            if (child == null) {
                break;
            }
            if (child instanceof XSLWithParam) {
                // OK;
            } else if (child instanceof XSLFallback && mayContainFallback()) {
                // xsl:fallback is not allowed on xsl:call-template, but is allowed on saxon:call-template (cheat!)
            } else if (child.getNodeKind() == Type.TEXT) {
                // with xml:space=preserve, white space nodes may still be there
                if (!Whitespace.isWhite(child.getStringValueCS())) {
                    compileError("No character data is allowed within xsl:call-template", "XTSE0010");
                }
            } else {
                compileError("Child element " + Err.wrap(child.getDisplayName(), Err.ELEMENT) +
                        " is not allowed as a child of xsl:call-template", "XTSE0010");
            }
        }
        if (!calledTemplateName.equals(ERROR_TEMPLATE_NAME)) {
            template = findTemplate(calledTemplateName);
        }
    }

    public void postValidate() throws XPathException {
        // check that a parameter is supplied for each required parameter
        // of the called template

        if (template != null && template.getBody() != null) {
            checkParams();
        } else {
            final XSLCallTemplate call = this;
            // this is a forwards reference
            getContainingPackage().addCompletionAction(new StylesheetPackage.Action() {
                @Override
                public void doAction() throws XPathException {
                    call.checkParams();
                }
            });
        }
    }

    private void checkParams() throws XPathException {
        List<LocalParam> declaredParams = template.getLocalParams();
        for (LocalParam param : declaredParams) {
            if (param.isRequiredParam() && !param.isTunnelParam()) {
                AxisIterator actualParams = iterateAxis(AxisInfo.CHILD);
                boolean ok = false;
                while (true) {
                    NodeInfo withParam = actualParams.next();
                    if (withParam == null) {
                        break;
                    }
                    if (withParam instanceof XSLWithParam &&
                            ((XSLWithParam) withParam).getVariableQName().equals(param.getVariableQName())) {
                        ok = true;
                        break;
                    }
                }
                if (!ok) {
                    compileError("No value supplied for required parameter " +
                            Err.wrap(param.getVariableQName().getDisplayName(), Err.VARIABLE), "XTSE0690");
                }
            }
        }

        // check that every supplied parameter is declared in the called
        // template

        AxisIterator actualParams = iterateAxis(AxisInfo.CHILD);
        while (true) {
            NodeInfo w = actualParams.next();
            if (w == null) {
                break;
            }
            if (w instanceof XSLWithParam && !((XSLWithParam) w).isTunnelParam()) {
                XSLWithParam withParam = (XSLWithParam) w;
                boolean ok = false;
                for (LocalParam param : declaredParams) {
                    if (param.getVariableQName().equals(withParam.getVariableQName()) && !param.isTunnelParam()) {
                        // Note: see bug 10534
                        ok = true;
                        SequenceType required = param.getRequiredType();
                        withParam.checkAgainstRequiredType(required);
                        break;
                    }
                }
                if (!ok && !xPath10ModeIsEnabled()) {
                    compileError("Parameter " +
                            withParam.getVariableQName().getDisplayName() +
                            " is not declared in the called template", "XTSE0680");

                }
            }
        }
    }

    private Template findTemplate(StructuredQName templateName) throws XPathException {
        StylesheetPackage pack = getContainingPackage();
        Template template = pack.getNamedTemplate(templateName);
        if (template == null) {
            compileError("No template exists named " + calledTemplateName, "XTSE0650");
        }
        return template;
    }

    /**
     * Mark tail-recursive calls on templates and functions.
     * For most instructions, this does nothing.
     */

    public boolean markTailCalls() {
        useTailRecursion = true;
        return true;
    }


    public Expression compile(Compilation exec, ComponentDeclaration decl) throws XPathException {
        if (template == null) {
            return null;   // error already reported
        }

        CallTemplate call = new CallTemplate(
                template,
                useTailRecursion);
        call.setActualParameters(getWithParamInstructions(exec, decl, false),
                getWithParamInstructions(exec, decl, true));
        return call;
    }

}

