package net.sf.saxon.serialize;

import net.sf.saxon.event.ReceiverOptions;
import net.sf.saxon.lib.SaxonOutputKeys;
import net.sf.saxon.om.NodeName;
import net.sf.saxon.trans.XPathException;
import net.sf.saxon.tree.tiny.CompressedWhitespace;
import net.sf.saxon.type.SchemaType;

import javax.xml.transform.OutputKeys;

/**
  * This class generates HTML output
  * @author Michael H. Kay
  */

public class HTMLEmitter extends XMLEmitter {

	/**
	* Preferred character representations
	*/

    private static final int REP_NATIVE = 0;
	private static final int REP_ENTITY = 1;
	private static final int REP_DECIMAL = 2;
	private static final int REP_HEX = 3;

	private int nonASCIIRepresentation = REP_NATIVE;
	private int excludedRepresentation = REP_ENTITY;

	private int inScript;
    private int version = 4;
	private String parentElement;
    private String uri;
    private boolean escapeNonAscii = false;

	/**
	 * Decode preferred representation
     * @param rep string containing preferred representation (native, entity, decimal, or hex)
     * @return integer code for the preferred representation
	*/

	private static int representationCode(String rep) {
		if (rep.equalsIgnoreCase("native")) return REP_NATIVE;
		if (rep.equalsIgnoreCase("entity")) return REP_ENTITY;
		if (rep.equalsIgnoreCase("decimal")) return REP_DECIMAL;
		if (rep.equalsIgnoreCase("hex")) return REP_HEX;
		return REP_ENTITY;
	}

    /**
    * Table of HTML tags that have no closing tag
    */

    static HTMLTagHashSet emptyTags = new HTMLTagHashSet(31);

    static {
        setEmptyTag("area");
        setEmptyTag("base");
        setEmptyTag("basefont");
        setEmptyTag("br");
        setEmptyTag("col");
        setEmptyTag("frame");
        setEmptyTag("hr");
        setEmptyTag("img");
        setEmptyTag("input");
        setEmptyTag("isindex");
        setEmptyTag("link");
        setEmptyTag("meta");
        setEmptyTag("param");
    }

    private static void setEmptyTag(String tag) {
        emptyTags.add(tag);
    }

    protected static boolean isEmptyTag(String tag) {
        return emptyTags.contains(tag);
    }

    /**
    * Table of boolean attributes
    */

    // we use two HashMaps to avoid unnecessary string concatenations

    private static HTMLTagHashSet booleanAttributes = new HTMLTagHashSet(31);
    private static HTMLTagHashSet booleanCombinations = new HTMLTagHashSet(53);

    static {
        setBooleanAttribute("area", "nohref");
        setBooleanAttribute("button", "disabled");
        setBooleanAttribute("dir", "compact");
        setBooleanAttribute("dl", "compact");
        setBooleanAttribute("frame", "noresize");
        setBooleanAttribute("hr", "noshade");
        setBooleanAttribute("img", "ismap");
        setBooleanAttribute("input", "checked");
        setBooleanAttribute("input", "disabled");
        setBooleanAttribute("input", "readonly");
        setBooleanAttribute("menu", "compact");
        setBooleanAttribute("object", "declare");
        setBooleanAttribute("ol", "compact");
        setBooleanAttribute("optgroup", "disabled");
        setBooleanAttribute("option", "selected");
        setBooleanAttribute("option", "disabled");
        setBooleanAttribute("script", "defer");
        setBooleanAttribute("select", "multiple");
        setBooleanAttribute("select", "disabled");
        setBooleanAttribute("td", "nowrap");
        setBooleanAttribute("textarea", "disabled");
        setBooleanAttribute("textarea", "readonly");
        setBooleanAttribute("th", "nowrap");
        setBooleanAttribute("ul", "compact");
    }

    private static void setBooleanAttribute(String element, String attribute) {
        booleanAttributes.add(attribute);
        booleanCombinations.add(element + '+' + attribute);
    }

    private static boolean isBooleanAttribute(String element, String attribute, String value) {
        return attribute.equalsIgnoreCase(value) &&
                booleanAttributes.contains(attribute) &&
                booleanCombinations.contains(element + '+' + attribute);
    }

    /**
    * Constructor
    */

    public HTMLEmitter() {

    }

    /**
     * Say that all non-ASCII characters should be escaped, regardless of the character encoding
     * @param escape true if all non ASCII characters should be escaped
     */

