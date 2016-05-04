package net.sf.saxon.functions;

import com.saxonica.functions.hof.AtomicConstructorFunction;
import net.sf.saxon.expr.Expression;
import net.sf.saxon.expr.XPathContext;
import net.sf.saxon.expr.instruct.UserFunction;
import net.sf.saxon.expr.parser.ContextItemStaticInfo;
import net.sf.saxon.expr.parser.ExplicitLocation;
import net.sf.saxon.expr.parser.ExpressionVisitor;
import net.sf.saxon.expr.parser.RoleDiagnostic;
import net.sf.saxon.ma.arrays.ArrayBlock;
import net.sf.saxon.ma.arrays.ArrayGet;
import net.sf.saxon.ma.arrays.ArrayItem;
import net.sf.saxon.ma.arrays.ArrayItemType;
import net.sf.saxon.ma.map.MapGet;
import net.sf.saxon.ma.map.MapType;
import net.sf.saxon.om.Function;
import net.sf.saxon.om.Sequence;
import net.sf.saxon.om.SequenceTool;
import net.sf.saxon.om.StandardNames;
import net.sf.saxon.trace.ExpressionPresenter;
import net.sf.saxon.trans.SymbolicName;
import net.sf.saxon.trans.XPathException;
import net.sf.saxon.type.*;
import net.sf.saxon.value.SequenceType;

import java.util.Properties;

/**
 * This class implements the function fn:apply(), which is a standard function in XQuery 3.1.
 * The fn:apply function is also used internally to implement dynamic function calls.
 */

public class ApplyFn extends SystemFunction  {

    // This flag is nonNull when the fn:apply() call actually implements a dynamic function call.
    // It only affects the error code/message if the call fails
    private String dynamicFunctionCall;

    public ApplyFn() {
    }

    /**
     * Say that this call to fn:apply was originally written as a dynamic function call
     * @param fnExpr string representation of the expression used as the dynamic call
     */

    public void setDynamicFunctionCall(String fnExpr) {
        dynamicFunctionCall = fnExpr;
    }

    /**
     * Ask whether this call to fn:apply was originally written as a dynamic function call
     *
     * @return true if it was originally a dynamic function call
     */

    public boolean isDynamicFunctionCall() {
        return dynamicFunctionCall != null;
    }

    /**
     * Get the return type, given knowledge of the actual arguments
     *
     * @param args the actual arguments supplied
     * @return the best available item type that the function will return
     */
    @Override
    public ItemType getResultItemType(Expression[] args) {
        // Item type of the result is the same as that of the supplied function
        ItemType fnType = args[0].getItemType();
        if (fnType instanceof AnyFunctionType) {
            return AnyItemType.getInstance();
        } else if (fnType instanceof FunctionItemType) {
            return ((FunctionItemType) fnType).getResultType().getPrimaryType();
        } else {
            return AnyItemType.getInstance();
        }
    }

    @Override
    public Expression makeOptimizedFunctionCall(ExpressionVisitor visitor, ContextItemStaticInfo contextInfo, Expression... arguments) throws XPathException {
        Expression target = arguments[0];
        if (target.getItemType() instanceof MapType) {
            // Convert $map($key) to map:get($map, $key)
            // This improves streamability analysis - see accumulator-053
            IntegratedFunctionLibrary lib = visitor.getConfiguration().getIntegratedFunctionLibrary();
            SymbolicName name = new SymbolicName(StandardNames.XSL_FUNCTION, MapGet.name, 2);
            Expression[] newArgs = new Expression[2];
            newArgs[0] = arguments[0]; // the map item
            newArgs[1] = (((ArrayBlock) arguments[1]).getOperanda().getOperand(0).getChildExpression());
            Expression mapGet = lib.bind(name, newArgs, visitor.getStaticContext());
            return mapGet.typeCheck(visitor, contextInfo);
        } else if (target.getItemType() instanceof ArrayItemType) {
            // Convert $array($key) to array:get($array, $key)
            IntegratedFunctionLibrary lib = visitor.getConfiguration().getIntegratedFunctionLibrary();
            SymbolicName name = new SymbolicName(StandardNames.XSL_FUNCTION, ArrayGet.name, 2);
            Expression[] newArgs = new Expression[2];
            newArgs[0] = arguments[0]; // the map item
            newArgs[1] = (((ArrayBlock) arguments[1]).getOperanda().getOperand(0).getChildExpression());
            Expression arrayGet = lib.bind(name, newArgs, visitor.getStaticContext());
            return arrayGet.typeCheck(visitor, contextInfo);
        }
        return null;
    }

