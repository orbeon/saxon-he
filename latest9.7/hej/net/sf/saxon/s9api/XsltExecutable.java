////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
// Copyright (c) 2015 Saxonica Limited.
// This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
// If a copy of the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
// This Source Code Form is "Incompatible With Secondary Licenses", as defined by the Mozilla Public License, v. 2.0.
////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

package net.sf.saxon.s9api;

import net.sf.saxon.Configuration;
import net.sf.saxon.PreparedStylesheet;
import net.sf.saxon.expr.instruct.GlobalParam;
import net.sf.saxon.style.StylesheetPackage;
import net.sf.saxon.trace.ExpressionPresenter;
import net.sf.saxon.trans.XPathException;
import net.sf.saxon.value.SequenceType;

import javax.xml.transform.stream.StreamResult;
import java.io.IOException;
import java.io.OutputStream;
import java.util.HashMap;

/**
 * An XsltExecutable represents the compiled form of a stylesheet.
 * To execute the stylesheet, it must first be loaded to form an {@link XsltTransformer}.
 * <p/>
 * <p>An XsltExecutable is immutable, and therefore thread-safe.
 * It is simplest to load a new XsltTransformer each time the stylesheet is to be run.
 * However, the XsltTransformer is serially reusable within a single thread. </p>
 * <p/>
 * <p>An XsltExecutable is created by using one of the <code>compile</code> methods on the
 * {@link XsltCompiler} class.</p>
 */
public class XsltExecutable {

    Processor processor;
    PreparedStylesheet preparedStylesheet;

    protected XsltExecutable(Processor processor, PreparedStylesheet preparedStylesheet) {
        this.processor = processor;
        this.preparedStylesheet = preparedStylesheet;
    }

    /**
     * Get the Processor that was used to create this XsltExecutable
     *
     * @return the original Processor object
     * @since 9.6
     */

    public Processor getProcessor() {
        return processor;
    }

    /**
     * Load the stylesheet to prepare it for execution. This version of the load() method
     * creates an <code>XsltTransformer</code> which offers interfaces for stylesheet
     * invocation corresponding to those described in the XSLT 2.0 specification. It can be used
     * with XSLT 2.0 or XSLT 3.0 stylesheets, but does not offer new XSLT 3.0 functionality such
     * as the ability to supply parameters to the initial template, or the ability to invoke
     * stylesheet-defined functions, or the ability to return an arbitrary sequence as a result
     * without wrapping it in a document node. If such facilities are required, use the
     * method {@link #load30()} in preference.
     *
     * @return An XsltTransformer. The returned XsltTransformer can be used to set up the
     *         dynamic context for stylesheet evaluation, and to run the stylesheet.
     */

    public XsltTransformer load() {
        return new XsltTransformer(processor, preparedStylesheet.newController(), preparedStylesheet.getCompileTimeParams());
    }

    /**
     * Load the stylesheet to prepare it for execution. This version of the load() method
     * creates an <code>Xslt30Transformer</code> which offers interfaces for stylesheet
     * invocation corresponding to those described in the XSLT 3.0 specification. It can be used
     * with XSLT 2.0 or XSLT 3.0 stylesheets, and in both cases it offers new XSLT 3.0 functionality such
     * as the ability to supply parameters to the initial template, or the ability to invoke
     * stylesheet-defined functions, or the ability to return an arbitrary sequence as a result
     * without wrapping it in a document node.
     *
     * @return An Xslt30Transformer. The returned Xslt30Transformer can be used to set up the
     *         dynamic context for stylesheet evaluation, and to run the stylesheet.
     */

    public Xslt30Transformer load30() {
//        String nextInChain = preparedStylesheet.getOutputProperties().getProperty(SaxonOutputKeys.NEXT_IN_CHAIN);
//        if (nextInChain != null) {
//            processor.getUnderlyingConfiguration().getLogger().warning("saxon:next-in-chain is ignored when using an Xslt30Transformer");
//        }
        return new Xslt30Transformer(processor, preparedStylesheet.newController(), preparedStylesheet.getCompileTimeParams());
    }

    /**
     * Produce a diagnostic representation of the compiled stylesheet, in XML form.
     * <p><i>The detailed form of this representation is not stable (or even documented).<i></p>
     *
     * @param destination the destination for the XML document containing the diagnostic representation
     *                    of the compiled stylesheet
     * @since 9.1
     */

    public void explain(Destination destination) throws SaxonApiException {
        Configuration config = processor.getUnderlyingConfiguration();
        try {
            preparedStylesheet.explain(new ExpressionPresenter(config, destination.getReceiver(config)));
        } catch (XPathException e) {
            throw new SaxonApiException(e);
        }
    }

    /**
     * Produce a representation of the compiled stylesheet, in XML form, suitable for
     * distribution and reloading. If the configuration under which the export takes place
     * is suitably licensed, then license information will be included in the export file
     * allowing execution of the stylesheet without any additional license.
     * <p><i>The detailed form of the output representation is not documented.<i></p>
     *
     * @param destination the destination for the XML document containing the diagnostic representation
     *                    of the compiled stylesheet. The stream will be closed when writing has finished.
     * @since 9.7
     */

