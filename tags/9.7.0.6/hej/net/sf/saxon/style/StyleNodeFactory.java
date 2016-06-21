////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
// Copyright (c) 2015 Saxonica Limited.
// This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
// If a copy of the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
// This Source Code Form is "Incompatible With Secondary Licenses", as defined by the Mozilla Public License, v. 2.0.
////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

package net.sf.saxon.style;

import net.sf.saxon.Configuration;
import net.sf.saxon.event.PipelineConfiguration;
import net.sf.saxon.expr.Expression;
import net.sf.saxon.expr.parser.Location;
import net.sf.saxon.lib.NamespaceConstant;
import net.sf.saxon.om.*;
import net.sf.saxon.trans.IAccumulatorRegistry;
import net.sf.saxon.trans.SaxonErrorCode;
import net.sf.saxon.trans.XPathException;
import net.sf.saxon.tree.linked.ElementImpl;
import net.sf.saxon.tree.linked.NodeFactory;
import net.sf.saxon.tree.linked.NodeImpl;
import net.sf.saxon.tree.linked.TextImpl;
import net.sf.saxon.tree.util.AttributeCollectionImpl;
import net.sf.saxon.type.SchemaType;
import net.sf.saxon.type.Type;

import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactoryConfigurationError;
import java.util.List;

/**
 * Class StyleNodeFactory. <br>
 * A Factory for nodes in the stylesheet tree. <br>
 * Currently only allows Element nodes to be user-constructed.
 *
 * @author Michael H. Kay
 */

public class StyleNodeFactory implements NodeFactory {


    protected Configuration config;
    protected NamePool namePool;
    private Compilation compilation;
    private boolean topLevelModule;

    /**
     * Create the node factory for representing an XSLT stylesheet as a tree structure
     *
     * @param config the Saxon configuration
     * @param compilation the compilation episode (compiling one package)
     */

    public StyleNodeFactory(Configuration config, Compilation compilation) {
        this.config = config;
        this.compilation = compilation;
        namePool = config.getNamePool();
    }

    /**
     * Say that this is the top-level module of a package
     * @param topLevelModule true if this stylesheet module is the top level of a package; false
     * if it is included or imported
     */

    public void setTopLevelModule(boolean topLevelModule) {
        this.topLevelModule = topLevelModule;
    }

    /**
     * Ask whether this is the top-level module of a package
     * @return true if this stylesheet module is the top level of a package; false
     * if it is included or imported
     */

    public boolean isTopLevelModule() {
        return topLevelModule;
    }

    public Compilation getCompilation() {
        return compilation;
    }


    public Configuration getConfiguration() {
        return config;
    }

    /**
     * Get the XSLT processor version to be used by this node factory.
     *
     * @return the processor version, times ten. (Always 2.0 or 3.0 once the stylesheet version attribute
     *         has been read)
     */

    public int getXsltProcessorVersion() {
        return compilation.getProcessorVersion();
    }

    /**
     * Create an Element node. Note, if there is an error detected while constructing
     * the Element, we add the element anyway, and return success, but flag the element
     * with a validation error. This allows us to report more than
     * one error from a single compilation.
     */

