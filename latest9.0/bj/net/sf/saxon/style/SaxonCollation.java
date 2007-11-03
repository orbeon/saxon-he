package net.sf.saxon.style;
import net.sf.saxon.Configuration;
import net.sf.saxon.value.Whitespace;
import net.sf.saxon.expr.Expression;
import net.sf.saxon.instruct.Executable;
import net.sf.saxon.om.AttributeCollection;
import net.sf.saxon.om.NamespaceConstant;
import net.sf.saxon.om.StandardNames;
import net.sf.saxon.sort.StringCollator;
import net.sf.saxon.trans.SaxonErrorCode;
import net.sf.saxon.trans.XPathException;

import java.net.URI;
import java.net.URISyntaxException;
import java.text.Collator;
import java.util.Locale;
import java.util.Properties;

/**
* A saxon:collation element in the style sheet: this is a top-level
* element that defines details of a named collation. The attributes of the
* element provide different ways of instantiating an instance of net.sf.saxon.sort.StringCollator
 *
 * <p>saxon:collation</p> is deprecated from Saxon 8.8</p>
*/

public class SaxonCollation extends StyleElement {

    private String collationName;

    private StringCollator collator;

    public void prepareAttributes() throws XPathException {

		AttributeCollection atts = getAttributeList();

		String nameAtt = null;              // collation name for use in expressions
		String defaultAtt = null;

        Properties props = new Properties();

		for (int a=0; a<atts.getLength(); a++) {
			int nc = atts.getNameCode(a);
			String f = getNamePool().getClarkName(nc);
			if (f==StandardNames.NAME) {
        		nameAtt = Whitespace.trim(atts.getValue(a));
            } else if (f==StandardNames.CLASS) {
                props.setProperty("class", Whitespace.trim(atts.getValue(a)) );
            } else if (f==StandardNames.STRENGTH) {
                props.setProperty("strength", Whitespace.trim(atts.getValue(a)) );
            } else if (f==StandardNames.DECOMPOSITION) {
                props.setProperty("decomposition", Whitespace.trim(atts.getValue(a)) );
            } else if (f==StandardNames.LANG) {
                props.setProperty("lang", Whitespace.trim(atts.getValue(a)) );
            } else if (f==StandardNames.RULES) {
                props.setProperty("rules", Whitespace.trim(atts.getValue(a)) );
            } else if (f==StandardNames.CASE_ORDER) {
                props.setProperty("case-order", Whitespace.trim(atts.getValue(a)) );
            } else if (f==StandardNames.ALPHANUMERIC) {
                props.setProperty("alphanumeric", Whitespace.trim(atts.getValue(a)) );
            } else if (f==StandardNames.IGNORE_CASE) {
                props.setProperty("ignore-case", Whitespace.trim(atts.getValue(a)) );
            } else if (f==StandardNames.IGNORE_MODIFIERS) {
                props.setProperty("ignore-modifiers", Whitespace.trim(atts.getValue(a)) );
            } else if (f==StandardNames.IGNORE_SYMBOLS) {
                props.setProperty("ignore-modifiers", Whitespace.trim(atts.getValue(a)) );
            } else if (f==StandardNames.IGNORE_WIDTH) {
                props.setProperty("ignore-width", Whitespace.trim(atts.getValue(a)) );
            } else if (f==StandardNames.DEFAULT) {
                defaultAtt = Whitespace.trim(atts.getValue(a)) ;
        	} else {
        		checkUnknownAttribute(nc);
        	}
        }

        if (nameAtt!=null) {
            collationName = Whitespace.trim(nameAtt);
            URI collationURI;
            try {
                collationURI = new URI(collationName);
                if (!collationURI.isAbsolute()) {
                    URI base = new URI(getBaseURI());
                    collationURI = base.resolve(collationURI);
                    collationName = collationURI.toString();
                }
            } catch (URISyntaxException err) {
                compileError("Collation name '" + collationName + "' is not a valid URI");
                collationName = NamespaceConstant.CODEPOINT_COLLATION_URI;
            }
        }

        if (defaultAtt != null) {
            compileWarning("The 'default' attribute no longer has any effect. Use [xsl:]default-collation instead",
                    SaxonErrorCode.SXWN9005);
            if (defaultAtt.equals("yes")) {
                // ignore it
            } else if (defaultAtt.equals("no")) {
                // ignore it
            } else {
                compileError("default attribute must be yes or no");
            }
        }

        if (collator == null) {
            final Configuration config = getConfiguration();
            collator = Configuration.getPlatform().makeCollation(config, props, "");
            // TODO: build a URI allowing the collation to be reconstructed
        }

        // register the collation early, so it's available when optimizing XPath expressions
        getPrincipalStylesheet().setCollation(collationName, collator);
    }

    public void validate() throws XPathException {
        checkTopLevel(null);
        checkEmpty();
    }

    public Expression compile(Executable exec) throws XPathException {
        getPrincipalStylesheet().setCollation(collationName, collator);
        exec.setReasonUnableToCompile("Cannot compile a stylesheet that uses saxon:collation (because the Java class " +
                "java.text.RuleBasedCollator is not serializable)");
        return null;
    }

    public String getCollationName() {
        if (collationName == null) {
            try {
                // a forwards reference, perhaps
                prepareAttributes();
            } catch (XPathException err) {
                return null;    // we'll report the error when we come back to it.
            }
        }
        return collationName;
    }

    public StringCollator getCollator() {
        return collator;
    }

    /**
    * Utility method to print details of the locales for which a collator
    * is available. (The results depend on the Java VM)
    */

    public static void main(String[] args) {
        System.err.println("The following locales have collations available:");
        Locale[] loc = Collator.getAvailableLocales();
        for (int i=0; i<loc.length; i++) {
            Locale l=loc[i];
            System.err.println("Locale:" +
              ("".equals(l.getCountry()) ? "" : " country='" + l.getCountry() + "' (" + l.getDisplayCountry() + ')' ) +
              ("".equals(l.getLanguage()) ? "" : " language='" + l.getLanguage() + "' (" + l.getDisplayLanguage() + ')' ) +
              ("".equals(l.getVariant()) ? "" : " variant='" + l.getVariant() + "' (" + l.getDisplayVariant() + ')' ));
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
// The Initial Developer of the Original Code is Michael H. Kay of International Computers Limited (mhkay@iclway.co.uk).
//
// Portions created by (your name) are Copyright (C) (your legal entity). All Rights Reserved.
//
// Contributor(s): none.
//
