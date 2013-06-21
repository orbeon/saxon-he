package net.sf.saxon.serialize;

import net.sf.saxon.event.ReceiverOptions;
import net.sf.saxon.lib.SaxonOutputKeys;
import net.sf.saxon.om.NamespaceBinding;
import net.sf.saxon.om.NodeName;
import net.sf.saxon.serialize.charcode.UTF8CharacterSet;
import net.sf.saxon.trans.XPathException;
import net.sf.saxon.type.SchemaType;
import net.sf.saxon.type.SimpleType;

import javax.xml.transform.OutputKeys;

/**
  * This class generates TEXT output
  * @author Michael H. Kay
  */

public class TEXTEmitter extends XMLEmitter {

    /**
    * Start of the document.
    */

    public void open () throws XPathException  {}

    protected void openDocument() throws XPathException {

        if (writer==null) {
            makeWriter();
        }
        if (characterSet==null) {
            characterSet = UTF8CharacterSet.getInstance();
        }
        // Write a BOM if requested
        String encoding = outputProperties.getProperty(OutputKeys.ENCODING);
        if (encoding==null || encoding.equalsIgnoreCase("utf8")) {
            encoding = "UTF-8";
        }
        String byteOrderMark = outputProperties.getProperty(SaxonOutputKeys.BYTE_ORDER_MARK);

        if ("yes".equals(byteOrderMark) && (
                "UTF-8".equalsIgnoreCase(encoding) ||
                    "UTF-16LE".equalsIgnoreCase(encoding) ||
                    "UTF-16BE".equalsIgnoreCase(encoding))) {
            try {
                writer.write('\uFEFF');
            } catch (java.io.IOException err) {
                // Might be an encoding exception; just ignore it
            }
        }
        started = true;
    }

    /**
    * Output the XML declaration. This implementation does nothing.
    */

    public void writeDeclaration() throws XPathException {}

    /**
    * Produce output using the current Writer. <BR>
    * Special characters are not escaped.
    * @param chars Character sequence to be output
    * @param properties bit fields holding special properties of the characters
    * @exception XPathException for any failure
    */

    public void characters(CharSequence chars, int locationId, int properties) throws XPathException {
        if (!started) {
            openDocument();
        }
        if ((properties & ReceiverOptions.NO_SPECIAL_CHARS) == 0) {
            int badchar = testCharacters(chars);
            if (badchar != 0) {
                throw new XPathException(
                        "Output character not available in this encoding (decimal " + badchar + ")");
            }
        }
        try {
            writer.write(chars.toString());
        } catch (java.io.IOException err) {
            throw new XPathException(err);
        }
    }

    /**
    * Output an element start tag. <br>
    * Does nothing with this output method.
     * @param elemName The element name (tag)
      * @param typeCode The type annotation
     * @param properties Bit fields holding any special properties of the element
     */

    public void startElement(NodeName elemName, SchemaType typeCode, int locationId, int properties) {
        // no-op
    }

    public void namespace(NamespaceBinding namespaceBinding, int properties) {}

    public void attribute(NodeName nameCode, SimpleType typeCode, CharSequence value, int locationId, int properties) {}


    /**
    * Output an element end tag. <br>
    * Does nothing  with this output method.
    */

    public void endElement() {
        // no-op
    }

    /**
     * Output a processing instruction. <br>
     * Does nothing with this output method.
    */

    public void processingInstruction(String name, /*@NotNull*/ CharSequence value, int locationId, int properties)
    throws XPathException {}

    /**
    * Output a comment. <br>
    * Does nothing with this output method.
    */

    public void comment(CharSequence chars, int locationId, int properties) throws XPathException {}

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