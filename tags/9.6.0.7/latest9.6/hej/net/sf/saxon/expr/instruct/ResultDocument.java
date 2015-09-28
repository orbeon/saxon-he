////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
// Copyright (c) 2014 Saxonica Limited.
// This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
// If a copy of the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
// This Source Code Form is "Incompatible With Secondary Licenses", as defined by the Mozilla Public License, v. 2.0.
////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

package net.sf.saxon.expr.instruct;

import com.saxonica.ee.bytecode.ExpressionCompiler;
import com.saxonica.ee.bytecode.ResultDocumentCompiler;
import com.saxonica.ee.stream.adjunct.ResultDocumentAdjunct;
import net.sf.saxon.Configuration;
import net.sf.saxon.Controller;
import net.sf.saxon.event.LocationProvider;
import net.sf.saxon.event.PipelineConfiguration;
import net.sf.saxon.event.Receiver;
import net.sf.saxon.event.SequenceReceiver;
import net.sf.saxon.expr.*;
import net.sf.saxon.expr.parser.ContextItemStaticInfo;
import net.sf.saxon.expr.parser.ExpressionVisitor;
import net.sf.saxon.expr.parser.PathMap;
import net.sf.saxon.expr.parser.PromotionOffer;
import net.sf.saxon.functions.EscapeURI;
import net.sf.saxon.lib.*;
import net.sf.saxon.om.*;
import net.sf.saxon.serialize.CharacterMapIndex;
import net.sf.saxon.serialize.ReconfigurableSerializer;
import net.sf.saxon.trace.ExpressionPresenter;
import net.sf.saxon.trans.Err;
import net.sf.saxon.trans.SaxonErrorCode;
import net.sf.saxon.trans.XPathException;
import net.sf.saxon.type.ErrorType;
import net.sf.saxon.type.ItemType;
import net.sf.saxon.type.SchemaType;
import net.sf.saxon.type.Type;
import net.sf.saxon.value.Whitespace;

import javax.xml.transform.OutputKeys;
import javax.xml.transform.Result;
import javax.xml.transform.TransformerException;
import javax.xml.transform.dom.DOMResult;
import javax.xml.transform.sax.SAXResult;
import javax.xml.transform.stream.StreamResult;
import java.net.URI;
import java.util.ArrayList;
import java.util.Map;
import java.util.Properties;


/**
 * The compiled form of an xsl:result-document element in the stylesheet.
 * <p/>
 * The xsl:result-document element takes an attribute href="filename". The filename will
 * often contain parameters, e.g. {position()} to ensure that a different file is produced
 * for each element instance.
 * <p/>
 * There is a further attribute "format" which determines the format of the
 * output file, it identifies the name of an xsl:output element containing the output
 * format details. In addition, individual serialization properties may be specified as attributes.
 * These are attribute value templates, so they may need to be computed at run-time.
 */

public class ResultDocument extends Instruction implements ValidatingInstruction {

    /*@Nullable*/ protected Expression href;
    protected Expression formatExpression;    // null if format was known at compile time
    protected Expression content;
    private boolean async = false;
    protected Properties globalProperties;
    protected Properties localProperties;
    protected String baseURI;     // needed only for saxon:next-in-chain, or with fn:put()
    protected ParseOptions validationOptions;
    protected Map<StructuredQName, Expression> serializationAttributes;
    protected NamespaceResolver nsResolver;
    protected Expression dynamicOutputElement;    // used in saxon:result-document() extension function
    protected boolean resolveAgainstStaticBase = false;        // used with fn:put()
    protected CharacterMapIndex characterMapIndex;


    /**
     * Create a result-document instruction
     *
     * @param globalProperties        properties defined on static xsl:output
     * @param localProperties         non-AVT properties defined on result-document element
     * @param href                    href attribute of instruction
     * @param formatExpression        format attribute of instruction
     * @param baseURI                 base URI of the instruction
     * @param validationAction        for example {@link net.sf.saxon.lib.Validation#STRICT}
     * @param schemaType              schema type against which output is to be validated
     * @param serializationAttributes computed local properties
     * @param nsResolver              namespace resolver
     */

