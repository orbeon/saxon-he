////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
// Copyright (c) 2014 Saxonica Limited.
// This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
// If a copy of the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
// This Source Code Form is "Incompatible With Secondary Licenses", as defined by the Mozilla Public License, v. 2.0.
////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

package net.sf.saxon.regex;


import net.sf.saxon.expr.sort.EmptyIntIterator;
import net.sf.saxon.tree.util.FastStringBuffer;
import net.sf.saxon.z.*;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Stack;

/**
 * Represents an operation or instruction in the regular expression program. The class Operation
 * is abstract, and has concrete subclasses for each kind of operation/instruction
 */
public abstract class Operation {

    /**
     * Get an iterator returning all the matches for this operation
     *
     * @param matcher  supplies the context for the matching; may be updated with information about
     *                 captured groups
     * @param position the start position to seek a match
     * @return an iterator returning the endpoints of all matches starting at the supplied position
     */

    public abstract IntIterator iterateMatches(REMatcher matcher, int position);

    /**
     * Get the length of the matches returned by this operation if they are fixed-length
     *
     * @return the length of the matches, or -1 if the length is variable
     */

    public int getMatchLength() {
        return -1;
    }

    /**
     * Get the minimum length of the matches returned by this operation
     * @return the length of the shortest string that will match the operation
     */

    public int getMinimumMatchLength() {
        int fixed = getMatchLength();
        return fixed < 0 ? 0 : fixed;
    }

    /**
     * Ask whether the regular expression is capable of matching a zero-length string
     * @return true if the regex matches a zero-length string
     */

    public abstract boolean matchesEmptyString();

    /**
     * Optimize the operation
     * @return the optimized operation
     * @param program the program being optimized
     * @param flags the regular expression flags
     */

    public Operation optimize(REProgram program, REFlags flags) {
        return this;
    }

    /**
     * Display the operation as a regular expression, possibly in abbreviated form
     * @return the operation in a form that is recognizable as a regular expression or abbreviated
     * regular expression
     */

    public abstract String display();

    /**
     * A choice of several branches
     */

    public static class OpChoice extends Operation {

        List<Operation> branches;
        boolean mutuallyExclusive = false;

        public OpChoice(List<Operation> branches) {
            this.branches = branches;
        }

        @Override
        public int getMatchLength() {
            int fixed = branches.get(0).getMatchLength();
            for (int i = 1; i < branches.size(); i++) {
                if (branches.get(i).getMatchLength() != fixed) {
                    return -1;
                }
            }
            return fixed;
        }

        @Override
        public int getMinimumMatchLength() {
            int min = branches.get(0).getMinimumMatchLength();
            for (int i = 1; i < branches.size(); i++) {
                int m = branches.get(i).getMinimumMatchLength();
                if (m < min) {
                    min = m;
                }
            }
            return min;
        }

        @Override
        public boolean matchesEmptyString() {
            for (Operation branch : branches) {
                if (branch.matchesEmptyString()) {
                    return true;
                }
            }
            return false;
        }

        @Override
        public Operation optimize(REProgram program, REFlags flags) {
            for (int i=0; i<branches.size(); i++) {
                Operation o1 = branches.get(i);
                Operation o2 = o1.optimize(program, flags);
                if (o1 != o2) {
                    branches.set(i, o2);
                }
            }
            return this;
        }

        @Override
        public IntIterator iterateMatches(final REMatcher matcher, final int position) {
            return new IntIterator() {
                Iterator<Operation> branchIter = branches.iterator();
                IntIterator currentIter = null;
                Operation currentOp = null;

                public boolean hasNext() {
                    while (true) {
                        if (currentIter == null) {
                            if (branchIter.hasNext()) {
                                matcher.clearCapturedGroupsBeyond(position);
                                currentOp = branchIter.next();
                                currentIter = currentOp.iterateMatches(matcher, position);
                            } else {
                                return false;
                            }
                        }
                        if (currentIter.hasNext()) {
                            return true;
                        } else {
                            currentIter = null;
                            //continue;
                        }
                    }
                }

                public int next() {
                    return currentIter.next();
                }

            };
        }

