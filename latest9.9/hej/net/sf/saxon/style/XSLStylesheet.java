////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
// Copyright (c) 2018 Saxonica Limited.
// This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
// If a copy of the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
// This Source Code Form is "Incompatible With Secondary Licenses", as defined by the Mozilla Public License, v. 2.0.
////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

package net.sf.saxon.style;

import net.sf.saxon.lib.NamespaceConstant;
import net.sf.saxon.om.AttributeCollection;
import net.sf.saxon.om.AxisInfo;
import net.sf.saxon.om.NodeInfo;
import net.sf.saxon.trans.XPathException;
import net.sf.saxon.tree.iter.AxisIterator;
import net.sf.saxon.type.Type;

/**
 * An xsl:stylesheet or xsl:transform element in the stylesheet. <br>
 * Note this element represents a stylesheet module, not necessarily
 * the whole stylesheet. However, much of the functionality (and the fields)
 * are relevant only to the top-level module.
 */

public class XSLStylesheet extends XSLModuleRoot {


    protected boolean mayContainParam() {
        return true;
    }


    /**
     * Prepare the attributes on the stylesheet element
     */

    public void prepareAttributes() throws XPathException {

        processDefaultCollationAttribute();
        processDefaultMode();
        String inputTypeAnnotationsAtt = null;
        AttributeCollection atts = getAttributeList();
        for (int a = 0; a < atts.getLength(); a++) {

            String f = atts.getQName(a);
            switch (f) {
                case "version":
                    // already processed
                    break;
                case "id":
                    //
                    break;
                case "extension-element-prefixes":
                    //
                    break;
                case "exclude-result-prefixes":
                    //
                    break;
                case "input-type-annotations":
                    inputTypeAnnotationsAtt = atts.getValue(a);
                    break;
                default:
                    checkUnknownAttribute(atts.getNodeName(a));
                    break;
            }
        }
        if (version == -1 && (getParent() == null || getParent().getNodeKind() == Type.DOCUMENT)) {
            reportAbsence("version");
        }

        if (inputTypeAnnotationsAtt != null) {
            switch (inputTypeAnnotationsAtt) {
                case "strip":
                    //setInputTypeAnnotations(ANNOTATION_STRIP);
                    break;
                case "preserve":
                    //setInputTypeAnnotations(ANNOTATION_PRESERVE);
                    break;
                case "unspecified":
                    //
                    break;
                default:
                    invalidAttribute("input-type-annotations", "strip|preserve|unspecified");
                    break;
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
        if (getParent() != null && getParent().getNodeKind() != Type.DOCUMENT) {
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
                assert curr instanceof StyleElement;
                ((StyleElement) curr).compileError("Element " + curr.getDisplayName() +
                        " must not appear directly within " + getDisplayName(), "XTSE0010");
            } else {
                assert curr instanceof StyleElement;
                ((StyleElement) curr).compileError("Element " + curr.getDisplayName() +
                        " must not appear directly within " + getDisplayName() +
                        " because it is not in a namespace", "XTSE0130");
            }
        }
    }





}

