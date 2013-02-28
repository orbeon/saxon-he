package net.sf.saxon.trans;

import net.sf.saxon.expr.XPathContext;
import net.sf.saxon.expr.XPathContextMajor;
import net.sf.saxon.expr.instruct.ParameterSet;
import net.sf.saxon.expr.instruct.TailCall;
import net.sf.saxon.om.Axis;
import net.sf.saxon.om.Item;
import net.sf.saxon.om.NodeInfo;
import net.sf.saxon.om.SequenceIterator;
import net.sf.saxon.trace.Location;
import net.sf.saxon.type.Type;

/**
 *  The built-in rule set used for 1.0 and 2.0, which for document and element nodes does an apply-templates
 *  to children, and for text nodes and attribute nodes copies the node.
 */
public class ShallowSkipRuleSet implements BuiltInRuleSet {

    private static ShallowSkipRuleSet THE_INSTANCE = new ShallowSkipRuleSet();

    /**
     * Get the singleton instance of this class
     * @return the singleton instance
     */

    public static ShallowSkipRuleSet getInstance() {
        return THE_INSTANCE;
    }

    private ShallowSkipRuleSet() {}

    /**
     * Perform the built-in template action for a given item.
     * @param item         the item to be processed
     * @param parameters   the parameters supplied to apply-templates
     * @param tunnelParams the tunnel parameters to be passed through
     * @param context      the dynamic evaluation context
     * @param locationId   location of the instruction (apply-templates, apply-imports etc) that caused
     *                     the built-in template to be invoked
     * @throws XPathException
     *          if any dynamic error occurs
     */

    public void process(Item item, ParameterSet parameters,
                        ParameterSet tunnelParams, /*@NotNull*/ XPathContext context,
                        int locationId) throws XPathException {
        if (item instanceof NodeInfo) {
            NodeInfo node = (NodeInfo)item;
            switch(node.getNodeKind()) {
                case Type.DOCUMENT:
                case Type.ELEMENT:
                    SequenceIterator iter = node.iterateAxis(Axis.CHILD);
                    XPathContextMajor c2 = context.newContext();
                    c2.setOriginatingConstructType(Location.BUILT_IN_TEMPLATE);
                    c2.setCurrentIterator(iter);
                    TailCall tc = c2.getCurrentMode().applyTemplates(parameters, tunnelParams, c2, locationId);
                    while (tc != null) {
                        tc = tc.processLeavingTail();
                    }
                    return;
                case Type.TEXT:
                case Type.ATTRIBUTE:
                case Type.COMMENT:
                case Type.PROCESSING_INSTRUCTION:
                case Type.NAMESPACE:
                    // no action
            }
        } else {
            // no action (e.g. for atomic values and function items
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