package net.sf.saxon.trace;

import java.util.HashMap;
import java.util.Map;

/**
 * Internal class used for instrumentation purposes. It maintains a number of counters and displays these at the
 * end of the run.
 */

public class Instrumentation {

    public static final boolean ACTIVE = false;

    public static HashMap<String, Integer> counters = new HashMap<String, Integer>();

    public static void count(String counter) {
        if (counters.containsKey(counter)) {
            counters.put(counter, counters.get(counter) + 1);
        } else {
            counters.put(counter, 1);
        }
    }

    public static void report() {
        if (ACTIVE && !counters.isEmpty()) {
            System.err.println("COUNTERS");
            for (Map.Entry<String, Integer> c : counters.entrySet()) {
                System.err.println(c.getKey() + " = " + c.getValue());
            }
        }
    }

    public static void reset() {
        counters.clear();
    }

}

