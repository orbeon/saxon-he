package net.sf.saxon.javax.xml.xquery;

import java.io.PrintWriter;
import java.util.Properties;

/**
 * XQJ interfaces reconstructed from version 0.9 documentation
 */
public interface ConnectionPoolXQDataSource {

    int getLoginTimeout() throws XQException;

    PrintWriter getLogWriter() throws XQException;

    PooledXQConnection getPooledConnection() throws XQException;

    PooledXQConnection getPooledConnection(String user, String password) throws XQException;

    String getProperty(String name) throws XQException;

    String[] getSupportedPropertyNames();

    void setLoginTimeout(int seconds) throws XQException;

    void setLogWriter(PrintWriter out) throws XQException;

    void setProperties(Properties props) throws XQException;

    void setProperty(String name, String value) throws XQException;


}

//
// This interface definition is transcribed from the Public Draft Specification (version 0.9)
// of the XQuery API for Java (XQJ) 1.0 Specification, available at
// http://jcp.org/aboutJava/communityprocess/pr/jsr225/index.html
//
// Copyright 2003, 2006, 2007 Oracle. All rights reserved.
// For licensing conditions, see the above specification
//

