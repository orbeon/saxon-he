package net.sf.saxon.style;
import net.sf.saxon.expr.Expression;
import net.sf.saxon.expr.instruct.Executable;
import net.sf.saxon.om.*;
import net.sf.saxon.trans.XPathException;
import net.sf.saxon.value.Whitespace;


/**
* An xsl:namespace-alias element in the stylesheet. <br>
*/

public class XSLNamespaceAlias extends StyleElement {

    private short stylesheetURICode;
    private int resultNamespaceCode;

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
			int nc = atts.getNameCode(a);
			String f = getNamePool().getClarkName(nc);
			if (f.equals(StandardNames.STYLESHEET_PREFIX)) {
        		stylesheetPrefix = Whitespace.trim(atts.getValue(a));
        	} else if (f.equals(StandardNames.RESULT_PREFIX)) {
        		resultPrefix = Whitespace.trim(atts.getValue(a));
        	} else {
        		checkUnknownAttribute(nc);
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
        try {
            String ssURI = getURIForPrefix(stylesheetPrefix, true);
            if (ssURI == null) {
                compileError("stylesheet-prefix " + stylesheetPrefix + " has not been declared", "XTSE0812");
                // recovery action
                stylesheetURICode = 0;
                resultNamespaceCode = 0;
                return;
            } else if (ssURI.equals("")) {
                //compileError("#default cannot be used: there is no default namespace", "XTSE0815");
                // recovery action
                stylesheetURICode = 0;
                //resultNamespaceCode = 0;
                //return;
            } else {
                stylesheetURICode = getURICodeForPrefix(stylesheetPrefix);
            }
            NamePool pool = getNamePool();
            String resultURI = getURIForPrefix(resultPrefix, true);
            if (resultURI == null) {
                compileError("result-prefix " + resultPrefix + " has not been declared", "XTSE0812");
                // recovery action
                stylesheetURICode = 0;
                resultNamespaceCode = 0;
            } else if (resultURI.equals("")) {
                //compileError("#default cannot be used: there is no default namespace", "XTSE0815");
                // recovery action
                //stylesheetURICode = 0;
                resultNamespaceCode = 0;
            } else {
                resultNamespaceCode = pool.getNamespaceCode(resultPrefix, resultURI);
            }
        } catch (NamespaceException err) {
            compileError(err.getMessage());
        }
    }

    public void validate(Declaration decl) throws XPathException {
        checkTopLevel(null);
    }

    public Expression compile(Executable exec, Declaration decl) throws XPathException {
        return null;
    }

    protected void index(Declaration decl, PrincipalStylesheetModule top) throws XPathException {
        top.addNamespaceAlias(decl);
    }

    public short getStylesheetURICode() {
        return stylesheetURICode;
    }

    public int getResultNamespaceCode() {
        return resultNamespaceCode;
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