    public void export(OutputStream destination) throws SaxonApiException {
        Configuration config = processor.getUnderlyingConfiguration();
        ExpressionPresenter presenter = new ExpressionPresenter();
        if (preparedStylesheet.getTopLevelPackage().getTargetEdition().equals("JS")) {
            presenter.setOption("target", "JS");
        }
        presenter.init(config, new StreamResult(destination), true);
        try {
            ((StylesheetPackage)preparedStylesheet.getTopLevelPackage()).export(presenter);
        } catch (XPathException e) {
            throw new SaxonApiException(e);
        }
        try {
            destination.close();
        } catch (IOException e) {
            throw new SaxonApiException(e);
        }
    }

    /**
     * Produce a representation of the compiled stylesheet for a particular target environment, in XML form, suitable for
     * distribution and reloading. If the configuration under which the export takes place
     * is suitably licensed, then license information will be included in the export file
     * allowing execution of the stylesheet without any additional license.
     * <p><i>The detailed form of the output representation is not documented.<i></p>
     *
     * @param destination the destination for the XML document containing the diagnostic representation
     *                    of the compiled stylesheet. The stream will be closed when writing has finished.
     * @param target the target environment. The only value currently recognized is "JS", which exports
     *               the package for running under Saxon-JS.
     * @since 9.7
     */

    public void export(OutputStream destination, String target) throws SaxonApiException {
        Configuration config = processor.getUnderlyingConfiguration();
        ExpressionPresenter presenter = new ExpressionPresenter();
        presenter.setOption("target", target);
        presenter.init(config, new StreamResult(destination), true);
        try {
            ((StylesheetPackage)preparedStylesheet.getTopLevelPackage()).export(presenter);
        } catch (XPathException e) {
            throw new SaxonApiException(e);
        }
        try {
            destination.close();
        } catch (IOException e) {
            throw new SaxonApiException(e);
        }
    }

    /**
     * Get the whitespace stripping policy defined by this stylesheet, that is, the policy
     * defined by the xsl:strip-space and xsl:preserve-space elements in the stylesheet
     *
     * @return a newly constructed WhitespaceStrippingPolicy based on the declarations in this
     *         stylesheet. This policy can be used as input to a {@link DocumentBuilder}.
     */

    public WhitespaceStrippingPolicy getWhitespaceStrippingPolicy() {
        return new WhitespaceStrippingPolicy(preparedStylesheet);
    }

    /**
     * Get the names of the xsl:param elements defined in this stylesheet, with details
     * of each parameter including its required type, and whether it is required or optional
     *
     * @return a HashMap whose keys are the names of global parameters in the stylesheet,
     *         and whose values are {@link ParameterDetails} objects giving information about the
     *         corresponding parameter.
     * @since 9.3
     */

    /*@NotNull*/
    public HashMap<QName, ParameterDetails> getGlobalParameters() {
        Iterable<GlobalParam> globals = preparedStylesheet.getGlobalParameters();
        if (globals == null) {
            return new HashMap<QName, ParameterDetails>(1);
        }
        HashMap<QName, ParameterDetails> params = new HashMap<QName, ParameterDetails>();
        for (GlobalParam v : globals) {
            ParameterDetails details = new ParameterDetails(v.getRequiredType(), v.isRequiredParam());
            params.put(new QName(v.getVariableQName()), details);
        }
        return params;
    }

    /**
     * Inner class containing information about a global parameter to a compiled stylesheet
     *
     * @since 9.3
     */

    public class ParameterDetails {

        private SequenceType type;
        private boolean isRequired;

        protected ParameterDetails(SequenceType type, boolean isRequired) {
            this.type = type;
            this.isRequired = isRequired;
        }

        /**
         * Get the declared item type of the parameter
         *
         * @return the type defined in the <code>as</code> attribute of the <code>xsl:param</code> element,
         *         without its occurrence indicator
         */

        public ItemType getDeclaredItemType() {
            return new ConstructedItemType(type.getPrimaryType(), processor);
        }

        /**
         * Get the declared cardinality of the parameter
         *
         * @return the occurrence indicator from the type appearing in the <code>as</code> attribute
         *         of the <code>xsl:param</code> element
         */

        public OccurrenceIndicator getDeclaredCardinality() {
            return OccurrenceIndicator.getOccurrenceIndicator(type.getCardinality());
        }

        /**
         *
         */

        public SequenceType getUnderlyingDeclaredType() {
            return type;
        }

        /**
         * Ask whether the parameter is required (mandatory) or optional
         *
         * @return true if the parameter is mandatory (<code>required="yes"</code>), false
         *         if it is optional
         */

        public boolean isRequired() {
            return this.isRequired;
        }
    }


    /**
     * Get the underlying implementation object representing the compiled stylesheet. This provides
     * an escape hatch into lower-level APIs. The object returned by this method may change from release
     * to release.
     *
     * @return the underlying implementation of the compiled stylesheet
     */

    public PreparedStylesheet getUnderlyingCompiledStylesheet() {
        return preparedStylesheet;
    }

}

