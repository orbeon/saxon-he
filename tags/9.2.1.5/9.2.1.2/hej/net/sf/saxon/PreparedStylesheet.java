package net.sf.saxon;

import net.sf.saxon.event.*;
import net.sf.saxon.expr.CollationMap;
import net.sf.saxon.functions.ExecutableFunctionLibrary;
import net.sf.saxon.functions.FunctionLibrary;
import net.sf.saxon.functions.FunctionLibraryList;
import net.sf.saxon.instruct.Executable;
import net.sf.saxon.instruct.UserFunction;
import net.sf.saxon.om.NamePool;
import net.sf.saxon.om.NamespaceConstant;
import net.sf.saxon.om.Validation;
import net.sf.saxon.style.*;
import net.sf.saxon.trace.ExpressionPresenter;
import net.sf.saxon.trans.CompilerInfo;
import net.sf.saxon.trans.DecimalFormatManager;
import net.sf.saxon.trans.RuleManager;
import net.sf.saxon.trans.XPathException;
import net.sf.saxon.tree.DocumentImpl;
import net.sf.saxon.tree.TreeBuilder;
import net.sf.saxon.value.Whitespace;
import org.xml.sax.InputSource;
import org.xml.sax.XMLReader;

import javax.xml.transform.*;
import javax.xml.transform.sax.SAXSource;
import java.io.Serializable;
import java.io.StringReader;
import java.math.BigDecimal;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;

/**
 * This <B>PreparedStylesheet</B> class represents a Stylesheet that has been
 * prepared for execution (or "compiled").
 */

public class PreparedStylesheet implements Templates, Serializable {

    private Executable executable;
    private CompilerInfo compilerInfo;
    private transient Configuration config;
    private NamePool targetNamePool;    // the namepool used when the stylesheet was compiled,
                                        // saved here so it can be used again when the stylesheet is run
    private transient StyleNodeFactory nodeFactory;
    private int errorCount = 0;
    private HashMap<URI, PreparedStylesheet> nextStylesheetCache;
                                        // cache for stylesheets named as "saxon:next-in-chain"

    // definitions of decimal formats
    private DecimalFormatManager decimalFormatManager;

    /**
     * Constructor - deliberately protected
     *
     * @param config The Configuration set up by the TransformerFactory
     * @param info Compilation options
     */

    protected PreparedStylesheet(Configuration config, CompilerInfo info) {
        this.config = config;
        executable = new Executable(config);
        RuleManager rm = new RuleManager();
        rm.setRecoveryPolicy(info.getRecoveryPolicy());
        executable.setRuleManager(rm);
        executable.setHostLanguage(Configuration.XSLT);
        executable.setCollationTable(new CollationMap(config.getCollationMap()));
        executable.setSchemaAware(info.isSchemaAware());
        compilerInfo = info;
        if (compilerInfo.getErrorListener() == null) {
            compilerInfo.setErrorListener(config.getErrorListener());
        }
    }

    /**
     * Factory method to make a PreparedStylesheet
     * @param source the source of this principal stylesheet module
     * @param config the Saxon configuration
     * @param info compile-time options for this stylesheet compilation
     * @return the prepared stylesheet
     */
    
    public static PreparedStylesheet compile(Source source, Configuration config, CompilerInfo info)
    throws TransformerConfigurationException {
        PreparedStylesheet pss = new PreparedStylesheet(config, info);
        pss.prepare(source);
        return pss;
    }

    /**
     * Make a Transformer from this Templates object.
     *
     * @return the new Transformer (always a Controller)
     * @see net.sf.saxon.Controller
     */

    public Transformer newTransformer() {
        Controller c = new Controller(config, executable);
        c.setPreparedStylesheet(this);
        return c;
    }

    /**
     * Set the configuration in which this stylesheet is compiled.
     * Intended for internal use.
     * @param config the configuration to be used.
     */

    public void setConfiguration(Configuration config) {
        this.config = config;
        this.compilerInfo = config.getDefaultXsltCompilerInfo();
    }

