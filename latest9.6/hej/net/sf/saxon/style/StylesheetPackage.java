////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
// Copyright (c) 2014 Saxonica Limited.
// This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
// If a copy of the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
// This Source Code Form is "Incompatible With Secondary Licenses", as defined by the Mozilla Public License, v. 2.0.
////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

package net.sf.saxon.style;

import net.sf.saxon.Configuration;
import net.sf.saxon.PreparedStylesheet;
import net.sf.saxon.expr.Component;
import net.sf.saxon.expr.ComponentBinding;
import net.sf.saxon.expr.PackageData;
import net.sf.saxon.expr.instruct.*;
import net.sf.saxon.expr.parser.Optimizer;
import net.sf.saxon.functions.*;
import net.sf.saxon.lib.NamespaceConstant;
import net.sf.saxon.lib.SaxonOutputKeys;
import net.sf.saxon.om.*;
import net.sf.saxon.query.XQueryFunction;
import net.sf.saxon.query.XQueryFunctionLibrary;
import net.sf.saxon.serialize.CharacterMap;
import net.sf.saxon.serialize.CharacterMapIndex;
import net.sf.saxon.trace.ExpressionPresenter;
import net.sf.saxon.trans.*;
import net.sf.saxon.value.DecimalValue;
import net.sf.saxon.value.NestedIntegerValue;
import net.sf.saxon.z.IntHashMap;

import java.util.*;
import java.util.Collection;


/**
 * Represents both the stylesheet module at the root of the import tree, that is, the module
 * that includes or imports all the others, and also the XSLT package that has this stylesheet
 * module as its root.
 *
 * This version of the StylesheetPackage class represents a trivial package that is constructed
 * by the system to wrap an ordinary (non-package) stylesheet, as available in XSLT 2.0. A subclass
 * is used for "real" packages, currently available only in Saxon-EE.
 */
public class StylesheetPackage extends StylesheetModule implements GlobalVariableManager {

    /**
     * A class that simply encapsulates a callback action of any kind
     */

    public static abstract class Action {
        public abstract void doAction() throws XPathException;
    }

    private NestedIntegerValue packageVersionOLD = null;
    private PackageVersion packageVersion = null;
    private String packageName;
    private List<StylesheetPackage> usedPackages = new ArrayList<StylesheetPackage>();

    // table of imported schemas. The members of this set are strings holding the target namespace.
    protected HashSet<String> schemaIndex = new HashSet<String>(10);

    // table of functions imported from XQuery library modules
    private XQueryFunctionLibrary queryFunctions;

    // library of functions that are in-scope for XPath expressions in this stylesheet
    private FunctionLibraryList functionLibrary;

    // library of stylesheet functions
    private FunctionLibraryList stylesheetFunctionLibrary;

    private ExecutableFunctionLibrary overriding;
    private ExecutableFunctionLibrary underriding;

    // version attribute on xsl:stylesheet element of principal stylesheet module
    private DecimalValue version;

    // index of global variables and parameters, by StructuredQName
    // (overridden variables are excluded).
    private HashMap<StructuredQName, ComponentDeclaration> globalVariableIndex =
            new HashMap<StructuredQName, ComponentDeclaration>(20);

    // Table of components declared in this package or imported from used packages. Key is the symbolic identifier of the
    // component; value is the component itself. Hidden components are not included in this table because their names
    // need not be unique, and because they are not available for reference by name.
    // Currently used only for named templates
    private HashMap<SymbolicName, Component> componentIndex =
            new HashMap<SymbolicName, Component>(20);

    private List<Component> hiddenComponents = new ArrayList<Component>(); // not currently used

    // index of templates - only includes those actually declared within this package
    private HashMap<StructuredQName, ComponentDeclaration> templateIndex = new HashMap<StructuredQName, ComponentDeclaration>(20);

    // Table of named stylesheet functions.
    private HashMap<SymbolicName, ComponentDeclaration> functionIndex =
            new HashMap<SymbolicName, ComponentDeclaration>(8);

    // map for allocating unique numbers to local parameter names. Key is a
    // StructuredQName; value is a boxed int.
    /*@Nullable*/ private HashMap<StructuredQName, Integer> localParameterNumbers = null;

    // key manager for all the xsl:key definitions in this package
    private KeyManager keyManager;

    // decimal format manager for all the xsl:decimal-format definitions in this package
    private DecimalFormatManager decimalFormatManager;

    // rule manager for template rules
    private RuleManager ruleManager;

    // manager class for accumulator rules (XSLT 3.0 only)
    private IAccumulatorManager accumulatorManager = null;

    // namespace aliases. This information is needed at compile-time only
    private int numberOfAliases = 0;
    private List<ComponentDeclaration> namespaceAliasList = new ArrayList<ComponentDeclaration>(5);
    private HashMap<String, NamespaceBinding> namespaceAliasMap;
    private Set<String> aliasResultUriSet;
    //private short[] aliasSCodes;
    //private int[] aliasNCodes;

    // flag: true if there's an xsl:result-document that uses a dynamic format
    private boolean needsDynamicOutputProperties = false;

    // count of the maximum number of local variables in xsl:template match patterns
    private int largestPatternStackFrame = 0;

    // cache of stylesheet documents. Note that multiple imports of the same URI
    // lead to the stylesheet tree being reused
    private HashMap<DocumentURI, XSLModuleRoot> moduleCache = new HashMap<DocumentURI, XSLModuleRoot>(4);

    // definitions of strip/preserve space action
    private SpaceStrippingRule stripperRules;

    private CharacterMapIndex characterMapIndex = new CharacterMapIndex();

    private List<Action> fixupActions = new ArrayList<Action>();
    private List<Action> completionActions = new ArrayList<Action>();
    private boolean createsSecondaryResultDocuments = false;

    /**
     * Create a StylesheetPackage
     * @param sourceElement the xsl:package element at the root of the package manifest
     */

    public StylesheetPackage(XSLPackage sourceElement) {
        super(sourceElement, 0);
        keyManager = new KeyManager(sourceElement.getConfiguration());
        decimalFormatManager = new DecimalFormatManager();
        ruleManager = new RuleManager();
        try {
            setInputTypeAnnotations(sourceElement.getInputTypeAnnotationsAttribute());
        } catch (XPathException err) {
            // it will be reported some other time
        }
    }

    /**
     * Search the package for a component with a given name
     * @param name the symbolic name of the required component
     * @return the requested component if found, or null otherwise
     */

    public Component getComponent(SymbolicName name) {
        return componentIndex.get(name);
    }

    /**
     * Remove a global variable if it is found to be redundant, i.e. it is private to the package
     * and not referenced
     * @param decl the global variable to be removed
     */

    public void removeGlobalVariable(ComponentDeclaration decl) {
        StructuredQName name = decl.getSourceElement().getObjectName();
        SymbolicName sName = new SymbolicName(StandardNames.XSL_VARIABLE, name);
        if (globalVariableIndex.get(name) == decl) {
            componentIndex.remove(sName);
            globalVariableIndex.remove(name);
        }
    }

    /**
     * Get the outermost stylesheet module in a package
     * @return this module (this class represents both the package and its outermost module)
     */

    /*@NotNull*/
    public StylesheetPackage getPrincipalStylesheetModule() {
        return this;
    }

