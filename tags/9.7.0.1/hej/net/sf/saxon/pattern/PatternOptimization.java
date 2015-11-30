////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
// Copyright (c) 2015 Saxonica Limited.
// This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
// If a copy of the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
// This Source Code Form is "Incompatible With Secondary Licenses", as defined by the Mozilla Public License, v. 2.0.
////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

package net.sf.saxon.pattern;


import javax.xml.stream.XMLStreamWriter;

/**
 * Stub version of the PatternOptimizationEE class, which holds pattern optimization options
 * used in Saxon-EE
 */
public abstract class PatternOptimization {

    /**
     * Ask whether optimization is enabled
     * @return on this class, return false: no optimization is enabled in Saxon-HE
     */

    public boolean isOptimize() {
        return false;
    }

    /**
     * Write information about the pattern optimizer (may be used in statistics)
     * @param writer the stream to write to
     */
    public void write(XMLStreamWriter writer) {}
}
