////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
// Copyright (c) 2014 Saxonica Limited.
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

    public SymbolicName(int kind, StructuredQName name) {
        this.kind = kind;
        this.name = name;
    }

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

    public int getComponentKind() {
        return kind;
    }

    public StructuredQName getComponentName() {
        return name;
    }

    public int getArity() {
        return arity;
    }

    public String toString() {
        return StandardNames.getLocalName(kind) + " " +
                name.getDisplayName() +
                (arity == -1 ? "" : ("#" + arity));
    }
}

