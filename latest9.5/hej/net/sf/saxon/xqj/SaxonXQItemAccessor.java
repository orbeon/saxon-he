package net.sf.saxon.xqj;

import net.sf.saxon.om.Item;

import javax.xml.xquery.XQException;

/**
 * All Saxon implementations of XQItemAccessor must implement this interface
 */
public interface SaxonXQItemAccessor {

    /**
     * Get the current item, in the form of a Saxon Item object. This allows access to non-XQJ methods
     * to manipulate the item, which will not necessarily be stable from release to release. The resulting
     * Item will be an instance of either {@link net.sf.saxon.om.NodeInfo} or {@link net.sf.saxon.value.AtomicValue}.
     * @return the current item
     */

    /*@Nullable*/ public Item getSaxonItem() throws XQException;
}

// Copyright (c) 2013 Saxonica Limited. All rights reserved.

