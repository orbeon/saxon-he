package net.sf.saxon.serialize;

import net.sf.saxon.event.Receiver;
import net.sf.saxon.event.ReceiverOptions;
import net.sf.saxon.lib.NamespaceConstant;
import net.sf.saxon.om.NodeName;
import net.sf.saxon.serialize.codenorm.Normalizer;
import net.sf.saxon.trans.XPathException;
import net.sf.saxon.type.SimpleType;

import java.util.HashSet;


/**
 * This class performs URI escaping for the XHTML output method. The logic for performing escaping
 * is the same as the HTML output method, but the way in which attributes are identified for escaping
 * is different, because XHTML is case-sensitive.
 */

public class XHTMLURIEscaper extends HTMLURIEscaper {

    /**
    * Table of attributes whose value is a URL
    */

    private static HashSet<String> urlTable = new HashSet<String>(70);
    private static HashSet<String> attTable = new HashSet<String>(20);
    
    private static void setUrlAttribute(String element, String attribute) {
        attTable.add(attribute);
        urlTable.add(element + "+" + attribute);
    }

    static {
        setUrlAttribute("form", "action");
        setUrlAttribute("object", "archive");
        setUrlAttribute("body", "background");
        setUrlAttribute("q", "cite");
        setUrlAttribute("blockquote", "cite");
        setUrlAttribute("del", "cite");
        setUrlAttribute("ins", "cite");
        setUrlAttribute("object", "classid");
        setUrlAttribute("object", "codebase");
        setUrlAttribute("applet", "codebase");
        setUrlAttribute("object", "data");
        setUrlAttribute("button", "datasrc");
        setUrlAttribute("div", "datasrc");
        setUrlAttribute("input", "datasrc");
        setUrlAttribute("object", "datasrc");
        setUrlAttribute("select", "datasrc");
        setUrlAttribute("span", "datasrc");
        setUrlAttribute("table", "datasrc");
        setUrlAttribute("textarea", "datasrc");
        setUrlAttribute("script", "for");
        setUrlAttribute("a", "href");
        setUrlAttribute("a", "name");       // see second note in section B.2.1 of HTML 4 specification
        setUrlAttribute("area", "href");
        setUrlAttribute("link", "href");
        setUrlAttribute("base", "href");
        setUrlAttribute("img", "longdesc");
        setUrlAttribute("frame", "longdesc");
        setUrlAttribute("iframe", "longdesc");
        setUrlAttribute("head", "profile");
        setUrlAttribute("script", "src");
        setUrlAttribute("input", "src");
        setUrlAttribute("frame", "src");
        setUrlAttribute("iframe", "src");
        setUrlAttribute("img", "src");
        setUrlAttribute("img", "usemap");
        setUrlAttribute("input", "usemap");
        setUrlAttribute("object", "usemap");
    }

    public XHTMLURIEscaper(Receiver next) {
        super(next);
    }

    /**
     * Determine whether a given attribute is a URL attribute
     */

    private static boolean isURLAttribute(NodeName elcode, NodeName atcode) {
        if (!elcode.isInNamespace(NamespaceConstant.XHTML)) {
            return false;
        }
        if (!atcode.isInNamespace("")) {
            return false;
        }
        String attName = atcode.getLocalPart();
        return attTable.contains(attName) && urlTable.contains(elcode.getLocalPart() + "+" + attName);
    }

    /**
     * Notify an attribute. Attributes are notified after the startElement event, and before any
     * children. Namespaces and attributes may be intermingled.
     *
     *
     *
     * @param attName   The name of the attribute, as held in the name pool
     * @param typeCode   The type of the attribute, as held in the name pool
     * @param properties Bit significant value. The following bits are defined:
     *                   <dd>DISABLE_ESCAPING</dd>    <dt>Disable escaping for this attribute</dt>
     *                   <dd>NO_SPECIAL_CHARACTERS</dd>      <dt>Attribute value contains no special characters</dt>
     * @throws IllegalStateException: attempt to output an attribute when there is no open element
     *                                start tag
     */

    public void attribute(NodeName attName, SimpleType typeCode, CharSequence value, int locationId, int properties) throws XPathException {
        if (escapeURIAttributes &&
                isURLAttribute(currentElement, attName) &&
                (properties & ReceiverOptions.DISABLE_ESCAPING) == 0) {
            CharSequence normalized =  (isAllAscii(value)
                    ? value
                    : new Normalizer(Normalizer.C, getConfiguration()).normalize(value) );
            
            getUnderlyingReceiver().attribute(
                    attName, typeCode, escapeURL(normalized, true, getConfiguration()), locationId,
                    properties | ReceiverOptions.DISABLE_CHARACTER_MAPS);
        } else {
            getUnderlyingReceiver().attribute(
                    attName, typeCode, value, locationId, properties);
        }
    }

    private static boolean isAllAscii(/*@NotNull*/ CharSequence value) {
        for (int i=0; i<value.length(); i++) {
            if (value.charAt(i) > 127) {
                return false;
            }
        }
        return true;
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