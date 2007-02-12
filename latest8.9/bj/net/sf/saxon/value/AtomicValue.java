package net.sf.saxon.value;

import net.sf.saxon.Err;
import net.sf.saxon.expr.StaticContext;
import net.sf.saxon.expr.StaticProperty;
import net.sf.saxon.expr.XPathContext;
import net.sf.saxon.om.Item;
import net.sf.saxon.om.NodeInfo;
import net.sf.saxon.om.SequenceIterator;
import net.sf.saxon.om.SingletonIterator;
import net.sf.saxon.trans.DynamicError;
import net.sf.saxon.trans.StaticError;
import net.sf.saxon.trans.XPathException;
import net.sf.saxon.type.*;


/**
 * The AtomicValue class corresponds to the concept of an atomic value in the
 * XPath 2.0 data model. Atomic values belong to one of the 19 primitive types
 * defined in XML Schema; or they are of type xs:untypedAtomic; or they are
 * "external objects", representing a Saxon extension to the XPath 2.0 type system.
 * <p>
 * The AtomicValue class contains some methods that are suitable for applications
 * to use, and many others that are designed for internal use by Saxon itself.
 * These have not been fully classified. At present, therefore, none of the methods on this
 * class should be considered to be part of the public Saxon API.
 * <p>
 * @author Michael H. Kay
 */

public abstract class AtomicValue extends Value implements Item {

    protected AtomicType typeLabel;

    /**
     * Set the type label on this atomic value
     */

    public void setTypeLabel(AtomicType type) {
        typeLabel = type;
    }

    /**
     * Determine whether the value is multivalued, that is, whether it is a sequence that
     * potentially contains more than one item
     *
     * @return false for an atomic value
     */

    public boolean isMultiValued() {
        return false;
    }

    /**
     * Test whether the type of this atomic value is a built-in type.
     */

    public boolean hasBuiltInType() {
        return typeLabel.isBuiltInType();
    }

    /**
     * Get a Comparable value that implements the XPath ordering comparison semantics for this value.
     * Returns null if the value is not comparable according to XPath rules. The default implementation
     * returns null. This is overridden for types that allow ordered comparisons in XPath: numeric, boolean,
     * string, date, time, dateTime, yearMonthDuration, dayTimeDuration, and anyURI.
     */

    public Comparable getXPathComparable() {
        return null;
    }

    /**
     * Get the value of the item as a CharSequence. This is in some cases more efficient than
     * the version of the method that returns a String.
     */

    public CharSequence getStringValueCS() {
        return getStringValue();
    }

    /**
     * Process the instruction, without returning any tail calls
     *
     * @param context The dynamic context, giving access to the current node,
     *                the current variables, etc.
     */

    public void process(XPathContext context) throws XPathException {
        context.getReceiver().append(this, 0, NodeInfo.ALL_NAMESPACES);
    }

    /**
     * Get the n'th item in the sequence (starting from 0). This is defined for all
     * Values, but its real benefits come for a sequence Value stored extensionally
     * (or for a MemoClosure, once all the values have been read)
     * @param n position of the required item, counting from zero.
     * @return the n'th item in the sequence, where the first item in the sequence is
     * numbered zero. If n is negative or >= the length of the sequence, returns null.
     */

    public Item itemAt(int n) throws XPathException {
        return (n==0 ? this : null);
    }


    /**
     * Determine the data type of the items in the expression, if possible
     *
     * @param th The TypeHierarchy. Can be null if the target is an AtomicValue,
     * except in the case where it is an external ObjectValue.
     * @return for the default implementation: AnyItemType (not known)
     */

    public ItemType getItemType(TypeHierarchy th) {
        return typeLabel;
    }

    /**
     * Determine the data type of the value. This
     * delivers the same answer as {@link #getItemType}, except in the case of external objects
     * (instances of {@link ObjectValue}, where the method may deliver a less specific type.
     *
     * @return for the default implementation: AnyItemType (not known)
     */

    public AtomicType getTypeLabel() {
        return typeLabel;
    }

    /**
     * Determine the primitive type of the value. This delivers the same answer as
     * getItemType().getPrimitiveItemType(). The primitive types are
     * the 19 primitive types of XML Schema, plus xs:integer, xs:dayTimeDuration and xs:yearMonthDuration,
     * and xs:untypedAtomic. For external objects, the result is AnyAtomicType.
     */

    public abstract BuiltInAtomicType getPrimitiveType();

    /**
     * Determine the static cardinality
     *
     * @return code identifying the cardinality
     * @see net.sf.saxon.value.Cardinality
     */

    public final int getCardinality() {
        return StaticProperty.EXACTLY_ONE;
    }

    /**
     * Convert the value to a given type. The result of the conversion will be an
     * atomic value of the required type. This method works only where the target
     * type is a built-in type.
     *
     * @param schemaType the required atomic type
     * @param context
     * @return the result of the conversion, if conversion was possible. This
     *         will always be an instance of the class corresponding to the type
     *         of value requested
     * @throws XPathException if conversion is not allowed for this
     *                        required type, or if the particular value cannot be converted
     */

    public final AtomicValue convert(BuiltInAtomicType schemaType, XPathContext context) throws XPathException {
            AtomicValue val = convertPrimitive(schemaType, true, context);
            if (val instanceof ValidationErrorValue) {
                throw ((ValidationErrorValue)val).getException();
            }
            return val;
    };

