package net.sf.saxon.functions;

import net.sf.saxon.Configuration;
import net.sf.saxon.expr.Expression;
import net.sf.saxon.expr.ExpressionVisitor;
import net.sf.saxon.expr.StaticProperty;
import net.sf.saxon.expr.XPathContext;
import net.sf.saxon.om.Item;
import net.sf.saxon.om.NameChecker;
import net.sf.saxon.om.SequenceIterator;
import net.sf.saxon.serialize.charcode.UTF16CharacterSet;
import net.sf.saxon.trans.Err;
import net.sf.saxon.trans.XPathException;
import net.sf.saxon.tree.util.FastStringBuffer;
import net.sf.saxon.value.BooleanValue;
import net.sf.saxon.value.StringValue;

import java.io.*;
import java.net.URI;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.MalformedInputException;
import java.nio.charset.UnmappableCharacterException;


public class UnparsedText extends SystemFunction {

    // TODO: There is now a requirement that the results should be stable

    // TODO: Consider supporting a query parameter ?substitute-character=xFFDE

    String expressionBaseURI = null;

    public static final int UNPARSED_TEXT = 0;
    public static final int UNPARSED_TEXT_AVAILABLE = 1;
    public static final int UNPARSED_TEXT_LINES = 2;

    public void checkArguments(ExpressionVisitor visitor) throws XPathException {
        if (expressionBaseURI == null) {
            super.checkArguments(visitor);
            expressionBaseURI = visitor.getStaticContext().getBaseURI();
        }
    }


    /**
     * preEvaluate: this method suppresses compile-time evaluation by doing nothing
     * @param visitor an expression visitor
     */

    public Expression preEvaluate(ExpressionVisitor visitor) {
        return this;
        // in principle we could pre-evaluate any call of unparsed-text() with
        // constant arguments. But we don't, because the file contents might
        // change before the stylesheet executes.
    }

    public int computeSpecialProperties() {
        return super.computeSpecialProperties() &~ StaticProperty.NON_CREATIVE;
        // Pretend the function is creative to prevent the result going into a global variable,
        // which takes excessive memory. (TODO: But it does ensure stability...)
    }

    /**
     * This method handles evaluation of the function:
     * it returns a StringValue in the case of unparsed-text(), or a BooleanValue
     * in the case of unparsed-text-available(). In the case of unparsed-text-lines()
     * this shouldn't be called, but we deal with it anyway.
     */

    public Item evaluateItem(XPathContext context) throws XPathException {
        CharSequence content;
        StringValue result;
        try {
            StringValue hrefVal = (StringValue)argument[0].evaluateItem(context);
            if (hrefVal == null) {
                return null;
            }
            String href = hrefVal.getStringValue();

            String encoding = null;
            if (getNumberOfArguments() == 2) {
                encoding = argument[1].evaluateItem(context).getStringValue();
            }
            content = readFile(href, expressionBaseURI, encoding, context);
            result = new StringValue(content);
        } catch (XPathException err) {
            if (operation == UNPARSED_TEXT_AVAILABLE) {
                return BooleanValue.FALSE;
            } else {
                err.maybeSetErrorCode("XTDE1170");
                throw err;
            }
        }
        switch (operation) {
            case UNPARSED_TEXT_AVAILABLE:
                return BooleanValue.TRUE;
            case UNPARSED_TEXT:
                return result;
            case UNPARSED_TEXT_LINES:
                String contentString = content.toString();
                if (contentString.indexOf('\n') >=0 || contentString.indexOf('\r') >= 0) {
                    throw new XPathException("unparsed-text-lines() returned a sequence of more than one string", "XPTY0004");
                } else {
                    return result;
                }
            default:
                throw new UnsupportedOperationException(operation+"");
        }
    }

    /**
     * Return an Iterator to iterate over the values of a sequence. The value of every
     * expression can be regarded as a sequence, so this method is supported for all
     * expressions. This default implementation handles iteration for expressions that
     * return singleton values: for non-singleton expressions, the subclass must
     * provide its own implementation.
     * @param context supplies the context for evaluation
     * @return a SequenceIterator that can be used to iterate over the result
     *         of the expression
     * @throws net.sf.saxon.trans.XPathException
     *          if any dynamic error occurs evaluating the
     *          expression
     */

