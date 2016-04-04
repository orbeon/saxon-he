////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
// Copyright (c) 2015 Saxonica Limited.
// This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
// If a copy of the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
// This Source Code Form is "Incompatible With Secondary Licenses", as defined by the Mozilla Public License, v. 2.0.
////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

package net.sf.saxon.expr.instruct;

import com.saxonica.ee.bytecode.ApplyImportsCompiler;
import com.saxonica.ee.bytecode.ExpressionCompiler;
import com.saxonica.ee.stream.adjunct.ApplyImportsAdjunct;
import com.saxonica.ee.stream.adjunct.StreamingAdjunct;
import net.sf.saxon.Controller;
import net.sf.saxon.expr.*;
import net.sf.saxon.expr.parser.*;
import net.sf.saxon.om.Item;
import net.sf.saxon.om.StandardNames;
import net.sf.saxon.trace.ExpressionPresenter;
import net.sf.saxon.trans.Mode;
import net.sf.saxon.trans.Rule;
import net.sf.saxon.trans.XPathException;

import java.util.ArrayList;
import java.util.List;


/**
 * An xsl:apply-imports element in the stylesheet. NOTE: NextMatch is a subclass
 */

public class ApplyImports extends Instruction implements ITemplateCall {

    // TODO: make ApplyImports and NextMatch subclasses of an abstract superclass

    private WithParam[] actualParams;
    private WithParam[] tunnelParams;

    boolean allowAnyItem = false;

    public ApplyImports() {
    }

    /**
     * Get the name of this instruction for diagnostic and tracing purposes
     */
    public int getInstructionNameCode() {
        return StandardNames.XSL_APPLY_IMPORTS;
    }

    /**
     * An implementation of Expression must provide at least one of the methods evaluateItem(), iterate(), or process().
     * This method indicates which of these methods is prefered. For instructions this is the process() method.
     */

    public int getImplementationMethod() {
        return super.getImplementationMethod() | Expression.WATCH_METHOD;
    }


    /**
     * Get the actual parameters passed to the called template
     *
     * @return the non-tunnel parameters
     */


    public WithParam[] getActualParams() {
        return actualParams;
    }

    /**
     * Get the tunnel parameters passed to the called template
     *
     * @return the tunnel parameters
     */


    public WithParam[] getTunnelParams() {
        return tunnelParams;
    }

    public void setActualParams(WithParam[] params) {
        this.actualParams = params;
    }

    public void setTunnelParams(WithParam[] params) {
        this.tunnelParams = params;
    }

    @Override
    public Iterable<Operand> operands() {
        List<Operand> operanda = new ArrayList<Operand>(actualParams.length + tunnelParams.length);
        WithParam.gatherOperands(this, actualParams, operanda);
        WithParam.gatherOperands(this, tunnelParams, operanda);
        return operanda;
    }

    /**
     * Ask whether the instruction can process any item (XSLT 3.0), or only nodes (XSLT 1.0/2.0)
     *
     * @return true if any item can be processed
     */

    public boolean isAllowAnyItem() {
        return allowAnyItem;
    }

    /**
     * Say whether the instruction can process any item (XSLT 3.0), or only nodes (XSLT 1.0/2.0)
     *
     * @param allowAny true if any item can be processed
     */

    public void setAllowAnyItem(boolean allowAny) {
        allowAnyItem = allowAny;
    }

    /**
     * Simplify an expression. This performs any static optimization (by rewriting the expression
     * as a different expression).
     *
     *
     *
     * @return the simplified expression
     * @throws net.sf.saxon.trans.XPathException
     *          if an error is discovered during expression
     *          rewriting
     */

    /*@NotNull*/
    public Expression simplify() throws XPathException {
        WithParam.simplify(getActualParams());
        WithParam.simplify(getTunnelParams());
        allowAnyItem = getPackageData().getXPathVersion() >= 30;
        return this;
    }

    /*@NotNull*/
    public Expression typeCheck(ExpressionVisitor visitor, ContextItemStaticInfo contextInfo) throws XPathException {
        WithParam.typeCheck(actualParams, visitor, contextInfo);
        WithParam.typeCheck(tunnelParams, visitor, contextInfo);
        return this;
    }

    /*@NotNull*/
    public Expression optimize(ExpressionVisitor visitor, ContextItemStaticInfo contextInfo) throws XPathException {
        WithParam.optimize(visitor, actualParams, contextInfo);
        WithParam.optimize(visitor, tunnelParams, contextInfo);
        return this;
    }

    public int getIntrinsicDependencies() {
        return StaticProperty.DEPENDS_ON_CONTEXT_ITEM;
    }

    /**
     * Copy an expression. This makes a deep copy.
     *
     * @return the copy of the original expression
     */

    /*@NotNull*/
    public Expression copy() {
        ApplyImports ai2 = new ApplyImports();
        ai2.setActualParams(WithParam.copy(ai2, actualParams));
        ai2.setTunnelParams(WithParam.copy(ai2, tunnelParams));
        ExpressionTool.copyLocationInfo(this, ai2);
        ai2.allowAnyItem = allowAnyItem;
        return ai2;
    }


    /**
     * Determine whether this instruction creates new nodes.
     * This implementation returns true (which is almost invariably the case, so it's not worth
     * doing any further analysis to find out more precisely).
     */

    public final boolean createsNewNodes() {
        return true;
    }

    /**
     * Handle promotion offers, that is, non-local tree rewrites.
     *
     * @param offer The type of rewrite being offered
     * @throws XPathException
     */

    protected void promoteChildren(PromotionOffer offer) throws XPathException {
        WithParam.promoteParams(actualParams, offer);
        WithParam.promoteParams(tunnelParams, offer);
    }

