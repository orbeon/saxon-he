package net.sf.saxon.instruct;
import net.sf.saxon.Controller;
import net.sf.saxon.Configuration;
import net.sf.saxon.expr.*;
import net.sf.saxon.om.*;
import net.sf.saxon.om.StandardNames;
import net.sf.saxon.trace.InstructionInfo;
import net.sf.saxon.trans.DynamicError;
import net.sf.saxon.trans.XPathException;
import net.sf.saxon.type.ItemType;

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Iterator;

/**
* Instruction representing an xsl:call-template element in the stylesheet.
*/

public class CallTemplate extends Instruction {

    private Template template = null;
    private WithParam[] actualParams = null;
    private WithParam[] tunnelParams = null;
    private boolean useTailRecursion = false;
    private Expression calledTemplateExpression;    // allows name to be an AVT
    private NamespaceResolver nsContext;             // needed only for a dynamic call

    /**
    * Construct a CallTemplate instruction.
    * @param template the Template object identifying the template to be called, in the normal
    * case where this is known statically
    * @param useTailRecursion
    * @param calledTemplateExpression expression to calculate the name of the template to be called
    * at run-time, this supports the saxon:allow-avt option
    * @param nsContext the static namespace context of the instruction, needed only in the case
    * where the name of the called template is to be calculated dynamically
    */

    public CallTemplate (   Template template,
                            boolean useTailRecursion,
                            Expression calledTemplateExpression,
                            NamespaceResolver nsContext ) {

        this.template = template;
        this.useTailRecursion = useTailRecursion;
        this.calledTemplateExpression = calledTemplateExpression;
        this.nsContext = nsContext;
        adoptChildExpression(calledTemplateExpression);
    }

   /**
     * Set the actual parameters on the call
     */

    public void setActualParameters(
                        WithParam[] actualParams,
                        WithParam[] tunnelParams ) {
        this.actualParams = actualParams;
        this.tunnelParams = tunnelParams;
        for (int i=0; i<actualParams.length; i++) {
            adoptChildExpression(actualParams[i]);
        }
        for (int i=0; i<tunnelParams.length; i++) {
            adoptChildExpression(tunnelParams[i]);
        }
    }

    /**
    * Return the name of this instruction.
    */

    public int getInstructionNameCode() {
        return StandardNames.XSL_CALL_TEMPLATE;
    }

    /**
     * Set additional trace properties appropriate to the kind of instruction. This
     * implementation adds the template property, which identities the template to be called
     */

    public InstructionInfo getInstructionInfo() {
        InstructionDetails details = (InstructionDetails)super.getInstructionInfo();
        if (template != null) {
            details.setProperty("template", template);
        }
        return details;
    }

    /**
     * Simplify an expression. This performs any static optimization (by rewriting the expression
     * as a different expression).
     *
     * @exception XPathException if an error is discovered during expression
     *     rewriting
     * @return the simplified expression
     */

    public Expression simplify(StaticContext env) throws XPathException {
        WithParam.simplify(actualParams, env);
        WithParam.simplify(tunnelParams, env);
        if (calledTemplateExpression != null) {
            calledTemplateExpression = calledTemplateExpression.simplify(env);
        }
        return this;
    }

    public Expression typeCheck(StaticContext env, ItemType contextItemType) throws XPathException {
        WithParam.typeCheck(actualParams, env, contextItemType);
        WithParam.typeCheck(tunnelParams, env, contextItemType);
        if (calledTemplateExpression != null) {
            calledTemplateExpression = calledTemplateExpression.typeCheck(env, contextItemType);
            adoptChildExpression(calledTemplateExpression);
        }
        return this;
    }

    public Expression optimize(Optimizer opt, StaticContext env, ItemType contextItemType) throws XPathException {
        WithParam.optimize(opt, actualParams, env, contextItemType);
        WithParam.optimize(opt, tunnelParams, env, contextItemType);
        if (calledTemplateExpression != null) {
            calledTemplateExpression = calledTemplateExpression.optimize(opt, env, contextItemType);
            adoptChildExpression(calledTemplateExpression);
        }
        return this;
    }

