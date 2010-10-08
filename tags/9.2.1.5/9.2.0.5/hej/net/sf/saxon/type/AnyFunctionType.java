package net.sf.saxon.type;

import net.sf.saxon.Configuration;
import net.sf.saxon.expr.Expression;
import net.sf.saxon.expr.ExpressionVisitor;
import net.sf.saxon.expr.RoleLocator;
import net.sf.saxon.expr.StaticProperty;
import net.sf.saxon.om.FunctionItem;
import net.sf.saxon.om.Item;
import net.sf.saxon.om.NamePool;
import net.sf.saxon.trans.XPathException;
import net.sf.saxon.value.SequenceType;

/**
 * An ItemType representing the type function(). Subtypes represent function items with more specific
 * type signatures.
 */
public class AnyFunctionType implements FunctionItemType {

    public static AnyFunctionType ANY_FUNCTION = new AnyFunctionType();

    public static SequenceType SINGLE_FUNCTION =
            SequenceType.makeSequenceType(ANY_FUNCTION, StaticProperty.EXACTLY_ONE);

    /**
     * Get the singular instance of this type (Note however that subtypes of this type
     * may have any number of instances)
     */

    public static AnyFunctionType getInstance() {
        return ANY_FUNCTION;
    }

    /**
     * Determine whether this item type is atomic (that is, whether it can ONLY match
     * atomic values)
     * @return true if this is ANY_ATOMIC_TYPE or a subtype thereof
     */

    public boolean isAtomicType() {
        return false;
    }

    /**
     * Test whether a given item conforms to this type
     * @param item              The item to be tested
     * @param allowURIPromotion
     * @param config
     * @return true if the item is an instance of this type; false otherwise
     */

    public boolean matchesItem(Item item, boolean allowURIPromotion, Configuration config) {
        return (item instanceof FunctionItem);
    }

    /**
     * Get the type from which this item type is derived by restriction. This
     * is the supertype in the XPath type heirarchy, as distinct from the Schema
     * base type: this means that the supertype of xs:boolean is xs:anyAtomicType,
     * whose supertype is item() (rather than xs:anySimpleType).
     * <p/>
     * In fact the concept of "supertype" is not really well-defined, because the types
     * form a lattice rather than a hierarchy. The only real requirement on this function
     * is that it returns a type that strictly subsumes this type, ideally as narrowly
     * as possible.
     * @param th the type hierarchy cache
     * @return the supertype, or null if this type is item()
     */

    public ItemType getSuperType(TypeHierarchy th) {
        return AnyItemType.getInstance();
    }

    /**
     * Get the primitive item type corresponding to this item type. For item(),
     * this is Type.ITEM. For node(), it is Type.NODE. For specific node kinds,
     * it is the value representing the node kind, for example Type.ELEMENT.
     * For anyAtomicValue it is Type.ATOMIC_VALUE. For numeric it is Type.NUMBER.
     * For other atomic types it is the primitive type as defined in XML Schema,
     * except that integer, xs:dayTimeDuration, and xs:yearMonthDuration
     * are considered to be primitive types. For function items it is the singular
     * instance FunctionItemType.getInstance().
     */

    public ItemType getPrimitiveItemType() {
        return ANY_FUNCTION;
    }

    /**
     * Get the primitive type corresponding to this item type. For item(),
     * this is Type.ITEM. For node(), it is Type.NODE. For specific node kinds,
     * it is the value representing the node kind, for example Type.ELEMENT.
     * For anyAtomicValue it is Type.ATOMIC_VALUE. For numeric it is Type.NUMBER.
     * For other atomic types it is the primitive type as defined in XML Schema,
     * except that INTEGER is considered to be a primitive type.
     */

    public int getPrimitiveType() {
        return Type.FUNCTION;
    }

    /**
     * Produce a representation of this type name for use in error messages.
     * @param pool the name pool
     * @return a string representation of the type, in notation resembling but not necessarily
     *         identical to XPath syntax
     */

    public String toString(NamePool pool) {
        return "function()";
    }

    /**
     * Produce a representation of this type name for use in error messages.
     * @return a string representation of the type, in notation resembling but not necessarily
     *         identical to XPath syntax
     */

    public String toString() {
        return "function()";
    }

    /**
     * Get the item type of the atomic values that will be produced when an item
     * of this type is atomized
     * @return the item type of the atomic values that will be produced when an item
     *         of this type is atomized
     */

    public AtomicType getAtomizedItemType() {
        return null;
    }

    /**
     * Ask whether values of this type are atomizable
     * @return true unless it is known that these items will be elements with element-only
     *         content, in which case return false
     */

    public boolean isAtomizable() {
        return false;
    }

    /**
     * Determine the relationship of one function item type to another
     * @return for example {@link TypeHierarchy#SUBSUMES}, {@link TypeHierarchy#SAME_TYPE}
     */

    public int relationship(FunctionItemType other, TypeHierarchy th) {
        if (other == this) {
            return TypeHierarchy.SAME_TYPE;
        } else {
            return TypeHierarchy.SUBSUMES;
        }
    }

    /**
     * Create an expression whose effect is to apply function coercion to coerce a function from this type to another type
     * @param exp the expression that delivers the supplied sequence of function items (the ones in need of coercion)
     * @param role information for use in diagnostics
     * @param visitor the expression visitor, supplies context information
     * @return the coerced function, a function that calls the original function after checking the parameters
     */

    public Expression makeFunctionSequenceCoercer(Expression exp, RoleLocator role, ExpressionVisitor visitor)
    throws XPathException {
        return exp;
    }
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
// The Initial Developer of the Original Code is Michael Kay,
//
// Portions created by (your name) are Copyright (C) (your legal entity). All
// Rights Reserved.
//
// Contributor(s): none.
//
