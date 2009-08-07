package net.sf.saxon.dotnet;

import cli.System.Environment;
import cli.System.Reflection.Assembly;
import cli.System.Xml.*;
import net.sf.saxon.AugmentedSource;
import net.sf.saxon.Configuration;
import net.sf.saxon.FeatureKeys;
import net.sf.saxon.Platform;
import net.sf.saxon.event.PipelineConfiguration;
import net.sf.saxon.functions.ResolveURI;
import net.sf.saxon.om.NamespaceConstant;
import net.sf.saxon.om.Validation;
import net.sf.saxon.pull.PullProvider;
import net.sf.saxon.pull.PullSource;
import net.sf.saxon.sort.CodepointCollator;
import net.sf.saxon.sort.NamedCollation;
import net.sf.saxon.sort.StringCollator;
import net.sf.saxon.trans.XPathException;
import net.sf.saxon.type.SchemaType;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Source;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactoryConfigurationError;
import javax.xml.transform.stream.StreamSource;
import java.io.InputStream;
import java.io.Reader;
import java.nio.charset.spi.CharsetProvider;
import java.util.Properties;
import java.util.StringTokenizer;

/**
 * Implementation of the Platform interface containing methods appropriate to the .NET platform
 */

public class DotNetPlatform implements Platform {

    /**
     * Create a link to the extended character sets in charsets.jar. This makes these accessible for
     * loading.
     */

    private static CharsetProvider provider = new sun.nio.cs.ext.ExtendedCharsets();

    public DotNetPlatform(){}

    /**
     * Perform platform-specific initialization of the configuration
     */

    public void initialize(Configuration config) {
        config.setURIResolver(new DotNetURIResolver(new XmlUrlResolver()));
        config.setModuleURIResolver(new DotNetStandardModuleURIResolver(new XmlUrlResolver()));
        config.setCollectionURIResolver(new DotNetCollectionURIResolver());
    }

    /**
     * Return true if this is the Java platform
     */

    public boolean isJava() {
        return false;
    }

    /**
     * Return true if this is the .NET platform
     */

    public boolean isDotNet() {
        return true;
    }

    /**
     * Get the verifier appropriate to the platform
     */

    public String getVerifierClassName() {
        return "com.saxonica.config.DotNetVerifier";
    }

    /**
     * Get the platform version
     */

    public String getPlatformVersion() {
        return ".NET " + Environment.get_Version().ToString() +
                " on " + Environment.get_OSVersion().ToString();
    }

    /**
     * Get a suffix letter to add to the Saxon version number to identify the platform
     */

    public String getPlatformSuffix() {
        return "N";
    }

    /**
     * Get a parser by instantiating the SAXParserFactory
     *
     * @return the parser (XMLReader)
     */

    public XMLReader loadParser() {
        XMLReader parser;
        try {
//            parser = SAXParserFactory.newInstance("org.apache.xerces.jaxp.SAXParserFactoryImpl", getClass().getClassLoader())
//                    .newSAXParser().getXMLReader();
            parser = new org.apache.xerces.jaxp.SAXParserFactoryImpl().newSAXParser().getXMLReader();
        } catch (ParserConfigurationException err) {
            throw new TransformerFactoryConfigurationError(err);
        } catch (SAXException err) {
            throw new TransformerFactoryConfigurationError(err);
        }
        return parser;
    }

    /**
     * Convert a StreamSource to either a SAXSource or a PullSource, depending on the native
     * parser of the selected platform
     *
     * @param pipe the pipeline configuration
     * @param input the supplied StreamSource
     * @param validation indicates whether schema validation is required, adn in what mode
     * @param dtdValidation true if DTD validation is required
     * @param stripspace defines the requird whitespace handling
     * @return the PullSource or SAXSource, initialized with a suitable parser, or the original
     * input Source, if now special handling is required or possible. May also return an AugmentedSource
     * that wraps one of these.
     */

