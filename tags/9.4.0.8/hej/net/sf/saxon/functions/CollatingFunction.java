package net.sf.saxon.functions;

import net.sf.saxon.expr.Expression;
import net.sf.saxon.expr.Literal;
import net.sf.saxon.expr.StaticContext;
import net.sf.saxon.expr.XPathContext;
import net.sf.saxon.expr.parser.ExpressionTool;
import net.sf.saxon.expr.parser.ExpressionVisitor;
import net.sf.saxon.expr.sort.CodepointCollator;
import net.sf.saxon.expr.sort.GenericAtomicComparer;
import net.sf.saxon.lib.StringCollator;
import net.sf.saxon.trans.Err;
import net.sf.saxon.trans.XPathException;
import net.sf.saxon.value.AtomicValue;
import net.sf.saxon.value.StringValue;
import net.sf.saxon.value.Value;

import java.net.URI;
import java.net.URISyntaxException;

/**
 * Abstract superclass for all functions that take an optional collation argument
 */

// Supports string comparison using a collation

public abstract class CollatingFunction extends SystemFunction {

    // The collation, if known statically
    /*@Nullable*/ protected StringCollator stringCollator = null;
    private String absoluteCollationURI = null;
    private URI expressionBaseURI = null;

    public void checkArguments(/*@NotNull*/ ExpressionVisitor visitor) throws XPathException {
        if (stringCollator == null) {
            StaticContext env = visitor.getStaticContext();
            if (expressionBaseURI == null) {
                expressionBaseURI = ExpressionTool.getBaseURI(env, this, false);
            }
            preEvaluateCollation(env);
        }
        super.checkArguments(visitor);
    }

    /**
     * Get the saved static base URI
     * @return the static base URI
     */

    public URI getExpressionBaseURI() {
        return expressionBaseURI;
    }

    /**
     * Get the collation if known statically, as a StringCollator object
     * @return a StringCollator. Return null if the collation is not known statically.
     */

    public StringCollator getStringCollator() {
        return stringCollator;
    }

    /**
     * Get the absolute collation URI if known statically, as a string
     * @return the absolute collation URI, as a string, or null if it is not known statically
     */

    public String getAbsoluteCollationURI() {
        return absoluteCollationURI;
    }

    /**
     * Pre-evaluate the collation argument if its value is known statically
     * @param env the static XPath evaluation context
     */

    private void preEvaluateCollation(StaticContext env) throws XPathException {
        if (getNumberOfArguments() == getDetails().maxArguments) {
            final Expression collationExp = argument[getNumberOfArguments() - 1];
            final Value collationVal = (collationExp instanceof Literal ? ((Literal)collationExp).getValue() : null);
            if (collationVal instanceof AtomicValue) {
                // Collation is supplied as a constant
                String collationName = collationVal.getStringValue();
                URI collationURI;
                try {
                    collationURI = new URI(collationName);
                    if (!collationURI.isAbsolute()) {
                        if (expressionBaseURI == null) {
                            expressionBaseURI = ExpressionTool.getBaseURI(env, this, true);
                        }
                        if (expressionBaseURI == null) {
                            XPathException err = new XPathException("The collation name is a relative URI, but the base URI is unknown");
                            err.setErrorCode("XPST0001");
                            err.setIsStaticError(true);
                            err.setLocator(this);
                            throw err;
                        }
                        URI base = expressionBaseURI;
                        collationURI = base.resolve(collationURI);
                        collationName = collationURI.toString();
                    }
                } catch (URISyntaxException e) {
                    XPathException err = new XPathException("Collation name '" + collationName + "' is not a valid URI");
                    err.setErrorCode("FOCH0002");
                    err.setIsStaticError(true);
                    err.setLocator(this);
                    throw err;
                }
                StringCollator comp = env.getCollation(collationName);
                if (comp == null) {
                    XPathException err = new XPathException("Unknown collation " + Err.wrap(collationName, Err.URI));
                    err.setErrorCode("FOCH0002");
                    err.setIsStaticError(true);
                    err.setLocator(this);
                    throw err;
                } else {
                    stringCollator = comp;
                }
            } else {
                // collation isn't known until run-time
            }
        } else {
            // Use the default collation
            String uri = env.getDefaultCollationName();
            stringCollator = env.getCollation(uri);
        }
    }


