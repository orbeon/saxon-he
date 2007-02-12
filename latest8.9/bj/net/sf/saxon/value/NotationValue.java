package net.sf.saxon.value;

import net.sf.saxon.expr.XPathContext;
import net.sf.saxon.om.NameChecker;
import net.sf.saxon.trans.XPathException;
import net.sf.saxon.trans.DynamicError;
import net.sf.saxon.type.*;


/**
 * An xs:NOTATION value.
 */

public final class NotationValue extends QualifiedNameValue {

   /**
     * Constructor
     * @param prefix The prefix part of the QName (not used in comparisons). Use null or "" to represent the
     * default prefix.
     * @param uri The namespace part of the QName. Use null or "" to represent the null namespace.
     * @param localName The local part of the QName
     */

    public NotationValue(String prefix, String uri, String localName, NameChecker checker) throws XPathException {
        if (checker != null && !checker.isValidNCName(localName)) {
            DynamicError err = new DynamicError("Malformed local name in NOTATION: '" + localName + '\'');
            err.setErrorCode("FORG0001");
            throw err;
        }
        this.prefix = (prefix==null ? "" : prefix);
        this.uri = (uri==null ? "" : uri);
        if (checker != null && this.uri.equals("") && !this.prefix.equals("")) {
            DynamicError err = new DynamicError("NOTATION has null namespace but non-empty prefix");
            err.setErrorCode("FOCA0002");
            throw err;
        }
        this.localPart = localName;
        this.typeLabel = BuiltInAtomicType.NOTATION;
    }

   /**
     * Constructor for a value that is known to be valid
     * @param prefix The prefix part of the QName (not used in comparisons). Use null or "" to represent the
     * default prefix.
     * @param uri The namespace part of the QName. Use null or "" to represent the null namespace.
     * @param localName The local part of the QName
     */

    public NotationValue(String prefix, String uri, String localName) {
        this.prefix = prefix;
        this.uri = uri;
        this.localPart = localName;
        this.typeLabel = BuiltInAtomicType.NOTATION;
    }

    /**
      * Constructor for a value that is known to be valid
      * @param prefix The prefix part of the QName (not used in comparisons). Use null or "" to represent the
      * default prefix.
      * @param uri The namespace part of the QName. Use null or "" to represent the null namespace.
      * @param localName The local part of the QName
      */

     public NotationValue(String prefix, String uri, String localName, AtomicType typeLabel) {
         this.prefix = prefix;
         this.uri = uri;
         this.localPart = localName;
         this.typeLabel = typeLabel;
     }


    /**
     * Create a copy of this atomic value, with a different type label
     *
     * @param typeLabel the type label of the new copy. The caller is responsible for checking that
     *                  the value actually conforms to this type.
     */

    public AtomicValue copy(AtomicType typeLabel) {
        NotationValue v = new NotationValue(getPrefix(), getNamespaceURI(), getLocalName());
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
        return BuiltInAtomicType.NOTATION;
    }

    /**
     * Convert to target data type
     * @param requiredType an integer identifying the required atomic type
     * @param context
     * @return an AtomicValue, a value of the required type; or an ErrorValue
     */

    public AtomicValue convertPrimitive(BuiltInAtomicType requiredType, boolean validate, XPathContext context) {
        switch (requiredType.getPrimitiveType()) {
            case Type.ANY_ATOMIC:
            case Type.ITEM:
            case Type.NOTATION:
                return this;

            case Type.STRING:
                return new StringValue(getStringValue());
                
            case Type.UNTYPED_ATOMIC:
                return new UntypedAtomicValue(getStringValue());

            default:
                ValidationException err = new ValidationException("Cannot convert NOTATION to " +
                        requiredType.getDisplayName());
                err.setErrorCode("XPTY0004");
                err.setIsTypeError(true);
                return new ValidationErrorValue(err);
        }
    }

    /**
     * Determine if two Notation values are equal. This comparison ignores the prefix part
     * of the value.
     * @throws ClassCastException if they are not comparable
     * @throws IllegalStateException if the two QNames are in different name pools
     */

    public boolean equals(Object other) {
        NotationValue val = (NotationValue)other;
        return localPart.equals(val.localPart) && (uri==val.uri || (uri != null && uri.equals(val.uri)));
    }

     /**
     * The toString() method returns the name in the form QName("uri", "local")
     * @return the name in Clark notation: {uri}local
     */

    public String toString() {
        return "NOTATION(" + getClarkName() + ')';
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
// The Initial Developer of the Original Code is Michael H. Kay
//
// Portions created by (your name) are Copyright (C) (your legal entity). All Rights Reserved.
//
// Contributor(s): none.
//