    public Source getParserSource(PipelineConfiguration pipe, StreamSource input, int validation, boolean dtdValidation,
                                  int stripspace) {
        Configuration config = pipe.getConfiguration();
        boolean preferJaxp = ((Boolean)config.getConfigurationProperty(FeatureKeys.PREFER_JAXP_PARSER)).booleanValue();
        InputStream is = input.getInputStream();
        if (is != null) {
            if (is instanceof DotNetInputStream && !preferJaxp) {
                XmlReader parser = new XmlTextReader(input.getSystemId(),
                        ((DotNetInputStream)is).getUnderlyingStream());
                ((XmlTextReader)parser).set_WhitespaceHandling(WhitespaceHandling.wrap(WhitespaceHandling.All));
                ((XmlTextReader)parser).set_Normalization(true);
                if (pipe.getURIResolver() instanceof DotNetURIResolver) {
                    ((XmlTextReader)parser).set_XmlResolver(
                            ((DotNetURIResolver)pipe.getURIResolver()).getXmlResolver());
                }
                
                // Always need a validating parser, because that's the only way to get entity references expanded
                parser = new XmlValidatingReader(parser);
                if (dtdValidation) {
                    ((XmlValidatingReader)parser).set_ValidationType(ValidationType.wrap(ValidationType.DTD));
                } else {
                    ((XmlValidatingReader)parser).set_ValidationType(ValidationType.wrap(ValidationType.None));
                }
                PullProvider provider = new DotNetPullProvider(parser);
                //provider = new PullTracer(provider);
                PullSource ps = new PullSource(provider);
                //System.err.println("Using PullSource(stream)");
                ps.setSystemId(input.getSystemId());
                if (validation == Validation.DEFAULT) {
                    return ps;
                } else {
                    AugmentedSource as = AugmentedSource.makeAugmentedSource(ps);
                    as.setSchemaValidationMode(validation);
                    return as;
                }
            } else {
                return input;
            }
        }
        Reader reader = input.getReader();
        if (reader != null) {
            if (reader instanceof DotNetReader && !preferJaxp) {
                XmlReader parser = new XmlTextReader(input.getSystemId(),
                        ((DotNetReader)reader).getUnderlyingTextReader());
                ((XmlTextReader)parser).set_Normalization(true);
                ((XmlTextReader)parser).set_WhitespaceHandling(WhitespaceHandling.wrap(WhitespaceHandling.All));
                if (pipe.getURIResolver() instanceof DotNetURIResolver) {
                    ((XmlTextReader)parser).set_XmlResolver(
                            ((DotNetURIResolver)pipe.getURIResolver()).getXmlResolver());
                }

                // Always need a validating parser, because that's the only way to get entity references expanded
                parser = new XmlValidatingReader(parser);
                if (dtdValidation) {
                    ((XmlValidatingReader)parser).set_ValidationType(ValidationType.wrap(ValidationType.DTD));
                } else {
                    ((XmlValidatingReader)parser).set_ValidationType(ValidationType.wrap(ValidationType.None));
                }
                PullSource ps = new PullSource(new DotNetPullProvider(parser));
                //System.err.println("Using PullSource(reader)");
                ps.setSystemId(input.getSystemId());
                if (validation == Validation.DEFAULT) {
                    return ps;
                } else {
                    AugmentedSource as = AugmentedSource.makeAugmentedSource(ps);
                    as.setSchemaValidationMode(validation);
                    return as;
                }
            } else {
                return input;
            }
        }
        String uri = input.getSystemId();
        if (uri != null) {
            try {
                Source r = pipe.getURIResolver().resolve(uri, null);
                if (r == null) {
                    return input;
                } else if (r instanceof AugmentedSource) {
                    Source r2 = ((AugmentedSource)r).getContainedSource();
                    if (r2 instanceof StreamSource) {
                        r2 = getParserSource(pipe, (StreamSource)r2, validation, dtdValidation, stripspace);
                        return r2;
                    } else {
                        return r2;
                    }
                } else if (r instanceof StreamSource && r != input) {
                    Source r2 = getParserSource(pipe, (StreamSource)r, validation, dtdValidation, stripspace);
                    AugmentedSource as = AugmentedSource.makeAugmentedSource(r2);
                    as.setPleaseCloseAfterUse(true);
                    return as;
                } else {
                    return r;
                }
            } catch (TransformerException err) {
                return input;
            }
        }
        return input;
    }

