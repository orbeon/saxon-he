////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
// Copyright (c) 2014 Saxonica Limited.
// This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
// If a copy of the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
// This Source Code Form is "Incompatible With Secondary Licenses", as defined by the Mozilla Public License, v. 2.0.
////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

package net.sf.saxon.functions;

import net.sf.saxon.Configuration;
import net.sf.saxon.expr.Callable;
import net.sf.saxon.expr.Expression;
import net.sf.saxon.expr.StaticProperty;
import net.sf.saxon.expr.XPathContext;
import net.sf.saxon.expr.parser.ExpressionVisitor;
import net.sf.saxon.regex.LatinString;
import net.sf.saxon.serialize.charcode.UTF16CharacterSet;
import net.sf.saxon.trans.Err;
import net.sf.saxon.trans.XPathException;
import net.sf.saxon.tree.util.FastStringBuffer;
import net.sf.saxon.z.IntPredicate;

import java.io.IOException;
import java.io.Reader;
import java.net.URI;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.MalformedInputException;
import java.nio.charset.UnmappableCharacterException;


/**
 * Abstract superclass containing common code supporting the functions
 * unparsed-text(), unparsed-text-lines(), and unparsed-text-available()
 */

public abstract class UnparsedTextFunction extends SystemFunctionCall implements Callable {

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
     * @return the static base URI if known, otherwise null
     */
    public String getExpressionBaseURI() {
        return expressionBaseURI;
    }

    /**
     * preEvaluate: this method suppresses compile-time evaluation by doing nothing
     *
     * @param visitor an expression visitor
     */

    public Expression preEvaluate(ExpressionVisitor visitor) {
        return this;
        // in principle we could pre-evaluate any call of unparsed-text() with
        // constant arguments. But we don't, because the file contents might
        // change before the stylesheet executes.
    }

    public int computeSpecialProperties() {
        return super.computeSpecialProperties() & ~StaticProperty.NON_CREATIVE;
        // Pretend the function is creative to prevent the result going into a global variable,
        // which takes excessive memory. (TODO: But it does ensure stability...)
    }



    /**
     * Get the prefix of the error code for dynamic errors: "XTDE" for XSLT 2.0, "FOUT" for XPath 3.0
     * @param context the dynamic context
     * @return the first four characters of the error code to be used
     */

    protected static String getErrorCodePrefix(XPathContext context) {
        try {
            if (context.getController().getExecutable().isAllowXPath30()) {
                return "FOUT";
            } else {
                return "XTDE";
            }
        } catch (Exception e) {
            return "XTDE";
        }
    }


    /**
     * Supporting routine to load one external file given a URI (href) and a baseURI
     * @param href the relative URI
     * @param baseURI the base URI
     * @param encoding the character encoding
     * @param context the XPath dynamic context
     * @return the content of the file
     * @throws XPathException if the file cannot be read
     */

    public CharSequence readFile(String href, String baseURI, String encoding, XPathContext context)
            throws XPathException {

        final Configuration config = context.getConfiguration();
        IntPredicate checker = config.getValidCharacterChecker();

        // Use the URI machinery to validate and resolve the URIs

        URI absoluteURI = getAbsoluteURI(href, baseURI, context);

        Reader reader;
        try {
            reader = context.getController().getUnparsedTextURIResolver().resolve(absoluteURI, encoding, config);
        } catch (XPathException err) {
            err.maybeSetErrorCode(getErrorCodePrefix(context) + "1170");
            err.maybeSetLocation(this);
            throw err;
        }
        try {
            return readFile(checker, reader, context);
        } catch (java.io.UnsupportedEncodingException encErr) {
            XPathException e = new XPathException("Unknown encoding " + Err.wrap(encoding), encErr);
            e.setErrorCode(getErrorCodePrefix(context) + "1190");
            throw e;
        } catch (java.io.IOException ioErr) {
//            System.err.println("ProxyHost: " + System.getProperty("http.proxyHost"));
//            System.err.println("ProxyPort: " + System.getProperty("http.proxyPort"));
            XPathException e = handleIOError(absoluteURI, ioErr, context);
            e.setLocator(this);
            throw e;
        }
    }

