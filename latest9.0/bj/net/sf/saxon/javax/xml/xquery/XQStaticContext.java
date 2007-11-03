package net.sf.saxon.javax.xml.xquery;

/**
 * XQJ interfaces reconstructed from version 0.5 documentation
 */
public interface XQStaticContext {

    void declareNamespace(String prefix, String uri) throws XQException;

    String getBaseURI();

    int getBindingMode();

    int getBoundarySpacePolicy();

    int getConstructionMode();

    XQItemType getContextItemStaticType();

    int getCopyNamespacesModeInherit();

    int getCopyNamespacesModePreserve();

    String getDefaultCollation();

    String getDefaultElementTypeNamespace();

    String getDefaultFunctionNamespace();

    int getDefaultOrderForEmptySequences();

    int getHoldability();

    String[] getNamespacePrefixes();

    String getNamespaceURI(String prefix) throws XQException;

    int getOrderingMode();

    int getQueryLanguageTypeAndVersion();

    int getQueryTimeout();

    int getScrollability();

    void 	setBaseURI(String baseUri) throws XQException;
    void 	setBindingMode(int bindingMode) throws XQException;
    void 	setBoundarySpacePolicy(int policy) throws XQException;
    void 	setConstructionMode(int mode) throws XQException;
    void 	setContextItemStaticType(XQItemType contextItemType) throws XQException;
    void 	setCopyNamespacesModeInherit(int mode) throws XQException;
    void 	setCopyNamespacesModePreserve(int mode) throws XQException;
    void 	setDefaultCollation(java.lang.String uri) throws XQException;
    void 	setDefaultElementTypeNamespace(java.lang.String uri) throws XQException;
    void 	setDefaultFunctionNamespace(java.lang.String uri) throws XQException;
    void 	setDefaultOrderForEmptySequences(int order) throws XQException;
    void 	setHoldability(int holdability) throws XQException;
    void 	setOrderingMode(int mode) throws XQException;
    void 	setQueryLanguageTypeAndVersion(int langType) throws XQException;
    void 	setQueryTimeout(int seconds) throws XQException;
    void 	setScrollability(int scrollability) throws XQException;
}

//
// This interface definition is transcribed from the Public Draft Specification (version 0.9)
// of the XQuery API for Java (XQJ) 1.0 Specification, available at
// http://jcp.org/aboutJava/communityprocess/pr/jsr225/index.html
//
// Copyright 2003, 2006, 2007 Oracle. All rights reserved.
// For licensing conditions, see the above specification
//
