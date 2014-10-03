////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
// Copyright (c) 2014 Saxonica Limited.
// This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
// If a copy of the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
// This Source Code Form is "Incompatible With Secondary Licenses", as defined by the Mozilla Public License, v. 2.0.
////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

package net.sf.saxon.trans;

import net.sf.saxon.pattern.PathFinder;
import net.sf.saxon.Configuration;
import net.sf.saxon.Controller;
import net.sf.saxon.expr.*;
import net.sf.saxon.expr.instruct.SlotManager;
import net.sf.saxon.expr.sort.AtomicMatchKey;
import net.sf.saxon.expr.sort.LocalOrderComparer;
import net.sf.saxon.expr.sort.CodepointMatchKey;
import net.sf.saxon.functions.StringFn;
import net.sf.saxon.functions.SystemFunctionCall;
import net.sf.saxon.functions.Tokenize;
import net.sf.saxon.lib.ConversionRules;
import net.sf.saxon.lib.StringCollator;
import net.sf.saxon.om.*;
import net.sf.saxon.pattern.IdrefTest;
import net.sf.saxon.pattern.PatternFinder;
import net.sf.saxon.trace.ExpressionPresenter;
import net.sf.saxon.tree.iter.*;
import net.sf.saxon.tree.iter.ListIterator;
import net.sf.saxon.type.*;
import net.sf.saxon.value.AtomicValue;
import net.sf.saxon.value.DoubleValue;
import net.sf.saxon.value.NumericValue;
import net.sf.saxon.value.UntypedAtomicValue;

import java.lang.ref.WeakReference;
import java.util.*;

/**
 * KeyManager manages the set of key definitions in a stylesheet, and the indexes
 * associated with these key definitions. It handles xsl:sort-key as well as xsl:key
 * definitions.
 * <p/>
 * <p>The memory management in this class is subtle, with extensive use of weak references.
 * The idea is that an index should continue to exist in memory so long as both the compiled
 * stylesheet and the source document exist in memory: if either is removed, the index should
 * go too. The document itself holds no reference to the index. The compiled stylesheet (which
 * owns the KeyManager) holds a weak reference to the index. The index, of course, holds strong
 * references to the nodes in the document. The Controller holds a strong reference to the
 * list of indexes used for each document, so that indexes remain in memory for the duration
 * of a transformation even if the documents themselves are garbage collected.</p>
 * <p/>
 * <p>Potentially there is a need for more than one index for a given key name, depending
 * on the primitive type of the value provided to the key() function. An index is built
 * corresponding to the type of the requested value; if subsequently the key() function is
 * called with the same name and a different type of value, then a new index is built.</p>
 * <p/>
 * <p>For XSLT-defined keys, equality matching follows the rules of the eq operator, which means
 * that untypedAtomic values are treated as strings. In backwards compatibility mode, <i>all</i>
 * values are converted to strings.</p>
 * <p/>
 * <p>This class is also used for internal indexes constructed (a) to support the idref() function,
 * and (b) (in Saxon-EE only) to support filter expressions of the form /a/b/c[d=e], where the
 * path expression being filtered must be a single-document context-free path rooted at a document node,
 * where exactly one of d and e must be dependent on the focus, and where certain other conditions apply
 * such as the filter predicate not being positional. The operator in this case may be either "=" or "eq".
 * If it is "eq", then the semantics are very similar to xsl:key indexes, except that use of non-comparable
 * types gives an error rather than a non-match. If the operator is "=", however, then the rules for
 * handling untypedAtomic values are different: these must be converted to the type of the other operand.
 * In this situation the following rules apply. Assume that the predicate is [use=value], where use is
 * dependent on the focus (the indexed value), and value is the sought value.</p>
 * <p/>
 * <ul>
 * <li>If value is a type other than untypedAtomic, say T, then we build an index for type T, in which any
 * untypedAtomic values that arise in evaluating "use" are converted to type T. A conversion failure results
 * in an error. A value of a type that is not comparable to T also results in an error.</li>
 * <li>If value is untypedAtomic, then we build an index for every type actually encountered in evaluating
 * the use expression (treating untypedAtomic as string), and then search each of these indexes. (Note that
 * it is not an error if the use expression returns a mixture of say numbers and dates, provided that the
 * sought value is untypedAtomic).</li>
 * </ul>
 *
 * @author Michael H. Kay
 */

public class KeyManager {

    // a dummy map, always empty, to act as a marker
    private final static Map<AtomicMatchKey, List<NodeInfo>> underConstruction = new HashMap<AtomicMatchKey, List<NodeInfo>>();

