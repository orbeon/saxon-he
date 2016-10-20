////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
// Copyright (c) 2015 Saxonica Limited.
// This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
// If a copy of the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
// This Source Code Form is "Incompatible With Secondary Licenses", as defined by the Mozilla Public License, v. 2.0.
////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

package net.sf.saxon.functions;

import net.sf.saxon.Configuration;
import net.sf.saxon.event.Receiver;
import net.sf.saxon.expr.*;
import net.sf.saxon.expr.parser.ExpressionVisitor;
import net.sf.saxon.lib.FeatureKeys;
import net.sf.saxon.lib.ParseOptions;
import net.sf.saxon.om.*;
import net.sf.saxon.trans.XPathException;
import net.sf.saxon.value.AtomicValue;

/**
 * Implement the fn:doc() function - a simplified form of the Document function
 */

public class Doc extends SystemFunction implements Callable {

    private boolean readOnce = false;
    private ParseOptions parseOptions;

    /**
     * Indicate that the document will be read once only (or that it should be treated as if it
     * is read once only. This means (a) the document will not be held in memory after all references
     * to it go out of scope, and (b) if the query or transformation tries to read it again, it will get a new
     * copy, with different node identities, and potentially with different content. It also means that the
     * document is eligible for document projection.
     *
     * @param once true if this document is to be treated as being read once only
     */

    public void setReadOnce(boolean once) {
        readOnce = once;
    }

    /**
     * Ask whether this document has been marked as being read once only.
     *
     * @return true if the document has been marked as being read once only
     */

    public boolean isReadOnce() {
        return readOnce;
    }

    /**
     * Get the parsing options set via setParseOptions()
     * @return the parsing options
     */

    public ParseOptions getParseOptions() {
        return parseOptions;
    }

    /**
     * Set options to be used for the parsing operation. Defaults to the parsing options set in the Configuration
     * @param parseOptions the parsing options to be used. Currently only affects the behaviour of the sendDocument()
     * method (used in streamed merging)
     */

    public void setParseOptions(ParseOptions parseOptions) {
        this.parseOptions = parseOptions;
    }

    public int getCardinality(Expression[] arguments) {
        return arguments[0].getCardinality() & ~StaticProperty.ALLOWS_MANY;
    }

    @Override
    public Expression makeFunctionCall(Expression... arguments) {
        return maybePreEvaluate(this, arguments);
    }


    public static Expression maybePreEvaluate(final SystemFunction sf, final Expression[] arguments) {
        if (arguments.length > 1 ||
                !sf.getRetainedStaticContext().getConfiguration().getBooleanProperty(FeatureKeys.PRE_EVALUATE_DOC_FUNCTION)) {
            return new SystemFunctionCall(sf, arguments) {
                @Override
                public Expression preEvaluate(ExpressionVisitor visitor) throws XPathException {
                    // Suppress early evaluation
                    return this;
                }
            };
        } else {
            // allow early evaluation
            return new SystemFunctionCall(sf, arguments) {
                @Override
                public Expression preEvaluate(ExpressionVisitor visitor) throws XPathException {
                    Configuration config = visitor.getConfiguration();
                    try {
                        GroundedValue firstArg = ((Literal)getArg(0)).getValue();
                        if (firstArg.getLength() == 0) {
                            return null;
                        } else if (firstArg.getLength() > 1) {
                            return this;
                        }
                        String href = firstArg.head().getStringValue();
                        if (href.indexOf('#') >= 0) {
                            return this;
                        }
                        NodeInfo item = DocumentFn.preLoadDoc(href, sf.getStaticBaseUriString(), config, getLocation());
                        if (item != null) {
                            return Literal.makeLiteral(SequenceTool.toGroundedValue(item));
                        }
                    } catch (Exception err) {
                        // ignore the exception and try again at run-time
                        return this;
                    }
                    return this;
                }
            };
        }
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
    public ZeroOrOne<NodeInfo> call(XPathContext context, Sequence[] arguments) throws XPathException {
        AtomicValue hrefVal = (AtomicValue) arguments[0].head();
        if (hrefVal == null) {
            return ZeroOrOne.empty();
        }
        String href = hrefVal.getStringValue();
        PackageData packageData = getRetainedStaticContext().getPackageData();
        NodeInfo item = DocumentFn.makeDoc(href, getRetainedStaticContext().getStaticBaseUriString(), packageData, null, context, null, false);
        if (item == null) {
            // we failed to read the document
            throw new XPathException("Failed to load document " + href, "FODC0002", context);
        }
        return new ZeroOrOne<NodeInfo>(item);
    }

    /**
     * Get the static properties of this expression (other than its type). The result is
     * bit-signficant. These properties are used for optimizations. In general, if
     * property bit is set, it is true, but if it is unset, the value is unknown.
     * @param arguments
     */

    public int getSpecialProperties(Expression[] arguments) {
        return StaticProperty.ORDERED_NODESET |
                StaticProperty.PEER_NODESET |
                StaticProperty.NON_CREATIVE |
                StaticProperty.SINGLE_DOCUMENT_NODESET;
        // Declaring it as a peer node-set expression avoids sorting of expressions such as
        // doc(XXX)/a/b/c
        // The doc() function might appear to be creative: but it isn't, because multiple calls
        // with the same arguments will produce identical results.
    }

    /**
     * Copy the document identified by this expression to a given Receiver. This method is used only when it is
     * known that the document is being copied, because there is then no problem about node identity.
     *
     * @param context the XPath dynamic context
     * @param args the arguments to the doc function
     * @param out     the destination to which the document will be sent
     * @throws XPathException if a failure occurs
     */

    public void sendDocument(XPathContext context, Expression[] args, Receiver out) throws XPathException {
        AtomicValue hrefVal = (AtomicValue) args[0].evaluateItem(context);
        if (hrefVal == null) {
            return;
        }
        String href = hrefVal.getStringValue();
        ParseOptions options = parseOptions;
        if (options == null) {
            options = context.getConfiguration().getParseOptions();
        }
        try {
            DocumentFn.sendDoc(href, getStaticBaseUriString(), context, null, out, options);
        } catch (XPathException e) {
            if (e.getErrorCodeQName() == null) {
                e.setErrorCode("FODC0002");
            }
            throw e;
        }
    }

}

