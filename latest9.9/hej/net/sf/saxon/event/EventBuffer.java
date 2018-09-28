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

import java.util.ArrayList;
import java.util.List;

/**
 * An <tt>EventBuffer</tt> is a receiver of events that records the events in memory
 * for subsequent replay. It is used, for example, in the implementation of try/catch,
 * where events cannot be written directly to the final serializer in case an error
 * occurs and is caught.
 * <p>Note that events are retained along with their properties, so the class implements
 * "sticky disable-output-escaping" - text nodes can have selected characters marked
 * with the disable-escaping property.</p>
 * @since 9.9
 */

public class EventBuffer extends SequenceReceiver {

    List<Event> buffer = new ArrayList<>();

    public EventBuffer(PipelineConfiguration pipe){
        super(pipe);
    }

    @Override
    public void startDocument(int properties) throws XPathException {
        buffer.add(new Event.StartDocument(properties));
    }

    @Override
    public void endDocument() throws XPathException {
        buffer.add(new Event.EndDocument());
    }

    @Override
    public void startElement(NodeName elemName, SchemaType typeCode, Location location, int properties) throws XPathException {
        buffer.add(new Event.StartElement(elemName, typeCode, location, properties));
    }

    @Override
    public void namespace(NamespaceBindingSet namespaceBindings, int properties) throws XPathException {
        buffer.add(new Event.Namespace(namespaceBindings, properties));
    }

    @Override
    public void attribute(NodeName attName, SimpleType typeCode, CharSequence value, Location location, int properties) throws XPathException {
        buffer.add(new Event.Attribute(attName, typeCode, location, value, properties));
    }

    @Override
    public void startContent() throws XPathException {
        buffer.add(new Event.StartContent());
    }

    @Override
    public void endElement() throws XPathException {
        buffer.add(new Event.EndElement());
    }

    @Override
    public void characters(CharSequence chars, Location location, int properties) throws XPathException {
        buffer.add(new Event.Text(chars, location, properties));
    }

    @Override
    public void processingInstruction(String name, CharSequence data, Location location, int properties) throws XPathException {
        buffer.add(new Event.ProcessingInstruction(name, data, location, properties));
    }

    @Override
    public void comment(CharSequence content, Location location, int properties) throws XPathException {
        buffer.add(new Event.Comment(content, location, properties));
    }

    @Override
    public void append(Item item, Location location, int properties) throws XPathException {
        buffer.add(new Event.Append(item, location, properties));
    }

    @Override
    public void close() throws XPathException {
        // no action
    }

    /**
     * Replay the captured events to a supplied destination
     * @param out the destination {@code Receiver} to receive the events
     * @throws XPathException if any error occurs
     */

    public void replay(Receiver out) throws XPathException {
        for (Event event : buffer) {
            event.replay(out);
        }
    }
}

