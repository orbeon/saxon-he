package net.sf.saxon.trans;

import net.sf.saxon.Configuration;
import net.sf.saxon.expr.Optimizer;
import net.sf.saxon.expr.XPathContext;
import net.sf.saxon.expr.XPathContextMajor;
import net.sf.saxon.instruct.Template;
import net.sf.saxon.om.NamespaceConstant;
import net.sf.saxon.om.Navigator;
import net.sf.saxon.om.NodeInfo;
import net.sf.saxon.om.StructuredQName;
import net.sf.saxon.pattern.*;
import net.sf.saxon.sort.GenericSorter;
import net.sf.saxon.sort.IntHashMap;
import net.sf.saxon.sort.IntIterator;
import net.sf.saxon.sort.Sortable;
import net.sf.saxon.trace.ExpressionPresenter;
import net.sf.saxon.trace.Location;
import net.sf.saxon.type.Type;
import net.sf.saxon.value.Whitespace;

import java.io.Serializable;
import java.util.ArrayList;

/**
 * A Mode is a collection of rules; the selection of a rule to apply to a given element
 * is determined by a Pattern.
 *
 * @author Michael H. Kay
 */

public class Mode implements Serializable {

    // TODO:PERF the data structure does not cater well for a stylesheet making heavy use of
    // match="schema-element(X)". We should probably expand the substitution group.

    public static final int DEFAULT_MODE = -1;
    public static final int NAMED_MODE = -3;
    public static final int STRIPPER_MODE = -4;

    public static final StructuredQName ALL_MODES =
            new StructuredQName("saxon", NamespaceConstant.SAXON, "_omniMode");
    public static final StructuredQName DEFAULT_MODE_NAME =
            new StructuredQName("saxon", NamespaceConstant.SAXON, "_defaultMode");

    private Rule genericNodeRuleChain = null;

    private Rule documentRuleChain = null;
    private Rule textRuleChain = null;
    private Rule commentRuleChain = null;
    private Rule processingInstructionRuleChain = null;
    private Rule namespaceRuleChain = null;
    private Rule unnamedElementRuleChain = null;
    private Rule unnamedAttributeRuleChain = null;
    private IntHashMap<Rule> namedElementRuleChains = new IntHashMap<Rule>(32);
    private IntHashMap<Rule> namedAttributeRuleChains = new IntHashMap<Rule>(8);

    private Rule mostRecentRule;
    private boolean isDefault;
    private boolean streamable;
    private boolean isStripper;
    private boolean hasRules = false;
    private StructuredQName modeName;
    private int stackFrameSlotsNeeded = 0;
    private int recoveryPolicy = Configuration.RECOVER_WITH_WARNINGS; // since 9.2 fixed at compile time

    /**
     * Default constructor - creates a Mode containing no rules
     * @param usage one of {@link #DEFAULT_MODE}, {@link #NAMED_MODE}, {@link #STRIPPER_MODE}
     * @param modeName the name of the mode
     */

    public Mode(int usage, StructuredQName modeName) {
        isDefault = (usage == DEFAULT_MODE);
        isStripper = (usage == STRIPPER_MODE);
        this.modeName = modeName;
    }

    /**
     * Construct a new Mode, copying the contents of an existing Mode
     *
     * @param omniMode the existing mode. May be null, in which case it is not copied
     * @param modeName the name of the new mode to be created
     */

