package net.sf.saxon.instruct;
import net.sf.saxon.Configuration;
import net.sf.saxon.expr.*;
import net.sf.saxon.functions.SystemFunction;
import net.sf.saxon.om.*;
import net.sf.saxon.pattern.NodeKindTest;
import net.sf.saxon.trace.ExpressionPresenter;
import net.sf.saxon.trans.Err;
import net.sf.saxon.trans.XPathException;
import net.sf.saxon.type.*;

/**
 * An instruction derived from an xsl:attribute element in stylesheet, or from
 * an attribute constructor in XQuery. This version deals only with attributes
 * whose name is known at compile time. It is also used for attributes of
 * literal result elements. The value of the attribute is in general computed
 * at run-time.
*/

public final class FixedAttribute extends AttributeCreator {

    private int nameCode;

    /**
     * Construct an Attribute instruction
     * @param nameCode Represents the attribute name
     * @param validationAction the validation required, for example strict or lax
     * @param schemaType the schema type against which validation is required, null if not applicable
     * @param annotation Integer code identifying the type named in the <code>type</code> attribute
     * of the instruction - zero if the attribute was not present
    */

    public FixedAttribute (  int nameCode,
                             int validationAction,
                             SimpleType schemaType,
                             int annotation ) {
        this.nameCode = nameCode;
        setSchemaType(schemaType);
        if (annotation == -1) {
            setAnnotation(StandardNames.XS_UNTYPED_ATOMIC);
        } else {
            setAnnotation(annotation);
        }
        setValidationAction(validationAction);
        setOptions(0);
    }

    /**
     * Get the name of this instruction (return 'xsl:attribute')
     */

    public int getInstructionNameCode() {
        return StandardNames.XSL_ATTRIBUTE;
    }

    /**
     * Set the expression defining the value of the attribute. If this is a constant, and if
     * validation against a schema type was requested, the validation is done immediately.
     * @param select The expression defining the content of the attribute
     * @param config The Saxon configuration
     * @throws XPathException if the expression is a constant, and validation is requested, and
     * the constant doesn't match the required type.
     */
    public void setSelect(Expression select, Configuration config) throws XPathException {
        super.setSelect(select, config);
        // If attribute name is xml:id, add whitespace normalization
        if ((nameCode & NamePool.FP_MASK) == StandardNames.XML_ID) {
            select = SystemFunction.makeSystemFunction("normalize-space", new Expression[]{select});
            super.setSelect(select, config);
        }
    }

    public void localTypeCheck(ExpressionVisitor visitor, ItemType contextItemType) throws XPathException {
        Configuration config = visitor.getConfiguration();
        SimpleType schemaType = getSchemaType();
        if (schemaType == null) {
            int validation = getValidationAction();
            if (validation == Validation.STRICT) {
                SchemaDeclaration decl = config.getAttributeDeclaration(nameCode & NamePool.FP_MASK);
                if (decl == null) {
                    XPathException se = new XPathException(
                            "Strict validation fails: there is no global attribute declaration for " +
                                    config.getNamePool().getDisplayName(nameCode));
                    se.setErrorCode("XTTE1510");
                    se.setLocator(this);
                    throw se;
                }
                schemaType = (SimpleType)decl.getType();
            } else if (validation == Validation.LAX) {
                SchemaDeclaration decl = config.getAttributeDeclaration(nameCode & NamePool.FP_MASK);
                if (decl != null) {
                    schemaType = (SimpleType)decl.getType();
                } else {
                    visitor.getStaticContext().issueWarning(
                            "Lax validation has no effect: there is no global attribute declaration for " +
                            config.getNamePool().getDisplayName(nameCode), this);
                }
            }
        }

        // Attempt early validation if possible
        if (Literal.isAtomic(select) && schemaType != null && !schemaType.isNamespaceSensitive()) {
            CharSequence value = ((Literal)select).getValue().getStringValueCS();
            ValidationFailure err = schemaType.validateContent(
                    value, DummyNamespaceResolver.getInstance(), config.getNameChecker());
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
        return nameCode;
    }

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

    public Expression copy() {
        FixedAttribute exp = new FixedAttribute(nameCode, getValidationAction(), getSchemaType(), getAnnotation());
        try {
            exp.setSelect(select.copy(), getExecutable().getConfiguration());
        } catch (XPathException err) {
            throw new UnsupportedOperationException(err.getMessage());
        }
        return exp;
    }

    public int evaluateNameCode(XPathContext context)  {
        return nameCode;
    }

    /**
     * Check that any elements and attributes constructed or returned by this expression are acceptable
     * in the content model of a given complex type. It's always OK to say yes, since the check will be
     * repeated at run-time. The process of checking element and attribute constructors against the content
     * model of a complex type also registers the type of content expected of those constructors, so the
     * static validation can continue recursively.
     */

    public void checkPermittedContents(SchemaType parentType, StaticContext env, boolean whole) throws XPathException {
        int fp = nameCode & NamePool.FP_MASK;
        if (fp == StandardNames.XSI_TYPE ||
                fp == StandardNames.XSI_SCHEMA_LOCATION ||
                fp == StandardNames.XSI_NIL ||
                fp == StandardNames.XSI_NO_NAMESPACE_SCHEMA_LOCATION) {
            return;
        }
        if (parentType instanceof SimpleType) {
            XPathException err = new XPathException("Attribute " + env.getNamePool().getDisplayName(nameCode) +
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
            XPathException err = new XPathException("Attribute " + env.getNamePool().getDisplayName(nameCode) +
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
        if (type instanceof AnyType) {
            return;
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


    public Item evaluateItem(XPathContext context) throws XPathException {
        Orphan o = (Orphan)super.evaluateItem(context);
        SimpleType schemaType = getSchemaType();
        int validationAction = getValidationAction();
        if (schemaType != null) {
            ValidationFailure err = schemaType.validateContent(
                    o.getStringValueCS(), DummyNamespaceResolver.getInstance(), context.getConfiguration().getNameChecker());
            if (err != null) {
                throw new ValidationException("Attribute value " + Err.wrap(o.getStringValueCS(), Err.VALUE) +
                                           " does not the match the required type " +
                                           schemaType.getDescription() + ". " +
                                           err.getMessage());
            }
            o.setTypeAnnotation(schemaType.getFingerprint());
            if (schemaType.isNamespaceSensitive()) {
                throw new XPathException("Cannot validate a parentless attribute whose content is namespace-sensitive");
            }
        } else if (validationAction==Validation.STRICT || validationAction==Validation.LAX) {
            try {
                int ann = context.getController().getConfiguration().validateAttribute(
                        nameCode, o.getStringValueCS(), validationAction);
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
        out.emitAttribute("name", out.getNamePool().getDisplayName(nameCode));
        out.emitAttribute("validation", Validation.toString(getValidationAction()));
        if (getSchemaType() != null) {
            out.emitAttribute("type", getSchemaType().getDescription());
        }
        getSelect().explain(out);
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
// The Original Code is: all this file.
//
// The Initial Developer of the Original Code is Michael H. Kay.
//
// Portions created by (your name) are Copyright (C) (your legal entity). All Rights Reserved.
//
// Contributor(s): none.
//
