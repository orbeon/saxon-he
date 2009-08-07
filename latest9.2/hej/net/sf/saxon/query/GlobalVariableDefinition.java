package net.sf.saxon.query;

import net.sf.saxon.expr.*;
import net.sf.saxon.instruct.Executable;
import net.sf.saxon.instruct.GlobalParam;
import net.sf.saxon.instruct.GlobalVariable;
import net.sf.saxon.instruct.SlotManager;
import net.sf.saxon.om.StructuredQName;
import net.sf.saxon.trace.ExpressionPresenter;
import net.sf.saxon.trans.XPathException;
import net.sf.saxon.type.AnyItemType;
import net.sf.saxon.type.ItemType;
import net.sf.saxon.type.TypeHierarchy;
import net.sf.saxon.value.SequenceType;
import net.sf.saxon.value.Value;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Class to hold compile-time information about an XQuery global variable
 * or parameter
 */

public class GlobalVariableDefinition implements VariableDeclaration, Declaration {

    protected List references = new ArrayList(10);
                                    // Note that variableReferences on this list might be dormant;
                                    // that is, they might be disconnected from the live expression tree.
    private SequenceType requiredType;
    private Expression value;
    private boolean isParameter;
    private StructuredQName variableName;
    private String systemId;        // identifies the module where the variable declaration occurred
    private int lineNumber;         // identifies the line number of the variable declaration
    private GlobalVariable compiledVar;

    /**
     * Set the required type of the variable
     * @param type the declared type, from the "as" clause if present
     */

    public void setRequiredType(SequenceType type) {
        requiredType = type;
    }

    /**
     * Get the required type of the variable
     * @return the declared type, from the "as" clause if present
     */

    public SequenceType getRequiredType() {
        return requiredType;
    }

    /**
     * Set the variable name
     * @param qName the variable name
     */
    public void setVariableQName(StructuredQName qName) {
        variableName = qName;
    }

    /**
     * Get the variable name
     * @return the variable name
     */
    public StructuredQName getVariableQName() {
        return variableName;
    }
    /**
     * Set the line number where the variable declaration appears in the source
     * @param lineNumber the line number
     */
    public void setLineNumber(int lineNumber) {
        this.lineNumber = lineNumber;
    }

    /**
     * Get the line number where the declaration appears
     */

    public int getLineNumber() {
        return lineNumber;
    }

    /**
     * Get column number
     * @return -1 always
     */

    public int getColumnNumber() {
        return -1;
    }

    /**
     * Get public identifier
     * @return null always
     */

    public String getPublicId() {
        return null;
    }

    /**
     * Set the system ID of the module where the variable declaration appears
     * @param systemId the System ID (base URI)
     */
    public void setSystemId(String systemId) {
        this.systemId = systemId;
    }

    /**
     * Get the system ID of the module containing the variable declaration
     * @return the System ID (base URI)
     */

    public String getSystemId() {
        return systemId;
    }

    /**
     * Set the expression used to define the value of the variable
     * @param val the initializing expression
     */

    public void setValueExpression(Expression val) {
        value = val;
    }

    /**
     * Get the expression used to initialize the value of the variable
     * @return the initializing expression
     */

    public Expression getValueExpression() {
        return value;
    }

    /**
     * Say whether this global variable is a "parameter" (an external variable, in XQuery terminology)
     * @param b true if this variable is external
     */
    public void setIsParameter(boolean b) {
        isParameter = b;
    }

    /**
     * Ask whether this global variable is a "parameter" (an external variable, in XQuery terminology)
     * @return true if this variable is an external variable
     */

    public boolean isParameter() {
        return isParameter;
    }

    /**
     * Register a variable reference that refers to this global variable
     * @param ref the variable reference
     */
    public void registerReference(BindingReference ref) {
        references.add(ref);
    }

    /**
     * Iterate over the references to this variable
     * @return an iterator over the references: returns objects of class {@link VariableReference}
     */

    public Iterator iterateReferences() {
        return references.iterator();
    }


    /**
     * Create a compiled representation of this global variable
     * @param exec the executable 
     * @param slot the slot number allocated to this variable
     * @return the compiled representation
     * @throws XPathException if compile-time errors are found.
     */

