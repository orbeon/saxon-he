package net.sf.saxon.expr;
import net.sf.saxon.Configuration;
import net.sf.saxon.sort.IntHashSet;
import net.sf.saxon.sort.IntIterator;
import net.sf.saxon.event.LocationProvider;
import net.sf.saxon.event.SequenceReceiver;
import net.sf.saxon.instruct.*;
import net.sf.saxon.value.Cardinality;
import net.sf.saxon.value.StringValue;
import net.sf.saxon.trace.InstructionInfoProvider;
import net.sf.saxon.trace.InstructionInfo;
import net.sf.saxon.trace.Location;
import net.sf.saxon.om.Item;
import net.sf.saxon.om.SequenceIterator;
import net.sf.saxon.om.SingletonIterator;
import net.sf.saxon.om.NodeInfo;
import net.sf.saxon.trans.XPathException;
import net.sf.saxon.trans.DynamicError;
import net.sf.saxon.type.ItemType;
import net.sf.saxon.type.SchemaType;
import net.sf.saxon.type.TypeHierarchy;

import javax.xml.transform.SourceLocator;
import java.io.PrintStream;
import java.io.Serializable;
import java.util.Iterator;
import java.util.Collections;
import java.util.Arrays;

/**
 * Interface supported by an XPath expression. This includes both compile-time
 * and run-time methods.
 */

public abstract class Expression implements SequenceIterable, EvaluableItem, Serializable, InstructionInfoProvider, Container {

    public static final int EVALUATE_METHOD = 1;
    public static final int ITERATE_METHOD = 2;
    public static final int PROCESS_METHOD = 4;
    protected int staticProperties = -1;
    protected int locationId = -1;
    private Container parentExpression;
    private int[] slotsUsed;

    /**
     * An implementation of Expression must provide at least one of the methods evaluateItem(), iterate(), or process().
     * This method indicates which of these methods is provided directly. The other methods will always be available
     * indirectly, using an implementation that relies on one of the other methods.
     */

    public int getImplementationMethod() {
        if (Cardinality.allowsMany(getCardinality())) {
            return ITERATE_METHOD;
        } else {
            return EVALUATE_METHOD;
        }
    }

    /**
     * Simplify an expression. This performs any static optimization (by rewriting the expression
     * as a different expression). The default implementation does nothing.
     *
     * @exception net.sf.saxon.trans.XPathException if an error is discovered during expression
     *     rewriting
     * @return the simplified expression
     */

    public Expression simplify(StaticContext env) throws XPathException {
        return this;
    }

    /**
     * Perform type checking of an expression and its subexpressions. This is the second phase of
     * static optimization.
     *
     * <p>This checks statically that the operands of the expression have
     * the correct type; if necessary it generates code to do run-time type checking or type
     * conversion. A static type error is reported only if execution cannot possibly succeed, that
     * is, if a run-time type error is inevitable. The call may return a modified form of the expression.</p>
     *
     * <p>This method is called after all references to functions and variables have been resolved
     * to the declaration of the function or variable. However, the types of such functions and
     * variables may not be accurately known if they have not been explicitly declared.</p>
     *
     * <p>If the implementation returns a value other than "this", then it is required to ensure that
     * the parent pointer and location information in the returned expression have been set up correctly.
     * It should not rely on the caller to do this, although for historical reasons many callers do so.</p>
     *
     * @param env the static context of the expression
     * @param contextItemType the static type of "." at the point where this expression is invoked.
     * The parameter is set to null if it is known statically that the context item will be undefined.
     * If the type of the context item is not known statically, the argument is set to
     * {@link net.sf.saxon.type.Type#ITEM_TYPE}
     * @exception net.sf.saxon.trans.StaticError if an error is discovered during this phase
     *     (typically a type error)
     * @return the original expression, rewritten to perform necessary run-time type checks,
     * and to perform other type-related optimizations
     */

    public Expression typeCheck(StaticContext env, ItemType contextItemType) throws XPathException {
        return this;
    }


    /**
     * Perform optimisation of an expression and its subexpressions. This is the third and final
     * phase of static optimization.
     *
     * <p>This method is called after all references to functions and variables have been resolved
     * to the declaration of the function or variable, and after all type checking has been done.</p>
     * @param opt the optimizer in use. This provides access to supporting functions; it also allows
     * different optimization strategies to be used in different circumstances.
     * @param env the static context of the expression
     * @param contextItemType the static type of "." at the point where this expression is invoked.
     * The parameter is set to null if it is known statically that the context item will be undefined.
     * If the type of the context item is not known statically, the argument is set to
     * {@link net.sf.saxon.type.Type#ITEM_TYPE}
     * @exception net.sf.saxon.trans.StaticError if an error is discovered during this phase
     *     (typically a type error)
     * @return the original expression, rewritten if appropriate to optimize execution
     */

