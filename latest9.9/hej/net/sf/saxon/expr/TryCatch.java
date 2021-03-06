////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
// Copyright (c) 2018 Saxonica Limited.
// This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
// If a copy of the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
// This Source Code Form is "Incompatible With Secondary Licenses", as defined by the Mozilla Public License, v. 2.0.
////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

package net.sf.saxon.expr;

import net.sf.saxon.event.EventBuffer;
import net.sf.saxon.event.EventMonitor;
import net.sf.saxon.expr.instruct.BreakInstr;
import net.sf.saxon.expr.parser.ContextItemStaticInfo;
import net.sf.saxon.expr.parser.ExpressionTool;
import net.sf.saxon.expr.parser.ExpressionVisitor;
import net.sf.saxon.expr.parser.RebindingMap;
import net.sf.saxon.lib.NamespaceConstant;
import net.sf.saxon.om.*;
import net.sf.saxon.pattern.QNameTest;
import net.sf.saxon.trace.ExpressionPresenter;
import net.sf.saxon.trans.XPathException;
import net.sf.saxon.type.ItemType;
import net.sf.saxon.type.Type;
import net.sf.saxon.value.Cardinality;

import javax.xml.transform.ErrorListener;
import javax.xml.transform.TransformerException;
import java.util.ArrayList;
import java.util.List;


/**
 * This class implements a try/catch expression. It consists of a try expression, and a sequence of Nametest/Catch
 * expression pairs. If the try expression succeeds, its result is returned; otherwise the error code of the
 * exception is matched against each of the Nametests in turn, and the first matching catch expression is
 * evaluated.
 */

public class TryCatch extends Expression {

    private Operand tryOp;
    private List<CatchClause> catchClauses = new ArrayList<>();
    private boolean rollbackOutput;

    public TryCatch(Expression tryExpr) {
        this.tryOp = new Operand(this, tryExpr, OperandRole.SAME_FOCUS_ACTION);
    }

    public void addCatchExpression(QNameTest test, Expression catchExpr) {
        CatchClause clause = new CatchClause();
        clause.catchOp = new Operand(this, catchExpr, OperandRole.SAME_FOCUS_ACTION);
        clause.nameTest = test;
        catchClauses.add(clause);
    }

    public void setRollbackOutput(boolean rollback) {
        this.rollbackOutput = rollback;
    }

    public boolean isRollbackOutput() {
        return this.rollbackOutput;
    }

    /**
     * Get the "try" operand
     * @return the primary operand to be evaluated
     */

    public Operand getTryOperand() {
        return tryOp;
    }

    /**
     * Get the "try" expression
     *
     * @return the primary expression to be evaluated
     */
    public Expression getTryExpr() {
        return tryOp.getChildExpression();
    }


    /**
     * Get the list of catch clauses
     *
     * @return the list of catch clauses
     */
    public List<CatchClause> getCatchClauses() {
        return catchClauses;
    }

    /**
     * Ask whether this expression is an instruction. In XSLT streamability analysis this
     * is used to distinguish constructs corresponding to XSLT instructions from other constructs,
     * typically XPath expressions.
     *
     * @return true (if this construct exists at all in an XSLT environment, then it represents an instruction)
     */
    @Override
    public boolean isInstruction() {
        return true;
    }

    /**
     * Ask whether common subexpressions found in the operands of this expression can
     * be extracted and evaluated outside the expression itself. The result is irrelevant
     * in the case of operands evaluated with a different focus, which will never be
     * extracted in this way, even if they have no focus dependency.
     *
     * @return false for this kind of expression
     */
    @Override
    public boolean allowExtractingCommonSubexpressions() {
        return false;
    }

    /**
     * Determine the cardinality of the function.
     */

    public int computeCardinality() {
        int card = getTryExpr().getCardinality();
        for (CatchClause catchClause : catchClauses) {
            card = Cardinality.union(card, catchClause.catchOp.getChildExpression().getCardinality());
        }
        return card;
    }

    /**
     * Determine the item type of the value returned by the function
     */

    /*@NotNull*/
    public ItemType getItemType() {
        ItemType type = getTryExpr().getItemType();
        for (CatchClause catchClause : catchClauses) {
            type = Type.getCommonSuperType(type, catchClause.catchOp.getChildExpression().getItemType());
        }
        return type;
    }

    /**
     * Get the immediate sub-expressions of this expression, with information about the relationship
     * of each expression to its parent expression. Default implementation
     * returns a zero-length array, appropriate for an expression that has no
     * sub-expressions.
     *
     * @return an iterator containing the sub-expressions of this expression
     */
    @Override
    public Iterable<Operand> operands() {
        List<Operand> list = new ArrayList<>();
        list.add(tryOp);
        for (CatchClause cc : catchClauses) {
            list.add(cc.catchOp);
        }
        return list;
    }

    @Override
    public Expression optimize(ExpressionVisitor visitor, ContextItemStaticInfo contextInfo) throws XPathException {
        optimizeChildren(visitor, contextInfo);
        Expression e = getParentExpression();
        while (e != null) {
            if (e instanceof LetExpression && ExpressionTool.dependsOnVariable(getTryExpr(), new Binding[]{(LetExpression)e})) {
                ((LetExpression)e).setNeedsEagerEvaluation(true);
            }
            e = e.getParentExpression();
        }
        return this;
    }