        /**
         * Display the operation as a regular expression, possibly in abbreviated form
         *
         * @return the operation in a form that is recognizable as a regular expression or abbreviated
         *         regular expression
         */
        @Override
        public String display() {
            FastStringBuffer fsb = new FastStringBuffer(FastStringBuffer.SMALL);
            fsb.append("(?:");
            boolean first = true;
            for (Operation branch : branches) {
                if (first) {
                    first = false;
                } else {
                    fsb.append('|');
                }
                fsb.append(branch.display());
            }
            fsb.append(")");
            return fsb.toString();
        }
    }

    /**
     * A sequence of multiple pieces
     */

    public static class OpSequence extends Operation {
        private List<Operation> operations;

        public OpSequence(List<Operation> operations) {
            this.operations = operations;
        }

        public List<Operation> getOperations() {
            return operations;
        }

        @Override
        public int getMatchLength() {
            int len = 0;
            for (Operation o : operations) {
                int i = o.getMatchLength();
                if (i == -1) {
                    return -1;
                }
                len += i;
            }
            return len;
        }

        @Override
        public int getMinimumMatchLength() {
            int len = 0;
            for (Operation o : operations) {
                len += o.getMinimumMatchLength();
            }
            return len;
        }

        @Override
        public boolean matchesEmptyString() {
            for (Operation o : operations) {
                if (!o.matchesEmptyString()) {
                    return false;
                }
            }
            return true;
        }

        /**
         * Display the operation as a regular expression, possibly in abbreviated form
         *
         * @return the operation in a form that is recognizable as a regular expression or abbreviated
         *         regular expression
         */
        @Override
        public String display() {
            FastStringBuffer fsb = new FastStringBuffer(FastStringBuffer.SMALL);
            for (Operation op : operations) {
                fsb.append(op.display());
            }
            return fsb.toString();
        }

        @Override
        public Operation optimize(REProgram program, REFlags flags) {
            if (operations.size() == 0) {
                return new OpNothing();
            } else if (operations.size() == 1) {
                return operations.get(0);
            } else {
//                if (operations.get(0) instanceof OpBOL) {
//                    program.optimizationFlags |= REProgram.OPT_HASBOL;
//                }
                for (int i=0; i<operations.size()-1; i++) {
                    Operation o1 = operations.get(i);
                    Operation o2 = o1.optimize(program, flags);
                    if (o1 != o2) {
                        operations.set(i, o2);
                    }
                    if (o2 instanceof OpRepeat) {
                        Operation o1r = ((OpRepeat)o1).getRepeatedOperation();
                        if (o1r instanceof OpAtom || o1r instanceof OpCharClass) {
                            Operation o2r = operations.get(i+1);
                            if (((OpRepeat)o1).min == ((OpRepeat)o1).max ||
                                    RECompiler.noAmbiguity(o1r, o2r, flags.isCaseIndependent(), !((OpRepeat)o1).greedy)) {
                                operations.set(i, new OpUnambiguousRepeat(o1r, ((OpRepeat)o1).min, ((OpRepeat)o1).max));
                            }
                        }
                    }
                }
                return this;
            }


        }

