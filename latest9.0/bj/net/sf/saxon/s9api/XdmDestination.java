package net.sf.saxon.s9api;

import net.sf.saxon.event.Receiver;
import net.sf.saxon.Configuration;
import net.sf.saxon.tinytree.TinyBuilder;

/**
    * An <code>XdmDestination</code> is a {@link Destination} in which an {@link XdmNode}
    * is constructed to hold the output of a query or transformation:
    * that is, a tree using Saxon's implementation of the XDM data model
    *
    * <p>No data needs to be supplied to the <code>XdmDestination</code> object. The query or transformation
    * populates an <code>XmlNode</code>, which may then be retrieved using the <code>getXdmNode</code>
    * method.</p>
     *
    * <p>An XdmDestination can be reused to hold the results of a second transformation only
    * if the {@link #reset} method is first called to reset its state.</p>
    */
public class XdmDestination implements Destination {

    TinyBuilder builder;

    public XdmDestination() {
        builder = new TinyBuilder();
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
        return builder;
    }

    /**
     * Return the node at the root of the tree, after it has been constructed.
     *
     * <p>This method should not be called while the tree is under construction.</p>
     *
     * @return the root node of the tree (normally a document node). Returns null if the
     * construction of the tree has not yet started. The result is undefined if tree construction
     * has started but is not complete.
     */

    public XdmNode getXdmNode() {
        return (XdmNode)XdmValue.wrap(builder.getCurrentRoot());
    }

    /**
     * Allow the <code>XdmDestination</code> to be reused
     */

    public void reset() {
        builder = new TinyBuilder();
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

