package net.sf.saxon.event;

import net.sf.saxon.om.CodedName;
import net.sf.saxon.om.NamePool;
import net.sf.saxon.om.NamespaceBinding;
import net.sf.saxon.om.NodeName;
import net.sf.saxon.trans.XPathException;
import net.sf.saxon.type.SchemaType;
import net.sf.saxon.type.SimpleType;

/**
* This class is a filter that passes all Receiver events through unchanged,
* except that it changes namecodes to allow for the source and the destination
* using different NamePools. This is necessary when a stylesheet has been constructed
* as a general document (e.g. as the result of a transformation) and is passed to
* newTemplates() to be compiled as a stylesheet.
*
* @author Michael Kay
*/


public class NamePoolConverter extends ProxyReceiver {

    NamePool oldPool;
    NamePool newPool;

    /**
    * Constructor
     * @param next the next receiver in the pipeline
     * @param oldPool the old namepool
     * @param newPool th new namepool
     */

    public NamePoolConverter(Receiver next, NamePool oldPool, NamePool newPool) {
        super(next);
        this.oldPool = oldPool;
        this.newPool = newPool;
    }

    /**
    * Set the underlying emitter. This call is mandatory before using the Emitter.
    * This version is modified from that of the parent class to avoid setting the namePool
    * of the destination Receiver.
    */

    @Override
    public void setUnderlyingReceiver(/*@NotNull*/ Receiver receiver) {
        nextReceiver = receiver;
    }

    /**
    * Output element start tag
    */

    public void startElement(NodeName nameCode, SchemaType typeCode, int locationId, int properties) throws XPathException {
        int nc = newPool.allocate(nameCode.getPrefix(), nameCode.getURI(), nameCode.getLocalPart());
        nextReceiver.startElement(new CodedName(nc, newPool), typeCode, locationId, properties);
    }

    /**
    * Handle a namespace
    */

    public void namespace(NamespaceBinding namespaceBinding, int properties) throws XPathException {
        nextReceiver.namespace(namespaceBinding, properties);
    }

    /**
    * Handle an attribute
    */

    public void attribute(NodeName nameCode, SimpleType typeCode, CharSequence value, int locationId, int properties)
    throws XPathException {
        int nc = newPool.allocate(nameCode.getPrefix(), nameCode.getURI(), nameCode.getLocalPart());
        nextReceiver.attribute(new CodedName(nc, newPool), typeCode, value, locationId, properties);
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
// The Initial Developer of the Original Code is Saxonica Limited.
// Portions created by ___ are Copyright (C) ___. All rights reserved.
//
// Contributor(s):
//