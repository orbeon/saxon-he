package net.sf.saxon.javax.xml.xquery;

import javax.xml.namespace.QName;

/**
 * XQJ interfaces reconstructed from version 0.5 documentation
 */
public class XQQueryException extends XQException {

    private QName errorCode;
    private XQSequence errorObject;
    private String moduleURI;
    private int line;
    private int column;
    private int position;
    private XQStackTraceElement[] trace;

    XQQueryException(String message) {
        super(message);
    }

    XQQueryException(String message, QName errorCode) {
        super(message);
        this.errorCode = errorCode;
    }

    XQQueryException(String message, QName errorCode, int line, int column, int position) {
        super(message);
        this.errorCode = errorCode;
        this.line = line;
        this.column = column;
        this.position = position;
    }

    XQQueryException(String message, String vendorCode,
                     QName errorCode, int line, int column, int position) {
        super(message, vendorCode);
        this.errorCode = errorCode;
        this.line = line;
        this.column = column;
        this.position = position;
    }

    XQQueryException(String message, String vendorCode,
                     QName errorCode,
                     int line, int column, int position,
                     String moduleURI, XQSequence errorObject, XQStackTraceElement[] trace) {
        super(message, vendorCode);
        this.errorCode = errorCode;
        this.errorObject = errorObject;
        this.moduleURI = moduleURI;
        this.line = line;
        this.column = column;
        this.position = position;
        this.trace = trace;

    }

    public int getColumnNumber() {
        return column;
    }

    public QName getErrorCode() {
        return errorCode;
    }

    public XQSequence getErrorObject() {
        return errorObject;
    }

    public int getLineNumber() {
        return line;
    }

    public String getModuleURI() {
        return moduleURI;
    }

    int getPosition() {
        return position;
    }

    XQStackTraceElement[] getQueryStackTrace() {
        return trace;
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
