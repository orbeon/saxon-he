package net.sf.saxon.event;

import net.sf.saxon.charcode.UTF8CharacterSet;
import net.sf.saxon.trans.Err;
import net.sf.saxon.trans.XPathException;
import net.sf.saxon.value.Base64BinaryValue;
import net.sf.saxon.value.HexBinaryValue;

import javax.xml.transform.OutputKeys;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Properties;

/**
  * This class generates TEXT output
  * @author Michael H. Kay
  */

public class TEXTEmitter extends XMLEmitter {

    boolean recognizeBinary;

    /**
     * Set output properties
     * @param details the output serialization properties
     */

    public void setOutputProperties(Properties details) throws XPathException {
        super.setOutputProperties(details);
        String recognize = outputProperties.getProperty(SaxonOutputKeys.RECOGNIZE_BINARY);
        if ("yes".equals(recognize)) {
            recognizeBinary = true;
        }
    }

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
    * @param nameCode The element name (tag)
     * @param typeCode The type annotation
     * @param properties Bit fields holding any special properties of the element
     */

    public void startElement(int nameCode, int typeCode, int locationId, int properties) {
        // no-op
    }

    public void namespace(int namespaceCode, int properties) {}

    public void attribute(int nameCode, int typeCode, CharSequence value, int locationId, int properties) {}


    /**
    * Output an element end tag. <br>
    * Does nothing  with this output method.
    */

    public void endElement() {
        // no-op
    }

    /**
     * Output a processing instruction. <br>
     * Does nothing with this output method, unless the saxon:recognize-binary option is set, and this is the
     * processing instructions hex or b64. The name of the processing instruction may be followed by an encoding
     * name, for example b64.ascii indicates base64-encoded ASCII strings; if no encoding is present, the encoding
     * of the output method is assumed.
    */

    public void processingInstruction(String name, CharSequence value, int locationId, int properties)
    throws XPathException {
        if (recognizeBinary) {
            if (!started) {
                openDocument();
            }
            String encoding;
            byte[] bytes = null;
            int dot = name.indexOf('.');
            if (dot >= 0 && dot != name.length()-1) {
                encoding = name.substring(dot+1);
                name = name.substring(0, dot);
            } else {
                encoding = outputProperties.getProperty(OutputKeys.ENCODING, "utf8");
            }
            if (name.equals("hex")) {
                bytes = new HexBinaryValue(value).getBinaryValue();
            } else if (name.equals("b64")) {
                bytes = new Base64BinaryValue(value).getBinaryValue();
            }
            if (bytes != null) {
                try {
                    ByteArrayInputStream stream = new ByteArrayInputStream(bytes);
                    InputStreamReader reader = new InputStreamReader(stream, encoding);
                    char[] array = new char[bytes.length];
                    int used = reader.read(array, 0, array.length);
                    writer.write(array, 0, used);
                } catch (IOException e) {
                    throw new XPathException(
                    "Text output method: failed to decode binary data " + Err.wrap(value.toString(), Err.VALUE));
                }
            }
        }
    }

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
// The Original Code is: all this file.
//
// The Initial Developer of the Original Code is Michael H. Kay.
//
// Portions created by (your name) are Copyright (C) (your legal entity). All Rights Reserved.
//
// Contributor(s): none.
//
