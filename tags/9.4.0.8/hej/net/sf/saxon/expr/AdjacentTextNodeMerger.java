package net.sf.saxon.expr;

import net.sf.saxon.event.SequenceReceiver;
import net.sf.saxon.expr.instruct.Block;
import net.sf.saxon.expr.instruct.Choose;
import net.sf.saxon.expr.instruct.ValueOf;
import net.sf.saxon.expr.parser.ExpressionVisitor;
import net.sf.saxon.om.Item;
import net.sf.saxon.om.NodeInfo;
import net.sf.saxon.om.SequenceIterator;
import net.sf.saxon.pattern.NodeKindTest;
import net.sf.saxon.trans.XPathException;
import net.sf.saxon.tree.iter.AdjacentTextNodeMergingIterator;
import net.sf.saxon.tree.util.FastStringBuffer;
import net.sf.saxon.type.ItemType;
import net.sf.saxon.type.Type;
import net.sf.saxon.type.TypeHierarchy;
import net.sf.saxon.value.Cardinality;

/**
 * This class performs the first phase of processing in "constructing simple content":
 * it takes an input sequence, eliminates empty text nodes, and combines adjacent text nodes
 * into one.
 * @since 9.3
 */
public class AdjacentTextNodeMerger extends UnaryExpression {

    public AdjacentTextNodeMerger(Expression p0) {
        super(p0);
    }

    /*@NotNull*/
    @Override
    public Expression typeCheck(ExpressionVisitor visitor, ExpressionVisitor.ContextItemType contextItemType) throws XPathException {
        Expression e = super.typeCheck(visitor, contextItemType);
        if (e != this) {
            return e;
        }
        // This wrapper expression is unnecessary if the base expression cannot return text nodes,
        // or if it can return at most one item
        TypeHierarchy th = visitor.getConfiguration().getTypeHierarchy();
        if (th.relationship(getBaseExpression().getItemType(th), NodeKindTest.TEXT) == TypeHierarchy.DISJOINT) {
            return getBaseExpression();
        }
        if (!Cardinality.allowsMany(getBaseExpression().getCardinality())) {
            return getBaseExpression();
        }
        // In a choose expression, we can push the wrapper down to the action branches (whence it may disappear)
        if (getBaseExpression() instanceof Choose) {
            Choose choose = (Choose) getBaseExpression();
            Expression[] actions = choose.getActions();
            for (int i=0; i<actions.length; i++) {
                AdjacentTextNodeMerger atm2 = new AdjacentTextNodeMerger(actions[i]);
                actions[i] = atm2.typeCheck(visitor, contextItemType);
            }
            return choose;
        }
        // In a Block expression, check whether adjacent text nodes can occur (used in test strmode089)
        // Code deleted:
        if (getBaseExpression() instanceof Block) {
            Block block = (Block) getBaseExpression();
            Expression[] actions = block.getChildren();
            boolean prevtext = false;
            boolean needed = false;
            boolean maybeEmpty = false;
            for (int i=0; i<actions.length; i++) {
                boolean maybetext;
                if (actions[i] instanceof ValueOf) {
                    maybetext = true;
                    Expression content = ((ValueOf)actions[i]).getContentExpression();
                    if (content instanceof StringLiteral) {
                        // if it's empty, we could remove it now, but that's awkward and probably doesn't happen
                        maybeEmpty |= ((StringLiteral)content).getStringValue().length() == 0;
                    } else {
                        maybeEmpty = true;
                    }
                } else {
                    maybetext = th.relationship(actions[i].getItemType(th), NodeKindTest.TEXT) != TypeHierarchy.DISJOINT;
                    maybeEmpty |= maybetext;
                }
                if (prevtext && maybetext) {
                    needed = true;
                    break; // may contain adjacent text nodes
                }
                if (maybetext && Cardinality.allowsMany(actions[i].getCardinality())) {
                    needed = true;
                    break; // may contain adjacent text nodes
                }
                prevtext = maybetext;
            }
            if (!needed) {
                // We don't need to merge adjacent text nodes, we only need to remove empty ones.
                if (maybeEmpty) {
                    return new EmptyTextNodeRemover(block);
                } else {
                    return block;
                }
            }
        }
        return this;
    }

    /**
     * Determine the data type of the expression, if possible. The default
     * implementation for unary expressions returns the item type of the operand
     * @param th the type hierarchy cache
     * @return the item type of the items in the result sequence, insofar as this
     *         is known statically.
     */

    /*@NotNull*/
    @Override
    public ItemType getItemType(TypeHierarchy th) {
        return getBaseExpression().getItemType(th);
    }

    @Override
    public int computeCardinality() {
        return getBaseExpression().getCardinality() | StaticProperty.ALLOWS_ZERO;
    }

    /**
     * Copy an expression. This makes a deep copy.
     * @return the copy of the original expression
     */

    /*@NotNull*/
    public Expression copy() {
        return new AdjacentTextNodeMerger(getBaseExpression().copy());
    }

    /**
     * An implementation of Expression must provide at least one of the methods evaluateItem(), iterate(), or process().
     * This method indicates which of these methods is prefered.
     */

    public int getImplementationMethod() {
        return Expression.PROCESS_METHOD | Expression.ITERATE_METHOD | ITEM_FEED_METHOD | WATCH_METHOD ;
    }    

    /**
     * Return an Iterator to iterate over the values of a sequence. The value of every
     * expression can be regarded as a sequence, so this method is supported for all
     * expressions. This default implementation handles iteration for expressions that
     * return singleton values: for non-singleton expressions, the subclass must
     * provide its own implementation.
     * @param context supplies the context for evaluation
     * @return a SequenceIterator that can be used to iterate over the result
     *         of the expression
     * @throws net.sf.saxon.trans.XPathException
     *          if any dynamic error occurs evaluating the
     *          expression
     */

    /*@NotNull*/
    @Override
    public SequenceIterator iterate(XPathContext context) throws XPathException {
        return new AdjacentTextNodeMergingIterator(getBaseExpression().iterate(context));
    }

    /**
     * Process the instruction, without returning any tail calls
     * @param context The dynamic context, giving access to the current node,
     *                the current variables, etc.
     */

    public void process(XPathContext context, int locationId, int options) throws XPathException {
        SequenceReceiver out = context.getReceiver();
        FastStringBuffer fsb = new FastStringBuffer(FastStringBuffer.MEDIUM);
        SequenceIterator iter = getBaseExpression().iterate(context);
        boolean prevText = false;
        while (true) {
            Item item = iter.next();
            if (item == null) {
                break;
            }
            if (isTextNode(item)) {
                CharSequence s = item.getStringValueCS();
                if (s.length() > 0) {
                    fsb.append(s);
                    prevText = true;
                }

            } else {
                if (prevText) {
                    out.characters(fsb, locationId, options);
                }
                prevText = false;
                fsb.setLength(0);
                out.append(item, locationId, options);
            }
        }
        if (prevText) {
            out.characters(fsb, locationId, options);
        }
    }


    @Override
    public String getExpressionName() {
        return "mergeAdjacentText";
    }

    /**
     * Ask whether an item is a text node
     * @param item the item in question
     * @return true if the item is a node of kind text
     */

    public static boolean isTextNode(Item item) {
        return item instanceof NodeInfo && ((NodeInfo)item).getNodeKind() == Type.TEXT;
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
