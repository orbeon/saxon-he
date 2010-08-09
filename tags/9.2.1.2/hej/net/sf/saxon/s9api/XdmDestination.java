package net.sf.saxon.s9api;

import net.sf.saxon.Configuration;
import net.sf.saxon.event.Builder;
import net.sf.saxon.event.Receiver;
import net.sf.saxon.om.TreeModel;

import java.net.URI;

  /**
    * An <code>XdmDestination</code> is a {@link Destination} in which an {@link XdmNode}
    * is constructed to hold the output of a query or transformation:
    * that is, a tree using Saxon's implementation of the XDM data model
    *
    * <p>No data needs to be supplied to the <code>XdmDestination</code> object. The query or transformation
    * populates an <code>XdmNode</code>, which may then be retrieved using the <code>getXdmNode</code>
    * method.</p>
    *
    * <p>An <code>XdmDestination</code> is designed to hold a single document.
    * It should therefore not be used as the destination of a query that produces multiple
    * documents. If multiple documents are sent to an <code>XdmDestination</code>,
    * the <code>getXdmNode</code> method will return the last one.</p>
    *
    * <p>An XdmDestination can be reused to hold the results of a second query or transformation only
    * if the {@link #reset} method is first called to reset its state.</p>
    */

public class XdmDestination implements Destination {

    TreeModel treeModel = null;
    URI baseURI;
    Builder builder;

    public XdmDestination() {
        //builder = new TinyBuilder();
    }

    /**
     * Set the base URI for the document node that will be created when the XdmDestination is written to.
     * This method must be called before writing to the destination; it has no effect on an XdmNode that
     * has already been constructed.
     * @param baseURI the base URI for the node that will be constructed when the XdmDestination is written to.
     * This must be an absolute URI
     * @throws IllegalArgumentException if the baseURI supplied is not an absolute URI
     * @since 9.1
     */

     public void setBaseURI(URI baseURI) {
        if (!baseURI.isAbsolute()) {
            throw new IllegalArgumentException("Supplied base URI must be absolute");
        }
        //builder.setBaseURI(baseURI.toString());
        this.baseURI = baseURI;
    }

    /**
     * Get the base URI that will be used for the document node when the XdmDestination is written to.
     * @return the base URI that will be used for the node that is constructed when the XdmDestination is written to.
     * @since 9.1
     */

     public URI getBaseURI() {
        return baseURI;
//        try {
//            return new URI(builder.getBaseURI());
//        } catch (URISyntaxException err) {
//            throw new IllegalStateException(err.getMessage());
//        }
    }

    /**
     * Set the tree model to be used for documents constructed using this XdmDestination.
     * By default, the TinyTree is used.
     *
     * @param model typically one of the constants {@link net.sf.saxon.om.TreeModel#TINY_TREE},
     * {@link TreeModel#TINY_TREE_CONDENSED}, or {@link TreeModel#LINKED_TREE}. However, in principle
     * a user-defined tree model can be used.
     * @since 9.2
     */

    public void setTreeModel(TreeModel model) {
        this.treeModel = model;
    }

    /**
     * Get the tree model to be used for documents constructed using this XdmDestination.
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
     * Return a Receiver. Saxon calls this method to obtain a Receiver, to which it then sends
     * a sequence of events representing the content of an XML document.
     *
     * @param config The Saxon configuration. This is supplied so that the destination can
     *               use information from the configuration (for example, a reference to the name pool)
     *               to construct or configure the returned Receiver.
     * @return the Receiver to which events are to be sent.
     * @throws net.sf.saxon.s9api.SaxonApiException
     *          if the Receiver cannot be created
     */

    public Receiver getReceiver(Configuration config) throws SaxonApiException {
        TreeModel model = treeModel;
        if (model == null) {
            model = TreeModel.getTreeModel(config.getTreeModel());
        }
        builder = model.makeBuilder();
        if (baseURI != null) {
            builder.setBaseURI(baseURI.toString());
        }
        builder.setPipelineConfiguration(config.makePipelineConfiguration());
        return builder;
    }

    /**
     * Return the node at the root of the tree, after it has been constructed.
     *
     * <p>This method should not be called while the tree is under construction.</p>
     *
     * @return the root node of the tree (normally a document node). Returns null if the
     * construction of the tree has not yet started.
     * @throws IllegalStateException not yet started, or if tree construction
     * has started but is not complete.
     */

    public XdmNode getXdmNode() {
        if (builder == null) {
            throw new IllegalStateException("The document has not yet been built");
        }
        return (XdmNode)XdmValue.wrap(builder.getCurrentRoot());
    }

    /**
     * Allow the <code>XdmDestination</code> to be reused, without resetting other properties
     * of the destination.
     */

    public void reset() {
        builder = null;
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