        @Override
        public IntIterator iterateMatches(final REMatcher matcher, final int position) {

            // A stack of iterators, one for each piece in the sequence
            final Stack<IntIterator> iterators = new Stack<IntIterator>();

            return new IntIterator() {

                private boolean primed = false;
                private int nextPos;

                /**
                 * Advance the current iterator if possible, getting the first match for all subsequent
                 * iterators in the sequence. If we get all the way to the end of the sequence, return the
                 * position in the input string that we have reached. If we don't get all the way to the
                 * end of the sequence, work backwards getting the next match for each term in the sequence
                 * until we find a route through.
                 * @return if we find a match for the whole sequence, return the position in the input string
                 * at which the match ends. Otherwise return -1.
                 */

                private int advance() {
                    while (!iterators.isEmpty()) {
                        IntIterator top = iterators.peek();
                        while (top.hasNext()) {
                            int p = top.next();
                            matcher.clearCapturedGroupsBeyond(p);
                            int i = iterators.size();
                            if (i >= operations.size()) {
                                return p;
                            }
                            top = operations.get(i).iterateMatches(matcher, p);
                            iterators.push(top);
                        }
                        iterators.pop();
                    }
                    matcher.clearCapturedGroupsBeyond(position);
                    return -1;
                }


                public boolean hasNext() {
                    if (!primed) {
                        iterators.push(operations.get(0).iterateMatches(matcher, position));
                        primed = true;
                    }
                    nextPos = advance();
                    return nextPos >= 0;
                }

                public int next() {
                    return nextPos;
                }
            };
        }

    }

    /**
     * A match of a single character in the input against a set of permitted characters
     */

    public static class OpCharClass extends Operation {
        private IntPredicate predicate;

        public OpCharClass(IntPredicate predicate) {
            this.predicate = predicate;
        }

        public IntPredicate getPredicate() {
            return predicate;
        }

        @Override
        public int getMatchLength() {
            return 1;
        }

        @Override
        public boolean matchesEmptyString() {
            return false;
        }

        @Override
        public IntIterator iterateMatches(REMatcher matcher, int position) {
            UnicodeString in = matcher.search;
            if (position < in.uLength() && predicate.matches(in.uCharAt(position))) {
                return new IntSingletonIterator(position + 1);
            } else {
                return EmptyIntIterator.getInstance();
            }
        }

        /**
         * Display the operation as a regular expression, possibly in abbreviated form
         *
         * @return the operation in a form that is recognizable as a regular expression or abbreviated
         *         regular expression
         */
        @Override
        public String display() {
            if (predicate instanceof IntSetPredicate) {
                IntSet s = ((IntSetPredicate)predicate).getIntSet();
                if (s instanceof IntSingletonSet) {
                    return "" + (char)((IntSingletonSet)s).getMember();
                } else if (s instanceof IntRangeSet) {
                    FastStringBuffer fsb = new FastStringBuffer(FastStringBuffer.SMALL);
                    IntRangeSet irs = (IntRangeSet)s;
                    fsb.append("[");
                    for (int i=0; i<irs.getNumberOfRanges(); i++) {
                        fsb.append((char)irs.getStartPoints()[1]);
                        fsb.append("-");
                        fsb.append((char)irs.getEndPoints()[1]);
                    }
                    fsb.append("[");
                    return fsb.toString();
                } else {
                    return "[....]";
                }
            } else {
                return "[....]";
            }
        }
    }

    /**
     * A match against a fixed string of any length
     */

    public static class OpAtom extends Operation {
        private UnicodeString atom;
        private int len;

        public OpAtom(UnicodeString atom) {
            this.atom = atom;
            this.len = atom.uLength();
        }

        public UnicodeString getAtom() {
            return atom;
        }

        @Override
        public int getMatchLength() {
            return len;
        }

        @Override
        public boolean matchesEmptyString() {
            return len == 0;
        }

        @Override
        public IntIterator iterateMatches(REMatcher matcher, int position) {
            UnicodeString in = matcher.search;
            if (position + len > in.uLength()) {
                return EmptyIntIterator.getInstance();
            }
            if (matcher.program.flags.isCaseIndependent()) {
                for (int i = 0; i < len; i++) {
                    if (!matcher.equalCaseBlind(in.uCharAt(position + i), atom.uCharAt(i))) {
                        return EmptyIntIterator.getInstance();
                    }
                }
            } else {
                for (int i = 0; i < len; i++) {
                    if (in.uCharAt(position + i) != atom.uCharAt(i)) {
                        return EmptyIntIterator.getInstance();
                    }
                }
            }
            return new IntSingletonIterator(position + len);
        }

