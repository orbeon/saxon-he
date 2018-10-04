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

import java.util.HashMap;
import java.util.Map;
import java.util.Stack;

/**
 * A <tt>RegularSequenceChecker</tt> is a filter that can be inserted into a Receiver pipeline
 * to check that the sequence of events passed in is a <b>regular event sequence</b>. Many
 * (though not all) implementations of {@link Receiver} require the sequence of events to
 * be regular according to this definition.
 * <p>A sequence of {@code Receiver} events is <b>regular</b> if the following conditions
 * are satisfied:</p>
 * <ol>
 *     <li>Calls to {@link #startElement(NodeName, SchemaType, Location, int)}, {@link #endElement()},
 *     {@link #startDocument(int)}, and {@link #endDocument()} are properly paired and nested.</li>
 *     <li>Events must only occur in a state where they are permitted; the states and transitions
 *     between states are defined by the table below. The initial state is <b>initial</b>,
 *     and the final state must be <b>final</b>.</li>
 * </ol>
 * <table>
 *     <thead>
 *         <tr><th>State</th><th>Events</th><th>Next State</th></tr>
 *     </thead>
 *     <tbody>
 *         <tr><td>initial</td><td>{@link #open()}</td><td>open</td></tr>
 *         <tr><td>open</td><td>{@link #open()}</td><td>open</td></tr>
 *         <tr><td>open</td><td>{@link #append(Item, Location, int)}, {@link #append(Item)},
 *         {@link #attribute(NodeName, SimpleType, CharSequence, Location, int)}, {@link #namespace(NamespaceBindingSet, int)},
 *         {@link #characters(CharSequence, Location, int)}, {@link #comment(CharSequence, Location, int)},
 *         {@link #processingInstruction(String, CharSequence, Location, int)}</td><td>open</td></tr>
 *         <tr><td>open</td><td>{@link #startDocument(int)}</td><td>content</td></tr>
 *         <tr><td>open</td><td>{@link #startElement(NodeName, SchemaType, Location, int)}</td><td>startTag</td></tr>
 *         <tr><td>startTag</td><td>{@link #attribute(NodeName, SimpleType, CharSequence, Location, int)}, {@link #namespace(NamespaceBindingSet, int)}</td><td>startTag</td></tr>
 *         <tr><td>startTag</td><td>{@link #startContent()}</td><td>content</td></tr>
 *         <tr><td>content</td><td>{@link #characters(CharSequence, Location, int)}, {@link #comment(CharSequence, Location, int)},
 *         {@link #processingInstruction(String, CharSequence, Location, int)}</td><td>content</td></tr>
 *         <tr><td>content</td><td>{@link #startElement(NodeName, SchemaType, Location, int)}</td><td>startTag</td></tr>
 *         <tr><td>content</td><td>{@link #endDocument()}, {@link #endElement()}</td><td>if the stack is empty, then content, otherwise open</td></tr>
 *         <tr><td>(any)</td><td>close</td><td>final</td></tr>
 *         <tr><td>final</td><td>close</td><td>final</td></tr>
 *     </tbody>
 * </table>
 * <p>This class is not normally used in production within Saxon, but is available for diagnostics when needed.</p>
 * <p>Some implementations of {@code Receiver} accept sequences of events that are not regular; indeed, some
 * implementations are explicitly designed to produce a regular sequence from an irregular sequence.
 * Examples of such irregularities are <b>append</b> or <b>startDocument</b> events appearing within
 * element content, or <b>attribute</b> events being followed by <b>text</b> events with no intervening
 * <b>startContent</b>.</p>
 * <p>The rules for a <b>regular sequence</b> imply that the top level events (any events not surrounded
 * by startElement-endElement or startDocument-endDocument) can represent any sequence of items, including
 * for example multiple document nodes, free-standing attribute and namespace nodes, maps, arrays, and functions;
 * but within a startElement-endElement or startDocument-endDocument pair, the events represent content
 * that has been normalized and validated according to the XSLT rules for constructing complex content, or
 * the XQuery equivalent: for example, attributes and namespaces must appear before child nodes, a
 * {@link #startContent()} event must occur after the attributes and namespaces, adjacent text nodes should
 * have been merged, zero-length text nodes should have been eliminated, all namespaces should be explicitly
 * declared, document nodes should be replaced by their children.</p>
 * <p>Element nodes in "composed form" (that is, existing as a tree in memory) may be passed through
 * the {@link #append(Item)} method at the top level, but within a startElement-endElement or
 * startDocument-endDocument pair, elements must be represented in "decomposed form" as a sequence
 * of events.</p>
 * <p>A call to {@link #close} is permitted in any state, but it should only be called in <code>Open</code>
 * state except on an error path; on error paths calling {@link #close} is recommended to ensure that
 * resources are released.</p>
 */