    public void setEscapeNonAscii(Boolean escape) {
        escapeNonAscii = escape;
    }

    /**
    * Output start of document
    */

    public void open() throws XPathException {}

    protected void openDocument() throws XPathException {
        if (writer==null) {
            makeWriter();
        }
        if (started) {
            return;
        }
        started = true;
            // This method is sometimes called twice, especially during an identity transform
            // This check stops two DOCTYPE declarations being output.

        String versionProperty = outputProperties.getProperty(OutputKeys.VERSION);
        if (versionProperty != null) {
            if (versionProperty.equals("4.0") || versionProperty.equals("4.01")) {
                version = 4;
            } else if (versionProperty.equals("5.0")) {
                version = 5;
            } else {
                XPathException err = new XPathException("Unsupported HTML version: " + versionProperty);
                err.setErrorCode("SESU0013");
                throw err;
            }
        }

        String byteOrderMark = outputProperties.getProperty(SaxonOutputKeys.BYTE_ORDER_MARK);

        if ("yes".equals(byteOrderMark) &&
                "UTF-8".equalsIgnoreCase(outputProperties.getProperty(OutputKeys.ENCODING))) {
            try {
                writer.write('\uFEFF');
            } catch (java.io.IOException err) {
                // Might be an encoding exception; just ignore it
            }
        }

        String systemId = outputProperties.getProperty(OutputKeys.DOCTYPE_SYSTEM);
        String publicId = outputProperties.getProperty(OutputKeys.DOCTYPE_PUBLIC);

        // Treat "" as equivalent to absent. This goes beyond what the spec strictly allows.
        if ("".equals(systemId)) {
            systemId = null;
        }
        if ("".equals(publicId)) {
            publicId = null;
        }
        if (systemId!=null || publicId!=null || version==5) {
            writeDocType("html", systemId, publicId);
        }

        inScript = -1000000;
    }

    /**
     * Output the document type declaration
     * @param type     The element name
     * @param systemId The DOCTYP system identifier
     * @param publicId The DOCTYPE public identifier
     */

    protected void writeDocType(String type, String systemId, String publicId) throws XPathException {
        if (version == 5) {
            try {
                writer.write("<!DOCTYPE HTML>\n");
            } catch (java.io.IOException err) {
                throw new XPathException(err);
            }
        } else {
            super.writeDocType(type, systemId, publicId);
        }
    }

    /**
    * Output element start tag
    */

    public void startElement(NodeName elemName, SchemaType typeCode, int locationId, int properties) throws XPathException {

        super.startElement(elemName, typeCode, locationId, properties);
		uri = elemName.getURI();
        parentElement = (String)elementStack.peek();
        if (elemName.isInNamespace("") &&
                (   parentElement.equalsIgnoreCase("script") ||
                    parentElement.equalsIgnoreCase("style"))) {
            inScript = 0;
        }
        inScript++;
    }

    public void startContent() throws XPathException {
        closeStartTag();                   // prevent <xxx/> syntax
    }

    /**
    * Write attribute name=value pair. Overrides the XML behaviour if the name and value
    * are the same (we assume this is a boolean attribute to be minimised), or if the value is
    * a URL.
    */

    protected void writeAttribute(NodeName elCode, String attname, CharSequence value, int properties) throws XPathException {
        try {
            if (uri.length()==0) {
                if (isBooleanAttribute(parentElement, attname, value.toString())) {
                    writer.write(attname);
                    return;
                }
            }
            super.writeAttribute(elCode, attname, value, properties);
        } catch (java.io.IOException err) {
            throw new XPathException(err);
        }
    }


    /**
    * Escape characters. Overrides the XML behaviour
    */