    /**
     * Get the configuration in which this stylesheet is compiled
     * @return the configuration in which this stylesheet is compiled
     */

    public Configuration getConfiguration() {
        return config;
    }

    /**
     * Set the name pool
     * @param pool the name pool
     */

    public void setTargetNamePool(NamePool pool) {
        targetNamePool = pool;
    }

	/**
	 * Get the name pool in use. This is the namepool used for names that need to be accessible
	 * at runtime, notably the names used in XPath expressions in the stylesheet.
	 *
	 * @return the name pool in use
	 */

	public NamePool getTargetNamePool() {
        if (targetNamePool==null) {
		    return config.getNamePool();
        } else {
            return targetNamePool;
        }
	}

	/**
	 * Get the StyleNodeFactory in use. The StyleNodeFactory determines which subclass of StyleElement
	 * to use for each element node in the stylesheet tree.
	 *
	 * @return the StyleNodeFactory
	 */

	public StyleNodeFactory getStyleNodeFactory() {
		return nodeFactory;
	}

    /**
     * Set the DecimalFormatManager which handles decimal-format definitions
     *
     * @param dfm the DecimalFormatManager containing the named xsl:decimal-format definitions
     */

    public void setDecimalFormatManager(DecimalFormatManager dfm) {
        decimalFormatManager = dfm;
    }

    /**
     * Get the DecimalFormatManager which handles decimal-format definitions
     *
     * @return the DecimalFormatManager containing the named xsl:decimal-format definitions
     */

    public DecimalFormatManager getDecimalFormatManager() {
        if (decimalFormatManager == null) {
            decimalFormatManager = new DecimalFormatManager();
        }
        return decimalFormatManager;
    }


    /**
     * Prepare a stylesheet from a Source document
     *
     * @param styleSource the source document containing the stylesheet
     * @exception TransformerConfigurationException if compilation of the
     *     stylesheet fails for any reason
     */

    protected void prepare(Source styleSource) throws TransformerConfigurationException {
        nodeFactory = config.getStyleNodeFactory();
        DocumentImpl doc;
        try {
            doc = loadStylesheetModule(styleSource, nodeFactory);
            setStylesheetDocument(doc, nodeFactory);
        } catch (XPathException e) {
            try {
                compilerInfo.getErrorListener().fatalError(e);
            } catch (TransformerException e2) {
                // ignore an exception thrown by the error handler
            }
            if (errorCount==0) {
                errorCount++;
            }
        }

        if (errorCount > 0) {
            throw new TransformerConfigurationException(
                            "Failed to compile stylesheet. " +
                            errorCount +
                            (errorCount==1 ? " error " : " errors ") +
                            "detected.");
        }
    }

    /**
     * Build the tree representation of a stylesheet module
     *
     * @param styleSource the source of the module
     * @param nodeFactory the StyleNodeFactory used for creating
     *     element nodes in the tree
     * @exception XPathException if XML parsing or tree
     *     construction fails
     * @return the root Document node of the tree containing the stylesheet
     *     module
     */
    public DocumentImpl loadStylesheetModule(Source styleSource, StyleNodeFactory nodeFactory)
    throws XPathException {

        TreeBuilder styleBuilder = new TreeBuilder();
        PipelineConfiguration pipe = config.makePipelineConfiguration();
        pipe.setURIResolver(compilerInfo.getURIResolver());
        styleBuilder.setPipelineConfiguration(pipe);
        styleBuilder.setSystemId(styleSource.getSystemId());
        styleBuilder.setNodeFactory(nodeFactory);
        styleBuilder.setLineNumbering(true);

        StartTagBuffer startTagBuffer = new StartTagBuffer();

        UseWhenFilter useWhenFilter = new UseWhenFilter(startTagBuffer);
        useWhenFilter.setUnderlyingReceiver(styleBuilder);

        startTagBuffer.setUnderlyingReceiver(useWhenFilter);

        StylesheetStripper styleStripper = new StylesheetStripper();
        styleStripper.setUnderlyingReceiver(startTagBuffer);

        CommentStripper commentStripper = new CommentStripper();
        commentStripper.setUnderlyingReceiver(styleStripper);

        // build the stylesheet document

        DocumentImpl doc;

        Sender sender = new Sender(pipe);
        AugmentedSource aug = AugmentedSource.makeAugmentedSource(styleSource);
        aug.setSchemaValidationMode(Validation.STRIP);
        aug.setDTDValidationMode(Validation.STRIP);
        aug.setLineNumbering(true);
        aug.setStripSpace(Whitespace.NONE);
        if (aug.getXMLReader() == null && Configuration.getPlatform().isJava()) {
            XMLReader styleParser = config.getStyleParser();
            aug.setXMLReader(styleParser);
            sender.send(aug, commentStripper, new ParseOptions());
            config.reuseStyleParser(styleParser);
        } else {
            sender.send(aug, commentStripper, new ParseOptions());
        }
        doc = (DocumentImpl)styleBuilder.getCurrentRoot();
        styleBuilder.reset();

        if (aug.isPleaseCloseAfterUse()) {
            aug.close();
        }

        return doc;
        // TODO: if the Source is a DocumentInfo built with line numbers, should retain the line numbers
    }



