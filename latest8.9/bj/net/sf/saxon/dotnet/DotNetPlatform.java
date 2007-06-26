package net.sf.saxon.dotnet;

import cli.System.Environment;
import cli.System.Reflection.Assembly;
import cli.System.Uri;
import cli.System.Xml.*;
import net.sf.saxon.AugmentedSource;
import net.sf.saxon.Configuration;
import net.sf.saxon.Platform;
import net.sf.saxon.Version;
import net.sf.saxon.functions.FunctionLibrary;
import net.sf.saxon.functions.FunctionLibraryList;
import net.sf.saxon.functions.JavaExtensionFunctionFactory;
import net.sf.saxon.functions.JavaExtensionLibrary;
import net.sf.saxon.om.NamespaceConstant;
import net.sf.saxon.om.Validation;
import net.sf.saxon.pull.PullProvider;
import net.sf.saxon.pull.PullSource;
import net.sf.saxon.regex.RegularExpression;
import net.sf.saxon.sort.CodepointCollator;
import net.sf.saxon.sort.NamedCollation;
import net.sf.saxon.sort.StringCollator;
import net.sf.saxon.trans.StaticError;
import net.sf.saxon.trans.XPathException;
import net.sf.saxon.type.SchemaType;

import javax.xml.transform.Source;
import javax.xml.transform.TransformerException;
import javax.xml.transform.stream.StreamSource;
import java.io.InputStream;
import java.io.Reader;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Properties;
import java.util.StringTokenizer;

/**
 * Implementation of the Platform interface containing methods appropriate to the .NET platform
 */

public class DotNetPlatform implements Platform {

    private static DotNetPlatform theInstance = new DotNetPlatform();

    public static DotNetPlatform getInstance() {
        return theInstance;
    }

    private DotNetPlatform(){};

    public final static String getSaxonSaFullyQualifiedClassName() {
        return "com.saxonica.validate.SchemaAwareConfiguration, " +
                "saxon8sa, " +
                "Version=" + Version.getProductVersion() + ", " +
                "Culture=neutral, " +
                "PublicKeyToken=e1fdd002d5083fe6";
    }

    /**
     * Perform platform-specific initialization of the configuration
     */