    private HashMap<StructuredQName, KeyDefinitionSet> keyMap;
    // one entry for each named key; the entry contains
    // a KeyDefinitionSet holding the key definitions with that name
    private transient WeakHashMap<DocumentInfo, WeakReference<HashMap<Long, Map<AtomicMatchKey, List<NodeInfo>>>>> docIndexes;
    // one entry for each document that is in memory;
    // the entry contains a HashMap mapping the fingerprint of
    // the key name plus the primitive item type
    // to the HashMap that is the actual index
    // of key/value pairs.

    /**
     * Create a KeyManager and initialise variables
     *
     * @param config the Saxon configuration
     */

    public KeyManager(Configuration config) {
        keyMap = new HashMap<StructuredQName, KeyDefinitionSet>(10);
        docIndexes = new WeakHashMap<DocumentInfo, WeakReference<HashMap<Long, Map<AtomicMatchKey, List<NodeInfo>>>>>(10);
        // Create a key definition for the idref() function
        registerIdrefKey(config);
    }

    /**
     * An internal key definition is used to support the idref() function. The key definition
     * is equivalent to xsl:key match="element(*, xs:IDREF) | element(*, IDREFS) |
     * attribute(*, xs:IDREF) | attribute(*, IDREFS)" use="tokenize(string(.))". This method creates this
     * key definition.
     *
     * @param config The configuration. This is needed because the patterns that are
     *               generated need access to schema information.
     */

    private void registerIdrefKey(Configuration config) {
        PatternFinder idref = IdrefTest.getInstance();
        StringFn sf = (StringFn) SystemFunctionCall.makeSystemFunction(
                "string", new Expression[]{new ContextItemExpression()});
        StringLiteral regex = new StringLiteral("\\s+", null);
        Tokenize use = (Tokenize) SystemFunctionCall.makeSystemFunction("tokenize", new Expression[]{sf, regex});
        KeyDefinition key = new KeyDefinition(idref, use, null, null);
        key.setPackageData(null); // TODO: FIXME
        key.setIndexedItemType(BuiltInAtomicType.STRING);
        try {
            addKeyDefinition(StandardNames.getStructuredQName(StandardNames.XS_IDREFS), key, true, config);
        } catch (XPathException err) {
            throw new AssertionError(err); // shouldn't happen
        }
    }

    /**
     * Pre-register a key definition. This simply registers that a key with a given name exists,
     * without providing any details.
     *
     * @param keyName the name of the key to be pre-registered
     */

    public void preRegisterKeyDefinition(StructuredQName keyName) {
        KeyDefinitionSet keySet = keyMap.get(keyName);
        if (keySet == null) {
            keySet = new KeyDefinitionSet(keyName, keyMap.size());
            keyMap.put(keyName, keySet);
        }
    }

    /**
     * Register a key definition. Note that multiple key definitions with the same name are
     * allowed
     *
     *
     * @param keyName Structured QName representing the name of the key
     * @param keydef  The details of the key's definition
     * @param reusable Set to true if indexes using this key definition can be used across multiple transformations, false if
     * the indexes need to be rebuilt for each transformation. Indexes are not reusable if the key definition contains references
     * to global variables or parameters, or calls used-defined functions or templates that might contain such references.
     * @param config  The configuration
     * @throws XPathException if this key definition is inconsistent with existing key definitions having the same name
     */

    public void addKeyDefinition(StructuredQName keyName, KeyDefinition keydef, boolean reusable, Configuration config) throws XPathException {
        KeyDefinitionSet keySet = keyMap.get(keyName);
        if (keySet == null) {
            keySet = new KeyDefinitionSet(keyName, keyMap.size());
            keyMap.put(keyName, keySet);
        }
        keySet.addKeyDefinition(keydef);

        if (!reusable) {
            keySet.setReusable(false);
        }

        boolean backwardsCompatible = keySet.isBackwardsCompatible();

        if (backwardsCompatible) {
            // In backwards compatibility mode, convert all the use-expression results to sequences of strings
            List<KeyDefinition> v = keySet.getKeyDefinitions();
            for (KeyDefinition kd : v) {
                kd.setBackwardsCompatible(true);
                if (!kd.getBody().getItemType().equals(BuiltInAtomicType.STRING)) {
                    Expression exp = new AtomicSequenceConverter(kd.getBody(), BuiltInAtomicType.STRING);
                    ((AtomicSequenceConverter) exp).allocateConverter(config, false);
                    kd.setBody(exp);
                }
            }
        }

    }

    /**
     * Get all the key definitions that match a particular name
     *
     * @param qName The name of the required key
     * @return The set of key definitions of the named key if there are any, or null otherwise.
     */

