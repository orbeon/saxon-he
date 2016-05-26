////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
// Copyright (c) 2014 Saxonica Limited.
// This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
// If a copy of the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
// This Source Code Form is "Incompatible With Secondary Licenses", as defined by the Mozilla Public License, v. 2.0.
////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

package net.sf.saxon.expr.instruct;

import com.saxonica.ee.bytecode.CompiledExpression;
import com.saxonica.ee.stream.Posture;
import com.saxonica.ee.stream.PostureAndSweep;
import com.saxonica.ee.stream.TemplateInversion;
import net.sf.saxon.Configuration;
import net.sf.saxon.expr.Expression;
import net.sf.saxon.expr.Operand;
import net.sf.saxon.expr.XPathContext;
import net.sf.saxon.expr.XPathContextMajor;
import net.sf.saxon.expr.parser.ContextItemStaticInfo;
import net.sf.saxon.expr.parser.ExpressionTool;
import net.sf.saxon.om.StandardNames;
import net.sf.saxon.om.StructuredQName;
import net.sf.saxon.pattern.Pattern;
import net.sf.saxon.pattern.PatternSponsor;
import net.sf.saxon.style.StylesheetPackage;
import net.sf.saxon.trace.ExpressionPresenter;
import net.sf.saxon.trace.Location;
import net.sf.saxon.trans.*;
import net.sf.saxon.value.SequenceType;

import java.util.ArrayList;
import java.util.List;

/**
 * The runtime object corresponding to an xsl:template element in the stylesheet.
 * <p/>
 * Note that the Template object no longer has precedence information associated with it; this is now
 * only in the Rule object that references this Template. This allows two rules to share the same template,
 * with different precedences. This occurs when a stylesheet module is imported more than once, from different
 * places, with different import precedences.
 */

public class Template extends ComponentBody implements RuleTarget {

    // TODO: change the calling mechanism for named templates to use positional parameters
    // in the same way as functions. For templates that have both a match and a name attribute,
    // create a match template as a wrapper around the named template, resulting in separate
    // NamedTemplate and MatchTemplate classes. For named templates, perhaps compile into function
    // calls directly, the only difference being that context is retained.

    // The body of the template is represented by an expression,
    // which is responsible for any type checking that's needed.

    private Pattern matchPattern;
    private StructuredQName templateName;
    private boolean hasRequiredParams;
    private boolean bodyIsTailCallReturner;
    private SequenceType requiredType;
    private boolean declaredStreamable;
    private StructuredQName[] modeNames;

    public List<Rule> references = new ArrayList<Rule>();

    /**
     * Create a template
     */

    public Template() {
        setHostLanguage(Configuration.XSLT);
    }

    /**
     * Initialize the template
     *
     * @param templateName the name of the template (if any)
     *                     performed by apply-imports
     */

    public void setTemplateName(StructuredQName templateName) {
        this.templateName = templateName;
    }

    /**
     * Set the match pattern used with this template
     *
     * @param pattern the match pattern (may be null for a named template)
     */

    public void setMatchPattern(Pattern pattern) {
//        if (matchPattern != pattern) {
//            for (Rule r: references) {
//                r.setPattern(pattern);
//            }
//        }
        matchPattern = pattern;

    }

    public int getComponentKind() {
        return StandardNames.XSL_TEMPLATE;
    }

    public SymbolicName getSymbolicName() {
        if (getTemplateName() == null) {
            return null;
        } else {
            return new SymbolicName(StandardNames.XSL_TEMPLATE, getTemplateName());
        }
    }

    /**
     * Get the match pattern used with this template
     *
     * @return the match pattern, or null if this is a named template with no match pattern
     */

    public Pattern getMatchPattern() {
        return matchPattern;
    }

    /**
     * Set the expression that forms the body of the template
     *
     * @param body the body of the template
     */

    public void setBody(Expression body) {
        super.setBody(body);
        bodyIsTailCallReturner = (body instanceof TailCallReturner);
    }

    /**
     * Get the name of the template (if it is named)
     *
     * @return the template name, or null if unnamed
     */

    public StructuredQName getTemplateName() {
        return templateName;
    }

    @Override
    public void allocateAllBindingSlots(StylesheetPackage pack) {
        super.allocateAllBindingSlots(pack);
        if (matchPattern != null) {
            allocateBindingSlotsRecursive(pack, this, new PatternSponsor(matchPattern));
        }
    }

    /**
     * Get a name identifying the object of the expression, for example a function name, template name,
     * variable name, key name, element name, etc. This is used only where the name is known statically.
     */