    /**
     * Get the key manager used to manage xsl:key declarations in this package
     * @return the key manager
     */

    public KeyManager getKeyManager() {
        return keyManager;
    }

    /**
     * Get the decimal format manager used to manage xsl:decimal-format declarations in this package
     * @return the decimal format manager
     */

    public DecimalFormatManager getDecimalFormatManager() {
        return decimalFormatManager;
    }

    /**
     * Get the rule manager used to manage modes declared explicitly or implicitly in this package
     * @return the rule manager
     */

    public RuleManager getRuleManager() {
        return ruleManager;
    }

    /**
     * Register a callback action to be performed during the fixup phase
     * @param action the callback action
     */

    public void addFixupAction(Action action) {
        fixupActions.add(action);
    }

    /**
     * Register a callback action to be performed during the completion phase
     * @param action the callback action
     */

    public void addCompletionAction(Action action) {
        completionActions.add(action);
    }

    /**
     * Create the function library containing stylesheet functions declared in this package
     *
     * @param compilation the controlling Compilation
     * @return the resulting function library
     */

    public FunctionLibraryList createFunctionLibrary(Compilation compilation) {
        Configuration config = getConfiguration();
        FunctionLibraryList stylesheetFunctions = new FunctionLibraryList();
        stylesheetFunctions.addFunctionLibrary(
                new StylesheetFunctionLibrary(this, true));
        stylesheetFunctions.addFunctionLibrary(
                new StylesheetFunctionLibrary(this, false));
        //getPreparedStylesheet().setStylesheetFunctionLibrary(stylesheetFunctions);
        stylesheetFunctionLibrary = stylesheetFunctions;

        FunctionLibraryList functionLibrary = new FunctionLibraryList();
        int functionSet = StandardFunction.CORE | StandardFunction.XSLT;
        if (DecimalValue.THREE.equals(getVersion())) {
            functionSet |= StandardFunction.XSLT30 | StandardFunction.XPATH30;
        }
        functionLibrary.addFunctionLibrary(
                SystemFunctionLibrary.getSystemFunctionLibrary(functionSet));
        functionLibrary.addFunctionLibrary(
                new StylesheetFunctionLibrary(this, true));
        functionLibrary.addFunctionLibrary(
                config.getVendorFunctionLibrary());
        functionLibrary.addFunctionLibrary(
                new ConstructorFunctionLibrary(config));
        CompilerInfo info = compilation.getCompilerInfo();
        if (info.getExtensionFunctionLibrary() != null) {
            functionLibrary.addFunctionLibrary(info.getExtensionFunctionLibrary());
        }
        queryFunctions = new XQueryFunctionLibrary(config);
        functionLibrary.addFunctionLibrary(queryFunctions);
        functionLibrary.addFunctionLibrary(config.getIntegratedFunctionLibrary());
        config.addExtensionBinders(functionLibrary);

        // temporary code to add functions from used packages, regardless of visibility
        for (StylesheetPackage pack : usedPackages) {
            FunctionLibraryList lib = pack.getStylesheetFunctionLibrary();
            functionLibrary.addFunctionLibrary(lib);
        }

        return this.functionLibrary = functionLibrary;
    }


    /**
     * Get the function library. Available only on the principal stylesheet module
     *
     * @return the function library
     */

    public FunctionLibrary getFunctionLibrary() {
        return functionLibrary;
    }

    public FunctionLibraryList getStylesheetFunctionLibrary() {
        return stylesheetFunctionLibrary;
    }

    /**
     * Get an index of character maps declared using xsl:character-map entries in this package
     * @return the character map index
     */

    public CharacterMapIndex getCharacterMapIndex() {
        return characterMapIndex;
    }


    /**
     * Declare an imported XQuery function
     *
     * @param function the imported function
     * @throws net.sf.saxon.trans.XPathException
     *          if an error occurs
     */

    public void declareXQueryFunction(XQueryFunction function) throws XPathException {
        queryFunctions.declareFunction(function);
    }

    /**
     * Add a module to the cache of stylesheet modules
     *
     * @param key    the key to be used (based on the absolute URI)
     * @param module the stylesheet document tree corresponding to this absolute URI
     */

    public void putStylesheetDocument(DocumentURI key, XSLStylesheet module) {
        moduleCache.put(key, module);
    }

    /**
     * Get a module from the cache of stylesheet modules
     *
     * @param key the key to be used (based on the absolute URI)
     * @return the stylesheet document tree corresponding to this absolute URI
     */

    public XSLModuleRoot getStylesheetDocument(DocumentURI key) {
        XSLModuleRoot sheet = moduleCache.get(key);
        if (sheet != null) {
            XPathException warning = new XPathException(
                    "Stylesheet module " + key + " is included or imported more than once. " +
                            "This is permitted, but may lead to errors or unexpected behavior");
            sheet.issueWarning(warning);
        }
        return sheet;
    }

    /**
     * Preprocess does all the processing possible before the source document is available.
     * It is done once per stylesheet, so the stylesheet can be reused for multiple source
     * documents. The method is called only on the XSLStylesheet element representing the
     * principal stylesheet module
     *
     * @throws net.sf.saxon.trans.XPathException
     *          if errors are found in the stylesheet
     */

    public void preprocess() throws XPathException {


        net.sf.saxon.trans.Timer timer;
        if (Compilation.TIMING) {
            timer = new net.sf.saxon.trans.Timer();
        }

        // process any xsl:use-package, xsl:include and xsl:import elements

        spliceUsePackages((XSLPackage) getRootElement(), getRootElement().getCompilation());

        if (Compilation.TIMING) {
            timer.report("spliceIncludes");
        }

        // import schema documents

        importSchemata();

        if (Compilation.TIMING) {
            timer.report("importSchemata");
        }

        // build indexes for selected top-level elements

        buildIndexes();

        if (Compilation.TIMING) {
            timer.report("buildIndexes");
        }

        // check for use of schema-aware constructs

        checkForSchemaAwareness();

        if (Compilation.TIMING) {
            timer.report("checkForSchemaAwareness");
        }

        // process the attributes of every node in the tree

        processAllAttributes();

        if (Compilation.TIMING) {
            timer.report("processAllAttributes");
        }
        // collect any namespace aliases

        collectNamespaceAliases();

        if (Compilation.TIMING) {
            timer.report("collectNamespaceAliases");
        }

        // fix up references from XPath expressions to variables and functions, for static typing

        for (ComponentDeclaration decl : topLevel) {
            StyleElement inst = decl.getSourceElement();
            if (!inst.isActionCompleted(StyleElement.ACTION_FIXUP)) {
                inst.setActionCompleted(StyleElement.ACTION_FIXUP);
//                if (inst instanceof XSLVariableDeclaration) {
//                    System.err.println("Fixup global variable " + ((XSLVariableDeclaration)inst).getVariableQName());
//                }
                inst.fixupReferences();
            }
        }

        if (Compilation.TIMING) {
            timer.report("fixupReferences");
        }
        // Validate the whole package (i.e. with included and imported stylesheet modules)

        XSLPackage top = (XSLPackage)getStylesheetElement();
        //setInputTypeAnnotations(top.getInputTypeAnnotationsAttribute());
        ComponentDeclaration decl = new ComponentDeclaration(this, top);
        if (!top.isActionCompleted(StyleElement.ACTION_VALIDATE)) {
            top.setActionCompleted(StyleElement.ACTION_VALIDATE);
            //top.validate(decl);
            getRootElement().validate(decl);
            top.validate(null);
            for (ComponentDeclaration d : topLevel) {
                d.getSourceElement().validateSubtree(d, false);
            }
        }

        if (Compilation.TIMING) {
            timer.report("validate");
            timer.reportCumulative("total preprocess");
        }

        // Index the character maps

        for (ComponentDeclaration d : topLevel) {
            StyleElement inst = d.getSourceElement();
            if (inst instanceof XSLCharacterMap) {
                XSLCharacterMap xcm = (XSLCharacterMap) inst;
                IntHashMap<String> map = new IntHashMap<String>();
                xcm.assemble(map);
                characterMapIndex.putCharacterMap(xcm.getCharacterMapName(), new CharacterMap(map));
            }
        }

    }

