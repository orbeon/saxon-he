////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
// Copyright (c) 2013 Saxonica Limited.
// This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
// If a copy of the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
// This Source Code Form is "Incompatible With Secondary Licenses", as defined by the Mozilla Public License, v. 2.0.
////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

package net.sf.saxon.type;

import com.saxonica.schema.UserSimpleType;
import net.sf.saxon.expr.*;
import net.sf.saxon.lib.ConversionRules;
import net.sf.saxon.lib.NamespaceConstant;
import net.sf.saxon.om.*;
import net.sf.saxon.trans.XPathException;
import net.sf.saxon.tree.iter.UnfailingIterator;
import net.sf.saxon.value.AtomicValue;
import net.sf.saxon.value.StringValue;
import net.sf.saxon.value.Whitespace;

import java.io.Serializable;

/**
 * <p>This class is used to implement the built-in
 * list types NMTOKENS, ENTITIES, IDREFS. It is also used to represent the anonymous type of the
 * xsi:schemaLocation attribute (a list of xs:anyURI values).</p>
 */

public class BuiltInListType implements ListType, Serializable {

    private int fingerprint;

    /*@NotNull*/ public static BuiltInListType ENTITIES = makeListType(NamespaceConstant.SCHEMA, "ENTITIES");
    /*@NotNull*/ public static BuiltInListType IDREFS = makeListType(NamespaceConstant.SCHEMA, "IDREFS");
    /*@NotNull*/ public static BuiltInListType NMTOKENS = makeListType(NamespaceConstant.SCHEMA, "NMTOKENS");
    /*@NotNull*/ public static BuiltInListType ANY_URIS = makeListType(NamespaceConstant.SCHEMA_INSTANCE, "anonymous_schemaLocationType");

    /**
     * Return true if this is an external object type, that is, a Saxon-defined type for external
     * Java or .NET objects
     */

    public boolean isExternalType() {
        return false;
    }

    /**
     * Determine whether this is a built-in type or a user-defined type
     */

    public boolean isBuiltInType() {
        return true;
    }


    /**
     * Get the URI of the schema document containing the definition of this type
     *
     * @return null for a built-in type
     */

    /*@Nullable*/
    public String getSystemId() {
        return null;
    }

    /**
     * Get the redefinition level. This is zero for a component that has not been redefined;
     * for a redefinition of a level-0 component, it is 1; for a redefinition of a level-N
     * component, it is N+1. This concept is used to support the notion of "pervasive" redefinition:
     * if a component is redefined at several levels, the top level wins, but it is an error to have
     * two versions of the component at the same redefinition level.
     *
     * @return the redefinition level
     */

    public int getRedefinitionLevel() {
        return 0;
    }

    /**
     * Determine how values of this simple type are whitespace-normalized.
     *
     * @return one of {@link net.sf.saxon.value.Whitespace#PRESERVE}, {@link net.sf.saxon.value.Whitespace#COLLAPSE},
     *         {@link net.sf.saxon.value.Whitespace#REPLACE}.
     */

    public int getWhitespaceAction() {
        return Whitespace.COLLAPSE;
    }

    /**
     * The SimpleType of the items in the list.
     */

    /*@NotNull*/
    private BuiltInAtomicType itemType;

    /**
     * Create a new ListType.
     *
     * @param fingerprint identifies the name of the type
     */

    public BuiltInListType(int fingerprint) {
        this.fingerprint = fingerprint;
        switch (fingerprint) {
            case StandardNames.XS_ENTITIES:
                itemType = BuiltInAtomicType.ENTITY;
                break;
            case StandardNames.XS_IDREFS:
                itemType = BuiltInAtomicType.IDREF;
                break;
            case StandardNames.XS_NMTOKENS:
                itemType = BuiltInAtomicType.NMTOKEN;
                break;
            case StandardNames.XSI_SCHEMA_LOCATION_TYPE:
                itemType = BuiltInAtomicType.ANY_URI;
                break;
        }
    }

    /**
     * Get the validation status - always valid
     */
    public int getValidationStatus() {
        return VALIDATED;
    }

