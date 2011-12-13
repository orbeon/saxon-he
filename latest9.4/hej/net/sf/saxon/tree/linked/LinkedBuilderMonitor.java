package net.sf.saxon.tree.linked;

import net.sf.saxon.event.BuilderMonitor;
import net.sf.saxon.om.*;
import net.sf.saxon.pattern.NameTest;
import net.sf.saxon.trans.XPathException;
import net.sf.saxon.type.SimpleType;
import net.sf.saxon.type.Type;

/**
 * Monitor construction of a TinyTree. This allows a marker to be set during tree construction, in such a way
 * that the node corresponding to the marker can be retrieved at the end of tree construction. This is used in the
 * implementation of the XSLT 3.0 snapshot function.
 */
public class LinkedBuilderMonitor extends BuilderMonitor {

    private LinkedTreeBuilder builder;
    private int mark = -1;
    /*@Nullable*/ private NodeInfo markedNode;

    public LinkedBuilderMonitor(/*@NotNull*/ LinkedTreeBuilder builder) {
        super(builder);
        this.builder = builder;
    }

    public void markNextNode(int nodeKind) {
        mark = nodeKind;
    }

    public void startDocument(int properties) throws XPathException {
        super.startDocument(properties);
        if (mark == Type.DOCUMENT) {
            markedNode = builder.getCurrentParentNode();
        }
        mark = -1;
    }


    public void startContent() throws XPathException {
        super.startContent();
        if (mark == Type.ELEMENT) {
            markedNode = builder.getCurrentParentNode();
        }
        mark = -1;
    }

    public void characters(CharSequence chars, int locationId, int properties) throws XPathException {
        super.characters(chars, locationId, properties);
        if (mark == Type.TEXT) {
            markedNode = builder.getCurrentLeafNode();
        }
        mark = -1;
    }

    public void comment(CharSequence chars, int locationId, int properties) throws XPathException {
        super.comment(chars, locationId, properties);
        if (mark == Type.COMMENT) {
            markedNode = builder.getCurrentLeafNode();
        }
        mark = -1;
    }

    public void processingInstruction(String target, CharSequence data, int locationId, int properties) throws XPathException {
        super.processingInstruction(target, data, locationId, properties);
        if (mark == Type.PROCESSING_INSTRUCTION) {
            markedNode = builder.getCurrentLeafNode();
        }
        mark = -1;
    }

    public void attribute(/*@NotNull*/ NodeName nameCode, SimpleType typeCode, CharSequence value, int locationId, int properties)
            throws XPathException {
        super.attribute(nameCode, typeCode, value, locationId, properties);
        if (mark == Type.ATTRIBUTE) {
            NodeInfo element = builder.getCurrentParentNode();
            markedNode = (NodeInfo)element.iterateAxis(
                    Axis.ATTRIBUTE, new NameTest(Type.ATTRIBUTE, nameCode, element.getNamePool())).next();
            mark = -1;
        }
    }

    public void namespace(/*@NotNull*/ NamespaceBinding namespaceBinding, int properties) throws XPathException {
        super.namespace(namespaceBinding, properties);
        if (mark == Type.NAMESPACE) {
            NodeInfo element = builder.getCurrentParentNode();
            NamePool pool = element.getNamePool();
            String prefix =  namespaceBinding.getPrefix();
            markedNode = (NodeInfo)element.iterateAxis(
                    Axis.NAMESPACE, new NameTest(Type.NAMESPACE, "", prefix, pool)).next();
            mark = -1;
        }
    }

    /*@Nullable*/ public NodeInfo getMarkedNode() {
        return markedNode;
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
// The Initial Developer of the Original Code is Saxonica Limited.
// Portions created by ___ are Copyright (C) ___. All rights reserved.
//
// Contributor(s):
//