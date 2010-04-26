package net.sf.saxon.functions;

import net.sf.saxon.expr.*;
import net.sf.saxon.om.Item;
import net.sf.saxon.trans.XPathException;
import net.sf.saxon.type.BuiltInAtomicType;
import net.sf.saxon.type.ItemType;
import net.sf.saxon.type.TypeHierarchy;
import net.sf.saxon.value.AtomicValue;
import net.sf.saxon.value.Int64Value;
import net.sf.saxon.value.NumericValue;
import net.sf.saxon.value.StringValue;

/**
 * This class implements the XPath substring() function
 */

public class Substring extends SystemFunction {

    /**
     * Type-check the expression. This also calls preEvaluate() to evaluate the function
     * if all the arguments are constant; functions that do not require this behavior
     * can override the preEvaluate method.
     */

    public Expression typeCheck(ExpressionVisitor visitor, ItemType contextItemType) throws XPathException {
        Expression e2 = super.typeCheck(visitor, contextItemType);
        if (e2 != this) {
            return e2;
        }
        TypeHierarchy th = visitor.getConfiguration().getTypeHierarchy();
        if (argument[1] instanceof NumberFn) {
            Expression a1 = ((NumberFn)argument[1]).getArguments()[0];
            if (th.isSubType(a1.getItemType(th), BuiltInAtomicType.INTEGER)) {
                argument[1] = a1;
            }
        }
        if (argument.length > 2 && argument[2] instanceof NumberFn) {
            Expression a2 = ((NumberFn)argument[2]).getArguments()[0];
            if (th.isSubType(a2.getItemType(th), BuiltInAtomicType.INTEGER)) {
                argument[2] = a2;
            }
        }
        return this;
    }

    /**
    * Evaluate the function
    */

    public Item evaluateItem(XPathContext context) throws XPathException {

        AtomicValue av = (AtomicValue)argument[0].evaluateItem(context);
        if (av==null) {
            return StringValue.EMPTY_STRING;
        }
        StringValue sv = (StringValue)av;
        if (sv.isZeroLength()) {
            return StringValue.EMPTY_STRING;
        }

        AtomicValue a1 = (AtomicValue)argument[1].evaluateItem(context);
        NumericValue a = (NumericValue)a1;

        if (argument.length==2) {
            StringValue result = StringValue.makeStringValue(substring(sv, a));
            if (sv.isKnownToContainNoSurrogates()) {
                result.setContainsNoSurrogates();
            }
            return result;
        } else {
            AtomicValue b2 = (AtomicValue)argument[2].evaluateItem(context);
            NumericValue b = (NumericValue)b2;
            StringValue result = StringValue.makeStringValue(substring(sv, a, b, context));
            if (sv.isKnownToContainNoSurrogates()) {
                result.setContainsNoSurrogates();
            }
            return result;
        }
    }

    /**
     * Implement the substring function with two arguments.
     * @param sv the string value
     * @param start the numeric offset (1-based) of the first character to be included in the result
     * (if not an integer, the XPath rules apply)
     * @return the substring starting at this position.
    */

    public static CharSequence substring(StringValue sv, NumericValue start) {
        CharSequence s = sv.getStringValueCS();
        int slength = s.length();

        long lstart;
        if (start instanceof Int64Value) {
            //noinspection RedundantCast
            lstart = ((Int64Value)start).longValue();
            if (lstart > slength) {
                return "";
            } else if (lstart <= 0) {
                lstart = 1;
            }
        } else {
            //NumericValue rstart = start.round();
            // We need to be careful to handle cases such as plus/minus infinity
            if (start.isNaN()) {
                return "";
            } else if (start.signum() <= 0) {
                return s;
            } else if (start.compareTo(slength) > 0) {
                // this works even where the string contains surrogate pairs,
                // because the Java length is always >= the XPath length
                return "";
            } else {
                lstart = Math.round(start.getDoubleValue());
            }
        }

        if (!sv.containsSurrogatePairs()) {
            return s.subSequence((int)lstart-1, s.length());
        }

        int pos=1;
        int cpos=0;
        while (cpos<slength) {
            if (pos >= lstart) {
                return s.subSequence(cpos, s.length());
            }

            int ch = (int)s.charAt(cpos++);
            if (ch<55296 || ch>56319) {
                pos++;    // don't count high surrogates, i.e. D800 to DBFF
            }
        }
        return "";
    }

