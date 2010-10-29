package net.sf.saxon.style;

import net.sf.saxon.event.RuleBasedStripper;
import net.sf.saxon.expr.Expression;
import net.sf.saxon.expr.instruct.Executable;
import net.sf.saxon.om.AttributeCollection;
import net.sf.saxon.om.NamePool;
import net.sf.saxon.om.QNameException;
import net.sf.saxon.om.StandardNames;
import net.sf.saxon.pattern.*;
import net.sf.saxon.trans.StripSpaceRules;
import net.sf.saxon.trans.XPathException;
import net.sf.saxon.type.Type;

import java.util.StringTokenizer;

/**
* An xsl:preserve-space or xsl:strip-space elements in stylesheet. <br>
*/

public class XSLPreserveSpace extends StyleElement {

    private String elements;

    /**
     * Ask whether this node is a declaration, that is, a permitted child of xsl:stylesheet
     * (including xsl:include and xsl:import).
     * @return true for this element
     */

    @Override
    public boolean isDeclaration() {
        return true;
    }    

    public void prepareAttributes() throws XPathException {

		AttributeCollection atts = getAttributeList();

		for (int a=0; a<atts.getLength(); a++) {
			int nc = atts.getNameCode(a);
			String f = getNamePool().getClarkName(nc);
			if (f.equals(StandardNames.ELEMENTS)) {
        		elements = atts.getValue(a);
        	} else {
        		checkUnknownAttribute(nc);
        	}
        }
        if (elements==null) {
            reportAbsence("elements");
            elements="*";   // for error recovery
        }
    }

    public void validate(Declaration decl) throws XPathException {
        checkEmpty();
        checkTopLevel(null);
    }

    public Expression compile(Executable exec, Declaration decl) throws XPathException
    {
        RuleBasedStripper.StripRuleTarget preserve =
                (getFingerprint() == StandardNames.XSL_PRESERVE_SPACE ? RuleBasedStripper.PRESERVE : RuleBasedStripper.STRIP);
        StripSpaceRules stripperRules = getPrincipalStylesheetModule().getStripperRules();

        // elements is a space-separated list of element names

        StringTokenizer st = new StringTokenizer(elements, " \t\n\r", false);
        while (st.hasMoreTokens()) {
            String s = st.nextToken();
            NodeTest nt;
            if (s.equals("*")) {
                nt = NodeKindTest.ELEMENT;
                stripperRules.addRule(nt, preserve, decl.getModule(), decl.getSourceElement().getLineNumber());

            } else if (s.endsWith(":*")) {
                if (s.length()==2) {
                    compileError("No prefix before ':*'");
                }
                String prefix = s.substring(0, s.length()-2);
                String uri = getURIForPrefix(prefix, false);
                nt = new NamespaceTest(getNamePool(), Type.ELEMENT, uri);
                stripperRules.addRule(nt, preserve, decl.getModule(), decl.getSourceElement().getLineNumber());

            } else if (s.startsWith("*:")) {
                if (s.length()==2) {
                    compileError("No local name after '*:'");
                }
                String localname = s.substring(2);
                nt = new LocalNameTest(getNamePool(), Type.ELEMENT, localname);
                stripperRules.addRule(nt, preserve, decl.getModule(), decl.getSourceElement().getLineNumber());

            } else {
                String prefix;
                String localName;
                String uri;
                try {
                    String[] parts = getConfiguration().getNameChecker().getQNameParts(s);
                    prefix = parts[0];
                    if (parts[0].equals("")) {
                        uri = getDefaultXPathNamespace();
                    } else {
                        uri = getURIForPrefix(prefix, false);
                        if (uri == null) {
                            undeclaredNamespaceError(prefix, "XTSE0280");
                            return null;
                        }
                    }
                    localName = parts[1];
                } catch (QNameException err) {
                    compileError("Element name " + s + " is not a valid QName", "XTSE0280");
                    return null;
                }
                NamePool target = getNamePool();
                int nameCode = target.allocate("", uri, localName);
                nt = new NameTest(Type.ELEMENT, nameCode, getNamePool());
                stripperRules.addRule(nt, preserve, decl.getModule(), decl.getSourceElement().getLineNumber());
            }

        }
        return null;
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