    public ElementImpl makeElementNode(
            NodeInfo parent,
            NodeName elemName,
            SchemaType elemType,
            boolean isNilled,
            AttributeCollectionImpl attlist,
            NamespaceBinding[] namespaces,
            int namespacesUsed,
            PipelineConfiguration pipe,
            Location location,
            int sequence) {
        int nameCode = elemName.allocateNameCode(pipe.getConfiguration().getNamePool());
        boolean toplevel = parent instanceof XSLModuleRoot;
        String baseURI = null;
        int lineNumber = -1;
        int columnNumber = -1;
        baseURI = location.getSystemId();
        lineNumber = location.getLineNumber();
        columnNumber = location.getColumnNumber();

        if (parent instanceof DataElement) {
            DataElement d = new DataElement();
            d.setNamespaceDeclarations(namespaces, namespacesUsed);
            d.initialise(elemName, elemType, attlist, parent, sequence);
            d.setLocation(baseURI, lineNumber, columnNumber);
            return d;
        }

        int f = nameCode & 0xfffff;

        // Try first to make an XSLT element

        StyleElement e = makeXSLElement(f, (NodeImpl)parent);
        if ((e instanceof XSLStylesheet || e instanceof XSLPackage) && parent.getNodeKind() != Type.DOCUMENT) {
            e = new AbsentExtensionElement();
            final XPathException reason =
                    new XPathException(elemName.getDisplayName() + " can only appear at the outermost level", "XTSE0010");
            e.setValidationError(reason, StyleElement.REPORT_ALWAYS);
        }

        if (e != null) {  // recognized as an XSLT element

            e.setCompilation(compilation);
            e.setNamespaceDeclarations(namespaces, namespacesUsed);
            e.initialise(elemName, elemType, attlist, parent, sequence);
            e.setLocation(baseURI, lineNumber, columnNumber);
            // We're not catching multiple errors in the following attributes, but catching each of the
            // exceptions helps to ensure we don't report spurious errors through not processing some
            // of the attributes when others are faulty.
            try {
                e.processExtensionElementAttribute("");
            } catch (TransformerException err) {
                e.setValidationError(err, StyleElement.REPORT_ALWAYS);
            }
            try {
                e.processExcludedNamespaces("");
            } catch (TransformerException err) {
                e.setValidationError(err, StyleElement.REPORT_ALWAYS);
            }
            try {
                e.processVersionAttribute("");
            } catch (TransformerException err) {
                e.setValidationError(err, StyleElement.REPORT_ALWAYS);
            }
            e.processDefaultXPathNamespaceAttribute("");
            try {
                e.processExpandTextAttribute("");
            } catch (XPathException err) {
                e.setValidationError(err, StyleElement.REPORT_ALWAYS);
            }

            if (e instanceof XSLModuleRoot && compilation.getProcessorVersion() == 0) {
                int effectiveVersion = e.getEffectiveVersion();
                compilation.setProcessorVersion(effectiveVersion == 30 ? 30 : 20);
            }
            return e;

        }

        String uri = elemName.getURI();

        if (toplevel && !uri.isEmpty() && !uri.equals(NamespaceConstant.XSLT)) {
            DataElement d = new DataElement();
            d.setNamespaceDeclarations(namespaces, namespacesUsed);
            d.initialise(elemName, elemType, attlist, parent, sequence);
            d.setLocation(baseURI, lineNumber, columnNumber);
            return d;

        } else {   // not recognized as an XSLT element, not top-level

            String localname = elemName.getLocalPart();
            StyleElement temp = null;

            // Detect a misspelt XSLT element, or a 3.0 element used in a 2.0 stylesheet

            if (uri.equals(NamespaceConstant.XSLT)) {
                if (parent instanceof XSLStylesheet) {
                    if (((XSLStylesheet) parent).getEffectiveVersion() <= 20) {
                        temp = new AbsentExtensionElement();
                        temp.setValidationError(new XPathException("Unknown top-level XSLT declaration"),
                                StyleElement.REPORT_UNLESS_FORWARDS_COMPATIBLE);
                    }
                } else {
                    temp = new AbsentExtensionElement();
                    temp.initialise(elemName, elemType, attlist, parent, sequence);
                    temp.setLocation(baseURI, lineNumber, columnNumber);
                    try {
                        temp.processStandardAttributes("");
                        if (temp.getEffectiveVersion() > 20) {
                            temp.setValidationError(new XPathException("Unknown XSLT instruction"),
                                    StyleElement.REPORT_UNLESS_FALLBACK_AVAILABLE);
                        } else {
                            temp.setValidationError(new XPathException("Unknown XSLT instruction"),
                                    StyleElement.REPORT_IF_INSTANTIATED);
                        }
                    } catch (XPathException err) {
                        temp.setValidationError(err, StyleElement.REPORT_ALWAYS);
                    }
                }
            }

            // Detect an unrecognized element in the Saxon namespace

            if (uri.equals(NamespaceConstant.SAXON)) {
                XPathException te = new XPathException(elemName.getDisplayName() +
                        " is not recognized as a Saxon instruction");
                te.setLocator(location.saveLocation());
                te.setErrorCode(SaxonErrorCode.SXWN9008);
                pipe.getErrorListener().warning(te);
            }

            Class assumedClass = LiteralResultElement.class;

            // We can't work out the final class of the node until we've examined its attributes
            // such as version and extension-element-prefixes; but we can have a good guess, and
            // change it later if need be.

            if (temp == null) {
                temp = new LiteralResultElement();
            }

            temp.setNamespaceDeclarations(namespaces, namespacesUsed);

            try {
                temp.setCompilation(compilation);
                temp.initialise(elemName, elemType, attlist, parent, sequence);
                temp.setLocation(baseURI, lineNumber, columnNumber);
                temp.processStandardAttributes(NamespaceConstant.XSLT);
            } catch (XPathException err) {
                temp.setValidationError(err, StyleElement.REPORT_ALWAYS);
            }

            // Now we work out what class of element we really wanted, and change it if necessary

            TransformerException reason;
            Class actualClass;

            if (uri.equals(NamespaceConstant.XSLT)) {
                reason = new XPathException("Unknown XSLT element: " + localname);
                ((XPathException) reason).setErrorCode("XTSE0010");
                ((XPathException) reason).setIsStaticError(true);
                actualClass = AbsentExtensionElement.class;
                temp.setValidationError(reason, StyleElement.REPORT_UNLESS_FALLBACK_AVAILABLE);

            } else if (temp.isExtensionNamespace(uri) && !toplevel) {

                // if we can't instantiate an extension element, we don't give up
                // immediately, because there might be an xsl:fallback defined. We
                // create a surrogate element called AbsentExtensionElement, and
                // save the reason for failure just in case there is no xsl:fallback

                actualClass = AbsentExtensionElement.class;
                if (NamespaceConstant.isReserved(uri)) {
                    reason = new XPathException("Cannot use a reserved namespace for extension instructions", "XTSE0800");
                    temp.setValidationError(reason, StyleElement.REPORT_ALWAYS);
                } else {
                    reason = new XPathException("Unknown extension instruction", "XTDE1450", temp);
                    temp.setValidationError(reason, StyleElement.REPORT_IF_INSTANTIATED);
                }

            } else {
                actualClass = LiteralResultElement.class;
            }

            StyleElement node;
            if (actualClass.equals(assumedClass)) {
                node = temp;    // the original element will do the job
            } else {
                try {
                    node = (StyleElement) actualClass.newInstance();
                } catch (InstantiationException err1) {
                    throw new TransformerFactoryConfigurationError(err1, "Failed to create instance of " + actualClass.getName());
                } catch (IllegalAccessException err2) {
                    throw new TransformerFactoryConfigurationError(err2, "Failed to access class " + actualClass.getName());
                }
                node.substituteFor(temp);   // replace temporary node with the new one
            }
            return node;
        }
    }

