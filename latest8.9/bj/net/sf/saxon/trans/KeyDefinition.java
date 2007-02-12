package net.sf.saxon.trans;
import net.sf.saxon.Configuration;
import net.sf.saxon.type.BuiltInAtomicType;
import net.sf.saxon.expr.Expression;
import net.sf.saxon.expr.SequenceIterable;
import net.sf.saxon.instruct.InstructionDetails;
import net.sf.saxon.instruct.Procedure;
import net.sf.saxon.instruct.SlotManager;
import net.sf.saxon.pattern.PatternFinder;
import net.sf.saxon.sort.StringCollator;
import net.sf.saxon.om.StandardNames;
import net.sf.saxon.trace.InstructionInfo;
import net.sf.saxon.trace.InstructionInfoProvider;

import java.io.Serializable;

/**
  * Corresponds to a single xsl:key declaration.<P>
  * @author Michael H. Kay
  */

public class KeyDefinition extends Procedure implements Serializable, InstructionInfoProvider {

    private PatternFinder match;          // the match pattern
    private SequenceIterable use;
    private BuiltInAtomicType useType;    // the type of the values returned by the atomized use expression
    private StringCollator collation;     // the collating sequence, when type=string
    private String collationName;         // the collation URI
    private boolean backwardsCompatible = false;
    private boolean strictComparison = false;
    private boolean convertUntypedToOther = false;

    /**
    * Constructor to create a key definition
     * @param match the pattern in the xsl:key match attribute
     * @param use the expression in the xsl:key use attribute, or the expression that results from compiling
     * the xsl:key contained instructions. Note that a KeyDefinition constructed by the XSLT or XQuery parser will
     * always use an Expression here; however, a KeyDefinition constructed at run-time by a compiled stylesheet
     * or XQuery might use a simple ExpressionEvaluator that lacks all the compile-time information associated
     * with an Expression
     * @param collationName the name of the collation being used
     * @param collation the actual collation. This must be one that supports generation of collation keys.
    */

    public KeyDefinition(PatternFinder match, SequenceIterable use, String collationName, StringCollator collation) {
        setHostLanguage(Configuration.XSLT);
        this.match = match;
        this.use = use;
        if (use instanceof Expression) {
            setBody((Expression)use);
        }
        this.collation = collation;
        this.collationName = collationName;
    }

    /**
     * Set the primitive item type of the values returned by the use expression
     */

    public void setIndexedItemType(BuiltInAtomicType itemType) {
        this.useType = itemType;
    }

    /**
     * Get the primitive item type of the values returned by the use expression
     */

    public BuiltInAtomicType getIndexedItemType() {
        if (useType == null) {
            return BuiltInAtomicType.ANY_ATOMIC;
        } else {
            return useType;
        }
    }

    /**
     * Set backwards compatibility mode. The key definition is backwards compatible if ANY of the xsl:key
     * declarations has version="1.0" in scope.
     */

    public void setBackwardsCompatible(boolean bc) {
        backwardsCompatible = bc;
    }

    /**
     * Test backwards compatibility mode
     */

    public boolean isBackwardsCompatible() {
        return backwardsCompatible;
    }

    /**
     * Set whether strict comparison is needed. Strict comparison treats non-comparable values as an
     * error rather than a no-match. This is used for internal keys that support value comparisons in
     * Saxon-SA, it is not used for user-defined XSLT keys.
     */

    public void setStrictComparison(boolean strict) {
        strictComparison = strict;
    }

    /**
     * Get whether strict comparison is needed.
     */

    public boolean isStrictComparison() {
        return strictComparison;
    }

    /**
     * Indicate that untypedAtomic values should be converted to the type of the other operand,
     * rather than to strings. This is used for indexes constructed internally by Saxon-SA to
     * support filter expressions that use the "=" operator, as distinct from "eq".
     * @param convertToOther true if comparisons follow the semantics of the "=" operator rather than
     * the "eq" operator
     */

    public void setConvertUntypedToOther(boolean convertToOther) {
        this.convertUntypedToOther = convertToOther;
    }

    /**
     * Determine whether untypedAtomic values are converted to the type of the other operand.
     * @return true if comparisons follow the semantics of the "=" operator rather than
     * the "eq" operator
     */

    public boolean isConvertUntypedToOther() {
        return convertUntypedToOther;
    }

    /**
     * Set the map of local variables needed while evaluating the "use" expression
     */

    public void setStackFrameMap(SlotManager map) {
        if (map != null && map.getNumberOfVariables() > 0) {
            super.setStackFrameMap(map);
        }
    }

    /**
     * Set the system Id and line number of the source xsl:key definition
     */

    public void setLocation(String systemId, int lineNumber) {
        setSystemId(systemId);
        setLineNumber(lineNumber);
    }

    /**
    * Get the match pattern for the key definition
     * @return the pattern specified in the "match" attribute of the xsl:key declaration
    */

    public PatternFinder getMatch() {
        return match;
    }

    /**
    * Get the use expression for the key definition
     * @return the expression specified in the "use" attribute of the xsl:key declaration
    */

    public SequenceIterable getUse() {
        return use;
    }

    /**
    * Get the collation name for this key definition.
    * @return the collation name (the collation URI)
    */

    public String getCollationName() {
        return collationName;
    }

    /**
    * Get the collation.
     * @return the collation
    */

    public StringCollator getCollation() {
        return collation;
    }

    /**
     * Get the InstructionInfo details about the construct. This information isn't used for tracing,
     * but it is available when inspecting the context stack.
     */

    public InstructionInfo getInstructionInfo() {
        InstructionDetails details = new InstructionDetails();
        details.setConstructType(StandardNames.XSL_KEY);
        details.setSystemId(getSystemId());
        details.setLineNumber(getLineNumber());
        details.setProperty("key", this);
        return details;
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
// The Initial Developer of the Original Code is Michael H. Kay.
//
// Contributor(s):
//
