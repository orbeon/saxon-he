////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
// Copyright (c) 2014 Saxonica Limited.
// This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
// If a copy of the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
// This Source Code Form is "Incompatible With Secondary Licenses", as defined by the Mozilla Public License, v. 2.0.
////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

package net.sf.saxon.expr;

import net.sf.saxon.expr.instruct.GlobalVariable;
import net.sf.saxon.om.Item;
import net.sf.saxon.om.Sequence;
import net.sf.saxon.om.StandardNames;
import net.sf.saxon.trans.SymbolicName;
import net.sf.saxon.trans.Visibility;
import net.sf.saxon.trans.XPathException;

/**
 * A reference to a global variable
 */
public class GlobalVariableReference extends VariableReference implements ComponentInvocation {

    int bindingSlot = -1;

    public GlobalVariableReference() {
        super();
    }

    public GlobalVariableReference(GlobalVariable var) {
        super(var);
    }


        /*@NotNull*/
    public Expression copy() {
        if (binding == null) {
            //System.err.println("copy unbound variable " + this);
            throw new UnsupportedOperationException("Cannot copy a variable reference whose binding is unknown");
        }
        GlobalVariableReference ref = new GlobalVariableReference();
        ref.copyFrom(this);
        return ref;
    }

    /**
     * Set the binding slot to be used. This is the offset within the binding vector of the containing
     * component where the actual target component is to be found. The target template is not held directly
     * in the invocation instruction/expression itself because it can be overridden in a using package.
     *
     * @param slot the offset in the binding vector of the containing package where the target component
     *             can be found.
     */
    public void setBindingSlot(int slot) {
        bindingSlot = slot;
    }

    /**
     * Get the binding slot to be used. This is the offset within the binding vector of the containing
     * component where the actual target component is to be found.
     *
     * @return the offset in the binding vector of the containing package where the target component
     *         can be found.
     */
    public int getBindingSlot() {
        return bindingSlot;
    }

    /**
     * Get the symbolic name of the component that this invocation references
     *
     * @return the symbolic name of the target component
     */
    public SymbolicName getSymbolicName() {
        return new SymbolicName(StandardNames.XSL_VARIABLE, binding.getVariableQName());
    }

    public Component getTarget() {
        return ((GlobalVariable)binding).getDeclaringComponent();
    }

    /**
     * Evaluate this variable
     *
     * @param c the XPath dynamic context
     * @return the value of the variable
     * @throws net.sf.saxon.trans.XPathException
     *          if any error occurs
     */
    @Override
    public Sequence evaluateVariable(XPathContext c) throws XPathException {

        if (bindingSlot >= 0) {
            Component target = c.getTargetComponent(bindingSlot);
            if (target.getVisibility() == Visibility.ABSTRACT) {
                // TODO: review whether this should be a static error
                XPathException err = new XPathException("Cannot evaluate an abstract variable", "XTSE3080");
                err.setLocator(this);
                throw err;
            }
            GlobalVariable p = (GlobalVariable)target.getProcedure();
            return p.evaluateVariable(c, target);
        } else {
            // non-packaging code (obsolescent, but needed for example in XQuery)
            GlobalVariable b = (GlobalVariable)binding;
            return b.evaluateVariable(c, b.getDeclaringComponent());
        }

    }
}