    /**
     * Create a PreparedStylesheet from a supplied DocumentInfo
     * Note: the document must have been built using the StyleNodeFactory
     *
     * @param doc the document containing the stylesheet module
     * @param snFactory the StyleNodeFactory used to build the tree
     * @exception XPathException if the document supplied
     *     is not a stylesheet
     */

    protected void setStylesheetDocument(DocumentImpl doc, StyleNodeFactory snFactory)
    throws XPathException {

        DocumentImpl styleDoc = doc;
		nodeFactory = snFactory;

        // If top-level node is a literal result element, stitch it into a skeleton stylesheet

        StyleElement topnode = (StyleElement)styleDoc.getDocumentElement();
        if (topnode instanceof LiteralResultElement) {
            styleDoc = ((LiteralResultElement)topnode).makeStylesheet(this, snFactory);
        }

        if (!(styleDoc.getDocumentElement() instanceof XSLStylesheet)) {
            throw new XPathException(
                        "Outermost element of stylesheet is not xsl:stylesheet or xsl:transform or literal result element");
        }

        XSLStylesheet top = (XSLStylesheet)styleDoc.getDocumentElement();
        if (compilerInfo.isVersionWarning() && top.getVersion().equals(BigDecimal.valueOf(1))) {
            try {
                TransformerException w = new TransformerException(
                        "Running an XSLT 1.0 stylesheet with an XSLT 2.0 processor");
                w.setLocator(topnode);
                config.getErrorListener().warning(w);
            } catch (TransformerException e) {
                throw XPathException.makeXPathException(e);
            }
        }

        // Preprocess the stylesheet, performing validation and preparing template definitions

        executable.setLocationMap(top.getLocationMap());
        top.setPreparedStylesheet(this);
        try {
            top.preprocess();
        } catch (XPathException e) {
            Throwable e2 = e.getException();
            if (e2 instanceof XPathException) {
                try {
                    compilerInfo.getErrorListener().fatalError((XPathException)e2);
                } catch (TransformerException e3) {
                    // ignore an error thrown by the ErrorListener
                }
            }
            throw e;
        }

        // Compile the stylesheet, retaining the resulting executable

        top.compileStylesheet();
    }

    /**
     * Get the associated executable
     *
     * @return the Executable for this stylesheet
     */

    public Executable getExecutable() {
        return executable;
    }

    /**
     * Determine whether trace hooks are included in the compiled code.
     * @return true if trace hooks are included, false if not.
     * @since 8.9
     */

    public boolean isCompileWithTracing() {
        return compilerInfo.isCompileWithTracing();
    }


