package net.sf.saxon.expr;

import net.sf.saxon.om.StructuredQName;
import net.sf.saxon.om.ValueRepresentation;
import net.sf.saxon.trans.XPathException;
import net.sf.saxon.value.Int64Value;
import net.sf.saxon.value.IntegerValue;
import net.sf.saxon.value.SequenceType;

import java.io.Serializable;

/**
 * Represents the defining occurrence of the position variable in a for expression
 * within an expression, for example the $p in "for $x at $p in ...".
 */

public class PositionVariable implements Binding, Serializable {

    private StructuredQName variableName;
    private int slotNumber = -999;

    /**
     * Create a RangeVariable
     */

    public PositionVariable(){}

    /**
     * Get the name of the variable, as a namepool name code
     * @return the nameCode
     */

    public StructuredQName getVariableQName() {
        return variableName;
    }

    public void addReference(boolean isLoopingReference) {

    }

    /**
     * Get the required type (declared type) of the variable
     * @return the required type
     */

    public SequenceType getRequiredType() {
        return SequenceType.SINGLE_INTEGER;
    }

    /**
     * If the variable is bound to an integer, get the minimum and maximum possible values.
     * Return null if unknown or not applicable
     */
    public IntegerValue[] getIntegerBoundsForVariable() {
        return new IntegerValue[]{Int64Value.PLUS_ONE, Expression.MAX_SEQUENCE_LENGTH};
    }

    /**
     * Set the name of the variable
     * @param variableName the name of the variable
     */

    public void setVariableQName(StructuredQName variableName) {
        this.variableName = variableName;
    }

    /**
    * Set the slot number for the range variable
     * @param nr the slot number to be used
    */

    public void setSlotNumber(int nr) {
        slotNumber = nr;
    }

    /**
     * If this is a local variable held on the local stack frame, return the corresponding slot number.
     * In other cases, return -1.
     */

    public int getLocalSlotNumber() {
        return slotNumber;
    }

    /**
    * Get the value of the range variable
    */

    public ValueRepresentation evaluateVariable(XPathContext context) throws XPathException {
        return context.evaluateLocalVariable(slotNumber);
    }

    /**
     * Test whether it is permitted to assign to the variable using the saxon:assign
     * extension element. This will only be for an XSLT global variable where the extra
     * attribute saxon:assignable="yes" is present.
     *
     * @return true if the binding is assignable
     */

    public boolean isAssignable() {
        return false;
    }

    /**
     * Indicate whether the binding is local or global. A global binding is one that has a fixed
     * value for the life of a query or transformation; any other binding is local.
     *
     * @return true if the binding is global
     */

    public boolean isGlobal() {
        return false;
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