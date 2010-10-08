package net.sf.saxon.om;

import net.sf.saxon.expr.XPathContext;
import net.sf.saxon.trans.XPathException;
import net.sf.saxon.type.FunctionItemType;

import java.io.Serializable;

/**
 * XQuery 1.1 introduces a third kind of item, beyond nodes and atomic values: the function item. Function
 * items implement this marker interface. The actual implementation class is in Saxon-PE and Saxon-EE only.
 */

public interface FunctionItem extends Item, Serializable {

    /**
     * Get the item type of the function item
     * @return the function item's type
     */

    public FunctionItemType getFunctionItemType();

    /**
     * Get the name of the function, or null if it is anonymous
     * @return the function name, or null for an anonymous inline function
     */

    public StructuredQName getFunctionName();

    /**
     * Get the arity of the function
     * @return the number of arguments in the function signature
     */

    public int getArity();

    /**
     * Invoke the function
     * @param args the actual arguments to be supplied
     * @param context the XPath dynamic evaluation context
     * @return the result of invoking the function
     * @throws net.sf.saxon.trans.XPathException
     */

    public ValueRepresentation invoke(ValueRepresentation[] args, XPathContext context) throws XPathException;

    /**
     * Curry a function by binding one of its arguments
     * @param arg the argument to be found (1-based)
     * @param value the value to which the argument is to be bound
     */

    public FunctionItem curry(int arg, ValueRepresentation value) throws XPathException;
}

//
// The contents of this file are subject to the Mozilla Public License Version
// 1.0 (the "License");
// you may not use this file except in compliance with the License. You may
// obtain a copy of the
// License at http://www.mozilla.org/MPL/
//
// Software distributed under the License is distributed on an "AS IS" basis,
// WITHOUT WARRANTY OF ANY KIND, either express or implied.
// See the License for the specific language governing rights and limitations
// under the License.
//
// The Original Code is: all this file.
//
// The Initial Developer of the Original Code is Michael Kay
//
// Portions created by (your name) are Copyright (C) (your legal entity). All
// Rights Reserved.
//
// Contributor(s): none.
//

