package net.sf.saxon.query;

import net.sf.saxon.Configuration;
import net.sf.saxon.Err;
import net.sf.saxon.Platform;
import net.sf.saxon.event.PipelineConfiguration;
import net.sf.saxon.expr.*;
import net.sf.saxon.functions.*;
import net.sf.saxon.instruct.*;
import net.sf.saxon.om.*;
import net.sf.saxon.pattern.NodeTest;
import net.sf.saxon.sort.*;
import net.sf.saxon.style.AttributeValueTemplate;
import net.sf.saxon.style.StandardNames;
import net.sf.saxon.trace.Location;
import net.sf.saxon.trans.DynamicError;
import net.sf.saxon.trans.IndependentContext;
import net.sf.saxon.trans.StaticError;
import net.sf.saxon.trans.XPathException;
import net.sf.saxon.type.ItemType;
import net.sf.saxon.type.SchemaType;
import net.sf.saxon.type.Type;
import net.sf.saxon.type.TypeHierarchy;
import net.sf.saxon.value.*;

import javax.xml.transform.OutputKeys;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.stream.StreamSource;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.*;
import java.util.regex.Pattern;

/**
 * This class defines extensions to the XPath parser to handle the additional
 * syntax supported in XQuery
 */
class QueryParser extends ExpressionParser {

    private boolean preserveSpace = false;
    private boolean defaultEmptyLeast = true;
    private boolean memoFunction = false;

    private int errorCount = 0;
    private StaticError firstError = null;

    protected Executable executable;

    private boolean foundCopyNamespaces = false;
    private boolean foundBoundarySpaceDeclaration = false;
    private boolean foundOrderingDeclaration = false;
    private boolean foundEmptyOrderingDeclaration = false;
    private boolean foundDefaultCollation = false;
    private boolean foundConstructionDeclaration = false;
    private boolean foundDefaultFunctionNamespace = false;
    private boolean foundDefaultElementNamespace = false;
    private boolean foundBaseURIDeclaration = false;
    private boolean preambleProcessed = false;

    public Set importedModules = new HashSet(5);
    List namespacesToBeSealed = new ArrayList(10);
    List schemaImports = new ArrayList(5);
    List moduleImports = new ArrayList(5);

    private Expression defaultValue = null;

    /**
     * Protected Constructor: this class should be instantiated via the StaticQueryContext
     */

    protected QueryParser() {
    };

    /**
     * Create an XQueryExpression
     */

    public XQueryExpression makeXQueryExpression(String query,
                                                 StaticQueryContext staticContext,
                                                 Configuration config) throws XPathException {
        try {
            if (config.getXMLVersion() == Configuration.XML10) {
                query = normalizeLineEndings10(query);
            } else {
                query = normalizeLineEndings11(query);
            }
            Executable exec = new Executable();

            Properties outputProps = new Properties();
            outputProps.setProperty(OutputKeys.METHOD, "xml");
            outputProps.setProperty(OutputKeys.INDENT, "yes");
            exec.setDefaultOutputProperties(outputProps);

            exec.setLocationMap(new LocationMap());
            exec.setConfiguration(config);
            exec.setFunctionLibrary(new ExecutableFunctionLibrary(config));
            // this will be changed later
            exec.setHostLanguage(Configuration.XQUERY);
            setExecutable(exec);
            staticContext.setExecutable(exec);
            Expression exp = parseQuery(query, 0, Token.EOF, staticContext);
            if (exp instanceof ComputedExpression) {
                int loc = env.getLocationMap().allocateLocationId(env.getSystemId(), 1);
                ((ComputedExpression)exp).setParentExpression(new TemporaryContainer(staticContext.getLocationMap(), loc));
            }
            staticContext.bindUnboundFunctionCalls();
            exec.fixupQueryModules(staticContext);
            XQueryExpression queryExp = new XQueryExpression(exp, exec, staticContext, config);
            exp = queryExp.getExpression();
//            DocumentInstr docInstruction;
//            if (exp instanceof DocumentInstr) {
//                docInstruction = (DocumentInstr)exp;
//            } else {
//                docInstruction = new DocumentInstr(false, null, staticContext.getSystemId());
//                docInstruction.setContentExpression(exp);
//                setLocation(docInstruction, 1);
//                //makeContentConstructor(exp, docInstruction, 1);
//            }
//            queryExp.setDocumentInstruction(docInstruction);

            // Make the function library that's available at run-time (e.g. for saxon:evaluate()). This includes
            // all user-defined functions regardless of which module they are in

            FunctionLibrary userlib = exec.getFunctionLibrary();
            FunctionLibraryList lib = new FunctionLibraryList();
            lib.addFunctionLibrary(new SystemFunctionLibrary(SystemFunctionLibrary.XPATH_ONLY));
            lib.addFunctionLibrary(config.getVendorFunctionLibrary());
            lib.addFunctionLibrary(new ConstructorFunctionLibrary(config));
            if (config.isAllowExternalFunctions()) {
                config.getPlatform().addFunctionLibraries(lib, config);
            }
            lib.addFunctionLibrary(userlib);
            exec.setFunctionLibrary(lib);

            return queryExp;
        } catch (XPathException e) {
            if (!e.hasBeenReported()) {
                e = StaticError.makeStaticError(e);
                reportError((StaticError)e);
            }
            throw e;
        }
    }

    /**
     * Normalize line endings in the source query, according to the XML 1.1 rules.
     */

    private static String normalizeLineEndings11(String in) {
        if (in.indexOf((char)0xa) < 0 && in.indexOf((char)0x85) < 0 && in.indexOf((char)0x2028) < 0) {
            return in;
        }
        FastStringBuffer sb = new FastStringBuffer(in.length());
        for (int i = 0; i < in.length(); i++) {
            char ch = in.charAt(i);
            switch (ch) {
                case 0x85:
                case 0x2028:
                    sb.append((char)0xa);
                    break;
                case 0xd:
                    if (i < in.length() - 1 && (in.charAt(i + 1) == (char)0xa || in.charAt(i + 1) == (char)0x85)) {
                        sb.append((char)0xa);
                        i++;
                    } else {
                        sb.append((char)0xa);
                    }
                    break;
                default:
                    sb.append(ch);
            }
        }
        return sb.toString();
    }

    /**
     * Normalize line endings in the source query, according to the XML 1.0 rules.
     */

    private static String normalizeLineEndings10(String in) {
        if (in.indexOf((char)0xa) < 0) {
            return in;
        }
        FastStringBuffer sb = new FastStringBuffer(in.length());
        for (int i = 0; i < in.length(); i++) {
            char ch = in.charAt(i);
            switch (ch) {
                case 0xd:
                    if (i < in.length() - 1 && in.charAt(i + 1) == (char)0xa) {
                        sb.append((char)0xa);
                        i++;
                    } else {
                        sb.append((char)0xa);
                    }
                    break;
                default:
                    sb.append(ch);
            }
        }
        return sb.toString();
    }


    /**
     * Get the executable containing this expression.
     */

    public Executable getExecutable() {
        return executable;
    }

    /**
     * Set the executable used for this query expression
     */

    public void setExecutable(Executable exec) {
        executable = exec;
    }


    /**
     * Parse a top-level Query.
     * Prolog? Expression
     *
     * @param queryString The text of the query
     * @param start       Offset of the start of the query
     * @param terminator  Token expected to follow the query (usually Token.EOF)
     * @param env         The static context
     * @return the Expression object that results from parsing
     * @throws net.sf.saxon.trans.XPathException
     *          if the expression contains a syntax error
     */

    private Expression parseQuery(String queryString,
                                  int start,
                                  int terminator,
                                  StaticQueryContext env) throws XPathException {
        this.env = env;
        this.nameChecker = env.getConfiguration().getNameChecker();
        this.language = XQUERY;
        t = new Tokenizer();
        try {
            t.tokenize(queryString, start, -1, 1);
        } catch (StaticError err) {
            grumble(err.getMessage());
        }
        parseVersionDeclaration();
        parseProlog();
        processPreamble();
        Expression exp = parseExpression();
        if (t.currentToken != terminator) {
            grumble("Unexpected token " + currentTokenDisplay() + " beyond end of query");
        }
        setLocation(exp);
        if (errorCount == 0) {
            return exp;
        } else {
            StaticError err = new StaticError("One or more static errors were reported during query analysis");
            err.setHasBeenReported();
            err.setErrorCode(firstError.getErrorCodeLocalPart());   // largely for the XQTS test driver
            throw err;
        }
    }

    /**
     * Parse a library module.
     * Prolog? Expression
     *
     * @param queryString The text of the library module.
     * @param env         The static context. The result of parsing
     *                    a library module is that the static context is populated with a set of function
     *                    declarations and variable declarations. Each library module must have its own
     *                    static context objext.
     * @throws net.sf.saxon.trans.StaticError if the expression contains a syntax error
     */

    public final void parseLibraryModule(String queryString, StaticQueryContext env)
            throws StaticError {
        this.env = env;
        this.nameChecker = env.getConfiguration().getNameChecker();
        this.executable = env.getExecutable();
        t = new Tokenizer();
        try {
            t.tokenize(queryString, 0, -1, 1);
        } catch (StaticError err) {
            grumble(err.getMessage());
        }
        parseVersionDeclaration();
        parseModuleDeclaration();
        parseProlog();
        processPreamble();
        if (t.currentToken != Token.EOF) {
            grumble("Unrecognized content found after the variable and function declarations in a library module");
        }
        if (errorCount != 0) {
            StaticError err = new StaticError("Static errors were reported in the imported library module");
            err.setErrorCode(firstError.getErrorCodeLocalPart());
            throw err;
        }
    }

    /**
     * Report a static error
     *
     * @param message the error message
     * @throws net.sf.saxon.trans.StaticError always thrown: an exception containing the
     *                                        supplied message
     */

    protected void grumble(String message, String errorCode) throws StaticError {
        String s = t.recentText();
        ExpressionLocation loc = makeLocator();
        String prefix = getLanguage() +
                ("XPST0003".equals(errorCode) ? " syntax error " : " static error ") +
                (message.startsWith("...") ? "near" : "in") +
                " #" + s + "#:\n    ";
        StaticError exception = new StaticError(prefix + message);
        exception.setErrorCode(errorCode);
        exception.setLocator(loc);
        reportError(exception);
    }

    private void reportError(StaticError exception) throws StaticError {
        errorCount++;
        if (firstError == null) {
            firstError = exception;
        }
        ((StaticQueryContext)env).reportFatalError(exception);
        throw exception;
    }

    /**
     * Make a Locator object representing the current parsing location
     *
     * @return a Locator
     */
    private ExpressionLocation makeLocator() {
        int line = t.getLineNumber();
        int column = t.getColumnNumber();

        ExpressionLocation loc = new ExpressionLocation();
        loc.setSystemId(env.getSystemId());
        loc.setLineNumber(line);
        loc.setColumnNumber(column);
        return loc;
    }

    private static Pattern encNamePattern = Pattern.compile("^[A-Za-z]([A-Za-z0-9._\\x2D])*$");

    /**
     * Parse the version declaration if present.
     *
     * @throws net.sf.saxon.trans.StaticError in the event of a syntax error.
     */
    private void parseVersionDeclaration() throws StaticError {
        if (t.currentToken == Token.XQUERY_VERSION) {
            nextToken();
            expect(Token.STRING_LITERAL);
            if (!("1.0".equals(t.currentTokenValue))) {
                grumble("XQuery version must be 1.0", "XQST0031");
            }
            nextToken();
            if ("encoding".equals(t.currentTokenValue)) {
                nextToken();
                expect(Token.STRING_LITERAL);
                if (!encNamePattern.matcher(t.currentTokenValue).matches()) {
                    grumble("Encoding name contains invalid characters", "XQST0087");
                }
                // we ignore the encoding now: it was handled earlier, while decoding the byte stream
                nextToken();
            }
            expect(Token.SEMICOLON);
            nextToken();
        }
    }

    /**
     * In a library module, parse the module declaration
     * Syntax: <"module" "namespace"> prefix "=" uri ";"
     *
     * @throws net.sf.saxon.trans.StaticError in the event of a syntax error.
     */

    private void parseModuleDeclaration() throws StaticError {
        expect(Token.MODULE_NAMESPACE);
        nextToken();
        expect(Token.NAME);
        String prefix = t.currentTokenValue;
        checkProhibitedPrefixes(prefix);
        nextToken();
        expect(Token.EQUALS);
        nextToken();
        expect(Token.STRING_LITERAL);
        String uri = URILiteral(t.currentTokenValue);
        if (uri.equals("")) {
            grumble("Module namespace cannot be \"\"", "XQST0088");
            uri = "http://saxon.fallback.namespace/";   // for error recovery
        }
        nextToken();
        expect(Token.SEMICOLON);
        nextToken();
        try {
            ((StaticQueryContext)env).declarePassiveNamespace(prefix, uri, true);
        } catch (StaticError err) {
            err.setLocator(makeLocator());
            reportError(err);
        }
        ((StaticQueryContext)env).setModuleNamespace(uri);
    }

    /**
     * Parse the query prolog. This method, and its subordinate methods which handle
     * individual declarations in the prolog, cause the static context to be updated
     * with relevant context information. On exit, t.currentToken is the first token
     * that is not recognized as being part of the prolog.
     *
     * @throws net.sf.saxon.trans.StaticError in the event of a syntax error.
     */

