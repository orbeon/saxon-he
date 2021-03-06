////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
// Copyright (c) 2014 Saxonica Limited.
// This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
// If a copy of the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
// This Source Code Form is "Incompatible With Secondary Licenses", as defined by the Mozilla Public License, v. 2.0.
////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

package net.sf.saxon;

import net.sf.saxon.expr.Component;
import net.sf.saxon.expr.instruct.Executable;
import net.sf.saxon.expr.instruct.GlobalParameterSet;
import net.sf.saxon.expr.instruct.Template;
import net.sf.saxon.expr.instruct.UserFunction;
import net.sf.saxon.functions.ExecutableFunctionLibrary;
import net.sf.saxon.functions.FunctionLibrary;
import net.sf.saxon.functions.FunctionLibraryList;
import net.sf.saxon.lib.OutputURIResolver;
import net.sf.saxon.om.FunctionItem;
import net.sf.saxon.om.StandardNames;
import net.sf.saxon.om.StructuredQName;
import net.sf.saxon.style.Compilation;
import net.sf.saxon.trace.ExpressionPresenter;
import net.sf.saxon.trans.*;
import net.sf.saxon.value.DecimalValue;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.*;

/**
 * This <b>PreparedStylesheet</b> class represents a Stylesheet that has been
 * prepared for execution (or "compiled").
 *
 * <p>Note that the PreparedStylesheet object does not contain a reference to the source stylesheet
 * tree (rooted at an XSLStyleSheet object). This allows the source tree to be garbage-collected
 * when it is no longer required.</p>
 *
 * <p>The PreparedStylesheet in XSLT 3.0 represents the set of all packages making up an executable
 * stylesheet.</p>
 */

public class PreparedStylesheet extends Executable {

    //private CompilerInfo compilerInfo;
    //private Compilation compilation;
    private HashMap<URI, PreparedStylesheet> nextStylesheetCache;
    // cache for stylesheets named as "saxon:next-in-chain"

    // definitions of template rules (XSLT only)
    private RuleManager ruleManager;

    // index of named templates.
    private HashMap<StructuredQName, Template> namedTemplateTable;

    // Table of components declared in this package or imported from used packages. Key is the symbolic identifier of the
    // component; value is the component itself. Hidden components are not included in this table because their names
    // need not be unique, and because they are not available for reference by name.
    // Currently used only for named templates
    private Map<SymbolicName, Component> componentIndex;

    // manager class for accumulator rules (XSLT 3.0 only)
    private IAccumulatorManager accumulatorManager = null;

    private FunctionLibraryList stylesheetFunctions;

    public FunctionLibraryList getStylesheetFunctions() {
        return stylesheetFunctions;
    }

    private StructuredQName defaultInitialTemplate;
    private StructuredQName defaultInitialMode;
    private int recoveryPolicy;
    private String messageReceiverClassName;
    private OutputURIResolver outputURIResolver;
    private GlobalParameterSet compileTimeParams;


    /**
     * Constructor - deliberately protected
     *
     * @param compilation Compilation options
     */

    public PreparedStylesheet(Compilation compilation) {
        super(compilation.getConfiguration());
        //this.compilation = compilation;
        setAccumulatorManager(compilation.getStyleNodeFactory(true).makeAccumulatorManager());
        CompilerInfo compilerInfo = compilation.getCompilerInfo();
        RuleManager rm = new RuleManager();
        rm.setRecoveryPolicy(compilerInfo.getRecoveryPolicy());
        setRuleManager(rm);
        setHostLanguage(Configuration.XSLT, compilation.getVersion().equals(DecimalValue.THREE));
        //setCollationMap(compilerInfo.getCollationMap());
        setSchemaAware(compilerInfo.isSchemaAware());
        if (compilerInfo.getErrorListener() == null) {
            compilerInfo.setErrorListener(getConfiguration().getErrorListener());
        }
        defaultInitialMode = compilerInfo.getDefaultInitialMode();
        defaultInitialTemplate = compilerInfo.getDefaultInitialTemplate();
        recoveryPolicy = compilerInfo.getRecoveryPolicy();
        messageReceiverClassName = compilerInfo.getMessageReceiverClassName();
        outputURIResolver = compilerInfo.getOutputURIResolver();
        compileTimeParams = compilerInfo.getParameters();
        setLocationMap(compilation.getLocationMap());
    }


