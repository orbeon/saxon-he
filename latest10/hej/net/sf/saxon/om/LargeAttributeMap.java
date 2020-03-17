////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
// Copyright (c) 2018-2020 Saxonica Limited
// This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
// If a copy of the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
// This Source Code Form is "Incompatible With Secondary Licenses", as defined by the Mozilla Public License, v. 2.0.
////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

package net.sf.saxon.om;


import net.sf.saxon.ma.trie.ImmutableHashTrieMap;
import net.sf.saxon.ma.trie.Tuple2;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * An implementation of AttributeMap suitable for small collections of attributes (say, up to four or five).
 * Searching for a particular attribute involves a sequential search, and adding a new attribute constructs
 * a full copy.
 */

public class LargeAttributeMap implements AttributeMap {

    private ImmutableHashTrieMap<NodeName, AttributeInfo> attributes;
    private int size;
    private List<AttributeInfo> attributeList = null;

    public static LargeAttributeMap EMPTY = new LargeAttributeMap(new ArrayList<>(0));

    public LargeAttributeMap(List<AttributeInfo> atts) {
        this.attributeList = atts;
        this.attributes = ImmutableHashTrieMap.empty();
        this.size = atts.size();
        for (AttributeInfo att : atts) {
            if (attributes.get(att.getNodeName()) != null) {
                throw new IllegalArgumentException("Attribute map contains duplicates");
            }
            attributes = attributes.put(att.getNodeName(), att);
        }
    }

    private LargeAttributeMap(ImmutableHashTrieMap<NodeName, AttributeInfo> attributes, int size) {
        this.attributes = attributes;
        this.size = size;
    }

    /**
     * Return the number of attributes in the map.
     *
     * @return The number of attributes in the map.
     */

    public int size() {
        return size;
    }

    @Override
    public AttributeInfo get(NodeName name) {
        return attributes.get(name);
    }

    @Override
    public AttributeInfo get(String uri, String local) {
        NodeName name = new FingerprintedQName("", uri, local);
        return attributes.get(name);
    }

    public AttributeInfo getByFingerprint(int fingerprint, NamePool namePool) {
        NodeName name = new FingerprintedQName(namePool.getStructuredQName(fingerprint), fingerprint);
        return attributes.get(name);
    }

    @Override
    public AttributeMap put(AttributeInfo att) {
        ImmutableHashTrieMap<NodeName, AttributeInfo> att2 = attributes.put(att.getNodeName(), att);
        int size2 = attributes.get(att.getNodeName()) == null ? size + 1 : size;
        return new LargeAttributeMap(att2, size2);
    }

    @Override
    public AttributeMap remove(NodeName name) {
        if (attributes.get(name) == null) {
            return this;
        } else {
            ImmutableHashTrieMap<NodeName, AttributeInfo> att2 = attributes.remove(name);
            return new LargeAttributeMap(att2, size - 1);
        }
    }

    @Override
    public Iterator<AttributeInfo> iterator() {
        if (attributeList != null) {
            return attributeList.iterator();
        } else {
            Iterator<Tuple2<NodeName, AttributeInfo>> tuples = attributes.iterator();
            return new Iterator<AttributeInfo>() {
                @Override
                public boolean hasNext() {
                    return tuples.hasNext();
                }

                @Override
                public AttributeInfo next() {
                    return tuples.next()._2;
                }
            };
        }
    }

    @Override
    public synchronized List<AttributeInfo> asList() {
        // Memo function
        if (attributeList == null) {
            List<AttributeInfo> list = new ArrayList<>(size);
            forEach(list::add);
            attributeList = list;
        }
        return new ArrayList<>(attributeList);
    }

}