    /**
     * Returns the base type that this type inherits from.
     * If this type is a Simpletype that is a built in primitive type then null is returned.
     *
     * @return the base type.
     */

    /*@NotNull*/
    public SchemaType getBaseType() {
        return AnySimpleType.getInstance();
    }

    /**
     * Test whether this Simple Type is an atomic type
     *
     * @return false, this is not an atomic type
     */

    public boolean isAtomicType() {
        return false;
    }

    /**
     * Ask whether this type is an ID type. This is defined to be any simple type
     * who typed value may contain atomic values of type xs:ID: that is, it includes types derived
     * from ID by restriction, list, or union. Note that for a node to be treated
     * as an ID, its typed value must be a *single* atomic value of type ID; the type of the
     * node, however, can still allow a list.
     */

    public boolean isIdType() {
        return false;
    }

    /**
     * Ask whether this type is an IDREF or IDREFS type. This is defined to be any simple type
     * who typed value may contain atomic values of type xs:IDREF: that is, it includes types derived
     * from IDREF or IDREFS by restriction, list, or union
     */

    public boolean isIdRefType() {
        return fingerprint == StandardNames.XS_IDREFS;
    }

    /**
     * Returns true if this type is derived by list, or if it is derived by restriction
     * from a list type, or if it is a union that contains a list as one of its members
     */

    public boolean isListType() {
        return true;
    }

    public boolean isUnionType() {
        return false;
    }

    /**
     * Test whether this is an anonymous type
     *
     * @return true if this SchemaType is an anonymous type
     */

    public boolean isAnonymousType() {
        return false;
    }

    /*@NotNull*/
    public SchemaType getBuiltInBaseType() {
        return this;
    }

    public boolean isNamespaceSensitive() {
        return false;
    }

    /**
     * Get the local name of this type
     *
     * @return the local name of this type definition, if it has one. Return null in the case of an
     *         anonymous type.
     */

    public String getName() {
        return StandardNames.getLocalName(fingerprint);
    }

    /**
     * Get the target namespace of this type
     *
     * @return the target namespace of this type definition, if it has one. Return null in the case
     *         of an anonymous type, and in the case of a global type defined in a no-namespace schema.
     */

    public String getTargetNamespace() {
        return NamespaceConstant.SCHEMA;
    }

    /**
     * Get the name of this type as an EQName, that is, a string in the format Q{uri}local.
     *
     * @return an EQName identifying the type.
     */
    public String getEQName() {
        return "Q{" + NamespaceConstant.SCHEMA + "}" + getName();
    }

    /**
     * Get the fingerprint of the name of this type
     *
     * @return the fingerprint. Returns an invented fingerprint for an anonymous type.
     */

    public int getFingerprint() {
        return fingerprint;
    }

    /**
     * Get the namecode of the name of this type. Because built-in types don't depend on the namePool,
     * this actually returns the fingerprint, which contains no information about the namespace prefix
     */

    public int getNameCode() {
        return fingerprint;
    }

    /**
     * Get the display name of the type: that is, a lexical QName with an arbitrary prefix
     *
     * @return a lexical QName identifying the type
     */

    public String getDisplayName() {
        return StandardNames.getDisplayName(fingerprint);
    }

    /**
     * Test whether this SchemaType is a complex type
     *
     * @return true if this SchemaType is a complex type
     */

    public boolean isComplexType() {
        return false;
    }

    /**
     * Test whether this SchemaType is a simple type
     *
     * @return true if this SchemaType is a simple type
     */

    public boolean isSimpleType() {
        return true;
    }

    /**
     * Returns the value of the 'block' attribute for this type, as a bit-signnificant
     * integer with fields such as {@link SchemaType#DERIVATION_LIST} and {@link SchemaType#DERIVATION_EXTENSION}
     *
     * @return the value of the 'block' attribute for this type
     */

    public int getBlock() {
        return 0;
    }

    /**
     * Returns the base type that this type inherits from. This method can be used to get the
     * base type of a type that is known to be valid.
     * If this type is a Simpletype that is a built in primitive type then null is returned.
     *
     * @return the base type.
     * @throws IllegalStateException if this type is not valid.
     */

