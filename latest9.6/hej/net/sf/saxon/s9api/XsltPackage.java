package net.sf.saxon.s9api;

////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
// Copyright (c) 2014 Saxonica Limited.
// This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
// If a copy of the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
// This Source Code Form is "Incompatible With Secondary Licenses", as defined by the Mozilla Public License, v. 2.0.
////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

import net.sf.saxon.PreparedStylesheet;
import net.sf.saxon.style.Compilation;
import net.sf.saxon.style.StylesheetPackage;
import net.sf.saxon.trans.CompilerInfo;
import net.sf.saxon.trans.XPathException;

/**
 * An XsltPackage object represents the result of compiling an XSLT 3.0 package, as
 * represented by an XML document containing an <code>xsl:package</code> element.
 *
 * @since 9.6
 */

public class XsltPackage {

    private Processor processor;
    private StylesheetPackage stylesheetPackage;

    protected XsltPackage(Processor p, StylesheetPackage pp) {
        this.processor = p;
        this.stylesheetPackage = pp;
    }

    /**
     * Get the processor under which this package was created
     *
     * @return the corresponding Processor
     */

    public Processor getProcessor() {
        return processor;
    }

    /**
     * Get the name of the package (the URI appearing as the value of <code>xsl:package/@name</code>)
     *
     * @return the package name
     */

    public String getName() {
        return stylesheetPackage.getPackageName();
    }

    /**
     * Get the version number of the package (the value of the attribute <code>xsl:package/@package-version</code>
     *
     * @return the package version number
     */

    public String getVersion() {
        return stylesheetPackage.getPackageVersion().toString();
    }

    /**
     * Link this package with the packages it uses to form an executable stylesheet. This process fixes
     * up any cross-package references to files, templates, and other components, and checks to ensure
     * that all such references are consistent.
     *
     * @return the resulting XsltExecutable
     * @throws SaxonApiException if any error is found during the linking process, for example
     * if the constituent packages containing duplicate component names, or if abstract components
     * are not resolved.
     */

    public XsltExecutable link() throws SaxonApiException {
        try {
            Compilation compilation = new Compilation(processor.getUnderlyingConfiguration(), new CompilerInfo());
            PreparedStylesheet pss = new PreparedStylesheet(compilation);
            stylesheetPackage.updatePreparedStylesheet(pss);
            return new XsltExecutable(processor, pss);
        } catch (XPathException e) {
            throw new SaxonApiException(e);
        }
    }

//    /**
//     * Save this compiled package to filestore. Not yet implemented (Saxon 9.6)
//     *
//     * @param file the file to which the compiled package should be saved
//     * @throws SaxonApiException if the compiled package cannot be saved to the specified
//     *                           location. In Saxon 9.6 this exception is always thrown.
//     */
//
//    public void save(File file) throws SaxonApiException {
//        throw new SaxonApiException("Saving compled packages is not yet implemented");
//    }

    /**
     * Escape-hatch interface to the underlying implementation class.
     * @return the underlying StylesheetPackage. The interface to StylesheetPackage
     * is not a stable part of the s9api API definition.
     */

    public StylesheetPackage getUnderlyingPreparedPackage() {
        return stylesheetPackage;
    }
}

