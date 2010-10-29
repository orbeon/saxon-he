package net.sf.saxon.trans;

import net.sf.saxon.expr.XPathContext;
import net.sf.saxon.expr.XPathContextMajor;
import net.sf.saxon.expr.instruct.ApplyTemplates;
import net.sf.saxon.expr.instruct.ParameterSet;
import net.sf.saxon.expr.instruct.TailCall;
import net.sf.saxon.om.Axis;
import net.sf.saxon.om.NodeInfo;
import net.sf.saxon.om.SequenceIterator;
import net.sf.saxon.trace.Location;
import net.sf.saxon.type.Type;

/**
 *  The built-in rule set used for 1.0 and 2.0, which for document and element nodes does an apply-templates
 *  to children, and for text nodes and attribute nodes copies the node.
 */
public class StringifyRuleSet implements BuiltInRuleSet {

    private static StringifyRuleSet THE_INSTANCE = new StringifyRuleSet();

    /**
     * Get the singleton instance of this class
     * @return the singleton instance
     */

    public static StringifyRuleSet getInstance() {
        return THE_INSTANCE;
    }

    private StringifyRuleSet() {};

    /**
     * Perform the built-in template action for a given node.
     * @param node         the node to be processed
     * @param parameters   the parameters supplied to apply-templates
     * @param tunnelParams the tunnel parameters to be passed through
     * @param context      the dynamic evaluation context
     * @param locationId   location of the instruction (apply-templates, apply-imports etc) that caused
     *                     the built-in template to be invoked
     * @throws net.sf.saxon.trans.XPathException
     *          if any dynamic error occurs
     */

    public void process(NodeInfo node, ParameterSet parameters,
                        ParameterSet tunnelParams, XPathContext context,
                        int locationId) throws XPathException {
        switch(node.getNodeKind()) {
            case Type.DOCUMENT:
            case Type.ELEMENT:
                SequenceIterator iter = node.iterateAxis(Axis.CHILD);
                XPathContextMajor c2 = context.newContext();
                c2.setOriginatingConstructType(Location.BUILT_IN_TEMPLATE);
	            TailCall tc = ApplyTemplates.applyTemplates(
                        iter, context.getCurrentMode(), parameters, tunnelParams, c2, locationId);
                while (tc != null) {
                    tc = tc.processLeavingTail();
                }
	            return;
	        case Type.TEXT:
	            // NOTE: I tried changing this to use the text node's copy() method, but
	            // performance was worse
	        case Type.ATTRIBUTE:
	            context.getReceiver().characters(node.getStringValueCS(), locationId, 0);
	            return;
	        case Type.COMMENT:
	        case Type.PROCESSING_INSTRUCTION:
	        case Type.NAMESPACE:
	            // no action
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




