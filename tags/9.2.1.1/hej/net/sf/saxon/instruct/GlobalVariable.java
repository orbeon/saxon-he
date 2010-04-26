package net.sf.saxon.instruct;

import net.sf.saxon.Configuration;
import net.sf.saxon.Controller;
import net.sf.saxon.expr.*;
import net.sf.saxon.om.NamespaceConstant;
import net.sf.saxon.om.SingletonIterator;
import net.sf.saxon.om.UnfailingIterator;
import net.sf.saxon.om.ValueRepresentation;
import net.sf.saxon.query.XQueryFunction;
import net.sf.saxon.query.XQueryFunctionLibrary;
import net.sf.saxon.trans.XPathException;
import net.sf.saxon.value.Value;

import java.util.ArrayList;
import java.util.List;
import java.util.Stack;

/**
* A compiled global variable in a stylesheet or query. <br>
*/

public class GlobalVariable extends GeneralVariable implements Container {

    private Executable executable;
    private SlotManager stackFrameMap = null;
    private boolean indexed;

    /**
     * Create a global variable
     */

    public GlobalVariable(){}

    /**
     * Get the executable containing this global variable
     * @return the containing executable
     */

    public Executable getExecutable() {
        return executable;
    }

    /**
     * Set the containing executable
     * @param executable the executable that contains this global variable
     */

    public void setExecutable(Executable executable) {
        this.executable = executable;
    }

    /**
     * Get the host language (XSLT, XQuery, XPath) used to implement the code in this container
     * @return typically {@link net.sf.saxon.Configuration#XSLT} or {@link net.sf.saxon.Configuration#XQUERY}
     */

    public int getHostLanguage() {
        return executable.getHostLanguage();
    }

    /**
     * Mark this as an indexed variable, to allow fast searching
     */

    public void setIndexedVariable() {
        indexed = true;
    }

    /**
     * Ask whether this is an indexed variable
     * @return true if this variable is indexed
     */

    public boolean isIndexedVariable() {
        return indexed;
    }

    /**
     * Get the granularity of the container.
     * @return 0 for a temporary container created during parsing; 1 for a container
     *         that operates at the level of an XPath expression; 2 for a container at the level
     *         of a global function or template
     */

    public int getContainerGranularity() {
        return 2;
    }

    /**
     * The expression that initializes a global variable may itself use local variables.
     * In this case a stack frame needs to be allocated while evaluating the global variable
     * @param map The stack frame map for local variables used while evaluating this global
     * variable.
     */

    public void setContainsLocals(SlotManager map) {
        this.stackFrameMap = map;
    }

    /**
     * Is this a global variable?
     * @return true (yes, it is a global variable)
     */

    public boolean isGlobal() {
        return true;
    }

    /**
     * Check for cycles in this variable definition
     * @param referees the calls leading up to this one; it's an error if this variable is on the
     * stack, because that means it calls itself directly or indirectly. The stack may contain
     * variable definitions (GlobalVariable objects) and user-defined functions (UserFunction objects).
     * It will never contain the same object more than once.
     * @param globalFunctionLibrary the library containing all global functions
     */

    public void lookForCycles(Stack referees, XQueryFunctionLibrary globalFunctionLibrary) throws XPathException {
        if (referees.contains(this)) {
            int s = referees.indexOf(this);
            referees.push(this);
            String message = "Circular definition of global variable. $" +
                    getVariableQName().getDisplayName();
            for (int i = s; i < referees.size() - 1; i++) {
                if (i != s) {
                    message += ", which";
                }
                if (referees.get(i + 1) instanceof GlobalVariable) {
                    GlobalVariable next = (GlobalVariable)referees.get(i + 1);
                    message += " uses $" + next.getVariableQName().getDisplayName();
                } else if (referees.get(i + 1) instanceof XQueryFunction) {
                    XQueryFunction next = (XQueryFunction)referees.get(i + 1);
                    message += " calls " + next.getFunctionName().getDisplayName() +
                            "#" + next.getNumberOfArguments() + "()";
                }
            }
            message += '.';
            XPathException err = new XPathException(message);
            err.setErrorCode("XQST0054");
            err.setIsStaticError(true);
            err.setLocator(this);
            throw err;
        }
        if (select != null) {
            referees.push(this);
            List list = new ArrayList(10);
            ExpressionTool.gatherReferencedVariables(select, list);
            for (int i=0; i<list.size(); i++) {
                Binding b = (Binding)list.get(i);
                if (b instanceof GlobalVariable) {
                    ((GlobalVariable)b).lookForCycles(referees, globalFunctionLibrary);
                }
            }
            list.clear();
            ExpressionTool.gatherCalledFunctionNames(select, list);
            for (int i=0; i<list.size(); i++) {
                XQueryFunction f = globalFunctionLibrary.getDeclarationByKey((String)list.get(i));
                if (!referees.contains(f)) {
                    // recursive function calls are allowed
                    lookForFunctionCycles(f, referees, globalFunctionLibrary);
                }
            }
            referees.pop();
        }
    }

