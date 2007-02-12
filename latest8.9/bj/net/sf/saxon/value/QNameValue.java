package net.sf.saxon.value;

import net.sf.saxon.expr.XPathContext;
import net.sf.saxon.functions.Component;
import net.sf.saxon.om.NameChecker;
import net.sf.saxon.om.NamePool;
import net.sf.saxon.trans.DynamicError;
import net.sf.saxon.trans.XPathException;
import net.sf.saxon.type.AtomicType;
import net.sf.saxon.type.BuiltInAtomicType;
import net.sf.saxon.type.Type;
import net.sf.saxon.type.ValidationException;

/**
 * A QName value. This implements the so-called "triples proposal", in which the prefix is retained as
 * part of the value. The prefix is not used in any operation on a QName other than conversion of the
 * QName to a string.
 */

public class QNameValue extends QualifiedNameValue {


    /**
     * Constructor
     * @param namePool The name pool containing the specified name code
     * @param nameCode The name code identifying this name in the name pool
     */

    public QNameValue(NamePool namePool, int nameCode) {
        prefix = namePool.getPrefix(nameCode);
        uri = namePool.getURI(nameCode);
        if (uri.length() == 0) {
            uri = null;
        }
        localPart = namePool.getLocalName(nameCode);
        this.typeLabel = BuiltInAtomicType.QNAME;
    }

    /**
     * Constructor for a QName that is known to be valid. No validation takes place.
     * @param prefix The prefix part of the QName (not used in comparisons). Use "" to represent the
     * default prefix.
     * @param uri The namespace part of the QName. Use "" to represent the non-namespace.
     * @param localName The local part of the QName
     */

    public QNameValue(String prefix, String uri, String localName) {
        this(prefix, uri, localName, BuiltInAtomicType.QNAME);
    }

    /**
     * Constructor for a QName that is known to be valid, allowing a user-defined subtype of QName
     * to be specified. No validation takes place.
     * @param prefix The prefix part of the QName (not used in comparisons). Use "" to represent the
     * default prefix (but null is also accepted)
     * @param uri The namespace part of the QName. Use null to represent the non-namespace (but "" is also
     * accepted).
     * @param localName The local part of the QName
     */

    public QNameValue(String prefix, String uri, String localName, AtomicType type) {
        this.prefix = (prefix==null ? "" : prefix);
        this.uri = ("".equals(uri) ? null : uri);
        this.localPart = localName;
        this.typeLabel = type;
    }

    /**
     * Constructor. This constructor validates that the local part is a valid NCName.
     * @param prefix The prefix part of the QName (not used in comparisons). Use "" to represent the
     * default prefix (but null is also accepted).
     * Note that the prefix is not checked for lexical correctness, because in most cases
     * it will already have been matched against in-scope namespaces. Where necessary the caller must
     * check the prefix.
     * @param uri The namespace part of the QName. Use null to represent the non-namespace (but "" is also
     * accepted).
     * @param localName The local part of the QName
     * @param type The atomic type, which must be either xs:QName, or a
     * user-defined type derived from xs:QName by restriction
     * @param checker NameChecker used to check the name against XML 1.0 or XML 1.1 rules. Supply null
     * if the name does not need to be checked (the caller asserts that it is known to be valid)
     */

    public QNameValue(String prefix, String uri, String localName, AtomicType type, NameChecker checker) throws XPathException {
        if (checker != null && !checker.isValidNCName(localName)) {
            DynamicError err = new DynamicError("Malformed local name in QName: '" + localName + '\'');
            err.setErrorCode("FORG0001");
            throw err;
        }
        this.prefix = (prefix==null ? "" : prefix);
        this.uri = ("".equals(uri) ? null : uri);
        if (checker != null && this.uri == null && this.prefix.length() != 0) {
            DynamicError err = new DynamicError("QName has null namespace but non-empty prefix");
            err.setErrorCode("FOCA0002");
            throw err;
        }
        this.localPart = localName;
        this.typeLabel = type;
    }

    /**
     * Create a copy of this atomic value, with a different type label
     *
     * @param typeLabel the type label of the new copy. The caller is responsible for checking that
     *                  the value actually conforms to this type.
     */

    public AtomicValue copy(AtomicType typeLabel) {
        QNameValue v = new QNameValue(prefix, uri, localPart);
        v.typeLabel = typeLabel;
        return v;
    }

    /**
     * Determine the primitive type of the value. This delivers the same answer as
     * getItemType().getPrimitiveItemType(). The primitive types are
     * the 19 primitive types of XML Schema, plus xs:integer, xs:dayTimeDuration and xs:yearMonthDuration,
     * and xs:untypedAtomic. For external objects, the result is AnyAtomicType.
     */

    public BuiltInAtomicType getPrimitiveType() {
        return BuiltInAtomicType.QNAME;
    }

    /**
     * Convert a QName to target data type
     * @param requiredType an integer identifying the required atomic type
     * @param context
     * @return an AtomicValue, a value of the required type; or an ErrorValue
     */

    public AtomicValue convertPrimitive(BuiltInAtomicType requiredType, boolean validate, XPathContext context) {
        switch (requiredType.getPrimitiveType()) {
            case Type.ANY_ATOMIC:
            case Type.ITEM:
            case Type.QNAME:
                return this;
            case Type.STRING:
                return new StringValue(getStringValue());
            case Type.UNTYPED_ATOMIC:
                return new UntypedAtomicValue(getStringValue());
            default:
                ValidationException err = new ValidationException("Cannot convert QName to " +
                        requiredType.getDisplayName());
                err.setErrorCode("XPTY0004");
                err.setIsTypeError(true);
                return new ValidationErrorValue(err);
        }
    }

    /**
     * Get a component. Returns a zero-length string if the namespace-uri component is
     * requested and is not present.
     * @param part either Component.LOCALNAME or Component.NAMESPACE indicating which
     * component of the value is required
     * @return either the local name or the namespace URI, in each case as a StringValue
     */

    public AtomicValue getComponent(int part) {
        if (part == Component.LOCALNAME) {
            return StringValue.makeRestrictedString(
                    localPart, BuiltInAtomicType.NCNAME, null);
        } else if (part == Component.NAMESPACE) {
            return new AnyURIValue(uri);
        } else if (part == Component.PREFIX) {
            if (prefix.length() == 0) {
                return null;
            } else {
                return StringValue.makeRestrictedString(
                        prefix, BuiltInAtomicType.NCNAME, null);
            }
        } else {
            throw new UnsupportedOperationException("Component of QName must be URI, Local Name, or Prefix");
        }
    }

    /**
     * Determine if two QName values are equal. This comparison ignores the prefix part
     * of the value.
     * @throws ClassCastException if they are not comparable
     * @throws IllegalStateException if the two QNames are in different name pools
     */

    public boolean equals(Object other) {
        QNameValue val = (QNameValue)other;
        return localPart.equals(val.localPart) && (uri==val.uri || (uri != null && uri.equals(val.uri)));
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
// The Initial Developer of the Original Code is Michael H. Kay.
//
// Contributor(s):
//

