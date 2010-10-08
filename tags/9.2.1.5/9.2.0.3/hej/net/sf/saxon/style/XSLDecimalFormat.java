package net.sf.saxon.style;
import net.sf.saxon.expr.Expression;
import net.sf.saxon.instruct.Executable;
import net.sf.saxon.om.AttributeCollection;
import net.sf.saxon.om.NamespaceException;
import net.sf.saxon.om.StandardNames;
import net.sf.saxon.om.StructuredQName;
import net.sf.saxon.trans.DecimalFormatManager;
import net.sf.saxon.trans.DecimalSymbols;
import net.sf.saxon.trans.XPathException;
import net.sf.saxon.value.StringValue;
import net.sf.saxon.value.Whitespace;

/**
* Handler for xsl:decimal-format elements in stylesheet. <br>
*/

public class XSLDecimalFormat extends StyleElement {

    boolean prepared = false;

    String name;
    String decimalSeparator;
    String groupingSeparator;
    String infinity;
    String minusSign;
    String NaN;
    String percent;
    String perMille;
    String zeroDigit;
    String digit;
    String patternSeparator;

    public void prepareAttributes() throws XPathException {

        if (prepared) {
            return;
        }
        prepared = true;

		AttributeCollection atts = getAttributeList();

        for (int a=0; a<atts.getLength(); a++) {
			int nc = atts.getNameCode(a);
			String f = getNamePool().getClarkName(nc);
			if (f.equals(StandardNames.NAME)) {
        		name = Whitespace.trim(atts.getValue(a));
        	} else if (f.equals(StandardNames.DECIMAL_SEPARATOR)) {
        		decimalSeparator = atts.getValue(a);
        	} else if (f.equals(StandardNames.GROUPING_SEPARATOR)) {
        		groupingSeparator = atts.getValue(a);
        	} else if (f.equals(StandardNames.INFINITY)) {
        		infinity = atts.getValue(a);
        	} else if (f.equals(StandardNames.MINUS_SIGN)) {
        		minusSign = atts.getValue(a);
        	} else if (f.equals(StandardNames.NAN)) {
        		NaN = atts.getValue(a);
        	} else if (f.equals(StandardNames.PERCENT)) {
        		percent = atts.getValue(a);
        	} else if (f.equals(StandardNames.PER_MILLE)) {
        		perMille = atts.getValue(a);
        	} else if (f.equals(StandardNames.ZERO_DIGIT)) {
        		zeroDigit = atts.getValue(a);
        	} else if (f.equals(StandardNames.DIGIT)) {
        		digit = atts.getValue(a);
        	} else if (f.equals(StandardNames.PATTERN_SEPARATOR)) {
        		patternSeparator = atts.getValue(a);
        	} else {
        		checkUnknownAttribute(nc);
        	}
        }
    }

    public void validate() throws XPathException {
        checkTopLevel(null);
        checkEmpty();
    }

    public DecimalSymbols makeDecimalFormatSymbols() throws XPathException {
        DecimalSymbols d = new DecimalSymbols();
        if (decimalSeparator!=null) {
            d.decimalSeparator = (toChar(decimalSeparator));
        }
        if (groupingSeparator!=null) {
            d.groupingSeparator = (toChar(groupingSeparator));
        }
        if (infinity!=null) {
            d.infinity = (infinity);
        }
        if (minusSign!=null) {
            d.minusSign = (toChar(minusSign));
        }
        if (NaN!=null) {
            d.NaN = (NaN);
        }
        if (percent!=null) {
            d.percent = (toChar(percent));
        }
        if (perMille!=null) {
            d.permill = (toChar(perMille));
        }
        if (zeroDigit!=null) {
            d.zeroDigit = (toChar(zeroDigit));
            if (!(d.isValidZeroDigit())) {
                compileError(
                    "The value of the zero-digit attribute must be a Unicode digit with value zero",
                    "XTSE1295");
            }
        }
        if (digit!=null) {
            d.digit = (toChar(digit));
        }
        if (patternSeparator!=null) {
            d.patternSeparator = (toChar(patternSeparator));
        }
        try {
            d.checkDistinctRoles();
        } catch (XPathException err) {
            compileError(err.getMessage(), "XTSE1300");
        }
        return d;
    }




    public void index(XSLStylesheet top) throws XPathException
    {
        prepareAttributes();
        DecimalSymbols d = makeDecimalFormatSymbols();
        DecimalFormatManager dfm = getPreparedStylesheet().getDecimalFormatManager();
        if (name==null) {
            try {
                dfm.setDefaultDecimalFormat(d, getPrecedence());
            } catch (XPathException err) {
                compileError(err.getMessage(), err.getErrorCodeQName());
            }
        } else {
            try {
                StructuredQName formatName = makeQName(name);
                try {
                    dfm.setNamedDecimalFormat(formatName, d, getPrecedence());
                } catch (XPathException err) {
                    compileError(err.getMessage(), err.getErrorCodeQName());
                }
            } catch (XPathException err) {
                compileError("Invalid decimal format name. " + err.getMessage(), "XTSE0020");
            } catch (NamespaceException err) {
                compileError("Invalid decimal format name. " + err.getMessage(), "XTSE0280");
            }
        }
    }

    public Expression compile(Executable exec) throws XPathException {
        return null;
    }

    /**
     * Get the Unicode codepoint corresponding to a String, which must represent a single Unicode character
     * @param s the input string, representing a single Unicode character, perhaps as a surrogate pair
     * @return
     * @throws XPathException
     */
    private int toChar(String s) throws XPathException {
        int[] e = StringValue.expand(s);
        if (e.length!=1)
            compileError("Attribute \"" + s + "\" should be a single character", "XTSE0020");
        return e[0];
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