    public SequenceIterator iterate(XPathContext context) throws XPathException {
        switch (operation) {
            case UNPARSED_TEXT_AVAILABLE:
            case UNPARSED_TEXT:
                return super.iterate(context);
            case UNPARSED_TEXT_LINES:
                final StringValue hrefVal = (StringValue)argument[0].evaluateItem(context);
                if (hrefVal == null) {
                    return null;
                }
                final String href = hrefVal.getStringValue();

                String encoding = null;
                if (getNumberOfArguments() == 2) {
                    encoding = argument[1].evaluateItem(context).getStringValue();
                }
                final Configuration config = context.getConfiguration();

                // Use the URI machinery to validate and resolve the URIs

                final URI absoluteURI = getAbsoluteURI(href, expressionBaseURI);

                final Reader reader = context.getController().getUnparsedTextURIResolver().resolve(absoluteURI, encoding, config);
                // TODO: does LineNumberReader use the correct definition of line endings?
                final LineNumberReader lnReader = new LineNumberReader(reader);
                return new UnparsedTextIterator(lnReader, absoluteURI, context);
            default:
                throw new UnsupportedOperationException(operation+"");
        }
    }

    /**
     * Supporting routine to load one external file given a URI (href) and a baseURI
     */

    private CharSequence readFile(String href, String baseURI, String encoding, XPathContext context)
            throws XPathException {

        final Configuration config = context.getConfiguration();
        NameChecker checker = config.getNameChecker();

        // Use the URI machinery to validate and resolve the URIs

        URI absoluteURI = getAbsoluteURI(href, baseURI);

        Reader reader;
        try {
            reader = context.getController().getUnparsedTextURIResolver().resolve(absoluteURI, encoding, config);
        } catch (XPathException err) {
            err.maybeSetErrorCode("XTDE1170");
            err.maybeSetLocation(this);
            throw err;
        }
        try {
            return readFile(checker, reader);
        } catch (java.io.UnsupportedEncodingException encErr) {
            XPathException e = new XPathException("Unknown encoding " + Err.wrap(encoding), encErr);
            e.setErrorCode("XTDE1190");
            throw e;
        } catch (java.io.IOException ioErr) {
//            System.err.println("ProxyHost: " + System.getProperty("http.proxyHost"));
//            System.err.println("ProxyPort: " + System.getProperty("http.proxyPort"));
            XPathException e = handleIOError(absoluteURI, ioErr);
            e.setLocator(this);
            throw e;
        }
    }

    private URI getAbsoluteURI(String href, String baseURI) throws XPathException {
        URI absoluteURI;
        try {
            absoluteURI = ResolveURI.makeAbsolute(href, baseURI);
        } catch (java.net.URISyntaxException err) {
            XPathException e = new XPathException(err.getReason() + ": " + err.getInput(), err);
            e.setErrorCode("XTDE1170");
            throw e;
        }

        if (absoluteURI.getFragment() != null) {
            XPathException e = new XPathException("URI for unparsed-text() must not contain a fragment identifier");
            e.setErrorCode("XTDE1170");
            throw e;
        }

        // The URL dereferencing classes throw all kinds of strange exceptions if given
        // ill-formed sequences of %hh escape characters. So we do a sanity check that the
        // escaping is well-formed according to UTF-8 rules

        EscapeURI.checkPercentEncoding(absoluteURI.toString());
        return absoluteURI;
    }

    private static XPathException handleIOError(URI absoluteURI, IOException ioErr) {
        String message = "Failed to read input file";
        if (!ioErr.getMessage().equals(absoluteURI.toString())) {
            message += ' ' + absoluteURI.toString();
        }
        message += " (" + ioErr.getClass().getName() + ')';
        XPathException e = new XPathException(message, ioErr);
        String errorCode;
        if (ioErr instanceof MalformedInputException) {
            errorCode = "XTDE1200";
        } else if (ioErr instanceof CharacterCodingException) {
            errorCode = "XTDE1200";
        } else if (ioErr instanceof UnmappableCharacterException) {
            errorCode = "XTDE1190";
        } else {
            errorCode = "XTDE1170";
        }
        e.setErrorCode(errorCode);
        return e;
    }

