////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
// Copyright (c) 2018 Saxonica Limited.
// This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
// If a copy of the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
// This Source Code Form is "Incompatible With Secondary Licenses", as defined by the Mozilla Public License, v. 2.0.
////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

package net.sf.saxon.style;

import net.sf.saxon.expr.Component;
import net.sf.saxon.expr.Expression;
import net.sf.saxon.expr.Literal;
import net.sf.saxon.expr.instruct.Actor;
import net.sf.saxon.expr.instruct.GlobalParam;
import net.sf.saxon.expr.instruct.GlobalVariable;
import net.sf.saxon.expr.instruct.SlotManager;
import net.sf.saxon.expr.parser.*;
import net.sf.saxon.om.GroundedValue;
import net.sf.saxon.om.StandardNames;
import net.sf.saxon.om.StructuredQName;
import net.sf.saxon.trace.LocationKind;
import net.sf.saxon.trans.SymbolicName;
import net.sf.saxon.trans.XPathException;
import net.sf.saxon.type.AnyItemType;
import net.sf.saxon.type.Type;
import net.sf.saxon.type.TypeHierarchy;
import net.sf.saxon.value.SequenceType;

/**
 * Handler for xsl:variable elements appearing as a child of xsl:stylesheet. <br>
 * The xsl:variable element has mandatory attribute name and optional attribute select
 */

public class XSLGlobalVariable extends StyleElement implements StylesheetComponent {

    private SlotManager slotManager; // used to manage local variables declared inside this global variable
    protected SourceBinding sourceBinding = new SourceBinding(this);
    protected GlobalVariable compiledVariable = null;

    /**
     * Get the source binding object that holds information about the declared variable.
     * @return the source binding
     */

    public SourceBinding getSourceBinding() {
        return sourceBinding;
    }

    public StructuredQName getVariableQName() {
        return sourceBinding.getVariableQName();
    }

    public StructuredQName getObjectName() {
        return sourceBinding.getVariableQName();
    }

    /**
     * Determine whether this type of element is allowed to contain a template-body
     *
     * @return true: yes, it may contain a template-body
     */

    public boolean mayContainSequenceConstructor() {
        return true;
    }

    /**
     * Test whether this is a global variable or parameter
     *
     * @return true if this is global
     */

    public boolean isGlobal() {
        return isTopLevel();
        // might be called before the "global" field is initialized
    }

    /**
     * Hook to allow additional validation of a parent element immediately after its
     * children have been validated.
     */

    public void postValidate() throws XPathException {
        sourceBinding.postValidate();
    }

    /**
     * Get the type of construct. This will be a constant in
     * class {@link LocationKind}. This method is part of the
     * {@link net.sf.saxon.trace.InstructionInfo} interface
     */

    public int getConstructType() {
        return StandardNames.XSL_VARIABLE;
    }

    public GlobalVariable getCompiledVariable() {
        return compiledVariable;
    }



    public XSLGlobalVariable() {
        sourceBinding.setProperty(SourceBinding.GLOBAL, true);
    }

    protected int getPermittedAttributes() {
        return
                SourceBinding.ASSIGNABLE |
                        SourceBinding.SELECT |
                        SourceBinding.AS |
                        SourceBinding.STATIC |
                        SourceBinding.VISIBILITY;
    }

    private int state = 0;
    // 0 = before prepareAttributes()
    // 1 = during prepareAttributes()
    // 2 = after prepareAttributes()

    protected boolean redundant = false;

    /**
     * Get the corresponding Procedure object that results from the compilation of this
     * StylesheetProcedure
     */
    public Actor getActor() throws XPathException {
        GlobalVariable gv = getCompiledVariable();
        if (gv == null) {
            gv = this instanceof XSLGlobalParam ? new GlobalParam() : new GlobalVariable();
            gv.setPackageData(getCompilation().getPackageData());
            gv.makeDeclaringComponent(getVisibility(), getContainingPackage());
            gv.setRequiredType(sourceBinding.getDeclaredType());
            gv.setDeclaredVisibility(getDeclaredVisibility());
            gv.setVariableQName(sourceBinding.getVariableQName());
            gv.setSystemId(getSystemId());
            gv.setLineNumber(getLineNumber());
            RetainedStaticContext rsc = makeRetainedStaticContext();
            gv.setRetainedStaticContext(rsc);
            if (gv.getBody() != null) {
                gv.getBody().setRetainedStaticContext(rsc);
            }
            compiledVariable = gv;
        }
        return gv;
    }

