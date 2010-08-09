package net.sf.saxon.event;

import net.sf.saxon.Configuration;
import net.sf.saxon.om.NamePool;
import net.sf.saxon.om.NodeInfo;
import net.sf.saxon.tinytree.TinyDocumentImpl;
import net.sf.saxon.trans.XPathException;

import javax.xml.transform.Source;
import java.util.Date;

/**
 * The abstract Builder class is responsible for taking a stream of SAX events
 * and constructing a Document tree. There is one concrete subclass for each
 * tree implementation.
 * @author Michael H. Kay
 */

public abstract class Builder implements Receiver {
    /**
     * Constant denoting a request for the default tree model
     */
    public static final int UNSPECIFIED_TREE_MODEL = -1;
    /**
     * Constant denoting the "linked tree" in which each node is represented as an object
     */
    public static final int LINKED_TREE = 0;
    /**
     * Alternative constant denoting the "linked tree" in which each node is represented as an object
     * Retained for backwards compatibility
     */
    public static final int STANDARD_TREE = 0;
    /**
     * Constant denoting the "tiny tree" in which the tree is represented internally using arrays of integers
     */
    public static final int TINY_TREE = 1;
    /**
     * Constant denoting the "tiny tree condensed", a variant of the tiny tree in which text and attribute nodes
     * sharing the same string value use shared storage for the value.
     */
    public static final int TINY_TREE_CONDENSED = 2;

    protected PipelineConfiguration pipe;
    protected Configuration config;
    protected NamePool namePool;
    protected String systemId;
    protected String baseURI;
    protected NodeInfo currentRoot;
    protected boolean lineNumbering = false;

    protected boolean started = false;
    protected boolean timing = false;
    private boolean open = false;

    private long startTime;

    /**
     * Create a Builder and initialise variables
     */

    public Builder() {
    }

    public void setPipelineConfiguration(PipelineConfiguration pipe) {
        //System.err.println("Builder#setPipelineConfiguration pipe = " + pipe);
//        if (pipe == null) {
//            new NullPointerException("pipe not initialized").printStackTrace();
//        }
        this.pipe = pipe;
        config = pipe.getConfiguration();
        namePool = config.getNamePool();
        lineNumbering = (lineNumbering || config.isLineNumbering());
    }

    public PipelineConfiguration getPipelineConfiguration () {
        return pipe;
    }

    /**
     * Get the Configuration
     * @return the Saxon configuration
     */

    public Configuration getConfiguration() {
        return config;
    }

    /**
     * The SystemId is equivalent to the document-uri property defined in the XDM data model.
     * It should be set only in the case of a document that is potentially retrievable via this URI.
     * This means it should not be set in the case of a temporary tree constructed in the course of
     * executing a query or transformation.
     * @param systemId the SystemId, that is, the document-uri.
     */

    public void setSystemId(String systemId) {
        this.systemId = systemId;
    }

    /**
     * The SystemId is equivalent to the document-uri property defined in the XDM data model.
     * It should be set only in the case of a document that is potentially retrievable via this URI.
     * This means the value will be null in the case of a temporary tree constructed in the course of
     * executing a query or transformation.
     * @return the SystemId, that is, the document-uri.
     */

    public String getSystemId() {
        return systemId;
    }

    /**
     * Set the base URI of the document node of the tree being constructed by this builder
     * @param baseURI the base URI
     */

    public void setBaseURI(String baseURI) {
        this.baseURI = baseURI;
    }

    /**
     * Get the base URI of the document node of the tree being constructed by this builder
     * @return the base URI
     */

    public String getBaseURI() {
        return baseURI;
    }



    /////////////////////////////////////////////////////////////////////////
    // Methods setting and getting options for building the tree
    /////////////////////////////////////////////////////////////////////////

    /**
     * Set line numbering on or off
     * @param lineNumbering set to true if line numbers are to be maintained for nodes in the tree being
     * constructed.
     */