        public String display() {
            return atom.toString();
        }
    }

    /**
     * Handle a greedy repetition (with possible min and max) where the
     * size of the repeated unit is fixed.
     */

    public static class OpGreedyFixed extends OpRepeat {
        private int len;

        public OpGreedyFixed(Operation op, int min, int max, int len) {
            super(op, min, max, true);
            this.len = len;
        }

        @Override
        public int getMatchLength() {
            return min == max ? min * len : -1;
        }

        @Override
        public boolean matchesEmptyString() {
            return op.matchesEmptyString() || min == 0;
        }

        @Override
        public Operation optimize(REProgram program, REFlags flags) {
            if (max == 0) {
                return new OpNothing();
            }
            if (op.getMatchLength() == 0) {
                return op;
            }
            op = op.optimize(program, flags);
            return this;
        }

        @Override
        public IntIterator iterateMatches(REMatcher matcher, int position) {
            int guard = matcher.search.uLength();
            if (max < Integer.MAX_VALUE) {
                guard = java.lang.Math.min(guard, position + len * max);
            }
            if (position >= guard && min > 0) {
                return EmptyIntIterator.getInstance();
            }

            int p = position;
            int matches = 0;
            while (p <= guard) {
                IntIterator it = op.iterateMatches(matcher, p);
                boolean matched = false;
                while (it.hasNext()) {
                    // although we know the length of the matches, we have to examine them all because
                    // they might capture subgroups
                    matched = true;
                    it.next();
                }
                if (matched) {
                    matches++;
                    p += len;
                    if (matches == max) {
                        break;
                    }
                } else {
                    break;
                }
            }
            if (matches < min) {
                return EmptyIntIterator.getInstance();
            }

            return new IntStepIterator(p, -len, position + len * min);
        }
    }

    /**
     * Handle a repetition where there is no ambiguity; if the repeated
     * term is matched in the string, then it cannot match anything other than
     * the repeated term. It is also used when the number of occurrences is
     * fixed. In this situation there will never be any need for
     * backtracking, so there is no need to keep any information to support
     * backtracking, and in addition, there is no distinction between greedy
     * and reluctant matching. This operation is used only for a repeated
     * atom or CharClass, which also means that if the repeated term matches
     * then it can only match in one way; a typical example is the term "A*"
     * in the regex "A*B".
     */

    public static class OpUnambiguousRepeat extends OpRepeat {

        public OpUnambiguousRepeat(Operation op, int min, int max) {
            super(op, min, max, true);
        }

        @Override
        public boolean matchesEmptyString() {
            return op.matchesEmptyString() || min == 0;
        }

        @Override
        public int getMatchLength() {
            if (op.getMatchLength() != -1 && min == max) {
                return op.getMatchLength()*min;
            } else {
                return -1;
            }
        }

        @Override
        public Operation optimize(REProgram program, REFlags flags) {
            op = op.optimize(program, flags);
            return this;
        }

        @Override
        public IntIterator iterateMatches(REMatcher matcher, int position) {
            int guard = matcher.search.uLength();

            int p = position;
            int matches = 0;
            while (matches < max && p <= guard) {
                IntIterator it = op.iterateMatches(matcher, p);
                if (it.hasNext()) {
                    matches++;
                    p = it.next();
                } else {
                    break;
                }
            }
            if (matches < min) {
                return EmptyIntIterator.getInstance();
            } else {
                return new IntSingletonIterator(p);
            }
        }
    }


    /**
     * Handle a greedy repetition (with possible min and max) where the
     * size of the repeated unit is variable.
     */

    public static class OpRepeat extends Operation {
        protected Operation op;
        protected int min;
        protected int max;
        private boolean greedy;

        public OpRepeat(Operation op, int min, int max, boolean greedy) {
            this.op = op;
            this.min = min;
            this.max = max;
            this.greedy = greedy;
        }

