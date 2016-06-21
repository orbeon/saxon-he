////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
// Copyright (c) 2015 Saxonica Limited.
// This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
// If a copy of the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
// This Source Code Form is "Incompatible With Secondary Licenses", as defined by the Mozilla Public License, v. 2.0.
////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

package net.sf.saxon.expr.parser;

import net.sf.saxon.functions.ApplyFn;
import net.sf.saxon.Configuration;
import net.sf.saxon.Version;
import net.sf.saxon.expr.*;
import net.sf.saxon.expr.flwor.Clause;
import net.sf.saxon.expr.instruct.Block;
import net.sf.saxon.expr.instruct.Choose;
import net.sf.saxon.expr.instruct.ForEach;
import net.sf.saxon.expr.instruct.UserFunction;
import net.sf.saxon.functions.*;
import net.sf.saxon.lib.FeatureKeys;
import net.sf.saxon.lib.NamespaceConstant;
import net.sf.saxon.ma.arrays.*;
import net.sf.saxon.ma.map.*;
import net.sf.saxon.om.*;
import net.sf.saxon.pattern.*;
import net.sf.saxon.query.Annotation;
import net.sf.saxon.style.ExpressionContext;
import net.sf.saxon.style.PrincipalStylesheetModule;
import net.sf.saxon.style.StylesheetPackage;
import net.sf.saxon.trace.LocationKind;
import net.sf.saxon.trans.Err;
import net.sf.saxon.trans.SymbolicName;
import net.sf.saxon.trans.XPathException;
import net.sf.saxon.type.*;
import net.sf.saxon.value.*;
import net.sf.saxon.z.IntArraySet;
import net.sf.saxon.z.IntPredicate;
import net.sf.saxon.z.IntSet;

import java.util.*;

import static net.sf.saxon.type.BuiltInAtomicType.*;

/**
 * Parser for XPath expressions and XSLT patterns.
 * <p/>
 * This code was originally inspired by James Clark's xt but has been totally rewritten (several times)
 * <p/>
 * The base class handles parsing of XPath 2.0, XPath 3.0 and XPath 3.1 syntax (switched by a languageVersion variable).
 * Subclasses refine this to handle XQuery syntax (1.0, 3.0 and 3.1) and XQuery Update syntax.
 *
 * @author Michael Kay
 */


public class XPathParser {

    protected Tokenizer t;
    protected StaticContext env;
    protected Stack<LocalBinding> rangeVariables = new Stack<LocalBinding>();
    // The stack holds a list of range variables that are in scope.
    // Each entry on the stack is a Binding object containing details
    // of the variable.

    protected QNameParser qNameParser;

    protected IntPredicate charChecker;

    protected boolean allowXPath30Syntax = false;
    protected boolean allowXPath31Syntax = false;

    protected boolean scanOnly = false;
    // scanOnly is set to true while attributes in direct element constructors
    // are being processed. We need to parse enclosed expressions in the attribute
    // in order to find the end of the attribute value, but we don't yet know the
    // full namespace context at this stage.

    private boolean allowAbsentExpression = false;
    // allowAbsentExpression is a flag that indicates that it is acceptable
    // for the expression to be empty (that is, to consist solely of whitespace and
    // comments). The result of parsing such an expression is equivalent to the
    // result of parsing an empty sequence literal, "()"

    /*@Nullable*/
    protected CodeInjector codeInjector = null;

    protected int language = XPATH;     // know which language we are parsing, for diagnostics
    public static final int XPATH = 0;
    public static final int XSLT_PATTERN = 1;
    public static final int SEQUENCE_TYPE = 2;
    public static final int XQUERY = 3;
    public static final int EXTENDED_ITEM_TYPE = 4;

    protected int languageVersion = 20;
    protected int catchDepth = 0;

    /**
     * Create an expression parser
     */

    public XPathParser() {
    }

    /**
     * Set a CodeInjector which can be used to modify or wrap expressions on the tree
     * as the expression is parsed and the tree is constructed. This is typically used
     * to add tracing code.
     *
     * @param injector the code injector to be used
     */

    public void setCodeInjector(/*@Nullable*/ CodeInjector injector) {
        this.codeInjector = injector;
    }

    /**
     * Set a CodeInjector which can be used to modify or wrap expressions on the tree
     * as the expression is parsed and the tree is constructed. This is typically used
     * to add tracing code.
     *
     * @return the code injector in use, if any; or null otherwise
     */

    /*@Nullable*/
    public CodeInjector getCodeInjector() {
        return codeInjector;
    }

    /**
     * Get the tokenizer (the lexical analyzer)
     *
     * @return the tokenizer (the lexical analyzer)
     */

    public Tokenizer getTokenizer() {
        return t;
    }

    /**
     * Get the static context used by this expression parser
     *
     * @return the static context
     */

    public StaticContext getStaticContext() {
        return env;
    }

//    /**
//     * Set the default container for newly constructed expressions
//     *
//     * @param container the default container
//     */
//
//    public void setDefaultContainer(Container container) {
//        this.defaultContainer = container;
//    }
//
//    /**
//     * Get the default container for newly constructed expressions
//     *
//     * @return the default container
//     */
//
//    public Container getDefaultContainer() {
//        return defaultContainer;
//    }

    /**
     * Set the depth of nesting within try/catch
     *
     * @param depth the depth of nesting
     */

    public void setCatchDepth(int depth) {
        catchDepth = depth;
    }

    /**
     * Read the next token, catching any exception thrown by the tokenizer
     *
     * @throws XPathException if an invalid token is found
     */

    public void nextToken() throws XPathException {
        try {
            t.next();
            if ((t.currentToken == Token.NAME || t.currentToken == Token.FUNCTION) &&
                    t.currentTokenValue.startsWith("Q{")) {
                if (allowXPath30Syntax) {
                    t.currentTokenValue = normalizeEQName(t.currentTokenValue);
                } else {
                    throw new XPathException("The expanded QName syntax Q{uri}local is not allowed in this version of XPath/XQuery");
                }
            }
        } catch (XPathException e) {
            grumble(e.getMessage());
        }
    }

    /**
     * Expect a given token; fail if the current token is different. Note that this method
     * does not read any tokens.
     *
     * @param token the expected token
     * @throws XPathException if the current token is not the expected
     *                        token
     */

    public void expect(int token) throws XPathException {
        if (t.currentToken != token) {
            grumble("expected \"" + Token.tokens[token] +
                    "\", found " + currentTokenDisplay());
        }
    }

    /**
     * Report a syntax error (a static error with error code XPST0003)
     *
     * @param message the error message
     * @throws XPathException always thrown: an exception containing the
     *                        supplied message
     */

    public void grumble(String message) throws XPathException {
        grumble(message, language == XSLT_PATTERN ? "XTSE0340" : "XPST0003");
    }

    /**
     * Report a static error
     *
     * @param message   the error message
     * @param errorCode the error code
     * @throws XPathException always thrown: an exception containing the
     *                        supplied message
     */

    public void grumble(String message, String errorCode) throws XPathException {
        grumble(message, new StructuredQName("", NamespaceConstant.ERR, errorCode), -1);
    }

    /**
     * Report a static error, with location information
     *
     * @param message   the error message
     * @param errorCode the error code
     * @param offset    the coded location of the error, or -1 if the location of the current token should be used
     * @throws XPathException always thrown: an exception containing the
     *                        supplied message
     */

    public void grumble(String message, String errorCode, int offset) throws XPathException {
        grumble(message, new StructuredQName("", NamespaceConstant.ERR, errorCode), offset);
    }

    /**
     * Report a static error
     *
     * @param message   the error message
     * @param errorCode the error code
     * @param offset    the coded location of the error, or -1 if the location of the current token should be used
     * @throws XPathException always thrown: an exception containing the
     *                        supplied message
     */

    protected void grumble(String message, StructuredQName errorCode, int offset) throws XPathException {
        if (errorCode == null) {
            errorCode = new StructuredQName("err", NamespaceConstant.ERR, "XPST0003");
        }
        String nearbyText = t.recentText(-1);
        int line;
        int column;
        if (offset == -1) {
            line = t.getLineNumber();
            column = t.getColumnNumber();
        } else {
            line = t.getLineNumber(offset);
            column = t.getColumnNumber(offset);
        }
        Location loc = makeNestedLocation(env.getContainingLocation(), line, column, nearbyText);

        XPathException err = new XPathException(message);
        err.setLocation(loc);
        err.setIsSyntaxError("XPST0003".equals(errorCode.getLocalPart()));
        err.setIsStaticError(true);
        err.setHostLanguage(getLanguage());
//        err.setAdditionalLocationText(prefix);
        err.setErrorCodeQName(errorCode);
        throw err;
    }


    /**
     * Output a warning message
     *
     * @param message the text of the message
     * @throws XPathException if the message cannot be output
     */

    protected void warning(/*@NotNull*/ String message) throws XPathException {
        String s = t.recentText(-1);
        String prefix =
                (message.startsWith("...") ? "near" : "in") +
                        ' ' + Err.wrap(s) + ":\n    ";
        env.issueWarning(prefix + message, null);
    }

    /**
     * Set the current language (XPath or XQuery, XSLT Pattern, or SequenceType)
     *
     * @param language one of the constants {@link #XPATH}, {@link #XQUERY}, {@link #XSLT_PATTERN}, {@link #SEQUENCE_TYPE}
     * @param version  The XPath or XQuery language version. For XQuery the value must be
     *                 10 (for "1.0"), 30 (for "3.0") or 31 (for "3.1"); for XPath it must be 20 (="2.0"),
     *                 30 (="3.0") or 31 (="3.1").
     */

    public void setLanguage(int language, int version) {
        if (version == 0) {
            version = 30; // default
        }
        switch (language) {
            case XPATH:
                if (!(version == 20 || version == 30 || version == 31)) {
                    throw new IllegalArgumentException("Unsupported language version " + version);
                }
                break;
            case XSLT_PATTERN:
            case SEQUENCE_TYPE:
                if (!(version == 20 || version == 30 || version == 31)) {
                    throw new IllegalArgumentException("Unsupported language version " + version);
                }
                break;
            case XQUERY:
                if (!(version == 10 || version == 30 || version == 31)) {
                    throw new IllegalArgumentException("Unsupported language version " + version);
                }
                break;
            default:
                throw new IllegalArgumentException("Unknown language " + language);
        }
        this.language = language;
        this.languageVersion = version;
        this.allowXPath30Syntax = languageVersion >= 30;
        this.allowXPath31Syntax = languageVersion >= 31;
    }

    /**
     * Get the current language (XPath or XQuery)
     *
     * @return a string representation of the language being parsed, for use in error messages
     */

    protected String getLanguage() {
        switch (language) {
            case XPATH:
                return "XPath";
            case XSLT_PATTERN:
                return "XSLT Pattern";
            case SEQUENCE_TYPE:
                return "SequenceType";
            case XQUERY:
                return "XQuery";
            case EXTENDED_ITEM_TYPE:
                return "Extended ItemType";
            default:
                return "XPath";
        }
    }

    /**
     * Ask if XPath 3.1 is in use
     *
     * @return true if XPath 3.1 syntax (and therefore XQuery 3.1 syntax) is permitted
     */

    public boolean isAllowXPath31Syntax() {
        return allowXPath31Syntax;
    }

    /**
     * Set the QNameParser to be used while parsing
     *
     * @param qp the QNameParser
     */

    public void setQNameParser(QNameParser qp) {
        this.qNameParser = qp;
    }

    /**
     * Get the QNameParser to be used while parsing
     *
     * @return the QNameParser
     */

    public QNameParser getQNameParser() {
        return qNameParser;
    }

    /**
     * Display the current token in an error message
     *
     * @return the display representation of the token
     */
    /*@NotNull*/
    protected String currentTokenDisplay() {
        if (t.currentToken == Token.NAME) {
            return "name \"" + t.currentTokenValue + '\"';
        } else if (t.currentToken == Token.UNKNOWN) {
            return "(unknown token)";
        } else {
            return '\"' + Token.tokens[t.currentToken] + '\"';
        }
    }

    /**
     * Parse a string representing an expression. This will accept an XPath expression if called on an
     * ExpressionParser, or an XQuery expression if called on a QueryParser.
     *
     * @param expression the expression expressed as a String
     * @param start      offset within the string where parsing is to start
     * @param terminator character to treat as terminating the expression
     * @param env        the static context for the expression
     * @return an Expression object representing the result of parsing
     * @throws XPathException if the expression contains a syntax error
     */

    /*@NotNull*/
    public Expression parse(String expression, int start, int terminator, StaticContext env)
            throws XPathException {
        // System.err.println("Parse expression: " + expression);
        this.env = env;
        int languageVersion = env.getXPathVersion();
        if (languageVersion == 20 && language == XQUERY) {
            languageVersion = 10;
        }
        setLanguage(language, languageVersion);

        qNameParser = new QNameParser(env.getNamespaceResolver());
        qNameParser.setAcceptEQName(env.getXPathVersion() >= 30);
        qNameParser.setDefaultNamespace("");
        qNameParser.setErrorOnBadSyntax(language == XSLT_PATTERN ? "XTSE0340" : "XPST0003");
        qNameParser.setErrorOnUnresolvedPrefix("XPST0081");

        charChecker = env.getConfiguration().getValidCharacterChecker();
        t = new Tokenizer();
        t.languageLevel = env.getXPathVersion();
        int offset = t.currentTokenStartOffset;
        customizeTokenizer(t);
        try {
            t.tokenize(expression, start, -1);
        } catch (XPathException err) {
            grumble(err.getMessage());
        }
        if (t.currentToken == terminator) {
            if (allowAbsentExpression) {
                Expression result = Literal.makeEmptySequence();
                result.setRetainedStaticContext(env.makeRetainedStaticContext());
                setLocation(result);
                return result;
            } else {
                grumble("The expression is empty");
            }
        }
        Expression exp = parseExpression();
        if (t.currentToken != terminator) {
            if (t.currentToken == Token.EOF && terminator == Token.RCURLY) {
                grumble("Missing curly brace after expression in attribute value template", "XTSE0350");
            } else {
                grumble("Unexpected token " + currentTokenDisplay() + " beyond end of expression");
            }
        }
        exp.setRetainedStaticContext(env.makeRetainedStaticContext());
        setLocation(exp, offset);
        //exp.verifyParentPointers();
        return exp;
    }

    /**
     * Callback to tailor the tokenizer
     *
     * @param t the Tokenizer to be customized
     */

    protected void customizeTokenizer(Tokenizer t) {
        // do nothing
    }


    /**
     * Parse a string representing a sequence type
     *
     * @param input the string, which should conform to the XPath SequenceType
     *              production
     * @param env   the static context
     * @return a SequenceType object representing the type
     * @throws XPathException if any error is encountered
     */

    public SequenceType parseSequenceType(String input, /*@NotNull*/ StaticContext env) throws XPathException {
        this.env = env;
        if (qNameParser == null) {
            qNameParser = new QNameParser(env.getNamespaceResolver());
        }
        language = SEQUENCE_TYPE;
        t = new Tokenizer();
        t.languageLevel = env.getXPathVersion();
        try {
            t.tokenize(input, 0, -1);
        } catch (XPathException err) {
            grumble(err.getMessage());
        }
        SequenceType req = parseSequenceType();
        if (t.currentToken != Token.EOF) {
            grumble("Unexpected token " + currentTokenDisplay() + " beyond end of SequenceType");
        }
        return req;
    }

    /**
     * Parse a string representing an extended item type: specifically, the content of itemType
     * or nodeTest attributes in an exported package. As well as regular itemType syntax, these
     * allow combined node tests separated with "|", "except", or "intersect" operators. Expressions
     * using these operators will always be parenthesized.
     *
     * @param input the string, which should conform to the XPath SequenceType
     *              production
     * @param env   the static context
     * @return a SequenceType object representing the type
     * @throws XPathException if any error is encountered
     */

    public ItemType parseExtendedItemType(String input, StaticContext env) throws XPathException {
        this.env = env;
        language = EXTENDED_ITEM_TYPE;
        t = new Tokenizer();
        t.languageLevel = env.getXPathVersion();
        try {
            t.tokenize(input, 0, -1);
        } catch (XPathException err) {
            grumble(err.getMessage());
        }
        ItemType req = parseItemType();
        if (t.currentToken != Token.EOF) {
            grumble("Unexpected token " + currentTokenDisplay() + " beyond end of SequenceType");
        }
        return req;
    }

    /**
     * Parse a string representing a sequence type with syntax extensions used in exported stylesheets
     *
     * @param input the string, which should conform to the XPath SequenceType
     *              production
     * @param env   the static context
     * @return a SequenceType object representing the type
     * @throws XPathException if any error is encountered
     */

    public SequenceType parseExtendedSequenceType(String input, /*@NotNull*/ StaticContext env) throws XPathException {
        this.env = env;
        language = EXTENDED_ITEM_TYPE;
        t = new Tokenizer();
        t.languageLevel = env.getXPathVersion();
        try {
            t.tokenize(input, 0, -1);
        } catch (XPathException err) {
            grumble(err.getMessage());
        }
        SequenceType req = parseSequenceType();
        if (t.currentToken != Token.EOF) {
            grumble("Unexpected token " + currentTokenDisplay() + " beyond end of SequenceType");
        }
        return req;
    }


