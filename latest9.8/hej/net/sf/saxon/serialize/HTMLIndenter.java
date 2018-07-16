////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
// Copyright (c) 2018 Saxonica Limited.
// This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
// If a copy of the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
// This Source Code Form is "Incompatible With Secondary Licenses", as defined by the Mozilla Public License, v. 2.0.
////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

package net.sf.saxon.serialize;

import net.sf.saxon.event.ProxyReceiver;
import net.sf.saxon.event.Receiver;
import net.sf.saxon.event.ReceiverOptions;
import net.sf.saxon.expr.parser.ExplicitLocation;
import net.sf.saxon.expr.parser.Location;
import net.sf.saxon.lib.NamespaceConstant;
import net.sf.saxon.om.FingerprintedQName;
import net.sf.saxon.om.NodeName;
import net.sf.saxon.trans.XPathException;
import net.sf.saxon.tree.tiny.CharSlice;
import net.sf.saxon.type.SchemaType;

import java.util.Arrays;
import java.util.HashSet;

/**
 * HTMLIndenter: This ProxyEmitter indents HTML elements, by adding whitespace
 * character data where appropriate.
 * The character data is never added when within an inline element.
 * The string used for indentation defaults to three spaces
 *
 * @author Michael Kay
 */


public class HTMLIndenter extends ProxyReceiver {

    // TODO: some of the logic in this class is probably redundant, e.g. the "sameLine" flag. However,
    // indentation is under-tested in the W3C test suites, so it's safest to avoid unnecessary changes.

    private int level = 0;

    protected char[] indentChars = {'\n', ' ', ' ', ' ', ' ', ' ', ' ', ' ', ' ', ' ', ' ', ' ', ' ', ' ', ' ', ' '};

    private boolean sameLine = false;
    private boolean inFormattedTag = false;
    private boolean afterInline = false;
    private boolean afterFormatted = true;    // to prevent a newline at the start
    private boolean afterEndElement = false;
    /*@NotNull*/ private int[] propertyStack = new int[20];
    private NameClassifier classifier;

    /**
     * The NameClassifier classifies element names according to whether the element is (a) an inline element,
     * and/or (b) a formatted element
     */

    interface NameClassifier {

        int IS_INLINE = 1;
        int IS_FORMATTED = 2;

        /**
         * Classify an element name as inline, formatted, or both or neither.
         * This method is overridden in the XHTML indenter
         *
         * @param name the element name
         * @return a bit-significant integer containing flags IS_INLINE and/or IS_FORMATTED
         */
        int classifyTag(NodeName name);

    }

    // When elements are classified as inline, indenting whitespace is not added adjacent to the element.

    // See Saxon bug 3839 and W3C bug 30276. We use a list of inline elements that is the union of
    // the HTML4 and HTML5 lists, on the basis that no harm is done treating an element as inline
    // even if the spec doesn't require us to do so. This also means we include elements such as
    // "ins", "del", and "area" that are sometimes inline and sometimes not.

    final private static String[] inlineTags = {
            "a", "abbr", "acronym", "applet", "area",
            "audio", "b", "basefont", "bdi", "bdo", "big", "br", "button", "canvas", "cite", "code", "data",
            "datalist", "del", "dfn", "em", "embed", "font", "i", "iframe", "img", "input", "ins",
            "kbd", "label", "link", "map",
            "mark", "math", "meter", "noscript", "object", "output", "picture",
            "progress", "q", "ruby", "s", "samp", "script", "select", "small", "span",
            "strike", "strong", "sub", "sup", "svg", "template", "textarea",
            "time", "tt", "u", "var", "video", "wbr"};

    final static String[] formattedTags = {"pre", "script", "style", "textarea", "title", "xmp"};
    // "xmp" is obsolete but still encountered!

    /**
     * Class to classify HTML names
     */

    static class HTMLNameClassifier implements NameClassifier {

        final static HTMLNameClassifier THE_INSTANCE = new HTMLNameClassifier();

        private static HTMLTagHashSet inlineTable = new HTMLTagHashSet(101);
        private static HTMLTagHashSet formattedTable = new HTMLTagHashSet(23);

        static {
            for (String inlineTag : inlineTags) {
                inlineTable.add(inlineTag);
            }

            for (String formattedTag : formattedTags) {
                formattedTable.add(formattedTag);
            }
        }

