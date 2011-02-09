package net.sf.saxon.style;
import net.sf.saxon.PreparedStylesheet;
import net.sf.saxon.lib.Validation;
import net.sf.saxon.lib.NamespaceConstant;
import net.sf.saxon.expr.Expression;
import net.sf.saxon.expr.Literal;
import net.sf.saxon.expr.instruct.*;
import net.sf.saxon.om.*;
import net.sf.saxon.trace.Location;
import net.sf.saxon.trans.XPathException;
import net.sf.saxon.tree.linked.DocumentImpl;
import net.sf.saxon.tree.linked.LinkedTreeBuilder;
import net.sf.saxon.type.SchemaType;

import javax.xml.transform.TransformerException;


/**
* This class represents a literal result element in the style sheet
* (typically an HTML element to be output). <br>
* It is also used to represent unknown top-level elements, which are ignored.
*/

public class LiteralResultElement extends StyleElement {

    private int resultNameCode;
    private int[] attributeNames;
    private Expression[] attributeValues;
    private int numberOfAttributes;
    private boolean toplevel;
    private int[] namespaceCodes;
    private AttributeSet[] attributeSets;
    private SchemaType schemaType = null;
    private int validation = Validation.STRIP;
    private boolean inheritNamespaces = true;

    /**
    * Determine whether this type of element is allowed to contain a sequence constructor
    * @return true: yes, it may contain a sequence constructor
    */

    public boolean mayContainSequenceConstructor() {
        return true;
    }

    /**
     * Specify that this is an instruction
     */

    public boolean isInstruction() {
        return true;
    }

    /**
    * Process the attribute list
    */

    public void prepareAttributes() throws XPathException {

        // Process the values of all attributes. At this stage we deal with attribute
        // values (especially AVTs), but we do not apply namespace aliasing to the
        // attribute names.

        AttributeCollection atts = getAttributeList();
        int num = atts.getLength();

        if (num == 0) {
            numberOfAttributes = 0;
        } else {
            NamePool namePool = getNamePool();
            attributeNames = new int[num];
            attributeValues = new Expression[num];
            numberOfAttributes = 0;

            for (int i=0; i<num; i++) {

                int anameCode = atts.getNameCode(i);
                short attURIcode = namePool.getURICode(anameCode);

                if (attURIcode==NamespaceConstant.XSLT_CODE) {
                    int fp = anameCode & NamePool.FP_MASK;

                    if (fp == StandardNames.XSL_USE_ATTRIBUTE_SETS) {
                        // deal with this later
                    } else if (fp == StandardNames.XSL_DEFAULT_COLLATION) {
                    	// already dealt with
                    } else if (fp == StandardNames.XSL_EXTENSION_ELEMENT_PREFIXES) {
                    	// already dealt with
                    } else if (fp == StandardNames.XSL_EXCLUDE_RESULT_PREFIXES) {
                    	// already dealt with
                    } else if (fp == StandardNames.XSL_VERSION) {
                        // already dealt with
                    } else if (fp == StandardNames.XSL_XPATH_DEFAULT_NAMESPACE) {
                        // already dealt with
                    } else if (fp == StandardNames.XSL_TYPE) {
                        // deal with this later
                    } else if (fp == StandardNames.XSL_USE_WHEN) {
                        // already dealt with
                    } else if (fp == StandardNames.XSL_VALIDATION) {
                        // deal with this later
                    } else if (fp == StandardNames.XSL_INHERIT_NAMESPACES) {
                        String inheritAtt = atts.getValue(i);
                        if (inheritAtt.equals("yes")) {
                            inheritNamespaces = true;
                        } else if (inheritAtt.equals("no")) {
                            inheritNamespaces = false;
                        } else {
                            compileError("The xsl:inherit-namespaces attribute has permitted values (yes, no)", "XTSE0020");
                        }
                    } else {
                        compileError("Unknown XSL attribute " + namePool.getDisplayName(anameCode), "XTSE0805");
                    }
                } else {
                    attributeNames[numberOfAttributes] = anameCode;
                    Expression exp = makeAttributeValueTemplate(atts.getValue(i));
                    attributeValues[numberOfAttributes] = exp;
                    numberOfAttributes++;
                }
            }

            // now shorten the arrays if necessary. This is necessary if there are [xsl:]-prefixed
            // attributes that weren't copied into the arrays.

            if (numberOfAttributes < attributeNames.length) {

                int[] attributeNames2 = new int[numberOfAttributes];
                System.arraycopy(attributeNames, 0, attributeNames2, 0, numberOfAttributes);
                attributeNames = attributeNames2;

                Expression[] attributeValues2 = new Expression[numberOfAttributes];
                System.arraycopy(attributeValues, 0, attributeValues2, 0, numberOfAttributes);
                attributeValues = attributeValues2;
            }
        }
    }

