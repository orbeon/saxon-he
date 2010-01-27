package net.sf.saxon.s9api;

import net.sf.saxon.AugmentedSource;
import net.sf.saxon.Configuration;
import net.sf.saxon.event.ParseOptions;
import net.sf.saxon.expr.EarlyEvaluationContext;
import net.sf.saxon.expr.JPConverter;
import net.sf.saxon.om.NodeInfo;
import net.sf.saxon.om.TreeModel;
import net.sf.saxon.om.Validation;
import net.sf.saxon.trans.XPathException;

import javax.xml.transform.Source;
import javax.xml.transform.stream.StreamSource;
import java.io.File;
import java.net.URI;

/**
 * A document builder holds properties controlling how a Saxon document tree should be built, and
 * provides methods to invoke the tree construction.
 * <p/>
 * <p>This class has no public constructor.  Users should construct a DocumentBuilder
 * by calling the factory method {@link net.sf.saxon.s9api.Processor#newDocumentBuilder()}.</p>
 * <p/>
 * <p>All documents used in a single Saxon query, transformation, or validation episode must
 * be built with the same {@link Configuration}. However, there is no requirement that they
 * should use the same <code>DocumentBuilder</code>.</p>
 * <p/>
 * <p>Sharing of a <code>DocumentBuilder</code> across multiple threads is not recommended. However,
 * in the current implementation sharing a <code>DocumentBuilder</code> (once initialized) will only
 * cause problems if a <code>SchemaValidator</code> is used.</p>
 *
 * @since 9.0
 */

public class DocumentBuilder {

    private Configuration config;
    private SchemaValidator schemaValidator;
    private boolean retainPSVI = true;
    private boolean dtdValidation;
    private boolean lineNumbering;
    private TreeModel treeModel = TreeModel.TINY_TREE;
    private WhitespaceStrippingPolicy whitespacePolicy;
    private URI baseURI;

    /**
     * Create a DocumentBuilder. This is a protected constructor. Users should construct a DocumentBuilder
     * by calling the factory method {@link net.sf.saxon.s9api.Processor#newDocumentBuilder()}.
     *
     * @param config the Saxon configuration
     */

    protected DocumentBuilder(Configuration config) {
        this.config = config;
    }

    /**
     * Set the tree model to be used for documents constructed using this DocumentBuilder.
     * By default, the TinyTree is used.
     *
     * @param model typically one of the constants {@link net.sf.saxon.om.TreeModel#TINY_TREE},
     * {@link TreeModel#TINY_TREE_CONDENSED}, or {@link TreeModel#LINKED_TREE}. It can also be
     * an external object model such as {@link net.sf.saxon.option.xom.XOMObjectModel}
     * @since 9.2
     */

    public void setTreeModel(TreeModel model) {
        this.treeModel = model;
    }

    /**
     * Get the tree model to be used for documents constructed using this DocumentBuilder.
     * By default, the TinyTree is used.
     *
     * @return the tree model in use: typically one of the constants {@link net.sf.saxon.om.TreeModel#TINY_TREE},
     * {@link net.sf.saxon.om.TreeModel#TINY_TREE_CONDENSED}, or {@link TreeModel#LINKED_TREE}. However, in principle
     * a user-defined tree model can be used.
     * @since 9.2
     */

    public TreeModel getTreeModel() {
        return treeModel;
    }

    /**
     * Say whether line numbering is to be enabled for documents constructed using this DocumentBuilder.
     * This has the effect that the line number in the original source document is maintained in the constructed
     * tree, for each element node (and only for elements). The line number in question is generally the line number
     * on which the closing ">" of the element start tag appears.
     * <p/>
     * <p>By default, line numbers are not maintained.</p>
     * <p/>
     * <p>Errors relating to document parsing and validation will generally contain line numbers whether or not
     * this option is set, because such errors are detected during document construction.</p>
     * <p/>
     * <p>Line numbering is not available for all kinds of source: for example,
     * it is not available when loading from an existing DOM Document.</p>
     * <p/>
     * <p>The resulting line numbers are accessible to applications using the
     * XPath extension function saxon:line-number() applied to a node, or using the
     * Java method {@link net.sf.saxon.om.NodeInfo#getLineNumber()} </p>
     * <p/>
     * <p>Line numbers are maintained only for element nodes; the line number
     * returned for any other node will be that of the most recent element. For an element node, the
     * line number is generally that of the closing angle bracket at the end of the start tag
     * (this is what a SAX parser notifies)</p>
     *
     * @param option true if line numbers are to be maintained, false otherwise.
     */

