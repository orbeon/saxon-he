package net.sf.saxon.event;

import net.sf.saxon.Configuration;
import net.sf.saxon.Controller;
import net.sf.saxon.expr.ExpressionLocation;
import net.sf.saxon.type.SchemaURIResolver;

import javax.xml.transform.ErrorListener;
import javax.xml.transform.SourceLocator;
import javax.xml.transform.URIResolver;

/**
 * A PipelineConfiguration sets options that apply to all the operations in a pipeline.
 * Unlike the global Configuration, these options are always local to a process.
 */

public class PipelineConfiguration {

    private Configuration config;
    private LocationProvider locationProvider;
    private URIResolver uriResolver;
    private SchemaURIResolver schemaURIResolver;
    private Controller controller;
    private ParseOptions parseOptions;
    private boolean isSerializing;
    private boolean recoverFromValidationErrors = false;
    private boolean shallowValidation = false;
    private int hostLanguage = Configuration.XSLT;
    private CopyInformee copyInformee = null;

    /**
     * Create a PipelineConfiguration. Note: the normal way to create
     * a PipelineConfiguration is via the factory methods in the Controller and
     * Configuration classes
     * @see Configuration#makePipelineConfiguration
     * @see Controller#makePipelineConfiguration
     */

    public PipelineConfiguration() {
        parseOptions = new ParseOptions();
    }

    /**
     * Create a PipelineConfiguration as a copy of an existing
     * PipelineConfiguration
     * @param p the existing PipelineConfiguration
     */

    public PipelineConfiguration(PipelineConfiguration p) {
        config = p.config;
        locationProvider = p.locationProvider;
        uriResolver = p.uriResolver;
        schemaURIResolver = p.schemaURIResolver;
        controller = p.controller;
        isSerializing = p.isSerializing;
        parseOptions = new ParseOptions(p.parseOptions);
        hostLanguage = p.hostLanguage;
        recoverFromValidationErrors = p.recoverFromValidationErrors;
        copyInformee = p.copyInformee;
        shallowValidation = p.shallowValidation;
    }

    /**
     * Get the Saxon Configuration object
     * @return the Saxon Configuration
     */

    public Configuration getConfiguration() {
        return config;
    }

    /**
     * Set the Saxon Configuration object
     * @param config the Saxon Configuration
     */

    public void setConfiguration(Configuration config) {
        this.config = config;
    }

    /**
     * Get the LocationProvider for interpreting location ids passed down this pipeline
     * @return the appropriate LocationProvider
     */

    public LocationProvider getLocationProvider() {
        return locationProvider;
    }

    /**
     * Set the LocationProvider for interpreting location ids passed down this pipeline
     * @param locationProvider the LocationProvider
     */

    public void setLocationProvider(LocationProvider locationProvider) {
        this.locationProvider = locationProvider;
    }

    /**
     * Get a SourceLocator for a given locationId, using this location provider
     */

    public SourceLocator getSourceLocation(long locationId) {
        return new ExpressionLocation(locationProvider, locationId);
    }

    /**
     * Get the ErrorListener used for reporting errors in processing this pipeline
     * @return the ErrorListener
     */

    public ErrorListener getErrorListener() {
        return parseOptions.getErrorListener();
    }

    /**
     * Set the ErrorListener used for reporting errors in processing this pipeline
     * @param errorListener the ErrorListener
     */

    public void setErrorListener(ErrorListener errorListener) {
        parseOptions.setErrorListener(errorListener);
    }

    /**
     * Get the URIResolver used for processing URIs encountered on this pipeline
     * @return the URIResolver
     */

    public URIResolver getURIResolver() {
        return uriResolver;
    }

    /**
     * Set the URIResolver used for processing URIs encountered on this pipeline
     * @param uriResolver the URIResolver
     */

    public void setURIResolver(URIResolver uriResolver) {
        this.uriResolver = uriResolver;
    }

    /**
     * Get the user-defined SchemaURIResolver for resolving URIs used in "import schema"
     * declarations; returns null if none has been explicitly set.
     * @return the SchemaURIResolver
     */

    public SchemaURIResolver getSchemaURIResolver() {
        return schemaURIResolver;
    }

    /**
     * Set the document parsing and building options to be used on this pipeline
     * @param options the options to be used
     */

    public void setParseOptions(ParseOptions options) {
        parseOptions = options;
    }

    /**
     * Get the document parsing and building options to be used on this pipeline
     * return the options to be used
     */