    /**
    * Validate that this node is OK
     * @param decl
     */

    public void validate(Declaration decl) throws XPathException {

        toplevel = (getParent() instanceof XSLStylesheet);

        resultNameCode = getNameCode();

        NamePool namePool = getNamePool();
        short elementURICode = namePool.getURICode(resultNameCode);

        if (toplevel) {
            // A top-level element can never be a "real" literal result element,
            // but this class gets used for unknown elements found at the top level

            if (elementURICode == 0) {
                compileError("Top level elements must have a non-null namespace URI", "XTSE0130");
            }
        } else {

            processDefaultCollationAttribute(NamespaceConstant.XSLT);

            // Build the list of output namespace nodes. Note we no longer optimize this list.
            // See comments in the 9.1 source code for some history of this decision.

            namespaceCodes = getInScopeNamespaceCodes();

            // Spec bug 5857: if there is no other binding for the default namespace, add an undeclaration
            String defaultNamespace = getURIForPrefix("", true);
            if (defaultNamespace.length() == 0) {
                int[] n2 = new int[namespaceCodes.length + 1];
                System.arraycopy(namespaceCodes, 0, n2, 0, namespaceCodes.length);
                n2[namespaceCodes.length] = 0;
                namespaceCodes = n2;
            }

            // apply any aliases required to create the list of output namespaces

            PrincipalStylesheetModule sheet = getPrincipalStylesheetModule();

            if (sheet.hasNamespaceAliases()) {
                for (int i=0; i<namespaceCodes.length; i++) {
                	// System.err.println("Examining namespace " + namespaceCodes[i]);
                	short scode = (short)(namespaceCodes[i]&0xffff);
                    int ncode = sheet.getNamespaceAlias(scode);
                    if (ncode != -1 && (ncode & 0xffff) != scode) {
                        // apply the namespace alias. Change in 7.3: use the prefix associated
                        // with the new namespace, not the old prefix.
                        namespaceCodes[i] = ncode;
                    }
                }

                // determine if there is an alias for the namespace of the element name

                int ercode = sheet.getNamespaceAlias(elementURICode);
                if ((ercode & 0xffff) != elementURICode) {
                    resultNameCode = namePool.allocate(namePool.getPrefixFromNamespaceCode(ercode),
                                                       namePool.getURIFromNamespaceCode(ercode),
                                                       getLocalPart());
                }
            }

            // deal with special attributes

            String useAttSets = getAttributeValue(StandardNames.XSL_USE_ATTRIBUTE_SETS);
            if (useAttSets != null) {
                attributeSets = getAttributeSets(useAttSets, null);
            }

            validation = getContainingStylesheet().getDefaultValidation();
            String type = getAttributeValue(StandardNames.XSL_TYPE);
            if (type != null) {
                if (!getExecutable().isSchemaAware()) {
                    compileError("The xsl:type attribute is available only with a schema-aware XSLT processor", "XTSE1660");
                }
                schemaType = getSchemaType(type);
                validation = Validation.BY_TYPE;
            }

            String validate = getAttributeValue(StandardNames.XSL_VALIDATION);
            if (validate != null) {
                validation = Validation.getCode(validate);
                if (validation != Validation.STRIP && !getExecutable().isSchemaAware()) {
                    validation = Validation.STRIP;
                    compileError("To perform validation, a schema-aware XSLT processor is needed", "XTSE1660");
                }
                if (validation == Validation.INVALID) {
                    compileError("Invalid value for xsl:validation. " +
                                 "Permitted values are (strict, lax, preserve, strip)", "XTSE0020");
                }
                if (schemaType != null) {
                    compileError("The attributes xsl:type and xsl:validation are mutually exclusive", "XTSE1505");
                }
            }

            // establish the names to be used for all the output attributes;
            // also type-check the AVT expressions

            if (numberOfAttributes > 0) {

                for (int i=0; i<numberOfAttributes; i++) {

                    int anameCode = attributeNames[i];
                    int alias = anameCode;
                    short attURIcode = namePool.getURICode(anameCode);

                    if (attURIcode!=0) {	// attribute has a namespace prefix
                        int newNSCode = sheet.getNamespaceAlias(attURIcode);
                        if ((newNSCode & 0xffff) != attURIcode) {
                            alias = namePool.allocate( namePool.getPrefixFromNamespaceCode(newNSCode),
                                                       namePool.getURIFromNamespaceCode(newNSCode),
                                                       getAttributeList().getLocalName(i));
                        }
                    }

                    attributeNames[i] = alias;
  	                attributeValues[i] = typeCheck(namePool.getDisplayName(alias), attributeValues[i]);
                }
            }

            // remove any namespaces that are on the exclude-result-prefixes list.
            // The namespace is excluded even if it is the namespace of the element or an attribute,
            // though in that case namespace fixup will reinstate it.

            int numberExcluded = 0;
            for (int n=0; n<namespaceCodes.length; n++) {
            	short uricode = (short)(namespaceCodes[n] & 0xffff);
                if (isExcludedNamespace(uricode) && !sheet.isAliasResultNamespace(uricode)) {
                    // exclude it from the output namespace list
                    namespaceCodes[n] = -1;
                    numberExcluded++;
                }
            }

            int count = namespaceCodes.length - numberExcluded;
            if (count == 0) {
                namespaceCodes = null;
            } else {
                int[] newNamespaceCodes = new int[count];
                count = 0;
                for (int i=0; i<namespaceCodes.length; i++) {
                    if (namespaceCodes[i] != -1) {
                        newNamespaceCodes[count++] = namespaceCodes[i];
                    }
                }
                namespaceCodes = newNamespaceCodes;
            }
        }
    }

