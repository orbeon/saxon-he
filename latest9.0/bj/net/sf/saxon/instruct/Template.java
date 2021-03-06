package net.sf.saxon.instruct;
import net.sf.saxon.Configuration;
import net.sf.saxon.value.SequenceType;
import net.sf.saxon.expr.Expression;
import net.sf.saxon.expr.XPathContext;
import net.sf.saxon.expr.XPathContextMajor;
import net.sf.saxon.om.StandardNames;
import net.sf.saxon.om.StructuredQName;
import net.sf.saxon.trace.InstructionInfo;
import net.sf.saxon.trace.InstructionInfoProvider;
import net.sf.saxon.trans.Rule;
import net.sf.saxon.trans.XPathException;

/**
* An xsl:template element in the style sheet.
*/

public class Template extends Procedure implements InstructionInfoProvider {

    // TODO: change the calling mechanism for named templates to use positional parameters
    // in the same way as functions. For templates that have both a match and a name attribute,
    // create a match template as a wrapper around the named template, resulting in separate
    // NamedTemplate and MatchTemplate classes. For named templates, perhaps compile into function
    // calls directly, the only difference being that context is retained.

    // The body of the template is represented by an expression,
    // which is responsible for any type checking that's needed.

    private int precedence;
    private int minImportPrecedence;
    private StructuredQName templateName;
    private boolean hasRequiredParams;
    private boolean bodyIsTailCallReturner;
    private SequenceType requiredType;
    private transient InstructionDetails details;

    /**
     * Create a template
     */

    public Template () {
        setHostLanguage(Configuration.XSLT);
    }

    /**
     * Initialize the template
     * @param templateName the name of the template (if any)
     * @param precedence the import precedence
     * @param minImportPrecedence the minimum import precedence to be considered in the search
     * performed by apply-imports
     */

    public void init (  StructuredQName templateName,
                        int precedence,
                        int minImportPrecedence) {
        this.templateName = templateName;
        this.precedence = precedence;
        this.minImportPrecedence = minImportPrecedence;
    }

    /**
     * Set the expression that forms the body of the template
     * @param body the body of the template
     */

    public void setBody(Expression body) {
        super.setBody(body);
        bodyIsTailCallReturner = (body instanceof TailCallReturner);
    }

    /**
     * Get the name of the template (if it is named)
     * @return the template name, or null if unnamed
     */

    public StructuredQName getTemplateName() {
        return templateName;
    }

    /**
     * Get the import precedence of the template
     * @return the import precedence (a higher number means a higher precedence)
     */

    public int getPrecedence() {
        return precedence;
    }

    /**
     * Get the minimum import precedence used by xsl:apply-imports
     * @return the minimum import precedence of templates that are candidates for calling by apply-imports
     */

    public int getMinImportPrecedence() {
        return minImportPrecedence;
    }

    /**
     * Set whether this template has one or more required parameters
     * @param has true if the template has at least one required parameter
     */

    public void setHasRequiredParams(boolean has) {
        hasRequiredParams = has;
    }

    /**
     * Ask whether this template has one or more required parameters
     * @return true if this template has at least one required parameter
     */

    public boolean hasRequiredParams() {
        return hasRequiredParams;
    }

    /**
     * Set the required type to be returned by this template
     * @param type the required type as defined in the "as" attribute on the xsl:template element
     */

    public void setRequiredType(SequenceType type) {
        requiredType = type;
    }

    /**
     * Get the required type to be returned by this template
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
     * Process the template, without returning any tail calls. This path is used by
     * xsl:apply-imports and xsl:next-match
     * @param context The dynamic context, giving access to the current node,
     * @param rule the template rule that caused this template to be invoked. When a template has
     * a match pattern defined as a union, there can be more than one Rule referring to the same template,
     * and further calls on next-match or apply-imports need to know which one in in force
     */

    public void apply(XPathContext context, Rule rule) throws XPathException {
        TailCall tc = applyLeavingTail(context, rule);
        while (tc != null) {
            tc = tc.processLeavingTail();
        }
    }

    /**
     * Process this template, with the possibility of returning a tail call package if the template
     * contains any tail calls that are to be performed by the caller.
     * @param context the XPath dynamic context
     * @param rule the template rule that caused this template to be invoked. When a template has
     * a match pattern defined as a union, there can be more than one Rule referring to the same template,
     * and further calls on next-match or apply-imports need to know which one in in force
     * @return null if the template exited normally; but if it was a tail call, details of the call
     * that hasn't been made yet and needs to be made by the caller
    */

    public TailCall applyLeavingTail(XPathContext context, Rule rule) throws XPathException {
        if (body==null) {
            // fast path for an empty template
            return null;
        }
        XPathContextMajor c2 = context.newContext();
        c2.setOrigin(this);
        c2.setCurrentTemplateRule(rule);

        if (bodyIsTailCallReturner) {
            return ((TailCallReturner)body).processLeavingTail(c2);
        } else {
            body.process(c2);
            return null;
        }
    }

    /**
     * Expand the template. Called when the template is invoked using xsl:call-template.
     * Invoking a template by this method does not change the current template.
     * @param context the XPath dynamic context
     * @return null if the template exited normally; but if it was a tail call, details of the call
     * that hasn't been made yet and needs to be made by the caller
    */

    public TailCall expand(XPathContext context) throws XPathException {
        if (bodyIsTailCallReturner) {
            return ((TailCallReturner)body).processLeavingTail(context);
        } else if (body != null) {
            body.process(context);
        }
        return null;
    }

    /**
     * Get the InstructionInfo details about the construct. This information isn't used for tracing,
     * but it is available when inspecting the context stack.
     */

    public InstructionInfo getInstructionInfo() {
        if (details==null) {
            details = new InstructionDetails();
            details.setSystemId(getSystemId());
            details.setLineNumber(getLineNumber());
            details.setConstructType(StandardNames.XSL_TEMPLATE);
            details.setProperty("template", this);
        }
        return details;
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
// The Initial Developer of the Original Code is Michael H. Kay.
//
// Portions created by (your name) are Copyright (C) (your legal entity). All Rights Reserved.
//
// Contributor(s):
// Portions marked "e.g." are from Edwin Glaser (edwin@pannenleiter.de)
//