    public Mode(Mode omniMode, StructuredQName modeName) {
        isDefault = false;
        isStripper = false;
        this.modeName = modeName;
        if (omniMode != null) {
            documentRuleChain =
                    omniMode.documentRuleChain==null ? null : new Rule(omniMode.documentRuleChain);
            textRuleChain =
                    omniMode.textRuleChain==null ? null : new Rule(omniMode.textRuleChain);
            commentRuleChain =
                    omniMode.commentRuleChain==null ? null : new Rule(omniMode.commentRuleChain);
            processingInstructionRuleChain =
                    omniMode.processingInstructionRuleChain==null ? null : new Rule(omniMode.processingInstructionRuleChain);
            namespaceRuleChain =
                    omniMode.namespaceRuleChain==null ? null : new Rule(omniMode.namespaceRuleChain);
            unnamedElementRuleChain =
                    omniMode.unnamedElementRuleChain==null ? null : new Rule(omniMode.unnamedElementRuleChain);
            unnamedAttributeRuleChain =
                    omniMode.unnamedAttributeRuleChain==null ? null : new Rule(omniMode.unnamedAttributeRuleChain);

            namedElementRuleChains = new IntHashMap(omniMode.namedElementRuleChains.size());
            IntIterator ii = omniMode.namedElementRuleChains.keyIterator();
            while (ii.hasNext()) {
                int fp = ii.next();
                Rule r = omniMode.namedElementRuleChains.get(fp);
                namedElementRuleChains.put(fp, new Rule(r));
            }
            ii = omniMode.namedAttributeRuleChains.keyIterator();
            while (ii.hasNext()) {
                int fp = ii.next();
                Rule r = omniMode.namedAttributeRuleChains.get(fp);
                namedAttributeRuleChains.put(fp, new Rule(r));
            }
            mostRecentRule = omniMode.mostRecentRule;
        }
    }

    /**
     * Determine if this is the default mode
     * @return true if this is the default (unnamed) mode
     */

    public boolean isDefaultMode() {
        return isDefault;
    }

    /**
     * Get the name of the mode (for diagnostics only)
     * @return the mode name. Null for the default (unnamed) mode
     */

    public StructuredQName getModeName() {
        return modeName;
    }

    /**
     * Ask whether there are any template rules in this mode
     * (a mode could exist merely because it is referenced in apply-templates)
     * @return true if no template rules exist in this mode
     */

    public boolean isEmpty() {
        return !hasRules;
    }

    /**
     * Set the policy for handling recoverable errrors. Note that for some errors the decision can be
     * made at run-time, but for the "ambiguous template match" error, the decision is (since 9.2)
     * fixed at compile time.
     * @param policy the recovery policy to be used. The options are {@link Configuration#RECOVER_SILENTLY},
     * {@link Configuration#RECOVER_WITH_WARNINGS}, or {@link Configuration#DO_NOT_RECOVER}.
     */

    public void setRecoveryPolicy(int policy) {
        recoveryPolicy = policy;
    }

    /**
     * Get the policy for handling recoverable errors. Note that for some errors the decision can be
     * made at run-time, but for the "ambiguous template match" error, the decision is (since 9.2)
     * fixed at compile time.
     *
     * @return the current policy.
     */

    public int getRecoveryPolicy() {
        return recoveryPolicy;
    }

    /**
     * Say that this mode is (or is not) streamable
     * @param streamable true if this mode is a streamable mode
     */

    public void setStreamable(boolean streamable) throws XPathException {
        this.streamable = streamable;
    }

    /**
     * Ask whether this mode is streamable
     * @return true if this mode is streamable
     */

    public boolean isStreamable() {
        return streamable;
    }


    /**
     * Add a rule to the Mode.
     *
     * @param pattern          a Pattern
     * @param action     the Object to return from getRule() when the supplied node matches this Pattern
     * @param precedence the import precedence of the rule
     * @param priority   the explicit or implicit priority of the rule
     * @param explicitMode  true if adding a template rule for a specific (default or named) mode;
     *      false if adding a rule because it applies to all modes
     */

