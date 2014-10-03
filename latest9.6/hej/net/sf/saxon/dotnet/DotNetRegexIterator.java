////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
// Copyright (c) 2014 Saxonica Limited.
// This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
// If a copy of the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
// This Source Code Form is "Incompatible With Secondary Licenses", as defined by the Mozilla Public License, v. 2.0.
////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

package net.sf.saxon.dotnet;

import cli.System.Collections.IEnumerator;
import cli.System.Text.RegularExpressions.Group;
import cli.System.Text.RegularExpressions.GroupCollection;
import cli.System.Text.RegularExpressions.Match;
import cli.System.Text.RegularExpressions.Regex;
import net.sf.saxon.event.Receiver;
import net.sf.saxon.expr.XPathContext;
import net.sf.saxon.z.IntHashMap;
import net.sf.saxon.z.IntToIntHashMap;
import net.sf.saxon.regex.RegexIterator;
import net.sf.saxon.om.Item;
import net.sf.saxon.om.SequenceIterator;
import net.sf.saxon.trans.XPathException;
import net.sf.saxon.tree.iter.ArrayIterator;
import net.sf.saxon.tree.iter.EmptyIterator;
import net.sf.saxon.tree.util.FastStringBuffer;
import net.sf.saxon.value.StringValue;

import java.util.ArrayList;
import java.util.List;

/**
 * Class JRegexIterator - provides an iterator over matched and unmatched substrings.
 * This implementation of RegexIterator uses the JDK regular expression engine.
 */

public class DotNetRegexIterator implements RegexIterator {

    private String theString;   // the input string being matched
    private Regex pattern;    // the regex against which the string is matched
    private IEnumerator matcher;    // the Matcher object that does the matching, and holds the state
    private Match match;        // the current match
    private String current;     // the string most recently returned by the iterator
    private String next;        // if the last string was a matching string, null; otherwise the next substring
    //        matched by the regex
    //private int position = 0;   // the value of XPath position()
    private int prevEnd = 0;    // the position in the input string of the end of the last match or non-match

    private IntToIntHashMap nestingTable = null;

    /**
     * Construct a RegexIterator. Note that the underlying matcher.find() method is called once
     * to obtain each matching substring. But the iterator also returns non-matching substrings
     * if these appear between the matching substrings.
     *
     * @param string  the string to be analysed
     * @param pattern the regular expression
     */

    public DotNetRegexIterator(String string, Regex pattern) {
        theString = string;
        this.pattern = pattern;
        matcher = pattern.Matches(string).GetEnumerator();
        next = null;
    }

    /**
     * Get the next item in the sequence
     *
     * @return the next item in the sequence
     */

    public Item next() {
        if (next == null && prevEnd >= 0) {
            // we've returned a match (or we're at the start), so find the next match
            if (matcher.MoveNext()) {
                match = (Match) matcher.get_Current();
                int start = match.get_Index();
                int end = match.get_Index() + match.get_Length();
                if (prevEnd == start) {
                    // there's no intervening non-matching string to return
                    next = null;
                    current = theString.substring(start, end);
                    prevEnd = end;
                } else {
                    // return the non-matching substring first
                    current = theString.substring(prevEnd, start);
                    next = theString.substring(start, end);
                }
            } else {
                // there are no more regex matches, we must return the final non-matching text if any
                if (prevEnd < theString.length()) {
                    current = theString.substring(prevEnd);
                    next = null;
                } else {
                    // this really is the end...
                    current = null;
                    prevEnd = -1;
                    return null;
                }
                prevEnd = -1;
            }
        } else {
            // we've returned a non-match, so now return the match that follows it, if there is one
            if (prevEnd >= 0) {
                current = next;
                next = null;
                prevEnd = match.get_Index() + match.get_Length();
            } else {
                current = null;
                return null;
            }
        }
        return StringValue.makeStringValue(current);
    }