    public Expression optimize(Optimizer opt, StaticContext env, ItemType contextItemType) throws XPathException {
        return this;
    }


    /**
     * Offer promotion for this subexpression. The offer will be accepted if the subexpression
     * is not dependent on the factors (e.g. the context item) identified in the PromotionOffer.
     * By default the offer is not accepted - this is appropriate in the case of simple expressions
     * such as constant values and variable references where promotion would give no performance
     * advantage. This method is always called at compile time.
     *
     * @param offer details of the offer, for example the offer to move
     *     expressions that don't depend on the context to an outer level in
     *     the containing expression
     * @exception net.sf.saxon.trans.XPathException if any error is detected
     * @return if the offer is not accepted, return this expression unchanged.
     *      Otherwise return the result of rewriting the expression to promote
     *      this subexpression
     */

    public Expression promote(PromotionOffer offer) throws XPathException {
        // The following temporary code checks that this method is implemented for all expressions
        // that have subexpressions
//        if (iterateSubExpressions().hasNext()) {
//            throw new UnsupportedOperationException("promote is not implemented for " + this.getClass());
//        }
        return this;
    }

    /**
     * Get the static properties of this expression (other than its type). The result is
     * bit-signficant. These properties are used for optimizations. In general, if
     * property bit is set, it is true, but if it is unset, the value is unknown.
     *
     * @return a set of flags indicating static properties of this expression
     */

    public final int getSpecialProperties() {
        if (staticProperties == -1) {
            computeStaticProperties();
        }
        return staticProperties & StaticProperty.SPECIAL_PROPERTY_MASK;
    }

    /**
     * Determine the static cardinality of the expression. This establishes how many items
     * there will be in the result of the expression, at compile time (i.e., without
     * actually evaluating the result.
     *
     * @return one of the values Cardinality.ONE_OR_MORE,
     *     Cardinality.ZERO_OR_MORE, Cardinality.EXACTLY_ONE,
     *     Cardinality.ZERO_OR_ONE, Cardinality.EMPTY. This default
     *     implementation returns ZERO_OR_MORE (which effectively gives no
     *     information).
     */

    public int getCardinality() {
        if (staticProperties == -1) {
            computeStaticProperties();
        }
        return staticProperties & StaticProperty.CARDINALITY_MASK;
    }

    /**
	 * Determine the data type of the expression, if possible. All expression return
	 * sequences, in general; this method determines the type of the items within the
	 * sequence, assuming that (a) this is known in advance, and (b) it is the same for
	 * all items in the sequence.
     *
     * <p>This method should always return a result, though it may be the best approximation
     * that is available at the time.</p>
	 *
	 * @return a value such as Type.STRING, Type.BOOLEAN, Type.NUMBER,
	 *     Type.NODE, or Type.ITEM (meaning not known at compile time)
     * @param th
     */

    public abstract ItemType getItemType(TypeHierarchy th);

    /**
     * Determine which aspects of the context the expression depends on. The result is
     * a bitwise-or'ed value composed from constants such as XPathContext.VARIABLES and
     * XPathContext.CURRENT_NODE. The default implementation combines the intrinsic
     * dependencies of this expression with the dependencies of the subexpressions,
     * computed recursively. This is overridden for expressions such as FilterExpression
     * where a subexpression's dependencies are not necessarily inherited by the parent
     * expression.
     *
     * @return a set of bit-significant flags identifying the dependencies of
     *     the expression
     */

    public int getDependencies() {
        // Implemented as a memo function: we only compute the dependencies
        // for each expression once
        if (staticProperties == -1) {
            computeStaticProperties();
        }
        return staticProperties & StaticProperty.DEPENDENCY_MASK;
    }

    /**
     * Get the immediate sub-expressions of this expression. Default implementation
     * returns a zero-length array, appropriate for an expression that has no
     * sub-expressions.
     * @return an iterator containing the sub-expressions of this expression
     */

    public Iterator iterateSubExpressions() {
        return Collections.EMPTY_LIST.iterator();
    }

