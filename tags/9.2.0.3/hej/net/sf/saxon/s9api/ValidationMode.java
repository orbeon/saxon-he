package net.sf.saxon.s9api;

import net.sf.saxon.om.Validation;


/**
 * Enumeration class defining different schema validation (or construction) modes
 */
public enum ValidationMode {
    /**
     * Strict validation
     */
    STRICT (Validation.STRICT),
    /**
     * Lax validation
     */
    LAX (Validation.LAX),
    /**
     * Preserve existing type annotations if any
     */
    PRESERVE (Validation.PRESERVE),
    /**
     * Remove any existing type annotations, mark as untyped
     */
    STRIP (Validation.STRIP),
    /**
     * Value indication no preference: the choice is defined elsewhere
     */
    DEFAULT (Validation.DEFAULT);

    private int number;

    private ValidationMode(int number) {
        this.number = number;
    }

    protected int getNumber() {
        return number;
    }
    
    protected static ValidationMode get(int number) {
        switch (number) {
            case Validation.STRICT:
                return STRICT;
            case Validation.LAX:
                return LAX;
            case Validation.STRIP:
                return STRIP;
            case Validation.PRESERVE:
                return PRESERVE;
            case Validation.DEFAULT:
            default:
                return DEFAULT;
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
// The Initial Developer of the Original Code is Michael H. Kay.
//
// Contributor(s):
//