package net.sf.saxon.trans;

import net.sf.saxon.pattern.Pattern;

import java.io.Serializable;

/**
 * Rule: a template rule, or a strip-space rule used to support the implementation
 */

public final class Rule implements Serializable {
    private Pattern pattern;        // The pattern that fires this rule
    private RuleTarget action;      // The action associated with this rule (usually a Template)
    private int precedence;         // The import precedence
    private double priority;        // The priority of the rule
    private Rule next;              // The next rule after this one in the chain of rules
    private int sequence;           // The relative position of this rule, its position in declaration order
    private boolean alwaysMatches;  // True if the pattern does not need to be tested, because the rule
                                    // is on a rule-chain such that the pattern is necessarily satisfied
    private int rank;               // Indicates the relative precedence/priority of a rule within a mode;
                                    // used for quick comparison

    public int getSequence() {
        return sequence;
    }

    public void setAction(RuleTarget action) {
        this.action = action;
    }

    public RuleTarget getAction() {
        return action;
    }

    public Rule getNext() {
        return next;
    }

    public void setNext(Rule next) {
        this.next = next;
    }

    public Pattern getPattern() {
        return pattern;
    }

    public int getPrecedence() {
        return precedence;
    }

    public double getPriority() {
        return priority;
    }

    public void setAlwaysMatches(boolean matches) {
        alwaysMatches = matches;
    }

    public boolean isAlwaysMatches() {
        return alwaysMatches;
    }

    public void setRank(int rank) {
        this.rank = rank;
    }

    public int getRank() {
        return rank;
    }

    /**
     * Create a Rule.
     *
     * @param p    the pattern that this rule matches
     * @param o    the object invoked by this rule (usually a Template)
     * @param prec the precedence of the rule
     * @param prio the priority of the rule
     * @param seq  a sequence number for ordering of rules
     */

    public Rule(Pattern p, RuleTarget o, int prec, double prio, int seq)  {
        pattern = p;
        action = o;
        precedence = prec;
        priority = prio;
        next = null;
        sequence = seq;
    }

    /**
     * Copy a rule, including the chain of rules linked to it
     * @param r the rule to be copied
     */

    public Rule(Rule r) {
        pattern = r.pattern;
        action = r.action;
        precedence = r.precedence;
        priority = r.priority;
        sequence = r.sequence;
        if (r.next == null) {
            next = null;
        } else {
            next = new Rule(r.next);
        }
    }

    /**
     * Rules have an ordering, based on their precedence and priority. This method compares
     * them using the precomputed rank value.
     * @param other Another rule whose ordering rank is to be compared with this one
     * @return <0 if this rule has lower rank, that is if it has lower precedence or equal
     * precedence and lower priority. 0 if the two rules have equal precedence and
     * priority. >0 if this rule has higher rank in precedence/priority order
     */

    public int compareRank(Rule other) {
        return rank - other.rank;
    }

    /**
     * Rules have an ordering, based on their precedence and priority.
     * @param other Another rule whose ordering rank is to be compared with this one
     * @return <0 if this rule has lower rank, that is if it has lower precedence or equal
     * precedence and lower priority. 0 if the two rules have equal precedence and
     * priority. >0 if this rule has higher rank in precedence/priority order
     */

   public int compareComputedRank(Rule other) {
        if (precedence == other.precedence) {
            if (priority == other.priority) {
                return 0;
            } else if (priority < other.priority) {
                return -1;
            } else {
                return +1;
            }
        } else if (precedence < other.precedence) {
            return -1;
        } else {
            return +1;
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
// The Original Code is: all this file.
//
// The Initial Developer of the Original Code is Michael H. Kay
//
// Portions created by (your name) are Copyright (C) (your legal entity). All Rights Reserved.
//
// Contributor(s): none.
//