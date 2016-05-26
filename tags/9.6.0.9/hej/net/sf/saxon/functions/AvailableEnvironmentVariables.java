////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
// Copyright (c) 2014 Saxonica Limited.
// This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
// If a copy of the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
// This Source Code Form is "Incompatible With Secondary Licenses", as defined by the Mozilla Public License, v. 2.0.
////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

package net.sf.saxon.functions;

import net.sf.saxon.expr.Callable;
import net.sf.saxon.expr.Expression;
import net.sf.saxon.expr.XPathContext;
import net.sf.saxon.expr.parser.ExpressionVisitor;
import net.sf.saxon.lib.EnvironmentVariableResolver;
import net.sf.saxon.lib.FeatureKeys;
import net.sf.saxon.om.Sequence;
import net.sf.saxon.om.SequenceIterator;
import net.sf.saxon.trans.XPathException;
import net.sf.saxon.tree.iter.ListIterator;
import net.sf.saxon.value.SequenceExtent;
import net.sf.saxon.value.StringValue;

import java.util.ArrayList;
import java.util.List;

public class AvailableEnvironmentVariables extends SystemFunctionCall implements Callable {

    /**
     * preEvaluate: this method suppresses compile-time evaluation by doing nothing
     *
     * @param visitor an expression visitor
     */

    public Expression preEvaluate(ExpressionVisitor visitor) {
        return this;
    }

    /**
     * Evaluate the expression (dynamic evaluation)
     *
     * @param context   the dynamic evaluation context
     * @param arguments the values of the arguments, supplied as SequenceIterators
     * @return the result of the evaluation, in the form of a SequenceIterator
     * @throws net.sf.saxon.trans.XPathException
     *          if a dynamic error occurs during the evaluation of the expression
     */
    public Sequence call(XPathContext context, Sequence[] arguments) throws XPathException {
        return new SequenceExtent(getNames(context));
    }

    /*@NotNull*/
    public SequenceIterator iterate(XPathContext context) throws XPathException {
        return new ListIterator(getNames(context));
    }

    public static List<StringValue> getNames(XPathContext context) {
        EnvironmentVariableResolver resolver = (EnvironmentVariableResolver) context.getConfiguration().getConfigurationProperty(
                FeatureKeys.ENVIRONMENT_VARIABLE_RESOLVER);
        List<StringValue> myList = new ArrayList<StringValue>();
        for (java.lang.String s : resolver.getAvailableEnvironmentVariables()) {
            myList.add(new StringValue(s));
        }
        return myList;
    }

}

//Copyright (c) 2010-2012 Saxonica Limited. All rights reserved.