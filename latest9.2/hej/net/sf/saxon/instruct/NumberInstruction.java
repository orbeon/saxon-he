package net.sf.saxon.instruct;

import net.sf.saxon.Configuration;
import net.sf.saxon.expr.*;
import net.sf.saxon.functions.NumberFn;
import net.sf.saxon.number.NumberFormatter;
import net.sf.saxon.number.Numberer;
import net.sf.saxon.om.Item;
import net.sf.saxon.om.Navigator;
import net.sf.saxon.om.NodeInfo;
import net.sf.saxon.om.SequenceIterator;
import net.sf.saxon.pattern.Pattern;
import net.sf.saxon.pattern.PatternSponsor;
import net.sf.saxon.trace.ExpressionPresenter;
import net.sf.saxon.trans.XPathException;
import net.sf.saxon.type.BuiltInAtomicType;
import net.sf.saxon.type.ItemType;
import net.sf.saxon.type.TypeHierarchy;
import net.sf.saxon.value.*;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

/**
 * An xsl:number element in the stylesheet. Although this is an XSLT instruction, it is compiled
 * into an expression, evaluated using xsl:value-of to create the resulting text node.<br>
 */

public class NumberInstruction extends Expression {

    private static final int SINGLE = 0;
    private static final int MULTI = 1;
    private static final int ANY = 2;
    private static final int SIMPLE = 3;

    private int level;
    private Pattern count = null;
    private Pattern from = null;
    private Expression select = null;
    private Expression value = null;
    private Expression format = null;
    private Expression groupSize = null;
    private Expression groupSeparator = null;
    private Expression letterValue = null;
    private Expression ordinal = null;
    private Expression lang = null;
    private NumberFormatter formatter = null;
    private Numberer numberer = null;
    private boolean hasVariablesInPatterns;
    private boolean backwardsCompatible;

    /**
     * Construct a NumberInstruction
     * @param config the Saxon configuration
     * @param select the expression supplied in the select attribute
     * @param level one of "single", "level", "multi"
     * @param count the pattern supplied in the count attribute
     * @param from the pattern supplied in the from attribute
     * @param value the expression supplied in the value attribute
     * @param format the expression supplied in the format attribute
     * @param groupSize the expression supplied in the group-size attribute
     * @param groupSeparator the expression supplied in the grouping-separator attribute
     * @param letterValue the expression supplied in the letter-value attribute
     * @param ordinal the expression supplied in the ordinal attribute
     * @param lang the expression supplied in the lang attribute
     * @param formatter A NumberFormatter to be used
     * @param numberer A Numberer to be used for localization
     * @param hasVariablesInPatterns true if one or more of the patterns contains variable references
     * @param backwardsCompatible true if running in 1.0 compatibility mode
     */

    public NumberInstruction(Configuration config,
                             Expression select,
                             int level,
                             Pattern count,
                             Pattern from,
                             Expression value,
                             Expression format,
                             Expression groupSize,
                             Expression groupSeparator,
                             Expression letterValue,
                             Expression ordinal,
                             Expression lang,
                             NumberFormatter formatter,
                             Numberer numberer,
                             boolean hasVariablesInPatterns,
                             boolean backwardsCompatible) {
        this.select = select;
        this.level = level;
        this.count = count;
        this.from = from;
        this.value = value;
        this.format = format;
        this.groupSize = groupSize;
        this.groupSeparator = groupSeparator;
        this.letterValue = letterValue;
        this.ordinal = ordinal;
        this.lang = lang;
        this.formatter = formatter;
        this.numberer = numberer;
        this.hasVariablesInPatterns = hasVariablesInPatterns;
        this.backwardsCompatible = backwardsCompatible;

        final TypeHierarchy th = config.getTypeHierarchy();
        if (this.value != null && !this.value.getItemType(th).isAtomicType()) {
            this.value = new Atomizer(this.value);
        }

        Iterator<Expression> kids = iterateSubExpressions();
        while (kids.hasNext()) {
            Expression child = kids.next();
            adoptChildExpression(child);
        }
    }

