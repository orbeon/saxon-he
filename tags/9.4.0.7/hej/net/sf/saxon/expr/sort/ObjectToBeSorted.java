package net.sf.saxon.expr.sort;

import net.sf.saxon.value.AtomicValue;

/**
 * This class represents a member of a sequence that is being sorted. The sequence may contain
 * items, tuples, groups, or anything else. An instance of this class holds the object itself, the
 * values of the sort keys, and the original position of the item in the input sequence (which is needed
 * to achieve stable sorting.)
 */
public class ObjectToBeSorted<T> {

    public T value;
    public AtomicValue[] sortKeyValues;
    public int originalPosition;

    public ObjectToBeSorted(int numberOfSortKeys) {
        sortKeyValues = new AtomicValue[numberOfSortKeys];
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
// The Original Code is: all this file
//
// The Initial Developer of the Original Code is Saxonica Limited.
// Portions created by ___ are Copyright (C) ___. All rights reserved.
//
// Contributor(s):
//
//
// Contributor(s): none.
//



