package net.sf.saxon.javax.xml.xquery;

/**
 * XQJ interfaces reconstructed from version 0.9 documentation
 */
public class XQConnectionEvent {

    PooledXQConnection con;
    XQException ex;

    public XQConnectionEvent(PooledXQConnection con) {
        this.con = con;
    }

    public XQConnectionEvent(PooledXQConnection con, XQException ex) {
        this.con = con;
        this.ex = ex;
    }

    public XQException getXQException() {
        return ex;
    }
}

//
// This interface definition is transcribed from the Public Draft Specification (version 0.9)
// of the XQuery API for Java (XQJ) 1.0 Specification, available at
// http://jcp.org/aboutJava/communityprocess/pr/jsr225/index.html
//
// Copyright 2003, 2006, 2007 Oracle. All rights reserved.
// For licensing conditions, see the above specification
//

