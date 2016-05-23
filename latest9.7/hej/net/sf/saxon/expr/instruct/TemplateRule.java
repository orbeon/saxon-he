////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
// Copyright (c) 2015 Saxonica Limited.
// This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
// If a copy of the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
// This Source Code Form is "Incompatible With Secondary Licenses", as defined by the Mozilla Public License, v. 2.0.
////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

package net.sf.saxon.expr.instruct;

import com.saxonica.ee.bytecode.CompiledExpression;
import com.saxonica.ee.stream.Posture;
import com.saxonica.ee.stream.PostureAndSweep;
import com.saxonica.ee.stream.TemplateInversion;
import net.sf.saxon.expr.*;
import net.sf.saxon.expr.parser.ContextItemStaticInfo;
import net.sf.saxon.expr.parser.Location;
import net.sf.saxon.om.StandardNames;
import net.sf.saxon.pattern.Pattern;
import net.sf.saxon.trace.ExpressionPresenter;
import net.sf.saxon.trace.LocationKind;
import net.sf.saxon.trans.Rule;
import net.sf.saxon.trans.RuleTarget;
import net.sf.saxon.trans.SaxonErrorCode;
import net.sf.saxon.trans.XPathException;
import net.sf.saxon.type.AnyItemType;
import net.sf.saxon.type.ItemType;
import net.sf.saxon.type.Type;
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
 *
 * <p>From Saxon 9.7, the old Template class is split into NamedTemplate and TemplateRule.</p>
 */

public class TemplateRule implements RuleTarget, Location {

    // The body of the template is represented by an expression,
    // which is responsible for any type checking that's needed.

    private Expression body;
    private Pattern matchPattern;
    private boolean hasRequiredParams;
    private boolean bodyIsTailCallReturner;
    private SequenceType requiredType;
    private boolean declaredStreamable;
    private ItemType requiredContextItemType = AnyItemType.getInstance();
    private boolean mayOmitContextItem = true;
    private boolean maySupplyContextItem = true;
    private SlotManager stackFrameMap;
    private PackageData packageData;
    private String systemId;
    private int lineNumber;

    private List<Rule> rules = new ArrayList<Rule>();
    private List<TemplateRule> slaveCopies = new ArrayList<TemplateRule>();

    /**
     * Create a template
     */

    public TemplateRule() {
    }

    /**
     * Set the match pattern used with this template
     *
     * @param pattern the match pattern (may be null for a named template)
     */

    public void setMatchPattern(Pattern pattern) {
//        if (matchPattern != pattern) {
//            for (Rule r : rules) {
//                r.setPattern(pattern);
//            }
//        }
        matchPattern = pattern;

    }

    public Expression getBody() {
        return body;
    }

    /**
     * Set the required context item type. Used when there is an xsl:context-item child element
     *
     * @param type          the required context item type
     * @param mayBeOmitted  true if the context item may be absent
     * @param mayBeSupplied true if the context item may be supplied
     */

    public void setContextItemRequirements(ItemType type, boolean mayBeOmitted, boolean mayBeSupplied) {
        requiredContextItemType = type;
        mayOmitContextItem = mayBeOmitted;
        maySupplyContextItem = mayBeSupplied;
    }

    public int getComponentKind() {
        return StandardNames.XSL_TEMPLATE;
    }

    /**
     * Get the match pattern used with this template
     *
     * @return the match pattern, or null if this is a named template with no match pattern. In the case of
     * a template rule whose pattern is a union pattern, this will be the original union pattern; the individual
     * Rule objects contain the branches of the union pattern.
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
        this.body = body;
        bodyIsTailCallReturner = (body instanceof TailCallReturner);
    }

    public void setStackFrameMap(SlotManager map) {
        stackFrameMap = map;
    }

    public SlotManager getStackFrameMap() {
        return stackFrameMap;
    }

//    @Override
//    public void allocateAllBindingSlots(StylesheetPackage pack) {
//        super.allocateAllBindingSlots(pack);
//        if (matchPattern != null) {
//            allocateBindingSlotsRecursive(pack, this, matchPattern);
//        }
//    }

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
     * Register a rule for which this is the target
     *
     * @param rule a rule in which this is the target
     */
    public void registerRule(Rule rule) {
        rules.add(rule);
    }

