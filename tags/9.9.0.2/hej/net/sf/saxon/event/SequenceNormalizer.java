////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
// Copyright (c) 2018 Saxonica Limited.
// This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
// If a copy of the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
// This Source Code Form is "Incompatible With Secondary Licenses", as defined by the Mozilla Public License, v. 2.0.
////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

package net.sf.saxon.event;

import net.sf.saxon.expr.parser.Location;
import net.sf.saxon.om.NamespaceBindingSet;
import net.sf.saxon.om.NodeName;
import net.sf.saxon.s9api.Action;
import net.sf.saxon.s9api.Destination;
import net.sf.saxon.s9api.SaxonApiException;
import net.sf.saxon.trans.XPathException;
import net.sf.saxon.type.SchemaType;
import net.sf.saxon.type.SimpleType;

import java.util.ArrayList;
import java.util.List;

/**
 * Implement the "sequence normalization" logic as defined in the XSLT 3.0/XQuery 3.0
 * serialization spec.
 * <p>
 * There are two subclasses, which handle the different logic for the case where an
 * {@code item-separator} is provided, and the case where whitespace-separation is used.
 * Note that the whitespace case behaves differently from the case where the item-separator
 * is set to a single space.
 * <p>Because this Receiver is often used as the entry point to the pipeline for a
 * {@link Destination}, it includes logic allowing {@code onClose} action for the
 * Destination to be triggered when the Receiver is closed.</p>
 */

public abstract class SequenceNormalizer extends ProxyReceiver {

    protected int level = 0;
    protected boolean inStartTag = false;
    private List<Action> actionList;
    private boolean failed = false;

    public SequenceNormalizer(Receiver next) {
        super(next);
    }

    /**
     * Start of event stream
     */
    @Override
    public void open() throws XPathException {
        level = 0;
        previousAtomic = false;
        super.open();
        getNextReceiver().startDocument(0);
    }

    /**
     * Start of a document node.
     */
    @Override
    public void startDocument(int properties) throws XPathException {
        level++;
        previousAtomic = false;
    }

    /**
     * Notify the end of a document node
     */
    @Override
    public void endDocument() throws XPathException {
        level--;
        previousAtomic = false;
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
        try {
            maybeStartContent();
            level++;
            super.startElement(elemName, typeCode, location, properties);
            inStartTag = true;
            previousAtomic = false;
        } catch (XPathException e) {
            failed = true;
            throw e;
        }
    }

    /**
     * Notify an attribute. Attributes are notified after the startElement event, and before any
     * children. Namespaces and attributes may be intermingled.
     *
     * @param name       The name of the attribute
     * @param typeCode   The type of the attribute
     * @param value
     * @param locationId the location of the node in the source, or of the instruction that created it
     * @param properties Bit significant value. The following bits are defined:
     *                   <dl>
     *                   <dd>DISABLE_ESCAPING</dd>    <dt>Disable escaping for this attribute</dt>
     *                   <dd>NO_SPECIAL_CHARACTERS</dd>      <dt>Attribute value contains no special characters</dt>
     *                   </dl>
     * @throws IllegalStateException: attempt to output an attribute when there is no open element
     */
    @Override
    public void attribute(NodeName name, SimpleType typeCode, CharSequence value, Location locationId, int properties) throws XPathException {
        if (level == 0) {
            failed = true;
            XPathException err = new XPathException("Cannot serialize a free-standing attribute node (" + name + ')', "SENR0001");
            err.setLocation(locationId);
            throw err;
        }
        try {
            super.attribute(name, typeCode, value, locationId, properties);
        } catch (XPathException e) {
            failed = true;
            throw e;
        }
    }

    /**
     * Notify a namespace. Namespaces are notified <b>after</b> the startElement event, and before
     * any children for the element. The namespaces that are reported are only required
     * to include those that are different from the parent element; however, duplicates may be reported.
     * A namespace must not conflict with any namespaces already used for element or attribute names.
     *
     * @param namespaceBindings the prefix/uri pair representing the namespace binding
     * @param properties        any special properties to be passed on this call
     * @throws IllegalStateException: attempt to output a namespace when there is no open element
     *                                start tag
     */
    @Override
    public void namespace(NamespaceBindingSet namespaceBindings, int properties) throws XPathException {
        if (level == 0) {
            failed = true;
            throw new XPathException("Cannot serialize a free-standing namespace node", "SENR0001");
        }
        try {
            super.namespace(namespaceBindings, properties);
        } catch (XPathException e) {
            failed = true;
            throw e;
        }
    }

    /**
     * Notify the start of the content, that is, the completion of all attributes and namespaces.
     * Note that the initial receiver of output from XSLT instructions will not receive this event,
     * it has to detect it itself. Note that this event is reported for every element even if it has
     * no attributes, no namespaces, and no content.
     */
    @Override
    public void startContent() throws XPathException {
        try {
            super.startContent();
            inStartTag = false;
        } catch (XPathException e) {
            failed = true;
            throw e;
        }
    }

    private void maybeStartContent() throws XPathException {
        if (inStartTag) {
            startContent();
        }
    }

    /**
     * Character data
     */
    @Override
    public void characters(CharSequence chars, Location locationId, int properties) throws XPathException {
        try {
            maybeStartContent();
            super.characters(chars, locationId, properties);
            previousAtomic = false;
        } catch (XPathException e) {
            failed = true;
            throw e;
        }
    }

    /**
     * Processing Instruction
     */
    @Override
    public void processingInstruction(String target, CharSequence data, Location locationId, int properties) throws XPathException {
        try {
            maybeStartContent();
            super.processingInstruction(target, data, locationId, properties);
            previousAtomic = false;
        } catch (XPathException e) {
            failed = true;
            throw e;
        }
    }

    /**
     * Output a comment
     */
    @Override
    public void comment(CharSequence chars, Location locationId, int properties) throws XPathException {
        try {
            maybeStartContent();
            super.comment(chars, locationId, properties);
            previousAtomic = false;
        } catch (XPathException e) {
            failed = true;
            throw e;
        }
    }

    /**
     * End of element
     */
    @Override
    public void endElement() throws XPathException {
        try {
            maybeStartContent();
            level--;
            super.endElement();
            previousAtomic = false;
        } catch (XPathException e) {
            failed = true;
            throw e;
        }
    }

    /**
     * End of output. Note that closing this receiver also closes the rest of the
     * pipeline.
     */
    @Override
    public void close() throws XPathException {
        if (failed) {
            super.close();
        } else {
            getNextReceiver().endDocument();
            super.close();
            try {
                if (actionList != null) {
                    for (Action action : actionList) {
                        action.act();
                    }
                }
            } catch (SaxonApiException e) {
                throw XPathException.makeXPathException(e);
            }
        }
    }

    /**
     * Set actions to be performed when this {@code Receiver} is closed
     * @param actionList a list of actions to be performed
     */

    public void onClose(List<Action> actionList) {
        this.actionList = actionList;
    }

    public void onClose(Action action) {
        if (actionList == null) {
            actionList = new ArrayList<>();
        }
        actionList.add(action);
    }

}

