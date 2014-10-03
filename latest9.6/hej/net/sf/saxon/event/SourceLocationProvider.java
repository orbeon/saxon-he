////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
// Copyright (c) 2014 Saxonica Limited.
// This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
// If a copy of the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
// This Source Code Form is "Incompatible With Secondary Licenses", as defined by the Mozilla Public License, v. 2.0.
////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

package net.sf.saxon.event;


/**
 * A SourceLocationProvider is a {@link LocationProvider} that represents locations
 * in the source document from which the events
 * are derived (as distinct from locations in a query or stylesheet of the instructions causing the
 * events)
 */

public interface SourceLocationProvider extends LocationProvider {

}