    /**
     * Copy an expression. This makes a deep copy.
     * @return the copy of the original expression
     */

    /*@NotNull*/
    public Expression copy() {
        CollatingFunction d = (CollatingFunction)super.copy();
        d.expressionBaseURI = expressionBaseURI;
        d.absoluteCollationURI = absoluteCollationURI;
        d.stringCollator = stringCollator;
        return d;
    }

    /**
     * Determine whether two expressions are equivalent
     */
    @Override
    public boolean equals(Object o) {
        return o instanceof CollatingFunction &&
                super.equals(o) &&
                equalOrNull(expressionBaseURI, ((CollatingFunction)o).expressionBaseURI) &&
                equalOrNull(absoluteCollationURI, ((CollatingFunction)o).absoluteCollationURI) &&
                equalOrNull(stringCollator, ((CollatingFunction)o).stringCollator);
    }

    private static boolean equalOrNull(Object x, Object y) {
        if (x == null) {
            return (y == null);
        } else {
            return (y != null) && x.equals(y);
        }
    }

    /**
     * Get a GenericAtomicComparer that can be used to compare values. This method is called
     * at run time by subclasses to evaluate the parameter containing the collation name.
     * <p/>
     * <p>The difference between this method and {@link #getCollator} is that a
     * GenericAtomicComparer is capable of comparing values of any atomic type, not only
     * strings. It is therefore called by functions such as compare, deep-equal, index-of, and
     * min() and max() where the operands may include a mixture of strings and other types.</p>
     *
     * @param arg     the position of the argument (starting at 0) containing the collation name.
     *                If this argument was not supplied, the default collation is used
     * @param context The dynamic evaluation context.
     * @return a GenericAtomicComparer that can be used to compare atomic values.
     */

    protected GenericAtomicComparer getAtomicComparer(int arg, XPathContext context) throws XPathException {
        // TODO:PERF avoid creating a new object on each call when the collation is specified dynamically
        return new GenericAtomicComparer(getCollator(arg, context), context);
    }

    /**
     * Get a collator suitable for comparing strings. Returns the collator specified in the
     * given function argument if present, otherwise returns the default collator. This method is
     * called by subclasses at run time. It is used (in contrast to {@link #getAtomicComparer})
     * when it is known that the values to be compared are always strings.
     *
     * @param arg     The argument position (counting from zero) that holds the collation
     *                URI if present
     * @param context The dynamic context
     * @return a StringCollator
     */

    protected StringCollator getCollator(int arg, XPathContext context) throws XPathException {

        if (stringCollator != null) {
            // the collation was determined statically
            return stringCollator;
        } else {
            int numargs = argument.length;
            if (numargs > arg) {
                AtomicValue av = (AtomicValue) argument[arg].evaluateItem(context);
                StringValue collationValue = (StringValue) av;
                String collationName = collationValue.getStringValue();
                try {
                    collationName = expandCollationURI(collationName, expressionBaseURI, context);
                } catch (XPathException err) {
                    err.setLocator(this);
                }
                return context.getCollation(collationName);
            } else {
                // Fallback - this shouldn't happen
                return CodepointCollator.getInstance();
            }
        }
    }

    public static String expandCollationURI(String collationName, URI expressionBaseURI, XPathContext context) throws XPathException {
        try {
            URI collationURI = new URI(collationName);
            if (!collationURI.isAbsolute()) {
                if (expressionBaseURI == null) {
                    XPathException err = new XPathException("Cannot resolve relative collation URI '" + collationName +
                            "': unknown or invalid base URI");
                    err.setErrorCode("FOCH0002");
                    err.setXPathContext(context);
                    throw err;
                }
                collationURI = expressionBaseURI.resolve(collationURI);
                collationName = collationURI.toString();
            }
        } catch (URISyntaxException e) {
            XPathException err = new XPathException("Collation name '" + collationName + "' is not a valid URI");
            err.setErrorCode("FOCH0002");
            err.setXPathContext(context);
            throw err;
        }
        return collationName;
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