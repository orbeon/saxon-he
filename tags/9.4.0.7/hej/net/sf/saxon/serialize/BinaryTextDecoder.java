package net.sf.saxon.serialize;

import net.sf.saxon.event.ProxyReceiver;
import net.sf.saxon.event.Receiver;
import net.sf.saxon.trans.Err;
import net.sf.saxon.trans.XPathException;
import net.sf.saxon.tree.tiny.CharSlice;
import net.sf.saxon.value.Base64BinaryValue;
import net.sf.saxon.value.HexBinaryValue;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Properties;

/**
  * This class generates decodes processing instructions in text output that represent text encoded
  * in base64 binary or hexBinary
  * @author Michael H. Kay
  */

public class BinaryTextDecoder extends ProxyReceiver {

    String outputEncoding = "utf8";

    public BinaryTextDecoder(Receiver next, Properties details) throws XPathException {
        super(next);
        setOutputProperties(details);
    }

    /**
     * Set output properties
     * @param details the output serialization properties
     */

    public void setOutputProperties(Properties details) throws XPathException {
        outputEncoding = details.getProperty("encoding", "utf8");
    }


    /**
     * Output a processing instruction. <br>
     * Does nothing with this output method, unless the saxon:recognize-binary option is set, and this is the
     * processing instructions hex or b64. The name of the processing instruction may be followed by an encoding
     * name, for example b64.ascii indicates base64-encoded ASCII strings; if no encoding is present, the encoding
     * of the output method is assumed.
    */

    public void processingInstruction(String name, /*@NotNull*/ CharSequence value, int locationId, int properties)
    throws XPathException {
        String encoding;
        byte[] bytes = null;
        int dot = name.indexOf('.');
        if (dot >= 0 && dot != name.length()-1) {
            encoding = name.substring(dot+1);
            name = name.substring(0, dot);
        } else {
            encoding = outputEncoding;
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
                nextReceiver.characters(new CharSlice(array, 0, used), locationId, properties);
            } catch (IOException e) {
                throw new XPathException(
                "Text output method: failed to decode binary data " + Err.wrap(value.toString(), Err.VALUE));
            }
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