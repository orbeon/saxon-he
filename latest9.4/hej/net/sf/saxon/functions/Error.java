package net.sf.saxon.functions;

import net.sf.saxon.expr.*;
import net.sf.saxon.expr.parser.ExpressionLocation;
import net.sf.saxon.expr.parser.ExpressionVisitor;
import net.sf.saxon.lib.NamespaceConstant;
import net.sf.saxon.om.Item;
import net.sf.saxon.om.NodeInfo;
import net.sf.saxon.om.SequenceIterator;
import net.sf.saxon.sxpath.XPathEvaluator;
import net.sf.saxon.sxpath.XPathExpression;
import net.sf.saxon.trans.XPathException;
import net.sf.saxon.tree.iter.SingletonIterator;
import net.sf.saxon.type.BuiltInAtomicType;
import net.sf.saxon.type.Type;
import net.sf.saxon.value.*;

/**
 * Implement XPath function fn:error()
 */

public class Error extends SystemFunction implements CallableExpression {

	@Override
	public int computeSpecialProperties() {
		return super.computeSpecialProperties() &~ StaticProperty.NON_CREATIVE;
	}

	/**
	 * preEvaluate: this method suppresses compile-time evaluation by doing nothing
	 * @param visitor an expression visitor
	 */

	public Expression preEvaluate(ExpressionVisitor visitor) {
		return this;
	}

	/**
	 * Determine whether this is a vacuous expression as defined in the XQuery update specification
	 * @return true if this expression is vacuous
	 */

	public boolean isVacuousExpression() {
		return true;
	}

	/**
	 * Evaluation of the expression always throws an error
	 */

	/*@Nullable*/ public Item evaluateItem(XPathContext context) throws XPathException {
		int len =argument.length;

		switch(len){
		case 0 : return error(context, null, null, null);
		case 1 : return error(context, (QNameValue)argument[0].evaluateItem(context), null, null);
		case 2:  return error(context, (QNameValue)argument[0].evaluateItem(context), (StringValue)argument[1].evaluateItem(context), null);
		case 3:  return error(context, (QNameValue)argument[0].evaluateItem(context), (StringValue)argument[1].evaluateItem(context), argument[2].iterate(context));
		default:
			return null;
		}
	}


	/**
	 * Evaluate an updating expression, adding the results to a Pending Update List.
	 * The default implementation of this method, which is used for non-updating expressions,
	 * throws an UnsupportedOperationException
	 *
	 * @param context the XPath dynamic evaluation context
	 * @param pul     the pending update list to which the results should be written
	 */

	public void evaluatePendingUpdates(XPathContext context, PendingUpdateList pul) throws XPathException {
		evaluateItem(context);
	}

	public <EO extends Item> Item error(
            XPathContext context,
            /*@Nullable*/ QNameValue errorCode,
            /*@Nullable*/ StringValue desc,
            /*@Nullable*/ SequenceIterator<EO> errObject) throws XPathException{
		QNameValue qname = null;
		if (argument.length > 0) {
			qname = errorCode;
		}
		if (qname == null) {
			qname = new QNameValue("err", NamespaceConstant.ERR,
					(argument.length == 1 ? "FOTY0004" : "FOER0000"),
					BuiltInAtomicType.QNAME, null);
		}
		String description;
		if (argument.length > 1) {
			description = (desc == null ? "" : desc.getStringValue());
		} else {
			description = "Error signalled by application call on error()";
		}
		XPathException e = new XPathException(description);
		e.setErrorCodeQName(qname.toStructuredQName());
		e.setXPathContext(context);
		e.setLocator(this);
		if (argument.length > 2 && errObject != null) {
			Value errorObject = ((Value)SequenceExtent.makeSequenceExtent(errObject)).reduce();
			if (errorObject instanceof SingletonItem) {
				Item root = ((SingletonItem)errorObject).asItem();
				if ((root instanceof NodeInfo) && ((NodeInfo)root).getNodeKind() == Type.DOCUMENT) {
					XPathEvaluator xpath = new XPathEvaluator();
					XPathExpression exp = xpath.createExpression("/error/@module");
					NodeInfo moduleAtt = (NodeInfo)exp.evaluateSingle((NodeInfo)root);
					String module = (moduleAtt == null ? null : moduleAtt.getStringValue());
					exp = xpath.createExpression("/error/@line");
					NodeInfo lineAtt = (NodeInfo)exp.evaluateSingle((NodeInfo)root);
					int line = (lineAtt == null ? -1 : Integer.parseInt(lineAtt.getStringValue()));
					exp = xpath.createExpression("/error/@column");
					NodeInfo columnAtt = (NodeInfo)exp.evaluateSingle((NodeInfo)root);
					int column = (columnAtt == null ? -1 : Integer.parseInt(columnAtt.getStringValue()));
					ExpressionLocation locator = new ExpressionLocation();
					locator.setSystemId(module);
					locator.setLineNumber(line);
					locator.setColumnNumber(column);
					e.setLocator(locator);
				}
			}
			e.setErrorObject(errorObject);
		}
		throw e;
	}


	public SequenceIterator<? extends Item> call(SequenceIterator<? extends Item>[] arguments,
			XPathContext context) throws XPathException {
		int len =argument.length;

		switch(len){
		case 0 : return SingletonIterator.makeIterator(
                error(context, null, null, null));
		case 1 : return SingletonIterator.makeIterator(
                error(context, (QNameValue)arguments[0].next(), null, null));
		case 2:  return SingletonIterator.makeIterator(
                error(context, (QNameValue)arguments[0].next(), (StringValue)arguments[1].next(), null));
		case 3:  return SingletonIterator.makeIterator(
                error(context, (QNameValue)arguments[0].next(), (StringValue)arguments[1].next(), arguments[2]));
		default:
			return null;
		}
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