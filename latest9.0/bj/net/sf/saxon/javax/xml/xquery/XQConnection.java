package net.sf.saxon.javax.xml.xquery;

import java.io.InputStream;
import java.io.Reader;

/**
 * XQJ interfaces reconstructed from version 0.9 documentation
 */
public interface XQConnection extends XQDataFactory {

    void close() throws XQException;

    void commit() throws XQException;

    XQExpression createExpression() throws XQException;

    XQExpression createExpression(XQStaticContext properties) throws XQException;

    boolean getAutoCommit() throws XQException;

    XQMetaData getMetaData() throws XQException;

    XQStaticContext getStaticContext() throws XQException;

    boolean isClosed();

    XQPreparedExpression prepareExpression(InputStream xquery) throws XQException;

    XQPreparedExpression prepareExpression(InputStream xquery, XQStaticContext properties) throws XQException;

    XQPreparedExpression prepareExpression(Reader xquery) throws XQException;

    XQPreparedExpression prepareExpression(Reader xquery, XQStaticContext properties) throws XQException;

    XQPreparedExpression prepareExpression(String xquery) throws XQException;

    XQPreparedExpression prepareExpression(String xquery, XQStaticContext properties) throws XQException;

    void rollback() throws XQException;

    void setAutoCommit(boolean autoCommit) throws XQException;

    void setStaticContext(XQStaticContext properties) throws XQException;


}


//
// This interface definition is transcribed from the Public Draft Specification (version 0.9)
// of the XQuery API for Java (XQJ) 1.0 Specification, available at
// http://jcp.org/aboutJava/communityprocess/pr/jsr225/index.html
//
// Copyright 2003, 2006, 2007 Oracle. All rights reserved.
// For licensing conditions, see the above specification
//
