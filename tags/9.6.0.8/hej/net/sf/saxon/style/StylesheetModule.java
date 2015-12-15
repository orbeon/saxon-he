////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
// Copyright (c) 2014 Saxonica Limited.
// This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
// If a copy of the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
// This Source Code Form is "Incompatible With Secondary Licenses", as defined by the Mozilla Public License, v. 2.0.
////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

package net.sf.saxon.style;

import net.sf.saxon.Configuration;
import net.sf.saxon.event.*;
import net.sf.saxon.lib.AugmentedSource;
import net.sf.saxon.lib.NamespaceConstant;
import net.sf.saxon.lib.ParseOptions;
import net.sf.saxon.lib.Validation;
import net.sf.saxon.om.AxisInfo;
import net.sf.saxon.om.DocumentURI;
import net.sf.saxon.om.NodeInfo;
import net.sf.saxon.om.StylesheetSpaceStrippingRule;
import net.sf.saxon.trans.XPathException;
import net.sf.saxon.tree.iter.AxisIterator;
import net.sf.saxon.tree.linked.DocumentImpl;
import net.sf.saxon.tree.linked.LinkedTreeBuilder;
import net.sf.saxon.type.Type;
import net.sf.saxon.value.NestedIntegerValue;
import net.sf.saxon.value.Whitespace;
import org.xml.sax.InputSource;
import org.xml.sax.XMLReader;

import javax.xml.transform.Source;
import javax.xml.transform.TransformerException;
import javax.xml.transform.URIResolver;
import javax.xml.transform.sax.SAXSource;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;

/**
 * A stylesheet module represents a module of a stylesheet. It is possible for two modules
 * to share the same stylesheet tree in the case where two includes or imports reference
 * the same URI; in this case the two modules will typically have a different import precedence.
 */
public class StylesheetModule {

    private StyleElement rootElement;
    private int precedence;
    private int minImportPrecedence;
    private StylesheetModule importer;
    boolean wasIncluded;


    // the value of the inputTypeAnnotations attribute on this module, combined with the values
    // on all imported/included modules. This is a combination of the bit-significant values
    // ANNOTATION_STRIP and ANNOTATION_PRESERVE.
    private int inputTypeAnnotations = 0;

    // A list of all the declarations in the stylesheet and its descendants, in increasing precedence order
    protected List<ComponentDeclaration> topLevel = new ArrayList<ComponentDeclaration>();

    public StylesheetModule(StyleElement rootElement, int precedence) {
        this.rootElement = rootElement;
        this.precedence = precedence;
    }