    /**
     * Read the contents of an unparsed text file
     * @param checker NameChecker for checking whether characters are valid XML characters
     * @param reader Reader to be used for reading the file
     * @return a CharSequence representing the contents of the file
     * @throws IOException if a failure occurs reading the file
     * @throws XPathException if the file contains illegal characters
     */

    public static CharSequence readFile(NameChecker checker, Reader reader) throws IOException, XPathException {
        FastStringBuffer sb = new FastStringBuffer(FastStringBuffer.LARGE);
        char[] buffer = new char[2048];
        boolean first = true;
        int actual;
        int line = 1;
        int column = 1;
        while (true) {
            actual = reader.read(buffer, 0, 2048);
            if (actual < 0) {
                break;
            }
            for (int c=0; c<actual;) {
                int ch32 = buffer[c++];
                if (ch32 == '\n') {
                    line++;
                    column = 0;
                }
                column++;
                if (UTF16CharacterSet.isHighSurrogate(ch32)) {
                    if (c==actual) {
                        actual = reader.read(buffer, 0, 2048);
                        c = 0;
                    }
                    char low = buffer[c++];
                    ch32 = UTF16CharacterSet.combinePair((char)ch32, low);
                }
                if (!checker.isValidChar(ch32)) {
                    XPathException err = new XPathException("The unparsed-text file contains a character that is illegal in XML (line=" +
                            line + " column=" + column + " value=hex " + Integer.toHexString(ch32) + ')');
                    err.setErrorCode("XTDE1190");
                    throw err;
                }
            }
            if (first) {
                first = false;
                if (buffer[0]=='\ufeff') {
                    // don't include the BOM in the result
                    sb.append(buffer, 1, actual-1);
                } else {
                    sb.append(buffer, 0, actual);
                }
            } else {
                sb.append(buffer, 0, actual);
            }
        }
        reader.close();
        return sb.condense();
    }

    // diagnostic method to output the octets of a file

    public static void main(String[] args) throws Exception {
        FastStringBuffer sb1 = new FastStringBuffer(FastStringBuffer.MEDIUM);
        FastStringBuffer sb2 = new FastStringBuffer(FastStringBuffer.MEDIUM);
        File file = new File(args[0]);
        InputStream is = new FileInputStream(file);
        while (true) {
            int b = is.read();
            if (b<0) {
                System.out.println(sb1.toString());
                System.out.println(sb2.toString()); break;
            }
            sb1.append(Integer.toHexString(b)+" ");
            sb2.append((char)b + " ");
            if (sb1.length() > 80) {
                System.out.println(sb1.toString());
                System.out.println(sb2.toString());
                sb1 = new FastStringBuffer(FastStringBuffer.MEDIUM);
                sb2 = new FastStringBuffer(FastStringBuffer.MEDIUM);
            }
        }
        is.close();
    }

    private class UnparsedTextIterator implements SequenceIterator {

        LineNumberReader reader;
        NameChecker checker;
        URI absoluteURI;
        XPathContext context;
        StringValue current = null;
        int position = 0;

        public UnparsedTextIterator(LineNumberReader reader, URI absoluteURI, XPathContext context) {
            this.reader = reader;
            this.absoluteURI = absoluteURI;
            this.context = context;
            this.checker = context.getConfiguration().getNameChecker();
        }

        public Item next() throws XPathException {
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
                XPathException e = handleIOError(absoluteURI, err);
                e.setLocator(UnparsedText.this);
                throw e;
            }
        }

        public Item current() {
            return current;
        }

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

        public SequenceIterator getAnother() throws XPathException {
            return UnparsedText.this.iterate(context);
        }

        public int getProperties() {
            return 0;
        }

        private void checkLine(NameChecker checker, String buffer) throws XPathException {
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
                    err.setLocator(UnparsedText.this);
                    throw err;
                }
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
// The Original Code is: all this file.
//
// The Initial Developer of the Original Code is Michael H. Kay. 
//
// Portions created by (your name) are Copyright (C) (your legal entity). All Rights Reserved.
//
// Contributor(s): none.
//
