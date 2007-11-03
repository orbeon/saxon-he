package net.sf.saxon.javax.xml.xquery;

/**
 * XQJ interfaces reconstructed from version 0.5 documentation
 */
public interface XQMetaData {

    int getMaxExpressionLength();

    int getMaxUserNameLength();

    int getProductMajorVersion();

    int getProductMinorVersion();

    java.lang.String getProductName();

    java.lang.String getProductVersion();

    java.util.Set getSupportedXQueryEncodings();

    java.lang.String getUserName();

    int getXQJMajorVersion();

    int getXQJMinorVersion();

    java.lang.String getXQJVersion();

    boolean isFullAxisFeatureSupported();

    boolean isModuleFeatureSupported();

    boolean isReadOnly();

    boolean isSchemaImportFeatureSupported();

    boolean isSchemaValidationFeatureSupported();

    boolean isSerializationFeatureSupported();

    boolean isStaticTypingExtensionsSupported();

    boolean isStaticTypingFeatureSupported();

    boolean isTransactionSupported();

    boolean isUserDefinedXMLSchemaTypeSupported();

    boolean isXQueryEncodingDeclSupported();

    boolean isXQueryEncodingSupported(String encoding);

    boolean isXQueryXSupported();

    boolean wasCreatedFromJDBCConnection();
}

//
// This interface definition is transcribed from the Public Draft Specification (version 0.9)
// of the XQuery API for Java (XQJ) 1.0 Specification, available at
// http://jcp.org/aboutJava/communityprocess/pr/jsr225/index.html
//
// Copyright 2003, 2006, 2007 Oracle. All rights reserved.
// For licensing conditions, see the above specification
//
