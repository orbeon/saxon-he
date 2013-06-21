package net.sf.saxon.style;

import net.sf.saxon.Configuration;
import net.sf.saxon.expr.Expression;
import net.sf.saxon.expr.Literal;
import net.sf.saxon.expr.instruct.Executable;
import net.sf.saxon.expr.instruct.SlotManager;
import net.sf.saxon.expr.instruct.Template;
import net.sf.saxon.expr.instruct.TraceExpression;
import net.sf.saxon.expr.parser.*;
import net.sf.saxon.om.*;
import net.sf.saxon.pattern.EmptySequenceTest;
import net.sf.saxon.pattern.Pattern;
import net.sf.saxon.trans.Mode;
import net.sf.saxon.trans.RuleManager;
import net.sf.saxon.trans.XPathException;
import net.sf.saxon.tree.iter.AxisIterator;
import net.sf.saxon.type.ItemType;
import net.sf.saxon.type.Type;
import net.sf.saxon.value.DecimalValue;
import net.sf.saxon.value.SequenceType;
import net.sf.saxon.value.Whitespace;

import javax.xml.transform.TransformerException;
import java.io.PrintStream;
import java.util.StringTokenizer;

/**
* An xsl:template element in the style sheet.
*/

public final class XSLTemplate extends StyleElement implements StylesheetProcedure {

    private String matchAtt = null;
    private String modeAtt = null;
    private String nameAtt = null;
    private String priorityAtt = null;
    private String asAtt = null;

    private StructuredQName[] modeNames;
    private String diagnosticId;
    private Pattern match;
    private boolean prioritySpecified;
    private double priority;
    private SlotManager stackFrameMap;
    private Template compiledTemplate = new Template();
    private SequenceType requiredType = null;
    private boolean hasRequiredParams = false;

    /**
     * Ask whether this node is a declaration, that is, a permitted child of xsl:stylesheet
     * (including xsl:include and xsl:import).
     * @return true for this element
     */

    @Override
    public boolean isDeclaration() {
        return true;
    }

    /**
    * Determine whether this type of element is allowed to contain a template-body
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
        return (child instanceof XSLParam);
    }

    /**
     * Return the name of this template. Note that this may
     * be called before prepareAttributes has been called.
     * @return the name of the template as a Structured QName.
    */