         /**
         * Get the operation being repeated
         * @return the child operation
         */

        public Operation getRepeatedOperation() {
            return op;
        }

        @Override
        public boolean matchesEmptyString() {
            return min == 0 || op.matchesEmptyString();
        }

        @Override
        public int getMatchLength() {
            return min == max && op.getMatchLength() >= 0 ? min * op.getMatchLength() : -1;
        }

        @Override
        public int getMinimumMatchLength() {
            return min * op.getMinimumMatchLength();
        }

        @Override
        public Operation optimize(REProgram program, REFlags flags) {
            op = op.optimize(program, flags);
            if (min == 0 && op.matchesEmptyString()) {
                // turns (a?)* into (a?)+
                min = 1;
            }
            return this;
        }

        @Override
        public IntIterator iterateMatches(final REMatcher matcher, int position) {
            final Stack<IntIterator> iterators = new Stack<IntIterator>();
            final Stack<Integer> positions = new Stack<Integer>();
            final int bound = Math.min(max, matcher.search.uLength() - position + 1);
            int p = position;
            if (greedy) {
                // Prime the arrays first with iterators up to the maximum length, stopping if there is no match
                if (min == 0 && !matcher.history.isDuplicateZeroLengthMatch(this, position)) {
                    // add a match at the current position if zero occurrences are allowed
                    iterators.push(new IntSingletonIterator(position));
                    positions.push(p);
                }
                for (int i=0; i<bound; i++) {
                    IntIterator it = op.iterateMatches(matcher, p);
                    if (it.hasNext()) {
                        p = it.next();
                        iterators.push(it);
                        positions.push(p);
                    } else if (iterators.isEmpty()) {
                        //if (min == 0) {
                        //    return new IntSingletonIterator(position);
                        //} else {
                            return EmptyIntIterator.getInstance();
                        //}
                    } else {
                        break;
                    }
                }
            } else {
                LinkedList<Integer> posSet = new LinkedList<Integer>();
                posSet.add(p);
                while (!posSet.isEmpty()) {
                    int q = posSet.getFirst();
                    IntIterator it = op.iterateMatches(matcher, q);
                    while (it.hasNext()) {
                        int q2 = it.next();
                        if (q2 != q) {
                            iterators.push(it);
                            positions.push(q2);
                            posSet.add(q2);
                        }
                    }
                    posSet.removeFirst();
                }
            }
            // Now return an iterator which returns all the matching positions in order
            IntIterator base = new IntIterator() {
                boolean primed = true;

                /**
                 * advance() moves to the next (potential) match position,
                 * ignoring constraints on the minimum number of occurrences
                 */

                private void advance() {
                    IntIterator top = iterators.peek();
                    if (top.hasNext()) {
                        int p = top.next();
                        positions.pop();
                        positions.push(p);
                        while (iterators.size() <= bound) {
                            IntIterator it = op.iterateMatches(matcher, p);
                            if (it.hasNext()) {
                                p = it.next();
                                iterators.push(it);
                                positions.push(p);
                            } else {
                                break;
                            }
                        }
                    } else {
                        iterators.pop();
                        positions.pop();
                    }
                }

                public boolean hasNext() {
                    if (primed && iterators.size() >= min) {
                        return !iterators.isEmpty();
                    } else if (iterators.isEmpty()) {
                        return false;
                    } else {
                        do {
                            advance();
                        } while (iterators.size() < min && !iterators.isEmpty());
                        return !iterators.isEmpty();
                    }
                }

                public int next() {
                    primed = false;
                    return positions.peek();
                }
            };
            return new ForceProgressIterator(base);
        }

