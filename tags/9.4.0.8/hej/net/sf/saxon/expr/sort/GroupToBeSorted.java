package net.sf.saxon.expr.sort;

import net.sf.saxon.om.Item;
import net.sf.saxon.om.SequenceIterator;
import net.sf.saxon.value.Value;

/**
 * This class is a specialization of ObjectToBeSorted for use when the sequence
 * being sorted is a sequence of groups. The group is represented by its initial
 * item, but the object holds in addition the value of the grouping key, and an
 * iterator over the items in the group.
 */
public class GroupToBeSorted extends ObjectToBeSorted<Item> {

    /*@Nullable*/ public Value currentGroupingKey;
    public SequenceIterator currentGroupIterator;

    public GroupToBeSorted(int numberOfSortKeys) {
        super(numberOfSortKeys);
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