    public KeyDefinitionSet getKeyDefinitionSet(StructuredQName qName) {
        return keyMap.get(qName);
    }

    /**
     * Look for a key definition that matches a proposed new key
     * @param finder matches/selects the nodes to be indexed
     * @param use computes the value on which the nodes are indexed
     * @param collationName collation to be used
     * @return a KeyDefinitionSet containing a key with the required characteristics if there
     * is one, or null otherwise
     */

    public KeyDefinitionSet findKeyDefinition(PathFinder finder, Expression use, String collationName) {
        for (KeyDefinitionSet keySet : keyMap.values()) {
            if (keySet.keyDefinitions.size() == 1) {
                for (KeyDefinition keyDef : keySet.getKeyDefinitions()) {
                    if (keyDef.getMatch().equals(finder) &&
                            keyDef.getUse().equals(use) &&
                            keyDef.getCollationName().equals(collationName)) {
                        return keySet;
                    }
                }
            }
        }
        return null;
    }

    /**
     * Build the index for a particular document for a named key
     *
     * @param keySet         The set of key definitions with this name
     * @param itemType       the type of the values to be indexed.
     * @param foundItemTypes Optional (may be null). If supplied, a set that is to be populated with
     *                       the set of primitive types actually found among the "use" values.
     * @param doc            The source document in question
     * @param context        The dynamic context
     * @return the index in question, as a Map mapping a key value onto a List of nodes
     * @throws XPathException if a dynamic error is encountered
     */

    private synchronized Map<AtomicMatchKey, List<NodeInfo>> buildIndex(KeyDefinitionSet keySet,
                                                                        BuiltInAtomicType itemType,
                                                                        Set<BuiltInAtomicType> foundItemTypes,
                                                                        DocumentInfo doc,
                                                                        XPathContext context) throws XPathException {

        List<KeyDefinition> definitions = keySet.getKeyDefinitions();
        Map<AtomicMatchKey, List<NodeInfo>> index =
                keySet.isRangeKey() ?
                        new TreeMap<AtomicMatchKey, List<NodeInfo>>() :
                        new HashMap<AtomicMatchKey, List<NodeInfo>>(100);

        // There may be multiple xsl:key definitions with the same name. Index them all.
        for (int k = 0; k < definitions.size(); k++) {
            constructIndex(doc, index, definitions.get(k), itemType, foundItemTypes, context, k == 0);
        }

        return index;

    }

    /**
     * Process one key definition to add entries to an index
     *
     * @param doc            the document to be indexed
     * @param index          the index to be built
     * @param keydef         the key definition used to build the index
     * @param soughtItemType the primitive type of the value that the user is searching for on the call
     *                       to the key() function that triggered this index to be built
     * @param foundItemTypes Optional (may be null): if supplied, a Set to be populated with the set of
     *                       primitive types actually found for the use expression
     * @param context        the XPath dynamic evaluation context
     * @param isFirst        true if this is the first index to be built for this key
     * @throws XPathException if a dynamic error is encountered
     */

    private void constructIndex(DocumentInfo doc,
                                Map<AtomicMatchKey, List<NodeInfo>> index,
                                KeyDefinition keydef,
                                BuiltInAtomicType soughtItemType,
                                Set<BuiltInAtomicType> foundItemTypes,
                                XPathContext context,
                                boolean isFirst) throws XPathException {
        //System.err.println("build index for doc " + doc.getDocumentNumber());
        PatternFinder match = keydef.getMatch();

        //NodeInfo curr;
        XPathContextMajor xc = context.newContext();
        xc.setOrigin(keydef);
        xc.setCurrentComponent(keydef.getDeclaringComponent());
        xc.setTemporaryOutputState(StandardNames.XSL_KEY);

        // The use expression (or sequence constructor) may contain local variables.
        SlotManager map = keydef.getStackFrameMap();
        if (map != null) {
            xc.openStackFrame(map);
        }

        SequenceIterator iter = match.selectNodes(doc, xc);
        Item item;
        while ((item = iter.next()) != null) {
            processKeyNode((NodeInfo) item, soughtItemType, foundItemTypes, keydef, index, xc, isFirst);
        }
    }

    /**
     * Process one matching node, adding entries to the index if appropriate
     *
     * @param curr           the node being processed
     * @param soughtItemType the primitive item type of the argument to the key() function that triggered
     *                       this index to be built
     * @param foundItemTypes Optional (may be null): if supplied, a Set to be populated with the set of
     *                       primitive types actually found for the use expression
     * @param keydef         the key definition
     * @param index          the index being constructed
     * @param xc             the context for evaluating expressions
     * @param isFirst        indicates whether this is the first key definition with a given key name (which means
     *                       no sort of the resulting key entries is required)
     * @throws XPathException if a dynamic error is encountered
     */

