////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
// Copyright (c) 2013 Saxonica Limited.
// This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
// If a copy of the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
// This Source Code Form is "Incompatible With Secondary Licenses", as defined by the Mozilla Public License, v. 2.0.
////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

package net.sf.saxon.style;


import net.sf.saxon.Configuration;
import net.sf.saxon.PreparedStylesheet;
import net.sf.saxon.event.LocationProvider;
import net.sf.saxon.expr.*;
import net.sf.saxon.expr.instruct.*;
import net.sf.saxon.expr.parser.*;
import net.sf.saxon.expr.sort.SortKeyDefinition;
import net.sf.saxon.functions.Current;
import net.sf.saxon.lib.NamespaceConstant;
import net.sf.saxon.om.*;
import net.sf.saxon.pattern.*;
import net.sf.saxon.trace.InstructionInfo;
import net.sf.saxon.trace.Location;
import net.sf.saxon.trans.Err;
import net.sf.saxon.trans.SaxonErrorCode;
import net.sf.saxon.trans.XPathException;
import net.sf.saxon.tree.iter.AxisIterator;
import net.sf.saxon.tree.linked.ElementImpl;
import net.sf.saxon.tree.linked.TextImpl;
import net.sf.saxon.tree.util.FastStringBuffer;
import net.sf.saxon.tree.util.NamespaceIterator;
import net.sf.saxon.tree.util.Navigator;
import net.sf.saxon.type.*;
import net.sf.saxon.value.DecimalValue;
import net.sf.saxon.value.SequenceType;
import net.sf.saxon.value.Whitespace;
import org.xml.sax.Locator;

import javax.xml.transform.SourceLocator;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.*;

/**
 * Abstract superclass for all element nodes in the stylesheet.
 * <p>Note: this class implements Locator. The element retains information about its own location
 * in the stylesheet, which is useful when an XSLT static error is found.</p>
 */

