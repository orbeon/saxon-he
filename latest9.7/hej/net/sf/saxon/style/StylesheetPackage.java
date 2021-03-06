////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
// Copyright (c) 2015 Saxonica Limited.
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
import net.sf.saxon.functions.*;
import net.sf.saxon.om.NoElementsSpaceStrippingRule;
import net.sf.saxon.om.SpaceStrippingRule;
import net.sf.saxon.om.StandardNames;
import net.sf.saxon.om.StructuredQName;
import net.sf.saxon.query.XQueryFunctionLibrary;
import net.sf.saxon.serialize.CharacterMap;
import net.sf.saxon.serialize.CharacterMapIndex;
import net.sf.saxon.trace.ExpressionPresenter;
import net.sf.saxon.trans.*;
import net.sf.saxon.tree.util.FastStringBuffer;

import java.util.*;

/**
 * A (compiled) stylesheet package. This may be created either by compiling a source XSLT package,
 * or by loading a saved package from disk. It therefore has no references to source XSLT documents.
 */
public class StylesheetPackage extends PackageData {

    /**
     * A class that simply encapsulates a callback action of any kind
     */

    public static abstract class Action {
        public abstract void doAction() throws XPathException;
    }

    private PackageVersion packageVersion = null;
    private String packageName;
    private List<StylesheetPackage> usedPackages = new ArrayList<StylesheetPackage>();
    private int xsltVersion;

    private RuleManager ruleManager;
    private CharacterMapIndex characterMapIndex;

    private boolean createsSecondaryResultDocuments;

    private List<Action> completionActions = new ArrayList<Action>();
    private boolean isRootPackage = true;
    protected GlobalContextRequirement globalContextRequirement = null;
    private boolean containsGlobalContextItemDeclaration = false;
    protected SpaceStrippingRule stripperRules;
    private boolean stripsWhitespace = false;
    private boolean stripsTypeAnnotations = false;
    protected Properties defaultOutputProperties;
    private StructuredQName defaultMode;
    protected Map<StructuredQName, Properties> namedOutputProperties = new HashMap<StructuredQName, Properties>(4);

    // table of imported schemas. The members of this set are strings holding the target namespace.
    protected Set<String> schemaIndex = new HashSet<String>(10);


    FunctionLibraryList functionLibrary;
    XQueryFunctionLibrary queryFunctions;
    ExecutableFunctionLibrary overriding;
    ExecutableFunctionLibrary underriding;
    private int maxFunctionArity = -1;


    // Table of components declared in this package or imported from used packages. Key is the symbolic identifier of the
    // component; value is the component itself. Hidden components are not included in this table because their names
    // need not be unique, and because they are not available for reference by name.

    private HashMap<SymbolicName, Component> componentIndex =
        new HashMap<SymbolicName, Component>(20);

    protected HashMap<SymbolicName, Component> hiddenComponents = new HashMap<SymbolicName, Component>();

    protected HashMap<SymbolicName, Component> overriddenComponents = new HashMap<SymbolicName, Component>();

    private HashMap<SymbolicName, Component> abstractComponents = new HashMap<SymbolicName, Component>();

    /**
     * Create a stylesheet package
     *
     * @param config the Saxon Configuration
     */

    public StylesheetPackage(Configuration config) {
        super(config);
        setHostLanguage(Configuration.XSLT);
        setAccumulatorRegistry(config.makeAccumulatorRegistry());
    }

    /**
     * Get a map containing all the components in this package, indexed by symbolic name
     *
     * @return a map containing all the components in the package. This does not include components
     * in used packages, except to the extent that they have been copied into this package.
     */