    /**
     * Get the properties for xsl:output.  JAXP method. The object returned will
     * be a clone of the internal values, and thus it can be mutated
     * without mutating the Templates object, and then handed in to
     * the process method.
     * <p>In Saxon, the properties object is a new, empty, Properties object that is
     * backed by the live properties to supply default values for missing properties.
     * This means that the property values must be read using the getProperty() method.
     * Calling the get() method on the underlying Hashtable will return null.</p>
     * <p>In Saxon 8.x, this method gets the output properties for the unnamed output
     * format in the stylesheet.</p>
     *
     * @see javax.xml.transform.Transformer#setOutputProperties
     * @return A Properties object reflecting the output properties defined
     *     for the default (unnamed) output format in the stylesheet. It may
     *     be mutated and supplied to the setOutputProperties() method of the
     *     Transformer, without affecting other transformations that use the
     *     same stylesheet.
     */


    public Properties getOutputProperties() {
        Properties details = executable.getDefaultOutputProperties();
        return new Properties(details);
    }

    /**
     * Report a compile time error. This calls the errorListener to output details
     * of the error, and increments an error count.
     *
     * @param err the exception containing details of the error
     * @exception TransformerException if the ErrorListener decides that the
     *     error should be reported
     */

    public void reportError(TransformerException err) throws TransformerException {
        errorCount++;
        if (err instanceof XPathException) {
            if (!((XPathException)err).hasBeenReported()) {
                try {
                    compilerInfo.getErrorListener().fatalError(err);
                    ((XPathException)err).setHasBeenReported(true);
                } catch (Exception err2) {
                    // ignore secondary error
                }
            }
        } else {
            compilerInfo.getErrorListener().fatalError(err);
        }
    }

    /**
     * Get the number of errors reported so far
     *
     * @return the number of errors reported
     */

    public int getErrorCount() {
        return errorCount;
    }

    /**
     * Report a compile time warning. This calls the errorListener to output details
     * of the warning.
     *
     * @param err an exception holding details of the warning condition to be
     *     reported
     */

    public void reportWarning(TransformerException err) {
        try {
            compilerInfo.getErrorListener().warning(err);
        } catch (TransformerException err2) {
            // ignore secondary error
        }
    }

    /**
     * Get a "next in chain" stylesheet. This method is intended for internal use.
     * @param href the relative URI of the next-in-chain stylesheet
     * @param baseURI the baseURI against which this relativeURI is to be resolved
     * @return the cached stylesheet if present in the cache, or null if not
     */

    public PreparedStylesheet getCachedStylesheet(String href, String baseURI) {
        URI abs = null;
        try {
            abs = new URI(baseURI).resolve(href);
        } catch (URISyntaxException err) {
            //
        }
        PreparedStylesheet result = null;
        if (abs != null && nextStylesheetCache != null) {
            result = nextStylesheetCache.get(abs);
        }
        return result;
    }

    /**
     * Save a "next in chain" stylesheet in compiled form, so that it can be reused repeatedly.
     * This method is intended for internal use.
     * @param href the relative URI of the stylesheet
     * @param baseURI the base URI against which the relative URI is resolved
     * @param pss the prepared stylesheet object to be cached
     */

    public void putCachedStylesheet(String href, String baseURI, PreparedStylesheet pss) {
        URI abs = null;
        try {
            abs = new URI(baseURI).resolve(href);
        } catch (URISyntaxException err) {
            //
        }
        if (abs != null) {
            if (nextStylesheetCache == null) {
                nextStylesheetCache = new HashMap<URI, PreparedStylesheet>(4);
            }
            nextStylesheetCache.put(abs, pss);
        }
    }

    /**
     * Get the CompilerInfo containing details of XSLT compilation options
     * @return the CompilerInfo containing compilation options
     * @since 9.2
     */

    public CompilerInfo getCompilerInfo() {
        return compilerInfo;
    }

    /**
     * Produce an XML representation of the compiled and optimized stylesheet
     * @param presenter defines the destination and format of the output
     */

