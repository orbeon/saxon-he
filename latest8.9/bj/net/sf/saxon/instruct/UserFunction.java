package net.sf.saxon.instruct;

import net.sf.saxon.Controller;
import net.sf.saxon.sort.SortExpression;
import net.sf.saxon.sort.TupleSorter;
import net.sf.saxon.expr.*;
import net.sf.saxon.om.*;
import net.sf.saxon.trace.InstructionInfo;
import net.sf.saxon.trace.InstructionInfoProvider;
import net.sf.saxon.trans.XPathException;
import net.sf.saxon.type.Type;
import net.sf.saxon.type.TypeHierarchy;
import net.sf.saxon.value.SequenceType;
import net.sf.saxon.value.Value;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Set;

/**
 * This object represents the compiled form of a user-written function
 * (the source can be either an XSLT stylesheet function or an XQuery function).
 *
 * <p>It is assumed that type-checking, of both the arguments and the results,
 * has been handled at compile time. That is, the expression supplied as the body
 * of the function must be wrapped in code to check or convert the result to the
 * required type, and calls on the function must be wrapped at compile time to check or
 * convert the supplied arguments.
 */

public final class UserFunction extends Procedure implements InstructionInfoProvider {

    private int functionNameCode;
    private boolean memoFunction = false;
    private boolean tailCalls = false;
            // indicates that the function contains tail calls, not necessarily recursive ones.
    private boolean tailRecursive = false;
            // indicates that the function contains tail calls on itself
    private UserFunctionParameter[] parameterDefinitions;
    private SequenceType resultType;
    private int evaluationMode = ExpressionTool.UNDECIDED;
    private transient InstructionDetails details = null;

    public UserFunction() {}

    public UserFunction(Expression body) {
        setBody(body);
    };

    public void computeEvaluationMode() {
        if (tailRecursive || memoFunction) {
                // If this function contains tail calls, we evaluate it eagerly, because
                // the caller needs to know whether a tail call was returned or not: if we
                // return a Closure, the tail call escapes into the wild and can reappear anywhere...
                // Eager evaluation also makes sense if it's a memo function.
            evaluationMode = ExpressionTool.eagerEvaluationMode(getBody());
        } else {
            evaluationMode = ExpressionTool.lazyEvaluationMode(getBody());
        }
    }

    public void setParameterDefinitions(UserFunctionParameter[] params) {
        this.parameterDefinitions = params;
    }

    public UserFunctionParameter[] getParameterDefinitions() {
        return parameterDefinitions;
    }

    public void setResultType(SequenceType resultType) {
        this.resultType = resultType;
    }

    /**
     * Indicate whether the function contains a tail call
     * @param tailCalls true if the function contains a tail call (on any function)
     * @param recursiveTailCalls true if the function contains a tail call (on itself)
     */

    public void setTailRecursive(boolean tailCalls, boolean recursiveTailCalls) {
        this.tailCalls = tailCalls;
        this.tailRecursive = recursiveTailCalls;
    }

    public boolean containsTailCalls() {
        return tailCalls;
    }

    public boolean isTailRecursive() {
        return tailRecursive;
    }

    /**
     * Get the type of value returned by this function
     * @return the declared result type, or the inferred result type
     * if this is more precise
     */
    public SequenceType getResultType(TypeHierarchy th) {
        if (resultType == SequenceType.ANY_SEQUENCE) {
            // see if we can infer a more precise result type. We don't do this if the function contains
            // calls on further functions, to prevent infinite regress.
            if (!containsUserFunctionCalls(getBody())) {
                resultType = SequenceType.makeSequenceType(
                        getBody().getItemType(th), getBody().getCardinality());
            }
        }
        return resultType;
    }