    public void addRule(Pattern pattern, RuleTarget action, int precedence, double priority, boolean explicitMode) {

        if (explicitMode) {
            hasRules = true;
        }

        // Ignore a pattern that will never match, e.g. "@comment"

        if (pattern.getNodeTest() instanceof EmptySequenceTest) {
            return;
        }

        // for fast lookup, we maintain one list for each element name for patterns that can only
        // match elements of a given name, one list for each node type for patterns that can only
        // match one kind of non-element node, and one generic list.
        // Each list is sorted in precedence/priority order so we find the highest-priority rule first

        // This logic is designed to ensure that when a UnionPattern contains multiple branches
        // with the same priority, next-match doesn't select the same template twice (override20_047)
        int sequence;
        if (mostRecentRule == null) {
            sequence = 0;
        } else if (action == mostRecentRule.getAction()) {
            sequence = mostRecentRule.getSequence();
        } else {
            sequence = mostRecentRule.getSequence() + 1;
        }
        Rule newRule = new Rule(pattern, action, precedence, priority, sequence);
        if (pattern instanceof NodeTestPattern) {
            NodeTest test = pattern.getNodeTest();
            if (test instanceof AnyNodeTest) {
                newRule.setAlwaysMatches(true);
            } else if (test instanceof NodeKindTest) {
                newRule.setAlwaysMatches(true);
            } else if (test instanceof NameTest) {
                int kind = test.getPrimitiveType();
                if (kind == Type.ELEMENT || kind == Type.ATTRIBUTE) {
                    newRule.setAlwaysMatches(true);
                }
            }

        }
        mostRecentRule = newRule;

        int kind = pattern.getNodeKind();
        switch (kind) {
            case Type.ELEMENT: {
                int fp = pattern.getFingerprint();
                if (fp == -1) {
                    unnamedElementRuleChain = addRuleToList(newRule, unnamedElementRuleChain);
                } else {
                    Rule chain = namedElementRuleChains.get(fp);
                    namedElementRuleChains.put(fp, addRuleToList(newRule, chain));
                }
                break;
            }
            case Type.ATTRIBUTE: {
                int fp = pattern.getFingerprint();
                if (fp == -1) {
                    unnamedAttributeRuleChain = addRuleToList(newRule, unnamedAttributeRuleChain);
                } else {
                    Rule chain = namedAttributeRuleChains.get(fp);
                    namedAttributeRuleChains.put(fp, addRuleToList(newRule, chain));
                }
                break;
            }
            case Type.NODE:
                genericNodeRuleChain = addRuleToList(newRule, genericNodeRuleChain);
                break;
            case Type.DOCUMENT:
                documentRuleChain = addRuleToList(newRule, documentRuleChain);
                break;
            case Type.TEXT:
                textRuleChain = addRuleToList(newRule, textRuleChain);
                break;
            case Type.COMMENT:
                commentRuleChain = addRuleToList(newRule, commentRuleChain);
                break;
            case Type.PROCESSING_INSTRUCTION:
                processingInstructionRuleChain = addRuleToList(newRule, processingInstructionRuleChain);
                break;
            case Type.NAMESPACE:
                namespaceRuleChain = addRuleToList(newRule, namespaceRuleChain);
                break;
        }

    }

    /**
     * Insert a new rule into this list before others of the same precedence/priority
     * @param newRule the new rule to be added into the list
     * @param list the Rule at the head of the list, or null if the list is empty
     * @return the new head of the list (which might be the old head, or the new rule if it
     * was inserted at the start)
     */


    private Rule addRuleToList(Rule newRule, Rule list) {
        if (list == null) {
            return newRule;
        }
        int precedence = newRule.getPrecedence();
        double priority = newRule.getPriority();
        Rule rule = list;
        Rule prev = null;
        while (rule != null) {
            if ((rule.getPrecedence() < precedence) ||
                    (rule.getPrecedence() == precedence && rule.getPriority() <= priority)) {
                newRule.setNext(rule);
                if (prev == null) {
                    return newRule;
                } else {
                    prev.setNext(newRule);
                }
                break;
            } else {
                prev = rule;
                rule = rule.getNext();
            }
        }
        if (rule == null) {
            prev.setNext(newRule);
            newRule.setNext(null);
        }
        return list;
    }

    /**
     * Specify how many slots for local variables are required by a particular pattern
     * @param slots the number of slots needed
     */

    public void allocatePatternSlots(int slots) {
        stackFrameSlotsNeeded = Math.max(stackFrameSlotsNeeded, slots);
    }

    /**
     * Make a new XPath context for evaluating patterns if there is any possibility that the
     * pattern uses local variables
     *
     * @param context The existing XPath context
     * @return a new XPath context (or the existing context if no new context was created)
     */

    private XPathContext makeNewContext(XPathContext context) {
        context = context.newContext();
        context.setOriginatingConstructType(Location.CONTROLLER);
        ((XPathContextMajor)context).openStackFrame(stackFrameSlotsNeeded);
        return context;
    }

    /**
     * Get the rule corresponding to a given Node, by finding the best Pattern match.
     *
     * @param node the NodeInfo referring to the node to be matched
     * @param context the XPath dynamic evaluation context
     * @return the best matching rule, if any (otherwise null).
     */

