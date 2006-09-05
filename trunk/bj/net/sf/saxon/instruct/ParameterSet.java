package net.sf.saxon.instruct;
import net.sf.saxon.om.ValueRepresentation;
import net.sf.saxon.value.Closure;
import net.sf.saxon.trans.XPathException;

/**
 * A ParameterSet is a set of parameters supplied when calling a template.
 * It is a collection of name-value pairs, the names being represented by numeric references
 * to the NamePool
 */

public class ParameterSet
{
	private int[] keys;
    private ValueRepresentation[] values;
    private int used = 0;

    public static ParameterSet EMPTY_PARAMETER_SET = new ParameterSet(0);

    /**
     * Create an empty parameter set
     */

    public ParameterSet() {
        this(10);
    }

    /**
     * Create a parameter set specifying the initial capacity
     */

    public ParameterSet(int capacity) {
        keys = new int[capacity];
        values = new ValueRepresentation[capacity];
    }

    /**
     * Create a parameter set as a copy of an existing parameter set
     */

    public ParameterSet(ParameterSet existing, int extra) {
        this(existing.used + extra);
        for (int i=0; i<existing.used; i++) {
            put(existing.keys[i], existing.values[i]);
        }
    }

    /**
     * Add a parameter to the ParameterSet
     *
     * @param fingerprint The fingerprint of the parameter name.
     * @param value The value of the parameter
     */

    public void put (int fingerprint, ValueRepresentation value) {
        for (int i=0; i<used; i++) {
            if (keys[i]==fingerprint) {
                values[i]=value;
                return;
            }
        }
        if (used+1 > keys.length) {
            int newlength = (used<=5 ? 10 : used*2);
        	int[] newkeys = new int[newlength];
            ValueRepresentation[] newvalues = new ValueRepresentation[newlength];
            System.arraycopy(values, 0, newvalues, 0, used);
            System.arraycopy(keys, 0, newkeys, 0, used);
            values = newvalues;
            keys = newkeys;
        }
        keys[used] = fingerprint;
        values[used++] = value;
    }

    /**
     * Get a parameter
     *
     * @param fingerprint The fingerprint of the name.
     * @return The value of the parameter, or null if not defined
     */

    public ValueRepresentation get (int fingerprint) {
        for (int i=0; i<used; i++) {
            if (keys[i]==fingerprint) {
                return values[i];
            }
        }
        return null;
    }

    /**
     * Clear all values
     */

    public void clear() {
        used = 0;
    }

    /**
     * If any values are non-memo closures, expand them
     */

    public void materializeValues() throws XPathException {
        for (int i=0; i<used; i++) {
            if (values[i] instanceof Closure) {
                values[i] = ((Closure)values[i]).reduce();
            }
        }
    }

}
//
// The contents of this file are subject to the Mozilla Public License Version 1.0 (the "License");
// you may not use this file except in compliance with the License. You may obtain a copy of the
// License at http://www.mozilla.org/MPL/
//
// Software distributed under the License is distributed on an "AS IS" basis,
// WITHOUT WARRANTY OF ANY KIND, either express or implied.
// See the License for the specific language governing rights and limitations under the License.
//
// The Original Code is: all this file.
//
// The Initial Developer of the Original Code is Michael H. Kay.
//
// Portions created by (your name) are Copyright (C) (your legal entity). All Rights Reserved.
//
// Contributor(s): none.
//