    public ParseOptions getParseOptions() {
        return parseOptions;
    }

    /**
     * Say whether xsi:schemaLocation and xsi:noNamespaceSchemaLocation attributes
     * should be recognized while validating an instance document
     * @param recognize true if these attributes should be recognized
     */

    public void setUseXsiSchemaLocation(boolean recognize) {
        parseOptions.setUseXsiSchemaLocation(recognize);
    }

    /**
     * Say whether validation errors encountered on this pipeline should be treated as fatal
     * or as recoverable.
     * @param recover set to true if validation errors are to be treated as recoverable. If this option is set to true,
     * such errors will be reported to the ErrorListener using the error() method, and validation will continue.
     * If it is set to false (the default), errors will be reported using the fatalError() method, and validation will
     * be abandoned.
     */

    public void setRecoverFromValidationErrors(boolean recover) {
        recoverFromValidationErrors = recover;
    }

    /**
     * Ask if this pipeline recovers from validation errors
     * @return true if validation errors on this pipeline are treated as recoverable; false if they are treated
     * as fatal
     */

    public boolean isRecoverFromValidationErrors() {
        return recoverFromValidationErrors;
    }

    /**
     * Say whether validation should be shallow. With shallow validation, the top-level sequence
     * of elements is checked against the content model, but the children of these top-level elements
     * are not provided to the validator and therefore cannot be checked. This mode is used during
     * compile-time checking of a query or stylesheet.
     * @param shallow set to true to request shallow validation
     */

    public void setShallowValidation(boolean shallow) {
        shallowValidation = shallow;
    }

    /**
     * Ask whether shallow validation has been requested
     * @return true if shallow validation has been requested
     */

    public boolean isShallowValidation() {
        return shallowValidation;
    }

    /**
     * Set a user-defined SchemaURIResolver for resolving URIs used in "import schema"
     * declarations.
     * @param resolver the SchemaURIResolver
     */

    public void setSchemaURIResolver(SchemaURIResolver resolver) {
        schemaURIResolver = resolver;
    }

    /**
     * Get the controller associated with this pipelineConfiguration
     * @return the controller if it is known; otherwise null.
     */

    public Controller getController() {
        return controller;
    }

    /**
     * Set the Controller associated with this pipelineConfiguration
     * @param controller the Controller
     */

    public void setController(Controller controller) {
        this.controller = controller;
    }

    /**
     * Get the host language in use
     * @return for example {@link Configuration#XSLT} or {@link Configuration#XQUERY}
     */

    public int getHostLanguage() {
        return hostLanguage;
    }

    /**
     * Set the host language in use
     * @param language for example {@link Configuration#XSLT} or {@link Configuration#XQUERY}
     */

    public void setHostLanguage(int language) {
        hostLanguage = language;
    }

    /**
     * Ask whether this pipeline is a serializing pipeline
     * @return true if this pipeline is producing serialized output
     */

    public boolean isSerializing() {
        return isSerializing;
    }

    /**
     * Set whether this pipeline is a serializing pipeline
     * @param isSerializing true if this pipeline is producing serialized output
     */

    public void setSerializing(boolean isSerializing) {
        this.isSerializing = isSerializing;
    }

    /**
     * Set whether attribute defaults defined in a schema or DTD are to be expanded or not
     * (by default, fixed and default attribute values are expanded, that is, they are inserted
     * into the document during validation as if they were present in the instance being validated)
     * @param expand true if defaults are to be expanded, false if not
     */

    public void setExpandAttributeDefaults(boolean expand) {
        parseOptions.setExpandAttributeDefaults(expand);
    }

    /**
     * Ask whether attribute defaults defined in a schema or DTD are to be expanded or not
     * (by default, fixed and default attribute values are expanded, that is, they are inserted
     * into the document during validation as if they were present in the instance being validated)
     * @return true if defaults are to be expanded, false if not
     */

    public boolean isExpandAttributeDefaults() {
        return parseOptions.isExpandAttributeDefaults();
    }

    /**
     * Set a CopyInformee to be notified of element nodes if the origin of the pipeline
     * is copying elements.
     * @param informee the CopyInformee to be notified
     */

    public void setCopyInformee(CopyInformee informee) {
        this.copyInformee = informee;
    }

    /**
     * Get the CopyInformee to be notified of element nodes if the origin of the pipeline
     * is copying elements.
     * @return the CopyInformee to be notified
     */

    public CopyInformee getCopyInformee() {
        return copyInformee;
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