    public Rule getRule(NodeInfo node, XPathContext context) throws XPathException {

        // If there are match patterns in the stylesheet that use local variables, we need to allocate
        // a new stack frame for evaluating the match patterns. We base this on the match pattern with
        // the highest number of range variables, so we can reuse the same stack frame for all rules
        // that we test against. If no patterns use range variables, we don't bother allocating a new
        // stack frame.

        // Note, this method isn't functionally necessary; we could call the 3-argument version
        // with a filter that always returns true. But this is the common path for apply-templates,
        // and we want to squeeze every drop of performance from it.

        if (stackFrameSlotsNeeded > 0) {
            context = makeNewContext(context);
        }

        // search the specific list for this node type / node name

        Rule unnamedNodeChain;
        Rule bestRule = null;

        switch (node.getNodeKind()) {
            case Type.DOCUMENT:
                unnamedNodeChain = documentRuleChain;
                break;

            case Type.ELEMENT: {
                unnamedNodeChain = unnamedElementRuleChain;
                Rule namedNodeChain = namedElementRuleChains.get(node.getFingerprint());
                if (namedNodeChain != null) {
                    bestRule = searchRuleChain(node, context, null, namedNodeChain);
                }
                break;
            }
            case Type.ATTRIBUTE: {
                unnamedNodeChain = unnamedAttributeRuleChain;
                Rule namedNodeChain = namedAttributeRuleChains.get(node.getFingerprint());
                if (namedNodeChain != null) {
                    bestRule = searchRuleChain(node, context, null, namedNodeChain);
                }
                break;
            }
            case Type.TEXT:
                unnamedNodeChain = textRuleChain;
                break;
            case Type.COMMENT:
                unnamedNodeChain = commentRuleChain;
                break;
            case Type.PROCESSING_INSTRUCTION:
                unnamedNodeChain = processingInstructionRuleChain;
                break;
            case Type.NAMESPACE:
                unnamedNodeChain = namespaceRuleChain;
                break;
            default:
                throw new AssertionError("Unknown node kind");
        }

        // search the list for unnamed nodes of a particular kind

        if (unnamedNodeChain != null) {
            bestRule = searchRuleChain(node, context, bestRule, unnamedNodeChain);
        }

        // Search the list for rules for nodes of unknown node kind

        if (genericNodeRuleChain != null) {
            bestRule = searchRuleChain(node, context, bestRule, genericNodeRuleChain);
        }

        return bestRule;
    }

    /**
     * Search a chain of rules
     * @param node the node being matched
     * @param context XPath dynamic context
     * @param bestRule the best rule so far in terms of precedence and priority (may be null)
     * @param head the rule at the head of the chain to be searched
     * @return the best match rule found in the chain, or the previous best rule, or null
     * @throws XPathException
     */

    private Rule searchRuleChain(NodeInfo node, XPathContext context, Rule bestRule, Rule head) throws XPathException {
        while (head != null) {
            if (bestRule != null) {
                int rank = head.compareRank(bestRule);
                if (rank < 0) {
                    // if we already have a match, and the precedence or priority of this
                    // rule is lower, quit the search
                    break;
                } else if (rank == 0) {
                    // this rule has the same precedence and priority as the matching rule already found
                    if (head.isAlwaysMatches() || head.getPattern().matches(node, context)) {
                        reportAmbiguity(node, bestRule, head, context);
                        // choose whichever one comes last (assuming the error wasn't fatal)
                        bestRule = (bestRule.getSequence() > head.getSequence() ? bestRule : head);
                        break;
                    } else {
                        // keep searching other rules of the same precedence and priority
                    }
                } else {
                    // this rule has higher rank than the matching rule already found
                    if (head.isAlwaysMatches() || head.getPattern().matches(node, context)) {
                        bestRule = head;
                    }
                }
            } else if (head.isAlwaysMatches() || head.getPattern().matches(node, context)) {
                bestRule = head;
                if (recoveryPolicy == Configuration.RECOVER_SILENTLY) {
                    break;   // choose the first match; rules within a chain are in order of rank
                }
            }
            head = head.getNext();
        }
        return bestRule;
    }

