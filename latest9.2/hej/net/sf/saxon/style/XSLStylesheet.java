package net.sf.saxon.style;

import net.sf.saxon.Configuration;
import net.sf.saxon.PreparedStylesheet;
import net.sf.saxon.event.SaxonOutputKeys;
import net.sf.saxon.expr.CollationMap;
import net.sf.saxon.expr.Expression;
import net.sf.saxon.functions.*;
import net.sf.saxon.instruct.Executable;
import net.sf.saxon.instruct.LocationMap;
import net.sf.saxon.om.*;
import net.sf.saxon.query.XQueryFunction;
import net.sf.saxon.query.XQueryFunctionLibrary;
import net.sf.saxon.sort.CodepointCollator;
import net.sf.saxon.sort.IntHashMap;
import net.sf.saxon.sort.StringCollator;
import net.sf.saxon.sort.IntIterator;
import net.sf.saxon.trans.*;
import net.sf.saxon.type.Type;
import net.sf.saxon.value.Whitespace;

import java.util.*;

/**
 * An xsl:stylesheet or xsl:transform element in the stylesheet. <br>
 * Note this element represents a stylesheet module, not necessarily
 * the whole stylesheet. However, much of the functionality (and the fields)
 * are relevant only to the top-level module.
 */

public class XSLStylesheet extends StyleElement {

    Executable exec;

    // the Location Map keeps track of modules and line numbers of expressions and instructions
    private LocationMap locationMap = new LocationMap();

    // index of global variables and parameters, by StructuredQName
    // (overridden variables are excluded).
    // Used at compile-time only, except for debugging
    private HashMap<StructuredQName, XSLVariableDeclaration> globalVariableIndex =
            new HashMap<StructuredQName, XSLVariableDeclaration>(20);

    // true if this stylesheet was included by xsl:include, false if it is the
    // principal stylesheet or if it was imported
    private boolean wasIncluded = false;

    // the import precedence for top-level elements in this stylesheet
    private int precedence = 0;

    // the lowest precedence of any stylesheet imported by this one
    private int minImportPrecedence = 0;

    // the StyleSheet module that included or imported this one; null for the principal stylesheet
    private XSLStylesheet importer = null;

    // the PreparedStylesheet object used to load this stylesheet
    private PreparedStylesheet stylesheet;

    // the top-level elements in this logical stylesheet (after include/import)
    private List topLevel;

    // table of named templates. Key is the integer fingerprint of the template name;
    // value is the XSLTemplate object in the source stylesheet.
    private HashMap<StructuredQName, XSLTemplate> templateIndex =
            new HashMap<StructuredQName, XSLTemplate>(20);

    // Table of named stylesheet functions. A two level lookup, using first the arity and then
    // the expanded name of the function.
    private IntHashMap<HashMap<StructuredQName, XSLFunction>> functionIndex =
            new IntHashMap(8);

    // the value of the inputTypeAnnotations attribute on this module, combined with the values
    // on all imported/included modules. This is a combination of the bit-significant values
    // ANNOTATION_STRIP and ANNOTATION_PRESERVE.
    private int inputAnnotations = 0;
    public static final int ANNOTATION_STRIP = 1;
    public static final int ANNOTATION_PRESERVE = 2;

    // table of imported schemas. The members of this set are strings holding the target namespace.
    private HashSet<String> schemaIndex = new HashSet<String>(10);

    // table of functions imported from XQuery library modules
    private XQueryFunctionLibrary queryFunctions;

    // namespace aliases. This information is needed at compile-time only
    private int numberOfAliases = 0;
    private List<XSLNamespaceAlias> namespaceAliasList = new ArrayList<XSLNamespaceAlias>(5);
    private short[] aliasSCodes;
    private int[] aliasNCodes;

    // count of the maximum number of local variables in xsl:template match patterns
    private int largestPatternStackFrame = 0;

    // default validation
    private int defaultValidation = Validation.STRIP;

    // library of functions that are in-scope for XPath expressions in this stylesheet
    private FunctionLibraryList functionLibrary;

    // flag: true if there's an xsl:result-document that uses a dynamic format
    private boolean needsDynamicOutputProperties = false;

    // map for allocating unique numbers to local parameter names. Key is a
    // StructuredQName; value is a boxed int. Use only on the principal module.
    private HashMap<StructuredQName, Integer> localParameterNumbers = null;

    /**
     * Create link to the owning PreparedStylesheet object
     * @param sheet the PreparedStylesheet
     */

    public void setPreparedStylesheet(PreparedStylesheet sheet) {
        Configuration config = sheet.getConfiguration();
        //System.err.println("XSLStylesheet: config is " + config.getClass().getName());
        stylesheet = sheet;
        this.exec = sheet.getExecutable();

        functionLibrary = new FunctionLibraryList();
        functionLibrary.addFunctionLibrary(
                SystemFunctionLibrary.getSystemFunctionLibrary(StandardFunction.CORE|StandardFunction.XSLT));
        functionLibrary.addFunctionLibrary(new StylesheetFunctionLibrary(this, true));
        functionLibrary.addFunctionLibrary(config.getVendorFunctionLibrary());
        functionLibrary.addFunctionLibrary(new ConstructorFunctionLibrary(config));
        queryFunctions = new XQueryFunctionLibrary(config);
        functionLibrary.addFunctionLibrary(queryFunctions);
        functionLibrary.addFunctionLibrary(config.getIntegratedFunctionLibrary());
        config.addExtensionBinders(functionLibrary);
        functionLibrary.addFunctionLibrary(new StylesheetFunctionLibrary(this, false));

    }