    public GlobalVariable compile(Executable exec, int slot) throws XPathException {

        TypeHierarchy th = exec.getConfiguration().getTypeHierarchy();
        GlobalVariable var;
        if (isParameter) {
            var = new GlobalParam();
            var.setExecutable(exec);
            var.setRequiredParam(value==null);
        } else {
            var = new GlobalVariable();
            var.setExecutable(exec);
        }

        var.setSelectExpression(value);
        var.setRequiredType(requiredType);
        var.setVariableQName(variableName);
        var.setSlotNumber(slot);

        int loc = exec.getLocationMap().allocateLocationId(systemId, lineNumber);
        var.setLocationId(loc);
        var.setContainer(var);

        final SequenceType type = getRequiredType();
        Iterator iter = references.iterator();
        while (iter.hasNext()) {
            BindingReference ref = (BindingReference)iter.next();
            ref.fixup(var);
            Value constantValue = null;
            int properties = 0;
            Expression select = value;
            if (select instanceof Literal && !isParameter) {
                // we can't rely on the constant value because it hasn't yet been type-checked,
                // which could change it (eg by numeric promotion). Rather than attempt all the type-checking
                // now, we do a quick check. See test bug64
                int relation = th.relationship(select.getItemType(th), type.getPrimaryType());
                if (relation == TypeHierarchy.SAME_TYPE || relation == TypeHierarchy.SUBSUMED_BY) {
                    constantValue = ((Literal)select).getValue();
                }
            }
            if (select != null) {
                properties = select.getSpecialProperties();
            }
            properties |= StaticProperty.NON_CREATIVE;
                // a variable reference is non-creative even if its initializer is creative
            ref.setStaticType(type, constantValue, properties);
        }
        exec.registerGlobalVariable(var);
        var.setReferenceCount(10); // TODO: temporary!
        compiledVar = var;
        return var;
    }


    /**
     * Type check the compiled representation of this global variable
     * @param visitor an expression visitor
     * @throws XPathException if compile-time errors are found.
     */

    public void typeCheck(ExpressionVisitor visitor) throws XPathException {
        GlobalVariable var = getCompiledVariable();
        Expression value = var.getSelectExpression();
        if (value != null) {
            value.checkForUpdatingSubexpressions();
            if (value.isUpdatingExpression()) {
                throw new XPathException(
                        "Initializing expression for global variable must not be an updating expression", "XUST0001");
            }
            value.setContainer(var);
            RoleLocator role = new RoleLocator(
                    RoleLocator.VARIABLE, var.getVariableQName(), 0);
            Expression value2 = TypeChecker.strictTypeCheck(
                    visitor.typeCheck(visitor.simplify(value), AnyItemType.getInstance()),
                    var.getRequiredType(), role, visitor.getStaticContext());
            value2 = value2.optimize(visitor, AnyItemType.getInstance());
            var.setSelectExpression(value2);
            value2.setContainer(var);
            // the value expression may declare local variables
            SlotManager map = visitor.getConfiguration().makeSlotManager();
            int slots = ExpressionTool.allocateSlots(value2, 0, map);
            if (slots > 0) {
                var.setContainsLocals(map);
            }

            if (var.getRequiredType() == SequenceType.ANY_SEQUENCE && !(var instanceof GlobalParam)) {
                // no type was declared; try to deduce a type from the value
                try {
                    final TypeHierarchy th = visitor.getConfiguration().getTypeHierarchy();
                    final ItemType itemType = value.getItemType(th);
                    final int cardinality = value.getCardinality();
                    var.setRequiredType(SequenceType.makeSequenceType(itemType, cardinality));
                    Value constantValue = null;
                    if (value2 instanceof Literal) {
                        constantValue = ((Literal)value2).getValue();
                    }
                    for (Iterator iter = references.iterator(); iter.hasNext(); ) {
                        BindingReference ref = ((BindingReference)iter.next());
                        if (ref instanceof VariableReference) {
                            ((VariableReference)ref).refineVariableType(
                                    itemType, cardinality, constantValue, value.getSpecialProperties(), visitor);
                        }
                    }
                } catch (Exception err) {
                    // exceptions can happen because references to variables and functions are still unbound
                }
            }


        }
    }

    /**
     * Get the compiled variable if the definition has been compiled
     * @return the compiled global variable
     */

    public GlobalVariable getCompiledVariable() {
        return compiledVar;
    }

    /**
     * Produce diagnostic output showing the compiled and optimized expression tree for a function
     * @param out the destination to be used
     */

    public void explain(ExpressionPresenter out) {
        out.startElement("declareVariable");
        out.emitAttribute("name", variableName.getDisplayName());
        if (value != null) {
            value.explain(out);
        }
        out.endElement();
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
// Contributor(s): none
//