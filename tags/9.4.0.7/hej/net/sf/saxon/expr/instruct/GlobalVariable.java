package net.sf.saxon.expr.instruct;

import net.sf.saxon.Configuration;
import net.sf.saxon.Controller;
import net.sf.saxon.expr.*;
import net.sf.saxon.expr.parser.ExpressionTool;
import net.sf.saxon.lib.NamespaceConstant;
import net.sf.saxon.om.StandardNames;
import net.sf.saxon.om.StructuredQName;
import net.sf.saxon.om.ValueRepresentation;
import net.sf.saxon.query.XQueryFunction;
import net.sf.saxon.query.XQueryFunctionLibrary;
import net.sf.saxon.trace.InstructionInfo;
import net.sf.saxon.trans.XPathException;
import net.sf.saxon.tree.iter.SingletonIterator;
import net.sf.saxon.tree.iter.UnfailingIterator;
import net.sf.saxon.value.SingletonClosure;
import net.sf.saxon.value.Value;

import java.util.*;

/**
* A compiled global variable in a stylesheet or query. <br>
*/

public class GlobalVariable extends GeneralVariable implements Container, InstructionInfo {

    private Executable executable;
    /*@Nullable*/ private SlotManager stackFrameMap = null;
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
     * Get the line number within the document, entity or module containing a particular location
     *
     * @param locationId identifier of the location in question (as passed down the Receiver pipeline)
     * @return the line number within the document, entity or module, or -1 if no information is available.
     */
    public int getLineNumber(long locationId) {
        return executable.getLocationMap().getLineNumber(locationId);
    }

    /**
     * Get the column number within the document, entity, or module containing a particular location
     *
     * @param locationId identifier of the location in question (as passed down the Receiver pipeline)
     * @return the column number within the document, entity, or module, or -1 if this is not available
     */
    public int getColumnNumber(long locationId) {
        return executable.getLocationMap().getLineNumber(locationId);
    }

    /**
     * Get the type of construct. This will either be the fingerprint of a standard XSLT instruction name
     * (values in {@link net.sf.saxon.om.StandardNames}: all less than 1024)
     * or it will be a constant in class {@link net.sf.saxon.trace.Location}.
     *
     * @return an integer identifying the kind of construct
     */
    public int getConstructType() {
        return StandardNames.XSL_VARIABLE;
    }

    /**
     * Get a name identifying the object of the expression, for example a function name, template name,
     * variable name, key name, element name, etc. This is used only where the name is known statically.
     *
     * @return the QName of the object declared or manipulated by this instruction or expression
     */
    public StructuredQName getObjectName() {
        return getVariableQName();
    }

    /**
     * Get the value of a particular property of the instruction. Properties
     * of XSLT instructions are generally known by the name of the stylesheet attribute
     * that defines them.
     *
     * @param name The name of the required property
     * @return The value of the requested property, or null if the property is not available
     */
    public Object getProperty(String name) {
        return null;
    }

    /**
     * Get an iterator over all the properties available. The values returned by the iterator
     * will be of type String, and each string can be supplied as input to the getProperty()
     * method to retrieve the value of the property. The iterator may return properties whose
     * value is null.
     *
     * @return an iterator over the properties.
     */
    public Iterator<String> getProperties() {
        List<String> list = Collections.emptyList();
        return list.iterator();
    }

