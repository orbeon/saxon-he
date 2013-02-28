package net.sf.saxon.expr.flwor;

import net.sf.saxon.om.ValueRepresentation;
import net.sf.saxon.value.ObjectValue;

/**
 * A tuple, as it appears in an XQuery tuple stream handled by extended FLWOR expressions.
 */
public class Tuple extends ObjectValue {

    public Tuple(ValueRepresentation[] members) {
        super(members);
    }

    public ValueRepresentation[] getMembers() {
        return (ValueRepresentation[])getObject();
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