    private void processKeyNode(NodeInfo curr,
                                BuiltInAtomicType soughtItemType,
                                Set<BuiltInAtomicType> foundItemTypes,
                                KeyDefinition keydef,
                                Map<AtomicMatchKey, List<NodeInfo>> index,
                                XPathContext xc,
                                boolean isFirst) throws XPathException {


        // Make the node we are testing the context node,
        // with context position and context size set to 1

        ManualIterator si = new ManualIterator(curr);
        xc.setCurrentIterator(si);

        StringCollator collation = keydef.getCollation();
        final ConversionRules rules = xc.getConfiguration().getConversionRules();
        int implicitTimezone = xc.getImplicitTimezone();

        // Evaluate the "use" expression against this context node

        Expression use = keydef.getUse();
        SequenceIterator useval = use.iterate(xc);
        while (true) {
            AtomicValue item = (AtomicValue) useval.next();
            if (item == null) {
                break;
            }
            BuiltInAtomicType actualItemType = item.getPrimitiveType();
            if (foundItemTypes != null) {
                foundItemTypes.add(actualItemType);
            }
            if (!Type.isGuaranteedComparable(actualItemType, soughtItemType, false)) {
                // the types aren't comparable
                if (keydef.isStrictComparison()) {
                    XPathException de = new XPathException("Cannot compare " + soughtItemType +
                            " to " + actualItemType + " using 'eq'");
                    de.setErrorCode("XPTY0004");
                    throw de;
                } else if (keydef.isConvertUntypedToOther() &&
                        actualItemType.equals(BuiltInAtomicType.UNTYPED_ATOMIC)) {
                    item = Converter.convert(item, soughtItemType, rules).asAtomic();
                } else if (keydef.isConvertUntypedToOther() &&
                        soughtItemType.equals(BuiltInAtomicType.UNTYPED_ATOMIC)) {
                    // index the item as is
                } else {
                    // simply ignore this key value
                    continue;
                }
            }
            AtomicMatchKey val;

            if (soughtItemType.equals(BuiltInAtomicType.UNTYPED_ATOMIC) ||
                    soughtItemType.equals(BuiltInAtomicType.STRING) ||
                    soughtItemType.equals(BuiltInAtomicType.ANY_URI)) {
                // If the supplied key value is untyped atomic, we build an index using the
                // actual type returned by the use expression
                // If the supplied key value is a string, there is no match unless the use expression
                // returns a string or an untyped atomic value
                if (collation == null) {
                    val = new CodepointMatchKey(item.getStringValue());
                } else {
                    val = collation.getCollationKey(item.getStringValue());
                }
            } else {
                // Ignore NaN values
                if (item.isNaN()) {
                    break;
                }
                try {
                    AtomicValue av = Converter.convert(item, soughtItemType, rules).asAtomic();
                    val = av.getXPathComparable(false, collation, implicitTimezone);
                } catch (XPathException err) {
                    // ignore values that can't be converted to the required type
                    break;
                }
            }

            List<NodeInfo> nodes = index.get(val);
            if (nodes == null) {
                // this is the first node with this key value
                nodes = new ArrayList<NodeInfo>(4);
                index.put(val, nodes);
                nodes.add(curr);
            } else {
                // this is not the first node with this key value.
                // add the node to the list of nodes for this key,
                // unless it's already there
                if (isFirst) {
                    // if this is the first index definition that we're processing,
                    // then this node must be after all existing nodes in document
                    // order, or the same node as the last existing node
                    if (nodes.get(nodes.size() - 1) != curr) {
                        nodes.add(curr);
                    }
                } else {
                    // otherwise, we need to insert the node at the correct
                    // position in document order. This code does an insertion sort:
                    // not ideal for performance, but it's very unusual to have more than
                    // one key definition for a key. We start looking at the end because
                    // it's most likely that the new node will come after all the others.
                    // See bug 2092 in saxonica.plan.io
                    LocalOrderComparer comparer = LocalOrderComparer.getInstance();
                    boolean found = false;
                    for (int i=nodes.size()-1; i>=0; i--) {
                        int d = comparer.compare(curr, nodes.get(i));
                        if (d>=0) {
                            if (d==0) {
                                // node already in list; do nothing
                            } else {
                                // add the node at this position
                                nodes.add(i+1, curr);
                            }
                            found = true;
                            break;
                        }
                        // else continue round the loop
                    }
                    // if we're still here, add the new node at the start
                    if (!found) {
                        nodes.add(0, curr);
                    }
                }
            }
        }

    }

