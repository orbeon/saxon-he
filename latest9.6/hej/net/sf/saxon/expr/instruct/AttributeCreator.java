////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
// Copyright (c) 2014 Saxonica Limited.
// This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
// If a copy of the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
// This Source Code Form is "Incompatible With Secondary Licenses", as defined by the Mozilla Public License, v. 2.0.
////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

package net.sf.saxon.expr.instruct;

import net.sf.saxon.Configuration;
import net.sf.saxon.Controller;
import net.sf.saxon.event.ReceiverOptions;
import net.sf.saxon.event.SequenceReceiver;
import net.sf.saxon.expr.StaticProperty;
import net.sf.saxon.expr.XPathContext;
import net.sf.saxon.lib.ConversionRules;
import net.sf.saxon.lib.Validation;
import net.sf.saxon.om.NodeName;
import net.sf.saxon.om.StandardNames;
import net.sf.saxon.pattern.NodeKindTest;
import net.sf.saxon.trans.Err;
import net.sf.saxon.trans.XPathException;
import net.sf.saxon.tree.util.Orphan;
import net.sf.saxon.type.*;
import net.sf.saxon.value.Whitespace;

/**
 * Abstract class for fixed and computed attribute constructor expressions
 */

public abstract class AttributeCreator extends SimpleNodeConstructor implements ValidatingInstruction {

    //Null implies untyped (no validation required)
    /*@Nullable*/ SimpleType schemaType = null;
    private int validationAction;
    private int options;

    /**
     * Set the required schema type of the attribute
     *
     * @param type the required schema type, if validation against a specific type is required,
     *             or null if no validation is required
     */

    public void setSchemaType(/*@Nullable*/ SimpleType type) {
        schemaType = type;
    }

    /**
     * Return the required schema type of the attribute
     *
     * @return if validation against a schema type was requested, return the schema type (always a simple type).
     *         Otherwise, if validation against a specific type was not requested, return null
     */

    /*@Nullable*/
    public SimpleType getSchemaType() {
        return schemaType;
    }

    /**
     * Set the validation action required
     *
     * @param action the validation action required, for example strict or lax
     */

    public void setValidationAction(int action) {
        validationAction = action;
    }

    /**
     * Get the validation action requested
     *
     * @return the validation action, for example strict or lax
     */

    public int getValidationAction() {
        return validationAction;
    }

    /**
     * Set the options to be used on the attribute event
     *
     * @param options Options to be used. The only option currently defined is
     *                {@link ReceiverOptions#REJECT_DUPLICATES}, which controls whether or not it is an error
     *                to create two attributes with the same name for the same element. (This is an error in XQuery
     *                but not in XSLT).
     */

    public void setOptions(int options) {
        this.options = options;
    }

    /**
     * Indicate that two attributes with the same name are not acceptable.
     * (This option is set in XQuery, but not in XSLT)
     */

    public void setRejectDuplicates() {
        options |= ReceiverOptions.REJECT_DUPLICATES;
    }

    /**
     * Indicate that the attribute value contains no special characters that
     * might need escaping
     */

    public void setNoSpecialChars() {
        options |= ReceiverOptions.NO_SPECIAL_CHARS;
    }

    /**
     * Get the options to be used on the attribute event
     *
     * @return the option flags to be used
     */

    public int getOptions() {
        return options;
    }


    /**
     * Get the static properties of this expression (other than its type). The result is
     * bit-signficant. These properties are used for optimizations. In general, if
     * property bit is set, it is true, but if it is unset, the value is unknown.
     *
     * @return a set of flags indicating static properties of this expression
     */
    @Override
    public int computeSpecialProperties() {
        int p = super.computeSpecialProperties();
        if (getValidationAction() == Validation.SKIP) {
            p |= StaticProperty.ALL_NODES_UNTYPED;
        }
        return p;
    }

    /**
     * Get the static type of this expression
     *
     * @return the static type of the item returned by this expression
     */

    /*@NotNull*/
    public ItemType getItemType() {
        return NodeKindTest.ATTRIBUTE;
    }

    /**
     * Process the value of the node, to create the new node.
     *
     * @param value   the string value of the new node
     * @param context the dynamic evaluation context
     * @throws XPathException
     */

