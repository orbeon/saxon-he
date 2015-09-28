package net.sf.saxon.expr;

import com.saxonica.ee.bytecode.ExpressionCompiler;
import com.saxonica.ee.bytecode.TryCatchExpressionCompiler;
import com.saxonica.ee.stream.adjunct.StreamingAdjunct;
import com.saxonica.ee.stream.adjunct.TryCatchAdjunct;
import net.sf.saxon.expr.parser.ContextItemStaticInfo;
import net.sf.saxon.expr.parser.ExpressionTool;
import net.sf.saxon.expr.parser.ExpressionVisitor;
import net.sf.saxon.expr.parser.PromotionOffer;
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


/**
 * This class implements a try/catch expression. It consists of a try expression, and a sequence of Nametest/Catch
 * expression pairs. If the try expression succeeds, its result is returned; otherwise the error code of the
 * exception is matched against each of the Nametests in turn, and the first matching catch expression is
 * evaluated.
 */

public class TryCatch extends Expression {

    private Expression tryExpr;
    private List<CatchClause> catchClauses = new ArrayList<CatchClause>();

    public TryCatch(Expression tryExpr) {
        this.tryExpr = tryExpr;
    }

    public void addCatchExpression(QNameTest test, Expression catchExpr) {
        CatchClause clause = new CatchClause();
        clause.catchExpr = catchExpr;
        clause.nameTest = test;
        catchClauses.add(clause);
    }

    /**
     * Get the "try" expression
     *
     * @return the primary expression to be evaluated
     */
    public Expression getTryExpr() {
        return tryExpr;
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
        int card = tryExpr.getCardinality();
        for (CatchClause catchClause : catchClauses) {
            card = Cardinality.union(card, catchClause.catchExpr.getCardinality());
        }
        return card;
    }

    /**
     * Determine the item type of the value returned by the function
     */

    /*@NotNull*/
    public ItemType getItemType() {
        ItemType type = tryExpr.getItemType();
        for (CatchClause catchClause : catchClauses) {
            type = Type.getCommonSuperType(type, catchClause.catchExpr.getItemType());
        }
        return type;
    }

    /**
     * Perform type checking of an expression and its subexpressions. This is the second phase of
     * static optimization.
     * <p/>
     * <p>This checks statically that the operands of the expression have
     * the correct type; if necessary it generates code to do run-time type checking or type
     * conversion. A static type error is reported only if execution cannot possibly succeed, that
     * is, if a run-time type error is inevitable. The call may return a modified form of the expression.</p>
     * <p/>
     * <p>This method is called after all references to functions and variables have been resolved
     * to the declaration of the function or variable. However, the types of such functions and
     * variables may not be accurately known if they have not been explicitly declared.</p>
     * <p/>
     * <p>If the implementation returns a value other than "this", then it is required to ensure that
     * the location information in the returned expression have been set up correctly.
     * It should not rely on the caller to do this, although for historical reasons many callers do so.</p>
     *
     *
     * @param visitor         an expression visitor
     * @param contextInfo
     * @return the original expression, rewritten to perform necessary run-time type checks,
     *         and to perform other type-related optimizations
     * @throws net.sf.saxon.trans.XPathException
     *          if an error is discovered during this phase
     *          (typically a type error)
     */

    /*@NotNull*/
    public Expression typeCheck(ExpressionVisitor visitor, ContextItemStaticInfo contextInfo) throws XPathException {
        tryExpr = visitor.typeCheck(tryExpr,  contextInfo);
        for (CatchClause clause : catchClauses) {
            Expression e0 = clause.catchExpr;
            Expression e1 = visitor.typeCheck(e0,  contextInfo);
            if (e0 != e1) {
                ExpressionTool.copyLocationInfo(e0, e1);
            }
            clause.catchExpr = e1;
        }
        return this;
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
        list.add(new Operand(tryExpr, OperandRole.SAME_FOCUS_ACTION));
        for (CatchClause cc : catchClauses) {
            list.add(new Operand(cc.catchExpr, OperandRole.SAME_FOCUS_ACTION));
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
     * @param offer  details of the offer, for example the offer to move
     *               expressions that don't depend on the context to an outer level in
     *               the containing expression
     * @param parent the parent of the current expression
     * @return if the offer is not accepted, return this expression unchanged.
     *         Otherwise return the result of rewriting the expression to promote
     *         this subexpression
     * @throws net.sf.saxon.trans.XPathException
     *          if any error is detected
     */

    public Expression promote(PromotionOffer offer, Expression parent) throws XPathException {
        if (offer.action != PromotionOffer.EXTRACT_GLOBAL_VARIABLES &&
                offer.action != PromotionOffer.FOCUS_INDEPENDENT) {
            Expression exp = offer.accept(parent, this);
            if (exp != null) {
                return exp;
            } else {
                tryExpr = doPromotion(tryExpr, offer);
                for (CatchClause clause : catchClauses) {
                    clause.catchExpr = doPromotion(clause.catchExpr, offer);
                }
                return this;
            }
        } else {
            return this;
        }
    }

    /**
     * Replace one subexpression by a replacement subexpression
     *
     * @param original    the original subexpression
     * @param replacement the replacement subexpression
     * @return true if the original subexpression is found
     */

    public boolean replaceOperand(Expression original, Expression replacement) {
        boolean found = false;
        if (tryExpr == original) {
            tryExpr = replacement;
            found = true;
        }
        for (CatchClause clause : catchClauses) {
            if (clause.catchExpr == original) {
                clause.catchExpr = replacement;
                found = true;
            }
        }
        return found;
    }

    /**
     * Copy an expression. This makes a deep copy.
     *
     * @return the copy of the original expression
     */

    /*@NotNull*/
    public Expression copy() {
        TryCatch t2 = new TryCatch(tryExpr.copy());
        for (CatchClause clause : catchClauses) {
            t2.addCatchExpression(clause.nameTest, clause.catchExpr.copy());
        }
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
            return ExpressionTool.eagerEvaluate(tryExpr, c1).head();
        } catch (XPathException err) {
            if (err.isGlobalError()) {
                err.setIsGlobalError(false);
            } else {
                StructuredQName code = err.getErrorCodeQName();
                for (CatchClause clause : catchClauses) {
                    if (clause.nameTest.matches(code)) {
                        Expression caught = clause.catchExpr;
                        XPathContextMinor c2 = c.newMinorContext();
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
            Sequence v = ExpressionTool.eagerEvaluate(tryExpr, c1);
            c1.notifyChildThreads();
            return v.iterate();
        } catch (XPathException err) {
            if (err.isGlobalError()) {
                err.setIsGlobalError(false);
            } else {
                StructuredQName code = err.getErrorCodeQName();
                for (CatchClause clause : catchClauses) {
                    if (clause.nameTest.matches(code)) {
                        Expression caught = clause.catchExpr;
                        XPathContextMinor c2 = c.newMinorContext();
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

    public void explain(ExpressionPresenter out) {
        out.startElement("tryCatch");
        out.startSubsidiaryElement("try");
        tryExpr.explain(out);
        out.endSubsidiaryElement();
        for (CatchClause clause : catchClauses) {
            out.startSubsidiaryElement("catch");
            out.emitAttribute("error", clause.nameTest.toString());
            clause.catchExpr.explain(out);
            out.endSubsidiaryElement();
        }
        out.endElement();
    }

    public static class CatchClause {
        public int slotNumber = -1;
        public Expression catchExpr;
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

// Copyright (c) 2014 Saxonica Limited. All Rights Reserved