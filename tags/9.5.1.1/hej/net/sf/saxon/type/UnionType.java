////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
// Copyright (c) 2013 Saxonica Limited.
// This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
// If a copy of the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
// This Source Code Form is "Incompatible With Secondary Licenses", as defined by the Mozilla Public License, v. 2.0.
////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

package net.sf.saxon.type;


import net.sf.saxon.value.SequenceType;

import java.util.Set;

public interface UnionType extends SimpleType, ItemType {

    public boolean containsListType();

    public Set<PlainType> getPlainMemberTypes();

    public SequenceType getResultTypeOfCast();

}

