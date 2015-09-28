////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
// Copyright (c) 2014 Saxonica Limited.
// This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
// If a copy of the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
// This Source Code Form is “Incompatible With Secondary Licenses”, as defined by the Mozilla Public License, v. 2.0.
////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

package net.sf.saxon.xpath;

import net.sf.saxon.expr.LocalBinding;
import net.sf.saxon.expr.LocalVariableReference;
import net.sf.saxon.expr.XPathContext;
import net.sf.saxon.om.Sequence;
import net.sf.saxon.trans.XPathException;


public class JAXPVariableReference extends LocalVariableReference {

    public JAXPVariableReference(LocalBinding binding) {
        super(binding);
    }

    /**
     * Evaluate this variable
     *
     * @param c the XPath dynamic context
     * @return the value of the variable
     * @throws net.sf.saxon.trans.XPathException if any error occurs
     */

    /*@NotNull*/
    public Sequence evaluateVariable(XPathContext c) throws XPathException {
        try {
            return binding.evaluateVariable(c);
        } catch (NullPointerException err) {
            if (binding == null) {
                throw new IllegalStateException("Variable $" + getDisplayName() + " has not been resolved");
            } else {
                throw err;
            }
        }
    }

}

