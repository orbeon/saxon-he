package net.sf.saxon.expr.flwor;

import net.sf.saxon.expr.*;
import net.sf.saxon.expr.parser.ExpressionTool;
import net.sf.saxon.expr.parser.ExpressionVisitor;
import net.sf.saxon.trace.ExpressionPresenter;
import net.sf.saxon.trans.XPathException;
import net.sf.saxon.tree.util.FastStringBuffer;
import net.sf.saxon.type.ItemType;
import net.sf.saxon.type.TypeHierarchy;

import java.util.List;

/**
 * A "where" clause in a FLWOR expression
 */
public class WhereClause extends Clause {

    private Expression predicate;

    public WhereClause(Expression predicate) {
        this.predicate = predicate;
    }

    @Override
    public int getClauseKey() {
        return WHERE;
    }

    public Expression getPredicate(){
        return predicate;
    }

    public void setPredicate(Expression predicate){
        this.predicate = predicate;
    }

    public WhereClause copy() {
        return new WhereClause(predicate.copy());
    }

      /**
     * Type-check the expression
     */
    @Override
    public void typeCheck(ExpressionVisitor visitor) throws XPathException {
        super.typeCheck(visitor);
    }


    /**
     * Get a tuple stream that implements the functionality of this clause, taking its
     * input from another tuple stream which this clause modifies
     * @param base the input tuple stream
     * @param context the dynamic evaluation context
     * @return the output tuple stream
     */
    @Override
    public TuplePull getPullStream(TuplePull base, XPathContext context) {
        return new WhereClausePull(base, predicate);
    }

    @Override
    public void gatherVariableReferences(final ExpressionVisitor visitor, Binding binding, List<VariableReference> references){
        ExpressionTool.gatherVariableReferences(predicate, binding, references);
    }

     @Override
    public void refineVariableType(ExpressionVisitor visitor, List<VariableReference> references, Expression returnExpr) {
        final TypeHierarchy th = visitor.getConfiguration().getTypeHierarchy();
        final ItemType actualItemType = predicate.getItemType(th);
         for (VariableReference ref : references) {
             ref.refineVariableType(actualItemType, predicate.getCardinality(),
                     (predicate instanceof Literal ? ((Literal) predicate).getValue() : null),
                     predicate.getSpecialProperties(), visitor);
             visitor.resetStaticProperties();
         }
    }

    /**
     * Get a push-mode tuple stream that implements the functionality of this clause, supplying its
     * output to another tuple stream
     *
     * @param destination the output tuple stream
     * @param context  the dynamic evaluation context
     * @return the push tuple stream that implements the functionality of this clause of the FLWOR
     *         expression
     */
    @Override
    public TuplePush getPushStream(TuplePush destination, XPathContext context) {
        return new WhereClausePush(destination, predicate);
    }

    /**
     * Process the subexpressions of this clause
    * @param processor the expression processor used to process the subexpressions
    *
    */
    @Override
    public void processSubExpressions(ExpressionProcessor processor) throws XPathException {
        predicate = processor.processExpression(predicate);
    }

    public  Expression getBaseExpression(){
        return predicate;
    }

    /**
     * Diagnostic print of expression structure. The abstract expression tree
     * is written to the supplied output destination.
     *
     * @param out the expression presenter used to display the structure
     */
    @Override
    public void explain(ExpressionPresenter out) {
        out.startElement("where");
        predicate.explain(out);
        out.endElement();
    }

    public String toString() {
        FastStringBuffer fsb = new FastStringBuffer(FastStringBuffer.SMALL);
        fsb.append("where ");
        fsb.append(predicate.toString());
        return fsb.toString();
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
// The Original Code is: all this file
//
// The Initial Developer of the Original Code is Saxonica Limited.
// Portions created by ___ are Copyright (C) ___. All rights reserved.
//
// Contributor(s):
//