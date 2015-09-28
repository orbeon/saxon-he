////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
// Copyright (c) 2014 Saxonica Limited.
// This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
// If a copy of the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
// This Source Code Form is "Incompatible With Secondary Licenses", as defined by the Mozilla Public License, v. 2.0.
////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

package net.sf.saxon.style;

import net.sf.saxon.expr.*;
import net.sf.saxon.expr.instruct.ForEachGroup;
import net.sf.saxon.expr.parser.ExpressionTool;
import net.sf.saxon.expr.parser.ExpressionVisitor;
import net.sf.saxon.expr.parser.RoleLocator;
import net.sf.saxon.expr.parser.TypeChecker;
import net.sf.saxon.lib.NamespaceConstant;
import net.sf.saxon.lib.StringCollator;
import net.sf.saxon.om.AttributeCollection;
import net.sf.saxon.om.AxisInfo;
import net.sf.saxon.om.NodeInfo;
import net.sf.saxon.om.StandardNames;
import net.sf.saxon.pattern.Pattern;
import net.sf.saxon.pattern.PatternSponsor;
import net.sf.saxon.trans.SaxonErrorCode;
import net.sf.saxon.trans.XPathException;
import net.sf.saxon.tree.iter.AxisIterator;
import net.sf.saxon.value.DecimalValue;
import net.sf.saxon.value.SequenceType;
import net.sf.saxon.value.Whitespace;

import java.net.URI;
import java.net.URISyntaxException;

/**
 * Handler for xsl:for-each-group elements in stylesheet. This is a new instruction
 * defined in XSLT 2.0
 */

public final class XSLForEachGroup extends StyleElement {

    /*@Nullable*/ private Expression select = null;
    private Expression groupBy = null;
    private Expression groupAdjacent = null;
    private Pattern starting = null;
    private Pattern ending = null;
    private Expression collationName;
    private boolean composite = false;

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
        return child instanceof XSLSort;
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
        String groupByAtt = null;
        String groupAdjacentAtt = null;
        String startingAtt = null;
        String endingAtt = null;
        String collationAtt = null;

        for (int a = 0; a < atts.getLength(); a++) {
            String f = atts.getQName(a);
            if (f.equals(StandardNames.SELECT)) {
                selectAtt = atts.getValue(a);
            } else if (f.equals(StandardNames.GROUP_BY)) {
                groupByAtt = atts.getValue(a);
            } else if (f.equals(StandardNames.GROUP_ADJACENT)) {
                groupAdjacentAtt = atts.getValue(a);
            } else if (f.equals(StandardNames.GROUP_STARTING_WITH)) {
                startingAtt = atts.getValue(a);
            } else if (f.equals(StandardNames.GROUP_ENDING_WITH)) {
                endingAtt = atts.getValue(a);
            } else if (f.equals(StandardNames.COLLATION)) {
                collationAtt = Whitespace.trim(atts.getValue(a));
            } else if (f.equals("bind-group")) {
                compileError("The bind-group attribute has been dropped from the XSLT 3.0 specification", "XTSE0090");
            } else if (f.equals("bind-grouping-key")) {
                compileError("The bind-grouping-key attribute has been dropped from the XSLT 3.0 specification", "XTSE0090");
            } else if (f.equals("composite")) {
                if (!isXslt30Processor()) {
                    compileError("The 'composite' attribute requires XSLT 3.0");
                }
                composite = processBooleanAttribute("composite", atts.getValue(a));
            } else {
                checkUnknownAttribute(atts.getNodeName(a));
            }
        }

        if (selectAtt == null) {
            reportAbsence("select");
            select = Literal.makeEmptySequence(this); // for error recovery
        } else {
            select = makeExpression(selectAtt);
        }

        int c = (groupByAtt == null ? 0 : 1) +
                (groupAdjacentAtt == null ? 0 : 1) +
                (startingAtt == null ? 0 : 1) +
                (endingAtt == null ? 0 : 1);
        if (c != 1) {
            compileError("Exactly one of the attributes group-by, group-adjacent, group-starting-with, " +
                    "and group-ending-with must be specified", "XTSE1080");
        }

        if (groupByAtt != null) {
            groupBy = makeExpression(groupByAtt);
        }

        if (groupAdjacentAtt != null) {
            groupAdjacent = makeExpression(groupAdjacentAtt);
        }

        if (startingAtt != null) {
            starting = makePattern(startingAtt);
        }

        if (endingAtt != null) {
            ending = makePattern(endingAtt);
        }

        if (collationAtt != null) {
            if (groupBy == null && groupAdjacent == null) {
                compileError("A collation may be specified only if group-by or group-adjacent is specified", "XTSE1090");
            } else {
                collationName = makeAttributeValueTemplate(collationAtt);
                if (collationName instanceof StringLiteral) {
                    String collation = ((StringLiteral) collationName).getStringValue();
                    URI collationURI;
                    try {
                        collationURI = new URI(collation);
                        if (!collationURI.isAbsolute()) {
                            URI base = new URI(getBaseURI());
                            collationURI = base.resolve(collationURI);
                            collationName = new StringLiteral(collationURI.toString(), this);
                        }
                    } catch (URISyntaxException err) {
                        compileError("Collation name '" + collationName + "' is not a valid URI", "XTDE1110");
                        collationName = new StringLiteral(NamespaceConstant.CODEPOINT_COLLATION_URI, this);
                    }
                }
            }
        } else {
            String defaultCollation = getDefaultCollationName();
            if (defaultCollation != null) {
                collationName = new StringLiteral(defaultCollation, this);
            }
        }

