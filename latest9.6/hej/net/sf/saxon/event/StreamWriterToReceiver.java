////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
// Copyright (c) 2014 Saxonica Limited.
// This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
// If a copy of the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
// This Source Code Form is "Incompatible With Secondary Licenses", as defined by the Mozilla Public License, v. 2.0.
////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

package net.sf.saxon.event;

import net.sf.saxon.lib.StandardURIChecker;
import net.sf.saxon.om.*;
import net.sf.saxon.serialize.charcode.UTF16CharacterSet;
import net.sf.saxon.trans.Err;
import net.sf.saxon.trans.XPathException;
import net.sf.saxon.type.BuiltInAtomicType;
import net.sf.saxon.type.Untyped;
import net.sf.saxon.z.IntPredicate;

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;
import java.util.*;

/**
 * This class implements the XmlStreamWriter interface, translating the events into Saxon
 * Receiver events. The Receiver can be anything: a serializer, a schema validator, a tree builder.
 * <p/>
 * <p>The class will attempt to generate namespace prefixes where none have been supplied, unless the
 * <code>inventPrefixes</code> option is set to false. The preferred mode of use is to call the versions
 * of <code>writeStartElement</code> and <code>writeAttribute</code> that supply the prefix, URI, and
 * local name in full. If the prefix is omitted, the class attempts to invent a prefix. If the URI is
 * omitted, the name is assumed to be in no namespace. The <code>writeNamespace</p> method should be
 * called only if there is a need to declare a namespace prefix that is not used on any element or
 * attribute name.</p>
 * <p/>
 * <p>The class will check all names, URIs, and character content for conformance against XML well-formedness
 * rules unless the <code>checkValues</code> option is set to false.</p>
 *
 * @since 9.3. Rewritten May 2015 to fix bug 2357. The handling of namespaces is probably not 100% conformant,
 * but it's difficult to establish what all the edge cases are, and it handles the common cases well.
 */
public class StreamWriterToReceiver implements XMLStreamWriter {

    private static class Triple {
        public String prefix;
        public String uri;
        public String local;
        public String value;
    }

    private static class StartTag {
        public Triple elementName;
        public List<Triple> attributes;
        public List<Triple> namespaces;

        public StartTag() {
            elementName = new Triple();
            attributes = new ArrayList<Triple>();
            namespaces = new ArrayList<Triple>();
        }
    }

    private StartTag pendingTag = null;

    /**
     * The receiver to which events will be passed
     */

    private Receiver receiver;

    /**
     * The Saxon NamePool
     */

    private NamePool namePool;

    /**
     * The Checker used for testing valid characters
     */

    private IntPredicate charChecker;

    /**
     * Flag to indicate whether names etc are to be checked for well-formedness
     */

    private boolean isChecking = false;

    /**
     * The current depth of element nesting. -1 indicates outside startDocument/endDocument; non-negative
     * values indicate the number of open start element tags
     */

    private int depth = -1;

    /**
     * Flag indicating that an empty element has been requested.
     */
    private boolean isEmptyElement;

    /**
     * Flag indicating that default prefixes should be allocated if the user does not declare them explicitly
     */

    private boolean inventPrefixes = true;

    /**
     * inScopeNamespaces represents namespaces that have been declared in the XML stream
     */
    private NamespaceReducer inScopeNamespaces;

    /**
     * declaredNamespaces represents prefix-to-uri bindings that have been set using setPrefix. These
     * do not necessarily correspond to namespace declarations appearing in the XML stream. Note that
     * this is a map from URIs to prefixes, not the other way around!
     */

    private Map<String, String> declaredNamespaces = new HashMap<String, String>(10);

    /**
     * rootNamespaceContext is the namespace context supplied at the start, is the final fallback
     * for allocating a prefix to a URI
     */

    private javax.xml.namespace.NamespaceContext rootNamespaceContext = null;

