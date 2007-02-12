package net.sf.saxon.instruct;
import net.sf.saxon.Configuration;
import net.sf.saxon.expr.*;
import net.sf.saxon.om.EmptyIterator;
import net.sf.saxon.om.Item;
import net.sf.saxon.om.SequenceIterator;
import net.sf.saxon.pattern.EmptySequenceTest;
import net.sf.saxon.trans.DynamicError;
import net.sf.saxon.trans.XPathException;
import net.sf.saxon.type.*;
import net.sf.saxon.value.Cardinality;
import net.sf.saxon.value.EmptySequence;
import net.sf.saxon.value.SequenceExtent;
import net.sf.saxon.value.AtomicValue;

import java.io.PrintStream;
import java.util.*;


/**
* Implements an imaginary xsl:block instruction which simply evaluates
* its contents. Used for top-level templates, xsl:otherwise, etc.
*/

public class Block extends Instruction {

    // TODO: allow the last expression in a Block to be a tail-call of a function, at least in push mode

    private Expression[] children;

    public Block() {
    }

    public static Expression makeBlock(Expression e1, Expression e2) {
        if (e1==null || Literal.isEmptySequence(e1)) {
            return e2;
        }
        if (e2==null || Literal.isEmptySequence(e2)) {
            return e1;
        }
        if (e1 instanceof Block || e2 instanceof Block) {
            Iterator it1 = (e1 instanceof Block ? e1.iterateSubExpressions() : new MonoIterator(e1));
            Iterator it2 = (e2 instanceof Block ? e2.iterateSubExpressions() : new MonoIterator(e2));
            List list = new ArrayList(10);
            while (it1.hasNext()) {
                list.add(it1.next());
            }
            while (it2.hasNext()) {
                list.add(it2.next());
            }
            Expression[] exps = new Expression[list.size()];
            exps = (Expression[])list.toArray(exps);
            Block b = new Block();
            b.setChildren(exps);
            return b;
        } else {
            Expression[] exps = {e1, e2};
            Block b = new Block();
            b.setChildren(exps);
            return b;
        }
    }

    /**
    * Set the children of this instruction
    * @param children The instructions that are children of this instruction
    */

    public void setChildren(Expression[] children) {
        if (children==null || children.length==0) {
            this.children = null;
        } else {
            this.children = children;
            for (int c=0; c<children.length; c++) {
                adoptChildExpression(children[c]);
            }
        }
    }

    /**
    * Get the children of this instruction
    * @return the children of this instruction, as an array of Instruction objects. May return either
     * a zero-length array or null if there are no children
    */

    public Expression[] getChildren() {
        return children;
    }

    public Iterator iterateSubExpressions() {
        if (children == null) {
            return Collections.EMPTY_LIST.iterator();
        } else {
            return Arrays.asList(children).iterator();
        }
    }

    /**
     * Replace one subexpression by a replacement subexpression
     * @param original the original subexpression
     * @param replacement the replacement subexpression
     * @return true if the original subexpression is found
     */

    public boolean replaceSubExpression(Expression original, Expression replacement) {
        boolean found = false;
        for (int c=0; c<children.length; c++) {
            if (children[c] == original) {
                children[c] = replacement;
                found = true;
            };
        }
        return found;
    }


    /**
    * Determine the data type of the items returned by this expression
    * @return the data type
     * @param th
     */

    public final ItemType getItemType(TypeHierarchy th) {
        if (children==null || children.length==0) {
            return EmptySequenceTest.getInstance();
        }
        ItemType t1 = children[0].getItemType(th);
        for (int i=1; i<children.length; i++) {
            t1 = Type.getCommonSuperType(t1, children[i].getItemType(th), th);
            if (t1 instanceof AnyItemType) {
                return t1;  // no point going any further
            }
        }
        return t1;
    }

    /**
     * Determine the cardinality of the expression
     */

    public final int getCardinality() {
        if (children==null || children.length==0) {
            return StaticProperty.EMPTY;
        }
        int c1 = children[0].getCardinality();
        for (int i=1; i<children.length; i++) {
            c1 = Cardinality.add(c1, children[i].getCardinality());
            if (c1 == StaticProperty.ALLOWS_ZERO_OR_MORE) {
                break;
            }
        }
        return c1;
    }

    /**
     * Determine whether this instruction creates new nodes.
     * This implementation returns true if any child instruction
     * returns true.
     */

    public final boolean createsNewNodes() {
        if (children==null) {
            return false;
        };
        for (int i=0; i<children.length; i++) {
            int props = children[i].getSpecialProperties();
            if ((props & StaticProperty.NON_CREATIVE) == 0) {
                return true;
            }
        }
        return false;
    }

    /**
     * Simplify an expression. This performs any static optimization (by rewriting the expression
     * as a different expression). The default implementation does nothing.
     *
     * @exception XPathException if an error is discovered during expression
     *     rewriting
     * @return the simplified expression
     */

    public Expression simplify(StaticContext env) throws XPathException {
        boolean allAtomic = true;
        boolean nested = false;
        if (children != null) {
            for (int c=0; c<children.length; c++) {
                children[c] = children[c].simplify(env);
                if (!(children[c] instanceof Item)) {
                    allAtomic = false;
                }
                if (children[c] instanceof Block) {
                    nested = true;
                } else if (Literal.isEmptySequence(children[c])) {
                    nested = true;
                }
            }
            if (children.length == 1) {
                return getChildren()[0];
            }
            if (children.length == 0) {
                return Literal.makeLiteral(EmptySequence.getInstance());
            }
            if (nested) {
                List list = new ArrayList(children.length*2);
                flatten(list);
                children = new Expression[list.size()];
                for (int i=0; i<children.length; i++) {
                    children[i] = (Expression)list.get(i);
                    adoptChildExpression(children[i]);
                }
            }
            if (allAtomic) {
                Item[] values = new Item[children.length];
                for (int c=0; c<children.length; c++) {
                    values[c] = (Item)children[c];
                }
                return Literal.makeLiteral(new SequenceExtent(values));
            }
        } else {
            return Literal.makeLiteral(EmptySequence.getInstance());
        }

        return this;
    }