    /*@NotNull*/
    public SchemaType getKnownBaseType() throws IllegalStateException {
        return AnySimpleType.getInstance();
    }

    /**
     * Gets the integer code of the derivation method used to derive this type from its
     * parent. Returns zero for primitive types.
     *
     * @return a numeric code representing the derivation method, for example {@link SchemaType#DERIVATION_RESTRICTION}
     */

    public int getDerivationMethod() {
        return SchemaType.DERIVATION_LIST;
    }

    /**
     * Determines whether derivation (of a particular kind)
     * from this type is allowed, based on the "final" property
     *
     * @param derivation the kind of derivation, for example {@link SchemaType#DERIVATION_LIST}
     * @return true if this kind of derivation is allowed
     */

    public boolean allowsDerivation(int derivation) {
        return true;
    }

    /**
     * Get the types of derivation that are not permitted, by virtue of the "final" property.
     *
     * @return the types of derivation that are not permitted, as a bit-significant integer
     *         containing bits such as {@link net.sf.saxon.type.SchemaType#DERIVATION_EXTENSION}
     */
    public int getFinalProhibitions() {
        return 0;
    }


//#ifdefined SCHEMA

    /**
     * Get the schema component in the form of a function item. This allows schema information
     * to be made visible to XSLT or XQuery code. The function makes available the contents of the
     * schema component as defined in the XSD specification. The function takes a string as argument
     * representing a property name, and returns the corresponding property of the schema component.
     * There is also a property "class" which returns the kind of schema component, for example
     * "Attribute Declaration".
     *
     * @return the schema component represented as a function from property names to property values.
     */
    public FunctionItem getComponentAsFunction() {
        return UserSimpleType.getComponentAsFunction(this);
    }
//#endif

    /**
     * Get the typed value of a node that is annotated with this schema type. The result of this method will always be consistent with the method
     *
     * @param node the node whose typed value is required
     * @return the typed value.
     * @since 8.5
     */

    public AtomicSequence atomize(/*@NotNull*/ NodeInfo node) throws XPathException {
        try {
            return getTypedValue(node.getStringValue(),
                    new InscopeNamespaceResolver(node),
                    node.getConfiguration().getConversionRules());
        } catch (ValidationException err) {
            throw new XPathException("Internal error: value doesn't match its type annotation. " + err.getMessage());
        }
    }

    /**
     * Test whether this is the same type as another type. They are considered to be the same type
     * if they are derived from the same type definition in the original XML representation (which
     * can happen when there are multiple includes of the same file)
     */

    public boolean isSameType(/*@NotNull*/ SchemaType other) {
        return other.getFingerprint() == getFingerprint();
    }

    public String getDescription() {
        return getDisplayName();
    }

    /**
     * Check that this type is validly derived from a given type
     *
     * @param type  the type from which this type is derived
     * @param block the derivations that are blocked by the relevant element declaration
     * @throws SchemaException if the derivation is not allowed
     */

    public void checkTypeDerivationIsOK(SchemaType type, int block) throws SchemaException {
        //
    }

    /**
     * Get the local name of this type
     *
     * @return the local part of the name, or null if the type is anonymous
     */

    public String getLocalName() {
        return getDisplayName().substring(3);
    }

    /**
     * Returns the simpleType of the items in this ListType.
     *
     * @return the simpleType of the items in this ListType.
     */

    /*@NotNull*/
    public SimpleType getItemType() {
        return itemType;
    }

    /**
     * Apply the whitespace normalization rules for this simple type
     *
     * @param value the string before whitespace normalization
     * @return the string after whitespace normalization
     */

    public String applyWhitespaceNormalization(String value) {
        return Whitespace.collapseWhitespace(value).toString();
    }

    /**
     * Analyze an expression to see whether the expression is capable of delivering a value of this
     * type.
     *
     * @param expression the expression that delivers the content
     * @param kind       the node kind whose content is being delivered: {@link Type#ELEMENT},
     *                   {@link Type#ATTRIBUTE}, or {@link Type#DOCUMENT}
     * @param env        the XPath static context
     * @throws net.sf.saxon.trans.XPathException
     *          if the expression will never deliver a value of the correct type
     */