    public ResultDocument(Properties globalProperties,      // properties defined on static xsl:output
                          Properties localProperties,       // non-AVT properties defined on result-document element
                          Expression href,
                          Expression formatExpression,      // AVT defining the output format
                          String baseURI,
                          int validationAction,
                          SchemaType schemaType,
                          Map<StructuredQName, Expression> serializationAttributes,  // computed local properties only
                          CharacterMapIndex characterMapIndex,
                          NamespaceResolver nsResolver) {
        this.globalProperties = globalProperties;
        this.localProperties = localProperties;
        this.href = href;
        this.formatExpression = formatExpression;
        this.baseURI = baseURI;
        setValidationAction(validationAction, schemaType);
        this.serializationAttributes = serializationAttributes;
        this.characterMapIndex = characterMapIndex;
        this.nsResolver = nsResolver;
        adoptChildExpression(href);
        for (Expression e : serializationAttributes.values()) {
            adoptChildExpression(e);
        }
    }

    /**
     * Set the expression that constructs the content
     *
     * @param content the expression defining the content of the result document
     */

    public void setContentExpression(Expression content) {
        this.content = content;
        adoptChildExpression(content);
    }

    /**
     * Get the expression that constructs the content
     *
     * @return the content expression
     */

    public Expression getContentExpression() {
        return content;
    }

    /**
     * Set the schema type to be used for validation
     *
     * @param type the type to be used for validation. (For a document constructor, this is the required
     *             type of the document element)
     */

    public void setSchemaType(SchemaType type) {
        if (validationOptions == null) {
            validationOptions = new ParseOptions();
        }
        validationOptions.setSchemaValidationMode(Validation.BY_TYPE);
        validationOptions.setTopLevelType(type);
    }

    /**
     * Get the schema type chosen for validation; null if not defined
     *
     * @return the type to be used for validation. (For a document constructor, this is the required
     * type of the document element)
     */

    public SchemaType getSchemaType() {
        return validationOptions == null ? null : validationOptions.getTopLevelType();
    }

    /**
     * Get the expression that computes the href attribute
     *
     * @return the href expression, or null if there is no href attribute
     */

    public Expression getHrefExpression() {
        return href;
    }

    /**
     * Get the static base URI of the expression
     *
     * @return the static base URI
     */

    public String getStaticBaseURI() {
        return baseURI;
    }

    public boolean isResolveAgainstStaticBase() {
        return resolveAgainstStaticBase;
    }

    /**
     * Get the validation options
     *
     * @return the validation options for the content of the constructed node. May be null if no
     * validation was requested.
     */

    public ParseOptions getValidationOptions() {
        return validationOptions;
    }

    /**
     * Set the validation mode for the new document
     *
     * @param mode       the validation mode, for example {@link Validation#STRICT}
     * @param schemaType the required type (for validation by type). Null if not
     *                   validating by type
     */


    public void setValidationAction(int mode, /*@Nullable*/ SchemaType schemaType) {
        boolean preservingTypes = mode == Validation.PRESERVE && schemaType == null;
        if (!preservingTypes) {
            if (validationOptions == null) {
                validationOptions = new ParseOptions();
                validationOptions.setSchemaValidationMode(mode);
                validationOptions.setTopLevelType(schemaType);
            }
        }
    }


    /**
     * Get the validation mode for this instruction
     *
     * @return the validation mode, for example {@link Validation#STRICT} or {@link Validation#PRESERVE}
     */
    public int getValidationAction() {
        return validationOptions == null ? Validation.PRESERVE : validationOptions.getSchemaValidationMode();
    }


    public Expression getFormatExpression() {
        return formatExpression;
    }

    /**
     * Set an expression that evaluates to a run-time xsl:output element, used in the saxon:result-document()
     * extension function designed for use in XQuery
     *
     * @param exp the expression whose result should be an xsl:output element
     */

    public void setDynamicOutputElement(Expression exp) {
        dynamicOutputElement = exp;
    }

    /**
     * Set whether the the instruction should resolve the href relative URI against the static
     * base URI (rather than the dynamic base output URI)
     *
     * @param staticBase set to true by fn:put(), to resolve against the static base URI of the query.
     *                   Default is false, which causes resolution against the base output URI obtained dynamically
     *                   from the Controller
     */

    public void setUseStaticBaseUri(boolean staticBase) {
        resolveAgainstStaticBase = staticBase;
    }


    public void setAsynchronous(boolean async) {
        this.async = async;
    }

    /**
     * Ask if the instruction is to be asynchronous
     *
     * @return true unless saxon:asynchronous="no" was specified (regardless of other options that might
     * suppress asychronous operation)
     */

    public boolean isAsynchronous() {
        return async;
    }

    /**
     * Simplify an expression. This performs any static optimization (by rewriting the expression
     * as a different expression). The default implementation does nothing.
     *
     * @param visitor an expression visitor
     * @return the simplified expression
     * @throws net.sf.saxon.trans.XPathException if an error is discovered during expression rewriting
     */

