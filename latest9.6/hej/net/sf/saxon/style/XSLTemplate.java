////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
// Copyright (c) 2014 Saxonica Limited.
// This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
// If a copy of the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
// This Source Code Form is "Incompatible With Secondary Licenses", as defined by the Mozilla Public License, v. 2.0.
////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

package net.sf.saxon.style;

import net.sf.saxon.expr.Component;
import net.sf.saxon.expr.Expression;
import net.sf.saxon.expr.Literal;
import net.sf.saxon.expr.StaticProperty;
import net.sf.saxon.expr.instruct.LocalParam;
import net.sf.saxon.expr.instruct.SlotManager;
import net.sf.saxon.expr.instruct.Template;
import net.sf.saxon.expr.instruct.TraceExpression;
import net.sf.saxon.expr.parser.*;
import net.sf.saxon.lib.Logger;
import net.sf.saxon.om.*;
import net.sf.saxon.pattern.Pattern;
import net.sf.saxon.trans.*;
import net.sf.saxon.tree.iter.AxisIterator;
import net.sf.saxon.type.AnyItemType;
import net.sf.saxon.type.ErrorType;
import net.sf.saxon.type.ItemType;
import net.sf.saxon.type.Type;
import net.sf.saxon.value.DecimalValue;
import net.sf.saxon.value.SequenceType;
import net.sf.saxon.value.Whitespace;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.StringTokenizer;

/**
 * An xsl:template element in the style sheet.
 */

public final class XSLTemplate extends StyleElement implements StylesheetComponent {

    private String matchAtt = null;
    private String modeAtt = null;
    private String nameAtt = null;
    private String priorityAtt = null;
    private String asAtt = null;
    private String visibilityAtt = null;

    private StructuredQName[] modeNames;
    private String diagnosticId;
    private Pattern match;
    private boolean prioritySpecified;
    private double priority;
    private SlotManager stackFrameMap;
    private Template compiledTemplate = new Template();
    private SequenceType requiredType = null;
    private boolean hasRequiredParams = false;
    private boolean isTailRecursive = false;
    private Visibility visibility = Visibility.PRIVATE;

    /**
     * Get the corresponding Procedure object that results from the compilation of this
     * StylesheetProcedure
     */
    public Template getCompiledProcedure() {
        return compiledTemplate;
    }

