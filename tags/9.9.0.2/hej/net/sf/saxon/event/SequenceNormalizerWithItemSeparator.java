////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
// Copyright (c) 2018 Saxonica Limited.
// This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
// If a copy of the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
// This Source Code Form is "Incompatible With Secondary Licenses", as defined by the Mozilla Public License, v. 2.0.
////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

package net.sf.saxon.event;

import net.sf.saxon.expr.parser.ExplicitLocation;
import net.sf.saxon.expr.parser.Location;
import net.sf.saxon.ma.arrays.ArrayItem;
import net.sf.saxon.om.Item;
import net.sf.saxon.om.NodeName;
import net.sf.saxon.trans.XPathException;
import net.sf.saxon.type.SchemaType;
import net.sf.saxon.value.AtomicValue;

/**
 * Implement the "sequence normalization" logic as defined in the XSLT 3.0/XQuery 3.0
 * serialization spec.
 *
 * <p>This class is used only if an ItemSeparator is specified. In the absence of an ItemSeparator,
 * the insertion of a single space performed by the ComplexContentOutputter serves the purpose.</p>
 */

public class SequenceNormalizerWithItemSeparator extends SequenceNormalizer {

    private String separator;
    private boolean first = true;

    public SequenceNormalizerWithItemSeparator(Receiver next, String separator) {
        super(next);
        this.separator = separator;
    }

    /**
     * Start of event stream
     */
    @Override
    public void open() throws XPathException {
        first = true;
        super.open();
    }

    /**
     * Start of a document node.
     */
    @Override
    public void startDocument(int properties) throws XPathException {
        sep();
        super.startDocument(properties);
    }


    /**
     * Notify the start of an element
     *  @param elemName   integer code identifying the name of the element within the name pool.
     * @param typeCode   integer code identifying the element's type within the name pool.
     * @param location
     * @param properties properties of the element node
     */
    @Override
    public void startElement(NodeName elemName, SchemaType typeCode, Location location, int properties) throws XPathException {
        sep();
        super.startElement(elemName, typeCode, location, properties);
    }

    /**
     * Character data
     */
    @Override
    public void characters(CharSequence chars, Location locationId, int properties) throws XPathException {
        sep();
        super.characters(chars, locationId, properties);
    }

    /**
     * Processing Instruction
     */
    @Override
    public void processingInstruction(String target, CharSequence data, Location locationId, int properties) throws XPathException {
        sep();
        super.processingInstruction(target, data, locationId, properties);
    }

    /**
     * Output a comment
     */
    @Override
    public void comment(CharSequence chars, Location locationId, int properties) throws XPathException {
        sep();
        super.comment(chars, locationId, properties);
    }

    /**
     * Append an arbitrary item (node or atomic value) to the output
     *
     * @param item           the item to be appended
     * @param locationId     the location of the calling instruction, for diagnostics
     * @param copyNamespaces if the item is an element node, this indicates whether its namespaces
     *                       need to be copied. Values are {@link ReceiverOptions#ALL_NAMESPACES},
     *                       {@link ReceiverOptions#LOCAL_NAMESPACES}; the default (0) means
     *                       no namespaces
     */
    @Override
    public void append(Item item, Location locationId, int copyNamespaces) throws XPathException {
        if (item instanceof ArrayItem) {
            flatten((ArrayItem) item, locationId, copyNamespaces);
        } else {
            if (item instanceof AtomicValue) {
                sep();
                nextReceiver.characters(item.getStringValueCS(), locationId, 0);
            } else {
                decompose(item, locationId, copyNamespaces);
            }
        }
    }

    /**
     * End of output. Note that closing this receiver also closes the rest of the
     * pipeline.
     */
    @Override
    public void close() throws XPathException {
        //getNextReceiver().endDocument();
        super.close();
    }

    /**
     * Output the separator, assuming we are at the top level and not at the start
     */

    public void sep() throws XPathException {
        if (level == 0 && !first) {
            super.characters(separator, ExplicitLocation.UNKNOWN_LOCATION, 0);
        } else {
            first = false;
        }
    }
}

