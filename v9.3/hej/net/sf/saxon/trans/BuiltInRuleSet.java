package net.sf.saxon.trans;

import net.sf.saxon.expr.XPathContext;
import net.sf.saxon.expr.instruct.ParameterSet;
import net.sf.saxon.om.NodeInfo;

import java.io.Serializable;

/**
 * Defines a set of built-in template rules (rules for use when no user-defined template
 * rules match a given node)
 */
public interface BuiltInRuleSet extends Serializable {

    /**
     * Perform the built-in template action for a given node.
     *
     * @param node the node to be processed
     * @param parameters the parameters supplied to apply-templates
     * @param tunnelParams the tunnel parameters to be passed through
     * @param context the dynamic evaluation context
     * @param locationId location of the instruction (apply-templates, apply-imports etc) that caused
     * the built-in template to be invoked
     * @exception XPathException if any dynamic error occurs
     */    

    public void process( NodeInfo node,
                         ParameterSet parameters,
                         ParameterSet tunnelParams,
                         XPathContext context,
                         int locationId) throws XPathException;

}

//
// The contents of this file are subject to the Mozilla Public License Version 1.0 (the "License");
// you may not use this file except in compliance with the License. You may obtain a copy of the
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