    //////////////////////////////////////////////////////////////////////////////////
    //                     EXPRESSIONS                                              //
    //////////////////////////////////////////////////////////////////////////////////

    /**
     * Parse a top-level Expression:
     * ExprSingle ( ',' ExprSingle )*
     *
     * @return the Expression object that results from parsing
     * @throws XPathException if the expression contains a syntax error
     */

    /*@NotNull*/
    public Expression parseExpression() throws XPathException {
        int offset = t.currentTokenStartOffset;
        Expression exp = parseExprSingle();
        ArrayList<Expression> list = null;
        while (t.currentToken == Token.COMMA) {
            // An expression containing a comma often contains many, so we accumulate all the
            // subexpressions into a list before creating the Block expression which reduces it to an array
            if (list == null) {
                list = new ArrayList<Expression>(10);
                list.add(exp);
            }
            nextToken();
            Expression next = parseExprSingle();
            setLocation(next);
            list.add(next);
        }
        if (list != null) {
            exp = Block.makeBlock(list);
            setLocation(exp, offset);
        }
        return exp;
    }

    /**
     * Parse an ExprSingle
     *
     * @return the resulting subexpression
     * @throws XPathException if any error is encountered
     */

    /*@NotNull*/
    public Expression parseExprSingle() throws XPathException {
        switch (t.currentToken) {
            case Token.EOF:
                grumble("Expected an expression, but reached the end of the input");
            case Token.FOR:
            case Token.LET:
            case Token.FOR_SLIDING:
            case Token.FOR_TUMBLING:
                return parseFLWORExpression();
            case Token.SOME:
            case Token.EVERY:
                return parseQuantifiedExpression();
            case Token.IF:
                return parseIfExpression();
            case Token.SWITCH:
                return parseSwitchExpression();
            case Token.TYPESWITCH:
                return parseTypeswitchExpression();
            case Token.VALIDATE:
            case Token.VALIDATE_STRICT:
            case Token.VALIDATE_LAX:
            case Token.VALIDATE_TYPE:
                return parseValidateExpression();
            case Token.PRAGMA:
                return parseExtensionExpression();  // XQuery only
            case Token.KEYWORD_CURLY:
                if (t.currentTokenValue.equals("try")) {
                    return parseTryCatchExpression();
                }
                // else drop through

            default:
                return parseBinaryExpression(parseUnaryExpression(), 4);
        }
    }

    /**
     * Parse a binary expression, using operator precedence parsing. This is used
     * to parse the part of the grammar consisting largely of binary operators
     * distinguished by precedence: from "or expressions" down to "unary expressions".
     * Algorithm for the mainstream binary operators is from Wikipedia article
     * on precedence parsing;  operator precedences are from the XQuery specification
     * appendix B.
     *
     * @param lhs           Left-hand side "basic expression"
     * @param minPrecedence the minimum precedence of an operator that is to be treated as not terminating the
     *                      current expression
     * @return the parsed expression
     * @throws XPathException if a static error is found
     */

    /*@NotNull*/
    public Expression parseBinaryExpression(Expression lhs, int minPrecedence) throws XPathException {
        while (getCurrentOperatorPrecedence() >= minPrecedence) {
            int offset = t.currentTokenStartOffset;
            int operator = t.currentToken;
            int prec = getCurrentOperatorPrecedence();
            switch (operator) {
                case Token.INSTANCE_OF:
                case Token.TREAT_AS:
                    nextToken();
                    SequenceType seq = parseSequenceType();
                    lhs = makeSequenceTypeExpression(lhs, operator, seq);
                    setLocation(lhs, offset);
                    if (getCurrentOperatorPrecedence() >= prec) {
                        grumble("Left operand of '" + Token.tokens[t.currentToken] + "' needs parentheses");
                    }
                    break;
                case Token.CAST_AS:
                case Token.CASTABLE_AS:
                    nextToken();
                    expect(Token.NAME);
                    SimpleType at = getSimpleType(t.currentTokenValue);
                    if (at == ANY_ATOMIC) {
                        grumble("No value is castable to xs:anyAtomicType", "XPST0080");
                    }
                    if (at == NOTATION) {
                        grumble("No value is castable to xs:NOTATION", "XPST0080");
                    }
                    nextToken();
                    boolean allowEmpty = t.currentToken == Token.QMARK;
                    if (allowEmpty) {
                        nextToken();
                    }
                    lhs = makeSingleTypeExpression(lhs, operator, at, allowEmpty);
                    setLocation(lhs, offset);
                    if (getCurrentOperatorPrecedence() >= prec) {
                        grumble("Left operand of '" + Token.tokens[t.currentToken] + "' needs parentheses");
                    }
                    break;
                case Token.ARROW:
                    lhs = parseArrowPostfix(lhs);
                    break;
                default:
                    nextToken();
                    Expression rhs = parseUnaryExpression();
                    while (getCurrentOperatorPrecedence() > prec) {
                        rhs = parseBinaryExpression(rhs, getCurrentOperatorPrecedence());
                    }
                    if (getCurrentOperatorPrecedence() == prec && !allowMultipleOperators()) {
                        String tok = Token.tokens[t.currentToken];
                        String message = "Left operand of '" + Token.tokens[t.currentToken] + "' needs parentheses";
                        if (tok.equals("<") || tok.equals(">")) {
                            // Example input: return <a>3</a><b>4</b> - bug 2659
                            message += ". Or perhaps an XQuery element constructor appears where it is not allowed";
                        }
                        grumble(message);
                    }
                    lhs = makeBinaryExpression(lhs, operator, rhs);
                    setLocation(lhs, offset);
            }
        }
        return lhs;
    }

    private boolean allowMultipleOperators() {
        switch (t.currentToken) {
            case Token.FEQ:
            case Token.FNE:
            case Token.FLE:
            case Token.FLT:
            case Token.FGE:
            case Token.FGT:
            case Token.EQUALS:
            case Token.NE:
            case Token.LE:
            case Token.LT:
            case Token.GE:
            case Token.GT:
            case Token.IS:
            case Token.PRECEDES:
            case Token.FOLLOWS:
            case Token.TO:
                return false;
            default:
                return true;
        }
    }

    private int getCurrentOperatorPrecedence() {
        switch (t.currentToken) {
            case Token.OR:
                return 4;
            case Token.AND:
                return 5;
            case Token.FEQ:
            case Token.FNE:
            case Token.FLE:
            case Token.FLT:
            case Token.FGE:
            case Token.FGT:
            case Token.EQUALS:
            case Token.NE:
            case Token.LE:
            case Token.LT:
            case Token.GE:
            case Token.GT:
            case Token.IS:
            case Token.PRECEDES:
            case Token.FOLLOWS:
                return 6;
            case Token.CONCAT:
                return 7;
            case Token.TO:
                return 8;
            case Token.PLUS:
            case Token.MINUS:
                return 9;
            case Token.MULT:
            case Token.DIV:
            case Token.IDIV:
            case Token.MOD:
                return 10;
            case Token.UNION:
                return 11;
            case Token.INTERSECT:
            case Token.EXCEPT:
                return 12;
            case Token.INSTANCE_OF:
                return 13;
            case Token.TREAT_AS:
                return 14;
            case Token.CASTABLE_AS:
                return 15;
            case Token.CAST_AS:
                return 16;
            case Token.ARROW:
                return 17;
            default:
                return -1;
        }
    }

    /*@NotNull*/
    private Expression makeBinaryExpression(Expression lhs, int operator, Expression rhs) throws XPathException {
        switch (operator) {
            case Token.OR:
                return new OrExpression(lhs, rhs);
            case Token.AND:
                return new AndExpression(lhs, rhs);
            case Token.FEQ:
            case Token.FNE:
            case Token.FLE:
            case Token.FLT:
            case Token.FGE:
            case Token.FGT:
                return new ValueComparison(lhs, operator, rhs);
            case Token.EQUALS:
            case Token.NE:
            case Token.LE:
            case Token.LT:
            case Token.GE:
            case Token.GT:
                return new GeneralComparison(lhs, operator, rhs);
            case Token.IS:
            case Token.PRECEDES:
            case Token.FOLLOWS:
                return new IdentityComparison(lhs, operator, rhs);
            case Token.TO:
                return new RangeExpression(lhs, rhs);
            case Token.CONCAT:
                if (!allowXPath30Syntax) {
                    grumble("Concatenation operator ('||') requires XPath 3.0 to be enabled");
                }
                RetainedStaticContext rsc = new RetainedStaticContext(env);
                Expression cc = SystemFunction.makeCall("concat", rsc, lhs, rhs);
                assert cc != null;
                return cc;
            case Token.PLUS:
            case Token.MINUS:
            case Token.MULT:
            case Token.DIV:
            case Token.IDIV:
            case Token.MOD:
                return new ArithmeticExpression(lhs, operator, rhs);
            case Token.UNION:
            case Token.INTERSECT:
            case Token.EXCEPT:
                return new VennExpression(lhs, operator, rhs);
            default:
                throw new IllegalArgumentException();
        }
    }

    private Expression makeSequenceTypeExpression(Expression lhs, int operator, /*@NotNull*/ SequenceType type) {
        switch (operator) {
            case Token.INSTANCE_OF:
                return new InstanceOfExpression(lhs, type);
            case Token.TREAT_AS:
                return TreatExpression.make(lhs, type);
            default:
                throw new IllegalArgumentException();
        }

    }

    private Expression makeSingleTypeExpression(Expression lhs, int operator, /*@NotNull*/ SimpleType type, boolean allowEmpty)
            throws XPathException {
        if (type instanceof AtomicType && !(type == ErrorType.getInstance())) {
            switch (operator) {
                case Token.CASTABLE_AS:
                    CastableExpression castable = new CastableExpression(lhs, (AtomicType) type, allowEmpty);
                    if (lhs instanceof StringLiteral) {
                        castable.setOperandIsStringLiteral(true);
                    }
                    return castable;

                case Token.CAST_AS:
                    CastExpression cast = new CastExpression(lhs, (AtomicType) type, allowEmpty);
                    if (lhs instanceof StringLiteral) {
                        cast.setOperandIsStringLiteral(true);
                    }
                    return cast;

                default:
                    throw new IllegalArgumentException();
            }
        } else if (allowXPath30Syntax) {
            switch (operator) {
                case Token.CASTABLE_AS:
                    if (type.isUnionType()) {
                        NamespaceResolver resolver = env.getNamespaceResolver();
                        UnionCastableFunction ucf = new UnionCastableFunction((UnionType) type, resolver, allowEmpty);
                        return new StaticFunctionCall(ucf, new Expression[]{lhs});
                    } else if (type.isListType()) {
                        NamespaceResolver resolver = env.getNamespaceResolver();
                        ListCastableFunction lcf = new ListCastableFunction((ListType) type, resolver, allowEmpty);
                        return new StaticFunctionCall(lcf, new Expression[]{lhs});
                    }
                    break;
                case Token.CAST_AS:
                    if (type.isUnionType()) {
                        NamespaceResolver resolver = env.getNamespaceResolver();
                        UnionConstructorFunction ucf = new UnionConstructorFunction((UnionType) type, resolver, allowEmpty);
                        return new StaticFunctionCall(ucf, new Expression[]{lhs});
                    } else if (type.isListType()) {
                        NamespaceResolver resolver = env.getNamespaceResolver();
                        ListConstructorFunction lcf = new ListConstructorFunction((ListType) type, resolver, allowEmpty);
                        return new StaticFunctionCall(lcf, new Expression[]{lhs});
                    }
                    break;
                default:
                    throw new IllegalArgumentException();
            }
            if (type == AnySimpleType.getInstance()) {
                throw new XPathException("Cannot cast to xs:anySimpleType", "XPST0080");
            } else {
                throw new XPathException("Cannot cast to " + type.getDescription(), "XPST0051");
            }
        } else {
            throw new XPathException("Casting to list or union types requires XPath 3.0 to be enabled", "XPST0051");
        }

    }

    /**
     * Parse a Typeswitch Expression.
     * This construct is XQuery-only, so the XPath version of this
     * method throws an error unconditionally
     *
     * @return the expression that results from the parsing
     * @throws XPathException if a static error is found
     */

    /*@NotNull*/
    protected Expression parseTypeswitchExpression() throws XPathException {
        grumble("typeswitch is not allowed in XPath");
        return new ErrorExpression();
    }


    /**
     * Parse a Switch Expression.
     * This construct is XQuery-only.
     * SwitchExpr ::= "switch" "(" Expr ")" SwitchCaseClause+ "default" "return" ExprSingle
     * SwitchCaseClause ::= ("case" ExprSingle)+ "return" ExprSingle
     *
     * @return the parsed expression
     * @throws XPathException in the event of a syntax error
     */

    /*@NotNull*/
    protected Expression parseSwitchExpression() throws XPathException {
        grumble("switch is not allowed in XPath");
        return new ErrorExpression();
    }

    /**
     * Parse a Validate Expression.
     * This construct is XQuery-only, so the XPath version of this
     * method throws an error unconditionally
     *
     * @return the parsed expression; except that this version of the method always
     * throws an exception
     * @throws XPathException if a static error is found
     */

    /*@NotNull*/
    protected Expression parseValidateExpression() throws XPathException {
        grumble("validate{} expressions are not allowed in XPath");
        return new ErrorExpression();
    }

    /**
     * Parse an Extension Expression
     * This construct is XQuery-only, so the XPath version of this
     * method throws an error unconditionally
     *
     * @return the parsed expression; except that this version of the method
     * always throws an exception
     * @throws XPathException if a static error is found
     */

    /*@NotNull*/
    protected Expression parseExtensionExpression() throws XPathException {
        grumble("extension expressions (#...#) are not allowed in XPath");
        return new ErrorExpression();
    }


    /**
     * Parse a try/catch Expression
     * This construct is XQuery-3.0 only, so the XPath version of this
     * method throws an error unconditionally
     *
     * @return the parsed expression; except that this version of the method
     * always throws an exception
     * @throws XPathException if a static error is found
     */

    /*@NotNull*/
    protected Expression parseTryCatchExpression() throws XPathException {
        grumble("try/catch expressions are not allowed in XPath");
        return new ErrorExpression();
    }

    /**
     * Parse a FOR or LET expression:
     * for $x in expr (',' $y in expr)* 'return' expr
     * let $x := expr (', $y := expr)* 'return' expr
     * This version of the method handles the subset of the FLWOR syntax allowed in XPath
     *
     * @return the resulting subexpression
     * @throws XPathException if any error is encountered
     */

    protected Expression parseFLWORExpression() throws XPathException {
        if (t.currentToken == Token.LET && !allowXPath30Syntax) {
            grumble("'let' is not permitted in XPath 2.0");
        }
        if (t.currentToken == Token.FOR_SLIDING || t.currentToken == Token.FOR_TUMBLING) {
            grumble("sliding/tumbling windows can only be used in XQuery");
        }
        int clauses = 0;
        int offset;
        int operator = t.currentToken;
        Assignation first = null;
        Assignation previous = null;
        do {
            offset = t.currentTokenStartOffset;
            nextToken();
            expect(Token.DOLLAR);
            nextToken();
            expect(Token.NAME);
            String var = t.currentTokenValue;

            // declare the range variable
            Assignation v;
            if (operator == Token.FOR) {
                v = new ForExpression();
                v.setRequiredType(SequenceType.SINGLE_ITEM);
            } else /*if (operator == Token.LET)*/ {
                v = new LetExpression();
                v.setRequiredType(SequenceType.ANY_SEQUENCE);
            }

            clauses++;
            setLocation(v, offset);
            v.setVariableQName(makeStructuredQName(var, ""));
            nextToken();

            // process the "in" or ":=" clause
            expect(operator == Token.LET ? Token.ASSIGN : Token.IN);
            nextToken();
            v.setSequence(parseExprSingle());
            declareRangeVariable(v);
            if (previous == null) {
                first = v;
            } else {
                previous.setAction(v);
            }
            previous = v;

        } while (t.currentToken == Token.COMMA);

        // process the "return" expression (called the "action")
        expect(Token.RETURN);
        nextToken();
        previous.setAction(parseExprSingle());

        // undeclare all the range variables

        for (int i = 0; i < clauses; i++) {
            undeclareRangeVariable();
        }
        return makeTracer(offset, first, LocationKind.FOR_EXPRESSION, first.getVariableQName());
    }

    /**
     * Parse a quantified expression:
     * (some|every) $x in expr 'satisfies' expr
     *
     * @return the resulting subexpression
     * @throws XPathException if any error is encountered
     */

