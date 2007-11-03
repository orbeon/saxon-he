package net.sf.saxon.javax.xml.xquery;

import javax.xml.namespace.QName;
import java.io.Serializable;

/**
 * XQJ interfaces reconstructed from version 0.9 documentation
 */
public class XQStackTraceVariable implements Serializable {
    private QName qname;
    private String value;

    XQStackTraceVariable(QName qname, java.lang.String value) {
        this.qname = qname;
        this.value = value;
    }

    public QName getQName() {
        return qname;
    }

    public String getValue() {
        return value;
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
