package net.sf.saxon.lib;

import net.sf.saxon.Configuration;
import org.xml.sax.EntityResolver;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * This class is an EntityResolver used to resolve references to common
 * DTDs and entity files, using local copies provided with the Saxon product.
 * It has become necessary to do this because W3C is no longer serving
 * these files from its server. Ideally the job of caching these files
 * would belong to the XML parser, but because many of the parsers were
 * issued years ago, they cannot be relied on to do it.
 */
public class StandardEntityResolver implements EntityResolver {

    private static StandardEntityResolver THE_INSTANCE = new StandardEntityResolver();

    public static StandardEntityResolver getInstance() {
        return THE_INSTANCE;
    }

    private static HashMap<String, String> publicIds = new HashMap<String, String>(30);
    private static HashMap<String, String> systemIds = new HashMap<String, String>(30);

    public boolean tracing = false;
    public PrintStream traceDestination = null;

    /**
     * Register a DTD or other entity to be resolved by this
     * entity resolver
     * @param publicId the public identifier of the DTD or entity
     * @param systemId the system identifier of the DTD or entity
     * @param fileName the fileName of the Saxon local copy of the
     * resource, relative to the data directory in the JAR file
     */

    public static void register(
            /*@Nullable*/ String publicId,
            String systemId,
            String fileName) {
        if (publicId != null) {
            publicIds.put(publicId, fileName);
        }
        if (systemId != null) {
            systemIds.put(systemId, fileName);
        }
    }