    /**
     * Get the expression that immediately contains this expression. This method
     * returns null for an outermost expression; it also return null in the case
     * of literal values. For an XPath expression occurring within an XSLT stylesheet,
     * this method returns the XSLT instruction containing the XPath expression.
     * @return the expression that contains this expression, if known; return null
     * if there is no containing expression or if the containing expression is unknown.
     */

    public Container getParentExpression() {
        return parentExpression;
    }

    /**
     * Evaluate an expression as a single item. This always returns either a single Item or
     * null (denoting the empty sequence). No conversion is done. This method should not be
     * used unless the static type of the expression is a subtype of "item" or "item?": that is,
     * it should not be called if the expression may return a sequence. There is no guarantee that
     * this condition will be detected.
     *
     * @param context The context in which the expression is to be evaluated
     * @exception net.sf.saxon.trans.XPathException if any dynamic error occurs evaluating the
     *     expression
     * @return the node or atomic value that results from evaluating the
     *     expression; or null to indicate that the result is an empty
     *     sequence
     */

    public Item evaluateItem(XPathContext context) throws XPathException {
        return iterate(context).next();
    }

    /**
     * Return an Iterator to iterate over the values of a sequence. The value of every
     * expression can be regarded as a sequence, so this method is supported for all
     * expressions. This default implementation handles iteration for expressions that
     * return singleton values: for non-singleton expressions, the subclass must
     * provide its own implementation.
     *
     * @exception net.sf.saxon.trans.XPathException if any dynamic error occurs evaluating the
     *     expression
     * @param context supplies the context for evaluation
     * @return a SequenceIterator that can be used to iterate over the result
     *     of the expression
     */

    public SequenceIterator iterate(XPathContext context) throws XPathException {
        Item value = evaluateItem(context);
        return SingletonIterator.makeIterator(value);
    }

    /**
     * Get the effective boolean value of the expression. This returns false if the value
     * is the empty sequence, a zero-length string, a number equal to zero, or the boolean
     * false. Otherwise it returns true.
     *
     * @param context The context in which the expression is to be evaluated
     * @exception net.sf.saxon.trans.XPathException if any dynamic error occurs evaluating the
     *     expression
     * @return the effective boolean value
     */

    public boolean effectiveBooleanValue(XPathContext context) throws XPathException {
        return ExpressionTool.effectiveBooleanValue(iterate(context));
    }

    /**
     * Evaluate an expression as a String. This function must only be called in contexts
     * where it is known that the expression will return a single string (or where an empty sequence
     * is to be treated as a zero-length string). Implementations should not attempt to convert
     * the result to a string, other than converting () to "". This method is used mainly to
     * evaluate expressions produced by compiling an attribute value template.
     *
     * @exception net.sf.saxon.trans.XPathException if any dynamic error occurs evaluating the
     *     expression
     * @exception ClassCastException if the result type of the
     *     expression is not xs:string?
     * @param context The context in which the expression is to be evaluated
     * @return the value of the expression, evaluated in the current context.
     *     The expression must return a string or (); if the value of the
     *     expression is (), this method returns "".
     */

    public String evaluateAsString(XPathContext context) throws XPathException {
        Item o = evaluateItem(context);
//        if (o instanceof AtomicValue && !((AtomicValue)o).hasBuiltInType()) {
//            o = ((AtomicValue) o).getPrimitiveValue();
//        }
        StringValue value = (StringValue) o;  // the ClassCastException is deliberate
        if (value == null) return "";
        return value.getStringValue();
    }

    /**
     * Process the instruction, without returning any tail calls
     * @param context The dynamic context, giving access to the current node,
     * the current variables, etc.
     */

    public void process(XPathContext context) throws XPathException {
        int m = getImplementationMethod();

        if ((m & EVALUATE_METHOD) != 0) {
            Item item = evaluateItem(context);
            if (item != null) {
                context.getReceiver().append(item, locationId, NodeInfo.ALL_NAMESPACES);
            }

        } else if ((m & ITERATE_METHOD) != 0) {

            SequenceIterator iter = iterate(context);
            SequenceReceiver out = context.getReceiver();
            try {
                while (true) {
                    Item it = iter.next();
                    if (it == null) {
                        break;
                    }
                    out.append(it, locationId, NodeInfo.ALL_NAMESPACES);
                }
            } catch (XPathException e) {
                if (e.getLocator() == null) {
                    e.setLocator(this);
                }
                throw e;
            }

        } else {
            throw new AssertionError("process() is not implemented in the subclass " + this.getClass());
        }
    }

