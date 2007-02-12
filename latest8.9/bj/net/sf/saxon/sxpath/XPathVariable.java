package net.sf.saxon.sxpath;
import net.sf.saxon.Configuration;
import net.sf.saxon.expr.Binding;
import net.sf.saxon.expr.BindingReference;
import net.sf.saxon.expr.VariableDeclaration;
import net.sf.saxon.expr.XPathContext;
import net.sf.saxon.om.ValueRepresentation;
import net.sf.saxon.value.QNameValue;
import net.sf.saxon.value.SequenceType;

import java.io.Serializable;


/**
 * An object representing an XPath variable for use in the standalone XPath API. The object
 * can only be created by calling the declareVariable method of class StandaloneContext.
 * Note that once declared, this object is thread-safe: it does not hold the actual variable
 * value, which means it can be used with any number of evaluations of a given XPath expression,
 * in series or in parallel.
*/

public final class XPathVariable implements VariableDeclaration, Binding, Serializable {

    private QNameValue name;
    private Configuration config;
    private int slotNumber;

    /**
    * Private constructor: for use only by the protected factory method make()
    */

    private XPathVariable() {};

    /**
    * Factory method, for use by the declareVariable method of class IndependentContext
    */

    protected static XPathVariable make(QNameValue name, Configuration config) {
        XPathVariable v = new XPathVariable();
        v.name = name;
        v.config = config;
        return v;
    }

    /**
     * Indicate whether the binding is local or global. A global binding is one that has a fixed
     * value for the life of a query or transformation; any other binding is local.
     */

    public boolean isGlobal() {
        return true;
    }

    /**
    * Test whether it is permitted to assign to the variable using the saxon:assign
    * extension element. This will only be for an XSLT global variable where the extra
    * attribute saxon:assignable="yes" is present.
    */

    public final boolean isAssignable() {
        return false;
    }

    /**
     * Set the slot number allocated to this variable
     * @param slotNumber
     */

    public void setSlotNumber(int slotNumber) {
        this.slotNumber = slotNumber;
    }

    /**
     * If this is a local variable held on the local stack frame, return the corresponding slot number.
     * In other cases, return -1.
     */

    public int getLocalSlotNumber() {
        return slotNumber;
    }

    /**
     * Get the name of the variable as a QNameValue.
     * @return the name of the variable, as a QNameValue
     */

    public QNameValue getVariableQName() {
        return name;
    }

    /**
     * Get the name of the variable. Used for diagnostic purposes only.
     * @return the name of the variable, as a string (containing the raw QName)
     */

    public String getVariableName() {
        return name.toString();
    }

    /**
     * Establish the nameCode of the name of this variable.
     * @return the nameCode
     */

    public int getNameCode() {
        return name.allocateNameCode(config.getNamePool());
    }

    /**
    * Method called by the XPath expression parser to register a reference to this variable.
    * This method should not be called by users of the API.
    */

    public void registerReference(BindingReference ref) {
        ref.setStaticType(SequenceType.ANY_SEQUENCE, null, 0);
        ref.fixup(this);
    }

    /**
     * Get the value of the variable. This method is used by the XPath execution engine
     * to retrieve the value. Note that the value is not held within the variable itself,
     * but within the dunamic context.
     * @param context    The dynamic evaluation context
     * @return           The value of the variable
     */

    public ValueRepresentation evaluateVariable(XPathContext context) {
        return context.evaluateLocalVariable(slotNumber);
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
