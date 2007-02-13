package net.sf.saxon.dotnet;

import net.sf.saxon.Configuration;
import net.sf.saxon.expr.Expression;
import net.sf.saxon.expr.StaticContext;
import net.sf.saxon.expr.Literal;
import net.sf.saxon.instruct.ValueOf;
import net.sf.saxon.om.*;
import net.sf.saxon.trans.XPathException;
import net.sf.saxon.type.*;
import net.sf.saxon.value.AtomicValue;
import net.sf.saxon.value.Value;
import net.sf.saxon.value.Whitespace;

import java.io.Serializable;

/**
 * This class represents the type of an external Java object returned by
 * an extension function, or supplied as an external variable/parameter.
 */

public class DotNetExternalObjectType implements AtomicType, Serializable {

    private cli.System.Type dotNetType;
    private Configuration config;
    int fingerprint;
    int baseFingerprint = -1;

    //public static final ExternalObjectType GENERAL_EXTERNAL_OBJECT_TYPE = new ExternalObjectType(Object.class, config);

    public DotNetExternalObjectType(cli.System.Type dotNetType, Configuration config) {
        this.dotNetType = dotNetType;
        this.config = config;
        final String localName = dotNetType.get_Name().replace('$', '_');
        this.fingerprint = config.getNamePool().allocate("", NamespaceConstant.DOT_NET_TYPE, localName);
    }

    /**
     * Return true if this is an external object type, that is, a Saxon-defined type for external
     * Java or .NET objects
     */

    public boolean isExternalType() {
        return true;
    }

    /**
     * Determine whether this is a built-in type or a user-defined type
     */

    public boolean isBuiltInType() {
        return false;
    }


    /**
     * Get the most specific possible atomic type that all items in this SimpleType belong to
     * @return the lowest common supertype of all member types
     */

    public AtomicType getCommonAtomicType() {
        return this;
    }

    /**
     * Determine whether the atomic type is ordered, that is, whether less-than and greater-than comparisons
     * are permitted
     *
     * @return true if ordering operations are permitted
     */

    public boolean isOrdered() {
        return false;  
    }

    /**
     * Get the validation status - always valid
     */
    public final int getValidationStatus()  {
        return VALIDATED;
    }

    /**
     * Returns the value of the 'block' attribute for this type, as a bit-signnificant
     * integer with fields such as {@link net.sf.saxon.type.SchemaType#DERIVATION_LIST} and {@link net.sf.saxon.type.SchemaType#DERIVATION_EXTENSION}
     *
     * @return the value of the 'block' attribute for this type
     */

    public final int getBlock() {
        return 0;
    }

    /**
     * Gets the integer code of the derivation method used to derive this type from its
     * parent. Returns zero for primitive types.
     *
     * @return a numeric code representing the derivation method, for example {@link net.sf.saxon.type.SchemaType#DERIVATION_RESTRICTION}
     */

    public final int getDerivationMethod() {
        return SchemaType.DERIVATION_RESTRICTION;
    }

    /**
     * Determines whether derivation (of a particular kind)
     * from this type is allowed, based on the "final" property
     *
     * @param derivation the kind of derivation, for example {@link net.sf.saxon.type.SchemaType#DERIVATION_LIST}
     * @return true if this kind of derivation is allowed
     */

    public final boolean allowsDerivation(int derivation) {
        return true;
    }

    /**
     * Get the namecode of the name of this type. This includes the prefix from the original
     * type declaration: in the case of built-in types, there may be a conventional prefix
     * or there may be no prefix.
     */

    public int getNameCode() {
        return fingerprint;
    }

    /**
     * Test whether this SchemaType is a complex type
     *
     * @return true if this SchemaType is a complex type
     */

    public final boolean isComplexType() {
        return false;
    }

    /**
     * Returns the base type that this type inherits from. This method can be used to get the
     * base type of a type that is known to be valid.
     * If this type is a Simpletype that is a built in primitive type then null is returned.
     *
     * @return the base type.
     */

    public final SchemaType getBaseType() {
        return BuiltInAtomicType.ANY_ATOMIC;
    }

    /**
     * Get the primitive item type corresponding to this item type. For item(),
     * this is Type.ITEM. For node(), it is Type.NODE. For specific node kinds,
     * it is the value representing the node kind, for example Type.ELEMENT.
     * For anyAtomicValue it is Type.ATOMIC_VALUE. For numeric it is Type.NUMBER.
     * For other atomic types it is the primitive type as defined in XML Schema,
     * except that INTEGER is considered to be a primitive type.
     */

    public ItemType getPrimitiveItemType() {
        return this;
    }

    /**
     * Get the primitive type corresponding to this item type. For item(),
     * this is Type.ITEM. For node(), it is Type.NODE. For specific node kinds,
     * it is the value representing the node kind, for example Type.ELEMENT.
     * For anyAtomicValue it is Type.ATOMIC. For numeric it is Type.NUMBER.
     * For other atomic types it is the primitive type as defined in XML Schema,
     * except that INTEGER is considered to be a primitive type.
     */

    public int getPrimitiveType() {
        return Type.ANY_ATOMIC;
    }

