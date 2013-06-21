package net.sf.saxon.style;

import net.sf.saxon.expr.Expression;
import net.sf.saxon.trans.XPathException;
import net.sf.saxon.type.ItemType;

/**
* An xsl:sort element in the stylesheet. <br>
*/

public class XSLSort extends XSLSortOrMergeKey {



    public void validate(Declaration decl) throws XPathException {
       
    	super.validate(decl);
        stable      = typeCheck("stable", stable);
        sortKeyDefinition.setStable(stable);
       
    }

    /**
     * Determine the type of item returned by this instruction (only relevant if
     * it is an instruction). Default implementation returns Type.ITEM, indicating
     * that we don't know, it might be anything. Returns null in the case of an element
     * such as xsl:sort or xsl:variable that can appear in a sequence constructor but
     * contributes nothing to the result sequence.
     * @return the item type returned
     */

    /*@Nullable*/ protected ItemType getReturnedItemType() {
        return null;
    }


    public Expression getStable() {
        return stable;
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