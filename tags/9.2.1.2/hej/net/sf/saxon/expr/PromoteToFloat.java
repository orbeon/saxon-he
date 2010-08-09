package net.sf.saxon.expr;

import net.sf.saxon.type.BuiltInAtomicType;
import net.sf.saxon.type.TypeHierarchy;
import net.sf.saxon.type.ItemType;
import net.sf.saxon.value.AtomicValue;
import net.sf.saxon.value.NumericValue;
import net.sf.saxon.value.UntypedAtomicValue;
import net.sf.saxon.value.DoubleValue;
import net.sf.saxon.trans.XPathException;


/**
 * Expression that performs numeric promotion to xs:double
 */
public class PromoteToFloat extends NumericPromoter {

    public PromoteToFloat(Expression exp) {
        super(exp);
    }

    /**
    * Determine the data type of the items returned by the expression, if possible
    * @return a value such as Type.STRING, Type.BOOLEAN, Type.NUMBER, Type.NODE,
    * or Type.ITEM (meaning not known in advance)
     * @param th the type hierarchy cache
     */

	public ItemType getItemType(TypeHierarchy th) {
        return BuiltInAtomicType.FLOAT;
	}


    /**
     * Copy an expression. This makes a deep copy.
     *
     * @return the copy of the original expression
     */

    public Expression copy() {
        return new PromoteToFloat(getBaseExpression().copy());
    }

    /**
     * Perform the promotion
     * @param value the numeric or untyped atomic value to be promoted
     * @param context the XPath dynamic evaluation context
     * @return the value that results from the promotion
     */

    protected AtomicValue promote(AtomicValue value, XPathContext context) throws XPathException {
        if (!(value instanceof NumericValue || value instanceof UntypedAtomicValue)) {
            final TypeHierarchy th = context.getConfiguration().getTypeHierarchy();
            XPathException err = new XPathException(
                    "Cannot promote non-numeric value to " + getItemType(th).toString(), "XPTY0004", context);
            err.setLocator(this);
            throw err;
        }
        if (value instanceof DoubleValue) {
            XPathException err = new XPathException(
                    "Cannot promote from xs:double to xs:float", "XPTY0004", context);
            err.setLocator(this);
            throw err;
        }
        return value.convert(BuiltInAtomicType.FLOAT, true, context).asAtomic();
    }
}

//
// The contents of this file are subject to the Mozilla Public License Version
// 1.0 (the "License");
// you may not use this file except in compliance with the License. You may
// obtain a copy of the
// License at http://www.mozilla.org/MPL/
//
// Software distributed under the License is distributed on an "AS IS" basis,
// WITHOUT WARRANTY OF ANY KIND, either express or implied.
// See the License for the specific language governing rights and limitations
// under the License.
//
// The Original Code is: all this file.
//
// The Initial Developer of the Original Code is Michael Kay,
//
// Portions created by (your name) are Copyright (C) (your legal entity). All
// Rights Reserved.
//
// Contributor(s): none.
//
