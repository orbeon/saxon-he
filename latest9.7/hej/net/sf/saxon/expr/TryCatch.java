////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
// Copyright (c) 2015 Saxonica Limited.
// This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
// If a copy of the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
// This Source Code Form is "Incompatible With Secondary Licenses", as defined by the Mozilla Public License, v. 2.0.
////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

package net.sf.saxon.expr;

import com.saxonica.ee.bytecode.ExpressionCompiler;
import com.saxonica.ee.bytecode.TryCatchExpressionCompiler;
import com.saxonica.ee.stream.adjunct.StreamingAdjunct;
import com.saxonica.ee.stream.adjunct.TryCatchAdjunct;
import net.sf.saxon.expr.parser.*;
import net.sf.saxon.lib.NamespaceConstant;
import net.sf.saxon.om.Item;
import net.sf.saxon.om.Sequence;
import net.sf.saxon.om.SequenceIterator;
import net.sf.saxon.om.StructuredQName;
import net.sf.saxon.pattern.QNameTest;
import net.sf.saxon.trace.ExpressionPresenter;
import net.sf.saxon.trans.XPathException;
import net.sf.saxon.type.ItemType;
import net.sf.saxon.type.Type;
import net.sf.saxon.value.Cardinality;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;


/**
 * This class implements a try/catch expression. It consists of a try expression, and a sequence of Nametest/Catch
 * expression pairs. If the try expression succeeds, its result is returned; otherwise the error code of the
 * exception is matched against each of the Nametests in turn, and the first matching catch expression is
 * evaluated.
 */

public class TryCatch extends Expression {

    private Operand tryOp;
    private List<CatchClause> catchClauses = new ArrayList<CatchClause>();

    public TryCatch(Expression tryExpr) {
        this.tryOp = new Operand(this, tryExpr, OperandRole.SAME_FOCUS_ACTION);
    }

    public void addCatchExpression(QNameTest test, Expression catchExpr) {
        CatchClause clause = new CatchClause();
        clause.catchOp = new Operand(this, catchExpr, OperandRole.SAME_FOCUS_ACTION);
        clause.nameTest = test;
        catchClauses.add(clause);
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
        List<Operand> list = new ArrayList<Operand>();
        list.add(tryOp);
        for (CatchClause cc : catchClauses) {
            list.add(cc.catchOp);
        }
        return list;
    }

    /**
     * Offer promotion for this subexpression. The offer will be accepted if the subexpression
     * is not dependent on the factors (e.g. the context item) identified in the PromotionOffer.
     * By default the offer is not accepted - this is appropriate in the case of simple expressions
     * such as constant values and variable references where promotion would give no performance
     * advantage. This method is always called at compile time.
     * <p/>
     * <p>This method must be overridden for any Expression that has subexpressions.</p>
     *
     *
     * @param offer  details of the offer, for example the offer to move
     *               expressions that don't depend on the context to an outer level in
     *               the containing expression
     * @return if the offer is not accepted, return this expression unchanged.
     *         Otherwise return the result of rewriting the expression to promote
     *         this subexpression
     * @throws net.sf.saxon.trans.XPathException
     *          if any error is detected
     */

    public Expression promote(PromotionOffer offer) throws XPathException {
        if (offer.action != PromotionOffer.EXTRACT_GLOBAL_VARIABLES &&
                offer.action != PromotionOffer.FOCUS_INDEPENDENT) {
            Expression exp = offer.accept(this);
            if (exp != null) {
                return exp;
            } else {
                //tryOp.setChildExpression(doPromotion(tryOp.getChildExpression(), offer));
                for (CatchClause clause : catchClauses) {
                    clause.catchOp.setChildExpression(doPromotion(clause.catchOp.getChildExpression(), offer));
                }
                return this;
            }
        } else {
            return this;
        }
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
        return other instanceof TryCatch && ((TryCatch)other).tryOp.getChildExpression().equals(tryOp.getChildExpression())
                && ((TryCatch)other).catchClauses.equals(catchClauses);
    }

    /**
     * Hashcode supporting equals()
     */

    public int hashCode() {
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
     * @param rebindings
     */

    /*@NotNull*/
    public Expression copy(Map<IdentityWrapper<Binding>, Binding> rebindings) {
        TryCatch t2 = new TryCatch(tryOp.getChildExpression().copy(rebindings));
        for (CatchClause clause : catchClauses) {
            t2.addCatchExpression(clause.nameTest, clause.catchOp.getChildExpression().copy(rebindings));
        }
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
                if(code == null) {
                    code = new StructuredQName("err", NamespaceConstant.SAXON,"SXWN9000");
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
    public SequenceIterator iterate(XPathContext c) throws XPathException {
        XPathContextMajor c1 = c.newContext();
        c1.createThreadManager();
        try {
            // Need to do eager iteration of the first argument to flush any errors out
            Sequence v = ExpressionTool.eagerEvaluate(tryOp.getChildExpression(), c1);
            c1.waitForChildThreads();
            return v.iterate();
        } catch (XPathException err) {
            if (err.isGlobalError()) {
                err.setIsGlobalError(false);
            } else {
                StructuredQName code = err.getErrorCodeQName();
                for (CatchClause clause : catchClauses) {
                    if (clause.nameTest.matches(code)) {
                        Expression caught = clause.catchOp.getChildExpression();
                        XPathContextMajor c2 = c.newContext();
                        c2.setCurrentException(err);
                        return caught.iterate(c2);
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
        tryOp.getChildExpression().export(out);
        for (CatchClause clause : catchClauses) {
            out.startElement("catch");
            out.emitAttribute("err", clause.nameTest.toString());
            if ("JS".equals(out.getOption("target"))) {
                out.emitAttribute("test", clause.nameTest.generateJavaScriptNameTest());
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

//#ifdefined BYTECODE

    /**
     * Return the compiler of the TryCatch expression
     *
     * @return the relevant ExpressionCompiler
     */
    @Override
    public ExpressionCompiler getExpressionCompiler() {
        return new TryCatchExpressionCompiler();
    }
//#endif

//#ifdefined STREAM
    /**
     * Get a class that supports streamed evaluation of this expression
     *
     * @return the relevant StreamingAdjunct, or null if none is available
     */
    @Override
    protected StreamingAdjunct getStreamingAdjunct() {
        return new TryCatchAdjunct();
    }
//#endif
}