    /**
     * Get the rules that use this template. For a template with no match pattern, this will be
     * an empty list. For a union pattern, there will be one rule for each branch of the union.
     * @return the rules corresponding to this template.
     */

    public List<Rule> getRules() {
        return rules;
    }

    /**
     * Say whether or not this template is declared as streamable
     *
     * @param streamable true if the template belongs to a streamable mode; set to false if it does not belong
     *                   to a streamable mode, or if it is found that the template is not actually streamable, and fallback
     *                   to non-streaming has been requested.
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

    public int getContainerGranularity() {
        return 0;
    }

    public PackageData getPackageData() {
        return packageData;
    }

    public void setPackageData(PackageData data) {
        this.packageData = data;
    }

    public String getPublicId() {
        return null;
    }

    public String getSystemId() {
        return systemId;
    }

    public void setSystemId(String id) {
        this.systemId = id;
    }

    public int getLineNumber() {
        return lineNumber;
    }

    public void setLineNumber(int line) {
        this.lineNumber = line;
    }

    public int getColumnNumber() {
        return -1;
    }

    /**
     * Get an immutable copy of this Location object. By default Location objects may be mutable, so they
     * should not be saved for later use. The result of this operation holds the same location information,
     * but in an immutable form.
     */
    public Location saveLocation() {
        return this;
    }

    public ItemType getRequiredContextItemType() {
        return requiredContextItemType;
    }

    public boolean isMayOmitContextItem() {
        return mayOmitContextItem;
    }