        /**
         * Display the operation as a regular expression, possibly in abbreviated form
         *
         * @return the operation in a form that is recognizable as a regular expression or abbreviated
         *         regular expression
         */
        @Override
        public String display() {
            String quantifier;
            if (min==0 && max==Integer.MAX_VALUE) {
                quantifier = "*";
            } else if (min==1 && max==Integer.MAX_VALUE) {
                quantifier = "+";
            } else if (min==0 && max==1) {
                quantifier = "?";
            } else {
                quantifier = "{" + min + "," + max + "}";
            }
            if (!greedy) {
                quantifier += "?";
            }
            return op.display() + quantifier;
        }
    }

    /**
     * Handle a reluctant repetition (with possible min and max) where the
     * size of the repeated unit is fixed.
     */

    public static class OpReluctantFixed extends OpRepeat {
        private int len;

        public OpReluctantFixed(Operation op, int min, int max, int len) {
            super(op, min, max, false);
            this.len = len;
        }

        @Override
        public int getMatchLength() {
            return min == max ? min * len : -1;
        }

        @Override
        public boolean matchesEmptyString() {
            return op.matchesEmptyString() || min == 0;
        }

        @Override
        public Operation optimize(REProgram program, REFlags flags) {
            op = op.optimize(program, flags);
            return this;
        }

        @Override
        public IntIterator iterateMatches(final REMatcher matcher, final int position) {
            return new IntIterator() {
                private int pos = position;
                private int count = 0;
                private boolean started = false;

                public boolean hasNext() {
                    if (!started) {
                        started = true;
                        while (count < min) {
                            IntIterator child = op.iterateMatches(matcher, pos);
                            if (child.hasNext()) {
                                pos = child.next();
                                count++;
                            } else {
                                return false;
                            }
                        }
                        return true;
                    }
                    if (count < max) {
                        matcher.clearCapturedGroupsBeyond(pos);
                        IntIterator child = op.iterateMatches(matcher, pos);
                        if (child.hasNext()) {
                            pos = child.next();
                            count++;
                            return true;
                        }
                    }
                    return false;
                }

                public int next() {
                    return pos;
                }
            };
        }
    }


    /**
     * End of program
     */

    public static class OpEndProgram extends Operation {

        @Override
        public int getMatchLength() {
            return 0;
        }

        @Override
        public boolean matchesEmptyString() {
            return true;
        }

        @Override
        public IntIterator iterateMatches(final REMatcher matcher, final int position) {
            // An anchored match is successful only if we are at the end of the string.
            // Otherwise, match has succeeded unconditionally
            if (matcher.anchoredMatch) {
                if (matcher.search.isEnd(position)) {
                    return new IntSingletonIterator(position);
                } else {
                    return EmptyIntIterator.getInstance();
                }
            } else {
                matcher.setParenEnd(0, position);
                return new IntSingletonIterator(position);
            }
        }

        /**
         * Display the operation as a regular expression, possibly in abbreviated form
         *
         * @return the operation in a form that is recognizable as a regular expression or abbreviated
         *         regular expression
         */
        @Override
        public String display() {
            return "\\Z";
        }
    }

    /**
     * Beginning of Line (^)
     */

    public static class OpBOL extends Operation {

        @Override
        public int getMatchLength() {
            return 0;
        }

        @Override
        public boolean matchesEmptyString() {
            return true;
        }

        @Override
        public IntIterator iterateMatches(final REMatcher matcher, final int position) {
            // Fail if we're not at the start of the string
            if (position != 0) {
                // If we're multiline matching, we could still be at the start of a line
                if (matcher.program.flags.isMultiLine()) {
                    // Continue if at the start of a line
                    if (matcher.isNewline(position - 1) && !matcher.search.isEnd(position)) {
                        return new IntSingletonIterator(position);
                    }
                }
                return EmptyIntIterator.getInstance();
            }
            return new IntSingletonIterator(position);
        }

        /**
         * Display the operation as a regular expression, possibly in abbreviated form
         *
         * @return the operation in a form that is recognizable as a regular expression or abbreviated
         *         regular expression
         */
        @Override
        public String display() {
            return "^";
        }
    }

    /**
     * End of Line ($)
     */

