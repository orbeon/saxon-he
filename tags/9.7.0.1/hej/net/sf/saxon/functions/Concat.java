////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
// Copyright (c) 2015 Saxonica Limited.
// This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
// If a copy of the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
// This Source Code Form is "Incompatible With Secondary Licenses", as defined by the Mozilla Public License, v. 2.0.
////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

package net.sf.saxon.functions;

import com.saxonica.ee.bytecode.ConcatCompiler;
import com.saxonica.ee.bytecode.ExpressionCompiler;
import net.sf.saxon.type.SpecificFunctionType;
import net.sf.saxon.expr.OperandRole;
import net.sf.saxon.expr.OperandUsage;
import net.sf.saxon.expr.XPathContext;
import net.sf.saxon.om.Item;
import net.sf.saxon.om.Sequence;
import net.sf.saxon.trans.XPathException;
import net.sf.saxon.tree.util.FastStringBuffer;
import net.sf.saxon.type.FunctionItemType;
import net.sf.saxon.value.SequenceType;
import net.sf.saxon.value.StringValue;

import java.util.Arrays;

/**
 * Implementation of the fn:concat() function
 */


public class Concat extends SystemFunction {

    @Override
    protected Sequence resultIfEmpty(int arg) {
        return null;
    }

    /**
     * Get the roles of the arguments, for the purposes of streaming
     *
     * @return an array of OperandRole objects, one for each argument
     */
    public OperandRole[] getOperandRoles() {
        OperandRole[] roles = new OperandRole[getArity()];
        OperandRole operandRole = new OperandRole(0, OperandUsage.ABSORPTION);
        for (int i = 0; i < getArity(); i++) {
            roles[i] = operandRole;
        }
        return roles;
    }



    /**
     * Get the item type of the function item
     *
     * @return the function item's type
     */
    @Override
    public FunctionItemType getFunctionItemType() {
        SequenceType[] argTypes = new SequenceType[getArity()];
        Arrays.fill(argTypes, SequenceType.OPTIONAL_ATOMIC);
        return new SpecificFunctionType(argTypes, SequenceType.SINGLE_STRING);
    }



    public StringValue call(XPathContext context, Sequence[] arguments) throws XPathException {
        FastStringBuffer fsb = new FastStringBuffer(FastStringBuffer.C64);
        for (Sequence arg : arguments) {
            Item item = arg.head();
            if (item != null) {
                fsb.append(item.getStringValueCS());
            }
        }
        return new StringValue(fsb);
    }


    /**
     * Get the required type of the nth argument
     */

    public SequenceType getRequiredType(int arg) {
        return getDetails().argumentTypes[0];
        // concat() is a special case
    }


    /**
     * Process the instruction in push mode. This avoids constructing the concatenated string
     * in memory, instead each argument can be sent straight to the serializer.
     *
     * @param context The dynamic context, giving access to the current node,
     *                the current variables, etc.
     */

//    public void process(/*@NotNull*/ XPathContext context) throws XPathException {
//        SequenceReceiver out = context.getReceiver();
//        if (out instanceof ComplexContentOutputter) {
//            // This optimization is only safe if the output forms part of document or element content
//            int numArgs = getArity();
//            // Start and end with an empty string to force space separation from any previous or following outputs
//            out.append(StringValue.EMPTY_STRING, 0, 0);
//            boolean empty = true;
//            for (int i = 0; i < numArgs; i++) {
//                AtomicValue val = (AtomicValue) getArg(i).evaluateItem(context);
//                if (val != null) {
//                    out.characters(val.getStringValueCS(), 0, 0);
//                    empty = false;
//                }
//            }
//            if (!empty) {
//                out.append(StringValue.EMPTY_STRING, 0, 0);
//            }
//        } else {
//            out.append(evaluateItem(context), 0, 0);
//        }
//    }

//#ifdefined BYTECODE

    /**
     * Return the compiler of the Concat expression
     *
     * @return the relevant ExpressionCompiler
     */
    @Override
    public ExpressionCompiler getExpressionCompiler() {
        return new ConcatCompiler();
    }
//#endif

}

