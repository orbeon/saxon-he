package net.sf.saxon.functions;

import net.sf.saxon.expr.CallableExpression;
import net.sf.saxon.expr.Expression;
import net.sf.saxon.expr.StaticProperty;
import net.sf.saxon.expr.XPathContext;
import net.sf.saxon.expr.parser.ExpressionVisitor;
import net.sf.saxon.om.Item;
import net.sf.saxon.om.SequenceIterator;
import net.sf.saxon.om.StandardNames;
import net.sf.saxon.trans.XPathException;
import net.sf.saxon.tree.iter.SingletonIterator;
import net.sf.saxon.type.Converter;
import net.sf.saxon.type.TypeHierarchy;
import net.sf.saxon.value.DateTimeValue;

/**
* This class implements the XPath 2.0 functions
 * current-date(), current-time(), and current-dateTime(), as
 * well as the function implicit-timezone(). The value that is required
 * is inferred from the type of result required.
*/


public class CurrentDateTime extends SystemFunction implements CallableExpression{

    /**
     * preEvaluate: this method suppresses compile-time evaluation by doing nothing
     * (because the value of the expression depends on the runtime context)
     * @param visitor an expression visitor
     */

    public Expression preEvaluate(ExpressionVisitor visitor) {
        return this;
    }

    /**
    * Determine the dependencies
    */

    public int getIntrinsicDependencies() {
        // current date/time is part of the context, but it is fixed for a transformation, so
        // we don't need to manage it as a dependency: expressions using it can be freely
        // rearranged
       return StaticProperty.DEPENDS_ON_RUNTIME_ENVIRONMENT;
    }

    /**
    * Evaluate in a general context
    */

    public Item evaluateItem(XPathContext context) throws XPathException {
        final DateTimeValue dt = DateTimeValue.getCurrentDateTime(context);
        final TypeHierarchy th = context.getConfiguration().getTypeHierarchy();
        final int targetType = getItemType(th).getPrimitiveType();
        switch (targetType) {
            case StandardNames.XS_DATE_TIME:
                return dt;
            case StandardNames.XS_DATE:
                return Converter.DATE_TIME_TO_DATE.convert(dt).asAtomic();
            case StandardNames.XS_TIME:
                return Converter.DATE_TIME_TO_TIME.convert(dt).asAtomic();
            case StandardNames.XS_DAY_TIME_DURATION:
            case StandardNames.XS_DURATION:
                return dt.getComponent(Component.TIMEZONE);
            default:
                throw new IllegalArgumentException("Wrong target type for current date/time");
        }
    }

	public SequenceIterator call(SequenceIterator[] arguments,
			/*@NotNull*/ XPathContext context) throws XPathException {
		return SingletonIterator.makeIterator(evaluateItem(context));
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