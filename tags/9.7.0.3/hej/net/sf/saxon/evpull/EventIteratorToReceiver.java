////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
// Copyright (c) 2015 Saxonica Limited.
// This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
// If a copy of the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
// This Source Code Form is "Incompatible With Secondary Licenses", as defined by the Mozilla Public License, v. 2.0.
////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

package net.sf.saxon.evpull;

import net.sf.saxon.event.ReceiverOptions;
import net.sf.saxon.event.SequenceReceiver;
import net.sf.saxon.expr.parser.ExplicitLocation;
import net.sf.saxon.expr.parser.Location;
import net.sf.saxon.om.*;
import net.sf.saxon.trans.XPathException;
import net.sf.saxon.tree.iter.AxisIterator;
import net.sf.saxon.tree.util.Orphan;
import net.sf.saxon.type.SimpleType;
import net.sf.saxon.type.Type;

import java.util.Iterator;

/**
 * Class to read pull events from an EventIterator and write them to a Receiver
 */
public class EventIteratorToReceiver {

    /**
     * Private constructor: this class holds static methods only
     */

    private EventIteratorToReceiver() {
    }

    /**
     * Read the data obtained from an EventIterator and write the same data to a SequenceReceiver
     *
     * @param in  the input EventIterator
     * @param out the output Receiver
     * @throws XPathException
     */

    public static void copy(EventIterator in, SequenceReceiver out) throws XPathException {
        final Location loc = ExplicitLocation.UNKNOWN_LOCATION;
        in = EventStackIterator.flatten(in);
        int level = 0;
        out.open();
        while (true) {
            PullEvent event = in.next();
            if (event == null) {
                break;
            }
            if (event instanceof Orphan && ((Orphan) event).getNodeKind() == Type.TEXT) {
                out.characters(((Orphan) event).getStringValueCS(), loc, 0);
            } else if (event instanceof NodeInfo && ((NodeInfo)event).getNodeKind() == Type.DOCUMENT && level > 0) {
                AxisIterator kids = ((NodeInfo) event).iterateAxis(AxisInfo.CHILD);
                NodeInfo kid;
                while ((kid = kids.next()) != null) {
                    out.append(kid, loc, 0);
                }
            } else if (event instanceof Item) {
                out.append((Item) event, loc, NodeInfo.ALL_NAMESPACES);
            } else if (event instanceof StartElementEvent) {
                StartElementEvent see = (StartElementEvent) event;
                level++;
                out.startElement(see.getElementName(), see.getTypeCode(), loc, ReceiverOptions.NAMESPACE_OK);
                NamespaceBinding[] localNamespaces = see.getLocalNamespaces();
                for (NamespaceBinding ns : localNamespaces) {
                    if (ns == null) {
                        break;
                    }
                    out.namespace(ns, 0);
                }
                if (see.hasAttributes()) {
                    for (Iterator ai = see.iterateAttributes(); ai.hasNext(); ) {
                        NodeInfo att = (NodeInfo) ai.next();
                        out.attribute(NameOfNode.makeName(att), (SimpleType) att.getSchemaType(), att.getStringValueCS(), loc, 0);
                    }
                }
                out.startContent();
            } else if (event instanceof EndElementEvent) {
                level--;
                out.endElement();
            } else if (event instanceof StartDocumentEvent) {
                if (level == 0) {
                    out.startDocument(0);
                } else {
                    // output a zero-length text node to prevent whitespace being added between atomic values
                    out.characters("", loc, 0);
                }
                level++;
            } else if (event instanceof EndDocumentEvent) {
                level--;
                if (level == 0) {
                    out.endDocument();
                } else {
                    // output a zero-length text node to prevent whitespace being added between atomic values
                    out.characters("", loc, 0);
                }
            } else {
                throw new AssertionError("Unknown event class " + event.getClass());
            }

        }
        out.close();
    }
}

