////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
// Copyright (c) 2018 Saxonica Limited.
// This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
// If a copy of the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
// This Source Code Form is "Incompatible With Secondary Licenses", as defined by the Mozilla Public License, v. 2.0.
////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

package net.sf.saxon.style;

import net.sf.saxon.Configuration;
import net.sf.saxon.expr.Component;
import net.sf.saxon.expr.accum.Accumulator;
import net.sf.saxon.lib.NamespaceConstant;
import net.sf.saxon.om.AttributeCollection;
import net.sf.saxon.om.StandardNames;
import net.sf.saxon.om.StructuredQName;
import net.sf.saxon.trans.*;
import net.sf.saxon.trans.rules.*;
import net.sf.saxon.value.Whitespace;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Set;

/**
 * Handler for xsl:mode elements in stylesheet.
 * The xsl:mode element defines the properties of a mode. The mode is identified
 * by the name attribute, defaulting to the unnamed default mode (which may also be
 * written "#default").
 * <p>The attribute streamable="yes|no" which
 * indicates whether templates in this mode are to be processed by streaming.</p>
 * <p>The attribute on-no-match indicates which family of template rules
 * should be used to process nodes when there is no explicit match</p>
 */

public class XSLMode extends StyleElement {

    private SimpleMode mode;
    private Set<? extends Accumulator> accumulators;
    private boolean prepared = false;
    private boolean streamable = false;
    private boolean failOnMultipleMatch = false;
    private boolean warningOnNoMatch = false;
    private boolean warningOnMultipleMatch = true;
    private boolean traceMatching = false;
    private BuiltInRuleSet defaultRules = TextOnlyCopyRuleSet.getInstance();

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
     * Determine whether this node is an instruction.
     *
     * @return false - it is a declaration
     */

    public boolean isInstruction() {
        return false;
    }

    /**
     * Get a name identifying the object of the expression, for example a function name, template name,
     * variable name, key name, element name, etc. This is used only where the name is known statically.
     * If there is no name, the value will be null.
     */

    public StructuredQName getObjectName() {
        StructuredQName qn = super.getObjectName();
        if (qn == null) {
            String nameAtt = Whitespace.trim(getAttributeValue("", "name"));
            if (nameAtt == null) {
                return Mode.UNNAMED_MODE_NAME;
            }
            try {
                qn = makeQName(nameAtt);
                setObjectName(qn);
            } catch (XPathException err) {
                return new StructuredQName("saxon", NamespaceConstant.SAXON, "badly-named-mode-" + generateId());
            }
        }
        return qn;
    }

    /**
     * Method supplied by declaration elements to add themselves to a stylesheet-level index
     *
     * @param decl the Declaration being indexed. (This corresponds to the StyleElement object
     *             except in cases where one module is imported several times with different precedence.)
     * @param top  the outermost XSLStylesheet element
     * @throws XPathException if any error is encountered
     */
    @Override
    public void index(ComponentDeclaration decl, PrincipalStylesheetModule top) throws XPathException {
        StructuredQName name = getObjectName();
        SymbolicName sName = new SymbolicName(StandardNames.XSL_MODE, name);
        HashMap<SymbolicName, Component> componentIndex = top.getStylesheetPackage().getComponentIndex();
        // see if there is already a named template with this precedence
        if (!name.equals(Mode.UNNAMED_MODE_NAME)) {
            Component other = componentIndex.get(sName);
            if (other != null && other.getDeclaringPackage() != top.getStylesheetPackage()) {
                compileError("Mode " + name.getDisplayName() +
                                     " conflicts with a public named mode in package " +
                                     other.getDeclaringPackage().getPackageName(), "XTSE3050");

            }
        }
        mode = (SimpleMode)top.getRuleManager().obtainMode(name, true);
        if (name.equals(Mode.UNNAMED_MODE_NAME)) {
            top.getRuleManager().setUnnamedModeExplicit(true);
        } else if (mode.getDeclaringComponent().getDeclaringPackage() != getContainingPackage()) {
            compileError("Mode name conflicts with a mode in a used package", "XTSE3050");
        } else {
            top.indexMode(decl);
            Visibility declaredVisibility = getDeclaredVisibility();
            Visibility actualVisibility = declaredVisibility == null ? Visibility.PRIVATE : declaredVisibility;
            VisibilityProvenance provenance = declaredVisibility == null ? VisibilityProvenance.DEFAULTED : VisibilityProvenance.EXPLICIT;
            mode.getDeclaringComponent().setVisibility(actualVisibility, provenance);
        }
    }

