////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
// Copyright (c) 2015 Saxonica Limited.
// This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
// If a copy of the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
// This Source Code Form is "Incompatible With Secondary Licenses", as defined by the Mozilla Public License, v. 2.0.
////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

package net.sf.saxon.trans;

import net.sf.saxon.om.StandardNames;
import net.sf.saxon.om.StructuredQName;

/**
 * The symbolic name of a component consists of the component kind (e.g. function, template, attribute set),
 * the QName acting as the name of the template, function etc, and in the case of functions, the arity
 */
public class SymbolicName {

    private int kind;
    private StructuredQName name;
    private int arity = -1;

    /**
     * Create a symbolic name for a component other than a function.
     * @param kind the component kind, for example {@link StandardNames#XSL_TEMPLATE}
     * @param name the QName that is the "name" of the component
     */

    public SymbolicName(int kind, StructuredQName name) {
        this.kind = kind;
        this.name = name;
    }

    /**
     * Create a symbolic name, typically for a function.
     * @param kind the component kind, typically {@link StandardNames#XSL_FUNCTION}
     * @param name the QName that is the "name" of the component
     * @param arity the number of arguments; should be -1 if the component is not a function
     */

    public SymbolicName(int kind, StructuredQName name, int arity) {
        this.kind = kind;
        this.name = name;
        this.arity = arity;
    }

    /**
     * Returns a hash code value for the object.
     */
    @Override
    public int hashCode() {
        return kind << 16 ^ arity ^ name.hashCode();
    }

    /**
     * Indicates whether some other object is "equal to" this one.
     */
    @Override
    public boolean equals(Object obj) {
        return obj instanceof SymbolicName
                && ((SymbolicName) obj).kind == this.kind
                && ((SymbolicName) obj).arity == this.arity
                && ((SymbolicName) obj).name.equals(this.name);
    }

    /**
     * Get the kind of component, for example {@link StandardNames#XSL_FUNCTION} or {@link StandardNames#XSL_VARIABLE}
     * @return the kind of component identified by this symbolic name
     */

    public int getComponentKind() {
        return kind;
    }

    /**
     * Get the QName part of the symbolic name of the component
     * @return the QName part of the name
     */

    public StructuredQName getComponentName() {
        return name;
    }

    /**
     * Get the arity, in the case of function components
     * @return in the case of a function, the arity, otherwise -1.
     */

    public int getArity() {
        return arity;
    }

    /**
     * Get the name as a string.
     * @return a string typically in the form "template p:my-template" or "function f:my-function#2"
     */

    public String toString() {
        //noinspection UnnecessaryParentheses
        return StandardNames.getLocalName(kind) + " " +
                name.getDisplayName() +
                (arity == -1 ? "" : ("#" + arity));
    }
}

