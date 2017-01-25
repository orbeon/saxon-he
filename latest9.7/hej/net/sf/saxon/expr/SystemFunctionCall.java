////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
// Copyright (c) 2015 Saxonica Limited.
// This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
// If a copy of the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
// This Source Code Form is "Incompatible With Secondary Licenses", as defined by the Mozilla Public License, v. 2.0.
////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

package net.sf.saxon.expr;

import com.saxonica.ee.bytecode.ExpressionCompiler;
import com.saxonica.ee.bytecode.util.CannotCompileException;
import com.saxonica.ee.stream.adjunct.StreamingAdjunct;
import net.sf.saxon.Configuration;
import net.sf.saxon.expr.oper.OperandArray;
import net.sf.saxon.expr.parser.*;
import net.sf.saxon.functions.*;
import net.sf.saxon.functions.Error;
import net.sf.saxon.om.LazySequence;
import net.sf.saxon.om.Sequence;
import net.sf.saxon.om.SequenceTool;
import net.sf.saxon.pattern.NodeSetPattern;
import net.sf.saxon.pattern.Pattern;
import net.sf.saxon.trace.ExpressionPresenter;
import net.sf.saxon.trans.XPathException;
import net.sf.saxon.type.BuiltInAtomicType;
import net.sf.saxon.type.ItemType;
import net.sf.saxon.type.TypeHierarchy;
import net.sf.saxon.value.IntegerValue;

/**
 * A call to a system-defined function (specifically, a function implemented as an instance
 * of {@link net.sf.saxon.functions.SystemFunction})
 */
public class SystemFunctionCall extends StaticFunctionCall implements Negatable {

    public SystemFunctionCall(SystemFunction target, Expression[] arguments) {
        super(target, arguments);
    }

    /**
     * Set the retained static context
     *
     * @param rsc the static context to be retained
     */
    @Override
    public void setRetainedStaticContext(RetainedStaticContext rsc) {
        super.setRetainedStaticContext(rsc);
        getTargetFunction().setRetainedStaticContext(rsc);
    }

    /**
     * Pre-evaluate a function at compile time. Functions that do not allow
     * pre-evaluation, or that need access to context information, can prevent early
     * evaluation by setting the LATE bit in the function properties.
     *
     * @param visitor an expression visitor
     * @return the result of the early evaluation, or the original expression, or potentially
     * a simplified expression
     * @throws net.sf.saxon.trans.XPathException if evaluation fails
     */
    @Override
    public Expression preEvaluate(ExpressionVisitor visitor) throws XPathException {
        SystemFunction target = getTargetFunction();
        if ((target.getDetails().properties & StandardFunction.LATE) == 0) {
            return super.preEvaluate(visitor);
        } else {
            // Early evaluation of this function is suppressed
            return this;
        }
    }

    /**
     * Type-check the expression. This also calls preEvaluate() to evaluate the function
     * if all the arguments are constant; functions that do not require this behavior
     * can override the preEvaluate method.
     *
     * @param visitor     the expression visitor
     * @param contextInfo information about the type of the context item
     */
    @Override
    public Expression typeCheck(ExpressionVisitor visitor, ContextItemStaticInfo contextInfo) throws XPathException {
        typeCheckChildren(visitor, contextInfo);
        checkFunctionCall(getTargetFunction(), visitor);
        // Give the function an opportunity to use the type information now available
        getTargetFunction().supplyTypeInformation(visitor, contextInfo, getArguments());
        if ((getTargetFunction().getDetails().properties & StandardFunction.LATE) == 0) {
            return preEvaluateIfConstant(visitor);
        }
        return this;
    }

    @Override
    public SystemFunction getTargetFunction() {
        return (SystemFunction)super.getTargetFunction();
    }

