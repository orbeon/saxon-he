////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
// Copyright (c) 2015 Saxonica Limited.
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
import net.sf.saxon.type.SchemaType;
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
        fans.add("method");
        fans.add("output-version");
        fans.add("build-tree");
        fans.add("byte-order-mark");
        fans.add("cdata-section-elements");
        fans.add("encoding");
        fans.add("escape-uri-attributes");
        fans.add("doctype-system");
        fans.add("doctype-public");
        fans.add("html-version");
        fans.add("include-content-type");
        fans.add("indent");
        fans.add("item-separator");
        fans.add("media-type");
        fans.add("normalization-form");
        fans.add("omit-xml-declaration");
        fans.add("standalone");
        fans.add("suppress-indentation");
        fans.add("undeclare-prefixes");
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

    public void prepareAttributes() throws XPathException {
        AttributeCollection atts = getAttributeList();

        String formatAttribute = null;
        String hrefAttribute = null;
        String validationAtt = null;
        String typeAtt = null;
        String useCharacterMapsAtt = null;


        for (int a = 0; a < atts.getLength(); a++) {
            String f = atts.getQName(a);
            if (f.equals("format")) {
                formatAttribute = Whitespace.trim(atts.getValue(a));
                formatExpression = makeAttributeValueTemplate(formatAttribute, a);
            } else if (f.equals("href")) {
                hrefAttribute = Whitespace.trim(atts.getValue(a));
                href = makeAttributeValueTemplate(hrefAttribute, a);
            } else if (f.equals("validation")) {
                validationAtt = Whitespace.trim(atts.getValue(a));
            } else if (f.equals("type")) {
                typeAtt = Whitespace.trim(atts.getValue(a));
            } else if (f.equals("use-character-maps")) {
                useCharacterMapsAtt = Whitespace.trim(atts.getValue(a));
            } else if (fans.contains(f) || f.startsWith("{")) {
                // this is a serialization attribute
                String val = Whitespace.trim(atts.getValue(a));
                Expression exp = makeAttributeValueTemplate(val, a);
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

        if (formatAttribute != null) {
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
            serializationAttributes.put(new StructuredQName("", "", "use-character-maps"),
                    new StringLiteral(s));
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

        getContainingPackage().setCreatesSecondaryResultDocuments(true);

    }

    public static StructuredQName METHOD = new StructuredQName("", "", "method");
    public static StructuredQName BUILD_TREE = new StructuredQName("", "", "build-tree");

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
                ErrorExpression ee = new ErrorExpression("Call to xsl:result-document while in temporary output state", "XTDE1480", false);
                return ee;
            }
        }

        Properties globalProps;
        if (formatExpression == null) {
            try {
                globalProps = getPrincipalStylesheetModule().gatherOutputProperties(formatQName);
            } catch (XPathException err) {
                compileError("Named output format has not been defined", "XTDE1460");
                return null;
            }
        } else {
            globalProps = new Properties();
            getPrincipalStylesheetModule().setNeedsDynamicOutputProperties(true);
        }

        // If no serialization method was specified, we can work it out statically if the
        // first contained instruction is a literal result element. This saves effort at run-time.

        String method = null;
        if (formatExpression == null &&
                globalProps.getProperty("method") == null &&
                serializationAttributes.get(METHOD) == null) {
            AxisIterator kids = iterateAxis(AxisInfo.CHILD);
            NodeInfo first = kids.next();
            if (first instanceof LiteralResultElement) {
                if (first.getURI().equals(NamespaceConstant.XHTML) && first.getLocalPart().equals("html")) {
                    method = getEffectiveVersion() == 10 ? "xml" : "xhtml";
                } else if (first.getLocalPart().equalsIgnoreCase("html") && first.getURI().isEmpty()) {
                    method = "html";
                } else {
                    method = "xml";
                }
                globalProps.setProperty("method", method);
            }
        }

        Properties localProps = new Properties();

        HashSet<StructuredQName> fixed = new HashSet<StructuredQName>(10);
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
                    if (property.equals(METHOD)) {
                        method = s;
                    }
                } catch (XPathException e) {
                    if (NamespaceConstant.SAXON.equals(e.getErrorCodeNamespace())) {
                        compileWarning(e.getMessage(), e.getErrorCodeQName());
                    } else {
                        compileError(e);
                    }
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
                validationAction,
                schemaType,
                serializationAttributes,
                getContainingPackage().getCharacterMapIndex()
        );

        Expression content = compileSequenceConstructor(exec, decl, true);
        if (content == null) {
            content = Literal.makeLiteral(EmptySequence.getInstance());
        }
        inst.setContentExpression(content);
        inst.setAsynchronous(async);
        return inst;
    }

}