    /*@NotNull*/
    public Expression simplify(ExpressionVisitor visitor) throws XPathException {
        content = visitor.simplify(content);
        href = visitor.simplify(href);
        for (Map.Entry<StructuredQName, Expression> entry : serializationAttributes.entrySet()) {
            Expression value = entry.getValue();
            if (!(value instanceof Literal)) {
                value = visitor.simplify(value);
                serializationAttributes.put(entry.getKey(), value);
            }
        }
        return this;
    }

    /*@NotNull*/
    public Expression typeCheck(ExpressionVisitor visitor, ContextItemStaticInfo contextInfo) throws XPathException {
        content = visitor.typeCheck(content, contextInfo);
        adoptChildExpression(content);
        if (href != null) {
            href = visitor.typeCheck(href, contextInfo);
            adoptChildExpression(href);
        }
        if (formatExpression != null) {
            formatExpression = visitor.typeCheck(formatExpression, contextInfo);
            adoptChildExpression(formatExpression);
        }
        for (Map.Entry<StructuredQName, Expression> entry : serializationAttributes.entrySet()) {
            Expression value = entry.getValue();
            if (!(value instanceof Literal)) {
                value = visitor.typeCheck(value, contextInfo);
                adoptChildExpression(value);
                serializationAttributes.put(entry.getKey(), value);
            }
        }
        try {
            DocumentInstr.checkContentSequence(visitor.getStaticContext(), content, validationOptions);
        } catch (XPathException err) {
            err.maybeSetLocation(this);
            throw err;
        }
        return this;
    }

    /*@NotNull*/
    public Expression optimize(ExpressionVisitor visitor, ContextItemStaticInfo contextItemType) throws XPathException {
        content = visitor.optimize(content, contextItemType);
        adoptChildExpression(content);
        if (href != null) {
            href = visitor.optimize(href, contextItemType);
            adoptChildExpression(href);
        }
        if (formatExpression != null) {
            formatExpression = visitor.optimize(formatExpression, contextItemType);
            adoptChildExpression(formatExpression);
            // TODO: if the formatExpression is now a constant, could get the output properties now
        }
        for (Map.Entry<StructuredQName, Expression> entry : serializationAttributes.entrySet()) {
            Expression value = entry.getValue();
            if (!(value instanceof Literal)) {
                value = visitor.optimize(value, contextItemType);
                adoptChildExpression(value);
                serializationAttributes.put(entry.getKey(), value);
            }
        }
        return this;
    }

    public int getIntrinsicDependencies() {
        return StaticProperty.HAS_SIDE_EFFECTS;
    }

    /**
     * Copy an expression. This makes a deep copy.
     *
     * @return the copy of the original expression
     */

    /*@NotNull*/
    public Expression copy() {
        ResultDocument r = new ResultDocument(
                globalProperties,
                localProperties,
                href == null ? null : href.copy(),
                formatExpression == null ? null : formatExpression.copy(),
                baseURI,
                getValidationAction(),
                getSchemaType(),
                serializationAttributes,
                characterMapIndex,
                nsResolver);
        r.content = content.copy();
        r.dynamicOutputElement = dynamicOutputElement == null ? null : dynamicOutputElement.copy();
        r.resolveAgainstStaticBase = resolveAgainstStaticBase;
        r.async = async;
        return r;
    }

    /**
     * Handle promotion offers, that is, non-local tree rewrites.
     *
     * @param offer The type of rewrite being offered
     * @throws XPathException
     */

    protected void promoteInst(PromotionOffer offer) throws XPathException {
        content = doPromotion(content, offer);
        if (href != null) {
            href = doPromotion(href, offer);
        }
        for (Map.Entry<StructuredQName, Expression> entry : serializationAttributes.entrySet()) {
            Expression value = entry.getValue();
            if (!(value instanceof Literal)) {
                value = doPromotion(value, offer);
                serializationAttributes.put(entry.getKey(), value);
            }
        }
    }

    /**
     * Get the name of this instruction for diagnostic and tracing purposes
     * (the string "xsl:result-document")
     */

    public int getInstructionNameCode() {
        return StandardNames.XSL_RESULT_DOCUMENT;
    }

    /**
     * Get the item type of the items returned by evaluating this instruction
     *
     * @return the static item type of the instruction. This is empty: the result-document instruction
     * returns nothing.
     */

    /*@NotNull*/
    public ItemType getItemType() {
        return ErrorType.getInstance();
    }

