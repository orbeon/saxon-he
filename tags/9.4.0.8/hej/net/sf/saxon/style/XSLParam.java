package net.sf.saxon.style;

import net.sf.saxon.expr.Expression;
import net.sf.saxon.expr.SuppliedParameterReference;
import net.sf.saxon.expr.instruct.*;
import net.sf.saxon.expr.parser.RoleLocator;
import net.sf.saxon.expr.parser.TypeChecker;
import net.sf.saxon.om.Axis;
import net.sf.saxon.om.NodeInfo;
import net.sf.saxon.trans.XPathException;
import net.sf.saxon.tree.iter.AxisIterator;
import net.sf.saxon.value.SequenceType;
import net.sf.saxon.value.Whitespace;

/**
* An xsl:param element in the stylesheet. <br>
* The xsl:param element has mandatory attribute name and optional attributes
 *  select, required, as, ...
*/

public class XSLParam extends XSLVariableDeclaration {

    /*@Nullable*/ Expression conversion = null;

    protected boolean allowsValue() {
        return !(getParent() instanceof XSLFunction);
        // function parameters cannot take a default value
    }

    protected boolean allowsRequired() {
        return ((StyleElement)getParent()).mayContainParam("required");
    }

    protected boolean allowsTunnelAttribute() {
        return true;
    }

    public void validate(Declaration decl) throws XPathException {

        NodeInfo parent = getParent();
        global = (parent instanceof XSLStylesheet);

        if (!((parent instanceof StyleElement) && ((StyleElement)parent).mayContainParam(null))) {
            compileError("xsl:param must be immediately within a template, function or stylesheet", "XTSE0010");
        }

        if (!global) {
            AxisIterator preceding = iterateAxis(Axis.PRECEDING_SIBLING);
            while (true) {
                NodeInfo node = (NodeInfo)preceding.next();
                if (node == null) {
                    break;
                }
                if (node instanceof XSLParam) {
                    if (this.getVariableQName().equals(((XSLParam)node).getVariableQName())) {
                        compileError("The name of the parameter is not unique", "XTSE0580");
                    }
                } else if (node instanceof StyleElement) {
                    compileError("xsl:param must not be preceded by other instructions", "XTSE0010");
                } else {
                    // it must be a text node; allow it if all whitespace
                    if (!Whitespace.isWhite(node.getStringValueCS())) {
                        compileError("xsl:param must not be preceded by text", "XTSE0010");
                    }
                }
            }

            SlotManager p = getContainingSlotManager();
            if (p==null) {
                compileError("Local variable must be declared within a template or function", "XTSE0010");
            } else {
                setSlotNumber(p.allocateSlotNumber(getVariableQName()));
            }

        }

        if (requiredParam) {
            if (select != null) {
                // NB, we do this test before setting the default select attribute
                compileError("The select attribute should be omitted when required='yes'", "XTSE0010");
            }
            if (hasChildNodes()) {
                compileError("A parameter specifying required='yes' must have empty content", "XTSE0010");
            }
        }

        super.validate(decl);
    }

    /**
    * Compile a global xsl:param element: this ensures space is available for local variables declared within
    * this global variable
    */

    public void compileDeclaration(Executable exec, Declaration decl) throws XPathException {
        if (!redundant) {
            int slot = getSlotNumber();
            if (requiredType != null) {
                SuppliedParameterReference pref = new SuppliedParameterReference(slot);
                pref.setLocationId(staticContext.getLocationMap().allocateLocationId(getSystemId(), getLineNumber()));
                RoleLocator role = new RoleLocator(RoleLocator.PARAM, getVariableDisplayName(), 0);
                //role.setSourceLocator(new ExpressionLocation(this));
                role.setErrorCode("XTTE0590");
                conversion = TypeChecker.staticTypeCheck(
                        pref,
                        requiredType,
                        false,
                        role, makeExpressionVisitor());
            }

            GeneralVariable binding;
            binding = new GlobalParam();
            ((GlobalParam)binding).setExecutable(getPreparedStylesheet());
            binding.setContainer(((GlobalParam)binding));
            if (isRequiredParam()) {
                getPreparedStylesheet().addRequiredParam(getVariableQName());
            }
            if (select != null) {
                select.setContainer(((GlobalVariable)binding));
            }
            compiledVariable = binding;

            initializeBinding(exec, decl, binding);
            binding.setVariableQName(getVariableQName());
            binding.setSlotNumber(slot);
            binding.setRequiredType(getRequiredType());
            fixupBinding(binding);
            compiledVariable = binding;
        }
    }


    public Expression compile(Executable exec, Declaration decl) throws XPathException {
        if (redundant) {
            return null;
        }
        if (getParent() instanceof XSLFunction) {
            // Do nothing. We did everything necessary while compiling the XSLFunction element.
            return null;
        } else {
            int slot = getSlotNumber();
            if (requiredType != null) {
                SuppliedParameterReference pref = new SuppliedParameterReference(slot);
                pref.setLocationId(staticContext.getLocationMap().allocateLocationId(getSystemId(), getLineNumber()));
                RoleLocator role = new RoleLocator(RoleLocator.PARAM, getVariableDisplayName(), 0);
                //role.setSourceLocator(new ExpressionLocation(this));
                role.setErrorCode("XTTE0590");
                conversion = TypeChecker.staticTypeCheck(
                        pref,
                        requiredType,
                        false,
                        role, makeExpressionVisitor());
            }

            GeneralVariable binding;
            Expression result;
            if (global) {
                throw new AssertionError("compiling global param as expression");
            } else {
                PrincipalStylesheetModule psm = getPrincipalStylesheetModule();
                binding = new LocalParam();
                ((LocalParam)binding).setConversion(conversion);
                ((LocalParam)binding).setParameterId(psm.allocateUniqueParameterNumber(getVariableQName()));
                result = new LocalParamSetter((LocalParam)binding);
            }
            initializeBinding(exec, decl, binding);
            binding.setVariableQName(getVariableQName());
            binding.setSlotNumber(slot);
            binding.setRequiredType(getRequiredType());
            fixupBinding(binding);
            compiledVariable = binding;
            return result;
        }
    }


    /**
    * Get the static type of the parameter. This is the declared type, because we cannot know
    * the actual value in advance.
    */

    public SequenceType getRequiredType() {
        if (requiredType!=null) {
            return requiredType;
        } else {
            return SequenceType.ANY_SEQUENCE;
        }
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