    /**
     * An implementation of Expression must provide at least one of the methods evaluateItem(), iterate(), or process().
     * This method indicates which of these methods is provided directly. The other methods will always be available
     * indirectly, using an implementation that relies on one of the other methods.
     *
     * @return the implementation method, for example {@link #ITERATE_METHOD} or {@link #EVALUATE_METHOD} or
     * {@link #PROCESS_METHOD}
     */
    @Override
    public int getImplementationMethod() {
        return ITERATE_METHOD;
    }

    /**
     * Is this expression the same as another expression?
     *
     * @param other the expression to be compared with this one
     * @return true if the two expressions are statically equivalent
     */
    @Override
    public boolean equals(Object other) {
        return other instanceof TryCatch && ((TryCatch)other).tryOp.getChildExpression().isEqual(tryOp.getChildExpression())
                && ((TryCatch)other).catchClauses.equals(catchClauses);
    }

    /**
     * Hashcode supporting equals()
     */

    public int computeHashCode() {
        int h = 0x836b12a0;
        for (int i = 0; i < catchClauses.size(); i++) {
            h ^= catchClauses.get(i).hashCode()<<i;
        }
        return h + tryOp.getChildExpression().hashCode();
    }

    /**
     * Copy an expression. This makes a deep copy.
     *
     * @return the copy of the original expression
     * @param rebindings variables that need to be re-bound
     */

    /*@NotNull*/
    public Expression copy(RebindingMap rebindings) {
        TryCatch t2 = new TryCatch(tryOp.getChildExpression().copy(rebindings));
        for (CatchClause clause : catchClauses) {
            t2.addCatchExpression(clause.nameTest, clause.catchOp.getChildExpression().copy(rebindings));
        }
        t2.setRollbackOutput(rollbackOutput);
        ExpressionTool.copyLocationInfo(this, t2);
        return t2;
    }

    /**
     * Evaluate as a singleton item
     *
     * @param c the dynamic XPath evaluation context
     */

    public Item evaluateItem(XPathContext c) throws XPathException {
        XPathContext c1 = c.newMinorContext();
        try {
            return ExpressionTool.eagerEvaluate(tryOp.getChildExpression(), c1).head();
        } catch (XPathException err) {
            if (err.isGlobalError()) {
                err.setIsGlobalError(false);
            } else {
                StructuredQName code = err.getErrorCodeQName();
                if (code == null) {
                    code = new StructuredQName("saxon", NamespaceConstant.SAXON, "SXWN9000");
                }
                for (CatchClause clause : catchClauses) {
                    if (clause.nameTest.matches(code)) {
                        Expression caught = clause.catchOp.getChildExpression();
                        XPathContextMajor c2 = c.newContext();
                        c2.setCurrentException(err);
                        return caught.evaluateItem(c2);
                    }
                }
            }
            err.setHasBeenReported(false);
            throw err;
        }
    }

    /**
     * Iterate over the results of the function
     */

    /*@NotNull*/
    public SequenceIterator<?> iterate(XPathContext c) throws XPathException {
        XPathContextMajor c1 = c.newContext();
        c1.createThreadManager();
        c1.setErrorListener(new FilteringErrorListener(c.getErrorListener()));
        try {
            // Need to do eager iteration of the first argument to flush any errors out
            Sequence<?> v = ExpressionTool.eagerEvaluate(tryOp.getChildExpression(), c1);
            c1.waitForChildThreads();
            // check for xsl:break within xsl:try - test iterate-035
            TailCallLoop.TailCallInfo tci = c1.getTailCallInfo();
            if (tci instanceof BreakInstr) {
                ((BreakInstr)tci).markContext(c);
            }
            return v.iterate();
        } catch (XPathException err) {
            if (err.isGlobalError()) {
                err.setIsGlobalError(false);
            } else {
                StructuredQName code = err.getErrorCodeQName();
                for (CatchClause clause : catchClauses) {
                    if (code == null) {
                        code = new StructuredQName("saxon", NamespaceConstant.SAXON, "SXWN9000");
                    }
                    if (clause.nameTest.matches(code)) {
                        Expression caught = clause.catchOp.getChildExpression();
                        XPathContextMajor c2 = c.newContext();
                        c2.setCurrentException(err);
                        // check for xsl:break within xsl:catch - test iterate-036
                        Sequence<?> v = ExpressionTool.eagerEvaluate(caught, c2);
                        TailCallLoop.TailCallInfo tci = c2.getTailCallInfo();
                        if (tci instanceof BreakInstr) {
                            ((BreakInstr) tci).markContext(c);
                        }
                        return v.iterate();
                    }
                }
            }
            err.setHasBeenReported(false);
            throw err;
        }
    }

