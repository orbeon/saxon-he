package net.sf.saxon.instruct;
import net.sf.saxon.Controller;
import net.sf.saxon.expr.XPathContext;
import net.sf.saxon.expr.XPathContextMajor;
import net.sf.saxon.om.Item;
import net.sf.saxon.om.NodeInfo;
import net.sf.saxon.om.StandardNames;
import net.sf.saxon.om.StandardNames;
import net.sf.saxon.trans.DynamicError;
import net.sf.saxon.trans.Mode;
import net.sf.saxon.trans.XPathException;
import net.sf.saxon.trans.Rule;

/**
* An xsl:next-match element in the stylesheet
*/

public class NextMatch extends ApplyImports {

    public NextMatch(boolean backwardsCompatible) {
        super(backwardsCompatible);
    }

    /**
    * Get the name of this instruction for diagnostic and tracing purposes
    */

    public int getInstructionNameCode() {
        return StandardNames.XSL_NEXT_MATCH;
    }

    public TailCall processLeavingTail(XPathContext context) throws XPathException {

        Controller controller = context.getController();

        // handle parameters if any

        ParameterSet params = assembleParams(context, actualParams);
        ParameterSet tunnels = assembleTunnelParams(context, tunnelParams);

        Rule currentRule = context.getCurrentTemplateRule();
        if (currentRule==null) {
            DynamicError e = new DynamicError("There is no current template rule");
            e.setXPathContext(context);
            e.setErrorCode("XTDE0560");
            throw e;
        }
        Mode mode = context.getCurrentMode();
        if (mode == null) {
            mode = controller.getRuleManager().getDefaultMode();
        }
        if (context.getCurrentIterator()==null) {
            DynamicError e = new DynamicError("There is no context item");
            e.setXPathContext(context);
            e.setErrorCode("XTDE0565");
            throw e;
        }
        Item currentItem = context.getCurrentIterator().current();
        if (!(currentItem instanceof NodeInfo)) {
            DynamicError e = new DynamicError("Cannot call xsl:next-match when context item is not a node");
            e.setXPathContext(context);
            e.setErrorCode("XTDE0565");
            throw e;
        }
        NodeInfo node = (NodeInfo)currentItem;
        Rule rule = controller.getRuleManager().getNextMatchHandler(node, mode, currentRule, context);

		if (rule==null) {             // use the default action for the node
            ApplyTemplates.defaultAction(node, params, tunnels, context, false, getLocationId());
        } else {
            Template nh = (Template)rule.getAction();
            XPathContextMajor c2 = context.newContext();
            c2.setOrigin(this);
            c2.openStackFrame(nh.getStackFrameMap());
            c2.setLocalParameters(params);
            c2.setTunnelParameters(tunnels);
            nh.apply(c2, rule);
        }
        return null;
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
// The Original Code is: all this file.
//
// The Initial Developer of the Original Code is Michael H. Kay.
//
// Portions created by (your name) are Copyright (C) (your legal entity). All Rights Reserved.
//
// Contributor(s): none.
//
