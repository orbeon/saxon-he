////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
// Copyright (c) 2015 Saxonica Limited.
// This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
// If a copy of the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
// This Source Code Form is "Incompatible With Secondary Licenses", as defined by the Mozilla Public License, v. 2.0.
////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

package net.sf.saxon.pattern;

import net.sf.saxon.expr.*;
import net.sf.saxon.expr.parser.*;
import net.sf.saxon.functions.Doc;
import net.sf.saxon.functions.SuperId;
import net.sf.saxon.functions.KeyFn;
import net.sf.saxon.om.AxisInfo;
import net.sf.saxon.om.StructuredQName;
import net.sf.saxon.trans.XPathException;
import net.sf.saxon.type.AnyItemType;

/**
 * Parser for XSLT patterns. This is created by overriding selected parts of the standard ExpressionParser.
 */

public class PatternParser20 extends XPathParser implements PatternParser {

    int inPredicate = 0;

    /**
     * Parse a string representing an XSLT pattern
     *
     *
     * @param pattern the pattern expressed as a String
     * @param env     the static context for the pattern
     * @param lineNumber
     * @return a Pattern object representing the result of parsing
     * @throws XPathException if the pattern contains a syntax error
     */

    /*@NotNull*/
    public Pattern parsePattern(String pattern, StaticContext env, int lineNumber) throws XPathException {
        this.env = env;
        language = XSLT_PATTERN;
        Expression exp = parse(pattern, 0, Token.EOF, env);
        exp.setRetainedStaticContext(env.makeRetainedStaticContext());
        ExpressionVisitor visitor = ExpressionVisitor.make(env);
        ContextItemStaticInfo cit = new ContextItemStaticInfo(AnyItemType.getInstance(), true);
        Pattern pat = PatternMaker.fromExpression(exp.simplify().typeCheck(visitor, cit), env.getConfiguration(), false);
        if (exp instanceof FilterExpression && pat instanceof NodeTestPattern) {
            // the pattern has been simplified but needs to retain a default priority based on its syntactic form (test match89)
            ((NodeTestPattern) pat).setPriority(0.5);
        }
        return pat;
    }

    /**
     * Callback to tailor the tokenizer
     */

    protected void customizeTokenizer(Tokenizer t) {
        t.disallowUnionKeyword = true;
    }

    /**
     * Override the parsing of top-level expressions
     *
     * @return the parsed expression
     * @throws XPathException
     */

    /*@NotNull*/
    public Expression parseExpression() throws XPathException {
        if (inPredicate > 0) {
            return super.parseExpression();
        } else {
            return parseBinaryExpression(parsePathExpression(), 10);
        }
    }

    /**
     * Parse a basic step expression (without the predicates)
     *
     * @param firstInPattern true only if we are parsing the first step in a
     *                       RelativePathPattern in the XSLT Pattern syntax
     * @return the resulting subexpression
     * @throws XPathException if any error is encountered
     */

    /*@NotNull*/
    protected Expression parseBasicStep(boolean firstInPattern) throws XPathException {
        if (inPredicate > 0) {
            return super.parseBasicStep(firstInPattern);
        } else {
            switch (t.currentToken) {
                case Token.DOLLAR:
                    grumble("A variable reference is not allowed in an XSLT pattern (except in a predicate)");
                    return null;

                case Token.LPAR:
                    grumble("Parentheses are not allowed in an XSLT 2.0 pattern");
                    return null;

                case Token.STRING_LITERAL:
                case Token.NUMBER:
                case Token.KEYWORD_CURLY:
                case Token.ELEMENT_QNAME:
                case Token.ATTRIBUTE_QNAME:
                case Token.NAMESPACE_QNAME:
                case Token.PI_QNAME:
                case Token.TAG:
                case Token.NAMED_FUNCTION_REF:
                case Token.DOTDOT:
                    grumble("Token " + currentTokenDisplay() + " not allowed here in an XSLT pattern");
                    return null;

                case Token.FUNCTION:
                    if (!firstInPattern) {
                        grumble("In an XSLT pattern, a function call is allowed only as the first step in a path");
                    }
                    return super.parseBasicStep(firstInPattern);
                default:
                    return super.parseBasicStep(firstInPattern);

            }
        }
    }