    @Override
    public Iterable<Operand> operands() {
        ArrayList<Operand> list = new ArrayList<Operand>(6);
        list.add(new Operand(content, OperandRole.SINGLE_ATOMIC));
        if (href != null) {
            list.add(new Operand(href, OperandRole.SINGLE_ATOMIC));
        }
        if (formatExpression != null) {
            list.add(new Operand(formatExpression, OperandRole.SINGLE_ATOMIC));
        }
        for (Expression e : serializationAttributes.values()) {
            list.add(new Operand(e, OperandRole.SINGLE_ATOMIC));
        }
        if (dynamicOutputElement != null) {
            list.add(new Operand(dynamicOutputElement, OperandRole.SINGLE_ATOMIC));
        }
        return list;
    }

    /**
     * Replace one subexpression by a replacement subexpression
     *
     * @param original    the original subexpression
     * @param replacement the replacement subexpression
     * @return true if the original subexpression is found
     */

    public boolean replaceOperand(Expression original, Expression replacement) {
        boolean found = false;
        if (content == original) {
            content = replacement;
            found = true;
        }
        if (href == original) {
            href = replacement;
            found = true;
        }

        for (Map.Entry<StructuredQName, Expression> entry : serializationAttributes.entrySet()) {
            if (entry.getValue() == original) {
                serializationAttributes.put(entry.getKey(), replacement);
                found = true;
            }
        }
        if (dynamicOutputElement == original) {
            dynamicOutputElement = replacement;
            found = true;
        }
        return found;
    }

    /**
     * Add a representation of this expression to a PathMap. The PathMap captures a map of the nodes visited
     * by an expression in a source tree.
     * <p/>
     * <p>The default implementation of this method assumes that an expression does no navigation other than
     * the navigation done by evaluating its subexpressions, and that the subexpressions are evaluated in the
     * same context as the containing expression. The method must be overridden for any expression
     * where these assumptions do not hold. For example, implementations exist for AxisExpression, ParentExpression,
     * and RootExpression (because they perform navigation), and for the doc(), document(), and collection()
     * functions because they create a new navigation root. Implementations also exist for PathExpression and
     * FilterExpression because they have subexpressions that are evaluated in a different context from the
     * calling expression.</p>
     *
     * @param pathMap        the PathMap to which the expression should be added
     * @param pathMapNodeSet the PathMapNodeSet to which the paths embodied in this expression should be added
     * @return the pathMapNodeSet representing the points in the source document that are both reachable by this
     * expression, and that represent possible results of this expression. For an expression that does
     * navigation, it represents the end of the arc in the path map that describes the navigation route. For other
     * expressions, it is the same as the input pathMapNode.
     */

    public PathMap.PathMapNodeSet addToPathMap(PathMap pathMap, PathMap.PathMapNodeSet pathMapNodeSet) {
        PathMap.PathMapNodeSet result = super.addToPathMap(pathMap, pathMapNodeSet);
        result.setReturnable(false);
        return new PathMap.PathMapNodeSet(pathMap.makeNewRoot(this));
    }


    public TailCall processLeavingTail(XPathContext context) throws XPathException {
        process(content, context);
        return null;
    }

    public void process(Expression content, XPathContext context) throws XPathException {
        if (context.getTemporaryOutputState() != 0) {
            // Check this before starting a new thread (because in temporary output state the context is sometimes
            // not sufficiently shared to allow the error to be passed back
            XPathException err = new XPathException("Cannot execute xsl:result-document while evaluating xsl:" +
                    context.getNamePool().getDisplayName(context.getTemporaryOutputState()));
            err.setErrorCode("XTDE1480");
            err.setLocator(this);
            throw err;
        }
        context.getConfiguration().processResultDocument(this, content, context);
    }

    /**
     * Evaluation method designed for calling from compiled bytecode.
     *
     * @param content The content expression. When called from bytecode, this will be the compiled version
     *                of the interpreted content expression
     * @param context dynamic evaluation context
     * @throws XPathException if a dynamic error occurs
     */