    public void analyzeContentExpression(/*@NotNull*/ Expression expression, int kind, StaticContext env) throws XPathException {
        BuiltInAtomicType.analyzeContentExpression(this, expression, env, kind);
    }

    /**
     * Check whether a given input string is valid according to this SimpleType
     *
     * @param value      the input string to be checked
     * @param nsResolver a namespace resolver used to resolve namespace prefixes if the type
     *                   is namespace sensitive. The value supplied may be null; in this case any namespace-sensitive
     *                   content will throw an UnsupportedOperationException.
     * @param rules      the conversion rules for this configuration
     * @return either null to indicate that validation succeeded, or a ValidationFailure object giving information
     *         about why it failed
     * @throws UnsupportedOperationException if the type is namespace-sensitive and no namespace
     *                                       resolver is supplied
     */

    /*@Nullable*/
    public ValidationFailure validateContent(
            /*@NotNull*/ CharSequence value, /*@Nullable*/ NamespaceResolver nsResolver, /*@NotNull*/ ConversionRules rules) {
        SimpleType base = getItemType();
        StringTokenIterator iter = new StringTokenIterator(value.toString());
        int count = 0;
        while (true) {
            StringValue val = iter.next();
            if (val == null) {
                break;
            }
            count++;
            ValidationFailure v = base.validateContent(val.getStringValue(), nsResolver, rules);
            if (v != null) {
                return v;
            }
        }
        if (count == 0) {
            return new ValidationFailure("The built-in list type " +
                    StandardNames.getDisplayName(fingerprint) +
                    " does not allow a zero-length list");
        }
        return null;
    }

    /**
     * Get the typed value of a given input string. This method assumes that the input value
     * is valid according to this SimpleType
     *
     * @param value    the string whose typed value is required
     * @param resolver namespace resolver for namespace-sensitive content
     * @param rules
     */

    /*@NotNull*/
    public AtomicSequence getTypedValue(/*@NotNull*/ CharSequence value, NamespaceResolver resolver, ConversionRules rules) throws ValidationException {
        UnfailingIterator<StringValue> iter = new StringTokenIterator(value.toString());
        ListTypeMappingFunction map = new ListTypeMappingFunction();
        map.resolver = resolver;
        map.atomicType = (AtomicType) getItemType();
        map.rules = rules;
        try {
            return new AtomicArray(new MappingIterator(iter, map));
        } catch (XPathException err) {
            throw new ValidationException(err); // should not happen
        }
    }

    /*@NotNull*/
    private static BuiltInListType makeListType(String namespace, String lname) {
        BuiltInListType t = new BuiltInListType(StandardNames.getFingerprint(namespace, lname));
        BuiltInType.register(t.getFingerprint(), t);
        return t;
    }

    private static class ListTypeMappingFunction implements MappingFunction<StringValue, AtomicValue> {

        public NamespaceResolver resolver;
        /*@Nullable*/ public AtomicType atomicType;
        public ConversionRules rules;

        /**
         * The typed value of a list-valued node is obtained by tokenizing the string value and
         * applying a mapping function to the sequence of tokens.
         * This method implements the mapping function. It is for internal use only.
         * For details see {@link net.sf.saxon.expr.MappingFunction}
         */

        public SequenceIterator map(/*@NotNull*/ StringValue item) throws XPathException {
            try {
                return atomicType.getTypedValue(item.getStringValue(), resolver, rules).iterate();
            } catch (ValidationException err) {
                throw new XPathException(err);
            }
        }
    }

    /**
     * Apply any pre-lexical facets, other than whitespace. At the moment the only such
     * facet is saxon:preprocess
     *
     * @param input the value to be preprocessed
     * @return the value after preprocessing
     */

    public CharSequence preprocess(CharSequence input) {
        return input;
    }

    /**
     * Reverse any pre-lexical facets, other than whitespace. At the moment the only such
     * facet is saxon:preprocess. This is called when converting a value of this type to
     * a string
     *
     * @param input the value to be postprocessed: this is the "ordinary" result of converting
     *              the value to a string
     * @return the value after postprocessing
     */

    public CharSequence postprocess(CharSequence input) throws ValidationException {
        return input;
    }
}

