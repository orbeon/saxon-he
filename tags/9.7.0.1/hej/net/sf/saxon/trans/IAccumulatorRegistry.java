////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
// Copyright (c) 2015 Saxonica Limited.
// This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
// If a copy of the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
// This Source Code Form is "Incompatible With Secondary Licenses", as defined by the Mozilla Public License, v. 2.0.
////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

package net.sf.saxon.trans;

import net.sf.saxon.expr.instruct.ComponentCode;

/**
 * Definition of the class that manages accumulators statically within a package. Generally accessed
 * via the PackageData. Subclassed for Saxon-PE and Saxon-EE.
 */

public interface IAccumulatorRegistry {

    /**
     * Get all the accumulators
     */

    Iterable<? extends ComponentCode> getAllAccumulators();

}

