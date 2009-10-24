package net.sf.saxon.trans;

import net.sf.saxon.Configuration;
import net.sf.saxon.OutputURIResolver;
import net.sf.saxon.event.StandardOutputResolver;

import javax.xml.transform.ErrorListener;
import javax.xml.transform.URIResolver;
import java.io.Serializable;

/**
 * This class exists to hold information associated with a specific XSLT compilation episode.
 * In JAXP, the URIResolver and ErrorListener used during XSLT compilation are those defined in the
 * TransformerFactory. The .NET API and the s9api API, however, allow finer granularity,
 * and this class exists to support that.
 */

public class CompilerInfo implements Serializable {

    private transient URIResolver uriResolver;
    private transient OutputURIResolver outputURIResolver = StandardOutputResolver.getInstance();
    private transient ErrorListener errorListener;
    private boolean compileWithTracing;
    private int recoveryPolicy = Configuration.RECOVER_WITH_WARNINGS;
    private boolean schemaAware;
    private boolean versionWarning;
    private String messageReceiverClassName = "net.sf.saxon.event.MessageEmitter";

    /**
     * Create an empty CompilerInfo object with default settings
     */

    public CompilerInfo() {}

    /**
     * Create a CompilerInfo object as a copy of another CompilerInfo object
     * @param info the existing CompilerInfo object
     * @since 9.2
     */

    public CompilerInfo(CompilerInfo info) {
        uriResolver = info.uriResolver;
        outputURIResolver = info.outputURIResolver;
        errorListener = info.errorListener;
        compileWithTracing = info.compileWithTracing;
        recoveryPolicy = info.recoveryPolicy;
        schemaAware = info.schemaAware;
        versionWarning = info.versionWarning;
        messageReceiverClassName = info.messageReceiverClassName;
    }

    /**
     * Set the URI Resolver to be used in this compilation episode.
     * @param resolver The URIResolver to be used. This is used to dereference URIs encountered in constructs
     * such as xsl:include, xsl:import, and xsl:import-schema.
     * @since 8.7
     */

    public void setURIResolver(URIResolver resolver) {
        uriResolver = resolver;
    }

    /**
     * Get the URI Resolver being used in this compilation episode.
     * @return resolver The URIResolver in use. This is used to dereference URIs encountered in constructs
     * such as xsl:include, xsl:import, and xsl:import-schema.
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
     * @param listener The error listener to be used. This is notified of all errors detected during the
     * compilation.
     * @since 8.7
     */

    public void setErrorListener(ErrorListener listener) {
        errorListener = listener;
    }

    /**
     * Get the ErrorListener being used during this compilation episode
     * @return listener The error listener in use. This is notified of all errors detected during the
     * compilation.
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
     *                            must implement net.sf.saxon.event.Emitter.
     * @since 9.2
     */

    public void setMessageReceiverClassName(String messageReceiverClassName) {
        this.messageReceiverClassName = messageReceiverClassName;
    }

    /**
     * Set whether trace hooks are to be included in the compiled code. To use tracing, it is necessary
     * both to compile the code with trace hooks included, and to supply a TraceListener at run-time
     * @param trueOrFalse true if trace code is to be compiled in, false otherwise
     * @since 8.9
     */

    public void setCompileWithTracing(boolean trueOrFalse) {
        compileWithTracing = trueOrFalse;
    }

    /**
     * Determine whether trace hooks are included in the compiled code.
     * @return true if trace hooks are included, false if not.
     * @since 8.9
     */

    public boolean isCompileWithTracing() {
        return compileWithTracing;
    }

    /**
     * Set the policy for handling recoverable errrors. Note that for some errors the decision can be
     * made at run-time, but for the "ambiguous template match" error, the decision is (since 9.2)
     * fixed at compile time.
     * @param policy the recovery policy to be used. The options are {@link Configuration#RECOVER_SILENTLY},
     * {@link Configuration#RECOVER_WITH_WARNINGS}, or {@link Configuration#DO_NOT_RECOVER}.
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
     * Determine whether a warning is to be output when running against a stylesheet labelled
     * as version="1.0". The XSLT specification requires such a warning unless the user disables it.
     *
     * @return true if these messages are to be output.
     * @since 9.2
     */

    public boolean isVersionWarning() {
        return versionWarning;
    }

    /**
     * Determine whether a warning is to be output when running against a stylesheet labelled
     * as version="1.0". The XSLT specification requires such a warning unless the user disables it.
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
     * @param schemaAware If true, the stylesheet will be compiled with schema-awareness
     * enabled even if it contains no xsl:import-schema declarations. If false, the stylesheet
     * is treated as schema-aware only if it contains one or more xsl:import-schema declarations
     * @since 9.2
     */

    public void setSchemaAware(boolean schemaAware) {
        this.schemaAware = schemaAware;
    }

    /**
     * Ask whether schema-awareness has been requested by means of a call on
     * {@link #setSchemaAware}
     * @return true if schema-awareness has been requested
     */

    public boolean isSchemaAware() {
        return schemaAware;
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
