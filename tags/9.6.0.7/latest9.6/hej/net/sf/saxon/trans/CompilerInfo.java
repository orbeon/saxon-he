////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
// Copyright (c) 2014 Saxonica Limited.
// This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
// If a copy of the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
// This Source Code Form is "Incompatible With Secondary Licenses", as defined by the Mozilla Public License, v. 2.0.
////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

package net.sf.saxon.trans;

import net.sf.saxon.Configuration;
import net.sf.saxon.expr.instruct.GlobalParameterSet;
import net.sf.saxon.expr.parser.CodeInjector;
import net.sf.saxon.functions.FunctionLibrary;
import net.sf.saxon.lib.ErrorGatherer;
import net.sf.saxon.lib.OutputURIResolver;
import net.sf.saxon.lib.StandardOutputResolver;
import net.sf.saxon.om.Sequence;
import net.sf.saxon.om.StructuredQName;
import net.sf.saxon.value.DecimalValue;

import javax.xml.transform.ErrorListener;
import javax.xml.transform.URIResolver;
import java.util.HashMap;
import java.util.Map;

/**
 * This class exists to hold information associated with a specific XSLT compilation episode.
 * In JAXP, the URIResolver and ErrorListener used during XSLT compilation are those defined in the
 * TransformerFactory. The .NET API and the s9api API, however, allow finer granularity,
 * and this class exists to support that.
 */

public class CompilerInfo {

    private transient URIResolver uriResolver;
    private transient OutputURIResolver outputURIResolver = StandardOutputResolver.getInstance();
    private transient ErrorListener errorListener;
    private CodeInjector codeInjector;
    private int recoveryPolicy = Configuration.RECOVER_WITH_WARNINGS;
    private boolean schemaAware;
    private boolean versionWarning;
    private String messageReceiverClassName = "net.sf.saxon.serialize.MessageEmitter";
    private StructuredQName defaultInitialMode;
    private StructuredQName defaultInitialTemplate;
    private DecimalValue xsltVersion = DecimalValue.ZERO;   // indicates selection based on xsl:stylesheet/@version
    private FunctionLibrary extensionFunctionLibrary;
    private GlobalParameterSet variableList = new GlobalParameterSet();
    private String defaultCollation;
    private Map<String, Object> options = new HashMap<String, Object>();      //TODO fix bytecode memory issue. See bug issue #1833
    private PackageLibrary packageLibrary = new PackageLibrary();
    private boolean generateByteCode = false;
    private ErrorGatherer errorGatherer;


    /**
     * Create an empty CompilerInfo object with default settings
     */

    public CompilerInfo() {
    }

    /**
     * Create a CompilerInfo object as a copy of another CompilerInfo object
     *
     * @param info the existing CompilerInfo object
     * @since 9.2
     */

    public CompilerInfo(CompilerInfo info) {
        copyFrom(info);
    }

    /**
     * Copy all properties from a supplied CompilerInfo
     *
     * @param info the CompilerInfo to be copied
     */

    public void copyFrom(CompilerInfo info) {
        uriResolver = info.uriResolver;
        outputURIResolver = info.outputURIResolver;
        errorListener = info.errorListener;
        codeInjector = info.codeInjector;
        recoveryPolicy = info.recoveryPolicy;
        schemaAware = info.schemaAware;
        versionWarning = info.versionWarning;
        messageReceiverClassName = info.messageReceiverClassName;
        defaultInitialMode = info.defaultInitialMode;
        defaultInitialTemplate = info.defaultInitialTemplate;
        xsltVersion = info.xsltVersion;
        packageLibrary = info.packageLibrary;
        generateByteCode = info.generateByteCode;
        extensionFunctionLibrary = info.extensionFunctionLibrary;
        defaultCollation = info.defaultCollation;
    }

    /**
     * Set the URI Resolver to be used in this compilation episode.
     *
     * @param resolver The URIResolver to be used. This is used to dereference URIs encountered in constructs
     *                 such as xsl:include, xsl:import, and xsl:import-schema.
     * @since 8.7
     */

    public void setURIResolver(URIResolver resolver) {
        uriResolver = resolver;
    }

