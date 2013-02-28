package net.sf.saxon.functions;

import net.sf.saxon.Configuration;
import net.sf.saxon.expr.*;
import net.sf.saxon.expr.parser.ExpressionVisitor;
import net.sf.saxon.lib.NamespaceConstant;
import net.sf.saxon.om.Item;
import net.sf.saxon.om.QNameException;
import net.sf.saxon.om.SequenceIterator;
import net.sf.saxon.om.StructuredQName;
import net.sf.saxon.trans.XPathException;
import net.sf.saxon.tree.iter.SingletonIterator;
import net.sf.saxon.value.AtomicValue;
import net.sf.saxon.value.BooleanValue;
import net.sf.saxon.value.NumericValue;
import net.sf.saxon.value.StringValue;

/**
* This class supports the XSLT element-available and function-available functions.
*/

public class FunctionAvailable extends Available implements CallableExpression {

    /**
    * preEvaluate: this method uses the static context to do early evaluation of the function
    * if the argument is known (which is the normal case)
     * @param visitor the expression visitor
     */

    public Expression preEvaluate(ExpressionVisitor visitor) throws XPathException {
        String lexicalQName = ((Literal)argument[0]).getValue().getStringValue();
        StaticContext env = visitor.getStaticContext();
        boolean b;
        Configuration config = visitor.getConfiguration();

        long arity = -1;
        if (argument.length == 2) {
            arity = ((NumericValue)argument[1].evaluateItem(env.makeEarlyEvaluationContext())).longValue();
        }
        try {
            String[] parts = config.getNameChecker().getQNameParts(lexicalQName);
            String prefix = parts[0];
            String uri;
            if (prefix.length() == 0) {
                uri = env.getDefaultFunctionNamespace();
            } else {
                uri = env.getURIForPrefix(prefix);
            }
            StructuredQName functionName = new StructuredQName(prefix, uri, parts[1]);
            b = (env.getFunctionLibrary().getFunctionSignature(functionName, (int)arity)) != null;
        } catch (QNameException e) {
            XPathException err = new XPathException(e.getMessage());
            err.setErrorCode("XTDE1400");
            throw err;
        } catch (XPathException e2) {
            if ("XTDE0290".equals(e2.getErrorCodeLocalPart())) {
                e2.setErrorCode("XTDE1400");
            }
            throw e2;
        }

        return Literal.makeLiteral(BooleanValue.get(b));
    }

    /**
     * Run-time evaluation. This is the only thing in the spec that requires information
     * about in-scope functions to be available at run-time. However, we keep it because
     * it's handy for some other things such as saxon:evaluate().
    */

    public Item evaluateItem(/*@NotNull*/ XPathContext context) throws XPathException {
        AtomicValue av1 = (AtomicValue)argument[0].evaluateItem(context);
        long arity = -1;
        if (argument.length == 2) {
            arity = ((NumericValue)argument[1].evaluateItem(context)).longValue();
        }
        StringValue nameValue = (StringValue)av1;
        String lexicalName = nameValue.getStringValue();
        boolean b = isFunctionAvailable(lexicalName, (int)arity, context);

        return BooleanValue.get(b);

    }

    private boolean isFunctionAvailable(String lexicalName, int arity, XPathContext context) throws XPathException {
        StructuredQName qName;
        try {
            if (lexicalName.indexOf(':') < 0) {
                // we're in XSLT, where the default namespace for functions can't be changed
                String uri = NamespaceConstant.FN;
                qName = new StructuredQName("", uri, lexicalName);
            } else {
                qName = StructuredQName.fromLexicalQName(lexicalName,
                    false,
                    context.getConfiguration().getNameChecker(),
                    nsContext);
            }
        } catch (XPathException e) {
            e.setErrorCode("XTDE1400");
            e.setLocator(this);
            e.setXPathContext(context);
            throw e;
        }

        final FunctionLibrary lib = context.getController().getExecutable().getFunctionLibrary();
        return lib.getFunctionSignature(qName, arity) != null;
    }

    /**
     * Evaluate the expression
     *
     * @param arguments the values of the arguments, supplied as SequenceIterators
     * @param context   the dynamic evaluation context
     * @return the result of the evaluation, in the form of a SequenceIterator
     * @throws net.sf.saxon.trans.XPathException
     *          if a dynamic error occurs during the evaluation of the expression
     */
    public SequenceIterator call(SequenceIterator[] arguments, XPathContext context) throws XPathException {
        String lexicalQName = arguments[0].next().getStringValue();
        int arity = -1;
        if (arguments.length == 2) {
            arity = (int)((NumericValue)arguments[1].next()).longValue();
        }
        boolean b = isFunctionAvailable(lexicalQName, arity, context);
        return SingletonIterator.makeIterator(BooleanValue.get(b));
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