////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
// Copyright (c) 2014 Saxonica Limited.
// This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
// If a copy of the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
// This Source Code Form is "Incompatible With Secondary Licenses", as defined by the Mozilla Public License, v. 2.0.
////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

package net.sf.saxon.style;

import net.sf.saxon.Configuration;
import net.sf.saxon.expr.Component;
import net.sf.saxon.expr.Expression;
import net.sf.saxon.expr.Literal;
import net.sf.saxon.expr.TailCallLoop;
import net.sf.saxon.expr.instruct.SlotManager;
import net.sf.saxon.expr.instruct.TraceExpression;
import net.sf.saxon.expr.instruct.UserFunction;
import net.sf.saxon.expr.instruct.UserFunctionParameter;
import net.sf.saxon.expr.parser.*;
import net.sf.saxon.lib.NamespaceConstant;
import net.sf.saxon.om.*;
import net.sf.saxon.trans.SaxonErrorCode;
import net.sf.saxon.trans.SymbolicName;
import net.sf.saxon.trans.Visibility;
import net.sf.saxon.trans.XPathException;
import net.sf.saxon.tree.iter.AxisIterator;
import net.sf.saxon.tree.util.FastStringBuffer;
import net.sf.saxon.value.SequenceType;
import net.sf.saxon.value.Whitespace;

/**
 * Handler for xsl:function elements in stylesheet (XSLT 2.0). <BR>
 * Attributes: <br>
 * name gives the name of the function
 * saxon:memo-function=yes|no indicates whether it acts as a memo function.
 */

public class XSLFunction extends StyleElement implements StylesheetComponent {


    /*@Nullable*/ private String nameAtt = null;
    private String asAtt = null;
    private SequenceType resultType;
    private String functionName;
    private SlotManager stackFrameMap;
    private boolean memoFunction = false;
    private String overrideExtensionFunctionAtt = null;
    private boolean overrideExtensionFunction = true;
    private int numberOfArguments = -1;  // -1 means not yet known
    private UserFunction compiledFunction;
    private Visibility visibility;

    /**
     * Get the corresponding Procedure object that results from the compilation of this
     * StylesheetProcedure
     */
    public UserFunction getCompiledProcedure() {
        return compiledFunction;
    }

    /**
     * Ask whether this node is a declaration, that is, a permitted child of xsl:stylesheet
     * (including xsl:include and xsl:import).
     *
     * @return true for this element
     */

    @Override
    public boolean isDeclaration() {
        return true;
    }

