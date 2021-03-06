package net.sf.saxon.xqj;

import net.sf.saxon.om.Item;

import javax.xml.xquery.XQException;
import javax.xml.xquery.XQItemAccessor;
import javax.xml.xquery.XQItemType;

/**
 * This interface is based on the "CommonHandler" concept defined in early drafts of XQJ. It defines the data
 * conversion routines used by the Saxon XQJ implementation to convert between native Java objects and XDM values.
 * Most applications will use the Saxon-supplied implementation {@link StandardObjectConverter}, but it is possible
 * to supply an alternative implementation using the method {@link SaxonXQDataFactory#setObjectConverter}
 */
public interface ObjectConverter {

    /**
     * Convert an Item to a Java object
     * @param xqItemAccessor the XQJ object representing the item to be converted
     * @return the Java object that results from the conversion
     * @throws XQException
     */

    Object toObject(XQItemAccessor xqItemAccessor) throws XQException;

    /**
     * Convert a Java object to an Item, when no information is available about the required type
     * @param value the supplied Java object. If null is supplied, null is returned.
     * @return the Item that results from the conversion
     * @throws XQException if the Java object cannot be converted to an XQItem
     */

    Item convertToItem(Object value) throws XQException;

   /**
     * Convert a Java object to an Item, when a required type has been specified. Note that Saxon only calls
     * this method when none of the standard conversions defined in the XQJ specification is able to handle
     * the object.
     * @param value the supplied Java object. If null is supplied, null is returned.
     * @param type the required XPath data type
     * @return the Item that results from the conversion
     * @throws XQException if the Java object cannot be converted to an XQItem
     */

    public Item convertToItem(Object value, XQItemType type) throws XQException;
}

// Copyright (c) 2013 Saxonica Limited. All rights reserved.