    /**
     * Incorporate declarations from used packages
     * @param xslpackage the used package
     * @param compilation this compilation
     * @throws XPathException if any error is detected in the used package
     */

    protected void spliceUsePackages(XSLPackage xslpackage, Compilation compilation) throws XPathException {
        CompilerInfo info = compilation.getCompilerInfo();

        if (info.isVersionWarning() &&
                xslpackage.getEffectiveVersion().compareTo(compilation.getStyleNodeFactory(true).getXsltProcessorVersion()) != 0) {
            XPathException w = new XPathException(
                    "Running an XSLT " + xslpackage.getEffectiveVersion() + " stylesheet with an XSLT " +
                            xslpackage.getCompilation().getStyleNodeFactory(true).getXsltProcessorVersion() + " processor");
            w.setLocator(xslpackage);
            compilation.reportWarning(w);
        }

        // Preprocess the stylesheet, performing validation and preparing template definitions
        spliceIncludes();
    }

    /**
     * Process import-schema declarations
     *
     * @throws net.sf.saxon.trans.XPathException
     *          if errors are detected
     */

    protected void importSchemata() throws XPathException {
        // Outside Saxon-EE, xsl:import-schemas are an error
        for (int i = topLevel.size() - 1; i >= 0; i--) {
            ComponentDeclaration decl = topLevel.get(i);
            if (decl.getSourceElement() instanceof XSLImportSchema) {
                XPathException xe = new XPathException("xsl:import-schema requires Saxon-EE");
                xe.setErrorCode("XTSE1650");
                xe.setLocator(decl.getSourceElement());
                throw xe;
            }
        }

    }


    /**
     * Build indexes for selected top-level declarations
     *
     * @throws net.sf.saxon.trans.XPathException
     *          if errors are detected
     */

    private void buildIndexes() throws XPathException {
        // Scan the declarations in reverse order, that is, highest precedence first
        for (int i = topLevel.size() - 1; i >= 0; i--) {
            ComponentDeclaration decl = topLevel.get(i);
            decl.getSourceElement().index(decl, this);
        }
//        // Now seal all the schemas that have been imported to guarantee consistency with instance documents
//        Configuration config = getConfiguration();
//        for (String ns : schemaIndex) {
//            config.sealNamespace(ns);
//        }
    }

    /**
     * Process the attributes of every node in the stylesheet
     *
     * @throws net.sf.saxon.trans.XPathException
     *          if static errors are found in the stylesheet
     */

    public void processAllAttributes() throws XPathException {
        getRootElement().processDefaultCollationAttribute();
        getRootElement().prepareAttributes();
        for (XSLModuleRoot xss : moduleCache.values()) {
            xss.prepareAttributes();
        }
        for (ComponentDeclaration decl : topLevel) {
            StyleElement inst = decl.getSourceElement();
            if (!inst.isActionCompleted(StyleElement.ACTION_PROCESS_ATTRIBUTES)) {
                inst.setActionCompleted(StyleElement.ACTION_PROCESS_ATTRIBUTES);
                try {
                    inst.processAllAttributes();
                } catch (XPathException err) {
                    decl.getSourceElement().compileError(err);
                }
            }
        }
    }

    /**
     * Add a stylesheet function to the index
     *
     * @param decl The declaration wrapping an XSLFunction object
     * @throws XPathException if errors are found
     */
    protected void indexFunction(ComponentDeclaration decl) throws XPathException {
        XSLFunction sourceFunction = (XSLFunction) decl.getSourceElement();
        UserFunction compiledFunction = sourceFunction.getCompiledFunction();
        Component declaringComponent = compiledFunction.getDeclaringComponent();
        if (declaringComponent == null) {
            compiledFunction.makeDeclaringComponent(sourceFunction.getVisibility(), this);
            declaringComponent = compiledFunction.getDeclaringComponent();
        }
        SymbolicName sName = sourceFunction.getSymbolicName();
        //StructuredQName qName = template.getTemplateName();
        if (sName != null) {
            // see if there is already a named function with this precedence
            Component other = componentIndex.get(sName);
            if (other == null) {
                // this is the first
                componentIndex.put(sName, declaringComponent);
                functionIndex.put(sName, decl);
            } else {
                if (other.getDeclaringPackage() == this) {
                    // check the precedences
                    int thisPrecedence = decl.getPrecedence();
                    ComponentDeclaration otherFunction = functionIndex.get(sName);
                    int otherPrecedence = otherFunction.getPrecedence();
                    if (thisPrecedence == otherPrecedence) {
                        sourceFunction.compileError("Duplicate named function (see line " +
                                otherFunction.getSourceElement().getLineNumber() + " of " + otherFunction.getSourceElement().getSystemId() + ')', "XTSE0770");
                    } else if (thisPrecedence < otherPrecedence) {
                        //template.setRedundantNamedTemplate();
                    } else {
                        // can't happen, but we'll play safe
                        //other.setRedundantNamedTemplate();
                        componentIndex.put(sName, declaringComponent);
                        functionIndex.put(sName, decl);
                    }
                } else {
                    // the new one wins
                    componentIndex.put(sName, declaringComponent);
                    functionIndex.put(sName, decl);
                }
            }
        }

    }

    private ComponentDeclaration getFunctionDeclaration(SymbolicName name) {
        return functionIndex.get(name);
    }

    /**
     * Get the function with a given name and arity
     *
     * @param name the symbolic name of the function (QName plus arity)
     * @return the requested function, or null if none can be found
     */

    protected UserFunction getFunction(SymbolicName name) {
        if (name.getArity() == -1) {
            // supports the single-argument function-available() function
            int maximumArity = 20;
            for (int a = 0; a < maximumArity; a++) {
                SymbolicName sn = new SymbolicName(StandardNames.XSL_FUNCTION, name.getComponentName(), a);
                ComponentDeclaration decl = getFunctionDeclaration(sn);
                if (decl != null) {
                    return ((XSLFunction) decl.getSourceElement()).getCompiledFunction();
                }
            }
            return null;
        } else {
            Component component = componentIndex.get(name);
            return component == null ? null : (UserFunction) component.getProcedure();
        }
    }

//    protected void putFunction(Declaration decl) {
//        XSLFunction function = (XSLFunction) decl.getSourceElement();
//        if (function.getNumberOfArguments() > maximumArity) {
//            maximumArity = function.getNumberOfArguments();
//        }
//        functionIndex.put(function.getSymbolicName(), decl);
//    }


