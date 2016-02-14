////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
// Copyright (c) 2015 Saxonica Limited.
// This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
// If a copy of the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
// This Source Code Form is "Incompatible With Secondary Licenses", as defined by the Mozilla Public License, v. 2.0.
////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

package net.sf.saxon.event;

import net.sf.saxon.expr.parser.Location;
import net.sf.saxon.om.Item;
import net.sf.saxon.om.NamespaceBinding;
import net.sf.saxon.om.NodeInfo;
import net.sf.saxon.om.NodeName;
import net.sf.saxon.trans.XPathException;
import net.sf.saxon.type.SchemaType;
import net.sf.saxon.type.SimpleType;
import net.sf.saxon.type.Type;

/**
 * This receiver is inserted into the output pipeline whenever on-empty or on-non-empty is used (XSLT 3.0).
 * It passes all events to the underlying receiver unchanged, but invokes a callback action when the
 * first item is written.
 */
public class SignificantItemDetector extends ProxyReceiver {

    private int level = 0;
    private boolean empty = true;
    private Action trigger;

    public interface Action {
        void doAction() throws XPathException;
    }

    public SignificantItemDetector(Receiver next, Action trigger) {
        super(next);
        this.trigger = trigger;
    }

    private void start() throws XPathException {
        if (level==0 && empty) {
            trigger.doAction();
            empty = false;
        }
    }

    /**
     * Start of a document node.
     */
    @Override
    public void startDocument(int properties) throws XPathException {
        if (level++ != 0) {
            super.startDocument(properties);
        }
    }

    @Override
    public void startElement(NodeName elemName, SchemaType typeCode, Location location, int properties) throws XPathException {
        start();
        level++;
        super.startElement(elemName, typeCode, location, properties);
    }

    @Override
    public void namespace(NamespaceBinding namespaceBinding, int properties) throws XPathException {
        start();
        super.namespace(namespaceBinding, properties);
    }

    @Override
    public void attribute(NodeName nameCode, SimpleType typeCode, CharSequence value, Location locationId, int properties) throws XPathException {
        start();
        super.attribute(nameCode, typeCode, value, locationId, properties);
    }

    @Override
    public void characters(CharSequence chars, Location locationId, int properties) throws XPathException {
        //if (!Whitespace.isWhite(chars)) {
            start();
        //}
        super.characters(chars, locationId, properties);
    }

    @Override
    public void processingInstruction(String target, CharSequence data, Location locationId, int properties) throws XPathException {
        start();
        super.processingInstruction(target, data, locationId, properties);
    }

    @Override
    public void comment(CharSequence chars, Location locationId, int properties) throws XPathException {
        start();
        super.comment(chars, locationId, properties);
    }

    public static boolean isSignificant(Item item) {
        if (item instanceof NodeInfo) {
            NodeInfo node = (NodeInfo) item;
            if (node.getNodeKind() == Type.TEXT && node.getStringValue().isEmpty()) {
                return false;
            }
            if (node.getNodeKind() == Type.DOCUMENT && !node.hasChildNodes()) {
                return false;
            }
        }
        return true;
    }

    @Override
    public void append(Item item, Location locationId, int copyNamespaces) throws XPathException {
        if (isSignificant(item)) {
            start();
        }
        super.append(item, locationId, copyNamespaces);
    }

    /**
     * Notify the end of a document node
     */
    @Override
    public void endDocument() throws XPathException {
        if (--level != 0) {
            super.endDocument();
        }
    }

    /**
     * End of element
     */
    @Override
    public void endElement() throws XPathException {
        level--;
        super.endElement();
    }

    /**
     * Ask if the sequence that has been written so far is considered empty
     * @return true if no significant items have been written (or started)
     */

    public boolean isEmpty() {
        return empty;
    }

}

