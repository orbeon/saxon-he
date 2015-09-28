////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
// Copyright (c) 2014 Saxonica Limited.
// This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
// If a copy of the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
// This Source Code Form is "Incompatible With Secondary Licenses", as defined by the Mozilla Public License, v. 2.0.
////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

package net.sf.saxon.style;

import com.saxonica.ee.stream.Sweep;
import net.sf.saxon.Configuration;
import net.sf.saxon.expr.Component;
import net.sf.saxon.expr.Expression;
import net.sf.saxon.expr.Literal;
import net.sf.saxon.expr.instruct.AttributeSet;
import net.sf.saxon.expr.instruct.SlotManager;
import net.sf.saxon.expr.parser.ContextItemStaticInfo;
import net.sf.saxon.expr.parser.ExpressionVisitor;
import net.sf.saxon.expr.parser.Optimizer;
import net.sf.saxon.lib.NamespaceConstant;
import net.sf.saxon.om.*;
import net.sf.saxon.trans.SymbolicName;
import net.sf.saxon.trans.Visibility;
import net.sf.saxon.trans.XPathException;
import net.sf.saxon.tree.iter.AxisIterator;
import net.sf.saxon.type.AnyItemType;
import net.sf.saxon.value.Whitespace;

import java.util.ArrayList;
import java.util.List;

/**
 * An xsl:attribute-set element in the stylesheet. <br>
 */

public class XSLAttributeSet extends StyleElement implements StylesheetComponent {

    private String nameAtt;
    // the name of the attribute set as written

    /*@Nullable*/ private String useAtt;
    // the value of the use-attribute-sets attribute, as supplied

    private String visibilityAtt;
    // the value of the visibility attribute, trimmed

    private SlotManager stackFrameMap;
    // needed if variables are used

    private List<ComponentDeclaration> attributeSetElements = null;
    // list of Declarations of XSLAttributeSet objects referenced by this one

    private AttributeSet[] useAttributeSets = null;
    // compiled instructions for the attribute sets used by this one

    private AttributeSet procedure = new AttributeSet();
    // the compiled form of this attribute set

    private int referenceCount = 0;
    // the number of references to this attribute set

    private boolean validated = false;
    private Visibility visibility;
    private boolean streamable = false;

    /**
     * Get the corresponding Procedure object that results from the compilation of this
     * StylesheetProcedure
     */
    public AttributeSet getCompiledProcedure() {
        return procedure;
    }

    public SymbolicName getSymbolicName() {
        return new SymbolicName(StandardNames.XSL_ATTRIBUTE_SET, getObjectName());
    }

    public void checkCompatibility(Component component) throws XPathException {
        // TODO: implement me
    }

    @Override
    public void setCompilation(Compilation compilation) {
        super.setCompilation(compilation);
        procedure.setPackageData(compilation.getPackageData());
    }

    /**
     * Ask whether this node is a declaration, that is, a permitted child of xsl:stylesheet
     * (including xsl:include and xsl:import).
     *
     * @return true for this element
     */

    @Override
    public boolean isDeclaration() {
        return true;
    }

    /**
     * Get the name of this attribute set
     *
     * @return the name of the attribute set, as a QName
     */

    public StructuredQName getAttributeSetName() {
        return getObjectName();
    }

    /**
     * Get the compiled code produced for this XSLT element
     *
     * @return the compiled AttributeSet
     */

    public AttributeSet getInstruction() {
        return procedure;
    }

    /**
     * Ask whether the attribute set is declared streamable
     * @return true if the attribute streamable="yes" is present
     */

    public boolean isDeclaredStreamable() {
        return streamable;
    }

    /**
     * Increment the number of references found to this attribute set
     */

    public void incrementReferenceCount() {
        referenceCount++;
    }

