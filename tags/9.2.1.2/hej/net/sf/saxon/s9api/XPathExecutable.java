package net.sf.saxon.s9api;

import net.sf.saxon.expr.StaticContext;
import net.sf.saxon.sxpath.IndependentContext;
import net.sf.saxon.sxpath.XPathExpression;
import net.sf.saxon.sxpath.XPathVariable;

import java.util.ArrayList;
import java.util.Iterator;

/**
 * An XPathExecutable represents the compiled form of an XPath expression.
 * To evaluate the expression, it must first be loaded to form an {@link XPathSelector}.
 *
 * <p>An XPathExecutable is immutable, and therefore thread-safe. It is simplest to load
 * a new XPathSelector each time the expression is to be evaluated. However, the XPathSelector
 * is serially reusable within a single thread.</p>
 *
 * <p>An XPathExecutable is created by using the {@link XPathCompiler#compile} method
 * on the {@link XPathCompiler} class.</p>
 */

public class XPathExecutable {

    private XPathExpression exp;
    private Processor processor;
    private IndependentContext env;
    //private ArrayList<XPathVariable> declaredVariables;

    // protected constructor

    protected XPathExecutable(XPathExpression exp, Processor processor,
            IndependentContext env//, ArrayList<XPathVariable> declaredVariables
             ) {
        this.exp = exp;
        this.processor = processor;
        this.env = env;
        //this.declaredVariables = declaredVariables;
    }

    /**
     * Load the compiled XPath expression to prepare it for execution.
     * @return An XPathSelector. The returned XPathSelector can be used to set up the
     * dynamic context, and then to evaluate the expression.
     */

    public XPathSelector load() {
        ArrayList<XPathVariable> declaredVariables = new ArrayList<XPathVariable>();
        for (Iterator iter=env.iterateExternalVariables(); iter.hasNext();) {
            XPathVariable var = (XPathVariable)iter.next();
            declaredVariables.add(var);
        }
        return new XPathSelector(exp, declaredVariables);
    }

    /**
     * Get the ItemType of the items in the result of the expression, as determined by static analysis. This
     * is the most precise ItemType that the processor is able to determine from static examination of the
     * expression; the actual items in the expression result are guaranteed to belong to this ItemType or to a subtype
     * of this ItemType.
     * @return the statically-determined ItemType of the result of the expression
     * @since 9.1
     */

    public ItemType getResultItemType() {
        net.sf.saxon.type.ItemType it =
                exp.getInternalExpression().getItemType(processor.getUnderlyingConfiguration().getTypeHierarchy());
        return new ConstructedItemType(it, processor);
    }

    /**
     * Get the statically-determined cardinality of the result of the expression. This is the most precise cardinality
     * that the processor is able to determine from static examination of the expression.
     * @return the statically-determined cardinality of the result of the expression
     * @since 9.1
     */

    public OccurrenceIndicator getResultCardinality() {
        int card = exp.getInternalExpression().getCardinality();
        return OccurrenceIndicator.getOccurrenceIndicator(card);
    }

    /**
     * Get an iterator over the names of all the external variables. This includes both variables that have
     * been explicitly declared using a call to <tt>declareVariable()</tt>, and variables that are implicitly
     * declared by reference in the case where the <tt>allowUndeclaredVariables</tt> option is set. It does
     * not include range variables bound in a <tt>for</tt>, <tt>some</tt>, or <tt>every</tt> expression.
     *
     * <p>If the <tt>allowUndeclaredVariables</tt> option is set, this method allows discovery of the variable
     * references that appear in the compiled expression.</p>
     * @since 9.2
     */

    public Iterator<QName> iterateExternalVariables() {
        final Iterator varIterator = env.iterateExternalVariables();
        return new Iterator<QName>() {
            public boolean hasNext() {
                return varIterator.hasNext();
            }
            public QName next() {
                return new QName(((XPathVariable)varIterator.next()).getVariableQName());
            }
            public void remove() {
                throw new UnsupportedOperationException("remove");
            }
        };
    }

    /**
     * Get the required item type of a declared variable in the static context of the expression.
     * @param variableName the name of a declared variable
     * @return the required item type.
     * <p>If the variable was explicitly declared, this will be the item type that was set when the
     * variable was declared. If no item type was set, it will be {@link ItemType#ANY_ITEM}.</p>
     * <p>If the variable was implicitly declared by reference (which can happen only when the
     * <tt>allowUndeclaredVariables</tt> option is set), the returned type will be {@link ItemType#ANY_ITEM}.</p>
     * <p>If no variable with the specified QName has been declared either explicitly or implicitly,
     * the method returns null.</p>
     * @since 9.2
     */

    public ItemType getRequiredItemTypeForVariable(QName variableName) {
        XPathVariable var = env.getExternalVariable(variableName.getStructuredQName());
        if (var == null) {
            return null;
        } else {
            return new ConstructedItemType(var.getRequiredType().getPrimaryType(), processor);
        }
    }

    /**
     * Get the required cardinality of a declared variable in the static context of the expression.
     * @param variableName the name of a declared variable
     * @return the required cardinality.
     * <p>If the variable was explicitly declared, this will be the occurrence indicator that was set when the
     * variable was declared. If no item type was set, it will be {@link OccurrenceIndicator#ZERO_OR_MORE}.</p>
     * <p>If the variable was implicitly declared by reference (which can happen only when the
     * <tt>allowUndeclaredVariables</tt> option is set), the returned type will be
     * {@link OccurrenceIndicator#ZERO_OR_MORE}.</p>
     * <p>If no variable with the specified QName has been declared either explicitly or implicitly,
     * the method returns null.</p>
     * @since 9.2
     */

    public OccurrenceIndicator getRequiredCardinalityForVariable(QName variableName) {
        XPathVariable var = env.getExternalVariable(variableName.getStructuredQName());
        if (var == null) {
            return null;
        } else {
            return OccurrenceIndicator.getOccurrenceIndicator(var.getRequiredType().getCardinality());
        }
    }

    /**
     * Get the underlying implementation object representing the compiled XPath expression.
     * This method provides access to lower-level Saxon classes and methods which may be subject to change
     * from one release to the next.
     * @return the underlying compiled XPath expression.
     */

    public XPathExpression getUnderlyingExpression() {
        return exp;
    }

    /**
     * Get the underlying implementation object representing the static context of the compiled
     * XPath expression. This method provides access to lower-level Saxon classes and methods which may be
     * subject to change from one release to the next.
     * @return the underlying static context.
     */

    public StaticContext getUnderlyingStaticContext() {
        return env;
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
// The Initial Developer of the Original Code is Michael H. Kay.
//
// Contributor(s):
//

