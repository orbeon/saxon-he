package net.sf.saxon.xpath;
import net.sf.saxon.expr.*;
import net.sf.saxon.om.SequenceIterator;
import net.sf.saxon.om.ValueRepresentation;
import net.sf.saxon.trans.DynamicError;
import net.sf.saxon.trans.XPathException;
import net.sf.saxon.type.ItemType;
import net.sf.saxon.type.Type;
import net.sf.saxon.type.TypeHierarchy;
import net.sf.saxon.value.SequenceType;
import net.sf.saxon.value.Value;
import net.sf.saxon.Configuration;

import javax.xml.xpath.XPathFunction;
import javax.xml.xpath.XPathFunctionException;
import java.util.ArrayList;
import java.util.List;



/**
* This class is an expression that calls an external function supplied using the
 * JAXP XPathFunction interface
*/

public class XPathFunctionCall extends FunctionCall {

    private XPathFunction function;
    /**
     * Default constructor
     */

    public XPathFunctionCall(XPathFunction function) {
        this.function = function;
    }

    /**
    * preEvaluate: this method suppresses compile-time evaluation by doing nothing
    * (because the external function might have side-effects and might use the context)
    */

    public Expression preEvaluate(StaticContext env) {
        return this;
    }


    /**
    * Method called by the expression parser when all arguments have been supplied
    */

    public void checkArguments(StaticContext env) throws XPathException {
    }


    /**
     * Determine which aspects of the context the expression depends on. XPath external
     * functions are given no access to context information so they cannot have any
     * dependencies on it.
    */

    public int getIntrinsicDependencies() {
        return 0;
    }


    /**
    * Evaluate the function. <br>
    * @param context The context in which the function is to be evaluated
    * @return a Value representing the result of the function.
    * @throws XPathException if the function cannot be evaluated.
    */

    public SequenceIterator iterate(XPathContext context) throws XPathException {
        ValueRepresentation[] argValues = new ValueRepresentation[argument.length];
        for (int i=0; i<argValues.length; i++) {
            argValues[i] = ExpressionTool.lazyEvaluate(argument[i], context, 10);
        }
        return call(argValues, context);
    }


    /**
     * Call an extension function previously identified using the bind() method. A subclass
     * can override this method.
     * @param argValues  The values of the arguments
     * @return  The value returned by the extension function
     */

    public SequenceIterator call(ValueRepresentation[] argValues, XPathContext context) throws XPathException {
        List convertedArgs = new ArrayList(argValues.length);
        for (int i=0; i<argValues.length; i++) {
            convertedArgs.add(Value.asValue(argValues[i]).convertToJava(Object.class, context));
        }
        try {
            Object result = function.evaluate(convertedArgs);
            Configuration config = context.getConfiguration();
            return Value.convertJavaObjectToXPath(result, SequenceType.ANY_SEQUENCE, context).iterate();
        } catch (XPathFunctionException e) {
            throw new DynamicError(e);
        }
    }

    /**
     * Determine the data type of the expression, if possible. All expressions return
     * sequences, in general; this method determines the type of the items within the
     * sequence, assuming that (a) this is known in advance, and (b) it is the same for
     * all items in the sequence.
     *
     * <p>This method will always return a result, though it may be the best approximation
     * that is available at the time.</p>
     *
     * @return the item type
     * @param th
     */

    public ItemType getItemType(TypeHierarchy th) {
        return Type.ITEM_TYPE;
    }

    /**
     * Determine the cardinality of the result
     * @return ZERO_OR_MORE (we don't know)
     */
    public int computeCardinality() {
        return StaticProperty.ALLOWS_ZERO_OR_MORE;
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
// Contributor(s): Gunther Schadow (changes to allow access to public fields; also wrapping
// of extensions and mapping of null to empty sequence).
//