    /**
     * Get the current item in the sequence
     *
     * @return the item most recently returned by next()
     */

    public Item current() {
        return StringValue.makeStringValue(current);
    }

    /**
     * Get another iterator over the same items
     *
     * @return a new iterator, positioned before the first item
     */

    public SequenceIterator getAnother() {
        return new DotNetRegexIterator(theString, pattern);
    }

    /**
     * Get properties of this iterator, as a bit-significant integer.
     *
     * @return the properties of this iterator.
     */

    public int getProperties() {
        return 0;
    }

    /**
     * Determine whether the current item is a matching item or a non-matching item
     *
     * @return true if the current item (the one most recently returned by next()) is
     *         an item that matches the regular expression, or false if it is an item that
     *         does not match
     */

    public boolean isMatching() {
        return next == null && prevEnd >= 0;
    }

    /**
     * Get a substring that matches a parenthesised group within the regular expression
     *
     * @param number the number of the group to be obtained
     * @return the substring of the current item that matches the n'th parenthesized group
     *         within the regular expression
     */

    public String getRegexGroup(int number) {
        if (!isMatching()) return null;
        GroupCollection groups = match.get_Groups();
        if (number > groups.get_Count() || number < 0) return "";
        String s = groups.get_Item(number).get_Value();
        if (s == null) return "";
        return s;
    }

    /**
     * Get a sequence containing all the regex groups (except group 0, because we want to use indexing from 1).
     * This is used by the saxon:analyze-string() higher-order extension function.
     */

    public SequenceIterator getRegexGroupIterator() {
        //System.err.println("getRegexGroupIterator");
        if (!isMatching()) {
            //System.err.println("no match");
            return null;
        }
        GroupCollection groups = match.get_Groups();
        int c = groups.get_Count();
        //System.err.println("groups: " + c);
        if (c == 0) {
            return EmptyIterator.getInstance();
        } else {
            StringValue[] groupArray = new StringValue[c - 1];
            IEnumerator e = groups.GetEnumerator();
            int i = 0;
            // we're not interested in group 0
            e.MoveNext();
            e.get_Current();
            while (e.MoveNext()) {
                Group g = (Group) e.get_Current();
                //System.err.println("group: " + i + " " + g.get_Value());
                groupArray[i++] = StringValue.makeStringValue(g.get_Value());
            }
            return new ArrayIterator(groupArray);
        }
    }

    /**
     * Process a matching substring, performing specified actions at the start and end of each captured
     * subgroup. This method will always be called when operating in "push" mode; it writes its
     * result to context.getReceiver(). The matching substring text is all written to the receiver,
     * interspersed with calls to the methods onGroupStart() and onGroupEnd().
     *
     * @param context the dynamic evaluation context
     * @param action  defines the processing to be performed at the start and end of a group
     */