    /**
     * Make an XSL element node
     *
     * @param f      the fingerprint of the node name
     * @param parent the parent node
     * @return the constructed element node
     */

    /*@Nullable*/
    protected StyleElement makeXSLElement(int f, NodeImpl parent) {
        switch (f) {
            case StandardNames.XSL_ANALYZE_STRING:
                return new XSLAnalyzeString();
            case StandardNames.XSL_APPLY_IMPORTS:
                return new XSLApplyImports();
            case StandardNames.XSL_APPLY_TEMPLATES:
                return new XSLApplyTemplates();
            case StandardNames.XSL_ATTRIBUTE:
                return new XSLAttribute();
            case StandardNames.XSL_ATTRIBUTE_SET:
                return new XSLAttributeSet();
            case StandardNames.XSL_CALL_TEMPLATE:
                return new XSLCallTemplate();
            case StandardNames.XSL_CHARACTER_MAP:
                return new XSLCharacterMap();
            case StandardNames.XSL_CHOOSE:
                return new XSLChoose();
            case StandardNames.XSL_COMMENT:
                return new XSLComment();
            case StandardNames.XSL_COPY:
                return new XSLCopy();
            case StandardNames.XSL_COPY_OF:
                return new XSLCopyOf();
            case StandardNames.XSL_DECIMAL_FORMAT:
                return new XSLDecimalFormat();
            case StandardNames.XSL_DOCUMENT:
                return new XSLDocument();
            case StandardNames.XSL_ELEMENT:
                return new XSLElement();
            case StandardNames.XSL_FALLBACK:
                return new XSLFallback();
            case StandardNames.XSL_FOR_EACH:
                return new XSLForEach();
            case StandardNames.XSL_FOR_EACH_GROUP:
                return new XSLForEachGroup();
            case StandardNames.XSL_FUNCTION:
                return new XSLFunction();
            case StandardNames.XSL_IF:
                return new XSLIf();
            case StandardNames.XSL_IMPORT:
                return new XSLImport();
            case StandardNames.XSL_IMPORT_SCHEMA:
                return new XSLImportSchema();
            case StandardNames.XSL_INCLUDE:
                return new XSLInclude();
            case StandardNames.XSL_KEY:
                return new XSLKey();
            case StandardNames.XSL_MATCHING_SUBSTRING:
                return new XSLMatchingSubstring();
            case StandardNames.XSL_MESSAGE:
                return new XSLMessage();
            case StandardNames.XSL_NEXT_MATCH:
                return new XSLNextMatch();
            case StandardNames.XSL_NON_MATCHING_SUBSTRING:
                return new XSLMatchingSubstring();    //sic
            case StandardNames.XSL_NUMBER:
                return new XSLNumber();
            case StandardNames.XSL_NAMESPACE:
                return new XSLNamespace();
            case StandardNames.XSL_NAMESPACE_ALIAS:
                return new XSLNamespaceAlias();
            case StandardNames.XSL_OTHERWISE:
                return new XSLOtherwise();
            case StandardNames.XSL_OUTPUT:
                return new XSLOutput();
            case StandardNames.XSL_OUTPUT_CHARACTER:
                return new XSLOutputCharacter();
            case StandardNames.XSL_PACKAGE: // here because we construct a package from an ordinary 2.0 stylesheet
                return new XSLPackage();
            case StandardNames.XSL_PARAM:
                return parent instanceof XSLModuleRoot || isXSLOverride(parent) ? new XSLGlobalParam() : new XSLLocalParam();
            case StandardNames.XSL_PERFORM_SORT:
                return new XSLPerformSort();
            case StandardNames.XSL_PRESERVE_SPACE:
                return new XSLPreserveSpace();
            case StandardNames.XSL_PROCESSING_INSTRUCTION:
                return new XSLProcessingInstruction();
            case StandardNames.XSL_RESULT_DOCUMENT:
                return new XSLResultDocument();
            case StandardNames.XSL_SEQUENCE:
                return new XSLSequence();
            case StandardNames.XSL_SORT:
                return new XSLSort();
            case StandardNames.XSL_STRIP_SPACE:
                return new XSLPreserveSpace();
            case StandardNames.XSL_STYLESHEET:
                return topLevelModule ? new XSLPackage() : new XSLStylesheet();
            case StandardNames.XSL_TEMPLATE:
                return new XSLTemplate();
            case StandardNames.XSL_TEXT:
                return new XSLText();
            case StandardNames.XSL_TRANSFORM:
                return topLevelModule ? new XSLPackage() : new XSLStylesheet();
            case StandardNames.XSL_VALUE_OF:
                return new XSLValueOf();
            case StandardNames.XSL_VARIABLE:
                return parent instanceof XSLModuleRoot || isXSLOverride(parent) ? new XSLGlobalVariable() : new XSLLocalVariable();
            case StandardNames.XSL_WITH_PARAM:
                return new XSLWithParam();
            case StandardNames.XSL_WHEN:
                return new XSLWhen();
            default:
                return null;
        }
    }