    /**
     * Get the rule corresponding to a given Node, by finding the best Pattern match.
     *
     * @param node the NodeInfo referring to the node to be matched
     * @param context the XPath dynamic evaluation context
     * @return the best matching rule, if any (otherwise null).
     */

    public Rule getRule(NodeInfo node, XPathContext context, RuleFilter filter) throws XPathException {

        // If there are match patterns in the stylesheet that use local variables, we need to allocate
        // a new stack frame for evaluating the match patterns. We base this on the match pattern with
        // the highest number of range variables, so we can reuse the same stack frame for all rules
        // that we test against. If no patterns use range variables, we don't bother allocating a new
        // stack frame.

        if (stackFrameSlotsNeeded > 0) {
            context = makeNewContext(context);
        }

        // search the specific list for this node type / node name

        Rule bestRule = null;
        Rule unnamedNodeChain;

        switch (node.getNodeKind()) {
            case Type.DOCUMENT:
                unnamedNodeChain = documentRuleChain;
                break;
            case Type.ELEMENT: {
                unnamedNodeChain = unnamedElementRuleChain;
                Rule namedNodeChain = namedElementRuleChains.get(node.getFingerprint());
                bestRule = searchRuleChain(node, context, null, namedNodeChain, filter);
                break;
            }
            case Type.ATTRIBUTE: {
                unnamedNodeChain = unnamedAttributeRuleChain;
                Rule namedNodeChain = namedAttributeRuleChains.get(node.getFingerprint());
                bestRule = searchRuleChain(node, context, null, namedNodeChain, filter);
                break;
            }
            case Type.TEXT:
                unnamedNodeChain = textRuleChain;
                break;
            case Type.COMMENT:
                unnamedNodeChain = commentRuleChain;
                break;
            case Type.PROCESSING_INSTRUCTION:
                unnamedNodeChain = processingInstructionRuleChain;
                break;
            case Type.NAMESPACE:
                unnamedNodeChain = namespaceRuleChain;
                break;
            default:
                throw new AssertionError("Unknown node kind");
        }

        // Search the list for unnamed nodes of a particular kind

        bestRule = searchRuleChain(node, context, bestRule, unnamedNodeChain, filter);

        // Search the list for rules for nodes of unknown node kind

        return searchRuleChain(node, context, bestRule, genericNodeRuleChain, filter);
    }

    /**
     * Search a chain of rules
     * @param node the node being matched
     * @param context XPath dynamic context
     * @param bestRule the best rule so far in terms of precedence and priority (may be null)
     * @param head the rule at the head of the chain to be searched
     * @return the best match rule found in the chain, or the previous best rule, or null
     * @throws XPathException
     */

    private Rule searchRuleChain(NodeInfo node, XPathContext context,
                                 Rule bestRule, Rule head, RuleFilter filter) throws XPathException {
        while (head != null) {
            if (filter.testRule(head)) {
                if (bestRule != null) {
                    int rank = head.compareRank(bestRule);
                    if (rank < 0) {
                        // if we already have a match, and the precedence or priority of this
                        // rule is lower, quit the search
                        break;
                    } else if (rank == 0) {
                        // this rule has the same precedence and priority as the matching rule already found
                        if (head.isAlwaysMatches() || head.getPattern().matches(node, context)) {
                            reportAmbiguity(node, bestRule, head, context);
                            // choose whichever one comes last (assuming the error wasn't fatal)
                            bestRule = (bestRule.getSequence() > head.getSequence() ? bestRule : head);
                            break;
                        } else {
                            // keep searching other rules of the same precedence and priority
                        }
                    } else {
                        // this rule has higher rank than the matching rule already found
                        if (head.isAlwaysMatches() || head.getPattern().matches(node, context)) {
                            bestRule = head;
                        }
                    }
                } else if (head.isAlwaysMatches() || head.getPattern().matches(node, context)) {
                    bestRule = head;
                    if (recoveryPolicy == Configuration.RECOVER_SILENTLY) {
                        break;   // choose the first match; rules within a chain are in order of rank
                    }
                }
            }
            head = head.getNext();
        }
        return bestRule;
    }