    /**
     * Convert a value to another primitive data type, with control over how validation is
     * handled.
     *
     * @param requiredType type code of the required atomic type
     * @param validate     true if validation is required. If set to false, the caller guarantees that
     *                     the value is valid for the target data type, and that further validation is therefore not required.
     *                     Note that a validation failure may be reported even if validation was not requested.
     * @param context   The conversion context to be used. This is required at present only when converting to
     *                     xs:Name or similar types: it determines the NameChecker to be used.
     * @return the result of the conversion, if successful. If unsuccessful, the value returned
     *         will be a ValidationErrorValue. The caller must check for this condition. No exception is thrown, instead
     *         the exception will be encapsulated within the ValidationErrorValue.
     */
    public abstract AtomicValue convertPrimitive(
            BuiltInAtomicType requiredType, boolean validate, XPathContext context);
            // TODO: we could avoid the need for supplying a context on this and
            // similar interfaces by making the BuiltInAtomicType object version-sensitive

    /**
     * Convert the value to a given type. The result of the conversion will be
     * an atomic value of the required type. This method works where the target
     * type is a built-in atomic type and also where it is a user-defined atomic
     * type.
     *
     * @param targetType the type to which the value is to be converted
     * @param context    provides access to conversion context
     * @param validate   true if validation is required, false if the caller already knows that the
     *                   value is valid
     * @return the value after conversion if successful; or a {@link ValidationErrorValue} if conversion failed. The
     *         caller must check for this condition. Validation may fail even if validation was not requested.
     */

    public AtomicValue convert(AtomicType targetType, XPathContext context, boolean validate) {
        if (targetType instanceof BuiltInAtomicType) {
            return convertPrimitive((BuiltInAtomicType)targetType, validate, context);
        } else {
            CharSequence lexicalValue = getStringValueCS();
            AtomicValue v = convertPrimitive(
                    (BuiltInAtomicType)targetType.getPrimitiveItemType(),
                    validate,
                    context);
            if (v instanceof ValidationErrorValue) {
                // conversion has failed
                return v;
            }

            return targetType.setDerivedTypeLabel(v.copy(null), lexicalValue, validate);
        }
    }

    /**
     * Create a copy of this atomic value, with a different type label
     * @param typeLabel the type label of the new copy. The caller is responsible for checking that
     * the value actually conforms to this type.
     */

    public abstract AtomicValue copy(AtomicType typeLabel);

    /**
     * Get the length of the sequence
     *
     * @return always 1 for an atomic value
     */

    public final int getLength() {
        return 1;
    }

    /**
     * Iterate over the (single) item in the sequence
     *
     * @return a SequenceIterator that iterates over the single item in this
     *         value
     */

    public final SequenceIterator iterate() {
        return SingletonIterator.makeIterator(this);
    }

    /**
     * Convert the value to a string, using the serialization rules.
     * For atomic values this is the same as a cast; for sequence values
     * it gives a space-separated list. This method is refined for AtomicValues
     * so that it never throws an Exception.
     */

    public abstract String getStringValue();


    /**
     * Get the typed value of this item
     *
     * @return the typed value of the expression (which is this value)
     */

    public final SequenceIterator getTypedValue() {
        return SingletonIterator.makeIterator(this);
    }

    /**
     * Get the effective boolean value of the value
     *
     * @return true, unless the value is boolean false, numeric zero, or
     *         zero-length string
     */
    public boolean effectiveBooleanValue() throws XPathException {
        DynamicError err = new DynamicError("Effective boolean value is not defined for an atomic value of type " +
                Type.displayTypeName(this));
        err.setIsTypeError(true);
        err.setErrorCode("FORG0006");
        throw err;
        // unless otherwise specified in a subclass
    }

    /**
     * Method to extract components of a value. Implemented by some subclasses,
     * but defined at this level for convenience
     */

    public AtomicValue getComponent(int component) throws XPathException {
        throw new UnsupportedOperationException("Data type does not support component extraction");
    }

    /**
     * Check statically that the results of the expression are capable of constructing the content
     * of a given schema type.
     *
     * @param parentType The schema type
     * @param env        the static context
     * @param whole      true if this atomic value accounts for the entire content of the containing node
     * @throws net.sf.saxon.trans.XPathException
     *          if the expression doesn't match the required content type
     */

    public void checkPermittedContents(SchemaType parentType, StaticContext env, boolean whole) throws XPathException {
        if (whole) {
            SimpleType stype = null;
            if (parentType instanceof SimpleType) {
                stype = (SimpleType)parentType;
            } else if (parentType instanceof ComplexType && ((ComplexType)parentType).isSimpleContent()) {
                stype = ((ComplexType)parentType).getSimpleContentType();
            }
            if (stype != null && !stype.isNamespaceSensitive()) {
                // Can't validate namespace-sensitive content statically
                XPathException err = stype.validateContent(
                        getStringValueCS(), null, env.getConfiguration().getNameChecker());
                if (err != null) {
                    throw err;
                }
                return;
            }
        }
        if (parentType instanceof ComplexType &&
                !((ComplexType)parentType).isSimpleContent() &&
                !((ComplexType)parentType).isMixedContent() &&
                !Whitespace.isWhite(getStringValueCS())) {
            StaticError err = new StaticError("Complex type " + parentType.getDescription() +
                    " does not allow text content " +
                    Err.wrap(getStringValueCS()));
            err.setIsTypeError(true);
            throw err;
        }
    }

    /**
     * Get string value. In general toString() for an atomic value displays the value as it would be
     * written in XPath: that is, as a literal if available, or as a call on a constructor function
     * otherwise.
     */

    public String toString() {
        return typeLabel.toString() + " (\"" + getStringValueCS() + "\")";
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