    static {

        register("-//W3C//ENTITIES Latin 1 for XHTML//EN",
                 "http://www.w3.org/TR/xhtml1/DTD/xhtml-lat1.ent",
                 "w3c/xhtml-lat1.ent");
        register("-//W3C//ENTITIES Symbols for XHTML//EN",
                 "http://www.w3.org/TR/xhtml1/DTD/xhtml-symbol.ent",
                 "w3c/xhtml-symbol.ent");
        register("-//W3C//ENTITIES Special for XHTML//EN",
                 "http://www.w3.org/TR/xhtml1/DTD/xhtml-special.ent",
                 "w3c/xhtml-special.ent");
        register("-//W3C//DTD XHTML 1.0 Transitional//EN",
                 "http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd",
                 "w3c/xhtml10/xhtml1-transitional.dtd");
        register("-//W3C//DTD XHTML 1.0 Strict//EN",
                 "http://www.w3.org/TR/xhtml1/DTD/xhtml1-strict.dtd",
                 "w3c/xhtml10/xhtml1-strict.dtd");
        register("-//W3C//DTD XHTML 1.0 Frameset//EN",
                 "http://www.w3.org/TR/xhtml1/DTD/xhtml1-frameset.dtd",
                 "w3c/xhtml10/xhtml1-frameset.dtd");
        register("-//W3C//DTD XHTML Basic 1.0//EN",
                 "http://www.w3.org/TR/xhtml-basic/xhtml-basic10.dtd",
                 "w3c/xhtml10/xhtml-basic10.dtd");
        register("-//W3C//DTD XHTML 1.1//EN",
                 "http://www.w3.org/MarkUp/DTD/xhtml11.dtd",
                 "w3c/xhtml11/xhtml11.dtd");
        register("-//W3C//DTD XHTML Basic 1.1//EN",
                 "http://www.w3.org/MarkUp/DTD/xhtml-basic11.dtd",
                 "w3c/xhtml11/xhtml-basic11.dtd");
        register("-//W3C//ELEMENTS XHTML Access Element 1.0//EN",
                 "http://www.w3.org/MarkUp/DTD/xhtml-access-1.mod",
                 "w3c/xhtml11/xhtml-access-1.mod");
        register("-//W3C//ENTITIES XHTML Access Attribute Qnames 1.0//EN",
                 "http://www.w3.org/MarkUp/DTD/xhtml-access-qname-1.mod",
                 "w3c/xhtml11/xhtml-access-qname-1.mod");
        register("-//W3C//ELEMENTS XHTML Java Applets 1.0//EN",
                 "http://www.w3.org/MarkUp/DTD/xhtml-applet-1.mod",
                 "w3c/xhtml11/xhtml-applet-1.mod");
        register("-//W3C//ELEMENTS XHTML Base Architecture 1.0//EN",
                 "http://www.w3.org/MarkUp/DTD/xhtml-arch-1.mod",
                 "w3c/xhtml11/xhtml-arch-1.mod");
        register("-//W3C//ENTITIES XHTML Common Attributes 1.0//EN",
                 "http://www.w3.org/MarkUp/DTD/xhtml-attribs-1.mod",
                 "w3c/xhtml11/xhtml-attribs-1.mod");
        register("-//W3C//ELEMENTS XHTML Base Element 1.0//EN",
                 "http://www.w3.org/MarkUp/DTD/xhtml-base-1.mod",
                 "w3c/xhtml11/xhtml-base-1.mod");
        register("-//W3C//ELEMENTS XHTML Basic Forms 1.0//EN",
                 "http://www.w3.org/MarkUp/DTD/xhtml-basic-form-1.mod",
                 "w3c/xhtml11/xhtml-basic-form-1.mod");
        register("-//W3C//ELEMENTS XHTML Basic Tables 1.0//EN",
                 "http://www.w3.org/MarkUp/DTD/xhtml-basic-table-1.mod",
                 "w3c/xhtml11/xhtml-basic-table-1.mod");
        register("-//W3C//ENTITIES XHTML Basic 1.0 Document Model 1.0//EN",
                 "http://www.w3.org/MarkUp/DTD/xhtml-basic10-model-1.mod",
                 "w3c/xhtml11/xhtml-basic10-model-1.mod");
        register("-//W3C//ENTITIES XHTML Basic 1.1 Document Model 1.0//EN",
                 "http://www.w3.org/MarkUp/DTD/xhtml-basic11-model-1.mod",
                 "w3c/xhtml11/xhtml-basic11-model-1.mod");
        register("-//W3C//ELEMENTS XHTML BDO Element 1.0//EN",
                 "http://www.w3.org/MarkUp/DTD/xhtml-bdo-1.mod",
                 "w3c/xhtml11/xhtml-bdo-1.mod");
        register("-//W3C//ELEMENTS XHTML Block Phrasal 1.0//EN",
                 "http://www.w3.org/MarkUp/DTD/xhtml-blkphras-1.mod",
                 "w3c/xhtml11/xhtml-blkphras-1.mod");
        register("-//W3C//ELEMENTS XHTML Block Presentation 1.0//EN",
                 "http://www.w3.org/MarkUp/DTD/xhtml-blkpres-1.mod",
                 "w3c/xhtml11/xhtml-blkpres-1.mod");
        register("-//W3C//ELEMENTS XHTML Block Structural 1.0//EN",
                 "http://www.w3.org/MarkUp/DTD/xhtml-blkstruct-1.mod",
                 "w3c/xhtml11/xhtml-blkstruct-1.mod");
        register("-//W3C//ENTITIES XHTML Character Entities 1.0//EN",
                 "http://www.w3.org/MarkUp/DTD/xhtml-charent-1.mod",
                 "w3c/xhtml11/xhtml-charent-1.mod");
        register("-//W3C//ELEMENTS XHTML Client-side Image Maps 1.0//EN",
                 "http://www.w3.org/MarkUp/DTD/xhtml-csismap-1.mod",
                 "w3c/xhtml11/xhtml-csismap-1.mod");
        register("-//W3C//ENTITIES XHTML Datatypes 1.0//EN",
                 "http://www.w3.org/MarkUp/DTD/xhtml-datatypes-1.mod",
                 "w3c/xhtml11/xhtml-datatypes-1.mod");
        register("-//W3C//ELEMENTS XHTML Editing Markup 1.0//EN",
                 "http://www.w3.org/MarkUp/DTD/xhtml-edit-1.mod",
                 "w3c/xhtml11/xhtml-edit-1.mod");
        register("-//W3C//ENTITIES XHTML Intrinsic Events 1.0//EN",
                 "http://www.w3.org/MarkUp/DTD/xhtml-events-1.mod",
                 "w3c/xhtml11/xhtml-events-1.mod");
        register("-//W3C//ELEMENTS XHTML Forms 1.0//EN",
                 "http://www.w3.org/MarkUp/DTD/xhtml-form-1.mod",
                 "w3c/xhtml11/xhtml-form-1.mod");
        register("-//W3C//ELEMENTS XHTML Frames 1.0//EN",
                 "http://www.w3.org/MarkUp/DTD/xhtml-frames-1.mod",
                 "w3c/xhtml11/xhtml-frames-1.mod");
        register("-//W3C//ENTITIES XHTML Modular Framework 1.0//EN",
                 "http://www.w3.org/MarkUp/DTD/xhtml-framework-1.mod",
                 "w3c/xhtml11/xhtml-framework-1.mod");
        register("-//W3C//ENTITIES XHTML HyperAttributes 1.0//EN",
                 "http://www.w3.org/MarkUp/DTD/xhtml-hyperAttributes-1.mod",
                 "w3c/xhtml11/xhtml-hyperAttributes-1.mod");
        register("-//W3C//ELEMENTS XHTML Hypertext 1.0//EN",
                 "http://www.w3.org/MarkUp/DTD/xhtml-hypertext-1.mod",
                 "w3c/xhtml11/xhtml-hypertext-1.mod");
        register("-//W3C//ELEMENTS XHTML Inline Frame Element 1.0//EN",
                 "http://www.w3.org/MarkUp/DTD/xhtml-iframe-1.mod",
                 "w3c/xhtml11/xhtml-iframe-1.mod");
        register("-//W3C//ELEMENTS XHTML Images 1.0//EN",
                 "http://www.w3.org/MarkUp/DTD/xhtml-image-1.mod",
                 "w3c/xhtml11/xhtml-image-1.mod");
        register("-//W3C//ELEMENTS XHTML Inline Phrasal 1.0//EN",
                 "http://www.w3.org/MarkUp/DTD/xhtml-inlphras-1.mod",
                 "w3c/xhtml11/xhtml-inlphras-1.mod");
        register("-//W3C//ELEMENTS XHTML Inline Presentation 1.0//EN",
                 "http://www.w3.org/MarkUp/DTD/xhtml-inlpres-1.mod",
                 "xhtml11/xhtml-inlpres-1.mod");
        register("-//W3C//ELEMENTS XHTML Inline Structural 1.0//EN",
                 "http://www.w3.org/MarkUp/DTD/xhtml-inlstruct-1.mod",
                 "w3c/xhtml11/xhtml-inlstruct-1.mod");
        register("-//W3C//ENTITIES XHTML Inline Style 1.0//EN",
                 "http://www.w3.org/MarkUp/DTD/xhtml-inlstyle-1.mod",
                 "w3c/xhtml11/xhtml-inlstyle-1.mod");
        register("-//W3C//ELEMENTS XHTML Inputmode 1.0//EN",
                 "http://www.w3.org/MarkUp/DTD/xhtml-inputmode-1.mod",
                 "w3c/xhtml11/xhtml-inputmode-1.mod");
        register("-//W3C//ELEMENTS XHTML Legacy Markup 1.0//EN",
                 "http://www.w3.org/MarkUp/DTD/xhtml-legacy-1.mod",
                 "w3c/xhtml11/xhtml-legacy-1.mod");
        register("-//W3C//ELEMENTS XHTML Legacy Redeclarations 1.0//EN",
                 "http://www.w3.org/MarkUp/DTD/xhtml-legacy-redecl-1.mod",
                 "w3c/xhtml11/xhtml-legacy-redecl-1.mod");
        register("-//W3C//ELEMENTS XHTML Link Element 1.0//EN",
                 "http://www.w3.org/MarkUp/DTD/xhtml-link-1.mod",
                 "w3c/xhtml11/xhtml-link-1.mod");
        register("-//W3C//ELEMENTS XHTML Lists 1.0//EN",
                 "http://www.w3.org/MarkUp/DTD/xhtml-list-1.mod",
                 "w3c/xhtml11/xhtml-list-1.mod");
        register("-//W3C//ELEMENTS XHTML Metainformation 1.0//EN",
                 "http://www.w3.org/MarkUp/DTD/xhtml-meta-1.mod",
                 "w3c/xhtml11/xhtml-meta-1.mod");
        register("-//W3C//ELEMENTS XHTML Metainformation 2.0//EN",
                 "http://www.w3.org/MarkUp/DTD/xhtml-meta-2.mod",
                 "w3c/xhtml11/xhtml-meta-2.mod");
        register("-//W3C//ENTITIES XHTML MetaAttributes 1.0//EN",
                 "http://www.w3.org/MarkUp/DTD/xhtml-metaAttributes-1.mod",
                 "w3c/xhtml11/xhtml-metaAttributes-1.mod");
        register("-//W3C//ELEMENTS XHTML Name Identifier 1.0//EN",
                 "http://www.w3.org/MarkUp/DTD/xhtml-nameident-1.mod",
                 "w3c/xhtml11/xhtml-nameident-1.mod");
        register("-//W3C//NOTATIONS XHTML Notations 1.0//EN",
                 "http://www.w3.org/MarkUp/DTD/xhtml-notations-1.mod",
                 "w3c/xhtml11/xhtml-notations-1.mod");
        register("-//W3C//ELEMENTS XHTML Embedded Object 1.0//EN",
                 "http://www.w3.org/MarkUp/DTD/xhtml-object-1.mod",
                 "w3c/xhtml11/xhtml-object-1.mod");
        register("-//W3C//ELEMENTS XHTML Param Element 1.0//EN",
                 "http://www.w3.org/MarkUp/DTD/xhtml-param-1.mod",
                 "w3c/xhtml11/xhtml-param-1.mod");
        register("-//W3C//ELEMENTS XHTML Presentation 1.0//EN",
                 "http://www.w3.org/MarkUp/DTD/xhtml-pres-1.mod",
                 "w3c/xhtml11/xhtml-pres-1.mod");
        register("-//W3C//ENTITIES XHTML-Print 1.0 Document Model 1.0//EN",
                 "http://www.w3.org/MarkUp/DTD/xhtml-print10-model-1.mod",
                 "w3c/xhtml11/xhtml-print10-model-1.mod");
        register("-//W3C//ENTITIES XHTML Qualified Names 1.0//EN",
                 "http://www.w3.org/MarkUp/DTD/xhtml-qname-1.mod",
                 "w3c/xhtml11/xhtml-qname-1.mod");
        register("-//W3C//ENTITIES XHTML+RDFa Document Model 1.0//EN",
                 "http://www.w3.org/MarkUp/DTD/xhtml-rdfa-model-1.mod",
                 "w3c/xhtml11/xhtml-rdfa-model-1.mod");
        register("-//W3C//ENTITIES XHTML RDFa Attribute Qnames 1.0//EN",
                 "http://www.w3.org/MarkUp/DTD/xhtml-rdfa-qname-1.mod",
                 "w3c/xhtml11/xhtml-rdfa-qname-1.mod");
        register("-//W3C//ENTITIES XHTML Role Attribute 1.0//EN",
                 "http://www.w3.org/MarkUp/DTD/xhtml-role-1.mod",
                 "w3c/xhtml11/xhtml-role-1.mod");
        register("-//W3C//ENTITIES XHTML Role Attribute Qnames 1.0//EN",
                 "http://www.w3.org/MarkUp/DTD/xhtml-role-qname-1.mod",
                 "w3c/xhtml11/xhtml-role-qname-1.mod");
        register("-//W3C//ELEMENTS XHTML Ruby 1.0//EN",
                 "http://www.w3.org/TR/ruby/xhtml-ruby-1.mod",
                 "w3c/xhtml11/xhtml-ruby-1.mod");
        register("-//W3C//ELEMENTS XHTML Scripting 1.0//EN",
                 "http://www.w3.org/MarkUp/DTD/xhtml-script-1.mod",
                 "w3c/xhtml11/xhtml-script-1.mod");
        register("-//W3C//ELEMENTS XHTML Server-side Image Maps 1.0//EN",
                 "http://www.w3.org/MarkUp/DTD/xhtml-ssismap-1.mod",
                 "w3c/xhtml11/xhtml-ssismap-1.mod");
        register("-//W3C//ELEMENTS XHTML Document Structure 1.0//EN",
                 "http://www.w3.org/MarkUp/DTD/xhtml-struct-1.mod",
                 "w3c/xhtml11/xhtml-struct-1.mod");
        register("-//W3C//DTD XHTML Style Sheets 1.0//EN",
                 "http://www.w3.org/MarkUp/DTD/xhtml-style-1.mod",
                 "w3c/xhtml11/xhtml-style-1.mod");
        register("-//W3C//ELEMENTS XHTML Tables 1.0//EN",
                 "http://www.w3.org/MarkUp/DTD/xhtml-table-1.mod",
                 "w3c/xhtml11/xhtml-table-1.mod");
        register("-//W3C//ELEMENTS XHTML Target 1.0//EN",
                 "http://www.w3.org/MarkUp/DTD/xhtml-target-1.mod",
                 "w3c/xhtml11/xhtml-target-1.mod");
        register("-//W3C//ELEMENTS XHTML Text 1.0//EN",
                 "http://www.w3.org/MarkUp/DTD/xhtml-text-1.mod",
                 "w3c/xhtml11/xhtml-text-1.mod");
        register("-//W3C//ENTITIES XHTML 1.1 Document Model 1.0//EN",
                 "http://www.w3.org/MarkUp/DTD/xhtml11-model-1.mod",
                 "w3c/xhtml11/xhtml11-model-1.mod");
        register("-//W3C//MathML 1.0//EN",
                 "http://www.w3.org/Math/DTD/mathml1/mathml.dtd",
                 "w3c/mathml/mathml1/mathml.dtd");
        register("-//W3C//DTD MathML 2.0//EN",
                 "http://www.w3.org/Math/DTD/mathml2/mathml2.dtd",
                 "w3c/mathml/mathml2/mathml2.dtd");
        register("-//W3C//DTD MathML 3.0//EN",
                 "http://www.w3.org/Math/DTD/mathml3/mathml3.dtd",
                 "w3c/mathml/mathml3/mathml3.dtd");
        register("-//W3C//DTD SVG 1.0//EN",
                 "http://www.w3.org/TR/2001/REC-SVG-20010904/DTD/svg10.dtd",
                 "w3c/svg10/svg10.dtd");
        register("-//W3C//DTD SVG 1.1//EN",
                 "http://www.w3.org/Graphics/SVG/1.1/DTD/svg11.dtd",
                 "w3c/svg11/svg11.dtd");
        register("-//W3C//DTD SVG 1.1 Tiny//EN",
                 "http://www.w3.org/Graphics/SVG/1.1/DTD/svg11-tiny.dtd",
                 "w3c/svg11/svg11-tiny.dtd");
        register("-//W3C//DTD SVG 1.1 Basic//EN",
                 "http://www.w3.org/Graphics/SVG/1.1/DTD/svg11-basic.dtd",
                 "w3c/svg11/svg11-basic.dtd");
        register("-//XML-DEV//ENTITIES RDDL Document Model 1.0//EN",
                 "http://www.rddl.org/xhtml-rddl-model-1.mod",
                 "w3c/rddl/xhtml-rddl-model-1.mod");
        register("-//XML-DEV//DTD XHTML RDDL 1.0//EN",
                 "http://www.rddl.org/rddl-xhtml.dtd",
                 "w3c/rddl/rddl-xhtml.dtd");
        register("-//XML-DEV//ENTITIES RDDL QName Module 1.0//EN",
                 "http://www.rddl.org/rddl-qname-1.mod",
                 "rddl/rddl-qname-1.mod");
        register("-//XML-DEV//ENTITIES RDDL Resource Module 1.0//EN",
                 "http://www.rddl.org/rddl-resource-1.mod",
                 "rddl/rddl-resource-1.mod");
        register("-//W3C//DTD Specification V2.10//EN",
                 "http://www.w3.org/2002/xmlspec/dtd/2.10/xmlspec.dtd",
                 "w3c/xmlspec/xmlspec.dtd");
        register("-//W3C//DTD XMLSCHEMA 200102//EN",
                 "http://www.w3.org/2001/XMLSchema.dtd",
                 "w3c/xmlschema/XMLSchema.dtd");
    }

