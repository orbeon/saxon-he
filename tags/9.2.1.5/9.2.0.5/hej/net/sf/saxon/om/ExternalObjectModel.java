package net.sf.saxon.om;

import net.sf.saxon.Configuration;
import net.sf.saxon.event.PipelineConfiguration;
import net.sf.saxon.event.Receiver;
import net.sf.saxon.expr.JPConverter;
import net.sf.saxon.expr.PJConverter;
import net.sf.saxon.trans.XPathException;

import javax.xml.transform.Result;
import javax.xml.transform.Source;
import java.io.Serializable;

/**
 * This interface must be implemented by any third-party object model that can
 * be wrapped with a wrapper that implements the Saxon Object Model (the NodeInfo interface).
 *
 * <p>This interface is designed to enable advanced applications to implement and register
 * new object model implementations that Saxon can then use without change. Although it is intended
 * for external use, it cannot at this stage be considered part of the stable Saxon Public API.
 * In particular, it is likely that the interface will grow by the addition of new methods.</p>
 *
 * <p>For maximum integration, an object may extend {@link TreeModel} as well as implementing
 * this interface. To implement <code>TreeModel</code>, it must supply a Builder; in effect this
 * means that it will be possible to use the external object model for output as well as for
 * input.</p>
 */

public interface ExternalObjectModel extends Serializable {

    /**
     * Get the URI of the external object model as used in the JAXP factory interfaces for obtaining
     * an XPath implementation
     * @return the URI used to identify this object model in the JAXP XPath factory mechanism.
     */

    public String getIdentifyingURI();

    /**
     * Get a converter from XPath values to values in the external object model
     * @param targetClass the required class of the result of the conversion. If this class represents
     * a node or list of nodes in the external object model, the method should return a converter that takes
     * a native node or sequence of nodes as input and returns a node or sequence of nodes in the
     * external object model representation. Otherwise, it should return null.
     * @return a converter, if the targetClass is recognized as belonging to this object model;
     * otherwise null
     */

    public PJConverter getPJConverter(Class targetClass);

    /**
     * Get a converter from values in the external object model to XPath values.
     * @param sourceClass the class (static or dynamic) of values to be converted
     * @return a converter, if the sourceClass is recognized as belonging to this object model;
     * otherwise null
     */

    public JPConverter getJPConverter(Class sourceClass);

    /**
     * Get a converter that converts a sequence of XPath nodes to this model's representation
     * of a node list.
     * <p><i>This method is primarily for the benefit of DOM, which uses its own NodeList
     * class to represent collections of nodes. Most other object models use standard
     * Java collection objects such as java.util.List</i></p>
     * @param node an example of the kind of node used in this model
     * @return if the model does not recognize this node as one of its own, return null. Otherwise
     * return a PJConverter that takes a list of XPath nodes (represented as NodeInfo objects) and
     * returns a collection of nodes in this object model
     */

    public PJConverter getNodeListCreator(Object node);

    /**
     * Test whether this object model recognizes a particular kind of JAXP Result object,
     * and if it does, return a Receiver that builds an instance of this data model from
     * a sequence of events. If the Result is not recognised, return null.
     * @param result a JAXP result object
     * @return a Receiver that writes to that result, if available; or null otherwise
     */

    public Receiver getDocumentBuilder(Result result) throws XPathException;

    /**
     * Test whether this object model recognizes a particular kind of JAXP Source object,
     * and if it does, send the contents of the document to a supplied Receiver, and return true.
     * Otherwise, return false.
     * @param source a JAXP Source object
     * @param receiver the Receiver that is to receive the data from the Source
     * @param pipe configuration information
     * @return true if the data from the Source has been sent to the Receiver, false otherwise
     */

    public boolean sendSource(Source source, Receiver receiver, PipelineConfiguration pipe) throws XPathException;

    /**
     * Wrap or unwrap a node using this object model to return the corresponding Saxon node. If the supplied
     * source does not belong to this object model, return null
     * @param source a JAXP Source object
     * @param config the Saxon configuration
     * @return a NodeInfo corresponding to the Source, if this can be constructed; otherwise null
     */

    public NodeInfo unravel(Source source, Configuration config);

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
// Contributor(s): Gunther Schadow (changes to allow access to public fields; also wrapping
// of extensions and mapping of null to empty sequence).
//