    public void processInstruction(Expression content, XPathContext context) throws XPathException {
        final Controller controller = context.getController();
        assert controller != null;
        SequenceReceiver saved = context.getReceiver();
        String savedOutputUri = context.getCurrentOutputUri();
        if (context.getTemporaryOutputState() != 0) {
            XPathException err = new XPathException("Cannot execute xsl:result-document while evaluating xsl:" +
                    context.getNamePool().getDisplayName(context.getTemporaryOutputState()));
            err.setErrorCode("XTDE1480");
            err.setLocator(this);
            throw err;
        }

        Properties computedLocalProps = gatherOutputProperties(context);
        String nextInChain = computedLocalProps.getProperty(SaxonOutputKeys.NEXT_IN_CHAIN);
        //TODO: reinstate this code
//        if (nextInChain != null && nextInChain.length() > 0) {
//            try {
//                result = sf.prepareNextStylesheet(controller, nextInChain, baseURI, result);
//            } catch (TransformerException e) {
//                throw XPathException.makeXPathException(e);
//            }
//        }

        Receiver out;
        OutputURIResolver resolver = null;
        Result result = null;
        if (href == null) {
            if (!computedLocalProps.propertyNames().hasMoreElements()) {
                out = controller.getPrincipalResult();
            } else if (controller.getPrincipalResult() instanceof ReconfigurableSerializer) {
                ReconfigurableSerializer rs = (ReconfigurableSerializer) controller.getPrincipalResult();
                rs.reconfigure(computedLocalProps, characterMapIndex);
                out = rs;
            } else {
                // assume we're not serializing
                out = controller.getPrincipalResult();
                //throw new IllegalStateException();
            }
            String resultURI = controller.getBaseOutputURI();
            if (resultURI == null) {
                resultURI = Controller.ANONYMOUS_PRINCIPAL_OUTPUT_URI;
            }
            checkAcceptableUri(context, resultURI);
            context.changeOutputDestination(out, getValidationOptions());
            context.setCurrentOutputUri(resultURI);
            out = context.getReceiver();
        } else {
            resolver = controller.getOutputURIResolver().newInstance();

            try {
                result = getResult(href, baseURI, context, resolver, resolveAgainstStaticBase);
            } catch (XPathException e) {
                e.maybeSetLocation(this);
                throw e;
            }
            SerializerFactory sf = context.getConfiguration().getSerializerFactory();

            PipelineConfiguration pipe = controller.makePipelineConfiguration();
            pipe.setHostLanguage(Configuration.XSLT);
            LocationProvider provider = pipe.getLocationProvider();
            out = sf.getReceiver(result, pipe, computedLocalProps, characterMapIndex);
            context.changeOutputDestination(out, getValidationOptions());
            context.setCurrentOutputUri(result.getSystemId());
            out = context.getReceiver();
            // changeOutputDestination changes the location provider in a way we don't want (affects validation errors)
            pipe.setLocationProvider(provider);
        }

        out.open();
        try {
            out.startDocument(0);
            content.process(context);
            out.endDocument();
        } catch (XPathException err) {
            err.setXPathContext(context);
            err.maybeSetLocation(this);
            throw err;
        } finally {
            out.close();
        }
        context.setReceiver(saved);
        context.setCurrentOutputUri(savedOutputUri);
        if (resolver != null) {
            try {
                //System.err.println("Trying to close " + result);
                resolver.close(result);
            } catch (TransformerException e) {
                throw XPathException.makeXPathException(e);
            }
        }
    }

    public static Result getResult(Expression href, String baseURI,
                                   XPathContext context, OutputURIResolver resolver,
                                   boolean resolveAgainstStaticBase) throws XPathException {
        String resultURI;
        Result result;
        Controller controller = context.getController();
        if (href == null) {
            result = controller.getPrincipalResult();
            resultURI = controller.getBaseOutputURI();
            if (resultURI == null) {
                resultURI = Controller.ANONYMOUS_PRINCIPAL_OUTPUT_URI;
            }
        } else {
            try {
                String base;
                if (resolveAgainstStaticBase) {
                    base = baseURI;
                } else {
                    base = controller.getCookedBaseOutputURI();
                }

                String hrefValue = EscapeURI.iriToUri(href.evaluateAsString(context)).toString();
                if (hrefValue.equals("")) {
                    result = controller.getPrincipalResult();
                    resultURI = controller.getBaseOutputURI();
                    if (resultURI == null) {
                        resultURI = Controller.ANONYMOUS_PRINCIPAL_OUTPUT_URI;
                    }
                } else {
                    try {
                        result = (resolver == null ? null : resolver.resolve(hrefValue, base));
                        //System.err.println("Resolver returned " + result);
                    } catch (TransformerException err) {
                        throw XPathException.makeXPathException(err);
                    } catch (Exception err) {
                        err.printStackTrace();
                        throw new XPathException("Exception thrown by OutputURIResolver", err);
                    }
                    if (result == null) {
                        resolver = StandardOutputResolver.getInstance();
                        result = resolver.resolve(hrefValue, base);
                    }
                    resultURI = result.getSystemId();
                    if (resultURI == null) {
                        try {
                            resultURI = new URI(base).resolve(hrefValue).toString();
                            result.setSystemId(resultURI);
                        } catch (Exception err) {
                            // no action
                        }
                    }
                }
            } catch (TransformerException e) {
                throw XPathException.makeXPathException(e);
            }
        }
        checkAcceptableUri(context, resultURI);
        traceDestination(context, result);
        return result;
    }