    public HashMap<SymbolicName, Component> getComponentIndex() {
        return componentIndex;
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
     * Add a package to the list of packages used by this one
     * @param pack the used package
     */

    public void addUsedPackage(StylesheetPackage pack) {
        usedPackages.add(pack);
    }

    /**
     * Set the version of the XSLT language specification to which the package manifest conforms
     *
     * @param version the version (times ten) as an integer
     */
    public void setVersion(int version) {
        this.xsltVersion = version;
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
     * @return the version of the xslPackage, times ten, as an integer
     */
    public int getVersion() {
        return xsltVersion;
    }

    /**
     * Get the package-version identifier appearing on the xsl:package element
     *
     * @return the version identifier as a structured entity
     */
    public PackageVersion getPackageVersion() {
        return packageVersion;
    }

    /**
     * Set the package-version identifier appearing on the xsl:package element
     *
     * @param version the version identifier as a structured entity
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

    public void setRootPackage(boolean isRoot) {
        isRootPackage = isRoot;
    }

    public boolean isRootPackage() {
        return isRootPackage;
    }

    /**
     * Get the rule manager, which knows about all the modes present in the package
     * @return the rule manager
     */

    public RuleManager getRuleManager() {
        return ruleManager;
    }

    /**
     * Set the rule manager, which knows about all the modes present in the package
     * @param ruleManager the rule manager
     */

    public void setRuleManager(RuleManager ruleManager) {
        this.ruleManager = ruleManager;
    }

    /**
     * Get the default mode for the package
     * @return the default mode
     * TODO: the default mode can now be defined on any element
     */

    public StructuredQName getDefaultMode() {
        return defaultMode;
    }

    /**
     * Set the default mode for the package
     * @param defaultMode
     */

    public void setDefaultMode(StructuredQName defaultMode) {
        this.defaultMode = defaultMode;
    }

    /**
     * Get the whitespace stripping rules for this package
     * @return the whitespace stripping rules (based on xsl:strip-space and xsl:preserve-space)
     */

    public SpaceStrippingRule getSpaceStrippingRule() {
        return stripperRules;
    }

    /**
     * Get the index of named character maps defined in this package
     * @return the index of character maps
     */

    public CharacterMapIndex getCharacterMapIndex() {
        return characterMapIndex;
    }

    /**
     * Set the index of named character maps defined in this package
     * @param characterMapIndex the index of named character maps
     */

    public void setCharacterMapIndex(CharacterMapIndex characterMapIndex) {
        this.characterMapIndex = characterMapIndex;
    }

    /**
     * Ask whether the package contains an xsl:result-document instruction
     * @return true if the package contains an xsl:result-document instruction
     */

    public boolean isCreatesSecondaryResultDocuments() {
        return createsSecondaryResultDocuments;
    }

    /**
     * Say whether the package contains an xsl:result-document instruction
     * @param createsSecondaryResultDocuments true if the package contains an xsl:result-document instruction
     */

    public void setCreatesSecondaryResultDocuments(boolean createsSecondaryResultDocuments) {
        this.createsSecondaryResultDocuments = createsSecondaryResultDocuments;
    }

    /**
     * Ask whether the package defines that type annotations should be stripped from input
     * documents loaded from within this package
     * @return true if documents loaded by this package should have any type annotations stripped.
     */

    public boolean isStripsTypeAnnotations() {
        return stripsTypeAnnotations;
    }

    /**
     * Say whether the package defines that type annotations should be stripped from input
     * documents loaded from within this package
     *
     * @param stripsTypeAnnotations set to true if documents loaded by this package
     *                              should have any type annotations stripped.
     */


    public void setStripsTypeAnnotations(boolean stripsTypeAnnotations) {
        this.stripsTypeAnnotations = stripsTypeAnnotations;
    }

    /**
     * Get the whitespace stripping rules for source documents loaded from within this package
     * @return the whitespace stripping rules for this package
     */

    public SpaceStrippingRule getStripperRules() {
        return stripperRules;
    }

    /**
     * Set the whitespace stripping rules for source documents loaded from within this package
     * @param stripperRules the whitespace stripping rules for this package
     */

    public void setStripperRules(SpaceStrippingRule stripperRules) {
        this.stripperRules = stripperRules;
    }

    /**
     * Set the default (unnamed) serialization properties to be used for documents output
     * using xsl:result-document instructions within this package
     * @param props the default serialization properties for this package
     */

    public void setDefaultOutputProperties(Properties props) {
        defaultOutputProperties = props;
    }

    /**
     * Define a named set serialization properties to be used for documents output
     * using xsl:result-document instructions within this package
     * @param name the name of the serialization property set
     * @param props the serialization properties
     */

    public void setNamedOutputProperties(StructuredQName name, Properties props) {
        namedOutputProperties.put(name, props);
    }

    /**
     * Get a set of named output properties
     * @param name the output declaration name
     * @return the corresponding properties; or null if the name is unknown
     */

    public Properties getNamedOutputProperties(StructuredQName name) {
        return namedOutputProperties.get(name);
    }

    /**
     * Get the set of namespaces of schema declarations imported into this package
     * @return the set of imported namespaces
     */

    public Set<String> getSchemaNamespaces() {
        return schemaIndex;
    }

    /**
     * Set the required context item type. Used when there is an xsl:global-context-item child element
     * @param requirement details of the requirement for the global context item
     */

    public void setContextItemRequirements(GlobalContextRequirement requirement) throws XPathException {
        containsGlobalContextItemDeclaration = true;
        globalContextRequirement = requirement;
    }

    /**
     * Get the required context item type and other details of the global context item.
     * Used when there is an xsl:global-context-item child element
     * @return details of the requirement for the global context item
     */

    public GlobalContextRequirement getContextItemRequirements() {
        return globalContextRequirement;
    }

    /**
     * Say whether there is an xsl:strip-space declaration in the stylesheet package
     *
     * @param strips true if an xsl:strip-space declaration has been found
     */

    public void setStripsWhitespace(boolean strips) {
        this.stripsWhitespace = strips;
    }

    /**
     * Register a callback action to be performed during the completion phase of building the package
     *
     * @param action the callback action
     */

    public void addCompletionAction(Action action) {
        completionActions.add(action);
    }

    /**
     * Perform all registered completion actions for the package
     * @throws XPathException if any of these completion actions fails
     */

    protected void complete() throws XPathException {
        // Perform the completion actions
        for (Action a : completionActions) {
            a.doAction();
        }
        allocateBinderySlots();

    }

    /**
     * Allocate slots to global variables. Slot numbers are unique within a package
     */

    public void allocateBinderySlots() {
        SlotManager slotManager = new SlotManager();
        for (Component c : componentIndex.values()) {
            registerGlobalVariable(c, slotManager);
        }
        for (Component c : hiddenComponents.values()) {
            registerGlobalVariable(c, slotManager);
        }
        setGlobalSlotManager(slotManager);
    }

    /**
     * Register a global variable declared within this package, allocating it a slot
     * number within the Bindery
     * @param c the component representing a global variable; it may also be another
     *          kind of component, but in this case it is ignored
     * @param slotManager the SlotManager defining the allocation of variables to
     *                    slots in the Bindery
     */

    private void registerGlobalVariable(Component c, SlotManager slotManager) {
        if (c.getCode() instanceof GlobalVariable) {
            GlobalVariable var = (GlobalVariable) c.getCode();
            int slot = slotManager.allocateSlotNumber(var.getVariableQName());
            var.setPackageData(this);
            var.setBinderySlotNumber(slot);
            addGlobalVariable(var);
        }
    }

    /**
     * Add a component to the package
     * @param component the component to be added
     */

    public void addComponent(Component component) {
        SymbolicName name = component.getCode().getSymbolicName();
        componentIndex.put(name, component);
        if (component.getVisibility() == Visibility.ABSTRACT) {
            abstractComponents.put(component.getCode().getSymbolicName(), component);
        }
    }

    @Override
    public void addGlobalVariable(GlobalVariable variable) {
        super.addGlobalVariable(variable);
        SymbolicName name = variable.getSymbolicName();
        if (componentIndex.get(name) == null) {
            Component<GlobalVariable> comp = variable.getDeclaringComponent();
            if (comp == null) {
                comp = variable.makeDeclaringComponent(Visibility.PRIVATE, this);
            }
            addComponent(comp);
        }
    }

    /**
     * Get the maximum arity of functions in this package
     * @return the maximum arity
     */

    public int getMaxFunctionArity() {
        if (maxFunctionArity == -1) {
            for (Component c : componentIndex.values()) {
                if (c.getCode() instanceof UserFunction) {
                    if (((UserFunction) c.getCode()).getArity() > maxFunctionArity) {
                        maxFunctionArity = ((UserFunction) c.getCode()).getArity();
                    }
                }
            }
        }
        return maxFunctionArity;
    }

    /**
     * Get the component within this package having a given symbolic name
     *
     * @param name the symbolic name of the required component
     * @return the requested component, or null if it does not exist in the package
     */

    public Component getComponent(SymbolicName name) {
        return componentIndex.get(name);
    }

    public Component getHiddenComponent(SymbolicName name) {
        return hiddenComponents.get(name);
    }

    public void addHiddenComponent(Component component) {
        hiddenComponents.put(component.getCode().getSymbolicName(), component);
    }

    /**
     * If this component overrides a component named N, get the component that N overrides (that
     * is, the component identified by xsl:original appearing within the overriding declaration of N)
     * @param name the name N
     * @return if there is an overriding declaration of N, then the original declaration of N from the
     * used package; otherwise null
     */

    public Component getOverriddenComponent(SymbolicName name) {
        return overriddenComponents.get(name);
    }

    public void addOverriddenComponent(Component comp) {
        overriddenComponents.put(comp.getCode().getSymbolicName(), comp);
    }

    /**
     * Add modified copies of components from a package that is referenced from this one in an xsl:use-package declaration
     *
     * @param usedPackage the used package
     * @param acceptors abstraction of a list of xsl:accept declarations, which can modify the visibility of accepted components
     * @param overrides the set of names of components from the used package that are overridden in the using package
     * @throws XPathException if duplicate components are found
     */

    public void addComponentsFromUsedPackage(StylesheetPackage usedPackage,
                                             List<ComponentAcceptor> acceptors,
                                             final Set<SymbolicName> overrides) throws XPathException {
        usedPackages.add(usedPackage);
        // Create copies of the components in the used package, with suitably adjusted visibility

        // Create a mapping from components in the used package to their corresponding components
        // in the using package, so that we can re-bind the component bindings

        final Map<Component, Component> correspondence = new HashMap<Component, Component>();

        for (Map.Entry<SymbolicName, Component> namedComponentEntry : usedPackage.componentIndex.entrySet()) {
            SymbolicName name = namedComponentEntry.getKey();
            Component<? extends ComponentCode> oldC = namedComponentEntry.getValue();

            // Spec section 3.6.2.4: The visibility of C(P) will be the same as the visibility of C(Q),
            // except that where the visibility of C(Q) is private, the visibility of C(P) will be hidden.
            Visibility oldV = oldC.getVisibility();
            Visibility newV = oldV == Visibility.PRIVATE ? Visibility.HIDDEN : oldV;

            final Component<ComponentCode> newC = new Component<ComponentCode>(oldC.getCode(), newV, this, oldC.getDeclaringPackage());
            correspondence.put(oldC, newC);
            newC.setBaseComponent(oldC);

            if (overrides.contains(name)) {
                // TODO: overrides is all the overrides, not only those for this xsl:use-package
                overriddenComponents.put(name, newC);
                if (newV != Visibility.ABSTRACT) {
                    abstractComponents.remove(name);
                }
            } else {
                if (newV != Visibility.HIDDEN) {
                    for (ComponentAcceptor acceptor : acceptors) {
                        acceptor.acceptComponent(newC);
                    }
                }
                if (oldV == Visibility.ABSTRACT) {
                    for (ComponentAcceptor acceptor : acceptors) {
                        acceptor.acceptComponent(newC);
                    }
                    if (newC.getVisibility() == Visibility.ABSTRACT) {
                        abstractComponents.put(name, newC);
                    }
                }
            }

            if (newC.getVisibility() == Visibility.HIDDEN) {
                hiddenComponents.put(namedComponentEntry.getKey(), newC);
            } else if (componentIndex.get(name) != null) {
                if (!(oldC.getCode() instanceof Mode)) {
                    throw new XPathException("Duplicate " + namedComponentEntry.getKey(), "XTSE3050", oldC.getCode());
                }
            } else {
                componentIndex.put(name, newC);
                if (oldC.getCode() instanceof Mode && (oldV == Visibility.PUBLIC || oldV == Visibility.FINAL) ) {
                    Mode existing = getRuleManager().obtainMode(name.getComponentName(), false);
                    if (existing != null) {
                        throw new XPathException("Duplicate " + namedComponentEntry.getKey(), "XTSE3050", oldC.getCode());
                    } else {
                        //getRuleManager().registerMode((Mode)oldC.getCode());
                    }
                }
            }


            addCompletionAction(new Action() {
                @Override
                public void doAction() throws XPathException {
                    List<ComponentBinding> oldBindings = newC.getBaseComponent().getComponentBindings();
                    List<ComponentBinding> newBindings = new ArrayList<ComponentBinding>(oldBindings.size());
                    for (ComponentBinding oldBinding : oldBindings) {
                        SymbolicName name = oldBinding.getSymbolicName();
                        Component target;
                        if (overrides.contains(name)) {
                            // if there is an override in this package, we bind to it
                            target = getComponent(name);
                            if (target==null) {
                                throw new AssertionError("We know there's an override for " + name + ", but we can't find it");
                            }
                        } else {
                            // otherwise we bind to the component in this package that corresponds to the component in the used package
                            target = correspondence.get(oldBinding.getTarget());
                            if (target==null) {
                                throw new AssertionError("Saxon can't find the new component corresponding to " + name);
                            }
                        }
                        ComponentBinding newBinding = new ComponentBinding(name, target);
                        newBindings.add(newBinding);
                    }
                    newC.setComponentBindings(newBindings);
                }
            });

        }
        if (usedPackage.isCreatesSecondaryResultDocuments()) {
            setCreatesSecondaryResultDocuments(true);
        }
    }

    /**
     * Create the function library containing stylesheet functions declared in this package
     *
     * @return the resulting function library
     */

    public FunctionLibraryList createFunctionLibrary() {

        FunctionLibraryList functionLibrary = new FunctionLibraryList();
        int functionSet = StandardFunction.CORE | StandardFunction.XSLT;
        if (getVersion() >= 30) {
            functionSet |= StandardFunction.XSLT30 | StandardFunction.XPATH30 | StandardFunction.XPATH31;
        }
        functionLibrary.addFunctionLibrary(
            SystemFunctionLibrary.getSystemFunctionLibrary(functionSet, config));
        functionLibrary.addFunctionLibrary(
            new StylesheetFunctionLibrary(this, true));
        functionLibrary.addFunctionLibrary(
            config.getVendorFunctionLibrary());
        functionLibrary.addFunctionLibrary(
            new ConstructorFunctionLibrary(config));
        if ("JS".equals(getTargetEdition())) {
            addIxslFunctionLibrary(functionLibrary);

        }

        queryFunctions = new XQueryFunctionLibrary(config);
        functionLibrary.addFunctionLibrary(queryFunctions);
        functionLibrary.addFunctionLibrary(config.getIntegratedFunctionLibrary());
        config.addExtensionBinders(functionLibrary);

        return this.functionLibrary = functionLibrary;
    }

    protected void addIxslFunctionLibrary(FunctionLibraryList functionLibrary) {}

    /**
     * Get the function library.
     *
     * @return the function library
     */

    public FunctionLibraryList getFunctionLibrary() {
        return functionLibrary;
    }

    /**
     * Get the library of functions imported from XQuery
     * @return the XQuery function library
     */

    public XQueryFunctionLibrary getXQueryFunctionLibrary() {
        return queryFunctions;
    }


    /**
     * Set details of functions available for calling anywhere in this package. This is the
     * function library used for resolving run-time references to functions, for example
     * from xsl:evaluate, function-available(), or function-lookup().
     * @param overriding      library of stylesheet functions declared with override=yes
     * @param underriding     library of stylesheet functions declared with override=no
     */


    public void setFunctionLibraryDetails(FunctionLibraryList library,
                                          ExecutableFunctionLibrary overriding,
                                          ExecutableFunctionLibrary underriding) {
        if (library != null) {
            this.functionLibrary = library;
        }
        this.overriding = overriding;
        this.underriding = underriding;
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
                UserFunction uf = getFunction(sn);
                if (uf != null) {
                    return uf;
                }
            }
            return null;
        } else {
            Component component = getComponentIndex().get(name);
            return component == null ? null : (UserFunction) component.getCode();
        }
    }

