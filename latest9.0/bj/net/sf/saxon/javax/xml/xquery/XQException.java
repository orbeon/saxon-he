package net.sf.saxon.javax.xml.xquery;

/**
 * XQJ interfaces reconstructed from version 0.9 documentation
 */
public class XQException extends Exception {

    private String vendorCode;
    private XQException nextException;

    public XQException(String message) {
        super(message);
    }

    public XQException(String message, String vendorCode) {
        super(message);
        this.vendorCode = vendorCode;
    }

    XQException getNextException() {
        return nextException;
    }

    java.lang.String getVendorCode() {
        return vendorCode;
    }

    void setNextException(XQException next) {
        nextException = next;
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