    public void prepareAttributes() throws XPathException {

        AttributeCollection atts = getAttributeList();

        overrideExtensionFunctionAtt = null;
        String visibilityAtt = null;
        String cacheAtt = null;
        String identitySensitiveAtt = null;

        for (int a = 0; a < atts.getLength(); a++) {
            String uri = atts.getURI(a);
            String local = atts.getLocalName(a);
            if ("".equals(uri)) {
                if (local.equals(StandardNames.NAME)) {
                    nameAtt = Whitespace.trim(atts.getValue(a));
                    assert nameAtt != null;
                    if (nameAtt.indexOf(':') < 0) {
                        compileError("Function name must have a namespace prefix", "XTSE0740");
                    }
                    try {
                        setObjectName(makeQName(nameAtt));
                    } catch (NamespaceException err) {
                        compileError(err.getMessage(), "XTSE0280");
                    } catch (XPathException err) {
                        compileError(err);
                    }
                } else if (local.equals(StandardNames.AS)) {
                    asAtt = atts.getValue(a);
                } else if (local.equals(StandardNames.VISIBILITY)) {
                    visibilityAtt = Whitespace.trim(atts.getValue(a));
                } else if (local.equals(StandardNames.OVERRIDE)) {
                    String overrideAtt = Whitespace.trim(atts.getValue(a));
                    boolean override = processBooleanAttribute(StandardNames.OVERRIDE, overrideAtt);
                    if (overrideExtensionFunctionAtt != null) {
                        if (override != overrideExtensionFunction) {
                            compileError("Attributes override-extension-function and override are both used, but do not match", "XTSE0020");
                        }
                    } else {
                        overrideExtensionFunctionAtt = overrideAtt;
                        overrideExtensionFunction = override;
                    }
                    if (isXslt30Processor()) {
                        compileWarning("The xsl:function/@override attribute is deprecated; use override-extension-function", SaxonErrorCode.SXWN9014);
                    }
                } else if (local.equals(StandardNames.OVERRIDE_EXTENSION_FUNCTION) && isXslt30Processor()) {
                    String overrideExtAtt = Whitespace.trim(atts.getValue(a));
                    boolean overrideExt = processBooleanAttribute(StandardNames.OVERRIDE_EXTENSION_FUNCTION, overrideExtAtt);
                    if (overrideExtensionFunctionAtt != null) {
                        if (overrideExt != overrideExtensionFunction) {
                            compileError("Attributes override-extension-function and override are both used, but do not match", "XTSE0020");
                        }
                    } else {
                        overrideExtensionFunctionAtt = overrideExtAtt;
                        overrideExtensionFunction = overrideExt;
                    }
                    if (local.equals(StandardNames.OVERRIDE)) {
                        compileWarning("The xsl:function/@override attribute is deprecated; use override-extension-function", SaxonErrorCode.SXWN9014);
                    }
                } else if (local.equals(StandardNames.CACHE)) {
                    cacheAtt = Whitespace.trim(atts.getValue(a));
                } else if (local.equals(StandardNames.IDENTITY_SENSITIVE)) {
                    identitySensitiveAtt = Whitespace.trim(atts.getValue(a));
                } else {
                    checkUnknownAttribute(atts.getNodeName(a));
                }
            } else if (local.equals("memo-function") && uri.equals(NamespaceConstant.SAXON)) {
                memoFunction = processBooleanAttribute("saxon:memo-function", atts.getValue(a));
            } else {
                checkUnknownAttribute(atts.getNodeName(a));
            }
        }

        if (nameAtt == null) {
            reportAbsence("name");
            nameAtt = "xsl:unnamed-function-" + generateId();
        }

        if (asAtt == null) {
            resultType = SequenceType.ANY_SEQUENCE;
        } else {
            resultType = makeSequenceType(asAtt);
        }

        if (visibilityAtt == null) {
            visibility = Visibility.PRIVATE;
        } else if (!isXslt30Processor()) {
            compileError("The xsl:function/@visibility attribute requires XSLT 3.0 to be enabled", "XTSE0020");
        } else {
            visibility = getVisibilityValue(visibilityAtt, "");
        }

        boolean identitySensitive = true;
        if (identitySensitiveAtt != null) {
            identitySensitive = processBooleanAttribute(StandardNames.IDENTITY_SENSITIVE, identitySensitiveAtt);
        }

        boolean cache = false;
        if (cacheAtt != null) {
            if ("full".equals(cacheAtt)) {
                cache = true;
            } else if ("partial".equals(cacheAtt)) {
                cache = true;
            } else if ("no".equals(cacheAtt)) {
                cache = false;
            } else {
                invalidAttribute("cache", "full|partial|no");
            }
        }

        if (cache && !identitySensitive) {
            memoFunction = true;
        }

        functionName = nameAtt;
    }

    private String generateId() {
        FastStringBuffer buff = new FastStringBuffer(FastStringBuffer.TINY);
        generateId(buff);
        return buff.toString();
    }

    /**
     * Get a name identifying the object of the expression, for example a function name, template name,
     * variable name, key name, element name, etc. This is used only where the name is known statically.
     * If there is no name, the value will be -1.
     */