public class RegularSequenceChecker extends ProxyReceiver {

    private Stack<Integer> stack = new Stack<>();
    public enum State {Initial, Open, StartTag, Content, Final}
    private State state = State.Initial;
    private static Map<State, Map<String, State>> machine = new HashMap<>();

    static void edge(State from, String event, State to) {
        Map<String, State> edges = machine.computeIfAbsent(from, s -> new HashMap<>());
        edges.put(event, to);
    }

    static {
        edge(State.Initial, "open", State.Open);
        //edge(State.Open, "open", State.Open); // TODO: we're only allowing this because it happens...
        edge(State.Open, "append", State.Open);
        edge(State.Open, "text", State.Open);
        edge(State.Open, "comment", State.Open);
        edge(State.Open, "pi", State.Open);
        edge(State.Open, "attribute", State.Open);
        edge(State.Open, "namespace", State.Open);
        edge(State.Open, "startDocument", State.Content);
        edge(State.Open, "startElement", State.StartTag);
        edge(State.StartTag, "attribute", State.StartTag);
        edge(State.StartTag, "namespace", State.StartTag);
        edge(State.StartTag, "startContent", State.Content);
        edge(State.Content, "text", State.Content);
        edge(State.Content, "comment", State.Content);
        edge(State.Content, "pi", State.Content);
        edge(State.Content, "startElement", State.StartTag);
        edge(State.Content, "endElement", State.Content); // or Open if the stack is empty
        edge(State.Content, "endDocument", State.Open);
        edge(State.Open, "close", State.Final);
        edge(State.Final, "close", State.Final);  // TODO: another concession to poor practice...
    }

    private void transition(String event) {
        State newState = machine.get(state).get(event);
        if (newState == null) {
            //assert false;
            throw new IllegalStateException("Event " + event + " is not permitted in state " + state);
        } else {
            state = newState;
        }
    }


    /**
     * Create a RegularSequenceChecker and allocate a unique Id.
     *
     * @param nextReceiver the underlying receiver to which the events will be sent (without change)
     */

    public RegularSequenceChecker(Receiver nextReceiver) {
        super(nextReceiver);
        state = State.Initial;
    }

    /**
     * Append an arbitrary item (node or atomic value) to the output. In a regular sequence, append
     * events occur only at the top level, that is, when the document / element stack is empty.
     *
     * @param item           the item to be appended
     * @param locationId     the location of the calling instruction, for diagnostics
     * @param copyNamespaces if the item is an element node, this indicates whether its namespaces
     *                       need to be copied. Values are {@link ReceiverOptions#ALL_NAMESPACES},
     *                       {@link ReceiverOptions#LOCAL_NAMESPACES}; the default (0) means
     *                       no namespaces
     */

    public void append(Item item, Location locationId, int copyNamespaces) throws XPathException {
        transition("append");
        nextReceiver.append(item, locationId, copyNamespaces);
    }