    private void flatten(List list) {
        for (int i=0; i<children.length; i++) {
            if (children[i] instanceof Block) {
                ((Block)children[i]).flatten(list);
            } else if (Literal.isEmptySequence(children[i])) {
                // no-op
            } else {
                list.add(children[i]);
            }
        }
    }

    public Expression typeCheck(StaticContext env, ItemType contextItemType) throws XPathException {
        boolean nested = false;
        if (children != null) {
            for (int c=0; c<children.length; c++) {
                children[c] = children[c].typeCheck(env, contextItemType);
                adoptChildExpression(children[c]);
                if (children[c] instanceof Block) {
                    nested = true;
                } else if (Literal.isEmptySequence(children[c])) {
                    nested = true;
                }
            }
        }
        if (nested) {
            List list = new ArrayList(children.length*2);
            flatten(list);
            children = new Expression[list.size()];
            for (int i=0; i<children.length; i++) {
                children[i] = (Expression)list.get(i);
                adoptChildExpression(children[i]);
            }
        }
        if (children.length == 0) {
            return Literal.makeLiteral(EmptySequence.getInstance());
        } else if (children.length == 1) {
            Expression.setParentExpression(children[0], getParentExpression());
            return children[0];
        } else {
            return this;
        }
    }

    public Expression optimize(Optimizer opt, StaticContext env, ItemType contextItemType) throws XPathException {
        boolean allAtomic = true;
        if (children != null) {
            for (int c=0; c<children.length; c++) {
                children[c] = children[c].optimize(opt, env, contextItemType);
                adoptChildExpression(children[c]);
                if (!Literal.isAtomic(children[c])) {
                    allAtomic = false;
                }
            }
        }
        if (allAtomic) {
            Item[] items = new Item[children.length];
            for (int c=0; c<children.length; c++) {
                items[c] = (AtomicValue)((Literal)children[c]).getValue();
            }
            return new Literal(new SequenceExtent(items));
        }
        return this;
    }



    /**
     * Handle promotion offers, that is, non-local tree rewrites.
     * @param offer The type of rewrite being offered
     * @throws XPathException
     */

    protected void promoteInst(PromotionOffer offer) throws XPathException {
        if (children != null) {
            for (int c=0; c<children.length; c++) {
                children[c] = doPromotion(children[c], offer);
                if (offer.accepted) {
                    break;  // can only handle one promotion at a time
                }
            }
        }
    }

    /**
     * Check that any elements and attributes constructed or returned by this expression are acceptable
     * in the content model of a given complex type. It's always OK to say yes, since the check will be
     * repeated at run-time. The process of checking element and attribute constructors against the content
     * model of a complex type also registers the type of content expected of those constructors, so the
     * static validation can continue recursively.
     */

    public void checkPermittedContents(SchemaType parentType, StaticContext env, boolean whole) throws XPathException {
        if (children != null) {
            for (int c=0; c<children.length; c++) {
                children[c].checkPermittedContents(parentType, env, false);
            }
        }
    }

    /**
     * Diagnostic print of expression structure. The expression is written to the System.err
     * output stream
     *
     * @param level indentation level for this expression
     * @param out
     * @param config
     */

    public void display(int level, PrintStream out, Configuration config) {
        if (children != null) {
            displayChildren(children, level+1, config, out);
        }
    }

    /**
     * Display the children of an instruction for diagostics
     */

    public static void displayChildren(Expression[] children, int level, Configuration config, PrintStream out) {
        if (children != null) {
            for (int c=0; c<children.length; c++) {
                children[c].display(level+1, out, config);
            }
        }
    }


    public TailCall processLeavingTail(XPathContext context) throws XPathException {

        if (children==null) {
            return null;
        }

        TailCall tc = null;
        for (int i=0; i<children.length; i++) {
            try {
                if (children[i] instanceof TailCallReturner) {
                    tc = ((TailCallReturner)children[i]).processLeavingTail(context);
                } else {
                    children[i].process(context);
                    tc = null;
                }
            } catch (DynamicError e) {
                if (e.getXPathContext() == null) {
                    e.setXPathContext(context);
                }
                if (e.getLocator()==null) {
                    e.setLocator(ExpressionTool.getLocator(children[i]));
                }
                throw e;
            }
        }
    	return tc;
    }

    /**
     * An implementation of Expression must provide at least one of the methods evaluateItem(), iterate(), or process().
     * This method indicates which of these methods is provided. This implementation provides both iterate() and
     * process() methods natively.
     */

    public int getImplementationMethod() {
        return ITERATE_METHOD | PROCESS_METHOD;
    }

    /**
     * Iterate over the results of all the child expressions
     */

    public SequenceIterator iterate(XPathContext context) throws XPathException {
        if (children==null || children.length == 0) {
            return EmptyIterator.getInstance();
        } else if (children.length == 1) {
            return children[0].iterate(context);
        } else {
            return new BlockIterator(children, context);
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
