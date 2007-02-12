package net.sf.saxon.trans;

import net.sf.saxon.Configuration;
import net.sf.saxon.expr.XPathContext;
import net.sf.saxon.expr.XPathContextMajor;
import net.sf.saxon.om.Navigator;
import net.sf.saxon.om.NodeInfo;
import net.sf.saxon.pattern.EmptySequenceTest;
import net.sf.saxon.pattern.Pattern;
import net.sf.saxon.type.Type;
import net.sf.saxon.value.Whitespace;

import java.io.Serializable;

/**
 * A Mode is a collection of rules; the selection of a rule to apply to a given element
 * is determined by a Pattern.
 *
 * @author Michael H. Kay
 */

public class Mode implements Serializable {

    // TODO: the data structure does not cater well for a stylesheet making heavy use of
    // match="schema-element(X)". We should probably expand the substitution group.

    public static final int DEFAULT_MODE = -1;
    public static final int ALL_MODES = -2;
    public static final int NAMED_MODE = -3;
    public static final int STRIPPER_MODE = -4;


    private Rule[] ruleDict = new Rule[101 + Type.MAX_NODE_TYPE];
    //private int sequence = 0;   // sequence number for the rules in this Mode
    private Rule mostRecentRule;
    private boolean isDefault;
    private boolean isStripper;
    private int modeNameCode;

    /**
     * Default constructor - creates a Mode containing no rules
     * @param usage one of {@link #DEFAULT_MODE}, {@link #NAMED_MODE}, {@link #STRIPPER_MODE}
     */

    public Mode(int usage, int nameCode) {
        this.isDefault = (usage == DEFAULT_MODE);
        this.isStripper = (usage == STRIPPER_MODE);
        this.modeNameCode = nameCode;
    }



    /**
     * Construct a new Mode, copying the contents of an existing Mode
     *
     * @param omniMode the existing mode. May be null, in which case it is not copied
     */

    public Mode(Mode omniMode, int modeNameCode) {
        isDefault = false;
        isStripper = false;
        this.modeNameCode = modeNameCode;
        if (omniMode != null) {
            for (int i = 0; i < this.ruleDict.length; i++) {
                if (omniMode.ruleDict[i] != null) {
                    this.ruleDict[i] = new Rule(omniMode.ruleDict[i]);
                }
            }
            this.mostRecentRule = omniMode.mostRecentRule;
        }
    }

    /**
     * Determine if this is the default mode
     */

    public boolean isDefaultMode() {
        return isDefault;
    }

    /**
     * Get the name of the mode (for diagnostics only)
     */

    public int getModeNameCode() {
        return modeNameCode;
    }

    /**
     * Add a rule to the Mode.
     *
     * @param p          a Pattern
     * @param action     the Object to return from getRule() when the supplied node matches this Pattern
     * @param precedence the import precedence of the rule
     * @param priority   the explicit or implicit priority of the rule
     */

