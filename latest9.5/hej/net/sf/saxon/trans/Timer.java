////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
// Copyright (c) 2013 Saxonica Limited.
// This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
// If a copy of the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
// This Source Code Form is "Incompatible With Secondary Licenses", as defined by the Mozilla Public License, v. 2.0.
////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

package net.sf.saxon.trans;

/**
 * Utility class for collecting and reporting timing information, used only under diagnostic control
 */
public class Timer {

    private long start;
    private long prev;

    public Timer() {
        start = System.currentTimeMillis();
        prev = start;
    }

    public void report(String label) {
        long time = System.currentTimeMillis();
        System.err.println(label + " " + (time - prev) + "ms");
        prev = time;
    }

    public void reportCumulative(String label) {
        long time = System.currentTimeMillis();
        System.err.println(label + " " + (time - start) + "ms");
        prev = time;
    }
}