    /**
     * Get the nodes with a given key value
     *
     * @param keySet      The set of key definitions identified by the key name used in the call to the key() function
     * @param doc         The source document in question
     * @param soughtValue The required key value
     * @param context     The dynamic context, needed only the first time when the key is being built
     * @return an iteration of the selected nodes, always in document order with no duplicates
     * @throws XPathException if a dynamic error is encountered
     */

    public SequenceIterator selectByKey(
            KeyDefinitionSet keySet,
            DocumentInfo doc,
            AtomicValue soughtValue,
            XPathContext context) throws XPathException {

        //System.err.println("*********** USING KEY ************");
        if (soughtValue == null) {
            return EmptyIterator.OfNodes.THE_INSTANCE;
        }
        List<KeyDefinition> definitions = keySet.getKeyDefinitions();
        KeyDefinition definition = definitions.get(0);
        // the itemType and collation and BC mode will be the same for all keys with the same name
        StringCollator collation = definition.getCollation();

        if (keySet.isBackwardsCompatible()) {
            // if backwards compatibility is in force, treat all values as strings
            final ConversionRules rules = context.getConfiguration().getConversionRules();
            soughtValue = Converter.convert(soughtValue, BuiltInAtomicType.STRING, rules).asAtomic();
        } else {
            // If the key value is numeric, promote it to a double
            // Note: this could result in two decimals comparing equal because they convert to the same double

            BuiltInAtomicType itemType = soughtValue.getPrimitiveType();
            if (itemType.equals(BuiltInAtomicType.INTEGER) ||
                    itemType.equals(BuiltInAtomicType.DECIMAL) ||
                    itemType.equals(BuiltInAtomicType.FLOAT)) {
                soughtValue = new DoubleValue(((NumericValue) soughtValue).getDoubleValue());
            }
        }

        // If the sought value is untypedAtomic and the equality matching mode is
        // "convertUntypedToOther", then we construct and search one index for each
        // primitive atomic type that could occur in the result of the "use" expression,
        // and merge the results. We rely on the fact that in this case, there will only
        // be one key definition.

        // NOTE: This is much more elaborate than it needs to be. The option convertUntypedToOther
        // is used for an index used to support a general comparison. This reports an error if two
        // non-comparable values are compared. We could report an error immediately if foundItemTypes
        // includes a type that is not comparable to the soughtValue. In practice we only need a maximum
        // of two indexes: one for the sought item type, and one for untypedAtomic.

        HashSet<BuiltInAtomicType> foundItemTypes = null;
        AtomicValue value = soughtValue;
        if (soughtValue instanceof UntypedAtomicValue && definition.isConvertUntypedToOther()) {
            // We try string first, but at the same time as building an index for strings,
            // we collect details of the other types actually encountered for the use expression
            BuiltInAtomicType useType = definition.getIndexedItemType();
            if (useType.equals(BuiltInAtomicType.ANY_ATOMIC)) {
                foundItemTypes = new HashSet<BuiltInAtomicType>(10);
                useType = BuiltInAtomicType.STRING;
            }
            final ConversionRules rules = context.getConfiguration().getConversionRules();
            value = Converter.convert(soughtValue, useType, rules).asAtomic();
        }

        // No special action needed for anyURI to string promotion (it just seems to work: tests idky44, 45)

        int keySetNumber = keySet.getKeySetNumber();
        BuiltInAtomicType itemType = value.getPrimitiveType();
        Map<AtomicMatchKey, List<NodeInfo>> index = obtainIndex(keySet, doc, context, foundItemTypes, itemType);

        if (foundItemTypes == null) {
            List<NodeInfo> nodes = index.get(getCollationKey(value, itemType, collation, context));
            if (nodes == null) {
                return EmptyIterator.emptyIterator();
            } else {
                return new ListIterator(nodes);
            }
        } else {
            // we need to search the indexes for all possible types, and combine the results.
            SequenceIterator result = null;
            WeakReference<HashMap<Long, Map<AtomicMatchKey, List<NodeInfo>>>> ref = docIndexes.get(doc);
            if (ref != null) {
                HashMap<Long, Map<AtomicMatchKey, List<NodeInfo>>> indexList = ref.get();
                if (indexList != null) {
                    for (long key : indexList.keySet()) {
                        if ((key >> 32) == keySetNumber) {
                            int typefp = (int) key;

                            BuiltInAtomicType type = (BuiltInAtomicType) BuiltInType.getSchemaType(typefp);

                            Map<AtomicMatchKey, List<NodeInfo>> index2 = getSharedIndex(doc, keySetNumber, type);
                            if (index2 == underConstruction) {
                                // index is under construction
                                XPathException de = new XPathException("Key definition is circular");
                                de.setXPathContext(context);
                                de.setErrorCode("XTDE0640");
                                throw de;
                            }
                            // NOTE: we've been known to encounter a null index2 here, but it doesn't seem possible
                            if (!index2.isEmpty()) {
                                final ConversionRules rules = context.getConfiguration().getConversionRules();
                                value = Converter.convert(soughtValue, type, rules).asAtomic();
                                List<NodeInfo> nodes = index2.get(getCollationKey(value, type, collation, context));
                                if (nodes != null) {
                                    if (result == null) {
                                        result = new ListIterator(nodes);
                                    } else {
                                        result = new UnionEnumeration(result, new ListIterator(nodes), LocalOrderComparer.getInstance());
                                    }
                                }
                            }
                        }
                    }
                }
            }
            if (result == null) {
                return EmptyIterator.emptyIterator();
            } else {
                return result;
            }
        }
    }