    /**
     * Constructor. Creates a StreamWriter as a front-end to a given Receiver.
     *
     * @param receiver the Receiver that is to receive the events generated
     *                 by this StreamWriter.
     */
    public StreamWriterToReceiver(Receiver receiver) {
        // Events are passed through a NamespaceReducer which maintains the namespace context
        // It also eliminates duplicate namespace declarations, and creates extra namespace declarations
        // where needed to support prefix-uri mappings used on elements and attributes
        PipelineConfiguration pipe = receiver.getPipelineConfiguration();
        this.inScopeNamespaces = new NamespaceReducer(receiver);
        this.receiver = inScopeNamespaces;
        //this.receiver = new TracingFilter(this.receiver);
        this.charChecker = pipe.getConfiguration().getValidCharacterChecker();
        this.namePool = pipe.getConfiguration().getNamePool();
    }

    /**
     * Say whether prefixes are to be invented when none is specified by the user
     *
     * @param invent true if prefixes are to be invented. Default is true;
     */

    public void setInventPrefixes(boolean invent) {
        this.inventPrefixes = invent;
    }

    /**
     * Ask whether prefixes are to be invented when none is specified by the user
     *
     * @return true if prefixes are to be invented. Default is true;
     */

    public boolean isInventPrefixes() {
        return this.inventPrefixes;
    }

    /**
     * Say whether names and values are to be checked for conformance with XML rules
     *
     * @param check true if names and values are to be checked. Default is true;
     */

    public void setCheckValues(boolean check) {
        this.isChecking = check;
    }

    /**
     * Ask whether names and values are to be checked for conformance with XML rules
     *
     * @return true if names and values are to be checked. Default is true;
     */

    public boolean isCheckValues() {
        return this.isChecking;
    }

    public void flushStartTag() throws XMLStreamException {
        if (depth == -1) {
            writeStartDocument();
        }
        if (pendingTag != null) {
            try {
                completeTriple(pendingTag.elementName, false);
                for (Triple t : pendingTag.attributes) {
                    completeTriple(t, true);
                }
                NodeName nc;
                if (pendingTag.elementName.uri.isEmpty()) {
                    nc = new NoNamespaceName(pendingTag.elementName.local);
                } else {
                    nc = new FingerprintedQName(pendingTag.elementName.prefix, pendingTag.elementName.uri, pendingTag.elementName.local);
                }
                receiver.startElement(nc, Untyped.getInstance(), 0, 0);
                for (Triple t : pendingTag.namespaces) {
                    if (t.prefix == null) {
                        t.prefix = "";
                    }
                    if (t.uri == null) {
                        t.uri = "";
                    }
                    receiver.namespace(new NamespaceBinding(t.prefix, t.uri), 0);
                }
                for (Triple t : pendingTag.attributes) {
                    if (t.uri.isEmpty()) {
                        nc = new NoNamespaceName(t.local);
                    } else {
                        nc = new FingerprintedQName(t.prefix, t.uri, t.local);
                    }
                    receiver.attribute(nc, BuiltInAtomicType.UNTYPED_ATOMIC, t.value, 0, 0);
                }
                pendingTag = null;
                receiver.startContent();
                if (isEmptyElement) {
                    isEmptyElement = false;
                    depth--;
                    receiver.endElement();
                }
            } catch (XPathException e) {
                throw new XMLStreamException(e);
            }
        }
    }

    public void completeTriple(Triple t, boolean isAttribute) throws XMLStreamException {
        if (t.local == null) {
            throw new XMLStreamException("Local name of " + (isAttribute ? "Attribute" : "Element") + " is missing");
        }
        if (isChecking && !isValidNCName(t.local)) {
            throw new XMLStreamException("Local name of " + (isAttribute ? "Attribute" : "Element") +
                Err.wrap(t.local) + " is invalid");
        }
        if (t.prefix == null) {
            t.prefix = "";
        }
        if (t.uri == null) {
            t.uri = "";
        }
        if (isChecking && !t.uri.isEmpty() && !isValidURI(t.uri)) {
            throw new XMLStreamException("Namespace URI " + Err.wrap(t.local) + " is invalid");
        }
        if (t.prefix.isEmpty()) {
            if (t.uri.isEmpty()) {
                if (isAttribute) {
                    // no action
                } else {
                    String ns = getDefaultNamespace();
                    t.uri = ns == null ? "" : ns;
                }
            } else {
                t.prefix = getPrefixForUri(t.uri);
                if (t.prefix.isEmpty() && isAttribute) {
                    t.prefix = inventPrefix(t.uri);
                }
            }
        } else {
            if (isChecking && !isValidNCName(t.prefix)) {
                throw new XMLStreamException("Prefix of " + (isAttribute ? "Attribute" : "Element") +
                    Err.wrap(t.local) + " is invalid");
            }
            if (t.uri.isEmpty()) {
                t.uri = getUriForPrefix(t.prefix);
            } else {
                String u = getUriForPrefix(t.prefix);
                if (!t.uri.equals(u)) {
                    throw new XMLStreamException("Prefix " + Err.wrap(t.local) + " is bound to a different URI");
                }
            }
        }
    }