    public void prepareAttributes() throws XPathException {

        String nameAtt = null;
        String visibilityAtt = null;

        if (prepared) {
            return;
        }
        prepared = true;

        AttributeCollection atts = getAttributeList();
        Visibility visibility = Visibility.PRIVATE;
        
        for (int a = 0; a < atts.getLength(); a++) {
            String f = atts.getQName(a);
            switch (f) {
                case "streamable":
                    streamable = processStreamableAtt(atts.getValue(a));
                    break;
                case "name":
                    nameAtt = Whitespace.trim(atts.getValue(a));
                    if (!nameAtt.equals("#default")) {
                        try {
                            setObjectName(makeQName(nameAtt));
                        } catch (XPathException err) {
                            compileError(err);
                        }
                    }
                    break;
                case "use-accumulators":
                    String useAccAtt = atts.getValue(a);
                    accumulators = getPrincipalStylesheetModule().getStylesheetPackage()
                            .getAccumulatorRegistry().getUsedAccumulators(useAccAtt, this);

                    break;
                case "on-multiple-match": {
                    String att = Whitespace.trim(atts.getValue(a));
                    switch (att) {
                        case "fail":
                            failOnMultipleMatch = true;
                            break;
                        case "use-last":
                            failOnMultipleMatch = false;
                            break;
                        default:
                            invalidAttribute(f, "fail|use-last");
                            break;
                    }
                    break;
                }
                case "on-no-match":
                    String value = Whitespace.trim(atts.getValue(a));
                    assert value != null;
                    switch (value) {
                        case "text-only-copy":
                            // no action, this is the default
                            break;
                        case "shallow-copy":
                            defaultRules = ShallowCopyRuleSet.getInstance();
                            break;
                        case "deep-copy":
                            defaultRules = DeepCopyRuleSet.getInstance();
                            break;
                        case "shallow-skip":
                            defaultRules = ShallowSkipRuleSet.getInstance();
                            break;
                        case "deep-skip":
                            defaultRules = DeepSkipRuleSet.getInstance();
                            break;
                        case "fail":
                            defaultRules = FailRuleSet.getInstance();
                            break;
                        default:
                            invalidAttribute(f, "text-only-copy|shallow-copy|deep-copy|shallow-skip|deep-skip|fail");
                            break;
                    }
                    break;
                case "warning-on-multiple-match": {
                    String att = Whitespace.trim(atts.getValue(a));
                    warningOnMultipleMatch = processBooleanAttribute("warning-on-multiple-match", att);
                    break;
                }
                case "warning-on-no-match": {
                    String att = Whitespace.trim(atts.getValue(a));
                    warningOnNoMatch = processBooleanAttribute("warning-on-no-match", att);
                    break;
                }
                case "typed": {
                    String att = Whitespace.trim(atts.getValue(a));
                    checkAttributeValue("typed", att, false, new String[]{
                            "0", "1", "false", "lax", "no", "strict", "true", "unspecified", "yes"});
                    break;
                }
                case "visibility":
                    visibilityAtt = Whitespace.trim(atts.getValue(a));
                    visibility = interpretVisibilityValue(visibilityAtt, "");
                    if (visibility == Visibility.ABSTRACT) {
                        invalidAttribute(f, "public|private|final");
                    }
                    //if (visibility != Visibility.PRIVATE) {
                        mode.setDeclaredVisibility(visibility);
                    //}

                    break;
                default:
                    if (atts.getURI(a).equals(NamespaceConstant.SAXON) && atts.getLocalName(a).equals("trace")) {
                        String att = Whitespace.trim(atts.getValue(a));
                        traceMatching = processBooleanAttribute("saxon:trace", att);
                    } else {
                        checkUnknownAttribute(atts.getNodeName(a));
                    }
                    break;
            }
        }

        if (nameAtt == null && visibilityAtt != null && mode.getDeclaredVisibility() != Visibility.PRIVATE) {
            compileError("The unnamed mode must be private", "XTSE0020");
        }

        RuleManager manager = getCompilation().getPrincipalStylesheetModule().getRuleManager();
        if (getObjectName() == null) {
            mode = manager.getUnnamedMode();
        } else {
            Mode m = manager.obtainMode(getObjectName(), true);
            if (m instanceof SimpleMode) {
                mode = (SimpleMode)m;
            } else {
                compileError("Mode name refers to an overridden mode");
                mode = manager.getUnnamedMode();
            }
        }

        mode.setStreamable(streamable);
        if (streamable) {
            Mode omniMode = manager.obtainMode(Mode.OMNI_MODE, true);
            omniMode.setStreamable(true);
        }
        if (warningOnNoMatch) {
            defaultRules = new RuleSetWithWarnings(defaultRules);
        }
        mode.setBuiltInRuleSet(defaultRules);

        int recoveryPolicy;
        if (failOnMultipleMatch) {
            recoveryPolicy = Configuration.DO_NOT_RECOVER;
        } else if (warningOnMultipleMatch) {
            recoveryPolicy = Configuration.RECOVER_WITH_WARNINGS;
        } else {
            recoveryPolicy = Configuration.RECOVER_SILENTLY;
        }
        mode.setRecoveryPolicy(recoveryPolicy);
        mode.obtainDeclaringComponent(this);
        mode.setModeTracing(traceMatching);
    }