    public Map<AtomicMatchKey, List<NodeInfo>> obtainIndex(
                            KeyDefinitionSet keySet,
                            DocumentInfo doc,
                            XPathContext context,
                            HashSet<BuiltInAtomicType> foundItemTypes,
                            BuiltInAtomicType itemType) throws XPathException {
        if (keySet.isReusable()) {
            return obtainSharedIndex(keySet, doc, context, foundItemTypes, itemType);
        } else {
            return obtainLocalIndex(keySet, doc, context, foundItemTypes, itemType);
        }
    }


    private Map<AtomicMatchKey, List<NodeInfo>> obtainSharedIndex(
                            KeyDefinitionSet keySet,
                            DocumentInfo doc,
                            XPathContext context,
                            HashSet<BuiltInAtomicType> foundItemTypes,
                            BuiltInAtomicType itemType) throws XPathException {
        Map<AtomicMatchKey, List<NodeInfo>> index;
        int keySetNumber = keySet.getKeySetNumber();
        //noinspection SynchronizationOnLocalVariableOrMethodParameter
        synchronized (doc) {
            // Need to synchronize to prevent two threads that use the same stylesheet indexing the same source
            // document simultaneously. We could synchronize on either the key definition or the document
            // (ideally we would use the combination of the two), but the document is less likely to cause
            // unnecessary contention: it's more likely that an index definition applies to large numbers of
            // documents than that a document has large numbers of indexes.
            index = getSharedIndex(doc, keySetNumber, itemType);
            if (index == underConstruction) {
                // index is under construction
                XPathException de = new XPathException("Key definition is circular");
                de.setXPathContext(context);
                de.setErrorCode("XTDE0640");
                throw de;
            }

            // If the index does not yet exist, then create it.
            if (index == null) {
                // Mark the index as being under construction, in case the definition is circular
                putSharedIndex(doc, keySetNumber, itemType, underConstruction, context);
                index = buildIndex(keySet, itemType, foundItemTypes, doc, context);
                putSharedIndex(doc, keySetNumber, itemType, index, context);
                if (foundItemTypes != null) {
                    // build indexes for each item type actually found
                    for (BuiltInAtomicType t : foundItemTypes) {
                        if (!t.equals(BuiltInAtomicType.STRING)) {
                            putSharedIndex(doc, keySetNumber, t, underConstruction, context);
                            //noinspection NullableProblems
                            index = buildIndex(keySet, t, null, doc, context);
                            putSharedIndex(doc, keySetNumber, t, index, context);
                        }
                    }
                }
            }
        }
        return index;
    }

    private Map<AtomicMatchKey, List<NodeInfo>> obtainLocalIndex(
                            KeyDefinitionSet keySet,
                            DocumentInfo doc,
                            XPathContext context,
                            HashSet<BuiltInAtomicType> foundItemTypes,
                            BuiltInAtomicType itemType) throws XPathException {
        Map<AtomicMatchKey, List<NodeInfo>> index;
        int keySetNumber = keySet.getKeySetNumber();
        //noinspection SynchronizationOnLocalVariableOrMethodParameter
        synchronized (doc) {
            // Need to synchronize to prevent two threads that use the same stylesheet indexing the same source
            // document simultaneously. We could synchronize on either the key definition or the document
            // (ideally we would use the combination of the two), but the document is less likely to cause
            // unnecessary contention: it's more likely that an index definition applies to large numbers of
            // documents than that a document has large numbers of indexes.
            index = getLocalIndex(doc, keySetNumber, itemType, context);
            if (index == underConstruction) {
                // index is under construction
                XPathException de = new XPathException("Key definition is circular");
                de.setXPathContext(context);
                de.setErrorCode("XTDE0640");
                throw de;
            }

            // If the index does not yet exist, then create it.
            if (index == null) {
                // Mark the index as being under construction, in case the definition is circular
                putLocalIndex(doc, keySetNumber, itemType, underConstruction, context);
                index = buildIndex(keySet, itemType, foundItemTypes, doc, context);
                putLocalIndex(doc, keySetNumber, itemType, index, context);
                if (foundItemTypes != null) {
                    // build indexes for each item type actually found
                    for (BuiltInAtomicType t : foundItemTypes) {
                        if (!t.equals(BuiltInAtomicType.STRING)) {
                            putLocalIndex(doc, keySetNumber, t, underConstruction, context);
                            //noinspection NullableProblems
                            index = buildIndex(keySet, t, null, doc, context);
                            putLocalIndex(doc, keySetNumber, t, index, context);
                        }
                    }
                }
            }
        }
        return index;
    }