    public StructuredQName getObjectName() {
        return templateName;
    }

    /**
     * Set whether this template has one or more required parameters
     *
     * @param has true if the template has at least one required parameter
     */

    public void setHasRequiredParams(boolean has) {
        hasRequiredParams = has;
    }

    /**
     * Ask whether this template has one or more required parameters
     *
     * @return true if this template has at least one required parameter
     */

    public boolean hasRequiredParams() {
        return hasRequiredParams;
    }

    /**
     * Set the required type to be returned by this template
     *
     * @param type the required type as defined in the "as" attribute on the xsl:template element
     */

    public void setRequiredType(SequenceType type) {
        requiredType = type;
    }

    /**
     * Get the required type to be returned by this template
     *
     * @return the required type as defined in the "as" attribute on the xsl:template element
     */

    public SequenceType getRequiredType() {
        if (requiredType == null) {
            return SequenceType.ANY_SEQUENCE;
        } else {
            return requiredType;
        }
    }

    /**
     * Set the names of the modes associated with this template rule
     *
     * @param modeNames the names of the modes for this template rule
     */

    public void setModeNames(StructuredQName[] modeNames) {
        this.modeNames = modeNames;
    }

    /**
     * Get the names of the modes associated with this template rule
     *
     * @return the names of the modes for this template rule
     */

    public StructuredQName[] getModeNames() {
        return modeNames;
    }

    /**
     * Register a rule for which this is the target
     *
     * @param rule a rule in which this is the target
     */
    public void regiaterRule(Rule rule) {
        references.add(rule);
    }

    /**
     * Say whether or not this template is declared as streamable
     *
     * @param streamable true if the template belongs to a streamable mode; set to false if it does not belong
     * to a streamable mode, or if it is found that the template is not actually streamable, and fallback
     * to non-streaming has been requested.
     */

    public void setDeclaredStreamable(boolean streamable) {
        this.declaredStreamable = streamable;
    }

    /**
     * Ask whether or not this template is declared as streamable
     *
     * @return true if the template belongs to a streamable mode; false if it does not belong
     * to a streamable mode, or if it is found that the template is not actually streamable, and fallback
     * to non-streaming has been requested.
     */

    public boolean isDeclaredStreamable() {
        return declaredStreamable;
    }


    public List<LocalParam> getLocalParams() {
        List<LocalParam> result = new ArrayList<LocalParam>();
        gatherLocalParams(getInterpretedBody(), result);
        return result;
    }

    private static void gatherLocalParams(Expression exp, List<LocalParam> result) {
        if (exp instanceof LocalParamSetter) {
            result.add(((LocalParamSetter) exp).getBinding());
        } else {
            for (Operand o : exp.operands()) {
                gatherLocalParams(o.getExpression(), result);
            }
        }
    }

    /**
     * Get the local parameter with a given parameter id
     *
     * @param id the parameter id
     * @return the local parameter with this id if found, otherwise null
     */

    /*@Nullable*/
    public LocalParam getLocalParam(StructuredQName id) {
        for (Operand o : body.operands()) {
            Expression child = o.getExpression();
            if (child instanceof LocalParamSetter && ((LocalParamSetter) child).getBinding().getVariableQName().equals(id)) {
                return ((LocalParamSetter) child).getBinding();
            } else if (ExpressionTool.containsLocalParam(child)) {
                LocalParam lp = getLocalParam(child, id);
                if (lp != null) {
                    return lp;
                }
            }
        }
        return null;
    }

    private static LocalParam getLocalParam(Expression exp, StructuredQName id) {
        for (Operand o : exp.operands()) {
            Expression child = o.getExpression();
            if (child instanceof LocalParamSetter && ((LocalParamSetter) child).getBinding().getVariableQName().equals(id)) {
                return ((LocalParamSetter) child).getBinding();
            } else if (ExpressionTool.containsLocalParam(child)) {
                return getLocalParam(child, id);
            }
        }
        return null;
    }

    /**
     * Process the template, without returning any tail calls. This path is used by
     * xsl:apply-imports and xsl:next-match
     *
     * @param context The dynamic context, giving access to the current node,
     * @throws net.sf.saxon.trans.XPathException
     *          if a dynamic error occurs while evaluating
     *          the template
     */

    public void apply(XPathContextMajor context) throws XPathException {
        TailCall tc = applyLeavingTail(context);
        while (tc != null) {
            tc = tc.processLeavingTail();
        }
    }