    /**
     * Obtain a collation with a given set of properties. The set of properties is extensible
     * and variable across platforms. Common properties with example values include lang=ed-GB,
     * strength=primary, case-order=upper-first, ignore-modifiers=yes, alphanumeric=yes.
     * Properties that are not supported are generally ignored; however some errors, such as
     * failing to load a requested class, are fatal.
     * @param config the configuration object
     * @param props the desired properties of the collation
     * @param uri the collation URI
     * @return a collation with these properties
     * @throws XPathException if a fatal error occurs
     */

    public StringCollator makeCollation(Configuration config, Properties props, String uri) throws XPathException {
        return DotNetCollationFactory.makeCollation(config, props, uri);
    }

    /**
     * Given a collation, determine whether it is capable of returning collation keys.
     * The essential property of collation keys
     * is that if two values are equal under the collation, then the collation keys are
     * equal under the equals() method.
     *
     * @param collation the collation, provided as a Comparator
     * @return true if this collation can supply collation keys
     */

    public boolean canReturnCollationKeys(StringCollator collation) {
        return collation instanceof DotNetComparator ||
                collation instanceof CodepointCollator;
    }

    /**
     * Given a collation, get a collation key. The essential property of collation keys
     * is that if two values are equal under the collation, then the collation keys are
     * equal under the equals() method.
     *
     * @throws ClassCastException if the collation is not one that is capable of supplying
     *                            collation keys (this should have been checked in advance)
     */

    public Object getCollationKey(NamedCollation namedCollation, String value) {
        DotNetComparator c = (DotNetComparator)((NamedCollation)namedCollation).getCollation();
        return c.getCollationKey(value);
    }

    /**
     * Dynamically load a .NET class with a given name, starting with a URI that contains information
     * about the type and the assembly
     * @param uri A URI in the form
     * <code>clitype:Full.Type.Name?param=value;</code>
     * <p>Query parameters in the URL may be separated by semicolons or ampersands. The recognized parameters
     * are:</p>
     *
     * <ul>
     * <li>asm - the name of the assembly</li>
     * <li>ver - the version of the assembly</li>
     * <li>loc - the culture</li>
     * <li>sn  - the strong name</li>
     * <li>from - the location to load from</li>
     * <li>partialname - the partial name of the assembly</li>
     * </ul>
     * @param baseURI base URI of the expression containing an extension function call (null if not known). This
     * is used to resolve any relative URI appearing in the "from" parameter of the query part of the namespace URI
     * @param debug true if diagnostic tracing is enabled @return the cli.System.Type object representing the
     * dynamically loaded class
     * @return the successfully loaded Type
     */

