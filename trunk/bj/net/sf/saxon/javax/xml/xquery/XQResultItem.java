package net.sf.saxon.javax.xml.xquery;

import net.sf.saxon.javax.xml.xquery.XQConnection;
import net.sf.saxon.javax.xml.xquery.XQException;
import net.sf.saxon.javax.xml.xquery.XQItem;
import net.sf.saxon.javax.xml.xquery.XQItemAccessor;

/**
 * XQJ interfaces reconstructed from version 0.5 documentation
 */
public interface XQResultItem extends XQItem, XQItemAccessor {

    void clearWarnings();

    XQConnection getConnection() throws XQException;

    XQWarning getWarnings() throws XQException;
}
