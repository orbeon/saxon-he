////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
// Copyright (c) 2018 Saxonica Limited.
// This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
// If a copy of the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
// This Source Code Form is "Incompatible With Secondary Licenses", as defined by the Mozilla Public License, v. 2.0.
////////////////////////////////////////////////////////////////////////////////////////////////////////////////////


package net.sf.saxon.event;

import net.sf.saxon.expr.parser.Location;
import net.sf.saxon.om.Item;
import net.sf.saxon.om.NamespaceBindingSet;
import net.sf.saxon.om.NodeName;
import net.sf.saxon.trans.XPathException;
import net.sf.saxon.type.SchemaType;
import net.sf.saxon.type.SimpleType;

/**
 * An <tt>EventMonitor</tt> is a filter that passes all events down the pipeline unchanged,
 * keeping a note of whether any data has passed through the filter. At any stage it is possible
 * to ask whether any data has been written.
 * @since 9.9
 */

public class EventMonitor extends ProxyReceiver {

    boolean written = false;

    public EventMonitor(Receiver next){
        super(next);
    }

    @Override
    public void startElement(NodeName elemName, SchemaType typeCode, Location location, int properties) throws XPathException {
        written = true;
        super.startElement(elemName, typeCode, location, properties);
    }

    @Override
    public void namespace(NamespaceBindingSet namespaceBindings, int properties) throws XPathException {
        written = true;
        super.namespace(namespaceBindings, properties);
    }

    @Override
    public void attribute(NodeName attName, SimpleType typeCode, CharSequence value, Location location, int properties) throws XPathException {
        written = true;
        super.attribute(attName, typeCode, value, location, properties);
    }

    @Override
    public void characters(CharSequence chars, Location location, int properties) throws XPathException {
        written = true;
        super.characters(chars, location, properties);
    }

    @Override
    public void processingInstruction(String name, CharSequence data, Location location, int properties) throws XPathException {
        written = true;
        super.processingInstruction(name, data, location, properties);
    }

    @Override
    public void comment(CharSequence content, Location location, int properties) throws XPathException {
        written = true;
        super.comment(content, location, properties);
    }

    @Override
    public void append(Item item, Location location, int properties) throws XPathException {
        written = true;
        super.append(item, location, properties);
    }

    public boolean hasBeenWrittenTo() {
        return written;
    }
}