    public Expression simplify(ExpressionVisitor visitor) throws XPathException {
        select = visitor.simplify(select);
        value = visitor.simplify(value);
        format = visitor.simplify(format);
        groupSize = visitor.simplify(groupSize);
        groupSeparator = visitor.simplify(groupSeparator);
        letterValue = visitor.simplify(letterValue);
        ordinal = visitor.simplify(ordinal);
        lang = visitor.simplify(lang);
        if (count != null) {
            count = count.simplify(visitor);
        }
        if (from != null) {
            from = from.simplify(visitor);
        }
        return this;
    }

    /**
     * Perform static analysis of an expression and its subexpressions.
     *
     * <p>This checks statically that the operands of the expression have
     * the correct type; if necessary it generates code to do run-time type checking or type
     * conversion. A static type error is reported only if execution cannot possibly succeed, that
     * is, if a run-time type error is inevitable. The call may return a modified form of the expression.</p>
     *
     * <p>This method is called after all references to functions and variables have been resolved
     * to the declaration of the function or variable. However, the types of such functions and
     * variables will only be accurately known if they have been explicitly declared.</p>
     *
     * @param visitor an expression visitor
     * @param contextItemType the static type of "." at the point where this expression is invoked.
     * The parameter is set to null if it is known statically that the context item will be undefined.
     * If the type of the context item is not known statically, the argument is set to
     * {@link net.sf.saxon.type.Type#ITEM_TYPE}
     * @throws XPathException if an error is discovered during this phase
     *     (typically a type error)
     * @return the original expression, rewritten to perform necessary
     *     run-time type checks, and to perform other type-related
     *     optimizations
     */

    public Expression typeCheck(ExpressionVisitor visitor, ItemType contextItemType) throws XPathException {
        if (select != null) {
            select = visitor.typeCheck(select, contextItemType);
        } else {
            if (value==null) {
                // we are numbering the context node
                XPathException err = null;
                if (contextItemType == null) {
                    err = new XPathException(
                            "xsl:number requires a select attribute, a value attribute, or a context item");
                } else if (contextItemType.isAtomicType()) {
                    err = new XPathException(
                            "xsl:number requires the context item to be a node, but it is an atomic value");

                }
                if (err != null) {
                    err.setIsTypeError(true);
                    err.setErrorCode("XTTE0990");
                    err.setLocator(this);
                    throw err;
                }
            }
        }
        if (value != null) {
            value = visitor.typeCheck(value, contextItemType);
        }
        if (format != null) {
            format = visitor.typeCheck(format, contextItemType);
        }
        if (groupSize != null) {
            groupSize = visitor.typeCheck(groupSize, contextItemType);
        }
        if (groupSeparator != null) {
            groupSeparator = visitor.typeCheck(groupSeparator, contextItemType);
        }
        if (letterValue != null) {
            letterValue = visitor.typeCheck(letterValue, contextItemType);
        }
        if (ordinal != null) {
            ordinal = visitor.typeCheck(ordinal, contextItemType);
        }
        if (lang != null) {
            lang = visitor.typeCheck(lang, contextItemType);
        }
        if (count != null) {
            visitor.typeCheck(new PatternSponsor(count), contextItemType);
        }
        if (from != null) {
            visitor.typeCheck(new PatternSponsor(from), contextItemType);
        }
        return this;
    }

    /**
     * Perform optimisation of an expression and its subexpressions.
     * <p/>
     * <p>This method is called after all references to functions and variables have been resolved
     * to the declaration of the function or variable, and after all type checking has been done.</p>
     *
     * @param visitor an expression visitor
     * @param contextItemType the static type of "." at the point where this expression is invoked.
     *                        The parameter is set to null if it is known statically that the context item will be undefined.
     *                        If the type of the context item is not known statically, the argument is set to
     *                        {@link net.sf.saxon.type.Type#ITEM_TYPE}
     * @return the original expression, rewritten if appropriate to optimize execution
     * @throws XPathException if an error is discovered during this phase
     *                                        (typically a type error)
     */

