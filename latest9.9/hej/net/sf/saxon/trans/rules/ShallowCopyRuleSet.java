////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
// Copyright (c) 2018 Saxonica Limited.
// This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
// If a copy of the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
// This Source Code Form is "Incompatible With Secondary Licenses", as defined by the Mozilla Public License, v. 2.0.
////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

package net.sf.saxon.trans.rules;

import net.sf.saxon.event.PipelineConfiguration;
import net.sf.saxon.event.Receiver;
import net.sf.saxon.expr.XPathContext;
import net.sf.saxon.expr.XPathContextMajor;
import net.sf.saxon.expr.instruct.ParameterSet;
import net.sf.saxon.expr.instruct.TailCall;
import net.sf.saxon.expr.parser.Location;
import net.sf.saxon.om.*;
import net.sf.saxon.trans.XPathException;
import net.sf.saxon.tree.iter.AxisIterator;
import net.sf.saxon.tree.iter.EmptyIterator;
import net.sf.saxon.tree.util.NamespaceIterator;
import net.sf.saxon.type.Type;

/**
 * The built-in rule set introduced in XSLT 3.0, which is effectively an identity template.
 */
public class ShallowCopyRuleSet implements BuiltInRuleSet {

    private static ShallowCopyRuleSet THE_INSTANCE = new ShallowCopyRuleSet();

    /**
     * Get the singleton instance of this class
     *
     * @return the singleton instance
     */

    public static ShallowCopyRuleSet getInstance() {
        return THE_INSTANCE;
    }

    private ShallowCopyRuleSet() {
    }

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
            switch (node.getNodeKind()) {
                case Type.DOCUMENT: {
                    Receiver out = context.getReceiver();
                    PipelineConfiguration pipe = out.getPipelineConfiguration();
                    if (out.getSystemId() == null) {
                        out.setSystemId(node.getBaseURI());
                    }
                    out.startDocument(0);
                    XPathContextMajor c2 = context.newContext();
                    c2.setOrigin(this);
                    c2.trackFocus(node.iterateAxis(AxisInfo.CHILD));
                    c2.setCurrentComponent(c2.getCurrentMode());  // Bug 3508
                    pipe.setXPathContext(c2);
                    TailCall tc = context.getCurrentMode().getActor().applyTemplates(parameters, tunnelParams, c2, locationId);
                    while (tc != null) {
                        tc = tc.processLeavingTail();
                    }
                    out.endDocument();
                    pipe.setXPathContext(context);
                    return;
                }
                case Type.ELEMENT: {
                    Receiver out = context.getReceiver();
                    PipelineConfiguration pipe = out.getPipelineConfiguration();
                    if (out.getSystemId() == null) {
                        out.setSystemId(node.getBaseURI());
                    }
                    NodeName fqn = NameOfNode.makeName(node);
                    out.startElement(fqn, node.getSchemaType(), locationId, 0);
                    NamespaceIterator.sendNamespaces(node, out);

                    XPathContextMajor c2 = context.newContext();
                    c2.setCurrentComponent(c2.getCurrentMode());  // Bug 3508
                    pipe.setXPathContext(c2);

                    // apply-templates to all attributes
                    AxisIterator attributes = node.iterateAxis(AxisInfo.ATTRIBUTE);
                    if (attributes != EmptyIterator.OfNodes.THE_INSTANCE) {
                        c2.setOrigin(this);
                        c2.trackFocus(attributes);
                        TailCall tc = c2.getCurrentMode().getActor().applyTemplates(parameters, tunnelParams, c2, locationId);
                        while (tc != null) {
                            tc = tc.processLeavingTail();
                        }
                    }

                    // apply-templates to all children
                    if (node.hasChildNodes()) {
                        c2.trackFocus(node.iterateAxis(AxisInfo.CHILD));
                        TailCall tc = c2.getCurrentMode().getActor().applyTemplates(parameters, tunnelParams, c2, locationId);
                        while (tc != null) {
                            tc = tc.processLeavingTail();
                        }
                    }
                    out.endElement();
                    pipe.setXPathContext(context);
                    return;
                }
                case Type.TEXT:
                    context.getReceiver().characters(item.getStringValueCS(), locationId, 0);
                    return;

                case Type.ATTRIBUTE:
                case Type.COMMENT:
                case Type.PROCESSING_INSTRUCTION:
                case Type.NAMESPACE:
                    node.copy(context.getReceiver(), 0, locationId);
                    return;

                default:
            }
        } else {
            context.getReceiver().append(item, locationId, 0);
        }

    }


    /**
     * Identify this built-in rule set
     *
     * @return "shallow-copy"
     */

    @Override
    public String getName() {
        return "shallow-copy";
    }

    /**
     * Get the default action for unmatched nodes
     *
     * @param nodeKind the node kind
     * @return the default action for unmatched nodes: one of DEEP_COPY, APPLY_TEMPLATES, DEEP_SKIP, FAIL
     */
    public int[] getActionForParentNodes(int nodeKind) {
        return new int[]{SHALLOW_COPY, APPLY_TEMPLATES_TO_ATTRIBUTES, APPLY_TEMPLATES_TO_CHILDREN};
    }
}