    private void parseProlog() throws StaticError {
        //boolean allowSetters = true;
        boolean allowModuleDecl = true;
        boolean allowDeclarations = true;

        while (true) {
            try {
                if (t.currentToken == Token.MODULE_NAMESPACE) {
                    String uri = ((StaticQueryContext)env).getModuleNamespace();
                    if (uri == null) {
                        grumble("Module declaration must not be used in a main module");
                    } else {
                        grumble("Module declaration appears more than once");
                    }
                    if (!allowModuleDecl) {
                        grumble("Module declaration must precede other declarations in the query prolog");
                    }
                }
                allowModuleDecl = false;
                switch (t.currentToken) {
                    case Token.DECLARE_NAMESPACE:
                        if (!allowDeclarations) {
                            grumble("Namespace declarations cannot follow variables, functions, or options");
                        }
                        //allowSetters = false;
                        parseNamespaceDeclaration();
                        break;
                    case Token.DECLARE_DEFAULT:
                        nextToken();
                        expect(Token.NAME);
                        if (t.currentTokenValue == "element") {
                            if (!allowDeclarations) {
                                grumble("Namespace declarations cannot follow variables, functions, or options");
                            }
                            //allowSetters = false;
                            parseDefaultElementNamespace();
                        } else if (t.currentTokenValue == "function") {
                            if (!allowDeclarations) {
                                grumble("Namespace declarations cannot follow variables, functions, or options");
                            }
                            //allowSetters = false;
                            parseDefaultFunctionNamespace();
                        } else if (t.currentTokenValue == "collation") {
                            if (!allowDeclarations) {
                                grumble("Collation declarations must appear earlier in the prolog");
                            }
                            parseDefaultCollation();
                        } else if (t.currentTokenValue == "order") {
                            if (!allowDeclarations) {
                                grumble("Order declarations must appear earlier in the prolog");
                            }
                            parseDefaultOrder();
                        } else {
                            grumble("After 'declare default', expected 'element', 'function', or 'collation'");
                        }
                        break;
                    case Token.DECLARE_BOUNDARY_SPACE:
                        if (!allowDeclarations) {
                            grumble("'declare boundary-space' must appear earlier in the query prolog");
                        }
                        parseBoundarySpaceDeclaration();
                        break;
                    case Token.DECLARE_ORDERING:
                        if (!allowDeclarations) {
                            grumble("'declare ordering' must appear earlier in the query prolog");
                        }
                        parseOrderingDeclaration();
                        break;
                    case Token.DECLARE_COPY_NAMESPACES:
                        if (!allowDeclarations) {
                            grumble("'declare copy-namespaces' must appear earlier in the query prolog");
                        }
                        parseCopyNamespacesDeclaration();
                        break;
                    case Token.DECLARE_BASEURI:
                        if (!allowDeclarations) {
                            grumble("'declare base-uri' must appear earlier in the query prolog");
                        }
                        parseBaseURIDeclaration();
                        break;
                    case Token.IMPORT_SCHEMA:
                        //allowSetters = false;
                        if (!allowDeclarations) {
                            grumble("Import schema must appear earlier in the prolog");
                        }
                        parseSchemaImport();
                        break;
                    case Token.IMPORT_MODULE:
                        //allowSetters = false;
                        if (!allowDeclarations) {
                            grumble("Import module must appear earlier in the prolog");
                        }
                        parseModuleImport();
                        break;
                    case Token.DECLARE_VARIABLE:
                        //allowSetters = false;
                        if (allowDeclarations) {
                            sealNamespaces(namespacesToBeSealed, env.getConfiguration());
                            allowDeclarations = false;
                        }
                        processPreamble();
                        parseVariableDeclaration();
                        break;
                    case Token.DECLARE_FUNCTION:
                        //allowSetters = false;
                        if (allowDeclarations) {
                            sealNamespaces(namespacesToBeSealed, env.getConfiguration());
                            allowDeclarations = false;
                        }
                        processPreamble();
                        parseFunctionDeclaration();
                        break;
                    case Token.DECLARE_OPTION:
                        //allowSetters = false;
                        if (allowDeclarations) {
                            sealNamespaces(namespacesToBeSealed, env.getConfiguration());
                            allowDeclarations = false;
                        }
                        processPreamble();
                        parseOptionDeclaration();
                        break;
                    case Token.DECLARE_CONSTRUCTION:
                        if (!allowDeclarations) {
                            grumble("'declare construction' must appear earlier in the query prolog");
                        }
                        parseConstructionDeclaration();
                        break;
                    default:
                        return;
                }
                expect(Token.SEMICOLON);
                nextToken();
            } catch (StaticError err) {
                if (err.getLocator() == null) {
                    err.setLocator(makeLocator());
                }
                if (!err.hasBeenReported()) {
                    errorCount++;
                    if (firstError == null) {
                        firstError = err;
                    }
                    ((StaticQueryContext)env).reportFatalError(err);
                }
                // we've reported an error, attempt to recover by skipping to the
                // next semicolon
                while (t.currentToken != Token.SEMICOLON) {
                    nextToken();
                    if (t.currentToken == Token.EOF) {
                        return;
                    } else if (t.currentToken == Token.RCURLY) {
                        t.lookAhead();
                    } else if (t.currentToken == Token.TAG) {
                        parsePseudoXML(true);
                    }
                }
                nextToken();
            }
        }
    }

    private void sealNamespaces(List namespacesToBeSealed, Configuration config) {
        for (Iterator iter = namespacesToBeSealed.iterator(); iter.hasNext();) {
            String ns = (String)iter.next();
            config.sealNamespace(ns);
        }
    }

    /**
     * Method called once the setters have been read to do tidying up that can't be done until we've got
     * to the end
     *
     * @throws StaticError
     */

    private void processPreamble() throws StaticError {
        if (preambleProcessed) {
            return;
        }
        preambleProcessed = true;
        if (foundDefaultCollation) {
            String collationName = env.getDefaultCollationName();
            URI collationURI;
            try {
                collationURI = new URI(collationName);
                if (!collationURI.isAbsolute()) {
                    URI base = new URI(env.getBaseURI());
                    collationURI = base.resolve(collationURI);
                    collationName = collationURI.toString();
                }
            } catch (URISyntaxException err) {
                grumble("Default collation name '" + collationName + "' is not a valid URI");
                collationName = NamespaceConstant.CODEPOINT_COLLATION_URI;
            }
            if (env.getCollation(collationName) == null) {
                grumble("Default collation name '" + collationName + "' is not a recognized collation", "XQST0038");
                collationName = NamespaceConstant.CODEPOINT_COLLATION_URI;
            }
            ((StaticQueryContext)env).declareDefaultCollation(collationName);
        }
        for (Iterator iter = schemaImports.iterator(); iter.hasNext();) {
            Import imp = (Import)iter.next();
            applySchemaImport(imp);
        }
        for (Iterator iter = moduleImports.iterator(); iter.hasNext();) {
            Import imp = (Import)iter.next();
            applyModuleImport(imp);
        }
    }

    private void parseDefaultCollation() throws StaticError {
        // <"default" "collation"> StringLiteral
        if (foundDefaultCollation) {
            grumble("default collation appears more than once", "XQST0038");
        }
        foundDefaultCollation = true;
        nextToken();
        expect(Token.STRING_LITERAL);
        String uri = URILiteral(t.currentTokenValue);
        ((StaticQueryContext)env).declareDefaultCollation(uri);
        nextToken();
    }

    /**
     * parse "declare default order empty (least|greatest)"
     */
    private void parseDefaultOrder() throws StaticError {
        if (foundEmptyOrderingDeclaration) {
            grumble("empty ordering declaration appears more than once", "XQST0069");
        }
        foundEmptyOrderingDeclaration = true;
        nextToken();
        if (!isKeyword("empty")) {
            grumble("After 'declare default order', expected keyword 'empty'");
        }
        nextToken();
        if (isKeyword("least")) {
            defaultEmptyLeast = true;
        } else if (isKeyword("greatest")) {
            defaultEmptyLeast = false;
        } else {
            grumble("After 'declare default order empty', expected keyword 'least' or 'greatest'");
        }
        nextToken();
    }

    /**
     * Parse the "declare xmlspace" declaration.
     * Syntax: <"declare" "boundary-space"> ("preserve" | "strip")
     *
     * @throws net.sf.saxon.trans.StaticError
     */

    private void parseBoundarySpaceDeclaration() throws StaticError {
        if (foundBoundarySpaceDeclaration) {
            grumble("'declare boundary-space' appears more than once", "XQST0068");
        }
        foundBoundarySpaceDeclaration = true;
        nextToken();
        expect(Token.NAME);
        if ("preserve".equals(t.currentTokenValue)) {
            preserveSpace = true;
        } else if ("strip".equals(t.currentTokenValue)) {
            preserveSpace = false;
        } else {
            grumble("boundary-space must be 'preserve' or 'strip'");
        }
        nextToken();
    }

    /**
     * Parse the "declare ordering" declaration.
     * Syntax: <"declare" "ordering"> ("ordered" | "unordered")
     *
     * @throws net.sf.saxon.trans.StaticError
     */

    private void parseOrderingDeclaration() throws StaticError {
        if (foundOrderingDeclaration) {
            grumble("ordering mode declaration appears more than once", "XQST0065");
        }
        foundOrderingDeclaration = true;
        nextToken();
        expect(Token.NAME);
        if ("ordered".equals(t.currentTokenValue)) {
            // no action
        } else if ("unordered".equals(t.currentTokenValue)) {
            // no action
        } else {
            grumble("ordering mode must be 'ordered' or 'unordered'");
        }
        nextToken();
    }

    /**
     * Parse the "declare copy-namespaces" declaration.
     * Syntax: <"declare" "copy-namespaces"> ("preserve" | "no-preserve") "," ("inherit" | "no-inherit")
     *
     * @throws net.sf.saxon.trans.StaticError
     */

    private void parseCopyNamespacesDeclaration() throws StaticError {
        if (foundCopyNamespaces) {
            grumble("declare inherit-namespaces appears more than once", "XQST0055");
        }
        foundCopyNamespaces = true;
        nextToken();
        expect(Token.NAME);
        if ("preserve".equals(t.currentTokenValue)) {
            ((StaticQueryContext)env).setPreserveNamespaces(true);
        } else if ("no-preserve".equals(t.currentTokenValue)) {
            ((StaticQueryContext)env).setPreserveNamespaces(false);
        } else {
            grumble("copy-namespaces must be followed by 'preserve' or 'no-preserve'");
        }
        nextToken();
        expect(Token.COMMA);
        nextToken();
        expect(Token.NAME);
        if ("inherit".equals(t.currentTokenValue)) {
            ((StaticQueryContext)env).setInheritNamespaces(true);
        } else if ("no-inherit".equals(t.currentTokenValue)) {
            ((StaticQueryContext)env).setInheritNamespaces(false);
        } else {
            grumble("After the comma in the copy-namespaces declaration, expected 'inherit' or 'no-inherit'");
        }
        nextToken();
    }


    /**
     * Parse the "declare construction" declaration.
     * Syntax: <"declare" "construction"> ("preserve" | "strip")
     *
     * @throws net.sf.saxon.trans.StaticError
     */

    private void parseConstructionDeclaration() throws StaticError {
        if (foundConstructionDeclaration) {
            grumble("declare construction appears more than once", "XQST0067");
        }
        foundConstructionDeclaration = true;
        nextToken();
        expect(Token.NAME);
        int val;
        if ("preserve".equals(t.currentTokenValue)) {
            val = Validation.PRESERVE;
        } else if ("strip".equals(t.currentTokenValue)) {
            val = Validation.STRIP;
        } else {
            grumble("construction mode must be 'preserve' or 'strip'");
            val = Validation.STRIP;
        }
        ((StaticQueryContext)env).setConstructionMode(val);
        nextToken();
    }

    /**
     * Parse (and process) the schema import declaration.
     * SchemaImport ::=	"import" "schema" SchemaPrefix? URILiteral ("at" URILiteral ("," URILiteral)*)?
     * SchemaPrefix ::=	("namespace" NCName "=") | ("default" "element" "namespace")
     */

    private void parseSchemaImport() throws StaticError {
        if (!env.getConfiguration().isSchemaAware(Configuration.XQUERY)) {
            grumble("To import a schema, you need the schema-aware version of Saxon", "XQST0009");
        }
        Import sImport = new Import();
        String prefix = null;
        sImport.namespaceURI = null;
        sImport.locationURIs = new ArrayList(5);
        nextToken();
        if (isKeyword("namespace")) {
            nextToken();
            expect(Token.NAME);
            prefix = t.currentTokenValue;
            checkProhibitedPrefixes(prefix);
            nextToken();
            expect(Token.EQUALS);
            nextToken();
        } else if (isKeyword("default")) {
            nextToken();
            if (!isKeyword("element")) {
                grumble("In 'import schema', expected 'element namespace'");
            }
            nextToken();
            if (!isKeyword("namespace")) {
                grumble("In 'import schema', expected keyword 'namespace'");
            }
            nextToken();
            prefix = "";
        }
        if (t.currentToken == Token.STRING_LITERAL) {
            sImport.namespaceURI = URILiteral(t.currentTokenValue);
            nextToken();
            if (isKeyword("at")) {
                nextToken();
                expect(Token.STRING_LITERAL);
                sImport.locationURIs.add(URILiteral(t.currentTokenValue));
                nextToken();
                while (t.currentToken == Token.COMMA) {
                    nextToken();
                    expect(Token.STRING_LITERAL);
                    sImport.locationURIs.add(URILiteral(t.currentTokenValue));
                    nextToken();
                }
            } else if (t.currentToken != Token.SEMICOLON) {
                grumble("After the target namespace URI, expected 'at' or ';'");
            }
        } else {
            grumble("After 'import schema', expected 'namespace', 'default', or a string-literal");
        }
        if (prefix != null) {
            try {
                if ("".equals(prefix)) {
                    ((StaticQueryContext)env).setDefaultElementNamespace(sImport.namespaceURI);
                } else {
                    if (sImport.namespaceURI == null || "".equals(sImport.namespaceURI)) {
                        grumble("A prefix cannot be bound to the null namespace", "XQST0057");
                    }
                    ((StaticQueryContext)env).declarePassiveNamespace(prefix, sImport.namespaceURI, true);
                }
            } catch (StaticError err) {
                err.setLocator(makeLocator());
                reportError(err);
            }
        }
        for (Iterator iter = schemaImports.iterator(); iter.hasNext();) {
            Import imp = (Import)iter.next();
            if (imp.namespaceURI.equals(sImport.namespaceURI)) {
                grumble("Schema namespace '" + sImport.namespaceURI + "' is imported more than once", "XQST0058");
                break;
            }
        }

        schemaImports.add(sImport);

    }

    private void applySchemaImport(Import sImport) throws StaticError {

        // Do the importing

        Configuration config = env.getConfiguration();
        if (!config.isSchemaAvailable(sImport.namespaceURI)) {
            if (sImport.locationURIs.size() > 0) {
                try {
                    PipelineConfiguration pipe = config.makePipelineConfiguration();
                    config.readMultipleSchemas(pipe, env.getBaseURI(), sImport.locationURIs, sImport.namespaceURI);
                    namespacesToBeSealed.add(sImport.namespaceURI);
                } catch (TransformerConfigurationException err) {
                    grumble("Error in schema. " + err.getMessage(), "XQST0059");
                }
            } else {
                grumble("Unable to locate requested schema", "XQST0059");
            }
        }
        ((StaticQueryContext)env).addImportedSchema(sImport.namespaceURI);
    }

    /**
     * Parse (and expand) the module import declaration.
     * Syntax: <"import" "module" ("namespace" NCName "=")? uri ("at" uri ("," uri)*)? ";"
     */