    public Expression optimize(ExpressionVisitor visitor, ItemType contextItemType) throws XPathException {
        if (select != null) {
            select = visitor.optimize(select, contextItemType);
        }
        if (value != null) {
            value = visitor.optimize(value, contextItemType);
        }
        if (format != null) {
            format = visitor.optimize(format, contextItemType);
        }
        if (groupSize != null) {
            groupSize = visitor.optimize(groupSize, contextItemType);
        }
        if (groupSeparator != null) {
            groupSeparator = visitor.optimize(groupSeparator, contextItemType);
        }
        if (letterValue != null) {
            letterValue = visitor.optimize(letterValue, contextItemType);
        }
        if (ordinal != null) {
            ordinal = visitor.optimize(ordinal, contextItemType);
        }
        if (lang != null) {
            lang = visitor.optimize(lang, contextItemType);
        }
        return this;
    }

    /**
     * Get the immediate sub-expressions of this expression. Default implementation
     * returns a zero-length array, appropriate for an expression that has no
     * sub-expressions.
     * @return an iterator containing the sub-expressions of this expression
     */

    public Iterator<Expression> iterateSubExpressions() {
        List<Expression> sub = new ArrayList<Expression>(9);
        if (select != null) {
            sub.add(select);
        }
        if (value != null) {
            sub.add(value);
        }
        if (format != null) {
            sub.add(format);
        }
        if (groupSize != null) {
            sub.add(groupSize);
        }
        if (groupSeparator != null) {
            sub.add(groupSeparator);
        }
        if (letterValue != null) {
            sub.add(letterValue);
        }
        if (ordinal != null) {
            sub.add(ordinal);
        }
        if (lang != null) {
            sub.add(lang);
        }
        if (count != null) {
            sub.add(new PatternSponsor(count));
        }
        if (from != null) {
            sub.add(new PatternSponsor(from));
        }
        return sub.iterator();
    }


    /**
     * Copy an expression. This makes a deep copy.
     *
     * @return the copy of the original expression
     */

    public Expression copy() {
        throw new UnsupportedOperationException("copy");
    }

    /**
     * Replace one subexpression by a replacement subexpression
     * @param original the original subexpression
     * @param replacement the replacement subexpression
     * @return true if the original subexpression is found
     */

    public boolean replaceSubExpression(Expression original, Expression replacement) {
        boolean found = false;
        if (select == original) {
            select = replacement;
            found = true;
        }
        if (value == original) {
            value = replacement;
            found = true;
        }
        if (format == original) {
            format = replacement;
            found = true;
        }
        if (groupSize == original) {
            groupSize = replacement;
            found = true;
        }
        if (groupSeparator == original) {
            groupSeparator = replacement;
            found = true;
        }
        if (letterValue == original) {
            letterValue = replacement;
            found = true;
        }
        if (ordinal == original) {
            ordinal = replacement;
            found = true;
        }
        if (lang == original) {
            lang = replacement;
            found = true;
        }
                return found;
    }


    /**
     * Determine the intrinsic dependencies of an expression, that is, those which are not derived
     * from the dependencies of its subexpressions. For example, position() has an intrinsic dependency
     * on the context position, while (position()+1) does not. The default implementation
     * of the method returns 0, indicating "no dependencies".
     *
     * @return a set of bit-significant flags identifying the "intrinsic"
     *         dependencies. The flags are documented in class net.sf.saxon.value.StaticProperty
     */

    public int getIntrinsicDependencies() {
        return (select == null ? StaticProperty.DEPENDS_ON_CONTEXT_ITEM : 0);
    }

    public ItemType getItemType(TypeHierarchy th) {
        return BuiltInAtomicType.STRING;
    }