    public static void traceDestination(XPathContext context, Result result) {
        Configuration config = context.getConfiguration();
        boolean timing = config.isTiming();
        if (timing) {
            String dest = result.getSystemId();
            if (dest == null) {
                if (result instanceof StreamResult) {
                    dest = "anonymous output stream";
                } else if (result instanceof SAXResult) {
                    dest = "SAX2 ContentHandler";
                } else if (result instanceof DOMResult) {
                    dest = "DOM tree";
                } else {
                    dest = result.getClass().getName();
                }
            }
            config.getStandardErrorOutput().println("Writing to " + dest);
        }
    }

    public static void checkAcceptableUri(XPathContext context, String uri) throws XPathException {
        Controller controller = context.getController();
        assert controller != null;
        //String uri = result.getSystemId();
        if (uri != null) {
            if (controller.getDocumentPool().find(uri) != null) {
                XPathException err = new XPathException("Cannot write to a URI that has already been read: " +
                        (uri.equals(Controller.ANONYMOUS_PRINCIPAL_OUTPUT_URI) ? "(implicit output URI)" : uri));
                err.setXPathContext(context);
                err.setErrorCode("XTRE1500");
                throw err;
            }

            DocumentURI documentKey = new DocumentURI(uri);
            //noinspection SynchronizationOnLocalVariableOrMethodParameter
            synchronized (controller) {
                if (!controller.checkUniqueOutputDestination(documentKey)) {
                    XPathException err = new XPathException("Cannot write more than one result document to the same URI: " +
                            (uri.equals(Controller.ANONYMOUS_PRINCIPAL_OUTPUT_URI) ? "(implicit output URI)" : uri));
                    err.setXPathContext(context);
                    err.setErrorCode("XTDE1490");
                    throw err;
                } else {
                    controller.addUnavailableOutputDestination(documentKey);
                }
            }
        }
        controller.setThereHasBeenAnExplicitResultDocument();
    }

    /**
     * Create a properties object that combines the serialization properties specified
     * on the xsl:result-document itself with those specified in the referenced xsl:output declaration
     *
     * @param context The XPath evaluation context
     * @return the assembled properties
     * @throws XPathException if invalid properties are found
     */

    public Properties gatherOutputProperties(XPathContext context) throws XPathException {
        Controller controller = context.getController();
        assert controller != null;
        Configuration config = context.getConfiguration();
        Properties computedGlobalProps = globalProperties;

        if (formatExpression != null) {
            // format was an AVT and now needs to be computed
            CharSequence format = formatExpression.evaluateAsString(context);
            String[] parts;
            try {
                parts = NameChecker.getQNameParts(format);
            } catch (QNameException e) {
                XPathException err = new XPathException("The requested output format " + Err.wrap(format) + " is not a valid QName");
                err.setErrorCode("XTDE1460");
                err.setXPathContext(context);
                throw err;
            }
            String uri = nsResolver.getURIForPrefix(parts[0], false);
            if (uri == null) {
                XPathException err = new XPathException("The namespace prefix in the format name " + format + " is undeclared");
                err.setErrorCode("XTDE1460");
                err.setXPathContext(context);
                throw err;
            }
            StructuredQName qName = new StructuredQName(parts[0], uri, parts[1]);
            //TODO: reinstate
//            computedGlobalProps = getExecutable().getOutputProperties(qName);
//            if (computedGlobalProps == null) {
//                XPathException err = new XPathException("There is no xsl:output format named " + format);
//                err.setErrorCode("XTDE1460");
//                err.setXPathContext(context);
//                throw err;
//            }

        }

        // Now combine the properties specified on xsl:result-document with those specified on xsl:output

        Properties computedLocalProps = new Properties(computedGlobalProps);

        // First handle the properties with fixed values on xsl:result-document

        for (Object keyo : localProperties.keySet()) {
            String key = (String) keyo;
            String[] parts = NamePool.parseClarkName(key);
            try {
                setSerializationProperty(computedLocalProps, parts[0], parts[1],
                        localProperties.getProperty(key), nsResolver, true, config);
            } catch (XPathException e) {
                e.setErrorCode("XTDE0030");
                e.maybeSetLocation(this);
                throw e;
            }
        }

        // Now add the properties that were specified as AVTs

        if (serializationAttributes.size() > 0) {
            for (Map.Entry<StructuredQName, Expression> entry : serializationAttributes.entrySet()) {
                String value = entry.getValue().evaluateAsString(context).toString();
                String lname = entry.getKey().getLocalPart();
                String uri = entry.getKey().getURI();
                try {
                    setSerializationProperty(computedLocalProps, uri, lname, value, nsResolver, false, config);
                } catch (XPathException e) {
                    e.setErrorCode("XTDE0030");
                    e.maybeSetLocation(this);
                    e.maybeSetContext(context);
                    if (NamespaceConstant.SAXON.equals(e.getErrorCodeNamespace()) &&
                            "SXWN".equals(e.getErrorCodeLocalPart().substring(0, 4))) {
                        controller.getErrorListener().warning(e);
                    } else {
                        throw e;
                    }
                }
            }
        }

        // Handle properties specified using a dynamic xsl:output element
        // (Used when the instruction is generated from a saxon:result-document extension function call)

        if (dynamicOutputElement != null) {
            Item outputArg = dynamicOutputElement.evaluateItem(context);
            if (!(outputArg instanceof NodeInfo &&
                    ((NodeInfo) outputArg).getNodeKind() == Type.ELEMENT &&
                    ((NodeInfo) outputArg).getFingerprint() == StandardNames.XSL_OUTPUT)) {
                XPathException err = new XPathException(
                        "The third argument of saxon:result-document must be an <xsl:output> element");
                err.setLocator(this);
                err.setXPathContext(context);
                throw err;
            }
            Properties dynamicProperties = new Properties();
            processXslOutputElement((NodeInfo) outputArg, dynamicProperties, context);
            for (Object o : dynamicProperties.keySet()) {
                String key = (String) o;
                StructuredQName name = StructuredQName.fromClarkName(key);
                String value = dynamicProperties.getProperty(key);
                try {
                    setSerializationProperty(
                            computedLocalProps, name.getURI(), name.getLocalPart(),
                            value, nsResolver, false, config);
                } catch (XPathException e) {
                    e.setErrorCode("XTDE0030");
                    e.maybeSetLocation(this);
                    e.maybeSetContext(context);
                    throw e;
                }
            }
        }
        return computedLocalProps;
    }

