package net.sf.saxon.expr;

import net.sf.saxon.Configuration;
import net.sf.saxon.instruct.ApplyTemplates;
import net.sf.saxon.instruct.Choose;
import net.sf.saxon.instruct.Template;
import net.sf.saxon.om.SequenceIterator;
import net.sf.saxon.om.ValueRepresentation;
import net.sf.saxon.pattern.NodeTest;
import net.sf.saxon.sort.DocumentSorter;
import net.sf.saxon.trans.RuleTarget;
import net.sf.saxon.trans.XPathException;
import net.sf.saxon.type.ItemType;
import net.sf.saxon.type.TypeHierarchy;
import net.sf.saxon.value.Closure;
import net.sf.saxon.value.MemoClosure;
import net.sf.saxon.value.SequenceExtent;
import net.sf.saxon.value.Value;

import java.io.Serializable;
import java.util.Iterator;

/**
 * This class performs optimizations that vary between different versions of the Saxon product.
 * The optimizer is obtained from the Saxon Configuration. This class is the version used in Saxon-B,
 * which in most cases does no optimization at all: the methods are provided so that they can be
 * overridden in Saxon-EE.
 */
public class Optimizer implements Serializable {

    public static final int NO_OPTIMIZATION = 0;
    public static final int FULL_OPTIMIZATION = 10;

    protected Configuration config;
    private int optimizationLevel = FULL_OPTIMIZATION;

    /**
     * Create an Optimizer.
     * @param config the Saxon configuration
     */

    public Optimizer(Configuration config) {
        this.config = config;
    }

    /**
     * Get the Saxon configuration object
     * @return the configuration
     */

    public Configuration getConfiguration() {
        return config;
    }

    /**
     * Set the optimization level
     * @param level the optimization level, between 0 (no optimization) and 10 (full optimization).
     * Currently all values greater than zero have the same effect as full optimization
     */

    public void setOptimizationLevel(int level) {
        if (level<NO_OPTIMIZATION || level>FULL_OPTIMIZATION) {
            throw new IllegalArgumentException("Optimization level");
        }
        optimizationLevel = level;
    }

    /**
     * Get the optimization level
     * @return the optimization level, between 0 (no optimization) and 10 (full optimization).
     * Currently all values greater than zero have the same effect as full optimization
     */

    public int getOptimizationLevel() {
        return optimizationLevel;
    }

    /**
     * Simplify a GeneralComparison expression
     * @param gc the GeneralComparison to be simplified
     * @param backwardsCompatible true if in 1.0 compatibility mode
     * @return the simplified expression
     */

    public BinaryExpression simplifyGeneralComparison(GeneralComparison gc, boolean backwardsCompatible) {
        if (backwardsCompatible) {
            Expression[] operands = gc.getOperands();
            GeneralComparison10 gc10 = new GeneralComparison10(operands[0], gc.getOperator(), operands[1]);
            gc10.setAtomicComparer(gc.getAtomicComparer());
            return gc10;
        } else {
            return gc;
        }
    }


    /**
     * Attempt to optimize a copy operation. Return null if no optimization is possible.
     * @param select the expression that selects the items to be copied
     * @return null if no optimization is possible, or an expression that does an optimized
     * copy of these items otherwise
     */

    public Expression optimizeCopy(Expression select) throws XPathException {
        final TypeHierarchy th = config.getTypeHierarchy();
        if (select.getItemType(th).isAtomicType()) {
            return select;
        }
        return null;
    }

    /**
     * Make a Closure, given the expected reference count
     * @param expression the expression to be evaluated
     * @param ref the (nominal) number of times the value of the expression is required
     * @param context the XPath dynamic evaluation context
     * @return the constructed Closure
     */

    public Value makeClosure(Expression expression, int ref, XPathContext context) throws XPathException {
        if (ref == 1) {
            return new Closure();
        } else {
            return new MemoClosure();
        }
    }

    /**
     * Make a SequenceExtent, given the expected reference count
     * @param expression the expression to be evaluated
     * @param ref the (nominal) number of times the value of the expression is required
     * @param context the XPath dynamic evaluation context
     * @return the constructed Closure
     */

    public ValueRepresentation makeSequenceExtent(Expression expression, int ref, XPathContext context) throws XPathException {
        return SequenceExtent.makeSequenceExtent(expression.iterate(context));
    }    

    /**
     * Examine a path expression to see whether it can be replaced by a call on the key() function;
     * if so, generate an appropriate key definition and return the call on key(). If not, return null.
     * @param pathExp The path expression to be converted.
     * @param visitor The expression visitor
     * @return the optimized expression, or null if no optimization is possible
     */

    public Expression convertPathExpressionToKey(PathExpression pathExp, ExpressionVisitor visitor)
    throws XPathException {
        return null;
    }

    /**
     * Try converting a filter expression to a call on the key function. Return the supplied
     * expression unchanged if not possible
     * @param f the filter expression to be converted
     * @param visitor the expression visitor, which must be currently visiting the filter expression f
     * @param indexFirstOperand true if the first operand of the filter comparison is to be indexed;
     * false if it is the second operand
     * @return the optimized expression, or the unchanged expression f if no optimization is possible
     */

    public Expression tryIndexedFilter(FilterExpression f, ExpressionVisitor visitor, boolean indexFirstOperand) {
        return f;
    }

    /**
     * Convert a path expression such as a/b/c[predicate] into a filter expression
     * of the form (a/b/c)[predicate]. This is possible whenever the predicate is non-positional.
     * The conversion is useful in the case where the path expression appears inside a loop,
     * where the predicate depends on the loop variable but a/b/c does not.
     * @param pathExp the path expression to be converted
     * @param th the type hierarchy cache
     * @return the resulting filterexpression if conversion is possible, or null if not
     */