    private String getDefaultNamespace() {
        for (Triple t : pendingTag.namespaces) {
            if (t.prefix == null || t.prefix.isEmpty()) {
                return t.uri;
            }
        }
        return inScopeNamespaces.getURIForPrefix("", true);
    }

    private String getUriForPrefix(String prefix) {
        for (Triple t : pendingTag.namespaces) {
            if (prefix.equals(t.prefix)) {
                return t.uri;
            }
        }
        return inScopeNamespaces.getURIForPrefix(prefix, false);
    }

    private String getPrefixForUri(String uri) {
        for (Triple t : pendingTag.namespaces) {
            if (uri.equals(t.uri)) {
                return t.prefix == null ? "" : t.prefix;
            }
        }
        Iterator<String> prefixes = inScopeNamespaces.iteratePrefixes();
        while (prefixes.hasNext()) {
            String p = prefixes.next();
            if (inScopeNamespaces.getURIForPrefix(p, false).equals(uri)) {
                return p;
            }
        }
        return "";
    }

    public void writeStartElement(String localName) throws XMLStreamException {
        flushStartTag();
        depth++;
        pendingTag = new StartTag();
        pendingTag.elementName.local = localName;
    }

    public void writeStartElement(String namespaceURI, String localName) throws XMLStreamException {
        flushStartTag();
        depth++;
        pendingTag = new StartTag();
        pendingTag.elementName.local = localName;
        pendingTag.elementName.uri = namespaceURI;

    }

    public void writeStartElement(String prefix, String localName, String namespaceURI) throws XMLStreamException {
        flushStartTag();
        depth++;
        pendingTag = new StartTag();
        pendingTag.elementName.local = localName;
        pendingTag.elementName.uri = namespaceURI;
        pendingTag.elementName.prefix = prefix;
    }

    /**
     * Creates a prefix that is not currently in use.
     *
     * @param uri the URI for which a prefix is required
     * @return the chosen prefix
     */
    private String inventPrefix(String uri) {
        String prefix = getPrefix(uri);
        if (prefix != null) {
            return prefix;
        }
        prefix = namePool.suggestPrefixForURI(uri);
        if (prefix != null) {
            return prefix;
        }
        int count = 0;
        while (true) {
            prefix = "ns" + count;
            if (inScopeNamespaces.getURIForPrefix(prefix, false) == null) {
                setPrefix(prefix, uri);
                return prefix;
            }
            count++;
        }
    }

    public void writeEmptyElement(String namespaceURI, String localName)
            throws XMLStreamException {
        flushStartTag();
        writeStartElement(namespaceURI, localName);
        isEmptyElement = true;
    }

    public void writeEmptyElement(String prefix, String localName, String namespaceURI) throws XMLStreamException {
        flushStartTag();
        writeStartElement(prefix, localName, namespaceURI);
        isEmptyElement = true;
    }

    public void writeEmptyElement(String localName) throws XMLStreamException {
        flushStartTag();
        writeStartElement(localName);
        isEmptyElement = true;
    }