    public void setLineNumbering(boolean option) {
        lineNumbering = option;
    }

    /**
     * Ask whether line numbering is enabled for documents loaded using this
     * <code>DocumentBuilder</code>.
     * <p/>
     * <p>By default, line numbering is disabled.</p>
     * <p/>
     * <p>Line numbering is not available for all kinds of source: in particular,
     * it is not available when loading from an existing XmlDocument.</p>
     * <p/>
     * <p>The resulting line numbers are accessible to applications using the
     * extension function saxon:line-number() applied to a node, or using the
     * Java method {@link net.sf.saxon.om.NodeInfo#getLineNumber()}</p>
     * <p/>
     * <p>Line numbers are maintained only for element nodes; the line number
     * returned for any other node will be that of the most recent element. For an element node, the
     * line number is generally that of the closing angle bracket at the end of the start tag
     * (this is what a SAX parser notifies)</p>
     *
     * @return true if line numbering is enabled
     */

    public boolean isLineNumbering() {
        return lineNumbering;
    }

    /**
     * Set the schemaValidator to be used. This determines whether schema validation is applied to an input
     * document and whether type annotations in a supplied document are retained. If no schemaValidator
     * is supplied, then schema validation does not take place.
     * <p/>
     * <p>This option requires the schema-aware version of the Saxon product (Saxon-EE).</p>
     * <p/>
     * <p>Since a <code>SchemaValidator</code> is serially reusable but not thread-safe, using this
     * method is not appropriate when the <code>DocumentBuilder</code> is shared between threads.</p>
     *
     * @param validator the SchemaValidator to be used
     */

    public void setSchemaValidator(SchemaValidator validator) {
        schemaValidator = validator;
    }

    /**
     * Get the SchemaValidator used to validate documents loaded using this
     * <code>DocumentBuilder</code>.
     *
     * @return the SchemaValidator if one has been set; otherwise null.
     */
    public SchemaValidator getSchemaValidator() {
        return schemaValidator;
    }

    /**
     * Set whether the constructed tree should contain information derived from schema
     * validation, specifically whether it should contain type annotations and expanded
     * defaults of missing element and attribute content. If no schema validator is set
     * then this option has no effect. The default value is true.
     * <p><b>Not yet implemented.</b></p>
     * @param retainPSVI if true, the constructed tree will contain type annotations
     * and expanded defaults of missing element and attribute content. If false, the
     * tree that is returned will be the same as if schema validation did not take place
     * (except that if the document is invalid, no tree will be constructed)
     */

    public void setRetainPSVI(boolean retainPSVI) {
        // TODO: not implemented
        this.retainPSVI = retainPSVI;
    }

    /**
     * Ask whether the constructed tree should contain information derived from schema
     * validation, specifically whether it should contain type annotations and expanded
     * defaults of missing element and attribute content. If no schema validator is set
     * then this option has no effect.
     *
     * <p><b>Not yet implemented.</b></p>
     * @return true, if the constructed tree will contain type annotations
     * and expanded defaults of missing element and attribute content. Return false, if the
     * tree that is returned will be the same as if schema validation did not take place
     * (except that if the document is invalid, no tree will be constructed)
     */

    public boolean isRetainPSVI() {
        return retainPSVI;
    }

    /**
     * Set whether DTD validation should be applied to documents loaded using this
     * <code>DocumentBuilder</code>.
     * <p/>
     * <p>By default, no DTD validation takes place.</p>
     *
     * @param option true if DTD validation is to be applied to the document
     */

