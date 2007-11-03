package net.sf.saxon.javax.xml.xquery;

import java.io.InputStream;
import java.io.Reader;

/**
 * XQJ interfaces reconstructed from version 0.5 documentation
 */
public interface XQExpression extends XQDynamicContext {

    void cancel() throws XQException;

    void close() throws XQException;

    void executeCommand(Reader command) throws XQException;

    void executeCommand(String command) throws XQException;

    XQResultSequence executeQuery(InputStream query) throws XQException;

    XQResultSequence executeQuery(Reader query) throws XQException;

    XQResultSequence executeQuery(String query) throws XQException;

    XQStaticContext getStaticContext() throws XQException;

    boolean isClosed();

}

//
// This interface definition is transcribed from the Public Draft Specification (version 0.9)
// of the XQuery API for Java (XQJ) 1.0 Specification, available at
// http://jcp.org/aboutJava/communityprocess/pr/jsr225/index.html
//
// Copyright 2003, 2006, 2007 Oracle. All rights reserved.
// For licensing conditions, see the above specification
//
