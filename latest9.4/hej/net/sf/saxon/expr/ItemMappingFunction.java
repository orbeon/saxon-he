package net.sf.saxon.expr;

import net.sf.saxon.om.Item;
import net.sf.saxon.trans.XPathException;

/**
 * ItemMappingFunction is an interface that must be satisfied by an object passed to a
 * ItemMappingIterator. It represents an object which, given an Item, can return either
 * another Item, or null.
 * <p/>
 * The interface is generic, paramterized by F (from) the input item type, and T (to),
 * the output item type
 */

public interface ItemMappingFunction<F extends Item, T extends Item> {

    /**
     * Map one item to another item.
     *
     * @param item The input item to be mapped.
     * @return either the output item, or null.
     * @throws XPathException if a dynamic error occurs
     */

    /*@Nullable*/
    public T mapItem(F item) throws XPathException;

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