    private Expression parseQuantifiedExpression() throws XPathException {
        int clauses = 0;
        int operator = t.currentToken;
        QuantifiedExpression first = null;
        QuantifiedExpression previous = null;
        int initialOffset = t.currentTokenStartOffset;
        do {
            int offset = t.currentTokenStartOffset;
            nextToken();
            expect(Token.DOLLAR);
            nextToken();
            expect(Token.NAME);
            String var = t.currentTokenValue;
            clauses++;

            // declare the range variable
            QuantifiedExpression v = new QuantifiedExpression();
            v.setRequiredType(SequenceType.SINGLE_ITEM);
            v.setOperator(operator);
            setLocation(v, offset);

            v.setVariableQName(makeStructuredQName(var, ""));
            nextToken();

            if (t.currentToken == Token.AS && language == XQUERY) {
                // We use this path for quantified expressions in XQuery, which permit an "as" clause
                nextToken();
                SequenceType type = parseSequenceType();
                if (type.getCardinality() != StaticProperty.EXACTLY_ONE) {
                    warning("Occurrence indicator on singleton range variable has no effect");
                    type = SequenceType.makeSequenceType(type.getPrimaryType(), StaticProperty.EXACTLY_ONE);
                }
                v.setRequiredType(type);
            }

            // process the "in" clause
            expect(Token.IN);
            nextToken();
            v.setSequence(parseExprSingle());
            declareRangeVariable(v);
            if (previous != null) {
                previous.setAction(v);
            } else {
                first = v;
            }
            previous = v;

        } while (t.currentToken == Token.COMMA);

        // process the "return/satisfies" expression (called the "action")
        expect(Token.SATISFIES);
        nextToken();
        previous.setAction(parseExprSingle());


        // undeclare all the range variables

        for (int i = 0; i < clauses; i++) {
            undeclareRangeVariable();
        }
        return makeTracer(initialOffset, first, LocationKind.FOR_EXPRESSION, first.getVariableQName());

    }


    /**
     * Parse an IF expression:
     * if '(' expr ')' 'then' expr 'else' expr
     *
     * @return the resulting subexpression
     * @throws XPathException if any error is encountered
     */

    private Expression parseIfExpression() throws XPathException {
        // left paren already read
        int ifoffset = t.currentTokenStartOffset;
        nextToken();
        Expression condition = parseExpression();
        expect(Token.RPAR);
        nextToken();
        int thenoffset = t.currentTokenStartOffset;
        expect(Token.THEN);
        nextToken();
        Expression thenExp = makeTracer(thenoffset, parseExprSingle(), LocationKind.THEN_EXPRESSION, null);
        int elseoffset = t.currentTokenStartOffset;
        expect(Token.ELSE);
        nextToken();
        Expression elseExp = makeTracer(elseoffset, parseExprSingle(), LocationKind.ELSE_EXPRESSION, null);
        Expression ifExp = Choose.makeConditional(condition, thenExp, elseExp);
        setLocation(ifExp, ifoffset);
        return makeTracer(ifoffset, ifExp, LocationKind.IF_EXPRESSION, null);
    }

    /**
     * Analyze a token whose expected value is the name of an atomic type,
     * or in XPath 3.0 a "plain" union type and return the object representing the atomic or union type.
     *
     * @param qname The lexical QName of the atomic type; alternatively, a Clark name
     * @return The atomic type
     * @throws XPathException if the QName is invalid or if no atomic type of that
     *                        name exists as a built-in type or a type in an imported schema
     */
    /*@NotNull*/
    private ItemType getPlainType(String qname) throws XPathException {
        if (scanOnly) {
            return STRING;
        }
        Configuration config = env.getConfiguration();
        qNameParser.setDefaultNamespace(env.getDefaultElementNamespace());
        StructuredQName sq = null;
        try {
            sq = qNameParser.parse(qname);
        } catch (XPathException e) {
            grumble(e.getMessage(), e.getErrorCodeLocalPart());
        }
        String uri = sq.getURI();
        if (uri.isEmpty()) {
            uri = env.getDefaultElementNamespace();
        }
        String local = sq.getLocalPart();

        boolean builtInNamespace = uri.equals(NamespaceConstant.SCHEMA);

        if (builtInNamespace) {
            ItemType t = Type.getBuiltInItemType(uri, local);
            if (t == null) {
                grumble("Unknown atomic type " + qname, "XPST0051");
            }
            if (t instanceof BuiltInAtomicType) {
                checkAllowedType(env, (BuiltInAtomicType) t);
                return t;
            } else if (t.isPlainType()) {
                return t;
            } else {
                grumble("The type " + qname + " is not atomic", "XPST0051");
            }
        } else if (uri.equals(NamespaceConstant.JAVA_TYPE)) {
            Class theClass;
            try {
                String className = JavaExternalObjectType.localNameToClassName(local);
                theClass = config.getClass(className, false, null);
            } catch (XPathException err) {
                grumble("Unknown Java class " + local, "XPST0051");
                return JavaExternalObjectType.EXTERNAL_OBJECT_TYPE;
            }
            return new JavaExternalObjectType(theClass, config);
        } else if (uri.equals(NamespaceConstant.DOT_NET_TYPE)) {
            return (AtomicType) Version.platform.getExternalObjectType(config, uri, local);
        } else {

            SchemaType st = config.getSchemaType(new StructuredQName("", uri, local));
            if (st == null) {
                grumble("Unknown atomic type " + qname, "XPST0051");
            } else if (st.isAtomicType()) {
                if (!env.isImportedSchema(uri)) {
                    grumble("Atomic type " + qname + " exists, but its schema definition has not been imported", "XPST0051");
                }
                return (AtomicType) st;
            } else if (st instanceof ItemType && ((ItemType) st).isPlainType() && allowXPath30Syntax) {
                if (!env.isImportedSchema(uri)) {
                    grumble("Type " + qname + " exists, but its schema definition has not been imported", "XPST0051");
                }
                return (ItemType) st;
            } else if (st.isComplexType()) {
                grumble("Type (" + qname + ") is a complex type", "XPST0051");
                return ANY_ATOMIC;
            } else if (((SimpleType) st).isListType()) {
                grumble("Type (" + qname + ") is a list type", "XPST0051");
                return ANY_ATOMIC;
            } else if (allowXPath30Syntax) {
                grumble("Type (" + qname + ") is a union type that cannot be used as an item type", "XPST0051");
                return ANY_ATOMIC;
            } else {
                grumble("The union type (" + qname + ") cannot be used as an item type unless XPath 3.0 is enabled", "XPST0051");
                return ANY_ATOMIC;
            }
        }
        grumble("Unknown atomic type " + qname, "XPST0051");
        return ANY_ATOMIC;
    }

    private void checkAllowedType(StaticContext env, BuiltInAtomicType type) throws XPathException {
        String s = whyDisallowedType(env.getPackageData(), type);
        if (s != null) {
            grumble(s, "XPST0080");
        }
    }

    /**
     * Determine whether a given built-in type is disallowed in a given environment, and in the
     * case where it is disallowed, return a string explaining why
     *
     * @param pack the containing package
     * @param type the built-in type to be tested
     * @return null if the type is OK to be used; or a string explaining why not.
     */

    public static String whyDisallowedType(PackageData pack, BuiltInAtomicType type) {
        if (pack instanceof StylesheetPackage &&
                !type.isAllowedInBasicXSLT20() &&
                pack.getXPathVersion() < 30 &&
                !pack.isSchemaAware() &&
                !pack.getConfiguration().isLicensedFeature(Configuration.LicenseFeature.PROFESSIONAL_EDITION)) {
            return "The built-in atomic type " + type.getDisplayName() + " is not recognized in a Basic XSLT 2.0 processor";
        }
        if (!type.isAllowedInXSD10() && pack.getConfiguration().getXsdVersion() == Configuration.XSD10) {
            return "The built-in atomic type " + type.getDisplayName() + " is not recognized unless XSD 1.1 is enabled";
        }
        return null;
    }


    /**
     * Analyze a token whose expected value is the name of a simple type: any type name
     * allowed as the operand of "cast" or "castable".
     *
     * @param qname The lexical QName of the atomic type; alternatively, a Clark name
     * @return The atomic type
     * @throws XPathException if the QName is invalid or if no atomic type of that
     *                        name exists as a built-in type or a type in an imported schema
     */
    /*@NotNull*/
    private SimpleType getSimpleType(/*@NotNull*/ String qname) throws XPathException {
        if (scanOnly) {
            return STRING;
        }
        qNameParser.setDefaultNamespace(env.getDefaultElementNamespace());
        StructuredQName sq = null;
        try {
            sq = qNameParser.parse(qname);
        } catch (XPathException e) {
            grumble(e.getMessage(), e.getErrorCodeLocalPart());
        }
        String uri = sq.getURI();
        String local = sq.getLocalPart();

        boolean builtInNamespace = uri.equals(NamespaceConstant.SCHEMA);
        if (builtInNamespace) {
            SimpleType t = Type.getBuiltInSimpleType(uri, local);
            if (t == null) {
                if (allowXPath30Syntax) {
                    grumble("Unknown simple type " + qname, "XQST0052");
                } else {
                    grumble("Unknown simple type " + qname, "XPST0051");
                }
            }
            if (t instanceof BuiltInAtomicType) {
                checkAllowedType(env, (BuiltInAtomicType) t);
            }
            return t;
        } else if (uri.equals(NamespaceConstant.DOT_NET_TYPE)) {
            return (AtomicType) Version.platform.getExternalObjectType(env.getConfiguration(), uri, local);

        } else {

            SchemaType st = env.getConfiguration().getSchemaType(new StructuredQName("", uri, local));
            if (st == null) {
                if (allowXPath30Syntax) {
                    grumble("Unknown simple type " + qname, "XQST0052");
                } else {
                    grumble("Unknown simple type " + qname, "XPST0051");
                }
                return ANY_ATOMIC;
            }
            if (allowXPath30Syntax) {
                // XPath 3.0
                if (!env.isImportedSchema(uri)) {
                    grumble("Simple type " + qname + " exists, but its target namespace has not been imported in the static context");
                }
                return (SimpleType) st;

            } else {
                // XPath 2.0
                if (st.isAtomicType()) {
                    if (!env.isImportedSchema(uri)) {
                        grumble("Atomic type " + qname + " exists, but its target namespace has not been imported in the static context");
                    }
                    return (AtomicType) st;
                } else if (st.isComplexType()) {
                    grumble("Cannot cast to a complex type (" + qname + ")", "XPST0051");
                    return ANY_ATOMIC;
                } else if (((SimpleType) st).isListType()) {
                    grumble("Casting to a list type (" + qname + ") requires XPath 3.0", "XPST0051");
                    return ANY_ATOMIC;
                } else {
                    grumble("casting to a union type (" + qname + ") requires XPath 3.0", "XPST0051");
                    return ANY_ATOMIC;
                }
            }
        }
    }

    /**
     * Parse the sequence type production.
     * The QName must be the name of a built-in schema-defined data type.
     *
     * @return the resulting subexpression
     * @throws XPathException if any error is encountered
     */

    public SequenceType parseSequenceType() throws XPathException {
        boolean disallowIndicator = t.currentTokenValue.equals("empty-sequence");
        ItemType primaryType = parseItemType();
        if (disallowIndicator) {
            // No occurrence indicator allowed
            return SequenceType.makeSequenceType(primaryType, StaticProperty.EMPTY);
        }
        int occurrenceFlag;
        switch (t.currentToken) {
            case Token.STAR:
            case Token.MULT:
                // "*" will be tokenized different ways depending on what precedes it
                occurrenceFlag = StaticProperty.ALLOWS_ZERO_OR_MORE;
                // Make the tokenizer ignore the occurrence indicator when classifying the next token
                t.currentToken = Token.RPAR;
                nextToken();
                break;
            case Token.PLUS:
                occurrenceFlag = StaticProperty.ALLOWS_ONE_OR_MORE;
                // Make the tokenizer ignore the occurrence indicator when classifying the next token
                t.currentToken = Token.RPAR;
                nextToken();
                break;
            case Token.QMARK:
                occurrenceFlag = StaticProperty.ALLOWS_ZERO_OR_ONE;
                // Make the tokenizer ignore the occurrence indicator when classifying the next token
                t.currentToken = Token.RPAR;
                nextToken();
                break;
            default:
                occurrenceFlag = StaticProperty.EXACTLY_ONE;
        }
        return SequenceType.makeSequenceType(primaryType, occurrenceFlag);
    }

    /**
     * Parse an ItemType within a SequenceType
     *
     * @return the ItemType after parsing
     * @throws XPathException if a static error is found
     */

    /*@NotNull*/
    public ItemType parseItemType() throws XPathException {
        ItemType primaryType;
        if (t.currentToken == Token.LPAR) {
            primaryType = parseParenthesizedItemType();
            //nextToken();
        } else if (t.currentToken == Token.NAME) {
            primaryType = getPlainType(t.currentTokenValue);
            nextToken();
        } else if (t.currentToken == Token.NODEKIND) {
            if (t.currentTokenValue.equals("item")) {
                nextToken();
                expect(Token.RPAR);
                nextToken();
                primaryType = AnyItemType.getInstance();
            } else if (t.currentTokenValue.equals("function")) {
                primaryType = parseFunctionItemType();
            } else if (t.currentTokenValue.equals("map")) {
                primaryType = parseMapItemType();
            } else if (t.currentTokenValue.equals("array")) {
                primaryType = parseArrayItemType();
            } else if (t.currentTokenValue.equals("empty-sequence")) {
                nextToken();
                expect(Token.RPAR);
                nextToken();
                primaryType = ErrorType.getInstance();
            } else {
                primaryType = parseKindTest();
            }
        } else if (t.currentToken == Token.PERCENT) {
            /*Map<StructuredQName, Annotation> annotationAssertions =*/
            checkAnnotations(parseAnnotationsList(), true);
            // TODO retain the annotation assertions
            if (t.currentTokenValue.equals("function")) {
                primaryType = parseFunctionItemType();
            } else {
                grumble("Expected 'function' to follow annotation assertions, found " + Token.tokens[t.currentToken]);
                return null;
            }
        } else if (language == EXTENDED_ITEM_TYPE && t.currentToken == Token.PREFIX) {
            String tokv = t.currentTokenValue;
            nextToken();
            return makeNamespaceTest(Type.ELEMENT, tokv);
        } else if (language == EXTENDED_ITEM_TYPE && t.currentToken == Token.SUFFIX) {
            nextToken();
            expect(Token.NAME);
            String tokv = t.currentTokenValue;
            nextToken();
            return makeLocalNameTest(Type.ELEMENT, tokv);
        } else if (language == EXTENDED_ITEM_TYPE && t.currentToken == Token.AT) {
            nextToken();
            if (t.currentToken == Token.PREFIX) {
                String tokv = t.currentTokenValue;
                nextToken();
                return makeNamespaceTest(Type.ATTRIBUTE, tokv);
            } else if (t.currentToken == Token.SUFFIX) {
                nextToken();
                expect(Token.NAME);
                String tokv = t.currentTokenValue;
                nextToken();
                return makeLocalNameTest(Type.ATTRIBUTE, tokv);
            } else {
                grumble("Expected NodeTest after '@'");
                return ANY_ATOMIC;
            }
        } else {
            grumble("Expected type name in SequenceType, found " + Token.tokens[t.currentToken]);
            return ANY_ATOMIC;
        }
        return primaryType;
    }

    /**
     * Get the item type used for function items (XPath 3.0 higher order functions)
     *
     * @return the item type representing a function item
     * @throws net.sf.saxon.trans.XPathException if a static error occurs (including the case
     *                                           where XPath 3.0 syntax is not enabled)
     */

    /*@NotNull*/
    protected ItemType parseFunctionItemType() throws XPathException {
        grumble("The item type function() is available only when XPath 3.0 is enabled");
        return ANY_ATOMIC;
    }

    /**
     * Parse the item type used for maps (XSLT extension to XPath 3.0)
     * Syntax:
     * map '(' '*' ') |
     * map '(' ItemType ',' SeqType ')' 'as' SeqType
     * The "map(" has already been read
     * @return the item type of the map
     * @throws XPathException if a parsing error occurs or if the map syntax
     *                        is not available
     */

    /*@NotNull*/
    protected ItemType parseMapItemType() throws XPathException {
        checkLanguageVersion31();
        Tokenizer t = getTokenizer();
        nextToken();
        if (t.currentToken == Token.STAR || t.currentToken == Token.MULT) {
            // Allow both to be safe
            nextToken();
            expect(Token.RPAR);
            nextToken();
            return MapType.ANY_MAP_TYPE;
        } else {
            ItemType keyType = parseItemType();
            expect(Token.COMMA);
            nextToken();
            SequenceType valueType = parseSequenceType();
            expect(Token.RPAR);
            nextToken();
            if (!(keyType instanceof AtomicType)) {
                grumble("Key type of a map must be atomic");
                return null;
            }
            return new MapType((AtomicType) keyType, valueType);
        }
    }

    /**
     * Get the item type used for array items (XPath 3.1)
     * Syntax:
     *    array '(' '*' ') |
     *    array '(' SeqType ')'
     * The "array(" has already been read
     * @return the item type of the array
     * @throws XPathException if a parsing error occurs or if the array syntax
     *                        is not available
     */

