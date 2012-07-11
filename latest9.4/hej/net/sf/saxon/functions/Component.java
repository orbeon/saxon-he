package net.sf.saxon.functions;

import net.sf.saxon.expr.Expression;
import net.sf.saxon.expr.XPathContext;
import net.sf.saxon.expr.parser.ExpressionVisitor;
import net.sf.saxon.om.Item;
import net.sf.saxon.trans.XPathException;
import net.sf.saxon.value.AtomicValue;
import net.sf.saxon.value.Int64Value;
import net.sf.saxon.value.IntegerValue;

/**
 * This class supports the get_X_from_Y functions defined in XPath 2.0
 */

public class Component extends SystemFunction {

    public static final int YEAR = 1;
    public static final int MONTH = 2;
    public static final int DAY = 3;
    public static final int HOURS = 4;
    public static final int MINUTES = 5;
    public static final int SECONDS = 6;
    public static final int TIMEZONE = 7;
    public static final int LOCALNAME = 8;
    public static final int NAMESPACE = 9;
    public static final int PREFIX = 10;
    public static final int MICROSECONDS = 11;   // internal use only
    public static final int WHOLE_SECONDS = 12;  // internal use only
    public static final int YEAR_ALLOWING_ZERO = 13;  // internal use only

    int component;

    /*@NotNull*/
    public Expression simplify(ExpressionVisitor visitor) throws XPathException {
        component = (operation >> 16) & 0xffff;
        return super.simplify(visitor);
    }

    /**
     * Set the details of this type of function
     *
     * @param entry information giving details of the function signature
     */
    @Override
    public void setDetails(StandardFunction.Entry entry) {
        super.setDetails(entry);
        component = (operation >> 16) & 0xffff;
    }

    /**
     * For an expression that returns an integer or a sequence of integers, get
     * a lower and upper bound on the values of the integers that may be returned, from
     * static analysis. The default implementation returns null, meaning "unknown" or
     * "not applicable". Other implementations return an array of two IntegerValue objects,
     * representing the lower and upper bounds respectively. The values
     * UNBOUNDED_LOWER and UNBOUNDED_UPPER are used by convention to indicate that
     * the value may be arbitrarily large. The values MAX_STRING_LENGTH and MAX_SEQUENCE_LENGTH
     * are used to indicate values limited by the size of a string or the size of a sequence.
     *
     * @return the lower and upper bounds of integer values in the result, or null to indicate
     *         unknown or not applicable.
     */
    @Override
    public IntegerValue[] getIntegerBounds() {
        switch (component) {
            case YEAR:
                return new IntegerValue[]{Int64Value.makeIntegerValue(-100000), Int64Value.makeIntegerValue(+100000)};
            case MONTH:
                return new IntegerValue[]{Int64Value.makeIntegerValue(-11), Int64Value.makeIntegerValue(+11)};
            case DAY:
                return new IntegerValue[]{Int64Value.makeIntegerValue(-31), Int64Value.makeIntegerValue(+31)};
            case HOURS:
                return new IntegerValue[]{Int64Value.makeIntegerValue(-24), Int64Value.makeIntegerValue(+24)};
            case MINUTES:
                return new IntegerValue[]{Int64Value.makeIntegerValue(-59), Int64Value.makeIntegerValue(+59)};
            case SECONDS:
                return new IntegerValue[]{Int64Value.makeIntegerValue(-59), Int64Value.makeIntegerValue(+59)};
            default:
                return null;
        }
    }

    /**
     * Get the required component
     */

    public int getRequiredComponent() {
        return component;
    }

    /**
     * Get the required component name as a string
     */

    public String getRequiredComponentAsString() {
        String[] components = {"", "YEAR", "MONTH", "DAY", "HOURS", "MINUTES", "SECONDS",
                               "TIMEZONE", "LOCALNAME", "NAMESPACE", "PREFIX", "MICROSECONDS",
                               "WHOLE_SECONDS", "YEAR_ALLOWING_ZERO"};
        return components[component];
    }

    /**
     * Evaluate the expression
     */

    public Item evaluateItem(XPathContext context) throws XPathException {
        AtomicValue arg = (AtomicValue)argument[0].evaluateItem(context);

        if (arg == null) {
            return null;
        }
        return arg.getComponent(component);

    }


    /**
     * Copy an expression. This makes a deep copy.
     *
     * @return the copy of the original expression
     */

    /*@NotNull*/
    public Expression copy() {
        Component c = (Component)super.copy();
        c.component = (c.operation >> 16) & 0xffff;
        return c;
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