    /**
     * Get the owning PreparedStylesheet object
     */

    public PreparedStylesheet getPreparedStylesheet() {
        if (importer != null) {
            return importer.getPreparedStylesheet();
        }
        return stylesheet;
    }

    /**
     * Get the run-time Executable object
     */

    public Executable getExecutable() {
        return exec;
    }

    protected boolean mayContainParam(String attName) {
        return true;
    }

    /**
     * Get the function library. Available only on the principal stylesheet module
     * @return the function library
     */

    public FunctionLibrary getFunctionLibrary() {
        return functionLibrary;
    }

    /**
     * Get the locationMap object
     * @return the LocationMap
     */

    public LocationMap getLocationMap() {
        return locationMap;
    }

    /**
     * Get the RuleManager which handles template rules
     * @return the template rule manager
     */

    public RuleManager getRuleManager() {
        return exec.getRuleManager();
    }

    /**
     * Get the rules determining which nodes are to be stripped from the tree
     * @return the Mode object holding the whitespace stripping rules. The stripping
     * rules defined in xsl:strip-space are managed in the same way as template rules,
     * hence the use of a special Mode object
     */

    protected Mode getStripperRules() {
        if (exec.getStripperRules() == null) {
            exec.setStripperRules(new Mode(Mode.STRIPPER_MODE, Mode.DEFAULT_MODE_NAME));
        }
        return exec.getStripperRules();
    }

    /**
     * Determine whether this stylesheet does any whitespace stripping
     * @return true if this stylesheet strips whitespace from source documents
     */

    public boolean stripsWhitespace() {
        for (int i = 0; i < topLevel.size(); i++) {
            NodeInfo s = (NodeInfo) topLevel.get(i);
            if (s.getFingerprint() == StandardNames.XSL_STRIP_SPACE) {
                return true;
            }
        }
        return false;
    }

    /**
     * Get the KeyManager which handles key definitions
     * @return the key manager
     */

    public KeyManager getKeyManager() {
        if (exec.getKeyManager() == null) {
            exec.setKeyManager(new KeyManager(getConfiguration()));
        }
        return exec.getKeyManager();
    }

    /**
     * Get the collation map
     * @return the CollationMap
     */

    public CollationMap getCollationMap() {
        return exec.getCollationTable();
    }

    /**
     * Register a named collation (actually a StringCollator)
     * @param name the name of the collation
     * @param collation the StringCollator that implements this collation
     */

    public void setCollation(String name, StringCollator collation) {
        if (exec.getCollationTable() == null) {
            exec.setCollationTable(new CollationMap(getConfiguration()));
        }
        exec.getCollationTable().setNamedCollation(name, collation);
    }

    /**
     * Find a named collation. Note this method should only be used at compile-time, before declarations
     * have been pre-processed. After that time, use getCollation().
     * @param name identifies the name of the collation required
     * @return null if the collation is not found
     */

    protected StringCollator findCollation(String name) {

        if (name.equals(NamespaceConstant.CODEPOINT_COLLATION_URI)) {
            return CodepointCollator.getInstance();
        }

        // First try to find it in the table

        StringCollator c = null;

        if (exec.getCollationTable() != null) {
            c = exec.getCollationTable().getNamedCollation(name);
        }
        if (c != null) return c;

        // At compile-time, the collation might not yet be in the table. So search for it

        XSLStylesheet stylesheet = getPrincipalStylesheet();
        List toplevel = stylesheet.getTopLevel();

        // search for a matching collation name, starting at the end in case of duplicates.
        // this also ensures we get the one with highest import precedence.
        for (int i = toplevel.size() - 1; i >= 0; i--) {
            if (toplevel.get(i) instanceof CollationDeclaration) {
                CollationDeclaration t = (CollationDeclaration) toplevel.get(i);
                if (t.getCollationName().equals(name)) {
                    return t.getCollator();
                }
            }
        }

        Configuration config = getConfiguration();
        return config.getCollationURIResolver().resolve(name, getBaseURI(), config);
    }

    /**
     * Get a character map, identified by the fingerprint of its name.
     * Search backwards through the stylesheet.
     * @param name The character map name being sought
     * @return the identified character map, or null if not found
     */

    public XSLCharacterMap getCharacterMap(StructuredQName name) {
        for (int i = topLevel.size() - 1; i >= 0; i--) {
            if (topLevel.get(i) instanceof XSLCharacterMap) {
                XSLCharacterMap t = (XSLCharacterMap) topLevel.get(i);
                if (t.getCharacterMapName().equals(name)) {
                    return t;
                }
            }
        }
        return null;
    }

    /**
     * Set the import precedence of this stylesheet
     * @param prec the import precedence. Higher numbers indicate higher precedence, but the actual
     * number has no significance
     */

    public void setPrecedence(int prec) {
        precedence = prec;
    }