    /**
     * Make a Controller from this stylesheet object.
     *
     * @return the new Controller
     * @see net.sf.saxon.Controller
     */

    public Controller newController() {
        Configuration config = getConfiguration();
        Controller c = new Controller(config, this);
        c.setMessageReceiverClassName(messageReceiverClassName);
        c.setOutputURIResolver(outputURIResolver);
        c.setRecoveryPolicy(recoveryPolicy);
        if (defaultInitialTemplate != null) {
            try {
                c.setInitialTemplate(defaultInitialTemplate);
            } catch (XPathException err) {
                // ignore error if there is no template with this name
            }

        }
        if (defaultInitialMode != null) {
            try {
                c.setInitialMode(defaultInitialMode);
            } catch (XPathException e) {
                // ignore the error if the default initial mode is not defined
            }
        }
        return c;
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

    /**
     * Get the parameters that were set at compile time. These will generally be static parameters,
     * but it is also permitted to set non-static parameters at compile time.
     *
     * @return the parameters that were set prior to XSLT compilation
     */

    public GlobalParameterSet getCompileTimeParams() {
        return compileTimeParams;
    }

    /**
     * Set the RuleManager that handles template rules
     *
     * @param rm the RuleManager containing details of all the template rules
     */

    public void setRuleManager(RuleManager rm) {
        ruleManager = rm;
    }

    /**
     * Get the RuleManager which handles template rules
     *
     * @return the RuleManager registered with setRuleManager
     */

    public RuleManager getRuleManager() {
        return ruleManager;
    }

    /**
     * Get the named template with a given name.
     *
     * @param qName The template name
     * @return The template (of highest import precedence) with this name if there is one;
     * null if none is found.
     */

    /*@Nullable*/
    public Template getNamedTemplate(StructuredQName qName) {
        if (namedTemplateTable == null) {
            return null;
        }
        return namedTemplateTable.get(qName);
    }

    /**
     * Register the named template with a given name
     *
     * @param templateName the name of a named XSLT template
     * @param template     the template
     */

    public void putNamedTemplate(StructuredQName templateName, Template template) {
        if (namedTemplateTable == null) {
            namedTemplateTable = new HashMap<StructuredQName, Template>(32);
        }
        namedTemplateTable.put(templateName, template);
    }


    /**
     * Get the function with a given name.
     *
     * @param qName The function name
     * @return The function with this name if there is one;
     * null if none is found.
     */

    /*@Nullable*/
    public FunctionItem getFunction(StructuredQName qName, int arity) {
        SymbolicName s = new SymbolicName(StandardNames.XSL_FUNCTION, qName, arity);
//#ifdefined HOF
        if (stylesheetFunctions.isAvailable(s)) {
            for (FunctionLibrary lib : stylesheetFunctions.getLibraryList()) {
                try {
                    FunctionItem fi = lib.getFunctionItem(s, null, null);
                    return fi;
                } catch (XPathException e) {
                    return null;
                }
            }
        }
//#endif
        return null;
    }


    /**
     * Register the index of components
     *
     * @param index the component index
     */

    public void setComponentIndex(Map<SymbolicName, Component> index) {
        componentIndex = index;
    }

    public Component getComponent(SymbolicName name) {
        return componentIndex.get(name);
    }

    /**
     * Iterate over all the named templates defined in this Executable
     *
     * @return an iterator, the items returned being of class {@link net.sf.saxon.expr.instruct.Template}
     */

    public Iterator<Template> iterateNamedTemplates() {
        if (namedTemplateTable == null) {
            List<Template> list = Collections.emptyList();
            return list.iterator();
        } else {
            return namedTemplateTable.values().iterator();
        }
    }

    /**
     * Explain the expression tree for named templates in a stylesheet
     *
     * @param presenter destination for the explanatory output
     */

    public void explainNamedTemplates(ExpressionPresenter presenter) {
        presenter.startElement("namedTemplates");
        if (namedTemplateTable != null) {
            for (Template t : namedTemplateTable.values()) {
                presenter.startElement("template");
                presenter.emitAttribute("name", t.getTemplateName().getDisplayName());
                presenter.emitAttribute("line", t.getLineNumber() + "");
                presenter.emitAttribute("module", t.getSystemId());
                if (t.getBody() != null) {
                    t.getBody().explain(presenter);
                }
                presenter.endElement();
            }
        }
        presenter.endElement();
    }

    /**
     * Get the properties for xsl:output.  JAXP method. The object returned will
     * be a clone of the internal values, and thus it can be mutated
     * without mutating the Templates object, and then handed in to
     * the process method.
     * <p>In Saxon, the properties object is a new, empty, Properties object that is
     * backed by the live properties to supply default values for missing properties.
     * This means that the property values must be read using the getProperty() method.
     * Calling the get() method on the underlying Hashtable will return null.</p>
     * <p>In Saxon 8.x, this method gets the output properties for the unnamed output
     * format in the stylesheet.</p>
     *
     * @return A Properties object reflecting the output properties defined
     * for the default (unnamed) output format in the stylesheet. It may
     * be mutated and supplied to the setOutputProperties() method of the
     * Transformer, without affecting other transformations that use the
     * same stylesheet.
     * @see javax.xml.transform.Transformer#setOutputProperties
     */


    public Properties getOutputProperties() {
        Properties details = getDefaultOutputProperties();
        return new Properties(details);
    }

    /**
     * Get a "next in chain" stylesheet. This method is intended for internal use.
     *
     * @param href    the relative URI of the next-in-chain stylesheet
     * @param baseURI the baseURI against which this relativeURI is to be resolved
     * @return the cached stylesheet if present in the cache, or null if not
     */

    /*@Nullable*/
    public PreparedStylesheet getCachedStylesheet(String href, String baseURI) {
        URI abs = null;
        try {
            abs = new URI(baseURI).resolve(href);
        } catch (URISyntaxException err) {
            //
        }
        PreparedStylesheet result = null;
        if (abs != null && nextStylesheetCache != null) {
            result = nextStylesheetCache.get(abs);
        }
        return result;
    }

    /**
     * Save a "next in chain" stylesheet in compiled form, so that it can be reused repeatedly.
     * This method is intended for internal use.
     *
     * @param href    the relative URI of the stylesheet
     * @param baseURI the base URI against which the relative URI is resolved
     * @param pss     the prepared stylesheet object to be cached
     */

    public void putCachedStylesheet(String href, String baseURI, PreparedStylesheet pss) {
        URI abs = null;
        try {
            abs = new URI(baseURI).resolve(href);
        } catch (URISyntaxException err) {
            //
        }
        if (abs != null) {
            if (nextStylesheetCache == null) {
                nextStylesheetCache = new HashMap<URI, PreparedStylesheet>(4);
            }
            nextStylesheetCache.put(abs, pss);
        }
    }

    /**
     * Produce an XML representation of the compiled and optimized stylesheet
     *
     * @param presenter defines the destination and format of the output
     */

    public void explain(ExpressionPresenter presenter) {
        presenter.startElement("stylesheet");
        getKeyManager().explainKeys(presenter);
        explainGlobalVariables(presenter);
        ruleManager.explainTemplateRules(presenter);
        explainNamedTemplates(presenter);
        FunctionLibraryList libList = getFunctionLibrary();
        List<FunctionLibrary> libraryList = libList.getLibraryList();
        presenter.startElement("functions");
        for (FunctionLibrary lib : libraryList) {
            if (lib instanceof ExecutableFunctionLibrary) {
                for (Iterator f = ((ExecutableFunctionLibrary) lib).iterateFunctions(); f.hasNext(); ) {
                    UserFunction func = (UserFunction) f.next();
                    presenter.startElement("function");
                    presenter.emitAttribute("name", func.getFunctionName().getDisplayName());
                    presenter.emitAttribute("line", func.getLineNumber() + "");
                    presenter.emitAttribute("module", func.getSystemId());
                    func.getBody().explain(presenter);
                    presenter.endElement();
                }
            }
        }
        presenter.endElement();
        presenter.endElement();
        presenter.close();
    }

    public void setStylesheetFunctionLibrary(FunctionLibraryList stylesheetFunctions) {
        this.stylesheetFunctions = stylesheetFunctions;
    }
}