    /**
     * Get the container in which this expression is located. This will usually be a top-level construct
     * such as a function or global variable, and XSLT template, or an XQueryExpression. In the case of
     * free-standing XPath expressions it will be the StaticContext object
     *
     * @return the expression's container
     */
    @Override
    public Container getContainer() {
        return this;
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
     * @throws net.sf.saxon.trans.XPathException if cycles are found
     */

    public void lookForCycles(Stack<Container> referees, XQueryFunctionLibrary globalFunctionLibrary) throws XPathException {
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
            List<Binding> list = new ArrayList<Binding>(10);
            ExpressionTool.gatherReferencedVariables(select, list);
            for (Binding b : list) {
                if (b instanceof GlobalVariable) {
                    ((GlobalVariable) b).lookForCycles(referees, globalFunctionLibrary);
                }
            }
            List<String> flist = new ArrayList<String>();
            ExpressionTool.gatherCalledFunctionNames(select, flist);
            for (String s : flist) {
                XQueryFunction f = globalFunctionLibrary.getDeclarationByKey(s);
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
            XQueryFunction f, Stack<Container> referees, XQueryFunctionLibrary globalFunctionLibrary) throws XPathException {
        Expression body = f.getBody();
        referees.push(f);
        List<Binding> list = new ArrayList<Binding>(10);
        ExpressionTool.gatherReferencedVariables(body, list);
        for (Binding b : list) {
            if (b instanceof GlobalVariable) {
                ((GlobalVariable) b).lookForCycles(referees, globalFunctionLibrary);
            }
        }
        List<String> flist = new ArrayList<String>();
        ExpressionTool.gatherCalledFunctionNames(body, flist);
        for (String s : flist) {
            XQueryFunction qf = globalFunctionLibrary.getDeclarationByKey(s);
            if (!referees.contains(qf)) {
                // recursive function calls are allowed
                lookForFunctionCycles(qf, referees, globalFunctionLibrary);
            }
        }
        referees.pop();
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
                final Controller controller = c2.getController();
                assert controller != null;
                UnfailingIterator initialNode =
                        SingletonIterator.makeIterator(controller.getContextForGlobalVariables());
                initialNode.next();
                c2.setCurrentIterator(initialNode);
                if (stackFrameMap != null) {
                    c2.openStackFrame(stackFrameMap);
                }
                return ExpressionTool.evaluate(select, evaluationMode, c2, referenceCount);
            } catch (XPathException e) {
                if (!getVariableQName().getURI().equals(NamespaceConstant.SAXON_GENERATED_GLOBAL)) {
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
        assert controller != null;
        final Bindery b = controller.getBindery();

        final ValueRepresentation v = b.getGlobalVariable(getSlotNumber());

        if (v != null) {
            return v;
        } else {
            return actuallyEvaluate(context);
        }
    }

    /**
     * Evaluate the global variable, and save its value for use in subsequent references.
     * @param context the XPath dynamic context
     * @return the value of the variable
     * @throws XPathException if evaluation fails
     */

    protected ValueRepresentation actuallyEvaluate(XPathContext context) throws XPathException {
        final Controller controller = context.getController();
        assert controller != null;
        final Bindery b = controller.getBindery();
        try {
            // This is the first reference to a global variable; try to evaluate it now.
            // But first check for circular dependencies.
            setDependencies(b, this, context);

            // Set a flag to indicate that the variable is being evaluated. This is designed to prevent
            // (where possible) the same global variable being evaluated several times in different threads
            boolean go = b.setExecuting(this);
            if (!go) {
                // some other thread has evaluated the variable while we were waiting
                return b.getGlobalVariable(getSlotNumber());
            }

            ValueRepresentation value = getSelectValue(context);
            if (indexed) {
                value = controller.getConfiguration().obtainOptimizer().makeIndexedValue(Value.asIterator(value));
            }
            return b.saveGlobalVariableValue(this, value);

        } catch (XPathException err) {
            b.setNotExecuting(this);
            if (err instanceof XPathException.Circularity) {
                err.setErrorCode(getHostLanguage() == Configuration.XSLT ? "XTDE0640" : "XQST0054");
                err.setXPathContext(context);
                // Detect it more quickly the next time (in a pattern, the error is recoverable)
                SingletonClosure closure = new SingletonClosure(new ErrorExpression(err), context);
                b.defineGlobalVariable(this, closure);
                err.setLocator(this);
                throw err;
            } else {
                throw err;
            }
        }
    }

    /**
     * Get the variable that is immediately dependent on this one, and register the dependency, so
     * that circularities can be detected across threads. This relies on the fact that when the initialiser
     * for variable X contains a reference to variable Y, then when Y is evaluated, a stack frame will be found
     * on the context stack representing the evaluation of X. We don't set a dependency from X to Y if the value
     * of Y was already available in the Bindery; it's not needed, because in this case we know that evaluation
     * of Y is unproblematic, and can't lead to any circularities.
     * @param bindery the Bindery
     * @param var the global variable or parameter being evaluated
     * @param context the dynamic evaluation context
     */

    protected static void setDependencies(Bindery bindery, GlobalVariable var, XPathContext context) throws XPathException {
        if (!(context instanceof XPathContextMajor)) {
            context = getMajorCaller(context);
        }
        while (context != null) {
            do {
                InstructionInfo instructionInfo = ((XPathContextMajor) context).getOrigin();
                if (instructionInfo instanceof GlobalVariable) {
                    bindery.registerDependency((GlobalVariable)instructionInfo, var);
                    return;
                }
                context = getMajorCaller(context);
            } while (context != null);
        }
    }

    private static XPathContextMajor getMajorCaller(XPathContext context) {
        XPathContext caller = context.getCaller();
        while (!(caller == null || caller instanceof XPathContextMajor)) {
            caller = caller.getCaller();
        }
        return (XPathContextMajor)caller;
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