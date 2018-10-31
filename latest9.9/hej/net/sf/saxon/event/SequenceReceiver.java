////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
// Copyright (c) 2018 Saxonica Limited.
// This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
// If a copy of the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
// This Source Code Form is "Incompatible With Secondary Licenses", as defined by the Mozilla Public License, v. 2.0.
////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

package net.sf.saxon.event;

import net.sf.saxon.Configuration;
import net.sf.saxon.expr.parser.ExplicitLocation;
import net.sf.saxon.expr.parser.Location;
import net.sf.saxon.ma.arrays.ArrayItem;
import net.sf.saxon.ma.map.MapItem;
import net.sf.saxon.om.*;
import net.sf.saxon.trans.Err;
import net.sf.saxon.trans.XPathException;
import net.sf.saxon.tree.util.Orphan;
import net.sf.saxon.type.SimpleType;
import net.sf.saxon.type.Type;
import net.sf.saxon.value.AtomicValue;
import net.sf.saxon.value.ExternalObject;

/**
 * SequenceReceiver: this extension of the Receiver interface is used when processing
 * a sequence constructor. It differs from the Receiver in allowing items (atomic values or
 * nodes) to be added to the sequence, not just tree-building events.
 */

public abstract class SequenceReceiver implements Receiver {

    protected boolean previousAtomic = false;
    /*@NotNull*/
    protected PipelineConfiguration pipelineConfiguration;
    /*@Nullable*/
    protected String systemId = null;

    /**
     * Create a SequenceReceiver
     *
     * @param pipe the pipeline configuration
     */

    public SequenceReceiver(/*@NotNull*/ PipelineConfiguration pipe) {
        this.pipelineConfiguration = pipe;
    }

    /*@NotNull*/
    public final PipelineConfiguration getPipelineConfiguration() {
        return pipelineConfiguration;
    }

    public void setPipelineConfiguration(/*@NotNull*/ PipelineConfiguration pipelineConfiguration) {
        this.pipelineConfiguration = pipelineConfiguration;
    }

    /**
     * Get the Saxon Configuration
     *
     * @return the Configuration
     */

    public final Configuration getConfiguration() {
        return pipelineConfiguration.getConfiguration();
    }

    /**
     * Set the system ID
     *
     * @param systemId the URI used to identify the tree being passed across this interface
     */

    public void setSystemId(/*@Nullable*/ String systemId) {
        this.systemId = systemId;
    }

    /**
     * Get the system ID
     *
     * @return the system ID that was supplied using the setSystemId() method
     */

    /*@Nullable*/
    public String getSystemId() {
        return systemId;
    }

    /**
     * Notify an unparsed entity URI.
     *
     * @param name     The name of the unparsed entity
     * @param systemID The system identifier of the unparsed entity
     * @param publicID The public identifier of the unparsed entity
     */

    public void setUnparsedEntity(String name, String systemID, String publicID) throws XPathException {
    }

    /**
     * Start the output process
     */

    public void open() throws XPathException {
        previousAtomic = false;
    }

    /**
     * Append an arbitrary item (node, atomic value, or function) to the output
     *
     * @param item           the item to be appended
     * @param locationId     the location of the calling instruction, for diagnostics
     * @param properties if the item is an element node, this indicates whether its namespaces
     *                       need to be copied. Values are {@link ReceiverOptions#ALL_NAMESPACES},
     *                       {@link ReceiverOptions#LOCAL_NAMESPACES}; the default (0) means
     *                       no namespaces
     */

    public abstract void append(Item item, Location locationId, int properties) throws XPathException;

    /**
     * Append an arbitrary item (node, atomic value, or function) to the output.
     * By default, if the item is an element
     * node, it is copied with all namespaces.
     *
     * @param item the item to be appended
     * @throws net.sf.saxon.trans.XPathException if the operation fails
     */

    public void append(Item item) throws XPathException {
        append(item, ExplicitLocation.UNKNOWN_LOCATION, ReceiverOptions.ALL_NAMESPACES);
    }

    /**
     * Get the name pool
     *
     * @return the Name Pool that was supplied using the setConfiguration() method
     */

    public NamePool getNamePool() {
        return pipelineConfiguration.getConfiguration().getNamePool();
    }