    /**
     * Build the tree representation of a stylesheet module
     *
     *
     * @param styleSource the source of the module
     * @param topLevelModule true if this module is the outermost module of a package
     * @param compilation the XSLT compilation episode
     * @param precedence the import precedence for static variables declared
     * in the module. (This is handled differently from the precedence of other components
     * because it needs to be allocated purely sequentially).   @return the root Document node of the tree containing the stylesheet
     *         module
     * @return the tree representation of the XML document containing the stylesheet module
     * @throws net.sf.saxon.trans.XPathException
     *          if XML parsing or tree
     *          construction fails
     */
    public static DocumentImpl loadStylesheetModule(
            Source styleSource, boolean topLevelModule, Compilation compilation, NestedIntegerValue precedence) throws XPathException {

        String systemId = styleSource.getSystemId();
        DocumentURI docURI = systemId == null ? null : new DocumentURI(systemId);
        if (systemId != null && compilation.getImportStack().contains(docURI)) {
            throw new XPathException("The stylesheet module includes/imports itself directly or indirectly", "XTSE0180");
        }
        compilation.getImportStack().push(docURI);

        Configuration config = compilation.getConfiguration();
        PipelineConfiguration pipe = config.makePipelineConfiguration();
        pipe.setErrorListener(compilation.getCompilerInfo().getErrorListener());
        LinkedTreeBuilder styleBuilder = new LinkedTreeBuilder(pipe);
        pipe.setURIResolver(compilation.getCompilerInfo().getURIResolver());
        styleBuilder.setSystemId(styleSource.getSystemId());
        styleBuilder.setNodeFactory(compilation.getStyleNodeFactory(topLevelModule));
        styleBuilder.setLineNumbering(true);

        UseWhenFilter useWhenFilter = new UseWhenFilter(compilation, styleBuilder, precedence);
        StartTagBuffer startTagBuffer = new StartTagBuffer(useWhenFilter);
        useWhenFilter.setStartTagBuffer(startTagBuffer);

        StylesheetSpaceStrippingRule rule = new StylesheetSpaceStrippingRule(config.getNamePool());
        Stripper styleStripper = new Stripper(rule, startTagBuffer);
        CommentStripper commentStripper = new CommentStripper(styleStripper);

        // build the stylesheet document

        DocumentImpl doc;

        ParseOptions options;
        if (styleSource instanceof AugmentedSource) {
            options = ((AugmentedSource) styleSource).getParseOptions();
            styleSource = ((AugmentedSource) styleSource).getContainedSource();
        } else {
            options = new ParseOptions();
        }
        options.setSchemaValidationMode(Validation.STRIP);
        options.setDTDValidationMode(Validation.STRIP);
        options.setLineNumbering(true);
        options.setStripSpace(Whitespace.NONE);
        options.setErrorListener(pipe.getErrorListener());
        try {
            if (options.getXMLReader() == null && Configuration.getPlatform().isJava()) {
                XMLReader styleParser = config.getStyleParser();
                options.setXMLReader(styleParser);
                Sender.send(styleSource, commentStripper, options);
                config.reuseStyleParser(styleParser);
            } else {
                Sender.send(styleSource, commentStripper, options);
            }
            doc = (DocumentImpl) styleBuilder.getCurrentRoot();
            styleBuilder.reset();
            compilation.getImportStack().pop();
            return doc;
        } catch (XPathException err) {
            if (topLevelModule && !err.hasBeenReported()) {  // bug 2244
                compilation.reportError(err);
            }
            throw err;
        } finally {
            if (options.isPleaseCloseAfterUse()) {
                ParseOptions.close(styleSource);
            }
        }
    }

    /**
     * Get the stylesheet specification(s) associated
     * via the xml-stylesheet processing instruction (see
     * http://www.w3.org/TR/xml-stylesheet/) with the document
     * document specified in the source parameter, and that match
     * the given criteria.  Note that it is possible to return several
     * stylesheets, in which case they are applied as if they were
     * a list of imports or cascades.
     *
     * @param config  The Saxon Configuration
     * @param source  The XML source document.
     * @param media   The media attribute to be matched.  May be null, in which
     *                case the prefered templates will be used (i.e. alternate = no).
     * @param title   The value of the title attribute to match.  May be null.
     * @param charset The value of the charset attribute to match.  May be null.
     * @return A Source object suitable for passing to the TransformerFactory.
     * @throws net.sf.saxon.trans.XPathException
     *          if any problems occur
     */


    public static Source getAssociatedStylesheet(
            Configuration config, URIResolver resolver, Source source, String media, String title, String charset)
            throws XPathException {
        PIGrabber grabber = new PIGrabber(new Sink(config.makePipelineConfiguration()));
        grabber.setFactory(config);
        grabber.setCriteria(media, title);
        grabber.setBaseURI(source.getSystemId());
        grabber.setURIResolver(resolver);

        try {
            Sender.send(source, grabber, null);
            // this parse will be aborted when the first start tag is found
        } catch (XPathException err) {
            if (grabber.isTerminated()) {
                // do nothing
            } else {
                throw new XPathException(
                        "Failed while looking for xml-stylesheet PI", err);
            }
        }

        try {
            Source[] sources = grabber.getAssociatedStylesheets();
            if (sources == null) {
                throw new XPathException(
                        "No matching <?xml-stylesheet?> processing instruction found");
            }
            return compositeStylesheet(config, source.getSystemId(), sources);
        } catch (TransformerException err) {
            if (err instanceof XPathException) {
                throw (XPathException) err;
            } else {
                throw new XPathException(err);
            }
        }
    }