    public static URI getAbsoluteURI(String href, String baseURI, XPathContext context) throws XPathException {
        URI absoluteURI;
        try {
            absoluteURI = ResolveURI.makeAbsolute(href, baseURI);
        } catch (java.net.URISyntaxException err) {
            XPathException e = new XPathException(err.getReason() + ": " + err.getInput(), err);
            e.setErrorCode(getErrorCodePrefix(context) + "1170");
            throw e;
        }

        if (absoluteURI.getFragment() != null) {
            XPathException e = new XPathException("URI for unparsed-text() must not contain a fragment identifier");
            e.setErrorCode(getErrorCodePrefix(context) + "1170");
            throw e;
        }

        // The URL dereferencing classes throw all kinds of strange exceptions if given
        // ill-formed sequences of %hh escape characters. So we do a sanity check that the
        // escaping is well-formed according to UTF-8 rules

        EscapeURI.checkPercentEncoding(absoluteURI.toString());
        return absoluteURI;
    }

    public static XPathException handleIOError(URI absoluteURI, IOException ioErr, XPathContext context) {
        String message = "Failed to read input file";
        if (absoluteURI != null && !ioErr.getMessage().equals(absoluteURI.toString())) {
            message += ' ' + absoluteURI.toString();
        }
        message += " (" + ioErr.getClass().getName() + ')';
        XPathException e = new XPathException(message, ioErr);
        String errorCode = "FOUT1200";
        if (context != null) {
            if (ioErr instanceof MalformedInputException) {
                errorCode = getErrorCodePrefix(context) + "1200";
            } else if (ioErr instanceof CharacterCodingException) {
                errorCode = getErrorCodePrefix(context) + "1200";
            } else if (ioErr instanceof UnmappableCharacterException) {
                errorCode = getErrorCodePrefix(context) + "1190";
            } else {
                errorCode = getErrorCodePrefix(context) + "1170";
            }
        }
        e.setErrorCode(errorCode);
        return e;
    }

    /**
     * Read the contents of an unparsed text file
     *
     * @param checker predicate for checking whether characters are valid XML characters
     * @param reader  Reader to be used for reading the file
     * @param context the XPath dynamic context
     * @return a CharSequence representing the contents of the file
     * @throws IOException    if a failure occurs reading the file
     * @throws XPathException if the file contains illegal characters
     */

    public static CharSequence readFile(IntPredicate checker, Reader reader, XPathContext context) throws IOException, XPathException {
        FastStringBuffer sb = new FastStringBuffer(FastStringBuffer.LARGE);
        char[] buffer = new char[2048];
        boolean first = true;
        int actual;
        int line = 1;
        int column = 1;
        boolean latin = true;
        while (true) {
            actual = reader.read(buffer, 0, 2048);
            if (actual < 0) {
                break;
            }
            for (int c = 0; c < actual; ) {
                int ch32 = buffer[c++];
                if (ch32 == '\n') {
                    line++;
                    column = 0;
                }
                column++;
                if (ch32 > 255) {
                    latin = false;
                    if (UTF16CharacterSet.isHighSurrogate(ch32)) {
                        if (c == actual) {
                            actual = reader.read(buffer, 0, 2048);
                            c = 0;
                        }
                        char low = buffer[c++];
                        ch32 = UTF16CharacterSet.combinePair((char) ch32, low);
                    }
                }
                if (!checker.matches(ch32)) {
                    XPathException err = new XPathException("The unparsed-text file contains a character that is illegal in XML (line=" +
                            line + " column=" + column + " value=hex " + Integer.toHexString(ch32) + ')');
                    err.setErrorCode(getErrorCodePrefix(context) + "1190");
                    throw err;
                }
            }
            if (first) {
                first = false;
                if (buffer[0] == '\ufeff') {
                    // don't include the BOM in the result
                    sb.append(buffer, 1, actual - 1);
                } else {
                    sb.append(buffer, 0, actual);
                }
            } else {
                sb.append(buffer, 0, actual);
            }
        }
        reader.close();
        if (latin) {
            return new LatinString(sb);
        } else {
            return sb.condense();
        }
    }


}

