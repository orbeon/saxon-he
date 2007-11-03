package net.sf.saxon.javax.xml.xquery;

import org.w3c.dom.Node;
import org.xml.sax.ContentHandler;

import javax.xml.stream.XMLStreamReader;
import javax.xml.transform.Result;
import java.io.OutputStream;
import java.io.Writer;
import java.net.URI;
import java.util.Properties;

/**
 * XQJ interface reconstructed from version 0.9 documentation
 */
public interface XQItemAccessor {

    String getAtomicValue() throws XQException;

    boolean getBoolean() throws XQException;

    byte getByte() throws XQException;

    double getDouble() throws XQException;

    float getFloat() throws XQException;

    int getInt() throws XQException;

    XMLStreamReader getItemAsStream() throws XQException;

    String getItemAsString(Properties props) throws XQException;

    XQItemType getItemType() throws XQException;

    long getLong() throws XQException;

    Node getNode() throws XQException;

    URI getNodeUri() throws XQException;

    Object getObject() throws XQException;

    short getShort() throws XQException;

    boolean instanceOf(XQItemType type) throws XQException;

    void writeItem(OutputStream os, Properties props) throws XQException;

    void writeItem(Writer ow, Properties props) throws XQException;

    void writeItemToResult(Result result) throws XQException;

    void writeItemToSAX(ContentHandler saxHandler) throws XQException;
}

//
// This interface definition is transcribed from the Public Draft Specification (version 0.9)
// of the XQuery API for Java (XQJ) 1.0 Specification, available at
// http://jcp.org/aboutJava/communityprocess/pr/jsr225/index.html
//
// Copyright 2003, 2006, 2007 Oracle. All rights reserved.
// For licensing conditions, see the above specification
//
