////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
// Copyright (c) 2015 Saxonica Limited.
// This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
// If a copy of the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
// This Source Code Form is "Incompatible With Secondary Licenses", as defined by the Mozilla Public License, v. 2.0.
////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

package net.sf.saxon.functions;

import net.sf.saxon.Configuration;
import net.sf.saxon.lib.Validation;
import net.sf.saxon.trans.XPathException;
import net.sf.saxon.tree.util.FastStringBuffer;
import net.sf.saxon.value.Whitespace;
import org.xml.sax.XMLReader;

import java.io.File;
import java.io.FilenameFilter;
import java.util.StringTokenizer;
import java.util.regex.Pattern;

/**
 * A set of query parameters on a URI passed to the collection() or document() function
 */

public class URIQueryParameters {

    /*@Nullable*/ FilenameFilter filter = null;
    Boolean recurse = null;
    Integer validation = null;
    int strip = Whitespace.UNSPECIFIED;
    Integer onError = null;
    XMLReader parser = null;
    Boolean xinclude = null;
    boolean unparsed;
    Boolean stable = null;
    Boolean metadata = null;

    public static final int ON_ERROR_FAIL = 1;
    public static final int ON_ERROR_WARNING = 2;
    public static final int ON_ERROR_IGNORE = 3;

    /**
     * Create an object representing the query part of a URI
     *
     * @param query  the part of the URI after the "?" symbol
     * @param config the Saxon configuration
     */

    public URIQueryParameters(String query, Configuration config) {
        if (query != null) {
            StringTokenizer t = new StringTokenizer(query, ";&");
            while (t.hasMoreTokens()) {
                String tok = t.nextToken();
                int eq = tok.indexOf('=');
                if (eq > 0 && eq < (tok.length() - 1)) {
                    String keyword = tok.substring(0, eq);
                    String value = tok.substring(eq + 1);
                    processParameter(config, keyword, value);
                }
            }
        }
    }

    private void processParameter(Configuration config, String keyword, String value) {
        if (keyword.equals("select")) {
            filter = makeGlobFilter(value);
        } else if (keyword.equals("recurse")) {
            recurse = "yes".equals(value);
        } else if (keyword.equals("validation")) {
            int v = Validation.getCode(value);
            if (v != Validation.INVALID) {
                validation = v;
            }
        } else if (keyword.equals("strip-space")) {
            if (value.equals("yes")) {
                strip = Whitespace.ALL;
            } else if (value.equals("ignorable")) {
                strip = Whitespace.IGNORABLE;
            } else if (value.equals("no")) {
                strip = Whitespace.NONE;
            }
        } else if (keyword.equals("stable")) {
            if (value.equals("yes")) {
                stable = Boolean.TRUE;
            } else if (value.equals("no")) {
                stable = Boolean.FALSE;
            }
        } else if (keyword.equals("metadata")) {
            if (value.equals("yes")) {
                metadata = Boolean.TRUE;
            } else if (value.equals("no")) {
                metadata = Boolean.FALSE;
            }
        } else if (keyword.equals("xinclude")) {
            if (value.equals("yes")) {
                xinclude = Boolean.TRUE;
            } else if (value.equals("no")) {
                xinclude = Boolean.FALSE;
            }
        } else if (keyword.equals("unparsed")) {
            if (value.equals("yes")) {
                unparsed = true;
            } else if (value.equals("no")) {
                unparsed = false;
            }
        } else if (keyword.equals("on-error")) {
            if (value.equals("warning")) {
                onError = ON_ERROR_WARNING;
            } else if (value.equals("ignore")) {
                onError = ON_ERROR_IGNORE;
            } else if (value.equals("fail")) {
                onError = ON_ERROR_FAIL;
            }
        } else if (keyword.equals("parser") && config != null) {
            try {
                parser = (XMLReader) config.getInstance(value, null);
            } catch (XPathException err) {
                config.getErrorListener().warning(err);
            }
        }
    }

    public static FilenameFilter makeGlobFilter(String value) {
        FastStringBuffer sb = new FastStringBuffer(value.length() + 6);
        sb.append('^');
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            if (c == '.') {
                // replace "." with "\."
                sb.append("\\.");
            } else if (c == '*') {
                // replace "*" with ".*"
                sb.append(".*");
            } else if (c == '?') {
                // replace "*" with ".?"
                sb.append(".?");
            } else {
                sb.append(c);
            }
        }
        sb.append('$');
        String s = sb.toString();
        Pattern pattern = Pattern.compile(s);
        return new RegexFilter(pattern);
    }

    /**
     * Get the value of the strip-space=yes|no parameter.
     *
     * @return one of the values
     *         {@link Whitespace#ALL}, {@link net.sf.saxon.value.Whitespace#IGNORABLE}, {@link net.sf.saxon.value.Whitespace#NONE},
     *         {@link Whitespace#UNSPECIFIED}
     */

    public int getStripSpace() {
        return strip;
    }

    /**
     * Get the value of the validation=strict|lax|preserve|strip parameter, or null if unspecified
     */

    public Integer getValidationMode() {
        return validation;
    }

    /**
     * Get the file name filter (select=pattern), or null if unspecified
     */

    public FilenameFilter getFilenameFilter() {
        return filter;
    }

    /**
     * Get the value of the recurse=yes|no parameter, or null if unspecified
     */

    public Boolean getRecurse() {
        return recurse;
    }

    /**
     * Get the value of the on-error=fail|warning|ignore parameter, or null if unspecified
     */

    public Integer getOnError() {
        return onError;
    }

    /**
     * Get the value of xinclude=yes|no, or null if unspecified
     */

    public Boolean getXInclude() {
        return xinclude;
    }

    /**
     * Get the value of metadata=yes|no, or null if unspecified
     */

    public Boolean getMetaData() {
        return metadata;
    }

    /**
     * Get the value of unparsed=yes|no, or false if unspecified
     */

    public boolean isUnparsed() {
        return unparsed;
    }

    /**
     * Get the value of stable=yes|no, or null if unspecified
     */

    public Boolean getStable() {
        return stable;
    }

    /**
     * Get the selected XML parser, or null if unspecified
     */

    public XMLReader getXMLReader() {
        return parser;
    }

    /**
     * A FilenameFilter that tests file names against a regular expression
     */

    public static class RegexFilter implements FilenameFilter {

        private Pattern pattern;


        public RegexFilter(Pattern regex) {
            this.pattern = regex;
        }

        /**
         * Tests if a specified file should be included in a file list.
         *
         * @param dir  the directory in which the file was found.
         * @param name the name (last component) of the file.
         * @return <code>true</code> if and only if the name should be
         *         included in the file list; <code>false</code> otherwise.
         *         Returns true if the file is a directory or if it matches the glob pattern.
         */

        public boolean accept(File dir, String name) {
            return new File(dir, name).isDirectory() || pattern.matcher(name).matches();
        }

        /**
         * Test whether a name matches the pattern (regardless whether it is a directory or not)
         *
         * @param name the name (last component) of the file
         * @return true if the name matches the pattern.
         */
        public boolean matches(String name) {
            return pattern.matcher(name).matches();
        }
    }
}

