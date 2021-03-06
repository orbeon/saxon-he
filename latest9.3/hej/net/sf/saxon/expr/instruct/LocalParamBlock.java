package net.sf.saxon.expr.instruct;

import net.sf.saxon.expr.*;
import net.sf.saxon.pattern.EmptySequenceTest;
import net.sf.saxon.trace.ExpressionPresenter;
import net.sf.saxon.trans.XPathException;
import net.sf.saxon.type.ItemType;
import net.sf.saxon.type.TypeHierarchy;

import java.util.Arrays;
import java.util.Iterator;


/**
* Represents the set of xsl:param elements at the start of an xsl:iterate instruction
*/

public class LocalParamBlock extends Instruction {

    private LocalParam[] children;

    /**
     * Create the block of parameters
     */

    public LocalParamBlock(LocalParam[] params) {
        this.children = params;
        for (int c=0; c<children.length; c++) {
            adoptChildExpression(children[c]);
        }
    }

    public String getExpressionName() {
        return "block";
    }

    /**
    * Get the children of this instruction
    * @return the children of this instruction, as an array of Instruction objects. May return
     * a zero-length array if there are no children
    */

    public LocalParam[] getChildren() {
        return children;
    }


    public int computeSpecialProperties() {
        return 0;
    }

    public Iterator<Expression> iterateSubExpressions() {
        return Arrays.asList((Expression[])children).iterator();
    }

    /**
     * Replace one subexpression by a replacement subexpression
     * @param original the original subexpression
     * @param replacement the replacement subexpression
     * @return true if the original subexpression is found
     */

    public boolean replaceSubExpression(Expression original, Expression replacement) {
        boolean found = false;
        if (replacement instanceof LocalParam) {
            for (int c=0; c<children.length; c++) {
                if (children[c] == original) {
                    children[c] = (LocalParam)replacement;
                    found = true;
                }
            }
        }
        return found;
    }


    /**
     * Copy an expression. This makes a deep copy.
     *
     * @return the copy of the original expression
     */

    public Expression copy() {
        LocalParam[] c2 = new LocalParam[children.length];
        for (int c=0; c<children.length; c++) {
            c2[c] = (LocalParam)children[c].copy();
        }
        return new LocalParamBlock(c2);
    }

    /**
     * Determine the data type of the items returned by this expression
     * @return the data type
     * @param th the type hierarchy cache
     */

    public final ItemType getItemType(TypeHierarchy th) {
        return EmptySequenceTest.getInstance();
    }

    /**
     * Determine the cardinality of the expression
     */

    public final int getCardinality() {
        return StaticProperty.EMPTY;
    }

     /**
     * Simplify an expression. This performs any static optimization (by rewriting the expression
     * as a different expression). The default implementation does nothing.
     *
     * @exception net.sf.saxon.trans.XPathException if an error is discovered during expression
     *     rewriting
     * @return the simplified expression
     * @param visitor an expression visitor
     */

    public Expression simplify(ExpressionVisitor visitor) throws XPathException {
        for (int c=0; c<children.length; c++) {
            children[c] = (LocalParam)visitor.simplify(children[c]);
            adoptChildExpression(children[c]);
        }
        return this;
    }

     public Expression typeCheck(ExpressionVisitor visitor, ItemType contextItemType) throws XPathException {
        for (int c=0; c<children.length; c++) {
            children[c] = (LocalParam)visitor.typeCheck(children[c], contextItemType);
            adoptChildExpression(children[c]);
        }
        return this;
    }

    public Expression optimize(ExpressionVisitor visitor, ItemType contextItemType) throws XPathException {
        for (int c=0; c<children.length; c++) {
            children[c] = (LocalParam)visitor.optimize(children[c], contextItemType);
            adoptChildExpression(children[c]);
        }
        return this;
    }



    /**
     * Handle promotion offers, that is, non-local tree rewrites.
     * @param offer The type of rewrite being offered
     * @throws net.sf.saxon.trans.XPathException
     */

    protected void promoteInst(PromotionOffer offer) throws XPathException {
        for (int c=0; c<children.length; c++) {
            LocalParam p = children[c];
            p.setSelectExpression(doPromotion(p.getSelectExpression(), offer));
        }
    }

    /**
     * Diagnostic print of expression structure. The abstract expression tree
     * is written to the supplied output destination.
     */

    public void explain(ExpressionPresenter out) {
        out.startElement("params");
        for (int c=0; c<children.length; c++) {
            children[c].explain(out);
        }
        out.endElement();
    }


    public TailCall processLeavingTail(XPathContext context) throws XPathException {
        for (int i=0; i<children.length; i++) {
            try {
                LocalParam param = children[i];
                context.setLocalVariable(param.getSlotNumber(), param.getSelectValue(context));
            } catch (XPathException e) {
                e.maybeSetLocation(children[i]);
                e.maybeSetContext(context);
                throw e;
            }
        }
    	return null;
    }

    /**
     * An implementation of Expression must provide at least one of the methods evaluateItem(), iterate(), or process().
     * This method indicates which of these methods is provided. This implementation provides both iterate() and
     * process() methods natively.
     */

    public int getImplementationMethod() {
        return PROCESS_METHOD;
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