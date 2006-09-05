package net.sf.saxon.instruct;

import net.sf.saxon.Configuration;
import net.sf.saxon.expr.*;
import net.sf.saxon.functions.NumberFn;
import net.sf.saxon.number.NumberFormatter;
import net.sf.saxon.number.Numberer;
import net.sf.saxon.number.Numberer_en;
import net.sf.saxon.om.Item;
import net.sf.saxon.om.Navigator;
import net.sf.saxon.om.NodeInfo;
import net.sf.saxon.om.SequenceIterator;
import net.sf.saxon.pattern.Pattern;
import net.sf.saxon.pattern.PatternSponsor;
import net.sf.saxon.trans.DynamicError;
import net.sf.saxon.trans.StaticError;
import net.sf.saxon.trans.XPathException;
import net.sf.saxon.type.ItemType;
import net.sf.saxon.type.Type;
import net.sf.saxon.type.TypeHierarchy;
import net.sf.saxon.value.*;

import java.io.PrintStream;
import java.util.*;

/**
 * An xsl:number element in the stylesheet. Although this is an XSLT instruction, it is compiled
 * into an expression, evaluated using xsl:value-of to create the resulting text node.<br>
 */

public class NumberInstruction extends ComputedExpression {

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
    private HashMap nationalNumberers = null;
    private boolean hasVariablesInPatterns;
    private boolean backwardsCompatible;