        if (composite && (starting != null || ending != null)) {
            compileError("The composite attribute cannot be used with " +
                    (starting == null ? "grouping-ending-with" : "group-starting-with"), "XTSE1090");
        }
    }

    public void validate(ComponentDeclaration decl) throws XPathException {
        checkSortComesFirst(false);
        select = typeCheck("select", select);

        ExpressionVisitor visitor = makeExpressionVisitor();
        if (groupBy != null) {
            groupBy = typeCheck("group-by", groupBy);
            try {
                RoleLocator role =
                        new RoleLocator(RoleLocator.INSTRUCTION, "xsl:for-each-group/group-by", 0);
                groupBy = TypeChecker.staticTypeCheck(groupBy,
                        SequenceType.ATOMIC_SEQUENCE,
                        false, role, visitor);
            } catch (XPathException err) {
                compileError(err);
            }
        } else if (groupAdjacent != null) {
            groupAdjacent = typeCheck("group-adjacent", groupAdjacent);
            try {
                RoleLocator role =
                        new RoleLocator(RoleLocator.INSTRUCTION, "xsl:for-each-group/group-adjacent", 0);
                role.setErrorCode("XTTE1100");
                groupAdjacent = TypeChecker.staticTypeCheck(groupAdjacent,
                        composite ? SequenceType.ATOMIC_SEQUENCE : SequenceType.SINGLE_ATOMIC,
                        false, role, visitor);
            } catch (XPathException err) {
                compileError(err);
            }
        }

        starting = typeCheck("starting", starting);
        ending = typeCheck("ending", ending);

        if ((starting != null || ending != null) && !visitor.getStaticContext().getXPathLanguageLevel().equals(DecimalValue.THREE)) {
            try {
                RoleLocator role =
                        new RoleLocator(RoleLocator.INSTRUCTION, "xsl:for-each-group/select", 0);
                role.setErrorCode("XTTE1120");
                select = TypeChecker.staticTypeCheck(select,
                        SequenceType.NODE_SEQUENCE,
                        false, role, visitor);
            } catch (XPathException err) {
                String prefix = starting != null ?
                        "With group-starting-with attribute: " :
                        "With group-ending-with attribute: ";
                compileError(prefix + err.getMessage(), err.getErrorCodeQName());
            }
        }
        if (!hasChildNodes()) {
            compileWarning("An empty xsl:for-each-group instruction has no effect", SaxonErrorCode.SXWN9009);
        }

    }

    @Override
    public void postValidate() throws XPathException {
        // Check that the variables bound in the xsl:for-each-group are not used inappropriately in any child xsl:sort
        AxisIterator kids = iterateAxis(AxisInfo.CHILD);
        while (true) {
            NodeInfo child = kids.next();
            if (child == null) {
                break;
            }
        }
    }

    public Expression compile(Compilation compilation, ComponentDeclaration decl) throws XPathException {

        StringCollator collator = null;
        if (collationName instanceof StringLiteral) {
            // if the collation name is constant, then we've already resolved it against the base URI
            final String uri = ((StringLiteral) collationName).getStringValue();
            collator = findCollation(uri, getBaseURI());
            if (collator == null) {
                compileError("The collation name '" + collationName + "' has not been defined", "XTDE1110");
            }
        }

        byte algorithm = 0;
        Expression key = null;
        if (groupBy != null) {
            algorithm = ForEachGroup.GROUP_BY;
            key = groupBy;
        } else if (groupAdjacent != null) {
            algorithm = ForEachGroup.GROUP_ADJACENT;
            key = groupAdjacent;
        } else if (starting != null) {
            algorithm = ForEachGroup.GROUP_STARTING;
            key = new PatternSponsor(starting);
        } else if (ending != null) {
            algorithm = ForEachGroup.GROUP_ENDING;
            key = new PatternSponsor(ending);
        }

        Expression action = compileSequenceConstructor(compilation, decl, iterateAxis(AxisInfo.CHILD), true);
        if (action == null) {
            // body of for-each is empty: it's a no-op.
            return Literal.makeEmptySequence(this);
        }
        try {

            ForEachGroup instr = new ForEachGroup(select,
                    makeExpressionVisitor().simplify(action),
                    algorithm,
                    key,
                    collator,
                    collationName,
                    ExpressionTool.getBaseURI(staticContext, action, true),
                    makeSortKeys(compilation, decl));

            instr.setComposite(composite);
            return instr;
        } catch (XPathException e) {
            compileError(e);
            return null;
        }

    }

}

