package net.sf.saxon.style;

import net.sf.saxon.expr.AxisExpression;
import net.sf.saxon.expr.Expression;
import net.sf.saxon.expr.instruct.ApplyTemplates;
import net.sf.saxon.expr.instruct.Executable;
import net.sf.saxon.expr.parser.RoleLocator;
import net.sf.saxon.expr.parser.TypeChecker;
import net.sf.saxon.expr.sort.SortExpression;
import net.sf.saxon.expr.sort.SortKeyDefinition;
import net.sf.saxon.om.*;
import net.sf.saxon.trans.Err;
import net.sf.saxon.trans.Mode;
import net.sf.saxon.trans.XPathException;
import net.sf.saxon.tree.iter.AxisIterator;
import net.sf.saxon.type.Type;
import net.sf.saxon.value.DecimalValue;
import net.sf.saxon.value.SequenceType;
import net.sf.saxon.value.Whitespace;


/**
* An xsl:apply-templates element in the stylesheet
*/

public class XSLApplyTemplates extends StyleElement {

    /*@Nullable*/ private Expression select;
    private StructuredQName modeName;   // null if no name specified or if conventional values such as #current used
    private boolean useCurrentMode = false;
    private boolean useTailRecursion = false;
    private boolean defaultedSelectExpression = true;
    private Mode mode;
    private String modeAttribute;

    /**
    * Determine whether this node is an instruction.
    * @return true - it is an instruction
    */

    public boolean isInstruction() {
        return true;
    }


    public void prepareAttributes() throws XPathException {

		AttributeCollection atts = getAttributeList();

		String selectAtt = null;

		for (int a=0; a<atts.getLength(); a++) {
			String f = atts.getQName(a);
			if (f.equals(StandardNames.MODE)) {
        		modeAttribute = Whitespace.trim(atts.getValue(a));
        	} else if (f.equals(StandardNames.SELECT)) {
        		selectAtt = atts.getValue(a);
                defaultedSelectExpression = false;
            } else {
        		checkUnknownAttribute(atts.getNodeName(a));
        	}
        }

        if (modeAttribute!=null) {
            if (modeAttribute.equals("#current")) {
                useCurrentMode = true;
            } else if (modeAttribute.equals("#unnamed") && isXslt30Processor()) {
                modeName = Mode.UNNAMED_MODE_NAME;
            } else if (modeAttribute.equals("#default")) {
                // do nothing;
            } else {
                try {
                    modeName = makeQName(modeAttribute);
                } catch (NamespaceException err) {
                    compileError(err.getMessage(), "XTSE0280");
                    modeName = null;
                } catch (XPathException err) {
                    compileError("Mode name " + Err.wrap(modeAttribute) + " is not a valid QName",
                            err.getErrorCodeQName());
                    modeName = null;
                }
            }
        }

        if (selectAtt!=null) {
            select = makeExpression(selectAtt);
        }
    }

    public void validate(Declaration decl) throws XPathException {

        // get the Mode object
        if (!useCurrentMode) {
            if (modeName == null) {
                // XSLT 3.0 allows a default mode to be specified on the xsl:stylesheet element
                modeName = getContainingStylesheet().getDefaultMode();
            }
            mode = getPreparedStylesheet().getRuleManager().getMode(modeName, true);
        }

        // handle sorting if requested

        AxisIterator kids = iterateAxis(Axis.CHILD);
        while (true) {
            NodeInfo child = kids.next();
            if (child == null) {
                break;
            }
            if (child instanceof XSLSort) {
                // no-op
            } else if (child instanceof XSLWithParam) {
                // usesParams = true;
            } else if (child.getNodeKind() == Type.TEXT) {
                    // with xml:space=preserve, white space nodes may still be there
                if (!Whitespace.isWhite(child.getStringValueCS())) {
                    compileError("No character data is allowed within xsl:apply-templates", "XTSE0010");
                }
            } else {
                compileError("Invalid element within xsl:apply-templates", "XTSE0010");
            }
        }

        if (select==null) {
            int lineNr = getLineNumber();
            select = new AxisExpression(Axis.CHILD, null);
            select.setLocationId(lineNr);
        }

        select = typeCheck("select", select);
        if (!getEffectiveVersion().equals(DecimalValue.THREE)) {
            try {
                RoleLocator role =
                    new RoleLocator(RoleLocator.INSTRUCTION, "xsl:apply-templates/select", 0);
                role.setErrorCode("XTTE0520");
                select = TypeChecker.staticTypeCheck(select,
                        SequenceType.NODE_SEQUENCE,
                        false, role, makeExpressionVisitor());
            } catch (XPathException err) {
                compileError(err);
            }
        }
    }

    /**
     * Mark tail-recursive calls on templates and functions.
     * For most instructions, this does nothing.
    */

    public boolean markTailCalls() {
        useTailRecursion = true;
        return true;
    }


    public Expression compile(Executable exec, Declaration decl) throws XPathException {
        SortKeyDefinition[] sortKeys = makeSortKeys(decl);
        if (sortKeys != null) {
            useTailRecursion = false;
        }
        assert select != null;
        Expression sortedSequence = select;
        if (sortKeys != null) {
            sortedSequence = new SortExpression(select, sortKeys);
        }
        compileSequenceConstructor(exec, decl, iterateAxis(Axis.CHILD), true);
        ApplyTemplates app = new ApplyTemplates(
                                    sortedSequence,
                                    useCurrentMode,
                                    useTailRecursion,
                                    defaultedSelectExpression,
                                    mode);
        app.setActualParameters(getWithParamInstructions(exec, decl, false, app),
                                 getWithParamInstructions(exec, decl, true, app));
        return app;
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