    public void setDTDValidation(boolean option) {
        dtdValidation = option;
    }

    /**
     * Ask whether DTD validation is to be applied to documents loaded using this <code>DocumentBuilder</code>
     *
     * @return true if DTD validation is to be applied
     */

    public boolean isDTDValidation() {
        return dtdValidation;
    }

    /**
     * Set the whitespace stripping policy applied when loading a document
     * using this <code>DocumentBuilder</code>.
     * <p/>
     * <p>By default, whitespace text nodes appearing in element-only content
     * are stripped, and all other whitespace text nodes are retained.</p>
     *
     * @param policy the policy for stripping whitespace-only text nodes from
     *               source documents
     */

    public void setWhitespaceStrippingPolicy(WhitespaceStrippingPolicy policy) {
        whitespacePolicy = policy;
    }

    /**
     * Get the white whitespace stripping policy applied when loading a document
     * using this <code>DocumentBuilder</code>.
     *
     * @return the policy for stripping whitespace-only text nodes
     */

    public WhitespaceStrippingPolicy getWhitespaceStrippingPolicy() {
        return whitespacePolicy;
    }

    /**
     * Set the base URI of a document loaded using this <code>DocumentBuilder</code>.
     * <p/>
     * <p>This is used for resolving any relative URIs appearing
     * within the document, for example in references to DTDs and external entities.</p>
     * <p/>
     * <p>This information is required when the document is loaded from a source that does not
     * provide an intrinsic URI, notably when loading from a Stream or a DOMSource. The value is
     * ignored when loading from a source that does have an intrinsic base URI.</p>
     *
     * @param uri the base URI of documents loaded using this <code>DocumentBuilder</code>. This
     *            must be an absolute URI.
     * @throws IllegalArgumentException if the baseURI supplied is not an absolute URI
     */

    public void setBaseURI(URI uri) {
        if (!uri.isAbsolute()) {
            throw new IllegalArgumentException("Supplied base URI must be absolute");
        }
        baseURI = uri;
    }

    /**
     * Get the base URI of documents loaded using this DocumentBuilder when no other URI is available.
     *
     * @return the base URI to be used, or null if no value has been set.
     */

    public URI getBaseURI() {
        return baseURI;
    }

    /**
     * Load an XML document, to create a tree representation of the document in memory.
     *
     * @param source A JAXP Source object identifying the source of the document. This can always be
     *   a {@link javax.xml.transform.stream.StreamSource} or a {@link javax.xml.transform.sax.SAXSource}.
     *   Some kinds of Source are consumed by this method, and should only be used once.
     *   <p/>
     *   <p>If a SAXSource is supplied, the XMLReader held within the SAXSource may be modified (by setting
     *   features and properties) to reflect the options selected on this DocumentBuilder.</p>
     *   <p>An instance of {@link javax.xml.transform.dom.DOMSource} is accepted provided that the Saxon support
     *   code for DOM (in saxon9-dom.jar) is on the classpath.</p>
     *   <p/>
     *   <p>If the source is an instance of {@link net.sf.saxon.om.NodeInfo} then the subtree rooted at this node
     *   will be copied (applying schema validation if requested) to create a new tree.</p>
     *   <p/>
     *   <p>Saxon also accepts an instance of {@link javax.xml.transform.stax.StAXSource} or
     *  {@link net.sf.saxon.pull.PullSource}, which can be used to supply a document that is to be parsed
     *  using a StAX parser.</p>
     *   <p>(9.2) This method no longer accepts an instance of {@link net.sf.saxon.AugmentedSource}, because of
     *  confusion over interactions between the properties of the AugmentedSource and the properties
     *  of this DocumentBuilder.</p>
     * @return An <code>XdmNode</code>. This will be
     *         the document node at the root of the tree of the resulting in-memory document.
     * @throws IllegalArgumentException if the kind of source is not recognized
     */

