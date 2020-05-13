////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
// Copyright (c) 2018-2020 Saxonica Limited
// This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
// If a copy of the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
// This Source Code Form is "Incompatible With Secondary Licenses", as defined by the Mozilla Public License, v. 2.0.
////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

package net.sf.saxon.option.dom4j;

import net.sf.saxon.Configuration;
import net.sf.saxon.event.Builder;
import net.sf.saxon.event.PipelineConfiguration;
import net.sf.saxon.event.Receiver;
import net.sf.saxon.expr.JPConverter;
import net.sf.saxon.expr.PJConverter;
import net.sf.saxon.expr.XPathContext;
import net.sf.saxon.lib.ExternalObjectModel;
import net.sf.saxon.lib.NamespaceConstant;
import net.sf.saxon.om.*;
import net.sf.saxon.pattern.AnyNodeTest;
import net.sf.saxon.trans.XPathException;
import net.sf.saxon.tree.wrapper.VirtualNode;
import net.sf.saxon.type.ItemType;
import org.dom4j.*;

import javax.xml.transform.Result;
import javax.xml.transform.Source;


/**
 * This class is the DOM4J implementation of Saxon's ExternalObjectModel interface; it supports
 * the wrapping of DOM4J documents as instances of the Saxon NodeInfo interface.
 */

public class DOM4JObjectModel extends TreeModel implements ExternalObjectModel {

    private final static DOM4JObjectModel THE_INSTANCE = new DOM4JObjectModel();

    /**
     * Get a singular instance of this class
     *
     * @return a singular instance of this class
     */

    public static DOM4JObjectModel getInstance() {
        return THE_INSTANCE;
    }

    public DOM4JObjectModel() {
    }

    /**
     * Get the name of a characteristic class, which, if it can be loaded, indicates that the supporting libraries
     * for this object model implementation are available on the classpath
     *
     * @return by convention (but not necessarily) the class that implements a document node in the relevant
     * external model
     */
    public String getDocumentClassName() {
        return "org.dom4j.Document";
    }

    /**
     * Get the URI of the external object model as used in the JAXP factory interfaces for obtaining
     * an XPath implementation
     */

    public String getIdentifyingURI() {
        return NamespaceConstant.OBJECT_MODEL_DOM4J;
    }

    public String getName() {
        return "DOM4J";
    }

    public Builder makeBuilder(PipelineConfiguration pipe) {
        return new DOM4JWriter(pipe);
    }


    /*@Nullable*/
    public PJConverter getPJConverter(Class<?> targetClass) {
        if (isRecognizedNodeClass(targetClass)) {
            return new PJConverter() {
                public Object convert(Sequence value, Class<?> targetClass, XPathContext context) {
                    return convertXPathValueToObject(value, targetClass);
                }
            };
        } else {
            return null;
        }
    }

    public JPConverter getJPConverter(Class sourceClass, Configuration config) {
        if (isRecognizedNodeClass(sourceClass)) {
            return new JPConverter() {
                public Sequence convert(Object object, XPathContext context) throws XPathException {
                    return convertObjectToXPathValue((Node) object, context.getConfiguration());
                }

                public ItemType getItemType() {
                    return AnyNodeTest.getInstance();
                }
            };
        } else {
            return null;
        }
    }

    /**
     * Get a converter that converts a sequence of XPath nodes to this model's representation
     * of a node list.
     *
     * @param node an example of the kind of node used in this model
     * @return if the model does not recognize this node as one of its own, return null. Otherwise
     *         return a PJConverter that takes a list of XPath nodes (represented as NodeInfo objects) and
     *         returns a collection of nodes in this object model
     */

    public PJConverter getNodeListCreator(Object node) {
        return null;
    }

    /**
     * Test whether this object model recognizes a given node as one of its own
     *
     * @param object the object in question
     * @return true if the object is a DOM4J node
     */

    private static boolean isRecognizedNode(Object object) {
        return object instanceof Document ||
                object instanceof Element ||
                object instanceof Attribute ||
                object instanceof Text ||
                object instanceof CDATA ||
                object instanceof Comment ||
                object instanceof ProcessingInstruction ||
                object instanceof Namespace;
    }

    /**
     * Test whether this object model recognizes a given class as representing a
     * node in that object model. This method will generally be called at compile time.
     *
     * @param nodeClass A class that possibly represents nodes
     * @return true if the class is used to represent nodes in this object model
     */

