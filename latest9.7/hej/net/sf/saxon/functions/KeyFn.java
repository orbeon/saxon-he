////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
// Copyright (c) 2015 Saxonica Limited.
// This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
// If a copy of the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
// This Source Code Form is "Incompatible With Secondary Licenses", as defined by the Mozilla Public License, v. 2.0.
////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

package net.sf.saxon.functions;

import net.sf.saxon.expr.*;
import net.sf.saxon.expr.parser.RetainedStaticContext;
import net.sf.saxon.expr.sort.DocumentOrderIterator;
import net.sf.saxon.expr.sort.LocalOrderComparer;
import net.sf.saxon.om.*;
import net.sf.saxon.trace.ExpressionPresenter;
import net.sf.saxon.trans.KeyDefinitionSet;
import net.sf.saxon.trans.KeyManager;
import net.sf.saxon.trans.XPathException;
import net.sf.saxon.tree.util.Navigator;
import net.sf.saxon.type.Type;
import net.sf.saxon.value.AtomicValue;
import net.sf.saxon.value.EmptySequence;


public class KeyFn extends SystemFunction {

    private KeyDefinitionSet staticKeySet = null; // null if name resolution is done at run-time
    private transient boolean checked = false;
    private transient boolean internal = false;
    private boolean is30 = false;
    // the second time checkArguments is called, it's a global check so the static context is inaccurate

    /**
     * Get the key name, if known statically. If not known statically, return null.
     *
     * @return the key name if known, otherwise null
     */

    public StructuredQName getStaticKeyName() {
        return staticKeySet == null ? null : staticKeySet.getKeyName();
    }

    public KeyDefinitionSet getStaticKeySet() {
        return staticKeySet;
    }

    public KeyManager getKeyManager() {
        return getRetainedStaticContext().getPackageData().getKeyManager();
    }

    public boolean getInternal() {
        return internal;
    }


    public NamespaceResolver getNamespaceResolver() {
        return getRetainedStaticContext();
    }

    /**
     * Factory method to create an internal call on key() with a known key definition
     *
     * @param keySet the set of KeyDefinitions (always a single KeyDefinition)
     * @param name   the name allocated to the key (first argument of the function)
     * @param value  the value being searched for (second argument of the function)
     * @param doc    the document being searched (third argument)
     * @return a call on the key() function
     */

    public static Expression internalKeyCall(KeyManager keyManager, KeyDefinitionSet keySet,
                                             String name, Expression value, Expression doc,
                                             RetainedStaticContext rsc) {
        KeyFn k = new KeyFn();
        k.setDetails(StandardFunction.getFunction("key", 3));
        k.setArity(3);
        k.staticKeySet = keySet;
        k.checked = true;
        k.internal = true;
        k.is30 = true;
        k.setRetainedStaticContext(rsc);
        return k.makeFunctionCall(new StringLiteral(name), value, doc);
    }

    /**
     * Get the static properties of this expression (other than its type). The result is
     * bit-signficant. These properties are used for optimizations. In general, if
     * a property bit is set, it is true, but if it is unset, the value is unknown.
     * @param arguments
     */

    public int getSpecialProperties(Expression[] arguments) {
        int prop = StaticProperty.ORDERED_NODESET |
                StaticProperty.SINGLE_DOCUMENT_NODESET |
                StaticProperty.NON_CREATIVE;
        if ((getArity() == 2) ||
                (arguments[2].getSpecialProperties() & StaticProperty.CONTEXT_DOCUMENT_NODESET) != 0) {
            prop |= StaticProperty.CONTEXT_DOCUMENT_NODESET;
        }
        return prop;
    }

    /**
     * Diagnostic print of expression structure. The abstract expression tree
     * is written to the supplied output destination.
     *
     * @param out
     */
    @Override
    public void export(ExpressionPresenter out) throws XPathException {
        super.export(out);
    }


    /**
     * Mapping class to filter nodes that have the origin node as an ancestor-or-self
     */

    public static class SubtreeFilter implements ItemMappingFunction<NodeInfo, NodeInfo> {

        private NodeInfo origin;

        public SubtreeFilter(NodeInfo origin) {
            this.origin = origin;
        }

        public NodeInfo mapItem(NodeInfo item) throws XPathException {
            if (Navigator.isAncestorOrSelf(origin, item)) {
                return item;
            } else {
                return null;
            }
        }

    }

