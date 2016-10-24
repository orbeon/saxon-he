////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
// Copyright (c) 2015 Saxonica Limited.
// This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
// If a copy of the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
// This Source Code Form is "Incompatible With Secondary Licenses", as defined by the Mozilla Public License, v. 2.0.
////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

package net.sf.saxon.style;

//import com.saxonica.xslt3.style.XSLMode;
import net.sf.saxon.Configuration;
import net.sf.saxon.expr.Component;
import net.sf.saxon.expr.Expression;
import net.sf.saxon.expr.instruct.*;
import net.sf.saxon.expr.parser.ExpressionTool;
import net.sf.saxon.expr.parser.Optimizer;
import net.sf.saxon.functions.ExecutableFunctionLibrary;
import net.sf.saxon.lib.SaxonOutputKeys;
import net.sf.saxon.om.DocumentURI;
import net.sf.saxon.om.NamespaceBinding;
import net.sf.saxon.om.StandardNames;
import net.sf.saxon.om.StructuredQName;
import net.sf.saxon.query.XQueryFunction;
import net.sf.saxon.query.XQueryFunctionLibrary;
import net.sf.saxon.serialize.CharacterMap;
import net.sf.saxon.serialize.CharacterMapIndex;
import net.sf.saxon.trans.*;
import net.sf.saxon.value.Whitespace;
import net.sf.saxon.z.IntHashMap;

import java.util.*;


/**
 * Represents both the stylesheet module at the root of the import tree, that is, the module
 * that includes or imports all the others, and also the XSLT package that has this stylesheet
 * module as its root.
 * <p/>
 * This version of the StylesheetPackage class represents a trivial package that is constructed
 * by the system to wrap an ordinary (non-package) stylesheet, as available in XSLT 2.0. A subclass
 * is used for "real" packages, currently available only in Saxon-EE.
 */
public class PrincipalStylesheetModule extends StylesheetModule implements GlobalVariableManager {

    /**
     * A class that simply encapsulates a callback action of any kind
     */

    public static abstract class Action {
        public abstract void doAction() throws XPathException;
    }

    private StylesheetPackage stylesheetPackage;
    private boolean declaredModes;


    // table of functions imported from XQuery library modules
    //private XQueryFunctionLibrary queryFunctions;

    // library of functions that are in-scope for XPath expressions in this stylesheet
    //private FunctionLibraryList functionLibrary;

    // index of global variables and parameters, by StructuredQName
    // (overridden variables are excluded).
    private HashMap<StructuredQName, ComponentDeclaration> globalVariableIndex =
            new HashMap<StructuredQName, ComponentDeclaration>(20);


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
    private IAccumulatorRegistry accumulatorManager = null;

    // namespace aliases. This information is needed at compile-time only
    private int numberOfAliases = 0;
    private List<ComponentDeclaration> namespaceAliasList = new ArrayList<ComponentDeclaration>(5);
    private HashMap<String, NamespaceBinding> namespaceAliasMap;
    private Set<String> aliasResultUriSet;

    // attribute sets. A package can contain several declarations attribute sets with the same name.
    // They are indexed in the order they will be applied, that is, highest precedence first
    private Map<StructuredQName, List<ComponentDeclaration>> attributeSetDeclarations = new HashMap<StructuredQName, List<ComponentDeclaration>>();

    // cache of stylesheet documents. Note that multiple imports of the same URI
    // lead to the stylesheet tree being reused
    private HashMap<DocumentURI, XSLModuleRoot> moduleCache = new HashMap<DocumentURI, XSLModuleRoot>(4);


    private CharacterMapIndex characterMapIndex;

    private List<Action> fixupActions = new ArrayList<Action>();
    private List<Action> completionActions = new ArrayList<Action>();
    //private boolean createsSecondaryResultDocuments = false;
    private boolean needsDynamicOutputProperties = false;