    private void parseModuleImport() throws StaticError {
        StaticQueryContext thisModule = (StaticQueryContext)env;
        Import mImport = new Import();
        String prefix = null;
        mImport.namespaceURI = null;
        mImport.locationURIs = new ArrayList(5);
        nextToken();
        if (t.currentToken == Token.NAME && t.currentTokenValue == "namespace") {
            nextToken();
            expect(Token.NAME);
            prefix = t.currentTokenValue;
            checkProhibitedPrefixes(prefix);
            nextToken();
            expect(Token.EQUALS);
            nextToken();
        }
        if (t.currentToken == Token.STRING_LITERAL) {
            mImport.namespaceURI = URILiteral(t.currentTokenValue);
            if (mImport.namespaceURI.equals("")) {
                grumble("Imported module namespace cannot be \"\"", "XQST0088");
                mImport.namespaceURI = "http://saxon.fallback.namespace/line" + t.getLineNumber();   // for error recovery
            }
            if (importedModules.contains(mImport.namespaceURI)) {
                grumble("Two 'import module' declarations specify the same module namespace", "XQST0047");
            }
            importedModules.add(mImport.namespaceURI);
            ((StaticQueryContext)env).addImportedNamespace(mImport.namespaceURI);
            nextToken();
            if (isKeyword("at")) {
                do {
                    nextToken();
                    expect(Token.STRING_LITERAL);
                    String uri = URILiteral(t.currentTokenValue);
                    mImport.locationURIs.add(uri);
                    nextToken();
                } while (t.currentToken == Token.COMMA);
            }
        } else {
            grumble("After 'import module', expected 'namespace' or a string-literal");
        }
        if (prefix != null) {
            try {
                thisModule.declarePassiveNamespace(prefix, mImport.namespaceURI, true);
            } catch (StaticError err) {
                err.setLocator(makeLocator());
                reportError(err);
            }
        }

        // Check that this import would not create a cycle involving a change of namespace
        if (!mImport.namespaceURI.equals(((StaticQueryContext)env).getModuleNamespace())) {
            StaticQueryContext parent = (StaticQueryContext)env;
            if (!parent.mayImport(mImport.namespaceURI)) {
                StaticError err = new StaticError("A module cannot import itself directly or indirectly, unless all modules in the cycle are in the same namespace");
                err.setErrorCode("XQST0073");
                throw err;
            }
        }

        moduleImports.add(mImport);
    }

    public void applyModuleImport(Import mImport) throws StaticError {
        boolean foundOne = false;

        // resolve the location URIs against the base URI
        Platform platform = env.getConfiguration().getPlatform();
        for (int i=0; i<mImport.locationURIs.size(); i++) {
            try {
                String uri = (String)mImport.locationURIs.get(i);
                uri = platform.makeAbsolute(uri, env.getBaseURI()).toString();
                //System.err.println("Absolute module URI = " + uri);
                mImport.locationURIs.set(i, uri);
            } catch (URISyntaxException e) {
                grumble("Invalid URI " + mImport.locationURIs.get(i) + ": " + e.getMessage());
            }
        }

        // If any of the modules are already loaded, don't re-read them; but do check that none of them
        // references the current module namespace directly or indirectly
        List existingModules = executable.getQueryLibraryModules(mImport.namespaceURI);
        if (existingModules != null) {
            //System.err.println("Number of existing modules: " + existingModules.size());
            for (int m = 0; m < existingModules.size(); m++) {
                StaticQueryContext importedModule = (StaticQueryContext)existingModules.get(m);
                //System.err.println("Existing module location URI =  " + importedModule.getLocationURI());
                if (!importedModule.getLocationURI().equals(((StaticQueryContext)env).getLocationURI())) {
                    //QueryReader.importModuleContents(importedModule, (StaticQueryContext)env);
                    foundOne = true;
                }
                if (((StaticQueryContext)env).getModuleNamespace() != null &&
                        !((StaticQueryContext)env).getModuleNamespace().equals(importedModule.getModuleNamespace()) &&
                        importedModule.importsNamespaceIndirectly(((StaticQueryContext)env).getModuleNamespace())) {
                    grumble("A cycle exists among the module imports, involving namespaces " +
                            ((StaticQueryContext)env).getModuleNamespace() + " and " +
                            importedModule.getModuleNamespace());
                }
                for (int h = mImport.locationURIs.size() - 1; h >= 0; h--) {
                    if (mImport.locationURIs.get(h).equals(importedModule.getLocationURI())) {
                        mImport.locationURIs.remove(h);
                    }
                }
            }
        }

        // If we've found at least one module, and there are no location URIs left, call it a day.

        if (mImport.locationURIs.size() == 0 && foundOne) {
            return;
        }

        // Call the module URI resolver to find the remaining modules

        ModuleURIResolver resolver = ((StaticQueryContext)env).getModuleURIResolver();

        String[] hints = new String[mImport.locationURIs.size()];
        hints = (String[])mImport.locationURIs.toArray(hints);
        StreamSource[] sources = null;
        try {
            if (resolver != null) {
                sources = resolver.resolve(mImport.namespaceURI, env.getBaseURI(), hints);
            }
            if (sources == null) {
                if (hints.length == 0) {
                    if (existingModules == null) {
                        grumble("Cannot locate module for namespace " + mImport.namespaceURI, "XQST0059");
                    }
                }
                resolver = env.getConfiguration().getStandardModuleURIResolver();
                sources = resolver.resolve(mImport.namespaceURI, env.getBaseURI(), hints);
            }
        } catch (XPathException e) {
            throw StaticError.makeStaticError(e);
        }

        for (int m = 0; m < sources.length; m++) {
            StreamSource ss = sources[m];
            String baseURI = ss.getSystemId();
            if (baseURI == null) {
                if (m < hints.length) {
                    ss.setSystemId(hints[m]);
                } else {
                    grumble("No base URI available for imported module", "XQST0059");
                }
            }
            try {
                String queryText = QueryReader.readSourceQuery(ss, nameChecker);
                StaticQueryContext importedModule = StaticQueryContext.makeStaticQueryContext(
                        baseURI, executable, (StaticQueryContext)env, queryText, mImport.namespaceURI);
                //QueryReader.importModuleContents(importedModule, (StaticQueryContext)env);
            } catch (StaticError err) {
                if (err.getLocator() == null) {
                    err.setLocator(makeLocator());
                }
                reportError(err);
            }
        }
    }

    /**
     * Parse the Base URI declaration.
     * Syntax: <"declare" "base-uri"> uri-literal
     *
     * @throws net.sf.saxon.trans.StaticError
     */

    private void parseBaseURIDeclaration() throws StaticError {
        if (foundBaseURIDeclaration) {
            grumble("Base URI Declaration may only appear once", "XQST0032");
        }
        foundBaseURIDeclaration = true;
        nextToken();
        expect(Token.STRING_LITERAL);
        String uri = URILiteral(t.currentTokenValue);
        ((StaticQueryContext)env).setBaseURI(uri);
        nextToken();
    }

    /**
     * Parse the "default function namespace" declaration.
     * Syntax: <"declare" "default" "function" "namespace"> StringLiteral
     *
     * @throws net.sf.saxon.trans.StaticError to indicate a syntax error
     */

    private void parseDefaultFunctionNamespace() throws StaticError {
        if (foundDefaultFunctionNamespace) {
            grumble("default function namespace appears more than once", "XQST0066");
        }
        foundDefaultFunctionNamespace = true;
        nextToken();
        expect(Token.NAME);
        if (!"namespace".equals(t.currentTokenValue)) {
            grumble("After 'declare default function', expected 'namespace'");
        }
        nextToken();
        expect(Token.STRING_LITERAL);
        String uri = URILiteral(t.currentTokenValue);
        ((StaticQueryContext)env).setDefaultFunctionNamespace(uri);
        nextToken();
    }

    /**
     * Parse the "default element namespace" declaration.
     * Syntax: <"declare" "default" "element" "namespace"> StringLiteral
     *
     * @throws net.sf.saxon.trans.StaticError to indicate a syntax error
     */

    private void parseDefaultElementNamespace() throws StaticError {
        if (foundDefaultElementNamespace) {
            grumble("default element namespace appears more than once", "XQST0066");
        }
        foundDefaultElementNamespace = true;
        nextToken();
        expect(Token.NAME);
        if (!"namespace".equals(t.currentTokenValue)) {
            grumble("After 'declare default element', expected 'namespace'");
        }
        nextToken();
        expect(Token.STRING_LITERAL);
        String uri = URILiteral(t.currentTokenValue);
        ((StaticQueryContext)env).setDefaultElementNamespace(uri);
        nextToken();
    }

    /**
     * Parse a namespace declaration in the Prolog.
     * Syntax: <"declare" "namespace"> NCName "=" StringLiteral
     *
     * @throws net.sf.saxon.trans.StaticError
     */

    private void parseNamespaceDeclaration() throws StaticError {
        nextToken();
        expect(Token.NAME);
        String prefix = t.currentTokenValue;
        if (!nameChecker.isValidNCName(prefix)) {
            grumble("Invalid namespace prefix " + Err.wrap(prefix));
        }
        checkProhibitedPrefixes(prefix);
        nextToken();
        expect(Token.EQUALS);
        nextToken();
        expect(Token.STRING_LITERAL);
        String uri = URILiteral(t.currentTokenValue);
        try {
            ((StaticQueryContext)env).declarePassiveNamespace(prefix, uri, true);
        } catch (StaticError err) {
            err.setLocator(makeLocator());
            reportError(err);
        }
        nextToken();
    }

    /**
     * Check that a namespace prefix is not a prohibited prefix (xml or xmlns)
     *
     * @param prefix the prefix to be tested
     * @throws StaticError if the prefix is prohibited
     */

    private void checkProhibitedPrefixes(String prefix) throws StaticError {
        if ("xml".equals(prefix) || "xmlns".equals(prefix)) {
            grumble("The namespace prefix " + Err.wrap(prefix) + " cannot be redeclared", "XQST0070");
        }
    }

    /**
     * Parse a global variable definition.
     * <"declare" "variable" "$"> VarName TypeDeclaration?
     * ((":=" Expr ) | "external")
     * Currently accept both
     *
     * @throws net.sf.saxon.trans.StaticError
     */

    private void parseVariableDeclaration() throws StaticError {
        int offset = t.currentTokenStartOffset;
        GlobalVariableDefinition var = new GlobalVariableDefinition();
        var.setLineNumber(t.getLineNumber());
        var.setSystemId(env.getSystemId());
        nextToken();
        expect(Token.DOLLAR);
        t.setState(Tokenizer.BARE_NAME_STATE);
        nextToken();
        expect(Token.NAME);
        String varName = t.currentTokenValue;
        var.setVariableName(varName);
        int varNameCode = makeNameCode(t.currentTokenValue, false);
        int varFingerprint = varNameCode & 0xfffff;
        var.setNameCode(varFingerprint);

        String uri = env.getNamePool().getURI(varNameCode);
        String moduleURI = ((StaticQueryContext)env).getModuleNamespace();
        if (moduleURI != null && !moduleURI.equals(uri)) {
            grumble("A variable declared in a library module must be in the module namespace", "XQST0048");
        }

        nextToken();
        SequenceType requiredType = SequenceType.ANY_SEQUENCE;
        if (isKeyword("as")) {
            t.setState(Tokenizer.SEQUENCE_TYPE_STATE);
            nextToken();
            requiredType = parseSequenceType();
        }
        var.setRequiredType(requiredType);

        if (t.currentToken == Token.ASSIGN) {
            t.setState(Tokenizer.DEFAULT_STATE);
            nextToken();
            Expression exp = parseExprSingle();
            var.setIsParameter(false);
            var.setValueExpression(makeTracer(offset, exp, StandardNames.XSL_VARIABLE, varNameCode));
        } else if (t.currentToken == Token.NAME) {
            if ("external".equals(t.currentTokenValue)) {
                var.setIsParameter(true);
                if (defaultValue != null) {
                    var.setValueExpression(defaultValue);
                }
                nextToken();
            } else {
                grumble("Variable must either be initialized or be declared as external");
            }
        } else {
            grumble("Expected ':=' or 'external' in variable declaration");
        }

        StaticQueryContext qenv = (StaticQueryContext)env;
        if (qenv.getModuleNamespace() != null &&
                env.getNamePool().getURICode(varFingerprint) != qenv.getModuleNamespaceCode()) {
            grumble("Variable " + Err.wrap(varName, Err.VARIABLE) + " is not defined in the module namespace");
        }
        try {
            qenv.declareVariable(var);
        } catch (XPathException e) {
            grumble(e.getMessage(), e.getErrorCodeLocalPart());
        }
    }

    /**
     * Parse a function declaration.
     * <p>Syntax:<br/>
     * <"declare" "function"> <QName "("> ParamList? (")" | (<")" "as"> SequenceType))
     * (EnclosedExpr | "external")
     * </p>
     * <p>On entry, the "define function" has already been recognized</p>
     *
     * @throws net.sf.saxon.trans.StaticError if a syntax error is found
     */

    private void parseFunctionDeclaration() throws StaticError {
        // the next token should be the < QNAME "("> pair
        int offset = t.currentTokenStartOffset;
        nextToken();
        expect(Token.FUNCTION);

        String uri;
        int fnc;
        if (t.currentTokenValue.indexOf(':') < 0) {
            uri = env.getDefaultFunctionNamespace();
            fnc = env.getNamePool().allocate("", uri, t.currentTokenValue);
        } else {
            fnc = makeNameCode(t.currentTokenValue, false);
            uri = env.getNamePool().getURI(fnc);
        }

        if (uri.equals("")) {
            grumble("The function must be in a namespace", "XQST0060");
        }

        String moduleURI = ((StaticQueryContext)env).getModuleNamespace();
        if (moduleURI != null && !moduleURI.equals(uri)) {
            grumble("A function in a library module must be in the module namespace", "XQST0048");
        }

        if (NamespaceConstant.isReservedInQuery(uri)) {
            grumble("The function name " + t.currentTokenValue + " is in a reserved namespace", "XQST0045");
        }

        XQueryFunction func = new XQueryFunction();
        func.setNameCode(fnc);
        func.arguments = new ArrayList(8);
        func.resultType = SequenceType.ANY_SEQUENCE;
        func.body = null;
        func.lineNumber = t.getLineNumber(offset);
        func.columnNumber = t.getColumnNumber(offset);
        func.systemId = env.getSystemId();
        func.staticContext = env;
        func.setMemoFunction(memoFunction);
        func.setExecutable(getExecutable());


        nextToken();
        IntHashSet paramNames = new IntHashSet(8);
        while (t.currentToken != Token.RPAR) {
            //     ParamList   ::=     Param ("," Param)*
            //     Param       ::=     "$" VarName  TypeDeclaration?
            expect(Token.DOLLAR);
            nextToken();
            expect(Token.NAME);
            String argName = t.currentTokenValue;
            int nameCode = makeNameCode(argName, false);
            int fingerprint = nameCode & 0xfffff;
            if (paramNames.contains(fingerprint)) {
                grumble("Duplicate parameter name " + Err.wrap(t.currentTokenValue, Err.VARIABLE), "XQST0039");
            }
            paramNames.add(fingerprint);
            SequenceType paramType = SequenceType.ANY_SEQUENCE;
            nextToken();
            if (t.currentToken == Token.NAME && "as".equals(t.currentTokenValue)) {
                nextToken();
                paramType = parseSequenceType();
            }

            RangeVariableDeclaration arg = new RangeVariableDeclaration();
            arg.setNameCode(nameCode);
            arg.setRequiredType(paramType);
            arg.setVariableName(argName);
            func.arguments.add(arg);
            declareRangeVariable(arg);
            if (t.currentToken == Token.RPAR) {
                break;
            } else if (t.currentToken == Token.COMMA) {
                nextToken();
            } else {
                grumble("Expected ',' or ')' after function argument, found '" +
                        Token.tokens[t.currentToken] + '\'');
            }
        }
        t.setState(Tokenizer.BARE_NAME_STATE);
        nextToken();
        if (isKeyword("as")) {
            t.setState(Tokenizer.SEQUENCE_TYPE_STATE);
            nextToken();
            func.resultType = parseSequenceType();
        }
        if (isKeyword("external")) {
            grumble("Saxon does not allow external functions to be declared");
        } else {
            expect(Token.LCURLY);
            t.setState(Tokenizer.DEFAULT_STATE);
            nextToken();
            func.body = parseExpression();
            ComputedExpression.setParentExpression(func.body, func);
            expect(Token.RCURLY);
            lookAhead();  // must be done manually after an RCURLY
        }
        UserFunctionParameter[] params = func.getParameterDefinitions();
        for (int i = 0; i < params.length; i++) {
            undeclareRangeVariable();
        }
        t.setState(Tokenizer.DEFAULT_STATE);
        nextToken();

        StaticQueryContext qenv = (StaticQueryContext)env;

        try {
            qenv.declareFunction(func);
        } catch (XPathException e) {
            grumble(e.getMessage(), e.getErrorCodeLocalPart());
        }
        memoFunction = false;
    }

