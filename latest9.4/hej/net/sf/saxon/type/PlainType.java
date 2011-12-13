package net.sf.saxon.type;

import java.util.Set;

/**
 * A "plain type" is either an atomic type, or a union type that (a) imposes no restrictions other
 * than those imposed by its member types, and (b) has exclusively plain types as its member types
 */

public interface PlainType extends ItemType {

    public boolean isExternalType();

    /**
     * Get the set of plain types that are subsumed by this type
     * @return for an atomic type, the type itself; for a plain union type, the set of plain types
     * in its transitive membership
     */

    public Set<PlainType> getPlainMemberTypes();
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