    public int computeCardinality() {
        return StaticProperty.EXACTLY_ONE;
    }

    /**
     * Offer promotion for this subexpression. The offer will be accepted if the subexpression
     * is not dependent on the factors (e.g. the context item) identified in the PromotionOffer.
     * By default the offer is not accepted - this is appropriate in the case of simple expressions
     * such as constant values and variable references where promotion would give no performance
     * advantage. This method is always called at compile time.
     *
     * @param offer details of the offer, for example the offer to move
     *              expressions that don't depend on the context to an outer level in
     *              the containing expression
     * @param parent
     * @return if the offer is not accepted, return this expression unchanged.
     *         Otherwise return the result of rewriting the expression to promote
     *         this subexpression
     * @throws net.sf.saxon.trans.XPathException
     *          if any error is detected
     */

    public Expression promote(PromotionOffer offer, Expression parent) throws XPathException {
        Expression exp = offer.accept(parent, this);
        if (exp!=null) {
            return exp;
        } else {
            if (select != null) {
                select = doPromotion(this, select, offer);
            }
            if (value != null) {
                value = doPromotion(this, value, offer);
            }
            if (format != null) {
                format = doPromotion(this, format, offer);
            }
            if (groupSize != null) {
                groupSize = doPromotion(this, groupSize, offer);
            }
            if (groupSeparator != null) {
                groupSeparator = doPromotion(this, groupSeparator, offer);
            }
            if (letterValue != null) {
                letterValue = doPromotion(this, letterValue, offer);
            }
            if (ordinal != null) {
                ordinal = doPromotion(this, ordinal, offer);
            }
            if (lang != null) {
                lang = doPromotion(this, lang, offer);
            }
            if (count != null) {
                count.promote(offer, this);
            }
            if (from != null) {
                from.promote(offer, this);
            }
            return this;
        }
    }

