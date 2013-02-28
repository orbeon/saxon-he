package net.sf.saxon.s9api;

import net.sf.saxon.Configuration;
import net.sf.saxon.event.PipelineConfiguration;
import net.sf.saxon.event.Receiver;
import net.sf.saxon.event.Sender;
import net.sf.saxon.event.Sink;
import net.sf.saxon.lib.FeatureKeys;
import net.sf.saxon.lib.ParseOptions;
import net.sf.saxon.lib.Validation;
import net.sf.saxon.trans.XPathException;
import net.sf.saxon.type.SchemaType;
import net.sf.saxon.value.Whitespace;

import javax.xml.transform.ErrorListener;
import javax.xml.transform.Source;

/**
 * A <tt>SchemaValidator</tt> is an object that is used for validating instance documents against a schema.
 * The schema consists of the collection of schema components that are available within the schema
 * cache maintained by the SchemaManager, together with any additional schema components located
 * during the course of validation by means of an xsl:schemaLocation or xsi:noNamespaceSchemaLocation
 * attribute within the instance document.
 *
 * <p>If validation fails, an exception is thrown. If validation succeeds, the validated document
 * can optionally be written to a specified destination. This will be a copy of the original document,
 * augmented with default values for absent elements and attributes, and carrying type annotations
 * derived from the schema processing. Expansion of defaults can be suppressed by means of the method
 * {@link #setExpandAttributeDefaults(boolean)}.</p>
 *
 * <p>A <tt>SchemaValidator</tt> is serially resuable but not thread-safe. That is, it should normally
 * be used in the thread where it is created, but it can be used more than once, to validate multiple
 * input documents.</p>
 * 
 * <p>A <tt>SchemaValidator</tt> is a <tt>Destination</tt>, which allows it to receive the output of a
 * query or transformation to be validated.</p>
 *
 * <p>Saxon does not deliver the full PSVI as described in the XML schema specifications,
 * only the subset of the PSVI properties featured in the XDM data model.</p>
 *
 */

public class SchemaValidator implements Destination {

    private Configuration config;
    private boolean lax;
    private ErrorListener errorListener;
    /*@Nullable*/ private Destination destination;
    private QName documentElementName;
    private SchemaType documentElementType;
    private boolean expandAttributeDefaults = true;
    private boolean useXsiSchemaLocation;
//    private Controller controller;

    // TODO: reinstate the saxon:param mechanism for parameterizing schema validation.

    protected SchemaValidator(Configuration config) {
        this.config = config;
        this.useXsiSchemaLocation = (Boolean) config.getConfigurationProperty(
                FeatureKeys.USE_XSI_SCHEMA_LOCATION);
    }

    /**
     * The validation mode may be either strict or lax. The default is strict; this method may be called
     * to indicate that lax validation is required. With strict validation, validation fails if no element
     * declaration can be located for the outermost element. With lax validation, the absence of an
     * element declaration results in the content being considered valid.
     * @param lax true if validation is to be lax, false if it is to be strict
     */

    public void setLax(boolean lax) {
        this.lax = lax;
    }

    /**
     * Ask whether validation is to be in lax mode.
     * @return true if validation is to be in lax mode, false if it is to be in strict mode.
     */

    public boolean isLax() {
        return lax;
    }

    /**
     * Set the ErrorListener to be used while validating instance documents.
     * @param listener The error listener to be used. This is notified of all errors detected during the
     * validation episode.
     */

    public void setErrorListener(ErrorListener listener) {
        this.errorListener = listener;
    }

    /**
     * Get the ErrorListener being used while validating instance documents
     * @return listener The error listener in use. This is notified of all errors detected during the
     * validation episode. Returns null if no user-supplied ErrorListener has been set.
     */

    public ErrorListener getErrorListener() {
        return errorListener;
    }

    /**
      * Say whether the schema processor is to take account of any xsi:schemaLocation and
      * xsi:noNamespaceSchemaLocation attributes encountered while validating an instance document
      * @param recognize true if these two attributes are to be recognized; false if they are to
      * be ignored. Default is true.
      */

     public void setUseXsiSchemaLocation(boolean recognize) {
         useXsiSchemaLocation = recognize;
     }

     /**
      * Ask whether the schema processor is to take account of any xsi:schemaLocation and
      * xsi:noNamespaceSchemaLocation attributes encountered while validating an instance document
      * @return true if these two attributes are to be recognized; false if they are to
      * be ignored. Default is true.
      */

     public boolean isUseXsiSchemaLocation() {
         return useXsiSchemaLocation;
     }


    /**
     * Set the Destination to receive the validated document. If no destination is supplied, the
     * validated document is discarded.
     * @param destination the destination to receive the validated document
     */

    public void setDestination(/*@Nullable*/ Destination destination) {
        this.destination = destination;
    }

    /**
     * Get the Destination that will receive the validated document. Return null if no destination
     * has been set.
     * @return the destination to receive the validated document, or null if none has been supplied
     */


   /*@Nullable*/ public Destination getDestination() {
        return destination;
    }

    /**
     * Set the name of the required top-level element of the document to be validated (that is, the
     * name of the outermost element of the document). If no value is supplied, there is no constraint
     * on the required element name
     * @param name the name of the document element, as a QName; or null to remove a previously-specified
     * value.
     */

    public void setDocumentElementName(QName name) {
        documentElementName = name;
    }

    /**
     * Get the name of the required top-level element of the document to be validated.
     * @return the name of the required document element, or null if no value has been set.
     */

    public QName getDocumentElementName() {
        return documentElementName;
    }

    /**
     * Set the name of the required type of the top-level element of the document to be validated.
     * If no value is supplied, there is no constraint on the required type
     * @param name the name of the type of the document element, as a QName;
     * or null to remove a previously-specified value. This must be the name of a type in the
     * schema (typically but not necessarily a complex type).
     * @throws SaxonApiException if there is no known type with this name
     */

