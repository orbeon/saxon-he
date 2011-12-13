package net.sf.saxon.trans;


import net.sf.saxon.Configuration;
import org.apache.xml.resolver.CatalogManager;
import org.apache.xml.resolver.helpers.Debug;

import javax.xml.transform.TransformerException;

/**
 * Provides the interface to the Apache catalog resolver. This is in a separate class to ensure that no failure
 * occurs if the resolver code is not on the classpath, unless catalog resolution is explicitly requested.
 */

public class XmlCatalogResolver {

    public static void setCatalog(String catalog, final Configuration config, boolean isTracing) throws XPathException {
        System.setProperty("xml.catalog.files", catalog);
        Configuration.getPlatform().setDefaultSAXParserFactory();
        if (isTracing) {
            // Customize the resolver to write messages to the Saxon logging destination
            CatalogManager.getStaticManager().debug = new Debug() {
                @Override
                public void message(int level, String message) {
                    if (level <= getDebug()) {
                        config.getStandardErrorOutput().println(message);
                    }
                }

                @Override
                public void message(int level, String message, String spec) {
                    if (level <= getDebug()) {
                        config.getStandardErrorOutput().println(message + ": " + spec);
                    }
                }

                @Override
                public void message(int level, String message, String spec1, String spec2) {
                    if (level <= getDebug()) {
                        config.getStandardErrorOutput().println(message + ": " + spec1);
                        config.getStandardErrorOutput().println("\t" + spec2);
                    }
                }
            };
            CatalogManager.getStaticManager().setVerbosity(2);
        }
        config.setSourceParserClass("org.apache.xml.resolver.tools.ResolvingXMLReader");
        config.setStyleParserClass("org.apache.xml.resolver.tools.ResolvingXMLReader");
        try {
            config.setURIResolver(config.makeURIResolver("org.apache.xml.resolver.tools.CatalogResolver"));
        } catch (TransformerException err) {
            throw XPathException.makeXPathException(err);
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