    /**
     * Add a representation of this expression to a PathMap. The PathMap captures a map of the nodes visited
     * by an expression in a source tree.
     * <p/>
     * <p>The default implementation of this method assumes that an expression does no navigation other than
     * the navigation done by evaluating its subexpressions, and that the subexpressions are evaluated in the
     * same context as the containing expression. The method must be overridden for any expression
     * where these assumptions do not hold. For example, implementations exist for AxisExpression, ParentExpression,
     * and RootExpression (because they perform navigation), and for the doc(), document(), and collection()
     * functions because they create a new navigation root. Implementations also exist for PathExpression and
     * FilterExpression because they have subexpressions that are evaluated in a different context from the
     * calling expression.</p>
     *
     * @param pathMap        the PathMap to which the expression should be added
     * @param pathMapNodeSet the PathMapNodeSet to which the paths embodied in this expression should be added
     * @return the pathMapNodeSet representing the points in the source document that are both reachable by this
     *         expression, and that represent possible results of this expression. For an expression that does
     *         navigation, it represents the end of the arc in the path map that describes the navigation route. For other
     *         expressions, it is the same as the input pathMapNode.
     */

    public PathMap.PathMapNodeSet addToPathMap(PathMap pathMap, PathMap.PathMapNodeSet pathMapNodeSet) {
        // This logic is assuming the mode is streamable (so called templates can't return streamed nodes)
        //PathMap.PathMapNodeSet result = super.addToPathMap(pathMap, pathMapNodeSet);
        //result.setReturnable(false);
        if (pathMapNodeSet == null) {
            ContextItemExpression cie = new ContextItemExpression();
            //cie.setContainer(getContainer());
            pathMapNodeSet = new PathMap.PathMapNodeSet(pathMap.makeNewRoot(cie));
        }
        pathMapNodeSet.addDescendants();
        return new PathMap.PathMapNodeSet(pathMap.makeNewRoot(this));
    }


    public TailCall processLeavingTail(XPathContext context) throws XPathException {

        Controller controller = context.getController();
        assert controller != null;
        // handle parameters if any

//        ParameterSet params = (ParameterSet)((ObjectValue)getActualParams().evaluateItem(context)).getObject();
//        ParameterSet tunnels = (ParameterSet)((ObjectValue)getTunnelParams().evaluateItem(context)).getObject();
        ParameterSet params = assembleParams(context, getActualParams());
        ParameterSet tunnels = assembleTunnelParams(context, getTunnelParams());

        Rule currentTemplateRule = context.getCurrentTemplateRule();
        if (currentTemplateRule == null) {
            XPathException e = new XPathException("There is no current template rule");
            e.setXPathContext(context);
            e.setErrorCode("XTDE0560");
            e.setLocation(getLocation());
            throw e;
        }

        int min = currentTemplateRule.getMinImportPrecedence();
        int max = currentTemplateRule.getPrecedence() - 1;
        Component<? extends Mode> modeComponent = context.getCurrentMode();
        if (modeComponent == null) {
            throw new AssertionError("Current mode is null");
        }
//        if (context.getCurrentIterator() == null) {
//            XPathException e = new XPathException("Cannot call xsl:apply-imports when there is no context item");
//            e.setXPathContext(context);
//            e.setErrorCode("XTDE0565");
//            e.setLocation(getLocation());
//            throw e;
//        }
        Item currentItem = context.getCurrentIterator().current();
//        if (!allowAnyItem && !(currentItem instanceof NodeInfo)) {
//            XPathException e = new XPathException("Cannot call xsl:apply-imports when context item is not a node");
//            e.setXPathContext(context);
//            e.setErrorCode("XTDE0565");
//            e.setLocation(getLocation());
//            throw e;
//        }
        Mode mode = modeComponent.getCode();
        Rule rule = mode.getRule(currentItem, min, max, context);
        if (rule == null) {             // use the default action for the node
            mode.getBuiltInRuleSet().process(currentItem, params, tunnels, context, getLocation());
        } else {
            XPathContextMajor c2 = context.newContext();
            TemplateRule nh = (TemplateRule) rule.getAction();
            c2.setOrigin(this);
            //c2.setOriginatingConstructType(Location.TEMPLATE);
            c2.setLocalParameters(params);
            c2.setTunnelParameters(tunnels);
            c2.openStackFrame(nh.getStackFrameMap());
            c2.setCurrentTemplateRule(rule);
            c2.setCurrentComponent(modeComponent);
            nh.apply(c2);
        }
        return null;
        // We never treat apply-imports as a tail call, though we could
    }

    //#ifdefined BYTECODE

    /**
     * Return the compiler of the ApplyImport expression
     *
     * @return the relevant ExpressionCompiler
     */
    @Override
    public ExpressionCompiler getExpressionCompiler() {
        return new ApplyImportsCompiler();
    }
    //#endif


    /**
     * Diagnostic print of expression structure. The abstract expression tree
     * is written to the supplied output destination.
     */

    public void export(ExpressionPresenter out) throws XPathException {
        out.startElement("applyImports", this);
        if (allowAnyItem) {
            out.emitAttribute("flags", "i");
        }
        if (getActualParams().length != 0) {
            WithParam.exportParameters(getActualParams(), out, false);
        }
        if (getTunnelParams().length != 0) {
            WithParam.exportParameters(getTunnelParams(), out, true);
        }
        out.endElement();
    }

//#ifdefined STREAM
    @Override
    public StreamingAdjunct getStreamingAdjunct() {
        return new ApplyImportsAdjunct();
    }
//#endif


}

