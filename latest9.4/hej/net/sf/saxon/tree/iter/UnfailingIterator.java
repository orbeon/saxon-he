package net.sf.saxon.tree.iter;

import net.sf.saxon.om.Item;
import net.sf.saxon.om.SequenceIterator;


/**
 * A SequenceIterator is used to iterate over a sequence. An UnfailingIterator
 * is a SequenceIterator that throws no checked exceptions.
 */

public interface UnfailingIterator<T extends Item> extends SequenceIterator<T> {

    /**
     * Get the next item in the sequence. <BR>
     * @return the next Item. If there are no more nodes, return null.
     */

    /*@Nullable*/ T next();

    /**
     * Get the current item in the sequence.
     *
     * @return the current item, that is, the item most recently returned by
     *     next()
     */

    /*@Nullable*/ T current();

    /**
     * Get the current position
     *
     * @return the position of the current item (the item most recently
     *     returned by next()), starting at 1 for the first node
     */

    int position();

    /**
     * Get another iterator over the same sequence of items, positioned at the
     * start of the sequence. It must be possible to call this method at any time, whether
     * none, some, or all of the items in the original iterator have been read. The method
     * is non-destructive: it does not change the state of the original iterator.
     * @return a new iterator over the same sequence
     */

    /*@NotNull*/
    UnfailingIterator<T> getAnother();

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