    public XdmNode build(Source source) throws SaxonApiException {
        if (source instanceof AugmentedSource) {
            throw new IllegalArgumentException("AugmentedSource not accepted");
        }
        ParseOptions options = new ParseOptions();
        options.setDTDValidationMode(dtdValidation ? Validation.STRICT : Validation.STRIP);
        if (schemaValidator != null) {
            options.setSchemaValidationMode(schemaValidator.isLax() ? Validation.LAX : Validation.STRICT);
            if (schemaValidator.getDocumentElementName() != null) {
                options.setTopLevelElement(schemaValidator.getDocumentElementName().getStructuredQName());
            }
            if (schemaValidator.getDocumentElementType() != null) {
                options.setTopLevelType(schemaValidator.getDocumentElementType());
            }
        }
        if (treeModel != null) {
            options.setModel(treeModel);
        }
        if (whitespacePolicy != null) {
            options.setStripSpace(whitespacePolicy.ordinal());
        }
        options.setLineNumbering(lineNumbering);
        if (source.getSystemId() == null && baseURI != null) {
            source.setSystemId(baseURI.toString());
        }
        try {
            NodeInfo doc = config.buildDocument(source, options);
            return new XdmNode(doc);
        } catch (XPathException e) {
            throw new SaxonApiException(e);
        }
    }                                                           

    /**
     * Build a document from a supplied XML file
     * @param file the supplied file
     * @return the XdmNode representing the root of the document tree
     * @throws SaxonApiException if any failure occurs retrieving or parsing the document
     */

    public XdmNode build(File file) throws SaxonApiException {
        return build(new StreamSource(file));
    }

     /**
     * Create a node by wrapping a recognized external node from a supported object model.
     *
     * <p>If the supplied object implements the {@link net.sf.saxon.om.NodeInfo} interface then it
     * will be wrapped as an <code>XdmNode</code> without copying and without change. The <code>NodeInfo</code>
     * must have been created using a {@link net.sf.saxon.Configuration} compatible
     * with the one used by this <code>Processor</code> (specifically, one that uses the same
     * {@link net.sf.saxon.om.NamePool})</p>
     *
     * <p>To wrap nodes from other object models, such as DOM, the support module for the external object
     * model must be on the class path and registered with the Saxon configuration. The support modules
     * for DOM, JDOM, DOM4J and XOM are registered automatically if they can be found on the classpath.</p>
     *
     * <p>It is best to avoid calling this method repeatedly to wrap different nodes in the same document.
     * Each such wrapper conceptually creates a new XDM tree instance with its own identity. Although the
     * memory is shared, operations that rely on node identity might not have the expected result. It is
     * best to create a single wrapper for the document node, and then to navigate to the other nodes in the
     * tree using S9API interfaces.</p>
     *
     * @param node the node in the external tree representation. Either an instance of
     * {@link net.sf.saxon.om.NodeInfo}, or an instances of a node in an external object model.
     * Nodes in other object models (such as DOM, JDOM, etc) are recognized only if
     * the support module for the external object model is known to the Configuration.
     * @return the supplied node wrapped as an XdmNode
     * @throws IllegalArgumentException if the type of object supplied is not recognized. This may be because
     * node was created using a different Saxon Processor, or because the required code for the external
     * object model is not on the class path
     */

    public XdmNode wrap(Object node) throws IllegalArgumentException {
         if (node instanceof NodeInfo) {
             NodeInfo nodeInfo = (NodeInfo)node;
             if (nodeInfo.getConfiguration().isCompatible(config)) {
                return new XdmNode((NodeInfo)node);
             } else {
                 throw new IllegalArgumentException("Supplied NodeInfo was created using a different Configuration");
             }
         } else {
             try {
                 JPConverter converter = JPConverter.allocate(node.getClass(), config);
                 NodeInfo nodeInfo = (NodeInfo)converter.convert(node, new EarlyEvaluationContext(config, null));
                 return (XdmNode)XdmItem.wrapItem(nodeInfo);
             } catch (XPathException e) {
                 throw new IllegalArgumentException(e.getMessage());
             }
         }
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
// The Initial Developer of the Original Code is Michael H. Kay.
//
// Contributor(s):
//