    public void setLineNumbering(boolean lineNumbering) {
        this.lineNumbering = lineNumbering;
    }

    /**
     * Set timing option on or off
     * @param on set to true to turn timing on. This causes the builder to display statistical information
     * about the tree that is constructed. It corresponds to the command line -t option
     */

    public void setTiming(boolean on) {
        timing = on;
    }

    /**
     * Get timing option
     * @return true if timing information has been requested
     */

    public boolean isTiming() {
        return timing;
    }

    public void open() throws XPathException {
        if (timing && !open) {
            System.err.println("Building tree for " + getSystemId() + " using " + getClass());
            startTime = (new Date()).getTime();
        }
        open = true;
    }

    public void close() throws XPathException {
        if (timing && open) {
            long endTime = (new Date()).getTime();
            System.err.println("Tree built in " + (endTime - startTime) + " milliseconds");
            if (currentRoot instanceof TinyDocumentImpl) {
                ((TinyDocumentImpl)currentRoot).showSize();
            }
            startTime = endTime;
        }
        open = false;
    }

    /**
     * Ask whether this Receiver (or the downstream pipeline) makes any use of the type annotations
     * supplied on element and attribute events
     * @return true if the Receiver makes any use of this information. If false, the caller
     *         may supply untyped nodes instead of supplying the type annotation
     */

    public boolean usesTypeAnnotations() {
        return true;
    }

    /**
     * Get the current root node. This will normally be a document node, but if the root of the tree
     * is an element node, it can be an element.
     * @return the root of the tree that is currently being built, or that has been most recently built
     * using this builder
     */

    public NodeInfo getCurrentRoot() {
        return currentRoot;
    }

    /**
     * Reset the builder to its initial state. The most important effect of calling this
     * method (implemented in subclasses) is to release any links to the constructed document
     * tree, allowing the memory occupied by the tree to released by the garbage collector even
     * if the Builder is still in memory. This can happen because the Builder is referenced from a
     * parser in the Configuration's parser pool.
     */

    public void reset() {
        pipe = null;
        config = null;
        namePool = null;
        systemId = null;
        baseURI = null;
        currentRoot = null;
        lineNumbering = false;
        started = false;
        timing = false;
        open = false;
    }

    /**
     * Static method to build a document from any kind of Source object. If the source
     * is already in the form of a tree, it is wrapped as required.
     * <p><i>The preferred way to construct a document tree from a Source object is to
     * use the method {@link Configuration#buildDocument}.</i></p>
     * @param source Any javax.xml.transform.Source object
     * @param stripper A stripper object, if whitespace text nodes are to be stripped;
     * otherwise null. <b>Ignored since Saxon 9.2</b>
     * @param config The Configuration object
     * @return the NodeInfo of the start node in the resulting document object.
     * @deprecated since Saxon 9.2: use {@link Configuration#buildDocument}. The method
     * was also changed in 9.2 to ignore the stripper parameter.
     */

    public static NodeInfo build(Source source, Stripper stripper, Configuration config) throws XPathException {
        return config.buildDocument(source);
    }

    /**
     * Static method to build a document from any kind of Source object. If the source
     * is already in the form of a tree, it is wrapped as required.
     * <p><i>The preferred way to construct a document tree from a Source object is to
     * use the method {@link Configuration#buildDocument}.</i></p>
     * @param source Any javax.xml.transform.Source object
     * @param stripper A stripper object, if whitespace text nodes are to be stripped;
     * otherwise null. <b>Ignored since Saxon 9.2</b>
     * @param pipe The PipelineConfiguration object
     * @return the NodeInfo of the start node in the resulting document object.
     * @deprecated since Saxon 9.2: use {@link Configuration#buildDocument}. The method
     * was also changed in 9.2 to ignore the stripper parameter.
     */

    public static NodeInfo build(Source source, Stripper stripper, PipelineConfiguration pipe)
    throws XPathException {
        Configuration config = pipe.getConfiguration();
        return config.buildDocument(source);
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
