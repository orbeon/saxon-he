////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
// Copyright (c) 2015 Saxonica Limited.
// This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
// If a copy of the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
// This Source Code Form is "Incompatible With Secondary Licenses", as defined by the Mozilla Public License, v. 2.0.
////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

package net.sf.saxon.functions;

import net.sf.saxon.expr.*;
import net.sf.saxon.expr.sort.DocumentOrderIterator;
import net.sf.saxon.expr.sort.LocalOrderComparer;
import net.sf.saxon.om.*;
import net.sf.saxon.trans.XPathException;
import net.sf.saxon.tree.iter.SingletonIterator;
import net.sf.saxon.type.Type;
import net.sf.saxon.value.AtomicValue;
import net.sf.saxon.value.StringValue;
import net.sf.saxon.value.Whitespace;


/**
 * The XPath id() or element-with-id() function
 * XPath 2.0 version: accepts any sequence as the first parameter; each item in the sequence
 * is taken as an IDREFS value, that is, a space-separated list of ID values.
 * Also accepts an optional second argument to identify the target document, this
 * defaults to the context node.
 */


public abstract class SuperId extends SystemFunction {

    public static final int ID = 0;
    public static final int ELEMENT_WITH_ID = 1;

    public abstract int getOp();

    private boolean isSingletonId = false;


    /**
     * Static analysis: prevent sorting of the argument
     */

//    public void checkArguments(/*@NotNull*/ ExpressionVisitor visitor) throws XPathException {
//        super.checkArguments(visitor);
//        isSingletonId = !Cardinality.allowsMany(getArg(0).getCardinality());
//    }

    /**
     * Get the static properties of this expression (other than its type). The result is
     * bit-signficant. These properties are used for optimizations. In general, if
     * property bit is set, it is true, but if it is unset, the value is unknown.
     * @param arguments the actual arguments to the call
     */

    public int getSpecialProperties(Expression[] arguments) {
        int prop = StaticProperty.ORDERED_NODESET |
                StaticProperty.SINGLE_DOCUMENT_NODESET |
                StaticProperty.NON_CREATIVE;
        if ((getArity() == 1) ||
                (arguments[1].getSpecialProperties() & StaticProperty.CONTEXT_DOCUMENT_NODESET) != 0) {
            prop |= StaticProperty.CONTEXT_DOCUMENT_NODESET;
        }
        return prop;
    }


//    /**
//     * Evaluate the function to return an iteration of selected nodes.
//     */
//
//    /*@NotNull*/
//    public SequenceIterator iterate(XPathContext context) throws XPathException {
//
//        NodeInfo arg1;
//        try {
//            arg1 = (NodeInfo) getArg(1).evaluateItem(context);
//        } catch (XPathException e) {
//            if (context.getContextItem() instanceof AtomicValue) {
//                // Override the unhelpful message that trickles down...
//                XPathException e2 = new XPathException("id() function called when the context item is not a node");
//                e2.setErrorCode("XPTY0004");
//                e2.setXPathContext(context);
//                throw e2;
//            } else {
//                throw e;
//            }
//        }
//        arg1 = arg1.getRoot();
//        if (arg1.getNodeKind() != Type.DOCUMENT) {
//            throw new XPathException("In the " + getFunctionName().getLocalPart() + "() function," +
//                    " the tree being searched must be one whose root is a document node", "FODC0001", context);
//        }
//        DocumentInfo doc = (DocumentInfo) arg1;
//
//        int operation = ID; // TODO: temporary
//        if (isSingletonId) {
//            AtomicValue arg = (AtomicValue) getArg(0).evaluateItem(context);
//            if (arg == null) {
//                return EmptyIterator.emptyIterator();
//            }
//            String idrefs = arg.getStringValue();
//            return getIdSingle(doc, idrefs, operation);
//        } else {
//            SequenceIterator idrefs = getArg(0).iterate(context);
//            return getIdMultiple(doc, idrefs, operation);
//        }
//    }

    /**
     * Get an iterator over the nodes that have an id equal to one of the values is a whitespace separated
     * string
     *
     * @param doc       The document to be searched
     * @param idrefs    a string containing zero or more whitespace-separated ID values to be found in the document
     * @param operation either {@link #ID} or {@link #ELEMENT_WITH_ID}
     * @return an iterator over the nodes whose ID is one of the specified values
     * @throws XPathException if an error occurs
     */

