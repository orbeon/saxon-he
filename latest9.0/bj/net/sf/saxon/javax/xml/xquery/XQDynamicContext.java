package net.sf.saxon.javax.xml.xquery;

import org.w3c.dom.Node;
import org.xml.sax.XMLReader;

import javax.xml.namespace.QName;
import javax.xml.transform.Source;
import javax.xml.stream.XMLStreamReader;
import java.util.TimeZone;
import java.io.InputStream;
import java.io.Reader;

/**
 * XQJ interfaces reconstructed from version 0.9 documentation
 */
public interface XQDynamicContext {

    void bindAtomicValue(QName varname, String value, XQItemType type) throws XQException;

    void bindBoolean(QName varname, boolean value, XQItemType type) throws XQException;

    void bindByte(QName varName, byte value, XQItemType type) throws XQException;

    void bindDocument(QName varname, InputStream value, XQItemType type) throws XQException;

    void bindDocument(QName varname, Reader value, XQItemType type) throws XQException;

    void bindDocument(QName varname, Source value, XQItemType type) throws XQException;

    void bindDocument(QName varname, String value, XQItemType type) throws XQException;

    void bindDocument(QName varname, XMLReader value, XQItemType type) throws XQException;

    void bindDocument(QName varname, XMLStreamReader value, XQItemType type) throws XQException;

    void bindDouble(QName varName, double value, XQItemType type) throws XQException;

    void bindFloat(QName varName, float value, XQItemType type) throws XQException;

    void bindInt(QName varName, int value, XQItemType type) throws XQException;

    void bindItem(QName varName, XQItem value) throws XQException;

    void bindLong(QName varName, long value, XQItemType type) throws XQException;

    void bindNode(QName varName, Node value, XQItemType type) throws XQException;

    void bindObject(QName varName, Object value, XQItemType type) throws XQException;

    void bindSequence(QName varName, XQSequence value) throws XQException;

    void bindShort(QName varName, short value, XQItemType type) throws XQException;

    void bindString(QName varName, String value, XQItemType type) throws XQException;

    TimeZone getImplicitTimeZone() throws XQException;

    void setImplicitTimeZone(TimeZone implicitTimeZone) throws XQException;
}

//
// This interface definition is transcribed from the Public Draft Specification (version 0.9)
// of the XQuery API for Java (XQJ) 1.0 Specification, available at
// http://jcp.org/aboutJava/communityprocess/pr/jsr225/index.html
//
// Copyright 2003, 2006, 2007 Oracle. All rights reserved.
// For licensing conditions, see the above specification
//
