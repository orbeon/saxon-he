////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
// Copyright (c) 2014 Saxonica Limited.
// This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
// If a copy of the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
// This Source Code Form is "Incompatible With Secondary Licenses", as defined by the Mozilla Public License, v. 2.0.
////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

package net.sf.saxon.event;

import net.sf.saxon.expr.Expression;
import net.sf.saxon.expr.XPathContext;
import net.sf.saxon.om.*;
import net.sf.saxon.trans.XPathException;
import net.sf.saxon.type.SchemaType;
import net.sf.saxon.type.SimpleType;

import java.util.ArrayList;
import java.util.List;

/**
 * This receiver is inserted into the output pipeline whenever on-empty is used (XSLT 3.0)
 * on a literal result element, on xsl:element, or on xsl:copy when processing elements.
 * It delays writing the start tag until significant content is encountered (anything except
 * namespaces and empty text nodes), and if no significant content has been encountered when the
 * endElement even occurs, it evaluates the onEmpty expression in place of the original element
 * constructor.
 */
public class OnEmptyHandler extends ProxyReceiver {

    //private boolean processingParent = true;
    private Expression onEmpty;
    private XPathContext context;

    private boolean savedDocumentNode;
    private NodeName savedNodeName;
    private SchemaType savedSchemaType;
    private int savedLocationId;
    private int savedProperties;
    private List<NamespaceBinding> savedNamespaces = new ArrayList<NamespaceBinding>(4);

    public OnEmptyHandler(Receiver next, Expression onEmpty, XPathContext context) {
        super(next);
        this.onEmpty = onEmpty;
        this.context = context;
    }

    private void flush() throws XPathException {
        if (savedNodeName != null) {
            nextReceiver.startElement(savedNodeName, savedSchemaType, savedLocationId, savedProperties);
            for (NamespaceBinding binding : savedNamespaces) {
                nextReceiver.namespace(binding, 0);
            }
            savedNodeName = null;
        } else if (savedDocumentNode) {
            nextReceiver.startDocument(savedProperties);
            savedDocumentNode = false;
        }
    }

    /**
     * Start of a document node.
     */
    @Override
    public void startDocument(int properties) throws XPathException {
        if ((properties & ReceiverOptions.IF_NOT_EMPTY) != 0) {
            savedDocumentNode = true;
            savedProperties = properties;
        } else {
            //flush();
            //super.startDocument(properties);
        }
    }

    @Override
    public void startElement(NodeName elemName, SchemaType typeCode, int locationId, int properties) throws XPathException {
        if ((properties & ReceiverOptions.IF_NOT_EMPTY) != 0) {
            savedNodeName = elemName;
            savedSchemaType = typeCode;
            savedLocationId = locationId;
            savedProperties = properties & ~ReceiverOptions.IF_NOT_EMPTY;
        } else {
            flush();
            super.startElement(elemName, typeCode, locationId, properties);
        }
    }

    @Override
    public void namespace(NamespaceBinding namespaceBinding, int properties) throws XPathException {
        if (savedNodeName != null) {
            savedNamespaces.add(namespaceBinding);
        } else {
            super.namespace(namespaceBinding, properties);
        }
    }

    @Override
    public void attribute(NodeName nameCode, SimpleType typeCode, CharSequence value, int locationId, int properties) throws XPathException {
        flush();
        super.attribute(nameCode, typeCode, value, locationId, properties);
    }

    @Override
    public void characters(CharSequence chars, int locationId, int properties) throws XPathException {
        if (chars.length() > 0) {
            flush();
            super.characters(chars, locationId, properties);
        }
    }

    @Override
    public void processingInstruction(String target, CharSequence data, int locationId, int properties) throws XPathException {
        flush();
        super.processingInstruction(target, data, locationId, properties);
    }

    @Override
    public void comment(CharSequence chars, int locationId, int properties) throws XPathException {
        flush();
        super.comment(chars, locationId, properties);
    }

    @Override
    public void append(Item item, int locationId, int copyNamespaces) throws XPathException {
        flush();
        super.append(item, locationId, copyNamespaces);
    }

    @Override
    public void endElement() throws XPathException {
        if (savedNodeName != null) {
            // implies we have reached the end of the element without encountering any content
            savedNodeName = null;
            Item onEmptyItem = onEmpty.evaluateItem(context);
            if (onEmptyItem instanceof NodeInfo) {
                // Must ensure the value is copied
                ((NodeInfo) onEmptyItem).copy(this, CopyOptions.ALL_NAMESPACES, onEmpty.getLocationId());
            }
        } else {
            super.endElement();
        }
    }

    /**
     * Notify the end of a document node
     */
    @Override
    public void endDocument() throws XPathException {
        if (savedDocumentNode) {
            // implies we have reached the end of the document node without encountering any content
            savedDocumentNode = false;
            Item onEmptyItem = onEmpty.evaluateItem(context);
            if (onEmptyItem instanceof NodeInfo) {
                // Must ensure the value is copied
                ((NodeInfo) onEmptyItem).copy(this, CopyOptions.ALL_NAMESPACES, onEmpty.getLocationId());
            }
        } else {
            //super.endDocument();
        }
    }
}