    public static SequenceIterator getIdSingle(TreeInfo doc, String idrefs, int operation) throws XPathException {
        if (Whitespace.containsWhitespace(idrefs)) {
            StringTokenIterator tokens = new StringTokenIterator(idrefs);
            IdMappingFunction map = new IdMappingFunction();
            map.document = doc;
            map.operation = operation;
            SequenceIterator result = new MappingIterator<StringValue, NodeInfo>(tokens, map);
            return new DocumentOrderIterator(result, LocalOrderComparer.getInstance());
        } else {
            return SingletonIterator.makeIterator(doc.selectID(idrefs, operation == ELEMENT_WITH_ID));
        }
    }

    /**
     * Get an iterator over the nodes that have an id equal to one of the values is a set of whitespace separated
     * strings
     *
     * @param doc    The document to be searched
     * @param idrefs an iterator over a set of strings each of which is a string containing
     *               zero or more whitespace-separated ID values to be found in the document
     * @param operation distinguishes id() and element-with-id()
     * @return an iterator over the nodes whose ID is one of the specified values
     * @throws XPathException if an error occurs
     */

    public static SequenceIterator getIdMultiple(
        TreeInfo doc, SequenceIterator idrefs, int operation) throws XPathException {
        IdMappingFunction map = new IdMappingFunction();
        map.document = doc;
        map.operation = operation;
        SequenceIterator result = new MappingIterator<StringValue, NodeInfo>(idrefs, map);
        return new DocumentOrderIterator(result, LocalOrderComparer.getInstance());
    }

    /**
     * Evaluate the expression
     *
     * @param context   the dynamic evaluation context
     * @param arguments the values of the arguments, supplied as SequenceIterators
     * @return the result of the evaluation, in the form of a SequenceIterator
     * @throws net.sf.saxon.trans.XPathException
     *          if a dynamic error occurs during the evaluation of the expression
     */
    public Sequence call(XPathContext context, Sequence[] arguments) throws XPathException {
        NodeInfo start = arguments.length == 1 ? getContextNode(context) : (NodeInfo) arguments[1].head();
        NodeInfo arg1 = start.getRoot();
        if (arg1.getNodeKind() != Type.DOCUMENT) {
            throw new XPathException("In the " + getFunctionName().getLocalPart() + "() function," +
                    " the tree being searched must be one whose root is a document node", "FODC0001", context);
        }
        TreeInfo doc = arg1.getTreeInfo();
        SequenceIterator result;
        if (arguments[0] instanceof AtomicValue) {
            result = getIdSingle(doc, ((AtomicValue)arguments[0]).getStringValue(), getOp());
        } else {
            SequenceIterator idrefs = arguments[0].iterate();
            result = getIdMultiple(doc, idrefs, getOp());
        }
        return SequenceTool.toLazySequence(result);
    }

    private static class IdMappingFunction implements MappingFunction<StringValue, NodeInfo> {

        public TreeInfo document;
        private int operation;

        /**
         * Evaluate the function for a single string value
         * (implements the MappingFunction interface)
         */

        public SequenceIterator map(StringValue item) throws XPathException {

            String idrefs = Whitespace.trim(item.getStringValueCS());

            // If this value contains a space, we need to break it up into its
            // separate tokens; if not, we can process it directly

            if (Whitespace.containsWhitespace(idrefs)) {
                StringTokenIterator tokens = new StringTokenIterator(idrefs);
                IdMappingFunction submap = new IdMappingFunction();
                submap.document = document;
                submap.operation = operation;
                return new MappingIterator<StringValue, NodeInfo>(tokens, submap);

            } else {
                return SingletonIterator.makeIterator(document.selectID(idrefs, operation == ELEMENT_WITH_ID));
            }
        }
    }

    public static class Id extends SuperId {
        @Override
        public int getOp() {
            return ID;
        }
    }

    public static class ElementWithId extends SuperId {
        @Override
        public int getOp() {
            return ELEMENT_WITH_ID;
        }
    }



}