    /**
     * Helper method for subclasses to invoke if required: flatten an array
     */

    protected void flatten(ArrayItem array, Location locationId, int copyNamespaces) throws XPathException {
        for (Sequence<?> member : array.members()) {
            member.iterate().forEachOrFail(it -> append(it, locationId, copyNamespaces));
        }
    }

    /**
     * Helper method for subclasses to invoke if required: decompose an item into a sequence
     * of node events. Note that when this is used, methods such as characters(), comment(),
     * startElement(), and processingInstruction() are responsible for setting previousAtomic to false.
     */

    protected void decompose(Item item, Location locationId, int copyNamespaces) throws XPathException {
        if (item != null) {
            if (item instanceof AtomicValue || item instanceof ExternalObject) {
                if (previousAtomic) {
                    characters(" ", locationId, 0);
                }
                characters(item.getStringValueCS(), locationId, 0);
                previousAtomic = true;
            } else if (item instanceof ArrayItem) {
                flatten((ArrayItem)item, locationId, copyNamespaces);
            } else if (item instanceof Function) {
                String thing = item instanceof MapItem ? "map" : "function item";
                String errorCode = getErrorCodeForDecomposingFunctionItems();
                if (errorCode.startsWith("SENR")) {
                    throw new XPathException("Cannot serialize a " + thing + " using this output method", errorCode, locationId);
                } else {
                    throw new XPathException("Cannot add a " + thing + " to an XDM node tree", errorCode, locationId);
                }
            } else {
                NodeInfo node = (NodeInfo)item;
                if (node instanceof Orphan && ((Orphan) node).isDisableOutputEscaping()) {
                    // see test case doe-0185 - needed for output buffered within try/catch  // TODO: is this still used?
                    characters(item.getStringValueCS(), locationId, ReceiverOptions.DISABLE_ESCAPING);
                    previousAtomic = false;
                } else if (node.getNodeKind() == Type.DOCUMENT) {
                    startDocument(0); // needed to ensure that illegal namespaces or attributes in the content are caught
                    node.iterateAxis(AxisInfo.CHILD).forEachOrFail(
                            child -> append(child, locationId, copyNamespaces)
                    );
                    previousAtomic = false;
                    endDocument();
                } else {
                    if (node.getNodeKind() == Type.ATTRIBUTE && ((SimpleType) node.getSchemaType()).isNamespaceSensitive()) {
                        XPathException err = new XPathException("Cannot copy attributes whose type is namespace-sensitive (QName or NOTATION): " +
                                                                        Err.wrap(node.getDisplayName(), Err.ATTRIBUTE));
                        err.setErrorCode(getPipelineConfiguration().getHostLanguage() == Configuration.XSLT ? "XTTE0950" : "XQTY0086");
                        throw err;
                    }
                    int copyOptions = CopyOptions.TYPE_ANNOTATIONS;
                    if (copyNamespaces == ReceiverOptions.LOCAL_NAMESPACES) {
                        copyOptions |= CopyOptions.LOCAL_NAMESPACES;
                    } else if (copyNamespaces == ReceiverOptions.ALL_NAMESPACES) {
                        copyOptions |= CopyOptions.ALL_NAMESPACES;
                    }
                    ((NodeInfo) item).copy(this, copyOptions, locationId);
                    previousAtomic = false;
                }
            }
        }
    }

    protected String getErrorCodeForDecomposingFunctionItems() {
        boolean isXSLT = getPipelineConfiguration().getHostLanguage() == Configuration.XSLT;
        return isXSLT ? "XTDE0450" : "XQTY0105";
        // Use SENR0001 when doing sequence normalization as per the Serialization spec.
    }

    /**
     * Ask whether this Receiver can handle arbitrary items in its {@link #append} and
     * {@link #append(Item, Location, int)} methods. If it cannot, then calling
     * these methods will raise an exception (typically but not necessarily an
     * {@code UnsupportedOperationException}). This implementation returns true.
     *
     * @return true if the Receiver is able to handle items supplied to
     *      its {@link #append} and {@link #append(Item, Location, int)} methods. A
     *      receiver that returns true may still reject some kinds of item, for example
     *      it may reject function items.
     */

    public boolean handlesAppend() {
        return true;
    }
}