    /**
     * Create a PrincipalStylesheetModule
     *
     * @param sourceElement the xsl:package element at the root of the package manifest
     */

    public PrincipalStylesheetModule(XSLPackage sourceElement) throws XPathException {
        super(sourceElement, 0);
        declaredModes = sourceElement.isDeclaredModes();
        stylesheetPackage = getConfiguration().makeStylesheetPackage();
        int xPathVersion = sourceElement.getProcessorVersion() >= 30 ? 31 : 20;
        stylesheetPackage.setXPathVersion(xPathVersion);
        stylesheetPackage.setTargetEdition(sourceElement.getCompilation().getCompilerInfo().getTargetEdition());

        keyManager = stylesheetPackage.getKeyManager();
        decimalFormatManager = stylesheetPackage.getDecimalFormatManager();

        ruleManager = new RuleManager(stylesheetPackage, sourceElement.getCompilation().getCompilerInfo());
        ruleManager.getUnnamedMode().makeDeclaringComponent(Visibility.PRIVATE, stylesheetPackage);
        stylesheetPackage.setRuleManager(ruleManager);
        stylesheetPackage.setDefaultMode(sourceElement.getDefaultMode());

        characterMapIndex = new CharacterMapIndex();
        stylesheetPackage.setCharacterMapIndex(characterMapIndex);

        try {
            setInputTypeAnnotations(sourceElement.getInputTypeAnnotationsAttribute());
        } catch (XPathException err) {
            // it will be reported some other time
        }
    }


    /**
     * Search the package for a component with a given name
     *
     * @param name the symbolic name of the required component
     * @return the requested component if found, or null otherwise
     */

    public Component getComponent(SymbolicName name) {
        return stylesheetPackage.getComponentIndex().get(name);
    }

    /**
     * Remove a global variable if it is found to be redundant, i.e. it is private to the package
     * and not referenced
     *
     * @param decl the global variable to be removed
     */

    public void removeGlobalVariable(ComponentDeclaration decl) {
        StructuredQName name = decl.getSourceElement().getObjectName();
        SymbolicName sName = new SymbolicName(StandardNames.XSL_VARIABLE, name);
        if (globalVariableIndex.get(name) == decl) {
            stylesheetPackage.getComponentIndex().remove(sName);
            globalVariableIndex.remove(name);
        }
    }

    /**
     * Get the outermost stylesheet module in a package
     *
     * @return this module (this class represents both the package and its outermost module)
     */

    /*@NotNull*/
    @Override
    public PrincipalStylesheetModule getPrincipalStylesheetModule() {
        return this;
    }

    /**
     * Get the stylesheet package
     *
     * @return the associated stylesheet package
     */

    public StylesheetPackage getStylesheetPackage() {
        return stylesheetPackage;
    }

    /**
     * Get the key manager used to manage xsl:key declarations in this package
     *
     * @return the key manager
     */

    public KeyManager getKeyManager() {
        return keyManager;
    }

    /**
     * Get the decimal format manager used to manage xsl:decimal-format declarations in this package
     *
     * @return the decimal format manager
     */

    public DecimalFormatManager getDecimalFormatManager() {
        return decimalFormatManager;
    }

    /**
     * Get the rule manager used to manage modes declared explicitly or implicitly in this package
     *
     * @return the rule manager
     */

    public RuleManager getRuleManager() {
        return ruleManager;
    }

    /**
     * Ask whether it is required that modes be explicitly declared
     *
     * @return true if modes referenced within this package must be explicitly declared
     */

    public boolean isDeclaredModes() {
        return declaredModes;
    }


    /**
     * Register a callback action to be performed during the fixup phase
     *
     * @param action the callback action
     */

    public void addFixupAction(Action action) {
        fixupActions.add(action);
    }

    /**
     * Register a callback action to be performed during the completion phase
     *
     * @param action the callback action
     */

    public void addCompletionAction(Action action) {
        completionActions.add(action);
    }