    public int getIntrinsicDependencies() {
        // we could go to the called template and find which parts of the context it depends on, but this
        // would create the risk of infinite recursion. So we just assume that the dependencies exist
        return StaticProperty.DEPENDS_ON_XSLT_CONTEXT |
                StaticProperty.DEPENDS_ON_FOCUS;
    }

    /**
     * Determine whether this instruction creates new nodes.
     * This implementation currently returns true unconditionally.
     */

    public final boolean createsNewNodes() {
        return true;
    }

    /**
     * Get all the XPath expressions associated with this instruction
     * (in XSLT terms, the expression present on attributes of the instruction,
     * as distinct from the child instructions in a sequence construction)
     */

    public Iterator iterateSubExpressions() {
        ArrayList list = new ArrayList(10);
        if (calledTemplateExpression != null) {
            list.add(calledTemplateExpression);
        }
        WithParam.getXPathExpressions(actualParams, list);
        WithParam.getXPathExpressions(tunnelParams, list);
        return list.iterator();
    }

    /**
     * Replace one subexpression by a replacement subexpression
     * @param original the original subexpression
     * @param replacement the replacement subexpression
     * @return true if the original subexpression is found
     */

    public boolean replaceSubExpression(Expression original, Expression replacement) {
        boolean found = false;
        if (WithParam.replaceXPathExpression(actualParams, original, replacement)) {
            found = true;
        }
        if (WithParam.replaceXPathExpression(tunnelParams, original, replacement)) {
            found = true;
        }
        if (calledTemplateExpression == original) {
            calledTemplateExpression = replacement;
        }
                return found;
    }


    /**
     * Handle promotion offers, that is, non-local tree rewrites.
     * @param offer The type of rewrite being offered
     * @throws net.sf.saxon.trans.XPathException
     */

    protected void promoteInst(PromotionOffer offer) throws XPathException {
        if (calledTemplateExpression != null) {
            calledTemplateExpression = doPromotion(calledTemplateExpression, offer);
        }
        WithParam.promoteParams(actualParams, offer);
        WithParam.promoteParams(tunnelParams, offer);
    }

    /**
     * Process this instruction, without leaving any tail calls.
     * @param context the dynamic context for this transformation
     * @throws XPathException if a dynamic error occurs
     */

    public void process(XPathContext context) throws XPathException {

        Template t = getTargetTemplate(context);
        XPathContextMajor c2 = context.newContext();
        c2.setOrigin(this);
        c2.openStackFrame(t.getStackFrameMap());
        c2.setLocalParameters(assembleParams(context, actualParams));
        c2.setTunnelParameters(assembleTunnelParams(context, tunnelParams));

        try {
            TailCall tc = t.expand(c2);
            while (tc != null) {
                tc = tc.processLeavingTail();
            }
        } catch (StackOverflowError e) {
            DynamicError err = new DynamicError(
                    "Too many nested template or function calls. The stylesheet may be looping.");
            err.setLocator(this);
            err.setXPathContext(context);
            throw err;
        }
    }

    /**
    * Process this instruction. If the called template contains a tail call (which may be
    * an xsl:call-template of xsl:apply-templates instruction) then the tail call will not
    * actually be evaluated, but will be returned in a TailCall object for the caller to execute.
    * @param context the dynamic context for this transformation
    * @return an object containing information about the tail call to be executed by the
    * caller. Returns null if there is no tail call.
    */

    public TailCall processLeavingTail(XPathContext context) throws XPathException
    {
        if (!useTailRecursion) {
            process(context);
            return null;
        }

        // if name is determined dynamically, determine it now

        Template target = getTargetTemplate(context);

        // handle parameters if any

        ParameterSet params = assembleParams(context, actualParams);
        ParameterSet tunnels = assembleTunnelParams(context, tunnelParams);

        // Call the named template. Actually, don't call it; rather construct a call package
        // and return it to the caller, who will then process this package.

        //System.err.println("Call template using tail recursion");
        if (params==null) {                  // bug 490967
            params = ParameterSet.EMPTY_PARAMETER_SET;
        }

        // clear all the local variables: they are no longer needed
        StackFrame frame = context.getStackFrame();
        ValueRepresentation[] vars = frame.getStackFrameValues();
        for (int i=0; i<vars.length; i++) {
            vars[i] = null;
        }

        return new CallTemplatePackage(target, params, tunnels, context);
    }