    /**
    * Validate the children of this node, recursively. Overridden for top-level
    * data elements.
     * @param decl
     */

    protected void validateChildren(Declaration decl) throws XPathException {
        if (!toplevel) {
            super.validateChildren(decl);
        }
    }

	/**
	* Compile code to process the literal result element at runtime
	*/

    public Expression compile(Executable exec, Declaration decl) throws XPathException {
        // top level elements in the stylesheet are ignored
        if (toplevel) return null;

        FixedElement inst = new FixedElement(
                        resultNameCode,
                        namespaceCodes,
                        inheritNamespaces,
                        schemaType,
                        validation);

        inst.setBaseURI(getBaseURI());
        Expression content = compileSequenceConstructor(exec, decl, iterateAxis(Axis.CHILD), true);

        if (numberOfAttributes > 0) {
            for (int i=attributeNames.length - 1; i>=0; i--) {
                FixedAttribute att = new FixedAttribute(
                        attributeNames[i],
                        Validation.STRIP,
                        null,
                        StandardNames.XS_UNTYPED_ATOMIC);
                try {
                    att.setSelect(attributeValues[i], exec.getConfiguration());
                } catch (XPathException err) {
                    compileError(err);
                }
                att.setLocationId(allocateLocationId(getSystemId(), getLineNumber()));
                Expression exp = att;
                if (getConfiguration().isCompileWithTracing()) {
                    TraceExpression trace = new TraceExpression(exp);
                    trace.setLineNumber(getLineNumber());
                    trace.setColumnNumber(-1);
                    trace.setSystemId(getSystemId());
                    trace.setNamespaceResolver(getNamespaceResolver());
                    trace.setConstructType(Location.LITERAL_RESULT_ATTRIBUTE);
                    trace.setLocationId(allocateLocationId(getSystemId(), getLineNumber()));
                    trace.setObjectName(new StructuredQName(getNamePool(), attributeNames[i]));
                    exp = trace;
                }

                if (content == null) {
                    content = exp;
                } else {
                    content = Block.makeBlock(exp, content);
                    content.setLocationId(allocateLocationId(getSystemId(), getLineNumber()));
                }
            }
        }

        if (attributeSets != null) {
            UseAttributeSets use = new UseAttributeSets(attributeSets);
            if (content == null) {
                content = use;
            } else {
                content = Block.makeBlock(use, content);
                content.setLocationId(allocateLocationId(getSystemId(), getLineNumber()));
            }
        }

        if (content == null) {
            content = Literal.makeEmptySequence();
        }
        inst.setContentExpression(content);
        return inst;
    }

    /**
     * Make a top-level literal result element into a stylesheet. This implements
     * the "Simplified Stylesheet" facility.
     * @param pss the PreparedStylesheet (the compiled stylesheet as provided)
     * @return the reconstructed stylesheet with an xsl:stylesheet and xsl:template element added
    */