    /**
     * Say that this stylesheet package needs dynamic output properties
     *
     * @param b true if this stylesheet needs dynamic output properties
     */

    public void setNeedsDynamicOutputProperties(boolean b) {
        needsDynamicOutputProperties = b;
    }


    /**
     * Get an index of character maps declared using xsl:character-map entries in this package
     *
     * @return the character map index
     */

    public CharacterMapIndex getCharacterMapIndex() {
        return characterMapIndex;
    }


    /**
     * Declare an imported XQuery function
     *
     * @param function the imported function
     * @throws net.sf.saxon.trans.XPathException if an error occurs
     */

    public void declareXQueryFunction(XQueryFunction function) throws XPathException {
        getStylesheetPackage().getXQueryFunctionLibrary().declareFunction(function);
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
     * @throws net.sf.saxon.trans.XPathException if errors are found in the stylesheet
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

        XSLPackage top = (XSLPackage) getStylesheetElement();
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

        // Gather the output properties

        Properties props = gatherOutputProperties(null);
        props.setProperty(SaxonOutputKeys.STYLESHEET_VERSION, getStylesheetPackage().getVersion() + "");
        getStylesheetPackage().setDefaultOutputProperties(props);

        // Handle named output formats for use at run-time

        HashSet<StructuredQName> outputNames = new HashSet<StructuredQName>(5);
        for (ComponentDeclaration outputDecl : topLevel) {
            if (outputDecl.getSourceElement() instanceof XSLOutput) {
                XSLOutput out = (XSLOutput) outputDecl.getSourceElement();
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
                    getStylesheetPackage().setNamedOutputProperties(qName, oprops);
                }
            }
        }

        // Index the character maps

