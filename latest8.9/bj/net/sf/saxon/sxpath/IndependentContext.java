package net.sf.saxon.sxpath;
import net.sf.saxon.Configuration;
import net.sf.saxon.expr.Container;
import net.sf.saxon.expr.VariableReference;
import net.sf.saxon.instruct.SlotManager;
import net.sf.saxon.om.*;
import net.sf.saxon.sort.IntHashMap;
import net.sf.saxon.trans.StaticError;
import net.sf.saxon.trans.XPathException;
import net.sf.saxon.type.Type;
import net.sf.saxon.value.QNameValue;

import javax.xml.transform.SourceLocator;
import java.io.Serializable;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Set;

/**
* An IndependentContext provides a context for parsing an XPath expression appearing
* in a context other than a stylesheet.
 *
 * <p>This class is used in a number of places where freestanding XPath expressions occur.
 * These include the native Saxon XPath API, the .NET XPath API, XPath expressions used
 * in XML Schema identity constraints, and XPath expressions supplied to saxon:evaluate().
 * It is not used by the JAXP XPath API (though it shares code with that API through
 * the common superclass AbstractStaticContext).</p>
 *
 * <p>This class currently provides no mechanism for binding user-defined functions.</p>
*/

public class IndependentContext extends AbstractStaticContext
                                implements XPathStaticContext, NamespaceResolver, Serializable, Container {

	private HashMap namespaces = new HashMap(10);
	private IntHashMap variables = new IntHashMap(20);
    //private SlotManager stackFrameMap;
    private NamespaceResolver externalResolver = null;
    private Set importedSchemaNamespaces = Collections.EMPTY_SET;

    public IndependentContext() {
        this(new Configuration());
    }

    /**
	* Create an IndependentContext using a specific Configuration
	*/

	public IndependentContext(Configuration config) {
        super();
        setConfiguration(config);
        //stackFrameMap = config.makeSlotManager();
		clearNamespaces();
        setDefaultFunctionLibrary();
    }

    /**
     * Create a copy of this IndependentContext. All aspects of the context are copied
     * except for declared variables.
     */

    public IndependentContext copy() {
        IndependentContext ic = new IndependentContext(getConfiguration());
        ic.setBaseURI(getBaseURI());
        ic.setLocationMap(getLocationMap());
        ic.setDefaultElementNamespace(getDefaultElementNamespace());
        ic.setDefaultFunctionNamespace(getDefaultFunctionNamespace());
        ic.namespaces = new HashMap(namespaces);
        ic.variables = new IntHashMap(10);
        ic.importedSchemaNamespaces = importedSchemaNamespaces;
        ic.externalResolver = externalResolver;
        return ic;
    }

	/**
	* Declare a namespace whose prefix can be used in expressions
	* @param prefix The namespace prefix. Must not be null. Supplying "" sets the
     * default element namespace.
	* @param uri The namespace URI. Must not be null.
	*/

	public void declareNamespace(String prefix, String uri) {
	    if (prefix==null) {
	        throw new NullPointerException("Null prefix supplied to declareNamespace()");
	    }
	    if (uri==null) {
	        throw new NullPointerException("Null namespace URI supplied to declareNamespace()");
	    }
        if ("".equals(prefix)) {
            setDefaultElementNamespace(uri);
        } else {
		    namespaces.put(prefix, uri);
		    getNamePool().allocateNamespaceCode(prefix, uri);
        }
	}

	/**
	 * Clear all the declared namespaces, except for the standard ones (xml, xslt, saxon, xdt).
     * This also resets the default element namespace to the "null" namespace
	*/

	public void clearNamespaces() {
	    namespaces.clear();
		declareNamespace("xml", NamespaceConstant.XML);
		declareNamespace("xsl", NamespaceConstant.XSLT);
		declareNamespace("saxon", NamespaceConstant.SAXON);
		declareNamespace("xs", NamespaceConstant.SCHEMA);
        declareNamespace("", "");
	}

	/**
	* Clear all the declared namespaces, including the standard ones (xml, xslt, saxon).
     * Leave only the XML namespace and the default namespace (xmlns="").
     * This also resets the default element namespace to the "null" namespace.
	*/

	public void clearAllNamespaces() {
	    namespaces.clear();
		declareNamespace("xml", NamespaceConstant.XML);
		declareNamespace("", "");
	}

	/**
	* Declares all the namespaces that are in-scope for a given node, removing all previous
    * namespace declarations.
	* In addition, the standard namespaces (xml, xslt, saxon) are declared. This method also
    * sets the default element namespace to be the same as the default namespace for this node.
	* @param node The node whose in-scope namespaces are to be used as the context namespaces.
	* If the node is an attribute, text node, etc, then the namespaces of its parent element are used.
	*/

	public void setNamespaces(NodeInfo node) {
	    namespaces.clear();
        int kind = node.getNodeKind();
        if (kind == Type.ATTRIBUTE || kind == Type.TEXT ||
                kind == Type.COMMENT || kind == Type.PROCESSING_INSTRUCTION ||
                kind == Type.NAMESPACE) {
            node = node.getParent();
        }
        if (node == null) {
            return;
        }

	    AxisIterator iter = node.iterateAxis(Axis.NAMESPACE);
	    while (true) {
	        NodeInfo ns = (NodeInfo)iter.next();
            if (ns == null) {
                return;
            }
            String prefix = ns.getLocalPart();
            if ("".equals(prefix)) {
                setDefaultElementNamespace(ns.getStringValue());
            } else {
	            declareNamespace(ns.getLocalPart(), ns.getStringValue());
            }
	    }
	}

    /**
     * Set an external namespace resolver. If this is set, then all resolution of namespace
     * prefixes is delegated to the external namespace resolver, and namespaces declared
     * individually on this IndependentContext object are ignored.
     */

    public void setNamespaceResolver(NamespaceResolver resolver) {
        this.externalResolver = resolver;
    }

    /**
     * Get the stack frame map containing the slot number allocations for the variables declared
     * in this static context
     */

//    public SlotManager getStackFrameMap() {
//        return stackFrameMap;
//    }

    /**
     * Declare a variable. A variable must be declared before an expression referring
     * to it is compiled. The initial value of the variable will be the empty sequence
     * @param qname The name of the variable
     * @return an XPathVariable object representing information about the variable that has been
     * declared.
    */

    public XPathVariable declareVariable(QNameValue qname) {
        XPathVariable var = XPathVariable.make(qname, getConfiguration());
        int fingerprint = qname.allocateNameCode(getNamePool()) & 0xfffff;
        int slot = variables.size();
        var.setSlotNumber(slot);
        variables.put(fingerprint, var);
        return var;
    }

    /**
     * Get the slot number allocated to a particular variable
     * @return the slot number, or -1 if the variable has not been declared
     */

    public int getSlotNumber(QNameValue qname) {
        int fingerprint = qname.allocateNameCode(getNamePool()) & 0xfffff;
        XPathVariable var = (XPathVariable)variables.get(fingerprint);
        if (var == null) {
            return -1;
        }
        return var.getLocalSlotNumber();
    }


    /**
    * Issue a compile-time warning. This method is used during XPath expression compilation to
    * output warning conditions. The default implementation writes the message to System.err. To
    * change the destination of messages, create a subclass of StandaloneContext that overrides
    * this method.
    */

    public void issueWarning(String s, SourceLocator locator) {
        System.err.println(s);
    }

    /**
     * Get the URI for a prefix, using the declared namespaces as
     * the context for namespace resolution. The default namespace is NOT used
     * when the prefix is empty.
     * This method is provided for use by the XPath parser.
     * @param prefix The prefix
     * @throws net.sf.saxon.trans.XPathException if the prefix is not declared
    */

    public String getURIForPrefix(String prefix) throws XPathException {
        String uri = getURIForPrefix(prefix, false);
    	if (uri==null) {
    		throw new StaticError("Prefix " + prefix + " has not been declared");
    	}
    	return uri;
    }

    public NamespaceResolver getNamespaceResolver() {
        if (externalResolver != null) {
            return externalResolver;
        } else {
            return this;
        }
    }

    /**
     * Get the namespace URI corresponding to a given prefix. Return null
     * if the prefix is not in scope.
     * @param prefix the namespace prefix
     * @param useDefault true if the default namespace is to be used when the
     * prefix is ""
     * @return the uri for the namespace, or null if the prefix is not in scope.
     * Return "" if the prefix maps to the null namespace.
     */

    public String getURIForPrefix(String prefix, boolean useDefault) {
        if (externalResolver != null) {
            return externalResolver.getURIForPrefix(prefix, useDefault);
        }
        if (prefix.equals("") && !useDefault) {
            if (useDefault) {
                return getDefaultElementNamespace();
            } else {
                return "";
            }
        } else {
    	    return (String)namespaces.get(prefix);
        }
    }

    /**
     * Get an iterator over all the prefixes declared in this namespace context. This will include
     * the default namespace (prefix="") and the XML namespace where appropriate
     */

    public Iterator iteratePrefixes() {
        if (externalResolver != null) {
            return externalResolver.iteratePrefixes();
        } else {
            return namespaces.keySet().iterator();
        }
    }

    /**
    * Bind a variable used in an XPath Expression to the XSLVariable element in which it is declared.
    * This method is provided for use by the XPath parser, and it should not be called by the user of
    * the API, or overridden, unless variables are to be declared using a mechanism other than the
    * declareVariable method of this class.
    */

    public VariableReference bindVariable(int fingerprint) throws StaticError {
        XPathVariable var = (XPathVariable)variables.get(fingerprint);
        if (var==null) {
            throw new StaticError("Undeclared variable in a standalone expression");
        } else {
            return new VariableReference(var);
        }
    }


    /**
     * Get a Stack Frame Map containing definitions of all the declared variables. This must be a newly
     * created object that the caller is free to modify by adding additional variables, without affecting
     * the static context itself.
     */

    public SlotManager getStackFrameMap() {
        SlotManager map = getConfiguration().makeSlotManager();
        for (Iterator v = variables.valueIterator(); v.hasNext();) {
            XPathVariable var = (XPathVariable)v.next();
            QNameValue name = var.getVariableQName();
            int nameCode = getConfiguration().getNamePool().allocate(
                    name.getPrefix(), name.getNamespaceURI(), name.getLocalName());
            map.allocateSlotNumber(nameCode & NamePool.FP_MASK);
        }
        return map;
    }



    public boolean isImportedSchema(String namespace) {
        return importedSchemaNamespaces.contains(namespace);
    }

    /**
     * Get the set of imported schemas
     *
     * @return a Set, the set of URIs representing the names of imported schemas
     */

    public Set getImportedSchemaNamespaces() {
        return importedSchemaNamespaces;
    }

    public void setImportedSchemaNamespaces(Set namespaces) {
        importedSchemaNamespaces = namespaces;
    }


}

//
// The contents of this file are subject to the Mozilla Public License Version 1.0 (the "License");
// you may not use this file except in compliance with the License. You may obtain a copy of the
// License at http://www.mozilla.org/MPL/
//
// Software distributed under the License is distributed on an "AS IS" basis,
// WITHOUT WARRANTY OF ANY KIND, either express or implied.
// See the License for the specific language governing rights and limitations under the License.
//
// The Original Code is: all this file.
//
// The Initial Developer of the Original Code is Michael H. Kay
//
// Portions created by (your name) are Copyright (C) (your legal entity). All Rights Reserved.
//
// Contributor(s): none.
//
