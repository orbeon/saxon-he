////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
// Copyright (c) 2018 Saxonica Limited.
// This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
// If a copy of the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
// This Source Code Form is "Incompatible With Secondary Licenses", as defined by the Mozilla Public License, v. 2.0.
////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

package net.sf.saxon.event;

import net.sf.saxon.expr.parser.ExplicitLocation;
import net.sf.saxon.expr.parser.Location;
import net.sf.saxon.om.*;
import net.sf.saxon.trans.XPathException;
import net.sf.saxon.tree.linked.LinkedTreeBuilder;
import net.sf.saxon.tree.util.Orphan;
import net.sf.saxon.type.SchemaType;
import net.sf.saxon.type.SimpleType;
import net.sf.saxon.type.Type;

/**
 * The <tt>SequenceWriter</tt> is used when writing a sequence of items, for
 * example, when {@code xsl:variable} is used with content and an "as" attribute. The {@code SequenceWriter}
 * builds the sequence; the concrete subclass is responsible for deciding what to do with the
 * resulting items.
 * <p>
 * Items may be supplied in either "composed" form (by calling the {@link #append(Item)} method,
 * or in "decomposed" form (by a sequence of calls representing XML push events: {@link #startDocument(int)},
 * {@link #startElement(NodeName, SchemaType, Location, int)}, and so on. When items are supplied
 * in decomposed form, a tree will be built, and the resulting document or element node is then
 * written to the sequence in the same way as if it were supplied directly as a {@link NodeInfo} item.
 * <p>
 * This class is not used to build temporary trees. For that, the {@link ComplexContentOutputter}
 * is used. The {@code ComplexContentOutputter} provides richer functionality than this class:
 * it detects illegal sequences of events, detects duplicate or inconsistent attributes and namespaces,
 * performs namespace fixup, allocates prefixes where necessary, and detects where it is necessary
 * to call {@link #startContent()} to mark the end of a start tag. By contrast, this class should
 * only be used where it is known that the sequence of events is complete and consistent.</p>
 */

public abstract class SequenceWriter extends SequenceReceiver {
    private Receiver outputter = null;
    private TreeModel treeModel = null;
    private Builder builder = null;
    private int level = 0;
    private boolean inStartTag = false;

    public SequenceWriter(/*@NotNull*/ PipelineConfiguration pipe) {
        super(pipe);
        //System.err.println("SequenceWriter init");
    }

    /**
     * Abstract method to be supplied by subclasses: output one item in the sequence.
     *
     * @param item the item to be written to the sequence
     * @throws net.sf.saxon.trans.XPathException
     *          if any failure occurs while writing the item
     */

    public abstract void write(Item item) throws XPathException;

    @Override
    public void startDocument(int properties) throws XPathException {
        if (outputter == null) {
            createTree((properties & ReceiverOptions.MUTABLE_TREE) != 0);
        }
        if (level++ == 0) {
            outputter.startDocument(properties);
        }
    }

    @Override
    public void setUnparsedEntity(String name, String systemID, String publicID) throws XPathException {
        if (builder != null) {
            builder.setUnparsedEntity(name, systemID, publicID);
        }
    }

    /**
     * Create a (skeletal) tree to hold a document or element node.
     *
     * @param mutable set to true if the tree is required to support in-situ updates (other that the initial
     *                sequential writing of nodes to construct the tree)
     * @throws net.sf.saxon.trans.XPathException
     *          if any error occurs while creating the tree
     */
    private void createTree(boolean mutable) throws XPathException {
        PipelineConfiguration pipe = getPipelineConfiguration();

        if (treeModel != null) {
            builder = treeModel.makeBuilder(pipe);
        } else if (pipe.getController() != null) {
            if (mutable) {
                TreeModel model = pipe.getController().getModel();
                if (model.isMutable()) {
                    builder = pipe.getController().makeBuilder();
                } else {
                    builder = new LinkedTreeBuilder(pipe);
                }
            } else {
                builder = pipe.getController().makeBuilder();
            }
        } else {
            TreeModel model = getConfiguration().getParseOptions().getModel();
            builder = model.makeBuilder(pipe);
        }

        builder.setPipelineConfiguration(pipe);
        builder.setSystemId(systemId);
        builder.setBaseURI(systemId);
        builder.setTiming(false);
        builder.setUseEventLocation(false);

        NamespaceReducer reducer = new NamespaceReducer(builder);

        outputter = new ComplexContentOutputter(reducer);

        outputter.setSystemId(systemId);
        outputter.setPipelineConfiguration(pipe);
        outputter.open();
    }

    /**
     * Get the tree model that will be used for creating trees when events are written to the sequence
     * @return the tree model, if one has been set using setTreeModel(); otherwise null
     */

    public TreeModel getTreeModel() {
        return treeModel;
    }

    /**
     * Set the tree model that will be used for creating trees when events are written to the sequence
     * @param treeModel the tree model to be used. If none has been set, the default tree model for the configuration
     * is used, unless a mutable tree is required and the default tree model is not mutable, in which case a linked
     * tree is used.
     */