    public FilterExpression convertToFilterExpression(PathExpression pathExp, TypeHierarchy th)
    throws XPathException {
        return null;
    }

    /**
     * Test whether a filter predicate is indexable.
     * @param filter the predicate expression
     * @return 0 if not indexable; +1 if the predicate is in the form expression=value; -1 if it is in
     * the form value=expression
     */

    public int isIndexableFilter(Expression filter) {
        return 0;
    }

    /**
     * Create an indexed value
     * @param iter the iterator that delivers the sequence of values to be indexed
     * @return the indexed value
     * @throws UnsupportedOperationException: this method should not be called in Saxon-B
     */

    public ValueRepresentation makeIndexedValue(SequenceIterator iter) throws XPathException {
        throw new UnsupportedOperationException("Indexing requires Saxon-EE");
    }

    /**
     * Determine whether it is possible to rearrange an expression so that all references to a given
     * variable are replaced by a reference to ".". This is true of there are no references to the variable
     * within a filter predicate or on the rhs of a "/" operator.
     * @param exp the expression in question
     * @param binding an array of bindings defining range variables; the method tests that there are no
     * references to any of these variables within a predicate or on the rhs of "/"
     * @return true if the variable reference can be replaced
     */

    public boolean isVariableReplaceableByDot(Expression exp, Binding[] binding) {
        // TODO: the fact that a variable reference appears inside a predicate (etc) shouldn't stop us
        // rewriting a where clause as a predicate. We just have to bind a new variable:
        // for $x in P where abc[n = $x/m] ==> for $x in P[let $z := . return abc[n = $z/m]
        // We could probably do this in all cases and then let $z be optimized away where appropriate
        if (exp instanceof ContextSwitchingExpression) {
            Expression start = ((ContextSwitchingExpression)exp).getControllingExpression();
            Expression step = ((ContextSwitchingExpression)exp).getControlledExpression();
            return isVariableReplaceableByDot(start, binding) &&
                    !ExpressionTool.dependsOnVariable(step, binding);
        } else {
            Iterator iter = exp.iterateSubExpressions();
            while (iter.hasNext()) {
                Expression sub = (Expression)iter.next();
                if (!isVariableReplaceableByDot(sub, binding)) {
                    return false;
                }
            }
            return true;
        }
    }


    /**
     * Make a conditional document sorter. This optimization is attempted
     * when a DocumentSorter is wrapped around a path expression
     * @param sorter the document sorter
     * @param path the path expression
     * @return the original sorter unchanged when no optimization is possible, which is always the
     * case in Saxon-B
     */

    public Expression makeConditionalDocumentSorter(DocumentSorter sorter, PathExpression path) {
        return sorter;
    }

    /**
     * Replace a function call by the body of the function, assuming all conditions for inlining
     * the function are satisfied
     * @param functionCall the functionCall expression
     * @param visitor the expression visitor
     * @param contextItemType the context item type
     * @return either the original expression unchanged, or an expression that consists of the inlined
     * function body, with all function parameters bound as required. In Saxon-B, function inlining is
     * not supported, so the original functionCall is always returned unchanged
     */

    public Expression tryInlineFunctionCall(
            UserFunctionCall functionCall, ExpressionVisitor visitor, ItemType contextItemType) {
        return functionCall;
    }

    /**
     * Identify expressions within a function or template body that can be promoted to be
     * evaluated as global variables.
     * @param body the body of the template or function
     * @param visitor the expression visitor
     * @param notTopLevel if true, only promote the children of the supplied body expression, not the
     * entire expression
     * @return the expression after subexpressions have been promoted to global variables
     */
    
    public Expression promoteExpressionsToGlobal(Expression body, ExpressionVisitor visitor, boolean notTopLevel) 
    throws XPathException {
        return body;
    }

    /**
     * Try to convert a Choose expression into a switch
     * @param choose the Choose expression
     * @param env the static context
     * @return the result of optimizing this (the original expression if no optimization was possible)
     */

    public Expression trySwitch(Choose choose, StaticContext env) {
        return choose;
    }

    /**
     * Extract subexpressions from the body of a function that can be evaluated
     * as global variables
     * @param body the body of the function
     * @return a reference to the new global variable if a variable has been created, or null if not
     */

    public Expression extractGlobalVariables(Expression body, ExpressionVisitor visitor)
    throws XPathException {
        return null;
    }

    /**
     * Make a streaming applyTemplates instruction. Supported in Saxon-EE only
     * @param inst the unoptimized applyTemplates instruction
     */

    public Expression makeStreamingApplyTemplates(ApplyTemplates inst) throws XPathException {
        return inst;
    }

    /**
     * Generate the inversion of the expression comprising the body of a template rules.
     * Supported in Saxon-EE only
     * @param template the template to be inverted
     * @param nodeTest the static item type of the context node of the template
     */

    public RuleTarget makeInversion(Template template, NodeTest nodeTest) throws XPathException {
        return null;
    }

    /**
     * Trace optimization actions
     * @param message the message to be displayed
     * @param exp the expression after being rewritten
     */

    public void trace(String message, Expression exp) {
        if (getConfiguration().isOptimizerTracing()) {
            System.err.println("OPT ======================================");
            System.err.println("OPT : At line " + exp.getLineNumber() + " of " + exp.getSystemId());
            System.err.println("OPT : " + message);
            System.err.println("OPT ====== Expression after rewrite ======");
            exp.explain(System.err);
            System.err.println("\nOPT ======================================");
        }
    }

    /**
     * Trace optimization actions
     * @param message the message to be displayed
     */

    public void trace(String message) {
        if (getConfiguration().isOptimizerTracing()) {
            System.err.println("OPT ======================================");
            System.err.println("OPT : " + message);
            System.err.println("OPT ======================================");
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