    private boolean isRecognizedNodeClass(Class nodeClass) {
        return Document.class.isAssignableFrom(nodeClass) ||
                Element.class.isAssignableFrom(nodeClass) ||
                Attribute.class.isAssignableFrom(nodeClass) ||
                Text.class.isAssignableFrom(nodeClass) ||
                CDATA.class.isAssignableFrom(nodeClass) ||
                Comment.class.isAssignableFrom(nodeClass) ||
                ProcessingInstruction.class.isAssignableFrom(nodeClass) ||
                Namespace.class.isAssignableFrom(nodeClass);
    }

    /**
     * Test whether this object model recognizes a particular kind of JAXP Result object,
     * and if it does, return a Receiver that builds an instance of this data model from
     * a sequence of events. If the Result is not recognised, return null.
     * @return always null
     */

    public Receiver getDocumentBuilder(Result result) {
        return null;
    }

    /**
     * Test whether this object model recognizes a particular kind of JAXP Source object.
     * @return always false
     */

    public boolean sendSource(Source source, Receiver receiver)  {
        return false;
    }

    /**
     * Wrap or unwrap a node using this object model to return the corresponding Saxon node. If the supplied
     * source does not belong to this object model, return null
     */

    public NodeInfo unravel(Source source, Configuration config) {
        return null;
    }

    /**
     * Convert a Java object to an XPath value. If the supplied object is recognized as a representation
     * of a value using this object model, the object model should convert the value to an XPath value
     * and return this as the result. If not, it should return null. If the object is recognized but cannot
     * be converted, an exception should be thrown
     *
     * @param object the node to be converted
     * @param config the Saxon configuration
     * @return the resulting XPath value, or null
     */

    private Sequence convertObjectToXPathValue(Node object, Configuration config) {
        if (isRecognizedNode(object)) {
            if (object instanceof Document) {
                return wrapDocument(object, "", config).getRootNode();
            } else {
                Document root = getDocumentRoot(object);
                TreeInfo docInfo = wrapDocument(root, "", config);
                return wrapNode(docInfo, object);
            }
        } else {
            return null;
        }
    }

    /*
     * Convert an XPath value to an object in this object model. If the supplied value can be converted
     * to an object in this model, of the specified class, then the conversion should be done and the
     * resulting object returned. If the value cannot be converted, the method should return null. Note
     * that the supplied class might be a List, in which case the method should inspect the contents of the
     * Value to see whether they belong to this object model.
     */

    private Object convertXPathValueToObject(Sequence value, Class<?> targetClass) {
        if (value instanceof VirtualNode) {
            Object u = ((VirtualNode) value).getRealNode();
            if (targetClass.isAssignableFrom(u.getClass())) {
                return u;
            }
        }
        return null;
    }

    /**
     * Wrap a document node in the external object model in a document wrapper that implements
     * the Saxon DocumentInfo interface
     *
     * @param node    a node (any node) in the third party document
     * @param baseURI the base URI of the node (supply "" if unknown)
     * @param config  the Saxon configuration (which among other things provides access to the NamePool)
     * @return the wrapper, which must implement DocumentInfo
     */

    public TreeInfo wrapDocument(Object node, String baseURI, Configuration config) {
        Document documentNode = getDocumentRoot(node);
        return new DOM4JDocumentWrapper(documentNode, baseURI, config);
    }

    /**
     * Wrap a node within the external object model in a node wrapper that implements the Saxon
     * VirtualNode interface (which is an extension of NodeInfo)
     *
     * @param document the document wrapper, as a DocumentInfo object
     * @param node     the node to be wrapped. This must be a node within the document wrapped by the
     *                 DocumentInfo provided in the first argument
     * @return the wrapper for the node, as an instance of VirtualNode
     */

    public NodeInfo wrapNode(TreeInfo document, Node node) {
        return ((DOM4JDocumentWrapper) document).wrap(node);
    }

    /**
     * Get the document root
     *
     * @param node the node whose root is required
     * @return the root of the DOM4J tree
     */

    private Document getDocumentRoot(Object node) {
        while (!(node instanceof Document)) {
            if (node instanceof Element) {
                if (((Element) node).isRootElement()) {
                    return ((Element) node).getDocument();
                } else {
                    node = ((Element) node).getParent();
                }
            } else if (node instanceof Text) {
                node = ((Text) node).getParent();
            } else if (node instanceof Comment) {
                node = ((Comment) node).getParent();
            } else if (node instanceof ProcessingInstruction) {
                node = ((ProcessingInstruction) node).getParent();
            } else if (node instanceof Attribute) {
                node = ((Attribute) node).getParent();
            } else if (node instanceof Namespace) {
                throw new UnsupportedOperationException("Cannot find parent of DOM4J namespace node");
            } else {
                throw new IllegalStateException("Unknown DOM4J node type " + node.getClass());
            }
        }
        return (Document) node;
    }

}

