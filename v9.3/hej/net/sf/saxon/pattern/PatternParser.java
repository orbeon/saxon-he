package net.sf.saxon.pattern;

import net.sf.saxon.expr.*;
import net.sf.saxon.functions.Doc;
import net.sf.saxon.functions.Id;
import net.sf.saxon.functions.KeyFn;
import net.sf.saxon.om.StructuredQName;
import net.sf.saxon.trans.XPathException;
import net.sf.saxon.value.DecimalValue;

/**
 * Parser for XSLT patterns. This is created by overriding selected parts of the standard ExpressionParser.
 */

public class PatternParser extends ExpressionParser {

    int inPredicate = 0;

    /**
     * Parse a string representing an XSLT pattern
     * @param pattern the pattern expressed as a String
     * @param env     the static context for the pattern
     * @return a Pattern object representing the result of parsing
     * @throws XPathException if the pattern contains a syntax error
     */

    public Pattern parsePattern(String pattern, StaticContext env) throws XPathException {
        this.env = env;
        nameChecker = env.getConfiguration().getNameChecker();
        language = XSLT_PATTERN;
        Expression exp = parse(pattern, 0, Token.EOF, 0, env);
        exp.setContainer(defaultContainer);
        ExpressionVisitor visitor = ExpressionVisitor.make(env, exp.getExecutable());
        return PatternMaker.fromExpression(exp.simplify(visitor), env.getConfiguration());
    }


    public Expression parseExpression() throws XPathException {
        if (inPredicate > 0) {
            return super.parseExpression();
        } else {
            return parseUnionExpression();
        }
    }

    protected Expression parseUnionExpression() throws XPathException {
        if (inPredicate > 0) {
            return super.parseUnionExpression();
        } else {
            Expression exp = parsePathExpression();
            while (t.currentToken == Token.UNION) {
                if (t.currentTokenValue.equals("union") && !env.getXPathLanguageLevel().equals(DecimalValue.THREE)) {
                    grumble("Union operator in an XSLT 2.0 pattern must be written as '|'");
                }
                nextToken();
                exp = new VennExpression(exp, Token.UNION, parsePathExpression());
                setLocation(exp);
            }
            return exp;
        }
    }

    /**
     * Parse a basic step expression (without the predicates)
     * @param firstInPattern true only if we are parsing the first step in a
     *                       RelativePathPattern in the XSLT Pattern syntax
     * @return the resulting subexpression
     * @throws XPathException if any error is encountered
     */

    protected Expression parseBasicStep(boolean firstInPattern) throws XPathException {
        if (inPredicate > 0) {
            return super.parseBasicStep(firstInPattern);
        } else {
            switch (t.currentToken) {
                case Token.DOLLAR:
                    if (!env.getXPathLanguageLevel().equals(DecimalValue.THREE)) {
                        grumble("A variable reference is not allowed in an XSLT pattern (except in a predicate)");
                        return null;
                    } else if (!firstInPattern) {
                        grumble("In an XSLT 3.0 pattern, a variable reference is allowed only as the first step in a path");
                        return null;
                    } else {
                        return super.parseBasicStep(firstInPattern);
                    }
                case Token.LPAR:
                case Token.STRING_LITERAL:
                case Token.NUMBER:
                case Token.KEYWORD_CURLY:
                case Token.ELEMENT_QNAME:
                case Token.ATTRIBUTE_QNAME:
                case Token.NAMESPACE_QNAME:
                case Token.PI_QNAME:
                case Token.TAG:
                case Token.INLINE_FUNCTION_LITERAL:
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

    protected Expression parsePredicate() throws XPathException {
        ++inPredicate;
        Expression exp = parseExpression();
        --inPredicate;
        return exp;
    }

    protected Expression parseFunctionCall() throws XPathException {
        Expression fn = super.parseFunctionCall();
        if (inPredicate > 0) {
            return fn;
        } else {
            if (fn instanceof Id) {
                if (!env.getXPathLanguageLevel().equals(DecimalValue.THREE)) {
                    // Only one argument allowed, which must be a string literal or variable reference
                    if (((Id)fn).getNumberOfArguments() != 1) {
                        grumble("id() in an XSLT 2.0 pattern must have only one argument");
                    } else {
                        Expression arg = ((Id)fn).getArguments()[0];
                        if (!(arg instanceof VariableReference || arg instanceof StringLiteral)) {
                             grumble("Argument to id() in a pattern must be a variable reference or string literal");
                        }
                    }
                }
            } else if (fn instanceof KeyFn) {
                if (!env.getXPathLanguageLevel().equals(DecimalValue.THREE)) {
                    // Only two arguments allowed
                    if (((KeyFn)fn).getNumberOfArguments() != 2) {
                        grumble("key() in an XSLT 2.0 pattern must have exactly two arguments");
                    } else {
                        Expression arg0 = ((KeyFn)fn).getArguments()[0];
                        if (!(arg0 instanceof StringLiteral)) {
                             grumble("First argument to key() in an XSLT 2.0 pattern must be a string literal");
                        }
                        Expression arg1 = ((KeyFn)fn).getArguments()[1];
                        if (!(arg1 instanceof VariableReference || arg1 instanceof Literal)) {
                             grumble("Second argument to id() in an XSLT 2.0 pattern must be a variable reference or literal");
                        }
                    }
                }
            } else if (fn instanceof Doc) {
                if (!env.getXPathLanguageLevel().equals(DecimalValue.THREE)) {
                    grumble("The doc() function is not allowed in an XSLT 2.0 pattern");
                }
            } else {
                grumble("The " + fn.toString() + " function is not allowed at the head of a pattern");
            }
        }
        return fn;
    }

    protected Expression parseFunctionArgument() throws XPathException {
        if (inPredicate > 0) {
            return super.parseFunctionArgument();
        } else {
            switch(t.currentToken) {
                case Token.DOLLAR:
                    return parseVariableReference();

                case Token.STRING_LITERAL:
                    return parseStringLiteral();

                case Token.NUMBER:
                    return parseNumericLiteral();

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



