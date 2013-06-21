////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
// Copyright (c) 2013 Saxonica Limited.
// This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
// If a copy of the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
// This Source Code Form is "Incompatible With Secondary Licenses", as defined by the Mozilla Public License, v. 2.0.
////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

package net.sf.saxon.type;

import java.util.Set;

/**
 * A "plain type" is either an atomic type, or a union type that (a) imposes no restrictions other
 * than those imposed by its member types, and (b) has exclusively plain types as its member types
 */

public interface PlainType extends ItemType {

    public boolean isExternalType();

    /**
     * Get the list of plain types that are subsumed by this type
     * @return for an atomic type, the type itself; for a plain union type, the list of plain types
     * in its transitive membership
     */

    public Set<PlainType> getPlainMemberTypes();
}