    public void addRule(Pattern p, Object action, int precedence, double priority) {

        // Ignore a pattern that will never match, e.g. "@comment"

        if (p.getNodeTest() instanceof EmptySequenceTest) {
            return;
        }

        // for fast lookup, we maintain one list for each element name for patterns that can only
        // match elements of a given name, one list for each node type for patterns that can only
        // match one kind of non-element node, and one generic list.
        // Each list is sorted in precedence/priority order so we find the highest-priority rule first

        int fingerprint = p.getFingerprint();
        int type = p.getNodeKind();

        int key = getList(fingerprint, type);
        // System.err.println("Fingerprint " + fingerprint + " key " + key + " type " + type);

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
        Rule newRule = new Rule(p, action, precedence, priority, sequence);
        mostRecentRule = newRule; 

        Rule rule = ruleDict[key];
        if (rule == null) {
            ruleDict[key] = newRule;
            return;
        }

        // insert the new rule into this list before others of the same precedence/priority

        Rule prev = null;
        while (rule != null) {
            if ((rule.getPrecedence() < precedence) ||
                    (rule.getPrecedence() == precedence && rule.getPriority() <= priority)) {
                newRule.setNext(rule);
                if (prev == null) {
                    ruleDict[key] = newRule;
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
    }

    /**
     * Determine which list to use for a given pattern (we must also search the generic list)
     */

    public int getList(int fingerprint, int type) {

        if (type == Type.ELEMENT) {
            if (fingerprint == -1) {
                return Type.NODE;   // the generic list
            } else {
                return Type.MAX_NODE_TYPE +
                        (fingerprint % 101);
            }
        } else {
            return type;
        }
    }

    /**
     * Get the rule corresponding to a given Node, by finding the best Pattern match.
     *
     * @param node the NodeInfo referring to the node to be matched
     * @return the best matching rule, if any (otherwise null).
     */

    public Rule getRule(NodeInfo node, XPathContext context) throws XPathException {
        int fingerprint = node.getFingerprint();
                    // This is inefficient with wrapped object models (DOM, XOM, JDOM),
                    // but there's not much we can do about it
        int type = node.getNodeKind();
        int key = getList(fingerprint, type);
        int policy = context.getController().getRecoveryPolicy();

        // If there are match patterns in the stylesheet that use local variables, we need to allocate
        // a new stack frame for evaluating the match patterns. We base this on the match pattern with
        // the highest number of range variables, so we can reuse the same stack frame for all rules
        // that we test against. If no patterns use range variables, we don't bother allocating a new
        // stack frame.

        context = perhapsMakeNewContext(context);

        Rule specificRule = null;
        Rule generalRule = null;
        int specificPrecedence = -1;
        double specificPriority = Double.NEGATIVE_INFINITY;

        // search the specific list for this node type / node name

        if (key != Type.NODE) {
            Rule r = ruleDict[key];
            while (r != null) {
                // if we already have a match, and the precedence or priority of this
                // rule is lower, quit the search for a second match
                if (specificRule != null) {
                    if (r.getPrecedence() < specificPrecedence ||
                            (r.getPrecedence() == specificPrecedence && r.getPriority() < specificPriority)) {
                        break;
                    }
                }
                if (r.getPattern().matches(node, context)) {

                    // is this a second match?
                    if (specificRule != null) {
                        if (r.getPrecedence() == specificPrecedence && r.getPriority() == specificPriority) {
                            reportAmbiguity(node, specificRule, r, context);
                        }
                        break;
                    }
                    specificRule = r;
                    specificPrecedence = r.getPrecedence();
                    specificPriority = r.getPriority();
                    if (policy == Configuration.RECOVER_SILENTLY) {
                        break;                      // find the first; they are in priority order
                    }
                }
                r = r.getNext();
            }
        }

        // search the general list

        Rule r2 = ruleDict[Type.NODE];
        while (r2 != null) {
            if (r2.getPrecedence() < specificPrecedence ||
                    (r2.getPrecedence() == specificPrecedence && r2.getPriority() < specificPriority)) {
                break;      // no point in looking at a lower priority rule than the one we've got
            }
            if (r2.getPattern().matches(node, context)) {
                // is it a second match?
                if (generalRule != null) {
                    if (r2.getPrecedence() == generalRule.getPrecedence() && r2.getPriority() == generalRule.getPriority()) {
                        reportAmbiguity(node, r2, generalRule, context);
                    }
                    break;
                } else {
                    generalRule = r2;
                    if (policy == Configuration.RECOVER_SILENTLY) {
                        break;                      // find only the first; they are in priority order
                    }
                }
            }
            r2 = r2.getNext();
        }

        if (specificRule != null && generalRule == null) {
            return specificRule;
        }
        if (specificRule == null && generalRule != null) {
            return generalRule;
        }
        if (specificRule != null && generalRule != null) {
            if (specificRule.getPrecedence() == generalRule.getPrecedence() &&
                    specificRule.getPriority() == generalRule.getPriority()) {
                // This situation is exceptional: we have a "specific" pattern and
                // a "general" pattern with the same priority. We have to select
                // the one that was added last
                // (Bug reported by Norman Walsh Jan 2002)
                Rule result = (specificRule.getSequence() > generalRule.getSequence() ?
                        specificRule :
                        generalRule);

                if (policy != Configuration.RECOVER_SILENTLY) {
                    reportAmbiguity(node, specificRule, generalRule, context);
                }
                return result;
            }
            if (specificRule.getPrecedence() > generalRule.getPrecedence() ||
                    (specificRule.getPrecedence() == generalRule.getPrecedence() &&
                    specificRule.getPriority() >= generalRule.getPriority())) {
                return specificRule;
            } else {
                return generalRule;
            }
        }
        return null;
    }

    /**
     * Make a new XPath context for evaluating patterns if there is any possibility that the
     * pattern uses local variables
     *
     * @param context The existing XPath context
     * @return a new XPath context (or the existing context if no new context was created)
     */

    private XPathContext perhapsMakeNewContext(XPathContext context) {
        int patternLocals = context.getController().getExecutable().getLargestPatternStackFrame();
        if (patternLocals > 0) {
            context = context.newContext();
            context.setOrigin(context.getController());
            ((XPathContextMajor)context).openStackFrame(patternLocals);
        }
        return context;
    }

    /**
     * Get the rule corresponding to a given Node, by finding the best Pattern match, subject to a minimum
     * and maximum precedence. (This supports xsl:apply-imports)
     *
     * @param node the NodeInfo referring to the node to be matched
     * @return the object (e.g. a NodeHandler) registered for that element, if any (otherwise null).
     */

    public Rule getRule(NodeInfo node, int min, int max, XPathContext context) throws XPathException {
        int fp = node.getFingerprint();
        int type = node.getNodeKind();
        int key = getList(fp, type);

        Rule specificRule = null;
        Rule generalRule = null;

        context = perhapsMakeNewContext(context);

        // search the the specific list for this node type / name

        if (key != Type.NODE) {
            Rule r = ruleDict[key];
            while (r != null) {
                if (r.getPrecedence() >= min && r.getPrecedence() <= max &&
                        r.getPattern().matches(node, context)) {
                    specificRule = r;
                    break;                      // find the first; they are in priority order
                }
                r = r.getNext();
            }
        }

        // search the generic list

        Rule r2 = ruleDict[Type.NODE];
        while (r2 != null) {
            if (r2.getPrecedence() >= min && r2.getPrecedence() <= max && r2.getPattern().matches(node, context)) {
                generalRule = r2;
                break;                      // find only the first; they are in priority order
            }
            r2 = r2.getNext();
        }
        if (specificRule != null && generalRule == null) {
            return specificRule;
        }
        if (specificRule == null && generalRule != null) {
            return generalRule;
        }
        if (specificRule != null && generalRule != null) {
            if (specificRule.getPrecedence() > generalRule.getPrecedence() ||
                    (specificRule.getPrecedence() == generalRule.getPrecedence() &&
                    specificRule.getPriority() >= generalRule.getPriority())) {
                return specificRule;
            } else {
                return generalRule;
            }
        }
        return null;
    }

    /**
     * Get the rule corresponding to a given Node, by finding the next-best Pattern match
     * after the specified object.
     *
     * @param node the NodeInfo referring to the node to be matched
     * @return the object (e.g. a NodeHandler) registered for that element, if any (otherwise null).
     */

    public Rule getNextMatchRule(NodeInfo node, Rule currentRule, XPathContext context) throws XPathException {
        int fingerprint = node.getFingerprint();
        int type = node.getNodeKind();
        int key = getList(fingerprint, type);
        int policy = context.getController().getRecoveryPolicy();

        context = perhapsMakeNewContext(context);

        // First find the Rule object corresponding to the current handler

        Rule specificRule = null;
        Rule generalRule = null;
        int specificPrecedence = -1;
        double specificPriority = Double.NEGATIVE_INFINITY;

        // search the specific list for this node type / node name

        Rule r;
        if (key != Type.NODE) {
            r = ruleDict[key];
            while (r != null) {
                // skip this rule if it is the current rule.
                if (r == currentRule) {
                    // skip this rule
                } else
                // skip this rule unless it's "below" the current rule in search order
                    if ((r.getPrecedence() > currentRule.getPrecedence()) ||
                            (r.getPrecedence() == currentRule.getPrecedence() &&
                            (r.getPriority() > currentRule.getPriority() ||
                            (r.getPriority() == currentRule.getPriority() && r.getSequence() >= currentRule.getSequence())))) {
                        // skip this rule
                    } else {
                        // quit the search on finding the second (recoverable error) match
                        if (specificRule != null) {
                            if (r.getPrecedence() < specificPrecedence ||
                                    (r.getPrecedence() == specificPrecedence && r.getPriority() < specificPriority)) {
                                break;
                            }
                        }
                        //System.err.println("Testing " + Navigator.getPath(node) + " against " + r.pattern);
                        if (r.getPattern().matches(node, context)) {
                            //System.err.println("Matches");

                            // is this a second match?
                            if (specificRule != null) {
                                if (r.getPrecedence() == specificPrecedence && r.getPriority() == specificPriority) {
                                    reportAmbiguity(node, specificRule, r, context);
                                }
                                break;
                            }
                            specificRule = r;
                            specificPrecedence = r.getPrecedence();
                            specificPriority = r.getPriority();
                            if (policy == Configuration.RECOVER_SILENTLY) {
                                break;                      // find the first; they are in priority order
                            }
                        }
                    }
                r = r.getNext();
            }
        }

        // search the general list

        Rule r2 = ruleDict[Type.NODE];
        while (r2 != null) {
            // skip this rule if the rule is the current template rule
            if (r2 == currentRule) {
                // skip this rule
            } else
            // skip this rule unless it's "after" the current rule in search order
                if ((r2.getPrecedence() > currentRule.getPrecedence()) ||
                        (r2.getPrecedence() == currentRule.getPrecedence() &&
                        (r2.getPriority() > currentRule.getPriority() ||
                        (r2.getPriority() == currentRule.getPriority() && r2.getSequence() >= currentRule.getSequence())))) {
                    // skip this rule
                } else {
                    if (r2.getPrecedence() < specificPrecedence ||
                            (r2.getPrecedence() == specificPrecedence && r2.getPriority() < specificPriority)) {
                        break;      // no point in looking at a lower priority rule than the one we've got
                    }
                    if (r2.getPattern().matches(node, context)) {
                        // is it a second match?
                        if (generalRule != null) {
                            if (r2.getPrecedence() == generalRule.getPrecedence() &&
                                    r2.getPriority() == generalRule.getPriority()) {
                                reportAmbiguity(node, r2, generalRule, context);
                            }
                            break;
                        } else {
                            generalRule = r2;
                            if (policy == Configuration.RECOVER_SILENTLY) {
                                break;                      // find only the first; they are in priority order
                            }
                        }
                    }
                }
            r2 = r2.getNext();
        }

        if (specificRule != null && generalRule == null) {
            return specificRule;
        }
        if (specificRule == null && generalRule != null) {
            return generalRule;
        }
        if (specificRule != null && generalRule != null) {
            if (specificRule.getPrecedence() == generalRule.getPrecedence() &&
                    specificRule.getPriority() == generalRule.getPriority()) {
                // This situation is exceptional: we have a "specific" pattern and
                // a "general" pattern with the same priority. We have to select
                // the one that was added last
                // (Bug reported by Norman Walsh Jan 2002)
                Rule result = (specificRule.getSequence() > generalRule.getSequence() ?
                        specificRule :
                        generalRule);

                if (policy != Configuration.RECOVER_SILENTLY) {
                    reportAmbiguity(node, specificRule, generalRule, context);
                }
                return result;
            }
            if (specificRule.getPrecedence() > generalRule.getPrecedence() ||
                    (specificRule.getPrecedence() == generalRule.getPrecedence() &&
                    specificRule.getPriority() >= generalRule.getPriority())) {
                return specificRule;
            } else {
                return generalRule;
            }
        }
        return null;
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

        DynamicError err = new DynamicError("Ambiguous rule match for " + path + '\n' +
                "Matches both \"" + showPattern(pat1) + "\" on line " + pat1.getLineNumber() + " of " + pat1.getSystemId() +
                "\nand \"" + showPattern(pat2) + "\" on line " + pat2.getLineNumber() + " of " + pat2.getSystemId());
        err.setErrorCode(errorCode);
        err.setLocator(c.getOrigin().getInstructionInfo());
        c.getController().recoverableError(err);
    }

    private static String showPattern(Pattern p) {
        // Complex patterns can be laid out with lots of whitespace, which looks messy in the error message
        return Whitespace.collapseWhitespace(p.toString()).toString();
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
