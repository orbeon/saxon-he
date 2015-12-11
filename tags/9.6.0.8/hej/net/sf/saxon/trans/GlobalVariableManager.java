////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
// Copyright (c) 2014 Saxonica Limited.
// This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
// If a copy of the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
// This Source Code Form is "Incompatible With Secondary Licenses", as defined by the Mozilla Public License, v. 2.0.
////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

package net.sf.saxon.trans;

import net.sf.saxon.expr.instruct.GlobalVariable;
import net.sf.saxon.om.StructuredQName;

import java.util.Collection;

/**
 * This interface provides access to a collection of global variables. This abstraction is used by the optimizer
 * to handle the rather different ways that global variables are managed in XSLT and XQuery, as a result of
 * XSLT packaging.
 */

public interface GlobalVariableManager {

    /**
     * Get the names of all global variables in the collection
     * @return the collections of global variables
     */

    public Collection<StructuredQName> getGlobalVariableNames();

    /**
     * Get the global variable with a specific name
     * @param name the name of the required global variable
     * @return the global variable in question
     */

    public GlobalVariable getGlobalVariable(StructuredQName name);

    /**
     * Add a global variable to the collection
     * @param variable the variable to be added
     * @throws XPathException if errors occur (some implementations of the method have side-effects,
     * such as causing the variable declaration to be compiled)
     */

    public void addGlobalVariable(GlobalVariable variable) throws XPathException;

}

// Copyright (c) 2014 Saxonica Limited. All rights reserved.

