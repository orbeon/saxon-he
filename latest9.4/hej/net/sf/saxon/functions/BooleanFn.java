package net.sf.saxon.functions;

import net.sf.saxon.Configuration;
import net.sf.saxon.expr.*;
import net.sf.saxon.expr.parser.ExpressionTool;
import net.sf.saxon.expr.parser.ExpressionVisitor;
import net.sf.saxon.expr.parser.Optimizer;
import net.sf.saxon.expr.parser.TypeChecker;
import net.sf.saxon.om.Item;
import net.sf.saxon.om.SequenceIterator;
import net.sf.saxon.pattern.NodeTest;
import net.sf.saxon.trans.XPathException;
import net.sf.saxon.tree.iter.SingletonIterator;
import net.sf.saxon.type.BuiltInAtomicType;
import net.sf.saxon.type.TypeHierarchy;
import net.sf.saxon.value.BooleanValue;
import net.sf.saxon.value.Cardinality;

/**
* This class supports the XPath functions boolean(), not(), true(), and false()
*/


public class BooleanFn extends SystemFunction implements Negatable, CallableExpression {

    /**
     * Static analysis: prevent sorting of the argument
     */

    public void checkArguments(/*@NotNull*/ ExpressionVisitor visitor) throws XPathException {
        super.checkArguments(visitor);
        XPathException err = TypeChecker.ebvError(argument[0], visitor.getConfiguration().getTypeHierarchy());
        if (err != null) {
            err.setLocator(this);
            throw err;
        }
        Optimizer opt = visitor.getConfiguration().obtainOptimizer();
        argument[0] = ExpressionTool.unsortedIfHomogeneous(opt, argument[0]);
    }

    /**
     * Perform optimisation of an expression and its subexpressions.
     * <p/>
     * <p>This method is called after all references to functions and variables have been resolved
     * to the declaration of the function or variable, and after all type checking has been done.</p>
     *
     * @param visitor an expression visitor
     * @param contextItemType the static type of "." at the point where this expression is invoked.
     *                        The parameter is set to null if it is known statically that the context item will be undefined.
     *                        If the type of the context item is not known statically, the argument is set to
     *                        {@link net.sf.saxon.type.Type#ITEM_TYPE}
     * @return the original expression, rewritten if appropriate to optimize execution
     * @throws XPathException if an error is discovered during this phase
     *                                        (typically a type error)
     */

    /*@NotNull*/
    public Expression optimize(/*@NotNull*/ ExpressionVisitor visitor, ExpressionVisitor.ContextItemType contextItemType) throws XPathException {
        Expression e = super.optimize(visitor, contextItemType);
        if (e == this) {
            Expression ebv = rewriteEffectiveBooleanValue(argument[0], visitor, contextItemType);
            if (ebv != null) {
                ebv = ebv.optimize(visitor, contextItemType);
                if (ebv.getItemType(visitor.getConfiguration().getTypeHierarchy()) == BuiltInAtomicType.BOOLEAN &&
                        ebv.getCardinality() == StaticProperty.EXACTLY_ONE) {
                    return ebv;
                } else {
                    argument[0] = ebv;
                    adoptChildExpression(ebv);
                    return this;
                }
            }
        }
        return e;
    }

    /**
     * Check whether this specific instance of the expression is negatable
     *
     * @return true if it is
     */

    public boolean isNegatable(ExpressionVisitor visitor) {
        return true;
    }

    /**
     * Create an expression that returns the negation of this expression
     * @return the negated expression
     */

    public Expression negate() {
        return SystemFunction.makeSystemFunction("not", getArguments());
    }

    /**
     * Optimize an expression whose effective boolean value is required. It is appropriate
     * to apply this rewrite to any expression whose value will be obtained by calling
     * the Expression.effectiveBooleanValue() method (and not otherwise)
     * @param exp the expression whose EBV is to be evaluated
     * @param visitor an expression visitor
     * @param contextItemType the type of the context item for this expression
     * @return an expression that returns the EBV of exp, or null if no optimization was possible
     * @throws XPathException if static errors are found
     */

    public static Expression rewriteEffectiveBooleanValue(
            Expression exp, ExpressionVisitor visitor, ExpressionVisitor.ContextItemType contextItemType) throws XPathException {
        Configuration config = visitor.getConfiguration();
        TypeHierarchy th = config.getTypeHierarchy();
        exp = ExpressionTool.unsortedIfHomogeneous(config.obtainOptimizer(), exp);
        if (exp instanceof ValueComparison) {
            ValueComparison vc = (ValueComparison)exp;
            if (vc.getResultWhenEmpty() == null) {
                vc.setResultWhenEmpty(BooleanValue.FALSE);
            }
            return exp;
        } else if (exp instanceof BooleanFn) {
            return ((BooleanFn)exp).getArguments()[0];
        } else if (th.isSubType(exp.getItemType(th), BuiltInAtomicType.BOOLEAN) &&
                exp.getCardinality() == StaticProperty.EXACTLY_ONE) {
            return exp;
        } else if (exp instanceof Count) {
            // rewrite boolean(count(x)) => exists(x)
            FunctionCall exists = SystemFunction.makeSystemFunction("exists", ((Count)exp).getArguments());
            assert exists != null;
            exists.setLocationId(exp.getLocationId());
            return exists.optimize(visitor, contextItemType);
        } else if (exp.getItemType(th) instanceof NodeTest) {
            // rewrite boolean(x) => exists(x)
            FunctionCall exists = SystemFunction.makeSystemFunction("exists", new Expression[]{exp});
            assert exists != null;
            exists.setLocationId(exp.getLocationId());
            return exists.optimize(visitor, contextItemType);
        } else {
            return null;
        }
    }

 
    /**
    * Evaluate the function
    */

    public Item evaluateItem(XPathContext context) throws XPathException {
        return BooleanValue.get(effectiveBooleanValue(context));
    }

    /**
    * Evaluate the effective boolean value
    */

    public boolean effectiveBooleanValue(XPathContext c) throws XPathException {
        try {
            return argument[0].effectiveBooleanValue(c);
        } catch (XPathException e) {
            e.maybeSetLocation(this);
            e.maybeSetContext(c);
            throw e;
        }
    }

	public SequenceIterator<BooleanValue> call(SequenceIterator[] arguments,
			XPathContext c) throws XPathException {
		boolean bValue;
		if (Cardinality.allowsMany(getCardinality())) {
            bValue = ExpressionTool.effectiveBooleanValue(arguments[0]);
        } else {
            bValue = ExpressionTool.effectiveBooleanValue(arguments[0].next());
        }
		BooleanValue bItem = BooleanValue.get(bValue);
		
		return SingletonIterator.makeIterator(bItem);
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