    /**
     * Parse an option declaration.
     * <p>Syntax:<br/>
     * <"declare" "option">  QName "string-literal"
     * </p>
     * <p>On entry, the "declare option" has already been recognized</p>
     *
     * @throws net.sf.saxon.trans.StaticError if a syntax error is found
     */

    private void parseOptionDeclaration() throws StaticError {
        nextToken();
        expect(Token.NAME);
        int varNameCode = makeNameCode(t.currentTokenValue, false);
        String uri = env.getNamePool().getURI(varNameCode);

        if (uri.equals("")) {
            grumble("The QName identifying an option declaration must be prefixed", "XPST0081");
            return;
        }

        nextToken();
        expect(Token.STRING_LITERAL);
        String value = URILiteral(t.currentTokenValue);

        if (uri.equals(NamespaceConstant.SAXON)) {
            String localName = env.getNamePool().getLocalName(varNameCode);
            if (localName.equals("output")) {
                setOutputProperty(value);
            } else if (localName.equals("default")) {
                defaultValue = setDefaultValue(value);
            } else if (localName.equals("memo-function")) {
                if (value.equals("true")) {
                    memoFunction = true;
                } else if (value.equals("false")) {
                    memoFunction = false;
                } else {
                    warning("Value of saxon:memo-function must be 'true' or 'false'");
                }
            } else {
                warning("Unknown Saxon option declaration: " + env.getNamePool().getDisplayName(varNameCode));
            }
        }

        nextToken();
    }

    /**
     * Handle a saxon:output option declaration. Format:
     * declare option saxon:output "indent = yes"
     */

    private void setOutputProperty(String property) {
        int equals = property.indexOf("=");
        if (equals < 0) {
            badOutputProperty("no equals sign");
        } else if (equals == 0) {
            badOutputProperty("starts with '=");
        } else if (equals == property.length() - 1) {
            badOutputProperty("ends with '=");
        }
        String keyword = property.substring(0, equals).trim();
        String value = property.substring(equals + 1).trim();

        Properties props = getExecutable().getDefaultOutputProperties();
        try {
            int key = makeNameCode(keyword, false) & NamePool.FP_MASK;
            String lname = env.getNamePool().getLocalName(key);
            String uri = env.getNamePool().getURI(key);
            ResultDocument.setSerializationProperty(props,
                    uri, lname,
                    value,
                    env.getNamespaceResolver(),
                    false,
                    nameChecker);
        } catch (XPathException e) {
            badOutputProperty(e.getMessage());
        }
    }

    private void badOutputProperty(String s) {
        try {
            warning("Invalid serialization property (" + s + ") - ignored");
        } catch (StaticError staticError) {
            //
        }
    }

    /**
     * Parse the expression (inside a string literal) used to define default values
     * for external variables. This requires instantiating a nested XPath parser.
     */

    public Expression setDefaultValue(String exp) {
        try {
            IndependentContext ic = new IndependentContext(env.getConfiguration());
            ic.setNamespaceResolver(env.getNamespaceResolver());
            Expression expr = ExpressionTool.make(exp, ic, 0, Token.EOF, 1);

            ItemType contextItemType = Type.ITEM_TYPE;
            expr = expr.typeCheck(ic, contextItemType);
            expr = expr.optimize(env.getConfiguration().getOptimizer(), env, contextItemType);
            SlotManager stackFrameMap = ic.getStackFrameMap();
            ExpressionTool.allocateSlots(expr, stackFrameMap.getNumberOfVariables(), stackFrameMap);
            return expr;
        } catch (XPathException e) {
            try {
                warning("Invalid expression for default value: " + e.getMessage() + " (ignored)");
            } catch (StaticError staticError) {
                //
            }
            return null;
        }
    }

    /**
     * Parse a FLWOR expression. This replaces the XPath "for" expression.
     * Full syntax:
     * <p/>
     * [41] FLWORExpr ::=  (ForClause  | LetClause)+
     * WhereClause? OrderByClause?
     * "return" ExprSingle
     * [42] ForClause ::=  <"for" "$"> VarName TypeDeclaration? PositionalVar? "in" ExprSingle
     * ("," "$" VarName TypeDeclaration? PositionalVar? "in" ExprSingle)*
     * [43] PositionalVar  ::= "at" "$" VarName
     * [44] LetClause ::= <"let" "$"> VarName TypeDeclaration? ":=" ExprSingle
     * ("," "$" VarName TypeDeclaration? ":=" ExprSingle)*
     * [45] WhereClause  ::= "where" Expr
     * [46] OrderByClause ::= (<"order" "by"> | <"stable" "order" "by">) OrderSpecList
     * [47] OrderSpecList ::= OrderSpec  ("," OrderSpec)*
     * [48] OrderSpec     ::=     ExprSingle  OrderModifier
     * [49] OrderModifier ::= ("ascending" | "descending")?
     * (<"empty" "greatest"> | <"empty" "least">)?
     * ("collation" StringLiteral)?
     * </p>
     *
     * @return the resulting subexpression
     * @throws net.sf.saxon.trans.StaticError if any error is encountered
     */

    protected Expression parseForExpression() throws StaticError {
        int offset = t.currentTokenStartOffset;
        Expression whereCondition = null;
        int whereOffset = -1;
        //boolean stableOrder = false;
        List clauseList = new ArrayList(4);
        while (true) {
            if (t.currentToken == Token.FOR) {
                parseForClause(clauseList);
            } else if (t.currentToken == Token.LET) {
                parseLetClause(clauseList);
            } else {
                break;
            }
        }
        if (t.currentToken == Token.WHERE || isKeyword("where")) {
            whereOffset = t.currentTokenStartOffset;
            nextToken();
            whereCondition = parseExprSingle();
        }
        int orderByOffset = t.currentTokenStartOffset;
        if (isKeyword("stable")) {
            // we read the "stable" keyword but ignore it; Saxon ordering is always stable
            nextToken();
            if (!isKeyword("order")) {
                grumble("'stable' must be followed by 'order by'");
            }
        }
        List sortSpecList = null;
        if (isKeyword("order")) {
            t.setState(Tokenizer.BARE_NAME_STATE);
            nextToken();
            if (!isKeyword("by")) {
                grumble("'order' must be followed by 'by'");
            }
            t.setState(Tokenizer.DEFAULT_STATE);
            nextToken();
            sortSpecList = parseSortDefinition();
        }
        int returnOffset = t.currentTokenStartOffset;
        expect(Token.RETURN);
        t.setState(Tokenizer.DEFAULT_STATE);
        nextToken();
        Expression action = parseExprSingle();
        action = makeTracer(returnOffset, action, Location.RETURN_EXPRESSION, -1);



        // If there is an order by clause, we modify the "return" expression so that it
        // returns a tuple containing the actual return value, plus the value of
        // each of the sort keys. We then wrap the entire FLWR expression inside a
        // TupleSorter that sorts the stream of tuples according to the sort keys,
        // discarding the sort keys and returning only the true result. The tuple
        // is implemented as a Java object wrapped inside an ObjectValue, which is
        // a general-purpose wrapper for objects that don't fit in the XPath type system.

        if (sortSpecList != null) {
            TupleExpression exp = new TupleExpression(1 + sortSpecList.size());
            setLocation(exp);
            exp.setExpression(0, action);
            for (int i = 0; i < sortSpecList.size(); i++) {
                try {
                    RoleLocator role = new RoleLocator(RoleLocator.ORDER_BY, "FLWR", i, null);
                    role.setSourceLocator(makeLocator());
                    Expression sk =
                            TypeChecker.staticTypeCheck(((SortSpec)sortSpecList.get(i)).sortKey,
                                    SequenceType.OPTIONAL_ATOMIC,
                                    false,
                                    role, env);
                    exp.setExpression(i + 1, sk);
                } catch (XPathException err) {
                    grumble(err.getMessage());
                }
            }
            action = exp;
        }

        // if there is a "where" condition, we implement this by wrapping an if/then/else
        // around the "return" expression. No clever optimization yet!

        if (whereCondition != null) {
            action = new IfExpression(whereCondition, action, EmptySequence.getInstance());
            action = makeTracer(whereOffset, action, Location.WHERE_CLAUSE, -1);
            setLocation(action);
        }

        for (int i = clauseList.size() - 1; i >= 0; i--) {
            Object clause = clauseList.get(i);
            if (clause instanceof ExpressionParser.ForClause) {
                ExpressionParser.ForClause fc = (ExpressionParser.ForClause)clause;
                ForExpression exp = new ForExpression();
                exp.setVariableDeclaration(fc.rangeVariable);
                exp.setPositionVariable(fc.positionVariable);
                exp.setLocationId(env.getLocationMap().allocateLocationId(env.getSystemId(), t.getLineNumber(fc.offset)));
                exp.setSequence(fc.sequence);
                exp.setAction(action);
                action = makeTracer(fc.offset, exp, Location.FOR_EXPRESSION, fc.rangeVariable.getNameCode());
            } else {
                LetClause lc = (LetClause)clause;
                LetExpression exp = makeLetExpression();
                exp.setVariableDeclaration(lc.variable);
                exp.setLocationId(env.getLocationMap().allocateLocationId(env.getSystemId(), t.getLineNumber(lc.offset)));
                exp.setSequence(lc.value);
                exp.setAction(action);
                action = makeTracer(lc.offset, exp, Location.LET_EXPRESSION, lc.variable.getNameCode());
            }
        }

        // Now wrap the whole expression in a TupleSorter if there is a sort specification

        if (sortSpecList != null) {
            SortKeyDefinition[] keys = new SortKeyDefinition[sortSpecList.size()];
            //Comparator[] comparators = new Comparator[keys.length];
            for (int i = 0; i < keys.length; i++) {
                SortSpec spec = (SortSpec)sortSpecList.get(i);
                SortKeyDefinition key = new SortKeyDefinition();
                key.setSortKey(((SortSpec) sortSpecList.get(i)).sortKey);
                //key.setSortKey(StringValue.EMPTY_STRING);
                key.setOrder(new StringValue(spec.ascending ? "ascending" : "descending"));
                key.setEmptyLeast(spec.emptyLeast);

                if (spec.collation != null) {
                    final Comparator comparator = env.getCollation(spec.collation);
                    if (comparator == null) {
                        grumble("Unknown collation '" + spec.collation + '\'', "XQST0076");
                    }
                    key.setCollation(comparator);
                }
               // try {
               //     // TODO:PERF: delay allocating comparators until type-checking time
               //     Comparator comp = key.bindComparer(env.makeEarlyEvaluationContext());
               //     key.setComparer(comp);
               //     comparators[i] = comp;
                    keys[i] = key;
               // } catch (XPathException e) {
               //     grumble(e.getMessage());
               // }
            }
            TupleSorter sorter = new TupleSorter(action, keys);
            setLocation(sorter);
            action = makeTracer(orderByOffset, sorter, Location.ORDER_BY_CLAUSE, -1);
        }

        // undeclare all the range variables

        for (int i = clauseList.size() - 1; i >= 0; i--) {
            Object clause = clauseList.get(i);
            if ((clause instanceof ExpressionParser.ForClause) &&
                    ((ExpressionParser.ForClause)clause).positionVariable != null) {
                // undeclare the "at" variable if it was declared
                undeclareRangeVariable();
            }
            // undeclare the primary variable
            undeclareRangeVariable();
        }

        setLocation(action, offset);
        return action;

    }

    /**
     * Make a LetExpression. This returns an ordinary LetExpression if tracing is off, and an EagerLetExpression
     * if tracing is on. This is so that trace events occur in an order that the user can follow.
     */

    private LetExpression makeLetExpression() {
        if (env.getConfiguration().isCompileWithTracing()) {
            return new LetExpression();
        } else {
            return new EagerLetExpression();
        }
    }

