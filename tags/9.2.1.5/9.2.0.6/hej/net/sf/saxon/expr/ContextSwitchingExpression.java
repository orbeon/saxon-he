package net.sf.saxon.expr;

/**
 * Interface implemented by expressions that switch the context, for example A/B or A[B]
 */
public interface ContextSwitchingExpression {

    /**
     * Get the subexpression that sets the context item
     * @return the subexpression that sets the context item, position, and size to each of its
     * items in turn
     */

    public Expression getControllingExpression();

    /**
     * Get the subexpression that is evaluated in the new context
     * @return the subexpression evaluated in the context set by the controlling expression
     */

    public Expression getControlledExpression();
}

//
// The contents of this file are subject to the Mozilla Public License Version
// 1.0 (the "License");
// you may not use this file except in compliance with the License. You may
// obtain a copy of the
// License at http://www.mozilla.org/MPL/
//
// Software distributed under the License is distributed on an "AS IS" basis,
// WITHOUT WARRANTY OF ANY KIND, either express or implied.
// See the License for the specific language governing rights and limitations
// under the License.
//
// The Original Code is: all this file.
//
// The Initial Developer of the Original Code is Michael Kay
//
// Portions created by (your name) are Copyright (C) (your legal entity). All
// Rights Reserved.
//
// Contributor(s): none.
//