public abstract class StyleElement extends ElementImpl
implements Locator, Container, InstructionInfo {

	/*@Nullable*/ protected String[] extensionNamespaces = null;        // a list of URIs
	private String[] excludedNamespaces = null;           // a list of URIs
	protected DecimalValue version = null;                 // the effective version of this element
	protected ExpressionContext staticContext = null;
	protected XPathException validationError = null;
	protected int reportingCircumstances = REPORT_ALWAYS;
	protected String defaultXPathNamespace = null;
	protected String defaultCollationName = null;
    protected boolean expandText = false;
	private boolean explaining = false;
	// true if saxon:explain="yes"
	private StructuredQName objectName;
	// for instructions that define an XSLT named object, the name of that object
	private XSLStylesheet containingStylesheet;

	// Conditions under which an error is to be reported

	public static final int REPORT_ALWAYS = 1;
	public static final int REPORT_UNLESS_FORWARDS_COMPATIBLE = 2;
	public static final int REPORT_IF_INSTANTIATED = 3;
	public static final int REPORT_UNLESS_FALLBACK_AVAILABLE = 4;

	protected int actionsCompleted = 0;
	public static final int ACTION_VALIDATE = 1;
	public static final int ACTION_COMPILE = 2;
	public static final int ACTION_TYPECHECK = 4;
	public static final int ACTION_OPTIMIZE = 8;
	public static final int ACTION_FIXUP = 16;
	public static final int ACTION_PROCESS_ATTRIBUTES = 32;


	/**
	 * Constructor
	 */

	public StyleElement() {
	}

    /*@NotNull*/
    public  Executable getExecutable() {
        return getPreparedStylesheet();
    }

    /**
	 * Get the LocationProvider allowing location identifiers to be resolved.
	 */

	public LocationProvider getLocationProvider() {
        return getPreparedStylesheet().getLocationMap();
	}

	/**
	 * Get the static context for expressions on this element
	 * @return the static context
	 */

	public StaticContext getStaticContext() {
		if (staticContext == null) {
			staticContext = new ExpressionContext(this);
		}
		return staticContext;
	}

	/**
	 * Get the granularity of the container.
	 * @return 0 for a temporary container created during parsing; 1 for a container
	 *         that operates at the level of an XPath expression; 2 for a container at the level
	 *         of a global function or template
	 */

	public int getContainerGranularity() {
		return 1;
	}

	/**
	 * Make an expression visitor
	 * @return the expression visitor
	 */

	public ExpressionVisitor makeExpressionVisitor() {
        return ExpressionVisitor.make(getStaticContext(), getPreparedStylesheet());
	}

	/**
	 * Determine whether saxon:explain has been set to "yes"
	 * @return true if saxon:explain has been set to "yes" on this element
	 */

	protected boolean isExplaining() {
		return explaining;
	}

	/**
	 * Make this node a substitute for a temporary one previously added to the tree. See
	 * StyleNodeFactory for details. "A node like the other one in all things but its class".
	 * Note that at this stage, the node will not yet be known to its parent, though it will
	 * contain a reference to its parent; and it will have no children.
	 * @param temp the element which this one is substituting for
	 */

	public void substituteFor(StyleElement temp) {
		setRawParent(temp.getRawParent());
		setAttributeList(temp.getAttributeList());
		setNamespaceList(temp.getNamespaceList());
		setNameCode(temp.getNameCode());
		setRawSequenceNumber(temp.getRawSequenceNumber());
		extensionNamespaces = temp.extensionNamespaces;
		excludedNamespaces = temp.excludedNamespaces;
		version = temp.version;
		staticContext = temp.staticContext;
		validationError = temp.validationError;
		reportingCircumstances = temp.reportingCircumstances;
		//lineNumber = temp.lineNumber;
	}

	/**
	 * Set a validation error. This is an error detected during construction of this element on the
	 * stylesheet, but which is not to be reported until later.
	 * @param reason        the details of the error
	 * @param circumstances a code identifying the circumstances under which the error is to be reported
	 */

	public void setValidationError(TransformerException reason,
			int circumstances) {
		validationError = XPathException.makeXPathException(reason);
		reportingCircumstances = circumstances;
	}

	/**
	 * Ask whether this node is an instruction. The default implementation says it isn't.
	 * @return true if this element is an instruction
	 */

	public boolean isInstruction() {
		return false;
	}

	/**
	 * Ask whether this node is a declaration, that is, a permitted child of xsl:stylesheet
	 * (including xsl:include and xsl:import). The default implementation returns false
	 * @return true if the element is a permitted child of xsl:stylesheet or xsl:transform
	 */

	public boolean isDeclaration() {
		return false;
	}

	/**
	 * Determine the type of item returned by this instruction (only relevant if
	 * it is an instruction). Default implementation returns Type.ITEM, indicating
	 * that we don't know, it might be anything. Returns null in the case of an element
	 * such as xsl:sort or xsl:variable that can appear in a sequence constructor but
	 * contributes nothing to the result sequence.
	 * @return the item type returned
	 */

	protected ItemType getReturnedItemType() {
		return AnyItemType.getInstance();
	}

	/**
	 * Get the most general type of item returned by the children of this instruction
	 * @return the lowest common supertype of the item types returned by the children
	 */

	protected ItemType getCommonChildItemType() {
		final TypeHierarchy th = getConfiguration().getTypeHierarchy();
		ItemType t = ErrorType.getInstance();
		AxisIterator children = iterateAxis(AxisInfo.CHILD);
		while (true) {
			NodeInfo next = children.next();
			if (next == null) {
				return t;
			}
			if (next instanceof StyleElement) {
				ItemType ret = ((StyleElement)next).getReturnedItemType();
				if (ret != null) {
					t = Type.getCommonSuperType(t, ret, th);
				}
			} else {
				t = Type.getCommonSuperType(t, NodeKindTest.TEXT, th);
			}
			if (t == AnyItemType.getInstance()) {
				return t;       // no point looking any further
			}
		}
	}

	/**
	 * Mark tail-recursive calls on templates and functions.
	 * For most instructions, this returns false.
	 * @return true if one or more tail calls were identified
	 */

	protected boolean markTailCalls() {
		return false;
	}

	/**
	 * Determine whether this type of element is allowed to contain a sequence constructor
	 * @return true if this instruction is allowed to contain a sequence constructor
	 */

	protected boolean mayContainSequenceConstructor() {
		return false;
	}

	/**
	 * Determine whether this type of element is allowed to contain an xsl:fallback
	 * instruction. Note that this is only relevant if the element is an instruction.
	 * @return true if this element is allowed to contain an xsl:fallback
	 */

	protected boolean mayContainFallback() {
		return mayContainSequenceConstructor();
	}

	/**
	 * Determine whether this type of element is allowed to contain an xsl:param element
	 * @param attName if null, the method tests whether an xsl:param child is allowed.
	 *                If non-null, it tests whether an xsl:param child with the given attribute name is allowed
	 * @return true if this element is allowed to contain an xsl:param
	 */

	protected boolean mayContainParam(String attName) {
		return false;
	}

	/**
	 * Get the containing XSLStylesheet element
	 * @return the XSLStylesheet element representing the outermost element of the containing
	 *         stylesheet module. Exceptionally, return null if there is no containing XSLStylesheet element
	 */

	public XSLStylesheet getContainingStylesheet() {
		if (containingStylesheet == null) {
			if (this instanceof XSLStylesheet) {
				containingStylesheet = (XSLStylesheet)this;
			} else {
				NodeInfo parent = getParent();
				if (parent instanceof StyleElement) {
					containingStylesheet = ((StyleElement)parent).getContainingStylesheet();
				} else {
					// this can happen when early errors are detected in a simplified stylesheet,
					return null;
				}
			}
		}
		return containingStylesheet;
	}

	/**
	 * Make a structured QName, using this Element as the context for namespace resolution.
     * If the name is unprefixed, the default namespace is <b>not</b> used.
	 * @param lexicalQName The lexical QName as written, in the form "[prefix:]localname". The name must have
	 *                     already been validated as a syntactically-correct QName. Leading and trailing whitespace
	 *                     will be trimmed. If XSLT 3.0 is enabled, then the EQName syntax "Q{uri}local" is also
     *                     accepted.
	 * @return the StructuredQName representation of this lexical QName
	 * @throws XPathException     if the qname is not a lexically-valid QName, or if the name
	 *                            is in a reserved namespace.
	 * @throws NamespaceException if the prefix of the qname has not been declared
	 */

	public final StructuredQName makeQName(String lexicalQName)
	throws XPathException, NamespaceException {

		StructuredQName qName;
		try {
			qName = StructuredQName.fromLexicalQName(lexicalQName, false,
                    isXslt30Processor(), getConfiguration().getNameChecker(), this);
		} catch (XPathException e) {
			e.setIsStaticError(true);
			String code = e.getErrorCodeLocalPart();
			if ("FONS0004".equals(code)) {
				e.setErrorCode("XTSE0280");
			} else if ("FOCA0002".equals(code)) {
				e.setErrorCode("XTSE0020");
			} else if (code == null) {
				e.setErrorCode("XTSE0020");
			}
			throw e;
		}
		if (NamespaceConstant.isReserved(qName.getURI())) {
			XPathException err = new XPathException("Namespace prefix " +
					qName.getPrefix() + " refers to a reserved namespace");
			err.setIsStaticError(true);
			err.setErrorCode("XTSE0080");
			throw err;
		}
		return qName;
	}


	/**
	 * Make a NamespaceContext object representing the list of in-scope namespaces. This will
	 * be a copy of the namespace context with no references to objects in the stylesheet tree,
	 * so that it can be kept until run-time without locking the tree down in memory.
	 * @return a copy of the namespace context
	 */

	public SavedNamespaceContext makeNamespaceContext() {
		return new SavedNamespaceContext(NamespaceIterator.iterateNamespaces(this));
	}

	/**
	 * Get the namespace context of the instruction.
	 * @return the namespace context. This method does not make a copy of the namespace context,
	 *         so a reference to the returned NamespaceResolver will lock the stylesheet tree in memory.
	 */

	public NamespaceResolver getNamespaceResolver() {
		return this;
	}

	/**
	 * Process the attributes of this element and all its children
	 * @throws XPathException in the event of a static error being detected
	 */

	protected void processAllAttributes() throws XPathException {
		processDefaultCollationAttribute();
		staticContext = new ExpressionContext(this);
		processAttributes();
		AxisIterator kids = iterateAxis(AxisInfo.CHILD);
		while (true) {
			NodeInfo child = kids.next();
			if (child == null) {
				return;
			}
			if (child instanceof StyleElement) {
				((StyleElement)child).processAllAttributes();
				if (((StyleElement)child).explaining) {
					// saxon:explain on any element in a template/function now causes an explanation at the
					// level of the template/function
					explaining = true;
				}
			}
		}
	}

	/**
	 * Process the standard attributes such as [xsl:]default-collation
	 * @param namespace either "" to find the attributes in the null namespace,
	 *                  or NamespaceConstant.XSLT to find them in the XSLT namespace
     * @throws net.sf.saxon.trans.XPathException if any of the standard attributes is incorrect
	 */

	public void processStandardAttributes(String namespace) throws XPathException {
		//processDefaultCollationAttribute(namespace);
		processExtensionElementAttribute(namespace);
		processExcludedNamespaces(namespace);
		processVersionAttribute(namespace);
		processDefaultXPathNamespaceAttribute(namespace);
        processExpandTextAttribute(namespace);
	}

	/**
	 * Get an attribute value given the Clark name of the attribute (that is,
	 * the name in {uri}local format).
	 * @param clarkName the name of the attribute in {uri}local format
	 * @return the value of the attribute if it exists, or null otherwise
	 */

	public String getAttributeValue(String clarkName) {
        NodeName nn = FingerprintedQName.fromClarkName(clarkName);
		return getAttributeValue(nn.getURI(), nn.getLocalPart());
	}

	/**
	 * Process the attribute list for the element. This is a wrapper method that calls
	 * prepareAttributes (provided in the subclass) and traps any exceptions
     * @throws net.sf.saxon.trans.XPathException if a static error is detected
     */

	protected final void processAttributes() throws XPathException {
		try {
			prepareAttributes();
		} catch (XPathException err) {
			compileError(err);
		}
	}

	/**
	 * Check whether an unknown attribute is permitted.
	 * @param nc The name code of the attribute name
	 * @throws XPathException (and reports the error) if this is an attribute
	 *                        that is not permitted on the containing element
	 */

	protected void checkUnknownAttribute(NodeName nc) throws XPathException {

		String attributeURI = nc.getURI();
		String elementURI = getURI();
		String clarkName = nc.getStructuredQName().getClarkName();

		if (clarkName.equals(StandardNames.SAXON_EXPLAIN)) {
			explaining = "yes".equals(getAttributeValue(clarkName));
		}

		if (forwardsCompatibleModeIsEnabled()) {
			// then unknown attributes are permitted and ignored
			return;
		}

		// allow xsl:extension-element-prefixes etc on an extension element

		if (isInstruction() &&
				attributeURI.equals(NamespaceConstant.XSLT) &&
				!(elementURI.equals(NamespaceConstant.XSLT)) &&
				(clarkName.endsWith("}default-collation") ||
						clarkName.endsWith("}xpath-default-namespace") ||
                        clarkName.endsWith("}expand-text") ||
						clarkName.endsWith("}extension-element-prefixes") ||
						clarkName.endsWith("}exclude-result-prefixes") ||
						clarkName.endsWith("}version") ||
						clarkName.endsWith("}use-when"))) {
			return;
		}

		// allow standard attributes on an XSLT element

		if (elementURI.equals(NamespaceConstant.XSLT) &&
				(clarkName.equals(StandardNames.DEFAULT_COLLATION) ||
                        clarkName.equals("expand-text") ||
						clarkName.equals(StandardNames.XPATH_DEFAULT_NAMESPACE) ||
						clarkName.equals(StandardNames.EXTENSION_ELEMENT_PREFIXES) ||
						clarkName.equals(StandardNames.EXCLUDE_RESULT_PREFIXES) ||
						clarkName.equals(StandardNames.VERSION) ||
						clarkName.equals(StandardNames.USE_WHEN) ||
                        clarkName.equals(StandardNames.STATIC))) {
			return;
		}

		if ("".equals(attributeURI) || NamespaceConstant.XSLT.equals(attributeURI)) {
			compileError("Attribute " + Err.wrap(nc.getDisplayName(), Err.ATTRIBUTE) +
					" is not allowed on element " + Err.wrap(getDisplayName(), Err.ELEMENT), "XTSE0090");
		}
	}


	/**
	 * Set the attribute list for the element. This is called to process the attributes (note
	 * the distinction from processAttributes in the superclass).
	 * Must be supplied in a subclass
     * @throws net.sf.saxon.trans.XPathException if a static error is detected
     */

	protected abstract void prepareAttributes() throws XPathException;

	/**
	 * Find the last child instruction of this instruction. Returns null if
	 * there are no child instructions, or if the last child is a text node.
	 * @return the last child instruction, or null if there are no child instructions
	 */

	protected StyleElement getLastChildInstruction() {
		StyleElement last = null;
		AxisIterator kids = iterateAxis(AxisInfo.CHILD);
		while (true) {
			NodeInfo child = kids.next();
			if (child == null) {
				return last;
			}
			if (child instanceof StyleElement) {
				last = (StyleElement)child;
			} else {
				last = null;
			}
		}
	}

	/**
	 * Compile an XPath expression in the context of this stylesheet element
	 * @param expression the source text of the XPath expression
	 * @return the compiled expression tree for the XPath expression
     * @throws net.sf.saxon.trans.XPathException if a static error is detected in the XPath expression
	 */

	public Expression makeExpression(String expression) throws XPathException {
		try {
			return ExpressionTool.make(expression,
					staticContext,
					this, 0, Token.EOF,
					getLineNumber(),
					getPreparedStylesheet().getCompilerInfo().getCodeInjector());
		} catch (XPathException err) {
			err.setLocator(this);
            if (err.isReportableStatically()) {
			    compileError(err);
            }
			ErrorExpression erexp = new ErrorExpression(err);
			erexp.setLocationId(allocateLocationId(getSystemId(), getLineNumber()));
			erexp.setContainer(this);
			return erexp;
		}
	}

	/**
	 * Make a pattern in the context of this stylesheet element
	 * @param pattern the source text of the pattern
	 * @return the compiled pattern
     * @throws net.sf.saxon.trans.XPathException if a static error is detected in the pattern
	 */

	public Pattern makePattern(String pattern)
	throws XPathException {
		try {
            return Pattern.make(pattern, staticContext, getPreparedStylesheet());
		} catch (XPathException err) {
            if ("XPST0003".equals(err.getErrorCodeLocalPart())) {
                err.setErrorCode("XTSE0340");
            }
			compileError(err);
			return new ItemTypePattern(AnyNodeTest.getInstance());
		}
	}

	/**
	 * Make an attribute value template in the context of this stylesheet element
	 * @param expression the source text of the attribute value template
	 * @return a compiled XPath expression that computes the value of the attribute (including
	 *         concatenating the results of embedded expressions with any surrounding fixed text)
     * @throws net.sf.saxon.trans.XPathException if a static error is detected in the AVT
	 */

	protected Expression makeAttributeValueTemplate(String expression)
	throws XPathException {
		try {
			return AttributeValueTemplate.make(expression, getLineNumber(), staticContext);
		} catch (XPathException err) {
			compileError(err);
            return new StringLiteral(expression);
		}
	}


    /**
     * Check the value of an attribute, as supplied statically
     * @param name the name of the attribute
     * @param value the value of the attribute
     * @param avt set to true if the value is permitted to be an attribute value template
     * @param allowed list of permitted values, which must be in alphabetical order
     * @throws net.sf.saxon.trans.XPathException if the value given for the attribute is not a permitted value
     */

    protected void checkAttributeValue(String name, String value, boolean avt, String[] allowed) throws XPathException {
        if (avt && value.contains("{")) {
            return;
        }
        if (Arrays.binarySearch(allowed, value) < 0) {
            FastStringBuffer sb = new FastStringBuffer(FastStringBuffer.SMALL);
            sb.append("Invalid value for ");
            sb.append("@");
            sb.append(name);
            sb.append(". Value must be one of (");
            for (int i=0; i<allowed.length; i++) {
                sb.append((i==0 ? "" : "|"));
                sb.append(allowed[i]);
            }
            sb.append(")");
            compileError(sb.toString(), "XTSE0020");
        }
    }

    protected final static String[] YES_NO = new String[]{"no", "yes"};

	/**
	 * Process an attribute whose value is a SequenceType
	 * @param sequenceType the source text of the attribute
	 * @return the processed sequence type
	 * @throws XPathException if the syntax is invalid or for example if it refers to a type
	 *                        that is not in the static context
	 */

	public SequenceType makeSequenceType(String sequenceType)
	throws XPathException {
		getStaticContext();
		try {
			ExpressionParser parser =
				getConfiguration().newExpressionParser("XP", false, staticContext.getXPathLanguageLevel());
			parser.setLanguage(ExpressionParser.XPATH, staticContext.getXPathLanguageLevel());
			return parser.parseSequenceType(sequenceType, staticContext);
		} catch (XPathException err) {
			compileError(err);
			// recovery path after reporting an error, e.g. undeclared namespace prefix
			return SequenceType.ANY_SEQUENCE;
		}
	}

	/**
	 * Process the [xsl:]extension-element-prefixes attribute if there is one
	 * @param ns the namespace URI of the attribute - either the XSLT namespace or "" for the null namespace
     * @throws net.sf.saxon.trans.XPathException if the value of the attribute is invalid
	 */

	protected void processExtensionElementAttribute(String ns)
	throws XPathException {
		String ext = getAttributeValue(ns, StandardNames.EXTENSION_ELEMENT_PREFIXES);
		if (ext != null) {
			// go round twice, once to count the values and next to add them to the array
			int count = 0;
			StringTokenizer st1 = new StringTokenizer(ext, " \t\n\r", false);
			while (st1.hasMoreTokens()) {
				st1.nextToken();
				count++;
			}
			extensionNamespaces = new String[count];
			count = 0;
			StringTokenizer st2 = new StringTokenizer(ext, " \t\n\r", false);
			while (st2.hasMoreTokens()) {
				String s = st2.nextToken();
				if ("#default".equals(s)) {
					s = "";
				}
                String uri = getURIForPrefix(s, false);
                if (uri == null) {
                    extensionNamespaces = null;
                    compileError("Namespace prefix " + s + " is undeclared", "XTSE1430");
                } else {
                    assert extensionNamespaces != null;
                    extensionNamespaces[count++] = uri;
                }
			}
		}
	}

	/**
	 * Process the [xsl:]exclude-result-prefixes attribute if there is one
	 * @param ns the namespace URI of the attribute required, either the XSLT namespace or ""
     * @throws net.sf.saxon.trans.XPathException if the value of the attribute is invalid
	 */

	protected void processExcludedNamespaces(String ns)
	throws XPathException {
		String ext = getAttributeValue(ns, StandardNames.EXCLUDE_RESULT_PREFIXES);
		if (ext != null) {
			if ("#all".equals(Whitespace.trim(ext))) {
				Iterator<NamespaceBinding> codes = NamespaceIterator.iterateNamespaces(this);
                List<String> excluded = new ArrayList<String>();
                while (codes.hasNext()) {
                    excluded.add(codes.next().getURI());
                }
				excludedNamespaces = excluded.toArray(new String[excluded.size()]);
			} else {
				// go round twice, once to count the values and next to add them to the array
				int count = 0;
				StringTokenizer st1 = new StringTokenizer(ext, " \t\n\r", false);
				while (st1.hasMoreTokens()) {
					st1.nextToken();
					count++;
				}
				excludedNamespaces = new String[count];
				count = 0;
				StringTokenizer st2 = new StringTokenizer(ext, " \t\n\r", false);
				while (st2.hasMoreTokens()) {
					String s = st2.nextToken();
					if ("#default".equals(s)) {
						s = "";
					} else if ("#all".equals(s)) {
						compileError("In exclude-result-prefixes, cannot mix #all with other values", "XTSE0020");
					}
			        String uri = getURIForPrefix(s, true);
                    if (uri == null) {
                        excludedNamespaces = null;
						compileError("Namespace prefix " + s + " is not declared", "XTSE0808");
                        break;
                    }
                    excludedNamespaces[count++] = uri;
                    if (s.length()==0 && uri.length()==0) {
                        compileError("Cannot exclude the #default namespace when no default namespace is declared",
                        "XTSE0809");
                    }
				}
			}
		}
	}

	/**
	 * Process the [xsl:]version attribute if there is one
	 * @param ns the namespace URI of the attribute required, either the XSLT namespace or ""
     * @throws net.sf.saxon.trans.XPathException if the value of the attribute is invalid
	 */

	protected void processVersionAttribute(String ns) throws XPathException {
		String v = Whitespace.trim(getAttributeValue(ns, StandardNames.VERSION));
		if (v != null) {
			ConversionResult val = DecimalValue.makeDecimalValue(v, true);
			if (val instanceof ValidationFailure) {
				compileError("The version attribute must be a decimal literal", "XTSE0110");
				version = DecimalValue.TWO;
			} else {
				// Note this will normalize the decimal so that trailing spaces are not significant
				version = (DecimalValue)val;
			}
		}
	}

	/**
	 * Get the numeric value of the version number appearing as an attribute on this element,
	 * or inherited from its ancestors
	 * @return the version number as a decimal
	 */

	public DecimalValue getEffectiveVersion() {
		if (version == null) {
			NodeInfo node = getParent();
			if (node instanceof StyleElement) {
				version = ((StyleElement)node).getEffectiveVersion();
			} else {
				return DecimalValue.TWO;    // defensive programming
			}
		}
		return version;
	}

	/**
	 * Determine whether forwards-compatible mode is enabled for this element
	 * @return true if forwards-compatible mode is enabled
	 */

	public boolean forwardsCompatibleModeIsEnabled() {
		return getEffectiveVersion().compareTo(getProcessorVersion()) > 0;
	}

	/**
	 * Determine whether 1.0-compatible mode is enabled for this element
	 * @return true if 1.0 compatable mode is enabled, that is, if this or an enclosing
	 *         element specifies an [xsl:]version attribute whose value is less than 2.0
	 */

	public boolean xPath10ModeIsEnabled() {
		return getEffectiveVersion().compareTo(DecimalValue.TWO) < 0;
	}

	/**
	 * Determine what version of the XSLT the configured processor supports
	 * @return the version of XSLT supported by the processor
	 */

	public DecimalValue getProcessorVersion() {
		return getPreparedStylesheet().getStyleNodeFactory().getXsltProcessorVersion();
	}

	/**
	 * Ask whether the configured XSLT processor supports XSLT 3.0
	 * @return true if the configured XSLT processor supports XSLT 3.0
	 */

	public boolean isXslt30Processor() {
		return getProcessorVersion().compareTo(DecimalValue.THREE) >= 0;
	}

	/**
	 * Process the [xsl:]default-xpath-namespace attribute if there is one
     * @throws net.sf.saxon.trans.XPathException if the value is not a valid URI, or not a recognized collation URI
     */

	protected void processDefaultCollationAttribute() throws XPathException {
        String ns = getURI().equals(NamespaceConstant.XSLT) ? "" : NamespaceConstant.XSLT;
		String v = getAttributeValue(ns, StandardNames.DEFAULT_COLLATION);
		if (v != null) {
			StringTokenizer st = new StringTokenizer(v, " \t\n\r", false);
			while (st.hasMoreTokens()) {
				String uri = st.nextToken();
				if (uri.equals(NamespaceConstant.CODEPOINT_COLLATION_URI)) {
					defaultCollationName = uri;
					return;
				} else if (uri.startsWith("http://saxon.sf.net/")) {
					defaultCollationName = uri;
					return;
				} else {
					URI collationURI;
					try {
						collationURI = new URI(uri);
						if (!collationURI.isAbsolute()) {
							URI base = new URI(getBaseURI());
							collationURI = base.resolve(collationURI);
							uri = collationURI.toString();
						}
					} catch (URISyntaxException err) {
						compileError("default collation '" + uri + "' is not a valid URI");
						uri = NamespaceConstant.CODEPOINT_COLLATION_URI;
					}

					if (uri.startsWith("http://saxon.sf.net/")) {
						defaultCollationName = uri;
						return;
					}

					if (getPreparedStylesheet().getNamedCollation(uri) != null) {
						defaultCollationName = uri;
						return;
					}

					if (getPrincipalStylesheetModule().findCollation(uri, getBaseURI()) != null) {
						defaultCollationName = uri;
						return;
					}
				}
				// if not recognized, try the next URI in order
			}
			compileError("No recognized collation URI found in default-collation attribute", "XTSE0125");
		}
	}

	/**
	 * Get the default collation for this stylesheet element. If no default collation is
	 * specified in the stylesheet, return the Unicode codepoint collation name.
	 * @return the name of the default collation
	 */

	protected String getDefaultCollationName() {
		StyleElement e = this;
		while (true) {
			if (e.defaultCollationName != null) {
				return e.defaultCollationName;
			}
			NodeInfo p = e.getParent();
			if (!(p instanceof StyleElement)) {
				break;
			}
			e = (StyleElement)p;
		}
		String globalDefault = getConfiguration().getCollationMap().getDefaultCollationName();
		return (globalDefault == null ? NamespaceConstant.CODEPOINT_COLLATION_URI : globalDefault);
	}

	/**
	 * Check whether a particular extension element namespace is defined on this node.
	 * This checks this node only, not the ancestor nodes.
	 * The implementation checks whether the prefix is included in the
	 * [xsl:]extension-element-prefixes attribute.
	 * @param uri the namespace URI being tested
	 * @return true if this namespace is defined on this element as an extension element namespace
	 */

	protected boolean definesExtensionElement(String uri) {
		if (extensionNamespaces == null) {
			return false;
		}
        for (String extensionNamespace : extensionNamespaces) {
            if (extensionNamespace.equals(uri)) {
                return true;
            }
        }
		return false;
	}

	/**
	 * Check whether a namespace uri defines an extension element. This checks whether the
	 * namespace is defined as an extension namespace on this or any ancestor node.
	 * @param uri the namespace URI being tested
	 * @return true if the URI is an extension element namespace URI
	 */

	public boolean isExtensionNamespace(String uri) {
		NodeInfo anc = this;
		while (anc instanceof StyleElement) {
			if (((StyleElement)anc).definesExtensionElement(uri)) {
				return true;
			}
			anc = anc.getParent();
		}
		return false;
	}

	/**
	 * Check whether this node excludes a particular namespace from the result.
	 * This method checks this node only, not the ancestor nodes.
	 * @param uri the namespace URI being tested
	 * @return true if the namespace is excluded by virtue of an [xsl:]exclude-result-prefixes attribute
	 */

	protected boolean definesExcludedNamespace(String uri) {
		if (excludedNamespaces == null) {
			return false;
		}
        for (String excludedNamespace : excludedNamespaces) {
            if (excludedNamespace.equals(uri)) {
                return true;
            }
        }
		return false;
	}

	/**
	 * Check whether a namespace uri defines an namespace excluded from the result.
	 * This checks whether the namespace is defined as an excluded namespace on this
	 * or any ancestor node.
	 * @param uri the namespace URI being tested
	 * @return true if this namespace URI is a namespace excluded by virtue of exclude-result-prefixes
	 *         on this element or on an ancestor element
	 */

	public boolean isExcludedNamespace(String uri) {
		if (uri.equals(NamespaceConstant.XSLT) || uri.equals(NamespaceConstant.XML)) {
			return true;
		}
		if (isExtensionNamespace(uri)) {
			return true;
		}
		NodeInfo anc = this;
		while (anc instanceof StyleElement) {
			if (((StyleElement)anc).definesExcludedNamespace(uri)) {
				return true;
			}
			anc = anc.getParent();
		}
		return false;
	}

	/**
	 * Process the [xsl:]xpath-default-namespace attribute if there is one
	 * @param ns the namespace URI of the attribute required  (the default namespace or the XSLT namespace.)
	 */

	protected void processDefaultXPathNamespaceAttribute(String ns) {
		String v = getAttributeValue(ns, StandardNames.XPATH_DEFAULT_NAMESPACE);
		if (v != null) {
			defaultXPathNamespace = v;
		}
	}

	/**
	 * Get the default XPath namespace for elements and types
	 * @return the default namespace for elements and types.
	 *         Return {@link NamespaceConstant#NULL} for the non-namespace
	 */

	protected String getDefaultXPathNamespace() {
		NodeInfo anc = this;
		while (anc instanceof StyleElement) {
			String x = ((StyleElement)anc).defaultXPathNamespace;
			if (x != null) {
				return x;
			}
			anc = anc.getParent();
		}
		return NamespaceConstant.NULL;
		// indicates that the default namespace is the null namespace
	}

	/**
	 * Process the [xsl:]expand-text attribute if there is one (and if XSLT 3.0 is enabled)
	 * @param ns the namespace URI of the attribute required  (the default namespace or the XSLT namespace.)
	 */

	protected void processExpandTextAttribute(String ns) throws XPathException {
		String v = getAttributeValue(ns, "expand-text");
		if (v != null) {
			v = Whitespace.trim(v);
            if ("no".equals(v)) {
                expandText = false;
            } else if ("yes".equals(v)) {
                expandText = true;
            } else {
                throw new XPathException("The value of expand-text must be 'yes' or 'no'", "XTSE0020");
            }
        } else {
            NodeInfo parent = getParent();
            if (parent instanceof StyleElement) {
                expandText = ((StyleElement)parent).expandText;
            } else {
                expandText = false;
            }
        }
	}

	/**
	 * Ask whether content value templates are available within this element
	 * @return true if content value templates are enabled
	 */

    public boolean isExpandingText() {
		return expandText;
	}

	/**
	 * Get the Schema type definition for a type named in the stylesheet (in a
	 * "type" attribute).
	 * @param typeAtt the value of the type attribute
	 * @return the corresponding schema type
	 * @throws XPathException if the type is not declared in an
	 *                        imported schema, or is not a built-in type
	 */

	public SchemaType getSchemaType(String typeAtt) throws XPathException {
		try {
			String[] parts = getConfiguration().getNameChecker().getQNameParts(typeAtt);
			String lname = parts[1];
			String uri;
			if ("".equals(parts[0])) {
				// Name is unprefixed: use the default-xpath-namespace
				uri = getDefaultXPathNamespace();
			} else {
				uri = getURIForPrefix(parts[0], false);
				if (uri == null) {
					compileError("Namespace prefix for type annotation is undeclared", "XTSE1520");
					return null;
				}
			}
			int nc = getNamePool().allocate(parts[0], uri, lname);
			if (uri.equals(NamespaceConstant.SCHEMA)) {
				SchemaType t = BuiltInType.getSchemaType(StandardNames.getFingerprint(uri, lname));
				if (t == null) {
					compileError("Unknown built-in type " + typeAtt, "XTSE1520");
					return null;
				}
				return t;
			}

			// not a built-in type: look in the imported schemas

			if (!getPrincipalStylesheetModule().isImportedSchema(uri)) {
				compileError("There is no imported schema for the namespace of type " + typeAtt, "XTSE1520");
				return null;
			}
			SchemaType stype = getConfiguration().getSchemaType(nc & 0xfffff);
			if (stype == null) {
				compileError("There is no type named " + typeAtt + " in an imported schema", "XTSE1520");
			}
			return stype;

		} catch (QNameException err) {
			compileError("Invalid type name. " + err.getMessage(), "XTSE1520");
		}
		return null;
	}

	/**
	 * Get the type annotation to use for a given schema type
	 * @param schemaType the schema type
	 * @return the corresponding numeric type annotation
	 */

	public SimpleType getTypeAnnotation(SchemaType schemaType) {
		return (SimpleType)schemaType;
	}

	/**
	 * Check that the stylesheet element is valid. This is called once for each element, after
	 * the entire tree has been built. As well as validation, it can perform first-time
	 * initialisation. The default implementation does nothing; it is normally overriden
	 * in subclasses.
	 * @param decl the declaration to be validated
     * @throws XPathException if any error is found during validation
	 */

	public void validate(Declaration decl) throws XPathException {
	}

	/**
	 * Hook to allow additional validation of a parent element immediately after its
	 * children have been validated.
     * @throws XPathException if any error is found during post-traversal validation
	 */

	public void postValidate() throws XPathException {
	}

	/**
	 * Method supplied by declaration elements to add themselves to a stylesheet-level index
	 * @param decl the Declaration being indexed. (This corresponds to the StyleElement object
	 * except in cases where one module is imported several times with different precedence.)
	 * @param top  the outermost XSLStylesheet element
     * @throws XPathException if any error is encountered
	 */

	protected void index(Declaration decl, PrincipalStylesheetModule top) throws XPathException {
	}

	/**
	 * Type-check an expression. This is called to check each expression while the containing
	 * instruction is being validated. It is not just a static type-check, it also adds code
	 * to perform any necessary run-time type checking and/or conversion.
	 * @param name the name of the attribute containing the expression to be checked (used for diagnostics)
	 * @param exp  the expression to be checked
	 * @return the (possibly rewritten) expression after type checking
     * @throws XPathException if type-checking fails statically, that is, if it can be determined that the
     * supplied value for the expression cannot possibly be of the required type
	 */

	// Note: the typeCheck() call is done at the level of individual path expression; the optimize() call is done
	// for a template or function as a whole. We can't do it all at the function/template level because
	// the static context (e.g. namespaces) changes from one XPath expression to another.
	public Expression typeCheck(String name, Expression exp) throws XPathException {

		if (exp == null) {
			return null;
		}

		exp.setContainer(this);
		// temporary, until the instruction is compiled

		try {
			exp = makeExpressionVisitor().typeCheck(exp, new ExpressionVisitor.ContextItemType(Type.ITEM_TYPE, true));
			exp = ExpressionTool.resolveCallsToCurrentFunction(exp, getConfiguration());
			//            if (explaining) {
			//                System.err.println("Attribute '" + name + "' of element '" + getDisplayName() + "' at line " + getLineNumber() + ':');
			//                System.err.println("Static type: " +
			//                        SequenceType.makeSequenceType(exp.getItemType(), exp.getCardinality()));
			//                System.err.println("Optimized expression tree:");
			//                exp.display(10, getNamePool(), System.err);
			//            }
			CodeInjector injector = getPreparedStylesheet().getCompilerInfo().getCodeInjector();
            if (injector != null) {
                return injector.inject(exp, getStaticContext(), Location.XPATH_IN_XSLT, new StructuredQName("", "", name));
//				InstructionDetails details = new InstructionDetails();
//				details.setConstructType(Location.XPATH_IN_XSLT);
//				details.setLineNumber(getLineNumber());
//				details.setSystemId(getSystemId());
//				details.setProperty("attribute-name", name);
//				TraceWrapper trace = new TraceInstruction(exp, details);
//				trace.setLocationId(allocateLocationId(getSystemId(), getLineNumber()));
//				trace.setContainer(this);
//				exp = trace;
			}
			return exp;
		} catch (XPathException err) {
			// we can't report a dynamic error such as divide by zero unless the expression
			// is actually executed.
            //err.printStackTrace();
			if (err.isReportableStatically()) {
				compileError(err);
				return exp;
			} else {
				ErrorExpression erexp = new ErrorExpression(err);
				erexp.setLocationId(allocateLocationId(getSystemId(), getLineNumber()));
				return erexp;
			}
		}
	}

	/**
	 * Allocate slots in the local stack frame to range variables used in an XPath expression
	 * @param exp the XPath expression for which slots are to be allocated
	 */

	public void allocateSlots(Expression exp) {
		SlotManager slotManager = getContainingSlotManager();
		if (slotManager == null) {
			throw new AssertionError("Slot manager has not been allocated");
			// previous code: ExpressionTool.allocateSlots(exp, 0, null);
		} else {
			int firstSlot = slotManager.getNumberOfVariables();
			int highWater = ExpressionTool.allocateSlots(exp, firstSlot, slotManager);
			if (highWater > firstSlot) {
				slotManager.setNumberOfVariables(highWater);
				// This algorithm is not very efficient because it never reuses
				// a slot when a variable goes out of scope. But at least it is safe.
				// Note that range variables within XPath expressions need to maintain
				// a slot until the instruction they are part of finishes, e.g. in
				// xsl:for-each.
			}
		}
	}

	/**
	 * Allocate space for range variables within predicates in the match pattern. The xsl:template
	 * element has no XPath expressions among its attributes, so if this method is called on this
	 * object it can only be because there are variables used in the match pattern. We work out
	 * how many slots are needed for the match pattern in each template rule, and then apply-templates
	 * can allocate a stack frame that is large enough for the most demanding match pattern in the
	 * entire stylesheet.
	 * @param slots the number of slots required
	 */

	public void allocatePatternSlots(int slots) {
		getPrincipalStylesheetModule().allocatePatternSlots(slots);
	}

	/**
	 * Type-check a pattern. This is called to check each pattern while the containing
	 * instruction is being validated. It is not just a static type-check, it also adds code
	 * to perform any necessary run-time type checking and/or conversion.
	 * @param name    the name of the attribute holding the pattern, for example "match": used in
	 *                diagnostics
	 * @param pattern the compiled pattern
	 * @return the original pattern, or a substitute pattern if it has been rewritten. Returns null
     * if and only if the supplied pattern is null.
     * @throws net.sf.saxon.trans.XPathException if the pattern fails optimistic static type-checking
	 */

	public Pattern typeCheck(String name, Pattern pattern) throws XPathException {
		if (pattern == null) {
			return null;
		}
		try {
			pattern = pattern.analyze(makeExpressionVisitor(), new ExpressionVisitor.ContextItemType(Type.NODE_TYPE, true));
			boolean usesCurrent = false;

			Iterator sub = pattern.iterateSubExpressions();
			while (sub.hasNext()) {
				Expression filter = (Expression)sub.next();
				if (ExpressionTool.callsFunction(filter, Current.FN_CURRENT)) {
					usesCurrent = true;
					break;
				}
			}
			if (usesCurrent) {
                PatternThatSetsCurrent p2 = new PatternThatSetsCurrent(pattern);
                pattern.bindCurrent(p2.getCurrentBinding());
                pattern = p2;
			}

			return pattern;
		} catch (XPathException err) {
			// we can't report a dynamic error such as divide by zero unless the pattern
			// is actually executed. We don't have an error pattern available, so we
			// construct one
			if (err.isReportableStatically()) {
				XPathException e2 = new XPathException("Error in " + name + " pattern", err);
				e2.setLocator(this);
				e2.setErrorCodeQName(err.getErrorCodeQName());
				throw e2;
			} else {
				PatternWithPredicate errpat = new PatternWithPredicate(
                        new ItemTypePattern(AnyItemType.getInstance()), new ErrorExpression(err));
                errpat.setExecutable(getPreparedStylesheet());
				return errpat;
			}
		}
	}

	/**
	 * Fix up references from XPath expressions. Overridden for function declarations
	 * and variable declarations
     * @throws net.sf.saxon.trans.XPathException if any references cannot be fixed up.
     */

	public void fixupReferences() throws XPathException {
		AxisIterator kids = iterateAxis(AxisInfo.CHILD);
		while (true) {
			NodeInfo child = kids.next();
			if (child == null) {
				return;
			}
			if (child instanceof StyleElement) {
				((StyleElement)child).fixupReferences();
			}
		}
	}

	/**
	 * Get the SlotManager for the containing Procedure definition
	 * @return the SlotManager associated with the containing Function, Template, etc,
	 *         or null if there is no such containing Function, Template etc.
	 */

	public SlotManager getContainingSlotManager() {
		NodeInfo node = this;
		while (true) {
			NodeInfo next = node.getParent();
			if (next instanceof XSLStylesheet) {
				if (node instanceof StylesheetProcedure) {
					return ((StylesheetProcedure)node).getSlotManager();
				} else {
					return null;
				}
			}
			node = next;
		}
	}


	/**
	 * Recursive walk through the stylesheet to validate all nodes
	 * @param decl the declaration to be validated
     * @throws XPathException if validation fails
	 */

	public void validateSubtree(Declaration decl) throws XPathException {
		if (isActionCompleted(StyleElement.ACTION_VALIDATE)) {
			return;
		}
		setActionCompleted(StyleElement.ACTION_VALIDATE);
		if (validationError != null) {
			if (reportingCircumstances == REPORT_ALWAYS) {
				compileError(validationError);
			} else if (reportingCircumstances == REPORT_UNLESS_FORWARDS_COMPATIBLE
					&& !forwardsCompatibleModeIsEnabled()) {
				compileError(validationError);
			} else if (reportingCircumstances == REPORT_UNLESS_FALLBACK_AVAILABLE) {
                if (!forwardsCompatibleModeIsEnabled()) {
                    compileError(validationError);
                } else {
                    boolean hasFallback = false;
                    AxisIterator kids = iterateAxis(AxisInfo.CHILD);
                    while (true) {
                        NodeInfo child = kids.next();
                        if (child == null) {
                            break;
                        }
                        if (child instanceof XSLFallback) {
                            hasFallback = true;
                            ((XSLFallback)child).validateSubtree(decl);
                        }
                    }
                    if (!hasFallback) {
                        compileError(validationError);
                    }
                }
			}
		} else {
			try {
				validate(decl);
			} catch (XPathException err) {
				compileError(err);
			}
			validateChildren(decl);
			postValidate();
		}
	}

	/**
	 * Validate the children of this node, recursively. Overridden for top-level
	 * data elements.
	 * @param decl the declaration whose children are to be validated
     * @throws XPathException if validation fails
	 */

	protected void validateChildren(Declaration decl) throws XPathException {
		boolean containsInstructions = mayContainSequenceConstructor();
		AxisIterator kids = iterateAxis(AxisInfo.CHILD);
		StyleElement lastChild = null;
        boolean endsWithTextTemplate = false;
		while (true) {
			NodeInfo child = kids.next();
			if (child == null) {
				break;
			}
			if (child instanceof StyleElement) {
                endsWithTextTemplate = false;
				if (containsInstructions && !((StyleElement)child).isInstruction()
						&& !isPermittedChild((StyleElement)child)) {
					((StyleElement)child).compileError("An " + getDisplayName() + " element must not contain an " +
							child.getDisplayName() + " element", "XTSE0010");
				}
				((StyleElement)child).validateSubtree(decl);
				lastChild = (StyleElement)child;
			} else {
                endsWithTextTemplate = getPreparedStylesheet().getStyleNodeFactory().validateTextNode(child);
            }
		}
		if (lastChild instanceof XSLLocalVariable &&
				!(this instanceof XSLStylesheet) && !endsWithTextTemplate) {
			lastChild.compileWarning("A variable with no following sibling instructions has no effect",
					SaxonErrorCode.SXWN9001);
		}
	}

	/**
	 * Check whether a given child is permitted for this element. This method is used when a non-instruction
	 * child element such as xsl:sort is encountered in a context where instructions would normally be expected.
	 * @param child the child that may or may not be permitted
	 * @return true if the child is permitted.
	 */

	protected boolean isPermittedChild(StyleElement child) {
		return false;
	}

	/**
	 * Get the PreparedStylesheet object.
	 * @return the PreparedStylesheet to which this stylesheet element belongs.
	 *         Exceptionally (with early errors in a simplified stylesheet module) return null.
	 */

	public PreparedStylesheet getPreparedStylesheet() {
		XSLStylesheet xss = getContainingStylesheet();
		return (xss==null ? null : xss.getPreparedStylesheet());
	}

	/**
	 * Get the principal stylesheet module
	 * @return the principal stylesheet module
	 */

	public PrincipalStylesheetModule getPrincipalStylesheetModule() {
		return getContainingStylesheet().getPrincipalStylesheetModule();
	}

	/**
	 * Check that among the children of this element, any xsl:sort elements precede any other elements
	 * @param sortRequired true if there must be at least one xsl:sort element
	 * @throws XPathException if invalid
	 */

	protected void checkSortComesFirst(boolean sortRequired) throws XPathException {
		AxisIterator kids = iterateAxis(AxisInfo.CHILD);
		boolean sortFound = false;
		boolean nonSortFound = false;
		while (true) {
			NodeInfo child = kids.next();
			if (child == null) {
				break;
			}
			if (child instanceof XSLSort) {
				if (nonSortFound) {
					((XSLSort)child).compileError("Within " + getDisplayName() +
							", xsl:sort elements must come before other instructions", "XTSE0010");
				}
				sortFound = true;
			} else if (child.getNodeKind() == Type.TEXT) {
				// with xml:space=preserve, white space nodes may still be there
				if (!Whitespace.isWhite(child.getStringValueCS())) {
					nonSortFound = true;
				}
			} else {
				nonSortFound = true;
			}
		}
		if (sortRequired && !sortFound) {
			compileError(getDisplayName() + " must have at least one xsl:sort child", "XTSE0010");
		}
	}

	/**
	 * Convenience method to check that the stylesheet element is at the top level (that is,
     * as a child of xsl:stylesheet or xsl:transform)
	 * @param errorCode the error to throw if it is not at the top level; defaults to XTSE0010
	 *                  if the value is null
	 * @throws XPathException if not at top level
	 */

	public void checkTopLevel(/*@NotNull*/ String errorCode) throws XPathException {
		if (!(getParent() instanceof XSLStylesheet)) {
			compileError("Element " + getDisplayName() + " must always be a child of xsl:stylesheet or xsl:transform", errorCode);
		}
	}

	/**
	 * Convenience method to check that the stylesheet element is empty
	 * @throws XPathException if it is not empty
	 */

	public void checkEmpty() throws XPathException {
		if (hasChildNodes()) {
			compileError("Element must be empty", "XTSE0260");
		}
	}

	/**
	 * Convenience method to report the absence of a mandatory attribute
	 * @param attribute the name of the attribute whose absence is to be reported
	 * @throws XPathException if the attribute is missing
	 */

	public void reportAbsence(String attribute)
	throws XPathException {
		compileError("Element must have an @" + attribute + " attribute", "XTSE0010");
	}


	/**
	 * Compile the instruction on the stylesheet tree into an executable instruction
	 * for use at run-time.
	 * @param exec the Executable
	 * @param decl the containing top-level declaration, for example xsl:function or xsl:template
	 * @return either a ComputedExpression, or null. The value null is returned when compiling an instruction
	 *         that returns a no-op, or when compiling a top-level object such as an xsl:template that compiles
	 *         into something other than an instruction.
     * @throws net.sf.saxon.trans.XPathException if validation fails
	 */

	public Expression compile(Executable exec, Declaration decl) throws XPathException {
        // no action: default for non-instruction elements
        return null;
    }

    /**
     * Compile a declaration in the stylesheet tree
     * for use at run-time.
     * @param exec the Executable
     * @param decl the containing top-level declaration, for example xsl:function or xsl:template
     * @throws net.sf.saxon.trans.XPathException if compilation fails
     */

    public void compileDeclaration(Executable exec, Declaration decl) throws XPathException {
        // no action: default for elements that are not declarations
    }


	/**
	 * Compile the children of this instruction on the stylesheet tree, adding the
	 * subordinate instructions to the parent instruction on the execution tree.
	 * @param exec          the Executable
	 * @param decl          the Declaration of the containing top-level stylesheet element
	 * @param iter          Iterator over the children. This is used in the case where there are children
	 *                      that are not part of the sequence constructor, for example the xsl:sort children of xsl:for-each;
	 *                      the iterator can be positioned past such elements.
	 * @param includeParams true if xsl:param elements are to be treated as child instructions (true
	 *                      for templates but not for functions) @return an Expression tree representing the children of this instruction
     * @return the compiled sequence constructor
     * @throws net.sf.saxon.trans.XPathException if compilation fails
	 */

	public Expression compileSequenceConstructor(Executable exec, Declaration decl,
			SequenceIterator iter, boolean includeParams)
	throws XPathException {

		//Expression result = Literal.makeEmptySequence();
		int locationId = allocateLocationId(getSystemId(), getLineNumber());
		List<Expression> contents = new ArrayList<Expression>(10);
		int lineNumber = getLineNumber();
		while (true) {
			NodeInfo node = ((NodeInfo)iter.next());
			if (node == null) {
				//return result;
				break;
			}
			if (node instanceof StyleElement) {
				lineNumber = node.getLineNumber();  // this is to get a line number for the next text node
			}
			if (node.getNodeKind() == Type.TEXT) {
                if (isExpandingText()) {
                    getPreparedStylesheet().getStyleNodeFactory().compileContentValueTemplate((TextImpl)node, contents);
                } else {
                    // handle literal text nodes by generating an xsl:value-of instruction, unless expand-text is enabled
                    AxisIterator lookahead = node.iterateAxis(AxisInfo.FOLLOWING_SIBLING);
                    NodeInfo sibling = lookahead.next();
                    if (!(sibling instanceof XSLLocalParam || sibling instanceof XSLSort)) {
                        // The test for XSLParam and XSLSort is to eliminate whitespace nodes that have been retained
                        // because of xml:space="preserve"
                        Expression text = new ValueOf(new StringLiteral(node.getStringValue()), false, false);

                        text.setLocationId(allocateLocationId(getSystemId(), lineNumber));

                        CodeInjector injector = getPreparedStylesheet().getCompilerInfo().getCodeInjector();
                        if (injector != null) {
                            Expression tracer = injector.inject(text, getStaticContext(), StandardNames.XSL_TEXT, null);
                            tracer.setLocationId(text.getLocationId());
                            text = tracer;
                        }

                        contents.add(text);
                    }
                }

			} else if (node instanceof XSLLocalVariable) {
                XSLLocalVariable var = (XSLLocalVariable)node;
                SourceBinding sourceBinding = var.getSourceBinding();
				var.compileLocalVariable(exec, decl);

                Expression tail = compileSequenceConstructor(exec, decl, iter, includeParams);
                if (tail == null || Literal.isEmptySequence(tail)) {
                    // this doesn't happen, because if there are no instructions following
                    // a variable, we'll have taken the var==null path above
                    //return result;
                } else {
                    LetExpression let = new LetExpression();
                    let.setRequiredType(var.getRequiredType());
                    let.setVariableQName(sourceBinding.getVariableQName());
                    let.setSequence(sourceBinding.getSelectExpression());
                    let.setAction(tail);
                    sourceBinding.fixupBinding(let);
                    locationId = allocateLocationId(node.getSystemId(), node.getLineNumber());
                    let.setLocationId(locationId);
                    if (getPreparedStylesheet().isCompileWithTracing()) {
                        TraceExpression t = new TraceExpression(let);
                        t.setConstructType(Location.LET_EXPRESSION);
                        t.setObjectName(var.getSourceBinding().getVariableQName());
                        t.setNamespaceResolver(getNamespaceResolver());
                        contents.add(t);
                    } else {
                        contents.add(let);
                    }
                    //result.setLocationId(locationId);
                }


			} else if (node instanceof StyleElement) {
				StyleElement snode = (StyleElement)node;
				Expression child;
				if (snode.validationError != null && !(this instanceof AbsentExtensionElement)) {
					child = fallbackProcessing(exec, decl, snode);

				} else {
					child = snode.compile(exec, decl);
					if (child != null) {
						if (child.getContainer() == null) {
							// for the time being, the XSLT stylesheet element acts as the container
							// for the XPath expressions within. This will later be replaced by a
							// compiled template, variable, or other top-level construct
							child.setContainer(this);
						}
						locationId = allocateLocationId(getSystemId(), snode.getLineNumber());
						child.setLocationId(locationId);
						if (includeParams || !(node instanceof XSLLocalParam)) {
							if (getPreparedStylesheet().isCompileWithTracing()) {
								child = makeTraceInstruction(snode, child);
							}
						}
					}
				}
				if (child != null) {
					contents.add(child);
				}
			}
		}
		Expression block = Block.makeBlock(contents);
		block.setLocationId(locationId);
		return block;
	}

	/**
	 * Create a trace instruction to wrap a real instruction
	 *
     *
     * @param source the parent element
     * @param child  the compiled expression tree for the instruction to be traced
     * @return a wrapper instruction that performs the tracing (if activated at run-time)
	 */

	protected static Expression makeTraceInstruction(StyleElement source, Expression child) {
		if (child instanceof TraceExpression && !(source instanceof StylesheetProcedure)) {
			return child;
			// this can happen, for example, after optimizing a compile-time xsl:if
		}
        CodeInjector injector = source.getPreparedStylesheet().getCompilerInfo().getCodeInjector();
        if (injector != null) {
            int construct = source.getFingerprint();
            if (source instanceof LiteralResultElement) {
                construct = Location.LITERAL_RESULT_ELEMENT;
            }
            Expression tracer = injector.inject(child, source.getStaticContext(), construct, source.getObjectName());
            tracer.setLocationId(source.allocateLocationId(source.getSystemId(), source.getLineNumber()));
            return tracer;
        }
        return child;
	}

	/**
	 * Perform fallback processing. Generate fallback code for an extension
	 * instruction that is not recognized by the implementation.
	 * @param exec        the Executable
	 * @param decl        the Declaration of the top-level element containing the extension instruction
	 * @param instruction The unknown extension instruction
     * @return the expression tree representing the fallback code
     * @throws net.sf.saxon.trans.XPathException if any error occurs
	 */

	protected Expression fallbackProcessing(Executable exec, Declaration decl, StyleElement instruction)
	throws XPathException {
		// process any xsl:fallback children; if there are none,
		// generate code to report the original failure reason
		Expression fallback = null;
		AxisIterator kids = instruction.iterateAxis(AxisInfo.CHILD);
		while (true) {
			NodeInfo child = kids.next();
			if (child == null) {
				break;
			}
			if (child instanceof XSLFallback) {
				//fallback.setLocationId(allocateLocationId(getSystemId(), child.getLineNumber()));
				//((XSLFallback)child).compileChildren(exec, fallback, true);
				Expression b = ((XSLFallback)child).compileSequenceConstructor(exec, decl, child.iterateAxis(AxisInfo.CHILD), true);
				if (b == null) {
					b = Literal.makeEmptySequence();
				}
				if (fallback == null) {
					fallback = b;
				} else {
					fallback = Block.makeBlock(fallback, b);
					fallback.setLocationId(
							allocateLocationId(getSystemId(), getLineNumber()));
				}
			}
		}
		if (fallback != null) {
			return fallback;
		} else {
			return new ErrorExpression(instruction.validationError);
			//            compileError(instruction.validationError);
			//            return EmptySequence.getInstance();
		}

	}

	/**
	 * Allocate a location identifier
	 * @param systemId   identifies the module containing the instruction
	 * @param lineNumber the line number of the instruction
	 * @return an integer location ID which can be used to report the location of the instruction,
	 *         by reference to a {@link LocationProvider}
	 */

	protected int allocateLocationId(String systemId, int lineNumber) {
		return getStaticContext().getLocationMap().allocateLocationId(systemId, lineNumber);
	}

	/**
	 * Construct sort keys for a SortedIterator
	 * @return an array of SortKeyDefinition objects if there are any sort keys;
	 *         or null if there are none.
	 * @param decl the declaration containing the sort keys
     * @throws XPathException if any error is detected
	 */

	public SortKeyDefinition[] makeSortKeys(Declaration decl) throws XPathException {
		// handle sort keys if any

		int numberOfSortKeys = 0;
		AxisIterator kids = iterateAxis(AxisInfo.CHILD);
		while (true) {
			Item child = kids.next();
			if (child == null) {
				break;
			}
			if (child instanceof XSLSortOrMergeKey) {
                ((XSLSortOrMergeKey)child).compile(getPreparedStylesheet(), decl);
				if (child instanceof XSLSort) {
                    if (numberOfSortKeys != 0 && ((XSLSort)child).getStable() != null) {
                        compileError("stable attribute may appear only on the first xsl:sort element", "XTSE1017");
                    }
                }
                numberOfSortKeys++;
            }
		}

		if (numberOfSortKeys > 0) {
			SortKeyDefinition[] keys = new SortKeyDefinition[numberOfSortKeys];
			kids = iterateAxis(AxisInfo.CHILD);
			int k = 0;
			while (true) {
				NodeInfo child = kids.next();
				if (child == null) {
					break;
				}
				if (child instanceof XSLSortOrMergeKey) {
					keys[k++] = ((XSLSortOrMergeKey)child).getSortKeyDefinition().simplify(makeExpressionVisitor());
				}
			}
			return keys;

		} else {
			return null;
		}
	}

	/**
	 * Get the list of attribute-sets associated with this element.
	 * This is used for xsl:element, xsl:copy, xsl:attribute-set, and on literal
	 * result elements
	 * @param use  the original value of the [xsl:]use-attribute-sets attribute
	 * @param list an empty list to hold the list of XSLAttributeSet elements in the stylesheet tree.
	 *             Or null, if these are not required.
	 * @return an array of AttributeList instructions representing the compiled attribute sets
     * @throws net.sf.saxon.trans.XPathException if any error is detected
	 */

	protected AttributeSet[] getAttributeSets(String use, /*@Nullable*/ List<Declaration> list)
	throws XPathException {

		if (list == null) {
			list = new ArrayList<Declaration>(4);
		}
		PrincipalStylesheetModule psm = getPrincipalStylesheetModule();
		StringTokenizer st = new StringTokenizer(use, " \t\n\r", false);
		while (st.hasMoreTokens()) {
			String asetname = st.nextToken();
			StructuredQName name;
			try {
				name = makeQName(asetname);
			} catch (NamespaceException err) {
				compileError(err.getMessage(), "XTSE0710");
				name = null;
			} catch (XPathException err) {
				compileError(err.getMessage(), "XTSE0710");
				name = null;
			}
			boolean found = psm.getAttributeSets(name, list);
			if (!found) {
				compileError("No attribute-set exists named " + asetname, "XTSE0710");
			}
		}
		AttributeSet[] array = new AttributeSet[list.size()];
		for (int i=0; i<list.size(); i++) {
			XSLAttributeSet aset = (XSLAttributeSet)list.get(i).getSourceElement();
			array[i] = aset.getInstruction();
		}
		return array;
	}

	/**
	 * Get the list of xsl:with-param elements for a calling element (apply-templates,
	 * call-template, apply-imports, next-match). This method can be used to get either
	 * the tunnel parameters, or the non-tunnel parameters.
	 * @param exec   the Executable
	 * @param decl   the containing stylesheet declaration
	 * @param tunnel true if the tunnel="yes" parameters are wanted, false to get
	 * @param caller the calling instruction (for example xsl:apply-templates), used
	 *               only for its location information @return an array of WithParam objects for either the ordinary parameters
	 *         or the tunnel parameters
     * @return an array containing the results of compiling the xsl:with-param children of this instruction (if any)
     * @throws XPathException if any error is detected
	 */

	protected WithParam[] getWithParamInstructions(Executable exec, Declaration decl, boolean tunnel, Expression caller)
	throws XPathException {
		int count = 0;
		AxisIterator kids = iterateAxis(AxisInfo.CHILD);
		while (true) {
			NodeInfo child = kids.next();
			if (child == null) {
				break;
			}
			if (child instanceof XSLWithParam) {
				XSLWithParam wp = (XSLWithParam)child;
				if (wp.getSourceBinding().hasProperty(SourceBinding.TUNNEL) == tunnel) {
					count++;
				}
			}
		}
        if (count == 0) {
            return WithParam.EMPTY_ARRAY;
        }
		WithParam[] array = new WithParam[count];
		count = 0;
		kids = iterateAxis(AxisInfo.CHILD);
		while (true) {
			NodeInfo child = kids.next();
			if (child == null) {
				return array;
			}
			if (child instanceof XSLWithParam) {
				XSLWithParam wp = (XSLWithParam)child;
				if (wp.getSourceBinding().hasProperty(SourceBinding.TUNNEL) == tunnel) {
					WithParam p = wp.compileWithParam(exec, decl);
					p.setLocationId(exec.getLocationMap().allocateLocationId(getSystemId(), getLineNumber()));
					array[count++] = p;
				}

			}
		}
	}

	/**
	 * Report an error with diagnostic information
	 * @param error contains information about the error
	 * @throws XPathException always, after reporting the error to the ErrorListener
	 */

	protected void compileError(XPathException error)
	throws XPathException {
		error.setIsStaticError(true);
		// Set the location of the error if there is no current location information,
		// or if the current location information is local to an XPath expression, unless we are
		// positioned on an xsl:function or xsl:template, in which case this would lose too much information
		if (error.getLocator() == null ||
				((error.getLocator() instanceof ExpressionLocation ||
						error.getLocator() instanceof Expression) && !(this instanceof StylesheetProcedure))) {
			error.setLocator(this);
		}
		PreparedStylesheet pss = getPreparedStylesheet();
		try {
			if (pss == null) {
				// it is null before the stylesheet has been fully built
				throw error;
			} else {
				pss.reportError(error);
			}
		} catch (TransformerException err2) {
			if (err2.getLocator() == null) {
				err2.setLocator(this);
			}
			throw XPathException.makeXPathException(err2);
		}
	}

	/**
	 * Report a static error in the stylesheet
	 * @param message the error message
	 * @throws XPathException always, after reporting the error to the ErrorListener
	 */

	public void compileError(String message)
	throws XPathException {
		XPathException tce = new XPathException(message);
		tce.setLocator(this);
		compileError(tce);
	}

	/**
	 * Compile time error, specifying an error code
	 * @param message   the error message
	 * @param errorCode the error code. May be null if not known or not defined
	 * @throws XPathException always, after reporting the error to the ErrorListener
	 */

	public void compileError(String message, StructuredQName errorCode) throws XPathException {
		XPathException tce = new XPathException(message);
		tce.setErrorCodeQName(errorCode);
		tce.setLocator(this);
		compileError(tce);
	}

	/**
	 * Compile time error, specifying an error code
	 * @param message   the error message
	 * @param errorCode the error code. May be null if not known or not defined
	 * @throws XPathException always, after reporting the error to the ErrorListener
	 */

	public void compileError(String message, String errorCode) throws XPathException {
		XPathException tce = new XPathException(message);
		tce.setErrorCode(errorCode);
		tce.setLocator(this);
		compileError(tce);
	}

	protected void undeclaredNamespaceError(String prefix, String errorCode) throws XPathException {
		if (errorCode == null) {
			errorCode = "XTSE0280";
		}
		compileError("Undeclared namespace prefix " + Err.wrap(prefix), errorCode);
	}

	protected void compileWarning(String message, StructuredQName errorCode)
	throws XPathException {
		XPathException tce = new XPathException(message);
		tce.setErrorCodeQName(errorCode);
		tce.setLocator(this);
		PreparedStylesheet pss = getPreparedStylesheet();
		if (pss != null) {
			pss.reportWarning(tce);
		}
	}

	protected void compileWarning(String message, String errorCode)
	throws XPathException {
		XPathException tce = new XPathException(message);
		tce.setErrorCode(errorCode);
		tce.setLocator(this);
		PreparedStylesheet pss = getPreparedStylesheet();
		if (pss != null) {
			pss.reportWarning(tce);
		}
	}

	/**
	 * Report a warning to the error listener
	 * @param error an exception containing the warning text
	 */

	protected void issueWarning(TransformerException error) {
		if (error.getLocator() == null) {
			error.setLocator(this);
		}
		PreparedStylesheet pss = getPreparedStylesheet();
		if (pss != null) {
			// it is null before the stylesheet has been fully built - ignore it
			pss.reportWarning(error);
		}
	}

	/**
	 * Report a warning to the error listener
	 * @param message the warning message text
	 * @param locator the location of the problem in the source stylesheet
	 */

	protected void issueWarning(String message, SourceLocator locator) {
		TransformerConfigurationException tce =
			new TransformerConfigurationException(message);
		if (locator == null) {
			tce.setLocator(this);
		} else {
			tce.setLocator(locator);
		}
		issueWarning(tce);
	}

	/**
	 * Test whether this is a top-level element
	 * @return true if the element is a child of the xsl:stylesheet element
	 */

	public boolean isTopLevel() {
		return (getParent() instanceof XSLStylesheet);
	}

    /**
     * Ask whether this element contains a binding for a variable with a given name; and if it does,
     * return the source binding information
     * @param name the variable name
     * @return the binding information if this element binds a variable of this name; otherwise null
     */

    public SourceBinding getBindingInformation(StructuredQName name) {
        return null;
    }

	/**
	 * Bind a variable used in this element to the compiled form of the XSLVariable element in which it is
	 * declared
	 * @param qName The name of the variable
	 * @return the XSLVariableDeclaration (that is, an xsl:variable or xsl:param instruction) for the variable,
	 *         or null if no declaration of the variable can be found
	 */

	public SourceBinding bindVariable(StructuredQName qName) {

        SourceBinding decl = bindLocalVariable(qName);
        if (decl != null) {
            return decl;
        }

		// Now check for a global variable
		// we rely on the search following the order of decreasing import precedence.
		SourceBinding binding = getPrincipalStylesheetModule().getGlobalVariable(qName);
        if (binding == null || Navigator.isAncestorOrSelf(binding.getSourceElement(), this)) {
            // test case variable-0118
            return null;
        } else {
            return binding;
        }
	}

   /**
	 * Bind a variable reference used in this element to the compiled form of the XSLVariable element in which it is
	 * declared, considering only local variables and params
	 * @param qName The name of the variable
	 * @return the XSLVariableDeclaration (that is, an xsl:variable or xsl:param instruction) for the variable,
	 *         or null if no local declaration of the variable can be found
	 */

    public SourceBinding bindLocalVariable(StructuredQName qName) {
        NodeInfo curr = this;
        NodeInfo prev = this;

        // first search for a local variable declaration
        if (!isTopLevel()) {
            AxisIterator preceding = curr.iterateAxis(AxisInfo.PRECEDING_SIBLING);
            while (true) {
                curr = preceding.next();
                while (curr == null) {
                    curr = prev.getParent();
                    while (curr instanceof StyleElement && !((StyleElement)curr).seesAvuncularVariables()) {
                        // a local variable is not visible within a sibling xsl:fallback or xsl:catch element
                        curr = curr.getParent();
                    }
                    if (curr instanceof XSLForEachGroup) {
                        XSLForEachGroup feg = ((XSLForEachGroup)curr);
                        if (qName.equals(feg.getGroupBindingName())) {
                            return feg.getGroupSourceBinding();
                        } else if (qName.equals(feg.getKeyBindingName())) {
                            return feg.getKeyBinding();
                        }
                    }
                    prev = curr;
                    if (curr.getParent() instanceof XSLStylesheet) {
                        break;   // top level
                    }
                    preceding = curr.iterateAxis(AxisInfo.PRECEDING_SIBLING);
                    curr = preceding.next();
                }
                if (curr.getParent() instanceof XSLStylesheet) {
                    break;
                }
                if (curr instanceof XSLGeneralVariable) {
                    SourceBinding sourceBinding = ((XSLGeneralVariable)curr).getBindingInformation(qName);
                    if (sourceBinding != null) {
                        return sourceBinding;
                    }
                }

            }
        }
        return null;
    }

	/**
	 * Ask whether variables declared in an "uncle" element are visible.
	 * @return true for all elements except xsl:fallback and saxon:catch
	 */

	protected boolean seesAvuncularVariables() {
		return true;
	}

	/**
	 * Get the type of construct. This will be a constant in
	 * class {@link Location}. This method is part of the {@link InstructionInfo} interface
	 */

	public int getConstructType() {
		return getFingerprint();
	}

	/**
	 * Get a name identifying the object of the expression, for example a function name, template name,
	 * variable name, key name, element name, etc. This is used only where the name is known statically.
	 * If there is no name, the value will be null.
	 * @return the name of the object declared in this element, if any
	 */

	public StructuredQName getObjectName() {
		return objectName;
	}

	/**
	 * Set the object name, for example the name of a function, variable, or template declared on this element
	 * @param qName the object name as a QName
	 */

	public void setObjectName(StructuredQName qName) {
		objectName = qName;
	}

	/**
	 * Get the value of a particular property of the instruction. This is part of the
	 * {@link InstructionInfo} interface for run-time tracing and debugging. The properties
	 * available include all the attributes of the source instruction (named by the attribute name):
	 * these are all provided as string values.
	 * @param name The name of the required property
	 * @return The value of the requested property, or null if the property is not available
	 */

	public Object getProperty(String name) {
		return getAttributeValue("", name);
	}

	/**
	 * Get an iterator over all the properties available. The values returned by the iterator
	 * will be of type String, and each string can be supplied as input to the getProperty()
	 * method to retrieve the value of the property.
	 */

	public Iterator<String> getProperties() {
		NamePool pool = getNamePool();
		List<String> list = new ArrayList<String>(10);
		AxisIterator it = iterateAxis(AxisInfo.ATTRIBUTE);
		while (true) {
			NodeInfo a = it.next();
			if (a == null) {
				break;
			}
			list.add(pool.getClarkName(a.getNameCode()));
		}
		return list.iterator();
	}

	public String getSystemId(long locationId) {
		return getSystemId();
	}

	public int getLineNumber(long locationId) {
		return getLineNumber();
	}

	public int getColumnNumber(long locationId) {
		return getColumnNumber();
	}

	/**
	 * Get the host language (XSLT, XQuery, XPath) used to implement the code in this container
	 * @return typically {@link net.sf.saxon.Configuration#XSLT} or {@link net.sf.saxon.Configuration#XQUERY}
	 */

	public int getHostLanguage() {
		return Configuration.XSLT;
	}

	/**
	 * Ask if an action on this StyleElement has been completed
	 * @param action for example ACTION_VALIDATE
	 * @return true if the action has already been performed
	 */

	public boolean isActionCompleted(int action) {
		return (actionsCompleted & action) != 0;
	}

	/**
	 * Say that an action on this StyleElement has been completed
	 * @param action for example ACTION_VALIDATE
	 */

	public void setActionCompleted(int action) {
		actionsCompleted |= action;
	}

}