        /**
         * Classify an element name as inline, formatted, or both or neither.
         * This method is overridden in the XHTML indenter
         *
         * @param elemName the element name
         * @return a bit-significant integer containing flags IS_INLINE and/or IS_FORMATTED
         */
        public int classifyTag(NodeName elemName) {
            int r = 0;
            String tag = elemName.getDisplayName();
            if (inlineTable.contains(tag)) {
                r |= IS_INLINE;
            }
            if (formattedTable.contains(tag)) {
                r |= IS_FORMATTED;
            }
            return r;
        }


    }

    /**
     * Class to classify XHTML names for html-version 4
     */

    static class XHTML4NameClassifier implements NameClassifier {

        final static XHTML4NameClassifier THE_INSTANCE = new XHTML4NameClassifier();

        private final static HashSet<NodeName> inlineTagSet;
        private final static HashSet<NodeName> formattedTagSet;

        static {
            inlineTagSet = new HashSet<NodeName>(50);
            formattedTagSet = new HashSet<NodeName>(10);
            for (String inlineTag : inlineTags) {
                String ns = NamespaceConstant.XHTML;
                if (inlineTag.equals("math")) {
                    ns = NamespaceConstant.MATH;
                } else if (inlineTag.equals("svg")) {
                    ns = NamespaceConstant.SVG;
                }
                inlineTagSet.add(new FingerprintedQName("", ns, inlineTag));
            }
            for (String formattedTag : formattedTags) {
                formattedTagSet.add(new FingerprintedQName("", NamespaceConstant.XHTML, formattedTag));
            }
        }

        /**
         * Classify an element name as inline, formatted, or both or neither.
         * This method is overridden in the XHTML indenter
         *
         * @param name the element name
         * @return a bit-significant integer containing flags IS_INLINE and/or IS_FORMATTED
         */

        public int classifyTag(NodeName name) {
            int r = 0;
            if (inlineTagSet.contains(name)) {
                r |= IS_INLINE;
            }
            if (formattedTagSet.contains(name)) {
                r |= IS_FORMATTED;
            }
            return r;
        }

    }

    /**
     * Class to classify XHTML names for html-version 5
     */

    static class XHTML5NameClassifier implements NameClassifier {

        final static XHTML5NameClassifier THE_INSTANCE = new XHTML5NameClassifier();

        private static HTMLTagHashSet inlineTable = new HTMLTagHashSet(101);
        private static HTMLTagHashSet formattedTable = new HTMLTagHashSet(23);
        private static NodeName SVG = new FingerprintedQName("svg", NamespaceConstant.SVG, "svg");
        private static NodeName MATH = new FingerprintedQName("math", NamespaceConstant.MATHML, "math");

        static {
            for (String inlineTag : inlineTags) {
                inlineTable.add(inlineTag);
            }

            for (String formattedTag : formattedTags) {
                formattedTable.add(formattedTag);
            }
        }

        /**
         * Classify an element name as inline, formatted, or both or neither.
         * This method is overridden in the XHTML indenter
         *
         * @param name the element name
         * @return a bit-significant integer containing flags IS_INLINE and/or IS_FORMATTED
         */

        public int classifyTag(NodeName name) {
            if (name.hasURI(NamespaceConstant.XHTML) || name.hasURI("") || name.equals(SVG) || name.equals(MATH)) {
                int r = 0;
                if (inlineTable.contains(name.getLocalPart())) {
                    r |= IS_INLINE;
                }
                if (formattedTable.contains(name.getLocalPart())) {
                    r |= IS_FORMATTED;
                }
                return r;
            } else {
                return 0;
            }
        }

    }


    public HTMLIndenter(Receiver next, String method) {
        super(next);
        if ("xhtml".equals(method)) {
            classifier = XHTML4NameClassifier.THE_INSTANCE;
        } else if ("xhtml5".equals(method)) {
            classifier = XHTML5NameClassifier.THE_INSTANCE;
        } else {
            classifier = HTMLNameClassifier.THE_INSTANCE;
        }
    }

    /**
     * Output element start tag
     */

