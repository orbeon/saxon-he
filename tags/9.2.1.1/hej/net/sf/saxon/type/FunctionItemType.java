package net.sf.saxon.type;

import net.sf.saxon.expr.Expression;
import net.sf.saxon.expr.ExpressionVisitor;
import net.sf.saxon.expr.RoleLocator;
import net.sf.saxon.trans.XPathException;

/**
 * Higher-order functions in XQuery 1.1 introduce a third kind of Item, namely a Function Item.
 * This type is represented here by a placeholder interfaces. The implementation of this type
 * is found only in Saxon-EE
 */

public interface FunctionItemType extends ItemType {

    /**
     * Determine the relationship of one function item type to another
     * @return for example {@link TypeHierarchy#SUBSUMES}, {@link TypeHierarchy#SAME_TYPE}
     */

    public int relationship(FunctionItemType other, TypeHierarchy th);

    /**
     * Create an expression whose effect is to apply function coercion to coerce a function to this function type
     * @param exp the expression that delivers the supplied sequence of function items (the ones in need of coercion)
     * @param role information for use in diagnostics
     * @param visitor the expression visitor, supplies context information
     * @return the coerced function, a function that calls the original function after checking the parameters
     */

    public Expression makeFunctionSequenceCoercer(Expression exp, RoleLocator role, ExpressionVisitor visitor)
    throws XPathException;
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

