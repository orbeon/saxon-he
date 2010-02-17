package net.sf.saxon.style;

import net.sf.saxon.expr.*;
import net.sf.saxon.instruct.GeneralVariable;
import net.sf.saxon.instruct.GlobalVariable;
import net.sf.saxon.instruct.SlotManager;
import net.sf.saxon.pattern.AnyNodeTest;
import net.sf.saxon.trans.XPathException;
import net.sf.saxon.type.TypeHierarchy;
import net.sf.saxon.value.SequenceType;
import net.sf.saxon.value.Value;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;


/**
* Generic class for xsl:variable and xsl:param elements. <br>
*/

public abstract class XSLVariableDeclaration
        extends XSLGeneralVariable
        implements VariableDeclaration, StylesheetProcedure {

    // The slot number for the variable is allocated at this level (a) for global variables, and
    // (b) for local parameters. For local variables, slot numbers are allocated only after an entire
    // template or function has been compiled.

    private int slotNumber = -9876;  // initial value designed solely to show up when debugging

    // List of VariableReference objects that reference this XSLVariableDeclaration
    protected List references = new ArrayList(10);

    /**
     * Get the SlotManager associated with this stylesheet construct. The SlotManager contains the
     * information needed to manage the local stack frames used by run-time instances of the code.
     * @return the associated SlotManager object
     */

    public SlotManager getSlotManager() {
        return slotManager;
    }

    /**
     * Get the slot number allocated to this variable (its position in the stackframe)
     * @return the allocated slot number
     */

    public int getSlotNumber() {
        return slotNumber;
    }

    /**
     * Allocate a slot number to this variable
     * @param slot the position of the variable on the local stack frame
     */

    public void setSlotNumber(int slot) {
        slotNumber = slot;
    }

    /**
     * Get the static type of the variable.
     * @return the static type declared for the variable
    */

    public abstract SequenceType getRequiredType();

    /**
    * Method called by VariableReference to register the variable reference for
    * subsequent fixup
    */

    public void registerReference(BindingReference ref) {
        references.add(ref);
    }

    /**
    * Determine whether this node is an instruction.
    * @return true - it is an instruction (well, it can be, anyway)
    */

    public boolean isInstruction() {
        return true;
    }

    /**
    * Notify all references to this variable of the data type
    */

    public void fixupReferences() throws XPathException {
        final SequenceType type = getRequiredType();
        final TypeHierarchy th = getConfiguration().getTypeHierarchy();
        final Iterator iter = references.iterator();
        while (iter.hasNext()) {
            Value constantValue = null;
            int properties = 0;
            if (this instanceof XSLVariable && !isAssignable()) {
                if (select instanceof Literal) {
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
            }
            ((BindingReference)iter.next()).setStaticType(type, constantValue, properties);
        }
        super.fixupReferences();
    }

    /**
    * Check that the variable is not already declared, and allocate a slot number
    */

    public void validate() throws XPathException {
        super.validate();
        if (global) {
            if (!redundant) {
                slotNumber = getPrincipalStylesheet().allocateGlobalSlot(getVariableQName());
            }
        } 
    }

    /**
     * Notify all variable references of the Binding instruction
     * @param binding the Binding that represents this variable declaration in the executable code tree
    */

    protected void fixupBinding(Binding binding) {
        Iterator iter = references.iterator();
        while (iter.hasNext()) {
            ((BindingReference)iter.next()).fixup(binding);
        }
    }

    /**
     * Set the number of references to this variable. This code is invoked only for a global variable,
     * and only if there is at least one reference.
     * @param var the variable
     */

    protected void setReferenceCount(GeneralVariable var) {
        var.setReferenceCount(10);  // TODO: temporary
    }

    protected void index(XSLStylesheet top) throws XPathException {
        top.indexVariableDeclaration(this);
    }

    /**
     * Optimize the stylesheet construct
     */

    public void optimize() throws XPathException {
        if (global && !redundant && select!=null) {
            Expression exp2 = select;
            ExpressionVisitor visitor = makeExpressionVisitor();
            Optimizer opt = getConfiguration().getOptimizer();
            try {
                if (opt.getOptimizationLevel() != Optimizer.NO_OPTIMIZATION) {
                    exp2 = exp2.optimize(visitor, AnyNodeTest.getInstance());
                }

            } catch (XPathException err) {
                err.maybeSetLocation(this);
                compileError(err);
            }

            // Try to extract new global variables from the body of the variable declaration
            // (but don't extract the whole body!)
//            if (opt.getOptimizationLevel() != Optimizer.NO_OPTIMIZATION) {
//                exp2 = opt.promoteExpressionsToGlobal(exp2, visitor, true);
//            }
            // dropped because it doesn't seem to do much good - just splits up an expression
            // into lots of small global variables.

            allocateSlots(exp2);
            if (slotManager != null && slotManager.getNumberOfVariables() > 0) {
                ((GlobalVariable)compiledVariable).setContainsLocals(slotManager);
            }

            if (exp2 != select) {
                select = exp2;
                compiledVariable.setSelectExpression(select);
            }
        }
    }

    /**
     * Get the compiled variable
     * @return the compiled variable if it has been compiled, or null otherwise
     */

    public GeneralVariable getCompiledVariable() {
        return compiledVariable;
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