    /**
     * Validate a serialization property and add its value to a Properties collection
     *
     * @param details      the properties to be updated
     * @param uri          the uri of the property name
     * @param lname        the local part of the property name
     * @param value        the value of the serialization property. In the case of QName-valued values,
     *                     this will use lexical QNames if prevalidated is false and a NamespaceResolver is supplied;
     *                     otherwise they will use Clark-format names
     * @param nsResolver   resolver for lexical QNames; not needed if prevalidated, or if QNames are supplied in Clark
     *                     format
     * @param prevalidated true if values are already known to be valid and lexical QNames have been
     *                     expanded into Clark notation
     * @param config       the Saxon configuration
     * @throws XPathException if any serialization property has an invalid value
     */

    public static void setSerializationProperty(Properties details, String uri, String lname,
                                                String value, /*@Nullable*/ NamespaceResolver nsResolver,
                                                boolean prevalidated, Configuration config)
            throws XPathException {

        if (uri.length() == 0 || NamespaceConstant.SAXON.equals(uri)) {
            if (lname.equals(StandardNames.METHOD)) {
                if (value.equals("xml") || value.equals("html") ||
                        value.equals("text") || value.equals("xhtml") || prevalidated || value.startsWith("{")) {
                    details.setProperty(OutputKeys.METHOD, value);
                } else {
                    String[] parts;
                    try {
                        parts = NameChecker.getQNameParts(value);
                        String prefix = parts[0];
                        if (prefix.length() == 0) {
                            XPathException err = new XPathException("method must be xml, html, xhtml, or text, or a prefixed name");
                            err.setErrorCode("SEPM0016");
                            err.setIsStaticError(true);
                            throw err;
                        } else if (nsResolver != null) {
                            String muri = nsResolver.getURIForPrefix(prefix, false);
                            if (muri == null) {
                                XPathException err = new XPathException("Namespace prefix '" + prefix + "' has not been declared");
                                err.setErrorCode("SEPM0016");
                                err.setIsStaticError(true);
                                throw err;
                            }
                            details.setProperty(OutputKeys.METHOD, '{' + muri + '}' + parts[1]);
                        } else {
                            details.setProperty(OutputKeys.METHOD, value);
                        }
                    } catch (QNameException e) {
                        XPathException err = new XPathException("Invalid method name. " + e.getMessage());
                        err.setErrorCode("SEPM0016");
                        err.setIsStaticError(true);
                        throw err;
                    }
                }
            } else if (lname.equals(StandardNames.USE_CHARACTER_MAPS)) {
                // The use-character-maps attribute is always turned into a Clark-format name at compile time
                String existing = details.getProperty(SaxonOutputKeys.USE_CHARACTER_MAPS);
                if (existing == null) {
                    existing = "";
                }
                details.setProperty(SaxonOutputKeys.USE_CHARACTER_MAPS, existing + value);
            } else if (lname.equals("cdata-section-elements")) {
                processListOfNodeNames(details, OutputKeys.CDATA_SECTION_ELEMENTS, value, nsResolver, true, prevalidated);
            } else if (lname.equals("suppress-indentation")) {
                processListOfNodeNames(details, SaxonOutputKeys.SUPPRESS_INDENTATION, value, nsResolver, true, prevalidated);
            } else if (lname.equals("double-space")) {
                processListOfNodeNames(details, SaxonOutputKeys.DOUBLE_SPACE, value, nsResolver, true, prevalidated);
            } else if (lname.equals("attribute-order")) {
                processListOfNodeNames(details, SaxonOutputKeys.ATTRIBUTE_ORDER, value, nsResolver, false, prevalidated);
            } else if (lname.equals("next-in-chain")) {
                XPathException e = new XPathException("saxon:next-in-chain property is available only on xsl:output");
                e.setErrorCodeQName(
                        new StructuredQName("saxon", NamespaceConstant.SAXON, SaxonErrorCode.SXWN9004));
                throw e;
            } else {
                // all other properties in the default or Saxon namespaces
                if (lname.equals("output-version")) {
                    lname = "version";
                }
                String clarkName = lname;
                if (uri.length() != 0) {
                    clarkName = '{' + uri + '}' + lname;
                }
                if (!prevalidated) {
                    try {
                        value = SaxonOutputKeys.checkOutputProperty(clarkName, Whitespace.trim(value), config, true); //TODO: test whether xslt3.0 is allowed
                    } catch (XPathException err) {
                        err.maybeSetErrorCode("SEPM0016");
                        throw err;
                    }
                }
                details.setProperty(clarkName, value);
            }
        } else {
            // properties in user-defined namespaces
            details.setProperty('{' + uri + '}' + lname, value);
        }

    }