    /*@Nullable*/ public StructuredQName getTemplateName() {

    	//We use null to mean "not yet evaluated"

        try {
        	if (getObjectName()==null) {
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

    /**
     * Determine the type of item returned by this template
     * @return the item type returned
     */

    protected ItemType getReturnedItemType() {
        if (requiredType==null) {
            return getCommonChildItemType();
        } else {
            return requiredType.getPrimaryType();
        }
    }

    
    public void prepareAttributes() throws XPathException {

		AttributeCollection atts = getAttributeList();

		for (int a=0; a<atts.getLength(); a++) {
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
        	} else {
        		checkUnknownAttribute(atts.getNodeName(a));
        	}
        }
        try {
            if (modeAtt==null) {
                // XSLT 3.0 allows the default mode to be specified at stylesheet module level
                StructuredQName defaultMode = getContainingStylesheet().getDefaultMode();
                if (defaultMode == null) {
                    defaultMode = Mode.UNNAMED_MODE_NAME;
                }
                modeNames = new StructuredQName[1];
                modeNames[0] = defaultMode;
            } else {
                if (matchAtt==null) {
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

                if (count==0) {
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
                        mname = Mode.ALL_MODES;
                    } else {
                        mname = makeQName(s);
                    }
                    for (int e=0; e < count; e++) {
                        if (modeNames[e].equals(mname)) {
                            compileError("In the list of modes, the value " + s + " is duplicated", "XTSE0550");
                        }
                    }
                    modeNames[count++] = mname;
                }
                if (allModes && (count>1)) {
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

        try{
            if (nameAtt!=null) {
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

        prioritySpecified = (priorityAtt != null);
        if (prioritySpecified) {
            if (matchAtt==null) {
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

        if (match==null && nameAtt==null)
            compileError("xsl:template must have a name or match attribute (or both)", "XTSE0500");

        if (asAtt != null) {
            requiredType = makeSequenceType(asAtt);
        }
	}

    public void validate(Declaration decl) throws XPathException {
        stackFrameMap = getConfiguration().makeSlotManager();
        checkTopLevel("XTSE0010");

        // the check for duplicates is now done in the buildIndexes() method of XSLStylesheet
        if (match != null) {
            match = typeCheck("match", match);
            if (match.getItemType() instanceof EmptySequenceTest) {
                try {
                    getConfiguration().getErrorListener().warning(
                            new TransformerException("Match pattern cannot match any nodes", this));
                } catch (TransformerException e) {
                    compileError(XPathException.makeXPathException(e));
                }
            }
        }

        // See if there are any required parameters.
        AxisIterator kids = iterateAxis(Axis.CHILD);
        while(true) {
            NodeInfo param = kids.next();
            if (param == null) {
                break;
            }
            if (param instanceof XSLParam && ((XSLParam)param).isRequiredParam()) {
                hasRequiredParams = true;
                break;
            }
        }

    }


    public void postValidate() throws XPathException {
        markTailCalls();
    }

    protected void index(Declaration decl, PrincipalStylesheetModule top) throws XPathException {
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

    public void compileDeclaration(Executable exec, Declaration decl) throws XPathException {

        Expression block = compileSequenceConstructor(exec, decl, iterateAxis(Axis.CHILD), true);
        if (block == null) {
            block = Literal.makeEmptySequence();
        }

        compiledTemplate.setMatchPattern(match);
        compiledTemplate.setBody(block);
        compiledTemplate.setStackFrameMap(stackFrameMap);
        compiledTemplate.setExecutable(getPreparedStylesheet());
        compiledTemplate.setSystemId(getSystemId());
        compiledTemplate.setLineNumber(getLineNumber());
        compiledTemplate.setHasRequiredParams(hasRequiredParams);
        compiledTemplate.setRequiredType(requiredType);


        Expression exp = null;
        try {
            exp = makeExpressionVisitor().simplify(block);
        } catch (XPathException e) {
            compileError(e);
        }

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

        compiledTemplate.setBody(exp);
        compiledTemplate.setTemplateName(getObjectName());

        if (getConfiguration().isCompileWithTracing()) {
            // Add trace wrapper code if required
            exp = makeTraceInstruction(this, exp);
            if (exp instanceof TraceExpression) {
                ((TraceExpression)exp).setProperty("match", matchAtt);
                ((TraceExpression)exp).setProperty("mode", modeAtt);
            }
            compiledTemplate.setBody(exp);
        }
    }

    /**
     * Registers the template rule with each Mode that it belongs to.
     * @param declaration Associates this template with a stylesheet module (in principle an xsl:template
     * element can be in a document that is imported more than once; these are separate declarations)
     * @throws XPathException
     */

    public void register(Declaration declaration) throws XPathException {
        if (match != null) {
            StylesheetModule module = declaration.getModule();
            int slots = match.allocateSlots(getStaticContext(), getSlotManager(), 0);
            RuleManager mgr = getPreparedStylesheet().getRuleManager();
            for (StructuredQName nc : modeNames) {
                Mode mode = mgr.getMode(nc, true);
                if (prioritySpecified) {
                    mgr.setTemplateRule(match, compiledTemplate, mode, module, priority);
                } else {
                    mgr.setTemplateRule(match, compiledTemplate, mode, module, Double.NaN);
                }
                mode.allocatePatternSlots(slots);
                if (mode.isStreamable()) {
                    compiledTemplate.setStreamable(true);
                }
            }

            allocatePatternSlots(slots);
        }
    }


    /**
     * This method is a bit of a misnomer, because it does more than invoke optimization of the template body.
     * In particular, it also registers the template rule with each Mode that it belongs to.
     * @throws XPathException
     * @param declaration Associates this template with a stylesheet module (in principle an xsl:template
     * element can be in a document that is imported more than once; these are separate declarations)
     */

    public void optimize(Declaration declaration) throws XPathException {
        ItemType contextItemType = Type.ITEM_TYPE;
        ExpressionVisitor.ContextItemType cit;
        if (getObjectName() == null) {
            // the template can't be called by name, so the context item must match the match pattern
            contextItemType = match.getItemType();
            cit = new ExpressionVisitor.ContextItemType(contextItemType, false);
        } else {
            cit = new ExpressionVisitor.ContextItemType(contextItemType, true);
        }

        Expression exp = compiledTemplate.getBody();
        ExpressionTool.resetPropertiesWithinSubtree(exp);
        ExpressionVisitor visitor = makeExpressionVisitor();
        visitor.setOptimizeForStreaming(compiledTemplate.isStreamable());
        Optimizer opt = getConfiguration().obtainOptimizer();
        try {
            // We've already done the typecheck of each XPath expression, but it's worth doing again at this
            // level because we have more information now.
            Expression exp2 = visitor.typeCheck(exp, cit);
            if (opt.getOptimizationLevel() != Optimizer.NO_OPTIMIZATION) {
                ExpressionTool.resetPropertiesWithinSubtree(exp2);
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
//        ExpressionPresenter presenter = ExpressionPresenter.make(getConfiguration());
//        exp.explain(presenter);
//        presenter.close();
        // TODO: it might be better to extract global variables much earlier
        if (opt.getOptimizationLevel() != Optimizer.NO_OPTIMIZATION && !getConfiguration().isCompileWithTracing()) {
            Expression exp2 = opt.promoteExpressionsToGlobal(exp, visitor);
            if (exp2 != null) {
                // Try another optimization pass: extracting global variables can identify things that are indexable
                compiledTemplate.setBody(visitor.optimize(exp2, cit));
                exp = exp2;
            }
        }

        allocateSlots(exp);

        // Generate byte code if appropriate

        if (getConfiguration().isGenerateByteCode(Configuration.XSLT) && !compiledTemplate.isStreamable()) {
            try {
                Expression cbody = opt.compileToByteCode(exp, (nameAtt==null ? matchAtt : nameAtt), Expression.PROCESS_METHOD);
                if (cbody != null) {
                    compiledTemplate.setBody(cbody);
                    exp = cbody;
                }
            } catch (Exception e) {
                System.err.println("Failed while compiling function " + nameAtt);
                e.printStackTrace();
                throw new XPathException(e);
            }
        }



        if (isExplaining()) {
            PrintStream err = getConfiguration().getStandardErrorOutput();
            err.println("Optimized expression tree for template at line " +
                    getLineNumber() + " in " + getSystemId() + ':');
            exp.explain(err);
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