    /*@NotNull*/
    public StructuredQName getObjectName() {
        StructuredQName qn = super.getObjectName();
        if (qn == null) {
            nameAtt = Whitespace.trim(getAttributeValue("", "name"));
            if (nameAtt == null) {
                return new StructuredQName("saxon", NamespaceConstant.SAXON, "badly-named-function" + generateId());
            }
            try {
                qn = makeQName(nameAtt);
                setObjectName(qn);
            } catch (NamespaceException err) {
                return new StructuredQName("saxon", NamespaceConstant.SAXON, "badly-named-function" + generateId());
            } catch (XPathException err) {
                return new StructuredQName("saxon", NamespaceConstant.SAXON, "badly-named-function" + generateId());
            }
        }
        return qn;
    }

    /**
     * Determine whether this type of element is allowed to contain a template-body.
     *
     * @return true: yes, it may contain a general template-body
     */

    public boolean mayContainSequenceConstructor() {
        return true;
    }

    protected boolean mayContainParam(String attName) {
        return !"required".equals(attName);
    }

    /**
     * Specify that xsl:param is a permitted child
     */

    protected boolean isPermittedChild(StyleElement child) {
        return child instanceof XSLLocalParam;
    }

    public Visibility getVisibility() {
        if (visibility == null) {
            try {
                String vAtt = getAttributeValue("", "visibility");
                return vAtt == null ? Visibility.PRIVATE : getVisibilityValue(Whitespace.trim(vAtt), "");
            } catch (XPathException e) {
                return Visibility.PRIVATE;
            }
        }
        return visibility;
    }

    public SymbolicName getSymbolicName() {
        return new SymbolicName(StandardNames.XSL_FUNCTION, getObjectName(), getNumberOfArguments());
    }

    public void checkCompatibility(Component component) throws XPathException {
        if (compiledFunction == null) {
            getCompiledFunction();
        }

        UserFunction other = (UserFunction) component.getProcedure();
        if (!compiledFunction.getSymbolicName().equals(other.getSymbolicName())) {
            // Can't happen
            compileError("The overriding xsl:function " + nameAtt + " does not match the overridden function: " +
                "the function name/arity does not match", "XTSE3070");
        }
        if (!compiledFunction.getResultType().equals(other.getResultType())) {
            compileError("The overriding xsl:function " + nameAtt + " does not match the overridden function: " +
                "the return type does not match", "XTSE3070");
        }

        for (int i = 0; i < getNumberOfArguments(); i++) {
            if (!compiledFunction.getArgumentType(i).equals(other.getArgumentType(i))) {
                compileError("The overriding xsl:function " + nameAtt + " does not match the overridden function: " +
                "the type of the " + RoleLocator.ordinal(i+1) + " argument does not match", "XTSE3070");
            }
        }
    }

    /**
     * Is override-extension-function="yes"?.
     *
     * @return true if override-extension-function="yes" was specified, otherwise false
     */

    public boolean isOverrideExtensionFunction() {
        if (overrideExtensionFunctionAtt == null) {
            // this is a forwards reference
            try {
                prepareAttributes();
            } catch (XPathException e) {
                // no action: error will be caught later
            }
        }
        return overrideExtensionFunction;
    }

    public void index(ComponentDeclaration decl, StylesheetPackage top) throws XPathException {
        //getSkeletonCompiledFunction();
        getCompiledFunction();
        top.indexFunction(decl);
    }

    /**
     * Notify all references to this function of the data type.
     *
     * @throws XPathException
     */

    public void fixupReferences() throws XPathException {
//        for (UserFunctionReference reference : references) {
//            if (reference instanceof UserFunctionCall) {
//                ((UserFunctionCall) reference).setStaticType(resultType);
//            }
//        }
        super.fixupReferences();
    }

    public void validate(ComponentDeclaration decl) throws XPathException {

        stackFrameMap = getConfiguration().makeSlotManager();

        // check the element is at the top level of the stylesheet

        checkTopLevel("XTSE0010", true);
        getNumberOfArguments();
//        if (compiledFunction != null) {
//            completeCompiledFunction();
//        }

    }


