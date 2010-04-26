package net.sf.saxon.expr;
import net.sf.saxon.value.SequenceType;

/**
* Treat Expression: implements "treat as data-type ( expression )". This is a factory class only.
*/

public abstract class TreatExpression {

    /**
     * This class is never instantiated
     */
    private TreatExpression() {
    }

    /**
    * Make a treat expression with error code XPDY0050
    * @param sequence the expression whose result is to be checked
    * @param type the type against which the result is to be checked
    * @return the expression
    */

    public static Expression make(Expression sequence, SequenceType type) {
        return make(sequence, type, "XPDY0050");
    }

    /**
    * Make a treat expression with a non-standard error code
    * @param sequence the expression whose result is to be checked
    * @param type the type against which the result is to be checked
    * @param errorCode the error code to be returned if the check fails
    * @return the expression
    */

    public static Expression make(Expression sequence, SequenceType type, String errorCode) {
        RoleLocator role = new RoleLocator(RoleLocator.TYPE_OP, "treat as", 0);
        role.setErrorCode(errorCode);
        Expression e = CardinalityChecker.makeCardinalityChecker(sequence, type.getCardinality(), role);
        return new ItemChecker(e, type.getPrimaryType(), role);
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
