package net.sf.saxon.functions;
import net.sf.saxon.expr.ReversibleIterator;
import net.sf.saxon.expr.StaticProperty;
import net.sf.saxon.expr.XPathContext;
import net.sf.saxon.om.SequenceIterator;
import net.sf.saxon.trans.XPathException;
import net.sf.saxon.type.AnyItemType;
import net.sf.saxon.type.ItemType;
import net.sf.saxon.type.TypeHierarchy;
import net.sf.saxon.value.SequenceExtent;

/**
* Implement XPath function fn:reverse()
*/

public class Reverse extends SystemFunction {

//    /**
//     * Simplify and validate.
//     * @param visitor an expression visitor
//     */

//     public Expression simplify(ExpressionVisitor visitor) throws XPathException {
//        Reverser a = new Reverser(argument[0]);
//        ExpressionTool.copyLocationInfo(this, a);
//        return visitor.simplify(a);
//    }


    /**
     * Determine the item type of the value returned by the function
     *
     * @param th the type hierarchy cache
     */

    public ItemType getItemType(TypeHierarchy th) {
        return argument[0].getItemType(th);    //AUTO
    }

    public int computeSpecialProperties() {
        int baseProps = argument[0].getSpecialProperties();
        if ((baseProps & StaticProperty.REVERSE_DOCUMENT_ORDER) != 0) {
            return (baseProps &
                (~StaticProperty.REVERSE_DOCUMENT_ORDER)) |
                StaticProperty.ORDERED_NODESET;
        } else if ((baseProps & StaticProperty.ORDERED_NODESET) != 0) {
            return (baseProps &
                (~StaticProperty.ORDERED_NODESET)) |
                StaticProperty.REVERSE_DOCUMENT_ORDER;
        } else {
            return baseProps;
        }
    }

    public SequenceIterator iterate(XPathContext context) throws XPathException {
        SequenceIterator forwards = argument[0].iterate(context);
        if (forwards instanceof ReversibleIterator) {
            return ((ReversibleIterator)forwards).getReverseIterator();
        } else {
            SequenceExtent extent = new SequenceExtent(forwards);
            return extent.reverseIterate();
        }
    }

    public boolean effectiveBooleanValue(XPathContext context) throws XPathException {
        // EBV is independent of sequence order unless the sequence mixes atomic values and nodes
        ItemType type = argument[0].getItemType(context.getConfiguration().getTypeHierarchy());
        if (type == AnyItemType.getInstance()) {
            return super.effectiveBooleanValue(context);
        } else {
            return argument[0].effectiveBooleanValue(context);
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
