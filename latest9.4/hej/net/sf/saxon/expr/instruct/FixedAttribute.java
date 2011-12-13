package net.sf.saxon.expr.instruct;

import net.sf.saxon.Configuration;
import net.sf.saxon.Controller;
import net.sf.saxon.expr.*;
import net.sf.saxon.expr.parser.ExpressionVisitor;
import net.sf.saxon.functions.SystemFunction;
import net.sf.saxon.lib.ConversionRules;
import net.sf.saxon.lib.Validation;
import net.sf.saxon.om.NodeInfo;
import net.sf.saxon.om.NodeName;
import net.sf.saxon.om.StandardNames;
import net.sf.saxon.pattern.NodeKindTest;
import net.sf.saxon.trace.ExpressionPresenter;
import net.sf.saxon.trans.Err;
import net.sf.saxon.trans.XPathException;
import net.sf.saxon.tree.util.Orphan;
import net.sf.saxon.type.*;

/**
 * An instruction derived from an xsl:attribute element in stylesheet, or from
 * an attribute constructor in XQuery. This version deals only with attributes
 * whose name is known at compile time. It is also used for attributes of
 * literal result elements. The value of the attribute is in general computed
 * at run-time.
*/

public final class FixedAttribute extends AttributeCreator {

    private NodeName nodeName;

    /**
     * Construct an Attribute instruction
     * @param nodeName Represents the attribute name
     * @param validationAction the validation required, for example strict or lax
     * @param schemaType the schema type against which validation is required, null if not applicable
     * of the instruction - zero if the attribute was not present
    */

    public FixedAttribute (  NodeName nodeName,
                             int validationAction,
                             SimpleType schemaType) {
        this.nodeName = nodeName;
        setSchemaType(schemaType);
        setValidationAction(validationAction);
        setOptions(0);
    }

    /**
     * Get the name of this instruction (return 'xsl:attribute')
     */

    public int getInstructionNameCode() {
        return StandardNames.XSL_ATTRIBUTE;
    }

    public NodeName getAttributeName() {
        return nodeName;
    }

    /**
     * Set the expression defining the value of the attribute. If this is a constant, and if
     * validation against a schema type was requested, the validation is done immediately.
     * @param select The expression defining the content of the attribute
     * @param config The Saxon configuration
     */
    public void setSelect(Expression select, Configuration config)  {
        super.setSelect(select, config);
        // If attribute name is xml:id, add whitespace normalization
        if (nodeName.equals(StandardNames.XML_ID_NAME)) {
            select = SystemFunction.makeSystemFunction("normalize-space", new Expression[]{select});
            super.setSelect(select, config);
        }
    }

    public void localTypeCheck(ExpressionVisitor visitor, ExpressionVisitor.ContextItemType contextItemType) throws XPathException {
        Configuration config = visitor.getConfiguration();
        final ConversionRules rules = config.getConversionRules();
        SimpleType schemaType = getSchemaType();
        if (schemaType == null) {
            int validation = getValidationAction();
            if (validation == Validation.STRICT) {
                SchemaDeclaration decl = config.getAttributeDeclaration(nodeName.getFingerprint());
                if (decl == null) {
                    XPathException se = new XPathException(
                            "Strict validation fails: there is no global attribute declaration for " +
                                    nodeName.getDisplayName());
                    se.setErrorCode("XTTE1510");
                    se.setLocator(this);
                    throw se;
                }
                schemaType = (SimpleType)decl.getType();
            } else if (validation == Validation.LAX) {
                SchemaDeclaration decl = config.getAttributeDeclaration(nodeName.getFingerprint());
                if (decl != null) {
                    schemaType = (SimpleType)decl.getType();
                } else {
                    visitor.getStaticContext().issueWarning(
                            "Lax validation has no effect: there is no global attribute declaration for " +
                            nodeName.getDisplayName(), this);
                }
            }
        }

        // Attempt early validation if possible
        if (Literal.isAtomic(select) && schemaType != null && !schemaType.isNamespaceSensitive()) {
            CharSequence value = ((Literal)select).getValue().getStringValueCS();
            ValidationFailure err = schemaType.validateContent(
                    value, DummyNamespaceResolver.getInstance(), rules);
            if (err != null) {
                XPathException se = new XPathException("Attribute value " + Err.wrap(value, Err.VALUE) +
                        " does not the match the required type " +
                        schemaType.getDescription() + ". " +
                        err.getMessage());
                se.setErrorCode("XTTE1540");
                throw se;
            }
        }

        // If value is fixed, test whether there are any special characters that might need to be
        // escaped when the time comes for serialization
        if (select instanceof StringLiteral) {
            boolean special = false;
            CharSequence val = ((StringLiteral)select).getStringValue();
            for (int k=0; k<val.length(); k++) {
                char c = val.charAt(k);
                if ((int)c<33 || (int)c>126 ||
                         c=='<' || c=='>' || c=='&' || c=='\"') {
                    special = true;
                    break;
                 }
            }
            if (!special) {
                setNoSpecialChars();
            }
        }
    }

