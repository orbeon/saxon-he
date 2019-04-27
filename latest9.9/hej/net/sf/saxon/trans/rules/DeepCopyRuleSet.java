////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
// Copyright (c) 2018 Saxonica Limited.
// This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
// If a copy of the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
// This Source Code Form is "Incompatible With Secondary Licenses", as defined by the Mozilla Public License, v. 2.0.
////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

package net.sf.saxon.trans.rules;

import net.sf.saxon.event.Receiver;
import net.sf.saxon.expr.XPathContext;
import net.sf.saxon.expr.instruct.ParameterSet;
import net.sf.saxon.expr.parser.Location;
import net.sf.saxon.om.CopyOptions;
import net.sf.saxon.om.Item;
import net.sf.saxon.om.NodeInfo;
import net.sf.saxon.trans.XPathException;
import net.sf.saxon.type.Type;

/**
 * The built-in rule set introduced in XSLT 3.0, which performs a deep copy of any unmatched node.
 */
public class DeepCopyRuleSet implements BuiltInRuleSet {

    private static DeepCopyRuleSet THE_INSTANCE = new DeepCopyRuleSet();

    /**
     * Get the singleton instance of this class
     *
     * @return the singleton instance
     */

    public static DeepCopyRuleSet getInstance() {
        return THE_INSTANCE;
    }

    private DeepCopyRuleSet() {
    }

    private static int COPY_OPTIONS = CopyOptions.TYPE_ANNOTATIONS | CopyOptions.ALL_NAMESPACES;
    /**
     * Perform the built-in template action for a given node.
     *  @param item
     * @param parameters   the parameters supplied to apply-templates
     * @param tunnelParams the tunnel parameters to be passed through
     * @param context      the dynamic evaluation context
     * @param locationId   location of the instruction (apply-templates, apply-imports etc) that caused
*                     the built-in template to be invoked     @throws net.sf.saxon.trans.XPathException
     */

    public void process(Item item, ParameterSet parameters,
                        ParameterSet tunnelParams, XPathContext context,
                        Location locationId) throws XPathException {
        if (item instanceof NodeInfo) {
            NodeInfo node = (NodeInfo) item;
            Receiver out = context.getReceiver();
            switch (node.getNodeKind()) {
                case Type.DOCUMENT: 
                case Type.ELEMENT: {
                    if (out.getSystemId() == null) {
                        out.setSystemId(node.getBaseURI());
                    }
                    node.copy(out, COPY_OPTIONS, locationId);
                    return;
                }
                case Type.TEXT:
                    out.characters(item.getStringValueCS(), locationId, 0);
                    return;

                case Type.ATTRIBUTE:
                case Type.COMMENT:
                case Type.PROCESSING_INSTRUCTION:
                case Type.NAMESPACE:
                    node.copy(out, COPY_OPTIONS, locationId);
                    return;

                default:
            }
        } else {
            context.getReceiver().append(item, locationId, 0);
        }

    }

    /**
     * Get the default action for unmatched nodes
     *
     * @param nodeKind the node kind
     * @return the default action for unmatched element nodes: one of DEEP_COPY, APPLY_TEMPLATES, SKIP, FAIL
     */
    public int[] getActionForParentNodes(int nodeKind) {
        return new int[]{DEEP_COPY};
    }

    /**
     * Identify this built-in rule set
     * @return "deep-copy"
     */

    @Override
    public String getName() {
        return "deep-copy";
    }
}