    public void startElement(NodeName nameCode, SchemaType typeCode, Location location, int properties) throws XPathException {
        int tagProps = classifier.classifyTag(nameCode);
        if (level >= propertyStack.length) {
            propertyStack = Arrays.copyOf(propertyStack, level * 2);
        }
        propertyStack[level] = tagProps;
        boolean inlineTag = (tagProps & NameClassifier.IS_INLINE) != 0;
        inFormattedTag = inFormattedTag || ((tagProps & NameClassifier.IS_FORMATTED) != 0);
        if (!inlineTag && !inFormattedTag &&
                !afterInline && !afterFormatted) {
            indent();
        }

        nextReceiver.startElement(nameCode, typeCode, location, properties);
        level++;
        sameLine = true;
        afterInline = false;
        afterFormatted = false;
        afterEndElement = false;
    }

    /**
     * Output element end tag
     */

    public void endElement() throws XPathException {
        level--;
        boolean thisInline = (propertyStack[level] & NameClassifier.IS_INLINE) != 0;
        boolean thisFormatted = (propertyStack[level] & NameClassifier.IS_FORMATTED) != 0;
        // See bug 3842. The test on afterEndElement is needed for conformance (indenting whitespace
        // isn't allowed before the end tag) but the change is potentially disruptive so is being
        // held back to the next major release.
        if (/*afterEndElement &&*/ !thisInline && !thisFormatted && !afterInline &&
                !sameLine && !afterFormatted && !inFormattedTag) {
            indent();
            afterInline = false;
            afterFormatted = false;
        } else {
            afterInline = thisInline;
            afterFormatted = thisFormatted;
        }
        nextReceiver.endElement();
        inFormattedTag = inFormattedTag && !thisFormatted;
        sameLine = false;
        afterEndElement = true;
    }

    /**
     * Output character data
     */

    public void characters(CharSequence chars, Location locationId, int properties) throws XPathException {
        if (inFormattedTag ||
                (properties & ReceiverOptions.USE_NULL_MARKERS) != 0 ||
                (properties & ReceiverOptions.DISABLE_ESCAPING) != 0) {
            // don't split the text if in a tag such as <pre>, or if the text contains the result of
            // expanding a character map or was produced using disable-output-escaping
            nextReceiver.characters(chars, locationId, properties);
        } else {
            // otherwise try to split long lines into multiple lines
            int lastNL = 0;
            for (int i = 0; i < chars.length(); i++) {
                if (chars.charAt(i) == '\n' || (i - lastNL > getLineLength() && chars.charAt(i) == ' ')) {
                    sameLine = false;
                    nextReceiver.characters(chars.subSequence(lastNL, i), locationId, properties);
                    indent();
                    lastNL = i + 1;
                    while (lastNL < chars.length() && chars.charAt(lastNL) == ' ') {
                        lastNL++;
                    }
                }
            }
            if (lastNL < chars.length()) {
                nextReceiver.characters(chars.subSequence(lastNL, chars.length()), locationId, properties);
            }
        }
        afterInline = false;
        afterEndElement = false;
    }

    /**
     * Output a comment
     */

    public void comment(CharSequence chars, Location locationId, int properties) throws XPathException {
        indent();
        nextReceiver.comment(chars, locationId, properties);
        afterEndElement = false;
    }

    /**
     * Output white space to reflect the current indentation level
     *
     * @throws net.sf.saxon.trans.XPathException if an error occurs downstream in the pipeline
     */

    private void indent() throws XPathException {
        int spaces = level * getIndentation();
        if (spaces + 1 >= indentChars.length) {
            int increment = 5 * getIndentation();
            if (spaces + 1 > indentChars.length + increment) {
                increment += spaces + 1;
            }
            char[] c2 = new char[indentChars.length + increment];
            System.arraycopy(indentChars, 0, c2, 0, indentChars.length);
            Arrays.fill(c2, indentChars.length, c2.length, ' ');
            indentChars = c2;
        }
        nextReceiver.characters(new CharSlice(indentChars, 0, spaces + 1), ExplicitLocation.UNKNOWN_LOCATION, 0);
        sameLine = false;
    }

    /**
     * Get the number of spaces to be used for indentation
     *
     * @return the number of spaces to be added to the indentation for each level
     */

    protected int getIndentation() {
        return 3;
    }

    /**
     * Get the maximum length of lines, after which long lines will be word-wrapped
     *
     * @return the maximum line length
     */

    protected int getLineLength() {
        return 80;
    }

}

