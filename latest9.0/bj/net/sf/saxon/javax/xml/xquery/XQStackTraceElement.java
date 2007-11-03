package net.sf.saxon.javax.xml.xquery;

import javax.xml.namespace.QName;
import java.io.Serializable;

/**
 * XQJ interfaces reconstructed from version 0.9 documentation
 */
public class XQStackTraceElement implements Serializable {
    String moduleURI;
    int lineNumber;
    int columnNumber;
    int position;
    QName function;
    XQStackTraceVariable[] variables;

    public XQStackTraceElement(java.lang.String moduleURI, int line, int column, int position,
                               QName function, XQStackTraceVariable[] variables) {
        this.moduleURI = moduleURI;
        this.lineNumber = line;
        this.columnNumber = column;
        this.position = position;
        this.function = function;
        this.variables = variables;
    }

    public int getColumnNumber() {
        return columnNumber;
    }

    public QName getFunctionQName() {
        return function;
    }

    public int getLineNumber() {
        return lineNumber;
    }

    public String getModuleURI() {
        return moduleURI;
    }

    public int getPosition() {
        return position;
    }

    public XQStackTraceVariable[] getVariables() {
        return variables;
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
