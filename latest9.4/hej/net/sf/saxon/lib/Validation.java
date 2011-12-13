package net.sf.saxon.lib;


/**
* This class contains constants and static methods to manipulate the validation
* property of a type.
*/

public final class Validation {

    /**
     * Code indicating that the value of a validation request was invalid
     */

    public static final int INVALID = -1;

    /**
     * Code for strict validation
     */

    public static final int STRICT = 1;

    /**
     * Code for lax validation
     */

    public static final int LAX = 2;

    /**
     * Code corresponding to the XSLT option validation=preserve, which indicates
     * that existing type annotations are to be preserved but no new validation is performed.
     */

    public static final int PRESERVE = 3;

    /**
     * Code corresponding to the XSLT option validation=strip, which indicates
     * that existing type annotations are to be removed and no new validation is performed.
     */

    public static final int STRIP = 4;

   /**
     * Synonym for {@link #STRIP}, corresponding to XQuery usage
     */

    public static final int SKIP = 4;   // synonym provided for the XQuery API

    /**
     * Code indicating that no specific validation options were requested
     */

    public static final int DEFAULT = 0;

    /**
     * Code indicating that validation against a named type was requested
     */

    public static final int BY_TYPE = 8;

    /**
     * Mask used when a validation code is combined with other information in an integer value
     */

    public static final int VALIDATION_MODE_MASK = 0xff;

    /**
     * Bit setting that can be combined with a validation code to indicate that the data being validated
     * is final output data, and that validation errors are therefore recoverable.
     */

    public static final int VALIDATE_OUTPUT = 0x10000;

    /**
     * This class is never instantiated
     */

    private Validation() {
    }

    /**
     * Get the integer validation code corresponding to a given string
     * @param value one of "strict", "lax", "preserve", or "strip"
     * @return the corresponding code {@link #STRICT}, {@link #LAX},
     * {@link #PRESERVE}, or {@link #STRIP}
     */

    public static int getCode(String value) {
        if (value.equals("strict")) {
            return STRICT;
        } else if (value.equals("lax")) {
            return LAX;
        } else if (value.equals("preserve")) {
            return PRESERVE;
        } else if (value.equals("strip")) {
            return STRIP;
        } else {
            return INVALID;
        }
    }

    /**
     * Get a string representation of a validation code
     * @param value one of the validation codes defined in this class
     * @return one of the strings "strict", "lax", "preserve", "skip" (sic), or "invalid"
     */

    /*@NotNull*/ public static String toString(int value) {
        switch(value & VALIDATION_MODE_MASK) {
            case STRICT: return "strict";
            case LAX: return "lax";
            case PRESERVE: return "preserve";
            case STRIP: return "skip";  // for XQuery
            case BY_TYPE: return "by type";
            default: return "invalid";
        }
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