    /**
     * Get the rule corresponding to a given Node, by finding the best Pattern match, subject to a minimum
     * and maximum precedence. (This supports xsl:apply-imports)
     *
     * @param node the NodeInfo referring to the node to be matched
     * @param min the minimum import precedence
     * @param max the maximum import precedence
     * @param context the XPath dynamic evaluation context
     * @return the Rule registered for that node, if any (otherwise null).
     */

    public Rule getRule(NodeInfo node, final int min, final int max, XPathContext context) throws XPathException {
        RuleFilter filter = new RuleFilter() {
            public boolean testRule(Rule r) {
                int p = r.getPrecedence();
                return p >= min && p <= max;
            }
        };
        return getRule(node, context, filter);
    }

    /**
     * Get the rule corresponding to a given Node, by finding the next-best Pattern match
     * after the specified object.
     *
     * @param node the NodeInfo referring to the node to be matched
     * @param currentRule the current rule; we are looking for the next match after the current rule
     * @param context the XPath dynamic evaluation context
     * @return the object (e.g. a NodeHandler) registered for that element, if any (otherwise null).
     */

    public Rule getNextMatchRule(NodeInfo node, final Rule currentRule, XPathContext context) throws XPathException {
        RuleFilter filter = new RuleFilter() {
            public boolean testRule(Rule r) {
                int comp = r.compareRank(currentRule);
                return comp < 0 || (comp == 0 && r.getSequence() < currentRule.getSequence());
            }
        };
        return getRule(node, context, filter);
    }

    /**
     * Report an ambiguity, that is, the situation where two rules of the same
     * precedence and priority match the same node
     *
     * @param node The node that matches two or more rules
     * @param r1   The first rule that the node matches
     * @param r2   The second rule that the node matches
     * @param c    The controller for the transformation
     */

    private void reportAmbiguity(NodeInfo node, Rule r1, Rule r2, XPathContext c)
            throws XPathException {
        // don't report an error if the conflict is between two branches of the same Union pattern
        if (r1.getAction() == r2.getAction()) {
            return;
        }
        String path;
        String errorCode = "XTRE0540";

        if (isStripper) {
            // don't report an error if the conflict is between strip-space and strip-space, or
            // preserve-space and preserve-space
            if (r1.getAction().equals(r2.getAction())) {
                return;
            }
            errorCode = "XTRE0270";
            path = "xsl:strip-space";
        } else {
            path = Navigator.getPath(node);
        }

        Pattern pat1 = r1.getPattern();
        Pattern pat2 = r2.getPattern();

        XPathException err = new XPathException("Ambiguous rule match for " + path + '\n' +
                "Matches both \"" + showPattern(pat1) + "\" on line " + pat1.getLineNumber() + " of " + pat1.getSystemId() +
                "\nand \"" + showPattern(pat2) + "\" on line " + pat2.getLineNumber() + " of " + pat2.getSystemId());
        err.setErrorCode(errorCode);
        //err.setLocator(pat1.getL);
        c.getController().recoverableError(err);
    }

    private static String showPattern(Pattern p) {
        // Complex patterns can be laid out with lots of whitespace, which looks messy in the error message
        return Whitespace.collapseWhitespace(p.toString()).toString();
    }

    /**
     * Walk over all the rules, applying a specified action to each one.
     * @param action an action that is to be applied to all the rules in this Mode
     */

    public void processRules(RuleAction action) throws XPathException {
        processRuleChain(documentRuleChain, action);
        processRuleChain(unnamedElementRuleChain, action);
        IntIterator ii = namedElementRuleChains.keyIterator();
        while (ii.hasNext()) {
            Rule r = namedElementRuleChains.get(ii.next());
            processRuleChain(r, action);
        }
        processRuleChain(unnamedAttributeRuleChain, action);
        ii = namedAttributeRuleChains.keyIterator();
        while (ii.hasNext()) {
            Rule r = namedAttributeRuleChains.get(ii.next());
            processRuleChain(r, action);
        }
        processRuleChain(textRuleChain, action);
        processRuleChain(commentRuleChain, action);
        processRuleChain(processingInstructionRuleChain, action);
        processRuleChain(namespaceRuleChain, action);
        processRuleChain(genericNodeRuleChain, action);
    }