    /**
     * Compile the function definition to create an executable representation
     * The compileDeclaration() method has the side-effect of binding
     * all references to the function to the executable representation
     * (a UserFunction object)
     *
     * @throws XPathException
     */

    public void compileDeclaration(Compilation compilation, ComponentDeclaration decl) throws XPathException {
        compileAsExpression(compilation, decl);
    }

    /**
     * Compile the function into a UserFunction object, which treats the function
     * body as a single XPath expression. This involves recursively translating
     * xsl:variable declarations into let expressions, with the action part of the
     * let expression containing the rest of the function body.
     * <p/>
     * <p>The UserFunction that is created will be linked from all calls to
     * this function, so nothing else needs to be done with the result. If there are
     * no calls to it, the compiled function will be garbage-collected away.</p>
     *
     * @param compilation the Executable
     * @param decl        this xsl:function declaration
     * @throws XPathException if a failure occurs
     */

    private void compileAsExpression(Compilation compilation, ComponentDeclaration decl) throws XPathException {
        Expression exp = compileSequenceConstructor(compilation, decl, iterateAxis(AxisInfo.CHILD), false);
        if (exp == null) {
            exp = Literal.makeEmptySequence(this);
        } else if (Literal.isEmptySequence(exp)) {
            // no action
        } else {
            if (visibility == Visibility.ABSTRACT) {
                compileError("A function defined with visibility='abstract' must have no body", "XTSE0010");
            }
            ExpressionVisitor visitor = makeExpressionVisitor();
            exp = exp.simplify(visitor);
        }

        if (compilation.getConfiguration().isCompileWithTracing()) {
            TraceExpression trace = new TraceExpression(exp);
            trace.setConstructType(StandardNames.XSL_FUNCTION);
            trace.setNamespaceResolver(getNamespaceResolver());
            trace.setObjectName(getObjectName());
            exp = trace;
        }

        UserFunction fn = getCompiledFunction();
        fn.setBody(exp);
        fn.setStackFrameMap(stackFrameMap);
        bindParameterDefinitions(fn);

//        UserFunction fn = compilation.getConfiguration().newUserFunction(memoFunction);
//        fn.setPackageData(getCompilation().getPackageData());
//        fn.setHostLanguage(Configuration.XSLT);
//        fn.setBody(exp);
//        fn.setFunctionName(getObjectName());
//        setParameterDefinitions(fn);
//        fn.setResultType(getResultType());
//        fn.setLineNumber(getLineNumber());
//        fn.setSystemId(getSystemId());
//        fn.setStackFrameMap(stackFrameMap);
//        fn.makeDeclaringComponent(visibility, getContainingPackage());
        //fn.setExecutable(compilation);
        //compiledFunction = fn;
        //fixupInstruction(fn);
        //fn.makeDeclaringComponent(visibility, getContainingPackage());
        if (memoFunction && !fn.isMemoFunction()) {
            compileWarning("Memo functions are not available in Saxon-HE: saxon:memo-function attribute ignored",
                    SaxonErrorCode.SXWN9011);
        }
    }

    public void typeCheckBody() throws XPathException {
        if (visibility != Visibility.ABSTRACT) {
            Expression exp = compiledFunction.getBody();
            ExpressionTool.resetPropertiesWithinSubtree(exp);
            Expression exp2 = exp;
            ExpressionVisitor visitor = makeExpressionVisitor();
            try {
                // We've already done the typecheck of each XPath expression, but it's worth doing again at this
                // level because we have more information now.
                ContextItemStaticInfo info = ContextItemStaticInfo.ABSENT;
                exp2 = visitor.typeCheck(exp, info);
                if (resultType != null) {
                    RoleLocator role =
                            new RoleLocator(RoleLocator.FUNCTION_RESULT, functionName, 0);
                    role.setErrorCode("XTTE0780");
                    exp2 = TypeChecker.staticTypeCheck(exp2, resultType, false, role, visitor);
                }
            } catch (XPathException err) {
                err.maybeSetLocation(this);
                compileError(err);
            }
            if (exp2 != exp) {
                compiledFunction.setBody(exp2);
            }
        }
    }