    /**
     * Get the template, in the case where it is specified dynamically.
     * @param context        The dynamic context of the transformation
     * @return                  The template to be called
     * @throws XPathException if a dynamic error occurs: specifically, if the
     * template name is computed at run-time (Saxon extension) and the name is invalid
     * or does not reference a known template
     */

    public Template getTargetTemplate(XPathContext context) throws XPathException {
        if (calledTemplateExpression != null) {
            Controller controller = context.getController();
            String qname = calledTemplateExpression.evaluateAsString(context);

            String prefix;
            String localName;
            try {
                String[] parts = controller.getConfiguration().getNameChecker().getQNameParts(qname);
                prefix = parts[0];
                localName = parts[1];
            } catch (QNameException err) {
                dynamicError("Invalid template name. " + err.getMessage(), "XTSE0650", context);
                return null;
            }
            String uri = nsContext.getURIForPrefix(prefix, false);
            if (uri==null) {
  		        dynamicError("Namespace prefix " + prefix + " has not been declared", "XTSE0650", context);
  		    }
            int fprint = controller.getNamePool().getFingerprint(uri, localName);
            Template target = controller.getExecutable().getNamedTemplate(fprint);
            if (target==null) {
            	dynamicError("Template " + qname + " has not been defined", "XTSE0650", context);
            }
            return target;
        } else {
            return template;
        }
    }

    /**
     * Diagnostic print of expression structure. The expression is written to the System.err
     * output stream
     *
     * @param level indentation level for this expression
     @param out
     @param config
     */

    public void display(int level, PrintStream out, Configuration config) {
        out.println(ExpressionTool.indent(level) + "call-template");
    }

    /**
    * A CallTemplatePackage is an object that encapsulates the name of a template to be called,
    * the parameters to be supplied, and the execution context. This object can be returned as a tail
    * call, so that the actual call is made from a lower point on the stack, allowing a tail-recursive
    * template to execute in a finite stack size
    */

    private class CallTemplatePackage implements TailCall {

        private Template target;
        private ParameterSet params;
        private ParameterSet tunnelParams;
        private XPathContext evaluationContext;

        /**
        * Construct a CallTemplatePackage that contains information about a call.
        * @param template the Template to be called
        * @param params the parameters to be supplied to the called template
        * @param evaluationContext saved context information from the Controller (current mode, etc)
        * which must be reset to ensure that the template is called with all the context information
        * intact
        */

        public CallTemplatePackage(Template template,
                                   ParameterSet params,
                                   ParameterSet tunnelParams,
                                   XPathContext evaluationContext) {
            this.target = template;
            this.params = params;
            this.tunnelParams = tunnelParams;
            this.evaluationContext = evaluationContext;
        }

        /**
        * Process the template call encapsulated by this package.
        * @return another TailCall. This will never be the original call, but it may be the next
        * recursive call. For example, if A calls B which calls C which calls D, then B may return
        * a TailCall to A representing the call from B to C; when this is processed, the result may be
        * a TailCall representing the call from C to D.
         * @throws XPathException if a dynamic error occurs
        */

        public TailCall processLeavingTail() throws XPathException {
            // TODO: the idea of tail call optimization is to reuse the caller's stack frame rather than
            // creating a new one. We're doing this for the Java stack, but not for the context stack where
            // local variables are held. It should be possible to avoid creating a new context, and instead
            // to update the existing one in situ.
            XPathContextMajor c2 = evaluationContext.newContext();
            c2.setOrigin(CallTemplate.this);
            c2.setLocalParameters(params);
            c2.setTunnelParameters(tunnelParams);
            c2.openStackFrame(target.getStackFrameMap());

            // System.err.println("Tail call on template");

            TailCall tc = target.expand(c2);
            return tc;
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
// The Original Code is: all this file.
//
// The Initial Developer of the Original Code is Michael H. Kay.
//
// Portions created by (your name) are Copyright (C) (your legal entity). All Rights Reserved.
//
// Contributor(s): none.
//