    private void processRuleChain(Rule r, RuleAction action) throws XPathException {
        while (r != null) {
            action.processRule(r);
            r = r.getNext();
        }
    }

    /**
     * For a streamable mode, invert all the templates to generate streamable code
     */

    public void invertStreamableTemplates(final Optimizer opt) throws XPathException {
        if (streamable) {
            RuleAction action = new RuleAction() {
                public void processRule(Rule r) throws XPathException {
                    NodeTest test = r.getPattern().getNodeTest();
                    int kind = test.getPrimitiveType();
                    if (kind == Type.DOCUMENT || kind == Type.ELEMENT) {
                        Template t = (Template)r.getAction();
                        RuleTarget inverse = opt.makeInversion(t, test);
                        r.setAction(inverse);
                    }
                }
            };
            processRules(action);
        }
    }

    /**
     * Explain all template rules in this mode by showing their
     * expression tree represented in XML.
     * @param presenter used to display the expression tree
     */

    public void explainTemplateRules(final ExpressionPresenter presenter) {
        RuleAction action = new RuleAction() {
            public void processRule(Rule r) {
                RuleTarget t = r.getAction();
                int s = presenter.startElement("templateRule");
                presenter.emitAttribute("match", r.getPattern().toString());
                presenter.emitAttribute("precedence", r.getPrecedence()+"");
                presenter.emitAttribute("priority", r.getPriority()+"");
                t.explain(presenter);
//                if (t instanceof Template) {
//                    presenter.emitAttribute("line", ((Template)t).getLineNumber()+"");
//                    presenter.emitAttribute("module", ((Template)t).getSystemId());
//                    if (((Template)t).getBody() != null) {
//                        ((Template)t).getBody().explain(presenter);
//                    }
//                }
                int e = presenter.endElement();
                if (s != e) {
                    throw new IllegalStateException(
                            "tree unbalanced in template at line " +
                                    (t instanceof Template ?
                                            (((Template)t).getLineNumber() + " of " + ((Template)t).getSystemId()) : ""));
                }
            }
        };
        try {
            processRules(action);
        } catch (XPathException err) {
            // can't happen, and doesn't matter if it does
        }
    }

    /**
     * Compute a rank for each rule, as a combination of the precedence and priority, to allow
     * rapid comparison.
     */

    public void computeRankings() throws XPathException {
        final RuleSorter sorter = new RuleSorter();
        RuleAction addToSorter = new RuleAction() {
            public void processRule(Rule r) {
                sorter.addRule(r);
            }
        };
        // add all the rules in this Mode to the sorter
        processRules(addToSorter);
        // now allocate ranks to all the modes
        sorter.allocateRanks();
    }

    /**
     * Supporting class used at compile time to sort all the rules into precedence/priority
     * order and allocate a rank to each one, so that at run-time, comparing two rules to see
     * which has higher precedence/priority is a simple integer subtraction.
     */

    private static class RuleSorter implements Sortable {
        public ArrayList<Rule> rules = new ArrayList<Rule>(100);
        public void addRule(Rule rule) {
            rules.add(rule);
        }
    
        public int compare(int a, int b) {
            return rules.get(a).compareComputedRank(rules.get(b));
        }

        public void swap(int a, int b) {
            Rule temp = rules.get(a);
            rules.set(a, rules.get(b));
            rules.set(b, temp);
        }

        public void allocateRanks() {
            GenericSorter.quickSort(0, rules.size(), this);
            int rank = 0;
            for (int i=0; i<rules.size(); i++) {
                if ( i> 0 && rules.get(i-1).compareComputedRank(rules.get(i)) != 0) {
                    rank++;
                }
                rules.get(i).setRank(rank);
            }
        }
    }

    /**
     * Interface for helper classes used to filter a chain of rules
     */

    private interface RuleFilter {
        /**
         * Test a rule to see whether it should be included
         * @param r the rule to be tested
         * @return true if the rule qualifies
         */
        public boolean testRule(Rule r);
    }

    /**
     * Interface for helper classes used to process all the rules in the Mode
     */

    private interface RuleAction {
        /**
         * Process a given rule
         * @param r the rule to be processed
         */
        public void processRule(Rule r) throws XPathException;
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
// Contributor(s): none.
//