    private static Numberer defaultNumberer = new Numberer_en();

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
            this.value = new Atomizer(this.value, config);
        }

        Iterator kids = iterateSubExpressions();
        while (kids.hasNext()) {
            Expression child = (Expression)kids.next();
            adoptChildExpression(child);
        }
    }

    public Expression simplify(StaticContext env) throws XPathException {
        if (select != null) {
            select = select.simplify(env);
        }
        if (value != null) {
            value = value.simplify(env);
        }
        if (format != null) {
            format = format.simplify(env);
        }
        if (groupSize != null) {
            groupSize = groupSize.simplify(env);
        }
        if (groupSeparator != null) {
            groupSeparator = groupSeparator.simplify(env);
        }
        if (letterValue != null) {
            letterValue = letterValue.simplify(env);
        }
        if (ordinal != null) {
            ordinal = ordinal.simplify(env);
        }
        if (lang != null) {
            lang = lang.simplify(env);
        }
        if (count != null) {
            count = count.simplify(env);
        }
        if (from != null) {
            from = from.simplify(env);
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
     * @param env the static context of the expression
     * @exception net.sf.saxon.trans.StaticError if an error is discovered during this phase
     *     (typically a type error)
     * @return the original expression, rewritten to perform necessary
     *     run-time type checks, and to perform other type-related
     *     optimizations
     */

    public Expression typeCheck(StaticContext env, ItemType contextItemType) throws XPathException {
        if (select != null) {
            select = select.typeCheck(env, contextItemType);
        } else {
            if (value==null) {
                // we are numbering the context node
                if (contextItemType.isAtomicType()) {
                    StaticError err = new StaticError("xsl:number requires the context item to be a node, but it is an atomic value");
                    err.setIsTypeError(true);
                    err.setErrorCode("XTTE0990");
                }
            }
        }
        if (value != null) {
            value = value.typeCheck(env, contextItemType);
        }
        if (format != null) {
            format = format.typeCheck(env, contextItemType);
        }
        if (groupSize != null) {
            groupSize = groupSize.typeCheck(env, contextItemType);
        }
        if (groupSeparator != null) {
            groupSeparator = groupSeparator.typeCheck(env, contextItemType);
        }
        if (letterValue != null) {
            letterValue = letterValue.typeCheck(env, contextItemType);
        }
        if (ordinal != null) {
            ordinal = ordinal.typeCheck(env, contextItemType);
        }
        if (lang != null) {
            lang = lang.typeCheck(env, contextItemType);
        }
        if (count != null) {
            count = count.analyze(env, contextItemType);
        }
        if (from != null) {
            from = from.analyze(env, contextItemType);
        }
        return this;
    }

    /**
     * Perform optimisation of an expression and its subexpressions.
     * <p/>
     * <p>This method is called after all references to functions and variables have been resolved
     * to the declaration of the function or variable, and after all type checking has been done.</p>
     *
     * @param opt             the optimizer in use. This provides access to supporting functions; it also allows
     *                        different optimization strategies to be used in different circumstances.
     * @param env             the static context of the expression
     * @param contextItemType the static type of "." at the point where this expression is invoked.
     *                        The parameter is set to null if it is known statically that the context item will be undefined.
     *                        If the type of the context item is not known statically, the argument is set to
     *                        {@link net.sf.saxon.type.Type#ITEM_TYPE}
     * @return the original expression, rewritten if appropriate to optimize execution
     * @throws net.sf.saxon.trans.StaticError if an error is discovered during this phase
     *                                        (typically a type error)
     */

    public Expression optimize(Optimizer opt, StaticContext env, ItemType contextItemType) throws XPathException {
        if (select != null) {
            select = select.optimize(opt, env, contextItemType);
        }
        if (value != null) {
            value = value.optimize(opt, env, contextItemType);
        }
        if (format != null) {
            format = format.optimize(opt, env, contextItemType);
        }
        if (groupSize != null) {
            groupSize = groupSize.optimize(opt, env, contextItemType);
        }
        if (groupSeparator != null) {
            groupSeparator = groupSeparator.optimize(opt, env, contextItemType);
        }
        if (letterValue != null) {
            letterValue = letterValue.optimize(opt, env, contextItemType);
        }
        if (ordinal != null) {
            ordinal = ordinal.optimize(opt, env, contextItemType);
        }
        if (lang != null) {
            lang = lang.optimize(opt, env, contextItemType);
        }
        return this;
    }


   /**
     * Get the immediate sub-expressions of this expression. Default implementation
     * returns a zero-length array, appropriate for an expression that has no
     * sub-expressions.
     * @return an iterator containing the sub-expressions of this expression
     */

    public Iterator iterateSubExpressions() {
        List sub = new ArrayList(9);
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
        return Type.STRING_TYPE;
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
     * @return if the offer is not accepted, return this expression unchanged.
     *         Otherwise return the result of rewriting the expression to promote
     *         this subexpression
     * @throws net.sf.saxon.trans.XPathException
     *          if any error is detected
     */

    public Expression promote(PromotionOffer offer) throws XPathException {
        Expression exp = offer.accept(this);
        if (exp!=null) {
            return exp;
        } else {
            if (select != null) {
                select = doPromotion(select, offer);
            }
            if (value != null) {
                value = doPromotion(value, offer);
            }
            if (format != null) {
                format = doPromotion(format, offer);
            }
            if (groupSize != null) {
                groupSize = doPromotion(groupSize, offer);
            }
            if (groupSeparator != null) {
                groupSeparator = doPromotion(groupSeparator, offer);
            }
            if (letterValue != null) {
                letterValue = doPromotion(letterValue, offer);
            }
            if (ordinal != null) {
                ordinal = doPromotion(ordinal, offer);
            }
            if (lang != null) {
                lang = doPromotion(lang, offer);
            }
            if (count != null) {
                count.promote(offer);
            }
            if (from != null) {
                from.promote(offer);
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
                if (backwardsCompatible && vec.size() > 0) {
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
                        throw new DynamicError("NaN");  // thrown to be caught
                    }
                    num = num.round();
                    if (num.compareTo(IntegerValue.MAX_LONG) > 0) {
                        vec.add(((BigIntegerValue)num.convert(Type.INTEGER, context)).getBigInteger());
                    } else {
                        if (num.compareTo(IntegerValue.ZERO) < 0) {
                            throw new DynamicError("The numbers to be formatted must not be negative");
                            // thrown to be caught
                        }
                        long i = ((NumericValue) num.convert(Type.INTEGER, context)).longValue();
                        vec.add(new Long(i));
                    }
                } catch (DynamicError err) {
                    if (backwardsCompatible) {
                        vec.add("NaN");
                    } else {
                        vec.add(val.getStringValue());
                        DynamicError e = new DynamicError("Cannot convert supplied value to an integer. " + err.getMessage());
                        e.setErrorCode("XTDE0980");
                        e.setXPathContext(context);
                        throw e;
                    }
                }
            }
            if (backwardsCompatible && vec.size()==0) {
                vec.add("NaN");
            }
        } else {
            NodeInfo source;
            if (select != null) {
                source = (NodeInfo) select.evaluateItem(context);
            } else {
                Item item = context.getContextItem();
                if (!(item instanceof NodeInfo)) {
                    DynamicError err = new DynamicError("context item for xsl:number must be a node");
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
            String g = groupSize.evaluateAsString(context);
            try {
                gpsize = Integer.parseInt(g);
            } catch (NumberFormatException err) {
                DynamicError e = new DynamicError("grouping-size must be numeric");
                e.setXPathContext(context);
                e.setErrorCode("XTDE0030");
                throw e;
            }
        }

        if (groupSeparator != null) {
            gpseparator = groupSeparator.evaluateAsString(context);
        }

        if (ordinal != null) {
            ordinalVal = ordinal.evaluateAsString(context);
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
            String language = lang.evaluateAsString(context);
            if (nationalNumberers == null) {
                nationalNumberers = new HashMap(4);
            }
            numb = (Numberer)nationalNumberers.get(language);
            if (numb == null) {
                numb = makeNumberer(language, null, context);
                nationalNumberers.put(language, numb);
            }
        }

        if (letterValue == null) {
            letterVal = "";
        } else {
            letterVal = letterValue.evaluateAsString(context);
            if (!("alphabetic".equals(letterVal) || "traditional".equals(letterVal))) {
                DynamicError e = new DynamicError("letter-value must be \"traditional\" or \"alphabetic\"");
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
            nf.prepare(format.evaluateAsString(context));
        } else {
            nf = formatter;
        }

        CharSequence s = nf.format(vec, gpsize, gpseparator, letterVal, ordinalVal, numb);
        return new StringValue(s);
    }

    /**
     * Load a Numberer class for a given language and check it is OK.
     * @param language the language for which a Numberer is required
     * @return a suitable numberer. If no specific numberer is available
     * for the language, the default (English) numberer is used.
     */

    public static Numberer makeNumberer(String language, String country, XPathContext context) {

        Numberer numberer;
        if ("en".equals(language)) {
            numberer = defaultNumberer;
        } else {
            String langClassName = "net.sf.saxon.number.Numberer_";
            for (int i = 0; i < language.length(); i++) {
                if (Character.isLetter(language.charAt(i))) {
                    langClassName += language.charAt(i);
                }
            }
            try {
                if (context == null) {
                    Object x = Class.forName(langClassName).newInstance();
                    numberer = (Numberer)x ;
                } else {
                    numberer = (Numberer) (context.getConfiguration().getInstance(langClassName, null));
                }
            } catch (XPathException err) {
                numberer = defaultNumberer;
            } catch (ClassNotFoundException err) {
                numberer = defaultNumberer;
            } catch (InstantiationException err) {
                numberer = defaultNumberer;
            } catch (IllegalAccessException err) {
                numberer = defaultNumberer;
            }
        }
        numberer.setCountry(country);

        return numberer;
    }

    /**
     * Diagnostic print of expression structure. The expression is written to the System.err
     * output stream
     *
     * @param level indentation level for this expression
     @param out
     @param config
     */

    public void display(int level, PrintStream out, Configuration config) {
        out.println(ExpressionTool.indent(level) + "xsl:number");
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