    private static AtomicMatchKey getCollationKey(AtomicValue value, BuiltInAtomicType itemType,
                                                  StringCollator collation, XPathContext context) throws XPathException {
        if (itemType.equals(BuiltInAtomicType.STRING) ||
                itemType.equals(BuiltInAtomicType.UNTYPED_ATOMIC) ||
                itemType.equals(BuiltInAtomicType.ANY_URI)) {
            if (collation == null) {
                return new CodepointMatchKey(value.getStringValue());
            } else {
                return collation.getCollationKey(value.getStringValue());
            }
        } else {
            return value.getXPathComparable(false, collation, context.getImplicitTimezone());
        }
    }

    /**
     * Save the index associated with a particular key, a particular item type,
     * and a particular document. This
     * needs to be done in such a way that the index is discarded by the garbage collector
     * if the document is discarded. We therefore use a WeakHashMap indexed on the DocumentInfo,
     * which returns HashMap giving the index for each key fingerprint. This index is itself another
     * HashMap.
     * The methods need to be synchronized because several concurrent transformations (which share
     * the same KeyManager) may be creating indexes for the same or different documents at the same
     * time.
     *
     * @param doc            the document being indexed
     * @param keyFingerprint represents the name of the key definition
     * @param itemType       the primitive type of the values being indexed
     * @param index          the index being saved
     * @param context        the dynamic evaluation context
     */

    private synchronized void putSharedIndex(
                DocumentInfo doc,
                int keyFingerprint,
                AtomicType itemType,
                Map<AtomicMatchKey, List<NodeInfo>> index,
                XPathContext context) {
        if (docIndexes == null) {
            // it's transient, so it will be null when reloading a compiled stylesheet
            docIndexes = new WeakHashMap<DocumentInfo, WeakReference<HashMap<Long, Map<AtomicMatchKey, List<NodeInfo>>>>>(10);
        }
        WeakReference<HashMap<Long, Map<AtomicMatchKey, List<NodeInfo>>>> indexRef = docIndexes.get(doc);
        HashMap<Long, Map<AtomicMatchKey, List<NodeInfo>>> indexList;
        if (indexRef == null || indexRef.get() == null) {
            indexList = new HashMap<Long, Map<AtomicMatchKey, List<NodeInfo>>>(10);
            // Ensure there is a firm reference to the indexList for the duration of a transformation
            // But for keys associated with temporary trees, or documents that have been discarded from
            // the document pool, keep the reference within the document node itself.
            Controller controller = context.getController();
            if (controller.getDocumentPool().contains(doc)) {
                context.getController().setUserData(doc, "saxon:key-index-list", indexList);
            } else {
                doc.setUserData("saxon:key-index-list", indexList);
            }
            docIndexes.put(doc, new WeakReference<HashMap<Long, Map<AtomicMatchKey, List<NodeInfo>>>>(indexList));
        } else {
            indexList = indexRef.get();
        }
        indexList.put((long) keyFingerprint << 32 | itemType.getFingerprint(), index);
    }

    /**
     * Save the index associated with a particular key, a particular item type,
     * and a particular document. This version of the method is used for indexes that are
     * not reusable across transformations, because the key depends on transformation-specific
     * data such as global variables or parameters.
     * The method still need to be synchronized because several threads within a transformation
     * may be creating indexes for the same or different documents at the same
     * time.
     *
     * @param doc            the document being indexed
     * @param keyFingerprint represents the name of the key definition
     * @param itemType       the primitive type of the values being indexed
     * @param index          the index being saved
     * @param context        the dynamic evaluation context
     */