    /**
     * Look for cyclic variable references that go via one or more function calls
     * @param f a used-defined function
     * @param referees a list of variables and functions that refer directly or indirectly to this variable
     * @param globalFunctionLibrary the library containing all global functions
     */

    private static void lookForFunctionCycles(
            XQueryFunction f, Stack referees, XQueryFunctionLibrary globalFunctionLibrary) throws XPathException {
        Expression body = f.getBody();
        referees.push(f);
        List list = new ArrayList(10);
        ExpressionTool.gatherReferencedVariables(body, list);
        for (int i=0; i<list.size(); i++) {
            Binding b = (Binding)list.get(i);
            if (b instanceof GlobalVariable) {
                ((GlobalVariable)b).lookForCycles(referees, globalFunctionLibrary);
            }
        }
        list.clear();
        ExpressionTool.gatherCalledFunctionNames(body, list);
        for (int i=0; i<list.size(); i++) {
            XQueryFunction qf = globalFunctionLibrary.getDeclarationByKey((String)list.get(i));
            if (!referees.contains(qf)) {
                // recursive function calls are allowed
                lookForFunctionCycles(qf, referees, globalFunctionLibrary);
            }
        }
        referees.pop();
    }


    /**
    * Process the variable declaration
    */

    public TailCall processLeavingTail(XPathContext context) throws XPathException {

        // This code is not used. A global variable is not really an instruction, although
        // it is modelled as such, and it will be evaluated using the evaluateVariable() call
        return null;
    }

    /**
     * Evaluate the variable. That is,
     * get the value of the select expression if present or the content
     * of the element otherwise, either as a tree or as a sequence
    */

    public ValueRepresentation getSelectValue(XPathContext context) throws XPathException {
        if (select==null) {
            throw new AssertionError("*** No select expression for global variable $" +
                    getVariableQName().getDisplayName() + "!!");
        } else {
            try {
                XPathContextMajor c2 = context.newCleanContext();
                c2.setOrigin(this);
                UnfailingIterator initialNode =
                        SingletonIterator.makeIterator(c2.getController().getContextForGlobalVariables());
                initialNode.next();
                c2.setCurrentIterator(initialNode);
                if (stackFrameMap != null) {
                    c2.openStackFrame(stackFrameMap);
                }
                return ExpressionTool.evaluate(select, evaluationMode, c2, referenceCount);
            } catch (XPathException e) {
                if (!getVariableQName().getNamespaceURI().equals(NamespaceConstant.SAXON_GENERATED_GLOBAL)) {
                    e.setIsGlobalError(true);
                }
                throw e;
            }
        }
    }

    /**
    * Evaluate the variable
    */

    public ValueRepresentation evaluateVariable(XPathContext context) throws XPathException {
        final Controller controller = context.getController();
        final Bindery b = controller.getBindery();

        final ValueRepresentation v = b.getGlobalVariable(getSlotNumber());

        if (v != null) {
            return v;
        } else {

            // This is the first reference to a global variable; try to evaluate it now.
            // But first set a flag to stop looping. This flag is set in the Bindery because
            // the VariableReference itself can be used by multiple threads simultaneously

            try {
                b.setExecuting(this, true);
                ValueRepresentation value = getSelectValue(context);
                if (indexed) {
                    value = controller.getConfiguration().getOptimizer().makeIndexedValue(Value.asIterator(value));
                }
                b.defineGlobalVariable(this, value);
                b.setExecuting(this, false);
                return value;

            } catch (XPathException err) {
                b.setExecuting(this, false);
                if (err instanceof XPathException.Circularity) {
                    XPathException e = new XPathException("Circular definition of variable " +
                            getVariableQName().getDisplayName());
                    int lang = getExecutable().getHostLanguage();
                    e.setErrorCode(lang == Configuration.XQUERY ? "XQST0054" : "XTDE0640");
                    e.setXPathContext(context);
                    // Detect it more quickly the next time (in a pattern, the error is recoverable)
                    select = new ErrorExpression(e);
                    e.setLocator(this);
                    throw e;
                } else {
                    throw err;
                }
            }
        }
    }

    /**
     * Get InstructionInfo for this expression
     */

//    public InstructionInfo getInstructionInfo() {
//        InstructionDetails details = new InstructionDetails();
//        details.setConstructType(getInstructionNameCode());
//        details.setObjectName(getVariableQName());
//        details.setProperty("expression", this);
//        details.setSystemId(getSystemId());
//        details.setLineAndColumn(getLineNumber());
//        details.setColumnNumber(getColumnNumber());
//        return details;
//    }


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
