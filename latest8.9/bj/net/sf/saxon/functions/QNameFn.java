package net.sf.saxon.functions;
import net.sf.saxon.expr.Expression;
import net.sf.saxon.expr.StaticContext;
import net.sf.saxon.expr.XPathContext;
import net.sf.saxon.expr.Literal;
import net.sf.saxon.om.Item;
import net.sf.saxon.om.NameChecker;
import net.sf.saxon.om.QNameException;
import net.sf.saxon.trans.DynamicError;
import net.sf.saxon.trans.XPathException;
import net.sf.saxon.value.AtomicValue;
import net.sf.saxon.value.QNameValue;
import net.sf.saxon.type.BuiltInAtomicType;


/**
* This class supports the fn:QName() function (previously named fn:expanded-QName())
*/

public class QNameFn extends SystemFunction {

    /**
     * Pre-evaluate a function at compile time. Functions that do not allow
     * pre-evaluation, or that need access to context information, can override this method.
     */

    public Expression preEvaluate(StaticContext env) throws XPathException {
        try {
            final Item item1 = argument[1].evaluateItem(env.makeEarlyEvaluationContext());
            final String lex = item1.getStringValue();
            final Item item0 = argument[0].evaluateItem(env.makeEarlyEvaluationContext());
            String uri;
            if (item0 == null) {
                uri = "";
            } else {
                uri = item0.getStringValue();
            }
            final NameChecker checker = env.getConfiguration().getNameChecker();
            final String[] parts = checker.getQNameParts(lex);
            // The QNameValue constructor does not check the prefix
            if (!parts[0].equals("") && !checker.isValidNCName(parts[0])) {
                DynamicError err = new DynamicError("Malformed prefix in QName: '" + parts[0] + '\'');
                err.setErrorCode("FOCA0002");
                throw err;
            }
            return Literal.makeLiteral(
                    new QNameValue(parts[0], uri, parts[1], BuiltInAtomicType.QNAME, checker));
        } catch (QNameException e) {
            DynamicError err = new DynamicError(e.getMessage(), this);
            err.setErrorCode("FOCA0002");
            err.setLocator(this);
            throw err;
        } catch (XPathException err) {
            if (err.getLocator() == null) {
                err.setLocator(this);
            }
            throw err;
        }
    }

    /**
    * Evaluate the expression
    */

    public Item evaluateItem(XPathContext context) throws XPathException {
        AtomicValue arg0 = (AtomicValue)argument[0].evaluateItem(context);

        String uri;
        if (arg0 == null) {
            uri = null;
        } else {
            uri = arg0.getStringValue();
        }

        try {
            final String lex = argument[1].evaluateItem(context).getStringValue();
            final NameChecker checker = context.getConfiguration().getNameChecker();
            final String[] parts = checker.getQNameParts(lex);
            // The QNameValue constructor does not check the prefix
            if (!parts[0].equals("") && !checker.isValidNCName(parts[0])) {
                DynamicError err = new DynamicError("Malformed prefix in QName: '" + parts[0] + '\'');
                err.setErrorCode("FORG0001");
                throw err;
            }
            return new QNameValue(parts[0], uri, parts[1], BuiltInAtomicType.QNAME, checker);
        } catch (QNameException e) {
            dynamicError(e.getMessage(), "FOCA0002", context);
            return null;
        } catch (XPathException err) {
            if (err.getLocator() == null) {
                err.setLocator(this);
            }
            throw err;
        }
    }

}

// Copyright (c) Saxonica Limited 2006. All rights reserved.