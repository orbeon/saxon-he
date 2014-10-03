////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
// Copyright (c) 2014 Saxonica Limited.
// This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
// If a copy of the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
// This Source Code Form is "Incompatible With Secondary Licenses", as defined by the Mozilla Public License, v. 2.0.
////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

package net.sf.saxon.style;

import net.sf.saxon.expr.Expression;
import net.sf.saxon.expr.Literal;
import net.sf.saxon.expr.StringLiteral;
import net.sf.saxon.expr.instruct.ForEach;
import net.sf.saxon.expr.sort.SortExpression;
import net.sf.saxon.expr.sort.SortKeyDefinition;
import net.sf.saxon.lib.NamespaceConstant;
import net.sf.saxon.om.AttributeCollection;
import net.sf.saxon.om.AxisInfo;
import net.sf.saxon.om.StandardNames;
import net.sf.saxon.trans.SaxonErrorCode;
import net.sf.saxon.trans.XPathException;
import net.sf.saxon.type.ItemType;
import net.sf.saxon.value.Cardinality;
import net.sf.saxon.value.Whitespace;


/**
 * Handler for xsl:for-each elements in stylesheet. <br>
 */

public class XSLForEach extends StyleElement {

    /*@Nullable*/ private Expression select = null;
    private boolean containsTailCall = false;
    private Expression threads = null;

    /**
     * Determine whether this node is an instruction.
     *
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
     *
     * @return the item type returned
     */

    protected ItemType getReturnedItemType() {
        return getCommonChildItemType();
    }

    protected boolean markTailCalls() {
        assert select != null;
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
     *
     * @return true: yes, it may contain a template-body
     */

    public boolean mayContainSequenceConstructor() {
        return true;
    }

    public void prepareAttributes() throws XPathException {

        AttributeCollection atts = getAttributeList();

        String selectAtt = null;

        for (int a = 0; a < atts.getLength(); a++) {
            String f = atts.getQName(a);
            if (f.equals(StandardNames.SELECT)) {
                selectAtt = atts.getValue(a);
            } else if (atts.getLocalName(a).equals("threads") && atts.getURI(a).equals(NamespaceConstant.SAXON)) {
                String threadsAtt = Whitespace.trim(atts.getValue(a));
                threads = makeAttributeValueTemplate(threadsAtt);
                if (getCompilation().getCompilerInfo().isCompileWithTracing()) {
                    compileWarning("saxon:threads - no multithreading takes place when compiling with trace enabled",
                            SaxonErrorCode.SXWN9012);
                    threads = new StringLiteral("0", this);
                } else if (!"EE".equals(getConfiguration().getEditionCode())) {
                    compileWarning("saxon:threads - ignored when not running Saxon-EE",
                            SaxonErrorCode.SXWN9013);
                    threads = new StringLiteral("0", this);
                }
            } else {
                checkUnknownAttribute(atts.getNodeName(a));
            }
        }

        if (selectAtt == null) {
            reportAbsence("select");
            select = Literal.makeEmptySequence(this);
        } else {
            select = makeExpression(selectAtt);
        }

    }

    public void validate(ComponentDeclaration decl) throws XPathException {
        checkSortComesFirst(false);
        select = typeCheck("select", select);
        if (!hasChildNodes()) {
            compileWarning("An empty xsl:for-each instruction has no effect", SaxonErrorCode.SXWN9009);
        }
    }

    public Expression compile(Compilation compilation, ComponentDeclaration decl) throws XPathException {
        SortKeyDefinition[] sortKeys = makeSortKeys(compilation, decl);
        Expression sortedSequence = select;
        if (sortKeys != null) {
            sortedSequence = new SortExpression(select, sortKeys);
        }

        Expression block = compileSequenceConstructor(compilation, decl, iterateAxis(AxisInfo.CHILD), true);
        if (block == null) {
            // body of for-each is empty: it's a no-op.
            return Literal.makeEmptySequence(this);
        }
        try {
            return new ForEach(sortedSequence, makeExpressionVisitor().simplify(block), containsTailCall, threads);
        } catch (XPathException err) {
            compileError(err);
            return null;
        }
    }


}