    /**
     * Index a global xsl:variable or xsl:param element
     *
     * @param decl The Declaration referencing the XSLVariable or XSLParam element
     * @throws XPathException if an error occurs
     */

    protected void indexVariableDeclaration(ComponentDeclaration decl) throws XPathException {
        XSLGlobalVariable var = (XSLGlobalVariable) decl.getSourceElement();
        StructuredQName qName = var.getSourceBinding().getVariableQName();
        if (qName != null) {
            // see if there is already a global variable with this precedence
            ComponentDeclaration other = globalVariableIndex.get(qName);
            if (other == null) {
                // this is the first
                globalVariableIndex.put(qName, decl);
                componentIndex.put(new SymbolicName(StandardNames.XSL_VARIABLE, qName),
                        var.getCompiledProcedure().getDeclaringComponent());
            } else {
                // check the precedences
                int thisPrecedence = decl.getPrecedence();
                int otherPrecedence = other.getPrecedence();
                if (thisPrecedence == otherPrecedence) {
                    StyleElement v2 = other.getSourceElement();
                    if (v2 == var) {
                        var.compileError(
                                "Global variable " + qName.getDisplayName() + " is declared more than once " +
                                        "(caused by including the containing module more than once)",
                                "XTSE0630");
                    } else {
                        var.compileError("Duplicate global variable declaration (see line " +
                                v2.getLineNumber() + " of " + v2.getSystemId() + ')', "XTSE0630");
                    }
                } else if (thisPrecedence < otherPrecedence && var != other.getSourceElement()) {
                    var.setRedundant(true);
                } else if (var != other.getSourceElement()) {
                    ((XSLGlobalVariable) other.getSourceElement()).setRedundant(true);
                    globalVariableIndex.put(qName, decl);
                    componentIndex.put(new SymbolicName(StandardNames.XSL_VARIABLE, qName),
                        var.getCompiledProcedure().getDeclaringComponent());
                }
            }
        }
    }

    /**
     * Get the global variable or parameter with a given name (taking
     * precedence rules into account). This will only return global variables
     * declared in the same package where they are used.
     *
     * @param qName name of the global variable or parameter
     * @return the variable declaration, or null if it does not exist
     */

    public SourceBinding getGlobalVariableBinding(StructuredQName qName) {
        ComponentDeclaration decl = globalVariableIndex.get(qName);
        return decl == null ? null : ((XSLGlobalVariable) decl.getSourceElement()).getSourceBinding();
    }

    /**
     * Allocate a unique number to a local parameter name. This should only be called on the principal
     * stylesheet module.
     *
     * @param qName the local parameter name
     * @return an integer that uniquely identifies this parameter name within the stylesheet
     */

    public int allocateUniqueParameterNumber(StructuredQName qName) {
        HashMap<StructuredQName, Integer> params = localParameterNumbers;
        if (params == null) {
            localParameterNumbers = new HashMap<StructuredQName, Integer>(50);
            params = localParameterNumbers;
        }
        Integer x = params.get(qName);
        if (x == null) {
            x = params.size();
            params.put(qName, x);
        }
        return x;
    }

    /**
     * Add a named template to the index
     *
     * @param decl the declaration of the Template object
     * @throws XPathException if an error occurs
     */
    protected void indexNamedTemplate(ComponentDeclaration decl) throws XPathException {
        XSLTemplate sourceTemplate = (XSLTemplate) decl.getSourceElement();
        Template compiledTemplate = sourceTemplate.getCompiledTemplate();
        Component declaringComponent = compiledTemplate.getDeclaringComponent();
        if (declaringComponent == null) {
            compiledTemplate.makeDeclaringComponent(sourceTemplate.getVisibility(), this);
            declaringComponent = compiledTemplate.getDeclaringComponent();
        }
        SymbolicName sName = sourceTemplate.getSymbolicName();
        //StructuredQName qName = template.getTemplateName();
        if (sName != null) {
            // see if there is already a named template with this precedence
            Component other = componentIndex.get(sName);
            if (other == null) {
                // this is the first
                componentIndex.put(sName, declaringComponent);
                templateIndex.put(sName.getComponentName(), decl);
            } else {
                if (other.getDeclaringPackage() == this) {
                    // check the precedences
                    int thisPrecedence = decl.getPrecedence();
                    ComponentDeclaration otherTemplate = templateIndex.get(sName.getComponentName());
                    int otherPrecedence = otherTemplate.getPrecedence();
                    if (thisPrecedence == otherPrecedence) {
                        sourceTemplate.compileError("Duplicate named template (see line " +
                                otherTemplate.getSourceElement().getLineNumber() + " of " + otherTemplate.getSourceElement().getSystemId() + ')', "XTSE0660");
                    } else if (thisPrecedence < otherPrecedence) {
                        //template.setRedundantNamedTemplate();
                    } else {
                        // can't happen, but we'll play safe
                        //other.setRedundantNamedTemplate();
                        componentIndex.put(sName, declaringComponent);
                        templateIndex.put(sName.getComponentName(), decl);
                    }
                } else {
                    // the new one wins
                    componentIndex.put(sName, declaringComponent);
                    templateIndex.put(sName.getComponentName(), decl);
                }
            }
        }
    }

    /**
     * Get the named template with a given name
     *
     * @param name the name of the required template
     * @return the template with the given name, if there is one, or null otherwise. If there
     *         are several templates with the same name, the one with highest import precedence
     *         is returned. Note that this template may subsequently be overridden in another package,
     *         but only by another template with a compatible signature.
     */

    public Template getNamedTemplate(StructuredQName name) {
        Component component = componentIndex.get(new SymbolicName(StandardNames.XSL_TEMPLATE, name));
        return component == null ? null : (Template) component.getProcedure();
    }

    /**
     * Add an attribute set to the index
     *
     * @param decl the declaration of the attribute set object
     * @throws XPathException if an error occurs
     */
    protected void indexAttributeSet(ComponentDeclaration decl) throws XPathException {
        XSLAttributeSet sourceAttributeSet = (XSLAttributeSet) decl.getSourceElement();
        AttributeSet compiledAttributeSet = sourceAttributeSet.getCompiledProcedure();
        Component declaringComponent = compiledAttributeSet.getDeclaringComponent();
        if (declaringComponent == null) {
            compiledAttributeSet.makeDeclaringComponent(sourceAttributeSet.getVisibility(), this);
            declaringComponent = compiledAttributeSet.getDeclaringComponent();
        }
        SymbolicName sName = sourceAttributeSet.getSymbolicName();
        //StructuredQName qName = template.getTemplateName();
        if (sName != null) {
            // see if there is already an attribute set with this precedence
            Component other = componentIndex.get(sName);
            if (other == null) {
                // this is the first
                componentIndex.put(sName, declaringComponent);
                //templateIndex.put(sName.getComponentName(), decl);
            } else {
                if (other.getDeclaringPackage() == this) {
                    // check the precedences
                    int thisPrecedence = decl.getPrecedence();

                    // TODO: reinstate below code

                    //Declaration otherTemplate = componentIndex.get(sName.getComponentName());
                    //int otherPrecedence = otherTemplate.getPrecedence();
//                    if (thisPrecedence == otherPrecedence) {
//                        sourceAttributeSet.compileError("Duplicate named attribute set (see line " +
//                                otherTemplate.getSourceElement().getLineNumber() + " of " + otherTemplate.getSourceElement().getSystemId() + ')', "XTSE0660");
//                    } else if (thisPrecedence < otherPrecedence) {
//                        //template.setRedundantNamedTemplate();
//                    } else {
//                        // can't happen, but we'll play safe
//                        //other.setRedundantNamedTemplate();
//                        componentIndex.put(sName, declaringComponent);
//                        //templateIndex.put(sName.getComponentName(), decl);
//                    }

                    // TODO: check consistency of "streamable" declaration
                } else {
                    // the new one wins
                    componentIndex.put(sName, declaringComponent);
                    //templateIndex.put(sName.getComponentName(), decl);
                }
            }
        }
    }