    public void processMatchingSubstring(XPathContext context, OnGroup action) throws XPathException {
        Receiver out = context.getReceiver();
        GroupCollection groups = match.get_Groups();
        int c = groups.get_Count();
        if (c == 0) {
            out.characters(current, 0, 0);
        } else {
            // Create a map from positions in the string to lists of actions.
            // The "actions" in each list are: +N: start group N; -N: end group N.
            IntHashMap<List<Integer>> actions = new IntHashMap<List<Integer>>(c);

            StringValue[] groupArray = new StringValue[c - 1];
            IEnumerator en = groups.GetEnumerator();
            int i = 0;
            // we're not interested in group 0
            en.MoveNext();
            en.get_Current();
            while (en.MoveNext()) {
                i++;
                Group g = (Group) en.get_Current();
                int start = g.get_Index();
                int end = start + g.get_Length();
                if (start < end) {
                    // Add the start action after all other actions on the list for the same position
                    List<Integer> s = actions.get(start);
                    if (s == null) {
                        s = new ArrayList<Integer>(4);
                        actions.put(start, s);
                    }
                    s.add(i);
                    // Add the end action before all other actions on the list for the same position
                    List<Integer> e = actions.get(end);
                    if (e == null) {
                        e = new ArrayList<Integer>(4);
                        actions.put(end, e);
                    }
                    e.add(0, -i);
                } else {
                    // zero-length group (start==end). The problem here is that the information available
                    // from Java isn't sufficient to determine the nesting of groups: match("a", "(a(b?))")
                    // and match("a", "(a)(b?)") will both give the same result for group 2 (start=1, end=1).
                    // So we need to go back to the original regex to determine the group nesting
                    if (nestingTable == null) {
                        computeNestingTable();
                    }
                    int parentGroup = nestingTable.get(i);
                    // insert the start and end events immediately before the end event for the parent group,
                    // if present; otherwise after all existing events for this position
                    List<Integer> s = actions.get(start);
                    if (s == null) {
                        s = new ArrayList<Integer>(4);
                        actions.put(start, s);
                        s.add(i);
                        s.add(-i);
                    } else {
                        int pos = s.size();
                        for (int e = 0; e < s.size(); e++) {
                            if (s.get(e) == -parentGroup) {
                                pos = e;
                                break;
                            }
                        }
                        s.add(pos, -i);
                        s.add(pos, i);
                    }

                }


            }
            FastStringBuffer buff = new FastStringBuffer(current.length());
            for (int k = 0; k < current.length() + 1; k++) {
                List<Integer> events = actions.get(k);
                if (events != null) {
                    if (buff.length() > 0) {
                        out.characters(buff, 0, 0);
                        buff.setLength(0);
                    }
                    for (Integer group : events) {
                        if (group > 0) {
                            action.onGroupStart(context, group);
                        } else {
                            action.onGroupEnd(context, -group);
                        }
                    }
                }
                if (k < current.length()) {
                    buff.append(current.charAt(k));
                }
            }
            if (buff.length() > 0) {
                out.characters(buff, 0, 0);
            }
        }

    }

    public RegexIterator getSnapShot(XPathContext context) throws XPathException {
        DotNetRegexIterator regexItr = new DotNetRegexIterator(theString, pattern);
        regexItr.current = this.current;
        regexItr.nestingTable = this.nestingTable;
        return regexItr;
    }

    /**
     * Compute a table showing for each captured group number (opening paren in the regex),
     * the number of its parent group. This is done by reparsing the source of the regular
     * expression. This is needed when the result of a match includes an empty group, to determine
     * its position relative to other groups finishing at the same character position.
     */

    private void computeNestingTable() {
        nestingTable = new IntToIntHashMap(16);
        String s = theString;
        int[] stack = new int[s.length()];
        int tos = 0;
        int group = 1;
        int inBrackets = 0;
        stack[tos++] = 0;
        for (int i = 0; i < s.length(); i++) {
            char ch = s.charAt(i);
            if (ch == '\'') {
                i++;
            } else if (ch == '[') {
                inBrackets++;
            } else if (ch == ']') {
                inBrackets--;
            } else if (ch == '(' && s.charAt(i + 1) != '?' && inBrackets == 0) {
                nestingTable.put(group, stack[tos - 1]);
                stack[tos++] = group++;
            } else if (ch == ')' && inBrackets == 0) {
                tos--;
            }
        }
    }

    /**
     * Close the iterator. This indicates to the supplier of the data that the client
     * does not require any more items to be delivered by the iterator. This may enable the
     * supplier to release resources. After calling close(), no further calls on the
     * iterator should be made; if further calls are made, the effect of such calls is undefined.
     * <p/>
     * <p>(Currently, closing an iterator is important only when the data is being "pushed" in
     * another thread. Closing the iterator terminates that thread and means that it needs to do
     * no additional work. Indeed, failing to close the iterator may cause the push thread to hang
     * waiting for the buffer to be emptied.)</p>
     *
     * @since 9.1
     */
    public void close() {
        //
    }
}

