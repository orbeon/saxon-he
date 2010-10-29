package net.sf.saxon.event;

import net.sf.saxon.om.NodeInfo;

/**
 * A BuilderMonitor can be inserted into a pipeline immediately in front of a Builder. During tree construction,
 * the method markNextNode() can be called to request that the next node to be created is treated specially by
 * remembering the current position on the tree; on completion of the tree construction, the method getMarkedNode()
 * can be called to return the NodeInfo that was created immediately after calling markNextNode().
 */
public abstract class BuilderMonitor extends ProxyReceiver {

    /**
     * Indicate that the next node to be created will be of a given type, and request the monitor to remember
     * the identity of this node.
     * @param nodeKind the kind of node that will be created next
     */

    public abstract void markNextNode(int nodeKind);

    /**
     * On completion of tree building, get the node that was marked using markNextNode().
     * @return the marked node, or null if none was marked
     */
    
    public abstract NodeInfo getMarkedNode();
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