    /**
     * Check for schema-awareness.
     * Typed input nodes are recognized if and only if the stylesheet contains an import-schema declaration.
     */

    private void checkForSchemaAwareness() {
        Compilation compilation = getRootElement().getCompilation();
        if (!compilation.isSchemaAware() &&
                getConfiguration().isLicensedFeature(Configuration.LicenseFeature.ENTERPRISE_XSLT)) {
            for (ComponentDeclaration decl : topLevel) {
                StyleElement node = decl.getSourceElement();
                if (node instanceof XSLImportSchema) {
                    compilation.setSchemaAware(true);
                    return;
                }
            }
        }
    }

    /**
     * Get the class that manages accumulator functions
     *
     * @return the class that manages accumulators. Always null in Saxon-HE
     */

    public IAccumulatorManager getAccumulatorManager() {
        return accumulatorManager;
    }

    /**
     * Set the class that manages accumulator functions
     *
     * @param accumulatorManager the manager of accumulator functions
     */

    public void setAccumulatorManager(IAccumulatorManager accumulatorManager) {
        this.accumulatorManager = accumulatorManager;
    }


    protected void addNamespaceAlias(ComponentDeclaration node) {
        namespaceAliasList.add(node);
        numberOfAliases++;
    }

    /**
     * Get the declared namespace alias for a given namespace URI code if there is one.
     * If there is more than one, we get the last.
     *
     * @param uri The uri used in the stylesheet.
     * @return The namespace binding to be used (prefix and uri): return null
     *         if no alias is defined
     */

    protected NamespaceBinding getNamespaceAlias(String uri) {
        return namespaceAliasMap.get(uri);
    }

    /**
     * Determine if a namespace is included in the result-prefix of a namespace-alias
     *
     * @param uri the namespace URI
     * @return true if an xsl:namespace-alias has been defined for this namespace URI
     */

    protected boolean isAliasResultNamespace(String uri) {
        return aliasResultUriSet.contains(uri);
    }

    /**
     * Collect any namespace aliases
     *
     * @throws net.sf.saxon.trans.XPathException
     *          if an error occurs
     */

    private void collectNamespaceAliases() throws XPathException {
        namespaceAliasMap = new HashMap<String, NamespaceBinding>(numberOfAliases);
        aliasResultUriSet = new HashSet<String>(numberOfAliases);
        HashSet<String> aliasesAtThisPrecedence = new HashSet<String>();
        int currentPrecedence = -1;
        // Note that we are processing the list in reverse stylesheet order,
        // that is, highest precedence first.
        for (int i = 0; i < numberOfAliases; i++) {
            ComponentDeclaration decl = namespaceAliasList.get(i);
            XSLNamespaceAlias xna = (XSLNamespaceAlias) decl.getSourceElement();
            String scode = xna.getStylesheetURI();
            NamespaceBinding resultBinding = xna.getResultNamespaceBinding();
            int prec = decl.getPrecedence();

            // check that there isn't a conflict with another xsl:namespace-alias
            // at the same precedence

            if (currentPrecedence != prec) {
                currentPrecedence = prec;
                aliasesAtThisPrecedence.clear();
                //precedenceBoundary = i;
            }
            if (aliasesAtThisPrecedence.contains(scode)) {
                if (!namespaceAliasMap.get(scode).getURI().equals(resultBinding.getURI())) {
                    xna.compileError("More than one alias is defined for the same namespace", "XTSE0810");
                }
            }
            if (namespaceAliasMap.get(scode) == null) {
                namespaceAliasMap.put(scode, resultBinding);
                aliasResultUriSet.add(resultBinding.getURI());
            }
            aliasesAtThisPrecedence.add(scode);
        }
        namespaceAliasList = null;  // throw it in the garbage
    }

    protected boolean hasNamespaceAliases() {
        return numberOfAliases > 0;
    }

    /**
     * Create an output properties object representing the xsl:output elements in the stylesheet.
     *
     * @param formatQName The name of the output format required. If set to null, gathers
     *                    information for the unnamed output format
     * @return the Properties object containing the details of the specified output format
     * @throws XPathException if a named output format does not exist in
     *                        the stylesheet
     */