    /**
     * Compute the dependencies of an expression, as the union of the
     * dependencies of its subexpressions. (This is overridden for path expressions
     * and filter expressions, where the dependencies of a subexpression are not all
     * propogated). This method should be called only once, to compute the dependencies;
     * after that, getDependencies should be used.
     *
     * @return the depencies, as a bit-mask
     */
    @Override
    public int getIntrinsicDependencies() {
        int properties = getTargetFunction().getDetails().properties;
        if ((properties & StandardFunction.FOCUS) != 0) {
            int dep = 0;
            if ((properties & StandardFunction.CITEM) != 0) {
                dep |= StaticProperty.DEPENDS_ON_CONTEXT_ITEM;
            }
            if ((properties & StandardFunction.POSN) != 0) {
                dep |= StaticProperty.DEPENDS_ON_POSITION;
            }
            if ((properties & StandardFunction.LAST) != 0) {
                dep |= StaticProperty.DEPENDS_ON_LAST;
            }
            return dep;
        } else if (isCallOn(RegexGroup.class)) {
            return StaticProperty.DEPENDS_ON_CURRENT_GROUP;
        } else if (isCallOnSystemFunction("current-merge-group")) {
            return StaticProperty.DEPENDS_ON_CURRENT_GROUP;
        } else if (isCallOnSystemFunction("current-merge-key")) {
            return StaticProperty.DEPENDS_ON_CURRENT_GROUP;
        } else {
            return 0;
        }
    }

    /**
     * Compute the static cardinality of this expression
     *
     * @return the computed cardinality, as one of the values {@link net.sf.saxon.expr.StaticProperty#ALLOWS_ZERO_OR_ONE},
     * {@link net.sf.saxon.expr.StaticProperty#EXACTLY_ONE}, {@link net.sf.saxon.expr.StaticProperty#ALLOWS_ONE_OR_MORE},
     * {@link net.sf.saxon.expr.StaticProperty#ALLOWS_ZERO_OR_MORE}
     */
    @Override
    protected int computeCardinality() {
        return getTargetFunction().getCardinality(getArguments());
    }

    /**
     * Compute the special properties of this expression. These properties are denoted by a bit-significant
     * integer, possible values are in class {@link net.sf.saxon.expr.StaticProperty}. The "special" properties are properties
     * other than cardinality and dependencies, and most of them relate to properties of node sequences, for
     * example whether the nodes are in document order.
     *
     * @return the special properties, as a bit-significant integer
     */
    @Override
    protected int computeSpecialProperties() {
        return getTargetFunction().getSpecialProperties(getArguments());
    }

    public Expression optimize(ExpressionVisitor visitor, ContextItemStaticInfo contextInfo) throws XPathException {
        Expression sf = super.optimize(visitor, contextInfo);
        if (sf == this) {
            // Give the function an opportunity to regenerate the function call, with more information about
            // the types of the arguments than was previously available
            Expression sfo = getTargetFunction().makeOptimizedFunctionCall(visitor, contextInfo, getArguments());
            if (sfo != null) {
                sfo.setParentExpression(getParentExpression());
                ExpressionTool.copyLocationInfo(this, sfo);
                return sfo;
            }
        }
        if (sf instanceof SystemFunctionCall) {
            // If any arguments are known to be empty, pre-evaluate the result
            StandardFunction.Entry details = ((SystemFunctionCall) sf).getTargetFunction().getDetails();
            if ((details.properties & StandardFunction.UO) != 0) {
                // First argument does not need to be in any particular order
                setArg(0, getArg(0).unordered(true, visitor.isOptimizeForStreaming()));
            }
            if (getArity() <= details.resultIfEmpty.length) {
                // the condition eliminates concat, which is a special case.
                for (int i = 0; i < getArity(); i++) {
                    if (Literal.isEmptySequence(getArg(i)) && details.resultIfEmpty[i] != null) {
                        return Literal.makeLiteral(SequenceTool.toGroundedValue(details.resultIfEmpty[i]));
                    }
                }
            }
        }
        return sf;
    }

    @Override
    public boolean isVacuousExpression() {
        return isCallOn(Error.class);
    }

    /**
     * Determine the data type of the expression, if possible. All expression return
     * sequences, in general; this method determines the type of the items within the
     * sequence, assuming that (a) this is known in advance, and (b) it is the same for
     * all items in the sequence.
     * <p/>
     * <p>This method should always return a result, though it may be the best approximation
     * that is available at the time.</p>
     *
     * @return a value such as Type.STRING, Type.BOOLEAN, Type.NUMBER,
     * Type.NODE, or Type.ITEM (meaning not known at compile time)
     */
    @Override
    public ItemType getItemType() {
        return getTargetFunction().getResultItemType(getArguments());
    }