    public SymbolicName getSymbolicName() {
        return new SymbolicName(StandardNames.XSL_VARIABLE, getObjectName());
    }

    public void checkCompatibility(Component component) throws XPathException {
        GlobalVariable other = (GlobalVariable) component.getActor();
        TypeHierarchy th = component.getDeclaringPackage().getConfiguration().getTypeHierarchy();
        int relation = th.sequenceTypeRelationship(getRequiredType(), other.getRequiredType());
        if (relation != TypeHierarchy.SAME_TYPE) {
            compileError(
                "The declared type of the overriding variable $" + getVariableQName().getDisplayName() +
                    " is different from that of the overridden variable",
                "XTSE3070");
        }
    }

    /**
     * Ask whether this element contains a binding for a variable with a given name; and if it does,
     * return the source binding information
     *
     * @param name the variable name
     * @return the binding information if this element binds a variable of this name; otherwise null
     */

    public SourceBinding getBindingInformation(StructuredQName name) {
        if (name.equals(sourceBinding.getVariableQName())) {
            return sourceBinding;
        } else {
            return null;
        }
    }

    public void prepareAttributes() throws XPathException {
        if (state == 2) {
            return;
        }
        if (state == 1) {
            compileError("Circular reference to variable", "XTDE0640");
        }
        state = 1;
        //System.err.println("Prepare attributes of $" + getVariableName());

        sourceBinding.prepareAttributes(getPermittedAttributes());
        state = 2;
    }

    @Override
    public void index(ComponentDeclaration decl, PrincipalStylesheetModule top) throws XPathException {
        top.indexVariableDeclaration(decl);
    }

    @Override
    public void validate(ComponentDeclaration decl) throws XPathException {
        slotManager = getConfiguration().makeSlotManager();
        sourceBinding.validate();
    }

    /**
     * Ask whether the global variable is declared with assignable="yes"
     *
     * @return true if assignabl="yes" was specified
     */

    public boolean isAssignable() {
        return sourceBinding.hasProperty(SourceBinding.ASSIGNABLE);
    }

    /**
     * Determine whether this node is a declaration.
     *
     * @return true - a global variable is a declaration
     */

    @Override
    public boolean isDeclaration() {
        return true;
    }

    /**
     * Determine whether this node is an instruction.
     *
     * @return false - a global variable is not an instruction
     */

    public boolean isInstruction() {
        return false;
    }

    /**
     * Get the static type of the variable. This is the declared type, unless the value
     * is statically known and constant, in which case it is the actual type of the value.
     * @return the declared or inferred static type of the variable
     */

    public SequenceType getRequiredType() {
        return sourceBinding.getInferredType(true);
    }

    @Override
    public void fixupReferences() throws XPathException {
        sourceBinding.fixupReferences(compiledVariable);
        super.fixupReferences();
    }

    /**
     * Compile.
     * This method ensures space is available for local variables declared within
     * this global variable
     */

