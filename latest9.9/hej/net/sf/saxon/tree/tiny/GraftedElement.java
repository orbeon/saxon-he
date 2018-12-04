////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
// Copyright (c) 2018 Saxonica Limited.
// This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
// If a copy of the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
// This Source Code Form is "Incompatible With Secondary Licenses", as defined by the Mozilla Public License, v. 2.0.
////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

package net.sf.saxon.tree.tiny;


import net.sf.saxon.event.Receiver;
import net.sf.saxon.expr.parser.Location;
import net.sf.saxon.om.*;
import net.sf.saxon.pattern.AnyNodeTest;
import net.sf.saxon.pattern.NodeTest;
import net.sf.saxon.trans.XPathException;
import net.sf.saxon.tree.iter.AxisIterator;
import net.sf.saxon.tree.util.NamespaceIterator;
import net.sf.saxon.tree.util.Navigator;
import net.sf.saxon.tree.wrapper.VirtualCopy;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * This class represents an element node in a TinyTree that is actually a reference to an element
 * node held elsewhere. Some properties of the node come from the containing TinyTree (for example,
 * document number, base URI, etc) while others are actually properties of the external node. Notably,
 * the parent and siblings of the node are found in the containing tree, while the children and
 * descendants (and attributes) are found in the external tree.
 */

public class GraftedElement extends TinyElementImpl {

    private NodeInfo externalNode;
    private VirtualCopy externalNodeCopy;
    private NamespaceBinding[] declaredNamespaces;

    /**
     * Created a grafted element node
     * @param tree the host tree that is to contain a reference to an external node
     * @param nodeNr the node number at which the external reference is to appear
     * @param externalNode the external node, in some other tree
     * @param copyNamespaces indicates whether namespaces in the external tree are
     *                       retained in the virtual copy.
     */

    public GraftedElement(TinyTree tree, int nodeNr, NodeInfo externalNode, boolean copyNamespaces) {
        super(tree, nodeNr);
        this.externalNode = externalNode;
        this.externalNodeCopy = VirtualCopy.makeVirtualCopy(externalNode, this);
        this.externalNodeCopy.setDropNamespaces(!copyNamespaces);
    }

    public NodeInfo getExternalNode() {
        return externalNode;
    }

    @Override
    public boolean hasChildNodes() {
        return externalNodeCopy.hasChildNodes();
    }

    @Override
    public CharSequence getStringValueCS() {
        return externalNode.getStringValueCS();
    }

    @Override
    public String getStringValue() {
        return externalNode.getStringValue();
    }

    @Override
    public AtomicSequence atomize() throws XPathException {
        return externalNode.atomize();
    }

    @Override
    public void copy(Receiver receiver, int copyOptions, Location location) throws XPathException {
        externalNodeCopy.copy(receiver, copyOptions, location);
    }

    @Override
    public AxisIterator iterateAxis(byte axisNumber) {
        return iterateAxis(axisNumber, AnyNodeTest.getInstance());
    }

    @Override
    public AxisIterator iterateAxis(byte axisNumber, NodeTest nodeTest) {
        switch (axisNumber) {
            case AxisInfo.ANCESTOR:
                return super.iterateAxis(axisNumber, nodeTest);

            case AxisInfo.ANCESTOR_OR_SELF:
                return super.iterateAxis(axisNumber, nodeTest);

            case AxisInfo.ATTRIBUTE:
                return externalNodeCopy.iterateAxis(axisNumber, nodeTest);

            case AxisInfo.CHILD:
                return externalNodeCopy.iterateAxis(axisNumber, nodeTest);

            case AxisInfo.DESCENDANT:
                return externalNodeCopy.iterateAxis(axisNumber, nodeTest);

            case AxisInfo.DESCENDANT_OR_SELF:
                return externalNodeCopy.iterateAxis(axisNumber, nodeTest);

            case AxisInfo.FOLLOWING:
                return super.iterateAxis(axisNumber, nodeTest);

            case AxisInfo.FOLLOWING_SIBLING:
                return super.iterateAxis(axisNumber, nodeTest);

            case AxisInfo.NAMESPACE:
                return externalNodeCopy.iterateAxis(axisNumber, nodeTest);

            case AxisInfo.PARENT:
                return super.iterateAxis(axisNumber, nodeTest);

            case AxisInfo.PRECEDING:
                return super.iterateAxis(axisNumber, nodeTest);

            case AxisInfo.PRECEDING_SIBLING:
                return super.iterateAxis(axisNumber, nodeTest);

            case AxisInfo.SELF:
                return Navigator.filteredSingleton(this, nodeTest);

            case AxisInfo.PRECEDING_OR_ANCESTOR:
                return super.iterateAxis(axisNumber, nodeTest);

            default:
                throw new IllegalArgumentException("Unknown axis number " + axisNumber);
        }
    }

    /**
     * Get the value of the attribute with a given fingerprint.
     *
     * @param fp the fingerprint of the required attribute
     * @return the string value of the attribute if present, or null if absent
     */

    public String getAttributeValue(int fp) {
        NodeInfo actual = externalNodeCopy.getOriginalNode();
        if (actual instanceof TinyElementImpl) {
            return ((TinyElementImpl) actual).getAttributeValue(fp);
        } else {
            StructuredQName qn = getNamePool().getStructuredQName(fp);
            return actual.getAttributeValue(qn.getURI(), qn.getLocalPart());
        }
    }

    @Override
    public NamespaceBinding[] getDeclaredNamespaces(NamespaceBinding[] buffer) {
        if (declaredNamespaces == null) {
            List<NamespaceBinding> bindings = new ArrayList<>();
            Iterator<NamespaceBinding> iter = NamespaceIterator.iterateNamespaces(externalNode);
            boolean foundDefaultNamespace = false;
            NamespaceBinding binding;
            while (iter.hasNext()) {
                binding = iter.next();
                if (binding.getPrefix().isEmpty()) {
                    foundDefaultNamespace = true;
                }
                bindings.add(binding);
            }
            if (!foundDefaultNamespace) {
                String defaultNamespace = new InscopeNamespaceResolver(getParent()).getURIForPrefix("", true);
                if (defaultNamespace != null && !defaultNamespace.isEmpty()) {
                    bindings.add(NamespaceBinding.DEFAULT_UNDECLARATION);
                }
            }
            declaredNamespaces = bindings.toArray(NamespaceBinding.EMPTY_ARRAY);
        }
        return declaredNamespaces;
    }
}