    /**
     * Produce a representation of this type name for use in error messages.
     * Where this is a QName, it will use conventional prefixes
     */

    public String toString(NamePool pool) {
        return getDisplayName();
    }

    /**
     * Get the item type of the atomic values that will be produced when an item
     * of this type is atomized
     */

    public AtomicType getAtomizedItemType() {
        return this;
    }

    /**
     * Returns the base type that this type inherits from. This method can be used to get the
     * base type of a type that is known to be valid.
     * If this type is a Simpletype that is a built in primitive type then null is returned.
     *
     * @return the base type.
     * @throws IllegalStateException if this type is not valid.
     */

    public SchemaType getKnownBaseType() {
        return getBaseType();
    }

    /**
     * Test whether this is the same type as another type. They are considered to be the same type
     * if they are derived from the same type definition in the original XML representation (which
     * can happen when there are multiple includes of the same file)
     */

    public boolean isSameType(SchemaType other) {
        return (other.getFingerprint() == this.getFingerprint());
    }

    /**
     * Get the relationship of this external object type to another external object type
     * @return
     */

    public int getRelationship(DotNetExternalObjectType other) {
        cli.System.Type j2 = other.dotNetType;
        if (dotNetType.equals(j2)) {
            return TypeHierarchy.SAME_TYPE;
        } else if (dotNetType.IsAssignableFrom(j2)) {
            return TypeHierarchy.SUBSUMES;
        } else if (j2.IsAssignableFrom(dotNetType)) {
            return TypeHierarchy.SUBSUMED_BY;
        } else if (dotNetType.get_IsInterface() || j2.get_IsInterface()) {
            return TypeHierarchy.OVERLAPS; // there may be an overlap, we play safe
        } else {
            return TypeHierarchy.DISJOINT;
        }
    }

    public String getDescription() {
        return getDisplayName();
    }

    /**
     * Check that this type is validly derived from a given type
     *
     * @param type  the type from which this type is derived
     * @param block the derivations that are blocked by the relevant element declaration
     * @throws net.sf.saxon.type.SchemaException if the derivation is not allowed
     */

    public void checkTypeDerivationIsOK(SchemaType type, int block) throws SchemaException, ValidationException {
        return;
    }

    /**
     * Returns true if this SchemaType is a SimpleType
     *
     * @return true (always)
     */

    public final boolean isSimpleType() {
        return true;
    }

    /**
     * Test whether this Simple Type is an atomic type
     * @return true, this is considered to be an atomic type
     */

    public boolean isAtomicType() {
        return true;
    }


    /**
     * Returns true if this type is derived by list, or if it is derived by restriction
     * from a list type, or if it is a union that contains a list as one of its members
     *
     * @return true if this is a list type
     */

    public boolean isListType() {
        return false;
    }

    /**
     * Return true if this type is a union type (that is, if its variety is union)
     *
     * @return true for a union type
     */

    public boolean isUnionType() {
        return false;
    }

    /**
     * Determine the whitespace normalization required for values of this type
     *
     * @return one of PRESERVE, REPLACE, COLLAPSE
     * @param th
     */

    public int getWhitespaceAction(TypeHierarchy th) {
        return Whitespace.PRESERVE;
    }

    /**
     * Apply the whitespace normalization rules for this simple type
     *
     * @param value the string before whitespace normalization
     * @return the string after whitespace normalization
     */

    public CharSequence applyWhitespaceNormalization(CharSequence value) throws ValidationException {
        return value;
    }

    /**
     * Returns the built-in base type this type is derived from.
     *
     * @return the first built-in type found when searching up the type hierarchy
     */
    public SchemaType getBuiltInBaseType() {
        return this;
    }

    /**
     * Test whether this simple type is namespace-sensitive, that is, whether
     * it is derived from xs:QName or xs:NOTATION
     *
     * @return true if this type is derived from xs:QName or xs:NOTATION
     */

    public boolean isNamespaceSensitive() {
        return false;
    }

    /**
     * Test whether this is an anonymous type
     * @return true if this SchemaType is an anonymous type
     */

    public boolean isAnonymousType() {
        return false;
    }

    /**
     * Get the typed value of a node that is annotated with this schema type
     *
     * @param node the node whose typed value is required
     * @return an iterator over the items making up the typed value of this node. The objects
     *         returned by this SequenceIterator will all be of type {@link net.sf.saxon.value.AtomicValue}
     */

    public final SequenceIterator getTypedValue(NodeInfo node) {
        throw new IllegalStateException("The type annotation of a node cannot be an external object type");
    }

    /**
     * Get the typed value of a node that is annotated with this schema type. The result of this method will always be consistent with the method
     * {@link #getTypedValue}. However, this method is often more convenient and may be
     * more efficient, especially in the common case where the value is expected to be a singleton.
     *
     * @param node the node whose typed value is required
     * @return the typed value.
     * @since 8.5
     */

    public Value atomize(NodeInfo node) throws XPathException {
        throw new IllegalStateException("The type annotation of a node cannot be an external object type");
    }