    /**
     * Set the value of a stylesheet parameter. Static (compile-time) parameters must be provided using
     * this method on the XsltCompiler object, prior to stylesheet compilation. Non-static parameters
     * may also be provided using this method if their values will not vary from one transformation
     * to another. No error occurs at this stage if the parameter is unknown, or if the value is incorrect
     * for the parameter. Setting a value for a parameter overwrites any previous value set for the
     * same parameter.
     *
     * @param name the name of the stylesheet parameter
     * @param seq  the value of the stylesheet parameter
     */

    public void setParameter(StructuredQName name, Sequence seq) {
        variableList.put(name, seq);
    }

    /**
     * Get the values of all stylesheet parameters that have been set using the
     * {@link #setParameter(net.sf.saxon.om.StructuredQName, net.sf.saxon.om.Sequence)}
     * method.
     * @return the set of all parameters that have been set.
     */

    public GlobalParameterSet getParameters() {
        return variableList;
    }


    /**
     * Set the package library to be used during the compilation episode. Any packages referenced
     * using xsl:use-package declarations during the stylesheet compilation must be found in this
     * package library
     * @param library the package library
     */

    public void setPackageLibrary(PackageLibrary library) {
        packageLibrary = library;
    }

    /**
     * Get the package library to be used during the compilation episode. Any packages referenced
     * using xsl:use-package declarations during the stylesheet compilation must be found in this
     * package library
     * @return the package library
     */

    public PackageLibrary getPackageLibrary() {
        return packageLibrary;
    }



    /**
     * Set whether bytecode should be generated for the compiled stylesheet. This option
     * is available only with Saxon-EE. The default depends on the setting in the configuration
     * at the time the XsltCompiler is instantiated, and by default is true for Saxon-EE.
     *
     * @param option true if bytecode is to be generated, false otherwise
     * @since 9.6
     */

    public void setGenerateByteCode(boolean option) {
        generateByteCode = option;
    }

    /**
     * Ask whether bytecode is to be generated in the compiled code.
     *
     * @return true if bytecode is to be generated, false if not.
     * @since 9.6
     */

    public boolean isGenerateByteCode() {
        return generateByteCode;
    }

    /**
     * Get the URI Resolver being used in this compilation episode.
     *
     * @return resolver The URIResolver in use. This is used to dereference URIs encountered in constructs
     *         such as xsl:include, xsl:import, and xsl:import-schema.
     * @since 8.7
     */

    public URIResolver getURIResolver() {
        return uriResolver;
    }

    /**
     * Get the OutputURIResolver that will be used to resolve URIs used in the
     * href attribute of the xsl:result-document instruction.
     *
     * @return the OutputURIResolver. If none has been supplied explicitly, the
     *         default OutputURIResolver is returned.
     * @since 9.2
     */

    public OutputURIResolver getOutputURIResolver() {
        return outputURIResolver;
    }

    /**
     * Set the OutputURIResolver that will be used to resolve URIs used in the
     * href attribute of the xsl:result-document instruction.
     *
     * @param outputURIResolver the OutputURIResolver to be used.
     * @since 9.2
     */

    public void setOutputURIResolver(OutputURIResolver outputURIResolver) {
        this.outputURIResolver = outputURIResolver;
    }


    /**
     * Set the ErrorListener to be used during this compilation episode
     *
     * @param listener The error listener to be used. This is notified of all errors detected during the
     *                 compilation.
     * @since 8.7
     */

    public void setErrorListener(ErrorListener listener) {
        errorListener = listener;
    }

    /**
     * Get the ErrorListener being used during this compilation episode
     *
     * @return listener The error listener in use. This is notified of all errors detected during the
     *         compilation.
     * @since 8.7
     */

    public ErrorListener getErrorListener() {
        return errorListener;
    }

    /**
     * Get the name of the class that will be instantiated to create a MessageEmitter,
     * to process the output of xsl:message instructions in XSLT.
     *
     * @return the full class name of the message emitter class.
     * @since 9.2
     */

    public String getMessageReceiverClassName() {
        return messageReceiverClassName;
    }

    /**
     * Set the name of the class that will be instantiated to create a MessageEmitter,
     * to process the output of xsl:message instructions in XSLT.
     *
     * @param messageReceiverClassName the message emitter class. This
     *                                 must implement net.sf.saxon.event.Emitter.
     * @since 9.2
     */

    public void setMessageReceiverClassName(String messageReceiverClassName) {
        this.messageReceiverClassName = messageReceiverClassName;
    }

    /**
     * Set the name of the default collation
     *
     * @param collation the name of the default collation (if the stylesheet doesn't specify otherwise)
     * @since 9.6
     */

