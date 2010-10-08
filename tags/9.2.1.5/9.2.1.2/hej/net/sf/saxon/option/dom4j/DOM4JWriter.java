package net.sf.saxon.option.dom4j;

import net.sf.saxon.om.FastStringBuffer;
import net.sf.saxon.om.NodeInfo;
import net.sf.saxon.trans.XPathException;
import org.dom4j.*;
import org.dom4j.tree.*;

import java.util.Stack;

/**
  * JDOMWriter is a Receiver that constructs a JDOM document from the stream of events
  */

public class DOM4JWriter extends net.sf.saxon.event.Builder {

    private Document document;
    private Stack ancestors = new Stack();
    private boolean implicitDocumentNode = false;
    private FastStringBuffer textBuffer = new FastStringBuffer(FastStringBuffer.MEDIUM);

    /**
     * Create a JDOMWriter using the default node factory
     */

    public DOM4JWriter() {
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
        document = new DefaultDocument();
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
        QName name = new QName(local, new Namespace(prefix, uri));
        if (ancestors.size() == 1) {
            element = new DefaultElement(name);
            document.setRootElement(element);
        } else {
            element = ((Element)ancestors.peek()).addElement(name);
        }
        ancestors.push(element);
    }

    public void namespace (int namespaceCode, int properties) throws XPathException {
        String prefix = namePool.getPrefixFromNamespaceCode(namespaceCode);
        String uri = namePool.getURIFromNamespaceCode(namespaceCode);
        ((Element)ancestors.peek()).addNamespace(prefix, uri);
    }

    public void attribute (int nameCode, int typeCode, CharSequence value, int locationId, int properties)
    throws XPathException {
        String local = namePool.getLocalName(nameCode);
        String uri = namePool.getURI(nameCode);
        String prefix = namePool.getPrefix(nameCode);
        Namespace ns = new Namespace(prefix, uri);
        Attribute att = new DefaultAttribute(local, value.toString(), ns);
        ((Element)ancestors.peek()).add(att);
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
            Text text = new DefaultText(textBuffer.toString());
            ((Element)ancestors.peek()).add(text);
            textBuffer.setLength(0);
        }
    }


    /**
    * Handle a processing instruction.
    */

    public void processingInstruction (String target, CharSequence data, int locationId, int properties)
            throws XPathException {
        flush();
        ProcessingInstruction pi = new DefaultProcessingInstruction(target, data.toString());
        ((Element)ancestors.peek()).add(pi);
    }

    /**
    * Handle a comment.
    */

    public void comment (CharSequence chars, int locationId, int properties) throws XPathException{
        flush();
        Comment comment = new DefaultComment(chars.toString());
        ((Element)ancestors.peek()).add(comment);
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

//
// The contents of this file are subject to the Mozilla Public License Version 1.0 (the "License");
// you may not use this file except in compliance with the License. You may obtain a copy of the
// License at http://www.mozilla.org/MPL/
//
// Software distributed under the License is distributed on an "AS IS" basis,
// WITHOUT WARRANTY OF ANY KIND, either express or implied.
// See the License for the specific language governing rights and limitations under the License.
//
// The Original Code is: all this file
//
// The Initial Developer of the Original Code is Michael H. Kay.
//
// Contributor(s):
//