    public void validate(ComponentDeclaration decl) throws XPathException {
        checkTopLevel("XTSE0010", false);
        AttributeCollection atts = getAttributeList();

        for (int a = 0; a < atts.getLength(); a++) {
            String f = atts.getQName(a);
            if (f.equals("streamable") || f.equals("on-multiple-match") || f.equals("on-no-match") ||
                    f.equals("warning-on-multiple-match") || f.equals("warning-on-no-match") || f.equals("typed")) {
                String att = Whitespace.trim(atts.getValue(a));
                String normalizedAtt;
                if ("true".equals(att)||"1".equals(att)){
                    normalizedAtt = "yes";
                } else if ("false".equals(att)||"0".equals(att)){
                    normalizedAtt = "no";
                } else {
                    normalizedAtt = att;
                }
                mode.getActivePart().setExplicitProperty(f, normalizedAtt, decl.getPrecedence());
                if (mode.isMustBeTyped() && getContainingPackage().getTargetEdition().matches("JS\\d?")) {
                    compileWarning("In Saxon-JS, all data is untyped", "XTTE3110");
                }
            } else if (f.equals("use-accumulators") && accumulators != null /*Can be null after an error*/) {
                String[] names = new String[accumulators.size()];
                int i=0;
                for (Accumulator acc: accumulators) {
                    names[i++] = acc.getAccumulatorName().getEQName();
                }
                Arrays.sort(names);
                String allNames = Arrays.toString(names);
                mode.getActivePart().setExplicitProperty(f, allNames, decl.getPrecedence());
            }
        }
        checkEmpty();
        checkTopLevel("XTSE0010", false);
    }

    public void compileDeclaration(Compilation compilation, ComponentDeclaration decl) throws XPathException {
        StylesheetPackage pack = getPrincipalStylesheetModule().getStylesheetPackage();
        Component c = pack.getComponent(mode.getSymbolicName());
        if (c == null) {
            throw new AssertionError();
        }
        if (accumulators != null) {
            mode.setAccumulators(accumulators);
        }
    }


}