    public void prepareAttributes() throws XPathException {
        useAtt = null;
        String streamableAtt = null;

        AttributeCollection atts = getAttributeList();

        for (int a = 0; a < atts.getLength(); a++) {
            String f = atts.getQName(a);
            if (f.equals("name")) {
                nameAtt = Whitespace.trim(atts.getValue(a));
            } else if (f.equals("use-attribute-sets")) {
                useAtt = atts.getValue(a);
            } else if (f.equals("streamable")) {
                streamableAtt = Whitespace.trim(atts.getValue(a));
            } else if (f.equals("visibility")) {
                visibilityAtt = Whitespace.trim(atts.getValue(a));
            } else {
                checkUnknownAttribute(atts.getNodeName(a));
            }
        }

        if (nameAtt == null) {
            reportAbsence("name");
            setObjectName(new StructuredQName("", "", "attribute-set-error-name"));
            return;
        }

        if (visibilityAtt == null) {
            visibility = Visibility.PRIVATE;
        } else if (!isXslt30Processor()) {
            compileError("The xsl:attribute-set/@visibility attribute requires XSLT 3.0 to be enabled", "XTSE0020");
        } else {
            visibility = getVisibilityValue(visibilityAtt, "");
        }

        if (streamableAtt != null) {
            streamable = processBooleanAttribute("streamable", streamableAtt);
            if (streamable && !getConfiguration().isLicensedFeature(Configuration.LicenseFeature.ENTERPRISE_XSLT)) {
                issueWarning("Request for streaming ignored: this Saxon configuration does not support streaming", this);
                streamable = false;
            }
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
     *
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

    public void validate(ComponentDeclaration decl) throws XPathException {

        if (validated) {
            return;
        }

        checkTopLevel("XTSE0010", true);

        stackFrameMap = getConfiguration().makeSlotManager();

        AxisIterator kids = iterateAxis(AxisInfo.CHILD);
        NodeInfo child;
        while ((child = kids.next()) != null) {
            if (!(child instanceof XSLAttribute)) {
                compileError("Only xsl:attribute is allowed within xsl:attribute-set", "XTSE0010");
            } else {
                if (visibility == Visibility.ABSTRACT) {
                    compileError("An abstract attribute-set must contain no xsl:attribute instructions");
                }
            }
        }

        if (useAtt != null) {
            if (visibility == Visibility.ABSTRACT) {
                compileError("An abstract attribute-set must have no @use-attribute-sets attribute");
            }

            // identify any attribute sets that this one refers to

            attributeSetElements = new ArrayList<ComponentDeclaration>(5);
            useAttributeSets = getAttributeSets(useAtt, attributeSetElements);

            // check for circularity

            for (ComponentDeclaration attributeSetElement : attributeSetElements) {
                ((XSLAttributeSet) attributeSetElement.getSourceElement()).checkCircularity(this);
            }

            // check for consistency of streamability attribute

            if (streamable) {
                for (ComponentDeclaration attributeSetElement : attributeSetElements) {
                    if (!((XSLAttributeSet) attributeSetElement.getSourceElement()).streamable) {
                        compileError("Attribute set is declared streamable, " +
                                "but references an attribute set that is not declared streamable", "XTSE0730");
                    }
                }
            }

        }

        validated = true;
    }


    public void index(ComponentDeclaration decl, StylesheetPackage top) throws XPathException {
        top.indexAttributeSet(decl);
    }


    /**
     * Check for circularity: specifically, check that this attribute set does not contain
     * a direct or indirect reference to the one supplied as a parameter
     *
     * @param origin the place from which the search started
     * @throws net.sf.saxon.trans.XPathException
     *          if an error is found
     */

    public void checkCircularity(XSLAttributeSet origin) throws XPathException {
        if (this == origin) {
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
                for (ComponentDeclaration attributeSetElement : attributeSetElements) {
                    XSLAttributeSet element = (XSLAttributeSet) attributeSetElement.getSourceElement();
                    element.checkCircularity(origin);
                    if (streamable && !element.streamable) {
                        compileError("Attribute-set is declared streamable but references a non-streamable attribute set " +
                                element.getAttributeSetName().getDisplayName(), "XTSE3430");
                    }
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
     *
     * @param compilation the current compilation episode
     * @param decl        this attribute set declaration
     * @throws XPathException if a failure is detected
     */
    public void compileDeclaration(Compilation compilation, ComponentDeclaration decl) throws XPathException {
        if (referenceCount > 0) {
            Expression body = compileSequenceConstructor(compilation, decl, iterateAxis(AxisInfo.CHILD), true);
            if (body == null) {
                body = Literal.makeEmptySequence(this);
            }

            try {

                ExpressionVisitor visitor = makeExpressionVisitor();
                body = visitor.simplify(body);
                if (getConfiguration().isCompileWithTracing()) {
                    body = makeTraceInstruction(this, body);
//                    trace.setLocationId(allocateLocationId(getSystemId(), getLineNumber()));
//                    trace.setContainer(procedure);
//                    body = trace;
                }

                procedure.setUseAttributeSets(useAttributeSets);
                procedure.setName(getObjectName());
                procedure.setBody(body);
                procedure.setSystemId(getSystemId());
                procedure.setLineNumber(getLineNumber());
                procedure.setDeclaredStreamable(streamable);
                //procedure.setExecutable(compilation);

                Expression exp2 = body.optimize(visitor, new ContextItemStaticInfo(AnyItemType.getInstance(), true));
                if (body != exp2) {
                    procedure.setBody(exp2);
                    body = exp2;
                }

                super.allocateSlots(body);
                procedure.setStackFrameMap(stackFrameMap);
                checkStreamability();
            } catch (XPathException e) {
                compileError(e);
            }
        } else {
            procedure.setBody(Literal.makeEmptySequence(this));
        }
    }

    /**
     * Optimize the stylesheet construct
     *
     * @param declaration this attribute set declaration
     */

    public void optimize(ComponentDeclaration declaration) throws XPathException {
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

    /**
     * Generate byte code if appropriate
     *
     * @param opt the optimizer
     */
    public void generateByteCode(Optimizer opt) {}

    private void checkStreamability() throws XPathException {
//#ifdefined STREAM
         if (streamable) {
             ContextItemStaticInfo info = new ContextItemStaticInfo(AnyItemType.getInstance(), false, true);
             procedure.getBody().getStreamability(false, info, null);
             if (procedure.getBody().getSweep() != Sweep.MOTIONLESS) {
                 compileError("The attribute set is declared streamable but it is not motionless", "XTSE3430");
             }
         }
//#endif
     }



}