    public void optimize(ComponentDeclaration declaration) throws XPathException {
        Expression exp = compiledFunction.getBody();
        ExpressionTool.resetPropertiesWithinSubtree(exp);
        ExpressionVisitor visitor = makeExpressionVisitor();
        Expression exp2 = exp;
        Optimizer opt = getConfiguration().obtainOptimizer();
        try {
            if (opt.getOptimizationLevel() != Optimizer.NO_OPTIMIZATION) {
                exp2 = exp.optimize(visitor, null);
            }

        } catch (XPathException err) {
            err.maybeSetLocation(this);
            compileError(err);
        }

        // Try to extract new global variables from the body of the function
        if (opt.getOptimizationLevel() != Optimizer.NO_OPTIMIZATION) {
            Expression exp3 = opt.promoteExpressionsToGlobal(exp2, getCompilation().getStylesheetPackage(), visitor);
            if (exp3 != null) {
                exp2 = visitor.optimize(exp3, null);
            }
        }

        // Add trace wrapper code if required
        exp2 = makeTraceInstruction(this, exp2);

        allocateSlots(exp2);
        if (exp2 != exp) {
            compiledFunction.setBody(exp2);
        }

        int tailCalls = ExpressionTool.markTailFunctionCalls(exp2, getObjectName(), getNumberOfArguments());
        if (tailCalls != 0) {
            compiledFunction.setTailRecursive(tailCalls > 0, tailCalls > 1);
            compiledFunction.setBody(new TailCallLoop(compiledFunction));
        }

        compiledFunction.computeEvaluationMode();

        if (isExplaining()) {
            exp2.explain(getConfiguration().getLogger());
        }
    }

    /**
     * Generate byte code if appropriate
     *
     * @param opt the optimizer
     * @throws net.sf.saxon.trans.XPathException
     *          if bytecode generation fails
     */
    public void generateByteCode(Optimizer opt) throws XPathException {
        // Generate byte code if appropriate

        if (getCompilation().getCompilerInfo().isGenerateByteCode()) {
            try {
                Expression cbody = opt.compileToByteCode(compiledFunction.getBody(), nameAtt,
                        Expression.PROCESS_METHOD | Expression.ITERATE_METHOD);
                if (cbody != null) {
                    compiledFunction.setBody(cbody);
                }
            } catch (Exception e) {
                System.err.println("Failed while compiling function " + nameAtt);
                e.printStackTrace();
                throw new XPathException(e);
            }
        }
    }

    /**
     * Get associated stack frame details.
     * @return the associated SlotManager object
     */

    public SlotManager getSlotManager() {
        return stackFrameMap;
    }

    /**
     * Get the type of value returned by this function
     *
     * @return the declared result type, or the inferred result type
     *         if this is more precise
     */
    public SequenceType getResultType() {
        if (resultType == null) {
            // may be handling a forwards reference - see hof-038
            String asAtt = getAttributeValue("", "as");
            if (asAtt != null) {
                try {
                    resultType = makeSequenceType(asAtt);
                } catch (XPathException err) {
                    // the error will be reported when we get round to processing the function declaration
                }
            }
        }
        return resultType == null ? SequenceType.ANY_SEQUENCE : resultType;
    }

    /**
     * Get the number of arguments declared by this function (that is, its arity).
     *
     * @return the arity of the function
     */

