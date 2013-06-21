package net.sf.saxon.functions;

import net.sf.saxon.expr.StaticProperty;
import net.sf.saxon.lib.NamespaceConstant;
import net.sf.saxon.om.StructuredQName;

/**
 * Implement the XSLT current() function
 */

public class Current extends CompileTimeFunction {
    
    /**
     * The name of the Current function
     */ 
    
    /*@NotNull*/ public static final StructuredQName FN_CURRENT =
            new StructuredQName("", NamespaceConstant.FN, "current");

    /**
    * Get the static properties of this expression (other than its type). The result is
    * bit-signficant. These properties are used for optimizations. In general, if
    * property bit is set, it is true, but if it is unset, the value is unknown.
     */

    public int computeSpecialProperties() {
        return StaticProperty.CONTEXT_DOCUMENT_NODESET |
                StaticProperty.SINGLE_DOCUMENT_NODESET |
                StaticProperty.ORDERED_NODESET |
                StaticProperty.NON_CREATIVE;
    }

    /**
    * Determine the dependencies
    */

    public int getIntrinsicDependencies() {
        return StaticProperty.DEPENDS_ON_CURRENT_ITEM | StaticProperty.DEPENDS_ON_LOCAL_VARIABLES;
        // the expression will be replaced by a local variable, so record the dependency now
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