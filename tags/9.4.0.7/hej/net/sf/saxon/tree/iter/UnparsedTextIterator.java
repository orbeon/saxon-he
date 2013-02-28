package net.sf.saxon.tree.iter;

import net.sf.saxon.Configuration;
import net.sf.saxon.expr.XPathContext;
import net.sf.saxon.functions.UnparsedText;
import net.sf.saxon.om.NameChecker;
import net.sf.saxon.om.SequenceIterator;
import net.sf.saxon.serialize.charcode.UTF16CharacterSet;
import net.sf.saxon.trans.XPathException;
import net.sf.saxon.value.StringValue;

import javax.xml.transform.SourceLocator;
import java.io.IOException;
import java.io.LineNumberReader;
import java.io.Reader;
import java.net.URI;

/**
* Class UnparsedTextIterator, iterates over a file line by line
*/
public class UnparsedTextIterator implements SequenceIterator<StringValue> {

    LineNumberReader reader;
    NameChecker checker;
    URI absoluteURI;
    XPathContext context;
    /*@Nullable*/ StringValue current = null;
    int position = 0;
    /*@Nullable*/ String encoding = null;
    SourceLocator location;
    
    /**
     * Create a UnparsedTextIterator over a given file
     * @param absoluteURI the URI identifying the file
     * @param context the dynamic evaluation context
     * @param encoding the expected encoding of the file
     * @param location the location of the instruction being executed
     * @throws XPathException if a dynamic error occurs
     */

    public UnparsedTextIterator(URI absoluteURI, /*@NotNull*/ XPathContext context, String encoding, SourceLocator location) throws XPathException {
    	Configuration config = context.getConfiguration();
   	 	Reader reader = context.getController().getUnparsedTextURIResolver().resolve(absoluteURI, encoding, config);
        // TODO: does LineNumberReader use the correct definition of line endings?
        
    	this.reader = new LineNumberReader(reader);
        this.absoluteURI = absoluteURI;
        this.context = context;
        this.checker = context.getConfiguration().getNameChecker();
        this.encoding = encoding;
        this.location = location;
    }

    /*@Nullable*/ public StringValue next() throws XPathException {
        if (position < 0) {
            // input already exhausted
            return null;
        }
        try {
            String s = reader.readLine();
            if (s == null) {
                current = null;
                position = -1;
                return null;
            }
            checkLine(checker, s);
            current = new StringValue(s);
            position++;
            return current;
        } catch (IOException err) {
            XPathException e = UnparsedText.handleIOError(absoluteURI, err);
            e.setLocator(location);
            throw e;
        }
    }

    /**
     * The current line in the file
     * @return returns StringValue: the current line in the file
     */
    /*@Nullable*/ public StringValue current() {
        return current;
    }

    /**
     * The line position currently being read
     * @return returns the current line in the file
     */
    public int position() {
        return position;
    }

    public void close() {
        try {
            reader.close();
        } catch (IOException err) {
            //
        }
    }

    /*@NotNull*/
    public SequenceIterator<StringValue> getAnother() throws XPathException {
        
         return new UnparsedTextIterator(absoluteURI, context, encoding, location);
    }

    public int getProperties() {
        return 0;
    }

    private void checkLine(/*@NotNull*/ NameChecker checker, /*@NotNull*/ String buffer) throws XPathException {
        for (int c=0; c<buffer.length();) {
            int ch32 = buffer.charAt(c++);
            if (UTF16CharacterSet.isHighSurrogate(ch32)) {
                char low = buffer.charAt(c++);
                ch32 = UTF16CharacterSet.combinePair((char)ch32, low);
            }
            if (!checker.isValidChar(ch32)) {
                XPathException err = new XPathException("The unparsed-text file contains a character that is illegal in XML (line=" +
                        position + " column=" + (c+1) + " value=hex " + Integer.toHexString(ch32) + ')');
                err.setErrorCode("XTDE1190");
                err.setLocator(location);
                throw err;
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