    /**
     * Get the name pool name code of the attribute to be constructed
     * @return the attribute's name code
     */

    public int getAttributeNameCode() {
        return nodeName.getNameCode();
    }

    /*@NotNull*/
    public ItemType getItemType(TypeHierarchy th) {
        return NodeKindTest.ATTRIBUTE;
    }

    public int getCardinality() {
        return StaticProperty.EXACTLY_ONE;
    }

    /**
     * Copy an expression. This makes a deep copy.
     *
     * @return the copy of the original expression
     */

    /*@NotNull*/
    public Expression copy() {
        FixedAttribute exp = new FixedAttribute(nodeName, getValidationAction(), getSchemaType());
        exp.setSelect(select.copy(), getExecutable().getConfiguration());
        return exp;
    }

    public NodeName evaluateNodeName(XPathContext context)  {
        return nodeName;
    }

    /**
     * Check that any elements and attributes constructed or returned by this expression are acceptable
     * in the content model of a given complex type. It's always OK to say yes, since the check will be
     * repeated at run-time. The process of checking element and attribute constructors against the content
     * model of a complex type also registers the type of content expected of those constructors, so the
     * static validation can continue recursively.
     */

    public void checkPermittedContents(SchemaType parentType, StaticContext env, boolean whole) throws XPathException {
        int fp = nodeName.getFingerprint();
        if (fp == StandardNames.XSI_TYPE ||
                fp == StandardNames.XSI_SCHEMA_LOCATION ||
                fp == StandardNames.XSI_NIL ||
                fp == StandardNames.XSI_NO_NAMESPACE_SCHEMA_LOCATION) {
            return;
        }
        if (parentType instanceof SimpleType) {
            XPathException err = new XPathException("Attribute " + nodeName.getDisplayName() +
                    " is not permitted in the content model of the simple type " + parentType.getDescription());
            err.setIsTypeError(true);
            err.setLocator(this);
            if (getHostLanguage() == Configuration.XSLT) {
                err.setErrorCode("XTTE1510");
            } else {
                err.setErrorCode("XQDY0027");
            }
            throw err;
        }
        SchemaType type;
        try {
            type = ((ComplexType)parentType).getAttributeUseType(fp);
        } catch (SchemaException e) {
            throw new XPathException(e);
        }
        if (type == null) {
            XPathException err = new XPathException("Attribute " + nodeName.getDisplayName() +
                    " is not permitted in the content model of the complex type " + parentType.getDescription());
            err.setIsTypeError(true);
            err.setLocator(this);
            if (getHostLanguage() == Configuration.XSLT) {
                err.setErrorCode("XTTE1510");
            } else {
                err.setErrorCode("XQDY0027");
            }
            throw err;
        }

        try {
            // When select is a SimpleContentConstructor, this does nothing
            select.checkPermittedContents(type, env, true);
        } catch (XPathException e) {
            if (e.getLocator() == null || e.getLocator() == e) {
                e.setLocator(this);
            }
            throw e;
        }
    }


    public NodeInfo evaluateItem(XPathContext context) throws XPathException {
        final ConversionRules rules = context.getConfiguration().getConversionRules();
        Orphan o = (Orphan)super.evaluateItem(context);
        assert o != null;
        SimpleType schemaType = getSchemaType();
        int validationAction = getValidationAction();
        if (schemaType != null) {
            ValidationFailure err = schemaType.validateContent(
                    o.getStringValueCS(), DummyNamespaceResolver.getInstance(), rules);
            if (err != null) {
                throw new ValidationException("Attribute value " + Err.wrap(o.getStringValueCS(), Err.VALUE) +
                                           " does not the match the required type " +
                                           schemaType.getDescription() + ". " +
                                           err.getMessage());
            }
            o.setTypeAnnotation(schemaType.getFingerprint());
            if (schemaType.isNamespaceSensitive()) {
                throw new XPathException("Cannot validate a parentless attribute whose content is namespace-sensitive", "XTTE1545");
            }
        } else if (validationAction==Validation.STRICT || validationAction==Validation.LAX) {
            try {
                final Controller controller = context.getController();
                assert controller != null;
                SimpleType ann = controller.getConfiguration().validateAttribute(
                        nodeName.getNameCode(), o.getStringValueCS(), validationAction);
                o.setTypeAnnotation(ann);
            } catch (ValidationException e) {
                XPathException err = XPathException.makeXPathException(e);
                err.setErrorCodeQName(e.getErrorCodeQName());
                err.setXPathContext(context);
                err.setLocator(this);
                err.setIsTypeError(true);
                throw err;
            }
        }

        return o;
    }

    /**
     * Diagnostic print of expression structure. The abstract expression tree
     * is written to the supplied output destination.
     */

    public void explain(ExpressionPresenter out) {
        out.startElement("directAttribute");
        out.emitAttribute("name", nodeName.getDisplayName());
        out.emitAttribute("validation", Validation.toString(getValidationAction()));
        if (getSchemaType() != null) {
            out.emitAttribute("type", getSchemaType().getDescription());
        }
        getContentExpression().explain(out);
        out.endElement();
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