    /**
     * Process this template, with the possibility of returning a tail call package if the template
     * contains any tail calls that are to be performed by the caller.
     *
     * @param context the XPath dynamic context
     * @return null if the template exited normally; but if it was a tail call, details of the call
     *         that hasn't been made yet and needs to be made by the caller
     * @throws net.sf.saxon.trans.XPathException
     *          if a dynamic error occurs while evaluating
     *          the template
     */

    public TailCall applyLeavingTail(XPathContextMajor context) throws XPathException {
        try {
            if (bodyIsTailCallReturner) {
                return ((TailCallReturner) body).processLeavingTail(context);
            } else {
                body.process(context);
                return null;
            }
        } catch (XPathException e) {
            e.maybeSetLocation(this);
            throw e;
        } catch (Exception e2) {
            String message = "Internal error evaluating template "
                    + (getTemplateName() != null ? getTemplateName().getDisplayName() : "")
                    + (getLineNumber() > 0 ? " at line " + getLineNumber() : "")
                    + (getSystemId() != null ? " in module " + getSystemId() : "");
            throw new RuntimeException(message, e2);
        }
    }

    /**
     * Expand the template. Called when the template is invoked using xsl:call-template.
     * Invoking a template by this method does not change the current template.
     *
     * @param context the XPath dynamic context
     * @return null if the template exited normally; but if it was a tail call, details of the call
     *         that hasn't been made yet and needs to be made by the caller
     * @throws net.sf.saxon.trans.XPathException
     *          if a dynamic error occurs while evaluating
     *          the template
     */

    public TailCall expand(XPathContext context) throws XPathException {
        if (bodyIsTailCallReturner) {
            return ((TailCallReturner) body).processLeavingTail(context);
        } else if (body != null) {
            body.process(context);
        }
        return null;
    }


    /**
     * Get the type of construct. This will either be the fingerprint of a standard XSLT instruction name
     * (values in {@link net.sf.saxon.om.StandardNames}: all less than 1024)
     * or it will be a constant in class {@link net.sf.saxon.trace.Location}.
     */

    public int getConstructType() {
        return Location.TEMPLATE;
    }

    /**
     * Output diagnostic explanation to an ExpressionPresenter
     */

    public void explain(ExpressionPresenter presenter) {
        presenter.emitAttribute("line", getLineNumber() + "");
        presenter.emitAttribute("module", getSystemId());
        if (isDeclaredStreamable()) {
            presenter.emitAttribute("streamable", "true");
        }
        if (getBody() != null) {
            getBody().explain(presenter);
        }
    }

    public Expression getInterpretedBody() {
        Expression original = body;
//#ifdefined BYTECODE
        if (original instanceof CompiledExpression) {
            original = ((CompiledExpression)original).getOriginalExpression();
        }
//#endif
        return original;
    }

//#ifdefined STREAM

    /**
     * Ask whether this template satisfies the rules for a guaranteed streamable template
     *
     * @param allowExtensions if true, the method tests for streamability against the Saxon
     *                        rules rather than the W3C rules
     * @param reasons         the caller may supply a list which on return will contain explanations
     *                        for why the template is not streamable, suitable for inclusion in error messages
     * @return true if the template is within the (Saxon or W3C) definition of guaranteed streamable
     */

    public boolean isActuallyStreamable(boolean allowExtensions, List<String> reasons) {
        if (!matchPattern.isMotionless(allowExtensions)) {
            if (reasons != null) {
                reasons.add("The match pattern is not motionless");
            }
            return false;
        }
        if (body == null) {
            if (reasons != null) {
                reasons.add("The template has no body");
            }
            return false;
        }
        ContextItemStaticInfo info = new ContextItemStaticInfo(matchPattern.getItemType(), false, true);
        PostureAndSweep s = body.getStreamability(allowExtensions, info, reasons);
        return s.getPosture() == Posture.GROUNDED;
    }

        /**
     * Generate the inversion of the expression comprising the body of a template rules.
     * Supported in Saxon-EE only
     *
     * @param pattern  the match pattern of this template rule
     * @param template the template to be inverted
     */


    public void makeInversion(Pattern pattern, Template template) throws XPathException {
        // Now create the Jackson inversion of the template
        if (template.getBody() instanceof TraceExpression) {
            XPathException err = new XPathException(
                    "Saxon restriction: code that uses streaming cannot be compiled with tracing enabled", template);
            err.setErrorCode(SaxonErrorCode.SXST0065);
            throw err;
        }
        TemplateInversion inv = new TemplateInversion(template);
        inv.setStackFrame(template.getStackFrameMap());
        inv.invert();
        inversion = inv;
    }

    private TemplateInversion inversion;
    public TemplateInversion getInversion() {
        return inversion;
    }
//#endif

}

