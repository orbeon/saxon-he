////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
// Copyright (c) 2015 Saxonica Limited.
// This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
// If a copy of the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
// This Source Code Form is "Incompatible With Secondary Licenses", as defined by the Mozilla Public License, v. 2.0.
////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

package net.sf.saxon.dotnet;

import cli.System.Xml.XmlDocument;
import cli.System.Xml.XmlNode;
import cli.System.Xml.XmlNodeType;
import net.sf.saxon.Configuration;
import net.sf.saxon.om.GenericTreeInfo;
import net.sf.saxon.om.NodeInfo;

/**
 * The document node of a tree implemented as a wrapper around a DOM Document.
 */

public class DotNetDocumentWrapper extends GenericTreeInfo {


    /**
     * Wrap a DOM Document or DocumentFragment node
     *
     * @param doc     a DOM Document or DocumentFragment node
     * @param baseURI the base URI of the document
     * @param config  the Saxon configuration
     */

    public DotNetDocumentWrapper(XmlNode doc, String baseURI, Configuration config) {
        //System.err.println("Creating DocumentWrapper for " +node);
        super(config);

        if (doc.get_NodeType().Value != XmlNodeType.Document) {
            throw new IllegalArgumentException("Node must be a DOM Document");
        }
        setRootNode(wrap(doc));
        setSystemId(baseURI);
    }

    /**
     * Create a wrapper for a node in this document
     *
     * @param node the DOM node to be wrapped. This must be a node within the document wrapped by this
     *             DocumentWrapper
     * @return a NodeInfo that wraps the supplied node
     * @throws IllegalArgumentException if the node is not a descendant of the Document node wrapped by
     *                                  this DocumentWrapper
     */

    public DotNetNodeWrapper wrap(XmlNode node) {
        return DotNetNodeWrapper.makeWrapper(node, this);
//        if (node == ((DotNetNodeWrapper)getRootNode()).node) {
//            return (DotNetNodeWrapper)getRootNode();
//        }
//        if (node.get_OwnerDocument() == ((DotNetNodeWrapper) getRootNode()).node) {
//            return DotNetNodeWrapper.makeWrapper(node, this);
//        } else {
//            throw new IllegalArgumentException(
//                    "DocumentWrapper#wrap: supplied node does not belong to the wrapped DOM document");
//        }
    }

    /**
     * Get the element with a given ID, if any
     *
     * @param id        the required ID value
     * @param getParent true if running the element-with-id() function rather than the id()
     *                  function; the difference is that in the case of an element of type xs:ID, the parent of
     *                  the element should be returned, not the element itself.
     * @return a NodeInfo representing the element with the given ID, or null if there
     *         is no such element. This implementation does not necessarily conform to the
     *         rule that if an invalid document contains two elements with the same ID, the one
     *         that comes last should be returned.
     */

    public NodeInfo selectID(String id, boolean getParent) {
        XmlNode node = ((DotNetNodeWrapper) getRootNode()).node;
        if (node instanceof XmlDocument) {
            XmlNode el = ((XmlDocument) node).GetElementById(id);
            if (el == null) {
                return null;
            }
            return wrap(el);
        } else {
            return null;
        }
    }


}