    public int getNumberOfArguments() {
        if (numberOfArguments == -1) {
            numberOfArguments = 0;
            AxisIterator kids = iterateAxis(AxisInfo.CHILD);
            while (true) {
                Item child = kids.next();
                if (child instanceof XSLLocalParam) {
                    numberOfArguments++;
                } else {
                    return numberOfArguments;
                }
            }
        }
        return numberOfArguments;
    }

    /**
     * Set the definitions of the parameters in the compiled function, as an array.
     *
     * @param fn the compiled object representing the user-written function
     */

    public void setParameterDefinitions(UserFunction fn) {
        UserFunctionParameter[] params = new UserFunctionParameter[getNumberOfArguments()];
        fn.setParameterDefinitions(params);
        int count = 0;
        AxisIterator kids = iterateAxis(AxisInfo.CHILD);
        while (true) {
            NodeInfo node = kids.next();
            if (node == null) {
                return;
            }
            if (node instanceof XSLLocalParam) {
                UserFunctionParameter param = new UserFunctionParameter();
                params[count++] = param;
                param.setRequiredType(((XSLLocalParam) node).getRequiredType());
                param.setVariableQName(((XSLLocalParam) node).getVariableQName());
                param.setSlotNumber(((XSLLocalParam) node).getSlotNumber());
            } else {
                break;
            }
        }
    }

    public void bindParameterDefinitions(UserFunction fn) {
        UserFunctionParameter[] params = fn.getParameterDefinitions();
        int count = 0;
        AxisIterator kids = iterateAxis(AxisInfo.CHILD);
        while (true) {
            NodeInfo node = kids.next();
            if (node == null) {
                return;
            }
            if (node instanceof XSLLocalParam) {
                UserFunctionParameter param = params[count++];
                param.setRequiredType(((XSLLocalParam) node).getRequiredType());
                param.setVariableQName(((XSLLocalParam) node).getVariableQName());
                param.setSlotNumber(((XSLLocalParam) node).getSlotNumber());
                ((XSLLocalParam) node).getSourceBinding().fixupBinding(param);
                int refs = ExpressionTool.getReferenceCount(fn.getBody(), param, false);
                param.setReferenceCount(refs);
            }
        }
    }

    /**
     * Get the argument types
     *
     * @return the declared types of the arguments
     */

    public SequenceType[] getArgumentTypes() {
        SequenceType[] types = new SequenceType[getNumberOfArguments()];
        int count = 0;
        AxisIterator kids = iterateAxis(AxisInfo.CHILD);
        while (true) {
            NodeInfo node = kids.next();
            if (node == null) {
                return types;
            }
            if (node instanceof XSLLocalParam) {
                types[count++] = ((XSLLocalParam) node).getRequiredType();
            }
        }
    }

    /**
     * Get the compiled function
     *
     * @return the object representing the compiled user-written function
     */

    public UserFunction getCompiledFunction() {
        if (compiledFunction == null) {
            try {
                prepareAttributes();
                UserFunction fn = getConfiguration().newUserFunction(memoFunction);
                fn.setPackageData(getCompilation().getPackageData());
                fn.setHostLanguage(Configuration.XSLT);
                fn.setFunctionName(getObjectName());
                setParameterDefinitions(fn);
                fn.setResultType(getResultType());
                fn.setLineNumber(getLineNumber());
                fn.setSystemId(getSystemId());
                fn.makeDeclaringComponent(visibility, getContainingPackage());
                compiledFunction = fn;
            } catch (XPathException err) {
                return null;
            }
        }
        return compiledFunction;
    }

//    private void completeCompiledFunction() {
//        // Complete the creation of the component now that schema information is available.
//        setParameterDefinitions(compiledFunction);
//        compiledFunction.setResultType(getResultType());
//    }

    /**
     * Get the type of construct. This will be a constant in
     * class {@link net.sf.saxon.trace.Location}. This method is part of the
     * {@link net.sf.saxon.trace.InstructionInfo} interface
     */

    public int getConstructType() {
        return StandardNames.XSL_FUNCTION;
    }

}