    public void setTreeModel(TreeModel treeModel) {
        this.treeModel = treeModel;
    }

    @Override
    public void endDocument() throws XPathException {
        if (--level == 0) {
            outputter.endDocument();
            outputter = null;
            NodeInfo doc = builder.getCurrentRoot();
            // add the constructed document to the result sequence
            append(doc, ExplicitLocation.UNKNOWN_LOCATION, ReceiverOptions.ALL_NAMESPACES);
        }
        previousAtomic = false;
    }

    @Override
    public void startElement(NodeName elemName, SchemaType typeCode, Location location, int properties) throws XPathException {

        if (inStartTag) {
            startContent();
        }

        if (outputter == null) {
            createTree((properties & ReceiverOptions.MUTABLE_TREE) != 0);
        }
        //System.err.println("SEQUENCE_WRITER startElement " + this);
        outputter.startElement(elemName, typeCode, location, properties);
        level++;
        inStartTag = true;
        previousAtomic = false;
    }

    @Override
    public void endElement() throws XPathException {
        if (inStartTag) {
            startContent();
        }
        //System.err.println("SEQUENCE_WRITER endElement " + this);
        outputter.endElement();
        if (--level == 0) {
            outputter.close();
            outputter = null;
            NodeInfo element = builder.getCurrentRoot();
            append(element, ExplicitLocation.UNKNOWN_LOCATION, ReceiverOptions.ALL_NAMESPACES);
        }
        previousAtomic = false;
    }

    @Override
    public void namespace(NamespaceBindingSet namespaceBindings, int properties)
            throws XPathException {
        if (level == 0) {
            for (NamespaceBinding ns : namespaceBindings) {
                Orphan o = new Orphan(getConfiguration());
                o.setNodeKind(Type.NAMESPACE);
                o.setNodeName(new NoNamespaceName(ns.getPrefix()));
                o.setStringValue(ns.getURI());
                append(o, ExplicitLocation.UNKNOWN_LOCATION, ReceiverOptions.ALL_NAMESPACES);
            }
        } else {
            outputter.namespace(namespaceBindings, properties);
        }
        previousAtomic = false;
    }

    @Override
    public void attribute(NodeName attName, SimpleType typeCode, CharSequence value, Location locationId, int properties)
            throws XPathException {
        if (level == 0) {
            Orphan o = new Orphan(getConfiguration());
            o.setNodeKind(Type.ATTRIBUTE);
            o.setNodeName(attName);
            o.setStringValue(value);
            o.setTypeAnnotation(typeCode);
            append(o, locationId, ReceiverOptions.ALL_NAMESPACES);
        } else {
            outputter.attribute(attName, typeCode, value, locationId, properties);
        }
        previousAtomic = false;
    }

    @Override
    public void startContent() throws XPathException {
        inStartTag = false;
        outputter.startContent();
        previousAtomic = false;
    }

    @Override
    public void characters(CharSequence s, Location locationId, int properties) throws XPathException {
        if (level == 0) {
            Orphan o = new Orphan(getConfiguration());
            o.setNodeKind(Type.TEXT);
            o.setStringValue(s.toString());
            write(o);
        } else {
            if (s.length() > 0) {
                if (inStartTag) {
                    startContent();
                }
                outputter.characters(s, locationId, properties);
            }
        }
        previousAtomic = false;
    }

    @Override
    public void comment(CharSequence comment, Location locationId, int properties) throws XPathException {
        if (inStartTag) {
            startContent();
        }
        if (level == 0) {
            Orphan o = new Orphan(getConfiguration());
            o.setNodeKind(Type.COMMENT);
            o.setStringValue(comment);
            write(o);
        } else {
            outputter.comment(comment, locationId, properties);
        }
        previousAtomic = false;
    }

    @Override
    public void processingInstruction(String target, CharSequence data, Location locationId, int properties) throws XPathException {
        if (inStartTag) {
            startContent();
        }
        if (level == 0) {
            Orphan o = new Orphan(getConfiguration());
            o.setNodeName(new NoNamespaceName(target));
            o.setNodeKind(Type.PROCESSING_INSTRUCTION);
            o.setStringValue(data);
            write(o);
        } else {
            outputter.processingInstruction(target, data, locationId, properties);
        }
        previousAtomic = false;
    }

    @Override
    public void close() throws XPathException {
        previousAtomic = false;
        if (outputter != null) {
            outputter.close();
        }
    }

    @Override
    public void append(/*@Nullable*/ Item item, Location locationId, int copyNamespaces) throws XPathException {
        if (item != null) {
            if (level == 0) {
                write(item);
                previousAtomic = false;
            } else {
                decompose(item, locationId, copyNamespaces);
            }
        }
    }

    @Override
    public boolean usesTypeAnnotations() {
        return outputter == null || outputter.usesTypeAnnotations();
    }
}

