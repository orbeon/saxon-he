////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
// Copyright (c) 2015 Saxonica Limited.
// This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
// If a copy of the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
// This Source Code Form is "Incompatible With Secondary Licenses", as defined by the Mozilla Public License, v. 2.0.
////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

package net.sf.saxon.expr.instruct;

import com.saxonica.ee.bytecode.ExpressionCompiler;
import com.saxonica.ee.bytecode.InterpretedExpressionCompiler;
import net.sf.saxon.Configuration;
import net.sf.saxon.expr.*;
import net.sf.saxon.expr.number.NumberFormatter;
import net.sf.saxon.expr.parser.ContextItemStaticInfo;
import net.sf.saxon.expr.parser.ExpressionTool;
import net.sf.saxon.expr.parser.ExpressionVisitor;
import net.sf.saxon.expr.parser.PromotionOffer;
import net.sf.saxon.functions.Number_1;
import net.sf.saxon.lib.ConversionRules;
import net.sf.saxon.lib.Numberer;
import net.sf.saxon.om.Item;
import net.sf.saxon.om.NodeInfo;
import net.sf.saxon.om.SequenceIterator;
import net.sf.saxon.pattern.Pattern;
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

    public static final int SINGLE = 0;
    public static final int MULTI = 1;
    public static final int ANY = 2;
    public static final int SIMPLE = 3;

    private Operand selectOp;
    private Operand valueOp;
    private Operand formatOp;
    private Operand groupSizeOp;
    private Operand groupSeparatorOp;
    private Operand letterValueOp;
    private Operand ordinalOp;
    private Operand startAtOp;
    private Operand langOp;

    private int level;
    /*@Nullable*/ private Pattern count = null;
    private Pattern from = null;
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
                             boolean hasVariablesInPatterns,
                             boolean backwardsCompatible) {

        if (select != null) {
            selectOp = new Operand(this, select, OperandRole.SINGLE_ATOMIC);
        }
        if (value != null) {
            valueOp = new Operand(this, value, OperandRole.SINGLE_ATOMIC);
        }
        if (format != null) {
            formatOp = new Operand(this, format, OperandRole.SINGLE_ATOMIC);
        }
        if (groupSize != null) {
            groupSizeOp = new Operand(this, groupSize, OperandRole.SINGLE_ATOMIC);
        }
        if (groupSeparator != null) {
            groupSeparatorOp = new Operand(this, groupSeparator, OperandRole.SINGLE_ATOMIC);
        }
        if (letterValue != null) {
            letterValueOp = new Operand(this, letterValue, OperandRole.SINGLE_ATOMIC);
        }
        if (ordinal != null) {
            ordinalOp = new Operand(this, ordinal, OperandRole.SINGLE_ATOMIC);
        }
        if (startAt != null) {
            startAtOp = new Operand(this, startAt, OperandRole.SINGLE_ATOMIC);
        }
        if (lang != null) {
            langOp = new Operand(this, lang, OperandRole.SINGLE_ATOMIC);
        }

        this.level = level;
        this.count = count;
        this.from = from;
        this.formatter = formatter;
        this.hasVariablesInPatterns = hasVariablesInPatterns;
        this.backwardsCompatible = backwardsCompatible;



    }

    /**
     * Simplify an expression. This performs any static optimization (by rewriting the expression
     * as a different expression). The default implementation simplifies its operands.
     *
     * @return the simplified expression (or the original if unchanged, or if modified in-situ)
     * @throws net.sf.saxon.trans.XPathException if an error is discovered during expression
     *                                           rewriting
     */
    @Override
    public Expression simplify() throws XPathException {
        if (valueOp != null && !valueOp.getChildExpression().getItemType().isPlainType()) {
            valueOp.setChildExpression(Atomizer.makeAtomizer(valueOp.getChildExpression()));
        }
        preallocateNumberer(getConfiguration());
        return super.simplify();
    }

    public void preallocateNumberer(Configuration config) throws XPathException {
        if (langOp == null) {
            numberer = config.makeNumberer(null, null);
        } else {
            if (langOp.getChildExpression() instanceof StringLiteral) {
                String language = ((StringLiteral) langOp.getChildExpression()).getStringValue();
                if (language.length() != 0) {
                    ValidationFailure vf = StringConverter.STRING_TO_LANGUAGE.validate(language);
                    if (vf != null) {
                        langOp.setChildExpression(new StringLiteral(StringValue.EMPTY_STRING));
                        throw new XPathException("The lang attribute must be a valid language code", "XTDE0030");
                    }
                }
                numberer = config.makeNumberer(language, null);
            }   // else we allocate a numberer at run-time
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
        typeCheckChildren(visitor, contextInfo);
        if (selectOp == null && valueOp == null) {
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
                err.setLocation(getLocation());
                throw err;
            }
        }

//        if (count != null) {
//            new count.typeCheck(visitor, contextInfo);
//        }
//        if (from != null) {
//            new PatternSponsor(from).typeCheck(visitor, contextInfo);
//        }
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
        optimizeChildren(visitor, contextInfo);
        return this;
    }

    @Override
    public Iterable<Operand> operands() {
        List<Operand> sub = new ArrayList<Operand>(9);
        for (Operand o : operandSparseList(selectOp, valueOp, formatOp, groupSizeOp,
                groupSeparatorOp, letterValueOp, ordinalOp, startAtOp, langOp)) {
            sub.add(o);
        }
        if (count != null) {
            sub.add(new Operand(this, count, OperandRole.INSPECT));
        }
        if (from != null) {
            sub.add(new Operand(this, from, OperandRole.INSPECT));
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
        NumberInstruction exp = new NumberInstruction(
                copy(selectOp), level, count, from, copy(valueOp), copy(formatOp),
                copy(groupSizeOp), copy(groupSeparatorOp), copy(letterValueOp),
                copy(ordinalOp), copy(startAtOp),
                copy(langOp), formatter, hasVariablesInPatterns, backwardsCompatible);
        ExpressionTool.copyLocationInfo(this, exp);
        return exp;
        // TODO: copy the patterns (level and count)
    }

    private Expression copy(Operand op) {
        return op == null ? null : op.getChildExpression().copy();
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
        return selectOp == null ? StaticProperty.DEPENDS_ON_CONTEXT_ITEM : 0;
    }

    /*@NotNull*/
    public ItemType getItemType() {
        return BuiltInAtomicType.STRING;
    }

    public int computeCardinality() {
        return StaticProperty.EXACTLY_ONE;
    }

    /**
     * An implementation of Expression must provide at least one of the methods evaluateItem(), iterate(), or process().
     * This method indicates which of these methods is provided directly. The other methods will always be available
     * indirectly, using an implementation that relies on one of the other methods.
     *
     * @return the implementation method, for example {@link #ITERATE_METHOD} or {@link #EVALUATE_METHOD} or
     * {@link #PROCESS_METHOD}
     */
    @Override
    public int getImplementationMethod() {
        return EVALUATE_METHOD;
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
     * @throws net.sf.saxon.trans.XPathException
     *          if any error is detected
     */

    public void promoteChildren(PromotionOffer offer) throws XPathException {
        super.promoteChildren(offer);
        if (count != null) {
            count.promote(offer, this);
        }
        if (from != null) {
            from.promote(offer, this);
        }
    }

    public Item evaluateItem(XPathContext context) throws XPathException {
        long value = -1;
        List<Object> vec = null;    // a list whose items may be of type either Long or
        // BigInteger or the string to be output (e.g. "NaN")
        final ConversionRules rules = context.getConfiguration().getConversionRules();
        String startAv = startAtOp.getChildExpression().evaluateAsString(context).toString();
        List<Integer> startValues = parseStartAtValue(startAv);

        if (valueOp != null) {
            SequenceIterator iter = valueOp.getChildExpression().iterate(context);
            vec = new ArrayList<Object>(4);
            AtomicValue val;
            int pos = 0;
            while ((val = (AtomicValue)iter.next()) != null) {
                if (backwardsCompatible && !vec.isEmpty()) {
                    break;
                }
                int startValue = startValues.size() > pos ? startValues.get(pos) : startValues.get(startValues.size()-1);
                pos++;
                try {
                    NumericValue num;
                    if (val instanceof NumericValue) {
                        num = (NumericValue) val;
                    } else {
                        num = Number_1.convert(val, context.getConfiguration());
                    }
                    if (num.isNaN()) {
                        throw new XPathException("NaN");  // thrown to be caught
                    }
                    num = num.round(0);
                    if (num.compareTo(Int64Value.MAX_LONG) > 0) {
                        BigInteger bi = ((BigIntegerValue) Converter.convert(num, BuiltInAtomicType.INTEGER, rules).asAtomic()).asBigInteger();
                        if (startValue != 1) {
                            bi = bi.add(BigInteger.valueOf(startValue - 1));
                        }
                        vec.add(bi);
                    } else {
                        if (num.compareTo(Int64Value.ZERO) < 0) {
                            throw new XPathException("The numbers to be formatted must not be negative");
                            // thrown to be caught
                        }
                        long i = ((NumericValue) Converter.convert(num, BuiltInAtomicType.INTEGER, rules).asAtomic()).longValue();
                        i += startValue - 1;
                        vec.add(i);
                    }
                } catch (XPathException err) {
                    if (backwardsCompatible) {
                        vec.add("NaN");
                    } else {
                        vec.add(val.getStringValue());
                        XPathException e = new XPathException("Cannot convert supplied value to an integer. " + err.getMessage());
                        e.setErrorCode("XTDE0980");
                        e.setLocation(getLocation());
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
            if (selectOp != null) {
                source = (NodeInfo) selectOp.getChildExpression().evaluateItem(context);
            } else {
                Item item = context.getContextItem();
                if (!(item instanceof NodeInfo)) {
                    XPathException err = new XPathException("context item for xsl:number must be a node");
                    err.setErrorCode("XTTE0990");
                    err.setIsTypeError(true);
                    err.setXPathContext(context);
                    err.setLocation(getLocation());
                    throw err;
                }
                source = (NodeInfo) item;
            }

            if (level == SIMPLE) {
                value = Navigator.getNumberSimple(source, context);
                value += startValues.get(0) - 1;
            } else if (level == SINGLE) {
                value = Navigator.getNumberSingle(source, count, from, context);
                if (value == 0) {
                    vec = Collections.emptyList();     // an empty list
                } else {
                    value += startValues.get(0) - 1;
                }
            } else if (level == ANY) {
                value = Navigator.getNumberAny(this, source, count, from, context, hasVariablesInPatterns);
                if (value == 0) {
                    vec = Collections.emptyList();     // an empty list
                } else {
                    value += startValues.get(0) - 1;
                }
            } else if (level == MULTI) {
                vec = new ArrayList<Object>();
                int pos = 0;
                for (long n : Navigator.getNumberMulti(source, count, from, context)) {
                    int startValue = startValues.size() > pos ? startValues.get(pos) : startValues.get(startValues.size()-1);
                    pos++;
                    vec.add(n + startValue - 1);
                }
            }
        }

        int gpsize = 0;
        String gpseparator = "";
        String letterVal;
        String ordinalVal = null;

        if (groupSizeOp != null) {
            String g = groupSizeOp.getChildExpression().evaluateAsString(context).toString();
            try {
                gpsize = Integer.parseInt(g);
            } catch (NumberFormatException err) {
                XPathException e = new XPathException("grouping-size must be numeric");
                e.setXPathContext(context);
                e.setErrorCode("XTDE0030");
                e.setLocation(getLocation());
                throw e;
            }
        }

        if (groupSeparatorOp != null) {
            gpseparator = groupSeparatorOp.getChildExpression().evaluateAsString(context).toString();
        }

        if (ordinalOp != null) {
            ordinalVal = ordinalOp.getChildExpression().evaluateAsString(context).toString();
        }

        // fast path for the simple case

        if (vec == null && formatOp == null && gpsize == 0 && langOp == null) {
            return new StringValue("" + value);
        }

        // Use the numberer decided at compile time if possible; otherwise try to get it from
        // a table of numberers indexed by language; if not there, load the relevant class and
        // add it to the table.
        Numberer numb = numberer;
        if (numb == null) {
            if (langOp == null) {
                numb = context.getConfiguration().makeNumberer(null, null);
            } else {
                String language = langOp.getChildExpression().evaluateAsString(context).toString();
                ValidationFailure vf = StringConverter.STRING_TO_LANGUAGE.validate(language);
                if (vf != null) {
                    throw new XPathException("The lang attribute of xsl:number must be a valid language code", "XTDE0030");
                }
                numb = context.getConfiguration().makeNumberer(language, null);
            }
        }

        if (letterValueOp == null) {
            letterVal = "";
        } else {
            letterVal = letterValueOp.getChildExpression().evaluateAsString(context).toString();
            if (!("alphabetic".equals(letterVal) || "traditional".equals(letterVal))) {
                XPathException e = new XPathException("letter-value must be \"traditional\" or \"alphabetic\"");
                e.setXPathContext(context);
                e.setErrorCode("XTDE0030");
                e.setLocation(getLocation());
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
            nf.prepare(formatOp.getChildExpression().evaluateAsString(context).toString());
        } else {
            nf = formatter;
        }

        CharSequence s = nf.format(vec, gpsize, gpseparator, letterVal, ordinalVal, numb);
        return new StringValue(s);
    }

    public List<Integer> parseStartAtValue(String value) throws XPathException {
        List<Integer> list = new ArrayList<Integer>();
        String[] tokens = value.split("\\s+");
        for (String tok : tokens) {
            try {
                int n = Integer.parseInt(tok);
                list.add(n);
            } catch (NumberFormatException err) {
                XPathException e = new XPathException("Invalid start-at value: non-integer component {" + tok + "}");
                e.setErrorCode("XTDE0030");
                e.setLocation(getLocation());
                throw e;
            }
        }
        if (list.isEmpty()) {
            XPathException e = new XPathException("Invalid start-at value: no numeric components found");
            e.setErrorCode("XTDE0030");
            e.setLocation(getLocation());
            throw e;
        }
        return list;
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

    public void export(ExpressionPresenter out) {
        out.startElement("xslNumber", this);
        String flags = "";
        if (hasVariablesInPatterns) {
            flags += "v";
        }
        if (backwardsCompatible) {
            flags += "1";
        }
        if (!flags.isEmpty()) {
            out.emitAttribute("flags", flags);
        }
        if (valueOp == null) {
            out.emitAttribute("level", level == ANY ? "any" : level == SINGLE ? "single" : "multi");
        }
        if (count != null) {
            out.setChildRole("count");
            count.export(out);
        }
        if (from != null) {
            out.setChildRole("from");
            from.export(out);
        }
        if (selectOp != null) {
            out.setChildRole("select");
            selectOp.getChildExpression().export(out);
        }
        if (valueOp != null) {
            out.setChildRole("value");
            valueOp.getChildExpression().export(out);
        }
        if (formatOp != null) {
            out.setChildRole("format");
            formatOp.getChildExpression().export(out);
        }
        if (startAtOp != null) {
            out.setChildRole("startAt");
            startAtOp.getChildExpression().export(out);
        }
        if (langOp != null) {
            out.setChildRole("lang");
            langOp.getChildExpression().export(out);
        }
        if (ordinalOp != null) {
            out.setChildRole("ordinal");
            ordinalOp.getChildExpression().export(out);
        }
        if (groupSeparatorOp != null) {
            out.setChildRole("gpSep");
            groupSeparatorOp.getChildExpression().export(out);
        }
        if (groupSizeOp != null) {
            out.setChildRole("gpSize");
            groupSizeOp.getChildExpression().export(out);
        }
        out.endElement();
    }
}