        for (ComponentDeclaration d : topLevel) {
            StyleElement inst = d.getSourceElement();
            if (inst instanceof XSLCharacterMap) {
                XSLCharacterMap xcm = (XSLCharacterMap) inst;
                StructuredQName qn = xcm.getCharacterMapName();
                IntHashMap<String> map = new IntHashMap<String>();
                xcm.assemble(map);
                characterMapIndex.putCharacterMap(xcm.getCharacterMapName(), new CharacterMap(qn, map));
            }
        }

    }

    /**
     * Incorporate declarations from used packages
     *
     * @param xslpackage  the used package
     * @param compilation this compilation
     * @throws XPathException if any error is detected in the used package
     */

    protected void spliceUsePackages(XSLPackage xslpackage, Compilation compilation) throws XPathException {
        CompilerInfo info = compilation.getCompilerInfo();

        if (info.isVersionWarning() &&
                xslpackage.getEffectiveVersion() != compilation.getStyleNodeFactory(true).getXsltProcessorVersion()) {
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
     * @throws net.sf.saxon.trans.XPathException if errors are detected
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
     * @throws net.sf.saxon.trans.XPathException if errors are detected
     */

    private void buildIndexes() throws XPathException {
        // Scan the declarations in reverse order, that is, highest precedence first
        for (int i = topLevel.size() - 1; i >= 0; i--) {
            ComponentDeclaration decl = topLevel.get(i);
            decl.getSourceElement().index(decl, this);
        }
    }

    /**
     * Process the attributes of every node in the stylesheet
     *
     * @throws net.sf.saxon.trans.XPathException if static errors are found in the stylesheet
     */

    public void processAllAttributes() throws XPathException {
        getRootElement().processDefaultCollationAttribute();
        getRootElement().processDefaultMode();
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
        HashMap<SymbolicName, Component> componentIndex = stylesheetPackage.getComponentIndex();
        XSLFunction sourceFunction = (XSLFunction) decl.getSourceElement();
        UserFunction compiledFunction = sourceFunction.getCompiledFunction();
        Component declaringComponent = compiledFunction.getDeclaringComponent();
        if (declaringComponent == null) {
            declaringComponent = compiledFunction.makeDeclaringComponent(sourceFunction.getVisibility(), getStylesheetPackage());
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
                if (other.getDeclaringPackage() == getStylesheetPackage()) {
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
                } else if (sourceFunction.findAncestorElement(StandardNames.XSL_OVERRIDE) != null) {
                    // the new one wins
                    componentIndex.put(sName, declaringComponent);
                    functionIndex.put(sName, decl);
                } else {
                    sourceFunction.compileError("Function " + sName.getComponentName().getDisplayName() + "#" + sName.getArity() +
                        " conflicts with a public function in package " + other.getDeclaringPackage().getPackageName(), "XTSE3050");

                }
            }
        }

    }

    /**
     * Index a global xsl:variable or xsl:param element
     *
     * @param decl The Declaration referencing the XSLVariable or XSLParam element
     * @throws XPathException if an error occurs
     */

    protected void indexVariableDeclaration(ComponentDeclaration decl) throws XPathException {
        XSLGlobalVariable varDecl = (XSLGlobalVariable) decl.getSourceElement();
        StructuredQName qName = varDecl.getSourceBinding().getVariableQName();
        GlobalVariable compiledVariable = (GlobalVariable)varDecl.getCompiledProcedure();
        Component declaringComponent = compiledVariable.getDeclaringComponent();
        if (declaringComponent == null) {
            declaringComponent = compiledVariable.makeDeclaringComponent(varDecl.getDeclaredVisibility(), getStylesheetPackage());
        }
        HashMap<SymbolicName, Component> componentIndex = stylesheetPackage.getComponentIndex();
        if (qName != null) {
            // see if there is already a global variable with this precedence
            SymbolicName sName = varDecl.getSymbolicName();
            Component other = componentIndex.get(sName);
            if (other == null) {
                // this is the first
                globalVariableIndex.put(qName, decl);
                componentIndex.put(new SymbolicName(StandardNames.XSL_VARIABLE, qName),
                    varDecl.getCompiledProcedure().getDeclaringComponent());
            } else {
                if (other.getDeclaringPackage() == getStylesheetPackage()) {
                    // check the precedences
                    int thisPrecedence = decl.getPrecedence();
                    ComponentDeclaration otherVarDecl = globalVariableIndex.get(sName.getComponentName());
                    int otherPrecedence = otherVarDecl.getPrecedence();
                    if (thisPrecedence == otherPrecedence) {
                        StyleElement v2 = otherVarDecl.getSourceElement();
                        if (v2 == varDecl) {
                            varDecl.compileError(
                                "Global variable or parameter $" + qName.getDisplayName() + " is declared more than once " +
                                    "(caused by including the containing module more than once)",
                                "XTSE0630");
                        } else {
                            varDecl.compileError("Duplicate global variable/parameter declaration (see line " +
                                v2.getLineNumber() + " of " + v2.getSystemId() + ')', "XTSE0630");
                        }
                    } else if (thisPrecedence < otherPrecedence && varDecl != otherVarDecl.getSourceElement()) {
                        varDecl.setRedundant(true);
                    } else if (varDecl != otherVarDecl.getSourceElement()) {
                        ((XSLGlobalVariable) otherVarDecl.getSourceElement()).setRedundant(true);
                        globalVariableIndex.put(qName, decl);
                        componentIndex.put(new SymbolicName(StandardNames.XSL_VARIABLE, qName),
                            varDecl.getCompiledProcedure().getDeclaringComponent());
                    }
                } else if (varDecl.findAncestorElement(StandardNames.XSL_OVERRIDE) != null) {
                    // the new one wins
                    componentIndex.put(sName, declaringComponent);
                    globalVariableIndex.put(sName.getComponentName(), decl);
                } else {
                    String kind = varDecl instanceof XSLGlobalParam ? "parameter" : "variable";
                    varDecl.compileError("Global " + kind + " $" + sName.getComponentName().getDisplayName() +
                        " conflicts with a public variable/parameter in package " + other.getDeclaringPackage().getPackageName(), "XTSE3050");

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
        HashMap<SymbolicName, Component> componentIndex = stylesheetPackage.getComponentIndex();
        XSLTemplate sourceTemplate = (XSLTemplate) decl.getSourceElement();
        if (sourceTemplate.getTemplateName() == null) {
            return; // Not a named template
        }
        NamedTemplate compiledTemplate = sourceTemplate.getCompiledNamedTemplate();
        Component declaringComponent = compiledTemplate.getDeclaringComponent();
        if (declaringComponent == null) {
            declaringComponent = compiledTemplate.makeDeclaringComponent(sourceTemplate.getVisibility(), getStylesheetPackage());
        }
        SymbolicName sName = sourceTemplate.getSymbolicName();
        if (sName != null) {
            // see if there is already a named template with this precedence
            Component other = componentIndex.get(sName);
            if (other == null) {
                // this is the first
                componentIndex.put(sName, declaringComponent);
                templateIndex.put(sName.getComponentName(), decl);
            } else {
                if (other.getDeclaringPackage() == getStylesheetPackage()) {
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
                } else if (sourceTemplate.findAncestorElement(StandardNames.XSL_OVERRIDE) != null) {
                    // the new one wins
                    componentIndex.put(sName, declaringComponent);
                    templateIndex.put(sName.getComponentName(), decl);
                } else {
                    sourceTemplate.compileError("Named template " + sName.getComponentName().getDisplayName() +
                        " conflicts with a public named template in package " + other.getDeclaringPackage().getPackageName(), "XTSE3050");

                }
            }
        }
    }

    /**
     * Get the named template with a given name
     *
     * @param name the name of the required template
     * @return the template with the given name, if there is one, or null otherwise. If there
     * are several templates with the same name, the one with highest import precedence
     * is returned. Note that this template may subsequently be overridden in another package,
     * but only by another template with a compatible signature.
     */

    public NamedTemplate getNamedTemplate(StructuredQName name) {
        HashMap<SymbolicName, Component> componentIndex = stylesheetPackage.getComponentIndex();
        Component component = componentIndex.get(new SymbolicName(StandardNames.XSL_TEMPLATE, name));
        return component == null ? null : (NamedTemplate) component.getCode();
    }

    /**
     * Add an attribute set to the index
     *
     * @param decl the declaration of the attribute set object
     * @throws XPathException if an error occurs
     */
    protected void indexAttributeSet(ComponentDeclaration decl) throws XPathException {
        //HashMap<SymbolicName, Component> componentIndex = stylesheetPackage.getComponentIndex();
        XSLAttributeSet sourceAttributeSet = (XSLAttributeSet) decl.getSourceElement();
        StructuredQName name = sourceAttributeSet.getAttributeSetName();
        List<ComponentDeclaration> entries = attributeSetDeclarations.get(name);
        if (entries == null) {
            entries = new ArrayList<ComponentDeclaration>();
            attributeSetDeclarations.put(name, entries);
        } else {
            String thisVis = Whitespace.trim(sourceAttributeSet.getAttributeValue("", "visibility"));
            String firstVis = Whitespace.trim(entries.get(0).getSourceElement().getAttributeValue("", "visibility"));
            if (!(thisVis == null ? firstVis == null : thisVis.equals(firstVis))) {
                throw new XPathException("Visibility attributes on attribute-sets sharing the same name must all be the same", "XTSE0010");
            }
        }
        entries.add(0, decl);
    }

    /**
     * Return all the attribute set declarations in the package having a particular QName
     *
     * @param name the name of the required declarations
     * @return the set of declarations having this name, or null if there are none
     */

    public List<ComponentDeclaration> getAttributeSetDeclarations(StructuredQName name) {
        return attributeSetDeclarations.get(name);
    }

    /**
     * Ask whether a particular attribute set exists and is declared streamable
     *
     * @param name the name of the required attribute set
     * @return true if it exists and is declared streamable
     */

    public boolean isAttributeSetDeclaredStreamable(StructuredQName name) {
        List<ComponentDeclaration> entries = attributeSetDeclarations.get(name);
        if (entries == null || entries.isEmpty()) {
            return false;
        }
        // if one is declared streamable, they all must be
        XSLAttributeSet aSet = (XSLAttributeSet) entries.get(0).getSourceElement();
        return aSet.isDeclaredStreamable();
    }

    /**
     * Combine all like-named xsl:attribute-set declarations in this package into a single Component, whose body
     * is an AttributeSet object. The resulting attribute set Components are saved in the StylesheetPackage
     * object.
     *
     * @throws XPathException if a failure occurs
     */


    public void combineAttributeSets(Compilation compilation) throws XPathException {
        Map<StructuredQName, AttributeSet> index = new HashMap<StructuredQName, AttributeSet>();
        for (Map.Entry<StructuredQName, List<ComponentDeclaration>> entry : attributeSetDeclarations.entrySet()) {
            AttributeSet as = new AttributeSet();
            as.setName(entry.getKey());
            as.setPackageData(stylesheetPackage);
            StyleElement firstDecl = entry.getValue().get(0).getSourceElement();
            as.setSystemId(firstDecl.getSystemId());
            as.setLineNumber(firstDecl.getLineNumber());
            index.put(entry.getKey(), as);

            Component declaringComponent = as.getDeclaringComponent();
            if (declaringComponent == null) {
                declaringComponent = as.makeDeclaringComponent(Visibility.PRIVATE, stylesheetPackage);
            }
            stylesheetPackage.addComponent(declaringComponent);
        }
        for (Map.Entry<StructuredQName, List<ComponentDeclaration>> entry : attributeSetDeclarations.entrySet()) {
            List<Expression> content = new ArrayList<Expression>();
            Visibility vis = null;
            boolean explicitVisibility = false;
            boolean streamable = false;
            for (ComponentDeclaration decl : entry.getValue()) {
                XSLAttributeSet src = (XSLAttributeSet) decl.getSourceElement();
                streamable |= src.isDeclaredStreamable();
                src.compileDeclaration(compilation, decl);
                content.addAll(src.getContainedInstructions());
                vis = src.getVisibility();
                explicitVisibility = explicitVisibility || src.getAttributeValue("", "visibility") != null;
            }

            AttributeSet aSet = index.get(entry.getKey());
            aSet.setDeclaredStreamable(streamable);
            Expression block = Block.makeBlock(content);
            aSet.setBody(block);
            SlotManager frame = new SlotManager();
            ExpressionTool.allocateSlots(block, 0, frame);
            aSet.setStackFrameMap(frame);
            aSet.getDeclaringComponent().setVisibility(vis, explicitVisibility);

            if (streamable) {
                checkStreamability(aSet);
            }

        }
    }

    /**
     * Check the streamability of an attribute set declared within this stylesheet module.
     * (Null implementation for Saxon-HE).
     * @param aSet the attribute set to be checked
     * @throws XPathException if the streamability rules are not satisifed
     */

    protected void checkStreamability(AttributeSet aSet) throws XPathException {
    }

    /**
     * Get the list of attribute-set declarations associated with a given QName.
     * This is used for xsl:element, xsl:copy, xsl:attribute-set, and on literal
     * result elements
     *
     * @param name the name of the required attribute set
     * @param list a list to hold the list of XSLAttributeSet elements in the stylesheet tree.
     * @return true if any declarations were found and added to the list; false if none were found
     * @throws net.sf.saxon.trans.XPathException if an error occurs
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
                    list.add(decl);
                    found = true;
                }
            }
        }
        return found;
    }

    /**
     * Handle an explicitly-declared mode
     *
     * @param decl the declaration of the Mode object
     * @throws XPathException if an error occurs
     */
    public void indexMode(ComponentDeclaration decl) throws XPathException {
        // Work done in subclass
    }

    /**
     * Check that it is legitimate to add a given template rule to a given mode
     *
     * @param template the template rule
     * @param mode     the mode
     * @return true if all is well
     * @throws XPathException if the mode is declared in a used package and the template
     *                        is not declared within the relevant xsl:mode declaration
     */

    public boolean checkAcceptableModeForPackage(XSLTemplate template, Mode mode) throws XPathException {
        // no action except in subclass
        return true;
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

    public IAccumulatorRegistry getAccumulatorManager() {
        return accumulatorManager;
    }

    /**
     * Set the class that manages accumulator functions
     *
     * @param accumulatorManager the manager of accumulator functions
     */

    public void setAccumulatorManager(IAccumulatorRegistry accumulatorManager) {
        this.accumulatorManager = accumulatorManager;
        stylesheetPackage.setAccumulatorRegistry(accumulatorManager);
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
     * if no alias is defined
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
     * @throws net.sf.saxon.trans.XPathException if an error occurs
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
     * Compile the source XSLT stylesheet package
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
            XQueryFunctionLibrary queryFunctions = stylesheetPackage.getXQueryFunctionLibrary();
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

            // Compile groups of like-named attribute sets into a single attributeSet object
            //combineAttributeSets(compilation);

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
                    if (node.getVisibility() != Visibility.ABSTRACT) {
                        ((XSLFunction) node).getCompiledFunction().typeCheck(node.makeExpressionVisitor());
                    }
                }
            }

            if (Compilation.TIMING) {
                timer.report("typeCheck functions");
            }

            if (compilation.getErrorCount() > 0) {
                // not much point carrying on
                return;
            }

            // Call optimize() method for each top-level declaration

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

            // Check consistency of modes

            getRuleManager().checkConsistency();


            if (Compilation.TIMING) {
                timer.report("build template rule tables");
            }

            // Build a run-time function library. This supports the use of function-available()
            // with a dynamic argument, and extensions such as saxon:evaluate(). The run-time
            // function library differs from the compile-time function library in that both
            // the StylesheetFunctionLibrary's on the library list are replaced by equivalent
            // ExecutableFunctionLibrary's. This is to prevent the retaining of run-time links
            // to the stylesheet document tree.

            ExecutableFunctionLibrary overriding = new ExecutableFunctionLibrary(config);
            ExecutableFunctionLibrary underriding = new ExecutableFunctionLibrary(config);

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

            getStylesheetPackage().setFunctionLibraryDetails(null, overriding, underriding);

            if (Compilation.TIMING) {
                timer.report("build runtime function tables");
                timer.reportCumulative("total compile phase");
            }

            // Allocate binding slots to named templates

            for (ComponentDeclaration decl : topLevel) {
                StyleElement inst = decl.getSourceElement();
                if (inst instanceof XSLTemplate) {
                    NamedTemplate proc = ((XSLTemplate) inst).getCompiledProcedure();
                    if (proc != null && proc.getTemplateName() == null) {
                        proc.allocateAllBindingSlots(stylesheetPackage);
                    }
                }
            }

            // Allocate binding slots to template rules

//            getRuleManager().getUnnamedMode().allocateAllBindingSlots(stylesheetPackage);
//            for (Mode m : getRuleManager().getAllNamedModes()) {
//                if (m.getDeclaringComponent() == null) {
//                    // This is temporary scaffolding, so long as modes are local to a package
//                    m.makeDeclaringComponent(Visibility.PRIVATE, stylesheetPackage);
//                }
//                m.allocateAllBindingSlots(stylesheetPackage);
//            }

            // Allocate binding slots to component reference expressions

            HashMap<SymbolicName, Component> componentIndex = stylesheetPackage.getComponentIndex();
            for (Component decl : componentIndex.values()) {
                ComponentCode proc = decl.getCode();
                if (proc != null) {
                    proc.allocateAllBindingSlots(stylesheetPackage);
                }
            }

            // Allocate binding slots in /*optimizer-generated*/ key definitions

            KeyManager keyMan = getKeyManager();
            for (KeyDefinitionSet keySet : keyMan.getAllKeyDefinitionSets()) {
                //if (keySet.getKeyName().hasURI(NamespaceConstant.SAXON)) {
                    for (KeyDefinition keyDef : keySet.getKeyDefinitions()) {
                        keyDef.makeDeclaringComponent(Visibility.PRIVATE, getStylesheetPackage());
                        keyDef.allocateAllBindingSlots(stylesheetPackage);
                    }
                //}
            }

            // Allocate binding slots in accumulators

            IAccumulatorRegistry accMan = getAccumulatorManager();
            if (accMan != null) {
                for (ComponentCode acc : accMan.getAllAccumulators()) {
                    acc.allocateAllBindingSlots(stylesheetPackage);
                }
            }

            // Generate byte code where appropriate

            Optimizer opt = config.obtainOptimizer();
            for (ComponentDeclaration decl : topLevel) {
                StyleElement inst = decl.getSourceElement();
                if (inst instanceof StylesheetComponent) {
                    ((StylesheetComponent) inst).generateByteCode(opt);
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
                ((XSLTemplate) node).allocatePatternSlotNumbers();
            }
        }
    }


    /**
     * Get an imported schema with a given namespace
     *
     * @param targetNamespace The target namespace of the required schema.
     *                        Supply an empty string for the default namespace
     * @return the required Schema, or null if no such schema has been imported
     */

    protected boolean isImportedSchema(String targetNamespace) {
        return stylesheetPackage.getSchemaNamespaces().contains(targetNamespace);
    }

    protected void addImportedSchema(String targetNamespace) {
        stylesheetPackage.getSchemaNamespaces().add(targetNamespace);
    }

    protected Set<String> getImportedSchemaTable() {
        return stylesheetPackage.getSchemaNamespaces();
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
     * Adjust visibility of components by applying xsl:expose rules
     */

    public void adjustExposedVisibility() throws XPathException {
        // no action in HE
    }

    /**
     * Compile time error, specifying an error code
     *
     * @param message   the error message
     * @param errorCode the error code. May be null if not known or not defined
     * @throws XPathException unconditionally
     */

    protected void compileError(String message, String errorCode) throws XPathException {
        XPathException tce = new XPathException(message, errorCode);
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
        //adjustExposedVisibility();
        stylesheetPackage.complete();
    }

    public SlotManager getSlotManager() {
        return null;
    }

    @Override
    public Collection<StructuredQName> getGlobalVariableNames() {
        return globalVariableIndex.keySet();
    }

    @Override
    public GlobalVariable getGlobalVariable(StructuredQName name) {
        ComponentDeclaration decl = globalVariableIndex.get(name);
        if (decl != null) {
            return ((XSLGlobalVariable) decl.getSourceElement()).getCompiledVariable();
        } else {
            return null;
        }
    }

    @Override
    public void addGlobalVariable(GlobalVariable variable) {
        Component component = variable.makeDeclaringComponent(Visibility.PRIVATE, getStylesheetPackage());
        if (variable.getPackageData() == null) {
            variable.setPackageData(stylesheetPackage);
        }
        stylesheetPackage.getComponentIndex().put(
            new SymbolicName(StandardNames.XSL_VARIABLE, variable.getVariableQName()), component);
    }


    //project:preconditions

    /**
     * Find possibly keyable patterns, accumulating them in the PatternOptimization
     * and then emitting the necessary key definitions
     *
     * @param compilation
     * @throws XPathException
     */
    public void findKeyablePatterns(Compilation compilation) throws XPathException {
        // No action in Saxon-HE; overridden for Saxon-EE
    }

}

