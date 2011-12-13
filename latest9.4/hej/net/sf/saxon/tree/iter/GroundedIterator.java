package net.sf.saxon.tree.iter;

import net.sf.saxon.om.GroundedValue;
import net.sf.saxon.om.Item;
import net.sf.saxon.om.SequenceIterator;
import net.sf.saxon.trans.XPathException;

/**
 * This interface is an extension to the SequenceIterator interface; it represents
 * a SequenceIterator that is based on an in-memory representation of a sequence,
 * and that is therefore capable of returned a SequenceValue containing all the items
 * in the sequence.
 */

public interface GroundedIterator<T extends Item> extends SequenceIterator<T> {

    /**
     * Return a GroundedValue containing all the items in the sequence returned by this
     * SequenceIterator. This should be an "in-memory" value, not a Closure.
     * @return the corresponding Value
     * @throws net.sf.saxon.trans.XPathException if an error occurs evaluating the sequence
     */

    public GroundedValue<T> materialize() throws XPathException;
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