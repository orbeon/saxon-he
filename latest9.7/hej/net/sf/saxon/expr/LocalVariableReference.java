////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
// Copyright (c) 2015 Saxonica Limited.
// This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
// If a copy of the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
// This Source Code Form is "Incompatible With Secondary Licenses", as defined by the Mozilla Public License, v. 2.0.
////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

package net.sf.saxon.expr;

import com.saxonica.ee.bytecode.ExpressionCompiler;
import com.saxonica.ee.bytecode.LocalVariableReferenceCompiler;
import net.sf.saxon.om.Sequence;
import net.sf.saxon.om.StructuredQName;
import net.sf.saxon.trans.XPathException;

/**
 * Variable reference: a reference to a local variable. This subclass of VariableReference
 * bypasses the Binding object to get the value directly from the relevant slot in the local
 * stackframe.
 */

public class LocalVariableReference extends VariableReference {

    int slotNumber = -999;

    /**
     * Create a local variable reference. The binding and slot number will be supplied later
     */

    public LocalVariableReference(StructuredQName name) {
        super(name);
    }

    /**
     * Create a LocalVariableReference bound to a given Binding
     *
     * @param binding the binding (that is, the declaration of this local variable)
     */

    public LocalVariableReference(LocalBinding binding) {
        super(binding);
    }

    /**
     * Create a clone copy of this VariableReference
     *
     * @return the cloned copy
     */

    /*@NotNull*/
    public Expression copy() {
        if (binding == null) {
            throw new UnsupportedOperationException("Cannot copy a variable reference whose binding is unknown");
        }
//        if (slotNumber < 0) {
//            // slot numbers have not yet been allocated. This is messy. For the time being, the safest
//            // thing is to reuse the existing variable reference, which usually works, rather than copying
//            // it, which will almost certainly fail. See XSLT streaming test case si-attribute-053
//            return this;
//        }
        LocalVariableReference ref = new LocalVariableReference(getVariableName());
        ref.copyFrom(this);
        ref.slotNumber = slotNumber;
        binding.addReference(ref, isInLoop());
        return ref;
    }

    @Override
    public LocalBinding getBinding() {
        return (LocalBinding)super.getBinding();
    }

    /**
     * Set the slot number for this local variable, that is, its position in the local stack frame
     *
     * @param slotNumber the slot number to be used
     */

    public void setSlotNumber(int slotNumber) {
        this.slotNumber = slotNumber;
    }

    /**
     * Get the slot number allocated to this local variable
     *
     * @return the slot number
     */

    public int getSlotNumber() {
        return slotNumber;
    }

    /**
     * Return the value of the variable
     *
     * @param c the XPath dynamic context
     * @return the value of the variable
     * @throws XPathException if any dynamic error occurs while evaluating the variable
     */

    /*@NotNull*/
    public Sequence evaluateVariable(XPathContext c) throws XPathException {
        try {
            return c.getStackFrame().slots[slotNumber];
        } catch (ArrayIndexOutOfBoundsException err) {
            if (slotNumber == -999) {
                if (binding != null) {
                    try {
                        slotNumber = getBinding().getLocalSlotNumber();
                        return c.getStackFrame().slots[slotNumber];
                    } catch (ArrayIndexOutOfBoundsException err2) {
                        // fall through
                    }
                }
                throw new ArrayIndexOutOfBoundsException("Local variable $" + getDisplayName() + " has not been allocated a stack frame slot");
            } else {
                int actual = c.getStackFrame().slots.length;
                throw new ArrayIndexOutOfBoundsException("Local variable $" + getDisplayName() + " uses slot "
                        + slotNumber + " but " + (actual == 0 ? "no" : "only " + c.getStackFrame().slots.length) + " slots" +
                        " are allocated on the stack frame");
            }
        }
    }

//#ifdefined BYTECODE

    /**
     * Return the compiler of the LocalVariableReference expression
     *
     * @return the relevant ExpressionCompiler
     */
    @Override
    public ExpressionCompiler getExpressionCompiler() {
        return new LocalVariableReferenceCompiler();
    }
//#endif

    /**
     * Replace this VariableReference where appropriate by a more efficient implementation. This
     * can only be done after all slot numbers are allocated. The efficiency is gained by binding the
     * VariableReference directly to a local or global slot, rather than going via the Binding object
     *
     * @param parent the parent expression of this variable reference
     */

//    public void refineVariableReference(Expression parent) {
//        // no-op
//    }
}