    @Override
    protected void testPermittedAxis(byte axis, String errorCode) throws XPathException {
        if (inPredicate == 0) {
            if (axis != AxisInfo.CHILD && axis != AxisInfo.ATTRIBUTE) {
                grumble("Unless XSLT 3.0 is enabled, the only axes allowed in a pattern are the child and attribute axes");
            }

        }
    }

    /**
     * Parse an expression appearing within a predicate. This enables full XPath parsing, without
     * the normal rules that apply within an XSLT pattern
     *
     * @return the parsed expression that appears within the predicate
     * @throws XPathException
     */

    /*@NotNull*/
    protected Expression parsePredicate() throws XPathException {
        boolean disallow = t.disallowUnionKeyword;
        t.disallowUnionKeyword = false;
        ++inPredicate;
        Expression exp = parseExpression();
        --inPredicate;
        t.disallowUnionKeyword = disallow;
        return exp;
    }

    /**
     * Parse a function call appearing within a pattern. Unless within a predicate, this
     * imposes the constraints on which function calls are allowed to appear in a pattern
     *
     * @return the expression that results from the parsing (usually a FunctionCall)
     * @throws XPathException
     * @param prefixArgument
     */

    /*@NotNull*/
    public Expression parseFunctionCall(Expression prefixArgument) throws XPathException {
        Expression fn = super.parseFunctionCall(prefixArgument);
        if (inPredicate > 0) {
            return fn;
        } else {
            if (fn.isCallOn(SuperId.class)) {
                SystemFunctionCall fnc = (SystemFunctionCall)fn;
                // Only one argument allowed, which must be a string literal or variable reference
                // But the parser has already added a second argument
                if (fnc.getArity() == 1 ||
                    fnc.getArity() == 2 && fnc.getArg(1) instanceof RootExpression) {
                    Expression arg = fnc.getArg(0);
                    if (!(arg instanceof VariableReference || arg instanceof StringLiteral)) {
                        grumble("Argument to id() in a pattern must be a variable reference or string literal");
                    }
                } else {
                    grumble("id() in an XSLT 2.0 pattern must have only one argument");
                }

            } else if (fn.isCallOn(KeyFn.class)) {
                SystemFunctionCall fnc = (SystemFunctionCall) fn;
                // Only two arguments allowed
                // But the parser has already added a second argument
                if (fnc.getArity() == 2 ||
                    fnc.getArity() == 3 && fnc.getArg(2) instanceof RootExpression) {
                    Expression arg0 = fnc.getArg(0);
                    if (!(arg0 instanceof StringLiteral)) {
                        grumble("First argument to key() in an XSLT 2.0 pattern must be a string literal");
                    }
                    Expression arg1 = fnc.getArg(1);
                    if (!(arg1 instanceof VariableReference || arg1 instanceof Literal)) {
                        grumble("Second argument to id() in an XSLT 2.0 pattern must be a variable reference or literal");
                    }
                } else {
                    grumble("key() in an XSLT 2.0 pattern must have exactly two arguments");
                }

            } else if (fn.isCallOn(Doc.class)) {

                grumble("The doc() function is not allowed in an XSLT 2.0 pattern");

            } else {
                grumble("The " + fn.toString() + " function is not allowed at the head of a pattern");
            }
        }
        return fn;
    }

    public Expression parseFunctionArgument() throws XPathException {
        if (inPredicate > 0) {
            return super.parseFunctionArgument();
        } else {
            switch (t.currentToken) {
                case Token.DOLLAR:
                    return parseVariableReference();

                case Token.STRING_LITERAL:
                    return parseStringLiteral(true);

                case Token.NUMBER:
                    return parseNumericLiteral(true);

                default:
                    grumble("A function argument in an XSLT pattern must be a variable reference or literal");
                    return null;
            }
        }
    }

    public Expression makeTracer(int startOffset, Expression exp, int construct, StructuredQName qName) {
        // Suppress tracing of pattern evaluation
        return exp;
    }
}

