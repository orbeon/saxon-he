package net.sf.saxon.option.jdom;

import net.sf.saxon.om.FastStringBuffer;
import net.sf.saxon.om.NodeInfo;
import net.sf.saxon.trans.XPathException;
import org.jdom.*;

import java.util.Stack;

/**
  * JDOMWriter is a Receiver that constructs a JDOM document from the stream of events
  */

public class JDOMWriter extends net.sf.saxon.event.Builder {

    private Document document;
    private Stack ancestors = new Stack();
    private boolean implicitDocumentNode = false;
    private FastStringBuffer textBuffer = new FastStringBuffer(FastStringBuffer.MEDIUM);

    /**
     * Create a JDOMWriter using the default node factory
     */

    public JDOMWriter() {
        //this.nodeFactory = new NodeFactory();
    }

    /**
     * Notify an unparsed entity URI.
     *
     * @param name     The name of the unparsed entity
     * @param systemID The system identifier of the unparsed entity
     * @param publicID The public identifier of the unparsed entity
     */

    public void setUnparsedEntity(String name, String systemID, String publicID) throws XPathException {
       // no-op
    }

    /**
    * Start of the document.
    */

    public void open () {}

    /**
    * End of the document.
    */

    public void close () {}

    /**
     * Start of a document node.
    */

    public void startDocument(int properties) throws XPathException {
        document = new Document();
        document.setBaseURI(systemId);
        ancestors.push(document);
        textBuffer.setLength(0);
    }

    /**
     * Notify the end of a document node
     */

    public void endDocument() throws XPathException {
        ancestors.pop();
    }

    /**
    * Start of an element.
    */

    public void startElement (int nameCode, int typeCode, int locationId, int properties) throws XPathException {
        flush();
        String local = namePool.getLocalName(nameCode);
        String uri = namePool.getURI(nameCode);
        String prefix = namePool.getPrefix(nameCode);
        Element element;
        if (ancestors.isEmpty()) {
            startDocument(0);
            implicitDocumentNode = true;
        }
        element = new Element(local, prefix, uri);
        if (ancestors.size() == 1) {
            document.setRootElement(element);
        } else {
            ((Element)ancestors.peek()).addContent(element);
        }
        ancestors.push(element);
    }

    public void namespace (int namespaceCode, int properties) throws XPathException {
        String prefix = namePool.getPrefixFromNamespaceCode(namespaceCode);
        String uri = namePool.getURIFromNamespaceCode(namespaceCode);
        Namespace ns = (prefix.length()==0 ?
                Namespace.getNamespace(uri) :
                Namespace.getNamespace(prefix, uri));
        ((Element)ancestors.peek()).addNamespaceDeclaration(ns);
    }

    public void attribute (int nameCode, int typeCode, CharSequence value, int locationId, int properties)
    throws XPathException {
        String local = namePool.getLocalName(nameCode);
        String uri = namePool.getURI(nameCode);
        String prefix = namePool.getPrefix(nameCode);
        Namespace ns = (prefix.length()==0 ?
                Namespace.getNamespace(uri) :
                Namespace.getNamespace(prefix, uri));
        Attribute att = new Attribute(local, value.toString(), ns);
        ((Element)ancestors.peek()).getAttributes().add(att);
    }

    public void startContent() throws XPathException {
        flush();
    }

    /**
    * End of an element.
    */

    public void endElement () throws XPathException {
        flush();
        ancestors.pop();
        Object parent = ancestors.peek();
        if (parent == document && implicitDocumentNode) {
            endDocument();
        }
    }

    /**
    * Character data.
    */

    public void characters (CharSequence chars, int locationId, int properties) throws XPathException {
        textBuffer.append(chars);
    }

    private void flush() {
        if (textBuffer.length() != 0) {
            Text text = new Text(textBuffer.toString());
            ((Element)ancestors.peek()).addContent(text);
            textBuffer.setLength(0);
        }
    }


    /**
    * Handle a processing instruction.
    */

    public void processingInstruction (String target, CharSequence data, int locationId, int properties)
            throws XPathException {
        flush();
        ProcessingInstruction pi = new ProcessingInstruction(target, data.toString());
        ((Element)ancestors.peek()).addContent(pi);
    }

    /**
    * Handle a comment.
    */

    public void comment (CharSequence chars, int locationId, int properties) throws XPathException{
        flush();
        Comment comment = new Comment(chars.toString());
        ((Element)ancestors.peek()).addContent(comment);
    }

    /**
     * Ask whether this Receiver (or the downstream pipeline) makes any use of the type annotations
     * supplied on element and attribute events
     * @return true if the Receiver makes any use of this information. If false, the caller
     *         may supply untyped nodes instead of supplying the type annotation
     */

    public boolean usesTypeAnnotations() {
        return false;
    }

    /**
     * Get the constructed document node
     * @return the document node of the constructed XOM tree
     */

    public Document getDocument() {
        return document;
    }

    /**
     * Get the current root node.
     * @return a Saxon wrapper around the constructed XOM document node
     */

    public NodeInfo getCurrentRoot() {
        return new DocumentWrapper(document, systemId, config);
    }
}


// Original Code is Copyright (c) Saxonica Limited 2009. All rights reserved.

//
// The contents of this file are subject to the Mozilla Public License Version 1.1 (the "License");
// you may not use this file except in compliance with the License. You may obtain a copy of the
// License at http://www.mozilla.org/MPL/
//
// Software distributed under the License is distributed on an "AS IS" basis,
// WITHOUT WARRANTY OF ANY KIND, either express or implied.
// See the License for the specific language governing rights and limitations under the License.
//
// The Original Code is: all this file.
//
// The Initial Developer of the Original Code is Saxonica Limited
//
// Portions created by (your name) are Copyright (C) (your legal entity). All Rights Reserved.
//
// Contributor(s): none
//