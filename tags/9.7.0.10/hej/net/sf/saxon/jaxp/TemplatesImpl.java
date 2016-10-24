////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
// Copyright (c) 2015 Saxonica Limited.
// This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
// If a copy of the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
// This Source Code Form is "Incompatible With Secondary Licenses", as defined by the Mozilla Public License, v. 2.0.
////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

package net.sf.saxon.jaxp;

import net.sf.saxon.s9api.XsltExecutable;

import javax.xml.transform.Templates;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import java.util.Properties;

/**
 * Saxon implementation of the JAXP Templates interface.
 * <p/>
 * <p>Since Saxon 9.6, JAXP interfaces are implemented as a layer above the s9api interface</p>
 */
public class TemplatesImpl implements Templates {

    private XsltExecutable executable;

    public TemplatesImpl(XsltExecutable executable) {
        this.executable = executable;
    }

    /**
     * Create a new transformation context for this Templates object.
     *
     * @return A valid non-null instance of a Transformer.
     * @throws javax.xml.transform.TransformerConfigurationException
     *          if a Transformer can not be created.
     */
    public Transformer newTransformer() throws TransformerConfigurationException {
        return new TransformerImpl(executable, executable.load());
    }

    /**
     * Get the properties corresponding to the effective xsl:output element.
     * The object returned will
     * be a clone of the internal values. Accordingly, it can be mutated
     * without mutating the Templates object, and then handed in to
     * {@link javax.xml.transform.Transformer#setOutputProperties}.
     * <p/>
     * <p>The properties returned should contain properties set by the stylesheet,
     * and these properties are "defaulted" by default properties specified by
     * <a href="http://www.w3.org/TR/xslt#output">section 16 of the
     * XSL Transformations (XSLT) W3C Recommendation</a>.  The properties that
     * were specifically set by the stylesheet should be in the base
     * Properties list, while the XSLT default properties that were not
     * specifically set should be in the "default" Properties list.  Thus,
     * getOutputProperties().getProperty(String key) will obtain any
     * property in that was set by the stylesheet, <em>or</em> the default
     * properties, while
     * getOutputProperties().get(String key) will only retrieve properties
     * that were explicitly set in the stylesheet.</p>
     * <p/>
     * <p>For XSLT,
     * <a href="http://www.w3.org/TR/xslt#attribute-value-templates">Attribute
     * Value Templates</a> attribute values will
     * be returned unexpanded (since there is no context at this point).  The
     * namespace prefixes inside Attribute Value Templates will be unexpanded,
     * so that they remain valid XPath values.</p>
     *
     * @return A Properties object, never null.
     */
    public Properties getOutputProperties() {
        Properties details = executable.getUnderlyingCompiledStylesheet().getDefaultOutputProperties();
        return new Properties(details);
    }
}