    public void setDefaultCollation(String collation) {
        this.defaultCollation = collation;
    }

    /**
     * Get the name of the default collation
     *
     * @return the default collation. If none has been specified at the level of this CompilerInfo,
     *         this defaults to the default collation defined in the Configuration, which in turn defaults
     *         to the Unicode codepoint collation
     * @since 9.6
     */

    public String getDefaultCollation() {
        return this.defaultCollation;
    }

    /**
     * Set whether trace hooks are to be included in the compiled code. To use tracing, it is necessary
     * both to compile the code with trace hooks included, and to supply a TraceListener at run-time
     *
     * @param injector the code injector used to insert trace or debugging hooks, or null to clear any
     *                 existing entry
     * @since 9.4
     */

    public void setCodeInjector(CodeInjector injector) {
        codeInjector = injector;
    }

    /**
     * Get the registered CodeInjector, if any
     *
     * @return the code injector used to insert trace or debugging hooks, or null if absent
     */

    public CodeInjector getCodeInjector() {
        return codeInjector;
    }

    /**
     * Determine whether trace hooks are included in the compiled code.
     *
     * @return true if trace hooks are included, false if not.
     * @since 8.9
     */

    public boolean isCompileWithTracing() {
        return codeInjector != null;
    }

    /**
     * Set the policy for handling recoverable errrors. Note that for some errors the decision can be
     * made at run-time, but for the "ambiguous template match" error, the decision is (since 9.2)
     * fixed at compile time.
     *
     * @param policy the recovery policy to be used. The options are {@link Configuration#RECOVER_SILENTLY},
     *               {@link Configuration#RECOVER_WITH_WARNINGS}, or {@link Configuration#DO_NOT_RECOVER}.
     * @since 9.2
     */

    public void setRecoveryPolicy(int policy) {
        recoveryPolicy = policy;
    }

    /**
     * Get the policy for handling recoverable errors. Note that for some errors the decision can be
     * made at run-time, but for the "ambiguous template match" error, the decision is (since 9.2)
     * fixed at compile time.
     *
     * @return the current policy.
     * @since 9.2
     */

    public int getRecoveryPolicy() {
        return recoveryPolicy;
    }

    /**
     * Ask whether a warning is to be output when the stylesheet version does not match the processor version.
     * In the case of stylesheet version="1.0", the XSLT specification requires such a warning unless the user disables it.
     *
     * @return true if these messages are to be output.
     * @since 9.2
     */

    public boolean isVersionWarning() {
        return versionWarning;
    }

    /**
     * Say whether a warning is to be output when the stylesheet version does not match the processor version.
     * In the case of stylesheet version="1.0", the XSLT specification requires such a warning unless the user disables it.
     *
     * @param warn true if these messages are to be output.
     * @since 9.2
     */

    public void setVersionWarning(boolean warn) {
        versionWarning = warn;
    }

    /**
     * Say that the stylesheet must be compiled to be schema-aware, even if it contains no
     * xsl:import-schema declarations. Normally a stylesheet is treated as schema-aware
     * only if it contains one or more xsl:import-schema declarations. If it is not schema-aware,
     * then all input documents must be untyped, and validation of temporary trees is disallowed
     * (though validation of the final result tree is permitted). Setting the argument to true
     * means that schema-aware code will be compiled regardless.
     *
     * @param schemaAware If true, the stylesheet will be compiled with schema-awareness
     *                    enabled even if it contains no xsl:import-schema declarations. If false, the stylesheet
     *                    is treated as schema-aware only if it contains one or more xsl:import-schema declarations
     * @since 9.2
     */

    public void setSchemaAware(boolean schemaAware) {
        this.schemaAware = schemaAware;
    }

    /**
     * Ask whether schema-awareness has been requested by means of a call on
     * {@link #setSchemaAware}
     *
     * @return true if schema-awareness has been requested
     */

    public boolean isSchemaAware() {
        return schemaAware;
    }

    /**
     * Set the default initial template name for a stylesheet compiled using this CompilerInfo.
     * This is only a default; it can be overridden when the stylesheet is executed
     *
     * @param initialTemplate the name of the default initial template, or null if there is
     *                        no default. No error occurs (until run-time) if the stylesheet does not contain a template
     *                        with this name.
     * @since 9.3
     */

    public void setDefaultInitialTemplate(StructuredQName initialTemplate) {
        defaultInitialTemplate = initialTemplate;
    }

