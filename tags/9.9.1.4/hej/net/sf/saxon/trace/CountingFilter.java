////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
// Copyright (c) 2018 Saxonica Limited.
// This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
// If a copy of the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
// This Source Code Form is "Incompatible With Secondary Licenses", as defined by the Mozilla Public License, v. 2.0.
////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

package net.sf.saxon.trace;

import net.sf.saxon.event.ProxyReceiver;
import net.sf.saxon.event.Receiver;
import net.sf.saxon.event.ReceiverOptions;
import net.sf.saxon.event.SequenceReceiver;
import net.sf.saxon.expr.parser.Location;
import net.sf.saxon.om.Item;
import net.sf.saxon.om.NamespaceBinding;
import net.sf.saxon.om.NamespaceBindingSet;
import net.sf.saxon.om.NodeName;
import net.sf.saxon.trans.XPathException;
import net.sf.saxon.type.SchemaType;
import net.sf.saxon.type.SimpleType;

import java.io.PrintStream;

/**
 * A filter that can be inserted into a Receiver pipeline to count the events that pass through.
 * This class is not normally used in Saxon, but is available for diagnostics when needed. Note
 * that the counters are only maintained if {@link Instrumentation#ACTIVE} is set to true. The counters
 * can be displayed by calling {@link Instrumentation#report()}.
 */
public class CountingFilter extends ProxyReceiver {

    private static int nextid = 0;
    private int id;

    /**
     * Create a TracingFilter and allocate a unique Id.
     *
     * @param nextReceiver the underlying receiver to which the events will be sent
     */

    public CountingFilter(Receiver nextReceiver) {
        super(nextReceiver);
        id = nextid++;
    }

    /**
     * Create a TracingFilter, allocate a unique Id, and supply the destination for diagnostic
     * trace messages
     *
     * @param nextReceiver     the underlying receiver to which the events will be sent
     * @param diagnosticOutput the destination for diagnostic trace messages
     */

    public CountingFilter(Receiver nextReceiver, PrintStream diagnosticOutput) {
        super(nextReceiver);
        id = nextid++;
    }

    /**
     * Get the unique id that was allocated to this TracingFilter
     *
     * @return the unique id (which is included in all diagnostic messages)
     */

    public int getId() {
        return id;
    }

    /**
     * Increment a named counter
     */

    private void count(String counter) {
        Instrumentation.count("Filter " + id + " " + counter);
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

    public void append(Item item, Location locationId, int copyNamespaces) throws XPathException {
        count("append");
        if (nextReceiver instanceof SequenceReceiver) {
            ((SequenceReceiver) nextReceiver).append(item, locationId, copyNamespaces);
        } else {
            super.append(item, locationId, copyNamespaces);
        }
    }

    /**
     * Notify an attribute. Attributes are notified after the startElement event, and before any
     * children. Namespaces and attributes may be intermingled.
     *
     * @param nameCode   The name of the attribute
     * @param typeCode   The type of the attribute
     * @param locationId the location of the node in the source, or of the instruction that created it
     * @param properties Bit significant value. The following bits are defined:
     *                   <dl>
     *                   <dd>DISABLE_ESCAPING</dd>    <dt>Disable escaping for this attribute</dt>
     *                   <dd>NO_SPECIAL_CHARACTERS</dd>      <dt>Attribute value contains no special characters</dt>
     *                   </dl>
     * @throws IllegalStateException: attempt to output an attribute when there is no open element
     *                                start tag
     */

    public void attribute(NodeName nameCode, SimpleType typeCode, CharSequence value, Location locationId, int properties) throws XPathException {
        count("attribute");
        nextReceiver.attribute(nameCode, typeCode, value, locationId, properties);
    }

    /**
     * Character data
     */

    public void characters(CharSequence chars, Location locationId, int properties) throws XPathException {
        count("characters");
        nextReceiver.characters(chars, locationId, properties);
    }

    /**
     * End of document
     */

    public void close() throws XPathException {
        count("close");
        nextReceiver.close();
    }

    /**
     * Output a comment
     */

    public void comment(CharSequence chars, Location locationId, int properties) throws XPathException {
        count("comment");
        nextReceiver.comment(chars, locationId, properties);
    }

    /**
     * Notify the end of a document node
     */

    public void endDocument() throws XPathException {
        count("endDocument");
        nextReceiver.endDocument();
    }

    /**
     * End of element
     */

    public void endElement() throws XPathException {
        count("endElement");
        nextReceiver.endElement();
    }

    /**
     * Notify a namespace. Namespaces are notified <b>after</b> the startElement event, and before
     * any children for the element. The namespaces that are reported are only required
     * to include those that are different from the parent element; however, duplicates may be reported.
     * A namespace must not conflict with any namespaces already used for element or attribute names.
     *
     * @param namespaceBindings the namespace (prefix, uri) pair to be notified
     * @throws IllegalStateException: attempt to output a namespace when there is no open element
     *                                start tag
     */

    public void namespace(NamespaceBindingSet namespaceBindings, int properties) throws XPathException {
        for (NamespaceBinding ns : namespaceBindings) {
            count("namespace");
        }
        nextReceiver.namespace(namespaceBindings, properties);
    }

    /**
     * Start of event stream
     */

    public void open() throws XPathException {
        count("open");
        nextReceiver.open();
    }

    /**
     * Processing Instruction
     */

    public void processingInstruction(String target, CharSequence data, Location locationId, int properties) throws XPathException {
        count("processingInstruction");
        nextReceiver.processingInstruction(target, data, locationId, properties);
    }

    /**
     * Notify the start of the content, that is, the completion of all attributes and namespaces.
     * Note that the initial receiver of output from XSLT instructions will not receive this event,
     * it has to detect it itself. Note that this event is reported for every element even if it has
     * no attributes, no namespaces, and no content.
     */


    public void startContent() throws XPathException {
        count("startContent");
        nextReceiver.startContent();
    }

    /**
     * Start of a document node.
     */

    public void startDocument(int properties) throws XPathException {
        count("startDocument");
        nextReceiver.startDocument(properties);
    }

    /**
     * Notify the start of an element
     *  @param nameCode   integer code identifying the name of the element within the name pool.
     * @param typeCode   integer code identifying the element's type within the name pool.
     * @param location  provides information such as line number and system ID.
     * @param properties properties of the element node
     */

    public void startElement(NodeName nameCode, SchemaType typeCode, Location location, int properties) throws XPathException {
        count("startElement");
        nextReceiver.startElement(nameCode, typeCode, location, properties);
    }
}

