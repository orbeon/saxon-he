package net.sf.saxon.functions;

/**
 * This is a marker interface representing an abstract superclass of JavaExtensionFunctionFactory
 * and DotNetExtensionFunctionFactory. These play equivalent roles in the system: that is, they
 * are responsible for determining how the QNames of extension functions are bound to concrete
 * implementation classes; but they do not share the same interface.
 *
 * <p>This interface was introduced in Saxon 8.9. Prior to that, <code>ExtensionFunctionFactory</code>
 * was a concrete class - the class now named <code>JavaExtensionFunctionFactory</code>.
 */

public interface ExtensionFunctionFactory {
}

// Copyright (c) 2009 Saxonica Limited. All rights reserved.
