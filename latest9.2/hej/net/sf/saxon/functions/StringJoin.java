package net.sf.saxon.functions;

import net.sf.saxon.event.ComplexContentOutputter;
import net.sf.saxon.event.SequenceReceiver;
import net.sf.saxon.expr.Expression;
import net.sf.saxon.expr.ExpressionVisitor;
import net.sf.saxon.expr.XPathContext;
import net.sf.saxon.om.FastStringBuffer;
import net.sf.saxon.om.Item;
import net.sf.saxon.om.SequenceIterator;
import net.sf.saxon.trans.XPathException;
import net.sf.saxon.type.ItemType;
import net.sf.saxon.value.Cardinality;
import net.sf.saxon.value.StringValue;

/**
* xf:string-join(string* $sequence, string $separator)
*/

public class StringJoin extends SystemFunction {

    public Expression optimize(ExpressionVisitor visitor, ItemType contextItemType) throws XPathException {
        Expression exp = super.optimize(visitor, contextItemType);
        if (exp instanceof StringJoin) {
            return ((StringJoin)exp).simplifySingleton();
        } else {
            return exp;
        }
    }

    private Expression simplifySingleton() {
        int card = argument[0].getCardinality();
        if (!Cardinality.allowsMany(card)) {
            if (Cardinality.allowsZero(card)) {
                return SystemFunction.makeSystemFunction("string", new Expression[]{argument[0]});
            } else {
                return argument[0];
            }
        }
        return this;
    }

    public Item evaluateItem(XPathContext c) throws XPathException {

        // This rather tortuous code is designed to ensure that we don't evaluate the
        // separator argument unless there are at least two items in the sequence.

        SequenceIterator iter = argument[0].iterate(c);
        Item it = iter.next();
        if (it==null) {
            return StringValue.EMPTY_STRING;
        }

        CharSequence first = it.getStringValueCS();

        it = iter.next();
        if (it==null) {
            return StringValue.makeStringValue(first);
        }

        FastStringBuffer sb = new FastStringBuffer(FastStringBuffer.SMALL);
        sb.append(first);

        // Type checking ensures that the separator is not an empty sequence
        CharSequence sep = argument[1].evaluateItem(c).getStringValueCS();
        sb.append(sep);
        sb.append(it.getStringValueCS());

        while (true) {
            it = iter.next();
            if (it == null) {
                return StringValue.makeStringValue(sb.condense());
            }
            sb.append(sep);
            sb.append(it.getStringValueCS());
        }
    }

    /**
     * Process the instruction in push mode. This avoids constructing the concatenated string
     * in memory, instead its parts can be sent straight to the serializer.
     * @param context The dynamic context, giving access to the current node,
     *                the current variables, etc.
     */

    public void process(XPathContext context) throws XPathException {
        // This rather tortuous code is designed to ensure that we don't evaluate the
        // separator argument unless there are at least two items in the sequence.

        SequenceReceiver out = context.getReceiver();
        if (out instanceof ComplexContentOutputter) {
            // Optimization is only safe if evaluated as part of a complex content constructor
            // Start and end with an empty string to force space separation from any previous or following outputs
            out.append(StringValue.EMPTY_STRING, 0, 0);

            SequenceIterator iter = argument[0].iterate(context);
            Item it = iter.next();
            if (it==null) {
                return;
            }

            CharSequence first = it.getStringValueCS();
            out.characters(first, 0, 0);

            it = iter.next();
            if (it==null) {
                out.append(StringValue.EMPTY_STRING, 0, 0);
                return;
            }

            // Type checking ensures that the separator is not an empty sequence
            CharSequence sep = argument[1].evaluateItem(context).getStringValueCS();
            out.characters(sep, 0, 0);
            out.characters(it.getStringValueCS(), 0, 0);

            while (true) {
                it = iter.next();
                if (it == null) {
                    break;
                }
                out.characters(sep, 0, 0);
                out.characters(it.getStringValueCS(), 0, 0);
            }

            out.append(StringValue.EMPTY_STRING, 0, 0);
        } else {
            out.append(evaluateItem(context), 0, 0);
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
// The Initial Developer of the Original Code is Michael H. Kay
//
// Portions created by (your name) are Copyright (C) (your legal entity). All Rights Reserved.
//
// Contributor(s): none.
//
