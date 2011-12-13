package net.sf.saxon.type;

import net.sf.saxon.lib.ConversionRules;
import net.sf.saxon.om.NamespaceResolver;
import net.sf.saxon.om.SequenceIterator;
import net.sf.saxon.value.AtomicValue;

/**
 * This interface represents a simple type, which may be a built-in simple type, or
 * a user-defined simple type.
 */

public interface SimpleType extends SchemaType {

    public static final int VARIETY_ATOMIC = 10;
    public static final int VARIETY_LIST = 11;
    public static final int VARIETY_UNION = 12;
    public static final int VARIETY_UNSPECIFIED_SIMPLE = 13;    

    /**
     * Test whether this Simple Type is an atomic type
     * @return true if this is an atomic type
     */

    boolean isAtomicType();

    /**
     * Test whether this Simple Type is a list type
     * @return true if this is a list type
     */
    boolean isListType();

   /**
     * Test whether this Simple Type is a union type
     * @return true if this is a union type
     */

    boolean isUnionType();

    /**
     * Return true if this is an external object type, that is, a Saxon-defined type for external
     * Java or .NET objects
     * @return true if this is an external type
     */

    boolean isExternalType();

    /**
     * Determine whether this is a built-in type or a user-defined type
     * @return true if this is a built-in type
     */

    boolean isBuiltInType();

    /**
     * Get the built-in type from which this type is derived by restriction
     * @return the built-in type from which this type is derived by restriction. This will not necessarily
     * be a primitive type.
     */

    /*@Nullable*/ SchemaType getBuiltInBaseType();

    /**
     * Get the typed value corresponding to a given string value, assuming it is
     * valid against this type
     * @param value the string value
     * @param resolver a namespace resolver used to resolve any namespace prefixes appearing
     * in the content of values. Can supply null, in which case any namespace-sensitive content
     * will be rejected.
     * @param rules the conversion rules from the configuration
     * @return an iterator over the atomic sequence comprising the typed value. The objects
     * returned by this SequenceIterator will all be of type {@link net.sf.saxon.value.AtomicValue},
     * The next() method on the iterator throws no checked exceptions, although it is not actually
     * declared as an UnfailingIterator.
     * @throws ValidationException if the supplied value is not in the lexical space of the data type
     */

    public SequenceIterator<AtomicValue> getTypedValue(CharSequence value, /*@Nullable*/ NamespaceResolver resolver, ConversionRules rules)
            throws ValidationException;

    /**
     * Check whether a given input string is valid according to this SimpleType
     * @param value the input string to be checked
     * @param nsResolver a namespace resolver used to resolve namespace prefixes if the type
     * is namespace sensitive. The value supplied may be null; in this case any namespace-sensitive
     * content will throw an UnsupportedOperationException.
     * @param rules the conversion rules from the configuration
     * @return null if validation succeeds; or return a ValidationFailure describing the validation failure
     * if validation fails. Note that the exception is returned rather than being thrown.
     * @throws UnsupportedOperationException if the type is namespace-sensitive and no namespace
     * resolver is supplied
     */

    /*@Nullable*/
    ValidationFailure validateContent(
            /*@NotNull*/ CharSequence value, /*@Nullable*/ NamespaceResolver nsResolver, /*@NotNull*/ ConversionRules rules);

    /**
     * Test whether this type is namespace sensitive, that is, if a namespace context is needed
     * to translate between the lexical space and the value space. This is true for types derived
     * from, or containing, QNames and NOTATIONs
     * @return true if the type is namespace-sensitive
     */

    boolean isNamespaceSensitive();

    /**
     * Determine how values of this simple type are whitespace-normalized.
     * @return one of {@link net.sf.saxon.value.Whitespace#PRESERVE}, {@link net.sf.saxon.value.Whitespace#COLLAPSE},
     * {@link net.sf.saxon.value.Whitespace#REPLACE}.
     */

    public int getWhitespaceAction();

    /**
     * Apply any pre-lexical facets, other than whitespace. At the moment the only such
     * facet is saxon:preprocess
     * @param input the value to be preprocessed
     * @return the value after preprocessing
     * @throws ValidationException if preprocessing detects that the value is invalid
     */

    public CharSequence preprocess(CharSequence input) throws ValidationException;

    /**
     * Reverse any pre-lexical facets, other than whitespace. At the moment the only such
     * facet is saxon:preprocess. This is called when converting a value of this type to
     * a string
     * @param input the value to be postprocessed: this is the "ordinary" result of converting
     * the value to a string
     * @return the value after postprocessing
     * @throws ValidationException if postprocessing detects that the value is invalid
     */

    public CharSequence postprocess(CharSequence input) throws ValidationException;

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