    /**
     * Process a series of stylesheet inputs, treating them in import or cascade
     * order.  This is mainly for support of the getAssociatedStylesheets
     * method, but may be useful for other purposes.
     *
     * @param config  the Saxon configuration
     * @param baseURI the base URI to be used for the synthesized composite stylesheet
     * @param sources An array of Source objects representing individual stylesheets.
     * @return A Source object representing a composite stylesheet.
     * @throws XPathException if there is a static error in the stylesheet
     */

    private static Source compositeStylesheet(Configuration config, String baseURI, Source[] sources)
            throws XPathException {

        if (sources.length == 1) {
            return sources[0];
        } else if (sources.length == 0) {
            throw new XPathException(
                    "No stylesheets were supplied");
        }

        // create a new top-level stylesheet that imports all the others

        StringBuilder sb = new StringBuilder(250);
        sb.append("<xsl:stylesheet version='1.0' ");
        sb.append(" xmlns:xsl='" + NamespaceConstant.XSLT + "'>");
        for (Source source : sources) {
            sb.append("<xsl:import href='").append(source.getSystemId()).append("'/>");
        }
        sb.append("</xsl:stylesheet>");
        InputSource composite = new InputSource();
        composite.setSystemId(baseURI);
        composite.setCharacterStream(new StringReader(sb.toString()));
        return new SAXSource(config.getSourceParser(), composite);
    }

    public void setImporter(StylesheetModule importer) {
        this.importer = importer;
    }

    public StylesheetModule getImporter() {
        return importer;
    }

    /*@NotNull*/
    public StylesheetPackage getPrincipalStylesheetModule() {
        return importer.getPrincipalStylesheetModule();
    }

    public StyleElement getRootElement() {
        return rootElement;
    }

    public XSLModuleRoot getStylesheetElement() {
        return (XSLModuleRoot) rootElement;
    }

    public Configuration getConfiguration() {
        return rootElement.getConfiguration();
    }

    public int getPrecedence() {
        return wasIncluded ? importer.getPrecedence() : precedence;
    }

    /**
     * Indicate that this stylesheet was included (by its "importer") using an xsl:include
     * statement as distinct from xsl:import
     */

    public void setWasIncluded() {
        wasIncluded = true;
    }

    /**
     * Set the minimum import precedence of this module, that is, the lowest import precedence of the modules
     * that it imports. This information is used to decide which template rules are eligible for consideration
     * by xsl:apply-imports
     *
     * @param min the minimum import precedence
     */

    public void setMinImportPrecedence(int min) {
        this.minImportPrecedence = min;
    }

    /**
     * Get the minimum import precedence of this module, that is, the lowest import precedence of the modules
     * that it imports. This information is used to decide which template rules are eligible for consideration
     * by xsl:apply-imports
     *
     * @return the minimum import precedence
     */

    public int getMinImportPrecedence() {
        return this.minImportPrecedence;
    }

    /**
     * Process xsl:include and xsl:import elements.
     *
     * @throws XPathException if the included/imported module is invalid
     */