    public void writeEndElement() throws XMLStreamException {
        if (depth <= 0) {
            throw new IllegalStateException("writeEndElement with no matching writeStartElement");
        }
//        if (isEmptyElement) {
//            throw new IllegalStateException("writeEndElement called for an empty element");
//        }
        try {
            flushStartTag();
            receiver.endElement();
            depth--;
        } catch (XPathException err) {
            throw new XMLStreamException(err);
        }
    }

    public void writeEndDocument() throws XMLStreamException {
        if (depth == -1) {
            throw new IllegalStateException("writeEndDocument with no matching writeStartDocument");
        }
        try {
            flushStartTag();
            while (depth > 0) {
                writeEndElement();
            }
            receiver.endDocument();
            depth = -1;
        } catch (XPathException err) {
            throw new XMLStreamException(err);
        }
    }

    public void close() throws XMLStreamException {
        if (depth >= 0) {
            writeEndDocument();
        }
        try {
            receiver.close();
        } catch (XPathException err) {
            throw new XMLStreamException(err);
        }
    }

    public void flush() throws XMLStreamException {
        // no action
    }

    public void writeAttribute(String localName, String value) throws XMLStreamException {
        if (pendingTag == null) {
            throw new IllegalStateException("Cannot write attribute when not in a start tag");
        }
        Triple t = new Triple();
        t.local = localName;
        t.value = value;
        pendingTag.attributes.add(t);
    }

    public void writeAttribute(String prefix, String namespaceURI, String localName, String value)
            throws XMLStreamException {
        if (pendingTag == null) {
            throw new IllegalStateException("Cannot write attribute when not in a start tag");
        }
        Triple t = new Triple();
        t.prefix = prefix;
        t.uri = namespaceURI;
        t.local = localName;
        t.value = value;
        pendingTag.attributes.add(t);
        try {
            NodeName nn = new FingerprintedQName(prefix, namespaceURI, localName);
            receiver.attribute(nn, BuiltInAtomicType.UNTYPED_ATOMIC, value, -1, 0);
        } catch (XPathException err) {
            throw new XMLStreamException(err);
        }
    }

    public void writeAttribute(String namespaceURI, String localName, String value) throws XMLStreamException {
        Triple t = new Triple();
        t.uri = namespaceURI;
        t.local = localName;
        t.value = value;
        pendingTag.attributes.add(t);

    }

    public void writeNamespace(String prefix, String namespaceURI) throws XMLStreamException {
        if (pendingTag == null) {
            throw new IllegalStateException("Cannot write namespace when not in a start tag");
        }
        Triple t = new Triple();
        t.uri = namespaceURI;
        t.prefix = prefix;
        pendingTag.namespaces.add(t);

    }

    public void writeDefaultNamespace(String namespaceURI)
            throws XMLStreamException {
        if (pendingTag == null) {
            throw new IllegalStateException("Cannot write namespace when not in a start tag");
        }
        Triple t = new Triple();
        t.uri = namespaceURI;
        pendingTag.namespaces.add(t);

    }

    public void writeComment(String data) throws XMLStreamException {
        flushStartTag();
        if (data == null) {
            data = "";
        }
        try {
            if (!isValidChars(data)) {
                throw new IllegalArgumentException("Invalid XML character in comment: " + data);
            }
            if (isChecking && data.contains("--")) {
                throw new IllegalArgumentException("Comment contains '--'");
            }
            receiver.comment(data, 0, 0);
        } catch (XPathException err) {
            throw new XMLStreamException(err);
        }
    }

    public void writeProcessingInstruction(String target) throws XMLStreamException {
        writeProcessingInstruction(target, "");
    }

    public void writeProcessingInstruction(/*@NotNull*/ String target, /*@NotNull*/ String data) throws XMLStreamException {
        flushStartTag();
        try {
            if (isChecking) {
                if (!isValidNCName(target) || "xml".equalsIgnoreCase(target)) {
                    throw new IllegalArgumentException("Invalid PITarget: " + target);
                }
                if (!isValidChars(data)) {
                    throw new IllegalArgumentException("Invalid character in PI data: " + data);
                }
            }
            receiver.processingInstruction(target, data, 0, 0);
        } catch (XPathException err) {
            throw new XMLStreamException(err);
        }
    }