    /**
     * Diagnostic print of expression structure. The abstract expression tree
     * is written to the supplied output destination.
     *
     * @param level indentation level for this expression
     @param out Output destination
     @param config
     */

    abstract public void display(int level, PrintStream out, Configuration config);

    /**
     * Check that any elements and attributes constructed or returned by this expression are acceptable
     * in the content model of a given complex type. It's always OK to say yes, since the check will be
     * repeated at run-time. The process of checking element and attribute constructors against the content
     * model of a complex type also registers the type of content expected of those constructors, so the
     * static validation can continue recursively.
     */

    public void checkPermittedContents(SchemaType parentType, StaticContext env, boolean whole) throws XPathException {
        return;
    }

    public void setParentExpression(Container parent) {
        if (this == parent) {
            throw new AssertionError("Incestuous relationship!");
        }
        parentExpression = parent;
        if (parent instanceof Expression) {
            ((Expression)parent).resetStaticProperties();
        }
    }

    public static void setParentExpression(Expression child, Container parent) {
        if (child != null) {
            child.setParentExpression(parent);
        }
    }

    /**
     * Set up a parent-child relationship between this expression and a given child expression.
     * <p>
     * Note: many calls on this method are now redundant, but are kept in place for "belt-and-braces"
     * reasons. The rule is that an implementation of simplify(), typeCheck(), or optimize() that returns
     * a value other than "this" is required to set the location information and parent pointer in the new
     * child expression. However, in the past this was often left to the caller, which did it by calling
     * this method, either unconditionally on return from one of these methods, or after testing that the
     * returned object was not the same as the original.
     * @param child the child expression
     */

    public void adoptChildExpression(Expression child) {
        if (child == null) {
            return;
        }
        if (child.getParentExpression() == this) {
            return;
        }
        child.setParentExpression(this);
        if (this.locationId == -1) {
            ExpressionTool.copyLocationInfo(child, this);
        } else if (child.locationId == -1) {
            ExpressionTool.copyLocationInfo(this, child);
        }
        if (staticProperties != -1) {
            resetStaticProperties();
        }
    }

    /**
     * Set the location ID on an expression.
     */

    public void setLocationId(int id) {
        locationId = id;
    }

    /**
     * Get the location ID of the expression
     */

    public final int getLocationId() {
        return locationId;
    }

    /**
     * Get the line number of the expression
     */

    public int getLineNumber() {
        if (locationId == -1) {
            if (parentExpression != null) {
                return parentExpression.getLineNumber();
            } else {
                return -1;
            }
        }
        return locationId & 0xfffff;
    }

    /**
     * Get the column number of the expression
     */

    public int getColumnNumber() {
        return -1;
    }

    /**
     * Get the systemId of the module containing the expression
     */

    public String getSystemId() {
        if (locationId == -1) {
            if (parentExpression != null) {
                return parentExpression.getSystemId();
            } else {
                return null;
            }
        }
        Executable exec = getExecutable();
        if (exec == null) {
            if (parentExpression == null) {
                return null;
            }
            if (parentExpression instanceof LocationProvider) {
                return ((LocationProvider)parentExpression).getSystemId(locationId);
            }
            return parentExpression.getSystemId();
        }
        LocationMap map = exec.getLocationMap();
        if (map == null) {
            return null;
        }
        return map.getSystemId(locationId);
    }

    /**
     * Get the publicId of the module containing the expression (to satisfy the SourceLocator interface)
     */

    public final String getPublicId() {
        return null;
    }

    /**
     * Get the executable containing this expression
     */

    public Executable getExecutable() {
        // This code is written defensively to detect cycles, which should never occur but occasionally
        // happen when the tree is rewritten incorrectly
        Expression e = this;
        int depth = 0;
        while (depth < 10000) {
            Container container = e.getParentExpression();
            if (container == null) {
                return null;
            } if (container == this) {
                int line = getLineNumber();
                String module = getSystemId();
                throw new IllegalStateException("Expression cannot contain itself: " +
                        this + " at line " + line + " of " + module);
            } else if (container instanceof GlobalVariable) {
                return ((GlobalVariable)container).getExecutable();
            } else if (container instanceof Expression) {
                e = (Expression)container;
                depth++;
                continue;
            } else {
                return container.getExecutable();
            }
        }
        showAncestorExpressions();
        int line = getLineNumber();
        //String module = getSystemId();
        throw new IllegalStateException("Expression tree appears to contain a cycle at line "
                + line);
    }

