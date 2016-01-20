////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
// Copyright (c) 2015 Saxonica Limited.
// This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
// If a copy of the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
// This Source Code Form is "Incompatible With Secondary Licenses", as defined by the Mozilla Public License, v. 2.0.
////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

package net.sf.saxon.om;

import net.sf.saxon.Configuration;

/**
 * This is a marker interface used to identify nodes that contain a namepool fingerprint. Although all nodes
 * are capable of returning a fingerprint, some (notably DOM, XOM, and JDOM nodes) need to calculate it on demand.
 * A node that implements this interface indicates that obtaining the fingerprint for use in name comparisons
 * is more efficient than using the URI and local name.
 */

public interface FingerprintedNode extends NodeInfo {

    /**
     * Get the configuration used to build the tree containing this node.
     *
     * @return the Configuration
     * @since 8.4. Moved in 9.7 from NodeInfo to FingerprintedNode
     */

    public Configuration getConfiguration();

    /**
     * Get the NamePool that holds the namecode for this node
     *
     * @return the namepool
     * @since 8.4. Moved in 9.7 from NodeInfo to FingerprintedNode
     */

    public NamePool getNamePool();

    /**
     * Get the value of the attribute with a given fingerprint.
     *
     * @param fp the fingerprint of the required attribute
     * @return the string value of the attribute if present, or null if absent
     */
    public String getAttributeValue(int fp);

    /**
     * Get fingerprint. The fingerprint is a coded form of the expanded name
     * of the node: two nodes
     * with the same name code have the same namespace URI and the same local name.
     * The fingerprint contains no information about the namespace prefix. For a name
     * in the null namespace, the fingerprint is the same as the name code.
     *
     * @return an integer fingerprint; two nodes with the same fingerprint have
     * the same expanded QName. For unnamed nodes (text nodes, comments, document nodes,
     * and namespace nodes for the default namespace), returns -1.
     * @since 8.4
     */

    public int getFingerprint();

    /**
     * Get nameCode. The fingerprint is a coded form of the expanded name
     * of the node: two nodes
     * with the same name code have the same namespace URI and the same local name.
     * The nameCode contains additional information about the namespace prefix. For a name
     * in the null namespace, the fingerprint is the same as the name code.
     *
     * @return an integer name code; two nodes with the same name code have
     * the same prefix, local name, and namespace URI. For unnamed nodes (text nodes, comments, document nodes,
     * and namespace nodes for the default namespace), returns -1.
     * @since 8.4
     */

    public int getNameCode();
}

