package net.sf.saxon.expr;

import net.sf.saxon.om.Item;
import net.sf.saxon.om.NodeInfo;
import net.sf.saxon.om.SequenceIterator;
import net.sf.saxon.trans.XPathException;
import net.sf.saxon.type.ItemType;
import net.sf.saxon.type.Type;
import net.sf.saxon.type.TypeHierarchy;

/**
 * This class performs the first phase of processing in "constructing simple content":
 * it takes an input sequence, eliminates empty text nodes, and combines adjacent text nodes
 * into one.
 * @since 9.3
 */
public class EmptyTextNodeRemover extends UnaryExpression
        implements ItemMappingFunction {

    public EmptyTextNodeRemover(Expression p0) {
        super(p0);
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
        return new EmptyTextNodeRemover(getBaseExpression().copy());
    }

    /**
     * An implementation of Expression must provide at least one of the methods evaluateItem(), iterate(), or process().
     * This method indicates which of these methods is prefered.
     */

    public int getImplementationMethod() {
        return Expression.ITERATE_METHOD | ITEM_FEED_METHOD | WATCH_METHOD ;
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
        return new ItemMappingIterator(getBaseExpression().iterate(context), this);
    }

    /**
     * Map an item to another item
     * @param item The input item to be mapped.
     * @return the result of the mapping: maybe null
     * @throws XPathException
     */

    /*@Nullable*/ public Item mapItem(Item item) throws XPathException {
        if (item instanceof NodeInfo &&
                ((NodeInfo)item).getNodeKind() == Type.TEXT &&
                item.getStringValueCS().length() == 0) {
             return null;
        } else {
            return item;
        }
     }

    @Override
    public String getExpressionName() {
        return "emptyTextNodeRemover";
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
