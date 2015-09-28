////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
// Copyright (c) 2014 Saxonica Limited.
// This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
// If a copy of the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
// This Source Code Form is "Incompatible With Secondary Licenses", as defined by the Mozilla Public License, v. 2.0.
////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

package net.sf.saxon.expr.instruct;

import com.saxonica.ee.bytecode.ExpressionCompiler;
import com.saxonica.ee.bytecode.InterpretedExpressionCompiler;
import net.sf.saxon.expr.*;
import net.sf.saxon.expr.number.NumberFormatter;
import net.sf.saxon.expr.parser.ContextItemStaticInfo;
import net.sf.saxon.expr.parser.ExpressionVisitor;
import net.sf.saxon.expr.parser.PromotionOffer;
import net.sf.saxon.functions.NumberFn;
import net.sf.saxon.lib.ConversionRules;
import net.sf.saxon.lib.Numberer;
import net.sf.saxon.om.Item;
import net.sf.saxon.om.NodeInfo;
import net.sf.saxon.om.SequenceIterator;
import net.sf.saxon.pattern.Pattern;
import net.sf.saxon.pattern.PatternSponsor;
import net.sf.saxon.trace.ExpressionPresenter;
import net.sf.saxon.trans.XPathException;
import net.sf.saxon.tree.util.Navigator;
import net.sf.saxon.type.*;
import net.sf.saxon.value.*;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collections;
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

    private static final int SELECT = 0;
    private static final int VALUE = 1;
    private static final int FORMAT = 2;
    private static final int GROUP_SIZE = 3;
    private static final int GROUP_SEPARATOR = 4;
    private static final int LETTER_VALUE = 5;
    private static final int ORDINAL = 6;
    private static final int START_AT = 7;
    private static final int LANG = 8;

    private Expression[] subExpressions = new Expression[LANG+1];

    private int level;
    /*@Nullable*/ private Pattern count = null;
    private Pattern from = null;