    /**
     * Implement the substring function with three arguments.
     * @param sv the string value
     * @param start the numeric offset (1-based) of the first character to be included in the result
     * (if not an integer, the XPath rules apply)
     * @param len the length of the required substring (again, XPath rules apply)
     * @param context the XPath dynamic context. Provided because some arithmetic computations require it
     * @return the substring starting at this position.
    */

    public static CharSequence substring(StringValue sv, NumericValue start, NumericValue len, XPathContext context) {

        CharSequence s = sv.getStringValueCS();
        int slength = s.length();

        long lstart;
        if (start instanceof Int64Value) {
            //noinspection RedundantCast
            lstart = ((Int64Value)start).longValue();
            if (lstart > slength) {
                return "";
            }
        } else {
            start = start.round();
            // TODO: convert directly to a long using Math.round, as in the 2-argument case
            // We need to be careful to handle cases such as plus/minus infinity and NaN
            if (start.isNaN()) {
                return "";
            } else if (start.signum() <= 0) {
                lstart = 0;
            } else if (start.compareTo(slength) > 0) {
                // this works even where the string contains surrogate pairs,
                // because the Java length is always >= the XPath length
                return "";
            } else {
                try {
                    lstart = start.longValue();
                } catch (XPathException err) {
                    // this shouldn't happen unless the string length exceeds the bounds
                    // of a long
                    throw new AssertionError("string length out of permissible range");
                }
            }
        }

        NumericValue end;
        try {
            //end = start.arithmetic(Token.PLUS, len.round(), context);
            end = (NumericValue)ArithmeticExpression.compute(start, Calculator.PLUS, len.round(), context);
        } catch (XPathException e) {
            throw new AssertionError("Unexpected arithmetic failure in substring");
        }
        long lend;
        if (end instanceof Int64Value) {
            //noinspection RedundantCast
            lend = ((Int64Value)end).longValue();
        } else {
            // We need to be careful to handle cases such as plus/minus infinity and NaN
            if (end.isNaN()) {
                return "";
            } else if (end.signum() <= 0) {
                return "";
            } else if (end.compareTo(slength) > 0) {
                // this works even where the string contains surrogate pairs,
                // because the Java length is always >= the XPath length
                lend = slength+1;
            } else {
                try {
                    lend = end.ceiling().longValue();
                } catch (XPathException err) {
                    // this shouldn't happen unless the string length exceeds the bounds
                    // of a long
                    throw new AssertionError("string length out of permissible range");
                }
            }
        }

        if (lend < lstart) {
            return "";
        }

        if (!sv.containsSurrogatePairs()) {
            int a1 = (int)lstart - 1;
            int a2 = Math.min(slength, (int)lend - 1);
            if (a1 < 0) {
                if (a2 < 0) {
                    return "";
                } else {
                    a1 = 0;
                }
            }
            return s.subSequence(a1, a2);
        }

        int jstart=-1;
        int jend=-1;
        int pos=1;
        int cpos=0;
        while (cpos<slength) {
            if (pos >= lstart) {
                if (pos < lend) {
                    if (jstart<0) {
                        jstart = cpos;
                    }
                } else {
                    jend = cpos;
                    break;
                }
            }

            int ch = (int)s.charAt(cpos++);
            if (ch<55296 || ch>56319) pos++;    // don't count high surrogates, i.e. D800 to DBFF
        }
        if (jstart<0 || jstart==jend) {
            return "";
        } else if (jend<0) {
            return s.subSequence(jstart, s.length());
        } else {
            return s.subSequence(jstart, jend);
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
