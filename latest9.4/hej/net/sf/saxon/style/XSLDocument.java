package net.sf.saxon.style;

import net.sf.saxon.expr.Expression;
import net.sf.saxon.expr.Literal;
import net.sf.saxon.expr.instruct.DocumentInstr;
import net.sf.saxon.expr.instruct.Executable;
import net.sf.saxon.lib.Validation;
import net.sf.saxon.om.AttributeCollection;
import net.sf.saxon.om.Axis;
import net.sf.saxon.om.StandardNames;
import net.sf.saxon.trans.XPathException;
import net.sf.saxon.type.SchemaType;
import net.sf.saxon.value.Whitespace;

/**
* An xsl:document instruction in the stylesheet. <BR>
* This instruction creates a document node in the result tree, optionally
 * validating it.
*/

public class XSLDocument extends StyleElement {

    private int validationAction = Validation.STRIP;
    /*@Nullable*/ private SchemaType schemaType = null;

    /**
    * Determine whether this node is an instruction.
    * @return true - it is an instruction
    */

    public boolean isInstruction() {
        return true;
    }

    /**
    * Determine whether this type of element is allowed to contain a template-body
    * @return true: yes, it may contain a template-body
    */

    public boolean mayContainSequenceConstructor() {
        return true;
    }

    public void prepareAttributes() throws XPathException {
		AttributeCollection atts = getAttributeList();

        String validationAtt = null;
        String typeAtt = null;

		for (int a=0; a<atts.getLength(); a++) {
			String f = atts.getQName(a);
            if (f.equals(StandardNames.VALIDATION)) {
                validationAtt = Whitespace.trim(atts.getValue(a));
            } else if (f.equals(StandardNames.TYPE)) {
                typeAtt = Whitespace.trim(atts.getValue(a));
        	} else {
        		checkUnknownAttribute(atts.getNodeName(a));
        	}
        }

        if (validationAtt==null) {
            validationAction = getContainingStylesheet().getDefaultValidation();
        } else {
            validationAction = Validation.getCode(validationAtt);
            if (validationAction != Validation.STRIP && !getPreparedStylesheet().isSchemaAware()) {
                compileError("To perform validation, a schema-aware XSLT processor is needed", "XTSE1660");
            }
            if (validationAction == Validation.INVALID) {
                compileError("Invalid value of @validation attribute", "XTSE0020");
            }
        }
        if (typeAtt!=null) {
            if (!getPreparedStylesheet().isSchemaAware()) {
                compileError("The @type attribute is available only with a schema-aware XSLT processor", "XTSE1660");
            }
            schemaType = getSchemaType(typeAtt);
            validationAction = Validation.BY_TYPE;
        }

        if (typeAtt != null && validationAtt != null) {
            compileError("The @validation and @type attributes are mutually exclusive", "XTSE1505");
        }
    }

    public void validate(Declaration decl) throws XPathException {
        //
    }

    public Expression compile(Executable exec, Declaration decl) throws XPathException {

        DocumentInstr inst = new DocumentInstr(false, null, getBaseURI());
        inst.setValidationAction(validationAction, schemaType);
        Expression b = compileSequenceConstructor(exec, decl, iterateAxis(Axis.CHILD), true);
        if (b == null) {
            b = Literal.makeEmptySequence();
        }
        inst.setContentExpression(b);
        return inst;
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