    /**
     * Get the default initial template name for a stylesheet compiled using this CompilerInfo.
     * This is only a default; it can be overridden when the stylesheet is executed
     *
     * @return the name of the default initial template, or null if there is
     *         no default, as set using {@link #setDefaultInitialTemplate}
     * @since 9.3
     */

    public StructuredQName getDefaultInitialTemplate() {
        return defaultInitialTemplate;
    }

    /**
     * Set the default initial mode name for a stylesheet compiled using this CompilerInfo.
     * This is only a default; it can be overridden when the stylesheet is executed
     *
     * @param initialMode the name of the default initial mode, or null if there is
     *                    no default. No error occurs (until run-time) if the stylesheet does not contain a mode
     *                    with this name.
     * @since 9.3
     */

    public void setDefaultInitialMode(StructuredQName initialMode) {
        defaultInitialMode = initialMode;
    }

    /**
     * Get the default initial mode name for a stylesheet compiled using this CompilerInfo.
     * This is only a default; it can be overridden when the stylesheet is executed
     *
     * @return the name of the default initial mode, or null if there is
     *         no default, as set using {@link #setDefaultInitialMode}
     * @since 9.3
     */

    public StructuredQName getDefaultInitialMode() {
        return defaultInitialMode;
    }

    /**
     * Set the version of XSLT to be supported by this processor. This determines the version
     * of the XSLT specification to which the processor conforms. This does not have to match
     * the version attribute in the stylesheet; for example if the processor version is 2.0
     * and the stylesheet is version 3.0, then the stylesheet will be processed using the rules
     * for a 2.0 processor in forwards compatibility mode, rather than the rules for a 3.01
     * processor.
     * <p>The value 0.0 (which is the default) indicates that the processor version is to be
     * taken from the version attribute of the xsl:stylesheet element.</p>
     * <p>If the value 1.0 is supplied, this is treated as 2.0. Typically this happens when
     * a stylesheet specifies version="1.0" and selection of the XSLT version is based on what
     * the stylesheet says. This will use an XSLT 2.0 processor running in backwards compatibilty
     * mode.</p>
     * <p><i>XSLT 3.0 features are supported only in Saxon-EE. Setting the version to 3.0
     * here will not fail if the wrong edition is in use, but use of XSLT 3.0 features will
     * fail subsequently.</i></p>
     *
     * @param version must be numerically equal to 0.0, 2.0 or 3.0
     * @throws IllegalArgumentException if the version is invalid
     * @since 9.3
     */

    public void setXsltVersion(/*@NotNull*/ DecimalValue version) {
        if (version.equals(DecimalValue.ONE)) {
            xsltVersion = DecimalValue.TWO;
        } else if (!version.equals(DecimalValue.TWO) && !version.equals(DecimalValue.THREE)
                && !version.equals(DecimalValue.ZERO)) {
            throw new IllegalArgumentException("XSLT version must be 0.0, 2.0 or 3.0");
        } else {
            xsltVersion = version;
        }
    }

    /**
     * Get the version of XSLT supported by this processor
     *
     * @return {@link net.sf.saxon.value.DecimalValue#TWO} or {@link net.sf.saxon.value.DecimalValue#THREE},
     *         or zero indicating that the processor version is taken from the version attribute of the xsl:stylesheet element.)
     * @since 9.3
     */

    public DecimalValue getXsltVersion() {
        return xsltVersion;
    }

    /**
     * Set a library of extension functions. The functions in this library will be available
     * in all modules of the stylesheet. The function library will be searched after language-defined
     * libraries (such as built-in functions, user-defined XQuery functions, and constructor
     * functions) but before extension functions defined at the Configuration level.
     *
     * @param library the function library to be added (replacing any that has previously been set).
     *                May be null to clear a previously-set library
     * @since 9.4
     */

    public void setExtensionFunctionLibrary(/*@Nullable*/ FunctionLibrary library) {
        this.extensionFunctionLibrary = library;
    }

    /**
     * Get any function library that was previously set using
     * {@link #setExtensionFunctionLibrary(net.sf.saxon.functions.FunctionLibrary)}.
     *
     * @return the extension function library, or null if none has been set.
     * @since 9.4
     */


    /*@Nullable*/
    public FunctionLibrary getExtensionFunctionLibrary() {
        return extensionFunctionLibrary;
    }

}