    /**
     * Get the import precedence of this stylesheet
     */

    public int getPrecedence() {
        if (wasIncluded) return importer.getPrecedence();
        return precedence;
    }

    /**
     * Get the minimum import precedence of this stylesheet, that is, the lowest precedence
     * of any stylesheet imported by this one
     * @return the minimum precedence of imported stylesheet modules
     */

    public int getMinImportPrecedence() {
        return minImportPrecedence;
    }

    /**
     * Set the minimum import precedence of this stylesheet, that is, the lowest precedence
     * of any stylesheet imported by this one
     * @param precedence the precedence of the first stylesheet module that this one imports
     */

    public void setMinImportPrecedence(int precedence) {
        minImportPrecedence = precedence;
    }

    /**
     * Set the StyleSheet that included or imported this one.
     * @param importer the stylesheet module that included or imported this module
     */

    public void setImporter(XSLStylesheet importer) {
        this.importer = importer;
    }

    /**
     * Get the StyleSheet that included or imported this one.
     * @return null if this is the principal stylesheet
     */

    public XSLStylesheet getImporter() {
        return importer;
    }

    /**
     * Indicate that this stylesheet was included (by its "importer") using an xsl:include
     * statement as distinct from xsl:import
     */

    public void setWasIncluded() {
        wasIncluded = true;
    }

    /**
     * Get the top level elements in this stylesheet, after applying include/import
     * @return a list of top-level elements in this stylesheet module or in those
     * modules that it includes or imports
     */

    public List getTopLevel() {
        return topLevel;
    }

    /**
     * Allocate a slot number for a global variable or parameter
     * @param qName the name of the variable or parameter
     * @return int the allocated slot number
     */

    public int allocateGlobalSlot(StructuredQName qName) {
        return exec.getGlobalVariableMap().allocateSlotNumber(qName);
    }

    /**
     * Ensure there is enough space for local variables or parameters when evaluating the match pattern of
     * template rules
     * @param n the number of slots to be allocated
     */

    public void allocatePatternSlots(int n) {
        if (n > largestPatternStackFrame) {
            largestPatternStackFrame = n;
        }
    }

    /**
     * Prepare the attributes on the stylesheet element
     */

    public void prepareAttributes() throws XPathException {

        String inputTypeAnnotationsAtt = null;
        AttributeCollection atts = getAttributeList();
        for (int a = 0; a < atts.getLength(); a++) {

            int nc = atts.getNameCode(a);
            String f = getNamePool().getClarkName(nc);
            if (f.equals(StandardNames.VERSION)) {
                // already processed
            } else if (f.equals(StandardNames.ID)) {
                //
            } else if (f.equals(StandardNames.EXTENSION_ELEMENT_PREFIXES)) {
                //
            } else if (f.equals(StandardNames.EXCLUDE_RESULT_PREFIXES)) {
                //
            } else if (f.equals(StandardNames.DEFAULT_VALIDATION)) {
                defaultValidation = Validation.getCode(atts.getValue(a));
                if (defaultValidation == Validation.INVALID) {
                    compileError("Invalid value for default-validation attribute. " +
                            "Permitted values are (strict, lax, preserve, strip)", "XTSE0020");
                } else if (!getExecutable().isSchemaAware() && defaultValidation != Validation.STRIP) {
                    defaultValidation = Validation.STRIP;
                    compileError("default-validation='" + atts.getValue(a) + "' requires a schema-aware processor",
                            "XTSE1660");
                }
            } else if (f.equals(StandardNames.INPUT_TYPE_ANNOTATIONS)) {
                inputTypeAnnotationsAtt = atts.getValue(a);
            } else {
                checkUnknownAttribute(nc);
            }
        }
        if (version == null) {
            reportAbsence("version");
        }

        if (inputTypeAnnotationsAtt != null) {
            if (inputTypeAnnotationsAtt.equals("strip")) {
                setInputTypeAnnotations(ANNOTATION_STRIP);
            } else if (inputTypeAnnotationsAtt.equals("preserve")) {
                setInputTypeAnnotations(ANNOTATION_PRESERVE);
            } else if (inputTypeAnnotationsAtt.equals("unspecified")) {
                //
            } else {
                compileError("Invalid value for input-type-annotations attribute. " +
                             "Permitted values are (strip, preserve, unspecified)", "XTSE0020");
            }
        }

    }

    /**
     * Get the value of the default validation attribute
     * @return the value of the default-validation attribute, as a constant such
     * as {@link Validation#STRIP}
     */

    public int getDefaultValidation() {
        return defaultValidation;
    }


    /**
     * Get the value of the input-type-annotations attribute, for this module alone.
     * The value is an or-ed combination of the two bits
     * {@link #ANNOTATION_STRIP} and {@link #ANNOTATION_PRESERVE}
     * @return the value if the input-type-annotations attribute in this stylesheet module
     */

