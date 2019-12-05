////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
// Copyright (c) 2018 Saxonica Limited.
// This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
// If a copy of the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
// This Source Code Form is "Incompatible With Secondary Licenses", as defined by the Mozilla Public License, v. 2.0.
////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

package net.sf.saxon.ma.map;

import net.sf.saxon.type.FunctionItemType;
import net.sf.saxon.value.SequenceType;

/**
 * An instance of this class represents a specific tuple item type, for example
 * tuple(x: xs:double, y: element(employee)).
 *
 * Tuple types are a Saxon extension introduced in Saxon 9.8. The syntax for constructing
 * a tuple type requires Saxon-PE or higher, but the supporting code is included in
 * Saxon-HE for convenience.
 */
public interface TupleType extends FunctionItemType {

    /**
     * Get the names of all the fields
     * @return the names of the fields (in arbitrary order)
     */

    Iterable<String> getFieldNames();

    /**
     * Get the type of a given field
     * @param field the name of the field
     * @return the type of the field if it is defined, or null otherwise
     */

    SequenceType getFieldType(String field);

    /**
     * Ask whether a particular field is optional (that is, allowed to be absent from the map)
     * @param field the name of the field
     * @return true if the field is optional
     */

    boolean isOptional(String field);

    /**
     * Ask whether the tuple type is extensible, that is, whether fields other than those named are permitted
     * @return true if fields other than the named fields are permitted to appear
     */

    boolean isExtensible();


}

// Copyright (c) 2018 Saxonica Limited.