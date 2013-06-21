package net.sf.saxon.expr;

import net.sf.saxon.om.Item;
import net.sf.saxon.om.SequenceIterator;
import net.sf.saxon.trans.XPathException;

/**
* MappingFunction is an interface that must be satisfied by an object passed to a
* MappingIterator. It represents an object which, given an Item, can return a
* SequenceIterator that delivers a sequence of zero or more Items.
 *
 * This is a generic interface, where F represents the type of the input to the mapping
 * function, and T represents the type of the result
*/

public interface MappingFunction<F extends Item, T extends Item> {

    /**
     * Map one item to a sequence.
     * @param item The item to be mapped.
     * @return one of the following: (a) a SequenceIterator over the sequence of items that the supplied input
     * item maps to, or (b) null if it maps to an empty sequence.
     * @throws XPathException if a dynamic error occurs
    */

    /*@Nullable*/ public SequenceIterator<T> map(F item) throws XPathException;

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