    public Item evaluateItem(XPathContext context) throws XPathException {
        long value = -1;
        List vec = null;    // a list whose items may be of type either Long or
                            // BigInteger or the string to be output (e.g. "NaN")

        if (this.value != null) {

            SequenceIterator iter = this.value.iterate(context);
            vec = new ArrayList(4);
            while (true) {
                AtomicValue val = (AtomicValue) iter.next();
                if (val == null) {
                    break;
                }
                if (backwardsCompatible && !vec.isEmpty()) {
                    break;
                }
                try {
                    NumericValue num;
                    if (val instanceof NumericValue) {
                        num = (NumericValue) val;
                    } else {
                        num = NumberFn.convert(val);
                    }
                    if (num.isNaN()) {
                        throw new XPathException("NaN");  // thrown to be caught
                    }
                    num = num.round();
                    if (num.compareTo(Int64Value.MAX_LONG) > 0) {
                        vec.add(((BigIntegerValue)num.convert(BuiltInAtomicType.INTEGER, true, context).asAtomic()).asBigInteger());
                    } else {
                        if (num.compareTo(Int64Value.ZERO) < 0) {
                            throw new XPathException("The numbers to be formatted must not be negative");
                            // thrown to be caught
                        }
                        long i = ((NumericValue)num.convert(BuiltInAtomicType.INTEGER, true, context).asAtomic()).longValue();
                        vec.add(new Long(i));
                    }
                } catch (XPathException err) {
                    if (backwardsCompatible) {
                        vec.add("NaN");
                    } else {
                        vec.add(val.getStringValue());
                        XPathException e = new XPathException("Cannot convert supplied value to an integer. " + err.getMessage());
                        e.setErrorCode("XTDE0980");
                        e.setXPathContext(context);
                        throw e;
                    }
                }
            }
            if (backwardsCompatible && vec.isEmpty()) {
                vec.add("NaN");
            }
        } else {
            NodeInfo source;
            if (select != null) {
                source = (NodeInfo) select.evaluateItem(context);
            } else {
                Item item = context.getContextItem();
                if (!(item instanceof NodeInfo)) {
                    XPathException err = new XPathException("context item for xsl:number must be a node");
                    err.setErrorCode("XTTE0990");
                    err.setIsTypeError(true);
                    err.setXPathContext(context);
                    throw err;
                }
                source = (NodeInfo) item;
            }

            if (level == SIMPLE) {
                value = Navigator.getNumberSimple(source, context);
            } else if (level == SINGLE) {
                value = Navigator.getNumberSingle(source, count, from, context);
                if (value == 0) {
                    vec = Collections.EMPTY_LIST; 	// an empty list
                }
            } else if (level == ANY) {
                value = Navigator.getNumberAny(this, source, count, from, context, hasVariablesInPatterns);
                if (value == 0) {
                    vec = Collections.EMPTY_LIST; 	// an empty list
                }
            } else if (level == MULTI) {
                vec = Navigator.getNumberMulti(source, count, from, context);
            }
        }

        int gpsize = 0;
        String gpseparator = "";
        String letterVal;
        String ordinalVal = null;

        if (groupSize != null) {
            String g = groupSize.evaluateAsString(context).toString();
            try {
                gpsize = Integer.parseInt(g);
            } catch (NumberFormatException err) {
                XPathException e = new XPathException("grouping-size must be numeric");
                e.setXPathContext(context);
                e.setErrorCode("XTDE0030");
                throw e;
            }
        }

        if (groupSeparator != null) {
            gpseparator = groupSeparator.evaluateAsString(context).toString();
        }

        if (ordinal != null) {
            ordinalVal = ordinal.evaluateAsString(context).toString();
        }

        // fast path for the simple case

        if (vec == null && format == null && gpsize == 0 && lang == null) {
            return new StringValue("" + value);
        }

        // Use the numberer decided at compile time if possible; otherwise try to get it from
        // a table of numberers indexed by language; if not there, load the relevant class and
        // add it to the table.
        Numberer numb = numberer;
        if (numb == null) {
            String language = lang.evaluateAsString(context).toString();
            numb = context.getConfiguration().makeNumberer(language, null);
        }

        if (letterValue == null) {
            letterVal = "";
        } else {
            letterVal = letterValue.evaluateAsString(context).toString();
            if (!("alphabetic".equals(letterVal) || "traditional".equals(letterVal))) {
                XPathException e = new XPathException("letter-value must be \"traditional\" or \"alphabetic\"");
                e.setXPathContext(context);
                e.setErrorCode("XTDE0030");
                throw e;
            }
        }

        if (vec == null) {
            vec = new ArrayList(1);
            vec.add(new Long(value));
        }

        NumberFormatter nf;
        if (formatter == null) {              // format not known until run-time
            nf = new NumberFormatter();
            nf.prepare(format.evaluateAsString(context).toString());
        } else {
            nf = formatter;
        }

        CharSequence s = nf.format(vec, gpsize, gpseparator, letterVal, ordinalVal, numb);
        return new StringValue(s);
    }

    /**
     * Diagnostic print of expression structure. The abstract expression tree
     * is written to the supplied output destination.
     */

    public void explain(ExpressionPresenter out) {
        out.startElement("xslNumber");
        out.emitAttribute("level", (level==ANY ? "any" : level==SINGLE ? "single" : "multi"));
        if (count != null) {
            out.emitAttribute("count", count.toString());
        }
        if (from != null) {
            out.emitAttribute("from", from.toString());
        }
        if (select != null) {
            out.startSubsidiaryElement("select");
            select.explain(out);
            out.endSubsidiaryElement();
        }
        if (value != null) {
            out.startSubsidiaryElement("value");
            value.explain(out);
            out.endSubsidiaryElement();
        }
        if (format != null) {
            out.startSubsidiaryElement("format");
            format.explain(out);
            out.endSubsidiaryElement();
        }
        out.endElement();
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