    @Override
    public void process(XPathContext context) throws XPathException {
        XPathContextMajor c1 = context.newContext();
        c1.createThreadManager();
        c1.setErrorListener(new FilteringErrorListener(context.getErrorListener()));
        EventBuffer buffer = null;
        EventMonitor monitor = null;
        if (rollbackOutput) {
            buffer = new EventBuffer(context.getReceiver().getPipelineConfiguration());
            c1.setReceiver(buffer);
        } else {
            monitor = new EventMonitor(context.getReceiver());
            c1.setReceiver(monitor);
        }
        try {
            tryOp.getChildExpression().process(c1);
            c1.waitForChildThreads();
            // check for xsl:break within xsl:try - test iterate-035
            TailCallLoop.TailCallInfo tci = c1.getTailCallInfo();
            if (tci instanceof BreakInstr) {
                ((BreakInstr) tci).markContext(context);
            }
            if (rollbackOutput) {
                buffer.replay(context.getReceiver());
            }
        } catch (XPathException err) {
            if (err.isGlobalError()) {
                err.setIsGlobalError(false);
            } else {
                StructuredQName code = err.getErrorCodeQName();
                if (code == null) {
                    code = new StructuredQName("saxon", NamespaceConstant.SAXON, "SXWN9000");
                }
                for (CatchClause clause : catchClauses) {
                    if (clause.nameTest.matches(code)) {
                        if (monitor != null && monitor.hasBeenWrittenTo()) {
                            // rollback=no was specified, and output has been written, so we cannot recover
                            String message = err.getMessage() +
                                    ". The error could not be caught, because rollback-output=no was specified, and output was already written to the result tree";
                            XPathException xe = new XPathException(message, "XTDE3530");
                            xe.setLocation(err.getLocator());
                            xe.setXPathContext(context);
                            throw xe;
                        }
                        Expression caught = clause.catchOp.getChildExpression();
                        XPathContextMajor c2 = context.newContext();
                        c2.setCurrentException(err);
                        // check for xsl:break within xsl:catch - test iterate-036
                        Sequence<?> v = ExpressionTool.eagerEvaluate(caught, c2);
                        TailCallLoop.TailCallInfo tci = c2.getTailCallInfo();
                        if (tci instanceof BreakInstr) {
                            ((BreakInstr) tci).markContext(context);
                        }
                        ((GroundedValue<?>) v).iterate().forEachOrFail(
                                item -> c2.getReceiver().append(item)
                        );
                        return;
                    }
                }
            }
            err.setHasBeenReported(false);
            throw err;

        }

    }

    /**
     * Get a name identifying the kind of expression, in terms meaningful to a user.
     *
     * @return a name identifying the kind of expression, in terms meaningful to a user.
     *         The name will always be in the form of a lexical XML QName, and should match the name used
     *         in explain() output displaying the expression.
     */
    @Override
    public String getExpressionName() {
        return "tryCatch";    // used in ExpressionVisitor
    }

    /**
     * Diagnostic print of expression structure. The abstract expression tree
     * is written to the supplied output destination.
     *
     * @param out the expression presenter used to display the structure
     */

    public void export(ExpressionPresenter out) throws XPathException {
        out.startElement("try", this);
        if (rollbackOutput) {
            out.emitAttribute("flags", "r");
        }
        tryOp.getChildExpression().export(out);
        for (CatchClause clause : catchClauses) {
            out.startElement("catch");
            out.emitAttribute("err", clause.nameTest.toString());
            if ("JS".equals(((ExpressionPresenter.ExportOptions) out.getOptions()).target)) {
                int targetVersion = ((ExpressionPresenter.ExportOptions) out.getOptions()).targetVersion;
                out.emitAttribute("test", clause.nameTest.generateJavaScriptNameTest(targetVersion));
            }
            clause.catchOp.getChildExpression().export(out);
            out.endElement();
        }
        out.endElement();
    }

    public static class CatchClause {
        public int slotNumber = -1;
        public Operand catchOp;
        public QNameTest nameTest;
    }

    /**
     * An error listener that filters out reporting of any errors that are caught be the try/catch
     */

    private class FilteringErrorListener implements ErrorListener {

        private ErrorListener base;

        FilteringErrorListener(ErrorListener base) {
            this.base = base;
        }

        private boolean isCaught(TransformerException err) {
            if (err instanceof XPathException) {
                StructuredQName code = ((XPathException) err).getErrorCodeQName();
                for (CatchClause clause : catchClauses) {
                    if (clause.nameTest.matches(code)) {
                        return true;
                    }
                }
            }
            return false;
        }

        @Override
        public void warning(TransformerException exception) throws TransformerException {
            base.warning(exception);
        }

        @Override
        public void error(TransformerException exception) throws TransformerException {
            if (!isCaught(exception)) {
                base.error(exception);
            }
        }

        @Override
        public void fatalError(TransformerException exception) throws TransformerException {
            if (!isCaught(exception)) {
                base.fatalError(exception);
            }
        }
    }

    /**
     * Get the (partial) name of a class that supports streaming of this kind of expression
     *
     * @return the partial name of a class that can be instantiated to provide streaming support in Saxon-EE,
     * or null if there is no such class
     */
    @Override
    public String getStreamerName() {
        return "TryCatch";
    }
}