    public cli.System.Type dynamicLoad(String uri, String baseURI, boolean debug) throws XPathException {
        if (uri.startsWith("clitype:")) {
            uri = uri.substring(8);
        } else {
            if (debug) {
                System.err.println("Unrecognized .NET external URI: " + uri);
            }
            throw new XPathException("Unrecognized .NET external URI: " + uri);
        }
        String typeName;
        String queryParams;
        int q = uri.indexOf('?');
        if (q == 0 || q == uri.length()-1) {
            if (debug) {
                System.err.println("Misplaced '?' in " + uri);
            }
            throw new XPathException("Misplaced '?' in " + uri);
        }
        if (q > 0) {
            typeName = uri.substring(0, q);
            queryParams = uri.substring(q+1);
        } else {
            typeName = uri;
            queryParams = "";
        }
        if ("".equals(queryParams)) {
            cli.System.Type type = cli.System.Type.GetType(typeName);
            if (type == null && debug) {
                try {
                    //noinspection ConstantIfStatement
                    if (false) throw new cli.System.TypeLoadException();
                    //noinspection UnusedDeclaration
                    cli.System.Type type2 = cli.System.Type.GetType(typeName, true);
                } catch (Exception err) {
                    System.err.println("Failed to load type " + typeName + ": " + err.getMessage());
                    return null;
                } catch (cli.System.TypeLoadException err) {
                    System.err.println("Failed to load type " + typeName + ": " + err.getMessage());
                    return null;
                }
                System.err.println("Failed to load type " + typeName);
            }
            return type;
        } else {
//            AssemblyName aname = new AssemblyName();
            String loadFrom = null;
            String href = null;
            String partialName = null;
            String asmName = null;
            String loc = null;
            String ver = null;
            String sn = null;
            StringTokenizer tok = new StringTokenizer(queryParams, ";&");
            while (tok.hasMoreTokens()) {
                String kv = tok.nextToken();
                int eq = kv.indexOf('=');
                if (eq <= 0) {
                    if (debug) {
                        System.err.println("Bad keyword=value pair in " + kv);
                    }
                    throw new XPathException("Bad keyword=value pair in " + kv);
                }
                String keyword = kv.substring(0, eq);
                String value = kv.substring(eq+1);
                if (keyword.equals("asm")) {
                    asmName = value;
                } else if (keyword.equals("ver")) {
                    ver = value;
                } else if (keyword.equals("loc")) {
                    loc = value;
                } else if (keyword.equals("sn")) {
                    sn = value;
                } else if (keyword.equals("from")) {
                    loadFrom = value;
                } else if (keyword.equals("href")) {
                    href = value;
                } else if (keyword.equals("partialname")) {
                    partialName = value;
                } else if (debug) {
                    System.err.println("Unrecognized keyword in URI: " + keyword + " (ignored)");
                }
            }
            Assembly asm;
            try {
                //noinspection ConstantIfStatement
                if (false) throw new cli.System.IO.FileNotFoundException();
                if (partialName != null) {
                    asm = Assembly.LoadWithPartialName(partialName);
                } else if (loadFrom != null) {
                    String abs = loadFrom;
                    if (baseURI != null) {
                        abs = ResolveURI.makeAbsolute(loadFrom, baseURI).toString();
                        if (debug) {
                            System.err.println("Absolute location URI: " + abs);
                        }
                    }
                    asm = Assembly.LoadFrom(abs);
                } else if (href != null) {
                    asm = Assembly.LoadFrom(href);                    
                } else {
                    String longName = asmName;
                    if (ver != null) {
                        longName += ", Version=" + ver;
                    }
                    if (loc != null) {
                        longName += ", Culture=" + loc;
                    }
                    if (sn != null) {
                        longName += ", PublicKeyToken=" + sn;
                    }
                    asm = Assembly.Load(longName);
//                    asm = Assembly.Load(aname);
                }
                if (debug) {
                    System.err.println("Assembly " + asm.get_FullName() + " successfully loaded");
                    System.err.println("Assembly codebase (" +
                            (asm.get_GlobalAssemblyCache() ? "GAC" : "local") +
                            "): " + asm.get_CodeBase());
                }
            } catch (cli.System.IO.FileNotFoundException err) {
                if (debug) {
                    System.err.println("Failed to load assembly "  + uri + ": " + err.getMessage() +
                            " (FileNotFoundException)");
                }
                throw new XPathException("Failed to load assembly " + uri + ": " + err.getMessage());
            } catch (Throwable err) {
                if (debug) {
                    System.err.println("Failed to load assembly "  + uri + ": " + err.getMessage() +
                            " (" + err.getClass().getName() + ")");
                }
                throw new XPathException("Failed to load assembly " + uri + ": " + err.getMessage());
            }
            cli.System.Type type = asm.GetType(typeName);
            if (type == null) {
                if (debug) {
                    System.err.println("Type " + typeName + " not found in assembly");
                }
                throw new XPathException("Type " + typeName + " not found in assembly");
            }
            return type;
        }
    }

    public SchemaType getExternalObjectType(Configuration config, String uri, String localName) {
        if (uri.equals(NamespaceConstant.DOT_NET_TYPE)) {
            return new DotNetExternalObjectType(cli.System.Type.GetType(localName), config);
        } else {
            throw new IllegalArgumentException("Type is not in .NET namespace");
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
// The Original Code is: all this file.
//
// The Initial Developer of the Original Code is Michael H. Kay
//
// Portions created by (your name) are Copyright (C) (your legal entity). All Rights Reserved.
//
// Contributor(s): none.
//


