package net.sf.saxon.style;

import net.sf.saxon.expr.Expression;
import net.sf.saxon.expr.instruct.Executable;
import net.sf.saxon.om.AttributeCollection;
import net.sf.saxon.om.NamespaceBinding;
import net.sf.saxon.om.StandardNames;
import net.sf.saxon.trans.XPathException;
import net.sf.saxon.value.Whitespace;


/**
* An xsl:namespace-alias element in the stylesheet. <br>
*/

public class XSLNamespaceAlias extends StyleElement {

    private String stylesheetURI;
    private NamespaceBinding resultNamespaceBinding;

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

	    String stylesheetPrefix=null;
	    String resultPrefix=null;

		AttributeCollection atts = getAttributeList();

		for (int a=0; a<atts.getLength(); a++) {
			String f = atts.getQName(a);
			if (f.equals(StandardNames.STYLESHEET_PREFIX)) {
        		stylesheetPrefix = Whitespace.trim(atts.getValue(a));
        	} else if (f.equals(StandardNames.RESULT_PREFIX)) {
        		resultPrefix = Whitespace.trim(atts.getValue(a));
        	} else {
        		checkUnknownAttribute(atts.getNodeName(a));
        	}
        }
        if (stylesheetPrefix==null) {
            reportAbsence("stylesheet-prefix");
            return;
        }
        if (stylesheetPrefix.equals("#default")) {
            stylesheetPrefix="";
        }
        if (resultPrefix==null) {
            reportAbsence("result-prefix");
            return;
        }
        if (resultPrefix.equals("#default")) {
            resultPrefix="";
        }
        stylesheetURI = getURIForPrefix(stylesheetPrefix, true);
        if (stylesheetURI == null) {
            compileError("stylesheet-prefix " + stylesheetPrefix + " has not been declared", "XTSE0812");
            // recovery action
            stylesheetURI = "";
            resultNamespaceBinding = NamespaceBinding.DEFAULT_UNDECLARATION;
            return;
        } 
        String resultURI = getURIForPrefix(resultPrefix, true);
        if (resultURI == null) {
            compileError("result-prefix " + resultPrefix + " has not been declared", "XTSE0812");
            // recovery action
            stylesheetURI = "";
            resultURI = "";
        }
        resultNamespaceBinding = new NamespaceBinding(resultPrefix, resultURI);
    }

    public void validate(Declaration decl) throws XPathException {
        checkTopLevel("XTSE0010");
    }

    /*@Nullable*/ public Expression compile(Executable exec, Declaration decl) throws XPathException {
        return null;
    }

    protected void index(Declaration decl, PrincipalStylesheetModule top) throws XPathException {
        top.addNamespaceAlias(decl);
    }

    public String getStylesheetURI() {
        return stylesheetURI;
    }

    public NamespaceBinding getResultNamespaceBinding() {
        return resultNamespaceBinding;
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