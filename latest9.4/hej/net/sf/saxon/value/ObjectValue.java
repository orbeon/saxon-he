package net.sf.saxon.value;

import net.sf.saxon.Configuration;
import net.sf.saxon.expr.XPathContext;
import net.sf.saxon.lib.StringCollator;
import net.sf.saxon.trans.XPathException;
import net.sf.saxon.type.*;


/**
* An XPath value that encapsulates a Java object. Such a value can only be obtained by
* calling an extension function that returns it.
*/

public class ObjectValue extends AtomicValue {

    private Object value;
    /*@NotNull*/ private final static ExternalObjectType objectType = new ExternalObjectType(Object.class);

    /**
     * Default constructor for use in subclasses
     */

    public ObjectValue() {
        typeLabel = objectType;
    }

    /**
    * Constructor
    * @param object the object to be encapsulated
    */

    public ObjectValue(/*@NotNull*/ Object object) {
        value = object;
        typeLabel = new ExternalObjectType(object.getClass());
    }

    /**
    * Constructor
    * @param object the object to be encapsulated
    * @param type the type of the external object
    */

    public ObjectValue(Object object, ExternalObjectType type) {
        value = object;
        typeLabel = type;
    }


    /**
     * Set the value in this object value
     * @param value the external value to be wrapped
     */

    public void setValue(/*@NotNull*/ Object value) {
        this.value = value;
        typeLabel = new ExternalObjectType(value.getClass());
    }

    /**
     * Create a copy of this atomic value, with a different type label
     *
     * @param typeLabel the type label of the new copy. The caller is responsible for checking that
     *                  the value actually conforms to this type.
     */

    /*@NotNull*/ public AtomicValue copyAsSubType(AtomicType typeLabel) {
        ObjectValue v = new ObjectValue(value);
        v.typeLabel = typeLabel;
        return v;
    }

    /**
     * Determine the primitive type of the value. This delivers the same answer as
     * getItemType().getPrimitiveItemType(). The primitive types are
     * the 19 primitive types of XML Schema, plus xs:integer, xs:dayTimeDuration and xs:yearMonthDuration,
     * and xs:untypedAtomic. For external objects, the result is AnyAtomicType.
     */

    /*@NotNull*/ public BuiltInAtomicType getPrimitiveType() {
        return BuiltInAtomicType.ANY_ATOMIC;
    }

    /**
     * Determine the data type of the items in the expression, if possible
     *
     * @param th The TypeHierarchy.
     * @return for the default implementation: AnyItemType (not known)
     */

    /*@NotNull*/
    public ItemType getItemType(/*@Nullable*/ TypeHierarchy th) {
        if (typeLabel.equals(BuiltInAtomicType.ANY_ATOMIC)) {
            if (th == null) {
                return AnyItemType.getInstance();
            }
            Configuration config = th.getConfiguration();
            typeLabel = new ExternalObjectType(value.getClass(), config);
        }
        return typeLabel;
    }

    /**
     * Display the type name for use in error messages
     * @return the type name
     */

    /*@NotNull*/ public String displayTypeName() {
        return "java-type:" + value.getClass().getName();
    }

    /**
    * Get the value as a String
    * @return a String representation of the value
    */

    /*@NotNull*/ public CharSequence getPrimitiveStringValue() {
        return (value==null ? "" : value.toString());
    }

    /**
     * Get the effective boolean value of the value
     *
     * @return true, unless the value is boolean false, numeric zero, or
     *         zero-length string
     */
    public boolean effectiveBooleanValue() throws XPathException {
        return value != null;
    }

    /**
     * Get the encapsulated object
     * @return the Java object that this external object wraps
    */

    public Object getObject() {
        return value;
    }


    /*@NotNull*/ public Comparable getSchemaComparable() {
        throw new UnsupportedOperationException("External objects cannot be compared according to XML Schema rules");
    }


    /**
     * Get an object value that implements the XPath equality and ordering comparison semantics for this value.
     * If the ordered parameter is set to true, the result will be a Comparable and will support a compareTo()
     * method with the semantics of the XPath lt/gt operator, provided that the other operand is also obtained
     * using the getXPathComparable() method. In all cases the result will support equals() and hashCode() methods
     * that support the semantics of the XPath eq operator, again provided that the other operand is also obtained
     * using the getXPathComparable() method. A context argument is supplied for use in cases where the comparison
     * semantics are context-sensitive, for example where they depend on the implicit timezone or the default
     * collation.
     *
     * @param ordered true if an ordered comparison is required. In this case the result is null if the
     *                type is unordered; in other cases the returned value will be a Comparable.
     * @param collator the string collation in use
     * @param context the XPath dynamic evaluation context, used in cases where the comparison is context
*                sensitive @return an Object whose equals() and hashCode() methods implement the XPath comparison semantics
     */

    /*@Nullable*/ public Object getXPathComparable(boolean ordered, StringCollator collator, XPathContext context) {
        return (ordered ? null : this);
    }

    /**
    * Determine if two ObjectValues are equal
    * @throws ClassCastException if they are not comparable
    */

    public boolean equals(/*@NotNull*/ Object other) {
        return other instanceof ObjectValue && value.equals(((ObjectValue)other).value);
    }

    public int hashCode() {
        return value.hashCode();
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