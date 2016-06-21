////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
// Copyright (c) 2015 Saxonica Limited.
// This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
// If a copy of the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
// This Source Code Form is "Incompatible With Secondary Licenses", as defined by the Mozilla Public License, v. 2.0.
////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

package net.sf.saxon.functions;

import net.sf.saxon.expr.Callable;
import net.sf.saxon.expr.XPathContext;
import net.sf.saxon.om.Item;
import net.sf.saxon.om.NodeInfo;
import net.sf.saxon.om.Sequence;
import net.sf.saxon.trans.XPathException;
import net.sf.saxon.type.Type;
import net.sf.saxon.value.StringValue;

/**
 * Implements the unparsed-entity-uri() function defined in XSLT 1.0
 * and the unparsed-entity-public-id() function defined in XSLT 2.0
 */


public abstract class UnparsedEntity extends SystemFunction implements Callable {

    public static int URI = 0;
    public static int PUBLIC_ID = 1;

    public abstract int getOp();

//    /**
//     * Simplify: add a second implicit argument, the context document
//     *
//     * @param env
//     */
//
//    /*@NotNull*/
//    public Expression simplify(StaticContext env) throws XPathException {
//        UnparsedEntity f = (UnparsedEntity) super.simplify(env);
//        f.addContextDocumentArgument(1);
//        return f;
//    }

    /**
     * Type-check the expression. This also calls preEvaluate() to evaluate the function
     * if all the arguments are constant; functions that do not require this behavior
     * can override the preEvaluate method.
     */

    /*@NotNull*/
//    public Expression typeCheck(ExpressionVisitor visitor, ContextItemStaticInfo contextInfo) throws XPathException {
//        try {
//            return super.typeCheck(visitor, contextInfo);
//        } catch (XPathException err) {
//            if ("XPDY0002".equals(err.getErrorCodeLocalPart())) {
//                if (operation == URI) {
//                    XPathException e = new XPathException("Cannot call the unparsed-entity-uri()" +
//                            " function when there is no context node");
//                    e.setErrorCode("XTDE1370");
//                    throw e;
//                } else {
//                    XPathException e = new XPathException("Cannot call the unparsed-entity-public-id()" +
//                            " function when there is no context node");
//                    e.setErrorCode("XTDE1380");
//                    e.setLocation(getLocation());
//                    throw e;
//                }
//            }
//            throw err;
//        }
//    }

    /**
     * Evaluate the expression
     *
     * @param context   the dynamic evaluation context
     * @param arguments the values of the arguments, supplied as SequenceIterators
     * @return the result of the evaluation, in the form of a SequenceIterator
     * @throws net.sf.saxon.trans.XPathException
     *          if a dynamic error occurs during the evaluation of the expression
     */
    public StringValue call(XPathContext context, Sequence[] arguments) throws XPathException {
        int operation = getOp();
        String arg0 = arguments[0].head().getStringValue();
        NodeInfo doc = null;
        if (getArity() == 1) {
            Item it = context.getContextItem();
            if (it instanceof NodeInfo) {
                doc = ((NodeInfo)it).getRoot();
            }
            if (doc == null || doc.getNodeKind() != Type.DOCUMENT) {
                String code = operation == URI ? "XTDE1370" : "XTDE1380";
                throw new XPathException("In function " + getFunctionName().getDisplayName() +
                    ", the context item must be a node in a tree whose root is a document node", code, context);
            }
        } else {
            doc = (NodeInfo) arguments[1].head();
            if (doc != null) {
                doc = doc.getRoot();
            }
            if (doc == null || doc.getNodeKind() != Type.DOCUMENT) {
                String code = operation == URI ? "XTDE1370" : "XTDE1380";
                throw new XPathException("In function " + getFunctionName().getDisplayName() +
                    ", the second argument must be a document node", code, context);
            }
        }
        String[] ids = doc.getTreeInfo().getUnparsedEntity(arg0);
        return ids == null ? StringValue.EMPTY_STRING : new StringValue(ids[operation]);
    }

    public static class UnparsedEntityUri extends UnparsedEntity {
        public int getOp() {
            return URI;
        }
    }

    public static class UnparsedEntityPublicId extends UnparsedEntity {
        public int getOp() {
            return PUBLIC_ID;
        }
    }
}

