package net.sf.saxon.style;
import net.sf.saxon.expr.Expression;
import net.sf.saxon.expr.ExpressionVisitor;
import net.sf.saxon.expr.Literal;
import net.sf.saxon.expr.instruct.*;
import net.sf.saxon.om.*;
import net.sf.saxon.tree.iter.AxisIterator;
import net.sf.saxon.trans.XPathException;
import net.sf.saxon.type.AnyItemType;
import net.sf.saxon.value.Whitespace;
import net.sf.saxon.lib.NamespaceConstant;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
* An xsl:attribute-set element in the stylesheet. <br>
*/

public class XSLAttributeSet extends StyleElement implements StylesheetProcedure {

    private String nameAtt;
                // the name of the attribute set as written

    private String useAtt;
                // the value of the use-attribute-sets attribute, as supplied

    private SlotManager stackFrameMap;
                // needed if variables are used

    private List<Declaration> attributeSetElements = null;
                // list of Declarations of XSLAttributeSet objects referenced by this one

    private AttributeSet[] useAttributeSets = null;
                // compiled instructions for the attribute sets used by this one

    private AttributeSet procedure = new AttributeSet();
                // the compiled form of this attribute set

    private int referenceCount = 0;
                // the number of references to this attribute set

    private boolean validated = false;

    /**
     * Ask whether this node is a declaration, that is, a permitted child of xsl:stylesheet
     * (including xsl:include and xsl:import).
     * @return true for this element
     */

    @Override
    public boolean isDeclaration() {
        return true;
    }

    /**
     * Get the name of this attribute set
     * @return the name of the attribute set, as a QName
     */

    public StructuredQName getAttributeSetName() {
        return getObjectName();
    }

    /**
     * Get the compiled code produced for this XSLT element
     * @return the compiled AttributeSet
     */

    public AttributeSet getInstruction() {
        return procedure;
    }

    /**
     * Increment the number of references found to this attribute set
     */

    public void incrementReferenceCount() {
        referenceCount++;
    }

    public void prepareAttributes() throws XPathException {
		useAtt = null;

		AttributeCollection atts = getAttributeList();

		for (int a=0; a<atts.getLength(); a++) {
			int nc = atts.getNameCode(a);
			String f = getNamePool().getClarkName(nc);
			if (f.equals(StandardNames.NAME)) {
        		nameAtt = Whitespace.trim(atts.getValue(a));
        	} else if (f.equals(StandardNames.USE_ATTRIBUTE_SETS)) {
        		useAtt = atts.getValue(a);
        	} else {
        		checkUnknownAttribute(nc);
        	}
        }

        if (nameAtt==null) {
            reportAbsence("name");
            setObjectName(new StructuredQName("", "", "attribute-set-error-name"));
            return;
        }

        try {
            setObjectName(makeQName(nameAtt));
        } catch (NamespaceException err) {
            compileError(err.getMessage(), "XTSE0280");
            setObjectName(new StructuredQName("", "", "attribute-set-error-name"));
        } catch (XPathException err) {
            compileError(err.getMessage(), err.getErrorCodeQName());
            setObjectName(new StructuredQName("", "", "attribute-set-error-name"));
        }

    }

    /**
     * Get a name identifying the object of the expression, for example a function name, template name,
     * variable name, key name, element name, etc. This is used only where the name is known statically.
     * If there is no name, the value will be null.
     * @return the name of the object declared in this element, if any
     */

    public StructuredQName getObjectName() {
        StructuredQName o = super.getObjectName();
        if (o == null) {
            try {
                prepareAttributes();
                o = getObjectName();
            } catch (XPathException err) {
                o = new StructuredQName("saxon", NamespaceConstant.SAXON, "badly-named-attribute-set");
                setObjectName(o);
            }
        }
        return o;
    }

    public void validate(Declaration decl) throws XPathException {

        if (validated) return;

        checkTopLevel(null);

        stackFrameMap = getConfiguration().makeSlotManager();

        AxisIterator kids = iterateAxis(Axis.CHILD);
        while (true) {
            Item child = kids.next();
            if (child == null) {
                break;
            }
            if (!(child instanceof XSLAttribute)) {
                compileError("Only xsl:attribute is allowed within xsl:attribute-set", "XTSE0010");
            }
        }

        if (useAtt!=null) {
            // identify any attribute sets that this one refers to

            attributeSetElements = new ArrayList<Declaration>(5);
            useAttributeSets = getAttributeSets(useAtt, attributeSetElements);

            // check for circularity

            for (Iterator<Declaration> it=attributeSetElements.iterator(); it.hasNext();) {
                ((XSLAttributeSet)it.next().getSourceElement()).checkCircularity(this);
            }
        }

        validated = true;
    }

    /**
     * Check for circularity: specifically, check that this attribute set does not contain
     * a direct or indirect reference to the one supplied as a parameter
     * @param origin the place from which the search started
    */

    public void checkCircularity(XSLAttributeSet origin) throws XPathException {
        if (this==origin) {
            compileError("The definition of the attribute set is circular", "XTSE0720");
            useAttributeSets = null;
        } else {
            if (!validated) {
                // if this attribute set isn't validated yet, we don't check it.
                // The circularity will be detected when the last attribute set in the cycle
                // gets validated
                return;
            }
            if (attributeSetElements != null) {
                for (Iterator<Declaration> it=attributeSetElements.iterator(); it.hasNext();) {
                    ((XSLAttributeSet)it.next().getSourceElement()).checkCircularity(origin);
                }
            }
        }
    }

    /**
    * Get details of stack frame
    */

    public SlotManager getSlotManager() {
        return stackFrameMap;
    }
    /**
     * Compile the attribute set
     * @param exec the Executable
     * @param decl
     * @return a Procedure object representing the compiled attribute set
     * @throws XPathException if a failure is detected
     */
    public Expression compile(Executable exec, Declaration decl) throws XPathException {
        if (referenceCount > 0 ) {
            Expression body = compileSequenceConstructor(exec, decl, iterateAxis(Axis.CHILD), true);
            if (body == null) {
                body = Literal.makeEmptySequence();
            }

            try {

                ExpressionVisitor visitor = makeExpressionVisitor();
                body = visitor.simplify(body);
                if (getConfiguration().isCompileWithTracing()) {
                    TraceWrapper trace = new TraceInstruction(body, this);
                    trace.setLocationId(allocateLocationId(getSystemId(), getLineNumber()));
                    trace.setContainer(procedure);
                    body = trace;
                }

                procedure.setUseAttributeSets(useAttributeSets);
                procedure.setName(getObjectName());
                procedure.setBody(body);
                procedure.setSystemId(getSystemId());
                procedure.setLineNumber(getLineNumber());
                procedure.setExecutable(exec);

                Expression exp2 = body.optimize(visitor, AnyItemType.getInstance());
                if (body != exp2) {
                    procedure.setBody(exp2);
                    body = exp2;
                }

                super.allocateSlots(body);
                procedure.setStackFrameMap(stackFrameMap);
            } catch (XPathException e) {
                compileError(e);
            }
        }
        return null;
    }

    /**
     * Optimize the stylesheet construct
     * @param declaration
     */

    public void optimize(Declaration declaration) throws XPathException {
        // Already done earlier
    }

    /**
     * Get the type of construct. This will be a constant in
     * class {@link net.sf.saxon.trace.Location}. This method is part of
     * the {@link net.sf.saxon.trace.InstructionInfo} interface
     */

    public int getConstructType() {
        return StandardNames.XSL_ATTRIBUTE_SET;
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