    public void writeCData(/*@NotNull*/ String data) throws XMLStreamException {
        flushStartTag();
        writeCharacters(data);
    }

    public void writeDTD(String dtd) throws XMLStreamException {
        // no-op
    }

    public void writeEntityRef(String name) throws XMLStreamException {
        throw new UnsupportedOperationException("writeEntityRef");
    }

    public void writeStartDocument() throws XMLStreamException {
        writeStartDocument(null, null);
    }

    public void writeStartDocument(/*@Nullable*/ String version) throws XMLStreamException {
        writeStartDocument(null, version);
    }

    public void writeStartDocument(/*@Nullable*/ String encoding, /*@Nullable*/ String version) throws XMLStreamException {
        if (depth != -1) {
            throw new IllegalStateException("writeStartDocument must be the first call");
        }
        try {
            receiver.startDocument(0);
        } catch (XPathException err) {
            throw new XMLStreamException(err);
        }
        depth = 0;
    }

    public void writeCharacters(/*@Nullable*/ String text)
            throws XMLStreamException {
        flushStartTag();
        if (text != null) {
            if (!isValidChars(text)) {
                throw new IllegalArgumentException("illegal XML character: " + text);
            }
            try {
                receiver.characters(text, 0, 0);
            } catch (XPathException err) {
                throw new XMLStreamException(err);
            }
        }
    }

    public void writeCharacters(char[] text, int start, int len)
            throws XMLStreamException {
        writeCharacters(new String(text, start, len));
    }

    public String getPrefix(String uri) {
        String prefix = declaredNamespaces.get(uri);
        if (prefix == null && rootNamespaceContext != null) {
            prefix = rootNamespaceContext.getPrefix(uri);
        }
        return prefix;
    }

    public void setPrefix(String prefix, String uri) {
        if (!isValidURI(uri)) {
            throw new IllegalArgumentException("Invalid namespace URI: " + uri);
        }
        if (!"".equals(prefix) && !isValidNCName(prefix)) {
            throw new IllegalArgumentException("Invalid namespace prefix: " + prefix);
        }
        declaredNamespaces.put(uri, prefix); //sic
    }

    public void setDefaultNamespace(String uri)
            throws XMLStreamException {
        setPrefix("", uri);
    }

    public void setNamespaceContext(javax.xml.namespace.NamespaceContext context)
            throws XMLStreamException {
        if (depth > 0) {
            throw new IllegalStateException("setNamespaceContext may only be called at the start of the document");
        }
        // Unfortunately the JAXP NamespaceContext class does not allow us to discover all the namespaces
        // that were declared, nor to declare new ones. So we have to retain it separately
        rootNamespaceContext = context;
    }

    /*@Nullable*/
    public javax.xml.namespace.NamespaceContext getNamespaceContext() {
        // Note: the spec is unclear. We return the namespace context that was supplied to setNamespaceContext,
        // regardless of any other subsequent additions
        return rootNamespaceContext;
    }

    public Object getProperty(String name) throws IllegalArgumentException {
        if (name.equals("javax.xml.stream.isRepairingNamespaces")) {
            return inventPrefixes;
        } else {
            throw new IllegalArgumentException(name);
        }
    }

    /**
     * Test whether a supplied name is a valid NCName
     *
     * @param name the name to be tested
     * @return true if the name is valid or if checking is disabled
     */

    private boolean isValidNCName(String name) {
        return !isChecking || NameChecker.isValidNCName(name);
    }

    /**
     * Test whether a supplied character string is valid in XML
     *
     * @param text the string to be tested
     * @return true if the string is valid or if checking is disabled
     */

    private boolean isValidChars(String text) {
        return !isChecking || (UTF16CharacterSet.firstInvalidChar(text, charChecker) == -1);
    }

    /**
     * Test whether a supplied namespace URI is a valid URI
     *
     * @param uri the namespace URI to be tested
     * @return true if the name is valid or if checking is disabled
     */

    private boolean isValidURI(String uri) {
        return !isChecking || StandardURIChecker.getInstance().isValidURI(uri);
    }

}