    public DocumentImpl makeStylesheet(PreparedStylesheet pss)
            throws XPathException {

        // the implementation grafts the LRE node onto a containing xsl:template and
        // xsl:stylesheet

		StyleNodeFactory nodeFactory = pss.getStyleNodeFactory();
        NamePool pool = getNamePool();
        String xslPrefix = getPrefixForURI(NamespaceConstant.XSLT);
        if (xslPrefix==null) {
            String message;
            if (getLocalPart().equals("stylesheet") || getLocalPart().equals("transform")) {
                if (getPrefixForURI(NamespaceConstant.MICROSOFT_XSL) != null) {
                    message = "Saxon is not able to process Microsoft's WD-xsl dialect";
                } else {
                    message = "Namespace for stylesheet element should be " + NamespaceConstant.XSLT;
                }
            } else {
                message = "The supplied file does not appear to be a stylesheet";
            }
            XPathException err = new XPathException(message);
            err.setLocator(this);
            err.setErrorCode("XTSE0150");
            err.setIsStaticError(true);
            //noinspection EmptyCatchBlock
            try {
                pss.reportError(err);
            } catch (TransformerException err2) {
            }
            throw err;

        }

        // check there is an xsl:version attribute (it's mandatory), and copy
        // it to the new xsl:stylesheet element

        String version = getAttributeValue(StandardNames.XSL_VERSION);
        if (version==null) {
            XPathException err = new XPathException("Simplified stylesheet: xsl:version attribute is missing");
            err.setErrorCode("XTSE0150");
            err.setIsStaticError(true);
            err.setLocator(this);
            //noinspection EmptyCatchBlock
            try {
                pss.reportError(err);
            } catch (TransformerException err2) {
            }
            throw err;
        }

        try {
            DocumentImpl oldRoot = (DocumentImpl)getDocumentRoot();
            LinkedTreeBuilder builder = new LinkedTreeBuilder();
            builder.setPipelineConfiguration(pss.getConfiguration().makePipelineConfiguration());
            builder.setNodeFactory(nodeFactory);
            builder.setSystemId(this.getSystemId());

            builder.open();
            builder.startDocument(0);

            int st = StandardNames.XSL_STYLESHEET;
            builder.startElement(st, StandardNames.XS_UNTYPED, 0, 0);
            builder.namespace(NamespaceConstant.XSLT_CODE, 0);
            builder.attribute(pool.allocate("", "", "version"), StandardNames.XS_UNTYPED_ATOMIC, version, 0, 0);
            builder.startContent();

            int te = StandardNames.XSL_TEMPLATE;
            builder.startElement(te, StandardNames.XS_UNTYPED, 0, 0);
            builder.attribute(pool.allocate("", "", "match"), StandardNames.XS_UNTYPED_ATOMIC, "/", 0, 0);
            builder.startContent();

            builder.graftElement(this);

            builder.endElement();
            builder.endElement();
            builder.endDocument();
            builder.close();

            DocumentImpl newRoot = (DocumentImpl)builder.getCurrentRoot();
            newRoot.graftLocationMap(oldRoot);
            return newRoot;
        } catch (XPathException err) {
            //TransformerConfigurationException e = new TransformerConfigurationException(err);
            err.setLocator(this);
            throw err;
        }

    }

    /**
     * Get the type of construct. This will be a constant in
     * class {@link net.sf.saxon.trace.Location}. This method is part of the
     * {@link net.sf.saxon.trace.InstructionInfo} interface
     */

    public int getConstructType() {
        return Location.LITERAL_RESULT_ELEMENT;
    }

    /**
     * Get a name identifying the object of the expression, for example a function name, template name,
     * variable name, key name, element name, etc. This is used only where the name is known statically.
     * If there is no name, the value will be -1.
     * @return the name of the literal result element
     */

    public int getObjectNameCode() {
        return resultNameCode;
    }

    /**
     * Get the value of a particular property of the instruction. This is part of the
     * {@link net.sf.saxon.trace.InstructionInfo} interface for run-time tracing and debugging. The properties
     * available include all the attributes of the source instruction (named by the attribute name):
     * these are all provided as string values.
     * @param name The name of the required property
     * @return  The value of the requested property, or null if the property is not available
     */

    public Object getProperty(String name) {
        if (name.equals("name")) {
            return getDisplayName();
        }
        return null;
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
// The Initial Developer of the Original Code is Michael H. Kay.
//
// Portions created by (your name) are Copyright (C) (your legal entity). All Rights Reserved.
//
// Contributor(s): none.
//