    public void setDocumentElementTypeName(QName name) throws SaxonApiException {
        int fp = config.getNamePool().allocate(
                "", name.getNamespaceURI(), name.getLocalName());
        documentElementType = config.getSchemaType(fp);
        if (documentElementType == null) {
            throw new SaxonApiException("Unknown type " + name.getClarkName());
        }
    }

    /**
     * Get the name of the required type of the top-level element of the document to be validated.
     * @return the name of the required type of the document element, or null if no value has been set.
     */

    public QName getDocumentElementTypeName() {
        if (documentElementType == null) {
            return null;
        } else {
            int fp = documentElementType.getFingerprint();
            return new QName(config.getNamePool().getStructuredQName(fp));
        }
    }

    /**
     * Get the schema type against which the document element is to be validated
     * @return the schema type
     */

    protected SchemaType getDocumentElementType() {
        return documentElementType;
    }

    /**
      * Set whether attribute defaults defined in a schema are to be expanded or not
      * (by default, fixed and default attribute values are expanded, that is, they are inserted
      * into the document during validation as if they were present in the instance being validated)
      * @param expand true if defaults are to be expanded, false if not
      */

     public void setExpandAttributeDefaults(boolean expand) {
         expandAttributeDefaults = expand;
     }

     /**
      * Ask whether attribute defaults defined in a schema are to be expanded or not
      * (by default, fixed and default attribute values are expanded, that is, they are inserted
      * into the document during validation as if they were present in the instance being validated)
      * @return true if defaults are to be expanded, false if not
      */

     public boolean isExpandAttributeDefaults() {
         return expandAttributeDefaults;
     }

//    /**
//     * Set the value of a schema parameter (a parameter defined in the schema using
//     * the <code>saxon:param</code> extension)
//     * @param name the name of the schema parameter, as a QName
//     * @param value the value of the schema parameter, or null to clear a previously set value
//     */
//
//    public void setParameter(QName name, XdmValue value) {
//        if (controller == null) {
//            controller = new Controller(config);
//        }
//        controller.setParameter(name.getStructuredQName(),
//                (value == null ? null : value.getUnderlyingValue()));
//    }

//    /**
//     * Get the value that has been set for a schema parameter (a parameter defined in the schema using
//     * the <code>saxon:param</code> extension)
//     * @param name the parameter whose name is required
//     * @return the value that has been set for the parameter, or null if no value has been set
//     */
//
//    public XdmValue getParameter(QName name) {
//        if (controller == null) {
//            return null;
//        }
//        Object oval = controller.getParameter(name.getClarkName());
//        if (oval == null) {
//            return null;
//        }
//        if (oval instanceof ValueRepresentation) {
//            return XdmValue.wrap((ValueRepresentation)oval);
//        }
//        throw new IllegalStateException(oval.getClass().getName());
//    }


    /**
     * Validate an instance document supplied as a Source object
     * @param source the instance document to be validated. The call getSystemId() applied to
     * this source object must return the base URI used for dereferencing any xsi:schemaLocation
     * or xsi:noNamespaceSchemaLocation attributes
     * @throws SaxonApiException if the source document is found to be invalid
     */

    public void validate(Source source) throws SaxonApiException {
        Receiver receiver = getReceiver(config, source.getSystemId());
        try {
            ParseOptions options = new ParseOptions();
            options.setContinueAfterValidationErrors(true);
            Sender.send(source, receiver, options);
            if (destination != null) {
                destination.close();
            }
        } catch (XPathException e) {
            throw new SaxonApiException(e);
        }
    }

    public Receiver getReceiver(Configuration config) throws SaxonApiException {
        return getReceiver(config, null);
    }

    private Receiver getReceiver(Configuration config, /*@Nullable*/ String systemId) throws SaxonApiException {
        PipelineConfiguration pipe = config.makePipelineConfiguration();
        pipe.setExpandAttributeDefaults(expandAttributeDefaults);
        pipe.setUseXsiSchemaLocation(useXsiSchemaLocation);
        pipe.setRecoverFromValidationErrors(true);
        pipe.getParseOptions().setCheckEntityReferences(true);
//        if (controller != null) {
//            pipe.setController(controller);
//            //Executable exec = config.obtainSchemaExecutable();
//            //controller.setExecutable(exec);
//            try {
//                controller.defineGlobalParameters();
//                controller.getBindery().allocateGlobals(config.makeSlotManager());
//            } catch (XPathException e) {
//                throw new SaxonApiException(e);
//            }
//        }

        Receiver output = (destination == null ? new Sink(pipe) : destination.getReceiver(config));
        output.setPipelineConfiguration(pipe);

        int topLevelElement = -1;
        if (documentElementName != null) {
            topLevelElement = config.getNamePool().allocate(
                    "", documentElementName.getNamespaceURI(), documentElementName.getLocalName());
        }
        Receiver receiver = config.getDocumentValidator(
                output,
                systemId,
                (lax ? Validation.LAX : Validation.STRICT),
                Whitespace.NONE,
                documentElementType,
                topLevelElement);
        if (errorListener != null) {
            pipe.setErrorListener(errorListener);
        }
        return receiver;
    }

    /**
     * Close the destination, allowing resources to be released. Saxon calls this method when
     * it has finished writing to the destination.
     */

    public void close() throws SaxonApiException {
        if (destination != null) {
            destination.close();
            destination = null;
        }
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
// The Original Code is: all this file
//
// The Initial Developer of the Original Code is Saxonica Limited.
// Portions created by ___ are Copyright (C) ___. All rights reserved.
//
// Contributor(s):
//