    /**
     * Copy information from this package to the PreparedStylesheet.
     *
     * @param pss the PreparedStylesheet to be updated
     * @throws net.sf.saxon.trans.XPathException if the PreparedStylesheet cannot be updated
     */

    public void updatePreparedStylesheet(PreparedStylesheet pss) throws XPathException {

        for (Map.Entry<SymbolicName, Component> entry : componentIndex.entrySet()) {
            if (entry.getValue().getVisibility() == Visibility.ABSTRACT) {
                abstractComponents.put(entry.getKey(), entry.getValue());
            }
        }

        pss.setTopLevelPackage(this);
        if (isSchemaAware() || !schemaIndex.isEmpty()) {
            pss.setSchemaAware(true);
        }
        pss.setHostLanguage(Configuration.XSLT, xsltVersion >= 30);

        //pss.setStylesheetFunctionLibrary(stylesheetFunctionLibrary);

        FunctionLibraryList libraryList = new FunctionLibraryList();
        for (FunctionLibrary lib : functionLibrary.getLibraryList()) {
            if (lib instanceof StylesheetFunctionLibrary) {
                if (((StylesheetFunctionLibrary) lib).isOverrideExtensionFunction()) {
                    libraryList.addFunctionLibrary(overriding);
                    //pss.getStylesheetFunctions().addFunctionLibrary(overriding);
                } else {
                    libraryList.addFunctionLibrary(underriding);
                    //pss.getStylesheetFunctions().addFunctionLibrary(underriding);
                }
            } else {
                libraryList.addFunctionLibrary(lib);
            }
        }
        pss.setFunctionLibrary(libraryList);

        pss.setKeyManager(keyManager);

        //pss.setAccumulatorManager(accumulatorManager);

        pss.setStripsWhitespace(stripsWhitespace);

        pss.setStripsInputTypeAnnotations(stripsTypeAnnotations);

        pss.setStripperRules(stripperRules == null ? NoElementsSpaceStrippingRule.getInstance() : stripperRules);

        if (!pss.createsSecondaryResult()) {
            pss.setCreatesSecondaryResult(createsSecondaryResultDocuments);
        }
        pss.setDefaultOutputProperties(defaultOutputProperties);
        for (Map.Entry<StructuredQName, Properties> entry : namedOutputProperties.entrySet()) {
            pss.setOutputProperties(entry.getKey(), entry.getValue());
        }

        // Build the index of named character maps

        if (characterMapIndex != null) {
            for (CharacterMap cm : characterMapIndex) {
                pss.getCharacterMapIndex().putCharacterMap(cm.getName(), cm);
            }
        }

        // Finish off the lists of template rules

        ruleManager.checkConsistency();
        ruleManager.computeRankings();
        ruleManager.invertStreamableTemplates();
        ruleManager.optimizeRules();
        pss.setRuleManager(ruleManager);


        // Add named templates to the prepared stylesheet

        for (Component comp : componentIndex.values()) {
            if (comp.getCode() instanceof NamedTemplate) {
                NamedTemplate t = (NamedTemplate) comp.getCode();
                pss.putNamedTemplate(t.getTemplateName(), t);
            }
        }

        // Share the component index with the prepared stylesheet

        pss.setComponentIndex(componentIndex);

        // Register stylesheet parameters

        for (Component comp : componentIndex.values()) {
            if (comp.getCode() instanceof GlobalParam) {
                GlobalParam gv = (GlobalParam) comp.getCode();
                pss.registerGlobalParameter(gv);
            }
        }

        // Set the requirements for the global context item

        if (globalContextRequirement != null) {
            pss.setGlobalContextRequirement(globalContextRequirement);
        }

    }

    public Map<SymbolicName, Component> getAbstractComponents() {
        return abstractComponents;
    }

    /**
     * Output the abstract expression tree to the supplied destination.
     *
     * @param presenter the expression presenter used to display the structure
     */

    public void export(ExpressionPresenter presenter) throws XPathException {
        throw new XPathException("Exporting a stylesheet requires Saxon-EE");
    }

    public void checkForAbstractComponents() throws XPathException {
        for (Map.Entry<SymbolicName, Component> entry : componentIndex.entrySet()) {
            if (entry.getValue().getVisibility() == Visibility.ABSTRACT) {
                abstractComponents.put(entry.getKey(), entry.getValue());
            }
        }
        if (!abstractComponents.isEmpty()) {
            FastStringBuffer buff = new FastStringBuffer(256);
            int count = 0;
            for (SymbolicName name : abstractComponents.keySet()) {
                if (count++ > 0) {
                    buff.append(", ");
                }
                buff.append(name.toString());
                if (buff.length() > 300) {
                    buff.append(" ...");
                    break;
                }
            }
            throw new XPathException(
                "The package is not executable, because it contains abstract components: " +
                    buff.toString(), "XTSE3080");
        }
    }


}

