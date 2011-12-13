package net.sf.saxon.dotnet;

import net.sf.saxon.type.AnyItemType;
import net.sf.saxon.type.ItemType;
import net.sf.saxon.type.TypeHierarchy;
import net.sf.saxon.value.ObjectValue;


/**
* An XPath value that encapsulates a .NET object. Such a value can only be obtained by
* calling an extension function that returns it.
*/

public class DotNetObjectValue extends ObjectValue {

    public DotNetObjectValue(Object value) {
        super(value);
    }

    /**
    * Determine the data type of the expression
    * @return Type.OBJECT
     * @param th the type hierarchy cache
     */

    /*@NotNull*/
    public ItemType getItemType(/*@Nullable*/ TypeHierarchy th) {
        if (th == null) {
            return AnyItemType.getInstance();
        }
        return new DotNetExternalObjectType(((cli.System.Object)getObject()).GetType(), th.getConfiguration());
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
