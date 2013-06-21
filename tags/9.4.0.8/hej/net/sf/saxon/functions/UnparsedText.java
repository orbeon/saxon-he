package net.sf.saxon.functions;

import net.sf.saxon.Configuration;
import net.sf.saxon.expr.CallableExpression;
import net.sf.saxon.expr.Expression;
import net.sf.saxon.expr.StaticProperty;
import net.sf.saxon.expr.XPathContext;
import net.sf.saxon.expr.parser.ExpressionVisitor;
import net.sf.saxon.om.Item;
import net.sf.saxon.om.NameChecker;
import net.sf.saxon.om.SequenceIterator;
import net.sf.saxon.serialize.charcode.UTF16CharacterSet;
import net.sf.saxon.trans.Err;
import net.sf.saxon.trans.XPathException;
import net.sf.saxon.tree.util.FastStringBuffer;
import net.sf.saxon.value.StringValue;
import net.sf.saxon.value.Value;

import java.io.*;
import java.net.URI;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.MalformedInputException;
import java.nio.charset.UnmappableCharacterException;


public class UnparsedText extends SystemFunction implements CallableExpression {

    // TODO: There is now a requirement that the results should be stable

    // TODO: Consider supporting a query parameter ?substitute-character=xFFDE

    /*@Nullable*/ String expressionBaseURI = null;

    public void checkArguments(/*@NotNull*/ ExpressionVisitor visitor) throws XPathException {
        if (expressionBaseURI == null) {
            super.checkArguments(visitor);
            expressionBaseURI = visitor.getStaticContext().getBaseURI();
        }
    }

    /**
     * getExpressionBaseURI: this method returns the expression base URI
     */
    public String getExpressionBaseURI(){
    	return expressionBaseURI;	
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
        StringValue hrefVal = (StringValue)argument[0].evaluateItem(context);
        String encoding = (getNumberOfArguments() == 2 ? argument[1].evaluateItem(context).getStringValue() : null);
        return evalUnparsedText(hrefVal, encoding, context);
    }

    /**
     * Evaluate the expression
     *
     * @param arguments the values of the arguments, supplied as SequenceIterators
     * @param context   the dynamic evaluation context
     * @return the result of the evaluation, in the form of a SequenceIterator
     * @throws net.sf.saxon.trans.XPathException
     *          if a dynamic error occurs during the evaluation of the expression
     */
    public SequenceIterator call(SequenceIterator[] arguments, XPathContext context) throws XPathException {
        StringValue hrefVal = (StringValue)arguments[0].next();
        String encoding = (getNumberOfArguments() == 2 ? arguments[1].next().getStringValue() : null);
        return Value.asIterator(evalUnparsedText(hrefVal, encoding, context));
    }

    public Item evalUnparsedText(StringValue hrefVal, String encoding, XPathContext context) throws XPathException {
        CharSequence content;
        StringValue result;
        try {
            if (hrefVal == null) {
                return null;
            }
            String href = hrefVal.getStringValue();

            content = readFile(href, expressionBaseURI, encoding, context);
            result = new StringValue(content);
        } catch (XPathException err) {
            err.maybeSetErrorCode("XTDE1170");
            throw err;
        }
        return result;
    }


    /**
     * Supporting routine to load one external file given a URI (href) and a baseURI
     */

    public CharSequence readFile(String href, String baseURI, String encoding, XPathContext context)
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

    public static URI getAbsoluteURI(String href, String baseURI) throws XPathException {
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

    public static XPathException handleIOError(URI absoluteURI, IOException ioErr) {
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