    public void processValue(CharSequence value, XPathContext context) throws XPathException {
        NodeName attName = evaluateNodeName(context);
//        if (nameCode == -1) {
//            return null;
//        }
        SequenceReceiver out = context.getReceiver();
        int opt = getOptions();
        SimpleType ann;

        // we may need to change the namespace prefix if the one we chose is
        // already in use with a different namespace URI: this is done behind the scenes
        // by the ComplexContentOutputter

        //CharSequence value = expandChildren(context).toString();
        SimpleType schemaType = getSchemaType();
        int validationAction = getValidationAction();
        if (schemaType != null) {
            ann = schemaType;
            // test whether the value actually conforms to the given type
            try {
                ValidationFailure err = schemaType.validateContent(
                        value, DummyNamespaceResolver.getInstance(), context.getConfiguration().getConversionRules());
                if (err != null) {
                    ValidationException ve = new ValidationException(
                            "Attribute value " + Err.wrap(value, Err.VALUE) +
                                    " does not match the required type " +
                                    schemaType.getDescription() + ". " +
                                    err.getMessage());
                    ve.setErrorCode("XTTE1540");
                    throw ve;
                }
            } catch (UnresolvedReferenceException ure) {
                throw new ValidationException(ure);
            }
        } else if (validationAction == Validation.STRICT ||
                validationAction == Validation.LAX) {
            try {
                Configuration config = context.getConfiguration();
                ann = config.validateAttribute(attName.allocateNameCode(config.getNamePool()), value, validationAction);
            } catch (ValidationException e) {
                XPathException err = XPathException.makeXPathException(e);
                err.maybeSetErrorCode(validationAction == Validation.STRICT ? "XTTE1510" : "XTTE1515");
                err.setXPathContext(context);
                err.maybeSetLocation(this);
                err.setIsTypeError(true);
                throw err;
            }
        } else {
            ann = BuiltInAtomicType.UNTYPED_ATOMIC;
        }
        if (attName.equals(StandardNames.XML_ID_NAME)) {
            value = Whitespace.collapseWhitespace(value);
        }

        try {
            out.attribute(attName, ann, value, locationId, opt);
        } catch (XPathException err) {
            throw dynamicError(this, err, context);
        }

    }

    /**
     * Validate a newly-constructed parentless attribute using the type and validation attributes
     *
     * @param orphan  the new attribute node
     * @param context the dynamic evaluation context
     * @throws XPathException if validation fails
     */

    protected void validateOrphanAttribute(Orphan orphan, XPathContext context) throws XPathException {
        ConversionRules rules = context.getConfiguration().getConversionRules();
        SimpleType schemaType = getSchemaType();
        int validationAction = getValidationAction();
        if (schemaType != null) {
            ValidationFailure err = schemaType.validateContent(
                    orphan.getStringValueCS(), DummyNamespaceResolver.getInstance(), rules);
            if (err != null) {
                ValidationException ve = new ValidationException("Attribute value " + Err.wrap(orphan.getStringValueCS(), Err.VALUE) +
                        " does not the match the required type " +
                        schemaType.getDescription() + ". " +
                        err.getMessage());
                ve.setErrorCode("XTTE1555"); // TODO: different for XQuery
                ve.setLocator(this);
                throw ve;
            }
            orphan.setTypeAnnotation(schemaType.getFingerprint());
            if (schemaType.isNamespaceSensitive()) {
                throw new XPathException("Cannot validate a parentless attribute whose content is namespace-sensitive", "XTTE1545");
            }
        } else if (validationAction == Validation.STRICT || validationAction == Validation.LAX) {
            try {
                final Controller controller = context.getController();
                assert controller != null;
                SimpleType ann = controller.getConfiguration().validateAttribute(
                        orphan.getNameCode(), orphan.getStringValueCS(), validationAction);
                orphan.setTypeAnnotation(ann);
            } catch (ValidationException e) {
                XPathException err = XPathException.makeXPathException(e);
                err.setErrorCodeQName(e.getErrorCodeQName());
                err.setXPathContext(context);
                err.setLocator(this);
                err.setIsTypeError(true);
                throw err;
            }
        }
    }
}

