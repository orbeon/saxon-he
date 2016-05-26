////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
// Copyright (c) 2014 Saxonica Limited.
// This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
// If a copy of the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
// This Source Code Form is "Incompatible With Secondary Licenses", as defined by the Mozilla Public License, v. 2.0.
////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

package net.sf.saxon.style;

import net.sf.saxon.lib.NamespaceConstant;
import net.sf.saxon.lib.Validation;
import net.sf.saxon.om.*;
import net.sf.saxon.trans.XPathException;
import net.sf.saxon.tree.iter.AxisIterator;
import net.sf.saxon.type.Type;
import net.sf.saxon.value.Whitespace;

/**
 * An xsl:stylesheet or xsl:transform element in the stylesheet. <br>
 * Note this element represents a stylesheet module, not necessarily
 * the whole stylesheet. However, much of the functionality (and the fields)
 * are relevant only to the top-level module.
 */

public class XSLStylesheet extends XSLModuleRoot {


    protected boolean mayContainParam(String attName) {
        return true;
    }


    /**
     * Prepare the attributes on the stylesheet element
     */

    public void prepareAttributes() throws XPathException {
        processDefaultCollationAttribute();
        String inputTypeAnnotationsAtt = null;
        AttributeCollection atts = getAttributeList();
        for (int a = 0; a < atts.getLength(); a++) {

            String f = atts.getQName(a);
            if (f.equals(StandardNames.VERSION)) {
                // already processed
            } else if (f.equals(StandardNames.ID)) {
                //
            } else if (f.equals(StandardNames.EXTENSION_ELEMENT_PREFIXES)) {
                //
            } else if (f.equals(StandardNames.EXCLUDE_RESULT_PREFIXES)) {
                //
            } else if (f.equals(StandardNames.DEFAULT_VALIDATION)) {
                String val = Whitespace.trim(atts.getValue(a));
                defaultValidation = Validation.getCode(val);
                if (defaultValidation == Validation.INVALID ||
                    defaultValidation == Validation.STRICT || defaultValidation == Validation.LAX) {   // bug 2303
                    defaultValidation = Validation.STRIP;
                    compileError("Invalid value for default-validation attribute. " +
                            "Permitted values are (preserve, strip)", "XTSE0020");
                } else if (!isSchemaAware() && defaultValidation != Validation.STRIP) {
                    defaultValidation = Validation.STRIP;
                    compileError("default-validation='" + val + "' requires a schema-aware processor",
                            "XTSE1660");
                }
            } else if (f.equals(StandardNames.INPUT_TYPE_ANNOTATIONS)) {
                inputTypeAnnotationsAtt = atts.getValue(a);
            } else if (f.equals(StandardNames.DEFAULT_MODE)) {
                if (!isXslt30Processor()) {
                    compileError("The @default-mode attribute requires XSLT 3.0");
                }
                String val = Whitespace.trim(atts.getValue(a));
                if (!"#unnamed".equals(val)) {
                    try {
                        defaultMode = makeQName(atts.getValue(a));
                    } catch (NamespaceException err) {
                        throw new XPathException(err.getMessage(), "XTST0030");
                    }
                }
            } else {
                checkUnknownAttribute(atts.getNodeName(a));
            }
        }
        if (version == null && getParent().getNodeKind() == Type.DOCUMENT) {
            reportAbsence("version");
        }

        if (inputTypeAnnotationsAtt != null) {
            if (inputTypeAnnotationsAtt.equals("strip")) {
                //setInputTypeAnnotations(ANNOTATION_STRIP);
            } else if (inputTypeAnnotationsAtt.equals("preserve")) {
                //setInputTypeAnnotations(ANNOTATION_PRESERVE);
            } else if (inputTypeAnnotationsAtt.equals("unspecified")) {
                //
            } else {
                compileError("Invalid value for input-type-annotations attribute. " +
                        "Permitted values are (strip, preserve, unspecified)", "XTSE0020");
            }
        }

    }



    /**
     * Validate this element
     *
     * @param decl Not used
     */

    public void validate(ComponentDeclaration decl) throws XPathException {
        if (validationError != null) {
            compileError(validationError);
        }
        if (getParent().getNodeKind() != Type.DOCUMENT) {
            compileError(getDisplayName() + " must be the outermost element", "XTSE0010");
        }

        AxisIterator kids = iterateAxis(AxisInfo.CHILD);
        NodeInfo curr;
        while ((curr = kids.next()) != null) {
            if (curr.getNodeKind() == Type.TEXT ||
                    (curr instanceof StyleElement && ((StyleElement) curr).isDeclaration()) ||
                    curr instanceof DataElement) {
                // all is well
            } else if (!NamespaceConstant.XSLT.equals(curr.getURI()) && !"".equals(curr.getURI())) {
                // elements in other namespaces are allowed and ignored
            } else if (curr instanceof AbsentExtensionElement && ((StyleElement) curr).forwardsCompatibleModeIsEnabled()) {
                // this is OK: an unknown XSLT element is allowed in forwards compatibility mode
            } else if (NamespaceConstant.XSLT.equals(curr.getURI())) {
                ((StyleElement) curr).compileError("Element " + curr.getDisplayName() +
                        " must not appear directly within " + getDisplayName(), "XTSE0010");
            } else {
                ((StyleElement) curr).compileError("Element " + curr.getDisplayName() +
                        " must not appear directly within " + getDisplayName() +
                        " because it is not in a namespace", "XTSE0130");
            }
        }
    }





}

