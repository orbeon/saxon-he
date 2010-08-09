package net.sf.saxon.om;

import net.sf.saxon.event.Builder;
import net.sf.saxon.tinytree.TinyBuilder;
import net.sf.saxon.tinytree.TinyBuilderCondensed;
import net.sf.saxon.tree.TreeBuilder;

import java.io.Serializable;

/**
 * A TreeModel represents an implementation of the Saxon NodeInfo interface, which itself
 * is essentially an implementation of the XDM model defined in W3C specifications (except
 * that Saxon's NodeInfo understands the 13 XPath axes, rather than merely supporting
 * parent and child properties).
 *
 * <p>This class serves two purposes: it acts as a factory for obtaining a Builder which
 * can be used to build trees using this tree model; and it provides static constants
 * that can be used to identify the built-in tree models.</p>
 *
 */

public abstract class TreeModel implements Serializable {

    /**
     * The TinyTree implementation. This is normally the default implementation
     * of the tree model.
     */
    public final static TreeModel TINY_TREE = new TinyTree();

    /**
     * The CondensedTinyTree implementation. This is a variant of the TinyTree that
     * uses less memory but takes a little longer to build. Run-time performance
     * is the same as the TinyTree.
     */
    public final static TreeModel TINY_TREE_CONDENSED = new TinyTreeCondensed();

    /**
     * The LinkedTree. This takes more memory than the TinyTree. The main advantage
     * of this model is that it is updateable: the nodes in a LinkedTree can be modified
     * using XQuery Updates.
     */
    public final static TreeModel LINKED_TREE = new LinkedTree();

    /**
     * Make a Builder to construct an instance of this tree model from a stream of events
     * @return a newly created Builder
     */
    
    public abstract Builder makeBuilder();

    /**
     * Get the integer constant used to identify this tree model in some legacy interfaces
     * @return an integer constant used to identify the model, for example {@link Builder#TINY_TREE}
     */

    public int getSymbolicValue() {
        return Builder.UNSPECIFIED_TREE_MODEL;
    }

    /**
     * Get the tree model corresponding to a given integer constant
     * @param symbolicValue one of the constants {@link Builder#TINY_TREE},
     * {@link Builder#TINY_TREE_CONDENSED}, or {@link Builder#LINKED_TREE}
     * @return the corresponding TreeModel
     */

    public static TreeModel getTreeModel(int symbolicValue) {
        switch (symbolicValue) {
            case Builder.TINY_TREE:
                return TreeModel.TINY_TREE;
            case Builder.TINY_TREE_CONDENSED:
                return TreeModel.TINY_TREE_CONDENSED;
            case Builder.LINKED_TREE:
                return TreeModel.LINKED_TREE;
            default:
                throw new IllegalArgumentException("tree model " + symbolicValue);
        }
    }

    /**
     * Ask whether this tree model supports updating (that is, whether the nodes
     * in the constructed tree will implement {@link MutableNodeInfo}, which is necessary
     * if they are to support XQuery Update. This method can be overridden in subclasses;
     * the default implementation returns false.
     * @return true if the tree model implementation supports updating, that is, if its
     * nodes support the MutableNodeInfo interface.
     */

    public boolean isMutable() {
        return false;
    }

    private static class TinyTree extends TreeModel {
        public Builder makeBuilder() {
            return new TinyBuilder();
        }
        public int getSymbolicValue() {
            return Builder.TINY_TREE;
        }
    }

    private static class TinyTreeCondensed extends TreeModel {
        public Builder makeBuilder() {
            return new TinyBuilderCondensed();
        }
        public int getSymbolicValue() {
            return Builder.TINY_TREE_CONDENSED;
        }
    }

    private static class LinkedTree extends TreeModel {
        public Builder makeBuilder() {
            return new TreeBuilder();
        }
        public int getSymbolicValue() {
            return Builder.LINKED_TREE;
        }
        public boolean isMutable() {
            return true;
        }
    }


}

//
// The contents of this file are subject to the Mozilla Public License Version
// 1.0 (the "License");
// you may not use this file except in compliance with the License. You may
// obtain a copy of the
// License at http://www.mozilla.org/MPL/
//
// Software distributed under the License is distributed on an "AS IS" basis,
// WITHOUT WARRANTY OF ANY KIND, either express or implied.
// See the License for the specific language governing rights and limitations
// under the License.
//
// The Original Code is: all this file.
//
// The Initial Developer of the Original Code is Michael Kay,
//
// Portions created by (your name) are Copyright (C) (your legal entity). All
// Rights Reserved.
//
// Contributor(s): none.
//