    protected void writeEscape(final CharSequence chars, final boolean inAttribute)
    throws java.io.IOException, XPathException {

        int segstart = 0;
        final boolean[] specialChars = (inAttribute ? specialInAtt : specialInText);

        if (chars instanceof CompressedWhitespace) {
            ((CompressedWhitespace)chars).writeEscape(specialChars, writer);
            return;
        }
        boolean disabled = false;

        while (segstart < chars.length()) {
            int i = segstart;

            // find a maximal sequence of "ordinary" characters

            if (escapeNonAscii) {
                char c;
                while (i < chars.length() && (c = chars.charAt(i)) < 127 && !specialChars[c]) {
                    i++;
                }
            } else {
                char c;
                while (i < chars.length() &&
                        ((c = chars.charAt(i)) < 127 ? !specialChars[c] : (characterSet.inCharset(c) && c > 160)
     				 )
     			  ) {
                    i++;
                }
            }

            // if this was the whole string, output the string and quit

            if (i == chars.length()) {
                if (segstart == 0) {
                    writeCharSequence(chars);
                } else {
                    writeCharSequence(chars.subSequence(segstart, i));
                }
                return;
            }

            // otherwise, output this sequence and continue
            if (i > segstart) {
                writeCharSequence(chars.subSequence(segstart, i));
            }

            final char c = chars.charAt(i);
            if (c==0) {
                // used to switch escaping on and off
                disabled = !disabled;
            } else if (disabled) {
                writer.write(c);
            } else if (c<=127) {
                // handle a special ASCII character
                if (inAttribute) {
                    if (c=='<') {
                        writer.write('<');      // not escaped
                    } else if (c=='>') {
                        writer.write("&gt;");   // recommended for older browsers
                    } else if (c=='&') {
                        if (i+1<chars.length() && chars.charAt(i+1)=='{') {
                            writer.write('&');                   // not escaped if followed by '{'
                        } else {
                            writer.write("&amp;");
                        }
                    } else if (c=='\"') {
                        writer.write("&#34;");
                    } else if (c=='\n') {
                        writer.write("&#xA;");
                    } else if (c=='\t') {
                        writer.write("&#x9;");
                    } else if (c=='\r') {
                        writer.write("&#xD;");
                    }
                } else {
                    if (c=='<') {
                        writer.write("&lt;");
                    } else if (c=='>') {
                        writer.write("&gt;");  // changed to allow for "]]>"
                    } else if (c=='&') {
                        writer.write("&amp;");
                    } else if (c=='\r') {
                        writer.write("&#xD;");
                    }
                }

            } else if (c < 160) {
                // these control characters are illegal in HTML
                XPathException err = new XPathException("Illegal HTML character: decimal " + (int)c);
                err.setErrorCode("SERE0014");
                throw err;

            } else if (c==160) {
        		// always output NBSP as an entity reference
            	writer.write("&nbsp;");

            } else if (c>=55296 && c<=56319) {  //handle surrogate pair

                //A surrogate pair is two consecutive Unicode characters.  The first
                //is in the range D800 to DBFF, the second is in the range DC00 to DFFF.
                //To compute the numeric value of the character corresponding to a surrogate
                //pair, use this formula (all numbers are hex):
        	    //(FirstChar - D800) * 400 + (SecondChar - DC00) + 10000

                    // we'll trust the data to be sound
                int charval = (((int)c - 55296) * 1024) + ((int)chars.charAt(i+1) - 56320) + 65536;
                characterReferenceGenerator.outputCharacterReference(charval, writer);
                i++;

            } else if (escapeNonAscii || !characterSet.inCharset(c)) {
                characterReferenceGenerator.outputCharacterReference(c, writer);
            } else {
                writer.write(c);
            }
            segstart = ++i;
        }

    }

    /**
    * Output an element end tag.
    */

    public void endElement() throws XPathException {
        String name = (String)elementStack.peek();
        inScript--;
        if (inScript==0) {
            inScript = -1000000;
        }

        if (isEmptyTag(name) && uri.length()==0) {
            // no end tag required
            elementStack.pop();
        } else {
            super.endElement();
        }

    }

    /**
    * Character data.
    */

    public void characters (CharSequence chars, int locationId, int properties)
    throws XPathException {
        int options = properties;
        if (inScript>0) {
            options |= ReceiverOptions.DISABLE_ESCAPING;
        }
        super.characters(chars, locationId, options);
    }

    /**
    * Handle a processing instruction.
    */

    public void processingInstruction (String target, CharSequence data, int locationId, int properties)
        throws XPathException
    {
        if (!started) {
            openDocument();
        }
        for (int i=0; i<data.length(); i++) {
            if (data.charAt(i) == '>') {
                XPathException err = new XPathException("A processing instruction in HTML must not contain a > character");
                err.setErrorCode("SERE0015");
                throw err;
            }
        }
        try {
            writer.write("<?");
            writer.write(target);
            writer.write(' ');
            writeCharSequence(data);
            writer.write('>');
        } catch (java.io.IOException err) {
            throw new XPathException(err);
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