    /**
     * Make a text node
     *
     * @param content the content of the text node
     * @return the constructed text node
     */
    public TextImpl makeTextNode(NodeInfo parent, CharSequence content) {
        return new TextImpl(content.toString());
    }

    /**
     * Method to support the element-available() function
     *
     *
     * @param uri       the namespace URI
     * @param localName the local Name
     * @param instructionsOnly true if only instruction elements qualify
     * @return true if an extension element of this name is recognized
     */

    public boolean isElementAvailable(String uri, String localName, boolean instructionsOnly) {
        int fingerprint = namePool.getFingerprint(uri, localName);
        if (uri.equals(NamespaceConstant.XSLT)) {
            if (fingerprint == -1) {
                return false;     // all names are pre-registered
            }
            StyleElement e = makeXSLElement(fingerprint, null);
            if (e != null) {
                return !instructionsOnly || e.isInstruction();
            }
        }
        return false;
    }

    public IAccumulatorRegistry makeAccumulatorManager() {
        return null;
    }

    /**
     * Validate a text node in the stylesheet
     *
     * @param node the text node
     * @return true if the node is a text value template containing one or more expressions
     * @throws XPathException if the node is invalid
     */

    public boolean validateTextNode(NodeInfo node) throws XPathException {
        // no action (overridden in Saxon-PE subclass)
        return false;
    }

    /**
     * Compile a content value text node. Dummy implementation (these can exist only in Saxon-PE or higher)
     * @param node the text node potentially containing the template
     * @param contents a list to which expressions representing the fixed and variable parts of the content template
     * will be appended
     * @throws XPathException if a static error is found
     */

    public void compileContentValueTemplate(TextImpl node, List<Expression> contents) throws XPathException {
        // no action
    }

    /**
     * Test whether an element is an XSLOverride element
     * @param node the element to be tested
     * @return true if the element is an xsl:override element
     */

     public boolean isXSLOverride(NodeImpl node) {
         return node.getFingerprint() == StandardNames.XSL_OVERRIDE;
     }

    /**
     * Create a stylesheet package
     * @param node the XSLPackage element
     * @return a new stylesheet package
     */

    public PrincipalStylesheetModule newPrincipalModule(XSLPackage node) throws XPathException {
        return new PrincipalStylesheetModule(node);
    }

    /**
     * Make an expression representing a sequence constructor with contained xsl:on-empty and xsl:on-non-empty
     * instructions
     * @param content the full list of compiled instructions, including OnEmpty and OnNotEmpty expressions
     * @return the expression to evaluate the extended sequence constructor
     * @throws XPathException if (for example) XSLT 3.0 is not enabled.
     */

    public Expression makeConditionalBlock(List<Expression> content) throws XPathException {
        throw new XPathException("xs:on-empty and xsl:non-empty require XSLT 3.0 to be enabled");
    }
}