    /**
     * Evaluate the expression
     *
     * @param context   the dynamic evaluation context
     * @param arguments the values of the arguments. The first argument is the function item
     *                  to be called; the second argument is an array containing
     *                  the arguments to be passed to that function, before conversion.
     * @return the result of the evaluation, in the form of a SequenceIterator
     * @throws net.sf.saxon.trans.XPathException
     *          if a dynamic error occurs during the evaluation of the expression
     */
    public Sequence call(XPathContext context, Sequence[] arguments) throws XPathException {
        final Function function = (Function) arguments[0].head();
        ArrayItem args = (ArrayItem)arguments[1].head();

        if (function.getArity() != args.size()) {
            String errorCode = isDynamicFunctionCall() ? "XPTY0004" : "FOAP0001";
            XPathException err = new XPathException(
                "Number of arguments required = " + function.getArity() +
                    "; number supplied = " + args.size(), errorCode);
            err.setIsTypeError(isDynamicFunctionCall());
            err.setXPathContext(context);
            throw err;
        }
        TypeHierarchy th = context.getConfiguration().getTypeHierarchy();
        FunctionItemType fit = function.getFunctionItemType();
        Sequence[] argArray = new Sequence[args.size()];
        if (fit == AnyFunctionType.ANY_FUNCTION) {
            for (int i = 0; i < argArray.length; i++) {
                argArray[i] = args.get(i);
            }
        } else {
            for (int i = 0; i < argArray.length; i++) {
                SequenceType expected = fit.getArgumentTypes()[i];
                RoleDiagnostic role;
                if (isDynamicFunctionCall()) {
                    role = new RoleDiagnostic(RoleDiagnostic.FUNCTION, dynamicFunctionCall, i);
                } else {
                    role = new RoleDiagnostic(RoleDiagnostic.FUNCTION, "fn:apply", i + 1);
                }
                Sequence converted = th.applyFunctionConversionRules(
                    args.get(i), expected, role, ExplicitLocation.UNKNOWN_LOCATION);
                argArray[i] = SequenceTool.makeRepeatable(converted);
            }
        }
        XPathContext c2 = context;
        if (function instanceof UserFunction) {
            c2 = ((UserFunction)function).makeNewContext(context);
        }
        Sequence rawResult = function.call(c2, argArray);
        if (function.isTrustedResultType()) {
            // trust system functions to return a result of the correct type
            return rawResult;
        } else {
            // Check the result of the function
            RoleDiagnostic resultRole = new RoleDiagnostic(RoleDiagnostic.FUNCTION_RESULT, "fn:apply", -1);
            return th.applyFunctionConversionRules(
                rawResult, fit.getResultType(), resultRole, ExplicitLocation.UNKNOWN_LOCATION);
        }
    }

    @Override
    public void exportAttributes(ExpressionPresenter out) {
        out.emitAttribute("dyn", dynamicFunctionCall);
    }

    /**
     * Import any attributes found in the export file, that is, any attributes output using
     * the exportAttributes method
     *
     * @param attributes the attributes, as a properties object
     * @throws net.sf.saxon.trans.XPathException
     */
    @Override
    public void importAttributes(Properties attributes) throws XPathException {
        dynamicFunctionCall = attributes.getProperty("dyn");
    }
}


// Copyright (c) 2015 Saxonica Limited.