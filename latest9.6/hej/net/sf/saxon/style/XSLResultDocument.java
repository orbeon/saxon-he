////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
// Copyright (c) 2014 Saxonica Limited.
// This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
// If a copy of the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
// This Source Code Form is "Incompatible With Secondary Licenses", as defined by the Mozilla Public License, v. 2.0.
////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

package net.sf.saxon.style;

import net.sf.saxon.expr.ErrorExpression;
import net.sf.saxon.expr.Expression;
import net.sf.saxon.expr.Literal;
import net.sf.saxon.expr.StringLiteral;
import net.sf.saxon.expr.instruct.ResultDocument;
import net.sf.saxon.lib.FeatureKeys;
import net.sf.saxon.lib.NamespaceConstant;
import net.sf.saxon.lib.Validation;
import net.sf.saxon.om.*;
import net.sf.saxon.trans.SaxonErrorCode;
import net.sf.saxon.trans.XPathException;
import net.sf.saxon.tree.iter.AxisIterator;
import net.sf.saxon.type.ItemType;
import net.sf.saxon.type.SchemaType;
import net.sf.saxon.value.DecimalValue;
import net.sf.saxon.value.EmptySequence;
import net.sf.saxon.value.Whitespace;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Properties;

/**
 * An xsl:result-document element in the stylesheet. <BR>
 * The xsl:result-document element takes an attribute href="filename". The filename will
 * often contain parameters, e.g. {position()} to ensure that a different file is produced
 * for each element instance. <BR>
 * There is a further attribute "name" which determines the format of the
 * output file, it identifies the name of an xsl:output element containing the output
 * format details.
 */

public class XSLResultDocument extends StyleElement {

    private static final HashSet<String> fans = new HashSet<String>(25);    // formatting attribute names

    static {
        fans.add(StandardNames.METHOD);
        fans.add(StandardNames.OUTPUT_VERSION);
        fans.add(StandardNames.HTML_VERSION);
        fans.add(StandardNames.BYTE_ORDER_MARK);
        fans.add(StandardNames.INDENT);
        fans.add(StandardNames.ENCODING);
        fans.add(StandardNames.MEDIA_TYPE);
        fans.add(StandardNames.DOCTYPE_SYSTEM);
        fans.add(StandardNames.DOCTYPE_PUBLIC);
        fans.add(StandardNames.OMIT_XML_DECLARATION);
        fans.add(StandardNames.STANDALONE);
        fans.add(StandardNames.CDATA_SECTION_ELEMENTS);
        fans.add(StandardNames.INCLUDE_CONTENT_TYPE);
        fans.add(StandardNames.ESCAPE_URI_ATTRIBUTES);
        fans.add(StandardNames.UNDECLARE_PREFIXES);
        fans.add(StandardNames.NORMALIZATION_FORM);
        fans.add(StandardNames.SAXON_NEXT_IN_CHAIN);
        fans.add(StandardNames.SAXON_CHARACTER_REPRESENTATION);
        fans.add(StandardNames.SAXON_INDENT_SPACES);
        fans.add(StandardNames.SAXON_REQUIRE_WELL_FORMED);
        fans.add(StandardNames.SAXON_SUPPRESS_INDENTATION);
        fans.add(StandardNames.SAXON_ATTRIBUTE_ORDER);
    }

    private Expression href;
    private StructuredQName formatQName;     // used when format is a literal string
    private Expression formatExpression;     // used when format is an AVT
    private int validationAction = Validation.STRIP;
    private SchemaType schemaType = null;
    private Map<StructuredQName, Expression> serializationAttributes = new HashMap<StructuredQName, Expression>(10);
    private boolean async = true;

    /**
     * Determine whether this node is an instruction.
     *
     * @return true - it is an instruction
     */

