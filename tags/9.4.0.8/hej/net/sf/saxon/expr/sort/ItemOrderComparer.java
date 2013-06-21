package net.sf.saxon.expr.sort;

import net.sf.saxon.om.Item;

import java.io.Serializable;

/**
 * A Comparer used for comparing nodes in document order, or items in merge order
 *
 * @author Michael H. Kay
 *
 */

public interface ItemOrderComparer extends Serializable {

    /**
    * Compare two objects.
    * @return <0 if a<b, 0 if a=b, >0 if a>b
    */

    public int compare(Item a, Item b);

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