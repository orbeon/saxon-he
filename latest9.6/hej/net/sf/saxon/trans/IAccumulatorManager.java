////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
// Copyright (c) 2014 Saxonica Limited.
// This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
// If a copy of the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
// This Source Code Form is "Incompatible With Secondary Licenses", as defined by the Mozilla Public License, v. 2.0.
////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

package net.sf.saxon.trans;

import net.sf.saxon.expr.instruct.ComponentBody;

/**
 * Definition of the class that manages accumulator functions. The concrete implementation class
 * is part of the Saxon-EE streaming module.
 */

public interface IAccumulatorManager {

    /**
     * Get all the accumulators
     */

    public Iterable<? extends ComponentBody> getAllAccumulators();
}

