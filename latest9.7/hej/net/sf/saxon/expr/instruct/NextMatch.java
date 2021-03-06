////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
// Copyright (c) 2015 Saxonica Limited.
// This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
// If a copy of the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
// This Source Code Form is "Incompatible With Secondary Licenses", as defined by the Mozilla Public License, v. 2.0.
////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

package net.sf.saxon.expr.instruct;

import com.saxonica.ee.bytecode.ExpressionCompiler;
import com.saxonica.ee.bytecode.NextMatchCompiler;
import com.saxonica.ee.stream.adjunct.NextMatchAdjunct;
import net.sf.saxon.Controller;
import net.sf.saxon.expr.Component;
import net.sf.saxon.expr.Expression;
import net.sf.saxon.expr.XPathContext;
import net.sf.saxon.expr.XPathContextMajor;
import net.sf.saxon.expr.parser.ExpressionTool;
import net.sf.saxon.expr.parser.RebindingMap;
import net.sf.saxon.om.Item;
import net.sf.saxon.om.StandardNames;
import net.sf.saxon.trace.ExpressionPresenter;
import net.sf.saxon.trans.Mode;
import net.sf.saxon.trans.Rule;
import net.sf.saxon.trans.XPathException;

import java.util.Arrays;


/**
 * An xsl:next-match element in the stylesheet
 */

public class NextMatch extends ApplyImports {

    boolean useTailRecursion;

    public NextMatch(boolean useTailRecursion) {
        super();
        this.useTailRecursion = useTailRecursion;
    }

    /**
     * Get the name of this instruction for diagnostic and tracing purposes
     */

    public int getInstructionNameCode() {
        return StandardNames.XSL_NEXT_MATCH;
    }

    /**
     * Copy an expression. This makes a deep copy.
     *
     * @return the copy of the original expression
     * @param rebindings
     */

    /*@NotNull*/
    public Expression copy(RebindingMap rebindings) {
        NextMatch nm2 = new NextMatch(useTailRecursion);
        nm2.setActualParams(WithParam.copy(nm2, getActualParams(), rebindings));
        nm2.setTunnelParams(WithParam.copy(nm2, getTunnelParams(), rebindings));
        ExpressionTool.copyLocationInfo(this, nm2);
        nm2.allowAnyItem = allowAnyItem;
        return nm2;
    }


    /*@Nullable*/
    public TailCall processLeavingTail(XPathContext context) throws XPathException {

        Controller controller = context.getController();
        assert controller != null;

        // handle parameters if any

        ParameterSet params = assembleParams(context, getActualParams());
        ParameterSet tunnels = assembleTunnelParams(context, getTunnelParams());

        Rule currentRule = context.getCurrentTemplateRule();
        if (currentRule == null) {
            XPathException e = new XPathException("There is no current template rule");
            e.setXPathContext(context);
            e.setErrorCode("XTDE0560");
            throw e;
        }
        Component<? extends Mode> mode = context.getCurrentMode();
        if (mode == null) {
            throw new AssertionError("Current mode is null");
        }
//        if (context.getCurrentIterator() == null) {
//            XPathException e = new XPathException("Cannot call xsl:next-match when there is no context item");
//            e.setXPathContext(context);
//            e.setErrorCode("XTDE0565");
//            throw e;
//        }
        Item currentItem = context.getCurrentIterator().current();
//        if (!allowAnyItem && !(currentItem instanceof NodeInfo)) {
//            XPathException e = new XPathException("Cannot call xsl:next-match when context item is not a node");
//            e.setXPathContext(context);
//            e.setErrorCode("XTDE0565");
//            throw e;
//        }
        Rule rule = mode.getCode().getNextMatchRule(currentItem, currentRule, context);
        //Rule rule = controller.getRuleManager().getNextMatchHandler(currentItem, mode.getCode(), currentRule, context);

        if (rule == null) {             // use the default action for the node
            mode.getCode().getBuiltInRuleSet().process(currentItem, params, tunnels, context, getLocation());
        } else if (useTailRecursion) {
            //Template nh = (Template)rule.getAction();
            // clear all the local variables: they are no longer needed
            Arrays.fill(context.getStackFrame().getStackFrameValues(), null);
            ((XPathContextMajor)context).setCurrentComponent(mode); // bug 2818
            return new NextMatchPackage(rule, params, tunnels, context);
        } else {
            TemplateRule nh = (TemplateRule) rule.getAction();
            XPathContextMajor c2 = context.newContext();
            c2.setOrigin(this);
            //c2.setOriginatingConstructType(LocationKind.TEMPLATE);
            c2.openStackFrame(nh.getStackFrameMap());
            c2.setLocalParameters(params);
            c2.setTunnelParameters(tunnels);
            c2.setCurrentTemplateRule(rule);
            c2.setCurrentComponent(mode); // needed in the case where next-match is called from a named template
            nh.apply(c2);
        }
        return null;
    }