    public void explain(ExpressionPresenter presenter) {
        presenter.startElement("stylesheet");
        getExecutable().getKeyManager().explainKeys(presenter);
        getExecutable().explainGlobalVariables(presenter);
        getExecutable().getRuleManager().explainTemplateRules(presenter);
        getExecutable().explainNamedTemplates(presenter);
        FunctionLibraryList libList = (FunctionLibraryList)getExecutable().getFunctionLibrary();
        List libraryList = libList.getLibraryList();
        presenter.startElement("functions");
        for (int i=0; i<libraryList.size(); i++) {
            FunctionLibrary lib = (FunctionLibrary)libraryList.get(i);
            if (lib instanceof ExecutableFunctionLibrary) {
                for (Iterator f = ((ExecutableFunctionLibrary)lib).iterateFunctions(); f.hasNext();) {
                    UserFunction func = (UserFunction)f.next();
                    presenter.startElement("function");
                    presenter.emitAttribute("name", func.getFunctionName().getDisplayName());
                    presenter.emitAttribute("line", func.getLineNumber()+"");
                    presenter.emitAttribute("module", func.getSystemId());
                    func.getBody().explain(presenter);
                    presenter.endElement();
                }
            }
        }
        presenter.endElement();
        presenter.endElement();
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
     * @param config The Saxon Configuration
     * @param source The XML source document.
     * @param media The media attribute to be matched.  May be null, in which
     *              case the prefered templates will be used (i.e. alternate = no).
     * @param title The value of the title attribute to match.  May be null.
     * @param charset The value of the charset attribute to match.  May be null.
     *
     * @return A Source object suitable for passing to the TransformerFactory.
     *
     * @throws TransformerConfigurationException if any problems occur
     */


    public static Source getAssociatedStylesheet(
            Configuration config, Source source, String media, String title, String charset)
            throws TransformerConfigurationException {
        PIGrabber grabber = new PIGrabber();
        grabber.setFactory(config);
        grabber.setCriteria(media, title, charset);
        grabber.setBaseURI(source.getSystemId());
        grabber.setURIResolver(config.getURIResolver());


        try {
            new Sender(config.makePipelineConfiguration()).send(source, grabber, new ParseOptions());
            // this parse will be aborted when the first start tag is found
        } catch (XPathException err) {
            if (grabber.isTerminated()) {
            	// do nothing
            } else {
                throw new TransformerConfigurationException(
                        "Failed while looking for xml-stylesheet PI", err);
            }
        }

        try {
            Source[] sources = grabber.getAssociatedStylesheets();
            if (sources==null) {
                throw new TransformerConfigurationException(
                    "No matching <?xml-stylesheet?> processing instruction found");
            }
            return compositeStylesheet(config, source.getSystemId(), sources);
        } catch (TransformerException err) {
            if (err instanceof TransformerConfigurationException) {
                throw (TransformerConfigurationException)err;
            } else {
                throw new TransformerConfigurationException(err);
            }
        }
    }

    /**
    * Process a series of stylesheet inputs, treating them in import or cascade
    * order.  This is mainly for support of the getAssociatedStylesheets
    * method, but may be useful for other purposes.
    * @param baseURI the base URI to be used for the synthesized composite stylesheet
    * @param sources An array of Source objects representing individual stylesheets.
    * @return A Source object representing a composite stylesheet.
    */

    private static Source compositeStylesheet(Configuration config, String baseURI, Source[] sources)
    					throws TransformerConfigurationException {

        if (sources.length == 1) {
            return sources[0];
        } else if (sources.length == 0) {
            throw new TransformerConfigurationException(
                            "No stylesheets were supplied");
        }

        // create a new top-level stylesheet that imports all the others

        StringBuffer sb = new StringBuffer(250);
        sb.append("<xsl:stylesheet version='1.0' ");
        sb.append(" xmlns:xsl='" + NamespaceConstant.XSLT + "'>");
        for (int i=0; i<sources.length; i++) {
            sb.append("<xsl:import href='").append(sources[i].getSystemId()).append("'/>");
        }
        sb.append("</xsl:stylesheet>");
        InputSource composite = new InputSource();
        composite.setSystemId(baseURI);
        composite.setCharacterStream(new StringReader(sb.toString()));
        return new SAXSource(config.getSourceParser(), composite);
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
// Contributor(s): none.
//
