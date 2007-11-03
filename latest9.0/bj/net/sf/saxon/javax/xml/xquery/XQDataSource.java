package net.sf.saxon.javax.xml.xquery;

import java.io.PrintWriter;
import java.util.Properties;

/**
 * XQJ interfaces reconstructed from version 0.5 documentation
 */
public interface XQDataSource {

    XQConnection getConnection() throws XQException;

    XQConnection getConnection(java.sql.Connection con) throws XQException;

    XQConnection getConnection(String username, String password) throws XQException;

    int getLoginTimeout();

    PrintWriter getLogWriter();

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