    /**
     * Evaluate the expression
     *
     * @param context   the dynamic evaluation context
     * @param arguments the values of the arguments, supplied as Sequences
     * @return the result of the evaluation, in the form of a Sequence
     * @throws net.sf.saxon.trans.XPathException
     *          if a dynamic error occurs during the evaluation of the expression
     */
    public Sequence call(XPathContext context, Sequence[] arguments) throws XPathException {
        NodeInfo origin;
        NodeInfo root;
        if (arguments.length == 3) {
            Item arg2;
            try {
                arg2 = arguments[2].head();
            } catch (XPathException e) {
                String code = e.getErrorCodeLocalPart();
                if ("XPDY0002".equals(code) && arguments[2] instanceof RootExpression) {
                    throw new XPathException("Cannot call the key() function when there is no context node", "XTDE1270", context);
                } else if ("XPDY0050".equals(code)) {
                    throw new XPathException("In the key() function," +
                        " the node supplied in the third argument (or the context node if absent)" +
                        " must be in a tree whose root is a document node", "XTDE1270", context);
                } else if ("XPTY0020".equals(code) || "XPTY0019".equals(code)) {
                    throw new XPathException("Cannot call the key() function when the context item is an atomic value",
                        "XTDE1270", context);
                }
                throw e;
            }

            origin = (NodeInfo) arg2;
            root = origin.getRoot();
        } else {
            Item contextItem = context.getContextItem();
            if (contextItem == null) {
                throw new XPathException("Cannot call the key() function when there is no context item", "XTDE1270", context);
            } else if (!(contextItem instanceof NodeInfo)) {
                throw new XPathException("Cannot call the key() function when the context item is not a node", "XTDE1270", context);
            }
            root = ((NodeInfo)contextItem).getRoot();
            origin = root;
        }
        if (root.getNodeKind() != Type.DOCUMENT) {
            throw new XPathException("In the key() function," +
                " the node supplied in the third argument (or the context node if absent)" +
                " must be in a tree whose root is a document node", "XTDE1270", context);
        }
        NodeInfo doc = root;

        KeyDefinitionSet selectedKeySet = staticKeySet;
        if (selectedKeySet == null) {
            String givenkeyname = arguments[0].head().getStringValue();
            StructuredQName qName = null;
            try {
                qName = StructuredQName.fromLexicalQName(
                    givenkeyname, false, true,
                    getNamespaceResolver());
            } catch (XPathException err) {
                throw new XPathException("Invalid key name: " + err.getMessage(), "XTDE1260", context);
            }
            selectedKeySet = getKeyManager().getKeyDefinitionSet(qName);
            if (selectedKeySet == null) {
                throw new XPathException("Key '" + givenkeyname + "' has not been defined", "XTDE1260", context);
            }
        }

//        if (internal) {
//            System.err.println("Using key " + fprint + " on doc " + doc);
//        }

        if (selectedKeySet.isComposite()) {
            SequenceIterator soughtKey = arguments[1].iterate();
            return new LazySequence(getKeyManager().selectByCompositeKey(selectedKeySet, doc.getTreeInfo(), soughtKey, context));

        } else {
            // If the second argument is a singleton, we evaluate the function
            // directly; otherwise we recurse to evaluate it once for each Item
            // in the sequence.

            SequenceIterator allResults;
            if (!(arguments[1] instanceof GroundedValue) || ((GroundedValue)arguments[1]).getLength() > 1) {
                final XPathContext keyContext = context;
                final TreeInfo document = doc.getTreeInfo();
                final KeyManager keyManager = getKeyManager();
                final KeyDefinitionSet keySet = selectedKeySet;
                MappingFunction map = new MappingFunction<AtomicValue, NodeInfo>() {
                    // Map a value to the sequence of nodes having that value as a key value
                    public SequenceIterator map(AtomicValue item) throws XPathException {
                        return keyManager.selectByKey(
                            keySet, document, item, keyContext);
                    }
                };

                SequenceIterator keys = arguments[1].iterate();
                SequenceIterator allValues = new MappingIterator(keys, map);
                allResults = new DocumentOrderIterator(allValues, LocalOrderComparer.getInstance());
            } else {
                try {
                    AtomicValue keyValue = (AtomicValue) arguments[1].head();
                    if (keyValue == null) {
                        return EmptySequence.getInstance();
                    }
                    allResults = getKeyManager().selectByKey(selectedKeySet, doc.getTreeInfo(), keyValue, context);
                } catch (XPathException e) {
                    //e.maybesetLocation(getLocation());
                    throw e;
                }
            }
            if (origin.isSameNodeInfo(root)) {
                return new LazySequence(allResults);
            }
            return new LazySequence(new ItemMappingIterator(allResults, new SubtreeFilter(origin)));
        }

    }

}