    /**
     * Get the typed value corresponding to a given string value, assuming it is
     * valid against this type
     *
     * @param value    the string value
     * @param resolver a namespace resolver used to resolve any namespace prefixes appearing
     *                 in the content of values. Can supply null, in which case any namespace-sensitive content
     *                 will be rejected.
     * @param nameChecker
     * @return an iterator over the atomic sequence comprising the typed value. The objects
     *         returned by this SequenceIterator will all be of type {@link net.sf.saxon.value.AtomicValue}
     */

    public SequenceIterator getTypedValue(CharSequence value, NamespaceResolver resolver, NameChecker nameChecker)
            throws ValidationException {
        throw new ValidationException("Cannot validate a string against an external object type");
    }


    /**
     * Factory method to create values of a derived atomic type. This method
     * is not used to create values of a built-in type, even one that is not
     * primitive.
     *
     * @param primValue    the value in the value space of the primitive type
     * @param lexicalValue the value in the lexical space. If null, the string value of primValue
      * @param validate     true if the value is to be validated against the facets of the derived
     *                     type; false if the caller knows that the value is already valid.
     */

    public AtomicValue setDerivedTypeLabel(AtomicValue primValue, CharSequence lexicalValue, boolean validate) {
        throw new UnsupportedOperationException("makeDerivedValue is not supported for external object types");
    }

    /**
     * Analyze an expression to see whether the expression is capable of delivering a value of this
     * type.
     *
     * @param expression the expression that delivers the content
     * @param kind       the node kind whose content is being delivered: {@link net.sf.saxon.type.Type#ELEMENT},
     *                   {@link net.sf.saxon.type.Type#ATTRIBUTE}, or {@link net.sf.saxon.type.Type#DOCUMENT}
     * @param env
     * @throws net.sf.saxon.trans.XPathException
     *          if the expression will never deliver a value of the correct type
     */

    public void analyzeContentExpression(Expression expression, int kind, StaticContext env) throws XPathException {
        analyzeContentExpression(this, expression, env, kind);
    }

   /**
     * Analyze an expression to see whether the expression is capable of delivering a value of this
     * type.
     * @param simpleType the simple type against which the expression is to be checked
     * @param expression the expression that delivers the content
     * @param kind       the node kind whose content is being delivered: {@link net.sf.saxon.type.Type#ELEMENT},
     *                   {@link net.sf.saxon.type.Type#ATTRIBUTE}, or {@link net.sf.saxon.type.Type#DOCUMENT}
     * @throws net.sf.saxon.trans.XPathException
     *          if the expression will never deliver a value of the correct type
     */

    public static void analyzeContentExpression(SimpleType simpleType, Expression expression, StaticContext env, int kind)
    throws XPathException {
        if (kind == Type.ELEMENT) {
            expression.checkPermittedContents(simpleType, env, true);
        } else if (kind == Type.ATTRIBUTE) {
            // for attributes, do a check only for text nodes and atomic values: anything else gets atomized
            if (expression instanceof ValueOf || expression instanceof Literal) {
                expression.checkPermittedContents(simpleType, env, true);
            }
        }
    }


    public cli.System.Type getDotNetType() {
        return dotNetType;
    }

    public boolean isBuiltIn() {
        return true;
    }

    public boolean matchesItem(Item item, boolean allowURIPromotion, Configuration config) {
        if (item instanceof DotNetObjectValue) {
            cli.System.Object obj = (cli.System.Object)((DotNetObjectValue)item).getObject();
            return dotNetType.IsAssignableFrom(obj.GetType());
        }
        return false;
    }

    /**
     * Check whether a given input string is valid according to this SimpleType
     * @param value the input string to be checked
     * @param nsResolver a namespace resolver used to resolve namespace prefixes if the type
     * is namespace sensitive. The value supplied may be null; in this case any namespace-sensitive
     * content will throw an UnsupportedOperationException.
     * @param nameChecker
     * @return null if validation succeeds; return a ValidationException describing the validation failure
     * if validation fails, unless throwException is true, in which case the exception is thrown rather than
     * being returned.
     * @throws UnsupportedOperationException if the type is namespace-sensitive and no namespace
     * resolver is supplied
     */

    public ValidationException validateContent(CharSequence value, NamespaceResolver nsResolver, NameChecker nameChecker) {
        throw new UnsupportedOperationException("Cannot use an external object type for validation");
    }

    public ItemType getSuperType(TypeHierarchy th) {
        if (dotNetType == cli.System.Type.GetType("cli.System.Object")) {
            return BuiltInAtomicType.ANY_ATOMIC;
        }
        cli.System.Type javaSuper = dotNetType.get_BaseType();
        if (javaSuper == null) {
            // this happens for an interface
            return BuiltInAtomicType.ANY_ATOMIC;
        }
        return new DotNetExternalObjectType(javaSuper, config);
    }

    public int getFingerprint() {
        return fingerprint;
    }

    public String toString() {
        String name = dotNetType.ToString();
        name = name.replace('$', '-');
        return "type:" + name;
    }

    public String getDisplayName() {
        return toString();
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
// The Initial Developer of the Original Code is Saxonica Limited
//
// Portions created by (your name) are Copyright (C) (your legal entity). All Rights Reserved.
//
// Contributor(s): none
//