    /*@NotNull*/
    protected ItemType parseArrayItemType() throws XPathException {
        checkLanguageVersion31();
        Tokenizer t = getTokenizer();
        nextToken();
        if (t.currentToken == Token.STAR || t.currentToken == Token.MULT) {
            // Allow both to be safe
            nextToken();
            expect(Token.RPAR);
            nextToken();
            return ArrayItemType.ANY_ARRAY_TYPE;
        } else {
            SequenceType memberType = parseSequenceType();
            expect(Token.RPAR);
            nextToken();
            return new ArrayItemType(memberType);
        }
    }

    /**
     * Parse a parenthesized item type (allowed in XQuery 3.0 and XPath 3.0 only)
     *
     * @return the item type
     * @throws XPathException in the event of a syntax error (or if 3.0 is not enabled)
     */

    /*@NotNull*/
    private ItemType parseParenthesizedItemType() throws XPathException {
        if (!allowXPath30Syntax) {
            grumble("Parenthesized item types require 3.0 to be enabled");
        }
        nextToken();
        ItemType primaryType = parseItemType();
        while (primaryType instanceof NodeTest && language == EXTENDED_ITEM_TYPE && t.currentToken != Token.RPAR) {
            switch (t.currentToken) {
                case Token.UNION:
                case Token.EXCEPT:
                case Token.INTERSECT:
                    int op = t.currentToken;
                    nextToken();
                    primaryType = new CombinedNodeTest((NodeTest) primaryType, op, (NodeTest) parseItemType());
            }
        }
        expect(Token.RPAR);
        nextToken();
        return primaryType;
    }


    /**
     * Parse a UnaryExpr:<br>
     * ('+'|'-')* ValueExpr
     * parsed as ('+'|'-')? UnaryExpr
     *
     * @return the resulting subexpression
     * @throws XPathException if any error is encountered
     */

    /*@NotNull*/
    private Expression parseUnaryExpression() throws XPathException {
        Expression exp;
        switch (t.currentToken) {
            case Token.MINUS:
                nextToken();
                exp = new ArithmeticExpression(Literal.makeLiteral(Int64Value.ZERO),
                        Token.NEGATE,
                        parseUnaryExpression());
                break;
            case Token.PLUS:
                nextToken();
                // Unary plus: can't ignore it completely, it might be a type error, or it might
                // force conversion to a number which would affect operations such as "=".
                exp = new ArithmeticExpression(Literal.makeLiteral(Int64Value.ZERO),
                        Token.PLUS,
                        parseUnaryExpression());
                break;
            case Token.VALIDATE:
            case Token.VALIDATE_STRICT:
            case Token.VALIDATE_LAX:
            case Token.VALIDATE_TYPE:
                exp = parseValidateExpression();
                break;
            case Token.PRAGMA:
                exp = parseExtensionExpression();
                break;

            case Token.KEYWORD_CURLY:
                if (t.currentTokenValue.equals("validate")) {
                    exp = parseValidateExpression();
                    break;
                }
                // else fall through
            default:
                exp = parseSimpleMappingExpression();
        }
        setLocation(exp);
        return exp;
    }

    /**
     * Test whether the current token is one that can start a RelativePathExpression
     *
     * @return the resulting subexpression
     */

    protected boolean atStartOfRelativePath() {
        switch (t.currentToken) {
            case Token.AXIS:
            case Token.AT:
            case Token.NAME:
            case Token.PREFIX:
            case Token.SUFFIX:
            case Token.STAR:
            case Token.NODEKIND:
            case Token.DOT:
            case Token.DOTDOT:
            case Token.FUNCTION:
            case Token.STRING_LITERAL:
            case Token.NUMBER:
            case Token.LPAR:
            case Token.DOLLAR:
            case Token.PRAGMA:
            case Token.ELEMENT_QNAME:
            case Token.ATTRIBUTE_QNAME:
            case Token.PI_QNAME:
            case Token.NAMESPACE_QNAME:
            case Token.NAMED_FUNCTION_REF:
                return true;
            case Token.KEYWORD_CURLY:
                return t.currentTokenValue.equals("ordered") || t.currentTokenValue.equals("unordered");
            default:
                return false;
        }
    }

    /**
     * Test whether the current token is one that is disallowed after a "leading lone slash".
     * These composite tokens have been parsed as operators, but are not allowed after "/" under the
     * rules of erratum E24
     *
     * @return the resulting subexpression
     */

    protected boolean disallowedAtStartOfRelativePath() {
        switch (t.currentToken) {
            // Although these "double keyword" operators can readily be recognized as operators,
            // they are not permitted after leading "/" under the rules of erratum XQ.E24
            case Token.CAST_AS:
            case Token.CASTABLE_AS:
            case Token.INSTANCE_OF:
            case Token.TREAT_AS:
                return true;
            default:
                return false;
        }
    }

    /**
     * Parse a PathExpresssion. This includes "true" path expressions such as A/B/C, and also
     * constructs that may start a path expression such as a variable reference $name or a
     * parenthesed expression (A|B). Numeric and string literals also come under this heading.
     *
     * @return the resulting subexpression
     * @throws XPathException if any error is encountered
     */

    /*@NotNull*/
    protected Expression parsePathExpression() throws XPathException {
        int offset = t.currentTokenStartOffset;
        switch (t.currentToken) {
            case Token.SLASH:
                nextToken();
                final RootExpression start = new RootExpression();
                setLocation(start);
                if (disallowedAtStartOfRelativePath()) {
                    grumble("Operator '" + Token.tokens[t.currentToken] + "' is not allowed after '/'");
                }
                if (atStartOfRelativePath()) {
                    final Expression path = parseRemainingPath(start);
                    setLocation(path, offset);
                    return path;
                } else {
                    return start;
                }

            case Token.SLASH_SLASH:
                nextToken();
                final RootExpression start2 = new RootExpression();
                setLocation(start2, offset);
                final AxisExpression axisExp = new AxisExpression(AxisInfo.DESCENDANT_OR_SELF, null);
                setLocation(axisExp, offset);
                final Expression slashExp = ExpressionTool.makePathExpression(start2, axisExp, false);
                setLocation(slashExp, offset);
                final Expression exp = parseRemainingPath(slashExp);
                setLocation(exp, offset);
                return exp;

            default:
                if (t.currentToken == Token.NAME &&
                        (t.currentTokenValue.equals("true") || t.currentTokenValue.equals("false"))) {
                    warning("The expression is looking for a child element named '" + t.currentTokenValue +
                            "' - perhaps " + t.currentTokenValue + "() was intended? To avoid this warning, use child::" +
                            t.currentTokenValue + " or ./" + t.currentTokenValue);
                }
                return parseRelativePath();
        }

    }

    /**
     * Parse an XPath 3.0 simple mapping expression ("!" operator)
     *
     * @return the parsed expression
     * @throws XPathException in the event of a syntax error
     */

    protected Expression parseSimpleMappingExpression() throws XPathException {
        int offset = t.currentTokenStartOffset;
        Expression exp = parsePathExpression();
        while (t.currentToken == Token.BANG) {
            if (!allowXPath30Syntax) {
                grumble("XPath '!' operator requires XPath 3.0 to be enabled");
            }
            nextToken();
            Expression next = parsePathExpression();
            exp = new ForEach(exp, next);
            setLocation(exp, offset);
        }
        return exp;
    }

    /**
     * Parse a relative path (a sequence of steps). Called when the current token immediately
     * follows a separator (/ or //), or an implicit separator (XYZ is equivalent to ./XYZ)
     *
     * @return the resulting subexpression
     * @throws XPathException if any error is encountered
     */

    /*@NotNull*/
    protected Expression parseRelativePath() throws XPathException {
        int offset = t.currentTokenStartOffset;
        Expression exp = parseStepExpression(language == XSLT_PATTERN);
        while (t.currentToken == Token.SLASH ||
                t.currentToken == Token.SLASH_SLASH) {
            int op = t.currentToken;
            nextToken();
            Expression next = parseStepExpression(false);
            if (op == Token.SLASH) {
                exp = ExpressionTool.makePathExpression(exp, next, true);
            } else if (op == Token.SLASH_SLASH) {
                // add implicit descendant-or-self::node() step
                AxisExpression ae = new AxisExpression(AxisInfo.DESCENDANT_OR_SELF, null);
                setLocation(ae, offset);
                Expression one = ExpressionTool.makePathExpression(exp, ae, false);
                setLocation(one, offset);
                exp = ExpressionTool.makePathExpression(one, next, true);
            }
            setLocation(exp, offset);
        }
        return exp;
    }

    /**
     * Parse the remaining steps of an absolute path expression (one starting in "/" or "//"). Note that the
     * token immediately after the "/" or "//" has already been read, and in the case of "/", it has been confirmed
     * that we have a path expression starting with "/" rather than a standalone "/" expression.
     *
     * @param start the initial implicit expression: root() in the case of "/", root()/descendant-or-self::node in
     *              the case of "//"
     * @return the completed path expression
     * @throws XPathException if a static error is found
     */
    /*@NotNull*/
    protected Expression parseRemainingPath(Expression start) throws XPathException {
        int offset = t.currentTokenStartOffset;
        Expression exp = start;
        int op = Token.SLASH;
        while (true) {
            Expression next = parseStepExpression(false);
            if (op == Token.SLASH) {
                exp = ExpressionTool.makePathExpression(exp, next, true);
            } else if (op == Token.SLASH_SLASH) {
                // add implicit descendant-or-self::node() step
                AxisExpression descOrSelf = new AxisExpression(AxisInfo.DESCENDANT_OR_SELF, null);
                setLocation(descOrSelf);
                Expression step = ExpressionTool.makePathExpression(descOrSelf, next, false);
                setLocation(step);
                exp = ExpressionTool.makePathExpression(exp, step, true);
            } else /*if (op == Token.BANG)*/ {
                if (!allowXPath30Syntax) {
                    grumble("XPath '!' operator requires XPath 3.0 to be enabled");
                }
                exp = new ForEach(exp, next);
            }
            setLocation(exp, offset);
            op = t.currentToken;
            if (op != Token.SLASH && op != Token.SLASH_SLASH && op != Token.BANG) {
                break;
            }
            nextToken();
        }
        return exp;
    }


    /**
     * Parse a step (including an optional sequence of predicates)
     *
     * @param firstInPattern true only if we are parsing the first step in a
     *                       RelativePathPattern in the XSLT Pattern syntax
     * @return the resulting subexpression
     * @throws XPathException if any error is encountered
     */

    /*@NotNull*/
    protected Expression parseStepExpression(boolean firstInPattern) throws XPathException {
        Expression step = parseBasicStep(firstInPattern);

        // When the filter is applied to an Axis step, the nodes are considered in
        // axis order. In all other cases they are considered in document order
        boolean reverse = (step instanceof AxisExpression) &&
                !AxisInfo.isForwards[((AxisExpression) step).getAxis()];

        while (true) {
            if (t.currentToken == Token.LSQB) {
                nextToken();
                Expression predicate = parsePredicate();
                expect(Token.RSQB);
                nextToken();
                step = new FilterExpression(step, predicate);
                setLocation(step);
            } else if (t.currentToken == Token.LPAR) {
                // dynamic function call (XQuery 3.0/XPath 3.0 syntax)
                step = parseDynamicFunctionCall(step, null);
                setLocation(step);
            } else if (t.currentToken == Token.QMARK) {
                step = parseLookup(step);
                setLocation(step);
            } else {
                break;
            }
        }
        if (reverse) { // TODO this should only apply to filter expressions
            RetainedStaticContext rsc = new RetainedStaticContext(env);
            step = SystemFunction.makeCall("reverse", rsc, step);
            assert step != null;
            return step;
        } else {
            return step;
        }
    }

    /**
     * Parse an XPath 3.1 arrow operator ("=>")
     *
     * @return the expression that results from the parsing
     */

    /*@NotNull*/
    protected Expression parseArrowPostfix(Expression lhs) throws XPathException {
        checkLanguageVersion31();
        nextToken();
        int token = getTokenizer().currentToken;
        if (token == Token.NAME || token == Token.FUNCTION) {
            return parseFunctionCall(lhs);
        } else if (token == Token.DOLLAR) {
            Expression var = parseVariableReference();
            expect(Token.LPAR);
            return parseDynamicFunctionCall(var, lhs);
        } else if (token == Token.LPAR) {
            Expression var = parseParenthesizedExpression();
            expect(Token.LPAR);
            return parseDynamicFunctionCall(var, lhs);
        } else {
            grumble("Unexpected " + Token.tokens[token] + " after '=>'");
            return null;
        }
    }

    /**
     * Parse the expression within a predicate. A separate method so it can be overridden
     *
     * @return the expression within the predicate
     * @throws XPathException if a static error is found
     */

    /*@NotNull*/
    protected Expression parsePredicate() throws XPathException {
        return parseExpression();
    }