    public static class OpEOL extends Operation {

        public int getMatchLength() {
            return 0;
        }

        @Override
        public boolean matchesEmptyString() {
            return true;
        }

        @Override
        public IntIterator iterateMatches(final REMatcher matcher, final int position) {
            // If we're not at the end of string

            UnicodeString search = matcher.search;
            if (matcher.program.flags.isMultiLine()) {
                if (search.isEnd(0) || search.isEnd(position) || matcher.isNewline(position)) {
                    return new IntSingletonIterator(position); //match successful
                } else {
                    return EmptyIntIterator.getInstance();
                }
            } else {
                // In spec bug 16809 we decided that '$' does not match a trailing newline when not in multiline mode
                if (search.isEnd(0) || search.isEnd(position)) {
                    return new IntSingletonIterator(position);
                } else {
                    return EmptyIntIterator.getInstance();
                }
            }
        }

        /**
         * Display the operation as a regular expression, possibly in abbreviated form
         *
         * @return the operation in a form that is recognizable as a regular expression or abbreviated
         *         regular expression
         */
        @Override
        public String display() {
            return "$";
        }
    }

    /**
     * Open paren (captured group)
     */

    public static class OpCapture extends Operation {

        public int groupNr;
        public Operation childOp;

        public OpCapture(Operation childOp, int group) {
            this.childOp = childOp;
            this.groupNr = group;
        }

        @Override
        public int getMatchLength() {
            return childOp.getMatchLength();
        }

        @Override
        public boolean matchesEmptyString() {
            return childOp.matchesEmptyString();
        }

        @Override
        public Operation optimize(REProgram program, REFlags flags) {
            childOp = childOp.optimize(program, flags);
            return this;
        }

        @Override
        public IntIterator iterateMatches(final REMatcher matcher, final int position) {
            if ((matcher.program.optimizationFlags & REProgram.OPT_HASBACKREFS) != 0) {
                matcher.startBackref[groupNr] = position;
            }
            final IntIterator base = childOp.iterateMatches(matcher, position);
            return new IntIterator() {
                public boolean hasNext() {
                    return base.hasNext();
                }

                public int next() {
                    int next = base.next();
                    // Increase valid paren count
                    if (groupNr >= matcher.parenCount) {
                        matcher.parenCount = groupNr + 1;
                    }

                    // Don't set paren if already set later on
                    //if (matcher.getParenStart(groupNr) == -1) {
                        matcher.setParenStart(groupNr, position);
                        matcher.setParenEnd(groupNr, next);
                    //}
                    if ((matcher.program.optimizationFlags & REProgram.OPT_HASBACKREFS) != 0) {
                        matcher.startBackref[groupNr] = position;
                        matcher.endBackref[groupNr] = next;
                    }
                    return next;
                }
            };
        }

        /**
         * Display the operation as a regular expression, possibly in abbreviated form
         *
         * @return the operation in a form that is recognizable as a regular expression or abbreviated
         *         regular expression
         */
        @Override
        public String display() {
            return "(" + childOp.display() + ")";
        }
    }

    /**
     * Back-reference
     */

    public static class OpBackReference extends Operation {

        public int groupNr;

        public OpBackReference(int groupNr) {
            this.groupNr = groupNr;
        }

        @Override
        public boolean matchesEmptyString() {
            return true;
        }