    /**
     * Get the Procedure containing this expression. Returns null if the expression is not
     * contained in a Procedure.
     */

    public Procedure getContainingProcedure() {
        // This code is written defensively to detect cycles, which should never occur but occasionally
        // happen when the tree is rewritten incorrectly
        Expression e = this;
        int depth = 0;
        while (depth < 10000) {
            Container container = e.getParentExpression();
            if (container == null) {
                return null;
            } if (container == this) {
                int line = getLineNumber();
                String module = getSystemId();
                throw new IllegalStateException("Expression cannot contain itself: " +
                        this + " at line " + line + " of " + module);
            } else if (container instanceof Procedure) {
                return (Procedure)container;
            } else if (container instanceof Expression) {
                e = (Expression)container;
                depth++;
                continue;
            } else {
                return null;
            }
        }
        showAncestorExpressions();
        int line = getLineNumber();
        String module = getSystemId();
        throw new IllegalStateException("Expression tree appears to contain a cycle at line "
                + line + " of " + module);
    }

    /**
     * Get the LocationProvider allowing location identifiers to be resolved.
     */

    public LocationProvider getLocationProvider() {
        Executable exec = getExecutable();
        if (exec != null) {
            return exec.getLocationMap();
        } else if (getParentExpression() instanceof LocationProvider) {
            return ((LocationProvider)getParentExpression());
        } else {
            return null;
        }
    }

    /**
     * Promote a subexpression if possible, and if the expression was changed, carry out housekeeping
     * to reset the static properties and correct the parent pointers in the tree
     */

    public final Expression doPromotion(Expression subexpression, PromotionOffer offer) throws XPathException {
        Expression e = subexpression.promote(offer);
        if (e != subexpression) {
            adoptChildExpression(e);
            resetStaticProperties();
        }
        return e;
    }

    /**
     * Compute the static properties. This should only be done once for each
     * expression.
     */

    public final void computeStaticProperties() {
        staticProperties =
                computeDependencies() |
                computeCardinality() |
                computeSpecialProperties();
    }

    /**
     * Reset the static properties. This should be done whenever the expression is changed in a way that might
     * affect the properties. It causes the properties to be recomputed next time they are needed.
     */

    public final void resetStaticProperties() {
        staticProperties = -1;
        Container container = parentExpression;
        // code written defensively to check for cycles
        int depth = 0;
        while (container instanceof Expression) {
            ((Expression)container).staticProperties = -1;
            if (depth++ > 10000) {
                showAncestorExpressions();
                int line = getLineNumber();
                String module = getSystemId();
                throw new IllegalStateException("Expression tree appears to contain a cycle at line "
                        + line + " of " + module);
            }
            container = ((Expression)container).parentExpression;
        }
    }

    /**
     * Diagnostic output method
     */

    private void showAncestorExpressions() {
        Expression exp = this;
        for (int i=0; i<20; i++) {
            System.err.println("in " + exp);
            Container c = exp.getParentExpression();
            if (c instanceof Expression) {
                exp = (Expression)c;
            } else {
                break;
            }
        }
    }

    protected abstract int computeCardinality();

    public int computeSpecialProperties() {
        return 0;
    }

    /**
     * Compute the dependencies of an expression, as the union of the
     * dependencies of its subexpressions. (This is overridden for path expressions
     * and filter expressions, where the dependencies of a subexpression are not all
     * propogated). This method should be called only once, to compute the dependencies;
     * after that, getDependencies should be used.
     * @return the depencies, as a bit-mask
     */

    public int computeDependencies() {
        int dependencies = getIntrinsicDependencies();
        for (Iterator children = iterateSubExpressions(); children.hasNext();) {
            dependencies |= ((Expression)children.next()).getDependencies();
        }
        return dependencies;
    }

    /**
     * Determine the intrinsic dependencies of an expression, that is, those which are not derived
     * from the dependencies of its subexpressions. For example, position() has an intrinsic dependency
     * on the context position, while (position()+1) does not. The default implementation
     * of the method returns 0, indicating "no dependencies".
     *
     * @return a set of bit-significant flags identifying the "intrinsic"
     *     dependencies. The flags are documented in class net.sf.saxon.value.StaticProperty
     */

    public int getIntrinsicDependencies() {
        return 0;
    }

    /**
      * Replace one subexpression by a replacement subexpression
      * @param original the original subexpression
      * @param replacement the replacement subexpression
      * @return true if the original subexpression is found
      */