    /**
     * Notify an attribute. An attribute is either free-standing attribute, or a parented attribute. A free-standing
     * attribute is one that occurs when the startElement / startDocument stack is empty; a parented attribute
     * occurs between a startElement event and a startContent event.
     * <p>
     * All attributes must satisfy the following constraints:
     * <ol>
     *     <li>The namespace prefix and URI must either both be present (non-zero-length) or both absent</li>
     *     <li>The prefix "xml" and the URI "http://www.w3.org/XML/1998/namespace"
     *     are allowed only in combination.</li>
     *     <li>The namespace URI "http://www.w3.org/2000/xmlns/" is not allowed.</li>
     *     <li>The namespace prefix "xmlns" is not allowed.</li>
     *     <li>The local name "xmlns" is not allowed in the absence of a namespace prefix and URI.</li>
     * </ol>
     * <p>
     * For a parented attribute, the following additional constraints apply to the set of attributes between
     * a startElement event and the next startContent event:
     * <ol>
     *     <li>No two attributes may have the same (local-name, namespace URI) combination.</li>
     *     <li>No namespace prefix may be used in conjunction with more than one namespace URI.</li>
     *     <li>Every (namespace prefix, namespace URI) combination must correspond to an in-scope namespace:
     *     that is, unless the (prefix, URI) pair is ("", "") or ("xml", "http://www.w3.org/XML/1998/namespace"),
     *     it must be the subject of a {@link #namespace(NamespaceBindingSet, int)} event applicable to this
     *     element or to some parent element, that has not been cancelled by a namespace undeclaration on
     *     an inner element. If the namespace event appears on the same element as the attribute event then they
     *     may arrive in either order.</li>
     * </ol>
     * <p>
     * These constraints are not currently enforced by this class.
     * </p>
     * 
     *
     * @param nameCode   The name of the attribute
     * @param typeCode   The type of the attribute
     * @param locationId  The location of the attribute
     * @param properties Bit significant value. The following bits are defined:
     *                   <dl>
     *                   <dd>DISABLE_ESCAPING</dd>    <dt>Disable escaping for this attribute</dt>
     *                   <dd>NO_SPECIAL_CHARACTERS</dd>      <dt>Attribute value contains no special characters</dt>
     *                   </dl>
     * @throws IllegalStateException: attempt to output an attribute when there is no open element
     *                                start tag
     */

    public void attribute(NodeName nameCode, SimpleType typeCode, CharSequence value, Location locationId, int properties) throws XPathException {
        transition("attribute");
        nextReceiver.attribute(nameCode, typeCode, value, locationId, properties);
    }

    /**
     * Character data (corresponding to a text node). For character data within content (that is, events occurring
     * when the startDocument / startElement stack is non-empty), character data events will never be consecutive
     * and will never be zero-length.
     */

    public void characters(CharSequence chars, Location locationId, int properties) throws XPathException {
        transition("text");
        if (chars.length() == 0 && !stack.isEmpty()) {
            throw new IllegalStateException("Zero-length text nodes not allowed within document/element content");
        }
        nextReceiver.characters(chars, locationId, properties);
    }

    /**
     * End of sequence
     */

    public void close() throws XPathException {
        if (state != State.Final) {
            nextReceiver.close();
            state = State.Final;
        }
    }

    /**
     * Output a comment
     */

    public void comment(CharSequence chars, Location locationId, int properties) throws XPathException {
        transition("comment");
        nextReceiver.comment(chars, locationId, properties);
    }

    /**
     * Notify the end of a document node
     */

    public void endDocument() throws XPathException {
        transition("endDocument");
        if (stack.isEmpty() || stack.pop() != 0) {
            throw new IllegalStateException("Unmatched endDocument() call");
        }
        nextReceiver.endDocument();
    }

    /**
     * End of element
     */

    public void endElement() throws XPathException {
        transition("endElement");
        if (stack.isEmpty() || stack.pop() != 1) {
            throw new IllegalStateException("Unmatched endElement() call");
        }
        if (stack.isEmpty()) {
            state = State.Open;
        }
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
        transition("namespace");
        nextReceiver.namespace(namespaceBindings, properties);
    }

    /**
     * Start of event stream
     */

    public void open() throws XPathException {
        transition("open");
        nextReceiver.open();
    }

    /**
     * Processing Instruction
     */

    public void processingInstruction(String target, CharSequence data, Location locationId, int properties) throws XPathException {
        transition("pi");
        nextReceiver.processingInstruction(target, data, locationId, properties);
    }

    /**
     * Notify the start of the content, that is, the completion of all attributes and namespaces.
     * Note that the initial receiver of output from XSLT instructions will not receive this event,
     * it has to detect it itself. Note that this event is reported for every element even if it has
     * no attributes, no namespaces, and no content.
     */


    public void startContent() throws XPathException {
        transition("startContent");
        nextReceiver.startContent();
    }

    /**
     * Start of a document node.
     */

    public void startDocument(int properties) throws XPathException {
        transition("startDocument");
        stack.push(0);
        nextReceiver.startDocument(properties);
    }

    /**
     * Notify the start of an element
     * @param nameCode   integer code identifying the name of the element within the name pool.
     * @param typeCode   integer code identifying the element's type within the name pool.
     * @param location  provides information such as line number and system ID.
     * @param properties properties of the element node
     */

    public void startElement(NodeName nameCode, SchemaType typeCode, Location location, int properties) throws XPathException {
        transition("startElement");
        stack.push(1);
        nextReceiver.startElement(nameCode, typeCode, location, properties);
    }
}