    public int getInputTypeAnnotationsAttribute() throws XPathException {
        String inputTypeAnnotationsAtt = getAttributeValue("", StandardNames.INPUT_TYPE_ANNOTATIONS);
        if (inputTypeAnnotationsAtt != null) {
            if (inputTypeAnnotationsAtt.equals("strip")) {
                setInputTypeAnnotations(ANNOTATION_STRIP);
            } else if (inputTypeAnnotationsAtt.equals("preserve")) {
                setInputTypeAnnotations(ANNOTATION_PRESERVE);
            } else if (inputTypeAnnotationsAtt.equals("unspecified")) {
                //
            } else {
                compileError("Invalid value for input-type-annotations attribute. " +
                             "Permitted values are (strip, preserve, unspecified)", "XTSE0020");
            }
        }
        return inputAnnotations;
    }


    /**
     * Get the value of the input-type-annotations attribute, for this module combined with that
     * of all included/imported modules. The value is an or-ed combination of the two bits
     * {@link #ANNOTATION_STRIP} and {@link #ANNOTATION_PRESERVE}
     * @return the value of the input-type-annotations attribute, for this module combined with that
     * of all included/imported modules
     */

    public int getInputTypeAnnotations() {
        return inputAnnotations;
    }

    /**
     * Set the value of the input-type-annotations attribute, for this module combined with that
     * of all included/imported modules. The value is an or-ed combination of the two bits
     * {@link #ANNOTATION_STRIP} and {@link #ANNOTATION_PRESERVE}
     * @param annotations the value of the input-type-annotations attribute, for this module combined with that
     * of all included/imported modules.
     */

    public void setInputTypeAnnotations(int annotations) throws XPathException {
        inputAnnotations |= annotations;
        if (inputAnnotations == (ANNOTATION_STRIP | ANNOTATION_PRESERVE)) {
            compileError("One stylesheet module specifies input-type-annotations='strip', " +
                    "another specifies input-type-annotations='preserve'", "XTSE0265");
        }
    }

    /**
     * Get the declared namespace alias for a given namespace URI code if there is one.
     * If there is more than one, we get the last.
     * @param uriCode The code of the uri used in the stylesheet.
     * @return The namespace code to be used (prefix in top half, uri in bottom half): return -1
     * if no alias is defined
     */

    protected int getNamespaceAlias(short uriCode) {

        // if there are several matches, the last in stylesheet takes priority;
        // but the list is in reverse stylesheet order
        for (int i = 0; i < numberOfAliases; i++) {
            if (uriCode == aliasSCodes[i]) {
                return aliasNCodes[i];
            }
        }
        return uriCode;
    }

    /**
     * Determine if a namespace is included in the result-prefix of a namespace-alias
     * @param uriCode the namepool code of the URI
     * @return true if an xsl:namespace-alias has been defined for this namespace URI
     */