    private static boolean containsUserFunctionCalls(Expression exp) {
        if (exp instanceof UserFunctionCall) {
            return true;
        }
        Iterator i = exp.iterateSubExpressions();
        while (i.hasNext()) {
            Expression e = (Expression)i.next();
            if (containsUserFunctionCalls(e)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Get the required types of an argument to this function
     * @param n identifies the argument in question, starting at 0
     * @return a SequenceType object, indicating the required type of the argument
     */

    public SequenceType getArgumentType(int n) {
        return parameterDefinitions[n].getRequiredType();
    }

    /**
     * Get the evaluation mode
     */

    public int getEvaluationMode() {
        if (evaluationMode == ExpressionTool.UNDECIDED) {
            computeEvaluationMode();
        }
        return evaluationMode;
    }

    /**
     * Get the arity of this function
     * @return the number of arguments
     */

    public int getNumberOfArguments() {
        return parameterDefinitions.length;
    }

    /**
     * Mark this function as a memo function (or not)
     * @param isMemo true if this is a memo function
     */

    public void setMemoFunction(boolean isMemo) {
        memoFunction = isMemo;
    }

    /**
     * Set the namepool name code of the function
     * @param nameCode represents the function name
     */

    public void setFunctionNameCode(int nameCode) {
        functionNameCode = nameCode;
    }

    /**
     * Get the namepool name code of the function
     * @return a name code representing the function name
     */

    public int getFunctionNameCode() {
        return functionNameCode;
    }

    /**
     * Gather the direct contributing callees of this function. A callee is a function that this one
     * calls. A contributing callee is a function whose output is added directly to the output of this
     * function without further processing (other than by attaching it to a parent element or document
     * node). A direct contributing callee is a function that is called directly, rather than indirectly.
     * @param result the list into which the callees are gathered.
     */

    public void gatherDirectContributingCallees(Set result) {
        Expression body = getBody();
        gatherDirectContributingCallees(body, result);
    }

    private void gatherDirectContributingCallees(Expression exp, Set result) {
        Iterator kids = exp.iterateSubExpressions();
        while (kids.hasNext()) {
            Expression kid = (Expression)kids.next();
            if (kid instanceof UserFunctionCall) {
                result.add(((UserFunctionCall)kid).getFunction());
            } else if (kid instanceof Assignation) {
                gatherDirectContributingCallees(((Assignation)kid).getAction(), result);
            } else if (kid instanceof IfExpression) {
                gatherDirectContributingCallees(((IfExpression)kid).getThenExpression(), result);
                gatherDirectContributingCallees(((IfExpression)kid).getElseExpression(), result);
            } else if (kid instanceof ParentNodeConstructor) {
                gatherDirectContributingCallees(((ParentNodeConstructor)kid).getContentExpression(), result);
            } else if (kid instanceof FilterExpression) {
                gatherDirectContributingCallees(((FilterExpression)kid).getBaseExpression(), result);
            } else if (kid instanceof SortExpression) {
                gatherDirectContributingCallees(((SortExpression)kid).getBaseExpression(), result);
            } else if (kid instanceof TupleSorter) {
                gatherDirectContributingCallees(((TupleSorter)kid).getBaseExpression(), result);
            } else if (kid instanceof TailCallLoop) {
                gatherDirectContributingCallees(((TailCallLoop)kid).getBaseExpression(), result);
            }
        }
    }


    /**
     * Call this function to return a value.
     * @param actualArgs the arguments supplied to the function. These must have the correct
     * types required by the function signature (it is the caller's responsibility to check this).
     * It is acceptable to supply a {@link net.sf.saxon.value.Closure} to represent a value whose
     * evaluation will be delayed until it is needed. The array must be the correct size to match
     * the number of arguments: again, it is the caller's responsibility to check this.
     * @param context This provides the run-time context for evaluating the function. It is the caller's
     * responsibility to allocate a "clean" context for the function to use; the context that is provided
     * will be overwritten by the function.
     * @return a Value representing the result of the function.
     */

    public ValueRepresentation call(ValueRepresentation[] actualArgs, XPathContextMajor context)
            throws XPathException {

        // If this is a memo function, see if the result is already known
        Controller controller = context.getController();
        if (memoFunction) {
            ValueRepresentation value = getCachedValue(controller, actualArgs);
            if (value != null) return value;
        }

        if (evaluationMode == ExpressionTool.UNDECIDED) {
            // should have been done at compile time
            computeEvaluationMode();
        }

        // Otherwise evaluate the function

        context.setStackFrame(getStackFrameMap(), actualArgs);
        ValueRepresentation result;
        try {
            result = ExpressionTool.evaluate(getBody(), evaluationMode, context, 1);
        } catch (XPathException err) {
            if (err.getLocator() == null) {
                err.setLocator(this);
            }
            throw err;
        }

        // If this is a memo function, save the result in the cache
        if (memoFunction) {
            putCachedValue(controller, actualArgs, result);
        }

        return result;
    }

    /**
      * Call this function in "push" mode, writing the results to the current output destination.
      * @param actualArgs the arguments supplied to the function. These must have the correct
      * types required by the function signature (it is the caller's responsibility to check this).
      * It is acceptable to supply a {@link net.sf.saxon.value.Closure} to represent a value whose
      * evaluation will be delayed until it is needed. The array must be the correct size to match
      * the number of arguments: again, it is the caller's responsibility to check this.
      * @param context This provides the run-time context for evaluating the function. It is the caller's
      * responsibility to allocate a "clean" context for the function to use; the context that is provided
      * will be overwritten by the function.
      */

     public void process(ValueRepresentation[] actualArgs, XPathContextMajor context)
             throws XPathException {
         context.setStackFrame(getStackFrameMap(), actualArgs);
         getBody().process(context);
     }


    /**
     * Call this function. This method allows an XQuery function to be called directly from a Java
     * application. It creates the environment needed to achieve this
     * @param actualArgs the arguments supplied to the function. These must have the correct
     * types required by the function signature (it is the caller's responsibility to check this).
     * It is acceptable to supply a {@link net.sf.saxon.value.Closure} to represent a value whose
     * evaluation will be delayed until it is needed. The array must be the correct size to match
     * the number of arguments: again, it is the caller's responsibility to check this.
     * @param controller This provides the run-time context for evaluating the function. A Controller
     * may be obtained by calling {@link net.sf.saxon.query.XQueryExpression#newController}. This may
     * be used for a series of calls on functions defined in the same module as the XQueryExpression.
     * @return a Value representing the result of the function.
     */

    public ValueRepresentation call(ValueRepresentation[] actualArgs, Controller controller) throws XPathException {
        return call(actualArgs, controller.newXPathContext());
    }

    /**
     * For memo functions, get a saved value from the cache.
     * @return the cached value, or null if no value has been saved for these parameters
     */

    private ValueRepresentation getCachedValue(Controller controller, ValueRepresentation[] params) throws XPathException {
        HashMap map = (HashMap) controller.getUserData(this, "memo-function-cache");
        if (map == null) {
            return null;
        }
        String key = getCombinedKey(params);
        //System.err.println("Used cached value");
        return (ValueRepresentation) map.get(key);
    }

    /**
     * For memo functions, put the computed value in the cache.
     */

    private void putCachedValue(Controller controller, ValueRepresentation[] params, ValueRepresentation value) throws XPathException {
        HashMap map = (HashMap) controller.getUserData(this, "memo-function-cache");
        if (map == null) {
            map = new HashMap(32);
            controller.setUserData(this, "memo-function-cache", map);
        }
        String key = getCombinedKey(params);
        map.put(key, value);
    }

    /**
     * Get a key value representing the values of all the supplied arguments
     */

    private static String getCombinedKey(ValueRepresentation[] params) throws XPathException {
        FastStringBuffer sb = new FastStringBuffer(120);

        for (int i = 0; i < params.length; i++) {
            ValueRepresentation val = params[i];
            // TODO: if the argument value is a sequence, use the identity of the sequence, not its content
            SequenceIterator iter = Value.getIterator(val);
            while (true) {
                Item item = iter.next();
                if (item == null) {
                    break;
                }
                if (item instanceof NodeInfo) {
                    NodeInfo node = (NodeInfo) item;
                    node.generateId(sb);
                } else {
                    sb.append("" + Type.displayTypeName(item));
                    sb.append('/');
                    sb.append(item.getStringValueCS());
                }
                sb.append('\u0001');
            }
            sb.append('\u0002');
        }
        return sb.toString();
    }

    /**
     * Get the InstructionInfo details about the construct. This information isn't used for tracing,
     * but it is available when inspecting the context stack.
     */

    public InstructionInfo getInstructionInfo() {
        if (details == null) {
            details = new InstructionDetails();
            details.setSystemId(getSystemId());
            details.setLineNumber(getLineNumber());
            details.setConstructType(StandardNames.XSL_FUNCTION);
            details.setObjectNameCode(functionNameCode);
            details.setProperty("function", this);
        }
        return details;
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
// The Initial Developer of the Original Code is Michael H. Kay
//
// Portions created by (your name) are Copyright (C) (your legal entity). All Rights Reserved.
//
// Contributor(s): none
//
