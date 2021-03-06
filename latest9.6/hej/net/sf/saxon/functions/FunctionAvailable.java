////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
// Copyright (c) 2014 Saxonica Limited.
// This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
// If a copy of the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
// This Source Code Form is "Incompatible With Secondary Licenses", as defined by the Mozilla Public License, v. 2.0.
////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

package net.sf.saxon.functions;

import net.sf.saxon.expr.*;
import net.sf.saxon.expr.parser.ExpressionVisitor;
import net.sf.saxon.lib.NamespaceConstant;
import net.sf.saxon.om.*;
import net.sf.saxon.trans.SymbolicName;
import net.sf.saxon.trans.XPathException;
import net.sf.saxon.value.AtomicValue;
import net.sf.saxon.value.BooleanValue;
import net.sf.saxon.value.NumericValue;
import net.sf.saxon.value.StringValue;

/**
 * This class supports the XSLT element-available and function-available functions.
 */

public class FunctionAvailable extends Available implements Callable {

    /**
     * preEvaluate: this method uses the static context to do early evaluation of the function
     * if the argument is known (which is the normal case)
     *
     * @param visitor the expression visitor
     */

    public Expression preEvaluate(ExpressionVisitor visitor) throws XPathException {
        String lexicalQName = ((Literal) argument[0]).getValue().getStringValue();
        StaticContext env = visitor.getStaticContext();
        boolean b = false;

        int minArity = 0;
        int maxArity = 20;
        if (argument.length == 2) {
            minArity = (int) ((NumericValue) argument[1].evaluateItem(env.makeEarlyEvaluationContext())).longValue();
            maxArity = minArity;
        }
        try {
            String[] parts = NameChecker.getQNameParts(lexicalQName);
            String prefix = parts[0];
            String uri;
            if (prefix.length() == 0) {
                uri = env.getDefaultFunctionNamespace();
            } else {
                uri = env.getURIForPrefix(prefix);
            }

            StructuredQName functionName = new StructuredQName(prefix, uri, parts[1]);
            // Special-case exslt:node-set : bug 2212
            if ("node-set".equals(parts[1]) && NamespaceConstant.EXSLT_COMMON.equals(uri) && minArity<=1 && maxArity>=1) {
                return Literal.makeLiteral(BooleanValue.TRUE, getContainer());
            }
            for (int i = minArity; i <= maxArity; i++) {
                SymbolicName sn = new SymbolicName(StandardNames.XSL_FUNCTION, functionName, i);
                if (env.getFunctionLibrary().isAvailable(sn)) {
                    b = true;
                    break;
                }
            }
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

        return Literal.makeLiteral(BooleanValue.get(b), getContainer());
    }

    /**
     * Run-time evaluation. This is the only thing in the spec that requires information
     * about in-scope functions to be available at run-time. However, we keep it because
     * it's handy for some other things such as saxon:evaluate().
     */

    public BooleanValue evaluateItem(/*@NotNull*/ XPathContext context) throws XPathException {
        AtomicValue av1 = (AtomicValue) argument[0].evaluateItem(context);
        long arity = -1;
        if (argument.length == 2) {
            arity = ((NumericValue) argument[1].evaluateItem(context)).longValue();
        }
        StringValue nameValue = (StringValue) av1;
        String lexicalName = nameValue.getStringValue();
        boolean b = isFunctionAvailable(lexicalName, (int) arity, context);

        return BooleanValue.get(b);

    }

    private boolean isFunctionAvailable(String lexicalName, int arity, XPathContext context) throws XPathException {
        if (arity == -1) {
            for (int i = 0; i < 20; i++) {
                if (isFunctionAvailable(lexicalName, i, context)) {
                    return true;
                }
            }
            return false;
        }
        StructuredQName qName;
        try {
            if (lexicalName.indexOf(':') < 0 && !lexicalName.startsWith("Q{")) {
                // we're in XSLT, where the default namespace for functions can't be changed
                String uri = NamespaceConstant.FN;
                qName = new StructuredQName("", uri, lexicalName);
            } else {
                boolean is30 = context.getController().getExecutable().isAllowXPath30();
                qName = StructuredQName.fromLexicalQName(lexicalName,
                        false, is30,
                        nsContext);
            }
        } catch (XPathException e) {
            e.setErrorCode("XTDE1400");
            e.setLocator(this);
            e.setXPathContext(context);
            throw e;
        }

        // Special-case exslt:node-set : bug 2212
        if ("node-set".equals(qName.getLocalPart()) && qName.hasURI(NamespaceConstant.EXSLT_COMMON) && arity==1) {
            return true;
        }

        final FunctionLibrary lib = context.getController().getExecutable().getFunctionLibrary();
        SymbolicName sn = new SymbolicName(StandardNames.XSL_FUNCTION, qName, arity);
        return lib.isAvailable(sn);
    }

    /**
     * Evaluate the expression
     *
     * @param context   the dynamic evaluation context
     * @param arguments the values of the arguments, supplied as SequenceIterators
     * @return the result of the evaluation, in the form of a SequenceIterator
     * @throws net.sf.saxon.trans.XPathException
     *          if a dynamic error occurs during the evaluation of the expression
     */
    public BooleanValue call(XPathContext context, Sequence[] arguments) throws XPathException {
        String lexicalQName = arguments[0].head().getStringValue();
        int arity = -1;
        if (arguments.length == 2) {
            arity = (int) ((NumericValue) arguments[1].head()).longValue();
        }
        return BooleanValue.get(isFunctionAvailable(lexicalQName, arity, context));
    }
}