    /**
     * Parse a ForClause.
     * <p/>
     * [42] ForClause ::=  <"for" "$"> VarName TypeDeclaration? PositionalVar? "in" ExprSingle
     * ("," "$" VarName TypeDeclaration? PositionalVar? "in" ExprSingle)*
     * </p>
     *
     * @param clauseList - the components of the parsed ForClause are appended to the
     *                   supplied list
     * @throws net.sf.saxon.trans.StaticError
     */
    private void parseForClause(List clauseList) throws StaticError {
        boolean first = true;
        do {
            ExpressionParser.ForClause clause = new ExpressionParser.ForClause();
            if (first) {
                clause.offset = t.currentTokenStartOffset;
            }
            clauseList.add(clause);
            nextToken();
            if (first) {
                first = false;
            } else {
                clause.offset = t.currentTokenStartOffset;
            }
            expect(Token.DOLLAR);
            nextToken();
            expect(Token.NAME);
            String var = t.currentTokenValue;

            RangeVariableDeclaration v = new RangeVariableDeclaration();
            final int varcode = makeNameCode(var, false);
            v.setNameCode(varcode);
            v.setRequiredType(SequenceType.SINGLE_ITEM);
            v.setVariableName(var);
            clause.rangeVariable = v;
            nextToken();

            if (isKeyword("as")) {
                nextToken();
                SequenceType type = parseSequenceType();
                if (type.getCardinality() != StaticProperty.EXACTLY_ONE) {
                    warning("Occurrence indicator on singleton range variable has no effect");
                    type = SequenceType.makeSequenceType(type.getPrimaryType(), StaticProperty.EXACTLY_ONE);
                }
                v.setRequiredType(type);
            }
            clause.positionVariable = null;
            if (isKeyword("at")) {
                nextToken();
                expect(Token.DOLLAR);
                nextToken();
                expect(Token.NAME);
                RangeVariableDeclaration pos = new RangeVariableDeclaration();
                int poscode = makeNameCode(t.currentTokenValue, false);
                if ((poscode & NamePool.FP_MASK) == (varcode & NamePool.FP_MASK)) {
                    grumble("The two variables declared in a single 'for' clause must have different names", "XQST0089");
                }
                pos.setNameCode(poscode);
                pos.setRequiredType(SequenceType.SINGLE_INTEGER);
                pos.setVariableName(t.currentTokenValue);
                clause.positionVariable = pos;
                //declareRangeVariable(pos);
                nextToken();
            }
            expect(Token.IN);
            nextToken();
            clause.sequence = parseExprSingle();
            declareRangeVariable(clause.rangeVariable);
            if (clause.positionVariable != null) {
                declareRangeVariable(clause.positionVariable);
            }
        } while (t.currentToken == Token.COMMA);
    }

    /**
     * Parse a LetClause.
     * <p/>
     * [44] LetClause ::= <"let" "$"> VarName TypeDeclaration? ":=" ExprSingle
     * ("," "$" VarName TypeDeclaration? ":=" ExprSingle)*
     * </p>
     *
     * @param clauseList - the components of the parsed LetClause are appended to the
     *                   supplied list
     * @throws net.sf.saxon.trans.StaticError
     */
    private void parseLetClause(List clauseList) throws StaticError {
        boolean first = true;
        do {
            LetClause clause = new LetClause();
            if (first) {
                clause.offset = t.currentTokenStartOffset;
            }
            clauseList.add(clause);
            nextToken();
            if (first) {
                first = false;
            } else {
                clause.offset = t.currentTokenStartOffset;
            }
            expect(Token.DOLLAR);
            nextToken();
            expect(Token.NAME);
            String var = t.currentTokenValue;

            RangeVariableDeclaration v = new RangeVariableDeclaration();
            v.setNameCode(makeNameCode(var, false));
            v.setRequiredType(SequenceType.ANY_SEQUENCE);
            v.setVariableName(var);
            clause.variable = v;
            nextToken();

            if (isKeyword("as")) {
                nextToken();
                v.setRequiredType(parseSequenceType());
            }

            expect(Token.ASSIGN);
            nextToken();
            clause.value = parseExprSingle();
            declareRangeVariable(v);
        } while (t.currentToken == Token.COMMA);
    }

    /**
     * Make a string-join expression that concatenates the string-values of items in
     * a sequence with intervening spaces. This may be simplified later as a result
     * of type-checking.
     */

    public static Expression makeStringJoin(Expression exp, StaticContext env) {

        exp = new Atomizer(exp, env.getConfiguration());
        final TypeHierarchy th = env.getConfiguration().getTypeHierarchy();
        ItemType t = exp.getItemType(th);
        if (t != Type.STRING_TYPE && t != Type.UNTYPED_ATOMIC_TYPE) {
            exp = new AtomicSequenceConverter(exp, Type.STRING_TYPE);
        }

        StringJoin fn = (StringJoin)SystemFunction.makeSystemFunction("string-join", 2, env.getNamePool());
        Expression[] args = new Expression[2];
        args[0] = exp;
        args[1] = StringValue.SINGLE_SPACE;
        fn.setArguments(args);
        if (exp instanceof ComputedExpression) {
            fn.setLocationId(((ComputedExpression)exp).getLocationId());
        }
        return fn;
    }

    private static class LetClause {
        public RangeVariableDeclaration variable;
        public Expression value;
        public int offset;
    }

    /**
     * Parse the "order by" clause.
     * [46] OrderByClause ::= (<"order" "by"> | <"stable" "order" "by">) OrderSpecList
     * [47] OrderSpecList ::= OrderSpec  ("," OrderSpec)*
     * [48] OrderSpec     ::=     ExprSingle  OrderModifier
     * [49] OrderModifier ::= ("ascending" | "descending")?
     * (<"empty" "greatest"> | <"empty" "least">)?
     * ("collation" StringLiteral)?
     *
     * @return a list of sort specifications (SortSpec), one per sort key
     * @throws net.sf.saxon.trans.StaticError
     */
    private List parseSortDefinition() throws StaticError {
        List sortSpecList = new ArrayList(5);
        while (true) {
            SortSpec sortSpec = new SortSpec();
            sortSpec.sortKey = parseExprSingle();
            sortSpec.ascending = true;
            sortSpec.emptyLeast = defaultEmptyLeast;
            sortSpec.collation = env.getDefaultCollationName();
            //t.setState(t.BARE_NAME_STATE);
            if (isKeyword("ascending")) {
                nextToken();
            } else if (isKeyword("descending")) {
                sortSpec.ascending = false;
                nextToken();
            }
            if (isKeyword("empty")) {
                nextToken();
                if (isKeyword("greatest")) {
                    sortSpec.emptyLeast = false;
                    nextToken();
                } else if (isKeyword("least")) {
                    sortSpec.emptyLeast = true;
                    nextToken();
                } else {
                    grumble("'empty' must be followed by 'greatest' or 'least'");
                }
            }
            if (isKeyword("collation")) {
                nextToken();
                expect(Token.STRING_LITERAL);
                String collationName = URILiteral(t.currentTokenValue);
                URI collationURI;
                try {
                    collationURI = new URI(collationName);
                    if (!collationURI.isAbsolute()) {
                        URI base = new URI(env.getBaseURI());
                        collationURI = base.resolve(collationURI);
                        collationName = collationURI.toString();
                    }
                } catch (URISyntaxException err) {
                    grumble("Collation name '" + collationName + "' is not a valid URI");
                    collationName = NamespaceConstant.CODEPOINT_COLLATION_URI;
                }
                sortSpec.collation = collationName;
                nextToken();
            }
            sortSpecList.add(sortSpec);
            if (t.currentToken == Token.COMMA) {
                nextToken();
            } else {
                break;
            }
        }
        return sortSpecList;
    }

    private static class SortSpec {
        public Expression sortKey;
        public boolean ascending;
        public boolean emptyLeast;
        public String collation;
    }

    /**
     * Parse a Typeswitch Expression.
     * This construct is XQuery-only.
     * TypeswitchExpr   ::=
     * "typeswitch" "(" Expr ")"
     * CaseClause+
     * "default" ("$" VarName)? "return" ExprSingle
     * CaseClause   ::=
     * "case" ("$" VarName "as")? SequenceType "return" ExprSingle
     */

    protected Expression parseTypeswitchExpression() throws StaticError {

        // On entry, the "(" has already been read
        int offset = t.currentTokenStartOffset;
        nextToken();
        Expression operand = parseExpression();
        List types = new ArrayList(10);
        List actions = new ArrayList(10);
        expect(Token.RPAR);
        nextToken();

        // The code generated takes the form:
        //    let $zzz := operand return
        //    if ($zzz instance of t1) then action1
        //    else if ($zzz instance of t2) then action2
        //    else default-action
        //
        // If a variable is declared in a case clause or default clause,
        // then "action-n" takes the form
        //    let $v as type := $zzz return action-n

        // we were generating "let $v as type := $zzz return action-n" but this gives a compile time error if
        // there's a case clause that specifies an impossible type.

        LetExpression outerLet = makeLetExpression();

        RangeVariableDeclaration gen = new RangeVariableDeclaration();
        gen.setNameCode(makeNameCode("zz_typeswitchVar", false));
        gen.setRequiredType(SequenceType.ANY_SEQUENCE);
        gen.setVariableName("zz_typeswitchVar");

        outerLet.setVariableDeclaration(gen);
        outerLet.setSequence(operand);

        while (t.currentToken == Token.CASE) {
            int caseOffset = t.currentTokenStartOffset;
            SequenceType type;
            Expression action;
            nextToken();
            if (t.currentToken == Token.DOLLAR) {
                nextToken();
                expect(Token.NAME);
                final String var = t.currentTokenValue;
                final int varCode = makeNameCode(var, false);
                nextToken();
                expect(Token.NAME);
                if (!"as".equals(t.currentTokenValue)) {
                    grumble("After 'case $" + var + "', expected 'as'");
                }
                nextToken();
                type = parseSequenceType();
                action = makeTracer(caseOffset,
                        parseTypeswitchReturnClause(var, varCode, gen),
                        Location.CASE_EXPRESSION,
                        varCode);
                if (action instanceof TraceExpression) {
                    ((TraceExpression)action).setProperty("type", type.toString());
                }

            } else {
                type = parseSequenceType();
                t.treatCurrentAsOperator();
                expect(Token.RETURN);
                nextToken();
                action = makeTracer(caseOffset, parseExprSingle(), Location.CASE_EXPRESSION, -1);
                if (action instanceof TraceExpression) {
                    ((TraceExpression)action).setProperty("type", type.toString());
                }
            }
            types.add(type);
            actions.add(action);
        }
        if (types.size() == 0) {
            grumble("At least one case clause is required in a typeswitch");
        }
        expect(Token.DEFAULT);
        final int defaultOffset = t.currentTokenStartOffset;
        nextToken();
        Expression defaultAction;
        if (t.currentToken == Token.DOLLAR) {
            nextToken();
            expect(Token.NAME);
            final String var = t.currentTokenValue;
            final int varCode = makeNameCode(var, false);
            nextToken();
            defaultAction = makeTracer(defaultOffset,
                    parseTypeswitchReturnClause(var, varCode, gen),
                    Location.DEFAULT_EXPRESSION,
                    varCode);
        } else {
            t.treatCurrentAsOperator();
            expect(Token.RETURN);
            nextToken();
            defaultAction = makeTracer(defaultOffset, parseExprSingle(), Location.DEFAULT_EXPRESSION, -1);
        }

        Expression lastAction = defaultAction;
        for (int i = types.size() - 1; i >= 0; i--) {
            final VariableReference var = new VariableReference(gen);
            setLocation(var);
            final InstanceOfExpression ioe =
                    new InstanceOfExpression(var, (SequenceType)types.get(i));
            setLocation(ioe);
            final IfExpression ife =
                    new IfExpression(ioe, (Expression)actions.get(i), lastAction);
            setLocation(ife);
            lastAction = ife;
        }
        outerLet.setAction(lastAction);
        return makeTracer(offset, outerLet, Location.TYPESWITCH_EXPRESSION, -1);
    }

    private Expression parseTypeswitchReturnClause(String var, int varCode, RangeVariableDeclaration gen)
            throws StaticError {
        Expression action;
        t.treatCurrentAsOperator();
        expect(Token.RETURN);
        nextToken();

        RangeVariableDeclaration v = new RangeVariableDeclaration();
        v.setNameCode(varCode);
        v.setRequiredType(SequenceType.ANY_SEQUENCE);
        v.setVariableName(var);

        declareRangeVariable(v);
        action = parseExprSingle();
        undeclareRangeVariable();

        LetExpression innerLet = makeLetExpression();
        innerLet.setVariableDeclaration(v);
        innerLet.setSequence(new VariableReference(gen));
        innerLet.setAction(action);
        action = innerLet;
        return action;
    }

    /**
     * Parse a Validate Expression.
     * This construct is XQuery-only. The syntax allows:
     * validate mode? { Expr }
     * mode ::= "strict" | "lax"
     */

    protected Expression parseValidateExpression() throws StaticError {
        if (!env.getConfiguration().isSchemaAware(Configuration.XQUERY)) {
            grumble("To use a validate expression, you need the schema-aware processor from http://www.saxonica.com/");
        }
        int offset = t.currentTokenStartOffset;
        int mode = Validation.STRICT;
        boolean foundCurly = false;
        switch (t.currentToken) {
            case Token.VALIDATE_STRICT:
                mode = Validation.STRICT;
                nextToken();
                break;
            case Token.VALIDATE_LAX:
                mode = Validation.LAX;
                nextToken();
                break;
            case Token.KEYWORD_CURLY:
                if (t.currentTokenValue == "validate") {
                    mode = Validation.STRICT;
                } else {
                    throw new AssertionError("shouldn't be parsing a validate expression");
                }
                foundCurly = true;
        }

        if (!foundCurly) {
            expect(Token.LCURLY);
        }
        nextToken();

        Expression exp = parseExpression();
        if (exp instanceof ElementCreator) {
            ((ElementCreator)exp).setValidationMode(mode);
        } else if (exp instanceof DocumentInstr) {
            ((DocumentInstr)exp).setValidationAction(mode);
        } else {
            // the expression must return a single element or document node. The type-
            // checking machinery can't handle a union type, so we just check that it's
            // a node for now. Because we are reusing XSLT copy-of code, we need
            // an ad-hoc check that the node is of the right kind.
            try {
                RoleLocator role = new RoleLocator(RoleLocator.TYPE_OP, "validate", 0, null);
                role.setSourceLocator(makeLocator());
                exp = TypeChecker.staticTypeCheck(exp,
                        SequenceType.SINGLE_NODE,
                        false,
                        role, env);
            } catch (XPathException err) {
                grumble(err.getMessage());
            }
            exp = new CopyOf(exp, true, mode, null, true);
            setLocation(exp);
            ((CopyOf)exp).setRequireDocumentOrElement(true);
        }

        expect(Token.RCURLY);
        t.lookAhead();      // always done manually after an RCURLY
        nextToken();
        return makeTracer(offset, exp, Location.VALIDATE_EXPRESSION, -1);
    }

    /**
     * Parse an Extension Expression.
     * Syntax: "(#" QName arbitrary-text "#)")+ "{" expr? "}"
     */

