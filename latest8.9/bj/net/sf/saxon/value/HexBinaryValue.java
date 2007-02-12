package net.sf.saxon.value;
import net.sf.saxon.expr.XPathContext;
import net.sf.saxon.om.FastStringBuffer;
import net.sf.saxon.trans.DynamicError;
import net.sf.saxon.trans.XPathException;
import net.sf.saxon.type.*;

/**
* A value of type xs:hexBinary
*/

public class HexBinaryValue extends AtomicValue {

    private byte[] binaryValue;


    /**
    * Constructor: create a hexBinary value from a supplied string, in which
     * each octet is represented by a pair of values from 0-9, a-f, A-F
    */

    public HexBinaryValue(CharSequence in) throws XPathException {
        CharSequence s = Whitespace.trimWhitespace(in);
        if ((s.length() & 1) != 0) {
            DynamicError err = new DynamicError(
                    "A hexBinary value must contain an even number of characters");
            err.setErrorCode("FORG0001");
            throw err;
        }
        binaryValue = new byte[s.length() / 2];
        for (int i=0; i<binaryValue.length; i++) {
            binaryValue[i] = (byte)((fromHex(s.charAt(2*i))<<4) +
                    (fromHex(s.charAt(2*i+1))));
        }
        typeLabel = BuiltInAtomicType.HEX_BINARY;
    }

    /**
     * Constructor: create a HexBinary value from a supplied string in hexBinary encoding,
     * with a specified type. This method throws no checked exceptions; the caller is expected
     * to ensure that the string is a valid Base64 lexical representation, that it conforms
     * to the specified type, and that the type is indeed a subtype of xs:base64Binary.
     * An unchecked exception such as an IllegalArgumentException may be thrown if these
     * conditions are not satisfied, but this is not guaranteed.
     * @param s the value in hexBinary encoding, with no leading or trailing whitespace
     * @param type the atomic type. This must be xs:base64binary or a subtype.
     */

    public HexBinaryValue(CharSequence s, AtomicType type) {
        if ((s.length() & 1) != 0) {
            throw new IllegalArgumentException(
                    "A hexBinary value must contain an even number of characters");
        }
        binaryValue = new byte[s.length() / 2];
        try {
            for (int i=0; i<binaryValue.length; i++) {
                binaryValue[i] = (byte)((fromHex(s.charAt(2*i))<<4) +
                        (fromHex(s.charAt(2*i+1))));
            }
        } catch (XPathException e) {
            throw new IllegalArgumentException(e.getMessage());
        }
        typeLabel = type;
    }

    /**
     * Constructor: create a hexBinary value from a given array of bytes
     */

    public HexBinaryValue(byte[] value) {
        this.binaryValue = value;
        this.typeLabel = BuiltInAtomicType.HEX_BINARY;
    }

    /**
     * Create a primitive copy of this atomic value (usually so that the type label can be changed).
     * @param typeLabel
     */

    public AtomicValue copy(AtomicType typeLabel) {
        HexBinaryValue v = new HexBinaryValue(binaryValue);
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
        return BuiltInAtomicType.HEX_BINARY;
    }

   /**
     * Get the binary value
     */

    public byte[] getBinaryValue() {
        return binaryValue;
    }

    /**
     * Decode a single hex digit
     * @param c the hex digit
     * @return the numeric value of the hex digit
     * @throws XPathException if it isn't a hex digit
     */

    private int fromHex(char c) throws XPathException {
        int d = "0123456789ABCDEFabcdef".indexOf(c);
        if (d > 15) {
            d = d - 6;
        }
        if (d < 0) {
            DynamicError err = new DynamicError("Invalid hexadecimal digit");
            err.setErrorCode("FORG0001");
            throw err;
        }
        return d;
    }

    /**
    * Convert to target data type
    * @param requiredType an integer identifying the required atomic type
    * @param context
     * @return an AtomicValue, a value of the required type; or an ErrorValue
    */

    public AtomicValue convertPrimitive(BuiltInAtomicType requiredType, boolean validate, XPathContext context) {
        switch(requiredType.getPrimitiveType()) {
        case Type.HEX_BINARY:
        case Type.ANY_ATOMIC:
        case Type.ITEM:
            return this;
        case Type.STRING:
            return new StringValue(getStringValueCS());
        case Type.UNTYPED_ATOMIC:
            return new UntypedAtomicValue(getStringValueCS());
        case Type.BASE64_BINARY:
            return new Base64BinaryValue(binaryValue);

        default:
            ValidationException err = new ValidationException("Cannot convert hexBinarry to " +
                                     requiredType.getDisplayName());
            err.setErrorCode("XPTY0004");
            err.setIsTypeError(true);
            return new ValidationErrorValue(err);
        }
    }

    /**
    * Convert to string
    * @return the canonical representation.
    */

    public String getStringValue() {
        String digits = "0123456789ABCDEF";
        FastStringBuffer sb = new FastStringBuffer(binaryValue.length * 2);
        for (int i=0; i<binaryValue.length; i++) {
            sb.append(digits.charAt((binaryValue[i]>>4)&0xf));
            sb.append(digits.charAt(binaryValue[i]&0xf));
        }
        return sb.toString();
    }


    /**
     * Get the number of octets in the value
     */

    public int getLengthInOctets() {
        return binaryValue.length;
    }

    /**
    * Convert to Java object (for passing to external functions)
    */

    public Object convertToJava(Class target, XPathContext context) throws XPathException {

        if (target.isAssignableFrom(HexBinaryValue.class)) {
            return this;
        } else if (target.isAssignableFrom(String.class)) {
            return getStringValue();
        } else {
            Object o = super.convertToJava(target, context);
            if (o == null) {
                throw new DynamicError("Conversion of hexBinary to " + target.getName() +
                        " is not supported");
            }
            return o;
        }
    }


    /**
    * Test if the two hexBinary values are equal.
    */

    public boolean equals(Object other) {
        // TODO: support schema semantics here...
        HexBinaryValue v2;
        if (other instanceof HexBinaryValue) {
            v2 = (HexBinaryValue)other;
        } else if (other instanceof AtomicValue) {
            try {
                v2 = (HexBinaryValue)((AtomicValue)other).convert(BuiltInAtomicType.HEX_BINARY, null);
            } catch (XPathException err) {
                return false;
            }
        } else {
            return false;
        }
        if (binaryValue.length != v2.binaryValue.length) {
            return false;
        };
        for (int i = 0; i < binaryValue.length; i++) {
            if (binaryValue[i] != v2.binaryValue[i]) {
                return false;
            };
        }
        return true;
    }

     public int hashCode() {
        return Base64BinaryValue.byteArrayHashCode(binaryValue);
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
// The Initial Developer of the Original Code is Michael H. Kay, Saxonica.
//
// Portions created by (your name) are Copyright (C) (your legal entity). All Rights Reserved.
//
// Contributor(s): none.
//