    private synchronized void putLocalIndex(
                DocumentInfo doc,
                int keyFingerprint,
                AtomicType itemType,
                Map<AtomicMatchKey, List<NodeInfo>> index,
                XPathContext context) {
        Controller controller = context.getController();
        Map<Long, Map<AtomicMatchKey, List<NodeInfo>>> docIndexes =
                (Map<Long, Map<AtomicMatchKey, List<NodeInfo>>>)controller.getUserData(doc, "saxon:unshared-key-index-list");
        if (docIndexes == null) {
            docIndexes = new HashMap<Long, Map<AtomicMatchKey, List<NodeInfo>>>();
        }
        long indexId = (long) keyFingerprint << 32 | itemType.getFingerprint();
        docIndexes.put(indexId, index);
    }


    /**
     * Get the shared index associated with a particular key, a particular source document,
     * and a particular primitive item type
     *
     * @param doc            the document whose index is required
     * @param keyFingerprint the name of the key definition
     * @param itemType       the primitive item type of the values being indexed
     * @return either an index (as a HashMap), or the dummy map "under construction", or null
     */

    private synchronized Map<AtomicMatchKey, List<NodeInfo>>
            getSharedIndex(DocumentInfo doc, int keyFingerprint, AtomicType itemType) {
        if (docIndexes == null) {
            // it's transient, so it will be null when reloading a compiled stylesheet
            docIndexes = new WeakHashMap<DocumentInfo, WeakReference<HashMap<Long, Map<AtomicMatchKey, List<NodeInfo>>>>>(10);
        }
        WeakReference<HashMap<Long, Map<AtomicMatchKey, List<NodeInfo>>>> ref = docIndexes.get(doc);
        if (ref == null) {
            return null;
        }
        HashMap<Long, Map<AtomicMatchKey, List<NodeInfo>>> indexList = ref.get();
        if (indexList == null) {
            return null;
        }
        return indexList.get((long) keyFingerprint << 32 | itemType.getFingerprint());
    }

    /**
     * Get the non-shared index associated with a particular key, a particular source document,
     * and a particular primitive item type
     *
     * @param doc            the document whose index is required
     * @param keyFingerprint the name of the key definition
     * @param itemType       the primitive item type of the values being indexed
     * @param context        the dynamic evaluation context
     * @return either an index (as a HashMap), or the dummy map "under construction", or null
     */

    private synchronized Map<AtomicMatchKey, List<NodeInfo>>
            getLocalIndex(DocumentInfo doc, int keyFingerprint, AtomicType itemType, XPathContext context) {
        Controller controller = context.getController();
        Map<Long, Map<AtomicMatchKey, List<NodeInfo>>> docIndexes =
                (Map<Long, Map<AtomicMatchKey, List<NodeInfo>>>)controller.getUserData(doc, "saxon:unshared-key-index-list");
        if (docIndexes == null) {
            return null;
        }
        long indexId = (long) keyFingerprint << 32 | itemType.getFingerprint();
        return docIndexes.get(indexId);
    }


    /**
     * Clear all the indexes for a given document. This is currently done whenever updates
     * are applied to the document, because updates can potentially invalidate the indexes.
     *
     * @param doc the document whose indexes are to be invalidated
     */

    public synchronized void clearDocumentIndexes(DocumentInfo doc) {
        docIndexes.remove(doc);
    }

    /**
     * Get all the key definition sets
     * @return a set containing all the key definition sets
     */

    public Collection<KeyDefinitionSet> getAllKeyDefinitionSets() {
        return keyMap.values();
    }

    /**
     * Get the number of distinctly-named key definitions
     *
     * @return the number of key definition sets (where the key definitions in one set share the same name)
     */

    public int getNumberOfKeyDefinitions() {
        return keyMap.size();
    }

    /**
     * Diagnostic output explaining the keys
     *
     * @param out the expression presenter that will display the information
     */

    public void explainKeys(ExpressionPresenter out) {
        if (keyMap.size() < 2) {
            // don't bother with IDREFS if it's the only index
            return;
        }
        out.startElement("keys");
        for (Map.Entry<StructuredQName, KeyDefinitionSet> e : keyMap.entrySet()) {
            StructuredQName qName = e.getKey();
            List<KeyDefinition> list = e.getValue().getKeyDefinitions();
            for (KeyDefinition kd : list) {
                out.startElement("key");
                out.emitAttribute("name", qName.getDisplayName());
                out.emitAttribute("match", kd.getMatch().toString());
                if (kd.isRangeKey()) {
                    out.emitAttribute("range", "true");
                }
                kd.getUse().explain(out);
                out.endElement();
            }
        }
        out.endElement();
    }
}