//    private Expression select = null;
//    private Expression value = null;
//    private Expression format = null;
//    private Expression groupSize = null;
//    private Expression groupSeparator = null;
//    private Expression letterValue = null;
//    private Expression ordinal = null;
//    private Expression startAt = null;
//    private Expression lang = null;
    private NumberFormatter formatter = null;
    private Numberer numberer = null;
    private boolean hasVariablesInPatterns;
    private boolean backwardsCompatible;

    /**
     * Construct a NumberInstruction
     *
     * @param select                 the expression supplied in the select attribute
     * @param level                  one of "single", "level", "multi"
     * @param count                  the pattern supplied in the count attribute
     * @param from                   the pattern supplied in the from attribute
     * @param value                  the expression supplied in the value attribute
     * @param format                 the expression supplied in the format attribute
     * @param groupSize              the expression supplied in the group-size attribute
     * @param groupSeparator         the expression supplied in the grouping-separator attribute
     * @param letterValue            the expression supplied in the letter-value attribute
     * @param ordinal                the expression supplied in the ordinal attribute
     * @param startAt                the expression supplied in the start-at attribute
     * @param lang                   the expression supplied in the lang attribute
     * @param formatter              A NumberFormatter to be used
     * @param numberer               A Numberer to be used for localization
     * @param hasVariablesInPatterns true if one or more of the patterns contains variable references
     * @param backwardsCompatible    true if running in 1.0 compatibility mode
     */

    public NumberInstruction(Expression select,
                             int level,
                             Pattern count,
                             Pattern from,
                             Expression value,
                             Expression format,
                             Expression groupSize,
                             Expression groupSeparator,
                             Expression letterValue,
                             Expression ordinal,
                             Expression startAt,
                             Expression lang,
                             NumberFormatter formatter,
                             Numberer numberer,
                             boolean hasVariablesInPatterns,
                             boolean backwardsCompatible) {
        subExpressions[SELECT] = select;
        subExpressions[VALUE] = value;
        subExpressions[FORMAT] = format;
        subExpressions[GROUP_SIZE] = groupSize;
        subExpressions[GROUP_SEPARATOR] = groupSeparator;
        subExpressions[LETTER_VALUE] = letterValue;
        subExpressions[ORDINAL] = ordinal;
        subExpressions[START_AT] = startAt;
        subExpressions[LANG] = lang;
        this.level = level;
        this.count = count;
        this.from = from;
        this.formatter = formatter;
        this.numberer = numberer;
        this.hasVariablesInPatterns = hasVariablesInPatterns;
        this.backwardsCompatible = backwardsCompatible;

        if (subExpressions[VALUE] != null && !subExpressions[VALUE].getItemType().isPlainType()) {
            subExpressions[VALUE] = Atomizer.makeAtomizer(subExpressions[VALUE]);
        }

        for (Operand o : operands()) {
            adoptChildExpression(o.getExpression());
        }
    }

    /**
     * Perform static analysis of an expression and its subexpressions.
     * <p/>
     * <p>This checks statically that the operands of the expression have
     * the correct type; if necessary it generates code to do run-time type checking or type
     * conversion. A static type error is reported only if execution cannot possibly succeed, that
     * is, if a run-time type error is inevitable. The call may return a modified form of the expression.</p>
     * <p/>
     * <p>This method is called after all references to functions and variables have been resolved
     * to the declaration of the function or variable. However, the types of such functions and
     * variables will only be accurately known if they have been explicitly declared.</p>
     *
     *
       param visitor         an expression visitor
     * @param  contextInfo   information about the type of the context item
     * @return the original expression, rewritten to perform necessary
     *         run-time type checks, and to perform other type-related
     *         optimizations
     * @throws XPathException if an error is discovered during this phase
     *                        (typically a type error)
     */

    /*@NotNull*/
    public Expression typeCheck(ExpressionVisitor visitor, ContextItemStaticInfo contextInfo) throws XPathException {
        for (int i=0; i<subExpressions.length; i++) {
            if (subExpressions[i] != null) {
                subExpressions[i] = visitor.typeCheck(subExpressions[i], contextInfo);
            }
        }
        if (subExpressions[SELECT] == null && subExpressions[VALUE] == null) {
            // we are numbering the context node
            XPathException err = null;
            if (contextInfo == null || contextInfo.getItemType() == null) {
                err = new XPathException(
                        "xsl:number requires a select attribute, a value attribute, or a context item");
            } else if (contextInfo.getItemType().isPlainType()) {
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

        if (count != null) {
            visitor.typeCheck(new PatternSponsor(count),  contextInfo);
        }
        if (from != null) {
            visitor.typeCheck(new PatternSponsor(from),  contextInfo);
        }
        return this;
    }

    /**
     * Perform optimisation of an expression and its subexpressions.
     * <p/>
     * <p>This method is called after all references to functions and variables have been resolved
     * to the declaration of the function or variable, and after all type checking has been done.</p>
     *
     * @param visitor         an expression visitor
     * @param contextInfo the static type of "." at the point where this expression is invoked.
     *                        The parameter is set to null if it is known statically that the context item will be undefined.
     *                        If the type of the context item is not known statically, the argument is set to
     *                        {@link net.sf.saxon.type.Type#ITEM_TYPE}
     * @return the original expression, rewritten if appropriate to optimize execution
     * @throws XPathException if an error is discovered during this phase
     *                        (typically a type error)
     */

    /*@NotNull*/
    public Expression optimize(ExpressionVisitor visitor, ContextItemStaticInfo contextInfo) throws XPathException {
        for (int i=0; i<subExpressions.length; i++) {
            if (subExpressions[i] != null) {
                subExpressions[i] = visitor.optimize(subExpressions[i], contextInfo);
            }
        }
        return this;
    }

    @Override
    public Iterable<Operand> operands() {
        List<Operand> sub = new ArrayList<Operand>(9);
        for (Expression subExpression : subExpressions) {
            if (subExpression != null) {
                sub.add(new Operand(subExpression, OperandRole.SINGLE_ATOMIC));
            }
        }
        if (count != null) {
            sub.add(new Operand(new PatternSponsor(count), OperandRole.INSPECT));
        }
        if (from != null) {
            sub.add(new Operand(new PatternSponsor(from), OperandRole.INSPECT));
        }
        return sub;
    }

    /**
     * Copy an expression. This makes a deep copy.
     *
     * @return the copy of the original expression
     */

    /*@NotNull*/
    public Expression copy() {
        return new NumberInstruction(
                copy(subExpressions[SELECT]), level, count, from, copy(subExpressions[VALUE]), copy(subExpressions[FORMAT]),
                copy(subExpressions[GROUP_SIZE]), copy(subExpressions[GROUP_SEPARATOR]), copy(subExpressions[LETTER_VALUE]),
                copy(subExpressions[ORDINAL]), copy(subExpressions[START_AT]),
                copy(subExpressions[LANG]), formatter, numberer, hasVariablesInPatterns, backwardsCompatible);
        // TODO: copy the patterns (level and count)
    }

    private Expression copy(Expression exp) {
        return exp == null ? null : exp.copy();
    }

    /**
     * Replace one subexpression by a replacement subexpression
     *
     * @param original    the original subexpression
     * @param replacement the replacement subexpression
     * @return true if the original subexpression is found
     */

    public boolean replaceOperand(Expression original, Expression replacement) {
        boolean found = false;
        for (int i=0; i<subExpressions.length; i++) {
            if (subExpressions[i] == original) {
                subExpressions[i] = replacement;
                found = true;
            }
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
        return subExpressions[SELECT] == null ? StaticProperty.DEPENDS_ON_CONTEXT_ITEM : 0;
    }

    /*@NotNull*/
    public ItemType getItemType() {
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
     * @param offer  details of the offer, for example the offer to move
     *               expressions that don't depend on the context to an outer level in
     *               the containing expression
     * @param parent the containing expression in the expression tree
     * @return if the offer is not accepted, return this expression unchanged.
     *         Otherwise return the result of rewriting the expression to promote
     *         this subexpression
     * @throws net.sf.saxon.trans.XPathException
     *          if any error is detected
     */

    public Expression promote(PromotionOffer offer, Expression parent) throws XPathException {
        Expression exp = offer.accept(parent, this);
        if (exp != null) {
            return exp;
        } else {
            for (int i=0; i<subExpressions.length; i++) {
                if (subExpressions[i] != null) {
                    subExpressions[i] = doPromotion(subExpressions[i], offer);
                }
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
        List<Object> vec = null;    // a list whose items may be of type either Long or
        // BigInteger or the string to be output (e.g. "NaN")
        final ConversionRules rules = context.getConfiguration().getConversionRules();
        long startValue;
        String startAv = subExpressions[START_AT].evaluateAsString(context).toString();
        try {
            startValue = Integer.parseInt(startAv);
        } catch (NumberFormatException e) {
            XPathException err = new XPathException("Value of start-at attribute must be an integer", "XTDE1001");
            err.setLocator(this);
            err.setXPathContext(context);
            throw err;
        }
        startValue--;
        if (subExpressions[VALUE] != null) {

            SequenceIterator iter = subExpressions[VALUE].iterate(context);
            vec = new ArrayList<Object>(4);
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
                        num = NumberFn.convert(val, context.getConfiguration());
                    }
                    if (num.isNaN()) {
                        throw new XPathException("NaN");  // thrown to be caught
                    }
                    num = num.round(0);
                    if (num.compareTo(Int64Value.MAX_LONG) > 0) {
                        BigInteger bi = ((BigIntegerValue) Converter.convert(num, BuiltInAtomicType.INTEGER, rules).asAtomic()).asBigInteger();
                        if (startValue != 0) {
                            bi = bi.add(BigInteger.valueOf(startValue));
                        }
                        vec.add(bi);
                    } else {
                        if (num.compareTo(Int64Value.ZERO) < 0) {
                            throw new XPathException("The numbers to be formatted must not be negative");
                            // thrown to be caught
                        }
                        long i = ((NumericValue) Converter.convert(num, BuiltInAtomicType.INTEGER, rules).asAtomic()).longValue();
                        i += startValue;
                        vec.add(i);
                    }
                } catch (XPathException err) {
                    if (backwardsCompatible) {
                        vec.add("NaN");
                    } else {
                        vec.add(val.getStringValue());
                        XPathException e = new XPathException("Cannot convert supplied value to an integer. " + err.getMessage());
                        e.setErrorCode("XTDE0980");
                        e.setLocator(this);
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
            if (subExpressions[SELECT] != null) {
                source = (NodeInfo) subExpressions[SELECT].evaluateItem(context);
            } else {
                Item item = context.getContextItem();
                if (!(item instanceof NodeInfo)) {
                    XPathException err = new XPathException("context item for xsl:number must be a node");
                    err.setErrorCode("XTTE0990");
                    err.setIsTypeError(true);
                    err.setXPathContext(context);
                    err.setLocator(this);
                    throw err;
                }
                source = (NodeInfo) item;
            }

            if (level == SIMPLE) {
                value = Navigator.getNumberSimple(source, context);
                value += startValue;
            } else if (level == SINGLE) {
                value = Navigator.getNumberSingle(source, count, from, context);
                if (value == 0) {
                    vec = Collections.emptyList();     // an empty list
                } else {
                    value += startValue;
                }
            } else if (level == ANY) {
                value = Navigator.getNumberAny(this, source, count, from, context, hasVariablesInPatterns);
                if (value == 0) {
                    vec = Collections.emptyList();     // an empty list
                } else {
                    value += startValue;
                }
            } else if (level == MULTI) {
                vec = new ArrayList<Object>();
                for (long n : Navigator.getNumberMulti(source, count, from, context)) {
                    vec.add(n + startValue);
                }
            }
        }

        int gpsize = 0;
        String gpseparator = "";
        String letterVal;
        String ordinalVal = null;

        if (subExpressions[GROUP_SIZE] != null) {
            String g = subExpressions[GROUP_SIZE].evaluateAsString(context).toString();
            try {
                gpsize = Integer.parseInt(g);
            } catch (NumberFormatException err) {
                XPathException e = new XPathException("grouping-size must be numeric");
                e.setXPathContext(context);
                e.setErrorCode("XTDE0030");
                e.setLocator(this);
                throw e;
            }
        }

        if (subExpressions[GROUP_SEPARATOR] != null) {
            gpseparator = subExpressions[GROUP_SEPARATOR].evaluateAsString(context).toString();
        }

        if (subExpressions[ORDINAL] != null) {
            ordinalVal = subExpressions[ORDINAL].evaluateAsString(context).toString();
        }

        // fast path for the simple case

        if (vec == null && subExpressions[FORMAT] == null && gpsize == 0 && subExpressions[LANG] == null) {
            return new StringValue("" + value);
        }

        // Use the numberer decided at compile time if possible; otherwise try to get it from
        // a table of numberers indexed by language; if not there, load the relevant class and
        // add it to the table.
        Numberer numb = numberer;
        if (numb == null) {
            String language = subExpressions[LANG].evaluateAsString(context).toString();
            ValidationFailure vf = StringConverter.STRING_TO_LANGUAGE.validate(language);
            if (vf != null) {
                throw new XPathException("The lang attribute of xsl:number must be a valid language code", "XTDE0030");
            }
            numb = context.getConfiguration().makeNumberer(language, null);
        }

        if (subExpressions[LETTER_VALUE] == null) {
            letterVal = "";
        } else {
            letterVal = subExpressions[LETTER_VALUE].evaluateAsString(context).toString();
            if (!("alphabetic".equals(letterVal) || "traditional".equals(letterVal))) {
                XPathException e = new XPathException("letter-value must be \"traditional\" or \"alphabetic\"");
                e.setXPathContext(context);
                e.setErrorCode("XTDE0030");
                e.setLocator(this);
                throw e;
            }
        }

        if (vec == null) {
            vec = new ArrayList<Object>(1);
            vec.add(value);
        }

        NumberFormatter nf;
        if (formatter == null) {              // format not known until run-time
            nf = new NumberFormatter();
            nf.prepare(subExpressions[FORMAT].evaluateAsString(context).toString());
        } else {
            nf = formatter;
        }

        CharSequence s = nf.format(vec, gpsize, gpseparator, letterVal, ordinalVal, numb);
        return new StringValue(s);
    }

//#ifdefined BYTECODE

    /**
     * Return the compiler of the NumberInstruction expression
     *
     * @return the relevant ExpressionCompiler
     */
    @Override
    public ExpressionCompiler getExpressionCompiler() {
        return new InterpretedExpressionCompiler();
    }
//#endif

    /**
     * Diagnostic print of expression structure. The abstract expression tree
     * is written to the supplied output destination.
     */

    public void explain(ExpressionPresenter out) {
        out.startElement("xslNumber");
        out.emitAttribute("level", level == ANY ? "any" : level == SINGLE ? "single" : "multi");
        if (count != null) {
            out.emitAttribute("count", count.toString());
        }
        if (from != null) {
            out.emitAttribute("from", from.toString());
        }
        if (subExpressions[SELECT] != null) {
            out.startSubsidiaryElement("select");
            subExpressions[SELECT].explain(out);
            out.endSubsidiaryElement();
        }
        if (subExpressions[VALUE] != null) {
            out.startSubsidiaryElement("value");
            subExpressions[VALUE].explain(out);
            out.endSubsidiaryElement();
        }
        if (subExpressions[FORMAT] != null) {
            out.startSubsidiaryElement("format");
            subExpressions[FORMAT].explain(out);
            out.endSubsidiaryElement();
        }
        out.endElement();
    }
}

