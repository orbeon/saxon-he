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
import net.sf.saxon.value.DecimalValue;
import net.sf.saxon.value.NestedIntegerValue;
import net.sf.saxon.value.Whitespace;

import java.math.BigDecimal;

/**
 * Handler for xsl:package elements. Explicit xsl:package elements are not permitted in Saxon-HE, but
 * implicit packages are created, so the class is present in HE. The top-level module of a stylesheet/package
 * will always be represented by an XSLPackage object, but if the original name was xsl:stylesheet or xsl:transform
 * then this original name will be present as the name of the element.
 */
public class XSLPackage extends XSLModuleRoot {

    private String nameAtt = null;
    private PackageVersion packageVersion = null;

    public String getName() {
        if (nameAtt == null) {
            try {
                prepareAttributes();
            } catch (XPathException e) {
                nameAtt = "default";
            }
        }
        return nameAtt;
    }

    public DecimalValue getVersion() {
        if (version == null) {
            try {
                prepareAttributes();
            } catch (XPathException e) {
                version = DecimalValue.THREE;
            }
        }
        return version;
    }

    public PackageVersion getPackageVersion() {
        if (packageVersion == null) {
            try {
                prepareAttributes();
            } catch (XPathException e) {
                packageVersion = PackageVersion.ONE_ZERO;
            }
        }
        return packageVersion;
    }


    @Override
    protected void prepareAttributes() throws XPathException {
        String inputTypeAnnotationsAtt = null;
        AttributeCollection atts = getAttributeList();

        for (int a = 0; a < atts.getLength(); a++) {
            String f = atts.getQName(a);
            if (f.equals(StandardNames.NAME) && getLocalPart().equals("package")) {
                nameAtt = Whitespace.trim(atts.getValue(a));
            } else if (f.equals("id")) {
                // no action
            } else if (f.equals(StandardNames.VERSION)) {
                String in = Whitespace.trim(atts.getValue(a));
                version = new DecimalValue(new BigDecimal(in));

            } else if (f.equals(StandardNames.PACKAGE_VERSION) && getLocalPart().equals("package")) {
                String pversion = Whitespace.trim(atts.getValue(a));
                try {
                    packageVersion = new PackageVersion(pversion);
                } catch (XPathException ex) {
                    throw new XPathException("Error in xsl:package - The package-version attribute has incorrect character(s): " + pversion);
                }
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
            } else if (f.equals(StandardNames.DEFAULT_VALIDATION)) {
                String val = Whitespace.trim(atts.getValue(a));
                defaultValidation = Validation.getCode(val);
                if (defaultValidation == Validation.INVALID) {
                    compileError("Invalid value for default-validation attribute. " +
                            "Permitted values are (strict, lax, preserve, strip)", "XTSE0020");
                } else if (!isSchemaAware() && defaultValidation != Validation.STRIP) {
                    defaultValidation = Validation.STRIP;
                    compileError("default-validation='" + val + "' requires a schema-aware processor",
                            "XTSE1660");
                }
            } else if (f.equals(StandardNames.INPUT_TYPE_ANNOTATIONS)) {
                inputTypeAnnotationsAtt = atts.getValue(a);
            } else {
                checkUnknownAttribute(atts.getNodeName(a));
            }
        }
        if (nameAtt == null) {
            nameAtt = "default";
            if (getLocalPart().equals("package")) {
                reportAbsence("name");
            }
        }
        if (packageVersion == null) {
            packageVersion = PackageVersion.ONE_ZERO;
        }

        if (version == null) {
            version = DecimalValue.THREE;
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
     * Determine whether forwards-compatible mode is enabled for this element
     *
     * @return true if forwards-compatible mode is enabled
     */

    public boolean forwardsCompatibleModeIsEnabled() {
        return false;
    }

    /**
     * Recursive walk through the stylesheet to validate all nodes
     * @param decl not used
     * @throws XPathException if invalid
     */


    public void validate(ComponentDeclaration decl) throws XPathException {

        AxisIterator kids = iterateAxis(AxisInfo.CHILD);
        NodeInfo child;
        while ((child = kids.next()) != null) {
            int fp = child.getFingerprint();
            if (child.getNodeKind() == Type.TEXT ||
                    (child instanceof StyleElement && ((StyleElement) child).isDeclaration()) ||
                    child instanceof DataElement ) {
                // all is well
            } else if (getLocalPart().equals("package") &&
                    (fp == StandardNames.XSL_USE_PACKAGE || fp == StandardNames.XSL_EXPOSE)) {
                // all is well
            } else if (!NamespaceConstant.XSLT.equals(child.getURI()) && !"".equals(child.getURI())) {
                // elements in other namespaces are allowed and ignored
            } else if (child instanceof AbsentExtensionElement && ((StyleElement) child).forwardsCompatibleModeIsEnabled()) {
                // this is OK: an unknown XSLT element is allowed in forwards compatibility mode
            } else if (NamespaceConstant.XSLT.equals(child.getURI())) {
                ((StyleElement) child).compileError("Element " + child.getDisplayName() +
                        " must not appear directly within " + getDisplayName(), "XTSE0010");
            } else {
                ((StyleElement) child).compileError("Element " + child.getDisplayName() +
                        " must not appear directly within " + getDisplayName() +
                        " because it is not in a namespace", "XTSE0130");
            }
        }

    }



}
