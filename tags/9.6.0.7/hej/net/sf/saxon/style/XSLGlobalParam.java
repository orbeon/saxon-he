////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
// Copyright (c) 2014 Saxonica Limited.
// This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
// If a copy of the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
// This Source Code Form is "Incompatible With Secondary Licenses", as defined by the Mozilla Public License, v. 2.0.
////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

package net.sf.saxon.style;

import net.sf.saxon.expr.Component;
import net.sf.saxon.expr.Expression;
import net.sf.saxon.expr.instruct.GlobalParam;
import net.sf.saxon.om.StructuredQName;
import net.sf.saxon.trans.SymbolicName;
import net.sf.saxon.trans.Visibility;
import net.sf.saxon.trans.XPathException;
import net.sf.saxon.value.SequenceType;

/**
 * An xsl:param element representing a global parameter (stylesheet parameter) in the stylesheet. <br>
 * The xsl:param element has mandatory attribute name and optional attributes
 * select, required, as, ...
 */

public class XSLGlobalParam extends XSLGlobalVariable {

    protected int getPermittedAttributes() {
        return
                SourceBinding.REQUIRED |
                SourceBinding.SELECT |
                SourceBinding.AS |
                SourceBinding.STATIC;
    }

    /*@Nullable*/ Expression conversion = null;

    public XSLGlobalParam() {
        sourceBinding.setProperty(SourceBinding.PARAM, true);
    }

    /**
     * Default visibility for xsl:param is public
     * @return the declared visibility, or "public" if not declared
     * @throws XPathException
     */

    public Visibility getVisibility() throws XPathException {
        String statik = getAttributeValue("static");
        if (statik == null) {
            return Visibility.PUBLIC;
        } else {
            try {
                boolean isStatic = processBooleanAttribute("static", statik);
                return isStatic ? Visibility.PRIVATE : Visibility.PUBLIC;
            } catch (XPathException err) {
                return Visibility.PUBLIC;
            }
        }
    }

    public void validate(ComponentDeclaration decl) throws XPathException {

        if (sourceBinding.hasProperty(SourceBinding.REQUIRED)) {
            if (sourceBinding.getSelectExpression() != null) {
                // NB, we do this test before setting the default select attribute
                compileError("The select attribute must be absent when required='yes'", "XTSE0010");
            }
            if (hasChildNodes()) {
                compileError("A parameter specifying required='yes' must have empty content", "XTSE0010");
            }
            Visibility vis = getVisibility();
            if (!sourceBinding.isStatic() &&
                    !(vis == Visibility.PUBLIC || vis == Visibility.FINAL || vis == Visibility.ABSTRACT)) {
                compileError("The visibility of a required non-static parameter must be public, final, or abstract", "XTSE3370");
            }
        }

        super.validate(decl);
    }


    /**
     * Compile a global xsl:param element: this ensures space is available for local variables declared within
     * this global variable
     */

    public void compileDeclaration(Compilation compilation, ComponentDeclaration decl) throws XPathException {
        if (sourceBinding.isStatic()) {
            super.compileDeclaration(compilation, decl);
        }
        if (!redundant) {
            StructuredQName name = sourceBinding.getVariableQName();
            //int slot = compilation.getGlobalVariableMap().allocateSlotNumber(name);
            // TODO: following removed temporarily for packaging - MHK 2013-07-11
//            if (sourceBinding.getDeclaredType() != null) {
//                SuppliedParameterReference pref = new SuppliedParameterReference(slot);
//                pref.setLocationId(staticContext.getLocationMap().allocateLocationId(getSystemId(), getLineNumber()));
//                RoleLocator role = new RoleLocator(RoleLocator.PARAM, name.getDisplayName(), 0);
//                role.setErrorCode("XTTE0590");
//                conversion = TypeChecker.staticTypeCheck(
//                        pref,
//                        sourceBinding.getDeclaredType(),
//                        false,
//                        role, makeExpressionVisitor());
//            }

            sourceBinding.handleSequenceConstructor(compilation, decl);

            GlobalParam binding = (GlobalParam)compiledVariable;
            binding.setPackageData(getCompilation().getPackageData());
            binding.makeDeclaringComponent(Visibility.PUBLIC, getContainingPackage());
            Expression select = sourceBinding.getSelectExpression();
            if (select != null) {
                select.setContainer(binding);
            }
            binding.setSelectExpression(select);
            binding.setVariableQName(sourceBinding.getVariableQName());
            initializeBinding(binding);

            binding.setRequiredType(getRequiredType());
            binding.setRequiredParam(sourceBinding.hasProperty(SourceBinding.REQUIRED));
            binding.setImplicitlyRequiredParam(sourceBinding.hasProperty(SourceBinding.IMPLICITLY_REQUIRED));
            sourceBinding.fixupBinding(binding);
            compiledVariable = binding;
        }
    }


    /**
     * Get the static type of the parameter. This is the declared type, because we cannot know
     * the actual value in advance.
     */

    public SequenceType getRequiredType() {
        SequenceType declaredType = sourceBinding.getDeclaredType();
        if (declaredType != null) {
            return declaredType;
        } else {
            return SequenceType.ANY_SEQUENCE;
        }
    }

    public SymbolicName getSymbolicName() {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public void checkCompatibility(Component component) throws XPathException {
        //To change body of implemented methods use File | Settings | File Templates.
    }
}