    protected boolean isAliasResultNamespace(short uriCode) {
        for (int i = 0; i < numberOfAliases; i++) {
            if (uriCode == (aliasNCodes[i] & 0xffff)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Validate this element
     */

    public void validate() throws XPathException {
        if (validationError != null) {
            compileError(validationError);
        }
        if (getParent().getNodeKind() != Type.DOCUMENT) {
            compileError(getDisplayName() + " must be the outermost element", "XTSE0010");
        }

        AxisIterator kids = iterateAxis(Axis.CHILD);
        while(true) {
            NodeInfo curr = (NodeInfo)kids.next();
            if (curr == null) break;
            if (curr.getNodeKind() == Type.TEXT ||
                    curr instanceof XSLTemplate ||
                    curr instanceof XSLImport ||
                    curr instanceof XSLInclude ||
                    curr instanceof XSLAttributeSet ||
                    curr instanceof XSLCharacterMap ||
                    curr instanceof XSLDecimalFormat ||
                    curr instanceof XSLFunction ||
                    curr instanceof XSLImportSchema ||
                    curr instanceof XSLKey ||
                    curr instanceof XSLNamespaceAlias ||
                    curr instanceof XSLOutput ||
                    curr instanceof XSLParam ||
                    curr instanceof XSLPreserveSpace ||
                    curr instanceof XSLVariable ||
                    curr instanceof DataElement) {
                // all is well
            } else if (!NamespaceConstant.XSLT.equals(curr.getURI()) && !"".equals(curr.getURI())) {
                // elements in other namespaces are allowed and ignored
            } else if (curr instanceof AbsentExtensionElement && ((StyleElement)curr).forwardsCompatibleModeIsEnabled()) {
                // this is OK: an unknown XSLT element is allowed in forwards compatibility mode
            } else if (NamespaceConstant.XSLT.equals(curr.getURI())) {
                ((StyleElement)curr).compileError("Element " + curr.getDisplayName() +
                        " must not appear directly within " + getDisplayName(), "XTSE0010");
            } else {
                ((StyleElement)curr).compileError("Element " + curr.getDisplayName() +
                        " must not appear directly within " + getDisplayName() +
                        " because it is not in a namespace", "XTSE0130");
            }
        }
    }

    /**
     * Preprocess does all the processing possible before the source document is available.
     * It is done once per stylesheet, so the stylesheet can be reused for multiple source
     * documents. The method is called only on the XSLStylesheet element representing the
     * principal stylesheet module
     */

    public void preprocess() throws XPathException {

        // process any xsl:include and xsl:import elements

        spliceIncludes();

        // build indexes for selected top-level elements

        buildIndexes();

        // check for use of schema-aware constructs

        checkForSchemaAwareness();

        // process the attributes of every node in the tree

        processAllAttributes();

        // collect any namespace aliases

        collectNamespaceAliases();

        // fix up references from XPath expressions to variables and functions, for static typing

        for (int i = 0; i < topLevel.size(); i++) {
            Object node = topLevel.get(i);
            if (node instanceof StyleElement) {
                ((StyleElement) node).fixupReferences();
            }
        }

        // Validate the whole logical style sheet (i.e. with included and imported sheets)

        validate();
        for (int i = 0; i < topLevel.size(); i++) {
            Object node = topLevel.get(i);
            if (node instanceof StyleElement) {
                ((StyleElement) node).validateSubtree();
            }
        }
    }

    /**
     * Process xsl:include and xsl:import elements.
     */

    public void spliceIncludes() throws XPathException {

        boolean foundNonImport = false;
        topLevel = new ArrayList(50);
        minImportPrecedence = precedence;
        StyleElement previousElement = this;

        AxisIterator kids = iterateAxis(Axis.CHILD);

        while (true) {
            NodeInfo child = (NodeInfo) kids.next();
            if (child == null) {
                break;
            }
            if (child.getNodeKind() == Type.TEXT) {
                // in an embedded stylesheet, white space nodes may still be there
                if (!Whitespace.isWhite(child.getStringValueCS())) {
                    previousElement.compileError(
                            "No character data is allowed between top-level elements", "XTSE0120");
                }

            } else if (child instanceof DataElement) {
                foundNonImport = true;
            } else {
                previousElement = (StyleElement) child;
                if (child instanceof XSLGeneralIncorporate) {
                    XSLGeneralIncorporate xslinc = (XSLGeneralIncorporate) child;
                    xslinc.processAttributes();

                    if (xslinc.isImport()) {
                        if (foundNonImport) {
                            xslinc.compileError("xsl:import elements must come first", "XTSE0200");
                        }
                    } else {
                        foundNonImport = true;
                    }

                    // get the included stylesheet. This follows the URL, builds a tree, and splices
                    // in any indirectly-included stylesheets.

                    XSLStylesheet inc =
                            xslinc.getIncludedStylesheet(this, precedence);
                    if (inc == null) return;  // error has been reported

                    // after processing the imported stylesheet and any others it brought in,
                    // adjust the import precedence of this stylesheet if necessary

                    if (xslinc.isImport()) {
                        precedence = inc.getPrecedence() + 1;
                    } else {
                        precedence = inc.getPrecedence();
                        inc.setMinImportPrecedence(minImportPrecedence);
                        inc.setWasIncluded();
                    }

                    // copy the top-level elements of the included stylesheet into the top level of this
                    // stylesheet. Normally we add these elements at the end, in order, but if the precedence
                    // of an element is less than the precedence of the previous element, we promote it.
                    // This implements the requirement in the spec that when xsl:include is used to
                    // include a stylesheet, any xsl:import elements in the included document are moved
                    // up in the including document to after any xsl:import elements in the including
                    // document.

                    List incchildren = inc.topLevel;
                    for (int j = 0; j < incchildren.size(); j++) {
                        StyleElement elem = (StyleElement) incchildren.get(j);
                        int last = topLevel.size() - 1;
                        if (last < 0 || elem.getPrecedence() >= ((StyleElement) topLevel.get(last)).getPrecedence()) {
                            topLevel.add(elem);
                        } else {
                            while (last >= 0 && elem.getPrecedence() < ((StyleElement) topLevel.get(last)).getPrecedence()) {
                                last--;
                            }
                            topLevel.add(last + 1, elem);
                        }
                    }
                } else {
                    foundNonImport = true;
                    topLevel.add(child);
                }
            }
        }
    }

    /**
     * Build indexes for selected top-level declarations
     */

    private void buildIndexes() throws XPathException {
    // Scan the declarations in reverse order
        for (int i = topLevel.size() - 1; i >= 0; i--) {
            Object node = topLevel.get(i);
            if (node instanceof StyleElement) {
                ((StyleElement)node).index(this);
            }
        }
        // Now seal all the schemas that have been imported to guarantee consistency with instance documents
        Configuration config = getConfiguration();
        Iterator<String> iter = schemaIndex.iterator();
        while (iter.hasNext()) {
            String ns = iter.next();
            config.sealNamespace(ns);
        }
    }

   
    protected void addNamespaceAlias(XSLNamespaceAlias node) {
        namespaceAliasList.add(node);
        numberOfAliases++;
    }

    /**
     * Check for schema-awareness.
     * Typed input nodes are recognized if and only if the stylesheet contains an import-schema declaration.
     */

    private void checkForSchemaAwareness() {
        Executable exec = getExecutable();
        if (!exec.isSchemaAware() && exec.getConfiguration().isLicensedFeature(Configuration.LicenseFeature.SCHEMA_AWARE_XSLT)) {
            for (int i = 0; i < topLevel.size(); i++) {
                Object node = topLevel.get(i);
                if (node instanceof XSLImportSchema) {
                    exec.setSchemaAware(true);
                    return;
                }
            }
            //exec.setSchemaAware(false);
        }
    }

    /**
     * Index a global xsl:variable or xsl:param element
     * @param var The XSLVariable or XSLParam element
     * @throws XPathException
     */

    protected void indexVariableDeclaration(XSLVariableDeclaration var) throws XPathException {
        StructuredQName qName = var.getVariableQName();
        if (qName != null) {
            // see if there is already a global variable with this precedence
            XSLVariableDeclaration other = globalVariableIndex.get(qName);
            if (other == null) {
                // this is the first
                globalVariableIndex.put(qName, var);
            } else {
                // check the precedences
                int thisPrecedence = var.getPrecedence();
                int otherPrecedence = other.getPrecedence();
                if (thisPrecedence == otherPrecedence) {
                    var.compileError("Duplicate global variable declaration (see line " +
                            other.getLineNumber() + " of " + other.getSystemId() + ')', "XTSE0630");
                } else if (thisPrecedence < otherPrecedence) {
                    var.setRedundant();
                } else {
                    // can't happen, but we'll play safe
                    other.setRedundant();
                    globalVariableIndex.put(qName, var);
                }
            }
        }
    }

    /**
     * Add a named template to the index
     * @param template The Template object
     * @throws XPathException
     */
    protected void indexNamedTemplate(XSLTemplate template) throws XPathException {
        StructuredQName qName = template.getTemplateName();
        if (qName != null) {
            // see if there is already a named template with this precedence
            XSLTemplate other = templateIndex.get(qName);
            if (other == null) {
                // this is the first
                templateIndex.put(qName, template);
                exec.putNamedTemplate(qName, template.getCompiledTemplate());
            } else {
                // check the precedences
                int thisPrecedence = template.getPrecedence();
                int otherPrecedence = other.getPrecedence();
                if (thisPrecedence == otherPrecedence) {
                    template.compileError("Duplicate named template (see line " +
                            other.getLineNumber() + " of " + other.getSystemId() + ')', "XTSE0660");
                } else if (thisPrecedence < otherPrecedence) {
                    //template.setRedundantNamedTemplate();
                } else {
                    // can't happen, but we'll play safe
                    //other.setRedundantNamedTemplate();
                    templateIndex.put(qName, template);
                    exec.putNamedTemplate(qName, template.getCompiledTemplate());
                }
            }
        }
    }

    /**
      * Add a stylesheet function to the index
      * @param function The XSLFunction object
      * @throws XPathException
      */
     protected void indexFunction(XSLFunction function) throws XPathException {
         StructuredQName qName = function.getObjectName();
         int arity = function.getNumberOfArguments();

         // see if there is already a named function with this precedence
         XSLFunction other = getFunction(qName, arity);
         if (other == null) {
             // this is the first
             putFunction(function);
         } else {
             // check the precedences
             int thisPrecedence = function.getPrecedence();
             int otherPrecedence = other.getPrecedence();
             if (thisPrecedence == otherPrecedence) {
                 function.compileError("Duplicate function declaration (see line " +
                         other.getLineNumber() + " of " + other.getSystemId() + ')', "XTSE0770");
             } else if (thisPrecedence < otherPrecedence) {
                 //
             } else {
                 // can't happen, but we'll play safe
                 putFunction(function);
             }
         }
     }

     protected XSLFunction getFunction(StructuredQName name, int arity) {
        if (arity == -1) {
            // supports the single-argument function-available() function (slowly)
            for (IntIterator arities = functionIndex.keyIterator(); arities.hasNext();) {
                int a = arities.next();
                HashMap<StructuredQName, XSLFunction> m = functionIndex.get(a);
                if (m != null && m.get(name) != null) {
                    return m.get(name);
                }
            }
            return null;
        } else {
             HashMap<StructuredQName, XSLFunction> m = functionIndex.get(arity);
             return (m == null ? null : m.get(name));
        }
     }

     protected void putFunction(XSLFunction function) {
         StructuredQName qName = function.getObjectName();
         int arity = function.getNumberOfArguments();
         HashMap<StructuredQName, XSLFunction> m = functionIndex.get(arity);
         if (m == null) {
             m = new HashMap<StructuredQName, XSLFunction>();
             functionIndex.put(arity, m);
         }
         m.put(qName, function);
     }


    /**
     * Collect any namespace aliases
     */

    private void collectNamespaceAliases() throws XPathException {
        aliasSCodes = new short[numberOfAliases];
        aliasNCodes = new int[numberOfAliases];
        int precedenceBoundary = 0;
        int currentPrecedence = -1;
        // Note that we are processing the list in reverse stylesheet order,
        // that is, highest precedence first.
        for (int i = 0; i < numberOfAliases; i++) {
            XSLNamespaceAlias xna = namespaceAliasList.get(i);
            short scode = xna.getStylesheetURICode();
            int ncode = xna.getResultNamespaceCode();
            int prec = xna.getPrecedence();

            // check that there isn't a conflict with another xsl:namespace-alias
            // at the same precedence

            if (currentPrecedence != prec) {
                currentPrecedence = prec;
                precedenceBoundary = i;
            }

            for (int j = precedenceBoundary; j < i; j++) {
                if (scode == aliasSCodes[j]) {
                    if ((ncode & 0xffff) != (aliasNCodes[j] & 0xffff)) {
                        xna.compileError("More than one alias is defined for the same namespace prefix", "XTSE0810");
                    }
                }
            }

            aliasSCodes[i] = scode;
            aliasNCodes[i] = ncode;
        }
        namespaceAliasList = null;  // throw it in the garbage
    }

    protected boolean hasNamespaceAliases() {
        return numberOfAliases > 0;
    }

    /**
     * Process the attributes of every node in the stylesheet
     */

    public void processAllAttributes() throws XPathException {
        processDefaultCollationAttribute("");
        prepareAttributes();
        if (topLevel == null) return;   // can happen if xsl:stylesheet appears in the wrong place
        for (int i = 0; i < topLevel.size(); i++) {
            Object s = topLevel.get(i);
            if (s instanceof StyleElement) {
                try {
                    ((StyleElement) s).processAllAttributes();
                } catch (XPathException err) {
                    ((StyleElement) s).compileError(err);
                }
            }
        }
    }

    /**
     * Get the global variable or parameter with a given name (taking
     * precedence rules into account)
     * @param qName name of the global variable or parameter
     * @return the variable declaration
     */

    public XSLVariableDeclaration getGlobalVariable(StructuredQName qName) {
        return globalVariableIndex.get(qName);
    }

    /**
     * Set that this stylesheet needs dynamic output properties
     * @param b true if this stylesheet needs dynamic output properties
     */

    public void setNeedsDynamicOutputProperties(boolean b) {
        needsDynamicOutputProperties = b;
    }

    /**
     * Create an output properties object representing the xsl:output elements in the stylesheet.
     * @param formatQName The name of the output format required. If set to null, gathers
     * information for the unnamed output format
     * @return the Properties object containing the details of the specified output format
     * @throws XPathException if a named output format does not exist in
     * the stylesheet
     */

    public Properties gatherOutputProperties(StructuredQName formatQName) throws XPathException {
        boolean found = (formatQName == null);
        Properties details = new Properties(getConfiguration().getDefaultSerializationProperties());
        HashMap precedences = new HashMap(10);
        for (int i = topLevel.size()-1; i >= 0; i--) {
            Object s = topLevel.get(i);
            if (s instanceof XSLOutput) {
                XSLOutput xo = (XSLOutput) s;
                if (formatQName == null
                        ? xo.getFormatQName() == null
                        : formatQName.equals(xo.getFormatQName())) {
                    found = true;
                    xo.gatherOutputProperties(details, precedences);
                }
            }
        }
        if (!found) {
            compileError("Requested output format " + formatQName.getDisplayName() +
                    " has not been defined", "XTDE1460");
        }
        return details;
    }

    /**
     * Declare an imported XQuery function
     * @param function the imported function
     */

    public void declareXQueryFunction(XQueryFunction function) throws XPathException {
        queryFunctions.declareFunction(function);
    }

    /**
     * Get an imported schema with a given namespace
     * @param targetNamespace The target namespace of the required schema.
     * Supply an empty string for the default namespace
     * @return the required Schema, or null if no such schema has been imported
     */

    protected boolean isImportedSchema(String targetNamespace) {
        return schemaIndex.contains(targetNamespace);
    }

    protected void addImportedSchema(String targetNamespace) {
        schemaIndex.add(targetNamespace);
    }

    protected HashSet<String> getImportedSchemaTable() {
        return schemaIndex;
    }

    /**
     * Compile the stylesheet to create an executable.
     */

    public void compileStylesheet() throws XPathException {

        try {

            PreparedStylesheet pss = getPreparedStylesheet();
            // If any XQuery functions were imported, fix up all function calls
            // registered against these functions.
            try {
                //queryFunctions.bindUnboundFunctionCalls();
                Iterator qf = queryFunctions.getFunctionDefinitions();
                while (qf.hasNext()) {
                    XQueryFunction f = (XQueryFunction) qf.next();
                    f.fixupReferences(getStaticContext());
                }
            } catch (XPathException e) {
                compileError(e);
            }

            // Call compile method for each top-level object in the stylesheet

            for (int i = 0; i < topLevel.size(); i++) {
                NodeInfo node = (NodeInfo) topLevel.get(i);
                if (node instanceof StyleElement) {
                    StyleElement snode = (StyleElement) node;
                    //int module = putModuleNumber(snode.getSystemId());
                    Expression inst = snode.compile(exec);
                    if (inst != null) {
                        inst.setLocationId(allocateLocationId(getSystemId(), snode.getLineNumber()));
                    }
                }
            }

            // Call type-check method for each user-defined function in the stylesheet. This is no longer
            // done during the optimize step, to avoid functions being inlined before they are type-checked.

            for (int i = 0; i < topLevel.size(); i++) {
                NodeInfo node = (NodeInfo) topLevel.get(i);
                if (node instanceof XSLFunction) {
                    ((XSLFunction) node).typeCheckBody();
                }
            }

            // Call optimize method for each top-level object in the stylesheet

            for (int i = 0; i < topLevel.size(); i++) {
                NodeInfo node = (NodeInfo) topLevel.get(i);
                if (node instanceof StylesheetProcedure) {
                    ((StylesheetProcedure) node).optimize();
                }
            }

            // Fix up references to the default default decimal format

            if (pss.getDecimalFormatManager() != null) {
                try {
                    pss.getDecimalFormatManager().fixupDefaultDefault();
                } catch (XPathException err) {
                    compileError(err.getMessage(), err.getErrorCodeQName());
                }
            }

            getPrincipalStylesheet().getStripperRules().computeRankings();
            exec.setStripsWhitespace(stripsWhitespace());
            
            Properties props = gatherOutputProperties(null);
            props.setProperty(SaxonOutputKeys.STYLESHEET_VERSION, getVersion().toString());
            exec.setDefaultOutputProperties(props);

            // handle named output formats for use at run-time
            HashSet<StructuredQName> outputNames = new HashSet<StructuredQName>(5);
            for (int i=0; i<topLevel.size(); i++) {
                Object child = topLevel.get(i);
                if (child instanceof XSLOutput) {
                    XSLOutput out = (XSLOutput)child;
                    StructuredQName qName = out.getFormatQName();
                    if (qName != null) {
                        outputNames.add(qName);
                    }
                }
            }
            if (outputNames.isEmpty()) {
                if (needsDynamicOutputProperties) {
                    compileError("The stylesheet contains xsl:result-document instructions that calculate the output " +
                            "format name at run-time, but there are no named xsl:output declarations", "XTDE1460");
                }
            } else {
                for (Iterator<StructuredQName> iter = outputNames.iterator(); iter.hasNext();) {
                    StructuredQName qName = iter.next();
                    Properties oprops = gatherOutputProperties(qName);
                    if (needsDynamicOutputProperties) {
                        exec.setOutputProperties(qName, oprops);
                    }
                }
            }

            exec.setPatternSlotSpace(largestPatternStackFrame);
            exec.setStripsInputTypeAnnotations(inputAnnotations == ANNOTATION_STRIP);

            // Build the index of named character maps

            for (int i = 0; i < topLevel.size(); i++) {
                if (topLevel.get(i) instanceof XSLCharacterMap) {
                    XSLCharacterMap t = (XSLCharacterMap) topLevel.get(i);
                    if (!t.isRedundant()) {
                        StructuredQName qn = t.getCharacterMapName();
                        IntHashMap<String> map = new IntHashMap<String>(20);
                        t.assemble(map);
                        if (exec.getCharacterMapIndex() == null) {
                            exec.setCharacterMapIndex(new HashMap<StructuredQName, IntHashMap<String>>(20));
                        }
                        exec.getCharacterMapIndex().put(qn, map);
                    }
                }
            }

            // Finish off the lists of template rules

            getRuleManager().computeRankings();
            getRuleManager().invertStreamableTemplates(getConfiguration().getOptimizer());

            // Build a run-time function library. This supports the use of function-available()
            // with a dynamic argument, and extensions such as saxon:evaluate().

            ExecutableFunctionLibrary overriding = new ExecutableFunctionLibrary(getConfiguration());
            ExecutableFunctionLibrary underriding = new ExecutableFunctionLibrary(getConfiguration());

            for (int i=0; i<topLevel.size(); i++) {
                Object child = topLevel.get(i);
                if (child instanceof XSLFunction) {
                    XSLFunction func = (XSLFunction)child;
                    if (func.isOverriding()) {
                        overriding.addFunction(func.getCompiledFunction());
                    } else {
                        underriding.addFunction(func.getCompiledFunction());
                    }
                }
            }

            Configuration config = getConfiguration();
            FunctionLibraryList libraryList = new FunctionLibraryList();
            libraryList.addFunctionLibrary(
                    SystemFunctionLibrary.getSystemFunctionLibrary(StandardFunction.CORE|StandardFunction.XSLT));
            libraryList.addFunctionLibrary(overriding);
            libraryList.addFunctionLibrary(config.getVendorFunctionLibrary());
            libraryList.addFunctionLibrary(new ConstructorFunctionLibrary(getConfiguration()));
            libraryList.addFunctionLibrary(queryFunctions);
            libraryList.addFunctionLibrary(config.getIntegratedFunctionLibrary());
            config.addExtensionBinders(libraryList);
            libraryList.addFunctionLibrary(underriding);
            exec.setFunctionLibrary(libraryList);

        } catch (RuntimeException err) {
        // if syntax errors were reported earlier, then exceptions may occur during this phase
        // due to inconsistency of data structures. We can ignore these exceptions as they
        // will go away when the user corrects the stylesheet
            if (getPreparedStylesheet().getErrorCount() == 0) {
                // rethrow the exception
                throw err;
            }
        }

    }

    /**
     * Dummy compile() method to satisfy the interface
     */

    public Expression compile(Executable exec) {
        return null;
    }

    /**
     * Allocate a unique number to a local parameter name. This should only be called on the principal
     * stylesheet module.
     * @param qName the local parameter name
     * @return an integer that uniquely identifies this parameter name within the stylesheet
     */

    public int allocateUniqueParameterNumber(StructuredQName qName) {
        if (localParameterNumbers == null) {
            localParameterNumbers = new HashMap<StructuredQName, Integer>(50);
        }
        Integer x = localParameterNumbers.get(qName);
        if (x == null) {
            x = new Integer(localParameterNumbers.size());
            localParameterNumbers.put(qName, x);
        }
        return x.intValue();
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
// Contributor(s):
//
