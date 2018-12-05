package net.sf.saxon.trace;

import java.util.HashMap;
import java.util.Map;

/**
 * Internal class used for instrumentation purposes. It maintains a number of counters and displays these at the
 * end of the run. Currently implemented only for the Transform command line, with -t option set.
 */

public class Instrumentation {

    public static final boolean ACTIVE = true;  // KILROY

    public static HashMap<String, Integer> counters = new HashMap<>();

    public static void count(String counter) {
        if (counters.containsKey(counter)) {
            counters.put(counter, counters.get(counter) + 1);
        } else {
            counters.put(counter, 1);
        }
    }

    public static void count(String counter, int increment) {
        if (counters.containsKey(counter)) {
            counters.put(counter, counters.get(counter) + increment);
        } else {
            counters.put(counter, increment);
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

