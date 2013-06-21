package net.sf.saxon.functions;

import net.sf.saxon.expr.Literal;
import net.sf.saxon.expr.parser.ExpressionVisitor;
import net.sf.saxon.om.NamespaceResolver;
import net.sf.saxon.trans.XPathException;

/**
* This class supports the XSLT element-available, function-available, and type-available functions.
*/

public abstract class Available extends SystemFunction {

    protected NamespaceResolver nsContext;
    private transient boolean checked = false;



    public void checkArguments(/*@NotNull*/ ExpressionVisitor visitor) throws XPathException {
        // the second time checkArguments is called, it's a global check so the static context is inaccurate
        if (checked) {
            return;
        }
        checked = true;
        super.checkArguments(visitor);
        if (!(argument[0] instanceof Literal &&
                (argument.length==1 || argument[1] instanceof Literal))) {
            // we need to save the namespace context
            nsContext = visitor.getStaticContext().getNamespaceResolver();
        }
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