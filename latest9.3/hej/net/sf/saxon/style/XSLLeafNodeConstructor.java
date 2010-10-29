package net.sf.saxon.style;

import net.sf.saxon.expr.*;
import net.sf.saxon.functions.SystemFunction;
import net.sf.saxon.expr.instruct.Executable;
import net.sf.saxon.expr.instruct.SimpleNodeConstructor;
import net.sf.saxon.om.Axis;
import net.sf.saxon.om.NodeInfo;
import net.sf.saxon.tree.iter.AxisIterator;
import net.sf.saxon.trans.XPathException;
import net.sf.saxon.type.BuiltInAtomicType;
import net.sf.saxon.type.Type;
import net.sf.saxon.value.StringValue;

/**
 * Common superclass for XSLT elements whose content template produces a text
 * value: xsl:text, xsl:value-of, xsl:attribute, xsl:comment, and xsl:processing-instruction
 */

public abstract class XSLLeafNodeConstructor extends StyleElement {

    //protected String stringValue = null;
    protected Expression select = null;

    /**
     * Determine whether this node is an instruction.
     *
     * @return true - it is an instruction
     */

    public boolean isInstruction() {
        return true;
    }

    /**
     * Determine whether this type of element is allowed to contain a template-body
     *
     * @return true: yes, it may contain a template-body
     */

    public boolean mayContainSequenceConstructor() {
        return true;
    }

    public void validate(Declaration decl) throws XPathException {
        if (select != null && hasChildNodes()) {
            String errorCode = getErrorCodeForSelectPlusContent();
            compileError("An " + getDisplayName() + " element with a select attribute must be empty", errorCode);
        }
        AxisIterator kids = iterateAxis(Axis.CHILD);
        NodeInfo first = (NodeInfo)kids.next();
        if (select == null) {
            if (first == null) {
                // there are no child nodes and no select attribute
                //stringValue = "";
                select = new StringLiteral(StringValue.EMPTY_STRING);
            } else {
                if (kids.next() == null) {
                    // there is exactly one child node
                    if (first.getNodeKind() == Type.TEXT) {
                        // it is a text node: optimize for this case
                        select = new StringLiteral(first.getStringValue());
                    }
                }
            }
        }
    }

    /**
     * Get the error code to be returned when the element has a select attribute but is not empty.
     * @return the error code defined for this condition, for this particular instruction
     */

    protected abstract String getErrorCodeForSelectPlusContent();

    protected void compileContent(Executable exec, Declaration decl, SimpleNodeConstructor inst, Expression separator) throws XPathException {
        if (separator == null) {
            separator = new StringLiteral(StringValue.SINGLE_SPACE);
        }
        try {
            if (select == null) {
                select = compileSequenceConstructor(exec, decl, iterateAxis(Axis.CHILD), true);
            }
            select = makeSimpleContentConstructor(select, separator);
            inst.setSelect(select, exec.getConfiguration());

        } catch (XPathException err) {
            compileError(err);
        }
    }

    /**
     * Construct an expression that implements the rules of "constructing simple content":
     * given an expression to select the base sequence, and an expression to compute the separator,
     * build an (unoptimized) expression to produce the value of the node as a string.
     * @param select the expression that selects the base sequence
     * @param separator the expression that computes the separator
     * @return an expression that returns a string containing the string value of the constructed node
     */

    public static Expression makeSimpleContentConstructor(Expression select, Expression separator) {
        // Merge adjacent text nodes
        select = new AdjacentTextNodeMerger(select);
        // Atomize the result
        select = new Atomizer(select);
        // Convert each atomic value to a string
        select = new AtomicSequenceConverter(select, BuiltInAtomicType.STRING);
        // Join the resulting strings with a separator
        select = SystemFunction.makeSystemFunction("string-join", new Expression[]{select, separator});
        // All that's left for the instruction to do is to construct the right kind of node
        return select;
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