    public boolean isMaySupplyContextItem() {
        return maySupplyContextItem;
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
                gatherLocalParams(o.getChildExpression(), result);
            }
        }
    }

    /**
     * Process the template, without returning any tail calls. This path is used by
     * xsl:apply-imports and xsl:next-match
     *
     * @param context The dynamic context, giving access to the current node,
     * @throws XPathException if a dynamic error occurs while evaluating
     *                                           the template
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
     * that hasn't been made yet and needs to be made by the caller
     * @throws XPathException if a dynamic error occurs while evaluating
     *                                           the template
     */

    public TailCall applyLeavingTail(XPathContext context) throws XPathException {

        if (requiredContextItemType != AnyItemType.getInstance() &&
                !requiredContextItemType.matches(context.getContextItem(), context.getConfiguration().getTypeHierarchy())) {
            XPathException err = new XPathException("The template requires a context item of type " + requiredContextItemType +
                    ", but the supplied context item has type " +
                    Type.getItemType(context.getContextItem(), context.getConfiguration().getTypeHierarchy()), "XTTE0590");
            err.setLocation(this);
            err.setIsTypeError(true);
            throw err;
        }
        if (!maySupplyContextItem) {
            context = context.newMinorContext();
            context.setCurrentIterator(null);
        }

        try {
            if (bodyIsTailCallReturner) {
                return ((TailCallReturner) body).processLeavingTail(context);
            } else {
                body.process(context);
                return null;
            }
        } catch (XPathException e) {
            e.maybeSetLocation(this);
            e.maybeSetContext(context);
            throw e;
        } catch (Exception e2) {
            String message = "Internal error evaluating template rule "
                    + (getLineNumber() > 0 ? " at line " + getLineNumber() : "")
                    + (getSystemId() != null ? " in module " + getSystemId() : "");
            throw new RuntimeException(message, e2);
        }
    }

    /**
     * Get the type of construct. This will either be the fingerprint of a standard XSLT instruction name
     * (values in {@link StandardNames}: all less than 1024)
     * or it will be a constant in class {@link LocationKind}.
     */

    public int getConstructType() {
        return LocationKind.TEMPLATE;
    }

    /**
     * Output diagnostic explanation to an ExpressionPresenter
     */

    public void export(ExpressionPresenter presenter) throws XPathException{
        presenter.startElement("template");

        explainProperties(presenter);

        presenter.emitAttribute("slots", "" + getStackFrameMap().getNumberOfVariables());
        //presenter.emitAttribute("binds", "" + getDeclaringComponent().getComponentBindings().size());

        if (matchPattern != null) {
            presenter.setChildRole("match");
            matchPattern.export(presenter);
        }
        if (getBody() != null) {
            presenter.setChildRole("body");
            getBody().export(presenter);
        }
        presenter.endElement();
    }

    public void explainProperties(ExpressionPresenter presenter) {
        if (getRequiredContextItemType() != AnyItemType.getInstance()) {
            presenter.emitAttribute("cxt", getRequiredContextItemType().toString());
        }

        String flags = "";
        if (mayOmitContextItem) {
            flags = "o";
        }
        if (maySupplyContextItem) {
            flags += "s";
        }
        presenter.emitAttribute("flags", flags);
        if (getRequiredType() != SequenceType.ANY_SEQUENCE) {
            presenter.emitAttribute("as", getRequiredType().toString());
        }
        presenter.emitAttribute("line", getLineNumber() + "");
        presenter.emitAttribute("module", getSystemId());
        if (isDeclaredStreamable()) {
            presenter.emitAttribute("streamable", "1");
        }
    }

    public Expression getInterpretedBody() {
        Expression original = body;
//#ifdefined BYTECODE
        if (original instanceof CompiledExpression) {
            original = ((CompiledExpression) original).getOriginalExpression();
        }
//#endif
        return original;
    }



    /**
     * Create a copy of a template rule. This is needed when copying a rule from the "omniMode" (mode=#all)
     * to a specific mode. Because we want the rules to be chained in the right order within the mode object,
     * we create the copy as soon as we know it is needed. The problem is that at this stage many of the properties
     * of the template rule are still uninitialised. So we mark the new copy as a slave of the original, and at
     * the end of the compilation process we update all the slave copies to match the properties of the original.
     */

    public TemplateRule copy() {
        TemplateRule tr = new TemplateRule();
        if (body == null || matchPattern == null) {
            slaveCopies.add(tr);
        } else {
            copyTo(tr);
        }
        return tr;
    }

    /**
     * Update the properties of template rules that have been marked as slave copies of this one (typically the same
     * template, but in a different mode).
     */

    public void updateSlaveCopies() {
        for (TemplateRule tr : slaveCopies) {
            copyTo(tr);
        }
    }

    private void copyTo(TemplateRule tr) {
        if (body != null) {
            tr.body = body.copy();
        }
        if (matchPattern != null) {
            tr.matchPattern = matchPattern.copy();
        }
        tr.hasRequiredParams = hasRequiredParams;
        tr.bodyIsTailCallReturner = bodyIsTailCallReturner;
        tr.requiredType = requiredType;
        tr.declaredStreamable = declaredStreamable; // ? this can vary from one mode to another
        tr.requiredContextItemType = requiredContextItemType;
        tr.mayOmitContextItem = mayOmitContextItem;
        tr.maySupplyContextItem = maySupplyContextItem;
        tr.stackFrameMap = stackFrameMap;
        tr.packageData = packageData;
        tr.systemId = systemId;
        tr.lineNumber = lineNumber;
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
        if (matchPattern == null) {
            if (reasons != null) {
                reasons.add("There is no match pattern");
            }
            return false;
        }
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
     */


    public void makeInversion() throws XPathException {
        // Create the Jackson inversion of the template
        if (inversion == null) {
            if (getBody() instanceof TraceExpression) {
                throw new XPathException(
                    "Saxon restriction: code that uses streaming cannot be compiled with tracing enabled", SaxonErrorCode.SXST0065, this);
            }
            TemplateInversion inv = new TemplateInversion(this);
            inv.setStackFrame(getStackFrameMap());
            inv.invert();
            inversion = inv;
        }
    }

    private TemplateInversion inversion;

    public TemplateInversion getInversion() {
        return inversion;
    }

//#endif

}