    public void spliceIncludes() throws XPathException {

        boolean foundNonImport = false;
        if (topLevel == null || topLevel.size() == 0) {
            topLevel = new ArrayList<ComponentDeclaration>(50);
        }
        minImportPrecedence = precedence;
        StyleElement previousElement = rootElement;

        AxisIterator kids = getStylesheetElement().iterateAxis(AxisInfo.CHILD);

        NodeInfo child;
        while ((child = kids.next()) != null) {
            if (child.getNodeKind() == Type.TEXT) {
                // in an embedded stylesheet, white space nodes may still be there
                if (!Whitespace.isWhite(child.getStringValueCS())) {
                    previousElement.compileError(
                            "No character data is allowed between top-level elements", "XTSE0120");
                }

            } else if (child instanceof DataElement) {
                foundNonImport = true;
            } else {
                previousElement = (StyleElement) child;
                if (child instanceof XSLGeneralIncorporate) {
                    XSLGeneralIncorporate xslinc = (XSLGeneralIncorporate) child;
                    xslinc.processAttributes();

                    if (xslinc.isImport()) {
                        if (foundNonImport && !rootElement.isXslt30Processor()) {
                            xslinc.compileError("xsl:import elements must come first", "XTSE0200");
                        }
                    } else {
                        foundNonImport = true;
                    }

                    // get the included stylesheet. This follows the URL, builds a tree, and splices
                    // in any indirectly-included stylesheets.

                    xslinc.validateInstruction();
                    int errors = ((XSLGeneralIncorporate) child).getCompilation().getErrorCount();
                    StylesheetModule inc =
                            xslinc.getIncludedStylesheet(this, precedence);
                    if (inc == null) {
                        return;  // error has been reported
                    }
                    errors = ((XSLGeneralIncorporate) child).getCompilation().getErrorCount() - errors;
                    if (errors > 0) {
                        xslinc.compileError("Reported " + errors + (errors == 1 ? " error" : " errors") +
                                " in " + (xslinc.isImport() ? "imported" : "included") +
                                " stylesheet module", "XTSE0165");
                    }

                    // after processing the imported stylesheet and any others it brought in,
                    // adjust the import precedence of this stylesheet if necessary

                    if (xslinc.isImport()) {
                        precedence = inc.getPrecedence() + 1;
                    } else {
                        precedence = inc.getPrecedence();
                        inc.setMinImportPrecedence(minImportPrecedence);
                        inc.setWasIncluded();
                    }

                    // copy the top-level elements of the included stylesheet into the top level of this
                    // stylesheet. Normally we add these elements at the end, in order, but if the precedence
                    // of an element is less than the precedence of the previous element, we promote it.
                    // This implements the requirement in the spec that when xsl:include is used to
                    // include a stylesheet, any xsl:import elements in the included document are moved
                    // up in the including document to after any xsl:import elements in the including
                    // document.

                    List<ComponentDeclaration> incchildren = inc.topLevel;
                    for (ComponentDeclaration decl : incchildren) {
                        int last = topLevel.size() - 1;
                        if (last < 0 || decl.getPrecedence() >= topLevel.get(last).getPrecedence()) {
                            topLevel.add(decl);
                        } else {
                            while (last >= 0 && decl.getPrecedence() < topLevel.get(last).getPrecedence()) {
                                last--;
                            }
                            topLevel.add(last + 1, decl);
                        }
                    }
                } else {
                    foundNonImport = true;
                    ComponentDeclaration decl = new ComponentDeclaration(this, (StyleElement) child);
                    topLevel.add(decl);
                }
            }
        }
    }

    /**
     * Get the value of the input-type-annotations attribute, for this module combined with that
     * of all included/imported modules. The value is an or-ed combination of the two bits
     * {@link XSLStylesheet#ANNOTATION_STRIP} and {@link XSLStylesheet#ANNOTATION_PRESERVE}
     *
     * @return the value of the input-type-annotations attribute, for this module combined with that
     *         of all included/imported modules
     */

    public int getInputTypeAnnotations() {
        return inputTypeAnnotations;
    }

    /**
     * Set the value of the input-type-annotations attribute, for this module combined with that
     * of all included/imported modules. The value is an or-ed combination of the two bits
     * {@link XSLModuleRoot#ANNOTATION_STRIP} and {@link XSLModuleRoot#ANNOTATION_PRESERVE}
     *
     * @param annotations the value of the input-type-annotations attribute, for this module combined with that
     *                    of all included/imported modules.
     * @throws XPathException if the values of the attribute in different modules are inconsistent
     */

    public void setInputTypeAnnotations(int annotations) throws XPathException {
        inputTypeAnnotations |= annotations;
        if (inputTypeAnnotations == (XSLModuleRoot.ANNOTATION_STRIP | XSLModuleRoot.ANNOTATION_PRESERVE)) {
            getPrincipalStylesheetModule().compileError(
                    "One stylesheet module specifies input-type-annotations='strip', " +
                            "another specifies input-type-annotations='preserve'", "XTSE0265");
        }
    }


}