    public Properties gatherOutputProperties(/*@Nullable*/ StructuredQName formatQName) throws XPathException {
        boolean found = formatQName == null;
        Configuration config = getConfiguration();
        Properties details = new Properties(config.getDefaultSerializationProperties());
        HashMap<String, Integer> precedences = new HashMap<String, Integer>(10);
        for (int i = topLevel.size() - 1; i >= 0; i--) {
            ComponentDeclaration decl = topLevel.get(i);
            if (decl.getSourceElement() instanceof XSLOutput) {
                XSLOutput xo = (XSLOutput) decl.getSourceElement();
                if (formatQName == null
                        ? xo.getFormatQName() == null
                        : formatQName.equals(xo.getFormatQName())) {
                    found = true;
                    xo.gatherOutputProperties(details, precedences, decl.getPrecedence());
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
     * Compile the stylesheet package
     *
     * @param compilation the compilation episode
     * @throws XPathException if compilation fails for any reason
     */

    protected void compile(Compilation compilation) throws XPathException {

        try {

            net.sf.saxon.trans.Timer timer;
            if (Compilation.TIMING) {
                timer = new net.sf.saxon.trans.Timer();
            }

            //PreparedStylesheet pss = getPreparedStylesheet();
            Configuration config = getConfiguration();

            // If any XQuery functions were imported, fix up all function calls
            // registered against these functions.
            try {
                Iterator qf = queryFunctions.getFunctionDefinitions();
                while (qf.hasNext()) {
                    XQueryFunction f = (XQueryFunction) qf.next();
                    f.fixupReferences();
                }
            } catch (XPathException e) {
                getRootElement().compileError(e);
            }

            if (Compilation.TIMING) {
                timer.report("fixup Query functions");
            }

            // Register template rules with the rule manager

            for (ComponentDeclaration decl : topLevel) {
                StyleElement snode = decl.getSourceElement();
                if (snode instanceof XSLTemplate) {
                    ((XSLTemplate) snode).register(decl);
                }
            }

            if (Compilation.TIMING) {
                timer.report("register templates");
            }


            // Call compile method for each top-level object in the stylesheet
            // Note, some declarations (templates) need to be compiled repeatedly if the module
            // is imported repeatedly; others (variables, functions) do not

            for (ComponentDeclaration decl : topLevel) {
                StyleElement snode = decl.getSourceElement();
                if (!snode.isActionCompleted(StyleElement.ACTION_COMPILE)) {
                    snode.setActionCompleted(StyleElement.ACTION_COMPILE);
                    snode.compileDeclaration(compilation, decl);
                }
            }

            if (Compilation.TIMING) {
                timer.report("compile top-level objects");
            }

            // Call type-check method for each user-defined function in the stylesheet. This is no longer
            // done during the optimize step, to avoid functions being inlined before they are type-checked.

            for (ComponentDeclaration decl : functionIndex.values()) {
                StyleElement node = decl.getSourceElement();
                if (!node.isActionCompleted(StyleElement.ACTION_TYPECHECK)) {
                    node.setActionCompleted(StyleElement.ACTION_TYPECHECK);
                    ((XSLFunction) node).typeCheckBody();
                }
            }

            if (Compilation.TIMING) {
                timer.report("typeCheck functions");
            }

            if (compilation.getErrorCount() > 0) {
                // not much point carrying on
                return;
            }

            optimizeTopLevel();


            // optimize functions that aren't overridden

            for (ComponentDeclaration decl : functionIndex.values()) {
                StyleElement node = decl.getSourceElement();
                if (!node.isActionCompleted(StyleElement.ACTION_OPTIMIZE)) {
                    node.setActionCompleted(StyleElement.ACTION_OPTIMIZE);
                    ((StylesheetComponent) node).optimize(decl);
                }
            }

            if (Compilation.TIMING) {
                timer.report("optimize");
            }

            if (config.isTiming() && config.isGenerateByteCode(Configuration.XSLT)) {
                config.getStandardErrorOutput().println("Generating byte code...");
            }

            if (Compilation.TIMING) {
                timer.report("miscellanea");
            }

            // Check consistency of decimal formats

            getDecimalFormatManager().checkConsistency();


            if (Compilation.TIMING) {
                timer.report("build template rule tables");
            }

            // Build a run-time function library. This supports the use of function-available()
            // with a dynamic argument, and extensions such as saxon:evaluate(). The run-time
            // function library differs from the compile-time function library in that both
            // the StylesheetFunctionLibrary's on the library list are replaced by equivalent
            // ExecutableFunctionLibrary's. This is to prevent the retaining of run-time links
            // to the stylesheet document tree.

            overriding = new ExecutableFunctionLibrary(config);
            underriding = new ExecutableFunctionLibrary(config);

            for (ComponentDeclaration decl : topLevel) {
                if (decl.getSourceElement() instanceof XSLFunction) {
                    XSLFunction func = (XSLFunction) decl.getSourceElement();
                    if (func.isOverrideExtensionFunction()) {
                        overriding.addFunction(func.getCompiledFunction());
                    } else {
                        underriding.addFunction(func.getCompiledFunction());
                    }
                }
            }

            if (Compilation.TIMING) {
                timer.report("build runtime function tables");
                timer.reportCumulative("total compile phase");
            }

            // Allocate binding slots to component reference expressions such as xsl:call-template

            for (ComponentDeclaration decl : topLevel) {
                StyleElement inst = decl.getSourceElement();
                if (inst instanceof StylesheetComponent) {
                    ComponentBody proc = ((StylesheetComponent) inst).getCompiledProcedure();
                    if (proc != null) {
                        proc.allocateAllBindingSlots(this);
                    }
                }
            }

            // Allocate binding slots to component reference expressions in system-allocated global variables

            for (Component decl : componentIndex.values()) {
                ComponentBody proc = decl.getProcedure();
                if (proc != null && proc instanceof GlobalVariable &&
                        ((GlobalVariable)proc).getVariableQName().hasURI(NamespaceConstant.SAXON_GENERATED_GLOBAL)) {
                    proc.allocateAllBindingSlots(this);
                }
            }

            // Allocate binding slots in optimizer-generated key definitions

            KeyManager keyMan = getKeyManager();
            for (KeyDefinitionSet keySet : keyMan.getAllKeyDefinitionSets()) {
                if (keySet.getKeyName().hasURI(NamespaceConstant.SAXON)) {
                    for (KeyDefinition keyDef : keySet.getKeyDefinitions()) {
                        keyDef.makeDeclaringComponent(Visibility.PRIVATE, this);
                        keyDef.allocateAllBindingSlots(this);
                    }
                }
            }

            // Allocate binding slots in accumulators

            IAccumulatorManager accMan = getAccumulatorManager();
            if (accMan != null) {
                for (ComponentBody acc : accMan.getAllAccumulators()) {
                    acc.allocateAllBindingSlots(this);
                }
            }

            // Generate byte code where appropriate

            Optimizer opt = config.obtainOptimizer();
            for (ComponentDeclaration decl : topLevel) {
                StyleElement inst = decl.getSourceElement();
                if (inst instanceof StylesheetComponent) {
                    ((StylesheetComponent)inst).generateByteCode(opt);
                }
            }

        } catch (RuntimeException err) {
            // if syntax errors were reported earlier, then exceptions may occur during this phase
            // due to inconsistency of data structures. We can ignore these exceptions as they
            // will go away when the user corrects the stylesheet
            if (compilation.getErrorCount() == 0) {
                // rethrow the exception
                throw err;
            }
        }
    }

    public void optimizeTopLevel() throws XPathException {
        // Call optimize method for each top-level object in the stylesheet
        // But for functions, do it only for those of highest precedence.

        for (ComponentDeclaration decl : topLevel) {
            StyleElement node = decl.getSourceElement();
            if (node instanceof StylesheetComponent && !(node instanceof XSLFunction) &&
                    !node.isActionCompleted(StyleElement.ACTION_OPTIMIZE)) {
                node.setActionCompleted(StyleElement.ACTION_OPTIMIZE);
                ((StylesheetComponent) node).optimize(decl);
            }
            if (node instanceof XSLTemplate) {
                ((XSLTemplate)node).allocatePatternSlotNumbers();
            }
        }
    }

    /**
     * Copy information from this package to the PreparedStylesheet.
     *
     * @param pss the PreparedStylesheet to be updated
     * @throws XPathException if the PreparedStylesheet cannot be updated
     */

    public void updatePreparedStylesheet(PreparedStylesheet pss) throws XPathException {


        // TODO: this is scaffolding; it doesn't take account of the fact that we need to merge
        // information from multiple packages. But it helps to isolate the dependencies.

        pss.setStylesheetFunctionLibrary(stylesheetFunctionLibrary);

        FunctionLibraryList libraryList = new FunctionLibraryList();
        for (FunctionLibrary lib : functionLibrary.getLibraryList()) {
            if (lib instanceof StylesheetFunctionLibrary) {
                if (((StylesheetFunctionLibrary) lib).isOverrideExtensionFunction()) {
                    libraryList.addFunctionLibrary(overriding);
                    pss.getStylesheetFunctions().addFunctionLibrary(overriding);
                } else {
                    libraryList.addFunctionLibrary(underriding);
                    pss.getStylesheetFunctions().addFunctionLibrary(underriding);
                }
            } else {
                libraryList.addFunctionLibrary(lib);
            }
        }
        pss.setFunctionLibrary(libraryList);

        pss.setKeyManager(keyManager);

        pss.setAccumulatorManager(accumulatorManager);

        pss.setStripsWhitespace(stripsWhitespace());

        pss.setPatternSlotSpace(largestPatternStackFrame);
        pss.setStripsInputTypeAnnotations(getInputTypeAnnotations() == XSLStylesheet.ANNOTATION_STRIP);

        pss.setStripperRules(stripperRules);

        pss.setCreatesSecondaryResult(createsSecondaryResultDocuments);

        // Build the index of named character maps

        for (ComponentDeclaration decl : topLevel) {
            if (decl.getSourceElement() instanceof XSLCharacterMap) {
                XSLCharacterMap t = (XSLCharacterMap) decl.getSourceElement();
                if (!t.isRedundant()) {
                    StructuredQName qn = t.getCharacterMapName();
                    IntHashMap<String> charMap = new IntHashMap<String>();
                    t.assemble(charMap);
                    CharacterMap map = new CharacterMap(charMap);
                    if (pss.getCharacterMapIndex() == null) {
                        pss.setCharacterMapIndex(new CharacterMapIndex());
                    }
                    pss.getCharacterMapIndex().putCharacterMap(qn, map);
                }
            }
        }

        // Finish off the lists of template rules


        ruleManager.computeRankings();
        ruleManager.invertStreamableTemplates(getConfiguration().obtainOptimizer());
        pss.setRuleManager(ruleManager);

        Properties props = gatherOutputProperties(null);
        props.setProperty(SaxonOutputKeys.STYLESHEET_VERSION, getVersion().getStringValue());
        pss.setDefaultOutputProperties(props);

        // Handle named output formats for use at run-time

        HashSet<StructuredQName> outputNames = new HashSet<StructuredQName>(5);
        for (ComponentDeclaration decl : topLevel) {
            if (decl.getSourceElement() instanceof XSLOutput) {
                XSLOutput out = (XSLOutput) decl.getSourceElement();
                StructuredQName qName = out.getFormatQName();
                if (qName != null) {
                    outputNames.add(qName);
                }
            }
        }
        if (outputNames.isEmpty()) {
            if (needsDynamicOutputProperties) {
                throw new XPathException(
                        "The stylesheet contains xsl:result-document instructions that calculate the output " +
                                "format name at run-time, but there are no named xsl:output declarations", "XTDE1460");
            }
        } else {
            for (StructuredQName qName : outputNames) {
                Properties oprops = gatherOutputProperties(qName);
                if (needsDynamicOutputProperties) {
                    pss.setOutputProperties(qName, oprops);
                }
            }
        }

        // Add named templates to the prepared stylesheet

        for (Map.Entry<StructuredQName, ComponentDeclaration> entry : templateIndex.entrySet()) {
            pss.putNamedTemplate(entry.getKey(), ((XSLTemplate) entry.getValue().getSourceElement()).getCompiledTemplate());
        }

        // Add named functions to the prepared stylesheet

        pss.setComponentIndex(componentIndex);

        for (Component comp : componentIndex.values()) {
            if (comp.getProcedure() instanceof GlobalVariable) {
                GlobalVariable gv = (GlobalVariable)comp.getProcedure();
                pss.registerGlobalVariable(gv);
                if (!gv.isUnused()) {
                    int slot = pss.getGlobalVariableMap().allocateSlotNumber(gv.getVariableQName());
                    gv.setBinderySlotNumber(slot);
                }
            }
        }

        for (Component comp : hiddenComponents) {
            if (comp.getProcedure() instanceof GlobalVariable) {
                GlobalVariable gv = (GlobalVariable)comp.getProcedure();
                pss.registerGlobalVariable(gv);
                if (!gv.isUnused()) {
                    int slot = pss.getGlobalVariableMap().allocateSlotNumber(gv.getVariableQName());
                    gv.setBinderySlotNumber(slot);
                }
            }
        }

        for (StylesheetPackage pack : usedPackages) {

        }
        // Remove references to source tree, so it can be garbage-collected
        topLevel = null;
        templateIndex = null;
        completionActions = null;

    }

    /**
     * Get an imported schema with a given namespace
     *
     * @param targetNamespace The target namespace of the required schema.
     *                        Supply an empty string for the default namespace
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
     * Get the list of attribute-set declarations associated with a given QName.
     * This is used for xsl:element, xsl:copy, xsl:attribute-set, and on literal
     * result elements
     *
     * @param name the name of the required attribute set
     * @param list a list to hold the list of XSLAttributeSet elements in the stylesheet tree.
     * @return true if any declarations were found and added to the list; false if none were found
     * @throws net.sf.saxon.trans.XPathException
     *          if an error occurs
     */

    protected boolean getAttributeSets(StructuredQName name, List<ComponentDeclaration> list)
            throws XPathException {

        boolean found = false;

        // search for the named attribute set, using all of them if there are several with the
        // same name

        for (ComponentDeclaration decl : topLevel) {
            if (decl.getSourceElement() instanceof XSLAttributeSet) {
                XSLAttributeSet t = (XSLAttributeSet) decl.getSourceElement();
                if (t.getAttributeSetName().equals(name)) {
                    t.incrementReferenceCount();
                    list.add(decl);
                    found = true;
                }
            }
        }
        return found;
    }

    /**
     * Determine whether this stylesheet does any whitespace stripping
     *
     * @return true if this stylesheet strips whitespace from source documents
     */

    public boolean stripsWhitespace() {
        for (ComponentDeclaration aTopLevel : topLevel) {
            NodeInfo s = aTopLevel.getSourceElement();
            if (s.getFingerprint() == StandardNames.XSL_STRIP_SPACE) {
                return true;
            }
        }
        return false;
    }

    /**
     * Set the rules determining which nodes are to be stripped from the tree
     *
     * @param rules a Mode object containing the whitespace stripping rules. A Mode
     *              is generally a collection of template rules, but it is reused here to represent
     *              a collection of stripping rules.
     */

    public void setStripperRules(SpaceStrippingRule rules) {
        stripperRules = rules;
    }

    /**
     * Get the rules determining which nodes are to be stripped from the tree
     *
     * @return a SpaceStrippingRule object containing the whitespace stripping rules.
     */

    /*@NotNull*/
    public SpaceStrippingRule getStripperRules() {
        return stripperRules == null ? NoElementsSpaceStrippingRule.getInstance() : stripperRules;
    }

    /**
     * Say that this stylesheet needs dynamic output properties
     *
     * @param b true if this stylesheet needs dynamic output properties
     */

    public void setNeedsDynamicOutputProperties(boolean b) {
        needsDynamicOutputProperties = b;
    }

    /**
     * Get a character map, identified by the fingerprint of its name.
     * Search backwards through the stylesheet.
     *
     * @param name The character map name being sought
     * @return the identified character map, or null if not found
     */

    public ComponentDeclaration getCharacterMap(StructuredQName name) {
        for (int i = topLevel.size() - 1; i >= 0; i--) {
            ComponentDeclaration decl = topLevel.get(i);
            if (decl.getSourceElement() instanceof XSLCharacterMap) {
                XSLCharacterMap t = (XSLCharacterMap) decl.getSourceElement();
                if (t.getCharacterMapName().equals(name)) {
                    return decl;
                }
            }
        }
        return null;
    }


    /**
     * Ensure there is enough space for local variables or parameters when evaluating the match pattern of
     * template rules
     *
     * @param n the number of slots to be allocated
     */

    public void allocatePatternSlots(int n) {
        if (n > largestPatternStackFrame) {
            largestPatternStackFrame = n;
        }
    }


    /**
     * Compile time error, specifying an error code
     *
     * @param message   the error message
     * @param errorCode the error code. May be null if not known or not defined
     * @throws XPathException unconditionally
     */

    protected void compileError(String message, String errorCode) throws XPathException {
        XPathException tce = new XPathException(message);
        tce.setErrorCode(errorCode);
        compileError(tce);
    }

    /**
     * Report an error with diagnostic information
     *
     * @param error contains information about the error
     * @throws XPathException unconditionally, after reporting the error to the ErrorListener
     */

    protected void compileError(XPathException error)
            throws XPathException {
        error.setIsStaticError(true);
        getRootElement().compileError(error);
    }

    /**
     * Register a package that is referenced from this one in an xsl:use-package declaration
     *
     * @param pack the used package
     * @throws XPathException if duplicate components are found
     */

    public void addUsedPackage(StylesheetPackage pack) throws XPathException {
        usedPackages.add(pack);
        // Create copies of the components in the used package, with suitably adjusted visibility
        // This currently implements the default rules only: no adjustment for xsl:accept or xsl:override
        for (Map.Entry<SymbolicName, Component> e : pack.componentIndex.entrySet()) {
            Component c = e.getValue();

            // Spec section 3.6.2.4: The potential visibility of C(Q) will be the same as the (actual) visibility of C(P),
            // except that where the visibility of C(P) is private, the potential visibility of C(Q) will be hidden.
            Visibility oldV = c.getVisibility();
            Visibility newV = oldV == Visibility.PRIVATE ? Visibility.HIDDEN : oldV;

            final Component d = new Component(c.getProcedure(), newV, this, c.getDeclaringPackage());
            d.setOriginalComponent(c);
            d.setComponentBindings(c.getComponentBindings());

            if (newV == Visibility.HIDDEN) {
                hiddenComponents.add(d);
            } else if (componentIndex.get(e.getKey()) != null) {
                XPathException err = new XPathException("Duplicate " + e.getKey(), "XTSE3050");
                err.setLocator(c.getProcedure());
                compileError(err);
            } else {
                componentIndex.put(e.getKey(), d);
            }

            addCompletionAction(new Action() {
                @Override
                public void doAction() throws XPathException {
                    List<ComponentBinding> bindings = d.getComponentBindings();
                    List<ComponentBinding> newBindings = new ArrayList<ComponentBinding>(bindings.size());
                    for (ComponentBinding binding : bindings) {
                        Visibility vis = binding.getTarget().getVisibility();
                        switch (vis) {
                            case ABSTRACT:
                            case PUBLIC: {
                                SymbolicName name = binding.getSymbolicName();
                                ComponentBinding b2 = new ComponentBinding(name);
                                b2.setTarget(componentIndex.get(name), false); // TODO: isFinal?
                                newBindings.add(b2);
                                break;
                            }
                            case FINAL:
                                newBindings.add(binding);
                                break;
                            case PRIVATE: {
                                SymbolicName name = binding.getSymbolicName();
                                ComponentBinding b2 = new ComponentBinding(name);
                                for (Component c : hiddenComponents) {
                                    if (c.getOriginalComponent() == binding.getTarget()) {
                                        b2.setTarget(c, true);
                                        newBindings.add(b2);
                                        break;
                                    }
                                }
                            }
                        }
                    }
                    d.setComponentBindings(newBindings);
                }
            });
        }
        if (pack.isCreatesSecondaryResultDocuments()) {
            setCreatesSecondaryResultDocuments(true);
        }
    }

    /**
     * Get the packages referenced from this package in an xsl:use-package declaration
     *
     * @return the packages used by this package
     */

    public Iterable<StylesheetPackage> getUsedPackages() {
        return usedPackages;
    }

    /**
     * Set the version of the XSLT language specification to which the package manifest conforms
     *
     * @param version the version as a DecimalValue type
     */
    public void setVersion(DecimalValue version) {
        this.version = version;
    }

    /**
     * Set the name of the package
     *
     * @param packageName the name of the package. This is supposed to be an absolute URI, but Saxon accepts any string.
     */
    public void setPackageName(String packageName) {
        this.packageName = packageName;
    }

    /**
     * Get the version of the XSLT language specification to which the package manifest conforms
     *
     * @return the version of the xslPackage as a DecimalValue
     */
    public DecimalValue getVersion() {
        return version;
    }

    /**
     *  Get the package-version identifier appearing on the xsl:package element
     *
     * @return  the version identifier as a structured entity
     */
    public PackageVersion getPackageVersion() {
        return packageVersion;
    }

    /**
     *  Set the package-version identifier appearing on the xsl:package element
     *
     * @param version  the version identifier as a structured entity
     */
    public void setPackageVersion(PackageVersion version) {
        packageVersion = version;
    }

    /**
     * Get the name identifier of the xsl:package as an absolute URI
     *
     * @return a string value. This is supposed to be an absolute URI, but Saxon accepts any string.
     */
    public String getPackageName() {
        return packageName;
    }

    public boolean isRootPackage() {
        return true;
    }

    protected void fixup() throws XPathException {
        // Perform the fixup actions
        for (Action a : fixupActions) {
            a.doAction();
        }
    }

    protected void complete() throws XPathException {
        // Perform the completion actions
        for (Action a : completionActions) {
            a.doAction();
        }
    }

    public void explain(ExpressionPresenter presenter) {
        presenter.startElement("package");
        presenter.emitAttribute("name", packageName);
        presenter.emitAttribute("version", version.getStringValue());
        if (packageVersion != null) {
            presenter.emitAttribute("package-version", packageVersion.getStringValue());
        }

        //pst.explain(presenter);
        presenter.endElement();
    }

    public SlotManager getSlotManager() {
        return null;
    }

    public Collection<StructuredQName> getGlobalVariableNames() {
        return globalVariableIndex.keySet();
    }

    public GlobalVariable getGlobalVariable(StructuredQName name) {
        ComponentDeclaration decl = globalVariableIndex.get(name);
        if (decl != null) {
            return ((XSLGlobalVariable)decl.getSourceElement()).getCompiledVariable();
        } else {
            return null;
        }
    }

    public void addGlobalVariable(GlobalVariable variable) {
        variable.makeDeclaringComponent(Visibility.PRIVATE, this);
        if (variable.getPackageData() == null) {
            variable.setPackageData(makePackageData());
        }
        componentIndex.put(new SymbolicName(StandardNames.XSL_VARIABLE, variable.getVariableQName()),
                variable.getDeclaringComponent());
    }

    private PackageData makePackageData() {
        PackageData pd = new PackageData(getConfiguration());
        pd.setAllowXPath30(false); // initially
        pd.setHostLanguage(Configuration.XSLT);
        return pd;
    }

    public void setCreatesSecondaryResultDocuments(boolean flag) {
        createsSecondaryResultDocuments = flag;
    }

    public boolean isCreatesSecondaryResultDocuments() {
        return createsSecondaryResultDocuments;
    }
}