    protected Expression parseExtensionExpression() throws StaticError {
        SchemaType requiredType = null;
        CharSequence trimmed = Whitespace.removeLeadingWhitespace(t.currentTokenValue);
        int c = 0;
        int len = trimmed.length();
        while (c < len && " \t\r\n".indexOf(trimmed.charAt(c)) < 0) {
            c++;
        }
        String qname = trimmed.subSequence(0, c).toString();
        String pragmaContents = "";
        while (c < len && " \t\r\n".indexOf(trimmed.charAt(c)) >= 0) {
            c++;
        }
        if (c < len) {
            pragmaContents = trimmed.subSequence(c, len).toString();
        }

        if (!nameChecker.isQName(qname)) {
            grumble("First token in pragma must be a valid QName, terminated by whitespace");
        } else {
            int nameCode = makeNameCode(qname, false);
            String uri = env.getNamePool().getURI(nameCode);
            if ((nameCode & NamePool.FP_MASK) == StandardNames.SAXON_VALIDATE_TYPE) {
                if (!env.getConfiguration().isSchemaAware(Configuration.XQUERY)) {
                    grumble("To use saxon:validate-type, " +
                            "you need the schema-aware processor from http://www.saxonica.com/");
                }
                String typeName = pragmaContents.trim();
                if (!nameChecker.isQName(typeName)) {
                    grumble("Schema type expected in saxon:validate-type pragma");
                }
                int typeCode = makeNameCode(typeName, true);
                requiredType = env.getConfiguration().getSchemaType(typeCode & NamePool.FP_MASK);
                if (requiredType == null) {
                    grumble("Unknown schema type " + typeName);
                }
            } else if (uri.equals(NamespaceConstant.SAXON)) {
                grumble("Unrecognized Saxon pragma " + qname);
            } else if (uri.equals("")) {
                grumble("The QName identifying an option declaration must be prefixed", "XPST0081");
            }
        }

        nextToken();
        Expression expr;
        if (t.currentToken == Token.PRAGMA) {
            expr = parseExtensionExpression();
        } else {
            expect(Token.LCURLY);
            nextToken();
            if (t.currentToken == Token.RCURLY) {
                t.lookAhead();      // always done manually after an RCURLY
                nextToken();
                grumble("Unrecognized pragma, with no fallback expression", "XQST0079");
            }
            expr = parseExpression();
            expect(Token.RCURLY);
            t.lookAhead();      // always done manually after an RCURLY
            nextToken();
        }
        if (requiredType == null) {
            return expr;
        } else if (expr instanceof ElementCreator) {
            ((ElementCreator)expr).setSchemaType(requiredType);
            return expr;
        } else if (expr instanceof ElementCreator) {
            ((ElementCreator)expr).setSchemaType(requiredType);
            return expr;
        } else {
            CopyOf copy = new CopyOf(expr, true, Validation.PRESERVE, requiredType, true);
            copy.setLocationId(env.getLocationMap().allocateLocationId(env.getSystemId(), t.getLineNumber()));
            return copy;
        }
    }

    /**
     * Parse a node constructor. This is allowed only in XQuery. This method handles
     * both the XML-like "direct" constructors, and the XQuery-based "computed"
     * constructors.
     *
     * @return an Expression for evaluating the parsed constructor
     * @throws net.sf.saxon.trans.StaticError in the event of a syntax error.
     */

    protected Expression parseConstructor() throws StaticError {
        int offset = t.currentTokenStartOffset;
        switch (t.currentToken) {
            case Token.TAG:
                Expression tag = parsePseudoXML(false);
                lookAhead();
                t.setState(Tokenizer.OPERATOR_STATE);
                nextToken();
                return tag;
            case Token.KEYWORD_CURLY:
                String nodeKind = t.currentTokenValue;
                if (nodeKind == "validate") {
                    return parseValidateExpression();
                } else if (nodeKind == "ordered" || nodeKind == "unordered") {
                    // these are currently no-ops in Saxon
                    nextToken();
                    Expression content = parseExpression();
                    expect(Token.RCURLY);
                    lookAhead();  // must be done manually after an RCURLY
                    nextToken();
                    return content;
                } else if (nodeKind == "document") {
                    nextToken();
                    Expression content = parseExpression();
                    expect(Token.RCURLY);
                    lookAhead();  // must be done manually after an RCURLY
                    nextToken();
                    DocumentInstr doc = new DocumentInstr(false, null, env.getBaseURI());
                    if (!((StaticQueryContext)env).isPreserveNamespaces()) {
                        content = new CopyOf(content, false, Validation.PRESERVE, null, true);
                    }
                    doc.setValidationAction(((StaticQueryContext)env).getConstructionMode());
                    doc.setContentExpression(content);
                    setLocation(doc, offset);
                    return doc;

                } else if ("element".equals(nodeKind)) {
                    nextToken();
                    // get the expression that yields the element name
                    Expression name = parseExpression();
                    expect(Token.RCURLY);
                    lookAhead();  // must be done manually after an RCURLY
                    nextToken();
                    expect(Token.LCURLY);
                    t.setState(Tokenizer.DEFAULT_STATE);
                    nextToken();
                    Expression content = null;
                    if (t.currentToken != Token.RCURLY) {
                        // get the expression that yields the element content
                        content = parseExpression();
                        // if the child expression creates another element,
                        // suppress validation, as the parent already takes care of it
                        if (content instanceof ElementCreator) {
                            ((ElementCreator)content).setValidationMode(Validation.PRESERVE);
                        }
                        expect(Token.RCURLY);
                    }
                    lookAhead();  // done manually after an RCURLY
                    nextToken();

                    Instruction inst;
                    if (name instanceof Value) {
                        // if element name is supplied as a literal, treat it like a direct element constructor
                        int nameCode;
                        if (name instanceof StringValue && !(name instanceof AnyURIValue)) {
                            String lex = ((StringValue)name).getStringValue();
                            // prevalidate the name before calling makeNameCode, because we want different
                            // error behaviour
                            try {
                                nameChecker.getQNameParts(lex);
                            } catch (QNameException err) {
                                DynamicError e = new DynamicError("Invalid QName in element constructor: " + lex);
                                e.setErrorCode("XQDY0074");
                                return new ErrorExpression(e);
                            }
                            try {
                                nameCode = makeNameCode(lex, true);
                            } catch (StaticError staticError) {
                                // TODO: too late to change the error - it's already been reported - also on line 2336
                                String code = staticError.getErrorCodeLocalPart();
                                if ("XPST0008".equals(code) || "XPST0081".equals(code)) {
                                    staticError.setErrorCode("XQDY0074");
                                }
                                throw staticError;
                            }
                        } else if (name instanceof QNameValue) {
                            String uri = ((QNameValue)name).getNamespaceURI();
                            nameCode = env.getNamePool().allocate("",
                                    (uri == null ? "" : uri),
                                    ((QNameValue)name).getLocalName());
                        } else {
                            grumble("Element name must be either a string or a QName", "XPTY0004");
                            return null;
                        }
                        inst = new FixedElement(nameCode,
                                ((StaticQueryContext)env).getActiveNamespaceCodes(),
                                ((StaticQueryContext)env).isInheritNamespaces(),
                                null,
                                ((StaticQueryContext)env).getConstructionMode());
                        ((FixedElement)inst).setBaseURI(env.getBaseURI());
                        if (content == null) {
                            content = EmptySequence.getInstance();
                        }
                        ((FixedElement)inst).setContentExpression(content);
                        setLocation(inst, offset);
                        //makeContentConstructor(content, (InstructionWithChildren) inst, offset);
                        return makeTracer(offset, inst, Location.LITERAL_RESULT_ELEMENT, nameCode);
                    } else {
                        // it really is a computed element constructor: save the namespace context
                        inst = new ComputedElement(name, null,
                                env.getNamespaceResolver(),
                                null,
                                ((StaticQueryContext)env).getConstructionMode(),
                                ((StaticQueryContext)env).isInheritNamespaces(),
                                true);
                        setLocation(inst);
                        if (content == null) {
                            content = EmptySequence.getInstance();
                        }
                        ((ComputedElement)inst).setContentExpression(content);
                        setLocation(inst, offset);
                        //makeContentConstructor(content, (InstructionWithChildren) inst, offset);
                        return makeTracer(offset, inst, StandardNames.XSL_ELEMENT, -1);
                    }


                } else if ("attribute".equals(nodeKind)) {
                    nextToken();
                    Expression name = parseExpression();
                    expect(Token.RCURLY);
                    lookAhead();  // must be done manually after an RCURLY
                    nextToken();
                    expect(Token.LCURLY);
                    t.setState(Tokenizer.DEFAULT_STATE);
                    nextToken();
                    Expression content = null;
                    if (t.currentToken != Token.RCURLY) {
                        content = parseExpression();
                        expect(Token.RCURLY);
                    }
                    lookAhead();  // after an RCURLY
                    nextToken();
                    if (name instanceof StringValue && !(name instanceof AnyURIValue) && content instanceof Value) {
                        String attName = ((StringValue)name).getStringValue();
                        if (attName.equals("xmlns") || attName.startsWith("xmlns:")) {
                            grumble("Cannot create a namespace using an attribute constructor", "XQDY0044");
                        }
                        int nameCode;
                        String lex = ((StringValue)name).getStringValue();
                        // prevalidate the name before calling makeNameCode, because we want different
                        // error behaviour
                        try {
                            nameChecker.getQNameParts(lex);
                        } catch (QNameException err) {
                            DynamicError e = new DynamicError("Invalid QName in attribute constructor: " + lex);
                            e.setErrorCode("XQDY0074");
                            return new ErrorExpression(e);
                        }
                        try {
                            nameCode = makeNameCode(attName, false);
                        } catch (StaticError staticError) {
                            String code = staticError.getErrorCodeLocalPart();
                            if ("XPST0008".equals(code) || "XPST0081".equals(code)) {
                                staticError.setErrorCode("XQDY0074");
                            }
                            throw staticError;
                        }
                        FixedAttribute fatt = new FixedAttribute(nameCode,
                                Validation.STRIP,
                                null,
                                StandardNames.XDT_UNTYPED_ATOMIC);
                        fatt.setRejectDuplicates();
                        makeSimpleContent(content, fatt, offset);
                        return makeTracer(offset, fatt, StandardNames.XSL_ATTRIBUTE, -1);
                    } else {
                        Attribute att = new Attribute(name,
                                null,
                                env.getNamespaceResolver(),
                                Validation.STRIP,
                                null,
                                -1,
                                true);
                        att.setRejectDuplicates();
                        makeSimpleContent(content, att, offset);
                        return makeTracer(offset, att, StandardNames.XSL_ATTRIBUTE, -1);
                    }

                } else if ("text".equals(nodeKind)) {
                    nextToken();
//                    if (t.currentToken == Token.RCURLY) {
//                        lookAhead(); // after an RCURLY
//                        nextToken();
//                        return EmptySequence.getInstance();
//                    } else {
                        Expression value = parseExpression();
                        expect(Token.RCURLY);
                        lookAhead(); // after an RCURLY
                        nextToken();
                        Expression select;
                        try {
                            select = stringify(value, true);
                        } catch (XPathException e) {
                            throw StaticError.makeStaticError(e);
                        }
                        ValueOf vof = new ValueOf(select, false, true);
                        setLocation(vof, offset);
                        return makeTracer(offset, vof, StandardNames.XSL_TEXT, -1);
//                    }
                } else if ("comment".equals(nodeKind)) {
                    nextToken();
//                    if (t.currentToken == Token.RCURLY) {
//                        lookAhead(); // after an RCURLY
//                        nextToken();
//                        return EmptySequence.getInstance();
//                    } else {
                        Expression value = parseExpression();
                        expect(Token.RCURLY);
                        lookAhead(); // after an RCURLY
                        nextToken();
                        Comment com = new Comment();
                        makeSimpleContent(value, com, offset);
                        return makeTracer(offset, com, StandardNames.XSL_COMMENT, -1);
//                    }
                } else if ("processing-instruction".equals(nodeKind)) {
                    nextToken();
                    Expression name = parseExpression();
                    expect(Token.RCURLY);
                    lookAhead();  // must be done manually after an RCURLY
                    nextToken();
                    expect(Token.LCURLY);
                    t.setState(Tokenizer.DEFAULT_STATE);
                    nextToken();
                    Expression content = null;
                    if (t.currentToken != Token.RCURLY) {
                        content = parseExpression();
                        expect(Token.RCURLY);
                    }
                    lookAhead();  // after an RCURLY
                    nextToken();
                    ProcessingInstruction pi = new ProcessingInstruction(name);
                    makeSimpleContent(content, pi, offset);
                    return makeTracer(offset, pi, StandardNames.XSL_PROCESSING_INSTRUCTION, -1);

                } else {
                    grumble("Unrecognized node constructor " + t.currentTokenValue + "{}");

                }
            case Token.ELEMENT_QNAME:
                int nameCode = makeNameCode(t.currentTokenValue, true);
                Expression content = null;
                nextToken();
                if (t.currentToken != Token.RCURLY) {
                    content = parseExpression();
                    expect(Token.RCURLY);
                }
                lookAhead();  // after an RCURLY
                nextToken();
                FixedElement el2 = new FixedElement(nameCode,
                        ((StaticQueryContext)env).getActiveNamespaceCodes(),
                        ((StaticQueryContext)env).isInheritNamespaces(),
                        null,
                        ((StaticQueryContext)env).getConstructionMode());
                el2.setBaseURI(env.getBaseURI());
                setLocation(el2, offset);
                if (content == null) {
                    content = EmptySequence.getInstance();
                }
                el2.setContentExpression(content);
                //makeContentConstructor(content, el2, offset);
                return makeTracer(offset, el2, Location.LITERAL_RESULT_ELEMENT, nameCode);
            case Token.ATTRIBUTE_QNAME:
                if (t.currentTokenValue.equals("xmlns") || t.currentTokenValue.startsWith("xmlns:")) {
                    grumble("Cannot create a namespace using an attribute constructor", "XQDY0044");
                }
                int attNameCode = makeNameCode(t.currentTokenValue, false);
                Expression attContent = null;
                nextToken();
                if (t.currentToken != Token.RCURLY) {
                    attContent = parseExpression();
                    expect(Token.RCURLY);
                }
                lookAhead();  // after an RCURLY
                nextToken();
                FixedAttribute att2 = new FixedAttribute(attNameCode,
                        Validation.STRIP,
                        null,
                        StandardNames.XDT_UNTYPED_ATOMIC);
                att2.setRejectDuplicates();
                makeSimpleContent(attContent, att2, offset);
                return makeTracer(offset, att2, Location.LITERAL_RESULT_ATTRIBUTE, attNameCode);
            case Token.PI_QNAME:
                String target = t.currentTokenValue;
                if (target.equalsIgnoreCase("xml")) {
                    grumble("A processing instruction must not be named 'xml' in any combination of upper and lower case");
                }

                Expression piName = new StringValue(target);
                Expression piContent = null;
                nextToken();
                if (t.currentToken != Token.RCURLY) {
                    piContent = parseExpression();
                    expect(Token.RCURLY);
                }
                lookAhead();  // after an RCURLY
                nextToken();
                ProcessingInstruction pi2 = new ProcessingInstruction(piName);
                makeSimpleContent(piContent, pi2, offset);
                return makeTracer(offset, pi2, StandardNames.XSL_PROCESSING_INSTRUCTION, -1);
        }
        return null;
    }

    /**
     * Make the instructions for the children of a node with simple content (attribute, text, PI, etc)
     *
     * @param content
     * @param inst
     * @param offset
     */