    /**
     * Copy an expression. This makes a deep copy.
     *
     * @return the copy of the original expression
     * @param rebindings
     */
    @Override
    public Expression copy(RebindingMap rebindings) {
        Expression[] args = new Expression[getArity()];
        for (int i = 0; i < args.length; i++) {
            args[i] = getArg(i).copy(rebindings);
        }
        return new SystemFunctionCall(getTargetFunction(), args);
    }

    /**
     * For an expression that returns an integer or a sequence of integers, get
     * a lower and upper bound on the values of the integers that may be returned, from
     * static analysis. The default implementation returns null, meaning "unknown" or
     * "not applicable". Other implementations return an array of two IntegerValue objects,
     * representing the lower and upper bounds respectively. The values
     * UNBOUNDED_LOWER and UNBOUNDED_UPPER are used by convention to indicate that
     * the value may be arbitrarily large. The values MAX_STRING_LENGTH and MAX_SEQUENCE_LENGTH
     * are used to indicate values limited by the size of a string or the size of a sequence.
     *
     * @return the lower and upper bounds of integer values in the result, or null to indicate
     * unknown or not applicable.
     */
    //@Override
    public IntegerValue[] getIntegerBounds() {
        SystemFunction fn = getTargetFunction();
        if ((fn.getDetails().properties & StandardFunction.FILTER) != 0) {
            return getArg(0).getIntegerBounds();
        }
        return fn.getIntegerBounds();
    }

    /**
     * Check whether this specific instance of the expression is negatable
     *
     * @param th the TypeHierarchy (in case it's needed)
     * @return true if it is
     */
    public boolean isNegatable(TypeHierarchy th) {
        return isCallOn(NotFn.class) || isCallOn(BooleanFn.class) || isCallOn(Empty.class) || isCallOn(Exists.class);
    }

    /**
     * Create an expression that returns the negation of this expression
     *
     * @return the negated expression
     * @throws UnsupportedOperationException if isNegatable() returns false
     */
    public Expression negate() {
        SystemFunction fn = getTargetFunction();
        if (fn instanceof NotFn) {
            Expression arg = getArg(0);
            if (arg.getItemType() == BuiltInAtomicType.BOOLEAN && arg.getCardinality() == StaticProperty.EXACTLY_ONE) {
                return arg;
            } else {
                return SystemFunction.makeCall("boolean", getRetainedStaticContext(), arg);
            }
        } else if (fn instanceof BooleanFn) {
            return SystemFunction.makeCall("not", getRetainedStaticContext(), getArg(0));
        } else if (fn instanceof Exists) {
            return SystemFunction.makeCall("empty", getRetainedStaticContext(), getArg(0));
        } else if (fn instanceof Empty) {
            return SystemFunction.makeCall("exists", getRetainedStaticContext(), getArg(0));
        }
        throw new UnsupportedOperationException();
    }

    /**
     * Replace this expression by a simpler expression that delivers the results without regard
     * to order.
     *
     * @param retainAllNodes set to true if the result must contain exactly the same nodes as the
     *                       original; set to false if the result can eliminate (or introduce) duplicates.
     * @param forStreaming   set to true if the result is to be optimized for streaming
     * @return an expression that delivers the same nodes in a more convenient order
     * @throws net.sf.saxon.trans.XPathException if the rewrite fails
     */
    @Override
    public Expression unordered(boolean retainAllNodes, boolean forStreaming) throws XPathException {
        SystemFunction fn = getTargetFunction();
        if (fn instanceof Reverse) {
            return getArg(0);
        }
        if (fn instanceof TreatFn) {
            setArg(0, getArg(0).unordered(retainAllNodes, forStreaming));
        }
        return this;
    }

