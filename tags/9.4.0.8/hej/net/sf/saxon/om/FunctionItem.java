package net.sf.saxon.om;

import net.sf.saxon.expr.XPathContext;
import net.sf.saxon.expr.sort.GenericAtomicComparer;
import net.sf.saxon.trans.XPathException;
import net.sf.saxon.type.FunctionItemType;
import net.sf.saxon.type.TypeHierarchy;

/**
 * XDM 3.0 introduces a third kind of item, beyond nodes and atomic values: the function item. Function
 * items implement this marker interface. The actual implementation class is in Saxon-PE and Saxon-EE only.
 */

public interface FunctionItem<T extends FunctionItem> extends Item<T>, ValueRepresentation<T> {

    /**
     * Get the item type of the function item
     * @param th the type hierarchy cache
     * @return the function item's type
     */

    public FunctionItemType getFunctionItemType(TypeHierarchy th);

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
     * @throws XPathException if a dynamic error occurs within the function
     */

    public SequenceIterator<? extends Item> invoke(SequenceIterator<? extends Item>[] args, XPathContext context) throws XPathException;

    /**
     * Curry a function by binding one or more (but not all) of its arguments
     * @param values the values to which the arguments are to be bound, representing an unbound argument (a placeholder)
     * by null
     * @return a new function item in which the specified arguments of the original function are bound to a value
     * @throws XPathException if any dynamic error occurs
     */

    public FunctionItem curry(ValueRepresentation<? extends Item>[] values) throws XPathException;

    /**
     * Test whether this FunctionItem is deep-equal to another function item,
     * under the rules of the deep-equal function
     * @param other the other function item
     * @param context the dynamic evaluation context
     * @param comparer the object to perform the comparison
     * @param flags options for how the comparison is performed
     * @return true if the two function items are deep-equal
     * @throws XPathException if the comparison cannot be performed
     */

    public boolean deepEquals(FunctionItem other, XPathContext context, GenericAtomicComparer comparer, int flags) throws XPathException;

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