    /**
     * Set configuration details. This is used to control tracing of accesses to files
     * @param config the Saxon configuration
     */

    public void setConfiguration(Configuration config) {
        tracing = config.isTiming();
        traceDestination = config.getStandardErrorOutput();
    }

    /**
     * Allow the application to resolve external entities.
     * <p/>
     * <p>The parser will call this method before opening any external
     * entity except the top-level document entity.  Such entities include
     * the external DTD subset and external parameter entities referenced
     * within the DTD (in either case, only if the parser reads external
     * parameter entities), and external general entities referenced
     * within the document element (if the parser reads external general
     * entities).  The application may request that the parser locate
     * the entity itself, that it use an alternative URI, or that it
     * use data provided by the application (as a character or byte
     * input stream).</p>
     * <p/>
     * <p>Application writers can use this method to redirect external
     * system identifiers to secure and/or local URIs, to look up
     * public identifiers in a catalogue, or to read an entity from a
     * database or other input source (including, for example, a dialog
     * box).  Neither XML nor SAX specifies a preferred policy for using
     * public or system IDs to resolve resources.  However, SAX specifies
     * how to interpret any InputSource returned by this method, and that
     * if none is returned, then the system ID will be dereferenced as
     * a URL.  </p>
     * <p/>
     * <p>If the system identifier is a URL, the SAX parser must
     * resolve it fully before reporting it to the application.</p>
     *
     * @param publicId The public identifier of the external entity
     *                 being referenced, or null if none was supplied.
     * @param systemId The system identifier of the external entity
     *                 being referenced.
     * @return An InputSource object describing the new input source,
     *         or null to request that the parser open a regular
     *         URI connection to the system identifier.
     * @throws org.xml.sax.SAXException Any SAX exception, possibly
     *                                  wrapping another exception.
     * @throws java.io.IOException      A Java-specific IO exception,
     *                                  possibly the result of creating a new InputStream
     *                                  or Reader for the InputSource.
     * @see org.xml.sax.InputSource
     */
    public InputSource resolveEntity(String publicId, String systemId) throws SAXException, IOException {
        // See if it's a known public ID
        String fileName = publicIds.get(publicId);
        if (fileName != null) {
            return fetch(fileName);
        }

        // See if it's a known system ID
        fileName = systemIds.get(systemId);
        if (fileName != null) {
            return fetch(fileName);
        }

        // Otherwise, leave the parser to resolve the URI in the normal way
        return null;
    }

    private InputSource fetch(String filename) {
        if (tracing) {
            if (traceDestination == null) {
                traceDestination = System.err;
            }
            traceDestination.println("Fetching Saxon copy of " + filename);
        }
        List<String> messages = new ArrayList<String>();
        List<ClassLoader> classLoaders = new ArrayList<ClassLoader>();
        InputStream in = Configuration.locateResource(filename, messages, classLoaders);
        if (tracing) {
            for (String s : messages) {
                traceDestination.println(s);
            }
        }
        if (in == null) {
            return null;
        }
        return new InputSource(in);
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