    public boolean replaceSubExpression(Expression original, Expression replacement) {
        // overridden in subclasses
        throw new IllegalArgumentException("Invalid replacement");
    }

    /**
     * Suppress validation on contained element constructors, on the grounds that the parent element
     * is already performing validation. The default implementation does nothing.
     */

    public void suppressValidation(int validationMode) {
        // do nothing
    }

    /**
     * Mark tail-recursive calls on stylesheet functions. For most expressions, this does nothing.
     *
     * @return 0 if no tail call was found; 1 if a tail call on a different function was found;
     * 2 if a tail recursive call was found and if this call accounts for the whole of the value.
     */

    public int markTailFunctionCalls(int nameCode, int arity) {
        return 0;
    }

    /**
     * Get the local variables (identified by their slot numbers) on which this expression depends.
     * Should only be called if the caller has established that there is a dependency on local variables.
     */

    public int[] getSlotsUsed() {
        if (slotsUsed != null) {
            return slotsUsed;
        }
        IntHashSet slots = new IntHashSet(10);
        gatherSlotsUsed(this, slots);
        slotsUsed = new int[slots.size()];
        int i=0;
        IntIterator iter = slots.iterator();
        while (iter.hasNext()) {
            slotsUsed[i++] = iter.next();
        }
        Arrays.sort(slotsUsed);
        return slotsUsed;
    }

    private static void gatherSlotsUsed(Expression exp, IntHashSet slots) {
        if (exp instanceof VariableReference) {
            Binding binding = ((VariableReference)exp).getBinding();
            if (!binding.isGlobal()) {
                int slot = binding.getLocalSlotNumber();
                if (slot != -1) {
                    if (!slots.contains(slot)) {
                        slots.add(slot);
                    }
                }
            }
        } else {
            Iterator iter = exp.iterateSubExpressions();
            while (iter.hasNext()) {
                Expression sub = (Expression)iter.next();
                gatherSlotsUsed(sub, slots);
            }
        }
    }

    /**
     * Method used in subclasses to signal a dynamic error
     */

    protected void dynamicError(String message, String code, XPathContext context) throws DynamicError {
        DynamicError err = new DynamicError(message, getSourceLocator());
        err.setXPathContext(context);
        err.setErrorCode(code);
        throw err;
    }

    /**
     * Method used in subclasses to signal a runtime type error
     */

    protected void typeError(String message, String errorCode, XPathContext context) throws DynamicError {
        DynamicError e = new DynamicError(message, getSourceLocator());
        e.setIsTypeError(true);
        e.setErrorCode(errorCode);
        e.setXPathContext(context);
        throw e;
    }

    /**
     * Get a SourceLocator for this expression
     */

    private SourceLocator getSourceLocator() {
        return ExpressionTool.getLocator(this);
    }

    /**
     * Get InstructionInfo for this expression
     */

    public InstructionInfo getInstructionInfo() {
        InstructionDetails details = new InstructionDetails();
        details.setConstructType(getConstructType());
        details.setProperty("expression", this);
        details.setSystemId(getSystemId());
        details.setLineNumber(getLineNumber());
        details.setColumnNumber(getColumnNumber());
        if (this instanceof Assignation) {
            details.setObjectNameCode(((Assignation)this).getVariableNameCode());
        }
        return details;
    }

    /**
     * Get the type of this expression for use in tracing and diagnostics
     * @return the type of expression, as enumerated in class {@link net.sf.saxon.trace.Location}
     */

    protected int getConstructType() {
        return Location.XPATH_EXPRESSION;
    }

    /**
     * Diagnostic method: search the tree for an expression whose parent expression is incorrectly set
     */

    public boolean hasBadParentPointer() {
        Iterator iter = iterateSubExpressions();
        while (iter.hasNext()) {
            Expression exp = (Expression)iter.next();
            if (this != exp.getParentExpression()) {
                System.err.println("Bad parent pointer to " + exp.getParentExpression() + " found in " + exp);
                return true;
            }
            if (exp.hasBadParentPointer()) {
                System.err.println("Found in "+ exp);
                return true;
            }
        }
        return false;
    }

    /**
     * Get the host language (XSLT, XQuery, XPath) used to implement the code in this container
     * @return typically {@link net.sf.saxon.Configuration#XSLT} or {@link net.sf.saxon.Configuration#XQUERY}
     */

    public int getHostLanguage() {
        if (parentExpression == null) {
            // this shouldn't happen, but it's been known
            return Configuration.XSLT;
        }
        return parentExpression.getHostLanguage();
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