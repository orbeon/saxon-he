package net.sf.saxon.style;
import net.sf.saxon.expr.Expression;
import net.sf.saxon.expr.Literal;
import net.sf.saxon.expr.instruct.Executable;
import net.sf.saxon.expr.instruct.ForEach;
import net.sf.saxon.om.AttributeCollection;
import net.sf.saxon.om.Axis;
import net.sf.saxon.om.StandardNames;
import net.sf.saxon.value.Whitespace;
import net.sf.saxon.expr.sort.SortExpression;
import net.sf.saxon.expr.sort.SortKeyDefinition;
import net.sf.saxon.trans.XPathException;
import net.sf.saxon.trans.SaxonErrorCode;
import net.sf.saxon.type.ItemType;
import net.sf.saxon.value.Cardinality;


/**
* Handler for xsl:for-each elements in stylesheet. <br>
*/

public class XSLForEach extends StyleElement {

    private Expression select = null;
    private boolean containsTailCall = false;
    private int threads = 0;

    /**
    * Determine whether this node is an instruction.
    * @return true - it is an instruction
    */

    public boolean isInstruction() {
        return true;
    }

    /**
     * Specify that xsl:sort is a permitted child
     */

    protected boolean isPermittedChild(StyleElement child) {
        return (child instanceof XSLSort);
    }

    /**
     * Determine the type of item returned by this instruction (only relevant if
     * it is an instruction).
     * @return the item type returned
     */

    protected ItemType getReturnedItemType() {
        return getCommonChildItemType();
    }

    protected boolean markTailCalls() {
        if (Cardinality.allowsMany(select.getCardinality())) {
            return false;
        } else {
            StyleElement last = getLastChildInstruction();
            containsTailCall = last != null && last.markTailCalls();
            return containsTailCall;
        }
    }

    /**
    * Determine whether this type of element is allowed to contain a template-body
    * @return true: yes, it may contain a template-body
    */

    public boolean mayContainSequenceConstructor() {
        return true;
    }

    public void prepareAttributes() throws XPathException {

		AttributeCollection atts = getAttributeList();

		String selectAtt = null;

		for (int a=0; a<atts.getLength(); a++) {
			int nc = atts.getNameCode(a);
			String f = getNamePool().getClarkName(nc);
			if (f.equals(StandardNames.SELECT)) {
        		selectAtt = atts.getValue(a);
            } else if (f.equals(StandardNames.SAXON_THREADS)) {
                String threadsAtt = Whitespace.trim(atts.getValue(a));
                try {
                    threads = Integer.parseInt(threadsAtt);
                } catch (NumberFormatException err) {
                    compileError("saxon:threads must be an integer", "XTSE0020");
                }
                if (threads < 1) {
                    compileError("saxon:threads must be greater than zero", "XTSE0020");
                }
                if (getPreparedStylesheet().isCompileWithTracing()) {
                    compileWarning("saxon:threads - no multithreading takes place when compiling with trace enabled",
                            SaxonErrorCode.SXWN9012);
                    threads = 0;
                } else if (!"EE".equals(getConfiguration().getEditionCode())) {
                    compileWarning("saxon:threads - ignored when not running Saxon-EE",
                            SaxonErrorCode.SXWN9013);
                    threads = 0;
                }
            } else {
        		checkUnknownAttribute(nc);
        	}
        }

        if (selectAtt==null) {
            reportAbsence("select");
        } else {
            select = makeExpression(selectAtt);
        }

    }

    public void validate(Declaration decl) throws XPathException {
        checkSortComesFirst(false);
        select = typeCheck("select", select);
        if (!hasChildNodes()) {
            compileWarning("An empty xsl:for-each instruction has no effect", SaxonErrorCode.SXWN9009);
        }
    }

    public Expression compile(Executable exec, Declaration decl) throws XPathException {
        SortKeyDefinition[] sortKeys = makeSortKeys(decl);
        Expression sortedSequence = select;
        if (sortKeys != null) {
            sortedSequence = new SortExpression(select, sortKeys);
        }

        Expression block = compileSequenceConstructor(exec, decl, iterateAxis(Axis.CHILD), true);
        if (block == null) {
            // body of for-each is empty: it's a no-op.
            return Literal.makeEmptySequence();
        }
        try {
            return new ForEach(sortedSequence, makeExpressionVisitor().simplify(block), containsTailCall, threads);
        } catch (XPathException err) {
            compileError(err);
            return null;
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
// The Initial Developer of the Original Code is Michael H. Kay.
//
// Portions created by (your name) are Copyright (C) (your legal entity). All Rights Reserved.
//
// Contributor(s): none.
//