    /**
     * Diagnostic print of expression structure. The abstract expression tree
     * is written to the supplied output destination.
     */

    public void export(ExpressionPresenter out) throws XPathException {
        out.startElement("nextMatch", this);
        String flags = "";
        if (useTailRecursion) {
            flags = "t";
        }
        if (allowAnyItem) {
            flags += "i";
        }
        out.emitAttribute("flags", flags);
        if (getActualParams().length != 0) {
            WithParam.exportParameters(getActualParams(), out, false);
        }
        if (getTunnelParams().length != 0) {
            WithParam.exportParameters(getTunnelParams(), out, true);
        }
        out.endElement();
    }

    /**
     * A NextMatchPackage is an object that encapsulates the name of a template to be called,
     * the parameters to be supplied, and the execution context. This object can be returned as a tail
     * call, so that the actual call is made from a lower point on the stack, allowing a tail-recursive
     * template to execute in a finite stack size
     */

    private class NextMatchPackage implements TailCall {

        private Rule rule;
        private ParameterSet params;
        private ParameterSet tunnelParams;
        private XPathContext evaluationContext;

        /**
         * Construct a NextMatchPackage that contains information about a call.
         *
         * @param rule              the rule identifying the Template to be called
         * @param params            the parameters to be supplied to the called template
         * @param tunnelParams      the tunnel parameter supplied to the called template
         * @param evaluationContext saved context information from the Controller (current mode, etc)
         *                          which must be reset to ensure that the template is called with all the context information
         *                          intact
         */

        public NextMatchPackage(Rule rule,
                                ParameterSet params,
                                ParameterSet tunnelParams,
                                XPathContext evaluationContext) {
            this.rule = rule;
            this.params = params;
            this.tunnelParams = tunnelParams;
            this.evaluationContext = evaluationContext;
        }

        /**
         * Process the template call encapsulated by this package.
         *
         * @return another TailCall. This will never be the original call, but it may be the next
         *         recursive call. For example, if A calls B which calls C which calls D, then B may return
         *         a TailCall to A representing the call from B to C; when this is processed, the result may be
         *         a TailCall representing the call from C to D.
         * @throws XPathException if a dynamic error occurs
         */

        public TailCall processLeavingTail() throws XPathException {
            TemplateRule nh = (TemplateRule) rule.getAction();
            XPathContextMajor c2 = evaluationContext.newContext();
            c2.setOrigin(NextMatch.this);
            //c2.setOriginatingConstructType(LocationKind.TEMPLATE);
            c2.setLocalParameters(params);
            c2.setTunnelParameters(tunnelParams);
            c2.openStackFrame(nh.getStackFrameMap());
            c2.setCurrentTemplateRule(rule);
            c2.setCurrentComponent(evaluationContext.getCurrentComponent());

            // System.err.println("Tail call on template");

            return nh.applyLeavingTail(c2);
        }
    }

//#ifdefined BYTECODE

    /**
     * Return the compiler of the NextMatch expression
     *
     * @return the relevant ExpressionCompiler
     */
    @Override
    public ExpressionCompiler getExpressionCompiler() {
        return new NextMatchCompiler();
    }

    @Override
    public NextMatchAdjunct getStreamingAdjunct() {
        return new NextMatchAdjunct();
    }

    //#endif

}