    public void compileDeclaration(Compilation compilation, ComponentDeclaration decl) throws XPathException {

        // Commented out: Can't eliminate unused variables at this stage because they might be xsl:expose'd as public
//        boolean unused = sourceBinding.getReferences().isEmpty() && !isAssignable() &&
//                getVisibility() == Visibility.PRIVATE;
//        if (unused && !compilation.getCompilerInfo().isCompileWithTracing()) {
//            redundant = true;
//            // Remove the global variable from the package (otherwise a failure can occur
//            // when pre-evaluating global variables)
//            compilation.getPrincipalStylesheetModule().removeGlobalVariable(decl);
//        }

        if (!redundant) {
            sourceBinding.handleSequenceConstructor(compilation, decl);
            GlobalVariable inst = getCompiledVariable();
            if (inst == null) {
                inst = new GlobalVariable();
                inst.setPackageData(getCompilation().getPackageData());
                inst.makeDeclaringComponent(getVisibility(), getContainingPackage());
                inst.setVariableQName(sourceBinding.getVariableQName());
            }
            if (sourceBinding.isStatic()) {
                GroundedValue value = compilation.getStaticVariable(sourceBinding.getVariableQName());
                if (value == null) {
                    throw new AssertionError();
                }
                Expression select = Literal.makeLiteral(value);
                select.setRetainedStaticContext(makeRetainedStaticContext());
                inst.setSelectExpression(select);
            } else {
                Expression select = sourceBinding.getSelectExpression();
                inst.setSelectExpression(select);
            }
            inst.setRetainedStaticContext(makeRetainedStaticContext());
            initializeBinding(inst);
            inst.setAssignable(isAssignable());
            inst.setRequiredType(getRequiredType());
            sourceBinding.fixupBinding(inst);
            compiledVariable = inst;

            Component overridden = getOverriddenComponent();
            if (overridden != null) {
                checkCompatibility(overridden);
            }

//            if (unused) {
//                // Happens when there are no references, but compileWithTracing is set
//                inst.setUnused(true);
//            }
        }

    }

    /**
     * Initialize - common code called from the compile() method of all subclasses
     *
     * @param var the representation of the variable declaration in the compiled executable
     * @throws net.sf.saxon.trans.XPathException
     *          if an error is detected
     */

    protected void initializeBinding(GlobalVariable var)
            throws XPathException {

        Expression select = var.getSelectExpression();
        Expression exp2 = select;
        if (exp2 != null) {
            try {
                ExpressionVisitor visitor = makeExpressionVisitor();
                exp2 = select.simplify().typeCheck(visitor, getConfiguration().makeContextItemStaticInfo(Type.ITEM_TYPE, true));
            } catch (XPathException err) {
                compileError(err);
            }

            // Add trace wrapper code if required
            exp2 = makeTraceInstruction(this, exp2);

            allocateLocalSlots(exp2);
        }
        if (slotManager != null && slotManager.getNumberOfVariables() > 0) {
            var.setContainsLocals(slotManager);
        }
        //StylesheetPackage pack = getCompilation().getStylesheetPackage();
        //pack.registerGlobalVariable(gvar);

        if (exp2 != select) {
            var.setSelectExpression(exp2);
        }

    }


    /**
     * Get the SlotManager associated with this stylesheet construct. The SlotManager contains the
     * information needed to manage the local stack frames used by run-time instances of the code.
     *
     * @return the associated SlotManager object
     */

    public SlotManager getSlotManager() {
        return slotManager;
    }

    /**
     * Optimize the stylesheet construct
     *
     * @param declaration the declaration of this variable
     */

    public void optimize(ComponentDeclaration declaration) throws XPathException {
        if (!redundant && compiledVariable.getSelectExpression() != null) {
            Expression exp2 = compiledVariable.getSelectExpression();
            ExpressionVisitor visitor = makeExpressionVisitor();

            exp2 = ExpressionTool.optimizeComponentBody(
                    exp2, getCompilation(), visitor,
                    getConfiguration().makeContextItemStaticInfo(AnyItemType.getInstance(), true), false);

            allocateLocalSlots(exp2);
            if (slotManager != null && slotManager.getNumberOfVariables() > 0) {
                compiledVariable.setContainsLocals(slotManager);
            }

            if (exp2 != compiledVariable.getSelectExpression()) {
                compiledVariable.setSelectExpression(exp2);
            }
        }
    }


    /**
     * Mark this global variable as redundant, typically because it is overridden by another global
     * variable of the same name, or because there are no references to it
     *
     * @param redundant true if this variable is redundant, otherwise false
     */

    public void setRedundant(boolean redundant) {
        this.redundant = redundant;
    }

    /**
     * Generate byte code if appropriate
     *
     * @param opt the optimizer
     *
     */
    public void generateByteCode(Optimizer opt) {}


}

