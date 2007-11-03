package net.sf.saxon.javax.xml.xquery;

import javax.xml.namespace.QName;

/**
 * XQJ interfaces reconstructed from version 0.5 documentation
 */
public interface XQPreparedExpression extends XQDynamicContext {

    void cancel() throws XQException;

    void close() throws XQException;

    XQResultSequence executeQuery() throws XQException;

    QName[] getAllExternalVariables() throws XQException;

    QName[] getAllUnboundExternalVariables() throws XQException;

    XQStaticContext getStaticContext() throws XQException;

    XQSequenceType getStaticResultType() throws XQException;

    XQSequenceType getStaticVariableType(QName name) throws XQException;

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