    private void makeSimpleContent(Expression content, SimpleNodeConstructor inst, int offset) throws StaticError {
        try {
            if (content == null) {
                inst.setSelect(StringValue.EMPTY_STRING, env.getConfiguration());
            } else {
                inst.setSelect(stringify(content, false), env.getConfiguration());
            }
            setLocation(inst, offset);
        } catch (XPathException e) {
            grumble(e.getMessage());
        }
    }

    /**
     * Make a sequence of instructions as the children of an element-construction instruction.
     * The idea here is to convert an XPath expression that "pulls" the content into a sequence
     * of XSLT-style instructions that push nodes directly onto the subtree, thus reducing the
     * need for copying of intermediate nodes.
     * @param content The content of the element as an expression
     * @param inst The element-construction instruction (Element or FixedElement)
     * @param offset the character position in the query
     */
//    private void makeContentConstructor(Expression content, InstructionWithChildren inst, int offset) {
//        if (content == null) {
//            inst.setChildren(null);
//        } else if (content instanceof AppendExpression) {
//            List instructions = new ArrayList(10);
//            convertAppendExpression((AppendExpression) content, instructions);
//            inst.setChildren((Expression[]) instructions.toArray(new Expression[instructions.size()]));
//        } else {
//            Expression children[] = {content};
//            inst.setChildren(children);
//        }
//        setLocation(inst, offset);
//    }

    /**
     * Parse pseudo-XML syntax in direct element constructors, comments, CDATA, etc.
     * This is handled by reading single characters from the Tokenizer until the
     * end of the tag (or an enclosed expression) is enountered.
     * This method is also used to read an end tag. Because an end tag is not an
     * expression, the method in this case returns a StringValue containing the
     * contents of the end tag.
     *
     * @param allowEndTag true if the context allows an End Tag to appear here
     * @return an Expression representing the result of parsing the constructor.
     *         If an end tag was read, its contents will be returned as a StringValue.
     */

    private Expression parsePseudoXML(boolean allowEndTag) throws StaticError {
        try {
            Expression exp = null;
            int offset = t.inputOffset;
            // we're reading raw characters, so we don't want the currentTokenStartOffset
            char c = t.nextChar();
            switch (c) {
                case '!':
                    c = t.nextChar();
                    if (c == '-') {
                        exp = parseCommentConstructor();
                    } else if (c == '[') {
                        grumble("A CDATA section is allowed only in element content");
                        // if CDATA were allowed here, we would have already read it
                    } else {
                        grumble("Expected '--' or '[CDATA[' after '<!'");
                    }
                    break;
                case '?':
                    exp = parsePIConstructor();
                    break;
                case '/':
                    if (allowEndTag) {
                        FastStringBuffer sb = new FastStringBuffer(40);
                        while (true) {
                            c = t.nextChar();
                            if (c == '>') {
                                break;
                            }
                            sb.append(c);
                        }
                        return new StringValue(sb);
                    }
                    grumble("Unmatched XML end tag");
                    break;
                default:
                    t.unreadChar();
                    exp = parseDirectElementConstructor();
            }
            setLocation(exp, offset);
            return exp;
        } catch (StringIndexOutOfBoundsException e) {
            grumble("End of input encountered while parsing direct constructor");
            return null;
        }
    }

    /**
     * Parse a direct element constructor
     *
     * @return the expression representing the constructor
     * @throws StaticError
     * @throws StringIndexOutOfBoundsException if the end of input is encountered prematurely
     */

    private Expression parseDirectElementConstructor() throws StaticError, StringIndexOutOfBoundsException {
        int offset = t.inputOffset - 1;
        // we're reading raw characters, so we don't want the currentTokenStartOffset
        char c;
        FastStringBuffer buff = new FastStringBuffer(40);
        int namespaceCount = 0;
        while (true) {
            c = t.nextChar();
            if (c == ' ' || c == '\n' || c == '\r' || c == '\t' || c == '/' || c == '>') {
                break;
            }
            buff.append(c);
        }
        String elname = buff.toString();
        HashMap attributes = new HashMap(10);
        while (true) {
            // loop through the attributes
            // We must process namespace declaration attributes first;
            // their scope applies to all preceding attribute names and values.
            // But finding the delimiting quote of an attribute value requires the
            // XPath expressions to be parsed, because they may contain nested quotes.
            // So we parse in "scanOnly" mode, which ignores any undeclared namespace
            // prefixes, use the result of this parse to determine the length of the
            // attribute value, save the value, and reparse it when all the namespace
            // declarations have been dealt with.
            c = skipSpaces(c);
            if (c == '/' || c == '>') {
                break;
            }
            int attOffset = t.inputOffset - 1;
            buff.setLength(0);
            // read the attribute name
            while (true) {
                buff.append(c);
                c = t.nextChar();
                if (c == ' ' || c == '\n' || c == '\r' || c == '\t' || c == '=') {
                    break;
                }
            }
            String attName = buff.toString();
            if (!nameChecker.isQName(attName)) {
                grumble("Invalid attribute name " + Err.wrap(attName, Err.ATTRIBUTE));
            }
            c = skipSpaces(c);
            expectChar(c, '=');
            c = t.nextChar();
            c = skipSpaces(c);

            Expression avt;
            try {
                avt = makeAttributeContent(t.input, t.inputOffset, c, true);
            } catch (StaticError err) {
                grumble(err.getMessage());
                return null;
            } catch (XPathException err) {
                throw err.makeStatic();
            }
            // by convention, this returns the end position when called with scanOnly set
            int end = (int)((IntegerValue)avt).longValue();
            // save the value with its surrounding quotes
            String val = t.input.substring(t.inputOffset - 1, end + 1);
            // and without
            String rval = t.input.substring(t.inputOffset, end);
            t.inputOffset = end + 1;
            // on return, the current character is the closing quote
            c = t.nextChar();
            if (!(c == ' ' || c == '\n' || c == '\r' || c == '\t' || c == '/' || c == '>')) {
                grumble("There must be whitespace after every attribute except the last");
            }
            if ("xmlns".equals(attName) || attName.startsWith("xmlns:")) {
                if (rval.indexOf('{') >= 0) {
                    grumble("Namespace URI must be a constant value", "XQST0022");
                }
                String prefix, uri;
                if ("xmlns".equals(attName)) {
                    prefix = "";
                    uri = rval;
                } else {
                    prefix = attName.substring(6);
                    if (prefix.equals("xml")) {
                        grumble("Cannot redeclare the XML namespace", "XQST0070");
                    } else if (prefix.equals("xmlns")) {
                        grumble("Cannot use xmlns as a namespace prefix", "XQST0070");
                    }
                    uri = rval;
                    if ("".equals(uri)) {
                        grumble("Namespace URI must not be empty", "XQST0085");
                    }
                }
                namespaceCount++;
                ((StaticQueryContext)env).declareActiveNamespace(prefix, uri);
            }
            if (attributes.get(attName) != null) {
                if ("xmlns".equals(attName) || attName.startsWith("xmlns:")) {
                    grumble("Duplicate namespace declaration " + attName, "XQST0071");
                } else {
                    grumble("Duplicate attribute name " + attName, "XQST0040");
                }
            }
//            if (attName.equals("xml:id") && !nameChecker.isValidNCName(rval)) {
//                grumble("Value of xml:id must be a valid NCName", "XQST0082");
//            }
            AttributeDetails a = new AttributeDetails();
            a.value = val;
            a.startOffset = attOffset;
            attributes.put(attName, a);
        }
        String namespace;
        int elNameCode = 0;
        try {
            String[] parts = nameChecker.getQNameParts(elname);
            namespace = ((StaticQueryContext)env).checkURIForPrefix(parts[0]);
            if (namespace == null) {
                grumble("Undeclared prefix in element name " + Err.wrap(elname, Err.ELEMENT), "XPST0081");
            }
            elNameCode = env.getNamePool().allocate(parts[0], namespace, parts[1]);
        } catch (QNameException e) {
            grumble("Invalid element name " + Err.wrap(elname, Err.ELEMENT));
        }
        int validationMode = ((StaticQueryContext)env).getConstructionMode();
        FixedElement elInst = new FixedElement(elNameCode,
                ((StaticQueryContext)env).getActiveNamespaceCodes(),
                ((StaticQueryContext)env).isInheritNamespaces(),
                null,
                validationMode);

        elInst.setBaseURI(env.getBaseURI());
        setLocation(elInst, offset);

        List contents = new ArrayList(10);

        IntHashSet attFingerprints = new IntHashSet(attributes.size());
        // we've checked for duplicate lexical QNames, but not for duplicate expanded-QNames
        for (Iterator iter = attributes.keySet().iterator(); iter.hasNext();) {
            String attName = (String)iter.next();
            AttributeDetails a = (AttributeDetails)attributes.get(attName);
            String attValue = a.value;
            int attOffset = a.startOffset;

            if ("xmlns".equals(attName) || attName.startsWith("xmlns:")) {
                // do nothing
            } else if (scanOnly) {
                // This means we are prescanning an attribute constructor, and we found a nested attribute
                // constructor, which we have prescanned; we now don't want to re-process the nested attribute
                // constructor because it might depend on things like variables declared in the containing
                // attribute constructor, and in any case we're going to come back to it again later.
                // See test qxmp180
            } else {
                int attNameCode = 0;
                String attNamespace;
                try {
                    String[] parts = nameChecker.getQNameParts(attName);
                    if ("".equals(parts[0])) {
                        // attributes don't use the default namespace
                        attNamespace = "";
                    } else {
                        attNamespace = ((StaticQueryContext)env).checkURIForPrefix(parts[0]);
                    }
                    if (attNamespace == null) {
                        grumble("Undeclared prefix in attribute name " +
                                Err.wrap(attName, Err.ATTRIBUTE), "XPST0081");
                    }
                    attNameCode = env.getNamePool().allocate(parts[0], attNamespace, parts[1]);
                    int key = (attNameCode & NamePool.FP_MASK);
                    if (attFingerprints.contains(key)) {
                        grumble("Duplicate expanded attribute name " + attName, "XQST0040");
                    }
                    attFingerprints.add(key);
                } catch (QNameException e) {
                    grumble("Invalid attribute name " + Err.wrap(attName, Err.ATTRIBUTE));
                }

                FixedAttribute attInst =
                        new FixedAttribute(attNameCode, Validation.STRIP, null, StandardNames.XDT_UNTYPED_ATOMIC);

                setLocation(attInst);
                Expression select;
                try {
                    select = makeAttributeContent(attValue, 1, attValue.charAt(0), false);
                } catch (XPathException err) {
                    throw err.makeStatic();
                }
                attInst.setSelect(select, env.getConfiguration());
                attInst.setRejectDuplicates();
                setLocation(attInst);
                contents.add(makeTracer(attOffset, attInst, Location.LITERAL_RESULT_ATTRIBUTE, attNameCode));
            }
        }
        if (c == '/') {
            // empty element tag
            expectChar(t.nextChar(), '>');
        } else {
            readElementContent(elname, contents);
        }

        Expression[] elk = new Expression[contents.size()];
        for (int i = 0; i < contents.size(); i++) {
            // if the child expression creates another element,
            // suppress validation, as the parent already takes care of it
            if (validationMode != Validation.STRIP && contents.get(i) instanceof ComputedExpression) {
                ((ComputedExpression)contents.get(i)).suppressValidation(validationMode);
            }
            elk[i] = (Expression)contents.get(i);
        }
        Block block = new Block();
        block.setChildren(elk);
        elInst.setContentExpression(block);

        // reset the in-scope namespaces to what they were before

        for (int n = 0; n < namespaceCount; n++) {
            ((StaticQueryContext)env).undeclareNamespace();
        }

        return makeTracer(offset, elInst, Location.LITERAL_RESULT_ELEMENT, elNameCode);
    }

    /**
     * Parse the content of an attribute in a direct element constructor. This may contain nested expressions
     * within curly braces. A particular problem is that the namespaces used in the expression may not yet be
     * known. This means we need the ability to parse in "scanOnly" mode, where undeclared namespace prefixes
     * are ignored.
     * <p/>
     * The code is based on the XSLT code in {@link AttributeValueTemplate#make}: the main difference is that
     * character entities and built-in entity references need to be recognized and expanded. Also, whitespace
     * needs to be normalized, mimicking the action of an XML parser
     */

    private Expression makeAttributeContent(String avt,
                                            int start,
                                            char terminator,
                                            boolean scanOnly) throws XPathException {

        int lineNumber = t.getLineNumber();
        List components = new ArrayList(10);

        int i0, i1, i2, i8, i9, len, last;
        last = start;
        len = avt.length();
        while (last < len) {
            i2 = avt.indexOf(terminator, last);
            if (i2 < 0) {
                throw new StaticError("Attribute constructor is not properly terminated");
            }

            i0 = avt.indexOf("{", last);
            i1 = avt.indexOf("{{", last);
            i8 = avt.indexOf("}", last);
            i9 = avt.indexOf("}}", last);

            if ((i0 < 0 || i2 < i0) && (i8 < 0 || i2 < i8)) {   // found end of string
                addStringComponent(components, avt, last, i2);

                // look for doubled quotes, and skip them (for now)
                if (i2 + 1 < avt.length() && avt.charAt(i2 + 1) == terminator) {
                    components.add(new StringValue(terminator + ""));
                    last = i2 + 2;
                    continue;
                } else {
                    last = i2;
                    break;
                }
            } else if (i8 >= 0 && (i0 < 0 || i8 < i0)) {             // found a "}"
                if (i8 != i9) {                        // a "}" that isn't a "}}"
                    throw new StaticError("Closing curly brace in attribute value template \"" + avt + "\" must be doubled");
                }
                addStringComponent(components, avt, last, i8 + 1);
                last = i8 + 2;
            } else if (i1 >= 0 && i1 == i0) {              // found a doubled "{{"
                addStringComponent(components, avt, last, i1 + 1);
                last = i1 + 2;
            } else if (i0 >= 0) {                        // found a single "{"
                if (i0 > last) {
                    addStringComponent(components, avt, last, i0);
                }
                Expression exp;
                ExpressionParser parser;
                parser = new QueryParser();
                parser.setScanOnly(scanOnly);
                if (rangeVariables != null) {
                    parser.setRangeVariableStack(rangeVariables);
                }
                exp = parser.parse(avt, i0 + 1, Token.RCURLY, lineNumber, env);
                if (!scanOnly) {
                    exp = exp.simplify(env);
                }
                last = parser.getTokenizer().currentTokenStartOffset + 1;
                components.add(makeStringJoin(exp, env));

            } else {
                throw new IllegalStateException("Internal error parsing direct attribute constructor");
            }
        }

        // if this is simply a prescan, return the position of the end of the
        // AVT, so we can parse it properly later

        if (scanOnly) {
            return new IntegerValue(last);
        }

        // is it empty?

        if (components.size() == 0) {
            return StringValue.EMPTY_STRING;
        }

        // is it a single component?

        if (components.size() == 1) {
            return ((Expression)components.get(0)).simplify(env);
        }

        // otherwise, return an expression that concatenates the components

        Concat fn = (Concat)SystemFunction.makeSystemFunction("concat", components.size(), env.getNamePool());
        Expression[] args = new Expression[components.size()];
        components.toArray(args);
        fn.setArguments(args);
        fn.setLocationId(env.getLocationMap().allocateLocationId(env.getSystemId(), lineNumber));
        return fn.simplify(env);

    }