        @Override
        public IntIterator iterateMatches(final REMatcher matcher, final int position) {
            // Get the start and end of the backref
            int s = matcher.startBackref[groupNr];
            int e = matcher.endBackref[groupNr];

            // We don't know the backref yet
            if (s == -1 || e == -1) {
                return EmptyIntIterator.getInstance();
            }

            // The backref is empty size
            if (s == e) {
                return new IntSingletonIterator(position);
            }

            // Get the length of the backref
            int l = e - s;

            // If there's not enough input left, give up.
            UnicodeString search = matcher.search;
            if (search.isEnd(position + l - 1)) {
                return EmptyIntIterator.getInstance();
            }

            // Case fold the backref?
            if (matcher.program.flags.isCaseIndependent()) {
                // Compare backref to input
                for (int i = 0; i < l; i++) {
                    if (!matcher.equalCaseBlind(search.uCharAt(position + i), search.uCharAt(s + i))) {
                        return EmptyIntIterator.getInstance();
                    }
                }
            } else {
                // Compare backref to input
                for (int i = 0; i < l; i++) {
                    if (search.uCharAt(position + i) != search.uCharAt(s + i)) {
                        return EmptyIntIterator.getInstance();
                    }
                }
            }
            return new IntSingletonIterator(position + l);
        }

        /**
         * Display the operation as a regular expression, possibly in abbreviated form
         *
         * @return the operation in a form that is recognizable as a regular expression or abbreviated
         *         regular expression
         */
        @Override
        public String display() {
            return "\\" + groupNr;
        }
    }


    /**
     * Match empty string
     */

    public static class OpNothing extends Operation {

        public IntIterator iterateMatches(final REMatcher matcher, final int position) {
            return new IntSingletonIterator(position);
        }

        @Override
        public boolean matchesEmptyString() {
            return true;
        }

        @Override
        public int getMatchLength() {
            return 0;
        }

        /**
         * Display the operation as a regular expression, possibly in abbreviated form
         *
         * @return the operation in a form that is recognizable as a regular expression or abbreviated
         *         regular expression
         */
        @Override
        public String display() {
            return "()";
        }
    }

    /**
     * Operation that wraps a base operation and traces its execution
     */

    public static class OpTrace extends Operation {

        private Operation base;
        private static int counter = 0;

        public OpTrace(Operation base) {
            this.base = base;
        }

        @Override
        public IntIterator iterateMatches(REMatcher matcher, int position) {
            final IntIterator baseIter = base.iterateMatches(matcher, position);
            final int iterNr = counter++;
            System.err.println("Iterating over " + base.getClass().getSimpleName() + " " +
                    base.display() + " at position " + position + " returning " +
                    baseIter.getClass().getSimpleName() + " " + iterNr);
            return new IntIterator() {
                public boolean hasNext() {
                    boolean b = baseIter.hasNext();
                    System.err.println("IntIterator " + iterNr + " hasNext() = " + b);
                    return b;
                }

                public int next() {
                    int n = baseIter.next();
                    System.err.println("IntIterator " + iterNr + " next() = " + n);
                    return n;
                }
            };
        }

        @Override
        public int getMatchLength() {
            return base.getMatchLength();
        }

        @Override
        public boolean matchesEmptyString() {
            return base.matchesEmptyString();
        }

        @Override
        public Operation optimize(REProgram program, REFlags flags) {
            base = base.optimize(program, flags);
            return this;
        }

        /**
         * Display the operation as a regular expression, possibly in abbreviated form
         *
         * @return the operation in a form that is recognizable as a regular expression or abbreviated
         *         regular expression
         */
        @Override
        public String display() {
            return base.display();
        }
    }

    /**
     * The ForceProgressIterator is used to protect against non-termination; specifically,
     * iterators that return an infinite number of zero-length matches. After getting a
     * certain number of zero-length matches at the same position, hasNext() returns false.
     * (Potentially this gives problems with an expression such as (a?|b?|c?|d) that can
     * legitimately return more than one zero-length match).
     */

    private static class ForceProgressIterator implements IntIterator {
        private IntIterator base;
        int countZeroLength = 0;
        int currentPos = -1;

        public ForceProgressIterator(IntIterator base) {
            this.base = base;
        }

        public boolean hasNext() {
            return countZeroLength <= 3 && base.hasNext();
        }

        public int next() {
            int p = base.next();
            if (p == currentPos) {
                countZeroLength++;
            } else {
                countZeroLength = 0;
                currentPos = p;
            }
            return p;
        }
    }


}