    @Override
    public void setCompilation(Compilation compilation) {
        super.setCompilation(compilation);
        compiledTemplate.setPackageData(compilation.getPackageData());
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

    /**
     * Determine whether this type of element is allowed to contain a template-body
     *
     * @return true: yes, it may contain a template-body
     */

    public boolean mayContainSequenceConstructor() {
        return true;
    }

    protected boolean mayContainParam(String attName) {
        return true;
    }

    /**
     * Specify that xsl:param is a permitted child
     */

    protected boolean isPermittedChild(StyleElement child) {
        return child instanceof XSLLocalParam;
    }

    /**
     * Return the name of this template. Note that this may
     * be called before prepareAttributes has been called.
     *
     * @return the name of the template as a Structured QName.
     */

    /*@Nullable*/
    public StructuredQName getTemplateName() {

        //We use null to mean "not yet evaluated"

        try {
            if (getObjectName() == null) {
                // allow for forwards references
                String nameAtt = getAttributeValue("", StandardNames.NAME);
                if (nameAtt != null) {
                    setObjectName(makeQName(nameAtt));
                }
            }
            return getObjectName();
        } catch (NamespaceException err) {
            return null;          // the errors will be picked up later
        } catch (XPathException err) {
            return null;
        }
    }

    public SymbolicName getSymbolicName() {
        if (getTemplateName() == null) {
            return null;
        } else {
            return new SymbolicName(StandardNames.XSL_TEMPLATE, getTemplateName());
        }
    }

    public void checkCompatibility(Component component) throws XPathException {
        Template other = (Template) component.getProcedure();
        if (!getSymbolicName().equals(other.getSymbolicName())) {
            throw new IllegalArgumentException();
        }

        if (!requiredType.equals(other.getRequiredType())) {
            compileError("The overriding template has a different required type from the overridden template", "XTSE3070");
        }

        List<LocalParam> otherParams = other.getLocalParams();
        Set<StructuredQName> overriddenParams = new HashSet<StructuredQName>();
        for (LocalParam lp0 : otherParams) {
            XSLLocalParam lp1 = getParam(lp0.getVariableQName());
            if (lp1 == null) {
                compileError("The overridden template declares a parameter " +
                        lp0.getVariableQName().getDisplayName() + " which is not declared in the overriding template", "XTSE3070");
                return;
            }
            if (!lp1.getRequiredType().equals(lp0.getRequiredType())) {
                lp1.compileError("The parameter " +
                        lp0.getVariableQName().getDisplayName() + " has a different required type in the overridden template", "XTSE3070");
                return;
            }
            if (lp1.isRequiredParam() != lp0.isRequiredParam()) {
                lp1.compileError("The parameter " +
                        lp0.getVariableQName().getDisplayName() + " is " +
                        (lp1.isRequiredParam() ? "required" : "optional") +
                        " in the overriding template, but " +
                        (lp0.isRequiredParam() ? "required" : "optional") +
                        " in the overridden template", "XTSE3070");
                return;
            }
            if (lp1.isTunnelParam() != lp0.isTunnelParam()) {
                lp1.compileError("The parameter " +
                        lp0.getVariableQName().getDisplayName() + " is a " +
                        (lp1.isTunnelParam() ? "tunnel" : "non-tunnel") +
                        " parameter in the overriding template, but " +
                        (lp0.isTunnelParam() ? "tunnel" : "non-tunnel") +
                        " parameter in the overridden template", "XTSE3070");
                return;
            }
            overriddenParams.add(lp0.getVariableQName());
        }

        AxisIterator params = iterateAxis(AxisInfo.CHILD);
        while (true) {
            NodeInfo param = params.next();
            if (param == null) {
                break;
            }
            if (param instanceof XSLLocalParam &&
                    !overriddenParams.contains(((XSLLocalParam) param).getObjectName()) &&
                    ((XSLLocalParam) param).isRequiredParam()) {
                ((XSLLocalParam) param).compileError(
                        "An overriding template cannot introduce a required parameter that is not declared in the overridden template", "XTSE3070");
            }
        }

    }

    public XSLLocalParam getParam(StructuredQName name) {
        AxisIterator params = iterateAxis(AxisInfo.CHILD);
        while (true) {
            NodeInfo param = params.next();
            if (param == null) {
                break;
            }
            if (param instanceof XSLLocalParam && name.equals(((XSLLocalParam) param).getObjectName())) {
                return (XSLLocalParam) param;
            }
        }
        return null;
    }

    /**
     * Determine the type of item returned by this template
     *
     * @return the item type returned
     */

    protected ItemType getReturnedItemType() {
        if (requiredType == null) {
            return getCommonChildItemType();
        } else {
            return requiredType.getPrimaryType();
        }
    }


    public void prepareAttributes() throws XPathException {

        AttributeCollection atts = getAttributeList();

        for (int a = 0; a < atts.getLength(); a++) {
            String f = atts.getQName(a);
            if (f.equals(StandardNames.MODE)) {
                modeAtt = Whitespace.trim(atts.getValue(a));
            } else if (f.equals(StandardNames.NAME)) {
                nameAtt = Whitespace.trim(atts.getValue(a));
            } else if (f.equals(StandardNames.MATCH)) {
                matchAtt = atts.getValue(a);
            } else if (f.equals(StandardNames.PRIORITY)) {
                priorityAtt = Whitespace.trim(atts.getValue(a));
            } else if (f.equals(StandardNames.AS)) {
                asAtt = atts.getValue(a);
            } else if (f.equals(StandardNames.VISIBILITY)) {
                visibilityAtt = Whitespace.trim(atts.getValue(a));
            } else {
                checkUnknownAttribute(atts.getNodeName(a));
            }
        }
        try {
            if (modeAtt == null) {
                if (matchAtt != null) {
                    // XSLT 3.0 allows the default mode to be specified at stylesheet module level
                    StructuredQName defaultMode = ((XSLModuleRoot)getContainingStylesheet()).getDefaultMode();
                    if (defaultMode == null) {
                        defaultMode = Mode.UNNAMED_MODE_NAME;
                    }
                    modeNames = new StructuredQName[1];
                    modeNames[0] = defaultMode;
                }
            } else {
                if (matchAtt == null) {
                    compileError("The mode attribute must be absent if the match attribute is absent", "XTSE0500");
                }
                // mode is a space-separated list of mode names, or "#default", or "#all"

                int count = 0;
                boolean allModes = false;
                StringTokenizer st = new StringTokenizer(modeAtt, " \t\n\r", false);
                while (st.hasMoreTokens()) {
                    st.nextToken();
                    count++;
                }

                if (count == 0) {
                    compileError("The mode attribute must not be empty", "XTSE0550");
                }

                modeNames = new StructuredQName[count];
                count = 0;
                st = new StringTokenizer(modeAtt, " \t\n\r", false);
                while (st.hasMoreTokens()) {
                    String s = st.nextToken();
                    StructuredQName mname;
                    if ("#default".equals(s)) {
                        mname = getContainingStylesheet().getDefaultMode();
                        if (mname == null) {
                            mname = Mode.UNNAMED_MODE_NAME;
                        }
                    } else if ("#unnamed".equals(s) && isXslt30Processor()) {
                        mname = Mode.UNNAMED_MODE_NAME;
                    } else if ("#all".equals(s)) {
                        allModes = true;
                        mname = Mode.OMNI_MODE;
                    } else {
                        mname = makeQName(s);
                    }
                    for (int e = 0; e < count; e++) {
                        if (modeNames[e].equals(mname)) {
                            compileError("In the list of modes, the value " + s + " is duplicated", "XTSE0550");
                        }
                    }
                    modeNames[count++] = mname;
                }
                if (allModes && (count > 1)) {
                    compileError("mode='#all' cannot be combined with other modes", "XTSE0550");
                }
            }
        } catch (NamespaceException err) {
            compileError(err.getMessage(), "XTSE0280");
        } catch (XPathException err) {
            err.maybeSetErrorCode("XTSE0280");
            if (err.getErrorCodeLocalPart().equals("XTSE0020")) {
                err.setErrorCode("XTSE0550");
            }
            err.setIsStaticError(true);
            compileError(err);
        }

        try {
            if (nameAtt != null) {
                StructuredQName qName = makeQName(nameAtt);
                setObjectName(qName);
                diagnosticId = nameAtt;
            }
        } catch (NamespaceException err) {
            compileError(err.getMessage(), "XTSE0280");
        } catch (XPathException err) {
            err.maybeSetErrorCode("XTSE0280");
            err.setIsStaticError(true);
            compileError(err);
        }

        prioritySpecified = priorityAtt != null;
        if (prioritySpecified) {
            if (matchAtt == null) {
                compileError("The priority attribute must be absent if the match attribute is absent", "XTSE0500");
            }
            try {
                // it's got to be a valid decimal, but we want it as a double, so parse it twice
                if (!DecimalValue.castableAsDecimal(priorityAtt)) {
                    compileError("Invalid numeric value for priority (" + priority + ')', "XTSE0530");
                }
                priority = Double.parseDouble(priorityAtt);
            } catch (NumberFormatException err) {
                // shouldn't happen
                compileError("Invalid numeric value for priority (" + priority + ')', "XTSE0530");
            }
        }

        if (matchAtt != null) {
            match = makePattern(matchAtt);
            if (diagnosticId == null) {
                diagnosticId = "match=\"" + matchAtt + '\"';
                if (modeAtt != null) {
                    diagnosticId += " mode=\"" + modeAtt + '\"';
                }
            }
        }

        if (match == null && nameAtt == null) {
            compileError("xsl:template must have a name or match attribute (or both)", "XTSE0500");
        }
        if (asAtt != null) {
            requiredType = makeSequenceType(asAtt);
        }

        if (visibilityAtt == null) {
            visibility = Visibility.PUBLIC;
        } else if (!isXslt30Processor()) {
            compileError("The xsl:template/@visibility attribute requires XSLT 3.0 to be enabled", "XTSE0020");
        } else {
            visibility = getVisibilityValue(visibilityAtt, "");
            if (nameAtt == null) {
                compileError("xsl:template/@visibility can be specified only if the template has a @name attribute", "XTSE0020");
            }
        }
    }

    public void validate(ComponentDeclaration decl) throws XPathException {
        stackFrameMap = getConfiguration().makeSlotManager();
        checkTopLevel("XTSE0010", true);

        // the check for duplicates is now done in the buildIndexes() method of XSLStylesheet
        if (match != null) {
            match = typeCheck("match", match);
            if (match.getItemType() instanceof ErrorType) {
                issueWarning(new XPathException("Pattern will never match anything", this));
            }
        }

        // See if there are any required parameters.
        AxisIterator kids = iterateAxis(AxisInfo.CHILD);
        boolean hasContent = false;
        while (true) {
            NodeInfo param = kids.next();
            if (param == null) {
                break;
            }
            if (param instanceof XSLLocalParam) {
                if (((XSLLocalParam) param).isRequiredParam()) {
                    hasRequiredParams = true;
                }
            } else if (param instanceof StyleElement) {
                hasContent = true;
            }
        }

        if (visibility == Visibility.ABSTRACT && hasContent) {
            compileError("A template with visibility='abstract' must have no body");
        }

    }


    public void postValidate() throws XPathException {
        isTailRecursive = markTailCalls();
    }

    public void index(ComponentDeclaration decl, StylesheetPackage top) throws XPathException {
        top.indexNamedTemplate(decl);
    }

    /**
     * Mark tail-recursive calls on templates and functions.
     */

    public boolean markTailCalls() {
        StyleElement last = getLastChildInstruction();
        return last != null && last.markTailCalls();
    }

    /**
     * Compile: creates the executable form of the template
     */

    public void compileDeclaration(Compilation compilation, ComponentDeclaration decl) throws XPathException {

        Expression block = compileSequenceConstructor(compilation, decl, iterateAxis(AxisInfo.CHILD), true);
        if (block == null) {
            block = Literal.makeEmptySequence(this);
        }

        compiledTemplate.setMatchPattern(match);
        compiledTemplate.setBody(block);
        compiledTemplate.setStackFrameMap(stackFrameMap);
        compiledTemplate.setSystemId(getSystemId());
        compiledTemplate.setLineNumber(getLineNumber());
        compiledTemplate.setHasRequiredParams(hasRequiredParams);
        compiledTemplate.setRequiredType(requiredType);
        compiledTemplate.setModeNames(modeNames);

        Expression exp = null;
        try {
            exp = makeExpressionVisitor().simplify(block);
        } catch (XPathException e) {
            compileError(e);
        }

        if (visibility != Visibility.ABSTRACT) {
            try {
                if (requiredType != null) {
                    RoleLocator role =
                            new RoleLocator(RoleLocator.TEMPLATE_RESULT, diagnosticId, 0);
                    //role.setSourceLocator(new ExpressionLocation(this));
                    role.setErrorCode("XTTE0505");
                    exp = TypeChecker.staticTypeCheck(exp, requiredType, false, role, makeExpressionVisitor());
                }
            } catch (XPathException err) {
                compileError(err);
            }
        }

        //compiledTemplate.makeDeclaringComponent(visibility, getContainingPackage());
        compiledTemplate.setBody(exp);
        compiledTemplate.setTemplateName(getObjectName());

        if (getConfiguration().isCompileWithTracing()) {
            // Add trace wrapper code if required
            exp = makeTraceInstruction(this, exp);
            if (exp instanceof TraceExpression) {
                ((TraceExpression) exp).setProperty("match", matchAtt);
                ((TraceExpression) exp).setProperty("mode", modeAtt);
            }
            compiledTemplate.setBody(exp);
        }
    }

    /**
     * Registers the template rule with each Mode that it belongs to.
     *
     * @param declaration Associates this template with a stylesheet module (in principle an xsl:template
     *                    element can be in a document that is imported more than once; these are separate declarations)
     * @throws XPathException if a failure occurs
     */

    public void register(ComponentDeclaration declaration) throws XPathException {
        if (match != null) {
            StylesheetModule module = declaration.getModule();
            RuleManager mgr = getCompilation().getStylesheetPackage().getRuleManager();
            ExpressionVisitor visitor = ExpressionVisitor.make(getStaticContext());
            boolean exclusive = modeNames.length == 1 && getTemplateName() == null;
            for (StructuredQName nc : modeNames) {
                Mode mode = mgr.getMode(nc, true);
                Pattern match2 = match;
                String typed = mode.getPropertyValue("typed");
                if ("strict".equals(typed) || "lax".equals(typed)) {
                    try {
                        match2 = match.convertToTypedPattern(typed);
                    } catch (XPathException e) {
                        e.maybeSetLocation(this);
                        throw e;
                    }
                    if (match2 != match) {
                        ContextItemStaticInfo info = new ContextItemStaticInfo(AnyItemType.getInstance(), false);
                        match2.setPackageData(match.getPackageData());
                        match2.setOriginalText(match.toString());
                        match2.setSystemId(match.getSystemId());
                        match2.setLineNumber(match.getLineNumber());
                        match2 = match2.analyze(visitor, info);
                    }
                    if (modeNames.length == 1) {
                        // If this is the only mode for the template, then we can use this enhanced match pattern
                        // for subsequent type-checking of the template body.
                        match = match2;
                    }
                }
                mgr.setTemplateRule(match2, compiledTemplate, mode, module, prioritySpecified ? priority : Double.NaN);

                if (mode.isDeclaredStreamable()) {
                    compiledTemplate.setDeclaredStreamable(true);
                }
            }

            //allocatePatternSlots(slots);
        }
    }

    /**
     * Registers the template rule with each Mode that it belongs to.
     *
     * @throws XPathException if a failure occurs
     */

    public void allocatePatternSlotNumbers() throws XPathException {
        if (match != null) {
            // first slot in pattern is reserved for current()
            int nextFree = 0;
            if ((match.getDependencies() & StaticProperty.DEPENDS_ON_CURRENT_ITEM) != 0) {
                nextFree = 1;
            }
            int slots = match.allocateSlots(getSlotManager(), nextFree);
            if (slots > 0) {
                RuleManager mgr = getCompilation().getStylesheetPackage().getRuleManager();
                boolean appliesToAll = false;
                for (StructuredQName nc : modeNames) {
                    if (nc.equals(Mode.OMNI_MODE)) {
                        appliesToAll = true;
                        break;
                    }
                    Mode mode = mgr.getMode(nc, true);
                    mode.allocatePatternSlots(slots);
                }
                if (appliesToAll) {
                    for (Mode m : mgr.getAllNamedModes()) {
                        m.allocatePatternSlots(slots);
                    }
                    mgr.getUnnamedMode().allocatePatternSlots(slots);
                }

                allocatePatternSlots(slots);
            }
        }
    }



    /**
     * This method is a bit of a misnomer, because it does more than invoke optimization of the template body.
     * In particular, it also registers the template rule with each Mode that it belongs to.
     *
     * @param declaration Associates this template with a stylesheet module (in principle an xsl:template
     *                    element can be in a document that is imported more than once; these are separate declarations)
     * @throws XPathException
     */

    public void optimize(ComponentDeclaration declaration) throws XPathException {
        ItemType contextItemType = Type.ITEM_TYPE;
        ContextItemStaticInfo cit;
        if (getObjectName() == null) {
            // the template can't be called by name, so the context item must match the match pattern
            contextItemType = match.getItemType();
            if (contextItemType.equals(ErrorType.getInstance())) {
                // if the match pattern can't match anything, we produce a warning, not a hard error
                contextItemType = AnyItemType.getInstance();
            }
            cit = new ContextItemStaticInfo(contextItemType, false);
        } else {
            cit = new ContextItemStaticInfo(contextItemType, true);
        }

//#ifdefined STREAM
        if (compiledTemplate.isDeclaredStreamable()) {
            cit.setContextPostureStriding();
        }
//#endif

        Expression exp = compiledTemplate.getBody();
        ExpressionTool.resetPropertiesWithinSubtree(exp);
        ExpressionVisitor visitor = makeExpressionVisitor();
        visitor.setOptimizeForStreaming(compiledTemplate.isDeclaredStreamable());
        Optimizer opt = getConfiguration().obtainOptimizer();
        try {
            // We've already done the typecheck of each XPath expression, but it's worth doing again at this
            // level because we have more information now.
            Expression exp2 = visitor.typeCheck(exp, cit);
            if (opt.getOptimizationLevel() != Optimizer.NO_OPTIMIZATION) {
                exp2 = visitor.optimize(exp2, cit);
            }
            if (exp != exp2) {
                compiledTemplate.setBody(exp2);
                exp = exp2;
            }
        } catch (XPathException e) {
            compileError(e);
        }

        // Try to extract new global variables from the body of the template
        // TODO: it might be better to extract global variables much earlier
        if (opt.getOptimizationLevel() != Optimizer.NO_OPTIMIZATION && !getConfiguration().isCompileWithTracing()) {
            Expression exp2 = opt.promoteExpressionsToGlobal(exp, getCompilation().getStylesheetPackage(), visitor);
            if (exp2 != null) {
                // Try another optimization pass: extracting global variables can identify things that are indexable
                compiledTemplate.setBody(visitor.optimize(exp2, cit));
                exp = exp2;
            }
        }

        allocateSlots(exp);

        if (match != null) {
            ContextItemStaticInfo info = new ContextItemStaticInfo(match.getItemType(), false, true);
            for (Rule r: compiledTemplate.references) {
                Pattern p2 = r.getPattern().analyze(visitor, info);
                r.setPattern(p2);
            }
//            Pattern m2 = match.analyze(visitor,  info);
//            if (m2 != match) {
//                match = m2;
//                compiledTemplate.setMatchPattern(m2);
//            }
        }

        opt.checkStreamability(this, compiledTemplate);

        if (isExplaining()) {
            Logger err = getConfiguration().getLogger();
            err.info("Optimized expression tree for template at line " +
                    getLineNumber() + " in " + getSystemId() + ':');
            exp.explain(err);
        }

    }

    /**
     * Generate byte code for the template (if appropriate)
     * @param opt the optimizer
     * @throws XPathException if byte code generation fails
     */

    public void generateByteCode(Optimizer opt) throws XPathException {
        // Generate byte code if appropriate

        if (getCompilation().getCompilerInfo().isGenerateByteCode() && !isTailRecursive && !compiledTemplate.isDeclaredStreamable()) {
            try {
                Expression exp = compiledTemplate.getBody();
                Expression cbody = opt.compileToByteCode(exp, nameAtt == null ? matchAtt : nameAtt, Expression.PROCESS_METHOD);
                if (cbody != null) {
                    compiledTemplate.setBody(cbody);
                }
            } catch (Exception e) {
                System.err.println("Failed while compiling template " + (nameAtt == null ? "match='" + matchAtt + "'" : nameAtt));
                e.printStackTrace();
                throw new XPathException(e);
            }
        }
    }


    /**
     * Get associated Procedure (for details of stack frame)
     */

    public SlotManager getSlotManager() {
        return stackFrameMap;
    }


    /**
     * Get the compiled template
     *
     * @return the compiled template
     */

    public Template getCompiledTemplate() {
        return compiledTemplate;
    }

    /**
     * Get the type of construct. This will be a constant in
     * class {@link net.sf.saxon.trace.Location}. This method is part of the {@link net.sf.saxon.trace.InstructionInfo} interface
     */

    public int getConstructType() {
        return StandardNames.XSL_TEMPLATE;
    }


}

