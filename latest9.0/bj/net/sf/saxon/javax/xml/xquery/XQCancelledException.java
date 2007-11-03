package net.sf.saxon.javax.xml.xquery;

import javax.xml.namespace.QName;

/**
 * XQJ interfaces reconstructed from version 0.9 documentation
 */
public class XQCancelledException extends XQQueryException {


    XQCancelledException(String message, String vendorCode,
                     XQException nextException, String errorCode,
                     XQItem errorItem, int lineNumber, int position, XQStackTraceElement[] trace) {
        super(message, vendorCode, new QName("", errorCode, ""), lineNumber, -1, position, null, null, trace);
        setNextException(nextException);
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

