package net.sf.saxon.expr;

import net.sf.saxon.value.SequenceType;
import net.sf.saxon.value.Value;

/**
* BindingReference is a interface used to mark references to a variable declaration. The main
* implementation is VariableReference, which represents a reference to a variable in an XPath
* expression, but it is also used to represent a reference to a variable in a saxon:assign instruction.
*/

public interface BindingReference  {

    /**
    * Fix up the static type of this variable reference; optionally, supply a constant value for
    * the variable. Also supplies other static properties of the expression to which the variable
    * is bound, for example whether it is an ordered node-set.
     * @param type The static type of the variable reference, typically either the declared type
     * of the variable, or the static type of the expression to which the variable is bound
     * @param constantValue if non-null, indicates that the value of the variable is known at compile
     * time, and supplies the value
     * @param properties static properties of the expression to which the variable is bound
     */

    public void setStaticType(SequenceType type, /*@Nullable*/ Value constantValue, int properties);

    /**
     * Fix up this binding reference to a binding
     * @param binding the Binding to which the variable refers
     */

    public void fixup(Binding binding);

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