    public boolean isInstruction() {
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

    /**
     * Determine the type of item returned by this instruction (only relevant if
     * it is an instruction). Default implementation returns Type.ITEM, indicating
     * that we don't know, it might be anything. Returns null in the case of an element
     * such as xsl:sort or xsl:variable that can appear in a sequence constructor but
     * contributes nothing to the result sequence.
     *
     * @return the item type returned
     */

    protected ItemType getReturnedItemType() {
        return null;
    }

    public void prepareAttributes() throws XPathException {
        AttributeCollection atts = getAttributeList();

        String formatAttribute = null;
        String hrefAttribute = null;
        String validationAtt = null;
        String typeAtt = null;
        String useCharacterMapsAtt = null;


        for (int a = 0; a < atts.getLength(); a++) {
            String f = atts.getQName(a);
            if (f.equals(StandardNames.FORMAT)) {
                formatAttribute = Whitespace.trim(atts.getValue(a));
            } else if (f.equals(StandardNames.HREF)) {
                hrefAttribute = Whitespace.trim(atts.getValue(a));
            } else if (f.equals(StandardNames.VALIDATION)) {
                validationAtt = Whitespace.trim(atts.getValue(a));
            } else if (f.equals(StandardNames.TYPE)) {
                typeAtt = Whitespace.trim(atts.getValue(a));
            } else if (f.equals(StandardNames.USE_CHARACTER_MAPS)) {
                useCharacterMapsAtt = Whitespace.trim(atts.getValue(a));
            } else if (fans.contains(f) || f.startsWith("{")) {
                // this is a serialization attribute
                String val = Whitespace.trim(atts.getValue(a));
                Expression exp = makeAttributeValueTemplate(val);
                serializationAttributes.put(new StructuredQName(atts.getPrefix(a), atts.getURI(a), atts.getLocalName(a)), exp);
            } else if (atts.getLocalName(a).equals("asynchronous") && atts.getURI(a).equals(NamespaceConstant.SAXON)) {
                async = processBooleanAttribute("saxon:asynchronous", atts.getValue(a));
                if (getCompilation().getCompilerInfo().isCompileWithTracing()) {
                    async = false;
                } else if (!"EE".equals(getConfiguration().getEditionCode())) {
                    compileWarning("saxon:asynchronous - ignored when not running Saxon-EE",
                            SaxonErrorCode.SXWN9013);
                    async = false;
                }
            } else {
                checkUnknownAttribute(atts.getNodeName(a));
            }
        }

        if (hrefAttribute == null) {
            //href = StringValue.EMPTY_STRING;
        } else {
            href = makeAttributeValueTemplate(hrefAttribute);
        }

        if (formatAttribute != null) {
            formatExpression = makeAttributeValueTemplate(formatAttribute);
            if (formatExpression instanceof StringLiteral) {
                try {
                    formatQName = makeQName(((StringLiteral) formatExpression).getStringValue());
                    formatExpression = null;
                } catch (NamespaceException err) {
                    compileError(err.getMessage(), "XTSE0280");
                } catch (XPathException err) {
                    compileError(err.getMessage(), "XTDE1460");
                }
            }
        }

        if (validationAtt == null) {
            validationAction = getDefaultValidation();
        } else {
            validationAction = validateValidationAttribute(validationAtt);
        }
        if (typeAtt != null) {
            if (!isSchemaAware()) {
                compileError("The @type attribute is available only with a schema-aware XSLT processor", "XTSE1660");
            }
            schemaType = getSchemaType(typeAtt);
            validationAction = Validation.BY_TYPE;
        }

        if (typeAtt != null && validationAtt != null) {
            compileError("The @validation and @type attributes are mutually exclusive", "XTSE1505");
        }

        if (useCharacterMapsAtt != null) {
            String s = XSLOutput.prepareCharacterMaps(this, useCharacterMapsAtt, new Properties());
            serializationAttributes.put(new StructuredQName("", "", StandardNames.USE_CHARACTER_MAPS),
                    new StringLiteral(s, this));
        }
    }

    public void validate(ComponentDeclaration decl) throws XPathException {
        if (href != null && !getConfiguration().getBooleanProperty(FeatureKeys.ALLOW_EXTERNAL_FUNCTIONS)) {
            compileError("xsl:result-document is disabled when extension functions are disabled");
        }
        href = typeCheck("href", href);
        formatExpression = typeCheck("format", formatExpression);

        for (StructuredQName prop : serializationAttributes.keySet()) {
            final Expression exp1 = serializationAttributes.get(prop);
            final Expression exp2 = typeCheck(prop.getDisplayName(), exp1);
            if (exp1 != exp2) {
                serializationAttributes.put(prop, exp2);
            }
        }

        getCompilation().getStylesheetPackage().setCreatesSecondaryResultDocuments(true);

    }

    public static StructuredQName METHOD = new StructuredQName("", "", "method");

    /*@Nullable*/
    public Expression compile(Compilation exec, ComponentDeclaration decl) throws XPathException {

        // Check that the call is not within xsl:variable or xsl:function.
        // This is a dynamic error, but worth detecting statically.
        // In fact this is a bit of a fudge. If a function or variable is inlined, we sometimes don't detect
        // XTDE1480 at run-time. Doing this static check improves our chances, though it won't catch all cases.
        AxisIterator ai = iterateAxis(AxisInfo.ANCESTOR);
        while (true) {
            NodeInfo node = ai.next();
            if (node == null) {
                break;
            }
            if (node instanceof XSLGeneralVariable || node instanceof XSLFunction) {
                issueWarning("An xsl:result-document instruction inside " + node.getDisplayName() +
                        " will always fail at run-time", this);
                XPathException err = new XPathException("Call to xsl:result-document while in temporary output state", "XTDE1480");
                err.setLocator(this);
                return new ErrorExpression(err);
            }
        }

        Properties globalProps;
        if (formatExpression == null) {
            try {
                globalProps = getContainingPackage().gatherOutputProperties(formatQName);
            } catch (XPathException err) {
                compileError("Named output format has not been defined", "XTDE1460");
                return null;
            }
        } else {
            globalProps = new Properties();
            getContainingPackage().setNeedsDynamicOutputProperties(true);
        }

        // If no serialization method was specified, we can work it out statically if the
        // first contained instruction is a literal result element. This saves effort at run-time.

        if (formatExpression == null &&
                globalProps.getProperty("method") == null &&
                serializationAttributes.get(METHOD) == null) {
            AxisIterator kids = iterateAxis(AxisInfo.CHILD);
            NodeInfo first = kids.next();
            if (first instanceof LiteralResultElement) {
                if (first.getURI().equals(NamespaceConstant.XHTML) && first.getLocalPart().equals("html")) {
                    String method = getEffectiveVersion().equals(DecimalValue.ONE) ? "xml" : "xhtml";
                    globalProps.setProperty("method", method);
                } else if (first.getLocalPart().equalsIgnoreCase("html") && first.getURI().length() == 0) {
                    globalProps.setProperty("method", "html");
                } else {
                    globalProps.setProperty("method", "xml");
                }
            }
        }

        Properties localProps = new Properties();

        HashSet<StructuredQName> fixed = new HashSet<StructuredQName>(10);
        boolean needsNamespaceContext = formatExpression != null;
        NamespaceResolver namespaceResolver = getStaticContext().getNamespaceResolver();
        for (StructuredQName property : serializationAttributes.keySet()) {
            Expression exp = serializationAttributes.get(property);
            if (exp instanceof StringLiteral) {
                String s = ((StringLiteral) exp).getStringValue();
                String lname = property.getLocalPart();
                String uri = property.getURI();
                try {

                    ResultDocument.setSerializationProperty(localProps, uri, lname, s,
                            namespaceResolver, false, exec.getConfiguration());
                    fixed.add(property);
                } catch (XPathException e) {
                    if (NamespaceConstant.SAXON.equals(e.getErrorCodeNamespace())) {
                        compileWarning(e.getMessage(), e.getErrorCodeQName());
                    } else {
                        compileError(e);
                    }
                }
            } else {
                String lname = property.getLocalPart();
                if (lname.equals("method") || lname.equals("cdata-section-elements") ||
                        lname.equals("suppress-indentation")) {
                    needsNamespaceContext = true;
                }
            }
        }
        for (StructuredQName p : fixed) {
            serializationAttributes.remove(p);
        }

        ResultDocument inst = new ResultDocument(globalProps,
                localProps,
                href,
                formatExpression,
                getBaseURI(),
                validationAction,
                schemaType,
                serializationAttributes,
                getContainingPackage().getCharacterMapIndex(),
                needsNamespaceContext ? namespaceResolver : null);

        Expression b = compileSequenceConstructor(exec, decl, iterateAxis(AxisInfo.CHILD), true);
        if (b == null) {
            b = Literal.makeLiteral(EmptySequence.getInstance(), this);
        }
        inst.setContentExpression(b);
        inst.setAsynchronous(async);
        return inst;
    }

}

