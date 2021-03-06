////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
// Copyright (c) 2015 Saxonica Limited.
// This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
// If a copy of the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
// This Source Code Form is "Incompatible With Secondary Licenses", as defined by the Mozilla Public License, v. 2.0.
////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

package net.sf.saxon.expr;

import net.sf.saxon.Configuration;
import net.sf.saxon.expr.parser.Location;
import net.sf.saxon.expr.parser.RetainedStaticContext;
import net.sf.saxon.functions.FunctionLibrary;
import net.sf.saxon.om.NamespaceResolver;
import net.sf.saxon.om.StructuredQName;
import net.sf.saxon.trans.DecimalFormatManager;
import net.sf.saxon.trans.KeyManager;
import net.sf.saxon.trans.XPathException;
import net.sf.saxon.type.ItemType;

import java.util.Set;

/**
 * A StaticContext contains the information needed while an expression or pattern
 * is being parsed. The information is also sometimes needed at run-time.
 */

public interface StaticContext {

    /**
     * Get the system configuration
     *
     * @return the Saxon configuration
     */

    Configuration getConfiguration();

    /**
     * Get information about the containing package (unit of compilation)
     * @return the package data
     */

    PackageData getPackageData();

    /**
     * Construct a dynamic context for early evaluation of constant subexpressions.
     *
     * @return a newly constructed dynamic context
     */

    XPathContext makeEarlyEvaluationContext();

    /**
     * Construct a RetainedStaticContext, which extracts information from this StaticContext
     * to provide the subset of static context information that is potentially needed
     * during expression evaluation
     * @return a RetainedStaticContext object: either a newly created one, or one that is
     * reused from a previous invocation.
     */

    RetainedStaticContext makeRetainedStaticContext();

    /**
     * Get the containing location. This is location information relevant to an expression or query
     * as a whole. In the case of an XPath expression held in a node of an XML document, it will
     * provide the location of that node. In the case of a query held in a file, it contains the
     * location of the file (in its systemId property). The method does NOT provide fine-grained
     * location information for each contained subexpression. The location that is returned should
     * be immutable for the duration of parsing of an XPath expression or query.
     *
     * @return the containing location
     */

    Location getContainingLocation();

    /**
     * Issue a compile-time warning.
     * @param message The warning message. This should not contain any prefix such as "Warning".
     * @param locator the location of the construct in question. May be null.
     */

    void issueWarning(String message, Location locator);

    /**
     * Get the System ID of the container of the expression. This is the containing
     * entity (file) and is therefore useful for diagnostics. Use getBaseURI() to get
     * the base URI, which may be different.
     *
     * @return the system ID
     */

    String getSystemId();

    /**
     * Get the static base URI, for resolving any relative URI's used
     * in the expression.
     * Used by the document(), doc(), resolve-uri(), and base-uri() functions.
     * May return null if the base URI is not known.
     *
     * @return the static base URI, or null if not known
     */

    String getStaticBaseURI();

    /**
     * Bind a variable used in this element to the XSLVariable element in which it is declared
     *
     * @param qName The name of the variable
     * @return an expression representing the variable reference, This will often be
     *         a {@link VariableReference}, suitably initialized to refer to the corresponding variable declaration,
     *         but in general it can be any expression which returns the variable's value when evaluated.
     * @throws XPathException if the variable cannot be bound (has not been declared)
     */

    Expression bindVariable(StructuredQName qName) throws XPathException;

    /**
     * Get the function library containing all the in-scope functions available in this static
     * context
     *
     * @return the function library
     */

    FunctionLibrary getFunctionLibrary();

    /**
     * Get the name of the default collation.
     *
     * @return the name of the default collation; or the name of the codepoint collation
     *         if no default collation has been defined
     */

    String getDefaultCollationName();

    /**
     * Get the default XPath namespace for elements and types
     *
     * @return the default namespace, or NamespaceConstant.NULL for the non-namespace
     */

    String getDefaultElementNamespace();

    /**
     * Get the default function namespace
     *
     * @return the default namespace for function names
     */

    String getDefaultFunctionNamespace();

    /**
     * Determine whether backwards compatibility mode is used
     *
     * @return true if 1.0 compaibility mode is in force.
     */

    boolean isInBackwardsCompatibleMode();

    /**
     * Ask whether a Schema for a given target namespace has been imported. Note that the
     * in-scope element declarations, attribute declarations and schema types are the types registered
     * with the (schema-aware) configuration, provided that their namespace URI is registered
     * in the static context as being an imported schema namespace. (A consequence of this is that
     * within a Configuration, there can only be one schema for any given namespace, including the
     * null namespace).
     *
     * @param namespace the target namespace in question
     * @return true if the given namespace has been imported
     */

    boolean isImportedSchema(String namespace);

    /**
     * Get the set of imported schemas
     *
     * @return a Set, the set of URIs representing the target namespaces of imported schemas,
     *         using the zero-length string to denote the "null" namespace.
     */

    Set<String> getImportedSchemaNamespaces();

    /**
     * Get a namespace resolver to resolve the namespaces declared in this static context.
     *
     * @return a namespace resolver.
     */

    NamespaceResolver getNamespaceResolver();

    /**
     * Get the required type of the context item. If no type has been explicitly declared for the context
     * item, an instance of AnyItemType (representing the type item()) is returned.
     *
     * @return the required type of the context item
     * @since 9.3
     */

    ItemType getRequiredContextItemType();

    /**
     * Get a DecimalFormatManager to resolve the names of decimal formats used in calls
     * to the format-number() function.
     *
     * @return the decimal format manager for this static context, or null if no named decimal
     *         formats are available in this environment.
     * @since 9.2
     */

    DecimalFormatManager getDecimalFormatManager();

    /**
     * Get the XPath language level supported, as an integer (being the actual version
     * number times ten). The levels supported are 20, 30, and 31. The default is 20.
     * If running XQuery 1.0, the value is 20; if running XQuery 3.0, it is 30.
     * if running XQuery 3.1, it is 31.
     *
     * @return the XPath language level; the return value will be either 20, 30, or 31
     * @since 9.7
     */

    int getXPathVersion();

    /**
     * Get the KeyManager, containing definitions of keys available for use.
     *
     * @return the KeyManager. This is used to resolve key names, both explicit calls
     *         on key() used in XSLT, and system-generated calls on key() which may
     *         also appear in XQuery and XPath
     */

    KeyManager getKeyManager();

}