    private static void processListOfNodeNames(Properties details, String key, String value,
                                               NamespaceResolver nsResolver, boolean useDefaultNS, boolean prevalidated) throws XPathException {
        String existing = details.getProperty(key);
        if (existing == null) {
            existing = "";
        }
        String s = SaxonOutputKeys.parseListOfNodeNames(value, nsResolver, useDefaultNS, prevalidated, "SEPM0016");
        details.setProperty(key, existing + s);
    }

    /**
     * Diagnostic print of expression structure. The abstract expression tree
     * is written to the supplied output destination.
     */

    public void explain(ExpressionPresenter out) {
        out.startElement("resultDocument");
        if (href != null) {
            out.startSubsidiaryElement("href");
            href.explain(out);
            out.endSubsidiaryElement();
        }
        out.startSubsidiaryElement("content");
        content.explain(out);
        out.endSubsidiaryElement();
        out.endElement();
    }

    /**
     * Construct a set of output properties from an xsl:output element supplied at run-time
     *
     * @param element an xsl:output element
     * @param props   Properties object to which will be added the values of those serialization properties
     *                that were specified
     * @param c       the XPath dynamic context
     * @throws net.sf.saxon.trans.XPathException if a dynamic error occurs
     */

    public static void processXslOutputElement(NodeInfo element, Properties props, XPathContext c) throws XPathException {
        SequenceIterator iter = element.iterateAxis(AxisInfo.ATTRIBUTE);
        NamespaceResolver resolver = new InscopeNamespaceResolver(element);
        while (true) {
            NodeInfo att = (NodeInfo) iter.next();
            if (att == null) {
                break;
            }
            String uri = att.getURI();
            String local = att.getLocalPart();
            String val = Whitespace.trim(att.getStringValueCS());
            setSerializationProperty(props, uri, local, val, resolver, false, c.getConfiguration());
        }
    }

//#ifdefined BYTECODE

    /**
     * Return the bytecode compiler for the ResultDocument expression
     *
     * @return the relevant ExpressionCompiler
     */
    @Override
    public ExpressionCompiler getExpressionCompiler() {
        return new ResultDocumentCompiler();
    }

//#endif

//#ifdefined STREAM

    /**
     * Get a class that supports streamed evaluation of this expression
     *
     * @return the relevant StreamingAdjunct, or null if none is available
     */
    @Override
    public ResultDocumentAdjunct getStreamingAdjunct() {
        return new ResultDocumentAdjunct();
    }


//#endif
}