    private void addStringComponent(List components, String avt, int start, int end)
            throws XPathException {
        // analyze fixed text within the value of a direct attribute constructor.
        if (start < end) {
            FastStringBuffer sb = new FastStringBuffer(end - start);
            for (int i = start; i < end; i++) {
                char c = avt.charAt(i);
                switch (c) {
                    case '&':
                        {
                            int semic = avt.indexOf(';', i);
                            if (semic < 0) {
                                grumble("No closing ';' found for entity or character reference");
                            } else {
                                String entity = avt.substring(i + 1, semic);
                                sb.append(analyzeEntityReference(entity));
                                i = semic;
                            }
                            break;
                        }
                    case '<':
                        grumble("The < character must not appear in attribute content");
                        break;
                    case '\n':
                    case '\t':
                        sb.append(' ');
                        break;
                    case '\r':
                        sb.append(' ');
                        if (i + 1 < end && avt.charAt(i + 1) == '\n') {
                            i++;
                        }
                        break;
                    default:
                        sb.append(c);

                }
            }
            components.add(StringValue.makeStringValue(sb));
        }
    }

    /**
     * Read the content of a direct element constructor
     *
     * @param startTag   the element start tag
     * @param components an empty list, to which the expressions comprising the element contents are added
     * @throws StaticError if any static errors are detected
     */
    private void readElementContent(String startTag, List components) throws StaticError {
        final TypeHierarchy th = env.getConfiguration().getTypeHierarchy();
        try {
            boolean afterEnclosedExpr = false;
            while (true) {
                // read all the components of the element value
                FastStringBuffer text = new FastStringBuffer(256);
                char c;
                boolean containsEntities = false;
                while (true) {
                    c = t.nextChar();
                    if (c == '<') {
                        // See if we've got a CDATA section
                        if (t.nextChar() == '!') {
                            if (t.nextChar() == '[') {
                                readCDATASection(text);
                                containsEntities = true;
                                continue;
                            } else {
                                t.unreadChar();
                                t.unreadChar();
                            }
                        } else {
                            t.unreadChar();
                        }
                        break;
                    } else if (c == '&') {
                        text.append(readEntityReference());
                        containsEntities = true;
                    } else if (c == '}') {
                        c = t.nextChar();
                        if (c != '}') {
                            grumble("'}' must be written as '}}' within element content");
                        }
                        text.append(c);
                    } else if (c == '{') {
                        c = t.nextChar();
                        if (c != '{') {
                            c = '{';
                            break;
                        }
                        text.append(c);
                    } else {
                        text.append(c);
                    }
                }
                if (text.length() > 0 &&
                        (containsEntities | preserveSpace | !Whitespace.isWhite(text))) {
                    ValueOf inst = new ValueOf(new StringValue(text.condense()), false, false);
                    setLocation(inst);
                    components.add(inst);
                    afterEnclosedExpr = false;
                }
                if (c == '<') {
                    Expression exp = parsePseudoXML(true);
                    // An end tag can appear here, and is returned as a string value
                    if (exp instanceof StringValue) {
                        String endTag = ((StringValue)exp).getStringValue();
                        if (Whitespace.isWhitespace(endTag.charAt(0))) {
                            grumble("End tag contains whitespace before the name");
                        }
                        endTag = endTag.trim();
                        if (endTag.equals(startTag)) {
                            return;
                        } else {
                            grumble("End tag </" + endTag +
                                    "> does not match start tag <" + startTag + '>');
                        }
                    } else {
                        components.add(exp);
                    }
                } else {
                    // we read an '{' indicating an enclosed expression
                    if (afterEnclosedExpr) {
                        Expression previousComponent = (Expression)components.get(components.size() - 1);
                        ItemType previousItemType = previousComponent.getItemType(th);
                        if (!(previousItemType instanceof NodeTest)) {
                            // Add a zero-length text node, to prevent {"a"}{"b"} generating an intervening space
                            // See tests (qxmp132, qxmp261)
                            ValueOf inst = new ValueOf(StringValue.EMPTY_STRING, false, false);
                            setLocation(inst);
                            components.add(inst);
                        }
                    }
                    t.unreadChar();
                    t.setState(Tokenizer.DEFAULT_STATE);
                    lookAhead();
                    nextToken();
                    Expression exp = parseExpression();
                    if (!((StaticQueryContext)env).isPreserveNamespaces()) {
                        exp = new CopyOf(exp, false, Validation.PRESERVE, null, true);
                    }
                    components.add(exp);
                    expect(Token.RCURLY);
                    afterEnclosedExpr = true;
                }
            }
        } catch (StringIndexOutOfBoundsException err) {
            grumble("No closing end tag found for direct element constructor");
        }
    }

    private Expression parsePIConstructor() throws StaticError {
        try {
            FastStringBuffer pi = new FastStringBuffer(120);
            int firstSpace = -1;
            while (!pi.toString().endsWith("?>")) {
                char c = t.nextChar();
                if (firstSpace < 0 && " \t\r\n".indexOf(c) >= 0) {
                    firstSpace = pi.length();
                }
                pi.append(c);
            }
            pi.setLength(pi.length() - 2);

            String target;
            String data = "";
            if (firstSpace < 0) {
                // there is no data part
                target = pi.toString();
            } else {
                // trim leading space from the data part, but not trailing space
                target = pi.toString().substring(0, firstSpace);
                firstSpace++;
                while (firstSpace < pi.length() && " \t\r\n".indexOf(pi.charAt(firstSpace)) >= 0) {
                    firstSpace++;
                }
                data = pi.toString().substring(firstSpace);
            }

            if (!nameChecker.isValidNCName(target)) {
                grumble("Invalid processing instruction name " + Err.wrap(target));
            }

            if (target.equalsIgnoreCase("xml")) {
                grumble("A processing instruction must not be named 'xml' in any combination of upper and lower case");
            }

            ProcessingInstruction instruction =
                    new ProcessingInstruction(new StringValue(target));
            instruction.setSelect(StringValue.makeStringValue(data), env.getConfiguration());
            setLocation(instruction);
            return instruction;
        } catch (StringIndexOutOfBoundsException err) {
            grumble("No closing '?>' found for processing instruction");
            return null;
        }
    }

    private void readCDATASection(FastStringBuffer cdata) throws StaticError {
        try {
            char c;
            // CDATA section
            c = t.nextChar();
            expectChar(c, 'C');
            c = t.nextChar();
            expectChar(c, 'D');
            c = t.nextChar();
            expectChar(c, 'A');
            c = t.nextChar();
            expectChar(c, 'T');
            c = t.nextChar();
            expectChar(c, 'A');
            c = t.nextChar();
            expectChar(c, '[');
            while (!cdata.toString().endsWith("]]>")) {
                cdata.append(t.nextChar());
            }
            cdata.setLength(cdata.length() - 3);
        } catch (StringIndexOutOfBoundsException err) {
            grumble("No closing ']]>' found for CDATA section");
        }
    }

    private Expression parseCommentConstructor() throws StaticError {
        try {
            char c = t.nextChar();
            ;
            // XML-like comment
            expectChar(c, '-');
            FastStringBuffer comment = new FastStringBuffer(240);
            while (!comment.toString().endsWith("--")) {
                comment.append(t.nextChar());
            }
            if (t.nextChar() != '>') {
                grumble("'--' is not permitted in an XML comment");
            }
            CharSequence commentText = comment.subSequence(0, comment.length() - 2);
            Comment instruction = new Comment();
            instruction.setSelect(new StringValue(commentText), env.getConfiguration());
            setLocation(instruction);
            return instruction;
        } catch (StringIndexOutOfBoundsException err) {
            grumble("No closing '-->' found for comment constructor");
            return null;
        }
    }

    /**
     * Convert an expression so it generates a space-separated sequence of strings
     *
     * @param exp           the expression that calculates the content
     * @param noNodeIfEmpty if true, no node is produced when the value of the content
     *                      expression is an empty sequence. If false, the effect of supplying an empty sequence
     *                      is that a node is created whose string-value is a zero-length string. Set to true for
     *                      text node constructors, false for other kinds of node.
     */

    private Expression stringify(Expression exp, boolean noNodeIfEmpty) throws XPathException {
        return new QuerySimpleContentConstructor(exp, StringValue.SINGLE_SPACE, noNodeIfEmpty).simplify(env);
    }

    /**
     * Method to make a string literal from a token identified as a string
     * literal. This is trivial in XPath, but in XQuery the method is overridden
     * to identify pseudo-XML character and entity references
     *
     * @param token
     * @return The string value of the string literal, after dereferencing entity and
     *         character references
     */

    protected StringValue makeStringLiteral(String token) throws StaticError {
        if (token.indexOf('&') == -1) {
            return StringValue.makeStringValue(token);
        } else {
            FastStringBuffer sb = unescape(token);
            return StringValue.makeStringValue(sb);
        }
    }

    /**
     * Unescape character references and built-in entity references in a string
     *
     * @param token
     * @return
     * @throws StaticError
     */

    private FastStringBuffer unescape(String token) throws StaticError {
        FastStringBuffer sb = new FastStringBuffer(80);
        for (int i = 0; i < token.length(); i++) {
            char c = token.charAt(i);
            if (c == '&') {
                int semic = token.indexOf(';', i);
                if (semic < 0) {
                    grumble("No closing ';' found for entity or character reference");
                } else {
                    String entity = token.substring(i + 1, semic);
                    sb.append(analyzeEntityReference(entity));
                    i = semic;
                }
            } else {
                sb.append(c);
            }
        }
        return sb;
    }

    /**
     * Read a pseudo-XML character reference or entity reference.
     *
     * @return The character represented by the character or entity reference. Note
     *         that this is a string rather than a char because a char only accommodates characters
     *         up to 65535.
     * @throws net.sf.saxon.trans.StaticError if the character or entity reference is not well-formed
     */

    private String readEntityReference() throws StaticError {
        try {
            FastStringBuffer sb = new FastStringBuffer(40);
            while (true) {
                char c = t.nextChar();
                if (c == ';') {
                    break;
                }
                sb.append(c);
            }
            String entity = sb.toString();
            return analyzeEntityReference(entity);
        } catch (StringIndexOutOfBoundsException err) {
            grumble("No closing ';' found for entity or character reference");
        }
        return null;     // to keep the Java compiler happy
    }

    private String analyzeEntityReference(String entity) throws StaticError {
        if ("lt".equals(entity)) {
            return "<";
        } else if ("gt".equals(entity)) {
            return ">";
        } else if ("amp".equals(entity)) {
            return "&";
        } else if ("quot".equals(entity)) {
            return "\"";
        } else if ("apos".equals(entity)) {
            return "'";
        } else if (entity.length() < 2 || entity.charAt(0) != '#') {
            grumble("invalid entity reference &" + entity + ';');
            return null;
        } else {
            //entity = entity.toLowerCase();
            return parseCharacterReference(entity);
        }
    }

    private String parseCharacterReference(String entity) throws StaticError {
        int value = 0;
        if (entity.charAt(1) == 'x') {
            entity = entity.toLowerCase();
            for (int i = 2; i < entity.length(); i++) {
                int digit = "0123456789abcdef".indexOf(entity.charAt(i));
                if (digit < 0) {
                    grumble("invalid hex digit '" + entity.charAt(i) + "' in character reference");
                }
                value = (value * 16) + digit;
            }
        } else {
            for (int i = 1; i < entity.length(); i++) {
                int digit = "0123456789".indexOf(entity.charAt(i));
                if (digit < 0) {
                    grumble("invalid digit '" + entity.charAt(i) + "' in decimal character reference");
                }
                value = (value * 10) + digit;
            }
        }

        NameChecker nc = env.getConfiguration().getNameChecker();
        if (!nc.isValidChar(value)) {
            grumble("Invalid XML character reference x"
                    + Integer.toHexString(value), "XPST0003");
        }
        // following code borrowed from AElfred
        // Check for surrogates: 00000000 0000xxxx yyyyyyyy zzzzzzzz
        //  (1101|10xx|xxyy|yyyy + 1101|11yy|zzzz|zzzz:
        if (value <= 0x0000ffff) {
            // no surrogates needed
            return "" + (char)value;
        } else if (value <= 0x0010ffff) {
            value -= 0x10000;
            // > 16 bits, surrogate needed
            return "" + ((char)(0xd800 | (value >> 10)))
                    + ((char)(0xdc00 | (value & 0x0003ff)));
        } else {
            // too big for surrogate
            grumble("Character reference x" + Integer.toHexString(value) + " is too large", "XPST0003");
        }
        return null;
    }

    /**
     * Handle a URI literal. This is whitespace-normalized as well as being unescaped
     */

    private String URILiteral(String in) throws StaticError {
        return Whitespace.applyWhitespaceNormalization(Whitespace.COLLAPSE, unescape(in)).toString();
    }

    /**
     * Lookahead one token, catching any exception thrown by the tokenizer. This
     * method is only called from the query parser when switching from character-at-a-time
     * mode to tokenizing mode
     */

    private void lookAhead() throws StaticError {
        try {
            t.lookAhead();
        } catch (XPathException err) {
            grumble(err.getMessage());
        }
    }

    /**
     * Skip whitespace.
     *
     * @param c the current character
     * @return the first character after any whitespace
     * @throws StringIndexOutOfBoundsException if the end of input is encountered
     */

    private char skipSpaces(char c) throws StringIndexOutOfBoundsException {
        while (c == ' ' || c == '\n' || c == '\r' || c == '\t') {
            c = t.nextChar();
        }
        return c;
    }

    /**
     * Test whether the current character is the expected character.
     *
     * @param actual   The character that was read
     * @param expected The character that was expected
     * @throws net.sf.saxon.trans.StaticError if they are different
     */

    private void expectChar(char actual, char expected) throws StaticError {
        if (actual != expected) {
            grumble("Expected '" + expected + "', found '" + actual + '\'');
        }
    }

    /**
     * Get the current language (XPath or XQuery)
     */

    protected String getLanguage() {
        return "XQuery";
    }

    private static class AttributeDetails {
        String value;
        int startOffset;
    }

    private static class Import {
        String namespaceURI;
        List locationURIs;
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
// The Initial Developer of the Original Code is Michael H. Kay
//
// Portions created by (your name) are Copyright (C) (your legal entity). All Rights Reserved.
//
// Contributor(s): none.
//