    public void initialize(Configuration config) {
        config.setURIResolver(new DotNetURIResolver(new XmlUrlResolver()));
        config.setModuleURIResolver(new DotNetStandardModuleURIResolver(config, new XmlUrlResolver()));
        config.setExtensionFunctionFactory("clitype", new DotNetExtensionFunctionFactory(config));
        config.setExtensionFunctionFactory("java", new JavaExtensionFunctionFactory(config));
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
     * Construct an absolute URI from a relative URI and a base URI
     *
     * @param relativeURI the relative URI
     * @param base        the base URI
     * @return the absolutized URI
     * @throws java.net.URISyntaxException
     */

    public URI makeAbsolute(String relativeURI, String base) throws URISyntaxException {

        // It's not entirely clear why the .NET product needs a different version of this method.
        // Possibly because of bugs in GNU classpath.

        try {
            if (false) {
                // dummy code to allow the exception to be caught
                throw new cli.System.UriFormatException();
            }
            XmlUrlResolver resolver = new XmlUrlResolver();
            Uri fulluri;
            if (base != null) {
                Uri baseUri = new Uri(base);
                fulluri = resolver.ResolveUri(baseUri, relativeURI);
            }
            else {
                fulluri = resolver.ResolveUri(null, relativeURI.replaceAll("file:", ""));
            }
            return new URI(fulluri.ToString());
        } catch (cli.System.UriFormatException e) {
            throw new URISyntaxException(base + " + " + relativeURI, e.getMessage());
        }
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
     * Convert a StreamSource to either a SAXSource or a PullSource, depending on the native
     * parser of the selected platform
     *
     * @param config
     * @param input the supplied StreamSource
     * @param validation indicates whether schema validation is required, adn in what mode
     * @param dtdValidation
     * @param stripspace
     * @return the PullSource or SAXSource, initialized with a suitable parser, or the original
     * input Source, if now special handling is required or possible. This implementation
     * always returns either a PullSource or the original StreamSource.
     */

    public Source getParserSource(Configuration config, StreamSource input, int validation, boolean dtdValidation, int stripspace) {
        InputStream is = input.getInputStream();
        if (is != null) {
            if (is instanceof DotNetInputStream) {
                XmlReader parser = new XmlTextReader(input.getSystemId(),
                        ((DotNetInputStream)is).getUnderlyingStream());
                ((XmlTextReader)parser).set_WhitespaceHandling(WhitespaceHandling.wrap(WhitespaceHandling.All));
                ((XmlTextReader)parser).set_Normalization(true);
                
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
            if (reader instanceof DotNetReader) {
                XmlReader parser = new XmlTextReader(input.getSystemId(),
                        ((DotNetReader)reader).getUnderlyingTextReader());
                ((XmlTextReader)parser).set_Normalization(true);
                ((XmlTextReader)parser).set_WhitespaceHandling(WhitespaceHandling.wrap(WhitespaceHandling.All));

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
                Source r = config.getURIResolver().resolve(uri, null);
                if (r == null) {
                    return input;
                } else if (r instanceof AugmentedSource) {
                    Source r2 = ((AugmentedSource)r).getContainedSource();
                    if (r2 instanceof StreamSource) {
                        r2 = getParserSource(config, (StreamSource)r2, validation, dtdValidation, stripspace);
                        // TODO: preserve the r.pleaseCloseAfterUse() flag
                        return r2;
                    } else {
                        return r2;
                    }
                } else if (r instanceof StreamSource && r != input) {
                    return getParserSource(config, (StreamSource)r, validation, dtdValidation, stripspace);
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
     * Create a compiled regular expression
     * @param regex the source text of the regular expression, in XML Schema or XPath syntax
     * @param isXPath set to true if this is an XPath regular expression, false if it is XML Schema
     * @param flags the flags argument as supplied to functions such as fn:matches(), in string form
     * @throws XPathException if the syntax of the regular expression or flags is incorrect
     * @return the compiled regular expression
     */

    public RegularExpression compileRegularExpression(CharSequence regex, boolean isXPath, CharSequence flags)
    throws XPathException {
        return new DotNetRegularExpression(regex, isXPath, flags);
    }

    /**
     * Obtain a collation with a given set of properties. The set of properties is extensible
     * and variable across platforms. Common properties with example values include lang=ed-GB,
     * strength=primary, case-order=upper-first, ignore-modifiers=yes, alphanumeric=yes.
     * Properties that are not supported are generally ignored; however some errors, such as
     * failing to load a requested class, are fatal.
     * @param config the configuration object
     * @param props the desired properties of the collation
     * @param uri
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
        return c.getCollationKey(value, this);
    }

    /**
     * Make the default extension function factory appropriate to the platform
     */

    public FunctionLibrary makeExtensionLibrary(Configuration config) {
        return new DotNetExtensionLibrary(config);
    }

    /**
     * Add platform-specific function libraries to the function library list
     */

    public void addFunctionLibraries(FunctionLibraryList list, Configuration config) {
        list.addFunctionLibrary(new DotNetExtensionLibrary(config));
        list.addFunctionLibrary(new JavaExtensionLibrary(config));
        list.addFunctionLibrary(config.getExtensionBinder());
    }

    public void declareJavaClass(FunctionLibrary library, String uri, Class theClass) {
        throw new IllegalStateException("saxon:script cannot be used on .NET");
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
     */

    public cli.System.Type dynamicLoad(String uri, boolean debug) throws XPathException {
            if (uri.startsWith("clitype:")) {
                uri = uri.substring(8);
            } else {
                if (debug) {
                    System.err.println("Unrecognized .NET external URI: " + uri);
                }
                throw new StaticError("Unrecognized .NET external URI: " + uri);
            }
            String typeName;
            String queryParams;
            int q = uri.indexOf('?');
            if (q == 0 || q == uri.length()-1) {
                if (debug) {
                    System.err.println("Misplaced '?' in " + uri);
                }
                throw new StaticError("Misplaced '?' in " + uri);
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
                        if (false) throw new cli.System.TypeLoadException();
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
                        throw new StaticError("Bad keyword=value pair in " + kv);
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
                    if (false) throw new cli.System.IO.FileNotFoundException();
                    if (partialName != null) {
                        asm = Assembly.LoadWithPartialName(partialName);
                    } else if (loadFrom != null) {
                        asm = Assembly.LoadFrom(loadFrom);
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
                    throw new StaticError("Failed to load assembly " + uri + ": " + err.getMessage());
                } catch (Throwable err) {
                    if (debug) {
                        System.err.println("Failed to load assembly "  + uri + ": " + err.getMessage() +
                                " (" + err.getClass().getName() + ")");
                    }
                    throw new StaticError("Failed to load assembly " + uri + ": " + err.getMessage());
                }
                cli.System.Type type = asm.GetType(typeName);
                if (type == null) {
                    if (debug) {
                        System.err.println("Type " + typeName + " not found in assembly");
                    }
                    throw new StaticError("Type " + typeName + " not found in assembly");
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
// The Initial Developer of the Original Code is Michael H. Kay, based on code written by
// M David Peterson.
//
// Portions created by (your name) are Copyright (C) (your legal entity). All Rights Reserved.
//
// Contributor(s): none.
//


