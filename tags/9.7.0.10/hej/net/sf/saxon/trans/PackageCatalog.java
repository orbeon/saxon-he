////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
// Copyright (c) 2015 Saxonica Limited.
// This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
// If a copy of the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
// This Source Code Form is "Incompatible With Secondary Licenses", as defined by the Mozilla Public License, v. 2.0.
////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

package net.sf.saxon.trans;

/**
 * This class represents a package catalog. Currently package catalogs are used only
 * when XSLT is invoked from the command line using the <code>-pack</code> option.
 * The package catalog is an XML file; the only current constraints on its structure
 * are that at some level of nesting it contains <code>&lt;package&gt;</code> elements
 * that have an <code>@href</code> attribute containing the location of the package
 * source code.
 */

import net.sf.saxon.om.TreeModel;
import net.sf.saxon.s9api.*;

import javax.xml.transform.Source;
import javax.xml.transform.stream.StreamSource;
import java.util.ArrayList;
import java.util.List;


public class PackageCatalog {
    List<Source> packages;
    Processor processor;

    /**
     * Create a package catalog
     * @param processor the s9api processor
     */

    public PackageCatalog(Processor processor) {
        packages = new ArrayList<Source>();
        this.processor = processor;
    }

    /**
     * Create a package catalog from a supplied XML document containing the
     * package information
     * @param processor the s9api processor
     * @param s the XML source of the package catalog
     * @throws SaxonApiException if processing the catalog fails
     */

    public PackageCatalog(Processor processor, Source s) throws SaxonApiException {
        this(processor);
        addCatalog(s);
    }

    /**
     * Add packages from an XML document containing the package catalog
     * @param s the XML source of the package catalog
     * @throws SaxonApiException if processing the catalog fails
     */
    public void addCatalog(Source s) throws SaxonApiException {
        DocumentBuilder catbuilder = processor.newDocumentBuilder();
        catbuilder.setTreeModel(TreeModel.TINY_TREE);
        XdmNode catalog = catbuilder.build(s);
        XPathCompiler xpc = processor.newXPathCompiler();
        for (XdmItem href : xpc.evaluate("//package/@href", catalog)) {
            packages.add(new StreamSource(catalog.getBaseURI().resolve(href.getStringValue()).toString()));
        }
    }

    /**
     * Get the Source objects representing packages listed in the catalog
     * @return a list of Source objects representing the packages listed in the catalog
     */
    public List<Source> getSources() {
        return packages;
    }
}