    /**
     * Add a representation of this expression to a PathMap. The PathMap captures a map of the nodes visited
     * by an expression in a source tree.
     * <p/>
     * <p>The default implementation of this method assumes that an expression does no navigation other than
     * the navigation done by evaluating its subexpressions, and that the subexpressions are evaluated in the
     * same context as the containing expression. The method must be overridden for any expression
     * where these assumptions do not hold. For example, implementations exist for AxisExpression, ParentExpression,
     * and RootExpression (because they perform navigation), and for the doc(), document(), and collection()
     * functions because they create a new navigation root. Implementations also exist for PathExpression and
     * FilterExpression because they have subexpressions that are evaluated in a different context from the
     * calling expression.</p>
     *
     * @param pathMap        the PathMap to which the expression should be added
     * @param pathMapNodeSet the PathMapNodeSet to which the paths embodied in this expression should be added
     * @return the pathMapNodeSet representing the points in the source document that are both reachable by this
     * expression, and that represent possible results of this expression. For an expression that does
     * navigation, it represents the end of the arc in the path map that describes the navigation route. For other
     * expressions, it is the same as the input pathMapNode.
     */
    @Override
    public PathMap.PathMapNodeSet addToPathMap(PathMap pathMap, PathMap.PathMapNodeSet pathMapNodeSet) {
        if (isCallOn(Doc.class) || isCallOn(DocumentFn.class) || isCallOn(CollectionFn.class)) {
            getArg(0).addToPathMap(pathMap, pathMapNodeSet);
            return new PathMap.PathMapNodeSet(pathMap.makeNewRoot(this));
        } else if (isCallOn(KeyFn.class)) {
            return ((KeyFn)getTargetFunction()).addToPathMap(pathMap, pathMapNodeSet);
        } else {
            return super.addToPathMap(pathMap, pathMapNodeSet);
        }
    }

    /**
     * Convert this expression to an equivalent XSLT pattern
     *
     * @param config the Saxon configuration
     * @param is30   true if this is XSLT 3.0
     * @return the equivalent pattern
     * @throws net.sf.saxon.trans.XPathException if conversion is not possible
     */
    @Override
    public Pattern toPattern(Configuration config, boolean is30) throws XPathException {
        SystemFunction fn = getTargetFunction();
        if (fn instanceof Root_1) {
            if (is30 &&
                (getArg(0) instanceof ContextItemExpression ||
                    (getArg(0) instanceof ItemChecker &&
                        ((ItemChecker) getArg(0)).getBaseExpression() instanceof ContextItemExpression))) {
                return new NodeSetPattern(this);
            }
        }
        return super.toPattern(config, is30);
    }

    public Sequence[] evaluateArguments(XPathContext context) throws XPathException {
        OperandArray operanda = getOperanda();
        int numArgs = operanda.getNumberOfOperands();
        Sequence[] actualArgs = new Sequence[numArgs];
        for (int i = 0; i < numArgs; i++) {
            actualArgs[i] = new LazySequence(operanda.getOperandExpression(i).iterate(context));
        }
        return actualArgs;
    }

    /**
     * Diagnostic print of expression structure. The abstract expression tree
     * is written to the supplied output destination.
     *
     * @param out
     */
    @Override
    public void export(ExpressionPresenter out) throws XPathException {
        out.startElement("fn", this);
        out.emitAttribute("name", getFunctionName().getDisplayName());
        getTargetFunction().exportAttributes(out);
        for (Operand o : operands()) {
            o.getChildExpression().export(out);
        }
        out.endElement();
    }

    //#ifdefined BYTECODE

    /**
     * Return the bytecode compiler for this system function call.
     *
     * @return the relevant ExpressionCompiler
     */
    @Override
    public ExpressionCompiler getExpressionCompiler() throws CannotCompileException {
        ExpressionCompiler compiler = getTargetFunction().getExpressionCompiler();
        if (compiler == null) {
            if (getTargetFunction() instanceof ContextItemAccessorFunction) {
                Expression exp = ((ContextItemAccessorFunction)getTargetFunction()).makeContextItemExplicit();
                return exp.getExpressionCompiler();
            }
            return super.getExpressionCompiler();
        }
        return compiler;
    }
    //#endif

    //#ifdefined STREAM

    /**
     * Get a class that supports streamed evaluation of this expression
     *
     * @return the relevant StreamingAdjunct, or null if none is available
     */

    public StreamingAdjunct getStreamingAdjunct() {
        StreamingAdjunct sa = getTargetFunction().getStreamingAdjunct();
        if (sa == null) {
            if (getTargetFunction() instanceof ContextItemAccessorFunction) {
                Expression exp = ((ContextItemAccessorFunction) getTargetFunction()).makeContextItemExplicit();
                return exp.getStreamingAdjunct();
            }
            return new StreamingAdjunct();
        }
        return sa;
    }
//#endif

}
