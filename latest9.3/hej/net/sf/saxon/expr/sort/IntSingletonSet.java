package net.sf.saxon.expr.sort;

/**
 * An immutable integer set containing a single integer
 */
public class IntSingletonSet implements IntSet {

    private int value;

    public IntSingletonSet(int value) {
        this.value = value;
    }

    public void clear() {
        throw new UnsupportedOperationException("IntSingletonSet is immutable");
    }

    public int size() {
        return 1;
    }

    public boolean isEmpty() {
        return false;
    }

    public boolean contains(int value) {
        return this.value == value;
    }

    public boolean remove(int value) {
        throw new UnsupportedOperationException("IntSingletonSet is immutable");
    }

    public boolean add(int value) {
        throw new UnsupportedOperationException("IntSingletonSet is immutable");
    }

    public IntIterator iterator() {
        return new IntIterator() {

            boolean gone = false;
            public boolean hasNext() {
                return !gone;
            }

            public int next() {
                gone = true;
                return value;
            }
        };
    }

    public boolean containsAll(IntSet other) {
        if (other.size() > 1) {
            return false;
        }
        IntIterator ii = other.iterator();
        while (ii.hasNext()) {
            if (value != ii.next()) {
                return false;
            }
        }
        return true;
    }
}

//
// The contents of this file are subject to the Mozilla Public License Version
// 1.0 (the "License");
// you may not use this file except in compliance with the License. You may
// obtain a copy of the
// License at http://www.mozilla.org/MPL/
//
// Software distributed under the License is distributed on an "AS IS" basis,
// WITHOUT WARRANTY OF ANY KIND, either express or implied.
// See the License for the specific language governing rights and limitations
// under the License.
//
// The Original Code is: all this file.
//
// The Initial Developer of the Original Code is Michael Kay,
//
// Portions created by (your name) are Copyright (C) (your legal entity). All
// Rights Reserved.
//
// Contributor(s): none.
//



