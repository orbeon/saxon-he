////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
// Copyright (c) 2015 Saxonica Limited.
// This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
// If a copy of the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
// This Source Code Form is "Incompatible With Secondary Licenses", as defined by the Mozilla Public License, v. 2.0.
////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

package net.sf.saxon.type;

import net.sf.saxon.expr.Expression;
import net.sf.saxon.expr.parser.ExpressionVisitor;
import net.sf.saxon.expr.parser.RoleDiagnostic;
import net.sf.saxon.trans.XPathException;
import net.sf.saxon.value.SequenceType;

/**
 * Higher-order functions in XPath 3.0 introduce a third kind of Item, namely a Function Item.
 * This type is represented here by a placeholder interfaces. The implementation of this type
 * is found only in Saxon-EE
 */

public interface FunctionItemType extends ItemType {

    /**
     * Get the argument types of the function
     *
     * @return the argument types, as an array of SequenceTypes; or null if this is the generic function type
     *         function(*)
     */

    /*@Nullable*/
    SequenceType[] getArgumentTypes();

    /**
     * Get the result type of the function
     *
     * @return the result type, as a SequenceType
     */

    SequenceType getResultType();

    /**
     * Determine the relationship of one function item type to another
     *
     * @return for example {@link TypeHierarchy#SUBSUMES}, {@link TypeHierarchy#SAME_TYPE}
     */

    int relationship(FunctionItemType other, TypeHierarchy th);

    /**
     * Create an expression whose effect is to apply function coercion to coerce a function to this function type
     *
     * @param exp     the expression that delivers the supplied sequence of function items (the ones in need of coercion)
     * @param role    information for use in diagnostics
     * @return the coerced function, a function that calls the original function after checking the parameters
     */

    Expression makeFunctionSequenceCoercer(Expression exp, RoleDiagnostic role)
            throws XPathException;

    /**
     * Ask whether this function item type is a map type. In this case function coercion (to the map type)
     * will never succeed.
     *
     * @return true if this FunctionItemType is a map type
     */

    boolean isMapType();

    /**
     * Ask whether this function item type is an array type. In this case function coercion (to the array type)
     * will never succeed.
     *
     * @return true if this FunctionItemType is an array type
     */

    boolean isArrayType();
}