    protected boolean isReservedInQuery(String uri) {
        if (allowXPath31Syntax) {
            return NamespaceConstant.isReservedInQuery31(uri);
        } else if (allowXPath30Syntax) {
            return NamespaceConstant.isReservedInQuery30(uri);
        } else {
            return NamespaceConstant.isReservedInQuery(uri);
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
        switch (t.currentToken) {
            case Token.DOLLAR:
                return parseVariableReference();

            case Token.LPAR:
                return parseParenthesizedExpression();

            case Token.LSQB:
                return parseArraySquareConstructor();

            case Token.STRING_LITERAL:
                return parseStringLiteral(true);

            case Token.STRING_TEMPLATE_COMPLETE:
                return parseStringTemplate(true);

            case Token.STRING_TEMPLATE_INITIAL:
                return parseStringTemplate(false);

            case Token.NUMBER:
                return parseNumericLiteral(true);

            case Token.FUNCTION:
                return parseFunctionCall(null);

            case Token.QMARK:
                return parseLookup(new ContextItemExpression());

            case Token.DOT:
                nextToken();
                Expression cie = new ContextItemExpression();
                setLocation(cie);
                return cie;

            case Token.DOTDOT:
                nextToken();
                Expression pne = new AxisExpression(AxisInfo.PARENT, null);
                setLocation(pne);
                return pne;

            case Token.PERCENT: {
                Map<StructuredQName, Annotation> annotations = checkAnnotations(parseAnnotationsList(), true);
                if (!t.currentTokenValue.equals("function")) {
                    grumble("Expected 'function' to follow the annotation assertion");
                }
                if (annotations.containsKey(Annotation.PRIVATE) ||
                        annotations.containsKey(Annotation.PUBLIC)) {
                    grumble("Inline functions must not be annotated %private or %public", "XQST0125");
                }
                return parseInlineFunction(annotations);
            }
            case Token.NODEKIND:
                if (t.currentTokenValue.equals("function")) {
                    Map<StructuredQName, Annotation> annotations = new HashMap<StructuredQName, Annotation>();
                    return parseInlineFunction(annotations);
//                } else if (t.currentTokenValue.equals("map")) {
//                    return parseFunctionCall(null);
                }
                // fall through!
            case Token.NAME:
            case Token.PREFIX:
            case Token.SUFFIX:
            case Token.STAR:
                byte defaultAxis = AxisInfo.CHILD;
                if (t.currentToken == Token.NODEKIND &&
                        (t.currentTokenValue.equals("attribute") || t.currentTokenValue.equals("schema-attribute"))) {
                    defaultAxis = AxisInfo.ATTRIBUTE;
                } else if (t.currentToken == Token.NODEKIND && t.currentTokenValue.equals("namespace-node")) {
                    defaultAxis = AxisInfo.NAMESPACE;
                    testPermittedAxis(AxisInfo.NAMESPACE, "XQST0134");
                } else if (firstInPattern && t.currentToken == Token.NODEKIND && t.currentTokenValue.equals("document-node")) {
                    defaultAxis = AxisInfo.SELF;
                }
                NodeTest test = parseNodeTest(Type.ELEMENT);
                if (test instanceof AnyNodeTest) {
                    // handles patterns of the form match="node()"
                    test = defaultAxis == AxisInfo.CHILD ? AnyChildNodeTest.getInstance() : NodeKindTest.ATTRIBUTE;
                }
                AxisExpression ae = new AxisExpression(defaultAxis, test);
                setLocation(ae);
                return ae;

            case Token.AT:
                nextToken();
                switch (t.currentToken) {

                    case Token.NAME:
                    case Token.PREFIX:
                    case Token.SUFFIX:
                    case Token.STAR:
                    case Token.NODEKIND:
                        AxisExpression ae2 = new AxisExpression(AxisInfo.ATTRIBUTE, parseNodeTest(Type.ATTRIBUTE));
                        setLocation(ae2);
                        return ae2;

                    default:
                        grumble("@ must be followed by a NodeTest");
                }
                break;

            case Token.AXIS:
                byte axis;
                try {
                    axis = AxisInfo.getAxisNumber(t.currentTokenValue);
                } catch (XPathException err) {
                    grumble(err.getMessage());
                    axis = AxisInfo.CHILD; // error recovery
                }
                testPermittedAxis(axis, "XPST0003");
                short principalNodeType = AxisInfo.principalNodeType[axis];
                nextToken();
                switch (t.currentToken) {

                    case Token.NAME:
                    case Token.PREFIX:
                    case Token.SUFFIX:
                    case Token.STAR:
                    case Token.NODEKIND:
                        Expression ax = new AxisExpression(axis, parseNodeTest(principalNodeType));
                        setLocation(ax);
                        return ax;

                    default:
                        grumble("Unexpected token " + currentTokenDisplay() + " after axis name");
                }
                break;

            case Token.KEYWORD_CURLY:
                if (t.currentTokenValue.equals("map")) {
                    return parseMapExpression();
                } else if (t.currentTokenValue.equals("array")) {
                    return parseArrayCurlyConstructor();
                }
                // else fall through
            case Token.ELEMENT_QNAME:
            case Token.ATTRIBUTE_QNAME:
            case Token.NAMESPACE_QNAME:
            case Token.PI_QNAME:
            case Token.TAG:
                return parseConstructor();

            case Token.NAMED_FUNCTION_REF:
                return parseNamedFunctionReference();

            default:
                grumble("Unexpected token " + currentTokenDisplay() + " at start of expression");
                //break;
        }
        return new ErrorExpression();
    }

    public Expression parseParenthesizedExpression() throws XPathException {
        nextToken();
        if (t.currentToken == Token.RPAR) {
            nextToken();
            return Literal.makeEmptySequence();
        }
        Expression seq = parseExpression();
        expect(Token.RPAR);
        nextToken();
        return seq;
    }

    protected void testPermittedAxis(byte axis, String errorCode) throws XPathException {
        // no action by default - all axes are permitted
    }


    public Expression parseNumericLiteral(boolean traceable) throws XPathException {
        int offset = t.currentTokenStartOffset;
        NumericValue number = NumericValue.parseNumber(t.currentTokenValue);
        if (number.isNaN()) {
            grumble("Invalid numeric literal " + Err.wrap(t.currentTokenValue, Err.VALUE));
        }
        nextToken();
        Literal lit = Literal.makeLiteral(number);
        setLocation(lit, offset);
        return traceable ? makeTracer(offset, lit, 0, null) : lit;
    }

    protected Expression parseStringLiteral(boolean traceable) throws XPathException {
        int offset = t.currentTokenStartOffset;
        Literal literal = makeStringLiteral(t.currentTokenValue);
        nextToken();
        return traceable ? makeTracer(offset, literal, 0, null) : literal;
    }

    protected Expression parseStringTemplate(boolean complete) throws XPathException {
        throw new XPathException("String constructor expressions are allowed only in XQuery");
    }

    /*@NotNull*/
    public Expression parseVariableReference() throws XPathException {
        int offset = t.currentTokenStartOffset;
        nextToken();
        expect(Token.NAME);
        String var = t.currentTokenValue;
        nextToken();

        if (scanOnly) {
            return new ContextItemExpression();
            // don't do any semantic checks during a prescan
        }

        //int vtest = makeNameCode(var, false) & 0xfffff;
        StructuredQName vtest = makeStructuredQName(var, "");

        // See if it's a range variable or a variable in the context
        LocalBinding b = findRangeVariable(vtest);
        Expression ref;
        if (b != null) {
            ref = new LocalVariableReference(b);
        } else {
            if (catchDepth > 0) {
                for (StructuredQName errorVariable : StandardNames.errorVariables) {
                    if (errorVariable.getLocalPart().equals(vtest.getLocalPart())) {
                        IntegratedFunctionLibrary lib = env.getConfiguration().getVendorFunctionLibrary();
                        StructuredQName functionName =
                                new StructuredQName("saxon", NamespaceConstant.SAXON, "dynamic-error-info");
                        SymbolicName sn = new SymbolicName(StandardNames.XSL_FUNCTION, functionName, 1);
                        Expression[] args = new Expression[]{new StringLiteral(vtest.getLocalPart())};
                        return lib.bind(sn, args, env);
                    }
                }
            }
            try {
                ref = env.bindVariable(vtest);
            } catch (XPathException err) {
                err.maybeSetLocation(makeLocation());
                throw err;
            }
        }
        setLocation(ref, offset);
        return ref;
    }

    /**
     * Method to make a string literal from a token identified as a string
     * literal. This is trivial in XPath, but in XQuery the method is overridden
     * to identify pseudo-XML character and entity references. Note that the job of handling
     * doubled string delimiters is done by the tokenizer.
     *
     * @param currentTokenValue the token as read (excluding quotation marks)
     * @return The string value of the string literal
     * @throws net.sf.saxon.trans.XPathException if a static error is found
     */

    /*@NotNull*/
    protected Literal makeStringLiteral(String currentTokenValue) throws XPathException {
        StringLiteral literal = new StringLiteral(currentTokenValue);
        setLocation(literal);
        return literal;
    }

    /**
     * Unescape character references and built-in entity references in a string. Does nothing
     * in XPath, because XPath does not recognize entity references in string literals
     *
     * @param token the input string, which may include XML-style character references or built-in
     *              entity references
     * @return the string with character references and built-in entity references replaced by their expansion
     * @throws XPathException if a malformed character or entity reference is found
     */

    /*@NotNull*/
    protected CharSequence unescape(String token) throws XPathException {
        return token;
    }


    /**
     * Parse a node constructor. This is allowed only in XQuery, so the method throws
     * an error for XPath.
     *
     * @return the expression that results from the parsing
     * @throws net.sf.saxon.trans.XPathException if a static error occurs
     */

    /*@NotNull*/
    protected Expression parseConstructor() throws XPathException {
        grumble("Node constructor expressions are allowed only in XQuery, not in XPath");
        return new ErrorExpression();
    }

    /**
     * Parse a dynamic function call
     *
     * @param functionItem the expression that determines the function to be called
     * @param prefixArgument the LHS of an arrow operator, or null if this is not part of an arrow expression
     * @return the expression that results from the parsing
     * @throws net.sf.saxon.trans.XPathException if a static error is found
     */

    /*@NotNull*/
    public Expression parseDynamicFunctionCall(Expression functionItem, Expression prefixArgument) throws XPathException {
        checkLanguageVersion30();

        ArrayList<Expression> args = new ArrayList<Expression>(10);
        if (prefixArgument != null) {
            args.add(prefixArgument);
        }
        IntSet placeMarkers = null;

        // the "(" has already been read by the Tokenizer: now parse the arguments
        nextToken();
        if (t.currentToken != Token.RPAR) {
            while (true) {
                Expression arg = parseFunctionArgument();
                if (arg == null) {
                    // this is a "?" placemarker
                    if (placeMarkers == null) {
                        placeMarkers = new IntArraySet();
                    }
                    placeMarkers.add(args.size());
                    arg = Literal.makeEmptySequence(); // a convenient fiction
                    checkHofFeature();
                }
                args.add(arg);
                if (t.currentToken == Token.COMMA) {
                    nextToken();
                } else {
                    break;
                }
            }
            expect(Token.RPAR);
        }
        nextToken();

        if (placeMarkers == null) {
            return generateApplyCall(functionItem, args);
        } else {
            return createDynamicCurriedFunction(functionItem, args, placeMarkers);
        }
        /*grumble("Unexpected '(' after primary expression. (Dynamic function calls require XPath 3.0)");
        return new ErrorExpression();*/
    }

    protected Expression createDynamicCurriedFunction(Expression functionItem, ArrayList<Expression> args, IntSet placeMarkers) throws XPathException {
        grumble("Partial function application not allowed in this Configuration");
        return null;
    }

    protected Expression generateApplyCall(Expression functionItem, ArrayList<Expression> args) throws XPathException {
        ArrayBlock block = new ArrayBlock(args);
        RetainedStaticContext rsc = new RetainedStaticContext(getStaticContext());
        SystemFunctionCall call = (SystemFunctionCall) SystemFunction.makeCall("_APPLY", rsc, functionItem, block);
        ((ApplyFn) call.getTargetFunction()).setDynamicFunctionCall(functionItem.toShortString());
        setLocation(call, t.currentTokenStartOffset);
        return call;
    }

    /**
     * Parse a lookup operator ("?")
     *
     * @param lhs the expression that the function to be called
     * @return the expression that results from the parsing
     * @throws net.sf.saxon.trans.XPathException if a static error is found
     */

    /*@NotNull*/
    protected Expression parseLookup(Expression lhs) throws XPathException {
        checkLanguageVersion31();
        Tokenizer t = getTokenizer();
        int offset = t.currentTokenStartOffset;
        t.setState(Tokenizer.BARE_NAME_STATE); // Prevent mis-recognition of x?f(2)
        t.currentToken = Token.LPAR;  // Hack to force following symbol to be recognised in post-operator mode
        nextToken();
        int token = t.currentToken;
        t.setState(Tokenizer.OPERATOR_STATE);

        Expression result;
        if (token == Token.NAME) {
            String name = t.currentTokenValue;
            if (!NameChecker.isValidNCName(name)) {
                grumble("The name following '?' must be a valid NCName");
            }
            nextToken();
            result = lookupName(lhs, StringLiteral.makeLiteral(new StringValue(name)));
        } else if (token == Token.NUMBER) {
            NumericValue number = NumericValue.parseNumber(t.currentTokenValue);
            if (!(number instanceof IntegerValue)) {
                grumble("Number following '?' must be an integer");
            }
            nextToken();
            result = lookup(lhs, Literal.makeLiteral(number));
        } else if (token == Token.MULT || token == Token.STAR) {
            nextToken();
            result = lookupStar(lhs);
        } else if (token == Token.LPAR) {
            result = lookup(lhs, parseParenthesizedExpression());
        } else {
            grumble("Unexpected " + Token.tokens[token] + " after '?'");
            return null;
        }
        setLocation(result, offset);
        return result;
    }

    /**
     * Supporting code for lookup expressions (A?B)
     *
     * @param lhs the LHS operand of the lookup expression
     * @param rhs the RHS operand of the lookup expression
     * @return the result of parsing the expression
     */

    private static Expression lookup(Expression lhs, Expression rhs) {
        ForExpression lhsVar = new ForExpression();
        lhsVar.setVariableQName(new StructuredQName("qq", NamespaceConstant.SAXON, "qq" + lhsVar.hashCode()));
        lhsVar.setSequence(lhs);
        lhsVar.setRequiredType(SequenceType.SINGLE_FUNCTION);
        ForExpression rhsVar = new ForExpression();
        rhsVar.setVariableQName(new StructuredQName("qq", NamespaceConstant.SAXON, "qq" + rhsVar.hashCode()));
        rhsVar.setSequence(new Atomizer(rhs));
        rhsVar.setRequiredType(SequenceType.SINGLE_ATOMIC);

        Expression condition1 = new InstanceOfExpression(new LocalVariableReference(lhsVar), HashTrieMap.SINGLE_MAP_TYPE);
        Expression condition2 = new InstanceOfExpression(new LocalVariableReference(lhsVar), ArrayItem.SINGLE_ARRAY_TYPE);
        Expression condition3 = Literal.makeLiteral(BooleanValue.TRUE);
        Expression action1 = IntegratedFunctionLibrary.makeFunctionCall(
                new MapGet(),
                new Expression[]{
                        new LocalVariableReference(lhsVar),
                        new LocalVariableReference(rhsVar)}
        );
        Expression action2branch1 = IntegratedFunctionLibrary.makeFunctionCall(
                new ArrayGet(),
                new Expression[]{
                        new LocalVariableReference(lhsVar),
                        new LocalVariableReference(rhsVar)}
        );
        Expression action2branch2 = IntegratedFunctionLibrary.makeFunctionCall(
                new ArrayGet(),
                new Expression[]{
                        new LocalVariableReference(lhsVar),
                        new LocalVariableReference(rhsVar)}
        );
        Expression action2branch3 = new ErrorExpression(
                "When the first argument of '?' is an array, the second argument must be an integer", "XPTY0004", true);
        Expression action2 = new Choose(
                new Expression[]{new InstanceOfExpression(new LocalVariableReference(rhsVar), SequenceType.SINGLE_INTEGER),
                        new InstanceOfExpression(new LocalVariableReference(rhsVar), SequenceType.SINGLE_UNTYPED_ATOMIC),
                        Literal.makeLiteral(BooleanValue.TRUE)},
                new Expression[]{action2branch1, action2branch2, action2branch3});
        Expression action3 = new ErrorExpression(
                "First argument of '?' must be a sequence of arrays and/or maps", "XPTY0004", true);
        Expression choice = new Choose(
                new Expression[]{condition1, condition2, condition3},
                new Expression[]{action1, action2, action3});
        lhsVar.setAction(rhsVar);
        rhsVar.setAction(choice);
        return lhsVar;
    }

    /**
     * Supporting code for lookup expressions (A?B) where B is an NCName
     *
     * @param lhs the LHS operand of the lookup expression
     * @param rhs the RHS operand of the lookup expression
     * @return the result of parsing the expression
     */

    private static Expression lookupName(Expression lhs, Expression rhs) {
        ForExpression lhsVar = new ForExpression();
        lhsVar.setVariableQName(new StructuredQName("qq", NamespaceConstant.SAXON, "qq" + lhsVar.hashCode()));
        lhsVar.setSequence(lhs);
        lhsVar.setRequiredType(SequenceType.SINGLE_FUNCTION);
        ForExpression rhsVar = new ForExpression();
        rhsVar.setVariableQName(new StructuredQName("qq", NamespaceConstant.SAXON, "qq" + rhsVar.hashCode()));
        rhsVar.setSequence(rhs);
        rhsVar.setRequiredType(SequenceType.ANY_SEQUENCE);

        Expression condition1 = new InstanceOfExpression(new LocalVariableReference(lhsVar), HashTrieMap.SINGLE_MAP_TYPE);
        Expression condition3 = Literal.makeLiteral(BooleanValue.TRUE);
        Expression action1 = IntegratedFunctionLibrary.makeFunctionCall(
                new MapGet(), new Expression[]{new LocalVariableReference(lhsVar), new LocalVariableReference(rhsVar)});
        Expression action3 = new ErrorExpression("First argument of '?' must be a sequence of maps", "XPTY0004", true);
        Expression choice = new Choose(new Expression[]{condition1, condition3}, new Expression[]{action1, action3});
        lhsVar.setAction(rhsVar);
        rhsVar.setAction(choice);
        return lhsVar;
    }

    /**
     * Supporting code for lookup expressions (A?B) where B is the wildcard "*"
     *
     * @param lhs the LHS operand of the lookup expression
     * @return the result of parsing the expression
     */

    private static Expression lookupStar(Expression lhs) {
        ForExpression lhsVar = new ForExpression();
        lhsVar.setVariableQName(new StructuredQName("qq", NamespaceConstant.SAXON, "qq" + lhsVar.hashCode()));
        lhsVar.setSequence(lhs);
        lhsVar.setRequiredType(SequenceType.SINGLE_FUNCTION);

        Expression condition1 = new InstanceOfExpression(new LocalVariableReference(lhsVar), HashTrieMap.SINGLE_MAP_TYPE);
        Expression condition2 = new InstanceOfExpression(new LocalVariableReference(lhsVar), ArrayItem.SINGLE_ARRAY_TYPE);
        Expression condition3 = Literal.makeLiteral(BooleanValue.TRUE);

        ExpressionTool.copyLocationInfo(lhs, condition1);
        ExpressionTool.copyLocationInfo(lhs, condition2);
        ExpressionTool.copyLocationInfo(lhs, condition3);

        // for $k in map:keys($M) return map:get($M, $k)
        ForExpression getMapValues = new ForExpression();
        getMapValues.setVariableQName(new StructuredQName("qq", NamespaceConstant.SAXON, "qq" + getMapValues.hashCode()));
        Expression mapKeys = IntegratedFunctionLibrary.makeFunctionCall(
                new MapKeys(), new Expression[]{new LocalVariableReference(lhsVar)});
        getMapValues.setSequence(mapKeys);
        getMapValues.setRequiredType(SequenceType.SINGLE_ATOMIC);
        Expression mapGet = IntegratedFunctionLibrary.makeFunctionCall(
                new MapGet(), new Expression[]{new LocalVariableReference(lhsVar), new LocalVariableReference(getMapValues)});
        getMapValues.setAction(mapGet);

        Expression action2 = IntegratedFunctionLibrary.makeFunctionCall(
                new ArrayToSequence(), new Expression[]{new LocalVariableReference(lhsVar)});
        Expression action3 = new ErrorExpression(
                "First argument of '?' must be a sequence of arrays and/or maps", "XPTY0004", true);
        Expression choice = new Choose(
                new Expression[]{condition1, condition2, condition3}, new Expression[]{getMapValues, action2, action3});
        lhsVar.setAction(choice);
        return lhsVar;
    }


    /**
     * Parse a NodeTest.
     * One of QName, prefix:*, *:suffix, *, text(), node(), comment(), or
     * processing-instruction(literal?), or element(~,~), attribute(~,~), etc.
     *
     * @param nodeType the node type being sought if one is specified
     * @return the resulting NodeTest object
     * @throws XPathException if any error is encountered
     */

    /*@NotNull*/
    protected NodeTest parseNodeTest(short nodeType) throws XPathException {
        int tok = t.currentToken;
        String tokv = t.currentTokenValue;
        switch (tok) {
            case Token.NAME:
                nextToken();
                return makeNameTest(nodeType, tokv, nodeType == Type.ELEMENT);

            case Token.PREFIX:
                nextToken();
                return makeNamespaceTest(nodeType, tokv);

            case Token.SUFFIX:
                nextToken();
                tokv = t.currentTokenValue;
                expect(Token.NAME);
                nextToken();
                return makeLocalNameTest(nodeType, tokv);

            case Token.STAR:
                nextToken();
                return NodeKindTest.makeNodeKindTest(nodeType);

            case Token.NODEKIND:
                return parseKindTest();

            default:
                grumble("Unrecognized node test");
                throw new XPathException(""); // unreachable instruction
        }
    }

    /**
     * Parse a KindTest
     *
     * @return the KindTest, expressed as a NodeTest object
     * @throws net.sf.saxon.trans.XPathException if a static error is found
     */

    /*@NotNull*/
    private NodeTest parseKindTest() throws XPathException {
        NamePool pool = env.getConfiguration().getNamePool();
        String typeName = t.currentTokenValue;
        boolean schemaDeclaration = typeName.startsWith("schema-");
        int primaryType = getSystemType(typeName);
        int nameCode = -1;
        boolean empty = false;
        nextToken();
        if (t.currentToken == Token.RPAR) {
            if (schemaDeclaration) {
                grumble("schema-element() and schema-attribute() require a name to be supplied");
                return null;
            }
            empty = true;
            nextToken();
        }
        switch (primaryType) {
            case Type.ITEM:
                grumble("item() is not allowed in a path expression");
                return null;
            case Type.NODE:
                if (empty) {
                    return AnyNodeTest.getInstance();
                } else {
                    grumble("No arguments are allowed in node()");
                    return null;
                }
            case Type.TEXT:
                if (empty) {
                    return NodeKindTest.TEXT;
                } else {
                    grumble("No arguments are allowed in text()");
                    return null;
                }
            case Type.COMMENT:
                if (empty) {
                    return NodeKindTest.COMMENT;
                } else {
                    grumble("No arguments are allowed in comment()");
                    return null;
                }
            case Type.NAMESPACE:
                if (empty) {
                    if (!isNamespaceTestAllowed()) {
                        grumble("namespace-node() test is not allowed in XPath 2.0/XQuery 1.0");
                    }
                    return NodeKindTest.NAMESPACE;
                } else {
                    if (language == EXTENDED_ITEM_TYPE && t.currentToken == Token.NAME) {
                        String nsName = t.currentTokenValue;
                        nextToken();
                        expect(Token.RPAR);
                        nextToken();
                        return new NameTest(Type.NAMESPACE, "", nsName, pool);
                    } else {
                        grumble("No arguments are allowed in namespace-node()");
                        return null;
                    }
                }
            case Type.DOCUMENT:
                if (empty) {
                    return NodeKindTest.DOCUMENT;
                } else {
                    int innerType;
                    try {
                        innerType = getSystemType(t.currentTokenValue);
                    } catch (XPathException err) {
                        innerType = Type.ITEM;
                    }
                    if (innerType != Type.ELEMENT) {
                        grumble("Argument to document-node() must be an element type descriptor");
                        return null;
                    }
                    NodeTest inner = parseKindTest();
                    expect(Token.RPAR);
                    nextToken();
                    return new DocumentNodeTest(inner);
                }
            case Type.PROCESSING_INSTRUCTION:
                if (empty) {
                    return NodeKindTest.PROCESSING_INSTRUCTION;
                } else if (t.currentToken == Token.STRING_LITERAL) {
                    String piName = Whitespace.trim(unescape(t.currentTokenValue));
                    if (!NameChecker.isValidNCName(piName)) {
                        // Became an error as a result of XPath erratum XP.E7
                        grumble("Processing instruction name must be a valid NCName", "XPTY0004");
                    } else {
                        nameCode = pool.allocate("", "", piName);
                    }
                } else if (t.currentToken == Token.NAME) {
                    try {
                        String[] parts = NameChecker.getQNameParts(t.currentTokenValue);
                        if (parts[0].isEmpty()) {
                            nameCode = makeNameCode(parts[1], false);
                        } else {
                            grumble("Processing instruction name must not contain a colon");
                        }
                    } catch (QNameException e) {
                        grumble("Invalid processing instruction name. " + e.getMessage());
                    }
                } else {
                    grumble("Processing instruction name must be a QName or a string literal");
                }
                nextToken();
                expect(Token.RPAR);
                nextToken();
                return new NameTest(Type.PROCESSING_INSTRUCTION, nameCode, pool);

            case Type.ATTRIBUTE:
                // drop through

            case Type.ELEMENT:
                String nodeName = "";
                if (empty) {
                    return NodeKindTest.makeNodeKindTest(primaryType);
                } else if (t.currentToken == Token.STAR || t.currentToken == Token.MULT) {
                    // allow for both representations of "*" to be safe
                    if (schemaDeclaration) {
                        grumble("schema-element() and schema-attribute() must specify an actual name, not '*'");
                        return null;
                    }
                    nameCode = -1;
                } else if (t.currentToken == Token.NAME) {
                    nodeName = t.currentTokenValue;
                    nameCode = makeNameCode(t.currentTokenValue, primaryType == Type.ELEMENT);
                } else {
                    grumble("Unexpected " + Token.tokens[t.currentToken] + " after '(' in SequenceType");
                }
                String suri = null;
                if (nameCode != -1) {
                    suri = pool.getURI(nameCode);
                }
                nextToken();
                if (t.currentToken == Token.RPAR) {
                    nextToken();
                    if (nameCode == -1) {
                        // element(*) or attribute(*)
                        return NodeKindTest.makeNodeKindTest(primaryType);
                    } else {
                        NodeTest nameTest;
                        if (primaryType == Type.ATTRIBUTE) {
                            // attribute(N) or schema-attribute(N)
                            if (schemaDeclaration) {
                                // schema-attribute(N)
                                SchemaDeclaration attributeDecl =
                                        env.getConfiguration().getAttributeDeclaration(nameCode & 0xfffff);
                                if (!env.isImportedSchema(suri)) {
                                    grumble("No schema has been imported for namespace '" + suri + '\'', "XPST0008");
                                }
                                if (attributeDecl == null) {
                                    grumble("There is no declaration for attribute @" + nodeName + " in an imported schema", "XPST0008");
                                    return null;
                                } else {
                                    return attributeDecl.makeSchemaNodeTest();
                                }
                            } else {
                                nameTest = new NameTest(Type.ATTRIBUTE, nameCode, pool);
                                return nameTest;
                            }
                        } else {
                            // element(N) or schema-element(N)
                            if (schemaDeclaration) {
                                // schema-element(N)
                                if (!env.isImportedSchema(suri)) {
                                    grumble("No schema has been imported for namespace '" + suri + '\'', "XPST0008");
                                }
                                SchemaDeclaration elementDecl =
                                        env.getConfiguration().getElementDeclaration(nameCode & 0xfffff);
                                if (elementDecl == null) {
                                    grumble("There is no declaration for element <" + nodeName + "> in an imported schema", "XPST0008");
                                    return null;
                                } else {
                                    return elementDecl.makeSchemaNodeTest();
                                }
                            } else {
                                nameTest = new NameTest(Type.ELEMENT, nameCode, pool);
                                return nameTest;
                            }
                        }
                    }
                } else if (t.currentToken == Token.COMMA) {
                    if (schemaDeclaration) {
                        grumble("schema-element() and schema-attribute() must have one argument only");
                        return null;
                    }
                    nextToken();
                    NodeTest result;
                    if (t.currentToken == Token.STAR) {
                        grumble("'*' is no longer permitted as the second argument of element() and attribute()");
                        return null;
                    } else if (t.currentToken == Token.NAME) {
                        SchemaType schemaType;
                        StructuredQName contentType = makeStructuredQName(t.currentTokenValue, env.getDefaultElementNamespace());
                        String uri = contentType.getURI();
                        String lname = contentType.getLocalPart();

                        if (uri.equals(NamespaceConstant.SCHEMA)) {
                            schemaType = env.getConfiguration().getSchemaType(contentType);
                        } else {
                            if (!env.isImportedSchema(uri)) {
                                grumble("No schema has been imported for namespace '" + uri + '\'', "XPST0008");
                            }
                            schemaType = env.getConfiguration().getSchemaType(contentType);
                        }
                        if (schemaType == null) {
                            grumble("Unknown type name " + contentType.getEQName(), "XPST0008");
                            return null;
                        }
                        if (primaryType == Type.ATTRIBUTE && schemaType.isComplexType()) {
                            warning("An attribute cannot have a complex type");
                        }
                        ContentTypeTest typeTest = new ContentTypeTest(primaryType, schemaType, env.getConfiguration(), false);
                        if (nameCode == -1) {
                            // this represents element(*,T) or attribute(*,T)
                            result = typeTest;
                            if (primaryType == Type.ATTRIBUTE) {
                                nextToken();
                            } else {
                                // assert (primaryType == Type.ELEMENT);
                                nextToken();
                                if (t.currentToken == Token.QMARK) {
                                    typeTest.setNillable(true);
                                    nextToken();
                                }
                            }
                        } else {
                            if (primaryType == Type.ATTRIBUTE) {
                                NodeTest nameTest = new NameTest(Type.ATTRIBUTE, nameCode, pool);
                                result = new CombinedNodeTest(nameTest, Token.INTERSECT, typeTest);
                                nextToken();
                            } else {
                                // assert (primaryType == Type.ELEMENT);
                                NodeTest nameTest = new NameTest(Type.ELEMENT, nameCode, pool);
                                result = new CombinedNodeTest(nameTest, Token.INTERSECT, typeTest);
                                nextToken();
                                if (t.currentToken == Token.QMARK) {
                                    typeTest.setNillable(true);
                                    nextToken();
                                }
                            }
                        }
                    } else {
                        grumble("Unexpected " + Token.tokens[t.currentToken] + " after ',' in SequenceType");
                        return null;
                    }

                    expect(Token.RPAR);
                    nextToken();
                    return result;
                } else {
                    grumble("Expected ')' or ',' in SequenceType");
                }
                return null;
            default:
                // can't happen!
                grumble("Unknown node kind");
                return null;
        }
    }

    /**
     * Ask whether the syntax namespace-node() is allowed in a node kind test.
     *
     * @return true unless XPath 2.0 / XQuery 1.0 syntax is required
     */

    protected boolean isNamespaceTestAllowed() {
        return allowXPath30Syntax;
    }

    /**
     * Get a system type - that is, one whose name is a keyword rather than a QName. This includes the node
     * kinds such as element and attribute, and the generic types node() and item()
     *
     * @param name the name of the system type, for example "element" or "comment"
     * @return the integer constant denoting the type, for example {@link Type#ITEM} or {@link Type#ELEMENT}
     * @throws XPathException if the name is not recognized
     */
    private int getSystemType(String name) throws XPathException {
        if ("item".equals(name)) {
            return Type.ITEM;
        } else if ("document-node".equals(name)) {
            return Type.DOCUMENT;
        } else if ("element".equals(name)) {
            return Type.ELEMENT;
        } else if ("schema-element".equals(name)) {
            return Type.ELEMENT;
        } else if ("attribute".equals(name)) {
            return Type.ATTRIBUTE;
        } else if ("schema-attribute".equals(name)) {
            return Type.ATTRIBUTE;
        } else if ("text".equals(name)) {
            return Type.TEXT;
        } else if ("comment".equals(name)) {
            return Type.COMMENT;
        } else if ("processing-instruction".equals(name)) {
            return Type.PROCESSING_INSTRUCTION;
        } else if ("namespace-node".equals(name)) {
            return Type.NAMESPACE;
        } else if ("node".equals(name)) {
            return Type.NODE;
        } else {
            grumble("Unknown type " + name);
            return -1;
        }
    }

    /**
     * Check that XPath 3.0 is in use
     *
     * @throws net.sf.saxon.trans.XPathException if XPath 3.0 support was not requested
     */

    protected void checkLanguageVersion30() throws XPathException {
        if (!allowXPath30Syntax) {
            grumble("To use XPath 3.0 syntax, you must configure the XPath parser to handle it");
        }
    }

    /**
     * Check that XPath 3.1 is in use
     *
     * @throws net.sf.saxon.trans.XPathException if XPath 3.1 support was not requested
     */

    protected void checkLanguageVersion31() throws XPathException {
        if (!allowXPath31Syntax) {
            grumble("To use XPath 3.1 syntax, you must configure the XPath parser to handle it");
        }
    }

    /**
     * Parse a map expression. Requires XPath/XQuery 3.0
     * Provisional syntax
     * map { expr : expr (, expr : expr )*} }
     *
     * @return the map expression
     * @throws XPathException
     */

    /*@NotNull*/
    protected Expression parseMapExpression() throws XPathException {
        checkLanguageVersion31();

        // have read the "map {"
        SymbolicName sn = new SymbolicName(StandardNames.XSL_FUNCTION, MapEntry.FUNCTION_NAME, 2);
        Tokenizer t = getTokenizer();
        int offset = t.currentTokenStartOffset;
        List<Expression> entries = new ArrayList<Expression>();
        StaticContext env = getStaticContext();
        nextToken();
        if (t.currentToken != Token.RCURLY) {
            while (true) {
                Expression key = parseExprSingle();
                if (t.currentToken == Token.ASSIGN) {
                    grumble("The ':=' notation is no longer accepted in map expressions: use ':' instead");
                }
                expect(Token.COLON);
                nextToken();
                Expression value = parseExprSingle();
                Expression[] arguments = new Expression[]{key, value};
                Expression fcall = env.getFunctionLibrary().bind(sn, arguments, env);
                entries.add(fcall);
                if (t.currentToken == Token.RCURLY) {
                    break;
                } else {
                    expect(Token.COMMA);
                    nextToken();
                }
            }
        }
        t.lookAhead(); //manual lookahead after an RCURLY
        nextToken();
        Expression[] entriesArray = new Expression[entries.size()];
        Block block = new Block(entries.toArray(entriesArray));
        sn = new SymbolicName(StandardNames.XSL_FUNCTION, MapNew.FUNCTION_NAME, 1);
        Expression result = env.getFunctionLibrary().bind(sn, new Expression[]{block}, env);
        setLocation(result, offset);
        return result;

    }

    /**
     * Parse a "square" array constructor
     * "[" (exprSingle ("," exprSingle)* )? "]"
     * Applies to XPath/XQuery 3.1 only
     */
    protected Expression parseArraySquareConstructor() throws XPathException {
        checkLanguageVersion31();
        Tokenizer t = getTokenizer();
        int offset = t.currentTokenStartOffset;
        List<Expression> members = new ArrayList<Expression>();
        nextToken();
        if (t.currentToken == Token.RSQB) {
            nextToken();
            ArrayBlock block = new ArrayBlock(members);
            setLocation(block, offset);
            return block;
        }
        while (true) {
            Expression member = parseExprSingle();
            members.add(member);
            if (t.currentToken == Token.COMMA) {
                nextToken();
                continue;
            } else if (t.currentToken == Token.RSQB) {
                nextToken();
                break;
            }
            grumble("Expected ',' or ']', " +
                    "found " + Token.tokens[t.currentToken]);
            return new ErrorExpression();
        }
        ArrayBlock block = new ArrayBlock(members);
        setLocation(block, offset);
        return block;
    }

    /**
     * Parse a "curly" array constructor
     * array "{" expr "}"
     * Applies to XPath/XQuery 3.1 only
     *
     * @return the parsed expression
     * @throws XPathException if the syntax is invalid or the construct is not permitted
     */

    protected Expression parseArrayCurlyConstructor() throws XPathException {
        checkLanguageVersion31();
        Tokenizer t = getTokenizer();
        int offset = t.currentTokenStartOffset;
        nextToken();
        if (t.currentToken == Token.RCURLY) {
            t.lookAhead(); //manual lookahead after an RCURLY
            nextToken();
            return Literal.makeLiteral(SimpleArrayItem.EMPTY_ARRAY);
        }
        Expression body = parseExpression();
        expect(Token.RCURLY);
        t.lookAhead(); //manual lookahead after an RCURLY
        nextToken();
        ArrayFromSequence defn = new ArrayFromSequence();
        Expression result =  IntegratedFunctionLibrary.makeFunctionCall(defn, new Expression[]{body});
        setLocation(result, offset);
        return result;
    }

    /**
     * Parse a function call.
     * function-name '(' ( Expression (',' Expression )* )? ')'
     *
     * @param prefixArgument left hand operand of arrow operator,
     *                       or null in the case of a conventional function call
     * @return the resulting subexpression
     * @throws XPathException if any error is encountered
     */

    /*@NotNull*/
    public Expression parseFunctionCall(Expression prefixArgument) throws XPathException {

        String fname = t.currentTokenValue;
        int offset = t.currentTokenStartOffset;
        ArrayList<Expression> args = new ArrayList<Expression>(10);
        if (prefixArgument != null) {
            args.add(prefixArgument);
        }

        StructuredQName functionName = resolveFunctionName(fname);
        IntSet placeMarkers = null;

        // the "(" has already been read by the Tokenizer: now parse the arguments

        nextToken();
        if (t.currentToken != Token.RPAR) {
            while (true) {
                Expression arg = parseFunctionArgument();
                if (arg == null) {
                    // this is a "?" placemarker
                    if (placeMarkers == null) {
                        placeMarkers = new IntArraySet();
                    }
                    placeMarkers.add(args.size());
                    arg = Literal.makeEmptySequence(); // a convenient fiction
                }
                args.add(arg);
                if (t.currentToken == Token.COMMA) {
                    nextToken();
                } else {
                    break;
                }
            }
            expect(Token.RPAR);
        }
        nextToken();

        if (scanOnly) {
            return new StringLiteral(StringValue.EMPTY_STRING);
        }

        Expression[] arguments = new Expression[args.size()];
        args.toArray(arguments);

        if (placeMarkers != null) {
            return makeCurriedFunction(offset, functionName, arguments, placeMarkers);
        }

        Expression fcall;
        try {
            SymbolicName sn = new SymbolicName(StandardNames.XSL_FUNCTION, functionName, args.size());
            fcall = env.getFunctionLibrary().bind(sn, arguments, env);
            if (fcall instanceof UserFunctionCall && env instanceof ExpressionContext) {
                final UserFunctionCall ufCall = (UserFunctionCall) fcall;
                final PrincipalStylesheetModule pack = ((ExpressionContext) env).getStyleElement().getCompilation().getPrincipalStylesheetModule();
                final ExpressionVisitor visitor = ExpressionVisitor.make(env);
                pack.addFixupAction(new PrincipalStylesheetModule.Action() {
                    public void doAction() throws XPathException {
                        if (ufCall.getFunction() == null) {
                            Component target = pack.getComponent(ufCall.getSymbolicName());
                            UserFunction fn = (UserFunction) target.getCode();
                            if (fn != null) {
                                ufCall.setFunction(fn);
                                ufCall.checkFunctionCall(fn, visitor);
                                ufCall.computeArgumentEvaluationModes();
                                ufCall.setStaticType(fn.getResultType());
                            } else {
                                XPathException err = new XPathException("There is no available function named " + ufCall.getDisplayName() +
                                        " with " + ufCall.getArity() + " arguments", "XPST0017");
                                err.setLocator(ufCall.getLocation());
                                throw err;
                            }
                        }
                    }
                });
            }
        } catch (XPathException err) {
            if (err.getErrorCodeQName() == null) {
                err.setErrorCode("XPST0017");
                err.setIsStaticError(true);
            }
            if (functionName.hasURI(NamespaceConstant.MAP_FUNCTIONS_2011)) {
                grumble("Saxon currently implements the XSLT 3.0 map functions in namespace " + NamespaceConstant.MAP_FUNCTIONS);
            } else {
                grumble(err.getMessage(), err.getErrorCodeQName(), offset);
            }
            return new ErrorExpression();
        }
        if (fcall == null) {
            return reportMissingFunction(offset, functionName, arguments);
        }
        //  A QName or NOTATION constructor function must be given the namespace context now
//        if (fcall instanceof CastExpression &&
//                ((AtomicType) fcall.getItemType()).isNamespaceSensitive()) {
//            ((CastExpression) fcall).bindStaticContext(env);
//        }
        // There are special rules for certain functions appearing in a pattern
        if (language == XSLT_PATTERN) {
            if (fcall.isCallOn(RegexGroup.class)) {
                return Literal.makeEmptySequence();
            } else if (fcall instanceof CurrentGroupCall) {
                grumble("The current-group() function cannot be used in a pattern",
                        "XTSE1060", offset);
                return new ErrorExpression();
            } else if (fcall instanceof CurrentGroupingKeyCall) {
                grumble("The current-grouping-key() function cannot be used in a pattern",
                        "XTSE1070", offset);
                return new ErrorExpression();
            } else if (fcall.isCallOnSystemFunction("current-merge-group")) {
                grumble("The current-merge-group() function cannot be used in a pattern",
                        "XTSE3470", offset);
                return new ErrorExpression();
            } else if (fcall.isCallOnSystemFunction("current-merge-key")) {
                grumble("The current-merge-key() function cannot be used in a pattern",
                        "XTSE3500", offset);
                return new ErrorExpression();
            }
        }
        setLocation(fcall, offset);
        for (Expression argument : arguments) {
            if (fcall != argument) {
                // avoid doing this when the function has already been optimized away, e.g. unordered()
                fcall.adoptChildExpression(argument);
            }
        }

        return makeTracer(offset, fcall, LocationKind.FUNCTION_CALL, functionName);

    }

    /*@NotNull*/
    public Expression reportMissingFunction(int offset, StructuredQName functionName, Expression[] arguments) throws XPathException {
        String msg = "Cannot find a matching " + arguments.length +
                "-argument function named " + functionName.getClarkName() + "()";
        Configuration config = env.getConfiguration();
        if (config.getBooleanProperty(FeatureKeys.ALLOW_EXTERNAL_FUNCTIONS)) {
            boolean existsWithDifferentArity = false;
            for (int i = 0; i < arguments.length + 5; i++) {
                if (i != arguments.length) {
                    SymbolicName sn = new SymbolicName(StandardNames.XSL_FUNCTION, functionName, i);
                    if (env.getFunctionLibrary().isAvailable(sn)) {
                        existsWithDifferentArity = true;
                        break;
                    }
                }
            }
            if (existsWithDifferentArity) {
                msg += ". The namespace URI and local name are recognized, but the number of arguments is wrong";
            } else {
                String supplementary = getMissingFunctionExplanation(functionName, config);
                if (supplementary != null) {
                    msg += ". " + supplementary;
                }
            }
        } else {
            msg += ". External function calls have been disabled";
        }
        if (env.isInBackwardsCompatibleMode()) {
            // treat this as a dynamic error to be reported only if the function call is executed
            return new ErrorExpression(msg, "XTDE1425", false);
        } else {
            grumble(msg, "XPST0017", offset);
            return null;
        }
    }

    public static String getMissingFunctionExplanation(StructuredQName functionName, Configuration config) {
        String actualURI = functionName.getURI();
        String similarNamespace = NamespaceConstant.findSimilarNamespace(actualURI);
        if (similarNamespace != null) {
            if (similarNamespace.equals(actualURI)) {
                if (similarNamespace.equals(NamespaceConstant.SAXON) && config.getEditionCode().equals("HE")) {
                    return "Saxon extension functions are not available under Saxon-HE";
                } else if (similarNamespace.equals(NamespaceConstant.SAXON) &&
                        !config.isLicensedFeature(Configuration.LicenseFeature.PROFESSIONAL_EDITION)) {
                    return "Saxon extension functions require a Saxon-PE or Saxon-EE license";
                } else if (similarNamespace.equals(NamespaceConstant.XSLT)) {
                    if (functionName.getLocalPart().equals("original")) {
                        return "Function name xsl:original is only available within an overriding function";
                    } else {
                        return "There are no functions defined in the XSLT namespace";
                    }
                } else {
                    return "There is no Saxon extension function with the local name " + functionName.getLocalPart();
                }
            } else {
                return "Perhaps the intended namespace was '" + similarNamespace + "'";
            }
        } else if (actualURI.contains("java")) {
            if (config.getEditionCode().equals("HE")) {
                return "Reflexive calls to Java methods are not available under Saxon-HE";
            } else if (!config.isLicensedFeature(Configuration.LicenseFeature.PROFESSIONAL_EDITION)) {
                return "Reflexive calls to Java methods require a Saxon-PE or Saxon-EE license, and none was found";
            } else {
                return "For diagnostics on calls to Java methods, use the -TJ command line option " +
                        "or set the Configuration property FeatureKeys.TRACE_EXTERNAL_FUNCTIONS";
            }
        } else if (actualURI.startsWith("clitype:")) {
            if (config.getEditionCode().equals("HE")) {
                return "Reflexive calls to external .NET methods are not available under Saxon-HE";
            } else if (!config.isLicensedFeature(Configuration.LicenseFeature.PROFESSIONAL_EDITION)) {
                return "Reflexive calls to external .NET methods require a Saxon-PE or Saxon-EE license, and none was found";
            } else {
                return "For diagnostics on calls to .NET methods, use the -TJ command line option " +
                        "or call processor.SetProperty(\"http://saxon.sf.net/feature/trace-external-functions\", \"true\")";
            }
        } else {
            return null;
        }
    }

    /**
     * Interpret a function name, returning it as a resolved QName
     *
     * @param fname the lexical QName used as the function name; or an EQName presented
     *              by the tokenizer as a name in Clark notation
     * @return the Structured QName obtained by resolving any prefix in the function name
     * @throws XPathException if the supplied name is not a valid QName or if its prefix
     *                        is not in scope
     */

    /*@NotNull*/
    protected StructuredQName resolveFunctionName(String fname) throws XPathException {
        if (scanOnly) {
            return new StructuredQName("", NamespaceConstant.SAXON, "dummy");
        }
        StructuredQName functionName = null;
        qNameParser.setDefaultNamespace(env.getDefaultFunctionNamespace());
        try {
            functionName = qNameParser.parse(fname);
        } catch (XPathException e) {
            grumble(e.getMessage(), e.getErrorCodeLocalPart());
        }

        if (functionName.hasURI(NamespaceConstant.SCHEMA)) {
            ItemType t = Type.getBuiltInItemType(functionName.getURI(), functionName.getLocalPart());
            if (t instanceof BuiltInAtomicType) {
                checkAllowedType(env, (BuiltInAtomicType) t);
            }
        }
        return functionName;
    }

    /**
     * Parse an argument to a function call. Separate method so it can
     * be overridden. With higher-order-function syntax in XPath 3.0/XQuery 3.0,
     * this returns null if the pseudo-argument "?" is found.
     *
     * @return the Expression used as the argument, or null if the argument is the place-holder "?"
     * @throws XPathException if the argument expression does not parse correctly
     */

    /*@Nullable*/
    public Expression parseFunctionArgument() throws XPathException {
        return parseExprSingle();
    }

    /**
     * Parse a literal function item (introduced in XQuery 1.1)
     * Syntax: QName # integer
     * The QName and # have already been read
     *
     * @return an ExternalObject representing the function item
     * @throws net.sf.saxon.trans.XPathException if a static error is encountered
     */

    /*@NotNull*/
    protected Expression parseNamedFunctionReference() throws XPathException {
        grumble("Named function references are not allowed in Saxon-HE");
        return new ErrorExpression();
    }

    /**
     * Parse the annotations that can appear in a variable or function declaration
     *
     * @return the annotations as a map, indexed by annotation name
     * @throws XPathException in the event of a syntax error
     */

    protected Map<StructuredQName, Annotation> parseAnnotations() throws XPathException {
        grumble("Inline functions are not allowed in Saxon-HE");
        return null;
    }

    /**
     * Parse the annotations that can appear in a variable or function declaration
     *
     * @return the annotations as a list
     * @throws XPathException in the event of a syntax error
     */

    protected ArrayList<Annotation> parseAnnotationsList() throws XPathException {
        grumble("Inline functions are not allowed in Saxon-HE");
        return null;
    }

    /**
     * Check the list of annotations that appear in a variable or function declaration, for duplicates, etc.
     *
     * @return the annotations as a map, indexed by annotation name
     * @throws XPathException in the event of a syntax error
     */

    protected Map<StructuredQName, Annotation> checkAnnotations(ArrayList<Annotation> annotationList, boolean isFunction) throws XPathException {
        return null;
    }

    /**
     * Parse an inline function
     * "function" "(" ParamList? ")" ("as" SequenceType)? EnclosedExpr
     * On entry, "function (" has already been read
     *
     * @param annotations the function annotations, which have already been read, or null if there are none
     * @return the parsed inline function
     * @throws XPathException if a syntax error is found
     */

    /*@NotNull*/
    protected Expression parseInlineFunction(Map<StructuredQName, Annotation> annotations) throws XPathException {
        grumble("Inline functions are not allowed in Saxon-HE");
        return new ErrorExpression();
    }

    /**
     * Process a function call in which one or more of the argument positions are
     * represented as "?" placemarkers (indicating partial application or currying)
     *
     * @param offset       the position of the expression in the source text
     * @param name         the function name (as if there were no currying)
     * @param args         the arguments (with EmptySequence in the placemarker positions)
     * @param placeMarkers the positions of the placemarkers    @return the curried function
     * @return the curried function
     * @throws XPathException if a static error is found
     */

    /*@NotNull*/
    protected Expression makeCurriedFunction(
            int offset, StructuredQName name, Expression[] args, IntSet placeMarkers)
            throws XPathException {
        grumble("Partial function application is not allowed in Saxon-HE");
        return new ErrorExpression();
    }

    /* Must be in alphabetical order, since a binary search is used */

    private final static String[] reservedFunctionNames30 = new String[]{
            "attribute", "comment", "document-node", "element", "empty-sequence", "function", "if", "item",
            "namespace-node", "node", "processing-instruction", "schema-attribute", "schema-element", "switch", "text", "typeswitch"
    };

    /**
     * Check whether a function name is reserved in XPath 3.0 (when unprefixed)
     *
     * @param name the function name (the local-name as a string)
     * @return true if the function name is reserved
     */

    protected final static boolean isReservedFunctionName30(String name) {
        int x = Arrays.binarySearch(reservedFunctionNames30, name);
        return x >= 0;
    }

    /* Must be in alphabetical order, since a binary search is used */

    private final static String[] reservedFunctionNames31 = new String[]{
            "array", "attribute", "comment", "document-node", "element", "empty-sequence", "function", "if", "item", "map",
            "namespace-node", "node", "processing-instruction", "schema-attribute", "schema-element", "switch", "text", "typeswitch"
    };

    /**
     * Check whether a function name is reserved in XPath 3.1 (when unprefixed)
     *
     * @param name the function name (the local-name as a string)
     * @return true if the function name is reserved
     */

    protected final static boolean isReservedFunctionName31(String name) {
        int x = Arrays.binarySearch(reservedFunctionNames31, name);
        return x >= 0;
    }


    //////////////////////////////////////////////////////////////////////////////////
    // Routines for handling range variables
    //////////////////////////////////////////////////////////////////////////////////

    /**
     * Get the stack of in-scope range variables
     *
     * @return the stack of variables
     */

    public Stack<LocalBinding> getRangeVariables() {
        return rangeVariables;
    }

    /**
     * Set a new stack of in-scope range variables
     *
     * @param variables the stack of variables
     */

    public void setRangeVariables(Stack<LocalBinding> variables) {
        this.rangeVariables = variables;
    }

    /**
     * Declare a range variable (record its existence within the parser).
     * A range variable is a variable declared within an expression, as distinct
     * from a variable declared in the context.
     *
     * @param declaration the variable declaration to be added to the stack
     * @throws XPathException if any error is encountered
     */

    public void declareRangeVariable(LocalBinding declaration) throws XPathException {
        rangeVariables.push(declaration);
    }

    /**
     * Note when the most recently declared range variable has gone out of scope
     */

    public void undeclareRangeVariable() {
        rangeVariables.pop();
    }

    /**
     * Locate a range variable with a given name. (By "range variable", we mean a
     * variable declared within the expression where it is used.)
     *
     * @param qName identifies the name of the range variable
     * @return null if not found (this means the variable is probably a
     * context variable); otherwise the relevant RangeVariable
     */

    /*@Nullable*/
    protected LocalBinding findRangeVariable(StructuredQName qName) {
        for (int v = rangeVariables.size() - 1; v >= 0; v--) {
            LocalBinding b = rangeVariables.elementAt(v);
            if (b.getVariableQName().equals(qName)) {
                return b;
            }
        }
        return null;  // not an in-scope range variable
    }

    /**
     * Set the range variable stack. Used when parsing a nested subexpression
     * inside an attribute constructor.
     *
     * @param stack the stack to be used for local variables declared within the expression
     */

    public void setRangeVariableStack(Stack<LocalBinding> stack) {
        rangeVariables = stack;
    }

    /**
     * Make a NameCode, using the static context for namespace resolution
     *
     * @param qname      The name as written, in the form "[prefix:]localname"; alternatively,
     *                   a QName in Clark notation ({uri}local)
     * @param useDefault Defines the action when there is no prefix. If
     *                   true, use the default namespace URI for element names. If false,
     *                   use no namespace URI (as for attribute names).
     * @return the namecode, which can be used to identify this name in the
     * name pool
     * @throws XPathException if the name is invalid, or the prefix
     *                        undeclared
     */

    public final int makeNameCode(/*@NotNull*/ String qname, boolean useDefault) throws XPathException {
        if (scanOnly) {
            return StandardNames.XML_SPACE;
        }
        try {
            qNameParser.setDefaultNamespace(useDefault ? env.getDefaultElementNamespace() : "");
            StructuredQName sq = qNameParser.parse(qname);
            return env.getConfiguration().getNamePool().allocate(sq.getPrefix(), sq.getURI(), sq.getLocalPart());
        } catch (XPathException e) {
            grumble(e.getMessage(), e.getErrorCodeLocalPart());
            return -1;
        }
    }

    /**
     * Make a NameCode, using the static context for namespace resolution.
     * This variant of the method does not call "grumble" to report any errors
     * to the ErrorListener, it only reports errors by throwing exceptions. This
     * allows the caller to control the message output.
     *
     * @param qname      The name as written, in the form "[prefix:]localname"
     * @param defaultUri Defines the URI to be returned if there is no prefix.
     * @return the structured QName
     * @throws XPathException if the name is invalid, or the prefix
     *                        undeclared or if the name is not a lexically valid QName
     */

    public final StructuredQName makeStructuredQNameSilently(/*@NotNull*/ String qname, String defaultUri)
            throws XPathException {
        if (scanOnly) {
            return new StructuredQName("", NamespaceConstant.SAXON, "dummy");
        }
        qNameParser.setDefaultNamespace(defaultUri);
        return qNameParser.parse(qname);
    }

    /**
     * Make a Structured QName, using the static context for namespace resolution
     *
     * @param qname      The name as written, in the form "[prefix:]localname"; alternatively, a QName in
     *                   Clark format ({uri}local)
     * @param defaultUri The URI to be used if the name is written as a localname with no prefix
     * @return the QName as an instance of StructuredQName
     * @throws XPathException if the name is invalid, or the prefix
     *                        undeclared
     */

    /*@NotNull*/
    public final StructuredQName makeStructuredQName(/*@NotNull*/ String qname, String defaultUri) throws XPathException {
        try {
            return makeStructuredQNameSilently(qname, defaultUri);
        } catch (XPathException err) {
            grumble(err.getMessage(), err.getErrorCodeLocalPart());
            return null;
        }
    }

    /**
     * Make a FingerprintedQName, using the static context for namespace resolution
     *
     * @param qname      The name as written, in the form "[prefix:]localname"; alternatively, a QName in
     *                   Clark format ({uri}local)
     * @param useDefault Defines the action when there is no prefix. If
     *                   true, use the default namespace URI for element names. If false,
     *                   use no namespace URI (as for attribute names).
     * @return the fingerprinted QName
     * @throws XPathException if the name is invalid, or the prefix
     *                        undeclared
     * @throws QNameException if the supplied qname is not a lexically valid QName
     */

    /*@NotNull*/
    public final NodeName makeNodeName(/*@NotNull*/ String qname, boolean useDefault) throws XPathException, QNameException {
        StructuredQName sq = makeStructuredQNameSilently(qname, useDefault ? env.getDefaultElementNamespace() : "");
        String prefix = sq.getPrefix();
        String uri = sq.getURI();
        String local = sq.getLocalPart();
        if (uri.isEmpty()) {
            int nc = env.getConfiguration().getNamePool().allocate("", "", qname);
            return new NoNamespaceName(qname, nc);
        } else {
            int nc = env.getConfiguration().getNamePool().allocate(prefix, uri, local);
            return new FingerprintedQName(prefix, uri, local, nc);
        }
    }


    /**
     * Make a NameTest, using the static context for namespace resolution
     *
     * @param nodeType   the type of node required (identified by a constant in
     *                   class Type)
     * @param qname      the lexical QName of the required node; alternatively,
     *                   a QName in Clark notation ({uri}local)
     * @param useDefault true if the default namespace should be used when
     *                   the QName is unprefixed
     * @return a NameTest, representing a pattern that tests for a node of a
     * given node kind and a given name
     * @throws XPathException if the QName is invalid
     */

    /*@NotNull*/
    public NameTest makeNameTest(short nodeType, /*@NotNull*/ String qname, boolean useDefault)
            throws XPathException {
        int nameCode = makeNameCode(qname, useDefault);
        return new NameTest(nodeType, nameCode, env.getConfiguration().getNamePool());
    }

    /**
     * Make a NamespaceTest (name:*)
     *
     * @param nodeType integer code identifying the type of node required
     * @param prefix   the namespace prefix
     * @return the NamespaceTest, a pattern that matches all nodes in this
     * namespace
     * @throws XPathException if the namespace prefix is not declared
     */

    /*@NotNull*/
    public NamespaceTest makeNamespaceTest(short nodeType, String prefix)
            throws XPathException {
        NamePool pool = env.getConfiguration().getNamePool();
        if (scanOnly) {
            // return an arbitrary namespace if we're only doing a syntax check
            return new NamespaceTest(pool, nodeType, NamespaceConstant.SAXON);
        }
        if (prefix.startsWith("Q{")) {
            String uri = prefix.substring(2, prefix.length() - 2);
            return new NamespaceTest(pool, nodeType, uri);
        }
        try {
            StructuredQName sq = qNameParser.parse(prefix + ":dummy");
            return new NamespaceTest(pool, nodeType, sq.getURI());
        } catch (XPathException err) {
            grumble(err.getMessage(), err.getErrorCodeLocalPart());
            return null;
        }
    }

    /**
     * Make a LocalNameTest (*:name)
     *
     * @param nodeType  the kind of node to be matched
     * @param localName the requred local name
     * @return a LocalNameTest, a pattern which matches all nodes of a given
     * local name, regardless of namespace
     * @throws XPathException if the local name is invalid
     */

    /*@NotNull*/
    public LocalNameTest makeLocalNameTest(short nodeType, String localName)
            throws XPathException {
        if (!NameChecker.isValidNCName(localName)) {
            grumble("Local name [" + localName + "] contains invalid characters");
        }
        return new LocalNameTest(env.getConfiguration().getNamePool(), nodeType, localName);
    }

    /**
     * Set location information on an expression. At present this consists of a simple
     * line number. Needed mainly for XQuery.
     *
     * @param exp the expression whose location information is to be set
     */

    protected void setLocation(/*@NotNull*/ Expression exp) {
        setLocation(exp, t.currentTokenStartOffset);
    }

    /**
     * Set location information on an expression. At present only the line number
     * is retained. Needed mainly for XQuery. This version of the method supplies an
     * explicit offset (character position within the expression or query), which the tokenizer
     * can convert to a line number and column number.
     *
     * @param exp    the expression whose location information is to be set
     * @param offset the character position within the expression (ignoring newlines)
     */

    public void setLocation(Expression exp, int offset) {
        if (exp != null) {
            int line = t.getLineNumber(offset);
            int column = t.getColumnNumber(offset);
            if (exp.getLocation() == null || exp.getLocation() == ExplicitLocation.UNKNOWN_LOCATION) {
                Location loc = makeNestedLocation(env.getContainingLocation(), line, column, null);
                exp.setLocation(loc);
            }
        }
    }

    /**
     * Set location information on a clause of a FLWOR expression. This version of the method supplies an
     * explicit offset (character position within the expression or query), which the tokenizer
     * can convert to a line number and column number.
     *
     * @param clause the clause whose location information is to be set
     * @param offset the character position within the expression (ignoring newlines)
     */

    public void setLocation(Clause clause, int offset) {
        int line = t.getLineNumber(offset);
        int column = t.getColumnNumber(offset);
        Location loc = makeNestedLocation(env.getContainingLocation(), line, column, null);
        clause.setLocation(loc);
        clause.setPackageData(env.getPackageData());
    }

    private Location mostRecentLocation = ExplicitLocation.UNKNOWN_LOCATION;

    public Location makeLocation() {
        if (t.getLineNumber() == mostRecentLocation.getLineNumber() &&
                t.getColumnNumber() == mostRecentLocation.getColumnNumber() &&
                ((env.getSystemId() == null && mostRecentLocation.getSystemId() == null) ||
                        env.getSystemId().equals(mostRecentLocation.getSystemId()))) {
            return mostRecentLocation;
        } else {
            int line = t.getLineNumber();
            int column = t.getColumnNumber();
            mostRecentLocation = makeNestedLocation(env.getContainingLocation(), line, column, null);
            return mostRecentLocation;
        }
    }

    /**
     * Make a Location object relative to an existing location
     * @param containingLoc the containing location
     * @param line the line number relative to the containing location (zero-based)
     * @param column the column number relative to the containing location (zero-based)
     * @param nearbyText (maybe null) expression text around the point of the error
     * @return a suitable Location object
     */

    public Location makeNestedLocation(Location containingLoc, int line, int column, String nearbyText) {
        if (containingLoc instanceof ExplicitLocation &&
                containingLoc.getLineNumber() <= 1 && containingLoc.getColumnNumber() == -1 &&
                nearbyText == null) {
            // No extra information available about the container
            return new ExplicitLocation(env.getSystemId(), line + 1, column + 1);
        } else {
            return new NestedLocation(containingLoc, line, column, nearbyText);
        }
    }


    /**
     * If tracing, wrap an expression in a trace instruction
     *
     * @param startOffset the position of the expression in the soruce
     * @param exp         the expression to be wrapped
     * @param construct   integer constant identifying the kind of construct
     * @param qName       the name of the construct (if applicable)
     * @return the expression that does the tracing
     */

    public Expression makeTracer(int startOffset, Expression exp, int construct, /*@Nullable*/ StructuredQName qName) {
        if (codeInjector != null) {
            return codeInjector.inject(exp, env, construct, qName);
        } else {
            return exp;
        }
    }

    /**
     * Test whether the current token is a given keyword.
     *
     * @param s The string to be compared with the current token
     * @return true if they are the same
     */

    protected boolean isKeyword(String s) {
        return t.currentToken == Token.NAME && t.currentTokenValue.equals(s);
    }

    /**
     * Normalize an EQName. This is written in the source code in the form Q{uri}local, but by the
     * time it gets here it has been converted to Clark format {uri}local. This method collapses
     * whitespace within the URI
     *
     * @param s the EQName in the form of a Clark name
     * @return the normalized EQName
     * @throws XPathException so that the XQuery implementation in a subclass can do so.
     */

    protected String normalizeEQName(String s) throws XPathException {
        // overridden for XQuery
        if (!Whitespace.containsWhitespace(s)) {
            return s;
        }
        StructuredQName sq;
        try {
            sq = StructuredQName.fromClarkName(s);
        } catch (IllegalArgumentException e) {
            throw new XPathException(e);
        }
        CharSequence uri = Whitespace.collapseWhitespace(sq.getURI());
        return "{" + uri + "}" + sq.getLocalPart();
    }

    /**
     * Set that we are parsing in "scan only"
     *
     * @param scanOnly true if parsing is to proceed in scan-only mode. In this mode
     *                 namespace bindings are not yet known, so no attempt is made to look up namespace
     *                 prefixes.
     */

    public void setScanOnly(boolean scanOnly) {
        this.scanOnly = scanOnly;
    }

    /**
     * Say whether an absent expression is permitted
     *
     * @param allowEmpty true if it is permitted for the expression to consist
     *                   only of whitespace and comments, in which case the result
     *                   of parsing will be an EmptySequence literal
     */

    public void setAllowAbsentExpression(boolean allowEmpty) {
        this.allowAbsentExpression = allowEmpty;
    }

    /**
     * Ask whether an absent expression is permitted
     *
     * @return true if it is permitted for the expression to consist
     * only of whitespace and comments, in which case the result
     * of parsing will be an EmptySequence literal
     */

    public boolean isAllowAbsentExpression(boolean allowEmpty) {
        return this.allowAbsentExpression;
    }

    /**
     * Check that the higher-order-function feature is licensed
     *
     * @throws XPathException if the feature is licensed
     */

    protected void checkHofFeature() throws XPathException {
        env.getConfiguration().checkLicensedFeature(Configuration.LicenseFeature.PROFESSIONAL_EDITION, "higher-order functions", -1);
    }

    /**
     * A nested location: for use with XPath expressions and queries nested within some
     * larger document. The location information comes in two parts: the location of the query
     * or expression within the containing document, and the location of an error within the
     * query or XPath expression.
     */

    public static class NestedLocation implements Location {

        private Location containingLocation;
        private int localLineNumber;
        private int localColumnNumber;
        private String nearbyText;

        /**
         * Create a NestedLocation
         *
         * @param containingLocation the location of the containing construct, typically an attribute or
         *                           text node in an XML document
         * @param localLineNumber    the line number within the containing construct, starting at zero
         * @param localColumnNumber  the column number within the containing construct, starting at zero
         */
        public NestedLocation(Location containingLocation, int localLineNumber, int localColumnNumber) {
            this.containingLocation = containingLocation.saveLocation();
            this.localLineNumber = localLineNumber;
            this.localColumnNumber = localColumnNumber;
        }

        /**
         * Create a NestedLocation
         *
         * @param containingLocation the location of the containing construct, typically an attribute or
         *                           text node in an XML document
         * @param localLineNumber    the line number within the containing construct, starting at zero
         * @param localColumnNumber  the column number within the containing construct, starting at zero
         * @param nearbyText         text appearing in the vicinity of the error location
         */

        public NestedLocation(Location containingLocation, int localLineNumber, int localColumnNumber, String nearbyText) {
            this.containingLocation = containingLocation.saveLocation();
            this.localLineNumber = localLineNumber;
            this.localColumnNumber = localColumnNumber;
            this.nearbyText = nearbyText;
        }

        /**
         * Get the location of the container. This is normally used for expressions nested within
         * an XML document, where the container location gives the location of the attribute or text
         * node holding the XPath expression as a whole
         *
         * @return the location of the containing expression or query
         */

        public Location getContainingLocation() {
            return containingLocation;
        }

        /**
         * Get the column number of the error within the expression or query
         *
         * @return the column number. This is generally maintained only during parsing,
         * so it will be returned as -1 (meaning not available) in the case of dynamic
         * errors. Column numbers start at 0. For expressions held within XML attributes,
         * the position is within the attribute after XML attribute-value normalization,
         * which replaces newlines by spaces and expands entity references.
         */

        public int getColumnNumber() {
            return localColumnNumber;
        }

        /**
         * Get the system identifier of the expression's container. This will normally
         * be the URI of the document (or external entity) in which the expression appears.
         *
         * @return the system identifier of the expression's container, or null if not known
         */

        public String getSystemId() {
            return containingLocation.getSystemId();
        }

        /**
         * Get the public identifier. This will normally be null, but is provided for
         * compatibility with SAX and JAXP interfaces
         *
         * @return the public identifier - usually null
         */

        public String getPublicId() {
            return containingLocation.getPublicId();
        }

        /**
         * Get the local line number, that is the line number relative to the start of the
         * expression or query. For expressions held within XML attributes,
         * the position is within the attribute after XML attribute-value normalization,
         * which replaces newlines by spaces and expands entity references; the value
         * will therefore in many cases not be usable. Local line numbers start at 0.
         *
         * @return the local line number within the expression or query. Set to -1
         * if not known.
         */

        public int getLocalLineNumber() {
            return localLineNumber;
        }

        /**
         * Get the line number within the containing entity. This is the sum of the containing
         * location's line number, plus the local line number. Returns -1 if unknown.
         *
         * @return the line number within the containing entity, or -1 if unknown.
         */

        public int getLineNumber() {
            return containingLocation.getLineNumber() + localLineNumber;
        }

        /**
         * Get text appearing near to the error (typically a syntax error) within the source
         * text of the expression or query.
         *
         * @return nearby text to the error. May be null.
         */

        public String getNearbyText() {
            return nearbyText;
        }

        /**
         * Save an immutable copy of the location information. This implementation does
         * nothing, because the object is already immutable
         *
         * @return immutable location information.
         */

        public Location saveLocation() {
            return this;
        }
    }

}

/*

The following copyright notice is copied from the licence for xt, from which the
original version of this module was derived:
--------------------------------------------------------------------------------
Copyright (c) 1998, 1999 James Clark

Permission is hereby granted, free of charge, to any person obtaining
a copy of this software and associated documentation files (the
"Software"), to deal in the Software without restriction, including
without limitation the rights to use, copy, modify, merge, publish,
distribute, sublicense, and/or sell copies of the Software, and to
permit persons to whom the Software is furnished to do so, subject to
the following conditions:

The above copyright notice and this permission notice shall be included
in all copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED ``AS IS'', WITHOUT WARRANTY OF ANY KIND, EXPRESS
OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT.
IN NO EVENT SHALL JAMES CLARK BE LIABLE FOR ANY CLAIM, DAMAGES OR
OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE,
ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR
OTHER DEALINGS IN THE SOFTWARE.

Except as contained in this notice, the name of James Clark shall
not be used in advertising or otherwise to promote the sale, use or
other dealings in this Software without prior written authorization
from James Clark.
---------------------------------------------------------------------------
*/